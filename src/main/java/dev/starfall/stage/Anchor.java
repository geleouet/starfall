package dev.starfall.stage;

import dev.starfall.combat.ContactPoint;
import dev.starfall.combat.Meeting;

import java.util.Locale;

/**
 * A point in the world, with the ordinal fact it came from still attached.
 *
 * <p><b>The whole job of the staging layer, in one type.</b> The engine says "the
 * leading side of body 3's torso, at middle height" and can say nothing else,
 * because {@link ContactPoint}'s own note is that "nothing here is in world units,
 * because the engine has none and acquiring them would make the whole event stream
 * a rendering artefact". A rig needs a coordinate. This is the mapping, and it
 * keeps the {@link Site} it was derived from so a directive stays readable and a
 * test can assert about the <em>reason</em> a target is where it is rather than
 * about a float.
 *
 * @param body the body this point belongs to, or {@link #NO_BODY} for a point on the lane
 * @param site what the point is
 * @param x    world x
 * @param y    world y, with the ground plane at zero
 */
public record Anchor(int body, Anchor.Site site, double x, double y) {

    public static final int NO_BODY = -1;

    /** What a point is, so a directive says why and not only where. */
    public enum Site {
        /** A {@link ContactPoint} the engine named: where two bodies actually touch. */
        CONTACT,
        /**
         * The one point in space two blades meet at, and the only site in this
         * enum that names a point on a <em>blade</em> rather than on a hand.
         *
         * <h2>Why it had to be its own site</h2>
         *
         * <p>{@link Meeting} names a crossing twice, once in each body's own
         * vocabulary, and the reason it does is that "each of them solves for it
         * in its own frame". Pass 1 read that literally and handed each half
         * straight to the body's sword-arm chain -- whose effector is the
         * <b>fist</b>. The blade hangs a further {@code 0.10} out of the fist at
         * 45 degrees and runs {@code 0.68} beyond that, so two fists a finger's
         * width apart put two blades a fifth of a body height apart, which is
         * exactly what shipped: 98.4 px of clear paper between the blades on a
         * 462 px figure, with the clash bloom sitting on a grip.
         *
         * <p>So the two named halves are reconciled here, in the layer that owns
         * the mapping to space, into the single world point they were always
         * describing -- and a target carrying this site is a point the
         * <em>blade</em> must pass through, not a point the hand must reach. See
         * {@code Stage#crossing} and {@code Director#arm}.
         */
        CROSSING,
        /** Where the blade sits when it is held rather than swung. */
        GUARD,
        /** The loaded rest position of the hand for this body's stance. */
        REST,
        /** The pelvis. The source of motion in STYLE.md 7.0.1. */
        HIP,
        /** The skull, and what the gaze channel of STYLE.md 4b.6 points from. */
        HEAD,
        /** The near foot's plant. */
        FOOT_LEAD,
        /** The far foot's plant. */
        FOOT_TRAIL,
        /** A tile on the lane, with no body attached: what a camera or a bloom is aimed at. */
        TILE,
        /** Where a body is looking. */
        GAZE
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s#%d(%.3f,%.3f)", site, body, x, y);
    }
}
