package dev.starfall.anim;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parent-first bone hierarchy plus the 2D affine math to turn per-bone local
 * transforms into GPU skinning matrices. API per docs/system1-contract.md section D.
 *
 * <p>Transforms are tracked internally as 2x3 affines {@code [a b c d tx ty]}
 * meaning {@code x' = a*x + c*y + tx, y' = b*x + d*y + ty} — a flat float layout
 * rather than libGDX's Matrix4/Affine2 because every bone in this rig is a pure
 * 2D transform and doing the composition by hand avoids ambiguity about which
 * of libGDX's several 2D matrix conventions is in play.
 */
public final class Skeleton {

    private static final int AFFINE_SIZE = 6; // a, b, c, d, tx, ty

    private final List<Bone> bones;
    private final Map<String, Bone> byName;

    /** Bind-pose global transform, inverted once at construction. Never changes after that. */
    private final float[] inverseBind;

    /** Scratch: this frame's global transform per bone, recomputed by updateWorldTransforms(). */
    private final float[] global;

    /** Scratch local-affine buffer for computeGlobals, hoisted out of the per-bone loop: this runs every animated frame and must not allocate. */
    private final float[] localScratch = new float[AFFINE_SIZE];

    public Skeleton(List<Bone> bonesInParentFirstOrder) {
        this.bones = new ArrayList<>(bonesInParentFirstOrder);
        this.byName = new HashMap<>();
        for (Bone b : bones) {
            if (b.parent != null && b.parent.index >= b.index) {
                throw new IllegalArgumentException(
                        "Bones must be in parent-first order: " + b + " comes after its parent " + b.parent);
            }
            if (b.index != bones.indexOf(b)) {
                // Cheap sanity check that Bone.index matches its position in this list;
                // fillSkinningMatrices below indexes by that field directly.
                throw new IllegalArgumentException("Bone " + b + " has index != its position in the bone list");
            }
            byName.put(b.name, b);
        }

        this.global = new float[bones.size() * AFFINE_SIZE];
        this.inverseBind = new float[bones.size() * AFFINE_SIZE];

        // Compute bind-pose globals once (bones start life reset to bind, see Bone
        // constructor), then invert each into inverseBind. This is the "T-pose" the
        // authored mesh vertices live in; every later pose is expressed relative to it.
        computeGlobals(global);
        for (int i = 0; i < bones.size(); i++) {
            invertAffine(global, i * AFFINE_SIZE, inverseBind, i * AFFINE_SIZE);
        }
    }

    public int boneCount() {
        return bones.size();
    }

    public Bone bone(int index) {
        return bones.get(index);
    }

    public Bone bone(String name) {
        Bone b = byName.get(name);
        if (b == null) {
            throw new IllegalArgumentException("No bone named '" + name + "'");
        }
        return b;
    }

    /** Recomputes global transforms from local ones. Parent-first, single pass. */
    public void updateWorldTransforms() {
        computeGlobals(global);
    }

    private void computeGlobals(float[] dst) {
        for (Bone b : bones) {
            int o = b.index * AFFINE_SIZE;
            localAffine(b, localScratch, 0);
            if (b.parent == null) {
                System.arraycopy(localScratch, 0, dst, o, AFFINE_SIZE);
            } else {
                multiplyAffine(dst, b.parent.index * AFFINE_SIZE, localScratch, 0, dst, o);
            }
        }
    }

    /** Fills dst with boneCount*16 floats: globalPose * inverseBind, column-major mat4. */
    public void fillSkinningMatrices(float[] dst) {
        float[] skin = new float[AFFINE_SIZE];
        for (Bone b : bones) {
            int o = b.index * AFFINE_SIZE;
            multiplyAffine(global, o, inverseBind, o, skin, 0);
            affineToMat4(skin, 0, dst, b.index * 16);
        }
    }

    /** World-space position of a bone's origin, after updateWorldTransforms(). */
    public Vector2 worldPosition(int boneIndex, Vector2 out) {
        int o = boneIndex * AFFINE_SIZE;
        out.set(global[o + 4], global[o + 5]);
        return out;
    }

    /**
     * A point {@code distance} along a bone's own +x axis, in world space.
     *
     * <p>No mirror term is needed and that is worth stating, because every other
     * bone-local-to-world helper in this project carries one: column {@code (a, b)}
     * <em>is</em> the bone's +x axis expressed in world space, so a pure along-bone
     * offset is already reflected by the transform. Only the {@code y} component of
     * a local offset needs {@code RigIk.fromBone}'s correction.
     */
    public Vector2 worldAlong(int boneIndex, float distance, Vector2 out) {
        int o = boneIndex * AFFINE_SIZE;
        float ax = global[o];
        float ay = global[o + 1];
        float len = (float) Math.sqrt(ax * ax + ay * ay);
        if (len < 1e-9f) {
            return out.set(global[o + 4], global[o + 5]);
        }
        return out.set(global[o + 4] + distance * ax / len, global[o + 5] + distance * ay / len);
    }

    public float worldRotationDeg(int boneIndex) {
        int o = boneIndex * AFFINE_SIZE;
        // Column (a, b) is the local +x axis expressed in world space; atan2 of it
        // gives the world angle regardless of positive scale magnitude.
        return MathUtils.atan2(global[o + 1], global[o + 0]) * MathUtils.radDeg;
    }

    // -- 2x3 affine helpers ------------------------------------------------

    private static void localAffine(Bone b, float[] dst, int o) {
        float cos = MathUtils.cosDeg(b.rotDeg);
        float sin = MathUtils.sinDeg(b.rotDeg);
        dst[o] = cos * b.scaleX;
        dst[o + 1] = sin * b.scaleX;
        dst[o + 2] = -sin * b.scaleY;
        dst[o + 3] = cos * b.scaleY;
        dst[o + 4] = b.x;
        dst[o + 5] = b.y;
    }

    /** dst = m1 * m2 (apply m2 first, then m1). dst may alias m1 or m2's array as long as offsets don't overlap the read. */
    private static void multiplyAffine(float[] m1, int o1, float[] m2, int o2, float[] dst, int od) {
        float a1 = m1[o1], b1 = m1[o1 + 1], c1 = m1[o1 + 2], d1 = m1[o1 + 3], tx1 = m1[o1 + 4], ty1 = m1[o1 + 5];
        float a2 = m2[o2], b2 = m2[o2 + 1], c2 = m2[o2 + 2], d2 = m2[o2 + 3], tx2 = m2[o2 + 4], ty2 = m2[o2 + 5];

        float a = a1 * a2 + c1 * b2;
        float b = b1 * a2 + d1 * b2;
        float c = a1 * c2 + c1 * d2;
        float d = b1 * c2 + d1 * d2;
        float tx = a1 * tx2 + c1 * ty2 + tx1;
        float ty = b1 * tx2 + d1 * ty2 + ty1;

        dst[od] = a;
        dst[od + 1] = b;
        dst[od + 2] = c;
        dst[od + 3] = d;
        dst[od + 4] = tx;
        dst[od + 5] = ty;
    }

    private static void invertAffine(float[] m, int o, float[] dst, int od) {
        float a = m[o], b = m[o + 1], c = m[o + 2], d = m[o + 3], tx = m[o + 4], ty = m[o + 5];
        float det = a * d - b * c;
        if (Math.abs(det) < 1e-12f) {
            throw new IllegalStateException("Degenerate bind-pose transform (zero scale?) at offset " + o);
        }
        float invDet = 1f / det;
        float ia = d * invDet;
        float ib = -b * invDet;
        float ic = -c * invDet;
        float id = a * invDet;
        float itx = -(ia * tx + ic * ty);
        float ity = -(ib * tx + id * ty);
        dst[od] = ia;
        dst[od + 1] = ib;
        dst[od + 2] = ic;
        dst[od + 3] = id;
        dst[od + 4] = itx;
        dst[od + 5] = ity;
    }

    /** Expands a 2x3 affine into a column-major mat4 (z untouched, per contract section B). */
    private static void affineToMat4(float[] m, int o, float[] dst, int od) {
        float a = m[o], b = m[o + 1], c = m[o + 2], d = m[o + 3], tx = m[o + 4], ty = m[o + 5];
        dst[od] = a;
        dst[od + 1] = b;
        dst[od + 2] = 0f;
        dst[od + 3] = 0f;
        dst[od + 4] = c;
        dst[od + 5] = d;
        dst[od + 6] = 0f;
        dst[od + 7] = 0f;
        dst[od + 8] = 0f;
        dst[od + 9] = 0f;
        dst[od + 10] = 1f;
        dst[od + 11] = 0f;
        dst[od + 12] = tx;
        dst[od + 13] = ty;
        dst[od + 14] = 0f;
        dst[od + 15] = 1f;
    }
}
