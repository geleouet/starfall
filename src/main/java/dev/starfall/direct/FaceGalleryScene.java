package dev.starfall.direct;

import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.render.HairRenderer;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;

import java.util.ArrayList;
import java.util.List;

/**
 * Six generated Charted Shadows in one frame, heads at push-in scale — the
 * exhibit the pass-1 review asked for by name (its ranked item 8): STYLE.md
 * 4b.6's variety claim existed only in parameter space (a 400-seed spread of
 * numbers), and "seeded variety, genuine not jitter" is a claim about pictures.
 *
 * <p>The six seeds are the first six the game itself would deal: the same
 * {@code 0xFACE0000L + body} keying {@link Figure#pale(int)} uses, bodies 1-6,
 * so the gallery shows the faces a run of encounters actually produces rather
 * than six hand-picked flattering ones. All six stand in READY facing +X on the
 * dusk stage, drawn through the same renderer and materials as the duel — a
 * face gallery on cream paper would have re-imported the portrait-ground
 * defect this pass exists to remove.
 */
public final class FaceGalleryScene implements Scene {

    /** Heads at ~52 px: above DuelScene's 44 px full-detail knee. */
    private static final float WORLD_WIDTH = 5.4f;
    private static final float EYE = 0.44f;

    private final String name;
    private final List<Figure> figures = new ArrayList<>();
    private InkSkinnedRenderer renderer;
    private HairRenderer hair;
    private PaperBackground paper;
    private OrthographicCamera camera;
    private int width = 960;
    private int height = 720;

    public FaceGalleryScene(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "six seeded faces at push-in scale: 4b.6's variety, on screen instead of in a table";
    }

    @Override
    public void create(SceneContext ctx) {
        renderer = new InkSkinnedRenderer();
        hair = new HairRenderer();
        paper = new PaperBackground().dusk(true);
        renderer.backdrop(PaperBackground.backdropStops(true));
        camera = new OrthographicCamera();
        for (int body = 1; body <= 6; body++) {
            Figure f = Figure.pale(body);
            // Facing -X, like the duel's foe: the stage breeze blows +X, so a
            // +X-facing gallery had every face buried under its own streaming
            // hair. Faced into the wind, the hair clears the profile.
            f.standAt(-2.35 + (body - 1) * 0.94, -1f);
            figures.add(f);
        }
        resize(ctx.width, ctx.height);
        for (Figure f : figures) {
            f.sim().wind(Director.BREEZE_X, Director.BREEZE_Y);
            f.snap(0f);
            // The same settle DuelScene gives a garment before its first frame.
            for (int i = 0; i < Math.round(0.8f / dev.starfall.capture.SceneClock.SUBSTEP); i++) {
                f.simulate(dev.starfall.capture.SceneClock.SUBSTEP, 0f);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        float h = WORLD_WIDTH * height / (float) width;
        camera.setToOrtho(false, WORLD_WIDTH, h);
        // Heads (world y ~1.67) in the upper third, feet allowed to leave frame:
        // this is a face gallery, not a figure gallery.
        camera.position.set(0f, 1.05f, 0f);
        camera.update();
    }

    @Override
    public void update(float dt) {
        for (Figure f : figures) {
            f.writePose();
            f.solve(dt);
            f.simulate(dt, 0f);
        }
    }

    @Override
    public void render() {
        paper.render(camera.combined, 0f);
        renderer.begin(camera.combined, 0f);
        for (Figure f : figures) {
            renderer.draw(f.rig().mesh(), f.skeleton(), f.clothMaterial());
            renderer.draw(f.rig().faceMesh(), f.skeleton(), f.skinMaterial());
            renderer.draw(f.rig().faceInkMesh(), f.skeleton(), f.faceInkMaterial());
            renderer.draw(f.rig().bladeMesh(), f.skeleton(), f.bladeMaterial());
        }
        renderer.end();
        hair.begin(camera.combined, 0f, HairRenderer.worldPerPixel(camera.combined, width));
        for (Figure f : figures) {
            hair.draw(f.sim().hair());
        }
        hair.end();
        paper.renderOverlay(camera.combined, 0f);
        Opaque.seal();
    }

    @Override
    public float duration() {
        return 1f;
    }

    @Override
    public void dispose() {
        for (Figure f : figures) {
            f.rig().mesh().dispose();
            f.rig().bladeMesh().dispose();
            if (f.rig().faceMesh() != null) {
                f.rig().faceMesh().dispose();
            }
            if (f.rig().faceInkMesh() != null) {
                f.rig().faceInkMesh().dispose();
            }
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
