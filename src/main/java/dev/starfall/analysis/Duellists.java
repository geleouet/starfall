package dev.starfall.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The two measurements a two-figure frame needs, and neither of them existed until
 * System 4 pass 2.
 *
 * <h2>Why they are here rather than in a review's scratch directory</h2>
 *
 * <p>Both numbers below decided the pass-1 review, and both were computed by hand
 * in a one-off script. STYLE.md §11.2b(e): <i>"a discipline written into a document
 * but not into the tool that reads it is documentation, not a guard."</i> A
 * measurement that lives in a reviewer's shell history is in the same position as a
 * rule that lives in someone's head — the next pass cannot run it, so the next pass
 * cannot grade itself, so the next pass ships blind. Checking them in costs
 * nothing and turns both acceptance criteria from prose into commands.
 *
 * <h2>Blade separation</h2>
 *
 * <p>The review's own definition, restated exactly: segment both blades as
 * connected components of <em>cool bright</em> pixels ({@code b − r > −6},
 * luminance {@code > 212}), then take the point-to-point minimum between the two
 * largest clouds. Paper is warm at {@code r − b ≈ +24}, so the colour test
 * separates steel from paper and, importantly, from the warm clash core — a bloom
 * that bridged the two blades would otherwise report them as met.
 *
 * <h2>The corridor</h2>
 *
 * <p>The widest run of image columns carrying no ink between the two bodies,
 * normalised by the frame's own figure height. STYLE.md §11.3 requires exactly
 * that normalisation on any scene with a camera move, and this project has one
 * whose figure box ranges 4.08× across a phrase.
 *
 * <p>Both are absolute pixel statistics reduced to ratios before they leave, per
 * §11.2b(d) — the raw pixel counts are printed too, but a comparison across
 * harness versions is only valid on the ratio.
 */
public final class Duellists {

    /** {@code b − r} above which a bright pixel counts as cool rather than warm. */
    public static final double COOL_BR = -6.0;

    /** Luminance above which a pixel is bright enough to be steel. */
    public static final double BRIGHT = 212.0;

    /** Blade clouds smaller than this are specular flecks, not blades. */
    public static final int MIN_BLADE_PIXELS = 30;

    private Duellists() {
    }

    // -- blades ----------------------------------------------------------------

    /**
     * @param a           the larger cool-bright cloud, or null
     * @param b           the second, or null when the two blades have merged into one
     * @param separation  point-to-point minimum between them, in pixels; zero when merged
     * @param figureHeight the frame's own figure height, the thing to normalise by
     */
    public record Blades(Cloud a, Cloud b, double separation, int figureHeight, int clouds) {

        /** STYLE.md §11.3: a pixel number on a moving-camera scene is a ratio or it is void. */
        public double fraction() {
            return figureHeight <= 0 ? Double.NaN : separation / figureHeight;
        }

        /** True when the frame contains no second blade at all — a different failure from a wide gap. */
        public boolean merged() {
            return b == null && a != null;
        }

        public String describe() {
            if (a == null) {
                return "no blade found";
            }
            if (merged()) {
                return String.format(java.util.Locale.ROOT,
                        "one cool-bright cloud of %d px %s: the blades are merged or only one is lit",
                        a.pixels, a.bounds.describe());
            }
            return String.format(java.util.Locale.ROOT,
                    "%.1f px = %.4f of a %d px figure   clouds %d px %s / %d px %s",
                    separation, fraction(), figureHeight, a.pixels, a.bounds.describe(),
                    b.pixels, b.bounds.describe());
        }
    }

    /** One connected component of cool bright pixels. */
    public record Cloud(int pixels, Rect bounds, int[] xs, int[] ys) {
    }

    public static Blades blades(Frame f, Paper paper, double inkFactor) {
        return blades(f, Figure.detect(f, paper, inkFactor).height());
    }

    /**
     * The same, with the figure height <em>given</em>.
     *
     * <p>STYLE.md §11.3 makes every pixel number on a moving-camera scene "a ratio to
     * the figure's own span", which makes the span a load-bearing part of the
     * measurement rather than a convenience. Detecting it is only safe while the
     * darkest thing in the frame is the figure; on a Family B dusk stage the ground is
     * itself a dark ink smear and the detected box spans the whole sheet.
     */
    public static Blades blades(Frame f, int figureHeight) {
        List<Cloud> clouds = coolBrightClouds(f);
        if (clouds.isEmpty()) {
            return new Blades(null, null, Double.NaN, figureHeight, 0);
        }
        Cloud a = clouds.get(0);
        if (clouds.size() < 2) {
            return new Blades(a, null, 0.0, figureHeight, 1);
        }
        Cloud b = clouds.get(1);
        return new Blades(a, b, minDistance(a, b), figureHeight, clouds.size());
    }

    /** Every cool-bright component above {@link #MIN_BLADE_PIXELS}, largest first. */
    public static List<Cloud> coolBrightClouds(Frame f) {
        boolean[] lit = new boolean[f.width * f.height];
        for (int i = 0; i < lit.length; i++) {
            int p = f.rgb[i];
            int r = (p >> 16) & 0xFF;
            int b = p & 0xFF;
            lit[i] = f.lum[i] > BRIGHT && (b - r) > COOL_BR;
        }
        List<Cloud> out = new ArrayList<>();
        boolean[] seen = new boolean[lit.length];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        List<Integer> members = new ArrayList<>();
        for (int start = 0; start < lit.length; start++) {
            if (!lit[start] || seen[start]) {
                continue;
            }
            q.clear();
            members.clear();
            q.add(start);
            seen[start] = true;
            int x0 = start % f.width, x1 = x0, y0 = start / f.width, y1 = y0;
            while (!q.isEmpty()) {
                int p = q.poll();
                members.add(p);
                int x = p % f.width, y = p / f.width;
                if (x < x0) x0 = x;
                if (x > x1) x1 = x;
                if (y < y0) y0 = y;
                if (y > y1) y1 = y;
                // Eight-connected: a blade is a one-pixel-wide diagonal sliver at
                // this framing (STYLE.md §5 wants it thin), and four-connectivity
                // shatters a diagonal one-pixel line into a string of components.
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= f.width || ny >= f.height) {
                            continue;
                        }
                        int n = ny * f.width + nx;
                        if (lit[n] && !seen[n]) {
                            seen[n] = true;
                            q.add(n);
                        }
                    }
                }
            }
            if (members.size() < MIN_BLADE_PIXELS) {
                continue;
            }
            int[] xs = new int[members.size()];
            int[] ys = new int[members.size()];
            for (int i = 0; i < members.size(); i++) {
                int p = members.get(i);
                xs[i] = p % f.width;
                ys[i] = p / f.width;
            }
            out.add(new Cloud(members.size(), Rect.ofCorners(x0, y0, x1, y1), xs, ys));
        }
        out.sort(Comparator.comparingInt(Cloud::pixels).reversed());
        return out;
    }

    private static double minDistance(Cloud a, Cloud b) {
        // Bounding boxes first: two clouds whose boxes are far apart cannot have
        // near points, and the parry's clouds run to thousands of pixels each.
        double best = Double.MAX_VALUE;
        for (int i = 0; i < a.xs.length; i++) {
            for (int j = 0; j < b.xs.length; j++) {
                double dx = a.xs[i] - b.xs[j];
                double dy = a.ys[i] - b.ys[j];
                double d = dx * dx + dy * dy;
                if (d < best) {
                    best = d;
                }
            }
        }
        return Math.sqrt(best);
    }

    // -- the corridor ----------------------------------------------------------

    /**
     * @param columns      widest run of ink-free image columns strictly between the two bodies
     * @param figureHeight the frame's own figure height
     * @param leftEdge     rightmost ink column of the left body
     * @param rightEdge    leftmost ink column of the right body
     * @param bodies       how many ink components above {@link #BODY_SHARE} were found
     */
    public record Corridor(int columns, int figureHeight, int leftEdge, int rightEdge, int bodies) {

        public double fraction() {
            return figureHeight <= 0 ? Double.NaN : columns / (double) figureHeight;
        }

        public boolean merged() {
            return bodies < 2;
        }

        public String describe() {
            if (merged()) {
                return "the two bodies are one connected ink component: no corridor exists";
            }
            return String.format(java.util.Locale.ROOT,
                    "%d columns = %.4f of a %d px figure   between x=%d and x=%d",
                    columns, fraction(), figureHeight, leftEdge, rightEdge);
        }
    }

    /**
     * Share of the frame's ink a component must hold to count as a body.
     *
     * <p>A tenth: large enough that a blade trail, a hair mass or a bloom cannot
     * pass for a duellist, small enough that a pale figure carrying a third of the
     * dark one's ink still counts. It is also the threshold {@code RegionSet}'s
     * refusal uses, and it is the same number on purpose — "two bodies" has to mean
     * one thing across the tool.
     */
    public static final double BODY_SHARE = 0.10;

    /**
     * The clear-paper corridor between the two bodies.
     *
     * <p>Measured between the two largest ink components rather than between two
     * halves of the frame, because the answer that matters is whether a reader's
     * eye finds <em>body — gap — body</em>. When the two bodies are one component
     * there is no corridor and this says so rather than returning zero, which is a
     * different claim.
     */
    public static Corridor corridor(Frame f, Paper paper, double inkFactor) {
        Figure figure = Figure.detect(f, paper, inkFactor);
        List<Component> parts = inkComponents(f, paper.threshold(inkFactor));
        int total = 0;
        for (Component c : parts) {
            total += c.pixels;
        }
        List<Component> bodies = new ArrayList<>();
        for (Component c : parts) {
            if (total > 0 && c.pixels >= BODY_SHARE * total) {
                bodies.add(c);
            }
        }
        if (bodies.size() < 2) {
            return new Corridor(0, figure.height(), 0, 0, bodies.size());
        }
        bodies.sort(Comparator.comparingInt(c -> c.x0));
        Component left = bodies.get(0);
        Component right = bodies.get(1);
        // The corridor is the run of columns carrying no ink *from either body*
        // between the right edge of the left one and the left edge of the right
        // one. Ink from a blade crossing the gap counts: a blade is allowed to
        // bridge it, and STYLE.md's reading is "body — gap — body, and then the
        // blades bridging the gap", so the corridor is measured on the bodies.
        int from = left.x1 + 1;
        int to = right.x0 - 1;
        int best = 0;
        int run = 0;
        for (int x = from; x <= to; x++) {
            boolean clear = !left.hasColumn(x) && !right.hasColumn(x);
            run = clear ? run + 1 : 0;
            best = Math.max(best, run);
        }
        return new Corridor(best, figure.height(), left.x1, right.x0, bodies.size());
    }

    /** One connected ink component, with the columns it occupies. */
    static final class Component {
        int pixels;
        int x0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y0 = Integer.MAX_VALUE;
        int y1 = Integer.MIN_VALUE;
        boolean[] columns;

        Component(int width) {
            columns = new boolean[width];
        }

        boolean hasColumn(int x) {
            return x >= 0 && x < columns.length && columns[x];
        }

        Rect bounds() {
            return Rect.ofCorners(x0, y0, x1, y1);
        }
    }

    /**
     * Every connected ink component in the frame, largest first.
     *
     * <p>Four-connected and unopened: the 3×3 opening the pass-1 review used is a
     * good idea for a corridor measurement and a bad one for a refusal, because it
     * changes what counts as "one component" and the refusal has to agree with what
     * the eye sees. The share threshold does the work the opening was doing.
     */
    static List<Component> inkComponents(Frame f, double threshold) {
        boolean[] ink = new boolean[f.width * f.height];
        for (int i = 0; i < ink.length; i++) {
            ink[i] = f.lum[i] < threshold;
        }
        List<Component> out = new ArrayList<>();
        boolean[] seen = new boolean[ink.length];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int start = 0; start < ink.length; start++) {
            if (!ink[start] || seen[start]) {
                continue;
            }
            Component c = new Component(f.width);
            q.clear();
            q.add(start);
            seen[start] = true;
            while (!q.isEmpty()) {
                int p = q.poll();
                c.pixels++;
                int x = p % f.width, y = p / f.width;
                c.columns[x] = true;
                if (x < c.x0) c.x0 = x;
                if (x > c.x1) c.x1 = x;
                if (y < c.y0) c.y0 = y;
                if (y > c.y1) c.y1 = y;
                if (x > 0 && ink[p - 1] && !seen[p - 1]) { seen[p - 1] = true; q.add(p - 1); }
                if (x < f.width - 1 && ink[p + 1] && !seen[p + 1]) { seen[p + 1] = true; q.add(p + 1); }
                if (y > 0 && ink[p - f.width] && !seen[p - f.width]) { seen[p - f.width] = true; q.add(p - f.width); }
                if (y < f.height - 1 && ink[p + f.width] && !seen[p + f.width]) { seen[p + f.width] = true; q.add(p + f.width); }
            }
            out.add(c);
        }
        out.sort(Comparator.comparingInt((Component c) -> c.pixels).reversed());
        return out;
    }

    /**
     * How many ink components in this frame are large enough to be a body.
     *
     * <p>The one thing {@code RegionSet} needs in order to refuse: STYLE.md §11.3's
     * <i>"the tool must refuse when the ink resolves into two components each above
     * some share of the total and no figure has been named."</i>
     */
    public static int bodyCount(Frame f, Paper paper, double inkFactor) {
        List<Component> parts = inkComponents(f, paper.threshold(inkFactor));
        int total = 0;
        for (Component c : parts) {
            total += c.pixels;
        }
        int n = 0;
        for (Component c : parts) {
            if (total > 0 && c.pixels >= BODY_SHARE * total) {
                n++;
            }
        }
        return n;
    }

    /**
     * The bounding box of the {@code index}-th body from the left, or null.
     *
     * <p>This is the other half of the two-figure region set: a {@code head} region
     * has to resolve against <em>a</em> figure, and on a two-figure frame the
     * detected figure box spans both. Naming the figure is what makes the box mean
     * anything.
     */
    public static Rect bodyBounds(Frame f, Paper paper, double inkFactor, int index) {
        List<Component> parts = inkComponents(f, paper.threshold(inkFactor));
        int total = 0;
        for (Component c : parts) {
            total += c.pixels;
        }
        List<Component> bodies = new ArrayList<>();
        for (Component c : parts) {
            if (total > 0 && c.pixels >= BODY_SHARE * total) {
                bodies.add(c);
            }
        }
        bodies.sort(Comparator.comparingInt(c -> c.x0));
        if (index < 0 || index >= bodies.size()) {
            return null;
        }
        return bodies.get(index).bounds();
    }
}
