package dev.starfall.ik;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Binds a solver to a run of bones in a {@link Skeleton} and writes the result
 * back as local rotations.
 *
 * <p><b>The API is shaped for System 4, not for one-shot solves.</b> The
 * interaction layer's normal case is a target that has moved a few millimetres
 * since last frame because a live opponent moved, sixty times a second, for the
 * whole duration of a parry. So the chain is a persistent object that owns its
 * temporal state, and the per-frame call is:
 *
 * <pre>{@code
 *   chain.target(opponentHandX, opponentHandY);   // cheap, just stores
 *   chain.update(dt);                             // settle + solve + write + FK
 * }</pre>
 *
 * <p>Everything expensive or stateful lives inside {@link #update}: the target
 * the solver actually chases lags the requested one through a critically damped
 * filter, so a target that stops moving produces a limb that eases to rest over
 * {@link #settleSeconds(float)} instead of arriving on the frame the target did.
 * That is STYLE.md 7.1's terminal settle, and it belongs here rather than in the
 * game logic because otherwise every caller would have to reimplement it and one
 * of them would get it wrong.
 *
 * <p>Ordering contract: animation writes {@code Bone} locals, <em>then</em> each
 * chain updates, then the renderer draws. {@link #update} refreshes world
 * transforms itself both before solving (to read the animation pose) and after
 * (so the skeleton is left current for the next chain and for the renderer).
 *
 * <p>{@link #weight(float)} blends the solved rotation against whatever the
 * animation put there, per bone and by shortest arc. System 4 will want this: a
 * parry that fades IK in over the wind-up and out over the recovery is a weight
 * ramp, and blending in local rotation space keeps bone lengths exact throughout,
 * which lerping world positions would not.
 */
public final class IkChain {

    /** Which solver drives this chain. */
    public enum Kind {
        /** Analytic, exactly two bones. Deterministic bend side from a pole. Use for limbs. */
        TWO_BONE,
        /** Iterative, any length. Use for the spine, and later hair and cloth. */
        FABRIK
    }

    private final Skeleton skeleton;
    private final Kind kind;
    private final Bone[] bones;

    /**
     * Marks the end effector: either a bone whose origin is the effector (the
     * hand at the end of an arm chain) or a bare length along the last bone.
     */
    private final Bone tipBone;
    private final float tipLength;

    private final TwoBoneIk twoBone = new TwoBoneIk();
    private final TwoBoneIk.Result twoBoneResult = new TwoBoneIk.Result();
    private final FabrikSolver fabrik = new FabrikSolver();
    private final IkConstraint[] limits;

    private final float[] joints;
    private final float[] lengths;
    private final float[] worldDeg;
    private final float[] prevWorldDeg;
    private final float[] preRot;
    private boolean hasPrevWorld;
    private float maxSlewDegPerSecond = 1200f;

    private final Damped settleX = new Damped(0f);
    private final Damped settleY = new Damped(0f);
    private float settleSeconds = 0.35f;

    private float targetX, targetY;
    private float poleX, poleY;
    private boolean poleSet;
    private float weight = 1f;
    private boolean primed;

    private final Vector2 scratch = new Vector2();
    private float endX, endY;

    private IkChain(Skeleton skeleton, Kind kind, Bone[] bones, Bone tipBone, float tipLength) {
        if (bones.length < 1) {
            throw new IllegalArgumentException("An IK chain needs at least one bone");
        }
        if (kind == Kind.TWO_BONE && bones.length != 2) {
            throw new IllegalArgumentException(
                    "TwoBoneIk is exactly two bones; '" + bones[bones.length - 1].name + "' is "
                            + bones.length + " bones below '" + bones[0].name + "'. Use fabrik() for longer runs.");
        }
        this.skeleton = skeleton;
        this.kind = kind;
        this.bones = bones;
        this.tipBone = tipBone;
        this.tipLength = tipLength;
        this.limits = new IkConstraint[bones.length];
        for (int i = 0; i < limits.length; i++) {
            limits[i] = IkConstraint.free();
        }
        this.joints = new float[2 * (bones.length + 1)];
        this.lengths = new float[bones.length];
        this.worldDeg = new float[bones.length];
        this.prevWorldDeg = new float[bones.length];
        this.preRot = new float[bones.length];
        this.fabrik.limits(limits);
    }

    // -- construction ---------------------------------------------------------

    /**
     * Two-bone limb whose end effector is the origin of {@code tipBone} -- for an
     * arm that is {@code (upperArmL, forearmL, handL)}, so the hand lands on the
     * target. {@code tipBone} need not be driven by the chain; it only says where
     * the effector is.
     */
    public static IkChain twoBone(Skeleton skeleton, String upper, String lower, String tipBone) {
        Bone tip = skeleton.bone(tipBone);
        return new IkChain(skeleton, Kind.TWO_BONE, walk(skeleton, upper, lower), tip, 0f);
    }

    /** Two-bone limb whose end effector sits {@code tipLength} world units along the lower bone. */
    public static IkChain twoBone(Skeleton skeleton, String upper, String lower, float tipLength) {
        return new IkChain(skeleton, Kind.TWO_BONE, walk(skeleton, upper, lower), null, tipLength);
    }

    /** FABRIK chain from {@code first} down to {@code last} inclusive, effector at {@code tipBone}'s origin. */
    public static IkChain fabrik(Skeleton skeleton, String first, String last, String tipBone) {
        Bone tip = skeleton.bone(tipBone);
        return new IkChain(skeleton, Kind.FABRIK, walk(skeleton, first, last), tip, 0f);
    }

    /** FABRIK chain from {@code first} down to {@code last} inclusive, effector {@code tipLength} along the last bone. */
    public static IkChain fabrik(Skeleton skeleton, String first, String last, float tipLength) {
        return new IkChain(skeleton, Kind.FABRIK, walk(skeleton, first, last), null, tipLength);
    }

    /** Walks parent links from {@code last} up to {@code first}, so callers name endpoints rather than list every bone. */
    private static Bone[] walk(Skeleton skeleton, String first, String last) {
        Bone from = skeleton.bone(first);
        Bone to = skeleton.bone(last);
        List<Bone> run = new ArrayList<>();
        for (Bone b = to; b != null; b = b.parent) {
            run.add(b);
            if (b == from) {
                Collections.reverse(run);
                return run.toArray(new Bone[0]);
            }
        }
        throw new IllegalArgumentException("'" + first + "' is not an ancestor of '" + last + "'");
    }

    // -- configuration --------------------------------------------------------

    /**
     * Limit on bone {@code index}'s local rotation, in the same units as
     * {@code Bone.rotDeg}. Applied inside the solver, never as a post-clamp.
     */
    public IkChain limit(int index, IkConstraint constraint) {
        limits[index] = constraint == null ? IkConstraint.free() : constraint;
        if (kind == Kind.TWO_BONE) {
            twoBone.rootLimit(limits[0]).jointLimit(limits[1]);
        }
        return this;
    }

    /** Blend against the animation pose: 0 leaves it untouched, 1 is full IK. */
    public IkChain weight(float w) {
        this.weight = IkMath.clamp(w, 0f, 1f);
        return this;
    }

    public float weight() {
        return weight;
    }

    /** Terminal settle time. STYLE.md 7.1 wants 0.3-0.6 s; 0 disables the lag entirely. */
    public IkChain settleSeconds(float seconds) {
        this.settleSeconds = Math.max(0f, seconds);
        return this;
    }

    public float settleSeconds() {
        return settleSeconds;
    }

    /**
     * Ceiling on how fast any bone in the chain may turn, as a net of last
     * resort against the two configurations no solver can be continuous through
     * -- a target passing exactly through the chain root, and a limit-saturated
     * FABRIK chain reaching far outside its workspace, where the pose is
     * bistable. STYLE.md 7.2 asks for exactly this ("cap per-step correction"),
     * and it turns an unavoidable reconfiguration into a fast sweep of a few
     * frames rather than a single-frame mirror. It is a soft ceiling, not a
     * clamp, so ordinary fast motion is not chopped. Zero disables it.
     */
    public IkChain maxSlewDegPerSecond(float degPerSecond) {
        this.maxSlewDegPerSecond = Math.max(0f, degPerSecond);
        return this;
    }

    public float maxSlewDegPerSecond() {
        return maxSlewDegPerSecond;
    }

    /** World target for the end effector. Cheap; call it every frame. */
    public IkChain target(float x, float y) {
        this.targetX = x;
        this.targetY = y;
        return this;
    }

    /**
     * World point the bend should aim toward -- for an arm, roughly where the
     * elbow ought to sit. Two-bone chains only.
     *
     * <p>Left unset, the chain uses the current pose's own joint position as its
     * pole, which reads as "keep bending the way the animation already bends".
     * That is a better default than a fixed world direction because it never
     * fights the authored pose.
     */
    public IkChain pole(float x, float y) {
        this.poleX = x;
        this.poleY = y;
        this.poleSet = true;
        return this;
    }

    /** Drops back to the pose-derived pole. */
    public IkChain clearPole() {
        this.poleSet = false;
        return this;
    }

    public TwoBoneIk twoBoneSolver() {
        return twoBone;
    }

    public FabrikSolver fabrikSolver() {
        return fabrik;
    }

    public Kind kind() {
        return kind;
    }

    public int boneCount() {
        return bones.length;
    }

    public Bone bone(int index) {
        return bones[index];
    }

    // -- per-frame ------------------------------------------------------------

    /**
     * Solves and writes local rotations. Leaves the skeleton's world transforms
     * current.
     *
     * <p>{@code dt <= 0} means "no temporal smoothing": the settle filter and the
     * bend flip both teleport. That is for scene setup and tests, not for frames.
     */
    public void update(float dt) {
        skeleton.updateWorldTransforms();
        measure();

        if (!primed || dt <= 0f) {
            settleX.set(targetX);
            settleY.set(targetY);
        } else {
            settleX.step(targetX, settleSeconds, dt);
            settleY.step(targetY, settleSeconds, dt);
        }

        float refDeg = bones[0].parent == null ? 0f : skeleton.worldRotationDeg(bones[0].parent.index);
        float mirror = mirrorSignAbove(bones[0]);

        if (kind == Kind.TWO_BONE) {
            solveTwoBone(refDeg, mirror, primed ? dt : 0f);
        } else {
            solveFabrik(refDeg, mirror);
        }

        slew(dt);
        writeBack(refDeg, mirror);
        skeleton.updateWorldTransforms();
        computeEndEffector();
        primed = true;
    }

    /**
     * Caps how fast any bone's world angle may change, as a net of last resort.
     *
     * <p>Both solvers are continuous everywhere they can be, but two
     * configurations are genuinely ambiguous and no solver is continuous through
     * them: a target passing exactly through the chain root, and a
     * limit-saturated FABRIK chain reaching for something far outside its
     * workspace, where the pose can be bistable. STYLE.md 7.2's instruction for
     * exactly this situation is to "cap per-step correction", so that is what
     * this does -- an unavoidable reconfiguration becomes a fast sweep of a few
     * frames instead of a single-frame mirror.
     *
     * <p>The cap is itself a soft ceiling, not a clamp, so a pose that is merely
     * moving quickly is not chopped at a hard rate and does not acquire a
     * velocity step of its own. At the default 1200 deg/s nothing in normal
     * tracking comes close to touching it.
     */
    private void slew(float dt) {
        if (hasPrevWorld && dt > 0f && maxSlewDegPerSecond > 0f) {
            float allowed = maxSlewDegPerSecond * dt;
            for (int i = 0; i < bones.length; i++) {
                float delta = IkMath.deltaDeg(prevWorldDeg[i], worldDeg[i]);
                float mag = Math.abs(delta);
                if (mag > 0.7f * allowed) {
                    float limited = IkMath.softCeil(mag, 0.7f * allowed, allowed);
                    worldDeg[i] = prevWorldDeg[i] + Math.signum(delta) * limited;
                }
            }
        }
        System.arraycopy(worldDeg, 0, prevWorldDeg, 0, bones.length);
        hasPrevWorld = true;
    }

    /**
     * Teleports the chain onto its current target: no settle lag, no in-flight
     * bend flip. For scene setup, respawns and camera cuts -- anything where
     * there is no previous frame to be continuous with.
     */
    public void snap() {
        settleX.set(targetX);
        settleY.set(targetY);
        twoBone.reset();
        primed = false;
        hasPrevWorld = false;
        update(0f);
    }

    /** End effector world position after the last {@link #update}. */
    public Vector2 endEffector(Vector2 out) {
        return out.set(endX, endY);
    }

    /** Distance from the end effector to the requested (not the settled) target. */
    public float residual() {
        return IkMath.length(endX - targetX, endY - targetY);
    }

    /** +1 or -1 when settled, in between during a flip. Two-bone chains only. */
    public float bendSide() {
        return twoBone.bendSide();
    }

    // -- internals ------------------------------------------------------------

    /**
     * Reads joint world positions and link lengths off the live pose. Lengths are
     * measured every frame rather than cached from bind, so a chain stays correct
     * if animation ever translates a bone or scales a limb.
     */
    private void measure() {
        for (int i = 0; i < bones.length; i++) {
            skeleton.worldPosition(bones[i].index, scratch);
            joints[2 * i] = scratch.x;
            joints[2 * i + 1] = scratch.y;
        }
        int n = bones.length;
        if (tipBone != null) {
            skeleton.worldPosition(tipBone.index, scratch);
            joints[2 * n] = scratch.x;
            joints[2 * n + 1] = scratch.y;
        } else {
            float deg = skeleton.worldRotationDeg(bones[n - 1].index);
            joints[2 * n] = joints[2 * n - 2] + tipLength * IkMath.cosDeg(deg);
            joints[2 * n + 1] = joints[2 * n - 1] + tipLength * IkMath.sinDeg(deg);
        }
        for (int i = 0; i < n; i++) {
            lengths[i] = IkMath.length(joints[2 * i + 2] - joints[2 * i], joints[2 * i + 3] - joints[2 * i + 1]);
        }
    }

    private void solveTwoBone(float refDeg, float mirror, float dt) {
        float px = poleX;
        float py = poleY;
        if (!poleSet) {
            // The joint's own current position: "keep bending the way you already
            // bend". Read before the solve, so it is last frame's answer and the
            // hysteresis has something stable to hold on to.
            px = joints[2];
            py = joints[3];
        }
        twoBone.solve(joints[0], joints[1], lengths[0], lengths[1],
                settleX.value, settleY.value, px, py, refDeg, mirror, dt, twoBoneResult);
        worldDeg[0] = twoBoneResult.rootWorldDeg;
        worldDeg[1] = twoBoneResult.jointWorldDeg;
    }

    private void solveFabrik(float refDeg, float mirror) {
        fabrik.solve(joints, bones.length + 1, lengths, settleX.value, settleY.value, refDeg, mirror);
        for (int i = 0; i < bones.length; i++) {
            float dx = joints[2 * i + 2] - joints[2 * i];
            float dy = joints[2 * i + 3] - joints[2 * i + 1];
            // A zero-length link carries no direction; keep the pose's own so a
            // degenerate bone cannot swing its descendants somewhere arbitrary.
            worldDeg[i] = IkMath.length(dx, dy) > IkMath.EPS
                    ? IkMath.atan2Deg(dy, dx)
                    : skeleton.worldRotationDeg(bones[i].index);
        }
    }

    /**
     * Turns solved world angles into {@code Bone.rotDeg}.
     *
     * <p>{@link Skeleton} exposes world rotation but not the world affine, so the
     * inverse -- "what local rotation puts this bone's +x axis at this world
     * angle" -- has to be reconstructed. It is
     * {@code world = parentWorld + det(parent) * rotDeg (+180 if scaleX < 0)}:
     * the determinant sign flips because a mirrored ancestor reverses which way
     * positive rotation turns, and that is exactly the case the rig hits every
     * time a fighter faces left via {@code root.scaleX = -1}. Getting it wrong
     * would make IK work perfectly for one facing and invert for the other.
     */
    private void writeBack(float refDeg, float mirror) {
        float sign = mirror;
        for (int i = 0; i < bones.length; i++) {
            Bone b = bones[i];
            float flip = b.scaleX < 0f ? 180f : 0f;
            // Measured against the *solved* parent, not the blended one, so that
            // partial weight is a straight lerp in local rotation space rather
            // than something whose meaning depends on where in the chain a bone
            // sits.
            float parentWorld = i == 0 ? refDeg : worldDeg[i - 1];
            float solvedLocal = sign * IkMath.deltaDeg(parentWorld + flip, worldDeg[i]);

            // Shortest-arc blend against the animation pose, so a weight ramp
            // never takes the long way round.
            float animLocal = preRot[i] = b.rotDeg;
            b.rotDeg = animLocal + IkMath.deltaDeg(animLocal, solvedLocal) * weight;

            sign *= b.scaleX * b.scaleY < 0f ? -1f : 1f;
        }
    }

    /** Rotation the animation had written for bone {@code index} before the last solve overwrote it. */
    public float animationRotDeg(int index) {
        return preRot[index];
    }

    private void computeEndEffector() {
        int n = bones.length;
        skeleton.worldPosition(bones[n - 1].index, scratch);
        if (tipBone != null) {
            // Re-read rather than trusting lengths[n-1]: the tip bone may carry its
            // own local offset and rotation, and the effector is wherever it
            // actually ends up.
            float baseX = scratch.x;
            float baseY = scratch.y;
            skeleton.worldPosition(tipBone.index, scratch);
            endX = scratch.x;
            endY = scratch.y;
            if (IkMath.length(endX - baseX, endY - baseY) < IkMath.EPS && lengths[n - 1] > IkMath.EPS) {
                float deg = skeleton.worldRotationDeg(bones[n - 1].index);
                endX = baseX + lengths[n - 1] * IkMath.cosDeg(deg);
                endY = baseY + lengths[n - 1] * IkMath.sinDeg(deg);
            }
        } else {
            float deg = skeleton.worldRotationDeg(bones[n - 1].index);
            endX = scratch.x + tipLength * IkMath.cosDeg(deg);
            endY = scratch.y + tipLength * IkMath.sinDeg(deg);
        }
    }

    /** Product of the determinant signs of every ancestor's local transform. */
    private static float mirrorSignAbove(Bone first) {
        float sign = 1f;
        for (Bone b = first.parent; b != null; b = b.parent) {
            if (b.scaleX * b.scaleY < 0f) {
                sign = -sign;
            }
        }
        return sign;
    }
}
