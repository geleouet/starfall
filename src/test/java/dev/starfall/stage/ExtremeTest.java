package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.Command;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Focus;
import dev.starfall.combat.Force;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Overlap;
import dev.starfall.combat.Phases;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md 7.2 -- "this is where the aesthetic dies".
 *
 * <p>The section is a list of negatives and 7.0 warns that satisfying all of them
 * is not the same as succeeding, so what is checked here is only the half a
 * scheduler can be responsible for: that it never hands the renderer a command
 * that <em>cannot</em> be drawn within the law. A teleport is only a teleport if
 * something is asked to arrive in no time; a knockback is only a launch if
 * something is asked to arrive too fast; a freeze is only a freeze if the time
 * scale is asked to reach zero.
 */
class ExtremeTest {

    // -- knockback -------------------------------------------------------------

    @Test
    void aShovedBodyIsCarriedOverAboutEightTenthsOfASecond() {
        // "Knockback is a drift, not a launch. A struck figure should be carried
        // backward like a sheet of silk caught in wind, arriving over ~0.8 s."
        // The number falls out of BASE_BEAT rather than being applied on top of it:
        // a shove resolves inside a TRAVEL beat and the carry is its contact plus its
        // recovery, which is 70 parts of 1.12 s.
        CombatEngine engine = Fixtures.lane(7, Hero.WARDEN, EnemyArchetype.WISP, 1,
                Tile.of(TileType.STEP));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s,
                Command.add(0), Command.execute());
        Schedule score = s.schedule();

        CombatEvent.Shoved shove = Fixtures.only(events, CombatEvent.Shoved.class).get(0);
        assertTrue(shove.gaveGround(), "the fixture did not actually shove anybody");
        CombatEvent.Moved carried = Fixtures.only(events, CombatEvent.Moved.class).stream()
                .filter(m -> m.entity() == shove.shoved())
                .findFirst().orElseThrow();
        assertEquals(Force.DRIFT, carried.force(), "a shove is a drift, never a launch");

        Directive.Impulse hem = score.of(Directive.Impulse.class).stream()
                .filter(d -> d.body() == shove.shoved() && d.region() == Region.CLOTH_HEM)
                .filter(d -> d.duration() > 0.5)
                .findFirst().orElseThrow(() -> new AssertionError("nothing carries the hem"));
        assertTrue(hem.duration() > 0.72 && hem.duration() < 0.88,
                "the carry takes " + hem.duration() + " s against STYLE.md 7.2's ~0.8 s");

        // "Streaming ahead is a displacement, not an arrival time... measure the
        // growing tip-to-root offset during the strike, not who gets there first."
        // So the cloth and the hair are thrown along the travel, never against it.
        double travel = Math.signum(carried.toTile() - carried.fromTile());
        for (Directive.Impulse d : score.of(Directive.Impulse.class)) {
            if (d.body() == shove.shoved() && d.duration() > 0.5) {
                assertEquals(travel, Math.signum(d.dirX()), 1e-9,
                        d.region() + " is thrown against the direction of travel");
            }
        }
    }

    @Test
    void theCarriedBodyIsNeverDrivenHarderThanTheBodyThatShovedIt() {
        // Force's own surprise, and STYLE.md 7.2's reason for it: "a launch is the
        // failure mode; a deliberate stride has more muscle behind it than being
        // carried does." A shove really must move less cloth than a step.
        assertTrue(Region.CLOTH_HEM.magnitude(Force.DRIFT) < Region.CLOTH_HEM.magnitude(Force.DRIVE));
        assertTrue(Region.HAIR.magnitude(Force.DRIFT) < Region.HAIR.magnitude(Force.DRIVE));
        assertEquals(0.10, Region.CLOTH_HEM.magnitude(Force.NONE) / 0.90, 1e-9,
                "a swap is an interpenetration with no impact at all");
    }

    // -- the collapse of distance ----------------------------------------------

    @Test
    void aChargerCollapsingTheLaneIsFasterPerTileAndStillNotATeleport() {
        // combat-design.md 2.4 names the Runner "the extreme-motion test case", and
        // 3d.5's first correction is that a Runner collapsing thirteen tiles and a
        // Wisp stepping one "are the same subject and completely different shots".
        // Two failure modes bracket this: a constant beat length makes the collapse
        // twelve times a step's speed, which strobes; a length linear in tiles makes
        // it exactly a step's speed, which is not a charge.
        CombatEngine engine = Fixtures.lane(15, Hero.WARDEN, EnemyArchetype.RUNNER, 14,
                Tile.of(TileType.CUT));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s,
                Command.hold(), Command.hold(), Command.hold());
        Schedule score = s.schedule();

        CombatEvent.Moved charge = Fixtures.only(events, CombatEvent.Moved.class).stream()
                .filter(m -> m.reason() == CombatEvent.MoveReason.CHARGE)
                .max((a, b) -> Integer.compare(Math.abs(a.delta()), Math.abs(b.delta())))
                .orElseThrow(() -> new AssertionError("the Runner never charged"));
        assertTrue(Math.abs(charge.delta()) >= 5, "the charge only covered " + charge.delta() + " tiles");
        assertEquals(Force.HEADLONG, charge.force());

        ScheduledBeat beat = score.beats().stream()
                .filter(b -> b.actor() != engine.state().hero().id())
                .max((a, b) -> Double.compare(a.duration(), b.duration()))
                .orElseThrow();
        double unit = Timing.beatSeconds(2);
        assertTrue(beat.duration() > unit * 1.4,
                "the collapse takes " + beat.duration() + " s, barely more than a step's " + unit);
        assertTrue(beat.tilesPerSecond() > 1.0 / (unit * 0.70),
                "the collapse is no faster per tile than a step, so it is not a charge");
        assertTrue(beat.tilesPerSecond() < 10.0,
                "the collapse runs at " + beat.tilesPerSecond()
                        + " tiles/s, fast enough to strobe rather than smear");
    }

    @Test
    void nothingIsEverAskedToArriveInstantly() {
        // The blunt anti-teleport check. Every directive that moves something has a
        // duration a renderer can interpolate over; a zero would be the snap 7.2's
        // first line forbids, and a schedule is the only place it could be
        // introduced.
        CombatEngine engine = Fixtures.lane(15, Hero.WARDEN, EnemyArchetype.RUNNER, 14,
                Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.THRUST),
                Tile.of(TileType.SWEEP), Tile.of(TileType.TURN));
        Schedule score = Fixtures.stanza(engine, 5);
        for (Directive d : score.directives()) {
            assertTrue(d.duration() >= 0.05,
                    "a " + d.duration() + " s directive is an instant: " + d.describe());
        }
    }

    // -- two beats overlapping maximally ---------------------------------------

    @Test
    void twoBeatsAtTheWidestLegalOverlapStillContactInOrder() {
        // Overlap.INDEPENDENT is 85 and is the most two beats may ever share. The
        // engine's reason for stopping short of 100 is that STYLE.md 10 "bans
        // simultaneity as such and not merely caused simultaneity", and this is that
        // rule surviving the conversion into seconds.
        for (Phases first : List.of(Phases.STRIKE, Phases.TRAVEL, Phases.GUARD,
                Phases.WIND_AROUND, Phases.BREATH, Phases.REACH)) {
            for (Phases second : List.of(Phases.STRIKE, Phases.TRAVEL, Phases.GUARD,
                    Phases.WIND_AROUND, Phases.BREATH, Phases.REACH)) {
                ScheduledBeat a = ScheduledBeat.after(null, 0, ScheduledBeat.Bracket.ENEMY, 1,
                        first, Overlap.firstBeat(), new Focus(1, 0, 1));
                ScheduledBeat b = ScheduledBeat.after(a, 1, ScheduledBeat.Bracket.ENEMY, 2,
                        second, Overlap.unrelated(), new Focus(2, 8, 9));
                assertTrue(b.contactStart() > a.contactStart(),
                        first + " then " + second + " contact together at maximal overlap");
                assertTrue(b.start() < a.end(),
                        first + " then " + second + " do not overlap at all, so the hint was ignored");
            }
        }
    }

    @Test
    void twoBodiesAtOppositeEndsOfALongLaneOverlapButDoNotCoincide() {
        CombatEngine engine = CombatEngine.create(dev.starfall.combat.EncounterSpec
                .builder(15, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .heroHp(60)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 14, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT))
                .build());
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.hold());
        Schedule score = s.schedule();

        List<ScheduledBeat> enemy = score.bracket(ScheduledBeat.Bracket.ENEMY);
        assertTrue(enemy.size() >= 2, "only " + enemy.size() + " enemy beats");
        ScheduledBeat a = enemy.get(0);
        ScheduledBeat b = enemy.get(1);
        assertEquals(Overlap.Limit.UNRELATED, b.overlap().limit());
        assertTrue(b.start() < a.end(), "two unrelated bodies played strictly in sequence");
        assertTrue(b.contactStart() > a.contactStart() + 4.0 / 60.0,
                "the two contacts are " + (b.contactStart() - a.contactStart())
                        + " s apart, inside the band a reviewer would read as together");
    }

    // -- the banned tools ------------------------------------------------------

    @Test
    void theOnlyTimeManipulationIsStyleSevenPointThreesHeldBreath() {
        // "No impact freeze. No screen shake. No hitstop." 7.3 replaces all three
        // with one soft ramp, and the vocabulary has to make the banned versions
        // unsayable rather than merely unused: a TimeRamp is the sole global
        // directive and its values are checked here.
        CombatEngine engine = Fixtures.lane(9, Hero.WARDEN, EnemyArchetype.BULWARK, 1,
                Tile.of(TileType.CUT), Tile.of(TileType.THRUST), Tile.of(TileType.SWEEP));
        Schedule score = Fixtures.stanza(engine, 3);
        List<Directive.TimeRamp> ramps = score.of(Directive.TimeRamp.class);
        assertTrue(!ramps.isEmpty(), "nothing marked the impact at all");
        double previous = Double.NEGATIVE_INFINITY;
        for (Directive.TimeRamp r : ramps) {
            assertEquals(Timing.HELD_BREATH_SCALE, r.scale(), 1e-9);
            assertEquals(Timing.HELD_BREATH_SECONDS, r.duration(), 1e-9);
            assertTrue(r.scale() > 0.5, "a ramp to " + r.scale() + " is a freeze");
            assertTrue(r.at() >= previous + Timing.HELD_BREATH_SECONDS - 1e-9,
                    "two held breaths overlap, which compounds into a hitstop");
            previous = r.at();
        }
    }

    @Test
    void everySwungBladeGetsATrailSoFastMotionSmearsRatherThanStrobing() {
        // "Fast motion should smear, not strobe. A blade crossing the frame in 3
        // frames must leave a continuous luminous ribbon." The scheduler cannot draw
        // the ribbon; it can guarantee the renderer is told to, and told over 5's
        // ~0.4 s rather than for a frame.
        CombatEngine engine = Fixtures.lane(9, Hero.WARDEN, EnemyArchetype.WISP, 3,
                Tile.of(TileType.CUT), Tile.of(TileType.THRUST), Tile.of(TileType.SWEEP));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s,
                Command.add(0), Command.add(1), Command.add(2), Command.execute());
        Schedule score = s.schedule();

        int swings = Fixtures.only(events, CombatEvent.Swung.class).size();
        List<Directive.Ink> trails = score.of(Directive.Ink.class).stream()
                .filter(d -> d.kind() == Directive.InkKind.TRAIL)
                .toList();
        assertEquals(swings, trails.size(), "a stroke went without a trail");
        for (Directive.Ink t : trails) {
            assertEquals(Scheduler.TRAIL_SECONDS, t.duration(), 1e-9);
            assertTrue(t.dirY() != 0.0,
                    "a trail with no vertical component is a straight line, which STYLE.md 5 "
                            + "fails as a generic slash VFX");
        }
    }

    @Test
    void aDeathKeepsSpreadingAfterTheBodyHasGiven() {
        // STYLE.md 7.3 and 3: the ink is a bloom on wet paper, not a fall. Dissolve
        // counts its length in beats because the engine owns no clock; this is the
        // one place a beat becomes a second.
        CombatEngine engine = Fixtures.lane(7, Hero.WARDEN, EnemyArchetype.WARDEN, 1,
                Tile.of(TileType.CUT).withDamage(9));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s, Command.add(0), Command.execute());
        Schedule score = s.schedule();

        CombatEvent.Died died = Fixtures.only(events, CombatEvent.Died.class).get(0);
        Directive.Ink dissolve = score.of(Directive.Ink.class).stream()
                .filter(d -> d.kind() == Directive.InkKind.DISSOLVE && d.body() == died.entity())
                .findFirst().orElseThrow();
        assertTrue(dissolve.duration() >= died.dissolve().spans() * Timing.BASE_BEAT - 1e-9,
                "the dissolve is shorter than the beats the engine counted for it");
        assertEquals(died.dissolve().along().step(), Math.signum(dissolve.dirX()), 1e-9,
                "the ink runs the wrong way; a body cut down from the left sheds to the right");

        // And the figure lets go before the ink has finished: the pose goes slack
        // inside the dissolve rather than at the end of it.
        Directive.PoseChange slack = score.of(Directive.PoseChange.class).stream()
                .filter(d -> d.body() == died.entity() && d.stance() == Stance.SLACK)
                .findFirst().orElseThrow();
        assertTrue(slack.end() < dissolve.end(), "the body outlasts its own ink");
    }
}
