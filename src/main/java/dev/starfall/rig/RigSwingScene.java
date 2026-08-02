package dev.starfall.rig;

import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.starfall.anim.Pose;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.render.InkMaterial;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;

/**
 * The graded scene: a 2.4s overhead sword swing, per contract section G and
 * STYLE.md 7.1. Side B owns the rendering (section F); this file only compiles
 * once dev.starfall.render exists.
 */
public class RigSwingScene implements Scene {

    private SamuraiRig rig;
    private SwingAnimation swing;
    private InkSkinnedRenderer renderer;
    private PaperBackground paper;
    private InkMaterial clothMaterial;
    private InkMaterial bladeMaterial;
    private OrthographicCamera camera;
    private int width, height;
    private float t;

    @Override
    public String name() {
        return "rig-swing";
    }

    @Override
    public String description() {
        return "2.4s overhead sword swing: anticipation / release / follow-through";
    }

    @Override
    public void create(SceneContext ctx) {
        this.rig = SamuraiRig.build();
        this.swing = new SwingAnimation();
        this.renderer = new InkSkinnedRenderer();
        this.paper = new PaperBackground();
        this.clothMaterial = new InkMaterial();
        // Blade is its own SkinnedMesh (contract section E revision 2), drawn
        // in a second pass with the ready-made blade material -- this is the
        // element the eye is meant to follow through the whole swing, so it
        // has to read as steel, not vanish into the same tone as the cloth.
        this.bladeMaterial = new InkMaterial().asBlade();
        this.camera = new OrthographicCamera();
        resize(ctx.width, ctx.height);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        float aspect = width / (float) height;
        // A touch wider than bindpose: the raised/extended blade sweeps outside
        // the resting silhouette. Revision 3 lifts the frame slightly so the
        // apex of the backswing stays inside the top edge (rig-fixes-3 item 6);
        // three of twelve frames had no visible weapon at all before.
        float halfH = 1.35f;
        camera.setToOrtho(false, halfH * aspect * 2f, halfH * 2f);
        camera.position.set(0f, 0.95f, 0f);
        camera.update();
    }

    @Override
    public void update(float dt) {
        t += dt;
        Pose pose = swing.sample(t);
        rig.applyPose(pose);
    }

    @Override
    public void render() {
        paper.render(camera.combined, t);
        renderer.begin(camera.combined, t);
        renderer.draw(rig.mesh(), rig.skeleton(), clothMaterial);
        renderer.draw(rig.bladeMesh(), rig.skeleton(), bladeMaterial);
        renderer.end();
        // Grass strokes over the hem (rig-fixes-3 item 5). Reference 2 draws
        // grass across the figure's legs, and that single trick does more for
        // grounding than any amount of haze -- but only if it is drawn after
        // the figure. Fog occlusion is not done here; it stays inside the ink
        // shader, per contract section F.
        paper.renderOverlay(camera.combined, t);
    }

    @Override
    public float duration() {
        return SwingAnimation.DURATION;
    }

    @Override
    public void dispose() {
        rig.mesh().dispose();
        rig.bladeMesh().dispose();
        renderer.dispose();
        paper.dispose();
    }
}
