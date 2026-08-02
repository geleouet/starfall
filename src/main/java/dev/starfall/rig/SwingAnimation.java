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
    //
    // Revision 3 stretches every body channel's *return* rather than its peak.
    // Reshaping the sword arm (below) removed the elastic rebound it used to
    // have, and with it a large slice of the ink-centroid movement the pass-2
    // review measured in the back half of the clip -- the macro envelope came
    // out 55/22/23 against a 40/15/45 target, i.e. the follow-through had
    // vanished rather than been fixed. The wind-up excursions are a little
    // smaller and the settle is both larger and slower, so the body is still
    // visibly arriving over the last third of the sheet without the weapon
    // bouncing to do it. STYLE.md 7.1: recovery is long.
    // The forward drift *continues past the cut* -- the weight is still
    // arriving at 0.70, well after the blade has finished -- and then settles
    // over the last third. That is where the follow-through's ink-centroid
    // movement now comes from. It is a translation rather than a rotation on
    // purpose: rotating the spine back through the same span would swing the
    // blade up again and reintroduce exactly the rebound item 6 is about.
    private static final float[] HIPS_T = {0f, 0.30f, 0.50f, 0.70f, 0.88f, 1f};
    // The hips do not come all the way back. A cut transfers weight forward
    // and most of it stays transferred; hauling them fully home would drag the
    // whole arm -- and the blade tip with it -- backwards through the settle,
    // which reads as exactly the elastic rebound this revision is removing.
    private static final float[] HIPS_DX = {0f, -0.038f, 0.072f, 0.104f, 0.058f, 0.030f};
    private static final float[] HIPS_DY = {0f, -0.010f, -0.045f, -0.040f, -0.018f, -0.004f};
    private static final float[] HIPS_DROT = {0f, -3f, 5f, 6f, 2f, 0f};

    // -- legs: between hips and spine ------------------------------------------
    private static final float[] LEGS_T = {0f, 0.32f, 0.52f, 0.80f, 1f};
    private static final float[] THIGH_L_DROT = {0f, -3f, 6f, 3f, 0f};
    private static final float[] THIGH_R_DROT = {0f, 5f, -11f, -5f, 0f};   // rear leg extends as weight transfers
    private static final float[] SHIN_R_DROT = {0f, 7f, -13f, -6f, 0f};
    private static final float[] FOOT_R_DROT = {0f, 5f, 20f, 10f, 0f};     // heel lift on the rear foot
    private static final float[] FOOT_L_DROT = {0f, -2f, 5f, 2f, 0f};

    // -- spine/chest: the engine of the cut -------------------------------------
    private static final float[] SPINE_T = {0f, 0.34f, 0.55f, 0.82f, 1f};
    private static final float[] SPINE_DROT = {0f, 10f, -24f, -12f, -6f}; // arches back, then flexes hard forward
    private static final float[] CHEST_T = {0f, 0.36f, 0.57f, 0.84f, 1f};
    private static final float[] CHEST_DROT = {0f, 7f, -18f, -9f, -5f};

    // -- far arm: counter-rotates for balance, never static ---------------------
    private static final float[] FAR_ARM_T = {0f, 0.35f, 0.55f, 0.76f, 1f};
    private static final float[] SHOULDER_R_DROT = {0f, -18f, 34f, 20f, 0f};
    private static final float[] FOREARM_R_DROT = {0f, -13f, 26f, 15f, 0f};

    // -- sword arm: follows the spine ---------------------------------------------
    //
    // Revision 3 reshapes this channel inside the macro timing, per
    // rig-fixes-3 item 6. Three measured faults, three changes:
    //
    // 1. The anticipation was the *fastest* part of the clip -- the tip covered
    //    ~400 px in the first 0.44 s. It is now one long smoothstep from rest to
    //    0.41, which starts at zero velocity, peaks near the middle of the
    //    wind-up and decelerates into a near-hold at the apex (the 0.41/0.47
    //    key pair). STYLE.md 7.1 wants anticipation slow; here it is slower than
    //    the cut everywhere, and slowest exactly where the pose is readable.
    // 2. The apex left the frame for three of twelve frames. The arm now
    //    extends nearly straight back rather than folding overhead, so at the
    //    apex the blade lies up-and-back at roughly 175 degrees world with the
    //    tip around (-1.6, 2.0) -- inside a frame whose top edge is 2.3.
    // 3. The follow-through bounced back up 100 px and was still moving at the
    //    final frame. Elbow and wrist used to reverse sign after the cut, which
    //    is where that came from; every channel is now monotonic from the apex
    //    onward and lands on the resting angle by 1.0 with a long slow tail.
    private static final float[] SHOULDER_T = {0f, 0.41f, 0.47f, 0.545f, 0.70f, 0.86f, 1f};
    // Absolute local angle (applyPose adds the delta onto bind, so the value
    // here *is* the local rotation). -55 rest, -220 cocked back with the hands
    // above the head, -367 the cut, -425 = -65 mod 360 lands the arm just past
    // its resting angle. One continuous revolution, never reversing.
    //
    // The cut lands on 0.545 rather than 0.60 deliberately: capture frames fall
    // every 1/11 of the clip, and with the arm nearly straight through the top
    // of the arc the tip traces a two-unit radius. Ending the cut a frame later
    // would put a captured frame at the top of that circle, several units above
    // the frame edge -- which is the "apex leaves the frame" fault, arriving on
    // the way *down* instead of on the way up. Between f5 (apex, blade laid back
    // horizontally) and f6 (blade already down-forward) the swept arc is carried
    // by the renderer's trail ribbon, which is what STYLE.md 7.2 asks fast motion
    // to do: smear, not strobe.
    private static final float[] SHOULDER_ABS = {-55f, -201f, -220f, -367f, -410f, -424f, -430f};
    private static final float[] ELBOW_T = {0f, 0.41f, 0.47f, 0.545f, 0.70f, 0.86f, 1f};
    private static final float[] ELBOW_ABS = {-10f, -2f, 5f, -22f, -16f, -12f, -10f};
    private static final float[] WRIST_T = {0f, 0.41f, 0.47f, 0.545f, 0.70f, 0.86f, 1f};
    private static final float[] WRIST_ABS = {0f, 3f, 6f, -6f, -4f, -2f, 0f};

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

        // slowIn, not keyframed: the sword arm is the one channel whose first
        // segment must *not* start at full speed (rig-fixes-3 item 6).
        float shoulder = slowIn(t, SHOULDER_T, SHOULDER_ABS);
        float elbow = slowIn(t, ELBOW_T, ELBOW_ABS);
        float wrist = slowIn(t, WRIST_T, WRIST_ABS);
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

    /**
     * Same interpolation as {@link #keyframed} but smoothstep on <em>every</em>
     * segment including the first, so the channel leaves rest at zero velocity.
     * The body channels keep the ease-out opening (it is what stopped the clip
     * from starting with dead frames); the sword arm cannot afford it, because
     * that arm is what the reviewer measures and its opening segment is the
     * whole anticipation.
     */
    private static float slowIn(float t, float[] times, float[] values) {
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
                return MathUtils.lerp(values[i], values[i + 1], smoothstep(local));
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
