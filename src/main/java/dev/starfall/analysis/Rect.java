package dev.starfall.analysis;

/**
 * An axis-aligned pixel rectangle in image coordinates (origin top-left, +y down).
 *
 * <p>Immutable, and printed back out with every measurement taken through it. That is
 * deliberate: {@code docs/system3-debt.md} records "hair-region coverage 23%" without saying
 * what the hair region was, and boxes exist in that capture giving 23% and boxes giving 42%.
 * A number reported with its rectangle is checkable; one without is not.
 */
public final class Rect {

    public final int x, y, w, h;

    public Rect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0, w);
        this.h = Math.max(0, h);
    }

    public static Rect ofCorners(int x0, int y0, int x1, int y1) {
        int lx = Math.min(x0, x1), ly = Math.min(y0, y1);
        return new Rect(lx, ly, Math.abs(x1 - x0) + 1, Math.abs(y1 - y0) + 1);
    }

    public int x1() {
        return x + w - 1;
    }

    public int y1() {
        return y + h - 1;
    }

    public int area() {
        return w * h;
    }

    public boolean isEmpty() {
        return w <= 0 || h <= 0;
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /** Clips to the frame. Regions may legitimately be authored slightly off-frame. */
    public Rect clamp(Frame f) {
        return clamp(f.width, f.height);
    }

    public Rect clamp(int width, int height) {
        int nx = Math.max(0, x), ny = Math.max(0, y);
        int nx1 = Math.min(width, x + w), ny1 = Math.min(height, y + h);
        return new Rect(nx, ny, nx1 - nx, ny1 - ny);
    }

    public Rect grow(int by) {
        return new Rect(x - by, y - by, w + 2 * by, h + 2 * by);
    }

    /** Sub-rectangle from fractional coordinates of this rectangle. Fractions may fall outside 0..1. */
    public Rect fraction(double fx, double fy, double fw, double fh) {
        int nx = (int) Math.round(x + fx * w);
        int ny = (int) Math.round(y + fy * h);
        int nw = (int) Math.round(fw * w);
        int nh = (int) Math.round(fh * h);
        return new Rect(nx, ny, nw, nh);
    }

    /**
     * Parses {@code x,y,w,h}. Whitespace tolerated.
     */
    public static Rect parse(String s) {
        String[] parts = s.trim().split("\\s*,\\s*");
        if (parts.length != 4) {
            throw new IllegalArgumentException("expected x,y,w,h but got '" + s + "'");
        }
        return new Rect(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    @Override
    public String toString() {
        return x + "," + y + "," + w + "," + h;
    }

    /** Human form used in reports: {@code x420..549 y108..217 (130x110)}. */
    public String describe() {
        return "x" + x + ".." + x1() + " y" + y + ".." + y1() + " (" + w + "x" + h + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rect r)) {
            return false;
        }
        return r.x == x && r.y == y && r.w == w && r.h == h;
    }

    @Override
    public int hashCode() {
        return ((x * 31 + y) * 31 + w) * 31 + h;
    }
}
