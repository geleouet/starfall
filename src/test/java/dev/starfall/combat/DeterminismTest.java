package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The property the rest of the project is built on.</b>
 *
 * <p>Every review loop in Starfall is "run the scene, look at the frames, change
 * one thing, run it again". That is worthless if the fight underneath drifts, so
 * the same encounter replayed with the same commands has to produce not merely
 * the same outcome but the same beats in the same order. Comparing event lists
 * rather than final board states is deliberate: two runs can agree on where
 * everybody ended up and disagree about how they got there, and the animation
 * layer consumes the how.
 *
 * <p>Resolution contains no randomness at all -- enemies resolve in ascending
 * tile index, which is a total order because one tile holds one body, so there is
 * never even a tie to break. The seed governs <em>composition</em> only, and the
 * split is tested here in both directions.
 */
class DeterminismTest {

    private static EncounterSpec spec(Hero hero) {
        return EncounterSpec.builder(11, hero)
                .heroAt(1, Facing.RIGHT)
                .heroHp(40)
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.SWEEP),
                        Tile.of(TileType.TURN), Tile.of(TileType.DRAW), Tile.of(TileType.FEINT))
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .enemy(EnemyArchetype.REACHER, 6, Facing.LEFT)
                .enemy(EnemyArchetype.WARDEN, 8, Facing.LEFT)
                .enemy(EnemyArchetype.RUNNER, 10, Facing.LEFT)
                .seed(20260802L)
                .build();
    }

    /**
     * A long, varied script: every command form, both movement verbs' entry point,
     * and enough turns for statuses, blooms and charges to interleave. Anything
     * order-dependent that is not actually ordered shows up here as a diff.
     */
    private static List<CombatEvent> play(Hero hero) {
        CombatEngine e = CombatEngine.create(spec(hero));
        List<CombatEvent> log = new ArrayList<>(e.opening());
        int step = 0;
        while (!e.state().outcome().over() && step < 60) {
            Command c = pick(e, step++);
            log.addAll(e.apply(c).events());
        }
        return log;
    }

    /** Deterministic, board-aware, and deliberately not random. */
    private static Command pick(CombatEngine e, int step) {
        if (e.state().stanza().size() >= 3 || (step % 4 == 3 && !e.state().stanza().isEmpty())) {
            return Command.execute();
        }
        for (int i = 0; i < e.state().loadout().size(); i++) {
            int candidate = (i + step) % e.state().loadout().size();
            if (e.can(Command.add(candidate))) {
                return Command.add(candidate);
            }
        }
        return e.can(Command.execute()) ? Command.execute() : Command.hold();
    }

    @Test
    void theSameEncounterReplayedProducesTheSameBeatsInTheSameOrder() {
        for (Hero hero : Hero.values()) {
            List<CombatEvent> first = play(hero);
            List<CombatEvent> second = play(hero);
            assertEquals(first.size(), second.size(), hero + ": the two runs are not even the same length");
            for (int i = 0; i < first.size(); i++) {
                assertEquals(first.get(i), second.get(i), hero + ": the runs diverge at beat " + i);
            }
            assertTrue(first.size() > 60, hero + ": that script should have produced a real fight");
        }
    }

    @Test
    void theSameEncounterReplayedEndsOnAnIdenticalBoard() {
        CombatEngine a = CombatEngine.create(spec(Hero.WARDEN));
        CombatEngine b = CombatEngine.create(spec(Hero.WARDEN));
        for (int step = 0; step < 40; step++) {
            if (a.state().outcome().over()) {
                break;
            }
            Command c = pick(a, step);
            a.apply(c);
            b.apply(c);
            assertEquals(a.state().fingerprint(), b.state().fingerprint(), "diverged on step " + step);
        }
    }

    @Test
    void thetwoHeroesDivergeBecauseTheirVerbsDoAndNothingElseDoes() {
        // The control that makes the replay test mean something: the engine is not
        // deterministic because it is inert. Same lane, same hand, same script, one
        // variable changed, and the fights differ.
        assertNotEquals(play(Hero.WARDEN), play(Hero.PILGRIM));
    }

    @Test
    void aSeededWaveIsTheSameWaveEveryTime() {
        EncounterSpec a = EncounterSpec.wave(11, Hero.WARDEN, 4, 99L);
        EncounterSpec b = EncounterSpec.wave(11, Hero.WARDEN, 4, 99L);
        assertEquals(a.enemies(), b.enemies());
        assertEquals(CombatEngine.create(a).state().fingerprint(),
                CombatEngine.create(b).state().fingerprint());
    }

    @Test
    void differentSeedsGenerallyGiveDifferentWaves() {
        // Not "always" -- a small lane with few slots can collide, and a test that
        // asserted otherwise would be asserting a property of the hash rather than
        // of the generator. What is required is that the seed is actually consulted.
        List<List<EncounterSpec.Placement>> seen = new ArrayList<>();
        for (long seed = 0; seed < 12; seed++) {
            seen.add(EncounterSpec.wave(15, Hero.PILGRIM, 5, seed).enemies());
        }
        assertTrue(seen.stream().distinct().count() > 8,
                "twelve seeds should not collapse to a handful of boards: " + seen);
    }

    @Test
    void aWaveAlwaysOpensWithAnApproach() {
        // Composition rule, and the reason the generator is allowed to be random at
        // all: a fight that starts with something already inside your guard has no
        // approach to dramatise, which is the whole argument for the long lane
        // (combat-design.md 1.6).
        for (long seed = 0; seed < 40; seed++) {
            EncounterSpec s = EncounterSpec.wave(15, Hero.WARDEN, 5, seed);
            for (EncounterSpec.Placement p : s.enemies()) {
                assertTrue(p.tile() - s.heroTile() >= 2,
                        "seed " + seed + " opened with " + p.archetype() + " at " + p.tile());
            }
            assertTrue(s.enemies().size() <= s.lane().maxSimultaneousEnemies());
        }
    }

    @Test
    void resolutionNeverConsultsTheRandomSource() {
        // Stated as a measurement rather than as a comment: the splitmix state is
        // part of the fingerprint, so if anything in the fight rolled a die this
        // would move.
        CombatEngine e = CombatEngine.create(spec(Hero.PILGRIM));
        long before = e.state().rng().seedState();
        for (int step = 0; step < 30 && !e.state().outcome().over(); step++) {
            e.apply(pick(e, step));
        }
        assertEquals(before, e.state().rng().seedState(),
                "the fight itself must be a pure function of the board and the commands");
    }
}
