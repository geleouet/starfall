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

    private SamuraiRig(Skeleton skeleton, SkinnedMesh mesh, SkinnedMesh bladeMesh) {
        this.skeleton = skeleton;
        this.mesh = mesh;
        this.bladeMesh = bladeMesh;
    }

    public static SamuraiRig build() {
        Skeleton skeleton = buildSkeleton();
        // Mesh authoring below reads bone world positions/rotations, so the
        // skeleton must have its bind-pose globals computed first.
        skeleton.updateWorldTransforms();
        MeshAuthor author = new MeshAuthor(skeleton);
        SkinnedMesh mesh = author.buildBody();
        SkinnedMesh bladeMesh = author.buildBlade();
        return new SamuraiRig(skeleton, mesh, bladeMesh);
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
        add(bones, new Bone("blade", 10, handL).bindLocal(0.10f, 0f, 45f));

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
        addClothChain(bones, hips, HAORI_BACK_X, HAORI_BACK_Y, HAORI_CLOTH_ROW0,
                "clothBackA", "clothBackB", "clothBackC");
        addClothChain(bones, hips, HAORI_FRONT_X, HAORI_FRONT_Y, HAORI_CLOTH_ROW0,
                "clothFrontA", "clothFrontB");
        addSleeveChain(bones, handL);

        return new Skeleton(bones);
    }

    private static Bone add(List<Bone> bones, Bone b) {
        bones.add(b);
        return b;
    }

    // -- cloth bone authoring -------------------------------------------------

    /** First haori row the cloth chains take over. Rows above it stay on hips/spine/chest. */
    private static final int HAORI_CLOTH_ROW0 = 6;

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

    /** The near sleeve's drape, which hangs off the wrist and is the second-largest trailing mass in the figure. */
    private static void addSleeveChain(List<Bone> bones, Bone hand) {
        Vector2 h = bindWorldPos(hand, new Vector2());
        float handRot = bindWorldRotDeg(hand);
        float c = MathUtils.cosDeg(handRot);
        float s = MathUtils.sinDeg(handRot);
        int n = SLEEVE_NODE_D.length;
        float[] nx = new float[n];
        float[] ny = new float[n];
        for (int i = 0; i < n; i++) {
            float d = SLEEVE_NODE_D[i];
            float perp = SLEEVE_NODE_LATERAL[i];
            nx[i] = h.x + d * c - perp * s;
            ny[i] = h.y + d * s + perp * c;
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
        private SkinnedMesh.Builder builder;

        MeshAuthor(Skeleton skeleton) {
            this.skeleton = skeleton;
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
         * heel to crown. <strong>This number is the correction of a specification
         * error.</strong> docs/system1-rig-fixes.md section 3 asked for 0.75 and
         * that was simply wrong: measured in the bind pose the blade rendered at
         * roughly 90% of body height and never terminated inside the frame. A
         * katana's nagasa is about 70 cm on a 170 cm swordsman, so the number is
         * 0.40, and rig-fixes section 3 is superseded on this point.
         */
        private static final float BLADE_NAGASA_FRACTION = 0.40f;

        /** Heel (y=0.13) to crown (y=1.83) in the world-bind-space units of this file. */
        private static final float FIGURE_HEIGHT = 1.70f;

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
            float[] dissolveB = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.14f, 0.58f, 1.0f};
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
            // The rows the cloth chains take over, per rail. Row 6 is the pivot
            // and stays mostly on the hips -- a vertex sitting exactly on a
            // bone's origin is unmoved by that bone's rotation, so this blend is
            // about how the weights *interpolate* across the quads above it, not
            // about row 6 itself. Below that each row hangs off the bone whose
            // tip it is, which is the standard chain weighting: rotating bone A
            // moves everything from its tip outward and nothing above it.
            Bone backA = skeleton.bone("clothBackA");
            Bone backB = skeleton.bone("clothBackB");
            Bone backC = skeleton.bone("clothBackC");
            Bone frontA = skeleton.bone("clothFrontA");
            Bone frontB = skeleton.bone("clothFrontB");
            BoneBlend[] clothF = new BoneBlend[n];
            BoneBlend[] clothB = new BoneBlend[n];
            clothB[6] = new BoneBlend(hips, 0.45f, backA, 0.55f);
            clothB[7] = new BoneBlend(backA, 1f, backA, 0f);
            clothB[8] = new BoneBlend(backB, 1f, backB, 0f);
            clothB[9] = new BoneBlend(backC, 1f, backC, 0f);
            clothF[6] = new BoneBlend(hips, 0.45f, frontA, 0.55f);
            clothF[7] = new BoneBlend(frontA, 1f, frontA, 0f);
            // The front chain is two bones rather than three: the front hem rises
            // to clear the leg and its last two rows are 0.16 units of frayed
            // smoke, so a third bone would buy a swing nobody can see against the
            // one spare the 32-bone cap has left after the sleeve.
            clothF[8] = new BoneBlend(frontB, 1f, frontB, 0f);
            clothF[9] = new BoneBlend(frontB, 1f, frontB, 0f);

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
        }

        private record BoneBlend(Bone boneA, float weightA, Bone boneB, float weightB) {
        }

        /** s=0 at the collar, s=1 at the hem: chest -> spine -> hips. Rows at or below {@link #HAORI_CLOTH_ROW0} use the cloth chains instead. */
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
        private void buildFoot(Bone foot, float scale) {
            RibbonPoint[] pts = {
                    RibbonPoint.of(foot, -0.052f),   // heel
                    RibbonPoint.of(foot, -0.012f),
                    RibbonPoint.of(foot, 0.048f),
                    RibbonPoint.of(foot, 0.104f),
                    RibbonPoint.of(foot, 0.152f),    // toe
            };
            float[] halfWidth = scaled(scale, 0.046f, 0.052f, 0.045f, 0.034f, 0.019f);
            float[] lateral = scaled(scale, -0.010f, -0.018f, -0.028f, -0.036f, -0.042f);
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
                // Rows 4, 5, 6 are the drape. Row 4 sits on sleeveA's origin, row
                // 5 on its tip and row 6 on sleeveB's tip, which is the same
                // tip-of-the-bone weighting the haori hem uses.
                pts[4] = pts[4].skinnedTo(clothBones[0]);
                pts[5] = pts[5].skinnedTo(clothBones[0]);
                pts[6] = pts[6].skinnedTo(clothBones[1]);
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
            float[] dissolve = {0f, 0f, 0.02f, 0.05f, 0.15f, 0.55f, 1.0f};
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
            float[] a = { 42f,  25f,  10f,  -2f, -12f, -22f, -34f, -50f, -66f};
            float[] r = {0.150f, 0.146f, 0.134f, 0.162f, 0.130f, 0.126f, 0.154f, 0.106f, 0.090f};
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
