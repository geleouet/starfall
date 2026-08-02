package dev.starfall.ik;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The binding layer: solved angles have to come back out as {@code Bone.rotDeg}
 * values that {@link Skeleton} agrees with, in both facings, blended against
 * whatever the animation wrote, and settled over time rather than snapped.
 *
 * <p>Accuracy note: {@link Skeleton} builds its transforms with libGDX's
 * lookup-table trig, which carries ~4e-4 of error per bone. Anything measured by
 * round-tripping through the skeleton is therefore checked to ~1e-3, not to the
 * solvers' own precision.
 */
class IkChainTest {

    private static final float EPS = 1e-3f;

    /**
     * root -> shoulder at (0, 1) -> upper -> lower (+1 x) -> hand (+1 x). Two unit
     * bones with the shoulder off the origin, so a write-back that quietly assumed
     * the chain root was at the world origin, or that the parent had no rotation,
     * fails here.
     */
    private static Skeleton arm() {
        Bone root = new Bone("root", 0, null).bindLocal(0f, 0f, 0f);
        Bone shoulder = new Bone("shoulder", 1, root).bindLocal(0f, 1f, 0f);
        Bone upper = new Bone("upper", 2, shoulder).bindLocal(0f, 0f, 0f);
        Bone lower = new Bone("lower", 3, upper).bindLocal(1f, 0f, 0f);
        Bone hand = new Bone("hand", 4, lower).bindLocal(1f, 0f, 0f);
        return new Skeleton(List.of(root, shoulder, upper, lower, hand));
    }

    /** root -> s0 -> s1 -> s2 -> s3 -> tip, five unit segments up +x. */
    private static Skeleton spine() {
        Bone root = new Bone("root", 0, null).bindLocal(0f, 0f, 0f);
        Bone s0 = new Bone("s0", 1, root).bindLocal(0f, 0f, 0f);
        Bone s1 = new Bone("s1", 2, s0).bindLocal(1f, 0f, 0f);
        Bone s2 = new Bone("s2", 3, s1).bindLocal(1f, 0f, 0f);
        Bone s3 = new Bone("s3", 4, s2).bindLocal(1f, 0f, 0f);
        Bone tip = new Bone("tip", 5, s3).bindLocal(1f, 0f, 0f);
        return new Skeleton(List.of(root, s0, s1, s2, s3, tip));
    }

    @Test
    void twoBoneChainPutsTheEndEffectorOnTheTarget() {
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f);
        arm.target(1.2f, 1.7f);
        arm.snap();

        Vector2 end = arm.endEffector(new Vector2());
        assertEquals(1.2f, end.x, EPS);
        assertEquals(1.7f, end.y, EPS);
        assertTrue(arm.residual() < EPS);
    }

    @Test
    void theSolvedPoseIsWrittenBackAsLocalRotationsTheSkeletonAgreesWith() {
        // The write-back is the one piece of this package that has to speak
        // Skeleton's language exactly. Reading the hand's world position straight
        // out of the skeleton, rather than out of the solver, is what proves it.
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f);
        arm.target(0.4f, 2.2f);
        arm.snap();

        Vector2 hand = s.worldPosition(s.bone("hand").index, new Vector2());
        assertEquals(0.4f, hand.x, EPS);
        assertEquals(2.2f, hand.y, EPS);
    }

    @Test
    void aRotatedParentDoesNotOffsetTheSolution() {
        Skeleton s = arm();
        s.bone("shoulder").rotDeg = 37f; // as a spine lean would do
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(-2f, 3f);
        arm.target(-0.6f, 2.1f);
        arm.snap();

        Vector2 hand = s.worldPosition(s.bone("hand").index, new Vector2());
        assertEquals(-0.6f, hand.x, EPS, "local rotations must be measured against the parent, not the world");
        assertEquals(2.1f, hand.y, EPS);
    }

    @Test
    void aMirroredRootStillSolvesCorrectly() {
        // Facing flips by negating root.scaleX (docs/system1-rig-fixes.md section
        // 1). That reverses which way a positive rotDeg turns, so a write-back
        // that ignores it produces an arm that works facing right and inverts
        // facing left -- a bug that would only ever show up in a capture.
        Skeleton s = arm();
        s.bone("root").scaleX = -1f;
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f);
        arm.target(-1.2f, 1.7f);
        arm.snap();

        Vector2 hand = s.worldPosition(s.bone("hand").index, new Vector2());
        assertEquals(-1.2f, hand.x, EPS);
        assertEquals(1.7f, hand.y, EPS);
    }

    @Test
    void mirroringTheWholeProblemProducesIdenticalLocalRotations() {
        // Mirror the rig, the target and the pole all at once and the *local*
        // rotations must come out unchanged -- the negative root scale is what
        // turns them into the mirrored world pose. That is the property that lets
        // System 4 drive both fighters from one set of numbers. It only holds if
        // the write-back tracks the determinant sign and the pole tracks the aim;
        // get either wrong and one facing bends its elbow the wrong way.
        Skeleton plain = arm();
        IkChain a = IkChain.twoBone(plain, "upper", "lower", "hand").pole(0f, 4f);
        a.target(1.2f, 1.7f);
        a.snap();

        Skeleton flipped = arm();
        flipped.bone("root").scaleX = -1f;
        IkChain b = IkChain.twoBone(flipped, "upper", "lower", "hand").pole(0f, 4f);
        b.target(-1.2f, 1.7f);
        b.snap();

        assertEquals(plain.bone("upper").rotDeg, flipped.bone("upper").rotDeg, 0.05f);
        assertEquals(plain.bone("lower").rotDeg, flipped.bone("lower").rotDeg, 0.05f);
    }

    @Test
    void weightZeroLeavesTheAnimationPoseCompletelyUntouched() {
        Skeleton s = arm();
        s.bone("upper").rotDeg = 20f;
        s.bone("lower").rotDeg = -35f;
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").weight(0f);
        arm.target(1.9f, 1.9f);
        arm.snap();

        assertEquals(20f, s.bone("upper").rotDeg, 1e-4f);
        assertEquals(-35f, s.bone("lower").rotDeg, 1e-4f);
    }

    @Test
    void weightBlendsPartwayAndStaysOnTheShortArc() {
        Skeleton s = arm();
        IkChain full = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f).target(0.6f, 2.1f);
        full.snap();
        float solvedUpper = s.bone("upper").rotDeg;
        float solvedLower = s.bone("lower").rotDeg;

        Skeleton h = arm();
        IkChain half = IkChain.twoBone(h, "upper", "lower", "hand").pole(0f, 4f).weight(0.5f).target(0.6f, 2.1f);
        half.snap();

        assertEquals(0.5f * solvedUpper, h.bone("upper").rotDeg, 0.05f);
        assertEquals(0.5f * solvedLower, h.bone("lower").rotDeg, 0.05f);
    }

    @Test
    void theTargetIsSettledIntoRatherThanArrivedAt() {
        // STYLE.md 7.1: nothing arrives at rest abruptly. One frame after the
        // target jumps, the hand must have moved only part of the way.
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f).settleSeconds(0.45f);
        arm.target(1.9f, 1.0f);
        arm.snap();
        Vector2 start = arm.endEffector(new Vector2());

        arm.target(0.2f, 2.4f);
        arm.update(1f / 60f);
        Vector2 afterOneFrame = arm.endEffector(new Vector2());

        float travelled = afterOneFrame.dst(start);
        float total = start.dst(0.2f, 2.4f);
        assertTrue(travelled < 0.25f * total,
                "one frame should cover a fraction of the move, covered " + (travelled / total));
        assertTrue(travelled > 0f, "but it must start moving immediately, not after a delay");
    }

    @Test
    void theSettleArrivesWithinItsWindowAndNeverRebounds() {
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f).settleSeconds(0.45f);
        arm.target(1.9f, 1.0f);
        arm.snap();

        arm.target(0.2f, 2.4f);
        Vector2 end = new Vector2();
        float closest = Float.MAX_VALUE;
        float worstRebound = 0f;
        for (int i = 0; i < 90; i++) { // 1.5 s, comfortably past a 0.45 s settle
            arm.update(1f / 60f);
            float d = arm.endEffector(end).dst(0.2f, 2.4f);
            closest = Math.min(closest, d);
            // Moving away again after having been closer is the visible
            // oscillation STYLE.md 7.2 bans: "a spring that visibly oscillates
            // twice and stops reads as a machine".
            worstRebound = Math.max(worstRebound, d - closest);
        }
        assertTrue(worstRebound < 0.01f, "the settle rebounded by " + worstRebound);
        assertTrue(arm.residual() < 0.01f, "and it should have arrived; residual " + arm.residual());
    }

    @Test
    void aStaticTargetProducesAStaticPose() {
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand").pole(0f, 4f).target(1.4f, 1.6f);
        arm.snap();
        for (int i = 0; i < 120; i++) {
            arm.update(1f / 60f);
        }
        float upper = s.bone("upper").rotDeg;
        float lower = s.bone("lower").rotDeg;
        for (int i = 0; i < 120; i++) {
            arm.update(1f / 60f);
        }
        assertEquals(upper, s.bone("upper").rotDeg, 5e-3f, "the shoulder drifted with a static target");
        assertEquals(lower, s.bone("lower").rotDeg, 5e-3f, "the elbow drifted with a static target");
    }

    @Test
    void jointLimitsDeclaredOnTheChainReachTheSolver() {
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", "hand")
                .limit(1, IkConstraint.range(-120f, -20f))
                .pole(0f, 4f);
        for (float a = 0f; a < 360f; a += 9f) {
            arm.target(1f + 1.4f * IkMath.cosDeg(a), 1f + 1.4f * IkMath.sinDeg(a));
            arm.snap();
            float elbow = s.bone("lower").rotDeg;
            assertTrue(elbow > -120f && elbow < -20f, "elbow at " + elbow + " escaped its limit");
        }
    }

    // -- FABRIK chains ---------------------------------------------------------

    @Test
    void aFabrikChainReachesItsTarget() {
        Skeleton s = spine();
        IkChain chain = IkChain.fabrik(s, "s0", "s3", "tip");
        chain.target(1.5f, 2.5f);
        for (int i = 0; i < 90; i++) {
            chain.update(1f / 60f);
        }
        Vector2 tip = s.worldPosition(s.bone("tip").index, new Vector2());
        assertEquals(1.5f, tip.x, 5e-3f);
        assertEquals(2.5f, tip.y, 5e-3f);
    }

    @Test
    void aFabrikChainWritesBackWithoutStretchingTheRig() {
        Skeleton s = spine();
        IkChain chain = IkChain.fabrik(s, "s0", "s3", "tip");
        chain.target(-2f, 2f);
        for (int i = 0; i < 90; i++) {
            chain.update(1f / 60f);
        }
        // Bone offsets are local and untouched, so the world segment lengths are a
        // direct check that write-back only ever wrote rotations.
        Vector2 a = new Vector2();
        Vector2 b = new Vector2();
        for (int i = 1; i <= 4; i++) {
            s.worldPosition(i, a);
            s.worldPosition(i + 1, b);
            assertEquals(1f, a.dst(b), EPS, "segment " + i + " changed length");
        }
    }

    @Test
    void aFabrikChainWithAStaticTargetSettlesAndStaysPut() {
        Skeleton s = spine();
        IkChain chain = IkChain.fabrik(s, "s0", "s3", "tip");
        chain.target(2f, 3f);
        for (int i = 0; i < 180; i++) {
            chain.update(1f / 60f);
        }
        float[] settled = new float[4];
        for (int i = 0; i < 4; i++) {
            settled[i] = s.bone(i + 1).rotDeg;
        }
        for (int i = 0; i < 180; i++) {
            chain.update(1f / 60f);
        }
        for (int i = 0; i < 4; i++) {
            assertEquals(settled[i], s.bone(i + 1).rotDeg, 0.02f, "spine bone " + i + " drifted");
        }
    }

    // -- construction ----------------------------------------------------------

    @Test
    void twoBoneRejectsARunThatIsNotExactlyTwoBones() {
        Skeleton s = spine();
        assertThrows(IllegalArgumentException.class, () -> IkChain.twoBone(s, "s0", "s3", "tip"));
    }

    @Test
    void aChainRejectsEndpointsThatAreNotRelated() {
        Skeleton s = arm();
        assertThrows(IllegalArgumentException.class, () -> IkChain.fabrik(s, "hand", "upper", 1f));
    }

    @Test
    void anExplicitTipLengthWorksWithoutATipBone() {
        Skeleton s = arm();
        IkChain arm = IkChain.twoBone(s, "upper", "lower", 1f).pole(0f, 4f);
        arm.target(1.2f, 1.7f);
        arm.snap();
        Vector2 end = arm.endEffector(new Vector2());
        assertEquals(1.2f, end.x, EPS);
        assertEquals(1.7f, end.y, EPS);
    }

    @Test
    void withNoPoleTheChainKeepsBendingTheWayThePoseAlreadyBends() {
        // The default has to be "do not fight the animation". Two chains whose only
        // difference is which side the authored pose put the elbow on must each
        // keep their own side, with no pole told to either of them.
        Skeleton up = arm();
        up.bone("upper").rotDeg = 40f; // elbow swung above the shoulder-to-target line
        IkChain a = IkChain.twoBone(up, "upper", "lower", "hand").target(1.5f, 1.5f);
        a.snap();

        Skeleton down = arm();
        down.bone("upper").rotDeg = -40f; // and below
        IkChain b = IkChain.twoBone(down, "upper", "lower", "hand").target(1.5f, 1.5f);
        b.snap();

        // A positive bend side puts the elbow left of the aim, which makes the
        // elbow's own rotation negative.
        assertTrue(up.bone("lower").rotDeg < 0f, "should have kept the elbow above the aim");
        assertTrue(down.bone("lower").rotDeg > 0f, "should have kept the elbow below the aim");
    }
}
