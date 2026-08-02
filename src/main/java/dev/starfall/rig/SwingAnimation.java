package dev.starfall.rig;

import com.badlogic.gdx.math.MathUtils;
import dev.starfall.anim.Pose;

/**
 * The System 1 hero motion: one overhead diagonal cut, per contract section G
 * and STYLE.md 7.1 (~40% anticipation / 15% release / 45% follow-through), and
 * rig-fixes section 4: a whole-body action, not an arm animating in isolation.
 * Pure keyframe animation, not physics -- the brief wants hand-authored
 * choreography.
 *
 * <p>Every bone group gets its own small set of keyframe times, not one
 * shared curve, so hips lead, spine drives, arms follow the spine, and the
 * head trails everything -- STYLE.md 7.1's "never peak on the same frame"
 * applied inside the body itself. See the {@code *_T} arrays below: hips
 * peaks earliest, head latest.
 *
 * <p>The sword-arm shoulder channel is a single continuous -360 degree
 * revolution across the whole clip: down-forward at rest, swung back-and-up
 * through the anticipation, down across the body through the release, and
 * back to exactly the resting angle by the end of follow-through. Never
 * reversing direction is what guarantees the blade traces one continuous arc
 * with no popping, independent of how the individual phase durations get
 * tuned later. Follow-through still carries a real fraction of that
 * revolution (not a token few degrees), so the settle stays visible across
 * the back half of a contact sheet rather than reading as already-finished.
 *
 * <p>Every channel is evaluated with {@link #keyframed}, which interpolates
 * with a per-segment smoothstep -- except the very first segment of every
 * channel, which uses ease-out instead (see {@link #ease0}). Smoothstep's
 * derivative is zero at both ends of a segment, so interior phase joins
 * always meet at zero relative velocity and can't visibly pop; ease-out
 * shares that same zero-velocity handoff into the next segment but starts at
 * full speed, so the gesture is visibly moving from frame one instead of
 * spending its first couple of frames easing up from a dead stop.
 */
public final class SwingAnimation {

    /** Total swing length, per contract section G. */
    public static final float DURATION = 2.4f;

    // Bind angles this animation deltas against (SamuraiRig.buildSkeleton).
    private static final float BIND_SHOULDER_L = -55f;
    private static final float BIND_FOREARM_L = -10f;
    private static final float BIND_HAND_L = 0f;

    // -- hips: leads ----------------------------------------------------------
    private static final float[] HIPS_T = {0f, 0.28f, 0.46f, 0.68f, 1f};
    private static final float[] HIPS_DX = {0f, -0.050f, 0.060f, 0.020f, 0f};
    private static final float[] HIPS_DY = {0f, -0.012f, -0.038f, -0.012f, 0f};
    private static final float[] HIPS_DROT = {0f, -4f, 5f, 1f, 0f};

    // -- legs: between hips and spine ------------------------------------------
    private static final float[] LEGS_T = {0f, 0.30f, 0.48f, 0.70f, 1f};
    private static final float[] THIGH_L_DROT = {0f, -4f, 6f, 2f, 0f};
    private static final float[] THIGH_R_DROT = {0f, 7f, -11f, -3f, 0f};   // rear leg extends as weight transfers
    private static final float[] SHIN_R_DROT = {0f, 9f, -13f, -4f, 0f};
    private static final float[] FOOT_R_DROT = {0f, 6f, 20f, 7f, 0f};      // heel lift on the rear foot
    private static final float[] FOOT_L_DROT = {0f, -3f, 5f, 1f, 0f};

    // -- spine/chest: the engine of the cut -------------------------------------
    private static final float[] SPINE_T = {0f, 0.34f, 0.52f, 0.72f, 1f};
    private static final float[] SPINE_DROT = {0f, 13f, -20f, -6f, 0f}; // arches back, then flexes hard forward
    private static final float[] CHEST_T = {0f, 0.36f, 0.54f, 0.74f, 1f};
    private static final float[] CHEST_DROT = {0f, 9f, -15f, -5f, 0f};

    // -- far arm: counter-rotates for balance, never static ---------------------
    private static final float[] FAR_ARM_T = {0f, 0.35f, 0.53f, 0.73f, 1f};
    private static final float[] SHOULDER_R_DROT = {0f, -20f, 30f, 8f, 0f};
    private static final float[] FOREARM_R_DROT = {0f, -15f, 22f, 6f, 0f};

    // -- sword arm: follows the spine ---------------------------------------------
    private static final float[] SHOULDER_T = {0f, 0.38f, 0.56f, 1f};
    // Absolute cumulative angle: -65 bind, -205 cocked back-and-up
    // (mod 360 = 155), -395 the cut (mod 360 = -35, down-forward), -425 lands
    // back on the resting angle (mod 360 = -65) -- one full revolution.
    private static final float[] SHOULDER_ABS = {-65f, -205f, -395f, -425f};
    private static final float[] ELBOW_T = {0f, 0.42f, 0.58f, 1f};
    private static final float[] ELBOW_ABS = {-10f, -50f, 10f, -10f};
    private static final float[] WRIST_T = {0f, 0.45f, 0.60f, 1f};
    private static final float[] WRIST_ABS = {0f, -15f, 15f, 0f};

    // -- neck/head: trails everything ---------------------------------------------
    private static final float[] NECK_T = {0f, 0.50f, 0.68f, 1f};
    private static final float[] NECK_DROT = {0f, -6f, 4f, 0f};
    private static final float[] HEAD_T = {0f, 0.58f, 0.76f, 1f};
    private static final float[] HEAD_DROT = {0f, -5f, 3f, 0f};

    /** Samples the pose at time t in [0, DURATION]. Clamped at the ends so callers can free-run past the clip. */
    public Pose sample(float timeSeconds) {
        float t = MathUtils.clamp(timeSeconds, 0f, DURATION) / DURATION;

        Pose pose = new Pose();
        pose.set("hips", keyframed(t, HIPS_T, HIPS_DX), keyframed(t, HIPS_T, HIPS_DY), keyframed(t, HIPS_T, HIPS_DROT));

        pose.set("thighL", 0f, 0f, keyframed(t, LEGS_T, THIGH_L_DROT));
        pose.set("thighR", 0f, 0f, keyframed(t, LEGS_T, THIGH_R_DROT));
        pose.set("shinR", 0f, 0f, keyframed(t, LEGS_T, SHIN_R_DROT));
        pose.set("footR", 0f, 0f, keyframed(t, LEGS_T, FOOT_R_DROT));
        pose.set("footL", 0f, 0f, keyframed(t, LEGS_T, FOOT_L_DROT));

        pose.set("spine", 0f, 0f, keyframed(t, SPINE_T, SPINE_DROT));
        pose.set("chest", 0f, 0f, keyframed(t, CHEST_T, CHEST_DROT));

        pose.set("shoulderR", 0f, 0f, keyframed(t, FAR_ARM_T, SHOULDER_R_DROT));
        pose.set("forearmR", 0f, 0f, keyframed(t, FAR_ARM_T, FOREARM_R_DROT));

        float shoulder = keyframed(t, SHOULDER_T, SHOULDER_ABS);
        float elbow = keyframed(t, ELBOW_T, ELBOW_ABS);
        float wrist = keyframed(t, WRIST_T, WRIST_ABS);
        pose.set("shoulderL", 0f, 0f, shoulder - BIND_SHOULDER_L);
        pose.set("forearmL", 0f, 0f, elbow - BIND_FOREARM_L);
        pose.set("handL", 0f, 0f, wrist - BIND_HAND_L);

        pose.set("neck", 0f, 0f, keyframed(t, NECK_T, NECK_DROT));
        pose.set("head", 0f, 0f, keyframed(t, HEAD_T, HEAD_DROT));

        return pose;
    }

    /** Piecewise interpolation through keyframes: ease-out on the first segment, smoothstep on the rest (see class doc). times must be non-decreasing and cover [0,1]. */
    private static float keyframed(float t, float[] times, float[] values) {
        int n = times.length;
        if (t <= times[0]) {
            return values[0];
        }
        if (t >= times[n - 1]) {
            return values[n - 1];
        }
        for (int i = 0; i < n - 1; i++) {
            if (t <= times[i + 1]) {
                float local = (t - times[i]) / (times[i + 1] - times[i]);
                float eased = i == 0 ? ease0(local) : smoothstep(local);
                return MathUtils.lerp(values[i], values[i + 1], eased);
            }
        }
        return values[n - 1];
    }

    private static float smoothstep(float x) {
        x = MathUtils.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    /** Ease-out (quadratic): full speed at x=0, zero velocity at x=1 -- so the gesture is visibly moving from the first frame, but still hands off cleanly into the next segment's smoothstep. */
    private static float ease0(float x) {
        x = MathUtils.clamp(x, 0f, 1f);
        return 1f - (1f - x) * (1f - x);
    }
}
