package dev.starfall.capture;

import com.badlogic.gdx.graphics.Pixmap;
import dev.starfall.analysis.Frame;

/** Bridges libGDX pixel buffers to the analysis package's {@link Frame}. */
public final class Framebuffers {

    private Framebuffers() {
    }

    /**
     * Converts a framebuffer readback into an analysis frame, flipping it the same way
     * {@link CaptureApp} flips the PNGs it writes, so an in-memory measurement and a
     * measurement of the written file address the same pixel by the same coordinates.
     */
    public static Frame toFrame(Pixmap pixmap) {
        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        int[] rgb = new int[w * h];
        for (int y = 0; y < h; y++) {
            int srcY = h - 1 - y;
            for (int x = 0; x < w; x++) {
                int rgba = pixmap.getPixel(x, srcY);
                rgb[y * w + x] = (rgba >>> 8) & 0xFFFFFF;
            }
        }
        return new Frame(w, h, rgb, null);
    }
}
