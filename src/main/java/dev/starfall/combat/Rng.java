package dev.starfall.combat;

/**
 * A seeded, copyable random source.
 *
 * <p>{@link java.util.Random} is not used, for one reason that matters:
 * {@link CombatEngine#preview} works by cloning the whole state and replaying a
 * command on the copy, so every piece of state -- including the random source --
 * has to be duplicable exactly. This is splitmix64, whose entire state is one
 * {@code long}, so {@link #copy()} is trivially correct.
 *
 * <p>The rules engine itself does not roll dice; nothing about resolving a tile
 * is random, and it should stay that way, because a fight that resolves
 * identically from the same inputs is what makes the animation review loop
 * possible at all. The random source exists for <em>composition</em> -- laying
 * out a wave (see {@link EncounterSpec#wave}) -- which is chosen once, before the
 * fight, and then never again.
 */
public final class Rng {

    private long state;

    public Rng(long seed) {
        this.state = seed;
    }

    public long seedState() {
        return state;
    }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform in {@code [0, bound)}. */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        return (int) Math.floorMod(nextLong(), bound);
    }

    public Rng copy() {
        return new Rng(state);
    }
}
