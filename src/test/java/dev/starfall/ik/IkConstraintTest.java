package dev.starfall.ik;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A joint limit is applied inside both solvers, on every intermediate angle they
 * compute. That is only safe if the limit itself cannot manufacture a
 * discontinuity, so the properties pinned here are what license the solvers to
 * use it that way: monotone, non-expanding, identity in the interior, strictly
 * inside the bounds. Break any one of them and the continuity sweeps in
 * {@link IkContinuityTest} start failing for reasons that look like solver bugs.
 */
class IkConstraintTest {

    private static final float EPS = 1e-4f;

    @Test
    void freeConstraintIsTheIdentity() {
        IkConstraint free = IkConstraint.free();
        for (float deg = -720f; deg <= 720f; deg += 7f) {
            assertEquals(deg, free.apply(deg), 0f, "a free joint must not touch the angle at all");
        }
    }

    @Test
    void anglesWellInsideTheRangePassThroughUntouched() {
        // An inactive limit must cost nothing in accuracy, otherwise every chain
        // in the rig would solve slightly wrong just for having limits declared.
        IkConstraint c = IkConstraint.range(-90f, 30f);
        for (float deg = -70f; deg <= 10f; deg += 1f) {
            assertEquals(deg, c.apply(deg), EPS, "interior angles must be exact");
        }
    }

    @Test
    void outputAlwaysStaysStrictlyInsideTheRange() {
        IkConstraint c = IkConstraint.range(-90f, 30f);
        for (float deg = -720f; deg <= 720f; deg += 3f) {
            float v = c.apply(deg);
            assertTrue(v > -90f && v < 30f,
                    "apply(" + deg + ") = " + v + " escaped [-90, 30]");
        }
    }

    @Test
    void applyIsMonotoneAndNonExpanding() {
        // The load-bearing property. A 1-Lipschitz monotone map sends a continuous
        // input to a continuous output with no amplification, which is exactly the
        // licence the solvers need to apply limits mid-solve instead of clamping
        // the finished pose.
        IkConstraint c = IkConstraint.range(-120f, 45f);
        float prev = c.apply(-400f);
        for (float deg = -400f + 0.25f; deg <= 400f; deg += 0.25f) {
            float v = c.apply(deg);
            assertTrue(v >= prev - EPS, "apply must be monotone, broke near " + deg);
            assertTrue(v - prev <= 0.25f + EPS,
                    "apply must not expand: 0.25 deg of input became " + (v - prev) + " near " + deg);
            prev = v;
        }
    }

    @Test
    void theLimitIsApproachedRatherThanHit() {
        // A hard clamp has zero slope past the bound: the joint stops dead and the
        // limb reads as hitting a mechanical stop. The soft limit keeps moving,
        // just less and less.
        IkConstraint c = IkConstraint.range(-90f, 30f);
        float at = c.apply(30f);
        float past = c.apply(40f);
        float farPast = c.apply(200f);

        assertTrue(at < 30f, "the bound itself must not be reachable");
        assertTrue(past > at, "the joint must keep easing outward past the bound, not stop");
        assertTrue(farPast > past, "and keep easing, monotonically");
        assertTrue(farPast < 30f, "while never escaping");
        assertTrue(30f - farPast < 0.5f, "far past the bound it should be essentially at the limit");
    }

    @Test
    void zeroSoftnessDegeneratesToAHardClamp() {
        // Offered only so the difference is demonstrable; nothing in the rig should
        // use it.
        IkConstraint hard = IkConstraint.range(-90f, 30f, 0f);
        assertEquals(30f, hard.apply(30f), EPS);
        assertEquals(30f, hard.apply(200f), EPS);
        assertEquals(-90f, hard.apply(-200f), EPS);
    }

    @Test
    void aroundBuildsASymmetricRange() {
        IkConstraint c = IkConstraint.around(-40f, 25f);
        assertEquals(-65f, c.minDeg(), EPS);
        assertEquals(-15f, c.maxDeg(), EPS);
        assertEquals(-40f, c.restDeg(), EPS);
    }

    @Test
    void invalidRangesAreRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> IkConstraint.range(30f, 30f));
        assertThrows(IllegalArgumentException.class, () -> IkConstraint.range(40f, 10f));
        // Relative joint angles are measured by shortest arc, so a range that
        // straddles +-180 has no consistent meaning and must not be constructible.
        assertThrows(IllegalArgumentException.class, () -> IkConstraint.range(-200f, 10f));
        assertThrows(IllegalArgumentException.class, () -> IkConstraint.range(10f, 200f));
    }

    @Test
    void aSoftLimitStillDistinguishesNearbyAngles() {
        // If the soft band collapsed everything onto the bound, a limb pressed into
        // its limit would freeze; small target changes must still register.
        IkConstraint c = IkConstraint.range(0f, 100f);
        assertTrue(c.apply(96f) - c.apply(95f) > 1e-3f, "the soft band must not flatten");
    }
}
