package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether five queued tiles read as one phrase or as five separate events, and
 * whether the motion has a source.
 *
 * <h2>How "reads as one phrase" is asserted, since it is not obviously a testable
 * claim</h2>
 *
 * <p>combat-design.md 1.1a bought the five-slot Ink Stanza with an argument about
 * exactly this -- "five linked beats resolving without interruption, each one
 * flowing out of the last, is a qualitatively different thing to animate than
 * three" -- and {@link dev.starfall.combat.Overlap} calls itself "the field that
 * decides whether five queued tiles read as one phrase or as five separate
 * events". So it has to be checkable, and it is, as the conjunction of three
 * measurable properties. None of the three is sufficient alone; together they are
 * what the phrase means.
 *
 * <ol>
 *   <li><b>The carrier never lets go.</b> The trunk chain's weight is a continuous
 *       positive function across the whole bracket. A phrase in which each beat
 *       raised and dropped its own trunk would be five gestures however tightly
 *       they were packed in time, and this is the property that catches it.
 *       Sampled at 240 Hz, not asserted at the endpoints.</li>
 *   <li><b>The beats genuinely occupy each other's time.</b> The sum of the beats'
 *       durations exceeds the wall-clock span of the phrase, so at most instants
 *       more than one beat is live. Five beats played end to end would give a ratio
 *       of exactly 1.</li>
 *   <li><b>The next clause has begun before the last one has come to rest.</b> The
 *       hips of beat <i>i+1</i> start moving while the hand of beat <i>i</i> is
 *       still inside its settle -- which is STYLE.md 7.0.1 and 7.0.3 acting across
 *       the seam rather than inside one gesture.</li>
 * </ol>
 */
class PhrasingTest {

    private static CombatEngine fiveTileHand(int lane) {
        return Fixtures.lane(lane, Hero.WARDEN, EnemyArchetype.WISP, lane - 1,
                Tile.of(TileType.CUT), Tile.of(TileType.THRUST), Tile.of(TileType.SWEEP),
                Tile.of(TileType.CUT).withCooldown(0), Tile.of(TileType.THRUST).withCooldown(0));
    }

    @Test
    void aFiveBeatStanzaReadsAsOnePhraseAndNotAsFiveEvents() {
        CombatEngine engine = fiveTileHand(11);
        int hero = engine.state().hero().id();
        Schedule s = Fixtures.stanza(engine, 5);
        List<ScheduledBeat> stanza = s.bracket(ScheduledBeat.Bracket.STANZA);
        assertEquals(5, stanza.size(), "the stanza did not produce five beats");

        double from = stanza.get(0).start();
        double to = stanza.get(4).end();

        // 1. The carrier never lets go.
        for (double t = from + 1e-3; t < to; t += 1.0 / 240.0) {
            assertTrue(s.weight(hero, Chain.SPINE, t) >= Scheduler.CARRIER_HOLD - 1e-9,
                    "the trunk dropped to " + s.weight(hero, Chain.SPINE, t) + " at t=" + t
                            + ", which makes this five gestures rather than one");
        }

        // 2. The beats occupy each other's time.
        double sum = 0;
        for (ScheduledBeat b : stanza) {
            sum += b.duration();
        }
        double packing = sum / (to - from);
        assertTrue(packing > 1.15,
                "the beats barely overlap (packing " + packing + "); five beats played end to "
                        + "end would give exactly 1.0 and would read as five events");

        // 3. The next clause begins before the last one has come to rest.
        for (int i = 1; i < stanza.size(); i++) {
            ScheduledBeat prev = stanza.get(i - 1);
            ScheduledBeat next = stanza.get(i);
            double stillSettling = prev.end() + Chain.SWORD_ARM.settle().tip();
            assertTrue(next.start() < stillSettling - 1e-9,
                    "beat " + i + " starts at " + next.start() + ", after beat " + (i - 1)
                            + " has entirely come to rest at " + stillSettling);
        }
    }

    @Test
    void theHipLeadsTheHandInsideEveryBeat() {
        // STYLE.md 7.0.1: "the hip turns before the shoulder, which turns before the
        // elbow, which turns before the tip. The arm is never the subject; it is the
        // end of a sentence the body started. Movement that simply begins at the
        // effector reads instantly as a puppet, before any analysis."
        CombatEngine engine = fiveTileHand(11);
        int hero = engine.state().hero().id();
        Schedule s = Fixtures.stanza(engine, 5);

        for (ScheduledBeat beat : s.bracket(ScheduledBeat.Bracket.STANZA)) {
            double spine = firstAfter(s, hero, Chain.SPINE, beat.start());
            double clavicle = firstAfter(s, hero, Chain.CLAVICLE, beat.start());
            double arm = firstAfter(s, hero, Chain.SWORD_ARM, beat.start());
            assertTrue(spine < clavicle, "the shoulder led the hips in beat " + beat.index());
            assertTrue(clavicle < arm, "the hand led the shoulder in beat " + beat.index());
            assertTrue(arm - spine >= Timing.HIP_LEAD_MIN - 1e-9,
                    "the spiral in beat " + beat.index() + " is only " + (arm - spine)
                            + " s wide, under the readable floor");
        }
    }

    @Test
    void theHipLeadsTheHandAcrossAPhraseBoundaryAndNotOnlyInsideOneGesture() {
        // The harder half, and the one system2-debt.md E1 is about: "the poetry is an
        // event rather than a condition. The hip moves during the gesture and
        // returns." A spiral that restarts at every beat is five spirals. So for each
        // seam, the next beat's hips must already be moving while the previous beat's
        // hand is still inside its own settle -- the two clauses interlock rather
        // than queue.
        CombatEngine engine = fiveTileHand(11);
        int hero = engine.state().hero().id();
        Schedule s = Fixtures.stanza(engine, 5);
        List<ScheduledBeat> stanza = s.bracket(ScheduledBeat.Bracket.STANZA);

        for (int i = 1; i < stanza.size(); i++) {
            double hipsOfNext = firstAfter(s, hero, Chain.SPINE, stanza.get(i).start());
            double handOfPrevAtRest = lastEndBefore(s, hero, Chain.SWORD_ARM, stanza.get(i).start()
                    + stanza.get(i).duration()) + Chain.SWORD_ARM.settle().tip();
            assertTrue(hipsOfNext < handOfPrevAtRest,
                    "at seam " + i + " the hips of the next clause start at " + hipsOfNext
                            + ", after the previous hand is fully at rest at " + handOfPrevAtRest);
        }
    }

    @Test
    void theHeroPhraseAndTheEnemyPhaseInterlockRatherThanQueue() {
        // combat-design.md 3d.5's third correction: "a lane with three bodies
        // resolving in board order is as much a phrase as a five-tile stanza is."
        // The seam between the two brackets is a seam like any other and must not
        // read as the hero stopping and the enemy starting.
        CombatEngine engine = fiveTileHand(9);
        int hero = engine.state().hero().id();
        Schedule s = Fixtures.stanza(engine, 5);
        List<ScheduledBeat> stanza = s.bracket(ScheduledBeat.Bracket.STANZA);
        List<ScheduledBeat> enemy = s.bracket(ScheduledBeat.Bracket.ENEMY);
        assertTrue(!enemy.isEmpty(), "the enemy phase produced no beat at all");

        ScheduledBeat lastHero = stanza.get(stanza.size() - 1);
        ScheduledBeat firstEnemy = enemy.stream()
                .filter(b -> b.start() >= lastHero.start())
                .findFirst().orElseThrow();
        assertTrue(firstEnemy.start() < lastHero.end() + Chain.SWORD_ARM.settle().tip(),
                "the enemy waits for the hero to be entirely still: enemy at " + firstEnemy.start()
                        + ", hero at rest at " + (lastHero.end() + Chain.SWORD_ARM.settle().tip()));
        assertTrue(firstEnemy.contactStart() > lastHero.contactStart(),
                "and yet the two contacts must still be ordered");
        assertTrue(s.weight(hero, Chain.SPINE, firstEnemy.start()) > 0.0,
                "the hero's trunk has gone inert before the answer arrives");
    }

    @Test
    void theBladeTipIsStillDriftingAfterTheHipsHaveStopped() {
        // STYLE.md 7.0.3, stated as the thing a reviewer would look at: the tip of
        // the chain outlasts its root by a readable margin. system2-debt.md E3 asks
        // for the tip to be "still drifting perceptibly a quarter second after the
        // hips have stopped", and this is that number in the schedule rather than in
        // a capture.
        double hips = Chain.SPINE.settle().base();
        double tip = Chain.SWORD_ARM.settle().tip();
        assertTrue(tip - hips >= 0.25,
                "the tip outlasts the hips by only " + (tip - hips) + " s");
    }

    private static double firstAfter(Schedule s, int body, Chain chain, double t) {
        for (Directive.IkTarget d : s.chain(body, chain)) {
            if (d.at() >= t - 1e-9) {
                return d.at();
            }
        }
        throw new AssertionError("no " + chain + " directive on body " + body + " at or after " + t);
    }

    private static double lastEndBefore(Schedule s, int body, Chain chain, double t) {
        double end = Double.NEGATIVE_INFINITY;
        for (Directive.IkTarget d : s.chain(body, chain)) {
            if (d.at() < t) {
                end = Math.max(end, d.end());
            }
        }
        return end;
    }
}
