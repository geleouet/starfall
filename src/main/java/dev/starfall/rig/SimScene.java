package dev.starfall.rig;

import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.render.HairRenderer;
import dev.starfall.render.InkMaterial;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;

/**
 * The graded System 3 scenes: {@code sim-sway} and {@code sim-extreme}, drawn
 * with the same ink material and paper as everything before them so they are
 * judged in context rather than as diagrams.
 *
 * <p>Structure follows {@link IkScene} exactly, with one addition and it is the
 * whole system: after the cloth resolve and the blade, the hair ribbons are
 * drawn over the top. That order is the reference's own -- in all three hair
 * references the wisps cross the shoulder and the collar and carry on into open
 * air, and hair tucked behind the figure would lose the half of its length that
 * does the work.
 */
public class SimScene implements Scene {

    private final SimScript.Kind kind;
    private final String name;

    private SimSceneDriver driver;
    private InkSkinnedRenderer renderer;
    private HairRenderer hair;
    private PaperBackground paper;
    private InkMaterial clothMaterial;
    private InkMaterial bladeMaterial;
    private OrthographicCamera camera;
    private int width = 960;

    public SimScene(SimScript.Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return driver != null ? driver.script().description() : name;
    }

    @Override
    public void create(SceneContext ctx) {
        SamuraiRig rig = SamuraiRig.build();
        this.driver = new SimSceneDriver(rig, kind);
        this.renderer = new InkSkinnedRenderer();
        this.hair = new HairRenderer();
        this.paper = new PaperBackground();
        this.clothMaterial = new InkMaterial();
        this.bladeMaterial = new InkMaterial().asBlade();
        this.camera = new OrthographicCamera();
        resize(ctx.width, ctx.height);
        driver.start();
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        float aspect = width / (float) height;
        // 1.38 puts the figure at about 330 px of a 540 px frame, which is the
        // height STYLE.md 11.0's matched-scale comparison has been run at since
        // System 1 -- deliberately, so this pass's part count is directly
        // comparable with the ones already on record.
        float halfH = 1.38f;
        camera.setToOrtho(false, halfH * aspect * 2f, halfH * 2f);
        // Biased back (-X) rather than centred on the figure: the hair trails
        // half a body-length behind the head and the knockback carries the whole
        // figure another 0.55 units the same way, and open paper behind the
        // figure is where the wisps do their work.
        float cx = kind == SimScript.Kind.EXTREME ? -0.22f : -0.06f;
        camera.position.set(cx, 1.02f, 0f);
        camera.update();
    }

    @Override
    public void update(float dt) {
        driver.advance(dt);
    }

    @Override
    public void render() {
        float t = driver.time();
        paper.render(camera.combined, t);
        renderer.begin(camera.combined, t);
        renderer.draw(driver.rig().mesh(), driver.skeleton(), clothMaterial);
        renderer.draw(driver.rig().bladeMesh(), driver.skeleton(), bladeMaterial);
        renderer.end();
        hair.begin(camera.combined, t, HairRenderer.worldPerPixel(camera.combined, width));
        hair.draw(driver.sim().hair());
        hair.end();
        paper.renderOverlay(camera.combined, t);
    }

    @Override
    public float duration() {
        return SimScript.durationOf(kind);
    }

    @Override
    public void dispose() {
        if (driver != null) {
            driver.rig().mesh().dispose();
            driver.rig().bladeMesh().dispose();
        }
        if (renderer != null) {
            renderer.dispose();
        }
        if (hair != null) {
            hair.dispose();
        }
        if (paper != null) {
            paper.dispose();
        }
    }
}
