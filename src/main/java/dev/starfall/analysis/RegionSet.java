package dev.starfall.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Named rectangles, authored in one of three coordinate spaces, resolved against a frame.
 *
 * <p>Regions are the reason this package exists in the form it does. Every pixel number in
 * {@code docs/system1-debt.md}, {@code system2-debt.md} and {@code system3-debt.md} was taken
 * through a rectangle that the document does not record, so none of them can be reproduced or
 * refuted — a box exists in {@code s3-p1-reversal} giving the recorded 23% hair coverage, and
 * another giving 42%. That is the same failure STYLE.md §7.1 already named for lag anchors,
 * one level down.
 *
 * <p>The fix is that a region is a named, checked-in, printable object, and every measurement
 * this package emits carries the resolved rectangle beside the number.
 *
 * <p>Spaces:
 * <ul>
 *   <li>{@code PIXEL} — absolute image pixels. Exact, but only valid for one framing.</li>
 *   <li>{@code IMAGE} — fractions of the frame. Survives a resolution change.</li>
 *   <li>{@code FIGURE} — fractions of the detected figure bounding box. Survives the figure
 *       moving or being captured at a different scale, which is what makes a region set
 *       reusable across captures and what STYLE.md §11.0's matched-scale comparison needs.
 *       Fractions outside 0..1 are legal and normal: hair leaves the body.</li>
 * </ul>
 */
public final class RegionSet {

    public enum Space { PIXEL, IMAGE, FIGURE }

    public record Def(String name, Space space, double a, double b, double c, double d) {

        public Rect resolve(Frame frame, Figure figure) {
            return switch (space) {
                case PIXEL -> new Rect((int) Math.round(a), (int) Math.round(b),
                        (int) Math.round(c), (int) Math.round(d));
                case IMAGE -> frame.bounds().fraction(a, b, c, d);
                case FIGURE -> {
                    if (figure == null || figure.bounds.isEmpty()) {
                        throw new IllegalStateException(
                                "region '" + name + "' is figure-relative but no figure was detected");
                    }
                    yield figure.bounds.fraction(a, b, c, d);
                }
            };
        }

        public String spec() {
            return switch (space) {
                case PIXEL -> String.format("%s=%d,%d,%d,%d", name,
                        (int) a, (int) b, (int) c, (int) d);
                case IMAGE -> String.format(java.util.Locale.ROOT, "%s=img:%.4f,%.4f,%.4f,%.4f", name, a, b, c, d);
                case FIGURE -> String.format(java.util.Locale.ROOT, "%s=fig:%.4f,%.4f,%.4f,%.4f", name, a, b, c, d);
            };
        }
    }

    private final Map<String, Def> defs = new LinkedHashMap<>();

    public RegionSet() {
    }

    public RegionSet add(Def def) {
        defs.put(def.name(), def);
        return this;
    }

    public RegionSet add(String name, Space space, double a, double b, double c, double d) {
        return add(new Def(name, space, a, b, c, d));
    }

    public RegionSet addAll(RegionSet other) {
        defs.putAll(other.defs);
        return this;
    }

    public boolean isEmpty() {
        return defs.isEmpty();
    }

    public java.util.Set<String> names() {
        return defs.keySet();
    }

    public Def def(String name) {
        return defs.get(name);
    }

    public Map<String, Rect> resolve(Frame frame, Figure figure) {
        Map<String, Rect> out = new LinkedHashMap<>();
        for (Def d : defs.values()) {
            out.put(d.name(), d.resolve(frame, figure));
        }
        return out;
    }

    /**
     * Parses one CLI region spec.
     * <pre>
     *   name=420,108,130,110      absolute pixels
     *   name=fig:0.35,0.01,0.22,0.21   fractions of the figure box
     *   name=img:0.44,0.20,0.14,0.20   fractions of the frame
     * </pre>
     */
    public static Def parseSpec(String spec) {
        int eq = spec.indexOf('=');
        if (eq <= 0) {
            throw new IllegalArgumentException("region spec must be name=... but got '" + spec + "'");
        }
        String name = spec.substring(0, eq).trim();
        String body = spec.substring(eq + 1).trim();
        Space space = Space.PIXEL;
        if (body.startsWith("fig:")) {
            space = Space.FIGURE;
            body = body.substring(4);
        } else if (body.startsWith("img:")) {
            space = Space.IMAGE;
            body = body.substring(4);
        }
        String[] p = body.split("\\s*,\\s*");
        if (p.length != 4) {
            throw new IllegalArgumentException("region '" + name + "' needs four numbers, got '" + body + "'");
        }
        return new Def(name, space,
                Double.parseDouble(p[0]), Double.parseDouble(p[1]),
                Double.parseDouble(p[2]), Double.parseDouble(p[3]));
    }

    /**
     * Loads a region file.
     * <pre>
     * {
     *   "space": "figure",
     *   "regions": {
     *     "hips":  [0.233, 0.431, 0.437, 0.099],
     *     "blade": {"space": "pixel", "rect": [620, 230, 130, 60]}
     *   }
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public static RegionSet load(File file) throws IOException {
        String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        Map<String, Object> root = Json.parseObject(text);
        Space defaultSpace = root.containsKey("space")
                ? Space.valueOf(String.valueOf(root.get("space")).trim().toUpperCase())
                : Space.FIGURE;
        Object regionsObj = root.get("regions");
        if (!(regionsObj instanceof Map)) {
            throw new IOException(file + ": expected a \"regions\" object");
        }
        RegionSet set = new RegionSet();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) regionsObj).entrySet()) {
            String name = e.getKey();
            Object v = e.getValue();
            Space space = defaultSpace;
            List<Object> nums;
            if (v instanceof Map<?, ?> m) {
                if (m.get("space") != null) {
                    space = Space.valueOf(String.valueOf(m.get("space")).trim().toUpperCase());
                }
                nums = (List<Object>) m.get("rect");
            } else {
                nums = (List<Object>) v;
            }
            if (nums == null || nums.size() != 4) {
                throw new IOException(file + ": region '" + name + "' needs four numbers");
            }
            set.add(new Def(name, space,
                    ((Number) nums.get(0)).doubleValue(), ((Number) nums.get(1)).doubleValue(),
                    ((Number) nums.get(2)).doubleValue(), ((Number) nums.get(3)).doubleValue()));
        }
        return set;
    }

    public String toJson() {
        Json.Writer w = new Json.Writer();
        w.beginObject();
        w.prop("space", "figure");
        w.prop("note", "Fractions of the figure bounding box (origin top-left, +y down). "
                + "Values outside 0..1 are legal: hair and blades leave the body box.");
        w.name("regions").beginObject();
        for (Def d : defs.values()) {
            w.name(d.name()).beginObject();
            w.prop("space", d.space().name().toLowerCase());
            w.name("rect").beginArray().value(d.a()).value(d.b()).value(d.c()).value(d.d()).endArray();
            w.endObject();
        }
        w.endObject();
        w.endObject();
        return w.toString();
    }

    /**
     * The default region set for the samurai figure as framed by the System 1-3 captures
     * (960x540, figure roughly 355 px tall).
     *
     * <p>Authored by inspection of {@code out/captures/s3-p1-reversal/frame_000.png} and
     * expressed as fractions of the detected figure box so it transfers to the other captures
     * in the corpus. <b>These are a starting point, not ground truth.</b> When the rig changes
     * shape they must be re-checked and re-committed; the point is that whatever a review used
     * is written down beside the numbers it produced.
     */
    public static RegionSet samurai() {
        RegionSet s = new RegionSet();
        s.add("figure",    Space.FIGURE,  0.000,  0.000, 1.000, 1.000);
        s.add("head",      Space.FIGURE,  0.548,  0.023, 0.223, 0.144);
        s.add("hair",      Space.FIGURE, -0.058, -0.011, 0.704, 0.301);
        s.add("hair-root", Space.FIGURE,  0.354,  0.008, 0.223, 0.211);
        s.add("hair-mid",  Space.FIGURE,  0.015,  0.037, 0.388, 0.211);
        s.add("shoulder",  Space.FIGURE,  0.112,  0.220, 0.850, 0.141);
        s.add("torso",     Space.FIGURE,  0.160,  0.220, 0.680, 0.225);
        s.add("hips",      Space.FIGURE,  0.233,  0.431, 0.437, 0.099);
        s.add("sleeve",    Space.FIGURE,  0.767,  0.431, 0.243, 0.141);
        s.add("hem",       Space.FIGURE,  0.184,  0.755, 0.607, 0.169);
        return s;
    }

    /** Looks for {@code regions.json} beside the frames, falling back to {@link #samurai()}. */
    public static RegionSet forCapture(File captureDir) throws IOException {
        File f = new File(captureDir, "regions.json");
        return f.isFile() ? load(f) : samurai();
    }

    public String describe(Frame frame, Figure figure) {
        StringBuilder sb = new StringBuilder();
        for (Def d : defs.values()) {
            sb.append(String.format("  %-12s %-6s %s -> %s%n", d.name(), d.space().name().toLowerCase(),
                    String.format(java.util.Locale.ROOT, "[%.3f %.3f %.3f %.3f]", d.a(), d.b(), d.c(), d.d()),
                    d.resolve(frame, figure).describe()));
        }
        return sb.toString();
    }
}
