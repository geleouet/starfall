package dev.starfall.combat;

/**
 * The nine tiles of combat-design.md 2.2. Every one is a contact event; there is
 * deliberately no tile whose whole effect is a number going up.
 *
 * <p>Damage and cooldown here are the <em>base</em> values, and they are
 * placeholders to be tuned against a lane whose length varies from 5 to 15. What
 * is not a placeholder is the shape of each tile -- which tiles it touches,
 * relative to the actor and its facing -- because that is what the animation
 * layer choreographs.
 */
public enum TileType {

    /** 1 damage on the adjacent tile in the facing direction. The baseline stroke. */
    CUT(1, 0, true),
    /** Pierces both tiles ahead, hitting every body in the line. */
    THRUST(1, 3, true),
    /**
     * Reactive. Raises {@link Status#GUARD} with a counter armed, so the next
     * attack absorbed becomes blade-on-blade rather than a negation.
     */
    PARRY(0, 4, false),
    /** One continuous arc through the tile in front and the tile behind. */
    SWEEP(1, 3, true),
    /** Strikes the first body up to {@link #DRAW_RANGE} ahead and hauls it one tile closer. */
    DRAW(1, 4, true),
    /** Move one tile forward. Into an occupied tile this becomes the hero's verb. */
    STEP(0, 0, false),
    /** Move one tile backward. Retreat has no verb, so an occupied tile simply refuses it. */
    BACK_STEP(0, 1, false),
    /** Reverse facing inside a phrase, instead of spending a whole turn on it. */
    TURN(0, 2, false),
    /** Free-play reposition at a cooldown penalty. Motion with no contact at all. */
    FEINT(0, 3, false);

    /** How far ahead {@link #DRAW} looks for something to haul in. */
    public static final int DRAW_RANGE = 3;

    private final int baseDamage;
    private final int baseCooldown;
    private final boolean strike;

    TileType(int baseDamage, int baseCooldown, boolean strike) {
        this.baseDamage = baseDamage;
        this.baseCooldown = baseCooldown;
        this.strike = strike;
    }

    public int baseDamage() {
        return baseDamage;
    }

    public int baseCooldown() {
        return baseCooldown;
    }

    /** True when the tile can deal damage, which is what on-hit enchantments hang off. */
    public boolean strike() {
        return strike;
    }

    /**
     * The enchantment a tile of this type carries unless overridden. Only
     * {@link #FEINT} has one: combat-design.md defines Feint <em>as</em> a
     * free-play tile, so its free-play-ness rides on the same mechanism as every
     * other enchantment instead of being a second special case.
     */
    public Enchantment defaultEnchantment() {
        return this == FEINT ? Enchantment.FREE_PLAY : null;
    }
}
