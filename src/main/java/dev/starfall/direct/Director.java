package dev.starfall.direct;

import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.Bone;
import dev.starfall.art.Palette;

import dev.starfall.ik.IkChain;
import dev.starfall.render.InkFxRenderer;
import dev.starfall.rig.SamuraiRig;
import dev.starfall.stage.Anchor;
import dev.starfall.stage.Chain;
import dev.starfall.stage.Directive;
import dev.starfall.stage.Ease;
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
     *
     * <h2>1.55 to 1.35, System 4 pass 5, and the refusal that carried it is void</h2>
     *
     * <p>Pass 2 raised this to 1.55 to stop the two figures merging. Pass 3 shot 1.35
     * and reported the parry destroyed; pass 4 re-shot it with the longer blade, found
     * the blades met <em>better</em> at 1.35 (0.0068 of a figure height against the
     * shipped 0.0182), and kept the refusal anyway on the merge count -- <i>"8 of 24
     * at 1.35, 1 of 24 at 1.55"</i>.
     *
     * <p><b>That comparison crossed spans.</b> The pass-4 review ran both settings
     * through the project's own tool consistently and got 1 of 24 either way with
     * {@code --span}, and 8 of 24 at 1.35 against <b>17</b> of 24 at 1.55 without it.
     * Taken consistently the lower spread is equal or twice better on the merge, which
     * was the last surviving half of the instruction not to lower it.
     *
     * <p>And it is the only lever on the thing pass 4's own corrected measurement
     * says is worst: through the 329-row span its capture stood the duellists
     * <b>1.048</b> figure heights apart at the skirt, against the corpus's 0.582 to
     * 0.614 -- 1.7x, not the 1.3x the record claimed. The stand separation is
     * {@code LANE_SPREAD * TILE_WIDTH / FIGURE_HEIGHT}: 0.912 at 1.55 and <b>0.794</b>
     * at 1.35. Still above the corpus, and the structural answer is still the
     * {@code Stage} change above -- 1.05 would be needed to match the corpus outright,
     * and that is a combat-design decision, not a visual one.
     *
     * <p><b>What it costs is measured and recorded, not assumed:</b> the framing is
     * stretched by the same factor, so at 1.35 the intimate shot is 13% narrower and
     * the figure lands at 378 rows of a 720-row frame rather than 329. See
     * {@code ParryWindowTest}, which recomputes the span from this constant, and
     * {@code docs/system4-debt.md} for the shipped comparison against
     * {@code out/captures/s4-p5-spread155}.
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
            case CONTACT, CROSSING, TILE -> anchor.x() * LANE_SPREAD;
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

    /**
     * Continues this performance under a longer score.
     *
     * <h2>Why this exists: a played fight grows its schedule</h2>
     *
     * <p>Every scene before the input loop built its whole {@link Schedule} up
     * front and played it once. A <em>played</em> fight cannot: each command
     * extends the score, and the {@code Scheduler} explicitly accumulates one
     * continuous timeline for exactly this reason. What must survive the
     * extension is everything the schedule does not carry -- the clock, the
     * pelvis carries, the opening origins, last frame's fists -- because those
     * are the performance's own state, and resetting any of them mid-fight is a
     * teleport, which STYLE.md 7.2 bans as the first word of its first rule.
     *
     * <p>The new schedule contains every directive the old one did (the scheduler
     * only appends, and {@code notBefore} can only move beats later), so replaying
     * it from the preserved clock is the same evaluation the old director would
     * have made -- pinned by {@code SessionTest}, which rescored a director
     * mid-run against an unrescored twin and asserted identical bodies.
     *
     * @return a new director over the same figures, mid-performance
     */
    public Director rescore(Schedule next) {
        Director d = new Director(next, figures);
        d.t = this.t;
        d.scale = this.scale;
        d.origins.clear();
        d.origins.putAll(this.origins);
        // The Carry objects themselves, not copies of their targets: a carry is a
        // filter with state, and handing the new director fresh ones would let
        // every pelvis snap to its target on the next frame.
        d.pelvis.clear();
        d.pelvis.putAll(this.pelvis);
        for (Map.Entry<Integer, Vector2> e : this.hilts.entrySet()) {
            d.hilts.put(e.getKey(), new Vector2(e.getValue()));
        }
        d.crossing.set(this.crossing);
        d.drawnCrossing.set(this.drawnCrossing);
        return d;
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
        rememberHilts();
        rememberCrossing();
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
        rememberHilts();
        rememberCrossing();
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
     * <p>{@code PoseChange} carries a stance, a gaze, a duration and a settle. All
     * three channels are honoured now: the stance and duration as before, and —
     * since System 3b — <b>the gaze</b>, which this method resolves through the
     * same lane stretch every other anchor gets and hands to the figure's
     * {@link dev.starfall.rig.FaceRig}. For four systems this javadoc recorded the
     * gaze as "a gap worth naming rather than papering over"; the face sub-rig is
     * the mechanism it was waiting for, and the schedule needed no change at all,
     * because the {@code Scheduler} has authored a gaze anchor on every pose
     * change since System 4 (STYLE.md 4b.6: "intent — who this character is about
     * to act on").
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
        if (active.gaze() != null) {
            f.gazeAt(stretch(active.gaze()), active.gaze().y());
        }
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
        Segment seg = segmentAt(chains.get(f.body()).get(Chain.SWORD_ARM), t);
        if (seg == null) {
            return;
        }
        // Each endpoint is mapped from what it *names* to what this chain can
        // *hold* before the interpolation, exactly as the trunk and the legs
        // already do. See #fist.
        double toX = fistX(f, seg.active().target());
        double toY = fistY(f, seg.active().target());
        double fromX = seg.previous() == null ? toX : fistX(f, seg.previous().target());
        double fromY = seg.previous() == null ? toY : fistY(f, seg.previous().target());
        Ease ease = seg.active().ease();
        sword.target((float) ease.lerp(fromX, toX, seg.u()), (float) ease.lerp(fromY, toY, seg.u()));
        sword.weight((float) seg.w());
        applySettle(sword, Chain.SWORD_ARM.settle());
        bladeAim(f, seg);
    }

    /**
     * STYLE.md 7.2's parry, at last taken literally: the <em>blade</em> goes to the
     * crossing.
     *
     * <p>An {@link Anchor.Site#CROSSING} target is a point the blade must pass
     * through. Two things carry that, and they are separate on purpose:
     *
     * <ul>
     *   <li>{@link #fist} puts the hand a blade's-crossing-length back from the
     *       point along the direction the figure's own guard reaches it from, so
     *       the crossing lands near the monouchi rather than on the hilt or past
     *       the kissaki. That is a <em>position</em> and it is what an IK chain
     *       whose effector is the fist can express.</li>
     *   <li>{@link RigIk#blade()} then turns the blade so its axis runs through the
     *       point. That is a <em>direction</em>, and nothing in the rig could
     *       express it until this pass.</li>
     * </ul>
     *
     * <p>Both are needed and neither is sufficient. The position alone leaves the
     * blade at whatever angle the forearm ended up at plus 45 degrees, which is
     * how pass 1 shipped a defender whose blade pointed down and away through the
     * whole contact span. The direction alone would point a blade at a crossing it
     * is too far from to reach, and the two segments would still not meet.
     *
     * <p>The aim's weight is the chain's, scaled by how far into the crossing
     * directive the clock is, so the blade turns into the meeting over the same
     * span the arm travels and lets go over the recovery. It is never switched.
     */
    private void bladeAim(Figure f, Segment seg) {
        boolean toCrossing = seg.active().target().site() == Anchor.Site.CROSSING;
        boolean fromCrossing = seg.previous() != null
                && seg.previous().target().site() == Anchor.Site.CROSSING;
        double aim = seg.active().ease().lerp(fromCrossing ? 1.0 : 0.0, toCrossing ? 1.0 : 0.0, seg.u());
        Anchor point = toCrossing ? seg.active().target()
                : fromCrossing ? seg.previous().target() : null;
        if (point == null || aim <= 0) {
            f.ik().aimBlade(0f, 0f, 0f);
            return;
        }
        Vector2 c = crossing(point);
        f.ik().aimBlade(c.x, c.y, (float) (aim * seg.w()));
        // The wrist and the blade take links 2 and 3 of the directive's own settle
        // profile -- which is exactly what Chain.SWORD_ARM declares them to be:
        // "extended by two links the chain does not solve but the eye reads: the
        // hand, and the blade tip hanging off it". Before this pass those two
        // arrivals were written down and driven by nothing at all.
        Settle settle = seg.active().settle();
        f.ik().wrist().settleSeconds((float) settle.arrival(Math.min(2, settle.links() - 1)));
        f.ik().blade().settleSeconds((float) settle.arrival(settle.links() - 1));
    }

    /**
     * How far the crossing point sits from the fist, along the blade:
     * {@link SamuraiRig#BLADE_GRIP_OFFSET} out of the fist to the habaki, then
     * {@link Figure#BLADE_CROSSING} of the nagasa to where two blades bind.
     */
    public static final double REACH_TO_CROSSING =
            SamuraiRig.BLADE_GRIP_OFFSET + Figure.BLADE_CROSSING * SamuraiRig.BLADE_LENGTH;


    /** Where the two blades are actually being made to meet this frame. */
    private final Vector2 crossing = new Vector2();

    /** Last frame's fists, per body. See {@link #crossing(Anchor)}. */
    private final Map<Integer, Vector2> hilts = new HashMap<>();

    /**
     * The point in space the two blades are aimed at, resolved against where those
     * blades actually are.
     *
     * <h2>Why the schedule's own coordinate is not good enough, and why that is
     * this layer's problem rather than the schedule's</h2>
     *
     * <p>{@code ContactPoint}: <i>"the renderer owns the mapping from that to a
     * point in space, and two renderers at different figure scales can both honour
     * it."</i> That sentence is doing real work here. The staging layer resolves the
     * engine's two ordinal names into one world point, and it does so <em>without
     * knowing where the arms ended up</em> -- it cannot know, because
     * {@code IkChain.weight} blends local rotations rather than effector positions,
     * so an arm at 0.85 driving a reachable target still finishes a fifth of a body
     * height from it, and a stance blend pulls the other way the whole time.
     *
     * <p>A fixed world coordinate handed to two rigs is therefore a coordinate
     * neither of them lands on, and a blade told to point at somewhere behind its
     * own habaki saturates its wrist and points somewhere else entirely. What is
     * invariant is the <em>relation</em>: two blades leave their hilts at
     * {@link #BLADE_ELEVATION_DEG} and meet above the line between them, which is
     * the X reference image 3 draws. So the crossing is the apex of that X, built
     * from where the two hilts actually are.
     *
     * <p><b>The schedule is still the authority on where the beat happens.</b>
     * {@code RehearsalTest} asserts that the resolved crossing stays within a tenth
     * of a figure height of the anchor the scheduler emitted, so a staging that put
     * the crossing inside a body or off the lane fails a test rather than quietly
     * re-staging itself.
     *
     * <p>Roots are last frame's, because {@code Figure.writePose} has already reset
     * the skeleton to its stance by the time this runs. At the 240 Hz substep that
     * is 4 ms of lag on a quantity that moves a few millimetres per step.
     */
    private Vector2 crossing(Anchor scheduled) {
        double cx = stretch(scheduled);
        double cy = scheduled.y();
        Vector2 a = null;
        Vector2 b = null;
        double sx = 0;
        // (kept for the fallback branch: which way the first figure faces)
        for (Figure f : figures) {
            Vector2 r = hilts.get(f.body());
            if (r == null) {
                continue;
            }
            if (a == null) {
                a = r;
                sx = f.facing();
            } else if (b == null) {
                b = r;
            }
        }
        if (a == null || b == null) {
            return crossing.set((float) cx, (float) cy);
        }
        // The apex of the X: each blade leaves its own hilt at BLADE_ELEVATION_DEG
        // to the line joining the two hilts, exactly as reference image 3 draws it,
        // and the two meet above the middle of that line. Building it from the
        // angle rather than from a fixed distance along the blade is what makes the
        // aim nearly free: the direction asked for is close to the direction the
        // blade already wants, so the wrist trims rather than saturating.
        //
        // <b>A ray-intersection construction was tried and measured worse.</b>
        // Intersecting the two rays each figure's blade "wants" (elevation measured
        // against the horizon rather than against the hilt line) is the more
        // obvious model and reads better on paper. On this rig it put the apex
        // where one blade could reach it and the other could not, and the minimum
        // separation went from 0.0% of a figure height to 3.0%. Recorded because
        // the obvious construction being the worse one is the sort of thing this
        // project keeps rediscovering.
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1e-4 || d > 2 * SamuraiRig.BLADE_LENGTH) {
            // Hilts further apart than two blades cannot be bridged, and hilts on
            // top of each other carry no direction. Fall back to what the schedule
            // asked for and let the guard report it.
            return crossing.set((float) cx, (float) cy);
        }
        double half = d / 2;
        double h = half * Math.tan(Math.toRadians(BLADE_ELEVATION_DEG));
        // Never past the kissaki: a crossing beyond the blade's own length is one
        // the blade cannot make. 0.9 leaves the last tenth clear so the two points
        // do not read as tips touching.
        double span = Math.sqrt(half * half + h * h);
        double limit = 0.9 * SamuraiRig.BLADE_LENGTH;
        if (span > limit) {
            h *= limit / span;
        }
        // The normal, taken upward: two blades meet above the line between their
        // hilts, never below it.
        double nx = -dy / d;
        double ny = dx / d;
        if (ny < 0) {
            nx = -nx;
            ny = -ny;
        }
        return crossing.set((float) (0.5 * (a.x + b.x) + nx * h),
                (float) (0.5 * (a.y + b.y) + ny * h));
    }

    /**
     * Where the clash mark is drawn: the point at which the two blades actually
     * cross, this frame.
     *
     * <h2>Why this is not {@link #crossing}, which is the whole of pass 3's item 3</h2>
     *
     * <p>{@link #crossing} is the point the two blades are <em>aimed</em> at, and it
     * is built from the two fists rather than from the two blades on purpose: built
     * from the blades it is a fixed point of the aim it drives, and it chattered.
     * That construction is right for an aim and wrong for a light. Measured on the
     * parry with the headless rehearsal, on the frame the bloom ignites the aim point
     * sits <b>0.165 world units from the hero's blade and 0.266 from the foe's</b> —
     * 9.7% and 15.7% of a figure height — while the two blade <em>segments</em>
     * intersect. STYLE.md §5 puts the star at the point of contact; drawing it at the
     * aim point draws it in open paper above both blades, which is what the pass-2
     * review measured in delivered pixels frame by frame ("on its first two frames it
     * is in open paper, 0.09-0.19 of a figure height from both").
     *
     * <p>So the light reads the blades. It is downstream of everything — nothing aims
     * at it, nothing is filtered against it — so there is no loop to close.
     */
    public Vector2 lastCrossing(Vector2 out) {
        return out.set(drawnCrossing);
    }

    /** Where the two blades actually cross this frame. See {@link #lastCrossing}. */
    private final Vector2 drawnCrossing = new Vector2();

    private final Vector2 bladeA0 = new Vector2();
    private final Vector2 bladeA1 = new Vector2();
    private final Vector2 bladeB0 = new Vector2();
    private final Vector2 bladeB1 = new Vector2();

    /**
     * Recomputes {@link #drawnCrossing} from the two blades' world segments.
     *
     * <p>The midpoint of the closest approach: the two points, one on each segment,
     * that are nearest each other. When the segments genuinely cross this is the
     * crossing; when they do not it is the middle of the gap, which is the honest
     * place for a light that claims they are meeting and is <em>between</em> them
     * rather than welded to one. With only one figure on stage there is nothing to
     * cross, and the aim point stands.
     */
    private void rememberCrossing() {
        if (figures.size() < 2) {
            drawnCrossing.set(crossing);
            return;
        }
        Figure a = figures.get(0);
        Figure b = figures.get(1);
        a.bladeAt(0f, bladeA0);
        a.bladeAt(1f, bladeA1);
        b.bladeAt(0f, bladeB0);
        b.bladeAt(1f, bladeB1);
        closestPoints(bladeA0, bladeA1, bladeB0, bladeB1, drawnCrossing);
    }

    /** Midpoint of the closest approach between two segments. */
    static Vector2 closestPoints(Vector2 p1, Vector2 p2, Vector2 q1, Vector2 q2, Vector2 out) {
        double best = Double.MAX_VALUE;
        double bx = 0;
        double by = 0;
        // Four point-to-segment feet plus the proper intersection, which is the case
        // that matters: when the segments cross, the crossing is the answer and the
        // four feet are not.
        double d1 = side(q1, q2, p1);
        double d2 = side(q1, q2, p2);
        double d3 = side(p1, p2, q1);
        double d4 = side(p1, p2, q2);
        if (((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0))) {
            double u = d1 / (d1 - d2);
            return out.set((float) (p1.x + u * (p2.x - p1.x)), (float) (p1.y + u * (p2.y - p1.y)));
        }
        Vector2[][] pairs = {
                {p1, q1, q2}, {p2, q1, q2}, {q1, p1, p2}, {q2, p1, p2},
        };
        for (Vector2[] trio : pairs) {
            double[] foot = footOn(trio[1], trio[2], trio[0]);
            double dx = trio[0].x - foot[0];
            double dy = trio[0].y - foot[1];
            double d = dx * dx + dy * dy;
            if (d < best) {
                best = d;
                bx = 0.5 * (trio[0].x + foot[0]);
                by = 0.5 * (trio[0].y + foot[1]);
            }
        }
        return out.set((float) bx, (float) by);
    }

    private static double side(Vector2 a, Vector2 b, Vector2 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static double[] footOn(Vector2 a, Vector2 b, Vector2 p) {
        double vx = b.x - a.x;
        double vy = b.y - a.y;
        double len2 = vx * vx + vy * vy;
        double u = len2 < 1e-12 ? 0 : ((p.x - a.x) * vx + (p.y - a.y) * vy) / len2;
        u = Math.max(0, Math.min(1, u));
        return new double[] {a.x + u * vx, a.y + u * vy};
    }

    /**
     * Records where each figure's fist finished this frame.
     *
     * <p><b>The fist and not the blade root, and the difference is a stability
     * result rather than a preference.</b> The habaki's position depends on the
     * hand's rotation, which the wrist aim writes, which is driven by the crossing,
     * which is built from the habaki -- a loop, and it behaves like one: built from
     * the blade roots the crossing chattered and both aims saturated chasing it.
     * The fist is upstream of the aim entirely, so the crossing built from it is a
     * fixed point of nothing and simply sits where the two hands put it.
     */
    private void rememberHilts() {
        for (Figure f : figures) {
            f.where("handL", scratch);
            hilts.computeIfAbsent(f.body(), k -> new Vector2()).set(scratch);
        }
    }

    /**
     * How high the blade rides out of the guard, above horizontal, in degrees.
     *
     * <p>Read off the corpus rather than tuned. In reference image 3 the left
     * duellist's blade runs from a tsuba at (390, 465) to a kissaki at (560, 135)
     * and the right one's mirrors it: 62 and 61 degrees above horizontal, out of
     * two hands almost touching at chest height. The rig is close to that on its
     * own -- the forearm comes up out of a tucked elbow and the blade's bind angle
     * adds 45 -- so the wrist has a few degrees to trim rather than a hundred.
     *
     * <p><b>Deriving this from the geometry instead was tried and is wrong.</b> The
     * obvious construction is "the direction from the body's own guard point to the
     * crossing", which is self-consistent and needs no constant. It degenerates:
     * with the two bodies squared up, the guard point sits almost directly under
     * the crossing, so the derived direction is nearly vertical, the fist target
     * lands 0.6 units in front of a shoulder that reaches 0.56, and the arm
     * saturates a third of a body height short of it. A constant elevation is the
     * honest thing here -- how a swordsman holds a sword is not a fact about where
     * the other one is standing.
     */
    public static final double BLADE_ELEVATION_DEG = 62.0;


    /**
     * Where the fist has to be for the blade to cross at {@code anchor}.
     *
     * <p>Back down the blade from the crossing: {@link #REACH_TO_CROSSING} along
     * the direction {@link #BLADE_ELEVATION_DEG} says the blade rides at, mirrored
     * with the figure. That puts the hand about a third of a body height in front
     * of the body at chest height, holding a blade angled up into the meeting,
     * which is the pose the corpus holds and -- not coincidentally -- one this
     * arm can actually reach.
     */
    private void fist(Figure f, Anchor anchor, Vector2 out) {
        double cx = stretch(anchor);
        double cy = anchor.y();
        if (anchor.site() != Anchor.Site.CROSSING) {
            out.set((float) cx, (float) cy);
            return;
        }
        double rad = Math.toRadians(BLADE_ELEVATION_DEG);
        double dx = Math.signum(f.facing()) * Math.cos(rad);
        double dy = Math.sin(rad);
        out.set((float) (cx - dx * REACH_TO_CROSSING),
                (float) (cy - dy * REACH_TO_CROSSING * FIST_DROP));
    }

    /**
     * How far below the crossing the fist is placed, as a fraction of the full
     * blade-length drop.
     *
     * <h2>Measured both ways, and 1.0 is right for the picture even though 0.0 is
     * better for the geometry</h2>
     *
     * <p>At 1.0 -- the fist a whole {@link #REACH_TO_CROSSING} back down the blade,
     * which is where the blade's own length puts it -- the hand target lands about
     * 0.74 world units from a shoulder whose arm reaches 0.56, so both arms saturate
     * and the bind is held by the blade aim rather than by the hands. Setting this to
     * 0.0 puts the fists at the crossing's own height, brings both hand targets inside
     * the arm's reach, and lengthens the bind measurably: the two blade <em>segments</em>
     * intersect for <b>0.118 s against 0.066 s</b>, 1.8x.
     *
     * <p>It also makes the picture worse, which is why it is not the value here.
     * Shot at 0.0 (this capture is not kept; the numbers are):
     * <ul>
     *   <li>minimum blade separation in delivered pixels <b>0.0287 -> 0.0388</b> of a
     *       figure height, a 35% regression on the one number the pass-2 review named
     *       as that pass's single biggest visual improvement;</li>
     *   <li>the defender's blade <b>disappears entirely</b> on frames 11, 14, 15, 18 and
     *       19 of the contact window -- {@code analyse blades} falls through to a
     *       background mote as the second cool-bright cloud -- against a defender's
     *       blade present on every frame at 1.0.</li>
     * </ul>
     *
     * <p>STYLE.md's own rule decides it: {@code Rehearsal} "is not a substitute for
     * pixels and must never be quoted as one". The geometry preferred 0.0 and the
     * picture preferred 1.0, and the picture wins. Recorded here rather than in a
     * commit message because the next pass that tries to give the parry a span will
     * reach for exactly this lever.
     */
    public static final double FIST_DROP = 1.0;

    private final Vector2 fistScratch = new Vector2();

    private double fistX(Figure f, Anchor anchor) {
        fist(f, anchor, fistScratch);
        return fistScratch.x;
    }

    private double fistY(Figure f, Anchor anchor) {
        fist(f, anchor, fistScratch);
        return fistScratch.y;
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
            if (d.origin().site() == Anchor.Site.CROSSING) {
                // <b>The bloom is drawn where the blades are, not where the score
                // hoped they would be.</b> Pass 1 fired this mark two frames after
                // the closest approach, 36 px from the attacker's own grip and
                // 223 px from the defender's blade -- "a light that asserts an
                // event the picture does not contain". Reading the same resolved
                // crossing the two aims used makes the assertion true by
                // construction: the mark cannot land anywhere but on the meeting.
                // <b>{@link #drawnCrossing}, not {@link #crossing}.</b> The first is
                // where the two blades actually cross this frame; the second is the
                // point they are aimed at, built from the two fists, which sits 0.10
                // to 0.27 world units away from either blade through the whole bind.
                // Getting this one line wrong is how System 4 pass 3 briefly shipped a
                // guard reading `lastCrossing` while the renderer drew something else
                // -- STYLE.md 11.2b(f)'s "a test that shares its input with the code
                // under test" in reverse, and it was caught by looking at the pixels.
                x = drawnCrossing.x;
                y = drawnCrossing.y;
            }
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
    /**
     * The two directives the clock is between on one chain, and where in the
     * crossfade it is.
     *
     * <p>Split out of {@link #sampleAt} because {@link #arm} has to map each
     * endpoint from what the anchor <em>names</em> to what the chain can
     * <em>hold</em> before interpolating, and a {@link Sample} has already thrown
     * the anchors away. That mapping is not a detail: an
     * {@link Anchor.Site#CROSSING} names a point on a blade and the chain's
     * effector is a fist, and interpolating in the wrong space is how the two
     * ended up 0.47 units apart.
     */
    record Segment(Directive.IkTarget active, Directive.IkTarget previous, double u, double w) {
    }

    static Segment segmentAt(List<Directive.IkTarget> list, double t) {
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
            // Before the first directive, hold its opening state -- the same rule
            // #sampleAt follows, so a chain is never driven at a weight nobody
            // asked for and never lurches on the frame the first directive starts.
            Directive.IkTarget first = list.get(0);
            return new Segment(first, null, 0.0, first.weightFrom());
        }
        double u = active.duration() <= 0 ? 1.0 : (t - active.at()) / active.duration();
        double w = t >= active.end() ? active.weightTo()
                : active.ease().lerp(active.weightFrom(), active.weightTo(), u);
        return new Segment(active, previous, u, w);
    }

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
