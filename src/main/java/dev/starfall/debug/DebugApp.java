package dev.starfall.debug;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import dev.starfall.analysis.Centroid;
import dev.starfall.analysis.Coverage;
import dev.starfall.analysis.Figure;
import dev.starfall.analysis.Frame;
import dev.starfall.analysis.Json;
import dev.starfall.analysis.Paper;
import dev.starfall.analysis.Rect;
import dev.starfall.analysis.RegionSet;
import dev.starfall.capture.ContactSheet;
import dev.starfall.capture.Framebuffers;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneClock;
import dev.starfall.capture.SceneContext;
import dev.starfall.capture.SceneEvents;
import dev.starfall.capture.SceneProbe;
import dev.starfall.capture.SceneRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * The GL side of the debug API: owns the context, the current scene and the simulated clock,
 * and executes every command on the render thread.
 *
 * <p>Simulated time only advances when something asks it to. There is no wall clock in here,
 * which is what makes the API reproducible: two identical command sequences produce identical
 * pixels, so a capture taken through the API and a capture taken by {@code ./gw capture} with
 * the same start and step are the same bytes. That property is the reason this is worth
 * having at all — an interactive harness that drifted from the batch harness would produce
 * evidence that could not be re-derived.
 *
 * <p>All state mutation happens in {@link #render()} draining the command queue. HTTP handler
 * threads never touch GL or the scene.
 */
public final class DebugApp extends ApplicationAdapter {

    private final int width;
    private final int height;
    private final String initialScene;

    private final ConcurrentLinkedQueue<FutureTask<String>> commands = new ConcurrentLinkedQueue<>();

    private FrameBuffer fbo;
    private Scene scene;
    private String sceneName;
    /** Simulated seconds since the scene's warmup completed. */
    private float time;
    private Rect camera;
    private int cameraOutW;
    private int cameraOutH;
    private RegionSet regions = RegionSet.samurai();
    private volatile boolean ready;

    public DebugApp(String initialScene, int width, int height) {
        this.initialScene = initialScene;
        this.width = width;
        this.height = height;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void create() {
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);
        loadScene(initialScene);
        ready = true;
    }

    @Override
    public void render() {
        FutureTask<String> task;
        while ((task = commands.poll()) != null) {
            task.run();
        }
        // Draw the current state to the visible window so a human watching sees the same
        // frame the API is measuring. No simulation happens here.
        Gdx.gl.glClearColor(0.07f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (scene != null) {
            scene.render();
        }
    }

    @Override
    public void dispose() {
        if (scene != null) {
            scene.dispose();
        }
        if (fbo != null) {
            fbo.dispose();
        }
    }

    public boolean isReady() {
        return ready;
    }

    /** Submits a command to the render thread and waits for its JSON result. */
    public String submit(java.util.concurrent.Callable<String> body) throws Exception {
        FutureTask<String> task = new FutureTask<>(body);
        commands.add(task);
        return task.get(60, java.util.concurrent.TimeUnit.SECONDS);
    }

    // ------------------------------------------------------------------ commands

    private void loadScene(String name) {
        if (scene != null) {
            scene.dispose();
        }
        scene = SceneRegistry.create(name);
        sceneName = name;
        scene.create(new SceneContext(width, height, true));
        scene.resize(width, height);
        SceneClock.advance(scene, scene.warmup());
        time = 0f;
    }

    public String cmdScenes() {
        Json.Writer w = new Json.Writer().beginObject().name("scenes").beginArray();
        for (String n : SceneRegistry.names()) {
            w.value(n);
        }
        return w.endArray().prop("current", sceneName).endObject().toString();
    }

    public String cmdLoad(String name) {
        loadScene(name);
        return cmdState();
    }

    /**
     * Seeks to an absolute simulated time.
     *
     * <p>Scenes are forward-only, so seeking backwards rebuilds the scene and replays from
     * warmup. That is slower but it is the only way to be sure the state at {@code t} is the
     * state a capture starting at {@code t} would see — a seek that merely stopped updating
     * would leave a Verlet solver in a state no capture can reproduce.
     */
    public String cmdSeek(float t) {
        if (t < time) {
            loadScene(sceneName);
        }
        SceneClock.advance(scene, t - time);
        time = t;
        return cmdState();
    }

    public String cmdStep(int frames, float dt) {
        float step = dt > 0 ? dt : 1f / 60f;
        for (int i = 0; i < Math.max(0, frames); i++) {
            SceneClock.advance(scene, step);
            time += step;
        }
        return cmdState();
    }

    public String cmdCamera(Rect rect, int outW, int outH) {
        this.camera = rect;
        this.cameraOutW = outW;
        this.cameraOutH = outH;
        return cmdState();
    }

    public String cmdEvent(String name, Map<String, String> args) {
        Json.Writer w = new Json.Writer().beginObject().prop("event", name);
        if (!(scene instanceof SceneEvents e)) {
            return w.prop("supported", false)
                    .prop("error", "scene '" + sceneName + "' does not implement SceneEvents; "
                            + "it is time-scripted, so drive it with /seek and /step instead")
                    .endObject().toString();
        }
        boolean fired = e.fire(name, args == null ? Map.of() : args);
        w.prop("supported", true).prop("fired", fired);
        if (!fired) {
            w.name("known").beginArray();
            for (String k : e.events()) {
                w.value(k);
            }
            w.endArray();
        }
        return w.prop("time", time).endObject().toString();
    }

    public String cmdState() {
        Json.Writer w = new Json.Writer().beginObject();
        w.prop("scene", sceneName);
        w.prop("description", scene == null ? null : scene.description());
        w.prop("time", time);
        w.prop("warmup", scene == null ? 0 : scene.warmup());
        w.prop("duration", scene == null ? 0 : scene.duration());
        w.prop("width", width);
        w.prop("height", height);
        w.prop("substep", SceneClock.SUBSTEP);
        if (camera == null) {
            w.name("camera").value((String) null);
        } else {
            w.prop("camera", camera);
            w.prop("cameraOut", new Rect(0, 0, outWidth(), outHeight()));
        }
        w.name("events").beginArray();
        if (scene instanceof SceneEvents e) {
            for (String k : e.events()) {
                w.value(k);
            }
        }
        w.endArray();
        if (scene instanceof SceneProbe p) {
            w.name("probe").beginObject();
            for (Map.Entry<String, float[]> e : p.probe().entrySet()) {
                w.name(e.getKey()).beginArray();
                for (float v : e.getValue()) {
                    w.value(v);
                }
                w.endArray();
            }
            w.endObject();
        }
        w.name("regions").beginArray();
        for (String n : regions.names()) {
            w.value(n);
        }
        w.endArray();
        return w.endObject().toString();
    }

    /** Renders one frame of the live scene and writes it to {@code out}. */
    public String cmdFrame(File out) {
        Pixmap pixmap = grab();
        out.getAbsoluteFile().getParentFile().mkdirs();
        PixmapIO.writePNG(new FileHandle(out), pixmap);
        pixmap.dispose();
        return new Json.Writer().beginObject()
                .prop("path", out.getAbsolutePath())
                .prop("scene", sceneName)
                .prop("time", time)
                .prop("width", outWidth())
                .prop("height", outHeight())
                .endObject().toString();
    }

    /**
     * Captures a windowed sequence plus a contact sheet.
     *
     * <p>With {@code fresh}, the scene is rebuilt and replayed from warmup to {@code start},
     * then sampled every {@code step} seconds — byte-for-byte what
     * {@code ./gw capture -Pstart -Pstep} produces, so evidence gathered through the API can
     * be re-derived by anyone with the repo and no server. Without it, the capture continues
     * from wherever the live scene currently is, which is what you want immediately after
     * firing an event.
     */
    public String cmdCapture(File dir, int frames, float step, float start, int cols,
                             String label, boolean fresh) throws Exception {
        dir.mkdirs();
        if (fresh) {
            loadScene(sceneName);
            SceneClock.advance(scene, start);
            time = start;
        }
        List<File> written = new ArrayList<>();
        float t0 = time;
        for (int i = 0; i < frames; i++) {
            if (i > 0) {
                SceneClock.advance(scene, step);
                time += step;
            }
            Pixmap pixmap = grab();
            File out = new File(dir, String.format("frame_%03d.png", i));
            PixmapIO.writePNG(new FileHandle(out), pixmap);
            pixmap.dispose();
            written.add(out);
        }
        File sheet = new File(dir, "contact-sheet.png");
        float window = step * Math.max(0, frames - 1);
        ContactSheet.build(written, sheet, cols,
                label != null ? label : scene.description() + "  [debug API]", window, frames);

        Json.Writer w = new Json.Writer().beginObject()
                .prop("dir", dir.getAbsolutePath())
                .prop("contactSheet", sheet.getAbsolutePath())
                .prop("scene", sceneName)
                .prop("frames", frames)
                .prop("step", step)
                .prop("startTime", t0)
                .prop("endTime", time)
                .prop("fresh", fresh)
                .prop("reproduceWith", fresh
                        ? String.format("./gw capture -Pscene=%s -Pframes=%d -Pcols=%d -Pstart=%s -Pstep=%s",
                                sceneName, frames, cols, trim(start), trim(step))
                        : "not reproducible from the CLI: captured from live scene state");
        w.name("files").beginArray();
        for (File f : written) {
            w.value(f.getName());
        }
        return w.endArray().endObject().toString();
    }

    private static String trim(float v) {
        return String.valueOf(Math.round(v * 100000f) / 100000f);
    }

    /** Measures the live frame in memory, without writing a PNG. */
    public String cmdMeasure(List<String> specs, double threshold) {
        Pixmap pixmap = grab();
        Frame frame = Framebuffers.toFrame(pixmap);
        pixmap.dispose();
        Paper paper = Paper.estimate(frame);
        Figure figure = Figure.detect(frame, paper, threshold);
        Map<String, Rect> rects = new LinkedHashMap<>();
        if (specs == null || specs.isEmpty()) {
            rects.putAll(regions.resolve(frame, figure));
        } else {
            for (String s : specs) {
                if (s.contains("=")) {
                    RegionSet.Def d = RegionSet.parseSpec(s);
                    rects.put(d.name(), d.resolve(frame, figure));
                } else if (regions.def(s) != null) {
                    rects.put(s, regions.def(s).resolve(frame, figure));
                }
            }
        }
        Json.Writer w = new Json.Writer().beginObject()
                .prop("scene", sceneName).prop("time", time)
                .prop("paper", paper.level)
                .prop("figure", figure.bounds)
                .prop("figureHeight", figure.bounds.h)
                .prop("figureInkPixels", figure.boxInk)
                .prop("threshold", threshold);
        w.name("regions").beginArray();
        for (Map.Entry<String, Rect> e : rects.entrySet()) {
            Coverage c = Coverage.measure(frame, paper, e.getKey(), e.getValue(), threshold, figure);
            Centroid k = Centroid.measure(frame, paper, e.getValue(), threshold);
            w.beginObject().prop("name", e.getKey()).prop("rect", e.getValue())
                    .prop("coverage", c.fraction()).prop("shareByCount", c.shareByCount())
                    .prop("meanInkLuminance", c.meanInkLuminance())
                    .prop("centroidX", k.x()).prop("centroidY", k.y()).endObject();
        }
        return w.endArray().endObject().toString();
    }

    public String cmdRegions(File file) throws Exception {
        if (file != null) {
            regions = RegionSet.load(file);
        }
        Pixmap pixmap = grab();
        Frame frame = Framebuffers.toFrame(pixmap);
        pixmap.dispose();
        Paper paper = Paper.estimate(frame);
        Figure figure = Figure.detect(frame, paper, 0.85);
        Json.Writer w = new Json.Writer().beginObject().prop("figure", figure.bounds).name("regions").beginObject();
        for (Map.Entry<String, Rect> e : regions.resolve(frame, figure).entrySet()) {
            w.prop(e.getKey(), e.getValue());
        }
        return w.endObject().endObject().toString();
    }

    // ------------------------------------------------------------------ rendering

    private int outWidth() {
        if (camera == null) {
            return width;
        }
        return cameraOutW > 0 ? cameraOutW : camera.w;
    }

    private int outHeight() {
        if (camera == null) {
            return height;
        }
        return cameraOutH > 0 ? cameraOutH : camera.h;
    }

    /**
     * Renders the current state offscreen and reads it back, applying the camera crop.
     *
     * <p>The camera is a crop of the rendered frame rather than a change to the scene's
     * projection, because scenes do not expose one and because a crop cannot change what the
     * scene draws. Framing the capture on the zone of interest — which is all the brief asks
     * for — must never alter the thing being measured.
     */
    private Pixmap grab() {
        fbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (scene != null) {
            scene.render();
        }
        Pixmap full = Pixmap.createFromFrameBuffer(0, 0, width, height);
        fbo.end();
        flipVertically(full);
        if (camera == null) {
            return full;
        }
        Rect c = camera.clamp(width, height);
        Pixmap cropped = new Pixmap(Math.max(1, c.w), Math.max(1, c.h), Pixmap.Format.RGBA8888);
        cropped.drawPixmap(full, 0, 0, c.x, c.y, c.w, c.h);
        full.dispose();
        int ow = outWidth();
        int oh = outHeight();
        if (ow == cropped.getWidth() && oh == cropped.getHeight()) {
            return cropped;
        }
        Pixmap scaled = new Pixmap(ow, oh, Pixmap.Format.RGBA8888);
        scaled.setFilter(Pixmap.Filter.BiLinear);
        scaled.drawPixmap(cropped, 0, 0, cropped.getWidth(), cropped.getHeight(), 0, 0, ow, oh);
        cropped.dispose();
        return scaled;
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
}
