package dev.starfall.combat;

import java.util.List;

/**
 * A Strikethrough: what a Charted Shadow has committed to doing next turn.
 *
 * <p>Published one turn early (combat-design.md 1.3) so the UI can wash the
 * threatened tiles in vermillion. {@link #threatened()} is the snapshot taken at
 * declaration -- the tiles the wash was actually painted over -- and the engine
 * recomputes the real ones at resolution. When those disagree, because the board
 * moved underneath, the engine says so with
 * {@link CombatEvent.IntentRetargeted} rather than quietly resolving somewhere
 * the player was never shown.
 *
 * <p><b>Ambiguity resolved:</b> combat-design.md does not say whether a telegraph
 * is pinned to tiles or to the enemy. It is pinned to the enemy -- shove a
 * Reacher and its threat travels with it. Pinning it to tiles would make pushing
 * an enemy a way to disarm it outright, which is far too strong for a Step, and
 * would make the Bulwark's refusal of the verb worth much less.
 *
 * <p><b>Why the step after a strike is declared too.</b> A blade and the footwork
 * that follows it are two beats, not one, and the engine resolves them on two
 * separate turns -- see {@code CombatEngine.enemyAttack} and combat-design.md
 * 3d.1. So {@link Kind#WITHDRAW} and {@link Kind#CLOSE_IN} exist for the same
 * reason {@link Kind#ADVANCE} does: the player is told where a body is about to
 * put itself before it puts itself there. A body that struck and then vanished
 * backward in the same instant would be announcing a threat without ever
 * offering a target.
 *
 * @param kind       what it will do
 * @param direction  the way it is turned -- always toward the hero, including
 *                   while giving ground, which is walked backward
 * @param threatened tiles washed at declaration time, in lane order
 * @param damage     damage per contact, for {@link Kind#ATTACK}
 * @param destination the tile it means to reach, for the three moving kinds; its
 *                   own tile otherwise
 */
public record Intent(Kind kind, Facing direction, List<Integer> threatened, int damage, int destination) {

    public enum Kind {
        /** A blade, over {@link Intent#threatened()}. */
        ATTACK,
        /** Closing on the hero, one tile or all of them. */
        ADVANCE,
        /** Giving ground: the step a body without Aggressive owes after it struck. */
        WITHDRAW,
        /** Walking in: the step an Aggressive body owes after it struck. */
        CLOSE_IN,
        /** Nothing this turn. */
        HOLD
    }

    public Intent {
        threatened = List.copyOf(threatened);
    }

    public static Intent attack(Facing direction, List<Integer> tiles, int damage, int at) {
        return new Intent(Kind.ATTACK, direction, tiles, damage, at);
    }

    public static Intent advance(Facing direction, int destination) {
        return new Intent(Kind.ADVANCE, direction, List.of(destination), 0, destination);
    }

    /** @param direction the way it stays turned -- at the hero, while stepping away from it */
    public static Intent withdraw(Facing direction, int destination) {
        return new Intent(Kind.WITHDRAW, direction, List.of(destination), 0, destination);
    }

    public static Intent closeIn(Facing direction, int destination) {
        return new Intent(Kind.CLOSE_IN, direction, List.of(destination), 0, destination);
    }

    public static Intent hold(Facing direction, int at) {
        return new Intent(Kind.HOLD, direction, List.of(), 0, at);
    }

    public boolean isAttack() {
        return kind == Kind.ATTACK;
    }

    /** True for the footwork a body owes after a strike, either direction. */
    public boolean isReposition() {
        return kind == Kind.WITHDRAW || kind == Kind.CLOSE_IN;
    }
}
