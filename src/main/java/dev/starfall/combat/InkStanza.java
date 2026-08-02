package dev.starfall.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Ink Stanza: five slots, LIFO, read downward.
 *
 * <p><b>The ordering contract, which is the whole point of this class.</b>
 * {@link #slots()} returns the stanza <em>top-first</em>, and top-first is
 * simultaneously:
 * <ul>
 *   <li>the order the engine resolves the tiles in (last banked resolves first),
 *       and</li>
 *   <li>the order the UI paints the vertical column in, top to bottom.</li>
 * </ul>
 *
 * <p>Those being the same list is not a coincidence, it is the design decision of
 * combat-design.md 3: new tiles enter at the top, so what you wrote last is at the
 * top and goes first, and the player never has to learn the letters L-I-F-O. The
 * API is shaped so the UI iterates {@code slots()} downward and the engine
 * iterates {@code slots()} in the same direction. Nothing anywhere reverses a
 * list, which is exactly the property that stops the rule from having to be
 * explained.
 *
 * <p>Slot indices used by {@link #remove(int)} and {@link #reorder(int, int)} are
 * therefore also measured from the top: slot 0 is the topmost cartouche and the
 * next thing to resolve.
 */
public final class InkStanza {

    /** Five, per combat-design.md 1.1a. Lane length is what makes five reachable. */
    public static final int CAPACITY = 5;

    /**
     * One banked tile. Carries its {@link Loadout} index so the engine can spend
     * the right charges, and a copy of the {@link Tile} so the UI can paint the
     * cartouche without reaching back into the loadout.
     *
     * @param loadoutIndex which tile of the hand this is
     * @param tile         the tile itself
     * @param bankedOnTurn the turn it was written, so the UI can age the ink
     */
    public record Slot(int loadoutIndex, Tile tile, int bankedOnTurn) {
    }

    private final List<Slot> slots = new ArrayList<>(CAPACITY);

    public int size() {
        return slots.size();
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public boolean isFull() {
        return slots.size() >= CAPACITY;
    }

    public int capacity() {
        return CAPACITY;
    }

    /** Top-first: index 0 is the topmost cartouche and the next tile to resolve. */
    public List<Slot> slots() {
        return Collections.unmodifiableList(slots);
    }

    /** The tile that would resolve first, or {@code null} on an empty stanza. */
    public Slot top() {
        return slots.isEmpty() ? null : slots.get(0);
    }

    public boolean contains(int loadoutIndex) {
        return slots.stream().anyMatch(s -> s.loadoutIndex() == loadoutIndex);
    }

    /** Writes a new tile at the top of the column. Returns its slot index, always 0. */
    int push(int loadoutIndex, Tile tile, int turn) {
        slots.add(0, new Slot(loadoutIndex, tile, turn));
        return 0;
    }

    Slot remove(int slot) {
        return slots.remove(slot);
    }

    /** Free correction: lift a cartouche out of the column and set it down elsewhere. */
    void reorder(int fromSlot, int toSlot) {
        Slot s = slots.remove(fromSlot);
        slots.add(toSlot, s);
    }

    void clear() {
        slots.clear();
    }

    InkStanza copy() {
        InkStanza c = new InkStanza();
        c.slots.addAll(slots);
        return c;
    }

    @Override
    public String toString() {
        return slots.toString();
    }
}
