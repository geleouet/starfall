package dev.starfall.combat;

/**
 * The two Night Pilgrims of combat-design.md 2.1, which differ by exactly one
 * variable: their {@link MovementVerb}.
 *
 * <p>Same hit points, same starting hand, same queue size. Per-hero queue size is
 * a natural later dial and is deliberately not used yet -- one variable at a time
 * is enough to prove the interaction layer generic.
 */
public enum Hero {

    /** Pushes. A collision with resistance and recoil. */
    WARDEN(MovementVerb.PUSH),

    /** Swaps. An interpenetration with no impact at all, and the harder animation. */
    PILGRIM(MovementVerb.SWAP);

    /** Placeholder, to be tuned against lane length. */
    public static final int DEFAULT_HP = 8;

    private final MovementVerb verb;

    Hero(MovementVerb verb) {
        this.verb = verb;
    }

    public MovementVerb verb() {
        return verb;
    }
}
