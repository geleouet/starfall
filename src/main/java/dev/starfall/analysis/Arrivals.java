package dev.starfall.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The chain of arrivals: when each tracked region reverses, measured <b>against a named
 * anchor</b>.
 *
 * <p>The anchor is a constructor argument with no default, and that is the whole point of
 * this class. STYLE.md §7.1:
 *
 * <blockquote>A hem trails the <i>hips</i>; a sleeve trails the <i>wrist</i>, which is itself
 * already far behind the hips because it hangs off an IK chain carrying its own settle. Both
 * readings are defensible and they differ by a factor of three, so a lag figure quoted without
 * its anchor is unfalsifiable. State it every time.</blockquote>
 *
 * <p>So there is no {@code lag()} that takes no argument, and the CLI refuses to run a track
 * comparison without {@code --anchor}. A tool that made the anchor optional would make the
 * unfalsifiable version the easy one to produce.
 *
 * <p>Which reversal is compared is also explicit. {@link Selector#DOMINANT} is the default,
 * because comparing first reversals lets a hair bundle wobbling in an ambient breeze appear
 * to lead the body — precisely the driver-versus-resonance trap STYLE.md §7.2 warns about,
 * and the one that contaminated System 3's "hair streaming 24 px ahead of the head" claim.
 */
public final class Arrivals {

    public enum Selector {
        /** The reversal with the largest velocity swing around it. */
        DOMINANT,
        /** The first reversal above the noise gate. */
        FIRST,
        /** The n-th reversal, set through {@link #ordinal}. */
        ORDINAL
    }

    public record Lag(String region, String anchor,
                      double regionFrame, double anchorFrame,
                      double lagFrames, double lagSeconds, double swing) {
    }

    public final String anchor;
    public final double fps;
    public final Selector selector;
    public final int ordinal;
    public final boolean matchDirection;
    public final Map<String, Track> tracks;
    public final List<Lag> lags;

    public Arrivals(String anchor, Map<String, Track> tracks, double fps) {
        this(anchor, tracks, fps, Selector.DOMINANT, 0, true);
    }

    public Arrivals(String anchor, Map<String, Track> tracks, double fps, Selector selector, int ordinal) {
        this(anchor, tracks, fps, selector, ordinal, true);
    }

    public Arrivals(String anchor, Map<String, Track> tracks, double fps, Selector selector,
                    int ordinal, boolean matchDirection) {
        if (anchor == null || anchor.isBlank()) {
            throw new IllegalArgumentException(
                    "an arrival chain needs a named anchor: a hem trails the hips, a sleeve trails "
                    + "the wrist, and those differ by a factor of three (STYLE.md 7.1)");
        }
        if (!tracks.containsKey(anchor)) {
            throw new IllegalArgumentException("anchor region '" + anchor + "' is not one of the tracked regions "
                    + tracks.keySet());
        }
        this.anchor = anchor;
        this.fps = fps;
        this.selector = selector;
        this.ordinal = ordinal;
        this.matchDirection = matchDirection;
        this.tracks = new LinkedHashMap<>(tracks);
        this.lags = computeLags();
    }

    /**
     * The reversal this chain compares, for one region; null when the region never turns that
     * way inside the window.
     *
     * <p>Candidates are restricted to reversals turning the <b>same way as the anchor's</b>.
     * Without that restriction a chain mixes the turn into a knockback with the turn out of
     * it, and the sleeve appears to lead the hips by four frames when it in fact trails them
     * by eleven — which is the same measurement error as quoting a lag without its anchor,
     * one step further in.
     */
    public Track.Crossing selected(String region) {
        Track t = tracks.get(region);
        if (t == null) {
            return null;
        }
        List<Track.Crossing> cs = t.crossings();
        if (!region.equals(anchor) && matchDirection) {
            Track.Crossing a = anchorCrossing();
            if (a != null) {
                cs = cs.stream().filter(x -> x.fromSign() == a.fromSign()).toList();
            }
        }
        if (cs.isEmpty()) {
            return null;
        }
        return switch (selector) {
            case DOMINANT -> cs.stream().max((p, q) -> Double.compare(p.swing(), q.swing())).orElse(null);
            case FIRST -> cs.get(0);
            case ORDINAL -> ordinal < cs.size() ? cs.get(ordinal) : null;
        };
    }

    private Track.Crossing anchorCrossing() {
        Track t = tracks.get(anchor);
        List<Track.Crossing> cs = t.crossings();
        if (cs.isEmpty()) {
            return null;
        }
        return switch (selector) {
            case DOMINANT -> t.dominantCrossing();
            case FIRST -> cs.get(0);
            case ORDINAL -> ordinal < cs.size() ? cs.get(ordinal) : null;
        };
    }

    private List<Lag> computeLags() {
        Track.Crossing a = selected(anchor);
        List<Lag> out = new ArrayList<>();
        for (String name : tracks.keySet()) {
            if (name.equals(anchor)) {
                continue;
            }
            Track.Crossing c = selected(name);
            if (c == null || a == null) {
                out.add(new Lag(name, anchor, c == null ? Double.NaN : c.frame(),
                        a == null ? Double.NaN : a.frame(), Double.NaN, Double.NaN,
                        c == null ? Double.NaN : c.swing()));
                continue;
            }
            double lag = c.frame() - a.frame();
            out.add(new Lag(name, anchor, c.frame(), a.frame(), lag,
                    fps > 0 ? lag / fps : Double.NaN, c.swing()));
        }
        return out;
    }

    /** Regions ordered by their selected reversal — the chain, read down the body. */
    public List<String> chain() {
        List<String> names = new ArrayList<>(tracks.keySet());
        names.sort((a, b) -> Double.compare(at(a), at(b)));
        return names;
    }

    private double at(String name) {
        Track.Crossing c = selected(name);
        return c == null ? Double.POSITIVE_INFINITY : c.frame();
    }

    /** Spread between the earliest and latest selected reversal, in frames. */
    public double spreadFrames() {
        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
        for (String n : tracks.keySet()) {
            double f = at(n);
            if (Double.isInfinite(f)) {
                continue;
            }
            lo = Math.min(lo, f);
            hi = Math.max(hi, f);
        }
        return Double.isInfinite(lo) || Double.isInfinite(hi) ? Double.NaN : hi - lo;
    }

    /**
     * STYLE.md §10's last row fails a pass on sight for everything peaking on the same frame.
     * True when every region's reversal lands within {@code tolerance} frames of every other.
     */
    public boolean everythingArrivesTogether(double tolerance) {
        double s = spreadFrames();
        return !Double.isNaN(s) && s <= tolerance;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("arrival chain, anchored on '").append(anchor).append("', comparing the ")
                .append(selector == Selector.ORDINAL ? "reversal #" + ordinal : selector.name().toLowerCase())
                .append(" reversal")
                .append(matchDirection ? " turning the same way as the anchor" : " in any direction");
        if (fps > 0) {
            sb.append(String.format(" at %.1f fps", fps));
        }
        sb.append("\n");
        for (String n : chain()) {
            Track.Crossing c = selected(n);
            sb.append(String.format("  %-14s %s%s%n", n,
                    c == null ? "no reversal in window"
                            : String.format("reverses at frame %6.2f (swing %.2f px/frame)", c.frame(), c.swing()),
                    n.equals(anchor) ? "   <-- anchor" : ""));
        }
        for (Lag l : lags) {
            sb.append(String.format("  %-14s trails %-10s by %+7.2f frames%s%n",
                    l.region(), l.anchor(), l.lagFrames(),
                    Double.isNaN(l.lagSeconds()) ? "" : String.format(" (%+.3f s)", l.lagSeconds())));
        }
        sb.append(String.format("  spread across the chain: %.2f frames%n", spreadFrames()));
        if (everythingArrivesTogether(1.0)) {
            sb.append("  FAIL STYLE.md 10: every tracked region reverses within one frame — "
                    + "nothing may arrive at the same time\n");
        }
        return sb.toString();
    }
}
