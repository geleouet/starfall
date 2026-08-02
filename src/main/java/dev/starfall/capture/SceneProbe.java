package dev.starfall.capture;

import java.util.Map;

/**
 * Optional: a scene that can report its own simulation state, in image pixels.
 *
 * <p>{@link dev.starfall.capture.TimingApp} measures timing from rendered pixels, which works
 * for every scene and needs nothing from the scene author. That is deliberate — it is the
 * only way to guarantee STYLE.md §7.1's requirement that a timing measurement drives the same
 * scene the capture runs rather than re-enacting it, without asking every scene to grow a
 * parallel reporting path that could drift from what it draws.
 *
 * <p>But pixels cannot see a particle hidden behind a torso, and {@code docs/system3-debt.md}
 * records exactly that case: a hem particle that moved while its rendered box moved 0.00 px.
 * A scene implementing this interface lets a review measure both and say which is which —
 * which is the honest answer, since only one of them is the picture.
 *
 * <p>Implementations must return points in the same coordinate space the scene renders into,
 * with the origin at the <b>top left</b> and +y downward, matching the written PNGs.
 */
public interface SceneProbe {

    /**
     * Named points of interest in the current simulation state, in image pixels.
     * Called after {@link Scene#update(float)} and before or instead of a render.
     */
    Map<String, float[]> probe();
}
