package dev.starfall.analysis;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A single captured image, decoded once into packed RGB plus a precomputed luminance plane.
 *
 * <p>Coordinates are image coordinates throughout this package: origin top-left, +y down,
 * the same convention the PNGs are written in. Every measurement this package produces
 * reports the rectangle it was taken from, because the standing lesson from the System 1-3
 * reviews is that a pixel number without its window is unfalsifiable — the same failure
 * mode STYLE.md §7.1 already codified for lag anchors.
 *
 * <p>Luminance is Rec. 709. The choice barely matters against this palette (601 and a flat
 * channel mean land within two levels on every capture on disk) but it must be fixed, so
 * that two reviews measuring the same frame get the same number.
 */
public final class Frame {

    /** Rec. 709 luma weights. */
    public static final double WR = 0.2126, WG = 0.7152, WB = 0.0722;

    public final int width;
    public final int height;
    /** Packed 0xRRGGBB, row-major. */
    public final int[] rgb;
    /** Luminance 0-255, row-major. */
    public final double[] lum;
    /** Source file, or null when built in-memory (headless timing). */
    public final File source;

    public Frame(int width, int height, int[] rgb, File source) {
        if (rgb.length != width * height) {
            throw new IllegalArgumentException("pixel array is " + rgb.length
                    + " but " + width + "x" + height + " needs " + (width * height));
        }
        this.width = width;
        this.height = height;
        this.rgb = rgb;
        this.source = source;
        this.lum = new double[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int p = rgb[i];
            lum[i] = WR * ((p >> 16) & 0xFF) + WG * ((p >> 8) & 0xFF) + WB * (p & 0xFF);
        }
    }

    public static Frame load(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("not a readable image: " + file);
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = new int[w * h];
        img.getRGB(0, 0, w, h, px, 0, w);
        for (int i = 0; i < px.length; i++) {
            px[i] &= 0xFFFFFF;
        }
        return new Frame(w, h, px, file);
    }

    public int index(int x, int y) {
        return y * width + x;
    }

    public boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public double lum(int x, int y) {
        return lum[y * width + x];
    }

    public int r(int i) {
        return (rgb[i] >> 16) & 0xFF;
    }

    public int g(int i) {
        return (rgb[i] >> 8) & 0xFF;
    }

    public int b(int i) {
        return rgb[i] & 0xFF;
    }

    /** Bilinearly sampled luminance, clamped at the borders. Used by sub-pixel registration. */
    public double sampleLum(double x, double y) {
        double cx = Math.max(0, Math.min(width - 1.0001, x));
        double cy = Math.max(0, Math.min(height - 1.0001, y));
        int x0 = (int) cx, y0 = (int) cy;
        double fx = cx - x0, fy = cy - y0;
        int x1 = Math.min(x0 + 1, width - 1), y1 = Math.min(y0 + 1, height - 1);
        double a = lum[y0 * width + x0], b = lum[y0 * width + x1];
        double c = lum[y1 * width + x0], d = lum[y1 * width + x1];
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }

    public Rect bounds() {
        return new Rect(0, 0, width, height);
    }

    public String name() {
        return source == null ? "<memory>" : source.getName();
    }
}
