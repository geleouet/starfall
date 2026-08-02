package dev.starfall.analysis;

/** Builders for synthetic frames with known ground truth. */
final class Synth {

    static final int PAPER = 218;
    static final int PAPER_RGB = rgb(220, 218, 210);

    private Synth() {
    }

    static int rgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    /** Grey level as a packed pixel; luminance of a neutral grey equals the level exactly. */
    static int grey(int v) {
        int c = Math.max(0, Math.min(255, v));
        return rgb(c, c, c);
    }

    interface Fill {
        int at(int x, int y);
    }

    static Frame frame(int w, int h, Fill fill) {
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                px[y * w + x] = fill.at(x, y);
            }
        }
        return new Frame(w, h, px, null);
    }

    /** Paper everywhere, with one solid dark rectangle. */
    static Frame paperWithBlock(int w, int h, Rect block, int inkLevel) {
        return frame(w, h, (x, y) -> block.contains(x, y) ? grey(inkLevel) : grey(PAPER));
    }
}
