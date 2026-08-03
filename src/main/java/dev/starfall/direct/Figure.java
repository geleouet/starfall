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
        ik.armPoleFromChest(REST_POLE_X, REST_POLE_Y);
    }

    /**
     * The elbow pole, in the chest's frame: below the sternum and <b>behind</b> it.
     *
     * <h2>The sign of the x is the whole of System 4 pass 1's aiming bug</h2>
     *
     * <p>It used to be {@code +0.10} -- below and in <em>front</em> of the sternum
     * -- copied from {@code SimSceneDriver}, where it is right: {@code ik-gesture}
     * reaches down and forward and the elbow leads.
     *
     * <p>On a duel it is wrong, and it is wrong in a way that decides the picture.
     * The blade's world angle is the forearm's plus 45 degrees, and the forearm's
     * angle is the line from elbow to fist -- so an elbow in <em>front</em> of the
     * fist points the blade <em>behind</em> the figure, whatever the wrist does.
     * That is the review's "drawn from a grip on the far side of its own torso,
     * pointing down and away, through the entire contact span", stated as a
     * coordinate.
     *
     * <p>Behind and under is also what the corpus holds: STYLE.md 7.0.2's "upper
     * arms hanging near the torso axis, the blade doing the reaching", and every
     * duellist in reference images 3, 4 and 5 has the elbow tucked at the ribs with
     * the forearm coming up out of it.
     *
     * <p>It is a rest pole rather than something switched on for the parry because
     * {@code TwoBoneIk.flipSeconds} is 0.60 s -- a bend flip is deliberately slower
     * than a whole beat, so an elbow asked to change sides when the crossing starts
     * arrives after the crossing has finished. Measured: half the flip had played
     * out by the end of the contact span and the blade still pointed backwards.
     * The pose has to be the standing one.
     */
    public static final float REST_POLE_X = -0.16f;
    public static final float REST_POLE_Y = -0.22f;

    /**
     * How much of its pooling the pale duellist keeps above the sash.
     *
     * <p>Tuned against delivered pixels rather than chosen: the target is reference
     * image 3's <b>3.3x</b> torso separation, measured as median ink luminance over the
     * frame's own ground level, and {@code DuellistValueTest} is the gate. See
     * {@code InkMaterial#sashLift}.
     */
    public static final float PALE_SASH_LIFT = 0.35f;

    /** And how much more the dark duellist pools above its sash. @see #PALE_SASH_LIFT */
    public static final float DARK_SASH_POOL = 1.70f;

    /** The dark duellist of reference images 3, 4 and 5. */
    public static Figure dark(int body) {
        return dark(body, SamuraiRig.build());
    }

    /** The same figure with no GPU meshes. See {@link SamuraiRig#headless()} and {@link Rehearsal}. */
    public static Figure headlessDark(int body) {
        return dark(body, SamuraiRig.headless());
    }

    private static Figure dark(int body, SamuraiRig rig) {
        Figure f = new Figure(body, rig);
        // <b>INK_BLACK, not INK_INDIGO, and this is a measurement rather than a
        // preference.</b> Pass 3 measured that the whole remaining Family B value gap
        // is the dark duellist -- 0.245 of the frame's own ground against reference
        // image 3's 0.133 -- and concluded that closing it "wants the mesh's authored
        // wetness above the sash raised, which is a System 1 change... that is the
        // concrete next step". The review refuted that with one line: shot with this
        // field set to INK_BLACK and nothing else changed
        // (out/captures/rev-p3-darkbase, frame 11, rects x300..470 y300..420 and
        // x600..760 y340..460) the torso ratio moves <b>2.12x -> 2.58x</b> -- four
        // times what DARK_SASH_POOL bought -- with the interior interquartile spread
        // unchanged, 0.277 -> 0.284.
        //
        // The counter-argument pass 3 gave, that reaching the corpus's value would
        // "print the flat fill 3b.5's first row fails on sight", is refuted by the
        // corpus's own pixels: reference image 3's dark torso (x190..300 y400..540)
        // has an interquartile spread of <b>0.063</b> against this capture's 0.277.
        // STYLE.md 1's "near-black ink silhouettes with just enough interior modelling
        // to find the face and hands" is a description of a tight, nearly flat dark
        // mass with a thin bright tail, and darkening moves toward it.
        //
        // Nothing here breaches 2.2's floor: INK_BLACK *is* the floor, and the pooling
        // rail of 3.4 sits on it while everything else lifts off it.
        f.cloth.base = Palette.INK_BLACK;
        f.cloth.deep = Palette.INK_BLACK;
        f.cloth.stain = Palette.OCHRE;
        // The other half of the Family B separation, and the half the review did not
        // ask for because nobody had measured it. Lifting the pale duellist alone gets
        // the torso ratio from 1.54 to 1.99 against the corpus's 3.27; the rest of the
        // gap is that <b>the dark duellist is not dark</b>. Measured on
        // s4-p3-parry-contact frame 11, rect x300..470 y300..420: median ink luminance
        // 56.2, which is 0.26 of the frame's ground and is exactly INK_INDIGO's own
        // luminance -- the torso is sitting on its base tone with nothing pooled into
        // it. Reference image 3's dark duellist reads 0.127 over the same statistic.
        // STYLE.md 1 calls Family B's bodies "near-black ink silhouettes with just
        // enough interior modelling to find the face and hands"; this is what makes
        // that true above the sash, and it cannot breach 2.2's floor because the value
        // it pools toward *is* the floor (INK_BLACK).
        f.cloth.sashHeight = 0.95f;
        f.cloth.sashLift = DARK_SASH_POOL;
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
        return pale(body, SamuraiRig.build());
    }

    /** The same figure with no GPU meshes. See {@link SamuraiRig#headless()} and {@link Rehearsal}. */
    public static Figure headlessPale(int body) {
        return pale(body, SamuraiRig.headless());
    }

    private static Figure pale(int body, SamuraiRig rig) {
        Figure f = new Figure(body, rig);
        f.cloth.base = Palette.CLOTH_PALE;
        // <b>The pooling colour is the floor, not the mid-tone, and pass 1 had this
        // backwards.</b> It pooled to INK_SLATE on the argument that "a pale garment
        // that pooled to the floor value would not be a pale garment, it would be a
        // dark one with a pale rim" -- which sounds right and is refuted by the
        // reference. Image 3's white-clad duellist is pale above the sash and
        // <em>near-black below it</em>, with a dark collar and dark hair on the
        // shoulder: the value floor is reached on the same figure, in the places ink
        // collects. Measured on the pass-1 capture, nothing in this figure's lower
        // body ever reached below 0.40x paper and its skirt bottomed out at 0.55x,
        // against the hero's 0.35x -- so it read as three marks rather than as a
        // silhouette, and STYLE.md 11.0 counts silhouettes.
        //
        // <b>This is a partial payment and it is reported as one.</b> What image 3
        // has and this cannot is a colourway that <em>changes</em> at the sash, and
        // the material has one base colour per draw call. Pooling to the floor
        // darkens the folds and the hem of the whole garment; it does not make the
        // hakama a different colour from the kimono. See docs/system4-debt.md.
        f.cloth.deep = Palette.INK_BLACK;
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
        // <b>And this is the compensation pass 2 named as missing and did not build.</b>
        // Pooling the pale figure to the floor is right below the sash and wrong above
        // it: reference image 3's white-clad duellist reads at 0.56 of the sky's value
        // at the torso against 0.17 for the dark one -- 3.3x apart -- and the pass-2
        // capture landed at 1.51x, where the review could not tell which duellist was
        // the pale one on any of twelve frames. INK_BLACK stays as the pooling colour;
        // what changes is that above the obi the pooling is scaled back so the garment
        // sits in the upper half of the range, which is what makes it read as the other
        // colour rather than as the same colour lit differently.
        //
        // Bind-space 0.95 is the obi line: Stage.Y_HIP is 0.98 and SamuraiRig authors
        // the haori rails' hip row at 0.96.
        f.cloth.sashHeight = 0.95f;
        f.cloth.sashLift = PALE_SASH_LIFT;
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
        pinElbow();
    }

    /**
     * Holds the sword elbow on the body side of the shoulder-to-hand line, for as
     * long as the figure faces that way.
     *
     * <h2>Why a pole is not enough here, which is a finding about the solver</h2>
     *
     * <p>{@code TwoBoneIk} picks its bend side by testing the pole against the
     * <em>settled</em> aim -- the filtered target, not the commanded one -- and then
     * defends the choice with a hysteresis deadband, an opening gate and a 0.60 s
     * flip. That is exactly right for a limb being swept around a body, and it is
     * what stops the elbow strobing when a target crosses the chain axis.
     *
     * <p>It is wrong for a strike, and measurably so. Through the whole approach the
     * settled aim still points <em>behind</em> the shoulder, because the filter is
     * half a second long and the hand is crossing most of a body height. So the pole
     * reads as being on the far side of an aim that is itself stale, the side is
     * never reconsidered, and the mirrored figure keeps the elbow it committed to at
     * bind -- above and in front of its own fist. Measured on the parry: the pole
     * sat 0.37 to the correct side of the aim for the entire contact span and
     * {@code bendSide} never moved off -1.00.
     *
     * <p>An elbow in front of the fist points the forearm backwards, and the blade
     * is the forearm plus 45 degrees, so that one bit decides which way the sword
     * points. It is not a quantity worth filtering: a duellist does not change which
     * side of the arm the elbow is on mid-strike. So it is pinned, per facing, and
     * the pole is left in place for everything else the solver uses it for.
     *
     * <p>The sign is the mirror: a left-facing figure's world-space bend side is the
     * opposite of a right-facing one's for the same anatomical pose.
     */
    private void pinElbow() {
        ik.swordArm().twoBoneSolver().preferSide(facing >= 0f ? -1f : 1f);
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
     * A point on the blade, {@code u} of the way from the habaki to the kissaki.
     *
     * <p>The blade is one bone and one authored strip, so its whole world segment
     * is two calls to this. <b>Nothing in the project could ask this question
     * before System 4 pass 2</b>, which is the mechanical reason a parry could
     * ship with the two blades a fifth of a body apart: every target, every
     * probe and every assertion named the <em>fist</em>, and the blade hangs a
     * further {@code 0.10} out of the fist at 45 degrees to it.
     */
    public com.badlogic.gdx.math.Vector2 bladeAt(float u, com.badlogic.gdx.math.Vector2 out) {
        return skeleton.worldAlong(skeleton.bone("blade").index, u * SamuraiRig.BLADE_LENGTH, out);
    }

    /**
     * Where along its own blade this figure crosses another's.
     *
     * <p><b>Measured off reference image 3 rather than assumed, and the assumption
     * was wrong.</b> The obvious guess is the monouchi -- the middle third, where a
     * blade is actually met in a fight -- and that guess puts the crossing 0.47
     * world units out of each fist, which on this lane is further from a shoulder
     * than the arm can reach, so both arms saturate and neither blade arrives.
     *
     * <p>What image 3 shows is quite different and much better staged: the two
     * duellists' <em>hands</em> are almost touching in the middle of the frame at
     * chest height, and the blades cross <b>just above the tsuba</b> -- 80 px above
     * the hilts on 335 px of blade -- then diverge upward into a narrow X with the
     * two kissaki a third of the frame apart. The bloom sits in the fork. It is a
     * bind, not a fencing measure, and it is why the picture reads as two people
     * leaning on each other rather than two people at arm's length.
     *
     * <p>0.24 is that measurement.
     */
    public static final float BLADE_CROSSING = 0.24f;

    /** The point on this blade that a {@code Meeting} is about. */
    public com.badlogic.gdx.math.Vector2 bladeCrossing(com.badlogic.gdx.math.Vector2 out) {
        return bladeAt(BLADE_CROSSING, out);
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
