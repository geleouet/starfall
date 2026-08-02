package dev.starfall.stage;

import java.util.List;

/**
 * The IK chains a directive can address, and when each of their links arrives.
 *
 * <p>These are exactly {@code dev.starfall.rig.RigIk}'s chains, named rather than
 * referenced: the staging layer is headless and must stay so, and a schedule that
 * held a live {@code IkChain} could not be built without a rig, compared for
 * equality, or written to a file. The renderer maps a {@link Chain} to its own
 * object; the {@link #bones()} list is here so that mapping is checkable rather
 * than conventional.
 *
 * <p><b>The settle profiles are the interesting part</b> and they are read down
 * {@link #SWORD_SIDE}, not per chain. STYLE.md 7.0.3 is a claim about one
 * kinematic run from the pelvis to the blade tip, and that run crosses three
 * chains in {@code RigIk}. Laid end to end the arrivals are
 *
 * <pre>
 *   hips 0.30 - spine 0.35 - chest 0.41 - clavicle 0.44
 *        - shoulder 0.48 - elbow 0.53 - wrist 0.57 - blade tip 0.60
 * </pre>
 *
 * strictly increasing, spanning STYLE.md 7.1's band exactly end to end, and with
 * 0.30 s between the pelvis coming to rest and the tip -- against
 * {@code system2-debt.md} E3's requirement that "the tip is still drifting
 * perceptibly a quarter second after the hips have stopped."
 *
 * <p>The legs branch off the pelvis rather than continuing the run, so they sit
 * beside the spine's numbers rather than after them. A planted foot that settled
 * as late as a wrist would read as the figure standing on jelly, and
 * {@code system2-debt.md} lists "planted feet under a moving hip" first among the
 * things System 4 must not regress.
 */
public enum Chain {

    /**
     * {@code RigIk.spine()}: hips to chest, effector at the neck. The source of
     * motion in STYLE.md 7.0.1, and therefore the first thing in the rig to move
     * and the first to arrive.
     */
    SPINE(List.of("hips", "spine", "chest"), Settle.of(0.30, 0.35, 0.41)),

    /**
     * {@code RigIk.clavicle()}. One short bone whose whole job, per {@code RigIk},
     * is "to stop the arm's root being a fixed pixel".
     */
    CLAVICLE(List.of("shoulderL"), Settle.of(0.44)),

    /**
     * {@code RigIk.swordArm()}, extended by two links the chain does not solve but
     * the eye reads: the hand, and the blade tip hanging off it. STYLE.md 7.0.3
     * names both -- "the wrist should arrive after the elbow; the blade tip should
     * drift a beat after the hand has stopped" -- so both need an arrival even
     * though only the first two are bones the solver turns.
     */
    SWORD_ARM(List.of("upperArmL", "forearmL", "handL", "bladeTip"), Settle.of(0.48, 0.53, 0.57, 0.60)),

    /** {@code RigIk.legL()}. The near foot. */
    LEG_LEAD(List.of("thighL", "shinL"), Settle.of(0.31, 0.34)),

    /** {@code RigIk.legR()}. The far foot, a hair behind the near one. */
    LEG_TRAIL(List.of("thighR", "shinR"), Settle.of(0.32, 0.36));

    /**
     * The run STYLE.md 7.0.3 is about, in order. Concatenating these three
     * profiles must give a strictly increasing sequence, and
     * {@code ChainSettleTest} checks it.
     */
    public static final List<Chain> SWORD_SIDE = List.of(SPINE, CLAVICLE, SWORD_ARM);

    private final List<String> bones;
    private final Settle settle;

    Chain(List<String> bones, Settle settle) {
        this.bones = bones;
        this.settle = settle;
    }

    /** Bone names in {@code SamuraiRig}, root first. {@code bladeTip} is a mark, not a bone. */
    public List<String> bones() {
        return bones;
    }

    public Settle settle() {
        return settle;
    }
}
