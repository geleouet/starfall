package dev.starfall.capture;

/**
 * The one place a scene's simulated time advances.
 *
 * <p>Three things drive scenes: the capture harness, the headless timing measurement, and the
 * debug API. STYLE.md §7.1 requires a timing measurement to drive <i>the same scene the
 * capture runs</i>, and a review has already been invalidated by a measurement that did not —
 * it reported peak frames outside its own captured window. That guarantee is worth nothing if
 * the three drivers integrate differently, so they share this class rather than each keeping
 * a copy of the substep.
 *
 * <p>The substep is fixed and small enough to keep Verlet constraints stable. Changing it
 * changes every capture on disk, so it is a deliberate, project-wide decision, not a tuning
 * knob.
 */
public final class SceneClock {

    /** Fixed physics substep, shared by every driver. */
    public static final float SUBSTEP = 1f / 240f;

    private SceneClock() {
    }

    /** Advances {@code scene} by {@code seconds}, in fixed substeps. */
    public static void advance(Scene scene, float seconds) {
        int steps = Math.round(seconds / SUBSTEP);
        for (int i = 0; i < steps; i++) {
            scene.update(SUBSTEP);
        }
    }

    /**
     * Runs a scene's warmup plus an extra offset, which is what every driver does before its
     * first delivered frame. Kept here so "start" means the same thing in all three.
     */
    public static void warmupAndSeek(Scene scene, float start) {
        advance(scene, scene.warmup() + start);
    }
}
