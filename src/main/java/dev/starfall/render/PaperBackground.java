package dev.starfall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.starfall.art.Palette;

/**
 * The warm paper ground of STYLE.md section 2, with the drifting fog bands of
 * section 6 -- contract section F.
 *
 * <p>Everything is procedural in {@code paper.frag}; the work here is only to
 * hand that shader a quad that exactly covers the visible world rect, plus the
 * rect itself. The rect comes from inverting the camera matrix rather than from
 * the caller passing viewport numbers, because the contract fixes render() to
 * take nothing but the matrix and the clock -- and because it means the paper
 * grain stays locked to world space when the camera pushes in (STYLE.md 9), so
 * the sheet does not slide under the figure during a camera move.
 */
public final class PaperBackground {

    private final ShaderProgram shader;
    private final Mesh quad;
    private final float[] vertices = new float[8];
    private final Matrix4 inverse = new Matrix4();
    private final Vector3 corner = new Vector3();

    public PaperBackground() {
        this.shader = InkSkinnedRenderer.Shaders.load("paper");
        this.quad = new Mesh(false, 4, 6,
                new VertexAttributes(new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")));
        this.quad.setIndices(new short[] {0, 1, 2, 2, 3, 0});
    }

    public void render(Matrix4 projTrans, float timeSeconds) {
        inverse.set(projTrans).inv();
        float minX = unprojectX(-1f, -1f);
        float minY = unprojectY(-1f, -1f);
        float maxX = unprojectX(1f, 1f);
        float maxY = unprojectY(1f, 1f);

        vertices[0] = minX; vertices[1] = minY;
        vertices[2] = maxX; vertices[3] = minY;
        vertices[4] = maxX; vertices[5] = maxY;
        vertices[6] = minX; vertices[7] = maxY;
        quad.setVertices(vertices);

        shader.bind();
        shader.setUniformMatrix("u_projTrans", projTrans);
        shader.setUniformf("u_time", timeSeconds);
        shader.setUniformf("u_frameMin", minX, minY);
        shader.setUniformf("u_frameSize", maxX - minX, maxY - minY);
        shader.setUniformf("u_paperWarm", Palette.PAPER_WARM.r, Palette.PAPER_WARM.g, Palette.PAPER_WARM.b);
        shader.setUniformf("u_paperCool", Palette.PAPER_COOL.r, Palette.PAPER_COOL.g, Palette.PAPER_COOL.b);
        shader.setUniformf("u_ochre", Palette.OCHRE.r, Palette.OCHRE.g, Palette.OCHRE.b);
        shader.setUniformf("u_inkIndigo", Palette.INK_INDIGO.r, Palette.INK_INDIGO.g, Palette.INK_INDIGO.b);
        shader.setUniformf("u_fogColor", Palette.FOG.r, Palette.FOG.g, Palette.FOG.b);
        shader.setUniformf("u_moteCyan", Palette.MOTE_CYAN.r, Palette.MOTE_CYAN.g, Palette.MOTE_CYAN.b);
        shader.setUniformf("u_moteMagenta", Palette.MOTE_MAGENTA.r, Palette.MOTE_MAGENTA.g, Palette.MOTE_MAGENTA.b);
        shader.setUniformf("u_ember", Palette.EMBER.r, Palette.EMBER.g, Palette.EMBER.b);
        Atmosphere.setFogUniforms(shader);

        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        quad.render(shader, GL20.GL_TRIANGLES);
    }

    public void dispose() {
        shader.dispose();
        quad.dispose();
    }

    private float unprojectX(float clipX, float clipY) {
        corner.set(clipX, clipY, 0f).prj(inverse);
        return corner.x;
    }

    private float unprojectY(float clipX, float clipY) {
        corner.set(clipX, clipY, 0f).prj(inverse);
        return corner.y;
    }
}
