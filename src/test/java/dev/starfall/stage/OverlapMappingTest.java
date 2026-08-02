package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.Command;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.Focus;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Intent;
import dev.starfall.combat.Overlap;
import dev.starfall.combat.Phases;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overlap theorem, carried across the mapping into seconds.
 *
 * <p>The engine proved this about its own stream and stated the proof as a promise
 * about a layer that did not exist yet ({@link Overlap}'s class note):
 *
 * <blockquote>{@code Phases} guarantees a strictly positive wind-up, so a beat that
 * starts no earlier than the previous beat's {@code recoveryStart()} necessarily
 * contacts after the previous beat has contacted. <b>Contacts stay strictly ordered
 * no matter how the renderer scales the beats.</b></blockquote>
 *
 * <p>This layer is the renderer's scaling. It is the only thing in the project that
 * can falsify that promise, and it would falsify it by consuming more of the
 * previous recovery than the hint allows, or by scaling two beats so differently
 * that the arithmetic stops holding. So the theorem is swept over generated
 * schedules rather than checked on a few hand-built cases: every phase shape the
 * game contains, against every phase shape, at every legal overlap value, across
 * the whole 1-to-15 range of tile spans on both sides.
 */
class OverlapMappingTest {

    /** Every beat shape the game can produce, including the two only a death uses. */
    private static List<Phases> catalogue() {
        List<Phases> out = new ArrayList<>();
        for (TileType t : TileType.values()) {
            Phases p = Phases.of(t);
            if (!out.contains(p)) {
                out.add(p);
            }
        }
        for (Intent.Kind k : Intent.Kind.values()) {
            Phases p = Phases.of(k);
            if (!out.contains(p)) {
                out.add(p);
            }
        }
        out.add(Phases.DEATH);
        out.add(Phases.BURST);
        return out;
    }

    private static final int[] SPANS = {1, 2, 3, 4, 6, 9, 12, 15};

    private static ScheduledBeat beat(ScheduledBeat previous, Phases phases, int overlap, int span) {
        return ScheduledBeat.after(previous, 0, ScheduledBeat.Bracket.STANZA, 1, phases,
                new Overlap(overlap, Overlap.Limit.CONTINUES), new Focus(1, 0, span - 1));
    }

    @Test
    void contactsStayStrictlyOrderedUnderEveryLegalOverlapCombination() {
        // The sweep is 6 phase shapes squared, times 101 overlap values, times 8
        // spans squared -- about 230,000 placements. That is the point: the promise
        // was made "however the renderer scales the beats", so checking one scaling
        // would not be checking the promise.
        List<Phases> shapes = catalogue();
        int checked = 0;
        for (Phases first : shapes) {
            for (int spanA : SPANS) {
                ScheduledBeat a = beat(null, first, 0, spanA);
                for (Phases second : shapes) {
                    for (int spanB : SPANS) {
                        for (int overlap = 0; overlap <= Phases.WHOLE; overlap++) {
                            ScheduledBeat b = beat(a, second, overlap, spanB);
                            assertTrue(b.contactStart() > a.contactStart() + 1e-9,
                                    () -> "contacts coincided or inverted: " + a + " then " + b);
                            assertTrue(b.start() >= a.recoveryStart() - 1e-9,
                                    () -> "a beat ate into the previous beat's contact: " + a + " then " + b);
                            checked++;
                        }
                    }
                }
            }
        }
        assertTrue(checked > 100_000, "the sweep did not run: " + checked);
    }

    @Test
    void theSeparationBetweenTwoContactsIsNeverZeroEvenAtTheWidestLegalOverlap() {
        // Overlap.INDEPENDENT is 85 and never 100, because STYLE.md 10 "bans
        // simultaneity as such and not merely caused simultaneity". That is a claim
        // about the rules; here it has to survive becoming a number of seconds, and
        // the smallest gap it can produce is the one worth knowing.
        double worst = Double.MAX_VALUE;
        for (Phases first : catalogue()) {
            for (Phases second : catalogue()) {
                for (int spanA : SPANS) {
                    for (int spanB : SPANS) {
                        ScheduledBeat a = beat(null, first, 0, spanA);
                        ScheduledBeat b = beat(a, second, Overlap.INDEPENDENT, spanB);
                        worst = Math.min(worst, b.contactStart() - a.contactStart());
                    }
                }
            }
        }
        // Four frames at 60 Hz is the floor STYLE.md 7.1 uses for overlapping action
        // anywhere else, and nothing in this layer gets closer than that.
        assertTrue(worst > 4.0 / 60.0,
                "the closest two contacts can get is " + worst + " s, inside the readable band");
    }

    @Test
    void aForbiddenOverlapPutsTheNextBeatExactlyWhereTheLastOneEnded() {
        // Overlap.forbidden() means intoRecovery == 0, and there are three different
        // reasons for it -- footing, facing, board. All three map to the same
        // arithmetic and must, because the engine's refusal is about geometry and
        // not about how long anything takes.
        for (Overlap.Limit limit : List.of(Overlap.Limit.AWAITS_FOOTING,
                Overlap.Limit.AWAITS_FACING, Overlap.Limit.AWAITS_BOARD, Overlap.Limit.FIRST_BEAT)) {
            Overlap o = new Overlap(0, limit);
            assertTrue(o.forbidden());
            ScheduledBeat a = beat(null, Phases.TRAVEL, 0, 2);
            ScheduledBeat b = ScheduledBeat.after(a, 1, ScheduledBeat.Bracket.STANZA, 1,
                    Phases.STRIKE, o, new Focus(1, 0, 1));
            assertTrue(Math.abs(b.start() - a.end()) < 1e-9,
                    limit + " should leave the previous beat to finish, started at " + b.start()
                            + " against an end of " + a.end());
        }
    }

    @Test
    void everyContactInAWholeRealTurnIsStrictlyOrdered() {
        // The generated sweep proves the arithmetic; this proves the arithmetic is
        // what the scheduler actually runs, on streams the engine really emits --
        // hero phrase and enemy phase, with contacts arriving from Swung, Meeting
        // and the beats' own nominal instants.
        for (int lane : new int[]{5, 7, 9, 11, 13, 15}) {
            for (Hero hero : Hero.values()) {
                for (EnemyArchetype archetype : EnemyArchetype.values()) {
                    CombatEngine e = Fixtures.lane(lane, hero, archetype, Math.min(lane - 1, 3),
                            Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.SWEEP),
                            Tile.of(TileType.TURN), Tile.of(TileType.THRUST));
                    Schedule s = Fixtures.stanza(e, 5);
                    List<Double> contacts = s.contacts();
                    for (int i = 1; i < contacts.size(); i++) {
                        assertTrue(contacts.get(i) > contacts.get(i - 1) + 1e-9,
                                "lane " + lane + " " + hero + " vs " + archetype
                                        + ": contact " + i + " at " + contacts.get(i)
                                        + " did not follow " + contacts.get(i - 1)
                                        + "\n" + s.fingerprint());
                    }
                    assertFalse(contacts.isEmpty(), "a five-tile stanza produced no contact at all");
                }
            }
        }
    }

    @Test
    void aParryAndTheStrokeItAnswersDoNotLandTogether() {
        // The case STYLE.md 7.2 cares about most: "parry is a deflection curve, not
        // a collision". Two bodies synchronise on one Meeting instant, which means
        // the two skeletons peak together *by design* -- and the beats around it
        // still must not.
        CombatEngine e = Fixtures.lane(7, Hero.WARDEN, EnemyArchetype.BULWARK, 1,
                Tile.of(TileType.PARRY), Tile.of(TileType.CUT));
        Scheduler s = Fixtures.scheduler(e);
        Fixtures.run(e, s, Command.add(0), Command.execute(), Command.add(1), Command.execute());
        Schedule score = s.schedule();

        List<Double> contacts = score.contacts();
        for (int i = 1; i < contacts.size(); i++) {
            assertTrue(contacts.get(i) > contacts.get(i - 1) + 1e-9,
                    "contact " + i + " coincided:\n" + score.fingerprint());
        }
    }
}
