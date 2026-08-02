package dev.starfall.stage;

/**
 * What the whole body is doing, as opposed to where one chain is pointing.
 *
 * <p><b>Why a pose channel exists beside the IK targets.</b> {@code system2-debt.md}
 * E1 is the reason, and it is the sharpest criticism in the project's record: "the
 * poetry is an <em>event</em> rather than a <em>condition</em>. The hip moves during
 * the gesture and returns. Images 1 and 2 are standing still and are more loaded
 * than any frame here." An IK target is an event. A stance is a condition -- the
 * weight already over one foot, the shoulders already counter-rotated against the
 * hip line -- and the fix E1 asks for is to make the working gesture ride on top of
 * a loaded stance rather than oscillate about a symmetric rest. That needs a channel
 * the schedule can hold a value on for a whole phrase, which is this one.
 *
 * <p>Each entry is a beat STYLE.md names. None of them is a clip.
 */
public enum Stance {

    /**
     * The loaded rest of reference images 1 and 2: hip over the weighted foot,
     * shoulders counter-rotated, head off-axis. The default, and never a symmetric
     * A-pose.
     */
    READY,

    /**
     * Gathering. The 40% of a strike STYLE.md 7.1 wants long, seen as a whole-body
     * condition rather than as an arm winding up.
     */
    GATHERING,

    /**
     * The blade held across. STYLE.md 7.2's parry is "a deflection curve, not a
     * collision", and the guard is the condition the curve is drawn out of.
     */
    GUARD_RAISED,

    /**
     * Two braced bodies. combat-design.md 2.1: "a shoulder-and-hilt check... contact
     * is sustained, not instantaneous", and 3d.4 rules that a push which kills does
     * not follow through, because "the beat is the brace, not the follow-through".
     */
    BRACED,

    /**
     * STYLE.md 7.3's fourth hit reaction: "the whole figure gives on a soft IK
     * curve, spine bending, rather than recoiling on a hard offset."
     */
    YIELDING,

    /**
     * Carried. STYLE.md 7.2's knockback -- "carried backward like a sheet of silk
     * caught in wind" -- which is a different condition from being struck.
     */
    CARRIED,

    /**
     * Two figures passing through each other. combat-design.md 2.1's Pilgrim swap:
     * "an interpenetration with no impact at all", and the harder of the two verbs.
     */
    PASSING,

    /**
     * combat-design.md 1.4's Stillness: "the figure's pigment dries; motion damping
     * raised hard". A thing to draw, not an absence of one -- which is why an
     * immobilised body still gets a beat.
     */
    DRIED,

    /**
     * Going. STYLE.md 7.3 and 3: the body gives early and the ink keeps spreading,
     * which is a bloom on wet paper rather than a fall.
     */
    SLACK
}
