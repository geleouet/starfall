package dev.starfall.anim;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A sparse set of per-bone local-transform deltas, relative to each bone's bind
 * pose. Deltas rather than absolutes so an animation only has to name the bones
 * it actually moves, and never needs to know a bone's authored bind offset —
 * that stays a rig-authoring detail. See SamuraiRig#applyPose, which resets
 * every bone to bind before applying the deltas named here.
 */
public final class Pose {

    public static final class Delta {
        public float dx, dy, dRotDeg;
        public float scaleX = 1f, scaleY = 1f;
    }

    private final Map<String, Delta> deltas = new LinkedHashMap<>();

    public Pose set(String boneName, float dx, float dy, float dRotDeg) {
        return set(boneName, dx, dy, dRotDeg, 1f, 1f);
    }

    public Pose set(String boneName, float dx, float dy, float dRotDeg, float scaleX, float scaleY) {
        Delta d = new Delta();
        d.dx = dx;
        d.dy = dy;
        d.dRotDeg = dRotDeg;
        d.scaleX = scaleX;
        d.scaleY = scaleY;
        deltas.put(boneName, d);
        return this;
    }

    /** Null if this pose does not touch that bone. */
    public Delta get(String boneName) {
        return deltas.get(boneName);
    }

    public Set<String> boneNames() {
        return deltas.keySet();
    }
}
