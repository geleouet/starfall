package dev.starfall.ik;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The tests this package exists for.</b>
 *
 * <p>STYLE.md 7.2 bans snapping, and the three places a textbook IK solver snaps
 * are all invisible to a "does it reach the target" test: the reach boundary,
 * the chain axis, and the root singularity. Each of them produces a pose that is
 * perfectly correct on both sides and jumps in between, which is exactly the
 * failure a capture review catches late and expensively. So the assertion here
 * is never about correctness of a single pose -- it is that the pose is a
 * <em>continuous function of the target</em>: sweep the target in small steps and
 * no joint may move much between consecutive steps.
 *
 * <p>Every sweep runs with {@code settleSeconds(0)}. The settle filter would
 * smooth a genuine snap into something that passes, and then the property would
 * be a property of the filter rather than of the solver.
 *
 * <p>Two of the sweeps also run at two step densities. A steep-but-finite region
 * shows a smaller largest-step when the sweep is refined; a real discontinuity
 * shows the same jump however finely you sample it. That refinement check is the
 * only assertion here that distinguishes "hard to sample" from "actually
 * broken", which is why the singular cases use it.
 */
class IkContinuityTest {

    /** What the target does over a normalised sweep parameter. */
    private interface Path {
        void at(float t, float[] outXY);
    }

    // -- fixtures --------------------------------------------------------------

    /** Two unit bones hanging off a shoulder at (0, 1). */
    private static Skeleton arm() {
        Bone root = new Bone("root", 0, null).bindLocal(0f, 0f, 0f);
        Bone shoulder = new Bone("shoulder", 1, root).bindLocal(0f, 1f, 0f);
        Bone upper = new Bone("upper", 2, shoulder).bindLocal(0f, 0f, 0f);
        Bone lower = new Bone("lower", 3, upper).bindLocal(1f, 0f, 0f);
        Bone hand = new Bone("hand", 4, lower).bindLocal(1f, 0f, 0f);
        return new Skeleton(List.of(root, shoulder, upper, lower, hand));
    }

    /** Four unit segments from the origin along +x, plus a tip marker. */
    private static Skeleton spine() {
        Bone root = new Bone("root", 0, null).bindLocal(0f, 0f, 0f);
        Bone s0 = new Bone("s0", 1, root).bindLocal(0f, 0f, 0f);
        Bone s1 = new Bone("s1", 2, s0).bindLocal(1f, 0f, 0f);
        Bone s2 = new Bone("s2", 3, s1).bindLocal(1f, 0f, 0f);
        Bone s3 = new Bone("s3", 4, s2).bindLocal(1f, 0f, 0f);
        Bone tip = new Bone("tip", 5, s3).bindLocal(1f, 0f, 0f);
        return new Skeleton(List.of(root, s0, s1, s2, s3, tip));
    }

    private static final float SHOULDER_X = 0f;
    private static final float SHOULDER_Y = 1f;

    // -- sweep harness ---------------------------------------------------------

    /** Largest per-step change, in degrees, of any joint in the chain over one sweep. */
    private static float maxJointStep(IkChain chain, Path path, int steps, float seconds) {
        return sweep(chain, path, null, steps, seconds)[0];
    }

    private static float[] sweep(IkChain chain, Path path, int steps, float seconds) {
        return sweep(chain, path, null, steps, seconds);
    }

    /**
     * Runs the chain along a path and returns
     * {@code [maxJointStepDeg, maxEndEffectorStep]}.
     *
     * <p>{@code polePath} moves the pole with the target, which is how a real
     * limb is driven -- the pole is carried on the body. Passing null pins the
     * pole wherever the caller put it, which is the harsher case because a target
     * going right round will then cross the chain axis.
     */
    private static float[] sweep(IkChain chain, Path path, Path polePath, int steps, float seconds) {
        float dt = seconds / steps;
        float[] xy = new float[2];
        float[] pole = new float[2];
        float[] prev = new float[chain.boneCount()];
        Vector2 end = new Vector2();
        Vector2 prevEnd = new Vector2();

        path.at(0f, xy);
        chain.target(xy[0], xy[1]);
        if (polePath != null) {
            polePath.at(0f, pole);
            chain.pole(pole[0], pole[1]);
        }
        chain.snap();
        for (int i = 0; i < chain.boneCount(); i++) {
            prev[i] = chain.bone(i).rotDeg;
        }
        chain.endEffector(prevEnd);

        float maxJoint = 0f;
        float maxEnd = 0f;
        for (int step = 1; step <= steps; step++) {
            float t = step / (float) steps;
            path.at(t, xy);
            chain.target(xy[0], xy[1]);
            if (polePath != null) {
                polePath.at(t, pole);
                chain.pole(pole[0], pole[1]);
            }
            chain.update(dt);
            for (int i = 0; i < chain.boneCount(); i++) {
                float now = chain.bone(i).rotDeg;
                maxJoint = Math.max(maxJoint, Math.abs(IkMath.deltaDeg(prev[i], now)));
                prev[i] = now;
            }
            chain.endEffector(end);
            maxEnd = Math.max(maxEnd, end.dst(prevEnd));
            prevEnd.set(end);
        }
        return new float[]{maxJoint, maxEnd};
    }

    private static IkChain armChain() {
        return IkChain.twoBone(arm(), "upper", "lower", "hand").settleSeconds(0f);
    }

    private static IkChain spineChain() {
        return IkChain.fabrik(spine(), "s0", "s3", "tip").settleSeconds(0f);
    }

    // -- two bone: the reach boundary -----------------------------------------

    @Test
    void twoBoneSweepStraightOutThroughTheReachBoundary() {
        // The classic snap: a hard clamp at l1 + l2 makes the elbow angle's
        // derivative blow up exactly where the limb straightens, so a target
        // drifting past arm's length pops the limb rigid on one frame.
        IkChain chain = armChain().pole(-2f, 3f);
        Path outward = (t, xy) -> {
            float d = 0.3f + t * 2.3f; // 0.3 -> 2.6, straight through 2.0
            xy[0] = SHOULDER_X + d * IkMath.cosDeg(30f);
            xy[1] = SHOULDER_Y + d * IkMath.sinDeg(30f);
        };
        float[] r = sweep(chain, outward, 460, 4f);
        assertTrue(r[0] < 3f, "a joint jumped " + r[0] + " degrees crossing the reach boundary");
        assertTrue(r[1] < 0.02f, "the hand jumped " + r[1] + " units crossing the reach boundary");
    }

    @Test
    void twoBoneSweepBackAndForthAcrossTheBoundaryIsSmoothBothWays() {
        // The reversal at the far end is its own hazard: hysteresis in the wrong
        // place would show up as the limb taking a different path back in than it
        // took out, with a step where the two paths meet.
        IkChain chain = armChain().pole(-2f, 3f);
        Path outAndBack = (t, xy) -> {
            float d = 1.5f + (t < 0.5f ? t * 2f : 2f - t * 2f); // 1.5 -> 2.5 -> 1.5
            xy[0] = SHOULDER_X + d;
            xy[1] = SHOULDER_Y;
        };
        float step = maxJointStep(chain, outAndBack, 400, 4f);
        assertTrue(step < 3f, "the boundary round trip jumped " + step + " degrees");
    }

    // -- two bone: the chain axis ---------------------------------------------

    /**
     * Bound for sweeps that deliberately provoke a bend flip. A flip moves the
     * elbow through nearly 200 degrees on purpose, spread over
     * {@code flipSeconds}; at 60 Hz that is inherently around 10 degrees a frame
     * at the peak of the critically damped profile, and the sweeps below measure
     * about 9.6. The failure being caught is a flip that happens in <em>one</em>
     * frame, so this is an order of magnitude of margin against that -- it is not
     * a claim that 12 degrees per frame is a good number.
     */
    private static final float FLIP_BOUND_DEG = 12f;

    @Test
    void twoBoneTargetCrossingTheChainAxisSweepsRatherThanFlips() {
        // The elbow side is a binary choice, so this is where a naive solver
        // mirrors the whole limb between two frames. The bend has to travel.
        IkChain chain = armChain().pole(SHOULDER_X, SHOULDER_Y + 3f);
        Path acrossThePole = (t, xy) -> {
            xy[0] = SHOULDER_X - 0.6f + 1.2f * t;
            xy[1] = SHOULDER_Y + 1.2f;
        };
        float step = maxJointStep(chain, acrossThePole, 240, 4f);
        assertTrue(step < FLIP_BOUND_DEG, "the limb popped " + step + " degrees through the chain axis");
    }

    @Test
    void aBendFlipTakesManyFramesAndPassesThroughTheMiddle() {
        IkChain chain = armChain().pole(SHOULDER_X, SHOULDER_Y + 3f);
        chain.target(SHOULDER_X - 0.6f, SHOULDER_Y + 1.2f);
        chain.snap();
        assertTrue(chain.bendSide() < -0.9f, "should start committed to one side");

        int framesInTransit = 0;
        for (int i = 0; i <= 240; i++) {
            chain.target(SHOULDER_X - 0.6f + 1.2f * i / 240f, SHOULDER_Y + 1.2f);
            chain.update(1f / 60f);
            if (Math.abs(chain.bendSide()) < 0.9f) {
                framesInTransit++;
            }
        }
        assertTrue(chain.bendSide() > 0.9f, "should have committed to the other side by the end");
        assertTrue(framesInTransit > 15,
                "the flip took only " + framesInTransit + " frames, which reads as a pop");
    }

    @Test
    void aTargetLoiteringOnTheChainAxisDoesNotChatter() {
        // Sitting exactly on the axis is the case where the "correct" side is
        // genuinely undefined, so the deadband has to hold a decision rather than
        // re-derive one from noise every frame.
        IkChain chain = armChain().pole(SHOULDER_X, SHOULDER_Y + 3f);
        chain.target(SHOULDER_X, SHOULDER_Y + 1.2f);
        chain.snap();
        float committed = chain.bendSide();

        float worst = 0f;
        float prev = chain.bone(1).rotDeg;
        for (int i = 0; i < 600; i++) {
            // A target jittering by a thousandth of a bone length either side.
            float wobble = (i % 2 == 0 ? 1e-3f : -1e-3f);
            chain.target(SHOULDER_X + wobble, SHOULDER_Y + 1.2f);
            chain.update(1f / 60f);
            worst = Math.max(worst, Math.abs(IkMath.deltaDeg(prev, chain.bone(1).rotDeg)));
            prev = chain.bone(1).rotDeg;
        }
        assertTrue(Math.abs(chain.bendSide() - committed) < 0.05f,
                "the bend side wandered while the target sat on the axis");
        assertTrue(worst < 1f, "the elbow chattered by " + worst + " degrees on an axis-bound target");
    }

    // -- two bone: full circle -------------------------------------------------

    @Test
    void twoBoneFullCircleWithABodyMountedPoleIsTight() {
        // The realistic System 4 case: the pole is carried on the body, so it
        // turns with the aim and the target never crosses the chain axis. Nothing
        // is provoked and nothing is being smoothed over, so the bound is tight --
        // this is the sweep that says the solver itself is smooth.
        IkChain chain = armChain();
        Path circle = (t, xy) -> {
            float a = t * 360f;
            xy[0] = SHOULDER_X + 1.3f * IkMath.cosDeg(a);
            xy[1] = SHOULDER_Y + 1.3f * IkMath.sinDeg(a);
        };
        Path poleAhead = (t, xy) -> {
            float a = t * 360f + 90f;
            xy[0] = SHOULDER_X + 3f * IkMath.cosDeg(a);
            xy[1] = SHOULDER_Y + 3f * IkMath.sinDeg(a);
        };
        float[] r = sweep(chain, circle, poleAhead, 720, 12f);
        assertTrue(r[0] < 2f, "a joint jumped " + r[0] + " degrees with no flip to blame");
        assertTrue(r[1] < 0.02f, "the hand jumped " + r[1] + " units with no flip to blame");
    }

    @Test
    void twoBoneFullCircleWithAFixedPoleSweepsThroughTwoFlips() {
        // Everything at once: two axis crossings, and the aim angle wrapping
        // through +-180, which is where an angle bookkeeping mistake shows up.
        IkChain chain = armChain().pole(SHOULDER_X, SHOULDER_Y + 3f);
        Path circle = (t, xy) -> {
            float a = t * 360f;
            xy[0] = SHOULDER_X + 1.3f * IkMath.cosDeg(a);
            xy[1] = SHOULDER_Y + 1.3f * IkMath.sinDeg(a);
        };
        float[] r = sweep(chain, circle, 720, 12f);
        assertTrue(r[0] < FLIP_BOUND_DEG, "a joint jumped " + r[0] + " degrees going round the circle");
        // During a flip the hand deliberately travels out through near-extension
        // and back, so it moves faster than the target does. Bounded, and nothing
        // like the whole-limb mirror a popping solver would produce.
        assertTrue(r[1] < 0.15f, "the hand jumped " + r[1] + " units going round the circle");
    }

    @Test
    void twoBoneCircleJustOutsideReachIsAlsoSmooth() {
        IkChain chain = armChain().pole(SHOULDER_X, SHOULDER_Y + 3f);
        Path circle = (t, xy) -> {
            float a = t * 360f;
            xy[0] = SHOULDER_X + 2.05f * IkMath.cosDeg(a);
            xy[1] = SHOULDER_Y + 2.05f * IkMath.sinDeg(a);
        };
        float step = maxJointStep(chain, circle, 720, 12f);
        assertTrue(step < 4f,
                "an out-of-reach circle should be among the smoothest cases; jumped " + step);
    }

    // -- two bone: constraints must not introduce a discontinuity --------------

    @Test
    void aJointLimitEngagingMidSweepDoesNotSnap() {
        // The reason limits are soft and applied inside the solver. A hard clamp
        // here is continuous in position but not in derivative: the elbow decides
        // to stop between two frames, and a limb that stops dead reads as
        // mechanical.
        IkChain chain = armChain()
                .pole(SHOULDER_X, SHOULDER_Y + 3f)
                .limit(1, IkConstraint.range(-110f, -35f));
        Path outward = (t, xy) -> {
            float d = 0.4f + t * 2.0f;
            xy[0] = SHOULDER_X + d;
            xy[1] = SHOULDER_Y + 0.2f;
        };
        float step = maxJointStep(chain, outward, 400, 4f);
        assertTrue(step < 3f, "the limb snapped " + step + " degrees as the elbow limit engaged");
    }

    @Test
    void aRootLimitEngagingMidSweepDoesNotSnap() {
        // The target sweeps an arc that drives the shoulder hard into both of its
        // stops and back out again, with the pole carried along so nothing else is
        // in play. The elbow takes up what the shoulder cannot give.
        IkChain chain = armChain().limit(0, IkConstraint.range(-40f, 40f));
        Path arc = (t, xy) -> {
            float a = -85f + 170f * t;
            xy[0] = SHOULDER_X + 1.4f * IkMath.cosDeg(a);
            xy[1] = SHOULDER_Y + 1.4f * IkMath.sinDeg(a);
        };
        Path poleAhead = (t, xy) -> {
            float a = -85f + 170f * t + 90f;
            xy[0] = SHOULDER_X + 3f * IkMath.cosDeg(a);
            xy[1] = SHOULDER_Y + 3f * IkMath.sinDeg(a);
        };
        float step = sweep(chain, arc, poleAhead, 600, 6f)[0];
        assertTrue(step < 3f, "the shoulder snapped " + step + " degrees against its limit");
    }

    // -- the refinement proof --------------------------------------------------

    @Test
    void refiningTheBoundarySweepShrinksItsLargestStep() {
        // A discontinuity has the same jump at every sampling density. A merely
        // steep region does not. This is the assertion that actually distinguishes
        // "smooth" from "we happened not to sample the pop".
        Path outward = (t, xy) -> {
            float d = 1.6f + t * 0.8f; // 1.6 -> 2.4, sitting on the boundary
            xy[0] = SHOULDER_X + d;
            xy[1] = SHOULDER_Y;
        };
        float coarse = maxJointStep(armChain().pole(-2f, 3f), outward, 200, 3f);
        float fine = maxJointStep(armChain().pole(-2f, 3f), outward, 800, 3f);
        assertTrue(fine < 0.5f * coarse,
                "refining 4x only took the largest step from " + coarse + " to " + fine
                        + "; that is a discontinuity, not a steep region");
    }

    // -- the root singularity --------------------------------------------------

    /** Target sliding straight through the shoulder, left to right. */
    private static final Path THROUGH_THE_SHOULDER = (t, xy) -> {
        xy[0] = SHOULDER_X + 0.5f - t;
        xy[1] = SHOULDER_Y;
    };

    @Test
    void aTargetPassingThroughTheShoulderDoesNotPop() {
        // Genuinely singular: the direction from the shoulder to the target
        // reverses, so the limb must swing 180 degrees. That motion is real and
        // has to happen; what must not happen is it happening in one frame. The
        // pole travels with the target so that this measures the singularity
        // alone, without a bend flip layered on top.
        IkChain chain = armChain();
        Path poleAbove = (t, xy) -> {
            xy[0] = SHOULDER_X + 0.5f - t;
            xy[1] = SHOULDER_Y + 3f;
        };
        float[] r = sweep(chain, THROUGH_THE_SHOULDER, poleAbove, 200, 2f);
        assertTrue(r[0] < 20f, "the limb spun " + r[0] + " degrees in one step through the shoulder");
        // The hand is folded almost onto the shoulder throughout, so however far
        // the limb rotates, the end effector stays in a small neighbourhood of it.
        // Compare with the ~2 units a naive atan2 would fling it.
        assertTrue(r[1] < 0.08f, "the hand jumped " + r[1] + " units through the shoulder");
    }

    @Test
    void refiningTheThroughShoulderSweepShrinksItsLargestStep() {
        // The one that proves the shoulder singularity is steep rather than
        // broken. Same path, same duration, four times the samples.
        float coarse = maxJointStep(armChain().pole(SHOULDER_X, SHOULDER_Y + 3f), THROUGH_THE_SHOULDER, 200, 2f);
        float fine = maxJointStep(armChain().pole(SHOULDER_X, SHOULDER_Y + 3f), THROUGH_THE_SHOULDER, 800, 2f);
        assertTrue(fine < 0.8f * coarse,
                "the shoulder singularity did not smooth out under refinement: " + coarse + " -> " + fine);
    }

    @Test
    void theBendSideIsHeldWhileTheLimbIsFoldedShut() {
        // A fixed pole plus a target reversing through the shoulder asks for a
        // bend flip at exactly the worst moment: the only continuous path between
        // the two mirror poses runs through full extension, so a limb folded onto
        // the shoulder would have to shoot out to arm's length and back. The
        // opening gate refuses the flip until the limb has opened up, which is why
        // this sweep comes out no worse than the one where the pole follows the
        // target and never asks for a flip at all.
        IkChain chain = armChain().pole(SHOULDER_X, SHOULDER_Y + 3f);
        float sideAtStart;
        float worstSideDrift = 0f;
        float[] xy = new float[2];

        THROUGH_THE_SHOULDER.at(0f, xy);
        chain.target(xy[0], xy[1]);
        chain.snap();
        sideAtStart = chain.bendSide();

        for (int i = 1; i <= 200; i++) {
            THROUGH_THE_SHOULDER.at(i / 200f, xy);
            chain.target(xy[0], xy[1]);
            chain.update(1f / 100f);
            worstSideDrift = Math.max(worstSideDrift, Math.abs(chain.bendSide() - sideAtStart));
        }
        assertTrue(worstSideDrift < 0.05f,
                "the solver started a flip on a folded limb; side drifted " + worstSideDrift);
    }

    // -- FABRIK ----------------------------------------------------------------

    @Test
    void fabrikSweepStraightOutThroughTheReachBoundary() {
        // Textbook FABRIK branches here, to a straight-line layout. The branch is
        // where a fixed iteration budget makes the chain visibly firm up and lock.
        IkChain chain = spineChain();
        Path outward = (t, xy) -> {
            float d = 1f + t * 4f; // 1 -> 5, through the 4-unit reach
            xy[0] = d * IkMath.cosDeg(35f);
            xy[1] = d * IkMath.sinDeg(35f);
        };
        float[] r = sweep(chain, outward, 400, 4f);
        assertTrue(r[0] < 3f, "a spine joint jumped " + r[0] + " degrees crossing the reach boundary");
        assertTrue(r[1] < 0.03f, "the spine tip jumped " + r[1] + " units crossing the reach boundary");
    }

    @Test
    void fabrikFullCircleSweep() {
        IkChain chain = spineChain();
        Path circle = (t, xy) -> {
            float a = t * 360f;
            xy[0] = 3f * IkMath.cosDeg(a);
            xy[1] = 3f * IkMath.sinDeg(a);
        };
        float[] r = sweep(chain, circle, 720, 12f);
        assertTrue(r[0] < 5f, "a spine joint jumped " + r[0] + " degrees going round the circle");
        assertTrue(r[1] < 0.06f, "the spine tip jumped " + r[1] + " units going round the circle");
    }

    @Test
    void fabrikSweepWithLimitsEngagingDoesNotSnap() {
        // An arc the constrained chain can actually follow: each joint is allowed
        // 35 degrees, so the tip can swing well past this without saturating.
        IkChain chain = spineChain();
        for (int i = 0; i < chain.boneCount(); i++) {
            chain.limit(i, IkConstraint.range(-35f, 35f));
        }
        Path arc = (t, xy) -> {
            float a = -50f + 100f * t;
            xy[0] = 3f * IkMath.cosDeg(a);
            xy[1] = 3f * IkMath.sinDeg(a);
        };
        float step = maxJointStep(chain, arc, 600, 6f);
        assertTrue(step < 3f, "a constrained spine snapped " + step + " degrees against its limits");
    }

    @Test
    void aFabrikChainSaturatedAgainstItsLimitsSlewsRatherThanFlipping() {
        // Honest about a real limitation. A FABRIK chain whose limits put the
        // target far outside its workspace saturates, and a saturated constrained
        // chain is bistable -- two configurations, both pinned to their limits,
        // both about equally wrong, and the unconstrained backward pass can throw
        // it from one to the other. No amount of softening in the limits fixes
        // that, because the ambiguity is in the chain, not in the limit. What
        // bounds it is IkChain's per-step correction cap (STYLE.md 7.2), which
        // turns the flip into a few frames of fast sweep. This test pins that
        // bound rather than pretending the flip is not there.
        IkChain chain = spineChain();
        for (int i = 0; i < chain.boneCount(); i++) {
            chain.limit(i, IkConstraint.range(-35f, 35f));
        }
        Path circle = (t, xy) -> {
            float a = t * 360f;
            xy[0] = 2.5f * IkMath.cosDeg(a);
            xy[1] = 2.5f * IkMath.sinDeg(a);
        };
        // The cap is on each bone's *world* angle, 20 deg at 1200 deg/s and 60 Hz.
        // A bone's local rotation is the difference between its own world angle
        // and its parent's, so it can move at twice the cap when neighbours slew
        // opposite ways -- hence 45 rather than 20.
        float step = maxJointStep(chain, circle, 720, 12f);
        assertTrue(step < 45f, "a saturated spine turned " + step + " degrees in one frame");
    }

    @Test
    void fabrikSweepThroughItsOwnRootDoesNotPop() {
        IkChain chain = spineChain();
        Path throughTheRoot = (t, xy) -> {
            xy[0] = 1f - 2f * t;
            xy[1] = 0f;
        };
        float[] r = sweep(chain, throughTheRoot, 400, 4f);
        assertTrue(r[0] < 3f, "a spine joint jumped " + r[0] + " degrees passing through the root");
        assertTrue(r[1] < 0.03f, "the spine tip jumped " + r[1] + " units passing through the root");
    }
}
