package dev.starfall.combat;

/**
 * How hard a body is travelling. Four steps, no units.
 *
 * <p><b>The gap this closes.</b> A Runner collapsing twelve tiles and a Wisp
 * stepping one emit the same {@code Moved}, distinguished only by the tile delta
 * -- and a tile delta is a distance, not a force. STYLE.md 7.2 asks for the
 * opposite reading of the same two events: "knockback is a drift, not a launch. A
 * struck figure should be <em>carried</em> backward like a sheet of silk caught
 * in wind." A body shoved one tile and a body stepping one tile cover identical
 * ground and must not be drawn identically, and neither the reason nor the
 * distance says so on its own.
 *
 * <p><b>The axis is drive, not speed.</b> This is deliberately not a velocity.
 * The scale runs from a body that nothing is driving to a body committing
 * everything, and the surprising consequence -- a shove being <em>softer</em>
 * than a deliberate step -- is exactly what STYLE.md 7.2 asks for. A launch is
 * the failure mode; a deliberate stride has more muscle behind it than being
 * carried does.
 */
public enum Force {

    /**
     * Nothing is driving it. The Pilgrim's swap, which combat-design.md 2.1 calls
     * "an interpenetration with no impact at all", and a body that dies where it
     * stands.
     */
    NONE,

    /**
     * Carried. Silk in wind -- STYLE.md 7.2's knockback, and a body giving ground
     * reluctantly on the turn after it struck.
     */
    DRIFT,

    /**
     * Weight behind it: a stride, a haul on a line of force, a body walking in.
     * The ordinary register of the game.
     */
    DRIVE,

    /**
     * Everything committed. A Charger crossing the open lane, and ink thrown by an
     * Explosive death.
     */
    HEADLONG;

    /**
     * How hard a move of {@code distance} tiles for {@code reason} is travelling.
     *
     * <p>Distance only escalates a charge, and that is the point: it is the one
     * move in the game whose magnitude is not fixed by its reason, and
     * combat-design.md 2.4 names the Runner "the extreme-motion test case" for
     * precisely that. Everything else on the lane travels one tile, so scaling it
     * by distance would be scaling it by a constant.
     */
    public static Force of(CombatEvent.MoveReason reason, int distance) {
        int tiles = Math.abs(distance);
        return switch (reason) {
            case SWAPPED -> NONE;
            case SHOVED, GAVE_GROUND, FEINT -> DRIFT;
            case STEP, BACK_STEP, DRAWN, ADVANCE, CLOSED_IN -> DRIVE;
            case CHARGE -> tiles <= 2 ? DRIVE : HEADLONG;
        };
    }
}
