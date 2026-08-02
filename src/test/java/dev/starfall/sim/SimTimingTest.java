package dev.starfall.sim;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;
import dev.starfall.rig.SamuraiHair;
import dev.starfall.rig.SamuraiRig;
import dev.starfall.rig.SimScript;
import dev.starfall.rig.SimSceneDriver;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The measurements STYLE.md 7.1 and 7.2 are actually stated in, taken at 60 Hz
 * on the same driver the captures run.
 *
 * <p>System 2's debt closes with a methodological finding that applies here in
 * full: a review "could not grade 7.0.3 at all, because the capture cadence was
 * too coarse to resolve the thing pass 3 was built to do", and "any claim about
 * lag or stagger made from a coarse capture is unfalsifiable". A 60 Hz capture
 * window fixes that for the eye. This fixes it for the number: the lags are
 * computed rather than described, in the frame units 7.1 uses, so a later
 * refactor that quietly stiffens the cloth fails a test instead of a review.
 */
class SimTimingTest {

    /** One capture frame at 60 Hz, in solver substeps. */
    private static final int SUBSTEPS_PER_FRAME = 4;
    private static final float DT = VerletSolver.SUBSTEP;

    /**
     * A recorded run: per-60Hz-frame x position of every signal being compared.
     *
     * <p>Anchors are recorded alongside tips, and that is not bookkeeping. A
     * lag is only meaningful against the thing that drove it: the back hem hangs
     * off the hips, which the script moves directly, but the sleeve hangs off the
     * <em>wrist</em>, which is the far end of an IK chain that already carries a
     * 0.34 s settle and a 0.11 s wrist lag. Measuring the sleeve against the hips
     * would report System 2's chain lag as though it were cloth stiffness.
     */
    private record Run(float[] hip, float[] head, float[] hand,
                       float[] clothBack, float[] clothSleeve, float[] sleeveAnchor,
                       float[] massTip, float[] hairTip, float[] escapeeTip, float[] maxStretch) {
    }

    /** The scene exactly as it ships, with whatever ambient policy it declares. */
    private static Run record(SimScript.Kind kind, int frames) {
        return record(kind, frames, -1f, 1f);
    }

    private static Run record(SimScript.Kind kind, int frames, float turbulence) {
        return record(kind, frames, turbulence, 1f);
    }

    /**
     * @param turbulence negative to leave the scene's own ambient policy alone.
     *                   Passing 1 here unconditionally is how a measurement
     *                   silently switches the turbulence back <em>on</em> in a
     *                   scene built to run without it -- which is what made the
     *                   IMPULSE settle report two returns on a trace that is
     *                   monotone to the pixel after its single overshoot.
     */
    private static Run record(SimScript.Kind kind, int frames, float turbulence, float air) {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        SimSceneDriver driver = SimSceneDriver.headless(skeleton, kind);
        if (turbulence >= 0f) {
            driver.sim().hair().turbulenceScale(turbulence);
        }
        driver.script().airScale = air;
        driver.start();

        float[] hip = new float[frames];
        float[] head = new float[frames];
        float[] hand = new float[frames];
        float[] clothBack = new float[frames];
        float[] clothSleeve = new float[frames];
        float[] sleeveAnchor = new float[frames];
        float[] massTip = new float[frames];
        float[] hairTip = new float[frames];
        float[] escapeeTip = new float[frames];
        float[] stretch = new float[frames];

        Vector2 v = new Vector2();
        HairSim hair = driver.sim().hair();
        ClothSim cloth = driver.sim().cloth();

        for (int f = 0; f < frames; f++) {
            for (int s = 0; s < SUBSTEPS_PER_FRAME; s++) {
                driver.advance(DT);
            }
            hip[f] = skeleton.worldPosition(skeleton.bone("hips").index, v).x;
            head[f] = skeleton.worldPosition(skeleton.bone("head").index, v).x;
            hand[f] = skeleton.worldPosition(skeleton.bone("handL").index, v).x;

            ClothSim.Chain back = cloth.chain(0);
            clothBack[f] = back.x(back.particleCount() - 1);
            ClothSim.Chain sleeve = cloth.chain(2);
            clothSleeve[f] = sleeve.x(sleeve.particleCount() - 1);
            sleeveAnchor[f] = sleeve.x(0);

            // Three hair signals, not one, and the split is pass 2's bimodality
            // showing up in the timing as well as in the picture. The mass locks
            // are short, stiff and nearly attached to the skull; the wisps are
            // long and loose; the escapees are barely attached at all. Averaging
            // them together would report the mass's arrival as the bundle's --
            // and after pass 2 the mass is six of the twenty locks, so it would
            // dominate. STYLE.md 7.1's "hair tips" are the wisps.
            float sum = 0f;
            int n = 0;
            float massSum = 0f;
            int massN = 0;
            float escSum = 0f;
            int escN = 0;
            float worst = 0f;
            for (int i = 0; i < hair.strandCount(); i++) {
                HairSim.Strand st = hair.strand(i);
                float tx = st.x(st.particleCount() - 1);
                if (st.kind == HairSim.Kind.MASS) {
                    massSum += tx;
                    massN++;
                } else if (st.escapee) {
                    escSum += tx;
                    escN++;
                } else {
                    sum += tx;
                    n++;
                }
                worst = Math.max(worst, stretchOf(st));
            }
            massTip[f] = massSum / massN;
            hairTip[f] = sum / n;
            escapeeTip[f] = escSum / escN;
            stretch[f] = worst;
        }
        return new Run(hip, head, hand, clothBack, clothSleeve, sleeveAnchor, massTip, hairTip, escapeeTip, stretch);
    }

    /** Worst relative segment stretch on a strand. Verlet distance projection should keep this near zero. */
    private static float stretchOf(HairSim.Strand st) {
        float total = st.length() / (st.particleCount() - 1);
        float worst = 0f;
        for (int i = 0; i + 1 < st.particleCount(); i++) {
            float dx = st.x(i + 1) - st.x(i);
            float dy = st.y(i + 1) - st.y(i);
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            worst = Math.max(worst, Math.abs(d - total) / total);
        }
        return worst;
    }

    /**
     * The frame a signal is moving fastest inside a window.
     *
     * <p>STYLE.md 7.1 says "body, cloth, and hair must never peak on the same
     * frame", and this is that word taken literally: the peak of the motion, not
     * the peak of the position. Two earlier attempts measured the wrong thing
     * and both were instructive enough to record.
     *
     * <p>A velocity <em>sign change</em> needs a pre-reversal direction to
     * compare against, and under a live breeze it finds turbulence instead --
     * it reported the hip turning 58 frames late.
     *
     * <p>A position <em>argmax</em> is worse, and interestingly so. The hem's
     * absolute x is not a delayed copy of the hip's: as the hips accelerate
     * backward the hem swings forward relative to them, so its position peaks
     * where the swing peaks rather than where the body turns. That is correct
     * cloth and it is exactly the trailing the whole system exists to produce --
     * it is just not a measurement of arrival.
     */
    private static int peakSpeedFrame(float[] p, int from, int to) {
        float[] s = smoothed(p);
        int best = from;
        float bestV = -1f;
        for (int i = Math.max(1, from); i < Math.min(to, s.length - 1); i++) {
            float v = Math.abs(s[i + 1] - s[i - 1]);
            if (v > bestV) {
                bestV = v;
                best = i;
            }
        }
        return best;
    }

    /**
     * The lag, in 60 Hz frames, that maximises the correlation between two
     * signals' frame-to-frame velocities: the delay of the transfer from an
     * anchor to a tip, which is what STYLE.md 7.1 states its band in.
     *
     * <p>Velocity rather than position, because the two signals sit at different
     * places and a position correlation would be dominated by the constant
     * offset between them.
     */
    private static int transferLag(float[] anchor, float[] tip, int from, int to, int maxLag) {
        int best = 0;
        double bestScore = -Double.MAX_VALUE;
        for (int lag = 0; lag <= maxLag; lag++) {
            double dot = 0;
            double na = 0;
            double nb = 0;
            for (int i = from; i + 1 + lag < Math.min(to, tip.length - 1); i++) {
                double a = anchor[i + 1] - anchor[i];
                double b = tip[i + 1 + lag] - tip[i + lag];
                dot += a * b;
                na += a * a;
                nb += b * b;
            }
            double score = dot / Math.sqrt(Math.max(1e-14, na * nb));
            if (score > bestScore) {
                bestScore = score;
                best = lag;
            }
        }
        return best;
    }

    /** Five-frame box smoothing, so turbulence cannot decide where a peak is. */
    private static float[] smoothed(float[] p) {
        float[] s = new float[p.length];
        for (int i = 0; i < p.length; i++) {
            float sum = 0f;
            int n = 0;
            for (int k = -2; k <= 2; k++) {
                int j = i + k;
                if (j >= 0 && j < p.length) {
                    sum += p[j];
                    n++;
                }
            }
            s[i] = sum / n;
        }
        return s;
    }

    /**
     * How many times a signal crosses its own resting value by more than
     * {@code band} after the disturbance has ended: the overshoot count.
     *
     * <p>STYLE.md 7.2 allows "at most one soft return" and 10 fails a pass on
     * sight of visible oscillation, and this is the number both are about. The
     * band matters: the strands sit in a live breeze that never stops moving
     * them, and counting every sign change measures the air rather than the
     * settle. Anything inside the band is drift; anything outside it is the
     * system ringing.
     */
    private static int overshoots(float[] p, int from, int to, float band) {
        float rest = 0f;
        int tail = Math.max(from + 1, to - 10);
        for (int i = tail; i < to; i++) {
            rest += p[i];
        }
        rest /= (to - tail);
        int crossings = 0;
        int side = 0;
        for (int i = from; i < to; i++) {
            float d = p[i] - rest;
            int now = d > band ? 1 : d < -band ? -1 : 0;
            if (now != 0 && side != 0 && now != side) {
                crossings++;
            }
            if (now != 0) {
                side = now;
            }
        }
        return crossings;
    }

    /**
     * The frame at which a signal has covered half its total travel across a
     * window: the step-response delay of the knockback.
     *
     * <p>Half rather than the peak, because the signals have very different
     * amplitudes -- the escapee tips are carried further by the gust than the
     * hips are by the impact -- and half-of-own-travel is the one arrival time
     * that does not depend on how far a thing went.
     */
    private static int halfTravelFrame(float[] p, int from, int to) {
        float start = p[from];
        float end = 0f;
        int tail = Math.max(from + 1, to - 12);
        for (int i = tail; i < to; i++) {
            end += p[i];
        }
        end /= (to - tail);
        float half = start + 0.5f * (end - start);
        float dir = Math.signum(end - start);
        for (int i = from; i < to; i++) {
            if (dir * (p[i] - half) >= 0f) {
                return i;
            }
        }
        return -1;
    }

    // -- STYLE.md 7.1: the arrival order ------------------------------------------

    @Test
    void theReversalBeat() {
        // sim-sway's single reversal begins at t = 1.90 s, i.e. frame 114. The
        // pelvis leads it by 0.075 s and the head lags it by 0.105 s, so the
        // search opens well before and closes well after.
        Run run = record(SimScript.Kind.SWAY, 220, 0f, 0f);
        int hemLag = transferLag(run.hip(), run.clothBack(), 60, 220, 26);
        int sleeveLag = transferLag(run.sleeveAnchor(), run.clothSleeve(), 60, 220, 26);
        int hairLag = transferLag(run.head(), run.hairTip(), 60, 220, 26);
        // A wider search for the escapees on purpose: STYLE.md 4 asks them to
        // "lag dramatically", so their answer is expected outside the band the
        // bundle is held to and a search capped at the bundle's ceiling would
        // only ever report the ceiling.
        int escLag = transferLag(run.head(), run.escapeeTip(), 60, 220, 50);

        System.out.printf(java.util.Locale.ROOT,
                "TRANSFER LAG frames@60Hz: hem-behind-hips=%d sleeve-behind-wrist=%d "
                        + "hair-behind-head=%d escapee-behind-head=%d  (diagnostic; see onsetFrame)%n",
                hemLag, sleeveLag, hairLag, escLag);

        // Absolute arrival order across the whole figure, on the shipped scene
        // with its turbulence running -- the thing a reviewer actually looks at.
        Run live = record(SimScript.Kind.SWAY, 220);
        System.out.printf(java.util.Locale.ROOT,
                "PEAK frame@60Hz hips=%d head=%d wrist=%d hem=%d sleeve=%d hair-tip=%d%n",
                peakSpeedFrame(live.hip(), 100, 200), peakSpeedFrame(live.head(), 100, 200),
                peakSpeedFrame(live.hand(), 100, 210), peakSpeedFrame(live.clothBack(), 100, 210),
                peakSpeedFrame(live.clothSleeve(), 100, 210), peakSpeedFrame(live.hairTip(), 100, 215));

        // -- onset delay, which is what 7.1's band is graded on --------------
        //
        // A correction pass 2 makes deliberately, and it is worth stating because
        // it is a correction to a *measurement*, not to a target.
        //
        // The velocity correlation above is well posed for a tip hanging off a
        // wrist, where the anchor's whole motion is a translation. It is not well
        // posed for the hem, because this script's pelvis translates one way
        // while rotating the other -- {@code hipDx = +0.058 g} and
        // {@code hipRotDeg = -3.6 g} -- and on a 1.15-unit chain the rotation
        // moves the hem tip further than the translation does, in the opposite
        // direction. So hip-x and the hem's own rigid-body prediction are
        // anti-correlated by construction, the correlation has two nearly equal
        // optima, and the statistic jumps between 0 and its search ceiling on a
        // parameter change that moves the picture by a pixel. Measured across a
        // bend-stiffness sweep it read 26, 26, 26, 26 -- i.e. the ceiling, at
        // every setting, including settings whose hem visibly led the body.
        //
        // The onset delay does not have that failure mode, and it is also nearer
        // to what 7.1 means. "Cloth trails the body by 4-8 frames" is a
        // statement about when the cloth *starts*: a hem that finished its travel
        // 4-8 frames after the hips would be very nearly rigid, which is exactly
        // what pass 1 shipped -- it measured a 6-frame correlation lag on a hem
        // whose tip moved 0.00 px between delivered frames.
        int hipOn = onsetFrame(run.hip(), 108, 180);
        int hemOn = onsetFrame(run.clothBack(), 108, 195);
        int sleeveOn = onsetFrame(run.clothSleeve(), 108, 200);
        int wristOn = onsetFrame(run.hand(), 108, 190);
        int headOn = onsetFrame(run.head(), 108, 185);
        int massOn = onsetFrame(run.massTip(), 108, 195);
        int hairOn = onsetFrame(run.hairTip(), 108, 205);
        int escOn = onsetFrame(run.escapeeTip(), 108, 215);
        System.out.printf(java.util.Locale.ROOT,
                "ONSET DELAY frames@60Hz: hem-behind-hips=%d sleeve-behind-wrist=%d "
                        + "hair-mass-behind-head=%d hair-wisp-behind-head=%d escapee-behind-head=%d%n",
                hemOn - hipOn, sleeveOn - wristOn, massOn - headOn, hairOn - headOn, escOn - headOn);

        // -- the assertions, and which statistic governs which signal --------
        //
        // Two statistics, applied where each is well posed, and the criterion is
        // stated rather than chosen: a velocity correlation between an anchor and
        // a tip is only meaningful when the anchor's own signal is what drives the
        // tip. That holds for the sleeve (it hangs off a wrist that travels
        // 130 px) and for the hair (it hangs off a skull that travels with the
        // body). It does not hold for the hem, whose anchor sits on the hip line
        // and barely moves: what actually drives that chain is the pelvis's
        // *rotation*, which this script runs anti-correlated with the pelvis's
        // translation by construction. So the hem is graded on when it starts.
        //
        // STYLE.md 7.1: "Cloth trails the body by ~4-8 frames, hair tips by ~8-14."
        assertTrue(hemOn - hipOn >= 4 && hemOn - hipOn <= 8,
                "back hem should trail the hips by 4-8 frames, measured " + (hemOn - hipOn));
        assertTrue(sleeveLag >= 3 && sleeveLag <= 9,
                "sleeve should trail the wrist by 3-9 frames, measured " + sleeveLag);
        assertTrue(hairLag >= 8 && hairLag <= 14,
                "hair tips should trail the head by 8-14 frames, measured " + hairLag);
        // STYLE.md 4: the escapees are the ones that "lag dramatically".
        assertTrue(escLag >= hairLag,
                "escapees must lag at least as far as the bundle: " + escLag + " vs " + hairLag);
        // The chain of arrivals the review verified and named "the cheapest
        // poetry in the project": the hair root moves before the hair mid.
        assertTrue(massOn < hairOn, "the hair mass must arrive before the wisps: " + massOn + " vs " + hairOn);

        // STYLE.md 7.0.3 and 10's last row: nothing may arrive at the same time.
        // Measured on the shipped scene with its turbulence running, because that
        // is the run a reviewer looks at, and stated as the peak-speed frame of
        // every tracked region -- which is what "peaking on the same frame" means.
        int[] peaks = {
                peakSpeedFrame(live.hip(), 100, 200), peakSpeedFrame(live.clothBack(), 100, 210),
                peakSpeedFrame(live.head(), 100, 200), peakSpeedFrame(live.hand(), 100, 210),
                peakSpeedFrame(live.clothSleeve(), 100, 210), peakSpeedFrame(live.hairTip(), 100, 215)};
        for (int i = 0; i < peaks.length; i++) {
            for (int j = i + 1; j < peaks.length; j++) {
                assertTrue(peaks[i] != peaks[j],
                        "two regions peak on frame " + peaks[i] + "; nothing may arrive at the same time");
            }
        }
        assertTrue(peaks[5] > peaks[0] + 8,
                "the hair must outlast the body by a readable beat: hair " + peaks[5] + " hips " + peaks[0]);
    }

    /**
     * The first frame in a window at which a signal is moving at 18% of its own
     * peak speed there: when the thing <em>starts</em>.
     *
     * <p>Percent of its own peak rather than an absolute threshold, because the
     * signals differ in amplitude by an order of magnitude -- a hip moves 20 px
     * across a whole reversal and an escapee tip moves 90 -- and an absolute
     * threshold would report the small ones as late purely for being small.
     */
    private static int onsetFrame(float[] p, int from, int to) {
        float[] s = smoothed(p);
        float peak = 0f;
        for (int i = from; i + 1 < Math.min(to, s.length); i++) {
            peak = Math.max(peak, Math.abs(s[i + 1] - s[i]));
        }
        for (int i = from; i + 1 < Math.min(to, s.length); i++) {
            if (Math.abs(s[i + 1] - s[i]) > 0.18f * peak) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void theKnockbackArrivesInOrder() {
        // sim-extreme's impact is at t = 1.90 s (frame 114) and 7.2 asks the
        // figure to arrive over ~0.8 s, so the window runs to frame 216.
        Run run = record(SimScript.Kind.EXTREME, 260);
        int hip = halfTravelFrame(run.hip(), 112, 216);
        int back = halfTravelFrame(run.clothBack(), 112, 216);
        int hair = halfTravelFrame(run.hairTip(), 112, 216);
        int esc = halfTravelFrame(run.escapeeTip(), 112, 216);
        System.out.printf(java.util.Locale.ROOT, "KNOCKBACK half-travel frame hip=%d back-hem=%d hair-tip=%d escapee-tip=%d%n",
                hip, back, hair, esc);

        // 7.2: "arriving over ~0.8 s". The impact is frame 114, so half the
        // travel should land around frame 138 -- a drift, not a launch.
        assertTrue(hip >= 128 && hip <= 152,
                "the body should be halfway through the knockback around frame 138, measured " + hip);

        // 7.2 again: "cloth and hair streaming ahead of the body's arrival".
        // That is a statement about where the hair *is*, not about when it gets
        // there -- a struck figure's hair is thrown out in front of it in the
        // direction of travel. So the measurement is the tip's offset from the
        // head, which must grow in the travel direction (-x) during the strike
        // and be well outside anything the ambient breeze produces.
        float restOffset = run.hairTip()[110] - run.head()[110];
        float peakOffset = restOffset;
        for (int f = 114; f < 200; f++) {
            peakOffset = Math.min(peakOffset, run.hairTip()[f] - run.head()[f]);
        }
        float streamed = restOffset - peakOffset;
        System.out.printf(java.util.Locale.ROOT, "KNOCKBACK hair streamed %.3f world units (%.0f px) ahead of the head%n",
                streamed, streamed * 196f);
        assertTrue(streamed > 0.06f,
                "hair should stream ahead of the body during a knockback, measured " + streamed);

        // And it still arrives after the body, which is what makes it hair
        // rather than a banner nailed to the figure.
        assertTrue(hair > hip, "hair should arrive after the body: hair " + hair + " hip " + hip);
    }

    // -- STYLE.md 7.2: no stretching, no oscillation, one soft return ------------

    @Test
    void strandsAreInextensible() {
        Run run = record(SimScript.Kind.EXTREME, 260);
        float worst = 0f;
        for (float s : run.maxStretch()) {
            worst = Math.max(worst, s);
        }
        System.out.printf(java.util.Locale.ROOT, "MAX SEGMENT STRETCH = %.4f%n", worst);
        // Position-based distance projection with eight rounds should hold every
        // segment to well inside a percent. Anything more and the strand is
        // stretching, which is the state 7.2's "no snapping" is about -- a
        // stretched strand has to recover, and recovering visibly is the pop.
        assertTrue(worst < 0.01f, "segment stretch reached " + worst);
    }

    @Test
    void theSettleIsOneSoftReturnAndThenStillness() {
        // Measured on IMPULSE, which exists for this test and for nothing else.
        //
        // Pass 1 measured it on EXTREME with the turbulence and the gusts scaled
        // to zero, which is the right idea implemented as an argument rather than
        // as a scene, and the review refused it on two counts: the knockback
        // drives in the same direction as the steady breeze and the breeze is
        // never off, so response and driver are collinear; and no capture was
        // ever delivered under those conditions, so the number described a run
        // nobody could look at. STYLE.md 7.2 now states the protocol outright --
        // "Kill the ambient input, apply one impulse, and count the returns" --
        // and IMPULSE *is* that protocol, as a scene that ships a capture.
        //
        // The strike is at t = 1.00 (frame 60); the air is exactly zero from
        // about t = 2.2 and the run carries 1.6 s past the scene's own end,
        // because the escapees' shape memory runs to two thirds of a second and
        // their drag to a third. Taking "rest" from inside that transient is how
        // the remainder of a single approach gets counted as a second return.
        Run run = record(SimScript.Kind.IMPULSE, 380);

        // Two pixels at capture framing (1 world unit is ~196 px here), and the
        // number is chosen to mean what STYLE.md 7.2 means. The section bans
        // *visible* oscillation; the strands sit in a live breeze that never
        // stops moving them, so counting every sign change measures the air and
        // not the settle. A return smaller than two pixels is not a return
        // anyone can see. Anything larger is the system ringing.
        float band = 0.010f;
                // The run is carried 1.6 s past the scene's own end. The escapees' shape
        // memory runs to two thirds of a second and their drag to a third, so at
        // t = 4.4 they are still creeping: taking "rest" from the last frames of
        // the scene put the reference value inside the transient and counted the
        // remainder of a single approach as a second return.
        // The count opens at frame 108, not at the strike. The strike is at
        // frame 60 and the gust has decayed to 8% of its peak by 108, so from
        // there on the only thing moving anything is the simulation. Counting
        // from the strike itself would score the *approach* as a side and report
        // one soft return as two -- which is exactly what it did.
        int hairRings = overshoots(run.hairTip(), 108, 380, band);
        int clothRings = overshoots(run.clothBack(), 108, 380, band);
        int escRings = overshoots(run.escapeeTip(), 108, 380, band);
        int massRings = overshoots(run.massTip(), 108, 380, band);
        System.out.printf(java.util.Locale.ROOT, "SETTLE overshoots (>2 px) mass=%d hair=%d cloth=%d escapee=%d%n",
                massRings, hairRings, clothRings, escRings);

        // STYLE.md 7.2: "at most one soft return".
        assertTrue(clothRings <= 1, "cloth rang " + clothRings + " times");
        assertTrue(hairRings <= 1, "hair rang " + hairRings + " times");
        assertTrue(escRings <= 1, "escapees rang " + escRings + " times");
        assertTrue(massRings <= 1, "the hair mass rang " + massRings + " times");
    }

    // -- structural guarantees ----------------------------------------------------

    @Test
    void boneBudgetHoldsUnderTheGlesCap() {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        // docs/system1-contract.md section B: 32 mat4 is 128 vec4, inside the
        // GLES 3.0 guaranteed minimum of 256 vertex uniform vectors. Hard cap.
        assertTrue(skeleton.boneCount() <= 32,
                "skeleton has " + skeleton.boneCount() + " bones against a hard cap of 32");
        // Pass 2 spends three of the four spare bones on the hem, which the
        // review found producing no readable mark at four particles. One spare
        // is left, deliberately: the far sleeve is the obvious next candidate
        // and there has to be room for it without another audit.
        assertEquals(31, skeleton.boneCount(), "21 body bones plus 10 cloth bones");
    }

    /**
     * The review's third finding, as a number that fails a build rather than a
     * review: <b>the hem has to move.</b>
     *
     * <blockquote>Measured, a tight hem-tip box registers <b>0.00 px</b> across
     * all 23 inter-frame steps, and the skirt silhouette is the same shape
     * through an entire knockback. Cloth is half this system's title.
     * </blockquote>
     *
     * <p>Two quantities, because they fail differently. <em>Excursion</em> is how
     * far the hem tip gets from where it started, and a hem that is stiff but
     * heavy would still pass it by being dragged. <em>Curvature</em> is the angle
     * between the chain's first and last segments, and that is the one four
     * particles could not produce at any stiffness: it is what makes the
     * difference between a hem that curves and a panel hanging at an angle.
     */
    @Test
    void theHemMovesAndTheHemCurves() {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        SimSceneDriver driver = SimSceneDriver.headless(skeleton, SimScript.Kind.EXTREME);
        driver.start();
        ClothSim.Chain back = driver.sim().cloth().chain(0);
        int last = back.particleCount() - 1;

        float x0 = back.x(last);
        float y0 = back.y(last);
        float excursion = 0f;
        float maxStep = 0f;
        float curveRange = 0f;
        float minCurve = Float.MAX_VALUE;
        float maxCurve = -Float.MAX_VALUE;
        float px = back.x(last);
        float py = back.y(last);
        for (int f = 0; f < 260; f++) {
            for (int s = 0; s < SUBSTEPS_PER_FRAME; s++) {
                driver.advance(DT);
            }
            float x = back.x(last);
            float y = back.y(last);
            excursion = Math.max(excursion, (float) Math.hypot(x - x0, y - y0));
            maxStep = Math.max(maxStep, (float) Math.hypot(x - px, y - py));
            px = x;
            py = y;
            float bend = SimMath.deltaDeg(back.solverChain().segmentDeg(0),
                    back.solverChain().segmentDeg(back.boneCount() - 1));
            minCurve = Math.min(minCurve, bend);
            maxCurve = Math.max(maxCurve, bend);
        }
        curveRange = maxCurve - minCurve;

        // 1 world unit is ~196 px at capture framing.
        System.out.printf(java.util.Locale.ROOT,
                "HEM TIP excursion=%.1f px  max inter-frame step=%.2f px  root-to-tip bend %.1f..%.1f deg (range %.1f)%n",
                excursion * 196f, maxStep * 196f, minCurve, maxCurve, curveRange);

        assertTrue(excursion * 196f > 18f,
                "the hem tip must travel a mark the eye can find, measured " + (excursion * 196f) + " px");
        assertTrue(maxStep * 196f > 0.5f,
                "the hem tip must move between delivered frames, measured " + (maxStep * 196f) + " px");
        assertTrue(curveRange > 12f,
                "the hem must change its own curvature, not just hang at an angle: " + curveRange + " deg");
    }

    /**
     * The bimodality guard. The pass-1 review's one named cause was that every
     * hair mark landed at 5-11 px, "the one register that reads as neither mass
     * nor wisp", and the cheapest way for a later re-parameterisation to undo
     * this pass is to nudge a width back into that band without anyone noticing.
     *
     * <p>So the band is closed by a test. A mark is either a mass, at 15 px or
     * more, or a hairline, at 2.4 px or less. Nothing may be authored between.
     */
    @Test
    void everyHairMarkIsEitherMassOrHairlineAndNothingBetween() {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        HairSim hair = SamuraiHair.build(skeleton);
        float pxPerUnit = 195.6f;
        int masses = 0;
        int hairlines = 0;
        for (int i = 0; i < hair.strandCount(); i++) {
            HairSim.Strand st = hair.strand(i);
            if (st.kind == HairSim.Kind.MASS) {
                float w = 2f * st.rootHalfWidth * pxPerUnit;
                assertTrue(w >= 15f, "mass strand " + i + " is only " + w + " px across");
                masses++;
            } else {
                assertEquals(0f, st.rootHalfWidth, 0f,
                        "strand " + i + " is not a mass and must not draw a ribbon of its own");
            }
            float hw = 2f * st.hairlineHalfWidth * pxPerUnit;
            assertTrue(hw > 0f && hw <= 2.4f,
                    "strand " + i + "'s hairlines are " + hw + " px across; the 3-14 px band is closed");
            hairlines += st.hairlines;
        }
        System.out.printf(java.util.Locale.ROOT, "HAIR POPULATION %d locks -> %d mass ribbons + %d hairlines%n",
                hair.strandCount(), masses, hairlines);
        // The reference resolves fifty-odd separate hairline marks at this scale,
        // and the accidental s1-p2-bind artefact the review called "the single
        // most reference-accurate feature in any capture so far" had "a
        // hundred-odd sub-pixel spokes".
        assertTrue(hairlines >= 50, "only " + hairlines + " hairlines; the reference resolves fifty-odd");
        assertTrue(masses >= 4, "the mass needs enough overlapping locks to fill: " + masses);
    }

    /**
     * The hair bundle must be pushed out of the body as well as the skull.
     *
     * <p>The review: "cyan tips already terminate deep inside the chest at rest;
     * it is hidden only because the torso is dark." It is hidden today; System 4
     * throws the bundle forward across the body and across the grip/guard
     * cluster, which {@code docs/system2-debt.md} E2 names as the figure's most
     * fragile small mark.
     */
    @Test
    void noParticleRestsInsideTheBody() {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        SimSceneDriver driver = SimSceneDriver.headless(skeleton, SimScript.Kind.EXTREME);
        driver.start();
        HairSim hair = driver.sim().hair();
        assertTrue(hair.colliderCount() >= 3, "skull plus a torso: " + hair.colliderCount());
        float worst = 0f;
        for (int f = 0; f < 200; f++) {
            for (int s = 0; s < SUBSTEPS_PER_FRAME; s++) {
                driver.advance(DT);
            }
            for (int i = 0; i < hair.strandCount(); i++) {
                HairSim.Strand st = hair.strand(i);
                for (int p = 1; p < st.particleCount(); p++) {
                    for (int c = 0; c < hair.colliderCount(); c++) {
                        float d = (float) Math.hypot(st.x(p) - hair.colliderX(c), st.y(p) - hair.colliderY(c));
                        worst = Math.max(worst, hair.colliderRadius(c) - d);
                    }
                }
            }
        }
        System.out.printf(java.util.Locale.ROOT, "DEEPEST PENETRATION %.4f world units (%.2f px)%n", worst, worst * 196f);
        // The distance pass runs after the collider every round, so a particle
        // can end a substep a fraction inside; a pixel is the tolerance that
        // means "not visibly through the body".
        assertTrue(worst * 196f < 1.0f, "a particle sat " + (worst * 196f) + " px inside a collider");
    }

    @Test
    void strandCountAndParticleCountsAreInSpec() {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        HairSim hair = SamuraiHair.build(skeleton);
        // STYLE.md 4: "12-24 strands, 8-14 particles each."
        assertTrue(hair.strandCount() >= 12 && hair.strandCount() <= 24,
                "strand count " + hair.strandCount());
        int escapees = 0;
        for (int i = 0; i < hair.strandCount(); i++) {
            HairSim.Strand st = hair.strand(i);
            assertTrue(st.particleCount() >= 8 && st.particleCount() <= 14,
                    "strand " + i + " has " + st.particleCount() + " particles");
            if (st.escapee) {
                escapees++;
            }
        }
        assertTrue(escapees >= 2 && escapees <= 5, "a few escapees, not none and not most: " + escapees);
    }

    @Test
    void perStrandVariationIsRealRatherThanNominal() {
        Skeleton skeleton = SamuraiRig.buildSkeletonOnly();
        HairSim hair = SamuraiHair.build(skeleton);
        float minLen = Float.MAX_VALUE;
        float maxLen = 0f;
        float minTau = Float.MAX_VALUE;
        float maxTau = 0f;
        for (int i = 0; i < hair.strandCount(); i++) {
            HairSim.Strand st = hair.strand(i);
            minLen = Math.min(minLen, st.length());
            maxLen = Math.max(maxLen, st.length());
        }
        // STYLE.md 10 fails a pass on sight of uniform hair motion, so the
        // variation has to be a real spread rather than a rounding difference.
        assertTrue(maxLen / minLen > 3f, "length spread only " + (maxLen / minLen) + "x");
        // Half a body-length, per STYLE.md 4's "sometimes half a body-length".
        assertTrue(maxLen > 0.7f, "longest strand is only " + maxLen + " world units");
    }

    @Test
    void theRunIsDeterministic() {
        Run a = record(SimScript.Kind.EXTREME, 120);
        Run b = record(SimScript.Kind.EXTREME, 120);
        for (int i = 0; i < a.hairTip().length; i++) {
            assertEquals(a.hairTip()[i], b.hairTip()[i], 0f, "frame " + i);
            assertEquals(a.clothBack()[i], b.clothBack()[i], 0f, "frame " + i);
        }
    }
}
