package dev.starfall.direct;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.art.Palette;
import dev.starfall.ik.IkChain;
import dev.starfall.render.InkFxRenderer;
import dev.starfall.stage.Anchor;
import dev.starfall.stage.Chain;
import dev.starfall.stage.Directive;
import dev.starfall.stage.Framing;
import dev.starfall.stage.Region;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.Settle;
import dev.starfall.stage.Stage;
import dev.starfall.stage.Stance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plays a {@link Schedule} onto a set of {@link Figure}s, one frame at a time.
 *
 * <h2>Its job is execution, not authorship</h2>
 *
 * <p>Every instant, every weight, every settle and every contact in the score was
 * decided by the staging layer, which derived them from STYLE.md 7 and recorded
 * the derivation in {@code Timing}. Nothing here re-derives any of them, and the
 * one number this class introduces -- {@link #GUST} -- is a unit conversion rather
 * than an aesthetic choice: {@code Directive.Impulse} carries a magnitude in
 * {@code 0..1} and {@code VerletChain.wind} wants world units per second squared.
 *
 * <p>What it does own is the reading of four directives whose vocabulary does not
 * land one-to-one on this rig. Each is documented at the method that resolves it,
 * and each is a genuine seam between the layers rather than a preference:
 *
 * <ul>
 *   <li><b>The trunk anchor is at hip height and the chain it names ends at the
 *       neck</b> -- see {@link #trunk}. Applied literally the figure folds double.</li>
 *   <li><b>The foot anchors are on the ground plane and the leg chains end at the
 *       ankle</b> -- see {@link #leg}. Applied literally both legs saturate.</li>
 *   <li><b>The clavicle is not an {@code IkChain}</b> and {@code RigIk} drives it
 *       from the sword arm's own target -- see {@link #clavicle}.</li>
 *   <li><b>Nothing translates a body.</b> The stream moves a figure by re-aiming
 *       its trunk at a new tile, so the pelvis has to follow the trunk anchor --
 *       see {@link #trunk} again.</li>
 * </ul>
 *
 * <h2>The ordering contract</h2>
 *
 * <p>Animation writes bone locals, then IK chains update, then the simulation,
 * then the renderer. {@link #advance} does the first three in that order for every
 * figure, and the fourth is the caller's. It is not negotiable: the hair hangs off
 * a skull the trunk chain carries and the sleeve off a wrist the arm chain
 * carries, so running the simulation first hangs this frame's hair off last
 * frame's head.
 *
 * <h2>The contact-ordering guarantee</h2>
 *
 * <p>The engine proved that a beat honouring its overlap hint contacts after the
 * previous one did, "however the renderer scales the beats"; {@code ScheduledBeat}
 * carried that through the mapping to seconds. This class is the third link, and
 * it keeps the guarantee for one structural reason rather than by checking
 * anything: <b>it never reorders, retimes or drops a directive.</b> The clock is
 * monotone, every directive is evaluated as a pure function of it, and the only
 * thing that changes the rate is {@link Directive.TimeRamp} -- which is global, so
 * it slows every body's clock by the same factor and cannot reorder two contacts
 * on different bodies. {@code DirectorTest} asserts it over a real schedule at
 * 240 Hz rather than trusting the argument.
 */
public final class Director {

    /**
     * World units per second squared of gust per unit of {@code Impulse.magnitude}.
     *
     * <p>Not a taste: System 3's own impact gust runs at 4.4 to 5.2 in the same
     * units on the same chains ({@code SimScript.wind}), against a cloth gravity of
     * 2.4. Matching it means a full-magnitude blow throws the garment about as far
     * as the knockback System 3 was graded on, which is the behaviour
     * {@code system3-debt.md} verified and the behaviour this layer must not
     * regress.
     */
    public static final double GUST = 5.0;

    /** The air both figures stand in. Light: STYLE.md 7.2's returns are graded in dead air, not here. */
    public static final float BREEZE_X = -0.55f;
    public static final float BREEZE_Y = 0.04f;

    /**
     * How far apart the lane's tiles are drawn, as a multiple of {@code Stage.TILE_WIDTH}.
     *
     * <h2>Why this is here at all, which is a finding rather than a preference</h2>
     *
     * <p>{@code Stage} puts one tile at 1.0 world unit and a standing figure at
     * 1.70, and sizes a body at {@code BODY_HALF = 0.28} -- 0.56 units across. This
     * rig is wider than that: {@code SamuraiRig}'s haori rails run to 0.34 behind
     * and 0.30 in front, so the authored garment alone is 0.64, and STYLE.md 3.2
     * then puts a wet-bleed halo "several pixels further" outside it -- measured by
     * System 3 at 10 to 131 px, which at the intimate framing is another 0.15 to
     * 0.2 units <em>per side</em>. Two bodies on adjacent tiles therefore have
     * about 0.36 units of clear ground between their garments and rather more than
     * that of halo, so <b>they merge into one mass</b>. The first two-figure capture
     * shot on this project shows exactly that.
     *
     * <p>Reference images 3, 4 and 5 are unambiguous about what should be there
     * instead: the duellists stand with roughly a body-width of open paper between
     * them and only the <em>blades</em> cross. That gap is not decoration -- it is
     * what lets each silhouette be read as a figure, which is the whole subject of
     * STYLE.md 11.0's part count.
     *
     * <p><b>The stretch is applied about tile centres, not to world x.</b> Every
     * anchor the staging layer produces is a tile centre plus an offset smaller
     * than half a tile -- {@code GUARD_REACH} is the largest at 0.46 -- so rounding
     * an anchor's x recovers the tile it belongs to exactly, and adding
     * {@code tile * (SPREAD - 1)} moves the tile while leaving the offset alone.
     * Nothing inside a body moves relative to anything else in it: a guard stays
     * 0.46 in front of its own hip, a contact point stays on the body it names, a
     * foot stays under the pelvis it hangs from. Only the distance between bodies
     * changes, and the camera framing is stretched by the same factor so the shot
     * is the one the schedule asked for.
     *
     * <p>1.35 rather than anything larger because the framing has to hold: at
     * {@code INTIMACY_TILES} of 3.2 the intimate shot becomes 4.32 units wide, two
     * 0.64-wide figures 1.35 apart span 2.0 of it, and the figure lands at about
     * 380 px of a 720 px frame -- close to the 330 px every matched-scale
     * comparison in this project has been run at, so the part count stays
     * comparable with the ones already on record.
     *
     * <p><b>This is a mitigation and it is reported as one.</b> The clean fix is in
     * {@code Stage}, which this pass does not own: either {@code TILE_WIDTH} rises
     * against {@code FIGURE_HEIGHT}, or {@code BODY_HALF} grows to the width the
     * rig actually has and the lane spacing follows it.
     */
    public static final double LANE_SPREAD = 1.35;

    /**
     * Moves an anchor's tile without moving the anchor within its tile.
     *
     * @see #LANE_SPREAD
     */
    public static double stretch(double x) {
        return x + Math.round(x / Stage.TILE_WIDTH) * Stage.TILE_WIDTH * (LANE_SPREAD - 1.0);
    }

    /**
     * Where an anchor is, once the lane has been spread.
     *
     * <p><b>A contact point does not belong to a tile, it belongs to the gap
     * between two, and that is the distinction this method exists for.</b>
     * {@code Meeting} names one crossing twice -- once on each body, in each body's
     * own vocabulary -- and the whole of STYLE.md 7.2's parry rests on the two
     * names describing the same point in space. On the engine's own spacing they
     * very nearly do: the attacker's leading side is {@code GUARD_REACH} toward the
     * defender and the defender's is {@code GUARD_REACH} back, so on adjacent tiles
     * the two land 0.08 units apart and the blades cross.
     *
     * <p>Stretching those two points about their <em>own</em> tiles pulls them
     * apart by the full spread -- 0.43 units, measured on the first capture -- and
     * the two blades stop meeting. That is not a cosmetic regression: it is the
     * signature beat failing. So a {@link Anchor.Site#CONTACT} or
     * {@link Anchor.Site#TILE} point scales with the lane instead, which keeps the
     * gap between the two names proportional to the gap between the bodies and
     * leaves the crossing a crossing. Everything that belongs to a body -- its
     * guard, its rest, its hips, its feet -- keeps its exact offset from that body,
     * because those are the figure's own dimensions and the figure has not changed
     * size.
     */
    public static double stretch(Anchor anchor) {
        return switch (anchor.site()) {
            case CONTACT, TILE -> anchor.x() * LANE_SPREAD;
            default -> stretch(anchor.x());
        };
    }

    /** The same, for a camera framing, which is quoted in tiles rather than in units. */
    public static double stretchTiles(double tiles) {
        return tiles * LANE_SPREAD;
    }

    private final Schedule schedule;
    private final List<Figure> figures;
    private final Map<Integer, Figure> byBody = new HashMap<>();

    /** Per body, per chain, the directives that drive it, in time order. */
    private final Map<Integer, Map<Chain, List<Directive.IkTarget>>> chains = new HashMap<>();
    private final Map<Integer, List<Directive.PoseChange>> poses = new HashMap<>();
    private final Map<Integer, List<Directive.FacingChange>> facings = new HashMap<>();
    private final Map<Integer, List<Directive.Impulse>> impulses = new HashMap<>();
    private final List<Directive.Ink> inks;
    private final List<Directive.TimeRamp> ramps;

    /** The pelvis of each body, carried on the trunk chain's own settle. */
    private final Map<Integer, Carry> pelvis = new HashMap<>();

    /** Where each body was standing before the score said anything about it. */
    private final Map<Integer, Double> origins = new HashMap<>();

    private double t;
    private double scale = 1.0;

    private final Vector2 scratch = new Vector2();

    public Director(Schedule schedule, List<Figure> figures) {
        this.schedule = schedule;
        this.figures = List.copyOf(figures);
        for (Figure f : figures) {
            byBody.put(f.body(), f);
            Map<Chain, List<Directive.IkTarget>> perChain = new EnumMap<>(Chain.class);
            for (Chain c : Chain.values()) {
                perChain.put(c, schedule.chain(f.body(), c));
            }
            chains.put(f.body(), perChain);
            poses.put(f.body(), schedule.of(Directive.PoseChange.class).stream()
                    .filter(d -> d.body() == f.body()).toList());
            facings.put(f.body(), schedule.of(Directive.FacingChange.class).stream()
                    .filter(d -> d.body() == f.body()).toList());
            impulses.put(f.body(), schedule.of(Directive.Impulse.class).stream()
                    .filter(d -> d.body() == f.body()).toList());
            pelvis.put(f.body(), new Carry(f.standX()));
            origins.put(f.body(), f.standX());
        }
        this.inks = schedule.of(Directive.Ink.class);
        this.ramps = schedule.of(Directive.TimeRamp.class);
    }

    public Schedule schedule() {
        return schedule;
    }

    public List<Figure> figures() {
        return figures;
    }

    /** Schedule time, in seconds. Not wall time: a held breath makes the two differ. */
    public double time() {
        return t;
    }

    /** The current clock scale. STYLE.md 7.3's held breath, and never zero. */
    public double timeScale() {
        return scale;
    }

    public Framing framing() {
        return schedule.framingAt(t);
    }

    /**
     * Puts every figure into the state the schedule describes at t = 0, with no
     * settle and no velocity anywhere.
     */
    public void start() {
        t = 0.0;
        for (Figure f : figures) {
            pelvis.get(f.body()).set(f.standX());
            drive(f, 0f);
            f.snap(0f);
            // Snap runs the solve from a cold pose; re-driving and re-snapping
            // lets the chains' own filters start on the target rather than on
            // wherever the bind pose left them, which is what stops frame zero
            // being a lurch.
            drive(f, 0f);
            f.snap(0f);
        }
    }

    /**
     * One frame.
     *
     * @param dt wall seconds. The schedule advances by {@code dt * timeScale()}.
     */
    public void advance(float dt) {
        scale = scaleAt(t);
        float step = (float) (dt * scale);
        t += step;
        for (Figure f : figures) {
            drive(f, step);
            f.solve(step);
            gusts(f);
            f.simulate(step, (float) t);
        }
    }

    /** Writes this frame's pose, placement and IK state for one figure. */
    private void drive(Figure f, float dt) {
        poseChannel(f);
        facingChannel(f);
        trunk(f, dt);
        f.writePose();
        arm(f);
        clavicle(f);
        leg(f, Chain.LEG_LEAD, f.ik().legL());
        leg(f, Chain.LEG_TRAIL, f.ik().legR());
        dissolve(f);
    }

    // -- the pose channel ------------------------------------------------------

    /**
     * The stance the body is holding, and how far into it it is.
     *
     * <p>{@code PoseChange} carries a stance, a gaze, a duration and a settle. The
     * stance and the duration are honoured here; <b>the gaze is not, and that is a
     * gap worth naming rather than papering over</b> -- there is no head-aim
     * channel in this rig. {@code RigIk} has chains for the trunk, the arm and both
     * legs and nothing for the neck or the eye, and STYLE.md 4b.6's gaze is a real
     * requirement that no layer below System 4 provides a mechanism for. What the
     * stance table does carry is the head's <em>attitude</em> per condition, which
     * is a coarser thing than a gaze and is what is available.
     */
    private void poseChannel(Figure f) {
        Directive.PoseChange active = null;
        for (Directive.PoseChange d : poses.get(f.body())) {
            if (d.at() > t) {
                break;
            }
            active = d;
        }
        if (active == null) {
            f.stance(Stance.READY, 1f);
            return;
        }
        // Two spans, and both are the staging layer's: the change takes
        // duration() to happen and settle() to stop happening. Blending across
        // their sum is what makes a stance a condition arrived at rather than a
        // state switched to.
        double span = Math.max(1e-6, active.duration() + active.settle());
        double u = (t - active.at()) / span;
        f.stance(active.stance(), (float) smooth(u));
    }

    /**
     * The facing, as a continuous quantity.
     *
     * <p>See {@link Figure#facing(float)}: a facing expressed as a boolean is the
     * one change in the whole directive vocabulary that can only ever snap, and
     * STYLE.md 7.2's first line is "no snapping".
     */
    private void facingChannel(Figure f) {
        Directive.FacingChange active = null;
        for (Directive.FacingChange d : facings.get(f.body())) {
            if (d.at() > t) {
                break;
            }
            active = d;
        }
        if (active == null) {
            return;
        }
        double u = active.duration() <= 0 ? 1.0 : (t - active.at()) / active.duration();
        double e = active.ease().apply(clamp01(u));
        f.facing((float) (active.from().step() + (active.to().step() - active.from().step()) * e));
    }

    // -- the trunk, which is also the body's position --------------------------

    /**
     * Drives the pelvis and the trunk chain from one directive.
     *
     * <p><b>Two seams meet here and both are reported rather than worked around.</b>
     *
     * <p>The first: {@code Stage} places every trunk anchor at {@code Y_HIP}, 0.98,
     * and {@code Chain.SPINE} is "hips to chest, <em>effector at the neck</em>",
     * about half a metre higher. Handing the chain its anchor verbatim asks the
     * neck to arrive at the pelvis, which the chain's own limits stop it doing and
     * which therefore leaves the trunk saturated against those limits on every
     * frame -- a constant maximal fold, with {@code hipLeaning}'s lean parameter
     * making no difference at all. So the anchor is read as what {@code Anchor.Site.HIP}
     * says it is, <em>the pelvis</em>, and the chain is aimed at the same point
     * carried up to the neck's own rest height.
     *
     * <p>The second: nothing in the vocabulary translates a body. A
     * {@code CombatEvent.Moved} becomes a trunk target at the new tile plus two
     * foot targets and three impulses -- and if the pelvis does not follow, a
     * figure crossing a tile is a figure whose legs stretch a metre while its hips
     * stay put. So the pelvis follows the trunk anchor, on {@code Chain.SPINE}'s
     * own {@link Settle#base()} of 0.30 s.
     *
     * <p>Those two readings together are what produces the lean, and produce it
     * without a number: the pelvis lags the anchor by 0.30 s while the neck target
     * tracks it exactly, so for the whole of a wind-up the neck is asked for a
     * point ahead of where the body has got to and the trunk bends into the
     * motion -- then unwinds as the pelvis arrives. That is STYLE.md 7.0.1's
     * spiral with the source in the right place ("the hip turns before the
     * shoulder"), and it is causally the right way round: a body accelerating
     * leans into it rather than being posed leaning.
     */
    private void trunk(Figure f, float dt) {
        IkChain spine = f.ik().spine();
        List<Directive.IkTarget> list = chains.get(f.body()).get(Chain.SPINE);
        // The opening stand position, as the place a first trunk directive moves
        // *from*.
        //
        // <b>This is a bug the knockback found and it was invisible anywhere
        // else.</b> A body that never takes a beat of its own -- the Wisp being
        // shoved -- has exactly one trunk directive in the whole score, the one
        // {@code Scheduler.moved} emits, and it has nothing before it. Easing from
        // "the directive's own target" then means easing from the destination,
        // which is not an ease at all: the pelvis's target teleports one tile and
        // only the carry filter's 0.30 s stands between the figure and a launch.
        // Measured, the shove completed in 0.14 s against STYLE.md 7.2's "arriving
        // over ~0.8 s", which is the launch that section forbids by name. Seeding
        // the ease with where the body already stands restores the 0.784 s the
        // schedule spent on it.
        Sample s = sampleAt(list, t, origin(f));
        Carry carry = pelvis.get(f.body());
        if (s == null) {
            carry.step(f.standX(), Chain.SPINE.settle().base(), dt);
            f.standAt(carry.value(), f.facing());
            return;
        }
        carry.step(s.x, Chain.SPINE.settle().base(), dt);
        f.standAt(carry.value(), f.facing());

        // The neck's own rest offset from the pelvis, mirrored with the figure, so
        // aiming the chain at "straight up from the anchor" preserves the authored
        // forward lean instead of quietly straightening it out.
        spine.target((float) (s.x + f.facing() * neckOffsetX(f)), (float) neckRestY(f));
        spine.weight((float) s.w);
        applySettle(spine, Chain.SPINE.settle());
    }

    // -- the sword arm ---------------------------------------------------------

    /**
     * The hand. One target, one weight, one settle profile, straight through.
     *
     * <p>This is the chain the vocabulary fits exactly, and it is also the one
     * carrying the parry: {@code Scheduler.bladeMet} aims the defender at its own
     * named {@code ContactPoint} a contact span <em>before</em> the crossing, holds
     * it through the crossing, and then aims it at a second point
     * {@code GIVE_GROUND} further along the lane over the rest of the contact span
     * and half the recovery. Three segments through one chain with continuous
     * weight is a curve, which is what STYLE.md 7.2 means by "the defender's arm
     * gives ground on an IK curve rather than stopping dead" -- and it is why the
     * director must interpolate between consecutive targets rather than snapping
     * to each as it starts. A held point, however softly eased into, is the
     * collision the section exists to forbid.
     */
    private void arm(Figure f) {
        IkChain sword = f.ik().swordArm();
        Sample s = sample(chains.get(f.body()).get(Chain.SWORD_ARM));
        if (s == null) {
            return;
        }
        sword.target((float) s.x, (float) s.y);
        sword.weight((float) s.w);
        applySettle(sword, Chain.SWORD_ARM.settle());
    }

    /**
     * The clavicle.
     *
     * <p>{@code Chain.CLAVICLE} names {@code RigIk.clavicle()}, which is an
     * {@code AimLink} and not an {@code IkChain}: {@code RigIk.update} overwrites
     * its target and its weight every frame from the sword arm's, deliberately, so
     * "callers set one target for the limb and get the whole shoulder-to-hand run
     * driven by it". A directive's clavicle <em>target</em> therefore cannot be
     * honoured and is not -- but its weight can, as the follow factor, and its
     * settle can. The scheduler emits a constant {@code CLAVICLE_WEIGHT} of 0.25,
     * which is {@code RigIk}'s own default, so nothing is lost today; a schedule
     * that varied it would be honoured.
     */
    private void clavicle(Figure f) {
        Sample s = sample(chains.get(f.body()).get(Chain.CLAVICLE));
        if (s == null) {
            return;
        }
        f.ik().clavicleWeight((float) s.w);
        f.ik().clavicle().settleSeconds((float) Chain.CLAVICLE.settle().base());
    }

    /**
     * A leg.
     *
     * <p>{@code Stage.footLead} and {@code footTrail} put the foot anchors on the
     * ground plane at y = 0, and {@code RigIk}'s leg chains end at the
     * <em>ankle</em> -- which sits 0.13 (near) and 0.22 (far) world units above the
     * ground in the bind pose, because the rig has feet. Both legs are also at
     * essentially full extension at bind: {@code RigIk} records "hip-to-ankle
     * measures 0.840 against a 0.840 reach". So aiming an ankle at the ground plane
     * asks each leg for 0.13 to 0.22 units it does not have, on every frame, and
     * the chain saturates -- planted feet become rigid stilts and the knee poles
     * stop meaning anything. The anchor is therefore read as the <em>sole</em>, and
     * the figure's own ankle height is added to it.
     *
     * <p>Read this way the anchors do what {@code system2-debt.md} lists first
     * among the things System 4 must not regress -- "planted feet under a moving
     * hip" -- because the anchors are world-fixed while the pelvis moves, so the
     * legs absorb the excursion instead of sliding with it.
     */
    private void leg(Figure f, Chain chain, IkChain leg) {
        Sample s = sample(chains.get(f.body()).get(chain));
        if (s == null) {
            return;
        }
        Vector2 ankle = ankleRest(f, chain);
        leg.target((float) s.x, (float) (s.y + ankle.y));
        leg.weight((float) s.w);
        applySettle(leg, chain.settle());
    }

    /**
     * {@code IkChain.settleSeconds} and {@code boneLagSeconds}, from the schedule's
     * {@link Settle}, exactly as {@code Directive.IkTarget}'s javadoc specifies.
     *
     * <p>Worth recording that this <b>overwrites the numbers {@code RigIk} chose</b>:
     * the trunk moves from 0.24 s to 0.30, the arm from 0.34 to 0.48, and the
     * arm's own wrist lag shrinks from 0.11 to 0.05. Those are not competing
     * opinions about the same quantity -- {@code RigIk}'s were tuned for one
     * gesture in isolation and its own note says so, while {@code Chain}'s are the
     * profile of a single kinematic run from pelvis to blade tip laid across
     * STYLE.md 7.1's whole band, which is the thing 7.0.3 is actually about. The
     * composite run stays strictly increasing either way -- 0.30, 0.35, 0.41, 0.44,
     * 0.48, 0.53 -- and only the schedule's version spends the band end to end.
     */
    static void applySettle(IkChain chain, Settle settle) {
        chain.settleSeconds((float) settle.base());
        int links = Math.min(chain.boneCount(), settle.links());
        for (int i = 0; i < links; i++) {
            chain.boneLagSeconds(i, (float) settle.lag(i));
        }
    }

    // -- the simulation --------------------------------------------------------

    /**
     * STYLE.md 7.3's "a change in how cloth trails", which is one of only four
     * things the section allows an impact to be expressed as.
     *
     * <p>An {@code Impulse} names a region, a direction and a magnitude, and says
     * when the <em>driver</em> acted; the surface answers {@code region.lag()}
     * later. Both halves are honoured: the gust read at time {@code t} is the
     * driver's value at {@code t - region.lag()}, so a hem thrown by a knockback
     * starts six frames after the hips and a sleeve five frames after the wrist,
     * with the anchors STYLE.md 7.1 insists on naming attached to both numbers.
     *
     * <p>It is an acceleration held over the driver's span and released, not a
     * velocity kick. A kick is a snap and 7.2 bans snapping; an acceleration that
     * rises, holds and goes leaves the chain to relax back on its own
     * {@code dragTau}, which is one soft return.
     */
    private void gusts(Figure f) {
        double hemX = 0;
        double hemY = 0;
        double sleeveX = 0;
        double sleeveY = 0;
        double hairX = 0;
        double hairY = 0;
        for (Directive.Impulse d : impulses.get(f.body())) {
            double u = (t - d.arrives()) / Math.max(1e-6, d.duration());
            if (u < 0 || u >= 1) {
                continue;
            }
            double env = envelope(u) * d.magnitude() * GUST;
            switch (d.region()) {
                case CLOTH_HEM -> {
                    hemX += d.dirX() * env;
                    hemY += d.dirY() * env;
                }
                case CLOTH_SLEEVE -> {
                    sleeveX += d.dirX() * env;
                    sleeveY += d.dirY() * env;
                }
                case HAIR -> {
                    hairX += d.dirX() * env;
                    hairY += d.dirY() * env;
                }
            }
        }
        f.sim().hemGust((float) hemX, (float) hemY);
        f.sim().sleeveGust((float) sleeveX, (float) sleeveY);
        f.sim().hairGust((float) hairX, (float) hairY);
    }

    /**
     * A gust's shape over its own span: up over the first fifth, then away.
     *
     * <p>Asymmetric on purpose -- STYLE.md 7.1's "anticipation is long, release is
     * smooth, recovery is long" applied to the air. A symmetric hump would put the
     * gust's peak in the middle of the beat, which is after the blow.
     */
    private static double envelope(double u) {
        if (u < 0.2) {
            return smooth(u / 0.2);
        }
        double v = (u - 0.2) / 0.8;
        return (1 - v) * (1 - v);
    }

    // -- ink -------------------------------------------------------------------

    /**
     * STYLE.md 7.3's shed flecks, on the material: "the dissolve threshold pushed
     * locally so the struck area sheds brush flecks that drift away".
     *
     * <p><b>Locally is the word this cannot honour and it is a real limitation.</b>
     * {@code u_dissolveBias} is a per-draw uniform over a whole mesh, so the push
     * is the whole figure's rather than the struck passage's. What it buys is
     * still the right thing at the right instant -- the garment's frayed edges
     * open further and shed for the length of the blow and close again -- and the
     * genuinely local half is carried by the flecks {@code InkFxRenderer} throws
     * from the contact point. Closing it properly wants a per-vertex or
     * per-region channel that the material does not currently have.
     */
    private void dissolve(Figure f) {
        double bias = 0;
        for (Directive.Ink d : inks) {
            if (d.body() != f.body() || t < d.at() || t >= d.end()) {
                continue;
            }
            double u = (t - d.at()) / Math.max(1e-6, d.duration());
            switch (d.kind()) {
                case FLECKS -> bias += d.magnitude() * 0.16 * (1 - u) * (1 - u);
                // Going is a monotone opening, not a pulse: STYLE.md 3 and 7.3
                // have the body leaving by dissolving rather than by falling.
                case DISSOLVE -> bias += d.magnitude() * 0.85 * smooth(u);
                default -> {
                }
            }
        }
        f.dissolveBias((float) Math.min(0.95, bias));
    }

    /**
     * Draws every ink mark alive at this instant.
     *
     * <p>{@code InkKind.TRAIL} is deliberately not drawn here. STYLE.md 5's
     * arc-trail already exists and is already better than anything this class
     * could add, because {@code InkSkinnedRenderer} builds it from the blade's own
     * sampled poses through a centripetal Catmull-Rom -- an actual swept path,
     * which is what the section requires ("it must curve"; "a straight one reads as
     * generic slash VFX and fails"). A second ribbon drawn from a directive's
     * origin and direction would be exactly that straight one. The directive is
     * therefore redundant against this renderer, which is worth reporting: it is
     * the one entry in the ink vocabulary the layer below did not need to emit.
     */
    public void renderInk(InkFxRenderer fx) {
        for (Directive.Ink d : inks) {
            if (t < d.at() || t >= d.end()) {
                continue;
            }
            float age = (float) ((t - d.at()) / Math.max(1e-6, d.duration()));
            float seed = seedOf(d);
            Figure f = byBody.get(d.body());
            float x = (float) stretch(d.origin());
            float y = (float) d.origin().y();
            switch (d.kind()) {
                case BLOOM -> fx.bloom(x, y, (float) d.dirX(), (float) d.dirY(),
                        (float) d.magnitude(), age, Palette.INDIGO_DEEP, seed);
                case FLECKS -> fx.flecks(x, y, (float) d.dirX(), (float) d.dirY(),
                        (float) d.magnitude(), age,
                        f == null ? Palette.INK_BLACK : f.clothMaterial().deep, seed);
                case CLASH -> fx.clash(x, y, (float) d.dirX(), (float) d.dirY(),
                        (float) d.magnitude(), age, seed);
                case SEAL -> fx.seal(x, y, (float) d.magnitude(), age, seed);
                case DISSOLVE, TRAIL -> {
                }
            }
        }
    }

    /** Stable per directive, so two runs of one schedule draw the same flecks. */
    private static float seedOf(Directive.Ink d) {
        return (float) (d.body() * 7.31 + d.kind().ordinal() * 13.7 + d.at() * 3.11);
    }

    // -- the clock -------------------------------------------------------------

    /**
     * STYLE.md 7.3's held breath: "a brief slowing of everything (a soft time
     * ramp, ~0.85x for ~0.25 s), never a hard freeze."
     *
     * <p>Two things make it a ramp rather than a switch. It is eased in and out
     * over its own span, so there is no frame on which the clock's rate steps --
     * a step in rate is a jerk, and a jerk at the moment of contact is hitstop
     * with a soft name. And it is bounded below by the directive's own scale,
     * which the scheduler never sets below 0.85 and which this method could not
     * take to zero even if it did: {@code TimeRamp} has no vocabulary for a freeze
     * and 7.1 and 10 both ban one outright.
     *
     * <p>Overlapping ramps multiply rather than add, so two blows landing close
     * together deepen the breath instead of restarting it. The scheduler already
     * refuses to emit them closer than {@code HELD_BREATH_SECONDS} apart, so in
     * practice this is the belt to that braces.
     */
    private double scaleAt(double at) {
        return clockScale(ramps, at);
    }

    /** Package-visible so {@code DirectorTest} can integrate the clock without a GL context. */
    static double clockScale(List<Directive.TimeRamp> ramps, double at) {
        double s = 1.0;
        for (Directive.TimeRamp r : ramps) {
            if (at < r.at() || at >= r.end()) {
                continue;
            }
            double u = (at - r.at()) / Math.max(1e-6, r.duration());
            // A raised cosine: zero slope at both ends, peak in the middle.
            double hump = 0.5 - 0.5 * Math.cos(2 * Math.PI * u);
            s *= 1.0 + (r.scale() - 1.0) * hump;
        }
        return Math.max(0.5, s);
    }

    // -- evaluating a chain ----------------------------------------------------

    /** One chain's state at {@link #t}: where the effector is asked to be, and how hard. */
    record Sample(double x, double y, double w) {
    }

    /**
     * The active segment of one chain, interpolated.
     *
     * <p>The weight is computed exactly as {@code Schedule.weight} computes it --
     * ramp between the endpoints, then hold -- and {@code DirectorTest} pins the
     * two against each other over a real schedule, because a director whose idea of
     * "how hard is this chain driving" differed from the schedule's would make
     * every phrasing assertion in the staging tests describe something that is not
     * on screen.
     *
     * <p>The <em>target</em> is interpolated between consecutive directives on the
     * same chain, with the incoming directive's ease. That is the half
     * {@code Schedule} does not compute and the half that makes a parry a curve:
     * three targets through one chain, each eased into the last, is an arc.
     */
    private Sample sample(List<Directive.IkTarget> list) {
        return sampleAt(list, t);
    }

    /** Package-visible so {@code DirectorTest} can pin it against {@code Schedule.weight}. */
    static Sample sampleAt(List<Directive.IkTarget> list, double t) {
        return sampleAt(list, t, Double.NaN);
    }

    /**
     * @param originX where the effector already is, for the case where the first
     *                directive on a chain has nothing before it to move from. NaN
     *                falls back to the directive's own target, which is right only
     *                when there genuinely is no earlier position -- see
     *                {@link #trunk}.
     */
    static Sample sampleAt(List<Directive.IkTarget> list, double t, double originX) {
        if (list.isEmpty()) {
            return null;
        }
        Directive.IkTarget active = null;
        Directive.IkTarget previous = null;
        for (Directive.IkTarget d : list) {
            if (t <= d.at()) {
                break;
            }
            previous = active;
            active = d;
        }
        if (active == null) {
            // Before the first directive: hold its opening state, so a chain is
            // never driven at a weight the schedule did not ask for.
            Directive.IkTarget first = list.get(0);
            double x = Double.isNaN(originX) ? stretch(first.target()) : originX;
            return new Sample(x, first.target().y(), first.weightFrom());
        }
        double u = active.duration() <= 0 ? 1.0 : (t - active.at()) / active.duration();
        double w = t >= active.end() ? active.weightTo()
                : active.ease().lerp(active.weightFrom(), active.weightTo(), u);
        // Each endpoint is stretched before the interpolation rather than after it:
        // the stretch is piecewise in the tile, so stretching a midpoint is not the
        // midpoint of two stretched ends, and a stroke crossing a tile boundary
        // would kink.
        double toX = stretch(active.target());
        double fromX = previous == null
                ? (Double.isNaN(originX) ? toX : originX)
                : stretch(previous.target());
        double fromY = previous == null ? active.target().y() : previous.target().y();
        double x = active.ease().lerp(fromX, toX, u);
        double y = active.ease().lerp(fromY, active.target().y(), u);
        return new Sample(x, y, w);
    }

    // -- rig geometry, read once off the bind pose ------------------------------

    private final Map<Integer, double[]> restCache = new HashMap<>();

    /** {@code [neckOffsetX, neckRestY, ankleLY, ankleRY]}, in the bind pose. */
    private double[] rest(Figure f) {
        return restCache.computeIfAbsent(f.body(), k -> {
            // Read off a throwaway bind skeleton rather than the live one: the live
            // one is posed, placed and mirrored by the time anything asks.
            var bind = dev.starfall.rig.SamuraiRig.buildSkeletonOnly();
            Vector2 v = new Vector2();
            Bone hips = bind.bone("hips");
            bind.worldPosition(hips.index, v);
            double hipX = v.x;
            double hipY = v.y;
            bind.worldPosition(bind.bone("neck").index, v);
            double neckDx = v.x - hipX;
            double neckY = v.y;
            bind.worldPosition(bind.bone("footL").index, v);
            double ankleL = v.y;
            bind.worldPosition(bind.bone("footR").index, v);
            double ankleR = v.y;
            // The trunk anchors are authored against Stage.Y_HIP rather than
            // against this rig's pelvis, and the two differ by a couple of
            // millimetres. Carrying the difference keeps the neck target at the
            // height the rig actually has rather than the height the map assumes.
            double lift = dev.starfall.stage.Stage.Y_HIP - hipY;
            return new double[] {neckDx, neckY + lift, ankleL, ankleR};
        });
    }

    private double origin(Figure f) {
        return origins.getOrDefault(f.body(), f.standX());
    }

    private double neckOffsetX(Figure f) {
        return rest(f)[0];
    }

    private double neckRestY(Figure f) {
        return rest(f)[1];
    }

    private Vector2 ankleRest(Figure f, Chain chain) {
        double[] r = rest(f);
        return scratch.set(0f, (float) (chain == Chain.LEG_LEAD ? r[2] : r[3]));
    }

    // -- helpers ---------------------------------------------------------------

    private static double clamp01(double u) {
        return u <= 0 ? 0 : u >= 1 ? 1 : u;
    }

    private static double smooth(double u) {
        double c = clamp01(u);
        return c * c * (3 - 2 * c);
    }

    /** Every body the schedule mentions that this director was given a figure for. */
    public List<Integer> bodies() {
        List<Integer> out = new ArrayList<>(byBody.keySet());
        out.sort(Integer::compare);
        return out;
    }
}
