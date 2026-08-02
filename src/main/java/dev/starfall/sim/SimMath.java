package dev.starfall.sim;

/**
 * Scalar helpers for the Verlet simulation.
 *
 * <p>Deliberately a sibling of {@code dev.starfall.ik.IkMath} rather than a
 * dependency on it: that class is package-private by design and the two packages
 * are meant to be independently replaceable. The shared discipline, not the
 * shared code, is what matters -- every operation here that would normally be a
 * {@code min}/{@code max} or a branch is monotone and C1, because STYLE.md 7.2
 * bans snapping and a derivative discontinuity in a quantity driving a moving
 * strand reads on screen as a pop.
 *
 * <p><strong>Trig is {@link Math}, never {@code MathUtils}.</strong> libGDX's
 * {@code cosDeg}/{@code sinDeg} are a 14-bit lookup with no interpolation and up
 * to ~4e-4 of error. That is invisible in a skinning matrix and fatal in an
 * iterative solver: a bend constraint whose rest direction jitters by 4e-4 never
 * lets the chain settle, so a strand that has visually stopped keeps taking
 * corrections every frame -- per-frame jitter on something that should be still.
 * System 2 found this the hard way in FABRIK; this package starts from the
 * answer.
 */
final class SimMath {

    /** Below this a vector's direction is noise, not information. */
    static final float EPS = 1e-6f;

    static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    private SimMath() {
    }

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    static float lerp(float a, float b, float s) {
        return a + (b - a) * s;
    }

    static float smoothstep(float x) {
        float c = clamp01(x);
        return c * c * (3f - 2f * c);
    }

    /**
     * Monotone C1 ceiling: identity up to {@code knee}, then asymptotic to
     * {@code limit}. The same shape {@code IkMath.softCeil} uses and for the same
     * reason -- a hard clamp gives whatever it limits an infinite derivative at
     * the moment it engages, which is the "arm hits end of travel" pop moved to a
     * hair tip.
     */
    static float softCeil(float v, float knee, float limit) {
        float band = limit - knee;
        if (band <= EPS) {
            return Math.min(v, limit);
        }
        if (v <= knee) {
            return v;
        }
        return limit - band * (float) Math.exp(-(v - knee) / band);
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

    static float atan2Deg(float y, float x) {
        if (Math.abs(x) < EPS && Math.abs(y) < EPS) {
            return 0f;
        }
        return (float) Math.atan2(y, x) * RAD_TO_DEG;
    }

    static float cosDeg(float deg) {
        return (float) Math.cos(deg * DEG_TO_RAD);
    }

    static float sinDeg(float deg) {
        return (float) Math.sin(deg * DEG_TO_RAD);
    }

    static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * A cheap deterministic scalar in 0..1 from an integer seed. Used for
     * per-strand variation, which STYLE.md 4 requires ("strands must not move in
     * unison") and which must be identical on every run -- a capture that differs
     * between two invocations cannot be compared with the previous pass.
     */
    static float hash01(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return (h >>> 8) / (float) (1 << 24);
    }

    /** Deterministic value in {@code [lo, hi)} from a seed and a channel index. */
    static float hashRange(int seed, int channel, float lo, float hi) {
        return lo + (hi - lo) * hash01(seed * 131 + channel * 7919 + 17);
    }
}
