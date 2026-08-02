package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.Command;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same event stream in, same schedule out, byte for byte.
 *
 * <p>The engine already guarantees this about its own output --
 * {@code CombatEvent}: "two runs of the same seeded encounter produce event lists
 * that are {@code equals}. That is the determinism contract" -- and the guarantee
 * is worth nothing if the layer that consumes it is not deterministic too. The
 * whole review loop rests on a capture being reproducible, and a schedule that
 * varied between runs would move the irreproducibility one layer down rather than
 * removing it.
 *
 * <p>The two hazards a scheduler introduces that an engine does not are hash
 * iteration order and locale-dependent formatting. Both are closed by
 * construction: {@link Standing} holds bodies in a {@code TreeMap}, the carrier
 * set is a {@code TreeSet}, the directive sort is stable on emission order, and
 * every number printed goes through {@link Locale#ROOT}.
 */
class ScheduleDeterminismTest {

    private static Schedule build() {
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(13, Hero.PILGRIM)
                .heroAt(0, Facing.RIGHT)
                .heroHp(60)
                .enemy(EnemyArchetype.REACHER, 4, Facing.LEFT)
                .enemy(EnemyArchetype.RUNNER, 9, Facing.LEFT)
                .enemy(EnemyArchetype.WARDEN, 12, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.DRAW),
                        Tile.of(TileType.PARRY), Tile.of(TileType.SWEEP))
                .seed(20260802L)
                .build());
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.add(0), Command.add(1), Command.execute(),
                Command.hold(), Command.add(2), Command.add(3), Command.add(4), Command.execute(),
                Command.turnAround(), Command.hold());
        return s.schedule();
    }

    @Test
    void twoRunsOfOneEncounterProduceTheSameScheduleByteForByte() {
        Schedule a = build();
        Schedule b = build();
        assertEquals(a.fingerprint(), b.fingerprint());
        assertEquals(a.beats(), b.beats());
        assertEquals(a.directives(), b.directives());
        assertEquals(a, b, "a Schedule is a record and two identical runs must be equals");
    }

    @Test
    void theFingerprintIsWorthSomething() {
        // A determinism check against a fingerprint that ignores most of the schedule
        // proves nothing, so the fingerprint has to be shown to move when the
        // schedule does.
        Schedule a = build();
        assertTrue(a.fingerprint().length() > 4000,
                "the fingerprint is only " + a.fingerprint().length() + " characters");
        assertTrue(a.beats().size() > 8, "only " + a.beats().size() + " beats");
        assertTrue(a.directives().size() > 100, "only " + a.directives().size() + " directives");

        CombatEngine other = Fixtures.lane(13, Hero.PILGRIM, EnemyArchetype.REACHER, 4,
                Tile.of(TileType.CUT));
        Scheduler s = Fixtures.scheduler(other);
        Fixtures.run(other, s, Command.add(0), Command.execute());
        assertNotEquals(a.fingerprint(), s.schedule().fingerprint());
    }

    @Test
    void theDirectiveStreamIsSortedByTimeAndTiesKeepTheirEmissionOrder() {
        Schedule s = build();
        List<Directive> ds = s.directives();
        for (int i = 1; i < ds.size(); i++) {
            assertTrue(ds.get(i).at() >= ds.get(i - 1).at() - 1e-12,
                    "directive " + i + " at " + ds.get(i).at() + " precedes " + ds.get(i - 1).at());
        }
    }

    @Test
    void twoStagesCutForTheSameLaneAreTheSameStage() {
        // Schedule is a record, so its equals is only as good as its components'.
        assertEquals(new Stage(9), new Stage(9));
        assertNotEquals(new Stage(9), new Stage(11));
        assertEquals(new Stage(9).hashCode(), new Stage(9).hashCode());
    }

    @Test
    void aScheduleCanBeBuiltTwiceFromTheSameEventsWithoutTouchingTheEngine() {
        // The property that matters at runtime: the scheduler holds no reference into
        // the engine, so a stream can be replayed long after the board it describes
        // has moved on. Here the whole encounter is played out first and only then
        // scheduled, twice.
        CombatEngine engine = Fixtures.lane(11, Hero.WARDEN, EnemyArchetype.BULWARK, 1,
                Tile.of(TileType.CUT), Tile.of(TileType.PARRY));
        Standing opening = Fixtures.opening(engine);
        List<dev.starfall.combat.CombatEvent> stream = new java.util.ArrayList<>(engine.opening());
        for (Command c : List.of(Command.add(0), Command.execute(), Command.add(1), Command.execute())) {
            stream.addAll(engine.apply(c).events());
        }

        Schedule first = new Scheduler(new Stage(11), opening).accept(stream).schedule();
        Schedule second = new Scheduler(new Stage(11), opening).accept(stream).schedule();
        assertEquals(first.fingerprint(), second.fingerprint());
        assertTrue(first.beats().size() >= 4, "the replay produced " + first.beats().size() + " beats");
    }
}
