package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Charted Shadows: their traits, their Strikethroughs, and the order they resolve
 * in.
 *
 * <p>The telegraph tests are the important ones. combat-design.md 1.3 says intent
 * is published one turn ahead, and STORY.md 1.2.4 makes reading it the player's
 * core skill -- so a telegraph that does not match what resolves is not a small
 * bug, it is a lie told to the player in vermillion. What is asserted here is
 * that an undisturbed intent resolves exactly as drawn, and that a disturbed one
 * <em>says</em> it changed rather than quietly resolving somewhere else.
 */
class EnemyBehaviourTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile STEP = Tile.of(TileType.STEP);

    // -- telegraphs ------------------------------------------------------------

    @Test
    void anUndisturbedIntentResolvesExactlyWhereItWasDrawn() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT);
        List<CombatState.Telegraph> drawn = e.state().strikethroughs();
        assertEquals(1, drawn.size(), "the intent is published before the player's first turn");
        List<Integer> washed = drawn.get(0).intent().threatened();
        assertEquals(List.of(0), washed, "the wash sits on the hero's tile");
        assertEquals(List.of(0), e.state().threatenedTiles());

        Resolution r = e.apply(Command.hold());

        CombatEvent.Swung swung = Encounters.firstOf(r.events(), CombatEvent.Swung.class);
        assertNotNull(swung);
        assertEquals(washed, swung.tiles(), "what resolved must be what was washed");
        assertFalse(Encounters.has(r.events(), CombatEvent.IntentRetargeted.class));
    }

    @Test
    void movingATelegraphedBodyCarriesItsThreatWithItAndSaysSo() {
        // Ambiguity resolved in Intent's class note: the Strikethrough is pinned to
        // the body, not to the tiles. Pinning it to tiles would make a single Step
        // a full disarm, which is far too much for a cooldown-0 tile.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        e.apply(Command.add(0));
        List<Integer> declared = e.state().strikethroughs().get(0).intent().threatened();
        assertEquals(List.of(2), declared);

        Resolution r = e.apply(Command.execute()); // shoves it back a tile

        CombatEvent.IntentRetargeted moved = Encounters.firstOf(r.events(), CombatEvent.IntentRetargeted.class);
        assertNotNull(moved, "the board moved under the wash, and the stream has to admit it");
        assertEquals(List.of(2), moved.declared());
        assertEquals(List.of(3), moved.actual());
        assertEquals(List.of(3), Encounters.firstOf(r.events(), CombatEvent.Swung.class).tiles());
    }

    @Test
    void aQuickBodyDeclaresAndResolvesInsideTheSameTurn() {
        // The trait, translated: Quick does not mean faster, it means no warning.
        // Its absence from strikethroughs() is the mechanic, so the UI has nothing
        // to wash and the player has nothing to read.
        CombatEngine e = CombatEngine.create(Encounters.duel(5, Hero.WARDEN)
                .enemy(EncounterSpec.Placement.of(EnemyArchetype.WISP, 1, Facing.LEFT).with(Trait.QUICK))
                .loadout(CUT)
                .build());
        assertTrue(e.state().strikethroughs().isEmpty(), "a Quick body publishes nothing in advance");
        assertNull(e.state().enemies().get(0).intent());

        Resolution r = e.apply(Command.hold());

        int declared = Encounters.indexOf(r.events(), CombatEvent.IntentDeclared.class);
        int swung = Encounters.indexOf(r.events(), CombatEvent.Swung.class);
        assertTrue(declared >= 0 && swung > declared,
                "it declares and swings inside one turn, in that order");
        assertTrue(e.state().strikethroughs().isEmpty(), "and still publishes nothing for the next one");
    }

    @Test
    void anOrdinaryBodyPublishesOnTheTurnBeforeItSwings() {
        // The control for the Quick case above: same board, no trait.
        CombatEngine e = CombatEngine.create(Encounters.duel(5, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 1, Facing.LEFT)
                .loadout(CUT)
                .build());
        assertFalse(e.opening().stream().noneMatch(CombatEvent.IntentDeclared.class::isInstance),
                "the first Strikethrough is published while the board is being set up");
        Resolution r = e.apply(Command.hold());
        assertTrue(Encounters.indexOf(r.events(), CombatEvent.Swung.class)
                        < Encounters.indexOf(r.events(), CombatEvent.IntentDeclared.class),
                "this turn's blade resolves before next turn's Strikethrough is drawn");
    }

    // -- traits ----------------------------------------------------------------

    @Test
    void aChargerCollapsesTheWholeOpenDistanceInOneMove() {
        // The extreme-motion case, and the reason a 15-tile lane does not read as
        // empty: a Runner crosses twelve tiles in a single beat.
        CombatEngine e = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.RUNNER, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant runner = Encounters.enemy(e, EnemyArchetype.RUNNER);
        Resolution r = e.apply(Command.hold());

        CombatEvent.Moved moved = Encounters.firstOf(r.events(), CombatEvent.Moved.class);
        assertNotNull(moved);
        assertEquals(CombatEvent.MoveReason.CHARGE, moved.reason());
        assertEquals(14, moved.fromTile());
        assertEquals(1, moved.toTile(), "it stops on the tile in front of the hero, not through it");
        assertEquals(1, runner.tile());
        Encounters.assertNoTileShared(e.state());
    }

    @Test
    void anAggressiveBodyClosesAfterStrikingAndAnOrdinaryOneGivesGround() {
        CombatEngine aggressive = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.BULWARK, 2, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant bulwark = Encounters.enemy(aggressive, EnemyArchetype.BULWARK);
        aggressive.apply(Command.hold()); // closes to 1
        assertEquals(1, bulwark.tile());
        Resolution struck = aggressive.apply(Command.hold());
        assertTrue(Encounters.has(struck.events(), CombatEvent.Hit.class));
        assertEquals(1, bulwark.tile(), "it wanted to close further and the hero was in the way");

        CombatEngine ordinary = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant wisp = Encounters.enemy(ordinary, EnemyArchetype.WISP);
        Resolution r = ordinary.apply(Command.hold());
        assertTrue(Encounters.has(r.events(), CombatEvent.Hit.class));
        assertEquals(4, wisp.tile(), "everything without Aggressive backs off after it strikes");
        assertEquals(CombatEvent.MoveReason.GAVE_GROUND,
                Encounters.only(r.events(), CombatEvent.Moved.class).get(0).reason());
    }

    @Test
    void aReacherThreatensTwoTilesSoClosingIsNotFree() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.REACHER, 4, Facing.LEFT)
                .loadout(CUT)
                .build());
        assertEquals(List.of(3, 2), e.state().strikethroughs().get(0).intent().threatened(),
                "the wash reaches past the empty tile between them");
        assertTrue(e.state().threatenedTiles().contains(2), "so the hero is already inside it");
        Resolution r = e.apply(Command.hold());
        assertTrue(Encounters.has(r.events(), CombatEvent.Hit.class));
    }

    @Test
    void anExplosiveDeathBloomsAcrossBothNeighbours() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WARDEN, 2, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT).withDamage(1))
                .build());
        Combatant bomb = Encounters.enemy(e, EnemyArchetype.WARDEN);
        Combatant neighbour = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        assertEquals(1, bomb.tile(), "it closed while the tile was being banked");
        int neighbourHp = neighbour.hp();

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Bloomed bloom = Encounters.firstOf(phrase, CombatEvent.Bloomed.class);
        assertNotNull(bloom);
        assertEquals(EnemyArchetype.BLOOM_DAMAGE, bloom.damage());
        assertEquals(List.of(0, 2), bloom.tiles(), "both neighbouring tiles, clipped to the lane");
        assertTrue(neighbour.hp() < neighbourHp || e.state().hero().hp() < Hero.DEFAULT_HP,
                "and it catches whatever is standing there -- including you");
    }

    // -- ordering --------------------------------------------------------------

    @Test
    void enemiesResolveInBoardOrderWhichIsATotalOrderAndNeedsNoTieBreak() {
        CombatEngine e = CombatEngine.create(Encounters.duel(11, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .enemy(EnemyArchetype.REACHER, 6, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 8, Facing.LEFT)
                .loadout(CUT)
                .build());
        Resolution r = e.apply(Command.hold());

        List<Integer> order = new ArrayList<>();
        for (CombatEvent.Moved m : Encounters.only(r.events(), CombatEvent.Moved.class)) {
            order.add(m.fromTile());
        }
        assertEquals(List.of(4, 6, 8), order, "ascending tile index, with no randomness anywhere in it");
    }

    @Test
    void twoBodiesWantingTheSameTileResolveSequentiallyAndTheSecondIsRefused() {
        // The classic double-occupancy bug. Board order means the second body sees
        // the board the first one left, so it finds the tile taken and says so
        // rather than sharing it.
        CombatEngine e = CombatEngine.create(Encounters.duel(5, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 1, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 2, Facing.RIGHT)
                .loadout(CUT)
                .build());
        Resolution r = e.apply(Command.hold());

        List<CombatEvent.MoveBlocked> blocked = Encounters.only(r.events(), CombatEvent.MoveBlocked.class);
        assertFalse(blocked.isEmpty());
        assertTrue(blocked.stream().anyMatch(b -> b.reason() == CombatEvent.BlockReason.OCCUPIED));
        Encounters.assertNoTileShared(e.state());
    }

    @Test
    void aChargerStopsShortOfABodyThatMovedFirst() {
        // Same rule with a Charger, which is where it actually bites: the Runner
        // computes its run against the board as it stands when its own step comes
        // up, not against the board at declaration.
        CombatEngine e = CombatEngine.create(Encounters.duel(11, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .enemy(EnemyArchetype.RUNNER, 8, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        Combatant runner = Encounters.enemy(e, EnemyArchetype.RUNNER);
        Resolution r = e.apply(Command.hold());

        assertEquals(3, wisp.tile());
        assertEquals(4, runner.tile(), "it ran into the tile the Wisp had just left, and stopped there");
        assertTrue(Encounters.has(r.events(), CombatEvent.IntentRetargeted.class),
                "its declared destination was recomputed, so it must say so");
        Encounters.assertNoTileShared(e.state());
    }

    @Test
    void aBladeStopsAtTheFirstBodyInLineWhoeverItBelongsTo() {
        // Which makes standing an enemy in front of another enemy a real play, and
        // is one of the better things a push or a swap can set up.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 1, Facing.RIGHT)
                .enemy(EnemyArchetype.REACHER, 2, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant shield = e.state().at(1);
        Combatant reacher = Encounters.enemy(e, EnemyArchetype.REACHER);
        assertEquals(List.of(1, 0), reacher.intent().threatened(),
                "the wash covers the hero's tile, which is what makes the play look necessary");
        int shieldHp = shield.hp();

        Resolution r = e.apply(Command.hold());

        List<CombatEvent.Hit> hits = Encounters.only(r.events(), CombatEvent.Hit.class);
        assertTrue(hits.stream().anyMatch(h -> h.attacker() == reacher.id() && h.target() == shield.id()),
                "the Reacher's blade found its own ally first");
        assertTrue(hits.stream().noneMatch(h -> h.attacker() == reacher.id()
                        && h.target() == e.state().hero().id()),
                "and never reached the tile it was washed over");
        assertTrue(shield.hp() < shieldHp);
    }

    @Test
    void theBoardNeverPutsTwoBodiesOnOneTileAcrossALongMixedFight() {
        // A sweep rather than a case: twenty turns of a crowded 15-tile lane with
        // every archetype on it, checked after each turn. Everything in the enemy
        // phase -- charges, give-ground, aggressive closes, explosive deaths -- has
        // to keep the one-body-per-tile invariant, and any one of them getting it
        // wrong is invisible until something walks through someone.
        CombatEngine e = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 5, Facing.LEFT)
                .enemy(EnemyArchetype.REACHER, 7, Facing.LEFT)
                .enemy(EnemyArchetype.RUNNER, 9, Facing.LEFT)
                .enemy(EnemyArchetype.WARDEN, 11, Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, 13, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.PARRY))
                .heroHp(200)
                .build());
        for (int turn = 0; turn < 20 && !e.state().outcome().over(); turn++) {
            Command c = e.can(Command.add(turn % 2)) ? Command.add(turn % 2)
                    : e.can(Command.execute()) ? Command.execute() : Command.hold();
            e.apply(c);
            Encounters.assertNoTileShared(e.state());
        }
    }
}
