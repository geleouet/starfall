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
     * side B addresses bones by name. 21 bones, well under the 24 budget the
     * contract reserves for System 3's cloth bones (32 hard cap).
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

        // Indices 21-23 intentionally unused: reserved for System 3 cloth bones.

        return new Skeleton(bones);
    }

    private static Bone add(List<Bone> bones, Bone b) {
        bones.add(b);
        return b;
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

            buildLimbSliver(chainPoints("thighR", "shinR", "footR", 0.44f, 0.40f, 0.14f), 0.040f, 0.16f, 0.42f, 0.88f);
            buildHakama(skeleton.bone("thighR"), skeleton.bone("shinR"), skeleton.bone("footR"), 0.84f);
            buildLimbSliver(chainPoints("upperArmR", "forearmR", "handR", 0.19f, 0.14f, 0.09f), 0.028f, 0.18f, 0.06f, 0.14f);
            buildSleeve(skeleton.bone("upperArmR"), skeleton.bone("forearmR"), skeleton.bone("handR"),
                    0.19f, 0.14f, 0.09f, 0.22f, 0.78f);

            buildTrunk(hips, spine, chest, skeleton.bone("neck"), skeleton.bone("head"));
            buildHaori(hips, spine, chest);

            // The worn pair, over the haori and under everything on the near
            // side. Their whole point is that they leave the garment silhouette
            // behind the hip and carry on into open paper.
            buildDaisho(hips, spine);

            // Near leg *under* its own hakama, not over it (rig-fixes-3 item 3):
            // a solid sliver composited on top of the skirt is what read as a
            // translucent tube with a bright centre seam.
            buildLimbSliver(chainPoints("thighL", "shinL", "footL", 0.44f, 0.40f, 0.14f), 0.055f, 1f, 0.45f, 0.95f);
            buildHakama(skeleton.bone("thighL"), skeleton.bone("shinL"), skeleton.bone("footL"), 1f);

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
                    0.30f, 0.26f, 0.10f, 0.42f, 1f);
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
            // Collar sits below the throat (head's lowest point is ~1.55) so the
            // neck -- not the garment -- is what connects to the head.
            float[] frontY = {1.46f, 1.36f, 1.22f, 1.06f, 0.92f, 0.74f, 0.56f, 0.40f, 0.26f, 0.14f};
            float[] frontX = {0.10f, 0.30f, 0.26f, 0.21f, 0.16f, 0.16f, 0.18f, 0.16f, 0.11f, 0.04f};
            float[] backY = {1.46f, 1.40f, 1.28f, 1.12f, 0.96f, 0.78f, 0.58f, 0.36f, 0.12f, -0.16f};
            float[] backX = {-0.08f, -0.34f, -0.32f, -0.25f, -0.19f, -0.20f, -0.22f, -0.24f, -0.26f, -0.23f};
            // Genuinely 0 through the shoulder-to-waist run (rows 0-3): the
            // dense core. Climbs only once the flare starts (row 4+).
            // The fray starts lower than revision 2 had it and then goes off a
            // cliff. Pass 2 measured luminance 60 at the mid-torso against
            // 137-160 at the hem, and the cause is coverage rather than value:
            // a row that is a third dissolved is a third transparent, so paper
            // shows through it and it measures pale no matter how wet it is
            // authored. Keeping the skirt genuinely opaque down to the last two
            // rows is what lets the pigment pooling below actually print.
            float[] dissolveF = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.10f, 0.55f, 1.0f};
            float[] dissolveB = {0f, 0f, 0f, 0f, 0f, 0f, 0.05f, 0.20f, 0.70f, 1.0f};
            // rig-fixes-3 item 4, the ink gravity. The chest is now the *lightest*
            // part of the wash and the pigment pools to near-black in the row
            // immediately above the fray line -- pass 2 measured luminance 60 at
            // the mid-torso against 137-160 at the hem, i.e. exactly backwards.
            float[] wetF = {0.03f, 0.06f, 0.12f, 0.22f, 0.36f, 0.55f, 0.74f, 0.92f, 1.0f, 0.85f};
            float[] wetB = {0.04f, 0.08f, 0.16f, 0.28f, 0.44f, 0.63f, 0.82f, 0.96f, 1.0f, 0.82f};
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
            float[] stainF = {0f, 0.07f, 0.15f, 0.17f, 0.09f, 0.14f, 0.11f, 0.03f, 0f, 0f};
            float[] stainB = {0f, 0.08f, 0.16f, 0.18f, 0.10f, 0.06f, 0.03f, 0f, 0f, 0f};

            int n = frontY.length;
            short[] front = new short[n];
            short[] back = new short[n];
            for (int i = 0; i < n; i++) {
                float s = i / (float) (n - 1);
                BoneBlend bbF = trunkBlend(s, hips, spine, chest);
                BoneBlend bbB = trunkBlend(Math.min(1f, s * 1.05f), hips, spine, chest);
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

        /** s=0 at the collar, s=1 at the hem: chest -> spine -> hips, since System 1 has no dedicated cloth bones yet. */
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

        private void buildHakama(Bone thigh, Bone shin, Bone foot, float scale) {
            // Revision 3: every row now hangs off the thigh or the shin, never
            // off the foot. footL/footR bind to world rotation 0 (they point
            // +X), so a half-width on a foot row offsets *vertically* -- the two
            // trailing rows of revision 2 were therefore a pair of 0.66-tall
            // vertical smears thrown forward from each ankle, and between them
            // they were the widest thing in the figure by a wide margin.
            RibbonPoint[] pts = {
                    RibbonPoint.of(thigh, 0.02f),
                    RibbonPoint.of(thigh, 0.24f),
                    RibbonPoint.blended(thigh, 0.44f, shin, 0.4f),
                    RibbonPoint.of(shin, 0.20f),
                    RibbonPoint.of(shin, 0.38f),
                    RibbonPoint.blended(shin, 0.54f, foot, 0.25f), // gathered past the ankle, into ink
            };
            // Wide-legged at the thigh, gathered at the ankle -- which is what a
            // hakama actually does, and is the shape in references 1 and 2 where
            // the leg below the knee is a narrow dark tube.
            float[] halfWidth = scaled(scale, 0.088f, 0.112f, 0.104f, 0.080f, 0.063f, 0.054f);
            float[] dissolve = {0f, 0f, 0f, 0f, 0.30f, 1.0f};
            // Far leg gets a higher dissolve floor throughout -- it never reads
            // as fully solid, which is what "lower contrast / recedes behind
            // the body" means when both legs share one InkMaterial draw call.
            float contrastFloor = scale < 1f ? 0.10f : 0f;
            for (int i = 0; i < dissolve.length; i++) {
                dissolve[i] = Math.max(dissolve[i], contrastFloor);
            }
            // Hakama and legs are the darkest masses in the figure (rig-fixes-3
            // item 4): wetness climbs hard and peaks just above the fray. Stain
            // is now zero throughout -- the ochre in the lower body printed as a
            // pair of bright vertical bars down the legs, which is exactly the
            // "bright vertical seam" item 3 fails on.
            float[] wetness = {0.42f, 0.62f, 0.80f, 0.94f, 1.0f, 0.78f};
            float[] stainBase = {0f, 0f, 0f, 0f, 0f, 0f};
            ribbon(pts, halfWidth, dissolve, wetness, stainBase);
        }

        // -- sleeves: wide, hanging, heavy dissolve at the opening ------------

        private void buildSleeve(Bone upperArm, Bone forearm, Bone hand,
                                  float upperArmLen, float forearmLen, float handLen, float drape, float scale) {
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
            float[] hw = {0.105f, 0.110f, 0.120f, 0.080f, 0.074f};
            // Ink gravity again: the trunk is the lightest thing in the figure
            // at the top and pools toward the hips, matching the haori over it.
            float[] wet = {0.55f, 0.30f, 0.10f, 0.06f, 0.06f};
            int n = chain.length;
            short[] left = new short[n];
            short[] right = new short[n];
            for (int i = 0; i < n; i++) {
                Vector2 o = skeleton.worldPosition(chain[i].index, new Vector2());
                float t = i / (float) (n - 1);
                float flow = angleToU(90f);
                left[i] = builder.vertex(o.x - hw[i], o.y, 0f, t, 0f, wet[i], 0f, flow,
                        chain[i].index, 1f, chain[i].index, 0f);
                right[i] = builder.vertex(o.x + hw[i], o.y, 1f, t, 0f, wet[i], 0f, flow,
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
            float cx = c.x + 0.012f;
            float cy = c.y + 0.048f;
            // rig-fixes-3 item 7: a *directional* mass, not a radially uniform
            // blob. Angle 0 is +X, the way the figure faces. Three things carry
            // the read at 45 px: a solid opaque crown that bulges up and back
            // (the hair mass), a clean interruption on the jaw side at 225-300
            // where the profile cuts in and the neck takes over, and the topknot
            // as a separate lobe below.
            float[] angle =  {0f, 25f, 50f, 75f, 100f, 125f, 150f, 175f, 200f, 225f, 250f, 275f, 300f, 330f};
            float[] radius = {0.128f, 0.132f, 0.142f, 0.152f, 0.162f, 0.170f, 0.167f, 0.150f,
                              0.127f, 0.104f, 0.094f, 0.114f, 0.112f, 0.122f};
            headLobe(head, cx, cy, angle, radius, 0.55f, 0f, 0.02f, 0.62f);

            // Topknot: offset up and back far enough that it clears the skull
            // outline and reads as its own lobe, but overlapping enough that it
            // is welded rather than floating. Slightly frayed at its own rim --
            // it is hair, and full strand simulation is System 3's problem.
            float lx = cx + MathUtils.cosDeg(140f) * 0.146f;
            float ly = cy + MathUtils.sinDeg(140f) * 0.146f;
            float[] knotAngle = {0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f};
            float[] knotRadius = {0.070f, 0.078f, 0.076f, 0.082f, 0.074f, 0.064f, 0.062f, 0.066f};
            headLobe(head, lx, ly, knotAngle, knotRadius, 0.50f, 0f, 0.05f, 0.70f);
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

        // -- shared ribbon builder for anything that follows a bone chain -----

        /** A row sample: {@code d} along {@code bone}'s bind direction, optionally blended toward a neighbouring bone at a joint. */
        private record RibbonPoint(Bone bone, float d, Bone blendBone, float blendWeight) {
            static RibbonPoint of(Bone bone, float d) {
                return new RibbonPoint(bone, d, bone, 0f);
            }

            static RibbonPoint blended(Bone bone, float d, Bone blendBone, float blendWeight) {
                return new RibbonPoint(bone, d, blendBone, blendWeight);
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
                left[i] = builder.vertex(pl.x, pl.y, 0f, t, dissolve[i], wetness[i], stainAt(stainBase[i]), flow,
                        p.bone.index, 1f - p.blendWeight, p.blendBone.index, p.blendWeight);
                right[i] = builder.vertex(pr.x, pr.y, 1f, t, dissolve[i], wetness[i], stainAt(stainBase[i]), flow,
                        p.bone.index, 1f - p.blendWeight, p.blendBone.index, p.blendWeight);
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
