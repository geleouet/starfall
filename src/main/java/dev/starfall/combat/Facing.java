package dev.starfall.combat;

/**
 * Which way a body is turned on the Fold of the World.
 *
 * <p>The lane is one-dimensional, so facing is one bit -- but it is a bit the
 * hero has to <em>pay a turn</em> to flip (combat-design.md 1.3), which is what
 * makes it a resource rather than a rendering detail. Most tiles act in the
 * facing direction, so a hero pointed the wrong way holds a hand of tiles that
 * do nothing.
 */
public enum Facing {
    LEFT(-1),
    RIGHT(+1);

    private final int step;

    Facing(int step) {
        this.step = step;
    }

    /** {@code -1} or {@code +1}: add this to a tile index to move one tile forward. */
    public int step() {
        return step;
    }

    public Facing opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    /**
     * The facing that points from {@code fromTile} at {@code toTile}. Equal tiles
     * cannot happen on a board that allows one body per tile, and resolve to
     * {@link #RIGHT} rather than throwing, so callers need no special case.
     */
    public static Facing toward(int fromTile, int toTile) {
        return toTile < fromTile ? LEFT : RIGHT;
    }
}
