package dev.starfall.anim;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Skinning-matrix math is the one place in this project where a sign error is
 * invisible in isolation and corrupts every downstream visual iteration — a
 * subtly wrong inverse-bind produces a figure that still looks like a figure,
 * just permanently slightly wrong. These tests pin the properties the renderer
 * relies on so that a broken rig fails here rather than in a contact sheet.
 */
class SkeletonTest {

    private static final float EPS = 1e-4f;

    /** root -> a (offset +10 x) -> b (offset +10 x), all unrotated at bind. */
    private static Skeleton chain() {
        Bone root = new Bone("root", 0, null);
        Bone a = new Bone("a", 1, root);
        a.bindX = 10f;
        Bone b = new Bone("b", 2, a);
        b.bindX = 10f;
        for (Bone bone : List.of(root, a, b)) {
            bone.resetToBind();
        }
        return new Skeleton(List.of(root, a, b));
    }

    @Test
    void bindPoseProducesIdentitySkinningMatrices() {
        Skeleton s = chain();
        s.updateWorldTransforms();

        float[] m = new float[s.boneCount() * 16];
        s.fillSkinningMatrices(m);

        // globalPose * inverseBind must be exactly identity while the pose is the
        // bind pose, otherwise the mesh is deformed before any animation runs.
        for (int bone = 0; bone < s.boneCount(); bone++) {
            for (int i = 0; i < 16; i++) {
                float expected = (i % 5 == 0) ? 1f : 0f; // diagonal of a column-major mat4
                assertEquals(expected, m[bone * 16 + i], EPS,
                        "bone " + bone + " element " + i + " should be identity at bind pose");
            }
        }
    }

    @Test
    void bindPoseWorldPositionsAccumulateThroughTheChain() {
        Skeleton s = chain();
        s.updateWorldTransforms();

        Vector2 out = new Vector2();
        assertEquals(0f, s.worldPosition(0, out).x, EPS);
        assertEquals(10f, s.worldPosition(1, out).x, EPS);
        assertEquals(20f, s.worldPosition(2, out).x, EPS);
        assertEquals(0f, s.worldPosition(2, out).y, EPS);
    }

    @Test
    void parentRotationPropagatesToDescendants() {
        Skeleton s = chain();
        // Rotate the middle bone 90 degrees; its child must swing from +x to +y
        // about the middle bone's origin at (10, 0).
        s.bone("a").rotDeg = 90f;
        s.updateWorldTransforms();

        Vector2 out = new Vector2();
        s.worldPosition(2, out);
        assertEquals(10f, out.x, EPS, "child should stay at the parent's x");
        assertEquals(10f, out.y, EPS, "child should swing to +y");

        assertEquals(90f, s.worldRotationDeg(2), EPS,
                "rotation must accumulate down the chain, not just translation");
    }

    @Test
    void rootTranslationMovesEveryDescendant() {
        Skeleton s = chain();
        s.bone("root").x = 5f;
        s.bone("root").y = -3f;
        s.updateWorldTransforms();

        Vector2 out = new Vector2();
        assertEquals(25f, s.worldPosition(2, out).x, EPS);
        assertEquals(-3f, s.worldPosition(2, out).y, EPS);
    }

    @Test
    void negativeRootScaleXFlipsFacingWithoutBreakingTheChain() {
        // Facing is flipped by negating root scaleX rather than rebuilding the mesh
        // (docs/system1-rig-fixes.md section 1), so this must stay well-defined.
        Skeleton s = chain();
        s.bone("root").scaleX = -1f;
        s.updateWorldTransforms();

        Vector2 out = new Vector2();
        assertEquals(-20f, s.worldPosition(2, out).x, EPS, "chain should mirror about the root");

        float[] m = new float[s.boneCount() * 16];
        s.fillSkinningMatrices(m);
        for (float v : m) {
            assertTrue(Float.isFinite(v), "mirrored skinning matrices must stay finite");
        }
    }

    @Test
    void skinningMatrixMapsBindPositionToPosedPosition() {
        Skeleton s = chain();
        // A vertex authored at bone b's bind origin (20, 0).
        float bindX = 20f;
        float bindY = 0f;

        s.bone("a").rotDeg = 90f;
        s.updateWorldTransforms();

        float[] m = new float[s.boneCount() * 16];
        s.fillSkinningMatrices(m);

        // Apply bone b's skinning matrix the same way the vertex shader does.
        int o = 2 * 16;
        float x = m[o] * bindX + m[o + 4] * bindY + m[o + 12];
        float y = m[o + 1] * bindX + m[o + 5] * bindY + m[o + 13];

        Vector2 expected = s.worldPosition(2, new Vector2());
        assertEquals(expected.x, x, EPS, "skinned vertex must land on the posed bone origin");
        assertEquals(expected.y, y, EPS, "skinned vertex must land on the posed bone origin");
    }
}
