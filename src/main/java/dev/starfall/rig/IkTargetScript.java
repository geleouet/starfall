package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;

/**
 * The three IK capture scenes' motion, as one deterministic function of time.
 *
 * <p>It lives apart from the scenes because each script has to drive two of
 * them -- the ink capture that gets graded and the {@code ShapeRenderer} debug
 * capture that gets measured -- and a target curve that differed between the two
 * would make the debug view a picture of a different run.
 *
 * <p>Everything here is a closed-form function of {@code t}. No integration, no
 * state, no {@code Math.random}, so a scene driven by any sequence of updates
 * summing to the same {@code t} produces the same frame, which is what
 * {@link dev.starfall.capture.Scene} requires. Trig is {@link Math}, not
 * {@code MathUtils}: the lookup table's ~4e-4 error is invisible in a skinning
 * matrix and is not invisible in a quantity feeding the bend-side filter.
 *
 * <p>Anchors are read off the rig's bind pose rather than written down, so the
 * scripts stay pointed at the right part of the figure if the rig is retuned.
 * Radii are fractions of the arm's own reach for the same reason: "1.0" means
 * "exactly at the reach boundary" whatever the arm is currently measured at.
 */
public final class IkTargetScript {

    public enum Kind {
        /**
         * The continuity showcase. A target orbiting the shoulder slowly through
         * more than a full revolution while its radius breathes across the reach
         * boundary four times -- so the arm passes out of reach and back in, in
         * front, above, behind and below, with nothing but the settle filter and
         * the soft reach limit holding it together.
         */
        REACH,
        /**
         * STYLE.md 7.2's list, in order: two hard direction reversals, two
         * teleports (the second across the chain axis), a target driven to nearly
         * three times reach, and a snap back to a close hold so the terminal
         * settle is visible on its own. Every phase joins the next linearly and
         * unsmoothed on purpose -- easing the script would be testing the script.
         */
        EXTREME,
        /**
         * A weight ramp against {@link SwingAnimation}: IK fades in over the
         * swing's wind-up, holds a raised guard, and fades back out in time for
         * the cut to play out untouched underneath it. 40/15/45 of a 1.05 s
         * gesture, per STYLE.md 7.1. The guard target itself slides along a smooth
         * arc throughout -- 7.2 wants a parry to be "a deflection curve, not a
         * collision" -- so the only discontinuity in the scene is the one being
         * demonstrated, which is the weight.
         */
        PARRY
    }

    private static final float REACH_DURATION = 5f;
    private static final float EXTREME_DURATION = 4f;

    /**
     * How long the parry gesture takes, inside the 2.4 s swing it interrupts.
     *
     * <p>1.05 s, and the number is load-bearing rather than tasteful. The parry
     * has to be over before the cut starts at 1.13 s, because the swing turns
     * shoulderL through a continuous -375 degree revolution across the clip and
     * IK holding a world-fixed guard through that revolution means the upper arm
     * has to counter-rotate through all of it. The blend then has more than a
     * whole turn of accumulated offset to unwind on the way out, and the recovery
     * comes back as a windmill rather than as a return -- measured at 437 degrees
     * of unwind before this window was pulled in. Ending inside the wind-up keeps
     * the accumulation at about 155 degrees, so the release is one short sweep.
     *
     * <p>Split 40 / 15 / 45, per STYLE.md 7.1: long anticipation, brief hold at
     * the point of contact, long recovery.
     */
    private static final float PARRY_SPAN = 1.05f;
    private static final float PARRY_IN = 0.40f * PARRY_SPAN;
    private static final float PARRY_HOLD_END = 0.55f * PARRY_SPAN;
    private static final float PARRY_OUT = PARRY_SPAN - PARRY_HOLD_END;

    public final Kind kind;

    private final float shoulderX, shoulderY;
    /** Upper arm + forearm, measured off the bind pose. The radius unit for every target below. */
    private final float reach;
    /** Neck origin in hips-local space at bind: where the trunk chain's effector rests. */
    private final float neckLocalX, neckLocalY;

    /** Bind ankle positions. The feet are planted at these for the whole of every script. */
    public final float footLX, footLY, footRX, footRY;

    // -- outputs, refreshed by sample() ---------------------------------------

    /** World target for the sword hand. */
    public float armX, armY;
    public float armWeight;
    /** Trunk target, in hips-local space -- the trunk rides the body, so its target must too. */
    public float spineLocalX, spineLocalY;
    public float spineWeight;
    public float legWeight;

    public IkTargetScript(Kind kind, Skeleton bindSkeleton) {
        this.kind = kind;
        Vector2 v = new Vector2();
        bindSkeleton.worldPosition(bindSkeleton.bone("upperArmL").index, v);
        this.shoulderX = v.x;
        this.shoulderY = v.y;
        float ux = v.x;
        float uy = v.y;
        bindSkeleton.worldPosition(bindSkeleton.bone("forearmL").index, v);
        float ex = v.x;
        float ey = v.y;
        float upper = (float) Math.hypot(ex - ux, ey - uy);
        bindSkeleton.worldPosition(bindSkeleton.bone("handL").index, v);
        this.reach = upper + (float) Math.hypot(v.x - ex, v.y - ey);

        bindSkeleton.worldPosition(bindSkeleton.bone("hips").index, v);
        float hx = v.x;
        float hy = v.y;
        bindSkeleton.worldPosition(bindSkeleton.bone("neck").index, v);
        // Bind hips rotation is zero, so hips-local is a plain translation here.
        this.neckLocalX = v.x - hx;
        this.neckLocalY = v.y - hy;

        bindSkeleton.worldPosition(bindSkeleton.bone("footL").index, v);
        this.footLX = v.x;
        this.footLY = v.y;
        bindSkeleton.worldPosition(bindSkeleton.bone("footR").index, v);
        this.footRX = v.x;
        this.footRY = v.y;

        sample(0f);
    }

    /** Clip length, without needing an instance: a {@link dev.starfall.capture.Scene} is asked this before it is created. */
    public static float durationOf(Kind kind) {
        return switch (kind) {
            case REACH -> REACH_DURATION;
            case EXTREME -> EXTREME_DURATION;
            case PARRY -> SwingAnimation.DURATION;
        };
    }

    public float duration() {
        return durationOf(kind);
    }

    /** True when the base pose under the IK is the swing clip rather than bind. */
    public boolean animated() {
        return kind == Kind.PARRY;
    }

    public String description() {
        return switch (kind) {
            case REACH -> "IK: sword hand tracks a slow orbit through the reach boundary, the chain axis and behind the shoulder";
            case EXTREME -> "IK extreme (STYLE.md 7.2): direction reversals, two teleports, driven far out of reach, snapped back";
            case PARRY -> "IK parry: weight ramp from the swing into a raised guard and back out, 40/15/45 of 2.4s";
        };
    }

    /** Reach in world units, so a scene can frame the workspace rather than guess at it. */
    public float armReach() {
        return reach;
    }

    public float shoulderX() {
        return shoulderX;
    }

    public float shoulderY() {
        return shoulderY;
    }

    /** Recomputes every output field for time {@code t}. Pure; call as often as you like. */
    public void sample(float t) {
        switch (kind) {
            case REACH -> reach(t);
            case EXTREME -> extreme(t);
            case PARRY -> parry(t);
        }
    }

    // -- ik-reach --------------------------------------------------------------

    private void reach(float t) {
        float u = clamp01(t / REACH_DURATION);
        // Slightly more than one revolution, at a constant 80 deg/s, starting at
        // the angle the bind pose already holds the hand at so frame 0 is the
        // figure at rest rather than a lurch out of it. Constant rate rather than
        // eased: this scene is about the solver, and an eased orbit would hide a
        // discontinuity behind its own slow patches.
        float theta = -70f + 400f * u;
        // Two full breaths of radius over the clip, centred just inside the reach
        // boundary and swinging 40% of reach either side of it. That crosses the
        // boundary four times -- out at 1.26x reach, back in to 0.46x -- so the
        // soft extension limit is exercised in both directions and at four
        // different orbit angles rather than once at a convenient one. The inner
        // end stops at 0.46 rather than folding further: below about 0.42 the
        // elbow closes past 130 degrees and the sleeve starts to candy-wrap at the
        // joint, which is a skinning fault (STYLE.md 7.2) rather than a solver one
        // and would be blamed on the wrong system.
        float rf = 0.86f + 0.40f * (float) Math.sin(2f * Math.PI * 2f * u + 0.0751f);
        polar(theta, rf);
        armWeight = 1f;

        // The trunk follows the reach a little, the way a body does: this is what
        // the FABRIK chain is for, and at 0.30 it is a lean rather than a re-pose.
        leanToward(0.11f);
        spineWeight = 0.30f;
        legWeight = 1f;
    }

    // -- ik-extreme ------------------------------------------------------------

    private void extreme(float t) {
        float theta;
        float rf;
        if (t < 0.55f) {
            float s = t / 0.55f;
            theta = lerp(-70f, 40f, s);
            rf = lerp(0.89f, 0.93f, s);
        } else if (t < 1.05f) {
            // Reversal one. The target's angular velocity changes sign in a single
            // frame, from +200 deg/s to -380 deg/s.
            float s = (t - 0.55f) / 0.50f;
            theta = lerp(40f, -150f, s);
            rf = lerp(0.93f, 0.86f, s);
        } else if (t < 1.35f) {
            // Reversal two, faster and shorter, before the first has finished
            // settling.
            float s = (t - 1.05f) / 0.30f;
            theta = lerp(-150f, -40f, s);
            rf = lerp(0.86f, 0.90f, s);
        } else if (t < 1.85f) {
            // Teleport one: behind the shoulder and high, arriving as a step.
            theta = 155f;
            rf = 0.80f;
        } else if (t < 2.35f) {
            // Teleport two, and the interesting one -- it lands on the far side of
            // the arm's own axis, which is the configuration the bend-side filter
            // exists for.
            theta = -25f;
            rf = 0.53f;
        } else if (t < 2.95f) {
            // Driven far out of reach: 0.53x to 2.85x the arm's length. The limb
            // should ease asymptotically into extension and then simply point,
            // never straightening with a click and never stretching.
            //
            // 2.95 rather than the round 2.90, so that a twelve-frame capture --
            // which samples at 0.3625 s intervals once the harness has rounded the
            // interval to a whole number of substeps -- lands frame 8 inside this
            // phase rather than exactly on its boundary. A frame sitting on a
            // discontinuity is ambiguous evidence, and this is the one frame in
            // the sheet that shows the fully extended reach.
            float s = (t - 2.35f) / 0.60f;
            theta = lerp(-25f, 10f, s);
            rf = lerp(0.53f, 2.85f, s);
        } else {
            // Snapped back to a close hold, with 1.1 s of nothing after it so the
            // terminal settle is on its own in the last three frames of the sheet:
            // one soft return and then stillness, or the scene has failed.
            //
            // Almost purely radial -- 2.85x reach to 0.42x, but only 25 degrees of
            // angle -- and that is deliberate. Snapping back across a large angle
            // as well rotates the aim far enough to put the elbow on the wrong
            // side of it, which commits a bend flip, and a flip is a 0.6 s sweep
            // of the hand out through full extension and back. It is a perfectly
            // graceful sweep, it is what the solver is supposed to do, and it
            // completely buries the terminal settle this phase exists to show:
            // measured, the hand reached within 0.06 of the target, then left
            // again by 0.32, then came back. The flip belongs at the second
            // teleport above, where it has the frame to itself.
            theta = -15f;
            rf = 0.42f;
        }
        polar(theta, rf);
        armWeight = 1f;

        leanToward(0.10f);
        spineWeight = 0.25f;
        legWeight = 1f;
    }

    // -- ik-parry --------------------------------------------------------------

    private void parry(float t) {
        float w;
        if (t < PARRY_IN) {
            w = smoothstep(t / PARRY_IN);
        } else if (t < PARRY_HOLD_END) {
            w = 1f;
        } else {
            w = 1f - smoothstep((t - PARRY_HOLD_END) / PARRY_OUT);
        }
        armWeight = w;

        // One smooth arc across the gesture, independent of the ramp: the guard
        // comes up high and near and gives ground forward and down as it is
        // pressed. STYLE.md 7.2 -- "the defender's arm gives ground on an IK curve
        // rather than stopping dead". Because the target never jumps, anything
        // that looks like a jump in the capture is the weight blend, which is the
        // thing under test.
        float s = smoothstep(clamp01(t / PARRY_SPAN));
        polar(lerp(70f, 34f, s), lerp(0.76f, 0.95f, s));

        // The trunk yields with the arm, and by less: a parry is absorbed, not
        // blocked. Weighted by the same ramp so the whole gesture arrives and
        // leaves as one thing.
        spineLocalX = neckLocalX - 0.10f * reach;
        spineLocalY = neckLocalY;
        spineWeight = 0.35f * w;

        // The feet stay planted for the entire clip, including through the swing's
        // own hip drive. That is the whole reason to wire the legs: rig-swing
        // translates the hips 0.10 forward and the feet go with them, and here
        // they do not.
        legWeight = 1f;
    }

    // -- helpers ---------------------------------------------------------------

    /** Places the arm target at {@code rFraction} of arm reach, {@code deg} around the bind shoulder. */
    private void polar(float deg, float rFraction) {
        double rad = Math.toRadians(deg);
        float r = rFraction * reach;
        armX = shoulderX + r * (float) Math.cos(rad);
        armY = shoulderY + r * (float) Math.sin(rad);
    }

    /** Trunk target: the resting neck offset, nudged {@code amount} of arm reach toward wherever the hand is going. */
    private void leanToward(float amount) {
        float dx = armX - shoulderX;
        float dy = armY - shoulderY;
        float d = (float) Math.hypot(dx, dy);
        if (d > 1e-6f) {
            spineLocalX = neckLocalX + amount * reach * dx / d;
            spineLocalY = neckLocalY + amount * reach * dy / d;
        } else {
            spineLocalX = neckLocalX;
            spineLocalY = neckLocalY;
        }
    }

    private static float lerp(float a, float b, float s) {
        return a + (b - a) * clamp01(s);
    }

    private static float smoothstep(float x) {
        float c = clamp01(x);
        return c * c * (3f - 2f * c);
    }

    private static float clamp01(float x) {
        return x < 0f ? 0f : (x > 1f ? 1f : x);
    }
}
