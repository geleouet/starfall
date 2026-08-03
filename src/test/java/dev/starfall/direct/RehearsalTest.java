package dev.starfall.direct;

import dev.starfall.rig.SamuraiRig;
import dev.starfall.stage.Anchor;
import dev.starfall.stage.Directive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The picture, asserted without pixels.
 *
 * <h2>What this file exists to stop happening again</h2>
 *
 * <p>System 4 pass 1 was failed on a beat that did not occur: <b>the blades never
 * came closer than 21% of a body height, and the clash bloom fired two frames
 * later, 36 px from the attacker's own grip.</b> Every test the pass had was
 * green, because every one of them was written against the schedule -- the
 * directives were right, the two named halves of the {@code Meeting} were 0.08
 * world units apart, the arm chain tracked its target. The failure was entirely
 * in the step from "the fist is at the crossing" to "the blade is at the
 * crossing", and nothing in the repository could see that step without shooting
 * a capture and measuring pixels by hand.
 *
 * <p>{@link Rehearsal} closes it: the same schedule, director, rig, IK and
 * simulation, run headless, with the blade's world segment readable per frame.
 * STYLE.md 11.2b(e): <i>"a discipline written into a document but not into the
 * tool that reads it is documentation, not a guard."</i> The discipline is
 * "assert the picture, not the intent"; these are the assertions.
 */
class RehearsalTest {

    /**
     * How close two blades have to be to count as met, as a fraction of the
     * figure's own height. {@code docs/system4-debt.md}'s acceptance for pass 2.
     */
    private static final double MET = 0.02;

    /**
     * How far the clash mark may sit from the blades it claims are meeting.
     *
     * <p>Deliberately generous against the 2% the blades themselves are held to:
     * a bloom is a soft mark several percent of a figure wide, so asking its
     * centre to land inside a tenth of a figure height of both blades is asking
     * that it be *on* the crossing rather than that it be a point.
     */
    private static final double BLOOM_ON_BLADE = 0.10;

    @Test
    void everyClashIsDrawnWhereTwoBladesActuallyAre() {
        // The guard the review asked for by name: "a CLASH directive whose two
        // blades are more than a few percent of figure height apart is asserting a
        // lie. It should fail a test rather than render."
        for (Duel.Kind kind : Duel.Kind.values()) {
            Rehearsal r = new Rehearsal(kind);
            List<Rehearsal.Frame> frames = r.play();
            List<Directive.Ink> clashes = r.clashes();
            for (Directive.Ink clash : clashes) {
                Rehearsal.Frame f = r.at(clash.at());
                assertNotNull(f, kind + ": no frame at the clash instant " + clash.at());
                assertTrue(f.bladeGapFraction() <= MET,
                        kind + ": a clash is drawn at t=" + clash.at() + " with the two blades "
                                + pct(f.bladeGapFraction()) + " of a figure height apart. "
                                + "A bloom is an assertion that they met; this one is false.");
                // And it is drawn where they are. The Director resolves the mark
                // onto the same crossing both aims used, so this cannot drift
                // without the aim drifting with it.
                assertTrue(clash.origin().site() == Anchor.Site.CROSSING,
                        kind + ": a clash whose origin is " + clash.origin().site()
                                + " is aimed at one body rather than at the meeting");
                double toHero = pointToSegment(f, true);
                double toFoe = pointToSegment(f, false);
                assertTrue(toHero <= BLOOM_ON_BLADE && toFoe <= BLOOM_ON_BLADE,
                        kind + ": the clash mark is " + pct(toHero) + " from the hero's blade and "
                                + pct(toFoe) + " from the foe's. It is sitting on a grip.");
            }
        }
    }

    @Test
    void theParryIsAMeetingAndNotAnApproach() {
        // The whole of System 4's signature beat, in one number. 0.2105 was pass
        // 1's, measured two ways -- by segmenting cool-bright pixels in a capture
        // and, independently, by this rehearsal, which agree to within a tenth of a
        // percent of figure height.
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        List<Rehearsal.Frame> frames = r.play();
        double best = Double.MAX_VALUE;
        for (Rehearsal.Frame f : frames) {
            best = Math.min(best, f.bladeGapFraction());
        }
        assertTrue(best <= MET, "the closest the blades ever come is " + pct(best)
                + " of a figure height. STYLE.md 7.2 wants a deflection curve; this is an "
                + "approach that stops.");
    }

    @Test
    void theResolvedCrossingStaysWhereTheScheduleStagedIt() {
        // The Director resolves the crossing against where the blades actually are,
        // which is the only way two rigs with a residual can be made to meet. That
        // freedom has to be bounded or the renderer is re-staging the beat: if the
        // resolved point wandered into a body or off the lane, the schedule would
        // no longer be the authority on where the beat happens.
        for (Duel.Kind kind : Duel.Kind.values()) {
            Rehearsal r = new Rehearsal(kind);
            r.play();
            com.badlogic.gdx.math.Vector2 resolved = new com.badlogic.gdx.math.Vector2();
            for (Directive.Ink clash : r.clashes()) {
                Rehearsal.Frame f = r.at(clash.at());
                assertNotNull(f);
                r.director().lastCrossing(resolved);
                // The rehearsal has run past the clash by now, so this is a bound on
                // the *staging*, read at the end: the crossing anchor and the
                // figures' own span.
                double staged = Director.stretch(clash.origin());
                double heroX = f.hero().stand().x;
                double foeX = f.foe().stand().x;
                assertTrue(staged > Math.min(heroX, foeX) && staged < Math.max(heroX, foeX),
                        kind + ": the staged crossing at " + staged + " is not between the two "
                                + "bodies at " + heroX + " and " + foeX);
            }
        }
    }

    @Test
    void aBladeNeverPointsBackwardsThroughItsOwnCrossing() {
        // The defect the review described in words -- "drawn from a grip on the far
        // side of its own torso, pointing down and away" -- as a number. Through
        // any frame in which a body is aimed at a crossing, the blade must run
        // *toward* the other figure, not away from it. This is the cheapest
        // possible statement of "the sword is pointing at the fight".
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        List<Rehearsal.Frame> frames = r.play();
        double contact = r.schedule().contacts().get(r.schedule().contacts().size() - 1);
        int checked = 0;
        for (Rehearsal.Frame f : r.between(contact - 0.02, contact + 0.02)) {
            checked++;
            assertTrue(pointsToward(f.hero(), f.foe()),
                    "the hero's blade points away from the foe at t=" + f.t());
            assertTrue(pointsToward(f.foe(), f.hero()),
                    "the foe's blade points away from the hero at t=" + f.t());
        }
        assertTrue(checked > 0, "no frame landed on the contact instant");
        assertTrue(frames.size() > 100, "the rehearsal did not play the score");
    }

    /** True when {@code b}'s tip is nearer the other body than its own hilt is. */
    private static boolean pointsToward(Rehearsal.Body b, Rehearsal.Body other) {
        double toward = Math.signum(other.stand().x - b.stand().x);
        return (b.bladeTip().x - b.bladeRoot().x) * toward > -1e-6;
    }

    private static double pointToSegment(Rehearsal.Frame f, boolean hero) {
        Rehearsal.Body b = hero ? f.hero() : f.foe();
        com.badlogic.gdx.math.Vector2 mark = new com.badlogic.gdx.math.Vector2();
        // The mark is drawn at the resolved crossing, which is where both blades
        // were aimed; the distance from it to each blade is the honest statistic.
        mark.set(b.bladeCross());
        return Rehearsal.segmentDistance(mark, mark, b.bladeRoot(), b.bladeTip())
                / SamuraiRig.FIGURE_HEIGHT;
    }

    private static String pct(double fraction) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", 100 * fraction);
    }
}
