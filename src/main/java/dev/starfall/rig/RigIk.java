package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;
import dev.starfall.ik.IkChain;
import dev.starfall.ik.IkConstraint;

/**
 * The IK chains bound to a {@link SamuraiRig}, plus the one thing a caller must
 * not get wrong: the order they update in.
 *
 * <p>It is a holder, not a system. {@link SamuraiRig} is untouched -- this is
 * built alongside it from its {@link Skeleton}, so a scene that wants no IK
 * simply does not construct one, and System 4 can reach every chain directly to
 * drive targets and weights per frame.
 *
 * <p><b>Ordering.</b> The contract is animation writes {@code Bone} locals, then
 * chains update, then the renderer draws. Within "then chains update" the order
 * is root-outward and it is not arbitrary: the spine chain moves the hips and the
 * chest, which carries the shoulder the arm chain hangs off and the hip sockets
 * the legs hang off. Solving a limb before the trunk it is attached to would
 * solve it against last frame's shoulder position. {@link #update} does this in
 * the right order and is the only method a per-frame caller needs.
 *
 * <p><b>Weights start at zero.</b> Every chain is constructed inert: present,
 * primed, updating, and writing nothing. That is deliberate on two counts. A rig
 * that silently rewrites its own animation the moment IK is constructed is a
 * trap, and -- more usefully -- a chain kept updating at weight 0 keeps its
 * settle filter and its bend-side filter warm against a target that is being fed
 * every frame, so ramping the weight in later starts from a pose that is already
 * tracking rather than from a cold one that lurches. Callers should therefore set
 * targets every frame whether or not the weight is up, which is exactly what
 * {@code ik-parry} does.
 */
public final class RigIk {

    /**
     * Sword-arm shoulder-to-hand. The end effector is {@code handL}'s origin, so
     * a target is where the fist goes; the blade hangs off the hand and follows.
     */
    private final IkChain swordArm;

    /** Near/front leg, effector at {@code footL}'s origin (the ankle). */
    private final IkChain legL;

    /** Far/back leg, effector at {@code footR}'s origin. */
    private final IkChain legR;

    /**
     * Trunk, {@code hips -> spine -> chest}, effector at {@code neck}'s origin.
     * FABRIK rather than two-bone because it is three links, and because the
     * spine is the first of the chains this project will eventually want on hair
     * and cloth.
     */
    private final IkChain spine;

    private final Skeleton skeleton;
    private final Bone hips;

    /**
     * Knee poles in hips-local space. See {@link #refreshLegPoles()} for why they
     * are carried rather than fixed in the world.
     */
    private static final float POLE_L_X = 0.363f;
    private static final float POLE_L_Y = -0.448f;
    private static final float POLE_R_X = 0.126f;
    private static final float POLE_R_Y = -0.407f;

    private final Vector2 scratch = new Vector2();
    private float poleLX, poleLY, poleRX, poleRY;

    public RigIk(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.hips = skeleton.bone("hips");

        // -- sword arm ---------------------------------------------------------
        //
        // No pole, on purpose. The default -- the chain's own current elbow
        // position -- reads as "keep bending the way the animation already
        // bends", and it is self-reinforcing: the elbow stays on whichever side
        // of the shoulder-to-target line it is already on, so a target orbiting
        // the shoulder sweeps the whole workspace without ever provoking a bend
        // flip. A fixed world pole would flip every time the target crossed it,
        // which is the one motion in this solver that cannot be made pretty.
        //
        // The shoulder is left unlimited: SwingAnimation drives shoulderL through
        // a full -360 revolution and its local angles run past -430 degrees,
        // which is outside anything IkConstraint will even accept as a range.
        //
        // The elbow limit is symmetric, and that is a decision rather than
        // laziness. A one-sided anatomical stop is the obvious thing to author and
        // it is wrong here, because with the pose-derived pole the *side* the
        // elbow ends up on is chosen by the pose, not by the rig: ik-reach and
        // ik-extreme commit to the elbow above the aim (the bind pose's own bend,
        // reaching down and forward) while ik-parry commits to it below (reaching
        // up into a guard). Authoring a stop that only allows one of those does
        // not make the arm anatomical, it makes the other scene saturate its
        // limit and miss the target by 0.10 world units -- measured, not guessed.
        // So the range is symmetric and its only job is the one a range can do
        // without knowing the side: stop the limb folding shut hard enough to
        // candy-wrap the sleeve at the elbow, which STYLE.md 7.2 also bans.
        //
        // The softness is 0.05 rather than the default 0.08 for a reason worth
        // recording. A soft bound eases over a band *inside* the range, so a
        // default band on a 304-degree range starts pulling at 128 degrees of
        // bend -- and the scripts' tightest pose is 131. That cost 9.5 mm of
        // reach on a target the arm could comfortably hit, which is a limit doing
        // something visible while nowhere near being reached. Narrowing the band
        // keeps the ease where it belongs, at the fold, and leaves the working
        // range exact.
        this.swordArm = IkChain.twoBone(skeleton, "upperArmL", "forearmL", "handL")
                .limit(1, IkConstraint.range(-152f, 152f, 0.05f))
                .settleSeconds(0.35f)
                .weight(0f);

        // -- legs --------------------------------------------------------------
        //
        // Poles, on purpose, and this is the case the solver author flagged as
        // safe: a pole carried on the body never fights the pose. Both legs are
        // very nearly straight at bind (thigh and shin share a world angle, and
        // hip-to-ankle measures 0.840 against a 0.840 reach), so the pose-derived
        // default pole sits exactly on the chain axis and carries no information
        // about which way the knee should go. A pole held out in front of the
        // pelvis does, and because it rides the hips it stays in front of the
        // pelvis however the trunk turns.
        this.legL = IkChain.twoBone(skeleton, "thighL", "shinL", "footL")
                .limit(1, IkConstraint.range(-150f, 3f))
                .settleSeconds(0.30f)
                .weight(0f);
        this.legR = IkChain.twoBone(skeleton, "thighR", "shinR", "footR")
                .limit(1, IkConstraint.range(-150f, 3f))
                .settleSeconds(0.30f)
                .weight(0f);

        // -- trunk -------------------------------------------------------------
        //
        // Limits here are in link space, not in Bone.rotDeg -- see IkChain's
        // class note. The three link directions in the bind stack are 90 (hips to
        // spine, straight up), -10 relative to that (the authored forward lean)
        // and 0 relative to that again. They are tight because a three-link
        // FABRIK chain is redundant: pinning the effector does not pin the pose,
        // so without limits the solver is free to find an equally valid answer
        // that folds the figure in half. Tight limits plus a partial weight are
        // what turn this chain into a lean rather than a re-pose.
        //
        // 12 degrees on the hips link and no more: rotating hips carries both
        // thighs with it, so every degree here is a degree of both legs swinging.
        this.spine = IkChain.fabrik(skeleton, "hips", "chest", "neck")
                .limit(0, IkConstraint.around(90f, 12f))
                .limit(1, IkConstraint.around(-10f, 20f))
                .limit(2, IkConstraint.around(0f, 20f))
                .settleSeconds(0.45f)
                .weight(0f);
        this.spine.fabrikSolver().maxIterations(16);

        refreshLegPoles();
    }

    public static RigIk of(SamuraiRig rig) {
        return new RigIk(rig.skeleton());
    }

    public IkChain swordArm() {
        return swordArm;
    }

    public IkChain legL() {
        return legL;
    }

    public IkChain legR() {
        return legR;
    }

    public IkChain spine() {
        return spine;
    }

    /** Where {@link #legL}'s pole currently sits in world space, for debug overlays. */
    public Vector2 legPoleL(Vector2 out) {
        return out.set(poleLX, poleLY);
    }

    public Vector2 legPoleR(Vector2 out) {
        return out.set(poleRX, poleRY);
    }

    /** Current world position of a bone, useful for authoring targets against the rig. */
    public Vector2 worldPosition(String bone, Vector2 out) {
        return skeleton.worldPosition(skeleton.bone(bone).index, out);
    }

    /**
     * Maps a point expressed in the hips' frame into world space, using the hips'
     * current transform.
     *
     * <p>The trunk chain's target has to be given this way rather than as a fixed
     * world point. Its own root is the hips, and the hips move: the swing clip
     * translates them 0.10 forward and rotates them 6 degrees. A target nailed to
     * the world would therefore ask the spine for a different amount of bend on
     * every frame of an animation that was not trying to bend it at all.
     */
    public Vector2 fromHips(float localX, float localY, Vector2 out) {
        skeleton.worldPosition(hips.index, out);
        float rot = skeleton.worldRotationDeg(hips.index);
        float cos = (float) Math.cos(Math.toRadians(rot));
        float sin = (float) Math.sin(Math.toRadians(rot));
        return out.set(out.x + localX * cos - localY * sin,
                out.y + localX * sin + localY * cos);
    }

    /**
     * Solves every chain for this frame, trunk first. Call once, after the
     * animation has written its locals and before the renderer runs; each chain
     * refreshes the skeleton's world transforms itself, so the skeleton is left
     * current on return.
     */
    public void update(float dt) {
        spine.update(dt);
        // After the trunk, because the poles ride the hips the trunk just moved.
        refreshLegPoles();
        swordArm.update(dt);
        legL.update(dt);
        legR.update(dt);
    }

    /**
     * Teleports every chain onto its current target with no settle and no
     * in-flight bend flip. Scene setup and respawns only -- anywhere there is a
     * previous frame to be continuous with, {@link #update} is the answer.
     */
    public void snap() {
        spine.snap();
        refreshLegPoles();
        swordArm.snap();
        legL.snap();
        legR.snap();
    }

    /**
     * Re-plants the knee poles in front of the pelvis, in world space, from the
     * hips' current transform. Doing this every frame is what makes them
     * body-carried: a pole nailed to a fixed world point would drift to the wrong
     * side of the leg as soon as the figure leaned, and the knee would flip.
     */
    private void refreshLegPoles() {
        skeleton.worldPosition(hips.index, scratch);
        float ox = scratch.x;
        float oy = scratch.y;
        float rot = skeleton.worldRotationDeg(hips.index);
        // Math, not MathUtils: the lookup-table trig carries ~4e-4 of error, and
        // a pole is an input to the bend-side filter, which is the one place in
        // this system where noise turns into a visible flip.
        float cos = (float) Math.cos(Math.toRadians(rot));
        float sin = (float) Math.sin(Math.toRadians(rot));
        poleLX = ox + POLE_L_X * cos - POLE_L_Y * sin;
        poleLY = oy + POLE_L_X * sin + POLE_L_Y * cos;
        poleRX = ox + POLE_R_X * cos - POLE_R_Y * sin;
        poleRY = oy + POLE_R_X * sin + POLE_R_Y * cos;
        legL.pole(poleLX, poleLY);
        legR.pole(poleRX, poleRY);
    }
}
