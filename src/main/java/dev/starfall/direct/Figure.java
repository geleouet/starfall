package dev.starfall.direct;

import com.badlogic.gdx.graphics.Color;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Pose;
import dev.starfall.anim.Skeleton;
import dev.starfall.art.Palette;
import dev.starfall.render.InkMaterial;
import dev.starfall.rig.RigIk;
import dev.starfall.rig.RigSim;
import dev.starfall.rig.SamuraiRig;
import dev.starfall.stage.Chain;
import dev.starfall.stage.Stance;

/**
 * One body on the lane: a rig, its IK, its simulation, where it stands, which way
 * it faces and what colour its cloth is.
 *
 * <p><b>This class exists because there has never been more than one figure.</b>
 * Every scene before System 4 built a {@code SamuraiRig} at the origin, drew it,
 * and never asked whether a second one would work. It does, and the audit that
 * established it is worth recording because the answer is not obvious from the
 * code: {@code SamuraiRig.build()} allocates a fresh {@code Skeleton}, fresh
 * {@code Bone}s and two fresh {@code Mesh} uploads per call, {@code RigIk},
 * {@code RigSim}, {@code HairSim}, {@code ClothSim}, {@code VerletChain} and
 * {@code VerletSolver} hold no static mutable state at all, and the shader's
 * 32-bone cap is a <em>per draw call</em> cap rather than a per-frame one, so two
 * 28-bone figures need 28 slots and not 56. Three things did have to change and
 * all three are named where they were fixed:
 *
 * <ul>
 *   <li>{@code InkSkinnedRenderer} latched the cloth colour per <em>flush</em>
 *       rather than per draw, so a pale figure drawn before a dark one came out
 *       dark. It now ends a merge group when the resolve parameters change, which
 *       also gives each figure its own coverage field -- otherwise two figures
 *       average their ink where they overlap, and overlapping is the Pilgrim's
 *       whole movement verb.</li>
 *   <li>{@code RigIk}'s body-carried poles rotated a bone-local offset by a world
 *       angle without the mirror term, so a left-facing figure's knee poles landed
 *       behind the pelvis instead of in front of it. See {@code RigIk.fromBone}.</li>
 *   <li>Every noise field in {@code ink_skin.frag} is sampled in bind space, on
 *       purpose, so two figures cut from one rig were painted with bit-identical
 *       ink. {@link InkMaterial#seedX} offsets the sample point in that same
 *       space.</li>
 * </ul>
 *
 * <h2>Standing somewhere, facing somewhere</h2>
 *
 * <p>Both are the root bone's, not the renderer's. {@code SamuraiRig}'s own note
 * says facing "flips by negating {@code root.scaleX}, not by rebuilding geometry",
 * and the same bone's {@code x} carries the stand position -- which puts both
 * figures in one shared world space, so a {@code Stage} anchor is a world point
 * for both of them and the two skeletons can be aimed at the same crossing without
 * either of them having to convert. A per-draw model matrix would have been the
 * other way to do it and would have made the parry impossible to express.
 */
public final class Figure {

    /** How far the root scale is allowed to collapse mid-turn. Never to zero. */
    private static final float TURN_PINCH = 0.12f;

    private final int body;
    private final SamuraiRig rig;
    private final RigIk ik;
    private final RigSim sim;
    private final Skeleton skeleton;

    private final InkMaterial cloth = new InkMaterial();
    private final InkMaterial blade = new InkMaterial().asBlade();

    private final Pose pose = new Pose();

    private double standX;
    /** +1 facing +X, -1 facing -X, and continuous through a turn rather than a boolean. */
    private float facing = 1f;

    private Stance stance = Stance.READY;
    private float stanceBlend = 1f;
    private Stance previousStance = Stance.READY;

    private float dissolveBias;

    private Figure(int body, SamuraiRig rig) {
        this.body = body;
        this.rig = rig;
        this.skeleton = rig.skeleton();
        this.ik = RigIk.of(rig);
        this.sim = new RigSim(skeleton);
        // The pose-derived elbow pole is decided by float rounding when the bind
        // arm is straight, which it very nearly is -- RigIk#armPoleFromChest says
        // so and SimSceneDriver uses these exact numbers. Below and behind the
        // shoulder is what STYLE.md 7.0.2 asks for: "upper arms hanging near the
        // torso axis, the blade doing the reaching".
        ik.armPoleFromChest(0.10f, -0.19f);
    }

    /** The dark duellist of reference images 3, 4 and 5. */
    public static Figure dark(int body) {
        Figure f = new Figure(body, SamuraiRig.build());
        f.cloth.base = Palette.INK_INDIGO;
        f.cloth.deep = Palette.INK_BLACK;
        f.cloth.stain = Palette.OCHRE;
        return f;
    }

    /**
     * The pale one standing against it. STYLE.md 2.1 carries {@code CLOTH_PALE}
     * "the white-kimono duellist; cool grey, not white" for exactly this frame.
     *
     * <p>Its pooling colour is {@code INK_SLATE} rather than {@code INK_BLACK}:
     * STYLE.md 3.4 wants ink darkest where it collects, and a pale garment that
     * pooled to the floor value would not be a pale garment, it would be a dark
     * one with a pale rim. Slate is four levels of value below the base and keeps
     * the whole figure inside the upper half of the range, which is what makes it
     * read as the other colour rather than as the same colour lit differently.
     * The ink seed is offset so the two are not photocopies of one another.
     */
    public static Figure pale(int body) {
        Figure f = new Figure(body, SamuraiRig.build());
        f.cloth.base = Palette.CLOTH_PALE;
        f.cloth.deep = Palette.INK_SLATE;
        // The dark figure's stain is OCHRE and its pale rim OCHRE_PALE, which reads
        // as rust bleeding through indigo. The same pair on a pale base is a large
        // warm field rather than a stain -- measured on the first two-figure
        // capture, where it took over most of the torso -- and STYLE.md 10 fails a
        // pass on sight of "saturated colour across large areas". The stain is
        // therefore the darker of the two on this figure, so the rust reads as
        // something that has soaked into the cloth rather than as its colour.
        f.cloth.stain = Palette.OCHRE;
        f.cloth.seedX = 3.70f;
        f.cloth.seedY = -2.30f;
        return f;
    }

    public int body() {
        return body;
    }

    public SamuraiRig rig() {
        return rig;
    }

    public Skeleton skeleton() {
        return skeleton;
    }

    public RigIk ik() {
        return ik;
    }

    public RigSim sim() {
        return sim;
    }

    /** The cloth material. Mutated per frame for STYLE.md 7.3's shed flecks. */
    public InkMaterial clothMaterial() {
        return cloth;
    }

    public InkMaterial bladeMaterial() {
        return blade;
    }

    public double standX() {
        return standX;
    }

    public float facing() {
        return facing;
    }

    public Figure standAt(double worldX, float facingSign) {
        this.standX = worldX;
        this.facing = facingSign;
        return this;
    }

    /**
     * Where the figure is turned, as a continuous value rather than a flag.
     *
     * <p>STYLE.md 7.2's first line is "no snapping", and a facing expressed as a
     * boolean is the one change in the whole vocabulary that can only ever snap.
     * {@code Directive.FacingChange} therefore carries a duration and an ease, and
     * this is what that duration drives: the root's x scale runs continuously from
     * +1 to -1, pinched rather than collapsed at the crossing so the figure
     * narrows into an edge and opens out the other way -- a brush figure turning,
     * which is what combat-design.md 2.2 means by "the whole body winding around".
     * The garment and the hair are thrown by their own impulses on the way round
     * and arrive last, which is the other half of the same beat.
     */
    public void facing(float signed) {
        float mag = Math.max(TURN_PINCH, Math.abs(signed));
        this.facing = signed < 0 ? -mag : mag;
    }

    /** The stance the body is holding, and how far into it it is. */
    public void stance(Stance to, float blend) {
        if (to != stance) {
            previousStance = stance;
            stance = to;
        }
        stanceBlend = Math.max(0f, Math.min(1f, blend));
    }

    public Stance stance() {
        return stance;
    }

    /** STYLE.md 7.3's shed flecks: the dissolve threshold pushed for a moment. */
    public void dissolveBias(float bias) {
        this.dissolveBias = bias;
        cloth.dissolveBias = bias;
    }

    public float dissolveBias() {
        return dissolveBias;
    }

    /**
     * Writes this frame's bone locals: the stance, then the stand position and
     * facing on the root.
     *
     * <p>First of the four steps in the ordering contract -- animation writes bone
     * locals, IK chains update, simulation, renderer -- and the root has to be
     * rewritten here rather than once at setup because {@code applyPose} resets
     * every bone to bind before it applies a delta.
     */
    public void writePose() {
        Stances.blend(pose, previousStance, stance, stanceBlend);
        pose.set("root", (float) standX, 0f, 0f, facing, 1f);
        rig.applyPose(pose);
    }

    /** Second step: solve every chain, trunk first. */
    public void solve(float dt) {
        ik.update(dt);
    }

    /** Third step: hair and cloth, off the bones IK has just moved. */
    public void simulate(float dt, float timeSeconds) {
        sim.update(dt, timeSeconds);
    }

    /** Lays the whole figure out with no velocity and no settle. Scene setup only. */
    public void snap(float timeSeconds) {
        writePose();
        ik.snap();
        sim.snap(timeSeconds);
    }

    /** World position of a bone, for probes and for aiming ink at a body. */
    public com.badlogic.gdx.math.Vector2 where(String bone, com.badlogic.gdx.math.Vector2 out) {
        Bone b = skeleton.bone(bone);
        return skeleton.worldPosition(b.index, out);
    }

    /**
     * The chain object a {@link Chain} names.
     *
     * <p>{@code Chain}'s own note says it names {@code RigIk}'s chains "rather than
     * referenced: the staging layer is headless and must stay so... the renderer
     * maps a Chain to its own object; the {@code bones()} list is here so that
     * mapping is checkable rather than conventional." This is that mapping, and
     * {@code DirectorTest} checks it against {@code Chain.bones()}.
     */
    public dev.starfall.ik.IkChain chainOf(Chain chain) {
        return switch (chain) {
            case SPINE -> ik.spine();
            case SWORD_ARM -> ik.swordArm();
            case LEG_LEAD -> ik.legL();
            case LEG_TRAIL -> ik.legR();
            // The clavicle is an AimLink, not an IkChain: RigIk drives it from the
            // sword arm's own target so a caller sets one target for the limb and
            // gets the whole shoulder-to-hand run. Director#clavicle handles it.
            case CLAVICLE -> null;
        };
    }

    /** A mutable copy, so nothing ever writes through a {@link Palette} constant. */
    public static Color copyOf(Color c) {
        return new Color(c);
    }
}
