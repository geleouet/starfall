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
            case "drape" -> drape(a);
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
                  analyse drape    <dir> --region <cloth> --anchor <name> [--control <dir>]
                                                        STYLE.md 7.1's cloth criterion: peak |x(cloth) - x(anchor)|
                                                        as a multiple of the anchor's travel, its return, and the
                                                        rigid control. Shoot the control with -Pclamp=cloth.
                                                        --control is CHECKED: refused unless its capture.txt
                                                        says clamp=cloth and its scene/start/step/frames/size
                                                        match the live capture's. --allow-harness-drift waives
                                                        only the harness check (STYLE.md 11.2b(d)).
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

        // The anchor is measured first and every other region's principal axis is signed
        // against it. An eigenvector has no natural sign, and taking it from each region's
        // own net travel made "did these two reverse in the same direction?" a question
        // about the flag rather than about the figure -- see Track#principalAxis.
        Map<String, Track> tracks = new LinkedHashMap<>();
        Track anchorTrack = Track.of(anchor, regions.get(anchor), frames, c.paper, c.factor,
                method, axis, gate, radius).smoothed(smooth);
        double[] reference = {anchorTrack.axisX, anchorTrack.axisY};
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            tracks.put(e.getKey(), e.getKey().equals(anchor) ? anchorTrack
                    : Track.of(e.getKey(), e.getValue(), frames, c.paper, c.factor,
                            method, axis, gate, radius, reference).smoothed(smooth));
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

    /**
     * STYLE.md §7.1's drape excursion, with its three gates.
     *
     * <p>The section was amended to require this statistic and, like the rigid control it
     * depends on, nothing in the repository could compute it — so the criterion that decides
     * whether cloth ships was being quoted from hand arithmetic. See {@link Drape} for why
     * displacement replaced phase.
     */
    private static int drape(Args a) throws IOException {
        Ctx c = load(a, 0, true);
        String anchor = a.get("anchor", null);
        if (anchor == null) {
            throw new IllegalArgumentException("--anchor is required: a drape excursion is a "
                    + "displacement *against* something, and STYLE.md 7.1 will not accept one "
                    + "without the anchor named. Try --anchor hips");
        }
        Map<String, Rect> regions = resolveRegions(c, a, false);
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("--region <cloth box> is required");
        }
        Rect anchorRect = regions.get(anchor);
        if (anchorRect == null) {
            RegionSet.Def def = c.regionSet.def(anchor);
            if (def == null) {
                throw new IllegalArgumentException("--anchor " + anchor + " is not in the region set "
                        + c.regionSet.names());
            }
            anchorRect = def.resolve(c.frame, c.figure);
        }
        // Registration by default, and the default is not a taste: STYLE.md 11.3 records that
        // integer registration on a box moving under half a pixel per step manufactures exact
        // zeros, and 7.1 asks for this statistic sub-pixel by name.
        Track.Method method = Track.Method.valueOf(a.get("method", "register").toUpperCase());
        Track.Axis axis = Track.Axis.valueOf(a.get("axis", "x").toUpperCase());
        double gate = a.getDouble("gate", Track.DEFAULT_GATE);
        int radius = a.getInt("radius", Registration.RADIUS);
        double fps = a.getDouble("fps", 60);
        int smooth = a.getInt("smooth", 0);

        header(c);
        out().println();
        List<Frame> frames = c.capture.loadAll();
        Track anchorTrack = Track.of(anchor, anchorRect, frames, c.paper, c.factor,
                method, axis, gate, radius).smoothed(smooth);
        double[] reference = {anchorTrack.axisX, anchorTrack.axisY};

        String controlDir = a.get("control", null);
        Map<String, Drape> controls = new LinkedHashMap<>();
        if (controlDir != null) {
            CaptureDir cd = CaptureDir.of(new File(controlDir));
            requireRigidControl(c.capture, cd, a.flag("allow-harness-drift"));
            List<Frame> cf = cd.loadAll();
            Paper cp = Paper.estimate(cf.get(0));
            // Deliberately the SAME rectangles as the live run, not rectangles re-resolved
            // against the control's own figure box. A control exists to show what a box
            // reads with the simulation dead; re-deriving the box from the dead figure would
            // measure a different box and prove nothing about this one.
            Track ca = Track.of(anchor, anchorRect, cf, cp, c.factor, method, axis, gate, radius)
                    .smoothed(smooth);
            double[] cref = {ca.axisX, ca.axisY};
            for (Map.Entry<String, Rect> e : regions.entrySet()) {
                Track ct = Track.of(e.getKey(), e.getValue(), cf, cp, c.factor, method, axis, gate,
                        radius, cref).smoothed(smooth);
                controls.put(e.getKey(), Drape.measure(ct, ca, fps));
            }
            out().printf("rigid control  %s  (%d frames)%n", cd.dir, cf.size());
            out().println();
        }

        boolean ok = true;
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            if (e.getKey().equals(anchor)) {
                continue;
            }
            Track cloth = Track.of(e.getKey(), e.getValue(), frames, c.paper, c.factor,
                    method, axis, gate, radius, reference).smoothed(smooth);
            Drape d = Drape.measure(cloth, anchorTrack, fps);
            out().print(d.describe());
            Drape ctrl = controls.get(e.getKey());
            if (ctrl != null) {
                out().print(Drape.describeControl(d, ctrl));
                ok &= ctrl.ratio() <= Drape.CONTROL_GATE;
            } else {
                out().printf("  GATE 3 rigid control  not run. Shoot one with"
                        + " `./gw capture -Pscene=... -Pclamp=cloth` and pass --control <dir>.%n");
                ok = false;
            }
            if (!cloth.clipped.isEmpty() || !anchorTrack.clipped.isEmpty()) {
                out().println("  WARNING registration hit its search window; widen --radius");
            }
            ok &= d.gate1() && d.gate2();
            out().println();
        }
        return ok ? 0 : 1;
    }
    /**
     * Refuses a {@code --control} directory that is not, in fact, a rigid control of this
     * capture.
     *
     * <p>STYLE.md §11.2b(e), which names this function's absence as its worked example:
     *
     * <blockquote>A discipline written into a document but not into the tool that reads it is
     * documentation, not a guard. {@code track} <i>refuses</i> to run without an anchor, and
     * anchors stopped being a problem. {@code drape} writes the control flag into the manifest
     * and never checks it — and a reviewer proved in one command that it will call a live
     * capture a rigid control.</blockquote>
     *
     * <p>Four refusals, and each one is a way the pass-4 control could have been wrong without
     * anybody noticing:
     *
     * <ol>
     *   <li><b>No {@code capture.txt}.</b> Then nothing about the directory is known and the
     *       gate-3 line printed under it would be a guess wearing a number.</li>
     *   <li><b>{@code clamp} is not {@code cloth}.</b> The literal defect reported: a live
     *       capture handed to {@code --control} produced a rigid-control verdict in silence.</li>
     *   <li><b>Scene, start, step or frame count differ.</b> A control exists to say what
     *       <em>this window</em> reads with the simulation dead. A control of some other window
     *       answers a question nobody asked, and the excursion is a function of the beat.</li>
     *   <li><b>A different harness.</b> §11.2b(d): a comparison spanning two harness versions
     *       is void by default rather than by discovery. Overridable with
     *       {@code --allow-harness-drift}, because the rule is "void by default", not
     *       "impossible" — but it has to be said out loud on the command line.</li>
     * </ol>
     */
    private static void requireRigidControl(CaptureDir live, CaptureDir control,
                                            boolean allowHarnessDrift) {
        Map<String, String> cm = control.manifest();
        if (cm.isEmpty()) {
            throw new IllegalArgumentException(
                    "--control " + control.dir + " has no capture.txt, so there is no evidence it is "
                    + "a rigid control at all. STYLE.md 11.2b(e): the flag has to be checked, not "
                    + "merely written. Re-shoot it with `./gw capture -Pscene=... -Pclamp=cloth`.");
        }
        String clamp = cm.getOrDefault("clamp", "none");
        if (!"cloth".equals(clamp)) {
            throw new IllegalArgumentException(
                    "--control " + control.dir + " is not a rigid control: its capture.txt says "
                    + "clamp=" + clamp + " (clothRigid=" + cm.getOrDefault("clothRigid", "?") + "). "
                    + "Gate 3 asks what this box reads with the cloth welded to the hips; a live "
                    + "capture answers a different question and scoring it as a control is the "
                    + "defect STYLE.md 11.2b(e) names. Shoot it with -Pclamp=cloth.");
        }
        if (live == null) {
            return;
        }
        Map<String, String> lm = live.manifest();
        if (lm.isEmpty()) {
            throw new IllegalArgumentException(
                    "the live capture " + live.dir + " has no capture.txt, so its scene, start and "
                    + "step cannot be checked against the control's. A paired measurement whose "
                    + "pairing is unverifiable is an anecdote (STYLE.md 11.3). Re-shoot it; the "
                    + "harness writes the manifest.");
        }
        for (String key : new String[] {"scene", "start", "step", "frames", "size"}) {
            String l = lm.get(key);
            String r = cm.get(key);
            if (l != null && r != null && !numericallyEqual(l, r)) {
                throw new IllegalArgumentException(
                        "--control " + control.dir + " is a control of a different window: "
                        + key + "=" + r + " against the live capture's " + key + "=" + l + ". "
                        + "STYLE.md 7.1 requires the pair to be the same scene, the same start, "
                        + "the same step and the same harness commit.");
            }
        }
        // A missing field is a harness version too -- the oldest one, from before the field
        // existed -- so it compares unequal to a recorded one and equal to another missing one.
        // Treating "unrecorded" as "matches anything" would let exactly the comparison this
        // check exists to stop through: a pass-5 live capture against a pass-4 control.
        String lh = lm.getOrDefault("harness", "(unrecorded)");
        String rh = cm.getOrDefault("harness", "(unrecorded)");
        if (!allowHarnessDrift && !lh.equals(rh)) {
            throw new IllegalArgumentException(
                    "--control " + control.dir + " was shot through a different harness ("
                    + rh + " against " + lh + "; commits " + cm.getOrDefault("commit", "(unrecorded)")
                    + " and " + lm.getOrDefault("commit", "(unrecorded)")
                    + "). STYLE.md 11.2b(d) makes a comparison spanning two "
                    + "harness versions void by default rather than by discovery. Re-shoot the "
                    + "pair, or pass --allow-harness-drift and say so in the report.");
        }
    }

    /** {@code 0.9} and {@code 0.90} are the same start. Falls back to string equality. */
    private static boolean numericallyEqual(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        try {
            return Math.abs(Double.parseDouble(a) - Double.parseDouble(b)) < 1e-6;
        } catch (NumberFormatException e) {
            return false;
        }
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

        // Two passes: the anchor's principal axis is resolved first and every other region's
        // is signed against it. See Track#principalAxis and the note in track() above.
        Map<String, Track> tracks = new LinkedHashMap<>();
        List<Object> regionRows = (List<Object>) root.get("regions");
        double[] reference = null;
        for (Object o : regionRows) {
            Map<String, Object> r = (Map<String, Object>) o;
            if (anchor.equals(String.valueOf(r.get("name")))) {
                Track t = Track.fromPositions(anchor, rectOf(r), toArray((List<Object>) r.get("x")),
                        toArray((List<Object>) r.get("y")), axis, gate).smoothed(smooth);
                reference = new double[] {t.axisX, t.axisY};
            }
        }
        for (Object o : regionRows) {
            Map<String, Object> r = (Map<String, Object>) o;
            String name = String.valueOf(r.get("name"));
            double[] xs = toArray((List<Object>) r.get("x"));
            double[] ys = toArray((List<Object>) r.get("y"));
            tracks.put(name, Track.fromPositions(name, rectOf(r), xs, ys, axis, gate, reference).smoothed(smooth));
        }

        // Scene probes, if the scene implements dev.starfall.capture.SceneProbe. Tagged
        // "sim:" and never merged into the pixel rows, because the whole reason the probe
        // exists is that the two have been confused: a cloth lag read off a particle is a
        // statement about the solver, and STYLE.md grades the picture. Printing them in one
        // chain is what makes "the particle moved and the picture did not" a sentence with
        // evidence rather than a suspicion.
        boolean anyProbe = false;
        List<Object> probeRows = root.get("probes") instanceof List<?> pl ? (List<Object>) pl : List.of();
        for (Object o : probeRows) {
            Map<String, Object> r = (Map<String, Object>) o;
            String name = "sim:" + r.get("name");
            double[] xs = toArray((List<Object>) r.get("x"));
            double[] ys = toArray((List<Object>) r.get("y"));
            tracks.put(name, Track.fromPositions(name, pathBounds(xs, ys), xs, ys, axis, gate)
                    .smoothed(smooth));
            anyProbe = true;
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
        if (anyProbe) {
            out().println("  rows tagged 'sim:' are SceneProbe particle positions, not pixels. Their");
            out().println("  rectangle is the box the particle swept, not a box anything was measured in.");
        }
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
        int off = reach(root, probeRows);
        return off == 0 ? 0 : 1;
    }

    /**
     * STYLE.md §7.1's one surviving scalar gate, printed and returned.
     *
     * <blockquote>One scalar gate does survive, because its null is structural rather than
     * statistical: <b>every simulated particle whose swept box falls outside the drawn figure
     * contributes nothing to the picture and must not be counted as cloth resolution. It should
     * read zero.</b></blockquote>
     *
     * <p>"Outside the drawn figure" is measured as ink rather than as a bounding box, and the
     * distinction is the whole point: the figure's box is a rectangle over feet, blade and hair,
     * and a particle can sit well inside it while hanging in open paper beside the skirt — which
     * is what {@code back4} did. So a particle <b>paints</b> at a sample when the darkest pixel
     * within {@code paintRadius} of it is at or below the midpoint between the paper and
     * STYLE.md §2.2's ink floor {@code #161A22} (luminance 25.73).
     *
     * <p>The midpoint, not the 0.85×paper ink threshold, and that is deliberate. A wet halo
     * measures as "ink" at 0.85×paper while being a wash the eye reads as empty; a frayed hem is
     * sparse but its surviving flecks are <em>dark</em>. Half-way to the floor is the line that
     * separates a mark from a stain, and it therefore rewards fray and rejects halo — which is
     * the behaviour §3 wants from a hem in the first place.
     *
     * @return how many particles paint nothing. The gate is that this is zero.
     */
    @SuppressWarnings("unchecked")
    private static int reach(Map<String, Object> root, List<Object> probeRows) {
        List<Object> withPaint = new ArrayList<>();
        for (Object o : probeRows) {
            if (((Map<String, Object>) o).get("darkest") instanceof List<?> d && !d.isEmpty()) {
                withPaint.add(o);
            }
        }
        if (withPaint.isEmpty()) {
            return 0;
        }
        double paper = ((Number) root.getOrDefault("paper", Double.NaN)).doubleValue();
        if (Double.isNaN(paper) || paper <= 0) {
            return 0;
        }
        double midpoint = 0.5 * (paper + INK_FLOOR);
        out().println();
        out().printf("STYLE.md 7.1 reach gate  -- a particle paints when the darkest pixel within its%n"
                + "  paint radius is <= %.1f, the midpoint between paper %.1f and the #161A22 ink%n"
                + "  floor %.2f. Particles that paint nothing are not cloth resolution.%n",
                midpoint, paper, INK_FLOOR);
        int off = 0;
        int total = 0;
        for (Object o : withPaint) {
            Map<String, Object> r = (Map<String, Object>) o;
            String name = String.valueOf(r.get("name"));
            double[] xs = toArray((List<Object>) r.get("x"));
            double[] ys = toArray((List<Object>) r.get("y"));
            double[] dk = toArray((List<Object>) r.get("darkest"));
            int paints = 0;
            double best = Double.MAX_VALUE;
            for (double v : dk) {
                if (v <= midpoint) {
                    paints++;
                }
                best = Math.min(best, v);
            }
            // Only the simulated chains are graded. `hips` and `head` are bones, not cloth, and
            // the hair has its own section; the gate is about cloth resolution.
            boolean graded = name.startsWith("back") || name.startsWith("front") || name.startsWith("sleeve");
            if (graded) {
                total++;
            }
            double share = dk.length == 0 ? 0 : 100.0 * paints / dk.length;
            boolean fails = graded && paints == 0;
            if (fails) {
                off++;
            }
            out().printf("  %-10s %s  paints on %5.1f%% of samples, darkest %5.1f  %s%n",
                    name, pathBounds(xs, ys).describe(), share, best,
                    !graded ? "(not cloth)" : fails ? "<- PAINTS NOTHING" : "");
        }
        out().printf("  %d of %d simulated cloth particles paint nothing (gate: 0)  -> %s%n",
                off, total, off == 0 ? "PASS" : "FAIL");
        return off;
    }

    /** STYLE.md §2.2's ink floor {@code #161A22}, as Rec. 709 luminance. The value floor every capture holds. */
    private static final double INK_FLOOR =
            Frame.WR * 0x16 + Frame.WG * 0x1A + Frame.WB * 0x22;

    /**
     * The integer box a probe path swept. A particle has no measurement rectangle -- it is
     * a point -- and STYLE.md §11.3 requires every printed number to carry one, so this
     * prints the honest thing: where the point went, labelled as such.
     */
    private static Rect pathBounds(double[] xs, double[] ys) {
        double x0 = Double.MAX_VALUE;
        double y0 = Double.MAX_VALUE;
        double x1 = -Double.MAX_VALUE;
        double y1 = -Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            x0 = Math.min(x0, xs[i]);
            x1 = Math.max(x1, xs[i]);
            y0 = Math.min(y0, ys[i]);
            y1 = Math.max(y1, ys[i]);
        }
        if (xs.length == 0) {
            return new Rect(0, 0, 0, 0);
        }
        int ix = (int) Math.floor(x0);
        int iy = (int) Math.floor(y0);
        return new Rect(ix, iy, Math.max(1, (int) Math.ceil(x1) - ix), Math.max(1, (int) Math.ceil(y1) - iy));
    }

    @SuppressWarnings("unchecked")
    private static Rect rectOf(Map<String, Object> row) {
        List<Object> n = (List<Object>) row.get("rect");
        return new Rect(((Number) n.get(0)).intValue(), ((Number) n.get(1)).intValue(),
                ((Number) n.get(2)).intValue(), ((Number) n.get(3)).intValue());
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
