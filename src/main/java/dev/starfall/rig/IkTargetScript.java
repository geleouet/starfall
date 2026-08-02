package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;

/**
 * The IK capture scenes' motion, as one deterministic function of time.
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
 *
 * <h2>The body drive -- STYLE.md 7.0's first positive</h2>
 *
 * <p>System 2's pass-2 review measured the hip moving 2 px and the head 4 px
 * across four seconds in which the hand travelled 130 px, and failed the pass on
 * it: "the movement has no source; it begins at the hand." Everything the
 * solver did was correct and none of it was motion.
 *
 * <p>So alongside the hand curve -- which is <em>unchanged</em> for
 * {@code ik-reach}, {@code ik-extreme} and {@code ik-parry}, because those three
 * are the regression fixtures and their measurements are the baseline -- this
 * class now also emits a trunk pose: a pelvis that shifts and drops, a ribcage
 * that counters it, and a head that arrives late. Three properties make it a
 * source rather than a decoration:
 *
 * <ul>
 *   <li><b>It leads.</b> The pelvis is driven from the hand curve sampled
 *       {@link #LEAD_HIP} seconds in the <em>future</em>, the trunk target from
 *       {@link #LEAD_SPINE}, and the head from {@link #LAG_HEAD} seconds in the
 *       past. A time shift is closed-form, so this costs the determinism
 *       guarantee nothing, and it means the hip is already moving four frames
 *       before the hand does -- including four frames before a teleport, which
 *       is 7.1's long anticipation arriving for free.</li>
 *   <li><b>It is smoothed, and the hand is not.</b> {@code ik-extreme}'s
 *       teleports are step discontinuities on purpose. A pelvis that stepped
 *       with them would be a pop, so every body channel reads the hand curve
 *       through a triangular window ({@link #smoothDrive}) rather than at a
 *       point. The hand still teleports; the body ramps into and out of it.</li>
 *   <li><b>It is measured from rest, not from the origin.</b> The bind pose's
 *       own hand sits at 0.996 of arm reach, so an absolute extension term would
 *       read "fully extended" with the figure standing still. The drive is the
 *       hand's displacement from where the bind pose already holds it, which is
 *       zero at rest by construction.</li>
 * </ul>
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
        PARRY,
        /**
         * The aesthetic scene, and the only one authored against the reference
         * corpus rather than against the solver.
         *
         * <p>The other three ask the solver hard questions and are graded on
         * whether it survives them. This one asks it an easy question and is
         * graded on STYLE.md 11's one-sentence test -- could the frame be cropped
         * out of one of the eight references. So there are no teleports, no
         * reversals and nothing out of reach: just a gesture gathered and
         * released inside the band every reference figure lives in.
         *
         * <p>Its targets are not authored as positions at all. They are authored
         * as <em>poses</em> -- an upper-arm angle and an elbow angle -- and
         * converted to a hand position by {@link #armPose}, because the corpus
         * constraint is a statement about angles: in all eight references the
         * elbow is bent 90-130 degrees, the upper arm hangs within about 25
         * degrees of the torso axis, and the blade does the reaching. Authoring
         * the target in pose space makes that constraint hold by construction
         * instead of by iteration.
         */
        GESTURE
    }

    private static final float REACH_DURATION = 5f;
    private static final float EXTREME_DURATION = 4f;
    private static final float GESTURE_DURATION = 3.6f;

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

    // -- the propagation delays of STYLE.md 7.0 point 1 and 7.1 ---------------
    //
    // Positive is a lead: the channel reads the hand curve from the future and
    // therefore moves first. Negative is a lag. Ordered hip -> spine -> (clavicle,
    // which RigIk gives its own filter) -> hand -> head -> blade tip, which is the
    // spiral every reference figure is built on read from the ground up.
    //
    // 0.075 s is four and a half frames at 60 Hz, which is what the review asked
    // for: "the moment the hip leads the hand by about four frames, the same
    // targets will read completely differently."

    private static final float LEAD_HIP = 0.075f;
    private static final float LEAD_SPINE = 0.045f;
    private static final float LAG_HEAD = -0.090f;
    private static final float LAG_BLADE = -0.130f;

    /**
     * Half-width of the triangular window every body channel reads the hand curve
     * through. 0.11 s is about seven frames: wide enough to turn ik-extreme's
     * teleports into a ramp the pelvis can follow, narrow enough that the body
     * still reads as responding to the hand rather than to its average.
     */
    private static final float DRIVE_WINDOW = 0.11f;
    private static final int DRIVE_TAPS = 6; // 2*TAPS+1 samples across the window

    public final Kind kind;

    private final float shoulderX, shoulderY;
    /** Upper arm + forearm, measured off the bind pose. The radius unit for every target below. */
    private final float reach;
    private final float upperLen, lowerLen;
    /** Neck origin in hips-local space at bind: where the trunk chain's effector rests. */
    private final float neckLocalX, neckLocalY;
    /** Where the bind pose already holds the hand, as a fraction of reach off the shoulder. The drive's zero. */
    private final float restDriveX, restDriveY;

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

    /** Pelvis translation, added to whatever the animation put on {@code hips}. */
    public float hipDx, hipDy;
    /** Pelvis rotation, degrees. */
    public float hipRotDeg;
    /** Ribcage counter-rotation, degrees, opposing {@link #hipRotDeg}. */
    public float chestDeg;
    /** Neck and skull, driven from a lagged sample so the head arrives after the hand. */
    public float neckDeg, headDeg;
    /** Blade follow-through: the tip drifts a beat after the hand has stopped. */
    public float bladeDeg;

    /**
     * Whether this scene wants an explicit elbow pole, and where, in the chest's
     * frame. Only {@code ik-gesture} does -- see {@link RigIk#armPoleFromChest}.
     * The three solver scenes keep the pose-derived default so their measurements
     * stay comparable with the pass-2 baseline.
     */
    public boolean armPoleSet;
    public float armPoleLocalX, armPoleLocalY;

    /**
     * Below and behind the shoulder in the chest's own frame: the shoulder sits at
     * chest-local (0.10, 0.11) and this is one upper-arm length straight down the
     * torso axis from it, which is where every reference figure's elbow is.
     */
    private static final float ARM_POLE_LOCAL_X = 0.10f;
    private static final float ARM_POLE_LOCAL_Y = -0.19f;

    private final Vector2 tmp = new Vector2();
    private final Vector2 drive = new Vector2();
    private final Vector2 driveLag = new Vector2();

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
        this.upperLen = (float) Math.hypot(ex - ux, ey - uy);
        bindSkeleton.worldPosition(bindSkeleton.bone("handL").index, v);
        this.lowerLen = (float) Math.hypot(v.x - ex, v.y - ey);
        this.reach = upperLen + lowerLen;
        // The bind hand sits at 0.996 of reach, which is why the drive has to be
        // measured from here rather than from the shoulder: an absolute extension
        // term would read the standing figure as fully extended.
        this.restDriveX = (v.x - ux) / reach;
        this.restDriveY = (v.y - uy) / reach;

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
            case GESTURE -> GESTURE_DURATION;
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
            case GESTURE -> "IK gesture (STYLE.md 7.0): corpus poses -- elbow 90-130, upper arm on the torso axis, hip leads, blade reaches";
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
        // The hand: exactly the curve each scene was authored with. ik-reach,
        // ik-extreme and ik-parry are regression fixtures and this line is what
        // keeps them comparable with the pass-2 measurements.
        armTargetAt(t, tmp);
        armX = tmp.x;
        armY = tmp.y;

        switch (kind) {
            case REACH -> {
                armWeight = 1f;
                spineWeight = 0.30f;
            }
            case EXTREME -> {
                armWeight = 1f;
                spineWeight = 0.25f;
            }
            case GESTURE -> {
                armWeight = 1f;
                // Higher than the solver scenes'. This is the one that has to look
                // like a body, and the trunk is where a body's motion starts.
                spineWeight = 0.42f;
            }
            case PARRY -> {
                armWeight = parryWeight(t);
                spineWeight = 0.35f * armWeight;
            }
        }
        legWeight = 1f;
        armPoleSet = kind == Kind.GESTURE;
        armPoleLocalX = ARM_POLE_LOCAL_X;
        armPoleLocalY = ARM_POLE_LOCAL_Y;

        body(t);
        trunkTarget(t);
    }

    // -- the hand curve --------------------------------------------------------

    /** The scripted hand target at any time, with no body drive and no smoothing. */
    private void armTargetAt(float t, Vector2 out) {
        switch (kind) {
            case REACH -> reach(t, out);
            case EXTREME -> extreme(t, out);
            case PARRY -> parry(t, out);
            case GESTURE -> gesture(t, out);
        }
    }

    // -- ik-reach --------------------------------------------------------------

    private void reach(float t, Vector2 out) {
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
        polar(theta, rf, out);
    }

    // -- ik-extreme ------------------------------------------------------------

    private void extreme(float t, Vector2 out) {
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
        polar(theta, rf, out);
    }

    // -- ik-parry --------------------------------------------------------------

    private static float parryWeight(float t) {
        if (t < PARRY_IN) {
            return smoothstep(t / PARRY_IN);
        }
        if (t < PARRY_HOLD_END) {
            return 1f;
        }
        return 1f - smoothstep((t - PARRY_HOLD_END) / PARRY_OUT);
    }

    private void parry(float t, Vector2 out) {
        // One smooth arc across the gesture, independent of the ramp: the guard
        // comes up high and near and gives ground forward and down as it is
        // pressed. STYLE.md 7.2 -- "the defender's arm gives ground on an IK curve
        // rather than stopping dead". Because the target never jumps, anything
        // that looks like a jump in the capture is the weight blend, which is the
        // thing under test.
        float s = smoothstep(clamp01(t / PARRY_SPAN));
        polar(lerp(70f, 34f, s), lerp(0.76f, 0.95f, s), out);
    }

    // -- ik-gesture ------------------------------------------------------------

    /**
     * Five poses read off the corpus, joined by eased segments of deliberately
     * unequal length so nothing in the clip is metronomic.
     *
     * <p>Columns are (segment end, upper-arm world angle, elbow interior angle).
     * The torso's own downward axis is -100 degrees at bind -- the spine carries a
     * 10 degree forward lean -- so every upper-arm angle here sits within 22
     * degrees of it. Reading down the elbow column is the whole point of the
     * scene: the limb gathers, releases and recovers, and never once approaches
     * the 176 degrees that {@code ik-extreme} used to hold for four consecutive
     * frames.
     *
     * <p>The authored angles run a few degrees wider than the 90-130 the corpus
     * measures at, and that is a correction rather than a drift. The chain's own
     * 0.34 s settle means the hand is always a little behind its target, so a
     * pose authored at the edge of the band is <em>solved</em> outside it: keys at
     * 94 and 124 measured 87 and 117 on the rig. Authoring at 100 and 128 lands
     * the solved pose where the references are, which is what the scene is graded
     * on.
     */
    private static final float[][] GESTURE_KEYS = {
            //  t     upperArmDeg  interiorDeg
            {0.00f, -96f, 116f},   // rest: chudan, hand forward at the waist
            {0.85f, -82f, 108f},   // gather: elbow closes, hand rises and comes in
            {1.60f, -110f, 130f},  // release: the upper arm settles onto the torso axis
            {2.35f, -90f, 114f},   // recovery, which is longer than the release
            {3.20f, -96f, 116f},   // back to rest, then held for the terminal settle
    };

    private void gesture(float t, Vector2 out) {
        float[] a = GESTURE_KEYS[0];
        float[] b = GESTURE_KEYS[GESTURE_KEYS.length - 1];
        for (int i = 0; i + 1 < GESTURE_KEYS.length; i++) {
            if (t < GESTURE_KEYS[i + 1][0] || i + 2 == GESTURE_KEYS.length) {
                a = GESTURE_KEYS[i];
                b = GESTURE_KEYS[i + 1];
                break;
            }
        }
        float span = Math.max(1e-4f, b[0] - a[0]);
        float s = smoothstep(clamp01((t - a[0]) / span));
        armPose(lerp(a[1], b[1], s), lerp(a[2], b[2], s), out);
    }

    /**
     * A hand position expressed as the pose that produces it: where the upper arm
     * points, and how far the elbow is open.
     *
     * <p>The law of cosines gives the hand's distance from the shoulder directly
     * from the elbow angle, and the angle between the upper arm and the line to
     * the hand follows from the same triangle -- so an authored pose maps to a
     * target with no iteration. The elbow is placed on the negative bend side,
     * which is the one the corpus uses: upper arm hanging, elbow tucked under and
     * slightly behind, forearm swinging forward. That is also the side the rig's
     * own bind pose implies for a target in this band, so the pose-derived pole
     * commits to it on frame zero without a flip.
     */
    private void armPose(float upperArmDeg, float interiorDeg, Vector2 out) {
        double a = Math.toRadians(interiorDeg);
        float d = (float) Math.sqrt(Math.max(0f,
                upperLen * upperLen + lowerLen * lowerLen - 2f * upperLen * lowerLen * Math.cos(a)));
        double j = Math.toRadians(180f - interiorDeg);
        float phi = (float) Math.toDegrees(Math.atan2(lowerLen * Math.sin(j), upperLen + lowerLen * Math.cos(j)));
        polar(upperArmDeg + phi, d / reach, out);
    }

    // -- the body drive --------------------------------------------------------

    /**
     * Reads the hand curve through a triangular window centred {@code lead}
     * seconds away and returns the result as a displacement from the bind pose's
     * own hand, in units of arm reach.
     *
     * <p>The window is what lets a stepped hand curve drive a continuous body. A
     * triangular kernel rather than a box because a box is only C0 -- the pelvis
     * would acquire a velocity step where a teleport entered and left the window,
     * which is precisely the pop this exists to avoid.
     */
    private void smoothDrive(float t, float lead, Vector2 out) {
        float duration = duration();
        float sx = 0f;
        float sy = 0f;
        float wsum = 0f;
        for (int i = -DRIVE_TAPS; i <= DRIVE_TAPS; i++) {
            float offset = DRIVE_WINDOW * i / DRIVE_TAPS;
            float tt = clamp(t + lead + offset, 0f, duration);
            float w = 1f - Math.abs(i) / (float) (DRIVE_TAPS + 1);
            armTargetAt(tt, tmp);
            sx += w * tmp.x;
            sy += w * tmp.y;
            wsum += w;
        }
        float px = sx / wsum;
        float py = sy / wsum;
        out.set(((px - shoulderX) / reach - restDriveX) * driveGain(),
                ((py - shoulderY) / reach - restDriveY) * driveGain());
        // Soft magnitude ceiling. ik-extreme drives the hand to 2.85x reach and a
        // body scaled linearly off that would tear itself off its own feet; the
        // ceiling is soft rather than a clamp for the usual reason -- a hard one
        // would give the pelvis an infinite derivative wherever the demand crossed
        // it, which is the arm-hits-end-of-travel pop moved to the hips.
        float m = out.len();
        if (m > 1e-5f) {
            out.scl(softCeil(m, 0.85f, 1.40f) / m);
        }
    }

    /**
     * The trunk pose for this frame. Coefficients are small on purpose: this is a
     * weight shift and a breath, not a second animation. Between them they move
     * the hip ink centroid about 15 px and the head about 25 px over
     * {@code ik-extreme}, against the 2 px and 4 px the pass-2 review measured.
     */
    private void body(float t) {
        float amp = bodyAmplitude(t);
        if (amp <= 0f) {
            hipDx = hipDy = hipRotDeg = chestDeg = neckDeg = headDeg = bladeDeg = 0f;
            return;
        }

        smoothDrive(t, LEAD_HIP, drive);
        float gx = drive.x;
        float gy = drive.y;
        float mag = drive.len();

        // Pelvis: shifts toward the reach and sinks into it. The sink is never
        // zero -- a standing figure carries its weight low, and the rig's bind
        // legs are dead straight, which is the one pose no reference contains.
        hipDx = amp * 0.036f * gx;
        hipDy = amp * (-0.020f - 0.014f * mag);
        // Leaning into a forward reach and away from one behind. Kept under four
        // degrees because rotating the hips carries both thighs with it, and both
        // legs are IK-planted at very nearly full extension.
        hipRotDeg = amp * (-3.2f * gx + 1.6f * gy);
        // The ribcage counters the pelvis, at about two thirds of it. This is the
        // spiral the review asked for, in the only two dimensions a profile view
        // has: the pelvis goes under and the chest comes back over it.
        chestDeg = amp * (2.2f * gx - 1.2f * gy);

        smoothDrive(t, LAG_HEAD, driveLag);
        // The head tracks the hand and arrives last. Negative neck rotation
        // carries the skull forward, so a hand out in front pulls the head after
        // it; the skull's own rotation is the gaze, which follows the hand up and
        // down far more than it follows it fore and aft.
        neckDeg = amp * (-4.0f * driveLag.x);
        headDeg = amp * (-2.5f * driveLag.x + 6.0f * driveLag.y);

        // The blade tip drifts a beat after the hand has stopped: the angle
        // between where the hand is pointing and where it was pointing, which is
        // identically zero the moment the hand comes to rest and so needs no
        // decay term of its own.
        smoothDrive(t, LAG_BLADE, driveLag);
        float now = (float) Math.toDegrees(Math.atan2(drive.y, drive.x));
        float then = (float) Math.toDegrees(Math.atan2(driveLag.y, driveLag.x));
        float trail = shortestArc(now, then);
        bladeDeg = amp * Math.signum(trail) * softCeil(Math.abs(0.34f * trail), 12f, 20f);
    }

    /**
     * How much trunk motion this scene wants. The parry scales with its own weight
     * ramp because the swing clip underneath it is already driving the hips, and
     * two uncoordinated hip drives on one figure is a stagger rather than a spiral.
     */
    private float bodyAmplitude(float t) {
        return kind == Kind.PARRY ? 0.8f * parryWeight(t) : 1f;
    }

    /**
     * How much of a departure from rest counts as a full-scale demand on the body,
     * per scene. It is an <em>input</em> gain, applied before the soft magnitude
     * ceiling, so raising it makes a small gesture read as effort without letting a
     * large one pull the figure off its feet.
     *
     * <p>{@code ik-gesture} needs 3.2 and the reason is worth writing down. The
     * drive is a displacement from where the bind pose holds the hand, and
     * ik-gesture deliberately never leaves the corpus band -- its whole hand path
     * is 0.26 of arm reach, against the 2.85 that ik-extreme reaches. At unit gain
     * the pelvis therefore moved 1.7 px across the clip, which is the pass-2
     * failure reproduced in the one scene written to answer it. The solver scenes
     * keep unit gain because their demand is already enormous.
     */
    private float driveGain() {
        return switch (kind) {
            case GESTURE -> 3.2f;
            case PARRY -> 1.6f;
            case REACH, EXTREME -> 1f;
        };
    }

    /** Trunk chain target: the resting neck offset, nudged toward where the hand is <em>going</em>. */
    private void trunkTarget(float t) {
        float amount = switch (kind) {
            case REACH -> 0.11f;
            case EXTREME -> 0.10f;
            case GESTURE -> 0.13f;
            case PARRY -> 0f;
        };
        if (kind == Kind.PARRY) {
            // The trunk yields with the arm, and by less: a parry is absorbed, not
            // blocked.
            spineLocalX = neckLocalX - 0.10f * reach;
            spineLocalY = neckLocalY;
            return;
        }
        // Read at LEAD_SPINE rather than at t, so the trunk is already leaning
        // while the hand is still on its way. Same smoothing as the pelvis, one
        // stage behind it.
        smoothDrive(t, LEAD_SPINE, driveLag);
        float d = driveLag.len();
        float dirX = restDriveX;
        float dirY = restDriveY;
        if (d > 1e-6f) {
            dirX = driveLag.x / d;
            dirY = driveLag.y / d;
        }
        float k = amount * Math.min(1f, d);
        spineLocalX = neckLocalX + k * reach * dirX;
        // The vertical component is held to a third. A trunk chain asked to put
        // its neck higher than the spine is long cannot comply and answers by
        // straightening, which throws away the 10 degree forward lean the rig
        // authors into the spine -- and that lean is most of what gives the
        // standing figure intent. Fore and aft it can genuinely bend, so that
        // component is passed through whole.
        spineLocalY = neckLocalY + k * reach * dirY * 0.35f;
    }

    /**
     * Writes the trunk pose onto the skeleton, on top of whatever the animation
     * put there. Call after the base pose and before any chain solves; the caller
     * refreshes world transforms.
     *
     * <p>Static-shaped rather than owned by {@link IkSceneDriver} because the
     * measurement tests drive the same scripts without a rig, and a body drive
     * that existed only in the capture path would mean the tests were measuring a
     * different scene from the one being graded.
     */
    public void applyBody(Skeleton skeleton) {
        Bone hips = skeleton.bone("hips");
        hips.x += hipDx;
        hips.y += hipDy;
        hips.rotDeg += hipRotDeg;
        skeleton.bone("chest").rotDeg += chestDeg;
        skeleton.bone("neck").rotDeg += neckDeg;
        skeleton.bone("head").rotDeg += headDeg;
        skeleton.bone("blade").rotDeg += bladeDeg;
    }

    // -- helpers ---------------------------------------------------------------

    /** Places a target at {@code rFraction} of arm reach, {@code deg} around the bind shoulder. */
    private void polar(float deg, float rFraction, Vector2 out) {
        double rad = Math.toRadians(deg);
        float r = rFraction * reach;
        out.set(shoulderX + r * (float) Math.cos(rad), shoulderY + r * (float) Math.sin(rad));
    }

    /** Monotone C1 ceiling: identity up to {@code knee}, then asymptotic to {@code limit}. */
    private static float softCeil(float v, float knee, float limit) {
        float band = limit - knee;
        if (band <= 1e-6f) {
            return Math.min(v, limit);
        }
        if (v <= knee) {
            return v;
        }
        return limit - band * (float) Math.exp(-(v - knee) / band);
    }

    private static float shortestArc(float a, float b) {
        float d = (b - a) % 360f;
        if (d > 180f) {
            d -= 360f;
        } else if (d < -180f) {
            d += 360f;
        }
        return d;
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

    private static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : (x > hi ? hi : x);
    }
}
