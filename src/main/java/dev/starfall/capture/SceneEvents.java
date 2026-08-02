package dev.starfall.capture;

import java.util.List;
import java.util.Map;

/**
 * Optional: a scene that can be told to do something, rather than only to advance.
 *
 * <p>The project brief's debug API is specified around
 * {@code POST /trigger {"action":"play_animation","state":"parry"}}, and this is the hook that
 * makes it more than a time slider. A scene that does not implement it is still fully
 * drivable — load it, seek to a moment, capture a window — which is how every scene in the
 * corpus works today, since they are all time-scripted.
 *
 * <p>Kept deliberately stringly-typed. The debug API is a test harness, not a runtime
 * interface: an event that does not exist should produce a clear error at the wire, not a
 * compile-time coupling between the scene packages and the server.
 */
public interface SceneEvents {

    /** Event names this scene understands, for {@code GET /state} to advertise. */
    List<String> events();

    /**
     * Fires a named event.
     *
     * @return true when the event was recognised and applied; false leaves the caller to
     *         report it as unknown rather than silently doing nothing
     */
    boolean fire(String event, Map<String, String> args);
}
