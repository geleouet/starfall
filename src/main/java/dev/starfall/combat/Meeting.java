package dev.starfall.combat;

/**
 * Two bodies touching: where on each of them, and when in the beat they share.
 *
 * <p><b>Why both sides are named.</b> A parry is two skeletons that have to agree
 * on one point in space, and each of them solves for it in its own frame. If the
 * stream named the crossing once, one of the two would have to infer its own
 * contact by mirroring the other's, which is exactly the sort of derivation that
 * drifts the moment the two figures are different heights. So the crossing is
 * named twice, once per body, in each body's own vocabulary -- and the fact that
 * the two names agree on {@link ContactPoint#height()} is what makes them the
 * same physical event.
 *
 * <p><b>Why the instant is here rather than looked up.</b> {@link #at} is the
 * point inside the enclosing beat, in {@link Phases#WHOLE} parts, at which the
 * contact lands. It is derivable from the beat's {@link Phases} in the simple
 * case, and deliberately not left derivable: a Double Strike resolves two
 * contacts inside one beat and they must not land together (STYLE.md 10 fails a
 * pass on sight of everything peaking on the same frame). Carrying the instant
 * also keeps a relational event self-contained, which is the property
 * {@link CombatEvent} already leans on -- a stream stays meaningful after the
 * state it describes has moved on.
 *
 * @param onActor  the contact on the body that initiated it
 * @param onTarget the contact on the body it reached
 * @param at       when, in {@link Phases#WHOLE} parts of the enclosing beat
 */
public record Meeting(ContactPoint onActor, ContactPoint onTarget, int at) {

    public Meeting {
        if (onActor == null || onTarget == null) {
            throw new IllegalArgumentException("a meeting names both bodies or it is not a meeting");
        }
        if (at < 0 || at > Phases.WHOLE) {
            throw new IllegalArgumentException("contact instant outside the beat: " + at);
        }
    }

    /** True when the two named points describe one physical crossing. */
    public boolean agrees() {
        return onActor.height() == onTarget.height() && onActor.body() != onTarget.body();
    }

    @Override
    public String toString() {
        return onActor + " x " + onTarget + " @" + at;
    }
}
