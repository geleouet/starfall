package dev.starfall.rig;

import dev.starfall.anim.Skeleton;
import dev.starfall.sim.ClothSim;
import dev.starfall.sim.HairSim;
import dev.starfall.sim.VerletSolver;

/**
 * The simulation bound to a {@link SamuraiRig}: hair strands, garment sway, and
 * the one thing a caller must not get wrong, which is when it runs.
 *
 * <p>Deliberately the same shape as {@link RigIk} -- a holder, not a system.
 * {@link SamuraiRig} is untouched, a scene that wants no simulation simply does
 * not construct one, and System 4 can reach the hair and the cloth directly to
 * drive wind and gusts per frame.
 *
 * <h2>Ordering</h2>
 *
 * <p>The contract is: animation writes {@code Bone} locals, then IK chains
 * update, <b>then this</b>, then the renderer draws. It has to be last of the
 * three, because both halves of it are attached to bones that IK moves -- the
 * hair to the skull the trunk chain carries, the cloth to the hips and the wrist
 * the arm chain carries. Running it before IK would hang this frame's hair off
 * last frame's head.
 *
 * <h2>The arrival order, which is the point</h2>
 *
 * <p>STYLE.md 7.1 puts cloth 4-8 frames behind the body and hair tips 8-14, and
 * 7.0's third positive -- "nothing may arrive at the same time" -- is the
 * standard everything here is graded against. System 2's own debt (E3) records
 * that its chain still arrives as a unit, and hair and cloth are the natural
 * cure: with these two running, the figure's arrival order is body, then
 * garment, then sleeve, then hair, then escapee tips, spread over about a fifth
 * of a second. That ordering is carried entirely by the numbers below --
 * {@code dragTau} and the bend stiffness -- and it is measured rather than
 * asserted; see {@code SimTimingTest}.
 */
public final class RigSim {

    /**
     * Cloth damping times, in seconds. These are what put the garment between
     * the body and the hair rather than on top of either.
     *
     * <p>They are shorter than any hair strand's and longer than nothing: a hem
     * is heavy and its own weight brings it back, where a hair is not and does
     * not. The front hem is the quickest of the three because it is the smallest
     * mass and it is pinned between the obi and the leg; the sleeve is the
     * slowest because it hangs off a wrist, which is the fastest-moving anchor
     * in the figure and therefore the one whose lag reads loudest.
     */
    private static final float BACK_TAU = 0.095f;
    private static final float FRONT_TAU = 0.095f;
    private static final float SLEEVE_TAU = 0.125f;

    /** Cloth is a hanging mass and falls harder than hair, but still nothing like 9.81 -- see {@code HairSim}. */
    private static final float CLOTH_GRAVITY = 2.4f;

    private final HairSim hair;
    private final ClothSim cloth;
    private final VerletSolver solver = new VerletSolver();

    private float windX;
    private float windY;

    public RigSim(Skeleton skeleton) {
        this.hair = SamuraiHair.build(skeleton);
        this.cloth = new ClothSim(skeleton);

        // The trailing hem: the biggest garment mass in the figure, the one
        // reference images 1 and 2 throw their ink cloud from, and the only one
        // long enough to hold a curve. Its swing limit is the widest because it
        // is the only chain with room to swing without colliding with a leg.
        //
        // Five bones and six particles, up from three and four. The pass-1
        // review measured this hem's tip at 0.00 px across every inter-frame step
        // of a knockback and put the cause plainly: "three chains of four
        // particles cannot bend -- the hem needs enough chain to curve and enough
        // render weight to change the silhouette." Two of the three numbers
        // below moved with it and neither is cosmetic:
        //
        //   * The swing limit is 34 degrees *per joint*, so a four-particle chain
        //     could only ever be a straight panel at an angle -- the shape a hem
        //     makes is the sum of five small deviations, not one large one, and
        //     the soft ceiling is now reached by none of them.
        //   * The wind gain rises from 0.55 to 0.95. A hem is the largest sail on
        //     the figure and was feeling half the air a hair does. Measured, this
        //     term sets where the hem *hangs* rather than how far it swings --
        //     the swing range is set by the bend stiffness -- so it is what
        //     decides whether the trailing panel reads as blown back at all.
        //   * The bend recovery drops from 0.085 s to 0.060 s. On a five-joint
        //     chain the old figure put the hem's onset 12 frames behind the hips,
        //     outside 7.1's 4-8 band; 0.060 lands it at 8 with the largest swing
        //     the band allows. Measured per bone over the delivered extreme
        //     window, the chain now deviates 13-21 degrees from bind where four
        //     particles could only ever hold one angle.
        cloth.addChain(new String[] {"clothBackA", "clothBackB", "clothBackC", "clothBackD", "clothBackE"},
                0.286f, BACK_TAU, 0.060f, CLOTH_GRAVITY, 0.95f, 30f);
        // The front hem clears the near leg, so it is limited harder: a front
        // panel free to swing 26 degrees per joint intersects the thigh it is
        // meant to hang in front of.
        cloth.addChain(new String[] {"clothFrontA", "clothFrontB", "clothFrontC"},
                0.160f, FRONT_TAU, 0.060f, CLOTH_GRAVITY, 0.80f, 20f);
        cloth.addChain(new String[] {"sleeveA", "sleeveB"},
                0.210f, SLEEVE_TAU, 0.060f, 2.2f, 1.00f, 30f);

        hair.register(solver);
        cloth.register(solver);
    }

    public HairSim hair() {
        return hair;
    }

    public ClothSim cloth() {
        return cloth;
    }

    /**
     * The breeze, in world units per second squared. Hair and cloth feel the
     * same air, scaled per strand and per chain -- a sleeve is heavier than a
     * hair and an escapee is lighter than either.
     */
    public RigSim wind(float wx, float wy) {
        this.windX = wx;
        this.windY = wy;
        hair.wind(wx, wy);
        cloth.wind(wx, wy);
        return this;
    }

    public float windX() {
        return windX;
    }

    public float windY() {
        return windY;
    }

    /** One frame. Call after the animation pose and after {@link RigIk#update}. */
    public void update(float dt, float timeSeconds) {
        hair.refresh(timeSeconds);
        cloth.refresh();
        solver.update(dt);
        cloth.writeBack();
    }

    /**
     * Lays hair and cloth out along their rest shapes with no velocity, from
     * wherever the skeleton currently is. Scene setup and respawns only.
     */
    public void snap(float timeSeconds) {
        solver.clear();
        hair.refresh(timeSeconds);
        hair.reset();
        cloth.reset();
    }
}
