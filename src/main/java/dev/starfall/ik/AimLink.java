package dev.starfall.ik;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;

/**
 * One bone that turns part of the way toward a target: the clavicle in front of a
 * two-bone arm, and later the same shape for a head that tracks what it is
 * looking at.
 *
 * <p><b>Why this exists.</b> System 2's pass-2 review measured the arm chain's
 * root sitting at the same pixel in all twenty-four frames of {@code ik-extreme}
 * and named it the mechanical cause of the frozen torso: "the root pinned at a
 * fixed pixel in all 24 frames is the mechanical cause of item 1. A three-link
 * chain (clavicle -> upper arm -> forearm) with a low-weight clavicle costs
 * almost nothing and buys the shoulder roll every reference figure has."
 *
 * <p>It is not folded into {@link IkChain} as a third bone, and that is a
 * decision rather than an omission. {@link TwoBoneIk} is analytic; making the arm
 * a three-link chain would mean solving it with {@link FabrikSolver} instead, and
 * with it would go the damped bend side, the hysteresis deadband, the soft reach
 * ceiling and the angle-space flip -- every property the same review listed as
 * must-not-regress. So the clavicle is solved <em>before</em> the arm and
 * independently: it turns, the shoulder socket moves with it, and the two-bone
 * chain then solves from a root that is no longer pinned. Mechanically that is
 * exactly "link 0 at a low weight", and it leaves the analytic solver intact.
 *
 * <p>Everything about it is deliberately weak. It aims a {@link #weight(float)}
 * fraction of the way, inside a soft {@link IkConstraint}, through a critically
 * damped filter with its own settle time -- which is what makes the clavicle
 * arrive <em>before</em> the elbow and the elbow before the wrist, per STYLE.md
 * 7.0's third positive. A clavicle that tracked the hand exactly would read as a
 * second elbow.
 */
public final class AimLink {

    private final Skeleton skeleton;
    private final Bone bone;

    private final Damped angle = new Damped(0f);
    private final Vector2 scratch = new Vector2();

    private IkConstraint limit = IkConstraint.free();
    private float weight;
    private float settleSeconds = 0.22f;
    private float maxSlewDegPerSecond = 1200f;

    private float targetX, targetY;
    private float axisOffsetDeg;
    private float restLocalDeg;
    private float prevLocalDeg;
    private boolean primed;

    public AimLink(Skeleton skeleton, String boneName) {
        this.skeleton = skeleton;
        this.bone = skeleton.bone(boneName);
        this.restLocalDeg = bone.bindRotDeg;
    }

    /** How far toward the target the bone turns: 0 leaves the pose alone, 1 points it straight at the target. */
    public AimLink weight(float w) {
        this.weight = IkMath.clamp(w, 0f, 1f);
        return this;
    }

    public float weight() {
        return weight;
    }

    /** Limit on this bone's own {@code rotDeg}, applied softly like every other limit here. */
    public AimLink limit(IkConstraint c) {
        this.limit = c == null ? IkConstraint.free() : c;
        return this;
    }

    /** Terminal settle. Shorter than the chain downstream of it, so this link leads. */
    public AimLink settleSeconds(float seconds) {
        this.settleSeconds = Math.max(0f, seconds);
        return this;
    }

    public float settleSeconds() {
        return settleSeconds;
    }

    /** Same soft ceiling of last resort as {@link IkChain#maxSlewDegPerSecond(float)}. Zero disables it. */
    public AimLink maxSlewDegPerSecond(float degPerSecond) {
        this.maxSlewDegPerSecond = Math.max(0f, degPerSecond);
        return this;
    }

    public AimLink target(float x, float y) {
        this.targetX = x;
        this.targetY = y;
        return this;
    }

    /**
     * The angle between this bone's own +x axis and the thing that should end up
     * pointing at the target.
     *
     * <p>Zero aims the bone itself, which is what a clavicle wants. A wrist wants
     * something else: the blade it carries sits at a fixed bind angle out of the
     * hand, so aiming the <em>hand</em> at a crossing points the hand at it and
     * leaves the blade 45 degrees off. Setting this to that bind angle makes the
     * link aim the blade and turn the hand by whatever that takes -- which is what
     * a wrist actually does, and what pass 1 of System 4 had no way to express.
     *
     * <p>It composes with mirroring correctly for free: the offset is a local
     * angle, so it reflects with the bone rather than needing a sign.
     */
    public AimLink axisOffsetDeg(float deg) {
        this.axisOffsetDeg = deg;
        return this;
    }

    /** Local rotation the bone falls back to at weight 0. Defaults to its bind angle. */
    public AimLink restLocalDeg(float deg) {
        this.restLocalDeg = deg;
        return this;
    }

    public Bone bone() {
        return bone;
    }

    /**
     * Turns the bone and refreshes the skeleton. Call after the animation has
     * written locals and before any chain that hangs off this bone solves.
     *
     * <p>{@code dt <= 0} teleports, matching {@link IkChain#update}.
     */
    public void update(float dt) {
        skeleton.updateWorldTransforms();

        float parentWorld = bone.parent == null ? 0f : skeleton.worldRotationDeg(bone.parent.index);
        float mirror = mirrorSignAbove(bone);
        float flip = bone.scaleX < 0f ? 180f : 0f;

        skeleton.worldPosition(bone.index, scratch);
        float dx = targetX - scratch.x;
        float dy = targetY - scratch.y;

        float wantLocal;
        if (IkMath.length(dx, dy) <= IkMath.EPS) {
            // A target sitting on the joint carries no direction; hold the rest
            // pose rather than spinning on atan2 noise.
            wantLocal = restLocalDeg;
        } else {
            float aimWorld = IkMath.atan2Deg(dy, dx);
            float aimLocal = mirror * IkMath.deltaDeg(parentWorld + flip, aimWorld) - axisOffsetDeg;
            // Partway, measured on the short arc from rest, so the weight is a
            // fraction of a turn rather than a lerp between two absolute angles
            // that could pick the long way round.
            wantLocal = restLocalDeg + weight * IkMath.deltaDeg(restLocalDeg, aimLocal);
        }
        wantLocal = limit.apply(wantLocal);

        if (!primed || dt <= 0f) {
            angle.set(wantLocal);
        } else {
            angle.step(wantLocal, settleSeconds, dt);
        }

        float local = angle.value;
        if (primed && dt > 0f && maxSlewDegPerSecond > 0f) {
            float allowed = maxSlewDegPerSecond * dt;
            float delta = IkMath.deltaDeg(prevLocalDeg, local);
            float mag = Math.abs(delta);
            if (mag > 0.7f * allowed) {
                local = prevLocalDeg + Math.signum(delta) * IkMath.softCeil(mag, 0.7f * allowed, allowed);
                angle.value = local;
            }
        }

        bone.rotDeg = local;
        prevLocalDeg = local;
        primed = true;
        skeleton.updateWorldTransforms();
    }

    /** Teleports onto the current target: scene setup and respawns only. */
    public void snap() {
        primed = false;
        update(0f);
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
