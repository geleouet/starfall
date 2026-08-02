package dev.starfall.analysis;

/**
 * Ink coverage inside a rectangle, at a threshold relative to paper, plus that rectangle's
 * share of the figure's ink.
 *
 * <p>This is the measurement that decided System 1's contrast question ("the fix is coverage
 * and mass, not value") and System 3's hair verdict ("23% against the reference's 60%").
 * Both are reported here, because they answer different questions: coverage says how solid
 * the mark is, share says how much of the picture it is.
 *
 * <p>Two shares are reported. {@code shareByCount} counts ink pixels, which is what the debt
 * documents quote. {@code shareByMass} weights each pixel by how far below paper it sits,
 * which does not change when a halo drifts one level across the threshold — for a material
 * built on wet bleed that is the more stable number, and the two diverging is itself a
 * signal that the region is mostly halo.
 */
public record Coverage(
        String region,
        Rect rect,
        double factor,
        double threshold,
        double paper,
        int inkPixels,
        int area,
        double inkMass,
        double shareByCount,
        double shareByMass,
        double meanInkLuminance,
        double medianInkLuminance) {

    public double fraction() {
        return area == 0 ? 0 : inkPixels / (double) area;
    }

    public double percent() {
        return 100.0 * fraction();
    }

    public static Coverage measure(Frame f, Paper paper, Rect region, double factor, Figure figure) {
        Rect r = region.clamp(f);
        double th = paper.threshold(factor);
        int ink = 0;
        double mass = 0;
        double[] lums = new double[Math.max(1, r.area())];
        for (int y = r.y; y <= r.y1(); y++) {
            for (int x = r.x; x <= r.x1(); x++) {
                double l = f.lum[y * f.width + x];
                if (l < th) {
                    lums[ink] = l;
                    ink++;
                    mass += paper.level - l;
                }
            }
        }
        double mean = 0, median = 0;
        if (ink > 0) {
            double sum = 0;
            for (int i = 0; i < ink; i++) {
                sum += lums[i];
            }
            mean = sum / ink;
            double[] sorted = new double[ink];
            System.arraycopy(lums, 0, sorted, 0, ink);
            java.util.Arrays.sort(sorted);
            median = sorted[ink / 2];
        }
        double byCount = 0, byMass = 0;
        if (figure != null && figure.boxInk > 0) {
            byCount = ink / (double) figure.boxInk;
            byMass = figure.boxInkMass > 0 ? mass / figure.boxInkMass : 0;
        }
        return new Coverage(region == null ? "region" : "region", r, factor, th, paper.level,
                ink, r.area(), mass, byCount, byMass, mean, median);
    }

    public static Coverage measure(Frame f, Paper paper, String name, Rect region,
                                   double factor, Figure figure) {
        Coverage c = measure(f, paper, region, factor, figure);
        return new Coverage(name, c.rect, c.factor, c.threshold, c.paper, c.inkPixels, c.area,
                c.inkMass, c.shareByCount, c.shareByMass, c.meanInkLuminance, c.medianInkLuminance);
    }

    public String describe() {
        return String.format(
                "%-14s %s  coverage %5.1f%% @%.2fxpaper (th %.0f)  ink %d/%d px"
                        + "  share of figure ink %5.2f%% by count, %5.2f%% by mass"
                        + "  ink luminance mean %.1f median %.1f",
                region, rect.describe(), percent(), factor, threshold, inkPixels, area,
                100 * shareByCount, 100 * shareByMass, meanInkLuminance, medianInkLuminance);
    }
}
