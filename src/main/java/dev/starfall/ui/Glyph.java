package dev.starfall.ui;

import dev.starfall.combat.TileType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One tile of the hand, written as the gesture it produces.
 *
 * <h2>Why the marks are strokes and not icons</h2>
 *
 * <p>combat-design.md 2.2 gives every tile two columns: what it does, and
 * <b>the choreography beat it makes</b>. The second column is already a drawing
 * instruction -- "one continuous arc through two bodies", "blade passing through
 * a body, not stopping at it", "the whole body winding around", "motion with no
 * contact". So the cartouche for a tile is that sentence drawn: the path the
 * blade takes, at the pressure a brush would put on it.
 *
 * <p>That choice is what lets STYLE.md 8's <i>"type is the last resort"</i> be
 * honoured completely rather than nearly. A label under a symbol is a label; a
 * symbol that <em>is</em> the movement needs no label, and the player learns the
 * tile and the beat in the same act. It also means the vocabulary is closed under
 * the rubric: a stroke has a direction, a pressure and a dryness and nothing
 * else, so no tile can acquire a box, a border or a fill by being added.
 *
 * <h2>Facing</h2>
 *
 * <p>Glyphs are authored facing right and mirrored when the hero is turned, so
 * the column shows which way the phrase points. combat-design.md 1.3 makes facing
 * a resource the hero pays a whole turn for; the stanza is where the player finds
 * out that the Cut at the top of the column is aimed at nothing.
 *
 * <p>Coordinates are in a unit square centred on the origin, extent
 * {@code -0.5..0.5}. Widths are in the same units.
 */
public final class Glyph {

    /**
     * One brush gesture.
     *
     * @param dryness how much of the stroke never lands. Only the Feint is high.
     */
    public record Stroke(float[] xs, float[] ys, float width, float weight, float dryness) {

        /** The same gesture drawn the other way along the lane. */
        public Stroke mirrored() {
            float[] mx = new float[xs.length];
            for (int i = 0; i < xs.length; i++) {
                mx[i] = -xs[i];
            }
            return new Stroke(mx, ys, width, weight, dryness);
        }
    }

    private static final Map<TileType, List<Stroke>> MARKS = new EnumMap<>(TileType.class);

    static {
        // "The baseline stroke." One decisive downstroke, landing high on the
        // trailing side and cutting away forward and down.
        MARKS.put(TileType.CUT, List.of(
                line(-0.20f, 0.42f, 0.24f, -0.40f, 0.26f, 1.0f, 0.0f)));

        // "Blade passing through a body, not stopping at it."
        //
        // <b>Re-authored: the interrupted line needed the thing that interrupts it.</b>
        // Pass 1's reviewer read this correctly only after being told it was an
        // interrupted line, and said so. The pass-2 reviewer read it unaided and got
        // <i>"two short level lens dashes with a gap... a weight transfer"</i> --
        // two footfalls, which is the Step's own device, and the vocabulary already
        // spends "two marks side by side" on Step. Two reviews, one correct reading
        // with the answer supplied and one movement verb without it.
        //
        // The brief for the fix was exact: <i>"give it something a movement cannot
        // have."</i> A movement cannot have a <b>body in it</b>. So the beat's own
        // sentence is drawn literally -- the long level stroke is the blade, the
        // steep stroke across it is what it passes through, and the blade's line
        // continues past it rather than stopping at it. Nothing in Step, Back-step,
        // Turn or Feint has a second stroke crossing a first, and nothing in Parry
        // crosses at a steep angle: Parry's two strokes run alongside each other for
        // a third of the mark, which is the span that makes it a deflection.
        MARKS.put(TileType.THRUST, List.of(
                line(-0.50f, 0.06f, -0.07f, 0.03f, 0.24f, 1.0f, 0.0f),
                line(0.11f, 0.02f, 0.50f, -0.01f, 0.20f, 0.82f, 0.0f),
                curve(new float[] {0.06f, 0.02f, 0.00f, 0.03f, 0.08f},
                        new float[] {0.31f, 0.15f, -0.02f, -0.19f, -0.33f}, 0.13f, 0.62f, 0.0f)));

        // "The signature beat. Blade-on-blade, a deflection curve rather than a
        // collision."
        //
        // <b>Pass 1 drew this as two straight lines meeting at a sharp apex, and a
        // corner is what a collision looks like.</b> STYLE.md 7.1 settles what the
        // mark has to say: <i>"contact is a span rather than an instant... reading
        // the middle span as travel-toward-a-hit puts the meeting at 55 and
        // collapses the deflection to a point, which is the collision this document
        // exists to forbid."</i> So the meeting here is a <em>span</em> and the
        // redirection is a <em>curve</em>:
        //
        //   - the defending blade is the long shallow arc, rolling as it takes the
        //     weight, and it never changes direction;
        //   - the attack comes down steeply from above, runs alongside it for a
        //     third of the mark's width at a fixed small gap, and leaves climbing --
        //     the same stroke, turned, with no vertex where the turn happens.
        //
        // Both are Catmull-Rom through their control points, so neither can print a
        // corner even if the points suggest one.
        MARKS.put(TileType.PARRY, List.of(
                curve(new float[] {-0.50f, -0.26f, 0.02f, 0.28f, 0.50f},
                        new float[] {-0.22f, -0.06f, 0.06f, 0.11f, 0.11f}, 0.22f, 1.0f, 0.0f),
                curve(new float[] {-0.36f, -0.17f, 0.02f, 0.26f, 0.48f},
                        new float[] {0.46f, 0.28f, 0.17f, 0.23f, 0.38f}, 0.19f, 0.9f, 0.0f)));

        // "One continuous arc through two bodies." One stroke, 230 degrees, drawn
        // through the tile in front and the tile behind.
        MARKS.put(TileType.SWEEP, List.of(
                arc(0f, -0.04f, 0.40f, 0.36f, 200f, -30f, 0.22f, 1.0f, 0.0f)));

        // "Contact at distance -- a line of force between two figures." The stroke
        // runs out to the far tile and hooks back, so the mark's own direction is
        // the haul.
        // <b>The hook is opened and the ribbon narrowed, because a brush lifts as it
        // turns.</b> At width 0.22 the stroke's own half-width was wider than the
        // radius of its hook, so the ribbon folded across itself and the two coats
        // printed a bright core with a step at its boundary -- measured through the
        // interface's raster at 0.48 of the frame's amplitude, the steepest thing
        // this layer drew. The same is true of a hand at a tight turn: it thins.
        MARKS.put(TileType.DRAW, List.of(
                curve(new float[] {0.50f, 0.20f, -0.08f, -0.34f, -0.50f},
                        new float[] {0.18f, 0.08f, -0.02f, -0.04f, 0.14f}, 0.16f, 1.0f, 0.0f)));

        // "Weight transfer", and it takes two feet to be one. A short heavy mark
        // where the weight leaves, and a long one where it arrives -- which is what
        // keeps the Step from being a second arc beside the Sweep.
        MARKS.put(TileType.STEP, List.of(
                line(-0.50f, -0.34f, -0.14f, -0.38f, 0.19f, 0.85f, 0.0f),
                line(-0.10f, -0.20f, 0.50f, 0.12f, 0.25f, 1.0f, 0.0f)));

        // The same weight transfer, walked backward -- and it is <b>not</b> the Step
        // mirrored.
        //
        // <b>Pass 1's note, "retreat has no verb, so the mark is the Step's own
        // gesture read the other way", was elegant and it was the bug.</b> Glyphs are
        // authored facing right and mirrored when the hero turns, so a Back-step that
        // is a mirrored Step is <em>vertex-identical</em> to a Step drawn by a hero
        // facing the other way: two tiles, one picture, and the only way to tell them
        // apart is to first read an 86 px figure's facing. Forward versus back is the
        // sharpest choice on a lane and it is the one that collision destroyed.
        //
        // The retreat is given its own asymmetry, from what a retreating body
        // actually does: the front foot does not lift and land, it <em>drags</em>.
        // Three marks rather than two -- the heel travelling back and hooking as it
        // takes the weight, the weight still standing where it was, and a dry scuff
        // under the travel where the sole never left the ground. Nothing in the Step
        // has a hook and nothing in it is dry, so no reflection of either mark can
        // reach the other.
        // <b>And pass 2's version of it was read cold as an attack.</b> The pass-2
        // reviewer, off the delivered pixels, wrote: <i>"a long tapering stroke,
        // thick through the middle, drawn out to a fine barbed point at the left --
        // toward the hero -- with three small dashes beneath it. A strike, aimed
        // leftward. This is the enemy's intent."</i> Confident, and wrong on the one
        // axis {@code combat-design.md} 2.2 spends a whole tile on.
        //
        // The diagnosis is in the pressure profile rather than in the drawing. A
        // {@link Brush} stroke lands heavy and thins along its travel, so a stroke
        // authored from {@code x=+0.46} to {@code x=-0.50} prints a heavy head on the
        // right and a fine point on the left -- which is exactly what a blade
        // travelling leftward looks like. The two marks that carried the meaning, the
        // hook and the dry scuff, measured 21.6 and 18.9 of luminance lift against
        // the travel stroke's 77.8: <b>the loud part said the wrong word</b>.
        //
        // So the retreat is now the loud part, and the mark is drawn in the
        // direction the body's weight goes:
        //
        //   - the <b>heel</b>, which is the whole gesture, is authored <em>from</em>
        //     the rear. It lands at the back, catches, and hooks; the brush's own
        //     pressure therefore puts its mass at the left and thins it forward, so
        //     the heaviest thing in the mark is behind the body and the faintest is
        //     the trace left toward the enemy. That is the reverse of a strike;
        //   - the weight <em>still standing</em> where it was, at the front, quiet;
        //   - and the dry scuff of the sole that never left the ground, running
        //     forward-to-back under the travel.
        //
        // Nothing in the Step has a hook and nothing in it is dry, so no reflection
        // of either mark can reach the other.
        MARKS.put(TileType.BACK_STEP, List.of(
                // Width 0.21 and not 0.26, and the hook opened, for the reason the
                // Draw's own note records: at a half-width wider than the radius of
                // its own turn a ribbon folds across itself, the two coats print a
                // bright core, and the boundary of that core is the steepest thing
                // the layer draws. Measured here at the hand's cartouche side, the
                // first version of this mark read 0.5479 of the field's amplitude in
                // one pixel against a ceiling of 0.354. A brush lifts as it turns.
                curve(new float[] {-0.30f, -0.50f, -0.42f, -0.10f, 0.28f},
                        new float[] {-0.32f, -0.10f, 0.16f, 0.12f, -0.02f}, 0.21f, 1.0f, 0.0f),
                line(0.26f, -0.34f, 0.50f, -0.31f, 0.19f, 0.62f, 0.0f),
                line(0.30f, -0.42f, -0.26f, -0.38f, 0.16f, 0.80f, 0.26f)));

        // "The whole body winding around; cloth and hair last to arrive." A single
        // closing spiral, so the mark itself has no end -- it runs out of ink.
        MARKS.put(TileType.TURN, List.of(
                spiral(0f, 0f, 0.42f, 0.21f, 20f, -320f, 0.16f, 1.0f, 0.0f)));

        // "Motion with no contact -- the negative space that makes contact read."
        //
        // <b>Pass 2 drew this as absence and it delivered nothing at all.</b> One
        // stroke at {@code dryness} 0.58, which the hand's own state dryness then
        // pushed to the 0.92 clamp: measured on the delivered planning frame through
        // {@code x835..900 y450..492}, the whole tile peaked at <b>4.5</b> of
        // luminance lift with six pixels above the floor -- 17x fainter than the tile
        // drawn immediately above it, and <em>three times fainter than the impression
        // an empty stanza slot leaves</em>. Its own cooldown ticks out-read it by 9x.
        // A tile the hand says is there and the eye cannot find is worse than an
        // omission, which is the same finding the charge run produced in pass 1.
        //
        // The tile is negative space, so the mark is a gesture <b>and the gesture it
        // did not make</b>: one committed stroke, and beside it the same stroke
        // again, offset and dry, which is where the blade would have gone. The ink is
        // present -- that is the fix -- and what says "no contact" is that the second
        // mark never lands. Two parallel strokes on a rising diagonal are nothing
        // else in this alphabet: Parry's two strokes cross, Thrust's are collinear
        // with a body between them, and Step's are a short dash and a long lens at
        // different heights.
        //
        // The state dryness of a held tile no longer adds to this one -- see
        // {@code LaneInterface.place}, which takes the drier of the two rather than
        // the sum, because they are two statements about one brush.
        MARKS.put(TileType.FEINT, List.of(
                line(-0.46f, -0.26f, 0.46f, 0.16f, 0.27f, 1.0f, 0.28f),
                line(-0.38f, -0.06f, 0.40f, 0.30f, 0.21f, 0.58f, 0.52f)));
    }

    private Glyph() {
    }

    /** The marks for one tile, facing right. */
    public static List<Stroke> of(TileType type) {
        List<Stroke> marks = MARKS.get(type);
        if (marks == null) {
            throw new IllegalArgumentException("no mark authored for " + type);
        }
        return marks;
    }

    /** The marks for one tile, turned along the lane. {@code step} is -1 or +1. */
    public static List<Stroke> of(TileType type, int step) {
        List<Stroke> marks = of(type);
        if (step >= 0) {
            return marks;
        }
        List<Stroke> out = new ArrayList<>(marks.size());
        for (Stroke s : marks) {
            out.add(s.mirrored());
        }
        return out;
    }

    /** Every tile type has a mark. Asserted, because a missing one is an exception at draw time. */
    public static boolean isComplete() {
        for (TileType t : TileType.values()) {
            if (!MARKS.containsKey(t)) {
                return false;
            }
        }
        return true;
    }

    // -- authoring helpers -----------------------------------------------------

    private static Stroke line(float x0, float y0, float x1, float y1, float width, float weight,
                               float dryness) {
        int n = 9;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            xs[i] = x0 + (x1 - x0) * u;
            ys[i] = y0 + (y1 - y0) * u;
        }
        return new Stroke(xs, ys, width, weight, dryness);
    }

    /** A Catmull-Rom through the control points, resampled -- a hand does not draw corners. */
    private static Stroke curve(float[] px, float[] py, float width, float weight, float dryness) {
        int n = 22;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1) * (px.length - 1);
            int k = Math.min(px.length - 2, (int) u);
            float f = u - k;
            xs[i] = spline(px, k, f);
            ys[i] = spline(py, k, f);
        }
        return new Stroke(xs, ys, width, weight, dryness);
    }

    private static float spline(float[] p, int k, float f) {
        float p0 = p[Math.max(0, k - 1)];
        float p1 = p[k];
        float p2 = p[k + 1];
        float p3 = p[Math.min(p.length - 1, k + 2)];
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * f
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * f * f
                + (-p0 + 3f * p1 - 3f * p2 + p3) * f * f * f);
    }

    private static Stroke arc(float cx, float cy, float rx, float ry, float fromDeg, float toDeg,
                              float width, float weight, float dryness) {
        int n = 24;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / (double) (n - 1));
            xs[i] = cx + (float) Math.cos(a) * rx;
            ys[i] = cy + (float) Math.sin(a) * ry;
        }
        return new Stroke(xs, ys, width, weight, dryness);
    }

    private static Stroke spiral(float cx, float cy, float r0, float r1, float fromDeg, float toDeg,
                                 float width, float weight, float dryness) {
        int n = 30;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * u);
            float r = r0 + (r1 - r0) * u;
            xs[i] = cx + (float) Math.cos(a) * r;
            ys[i] = cy + (float) Math.sin(a) * r * 0.92f;
        }
        return new Stroke(xs, ys, width, weight, dryness);
    }
}
