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
 * animation put there, per bone, along the arc that is continuous with the
 * previous frame's (see {@link #writeBack}). System 4 will want this: a
 * parry that fades IK in over the wind-up and out over the recovery is a weight
 * ramp, and blending in local rotation space keeps bone lengths exact throughout,
 * which lerping world positions would not.
 *
 * <p>Note on joint limits: {@link IkConstraint} ranges on a {@link Kind#TWO_BONE}
 * chain are the bone's own {@code rotDeg}, as {@link IkConstraint} says. On a
 * {@link Kind#FABRIK} chain they are not -- {@link FabrikSolver} applies them to
 * <em>link</em> directions relative to the previous link, and for a bone whose
 * axis does not point straight at its child those two differ by a constant (see
 * {@link #boneWorldDeg}). A trunk chain's limits are therefore authored around
 * the link angles, not around the numbers sitting in the rig.
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
    /** Per bone, the angle from the link direction to the bone's own +x axis. See {@link #boneWorldDeg}. */
    private final float[] linkOffsetDeg;
    /** Per bone, last frame's animation-to-solved rotation offset. See {@link #writeBack}. */
    private final float[] blendDeltaDeg;
    /** Per bone, the arrival lag of {@link #boneLagSeconds}. Null entries are "no lag", which is the default. */
    private final Damped[] boneLag;
    private final float[] boneLagSeconds;
    private final float[] lagWorldTarget;
    private boolean hasLagState;
    /** Last solved joint direction, in the chain parent's frame. See {@link #solveTwoBone}. */
    private float lastPoleLocalDeg;
    private float lastPoleLen;
    private boolean hasLastPole;
    private boolean hasPrevBlend;
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
        this.linkOffsetDeg = new float[bones.length];
        this.blendDeltaDeg = new float[bones.length];
        this.boneLag = new Damped[bones.length];
        this.boneLagSeconds = new float[bones.length];
        this.lagWorldTarget = new float[bones.length];
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
     * Extra arrival lag on one bone: it reaches the angle the solve asked for a
     * little after the bones above it do.
     *
     * <p>{@link #settleSeconds(float)} lags the whole chain by lagging its target,
     * so every bone in it arrives on the same frame. STYLE.md 7.0's third positive
     * bans exactly that -- "nothing may arrive at the same time... the wrist should
     * arrive after the elbow" -- and 10's last row fails a pass on sight of
     * "everything peaking on the same frame". This is the knob that separates
     * them: give bone 1 a little more lag than bone 0 and the elbow leads the hand
     * down the chain, which is the cheapest overlapping action available and costs
     * one critically damped scalar per bone.
     *
     * <p>It filters the <em>solved world angle</em>, upstream of both the slew
     * ceiling and the weight blend -- see {@link #applyBoneLag} for why that
     * placement is the one that works. It is critically damped like everything
     * else here, so it can lag but never overshoot, and a bone whose target has
     * stopped still comes to rest: the chain remains exact in the steady state and
     * is only late in the transient, which is precisely the effect wanted.
     *
     * <p>Zero, the default, removes the filter entirely rather than running it at
     * a tiny time constant, so an unlagged chain is bit-identical to one built
     * before this existed.
     */
    public IkChain boneLagSeconds(int index, float seconds) {
        float s = Math.max(0f, seconds);
        boneLagSeconds[index] = s;
        boneLag[index] = s <= 0f ? null : new Damped(0f);
        hasLagState = false;
        return this;
    }

    public float boneLagSeconds(int index) {
        return boneLagSeconds[index];
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

    /** The requested (not the settled) target, so a driven link upstream can follow the same point. */
    public float targetX() {
        return targetX;
    }

    public float targetY() {
        return targetY;
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

    /** Where the pole currently sits, for debug overlays and rehearsal probes. */
    public float poleX() {
        return poleX;
    }

    public float poleY() {
        return poleY;
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

        // Lag first, then slew: the ceiling has to be the last word on how fast a
        // bone may turn, or a filter catching up after a teleport can carry its
        // own velocity straight through it.
        applyBoneLag(primed ? dt : 0f);
        slew(dt);
        writeBack(refDeg, mirror);
        hasPrevBlend = true;
        skeleton.updateWorldTransforms();
        computeEndEffector();
        primed = true;
    }

    /**
     * Holds each lagged bone's world angle behind the solved one.
     *
     * <p>It runs on the solved <em>world</em> angle, before the slew ceiling and
     * before the write-back, and both placements are deliberate. Before the slew,
     * because the ceiling is the last word on angular rate and a filter that had
     * built up velocity chasing a teleport would otherwise carry it straight
     * through -- measured at 27 deg/frame on {@code forearmL} against a ceiling of
     * 20 when this ran after. On the world angle rather than the local one,
     * because "the wrist arrives after the elbow" is a statement about where the
     * forearm is pointing, not about a number relative to a parent that is itself
     * still moving.
     *
     * <p>The filter target is accumulated by shortest arc rather than assigned, so
     * a chain revolving past +-180 degrees does not make the filter chase the long
     * way round once per turn.
     */
    private void applyBoneLag(float dt) {
        for (int i = 0; i < bones.length; i++) {
            Damped lag = boneLag[i];
            if (lag == null) {
                continue;
            }
            if (dt <= 0f || !hasLagState) {
                lagWorldTarget[i] = worldDeg[i];
                lag.set(worldDeg[i]);
            } else {
                lagWorldTarget[i] += IkMath.deltaDeg(lagWorldTarget[i], worldDeg[i]);
                lag.step(lagWorldTarget[i], boneLagSeconds[i], dt);
            }
            worldDeg[i] = lag.value;
        }
        hasLagState = true;
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
        hasPrevBlend = false;
        hasLagState = false;
        hasLastPole = false;
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
     * Reads joint world positions, link lengths and link offsets off the live
     * pose. Lengths are measured every frame rather than cached from bind, so a
     * chain stays correct if animation ever translates a bone or scales a limb.
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
            float dx = joints[2 * i + 2] - joints[2 * i];
            float dy = joints[2 * i + 3] - joints[2 * i + 1];
            lengths[i] = IkMath.length(dx, dy);
            // How far the bone's +x axis is turned away from the line to the next
            // joint, in this pose (see boneWorldDeg). Zero for a limb,
            // ninety for a trunk bone that stacks upward. Held from the previous
            // frame when the link is degenerate, since a zero-length link has no
            // direction to measure against.
            if (kind == Kind.FABRIK && lengths[i] > IkMath.EPS) {
                linkOffsetDeg[i] = IkMath.deltaDeg(IkMath.atan2Deg(dy, dx),
                        skeleton.worldRotationDeg(bones[i].index));
            }
        }
    }

    /**
     * The bone's own world axis angle implied by the solved link direction.
     *
     * <p>Both solvers work purely in <em>link</em> space: they know joint
     * positions and link lengths, so every angle they return is the direction
     * from one joint to the next. {@code Bone.rotDeg} is not that. It is the
     * angle of the bone's local +x axis, and the two coincide only for a bone
     * whose child sits straight out along that axis -- which is true of every
     * limb in the rig and false of every trunk bone, because
     * {@code SamuraiRig}'s hips/spine/chest stack via a +y offset with
     * {@code rotDeg} near zero. Writing a link direction into {@code rotDeg}
     * there rotates the bone by the ninety degrees between the two conventions:
     * a FABRIK chain on {@code hips -> chest} asked to hold its own current pose
     * used to answer {@code hips.rotDeg = 96.8} and miss the effector by 0.70
     * world units, i.e. it destroyed the pose on the frame it was switched on.
     *
     * <p>{@link #measure} therefore records the per-bone angle between the two,
     * read off the incoming pose, and the write-back converts through it. The
     * offset is invariant under the solve because rotating a bone turns its axis
     * and the direction to its child by the same amount, and it is identically
     * zero for every limb chain, so this costs nothing and changes nothing where
     * the two conventions already agreed.
     *
     * <p>It is measured for FABRIK chains only, and deliberately. {@link TwoBoneIk}
     * does not merely report link directions, it <em>reconstructs the effector</em>
     * from them ({@code root + l1*cos(rootWorld) + l2*cos(jointWorld)}), so a
     * two-bone chain whose bones are not aligned with their links is wrong inside
     * the solver and not merely in the write-back -- an offset here could not
     * rescue it. Measuring one anyway would only inject {@link Skeleton}'s
     * lookup-table trig noise (~1e-4 degrees) into an otherwise exact analytic
     * result, which is enough to push a limit-saturated joint a hair outside a
     * bound that {@link IkConstraint} promises to stay strictly inside.
     */
    private float boneWorldDeg(int index) {
        return kind == Kind.FABRIK ? worldDeg[index] + linkOffsetDeg[index] : worldDeg[index];
    }

    private void solveTwoBone(float refDeg, float mirror, float dt) {
        float px = poleX;
        float py = poleY;
        if (!poleSet) {
            // "Keep bending the way you already bend", and the definition of
            // "already" is the whole of this block.
            //
            // The obvious reading is the joint's current position, joints[2], and
            // for four passes that is what this did -- on the stated grounds that
            // it was "last frame's answer, so the default is self-reinforcing".
            // It is not, and the gap only shows up on the rig. Every scene in the
            // capture harness re-poses the skeleton from bind (or from a clip)
            // before the chains run, so by the time measure() reads it the elbow
            // is back where the *bind pose* put it. The default was therefore not
            // last frame's answer at all: it was a pole nailed to the shoulder,
            // which is exactly the fixed-pole configuration RigIk's own comment
            // says can never be made pretty. It stayed hidden because with no fold
            // preference the limb ran near enough to straight that the elbow sat
            // inside the pole hysteresis deadband and carried no signal; the
            // moment the limb was given a reason to stay bent, ik-reach flipped
            // its elbow at t=0.53 on a target that had done nothing unusual.
            //
            // So the previous solved joint is remembered here, in the chain, and
            // carried in the *parent's* frame rather than the world -- a clavicle
            // that rolls should take the elbow's preference with it, which is what
            // an anatomical shoulder does. It is blended toward the incoming pose
            // by weight, so at weight 0 the documented behaviour is exact ("do not
            // fight the animation") and at weight 1 the default is genuinely
            // self-reinforcing.
            px = joints[2];
            py = joints[3];
            if (hasLastPole && weight > 0f) {
                float deg = refDeg + lastPoleLocalDeg;
                float rx = joints[0] + lastPoleLen * IkMath.cosDeg(deg);
                float ry = joints[1] + lastPoleLen * IkMath.sinDeg(deg);
                px += (rx - px) * weight;
                py += (ry - py) * weight;
            }
        }
        twoBone.solve(joints[0], joints[1], lengths[0], lengths[1],
                settleX.value, settleY.value, px, py, refDeg, mirror, dt, twoBoneResult);
        worldDeg[0] = twoBoneResult.rootWorldDeg;
        worldDeg[1] = twoBoneResult.jointWorldDeg;

        // Remember where the solve put the joint, relative to the parent's frame.
        lastPoleLocalDeg = IkMath.deltaDeg(refDeg, worldDeg[0]);
        lastPoleLen = lengths[0];
        hasLastPole = true;
    }

    private void solveFabrik(float refDeg, float mirror) {
        fabrik.solve(joints, bones.length + 1, lengths, settleX.value, settleY.value, refDeg, mirror);
        for (int i = 0; i < bones.length; i++) {
            float dx = joints[2 * i + 2] - joints[2 * i];
            float dy = joints[2 * i + 3] - joints[2 * i + 1];
            // A zero-length link carries no direction; keep the pose's own so a
            // degenerate bone cannot swing its descendants somewhere arbitrary.
            // Minus the link offset, because worldDeg is in link space -- see
            // boneWorldDeg.
            worldDeg[i] = IkMath.length(dx, dy) > IkMath.EPS
                    ? IkMath.atan2Deg(dy, dx)
                    : skeleton.worldRotationDeg(bones[i].index) - linkOffsetDeg[i];
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
            // sits. Both sides go through boneWorldDeg: solver output is a link
            // direction and Bone.rotDeg is an axis angle.
            float parentWorld = i == 0 ? refDeg : boneWorldDeg(i - 1);
            float solvedLocal = sign * IkMath.deltaDeg(parentWorld + flip, boneWorldDeg(i));

            // Blend against the animation pose. Shortest arc on the first frame,
            // and continuous with the previous frame after that -- which is not
            // the same thing, and the difference is a visible 180-degree pop.
            //
            // Shortest arc alone is discontinuous exactly where the animation
            // pose and the solved pose are antipodal: the offset between them
            // flips from +180 to -180 across one frame, and at partial weight the
            // blended bone jumps by 360 * weight. It is not a corner case. It is
            // guaranteed whenever a parent bone revolves while IK holds a
            // world-fixed target, which is precisely the parry case this blend
            // exists to serve -- ik-parry caught upperArmL stepping 136 degrees in
            // one frame at weight 0.63, with the animation not driving that bone
            // at all.
            //
            // So the offset is unwrapped against last frame's rather than
            // re-derived from scratch: past the antipode the blend carries on the
            // way it was already going instead of reversing. Both endpoints are
            // untouched -- at weight 0 the animation is returned exactly, and at
            // weight 1 the result differs from the solved angle only by a whole
            // turn, which is the same pose -- so this changes the path a ramp
            // takes and never where it arrives.
            float animLocal = preRot[i] = b.rotDeg;
            float delta = IkMath.deltaDeg(animLocal, solvedLocal);
            if (hasPrevBlend) {
                delta = blendDeltaDeg[i] + IkMath.deltaDeg(blendDeltaDeg[i], delta);
            }
            blendDeltaDeg[i] = delta;
            b.rotDeg = animLocal + delta * weight;

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
