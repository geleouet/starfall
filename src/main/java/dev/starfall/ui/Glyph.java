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

        // "Blade passing through a body, not stopping at it." Two collinear runs
        // with the body's width between them: the mark is what a thrust leaves
        // behind, which is a line interrupted rather than a line stopped.
        MARKS.put(TileType.THRUST, List.of(
                line(-0.50f, 0.05f, -0.08f, 0.02f, 0.24f, 1.0f, 0.0f),
                line(0.08f, 0.00f, 0.50f, -0.04f, 0.17f, 0.85f, 0.0f)));

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
        MARKS.put(TileType.BACK_STEP, List.of(
                curve(new float[] {0.46f, 0.20f, -0.06f, -0.32f, -0.50f},
                        new float[] {0.08f, -0.02f, -0.12f, -0.15f, -0.02f}, 0.25f, 1.0f, 0.0f),
                line(0.18f, -0.34f, 0.50f, -0.29f, 0.21f, 0.90f, 0.0f),
                line(-0.24f, -0.45f, 0.24f, -0.41f, 0.17f, 0.72f, 0.18f)));

        // "The whole body winding around; cloth and hair last to arrive." A single
        // closing spiral, so the mark itself has no end -- it runs out of ink.
        MARKS.put(TileType.TURN, List.of(
                spiral(0f, 0f, 0.42f, 0.21f, 20f, -320f, 0.16f, 1.0f, 0.0f)));

        // "Motion with no contact -- the negative space that makes contact read."
        // The Step's travel at a dryness that keeps the brush off the paper for most
        // of it. The gesture is there and the ink is not, which is the tile.
        MARKS.put(TileType.FEINT, List.of(
                line(-0.46f, -0.24f, 0.48f, 0.20f, 0.26f, 1.0f, 0.58f)));
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
