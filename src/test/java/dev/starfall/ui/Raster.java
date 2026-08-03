package dev.starfall.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The coverage field the interface's triangles would actually print, computed on
 * the CPU.
 *
 * <h2>Why a second rasteriser exists in the test source</h2>
 *
 * <p>STYLE.md 3's first rule -- <i>"nothing in this game has a hard edge except the
 * blades"</i> -- is a statement about <b>pixels</b>, and pass 1 certified it with a
 * statement about <b>vertices</b>: every triangle has a corner at alpha zero. A
 * reviewer then wrote a triangle of the form {@code (0, a, a)}, which satisfies that
 * and prints a hard edge along the {@code a}-{@code a} side, and drew a bordered HUD
 * panel that passed the whole suite. STYLE.md 11.2b(f)'s closing rule is the lesson:
 * <i>"a guard that carries a broad claim owes a second exhibit: the adversarial
 * instance."</i>
 *
 * <p>A claim about edges therefore has to be measured where an edge exists, which is
 * on a raster. This class is that raster, and it is deliberately <em>not</em>
 * {@link InkUiRenderer}: it is a second implementation of the same compositing rule,
 * so an assertion made through it crosses an implementation boundary rather than a
 * call site (STYLE.md 11.2b(c)). It is also not anti-aliased, on purpose -- a
 * measurement of how steeply a mark arrives must not be softened by the instrument.
 *
 * <p>Compositing is premultiplied source-over of a single ink value, in emission
 * order, which is what {@code shaders/ink_fx} does to a constant colour: the field is
 * the alpha the frame would carry if the interface were the only thing drawn. That is
 * exactly the quantity the review measured as {@code live - bare}.
 */
final class Raster {

    final int width;
    final int height;
    final float[] alpha;

    Raster(int width, int height) {
        this.width = width;
        this.height = height;
        this.alpha = new float[width * height];
    }

    /**
     * Paints a recording into the field.
     *
     * @param scale  pixels per unit of the recording's own space
     * @param originX the recording-space x that lands on pixel column 0
     * @param originY the recording-space y that lands on the <em>bottom</em> row
     */
    void paint(Recorder rec, float scale, float originX, float originY) {
        for (Recorder.Triangle t : rec.triangles) {
            Recorder.Vertex a = rec.vertices.get(t.a());
            Recorder.Vertex b = rec.vertices.get(t.b());
            Recorder.Vertex c = rec.vertices.get(t.c());
            fill(px(a.x(), scale, originX), py(a.y(), scale, originY), a.alpha(),
                    px(b.x(), scale, originX), py(b.y(), scale, originY), b.alpha(),
                    px(c.x(), scale, originX), py(c.y(), scale, originY), c.alpha());
        }
    }

    private static float px(float x, float scale, float originX) {
        return (x - originX) * scale;
    }

    private float py(float y, float scale, float originY) {
        return height - (y - originY) * scale;
    }

    /**
     * One triangle, composited with the <b>top-left fill rule</b>.
     *
     * <p>The rule is not a nicety: without it a pixel that lands exactly on an edge
     * two triangles share is composited twice, and two coats of a 0.30 wash print a
     * 0.51 line one pixel wide -- which is a hard edge <em>manufactured by the
     * instrument</em>, in a measurement whose entire purpose is to find hard edges.
     * It is also what GL does, so the field this class produces is the field the
     * renderer draws rather than a harsher relative of it.
     *
     * <p><b>And the edge functions are in double.</b> The lane's washes are fans of
     * very thin slivers -- at the intimate framing a reach pool is 127 px long and
     * 15 px tall, cut into 28 wedges -- and in {@code float} the edge tests on a
     * sliver disagree with the neighbouring sliver's by one ulp often enough to leave
     * <b>single-pixel holes</b>. Measured: one such hole read 0.15 between neighbours
     * at 0.39, and it was the steepest "edge" in the whole interface. An instrument
     * that manufactures the artefact it is looking for is worse than no instrument,
     * and this is the second way that happened here -- see the fill rule above.
     */
    private void fill(float x0, float y0, float a0, float x1, float y1, float a1,
                      float x2, float y2, float a2) {
        double area = (double) (x1 - x0) * (y2 - y0) - (double) (x2 - x0) * (y1 - y0);
        if (Math.abs(area) < 1e-9) {
            return;
        }
        if (area < 0) {
            float tx = x1;
            float ty = y1;
            float ta = a1;
            x1 = x2;
            y1 = y2;
            a1 = a2;
            x2 = tx;
            y2 = ty;
            a2 = ta;
            area = -area;
        }
        boolean in0 = topLeft(x1 - x0, y1 - y0);
        boolean in1 = topLeft(x2 - x1, y2 - y1);
        boolean in2 = topLeft(x0 - x2, y0 - y2);
        int lo = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
        int hi = Math.min(width - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int bot = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
        int top = Math.min(height - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
        for (int py = bot; py <= top; py++) {
            double sy = py + 0.5;
            for (int pxi = lo; pxi <= hi; pxi++) {
                double sx = pxi + 0.5;
                double e0 = (double) (x1 - x0) * (sy - y0) - (double) (y1 - y0) * (sx - x0);
                double e1 = (double) (x2 - x1) * (sy - y1) - (double) (y2 - y1) * (sx - x1);
                double e2 = (double) (x0 - x2) * (sy - y2) - (double) (y0 - y2) * (sx - x2);
                if (e0 < 0 || e1 < 0 || e2 < 0) {
                    continue;
                }
                if ((e0 == 0 && !in0) || (e1 == 0 && !in1) || (e2 == 0 && !in2)) {
                    continue;
                }
                double w2 = e0 / area;
                double w0 = e1 / area;
                double w1 = e2 / area;
                float v = (float) (w0 * a0 + w1 * a1 + w2 * a2);
                if (v <= 0f) {
                    continue;
                }
                if (v > 1f) {
                    v = 1f;
                }
                int i = py * width + pxi;
                alpha[i] = alpha[i] * (1f - v) + v;
            }
        }
    }

    /** Whether a pixel exactly on this edge belongs to this triangle rather than its neighbour. */
    private static boolean topLeft(float dx, float dy) {
        return dy > 0f || (dy == 0f && dx < 0f);
    }

    float peak() {
        float best = 0f;
        for (float v : alpha) {
            best = Math.max(best, v);
        }
        return best;
    }

    /**
     * The largest change between two pixels that touch, in either axis, ignoring a
     * margin at the frame border.
     *
     * <p>The margin is the one qualification the pass-1 review recorded and it is
     * kept in the same words: a mark the viewport cuts is <i>"a mark cut by the
     * viewport, which the ground and sky also are; it is not a printed boundary"</i>.
     * The lane's pools reach the frame edge at the intimate framing and are sliced
     * there by the lens, not by the brush.
     */
    Step steepestStep(int margin) {
        Step worst = new Step(0f, 0, 0);
        for (int y = margin; y < height - margin; y++) {
            for (int x = margin; x < width - margin; x++) {
                float v = alpha[y * width + x];
                if (x + 1 < width - margin) {
                    float d = Math.abs(alpha[y * width + x + 1] - v);
                    if (d > worst.size()) {
                        worst = new Step(d, x, y);
                    }
                }
                if (y + 1 < height - margin) {
                    float d = Math.abs(alpha[(y + 1) * width + x] - v);
                    if (d > worst.size()) {
                        worst = new Step(d, x, y);
                    }
                }
            }
        }
        return worst;
    }

    /** How steep a step is, and where it is -- STYLE.md 11.3 wants the place with the number. */
    record Step(float size, int x, int y) {
    }

    /** How wide an inked block is, which axis limits it, and where it is. */
    record Run(int length, boolean horizontal, int x, int y) {
    }

    /**
     * <b>The largest inked block: the interior of a filled region, measured.</b>
     *
     * <p>For every pixel, the length of the unbroken inked run it lies in along each
     * axis; the statistic is the largest {@code min(horizontal, vertical)} anywhere
     * in the field. It is the side of the largest square of solid ink the picture
     * contains.
     *
     * <h2>What it is for</h2>
     *
     * <p>This is the criterion {@code system5-debt.md} 1.3 said nobody had -- <i>"a
     * ban on a <b>shape</b>, and nothing in this project tests for a shape"</i> --
     * and which the pass-2 review then defeated the entire suite with, by hatching a
     * filled HUD panel out of legal {@link Brush} strokes until it read 0.0515 of its
     * amplitude in one pixel, seven times <em>softer</em> than the interface it was
     * hiding in.
     *
     * <p>What a filled region has that a mark does not is an interior in <b>both</b>
     * directions at once. A stroke is a ridge: cut across it and the ink ends within
     * a stroke width. A wash lying on the ground is a lens: long one way, thin the
     * other. A panel -- feathered, hatched, or assembled from ten thousand legal
     * brush strokes -- is thick both ways over hundreds of pixels, and nothing done
     * to its rim changes that, because the defect is its middle.
     *
     * <h2>Why thickness and not the flatness the review asked for</h2>
     *
     * <p>The pass-2 review proposed <i>"no axis-aligned run of near-constant coverage
     * longer than N px, on either axis"</i>. Built and measured, that form fails
     * twice. On <b>one</b> axis it convicts the interface's own lane: a wash seen
     * edge-on is 129 px of near-constant coverage across and about thirty tall, and
     * that is a wash, which STYLE.md 8 asks for by name. And <b>near-constant</b> is
     * the wrong predicate: the review's own hatch ripples at its stroke pitch, so at
     * a band of 0.06 of amplitude its largest near-constant block measures <b>23 px
     * against the interface's own 13</b> -- a margin of 1.8x, which a hatch tuned to
     * ripple a little harder would close entirely. Thickness has no such dial: the
     * same hatch measures <b>247 px against the interface's 27</b>.
     *
     * @param ink the share of the field's amplitude below which a pixel is paper
     */
    Run inkBlock(float amplitude, float ink, int margin) {
        float floor = ink * amplitude;
        int[] h = new int[width * height];
        int[] v = new int[width * height];
        for (int y = margin; y < height - margin; y++) {
            segment(h, y, margin, width - margin, true, floor);
        }
        for (int x = margin; x < width - margin; x++) {
            segment(v, x, margin, height - margin, false, floor);
        }
        Run best = new Run(0, true, 0, 0);
        for (int y = margin; y < height - margin; y++) {
            for (int x = margin; x < width - margin; x++) {
                int i = y * width + x;
                int side = Math.min(h[i], v[i]);
                if (side > best.length()) {
                    best = new Run(side, h[i] <= v[i], x, y);
                }
            }
        }
        return best;
    }

    /** Cuts one line into unbroken inked runs and writes each pixel's own run length. */
    private void segment(int[] out, int line, int from, int to, boolean horizontal, float floor) {
        int start = from;
        for (int i = from; i <= to; i++) {
            boolean inked = i < to && at(i, horizontal, line) >= floor;
            if (inked) {
                continue;
            }
            int len = i - start;
            for (int k = start; k < i; k++) {
                out[horizontal ? line * width + k : k * width + line] = len;
            }
            start = i + 1;
        }
    }

    private float at(int i, boolean horizontal, int line) {
        return horizontal ? alpha[line * width + i] : alpha[i * width + line];
    }

    /**
     * The runs of ink along one row: where a counted mark is counted.
     *
     * <p>A run opens when the field rises above {@code floor} and closes when it falls
     * back below it, so this is the same reader a player's eye is -- it separates two
     * marks only when there is genuinely paper between them.
     */
    List<int[]> runs(int row, int fromX, int toX, float floor) {
        List<int[]> out = new ArrayList<>();
        int start = -1;
        for (int x = fromX; x <= toX; x++) {
            boolean on = alpha[row * width + x] > floor;
            if (on && start < 0) {
                start = x;
            } else if (!on && start >= 0) {
                out.add(new int[] {start, x - 1});
                start = -1;
            }
        }
        if (start >= 0) {
            out.add(new int[] {start, toX});
        }
        return out;
    }

    /** The row with the most ink in a band -- the one a counting eye would use. */
    int densestRow(int fromX, int toX, int fromY, int toY) {
        int best = fromY;
        double most = -1;
        for (int y = fromY; y <= toY; y++) {
            double sum = 0;
            for (int x = fromX; x <= toX; x++) {
                sum += alpha[y * width + x];
            }
            if (sum > most) {
                most = sum;
                best = y;
            }
        }
        return best;
    }

    /**
     * How different two marks are as <em>pictures</em>: the ink that does not
     * coincide, over the ink there is.
     *
     * <p>0 means the same picture; 1 means two marks that share no paper. Normalised
     * by the ink rather than by the box, because a mean over the whole cartouche
     * makes every sparse mark look like every other sparse mark -- a Feint and its
     * own mirror measured 0.0037 apart on that statistic while being opposite
     * diagonals, which is the metric failing, not the marks.
     */
    static double distance(Raster a, Raster b) {
        double diff = 0;
        double ink = 0;
        for (int i = 0; i < a.alpha.length; i++) {
            diff += Math.abs(a.alpha[i] - b.alpha[i]);
            ink += Math.max(a.alpha[i], b.alpha[i]);
        }
        return ink <= 1e-9 ? 0 : diff / ink;
    }
}
