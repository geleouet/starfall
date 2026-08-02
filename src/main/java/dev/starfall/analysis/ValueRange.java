package dev.starfall.analysis;

import java.util.Arrays;

/**
 * The value floor and ceiling check: STYLE.md §2.2's "no pure black, no pure white, ever.
 * Darkest is {@code #161A22}; brightest non-emissive is the paper ground."
 *
 * <p>"Non-emissive" cannot be decided from a pixel, so the ceiling is reported twice: the
 * absolute maximum, which the blade and the clash bloom are allowed to own, and the 99.5th
 * percentile, which they are too small to move. If those two are far apart the frame has a
 * small bright emissive mark, which is correct; if the percentile itself sits above paper the
 * frame has a large one, which is not.
 *
 * <p>The floor is checked against {@code #161A22}'s luminance of 25.73 rather than against
 * its channels, because a pixel can be darker in one channel and lighter in another without
 * breaking the rule.
 */
public final class ValueRange {

    /** STYLE.md §2.2's darkest permitted ink, #161A22. */
    public static final int FLOOR_RGB = 0x161A22;
    public static final double FLOOR_LUM =
            Frame.WR * 0x16 + Frame.WG * 0x1A + Frame.WB * 0x22;

    public final double minLuminance;
    public final int minRgb;
    public final double p005Luminance;
    public final double p995Luminance;
    public final double maxLuminance;
    public final int maxRgb;
    public final double paper;
    public final boolean pureBlack;
    public final boolean pureWhite;

    private ValueRange(double minLuminance, int minRgb, double p005, double p995,
                       double maxLuminance, int maxRgb, double paper,
                       boolean pureBlack, boolean pureWhite) {
        this.minLuminance = minLuminance;
        this.minRgb = minRgb;
        this.p005Luminance = p005;
        this.p995Luminance = p995;
        this.maxLuminance = maxLuminance;
        this.maxRgb = maxRgb;
        this.paper = paper;
        this.pureBlack = pureBlack;
        this.pureWhite = pureWhite;
    }

    public static ValueRange measure(Frame f, Paper paper) {
        return measure(f, paper, f.bounds());
    }

    public static ValueRange measure(Frame f, Paper paper, Rect region) {
        Rect r = region.clamp(f);
        double[] lums = new double[r.area()];
        int n = 0;
        double lo = Double.MAX_VALUE, hi = -1;
        int loRgb = 0, hiRgb = 0;
        boolean black = false, white = false;
        for (int y = r.y; y <= r.y1(); y++) {
            for (int x = r.x; x <= r.x1(); x++) {
                int i = y * f.width + x;
                double l = f.lum[i];
                lums[n++] = l;
                if (l < lo) {
                    lo = l;
                    loRgb = f.rgb[i];
                }
                if (l > hi) {
                    hi = l;
                    hiRgb = f.rgb[i];
                }
                if (f.rgb[i] == 0x000000) {
                    black = true;
                }
                if (f.rgb[i] == 0xFFFFFF) {
                    white = true;
                }
            }
        }
        double[] sorted = Arrays.copyOf(lums, n);
        Arrays.sort(sorted);
        double p005 = n == 0 ? 0 : sorted[(int) Math.min(n - 1, Math.round(0.005 * (n - 1)))];
        double p995 = n == 0 ? 0 : sorted[(int) Math.min(n - 1, Math.round(0.995 * (n - 1)))];
        return new ValueRange(lo, loRgb, p005, p995, hi, hiRgb, paper.level, black, white);
    }

    public boolean floorRespected() {
        return minLuminance >= FLOOR_LUM - 0.5;
    }

    /** True when the bulk of the frame stays at or below paper, per §2.2. */
    public boolean ceilingRespected() {
        return p995Luminance <= paper + 2.0;
    }

    private static String hex(int rgb) {
        return String.format("#%06X (%d,%d,%d)", rgb, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("value range against STYLE.md 2.2 (floor #161A22 = luminance %.2f, ceiling = paper %.1f)%n",
                FLOOR_LUM, paper));
        sb.append(String.format("  darkest pixel   luminance %6.2f  %s  -> %s%n",
                minLuminance, hex(minRgb),
                floorRespected() ? "floor respected" : "BELOW THE #161A22 FLOOR"));
        sb.append(String.format("  darkest 0.5%%    luminance %6.2f%n", p005Luminance));
        sb.append(String.format("  brightest 0.5%%  luminance %6.2f  -> %s%n",
                p995Luminance,
                ceilingRespected() ? "non-emissive bulk stays at or under paper" : "BRIGHTER THAN PAPER"));
        sb.append(String.format("  brightest pixel luminance %6.2f  %s   (emissive marks may own this)%n",
                maxLuminance, hex(maxRgb)));
        if (pureBlack) {
            sb.append("  FAIL: a pure black pixel is present\n");
        }
        if (pureWhite) {
            sb.append("  NOTE: a pure white pixel is present — only the clash bloom and blade specular may approach it\n");
        }
        return sb.toString();
    }
}
