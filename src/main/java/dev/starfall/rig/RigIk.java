package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;
import dev.starfall.ik.AimLink;
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

    /**
     * The clavicle, driven as link 0 in front of {@link #swordArm}. See
     * {@link AimLink} for why it is a separate object rather than a third bone in
     * the arm chain.
     */
    private final AimLink clavicle;

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
    private float clavicleWeight = 0.25f;

    private final Bone chest;
    private boolean armPoleSet;
    private float armPoleLocalX, armPoleLocalY;

    public RigIk(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.hips = skeleton.bone("hips");
        this.chest = skeleton.bone("chest");

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
                // 0.34, and the number is only meaningful next to the three around
                // it: clavicle 0.18, spine 0.24, arm 0.34, forearm +0.11 on top.
                // STYLE.md 7.0's third positive is that nothing in a chain may
                // arrive on the same frame, and a single shared settle time is the
                // one way to guarantee that everything does. Ordered this way the
                // trunk is already leaning while the shoulder is still rolling and
                // the hand is still travelling, which is 10's "everything peaking
                // on the same frame" answered by construction rather than by luck.
                .settleSeconds(0.34f)
                // The wrist arrives after the elbow. 0.11 s is about seven frames
                // of separation on a limb whose own settle is 0.34 -- enough to
                // read as the hand being carried, not enough to look like the
                // forearm has come loose.
                .boneLagSeconds(1, 0.11f)
                .weight(0f);
        // The reluctance of STYLE.md 7.0's second positive. Measured on the pass-2
        // capture, the elbow held 3.9 degrees of bend at maximum reach -- a rod --
        // and the corpus never contains that pose: all eight reference figures
        // hold the elbow between 90 and 130. So the arm opens freely to 112 and
        // then gives way, and however far out of reach ik-extreme drives the
        // target it never straightens past 142. Authored on the sword arm alone:
        // both legs sit at full extension in the bind pose, so the same knob on a
        // leg would fold the figure's knees at rest.
        this.swordArm.twoBoneSolver().foldPreference(112f, 142f);

        // -- clavicle ----------------------------------------------------------
        //
        // Low weight, tight limit, fastest settle in the rig. It is not trying to
        // reach anything -- the arm does that -- it is trying to stop the arm's
        // root being a fixed pixel, which is what the pass-2 review measured and
        // named as the mechanical cause of the frozen torso.
        //
        // The honest limit here is the bone: shoulderL is 0.04 world units long,
        // so a 30 degree roll moves the shoulder socket about 4 px at capture
        // framing. That is real and it is small, and it is why the clavicle is the
        // second item on the work order rather than the first -- the hips and the
        // spine carry the excursion, and this carries the shoulder's own attitude.
        this.clavicle = new AimLink(skeleton, "shoulderL")
                .limit(IkConstraint.around(-55f, 30f))
                .settleSeconds(0.18f)
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
                // 0.24, down from 0.45. STYLE.md 7.0's first positive is that
                // motion must have a source and that the source is the trunk, so
                // the trunk cannot be the last thing in the rig to arrive. It is
                // now the first, ahead of the clavicle's 0.18-second roll only
                // because the script hands it a target that leads by 45 ms as well
                // -- lag in the filter and lead in the script are two different
                // halves of the same spiral and this rig uses both.
                .settleSeconds(0.24f)
                // The chest is the last link of the trunk and it carries the
                // shoulder, so giving it its own lag is what makes the ribcage
                // counter-rotate behind the pelvis rather than with it.
                .boneLagSeconds(2, 0.09f)
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

    /** The sword arm's link 0. Aimed automatically from {@link #swordArm}'s own target. */
    public AimLink clavicle() {
        return clavicle;
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
        // Trunk, then clavicle, then arm: the same root-outward order the rest of
        // this class follows, extended by one link. The clavicle takes the arm's
        // own target so callers set one target for the limb and get the whole
        // shoulder-to-hand run driven by it.
        clavicle.target(swordArm.targetX(), swordArm.targetY()).weight(clavicleWeight * swordArm.weight());
        refreshArmPole();
        clavicle.update(dt);
        swordArm.update(dt);
        legL.update(dt);
        legR.update(dt);
    }

    /**
     * Plants the sword arm's elbow pole at a point in the chest's frame, so it
     * rides the ribcage the way {@link #refreshLegPoles()} rides the pelvis.
     *
     * <p>The class comment above explains why the arm normally has <em>no</em>
     * pole, and that reasoning stands for the three solver scenes. It does not
     * stand for {@code ik-gesture}, and the reason is the one the legs already
     * demonstrate: the bind arm is very nearly straight -- shoulder to hand
     * measures 0.996 of its own reach -- so the elbow sits almost exactly on the
     * chain axis, and a pose-derived pole there carries no information at all.
     * ik-gesture's opening aim happens to fall within half a degree of the bind
     * arm's own direction, which means the side the elbow commits to on frame
     * zero is decided by float rounding. It was measured committing to the
     * <em>wrong</em> one: upper arm thrust forward, elbow above the aim, which is
     * the pose no reference contains and the exact opposite of the one the scene
     * exists to show.
     *
     * <p>A chest-carried pole below and behind the shoulder says what the corpus
     * says -- upper arm hanging, elbow tucked under -- and because it rides the
     * ribcage it stays behind the shoulder however the trunk turns. Over the whole
     * of ik-gesture the pole sits 20 to 52 degrees clear of the aim on the same
     * side, i.e. far outside the hysteresis deadband in the same direction
     * throughout, so it decides frame zero and is then never in a position to
     * provoke a flip.
     */
    public RigIk armPoleFromChest(float localX, float localY) {
        this.armPoleSet = true;
        this.armPoleLocalX = localX;
        this.armPoleLocalY = localY;
        return this;
    }

    /** Drops back to the pose-derived pole, which is what the three solver scenes use. */
    public RigIk clearArmPole() {
        this.armPoleSet = false;
        swordArm.clearPole();
        return this;
    }

    private void refreshArmPole() {
        if (!armPoleSet) {
            return;
        }
        skeleton.worldPosition(chest.index, scratch);
        float rot = skeleton.worldRotationDeg(chest.index);
        float cos = (float) Math.cos(Math.toRadians(rot));
        float sin = (float) Math.sin(Math.toRadians(rot));
        swordArm.pole(scratch.x + armPoleLocalX * cos - armPoleLocalY * sin,
                scratch.y + armPoleLocalX * sin + armPoleLocalY * cos);
    }

    /**
     * How far the clavicle follows the hand, scaled by the arm chain's own weight
     * so a limb faded out takes its shoulder roll with it.
     */
    public RigIk clavicleWeight(float w) {
        this.clavicleWeight = Math.max(0f, Math.min(1f, w));
        return this;
    }

    /**
     * Teleports every chain onto its current target with no settle and no
     * in-flight bend flip. Scene setup and respawns only -- anywhere there is a
     * previous frame to be continuous with, {@link #update} is the answer.
     */
    public void snap() {
        spine.snap();
        refreshLegPoles();
        clavicle.target(swordArm.targetX(), swordArm.targetY()).weight(clavicleWeight * swordArm.weight());
        refreshArmPole();
        clavicle.snap();
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
