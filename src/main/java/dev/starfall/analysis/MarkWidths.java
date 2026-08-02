package dev.starfall.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Mark widths on cuts through a region: run lengths of continuous ink, and whether their
 * distribution is bimodal.
 *
 * <p>This measurement is the single named cause of System 3's failure. At matched scale the
 * reference's hair is bimodal — a 30-55 px near-opaque mass plus 1-2 px hairlines, with
 * nothing in between — and the capture's was unimodal at 5-11 px, "the one register that
 * reads as neither mass nor wisp". Nothing about coverage, value or edge hardness would have
 * found that; only the width distribution did.
 *
 * <p>The bimodality statistic is Sarle's coefficient, {@code (skew^2 + 1) / kurtosis}, which
 * exceeds 5/9 for a distribution flatter than uniform and is the standard screen for two
 * modes. It is reported alongside the raw runs, because with a handful of cuts the raw runs
 * are the honest evidence and the statistic is a summary of very little data.
 */
public final class MarkWidths {

    /** Sarle's coefficient above this suggests two modes rather than one. */
    public static final double BIMODAL_LIMIT = 5.0 / 9.0;

    public record Cut(int position, List<Integer> runs) {
    }

    public final Rect rect;
    public final boolean vertical;
    public final double factor;
    public final double threshold;
    public final List<Cut> cuts;
    public final List<Integer> allRuns;

    private MarkWidths(Rect rect, boolean vertical, double factor, double threshold, List<Cut> cuts) {
        this.rect = rect;
        this.vertical = vertical;
        this.factor = factor;
        this.threshold = threshold;
        this.cuts = List.copyOf(cuts);
        List<Integer> all = new ArrayList<>();
        cuts.forEach(c -> all.addAll(c.runs()));
        this.allRuns = List.copyOf(all);
    }

    /**
     * @param vertical true for vertical cuts (columns), which is what the hair comparison used
     * @param cutCount number of evenly spaced cuts across the region
     */
    public static MarkWidths measure(Frame f, Paper paper, Rect region, boolean vertical,
                                     int cutCount, double factor) {
        Rect r = region.clamp(f);
        double th = paper.threshold(factor);
        int n = Math.max(1, cutCount);
        int span = vertical ? r.w : r.h;
        List<Cut> cuts = new ArrayList<>(n);
        for (int c = 0; c < n; c++) {
            int at = (int) Math.round((c + 0.5) * span / (double) n);
            List<Integer> runs = new ArrayList<>();
            int run = 0;
            int steps = vertical ? r.h : r.w;
            for (int i = 0; i < steps; i++) {
                int x = vertical ? r.x + Math.min(span - 1, at) : r.x + i;
                int y = vertical ? r.y + i : r.y + Math.min(span - 1, at);
                boolean ink = f.inside(x, y) && f.lum[y * f.width + x] < th;
                if (ink) {
                    run++;
                } else if (run > 0) {
                    runs.add(run);
                    run = 0;
                }
            }
            if (run > 0) {
                runs.add(run);
            }
            cuts.add(new Cut(vertical ? r.x + Math.min(span - 1, at) : r.y + Math.min(span - 1, at), runs));
        }
        return new MarkWidths(r, vertical, factor, th, cuts);
    }

    /** Sarle's bimodality coefficient over all runs, or NaN with fewer than four runs. */
    public double bimodality() {
        int n = allRuns.size();
        if (n < 4) {
            return Double.NaN;
        }
        double mean = allRuns.stream().mapToInt(Integer::intValue).average().orElse(0);
        double m2 = 0, m3 = 0, m4 = 0;
        for (int v : allRuns) {
            double d = v - mean;
            m2 += d * d;
            m3 += d * d * d;
            m4 += d * d * d * d;
        }
        m2 /= n;
        m3 /= n;
        m4 /= n;
        if (m2 <= 1e-12) {
            return Double.NaN;
        }
        double skew = m3 / Math.pow(m2, 1.5);
        double kurt = m4 / (m2 * m2);
        return (skew * skew + 1) / kurt;
    }

    public boolean bimodal() {
        double b = bimodality();
        return !Double.isNaN(b) && b > BIMODAL_LIMIT;
    }

    /** Histogram in octave-ish buckets: 1-2, 3-4, 5-8, 9-16, 17-32, 33-64, 65+. */
    public int[] histogram() {
        int[] bins = new int[7];
        for (int v : allRuns) {
            if (v <= 2) bins[0]++;
            else if (v <= 4) bins[1]++;
            else if (v <= 8) bins[2]++;
            else if (v <= 16) bins[3]++;
            else if (v <= 32) bins[4]++;
            else if (v <= 64) bins[5]++;
            else bins[6]++;
        }
        return bins;
    }

    private static final String[] BIN_LABELS = {"1-2", "3-4", "5-8", "9-16", "17-32", "33-64", "65+"};

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("mark widths on %d %s cuts through %s, ink < %.2fxpaper (%.0f)%n",
                cuts.size(), vertical ? "vertical" : "horizontal", rect.describe(), factor, threshold));
        for (Cut c : cuts) {
            sb.append(String.format("  cut at %s=%d : %s%n", vertical ? "x" : "y", c.position(),
                    c.runs().isEmpty() ? "(no ink)" : c.runs().toString()));
        }
        int[] h = histogram();
        sb.append("  histogram ");
        for (int i = 0; i < h.length; i++) {
            sb.append(String.format("%s:%d  ", BIN_LABELS[i], h[i]));
        }
        double b = bimodality();
        sb.append(String.format("%n  %d runs, bimodality %.3f -> %s (limit %.3f)%n",
                allRuns.size(), b, bimodal() ? "bimodal" : "UNIMODAL", BIMODAL_LIMIT));
        return sb.toString();
    }
}
