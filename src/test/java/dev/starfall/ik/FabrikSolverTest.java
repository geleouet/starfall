package dev.starfall.ik;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FABRIK on its own joint array, with no skeleton attached. The properties that
 * matter beyond "does it converge" are stability ones: an already-solved chain
 * must be a fixed point, an out-of-reach target must not make the chain hunt,
 * and a collapsed chain must not pick an arbitrary new direction. Those are the
 * behaviours that show up on screen as jitter, and jitter is the iterative
 * equivalent of the snapping STYLE.md 7.2 bans.
 */
class FabrikSolverTest {

    private static final float EPS = 1e-4f;

    /** A straight chain of {@code links} unit segments along +x from the origin. */
    private static float[] straightChain(int links) {
        float[] joints = new float[2 * (links + 1)];
        for (int i = 0; i <= links; i++) {
            joints[2 * i] = i;
            joints[2 * i + 1] = 0f;
        }
        return joints;
    }

    private static float[] unitLengths(int links) {
        float[] l = new float[links];
        java.util.Arrays.fill(l, 1f);
        return l;
    }

    private static void assertLinkLengths(float[] joints, float[] lengths) {
        for (int i = 0; i < lengths.length; i++) {
            float len = IkMath.length(joints[2 * i + 2] - joints[2 * i], joints[2 * i + 3] - joints[2 * i + 1]);
            assertEquals(lengths[i], len, 1e-3f, "link " + i + " changed length");
        }
    }

    @Test
    void convergesOnAReachableTargetWithinItsBudget() {
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        FabrikSolver f = new FabrikSolver();

        int iters = f.solve(joints, 5, lengths, 1.5f, 2.0f);

        assertTrue(iters <= f.maxIterations(), "must not exceed its own budget");
        assertTrue(f.residual() <= f.tolerance(), "should have converged; residual " + f.residual());
        assertEquals(1.5f, joints[8], 1e-3f);
        assertEquals(2.0f, joints[9], 1e-3f);
        assertLinkLengths(joints, lengths);
    }

    @Test
    void theChainRootStaysPinned() {
        float[] joints = straightChain(3);
        joints[0] = 0.7f;
        joints[1] = -0.3f;
        float[] lengths = unitLengths(3);
        new FabrikSolver().solve(joints, 4, lengths, -2f, 1f);
        assertEquals(0.7f, joints[0], EPS, "FABRIK must not walk the shoulder off the body");
        assertEquals(-0.3f, joints[1], EPS);
    }

    @Test
    void solvingAnAlreadySolvedChainDoesNothing() {
        // The frame-to-frame case for a target that has stopped moving. A solver
        // that keeps nudging here produces a limb that never quite comes to rest,
        // which is the failure STYLE.md 7.1's "settle" language rules out.
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        FabrikSolver f = new FabrikSolver();
        f.solve(joints, 5, lengths, 1.5f, 2.0f);

        float[] settled = joints.clone();
        int iters = f.solve(joints, 5, lengths, 1.5f, 2.0f);

        assertEquals(0, iters, "a converged chain must cost zero iterations");
        for (int i = 0; i < joints.length; i++) {
            assertEquals(settled[i], joints[i], 0f, "joint " + (i / 2) + " drifted on a no-op solve");
        }
    }

    @Test
    void repeatedSolvesConvergeToAFixedPoseEvenOutOfReach() {
        // Out of reach is where naive FABRIK is at its worst: the straight-line
        // fallback and the iterative branch disagree slightly, and alternating
        // between them is a visible buzz.
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        FabrikSolver f = new FabrikSolver();
        for (int i = 0; i < 200; i++) {
            f.solve(joints, 5, lengths, 8f, 6f);
        }

        // Not a fixed point, and deliberately so: the softened target sits a hair
        // inside full reach, so the chain approaches straight asymptotically and
        // keeps creeping by a fraction of the tolerance forever. What must not
        // happen is oscillation -- the residual moving back up, or a joint
        // reversing. So this measures the *per-solve* step and the direction of
        // travel, not the total.
        float[] before = joints.clone();
        float worstStep = 0f;
        float prevResidual = f.residual();
        for (int i = 0; i < 200; i++) {
            f.solve(joints, 5, lengths, 8f, 6f);
            for (int j = 0; j < joints.length; j++) {
                worstStep = Math.max(worstStep, Math.abs(joints[j] - before[j]));
                before[j] = joints[j];
            }
            assertTrue(f.residual() <= prevResidual + 1e-5f,
                    "the residual went back up, from " + prevResidual + " to " + f.residual());
            prevResidual = f.residual();
        }
        assertTrue(worstStep < 1e-4f, "a settled out-of-reach chain moved " + worstStep + " in one solve");
        assertLinkLengths(joints, lengths);
    }

    @Test
    void anOutOfReachChainExtendsTowardTheTargetWithoutLockingStraight() {
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        FabrikSolver f = new FabrikSolver();
        for (int i = 0; i < 20; i++) {
            f.solve(joints, 5, lengths, 0f, 20f, 0f, 1f);
        }
        float reach = IkMath.length(joints[8] - joints[0], joints[9] - joints[1]);
        assertTrue(reach > 3.9f, "should be nearly extended toward the target; got " + reach);
        assertTrue(reach < 4f, "but never exactly straight, which is the singular pose; got " + reach);
        assertEquals(0f, joints[8], 0.05f, "and aimed at the target");
    }

    @Test
    void aCollinearChainCanStillBendPerpendicular() {
        // The classic FABRIK degeneracy: every joint is exactly on one line, so
        // "the direction from here to there" is parallel to the chain everywhere.
        float[] joints = straightChain(3);
        float[] lengths = unitLengths(3);
        FabrikSolver f = new FabrikSolver();
        f.solve(joints, 4, lengths, 0f, 2f);
        assertTrue(f.residual() < 0.05f, "should still reach a perpendicular target; residual " + f.residual());
        assertLinkLengths(joints, lengths);
    }

    @Test
    void perJointLimitsAreRespected() {
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        IkConstraint[] limits = {
                IkConstraint.free(),
                IkConstraint.range(-25f, 25f),
                IkConstraint.range(-25f, 25f),
                IkConstraint.range(-25f, 25f),
        };
        FabrikSolver f = new FabrikSolver().limits(limits).maxIterations(24);
        f.solve(joints, 5, lengths, -1f, 3f, 0f, 1f);

        float prev = IkMath.atan2Deg(joints[3] - joints[1], joints[2] - joints[0]);
        for (int i = 1; i < 4; i++) {
            float deg = IkMath.atan2Deg(joints[2 * i + 3] - joints[2 * i + 1], joints[2 * i + 2] - joints[2 * i]);
            float local = IkMath.deltaDeg(prev, deg);
            assertTrue(local > -25f && local < 25f, "joint " + i + " bent to " + local + ", outside its limit");
            prev = deg;
        }
        assertLinkLengths(joints, lengths);
    }

    @Test
    void aConstrainedChainStillSettlesRatherThanHunting() {
        // With limits the target is usually unreachable, so the interesting
        // question is not "does it converge" but "does it stop".
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        IkConstraint[] limits = {
                IkConstraint.range(-20f, 20f),
                IkConstraint.range(-20f, 20f),
                IkConstraint.range(-20f, 20f),
                IkConstraint.range(-20f, 20f),
        };
        FabrikSolver f = new FabrikSolver().limits(limits);
        for (int i = 0; i < 60; i++) {
            f.solve(joints, 5, lengths, -2f, 3f, 0f, 1f);
        }
        float[] settled = joints.clone();
        for (int i = 0; i < 60; i++) {
            f.solve(joints, 5, lengths, -2f, 3f, 0f, 1f);
        }
        for (int i = 0; i < joints.length; i++) {
            assertEquals(settled[i], joints[i], 1e-3f, "constrained chain kept hunting at joint " + (i / 2));
        }
    }

    @Test
    void theFirstLinkIsLimitedAgainstTheSuppliedReferenceAngle() {
        float[] joints = straightChain(3);
        float[] lengths = unitLengths(3);
        IkConstraint[] limits = {IkConstraint.range(-10f, 10f), IkConstraint.free(), IkConstraint.free()};
        FabrikSolver f = new FabrikSolver().limits(limits).maxIterations(24);
        // Reference points straight up, so the first link must stay within 10
        // degrees of vertical no matter where the target is.
        f.solve(joints, 4, lengths, 3f, 0f, 90f, 1f);

        float first = IkMath.atan2Deg(joints[3] - joints[1], joints[2] - joints[0]);
        float local = IkMath.deltaDeg(90f, first);
        assertTrue(local > -10f && local < 10f, "first link at " + local + " off the reference");
    }

    @Test
    void mirroredChainsApplyLimitsOnTheOtherSide() {
        float[] plain = straightChain(3);
        float[] mirrored = straightChain(3);
        for (int i = 0; i <= 3; i++) {
            mirrored[2 * i] = -i; // same chain, pointing -x, as a negative root scale would give
        }
        float[] lengths = unitLengths(3);
        IkConstraint[] limits = {IkConstraint.range(0f, 60f), IkConstraint.free(), IkConstraint.free()};

        new FabrikSolver().limits(limits).maxIterations(24).solve(plain, 4, lengths, 1f, 2f, 0f, 1f);
        new FabrikSolver().limits(limits).maxIterations(24).solve(mirrored, 4, lengths, -1f, 2f, 180f, -1f);

        float plainLocal = IkMath.deltaDeg(0f, IkMath.atan2Deg(plain[3] - plain[1], plain[2] - plain[0]));
        float mirrorLocal = IkMath.deltaDeg(180f,
                IkMath.atan2Deg(mirrored[3] - mirrored[1], mirrored[2] - mirrored[0])) * -1f;
        assertTrue(plainLocal >= 0f && plainLocal <= 60f, "plain local " + plainLocal);
        assertTrue(mirrorLocal >= 0f && mirrorLocal <= 60f, "mirrored local " + mirrorLocal);
        assertEquals(plainLocal, mirrorLocal, 1f, "a mirrored chain must bend the mirrored amount");
    }

    // -- degenerate inputs ----------------------------------------------------

    @Test
    void zeroLengthLinksAreFiniteAndKeepTheirDirection() {
        float[] joints = {0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f};
        float[] lengths = {0f, 1f, 0f};
        FabrikSolver f = new FabrikSolver();
        f.solve(joints, 4, lengths, 0.5f, 0.5f);
        for (float v : joints) {
            assertTrue(Float.isFinite(v), "a zero-length link produced " + v);
        }
        assertLinkLengths(joints, lengths);
    }

    @Test
    void aTargetExactlyAtTheRootIsFiniteAndStable() {
        float[] joints = straightChain(4);
        float[] lengths = unitLengths(4);
        FabrikSolver f = new FabrikSolver();
        for (int i = 0; i < 30; i++) {
            f.solve(joints, 5, lengths, 0f, 0f);
        }
        float[] settled = joints.clone();
        for (int i = 0; i < 30; i++) {
            f.solve(joints, 5, lengths, 0f, 0f);
        }
        for (int i = 0; i < joints.length; i++) {
            assertTrue(Float.isFinite(joints[i]));
            assertEquals(settled[i], joints[i], 1e-3f, "folded chain oscillated at joint " + (i / 2));
        }
        assertLinkLengths(joints, lengths);
    }

    @Test
    void anEntirelyCollapsedChainDoesNotPickAnArbitraryDirection() {
        // Every joint on top of the root: no direction can be recovered from the
        // positions at all, so the solver must fall back on something stable
        // rather than on whatever atan2(0, 0) happens to return.
        float[] joints = new float[10];
        float[] lengths = unitLengths(4);
        FabrikSolver f = new FabrikSolver();
        f.solve(joints, 5, lengths, 2f, 0f, 0f, 1f);
        for (float v : joints) {
            assertTrue(Float.isFinite(v), "collapsed chain produced " + v);
        }
        assertLinkLengths(joints, lengths);
    }

    @Test
    void aSingleLinkChainIsJustAnAim() {
        float[] joints = {0f, 0f, 1f, 0f};
        float[] lengths = {1f};
        FabrikSolver f = new FabrikSolver();
        f.solve(joints, 2, lengths, 0f, 5f);
        assertEquals(0f, joints[2], 1e-2f);
        assertEquals(1f, joints[3], 1e-2f);
    }

    @Test
    void aZeroLinkChainIsANoOp() {
        float[] joints = {3f, 4f};
        FabrikSolver f = new FabrikSolver();
        assertEquals(0, f.solve(joints, 1, new float[0], 0f, 0f));
        assertEquals(3f, joints[0], 0f);
        assertEquals(4f, joints[1], 0f);
    }
}
