package dev.starfall.analysis;

/**
 * A scanline across a silhouette boundary: how far paper takes to become ink, and how wide
 * the wet-bleed halo in front of it is.
 *
 * <p>STYLE.md §3 opens with "nothing in this game has a hard edge except the blades", and
 * §3.2 requires a low-alpha halo <i>softer and larger</i> than the dissolve band so the
 * figure sits in the paper rather than on it. This primitive is how that is checked, and it
 * is what convicted System 3's hair: paper to core in one pixel with no halo at all, beside
 * a garment transitioning over 1-76 px behind a 10-131 px halo. It is also what discharged
 * System 1's D3, which had recorded a three-pixel polygon cut that pass 6 had already fixed.
 *
 * <p>Definitions, stated because a transition width means nothing without them. Walking from
 * the paper side inward, with {@code P} the paper level at the start of the walk and {@code C}
 * the darkest luminance reached:
 * <ul>
 *   <li><b>core</b> is the first sample at or below {@code P - 0.95*(P-C)}</li>
 *   <li><b>transition</b> runs from the last sample at or above {@code P - 0.05*(P-C)} to core</li>
 *   <li><b>halo</b> runs from the first sample after which the signal stays continuously below
 *       the halo level all the way to the transition — the region where pigment has wicked
 *       into the paper but the shape has not started</li>
 * </ul>
 *
 * <p>The halo level is {@code P} minus the larger of 1% of the range and twice the paper's own
 * standard deviation over the leading samples, with a floor of 1.5 levels. That matters: the
 * paper in this project has visible tooth and oscillates about four levels peak to peak, so a
 * flat 1% threshold reports a 12 px halo in front of a mark that has none — which would have
 * quietly cleared System 3's hair of the one-pixel edge it was correctly convicted of. And the
 * halo must be <i>continuous</i> up to the transition, because a halo is an approach, not a
 * single dip.
 */
public final class EdgeProfile {

    public enum Direction {
        RIGHT(1, 0), LEFT(-1, 0), DOWN(0, 1), UP(0, -1);

        public final int dx, dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public static Direction parse(String s) {
            return Direction.valueOf(s.trim().toUpperCase());
        }
    }

    public final int startX, startY;
    public final Direction direction;
    public final double[] samples;
    public final double paperLevel;
    /** Standard deviation of the leading paper samples — the noise floor any halo must clear. */
    public final double paperNoise;
    public final double coreLevel;
    public final int haloStart;
    public final int transitionStart;
    public final int coreIndex;

    private EdgeProfile(int startX, int startY, Direction direction, double[] samples,
                        double paperLevel, double paperNoise, double coreLevel,
                        int haloStart, int transitionStart, int coreIndex) {
        this.startX = startX;
        this.startY = startY;
        this.direction = direction;
        this.samples = samples;
        this.paperLevel = paperLevel;
        this.paperNoise = paperNoise;
        this.coreLevel = coreLevel;
        this.haloStart = haloStart;
        this.transitionStart = transitionStart;
        this.coreIndex = coreIndex;
    }

    /** Width in px of the paper-to-core transition. -1 when the walk never reaches core. */
    public int transitionWidth() {
        return coreIndex < 0 || transitionStart < 0 ? -1 : coreIndex - transitionStart;
    }

    /** Width in px of the wet-bleed halo ahead of the transition. -1 when undefined. */
    public int haloWidth() {
        return transitionStart < 0 || haloStart < 0 ? -1 : transitionStart - haloStart;
    }

    public boolean hardEdge() {
        int t = transitionWidth();
        return t >= 0 && t <= 2 && haloWidth() <= 2;
    }

    /**
     * @param length how far to walk, in px; must reach clear of the boundary in both
     *               directions or the levels will be wrong
     * @param paperSamples how many leading samples define the paper level
     */
    public static EdgeProfile measure(Frame f, int x, int y, Direction dir, int length, int paperSamples) {
        int n = Math.max(2, length);
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            int px = x + dir.dx * i;
            int py = y + dir.dy * i;
            s[i] = f.inside(px, py) ? f.lum[py * f.width + px] : Double.NaN;
        }
        int lead = Math.max(1, Math.min(paperSamples, n / 4));
        double p = 0;
        int pn = 0;
        for (int i = 0; i < lead; i++) {
            if (!Double.isNaN(s[i])) {
                p += s[i];
                pn++;
            }
        }
        double paper = pn == 0 ? 0 : p / pn;
        double variance = 0;
        for (int i = 0; i < lead; i++) {
            if (!Double.isNaN(s[i])) {
                variance += (s[i] - paper) * (s[i] - paper);
            }
        }
        double noise = pn > 1 ? Math.sqrt(variance / (pn - 1)) : 0;
        double core = paper;
        for (double v : s) {
            if (!Double.isNaN(v) && v < core) {
                core = v;
            }
        }
        double range = paper - core;
        int coreIdx = -1, transStart = -1, halo = -1;
        if (range > 1e-6) {
            double coreLevel = paper - 0.95 * range;
            double edgeLevel = paper - 0.05 * range;
            double haloLevel = paper - Math.max(Math.max(0.01 * range, 2 * noise), 1.5);
            for (int i = 0; i < n; i++) {
                if (!Double.isNaN(s[i]) && s[i] <= coreLevel) {
                    coreIdx = i;
                    break;
                }
            }
            if (coreIdx >= 0) {
                for (int i = coreIdx; i >= 0; i--) {
                    if (!Double.isNaN(s[i]) && s[i] >= edgeLevel) {
                        transStart = i;
                        break;
                    }
                }
                // Walk back from the transition while the signal stays below the halo level.
                // A halo is a continuous approach; a single noisy dip out on clean paper is
                // paper tooth, not pigment.
                int from = transStart >= 0 ? transStart : coreIdx;
                halo = from;
                for (int i = from - 1; i >= 0; i--) {
                    if (Double.isNaN(s[i]) || s[i] > haloLevel) {
                        break;
                    }
                    halo = i;
                }
            }
        }
        return new EdgeProfile(x, y, dir, s, paper, noise, core, halo, transStart, coreIdx);
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("edge from (%d,%d) heading %s, %d samples%n",
                startX, startY, direction, samples.length));
        sb.append(String.format("  paper %.1f (noise sd %.2f)  core %.1f  range %.1f%n",
                paperLevel, paperNoise, coreLevel, paperLevel - coreLevel));
        sb.append(String.format("  halo starts at +%d px, transition starts at +%d px, core reached at +%d px%n",
                haloStart, transitionStart, coreIndex));
        sb.append(String.format("  transition width %d px, halo width %d px  -> %s%n",
                transitionWidth(), haloWidth(),
                hardEdge() ? "HARD EDGE (STYLE.md 3: only blades may have one)" : "soft, sits in the paper"));
        sb.append("  samples ");
        for (int i = 0; i < samples.length; i++) {
            sb.append(String.format("%.0f ", samples[i]));
        }
        sb.append('\n');
        return sb.toString();
    }
}
