package dev.starfall.analysis;

/**
 * Sub-pixel inter-frame registration of a rectangle: the translation that best explains the
 * change between two frames.
 *
 * <p>This is the primitive that established System 3's one genuinely good result — the chain
 * of arrivals down the body, hips before head before hair before sleeve — and the primitive
 * that refuted the claimed hem lag by showing the hem-tip box moving 0.00 px while its
 * particle moved. It is deliberately blunt: a single translation per region per step, found
 * by minimising the sum of squared luminance differences over an integer search window and
 * then refined to sub-pixel by fitting a parabola to the error surface.
 *
 * <p>Sub-pixel matters more than it looks. At 60 Hz an integer search quantises velocity to
 * 60 px/s, and the lags STYLE.md §7.1 asks for are a few frames of a motion that peaks around
 * 200 px/s — so integer registration reports a staircase and puts zero-crossings wherever the
 * staircase happens to have a flat.
 */
public final class Registration {

    /** Default integer search radius, in pixels. */
    public static final int RADIUS = 14;

    public record Shift(Rect rect, double dx, double dy, double error, boolean clipped) {
        public double magnitude() {
            return Math.hypot(dx, dy);
        }
    }

    private Registration() {
    }

    public static Shift between(Frame a, Frame b, Rect region) {
        return between(a, b, region, RADIUS);
    }

    /**
     * @param region the patch in {@code a} to locate in {@code b}
     * @param radius integer search radius; a shift landing on the edge is reported as clipped,
     *               which usually means the region is too small or the motion too fast
     */
    public static Shift between(Frame a, Frame b, Rect region, int radius) {
        Rect r = region.clamp(a);
        if (r.isEmpty()) {
            return new Shift(r, 0, 0, 0, false);
        }
        int n = 2 * radius + 1;
        double[] err = new double[n * n];
        double best = Double.MAX_VALUE;
        int bi = radius, bj = radius;
        for (int j = -radius; j <= radius; j++) {
            for (int i = -radius; i <= radius; i++) {
                double e = ssd(a, b, r, i, j);
                err[(j + radius) * n + (i + radius)] = e;
                if (e < best) {
                    best = e;
                    bi = i;
                    bj = j;
                }
            }
        }
        boolean clipped = Math.abs(bi) == radius || Math.abs(bj) == radius;
        double dx = bi + parabolic(
                err[(bj + radius) * n + clampIdx(bi - 1, radius, n)],
                best,
                err[(bj + radius) * n + clampIdx(bi + 1, radius, n)]);
        double dy = bj + parabolic(
                err[clampIdx(bj - 1, radius, n) * n + (bi + radius)],
                best,
                err[clampIdx(bj + 1, radius, n) * n + (bi + radius)]);
        return new Shift(r, dx, dy, best, clipped);
    }

    private static int clampIdx(int v, int radius, int n) {
        return Math.max(0, Math.min(n - 1, v + radius));
    }

    /**
     * Vertex of the parabola through (-1, left), (0, mid), (+1, right), clamped to the cell.
     * Returns 0 when the samples are degenerate.
     */
    private static double parabolic(double left, double mid, double right) {
        double denom = left - 2 * mid + right;
        if (Math.abs(denom) < 1e-12) {
            return 0;
        }
        double off = 0.5 * (left - right) / denom;
        return Math.max(-0.5, Math.min(0.5, off));
    }

    /** Mean squared luminance difference between {@code a}'s patch and {@code b} shifted by (dx, dy). */
    private static double ssd(Frame a, Frame b, Rect r, int dx, int dy) {
        double sum = 0;
        int n = 0;
        for (int y = r.y; y <= r.y1(); y++) {
            int sy = y + dy;
            if (sy < 0 || sy >= b.height) {
                continue;
            }
            for (int x = r.x; x <= r.x1(); x++) {
                int sx = x + dx;
                if (sx < 0 || sx >= b.width) {
                    continue;
                }
                double d = a.lum[y * a.width + x] - b.lum[sy * b.width + sx];
                sum += d * d;
                n++;
            }
        }
        return n == 0 ? Double.MAX_VALUE : sum / n;
    }
}
