package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;

/**
 * The motion of the System 3 capture scenes, as one deterministic function of
 * time.
 *
 * <p>Same shape and same discipline as {@link IkTargetScript}: closed form, no
 * integration, no state, no {@code Math.random}, trig through {@link Math}
 * rather than {@code MathUtils}. It is a separate class rather than two more
 * entries in that enum because {@code SceneRegistry} registers an {@code ik-}
 * scene for every {@link IkTargetScript.Kind}, and hanging cloth scenes off that
 * enum would silently mint {@code ik-*} scenes that have nothing to do with IK.
 *
 * <h2>Everything is one master curve, read at different times</h2>
 *
 * <p>STYLE.md 7.0's first positive is that motion must have a source, and its
 * third is that nothing may arrive at the same time. Both fall out of a single
 * device here: there is exactly one authored curve, {@link #drive}, and every
 * body channel reads it at its own time offset -- the pelvis from the future,
 * the chest from slightly less of the future, the head from the past. So the hip
 * is always already moving before the shoulder, which is already moving before
 * the head, and the hair and cloth then arrive after all three because they are
 * simulated rather than driven. The spiral is not decorated on afterwards; it is
 * the only thing in the file.
 *
 * <h2>The two scenes</h2>
 *
 * <p>{@link Kind#SWAY} is the picture. Nothing sudden: a weight shift gathered
 * and released with one smooth reversal in the middle, under a steady breeze. It
 * is graded on STYLE.md 11.0's matched-scale comparison and on the one-sentence
 * test.
 *
 * <p>{@link Kind#EXTREME} is STYLE.md 7.2's list, and the brief is explicit that
 * fast motion is where the dreamlike quality is easiest to lose. Two hard
 * direction reversals 0.2 s apart, then an impact: a knockback that <em>carries
 * the figure 0.55 units backward over 0.8 s</em> -- 7.2's "drift, not a launch"
 * -- with a wind gust so cloth and hair stream ahead of the body's arrival, and
 * a held breath rather than a freeze. Then 1.7 s of nothing, so the terminal
 * settle is on its own and can be checked for the single soft return 7.2 allows.
 */
public final class SimScript {

    public enum Kind {
        /** The aesthetic scene: a weight shift with one smooth reversal, under a steady breeze. */
        SWAY,
        /** STYLE.md 7.2: two hard reversals, an impact, a knockback drift, and a long settle. */
        EXTREME,
        /**
         * STYLE.md 7.2's overshoot test, and nothing else: <b>one impulse, with
         * every ambient input switched off</b>.
         *
         * <p>This scene exists because the pass-1 review refused to grade the
         * one it was offered, and was right to. Two things were wrong with
         * measuring "at most one soft return" on {@link #EXTREME}:
         *
         * <ul>
         *   <li>the knockback drives in the <em>same direction</em> as the steady
         *       breeze, and the breeze is never switched off, so the response and
         *       the driver are collinear and inseparable;</li>
         *   <li>the 60 Hz window that was delivered is pure acceleration -- every
         *       tracked region's speed rises monotonically to the last frame --
         *       so there is no arrival, no settle and no return in it at all.</li>
         * </ul>
         *
         * <p>7.2 now says so in as many words: "Kill the ambient input, apply one
         * impulse, and count the returns." Here the figure stands still under
         * dead air until t = 1.0, takes one strike, and is then left alone for
         * three seconds. There is no steady breeze, no gust sinusoid and no
         * per-strand turbulence, so every motion in the capture after t = 1.9 is
         * the simulation returning to rest and the count is a question with an
         * answer.
         *
         * <p>It is a <b>measurement rig</b>, and it carries exactly the narrow
         * exemption {@code docs/system2-debt.md} grants {@code ik-extreme}: it is
         * not obliged to answer the one-sentence test as a composition, because
         * dead air is not a condition any reference is painted in. It <b>is</b>
         * obliged never to produce a frame violating 7.2. The exemption is from
         * composition, not from the aesthetic.
         */
        IMPULSE
    }

    private static final float SWAY_DURATION = 4.0f;
    private static final float EXTREME_DURATION = 4.4f;
    private static final float IMPULSE_DURATION = 4.6f;

    // -- propagation delays, STYLE.md 7.0 point 1 and 7.1 --------------------
    //
    // Positive is a lead. Ordered pelvis -> chest -> neck -> head, which is the
    // spiral every reference figure is built on read from the ground up, and
    // then hair and cloth arrive after all of them by simulating rather than by
    // being offset. 0.075 s is four and a half frames at 60 Hz.
    private static final float LEAD_HIP = 0.075f;
    private static final float LEAD_CHEST = 0.038f;
    private static final float LAG_NECK = -0.055f;
    private static final float LAG_HEAD = -0.105f;

    /** Half-width of the triangular window each body channel reads the master curve through. */
    private static final float WINDOW = 0.055f;
    private static final int TAPS = 5;

    public final Kind kind;

    private final float shoulderX;
    private final float shoulderY;
    private final float upperLen;
    private final float lowerLen;
    private final float reach;
    private final float neckLocalX;
    private final float neckLocalY;

    /** Bind ankle positions. Feet are planted at these, carried by {@link #rootDx}. */
    private final float bindFootLX;
    private final float bindFootLY;
    private final float bindFootRX;
    private final float bindFootRY;

    // -- outputs, refreshed by sample() ---------------------------------------

    /** Whole-figure translation. Nonzero only during a knockback. */
    public float rootDx;

    public float hipDx, hipDy, hipRotDeg;
    public float chestDeg, neckDeg, headDeg;

    public float armX, armY, armWeight;
    public float spineLocalX, spineLocalY, spineWeight;
    public float legWeight;
    public float footLX, footLY, footRX, footRY;

    /** The breeze, in world units per second squared, handed straight to {@link RigSim#wind}. */
    public float windX, windY;

    /** STYLE.md 7.3's held breath: a soft time ramp, never a freeze. 1.0 outside an impact. */
    public float timeScale = 1f;

    /**
     * Scales the wind's <em>varying</em> part -- the two gust sinusoids and the
     * impact gust -- leaving the steady breeze alone. 1 is the shipped scene.
     *
     * <p>It exists for the same reason {@code HairSim.turbulenceScale} does, and
     * the distinction matters more here than anywhere else in the system.
     * STYLE.md 7.2's "at most one soft return" is a statement about a system's
     * response to a disturbance. A breeze that keeps changing is not a
     * disturbance, it is a continuous driver, and a settle measured under one
     * counts the air's own reversals as the strand's. Measured with the gusts
     * running, the escapees "rang" two or three times and <em>damping them
     * harder made it worse</em> -- which is the unmistakable signature of
     * measuring a driver rather than a resonance. Setting this to 0 leaves the
     * steady breeze that shapes the hair and removes the thing that keeps
     * pushing it, which is the only condition under which "did it come to rest"
     * is a question with an answer.
     */
    public float airScale = 1f;

    private final Vector2 tmp = new Vector2();

    public SimScript(Kind kind, Skeleton bindSkeleton) {
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
        this.upperLen = (float) Math.hypot(ex - ux, ey - uy);
        bindSkeleton.worldPosition(bindSkeleton.bone("handL").index, v);
        this.lowerLen = (float) Math.hypot(v.x - ex, v.y - ey);
        this.reach = upperLen + lowerLen;

        bindSkeleton.worldPosition(bindSkeleton.bone("hips").index, v);
        float hx = v.x;
        float hy = v.y;
        bindSkeleton.worldPosition(bindSkeleton.bone("neck").index, v);
        this.neckLocalX = v.x - hx;
        this.neckLocalY = v.y - hy;

        bindSkeleton.worldPosition(bindSkeleton.bone("footL").index, v);
        this.bindFootLX = v.x;
        this.bindFootLY = v.y;
        bindSkeleton.worldPosition(bindSkeleton.bone("footR").index, v);
        this.bindFootRX = v.x;
        this.bindFootRY = v.y;

        sample(0f);
    }

    public static float durationOf(Kind kind) {
        return switch (kind) {
            case SWAY -> SWAY_DURATION;
            case EXTREME -> EXTREME_DURATION;
            case IMPULSE -> IMPULSE_DURATION;
        };
    }

    /**
     * How much of each strand's own turbulence this scene runs. 1 everywhere
     * except {@link Kind#IMPULSE}, which is 0.
     *
     * <p>The driver applies it at construction, so a scene's ambient policy is a
     * property of the scene rather than something a caller has to remember --
     * and so a measurement that overrides it afterwards still can.
     */
    public float ambientTurbulence() {
        return kind == Kind.IMPULSE ? 0f : 1f;
    }

    public float duration() {
        return durationOf(kind);
    }

    public String description() {
        return switch (kind) {
            case SWAY -> "Verlet hair + cloth: weight shift with one smooth reversal, steady breeze "
                    + "(STYLE.md 4, 7.0, 7.1)";
            case EXTREME -> "Verlet extreme (STYLE.md 7.2): two hard reversals, impact, 0.8s knockback drift, long settle";
            case IMPULSE -> "STYLE.md 7.2 overshoot test: dead air, one impulse at t=1.0, 3s of settle. "
                    + "No breeze, no gusts, no per-strand turbulence";
        };
    }

    // -- the master curve -------------------------------------------------------

    /**
     * The figure's lateral intent, in -1..1. Everything else in this file is a
     * reading of this one function.
     */
    private float drive(float t) {
        if (kind == Kind.IMPULSE) {
            // Flat before the strike, so the bundle is genuinely at rest and the
            // impulse is the only thing that has ever moved it; then one soft
            // give and one slow recovery, which is 7.3's "yielding" and is the
            // only body motion in the scene.
            if (t < IMPULSE_TIME) {
                return 0f;
            }
            // One eased arrival and then nothing. Deliberately monotone: a body
            // that yielded and then straightened would be a *second* impulse in
            // the other direction, and the returns after it would be the script's
            // rather than the solver's -- which is the exact fault this scene
            // exists to remove from the measurement. 7.2: the struck figure ends
            // up somewhere else, and the poetry is in how long it takes.
            return lerp(0f, -0.85f, smoothstep((t - IMPULSE_TIME) / 0.80f));
        }
        if (kind == Kind.SWAY) {
            if (t < 0.60f) {
                return 0f;
            }
            if (t < 1.50f) {
                return smoothstep((t - 0.60f) / 0.90f);
            }
            if (t < 1.90f) {
                return 1f;
            }
            if (t < 2.50f) {
                // The reversal the 60 Hz window is captured around: smooth, but
                // fast enough that the tips are still travelling the old way for
                // a beat after the head has committed to the new one.
                return lerp(1f, -0.85f, smoothstep((t - 1.90f) / 0.60f));
            }
            if (t < 2.90f) {
                return -0.85f;
            }
            return lerp(-0.85f, 0f, smoothstep((t - 2.90f) / 0.90f));
        }

        // EXTREME. Every phase joins the next unsmoothed on purpose: easing the
        // script would be testing the script. The smoothing that exists is the
        // triangular window each body channel reads this through, which is what
        // turns a cornered curve into a body that can follow it.
        if (t < 0.70f) {
            return 0f;
        }
        if (t < 0.95f) {
            return (t - 0.70f) / 0.25f;
        }
        if (t < 1.15f) {
            // Reversal one: +1 to -1 in 0.20 s, i.e. the intent's sign changes
            // ten times faster than the sway scene ever does.
            return lerp(1f, -1f, (t - 0.95f) / 0.20f);
        }
        if (t < 1.35f) {
            // Reversal two, before the first has finished settling.
            return lerp(-1f, 0.9f, (t - 1.15f) / 0.20f);
        }
        if (t < 1.90f) {
            return lerp(0.9f, 0.15f, (t - 1.35f) / 0.55f);
        }
        if (t < 2.70f) {
            // Through the impact the body gives rather than braces.
            return lerp(0.15f, -0.55f, smoothstep((t - 1.90f) / 0.80f));
        }
        return lerp(-0.55f, 0f, smoothstep((t - 2.70f) / 1.10f));
    }

    /**
     * The master curve read {@code lead} seconds away, through a triangular
     * window. Triangular rather than a box because a box is only C0 -- the
     * pelvis would acquire a velocity step where a corner entered and left the
     * window, which is precisely the pop this exists to avoid.
     */
    private float smoothDrive(float t, float lead) {
        float d = duration();
        float sum = 0f;
        float wsum = 0f;
        for (int i = -TAPS; i <= TAPS; i++) {
            float tt = clamp(t + lead + WINDOW * i / TAPS, 0f, d);
            float w = 1f - Math.abs(i) / (float) (TAPS + 1);
            sum += w * drive(tt);
            wsum += w;
        }
        return sum / wsum;
    }

    // -- knockback ---------------------------------------------------------------

    /** When {@link Kind#IMPULSE} strikes. Early, so three seconds of settle fit after it. */
    private static final float IMPULSE_TIME = 1.00f;

    /** Impact time, or a negative number for a scene that has none. */
    private float impactTime() {
        return switch (kind) {
            case EXTREME -> 1.90f;
            case IMPULSE -> IMPULSE_TIME;
            case SWAY -> -1f;
        };
    }

    /**
     * STYLE.md 7.2: "Knockback is a drift, not a launch. A struck figure should
     * be carried backward like a sheet of silk caught in wind, arriving over
     * ~0.8 s." So the profile is a single eased arrival with no overshoot and no
     * return -- the figure ends up somewhere else, which is what being struck
     * means, and the poetry is in how long it takes to get there.
     */
    private float knockback(float t) {
        float t0 = impactTime();
        if (t0 < 0f || t < t0) {
            return 0f;
        }
        return -0.55f * smoothstep((t - t0) / 0.80f);
    }

    // -- sampling ------------------------------------------------------------------

    /** Recomputes every output for time {@code t}. Pure; call as often as you like. */
    public void sample(float t) {
        float gHip = smoothDrive(t, LEAD_HIP);
        float gChest = smoothDrive(t, LEAD_CHEST);
        float gNeck = smoothDrive(t, LAG_NECK);
        float gHead = smoothDrive(t, LAG_HEAD);

        rootDx = knockback(t);

        // The pelvis carries the weight shift and sinks into it. The rig's bind
        // legs are dead straight, which is the one pose no reference contains,
        // so the sink is never zero.
        hipDx = 0.058f * gHip;
        hipDy = -0.018f - 0.014f * Math.abs(gHip);
        // Under four degrees: rotating the hips carries both thighs with it and
        // both legs are IK-planted at very nearly full extension.
        hipRotDeg = -3.6f * gHip;
        // The ribcage counters the pelvis at about two thirds. This is the
        // spiral, in the only two dimensions a profile view has.
        chestDeg = 2.8f * gChest;
        neckDeg = -4.2f * gNeck;
        // The skull turns most and last, which matters more here than in any
        // earlier system: it is the hair's anchor, and every strand's rest
        // direction is measured off it.
        headDeg = -13f * gHead;

        // The arm holds a low guard and drifts. It is not the subject of these
        // scenes and it must not become one, but it also must not go rigid --
        // System 2's debt is explicit that a still limb under a moving body is
        // the failure, not the fix. Authored in pose space so the elbow floor
        // and the upper-arm-on-the-torso-axis property survive by construction.
        float upperArmDeg = -96f + 7f * gChest;
        float interiorDeg = 116f - 9f * gChest;
        armPose(upperArmDeg, interiorDeg, tmp);
        armX = tmp.x + rootDx;
        armY = tmp.y;
        armWeight = 1f;

        spineWeight = 0.38f;
        spineLocalX = neckLocalX + 0.085f * reach * gChest;
        spineLocalY = neckLocalY;

        legWeight = 1f;
        footLX = bindFootLX + rootDx;
        footLY = bindFootLY;
        footRX = bindFootRX + rootDx;
        footRY = bindFootRY;

        wind(t);
        timeScale = heldBreath(t);
    }

    /**
     * The breeze. A steady flow toward -X -- the figure faces +X, so its hair
     * and its hem stream behind it, which is what all three hair references show
     * -- plus two incommensurate gust terms so nothing in the air has a period
     * the eye can lock onto, plus the impact gust.
     */
    private void wind(float t) {
        if (kind == Kind.IMPULSE) {
            // Dead air, and one strike. This is 7.2's overshoot test taken at its
            // word: "Kill the ambient input, apply one impulse, and count the
            // returns." Nothing here is periodic, nothing is steady, and after
            // about t = 2.2 the air is exactly zero, so every pixel that moves
            // in the last two seconds of the capture is the simulation coming to
            // rest.
            //
            // The impulse pushes *back* along the hair's own rest lie rather than
            // across it, for the reason 4b.1 gives: a gust that threw the bundle
            // forward would drape it over the one surface in the figure that is
            // exempt from the ink dissolve.
            windX = 0f;
            windY = 0f;
            float u = t - IMPULSE_TIME;
            if (u >= 0f) {
                float env = u < 0.10f ? u / 0.10f : (float) Math.exp(-(u - 0.10f) / 0.26f);
                windX = -5.2f * env;
                windY = 1.5f * env;
            }
            return;
        }
        // The gust amplitude is a fifth of the base rather than a third, and the
        // number is the outcome of a measurement rather than a taste. The lag
        // requirement of 7.1 is a statement about how long the *body's* motion
        // takes to reach a tip, and an independent driver of the same size as
        // the body's own contribution makes that unmeasurable -- the first
        // tuning measured a correlation lag of 30 frames on the hem simply
        // because the hem was mostly answering the air rather than the hips. The
        // air still moves, and it still has no period the eye can lock onto; it
        // just no longer outvotes the figure.
        float base = -0.95f;
        float gust = 0.15f * (float) Math.sin(2 * Math.PI * 0.13 * t + 0.7)
                + 0.08f * (float) Math.sin(2 * Math.PI * 0.31 * t + 2.9);
        windX = base + gust * airScale;
        windY = airScale * 0.07f * (float) Math.sin(2 * Math.PI * 0.17 * t + 1.4);

        float t0 = impactTime();
        if (t0 >= 0f && t >= t0 - 0.05f) {
            // The gust that makes the cloth and the hair stream *ahead* of the
            // body's arrival, per 7.2. It peaks fast and decays over about a
            // second, so by the time the figure has finished drifting the air is
            // still again and the settle is the sim's own.
            float u = Math.max(0f, t - (t0 - 0.05f));
            float env = u < 0.16f ? u / 0.16f : (float) Math.exp(-(u - 0.16f) / 0.42f);
            windX += -4.4f * env * airScale;
            windY += 1.3f * env * airScale;
        }
    }

    /**
     * STYLE.md 7.3: "a brief slowing of everything (a soft time ramp, ~0.85x for
     * ~0.25 s), never a hard freeze." Explicitly not hitstop -- 7.1 and 10 both
     * ban that -- and it is a ramp in and a ramp out, so nothing steps.
     */
    private float heldBreath(float t) {
        float t0 = impactTime();
        if (t0 < 0f) {
            return 1f;
        }
        float u = t - t0;
        if (u < 0f || u > 0.34f) {
            return 1f;
        }
        float shape = u < 0.06f ? u / 0.06f
                : u < 0.25f ? 1f
                : 1f - (u - 0.25f) / 0.09f;
        return 1f - 0.15f * clamp(shape, 0f, 1f);
    }

    /**
     * A hand position expressed as the pose that produces it. Copied in spirit
     * from {@link IkTargetScript#armPose} and for the same reason: the corpus
     * constraint is a statement about angles (elbow 90-130, upper arm within
     * about 25 degrees of the torso axis, the blade doing the reaching), and
     * authoring in pose space makes it hold by construction rather than by
     * iteration.
     */
    private void armPose(float upperArmDeg, float interiorDeg, Vector2 out) {
        double a = Math.toRadians(interiorDeg);
        float d = (float) Math.sqrt(Math.max(0f,
                upperLen * upperLen + lowerLen * lowerLen - 2f * upperLen * lowerLen * Math.cos(a)));
        double j = Math.toRadians(180f - interiorDeg);
        float phi = (float) Math.toDegrees(Math.atan2(lowerLen * Math.sin(j), upperLen + lowerLen * Math.cos(j)));
        double rad = Math.toRadians(upperArmDeg + phi);
        out.set(shoulderX + d * (float) Math.cos(rad), shoulderY + d * (float) Math.sin(rad));
    }

    /** Writes the body pose onto the skeleton, on top of whatever the animation put there. */
    public void applyBody(Skeleton skeleton) {
        Bone root = skeleton.bone("root");
        root.x += rootDx;
        Bone hips = skeleton.bone("hips");
        hips.x += hipDx;
        hips.y += hipDy;
        hips.rotDeg += hipRotDeg;
        skeleton.bone("chest").rotDeg += chestDeg;
        skeleton.bone("neck").rotDeg += neckDeg;
        skeleton.bone("head").rotDeg += headDeg;
    }

    /** Arm reach in world units, so a scene can frame the workspace rather than guess at it. */
    public float armReach() {
        return reach;
    }

    // -- helpers ---------------------------------------------------------------------

    private static float lerp(float a, float b, float s) {
        return a + (b - a) * clamp(s, 0f, 1f);
    }

    private static float smoothstep(float x) {
        float c = clamp(x, 0f, 1f);
        return c * c * (3f - 2f * c);
    }

    private static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }
}
