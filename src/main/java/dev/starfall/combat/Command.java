package dev.starfall.combat;

/**
 * Everything the Night Pilgrim can do, and what each one costs.
 *
 * <p>The cost column <em>is</em> the game (combat-design.md 1.1): banking a tile
 * costs a turn spent standing in front of a telegraphed attack, correcting the
 * stanza costs nothing at all, and turning costs a whole turn unless a tile
 * grants it. Modelling the commands as one closed set with the cost attached
 * keeps that economy in one readable place instead of scattered across the
 * engine.
 *
 * <table>
 *   <caption>Cost</caption>
 *   <tr><th>Command</th><th>Costs a turn</th></tr>
 *   <tr><td>{@link Add}</td><td>yes, unless the tile is free-play</td></tr>
 *   <tr><td>{@link Remove}</td><td>no</td></tr>
 *   <tr><td>{@link Reorder}</td><td>no</td></tr>
 *   <tr><td>{@link Execute}</td><td>yes</td></tr>
 *   <tr><td>{@link TurnAround}</td><td>yes</td></tr>
 *   <tr><td>{@link Hold}</td><td>yes</td></tr>
 * </table>
 */
public sealed interface Command {

    /** Bank a tile at the top of the Ink Stanza. Costs a turn unless it is free-play. */
    record Add(int loadoutIndex) implements Command {
    }

    /** Rub a cartouche out. Free. Slots are counted from the top. */
    record Remove(int slot) implements Command {
    }

    /** Lift a cartouche and set it down elsewhere in the column. Free. */
    record Reorder(int fromSlot, int toSlot) implements Command {
    }

    /** Trigger the phrase. All-or-nothing, top slot first. Costs a turn. */
    record Execute() implements Command {
    }

    /** Spend a whole turn reversing facing. Facing is a resource. */
    record TurnAround() implements Command {
    }

    /** Do nothing and let the turn pass. */
    record Hold() implements Command {
    }

    static Command add(int loadoutIndex) {
        return new Add(loadoutIndex);
    }

    static Command remove(int slot) {
        return new Remove(slot);
    }

    static Command reorder(int fromSlot, int toSlot) {
        return new Reorder(fromSlot, toSlot);
    }

    static Command execute() {
        return new Execute();
    }

    static Command turnAround() {
        return new TurnAround();
    }

    static Command hold() {
        return new Hold();
    }
}
