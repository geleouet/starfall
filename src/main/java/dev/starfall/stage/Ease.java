package dev.starfall.stage;

/**
 * How a ramp gets from one value to another.
 *
 * <p>STYLE.md 9 requires every camera move to be "eased and continuous" and 7.1
 * opens with "slow in, slower out. Nothing arrives at rest abruptly", so a
 * directive that carried only a start and an end would leave the one property the
 * rubric grades to the renderer's taste. The curve travels with the directive.
 *
 * <p>All four are monotone with {@code f(0) = 0} and {@code f(1) = 1}, which is
 * what lets a schedule be checked for continuity by comparing endpoints alone.
 */
public enum Ease {

    /** For things that genuinely are uniform, which is almost nothing. */
    LINEAR {
        @Override
        public double apply(double u) {
            return u;
        }
    },

    /** Arrives softly, leaves hard. For a release out of a wind-up. */
    EASE_OUT {
        @Override
        public double apply(double u) {
            double v = 1.0 - u;
            return 1.0 - v * v * v;
        }
    },

    /** Symmetric. For a weight ramp that has no arrival to protect. */
    EASE_IN_OUT {
        @Override
        public double apply(double u) {
            return u * u * (3.0 - 2.0 * u);
        }
    },

    /**
     * STYLE.md 7.1's own phrase, and deliberately asymmetric: peak speed lands a
     * third of the way through, so the tail is twice as long as the head. A
     * symmetric ease is "slow in, slow out", which is not what the rubric says.
     *
     * <p>{@code 1 - (1-u)^3 (1 + 3u)}, whose derivative is {@code 12u(1-u)^2}. Zero
     * slope at both ends, continuous everywhere, and a peak slope of 16/9 -- which
     * matters: a piecewise ease with a slope step in the middle produces a visible
     * kink in a camera move, and a peak slope much above 2 turns STYLE.md 9's
     * half-second push-in on a long lane into something the eye reads as a snap.
     */
    SLOW_IN_SLOWER_OUT {
        @Override
        public double apply(double u) {
            double v = 1.0 - u;
            return 1.0 - v * v * v * (1.0 + 3.0 * u);
        }
    };

    /** The eased fraction for a raw fraction {@code u}, clamped to {@code 0..1}. */
    public abstract double apply(double u);

    /** {@code from} to {@code to} at raw fraction {@code u}. */
    public double lerp(double from, double to, double u) {
        double c = u <= 0.0 ? 0.0 : u >= 1.0 ? 1.0 : u;
        return from + (to - from) * apply(c);
    }
}
