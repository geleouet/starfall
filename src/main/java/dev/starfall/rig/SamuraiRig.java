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
        add(bones, new Bone("head", 5, neck).bindLocal(0f, 0.10f, 0f));

        // Sword arm (near/front side). Absolute (world) bind angle is roughly
        // -65 at the shoulder, -75 through the elbow, so bindRotDeg here backs
        // out chest's inherited -10 lean to hit that target.
        Bone shoulderL = add(bones, new Bone("shoulderL", 6, chest).bindLocal(0.11f, 0.18f, -55f));
        Bone upperArmL = add(bones, new Bone("upperArmL", 7, shoulderL).bindLocal(0.04f, 0f, 0f));
        Bone forearmL = add(bones, new Bone("forearmL", 8, upperArmL).bindLocal(0.30f, 0f, -10f));
        Bone handL = add(bones, new Bone("handL", 9, forearmL).bindLocal(0.26f, 0f, 0f));
        // Blade bind angle ~-45 absolute: a long forward-reaching diagonal, tip
        // just grazing the ground, per rig-fixes section 3.
        add(bones, new Bone("blade", 10, handL).bindLocal(0.10f, 0f, 30f));

        // Off arm (far/back side): shorter, tucked, attached higher on the chest.
        Bone shoulderR = add(bones, new Bone("shoulderR", 11, chest).bindLocal(-0.09f, 0.24f, -85f));
        Bone upperArmR = add(bones, new Bone("upperArmR", 12, shoulderR).bindLocal(0.02f, 0f, 0f));
        Bone forearmR = add(bones, new Bone("forearmR", 13, upperArmR).bindLocal(0.19f, 0f, -30f));
        add(bones, new Bone("handR", 14, forearmR).bindLocal(0.14f, 0f, 0f));

        // Near/front leg, planted slightly forward.
        Bone thighL = add(bones, new Bone("thighL", 15, hips).bindLocal(0.13f, -0.01f, -85f));
        Bone shinL = add(bones, new Bone("shinL", 16, thighL).bindLocal(0.44f, 0f, 0f));
        add(bones, new Bone("footL", 17, shinL).bindLocal(0.40f, 0f, 85f));

        // Far/back leg, trailing, smaller.
        Bone thighR = add(bones, new Bone("thighR", 18, hips).bindLocal(-0.13f, -0.01f, -97f));
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

            buildLimbSliver(chainPoints("thighR", "shinR", "footR", 0.44f, 0.40f, 0.14f), 0.040f, 0.16f, 0.20f, 0.42f);
            buildHakama(skeleton.bone("thighR"), skeleton.bone("shinR"), skeleton.bone("footR"), 0.82f);
            buildLimbSliver(chainPoints("upperArmR", "forearmR", "handR", 0.19f, 0.14f, 0.09f), 0.028f, 0.18f, 0.08f, 0.18f);
            buildSleeve(skeleton.bone("upperArmR"), skeleton.bone("forearmR"), skeleton.bone("handR"),
                    0.19f, 0.14f, 0.09f, 0.22f, 0.78f);

            buildTrunk(hips, spine, chest, skeleton.bone("neck"), skeleton.bone("head"));
            buildHaori(hips, spine, chest);

            buildHakama(skeleton.bone("thighL"), skeleton.bone("shinL"), skeleton.bone("footL"), 1f);
            buildLimbSliver(chainPoints("thighL", "shinL", "footL", 0.44f, 0.40f, 0.14f), 0.052f, 1f, 0.25f, 0.55f);

            buildHead(skeleton.bone("head"));

            buildLimbSliver(chainPoints("upperArmL", "forearmL", "handL", 0.30f, 0.26f, 0.10f), 0.045f, 1f, 0.10f, 0.25f);
            buildSleeve(skeleton.bone("upperArmL"), skeleton.bone("forearmL"), skeleton.bone("handL"),
                    0.30f, 0.26f, 0.10f, 0.42f, 1f);

            return builder.build();
        }

        SkinnedMesh buildBlade() {
            builder = new SkinnedMesh.Builder();
            Bone blade = skeleton.bone("blade");

            // rig-fixes section 3: ~0.75 of total figure height (~1.72), thin,
            // with a slight sori curvature convex away from the edge so it
            // doesn't read as a straight stick. Single bone, so the curve has
            // to be baked into the authored geometry rather than a joint chain.
            float len = 1.30f;
            int n = 7;
            float[] d = new float[n];
            float[] halfWidth = new float[n];
            float[] bow = new float[n];
            for (int i = 0; i < n; i++) {
                float t = i / (float) (n - 1);
                d[i] = t * len;
                // Widest just past the guard, tapering to a fine point.
                halfWidth[i] = MathUtils.lerp(0.017f, 0f, t * t) * (1f - 0.15f * t);
                // Peak curvature around the middle third, easing to 0 at both ends.
                bow[i] = 0.030f * MathUtils.sin(t * MathUtils.PI) ;
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
            float[] frontY = {1.44f, 1.34f, 1.20f, 1.02f, 0.84f, 0.62f, 0.38f, 0.20f, 0.08f, 0.02f};
            float[] frontX = {0.07f, 0.42f, 0.30f, 0.20f, 0.27f, 0.36f, 0.30f, 0.19f, 0.09f, 0.01f};
            float[] backY = {1.44f, 1.38f, 1.28f, 1.12f, 0.95f, 0.76f, 0.50f, 0.20f, -0.12f, -0.50f};
            float[] backX = {-0.06f, -0.40f, -0.40f, -0.42f, -0.55f, -0.73f, -0.90f, -1.00f, -1.04f, -1.00f};
            // Genuinely 0 through the shoulder-to-waist run (rows 0-3): the
            // dense core. Climbs only once the flare starts (row 4+).
            float[] dissolveF = {0f, 0f, 0f, 0f, 0f, 0.06f, 0.30f, 0.65f, 0.95f, 1.0f};
            float[] dissolveB = {0f, 0f, 0f, 0f, 0.04f, 0.15f, 0.35f, 0.60f, 0.85f, 1.0f};
            // Wetness peaks one row *before* full dissolve, not alongside it --
            // pigment pools at the trailing edge of the wash just above where it
            // frays, then the frayed flecks themselves carry less (rig-fixes
            // review, finding E). Legs/hakama below share the same shape.
            float[] wetF = {0.05f, 0.08f, 0.14f, 0.20f, 0.30f, 0.45f, 0.62f, 0.80f, 0.70f, 0.55f};
            float[] wetB = {0.05f, 0.10f, 0.18f, 0.28f, 0.42f, 0.58f, 0.75f, 0.88f, 0.78f, 0.60f};
            float[] stainF = {0f, 0f, 0.04f, 0.12f, 0.20f, 0.16f, 0.08f, 0.03f, 0f, 0f};
            float[] stainB = {0f, 0f, 0.06f, 0.16f, 0.22f, 0.19f, 0.13f, 0.06f, 0.02f, 0f};

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
            RibbonPoint[] pts = {
                    RibbonPoint.of(thigh, 0.05f),
                    RibbonPoint.blended(thigh, 0.44f, shin, 0.4f),
                    RibbonPoint.of(shin, 0.26f),
                    RibbonPoint.blended(shin, 0.40f, foot, 0.4f),
                    RibbonPoint.of(foot, 0.14f),
                    RibbonPoint.of(foot, 0.34f), // trailing hem past the ankle, into ink
            };
            float[] halfWidth = scaled(scale, 0.16f, 0.22f, 0.32f, 0.44f, 0.56f, 0.66f);
            float[] dissolve = {0f, 0f, 0.05f, 0.25f, 0.60f, 1.0f};
            // Far leg gets a higher dissolve floor throughout -- it never reads
            // as fully solid, which is what "lower contrast / recedes behind
            // the body" means when both legs share one InkMaterial draw call.
            float contrastFloor = scale < 1f ? 0.16f : 0f;
            for (int i = 0; i < dissolve.length; i++) {
                dissolve[i] = Math.max(dissolve[i], contrastFloor);
            }
            // Hakama and legs are meant to be among the darkest masses in the
            // figure (rig-fixes review, finding E), not a mid-slate mush zone --
            // wetness climbs hard and peaks at the ankle, just above the fray.
            float[] wetness = {0.15f, 0.30f, 0.50f, 0.75f, 0.90f, 0.55f};
            float[] stainBase = {0f, 0.05f, 0.10f, 0.05f, 0f, 0f};
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
            float[] halfWidth = scaled(scale, 0.10f, 0.11f, 0.13f, 0.16f, 0.19f, 0.24f, 0.30f);
            float[] dissolve = {0f, 0f, 0.02f, 0.05f, 0.15f, 0.55f, 1.0f};
            float contrastFloor = scale < 1f ? 0.14f : 0f;
            for (int i = 0; i < dissolve.length; i++) {
                dissolve[i] = Math.max(dissolve[i], contrastFloor);
            }
            float[] wetness = {0.05f, 0.08f, 0.12f, 0.18f, 0.25f, 0.40f, 0.55f};
            float[] stainBase = {0f, 0f, 0f, 0.05f, 0.10f, 0.15f, 0.10f};
            ribbon(pts, halfWidth, dissolve, wetness, stainBase);
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
            float contrastFloor = scale < 1f ? 0.12f : 0f;
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
            float[] hw = {0.095f, 0.100f, 0.115f, 0.105f, 0.085f};
            int n = chain.length;
            short[] left = new short[n];
            short[] right = new short[n];
            for (int i = 0; i < n; i++) {
                Vector2 o = skeleton.worldPosition(chain[i].index, new Vector2());
                float t = i / (float) (n - 1);
                float flow = angleToU(90f);
                left[i] = builder.vertex(o.x - hw[i], o.y, 0f, t, 0f, 0.10f, 0f, flow,
                        chain[i].index, 1f, chain[i].index, 0f);
                right[i] = builder.vertex(o.x + hw[i], o.y, 1f, t, 0f, 0.10f, 0f, flow,
                        chain[i].index, 1f, chain[i].index, 0f);
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
            float cx = c.x + 0.02f;
            float cy = c.y + 0.06f;
            float[] angle = {0f, 25f, 55f, 90f, 120f, 150f, 180f, 210f, 240f, 270f, 300f, 330f};
            float[] radius = {0.135f, 0.118f, 0.110f, 0.122f, 0.148f, 0.178f, 0.150f, 0.118f, 0.088f, 0.082f, 0.092f, 0.110f};
            int n = angle.length;
            float innerScale = 0.52f;
            float flowUp = angleToU(90f);

            short center = builder.vertex(cx, cy, 0.5f, 0.5f, 0f, 0.10f, 0f, flowUp, head.index, 1f, head.index, 0f);
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
                        0f, 0.10f, 0f, flow, head.index, 1f, head.index, 0f);
                outer[k] = builder.vertex(cx + rOut * ca, cy + rOut * sa,
                        0.5f + 0.5f * ca, 0.5f + 0.5f * sa,
                        0.05f, 0.08f, 0f, flow, head.index, 1f, head.index, 0f);
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

        /** Builds a quad-strip between two rails on either side of a bone chain. flowU is derived per-point from the bone's own bind rotation, so streaks run along the limb (contract section C). */
        private void ribbon(RibbonPoint[] pts, float[] halfWidth, float[] dissolve, float[] wetness, float[] stainBase) {
            int n = pts.length;
            short[] left = new short[n];
            short[] right = new short[n];
            for (int i = 0; i < n; i++) {
                RibbonPoint p = pts[i];
                float flow = angleToU(skeleton.worldRotationDeg(p.bone.index));
                Vector2 pl = alongBone(p.bone, p.d, halfWidth[i]);
                Vector2 pr = alongBone(p.bone, p.d, -halfWidth[i]);
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
