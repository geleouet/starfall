package dev.starfall.ui;

import com.badlogic.gdx.graphics.Color;
import dev.starfall.combat.TileType;
import dev.starfall.stage.Framing;
import dev.starfall.stage.Stage;

import dev.starfall.art.Palette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The fields the interface's guards are measured through, in one place.
 *
 * <p>Two spaces, because {@link LaneInterface} draws in two: the margin is in frame
 * heights and the lane is in world units. A statement about how steeply a mark
 * arrives is a statement about <em>pixels</em>, so each has to be rasterised through
 * the mapping the delivered capture uses, not through whichever one is convenient.
 */
final class Guards {

    /** The width the game has been shot at, and the one the captures use. */
    static final int SHIPPED_WIDTH = 960;

    /**
     * The shipped heights. Two of them on purpose: STYLE.md 11.2b(d) makes an
     * absolute pixel statistic valid only against the harness that produced it, and
     * {@code system5-debt.md} conceded that the interface <i>"has never been shot at
     * any resolution but 960x720"</i> while several of its legibility claims are
     * resolution-dependent. A mark that is counted has to be countable at the
     * smallest of these.
     */
    static final int[] SHIPPED_HEIGHTS = {720, 540};

    private Guards() {
    }

    // -- the forbidden thing, so the guards can be pointed at it -----------------

    /**
     * <b>The adversarial instance.</b> The bordered HUD panel the pass-1 reviewer
     * drew into {@code LaneInterface.sheet}, reproduced exactly.
     *
     * <p>STYLE.md 11.2b(f): <i>"a guard that carries a broad claim owes a second
     * exhibit: the adversarial instance. Try to build the thing the guard forbids
     * while satisfying it."</i> This is that thing, and it is checked in rather than
     * described, so no future edit can quietly narrow a guard back to the scope that
     * let it through.
     *
     * <p>It is a filled rectangle in frame heights with <em>one</em> corner at alpha
     * zero, cut as {@code (TL,TR,BR)} + {@code (TL,BR,BL)}. Every triangle therefore
     * has a vertex at exactly zero alpha, which is all pass 1's guard ever asked, and
     * it prints a hard border down three of its four sides.
     */
    static void borderedPanel(Brush.Sink sink) {
        Color ink = Palette.CLOTH_PALE;
        int tl = sink.vertex(0.60f, 0.90f, ink, 0f);
        int tr = sink.vertex(1.00f, 0.90f, ink, 0.95f);
        int br = sink.vertex(1.00f, 0.60f, ink, 0.95f);
        int bl = sink.vertex(0.60f, 0.60f, ink, 0.95f);
        sink.triangle(tl, tr, br);
        sink.triangle(tl, br, bl);
    }

    /**
     * <b>The second adversarial instance, and it is the one that beat pass 2.</b>
     *
     * <p>The pass-2 review built a filled rectangular HUD panel <em>out of nothing
     * but {@code Brush.stroke}</em> -- a schedule of legal, edgeless, zero-rimmed
     * strokes laid side by side until they close -- and it passed all 410 tests. Its
     * parameters are the review's own: strokes of width {@value #HATCH_WIDTH} at a
     * pitch of {@value #HATCH_PITCH}, running {@code x0.51..1.09} across the box
     * {@code x0.60..1.00 y0.60..0.90} in frame heights. Measured through the raster
     * the hard-edge guard uses, it reads <b>0.0515</b> of its own amplitude in one
     * pixel -- <em>seven times softer</em> than anything the interface itself draws
     * -- with zero flat-fill triangles and zero inked silhouette edges.
     *
     * <p>That is the whole finding, and it is why the hard-edge guards cannot be the
     * ones to catch this: <i>"{@code LaneInterface} calling only {@code Brush} is the
     * project's structural argument against chrome, and it is not an argument -- a
     * rectangle is a schedule of legal strokes."</i>
     *
     * <p>Checked in as a fixture beside {@link #borderedPanel} so that the next pass
     * cannot rediscover it, exactly as pass 2 did with the pass-1 reviewer's panel.
     */
    static void hatchPanel(Brush.Sink sink) {
        hatchPanel(sink, HATCH_WIDTH, HATCH_PITCH, 0.45f);
    }

    static void hatchPanel(Brush.Sink sink, float width, float pitch, float alpha) {
        for (float y = 0.60f; y <= 0.90f + 1e-4f; y += pitch) {
            int n = 9;
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                xs[i] = 0.51f + (1.09f - 0.51f) * i / (n - 1);
                ys[i] = y;
            }
            Brush.stroke(sink, xs, ys, width, alpha, Palette.CLOTH_PALE, 4.1f + y * 37f, 0f);
        }
    }

    /** The review's own parameters, kept as constants so the exhibit cannot drift. */
    static final float HATCH_WIDTH = 0.080f;

    static final float HATCH_PITCH = 0.020f;

    // -- the two properties, as predicates over what a sink was handed ------------

    /**
     * Every edge of the emitted mesh that belongs to only one triangle and carries
     * ink at <b>both</b> ends -- that is, every silhouette that is not on paper.
     *
     * <p>This is the property pass 1's guard was <em>named</em> for and did not test.
     * "At least one vertex at zero alpha" is satisfied by {@code (0, a, a)}, whose
     * {@code a}-{@code a} side is a printed boundary; what actually makes
     * {@link Brush} edgeless is that both its rims sit on zero, so every inked edge
     * it emits is <em>interior</em> -- shared with the triangle next to it -- and
     * every edge on the outline of the mark has zero ink at both ends.
     */
    static List<String> inkedSilhouetteEdges(Recorder rec) {
        Map<Long, Integer> count = new HashMap<>();
        for (Recorder.Triangle t : rec.triangles) {
            bump(count, t.a(), t.b());
            bump(count, t.b(), t.c());
            bump(count, t.c(), t.a());
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : count.entrySet()) {
            if (e.getValue() % 2 == 0) {
                continue;
            }
            int u = (int) (e.getKey() >> 32);
            int v = (int) (e.getKey() & 0xffffffffL);
            Recorder.Vertex a = rec.vertices.get(u);
            Recorder.Vertex b = rec.vertices.get(v);
            if (a.alpha() > 0f && b.alpha() > 0f) {
                out.add(String.format(Locale.ROOT, "(%.4f,%.4f)@%.3f -- (%.4f,%.4f)@%.3f",
                        a.x(), a.y(), a.alpha(), b.x(), b.y(), b.alpha()));
            }
        }
        return out;
    }

    private static void bump(Map<Long, Integer> count, int a, int b) {
        long key = a < b ? ((long) a << 32) | (b & 0xffffffffL)
                : ((long) b << 32) | (a & 0xffffffffL);
        count.merge(key, 1, Integer::sum);
    }

    /**
     * The whole interface as one coverage field, in the order it is drawn.
     *
     * <p>Ground first and margin over it, which is what {@code LaneScene} does, so
     * this field is the delivered {@code live - bare} difference the pass-1 review
     * measured -- one picture with one amplitude, rather than two half-measurements
     * each normalised by its own faintest moment.
     */
    static Raster interfaceField(Bout.Staged staged, double t, int w, int h) {
        Raster r = groundField(staged, t, w, h);
        Recorder rec = new Recorder();
        LaneInterface.sheet(rec, staged.readout().at(t), w / (float) h, 0f);
        r.paint(rec, h, 0f, 0f);
        return r;
    }

    /** The margin, rasterised at a shipped size. */
    static Raster sheetField(Bout.Staged staged, double t, int w, int h) {
        return sheetField(staged.readout().at(t), w, h);
    }

    static Raster sheetField(Look look, int w, int h) {
        Recorder rec = new Recorder();
        LaneInterface.sheet(rec, look, w / (float) h, 0f);
        Raster r = new Raster(w, h);
        r.paint(rec, h, 0f, 0f);
        return r;
    }

    /** The lane, rasterised through the framing the schedule is at. */
    static Raster groundField(Bout.Staged staged, double t, int w, int h) {
        Framing f = staged.schedule().framingAt(t);
        double spread = 1.55 * Stage.TILE_WIDTH;
        double worldWidth = f.widthTiles() * 1.55;
        double frameHeight = worldWidth * h / (double) w;
        Recorder rec = new Recorder();
        LaneInterface.ground(rec, staged.readout().at(t), spread, frameHeight);
        Raster r = new Raster(w, h);
        // The eye line of LaneScene at the planning framing, so the lane's marks land
        // where the capture puts them.
        r.paint(rec, (float) (h / frameHeight),
                (float) (f.centreTile() * spread - worldWidth / 2.0),
                (float) (-0.28 * frameHeight));
        return r;
    }

    /** One cartouche alone, at the side a delivered cartouche is drawn at. */
    static Raster glyphField(TileType type, int step, int box) {
        Recorder rec = new Recorder();
        for (Glyph.Stroke s : Glyph.of(type, step)) {
            Brush.stroke(rec, s.xs().clone(), s.ys().clone(), s.width(), 0.95f * s.weight(),
                    Color.WHITE, 3.1f, s.dryness());
        }
        Raster r = new Raster(box, box);
        r.paint(rec, box, -0.5f, -0.5f);
        return r;
    }

    /** A hand of one tile, at a stated cooldown and charge count, and nothing else on the sheet. */
    static Look hand(TileType type, int charges, int cooldown) {
        return new Look(0.0, List.of(),
                List.of(new Look.Held(type, null, charges, cooldown, false)),
                8, 8, 11, 2, 1, List.of(), List.of(), 0.0, 0.0);
    }

    /**
     * The runs of ink a counting eye finds in one tile's charge marks.
     *
     * <p>Read across the row with the most ink in the tick band, at a floor a tenth
     * of the row's own peak -- a mark is separate from its neighbour only when there
     * is paper between them.
     */
    static List<int[]> tickRuns(TileType type, int charges, int cooldown, int w, int h) {
        Raster live = sheetField(hand(type, charges, cooldown), w, h);
        Raster none = sheetField(hand(type, 0, 0), w, h);
        // The difference against the same hand with no cooldown at all: what is left
        // is the charge marks and nothing else, which is the control STYLE.md 11.2b(g)
        // asks for rather than a threshold against whatever the glyph happens to do.
        Raster only = new Raster(w, h);
        float peak = 0f;
        for (int i = 0; i < only.alpha.length; i++) {
            only.alpha[i] = Math.max(0f, live.alpha[i] - none.alpha[i]);
            peak = Math.max(peak, only.alpha[i]);
        }
        if (peak <= 0f) {
            return new ArrayList<>();
        }
        float aspect = w / (float) h;
        int cx = Math.round((aspect - LaneInterface.HAND_INSET) * h);
        int reach = Math.round(5f * LaneInterface.TICK_PITCH * h);
        int left = Math.max(0, cx - reach);
        int right = Math.min(w - 1, cx + reach);
        int mid = Math.round((1f - (LaneInterface.HAND_TOP_Y - LaneInterface.TICK_ROW)) * h);
        int band = Math.round(0.010f * h);
        int row = only.densestRow(left, right, Math.max(0, mid - band),
                Math.min(h - 1, mid + band));
        return only.runs(row, left, right, peak * 0.10f);
    }

    /**
     * The runs of ink in one tile's charge marks <b>on the sheet the bout actually
     * draws</b>, rather than on a sheet holding that tile and nothing else.
     *
     * <p>STYLE.md 11.2b(f): <i>"a guard is a claim about the product, and the product
     * is every configuration it ships in."</i> {@link #tickRuns} answers "is this run
     * separable", and it answers it about a tile alone in an empty hand. A player
     * reads a run with a cartouche beside it and two more cartouches above and below,
     * so this reads the same row out of the whole delivered sheet -- and the control
     * it differences against is the same sheet with that one tile's cooldown set to
     * zero, so what is left is that tile's charge marks and nothing else.
     */
    static List<int[]> tickRunsInHand(Look look, int index, int w, int h) {
        Raster live = sheetField(look, w, h);
        Raster none = sheetField(withoutCharges(look, index), w, h);
        Raster only = new Raster(w, h);
        float peak = 0f;
        for (int i = 0; i < only.alpha.length; i++) {
            only.alpha[i] = Math.max(0f, live.alpha[i] - none.alpha[i]);
            peak = Math.max(peak, only.alpha[i]);
        }
        if (peak <= 0f) {
            return new ArrayList<>();
        }
        float aspect = w / (float) h;
        int cx = Math.round((aspect - LaneInterface.HAND_INSET) * h);
        int reach = Math.round(5f * LaneInterface.TICK_PITCH * h);
        int left = Math.max(0, cx - reach);
        int right = Math.min(w - 1, cx + reach);
        int mid = Math.round((1f - (LaneInterface.HAND_TOP_Y - index * LaneInterface.HAND_PITCH
                - LaneInterface.TICK_ROW)) * h);
        int band = Math.round(0.008f * h);
        int row = only.densestRow(left, right, Math.max(0, mid - band),
                Math.min(h - 1, mid + band));
        return only.runs(row, left, right, peak * 0.10f);
    }

    /** The same sheet with one held tile's cooldown taken off it. */
    private static Look withoutCharges(Look look, int index) {
        List<Look.Held> hand = new ArrayList<>(look.hand());
        Look.Held h = hand.get(index);
        hand.set(index, new Look.Held(h.type(), h.enchantment(), 0, 0, h.banked()));
        return new Look(look.time(), look.stanza(), List.copyOf(hand), look.health(),
                look.maxHealth(), look.laneLength(), look.heroTile(), look.heroStep(),
                look.reached(), look.threatened(), look.bleed(), look.intimacy());
    }

    /** A sheet with health alone on it, at a stated tally. */
    static Look life(int health, int max) {
        return new Look(0.0, List.of(), List.of(), health, max, 11, 2, 1,
                List.of(), List.of(), 0.0, 0.0);
    }

    /** The runs of ink a counting eye finds in the health row. */
    static List<int[]> healthRuns(int health, int max, int w, int h) {
        Raster live = sheetField(life(health, max), w, h);
        Raster none = sheetField(life(0, 0), w, h);
        Raster only = new Raster(w, h);
        float peak = 0f;
        for (int i = 0; i < only.alpha.length; i++) {
            only.alpha[i] = Math.max(0f, live.alpha[i] - none.alpha[i]);
            peak = Math.max(peak, only.alpha[i]);
        }
        if (peak <= 0f) {
            return new ArrayList<>();
        }
        int left = Math.round((LaneInterface.HEALTH_X - LaneInterface.HEALTH_PITCH) * h);
        int right = Math.round((LaneInterface.HEALTH_X + (max + 1) * LaneInterface.HEALTH_PITCH) * h);
        int mid = Math.round((1f - LaneInterface.HEALTH_Y) * h);
        int band = Math.round(0.02f * h);
        int row = only.densestRow(Math.max(0, left), Math.min(w - 1, right),
                Math.max(0, mid - band), Math.min(h - 1, mid + band));
        return only.runs(row, Math.max(0, left), Math.min(w - 1, right), peak * 0.10f);
    }

    static void tickReport(int h) {
        int w = Math.round(h * 4f / 3f);
        for (int cd : new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}) {
            for (int ch : new int[] {0, cd / 2, cd}) {
                List<int[]> runs = tickRuns(TileType.CUT, ch, cd, w, h);
                if (runs.size() != cd) {
                    System.out.printf(Locale.ROOT, "PROBE ticks %dx%d cd=%d charges=%d runs=%d%n",
                            w, h, cd, ch, runs.size());
                }
            }
        }
        for (int max : new int[] {8, 10, 12}) {
            for (int hp = 0; hp <= max; hp++) {
                List<int[]> runs = healthRuns(hp, max, w, h);
                if (runs.size() != max) {
                    System.out.printf(Locale.ROOT, "PROBE health %dx%d max=%d hp=%d runs=%d%n",
                            w, h, max, hp, runs.size());
                }
            }
        }
        System.out.printf(Locale.ROOT, "PROBE counts done at %dx%d%n", w, h);
    }
}
