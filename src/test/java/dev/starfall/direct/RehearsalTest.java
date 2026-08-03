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

    /**
     * The guard STYLE.md 11.2b(f) was written about, rebuilt so that it can fail.
     *
     * <h2>What the old one did, and why a green build meant nothing</h2>
     *
     * <p>It took the mark from {@code Body.bladeCross()} — a point <em>on</em> a
     * blade — and measured it against <em>that same blade's own segment</em>. Three
     * collinear points; the distance was identically {@code 0.0} for every input, and
     * the assertion was {@code 0.0 <= 0.10}. Two reviews and a commit message cited it
     * as proof that the bloom lands on the crossing. In delivered pixels the bloom was
     * 0.088-0.190 of a figure height from <em>both</em> blades on two of the eleven
     * frames it is drawn on, and 0.11-0.19 from one of them on eight more.
     *
     * <p>Three things change here and each one closes a separate hole:
     *
     * <ol>
     *   <li><b>The mark is {@link Director#lastCrossing}</b>, recorded per frame by the
     *       rehearsal — the field {@code Director.renderInk} substitutes for a
     *       {@code CROSSING} origin. It is the coordinate the renderer is handed, not a
     *       coordinate recomputed from the geometry the renderer was supposed to use.</li>
     *   <li><b>Every frame the mark is drawn on is checked</b>, not the single frame
     *       nearest the directive's instant. A bloom drawn over 0.2 s asserts the
     *       meeting for 0.2 s; checking one frame of it is checking a twelfth of the
     *       claim, and the eleven-frame table in the pass-2 review is what one-frame
     *       checking missed.</li>
     *   <li><b>The mark must be <em>between</em> the two blades</b>, not merely near
     *       both. "Near both" is satisfied by a light welded to one blade when the
     *       other happens to pass close; the picture the corpus draws has the star in
     *       the fork.</li>
     * </ol>
     *
     * <p>Observed red before it was believed, per 11.2b(f) — see
     * {@code docs/system4-debt.md} for the failure messages it printed on the code it
     * was written against.
     */
    @Test
    void everyClashIsDrawnWhereTwoBladesActuallyAre() {
        for (Duel.Kind kind : Duel.Kind.values()) {
            Rehearsal r = new Rehearsal(kind);
            r.play();
            for (Directive.Ink clash : r.clashes()) {
                assertTrue(clash.origin().site() == Anchor.Site.CROSSING,
                        kind + ": a clash whose origin is " + clash.origin().site()
                                + " is aimed at one body rather than at the meeting");
                List<Rehearsal.Frame> drawn = r.between(clash.at(), clash.end());
                assertTrue(drawn.size() >= 2,
                        kind + ": the clash at t=" + clash.at() + " is drawn on " + drawn.size()
                                + " frames of the rehearsal; there is nothing to check");
                for (Rehearsal.Frame f : drawn) {
                    assertTrue(f.bladeGapFraction() <= MET,
                            kind + ": the clash that starts at t=" + clash.at() + " is still drawn at t="
                                    + String.format(java.util.Locale.ROOT, "%.4f", f.t()) + " with the two blades "
                                    + pct(f.bladeGapFraction()) + " of a figure height apart. "
                                    + "A bloom is an assertion that they are meeting; on this frame "
                                    + "it is false.");
                    double toHero = f.markToBlade(true);
                    double toFoe = f.markToBlade(false);
                    assertTrue(toHero <= BLOOM_ON_BLADE && toFoe <= BLOOM_ON_BLADE,
                            kind + ": at t=" + String.format(java.util.Locale.ROOT, "%.4f", f.t())
                                    + " the drawn clash mark " + point(f.mark()) + " is " + pct(toHero)
                                    + " from the hero's blade and " + pct(toFoe)
                                    + " from the foe's. It is sitting on a grip.");
                    assertTrue(f.markIsBetweenTheBlades(),
                            kind + ": at t=" + String.format(java.util.Locale.ROOT, "%.4f", f.t())
                                    + " the drawn clash mark " + point(f.mark())
                                    + " has both blades on the same side of it — it is welded to one "
                                    + "of them rather than sitting in the fork.");
                }
            }
        }
    }

    private static String point(com.badlogic.gdx.math.Vector2 v) {
        return String.format(java.util.Locale.ROOT, "(%.3f,%.3f)", v.x, v.y);
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

    /**
     * STYLE.md 7.3's held breath, on every scene, measured rather than described.
     *
     * <h2>This assertion replaces a reviewer's throwaway script, and it exists because
     * the document was wrong about it in both directions</h2>
     *
     * <p>{@code docs/system4-debt.md} carried, for a whole pass, that <i>"the held
     * breath is still 0.857x for ~0.12 s against 7.3's ~0.25 s, and there is still none
     * on the knockback"</i>. Both halves are false. The ramp is <b>0.850x over 0.258 s
     * end to end</b>, and the knockback's is identical to the parry's in floor, span and
     * shape. The 0.12 s was the plateau below 0.90x, which is what an eased ramp
     * reaching its floor necessarily looks like — reported against 7.3 it would have
     * sent this pass to lengthen a constant that is already at spec.
     *
     * <p>The measurement was made by a temporary instrument checked in for the pass-2
     * review ({@code RevTimingDumpTest}, which wrote CSVs for a human to read). STYLE.md
     * 11.2b(e): a discipline that lives in a reviewer's script is not a guard. This is
     * that instrument folded into an assertion, and the temporary one is deleted.
     *
     * <p>Sampled at 120 Hz of <em>wall</em> time, because the held breath is precisely
     * the thing that makes wall time and schedule time differ.
     */
    @Test
    void theHeldBreathIsAtSpecOnEveryScene() {
        for (Duel.Kind kind : Duel.Kind.values()) {
            Rehearsal r = new Rehearsal(kind);
            List<Rehearsal.Frame> frames = r.play(r.schedule().duration(), 120.0);
            double floor = 1.0;
            int below = 0;
            double firstBelow = Double.NaN;
            double lastBelow = Double.NaN;
            int ramps = 0;
            boolean wasBelow = false;
            for (Rehearsal.Frame f : frames) {
                boolean isBelow = f.timeScale() < 0.999;
                if (isBelow) {
                    below++;
                    floor = Math.min(floor, f.timeScale());
                    if (Double.isNaN(firstBelow)) {
                        firstBelow = f.wall();
                    }
                    lastBelow = f.wall();
                    if (!wasBelow) {
                        ramps++;
                    }
                }
                wasBelow = isBelow;
            }
            assertTrue(ramps >= 1, kind + ": no held breath anywhere in the score. STYLE.md 7.3 "
                    + "lists it as one of only four things an impact may be expressed as, and "
                    + "docs/system4-debt.md claimed for a pass that the knockback had none.");
            // Every ramp is the same shape, so span-per-ramp is the total over the count.
            double spanPerRamp = (lastBelow - firstBelow + (below / (double) ramps) / 120.0) / ramps;
            assertTrue(floor <= 0.86 && floor >= 0.84,
                    kind + ": the held breath bottoms out at " + floor + "x. STYLE.md 7.3 asks "
                            + "for ~0.85x and Timing.HELD_BREATH_SCALE is 0.85.");
            assertTrue(floor > 0.5,
                    kind + ": a time scale of " + floor + " is a freeze, which 7.1 and 10 both ban.");
            double perRamp = below / (double) ramps / 120.0;
            assertTrue(perRamp >= 0.22 && perRamp <= 0.30,
                    kind + ": the held breath runs " + String.format(java.util.Locale.ROOT, "%.3f", perRamp)
                            + " s per ramp over " + ramps + " ramp(s). STYLE.md 7.3 asks for ~0.25 s "
                            + "and Timing.HELD_BREATH_SECONDS is " + dev.starfall.stage.Timing.HELD_BREATH_SECONDS
                            + ". (Span per ramp measured end to end: "
                            + String.format(java.util.Locale.ROOT, "%.3f", spanPerRamp) + " s.)");
        }
    }

    private static String pct(double fraction) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", 100 * fraction);
    }
}
