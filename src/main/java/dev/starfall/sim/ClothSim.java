package dev.starfall.sim;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Skeleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Garment sway: a Verlet chain per run of cloth bones, written back as bone
 * rotations so the existing skinned garment follows it.
 *
 * <p>The same {@link VerletChain} the hair uses, with three differences that are
 * all policy rather than physics:
 *
 * <ul>
 *   <li><b>The rest shape is the bind pose.</b> A cloth bone's authored
 *       {@code bindRotDeg} <em>is</em> the rest bend at its joint, and the chain
 *       root's rest direction is its parent's current world rotation plus its own
 *       bind rotation. So a garment with the simulation switched off is exactly
 *       the garment System 1 authored, and the sim only ever adds a deviation.</li>
 *   <li><b>The swing is limited.</b> A hem free to swing 90 degrees stops being a
 *       hem and starts being a flag. Each bone's deviation from bind passes
 *       through a soft ceiling ({@link SimMath#softCeil}) rather than a clamp --
 *       a hard limit engaging mid-swing is a corner in the motion, which is the
 *       snap STYLE.md 7.2 bans.</li>
 *   <li><b>It arrives before the hair and after the body.</b> STYLE.md 7.1 puts
 *       cloth 4-8 frames behind the body and hair tips 8-14. That ordering is
 *       carried entirely by {@code dragTau} and the bend stiffness, and it is
 *       measured rather than asserted -- see {@code SimTimingTest}.</li>
 * </ul>
 *
 * <h2>Ordering</h2>
 *
 * <p>Animation writes bone locals, then IK chains solve, <em>then</em> this, then
 * the renderer. {@link #refresh()} reads the anchor and the rest directions off
 * the posed skeleton; {@link #writeBack()} puts the solved directions back as
 * local rotations and leaves the skeleton's world transforms current.
 */
public final class ClothSim {

    /** One run of cloth bones and the chain that drives it. */
    public static final class Chain {

        final VerletChain chain;
        final Bone[] bones;
        final Bone anchorParent;
        final float[] bindRotDeg;
        final float swingKneeDeg;
        final float swingLimitDeg;
        float windScale = 1f;

        Chain(VerletChain chain, Bone[] bones, float swingKneeDeg, float swingLimitDeg) {
            this.chain = chain;
            this.bones = bones;
            this.anchorParent = bones[0].parent;
            this.bindRotDeg = new float[bones.length];
            for (int i = 0; i < bones.length; i++) {
                bindRotDeg[i] = bones[i].bindRotDeg;
            }
            this.swingKneeDeg = swingKneeDeg;
            this.swingLimitDeg = swingLimitDeg;
        }

        public int particleCount() {
            return chain.particleCount();
        }

        public float x(int i) {
            return chain.x(i);
        }

        public float y(int i) {
            return chain.y(i);
        }

        /** Per-substep displacement of the free end. The quantity a lag measurement reads. */
        public float tipSpeed() {
            return chain.speedOf(chain.particleCount() - 1);
        }

        public VerletChain solverChain() {
            return chain;
        }

        public Bone bone(int i) {
            return bones[i];
        }

        public int boneCount() {
            return bones.length;
        }
    }

    /**
     * System property, and {@code --clamp cloth} on the capture and timing harnesses, that
     * welds every cloth chain to its bind pose.
     *
     * @see #clampRigid(boolean)
     */
    public static final String CLAMP_PROPERTY = "starfall.clamp.cloth";

    private static volatile boolean rigidClamp = Boolean.getBoolean(CLAMP_PROPERTY);

    /**
     * The rigid control STYLE.md §7.1 requires and nothing in this repository could run.
     *
     * <p>§7.1 says, twice and in its own words: <i>"Before quoting a lag, run the rigid
     * control; if a dead system scores near the live one, the rectangle is measuring the
     * wrong thing"</i>, and then <i>"a rule that needs an instrument must ship the
     * instrument"</i>. The instrument was still missing three passes later — the pass-3
     * control that produced the +0.34 frame figure was a hand-edited swing limit that was
     * never committed, so the one number that invalidated the graded rectangle could not be
     * re-run by anybody. §7.1's new drape-excursion gate 3 (<i>rigid control ≤ 0.15×</i>)
     * is unmeasurable without it.
     *
     * <p>Clamping happens in {@link #writeBack()} rather than by zeroing a swing limit,
     * because it has to be <em>exact</em>: a limit of zero still passes the deviation
     * through {@link SimMath#softCeil}, which is zero only in the limit, and a control that
     * leaks a tenth of a degree is a control that cannot prove a statistic has no dynamic
     * range. With this on, every cloth bone holds {@code bindRotDeg} on every frame, so the
     * garment is exactly the garment {@code SamuraiRig} authored, welded to the hips. The
     * solver still runs — the chains are still stepped and are still readable through
     * {@code SceneProbe} — so a probe series taken under the clamp shows the particles
     * moving while the picture does not, which is the distinction the control exists to
     * draw.
     *
     * <p>Global rather than per-instance on purpose. It is a measurement switch thrown once
     * at process start by {@code --clamp cloth}, and every {@code RigSim} in the scene must
     * be under it or the control is only half a control.
     */
    public static void clampRigid(boolean on) {
        rigidClamp = on;
    }

    /** True when {@link #clampRigid} is engaged. Written into {@code capture.txt}. */
    public static boolean isClampedRigid() {
        return rigidClamp;
    }

    private final Skeleton skeleton;
    private final List<Chain> chains = new ArrayList<>();
    private final Vector2 scratch = new Vector2();

    private float windX;
    private float windY;

    public ClothSim(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    /**
     * Binds a chain to a run of bones. The bones must already be posed at bind
     * when this is called: the segment lengths are read off their bind local
     * translations, which is what makes "rest shape == bind pose" exact.
     *
     * @param tailLength    how long the last bone's segment is. It cannot be derived
     *                      -- the last cloth bone has no child to measure to -- and it
     *                      is the lever arm the write-back reads that bone's angle off,
     *                      so it should be roughly how far the authored garment rows
     *                      below it actually reach.
     * @param swingLimitDeg how far a bone may deviate from its bind rotation. Soft.
     */
    public Chain addChain(String[] boneNames, float tailLength, float dragTau, float bendTau,
                          float gravity, float windScale, float swingLimitDeg) {
        Bone[] bones = new Bone[boneNames.length];
        for (int i = 0; i < boneNames.length; i++) {
            bones[i] = skeleton.bone(boneNames[i]);
        }
        // n bones -> n segments -> n+1 particles: particle 0 sits at bone 0's
        // origin and particle i+1 at bone i's tip, which is bone i+1's origin.
        VerletChain chain = new VerletChain(bones.length + 1);
        for (int i = 0; i < bones.length; i++) {
            float len = i + 1 < bones.length
                    ? SimMath.length(bones[i + 1].bindX, bones[i + 1].bindY)
                    : tailLength;
            // Joint 0's direction is the parent's, handled by rootDirDeg; joints
            // above it bend by the child's own authored bind rotation.
            float bend = i == 0 ? 0f : bones[i].bindRotDeg;
            chain.segment(i, len, bend, bendTau);
        }
        chain.dragTau(dragTau).iterations(8).gravity(0f, -gravity);
        Chain c = new Chain(chain, bones, 0.55f * swingLimitDeg, swingLimitDeg);
        c.windScale = windScale;
        chains.add(c);
        return c;
    }

    public int chainCount() {
        return chains.size();
    }

    public Chain chain(int i) {
        return chains.get(i);
    }

    public List<Chain> chains() {
        return chains;
    }

    public ClothSim wind(float wx, float wy) {
        this.windX = wx;
        this.windY = wy;
        return this;
    }

    public void register(VerletSolver solver) {
        for (Chain c : chains) {
            solver.add(c.chain);
        }
    }

    /** Re-anchors every chain onto the posed skeleton. Call after IK, before the solver steps. */
    public void refresh() {
        for (Chain c : chains) {
            skeleton.worldPosition(c.bones[0].index, scratch);
            c.chain.anchor(scratch.x, scratch.y);
            c.chain.rootDirDeg(parentWorldDeg(c) + c.bindRotDeg[0]);
            c.chain.wind(windX * c.windScale, windY * c.windScale);
        }
    }

    /** Lays every chain out along its bind shape from the current anchors, with no velocity. */
    public void reset() {
        refresh();
        for (Chain c : chains) {
            c.chain.reset();
        }
        writeBack();
    }

    /**
     * Turns solved segment directions into {@code Bone.rotDeg} and refreshes the
     * skeleton.
     *
     * <p>The conversion is the one {@code IkChain.writeBack} documents:
     * {@code world = parentWorld + det(parent) * rotDeg}, where the determinant
     * sign flips for every mirrored ancestor. That is not a corner case here --
     * a fighter facing left is {@code root.scaleX = -1}, and getting the sign
     * wrong would make cloth sway correctly for one facing and invert for the
     * other.
     */
    public void writeBack() {
        for (Chain c : chains) {
            float parentWorld = parentWorldDeg(c);
            float sign = mirrorSignAbove(c.bones[0]);
            for (int i = 0; i < c.bones.length; i++) {
                Bone b = c.bones[i];
                if (rigidClamp) {
                    // The control: bind exactly, every frame. Nothing the solver did reaches
                    // the skeleton, so the garment is welded to whatever bone the chain
                    // hangs off and the picture contains no cloth motion at all.
                    b.rotDeg = c.bindRotDeg[i];
                    parentWorld = parentWorld + sign * b.rotDeg;
                    sign *= b.scaleX * b.scaleY < 0f ? -1f : 1f;
                    continue;
                }
                float solvedWorld = c.chain.segmentDeg(i);
                float local = sign * SimMath.deltaDeg(parentWorld, solvedWorld);
                // Soft ceiling on the deviation from bind, both ways.
                float dev = SimMath.deltaDeg(c.bindRotDeg[i], local);
                float mag = Math.abs(dev);
                if (mag > c.swingKneeDeg) {
                    dev = Math.signum(dev) * SimMath.softCeil(mag, c.swingKneeDeg, c.swingLimitDeg);
                }
                b.rotDeg = c.bindRotDeg[i] + dev;
                parentWorld = parentWorld + sign * b.rotDeg;
                sign *= b.scaleX * b.scaleY < 0f ? -1f : 1f;
            }
        }
        skeleton.updateWorldTransforms();
    }

    private float parentWorldDeg(Chain c) {
        return c.anchorParent == null ? 0f : skeleton.worldRotationDeg(c.anchorParent.index);
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
