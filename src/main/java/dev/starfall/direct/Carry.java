package dev.starfall.direct;

/**
 * One critically damped scalar, with the same integrator and the same meaning of
 * "settle" as {@code dev.starfall.ik.Damped}.
 *
 * <p>It is a copy rather than a reuse because {@code Damped} is package-private in
 * the IK package, and widening it to drive a root bone would be widening it for
 * the wrong reason -- it is documented there as the filter on "every quantity in
 * this package". What matters is that the two agree, because the pelvis carried by
 * this filter and the trunk carried by {@code IkChain}'s are the two halves of one
 * lean: if they settled by different laws the trunk would arrive before or after
 * the body it hangs off, and STYLE.md 7.0.3's chain of arrivals would be measuring
 * an artefact of the filter rather than the rig.
 *
 * <p>Critically damped, for STYLE.md 7.2's reason: "a spring that visibly
 * oscillates twice and stops reads as a machine." Critical damping is the fastest
 * response that never crosses its target, so it is the only legal setting.
 */
final class Carry {

    /** See {@code Damped.SETTLE_TO_SMOOTH_TIME}: makes {@code settle} mean STYLE.md 7.1's. */
    private static final double SETTLE_TO_SMOOTH_TIME = 2.37;

    private double value;
    private double velocity;

    Carry(double value) {
        this.value = value;
    }

    double value() {
        return value;
    }

    /** Teleports. Scene setup only. */
    void set(double v) {
        value = v;
        velocity = 0.0;
    }

    void step(double target, double settleSeconds, double dt) {
        if (dt <= 0.0 || settleSeconds <= 1e-6) {
            set(target);
            return;
        }
        double omega = 2.0 * SETTLE_TO_SMOOTH_TIME / settleSeconds;
        double x = omega * dt;
        // The same Pade-style approximation of exp(-x): monotone and stable for
        // every dt >= 0, which an explicit spring is not.
        double exp = 1.0 / (1.0 + x + 0.48 * x * x + 0.235 * x * x * x);
        double change = value - target;
        double temp = (velocity + omega * change) * dt;
        velocity = (velocity - omega * temp) * exp;
        value = target + (change + temp) * exp;
    }
}
