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
     * One image of the corpus that depicts the situation this criterion measures.
     *
     * <p>STYLE.md §11.0, added after pass 3 built the criterion and calibrated it on
     * one painting: <i>"show it on <b>every image in the family</b> that depicts the
     * situation being measured, and name the ones you excluded and why."</i> Family B
     * is images 3, 4 and 5 and all three are two duellists with crossed blades, so all
     * three are here. Images 1 and 2 are Family A (one figure, no corridor to measure)
     * and 6, 7 and 8 are Family C. One of the three, image 5, is excluded from setting
     * the band and {@link #note} says exactly why in the words of its own content;
     * {@code CorridorProfileTest} asserts the reason still holds rather than trusting it.
     *
     * <p>Each carries its own span because each image frames its figures differently,
     * and §11.3's rule that the number is printed beside its rectangle applies to the
     * normaliser as much as to the count.
     */
    public record Reference(String name, java.io.File file, Rect span, boolean measurable, String note) {
    }

    /** Family B, all of it, with the one exclusion named per STYLE.md §11.0. */
    public static final List<Reference> FAMILY_B = List.of(
            new Reference("image 3", new java.io.File("inspirations/image - 2026-08-02T101033.164.png"),
                    Rect.ofCorners(0, 283, 831, 955), true, ""),
            new Reference("image 4", new java.io.File("inspirations/image - 2026-08-02T101128.842.png"),
                    Rect.ofCorners(0, 255, 831, 930), true, ""),
            new Reference("image 5", new java.io.File("inspirations/image - 2026-08-02T101232.595.png"),
                    Rect.ofCorners(0, 255, 831, 930), false,
                    "EXCLUDED, and named rather than dropped (STYLE.md 11.0). It sets no floor "
                            + "either way, and System 4 pass 5 corrected the reason.\n"
                            + "  At the operating factor 0.85 its two duellists are ONE connected "
                            + "ink component inside their own figure span, holding 99.3% of that "
                            + "span's ink: each carries a second sheathed blade whose scabbard "
                            + "crosses the gap at hip height, and their hilts touch. An image with "
                            + "no corridor cannot set a corridor floor.\n"
                            + "  The previous note said 'at every threshold tried', and that is not "
                            + "so. Sweeping the ink factor with the span crop in place, two genuine "
                            + "duellists resolve at 0.40 (component shares 0.554 / 0.313), 0.45 "
                            + "(0.546 / 0.362) and 0.50 (0.545 / 0.371), and it is one mass from "
                            + "0.55 up. At those factors its torso corridor reads exactly 0.0000 -- "
                            + "the number the pass-3 review quoted -- and it reads it between the "
                            + "two BODIES, not between a duellist and the ground. So the corpus "
                            + "does contain a two-duellist painting whose torso corridor is "
                            + "literally zero, because their hilts touch. The exclusion is right; "
                            + "the overstatement was not. Either way image 5 sets no floor: one "
                            + "mass at the operating factor, and 0.0000 at every factor at which "
                            + "the two masses separate.\n"
                            + "  It DID appear to set one. Before the component analysis was "
                            + "cropped to the figure span, image 5 resolved 'two bodies' whose "
                            + "second component was the ground smear below the feet, and the "
                            + "readings taken through that window -- head 0.0799, torso 0.0000, "
                            + "sash 0.0488, skirt 0.0725, feet 0.0814 -- are the numbers the "
                            + "pass-3 review used to argue the criterion was fitted to one image. "
                            + "They were measured between a duellist and the ground. STYLE.md "
                            + "11.3's silent wrong answer, one level up."));

    /**
     * The acceptance <b>band</b> per band, as a fraction of figure height: the corpus's
     * own spread, both edges.
     *
     * <h2>What was wrong with the floors this replaces, in two separate ways</h2>
     *
     * <p><b>(1) They were one image's readings.</b> Pass 3 set them from reference image
     * 3 alone, and the tool's headline — "the criterion, run on the corpus first" — was
     * true of a single hard-coded file. Run on the rest of the family, with this class,
     * ink factor 0.85, each image on its own span:
     *
     * <pre>
     *   band    image 3   image 4   image 5     old floor
     *   head    0.0847    0.1612    0.0799      0.080   image 5 MISSES
     *   torso   0.0149    0.0118    0.0000      0.014   images 4 and 5 MISS
     *   sash    0.0921    0.0976    0.0488      0.085   image 5 MISSES
     *   skirt   0.1010    0.0858    0.0725      0.095   images 4 and 5 MISS
     *   feet    0.1129    0.0444    0.0814      0.065   image 4 MISSES
     * </pre>
     *
     * <p>(That table is the state of the world before this pass corrected two things in
     * the reader: image 5's row is measured between a duellist and the ground smear and
     * is void — see {@link #FAMILY_B} — and image 4's is now taken through a
     * span-cropped window. The band below is set from images 3 and 4 alone and image 5
     * is excluded by name.)
     *
     * <p><b>(2) They were floors only, and a one-sided criterion rewards running away.</b>
     * STYLE.md §11.0: <i>"the highest score in the sweep — 21 of 24 bands passing —
     * belongs to the setting that pushes the skirt gap to 4.19× the corpus and destroys
     * the parry entirely... State the target as a band with both edges, taken from the
     * corpus's own spread."</i> That is what these are. The floor is the corpus minimum
     * rounded down and the ceiling is the corpus maximum rounded up, both to three
     * decimals, with no margin added in either direction — a margin would be a number
     * nobody measured.
     *
     * <p>Measured with this class at ink factor 0.85, each image on its own span, the
     * component analysis cropped to that span:
     *
     * <pre>
     *   band    image 3   image 4   band adopted
     *   head    0.0847    0.1612    0.084 .. 0.162
     *   torso   0.0149    0.0118    0.011 .. 0.015
     *   sash    0.0921    0.0976    0.092 .. 0.098
     *   skirt   0.1010    0.0858    0.085 .. 0.102
     *   feet    0.1129    0.0444    0.044 .. 0.113
     * </pre>
     *
     * <p><b>Two samples is a thin corpus and the {@code sash} band shows it</b>: 0.092
     * to 0.098 is a 6% window and nothing but two paintings stands behind it. That is
     * stated rather than padded, because a margin added here would be a number nobody
     * measured — which is the failure this whole class is a correction of. What makes
     * it usable is that the capture misses these bands by factors of three to four, not
     * by percent.
     *
     * <p>The {@code torso} <em>ceiling</em> of 0.015 is the binding constraint on that
     * band, and it is the right way round: two duellists in a bind have their hands
     * nearly touching, so a wide gap at the hands is the defect and a narrow one is the
     * beat.
     */
    public static final Map<String, double[]> ACCEPT = accept();

    private static Map<String, double[]> accept() {
        Map<String, double[]> m = new LinkedHashMap<>();
        m.put("head", new double[] {0.084, 0.162});
        m.put("torso", new double[] {0.011, 0.015});
        m.put("sash", new double[] {0.092, 0.098});
        m.put("skirt", new double[] {0.085, 0.102});
        m.put("feet", new double[] {0.044, 0.113});
        return java.util.Collections.unmodifiableMap(m);
    }

    /** The lower edge of {@link #ACCEPT}. */
    public static final Map<String, Double> FLOORS = edge(0);

    /** The upper edge of {@link #ACCEPT}. */
    public static final Map<String, Double> CEILINGS = edge(1);

    private static Map<String, Double> edge(int i) {
        Map<String, Double> m = new LinkedHashMap<>();
        ACCEPT.forEach((k, v) -> m.put(k, v[i]));
        return java.util.Collections.unmodifiableMap(m);
    }

    /**
     * A second ink threshold, held fixed, quoted beside every reading.
     *
     * <p>The pass-3 review's finding, and the reason this exists: pass 3 reported the
     * corridor improving from 8 to 11 frames, and <b>none of it was geometry</b> — the
     * {@code skirt} and {@code feet} bands were bit-identical to pass 2 on all 24
     * frames, and the three frames that flipped flipped on {@code torso} alone, at
     * columns inside the pale duellist's own silhouette, on pixels that did not move
     * but got brighter. A threshold-based corridor widens when a figure is lightened.
     *
     * <p>0.60 of the row's own background counts only ink that is <em>strongly</em>
     * dark, so a passage lifted from 0.80 to 0.90 of background changes the 0.85
     * reading and not this one. It is a diagnostic and not a second acceptance: the two
     * numbers moving together is geometry, one moving without the other is photometry,
     * and the pair is printed so the next reader can tell which happened without
     * re-shooting the previous pass.
     */
    public static final double FIXED_FACTOR = 0.60;

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
    public record Reading(String band, Rect rect, int columns, int at, double fraction,
                          double floor, double ceiling, int fixedColumns, double fixedFraction) {

        /**
         * STYLE.md §11.0: the acceptance is a band with both edges, taken from the
         * corpus's own spread. A capture can now fail for being too <em>wide</em>,
         * which is what the pass-3 review measured as the real error and what a
         * floors-only criterion could not express.
         */
        public boolean pass() {
            return fraction >= floor && fraction <= ceiling;
        }

        public String describe() {
            String verdict = pass() ? "pass" : fraction < floor ? "MISS low" : "MISS high";
            return String.format(java.util.Locale.ROOT,
                    "  %-6s %-26s %4d px = %.4f  (%.3f..%.3f)  %-9s  [fixed %.2f: %4d px = %.4f]%s",
                    band, rect.describe(), columns, fraction, floor, ceiling, verdict,
                    FIXED_FACTOR, fixedColumns, fixedFraction,
                    at < 0 ? "" : String.format(java.util.Locale.ROOT, "  run starts x=%d", at));
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
        boolean[] fixedInk = inkMask(f, FIXED_FACTOR, strip);
        // <b>Ink below the figure's own feet is ground, not a duellist.</b> When a span
        // is given, the component analysis is cropped to it -- which is what decides
        // both the "one mass" verdict and the two centroids the window is drawn
        // between. Without the crop, the Family B ground of STYLE.md 1 ("a dark ink
        // smear") is itself ink, it runs the full width of the sheet, and both figures
        // stand in it: measured on {@code s4-p4-parry-contact}, the largest component
        // spans x0..678 y320..719 and the profile calls 17 of 24 frames one connected
        // mass while the two duellists are plainly separate above the ground.
        //
        // The corpus has always been read this way without anyone saying so: reference
        // image 3's span ends at the feet (y955) with its own smear below it, which is
        // why the same defect never showed there. Cropping makes the two agree by
        // construction rather than by luck. Nothing else about the rule changes -- all
        // ink inside the span still counts, hair and smoke included, per the note above.
        if (span != null) {
            ink = cropToRows(ink, f.width, f.height, span);
            fixedInk = cropToRows(fixedInk, f.width, f.height, span);
        }
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
            int[] run = widestClearRun(ink, f, rect, left, right);
            int[] fixedRun = widestClearRun(fixedInk, f, rect, left, right);
            double[] accept = ACCEPT.getOrDefault(band.name(), new double[] {0.0, 1.0});
            readings.add(new Reading(band.name(), rect, run[0], run[1],
                    height <= 0 ? Double.NaN : run[0] / (double) height,
                    accept[0], accept[1], fixedRun[0],
                    height <= 0 ? Double.NaN : fixedRun[0] / (double) height));
        }
        return new Profile(box, left, right, List.copyOf(readings), false);
    }

    private static boolean[] cropToRows(boolean[] mask, int w, int h, Rect span) {
        boolean[] out = new boolean[mask.length];
        int y0 = Math.max(0, span.y);
        int y1 = Math.min(h - 1, span.y1());
        for (int y = y0; y <= y1; y++) {
            System.arraycopy(mask, y * w, out, y * w, w);
        }
        return out;
    }

    /** {@code {widest run of fully clear columns, where it starts}} inside {@code rect}. */
    private static int[] widestClearRun(boolean[] ink, Frame f, Rect rect, int left, int right) {
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
        return new int[] {best, at};
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

    /**
     * Median luminance of <b>every</b> pixel in {@code rect}, as a fraction of the
     * background at those rows.
     *
     * <h2>Why {@link #medianInkOverGround} had to be joined by a threshold-free one</h2>
     *
     * <p>That statistic takes the median of the pixels <em>darker than
     * {@code inkFactor} x</em> the row background. It is exactly right for a figure
     * that is darker than its ground, which every Family A capture and the corpus's
     * dark duellist are. It is <b>ill-posed for a figure that is brighter than its
     * ground</b> — and on the Family B dusk stage the pale duellist is: measured on
     * {@code s4-p4-parry-contact} frame 11, only 3,671 px of the pale torso box fall
     * below the threshold against 6,884 on the cream-paper capture, so the statistic
     * silently switches from "how pale is this figure" to "how dark is the darkest
     * quarter of it". The delivered ratio it reports drops from 2.12x to 1.20x on a
     * frame where the two duellists are more separated than they have ever been.
     *
     * <p>This one has no threshold, so it cannot change meaning when the figure
     * crosses its ground. Run on the corpus first, per STYLE.md 11.0, through boxes
     * inside each duellist's torso: image 3 reads <b>3.28x</b>, image 4 <b>3.22x</b>,
     * image 5 <b>9.32x</b> — image 5's pale duellist is itself brighter than its sky,
     * at 1.245 of it, which is the case the thresholded statistic cannot express and
     * the corpus contains.
     *
     * @return NaN when the rectangle is empty
     */
    public static double medianOverGround(Frame f, Rect rect, int strip) {
        double[] bg = rowBackground(f, strip);
        Rect r = rect.clamp(f);
        java.util.List<Double> all = new ArrayList<>();
        double groundSum = 0;
        int rows = 0;
        for (int y = r.y; y <= r.y1(); y++) {
            groundSum += bg[y];
            rows++;
            for (int x = r.x; x <= r.x1(); x++) {
                all.add(f.lum[y * f.width + x]);
            }
        }
        if (all.isEmpty() || rows == 0) {
            return Double.NaN;
        }
        java.util.Collections.sort(all);
        return all.get(all.size() / 2) / (groundSum / rows);
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
