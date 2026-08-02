package dev.starfall.rig;

import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.render.InkMaterial;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;

/**
 * Static bind pose, full figure over the paper ground -- contract section G.
 * Side A owns the rig; side B owns InkSkinnedRenderer/PaperBackground (section
 * F), so this file only compiles once dev.starfall.render exists.
 */
public class RigBindposeScene implements Scene {

    private SamuraiRig rig;
    private InkSkinnedRenderer renderer;
    private PaperBackground paper;
    private InkMaterial clothMaterial;
    private InkMaterial bladeMaterial;
    private OrthographicCamera camera;
    private int width, height;
    private float t;

    @Override
    public String name() {
        return "rig-bindpose";
    }

    @Override
    public String description() {
        return "Samurai in bind pose, full figure, paper background";
    }

    @Override
    public void create(SceneContext ctx) {
        this.rig = SamuraiRig.build();
        this.renderer = new InkSkinnedRenderer();
        this.paper = new PaperBackground();
        this.clothMaterial = new InkMaterial();
        // Blade is its own SkinnedMesh (contract section E revision 2), drawn
        // in a second pass with the ready-made blade material so it reads as
        // steel -- near-white, hard-edged, emissive -- rather than dark cloth.
        this.bladeMaterial = new InkMaterial().asBlade();
        this.camera = new OrthographicCamera();
        resize(ctx.width, ctx.height);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        // World-space ortho tall enough for the whole figure: haori hem well
        // below the feet up to head clearance. See SamuraiRig javadoc for units.
        float aspect = width / (float) height;
        float halfH = 1.2f;
        camera.setToOrtho(false, halfH * aspect * 2f, halfH * 2f);
        camera.position.set(0f, 0.85f, 0f);
        camera.update();
    }

    @Override
    public void update(float dt) {
        t += dt;
        rig.applyBindPose();
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
        return 1f;
    }

    @Override
    public void dispose() {
        rig.mesh().dispose();
        rig.bladeMesh().dispose();
        renderer.dispose();
        paper.dispose();
    }
}
