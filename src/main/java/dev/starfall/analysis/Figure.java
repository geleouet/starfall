package dev.starfall.analysis;

import java.util.ArrayDeque;

/**
 * The figure: the largest 4-connected component of ink in the frame, and its bounding box.
 *
 * <p>This exists so that STYLE.md §11.0's matched-scale comparison has a mechanical
 * definition of "figure height", and so that region rectangles can be authored as fractions
 * of the figure rather than as absolute pixels — the figure moves between captures, the
 * anatomy does not.
 *
 * <p>The largest-component rule is what separates the figure from the ground grass and the
 * fog band, both of which cross the ink threshold over wide areas. Verified against the
 * corpus: it returns 355 px on {@code s3-p1-reversal} and 358 px on {@code s3-p1-hair},
 * against the 357 px matched scale recorded in {@code docs/system3-debt.md}.
 */
public final class Figure {

    public final Rect bounds;
    /** Ink pixels belonging to the figure component. */
    public final int componentInk;
    /** Ink pixels anywhere inside {@link #bounds}, including detached flecks. */
    public final int boxInk;
    /** Summed ink weight (paper minus luminance) inside {@link #bounds}. */
    public final double boxInkMass;
    public final double threshold;

    private Figure(Rect bounds, int componentInk, int boxInk, double boxInkMass, double threshold) {
        this.bounds = bounds;
        this.componentInk = componentInk;
        this.boxInk = boxInk;
        this.boxInkMass = boxInkMass;
        this.threshold = threshold;
    }

    /**
     * A figure that is simply this box.
     *
     * <p>For the two-figure case, where "the figure" is one named body rather than
     * the largest component -- see {@code RegionSet.Which}. The ink counts are not
     * recomputed because nothing that resolves a region against a box reads them,
     * and inventing numbers here would be worse than leaving them zero.
     */
    public static Figure atBounds(Rect bounds) {
        return new Figure(bounds, 0, 0, 0, Double.NaN);
    }

    public int height() {
        return bounds.h;
    }

    public int width() {
        return bounds.w;
    }

    /**
     * @param factor ink threshold as a fraction of paper; 0.85 is the corpus default and the
     *               one the debt documents quote.
     */
    public static Figure detect(Frame f, Paper paper, double factor) {
        double th = paper.threshold(factor);
        boolean[] ink = new boolean[f.width * f.height];
        for (int i = 0; i < ink.length; i++) {
            ink[i] = f.lum[i] < th;
        }
        boolean[] seen = new boolean[ink.length];
        int bestN = 0;
        int bx0 = 0, by0 = 0, bx1 = 0, by1 = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int start = 0; start < ink.length; start++) {
            if (!ink[start] || seen[start]) {
                continue;
            }
            q.clear();
            q.add(start);
            seen[start] = true;
            int n = 0;
            int x0 = start % f.width, x1 = x0, y0 = start / f.width, y1 = y0;
            while (!q.isEmpty()) {
                int p = q.poll();
                n++;
                int x = p % f.width, y = p / f.width;
                if (x < x0) x0 = x;
                if (x > x1) x1 = x;
                if (y < y0) y0 = y;
                if (y > y1) y1 = y;
                if (x > 0 && ink[p - 1] && !seen[p - 1]) { seen[p - 1] = true; q.add(p - 1); }
                if (x < f.width - 1 && ink[p + 1] && !seen[p + 1]) { seen[p + 1] = true; q.add(p + 1); }
                if (y > 0 && ink[p - f.width] && !seen[p - f.width]) { seen[p - f.width] = true; q.add(p - f.width); }
                if (y < f.height - 1 && ink[p + f.width] && !seen[p + f.width]) { seen[p + f.width] = true; q.add(p + f.width); }
            }
            if (n > bestN) {
                bestN = n;
                bx0 = x0; by0 = y0; bx1 = x1; by1 = y1;
            }
        }
        if (bestN == 0) {
            return new Figure(new Rect(0, 0, 0, 0), 0, 0, 0, th);
        }
        Rect box = Rect.ofCorners(bx0, by0, bx1, by1);
        int boxInk = 0;
        double mass = 0;
        for (int y = box.y; y <= box.y1(); y++) {
            for (int x = box.x; x <= box.x1(); x++) {
                int i = y * f.width + x;
                if (ink[i]) {
                    boxInk++;
                    mass += paper.level - f.lum[i];
                }
            }
        }
        return new Figure(box, bestN, boxInk, mass, th);
    }

    @Override
    public String toString() {
        return String.format("figure %s  height=%d px  component ink=%d px  box ink=%d px",
                bounds.describe(), bounds.h, componentInk, boxInk);
    }
}
