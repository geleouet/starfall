package dev.starfall.stage;

import dev.starfall.combat.ContactPoint;

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
