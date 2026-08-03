package dev.starfall.rig;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Pose;
import dev.starfall.anim.Skeleton;
import dev.starfall.anim.SkinnedMesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The procedural samurai: bone hierarchy + skinned meshes, per
 * docs/system1-contract.md section E and revision 2 (docs/system1-rig-fixes.md).
 * Silhouette target is inspirations/1-2 -- a three-quarter profile figure
 * facing +X, shoulder-heavy and asymmetric, with a haori that flares far wider
 * than the body and dissolves into ink over its lower third, trailing further
 * behind (-X) than in front.
 *
 * <p>All mesh coordinates are authored directly in world-bind-space (the
 * position a vertex has when every bone sits at its bind pose) -- that is the
 * space {@link Skeleton#fillSkinningMatrices} expects, since the inverse-bind
 * matrices baked in at construction map exactly out of it. Units are metres-ish
 * with the root at ground level and a ~1.75-tall standing figure (feet ~0.13,
 * head ~1.7 -- matched to what dev.starfall.render.Atmosphere assumes); a scene
 * picks whatever camera/scale gets that into frame.
 *
 * <p>Facing flips by negating {@code root.scaleX}, not by rebuilding geometry
 * (rig-fixes section 1) -- {@link Skeleton}'s affine math and the shader's
 * disabled face culling both already support that without special-casing here.
 */
public final class SamuraiRig {

    private final Skeleton skeleton;
    private final SkinnedMesh mesh;
    private final SkinnedMesh bladeMesh;
    private final SkinnedMesh faceMesh;
    private final SkinnedMesh faceInkMesh;
    private final SkinnedMesh fittingsMesh;
    private final FaceParams face;

    private SamuraiRig(Skeleton skeleton, SkinnedMesh mesh, SkinnedMesh bladeMesh,
                       SkinnedMesh faceMesh, SkinnedMesh faceInkMesh, SkinnedMesh fittingsMesh,
                       FaceParams face) {
        this.skeleton = skeleton;
        this.mesh = mesh;
        this.bladeMesh = bladeMesh;
        this.faceMesh = faceMesh;
        this.faceInkMesh = faceInkMesh;
        this.fittingsMesh = fittingsMesh;
        this.face = face;
    }

    /**
     * The fittings pass's own null control (MEASUREMENT.md 11.2b(g)): {@code
     * -Pfittings=off} on the capture task sets {@code starfall.fittings=off} and
     * this rig authors no fittings-pass geometry at all — no fittings mesh, no
     * sash knot, no fold clefts. A {@code -bare} capture shot that way is the
     * "what would the count read if the thing being measured were absent" case.
     *
     * <p>A system property rather than a {@code CaptureSpec} field for the reason
     * docs/ink-hypothesis.md section 2 records: {@code CaptureSpec} is in the
     * {@code HarnessId} apparatus digest and this switch is subject, not
     * apparatus. Read per build, not cached in a static, so one JVM can build
     * both variants (the tests do).
     */
    public static boolean fittingsEnabled() {
        return !"off".equals(System.getProperty("starfall.fittings", "on"));
    }

    /** Heel to crown, in world units. The scale everything on a figure is quoted against. */
    public static final float FIGURE_HEIGHT = 1.70f;

    /**
     * The nagasa, in world units: {@link MeshAuthor#BLADE_NAGASA_FRACTION} of the
     * figure's own height. Public because aiming a blade needs to know how long
     * it is, and because every blade statistic in {@code docs/system4-debt.md} is
     * a fraction of the figure height this is derived from.
     */
    public static final float BLADE_LENGTH = MeshAuthor.BLADE_NAGASA_FRACTION * FIGURE_HEIGHT;

    /**
     * How far the blade's habaki sits out of the fist, along the hand's own axis.
     *
     * <p>Small, and load-bearing out of all proportion to its size: this offset
     * plus the blade's 45-degree bind angle is the entire distance between "the
     * two hands agree on a point" and "the two blades agree on a point", and pass
     * 1 of System 4 spent a whole pass on the wrong one of those.
     */
    public static final float BLADE_GRIP_OFFSET = 0.10f;

    public static SamuraiRig build() {
        return build(FaceParams.hero());
    }

    /**
     * The same rig with this face. System 3b: the hero and bosses hand in
     * {@link FaceParams#hero()} (or their own authored constants); ordinary
     * Charted Shadows hand in {@link FaceParams#generate(long)}.
     */
    public static SamuraiRig build(FaceParams face) {
        Skeleton skeleton = buildSkeleton();
        // Mesh authoring below reads bone world positions/rotations, so the
        // skeleton must have its bind-pose globals computed first.
        skeleton.updateWorldTransforms();
        MeshAuthor author = new MeshAuthor(skeleton, face);
        SkinnedMesh mesh = author.buildBody();
        SkinnedMesh bladeMesh = author.buildBlade();
        SkinnedMesh faceMesh = author.buildFace(face);
        SkinnedMesh faceInkMesh = author.buildFaceInk(face);
        SkinnedMesh fittingsMesh = fittingsEnabled() ? author.buildFittings() : null;
        return new SamuraiRig(skeleton, mesh, bladeMesh, faceMesh, faceInkMesh, fittingsMesh, face);
    }

    /**
     * The whole rig except the two GPU meshes: same skeleton, same bind pose, no
     * vertex buffers and therefore no GL context.
     *
     * <p><b>This is what makes a schedule rehearsable in a test.</b> Everything
     * between the staging layer and the picture -- the director's reading of a
     * directive, the IK chains, the blade aim, the cloth and hair -- is pure
     * arithmetic on this skeleton; only the two {@link SkinnedMesh} uploads need
     * a context. Before this existed, the only way to ask "where is the
     * defender's blade during the contact span" was to shoot a capture and
     * measure pixels, which is how a beat that never happened survived a whole
     * pass. See {@code dev.starfall.direct.Rehearsal}.
     *
     * <p>{@link #mesh()} and {@link #bladeMesh()} are null on a headless rig and
     * say so rather than returning an empty mesh, because a renderer handed an
     * empty mesh draws nothing and reports success.
     */
    public static SamuraiRig headless() {
        return new SamuraiRig(buildSkeletonOnly(), null, null, null, null, null, FaceParams.hero());
    }

    /**
     * The bone hierarchy alone, with no GPU meshes -- world transforms already
     * refreshed onto bind. {@link #build()} needs a live GL context because
     * {@link SkinnedMesh.Builder} uploads vertex buffers, so anything that only
     * wants to reason about the skeleton (IK unit tests, tooling) has to be able
     * to ask for it without one. Purely additive: {@link #build()} still goes
     * through the same {@code buildSkeleton()}.
     */
    public static Skeleton buildSkeletonOnly() {
        Skeleton skeleton = buildSkeleton();
        skeleton.updateWorldTransforms();
        return skeleton;
    }

    public Skeleton skeleton() {
        return skeleton;
    }

    /** Body + all cloth. Draw with a cloth InkMaterial. */
    public SkinnedMesh mesh() {
        return mesh;
    }

    /**
     * The blade alone, same skeleton, weighted entirely to the {@code blade}
     * bone. Split out per rig-fixes section 3 / contract section E: it is the
     * only part of the figure drawn with a different material (near-white,
     * emissive), and InkMaterial applies per draw call, so it cannot share a
     * mesh with the cloth. Draw with {@code new InkMaterial().asBlade()}.
     */
    public SkinnedMesh bladeMesh() {
        return bladeMesh;
    }

    /**
     * The skin of STYLE.md 4b: the value structure of a face — lit plane, socket
     * shadow, jaw turn, blush and marks. Draw with a skin {@code InkMaterial}
     * (base {@code SKIN_BASE}, deep {@code SKIN_DEEP}, stain {@code BLUSH}, pale
     * stain {@code LIP}) after the cloth so it forms its own merge group. Null on
     * a headless rig.
     */
    public SkinnedMesh faceMesh() {
        return faceMesh;
    }

    /**
     * The face's ink marks — brow stroke, lash and iris, nostril, lip parting,
     * beard. Draw with an ink material AFTER {@link #faceMesh()}; fade both with
     * {@code InkMaterial#covScale} on pull-out (STYLE.md 3b.1 for authored
     * marks). Null on a headless rig.
     */
    public SkinnedMesh faceInkMesh() {
        return faceInkMesh;
    }

    /**
     * The fittings pass (STYLE.md 11.4, System 1 debt D1): the hand-grip-guard
     * cluster and the feet, as their own merge group so they can be drawn with a
     * material anchored to the corpus's own fitting register — the sub-floor
     * {@code INK_BLACK_DUSK} strokes of 2.2's measured 0.12-0.14x-sky band —
     * rather than the garment's. The cloth path bottoms out at the paper-ground
     * floor (L ~25 on the dusk stage), and the reference's grips and guards sit
     * at 0.13-0.18x their local sky (ref3-matched-378.png, right duellist:
     * tsuba x243..265 y263..272 p2 18.5, grip-below-fist x270..286 y313..328
     * p2 16.4, against local sky x220..240 y280..300 median 109.9) — darker
     * than anything the cloth material can print, which is why the delivered
     * grip read as more sleeve. Fists ride the stain channel's fitting regime
     * instead, so they print the material's leather tone: the corpus fist plane
     * is 0.26-0.28x sky with lit knuckles at 0.4-0.7x (fist boxes x248..268
     * y274..290 and x256..276 y296..312, medians 30.2/28.6 on the same sky).
     *
     * <p>Draw with {@link dev.starfall.direct.Figure#fittingsMaterial()} AFTER
     * the cloth and BEFORE the blade, so nothing composites over the cluster
     * except the steel it explains. Null on a headless rig and null when
     * {@link #fittingsEnabled()} is off (the {@code -bare} control).
     */
    public SkinnedMesh fittingsMesh() {
        return fittingsMesh;
    }

    /** The parameters this rig's face was authored or generated from. */
    public FaceParams face() {
        return face;
    }

    /** Every bone back to bind, world transforms refreshed. Also what applyPose starts from. */
    public void applyBindPose() {
        for (int i = 0; i < skeleton.boneCount(); i++) {
            skeleton.bone(i).resetToBind();
        }
        skeleton.updateWorldTransforms();
    }

    /**
     * Writes local transforms: every bone resets to bind, then bones named in
     * {@code pose} get bind + delta. Pose is sparse (see {@link Pose}) precisely
     * so callers never have to restate a whole skeleton's bind offsets.
     */
    public void applyPose(Pose pose) {
        for (int i = 0; i < skeleton.boneCount(); i++) {
            Bone b = skeleton.bone(i);
            b.resetToBind();
            Pose.Delta d = pose.get(b.name);
            if (d != null) {
                b.x = b.bindX + d.dx;
                b.y = b.bindY + d.dy;
                b.rotDeg = b.bindRotDeg + d.dRotDeg;
                b.scaleX = b.bindScaleX * d.scaleX;
                b.scaleY = b.bindScaleY * d.scaleY;
            }
        }
        skeleton.updateWorldTransforms();
    }

    // -- Skeleton -----------------------------------------------------------

    /**
     * Parent-first hierarchy per contract section E. Names are load-bearing --
     * side B addresses bones by name. 21 body bones plus System 3's seven cloth
     * bones is 28, inside the 32 hard cap of contract section B with four spare.
     *
     * <p>Trunk bones (hips..head) stack via Y offset. {@code spine} carries an
     * 8-12 degree forward lean (rig-fixes section 2) as its own bindRotDeg, so
     * everything downstream of it -- chest, neck, head, both arms -- inherits
     * that lean as their cumulative rotation. Limb bones encode their bind
     * direction directly in bindRotDeg, and each child's bindX is the parent
     * segment's length, so walking a chain via cumulative rotation ("along the
     * bone") is meaningful. See MeshAuthor#alongBone.
     *
     * <p>{@code L} bones are the near/front side (facing +X): the sword arm and
     * the leading leg, larger and drawn last so they sit on top. {@code R}
     * bones are the far/back side: smaller, higher on the chest, and drawn
     * first so the torso and haori occlude most of them.
     */
    private static Skeleton buildSkeleton() {
        List<Bone> bones = new ArrayList<>();

        Bone root = add(bones, new Bone("root", 0, null).bindLocal(0f, 0f, 0f));
        Bone hips = add(bones, new Bone("hips", 1, root).bindLocal(0f, 0.98f, 0f));
        // Forward lean lives here, not on chest: everything above hips inherits
        // it as cumulative rotation, giving the figure intent even at rest.
        Bone spine = add(bones, new Bone("spine", 2, hips).bindLocal(0f, 0.16f, -10f));
        Bone chest = add(bones, new Bone("chest", 3, spine).bindLocal(0f, 0.20f, 0f));
        Bone neck = add(bones, new Bone("neck", 4, chest).bindLocal(0f, 0.14f, 0f));
        // 0.14 rather than 0.10: with the shoulder row dropped to 1.43 this is
        // what buys a visible neck column between the mantle and the jaw.
        add(bones, new Bone("head", 5, neck).bindLocal(0f, 0.14f, 0f));

        // Sword arm (near/front side). Revision 3: the shoulder sits 0.07 lower
        // on the chest than revision 2 had it. It used to land at y=1.495, above
        // the neck bone and level with the skull's chin, which left the figure
        // with no neck at all -- the head simply rested on the shoulder line,
        // which is the gap the pass-2 review measured at the throat.
        Bone shoulderL = add(bones, new Bone("shoulderL", 6, chest).bindLocal(0.10f, 0.11f, -55f));
        Bone upperArmL = add(bones, new Bone("upperArmL", 7, shoulderL).bindLocal(0.04f, 0f, 0f));
        Bone forearmL = add(bones, new Bone("forearmL", 8, upperArmL).bindLocal(0.30f, 0f, -10f));
        Bone handL = add(bones, new Bone("handL", 9, forearmL).bindLocal(0.26f, 0f, 0f));
        // Blade bind angle -30 absolute: a long forward-reaching diagonal whose
        // tip lands *on* the ground line rather than buried a fifth of a body
        // below it, which is where revision 2's -45 put it. The extra 15 degrees
        // live here rather than in the shoulder because the arm has to stay
        // tucked against the ribs -- swing it forward to raise the tip and a
        // 50 px pocket of bare paper opens between the haori front and the
        // sleeve, which is a worse fault than the one it fixes.
        add(bones, new Bone("blade", 10, handL).bindLocal(BLADE_GRIP_OFFSET, 0f, 45f));

        // Off arm (far/back side): shorter, tucked, attached higher on the chest.
        Bone shoulderR = add(bones, new Bone("shoulderR", 11, chest).bindLocal(-0.09f, 0.14f, -85f));
        Bone upperArmR = add(bones, new Bone("upperArmR", 12, shoulderR).bindLocal(0.02f, 0f, 0f));
        Bone forearmR = add(bones, new Bone("forearmR", 13, upperArmR).bindLocal(0.19f, 0f, -30f));
        add(bones, new Bone("handR", 14, forearmR).bindLocal(0.14f, 0f, 0f));

        // Near/front leg, planted slightly forward. Revision 3 narrows the
        // stance from +-0.13 to +-0.09: the two legs plus the hakama around them
        // were the widest horizontal in the figure, which is half of why the
        // silhouette measured as a cone.
        Bone thighL = add(bones, new Bone("thighL", 15, hips).bindLocal(0.075f, -0.01f, -85f));
        Bone shinL = add(bones, new Bone("shinL", 16, thighL).bindLocal(0.44f, 0f, 0f));
        add(bones, new Bone("footL", 17, shinL).bindLocal(0.40f, 0f, 85f));

        // Far/back leg, trailing, smaller.
        Bone thighR = add(bones, new Bone("thighR", 18, hips).bindLocal(-0.075f, -0.01f, -97f));
        Bone shinR = add(bones, new Bone("shinR", 19, thighR).bindLocal(0.40f, 0f, 0f));
        add(bones, new Bone("footR", 20, shinR).bindLocal(0.36f, 0f, 95f));

        // -- System 3 cloth bones ------------------------------------------
        //
        // Contract section E reserved indices past 20 for these and section B
        // caps the skeleton at 32 (32 mat4 = 128 vec4, inside the GLES 3.0
        // guaranteed minimum of 256 vertex uniform vectors). Seven bones takes
        // the rig to 28 and leaves four spare, which is deliberate: the far
        // sleeve and the far hakama are the obvious next candidates and there
        // has to be room for them without another audit.
        //
        // Each chain is authored by naming the *garment rows* it drives, in
        // world-bind space, so the bone chain and the mesh rows it is skinned to
        // cannot drift apart -- the arrays below are the same numbers buildHaori
        // and buildSleeve use, and are named as constants for exactly that
        // reason. At bind every one of these bones sits exactly where the
        // authored garment already was, so the skinning matrices are identity
        // and switching the simulation off reproduces System 1's figure to the
        // bit.
        // The back rail is authored on RESAMPLED nodes, not one bone per garment
        // row. See backRailNodes(): the rail used to run one bone per row from
        // row 4 to row 9, and rows 8 and 9 are 58% and 100% dissolved, so two of
        // its five bones and two of its six particles were articulating ink that
        // is not drawn.
        float[][] backNodes = backRail().nodes();
        addClothChain(bones, hips, backNodes[0], backNodes[1], 0,
                "clothBackA", "clothBackB", "clothBackC", "clothBackD", "clothBackE");
        addClothChain(bones, hips, HAORI_FRONT_X, HAORI_FRONT_Y, HAORI_FRONT_ROW0,
                "clothFrontA", "clothFrontB", "clothFrontC");
        addSleeveChain(bones, handL);

        // -- System 3b face bones ------------------------------------------
        //
        // Three, not four, and the budget is why. The class comment above says
        // "21 body bones plus System 3's seven cloth bones is 28" — measured,
        // that was stale before 3b touched anything: the cloth chains add TEN
        // bones (five back, three front, two sleeve), so the skeleton stood at
        // 31 against a cap of 32. These three take it to 34 and the cap moves
        // to 36 (ink_skin.vert documents the uniform arithmetic), leaving two
        // slots for 3c's far sleeve / far hakama.
        // The eyelid is not its own bone — it is the eye bone's scaleY, which
        // collapses the lash-and-iris cluster toward the upper lash line where
        // the bone's origin deliberately sits. Gaze is the same bone's
        // translation. STYLE.md 4b.6's four channels land on three bones: brow
        // (translate+rotate), eyelid (eye scaleY), jaw (rotate), gaze (eye
        // translate).
        Bone headBone = bones.stream().filter(b -> "head".equals(b.name)).findFirst().orElseThrow();
        addFaceBone(bones, headBone, "brow", 0.096f, 0.062f);
        addFaceBone(bones, headBone, "eye", EYE_BIND_DX, EYE_BIND_DY);
        addFaceBone(bones, headBone, "jaw", -0.010f, -0.062f);

        return new Skeleton(bones);
    }

    /**
     * A face bone at a world-bind offset from the skull centre (head world
     * position + {@code HEAD_LOBE_DX/DY}), expressed in the head's local frame so
     * it inherits the head's lean and every stance the head takes.
     */
    private static void addFaceBone(List<Bone> bones, Bone head, String name, float dx, float dy) {
        Vector2 headWorld = bindWorldPos(head, new Vector2());
        float headRot = bindWorldRotDeg(head);
        float wx = headWorld.x + HEAD_LOBE_DX + dx;
        float wy = headWorld.y + HEAD_LOBE_DY + dy;
        float c = MathUtils.cosDeg(-headRot);
        float s = MathUtils.sinDeg(-headRot);
        float lx = (wx - headWorld.x) * c - (wy - headWorld.y) * s;
        float ly = (wx - headWorld.x) * s + (wy - headWorld.y) * c;
        add(bones, new Bone(name, bones.size(), head).bindLocal(lx, ly, 0f));
    }

    private static Bone add(List<Bone> bones, Bone b) {
        bones.add(b);
        return b;
    }

    // -- cloth bone authoring -------------------------------------------------

    /**
     * First haori row each cloth chain takes over. Rows above it stay on
     * hips/spine/chest.
     *
     * <p>Pass 2 moves the back rail's pivot from row 6 up to row 4 and the
     * front's to row 5, and the reason is the pass-1 review's third finding:
     *
     * <blockquote><b>Build the cloth. It currently produces no readable
     * mark.</b> The debug overlay shows the entire cloth simulation as three
     * chains of four particles... Measured, a tight hem-tip box registers
     * <b>0.00 px</b> across all 23 inter-frame steps, and the skirt silhouette is
     * the same shape through an entire knockback. Cloth is half this system's
     * title. Three chains of four particles cannot bend -- the hem needs enough
     * chain to curve and enough render weight to change the silhouette.
     * </blockquote>
     *
     * <p>Row 4 is at the hip line (y = 0.96 on the back rail, and the hips bone
     * is at 0.98), so the chain now starts where the garment stops being a torso
     * covering and starts being a hanging mass -- which is also where every
     * reference throws its ink cloud from. Six particles rather than four is the
     * difference between a hem that can hold an S and one that can only hold a
     * straight line at an angle.
     *
     * <p>This does not move the bind pose by a micron: every cloth bone is
     * authored to sit exactly on its row with its +x along the rail, so its
     * skinning matrix at bind is identity, exactly like the trunk blend it
     * replaces there. {@code s3-p2-bind-regress} is md5-identical to
     * {@code s1-p7-bind}.
     */
    private static final int HAORI_BACK_ROW0 = 4;
    private static final int HAORI_FRONT_ROW0 = 5;

    // The haori's two rails, hoisted to class scope because System 3's cloth
    // bones are authored onto the same rows the mesh is. Two copies of these
    // numbers would be a bone chain that silently stopped matching the garment
    // it drives, and nothing would fail loudly.
    //
    // Collar sits below the throat (head's lowest point is ~1.55) so the neck --
    // not the garment -- is what connects to the head.
    private static final float[] HAORI_FRONT_Y = {1.46f, 1.36f, 1.22f, 1.06f, 0.92f, 0.74f, 0.56f, 0.40f, 0.26f, 0.14f};
    private static final float[] HAORI_FRONT_X = {0.10f, 0.30f, 0.26f, 0.21f, 0.17f, 0.21f, 0.25f, 0.22f, 0.14f, 0.05f};
    private static final float[] HAORI_BACK_Y = {1.46f, 1.40f, 1.28f, 1.12f, 0.96f, 0.78f, 0.58f, 0.36f, 0.12f, -0.16f};
    private static final float[] HAORI_BACK_X = {-0.08f, -0.34f, -0.32f, -0.25f, -0.20f, -0.26f, -0.32f, -0.33f, -0.30f, -0.24f};

    /**
     * The back rail's authored edge dissolve, per row. Hoisted to class scope because
     * {@link Rail} reads it: a cloth chain must stop where the garment stops, and the garment
     * stops where this array says it does.
     */
    private static final float[] HAORI_BACK_DISSOLVE = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.14f, 0.58f, 1.0f};

    /** The near sleeve's drape dissolve, for its three drape rows (ribbon rows 4, 5, 6). */
    private static final float[] SLEEVE_DRAPE_DISSOLVE = {0.15f, 0.55f, 1.0f};

    /**
     * How dissolved the garment is allowed to be under the last particle of a cloth chain.
     *
     * <h2>Why a chain needs a stopping rule at all -- the pass-4 review's first finding</h2>
     *
     * <p>STYLE.md 7.1 carries a standing gate, and it is the only scalar gate that survived
     * four passes of trying to grade this cloth:
     *
     * <blockquote>Every simulated particle whose swept box falls outside the drawn figure
     * contributes nothing to the picture and <b>must not be counted as cloth resolution. It
     * should read zero.</b> It currently reads two of six.</blockquote>
     *
     * <p>The two were {@code back4} and {@code back5}. Every cloth chain in this file was
     * authored one bone per garment row, all the way to the last row -- and the last rows of
     * every rail are authored 0.55 to 1.00 dissolved on purpose, because STYLE.md 3 wants the
     * bottom of the figure to be ink smoke. So the ends of the chains were articulating rows
     * that are not drawn. Measured on {@code sim-sway}, the back rail's particles landed at
     * image y = 285, 321, 360, 403, <b>450, 505</b> against a drawn figure ending at y484 and a
     * back skirt whose ink stops being darker than a wash at about y440: {@code back5} stood on
     * clean paper, {@code back4} on the wet halo with its darkest neighbour at luminance 142
     * against 26-32 for the particles above it.
     *
     * <p>The fix is not to draw more down there. It is to stop spending the chain's articulation
     * below the garment, which is what this constant does: a chain reaches exactly as far as the
     * point where its own rail's authored dissolve reaches this value, and its bones are spread
     * at equal arc length over that reach.
     *
     * <h2>Why 0.20, measured rather than chosen</h2>
     *
     * <p>0.50 was the obvious value -- "more ink than paper" -- and it does not work. At 0.50
     * the back rail's last particle lands at image y442 and still reads "paints nothing":
     * darkest neighbour 144 against a gate of 122. The reason is that a rail is the strip's own
     * <em>boundary</em>, not its middle, so the fray band is eating the ink there from both
     * sides and a particle standing on the boundary at half dissolve has no mark within reach.
     * Swept 0.50 / 0.35 / 0.20 / 0.14 on {@code sim-sway}: the gate flips at 0.20, where the
     * last particle sits at y407..413 with a darkest neighbour of 31.7. 0.14 is too far -- it
     * falls below the sleeve drape's first authored dissolve (0.15), so that chain finds no
     * crossing at all and falls back to its full length.
     *
     * <p>Under-reaching costs almost nothing, because the rows below the chain's end still ride
     * the last bone and therefore still swing; over-reaching costs a particle. The asymmetry is
     * why this number should be set low rather than tuned to the edge.
     *
     * <p>Stated as a dissolve rather than as a length so that re-authoring a rail's fray moves
     * its chain with it, instead of silently stopping matching it. That is the rule the rails
     * themselves are already held to at the top of this section, applied one level up.
     */
    private static final float RAIL_REACH_DISSOLVE = 0.20f;

    /**
     * A cloth chain's geometry, resampled off an authored garment rail.
     *
     * <p>Given a rail polyline, the row the chain takes over, the authored dissolve per row and
     * a bone count, this produces the three things that must agree with each other and used to
     * be written down in three places:
     *
     * <ul>
     *   <li>{@link #nodes()} -- where the bones sit, at equal arc length over the reach;</li>
     *   <li>{@link #tailLength()} -- the last bone's segment, which {@code ClothSim} cannot
     *       derive because the last bone has no child to measure to;</li>
     *   <li>{@link #boneFor(int)} -- which bone each garment row is skinned to.</li>
     * </ul>
     *
     * <p>All three come from one arc-length parameterisation, so a chain cannot end up
     * articulating rows it does not reach or leaving rows it does reach on the body. Before this
     * existed the back rail's tail length lived in {@code RigSim} and its row-to-bone table
     * lived in {@code buildHaori}, and both were transcriptions of a layout asserted in a third
     * place.
     *
     * <h2>The side effect, which goes the right way</h2>
     *
     * <p>Because the bones now pack into the drawn part of the rail, every readable garment row
     * hangs one bone further down the chain than it did -- back row 5 off {@code clothBackB}
     * rather than {@code clothBackA}, row 6 off C, rows 7-9 off E. Rows past the end of the
     * chain ride the last bone, which is what an extrapolated hem does, and a bone with no
     * vertex sitting on it is not wasted: bones are hierarchical, so its rotation still reaches
     * every row below it. For the same per-joint bend the summed lever arm reaching back row 7
     * rises from 1.268 to 1.790 world units (+41%) and row 8's from 2.236 to 2.999 (+34%). The
     * rail articulates the drawn cloth a third harder while being 43% shorter, because none of
     * it is hanging in open paper any more.
     */
    private static final class Rail {

        private final float[] railX;
        private final float[] railY;
        private final int row0;
        private final int bones;
        private final float[] arc;
        private final float reach;

        Rail(float[] railX, float[] railY, float[] dissolve, int row0, int bones) {
            this.railX = railX;
            this.railY = railY;
            this.row0 = row0;
            this.bones = bones;
            int n = railX.length - row0;
            this.arc = new float[n];
            for (int i = 1; i < n; i++) {
                int r = row0 + i;
                arc[i] = arc[i - 1] + Vector2.len(railX[r] - railX[r - 1], railY[r] - railY[r - 1]);
            }
            float found = arc[n - 1];
            for (int i = 0; i + 1 < n; i++) {
                float d0 = dissolve[row0 + i];
                float d1 = dissolve[row0 + i + 1];
                if (d0 < RAIL_REACH_DISSOLVE && d1 >= RAIL_REACH_DISSOLVE) {
                    found = arc[i] + (RAIL_REACH_DISSOLVE - d0) / (d1 - d0) * (arc[i + 1] - arc[i]);
                    break;
                }
            }
            this.reach = found;
        }

        /** {@code {xs, ys}}, {@code bones + 1} nodes each, in the rail's own coordinates. */
        float[][] nodes() {
            float[] xs = new float[bones + 1];
            float[] ys = new float[bones + 1];
            for (int i = 0; i <= bones; i++) {
                float[] p = point(i * tailLength());
                xs[i] = p[0];
                ys[i] = p[1];
            }
            return new float[][] {xs, ys};
        }

        /** One segment, which is also the last bone's tail. */
        float tailLength() {
            return reach / bones;
        }

        /**
         * Which bone drives garment row {@code r}: the segment its arc position falls in.
         *
         * <p>Rotating bone {@code i} moves everything past bone {@code i}'s origin, so a vertex
         * inside segment {@code i} belongs to bone {@code i}. Rows past the end ride the last.
         */
        int boneFor(int r) {
            int i = r - row0;
            float a = i < arc.length ? arc[i] : arc[arc.length - 1];
            return Math.max(0, Math.min(bones - 1, (int) (a / tailLength() - 1e-4f)));
        }

        private float[] point(float s) {
            for (int i = 0; i + 1 < arc.length; i++) {
                if (s <= arc[i + 1] + 1e-6f) {
                    float t = (s - arc[i]) / (arc[i + 1] - arc[i]);
                    int r = row0 + i;
                    return new float[] {railX[r] + t * (railX[r + 1] - railX[r]),
                            railY[r] + t * (railY[r + 1] - railY[r])};
                }
            }
            int last = railX.length - 1;
            return new float[] {railX[last], railY[last]};
        }
    }

    /** The back haori rail's chain: five bones, resampled over the part of the rail that is drawn. */
    private static Rail backRail() {
        return new Rail(HAORI_BACK_X, HAORI_BACK_Y, HAORI_BACK_DISSOLVE, HAORI_BACK_ROW0, 5);
    }

    /**
     * The near sleeve's drape chain: two bones over the three drape nodes.
     *
     * <p>Same defect, found by the same gate as soon as it existed. The sleeve tip sat on drape
     * row 6, authored dissolve 1.00 -- nothing at all -- and measures "paints nothing" with a
     * darkest neighbour of 221 against paper 219. It had been passing only because the pass-4
     * splatter was throwing marks up to a hundred pixels past every silhouette and the sleeve
     * tip happened to land on one; pulling the splatter back (see {@code ink_skin.frag}) exposed
     * it in the same run. That is the same class of accident as the pass-4 review's own finding
     * that stray ink was moving the detected figure box.
     */
    private static Rail sleeveRail(float[] nx, float[] ny) {
        return new Rail(nx, ny, SLEEVE_DRAPE_DISSOLVE, 0, 2);
    }

    /** The last bone's segment on the back rail. Read by {@link RigSim}; see {@link Rail}. */
    public static float backRailTailLength() {
        return backRail().tailLength();
    }

    /** The last bone's segment on the sleeve drape. Read by {@link RigSim}; see {@link Rail}. */
    public static float sleeveRailTailLength() {
        float[][] n = sleeveDrapeNodes();
        return sleeveRail(n[0], n[1]).tailLength();
    }

    /** Near-sleeve drape nodes: distance along {@code handL} and offset across it, for the last three ribbon rows. */
    private static final float[] SLEEVE_NODE_D = {0.10f, 0.31f, 0.52f};
    private static final float[] SLEEVE_NODE_LATERAL = {-0.115f, -0.200f, -0.290f};

    // Skull geometry, public because System 3's hair has to root itself on the
    // same skull this mesh draws. Offsets are from the head bone's own origin,
    // in the head's frame; the skull is very nearly a circle of SKULL_RADIUS
    // about that point, and the topknot is a second lobe hung off it.
    public static final float HEAD_LOBE_DX = 0.012f;
    public static final float HEAD_LOBE_DY = 0.048f;
    public static final float SKULL_RADIUS = 0.150f;

    /**
     * The eye bone's bind offset from the head bone, shared with the tests that
     * recover the head's world rotation from the head-to-eye vector
     * ({@code FaceValueTest#boxesAt}).
     *
     * <p>Pass 2 moves the eye up and in: the pass-1 offset (0.098, 0.036) put the
     * eye at minus 8 degrees from the lobe centre — nostril height — and 3 px
     * inside the delivered silhouette, which is the review's own eye finding on
     * the foe ("at nostril height, on the silhouette edge") and the reason the
     * hero's 11x11 eye box read three columns of sky. The socket now sits where
     * 4b.4's degradation demands: behind the brow ridge, above nostril height,
     * with the break of light dipped dark across it (see buildFace).
     */
    public static final float EYE_BIND_DX = 0.078f;
    public static final float EYE_BIND_DY = 0.058f;
    public static final float TOPKNOT_ANGLE_DEG = 140f;
    public static final float TOPKNOT_DIST = 0.146f;
    public static final float TOPKNOT_RADIUS = 0.072f;

    /**
     * Builds a run of cloth bones along the garment rows {@code row0..} of an
     * authored rail, so that at bind each bone's origin sits on a row and its +x
     * axis points at the next one. That last property is what lets
     * {@link dev.starfall.sim.ClothSim} treat a bone's {@code bindRotDeg} as the
     * rest bend at its joint and a segment direction as the bone's own world
     * axis, with no link-offset conversion of the kind {@code IkChain} needs for
     * the trunk.
     */
    private static void addClothChain(List<Bone> bones, Bone parent,
                                       float[] railX, float[] railY, int row0, String... names) {
        Vector2 p = bindWorldPos(parent, new Vector2());
        float parentRot = bindWorldRotDeg(parent);
        float prevDeg = parentRot;
        Bone attach = parent;
        for (int i = 0; i < names.length; i++) {
            int r = row0 + i;
            float dx = railX[r + 1] - railX[r];
            float dy = railY[r + 1] - railY[r];
            float dirDeg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
            float lx;
            float ly;
            if (i == 0) {
                float ox = railX[r] - p.x;
                float oy = railY[r] - p.y;
                float c = MathUtils.cosDeg(-parentRot);
                float s = MathUtils.sinDeg(-parentRot);
                lx = ox * c - oy * s;
                ly = ox * s + oy * c;
            } else {
                lx = Vector2.len(railX[r] - railX[r - 1], railY[r] - railY[r - 1]);
                ly = 0f;
            }
            attach = add(bones, new Bone(names[i], bones.size(), attach).bindLocal(lx, ly, dirDeg - prevDeg));
            prevDeg = dirDeg;
        }
    }

    /**
     * The near sleeve's drape nodes in the hand's own frame, as {@code {xs, ys}}.
     *
     * <p>The hand frame rather than world space, because {@link Rail} only needs the shape of
     * the polyline to resample it, and computing it here twice -- once for the bones and once
     * for the tail length {@code RigSim} reads -- must give the same answer both times.
     */
    private static float[][] sleeveDrapeNodes() {
        int n = SLEEVE_NODE_D.length;
        float[] nx = new float[n];
        float[] ny = new float[n];
        for (int i = 0; i < n; i++) {
            nx[i] = SLEEVE_NODE_D[i];
            ny[i] = SLEEVE_NODE_LATERAL[i];
        }
        return new float[][] {nx, ny};
    }

    /** The near sleeve's drape, which hangs off the wrist and is the second-largest trailing mass in the figure. */
    private static void addSleeveChain(List<Bone> bones, Bone hand) {
        Vector2 h = bindWorldPos(hand, new Vector2());
        float handRot = bindWorldRotDeg(hand);
        float c = MathUtils.cosDeg(handRot);
        float s = MathUtils.sinDeg(handRot);
        float[][] local = sleeveDrapeNodes();
        // Resampled onto the drawn part of the drape -- see Rail. The three authored nodes are
        // dissolve 0.15, 0.55 and 1.00, so the old layout put sleeveB's tip on a row that is not
        // drawn at all.
        float[][] node = sleeveRail(local[0], local[1]).nodes();
        int n = node[0].length;
        float[] nx = new float[n];
        float[] ny = new float[n];
        for (int i = 0; i < n; i++) {
            float d = node[0][i];
            float across = node[1][i];
            nx[i] = h.x + d * c - across * s;
            ny[i] = h.y + d * s + across * c;
        }
        addClothChain(bones, hand, nx, ny, 0, "sleeveA", "sleeveB");
    }

    /** Bind-pose world rotation of a bone, summed up its parent chain. Every bind scale in this rig is 1. */
    private static float bindWorldRotDeg(Bone b) {
        float r = 0f;
        for (Bone c = b; c != null; c = c.parent) {
            r += c.bindRotDeg;
        }
        return r;
    }

    /**
     * Bind-pose world position of a bone, without a {@link Skeleton}. The cloth
     * bones have to be authored inside {@code buildSkeleton()}, before the
     * Skeleton (and therefore its inverse-bind matrices) exists.
     */
    private static Vector2 bindWorldPos(Bone b, Vector2 out) {
        if (b.parent == null) {
            return out.set(b.bindX, b.bindY);
        }
        bindWorldPos(b.parent, out);
        float pr = bindWorldRotDeg(b.parent);
        float c = MathUtils.cosDeg(pr);
        float s = MathUtils.sinDeg(pr);
        return out.set(out.x + b.bindX * c - b.bindY * s, out.y + b.bindX * s + b.bindY * c);
    }

    // -- Mesh -----------------------------------------------------------------

    /**
     * Builds every garment region into shared vertex/index buffers. A
     * throwaway helper object rather than static methods purely so region
     * builders can share {@code skeleton} and the seeded RNG without threading
     * them through every parameter list.
     */
    private static final class MeshAuthor {

        private final Skeleton skeleton;
        // Fixed seed: stain blotches must be identical every build, per the
        // no-Math.random()-determinism rule -- this is construction time, not
        // render time, so a seeded Random is exactly what's allowed.
        private final Random rnd = new Random(0xA1C0FFEEL);
        private final FaceParams face;
        private SkinnedMesh.Builder builder;

        MeshAuthor(Skeleton skeleton, FaceParams face) {
            this.skeleton = skeleton;
            this.face = face;
        }

        // -- the profile contour, shared by the silhouette and the skin --------
        //
        // STYLE.md 4b.3: "this single silhouette carries most of the character's
        // identity" — so FaceParams moves the CONTOUR, not a texture. Deviations
        // are scaled as a ratio to the hero's own parameters, which makes the
        // hero's contour bit-identical to the table three rig passes tuned and a
        // generated face a genuine reshaping of the same line.

        /** The face-edge stations, 0 = +X, negative runs down the face. */
        private static final float[] FACE_A = {42f, 25f, 10f, -2f, -12f, -22f, -34f, -50f, -66f};
        private static final float[] FACE_R = {0.150f, 0.146f, 0.134f, 0.162f, 0.130f, 0.126f, 0.154f, 0.106f, 0.090f};
        /** The neutral radius the notch/jut deviations are measured against. */
        private static final float FACE_R0 = 0.146f;

        /** {@link #contourR} interpolated at an arbitrary angle, for marks
         * that follow the head's edge rather than sit on a station. Above the
         * face table's 42-degree top station it follows the skull lobe. */
        private float contourRAt(float a) {
            if (a >= FACE_A[0]) {
                return 0.153f;
            }
            for (int i = 0; i < FACE_A.length - 1; i++) {
                if (a <= FACE_A[i] && a >= FACE_A[i + 1]) {
                    float u = (FACE_A[i] - a) / (FACE_A[i] - FACE_A[i + 1]);
                    return contourR(i) + (contourR(i + 1) - contourR(i)) * u;
                }
            }
            return contourR(FACE_A.length - 1);
        }

        /** Contour radius at station {@code i}, reshaped by this rig's face. */
        private float contourR(int i) {
            FaceParams hero = FaceParams.hero();
            float base = FACE_R[i];
            float dev = base - FACE_R0;
            float amp = 1f;
            float a = FACE_A[i];
            if (a <= 14f && a >= -28f) {
                // bridge notch, nose jut, lip setback: the nose group.
                amp = (0.55f + 0.90f * face.noseDepth()) / (0.55f + 0.90f * hero.noseDepth());
            } else if (a < -28f) {
                // chin and under-jaw: the jaw group, softened by age.
                amp = face.jawScale() / hero.jawScale();
            }
            return FACE_R0 + dev * amp;
        }

        /**
         * Draw order is occlusion order (contract F: straight alpha, no depth
         * test, painter's algorithm). Far side first so the torso and haori
         * bury most of it, per rig-fixes section 1; near side and head last so
         * they read on top and dominate.
         */
        SkinnedMesh buildBody() {
            builder = new SkinnedMesh.Builder();
            Bone chest = skeleton.bone("chest");
            Bone spine = skeleton.bone("spine");
            Bone hips = skeleton.bone("hips");

            // 0.40/0.36, which is what the far leg's bones actually are (the
            // near leg's are 0.44/0.40). Both legs used to be authored with the
            // near leg's lengths.
            buildLimbSliver(chainPoints("thighR", "shinR", "footR", 0.40f, 0.36f, 0.13f), 0.040f, 0.16f, 0.50f, 0.92f);
            buildHakama(skeleton.bone("thighR"), skeleton.bone("shinR"), skeleton.bone("footR"),
                    0.40f, 0.36f, 0.84f);
            buildFoot(skeleton.bone("footR"), 0.86f);
            buildLimbSliver(chainPoints("upperArmR", "forearmR", "handR", 0.19f, 0.14f, 0.09f), 0.028f, 0.18f, 0.06f, 0.14f);
            buildSleeve(skeleton.bone("upperArmR"), skeleton.bone("forearmR"), skeleton.bone("handR"),
                    0.19f, 0.14f, 0.09f, 0.22f, 0.78f, null);

            buildTrunk(hips, spine, chest, skeleton.bone("neck"), skeleton.bone("head"));
            buildHaori(hips, spine, chest);

            // The worn pair, over the haori and under everything on the near
            // side. Their whole point is that they leave the garment silhouette
            // behind the hip and carry on into open paper.
            buildDaisho(hips, spine);

            // Near leg *under* its own hakama, not over it (rig-fixes-3 item 3):
            // a solid sliver composited on top of the skirt is what read as a
            // translucent tube with a bright centre seam.
            buildLimbSliver(chainPoints("thighL", "shinL", "footL", 0.44f, 0.40f, 0.14f), 0.055f, 1f, 0.62f, 1f);
            buildHakama(skeleton.bone("thighL"), skeleton.bone("shinL"), skeleton.bone("footL"),
                    0.44f, 0.40f, 1f);
            buildFoot(skeleton.bone("footL"), 1f);

            // Over the skirt and over the scabbards' forward ends, which is
            // physically where a sash sits and what makes it read as a wrap
            // rather than a stripe.
            buildObi(hips, spine);

            // The shoulder mass, drawn over everything on the trunk. This is the
            // widest horizontal in the figure by construction (rig-fixes-3 item 1).
            buildShoulderMantle(spine, chest);

            buildHead(skeleton.bone("head"));

            buildLimbSliver(chainPoints("upperArmL", "forearmL", "handL", 0.30f, 0.26f, 0.10f), 0.045f, 1f, 0.10f, 0.25f);
            buildSleeve(skeleton.bone("upperArmL"), skeleton.bone("forearmL"), skeleton.bone("handL"),
                    0.30f, 0.26f, 0.10f, 0.42f, 1f,
                    new Bone[] {skeleton.bone("sleeveA"), skeleton.bone("sleeveB")});
            // Last, and in this order, because this is the one cluster the
            // references spend their whole interior budget on and nothing may
            // composite over it. See buildGrip.
            buildHand(skeleton.bone("handL"), skeleton.bone("blade"));
            buildTsuka(skeleton.bone("blade"));
            buildTsuba(skeleton.bone("blade"));

            return builder.build();
        }

        /**
         * The nagasa (edge length) as a fraction of the figure's own height,
         * heel to crown.
         *
         * <h2>0.40 was anatomy; 0.55 is the corpus, and STYLE.md says the corpus wins</h2>
         *
         * <p>docs/system1-rig-fixes.md section 3 asked for 0.75, which rendered at
         * roughly 90% of body height and never terminated inside the frame, and it was
         * corrected to <b>0.40</b> on the argument that "a katana's nagasa is about
         * 70 cm on a 170 cm swordsman". That argument is sound about swords and wrong
         * about these paintings, and nobody had measured the paintings.
         *
         * <p>Measured, on all three Family B images, with an independently written
         * reader: the crossed pair of blades is one cool-bright component
         * ({@code L > 1.30 x} the row's own background, 2x2 opening, 8-connected) whose
         * bounding diagonal is
         *
         * <pre>
         *   image 3  14,778 px  x272..568 y317..456  diag 327 px = 0.487 figure heights
         *   image 4  19,414 px  x348..581 y117..444  diag 402 px = 0.595
         *   image 5  16,992 px  x342..627 y122..460  diag 442 px = 0.655
         * </pre>
         *
         * <p>and image 3's left blade runs tsuba (390,465) to kissaki (560,135), which
         * is 379 px on a 672 px figure -- <b>0.56 of a figure height of steel per
         * duellist</b>. The review of pass 3 measured the same thing from the other end
         * and asked for "about 0.50 of a figure height of blade per duellist and a
         * crossed figure spanning >= 0.49", against a delivered 0.286 (hero) and 0.154
         * (foe).
         *
         * <p>STYLE.md's preamble decides it: "when this document and the reference
         * images disagree, <b>the reference images win</b>". The blade stays exactly as
         * thin as it was -- {@code halfWidth} is in absolute world units and is not
         * scaled with the length, so STYLE.md 5's "sliver" is unchanged and the blade
         * gets longer rather than bigger.
         */
        static final float BLADE_NAGASA_FRACTION = 0.55f;

        /** Heel (y=0.13) to crown (y=1.83) in the world-bind-space units of this file. */
        private static final float FIGURE_HEIGHT = SamuraiRig.FIGURE_HEIGHT;

        SkinnedMesh buildBlade() {
            builder = new SkinnedMesh.Builder();
            Bone blade = skeleton.bone("blade");

            // Thin, with a real kissaki inside the frame and a slight sori
            // curvature convex away from the edge so it doesn't read as a
            // straight stick. Single bone, so the curve has to be baked into the
            // authored geometry rather than a joint chain.
            float len = BLADE_NAGASA_FRACTION * FIGURE_HEIGHT;
            // Thirteen rows rather than seven. The last fifth is where the
            // profile has to resolve a point, and at seven rows the whole taper
            // was two quads: the rasteriser dropped it and the blade ended by
            // fading out at a constant two pixels rather than converging.
            int n = 13;
            float[] d = new float[n];
            float[] halfWidth = new float[n];
            float[] bow = new float[n];
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                d[i] = t * len;
                // Real blade geometry, which is not a triangle: a gentle taper
                // from the habaki to the yokote at ~0.86, then the kissaki
                // converging to a genuine point over the last seventh. 0.0165
                // is 7 px of blade at capture framing -- STYLE.md 5's "sliver".
                float body = MathUtils.lerp(0.0165f, 0.0120f, Math.min(t / 0.86f, 1f));
                float point = 1f - MathUtils.clamp((t - 0.86f) / 0.14f, 0f, 1f);
                halfWidth[i] = body * point * point;
                // Torii-zori: peak curvature near the middle, zero at both ends.
                // 0.024 is about 5 px of bow over a 150 px blade, which is the
                // proportion the reference katana carries.
                bow[i] = 0.024f * MathUtils.sin(t * MathUtils.PI);
            }
            short[] left = new short[n];
            short[] right = new short[n];
            float flow = angleToU(skeleton.worldRotationDeg(blade.index));
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                Vector2 pl = alongBone(blade, d[i], halfWidth[i] + bow[i]);
                Vector2 pr = alongBone(blade, d[i], -halfWidth[i] + bow[i]);
                left[i] = builder.vertex(pl.x, pl.y, 0f, t, 0f, 0f, 0f, flow, blade.index, 1f, blade.index, 0f);
                right[i] = builder.vertex(pr.x, pr.y, 1f, t, 0f, 0f, 0f, flow, blade.index, 1f, blade.index, 0f);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(left[i], left[i + 1], right[i + 1], right[i]);
            }
            return builder.build();
        }

        // -- haori: the single most important mesh in the system -------------

        /**
         * Asymmetric, shoulder-heavy: independently authored front (+X) and
         * back (-X) contours rather than a mirrored halfWidth, per rig-fixes
         * section 2. Widest at the shoulder row, pinched at the waist, flaring
         * hard again below -- and the flare is lopsided, trailing about 1.35x
         * further behind than in front, with the front hem rising to clear the
         * lower leg while the back keeps dropping into ink.
         *
         * <p>Dissolve is genuinely 0 through the collar/shoulder/waist rows --
         * the solid core the references keep -- and only climbs from the hip
         * row down. Rows stay wide (front+back combined 0.5-1.4 units) through
         * that solid stretch specifically so the shader's boundary-distance
         * fray band (which eats a fixed UV fraction of every strip regardless
         * of authored dissolve) still leaves real pixels of interior untouched.
         */
        private void buildHaori(Bone hips, Bone spine, Bone chest) {
            float[] frontY = HAORI_FRONT_Y;
            // Debt D5, the mass half of it. Measured across the p6 bind capture
            // the garment ran 144 px wide at the chest, pinched to 79 at the
            // waist and only recovered to 90 at the knee -- so the lower third
            // was not only pale, it was the *narrowest* part of the figure below
            // the collar. The debt's own postscript on contrast is explicit that
            // what separates a good capture from a washed-out one is coverage
            // and mass rather than value, and no amount of pooling can darken a
            // band that has nothing in it.
            //
            // Reference images 1 and 2 make the thigh-and-knee cloud the widest
            // passage in the painting, wider than the shoulders. That is not
            // available here: rig-fixes-3 item 1 established the shoulder mantle
            // as the widest horizontal (1.04 units, 234 px) to kill the cone
            // silhouette that read as a wraith, and inverting it again would
            // undo three passes. So the knee cloud goes to 0.57 units (128 px),
            // second-widest in the figure and 1.8x narrower than the shoulder --
            // an ink cloud around the knees rather than a rival to the mantle.
            // The back rows widen more than the front, which is the 1.35:1
            // trailing asymmetry rig-fixes section 2 asks for and is where
            // reference 1 throws its cloud.
            float[] frontX = HAORI_FRONT_X;
            float[] backY = HAORI_BACK_Y;
            float[] backX = HAORI_BACK_X;
            // Genuinely 0 through the shoulder-to-waist run (rows 0-3): the
            // dense core. Climbs only once the flare starts (row 4+).
            // The fray starts lower than revision 2 had it and then goes off a
            // cliff. Pass 2 measured luminance 60 at the mid-torso against
            // 137-160 at the hem, and the cause is coverage rather than value:
            // a row that is a third dissolved is a third transparent, so paper
            // shows through it and it measures pale no matter how wet it is
            // authored. Keeping the skirt genuinely opaque down to the last two
            // rows is what lets the pigment pooling below actually print.
            //
            // Debt D5. The cliff moves down by one row. Row 6 sits at world
            // y=0.56/0.58, which is the knee (the near knee lands at y=0.532),
            // and that row is where reference images 1 and 2 put the heaviest
            // black in the whole painting. Pass 6 was already 0.05 dissolved on
            // the back at that row and 0.10-0.20 the row below, so the fray was
            // opening exactly where the ink is meant to be densest.
            float[] dissolveF = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.06f, 0.42f, 1.0f};
            float[] dissolveB = HAORI_BACK_DISSOLVE;
            // rig-fixes-3 item 4, the ink gravity. The chest is now the *lightest*
            // part of the wash and the pigment pools to near-black in the row
            // immediately above the fray line -- pass 2 measured luminance 60 at
            // the mid-torso against 137-160 at the hem, i.e. exactly backwards.
            //
            // Debt D5 again, and this is the change that gives the leg tubes
            // somewhere to read *against*. The wetness peak used to sit at row 8
            // (world y=0.26, i.e. mid-shin) at the same 1.0 the hakama carries,
            // so the garment cloud and the legs inside it were the identical
            // value and merged into one column -- the D1 lesson (four luminance
            // levels on a thirty-level ramp is one mark, however carefully it is
            // shaped) applied to the lower body instead of the grip.
            //
            // So the peak moves up to rows 6-7, the knee, and the last two rows
            // come back down to a dilute smoke. That is also what the reference
            // does: the black is at the thighs and knees, and below the ankles
            // the painting goes pale again and the *legs* are what stay dark.
            // Row 9 is held at 0.62 rather than lower because ink_skin.frag
            // opens the cream reserves below wetness 0.55 and the paper tooth
            // below 0.45, and either firing here would bleach the hem again.
            float[] wetF = {0.03f, 0.06f, 0.13f, 0.26f, 0.46f, 0.72f, 0.95f, 1.0f, 0.86f, 0.62f};
            float[] wetB = {0.04f, 0.09f, 0.18f, 0.32f, 0.54f, 0.80f, 1.0f, 1.0f, 0.84f, 0.60f};
            // Ochre lives in the chest and ribs, not the skirt: down at the
            // thigh it printed as a bright warm blob right where item 4 wants
            // the darkest ink in the figure.
            // Ochre lives in the chest and ribs, not the skirt. It is also the
            // one channel with real authority over value here: the resolve lets
            // the stain *displace* the ink rather than tint it, so a stained
            // chest is genuinely lighter and warmer than an unstained skirt.
            // That is both what references 1 and 2 do -- rust bleeding through
            // the breastplate over a near-black hakama -- and the only mesh-side
            // lever that moves the ink gravity of item 4 the right way.
            //
            // Rows 5-6 are new. The matched-scale comparison of STYLE.md 11.0
            // lists "a loud ochre bloom over the thigh" among the parts
            // reference image 1 still resolves at this figure height, and it is
            // the single loudest mark in that painting. It goes on the garment
            // panel over the thigh, not on the hakama ribbon: stain authored
            // down a limb strip interpolates into a pair of bright vertical bars
            // (the fault rig-fixes-3 item 3 fails on), whereas on the haori's
            // wide front panel the shader's blotchy gate has room to cut it into
            // an irregular bloom. Held under 0.29 so it stays on the soft side
            // of ink_skin.frag's fitting threshold and blooms rather than
            // printing as a flat leather tone.
            //
            // Debt D5 moves the bloom up one row. Rows 5-6 are world y=0.74 and
            // 0.56 -- the second of those is the knee, and OCHRE is luminance
            // 130 against an ink floor of 26, so a bloom there was lifting the
            // one band that has to be the darkest in the picture by twenty
            // levels. Reference image 1 puts its bloom over the *thigh*, above
            // the knee, which is rows 4-5. It is also stronger there now: it is
            // the loudest mark in that painting and this is the only place in
            // the figure it is allowed to be loud.
            float[] stainF = {0f, 0.07f, 0.15f, 0.17f, 0.14f, 0.19f, 0.04f, 0f, 0f, 0f};
            float[] stainB = {0f, 0.08f, 0.16f, 0.18f, 0.11f, 0.07f, 0f, 0f, 0f, 0f};

            int n = frontY.length;
            short[] front = new short[n];
            short[] back = new short[n];
            // The rows the cloth chains take over, per rail. The pivot row stays
            // mostly on the hips -- a vertex sitting exactly on a bone's origin
            // is unmoved by that bone's rotation, so this blend is about how the
            // weights *interpolate* across the quads above it, not about the
            // pivot row itself. Below that each row hangs off the bone whose tip
            // it is, which is the standard chain weighting: rotating bone A moves
            // everything from its tip outward and nothing above it.
            //
            // Pass 2 hands five bones to the back rail and three to the front,
            // starting two rows higher. The review measured pass 1's hem tip
            // moving 0.00 px across every inter-frame step of a knockback, and a
            // hem that cannot move is not a cloth simulation -- it is a garment
            // with a chain drawn on it in the debug overlay.
            Bone backA = skeleton.bone("clothBackA");
            Bone backB = skeleton.bone("clothBackB");
            Bone backC = skeleton.bone("clothBackC");
            Bone backD = skeleton.bone("clothBackD");
            Bone backE = skeleton.bone("clothBackE");
            Bone frontA = skeleton.bone("clothFrontA");
            Bone frontB = skeleton.bone("clothFrontB");
            Bone frontC = skeleton.bone("clothFrontC");
            BoneBlend[] clothF = new BoneBlend[n];
            BoneBlend[] clothB = new BoneBlend[n];
            // The back rail's pivot keeps the *spine* share {@code trunkBlend}
            // gave it, with only the hips share handed to the chain. The back
            // rail reads that blend at 1.05x its row parameter, so row 4 lands at
            // s = 0.467, where the old weights were spine 0.417 / hips 0.583, and a
            // scene that runs no cloth simulation -- every System 2 IK scene --
            // leaves clothBackA at bind, i.e. rigidly on the hips. Handing the
            // whole row to the chain therefore silently took the spine's lean out
            // of the garment in those scenes: measured against a pre-System-3
            // capture of ik-gesture it moved 76 pixels of a 518,400 px frame by
            // more than four levels. Small, and still a regression in a system
            // this pass does not own.
            //
            // Pass 5 resamples the rail (see backRailNodes()) so every one of its six
            // particles stands on drawn cloth, which is STYLE.md 7.1's standing reach gate.
            // The rows below therefore no longer map one-to-one onto bones: the mapping is
            // computed from the rail's own arc so that it cannot drift from where the bones
            // actually sit. Measured, it comes out one bone lower per row than the table it
            // replaces -- row 5 on backB rather than backA, and so on down -- which is the
            // whole gain: each readable row now hangs off one more joint of chain.
            Bone[] backChain = {backA, backB, backC, backD, backE};
            Rail rail = backRail();
            clothB[4] = new BoneBlend(spine, 0.417f, backA, 0.583f);
            for (int r = HAORI_BACK_ROW0 + 1; r < n; r++) {
                Bone b = backChain[rail.boneFor(r)];
                clothB[r] = new BoneBlend(b, 1f, b, 0f);
            }
            // Pass 3 tried moving the front rail's pivot from row 5 to row 4 and
            // reverted it. Recorded because the reasoning was sound and the
            // measurement refuted it, which is the useful half.
            //
            // The reasoning: STYLE.md 11.3's box for the upper skirt --
            // fig[0.15, 0.55, 0.70, 0.13], x466..602 y308..353 on the sim-extreme
            // window -- contains the back rail's rows 5 and 6 (a SceneProbe run
            // puts those particles at y=321 and y=360) and, across the figure,
            // the *front* rail's rows 4 and 5. With the front pivot at row 5,
            // frontA's first moving vertex is row 6 at y=365, four pixels below
            // the box, so every front-edge pixel inside the graded region is
            // welded to the hips. Registration fits one translation to the whole
            // box, so half of it could not lag.
            //
            // The measurement: moving the pivot changed the graded lag by under
            // a tenth of a frame, and cost real silhouette -- the longer front
            // panel blows back under the steady breeze and the skirt visibly
            // narrows. The control in docs/system3-debt.md says why no weighting
            // change could have worked: with the cloth clamped rigid that box
            // still reads +0.34 frames, so the statistic's whole range there is
            // about half a frame and the box is mostly obi, thigh and scabbard.
            clothF[5] = new BoneBlend(hips, 0.45f, frontA, 0.55f);
            clothF[6] = new BoneBlend(frontA, 1f, frontA, 0f);
            clothF[7] = new BoneBlend(frontB, 1f, frontB, 0f);
            // The front rail's last two rows share the last bone: they are 0.16
            // units of frayed smoke that clears the leg, so a fourth bone would
            // buy a swing nobody can see against the one spare the 32-bone cap
            // has left after the sleeve.
            clothF[8] = new BoneBlend(frontC, 1f, frontC, 0f);
            clothF[9] = new BoneBlend(frontC, 1f, frontC, 0f);

            for (int i = 0; i < n; i++) {
                float s = i / (float) (n - 1);
                BoneBlend bbF = clothF[i] != null ? clothF[i] : trunkBlend(s, hips, spine, chest);
                BoneBlend bbB = clothB[i] != null ? clothB[i] : trunkBlend(Math.min(1f, s * 1.05f), hips, spine, chest);
                float flowF = angleToU(280f - 15f * s);
                float flowB = angleToU(255f - 45f * s);
                front[i] = builder.vertex(frontX[i], frontY[i], 1f, s, dissolveF[i], wetF[i],
                        stainAt(stainF[i]), flowF, bbF.boneA.index, bbF.weightA, bbF.boneB.index, bbF.weightB);
                back[i] = builder.vertex(backX[i], backY[i], 0f, s, dissolveB[i], wetB[i],
                        stainAt(stainB[i]), flowB, bbB.boneA.index, bbB.weightA, bbB.boneB.index, bbB.weightB);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(back[i], back[i + 1], front[i + 1], front[i]);
            }

            // -- the trailing panels, STYLE.md 3 and 3.1 ---------------------
            //
            // The pass-2 review's ruling, and the sentence this is built from:
            //
            //   "A silhouette with a smooth continuous boundary cannot express a
            //    fold no matter how the bones underneath it rotate."
            //
            // Everything above is one quad strip between two rails. Its
            // boundary is two polylines, so the garment can only ever be a
            // shape that widens and narrows: when the chain bends, the outline
            // moves, and it is still one outline. There is no second edge for it
            // to be an edge *against*, which is what a fold is. Coverage
            // measured 97.0% on `torso` and 97.7% on `hips` -- §3.3 says ink
            // skips and here it did not, because there is nothing for it to skip
            // between.
            //
            // So the rows the chains own get three overlapping leaves, each
            // attached along the rail and bulging past it, each terminating in
            // its own frayed boundary. Three properties matter and each is
            // deliberate:
            //
            //   * The outer edge is the strip's own u = 1 boundary, so
            //     ink_skin.frag's boundary-distance fray band cuts it into
            //     flecks; the inner edge sits at u = 0.5, i.e. interior, so the
            //     leaf melts into the solid mass rather than drawing a seam
            //     across it. v carries the main rail's row parameter and never
            //     reaches 0 or 1, so the fray happens on the trailing edge only.
            //   * The offset closes to near zero at both ends, so a leaf is a
            //     brush mark with two tapered ends rather than a flap with a
            //     cut across it.
            //   * Each leaf's outer edge is skinned one or two bones further
            //     down the chain than its inner edge. That is the whole point:
            //     when the chain bends, the leaves rotate by different amounts,
            //     their boundaries cross and separate, and the gaps between them
            //     are the fold. Weighting them identically to the rail would
            //     re-draw the same smooth outline three times.
            //
            // Authored fray now reaches rows 5 and 6, where dissolveB was 0.0
            // flat. The solid rail underneath is untouched -- the review is
            // explicit that rows 5-6 are where reference images 1 and 2 put the
            // heaviest black, so the mass stays and the *edge* frays.
            // Every leaf closes back onto the rail at its last row, and the
            // downward part of the last offset is small. The first version let
            // leaf two carry 0.12 units of drop at row 9 with almost no width,
            // which draws a one-pixel vertical tail hanging past the feet -- a
            // hard thin mark on open paper, and §10's "a hem that is a straight
            // line" in miniature.
            trailingLeaf(backX, backY, 4, new float[] {-0.014f, -0.062f, -0.112f, -0.086f, -0.022f},
                    new float[] {-0.008f, -0.026f, -0.020f, -0.038f, -0.026f},
                    new float[] {0.24f, 0.40f, 0.44f, 0.58f, 0.88f}, wetB, dissolveB, clothB, 1, 255f, 45f);
            trailingLeaf(backX, backY, 5, new float[] {-0.024f, -0.140f, -0.150f, -0.036f},
                    new float[] {-0.030f, -0.055f, -0.098f, -0.052f},
                    new float[] {0.38f, 0.48f, 0.62f, 0.92f}, wetB, dissolveB, clothB, 2, 255f, 45f);
            // The front leaf is half the reach of the back ones for the reason
            // the front rail is limited harder everywhere else in this file: it
            // has a thigh 0.2 units behind it, and a front panel free to fly is
            // a panel that intersects the leg it hangs in front of.
            trailingLeaf(frontX, frontY, 5, new float[] {0.014f, 0.056f, 0.040f, 0.010f},
                    new float[] {-0.010f, -0.016f, -0.028f, -0.022f},
                    new float[] {0.26f, 0.38f, 0.56f, 0.90f}, wetF, dissolveF, clothF, 1, 280f, 15f);
        }

        /**
         * One overlapping trailing panel on a haori rail: a leaf attached along
         * the rail from {@code row0}, bulging outward by {@code outX}/{@code outY}
         * and closing again, whose outer boundary is the strip's own {@code u = 1}
         * edge and therefore frays.
         *
         * <p>The inner edge carries the rail's own authored dissolve rather than
         * zero. Zero was wrong and visibly so: it laid a fully solid panel over
         * rows 7 and 8, where the rail is 0.14 and 0.58 dissolved, and filled in
         * the ink smoke §3 asks the bottom third of the figure to be. A leaf is a
         * second sheet of the same cloth, so it dissolves the same way.
         *
         * @param blend    the rail's per-row bone blends. The inner edge uses the
         *                 blend of its own row; the outer edge uses the blend
         *                 {@code boneShift} rows further down the chain, which is
         *                 what makes the leaves separate under a bend instead of
         *                 tracing the same curve.
         * @param flowDeg  stroke direction at the top of the leaf, and how far it
         *                 turns by the bottom -- §3.3's dry-brush streaks run
         *                 along the cloth, not across the screen.
         */
        /**
         * How far inside the rail a leaf's inner edge sits, as a fraction of that
         * leaf's own outward reach at the same row.
         *
         * <p>Not a taste. A leaf's inner edge is a geometric boundary authored at
         * {@code u = 0.5}, i.e. deep interior as far as {@code ink_skin.frag}'s
         * boundary-distance fray is concerned, so it prints as fully solid ink and
         * then stops dead. Authored exactly on the rail -- which is where the
         * first version of this put it -- that solid edge lands on the main
         * strip's own {@code u = 0} boundary, the one place the main strip is
         * <em>fraying</em>, and the result is a one-pixel hard line down the whole
         * back of the skirt, visible at 1x in the first capture. It is the failure
         * §3 opens by banning: "nothing in this game has a hard edge except the
         * blades."
         *
         * <p>Pushing the inner edge well inside the opaque mass buries it under
         * ink that is already solid there, so the only boundary a leaf contributes
         * to the silhouette is its frayed outer one.
         */
        private static final float LEAF_INSET = 0.85f;

        private void trailingLeaf(float[] railX, float[] railY, int row0,
                                  float[] outX, float[] outY, float[] outerDissolve,
                                  float[] railWet, float[] railDissolve, BoneBlend[] blend, int boneShift,
                                  float flowDeg, float flowTurnDeg) {
            int rows = outX.length;
            int n = railY.length;
            short[] inner = new short[rows];
            short[] outer = new short[rows];
            for (int i = 0; i < rows; i++) {
                int r = Math.min(n - 1, row0 + i);
                int rOuter = Math.min(n - 1, r + 1 + boneShift);
                float s = r / (float) (n - 1);
                float flow = angleToU(flowDeg - flowTurnDeg * s);
                // Both edges hang off the chain, one row apart. The first
                // version weighted the inner edge to the rail's own row, which
                // for the top row of a leaf is the *pivot* -- rigid on the hips
                // by construction -- so the leaves added a large solid mass to
                // the upper skirt that could not lag at all, and the measured
                // lag of the graded box went down rather than up. A leaf is a
                // separate panel of cloth; nothing about it belongs to the body.
                BoneBlend bi = blend[Math.min(n - 1, r + 1)];
                BoneBlend bo = blend[rOuter] != null ? blend[rOuter] : bi;
                float wet = railWet[r];
                // The trailing edge of a wash is where pigment collects (§3.4),
                // and the lift is small because the rail underneath is already
                // at or near the ceiling through these rows.
                float wetOut = Math.min(1f, wet + 0.06f);
                inner[i] = builder.vertex(railX[r] - LEAF_INSET * outX[i], railY[r] - LEAF_INSET * outY[i],
                        0.5f, s, railDissolve[r], wet,
                        0f, flow, bi.boneA.index, bi.weightA, bi.boneB.index, bi.weightB);
                outer[i] = builder.vertex(railX[r] + outX[i], railY[r] + outY[i], 1f, s,
                        outerDissolve[i], wetOut,
                        0f, flow, bo.boneA.index, bo.weightA, bo.boneB.index, bo.weightB);
            }
            for (int i = 0; i < rows - 1; i++) {
                builder.quad(inner[i], inner[i + 1], outer[i + 1], outer[i]);
            }
        }

        private record BoneBlend(Bone boneA, float weightA, Bone boneB, float weightB) {
        }

        /** s=0 at the collar, s=1 at the hem: chest -> spine -> hips. Rows at or below {@link #HAORI_BACK_ROW0} / {@link #HAORI_FRONT_ROW0} use the cloth chains instead. */
        private static BoneBlend trunkBlend(float s, Bone hips, Bone spine, Bone chest) {
            if (s < 0.15f) {
                return new BoneBlend(chest, 1f, chest, 0f);
            }
            if (s < 0.35f) {
                float t = (s - 0.15f) / 0.20f;
                return new BoneBlend(chest, 1f - t, spine, t);
            }
            if (s < 0.55f) {
                float t = (s - 0.35f) / 0.20f;
                return new BoneBlend(spine, 1f - t, hips, t);
            }
            return new BoneBlend(hips, 1f, hips, 0f);
        }

        // -- hakama: wide-legged trousers, dissolve at the ankles -------------

        private void buildHakama(Bone thigh, Bone shin, Bone foot,
                                  float thighLen, float shinLen, float scale) {
            // Revision 3: every row now hangs off the thigh or the shin, never
            // off the foot. footL/footR bind to world rotation 0 (they point
            // +X), so a half-width on a foot row offsets *vertically* -- the two
            // trailing rows of revision 2 were therefore a pair of 0.66-tall
            // vertical smears thrown forward from each ankle, and between them
            // they were the widest thing in the figure by a wide margin.
            // Debt D5 / "legs and feet". The row at shin 0.40 is the *ankle*
            // (world y=0.133 on the near leg, which is where SamuraiRig's units
            // put the sole), and pass 6 had no row there at all: it went shin
            // 0.38 at dissolve 0.30 straight to shin 0.54 at dissolve 1.0, i.e.
            // the tube was already three-tenths erased above the ankle and gone
            // entirely below it. Measured on the p6 bind capture, ink coverage
            // over the leg band collapsed from 69% at the knee to 23% at the
            // ankle and 12% below it. That is the "ends in erasure" the debt
            // names: the figure has no ankle, so it can have no foot.
            //
            // Now the tube stays genuinely solid through the ankle and only the
            // single row *below* the sole frays, which is ink smoke pooling on
            // the ground rather than a leg being deleted.
            // <strong>Fractions of the actual bone lengths, not absolute
            // distances.</strong> Pass 6 hard-coded the near leg's 0.44/0.40 for
            // both legs, but the far leg's bones are 0.40/0.36 -- so every row
            // below the far knee was authored past where that leg actually is,
            // and the last one landed at world y=0.037 against a far sole at
            // y=0.216. The far leg was rendering a 40 px dark spike straight
            // through its own foot and into the ground, which is a large part of
            // why nothing down there read as a foot: the mark was there, but it
            // was drawn as a tapering spear.
            RibbonPoint[] pts = {
                    RibbonPoint.of(thigh, 0.05f * thighLen),
                    RibbonPoint.of(thigh, 0.55f * thighLen),
                    RibbonPoint.blended(thigh, thighLen, shin, 0.4f),      // the knee
                    RibbonPoint.of(shin, 0.45f * shinLen),
                    RibbonPoint.of(shin, 0.80f * shinLen),
                    RibbonPoint.blended(shin, shinLen, foot, 0.35f),       // the ankle
                    RibbonPoint.blended(shin, 1.35f * shinLen, foot, 0.6f), // below the sole
            };
            // Wide-legged at the thigh, gathered at the ankle -- which is what a
            // hakama actually does, and is the shape in references 1 and 2 where
            // the leg below the knee is a narrow dark tube. The last row flares
            // slightly again: what is below the ankle is not cloth any more, it
            // is the wet pool the figure is standing in.
            float[] halfWidth = scaled(scale, 0.088f, 0.112f, 0.104f, 0.084f, 0.068f, 0.060f, 0.076f);
            // The ankle row is 0.03 rather than 0.08. frayPx on a 27 px tube is
            // 4.5 px at zero dissolve and 8.9 px at 0.08, and the wider band was
            // opening a bright horizontal gap between the bottom of the tube and
            // the top of the foot -- the join being visibly cut is worse than the
            // join being visibly soft. The last row is 0.94: what is below the
            // sole should be a scatter of flecks in the wet ground, and at 0.88
            // it was still solid enough to print as a grey puddle.
            float[] dissolve = {0f, 0f, 0f, 0f, 0f, 0.03f, 0.94f};
            // Far leg gets a higher dissolve floor throughout -- it never reads
            // as fully solid, which is what "lower contrast / recedes behind
            // the body" means when both legs share one InkMaterial draw call.
            float contrastFloor = scale < 1f ? 0.10f : 0f;
            for (int i = 0; i < dissolve.length; i++) {
                dissolve[i] = Math.max(dissolve[i], contrastFloor);
            }
            // Hakama and legs are the darkest masses in the figure (rig-fixes-3
            // item 4): wetness climbs hard and holds at the ceiling from the
            // knee all the way through the ankle. It has to *hold* rather than
            // peak-and-fall, because the haori's own cloud is now authored to
            // fall away below the knee (0.86 then 0.62) -- that difference is
            // the value step that makes the tube read as a leg inside a garment
            // instead of the two merging into one column, which is the same
            // lesson the grip cluster learned in D1.
            //
            // Stain is zero throughout -- the ochre in the lower body printed as
            // a pair of bright vertical bars down the legs, which is exactly the
            // "bright vertical seam" item 3 fails on.
            float[] wetness = {0.50f, 0.72f, 0.92f, 1.0f, 1.0f, 1.0f, 0.88f};
            float[] stainBase = {0f, 0f, 0f, 0f, 0f, 0f, 0f};
            ribbon(pts, halfWidth, dissolve, wetness, stainBase);
        }

        // -- feet: the part the matched-scale comparison says is missing -------

        /**
         * A small solid wedge on the foot bone, authored the way the grip
         * cluster of D1 is and for the same reason: a foot at this framing is
         * roughly 38 x 16 px, and the shader's fray band is an absolute width in
         * pixels, so any authored dissolve at all on an object that size puts
         * the whole object inside its own fray and deletes it. Small hard things
         * get dissolve 0 and let the shader's own floor (0.22 * halfPx + 1.5,
         * about 3 px here) do all the break-up they can afford.
         *
         * <p>It is also authored at the wetness ceiling. The reference resolves
         * two feet at matched scale because they are the darkest marks on a pale
         * wet ground, not because they are detailed; here they sit on the mist,
         * which is the brightest region in the frame, so the value contrast is
         * doing all the work.
         *
         * <p>{@code lateral} hangs the mass below the bone axis: the foot bone
         * sits at the ankle, and the sole is a few centimetres under it.
         */
        /**
         * The foot's authored rows, shared with the fittings overlay of
         * {@link #buildFittings()} so the dark wedge drawn over each foot on the
         * dusk stage is the same wedge, not a transcription that can drift.
         */
        private static final float[] FOOT_D = {-0.052f, -0.012f, 0.048f, 0.104f, 0.152f};
        private static final float[] FOOT_HW = {0.046f, 0.052f, 0.045f, 0.034f, 0.019f};
        private static final float[] FOOT_LATERAL = {-0.010f, -0.018f, -0.028f, -0.036f, -0.042f};

        private RibbonPoint[] footPoints(Bone foot) {
            RibbonPoint[] pts = new RibbonPoint[FOOT_D.length];
            for (int i = 0; i < FOOT_D.length; i++) {
                pts[i] = RibbonPoint.of(foot, FOOT_D[i]);
            }
            return pts;
        }

        private void buildFoot(Bone foot, float scale) {
            RibbonPoint[] pts = footPoints(foot);
            float[] halfWidth = scaled(scale, FOOT_HW);
            float[] lateral = scaled(scale, FOOT_LATERAL);
            // Flat zero on both legs, and the first attempt at this got it
            // wrong. frayPx is mix(0.22 * halfPx + 1.5, 34, dissolve^0.75) -- an
            // absolute width in pixels -- and a foot is about 20 px across, so
            // halfPx is 5. The far foot was authored at the 0.09 contrast floor
            // the far leg and far sleeve carry, which buys a 7.3 px fray band on
            // a strip with 4 px of half-width: the whole object sat inside its
            // own fray and never appeared at all. Even the near foot's 0.04-0.05
            // end caps were costing it its heel and its toe.
            //
            // This is the identical trap sheathedSword documents and it is worth
            // restating: a contrast floor is a *value* device on a large soft
            // mass and a delete key on a small hard one. The far foot recedes by
            // being wetter-but-lighter than the near one, not by dissolving.
            float[] dissolve = {0f, 0f, 0f, 0f, 0f};
            float[] wetness = scale < 1f
                    ? new float[] {0.68f, 0.74f, 0.74f, 0.74f, 0.66f}
                    : new float[] {0.94f, 1.0f, 1.0f, 1.0f, 0.92f};
            float[] stainBase = {0f, 0f, 0f, 0f, 0f};
            ribbon(pts, halfWidth, lateral, dissolve, wetness, stainBase);
        }

        // -- sleeves: wide, hanging, heavy dissolve at the opening ------------

        /**
         * @param clothBones the near sleeve's two drape bones, or null for the far
         *                   arm. Only the <em>weights</em> of the last three rows
         *                   change: their geometry stays authored off {@code hand}
         *                   exactly as before, because those rows' rails are
         *                   perpendicular to the wrist and re-deriving them from
         *                   the cloth bones' own axes would silently re-cut the
         *                   sleeve's silhouette.
         */
        private void buildSleeve(Bone upperArm, Bone forearm, Bone hand,
                                  float upperArmLen, float forearmLen, float handLen, float drape, float scale,
                                  Bone[] clothBones) {
            RibbonPoint[] pts = {
                    // Starts right at the shoulder joint, not partway down the
                    // upper arm: a gap here was reading as a hairline bridging
                    // the shoulder to the hilt instead of a visible arm
                    // (rig-fixes review, finding F).
                    RibbonPoint.of(upperArm, 0f),
                    RibbonPoint.blended(upperArm, upperArmLen, forearm, 0.5f),
                    RibbonPoint.of(forearm, forearmLen * 0.60f),
                    RibbonPoint.blended(forearm, forearmLen, hand, 0.5f),
                    RibbonPoint.of(hand, handLen),
                    RibbonPoint.of(hand, handLen + drape * 0.5f),
                    RibbonPoint.of(hand, handLen + drape),
            };
            if (clothBones != null) {
                // Rows 4, 5, 6 are the drape. Which bone each rides is resolved from the
                // resampled chain rather than written down here -- see Rail. Row 4 is the pivot
                // and sits on sleeveA's origin, so it is unmoved by sleeveA's own rotation
                // whatever the table says; rows 5 and 6 are past the chain's end and ride
                // sleeveB, which gains them a second joint of swing.
                float[][] local = sleeveDrapeNodes();
                Rail rail = sleeveRail(local[0], local[1]);
                pts[4] = pts[4].skinnedTo(clothBones[rail.boneFor(0)]);
                pts[5] = pts[5].skinnedTo(clothBones[rail.boneFor(1)]);
                pts[6] = pts[6].skinnedTo(clothBones[rail.boneFor(2)]);
            }
            // Wider at the base than revision 1: a sleeve that starts as a
            // two-pixel sliver at the shoulder has no interior for the shader
            // to keep solid, regardless of authored dissolve.
            float[] halfWidth = scaled(scale, 0.135f, 0.140f, 0.145f, 0.160f, 0.180f, 0.220f, 0.268f);
            // The drape hangs *behind* the arm rather than centred on it
            // (rig-fixes section 2's 1.4:1 asymmetry, and the reason the grip
            // cluster now has open ground on its forward side). Positive is
            // forward, so these push the sleeve's mass back toward the haori's
            // own trailing cloud, where reference images 1 and 2 keep it.
            float[] lateral = scaled(scale, 0f, 0f, -0.015f, -0.050f, -0.115f, -0.200f, -0.290f);
            float[] dissolve = {0f, 0f, 0.02f, 0.05f,
                    SLEEVE_DRAPE_DISSOLVE[0], SLEEVE_DRAPE_DISSOLVE[1], SLEEVE_DRAPE_DISSOLVE[2]};
            float contrastFloor = scale < 1f ? 0.10f : 0f;
            for (int i = 0; i < dissolve.length; i++) {
                dissolve[i] = Math.max(dissolve[i], contrastFloor);
            }
            // Sleeves are hanging garments too: pigment pools toward the
            // opening (item 4), and the ramp is kept close to the haori's own so
            // the boundary between the two stops printing as a straight band.
            float[] wetness = {0.06f, 0.12f, 0.22f, 0.36f, 0.52f, 0.70f, 0.85f};
            // The ochre used to sit at the sleeve opening, i.e. right on the
            // hand, and printed as a bright warm blob over the one join the
            // references spend their whole interior budget resolving.
            float[] stainBase = {0f, 0.04f, 0.06f, 0.04f, 0f, 0f, 0f};
            ribbon(pts, halfWidth, lateral, dissolve, wetness, stainBase);
        }

        // -- torso/limbs: narrow, low dissolve, mostly hidden by cloth --------

        /**
         * Genuinely 0 dissolve at the attachment end (the core), rising toward
         * a modest ceiling at the far tip -- not a flat value, which was
         * reading as a permanently half-dissolved sliver. Wetness ranges from
         * {@code wetLo} at the attachment to {@code wetHi} at the tip: legs get
         * a much higher ceiling than arms so they read as one of the darkest
         * masses in the figure once the hakama frays away over them
         * (rig-fixes review, finding E), not a mid-slate mush zone.
         */
        private void buildLimbSliver(RibbonPoint[] pts, float halfWidth, float scale, float wetLo, float wetHi) {
            int n = pts.length;
            float[] hw = new float[n];
            float[] dissolve = new float[n];
            float[] wetness = new float[n];
            float[] stainBase = new float[n];
            float contrastFloor = scale < 1f ? 0.08f : 0f;
            for (int i = 0; i < n; i++) {
                float t = n <= 1 ? 0f : i / (float) (n - 1);
                hw[i] = halfWidth * scale;
                dissolve[i] = Math.max(contrastFloor, MathUtils.lerp(0f, 0.14f, t));
                wetness[i] = MathUtils.lerp(wetLo, wetHi, t);
            }
            ribbon(pts, hw, dissolve, wetness, stainBase);
        }

        private void buildTrunk(Bone hips, Bone spine, Bone chest, Bone neck, Bone head) {
            // Trunk bones stack via Y offset; this walks bone origins directly
            // rather than via alongBone/ribbon(). Genuinely solid (dissolve 0):
            // this is the body core, entirely re-covered by the haori below the
            // collar. Extended up to the head bone itself (not stopping at
            // neck) and widened there, so there is a real connecting neck
            // between shoulders and skull instead of a pale narrow stalk
            // (rig-fixes review, finding C).
            Bone[] chain = {hips, spine, chest, neck, head};
            // Debt D7. The neck rows were 0.080/0.074 half-width -- a 36 px
            // column of very nearly constant width, so its two rails were a pair
            // of near-parallel straight lines, and the review's exact words were
            // "a pale vertical neck column whose left boundary is a straight
            // line". A real neck narrows going up and the *back* of it is eaten
            // by the collar, so the taper is now steep and asymmetric-by-fray
            // rather than by shape.
            float[] hw = {0.105f, 0.110f, 0.120f, 0.066f, 0.050f};
            // Ink gravity again: the trunk is the lightest thing in the figure
            // at the top and pools toward the hips, matching the haori over it.
            //
            // The two neck rows are the exception, and D7 is why. At 0.06 the
            // column landed near INK_INDIGO *and* sat under both of
            // ink_skin.frag's low-wetness gates -- cream reserves open below
            // 0.55 and the paper tooth below 0.45 -- so it printed as a mottled
            // pale slab, brighter than anything else on the figure and reading
            // as a gap rather than as a body part. At 0.44 it is a mid value:
            // clearly lighter than the head above it and the jaw wedge across
            // it, clearly darker than paper.
            // The two neck rows land at luminance ~44 against a jaw wedge at 26
            // and a hair mass at 31 -- roughly two thirds of the thirty-level
            // cloth ramp, which is what makes the wedge read as a *separation*
            // rather than as more head. 0.28 is as light as this material goes
            // without reopening the low-wetness gates: `dark` starts at 0.14
            // before wetness contributes at all, so the pooled half of the ramp
            // is the only half a garment can reach, and anything paler than
            // INK_INDIGO has to come from the cream reserves -- which is what
            // printed the mottled pale slab the review measured.
            float[] wet = {0.62f, 0.38f, 0.16f, 0.28f, 0.32f};
            // Nonzero only at the neck. The trunk core is meant to be solid, but
            // a strip whose fray band is the shader's 3 px floor reproduces its
            // own polygon rail exactly, offset inward -- which is what printed
            // the straight boundary. 0.06 buys about 8 px of fray band and a
            // +-3 px wander on a 30 px column: enough that the contour is a
            // noise contour, nowhere near enough to delete it.
            float[] dis = {0f, 0f, 0f, 0.06f, 0.09f};
            int n = chain.length;
            short[] left = new short[n];
            short[] right = new short[n];
            for (int i = 0; i < n; i++) {
                Vector2 o = skeleton.worldPosition(chain[i].index, new Vector2());
                float t = i / (float) (n - 1);
                float flow = angleToU(90f);
                left[i] = builder.vertex(o.x - hw[i], o.y, 0f, t, dis[i], wet[i], 0f, flow,
                        chain[i].index, 1f, chain[i].index, 0f);
                right[i] = builder.vertex(o.x + hw[i], o.y, 1f, t, dis[i], wet[i], 0f, flow,
                        chain[i].index, 1f, chain[i].index, 0f);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(left[i], left[i + 1], right[i + 1], right[i]);
            }
        }

        // -- the shoulder mass: rig-fixes-3 item 1 ---------------------------

        /**
         * Sode and haori shoulder line as one wide horizontal strip arching over
         * both shoulders -- the single highest-impact fix in System 1.
         *
         * <p>Pass 2 measured the silhouette as a cone: shoulder line ~55 px,
         * hem ~185 px, widening monotonically, so shoulder span was about 0.3x
         * hip span where every samurai reference is about 1.6x the other way.
         * That is the wraith read, and no amount of work on the head fixes it
         * while the shoulder mass is absent.
         *
         * <p>Authored as a top rail and a bottom rail in world-bind-space rather
         * than as a bone ribbon, because this mass is *body*-mounted armour: it
         * must not spin with the sword arm through the swing's full shoulder
         * revolution. Weights are therefore chest (top) blending to spine
         * (bottom), never shoulderL/R.
         *
         * <p>The top edge is the hard-ish one -- fabric stretched over the
         * shoulder, dissolve near 0 right out to the tips -- against a frayed
         * lower edge, which is the contrast rig-fixes section 2 asked for and
         * is what keeps the widest row from being eaten by the fray band.
         */
        private void buildShoulderMantle(Bone spine, Bone chest) {
            // The end columns are deliberately short: a mantle that stops on a
            // full-height vertical rail prints that rail as a straight polygon
            // edge (STYLE.md 10 fails on sight), so each end tapers to a near
            // point over two columns instead.
            float[] x =    {-0.53f, -0.47f, -0.38f, -0.24f, -0.06f,  0.09f,  0.25f,  0.38f,  0.46f,  0.51f};
            float[] top =  { 1.286f, 1.352f, 1.428f, 1.482f, 1.508f, 1.500f, 1.470f, 1.416f, 1.362f, 1.300f};
            float[] bot =  { 1.230f, 1.140f, 1.062f, 1.055f, 1.096f, 1.116f, 1.110f, 1.140f, 1.196f, 1.262f};
            float[] disT = { 0.30f,  0.12f,  0.03f,  0f,     0f,     0f,     0f,     0.03f,  0.12f,  0.30f};
            float[] disB = { 0.42f,  0.30f,  0.20f,  0.10f,  0.06f,  0.06f,  0.10f,  0.20f,  0.30f,  0.42f};
            // Kept close to the haori and sleeve values underneath it. The merge
            // keeps the topmost strip's material outright, so an authored wetness
            // step across a region boundary prints as a straight pale band.
            float[] wetT = { 0.10f,  0.08f,  0.05f,  0.03f,  0.03f,  0.03f,  0.03f,  0.05f,  0.08f,  0.10f};
            float[] wetB = { 0.20f,  0.18f,  0.16f,  0.14f,  0.13f,  0.13f,  0.14f,  0.16f,  0.18f,  0.20f};
            float[] stn =  { 0.05f,  0.08f,  0.14f,  0.19f,  0.12f,  0.07f,  0.12f,  0.16f,  0.10f,  0.05f};

            int n = x.length;
            short[] hi = new short[n];
            short[] lo = new short[n];
            for (int i = 0; i < n; i++) {
                float s = i / (float) (n - 1);
                // Streaks run across the shoulder, following the drape.
                float flow = angleToU(200f - 40f * s);
                hi[i] = builder.vertex(x[i], top[i], s, 0f, disT[i], wetT[i], stainAt(stn[i] * 0.5f), flow,
                        chest.index, 1f, chest.index, 0f);
                lo[i] = builder.vertex(x[i], bot[i], s, 1f, disB[i], wetB[i], stainAt(stn[i]), flow,
                        chest.index, 0.7f, spine.index, 0.3f);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(hi[i], hi[i + 1], lo[i + 1], lo[i]);
            }
        }

        // -- the grip cluster: hand + tsuka + tsuba (debt D1) -----------------
        //
        // Why this failed for five passes, and it was never a geometry problem.
        // A hand mesh and a hilt mesh have existed since revision 3, both drawn
        // last, both at the right place -- and the pass-5 review still recorded
        // "the blade emerges directly out of the mantle mass with nothing
        // between cloth and steel". They were authored at wetness 0.50-0.72
        // against a sleeve authored at 0.52-0.85, i.e. *the same value*, and the
        // whole cloth path resolves to a 30-level ramp between INK_INDIGO and
        // INK_BLACK. Three marks that differ by four luminance levels are one
        // mark however carefully they are shaped.
        //
        // So the fix is value and hue, not outline. Reading outward the cluster
        // now steps sleeve (mid indigo) -> hand (warm mid, the leather-and-brass
        // kote) -> tsuka (near-black) -> tsuba (black, the widest mark) -> steel
        // (near-white). That is five values in about forty pixels, which is what
        // makes the cluster survive downscaling in every Family A and B
        // reference. Nothing composites over any of it -- see buildBody's order.

        /**
         * The fist, authored on the <em>blade</em>'s axis so it actually sits on
         * the grip (the old mesh ran along the hand bone and missed the grip
         * line by 16 px, which is why it read as part of the sleeve), but skinned
         * to {@code handL} so a later IK solver driving the wrist carries it.
         *
         * <p>The warm channel is the point. {@code stainMask} at 0.60 puts this
         * over the "fitting" threshold in ink_skin.frag, which trades the ochre's
         * blotchy shibori gate for a muted, reliable leather-brown -- the one
         * value in the cluster that is neither ink nor steel.
         */
        private void buildHand(Bone hand, Bone blade) {
            float[] d = {-0.190f, -0.150f, -0.105f, -0.062f};
            float[] hw = {0.038f, 0.060f, 0.058f, 0.036f};
            float[] wet = {0.60f, 0.55f, 0.57f, 0.64f};
            float[] stn = {0.52f, 0.62f, 0.60f, 0.50f};
            // Zero. At 26 px across, even a 0.05 dissolve buys a 6 px fray band
            // against 13 px of half-width, and the mark stops being a mark.
            float[] dis = {0f, 0f, 0f, 0f};
            gripPiece(blade, hand, d, hw, dis, wet, stn);
        }

        /**
         * The grip: a long near-black bar, deliberately the darkest cloth-path
         * value in the figure, running from the kashira back at -0.23 to the
         * fuchi just short of the guard.
         */
        private void buildTsuka(Bone blade) {
            float[] d = {-0.232f, -0.190f, -0.120f, -0.062f, -0.030f};
            float[] hw = {0.026f, 0.030f, 0.030f, 0.028f, 0.025f};
            float[] wet = {0.94f, 1.0f, 1.0f, 1.0f, 1.0f};
            float[] stn = {0f, 0f, 0f, 0f, 0f};
            float[] dis = {0.03f, 0f, 0f, 0f, 0f};
            gripPiece(blade, blade, d, hw, dis, wet, stn);
        }

        /**
         * The guard, as its own mass rather than one lozenge shared with the
         * grip. It is the widest mark in the cluster and the one that reads
         * longest as the figure shrinks: 10 x 30 px here against roughly 8 x 20
         * in reference image 1 downscaled to this figure height.
         */
        private void buildTsuba(Bone blade) {
            float[] d = {-0.030f, -0.014f, 0.004f, 0.020f};
            float[] hw = {0.030f, 0.068f, 0.064f, 0.026f};
            float[] wet = {1.0f, 1.0f, 1.0f, 1.0f};
            // Stain-free. The guard is the black anchor the two bright things in
            // the cluster -- the warm kote behind it and the steel in front --
            // are both measured against, and a fitting tone here would collapse
            // it into the hand.
            float[] stn = {0f, 0f, 0f, 0f};
            float[] dis = {0f, 0f, 0f, 0f};
            gripPiece(blade, blade, d, hw, dis, wet, stn);
        }

        // -- the fittings pass: STYLE.md 11.4, the parts D1 has owed since System 1

        /**
         * The fittings mesh: the two-fisted grip cluster and the feet, re-anchored
         * to the corpus's fitting register. See {@link SamuraiRig#fittingsMesh()}
         * for the measured registers this is built against.
         *
         * <p>Everything here is authored at dissolve 0 — these are exactly the
         * "small hard marks" the fray band deletes (the sheathedSword and
         * buildFoot findings) — and the softness STYLE.md 3 requires comes from
         * the material's feather, the way the face's does: soft-but-whole, the
         * leading-edge instrument, never the broken one. A hand is a leading
         * edge.
         *
         * <p>The corpus's grip cluster is two fists stacked on a grip with the
         * grip's two ends visible past them — kashira below the lower fist,
         * fuchi plus guard above the upper — and the fists read as *warm
         * mid-value lumps between dark bars*, not as drawn fingers
         * (ref3-matched-378.png right duellist, regions in
         * {@code SamuraiRig#fittingsMesh()}). So: two lumpy strips on the stain
         * channel's fitting regime over a sub-floor grip bar, with a 3 px gap
         * between the fists where the grip shows through.
         */
        SkinnedMesh buildFittings() {
            builder = new SkinnedMesh.Builder();
            Bone hand = skeleton.bone("handL");
            Bone blade = skeleton.bone("blade");

            // The grip bar, kashira to fuchi. Slightly longer below than the old
            // body-mesh tsuka (-0.265 against -0.232): the reference's kashira
            // runs a readable ~18 px past the lower fist, and the old stub was
            // 9. Skinned to the blade bone like the strip it underlies.
            fittingPiece(blade, blade,
                    new float[] {-0.265f, -0.235f, -0.150f, -0.060f, -0.030f},
                    new float[] {0.019f, 0.026f, 0.028f, 0.026f, 0.023f},
                    null,
                    new float[] {1f, 1f, 1f, 1f, 1f},
                    new float[] {0f, 0f, 0f, 0f, 0f});

            // The guard: the widest dark mark in the cluster, right where the
            // steel starts. The corpus's is about 22 x 7 px at matched scale.
            fittingPiece(blade, blade,
                    new float[] {-0.032f, -0.016f, 0.004f, 0.022f},
                    new float[] {0.026f, 0.056f, 0.052f, 0.024f},
                    null,
                    new float[] {1f, 1f, 1f, 1f},
                    new float[] {0f, 0f, 0f, 0f});

            // The two fists. Sized to cover the old body-mesh hand (half-width
            // 0.060 at its widest) so the pale kote it prints on the dusk stage
            // cannot fringe the new cluster; the 0.013-unit gap between them is
            // the lit sliver the corpus keeps between the two hands. Lumpy on
            // purpose — the corpus does not draw fingers, it draws knuckle
            // bulges. Skinned to the hand so a wrist solve carries them.
            fittingPiece(blade, hand,
                    new float[] {-0.190f, -0.174f, -0.148f, -0.135f},
                    new float[] {0.034f, 0.058f, 0.062f, 0.042f},
                    new float[] {0.004f, 0.007f, 0.006f, 0.004f},
                    new float[] {0.42f, 0.38f, 0.40f, 0.46f},
                    new float[] {0.62f, 0.74f, 0.72f, 0.58f});
            fittingPiece(blade, hand,
                    new float[] {-0.122f, -0.106f, -0.076f, -0.058f},
                    new float[] {0.040f, 0.060f, 0.056f, 0.032f},
                    new float[] {0.004f, 0.007f, 0.006f, 0.003f},
                    new float[] {0.44f, 0.38f, 0.40f, 0.46f},
                    new float[] {0.60f, 0.74f, 0.70f, 0.56f});

            // The feet, restated in the fitting register. The body mesh's own
            // wedges stay (family A draws only the body mesh, and there they
            // read); on the dusk stage the ground band sits at L 34-51 where the
            // cloth path's darkest is 25.7 (fit-p1-parry-before frame_011,
            // hero feet x260..460 y600..700) — a Delta-L of 8-15, which is why
            // the hero has no feet at the duel framing. The same wedge at the
            // fitting floor is the corpus's own answer: dark marks standing on
            // the smear (ref3 feet), not attenuation.
            float[] footDis = {0f, 0f, 0f, 0f, 0f};
            float[] footStain = {0f, 0f, 0f, 0f, 0f};
            ribbon(footPoints(skeleton.bone("footR")),
                    scaled(0.86f, FOOT_HW), scaled(0.86f, FOOT_LATERAL),
                    footDis, new float[] {0.78f, 0.84f, 0.84f, 0.84f, 0.76f}, footStain);
            ribbon(footPoints(skeleton.bone("footL")),
                    scaled(1f, FOOT_HW), scaled(1f, FOOT_LATERAL),
                    footDis, new float[] {0.94f, 1f, 1f, 1f, 0.92f}, footStain);

            return builder.build();
        }

        /** {@link #gripPiece} with a per-row lateral offset, for the fists' knuckle bulge. */
        private void fittingPiece(Bone axis, Bone skin, float[] d, float[] hw, float[] lateral,
                                   float[] wetness, float[] stain) {
            int n = d.length;
            short[] left = new short[n];
            short[] right = new short[n];
            float flow = angleToU(skeleton.worldRotationDeg(axis.index));
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                float lat = lateral == null ? 0f : lateral[i];
                Vector2 pl = alongBone(axis, d[i], lat + hw[i]);
                Vector2 pr = alongBone(axis, d[i], lat - hw[i]);
                left[i] = builder.vertex(pl.x, pl.y, 0f, t, 0f, wetness[i], stain[i], flow, skin.index);
                right[i] = builder.vertex(pr.x, pr.y, 1f, t, 0f, wetness[i], stain[i], flow, skin.index);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(left[i], left[i + 1], right[i + 1], right[i]);
            }
        }

        /** A short strip laid on the blade's own axis, skinned to whichever bone should carry it. */
        private void gripPiece(Bone axis, Bone skin, float[] d, float[] hw,
                                float[] dissolve, float[] wetness, float[] stain) {
            int n = d.length;
            short[] left = new short[n];
            short[] right = new short[n];
            float flow = angleToU(skeleton.worldRotationDeg(axis.index));
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                Vector2 pl = alongBone(axis, d[i], hw[i]);
                Vector2 pr = alongBone(axis, d[i], -hw[i]);
                left[i] = builder.vertex(pl.x, pl.y, 0f, t, dissolve[i], wetness[i], stain[i], flow, skin.index);
                right[i] = builder.vertex(pr.x, pr.y, 1f, t, dissolve[i], wetness[i], stain[i], flow, skin.index);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(left[i], left[i + 1], right[i + 1], right[i]);
            }
        }

        // -- obi and daisho (debt D1) -----------------------------------------

        /**
         * The sash. Three rails, not two, because what makes an obi read at this
         * scale is not its shape but the dark-light-dark sandwich: a heavy ink
         * line where the haori folds over it, a warm linen band, another heavy
         * line where the hakama is gathered under it.
         *
         * <p>It is also doing structural work the debt document calls out --
         * without it the shoulder-heavy upper mass and the hakama are one
         * uninterrupted 300 px column of indigo, and the eye has nothing to
         * measure the figure's proportions against.
         */
        private void buildObi(Bone hips, Bone spine) {
            float[] x =   {-0.268f, -0.208f, -0.120f, -0.020f,  0.076f,  0.156f,  0.216f};
            float[] top = { 1.024f,  1.058f,  1.078f,  1.082f,  1.070f,  1.044f,  1.008f};
            float[] mid = { 0.972f,  1.002f,  1.020f,  1.024f,  1.012f,  0.988f,  0.954f};
            float[] bot = { 0.912f,  0.938f,  0.954f,  0.958f,  0.946f,  0.924f,  0.892f};
            // Near zero across the body of the wrap; only the two ends, which
            // disappear round the far side, are allowed to break up.
            float[] dis = { 0.15f,   0.04f,   0f,      0f,      0f,      0.03f,   0.14f};
            float[] stn = { 0.44f,   0.58f,   0.62f,   0.60f,   0.58f,   0.52f,   0.40f};

            int n = x.length;
            short[] hi = new short[n];
            short[] md = new short[n];
            short[] lo = new short[n];
            for (int i = 0; i < n; i++) {
                float s = i / (float) (n - 1);
                float flow = angleToU(6f - 12f * s);   // the wrap runs across the body
                hi[i] = builder.vertex(x[i], top[i], s, 0f, dis[i], 0.72f, 0.10f, flow,
                        hips.index, 0.65f, spine.index, 0.35f);
                md[i] = builder.vertex(x[i], mid[i], s, 0.5f, dis[i] * 0.6f, 0.52f, stn[i], flow,
                        hips.index, 0.85f, spine.index, 0.15f);
                lo[i] = builder.vertex(x[i], bot[i], s, 1f, dis[i], 0.80f, 0.08f, flow,
                        hips.index, 1f, hips.index, 0f);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(hi[i], hi[i + 1], md[i + 1], md[i]);
                builder.quad(md[i], md[i + 1], lo[i + 1], lo[i]);
            }
        }

        /**
         * The worn pair -- katana in its saya and the shorter wakizashi -- thrust
         * through the obi on the far hip, tsuka forward, scabbards trailing back
         * and down. Reference images 1 and 2 both put a long pale diagonal here
         * and it does more silhouette work than anything else in either picture:
         * the kojiri of each ends 70-80 px <em>outside</em> the haori's back
         * contour, in open paper, which is a readable part the garment cloud can
         * never supply because it has no straight lines in it.
         *
         * <p>Both are skinned to {@code hips}, which is physically right (they
         * are held by the sash) and means they swing with the hip rotation of the
         * cut rather than floating.
         */
        private void buildDaisho(Bone hips, Bone spine) {
            // Wakizashi: shorter, shallower, and drawn first so the katana
            // crosses in front of it. The 12 degrees between the two axes is
            // what stops them reading as a pair of rails.
            sheathedSword(hips, spine, 0.160f, 1.104f, -0.500f, 0.902f,
                    0.31f, 0.0262f, 0.44f);
            sheathedSword(hips, spine, 0.322f, 1.176f, -0.566f, 0.638f,
                    0.26f, 0.0330f, 0.50f);
        }

        /**
         * One sheathed sword as a straight strip from the kashira at
         * {@code (x0,y0)} to the kojiri at {@code (x1,y1)}. {@code tsukaFrac} is
         * how much of that run is grip; everything past it is scabbard.
         */
        private void sheathedSword(Bone hips, Bone spine, float x0, float y0, float x1, float y1,
                                    float tsukaFrac, float halfWidth, float sayaStain) {
            float[] t = {0f, tsukaFrac * 0.55f, tsukaFrac, tsukaFrac + 0.035f,
                         0.52f, 0.72f, 0.88f, 1f};
            int n = t.length;
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ux = dx / len;
            float uy = dy / len;
            // Perpendicular, so half-widths offset across the strip rather than
            // along it.
            float px = -uy;
            float py = ux;
            float flow = angleToU(MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees);

            // Grip near-black like the drawn sword's, a small dark bump for the
            // koiguchi fitting, then the lacquered saya -- which is the *lighter*
            // of the two, as it is in both references, and is why it survives
            // being laid over a near-black haori.
            float[] hw = {halfWidth * 0.82f, halfWidth * 0.94f, halfWidth * 0.96f, halfWidth * 1.18f,
                          halfWidth, halfWidth * 0.96f, halfWidth * 0.93f, halfWidth * 0.90f};
            // Wet, not dry. The first p6 capture authored the scabbard bodies at
            // 0.28-0.34 on the reasoning that a lacquered saya is lighter than
            // the haori -- but low wetness in this material does not mean
            // "lighter", it means "the brush was dry here": ink_skin.frag opens
            // the paper tooth below wetness 0.45 and the cream reserves below
            // 0.55, and both fired at once. The scabbards came out as pale
            // ghosts wherever they left the garment, which is precisely the
            // stretch that has to read. Fittings are solid objects: they are
            // authored wet, and the *stain* carries the value and hue step.
            float[] wet = {0.95f, 1.0f, 1.0f, 0.88f, 0.60f, 0.56f, 0.58f, 0.74f};
            float[] stn = {0f, 0f, 0f, 0.20f, sayaStain, sayaStain, sayaStain * 0.9f, sayaStain * 0.5f};
            // Flat zero, and this is the whole reason the first p6 capture lost
            // both scabbards past two thirds of their length. frayPx is
            // mix(0.22*halfPx + 1.5, 34, dissolve^0.75) -- an absolute width in
            // pixels -- while a saya is 13 px across, so halfPx is about 6. A
            // dissolve of 0.22 buys a 12.6 px fray band on a strip with 6 px of
            // half-width: the entire object sits inside its own fray and the
            // threshold never drops far enough for anything to survive. It
            // rendered as two tapering spikes that stopped dead at the haori's
            // edge, i.e. the exact opposite of the silhouette work it is here to
            // do. Small hard objects get zero, always; the shader's own floor
            // (0.22 * halfPx + 1.5, about 3 px here) is all the break-up a
            // 13 px strip can afford.
            float[] dis = {0.04f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};

            short[] left = new short[n];
            short[] right = new short[n];
            for (int i = 0; i < n; i++) {
                float cx = x0 + dx * t[i];
                float cy = y0 + dy * t[i];
                // The forward end is tucked into the sash and rides the spine a
                // little; the trailing end is pure hip.
                float wSpine = 0.28f * (1f - MathUtils.clamp(t[i] / 0.55f, 0f, 1f));
                left[i] = builder.vertex(cx + px * hw[i], cy + py * hw[i], 0f, t[i],
                        dis[i], wet[i], stn[i], flow, hips.index, 1f - wSpine, spine.index, wSpine);
                right[i] = builder.vertex(cx - px * hw[i], cy - py * hw[i], 1f, t[i],
                        dis[i], wet[i], stn[i], flow, hips.index, 1f - wSpine, spine.index, wSpine);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(left[i], left[i + 1], right[i + 1], right[i]);
            }
        }

        // -- head: profile silhouette, face detail out of scope for System 1 --

        /**
         * A radius(angle) shape rather than a circle: tighter and pointed
         * toward +X (brow/nose/chin as one flowing front contour), bulging up
         * and back toward the topknot mass, per rig-fixes section 1. angle 0 =
         * +X.
         *
         * <p>Two rings (centre-to-inner solid fan, inner-to-outer a proper
         * ring strip) rather than one fan straight from the centre: a plain
         * fan is all thin wedges meeting at a point, and the shader's
         * boundary-distance dissolve ate each wedge back to a spike, printing
         * a fringe of uniform radial spurs instead of a resolved skull
         * (rig-fixes review, finding C). The ring strip gives real width for
         * the outer edge to fray into, and dissolve here stays low and nearly
         * flat -- this is meant to be the *most* resolved mass in the figure,
         * and hair fraying is System 3's job, not this mesh's.
         */
        private void buildHead(Bone head) {
            Vector2 c = skeleton.worldPosition(head.index, new Vector2());
            float cx = c.x + HEAD_LOBE_DX;
            float cy = c.y + HEAD_LOBE_DY;
            // rig-fixes-3 item 7 gave this a *directional* mass rather than a
            // radially uniform blob, at 25-30 degree angular resolution. Debt
            // D7 is that that is not enough: at 25 degrees the front quadrant
            // gets four samples over the whole of brow, nose, lip and chin, so
            // the profile can only ever be a smooth arc and the head reads as a
            // dark blob with a hat.
            //
            // STYLE.md 4b.3 asks for brow-nose-lip-chin as *one continuous
            // contour*. That is a silhouette property -- it is this mesh, not
            // System 3b's face -- and it needs samples where the contour turns.
            // The front quadrant (angles 340 through 60) now carries eight of
            // the twenty-two samples and the back half keeps its old coarse
            // spacing, because nothing there turns.
            //
            // Reading down the face side from the crown: forehead (58, 40),
            // brow ridge (24), then the bridge *recesses* at 12 -- a smaller
            // radius between two larger ones is a concave notch, and that notch
            // is the single mark that makes a profile read as a face rather
            // than an egg -- then the nose juts at 0 and 352, the lip recesses
            // at 338, the chin juts at 324, and 308 cuts back hard under the
            // jaw. 272-252 is the under-jaw and nape, which is where the neck
            // takes over and where the wedge below attaches.
            float[] angle =  {  0f,  12f,  24f,  40f,  58f,  78f, 100f, 122f,
                              145f, 168f, 190f, 212f, 232f, 252f, 272f, 290f,
                              308f, 324f, 338f, 352f};
            float[] radius = {0.152f, 0.134f, 0.141f, 0.146f, 0.152f, 0.158f, 0.164f, 0.170f,
                              0.172f, 0.166f, 0.152f, 0.134f, 0.112f, 0.094f, 0.088f, 0.100f,
                              0.114f, 0.146f, 0.132f, 0.147f};
            headLobe(head, cx, cy, angle, radius, 0.55f, 0f, 0.02f, 0.78f);

            // ...and the lobe alone cannot carry that contour, which is why the
            // first attempt at this only moved the numbers. ink_skin.frag sizes
            // its fray band off the strip's own thickness -- 0.22 * halfPx + 1.5
            // -- and the head is a 68 px disc, so halfPx is 34 and the rim frays
            // over 10 px with a +-4 px wander on top. A nose is 6 px of
            // protrusion at this framing. The band was eating the feature whole.
            //
            // This is the same problem the grip cluster hit in D1 and it takes
            // the same answer: a small hard mark gets its own narrow strip, so
            // its half-width and therefore its fray floor are small. The profile
            // strip is 12 px across, which buys a 2.8 px band -- fine enough to
            // keep a nose and a chin.
            buildFaceEdge(head, cx, cy);

            // The jaw-neck wedge of STYLE.md 4b.3, and the second half of D7.
            // A profile head does not sit on a column; it overhangs one, and the
            // dark under-jaw is what separates the two. Without it the neck runs
            // straight up into the skull and the eye has nothing to tell it
            // where the head ends -- which is the "headless with a hat" read.
            buildJawWedge(head, skeleton.bone("neck"), cx, cy);

            // Topknot: offset up and back far enough that it clears the skull
            // outline and reads as its own lobe, but overlapping enough that it
            // is welded rather than floating. Slightly frayed at its own rim --
            // it is hair, and full strand simulation is System 3's problem.
            float lx = cx + MathUtils.cosDeg(TOPKNOT_ANGLE_DEG) * TOPKNOT_DIST;
            float ly = cy + MathUtils.sinDeg(TOPKNOT_ANGLE_DEG) * TOPKNOT_DIST;
            float[] knotAngle = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};
            float[] knotRadius = {0.070f, 0.078f, 0.076f, 0.082f, 0.074f, 0.064f, 0.062f, 0.066f};
            headLobe(head, lx, ly, knotAngle, knotRadius, 0.50f, 0f, 0.05f, 0.88f);
        }

        /**
         * A centre-to-inner solid fan plus an inner-to-outer ring strip. A plain
         * fan straight from the centre is all thin wedges meeting at a point, and
         * the shader's boundary-distance dissolve eats each wedge back to a
         * spike, printing a fringe of uniform radial spurs instead of a resolved
         * mass (rig-fixes review, finding C). The ring gives the outer edge real
         * width to fray into.
         */
        private void headLobe(Bone head, float cx, float cy, float[] angle, float[] radius,
                               float innerScale, float dissolveIn, float dissolveOut, float wet) {
            int n = angle.length;
            float flowUp = angleToU(90f);
            short center = builder.vertex(cx, cy, 0.5f, 0.5f, dissolveIn, wet, 0f, flowUp,
                    head.index, 1f, head.index, 0f);
            short[] inner = new short[n];
            short[] outer = new short[n];
            for (int k = 0; k < n; k++) {
                float ca = MathUtils.cosDeg(angle[k]);
                float sa = MathUtils.sinDeg(angle[k]);
                float rIn = radius[k] * innerScale;
                float rOut = radius[k];
                float flow = angleToU(angle[k]);
                inner[k] = builder.vertex(cx + rIn * ca, cy + rIn * sa,
                        0.5f + 0.5f * innerScale * ca, 0.5f + 0.5f * innerScale * sa,
                        dissolveIn, wet, 0f, flow, head.index, 1f, head.index, 0f);
                outer[k] = builder.vertex(cx + rOut * ca, cy + rOut * sa,
                        0.5f + 0.5f * ca, 0.5f + 0.5f * sa,
                        dissolveOut, wet * 0.92f, 0f, flow, head.index, 1f, head.index, 0f);
            }
            for (int k = 0; k < n; k++) {
                int k2 = (k + 1) % n;
                builder.triangle(center, inner[k], inner[k2]);
                builder.quad(inner[k], inner[k2], outer[k2], outer[k]);
            }
        }

        /**
         * The front contour of STYLE.md 4b.3 -- brow, nose, lip, chin as one
         * continuous line -- as a narrow strip laid over the front arc of the
         * skull lobe, running from the forehead round to under the jaw.
         *
         * <p>Its outer rail is the profile and it sits a few percent proud of
         * the lobe beneath, so the union's silhouette is this strip's edge
         * rather than the lobe's. The inner rail is well inside the lobe, which
         * is what welds the two: two strips that meet rail-to-rail each lose
         * their own fray band and print a bright seam between them, which is
         * exactly what the first version of the jaw wedge did.
         *
         * <p>Angles are measured with 0 = +X, the way the figure faces, and run
         * negative down the face. The feature that does the work is the notch at
         * 10 degrees: a smaller radius between the brow above and the nose tip
         * below is a concave bridge, and a concave bridge is the one mark that
         * separates a profile from an egg. Everything else here -- the lip
         * setback at -22, the chin at -34, the hard cut under it at -50 -- is
         * about 6 px of relief, which is what the reference carries at matched
         * scale.
         */
        private void buildFaceEdge(Bone head, float cx, float cy) {
            float[] a = FACE_A;
            float[] r = new float[FACE_A.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = contourR(i);   // the hero reproduces the authored table exactly
            }
            int n = a.length;
            short[] outer = new short[n];
            short[] inner = new short[n];
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                float ca = MathUtils.cosDeg(a[i]);
                float sa = MathUtils.sinDeg(a[i]);
                float rOut = r[i] * 1.02f;
                float rIn = r[i] * 0.58f;
                float flow = angleToU(a[i] - 90f);   // the stroke runs down the face
                // Zero dissolve the whole way. STYLE.md 4b.1: the face is exempt
                // from the ink dissolve, because the dissolve exists to destroy
                // edges and this is the one edge in the figure that carries
                // identity.
                outer[i] = builder.vertex(cx + rOut * ca, cy + rOut * sa, 1f, t, 0f, 0.60f, 0f, flow,
                        head.index, 1f, head.index, 0f);
                inner[i] = builder.vertex(cx + rIn * ca, cy + rIn * sa, 0f, t, 0f, 0.66f, 0f, flow,
                        head.index, 1f, head.index, 0f);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(inner[i], inner[i + 1], outer[i + 1], outer[i]);
            }
        }

        /**
         * The dark wedge of STYLE.md 4b.3 that separates head from body, hung
         * off the head lobe's lower contour between the chin (angle 324) and
         * the nape (252) and dropping toward the collar.
         *
         * <p>Two things make it work and both are measured rather than shaped.
         * It is authored at the wetness ceiling, so it is the near-black end of
         * the ramp against a neck column the trunk now authors at 0.44 -- about
         * a third of the thirty-level cloth ramp, where D1's post-mortem
         * established that four levels is invisible. And its front point sits
         * roughly 19 px forward of the throat rail, so against open paper the
         * silhouette turns a corner there: that corner is the jaw line, and it
         * is the only thing at this scale that says the head overhangs the neck
         * rather than resting on it.
         *
         * <p>Deepest at the back and shallowest under the chin, which is the way
         * the shadow actually falls and which leaves a sliver of neck visible in
         * front of it. A wedge that reached the collar everywhere would delete
         * the neck instead of explaining it.
         */
        private void buildJawWedge(Bone head, Bone neck, float cx, float cy) {
            // The top rail runs ~8 px *inside* the lobe, not along its boundary.
            // Authored flush the two strips each lost their own fray band and a
            // bright seam opened between them, which printed the wedge as a
            // detached lozenge floating under the chin -- worse than no wedge.
            float[] tx = { 0.116f,  0.062f,  0.008f, -0.040f};
            float[] ty = {-0.046f, -0.052f, -0.052f, -0.050f};
            float[] bx = { 0.090f,  0.040f, -0.008f, -0.048f};
            float[] by = {-0.112f, -0.132f, -0.138f, -0.128f};
            // Zero through the body of the wedge. It is 36 x 15 px, the same
            // size class as the grip cluster, and the fray band is an absolute
            // width in pixels: anything but zero here eats the whole mark.
            float[] dis = {0.05f, 0f, 0f, 0.08f};
            int n = tx.length;
            short[] hi = new short[n];
            short[] lo = new short[n];
            for (int i = 0; i < n; i++) {
                float s = i / (float) (n - 1);
                float flow = angleToU(200f - 30f * s);
                hi[i] = builder.vertex(cx + tx[i], cy + ty[i], s, 0f, dis[i], 1f, 0f, flow,
                        head.index, 1f, head.index, 0f);
                lo[i] = builder.vertex(cx + bx[i], cy + by[i], s, 1f, dis[i], 0.96f, 0f, flow,
                        head.index, 0.72f, neck.index, 0.28f);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(hi[i], hi[i + 1], lo[i + 1], lo[i]);
            }
        }

        // -- System 3b: the face (STYLE.md 4b) ---------------------------------

        /**
         * The skin field: the value structure of a face, which is what survives
         * every framing. Family D finds a face with a handful of marks — "a
         * shadow where the eye socket is, one stroke for the brow, a break of
         * light along the nose and jaw" — and this mesh is those values with no
         * line work at all: a three-rail strip inside the profile contour whose
         * wetness carries lit plane, socket, under-nose, under-chin, and whose
         * hairline rail darkens toward the hair it meets.
         *
         * <p>Drawn in its own merge group with the skin palette, so 4b.2's
         * cool grey-violet shadow is structural: {@code deep} IS
         * {@code SKIN_DEEP}, and a brown shadow is unreachable.
         *
         * <p>{@code dissolve} is 0 on every vertex — 4b.1, "a face may not fray
         * the way a hem does" — so the only edges this mesh can print are its
         * own soft coverage ramps.
         */
        SkinnedMesh buildFace(FaceParams p) {
            builder = new SkinnedMesh.Builder();
            Bone head = skeleton.bone("head");
            Bone jaw = skeleton.bone("jaw");
            Bone eye = skeleton.bone("eye");
            Vector2 c = skeleton.worldPosition(head.index, new Vector2());
            float cx = c.x + HEAD_LOBE_DX;
            float cy = c.y + HEAD_LOBE_DY;
            Random fr = new Random(p.seed());

            // Stations run down the face. Contour stations reuse the face-edge
            // table (via contourR) so skin and silhouette cannot disagree; the
            // two above 42 degrees follow the skull lobe (forehead into hairline).
            float[] ang = {74f, 58f, 42f, 25f, 10f, -2f, -12f, -22f, -34f, -50f, -64f};
            float[] rOut = new float[ang.length];
            // A pixel prouder at the crown (0.164/0.159, was 0.158/0.153):
            // the upper-front silhouette was the hair sim's ragged fringe,
            // and its boundary wandered a column every 3-5 rows — the head's
            // one long clean edge, shredded (try31: 4-5 px step runs at
            // x443-444 where the corpus holds 9-12). The skin's own spline
            // now owns that edge on both heads; the fringe frays over it.
            rOut[0] = 0.164f;
            rOut[1] = 0.159f;
            for (int i = 2; i < ang.length; i++) {
                // PROUD of the ink contour strip, not inside it. Pass 2: the skin
                // group owns the head's silhouette in the duel scenes, so the
                // visible edge is this mesh's feathered rim (InkMaterial#feather)
                // and not the body mesh's contour strip beneath — which draws in
                // the CLOTH material, and on the pale duellist printed CLOTH_PALE
                // washed flat by its own sash lift: the pale rim of the shard the
                // pass-1 review measured at 1.36x the sky. The body strip stays,
                // for the Family A scenes that draw rig.mesh() alone.
                rOut[i] = contourR(i - 2) * (i == 2 ? 1.065f : 1.05f);
            }
            // Value structure, from family B rather than family D, and pass 2
            // (second iteration) re-derives the STRUCTURE from the corpus, not
            // only the mean. The first iteration hit the 4b.2 ratio and went
            // flat doing it: the whole interior printed L 27-31, the authored
            // break of light sat on the feathered silhouette where blending
            // against an 87-luma sky lifted it to 74-76 — eight levels below
            // the sky, i.e. invisible — and the face read as a plain dark
            // shard with a floating specular. Measured on ref3's dark duellist
            // (x145..184 y182..231, sky 96..105), the corpus profile is FOUR
            // value events per row, inside out: the plane (L 12-24), a lit
            // break 2-4 px wide (L 45-61 at the nose, dipped dark 18-25 across
            // the socket and bridge, lit again 30-48 on the brow ridge), then
            // a dark contour LINE 1-2 px wide (L 33-39) between the break and
            // the sky — 4b.3's "one continuous flowing line", which is also
            // what stops the break from bleeding into the sky the way
            // iteration one's did. The rails below are those four events.
            float socket = 0.90f + 6f * p.socketDepth();   // deepens with age
            // The break of light per station: lit on the brow ridge (42),
            // dark across socket and bridge (25, 10), brightest on the nose
            // (-2), lit on lip and chin (-22, -34), dark under the jaw.
            // Third iteration: the lit break runs CONTINUOUSLY from the nose
            // through lip to chin (ref3 rows 207-231 hold 40-90 the whole
            // way), where the second iteration re-dipped at the philtrum and
            // cut the one near-vertical lit boundary the delivered profile
            // has into sub-run fragments (try20: 5 contiguous step rows at
            // the nose inner edge, one short of the instrument's own 6).
            // (A lit bridge was tried and reverted — try22: it printed 25-47,
            // too dim to count and a texture right at the eye box's edge.)
            float[] wLight = {0.85f, 0.75f, 0.28f, 0.70f, 0.68f, 0.10f, 0.28f, 0.12f, 0.24f, 0.72f, 0.92f};
            // The plane sits ON the ramp (~0.86), not at the ceiling: with
            // `deep` re-anchored to the corpus's below-the-plane register
            // (Figure.dark()), full wetness now means socket / contour line /
            // under-jaw — the registers the corpus paints BELOW its plane —
            // and a socket that is 8-10 levels under the plane exists at all.
            // Iteration two had plane == deep == socket == hair, one flat 26.
            float[] wMid = new float[ang.length];
            float[] wIn = new float[ang.length];
            // 0.55, not 0.80: the pool term saturates above ~0.60 wetness
            // (measured across try16/try17: 0.80, 0.86 and 1.0 all print the
            // deep register), so the plane sits under the knee and full
            // wetness keeps a real register below it for the socket.
            for (int i = 0; i < ang.length; i++) {
                wMid[i] = 0.55f;
                wIn[i] = 0.55f;
            }
            // The washes brightened one step (0.48/0.42 printed 26-30, a
            // Delta-L of barely 8 against the socket's 17-19 — the exact
            // knife-edge the instrument cannot be trusted to hold through
            // run-to-run noise): at 0.40/0.34 they print 33-36 and the
            // wash-to-socket turns, which run near-horizontal along the 25
            // and -12 degree station lines, become honest 1-px-readable
            // wet edges. Corpus cheek turn: 27-35 on a 12-24 plane.
            wMid[2] = 0.30f;                          // the lift under the ridge, prints ~40
            wMid[3] = Math.min(1f, socket);           // the socket shadow: BELOW the plane now
            wMid[4] = Math.min(1f, socket * 0.99f);
            wMid[6] = 0.26f;                          // the cheek's turn of light, prints ~40
            wMid[8] = 0.46f;

            // Blush and marks: asymmetric by construction (4b.7) — everything is
            // placed off the generator's own noise, nothing mirrored.
            float blushA = p.stainAmount() * (0.55f + 0.5f * fr.nextFloat());
            float blushB = p.stainAmount() * (0.30f + 0.5f * fr.nextFloat());
            float[] sLight = new float[ang.length];
            float[] sMid = new float[ang.length];
            // Halved from the third iteration: on the brightened washes the
            // full blush printed a flat tan blotch across the whole mid-face
            // (s3b-p2-try24) — 4b.7's "symmetric blush reads as makeup" in
            // spirit. The corpus's warmth is a small note, not a field.
            sMid[6] = blushA * 0.5f;             // cheek
            sMid[7] = blushB * 0.35f;
            sLight[5] = blushB * 0.5f;           // nose tip warmth
            sLight[7] = 0.85f;                   // the lip: pushes toward stainPale = LIP
            sLight[2] = 0.45f;                   // the corpus's coral rim, on the lit ridge

            // The rails are sampled through a Catmull-Rom spline of the stations,
            // three points per span. Pass 2, from the review's 4b.3 row: "the
            // profile is a chain of straight facets on both duellists; the foe's
            // contour stair-steps". Eleven stations at 220 px per world unit is a
            // vertex every 5-8 px, and a polyline at that pitch IS a chain of
            // facets, feathered or not; the corpus's contour is "one continuous
            // flowing line". The spline passes through every authored station —
            // identity still lives in the station table and FaceParams still
            // reshapes it — the flats between them just stop being flat.
            int n0 = ang.length;
            int sub = 3;
            int n = (n0 - 1) * sub + 1;
            float[] angS = new float[n];
            float[] rS = new float[n];
            float[] wLightS = new float[n];
            float[] wMidS = new float[n];
            float[] wInS = new float[n];
            float[] sLightS = new float[n];
            float[] sMidS = new float[n];
            for (int k = 0; k < n; k++) {
                int i = Math.min(k / sub, n0 - 2);
                float u = (k - i * sub) / (float) sub;
                angS[k] = catmullRom(ang, i, u);
                rS[k] = catmullRom(rOut, i, u);
                wLightS[k] = MathUtils.clamp(catmullRom(wLight, i, u), 0f, 1f);
                wMidS[k] = MathUtils.clamp(catmullRom(wMid, i, u), 0f, 1f);
                wInS[k] = MathUtils.clamp(catmullRom(wIn, i, u), 0f, 1f);
                sLightS[k] = MathUtils.clamp(catmullRom(sLight, i, u), 0f, 1f);
                sMidS[k] = MathUtils.clamp(catmullRom(sMid, i, u), 0f, 1f);
            }
            short[] vo = new short[n];
            short[] vo2 = new short[n];
            short[] vb = new short[n];
            short[] vl = new short[n];
            short[] vm = new short[n];
            short[] vi = new short[n];
            for (int i = 0; i < n; i++) {
                float ca = MathUtils.cosDeg(angS[i]);
                float sa = MathUtils.sinDeg(angS[i]);
                float t = i / (float) (n - 1);
                // ONE flow direction for the whole face, down it. This shader's
                // header warns what radial flow does on a head ("the black
                // dandelion"), and the subdivided rails made it measurable
                // again: with per-station flow the dry-brush smear direction
                // rotates wedge to wedge and the face prints as a fan of
                // radial pleats at exactly the station pitch (measured on
                // s3b-p2-try4 frame 22, foe head — and the same mechanism at
                // pass 1's coarser pitch is the review's "flat polygon wedges
                // whose facet boundaries change frame to frame"). A brush
                // models a cheek in one stroke.
                float flow = angleToU(-72f);
                float flowDown = flow;
                // Chin and below ride the jaw bone so the jaw channel reads.
                float jawW = angS[i] < -30f ? 0.55f : (angS[i] < -18f ? 0.25f : 0f);
                int bA = head.index;
                int bB = jaw.index;
                float wA = 1f - jawW;
                // Outermost rail: the contour LINE, not the break. Iteration
                // one put the break here and the feather blended it into the
                // sky (authored ~52, delivered 74-76 against sky 87 — nothing).
                // The corpus closes the profile with a dark line OUTSIDE the
                // break (ref3 nose rows 209-215: break 45-61, line 33-39, sky
                // 96-105), so the break reads against dark on BOTH sides and
                // the silhouette's feather blends dark-to-sky, which no bright
                // rim can survive.
                //
                // The widths are the second iteration's measured correction:
                // at ratios 1.0/0.956/0.90 on a 29-32 px face radius the line
                // was 1.3 px and the break 1.8, the 1.6 px feather ate the
                // line whole and the break printed one broken pixel
                // (s3b-p2-try14, hero rows 287-290: a lone 72-73 column).
                // Line 2.4 px (outer 1.6 is the feather's dark-to-sky ramp,
                // ~1 px solid), break 2.4, falloff 2.4 — corpus widths.
                vo[i] = builder.vertex(cx + rS[i] * ca, cy + rS[i] * sa, 1f, t,
                        0f, 0.97f, 0f, flow, bA, wA, bB, jawW);
                vo2[i] = builder.vertex(cx + rS[i] * 0.925f * ca, cy + rS[i] * 0.925f * sa, 0.92f, t,
                        0f, 0.97f, 0f, flow, bA, wA, bB, jawW);
                // The break of light, fully inside the line.
                vb[i] = builder.vertex(cx + rS[i] * 0.85f * ca, cy + rS[i] * 0.85f * sa, 0.85f, t,
                        0f, wLightS[i], sLightS[i], flow, bA, wA, bB, jawW);
                // The break's inner falloff: half a step down toward the plane,
                // so the inner edge is a wash edge (~2.4 px, 12-17 levels per
                // px where the break is lit) rather than either a cliff or a
                // 6-px gradient the facet instrument reads at base 2 only.
                // +0.28, not +0.35: at +0.35 the falloff-to-plane step sat
                // at 8-10 levels and the head's run-to-run noise (max delta
                // 88) flipped exactly those runs between reruns of one
                // command — the repro capture read 0.4 straight-edge runs
                // per 1000 px under its own twin. A criterion may not sit
                // inside its apparatus's noise floor (11.2b(g)).
                vl[i] = builder.vertex(cx + rS[i] * 0.775f * ca, cy + rS[i] * 0.775f * sa, 0.8f, t,
                        0f, Math.min(1f, wLightS[i] + 0.28f), sLightS[i] * 0.5f, flow, bA, wA, bB, jawW);
                vm[i] = builder.vertex(cx + rS[i] * 0.70f * ca, cy + rS[i] * 0.70f * sa, 0.45f, t,
                        0f, wMidS[i], sMidS[i], flowDown, bA, wA, bB, jawW);
                // u = 0.5 so the inner rail never feathers: the fan's core is
                // FILLED (below), and a rail that fed the fray band there would
                // print a pale seam ring through the middle of the face — the
                // first capture of this mesh had exactly that hole, and the old
                // cloth-coloured skull showed through it as a jagged pale patch.
                vi[i] = builder.vertex(cx + rS[i] * 0.34f * ca, cy + rS[i] * 0.34f * sa, 0.5f, t,
                        0f, wInS[i], 0f, flowDown, bA, wA, bB, jawW);
            }
            // (The contour line rides at 0.97 wetness: on the re-anchored deep
            // that is the corpus's own line register, ~0.2x sky, dark against
            // both the break inside it and the sky outside it.)
            for (int i = 0; i < n - 1; i++) {
                builder.quad(vo[i], vo[i + 1], vo2[i + 1], vo2[i]);
                builder.quad(vo2[i], vo2[i + 1], vb[i + 1], vb[i]);
                builder.quad(vb[i], vb[i + 1], vl[i + 1], vl[i]);
                builder.quad(vl[i], vl[i + 1], vm[i + 1], vm[i]);
                builder.quad(vm[i], vm[i + 1], vi[i + 1], vi[i]);
            }
            // The core fill behind the inner rail, at the same u so no fray band
            // can open between the two.
            short core = builder.vertex(cx + 0.010f, cy + 0.004f, 0.5f, 0.5f, 0f, 0.55f, 0f,
                    angleToU(-72f), head.index, 1f, head.index, 0f);
            for (int i = 0; i < n - 1; i++) {
                builder.triangle(core, vi[i], vi[i + 1]);
            }

            // The scalp: the rest of the skull, so the visible head is skin and
            // hair rather than garment. Without this the head keeps printing the
            // CLOTH material — which on the pale duellist is CLOTH_PALE washed
            // flat by its own sash lift: debt item 4's "pale face patch", the
            // amoeba, was never a face at all, it was the pale figure's HAORI
            // COLOUR on its skull. Dark and quiet: this is the ground the hair
            // mass roots into, and HairRenderer's own doc wants the topknot and
            // the hair "the same value and the same object".
            // Subdivided like the face rails, and for the cap it was measured
            // first: 13 stations on a 33-px-radius disc is a 12-px straight
            // chord per span, and once the sash-lift fix darkened the skull the
            // cap's own silhouette became the head's front boundary — two
            // near-collinear chords printed the 28-px vertical run of
            // s3b-p2-try10 (V x=601 y283..311), the exact class the facet
            // instrument polices. A skull is round.
            float[] ba2c = {58f, 78f, 100f, 122f, 145f, 168f, 190f, 212f, 232f, 252f, 272f, 284f, 296f};
            float[] br2c = {0.152f, 0.158f, 0.164f, 0.170f, 0.172f, 0.166f, 0.152f, 0.134f, 0.112f, 0.094f, 0.088f, 0.094f, 0.090f};
            int capN = (ba2c.length - 1) * sub + 1;
            float[] ba2 = new float[capN];
            float[] br2 = new float[capN];
            for (int k = 0; k < capN; k++) {
                int i = Math.min(k / sub, ba2c.length - 2);
                float u = (k - i * sub) / (float) sub;
                ba2[k] = catmullRom(ba2c, i, u);
                br2[k] = catmullRom(br2c, i, u);
            }
            float flowUp = angleToU(90f);
            short ctr = builder.vertex(cx, cy, 0.5f, 0.5f, 0f, 0.94f, 0f, flowUp,
                    head.index, 1f, head.index, 0f);
            short[] capIn = new short[ba2.length];
            short[] capOut = new short[ba2.length];
            for (int i = 0; i < ba2.length; i++) {
                float caB = MathUtils.cosDeg(ba2[i]);
                float saB = MathUtils.sinDeg(ba2[i]);
                // Out to the lobe's own radius: the cap must COVER the body
                // mesh's cloth-material lobe, which on the pale figure prints
                // the haori colour (debt 5.2's mechanism, half-fixed in pass 1
                // by a cap that stopped 1.5% short and printed a pale ring at
                // the wide framing — the 1.20x-sky blob of review 6.3).
                float rr2 = br2[i] * 1.005f;
                // Constant flow here too — the cap fanned the same way.
                float fl = flowUp;
                capIn[i] = builder.vertex(cx + rr2 * 0.45f * caB, cy + rr2 * 0.45f * saB,
                        0.5f + 0.225f * caB, 0.5f + 0.225f * saB, 0f, 0.95f, 0f, fl,
                        head.index, 1f, head.index, 0f);
                capOut[i] = builder.vertex(cx + rr2 * caB, cy + rr2 * saB,
                        0.5f + 0.5f * caB, 0.5f + 0.5f * saB, 0.02f, 0.90f, 0f, fl,
                        head.index, 1f, head.index, 0f);
            }
            for (int i = 0; i < ba2.length - 1; i++) {
                builder.triangle(ctr, capIn[i], capIn[i + 1]);
                builder.quad(capIn[i], capIn[i + 1], capOut[i + 1], capOut[i]);
            }

            // The topknot, capped the same way and for the same reason as the
            // scalp: it is a SECOND lobe the body mesh draws in the cloth
            // material, so on the pale duellist it printed CLOTH_PALE — §5.2's
            // "garment colour on the skull", one lobe over. Nobody saw it at
            // the intimate framing because the hair strands cover it there; at
            // the planning framing the strands thin and the pale figure wore a
            // white crescent for a topknot (s3b-p2-wide-try frame 0, the
            // brightest thing on the head). A topknot is hair and prints dark.
            float kx = cx + MathUtils.cosDeg(TOPKNOT_ANGLE_DEG) * TOPKNOT_DIST;
            float ky = cy + MathUtils.sinDeg(TOPKNOT_ANGLE_DEG) * TOPKNOT_DIST;
            float[] knotAngle = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};
            float[] knotRadius = {0.070f, 0.078f, 0.076f, 0.082f, 0.074f, 0.064f, 0.062f, 0.066f};
            short kc = builder.vertex(kx, ky, 0.5f, 0.5f, 0f, 0.97f, 0f, flowUp,
                    head.index, 1f, head.index, 0f);
            int knotN = knotAngle.length * sub;   // closed ring, subdivided
            short[] kOut = new short[knotN + 1];
            for (int i = 0; i <= knotN; i++) {
                int k2 = i % knotN;
                int j = k2 / sub;
                float u = (k2 - j * sub) / (float) sub;
                int j0 = (j + knotAngle.length - 1) % knotAngle.length;
                int j2 = (j + 1) % knotAngle.length;
                int j3 = (j + 2) % knotAngle.length;
                float aK = crWrap(knotAngle[j0] - (j == 0 ? 360f : 0f), knotAngle[j],
                        knotAngle[j2] + (j2 == 0 ? 360f : 0f),
                        knotAngle[j3] + (j3 <= 1 ? 360f : 0f), u);
                float rK = crWrap(knotRadius[j0], knotRadius[j], knotRadius[j2], knotRadius[j3], u);
                float caK = MathUtils.cosDeg(aK);
                float saK = MathUtils.sinDeg(aK);
                // Elongated along its own axis into a folded queue: ref3's
                // knot is an oblong that clears the crown with sky behind
                // it, and a knot buried in the hair mass contributes
                // nothing to the head's silhouette (every capture through
                // try25 printed it fully inside the mass). The stretch
                // points up-back (the TOPKNOT_ANGLE axis), tapering to a
                // round root, so the fold reads and the crown still owns it.
                float stretch = 1f + 0.60f * Math.max(0f, MathUtils.cosDeg(aK - TOPKNOT_ANGLE_DEG));
                float rr3 = rK * 1.005f * stretch;
                kOut[i] = builder.vertex(kx + rr3 * caK, ky + rr3 * saK,
                        0.5f + 0.5f * caK, 0.5f + 0.5f * saK, 0.02f, 0.93f, 0f, flowUp,
                        head.index, 1f, head.index, 0f);
            }
            for (int i = 0; i < knotN; i++) {
                builder.triangle(kc, kOut[i], kOut[i + 1]);
            }

            // The jaw/neck wedge, restated in the SKIN group. The body mesh's
            // wedge draws with the CLOTH material, and on the pale duellist the
            // sash lift washes it flat — the review's "jaw/neck wedge: weak
            // (foe)" row. Same polygon, same skinning, skin values: wetness at
            // the ceiling prints `deep`, which pass 2 anchors to the corpus's
            // own under-jaw register on both colourways.
            Bone neckBone = skeleton.bone("neck");
            // Subdivided through the same spline as the face rails, with a
            // slight bow on the resampled points. The first version kept the
            // body wedge's four straight chords, and its nape chord printed as
            // a dead-straight 2-px-soft vertical — invisible at base 1, a
            // 40-px run at base 2 (s3b-p2-try5, V x=621 y332..351): exactly
            // the between-the-bases signature the review's 5.4 convicted. A
            // shadow's edge wanders; a chord cannot.
            // The nape end stops short of the neck column on purpose: the
            // wedge's back edge and the trunk's own neck rail are both near-
            // vertical, and end to end they read as ONE long straight — the
            // 40-px base-2 run of try5/try6. Two short offset edges, not one.
            float[] wtx = {0.116f, 0.062f, 0.008f, -0.026f};
            float[] wty = {-0.046f, -0.052f, -0.052f, -0.050f};
            float[] wbx = {0.090f, 0.040f, -0.008f, -0.034f};
            float[] wby = {-0.112f, -0.132f, -0.138f, -0.120f};
            int wn = (wtx.length - 1) * sub + 1;
            short[] whi = new short[wn];
            short[] wlo = new short[wn];
            for (int k = 0; k < wn; k++) {
                int i = Math.min(k / sub, wtx.length - 2);
                float u = (k - i * sub) / (float) sub;
                float s = k / (float) (wn - 1);
                float bowW = 0.010f * MathUtils.sin(s * MathUtils.PI);
                float flowW = angleToU(200f - 30f * s);
                whi[k] = builder.vertex(cx + catmullRom(wtx, i, u), cy + catmullRom(wty, i, u),
                        s, 0f, 0f, 1f, 0f, flowW, head.index, 1f, head.index, 0f);
                wlo[k] = builder.vertex(cx + catmullRom(wbx, i, u) - bowW,
                        cy + catmullRom(wby, i, u) - bowW,
                        s, 1f, 0f, 0.97f, 0f, flowW, head.index, 0.72f, neckBone.index, 0.28f);
            }
            for (int i = 0; i < wn - 1; i++) {
                builder.quad(whi[i], whi[i + 1], wlo[i + 1], wlo[i]);
            }

            // The sclera: a sliver hanging off the eye bone so the lid (the
            // bone's scaleY) closes over it. Pass 2 dims it: on a face plane at
            // 0.25–0.31x sky, a near-dilute sliver was the second-brightest
            // object on the head and read as the "white dot" of the review's
            // eye finding. 4b.4 wants the sclera "usually partly shadowed by
            // the upper lid" — so it sits a third of the way up the ramp, pale
            // against the socket's ceiling-wetness shadow but never a highlight;
            // the one highlight is the specular, which is authored LAST and
            // dies first (4b.4's degradation order, restored).
            Vector2 ec = skeleton.worldPosition(eye.index, new Vector2());
            float el = 0.018f + 0.011f * p.eyeSize();
            short s0 = builder.vertex(ec.x - el * 0.9f, ec.y, 0f, 0f, 0f, 0.32f, 0f, 0.25f,
                    eye.index, 1f, eye.index, 0f);
            short s1 = builder.vertex(ec.x + el, ec.y, 1f, 0f, 0f, 0.28f, 0f, 0.25f,
                    eye.index, 1f, eye.index, 0f);
            // -0.011, not -0.0075: at 1.65 px tall the 1.6 px feather left
            // half a pixel of solid sclera and the eye's pale band never
            // printed more than a sliver (try24). The corpus's sclera/upper-
            // lid light is a 2 px band ~6 px long (ref3 rows 204-206).
            short s2 = builder.vertex(ec.x + el * 0.7f, ec.y - 0.011f, 1f, 1f, 0f, 0.36f, 0f, 0.25f,
                    eye.index, 1f, eye.index, 0f);
            short s3 = builder.vertex(ec.x - el * 0.6f, ec.y - 0.0105f, 0f, 1f, 0f, 0.38f, 0f, 0.25f,
                    eye.index, 1f, eye.index, 0f);
            builder.quad(s0, s1, s2, s3);

            // One scar at most, placed by the generator, warm-toned via the
            // stain channel (4b.5: "history rather than damage").
            if (p.scar() >= 0f) {
                float sa2 = 30f - 55f * p.scar();      // brow to cheek
                float rr = rOut[3] * (0.80f + 0.12f * fr.nextFloat());
                float sx = cx + rr * MathUtils.cosDeg(sa2);
                float sy = cy + rr * MathUtils.sinDeg(sa2);
                float dx = 0.012f + 0.008f * fr.nextFloat();
                float dy = -0.006f - 0.010f * fr.nextFloat();
                short a0 = builder.vertex(sx, sy, 0f, 0f, 0f, 0.25f, 1f, 0.1f, head.index, 1f, head.index, 0f);
                short a1 = builder.vertex(sx + dx, sy + dy, 1f, 0f, 0f, 0.30f, 1f, 0.1f, head.index, 1f, head.index, 0f);
                short a2 = builder.vertex(sx + dx + 0.002f, sy + dy - 0.003f, 1f, 1f, 0f, 0.30f, 0.6f, 0.1f, head.index, 1f, head.index, 0f);
                short a3 = builder.vertex(sx + 0.002f, sy - 0.003f, 0f, 1f, 0f, 0.25f, 0.6f, 0.1f, head.index, 1f, head.index, 0f);
                builder.quad(a0, a1, a2, a3);
            }
            return builder.build();
        }

        /**
         * The face's ink: brow, lash-and-iris, nostril, lip parting, and the
         * beard mass when the generator grew one. Everything here is small, dark
         * and meaningful, which is why it fades out with the camera
         * ({@code InkMaterial#covScale}) instead of shimmering at the planning
         * framing: STYLE.md 4b.0 — at 20-30 px of head, none of this survives,
         * and the skin field alone is the "suggestion of a face".
         */
        SkinnedMesh buildFaceInk(FaceParams p) {
            builder = new SkinnedMesh.Builder();
            Bone head = skeleton.bone("head");
            Bone brow = skeleton.bone("brow");
            Bone eye = skeleton.bone("eye");
            Bone jaw = skeleton.bone("jaw");
            Vector2 c = skeleton.worldPosition(head.index, new Vector2());
            float cx = c.x + HEAD_LOBE_DX;
            float cy = c.y + HEAD_LOBE_DY;

            // The brow: 4b.3's "heavy ink stroke", the primary expression carrier
            // at distance. A four-station strip with a slight bow, tapered at
            // both ends, its weight from the generator. Age drops its tail.
            Vector2 bc = skeleton.worldPosition(brow.index, new Vector2());
            // Pass 2: +0.0015 base width. The feather (InkMaterial#feather) costs
            // every mark its outer ~1.4 px of full coverage, so the marks that
            // must survive it are authored a shade wider, not printed harder.
            float bw = 0.0060f + 0.0075f * p.browWeight();
            float sag = 0.010f * p.age();
            // Pass 2 lifts the stroke onto the LIT brow ridge — the corpus's
            // "forehead + brow ridge with coral rim" puts the dark stroke on
            // lit ground, which is what makes it a readable mark; pass 1 drew
            // ink on the near-black socket plane, |dL| about 3, invisible to
            // the eye and to the instrument alike.
            // bv +0.006 over the second iteration: the stroke sat wholly
            // inside the socket passage (ink 13 on ground 14-16, invisible,
            // the exact defect class the FaceWindowTest javadoc names). Its
            // upper edge now borders the forehead wash (26-31), which is the
            // corpus's own construction: a dark brow line against lit ground.
            // Shifted inward (bu -0.008) so the stroke's length lies over
            // the brightened ridge wash rather than the socket passage: a
            // dark brow only exists where its ground is lit, and the wash
            // zone is the one lit ground at brow height (the corpus's own
            // construction, ref3 rows 197-204: stroke 12-27 on ground 29-48).
            float[] bu = {-0.011f, 0.005f, 0.023f, 0.039f};
            float[] bv = {0.019f - sag, 0.024f, 0.023f, 0.013f};
            float[] bt = {0.35f, 1f, 0.95f, 0.30f};
            short[] hi2 = new short[4];
            short[] lo2 = new short[4];
            for (int i = 0; i < 4; i++) {
                float t = i / 3f;
                hi2[i] = builder.vertex(bc.x + bu[i], bc.y + bv[i] + bw * bt[i] * 0.5f, t, 0f,
                        0f, 1f, 0f, 0.02f, brow.index, 1f, brow.index, 0f);
                lo2[i] = builder.vertex(bc.x + bu[i], bc.y + bv[i] - bw * bt[i] * 0.5f, t, 1f,
                        0f, 0.96f, 0f, 0.02f, brow.index, 1f, brow.index, 0f);
            }
            for (int i = 0; i < 3; i++) {
                builder.quad(hi2[i], hi2[i + 1], lo2[i + 1], lo2[i]);
            }

            // The lash line and iris, on the eye bone: heavier above than below
            // (there is no below), one dark mass the lid can close over. 4b.4's
            // degradation rule is authored in: at two surviving pixels these are
            // dark-iris-plus-specular, because the lash and iris are one dark
            // cluster and the specular is the scene's light speck.
            Vector2 ec = skeleton.worldPosition(eye.index, new Vector2());
            float el = 0.020f + 0.012f * p.eyeSize();
            short l0 = builder.vertex(ec.x - el, ec.y + 0.0035f, 0f, 0f, 0f, 1f, 0f, 0.5f,
                    eye.index, 1f, eye.index, 0f);
            short l1 = builder.vertex(ec.x + el * 1.05f, ec.y + 0.0045f, 1f, 0f, 0f, 1f, 0f, 0.5f,
                    eye.index, 1f, eye.index, 0f);
            short l2 = builder.vertex(ec.x + el * 0.9f, ec.y - 0.0015f, 1f, 1f, 0f, 0.92f, 0f, 0.5f,
                    eye.index, 1f, eye.index, 0f);
            short l3 = builder.vertex(ec.x - el * 0.85f, ec.y - 0.0005f, 0f, 1f, 0f, 0.92f, 0f, 0.5f,
                    eye.index, 1f, eye.index, 0f);
            builder.quad(l0, l1, l2, l3);
            // Iris: a small fan hanging from the lash, off-centre toward the gaze.
            // Pass 2 sizes it up: the review's eye finding is a specular with "no
            // dark iris behind it" — the iris is the anchor and must survive both
            // the feather and the 2-px degradation of 4b.4.
            float ir = 0.0100f + 0.0055f * p.eyeSize();
            float ix = ec.x + el * 0.18f;
            float iy = ec.y - 0.0025f;
            short icv = builder.vertex(ix, iy, 0.5f, 0.5f, 0f, 0.95f, 0f, 0.5f,
                    eye.index, 1f, eye.index, 0f);
            short[] ring = new short[7];
            for (int i = 0; i < 7; i++) {
                float a = (float) (Math.PI * 2.0 * i / 7.0);
                ring[i] = builder.vertex(ix + ir * MathUtils.cos(a), iy + ir * MathUtils.sin(a) * 0.85f,
                        0.5f + 0.5f * MathUtils.cos(a), 0.5f + 0.5f * MathUtils.sin(a),
                        0f, 0.90f, 0f, 0.5f, eye.index, 1f, eye.index, 0f);
            }
            for (int i = 0; i < 7; i++) {
                builder.triangle(icv, ring[i], ring[(i + 1) % 7]);
            }

            // Nostril and lip parting: two of 4b.3's "bonus" marks, cheap and
            // gone below push-in framing with the rest of this mesh.
            float nr = contourR(2) * 0.94f;
            float nx2 = cx + nr * MathUtils.cosDeg(-6f);
            float ny2 = cy + nr * MathUtils.sinDeg(-6f);
            // 0.94 wetness, not the old 0.58-0.62: with base == deep this
            // material prints its stroke colour only where the pool is full —
            // at mid wetness the resolve mixes toward dilute-on-paper and the
            // mark printed at ~45 on a lit band, which is why no capture of
            // pass 1 or 2 ever showed a nostril.
            short n0 = builder.vertex(nx2 - 0.0026f, ny2 + 0.002f, 0f, 0f, 0f, 0.94f, 0f, 0.3f, head.index, 1f, head.index, 0f);
            short n1 = builder.vertex(nx2 + 0.0026f, ny2 + 0.002f, 1f, 0f, 0f, 0.94f, 0f, 0.3f, head.index, 1f, head.index, 0f);
            short n2 = builder.vertex(nx2 + 0.002f, ny2 - 0.0026f, 1f, 1f, 0f, 0.92f, 0f, 0.3f, head.index, 1f, head.index, 0f);
            short n3 = builder.vertex(nx2 - 0.002f, ny2 - 0.0026f, 0f, 1f, 0f, 0.92f, 0f, 0.3f, head.index, 1f, head.index, 0f);
            builder.quad(n0, n1, n2, n3);
            // The lip parting. Two pass-2 changes, both from the review's mouth
            // finding (its §0 second failure): the quad's skin weights now MATCH
            // the skin field's own at this station (head 0.75 / jaw 0.25 — the
            // -22 degree band), where pass 1 gave it 0.6/0.4 and the mismatch
            // slid the mark off the chin as the jaw opened; and it sits at 0.90
            // of the contour, well inside the skin field's 1.03 silhouette, so
            // an open jaw can never carry it into open sky. Feathered like every
            // other mark: a mouth is a soft dark parting, not a rectangle.
            // 0.945: ON the lit lip band, not inside the dark plane — a dark
            // parting against lit ground, the corpus's own "lip + moustache".
            // Widened in pass 2's second iteration: the corpus's "lip +
            // moustache" is an 8-10 px mark and the 3.3 px version could not
            // carry a single 6-px facet run; on the now-lit lip band its two
            // long edges are the mark's whole readability.
            float lr = contourR(5) * 0.945f;
            float lx2 = cx + lr * MathUtils.cosDeg(-22f);
            float ly2 = cy + lr * MathUtils.sinDeg(-22f);
            short p0 = builder.vertex(lx2 - 0.021f, ly2 + 0.0008f, 0f, 0f, 0f, 0.90f, 0f, 0.3f,
                    head.index, 0.75f, jaw.index, 0.25f);
            short p1 = builder.vertex(lx2 + 0.009f, ly2 + 0.002f, 1f, 0f, 0f, 0.96f, 0f, 0.3f,
                    head.index, 0.75f, jaw.index, 0.25f);
            short p2 = builder.vertex(lx2 + 0.0065f, ly2 - 0.0012f, 1f, 1f, 0f, 0.94f, 0f, 0.3f,
                    head.index, 0.75f, jaw.index, 0.25f);
            short p3 = builder.vertex(lx2 - 0.017f, ly2 - 0.002f, 0f, 1f, 0f, 0.88f, 0f, 0.3f,
                    head.index, 0.75f, jaw.index, 0.25f);
            builder.quad(p0, p1, p2, p3);

            // The moustache, with any facial hair at all: the corpus's "lip +
            // moustache" is ONE compound mark (ref3 dark, rows 215-219: a
            // dark horizontal stroke over the lit lip), and it is the only
            // near-horizontal ink mark the profile owns at lip height. Rides
            // the same head/jaw blend as the parting so the mouth cluster
            // moves as one thing.
            if (p.facialHair() > 0.15f) {
                float mw = 0.010f + 0.014f * p.facialHair();
                short m0 = builder.vertex(lx2 - 0.007f - mw, ly2 + 0.0046f, 0f, 0f, 0f, 0.92f, 0f, 0.3f,
                        head.index, 0.75f, jaw.index, 0.25f);
                short m1 = builder.vertex(lx2 + 0.009f, ly2 + 0.0058f, 1f, 0f, 0f, 0.97f, 0f, 0.3f,
                        head.index, 0.75f, jaw.index, 0.25f);
                short m2 = builder.vertex(lx2 + 0.008f, ly2 + 0.0112f, 1f, 1f, 0f, 0.95f, 0f, 0.3f,
                        head.index, 0.75f, jaw.index, 0.25f);
                short m3 = builder.vertex(lx2 - 0.006f - mw, ly2 + 0.0100f, 0f, 1f, 0f, 0.90f, 0f, 0.3f,
                        head.index, 0.75f, jaw.index, 0.25f);
                builder.quad(m0, m1, m2, m3);
            }

            // The beard, when there is one: hair, not skin, so unlike everything
            // above it carries dissolve on its free rim and may fray (4b.1 exempts
            // the skin; a beard is the hairline's rule, not the face's).
            if (p.facialHair() > 0.05f) {
                float[] ba = {-14f, -32f, -50f, -68f, -84f};
                short[] bin = new short[ba.length];
                short[] bout = new short[ba.length];
                for (int i = 0; i < ba.length; i++) {
                    float t = i / (float) (ba.length - 1);
                    // Base radius: hug the contour where the contour exists,
                    // close over the under-jaw cutback below it.
                    float baseR = ba[i] > -40f ? contourR(6) : 0.118f;
                    // Reaches past the jaw wedge into the neck's 29-44
                    // register: a beard fringe on an under-jaw that is
                    // already ink-dark is invisible (Delta-L 3); ref3's
                    // beard reads where it crosses the lit collar band
                    // (rows 224-231, values 41-105 under strokes 15-27).
                    float outR = baseR + 0.014f + 0.085f * p.facialHair() * (0.55f + 0.45f * MathUtils.sinDeg(-ba[i]));
                    float caB = MathUtils.cosDeg(ba[i]);
                    float saB = MathUtils.sinDeg(ba[i]);
                    float flow = angleToU(ba[i] - 90f);
                    bin[i] = builder.vertex(cx + baseR * 0.82f * caB, cy + baseR * 0.82f * saB, 0f, t,
                            0f, 0.95f, 0f, flow, head.index, 0.45f, jaw.index, 0.55f);
                    bout[i] = builder.vertex(cx + outR * caB, cy + outR * saB, 1f, t,
                            0.35f, 0.90f, 0f, flow, head.index, 0.45f, jaw.index, 0.55f);
                }
                for (int i = 0; i < ba.length - 1; i++) {
                    builder.quad(bin[i], bin[i + 1], bout[i + 1], bout[i]);
                }
            }

            // The hairline — 4b.1's own order, never delivered until now:
            // "the hairline is where the two treatments meet... that boundary
            // should be a hard wet edge, not a blend." An authored root-line
            // arc under the sim's hair mass, from the crown front down the
            // temple, whose INNER edge cuts against the forehead wash — the
            // corpus part list's "hair mass / hairline" (review 11.0, part 3).
            // Hair-valued and drawn in the ink group, so it fades on pull-out
            // with every other authored mark and the wide framing keeps
            // 4b.0's suggestion-of-a-face.
            {
                float[] ha = {58f, 46f, 34f, 22f};
                short[] hIn = new short[ha.length];
                short[] hOut = new short[ha.length];
                for (int i = 0; i < ha.length; i++) {
                    float t = i / (float) (ha.length - 1);
                    float rIn = contourRAt(ha[i]) * (0.86f + 0.02f * t);
                    float rOut2 = rIn + 0.013f + 0.004f * MathUtils.sin(t * MathUtils.PI);
                    float caH = MathUtils.cosDeg(ha[i]);
                    float saH = MathUtils.sinDeg(ha[i]);
                    float fl = angleToU(ha[i] - 90f);
                    hIn[i] = builder.vertex(cx + rIn * caH, cy + rIn * saH, 0f, t,
                            0f, 0.94f, 0f, fl, head.index, 1f, head.index, 0f);
                    hOut[i] = builder.vertex(cx + rOut2 * caH, cy + rOut2 * saH, 1f, t,
                            0f, 0.97f, 0f, fl, head.index, 1f, head.index, 0f);
                }
                for (int i = 0; i < ha.length - 1; i++) {
                    builder.quad(hIn[i], hIn[i + 1], hOut[i + 1], hOut[i]);
                }
            }

            // Loose crown wisps — the second entry on the pass-1 review's
            // corpus part list, and a mark family this head simply lacked: a
            // few fine strands escaping the crown into open sky. Dark ink on
            // sky is the one high-contrast mark the ink floor cannot compress
            // (the review's finding that the delivered head under-marks the
            // corpus band is, on a floor-bound figure, mostly a statement
            // about marks that need a LIT ground — and the sky is one). Hair,
            // not skin: they ride the covScale fade like every other authored
            // mark, so nothing shimmers at the planning framing. Seeded and
            // asymmetric (4b.7).
            java.util.Random wr = new java.util.Random(p.seed() * 31L + 7L);
            for (int w = 0; w < 5; w++) {
                // Five, not three — the corpus crowns trail many escapees and
                // the two extra strands carry the last of the head's mark
                // density against the one ground the ink floor cannot
                // compress, the sky. 92+: clear of the face-front hair edge,
                // whose own near-vertical boundary a 78-degree wisp extended
                // into one compound 28-px straight (s3b-p2-try10, V x=599..601).
                // The first strand is the corpus's long nape strand
                // (prominent down the back of ref3's pale duellist): rooted
                // behind the skull, hanging toward the shoulder, barely
                // drooping. The curls curl; one strand hangs. Crown-rooted
                // versions of it grew straight out of the top of the frame's
                // head box and counted for nothing (tries 26-28).
                boolean hanging = w < 2;
                float a0 = hanging ? 186f + 13f * w + 8f * wr.nextFloat()
                        : 92f + 20f * w + 14f * (wr.nextFloat() - 0.5f);
                float r0 = SKULL_RADIUS * 0.98f;
                // Long enough that the tips clear the hair mass the strands
                // root under — the two extra strands of the fourth iteration
                // printed nothing because everything inside the mass is
                // painted over by the hair pass, which draws after the face.
                float len = hanging ? 0.100f + 0.030f * wr.nextFloat()
                        : 0.080f + 0.070f * wr.nextFloat();
                // Enough turn that no chord of the stroke is straight: the
                // first cut of this drooped only at the tip and printed a
                // 28-px vertical run — the very defect class Facets polices.
                float droop = hanging ? 12f + 10f * wr.nextFloat()
                        : 36f + 34f * wr.nextFloat();
                float hw0 = (hanging ? 0.0068f : 0.0048f) + 0.0016f * wr.nextFloat();
                int segs = 4;
                short[] wa = new short[segs + 1];
                short[] wb = new short[segs + 1];
                for (int i = 0; i <= segs; i++) {
                    float t = i / (float) segs;
                    float ang2 = a0 + droop * (0.55f * t + t * t);   // continuous turn, tip drooping hardest
                    float rr = r0 + len * t;
                    float px2 = cx + rr * MathUtils.cosDeg(ang2);
                    float py2 = cy + rr * MathUtils.sinDeg(ang2);
                    float hw = hw0 * (1f - 0.75f * t);
                    float nx3 = MathUtils.cosDeg(ang2 + 90f);
                    float ny3 = MathUtils.sinDeg(ang2 + 90f);
                    float fl = angleToU(ang2 + 90f);
                    wa[i] = builder.vertex(px2 + hw * nx3, py2 + hw * ny3, 0f, t,
                            0f, 0.97f, 0f, fl, head.index, 1f, head.index, 0f);
                    wb[i] = builder.vertex(px2 - hw * nx3, py2 - hw * ny3, 1f, t,
                            0f, 0.95f, 0f, fl, head.index, 1f, head.index, 0f);
                }
                for (int i = 0; i < segs; i++) {
                    builder.quad(wa[i], wa[i + 1], wb[i + 1], wb[i]);
                }
            }
            return builder.build();
        }

        // -- shared ribbon builder for anything that follows a bone chain -----

        /**
         * A row sample: {@code d} along {@code bone}'s bind direction, optionally
         * blended toward a neighbouring bone at a joint.
         *
         * <p>{@code skinOverride} separates <em>where the row is</em> from
         * <em>what carries it</em>. System 3 re-weights the sleeve's drape rows
         * onto cloth bones without moving them by a micron: re-deriving their
         * geometry from the cloth bones' own axes would rotate each row's rails
         * to a new perpendicular and quietly re-cut a silhouette three passes
         * were spent tuning.
         */
        private record RibbonPoint(Bone bone, float d, Bone blendBone, float blendWeight, Bone skinOverride) {
            static RibbonPoint of(Bone bone, float d) {
                return new RibbonPoint(bone, d, bone, 0f, null);
            }

            static RibbonPoint blended(Bone bone, float d, Bone blendBone, float blendWeight) {
                return new RibbonPoint(bone, d, blendBone, blendWeight, null);
            }

            RibbonPoint skinnedTo(Bone skin) {
                return new RibbonPoint(bone, d, blendBone, blendWeight, skin);
            }
        }

        /** Convenience for a three-bone limb chain (e.g. thigh/shin/foot), with a blended row at each joint. */
        private RibbonPoint[] chainPoints(String boneA, String boneB, String boneC,
                                           float lenA, float lenB, float lenC) {
            Bone a = skeleton.bone(boneA);
            Bone b = skeleton.bone(boneB);
            Bone c = skeleton.bone(boneC);
            return new RibbonPoint[] {
                    RibbonPoint.of(a, 0f),
                    RibbonPoint.blended(a, lenA, b, 0.4f),
                    RibbonPoint.of(b, lenB * 0.5f),
                    RibbonPoint.blended(b, lenB, c, 0.4f),
                    RibbonPoint.of(c, lenC),
            };
        }

        /** Symmetric ribbon: every row centred on the bone axis. */
        private void ribbon(RibbonPoint[] pts, float[] halfWidth, float[] dissolve, float[] wetness, float[] stainBase) {
            ribbon(pts, halfWidth, new float[pts.length], dissolve, wetness, stainBase);
        }

        /**
         * Builds a quad-strip between two rails on either side of a bone chain.
         * flowU is derived per-point from the bone's own bind rotation, so
         * streaks run along the limb (contract section C). {@code lateral}
         * offsets each row's centre across the bone axis, which is how a hanging
         * garment is made to trail behind the limb it hangs from instead of
         * being a symmetric tube around it.
         */
        private void ribbon(RibbonPoint[] pts, float[] halfWidth, float[] lateral,
                             float[] dissolve, float[] wetness, float[] stainBase) {
            int n = pts.length;
            short[] left = new short[n];
            short[] right = new short[n];
            for (int i = 0; i < n; i++) {
                RibbonPoint p = pts[i];
                float flow = angleToU(skeleton.worldRotationDeg(p.bone.index));
                Vector2 pl = alongBone(p.bone, p.d, lateral[i] + halfWidth[i]);
                Vector2 pr = alongBone(p.bone, p.d, lateral[i] - halfWidth[i]);
                float t = n <= 1 ? 0f : i / (float) (n - 1);
                int skinA = p.skinOverride != null ? p.skinOverride.index : p.bone.index;
                int skinB = p.skinOverride != null ? p.skinOverride.index : p.blendBone.index;
                float wA = p.skinOverride != null ? 1f : 1f - p.blendWeight;
                float wB = p.skinOverride != null ? 0f : p.blendWeight;
                left[i] = builder.vertex(pl.x, pl.y, 0f, t, dissolve[i], wetness[i], stainAt(stainBase[i]), flow,
                        skinA, wA, skinB, wB);
                right[i] = builder.vertex(pr.x, pr.y, 1f, t, dissolve[i], wetness[i], stainAt(stainBase[i]), flow,
                        skinA, wA, skinB, wB);
            }
            for (int i = 0; i < n - 1; i++) {
                builder.quad(left[i], left[i + 1], right[i + 1], right[i]);
            }
        }

        /** World-bind-space point at distance {@code d} along a bone's bind direction, offset {@code perp} to the side. */
        private Vector2 alongBone(Bone bone, float d, float perp) {
            Vector2 origin = skeleton.worldPosition(bone.index, new Vector2());
            float rot = skeleton.worldRotationDeg(bone.index);
            float cos = MathUtils.cosDeg(rot);
            float sin = MathUtils.sinDeg(rot);
            return new Vector2(origin.x + d * cos - perp * sin, origin.y + d * sin + perp * cos);
        }

        /**
         * Catmull-Rom through {@code arr} at parameter {@code u} of the span
         * {@code i..i+1}, endpoints clamped. Used to subdivide the face rails:
         * the spline passes through every authored station, so the authored
         * table (and everything {@link FaceParams} does to it) is preserved
         * exactly — only the straight flats between stations go.
         */
        /** Catmull-Rom on four explicit points (for closed rings). */
        private static float crWrap(float p0, float p1, float p2, float p3, float u) {
            float u2 = u * u;
            float u3 = u2 * u;
            return 0.5f * ((2f * p1)
                    + (p2 - p0) * u
                    + (2f * p0 - 5f * p1 + 4f * p2 - p3) * u2
                    + (3f * p1 - 3f * p2 + p0 - p3) * u3);
        }

        private static float catmullRom(float[] arr, int i, float u) {
            int last = arr.length - 1;
            float p0 = arr[Math.max(0, i - 1)];
            float p1 = arr[i];
            float p2 = arr[Math.min(last, i + 1)];
            float p3 = arr[Math.min(last, i + 2)];
            float u2 = u * u;
            float u3 = u2 * u;
            return 0.5f * ((2f * p1)
                    + (p2 - p0) * u
                    + (2f * p0 - 5f * p1 + 4f * p2 - p3) * u2
                    + (3f * p1 - 3f * p2 + p0 - p3) * u3);
        }

        private static float angleToU(float deg) {
            float d = deg % 360f;
            if (d < 0f) {
                d += 360f;
            }
            return d / 360f;
        }

        private static float[] scaled(float scale, float... values) {
            float[] out = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                out[i] = values[i] * scale;
            }
            return out;
        }

        /** Sparse blotchy variation around a base stain strength; base=0 rows stay stain-free. Seeded, so deterministic across builds. */
        private float stainAt(float base) {
            if (base <= 0f) {
                return 0f;
            }
            return MathUtils.clamp(base * (0.3f + 1.4f * rnd.nextFloat()), 0f, 1f);
        }
    }
}
