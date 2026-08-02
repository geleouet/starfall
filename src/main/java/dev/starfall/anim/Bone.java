package dev.starfall.anim;

/**
 * One joint in a {@link Skeleton}. Holds both the authored bind-pose local
 * transform and the animated local transform that poses/animations write to
 * every frame. Exact field set per docs/system1-contract.md section D.
 *
 * <p>Local transforms compose parent-first: a bone's world transform is its
 * parent's world transform combined with its own local (x, y, rotDeg, scaleX,
 * scaleY). See {@link Skeleton#updateWorldTransforms()}.
 */
public final class Bone {

    public final String name;
    public final int index;
    /** Null only for the skeleton root. */
    public final Bone parent;

    // Local bind pose. Authored once by the rig builder, never touched by animation.
    public float bindX, bindY, bindRotDeg, bindScaleX = 1f, bindScaleY = 1f;

    // Animated local transform. This is what Pose/animation code writes to every
    // frame; Skeleton reads only these fields when computing world transforms.
    public float x, y, rotDeg, scaleX = 1f, scaleY = 1f;

    public Bone(String name, int index, Bone parent) {
        this.name = name;
        this.index = index;
        this.parent = parent;
        resetToBind();
    }

    /** Convenience for rig authoring: set bind local (uniform scale 1) and reset the animated transform to match. */
    public Bone bindLocal(float x, float y, float rotDeg) {
        return bindLocal(x, y, rotDeg, 1f, 1f);
    }

    public Bone bindLocal(float x, float y, float rotDeg, float scaleX, float scaleY) {
        this.bindX = x;
        this.bindY = y;
        this.bindRotDeg = rotDeg;
        this.bindScaleX = scaleX;
        this.bindScaleY = scaleY;
        resetToBind();
        return this;
    }

    /** Copies bind values into the animated transform. Called on every bone before a Pose is applied, so poses can be sparse. */
    public void resetToBind() {
        x = bindX;
        y = bindY;
        rotDeg = bindRotDeg;
        scaleX = bindScaleX;
        scaleY = bindScaleY;
    }

    @Override
    public String toString() {
        return "Bone[" + index + ":" + name + "]";
    }
}
