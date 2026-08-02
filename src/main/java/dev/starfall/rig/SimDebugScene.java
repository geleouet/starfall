package dev.starfall.rig;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.SkinnedMesh;
import dev.starfall.art.Palette;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.sim.ClothSim;
import dev.starfall.sim.HairSim;

/**
 * Debug overlay for the System 3 scenes: every particle, every constraint, the
 * cloth bone chains and the wind vector, over a ghosted CPU-skinned fill of the
 * same figure.
 *
 * <p>It exists for the reason System 2's debt (E5) records: the graded capture
 * cannot show a particle, and most of what goes wrong in a simulation is a
 * question about one. Where is the tip actually, versus where the ribbon draws
 * it? Is a strand stretched or is that the taper? Did the swing limit engage?
 * None of those is visible in ink.
 *
 * <p>E5 also records that the easy scene got the instrumentation and the hard
 * one did not, so both {@code sim-sway-debug} and {@code sim-extreme-debug}
 * exist from the first pass.
 *
 * <p>{@link ShapeRenderer} only, matching {@link IkDebugScene}: no shader and no
 * dependency on {@code dev.starfall.render}.
 */
public class SimDebugScene implements Scene {

    private static final int FLOATS = SkinnedMesh.FLOATS_PER_VERTEX;

    private final SimScript.Kind kind;
    private final String name;

    private ShapeRenderer shapes;
    private SimSceneDriver driver;
    private int width, height;

    private float[] vertexData;
    private short[] indexData;
    private float[] skinMats;
    private float[] posX, posY;

    private final Vector2 a = new Vector2();

    public SimDebugScene(SimScript.Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "Debug: " + (driver != null ? driver.script().description() : name);
    }

    @Override
    public void create(SceneContext ctx) {
        this.width = ctx.width;
        this.height = ctx.height;
        this.shapes = new ShapeRenderer();
        this.shapes.setAutoShapeType(true);

        SamuraiRig rig = SamuraiRig.build();
        this.driver = new SimSceneDriver(rig, kind);

        Mesh mesh = rig.mesh().mesh();
        vertexData = new float[rig.mesh().vertexCount() * FLOATS];
        mesh.getVertices(vertexData);
        indexData = new short[mesh.getNumIndices()];
        mesh.getIndices(indexData);
        skinMats = new float[rig.skeleton().boneCount() * 16];
        posX = new float[rig.mesh().vertexCount()];
        posY = new float[rig.mesh().vertexCount()];

        driver.start();
        skin();
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void update(float dt) {
        driver.advance(dt);
        skin();
    }

    /** Reproduces Skeleton#fillSkinningMatrices' linear blend on the CPU, for the ghost fill only. */
    private void skin() {
        driver.skeleton().fillSkinningMatrices(skinMats);
        for (int v = 0; v < posX.length; v++) {
            int o = v * FLOATS;
            float px = vertexData[o];
            float py = vertexData[o + 1];
            float sx = 0f;
            float sy = 0f;
            for (int slot = 0; slot < 4; slot++) {
                int wo = o + 9 + slot * 2; // position(3) + uv(2) + color(4)
                int boneIndex = (int) vertexData[wo];
                float weight = vertexData[wo + 1];
                if (weight <= 0f) {
                    continue;
                }
                int mo = boneIndex * 16;
                sx += weight * (skinMats[mo] * px + skinMats[mo + 4] * py + skinMats[mo + 12]);
                sy += weight * (skinMats[mo + 1] * px + skinMats[mo + 5] * py + skinMats[mo + 13]);
            }
            posX[v] = sx;
            posY[v] = sy;
        }
    }

    @Override
    public void render() {
        float aspect = width / (float) height;
        float halfH = 1.55f;
        float halfW = halfH * aspect;
        float cx = kind == SimScript.Kind.EXTREME ? -0.30f : -0.10f;
        float cy = 1.05f;
        shapes.getProjectionMatrix().setToOrtho(cx - halfW, cx + halfW, cy - halfH, cy + halfH, -1f, 1f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.PAPER_WARM);
        shapes.rect(cx - halfW, cy - halfH, halfW * 2f, halfH * 2f);

        shapes.setColor(Palette.alpha(Palette.INK_INDIGO, 0.14f));
        for (int i = 0; i + 2 < indexData.length; i += 3) {
            int p = indexData[i];
            int q = indexData[i + 1];
            int r = indexData[i + 2];
            shapes.triangle(posX[p], posY[p], posX[q], posY[q], posX[r], posY[r]);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Cloth first, so hair reads on top of it -- the same painter's order the
        // graded scene uses.
        ClothSim cloth = driver.sim().cloth();
        for (int i = 0; i < cloth.chainCount(); i++) {
            ClothSim.Chain c = cloth.chain(i);
            shapes.setColor(Palette.OCHRE);
            for (int p = 0; p + 1 < c.particleCount(); p++) {
                shapes.rectLine(c.x(p), c.y(p), c.x(p + 1), c.y(p + 1), 0.009f);
            }
            shapes.setColor(Palette.VERMILLION);
            for (int p = 0; p < c.particleCount(); p++) {
                shapes.circle(c.x(p), c.y(p), 0.013f, 12);
            }
        }

        // Hair. Constraints as thin segments -- the raw polyline, deliberately,
        // because the graded scene draws a Catmull-Rom through these and the
        // difference between the two is exactly what STYLE.md 4's "no visible
        // polyline kink" is about.
        HairSim hairSim = driver.sim().hair();
        for (int i = 0; i < hairSim.strandCount(); i++) {
            HairSim.Strand st = hairSim.strand(i);
            shapes.setColor(st.escapee ? Palette.MOTE_MAGENTA : Palette.INK_SLATE);
            for (int p = 0; p + 1 < st.particleCount(); p++) {
                shapes.rectLine(st.x(p), st.y(p), st.x(p + 1), st.y(p + 1), 0.0035f);
            }
            shapes.setColor(st.escapee ? Palette.MOTE_MAGENTA : Palette.INK_BLACK);
            for (int p = 0; p < st.particleCount(); p++) {
                shapes.circle(st.x(p), st.y(p), 0.0058f, 8);
            }
            // The tip, marked, because "where is the tip" is the question every
            // lag measurement asks.
            shapes.setColor(Palette.MOTE_CYAN);
            int last = st.particleCount() - 1;
            shapes.circle(st.x(last), st.y(last), 0.011f, 10);
        }

        // Every collider, read off the sim rather than re-derived here -- the
        // skull and, since pass 2, the torso. Re-deriving them would let the
        // overlay disagree with the solver, which is the one thing an
        // instrument may never do; the pass-1 overlay drew the skull at 0.92 of
        // its radius while the solver used 0.70, so the picture showed strands
        // "sinking into" a circle they were never tested against.
        shapes.setColor(Palette.alpha(Palette.INK_SLATE, 0.4f));
        for (int i = 0; i < hairSim.colliderCount(); i++) {
            ring(hairSim.colliderX(i), hairSim.colliderY(i), hairSim.colliderRadius(i));
        }
        driver.skeleton().worldPosition(driver.skeleton().bone("head").index, a);

        // Wind, drawn from the head as an arrow-less bar whose length is the
        // acceleration: it is the only input to the sim that is not visible in
        // the geometry.
        shapes.setColor(Palette.EMBER);
        float wx = driver.sim().windX();
        float wy = driver.sim().windY();
        shapes.rectLine(a.x, a.y + 0.42f, a.x + wx * 0.12f, a.y + 0.42f + wy * 0.12f, 0.012f);

        shapes.end();
    }

    private void ring(float x, float y, float r) {
        int n = 28;
        float inner = r * 0.94f;
        for (int i = 0; i < n; i++) {
            double t0 = 2 * Math.PI * i / n;
            double t1 = 2 * Math.PI * (i + 1) / n;
            float c0 = (float) Math.cos(t0);
            float s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1);
            float s1 = (float) Math.sin(t1);
            shapes.triangle(x + inner * c0, y + inner * s0, x + r * c0, y + r * s0, x + r * c1, y + r * s1);
            shapes.triangle(x + inner * c0, y + inner * s0, x + r * c1, y + r * s1, x + inner * c1, y + inner * s1);
        }
    }

    @Override
    public float duration() {
        return SimScript.durationOf(kind);
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
        if (driver != null) {
            driver.rig().mesh().dispose();
            driver.rig().bladeMesh().dispose();
        }
    }
}
