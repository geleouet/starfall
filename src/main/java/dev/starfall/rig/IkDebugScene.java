package dev.starfall.rig;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;
import dev.starfall.anim.SkinnedMesh;
import dev.starfall.art.Palette;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.ik.IkChain;

/**
 * Debug overlay for the IK scenes: the solved chains, their targets, their poles
 * and their end effectors, over a ghosted CPU-skinned fill of the same figure --
 * contract section G, technical verification, not aesthetic review.
 *
 * <p>The reason this exists rather than being read off the ink capture: the ink
 * capture cannot show the target. Half the questions a reviewer has about an IK
 * scene ("is the hand missing the target or is the target somewhere silly?",
 * "which side is the elbow on?", "is the limb saturated at extension or has it
 * given up?") are questions about things that are not drawn in the graded frame.
 * Here they are all drawn, and drawn from {@link IkSceneDriver}, so this is the
 * same run and not a re-enactment of it.
 *
 * <p>{@link ShapeRenderer} only, matching {@link RigBonesScene}: no shader and no
 * dependency on {@code dev.starfall.render}.
 */
public class IkDebugScene implements Scene {

    private static final int FLOATS = SkinnedMesh.FLOATS_PER_VERTEX;

    private final IkTargetScript.Kind kind;
    private final String name;

    private ShapeRenderer shapes;
    private IkSceneDriver driver;
    private int width, height;

    private float[] vertexData;
    private short[] indexData;
    private float[] skinMats;
    private float[] posX, posY;

    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();

    public IkDebugScene(IkTargetScript.Kind kind, String name) {
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
        this.driver = new IkSceneDriver(rig, kind);

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
        int vcount = posX.length;
        for (int v = 0; v < vcount; v++) {
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
        // Wider and taller than the ink framing on purpose: ik-extreme drives its
        // target to nearly three times arm reach, and a debug view that cropped
        // the target would be answering the one question it exists to answer with
        // "off-screen somewhere".
        float halfH = 1.75f;
        float halfW = halfH * aspect;
        float cx = 0.35f;
        float cy = 1.05f;
        shapes.getProjectionMatrix().setToOrtho(cx - halfW, cx + halfW, cy - halfH, cy + halfH, -1f, 1f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.PAPER_WARM);
        shapes.rect(cx - halfW, cy - halfH, halfW * 2f, halfH * 2f);

        // The figure, ghosted flat, so chain geometry can be judged against the
        // silhouette it is driving without competing with it.
        shapes.setColor(Palette.alpha(Palette.INK_INDIGO, 0.16f));
        for (int i = 0; i + 2 < indexData.length; i += 3) {
            int p = indexData[i];
            int q = indexData[i + 1];
            int r = indexData[i + 2];
            shapes.triangle(posX[p], posY[p], posX[q], posY[q], posX[r], posY[r]);
        }
        shapes.end();

        RigIk ik = driver.ik();
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Chains, thickest first so the sword arm reads on top of the trunk.
        drawChain(ik.spine(), Palette.INK_SLATE, 0.013f);
        drawChain(ik.legL(), Palette.INK_SLATE, 0.013f);
        drawChain(ik.legR(), Palette.INK_SLATE, 0.013f);
        drawChain(ik.swordArm(), Palette.INK_BLACK, 0.018f);

        // Poles. The legs always carry one and the arm carries one only in
        // ik-gesture (see RigIk#armPoleFromChest); everywhere else the arm's pole
        // is the previous frame's own elbow, so it is drawn as a hollow tick on
        // the elbow itself rather than as a point out in space, because that is
        // literally where it is.
        shapes.setColor(Palette.MOTE_CYAN);
        ik.legPoleL(a);
        cross(a.x, a.y, 0.030f);
        ik.legPoleR(a);
        cross(a.x, a.y, 0.030f);
        driver.skeleton().worldPosition(ik.swordArm().bone(1).index, a);
        cross(a.x, a.y, 0.022f);

        // Targets: requested position, plus the line from the chain root, which is
        // what makes an out-of-reach frame legible -- the hand stops and the line
        // keeps going.
        IkTargetScript s = driver.script();
        shapes.setColor(Palette.VERMILLION);
        driver.skeleton().worldPosition(ik.swordArm().bone(0).index, a);
        shapes.rectLine(a.x, a.y, s.armX, s.armY, 0.004f);
        ring(s.armX, s.armY, 0.036f);

        shapes.setColor(Palette.OCHRE);
        ik.fromHips(s.spineLocalX, s.spineLocalY, b);
        ring(b.x, b.y, 0.024f);
        shapes.setColor(Palette.OCHRE_PALE);
        ring(s.footLX, s.footLY, 0.022f);
        ring(s.footRX, s.footRY, 0.022f);

        // End effectors, filled, so target-versus-arrival is one glance: a ring
        // with a dot in it has arrived, a ring on its own has not.
        shapes.setColor(Palette.BLADE);
        ik.swordArm().endEffector(a);
        shapes.circle(a.x, a.y, 0.017f, 14);
        ik.spine().endEffector(a);
        shapes.circle(a.x, a.y, 0.012f, 12);
        ik.legL().endEffector(a);
        shapes.circle(a.x, a.y, 0.012f, 12);
        ik.legR().endEffector(a);
        shapes.circle(a.x, a.y, 0.012f, 12);

        // Weight readout: a bar along the bottom, filled to the arm chain's
        // current blend. ik-parry is entirely about this number and there is no
        // text in a ShapeRenderer.
        float barW = 1.6f;
        float barX = cx - barW * 0.5f;
        float barY = cy - halfH + 0.10f;
        shapes.setColor(Palette.alpha(Palette.INK_SLATE, 0.35f));
        shapes.rect(barX, barY, barW, 0.026f);
        shapes.setColor(Palette.VERMILLION);
        shapes.rect(barX, barY, barW * ik.swordArm().weight(), 0.026f);
        shapes.setColor(Palette.OCHRE);
        shapes.rect(barX, barY - 0.040f, barW * ik.spine().weight(), 0.020f);
        shapes.end();
    }

    /** Bones of one chain, plus a joint dot at each, plus the run out to the effector. */
    private void drawChain(IkChain chain, Color color, float thickness) {
        Skeleton sk = driver.skeleton();
        shapes.setColor(color);
        for (int i = 0; i < chain.boneCount(); i++) {
            sk.worldPosition(chain.bone(i).index, a);
            if (i + 1 < chain.boneCount()) {
                sk.worldPosition(chain.bone(i + 1).index, b);
            } else {
                chain.endEffector(b);
            }
            shapes.rectLine(a.x, a.y, b.x, b.y, thickness);
        }
        for (int i = 0; i < chain.boneCount(); i++) {
            sk.worldPosition(chain.bone(i).index, a);
            shapes.circle(a.x, a.y, thickness * 1.35f, 12);
        }
    }

    /** A hollow ring, drawn as a fan of short chords so it works in Filled mode. */
    private void ring(float x, float y, float r) {
        int n = 24;
        float inner = r * 0.72f;
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

    private void cross(float x, float y, float r) {
        shapes.rectLine(x - r, y, x + r, y, 0.006f);
        shapes.rectLine(x, y - r, x, y + r, 0.006f);
    }

    @Override
    public float duration() {
        return IkTargetScript.durationOf(kind);
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
