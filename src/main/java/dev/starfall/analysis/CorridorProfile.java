package dev.starfall.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The corridor between two duellists, <b>per height band</b>, with the acceptance
 * floors taken from reference image 3 and asserted to pass on it.
 *
 * <h2>Why the old statistic had to be replaced rather than re-tuned</h2>
 *
 * <p>{@link Duellists#corridor} answers "what is the widest run of image columns
 * carrying no ink anywhere between the two bodies". System 4 passes 1 and 2 were
 * both graded against it, at a floor of 0.06 of a figure height read off reference
 * image 3 by eye. STYLE.md §11.0 now records what happens when that floor is
 * measured on the image it was read off:
 *
 * <blockquote>Measured, that image's clear whole-frame column is <b>0.015</b> — and,
 * like the capture, <b>0.000</b> over the full figure height, because the duellists'
 * hands nearly touch. The acceptance the project was failing against was one its own
 * ground truth fails by 4×.</blockquote>
 *
 * <p>And the diagnosis of <em>why</em>, which is what this class is built from:
 *
 * <blockquote>The failure is not the wrong number, it is the wrong <em>shape</em> of
 * number. A single scalar across the whole figure is decided by the tightest band,
 * which for two duellists is always the hands — the one place they are supposed to
 * be close. What separates the reference from the capture is the <b>profile</b>.</blockquote>
 *
 * <p>So the statistic is a vector of five numbers, one per band of the figure, and
 * the acceptance is the reference's own profile. {@code CorridorProfileTest} asserts
 * that reference image 3 passes every floor this class declares, in the same suite
 * as the assertion that a capture must — which is STYLE.md §11.0's <i>"let the
 * assertion that the reference passes live in the test suite beside the one that the
 * capture must"</i>, put in the tool rather than in a document.
 *
 * <h2>Two things this measures differently from {@link Duellists#corridor}</h2>
 *
 * <ol>
 *   <li><b>The background is per row, not one paper level.</b> A dusk sky graded from
 *       indigo to coral has no single "paper", and the whole point of the exercise is
 *       to run the same instrument on the corpus and on the capture. Row background is
 *       the median of the leftmost and rightmost {@link #BACKGROUND_STRIP} columns of
 *       that row, which is a warm paper level on a capture and the sky at that height
 *       on reference image 3. On a capture the two agree to about a level.</li>
 *   <li><b>All ink counts, not only the two body components.</b> Tried both. Counting
 *       only the two largest components drops the foe's hair mass on the capture —
 *       measured as a <em>separate</em> 4,484 px component on {@code s4-p2-parry-contact}
 *       frame 11, which is itself worth knowing — and drops the ink smoke between the
 *       heads on the reference. Both are marks the eye uses to separate the figures, so
 *       both count. The two body centroids are used only to bound the search window.</li>
 * </ol>
 */
public final class CorridorProfile {

    /**
     * Half-width of the strip sampled for the row background, in pixels.
     *
     * <p>70 rather than {@link Paper#STRIP}'s 16 because reference image 3 is 832 px
     * wide and its duellists' smoke reaches within about 30 px of the frame edge; a
     * median over 70 columns per side rejects it. Measured invariant: the reference's
     * torso reading moves from 0.0149 to 0.0149 between strips of 40 and 100.
     */
    public static final int BACKGROUND_STRIP = 70;

    /** A component must hold this share of the frame's ink to be a body. Same number as {@link Duellists#BODY_SHARE}. */
    public static final double BODY_SHARE = Duellists.BODY_SHARE;

    /**
     * The five bands, as fractions of the figure's own height from the top of the head.
     *
     * <p>Read off reference image 3's anatomy at the figure span the review of System 4
     * pass 2 recorded (head y283 to feet y955, 673 px): heads to y404, torsos and the
     * two hands on their hilts to y560, sashes to y700, skirts to y880, feet below.
     * Converted to fractions so the same table applies to a 399 px figure in a capture.
     */
    public static final List<Band> BANDS = List.of(
            new Band("head", 0.00, 0.18),
            new Band("torso", 0.18, 0.41),
            new Band("sash", 0.41, 0.62),
            new Band("skirt", 0.62, 0.89),
            new Band("feet", 0.89, 1.00));

    /**
     * The acceptance floor per band, as a fraction of figure height.
     *
     * <h2>Every one of these is reference image 3's own reading, rounded down</h2>
     *
     * <p>Measured with this class, {@code inkFactor} 0.85, span y283..955, window
     * x283..629 (the two bodies' ink centroids):
     *
     * <pre>
     *   head 0.0936   torso 0.0149   sash 0.0921   skirt 0.1010   feet 0.1129
     * </pre>
     *
     * <p>and swept for stability. Across ink factors 0.60 to 0.90 and across six
     * plausible figure spans, {@code torso} moves 0.0146-0.0178, {@code sash}
     * 0.0894-0.0998 and {@code skirt} 0.0991-0.1070 — those three are the load-bearing
     * numbers. {@code head} is stable at 0.085-0.094 from factor 0.85 up and runs to
     * 0.284 at 0.60, and {@code feet} runs 0.070-0.394; both are reported and both get
     * a floor set at the <em>bottom</em> of their own sweep rather than at the central
     * reading, because a threshold justified by a number that moves 4× under its own
     * nuisance parameters cannot be allowed to fail anything at its central value.
     *
     * <p>STYLE.md §11.2b's generalised control — "before a number is allowed to decide
     * anything, someone must say what it would read if the thing being measured were
     * absent" — is discharged twice in {@code CorridorProfileTest}: on a synthetic
     * frame whose gap is known analytically, and on the reference itself.
     */
    public static final Map<String, Double> FLOORS = floors();

    private static Map<String, Double> floors() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("head", 0.080);
        m.put("torso", 0.014);
        m.put("sash", 0.085);
        m.put("skirt", 0.095);
        m.put("feet", 0.065);
        return java.util.Collections.unmodifiableMap(m);
    }

    /** One band of a figure: a name and where it starts and ends, top-down. */
    public record Band(String name, double from, double to) {
    }

    /**
     * @param band     which band
     * @param rect     the rectangle the number was taken through. STYLE.md §11.3.
     * @param columns  widest run of columns in {@code rect} carrying no ink at all
     * @param at       x of the left end of that run, or -1
     * @param fraction {@code columns} over the figure height
     * @param floor    the acceptance this band is held to
     */
    public record Reading(String band, Rect rect, int columns, int at, double fraction, double floor) {

        public boolean pass() {
            return fraction >= floor;
        }

        public String describe() {
            return String.format(java.util.Locale.ROOT,
                    "  %-6s %-26s %4d px = %.4f  (floor %.3f)  %s%s",
                    band, rect.describe(), columns, fraction, floor, pass() ? "pass" : "MISS",
                    at < 0 ? "" : String.format(java.util.Locale.ROOT, "   run starts x=%d", at));
        }
    }

    /**
     * @param span     the figure box the bands were cut from, and the height every
     *                 fraction is against
     * @param left     ink centroid of the left body
     * @param right    ink centroid of the right body
     * @param merged   true when fewer than two ink components reach {@link #BODY_SHARE}
     */
    public record Profile(Rect span, int left, int right, List<Reading> readings, boolean merged) {

        public boolean pass() {
            if (merged) {
                return false;
            }
            for (Reading r : readings) {
                if (!r.pass()) {
                    return false;
                }
            }
            return true;
        }

        public Reading band(String name) {
            for (Reading r : readings) {
                if (r.band().equals(name)) {
                    return r;
                }
            }
            return null;
        }

        public String describe() {
            if (merged) {
                return "  the two bodies are one connected ink component: no corridor exists\n";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(java.util.Locale.ROOT,
                    "  figure %s (height %d)   window between body centroids x%d..x%d%n",
                    span.describe(), span.h, left, right));
            for (Reading r : readings) {
                sb.append(r.describe()).append('\n');
            }
            return sb.toString();
        }
    }

    private CorridorProfile() {
    }

    // -- the instrument --------------------------------------------------------

    /**
     * The row-local background: for each row, the median luminance of the leftmost
     * and rightmost {@link #BACKGROUND_STRIP} columns.
     *
     * <p>This is the one change that lets a capture and a painting be measured by the
     * same command. {@link Paper} estimates a single modal level off a border ring,
     * which is exactly right for a sheet of paper and wrong for a sky that runs from
     * luminance 57 at the zenith to 97 at the horizon and back to 29 at the ground.
     */
    public static double[] rowBackground(Frame f, int strip) {
        int s = Math.max(1, Math.min(strip, f.width / 2));
        double[] bg = new double[f.height];
        double[] row = new double[2 * s];
        for (int y = 0; y < f.height; y++) {
            int n = 0;
            for (int x = 0; x < s; x++) {
                row[n++] = f.lum[y * f.width + x];
                row[n++] = f.lum[y * f.width + (f.width - 1 - x)];
            }
            double[] copy = java.util.Arrays.copyOf(row, n);
            java.util.Arrays.sort(copy);
            bg[y] = copy[n / 2];
        }
        return bg;
    }

    /**
     * Ink, against the row background, opened 3x3.
     *
     * <p>The opening is the pass-1 reviewer's and it matters: without it a single
     * stray dark pixel from the paper tooth closes a corridor, and the statistic
     * becomes a measurement of the noise floor. {@link Duellists#inkComponents}
     * deliberately does <em>not</em> open, because a refusal has to agree with what
     * the eye calls one mass; a corridor is the opposite question.
     */
    public static boolean[] inkMask(Frame f, double factor, int strip) {
        double[] bg = rowBackground(f, strip);
        boolean[] raw = new boolean[f.width * f.height];
        for (int y = 0; y < f.height; y++) {
            double th = factor * bg[y];
            for (int x = 0; x < f.width; x++) {
                int i = y * f.width + x;
                raw[i] = f.lum[i] < th;
            }
        }
        return open3x3(raw, f.width, f.height);
    }

    private static boolean[] open3x3(boolean[] m, int w, int h) {
        boolean[] eroded = new boolean[m.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean all = true;
                for (int dy = -1; dy <= 1 && all; dy++) {
                    for (int dx = -1; dx <= 1 && all; dx++) {
                        int nx = x + dx, ny = y + dy;
                        all = nx >= 0 && ny >= 0 && nx < w && ny < h && m[ny * w + nx];
                    }
                }
                eroded[y * w + x] = all;
            }
        }
        boolean[] out = new boolean[m.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean any = false;
                for (int dy = -1; dy <= 1 && !any; dy++) {
                    for (int dx = -1; dx <= 1 && !any; dx++) {
                        int nx = x + dx, ny = y + dy;
                        any = nx >= 0 && ny >= 0 && nx < w && ny < h && eroded[ny * w + nx];
                    }
                }
                out[y * w + x] = any;
            }
        }
        return out;
    }

    /** One 8-connected component of the opened ink mask. */
    private record Blob(int pixels, int x0, int y0, int x1, int y1, double centroidX) {
    }

    private static List<Blob> blobs(boolean[] ink, int w, int h) {
        List<Blob> out = new ArrayList<>();
        boolean[] seen = new boolean[ink.length];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int start = 0; start < ink.length; start++) {
            if (!ink[start] || seen[start]) {
                continue;
            }
            q.clear();
            q.add(start);
            seen[start] = true;
            int n = 0;
            long sumX = 0;
            int x0 = start % w, x1 = x0, y0 = start / w, y1 = y0;
            while (!q.isEmpty()) {
                int p = q.poll();
                n++;
                int x = p % w, y = p / w;
                sumX += x;
                if (x < x0) x0 = x;
                if (x > x1) x1 = x;
                if (y < y0) y0 = y;
                if (y > y1) y1 = y;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                            continue;
                        }
                        int i = ny * w + nx;
                        if (ink[i] && !seen[i]) {
                            seen[i] = true;
                            q.add(i);
                        }
                    }
                }
            }
            out.add(new Blob(n, x0, y0, x1, y1, sumX / (double) n));
        }
        out.sort(Comparator.comparingInt(Blob::pixels).reversed());
        return out;
    }

    /**
     * Measures the profile.
     *
     * @param span the figure box the bands are cut from, or null to take the bounding
     *             box of the largest ink component. <b>Pass it explicitly on a frame
     *             where the figure dissolves into the ground</b> — which reference
     *             image 3 does, exactly as STYLE.md §3 says a figure should, so its
     *             largest component runs from the head to the bottom of the frame and
     *             an automatic figure height is 800 px rather than 673. The span is
     *             printed with every number, per §11.3.
     */
    public static Profile measure(Frame f, double inkFactor, int strip, Rect span) {
        boolean[] ink = inkMask(f, inkFactor, strip);
        List<Blob> parts = blobs(ink, f.width, f.height);
        long total = 0;
        for (Blob b : parts) {
            total += b.pixels;
        }
        List<Blob> bodies = new ArrayList<>();
        for (Blob b : parts) {
            if (total > 0 && b.pixels >= BODY_SHARE * total) {
                bodies.add(b);
            }
        }
        Rect box = span;
        if (box == null) {
            box = parts.isEmpty() ? new Rect(0, 0, 0, 0)
                    : Rect.ofCorners(parts.get(0).x0, parts.get(0).y0, parts.get(0).x1, parts.get(0).y1);
        }
        if (bodies.size() < 2) {
            return new Profile(box, 0, 0, List.of(), true);
        }
        bodies.sort(Comparator.comparingDouble(Blob::centroidX));
        int left = (int) Math.round(bodies.get(0).centroidX);
        int right = (int) Math.round(bodies.get(bodies.size() - 1).centroidX);

        List<Reading> readings = new ArrayList<>();
        int y0 = box.y;
        int height = box.h;
        for (Band band : BANDS) {
            int by0 = (int) Math.round(y0 + band.from() * height);
            int by1 = Math.min(box.y1(), (int) Math.round(y0 + band.to() * height) - 1);
            Rect rect = Rect.ofCorners(left, Math.max(0, by0), right, Math.min(f.height - 1, by1));
            int best = 0;
            int run = 0;
            int cur = -1;
            int at = -1;
            for (int x = left; x <= right && x < f.width; x++) {
                boolean clear = true;
                for (int y = rect.y; y <= rect.y1() && clear; y++) {
                    clear = !ink[y * f.width + x];
                }
                if (clear) {
                    if (run == 0) {
                        cur = x;
                    }
                    run++;
                    if (run > best) {
                        best = run;
                        at = cur;
                    }
                } else {
                    run = 0;
                }
            }
            readings.add(new Reading(band.name(), rect, best, at,
                    height <= 0 ? Double.NaN : best / (double) height,
                    FLOORS.getOrDefault(band.name(), 0.0)));
        }
        return new Profile(box, left, right, List.copyOf(readings), false);
    }

    /**
     * Median ink luminance inside {@code rect}, as a fraction of the background at
     * those rows.
     *
     * <p>The statistic Family B's composition is graded on: STYLE.md §1 has "bodies
     * read as near-black ink silhouettes" and the corpus draws a <em>dark</em> duellist
     * against a <em>pale</em> one. Expressed against the frame's own ground so a
     * capture on cream paper and a painting against a dusk sky are comparable, which is
     * the same reason {@link #rowBackground} exists.
     *
     * <p>Delivered pixels, deliberately. {@code DirectorTest.bothFiguresAreVisuallyDistinguishable}
     * asserts on {@code InkMaterial}'s base colour, and a base colour is not a picture —
     * it passed green through the whole regression the pass-2 review measured.
     *
     * @return NaN when the rectangle holds no ink at all
     */
    public static double medianInkOverGround(Frame f, Rect rect, double inkFactor, int strip) {
        double[] bg = rowBackground(f, strip);
        Rect r = rect.clamp(f);
        java.util.List<Double> ink = new ArrayList<>();
        double groundSum = 0;
        int rows = 0;
        for (int y = r.y; y <= r.y1(); y++) {
            groundSum += bg[y];
            rows++;
            double th = inkFactor * bg[y];
            for (int x = r.x; x <= r.x1(); x++) {
                double l = f.lum[y * f.width + x];
                if (l < th) {
                    ink.add(l);
                }
            }
        }
        if (ink.isEmpty() || rows == 0) {
            return Double.NaN;
        }
        java.util.Collections.sort(ink);
        return ink.get(ink.size() / 2) / (groundSum / rows);
    }

    /** The whole-figure scalar the first two passes were graded on, for comparison. */
    public static double wholeColumn(Frame f, double inkFactor, int strip, Rect span) {
        Profile p = measure(f, inkFactor, strip, span);
        if (p.merged()) {
            return 0.0;
        }
        boolean[] ink = inkMask(f, inkFactor, strip);
        Rect box = p.span();
        int best = 0;
        int run = 0;
        for (int x = p.left(); x <= p.right() && x < f.width; x++) {
            boolean clear = true;
            for (int y = Math.max(0, box.y); y <= Math.min(f.height - 1, box.y1()) && clear; y++) {
                clear = !ink[y * f.width + x];
            }
            run = clear ? run + 1 : 0;
            best = Math.max(best, run);
        }
        return box.h <= 0 ? Double.NaN : best / (double) box.h;
    }
}
