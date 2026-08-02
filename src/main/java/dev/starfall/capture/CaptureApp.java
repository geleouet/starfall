package dev.starfall.capture;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link Scene} offscreen at a fixed timestep and writes one PNG per
 * captured frame, then assembles a contact sheet.
 *
 * <p>Simulation is decoupled from the host's frame rate entirely: each host frame
 * advances the scene by exactly one capture interval, subdivided into fixed
 * physics substeps. This makes captures bit-for-bit reproducible across machines
 * and load conditions, which is what makes iteration-to-iteration comparison
 * meaningful.
 */
public class CaptureApp extends ApplicationAdapter {

    /** Fixed physics substep. Small enough to keep Verlet constraints stable. */
    private static final float SUBSTEP = 1f / 240f;

    private final CaptureSpec spec;
    private final Scene scene;

    private FrameBuffer fbo;
    private int frameIndex;
    private final List<File> written = new ArrayList<>();

    public CaptureApp(CaptureSpec spec) {
        this.spec = spec;
        this.scene = SceneRegistry.create(spec.sceneName);
    }

    @Override
    public void create() {
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, spec.width, spec.height, true);
        scene.create(new SceneContext(spec.width, spec.height, true));
        scene.resize(spec.width, spec.height);
        simulate(scene.warmup());
    }

    @Override
    public void render() {
        if (frameIndex >= spec.frames) {
            finish();
            return;
        }

        // Advance to this frame's sample point before drawing it, except for the
        // very first frame which shows the state right after warmup.
        if (frameIndex > 0) {
            simulate(frameInterval());
        }

        fbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        scene.render();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, spec.width, spec.height);
        fbo.end();

        flipVertically(pixmap);

        File out = new File(spec.outDir, String.format("frame_%03d.png", frameIndex));
        PixmapIO.writePNG(new FileHandle(out), pixmap);
        pixmap.dispose();
        written.add(out);

        frameIndex++;
    }

    private float frameInterval() {
        return spec.frames <= 1 ? 0f : scene.duration() / (spec.frames - 1);
    }

    /** Advances the scene by {@code seconds} using fixed substeps. */
    private void simulate(float seconds) {
        int steps = Math.round(seconds / SUBSTEP);
        for (int i = 0; i < steps; i++) {
            scene.update(SUBSTEP);
        }
    }

    private void finish() {
        try {
            File sheet = new File(spec.outDir, "contact-sheet.png");
            ContactSheet.build(
                    written,
                    sheet,
                    spec.cols,
                    spec.label != null ? spec.label : scene.description(),
                    scene.duration(),
                    spec.frames);
            System.out.println("CAPTURE_FRAMES=" + written.size());
            System.out.println("CAPTURE_SHEET=" + sheet.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Contact sheet assembly failed: " + e);
            e.printStackTrace();
        }
        Gdx.app.exit();
    }

    private static void flipVertically(Pixmap pixmap) {
        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        for (int y = 0; y < h / 2; y++) {
            for (int x = 0; x < w; x++) {
                int top = pixmap.getPixel(x, y);
                int bottom = pixmap.getPixel(x, h - 1 - y);
                pixmap.drawPixel(x, y, bottom);
                pixmap.drawPixel(x, h - 1 - y, top);
            }
        }
    }

    @Override
    public void dispose() {
        scene.dispose();
        if (fbo != null) {
            fbo.dispose();
        }
    }
}
