package dev.starfall.analysis;

/**
 * Autocorrelation of a high-passed band, to detect periodic artefacts.
 *
 * <p>This is the measurement that found the torso banding which failed System 1's first two
 * reviews, and later the measurement that cleared it: "no peak above 0.25 at any lag from 4
 * to 200 px". STYLE.md §3's postscript records the expensive lesson — the banding was
 * diagnosed as a compositing problem and fixed architecturally when it was a frequency
 * problem, costing two passes. Before calling a ripple structural, measure its period.
 *
 * <p>Each scanline is high-passed by subtracting a moving average, which removes the
 * value gradient that would otherwise dominate the correlation at every lag, then correlated
 * against itself. Results are averaged over the scanlines of the band, which suppresses the
 * paper tooth (uncorrelated between rows) while preserving a genuine band (correlated).
 */
public final class Autocorrelation {

    public enum Axis { X, Y }

    /**
     * @param prominence how far the peak stands above the troughs either side of it. This,
     *                   not the raw correlation, is what says "periodic": a smooth wash
     *                   correlates near 1.0 at short lags and decays monotonically, so a raw
     *                   threshold convicts every soft gradient in the picture. A ripple puts
     *                   a local maximum at its period, and that is what the eye sees as
     *                   banding.
     */
    public record Peak(int lag, double value, double prominence) {
    }

    public final Rect rect;
    public final Axis axis;
    public final int minLag;
    public final int maxLag;
    public final int highPassWindow;
    /** Normalised correlation, indexed from {@link #minLag}. */
    public final double[] values;

    private Autocorrelation(Rect rect, Axis axis, int minLag, int maxLag, int window, double[] values) {
        this.rect = rect;
        this.axis = axis;
        this.minLag = minLag;
        this.maxLag = maxLag;
        this.highPassWindow = window;
        this.values = values;
    }

    public static Autocorrelation measure(Frame f, Rect region, Axis axis, int minLag, int maxLag) {
        return measure(f, region, axis, minLag, maxLag, 0);
    }

    /**
     * @param highPassWindow moving-average width; 0 picks {@code 2 * maxLag + 1}, wide enough
     *                       that the high-pass cannot itself manufacture a peak inside the
     *                       lag range being searched
     */
    public static Autocorrelation measure(Frame f, Rect region, Axis axis,
                                          int minLag, int maxLag, int highPassWindow) {
        Rect r = region.clamp(f);
        int lineLen = axis == Axis.X ? r.w : r.h;
        int lineCount = axis == Axis.X ? r.h : r.w;
        int lo = Math.max(1, minLag);
        int hi = Math.min(maxLag, lineLen - 2);
        if (hi < lo || lineLen < 4 || lineCount < 1) {
            return new Autocorrelation(r, axis, lo, Math.max(lo, hi), highPassWindow, new double[0]);
        }
        int win = highPassWindow > 0 ? highPassWindow : 2 * hi + 1;
        double[] sum = new double[hi - lo + 1];
        int used = 0;
        double[] line = new double[lineLen];
        for (int k = 0; k < lineCount; k++) {
            for (int i = 0; i < lineLen; i++) {
                int x = axis == Axis.X ? r.x + i : r.x + k;
                int y = axis == Axis.X ? r.y + k : r.y + i;
                line[i] = f.lum[y * f.width + x];
            }
            double[] hp = highPass(line, win);
            double energy = 0;
            for (double v : hp) {
                energy += v * v;
            }
            if (energy <= 1e-9) {
                continue;
            }
            used++;
            for (int lag = lo; lag <= hi; lag++) {
                double acc = 0;
                for (int i = 0; i + lag < lineLen; i++) {
                    acc += hp[i] * hp[i + lag];
                }
                sum[lag - lo] += acc / energy;
            }
        }
        double[] out = new double[hi - lo + 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = used == 0 ? 0 : sum[i] / used;
        }
        return new Autocorrelation(r, axis, lo, hi, win, out);
    }

    /** Signal minus its centred moving average. */
    static double[] highPass(double[] signal, int window) {
        int w = Math.max(3, window | 1);
        int half = w / 2;
        double[] out = new double[signal.length];
        double[] prefix = new double[signal.length + 1];
        for (int i = 0; i < signal.length; i++) {
            prefix[i + 1] = prefix[i] + signal[i];
        }
        for (int i = 0; i < signal.length; i++) {
            int a = Math.max(0, i - half);
            int b = Math.min(signal.length, i + half + 1);
            double mean = (prefix[b] - prefix[a]) / (b - a);
            out[i] = signal[i] - mean;
        }
        return out;
    }

    /** The most prominent local maximum — the strongest candidate period. */
    public Peak peak() {
        Peak best = new Peak(-1, 0, 0);
        for (int i = 1; i < values.length - 1; i++) {
            if (values[i] <= values[i - 1] || values[i] < values[i + 1]) {
                continue;
            }
            double leftMin = values[i];
            for (int k = i - 1; k >= 0; k--) {
                leftMin = Math.min(leftMin, values[k]);
                if (values[k] > values[i]) {
                    break;
                }
            }
            double rightMin = values[i];
            for (int k = i + 1; k < values.length; k++) {
                rightMin = Math.min(rightMin, values[k]);
                if (values[k] > values[i]) {
                    break;
                }
            }
            double prom = values[i] - Math.max(leftMin, rightMin);
            if (prom > best.prominence()) {
                best = new Peak(minLag + i, values[i], prom);
            }
        }
        return best;
    }

    /** Highest raw correlation anywhere in the range, reported for context, not for judging. */
    public Peak maxCorrelation() {
        int bi = -1;
        double bv = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > bv) {
                bv = values[i];
                bi = i;
            }
        }
        return bi < 0 ? new Peak(-1, 0, 0) : new Peak(minLag + bi, bv, 0);
    }

    /** STYLE.md's working limit: a peak standing this far above its troughs is a periodic artefact. */
    public static final double ARTEFACT_LIMIT = 0.25;

    public boolean clean() {
        return peak().prominence() <= ARTEFACT_LIMIT;
    }

    public String describe() {
        Peak p = peak();
        Peak m = maxCorrelation();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("autocorrelation over %s along %s, lags %d..%d, high-pass window %d%n",
                rect.describe(), axis, minLag, maxLag, highPassWindow));
        sb.append(String.format("  strongest periodic peak: prominence %.3f (correlation %.3f) at lag %d px%n",
                p.prominence(), p.value(), p.lag()));
        sb.append(String.format("  highest raw correlation %.3f at lag %d px (a smooth wash correlates high "
                + "at short lags; that is not banding)%n", m.value(), m.lag()));
        sb.append(String.format("  -> %s (prominence limit %.2f)%n",
                clean() ? "no periodic artefact" : "PERIODIC ARTEFACT", ARTEFACT_LIMIT));
        return sb.toString();
    }
}
