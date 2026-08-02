package dev.starfall.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The hero's hand of tiles and the charges each one has recovered.
 *
 * <p>Three states per tile, and the distinction matters for the UI:
 * <ul>
 *   <li><b>ready</b> -- fully charged and on the sheet, so it may be banked;</li>
 *   <li><b>drying</b> -- charges below its cooldown, recovering one per turn;</li>
 *   <li><b>banked</b> -- currently sitting in the Ink Stanza, so it is neither
 *       available nor recovering.</li>
 * </ul>
 *
 * <p>Two rules of combat-design.md 1.2 are resolved here because the document
 * leaves them implicit. <b>The cooldown gates banking, not execution</b>: a tile
 * is spent (charges to zero) at the moment its beat resolves in a phrase, and it
 * cannot be banked again until fully recovered. And <b>a banked tile does not
 * recover</b> -- it is off the sheet and in the hand. Otherwise a five-tile
 * stanza banked over five turns would silently pre-charge itself for the next
 * one, which drains the cost out of the whole cooldown system.
 */
public final class Loadout {

    private final List<Tile> tiles;
    private final int[] charges;
    private final boolean[] banked;

    private Loadout(List<Tile> tiles) {
        this.tiles = List.copyOf(tiles);
        this.charges = new int[tiles.size()];
        this.banked = new boolean[tiles.size()];
        for (int i = 0; i < charges.length; i++) {
            charges[i] = this.tiles.get(i).cooldown();
        }
    }

    public static Loadout of(Tile... tiles) {
        return new Loadout(List.of(tiles));
    }

    public static Loadout of(List<Tile> tiles) {
        return new Loadout(tiles);
    }

    /**
     * The five tiles both heroes open with. Deliberately identical between them:
     * combat-design.md 2.1 wants the two heroes to differ by exactly one variable,
     * their movement verb, and giving them different hands would blur that.
     */
    public static Loadout starting() {
        return of(
                Tile.of(TileType.CUT),
                Tile.of(TileType.STEP),
                Tile.of(TileType.SWEEP),
                Tile.of(TileType.TURN),
                Tile.of(TileType.BACK_STEP));
    }

    public int size() {
        return tiles.size();
    }

    public List<Tile> tiles() {
        return tiles;
    }

    public Tile tile(int index) {
        return tiles.get(index);
    }

    public int charges(int index) {
        return charges[index];
    }

    public boolean banked(int index) {
        return banked[index];
    }

    /** Fully charged and not already in the stanza. */
    public boolean ready(int index) {
        return !banked[index] && charges[index] >= tiles.get(index).cooldown();
    }

    /** Indices of every tile that could be banked right now, in hand order. */
    public List<Integer> ready() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < tiles.size(); i++) {
            if (ready(i)) {
                out.add(i);
            }
        }
        return Collections.unmodifiableList(out);
    }

    void bank(int index) {
        banked[index] = true;
    }

    void unbank(int index) {
        banked[index] = false;
    }

    /** Used: charges to zero whether it hit or not (combat-design.md 1.2). */
    void spend(int index) {
        charges[index] = 0;
        banked[index] = false;
    }

    /** Perfect Strike's refund: the stroke never dried. */
    void refill(int index) {
        charges[index] = tiles.get(index).cooldown();
    }

    /** One turn of recovery, for every tile still on the sheet. */
    void recover() {
        for (int i = 0; i < charges.length; i++) {
            if (!banked[i]) {
                charges[i] = Math.min(tiles.get(i).cooldown(), charges[i] + 1);
            }
        }
    }

    Loadout copy() {
        Loadout c = new Loadout(tiles);
        System.arraycopy(charges, 0, c.charges, 0, charges.length);
        System.arraycopy(banked, 0, c.banked, 0, banked.length);
        return c;
    }

    String fingerprint() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tiles.size(); i++) {
            sb.append(tiles.get(i)).append(':').append(charges[i]).append(banked[i] ? "B" : "-").append(' ');
        }
        return sb.toString();
    }
}
