package com.starfall.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * An integer-locked, adaptive viewport for pixel art.
 *
 * <p>Behaves like libGDX's {@code ExtendViewport} - a guaranteed minimum world area is always
 * visible and any leftover window space simply reveals more world, never black bars - except that
 * the zoom is restricted to whole numbers. One world pixel therefore always covers exactly
 * {@link #getScale()} screen pixels, so pixel art never shows neighbouring pixels of different
 * sizes and never shimmers while scrolling.
 *
 * <p>World units are world pixels: the game's art is authored at that resolution (a character is
 * 32 world pixels tall), so all gameplay coordinates are simply pixel coordinates.
 *
 * <p>The camera is kept snapped to whole world pixels: {@link #setCameraTarget} accepts a
 * fractional position (physics, smooth following) but the frustum edges it produces are always
 * integers, which is what actually guarantees a jitter-free image.
 *
 * <p>All the arithmetic lives in {@link PixelScale} so it can be unit tested without a GL context.
 */
public class PixelViewport extends Viewport {

    private final int minWorldWidth;
    private final int minWorldHeight;

    /** Desired camera centre in world pixels; may be fractional, it is snapped on apply. */
    private final Vector2 cameraTarget = new Vector2();

    private int scale = 1;
    /** Left/bottom edge of the frustum, in whole world pixels. */
    private int cameraLeft;
    private int cameraBottom;

    public PixelViewport(int minWorldWidth, int minWorldHeight) {
        this(minWorldWidth, minWorldHeight, new OrthographicCamera());
    }

    public PixelViewport(int minWorldWidth, int minWorldHeight, Camera camera) {
        if (minWorldWidth < 1 || minWorldHeight < 1) {
            throw new IllegalArgumentException("Minimum world size must be at least 1x1 world pixel");
        }
        this.minWorldWidth = minWorldWidth;
        this.minWorldHeight = minWorldHeight;
        setCamera(camera);
        setWorldSize(minWorldWidth, minWorldHeight);
        cameraTarget.set(minWorldWidth / 2f, minWorldHeight / 2f);
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        PixelScale layout = PixelScale.compute(screenWidth, screenHeight, minWorldWidth, minWorldHeight);
        this.scale = layout.scale;

        setWorldSize(layout.worldWidth, layout.worldHeight);
        setScreenBounds(layout.screenX, layout.screenY, layout.screenWidth, layout.screenHeight);

        if (centerCamera) {
            cameraTarget.set(minWorldWidth / 2f, minWorldHeight / 2f);
        }
        snapCamera();
        apply(false);
    }

    /**
     * Moves the camera. The position may be fractional - it is snapped to the world pixel grid so
     * the rendered image never lands between screen pixels.
     */
    public void setCameraTarget(float worldX, float worldY) {
        cameraTarget.set(worldX, worldY);
        snapCamera();
    }

    /** Aligns the frustum edges with the world pixel grid and pushes the result to the camera. */
    private void snapCamera() {
        int worldWidth = (int) getWorldWidth();
        int worldHeight = (int) getWorldHeight();

        cameraLeft = Math.round(cameraTarget.x - worldWidth / 2f);
        cameraBottom = Math.round(cameraTarget.y - worldHeight / 2f);

        Camera camera = getCamera();
        camera.viewportWidth = worldWidth;
        camera.viewportHeight = worldHeight;
        // Frustum edges land exactly on integers even when the world size is odd.
        camera.position.set(cameraLeft + worldWidth / 2f, cameraBottom + worldHeight / 2f, 0f);
        camera.update();
    }

    /** Screen pixels per world pixel. Always a whole number, never below 1. */
    public int getScale() {
        return scale;
    }

    public int getMinWorldWidth() {
        return minWorldWidth;
    }

    public int getMinWorldHeight() {
        return minWorldHeight;
    }

    /** Left edge of the visible world area, in whole world pixels. */
    public int getCameraLeft() {
        return cameraLeft;
    }

    /** Bottom edge of the visible world area, in whole world pixels. */
    public int getCameraBottom() {
        return cameraBottom;
    }

    /** Visible world width in whole world pixels. */
    public int getVisibleWorldWidth() {
        return (int) getWorldWidth();
    }

    /** Visible world height in whole world pixels. */
    public int getVisibleWorldHeight() {
        return (int) getWorldHeight();
    }
}
