package dev.starfall.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed-substep driver every simulated thing in this package runs under.
 *
 * <p>Verlet integration is only well-behaved at a constant timestep -- the
 * displacement it carries as velocity is implicitly divided by whatever
 * {@code dt} produced it, so feeding it a varying one changes the effective
 * damping frame to frame and, at a hitch, injects energy. So the accumulator
 * lives here rather than in each chain, and every chain is advanced by exactly
 * {@link #SUBSTEP} or not at all.
 *
 * <p>1/240 s matches {@code CaptureApp.SUBSTEP} on purpose: under the capture
 * harness the accumulator therefore runs exactly one substep per
 * {@code Scene.update}, which makes a capture bit-for-bit reproducible and makes
 * the sim's own timeline identical to the scene's. Off the harness (a live
 * window at 60 Hz) it runs four, and the result is the same motion.
 *
 * <p>The leftover accumulator is deliberately <em>not</em> interpolated away.
 * Rendering the last completed substep rather than a blend of two is what keeps
 * a still chain genuinely still: an interpolated position drifts by a fraction
 * of a substep every frame, which at hair-tip scale is visible shimmer on
 * something that has stopped.
 */
public final class VerletSolver {

    /** Matches {@code dev.starfall.capture.CaptureApp}'s own substep. */
    public static final float SUBSTEP = 1f / 240f;

    /**
     * Ceiling on substeps per update, so a debugger pause or a first-frame stall
     * cannot turn into a multi-second simulation burst that looks like an
     * explosion. The excess time is discarded rather than banked.
     */
    private static final int MAX_SUBSTEPS = 16;

    /** Anything the solver advances. Chains implement it directly; owners may implement it to refresh anchors mid-step. */
    public interface Stepped {
        void substep(float dt);
    }

    private final List<Stepped> items = new ArrayList<>();
    private float accumulator;

    public VerletSolver add(Stepped item) {
        items.add(item);
        return this;
    }

    public int size() {
        return items.size();
    }

    /** Advances everything registered. Deterministic for any given sequence of {@code dt}. */
    public void update(float dt) {
        if (dt <= 0f) {
            return;
        }
        accumulator += dt;
        int steps = 0;
        while (accumulator >= SUBSTEP && steps < MAX_SUBSTEPS) {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).substep(SUBSTEP);
            }
            accumulator -= SUBSTEP;
            steps++;
        }
        if (steps == MAX_SUBSTEPS) {
            accumulator = 0f;
        }
    }

    /** Drops any banked time. Paired with a chain reset, for scene setup. */
    public void clear() {
        accumulator = 0f;
    }
}
