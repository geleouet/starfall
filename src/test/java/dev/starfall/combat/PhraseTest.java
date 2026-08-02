package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Execution: all-or-nothing, and what "all" turns out to mean.
 *
 * <p>combat-design.md 1.1 says the whole sequence resolves without interruption,
 * and 1.1a says a five-tile stanza spent on a board that moved underneath is a
 * five-turn loss and that this is "better drama, not worse balance". Both of
 * those only bite if a phrase genuinely keeps going after it has stopped being
 * useful, so most of these tests are about failure: strokes into empty air, tiles
 * aimed at bodies that are already gone, charges spent for nothing.
 *
 * <p>The combo tests are the other half. Combos are the economy's engine
 * (combat-design.md 1.5), and a five-tile execution can clear far more than a
 * three-tile one, so what counts has to be exact -- including kills caused by an
 * explosion rather than by a blade, and excluding kills that happened to fall in
 * the same fight but different phrases.
 */
class PhraseTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile HEAVY_CUT = Tile.of(TileType.CUT).withDamage(3);
    private static final Tile KILLING_CUT = Tile.of(TileType.CUT).withDamage(5);
    private static final Tile SWEEP = Tile.of(TileType.SWEEP);
    private static final Tile THRUST = Tile.of(TileType.THRUST);
    private static final Tile STEP = Tile.of(TileType.STEP);

    @Test
    void aTileAimedAtSomethingTheTileBeforeItKilledStillSwingsAndStillPays() {
        // The case the LIFO rule makes easy to get wrong: the top tile clears the
        // target the second one was written for. It is not skipped and not
        // redirected -- it swings through the empty tile and dries out.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(CUT, HEAVY_CUT)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0)); // plain Cut, resolves second
        e.apply(Command.add(1)); // heavy Cut, resolves first
        assertEquals(1, wisp.tile(), "it has closed to arm's length");

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertTrue(Encounters.has(phrase, CombatEvent.Died.class));
        assertTrue(Encounters.has(phrase, CombatEvent.Whiffed.class),
                "the second stroke goes through the space where the body was");
        assertEquals(2, Encounters.only(phrase, CombatEvent.TileSpent.class).size(),
                "both tiles are spent -- all-or-nothing applies to the cost as well as the effect");
        assertEquals(EncounterOutcome.VICTORY, e.state().outcome());
    }

    @Test
    void aPhraseKeepsResolvingAfterTheBoardIsAlreadyClear() {
        // STORY.md 1.2.3: "le Pèlerin poursuit son tracé jusqu'au bout de son
        // encre, restant exposé". Three beats written, three beats resolved,
        // whatever happens to the board on the first one. This is also what gives
        // System 4 a phrase of fixed length to choreograph rather than one that can
        // be truncated mid-gesture.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, KILLING_CUT, CUT, SWEEP);
        e.apply(Command.add(1));
        e.apply(Command.add(2));
        e.apply(Command.add(0)); // the killing Cut on top: resolves first

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertTrue(Encounters.has(phrase, CombatEvent.Died.class));
        assertEquals(3, Encounters.only(phrase, CombatEvent.BeatBegan.class).size());
        assertEquals(3, Encounters.only(phrase, CombatEvent.TileSpent.class).size());
        assertEquals(2, Encounters.only(phrase, CombatEvent.Whiffed.class).size(),
                "the two beats after the kill still swing, at nothing");
        assertEquals(EncounterOutcome.VICTORY, e.state().outcome());
    }

    @Test
    void aPhraseSpentOnABoardThatMovedUnderneathIsAWholeTurnLost() {
        // The five-turn-loss drama of combat-design.md 1.1a, in miniature: the Wisp
        // gave ground after striking, so the Cut banked while it was adjacent finds
        // nothing at all and pays anyway.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(SWEEP)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        assertEquals(4, wisp.tile(), "it struck and stepped back out of reach");

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Whiffed whiffed = Encounters.firstOf(phrase, CombatEvent.Whiffed.class);
        assertNotNull(whiffed);
        assertEquals(List.of(3, 1), whiffed.tiles(), "the arc went through both tiles and found neither");
        assertEquals(wisp.maxHp(), wisp.hp());
        assertFalse(e.state().loadout().ready(0), "and the stroke dried out for it");
    }

    @Test
    void everyBeatOfThePhraseIsAnnouncedInOrderWithTheStanzaEmptiedAtTheEnd() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT, STEP, SWEEP, Tile.of(TileType.TURN),
                Tile.of(TileType.BACK_STEP));
        for (int i = 0; i < 5; i++) {
            e.apply(Command.add(i));
        }
        Resolution r = e.apply(Command.execute());
        List<CombatEvent> phrase = Encounters.phrase(r);

        CombatEvent.PhraseBegan began = Encounters.firstOf(phrase, CombatEvent.PhraseBegan.class);
        assertEquals(5, began.tiles(), "five linked beats, which is the material 1.1a bought");
        assertEquals(5, Encounters.only(phrase, CombatEvent.BeatBegan.class).size());
        assertTrue(e.state().stanza().isEmpty());
        assertEquals(6, e.state().turn() - 1, "five turns to write the sentence and one to speak it");
    }

    // -- combos ----------------------------------------------------------------

    @Test
    void twoKillsInOneExecutionIsACombo() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 2, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(Tile.of(TileType.THRUST).withDamage(3))
                .build());
        e.apply(Command.add(0)); // they close to 1 and 2 while it is banked

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.ComboLanded combo = Encounters.firstOf(phrase, CombatEvent.ComboLanded.class);
        assertNotNull(combo, "one Thrust through two bodies is the cleanest combo in the game");
        assertEquals(2, combo.count());
        assertEquals(2, combo.victims().size());
        assertEquals(2, Encounters.firstOf(phrase, CombatEvent.PhraseEnded.class).kills());
    }

    @Test
    void aSingleKillIsNotACombo() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 2, Facing.LEFT)
                .loadout(HEAVY_CUT)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertTrue(Encounters.has(phrase, CombatEvent.Died.class));
        assertTrue(Encounters.only(phrase, CombatEvent.ComboLanded.class).isEmpty());
        assertEquals(1, Encounters.firstOf(phrase, CombatEvent.PhraseEnded.class).kills());
    }

    @Test
    void aKillCausedByAnExplosionCountsTowardTheCombo() {
        // "Choose *where* it dies, not just whether" (combat-design.md 2.4). If a
        // bloom's kills did not count, the whole reason to line an Explosive body
        // up next to something would evaporate.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WARDEN, 3, Facing.LEFT)
                .enemy(EnemyArchetype.WARDEN, 4, Facing.LEFT)
                .loadout(THRUST)
                .build());
        e.apply(Command.add(0));

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertEquals(2, Encounters.only(phrase, CombatEvent.Bloomed.class).size(),
                "one death sets off the other");
        CombatEvent.ComboLanded combo = Encounters.firstOf(phrase, CombatEvent.ComboLanded.class);
        assertNotNull(combo);
        assertEquals(2, combo.count());
        assertEquals(EncounterOutcome.VICTORY, e.state().outcome());
    }

    @Test
    void killsSpreadAcrossSeparateExecutionsAreNeverACombo() {
        // The rule is per execution, not per fight. Worth pinning explicitly,
        // because a phrase-scoped counter held in a field is exactly the sort of
        // thing that leaks across turns.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 2, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .heroHp(100)
                .loadout(HEAVY_CUT)
                .build());
        e.apply(Command.add(0));
        Resolution first = e.apply(Command.execute());
        assertTrue(Encounters.has(first.events(), CombatEvent.Died.class));
        assertEquals(1, Encounters.firstOf(first.events(), CombatEvent.PhraseEnded.class).kills());
        assertTrue(Encounters.only(first.events(), CombatEvent.ComboLanded.class).isEmpty());

        for (int i = 0; i < 30 && !e.state().outcome().over(); i++) {
            e.apply(e.can(Command.add(0)) ? Command.add(0) : Command.execute());
        }
        assertTrue(e.history().stream().noneMatch(CombatEvent.ComboLanded.class::isInstance),
                "one kill per phrase, however many phrases the fight takes");
        assertTrue(e.history().stream().filter(CombatEvent.PhraseEnded.class::isInstance)
                        .map(CombatEvent.PhraseEnded.class::cast)
                        .allMatch(p -> p.kills() <= 1),
                "and every phrase agrees");
    }

    // -- enchantments that reshape the beat ------------------------------------

    @Test
    void doubleStrikeResolvesTheSameBeatTwice() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN,
                Tile.of(TileType.CUT, Enchantment.DOUBLE_STRIKE));
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        int hp = bulwark.hp();
        e.apply(Command.add(0));

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertEquals(1, Encounters.only(phrase, CombatEvent.BeatBegan.class).size(), "one beat");
        assertEquals(2, Encounters.only(phrase, CombatEvent.Swung.class).size(), "two strokes inside it");
        assertEquals(hp - 2, bulwark.hp());
    }

    @Test
    void perfectStrikeRefundsATileThatFinishedABodyExactly() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WARDEN, 2, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT, Enchantment.PERFECT_STRIKE))
                .build());
        e.apply(Command.add(0));
        Resolution r = e.apply(Command.execute());

        assertTrue(Encounters.has(r.events(), CombatEvent.TileRefunded.class),
                "one damage into a one-hit-point body is exact, so the stroke never dried");
        assertEquals(e.state().loadout().tile(0).cooldown(), e.state().loadout().charges(0));
    }

    @Test
    void perfectStrikeRefundsNothingWhenTheBodySurvives() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN,
                Tile.of(TileType.CUT, Enchantment.PERFECT_STRIKE));
        e.apply(Command.add(0));
        Resolution r = e.apply(Command.execute());
        assertFalse(Encounters.has(r.events(), CombatEvent.TileRefunded.class),
                "one damage into five hit points is not an exact finish");
        assertEquals(1, e.state().loadout().charges(0), "so it dried out and recovered one, like anything else");
    }

    @Test
    void shockwaveCatchesTheTileBeyondTheOneStruck() {
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 2, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT, Enchantment.SHOCKWAVE))
                .build());
        e.apply(Command.add(0));
        Combatant struck = e.state().at(1);
        Combatant behind = e.state().at(2);
        assertNotNull(struck);
        assertNotNull(behind);
        int behindHp = behind.hp();

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        List<CombatEvent.Hit> splash = Encounters.only(phrase, CombatEvent.Hit.class).stream()
                .filter(h -> h.source() == CombatEvent.HitSource.SHOCKWAVE).toList();
        assertEquals(1, splash.size(), "one neighbour occupied, one bloom");
        assertEquals(behind.id(), splash.get(0).target());
        assertEquals(behindHp - CombatEngine.SHOCKWAVE_DAMAGE, behind.hp());
        assertTrue(struck.hp() < struck.maxHp(), "and the body actually struck took the blade itself");
    }
}
