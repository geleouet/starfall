package dev.starfall.analysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One command per question, over a capture directory or a single frame.
 *
 * <p>Run with no arguments for usage. Every command accepts {@code --json} so it can be
 * driven from a script or an MCP tool; the text form is the one a reviewer reads.
 *
 * <p>Two rules are enforced rather than documented:
 * <ul>
 *   <li>{@code track} refuses to run without {@code --anchor}. A lag figure without its
 *       anchor is unfalsifiable (STYLE.md §7.1) and this tool will not produce one.</li>
 *   <li>Every measurement prints the rectangle it was taken through. The debt documents'
 *       unreproducible numbers are all missing exactly that.</li>
 * </ul>
 */
public final class AnalysisCli {

    /**
     * Resolved on every call rather than cached in a static.
     *
     * <p>{@code DebugServer} runs this CLI in-process by swapping {@link System#out} for a
     * buffer, so a cached stream would send the second and later responses to the first
     * request's buffer — output that silently goes to the wrong caller, which is the worst
     * failure mode available to a measurement tool.
     */
    private static PrintStream out() {
        return System.out;
    }

    public static void main(String[] args) {
        // Reports are read by scripts and diffed between passes; a locale that prints "23,4"
        // instead of "23.4" would silently break both.
        java.util.Locale.setDefault(java.util.Locale.ROOT);
        // Windows consoles default stdout to the ANSI codepage, which mangles the section
        // signs and dashes in this tool's own help and reports.
        System.setOut(new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out),
                true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            System.exit(run(args));
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("error: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static int run(String[] argv) throws IOException {
        if (argv.length == 0 || "help".equals(argv[0]) || "--help".equals(argv[0])) {
            usage();
            return argv.length == 0 ? 2 : 0;
        }
        String cmd = argv[0];
        Args a = new Args(java.util.Arrays.copyOfRange(argv, 1, argv.length));
        return switch (cmd) {
            case "report" -> report(a);
            case "figure" -> figure(a);
            case "regions" -> regions(a);
            case "coverage" -> coverage(a);
            case "bands" -> bands(a);
            case "track" -> track(a);
            case "autocorr" -> autocorr(a);
            case "edge" -> edge(a);
            case "marks" -> marks(a);
            case "values" -> values(a);
            case "diff" -> diff(a);
            case "timing" -> timing(a);
            default -> {
                System.err.println("unknown command '" + cmd + "'");
                usage();
                yield 2;
            }
        };
    }

    private static void usage() {
        out().println("""
                starfall analysis — measure captured pixels, reproducibly.

                  analyse report   <dir|png>            the standard battery: paper, figure, values,
                                                        coverage and bands by region, autocorrelation,
                                                        and the frame-to-frame diff summary
                  analyse figure   <dir|png> [--reference <png>]
                                                        figure bounding box and height; with a reference,
                                                        the downscale factor for STYLE.md 11.0's
                                                        matched-scale comparison
                  analyse regions  <dir|png> [--emit <file>]
                                                        resolve the region set against this frame; --emit
                                                        writes it as regions.json to edit and commit
                  analyse coverage <dir|png> --region <spec>...
                                                        ink coverage in a region and its share of figure ink
                  analyse bands    <dir|png> [--region <name|spec>] [--bands 6]
                                                        mean ink luminance top to bottom — the ink-gravity test
                  analyse track    <dir> --region <spec>... --anchor <name> [--fps 60]
                                                        centroid / registration tracking, velocity,
                                                        reversals, and lag AGAINST THE NAMED ANCHOR
                  analyse autocorr <dir|png> --region <spec> [--axis x|y] [--min 4] [--max 200]
                                                        periodic artefact detection on a high-passed band
                  analyse edge     <png> --at x,y --dir right|left|up|down [--len 140]
                                                        paper-to-core distance and wet-bleed halo width
                  analyse marks    <dir|png> --region <spec> [--cuts 3] [--horizontal]
                                                        mark-width runs and whether they are bimodal
                  analyse values   <dir|png>            floor against #161A22, ceiling against paper
                  analyse timing   <series.json> --anchor <name>
                                                        read a headless timing series (./gw timing) and
                                                        report arrivals in samples and in seconds. 7.1:
                                                        a timing claim ships with one of these
                  analyse diff     <a> <b> [--tolerance 0]
                                                        two frames or two capture directories, plus md5

                Region specs
                  name=420,108,130,110           absolute pixels
                  name=fig:0.35,0.01,0.22,0.21   fractions of the detected figure box (preferred)
                  name=img:0.44,0.20,0.14,0.20   fractions of the frame
                  a bare name (e.g. --region hips) resolves against the loaded region set

                Common options
                  --frame N        which frame of a directory to measure (default 0)
                  --regions FILE   load a region set; otherwise <dir>/regions.json, else the built-in set
                  --paper V        override the estimated paper level
                  --threshold F    ink threshold as a fraction of paper (default 0.85)
                  --json           machine-readable output
                """);
    }

    // ------------------------------------------------------------------ context

    /** Everything the commands share: the frame(s), the paper level, the figure and the regions. */
    private static final class Ctx {
        CaptureDir capture;
        Frame frame;
        Paper paper;
        Figure figure;
        RegionSet regionSet;
        double factor;
        File source;
    }

    private static Ctx load(Args a, int positional, boolean needSequence) throws IOException {
        Ctx c = new Ctx();
        String path = a.requirePositional(positional, "a capture directory or a PNG file");
        File f = new File(path);
        c.source = f;
        if (f.isDirectory()) {
            c.capture = CaptureDir.of(f);
            int idx = a.getInt("frame", 0);
            c.frame = c.capture.frame(Math.max(0, Math.min(c.capture.size() - 1, idx)));
        } else {
            if (needSequence) {
                throw new IllegalArgumentException(path + " is a file; this command needs a capture directory");
            }
            c.frame = Frame.load(f);
        }
        c.paper = a.has("paper") ? Paper.fixed(a.getDouble("paper", 217)) : Paper.estimate(c.frame);
        c.factor = a.getDouble("threshold", 0.85);
        c.figure = Figure.detect(c.frame, c.paper, c.factor);
        if (a.has("regions")) {
            c.regionSet = RegionSet.load(a.getFile("regions", null));
        } else if (c.capture != null) {
            c.regionSet = RegionSet.forCapture(c.capture.dir);
        } else {
            File beside = new File(f.getAbsoluteFile().getParentFile(), "regions.json");
            c.regionSet = beside.isFile() ? RegionSet.load(beside) : RegionSet.samurai();
        }
        return c;
    }

    /** Resolves {@code --region} specs: named entries from the region set, or inline definitions. */
    private static Map<String, Rect> resolveRegions(Ctx c, Args a, boolean defaultToAll) {
        Map<String, Rect> out = new LinkedHashMap<>();
        List<String> specs = a.all("region");
        if (specs.isEmpty()) {
            if (defaultToAll) {
                return c.regionSet.resolve(c.frame, c.figure);
            }
            out.put("figure", c.figure.bounds);
            return out;
        }
        for (String spec : specs) {
            if (spec.contains("=")) {
                RegionSet.Def d = RegionSet.parseSpec(spec);
                out.put(d.name(), d.resolve(c.frame, c.figure));
            } else {
                RegionSet.Def d = c.regionSet.def(spec.trim());
                if (d == null) {
                    throw new IllegalArgumentException("no region named '" + spec + "' in the region set "
                            + c.regionSet.names() + " — pass an inline spec name=x,y,w,h instead");
                }
                out.put(d.name(), d.resolve(c.frame, c.figure));
            }
        }
        return out;
    }

    private static void header(Ctx c) {
        out().printf("source   %s%s%n", c.source,
                c.capture == null ? "" : "  (" + c.capture.size() + " frames, measuring " + c.frame.name() + ")");
        out().printf("%s%n", c.paper);
        out().printf("%s%n", c.figure);
    }

    // ------------------------------------------------------------------ commands

    private static int figure(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject();
            w.prop("source", String.valueOf(c.source));
            w.prop("paper", c.paper.level);
            w.prop("bounds", c.figure.bounds);
            w.prop("height", c.figure.bounds.h);
            w.prop("width", c.figure.bounds.w);
            w.prop("componentInk", c.figure.componentInk);
            w.prop("boxInk", c.figure.boxInk);
            if (a.has("reference")) {
                Frame ref = Frame.load(a.getFile("reference", null));
                Paper rp = Paper.estimate(ref);
                Figure rf = Figure.detect(ref, rp, c.factor);
                w.prop("referenceHeight", rf.bounds.h);
                w.prop("matchedScale", c.figure.bounds.h / (double) rf.bounds.h);
            }
            out().println(w.endObject());
            return 0;
        }
        header(c);
        if (a.has("reference")) {
            File rfile = a.getFile("reference", null);
            Frame ref = Frame.load(rfile);
            Paper rp = Paper.estimate(ref);
            Figure rf = Figure.detect(ref, rp, c.factor);
            double scale = c.figure.bounds.h / (double) rf.bounds.h;
            out().println();
            out().printf("reference %s%n  %s%n", rfile, rf);
            out().printf("STYLE.md 11.0 matched scale: downscale the reference by %.4f "
                            + "(%d px -> %d px) then count readable parts on each side.%n",
                    scale, rf.bounds.h, c.figure.bounds.h);
            out().printf("  suggested reference size: %d x %d%n",
                    Math.round((float) (ref.width * scale)), Math.round((float) (ref.height * scale)));
        }
        return 0;
    }

    private static int regions(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        if (a.has("emit")) {
            File out = a.getFile("emit", null);
            Files.writeString(out.toPath(), c.regionSet.toJson(), StandardCharsets.UTF_8);
            out().println("wrote " + out);
            return 0;
        }
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject();
            w.prop("figure", c.figure.bounds);
            w.name("regions").beginObject();
            for (Map.Entry<String, Rect> e : c.regionSet.resolve(c.frame, c.figure).entrySet()) {
                w.prop(e.getKey(), e.getValue());
            }
            w.endObject();
            out().println(w.endObject());
            return 0;
        }
        header(c);
        out().println("regions resolved against this frame:");
        out().print(c.regionSet.describe(c.frame, c.figure));
        return 0;
    }

    private static int coverage(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        Map<String, Rect> regions = resolveRegions(c, a, true);
        List<Coverage> out = new ArrayList<>();
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            out.add(Coverage.measure(c.frame, c.paper, e.getKey(), e.getValue(), c.factor, c.figure));
        }
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject();
            w.prop("paper", c.paper.level);
            w.prop("threshold", c.factor);
            w.prop("figure", c.figure.bounds);
            w.prop("figureInkPixels", c.figure.boxInk);
            w.name("regions").beginArray();
            for (Coverage cov : out) {
                w.beginObject()
                        .prop("name", cov.region())
                        .prop("rect", cov.rect())
                        .prop("coverage", cov.fraction())
                        .prop("inkPixels", cov.inkPixels())
                        .prop("area", cov.area())
                        .prop("shareByCount", cov.shareByCount())
                        .prop("shareByMass", cov.shareByMass())
                        .prop("meanInkLuminance", cov.meanInkLuminance())
                        .prop("medianInkLuminance", cov.medianInkLuminance())
                        .endObject();
            }
            w.endArray();
            out().println(w.endObject());
            return 0;
        }
        header(c);
        out().println();
        for (Coverage cov : out) {
            out().println(cov.describe());
        }
        return 0;
    }

    private static int bands(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        Map<String, Rect> regions = resolveRegions(c, a, false);
        int n = a.getInt("bands", 6);
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject();
            w.prop("paper", c.paper.level);
            w.name("profiles").beginArray();
            for (Map.Entry<String, Rect> e : regions.entrySet()) {
                BandProfile p = BandProfile.measure(c.frame, c.paper, e.getValue(), n, c.factor);
                w.beginObject().prop("name", e.getKey()).prop("rect", e.getValue())
                        .prop("gravity", p.gravity()).name("bands").beginArray();
                for (BandProfile.Band b : p.bands) {
                    w.beginObject().prop("index", b.index()).prop("rect", b.rect())
                            .prop("inkPixels", b.inkPixels()).prop("coverage", b.coverage())
                            .prop("meanInkLuminance", b.meanInkLuminance())
                            .prop("medianInkLuminance", b.medianInkLuminance()).endObject();
                }
                w.endArray().endObject();
            }
            w.endArray();
            out().println(w.endObject());
            return 0;
        }
        header(c);
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            out().println();
            out().println("region " + e.getKey());
            out().println(BandProfile.measure(c.frame, c.paper, e.getValue(), n, c.factor).describe());
        }
        return 0;
    }

    private static int track(Args a) throws IOException {
        Ctx c = load(a, 0, true);
        String anchor = a.get("anchor", null);
        if (anchor == null) {
            throw new IllegalArgumentException(
                    "--anchor is required. STYLE.md 7.1: 'a hem trails the hips; a sleeve trails the wrist, "
                    + "which is itself already far behind the hips. Both readings are defensible and they "
                    + "differ by a factor of three, so a lag figure quoted without its anchor is "
                    + "unfalsifiable.' Name the region every lag is measured against, e.g. --anchor hips");
        }
        Map<String, Rect> regions = resolveRegions(c, a, true);
        if (!regions.containsKey(anchor)) {
            throw new IllegalArgumentException("--anchor " + anchor + " is not among the tracked regions "
                    + regions.keySet());
        }
        Track.Method method = Track.Method.valueOf(a.get("method", "centroid").toUpperCase());
        Track.Axis axis = Track.Axis.valueOf(a.get("axis", "principal").toUpperCase());
        double gate = a.getDouble("gate", Track.DEFAULT_GATE);
        int radius = a.getInt("radius", Registration.RADIUS);
        double fps = a.getDouble("fps", 0);
        int smooth = a.getInt("smooth", 0);
        List<Frame> frames = c.capture.loadAll();

        Map<String, Track> tracks = new LinkedHashMap<>();
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            tracks.put(e.getKey(), Track.of(e.getKey(), e.getValue(), frames, c.paper, c.factor,
                    method, axis, gate, radius).smoothed(smooth));
        }
        String rev = a.get("reversal", "dominant").trim().toLowerCase();
        Arrivals.Selector selector;
        int ordinal = 0;
        if ("dominant".equals(rev)) {
            selector = Arrivals.Selector.DOMINANT;
        } else if ("first".equals(rev)) {
            selector = Arrivals.Selector.FIRST;
        } else {
            selector = Arrivals.Selector.ORDINAL;
            ordinal = Integer.parseInt(rev);
        }
        boolean matchDirection = !a.flag("any-direction");
        Arrivals arrivals = new Arrivals(anchor, tracks, fps, selector, ordinal, matchDirection);

        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject();
            w.prop("capture", c.capture.name());
            w.prop("frames", c.capture.size());
            w.prop("method", method.name().toLowerCase());
            w.prop("axis", axis.name().toLowerCase());
            w.prop("gate", gate);
            w.prop("smooth", smooth);
            w.prop("anchor", anchor);
            w.prop("reversal", rev);
            w.prop("fps", fps);
            w.name("tracks").beginArray();
            for (Track t : tracks.values()) {
                w.beginObject().prop("name", t.name).prop("rect", t.rect)
                        .prop("axisX", t.axisX).prop("axisY", t.axisY)
                        .prop("x", t.x).prop("y", t.y).prop("pos", t.pos)
                        .prop("velocity", t.vel).prop("speed", t.speed)
                        .prop("peakSpeed", t.peakSpeed())
                        .prop("peakSpeedStep", t.peakSpeedStep() + 0.5)
                        .prop("travel", t.travel())
                        .prop("monotoneSpeed", t.monotoneSpeed());
                w.name("reversals").beginArray();
                for (Track.Crossing x : t.crossings()) {
                    w.beginObject().prop("frame", x.frame()).prop("fromSign", x.fromSign())
                            .prop("swing", x.swing()).endObject();
                }
                w.endArray();
                Track.Crossing dom = t.dominantCrossing();
                w.prop("dominantReversal", dom == null ? Double.NaN : dom.frame());
                w.endObject();
            }
            w.endArray();
            w.name("lags").beginArray();
            for (Arrivals.Lag l : arrivals.lags) {
                w.beginObject().prop("region", l.region()).prop("anchor", l.anchor())
                        .prop("regionFrame", l.regionFrame())
                        .prop("anchorFrame", l.anchorFrame()).prop("lagFrames", l.lagFrames())
                        .prop("lagSeconds", l.lagSeconds()).prop("swing", l.swing()).endObject();
            }
            w.endArray();
            w.prop("spreadFrames", arrivals.spreadFrames());
            w.name("chain").beginArray();
            for (String n : arrivals.chain()) {
                w.value(n);
            }
            w.endArray();
            out().println(w.endObject());
            return 0;
        }
        header(c);
        out().println();
        for (Track t : tracks.values()) {
            out().print(t.describe());
        }
        out().println();
        out().print(arrivals.describe());
        return 0;
    }

    private static int autocorr(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        Map<String, Rect> regions = resolveRegions(c, a, false);
        Autocorrelation.Axis axis = Autocorrelation.Axis.valueOf(a.get("axis", "x").toUpperCase());
        int min = a.getInt("min", 4);
        int max = a.getInt("max", 200);
        int window = a.getInt("window", 0);
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject().name("regions").beginArray();
            for (Map.Entry<String, Rect> e : regions.entrySet()) {
                Autocorrelation ac = Autocorrelation.measure(c.frame, e.getValue(), axis, min, max, window);
                w.beginObject().prop("name", e.getKey()).prop("rect", e.getValue())
                        .prop("axis", axis.name().toLowerCase())
                        .prop("minLag", ac.minLag).prop("maxLag", ac.maxLag)
                        .prop("peakLag", ac.peak().lag()).prop("peakValue", ac.peak().value())
                        .prop("peakProminence", ac.peak().prominence())
                        .prop("maxCorrelation", ac.maxCorrelation().value())
                        .prop("maxCorrelationLag", ac.maxCorrelation().lag())
                        .prop("clean", ac.clean()).prop("values", ac.values).endObject();
            }
            out().println(w.endArray().endObject());
            return 0;
        }
        header(c);
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            out().println();
            out().println("region " + e.getKey());
            out().print(Autocorrelation.measure(c.frame, e.getValue(), axis, min, max, window).describe());
        }
        return 0;
    }

    private static int edge(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        String at = a.require("at", "the scanline start, as x,y on the paper side of the boundary");
        String[] parts = at.split("\\s*,\\s*");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        EdgeProfile.Direction dir = EdgeProfile.Direction.parse(a.get("dir", "right"));
        int len = a.getInt("len", 140);
        int lead = a.getInt("paper-samples", 5);
        EdgeProfile p = EdgeProfile.measure(c.frame, x, y, dir, len, lead);
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject()
                    .prop("x", x).prop("y", y).prop("direction", dir.name().toLowerCase())
                    .prop("paperLevel", p.paperLevel).prop("coreLevel", p.coreLevel)
                    .prop("haloStart", p.haloStart).prop("transitionStart", p.transitionStart)
                    .prop("coreIndex", p.coreIndex)
                    .prop("transitionWidth", p.transitionWidth()).prop("haloWidth", p.haloWidth())
                    .prop("hardEdge", p.hardEdge())
                    .prop("samples", p.samples);
            out().println(w.endObject());
            return 0;
        }
        header(c);
        out().println();
        out().print(p.describe());
        return 0;
    }

    private static int marks(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        Map<String, Rect> regions = resolveRegions(c, a, false);
        int cuts = a.getInt("cuts", 3);
        boolean vertical = !a.flag("horizontal");
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject().name("regions").beginArray();
            for (Map.Entry<String, Rect> e : regions.entrySet()) {
                MarkWidths m = MarkWidths.measure(c.frame, c.paper, e.getValue(), vertical, cuts, c.factor);
                w.beginObject().prop("name", e.getKey()).prop("rect", e.getValue())
                        .prop("vertical", vertical).prop("bimodality", m.bimodality())
                        .prop("bimodal", m.bimodal());
                w.name("cuts").beginArray();
                for (MarkWidths.Cut cut : m.cuts) {
                    w.beginObject().prop("position", cut.position()).name("runs").beginArray();
                    for (int r : cut.runs()) {
                        w.value(r);
                    }
                    w.endArray().endObject();
                }
                w.endArray();
                w.name("histogram").beginArray();
                for (int h : m.histogram()) {
                    w.value(h);
                }
                w.endArray().endObject();
            }
            out().println(w.endArray().endObject());
            return 0;
        }
        header(c);
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            out().println();
            out().println("region " + e.getKey());
            out().print(MarkWidths.measure(c.frame, c.paper, e.getValue(), vertical, cuts, c.factor).describe());
        }
        return 0;
    }

    private static int values(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        ValueRange v = ValueRange.measure(c.frame, c.paper);
        if (a.flag("json")) {
            out().println(new Json.Writer().beginObject()
                    .prop("source", c.frame.name())
                    .prop("paper", c.paper.level)
                    .prop("minLuminance", v.minLuminance)
                    .prop("minRgb", String.format("#%06X", v.minRgb))
                    .prop("darkest005", v.p005Luminance)
                    .prop("brightest995", v.p995Luminance)
                    .prop("maxLuminance", v.maxLuminance)
                    .prop("maxRgb", String.format("#%06X", v.maxRgb))
                    .prop("floorRespected", v.floorRespected())
                    .prop("ceilingRespected", v.ceilingRespected())
                    .prop("pureBlack", v.pureBlack)
                    .prop("pureWhite", v.pureWhite)
                    .endObject());
            return 0;
        }
        header(c);
        out().println();
        out().print(v.describe());
        return 0;
    }

    private static int diff(Args a) throws IOException {
        File x = new File(a.requirePositional(0, "first frame or capture directory"));
        File y = new File(a.requirePositional(1, "second frame or capture directory"));
        int tol = a.getInt("tolerance", 0);
        List<FrameDiff> diffs = new ArrayList<>();
        if (x.isDirectory() && y.isDirectory()) {
            CaptureDir cx = CaptureDir.of(x);
            CaptureDir cy = CaptureDir.of(y);
            int n = Math.min(cx.size(), cy.size());
            for (int i = 0; i < n; i++) {
                diffs.add(FrameDiff.of(cx.frameFile(i), cy.frameFile(i), tol));
            }
            if (cx.size() != cy.size()) {
                System.err.printf("note: %s has %d frames, %s has %d; compared the first %d%n",
                        cx.name(), cx.size(), cy.name(), cy.size(), n);
            }
        } else {
            diffs.add(FrameDiff.of(x, y, tol));
        }
        long identical = diffs.stream().filter(FrameDiff::identicalBytes).count();
        long changed = diffs.stream().mapToLong(d -> d.changedPixels).sum();
        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject()
                    .prop("a", x.getPath()).prop("b", y.getPath())
                    .prop("tolerance", tol)
                    .prop("frames", diffs.size())
                    .prop("bitIdenticalFrames", identical)
                    .prop("totalChangedPixels", changed);
            w.name("perFrame").beginArray();
            for (FrameDiff d : diffs) {
                w.beginObject().prop("a", d.a.getName()).prop("b", d.b.getName())
                        .prop("md5a", d.md5a).prop("md5b", d.md5b)
                        .prop("identical", d.identicalBytes())
                        .prop("changedPixels", d.changedPixels)
                        .prop("maxDelta", d.maxDelta)
                        .prop("meanAbsDelta", d.meanAbsDelta)
                        .prop("changedBounds", d.changedBounds).endObject();
            }
            out().println(w.endArray().endObject());
            return 0;
        }
        for (FrameDiff d : diffs) {
            out().print(d.describe());
        }
        if (diffs.size() > 1) {
            out().printf("%n%d of %d frames bit-identical; %d pixels changed in total%n",
                    identical, diffs.size(), changed);
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static int timing(Args a) throws IOException {
        File file = new File(a.requirePositional(0, "a timing series written by ./gw timing"));
        Map<String, Object> root = Json.parseObject(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        String anchor = a.get("anchor", null);
        if (anchor == null) {
            throw new IllegalArgumentException(
                    "--anchor is required, for the same reason it is required on `track`: a lag "
                    + "figure without its anchor is unfalsifiable (STYLE.md 7.1)");
        }
        double rate = ((Number) root.getOrDefault("rate", 240.0)).doubleValue();
        double start = ((Number) root.getOrDefault("start", 0.0)).doubleValue();
        double gate = a.getDouble("gate", Track.DEFAULT_GATE);
        Track.Axis axis = Track.Axis.valueOf(a.get("axis", "principal").toUpperCase());
        int smooth = a.getInt("smooth", 0);

        Map<String, Track> tracks = new LinkedHashMap<>();
        for (Object o : (List<Object>) root.get("regions")) {
            Map<String, Object> r = (Map<String, Object>) o;
            String name = String.valueOf(r.get("name"));
            List<Object> rectNums = (List<Object>) r.get("rect");
            Rect rect = new Rect(((Number) rectNums.get(0)).intValue(), ((Number) rectNums.get(1)).intValue(),
                    ((Number) rectNums.get(2)).intValue(), ((Number) rectNums.get(3)).intValue());
            double[] xs = toArray((List<Object>) r.get("x"));
            double[] ys = toArray((List<Object>) r.get("y"));
            tracks.put(name, Track.fromPositions(name, rect, xs, ys, axis, gate).smoothed(smooth));
        }
        if (!tracks.containsKey(anchor)) {
            throw new IllegalArgumentException("--anchor " + anchor + " is not in this series " + tracks.keySet());
        }
        Arrivals arrivals = new Arrivals(anchor, tracks, rate);

        if (a.flag("json")) {
            Json.Writer w = new Json.Writer().beginObject()
                    .prop("source", file.getPath())
                    .prop("scene", String.valueOf(root.get("scene")))
                    .prop("rate", rate).prop("start", start).prop("anchor", anchor);
            w.name("arrivals").beginArray();
            for (String n : arrivals.chain()) {
                Track.Crossing c = arrivals.selected(n);
                w.beginObject().prop("region", n)
                        .prop("sample", c == null ? Double.NaN : c.frame())
                        .prop("seconds", c == null ? Double.NaN : start + c.frame() / rate)
                        .prop("swing", c == null ? Double.NaN : c.swing()).endObject();
            }
            w.endArray();
            w.name("lags").beginArray();
            for (Arrivals.Lag l : arrivals.lags) {
                w.beginObject().prop("region", l.region()).prop("anchor", l.anchor())
                        .prop("lagSamples", l.lagFrames()).prop("lagSeconds", l.lagSeconds()).endObject();
            }
            w.endArray().prop("spreadSamples", arrivals.spreadFrames());
            out().println(w.endObject());
            return 0;
        }
        out().printf("timing series %s%n  scene '%s', %.1f samples/s from t=%.4f s, figure %s%n",
                file, root.get("scene"), rate, start, root.get("figure"));
        out().println("  measured from the same offscreen render the capture writes, driving the same scene");
        out().println();
        for (Track t : tracks.values()) {
            out().print(t.describe());
        }
        out().println();
        out().print(arrivals.describe());
        out().println("  in seconds of simulated time:");
        for (String n : arrivals.chain()) {
            Track.Crossing c = arrivals.selected(n);
            out().printf("    %-14s %s%n", n,
                    c == null ? "no reversal" : String.format("t = %.4f s", start + c.frame() / rate));
        }
        return 0;
    }

    private static double[] toArray(List<Object> list) {
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) {
            Object v = list.get(i);
            out[i] = v instanceof Number n ? n.doubleValue() : Double.NaN;
        }
        return out;
    }

    private static int report(Args a) throws IOException {
        Ctx c = load(a, 0, false);
        header(c);
        out().println();
        out().print(ValueRange.measure(c.frame, c.paper).describe());

        out().println();
        out().println("-- coverage by region (STYLE.md 11.0: count what survives at this scale) --");
        for (Map.Entry<String, Rect> e : c.regionSet.resolve(c.frame, c.figure).entrySet()) {
            out().println(Coverage.measure(c.frame, c.paper, e.getKey(), e.getValue(), c.factor, c.figure).describe());
        }

        out().println();
        out().println("-- ink gravity over the figure (STYLE.md 3.4; System 1 debt D5) --");
        out().println(BandProfile.measure(c.frame, c.paper, c.figure.bounds, a.getInt("bands", 6), c.factor).describe());

        out().println();
        out().println("-- periodic artefacts (STYLE.md 3 postscript: measure the period before calling it structural) --");
        Rect torso = c.regionSet.def("torso") != null
                ? c.regionSet.def("torso").resolve(c.frame, c.figure) : c.figure.bounds;
        out().print(Autocorrelation.measure(c.frame, torso, Autocorrelation.Axis.X, 4, 200).describe());
        Rect ground = new Rect(0, (int) (c.frame.height * 0.82), c.frame.width, (int) (c.frame.height * 0.14));
        out().print(Autocorrelation.measure(c.frame, ground, Autocorrelation.Axis.X, 4, 200).describe());

        if (c.capture != null && c.capture.size() > 1) {
            out().println();
            out().println("-- frame-to-frame change (a flat series means the window shows nothing) --");
            for (int i = 1; i < c.capture.size(); i++) {
                FrameDiff d = FrameDiff.of(c.capture.frameFile(i - 1), c.capture.frameFile(i), 2);
                out().printf("  %02d->%02d  %7d px changed, max delta %3d, mean |d| %.3f%n",
                        i - 1, i, d.changedPixels, d.maxDelta, d.meanAbsDelta);
            }
            out().println();
            out().println("For motion, run `analyse track <dir> --anchor <region>`; this report deliberately");
            out().println("does not print a lag, because a lag without its anchor is unfalsifiable (7.1).");
        }
        return 0;
    }
}
