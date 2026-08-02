package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ordering contract, which is the one rule in this engine that also has to be
 * true of the <em>UI</em>.
 *
 * <p>combat-design.md 3 settles that the stanza is a vertical column with new
 * tiles entering at the top, so that "the player never learns LIFO, they just read
 * downward". That only works if the engine's resolution order and the column's
 * paint order are literally the same list. So the assertions here are not just
 * "LIFO resolves backwards" -- they are that {@code slots()} read front-to-back
 * <b>is</b> the resolution order, and that nothing anywhere has to reverse
 * anything. A test that passed while the UI had to call {@code reverse()} would
 * be measuring the wrong property.
 */
class InkStanzaTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile STEP = Tile.of(TileType.STEP);
    private static final Tile SWEEP = Tile.of(TileType.SWEEP);
    private static final Tile TURN = Tile.of(TileType.TURN);

    private CombatEngine engine() {
        return Encounters.quietLane(Hero.WARDEN, CUT, STEP, SWEEP, TURN);
    }

    @Test
    void newTilesEnterAtTheTopAndTheTopIsWhatResolvesFirst() {
        CombatEngine e = engine();
        e.apply(Command.add(0)); // Cut
        e.apply(Command.add(1)); // Step
        e.apply(Command.add(2)); // Sweep

        List<InkStanza.Slot> column = e.state().stanza().slots();
        assertEquals(List.of(SWEEP, STEP, CUT), column.stream().map(InkStanza.Slot::tile).toList(),
                "slots() must read top-first: last written sits at the top of the column");
        assertEquals(SWEEP, e.state().stanza().top().tile());

        Resolution r = e.apply(Command.execute());
        List<TileType> resolved = Encounters.only(Encounters.phrase(r), CombatEvent.BeatBegan.class)
                .stream().map(b -> b.tile().type()).toList();
        assertEquals(List.of(TileType.SWEEP, TileType.STEP, TileType.CUT), resolved,
                "the phrase must resolve in exactly the order the column is painted, top downward");
    }

    @Test
    void beatIndicesCountDownFromTheTopSoTheUiNeverReversesAnything() {
        CombatEngine e = engine();
        e.apply(Command.add(0));
        e.apply(Command.add(1));
        e.apply(Command.add(2));
        Resolution r = e.apply(Command.execute());

        List<CombatEvent.BeatBegan> beats =
                Encounters.only(Encounters.phrase(r), CombatEvent.BeatBegan.class);
        for (int i = 0; i < beats.size(); i++) {
            assertEquals(i, beats.get(i).index(),
                    "beat index must be the slot index measured from the top, not from the bottom");
        }
    }

    @Test
    void theStanzaHoldsFiveAndRefusesASixth() {
        // Five, not three (combat-design.md 1.1a). Length of the queue is length of
        // the phrase, and phrase length is the material System 4 gets to animate.
        CombatEngine e = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 14, Facing.LEFT)
                .loadout(CUT, STEP, SWEEP, TURN, Tile.of(TileType.BACK_STEP), Tile.of(TileType.PARRY))
                .build());
        for (int i = 0; i < InkStanza.CAPACITY; i++) {
            e.apply(Command.add(i));
        }
        assertTrue(e.state().stanza().isFull());
        assertFalse(e.can(Command.add(5)));
        assertTrue(e.reject(Command.add(5)).contains("already holds 5"));
        assertThrows(IllegalStateException.class, () -> e.apply(Command.add(5)));
    }

    @Test
    void correctingTheStanzaCostsNoTurnAndTheBoardDoesNotMove() {
        // "La Rature et la Correction" (STORY.md 1.2.2): the stanza can be debugged
        // in place. If removal cost anything at all the player could not respond to
        // a board that moved under a half-written phrase.
        CombatEngine e = engine();
        e.apply(Command.add(0));
        e.apply(Command.add(1));
        e.apply(Command.add(2));
        int turn = e.state().turn();
        String before = e.state().fingerprint();

        Resolution removed = e.apply(Command.remove(1));
        assertEquals(turn, e.state().turn(), "removing must not spend a turn");
        assertFalse(Encounters.has(removed.events(), CombatEvent.TurnBegan.class),
                "a free correction must not open a turn at all, or enemies would act");

        Resolution moved = e.apply(Command.reorder(0, 1));
        assertEquals(turn, e.state().turn(), "reordering must not spend a turn");
        assertFalse(Encounters.has(moved.events(), CombatEvent.EnemyPhaseBegan.class));

        // The board itself is untouched by all of that; only the column changed.
        assertEquals(before.lines().filter(l -> !l.contains("stanza") && !l.contains("hand")).toList(),
                e.state().fingerprint().lines().filter(l -> !l.contains("stanza") && !l.contains("hand")).toList());
    }

    @Test
    void reorderingChangesTheResolutionOrderAndNothingElse() {
        CombatEngine e = engine();
        e.apply(Command.add(0)); // Cut  -> bottom
        e.apply(Command.add(2)); // Sweep-> top
        e.apply(Command.reorder(0, 1));

        assertEquals(List.of(CUT, SWEEP), e.state().stanza().slots().stream()
                .map(InkStanza.Slot::tile).toList());
        Resolution r = e.apply(Command.execute());
        assertEquals(List.of(TileType.CUT, TileType.SWEEP),
                Encounters.only(Encounters.phrase(r), CombatEvent.BeatBegan.class)
                        .stream().map(b -> b.tile().type()).toList());
    }

    @Test
    void aTileCannotBeBankedTwice() {
        // One tile is one object in the hand, not a card with copies. Without this
        // the whole cooldown economy is bypassed by banking the same Cut five times.
        CombatEngine e = engine();
        e.apply(Command.add(0));
        assertFalse(e.can(Command.add(0)));
        assertTrue(e.reject(Command.add(0)).contains("already in the Ink Stanza"));
    }

    @Test
    void anEmptyStanzaCannotBeExecuted() {
        CombatEngine e = engine();
        assertEquals("the Ink Stanza is empty", e.reject(Command.execute()));
        assertThrows(IllegalStateException.class, () -> e.apply(Command.execute()));
    }

    @Test
    void executingEmptiesTheColumn() {
        CombatEngine e = engine();
        e.apply(Command.add(0));
        e.apply(Command.add(1));
        e.apply(Command.execute());
        assertTrue(e.state().stanza().isEmpty(), "a phrase is spent whole; nothing is left over");
    }
}
