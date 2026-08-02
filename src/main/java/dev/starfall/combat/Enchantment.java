package dev.starfall.combat;

/**
 * The single enchantment a tile may carry (combat-design.md 1.5).
 *
 * <p>Every one of them is paid for in cooldown, which is the only currency the
 * encounter layer has. The penalties are placeholders in the sense that the
 * numbers will move; the <em>ordering</em> is not a placeholder -- Free-Play is
 * the most expensive because it buys the scarcest thing in the game, a turn.
 */
public enum Enchantment {
    /** On hit, 1 damage to the tiles either side of the target. A bloom that catches neighbours. */
    SHOCKWAVE(2),
    /** If the hit exactly finishes the target, the tile's charges are handed straight back. */
    PERFECT_STRIKE(1),
    /** On hit, applies {@link Status#SEEPING}. */
    SEEPING(2),
    /** On hit, applies {@link Status#STILLNESS}. */
    STILLNESS(3),
    /** The tile's effect resolves twice within its beat. */
    DOUBLE_STRIKE(3),
    /** On hit, applies {@link Status#MARKED}. */
    MARKING(2),
    /** Adding the tile to the Ink Stanza costs no turn. */
    FREE_PLAY(3);

    private final int cooldownPenalty;

    Enchantment(int cooldownPenalty) {
        this.cooldownPenalty = cooldownPenalty;
    }

    /** Charges added to the tile's base cooldown for carrying this. */
    public int cooldownPenalty() {
        return cooldownPenalty;
    }
}
