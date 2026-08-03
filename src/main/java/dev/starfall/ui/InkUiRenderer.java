package dev.starfall.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

/**
 * The GL half of the interface: {@link Brush.Sink} on a mesh.
 *
 * <p>It draws through <b>the same shader as the impact marks</b> --
 * {@code shaders/ink_fx} -- and with the same premultiplied "over" blend, and that
 * is a claim rather than a convenience. STYLE.md 8 asks for an interface that is
 * brush marks on the paper the figures are painted on; sharing the material with
 * the bloom, the shed flecks and the vermillion seal is the strongest available
 * form of that, because it means a queue mark and a wound are made of literally
 * the same ink. There is no second material and no place to put one.
 *
 * <p>There is only one blend state here, unlike {@code InkFxRenderer}, and the
 * reason is a rule rather than an omission: nothing in the interface emits light.
 * STYLE.md 2.2 allows only the clash bloom and the blade specular to approach
 * white, and a UI that glows is STYLE.md 10's "neon glow on everything" arriving
 * by the back door.
 */
public final class InkUiRenderer implements Brush.Sink {

    private static final int MAX_VERTS = 24576;
    private static final int MAX_INDICES = 49152;
    private static final int FLOATS = 6;

    private final ShaderProgram shader;
    private final Mesh mesh;

    private final float[] verts = new float[MAX_VERTS * FLOATS];
    private final short[] indices = new short[MAX_INDICES];
    private int v;
    private int i;

    private final Matrix4 projTrans = new Matrix4();

    public InkUiRenderer() {
        FileHandle vert = Gdx.files.classpath("shaders/ink_fx.vert");
        FileHandle frag = Gdx.files.classpath("shaders/ink_fx.frag");
        this.shader = new ShaderProgram(vert.readString(), frag.readString());
        if (!shader.isCompiled()) {
            throw new IllegalStateException("ink_fx failed to compile:\n" + shader.getLog());
        }
        this.mesh = new Mesh(false, MAX_VERTS, MAX_INDICES,
                new VertexAttributes(
                        new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                        new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")));
    }

    public void begin(Matrix4 projTrans) {
        this.projTrans.set(projTrans);
        v = 0;
        i = 0;
    }

    @Override
    public int vertex(float x, float y, Color ink, float alpha) {
        if (v + 1 >= MAX_VERTS) {
            return v == 0 ? 0 : v - 1;
        }
        int o = v * FLOATS;
        verts[o] = x;
        verts[o + 1] = y;
        verts[o + 2] = ink.r;
        verts[o + 3] = ink.g;
        verts[o + 4] = ink.b;
        verts[o + 5] = alpha;
        return v++;
    }

    @Override
    public void triangle(int a, int b, int c) {
        if (i + 3 >= MAX_INDICES) {
            return;
        }
        indices[i++] = (short) a;
        indices[i++] = (short) b;
        indices[i++] = (short) c;
    }

    public void end() {
        if (i == 0) {
            return;
        }
        mesh.setVertices(verts, 0, v * FLOATS);
        mesh.setIndices(indices, 0, i);
        shader.bind();
        shader.setUniformMatrix("u_projTrans", projTrans);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, i);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        v = 0;
        i = 0;
    }

    public void dispose() {
        shader.dispose();
        mesh.dispose();
    }
}
