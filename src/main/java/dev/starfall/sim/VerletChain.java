package dev.starfall.sim;

/**
 * One position-based Verlet chain: a run of particles, pinned at particle 0,
 * held together by distance constraints and pulled toward an authored rest shape
 * by bend constraints. This is the whole solver -- hair strands and cloth
 * segments are both nothing but a {@code VerletChain} plus a policy for where
 * the anchor is and what is done with the result.
 *
 * <h2>Why position-based rather than a mass-spring</h2>
 *
 * <p>STYLE.md 7.2 bans two things that a spring gives you for free: visible
 * oscillation ("a spring that visibly oscillates twice and stops reads as a
 * machine") and snapping. A stiff spring needs a small timestep and still rings;
 * softening it lets the strand stretch, and a stretched strand recovering is
 * exactly the "pop a strand straight" the same section forbids. Position-based
 * distance projection is unconditionally inextensible at any stiffness, so the
 * only dynamics left in the system are the ones that were deliberately put
 * there: the drag, the bend stiffness and the anchor's own motion.
 *
 * <h2>The step</h2>
 *
 * <p>Per fixed substep:
 * <ol>
 *   <li>Verlet integrate, with a soft ceiling on the carried velocity --
 *       STYLE.md 7.2's "cap per-step correction". The <em>displacement</em>
 *       carries the velocity, and drag multiplies it by {@code exp(-dt/dragTau)}
 *       -- an exponential rather than a per-step constant so the decay is the
 *       same however the substep is chosen. {@code dragTau} is the knob that
 *       makes motion ink-trail-like rather than snappy.</li>
 *   <li>Pin particle 0 to the anchor, keeping its previous position, so a moving
 *       anchor genuinely drags the chain instead of teleporting it.</li>
 *   <li>{@code iterations} rounds of bend, then the collider, then distance --
 *       in that order, so the invariant that holds when the substep ends is
 *       inextensibility. The bend is a first-order relaxation toward the rest
 *       shape with an authored time constant, and it moves the previous position
 *       along with the current one so that it injects no velocity: it relaxes,
 *       it does not ring.</li>
 * </ol>
 *
 * <h2>The rest shape</h2>
 *
 * <p>A chain with only distance constraints hangs straight down, which is not
 * what any reference image shows: hair lies <em>back</em> along the skull and
 * cloth hangs along the garment it belongs to. So each joint carries a rest
 * bend, and segment 0 carries a rest direction in <em>world</em> terms that the
 * owner refreshes every frame from the bone it is attached to. Turning the head
 * therefore rotates segment 0's rest direction, which the bend constraint chases
 * at its own stiffness, which propagates outward one joint per round -- so lag
 * increases down the strand for free and by construction. That is STYLE.md 4's
 * "hair leads and lags the head" and 7.0's "nothing may arrive at the same time",
 * and neither needed a special case.
 */
public final class VerletChain implements VerletSolver.Stepped {

    private final int count;

    private final float[] x;
    private final float[] y;
    private final float[] prevX;
    private final float[] prevY;
    /** 0 for the pinned root, 1 elsewhere. Kept as an array so a future pinned mid-point is one assignment. */
    private final float[] invMass;

    /** Segment rest lengths, {@code count - 1} of them. */
    private final float[] restLen;
    /**
     * Rest bend at joint {@code i}: the angle from segment {@code i-1}'s
     * direction to segment {@code i}'s, in degrees. Index 0 is unused -- segment
     * 0's direction is {@link #rootDirDeg}, which lives in world space.
     */
    private final float[] bendDeg;
    /**
     * Per-joint shape-recovery time constant, in seconds: how long this joint
     * takes to relax back onto its rest bend. Authored as a time rather than as
     * a per-step fraction, because a per-step fraction silently means something
     * different at a different substep rate -- the first version of this file
     * took a fraction and, at 240 Hz, 0.30 meant a recovery time constant of
     * 12 milliseconds, i.e. every strand in the bundle was rigid and the
     * measured hair lag was zero frames.
     */
    private final float[] bendTau;

    private float anchorX;
    private float anchorY;
    private float rootDirDeg;

    private float gravityX;
    private float gravityY = -1.6f;
    private float windX;
    private float windY;

    private float dragTau = 0.25f;
    private int iterations = 8;

    private boolean colliderOn;
    private float colliderX;
    private float colliderY;
    private float colliderR;

    /** {@link #bendTau} converted to a per-round fraction; see {@link #refreshPerRound}. Hoisted: this runs 240 times a second per chain and must not allocate. */
    private final float[] perRound;
    private boolean perRoundDirty = true;

    public VerletChain(int particleCount) {
        if (particleCount < 2) {
            throw new IllegalArgumentException("A chain needs at least two particles, got " + particleCount);
        }
        this.count = particleCount;
        this.x = new float[count];
        this.y = new float[count];
        this.prevX = new float[count];
        this.prevY = new float[count];
        this.invMass = new float[count];
        this.restLen = new float[count - 1];
        this.bendDeg = new float[count - 1];
        this.bendTau = new float[count - 1];
        this.perRound = new float[count - 1];
        for (int i = 1; i < count; i++) {
            invMass[i] = 1f;
        }
    }

    // -- authoring ------------------------------------------------------------

    public int particleCount() {
        return count;
    }

    /**
     * @param bendTauSeconds how long this joint takes to relax back onto its rest
     *                       bend. Small is stiff and early; large is loose and
     *                       late. This and {@link #dragTau} together are the
     *                       entire tuning surface for STYLE.md 7.1's lag band.
     */
    public VerletChain segment(int index, float restLength, float restBendDeg, float bendTauSeconds) {
        restLen[index] = restLength;
        bendDeg[index] = restBendDeg;
        bendTau[index] = Math.max(1e-4f, bendTauSeconds);
        perRoundDirty = true;
        return this;
    }

    public float restLength(int index) {
        return restLen[index];
    }

    public float totalRestLength() {
        float sum = 0f;
        for (float len : restLen) {
            sum += len;
        }
        return sum;
    }

    public VerletChain gravity(float gx, float gy) {
        this.gravityX = gx;
        this.gravityY = gy;
        return this;
    }

    /** External acceleration, refreshed by the owner every frame. Not a force: this is already divided by mass. */
    public VerletChain wind(float wx, float wy) {
        this.windX = wx;
        this.windY = wy;
        return this;
    }

    /**
     * 1/e time of the chain's own velocity, in seconds. Larger is slower, later
     * and more ink-like. Below about 0.08 s the chain starts to read as snappy,
     * which STYLE.md 10 fails on sight.
     */
    public VerletChain dragTau(float seconds) {
        this.dragTau = Math.max(1e-3f, seconds);
        return this;
    }

    public float dragTau() {
        return dragTau;
    }

    public VerletChain iterations(int n) {
        this.iterations = Math.max(1, n);
        perRoundDirty = true;
        return this;
    }

    /** A circle particles are pushed out of -- the skull, so hair cannot sink through the head. */
    public VerletChain collider(float cx, float cy, float radius) {
        this.colliderOn = radius > 0f;
        this.colliderX = cx;
        this.colliderY = cy;
        this.colliderR = radius;
        return this;
    }

    // -- per-frame inputs ------------------------------------------------------

    /** Where particle 0 is pinned this frame. */
    public VerletChain anchor(float ax, float ay) {
        this.anchorX = ax;
        this.anchorY = ay;
        return this;
    }

    /** World direction segment 0's rest shape points along this frame. */
    public VerletChain rootDirDeg(float deg) {
        this.rootDirDeg = deg;
        return this;
    }

    // -- state -----------------------------------------------------------------

    public float x(int i) {
        return x[i];
    }

    public float y(int i) {
        return y[i];
    }

    /** World angle of segment {@code i}, or the previous segment's if it is degenerate. */
    public float segmentDeg(int i) {
        float dx = x[i + 1] - x[i];
        float dy = y[i + 1] - y[i];
        if (SimMath.length(dx, dy) < SimMath.EPS) {
            return i > 0 ? segmentDeg(i - 1) : rootDirDeg;
        }
        return SimMath.atan2Deg(dy, dx);
    }

    /** Per-substep displacement of a particle, i.e. its velocity times the substep. */
    public float speedOf(int i) {
        return SimMath.length(x[i] - prevX[i], y[i] - prevY[i]);
    }

    /**
     * Lays the chain out along its own rest shape from the current anchor and
     * kills all velocity. This is the {@code snap()} of the IK package: for scene
     * setup and respawns, where there is no previous frame to be continuous with.
     */
    public void reset() {
        x[0] = anchorX;
        y[0] = anchorY;
        float deg = rootDirDeg;
        for (int i = 0; i < count - 1; i++) {
            if (i > 0) {
                deg += bendDeg[i];
            }
            x[i + 1] = x[i] + restLen[i] * SimMath.cosDeg(deg);
            y[i + 1] = y[i] + restLen[i] * SimMath.sinDeg(deg);
        }
        System.arraycopy(x, 0, prevX, 0, count);
        System.arraycopy(y, 0, prevY, 0, count);
    }

    // -- the step ---------------------------------------------------------------

    /** One fixed substep. Callers must not vary {@code dt}: see {@link VerletSolver}. */
    @Override
    public void substep(float dt) {
        integrate(dt);

        prevX[0] = x[0];
        prevY[0] = y[0];
        x[0] = anchorX;
        y[0] = anchorY;

        // Bend, then collide, then distance -- and the order is the whole
        // difference between a chain that is inextensible and one that is not.
        // Every pass here moves particles, so whichever runs last is the one
        // whose invariant actually holds when the substep ends. Distance runs
        // last, every round, so a segment is never left stretched; the collider
        // and the rest shape get their say and then the chain is made rigid
        // again. The first version of this ran the collider and a per-substep
        // motion clamp *after* the loop and measured 59% segment stretch under
        // the knockback gust -- a strand visibly rubber-banding, which is
        // exactly what STYLE.md 7.2's "must not visibly pop a strand straight"
        // is a complaint about.
        refreshPerRound(dt);
        for (int it = 0; it < iterations; it++) {
            bendPass();
            if (colliderOn) {
                collidePass();
            }
            distancePass();
        }
    }

    /**
     * Ceiling on how far a particle may be carried by its own velocity in one
     * substep -- STYLE.md 7.2's "cap per-step correction", applied where it
     * belongs, which is to the velocity rather than to the position.
     *
     * <p>A position clamp after the constraint solve would be a fourth constraint
     * that runs last and therefore wins, and it would win by stretching a
     * segment. Capping the velocity instead leaves the constraint solve free to
     * be exactly rigid, and it is soft rather than hard so a fast strand is not
     * chopped at a rate and does not acquire a velocity step of its own.
     *
     * <p>6 world units/second is about 1180 px/s at capture framing -- far
     * outside anything the scenes produce. It exists for the pathological frame.
     */
    private static final float MAX_SPEED = 6f;

    private void integrate(float dt) {
        float damp = (float) Math.exp(-dt / dragTau);
        float ax = (gravityX + windX) * dt * dt;
        float ay = (gravityY + windY) * dt * dt;
        float limit = MAX_SPEED * dt;
        float knee = 0.7f * limit;
        for (int i = 1; i < count; i++) {
            float vx = (x[i] - prevX[i]) * damp;
            float vy = (y[i] - prevY[i]) * damp;
            float m = SimMath.length(vx, vy);
            if (m > knee) {
                float s = SimMath.softCeil(m, knee, limit) / m;
                vx *= s;
                vy *= s;
            }
            prevX[i] = x[i];
            prevY[i] = y[i];
            x[i] += vx + ax;
            y[i] += vy + ay;
        }
    }

    /**
     * Pulls each particle toward where the rest shape says it should be, given
     * where its parent joint actually is. Only the child moves: pushing back on
     * the parent would let a stiff tip re-aim the root, and the root's direction
     * is the head's business, not the hair's.
     */
    /**
     * Per-round bend fraction, chosen so the compounded recovery over a whole
     * substep is exactly {@code exp(-dt/bendTau)}. Two things fall out of doing
     * it in closed form: the tuning is independent of the iteration count, so
     * raising iterations to satisfy 7.2's "use enough relaxation iterations"
     * cannot silently stiffen the whole scene; and it is independent of the
     * substep, so the same numbers mean the same motion on any clock.
     */
    private void refreshPerRound(float dt) {
        if (!perRoundDirty && dt == perRoundDt) {
            return;
        }
        for (int i = 0; i < count - 1; i++) {
            perRound[i] = 1f - (float) Math.exp(-dt / (iterations * bendTau[i]));
        }
        perRoundDt = dt;
        perRoundDirty = false;
    }

    private float perRoundDt = -1f;

    private void bendPass() {
        float deg = rootDirDeg;
        for (int i = 0; i < count - 1; i++) {
            if (i > 0) {
                deg = segmentDeg(i - 1) + bendDeg[i];
            }
            float k = perRound[i];
            if (k <= 0f) {
                continue;
            }
            float tx = x[i] + restLen[i] * SimMath.cosDeg(deg);
            float ty = y[i] + restLen[i] * SimMath.sinDeg(deg);
            float dx = (tx - x[i + 1]) * k;
            float dy = (ty - y[i + 1]) * k;
            x[i + 1] += dx;
            y[i + 1] += dy;
            // The previous position moves with it, so the correction adds no
            // velocity. This one line is the difference between a strand that
            // relaxes and a strand that rings.
            //
            // In Verlet the velocity *is* the gap between x and prev, so a
            // positional correction that leaves prev alone is indistinguishable
            // from an impulse -- the bend constraint becomes a spring, the drag
            // becomes its damping ratio, and the chain acquires a second-order
            // response with a natural period of its own. Measured on the first
            // version: the back hem swung 0.087 world units *forward* while the
            // body moved 0.060 backward, i.e. it was oscillating about its rest
            // shape rather than trailing toward it, and STYLE.md 7.2 bans
            // exactly that ("a spring that visibly oscillates twice and stops
            // reads as a machine").
            //
            // Moving prev too makes the bend a pure first-order relaxation:
            // monotone, no overshoot, with a time constant that is exactly
            // bendTau. All the springiness left in the system is inertia against
            // drag, which is the springiness a hair actually has.
            prevX[i + 1] += dx;
            prevY[i + 1] += dy;
        }
    }

    /**
     * How much of each Follow-The-Leader correction is fed back as an impulse on
     * the particle above. Han and Harada's Dynamic FTL: the correction the sweep
     * applies to a child is free energy, and returning it to the parent's
     * velocity is what stops an inextensible chain behaving like a whip.
     * 0 leaves the chain stiff and lively; 1 removes the energy entirely and
     * reads dead. 0.72 is where a strand still swings but never cracks.
     */
    private static final float FTL_FEEDBACK = 0.72f;

    /**
     * Follow-The-Leader: sweep from the root outward putting each child exactly
     * one rest length from its parent, with only the child moving.
     *
     * <p>This replaced symmetric Gauss-Seidel projection and the reason is
     * measured. Symmetric projection splits each correction between the two
     * particles, so fixing joint {@code i+1} breaks joint {@code i}, and a chain
     * of thirteen particles needs on the order of thirteen sweeps to converge --
     * at the eight this runs, the knockback gust left segments 6.5% off their
     * rest length. A 6.5% error that recovers over the following frames is
     * exactly the rubber-banding STYLE.md 7.2's "must not visibly pop a strand
     * straight" forbids, and it is not fixable by adding iterations without
     * paying for them on every strand on every substep.
     *
     * <p>An outward sweep is instead <em>exact in one pass</em>: after it, every
     * segment is at its rest length to float precision, always, at any speed and
     * under any perturbation. That is also the physically honest model for hair,
     * which is pinned at the scalp and genuinely cannot pull its own root
     * around. The energy this adds is returned by {@link #FTL_FEEDBACK} and then
     * removed by the drag.
     */
    private void distancePass() {
        for (int i = 0; i < count - 1; i++) {
            float dx = x[i + 1] - x[i];
            float dy = y[i + 1] - y[i];
            float d = SimMath.length(dx, dy);
            float tx;
            float ty;
            if (d < SimMath.EPS) {
                // Degenerate: fall back to the rest direction rather than
                // dividing by zero. A collapsed segment has no direction.
                float deg = i > 0 ? segmentDeg(i - 1) + bendDeg[i] : rootDirDeg;
                tx = x[i] + restLen[i] * SimMath.cosDeg(deg);
                ty = y[i] + restLen[i] * SimMath.sinDeg(deg);
            } else {
                float s = restLen[i] / d;
                tx = x[i] + dx * s;
                ty = y[i] + dy * s;
            }
            float cx = tx - x[i + 1];
            float cy = ty - y[i + 1];
            x[i + 1] = tx;
            y[i + 1] = ty;
            if (i > 0) {
                prevX[i] += FTL_FEEDBACK * cx;
                prevY[i] += FTL_FEEDBACK * cy;
            }
        }
    }

    private void collidePass() {
        for (int i = 1; i < count; i++) {
            float dx = x[i] - colliderX;
            float dy = y[i] - colliderY;
            float d = SimMath.length(dx, dy);
            if (d >= colliderR || d < SimMath.EPS) {
                continue;
            }
            float push = (colliderR - d) / d;
            x[i] += dx * push;
            y[i] += dy * push;
        }
    }

}
