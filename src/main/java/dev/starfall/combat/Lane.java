package dev.starfall.combat;

/**
 * The Fold of the World: a run of tiles, indexed {@code 0 .. length-1}.
 *
 * <p>Length is a design parameter and not a constant (combat-design.md 1.6). It
 * is the strongest composition dial in the game, because it changes what the
 * fight <em>is</em>: 5-7 is a knife fight where everything is already in reach,
 * 13-15 is an approach with several turns of closing before contact.
 */
public record Lane(int length) {

    public static final int MIN_LENGTH = 5;
    public static final int MAX_LENGTH = 15;

    public Lane {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "lane length must be within " + MIN_LENGTH + ".." + MAX_LENGTH + ": " + length);
        }
    }

    public boolean contains(int tile) {
        return tile >= 0 && tile < length;
    }

    /** The tile that a body walking off the near end would leave from. */
    public int first() {
        return 0;
    }

    public int last() {
        return length - 1;
    }

    /**
     * How many Charted Shadows may share the lane, which scales with length
     * (combat-design.md 1.6): "a 15-tile lane with three enemies is a corridor; a
     * 5-tile lane with three enemies is a crisis."
     *
     * <p>Half the lane, floor, minimum two. That keeps board density roughly
     * constant across the whole 5-15 range, which is what makes the density
     * <em>readable</em> as a design choice rather than an accident of length. It
     * is a tuning knob, not a law -- it caps what {@link EncounterSpec} will
     * accept, and encounters are free to use fewer.
     */
    public int maxSimultaneousEnemies() {
        return Math.max(2, length / 2);
    }
}
