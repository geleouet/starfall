package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rhythm of the fight: what a Strikethrough promises, and what happens when
 * two cycles beat against each other.
 *
 * <p><b>The invariant, and it is the important thing in this file.</b> A
 * telegraph is a promise that the player can act on the information. If a body
 * leaves the position it threatened from inside the same step in which it
 * resolves, the telegraph announced a threat but never offered a target -- the
 * player is told where the blade is coming from and then finds nothing standing
 * there. So <em>every threatening body spends at least one complete hero turn in
 * the position it threatens from</em>, and
 * {@link #everyThreateningBodyStandsInThePositionItThreatensFromForAWholeHeroTurn}
 * asserts exactly that, for every archetype, off the event stream.
 *
 * <p>That property was violated by the engine's first draft, which fired the
 * give-ground step in the same instant as the blade. The visible symptom was far
 * narrower than the cause: the enemy cycle came out exactly two beats long,
 * every hero cadence of period two locked in antiphase with it, and a hero
 * alternating bank and execute could never land a Cut on a Wisp -- not rarely,
 * never, from half the starting phases. combat-design.md 3d.1 has the account.
 * The rest of this file pins the two consequences: contact is now bounded from
 * every starting phase, and {@code hold} re-phases a cadence that has locked.
 */
class CadenceTest {

    private static final int SWEEP_TURNS = 18;

    // -- the invariant ---------------------------------------------------------

    @Test
    void everyThreateningBodyStandsInThePositionItThreatensFromForAWholeHeroTurn() {
        // Every archetype, three hero cadences each, and two hands: one that can
        // kill (so deaths, blooms and counters interleave) and one that cannot (so
        // even the one-hit Warden survives long enough to run its whole cycle
        // several times). The Quick variant is in deliberately: a Quick body
        // publishes no telegraph, so it makes no promise about the turn before --
        // but it still owes the player the turn after, and it pays it.
        for (EnemyArchetype archetype : EnemyArchetype.values()) {
            for (int phase = 0; phase < 3; phase++) {
                assertAnswerable(duel(archetype, false), phase);
                assertAnswerable(duel(archetype, true), phase);
                assertAnswerable(harmless(archetype, false), phase);
                assertAnswerable(harmless(archetype, true), phase);
            }
        }
    }

    @Test
    void theInvariantAlsoHoldsOnACrowdedLaneWithEveryArchetypeOnIt() {
        // Five bodies resolving in board order, so give-ground steps, close-ins,
        // charges and explosive deaths all land on each other. The single-archetype
        // duels cannot produce a body whose declared step is refused because
        // another body took the tile first.
        for (int phase = 0; phase < 3; phase++) {
            assertAnswerable(CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                    .heroAt(0, Facing.RIGHT)
                    .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                    .enemy(EnemyArchetype.REACHER, 6, Facing.LEFT)
                    .enemy(EnemyArchetype.RUNNER, 8, Facing.LEFT)
                    .enemy(EnemyArchetype.WARDEN, 10, Facing.LEFT)
                    .enemy(EnemyArchetype.BULWARK, 12, Facing.LEFT)
                    .loadout(Tile.of(TileType.CUT), Tile.of(TileType.PARRY), Tile.of(TileType.STEP))
                    .heroHp(500)
                    .build()), phase);
        }
    }

    private static CombatEngine duel(EnemyArchetype archetype, boolean quick) {
        return CombatEngine.create(Encounters.duel(11, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(placement(archetype, quick))
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.PARRY), Tile.of(TileType.STEP))
                .heroHp(500)
                .build());
    }

    /** The same board with a hand that cannot kill, so nothing leaves the sweep early. */
    private static CombatEngine harmless(EnemyArchetype archetype, boolean quick) {
        return CombatEngine.create(Encounters.duel(11, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(placement(archetype, quick))
                .loadout(Tile.of(TileType.STEP), Tile.of(TileType.BACK_STEP))
                .heroHp(500)
                .build());
    }

    private static EncounterSpec.Placement placement(EnemyArchetype archetype, boolean quick) {
        EncounterSpec.Placement p = EncounterSpec.Placement.of(archetype, 5, Facing.LEFT);
        return quick ? p.with(Trait.QUICK) : p;
    }

    /**
     * Plays {@code SWEEP_TURNS} turns and checks the promise from both sides.
     *
     * <p>Positions are reconstructed by replaying {@link CombatEvent.Moved} rather
     * than read off the board, so the check is against what the stream told the
     * animation layer. A body that moved without saying so fails here too, which
     * is worth having: the whole telegraph contract is a claim about the stream.
     */
    private static void assertAnswerable(CombatEngine engine, int phase) {
        int heroId = engine.state().hero().id();
        Map<Integer, Integer> where = new LinkedHashMap<>();
        for (Combatant c : engine.state().all()) {
            where.put(c.id(), c.tile());
        }
        List<CombatState.Telegraph> published = engine.state().strikethroughs();

        for (int turn = 0; turn < SWEEP_TURNS && !engine.state().outcome().over(); turn++) {
            // The turn before: nothing published last turn may have moved before the
            // hero gets to act on it.
            for (CombatState.Telegraph t : published) {
                Combatant body = engine.state().byId(t.entity());
                if (body.alive()) {
                    assertEquals(t.fromTile(), body.tile(),
                            "a Strikethrough was published from tile " + t.fromTile()
                                    + " and the body was somewhere else when the hero came to answer it\n"
                                    + engine.state().fingerprint());
                }
            }

            Resolution r = engine.apply(script(engine, turn, phase));

            // The turn after: nothing may leave the tile it struck from inside the
            // step in which it struck.
            Map<Integer, Integer> struckFrom = new LinkedHashMap<>();
            boolean inEnemyPhase = false;
            for (CombatEvent event : r.events()) {
                if (event instanceof CombatEvent.EnemyPhaseBegan) {
                    inEnemyPhase = true;
                } else if (event instanceof CombatEvent.EnemyPhaseEnded) {
                    inEnemyPhase = false;
                } else if (event instanceof CombatEvent.Moved m) {
                    where.put(m.entity(), m.toTile());
                } else if (event instanceof CombatEvent.Swung s && inEnemyPhase && s.attacker() != heroId) {
                    struckFrom.put(s.attacker(), where.get(s.attacker()));
                }
            }
            for (Map.Entry<Integer, Integer> struck : struckFrom.entrySet()) {
                Combatant body = engine.state().byId(struck.getKey());
                if (!body.alive()) {
                    continue;
                }
                String said = body + " threatened from tile " + struck.getValue()
                        + " and was not standing there when the hero's next turn began\n"
                        + engine.state().fingerprint();
                assertEquals(struck.getValue(), body.tile(), said);
                assertEquals(struck.getValue(), where.get(struck.getKey()), said);
            }
            published = engine.state().strikethroughs();
        }
    }

    /** Bank round-robin, execute, hold -- offset by {@code phase} so every parity is played. */
    private static Command script(CombatEngine engine, int turn, int phase) {
        if (turn < phase) {
            return Command.hold();
        }
        int step = turn - phase;
        if (step % 3 != 2) {
            for (int i = 0; i < engine.state().loadout().size(); i++) {
                int candidate = (i + step) % engine.state().loadout().size();
                if (engine.can(Command.add(candidate))) {
                    return Command.add(candidate);
                }
            }
        }
        return engine.can(Command.execute()) ? Command.execute() : Command.hold();
    }

    // -- the bug that found the invariant --------------------------------------

    @Test
    void aHeroAlternatingBankAndExecuteReachesAWispFromEveryStartingPhase() {
        // The original non-terminating loop, tested from every phase rather than
        // from the one that happened to fail. Two dimensions of phase: where the
        // Wisp starts, which sets where in its three-beat cycle the fight opens,
        // and how many turns the hero waits first, which sets where in its own.
        List<String> worst = new ArrayList<>();
        int longest = 0;
        for (int distance = 2; distance <= 10; distance++) {
            for (int lead = 0; lead <= 4; lead++) {
                int contact = turnsToFirstContact(distance, lead, 2, 60);
                assertTrue(contact > 0,
                        "no contact in 60 turns from distance " + distance + ", lead " + lead
                                + " -- that is the phase-lock back again");
                assertTrue(contact <= distance + 4,
                        "distance " + distance + ", lead " + lead + " took " + contact + " turns");
                if (contact > longest) {
                    longest = contact;
                    worst.clear();
                }
                if (contact == longest) {
                    worst.add("d" + distance + "/lead" + lead);
                }
            }
        }
        assertEquals(11, longest,
                "the bound over every phase on an 11-tile lane, reached at " + worst
                        + " -- almost all of it is the walk in, since the Wisp starts ten tiles out");
    }

    @Test
    void holdingIsHowAHeroBreaksAPhaseLockOfItsOwnMaking() {
        // Tactical patience, promoted from remedy to tactic. A three-beat hero
        // cadence -- bank, bank, execute -- can still line up against the enemy's
        // three-beat cycle so that every execution falls on the one turn in three
        // when the body is not adjacent. That is a real tactical mistake now rather
        // than an unwinnable board: one held turn shifts the phase and the same
        // cadence connects immediately. See combat-design.md 3d.1's last paragraph.
        assertEquals(-1, turnsToFirstContact(4, 0, 3, 60),
                "this is the locked phase: three beats against three beats, in antiphase");
        assertEquals(4, turnsToFirstContact(4, 1, 3, 60),
                "one turn of patience and the same cadence lands on the next execution");
    }

    /**
     * Turns until the hero's blade first touches the Wisp, or {@code -1} if it
     * never does inside {@code limit}.
     *
     * @param lead   turns held before the cadence starts, i.e. the hero's phase
     * @param period the cadence: {@code period - 1} tiles banked, then an execution
     */
    private static int turnsToFirstContact(int distance, int lead, int period, int limit) {
        Tile[] hand = new Tile[period - 1];
        for (int i = 0; i < hand.length; i++) {
            hand[i] = Tile.of(TileType.CUT);
        }
        CombatEngine e = CombatEngine.create(Encounters.duel(11, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, distance, Facing.LEFT)
                .loadout(hand)
                .heroHp(500)
                .build());
        int heroId = e.state().hero().id();
        int wispId = Encounters.enemy(e, EnemyArchetype.WISP).id();
        int turns = 0;
        for (int i = 0; i < lead; i++) {
            e.apply(Command.hold());
            turns++;
        }
        int banked = 0;
        while (turns < limit && !e.state().outcome().over()) {
            Command c;
            if (banked < period - 1 && e.can(Command.add(banked))) {
                c = Command.add(banked);
                banked++;
            } else {
                c = Command.execute();
                banked = 0;
            }
            Resolution r = e.apply(c);
            turns++;
            for (CombatEvent.Hit h : Encounters.only(r.events(), CombatEvent.Hit.class)) {
                if (h.attacker() == heroId && h.target() == wispId) {
                    return turns;
                }
            }
        }
        return -1;
    }
}
