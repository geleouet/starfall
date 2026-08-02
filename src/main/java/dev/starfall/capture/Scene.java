package dev.starfall.capture;

/**
 * A scriptable scene that can be driven either interactively or by the offscreen
 * capture harness. Scenes must be deterministic: given the same sequence of
 * {@link #update(float)} calls they must produce identical frames, so that
 * contact sheets from different iterations are directly comparable.
 */
public interface Scene {

    /** Stable identifier used on the command line, e.g. {@code --scene skinning-swing}. */
    String name();

    /** One-line human description, printed into the contact sheet header. */
    default String description() {
        return name();
    }

    void create(SceneContext ctx);

    /** Advance simulation by {@code dt} seconds. Called with a fixed substep. */
    void update(float dt);

    /** Draw the current state. The target framebuffer is already bound and cleared. */
    void render();

    default void resize(int width, int height) {
    }

    default void dispose() {
    }

    /**
     * Simulated length of a capture run, in seconds. The harness spreads its
     * frames evenly across this window.
     */
    default float duration() {
        return 2f;
    }

    /**
     * Seconds of simulation to run before the first captured frame, to let
     * physics settle out of its initial state.
     */
    default float warmup() {
        return 0f;
    }
}
