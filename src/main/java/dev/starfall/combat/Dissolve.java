package dev.starfall.combat;

/**
 * How a body leaves the Fold of the World.
 *
 * <p><b>Death was the one instantaneous thing left in the stream.</b> A body's
 * hit points reached zero and it was gone in the same instant, which is the one
 * reading STYLE.md forbids everywhere else: 7.1 bans the hard freeze, 7.3 asks
 * for "a bloom of ink ... like a drop hitting wet paper", and 3 makes dissolving
 * into the paper the single most important material rule in the game. An ink
 * dissolve needs a duration and a direction and had neither.
 *
 * <p><b>The duration is in beats, which is a unit the engine already owns.</b>
 * Not seconds -- the moment the engine learns about time the whole replay
 * property becomes a rendering concern. {@link #spans} says how many beats of
 * the surrounding phrase the ink is still settling for, and it is allowed to run
 * past the end of the phrase: the sentence finishes and the pigment does not,
 * which is STYLE.md 7.1's slow-in-slower-out applied to a whole body.
 *
 * <p><b>The direction is the way the blow travelled</b>, expressed as a
 * {@link Facing} because that is the only direction a one-dimensional lane has.
 * A body cut down from the left sheds its ink to the right. A body that dies of
 * its own wounds has no blow to inherit a direction from, so it pools along its
 * own facing with {@link Force#NONE} -- it goes nowhere, and saying so is more
 * useful than a null.
 *
 * @param along  the way the ink runs, which is the way the blow travelled
 * @param force  how violently. Silk for a blade, thrown apart for a bloom.
 * @param spans  how many beats the dissolve occupies, at least one
 * @param phases the shape of the dissolve within its own span
 */
public record Dissolve(Facing along, Force force, int spans, Phases phases) {

    public Dissolve {
        if (along == null || force == null || phases == null) {
            throw new IllegalArgumentException("a dissolve is a direction, a force and a shape");
        }
        if (spans < 1) {
            throw new IllegalArgumentException("a dissolve occupies at least one beat: " + spans);
        }
    }

    /**
     * How {@code victim} goes, given what finished it.
     *
     * <p>The three staging figures are each a different drawing. A blade lays a
     * body down and the ink spreads for two beats; a bloom flings it apart inside
     * one; a wound that has been bleeding for three turns lets it sink over three,
     * which is the slowest and quietest death in the game and ought to be.
     */
    static Dissolve of(Combatant victim, Combatant killer, CombatEvent.HitSource source) {
        boolean struck = killer != null && killer.tile() != victim.tile();
        Facing along = struck ? Facing.toward(killer.tile(), victim.tile()) : victim.facing();
        return switch (source) {
            case SEEPING -> new Dissolve(victim.facing(), Force.NONE, 3, Phases.DEATH);
            case BLOOM, SHOCKWAVE -> new Dissolve(along, Force.HEADLONG, 1, Phases.BURST);
            case COLLISION -> new Dissolve(along, Force.DRIVE, 2, Phases.DEATH);
            case BLADE, COUNTER -> new Dissolve(along, Force.DRIFT, 2, Phases.DEATH);
        };
    }

    @Override
    public String toString() {
        return force + " " + along + " over " + spans + " beat" + (spans == 1 ? "" : "s") + " " + phases;
    }
}
