package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a turn costs, and what it buys.
 *
 * <p>This is the tension the whole design hangs off (combat-design.md 1.1): every
 * tile you bank is a turn you spend standing in front of a telegraphed attack, so
 * a five-tile stanza is five turns of exposure before a single blade moves. If
 * banking were ever free the game would be a puzzle with no clock. The tests here
 * pin the price list, including the two deliberate exceptions -- free correction
 * and the free-play enchantment.
 */
class TurnEconomyTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile SWEEP = Tile.of(TileType.SWEEP);   // cooldown 3
    private static final Tile FEINT = Tile.of(TileType.FEINT);   // free-play, cooldown 6
    private static final Tile TURN = Tile.of(TileType.TURN);

    @Test
    void bankingATileSpendsATurnAndLetsTheEnemiesResolve() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT, SWEEP);
        int before = e.state().turn();
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        int wispTile = wisp.tile();

        Resolution r = e.apply(Command.add(0));

        assertEquals(before + 1, e.state().turn(), "banking a tile costs a turn");
        assertTrue(Encounters.has(r.events(), CombatEvent.EnemyPhaseBegan.class),
                "and the enemies get that turn");
        assertTrue(wisp.tile() < wispTile, "which they spend closing");
    }

    @Test
    void executingSpendsATurnOfItsOwn() {
        // The five-tile stanza costs six turns, not five: five to write and one to
        // spend. Worth stating, because it is the number that makes the long lane
        // and the long queue the same design decision.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT, SWEEP);
        e.apply(Command.add(0));
        int before = e.state().turn();
        e.apply(Command.execute());
        assertEquals(before + 1, e.state().turn());
    }

    @Test
    void turningAroundCostsAWholeTurn() {
        // Facing is a resource (combat-design.md 1.3). A hero pointed the wrong way
        // holds a hand of tiles that do nothing, and buying the right way costs
        // exactly as much as banking a tile would have.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        assertEquals(Facing.RIGHT, e.state().hero().facing());
        int before = e.state().turn();

        Resolution r = e.apply(Command.turnAround());

        assertEquals(Facing.LEFT, e.state().hero().facing());
        assertEquals(before + 1, e.state().turn());
        CombatEvent.Turned turned = Encounters.firstOf(r.events(), CombatEvent.Turned.class);
        assertNotNull(turned);
        assertEquals(CombatEvent.TurnReason.COMMAND, turned.reason());
    }

    @Test
    void aTurnTileBuysTheSameFlipInsideAPhraseForNoExtraTurn() {
        // "unless a tile grants it free" -- the Turn tile is that grant. It still
        // costs a turn to *bank*, so it is not free, it is fungible: you may pay
        // for the flip in advance and spend it mid-phrase, which is what lets a
        // phrase strike in both directions.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, TURN, CUT);
        e.apply(Command.add(1)); // Cut, resolves second
        e.apply(Command.add(0)); // Turn, resolves first
        Resolution r = e.apply(Command.execute());

        assertEquals(Facing.LEFT, e.state().hero().facing());
        assertEquals(1, Encounters.only(Encounters.phrase(r), CombatEvent.Turned.class).size(),
                "one flip, inside the phrase, on the same turn the phrase was spent");
    }

    @Test
    void aFreePlayTileIsBankedWithoutSpendingATurn() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, FEINT, CUT);
        assertTrue(FEINT.freePlay());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        int wispTile = wisp.tile();
        int before = e.state().turn();

        Resolution r = e.apply(Command.add(0));

        assertEquals(before, e.state().turn(), "free-play means exactly this: no turn passes");
        assertFalse(Encounters.has(r.events(), CombatEvent.EnemyPhaseBegan.class));
        assertEquals(wispTile, wisp.tile(), "and the board must not move either");
        CombatEvent.TileQueued queued = Encounters.firstOf(r.events(), CombatEvent.TileQueued.class);
        assertFalse(queued.costTurn(), "the stream has to say it was free, so the UI can too");
    }

    @Test
    void freePlayIsPaidForInCooldown() {
        // The turn is bought, not given. Feint's base cooldown is 3 and it carries
        // Free-Play's penalty of 3, so the tile that costs no time costs the most
        // charges of anything in the hand.
        assertEquals(TileType.FEINT.baseCooldown() + Enchantment.FREE_PLAY.cooldownPenalty(), FEINT.cooldown());
        assertTrue(FEINT.cooldown() > SWEEP.cooldown());
    }

    @Test
    void usingATileResetsItsCooldownEvenWhenItMisses() {
        // combat-design.md 1.2: "missing is punished twice". The Sweep below hits
        // nothing at all -- the Wisp is thirteen tiles away -- and pays in full.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, SWEEP);
        assertTrue(e.state().loadout().ready(0));

        e.apply(Command.add(0));
        Resolution r = e.apply(Command.execute());

        assertTrue(Encounters.has(Encounters.phrase(r), CombatEvent.Whiffed.class), "it must genuinely have missed");
        assertTrue(Encounters.has(r.events(), CombatEvent.TileSpent.class));
        assertFalse(e.state().loadout().ready(0), "a miss still dries the stroke out");
        assertEquals(1, e.state().loadout().charges(0), "reset to zero, then the turn's own recovery");
    }

    @Test
    void aDriedTileRecoversExactlyOneChargePerTurn() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, SWEEP, CUT);
        e.apply(Command.add(0));
        e.apply(Command.execute());

        // Sweep is cooldown 3, and it was used on the turn that took it to 1.
        assertEquals(1, e.state().loadout().charges(0));
        e.apply(Command.hold());
        assertEquals(2, e.state().loadout().charges(0));
        assertFalse(e.state().loadout().ready(0));
        e.apply(Command.hold());
        assertEquals(3, e.state().loadout().charges(0));
        assertTrue(e.state().loadout().ready(0), "three turns of recovery for a cooldown of three");
    }

    @Test
    void aBankedTileDoesNotRecoverWhileItSitsInTheColumn() {
        // Otherwise a long stanza would silently pre-charge itself for the next
        // one, and the cost of banking would leak away over the turns you spend
        // banking. See Loadout's class note.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, SWEEP, CUT);
        e.apply(Command.add(0));
        e.apply(Command.execute());
        e.apply(Command.hold());
        assertEquals(2, e.state().loadout().charges(0));

        // Cut has cooldown 0, so it can be banked and re-banked freely; use it to
        // pass turns while Sweep is banked... except Sweep is not ready. Bank Cut
        // instead and watch Sweep recover normally, then bank Sweep and stop.
        e.apply(Command.hold());
        assertTrue(e.state().loadout().ready(0));
        e.apply(Command.add(0));
        int frozen = e.state().loadout().charges(0);
        e.apply(Command.hold());
        assertEquals(frozen, e.state().loadout().charges(0), "a banked tile is in the hand, not on the sheet");
    }

    @Test
    void aTileStillDryingCannotBeBanked() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, SWEEP);
        e.apply(Command.add(0));
        e.apply(Command.execute());
        assertFalse(e.can(Command.add(0)));
        assertTrue(e.reject(Command.add(0)).contains("still drying"),
                "the UI has to be able to say why, not just that");
    }

    @Test
    void holdingPassesTheTurnToTheEnemies() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        int before = wisp.tile();
        e.apply(Command.hold());
        assertTrue(wisp.tile() < before);
    }

    @Test
    void costsTurnAgreesWithWhatActuallyHappens() {
        // The UI asks this before committing, so a disagreement between the
        // prediction and the rule is a class of bug that would only show up as a
        // desynced clock. Sweep it across every command.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, FEINT, CUT, SWEEP);
        Command[] script = {
                Command.add(1), Command.add(0), Command.reorder(0, 1),
                Command.remove(1), Command.add(2), Command.execute(),
                Command.turnAround(), Command.hold()
        };
        for (Command c : script) {
            boolean predicted = e.costsTurn(c);
            int before = e.state().turn();
            e.apply(c);
            assertEquals(predicted ? before + 1 : before, e.state().turn(), "costsTurn lied about " + c);
        }
    }
}
