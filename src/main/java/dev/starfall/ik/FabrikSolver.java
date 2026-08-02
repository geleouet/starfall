package dev.starfall.ik;


/**
 * Forward-and-backward reaching inverse kinematics for chains longer than two
 * bones: the spine now, hair and cloth strands later. Iterative, so unlike
 * {@link TwoBoneIk} it has a convergence budget.
 *
 * <p>Two departures from the published algorithm, both for STYLE.md 7.2:
 *
 * <ol>
 *   <li><b>There is no out-of-reach branch.</b> Textbook FABRIK tests
 *       {@code |target - root| > sum(lengths)} and, if so, abandons the
 *       iteration and lays every joint out along the straight line to the
 *       target. The two branches agree in the limit, but the iterative branch
 *       needs unboundedly many iterations as the boundary is approached, so with
 *       a fixed budget the chain visibly firms up and snaps straight as the
 *       target crosses. Here the target distance is passed through
 *       {@link IkMath#softCeil} first, so the effective target is always
 *       strictly inside reach, the straight-line branch is unreachable, and the
 *       iteration count needed stays bounded. The chain eases into extension
 *       instead of locking.</li>
 *   <li><b>The loop stops when it stops improving</b>, not only when it
 *       converges. A constrained chain often cannot reach its target at all, and
 *       burning the full budget on a configuration that is already at its fixed
 *       point is exactly how an IK chain acquires a per-frame jitter. Bailing on
 *       stagnation also makes {@link #solve} idempotent: solving an already
 *       solved chain does nothing, so a target that has stopped moving produces a
 *       pose that has stopped moving.</li>
 * </ol>
 *
 * <p>Joint positions are passed in and out as a flat {@code [x0, y0, x1, y1,
 * ...]} array with {@code jointCount} points and {@code jointCount - 1} links:
 * joint 0 is the pinned chain root, the last joint is the end effector. Flat
 * float arrays rather than {@code Vector2[]} because this runs per chain per
 * frame and must not allocate.
 */
public final class FabrikSolver {

    private int maxIterations = 12;
    private float tolerance = 1e-4f;
    private float reachSoftness = 0.05f;

    private IkConstraint[] limits = new IkConstraint[0];

    private float lastResidual;
    private int lastIterations;

    /** Scratch for the reference direction walk; sized lazily, never per-solve. */
    private float[] dirScratch = new float[0];

    public FabrikSolver maxIterations(int n) {
        this.maxIterations = Math.max(1, n);
        return this;
    }

    /** Distance below which the end effector counts as arrived, in world units. */
    public FabrikSolver tolerance(float t) {
        this.tolerance = Math.max(IkMath.EPS, t);
        return this;
    }

    /** Fraction of the chain's total length over which it eases into full extension. */
    public FabrikSolver reachSoftness(float fraction) {
        this.reachSoftness = IkMath.clamp(fraction, 0f, 0.25f);
        return this;
    }

    /**
     * Per-joint limits. Index {@code i} limits link {@code i}'s direction
     * relative to link {@code i - 1}; index 0 is measured against the
     * {@code referenceDeg} passed to {@link #solve}. Nulls mean free.
     */
    public FabrikSolver limits(IkConstraint[] perJoint) {
        this.limits = perJoint == null ? new IkConstraint[0] : perJoint;
        return this;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public float tolerance() {
        return tolerance;
    }

    /** Distance from the end effector to the softened target after the last solve. */
    public float residual() {
        return lastResidual;
    }

    /** Iterations actually spent by the last solve. Zero means it was already there. */
    public int iterations() {
        return lastIterations;
    }

    /**
     * Solves in place.
     *
     * @param joints      {@code 2 * jointCount} floats, joint 0 pinned as the chain root
     * @param lengths     {@code jointCount - 1} link lengths, taken from the rig
     *                    rather than measured from {@code joints} so that a
     *                    degenerate incoming pose cannot silently shrink a bone
     * @param referenceDeg world angle the first link's limit is measured against
     * @param mirror      +1, or -1 when an ancestor's negative scale flipped the
     *                    chain's handedness
     * @return iterations spent
     */
    public int solve(float[] joints, int jointCount, float[] lengths,
                     float targetX, float targetY, float referenceDeg, float mirror) {

        int links = jointCount - 1;
        if (links < 1) {
            lastResidual = 0f;
            lastIterations = 0;
            return 0;
        }
        if (dirScratch.length < links) {
            dirScratch = new float[links];
        }

        float rootX = joints[0];
        float rootY = joints[1];

        float total = 0f;
        for (int i = 0; i < links; i++) {
            total += Math.max(0f, lengths[i]);
        }

        // Soft reach limit on the target itself. Doing it here, once, is what
        // removes the out-of-reach branch entirely -- everything downstream sees a
        // target it can actually hit.
        float tx = targetX;
        float ty = targetY;
        if (total > IkMath.EPS) {
            float dx = targetX - rootX;
            float dy = targetY - rootY;
            float d = IkMath.length(dx, dy);
            float w = Math.min(reachSoftness * total, total * 0.25f);
            float capped = IkMath.softCeil(d, total - w, total);
            if (d > IkMath.EPS) {
                float s = capped / d;
                tx = rootX + dx * s;
                ty = rootY + dy * s;
            }
        }

        // Seed the per-link directions from the incoming pose. These are the
        // fallback whenever two joints coincide and a direction cannot be
        // recovered -- holding the previous direction is what keeps a collapsed
        // chain from picking an arbitrary new one and popping.
        float prevDeg = referenceDeg;
        for (int i = 0; i < links; i++) {
            float sx = joints[2 * i + 2] - joints[2 * i];
            float sy = joints[2 * i + 3] - joints[2 * i + 1];
            dirScratch[i] = IkMath.length(sx, sy) > IkMath.EPS ? IkMath.atan2Deg(sy, sx) : prevDeg;
            prevDeg = dirScratch[i];
        }

        if (isCollinearWith(joints, links, dirScratch, tx, ty, total)) {
            nudgeOffTheAxis(joints, links, lengths, dirScratch[0], mirror);
        }

        lastResidual = IkMath.length(joints[2 * links] - tx, joints[2 * links + 1] - ty);
        lastIterations = 0;

        for (int iter = 0; iter < maxIterations; iter++) {
            if (lastResidual <= tolerance) {
                break;
            }
            float before = lastResidual;

            backward(joints, links, lengths, tx, ty);
            forward(joints, links, lengths, rootX, rootY, referenceDeg, mirror);

            lastResidual = IkMath.length(joints[2 * links] - tx, joints[2 * links + 1] - ty);
            lastIterations = iter + 1;

            // Stagnation bail. A constrained chain settles at a fixed point with a
            // non-zero residual; continuing to iterate there is how jitter gets in.
            if (before - lastResidual < tolerance * 0.1f) {
                break;
            }
        }
        return lastIterations;
    }

    /** Unconstrained, unrotated convenience form. */
    public int solve(float[] joints, int jointCount, float[] lengths, float targetX, float targetY) {
        return solve(joints, jointCount, lengths, targetX, targetY, 0f, 1f);
    }

    /**
     * True when the chain is straight, the target is on the same line, and the
     * chain has to shorten to reach it.
     *
     * <p>That is FABRIK's one genuine degeneracy. Every direction the algorithm
     * can recover from the joint positions is parallel to the chain, so a
     * straight chain has no way to discover which side to fold to; both passes
     * become identities and it stays straight with a large residual. The pose is
     * ambiguous in the maths -- a whole circle of solutions is equally valid --
     * but "stay straight and miss" is not one of them, and worse, it defers the
     * whole reconfiguration to the first frame the target leaves the axis, where
     * it arrives as a pop.
     */
    private static boolean isCollinearWith(float[] joints, int links, float[] dirs, float tx, float ty, float total) {
        if (total <= IkMath.EPS) {
            return false;
        }
        float toTarget = IkMath.length(tx - joints[0], ty - joints[1]);
        if (total - toTarget <= 1e-3f * total) {
            return false; // already at full stretch: straight is the right answer
        }
        float axis = dirs[0];
        for (int i = 1; i < links; i++) {
            if (Math.abs(IkMath.deltaDeg(axis, dirs[i])) > 1f) {
                return false;
            }
        }
        if (toTarget <= IkMath.EPS) {
            return true; // target on the root, which is on the chain's line by definition
        }
        float targetDeg = IkMath.atan2Deg(ty - joints[1], tx - joints[0]);
        float off = Math.abs(IkMath.deltaDeg(axis, targetDeg));
        return off < 1f || off > 179f;
    }

    /**
     * One deterministic sideways nudge, growing along the chain so the shape it
     * suggests is a bend rather than a shear. It is tiny and the first iteration's
     * length restoration washes it out entirely; all it has to do is give the
     * solver a side. The side follows {@code mirror}, so a fighter facing the
     * other way folds the mirrored way rather than the same way.
     */
    private static void nudgeOffTheAxis(float[] joints, int links, float[] lengths, float axisDeg, float mirror) {
        float perp = axisDeg + (mirror >= 0f ? 90f : -90f);
        float px = IkMath.cosDeg(perp);
        float py = IkMath.sinDeg(perp);
        for (int i = 1; i <= links; i++) {
            float k = 0.01f * i * Math.max(0f, lengths[i - 1]);
            joints[2 * i] += px * k;
            joints[2 * i + 1] += py * k;
        }
    }

    /** Drags the end effector onto the target and pulls the chain after it, root free. */
    private void backward(float[] joints, int links, float[] lengths, float tx, float ty) {
        joints[2 * links] = tx;
        joints[2 * links + 1] = ty;
        for (int i = links - 1; i >= 0; i--) {
            float cx = joints[2 * i + 2];
            float cy = joints[2 * i + 3];
            float vx = joints[2 * i] - cx;
            float vy = joints[2 * i + 1] - cy;
            float len = IkMath.length(vx, vy);
            float deg = len > IkMath.EPS ? IkMath.atan2Deg(vy, vx) : dirScratch[i] + 180f;
            float l = Math.max(0f, lengths[i]);
            joints[2 * i] = cx + l * IkMath.cosDeg(deg);
            joints[2 * i + 1] = cy + l * IkMath.sinDeg(deg);
        }
    }

    /**
     * Pins the root back where it belongs and rebuilds the chain outward. Joint
     * limits are applied here rather than as a separate pass: the forward walk
     * already knows each link's parent direction, so limiting in place costs one
     * function call and keeps the link lengths exact.
     */
    private void forward(float[] joints, int links, float[] lengths,
                         float rootX, float rootY, float referenceDeg, float mirror) {
        joints[0] = rootX;
        joints[1] = rootY;
        float prevDeg = referenceDeg;
        for (int i = 0; i < links; i++) {
            float vx = joints[2 * i + 2] - joints[2 * i];
            float vy = joints[2 * i + 3] - joints[2 * i + 1];
            float len = IkMath.length(vx, vy);
            float deg = len > IkMath.EPS ? IkMath.atan2Deg(vy, vx) : dirScratch[i];

            IkConstraint c = i < limits.length && limits[i] != null ? limits[i] : IkConstraint.free();
            if (!c.isFree()) {
                // Measured by shortest arc, so a free joint round-trips exactly and
                // the +-180 wrap never shows up in the output.
                float local = c.apply(mirror * IkMath.deltaDeg(prevDeg, deg));
                deg = prevDeg + mirror * local;
            }

            float l = Math.max(0f, lengths[i]);
            joints[2 * i + 2] = joints[2 * i] + l * IkMath.cosDeg(deg);
            joints[2 * i + 3] = joints[2 * i + 1] + l * IkMath.sinDeg(deg);
            dirScratch[i] = deg;
            prevDeg = deg;
        }
    }
}
