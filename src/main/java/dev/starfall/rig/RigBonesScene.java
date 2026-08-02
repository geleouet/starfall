package dev.starfall.rig;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.anim.Pose;
import dev.starfall.anim.Skeleton;
import dev.starfall.anim.SkinnedMesh;
import dev.starfall.art.Palette;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;

/**
 * Debug overlay: bone segments and joints over a ghosted, CPU-skinned fill of
 * the rig mesh -- contract section G, technical verification only, not
 * aesthetic review. ShapeRenderer only, entirely side A's: no shader, no
 * dev.starfall.render dependency, so this is the one scene buildable and
 * capturable before side B lands.
 *
 * <p>Plays the same swing as rig-swing so a reviewer can check the arc traced
 * by the blade bone across the contact sheet. Skinning is redone here on the
 * CPU (matching Skeleton#fillSkinningMatrices exactly) purely to draw the
 * ghost fill -- the real ink render is GPU-side, owned by side B.
 */
public class RigBonesScene implements Scene {

    private static final int FLOATS = SkinnedMesh.FLOATS_PER_VERTEX;

    private ShapeRenderer shapes;
    private SamuraiRig rig;
    private SwingAnimation swing;
    private int width, height;
    private float t;

    private float[] vertexData;
    private short[] indexData;
    private float[] skinMats;
    private float[] posX, posY; // CPU-skinned positions, refreshed every update()

    @Override
    public String name() {
        return "rig-bones";
    }

    @Override
    public String description() {
        return "Debug: bone skeleton + ghosted mesh over the overhead swing";
    }

    @Override
    public void create(SceneContext ctx) {
        this.width = ctx.width;
        this.height = ctx.height;
        this.shapes = new ShapeRenderer();
        this.shapes.setAutoShapeType(true);
        this.rig = SamuraiRig.build();
        this.swing = new SwingAnimation();

        Mesh mesh = rig.mesh().mesh();
        vertexData = new float[rig.mesh().vertexCount() * FLOATS];
        mesh.getVertices(vertexData);
        indexData = new short[mesh.getNumIndices()];
        mesh.getIndices(indexData);
        skinMats = new float[rig.skeleton().boneCount() * 16];
        posX = new float[rig.mesh().vertexCount()];
        posY = new float[rig.mesh().vertexCount()];

        applyAndSkin(0f);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void update(float dt) {
        t += dt;
        applyAndSkin(t);
    }

    /** Poses the rig, then reproduces Skeleton#fillSkinningMatrices' linear blend on the CPU, per vertex, for the ghost fill. */
    private void applyAndSkin(float time) {
        Pose pose = swing.sample(time);
        rig.applyPose(pose); // resets to bind, applies deltas, updates world transforms
        rig.skeleton().fillSkinningMatrices(skinMats);

        int vcount = rig.mesh().vertexCount();
        for (int v = 0; v < vcount; v++) {
            int o = v * FLOATS;
            float px = vertexData[o];
            float py = vertexData[o + 1];
            float sx = 0f;
            float sy = 0f;
            for (int slot = 0; slot < 4; slot++) {
                int wo = o + 9 + slot * 2; // position(3) + uv(2) + color(4) = 9 floats before bone weights
                int boneIndex = (int) vertexData[wo];
                float weight = vertexData[wo + 1];
                if (weight <= 0f) {
                    continue;
                }
                int mo = boneIndex * 16;
                float a = skinMats[mo];
                float b = skinMats[mo + 1];
                float c = skinMats[mo + 4];
                float d = skinMats[mo + 5];
                float tx = skinMats[mo + 12];
                float ty = skinMats[mo + 13];
                sx += weight * (a * px + c * py + tx);
                sy += weight * (b * px + d * py + ty);
            }
            posX[v] = sx;
            posY[v] = sy;
        }
    }

    @Override
    public void render() {
        // World-space ortho: tall enough for the full figure, centred on the
        // torso. Matches the world the rig is authored in (see SamuraiRig).
        float aspect = width / (float) height;
        float halfH = 1.3f;
        float halfW = halfH * aspect;
        float cy = 0.9f;
        shapes.getProjectionMatrix().setToOrtho(-halfW, halfW, cy - halfH, cy + halfH, -1f, 1f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.PAPER_WARM);
        shapes.rect(-halfW, cy - halfH, halfW * 2f, halfH * 2f);

        // Ghosted mesh fill: flat, low-alpha, no material -- purely so bone
        // placement can be judged against the silhouette it drives.
        Color ghost = Palette.alpha(Palette.INK_INDIGO, 0.18f);
        shapes.setColor(ghost);
        for (int i = 0; i + 2 < indexData.length; i += 3) {
            int a = indexData[i];
            int b = indexData[i + 1];
            int c = indexData[i + 2];
            shapes.triangle(posX[a], posY[a], posX[b], posY[b], posX[c], posY[c]);
        }
        shapes.end();

        // Bone segments: a line to each bone's primary child, or a short stub
        // in its own facing direction for leaves (head, handR, blade tip, feet).
        Skeleton sk = rig.skeleton();
        boolean[] hasChild = new boolean[sk.boneCount()];
        for (int i = 0; i < sk.boneCount(); i++) {
            Bone bone = sk.bone(i);
            if (bone.parent != null) {
                hasChild[bone.parent.index] = true;
            }
        }
        Vector2 a = new Vector2();
        Vector2 b = new Vector2();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.OCHRE_PALE);
        for (int i = 0; i < sk.boneCount(); i++) {
            sk.worldPosition(i, a);
            if (hasChild[i]) {
                for (int j = 0; j < sk.boneCount(); j++) {
                    Bone child = sk.bone(j);
                    if (child.parent == sk.bone(i)) {
                        sk.worldPosition(j, b);
                        shapes.rectLine(a.x, a.y, b.x, b.y, 0.012f);
                    }
                }
            } else {
                float rot = sk.worldRotationDeg(i);
                float len = 0.10f;
                b.set(a.x + len * MathUtils.cosDeg(rot), a.y + len * MathUtils.sinDeg(rot));
                shapes.rectLine(a.x, a.y, b.x, b.y, 0.012f);
            }
        }
        shapes.end();

        // Joints on top, blade tip highlighted so the swept arc is easy to track across the sheet.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.VERMILLION);
        for (int i = 0; i < sk.boneCount(); i++) {
            sk.worldPosition(i, a);
            shapes.circle(a.x, a.y, 0.018f, 12);
        }
        shapes.setColor(Palette.BLADE);
        Bone blade = sk.bone("blade");
        sk.worldPosition(blade.index, a);
        float rot = sk.worldRotationDeg(blade.index);
        b.set(a.x + 0.55f * MathUtils.cosDeg(rot), a.y + 0.55f * MathUtils.sinDeg(rot));
        shapes.circle(b.x, b.y, 0.022f, 12);
        shapes.end();
    }

    @Override
    public float duration() {
        return SwingAnimation.DURATION;
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
        if (rig != null) {
            rig.mesh().dispose();
        }
    }
}
