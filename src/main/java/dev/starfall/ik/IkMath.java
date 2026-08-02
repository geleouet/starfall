package dev.starfall.ik;

import com.badlogic.gdx.math.MathUtils;

/**
 * Scalar helpers shared by the IK solvers.
 *
 * <p>Almost everything here exists to serve one rule: STYLE.md section 7.2 bans
 * snapping, and a textbook IK solver snaps in three specific places -- a hard
 * {@code min(d, l1+l2)} at the reach boundary, a hard {@code clamp()} on a joint
 * limit, and a sign flip on the bend direction. All three are branches whose
 * output is continuous but whose *derivative* is not, and a derivative
 * discontinuity in a pose driven by a moving target reads on screen as a pop.
 * So every operation in this file that would normally be a min/max or a branch
 * is replaced by something monotone and C1.
 *
 * <p>The soft limits below are all built from the same shape: pass the input
 * through untouched until a knee, then decay exponentially toward the bound. The
 * exponential is chosen over a smoothstep because it has slope exactly 1 at the
 * knee (so it joins the identity segment with no kink) and never actually
 * reaches the bound (so downstream {@code acos} arguments stay strictly inside
 * (-1, 1) and the singular fully-extended configuration is unreachable).
 */
final class IkMath {

    /** Below this a vector's direction is noise, not information. */
    static final float EPS = 1e-6f;

    private IkMath() {
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * Monotone C1 ceiling: identity up to {@code knee}, then asymptotic to
     * {@code limit}. Requires {@code knee < limit}.
     *
     * <p>The result is strictly below {@code limit} for every finite input, which
     * is what keeps a two-bone limb from ever reaching the exactly-straight
     * configuration where the elbow angle stops being a differentiable function
     * of the target.
     */
    static float softCeil(float v, float knee, float limit) {
        float band = limit - knee;
        if (band <= EPS) {
            return Math.min(v, limit);
        }
        if (v <= knee) {
            return v;
        }
        return Math.min(limit - band * (float) Math.exp(-(v - knee) / band), limit - gap(band, limit));
    }

    /** Mirror of {@link #softCeil}: identity down to {@code knee}, then asymptotic to {@code limit < knee}. */
    static float softFloor(float v, float knee, float limit) {
        float band = knee - limit;
        if (band <= EPS) {
            return Math.max(v, limit);
        }
        if (v >= knee) {
            return v;
        }
        return Math.max(limit + band * (float) Math.exp(-(knee - v) / band), limit + gap(band, limit));
    }

    /**
     * Smallest separation from the bound that a float can actually represent
     * here. In exact arithmetic the exponential never reaches the limit, but in
     * float it does: a few band-widths out, {@code limit - band * exp(-x)} rounds
     * to {@code limit} exactly, and then a two-bone limb *is* perfectly straight
     * and its {@code acos} argument *is* exactly -1 -- the singular pose the
     * softening exists to avoid. Holding a representable gap keeps the guarantee
     * literally true. It costs a derivative kink, but only where the derivative
     * has already decayed to ~1e-6, so nothing can see it.
     */
    private static float gap(float band, float limit) {
        return Math.max(band * 1e-5f, Math.abs(limit) * 4e-6f + 1e-7f);
    }

    /** Shortest signed rotation from {@code a} to {@code b}, in (-180, 180]. */
    static float deltaDeg(float a, float b) {
        float d = (b - a) % 360f;
        if (d > 180f) {
            d -= 360f;
        } else if (d < -180f) {
            d += 360f;
        }
        return d;
    }

    /** Wraps an angle into (-180, 180]. */
    static float wrapDeg(float deg) {
        return deltaDeg(0f, deg);
    }

    static float atan2Deg(float y, float x) {
        if (Math.abs(x) < EPS && Math.abs(y) < EPS) {
            // atan2(0, 0) is defined as 0 by the JDK, but relying on that hides a
            // degenerate chain from the caller. Being explicit keeps the zero-length
            // bone case obviously deliberate rather than accidentally NaN-free.
            return 0f;
        }
        return (float) Math.atan2(y, x) * MathUtils.radDeg;
    }

    static float acosDeg(float cos) {
        return (float) Math.acos(clamp(cos, -1f, 1f)) * MathUtils.radDeg;
    }

    /**
     * Deliberately {@link Math#cos} rather than {@code MathUtils.cosDeg}.
     * libGDX's version is a 14-bit lookup with no interpolation, so it carries
     * up to ~4e-4 of error. That is invisible in a skinning matrix and fatal in
     * an iterative solver: FABRIK would stall at a residual it can never get
     * below its own tolerance, and the stagnation bail would then re-run a
     * couple of iterations every frame on a chain that has actually finished,
     * which is a per-frame jitter on a limb that should be still. IK runs a few
     * dozen trig calls per frame, not a few million, so exact trig is free here.
     */
    static float cosDeg(float deg) {
        return (float) Math.cos(deg * MathUtils.degRad);
    }

    static float sinDeg(float deg) {
        return (float) Math.sin(deg * MathUtils.degRad);
    }

    static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }
}
