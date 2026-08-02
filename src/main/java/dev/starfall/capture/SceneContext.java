package dev.starfall.capture;

/** Render-target dimensions and mode handed to a scene at creation time. */
public final class SceneContext {

    public final int width;
    public final int height;
    /** True when running under the offscreen capture harness rather than a live window. */
    public final boolean capturing;

    public SceneContext(int width, int height, boolean capturing) {
        this.width = width;
        this.height = height;
        this.capturing = capturing;
    }

    public float aspect() {
        return width / (float) height;
    }
}
