package dev.starfall.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Mean ink luminance in horizontal bands from the top of a region to its bottom.
 *
 * <p>This is the measurement that proved System 1's ink gravity was inverted: STYLE.md's
 * references put their darkest passage at the knees, and the capture measured 65 at the
 * chest rising to 146 at the hem — the figure ending in erasure rather than in ink smoke.
 * It is also the measurement that found the cause was the grass overlay rather than the
 * shader, once the bands were taken with and without it.
 *
 * <p>Only ink pixels contribute to the mean. Including paper would make the profile a
 * coverage measurement wearing a luminance costume: a band that is 20% covered in pure black
 * and a band that is 80% covered in mid grey would read the same.
 */
public final class BandProfile {

    public record Band(int index, Rect rect, int inkPixels, double coverage,
                       double meanInkLuminance, double medianInkLuminance, double inkMass) {
    }

    public final Rect rect;
    public final double factor;
    public final double threshold;
    public final List<Band> bands;

    private BandProfile(Rect rect, double factor, double threshold, List<Band> bands) {
        this.rect = rect;
        this.factor = factor;
        this.threshold = threshold;
        this.bands = List.copyOf(bands);
    }

    public static BandProfile measure(Frame f, Paper paper, Rect region, int bandCount, double factor) {
        Rect r = region.clamp(f);
        double th = paper.threshold(factor);
        int n = Math.max(1, bandCount);
        List<Band> out = new ArrayList<>(n);
        for (int b = 0; b < n; b++) {
            int y0 = r.y + (int) Math.round(b * r.h / (double) n);
            int y1 = r.y + (int) Math.round((b + 1) * r.h / (double) n) - 1;
            Rect br = Rect.ofCorners(r.x, y0, r.x1(), Math.max(y0, y1));
            int ink = 0;
            double sum = 0, mass = 0;
            double[] lums = new double[Math.max(1, br.area())];
            for (int y = br.y; y <= br.y1(); y++) {
                for (int x = br.x; x <= br.x1(); x++) {
                    double l = f.lum[y * f.width + x];
                    if (l < th) {
                        lums[ink++] = l;
                        sum += l;
                        mass += paper.level - l;
                    }
                }
            }
            double mean = ink == 0 ? Double.NaN : sum / ink;
            double median = Double.NaN;
            if (ink > 0) {
                double[] sorted = new double[ink];
                System.arraycopy(lums, 0, sorted, 0, ink);
                java.util.Arrays.sort(sorted);
                median = sorted[ink / 2];
            }
            out.add(new Band(b, br, ink, br.area() == 0 ? 0 : ink / (double) br.area(),
                    mean, median, mass));
        }
        return new BandProfile(r, factor, th, out);
    }

    /**
     * Positive when ink gets darker further down — the direction STYLE.md's references have.
     * Negative means the figure bleaches towards its hem, which is System 1 debt item D5.
     */
    public double gravity() {
        List<Band> valid = bands.stream().filter(b -> !Double.isNaN(b.meanInkLuminance())).toList();
        if (valid.size() < 2) {
            return 0;
        }
        return valid.get(0).meanInkLuminance() - valid.get(valid.size() - 1).meanInkLuminance();
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("band profile over %s, %d bands, ink < %.2fxpaper (%.0f)%n",
                rect.describe(), bands.size(), factor, threshold));
        for (Band b : bands) {
            sb.append(String.format("  band %d  y%d..%d  ink %6d px (%5.1f%%)  mean %6.1f  median %6.1f%n",
                    b.index(), b.rect().y, b.rect().y1(), b.inkPixels(), 100 * b.coverage(),
                    b.meanInkLuminance(), b.medianInkLuminance()));
        }
        sb.append(String.format("  gravity (top mean - bottom mean) = %+.1f  [positive = darkens downward, "
                + "which is the reference direction]", gravity()));
        return sb.toString();
    }
}
