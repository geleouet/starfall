package dev.starfall.capture;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import dev.starfall.analysis.Centroid;
import dev.starfall.analysis.Figure;
import dev.starfall.analysis.Frame;
import dev.starfall.analysis.Json;
import dev.starfall.analysis.Paper;
import dev.starfall.analysis.Rect;
import dev.starfall.analysis.RegionSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless timing measurement: drives the scene a capture runs, at a finer sample rate than
 * the capture, and records where each named region is at every sample.
 *
 * <p>STYLE.md §7.1:
 *
 * <blockquote>Any timing claim ships with a headless measurement, and that measurement must
 * drive the same scene the capture runs rather than re-enacting it.</blockquote>
 *
 * <p>This is that measurement, and it takes the requirement literally. It builds the scene
 * through the same {@link SceneRegistry}, warms it up the same way, advances it with the same
 * fixed {@code 1/240 s} substep as {@link CaptureApp}, and renders through the same offscreen
 * path. The only thing it changes is that it samples more often and writes numbers instead of
 * PNGs. A previous pass reported peak frames at 126/132/139/146/147/150 for a window that
 * covered frames 111-134; a measurement built this way cannot do that, because the sample
 * index and the capture frame index are the same clock.
 *
 * <p>Regions are resolved once, on the first sample, against the detected figure — so every
 * sample in the series is measured through the identical rectangle, and the rectangle is
 * written into the output file beside the numbers.
 */
public class TimingApp extends ApplicationAdapter {

    private final TimingSpec spec;
    private final Scene scene;

    private FrameBuffer fbo;
    private Map<String, Rect> regions;
    private Figure figure;
    private Paper paper;
    private final List<Double> times = new ArrayList<>();
    private final Map<String, List<double[]>> series = new LinkedHashMap<>();
    private final Map<String, List<float[]>> probes = new LinkedHashMap<>();
    private int sample;

    public TimingApp(TimingSpec spec) {
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
        if (sample >= spec.samples()) {
            finish();
            return;
        }
        if (sample > 0) {
            simulate(spec.interval());
        }
        Frame frame = grab();
        if (regions == null) {
            paper = Paper.estimate(frame);
            figure = Figure.detect(frame, paper, spec.threshold);
            regions = spec.regions.resolve(frame, figure);
            for (String name : regions.keySet()) {
                series.put(name, new ArrayList<>());
            }
        }
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            Centroid c = Centroid.measure(frame, paper, e.getValue(), spec.threshold);
            series.get(e.getKey()).add(new double[] {c.x(), c.y(), c.mass(), c.inkPixels()});
        }
        if (scene instanceof SceneProbe p) {
            for (Map.Entry<String, float[]> e : p.probe().entrySet()) {
                probes.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue().clone());
            }
        }
        times.add(spec.start + sample * (double) spec.interval());
        sample++;
    }

    private Frame grab() {
        fbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        scene.render();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, spec.width, spec.height);
        fbo.end();
        Frame f = Framebuffers.toFrame(pixmap);
        pixmap.dispose();
        return f;
    }

    private void simulate(float seconds) {
        SceneClock.advance(scene, seconds);
    }

    private void finish() {
        try {
            String json = toJson();
            if (spec.out != null) {
                File parent = spec.out.getAbsoluteFile().getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                Files.writeString(spec.out.toPath(), json, StandardCharsets.UTF_8);
                System.out.println("TIMING_OUT=" + spec.out.getAbsolutePath());
            } else {
                System.out.println(json);
            }
            System.out.println("TIMING_SAMPLES=" + sample);
            System.out.printf("TIMING_RATE=%.2f%n", 1.0 / spec.interval());
        } catch (IOException e) {
            System.err.println("failed to write timing series: " + e);
        }
        Gdx.app.exit();
    }

    private String toJson() {
        Json.Writer w = new Json.Writer().beginObject();
        w.prop("scene", spec.sceneName);
        w.prop("clamp", spec.clamp.isEmpty() ? "none" : spec.clamp);
        w.prop("sceneDescription", scene.description());
        w.prop("warmup", scene.warmup());
        w.prop("start", spec.start);
        w.prop("interval", spec.interval());
        w.prop("rate", 1.0 / spec.interval());
        w.prop("substep", SceneClock.SUBSTEP);
        w.prop("samples", sample);
        w.prop("width", spec.width);
        w.prop("height", spec.height);
        w.prop("threshold", spec.threshold);
        w.prop("paper", paper == null ? Double.NaN : paper.level);
        w.prop("figure", figure == null ? new Rect(0, 0, 0, 0) : figure.bounds);
        w.prop("note", "Positions are ink-weighted centroids of fixed rectangles, measured from the "
                + "same offscreen render the capture writes, driving the same scene with the same "
                + "fixed substep. Sample index i corresponds to simulated time start + i*interval.");
        w.name("times").beginArray();
        for (double t : times) {
            w.value(t);
        }
        w.endArray();
        w.name("regions").beginArray();
        for (Map.Entry<String, List<double[]>> e : series.entrySet()) {
            w.beginObject().prop("name", e.getKey()).prop("rect", regions.get(e.getKey()));
            double[] xs = new double[e.getValue().size()];
            double[] ys = new double[e.getValue().size()];
            double[] mass = new double[e.getValue().size()];
            for (int i = 0; i < xs.length; i++) {
                xs[i] = e.getValue().get(i)[0];
                ys[i] = e.getValue().get(i)[1];
                mass[i] = e.getValue().get(i)[2];
            }
            w.prop("x", xs).prop("y", ys).prop("mass", mass).endObject();
        }
        w.endArray();
        if (!probes.isEmpty()) {
            w.name("probes").beginArray();
            for (Map.Entry<String, List<float[]>> e : probes.entrySet()) {
                double[] xs = new double[e.getValue().size()];
                double[] ys = new double[e.getValue().size()];
                for (int i = 0; i < xs.length; i++) {
                    xs[i] = e.getValue().get(i)[0];
                    ys[i] = e.getValue().get(i).length > 1 ? e.getValue().get(i)[1] : 0;
                }
                w.beginObject().prop("name", e.getKey()).prop("x", xs).prop("y", ys).endObject();
            }
            w.endArray();
        }
        return w.endObject().toString();
    }

    @Override
    public void dispose() {
        scene.dispose();
        if (fbo != null) {
            fbo.dispose();
        }
    }

    /** Parsed configuration for a headless timing run. */
    public static final class TimingSpec {
        public String sceneName = "smoke";
        public File out;
        public int width = 960;
        public int height = 540;
        public float start = 0f;
        public float duration = 0.4f;
        public float rate = 240f;
        public double threshold = 0.85;
        public RegionSet regions = RegionSet.samurai();
        /** {@code cloth} welds every cloth chain to bind. See {@link dev.starfall.sim.ClothSim#clampRigid}. */
        public String clamp = "";

        public float interval() {
            return 1f / Math.max(1f, rate);
        }

        public int samples() {
            return Math.max(1, Math.round(duration * rate) + 1);
        }

        public static TimingSpec parse(String[] args) throws IOException {
            TimingSpec s = new TimingSpec();
            for (int i = 0; i < args.length - 1; i++) {
                String key = args[i];
                String value = args[i + 1];
                switch (key) {
                    case "--scene" -> { s.sceneName = value; i++; }
                    case "--out" -> { s.out = new File(value); i++; }
                    case "--w" -> { s.width = Integer.parseInt(value); i++; }
                    case "--h" -> { s.height = Integer.parseInt(value); i++; }
                    case "--start" -> { s.start = Float.parseFloat(value); i++; }
                    case "--duration" -> { s.duration = Float.parseFloat(value); i++; }
                    case "--rate" -> { s.rate = Float.parseFloat(value); i++; }
                    case "--threshold" -> { s.threshold = Double.parseDouble(value); i++; }
                    case "--regions" -> { s.regions = RegionSet.load(new File(value)); i++; }
                    case "--clamp" -> { s.clamp = value; i++; }
                    default -> { }
                }
            }
            return s;
        }
    }
}
