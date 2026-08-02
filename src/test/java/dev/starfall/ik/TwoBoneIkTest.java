package dev.starfall.ik;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Correctness of the analytic limb solver in isolation, with no skeleton in the
 * way. The continuity properties -- which are the ones this project actually
 * lives or dies on -- are pinned separately in {@link IkContinuityTest}; what is
 * here is "does it solve the triangle, does the pole decide the bend, does it
 * survive degenerate input".
 */
class TwoBoneIkTest {

    private static final float EPS = 1e-4f;
    /** Length of one bone in every fixture below; a two-bone limb of total length 2. */
    private static final float L = 1f;

    private final TwoBoneIk.Result r = new TwoBoneIk.Result();

    /** One cold solve at the origin with unit bones, pole above the axis unless told otherwise. */
    private void solve(TwoBoneIk ik, float tx, float ty, float px, float py) {
        ik.solve(0f, 0f, L, L, tx, ty, px, py, 0f, 1f, 0f, r);
    }

    @Test
    void reachableTargetLandsOnTheTarget() {
        TwoBoneIk ik = new TwoBoneIk();
        solve(ik, 1.2f, 0.6f, 0f, 1f);
        assertEquals(1.2f, r.endX, 1e-3f, "end effector should sit on a comfortably reachable target");
        assertEquals(0.6f, r.endY, 1e-3f);
    }

    @Test
    void boneLengthsAreExactWhateverTheTarget() {
        // The one invariant that must survive every softening and every limit: the
        // solved pose is always a real pose of a real limb. If this breaks, the
        // mesh candy-wraps (STYLE.md 7.2).
        TwoBoneIk ik = new TwoBoneIk();
        for (float a = 0f; a < 360f; a += 11f) {
            for (float d = 0f; d <= 3f; d += 0.31f) {
                solve(ik, d * IkMath.cosDeg(a), d * IkMath.sinDeg(a), 0f, 1f);
                float jx = L * IkMath.cosDeg(r.rootWorldDeg);
                float jy = L * IkMath.sinDeg(r.rootWorldDeg);
                assertEquals(L, IkMath.length(jx, jy), EPS);
                assertEquals(L, IkMath.length(r.endX - jx, r.endY - jy), EPS,
                        "lower bone stretched at angle " + a + " distance " + d);
            }
        }
    }

    @Test
    void poleDecidesTheBendDirection() {
        solve(new TwoBoneIk(), 1.4f, 0f, 0f, 5f);
        assertTrue(L * IkMath.sinDeg(r.rootWorldDeg) > 0.1f,
                "pole above the axis must put the elbow above the axis");

        solve(new TwoBoneIk(), 1.4f, 0f, 0f, -5f);
        assertTrue(L * IkMath.sinDeg(r.rootWorldDeg) < -0.1f,
                "pole below the axis must put the elbow below it");
    }

    @Test
    void samePoleAndTargetAlwaysGiveTheSameAnswer() {
        // Determinism is what makes the pole worth having at all: a solver that
        // picked a side from whatever floating-point noise was nearest would give a
        // different silhouette every time the same strike replayed.
        float firstRoot = Float.NaN;
        float firstJoint = Float.NaN;
        for (int trial = 0; trial < 5; trial++) {
            TwoBoneIk ik = new TwoBoneIk();
            solve(ik, 0.9f, -1.1f, -2f, 3f);
            if (trial == 0) {
                firstRoot = r.rootWorldDeg;
                firstJoint = r.jointWorldDeg;
            } else {
                assertEquals(firstRoot, r.rootWorldDeg, 0f, "cold solves must be bit-identical");
                assertEquals(firstJoint, r.jointWorldDeg, 0f);
            }
        }
    }

    @Test
    void unreachableTargetExtendsAlongTheRayWithoutReachingIt() {
        TwoBoneIk ik = new TwoBoneIk();
        solve(ik, 6f, 0f, 0f, 1f);

        float reach = IkMath.length(r.endX, r.endY);
        assertTrue(reach < 2f, "the limb must never actually straighten fully; got " + reach);
        assertTrue(reach > 1.99f, "but far out of reach it should be essentially extended; got " + reach);
        assertEquals(0f, r.endY, 1e-2f, "and it should point straight at the target");
    }

    @Test
    void anUnreachableTargetDoesNotOscillateAcrossRepeatedSolves() {
        // The failure this guards against is a solver that alternates between "as
        // far as I can go" and "snapped straight" on successive frames.
        TwoBoneIk ik = new TwoBoneIk();
        ik.solve(0f, 0f, L, L, 4f, 1f, 0f, 3f, 0f, 1f, 0f, r);
        float rootA = r.rootWorldDeg;
        float jointA = r.jointWorldDeg;
        for (int i = 0; i < 200; i++) {
            ik.solve(0f, 0f, L, L, 4f, 1f, 0f, 3f, 0f, 1f, 1f / 60f, r);
        }
        assertEquals(0f, IkMath.deltaDeg(rootA, r.rootWorldDeg), 1e-2f,
                "an unmoving out-of-reach target must produce an unmoving pose");
        assertEquals(0f, IkMath.deltaDeg(jointA, r.jointWorldDeg), 1e-2f);
    }

    @Test
    void reachingOutwardStraightensMonotonicallyAndNeverStops() {
        // A hard clamp gives a flat line past d = 2: the limb opens, opens, and
        // then stops on one frame. The soft ceiling must keep opening, by less and
        // less, well past the nominal reach.
        TwoBoneIk ik = new TwoBoneIk();
        float prevOpening = -1f;
        for (float d = 0.2f; d <= 2.5f; d += 0.02f) {
            solve(ik, d, 0f, 0f, 1f);
            float opening = 180f - Math.abs(IkMath.deltaDeg(r.rootWorldDeg, r.jointWorldDeg));
            assertTrue(opening > prevOpening,
                    "the limb must keep opening as the target recedes; stalled at d = " + d);
            prevOpening = opening;
        }
        assertTrue(prevOpening < 179.9f,
                "and 25% past its own reach it must still not be exactly straight; got " + prevOpening);
    }

    @Test
    void anAbsurdlyDistantTargetStaysFinite() {
        TwoBoneIk ik = new TwoBoneIk();
        solve(ik, 1e6f, -1e6f, 0f, 1f);
        assertFinite();
        assertEquals(2f, IkMath.length(r.endX, r.endY), 1e-3f);
    }

    @Test
    void jointLimitIsRespectedIncludingWellPastIt() {
        // Elbow allowed only to bend one way, and only so far.
        TwoBoneIk ik = new TwoBoneIk().jointLimit(IkConstraint.range(-100f, -10f));
        for (float a = 0f; a < 360f; a += 5f) {
            for (float d = 0.1f; d <= 2.5f; d += 0.3f) {
                solve(ik, d * IkMath.cosDeg(a), d * IkMath.sinDeg(a), 0f, 3f);
                assertTrue(r.jointLocalDeg > -100f && r.jointLocalDeg < -10f,
                        "elbow at " + r.jointLocalDeg + " escaped its limit at angle " + a);
            }
        }
    }

    @Test
    void aLimitedJointStillAimsTheEndEffectorAtTheTarget() {
        // The elbow is not allowed to close far enough to bring the hand in to
        // 0.5, so the hand misses. It must miss *along the ray* -- too far, not
        // sideways. That is what makes a constrained parry read as "giving ground"
        // rather than "reaching somewhere else entirely".
        TwoBoneIk ik = new TwoBoneIk().jointLimit(IkConstraint.range(-30f, -5f));
        solve(ik, 0.5f, 0f, 0f, 3f);
        assertTrue(Math.abs(IkMath.length(r.endX, r.endY) - 0.5f) > 0.1f,
                "the limit should stop the hand from arriving");
        assertEquals(0f, r.endY, 2e-2f, "but it must stay on the ray to the target");
        assertTrue(r.endX > 0f, "and on the correct side of the shoulder");
    }

    @Test
    void rootLimitIsRespected() {
        TwoBoneIk ik = new TwoBoneIk().rootLimit(IkConstraint.range(-45f, 45f));
        for (float a = 0f; a < 360f; a += 5f) {
            solve(ik, 1.5f * IkMath.cosDeg(a), 1.5f * IkMath.sinDeg(a), 0f, 3f);
            assertTrue(r.rootLocalDeg > -45f && r.rootLocalDeg < 45f,
                    "shoulder at " + r.rootLocalDeg + " escaped its limit aiming at " + a);
        }
    }

    @Test
    void mirroredChainsProduceLocalAnglesThatMatchBoneRotDeg() {
        // With mirror = -1 (a fighter facing the other way via a negative root
        // scale) the same world pose must come back as the negated local angle,
        // because positive rotation turns the other way through a mirror.
        TwoBoneIk plain = new TwoBoneIk();
        plain.solve(0f, 0f, L, L, 1.2f, 0.6f, 0f, 2f, 0f, 1f, 0f, r);
        float plainRoot = r.rootLocalDeg;
        float plainJoint = r.jointLocalDeg;

        TwoBoneIk mirrored = new TwoBoneIk();
        mirrored.solve(0f, 0f, L, L, 1.2f, 0.6f, 0f, 2f, 0f, -1f, 0f, r);
        assertEquals(-plainRoot, r.rootLocalDeg, 1e-3f);
        assertEquals(-plainJoint, r.jointLocalDeg, 1e-3f);
    }

    // -- degenerate inputs ----------------------------------------------------

    @Test
    void zeroLengthLowerBoneIsFiniteAndAimsAtTheTarget() {
        TwoBoneIk ik = new TwoBoneIk();
        ik.solve(0f, 0f, L, 0f, 0.4f, 0.4f, 0f, 1f, 0f, 1f, 0f, r);
        assertFinite();
        assertEquals(45f, IkMath.wrapDeg(r.rootWorldDeg), 1e-2f, "a rigid limb can only aim");
        assertEquals(L, IkMath.length(r.endX, r.endY), EPS);
    }

    @Test
    void zeroLengthUpperBoneIsFinite() {
        TwoBoneIk ik = new TwoBoneIk();
        ik.solve(0f, 0f, 0f, L, -0.5f, 0.2f, 0f, 1f, 0f, 1f, 0f, r);
        assertFinite();
        assertEquals(L, IkMath.length(r.endX, r.endY), EPS);
    }

    @Test
    void bothBonesZeroLengthIsFinite() {
        TwoBoneIk ik = new TwoBoneIk();
        ik.solve(0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 1f, 0f, r);
        assertFinite();
        assertEquals(0f, r.endX, EPS);
        assertEquals(0f, r.endY, EPS);
    }

    @Test
    void targetExactlyAtTheRootIsFiniteAndDoesNotSpin() {
        TwoBoneIk ik = new TwoBoneIk();
        solve(ik, 1.5f, 0f, 0f, 1f);
        float before = r.rootWorldDeg;
        for (int i = 0; i < 30; i++) {
            ik.solve(0f, 0f, L, L, 0f, 0f, 0f, 1f, 0f, 1f, 1f / 60f, r);
            assertFinite();
        }
        // No information about where to aim, so it must hold its last aim rather
        // than pick an arbitrary one and rotate the whole limb there.
        assertTrue(Math.abs(IkMath.deltaDeg(before, r.rootWorldDeg)) < 95f,
                "a target at the shoulder must not make the limb spin");
    }

    @Test
    void unequalBoneLengthsFoldToTheirRealMinimumReach() {
        // l1 = 1, l2 = 0.4 cannot bring the hand closer than 0.6 to the shoulder.
        TwoBoneIk ik = new TwoBoneIk();
        ik.solve(0f, 0f, 1f, 0.4f, 0f, 0f, 0f, 1f, 0f, 1f, 0f, r);
        assertFinite();
        float reach = IkMath.length(r.endX, r.endY);
        assertTrue(reach > 0.5999f, "must not fold past the geometric minimum; got " + reach);
        assertTrue(reach < 0.75f, "but should get close to it; got " + reach);
    }

    private void assertFinite() {
        assertTrue(Float.isFinite(r.rootWorldDeg), "rootWorldDeg was " + r.rootWorldDeg);
        assertTrue(Float.isFinite(r.jointWorldDeg), "jointWorldDeg was " + r.jointWorldDeg);
        assertTrue(Float.isFinite(r.endX) && Float.isFinite(r.endY), "end effector was not finite");
    }
}
