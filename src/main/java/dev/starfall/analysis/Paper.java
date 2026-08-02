package dev.starfall.analysis;

/**
 * Estimates the paper ground level of a frame.
 *
 * <p>Every ink measurement in this package is expressed relative to paper rather than to an
 * absolute level, because the paper is warm and slightly different in every capture (215-219
 * across the corpus on disk) and STYLE.md §2.2 defines the brightest non-emissive element as
 * the paper itself. A threshold of "0.85 x paper" is portable between captures; a threshold
 * of "184" is not.
 *
 * <p>The estimator is the modal luminance of a border strip. The border is chosen over the
 * whole frame because the figure, the fog band and the ground grass all pull a global
 * percentile around, and over a corner sample because a single corner catches whatever
 * vignette or fog happens to be there.
 */
public final class Paper {

    /** Width of the border strip sampled, in pixels. */
    public static final int STRIP = 16;

    public final double level;
    public final double median;
    public final int strip;

    private Paper(double level, double median, int strip) {
        this.level = level;
        this.median = median;
        this.strip = strip;
    }

    public static Paper estimate(Frame f) {
        return estimate(f, STRIP);
    }

    public static Paper estimate(Frame f, int strip) {
        int s = Math.max(1, Math.min(strip, Math.min(f.width, f.height) / 2));
        int[] hist = new int[256];
        int n = 0;
        double[] all = new double[2 * s * f.width + 2 * s * f.height];
        for (int y = 0; y < f.height; y++) {
            boolean edgeRow = y < s || y >= f.height - s;
            for (int x = 0; x < f.width; x++) {
                if (!edgeRow && x >= s && x < f.width - s) {
                    continue;
                }
                double l = f.lum[y * f.width + x];
                hist[Math.max(0, Math.min(255, (int) Math.round(l)))]++;
                all[n++] = l;
            }
        }
        int mode = 0;
        for (int i = 1; i < 256; i++) {
            if (hist[i] > hist[mode]) {
                mode = i;
            }
        }
        double[] trimmed = new double[n];
        System.arraycopy(all, 0, trimmed, 0, n);
        java.util.Arrays.sort(trimmed);
        double med = n == 0 ? mode : trimmed[n / 2];
        return new Paper(mode, med, s);
    }

    /** A fixed, operator-supplied paper level — for reproducing a historical measurement. */
    public static Paper fixed(double level) {
        return new Paper(level, level, 0);
    }

    /** Ink threshold: a pixel is ink when its luminance is below {@code factor} x paper. */
    public double threshold(double factor) {
        return factor * level;
    }

    /** Ink weight of a luminance: how far below paper it sits, never negative. */
    public double weight(double luminance) {
        return Math.max(0.0, level - luminance);
    }

    @Override
    public String toString() {
        return String.format("paper=%.1f (border mode, %d px strip; median %.1f)", level, strip, median);
    }
}
