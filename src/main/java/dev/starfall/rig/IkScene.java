package dev.starfall.rig;

import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.render.InkMaterial;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;

/**
 * The graded IK scenes: {@code ik-reach}, {@code ik-extreme} and
 * {@code ik-parry}, drawn with the same ink material and paper as
 * {@code rig-swing} so they are judged in context rather than as diagrams. The
 * technical read is {@link IkDebugScene}'s job; this is the one the gate in
 * STYLE.md 7.2 applies to.
 *
 * <p>Structure follows {@link RigSwingScene} exactly -- same renderer, same two
 * material passes, same grass overlay after the figure. The only difference is
 * what writes the pose, and that is {@link IkSceneDriver}.
 */
public class IkScene implements Scene {

    private final IkTargetScript.Kind kind;
    private final String name;

    private IkSceneDriver driver;
    private InkSkinnedRenderer renderer;
    private PaperBackground paper;
    private InkMaterial clothMaterial;
    private InkMaterial bladeMaterial;
    private OrthographicCamera camera;

    public IkScene(IkTargetScript.Kind kind, String name) {
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
        this.driver = new IkSceneDriver(rig, kind);
        this.renderer = new InkSkinnedRenderer();
        this.paper = new PaperBackground();
        this.clothMaterial = new InkMaterial();
        this.bladeMaterial = new InkMaterial().asBlade();
        this.camera = new OrthographicCamera();
        resize(ctx.width, ctx.height);
        driver.start();
    }

    @Override
    public void resize(int width, int height) {
        float aspect = width / (float) height;
        // Taller than rig-swing's frame. The reach scene deliberately drives the
        // hand to 1.25x arm reach above and behind the shoulder, and the blade
        // extends most of another arm's length past the hand, so the tip clears
        // 2.4 -- three frames with no visible weapon is the fault rig-fixes-3
        // item 6 was written about, and it is easier to avoid here than to
        // rediscover.
        float halfH = 1.5f;
        camera.setToOrtho(false, halfH * aspect * 2f, halfH * 2f);
        // Lifted to 1.18 rather than rig-swing's 0.95: the blade sits 45 degrees
        // off the forearm, so a hand at full reach straight up puts the kissaki at
        // y = 2.7, and the parry's raised guard is the pose that goes closest to
        // it. The hem bottoms out at -0.16 and the frame's lower edge is at
        // -0.32, so nothing is lost at that end.
        camera.position.set(0.10f, 1.18f, 0f);
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
        paper.renderOverlay(camera.combined, t);
    }

    @Override
    public float duration() {
        return IkTargetScript.durationOf(kind);
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
        if (paper != null) {
            paper.dispose();
        }
    }
}
