package com.starfall.render;

/**
 * Pure, GL-free maths behind {@link PixelViewport}.
 *
 * <p>Given a window size in screen pixels and a minimum world area that must always remain visible
 * (expressed in world pixels, i.e. the true resolution the sprite art is authored at), this computes:
 *
 * <ul>
 *   <li>the largest <em>integer</em> zoom factor at which the minimum area still fits;</li>
 *   <li>the world area actually visible at that zoom - always at least the minimum, expanded to
 *       swallow the leftover space so the player simply sees more world instead of black bars;</li>
 *   <li>the GL viewport rectangle to use.</li>
 * </ul>
 *
 * <p>The world size is rounded <em>up</em> to a whole number of world pixels, so the GL viewport is
 * up to one world pixel larger than the window on each axis. That surplus is split evenly and lets
 * the viewport bleed a fraction of a world pixel off-screen rather than leaving a black gutter.
 * Because both the scale and the viewport origin are integers, every world pixel covers exactly
 * {@code scale} screen pixels and lands on the screen pixel grid.
 *
 * <p>This class is deliberately free of any libGDX runtime dependency so the maths can be unit
 * tested without a GL context.
 */
public final class PixelScale {

    /** Number of screen pixels per world pixel. Always >= 1. */
    public final int scale;
    /** Width of the visible world area, in whole world pixels. */
    public final int worldWidth;
    /** Height of the visible world area, in whole world pixels. */
    public final int worldHeight;
    /** X origin of the GL viewport, in screen pixels. Zero or negative (overscan). */
    public final int screenX;
    /** Y origin of the GL viewport, in screen pixels. Zero or negative (overscan). */
    public final int screenY;
    /** Width of the GL viewport, in screen pixels. Equals {@code worldWidth * scale}. */
    public final int screenWidth;
    /** Height of the GL viewport, in screen pixels. Equals {@code worldHeight * scale}. */
    public final int screenHeight;

    private PixelScale(int scale, int worldWidth, int worldHeight,
                       int screenX, int screenY, int screenWidth, int screenHeight) {
        this.scale = scale;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Computes the pixel-perfect layout for a window.
     *
     * @param windowWidth    window width in screen pixels (clamped to at least 1)
     * @param windowHeight   window height in screen pixels (clamped to at least 1)
     * @param minWorldWidth  world width that must always be visible, in world pixels (at least 1)
     * @param minWorldHeight world height that must always be visible, in world pixels (at least 1)
     */
    public static PixelScale compute(int windowWidth, int windowHeight,
                                     int minWorldWidth, int minWorldHeight) {
        int w = Math.max(1, windowWidth);
        int h = Math.max(1, windowHeight);
        int minW = Math.max(1, minWorldWidth);
        int minH = Math.max(1, minWorldHeight);

        // Largest integer zoom at which the guaranteed area still fits on both axes.
        // Never below 1: on a window smaller than the minimum area we keep 1:1 and show less world
        // rather than producing fractional (shimmering) pixels.
        int scale = Math.max(1, Math.min(w / minW, h / minH));

        // Expand to fill the window, rounding up to whole world pixels.
        int worldWidth = ceilDiv(w, scale);
        int worldHeight = ceilDiv(h, scale);

        int screenWidth = worldWidth * scale;
        int screenHeight = worldHeight * scale;

        // Surplus is <= scale-1 px per axis; centre it with an integer offset so the grid stays aligned.
        int screenX = floorDiv2(w - screenWidth);
        int screenY = floorDiv2(h - screenHeight);

        return new PixelScale(scale, worldWidth, worldHeight, screenX, screenY, screenWidth, screenHeight);
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    /** Halves a (possibly negative) value, rounding towards negative infinity so the result is exact. */
    private static int floorDiv2(int value) {
        return Math.floorDiv(value, 2);
    }

    @Override
    public String toString() {
        return "PixelScale{scale=" + scale
                + ", world=" + worldWidth + "x" + worldHeight
                + ", viewport=" + screenWidth + "x" + screenHeight
                + " @ " + screenX + "," + screenY + '}';
    }
}
