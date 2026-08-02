package dev.starfall.debug;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.starfall.analysis.AnalysisCli;
import dev.starfall.analysis.Json;
import dev.starfall.analysis.Rect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * The in-game debug API: a small HTTP server over {@link DebugApp}.
 *
 * <p>Bound to loopback only and started by an explicit flag, so it never runs in a normal
 * build — see {@link DebugLauncher}.
 *
 * <p>The endpoints are deliberately one-to-one with the MCP tools in {@code mcp/}, so that
 * server is a wrapper with no logic of its own, and equally one-to-one with
 * {@code tools/sfctl.mjs}, which is the same thing usable from a shell today. Anything the
 * MCP server can do, the CLI can do; anything either can do, {@code curl} can do.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><td>{@code GET  /health}</td><td>liveness plus the current scene and time</td></tr>
 *   <tr><td>{@code GET  /scenes}</td><td>every registered scene name</td></tr>
 *   <tr><td>{@code GET  /state}</td><td>full state: scene, time, camera, events, probe</td></tr>
 *   <tr><td>{@code POST /scene}</td><td>{@code {"name": "sim-extreme"}}</td></tr>
 *   <tr><td>{@code POST /time}</td><td>{@code {"t": 1.95}} — absolute simulated seconds</td></tr>
 *   <tr><td>{@code POST /step}</td><td>{@code {"frames": 4, "dt": 0.0167}}</td></tr>
 *   <tr><td>{@code POST /camera}</td><td>{@code {"x":420,"y":100,"w":260,"h":260,"outW":520}} or {@code {"reset":true}}</td></tr>
 *   <tr><td>{@code POST /event}</td><td>{@code {"name":"knockback","args":{"dir":"left"}}}</td></tr>
 *   <tr><td>{@code POST /frame}</td><td>{@code {"out":"out/debug/one.png"}}</td></tr>
 *   <tr><td>{@code POST /capture}</td><td>{@code {"out":"out/captures/x","frames":24,"step":0.0167,"start":1.95,"cols":6}}</td></tr>
 *   <tr><td>{@code POST /measure}</td><td>{@code {"regions":["hair","hips"]}} — measures the live frame</td></tr>
 *   <tr><td>{@code POST /regions}</td><td>{@code {"file":"docs/regions.json"}} — load, then resolve</td></tr>
 *   <tr><td>{@code POST /analyse}</td><td>{@code {"args":["report","out/captures/x"]}} — runs the analysis CLI</td></tr>
 *   <tr><td>{@code POST /shutdown}</td><td>stops the server and the app</td></tr>
 * </table>
 */
public final class DebugServer {

    private final DebugApp app;
    private final HttpServer http;
    private final int port;

    public DebugServer(DebugApp app, int port) throws IOException {
        this.app = app;
        this.port = port;
        this.http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        this.http.setExecutor(Executors.newFixedThreadPool(2));

        get("/health", e -> new Json.Writer().beginObject()
                .prop("ok", app.isReady()).prop("port", port).endObject().toString());
        get("/scenes", e -> app.submit(app::cmdScenes));
        get("/state", e -> app.submit(app::cmdState));

        post("/scene", body -> {
            String name = str(body, "name", null);
            require(name != null, "name is required");
            return app.submit(() -> app.cmdLoad(name));
        });
        post("/time", body -> {
            float t = (float) num(body, "t", Double.NaN);
            require(!Double.isNaN(t), "t (absolute simulated seconds) is required");
            return app.submit(() -> app.cmdSeek(t));
        });
        post("/step", body -> {
            int frames = (int) num(body, "frames", 1);
            float dt = (float) num(body, "dt", 1.0 / 60.0);
            return app.submit(() -> app.cmdStep(frames, dt));
        });
        post("/camera", body -> {
            if (bool(body, "reset", false)) {
                return app.submit(() -> app.cmdCamera(null, 0, 0));
            }
            Rect r = new Rect((int) num(body, "x", 0), (int) num(body, "y", 0),
                    (int) num(body, "w", 0), (int) num(body, "h", 0));
            require(!r.isEmpty(), "camera needs a non-empty w and h, or {\"reset\": true}");
            int ow = (int) num(body, "outW", 0);
            int oh = (int) num(body, "outH", 0);
            return app.submit(() -> app.cmdCamera(r, ow, oh));
        });
        post("/event", body -> {
            String name = str(body, "name", null);
            require(name != null, "name is required");
            Map<String, String> args = new LinkedHashMap<>();
            if (body.get("args") instanceof Map<?, ?> m) {
                m.forEach((k, v) -> args.put(String.valueOf(k), String.valueOf(v)));
            }
            return app.submit(() -> app.cmdEvent(name, args));
        });
        post("/frame", body -> {
            File out = new File(str(body, "out", "out/debug/frame.png"));
            return app.submit(() -> app.cmdFrame(out));
        });
        post("/capture", body -> {
            File dir = new File(str(body, "out", "out/captures/debug"));
            int frames = (int) num(body, "frames", 12);
            float step = (float) num(body, "step", 1.0 / 60.0);
            float start = (float) num(body, "start", 0);
            int cols = (int) num(body, "cols", 6);
            String label = str(body, "label", null);
            boolean fresh = bool(body, "fresh", true);
            return app.submit(() -> app.cmdCapture(dir, frames, step, start, cols, label, fresh));
        });
        post("/measure", body -> {
            List<String> specs = new ArrayList<>();
            if (body.get("regions") instanceof List<?> l) {
                l.forEach(v -> specs.add(String.valueOf(v)));
            }
            double th = num(body, "threshold", 0.85);
            return app.submit(() -> app.cmdMeasure(specs, th));
        });
        post("/regions", body -> {
            String file = str(body, "file", null);
            return app.submit(() -> app.cmdRegions(file == null ? null : new File(file)));
        });
        post("/analyse", body -> {
            List<String> args = new ArrayList<>();
            if (body.get("args") instanceof List<?> l) {
                l.forEach(v -> args.add(String.valueOf(v)));
            }
            require(!args.isEmpty(), "args is required, e.g. [\"report\", \"out/captures/x\"]");
            return runAnalysis(args);
        });
        post("/shutdown", body -> {
            new Thread(() -> {
                sleepQuietly();
                stop();
                com.badlogic.gdx.Gdx.app.exit();
            }, "starfall-debug-shutdown").start();
            return new Json.Writer().beginObject().prop("stopping", true).endObject().toString();
        });
    }

    /**
     * Runs the analysis CLI in-process and returns whatever it printed.
     *
     * <p>In-process rather than by spawning a JVM: the point of the loop is that a reviewer
     * gets an answer in one round trip, and a cold Gradle start is several seconds. The
     * output is byte-identical to {@code ./gw analyse} because it is the same code path.
     */
    private static String runAnalysis(List<String> args) throws Exception {
        PrintStream stdout = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code;
        synchronized (DebugServer.class) {
            try {
                System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
                code = AnalysisCli.run(args.toArray(new String[0]));
            } finally {
                System.setOut(stdout);
            }
        }
        String text = buffer.toString(StandardCharsets.UTF_8);
        return new Json.Writer().beginObject()
                .prop("exit", code)
                .prop("command", String.join(" ", args))
                .prop("output", text)
                .endObject().toString();
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        http.start();
        System.out.println("DEBUG_API=http://127.0.0.1:" + port);
        System.out.println("DEBUG_API_READY=1");
    }

    public void stop() {
        http.stop(0);
    }

    public int port() {
        return port;
    }

    // ------------------------------------------------------------------ plumbing

    private interface GetHandler {
        String handle(HttpExchange e) throws Exception;
    }

    private interface PostHandler {
        String handle(Map<String, Object> body) throws Exception;
    }

    private void get(String path, GetHandler h) {
        http.createContext(path, wrap(e -> {
            if (!"GET".equalsIgnoreCase(e.getRequestMethod())) {
                throw new IllegalArgumentException(path + " is GET only");
            }
            return h.handle(e);
        }));
    }

    private void post(String path, PostHandler h) {
        http.createContext(path, wrap(e -> {
            if (!"POST".equalsIgnoreCase(e.getRequestMethod())) {
                throw new IllegalArgumentException(path + " is POST only");
            }
            String raw = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            Map<String, Object> body = raw.isEmpty() ? Map.of() : Json.parseObject(raw);
            return h.handle(body);
        }));
    }

    private HttpHandler wrap(GetHandler h) {
        return exchange -> {
            String response;
            int status = 200;
            try {
                response = h.handle(exchange);
            } catch (IllegalArgumentException ex) {
                status = 400;
                response = error(ex);
            } catch (Exception ex) {
                status = 500;
                response = error(ex);
            }
            byte[] bytes = (response + "\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        };
    }

    private static String error(Exception ex) {
        String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        return new Json.Writer().beginObject()
                .prop("error", message)
                .prop("type", ex.getClass().getSimpleName())
                .endObject().toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String str(Map<String, Object> body, String key, String fallback) {
        Object v = body.get(key);
        return v == null ? fallback : String.valueOf(v);
    }

    private static double num(Map<String, Object> body, String key, double fallback) {
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof String s && !s.isBlank()) {
            return Double.parseDouble(s.trim());
        }
        return fallback;
    }

    private static boolean bool(Map<String, Object> body, String key, boolean fallback) {
        Object v = body.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return fallback;
    }
}
