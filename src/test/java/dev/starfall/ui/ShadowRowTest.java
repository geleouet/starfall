package dev.starfall.ui;

import dev.starfall.combat.EnemyArchetype;
import dev.starfall.direct.Director;
import dev.starfall.stage.Framing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Charted Shadows' strokes: the enemy hit points {@code system5-debt.md} 8
 * called the largest omission, drawn and guarded.
 *
 * <p>Every guard here was observed red during authoring; the break is named in
 * each note.
 */
class ShadowRowTest {

    /**
     * The world heights of the frames the game actually cuts: the intimate shot,
     * the planning floor, and the approach lane's own wide framing -- the row is
     * sized in shares of the frame precisely so these three all deliver the same
     * pixels, and the guard enumerates them rather than trusting the argument.
     */
    private static final double[] FRAME_HEIGHTS = {3.24, 6.58, 16.71};

    /** Rasterises one row alone, centred, at a shipped height and a world framing. */
    private static Raster rowField(Look.Shadow s, double frameHeight, int w, int h) {
        Recorder rec = new Recorder();
        LaneInterface.shadow(rec, s, 0f, (float) frameHeight, 0.0);
        Raster r = new Raster(w, h);
        float scale = (float) (h / frameHeight);
        // x = 0 lands on the middle column; the row's own world y lands mid-frame.
        r.paint(rec, scale, (float) (-(w / 2.0) / scale),
                (float) (LaneInterface.SHADOW_Y - frameHeight / 2.0));
        return r;
    }

    private static List<int[]> rowRuns(Look.Shadow s, double frameHeight, int w, int h) {
        Raster r = rowField(s, frameHeight, w, h);
        float peak = 0f;
        for (float a : r.alpha) {
            peak = Math.max(peak, a);
        }
        if (peak <= 0f) {
            return List.of();
        }
        int cx = w / 2;
        int reach = Math.round((s.maxHp() + 1) * LaneInterface.SHADOW_PITCH * h);
        int mid = h / 2;
        int band = Math.round(0.025f * h);
        int row = r.densestRow(Math.max(0, cx - reach), Math.min(w - 1, cx + reach),
                Math.max(0, mid - band), Math.min(h - 1, mid + band));
        return r.runs(row, Math.max(0, cx - reach), Math.min(w - 1, cx + reach), peak * 0.10f);
    }

    /**
     * A counted mark is countable at every resolution the game ships, for every
     * body the game can put on the lane, at every framing the camera cuts --
     * STYLE.md 8's countability rule with its axes enumerated rather than
     * indexed (MEASUREMENT.md 11.2b(f), fourth clause).
     *
     * <p>The live strokes and the dry ghosts are one count: a row at 2 of 5 must
     * read as five marks, two wet and three dry, because "how much there ever
     * was" is half of what the row says.
     *
     * <p><b>Observed red twice, and the first red was a real defect.</b> The
     * row as first authored gave ghost strokes dryness 0.30, and this guard's
     * first run printed "WISP at 0/3... a player would count 2" at 960x720 --
     * the dry-brush breakup ate a ghost, the exact failure the charge run's own
     * note warns of. Fixed in the drawing (ghosts dry by value and hue, never by
     * breakup). Then broken deliberately: at pitch 0.8 widths a Wisp's three
     * read as ONE ("would count 1"). Pitches of 1.05-1.2 widths still separate
     * in this raster, because {@code Brush.stroke}'s rims sit at alpha zero and
     * the inked core is narrower than the nominal width -- so the shipped 2.6
     * spacing law carries roughly 2.5x of margin in this instrument, and the
     * delivered-pixel margin is checked on captures instead.
     */
    @Test
    void everyShadowsStrokeIsCountableAtEveryShippedResolution() {
        for (int h : Guards.SHIPPED_HEIGHTS) {
            int w = Math.round(h * 4f / 3f);
            for (double frameHeight : FRAME_HEIGHTS) {
                for (EnemyArchetype a : EnemyArchetype.values()) {
                    for (int hp = 0; hp <= a.hp(); hp++) {
                        Look.Shadow s = new Look.Shadow(7, 4, hp, a.hp(), 0.0);
                        List<int[]> runs = rowRuns(s, frameHeight, w, h);
                        assertEquals(a.hp(), runs.size(), String.format(Locale.ROOT,
                                "%s at %d/%d must read as %d separable strokes at %dx%d, "
                                        + "frame %.2f world units tall; a player would count %d",
                                a, hp, a.hp(), a.hp(), w, h, frameHeight, runs.size()));
                    }
                }
            }
        }
    }

    /**
     * The row dries with the body it counts: receding with the push-in like
     * every other mark, fading through the dissolve, gone when the ink is gone.
     *
     * <p><b>Observed red</b> by negating the dying term in
     * {@code LaneInterface.shadow} ({@code 1 + dying}): the mid-dissolve row
     * came out louder than the living one and the monotone assertion failed.
     */
    @Test
    void theRowDriesWithTheBodyItCounts() {
        Look.Shadow alive = new Look.Shadow(7, 4, 3, 5, 0.0);
        Look.Shadow going = new Look.Shadow(7, 4, 0, 5, 0.5);
        Look.Shadow gone = new Look.Shadow(7, 4, 0, 5, 1.0);

        double aliveInk = ink(alive, 0.0);
        double goingInk = ink(going, 0.0);
        double goneInk = ink(gone, 0.0);
        assertTrue(aliveInk > goingInk,
                "a dissolving body's row must be quieter than a living one's: "
                        + aliveInk + " vs " + goingInk);
        assertEquals(0.0, goneInk, 1e-9, "a body whose ink has gone keeps no counter");

        double wide = ink(alive, 0.0);
        double close = ink(alive, 1.0);
        assertTrue(close < wide,
                "the row recedes with the push-in like every other mark: "
                        + wide + " -> " + close);
    }

    private static double ink(Look.Shadow s, double intimacy) {
        Recorder rec = new Recorder();
        LaneInterface.shadow(rec, s, 0f, 6.58f, intimacy);
        return rec.ink();
    }

    /**
     * No vermillion: the row states a fact and vermillion means danger. Guard B
     * asserts every vermillion vertex lies over a threatened tile; this asserts
     * the row never spends the colour at all, so the two cannot collide.
     *
     * <p><b>Observed red</b> by drawing the live strokes in
     * {@code Palette.VERMILLION}.
     */
    @Test
    void theRowNeverSpendsVermillion() {
        Recorder rec = new Recorder();
        for (EnemyArchetype a : EnemyArchetype.values()) {
            for (int hp = 0; hp <= a.hp(); hp++) {
                LaneInterface.shadow(rec, new Look.Shadow(3, 4, hp, a.hp(), 0.0),
                        0f, 6.58f, 0.0);
            }
        }
        assertTrue(rec.vermillion().isEmpty(),
                "the enemy's count must never wear the colour that means threat");
    }

    /**
     * On the three graded bouts' opening boards, every Shadow's row stays clear
     * of every <b>counted mark</b> in both margins: the stanza's glyph column on
     * the left, and on the right the hand's glyphs and -- at the row's own
     * height -- that tile's actual charge run. This is the confusable-pair
     * warning of STYLE.md 8 made a measurement: two counts may not touch.
     *
     * <p>It deliberately does <em>not</em> assert clearance from the margins'
     * foxing. On the KNIFE opening the Bulwark's worst-case row genuinely enters
     * the hand margin's stain band by 0.031 fh (22 px at 720) while staying
     * 0.019 fh clear of the nearest glyph -- measured, and recorded in
     * {@code docs/system5-input-debt.md}: foxing is substrate, not a mark, and a
     * row over a stain is not a miscount.
     *
     * <p><b>Scope, stated per 11.2b(f):</b> the three shipped openings, not
     * every reachable board -- a body walked to the extreme visible tile of a
     * wide framing can carry its row further right than any opening does. That
     * axis is measured in the debt file rather than guarded, and the trade
     * (an edge taper would blank a cornered body's count exactly when it is
     * needed) is named there.
     *
     * <p><b>Observed red</b> by asserting against the foxing band, which the
     * KNIFE Bulwark fails at 1.139 against 1.108 -- the number in the note
     * above is the guard's own red print.
     */
    @Test
    void theGradedOpeningsKeepEveryRowClearOfEveryCountedMark() {
        double aspect = 4.0 / 3.0;
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            Framing plan = staged.planning();
            double worldWidth = Director.stretchTiles(plan.widthTiles());
            double frameH = worldWidth * 3.0 / 4.0;
            double left = Director.stretch(plan.centreTile() * dev.starfall.stage.Stage.TILE_WIDTH)
                    - worldWidth / 2.0;
            // The eye law at the planning framing: the ground plane sits at 0.28
            // of the frame, and the row is SHADOW_Y world units above it.
            double rowY = 0.28 + LaneInterface.SHADOW_Y / frameH;
            List<dev.starfall.combat.Tile> hand = Bout.hand(kind);
            for (Bout.Staged.Body b : staged.bodies()) {
                if (b.hero()) {
                    continue;
                }
                double x = Director.stretch(b.tile() * dev.starfall.stage.Stage.TILE_WIDTH);
                double xFh = (x - left) / frameH;
                // Worst case: the widest row the game can carry, a Bulwark's five.
                double half = (5 - 1) * 0.5 * LaneInterface.SHADOW_PITCH
                        + LaneInterface.SHADOW_WIDTH;
                double rowLeft = xFh - half;
                double rowRight = xFh + half;
                if (rowLeft > aspect || rowRight < 0.0) {
                    // Off the sheet entirely -- the APPROACH bout's Bulwark opens
                    // 8.5 tiles right of a 6.5-tile planning frame. A row nobody
                    // can see is not a confusable pair; it is measured here and
                    // skipped rather than silently passed by a looser band.
                    continue;
                }

                double stanzaGlyphs = LaneInterface.COLUMN_X + LaneInterface.STANZA_GLYPH * 0.62;
                assertTrue(rowLeft > stanzaGlyphs, String.format(Locale.ROOT,
                        "%s: body %d's row reaches x=%.3f fh into the stanza's glyphs (%.3f)",
                        kind, b.id(), rowLeft, stanzaGlyphs));

                double handX = aspect - LaneInterface.HAND_INSET;
                double handGlyphs = handX - LaneInterface.HAND_GLYPH * 0.62;
                assertTrue(rowRight < handGlyphs, String.format(Locale.ROOT,
                        "%s: body %d's row reaches x=%.3f fh into the hand's glyphs (%.3f)",
                        kind, b.id(), rowRight, handGlyphs));

                // And the charge runs, which reach further left than the glyphs
                // do -- but only at their own heights. Two counts may not touch.
                double vClear = LaneInterface.SHADOW_HALF_LENGTH + 0.010;
                for (int i = 0; i < hand.size(); i++) {
                    int cd = hand.get(i).cooldown();
                    if (cd <= 0) {
                        continue;
                    }
                    double tickY = LaneInterface.HAND_TOP_Y - i * LaneInterface.HAND_PITCH
                            - LaneInterface.TICK_ROW;
                    if (Math.abs(rowY - tickY) > vClear) {
                        continue;
                    }
                    double runLeft = handX - (cd - 1) * LaneInterface.TICK_PITCH * 0.5
                            - LaneInterface.TICK_WIDTH;
                    assertTrue(rowRight < runLeft - 0.005, String.format(Locale.ROOT,
                            "%s: body %d's row (to x=%.3f, y=%.3f) touches tile %d's "
                                    + "charge run (from x=%.3f, y=%.3f)",
                            kind, b.id(), rowRight, rowY, i, runLeft, tickY));
                }
            }
        }
    }
}
