package dev.starfall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import dev.starfall.art.Palette;
import dev.starfall.sim.HairSim;

/**
 * Draws {@link HairSim}'s strands as the tapered ribbons of STYLE.md 4.
 *
 * <p>Three requirements from that section decide everything here.
 *
 * <p><b>"Strand paths must be smoothed (Catmull-Rom through the particles) -- a
 * visible polyline kink is an instant fail."</b> A ten-particle strand half a
 * body-length long has segments 15 px across at capture framing, and the angle
 * between two of them under a gust is easily 20 degrees; drawn as a polyline
 * that is a visible corner every 15 px. Each segment is therefore subdivided
 * {@link #SEGMENT_SAMPLES} times along a Catmull-Rom through the particles,
 * which makes the spine C1 -- the same fix, and the same reasoning, as the blade
 * trail's spline in {@link InkSkinnedRenderer#drawTrail}.
 *
 * <p><b>"Wide and near-opaque at the root, narrowing to sub-pixel and
 * near-transparent at the tip... the taper must be nonlinear (fast narrowing in
 * the last third)."</b> See {@link #taper}. The sub-pixel half of it is the
 * vertex shader's job, because a polygon thinner than a pixel is not thin, it is
 * gone -- debt D2's lesson about the kissaki, restated for something an order of
 * magnitude smaller.
 *
 * <p><b>Value, not outline.</b> The debt documents' hardest-won lesson is that a
 * mark authored within four luminance levels of what it crosses is invisible
 * however well it is shaped. Hair crosses two things: open paper at luminance
 * ~218, and the garment at ~58. So the root is {@code INK_BLACK} (~26), a full
 * cloth ramp below the garment it lies on, and the tips lift only as far as
 * {@code INDIGO_MID} (~81) -- and they earn that because by then they are in
 * open paper, where 81 against 218 is still an enormous mark. Nothing in the
 * hair is ever within reach of the garment's own value.
 */
public final class HairRenderer {

    /** Catmull-Rom samples per particle-to-particle segment. */
    private static final int SEGMENT_SAMPLES = 6;

    private static final int MAX_VERTS = 16384;
    private static final int MAX_INDICES = 24576;

    private static final VertexAttributes ATTRIBUTES = new VertexAttributes(
            new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 3, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_generic"));

    private static final int FLOATS = ATTRIBUTES.vertexSize / 4;

    private final ShaderProgram shader;
    private final Mesh mesh;
    private final float[] verts = new float[MAX_VERTS * FLOATS];
    private final short[] indices = new short[MAX_INDICES];
    private int vertCount;
    private int indexCount;

    private final Matrix4 projTrans = new Matrix4();
    private float time;
    private float pxWorld = 1f / 320f;
    private float fogStrength = 1f;

    private final Color rootInk = new Color(Palette.INK_BLACK);
    private final Color tipInkLow = new Color(Palette.INDIGO_DEEP);
    private final Color tipInkHigh = new Color(Palette.INDIGO_MID);

    // Scratch for one strand's spine.
    private final float[] spineX = new float[256];
    private final float[] spineY = new float[256];
    private final float[] spineArc = new float[256];

    public HairRenderer() {
        this.shader = InkSkinnedRenderer.Shaders.load("hair", "hair");
        this.mesh = new Mesh(false, MAX_VERTS, MAX_INDICES, ATTRIBUTES);
    }

    /**
     * @param worldPerPixel world units covered by one screen pixel. STYLE.md
     *                      3b.1's pixel footprint, handed down from the camera
     *                      rather than measured off a rendered image.
     */
    public void begin(Matrix4 projTrans, float timeSeconds, float worldPerPixel) {
        this.projTrans.set(projTrans);
        this.time = timeSeconds;
        this.pxWorld = Math.max(1e-6f, worldPerPixel);
        vertCount = 0;
        indexCount = 0;
    }

    /** How far the hair fades into the fog bands. 0 disables it (the debug scenes). */
    public HairRenderer fogStrength(float s) {
        this.fogStrength = s;
        return this;
    }

    public void draw(HairSim hair) {
        for (int i = 0; i < hair.strandCount(); i++) {
            appendStrand(hair.strand(i));
        }
    }

    public void end() {
        if (indexCount == 0) {
            return;
        }
        mesh.setVertices(verts, 0, vertCount * FLOATS);
        mesh.setIndices(indices, 0, indexCount);

        shader.bind();
        shader.setUniformMatrix("u_projTrans", projTrans);
        shader.setUniformf("u_time", time);
        shader.setUniformf("u_pxWorld", pxWorld);
        shader.setUniformf("u_fogStrength", fogStrength);
        shader.setUniformf("u_fogColor", Palette.FOG.r, Palette.FOG.g, Palette.FOG.b);
        Atmosphere.setFogUniforms(shader);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        // Straight alpha, not the screen operator the blade's glow uses. Hair is
        // pigment: it can only ever darken what it is laid over, and screening it
        // would make the darkest object in the figure lighten the paper.
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, indexCount);
    }

    public void dispose() {
        shader.dispose();
        mesh.dispose();
    }

    // -- geometry --------------------------------------------------------------

    private void appendStrand(HairSim.Strand strand) {
        int n = strand.particleCount();
        if (n < 2) {
            return;
        }
        int samples = Math.min(spineX.length, (n - 1) * SEGMENT_SAMPLES + 1);
        int k = 0;
        for (int seg = 0; seg < n - 1 && k < samples; seg++) {
            int last = seg == n - 2 ? SEGMENT_SAMPLES : SEGMENT_SAMPLES - 1;
            for (int j = 0; j <= last && k < samples; j++) {
                float u = j / (float) SEGMENT_SAMPLES;
                int i0 = Math.max(0, seg - 1);
                int i1 = seg;
                int i2 = Math.min(n - 1, seg + 1);
                int i3 = Math.min(n - 1, seg + 2);
                spineX[k] = catmullRom(strand.x(i0), strand.x(i1), strand.x(i2), strand.x(i3), u);
                spineY[k] = catmullRom(strand.y(i0), strand.y(i1), strand.y(i2), strand.y(i3), u);
                k++;
            }
        }
        int count = k;
        if (count < 2) {
            return;
        }

        spineArc[0] = 0f;
        for (int i = 1; i < count; i++) {
            float dx = spineX[i] - spineX[i - 1];
            float dy = spineY[i] - spineY[i - 1];
            spineArc[i] = spineArc[i - 1] + (float) Math.sqrt(dx * dx + dy * dy);
        }

        int base = vertCount;
        for (int i = 0; i < count; i++) {
            // Central difference for the tangent, so the normal is continuous
            // through every sample rather than stepping at segment joins.
            int a = Math.max(0, i - 1);
            int b = Math.min(count - 1, i + 1);
            float tx = spineX[b] - spineX[a];
            float ty = spineY[b] - spineY[a];
            float len = (float) Math.sqrt(tx * tx + ty * ty);
            if (len < 1e-7f) {
                tx = 1f;
                ty = 0f;
            } else {
                tx /= len;
                ty /= len;
            }
            float nx = -ty;
            float ny = tx;

            float s = i / (float) (count - 1);
            float halfWidth = strand.rootHalfWidth * taper(s);
            float alpha = strand.rootAlpha * opacity(s);

            float valueT = Math.min(1f, s * 1.15f);
            float r = lerp(rootInk.r, tipColour(strand.valueBias, 0), valueT);
            float g = lerp(rootInk.g, tipColour(strand.valueBias, 1), valueT);
            float bl = lerp(rootInk.b, tipColour(strand.valueBias, 2), valueT);

            if (vertCount + 2 > MAX_VERTS || indexCount + 6 > MAX_INDICES) {
                return;
            }
            pushVertex(spineX[i], spineY[i], strand.seed, spineArc[i], s, r, g, bl, alpha, nx, ny, halfWidth, -1f);
            pushVertex(spineX[i], spineY[i], strand.seed, spineArc[i], s, r, g, bl, alpha, nx, ny, halfWidth, 1f);
        }
        for (int i = 0; i + 1 < count; i++) {
            int p0 = base + i * 2;
            int p1 = p0 + 1;
            int p2 = p0 + 2;
            int p3 = p0 + 3;
            if (indexCount + 6 > MAX_INDICES || p3 >= vertCount) {
                return;
            }
            indices[indexCount++] = (short) p0;
            indices[indexCount++] = (short) p2;
            indices[indexCount++] = (short) p3;
            indices[indexCount++] = (short) p0;
            indices[indexCount++] = (short) p3;
            indices[indexCount++] = (short) p1;
        }
    }

    private void pushVertex(float x, float y, float seed, float arc, float s,
                            float r, float g, float b, float a,
                            float nx, float ny, float halfWidth, float across) {
        int o = vertCount * FLOATS;
        verts[o] = x;
        verts[o + 1] = y;
        verts[o + 2] = seed;
        verts[o + 3] = arc;
        verts[o + 4] = s;
        verts[o + 5] = r;
        verts[o + 6] = g;
        verts[o + 7] = b;
        verts[o + 8] = a;
        verts[o + 9] = nx;
        verts[o + 10] = ny;
        verts[o + 11] = halfWidth;
        verts[o + 12] = across;
        vertCount++;
    }

    /**
     * STYLE.md 4's nonlinear taper. Two factors: a mild power curve that holds
     * the root broad, times a quartic that only bites in the last third.
     * Measured, it gives 1.00 / 0.73 / 0.36 / 0.12 / 0.00 at s = 0 / 0.33 / 0.66
     * / 0.85 / 1 -- so the first half of the strand loses a quarter of its width
     * and the last sixth loses everything, which is what a brush leaving paper
     * does and what a linear ramp conspicuously does not.
     */
    private static float taper(float s) {
        float u = 1f - s;
        float q = s * s;
        return (float) Math.pow(u, 0.75) * (1f - q * q);
    }

    /**
     * The opacity ramp, deliberately shallower than the width taper: the mark
     * gets thin well before it gets faint, which is how an ink line behaves and
     * is why the tip reads as a hairline rather than as a fading smudge.
     */
    private static float opacity(float s) {
        float u = 1f - s;
        float q = s * s * s;
        return (float) Math.pow(u, 0.45) * (1f - q * q);
    }

    private float tipColour(float bias, int channel) {
        float lo = channel == 0 ? tipInkLow.r : channel == 1 ? tipInkLow.g : tipInkLow.b;
        float hi = channel == 0 ? tipInkHigh.r : channel == 1 ? tipInkHigh.g : tipInkHigh.b;
        return lerp(lo, hi, bias);
    }

    private static float lerp(float a, float b, float s) {
        return a + (b - a) * s;
    }

    /** Uniform Catmull-Rom, clamped at the ends by duplicating the endpoint control values. */
    private static float catmullRom(float p0, float p1, float p2, float p3, float s) {
        float s2 = s * s;
        float s3 = s2 * s;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * s
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * s2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * s3);
    }

    /** World units per pixel for an orthographic projection filling {@code widthPx} pixels. */
    public static float worldPerPixel(Matrix4 projTrans, int widthPx) {
        float m00 = Math.abs(projTrans.val[Matrix4.M00]);
        if (m00 < 1e-9f || widthPx <= 0) {
            return 1f / 320f;
        }
        return 2f / (m00 * widthPx);
    }
}
