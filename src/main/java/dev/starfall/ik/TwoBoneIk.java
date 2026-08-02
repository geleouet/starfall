package dev.starfall.ik;


/**
 * Analytic two-bone solver -- upper arm + forearm, thigh + shin. Law of cosines,
 * so there is no iteration and no convergence question: the pose is a closed
 * form of the target.
 *
 * <p>Closed form is not the same as continuous, though, and the textbook closed
 * form is discontinuous in three places that all matter to this project. Each is
 * handled here rather than papered over by the caller:
 *
 * <ol>
 *   <li><b>The reach boundary.</b> Clamping the target distance at
 *       {@code l1 + l2} makes the elbow angle's derivative infinite there: a
 *       target drifting outward straightens the limb slower and slower and then
 *       stops dead. Instead the distance is passed through a soft ceiling
 *       ({@link IkMath#softCeil}) so the limb eases asymptotically into
 *       extension and never actually reaches the singular straight pose. The
 *       fold limit {@code |l1 - l2|} gets the same treatment.</li>
 *   <li><b>The bend flip.</b> The elbow side is a binary choice and a target
 *       crossing the chain axis flips it, which mirrors the whole limb in one
 *       frame. Here the side is a continuous state in [-1, +1] that is *driven*
 *       toward the pole's preference through a critically damped filter, with a
 *       hysteresis deadband so noise near the axis does not start a flip at all.
 *       Crucially the blend happens in <em>angle</em> space, not by lerping the
 *       elbow position: at any intermediate side the bone lengths are still
 *       exact and the end effector still points at the target, only its distance
 *       is short. So a flip reads as the limb sweeping through near-extension
 *       over ~0.4 s, which is a pose a brush could have drawn, rather than a
 *       mirror-image pop.</li>
 *   <li><b>The target at the root.</b> {@code atan2} of a near-zero vector is
 *       arbitrary, so a target passing through the shoulder would spin the whole
 *       limb. Inside a small hold radius the aim direction fades toward the last
 *       known aim instead.</li>
 * </ol>
 *
 * <p>Angles come out in both world space (for FK and tests) and local space
 * (ready to write to {@code Bone.rotDeg}), because joint limits are authored in
 * local space and are applied here, mid-solve, not clamped afterwards.
 *
 * <p>One instance per limb: the flip filter and the pole hysteresis are per-limb
 * state, and sharing an instance across limbs would cross-contaminate them.
 */
public final class TwoBoneIk {

    /** Output of one solve. Reused across frames; this runs per limb per frame and must not allocate. */
    public static final class Result {
        /** World angle of the upper bone's +x axis. */
        public float rootWorldDeg;
        /** World angle of the lower bone's +x axis. */
        public float jointWorldDeg;
        /** Upper bone's local rotation, ready for {@code Bone.rotDeg}. */
        public float rootLocalDeg;
        /** Lower bone's local rotation, ready for {@code Bone.rotDeg}. */
        public float jointLocalDeg;
        /** End effector position after FK from the solved angles. */
        public float endX, endY;
        /** Where the bend filter currently sits: +1 elbow left of the aim, -1 right, in between mid-flip. */
        public float bendSide;
        /** Raw root-to-target distance. */
        public float targetDistance;
        /** Distance actually solved for, after the soft reach limits. */
        public float solvedDistance;
    }

    // -- tuning ---------------------------------------------------------------

    private float reachSoftness = 0.05f;
    private float foldSoftness = 0.05f;
    private float poleHysteresis = 0.15f;
    private float flipSeconds = 0.60f;
    private float holdRadius = 0.06f;
    private float flipMinOpeningDeg = 45f;
    /** Interior angle at which the limb starts resisting; 180 disables the preference entirely. */
    private float foldEaseFromDeg = 180f;
    /** Interior angle the limb asymptotically approaches however far the target goes. */
    private float foldCeilingDeg = 180f;

    private IkConstraint rootLimit = IkConstraint.free();
    private IkConstraint jointLimit = IkConstraint.free();

    // -- state ----------------------------------------------------------------

    private final Damped bend = new Damped(1f);
    private float committedSide = 1f;
    private float lastAimDeg;
    private boolean primed;

    /**
     * Fraction of the chain's total length over which the limb eases into full
     * extension. Larger is softer and costs reach accuracy near the boundary;
     * below ~0.02 the ease stops being visible and the boundary starts to read
     * as a stop.
     */
    public TwoBoneIk reachSoftness(float fraction) {
        this.reachSoftness = IkMath.clamp(fraction, 0f, 0.25f);
        return this;
    }

    /** Same, at the folded-shut end of the range. */
    public TwoBoneIk foldSoftness(float fraction) {
        this.foldSoftness = IkMath.clamp(fraction, 0f, 0.25f);
        return this;
    }

    /**
     * Deadband, as a sine of the angle between the aim and the pole, that the
     * pole must clear before the solver will even consider changing sides. Zero
     * would make a target sitting exactly on the axis chatter between sides.
     */
    public TwoBoneIk poleHysteresis(float sine) {
        this.poleHysteresis = IkMath.clamp(sine, 0f, 0.9f);
        return this;
    }

    /**
     * How long a committed bend flip takes to play out. Defaults to the top of
     * STYLE.md 7.1's settle window, because a flip is not a settle -- it moves the
     * elbow through nearly 200 degrees, and doing that any faster puts the hand
     * through full extension at a speed that starts to read as a snap even though
     * it is technically continuous.
     */
    public TwoBoneIk flipSeconds(float seconds) {
        this.flipSeconds = Math.max(0f, seconds);
        return this;
    }

    /**
     * How far open the joint must be, in degrees of interior angle, before the
     * solver will accept a new bend side from the pole at all. Below it the
     * previous commitment is held: a flip of a folded limb has to travel through
     * full extension, which is the largest and ugliest motion the solver can
     * produce, in exchange for a difference in pose that is barely visible.
     */
    public TwoBoneIk flipMinOpeningDeg(float deg) {
        this.flipMinOpeningDeg = IkMath.clamp(deg, 0f, 170f);
        return this;
    }

    /**
     * A gravity-loaded limb's reluctance to straighten, per STYLE.md 7.0's second
     * positive: "the corpus's limbs are gravity-loaded and reluctant -- elbows
     * held between 90 and 130 degrees... A limb that extends fully because a
     * coordinate permitted it reads as a linkage. A limb that declines to
     * straighten reads as a brushstroke."
     *
     * <p>It is expressed in interior angle rather than as an abstract stiffness
     * because that is the quantity the reference corpus is measured in and the
     * quantity a reviewer can check in a capture. {@code easeFromDeg} is where the
     * limb starts giving way, {@code ceilingDeg} is the angle it approaches but
     * never reaches however far out of reach the target is driven. So
     * {@code foldPreference(112, 142)} means "opens freely to 112 degrees, then
     * resists, and is never straighter than 142" -- against the 3.9 degrees of
     * bend that System 2's pass-2 capture held at maximum reach, which is a rod.
     *
     * <p>Implemented as a second {@link IkMath#softCeil} on the solved distance,
     * composed after the reach ceiling. That placement matters:
     *
     * <ul>
     *   <li>it is monotone and C1, like every other limit in this package, so it
     *       cannot introduce a pop -- and unlike a post-solve angle clamp it
     *       cannot fight the bend filter or the joint limit either;</li>
     *   <li>the bone lengths stay exact and the hand stays on the target
     *       <em>ray</em>. The limb declines to reach the last few millimetres; it
     *       does not detach from the direction it was asked for;</li>
     *   <li>it is the identity below {@code easeFromDeg}, so a limb working inside
     *       the corpus band solves exactly as it did before. That is what keeps
     *       {@code ik-gesture} unaffected by a knob authored for {@code ik-extreme}.</li>
     * </ul>
     *
     * <p>The cost is reach: the limb can no longer put its hand on a target near
     * the boundary, and misses by the difference between the two distances -- 4 px
     * at capture framing with the numbers RigIk uses. That is the price of the
     * effect and it is the right way round, because a hand 4 px short of an
     * unreachable target is invisible and a straight arm is not.
     *
     * @param easeFromDeg interior angle where resistance begins, in (0, 180]
     * @param ceilingDeg  interior angle approached asymptotically, in (easeFromDeg, 180]
     */
    public TwoBoneIk foldPreference(float easeFromDeg, float ceilingDeg) {
        float ease = IkMath.clamp(easeFromDeg, 1f, 180f);
        float ceil = IkMath.clamp(ceilingDeg, ease, 180f);
        this.foldEaseFromDeg = ease;
        this.foldCeilingDeg = ceil;
        return this;
    }

    /** Drops the fold preference: the limb straightens as far as the target asks. */
    public TwoBoneIk noFoldPreference() {
        this.foldEaseFromDeg = 180f;
        this.foldCeilingDeg = 180f;
        return this;
    }

    /** Radius, as a fraction of chain length, inside which the aim direction is held rather than trusted. */
    public TwoBoneIk holdRadius(float fraction) {
        this.holdRadius = IkMath.clamp(fraction, 0f, 0.5f);
        return this;
    }

    /** Limit on the upper bone's local rotation. */
    public TwoBoneIk rootLimit(IkConstraint c) {
        this.rootLimit = c == null ? IkConstraint.free() : c;
        return this;
    }

    /** Limit on the lower bone's local rotation -- the elbow/knee stop. */
    public TwoBoneIk jointLimit(IkConstraint c) {
        this.jointLimit = c == null ? IkConstraint.free() : c;
        return this;
    }

    /**
     * Commits a bend side without a pole and without a flip. Used at bind time so
     * the limb starts out bending the way the rig was authored, and by
     * {@link IkChain#snap()}.
     */
    public TwoBoneIk preferSide(float side) {
        this.committedSide = side >= 0f ? 1f : -1f;
        this.bend.set(committedSide);
        return this;
    }

    public float bendSide() {
        return bend.value;
    }

    /** Forgets all temporal state, so the next solve is a cold, deterministic one-shot. */
    public void reset() {
        primed = false;
        bend.set(committedSide);
    }

    /**
     * Solves for the two bone angles that put the end effector on (or as near as
     * the limits allow to) the target.
     *
     * @param rootX     world position of the upper bone's origin
     * @param l1        length of the upper bone (root to joint)
     * @param l2        length of the lower bone (joint to end effector)
     * @param poleX     world point the bend should aim toward; pass the root
     *                  itself (a zero-length pole vector) to mean "no opinion,
     *                  keep the current side"
     * @param refDeg    world angle the upper bone's local rotation is measured
     *                  against, i.e. the parent bone's world angle
     * @param mirror    +1, or -1 when an ancestor's negative scale has flipped
     *                  the chain's handedness, so that local angles still match
     *                  {@code Bone.rotDeg}
     * @param dt        seconds since the last solve; &lt;= 0 means "no temporal
     *                  smoothing", which snaps the bend side and is for setup and
     *                  tests only
     */
    public void solve(float rootX, float rootY,
                      float l1, float l2,
                      float targetX, float targetY,
                      float poleX, float poleY,
                      float refDeg, float mirror, float dt,
                      Result out) {

        l1 = Math.max(0f, l1);
        l2 = Math.max(0f, l2);
        float total = l1 + l2;

        float dx = targetX - rootX;
        float dy = targetY - rootY;
        float d = IkMath.length(dx, dy);
        out.targetDistance = d;

        if (!primed) {
            lastAimDeg = d > IkMath.EPS ? IkMath.atan2Deg(dy, dx) : refDeg;
        }

        // -- aim direction, with the near-root hold ---------------------------
        float aimDeg;
        float hold = Math.max(holdRadius * total, IkMath.EPS);
        if (d >= hold) {
            aimDeg = IkMath.atan2Deg(dy, dx);
        } else {
            // Fade from "hold the last aim" at the root to "trust the target" at
            // the hold radius. Continuous in the target at the seam because the
            // blend weight reaches 1 exactly there.
            float raw = d > IkMath.EPS ? IkMath.atan2Deg(dy, dx) : lastAimDeg;
            aimDeg = lastAimDeg + IkMath.deltaDeg(lastAimDeg, raw) * (d / hold);
        }
        lastAimDeg = aimDeg;

        // -- soft reach limits -------------------------------------------------
        float fold = Math.abs(l1 - l2);
        float span = total - fold; // width of the annulus this limb can actually reach
        float solved;
        if (span <= IkMath.EPS) {
            // A zero-length bone: the "limb" is rigid, there is no elbow to solve.
            solved = total;
        } else {
            float wHigh = Math.min(reachSoftness * total, span * 0.25f);
            float wLow = Math.min(foldSoftness * total, span * 0.25f);
            solved = IkMath.softCeil(d, total - wHigh, total);
            solved = IkMath.softFloor(solved, fold + wLow, fold);
            // The fold preference, composed after the reach ceiling rather than
            // instead of it. Both are monotone C1 soft ceilings, so the
            // composition is one too, and the reach ceiling still guarantees the
            // acos argument stays inside (-1, 1) if the preference is switched off.
            if (foldCeilingDeg < 179.999f && l1 > IkMath.EPS && l2 > IkMath.EPS) {
                float knee = interiorDistance(l1, l2, foldEaseFromDeg);
                float limit = interiorDistance(l1, l2, foldCeilingDeg);
                if (limit > knee + IkMath.EPS) {
                    solved = IkMath.softCeil(solved, knee, limit);
                }
            }
        }
        out.solvedDistance = solved;

        // -- law of cosines ----------------------------------------------------
        // Interior angle at the joint, between the two bone vectors: 180 when
        // straight, 0 when folded shut. The soft limits above guarantee the
        // argument stays strictly inside (-1, 1), so this is differentiable.
        float interiorDeg;
        if (l1 < IkMath.EPS || l2 < IkMath.EPS) {
            interiorDeg = 180f;
        } else {
            interiorDeg = IkMath.acosDeg((l1 * l1 + l2 * l2 - solved * solved) / (2f * l1 * l2));
        }

        // -- which way the elbow points ---------------------------------------
        float px = poleX - rootX;
        float py = poleY - rootY;
        float poleLen = IkMath.length(px, py);
        if (poleLen > IkMath.EPS && (interiorDeg > flipMinOpeningDeg || !primed)) {
            float ax = IkMath.cosDeg(aimDeg);
            float ay = IkMath.sinDeg(aimDeg);
            float cross = (ax * py - ay * px) / poleLen;
            if (!primed) {
                committedSide = cross >= 0f ? 1f : -1f;
            } else if (cross > poleHysteresis) {
                committedSide = 1f;
            } else if (cross < -poleHysteresis) {
                committedSide = -1f;
            }
            // Inside the deadband the pole carries no usable information about
            // which side the elbow belongs on, so the previous commitment stands.
            // This is the whole defence against a target sliding along the chain
            // axis strobing the limb between mirror images.
        }
        // Above the opening gate the commitment is left alone entirely. Mirroring
        // a limb that is folded shut is both the most expensive flip -- the only
        // continuous path between the two mirror poses runs through full
        // extension, so a limb folded against the shoulder would have to shoot out
        // to arm's length and back -- and the least meaningful one, since the two
        // poses barely differ. Deferring the decision until the limb has opened up
        // makes the flip cheap when it eventually happens.
        if (!primed) {
            bend.set(committedSide);
        } else {
            bend.step(committedSide, flipSeconds, dt);
        }
        float sigma = IkMath.clamp(bend.value, -1f, 1f);

        float jointWorldDelta = -sigma * (180f - interiorDeg);

        // Joint limit applied here, on the way through, and in local space so the
        // authored numbers match Bone.rotDeg. Because IkConstraint.apply is
        // monotone and 1-Lipschitz this cannot introduce a discontinuity, and
        // because it is the identity in the interior an inactive limit costs
        // nothing.
        float jointLocal = jointLimit.apply(mirror * jointWorldDelta);
        jointWorldDelta = mirror * jointLocal;

        // Aim the *end effector* at the target given whatever joint angle
        // survived the limit, rather than aiming the upper bone and letting the
        // hand drift off the target ray. phi is the angle from the upper bone to
        // the end effector; for an unconstrained solve it works out to exactly
        // the law-of-cosines root angle, so this costs nothing in the common case
        // and degrades gracefully when the joint is limited or mid-flip.
        float sinJ = IkMath.sinDeg(jointWorldDelta);
        float cosJ = IkMath.cosDeg(jointWorldDelta);
        float phi = IkMath.atan2Deg(l2 * sinJ, l1 + l2 * cosJ);

        float rootWorld = aimDeg - phi;
        float rootLocal = rootLimit.apply(mirror * IkMath.deltaDeg(refDeg, rootWorld));
        rootWorld = refDeg + mirror * rootLocal;
        float jointWorld = rootWorld + jointWorldDelta;

        out.rootWorldDeg = rootWorld;
        out.jointWorldDeg = jointWorld;
        out.rootLocalDeg = rootLocal;
        out.jointLocalDeg = jointLocal;
        out.bendSide = sigma;
        out.endX = rootX + l1 * IkMath.cosDeg(rootWorld) + l2 * IkMath.cosDeg(jointWorld);
        out.endY = rootY + l1 * IkMath.sinDeg(rootWorld) + l2 * IkMath.sinDeg(jointWorld);

        primed = true;
    }

    /** Root-to-effector distance that puts the joint at a given interior angle. Law of cosines, forwards. */
    private static float interiorDistance(float l1, float l2, float interiorDeg) {
        float c = IkMath.cosDeg(interiorDeg);
        return (float) Math.sqrt(Math.max(0f, l1 * l1 + l2 * l2 - 2f * l1 * l2 * c));
    }

    /** Convenience for the unrotated, unmirrored, pole-less case -- mostly tests. */
    public void solve(float rootX, float rootY, float l1, float l2,
                      float targetX, float targetY, float poleX, float poleY,
                      float dt, Result out) {
        solve(rootX, rootY, l1, l2, targetX, targetY, poleX, poleY, 0f, 1f, dt, out);
    }
}
