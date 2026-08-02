package dev.starfall.anim;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

/**
 * A GPU mesh built with the exact vertex layout in docs/system1-contract.md
 * section A: position, material-space UV, the four-channel ink material tint
 * (section C), and four bone-weight pairs. One SkinnedMesh holds every garment
 * region of a rig in a single draw call.
 */
public final class SkinnedMesh {

    /** Layout is the hard boundary with side B's shader (contract section A) — do not reorder. */
    public static final VertexAttributes ATTRIBUTES = new VertexAttributes(
            VertexAttribute.Position(),
            VertexAttribute.TexCoords(0),
            VertexAttribute.ColorUnpacked(),
            VertexAttribute.BoneWeight(0),
            VertexAttribute.BoneWeight(1),
            VertexAttribute.BoneWeight(2),
            VertexAttribute.BoneWeight(3));

    /** Derived, never hardcoded, per contract section A. */
    public static final int FLOATS_PER_VERTEX = ATTRIBUTES.vertexSize / 4;

    private final Mesh mesh;
    private final int vertexCount;

    public SkinnedMesh(float[] vertices, short[] indices) {
        if (vertices.length % FLOATS_PER_VERTEX != 0) {
            throw new IllegalArgumentException(
                    "vertex array length " + vertices.length + " is not a multiple of " + FLOATS_PER_VERTEX);
        }
        this.vertexCount = vertices.length / FLOATS_PER_VERTEX;
        this.mesh = new Mesh(true, vertexCount, indices.length, ATTRIBUTES);
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
    }

    public Mesh mesh() {
        return mesh;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public void dispose() {
        mesh.dispose();
    }

    /**
     * Accumulates vertices/triangles for one or more garment regions into a single
     * buffer, so a rig with several authored shapes (haori, sleeves, hakama, ...)
     * still ends up as one draw call. Not part of the contract API; a construction
     * convenience local to how rigs are authored.
     */
    public static final class Builder {

        private final FloatArray vertices = new FloatArray(1024);
        private final ShortArray indices = new ShortArray(1536);

        /** Appends one vertex with up to four bone influences; weights are normalised to sum to 1. Returns the vertex index for use with triangle()/quad(). */
        public short vertex(float x, float y, float u, float v,
                             float dissolve, float wetness, float stainMask, float flowU01,
                             int[] boneIndices, float[] boneWeights) {
            if (boneIndices.length > 4 || boneIndices.length != boneWeights.length) {
                throw new IllegalArgumentException("at most 4 bone influences, indices/weights must match length");
            }
            float sum = 0f;
            for (float w : boneWeights) {
                sum += w;
            }
            if (sum <= 0f) {
                throw new IllegalArgumentException("bone weights must sum to > 0");
            }

            int start = vertices.size;
            vertices.add(x, y, 0f);           // a_position (z always 0)
            vertices.add(u, v);                // a_texCoord0, material-space
            vertices.add(dissolve, wetness, stainMask, flowU01); // a_color, section C
            for (int slot = 0; slot < 4; slot++) {
                if (slot < boneIndices.length) {
                    vertices.add(boneIndices[slot], boneWeights[slot] / sum);
                } else {
                    vertices.add(0f, 0f); // unused slot: index 0, weight 0 -- never a garbage index
                }
            }
            return (short) (start / FLOATS_PER_VERTEX);
        }

        /** Single-bone convenience: full-weight rigid skin. */
        public short vertex(float x, float y, float u, float v,
                             float dissolve, float wetness, float stainMask, float flowU01,
                             int boneIndex) {
            return vertex(x, y, u, v, dissolve, wetness, stainMask, flowU01,
                    new int[] {boneIndex}, new float[] {1f});
        }

        /** Two-bone convenience: linear blend, e.g. across a joint. */
        public short vertex(float x, float y, float u, float v,
                             float dissolve, float wetness, float stainMask, float flowU01,
                             int boneA, float weightA, int boneB, float weightB) {
            return vertex(x, y, u, v, dissolve, wetness, stainMask, flowU01,
                    new int[] {boneA, boneB}, new float[] {weightA, weightB});
        }

        public void triangle(short a, short b, short c) {
            indices.add(a, b, c);
        }

        /** Two triangles (a,b,c) and (a,c,d); winding assumes a-b-c-d runs around the quad. */
        public void quad(short a, short b, short c, short d) {
            triangle(a, b, c);
            triangle(a, c, d);
        }

        public SkinnedMesh build() {
            return new SkinnedMesh(vertices.toArray(), indices.toArray());
        }
    }
}
