package dev.starfall.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Difference between two frames, plus a file hash comparison — the regression check.
 *
 * <p>Two claims in the debt documents rest on this. "The bind pose is bit-identical to
 * {@code s1-p7-bind}, md5 {@code ce533e...} on both" was verified; "{@code ik-gesture}
 * unchanged" was not, because the diff was 76,287 pixels and no baseline existed from the
 * right side of an intervening pass. So this reports both: the hash, which answers "is it the
 * same file", and the pixel statistics, which answer "how different, and where".
 *
 * <p>The bounding box of changed pixels is included because a diff concentrated in one
 * quadrant and a diff spread over the frame mean different things — the first is a change to
 * one part, the second is usually a global material or lighting change.
 */
public final class FrameDiff {

    public final File a;
    public final File b;
    public final String md5a;
    public final String md5b;
    public final int width;
    public final int height;
    public final int changedPixels;
    public final int tolerance;
    public final int maxDelta;
    public final double meanAbsDelta;
    public final Rect changedBounds;

    private FrameDiff(File a, File b, String md5a, String md5b, int width, int height,
                      int changedPixels, int tolerance, int maxDelta, double meanAbsDelta,
                      Rect changedBounds) {
        this.a = a;
        this.b = b;
        this.md5a = md5a;
        this.md5b = md5b;
        this.width = width;
        this.height = height;
        this.changedPixels = changedPixels;
        this.tolerance = tolerance;
        this.maxDelta = maxDelta;
        this.meanAbsDelta = meanAbsDelta;
        this.changedBounds = changedBounds;
    }

    public boolean identicalBytes() {
        return md5a != null && md5a.equals(md5b);
    }

    public boolean identicalPixels() {
        return changedPixels == 0;
    }

    public static FrameDiff of(File fa, File fb, int tolerance) throws IOException {
        Frame x = Frame.load(fa);
        Frame y = Frame.load(fb);
        String ha = md5(fa);
        String hb = md5(fb);
        if (x.width != y.width || x.height != y.height) {
            throw new IOException("frames differ in size: " + x.width + "x" + x.height
                    + " vs " + y.width + "x" + y.height);
        }
        int changed = 0, max = 0;
        long sum = 0;
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = -1, y1 = -1;
        for (int i = 0; i < x.rgb.length; i++) {
            int d = Math.max(Math.abs(x.r(i) - y.r(i)),
                    Math.max(Math.abs(x.g(i) - y.g(i)), Math.abs(x.b(i) - y.b(i))));
            sum += d;
            if (d > max) {
                max = d;
            }
            if (d > tolerance) {
                changed++;
                int px = i % x.width, py = i / x.width;
                if (px < x0) x0 = px;
                if (px > x1) x1 = px;
                if (py < y0) y0 = py;
                if (py > y1) y1 = py;
            }
        }
        Rect bounds = x1 < 0 ? new Rect(0, 0, 0, 0) : Rect.ofCorners(x0, y0, x1, y1);
        return new FrameDiff(fa, fb, ha, hb, x.width, x.height, changed, tolerance, max,
                sum / (double) x.rgb.length, bounds);
    }

    public static String md5(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(Files.readAllBytes(f.toPath()));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("diff %s  vs  %s%n", a.getName(), b.getName()));
        sb.append(String.format("  md5 %s%n  md5 %s%n", md5a, md5b));
        if (identicalBytes()) {
            sb.append("  BIT-IDENTICAL\n");
            return sb.toString();
        }
        sb.append(String.format("  %d of %d pixels differ by more than %d (%.4f%%)%n",
                changedPixels, width * height, tolerance,
                100.0 * changedPixels / (width * height)));
        sb.append(String.format("  max channel delta %d, mean |delta| %.3f%n", maxDelta, meanAbsDelta));
        if (changedPixels > 0) {
            sb.append(String.format("  changed region %s%n", changedBounds.describe()));
        }
        return sb.toString();
    }
}
