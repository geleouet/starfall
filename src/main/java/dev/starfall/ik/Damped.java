package dev.starfall.ik;

/**
 * One critically damped scalar. Used for every quantity in this package that has
 * to change over time rather than instantly: the IK target the chain is actually
 * chasing, and the bend side of a two-bone limb.
 *
 * <p>Critically damped rather than a spring with a tunable ratio because
 * STYLE.md 7.2 explicitly bans "mechanical overshoot-and-return": an
 * underdamped response that crosses the target and comes back is the single
 * clearest tell that a machine is driving the limb. Critical damping is the
 * fastest response that never crosses, so it is the only setting that is both
 * responsive and legal here.
 *
 * <p>The integrator is the standard closed-form critically damped step (the one
 * popularised as {@code SmoothDamp}) rather than an explicit Euler spring,
 * because an explicit spring goes unstable when {@code dt} is large relative to
 * the time constant -- and this runs off a real frame clock that will
 * occasionally hitch.
 */
final class Damped {

    float value;
    float velocity;

    Damped(float value) {
        this.value = value;
    }

    /** Teleports: no lag, no residual velocity. For scene setup and respawns, not for frames. */
    void set(float v) {
        value = v;
        velocity = 0f;
    }

    /**
     * How much faster the internal time constant runs than the settle time the
     * caller asked for. A critically damped response is within 5% of its target
     * after {@code 4.74 / omega} seconds and {@code omega = 2 / smoothTime}, so
     * "arrived" happens at {@code 2.37 * smoothTime}. Dividing here means
     * {@code settleSeconds} means what STYLE.md 7.1 means by it -- the time the
     * motion visibly takes -- instead of an internal filter constant that would
     * be off by more than a factor of two.
     */
    private static final float SETTLE_TO_SMOOTH_TIME = 2.37f;

    /**
     * Advances toward {@code target}. {@code settleSeconds} is how long the
     * approach visibly takes; STYLE.md 7.1 wants 0.3-0.6 s for a terminal settle.
     *
     * <p>A non-positive {@code dt} teleports, so callers doing one-shot
     * deterministic solves (tests, editor tooling) do not have to spin frames.
     */
    void step(float target, float settleSeconds, float dt) {
        if (dt <= 0f || settleSeconds <= IkMath.EPS) {
            set(target);
            return;
        }
        float omega = 2f * SETTLE_TO_SMOOTH_TIME / settleSeconds;
        float x = omega * dt;
        // Pade-style approximation of exp(-x); cheaper than Math.exp and, more
        // importantly, monotone and stable for every dt >= 0.
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);
        float change = value - target;
        float temp = (velocity + omega * change) * dt;
        velocity = (velocity - omega * temp) * exp;
        value = target + (change + temp) * exp;
    }
}
