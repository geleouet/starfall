package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;

/**
 * One frame of the IK capture scenes, in the order the contract requires:
 * animation writes {@code Bone} locals, then every chain solves, then the
 * renderer draws.
 *
 * <p>Shared by {@link IkScene} and {@link IkDebugScene} so the two are provably
 * pictures of the same run -- if the debug overlay were driven by its own copy of
 * this it would eventually drift from the thing it is supposed to be explaining,
 * and the drift would be invisible.
 *
 * <p>Determinism: {@link #advance} accumulates {@code t} and everything else is a
 * function of it, so a given sequence of {@code dt} always produces the same
 * frame. {@link #start} is the one place {@link RigIk#snap()} is called -- there
 * is no previous frame at scene creation to be continuous with, and everywhere
 * else there is.
 */
public final class IkSceneDriver {

    private final SamuraiRig rig;
    private final RigIk ik;
    private final IkTargetScript script;
    private final SwingAnimation swing = new SwingAnimation();
    private final Vector2 scratch = new Vector2();

    private float t;

    public IkSceneDriver(SamuraiRig rig, IkTargetScript.Kind kind) {
        this.rig = rig;
        this.script = new IkTargetScript(kind, rig.skeleton());
        this.ik = new RigIk(rig.skeleton());
    }

    public SamuraiRig rig() {
        return rig;
    }

    public RigIk ik() {
        return ik;
    }

    public IkTargetScript script() {
        return script;
    }

    public Skeleton skeleton() {
        return rig.skeleton();
    }

    public float time() {
        return t;
    }

    /** Poses frame zero and teleports every chain onto it. */
    public void start() {
        t = 0f;
        poseAndAim(0f);
        ik.snap();
    }

    /** Advances one substep: re-pose, re-aim, re-solve. */
    public void advance(float dt) {
        t += dt;
        poseAndAim(t);
        ik.update(dt);
    }

    /**
     * Writes the base animation pose and hands every chain its target and weight
     * for time {@code time}. Deliberately does not solve: the pose has to be
     * complete, and the trunk target has to be resolved against the hips the
     * animation just moved, before anything reads it.
     */
    private void poseAndAim(float time) {
        if (script.animated()) {
            rig.applyPose(swing.sample(time));
        } else {
            rig.applyBindPose();
        }

        script.sample(time);

        ik.swordArm().target(script.armX, script.armY).weight(script.armWeight);

        ik.fromHips(script.spineLocalX, script.spineLocalY, scratch);
        ik.spine().target(scratch.x, scratch.y).weight(script.spineWeight);

        // Planted: world-fixed for the whole clip, so the legs absorb whatever the
        // hips do instead of sliding with them.
        ik.legL().target(script.footLX, script.footLY).weight(script.legWeight);
        ik.legR().target(script.footRX, script.footRY).weight(script.legWeight);
    }
}
