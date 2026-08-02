package dev.starfall.rig;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Skeleton;

/**
 * One frame of the System 3 capture scenes, in the order the contract requires:
 * animation writes {@code Bone} locals, then every IK chain solves, <b>then the
 * simulation</b>, then the renderer draws.
 *
 * <p>Shared by {@link SimScene} and {@link SimDebugScene} so the two are provably
 * pictures of the same run -- the same reasoning {@link IkSceneDriver} records,
 * and it matters more here, because the debug overlay's whole job is to show
 * particles the graded capture deliberately hides.
 *
 * <p>Determinism: {@link #advance} accumulates {@code t} and everything else is a
 * function of it. The one wrinkle is STYLE.md 7.3's held breath, which scales
 * {@code dt} on the way in -- so {@code t} is the figure's own time rather than
 * the wall clock, and a capture's frame spacing is still exactly what the
 * harness asked for.
 */
public final class SimSceneDriver {

    private final SamuraiRig rig;
    private final Skeleton skeleton;
    private final RigIk ik;
    private final RigSim sim;
    private final SimScript script;
    private final Vector2 scratch = new Vector2();

    private float t;

    public SimSceneDriver(SamuraiRig rig, SimScript.Kind kind) {
        this(rig, rig.skeleton(), kind);
    }

    /**
     * The same run without GPU meshes, for measurement.
     *
     * <p>It exists so the timing tests grade <em>this</em> driver rather than a
     * re-enactment of it. {@link SamuraiRig#build()} needs a live GL context
     * because it uploads vertex buffers, and a test that reimplemented the
     * pose-aim-solve-simulate order in order to avoid that would eventually
     * measure a different scene from the one being captured -- and the drift
     * would be invisible.
     */
    public static SimSceneDriver headless(Skeleton skeleton, SimScript.Kind kind) {
        return new SimSceneDriver(null, skeleton, kind);
    }

    private SimSceneDriver(SamuraiRig rig, Skeleton skeleton, SimScript.Kind kind) {
        this.rig = rig;
        this.skeleton = skeleton;
        this.script = new SimScript(kind, skeleton);
        this.ik = new RigIk(skeleton);
        this.sim = new RigSim(skeleton);
    }

    /** Null in a {@link #headless} run. */
    public SamuraiRig rig() {
        return rig;
    }

    public RigIk ik() {
        return ik;
    }

    public RigSim sim() {
        return sim;
    }

    public SimScript script() {
        return script;
    }

    public Skeleton skeleton() {
        return skeleton;
    }

    public float time() {
        return t;
    }

    /** Poses frame zero, teleports every chain onto it and lays the hair out along its rest shape. */
    public void start() {
        t = 0f;
        poseAndAim(0f);
        ik.snap();
        sim.snap(0f);
    }

    /** Advances one substep: re-pose, re-aim, re-solve, re-simulate. */
    public void advance(float dt) {
        // The held breath is read at the time the frame starts, so the scale
        // applied to a step is a function of the state the step begins in and
        // the integration stays deterministic.
        script.sample(t);
        float scaled = dt * script.timeScale;
        t += scaled;
        poseAndAim(t);
        ik.update(scaled);
        // Last, per the ordering contract. Both halves of it hang off bones the
        // IK chains just moved.
        sim.update(scaled, t);
    }

    private void poseAndAim(float time) {
        for (int i = 0; i < skeleton.boneCount(); i++) {
            skeleton.bone(i).resetToBind();
        }
        script.sample(time);
        script.applyBody(skeleton);
        skeleton.updateWorldTransforms();

        ik.swordArm().target(script.armX, script.armY).weight(script.armWeight);
        // The corpus pole: below and behind the shoulder in the chest's own
        // frame. Without it the bind arm sits at 0.996 of its own reach, the
        // elbow is on the chain axis, and which side it commits to on frame zero
        // is decided by float rounding -- see RigIk#armPoleFromChest.
        ik.armPoleFromChest(0.10f, -0.19f);

        ik.fromHips(script.spineLocalX, script.spineLocalY, scratch);
        ik.spine().target(scratch.x, scratch.y).weight(script.spineWeight);

        ik.legL().target(script.footLX, script.footLY).weight(script.legWeight);
        ik.legR().target(script.footRX, script.footRY).weight(script.legWeight);

        sim.wind(script.windX, script.windY);
    }
}
