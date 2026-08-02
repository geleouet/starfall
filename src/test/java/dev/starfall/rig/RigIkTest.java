package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;
import dev.starfall.ik.IkChain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rig-side half of System 2: chains bound to the real skeleton, driven by the
 * real capture scripts.
 *
 * <p>{@code dev.starfall.ik}'s own tests prove the solvers are continuous on
 * synthetic chains laid along +x. That is necessary and it is not sufficient,
 * because it cannot catch anything about how the chains meet <em>this</em> rig --
 * whose trunk bones stack upward with their axes pointing sideways, whose legs
 * are dead straight at bind, and whose sword arm's animation channel runs past
 * -430 degrees. Every check below is one the synthetic rigs structurally cannot
 * make.
 *
 * <p>The continuity checks are also the measured half of the STYLE.md 7.2 gate.
 * A contact sheet samples twelve frames out of hundreds and a pop between two
 * unsampled frames is invisible in it; these run every frame.
 */
class RigIkTest {

    private static final float DT = 1f / 60f;

    /** Headless: no GL, so no meshes -- see SamuraiRig#buildSkeletonOnly. */
    private static Skeleton rig() {
        return SamuraiRig.buildSkeletonOnly();
    }

    // -- binding ---------------------------------------------------------------

    @Test
    void aTrunkChainHoldingItsOwnPoseDoesNotThrowTheFigureOnTheFloor() {
        // The regression this file exists for. IkChain's write-back turns solved
        // *link* directions into Bone.rotDeg, and for hips/spine/chest those are
        // ninety degrees apart, because the trunk stacks via a +y offset while
        // every bone's own axis points +x. Before the link-offset correction, a
        // spine chain asked to hold the pose it was already in answered
        // hips.rotDeg = 96.8 and put the neck 0.70 units away from where it had
        // just been: switching the chain on destroyed the pose.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        Vector2 neck = s.worldPosition(s.bone("neck").index, new Vector2());

        ik.spine().target(neck.x, neck.y).weight(1f);
        ik.spine().snap();

        assertTrue(Math.abs(s.bone("hips").rotDeg) < 12f,
                "the hips rotated to " + s.bone("hips").rotDeg + " to hold a pose they were already in");
        Vector2 after = s.worldPosition(s.bone("neck").index, new Vector2());
        assertEquals(neck.x, after.x, 0.02f);
        assertEquals(neck.y, after.y, 0.02f);
    }

    @Test
    void theSwordHandReachesTargetsAllRoundTheShoulder() {
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkChain arm = ik.swordArm().weight(1f);
        Vector2 shoulder = s.worldPosition(s.bone("upperArmL").index, new Vector2());
        Vector2 hand = new Vector2();
        for (float deg = 0f; deg < 360f; deg += 15f) {
            // 0.8 of reach: comfortably inside, so a miss means a binding fault
            // rather than the soft extension limit doing its job.
            float r = 0.8f * armReach(s);
            arm.target(shoulder.x + r * (float) Math.cos(Math.toRadians(deg)),
                    shoulder.y + r * (float) Math.sin(Math.toRadians(deg)));
            arm.snap();
            s.worldPosition(s.bone("handL").index, hand);
            assertTrue(hand.dst(arm.endEffector(new Vector2())) < 5e-3f,
                    "the skeleton and the chain disagree about where the hand is, at " + deg);
            assertTrue(arm.residual() < 5e-3f, "hand missed by " + arm.residual() + " at " + deg + " degrees");
        }
    }

    @Test
    void theElbowStaysOnOneSideForAWholeOrbit() {
        // The reason ik-reach sets no pole. The pose-derived default is
        // self-reinforcing, so an orbiting target must never provoke a flip -- and
        // a flip is the one motion in this solver that cannot be made to look
        // like a brushstroke.
        // Driven through the whole rig rather than through the arm chain alone,
        // because as of pass 3 the shoulder is no longer a fixed point: the hips
        // shift, the chest counters and the clavicle rolls, so the aim direction
        // the bend filter sees is measured from a socket that is itself moving.
        // A flip provoked by the body would be invisible to a bare-chain test.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkChain arm = ik.swordArm();
        IkTargetScript script = new IkTargetScript(IkTargetScript.Kind.REACH, rig());
        SwingAnimation swing = new SwingAnimation();

        pose(s, swing, script, 0f);
        aim(ik, script);
        ik.snap();
        float side0 = Math.signum(arm.bendSide());
        for (float t = 0f; t <= script.duration(); t += DT) {
            pose(s, swing, script, t);
            aim(ik, script);
            ik.update(DT);
            assertEquals(side0, Math.signum(arm.bendSide()), 0f,
                    "the elbow changed sides at t=" + t + ", bendSide " + arm.bendSide());
        }
    }

    @Test
    void bothKneesBendForwardRatherThanBackward() {
        // Both legs are dead straight at bind -- hip-to-ankle measures the whole
        // reach to three decimal places -- so the pose-derived pole sits exactly
        // on the chain axis and says nothing. That is why RigIk carries an
        // explicit pole on the hips, and this is the assertion that it points the
        // right way: a knee that bends backwards is instantly, comically wrong and
        // no residual check would notice.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkTargetScript script = new IkTargetScript(IkTargetScript.Kind.PARRY, rig());
        ik.legL().target(script.footLX, script.footLY).weight(1f);
        ik.legR().target(script.footRX, script.footRY).weight(1f);
        ik.snap();

        assertTrue(s.bone("shinL").rotDeg <= 0.5f, "near knee bent backwards: " + s.bone("shinL").rotDeg);
        assertTrue(s.bone("shinR").rotDeg <= 0.5f, "far knee bent backwards: " + s.bone("shinR").rotDeg);
    }

    @Test
    void plantedFeetStayPlantedThroughTheSwingsHipDrive() {
        // rig-swing translates the hips 0.10 forward and 0.045 down and the feet
        // go with them, because nothing holds them. This is the fix, and the point
        // of wiring the legs at all.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        SwingAnimation swing = new SwingAnimation();
        IkTargetScript script = new IkTargetScript(IkTargetScript.Kind.PARRY, rig());

        float worst = 0f;
        Vector2 v = new Vector2();
        for (float t = 0f; t <= SwingAnimation.DURATION; t += DT) {
            applyPose(s, swing, t);
            ik.legL().target(script.footLX, script.footLY).weight(1f);
            ik.legR().target(script.footRX, script.footRY).weight(1f);
            ik.legL().update(DT);
            ik.legR().update(DT);
            s.worldPosition(s.bone("footL").index, v);
            worst = Math.max(worst, v.dst(script.footLX, script.footLY));
            s.worldPosition(s.bone("footR").index, v);
            worst = Math.max(worst, v.dst(script.footRX, script.footRY));
        }
        // 25 mm on a 1.75 m figure. Not zero: the settle filter is allowed to lag
        // a hip that is moving, and near full extension the soft reach limit is
        // allowed to fall a few millimetres short rather than locking straight.
        assertTrue(worst < 0.025f, "a foot slid " + worst + " world units off its plant");
    }

    // -- the STYLE.md 7.2 gate, measured --------------------------------------

    @Test
    void ikReachNeverPopsAnyBoneBetweenFrames() {
        // Measured worst: 7.4 deg/frame, i.e. 440 deg/s, on a limb whose target is
        // crossing the reach boundary four times. Nothing here is near the slew
        // ceiling; the scene is smooth because the solver is, not because it is
        // being caught.
        assertTrue(worstFrameStep(IkTargetScript.Kind.REACH, true) < 9f);
    }

    @Test
    void ikExtremeNeverPopsAnyBoneBetweenFrames() {
        // Two teleports, two hard reversals and a target driven to 2.85x reach.
        // The measured worst is 20.0 deg/frame, which is the 1200 deg/s slew
        // ceiling to two decimal places: the teleports do saturate it, which is
        // the ceiling doing exactly the job STYLE.md 7.2 asks for -- an
        // unavoidable reconfiguration served as a fast sweep of a few frames
        // rather than a single-frame mirror. The assertion is that nothing ever
        // gets past it.
        assertTrue(worstFrameStep(IkTargetScript.Kind.EXTREME, true) < 20.1f);
    }

    @Test
    void ikParryAddsNoMotionOfItsOwnBeyondTheClipItBlendsWith() {
        // A fixed bound would be meaningless here, because the parry scene's
        // fastest frame belongs to the swing: the cut turns the elbow 26 degrees
        // in a frame all by itself. So the check is differential -- run the clip
        // with the chains driven and again with them inert, and require the IK to
        // add nothing. It currently adds nothing at all, to two decimal places.
        float withIk = worstFrameStep(IkTargetScript.Kind.PARRY, true);
        float animationOnly = worstFrameStep(IkTargetScript.Kind.PARRY, false);
        assertTrue(withIk <= animationOnly + 1f,
                "the weight ramp added " + (withIk - animationOnly) + " deg/frame over the clip's own worst step");
    }

    @Test
    void theSettleAfterTheExtremeSnapBackIsOneSoftReturnAndNotAnOscillation() {
        // STYLE.md 7.2: "a spring that visibly oscillates twice and stops reads as
        // a machine. Damping should be high enough that there is at most one soft
        // return." ik-extreme's last phase exists to be looked at for this: the
        // target is snapped from 2.85x reach back to a close hold at t=2.95 and
        // then left alone for 1.1 s. The hand must approach and stop, never having
        // got closer and drifted back out.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkTargetScript script = new IkTargetScript(IkTargetScript.Kind.EXTREME, rig());
        driveTo(s, ik, script, 2.95f);

        float closest = Float.MAX_VALUE;
        float worstRebound = 0f;
        float previous = Float.NaN;
        boolean returning = false;
        SwingAnimation swing = new SwingAnimation();
        for (float t = 2.95f; t <= script.duration(); t += DT) {
            pose(s, swing, script, t);
            aim(ik, script);
            ik.update(DT);
            float d = ik.swordArm().residual();
            // The first frames after the snap are the settle filter reversing:
            // the target had been running outward at 2.4 units/s and a critically
            // damped filter carries that momentum out before it comes back, which
            // is a brush decelerating rather than a spring oscillating -- it never
            // crosses the target, it only takes a moment to turn round. What must
            // be monotone is everything from the turn onward, so the measurement
            // starts there.
            if (!returning) {
                returning = d < previous;
                previous = d;
                if (!returning) {
                    continue;
                }
            }
            closest = Math.min(closest, d);
            worstRebound = Math.max(worstRebound, d - closest);
        }
        assertTrue(returning, "the hand never started returning at all");
        assertTrue(worstRebound < 0.004f, "the hand rebounded " + worstRebound + " world units after settling");
        assertTrue(ik.swordArm().residual() < 0.004f,
                "and it never arrived: residual " + ik.swordArm().residual());
    }

    // -- the STYLE.md 7.0 gate, measured ---------------------------------------

    /**
     * IkScene's camera is ortho with a half-height of 1.5 on a 540 px capture, so
     * one world unit is 180 px. Excursions below are quoted in those pixels
     * because that is the unit the review's acceptance criterion is written in.
     */
    private static final float PX_PER_UNIT = 180f;

    @Test
    void theTorsoMovesDuringIkExtreme() {
        // The acceptance criterion of the whole pass, and the only measurement in
        // this file that is about the aesthetic rather than about continuity.
        //
        // STYLE.md 7.0: "the body's own centroids must move measurably during any
        // limb action. A frozen torso fails regardless of how good the limb is."
        // The pass-2 capture measured a hip ink centroid moving 2 px and a head
        // moving 4 px while the hand travelled 130 px, and the chain root sat at
        // the same pixel in all twenty-four frames.
        //
        // This measures bone origins rather than ink centroids: a centroid is a
        // proxy for the thing that has to move, and a proxy that the sword arm
        // sweeping through the measurement band can drag around by 70 px on its
        // own. The bones are unambiguous, and if they do not move nothing that
        // is skinned to them can.
        float[] hip = new float[2];
        float[] head = new float[2];
        float[] shoulder = new float[2];
        excursions(IkTargetScript.Kind.EXTREME, hip, head, shoulder);

        System.out.println("[RigIkTest] ik-extreme excursion px: hips " + hip[0] + " head " + head[0]
                + " shoulder " + shoulder[0] + " (paths " + hip[1] + " / " + head[1] + " / " + shoulder[1] + ")");
        assertTrue(hip[0] > 8f, "the hips moved " + hip[0] + " px, which is a mannequin");
        assertTrue(head[0] > 12f, "the head moved " + head[0] + " px, which is a mannequin");
        assertTrue(shoulder[0] > 8f, "the arm chain's root moved " + shoulder[0] + " px; it is still pinned");
    }

    @Test
    void theHipLeadsTheHand() {
        // STYLE.md 7.0's first positive is not "the body also moves", it is that
        // the movement has a source. A body that responded to the hand would
        // satisfy the excursion check above and still read as a puppet, so the
        // ordering is measured too: the pelvis's peak speed must come before the
        // hand's, at every peak, not on average.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkTargetScript script = new IkTargetScript(IkTargetScript.Kind.GESTURE, rig());
        SwingAnimation swing = new SwingAnimation();

        int n = (int) (script.duration() / DT);
        float[] hipSpeed = new float[n];
        float[] handSpeed = new float[n];
        Vector2 v = new Vector2();
        float px = 0f, py = 0f, hx = 0f, hy = 0f;
        boolean primed = false;
        for (int i = 0; i < n; i++) {
            float t = i * DT;
            pose(s, swing, script, t);
            aim(ik, script);
            if (!primed) {
                ik.snap();
            } else {
                ik.update(DT);
            }
            s.worldPosition(s.bone("hips").index, v);
            if (primed) {
                hipSpeed[i] = v.dst(px, py);
            }
            px = v.x;
            py = v.y;
            s.worldPosition(s.bone("handL").index, v);
            if (primed) {
                handSpeed[i] = v.dst(hx, hy);
            }
            hx = v.x;
            hy = v.y;
            primed = true;
        }
        int hipPeak = argmax(hipSpeed);
        int handPeak = argmax(handSpeed);
        System.out.println("[RigIkTest] ik-gesture peak frames: hips " + hipPeak + " hand " + handPeak
                + " (lead " + (handPeak - hipPeak) + " frames)");
        assertTrue(handPeak - hipPeak >= 3,
                "the hand peaked " + (handPeak - hipPeak) + " frames after the hips; the review asked for about four");
    }

    @Test
    void ikGestureStaysInsideTheCorpusBand() {
        // What makes ik-gesture a different kind of scene from the other three:
        // it is authored against the reference images rather than against the
        // solver, and the corpus constraint is a statement about angles. In all
        // eight references the elbow is bent 90-130 degrees and the upper arm
        // hangs within about 25 degrees of the torso axis. If a capture of this
        // scene ever leaves that band the scene has stopped being evidence.
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkTargetScript script = new IkTargetScript(IkTargetScript.Kind.GESTURE, rig());
        SwingAnimation swing = new SwingAnimation();

        float worstElbowLo = 180f;
        float worstElbowHi = 0f;
        float worstOffAxis = 0f;
        boolean primed = false;
        Vector2 a = new Vector2();
        Vector2 b = new Vector2();
        Vector2 c = new Vector2();
        for (float t = 0f; t <= script.duration(); t += DT) {
            pose(s, swing, script, t);
            aim(ik, script);
            if (!primed) {
                ik.snap();
                primed = true;
                continue; // the snap frame is the bind pose arriving, not a pose of the clip
            }
            ik.update(DT);

            s.worldPosition(s.bone("upperArmL").index, a);
            s.worldPosition(s.bone("forearmL").index, b);
            s.worldPosition(s.bone("handL").index, c);
            float upperDeg = (float) Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
            float lowerDeg = (float) Math.toDegrees(Math.atan2(c.y - b.y, c.x - b.x));
            float interior = 180f - Math.abs(shortestArc(upperDeg, lowerDeg));
            worstElbowLo = Math.min(worstElbowLo, interior);
            worstElbowHi = Math.max(worstElbowHi, interior);

            // The torso's own downward axis, read off the live spine rather than
            // written down, so this keeps meaning the same thing if the lean is
            // ever retuned. Minus ninety, not minus one eighty: trunk bones stack
            // via a +y offset while their own axes point +x, so the spine's world
            // rotation is the direction across the body, not along it.
            float torsoDeg = s.worldRotationDeg(s.bone("spine").index) - 90f;
            worstOffAxis = Math.max(worstOffAxis, Math.abs(shortestArc(torsoDeg, upperDeg)));
        }
        System.out.println("[RigIkTest] ik-gesture elbow " + worstElbowLo + ".." + worstElbowHi
                + " deg, upper arm off torso axis by up to " + worstOffAxis + " deg");
        assertTrue(worstElbowLo > 88f, "the elbow opened to " + worstElbowLo + ", tighter than any reference");
        assertTrue(worstElbowHi < 132f, "the elbow straightened to " + worstElbowHi + ", which no reference contains");
        assertTrue(worstOffAxis < 27f, "the upper arm left the torso axis by " + worstOffAxis + " degrees");
    }

    /** Peak-to-peak excursion and total path of the hips, head and shoulder over a whole script, in capture pixels. */
    private static void excursions(IkTargetScript.Kind kind, float[] hip, float[] head, float[] shoulder) {
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkTargetScript script = new IkTargetScript(kind, rig());
        SwingAnimation swing = new SwingAnimation();

        String[] watched = {"hips", "head", "upperArmL"};
        float[][] out = {hip, head, shoulder};
        int n = (int) (script.duration() / DT) + 1;
        float[][] xs = new float[watched.length][n];
        float[][] ys = new float[watched.length][n];

        Vector2 v = new Vector2();
        boolean primed = false;
        int f = 0;
        for (float t = 0f; t <= script.duration() && f < n; t += DT, f++) {
            pose(s, swing, script, t);
            aim(ik, script);
            if (!primed) {
                ik.snap();
                primed = true;
            } else {
                ik.update(DT);
            }
            for (int i = 0; i < watched.length; i++) {
                s.worldPosition(s.bone(watched[i]).index, v);
                xs[i][f] = v.x;
                ys[i][f] = v.y;
            }
        }
        for (int i = 0; i < watched.length; i++) {
            float span = 0f;
            float path = 0f;
            for (int p = 0; p < f; p++) {
                for (int q = p + 1; q < f; q++) {
                    span = Math.max(span, (float) Math.hypot(xs[i][p] - xs[i][q], ys[i][p] - ys[i][q]));
                }
                if (p > 0) {
                    path += (float) Math.hypot(xs[i][p] - xs[i][p - 1], ys[i][p] - ys[i][p - 1]);
                }
            }
            out[i][0] = span * PX_PER_UNIT;
            out[i][1] = path * PX_PER_UNIT;
        }
    }

    private static int argmax(float[] v) {
        int best = 0;
        for (int i = 1; i < v.length; i++) {
            if (v[i] > v[best]) {
                best = i;
            }
        }
        return best;
    }

    /**
     * Drives a whole script at 60 Hz and returns the largest one-frame world
     * rotation change of any bone in any chain.
     *
     * <p>This is the check a contact sheet cannot make. Twelve frames spread over
     * four seconds are 22 substeps apart, so a single-frame mirror -- the exact
     * failure the bend filter and the slew ceiling exist to prevent -- would show
     * up on the sheet as two plausible poses next to each other and nothing else.
     *
     * @param driven false to run the same clip with the chains left inert, which
     *               is the only way to attribute motion to the IK rather than to
     *               the animation underneath it
     */
    private static float worstFrameStep(IkTargetScript.Kind kind, boolean driven) {
        Skeleton s = rig();
        RigIk ik = new RigIk(s);
        IkTargetScript script = new IkTargetScript(kind, rig());
        SwingAnimation swing = new SwingAnimation();
        String[] watched = {"hips", "spine", "chest", "upperArmL", "forearmL", "thighL", "shinL", "thighR", "shinR"};

        float[] prev = new float[watched.length];
        boolean primed = false;
        float worst = 0f;
        String worstBone = "";
        float worstAt = 0f;

        for (float t = 0f; t <= script.duration(); t += DT) {
            pose(s, swing, script, t);
            if (driven) {
                aim(ik, script);
                if (!primed) {
                    ik.snap();
                } else {
                    ik.update(DT);
                }
            }

            for (int i = 0; i < watched.length; i++) {
                float w = s.worldRotationDeg(s.bone(watched[i]).index);
                if (primed) {
                    float d = Math.abs(shortestArc(prev[i], w));
                    if (d > worst) {
                        worst = d;
                        worstBone = watched[i];
                        worstAt = t;
                    }
                }
                prev[i] = w;
            }
            primed = true;
        }
        if (worst > 0f) {
            // Named in the failure message rather than swallowed: "something
            // popped" is not an actionable finding, "forearmL at t=2.03" is.
            System.out.println("[RigIkTest] " + kind + (driven ? " driven" : " inert")
                    + " worst frame step " + worst + " deg on " + worstBone + " at t=" + worstAt);
        }
        return worst;
    }

    // -- helpers ---------------------------------------------------------------

    /**
     * Poses the base clip and writes the script's trunk drive, in the order
     * {@link IkSceneDriver} uses. Every check in this file that claims to measure
     * a capture scene has to go through here, or it is measuring a figure whose
     * hips do not move and the capture's do.
     */
    private static void pose(Skeleton s, SwingAnimation swing, IkTargetScript script, float t) {
        if (script.animated()) {
            applyPose(s, swing, t);
        } else {
            applyBind(s);
        }
        script.sample(t);
        script.applyBody(s);
        s.updateWorldTransforms();
    }

    /** Hands every chain its target and weight for the sample already taken. */
    private static void aim(RigIk ik, IkTargetScript script) {
        ik.swordArm().target(script.armX, script.armY).weight(script.armWeight);
        if (script.armPoleSet) {
            ik.armPoleFromChest(script.armPoleLocalX, script.armPoleLocalY);
        } else {
            ik.clearArmPole();
        }
        Vector2 v = ik.fromHips(script.spineLocalX, script.spineLocalY, new Vector2());
        ik.spine().target(v.x, v.y).weight(script.spineWeight);
        ik.legL().target(script.footLX, script.footLY).weight(script.legWeight);
        ik.legR().target(script.footRX, script.footRY).weight(script.legWeight);
    }

    /** Runs a bind-pose script from zero up to {@code until}, so a later phase can be examined in context. */
    private static void driveTo(Skeleton s, RigIk ik, IkTargetScript script, float until) {
        boolean primed = false;
        SwingAnimation swing = new SwingAnimation();
        for (float t = 0f; t < until; t += DT) {
            pose(s, swing, script, t);
            aim(ik, script);
            if (!primed) {
                ik.snap();
                primed = true;
            } else {
                ik.update(DT);
            }
        }
    }

    private static float armReach(Skeleton s) {
        Vector2 u = s.worldPosition(s.bone("upperArmL").index, new Vector2());
        Vector2 e = s.worldPosition(s.bone("forearmL").index, new Vector2());
        Vector2 h = s.worldPosition(s.bone("handL").index, new Vector2());
        return u.dst(e) + e.dst(h);
    }

    /** SamuraiRig#applyPose without the mesh, so this can run headless. */
    private static void applyPose(Skeleton s, SwingAnimation swing, float t) {
        dev.starfall.anim.Pose pose = swing.sample(t);
        for (int i = 0; i < s.boneCount(); i++) {
            dev.starfall.anim.Bone bone = s.bone(i);
            bone.resetToBind();
            dev.starfall.anim.Pose.Delta d = pose.get(bone.name);
            if (d != null) {
                bone.x = bone.bindX + d.dx;
                bone.y = bone.bindY + d.dy;
                bone.rotDeg = bone.bindRotDeg + d.dRotDeg;
                bone.scaleX = bone.bindScaleX * d.scaleX;
                bone.scaleY = bone.bindScaleY * d.scaleY;
            }
        }
        s.updateWorldTransforms();
    }

    private static void applyBind(Skeleton s) {
        for (int i = 0; i < s.boneCount(); i++) {
            s.bone(i).resetToBind();
        }
        s.updateWorldTransforms();
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
}
