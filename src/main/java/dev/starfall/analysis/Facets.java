package dev.starfall.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Straight axis-aligned luminance edges per unit area — the instrument the System 4
 * audit (docs/system4-audit.md C2) had to build ad hoc, checked in.
 *
 * <p><b>Why this exists as a tool and not as a paragraph.</b> The closing record of
 * System 4 certified its clash marks clean with a connected-component bbox-fill
 * statistic, and the audit proved that statistic <em>structurally blind</em> to the
 * worst §3 violation in the frame: a field of axis-aligned filled rectangles reads
 * as one large component at fill 0.34, which is the same score an honest ink cloud
 * gets. A defect the certifying metric cannot see needs its own instrument, and
 * STYLE.md §7.1's amendment — "a rule that needs an instrument must ship the
 * instrument" — applies to reviews as much as to rules.
 *
 * <p><b>What it measures.</b> A <em>straight axis-aligned edge</em> is a contiguous
 * run, at least {@code minRun} px long, of adjacent-pixel luminance steps all
 * exceeding {@code minStep}, along a single row or a single column. That is the
 * audit's definition verbatim (|ΔL| &gt; 8, run ≥ 6), and this class reproduces the
 * audit's numbers on the tracked {@code s4-p4-parry-contact/frame_011.png} exactly
 * — {@code CorpusTest} asserts it, so the definition cannot drift silently.
 *
 * <p><b>The 2-px base is the adversarial hardening.</b> A boundary quantised into a
 * staircase whose risers are spread over two pixels carries steps of ΔL/2 each, so
 * a grid built that way evades the 1-px reading entirely while looking identical at
 * 8×. {@code FacetsTest.aTwoPixelStaircaseEvadesTheOnePixelBase} exhibits the
 * evasion, which is why every report prints both bases: the 1-px number is the
 * audit-comparable one, the 2-px number is the one an evader has to beat as well.
 *
 * <p><b>The blade mask.</b> STYLE.md §5 licenses exactly one hard edge, and a box
 * near the clash usually contains it. Measured on this project's own parry window,
 * the single longest straight edge in the audit's face box (V, 50 px) is the
 * blade's own rim — so an unmasked reading over a box the blade crosses charges the
 * figure for steel. The mask is the same one §2.1's step statistic used: cool-bright
 * (L &gt; 130, b − r &gt; 4), dilated one pixel.
 */
public final class Facets {

    /** One straight run, for the report: axis, position, start, length. */
    public record Run(boolean vertical, int x, int y, int length) {
        String describe() {
            return (vertical ? "V x=" + x + " y" + y + ".." + (y + length)
                    : "H y=" + y + " x" + x + ".." + (x + length));
        }
    }

    /** One reading: counts and longest runs for both axes, through {@code rect}. */
    public record Reading(Rect rect, int base, boolean bladeMasked,
                          int vCount, int vLongest, int hCount, int hLongest,
                          List<Run> topRuns) {

        /** Straight edges per 1000 px of the rectangle — the audit's headline unit. */
        public double per1000() {
            return (vCount + hCount) * 1000.0 / Math.max(1, rect.area());
        }
    }

    private Facets() {
    }

    /**
     * Measures {@code frame} through {@code rect}.
     *
     * @param base      pixel distance of the luminance difference: 1 is the audit's
     *                  definition, 2 catches staircases spread over two pixels
     * @param maskBlade true to skip any step whose either endpoint is cool-bright
     *                  (the blade, the one licensed hard edge)
     */
    public static Reading measure(Frame f, Rect rectIn, double minStep, int minRun,
                                  int base, boolean maskBlade) {
        Rect rect = rectIn.clamp(f);
        if (rect.isEmpty()) {
            throw new IllegalArgumentException("rect " + rectIn + " is outside the frame");
        }
        boolean[] blade = maskBlade ? bladeMask(f, rect) : null;

        List<Run> runs = new ArrayList<>();
        int vCount = 0, vLongest = 0, hCount = 0, hLongest = 0;

        // Vertical edges: a step along x, run contiguous in y at one x-boundary.
        for (int x = rect.x; x < rect.x1() - base + 1; x++) {
            int run = 0, start = 0;
            for (int y = rect.y; y <= rect.y1(); y++) {
                boolean step = Math.abs(f.lum(x + base, y) - f.lum(x, y)) > minStep
                        && !masked(blade, rect, x, y) && !masked(blade, rect, x + base, y);
                if (step) {
                    if (run == 0) {
                        start = y;
                    }
                    run++;
                } else {
                    if (run >= minRun) {
                        vCount++;
                        vLongest = Math.max(vLongest, run);
                        runs.add(new Run(true, x, start, run));
                    }
                    run = 0;
                }
            }
            if (run >= minRun) {
                vCount++;
                vLongest = Math.max(vLongest, run);
                runs.add(new Run(true, x, start, run));
            }
        }
        // Horizontal edges: a step along y, run contiguous in x at one y-boundary.
        for (int y = rect.y; y < rect.y1() - base + 1; y++) {
            int run = 0, start = 0;
            for (int x = rect.x; x <= rect.x1(); x++) {
                boolean step = Math.abs(f.lum(x, y + base) - f.lum(x, y)) > minStep
                        && !masked(blade, rect, x, y) && !masked(blade, rect, x, y + base);
                if (step) {
                    if (run == 0) {
                        start = x;
                    }
                    run++;
                } else {
                    if (run >= minRun) {
                        hCount++;
                        hLongest = Math.max(hLongest, run);
                        runs.add(new Run(false, start, y, run));
                    }
                    run = 0;
                }
            }
            if (run >= minRun) {
                hCount++;
                hLongest = Math.max(hLongest, run);
                runs.add(new Run(false, start, y, run));
            }
        }
        runs.sort((a, b) -> Integer.compare(b.length(), a.length()));
        List<Run> top = new ArrayList<>(runs.subList(0, Math.min(5, runs.size())));
        return new Reading(rect, base, maskBlade, vCount, vLongest, hCount, hLongest, top);
    }

    private static boolean masked(boolean[] blade, Rect rect, int x, int y) {
        if (blade == null || !rect.contains(x, y)) {
            return false;
        }
        return blade[(y - rect.y) * rect.w + (x - rect.x)];
    }

    /**
     * The licensed-light mask, dilated one pixel: cool-bright (L &gt; 130,
     * b − r &gt; 4 — the §2.1 blade convention) <b>or</b> plain bright (L &gt; 165).
     *
     * <p>The second clause is System 3b pass 2's, with the finding that forced
     * it: the clash bloom of §7.3 is {@code CLASH_CORE #FFF6E2} — <em>warm</em>
     * light — so the cool-only mask passed its trail straight through, and the
     * longest "straight edge" in the foe's head box on the graded frame was the
     * glow's own falloff rail (V x=621 y332..351, L 227→168 over two pixels,
     * s3b-p2-try7). §5 licenses the blade's edge and §7.3 licenses the clash;
     * a mask that strips one licensed light and charges the figure for the
     * other is a convention nobody had named (the exact class of debt §5.3).
     * Nothing on a delivered face may print above L 165 by construction — the
     * skin base tops out near L 101 on the dusk stage — so the clause cannot
     * hide face defects.
     */
    private static boolean[] bladeMask(Frame f, Rect rect) {
        boolean[] raw = new boolean[rect.w * rect.h];
        for (int y = rect.y; y <= rect.y1(); y++) {
            for (int x = rect.x; x <= rect.x1(); x++) {
                int i = f.index(x, y);
                raw[(y - rect.y) * rect.w + (x - rect.x)] =
                        (f.lum[i] > 130 && (f.b(i) - f.r(i)) > 4) || f.lum[i] > 165;
            }
        }
        boolean[] out = new boolean[raw.length];
        for (int y = 0; y < rect.h; y++) {
            for (int x = 0; x < rect.w; x++) {
                boolean any = false;
                for (int dy = -1; dy <= 1 && !any; dy++) {
                    for (int dx = -1; dx <= 1 && !any; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < rect.w && ny < rect.h) {
                            any = raw[ny * rect.w + nx];
                        }
                    }
                }
                out[y * rect.w + x] = any;
            }
        }
        return out;
    }
}
