package dev.starfall.ui;

import dev.starfall.combat.Enchantment;
import dev.starfall.combat.TileType;

import java.util.List;

/**
 * Everything the interface shows at one instant, and nothing else.
 *
 * <p>Deliberately a value: no engine, no schedule, no GL. {@link Readout} makes
 * one of these per frame and {@link LaneInterface} draws it, which is what lets
 * the whole of the interface's <em>content</em> be asserted headlessly -- the
 * reading order of the column, which mark is resolving, what an empty stanza
 * shows -- while the drawing is asserted separately through {@link Brush.Sink}.
 *
 * <p>Its shape is also the argument about what the interface is allowed to
 * contain. Every field below is something a player cannot choose without, and the
 * omissions are as deliberate as the entries: there is no turn counter, no damage
 * number, no name and no timer. Enemy hit points were on that list for two passes
 * and {@code docs/system5-debt.md} 8 called their absence "the largest omission";
 * they are now {@link Shadow}, because a player cannot plan against a state that
 * is not drawn.
 *
 * @param time       schedule seconds, so a mark can age
 * @param stanza     the written column, <b>top first</b> -- index 0 resolves next
 * @param hand       the tiles still on the sheet, in hand order
 * @param health     the Night Pilgrim's remaining strokes
 * @param maxHealth  how many it started with, so the lost ones can stay as ghosts
 * @param laneLength the Fold of the World, in tiles
 * @param heroTile   where the Night Pilgrim stands
 * @param heroStep   which way it is turned, -1 or +1
 * @param reached    every tile the written stanza would touch, ascending
 * @param threatened every tile under a Strikethrough, ascending
 * @param bleed      0..1, how far the vermillion has wicked into the paper
 * @param intimacy   0 at the planning framing, 1 at the intimate one
 * @param shadows    every Charted Shadow still on the sheet, in board order --
 *                   the enemy hit points the planning phase is read against
 */
public record Look(double time, List<Written> stanza, List<Held> hand, int health, int maxHealth,
                   int laneLength, int heroTile, int heroStep, List<Integer> reached,
                   List<Integer> threatened, double bleed, double intimacy,
                   List<Shadow> shadows) {

    /**
     * The old arity, for callers that predate the shadows. Same picture, no
     * enemies on it.
     */
    public Look(double time, List<Written> stanza, List<Held> hand, int health, int maxHealth,
                int laneLength, int heroTile, int heroStep, List<Integer> reached,
                List<Integer> threatened, double bleed, double intimacy) {
        this(time, stanza, hand, health, maxHealth, laneLength, heroTile, heroStep,
                reached, threatened, bleed, intimacy, List.of());
    }

    /**
     * One cartouche in the column.
     *
     * <h2>{@code height} is the field the whole LIFO argument rests on</h2>
     *
     * <p>combat-design.md 3 requires new tiles to enter at the top, and STYLE.md 8
     * forbids slides: <i>"transitions are washes and bleeds, never slides or
     * pops"</i>. Held together literally those two are contradictory, because
     * inserting at the top of a list moves every mark below it down the sheet.
     *
     * <p>The resolution is that <b>the column is anchored at its base and grows
     * upward.</b> The first tile banked is written at the foot of the column and
     * stays there; each further tile is written above the last; so the newest mark
     * is always the topmost and always resolves first, and <b>no mark ever
     * moves.</b> The column then drains from the top back down to the base as the
     * phrase resolves. {@code height} is that fixed position, counted from the
     * base, and it is exactly the order the tile was banked in.
     *
     * <p><b>{@code spent} and {@code dried} are two different things and the first
     * version folded them into one.</b> A beat drinking a mark and a spent mark
     * leaving the sheet are consecutive, not the same event: the first runs over the
     * beat's own contact span and the second over
     * {@link Readout#DRY_SECONDS} afterwards. Folded together, the mark reached its
     * minimum before the drying began and the whole of STYLE.md 8's "leave by drying
     * out and fading" happened below the threshold of visibility.
     *
     * @param height   0 at the foot of the column; the banking order of this tile
     * @param wetness  1 for ink just laid down or now resolving, 0.52 while it waits
     * @param spent    0 while queued, rising to 1 as the mark is drunk by its beat
     * @param dried    0 until the beat is over, then rising to 1 as the mark leaves
     * @param seed     stable per mark, so its dry-brush breakup is reproducible
     */
    public record Written(TileType type, Enchantment enchantment, int height, double wetness,
                          double spent, double dried, float seed) {
    }

    /**
     * One tile still on the sheet.
     *
     * @param charges  how many are recovered
     * @param cooldown how many it needs. Zero means the tile has no wait at all,
     *                 and the absence of any charge mark is the right way to say so.
     * @param banked   true when the tile's ink has moved up into the column
     */
    public record Held(TileType type, Enchantment enchantment, int charges, int cooldown,
                       boolean banked) {
    }

    /**
     * One Charted Shadow's standing, for the row of strokes drawn over its body.
     *
     * <h2>Why this is in the Look at all, given the record's own argument</h2>
     *
     * <p>This record's preamble used to list "no enemy hit points" among the
     * deliberate omissions, and {@code docs/system5-debt.md} 8 named that the
     * largest omission in the system -- <i>"on a turn-based tactical game you
     * cannot plan against an enemy whose state you cannot see"</i>. It is a
     * playability blocker, not a decoration, so it joins the fields a player
     * cannot choose without.
     *
     * <p>The grammar is the hero's own (STYLE.md 8): a <b>row</b> of ink strokes
     * that dry to ghosts as they are spent, never a second vertical run beside
     * the stanza -- the row is horizontal, pinned to the body it counts, and
     * drawn in the world rather than in the margin, which is what keeps it from
     * being confusable with either column.
     *
     * @param body   the engine's id, so the scene can pin the row to the figure
     * @param tile   where the body stood at the last board chapter (fallback pin)
     * @param hp     strokes still wet, exact to the blow's own second
     * @param maxHp  how many there ever were, so the spent ones stay as ghosts
     * @param dying  0 while the body stands; rising to 1 as its dissolve runs,
     *               so the row dries off the sheet with the figure it counted
     */
    public record Shadow(int body, int tile, int hp, int maxHp, double dying) {
    }

    /** The topmost cartouche, or {@code null} on an empty stanza. */
    public Written top() {
        return stanza.isEmpty() ? null : stanza.get(0);
    }

    /** The mark a beat is currently drinking, or {@code null} when nothing is resolving. */
    public Written resolving() {
        for (Written w : stanza) {
            if (w.spent() > 0.0 && w.dried() <= 0.0) {
                return w;
            }
        }
        return null;
    }

    /** True when the sheet has to show a column that has nothing written in it. */
    public boolean stanzaIsEmpty() {
        return stanza.isEmpty();
    }
}
