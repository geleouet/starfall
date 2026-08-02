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
    private static final float BACK_TAU = 0.120f;
    private static final float FRONT_TAU = 0.095f;
    private static final float SLEEVE_TAU = 0.125f;

    /** Cloth is a hanging mass and falls harder than hair, but still nothing like 9.81 -- see {@code HairSim}. */
    private static final float CLOTH_GRAVITY = 2.4f;

    /**
     * Per-chain wind gains, held here as well as handed to {@link ClothSim}.
     *
     * <p>{@link ClothSim#refresh()} multiplies one scene-wide breeze by these, which
     * is exactly right for a breeze and exactly wrong for the gusts of STYLE.md
     * 7.3 -- {@code Directive.Impulse} names a <em>region</em>, and a hem thrown
     * along a knockback while the sleeve answers the wrist is two different
     * vectors on two chains of the same garment. So the gusts are applied per
     * chain after the refresh, and the gains have to be reapplied here because
     * they are the cloth's own and not the caller's.
     */
    private static final float BACK_GAIN = 0.95f;
    private static final float FRONT_GAIN = 0.80f;
    private static final float SLEEVE_GAIN = 1.00f;

    private final HairSim hair;
    private final ClothSim cloth;
    private final VerletSolver solver = new VerletSolver();

    private float windX;
    private float windY;

    private float hemGustX, hemGustY;
    private float sleeveGustX, sleeveGustY;
    private float hairGustX, hairGustY;

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
        //
        // Pass 3: drag 0.095 -> 0.120, bend 0.060 -> 0.070, swing 30 -> 40 deg.
        // A small move, and the size of it is the finding.
        //
        // The brief asked for drag raised and stiffness lowered "until the graded
        // box reads 4-8 frames in delivered pixels". Two measurements say that is
        // the wrong knob, and both are in docs/system3-debt.md with their boxes:
        //
        //   * Doubling the lag here doubled the *particle* lag (8.6 -> 15.8
        //     samples at 240 Hz on the row-5 particle) and moved the *picture* by
        //     0.15 of a frame. With the chains clamped rigid the same box still
        //     reads +0.34 frames, so its whole dynamic range is about half a
        //     frame -- it is measuring the obi, the thighs and a sword scabbard.
        //   * 7.1's band is a statement about onset, and on sim-sway -- the slow
        //     scene, the one the one-sentence test is answered on -- onset reads
        //     this bend constant almost directly. 0.070 puts the readable row 8
        //     frames behind the hips, at the top of the band. 0.085 puts it at 9,
        //     outside. There is no headroom above these numbers on that scene,
        //     whatever the fast one reads.
        //
        // The swing limit is the one that could move freely: at 30 degrees with a
        // knee at 16.5 the soft ceiling was engaging on a chain measured at 13-21
        // degrees of deviation, i.e. the limit was shaping the drape rather than
        // catching an accident. At 40 the knee is 22 and it catches accidents.
        cloth.addChain(new String[] {"clothBackA", "clothBackB", "clothBackC", "clothBackD", "clothBackE"},
                0.286f, BACK_TAU, 0.070f, CLOTH_GRAVITY, BACK_GAIN, 40f);
        // The front hem clears the near leg, so it is limited harder: a front
        // panel free to swing 26 degrees per joint intersects the thigh it is
        // meant to hang in front of.
        cloth.addChain(new String[] {"clothFrontA", "clothFrontB", "clothFrontC"},
                0.160f, FRONT_TAU, 0.060f, CLOTH_GRAVITY, FRONT_GAIN, 20f);
        cloth.addChain(new String[] {"sleeveA", "sleeveB"},
                0.210f, SLEEVE_TAU, 0.060f, 2.2f, SLEEVE_GAIN, 30f);

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

    /**
     * A gust on the hem, in world units per second squared, on top of the breeze.
     *
     * <p>The interface {@code docs/system3-debt.md} records as missing: "System 4
     * must express impact through <em>how cloth trails</em>, because 7.1 bans
     * freeze and shake -- and it has nothing to express it with." An acceleration
     * on the chain is that something, and it is deliberately an acceleration
     * rather than a velocity kick: a kick would be a snap, and STYLE.md 7.2's
     * first line is "no snapping". Held for the length of the directive and
     * released, the chain leans out and relaxes back on its own {@code dragTau} --
     * one soft return, which is the other thing 7.2 asks for.
     *
     * <p>Both hem chains, because {@code Region.CLOTH_HEM} is one region: the
     * front panel and the trailing panel are the same garment and are thrown by
     * the same blow, at their own gains.
     */
    public RigSim hemGust(float ax, float ay) {
        this.hemGustX = ax;
        this.hemGustY = ay;
        return this;
    }

    /** A gust on the sleeve. {@code Region.CLOTH_SLEEVE}, which trails the wrist. */
    public RigSim sleeveGust(float ax, float ay) {
        this.sleeveGustX = ax;
        this.sleeveGustY = ay;
        return this;
    }

    /** A gust on the hair. {@code Region.HAIR}, which trails the head. */
    public RigSim hairGust(float ax, float ay) {
        this.hairGustX = ax;
        this.hairGustY = ay;
        return this;
    }

    /** One frame. Call after the animation pose and after {@link RigIk#update}. */
    public void update(float dt, float timeSeconds) {
        // The gust rides on the breeze rather than replacing it, and is applied
        // before the refresh for hair (which reads HairSim's own field per strand)
        // and after it for cloth (whose refresh overwrites each chain's wind from
        // the scene-wide value). Both end up as one acceleration per chain.
        hair.wind(windX + hairGustX, windY + hairGustY);
        hair.refresh(timeSeconds);
        cloth.refresh();
        applyClothGusts();
        solver.update(dt);
        cloth.writeBack();
        // The breeze is restored so a caller that never gusts sees exactly the
        // behaviour System 3 shipped, and so a gust released this frame is gone
        // next frame unless it is set again.
        hair.wind(windX, windY);
    }

    private void applyClothGusts() {
        if (cloth.chainCount() >= 3) {
            gust(0, hemGustX, hemGustY, BACK_GAIN);
            gust(1, hemGustX, hemGustY, FRONT_GAIN);
            gust(2, sleeveGustX, sleeveGustY, SLEEVE_GAIN);
        }
    }

    private void gust(int index, float ax, float ay, float gain) {
        if (ax == 0f && ay == 0f) {
            return;
        }
        cloth.chain(index).solverChain().wind((windX + ax) * gain, (windY + ay) * gain);
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
