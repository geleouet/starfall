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
        simulate(scene.warmup() + spec.start);
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

    /**
     * Seconds between captured frames.
     *
     * <p>An explicit {@code --step} wins, and is how motion timing gets graded at all: the
     * default of spreading frames across the whole scene samples far too coarsely to see
     * overlapping action, which STYLE.md §7.1 specifies in the 0.067-0.133s band. See
     * {@link CaptureSpec#step}.
     */
    private float frameInterval() {
        if (spec.step > 0f) {
            return spec.step;
        }
        return spec.frames <= 1 ? 0f : scene.duration() / (spec.frames - 1);
    }

    /**
     * Advances the scene by {@code seconds} using fixed substeps.
     *
     * <p>Delegates to {@link SceneClock} so the headless timing measurement and the debug API
     * integrate this scene identically — the guarantee STYLE.md §7.1 asks for.
     */
    private void simulate(float seconds) {
        SceneClock.advance(scene, seconds);
    }

    private void finish() {
        try {
            File sheet = new File(spec.outDir, "contact-sheet.png");
            // Report the window actually captured, not the scene's full duration — with an
            // explicit --step those differ, and a sheet labelled with the wrong timebase is
            // worse than one with none.
            float window = frameInterval() * Math.max(0, spec.frames - 1);
            ContactSheet.build(
                    written,
                    sheet,
                    spec.cols,
                    spec.label != null ? spec.label : scene.description(),
                    window,
                    spec.frames);
            writeManifest(window);
            System.out.println("CAPTURE_FRAMES=" + written.size());
            System.out.println("CAPTURE_SHEET=" + sheet.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Contact sheet assembly failed: " + e);
            e.printStackTrace();
        }
        Gdx.app.exit();
    }

    /**
     * Writes the exact command that reproduces this capture, next to its frames.
     *
     * <p>Added after an agent had to recover eight capture recipes by md5-matching frame 0
     * against a sweep of start times, because nothing in the repository recorded how any
     * capture had been made. A capture whose recipe is lost cannot be re-shot after a fix,
     * so it cannot be used as a before/after — which is most of what captures are for here.
     * Same principle as STYLE.md §11.3: a measurement you cannot reproduce is an anecdote.
     */
    private void writeManifest(float window) {
        StringBuilder cmd = new StringBuilder("./gw capture");
        cmd.append(" -Pscene=").append(spec.sceneName);
        cmd.append(" -Pout=").append(spec.outDir.getPath().replace('\\', '/'));
        cmd.append(" -Pframes=").append(spec.frames);
        cmd.append(" -Pcols=").append(spec.cols);
        if (spec.start > 0f) {
            cmd.append(" -Pstart=").append(spec.start);
        }
        if (spec.step > 0f) {
            cmd.append(" -Pstep=").append(spec.step);
        }
        cmd.append(" -Pw=").append(spec.width).append(" -Ph=").append(spec.height);

        StringBuilder out = new StringBuilder();
        out.append("# Reproduce this capture:\n").append(cmd).append('\n');
        out.append("\nscene=").append(spec.sceneName);
        out.append("\ndescription=").append(scene.description());
        out.append("\nframes=").append(spec.frames);
        out.append("\nstart=").append(scene.warmup() + spec.start);
        out.append("\nstep=").append(frameInterval());
        out.append("\nwindow=").append(window);
        out.append("\nsize=").append(spec.width).append('x').append(spec.height);
        out.append("\nsubstep=").append(SceneClock.SUBSTEP);
        out.append('\n');
        new FileHandle(new File(spec.outDir, "capture.txt")).writeString(out.toString(), false);
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
