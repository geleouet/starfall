package dev.starfall.analysis;

/**
 * Ink-weighted centroid of a rectangle.
 *
 * <p>Each ink pixel contributes its distance below paper, so a strand that fades rather than
 * moves does not read as a translation. Pixels above the ink threshold contribute nothing at
 * all, which keeps the fog band and the paper tooth out of the result.
 *
 * <p>Centroid tracking answers "where is this part of the figure", which is the right
 * question for a soft mass like a hair bundle or a hem. For a rigid mark that keeps its
 * shape, {@link Registration} is sharper — it finds the displacement that best explains the
 * whole patch rather than the shift of its mean.
 */
public record Centroid(Rect rect, double x, double y, double mass, int inkPixels) {

    public boolean isEmpty() {
        return inkPixels == 0;
    }

    public static Centroid measure(Frame f, Paper paper, Rect region, double factor) {
        Rect r = region.clamp(f);
        double th = paper.threshold(factor);
        double sx = 0, sy = 0, sw = 0;
        int n = 0;
        for (int y = r.y; y <= r.y1(); y++) {
            for (int x = r.x; x <= r.x1(); x++) {
                double l = f.lum[y * f.width + x];
                if (l < th) {
                    double w = paper.level - l;
                    sx += x * w;
                    sy += y * w;
                    sw += w;
                    n++;
                }
            }
        }
        if (sw <= 0) {
            return new Centroid(r, Double.NaN, Double.NaN, 0, 0);
        }
        return new Centroid(r, sx / sw, sy / sw, sw, n);
    }

    public String describe() {
        return String.format("centroid (%.2f, %.2f) mass %.0f over %d ink px in %s",
                x, y, mass, inkPixels, rect.describe());
    }
}
