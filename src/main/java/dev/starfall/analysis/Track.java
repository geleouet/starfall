package dev.starfall.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * A named region's motion across a frame sequence: position, velocity, velocity
 * zero-crossings, peak speed and settle.
 *
 * <p>A zero-crossing is the moment a region reverses. Comparing crossings down a chain is
 * how STYLE.md §7.0.3 and §10's last row get graded — "nothing may arrive at the same time"
 * is exactly a statement that these numbers must differ — and it is the measurement that
 * produced System 3's best result and refuted two of its claims.
 *
 * <p><b>A crossing frame on its own is not a lag.</b> §7.1 is explicit that a lag figure
 * without its anchor is unfalsifiable, because a hem trailing the hips and a sleeve trailing
 * the wrist differ by a factor of three. Lag therefore lives in {@link Arrivals}, which
 * cannot be constructed without an anchor.
 */
public final class Track {

    public enum Axis { X, Y, PRINCIPAL }

    public enum Method {
        /** Ink-weighted centroid of a fixed box. Right for soft masses that change shape. */
        CENTROID,
        /** Sub-pixel SSD registration between consecutive frames, accumulated. Right for marks that keep their shape. */
        REGISTER
    }

    /** Velocity below this many px/frame is treated as noise when detecting a reversal. */
    public static final double DEFAULT_GATE = 0.15;

    /**
     * One velocity reversal.
     *
     * @param frame  fractional frame index, on the same half-frame convention the velocity uses
     * @param swing  peak speed of the run before plus peak speed of the run after; this is how
     *               a real reversal is told from a wobble, and why {@link #dominantCrossing()}
     *               rather than the first crossing is what an arrival chain should compare
     */
    public record Crossing(double frame, int fromSign, double swing) {
    }

    public final String name;
    public final Rect rect;
    public final Method method;
    public final Axis axis;
    /** Per-frame 2D position; for REGISTER this is the box origin plus accumulated shift. */
    public final double[] x;
    public final double[] y;
    /** Signed position along {@link #axis}, per frame. */
    public final double[] pos;
    /** Signed velocity along {@link #axis} in px/frame; {@code vel[i]} is the step from frame i to i+1, at time i+0.5. */
    public final double[] vel;
    /** Speed magnitude in px/frame at time i+0.5, independent of axis. */
    public final double[] speed;
    /** Unit vector of the axis the signed values are projected onto. */
    public final double axisX, axisY;
    public final double gate;
    /** Frames where registration hit the search-window edge; a non-empty list invalidates the track. */
    public final List<Integer> clipped;

    private Track(String name, Rect rect, Method method, Axis axis, double[] x, double[] y,
                  double axisX, double axisY, double gate, List<Integer> clipped) {
        this.name = name;
        this.rect = rect;
        this.method = method;
        this.axis = axis;
        this.x = x;
        this.y = y;
        this.axisX = axisX;
        this.axisY = axisY;
        this.gate = gate;
        this.clipped = List.copyOf(clipped);
        this.pos = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            pos[i] = x[i] * axisX + y[i] * axisY;
        }
        this.vel = new double[Math.max(0, x.length - 1)];
        this.speed = new double[vel.length];
        for (int i = 0; i < vel.length; i++) {
            vel[i] = pos[i + 1] - pos[i];
            speed[i] = Math.hypot(x[i + 1] - x[i], y[i + 1] - y[i]);
        }
    }

    /**
     * Builds a track from a position series that did not come from images — the headless
     * timing run, which samples the same scene the capture drives but at a finer rate, and a
     * scene probe that reports simulation state directly.
     */
    public static Track fromPositions(String name, Rect rect, double[] x, double[] y,
                                      Axis axis, double gate) {
        double ax = 1, ay = 0;
        switch (axis) {
            case X -> { ax = 1; ay = 0; }
            case Y -> { ax = 0; ay = 1; }
            case PRINCIPAL -> {
                double[] pa = principalAxis(x, y);
                ax = pa[0];
                ay = pa[1];
            }
        }
        return new Track(name, rect, Method.CENTROID, axis, x.clone(), y.clone(), ax, ay, gate, List.of());
    }

    public static Track of(String name, Rect rect, List<Frame> frames, Paper paper, double factor,
                           Method method, Axis axis, double gate, int radius) {
        int n = frames.size();
        double[] px = new double[n];
        double[] py = new double[n];
        List<Integer> clipped = new ArrayList<>();
        if (method == Method.CENTROID) {
            for (int i = 0; i < n; i++) {
                Centroid c = Centroid.measure(frames.get(i), paper, rect, factor);
                px[i] = c.x();
                py[i] = c.y();
            }
        } else {
            px[0] = rect.x + rect.w / 2.0;
            py[0] = rect.y + rect.h / 2.0;
            for (int i = 1; i < n; i++) {
                Registration.Shift s = Registration.between(frames.get(i - 1), frames.get(i), rect, radius);
                if (s.clipped()) {
                    clipped.add(i);
                }
                px[i] = px[i - 1] + s.dx();
                py[i] = py[i - 1] + s.dy();
            }
        }
        double ax = 1, ay = 0;
        switch (axis) {
            case X -> { ax = 1; ay = 0; }
            case Y -> { ax = 0; ay = 1; }
            case PRINCIPAL -> {
                double[] pa = principalAxis(px, py);
                ax = pa[0];
                ay = pa[1];
            }
        }
        return new Track(name, rect, method, axis, px, py, ax, ay, gate, clipped);
    }

    /**
     * First principal direction of the per-step displacements, sign-fixed so the axis points
     * along the net travel. Two-by-two eigenproblem, solved closed-form.
     */
    static double[] principalAxis(double[] px, double[] py) {
        double sxx = 0, sxy = 0, syy = 0;
        double nx = 0, ny = 0;
        for (int i = 1; i < px.length; i++) {
            double dx = px[i] - px[i - 1];
            double dy = py[i] - py[i - 1];
            if (Double.isNaN(dx) || Double.isNaN(dy)) {
                continue;
            }
            sxx += dx * dx;
            sxy += dx * dy;
            syy += dy * dy;
            nx += dx;
            ny += dy;
        }
        if (sxx + syy <= 0) {
            return new double[] {1, 0};
        }
        double tr = sxx + syy;
        double det = sxx * syy - sxy * sxy;
        double lambda = tr / 2 + Math.sqrt(Math.max(0, tr * tr / 4 - det));
        double vx, vy;
        if (Math.abs(sxy) > 1e-12) {
            vx = lambda - syy;
            vy = sxy;
        } else {
            vx = sxx >= syy ? 1 : 0;
            vy = sxx >= syy ? 0 : 1;
        }
        double len = Math.hypot(vx, vy);
        if (len < 1e-12) {
            return new double[] {1, 0};
        }
        vx /= len;
        vy /= len;
        if (vx * nx + vy * ny < 0) {
            vx = -vx;
            vy = -vy;
        }
        return new double[] {vx, vy};
    }

    /**
     * Frame indices at which the signed velocity reverses, interpolated to fractional frames.
     *
     * <p>Samples whose magnitude is below {@link #gate} are treated as neutral rather than as
     * a sign, so a region that pauses does not report two reversals. The returned value uses
     * the same half-frame convention the velocity does: a reversal between frames 11 and 12
     * reports 11.5.
     */
    public List<Double> zeroCrossings() {
        return crossings().stream().map(Crossing::frame).toList();
    }

    /**
     * Every sign change in the velocity, with the prominence of the runs either side.
     *
     * <p>{@link #gate} filters on prominence rather than on individual samples, and only
     * one-sidedly: a crossing survives when <i>either</i> the run before or the run after
     * peaks above the gate. That asymmetry is deliberate. A limb arriving at the end of a
     * knockback decelerates through zero and barely turns — the run after it is tiny — and
     * a two-sided gate would delete exactly the arrivals STYLE.md §7.0.3 is about. The
     * trade is that noise adjacent to a large run can register; that is what {@code swing}
     * and {@link Arrivals.Selector#DOMINANT} are for.
     */
    public List<Crossing> crossings() {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < vel.length; i++) {
            if (!Double.isNaN(vel[i]) && vel[i] != 0.0) {
                idx.add(i);
            }
        }
        // Runs of constant sign, as {firstPos, lastPos, sign} into idx.
        List<int[]> runs = new ArrayList<>();
        for (int k = 0; k < idx.size(); k++) {
            int sign = vel[idx.get(k)] > 0 ? 1 : -1;
            if (!runs.isEmpty() && runs.get(runs.size() - 1)[2] == sign) {
                runs.get(runs.size() - 1)[1] = k;
            } else {
                runs.add(new int[] {k, k, sign});
            }
        }
        List<Crossing> out = new ArrayList<>();
        for (int r = 1; r < runs.size(); r++) {
            int[] before = runs.get(r - 1);
            int[] after = runs.get(r);
            int i = idx.get(before[1]);
            int j = idx.get(after[0]);
            double a = Math.abs(vel[i]);
            double b = Math.abs(vel[j]);
            double frame = (i + 0.5) + (j - i) * (a / (a + b));
            double pa = runPeak(idx, before);
            double pb = runPeak(idx, after);
            if (Math.max(pa, pb) < gate) {
                continue;
            }
            out.add(new Crossing(frame, before[2], pa + pb));
        }
        return out;
    }

    private double runPeak(List<Integer> idx, int[] run) {
        double peak = 0;
        for (int k = run[0]; k <= run[1]; k++) {
            peak = Math.max(peak, Math.abs(vel[idx.get(k)]));
        }
        return peak;
    }

    /**
     * The reversal with the largest velocity swing around it — the one a reader would call
     * "the" reversal. Comparing first crossings down a chain is fragile: a hair bundle
     * wobbling in a breeze reverses several times before the body does anything.
     */
    public Crossing dominantCrossing() {
        Crossing best = null;
        for (Crossing c : crossings()) {
            if (best == null || c.swing() > best.swing()) {
                best = c;
            }
        }
        return best;
    }

    /** A copy with the velocity series smoothed by a centred [1,2,1]-style kernel, {@code n} times. */
    public Track smoothed(int passes) {
        if (passes <= 0) {
            return this;
        }
        double[] sx = x.clone();
        double[] sy = y.clone();
        for (int p = 0; p < passes; p++) {
            sx = smooth1(sx);
            sy = smooth1(sy);
        }
        return new Track(name, rect, method, axis, sx, sy, axisX, axisY, gate, clipped);
    }

    private static double[] smooth1(double[] v) {
        if (v.length < 3) {
            return v;
        }
        double[] out = v.clone();
        for (int i = 1; i < v.length - 1; i++) {
            out[i] = (v[i - 1] + 2 * v[i] + v[i + 1]) / 4.0;
        }
        return out;
    }

    /** Index into {@link #speed} of the fastest step, or -1. */
    public int peakSpeedStep() {
        int best = -1;
        double bv = -1;
        for (int i = 0; i < speed.length; i++) {
            if (!Double.isNaN(speed[i]) && speed[i] > bv) {
                bv = speed[i];
                best = i;
            }
        }
        return best;
    }

    public double peakSpeed() {
        int i = peakSpeedStep();
        return i < 0 ? Double.NaN : speed[i];
    }

    /** Total path length travelled, in px. */
    public double travel() {
        double s = 0;
        for (double v : speed) {
            if (!Double.isNaN(v)) {
                s += v;
            }
        }
        return s;
    }

    /**
     * The half-frame time after which speed never again exceeds {@code threshold} px/frame —
     * the settle. Returns NaN when the region is still moving at the last delivered frame,
     * which is itself the finding: {@code s3-p1-extreme} settles nowhere inside its window.
     */
    public double settle(double threshold) {
        for (int i = speed.length - 1; i >= 0; i--) {
            if (!Double.isNaN(speed[i]) && speed[i] > threshold) {
                return i + 1.5 > speed.length ? Double.NaN : i + 0.5;
            }
        }
        return 0;
    }

    public boolean monotoneSpeed() {
        for (int i = 1; i < speed.length; i++) {
            if (speed[i] < speed[i - 1] - 1e-9) {
                return false;
            }
        }
        return speed.length > 1;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-14s %s  method=%s axis=%s(%.2f,%.2f) gate=%.2f px/frame%n",
                name, rect.describe(), method, axis, axisX, axisY, gate));
        sb.append("   pos  ");
        for (double p : pos) {
            sb.append(String.format("%.1f ", p));
        }
        sb.append(String.format("%n   vel  "));
        for (double v : vel) {
            sb.append(String.format("%+.2f ", v));
        }
        sb.append(String.format("%n   peak speed %.2f px/frame at step %.1f   travel %.1f px%n",
                peakSpeed(), peakSpeedStep() + 0.5, travel()));
        List<Crossing> cs = crossings();
        if (cs.isEmpty()) {
            sb.append("   no reversal above the gate in this window\n");
        } else {
            Crossing dom = dominantCrossing();
            sb.append("   reversals ");
            for (Crossing c : cs) {
                sb.append(String.format("%.2f(swing %.2f%s) ", c.frame(), c.swing(),
                        c == dom ? ", dominant" : ""));
            }
            sb.append('\n');
        }
        if (!clipped.isEmpty()) {
            sb.append("   WARNING registration hit the search window at frames ").append(clipped)
                    .append(" — widen --radius or the displacement is not measurable\n");
        }
        return sb.toString();
    }
}
