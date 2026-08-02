package dev.starfall.combat;

/**
 * One tile of the hero's hand: a shape, a number, a cooldown, and at most one
 * enchantment.
 *
 * <p>Immutable. The mutable half -- how many charges this particular tile has
 * recovered -- lives in {@link Loadout}, because a tile is a design object and a
 * charge count is a fact about one fight.
 *
 * @param type        the shape of the contact
 * @param damage      damage per contact, already including any tuning
 * @param cooldown    charges to recover before it may be banked again, 0 to 8
 * @param enchantment the one enchantment, or {@code null}
 */
public record Tile(TileType type, int damage, int cooldown, Enchantment enchantment) {

    /** The hard cap of combat-design.md 1.2. */
    public static final int MAX_COOLDOWN = 8;

    public Tile {
        if (damage < 0) {
            throw new IllegalArgumentException("damage must not be negative: " + damage);
        }
        if (cooldown < 0 || cooldown > MAX_COOLDOWN) {
            throw new IllegalArgumentException("cooldown must be within 0.." + MAX_COOLDOWN + ": " + cooldown);
        }
    }

    /** The tile at its designed values, carrying its type's default enchantment if any. */
    public static Tile of(TileType type) {
        Enchantment e = type.defaultEnchantment();
        int cd = type.baseCooldown() + (e == null ? 0 : e.cooldownPenalty());
        return new Tile(type, type.baseDamage(), Math.min(MAX_COOLDOWN, cd), e);
    }

    /**
     * The tile with an enchantment fitted, paying its cooldown penalty. A tile
     * carries at most one, so this replaces rather than adds -- including
     * replacing a type's default.
     */
    public static Tile of(TileType type, Enchantment enchantment) {
        int cd = type.baseCooldown() + enchantment.cooldownPenalty();
        return new Tile(type, type.baseDamage(), Math.min(MAX_COOLDOWN, cd), enchantment);
    }

    /** The same tile with different damage, for encounters that want to tune one card. */
    public Tile withDamage(int newDamage) {
        return new Tile(type, newDamage, cooldown, enchantment);
    }

    public Tile withCooldown(int newCooldown) {
        return new Tile(type, damage, newCooldown, enchantment);
    }

    public boolean has(Enchantment e) {
        return enchantment == e;
    }

    /** Adding this tile to the Ink Stanza costs no turn. */
    public boolean freePlay() {
        return enchantment == Enchantment.FREE_PLAY;
    }

    @Override
    public String toString() {
        return type + (enchantment == null ? "" : "[" + enchantment + "]");
    }
}
