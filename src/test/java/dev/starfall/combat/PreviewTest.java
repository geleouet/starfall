package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "What will happen if I execute this stanza right now", answered without
 * anything happening.
 *
 * <p>The UI needs it to show the player their own phrase before they commit five
 * turns to it, and the engine needs it to be exactly the real thing rather than a
 * second implementation of the rules that will drift. So a preview is a full deep
 * copy replayed with the same code path, and what is tested here is both halves
 * of that: the answer is identical to what actually happens, and the live board
 * is untouched down to the last charge.
 */
class PreviewTest {

    private static CombatEngine loaded() {
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(1, Facing.RIGHT)
                .heroHp(30)
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.SWEEP))
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .enemy(EnemyArchetype.REACHER, 5, Facing.LEFT)
                .enemy(EnemyArchetype.WARDEN, 7, Facing.LEFT)
                .build());
        e.apply(Command.add(0));
        e.apply(Command.add(1));
        e.apply(Command.add(2));
        return e;
    }

    @Test
    void aPreviewChangesNothingAtAll() {
        CombatEngine e = loaded();
        String before = e.state().fingerprint();
        int historyBefore = e.history().size();

        e.previewExecution();

        assertEquals(before, e.state().fingerprint(), "the board must be byte-identical afterwards");
        assertEquals(historyBefore, e.history().size(), "and a dry run must not be written into the history");
    }

    @Test
    void aPreviewIsTheSameAnswerTheRealThingGives() {
        CombatEngine ghost = loaded();
        CombatEngine real = loaded();

        Resolution dry = ghost.previewExecution();
        Resolution wet = real.apply(Command.execute());

        assertEquals(wet.events(), dry.events(),
                "a preview that disagreed with the rules would be worse than no preview");
        assertEquals(real.state().fingerprint(), dry.state().fingerprint());
    }

    @Test
    void thePreviewStateIsDetachedSoTheCallerCanPokeAtIt() {
        CombatEngine e = loaded();
        Resolution dry = e.previewExecution();
        assertNotSame(e.state(), dry.state());

        Combatant ghostHero = dry.state().hero();
        ghostHero.hp(1);
        assertTrue(e.state().hero().hp() > 1, "the live hero must not feel that");
    }

    @Test
    void everyCommandCanBePreviewedNotJustExecution() {
        // Banking a tile also costs a turn, so "what does this cost me" is a
        // question about Add as much as about Execute. Same machinery, no special
        // case.
        CombatEngine e = loaded();
        for (Command c : List.of(Command.hold(), Command.turnAround(), Command.reorder(0, 2),
                Command.remove(1), Command.execute())) {
            String before = e.state().fingerprint();
            Resolution dry = e.preview(c);
            assertEquals(before, e.state().fingerprint(), "preview of " + c + " leaked");
            assertTrue(dry.events().size() > 0, "preview of " + c + " said nothing");
        }
    }

    @Test
    void previewingAnIllegalCommandFailsTheSameWayIssuingItWould() {
        CombatEngine e = loaded();
        e.apply(Command.execute());
        assertEquals(e.reject(Command.execute()), "the Ink Stanza is empty");
        assertThrows(IllegalStateException.class, () -> e.preview(Command.execute()));
        assertThrows(IllegalStateException.class, () -> e.apply(Command.execute()));
    }

    @Test
    void aPreviewCanBeUsedToChooseBetweenTwoOrderingsOfTheSameStanza() {
        // The reason the feature exists: the player writes a phrase, sees it, and
        // corrects it for free before spending the turn. Reordering is free, so
        // this whole comparison costs nothing but is worth a great deal.
        CombatEngine e = loaded();
        Resolution asWritten = e.previewExecution();

        e.apply(Command.reorder(0, 2));
        Resolution reordered = e.previewExecution();

        assertEquals(asWritten.turn(), reordered.turn(), "neither preview advanced the clock");
        assertTrue(Encounters.only(asWritten.events(), CombatEvent.BeatBegan.class).size() == 3);
        assertEquals(3, Encounters.only(reordered.events(), CombatEvent.BeatBegan.class).size());
        assertEquals(e.state().stanza().slots().stream().map(InkStanza.Slot::tile).toList(),
                Encounters.only(reordered.events(), CombatEvent.BeatBegan.class).stream()
                        .map(CombatEvent.BeatBegan::tile).toList(),
                "the preview resolves the column exactly as it is currently painted");
    }
}
