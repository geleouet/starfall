package dev.starfall.stage;

import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.ContactPoint;
import dev.starfall.combat.Dissolve;
import dev.starfall.combat.Focus;
import dev.starfall.combat.Force;
import dev.starfall.combat.Meeting;
import dev.starfall.combat.Overlap;
import dev.starfall.combat.Phases;
import dev.starfall.combat.Resolution;
import dev.starfall.combat.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Turns an ordinal event stream into a timed, positioned schedule of animation
 * directives. This is System 4's hinge.
 *
 * <h2>What it is for</h2>
 *
 * <p>combat-design.md 3d.5, on what the engine deliberately does not do: <i>"The
 * event stream is ordinal: turns, tiles, proportions and sides. No seconds, no
 * pixels, no world coordinates. Mapping that to space and time is the animation
 * layer's job, and the moment the engine learns what a second is, the
 * reproducibility the whole review loop depends on becomes a rendering
 * concern."</i> This class is that mapping and nothing else. It draws nothing,
 * holds no GL context, and never touches {@code CombatState}: everything it knows
 * about the board it learns from {@link CombatEvent.Moved} and
 * {@link CombatEvent.Turned}, which is why a schedule stays correct when it is
 * consumed several frames after it was built.
 *
 * <h2>The four properties it is graded on</h2>
 *
 * <ol>
 *   <li><b>The overlap theorem survives the mapping.</b> The engine proved that a
 *       beat honouring its {@link Overlap} hint contacts after the previous beat
 *       did, "however the renderer scales the beats". The scaling lives in
 *       {@link ScheduledBeat#after}, which consumes exactly the fraction of the
 *       previous <em>recovery</em> the hint permits and never more, so the theorem
 *       transfers unchanged.</li>
 *   <li><b>Nothing arrives at the same time, inside a chain as well as between
 *       bodies.</b> Every {@link Directive.IkTarget} carries a {@link Settle}
 *       spanning STYLE.md 7.1's band rather than a single number, which is
 *       {@code system2-debt.md} E3 closed.</li>
 *   <li><b>Motion has a source, across a whole phrase.</b> The spine is staged
 *       first in every beat and its weight never returns to zero between the beats
 *       of one bracket, so five clauses are one gesture rather than five.</li>
 *   <li><b>Camera framing derives from lane length.</b> See {@link Stage}.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *   Scheduler s = new Scheduler(new Stage(laneLength), opening);
 *   s.accept(engine.apply(Command.execute()));
 *   Schedule score = s.schedule();
 * }</pre>
 *
 * <p>A scheduler accumulates: feeding it several resolutions produces one
 * continuous timeline, which is what lets the hip lead the hand across a phrase
 * boundary rather than only inside one.
 */
public final class Scheduler {

    // -- weights ---------------------------------------------------------------

    /**
     * How hard the trunk chain drives. A lean, not a re-pose -- {@code RigIk}:
     * "tight limits plus a partial weight are what turn this chain into a lean
     * rather than a re-pose."
     */
    public static final double CARRIER_WEIGHT = 0.55;

    /**
     * The floor the carrier drops to between brackets but never inside one. Zero
     * would make each beat a separate event; this is what makes a phrase a phrase.
     */
    public static final double CARRIER_HOLD = 0.22;

    /**
     * Peak weight on the sword arm, and deliberately not 1. STYLE.md 7.0.2: "a limb
     * that extends fully because a coordinate permitted it reads as a linkage. A
     * limb that declines to straighten reads as a brushstroke." Full weight hands
     * the solver's reach ceiling the last word; 0.85 leaves the authored pose a
     * share of every frame.
     */
    public static final double ARM_PEAK = 0.85;

    /** What the arm falls back to between beats of one bracket. */
    public static final double ARM_HOLD = 0.30;

    /** {@code RigIk}'s own clavicle weight: it carries attitude, not reach. */
    public static final double CLAVICLE_WEIGHT = 0.25;

    /** Feet are planted, not driven. {@code system2-debt.md}: they must not slide. */
    public static final double LEG_WEIGHT = 0.70;

    // -- distances -------------------------------------------------------------

    /** How far the pelvis leads into a beat, in tiles. Small: it is a shift of weight. */
    public static final double HIP_LEAN = 0.16;

    /** How far a parried arm gives ground, in tiles. STYLE.md 7.2's deflection curve. */
    public static final double GIVE_GROUND = 0.30;

    /**
     * The settle profile a blade travelling into a crossing is driven with.
     *
     * <h2>Why the strike gets its own, and why it is legal</h2>
     *
     * <p>{@link Chain#SWORD_ARM}'s profile spends the top of STYLE.md 7.1's band --
     * 0.48 to 0.60 -- and that is right for a gesture coming to rest. It is not a
     * rest here. {@code IkChain} implements {@code settleSeconds} as the time
     * constant of the filter that <em>chases</em> the target, so 0.48 s means the
     * hand covers about a fifth of its journey in a 0.168 s contact span. Measured
     * on pass 1: the attacker's fist was still 0.62 world units from the crossing
     * when its own beat's contact span ended, so the blades could not have met
     * however well they were aimed.
     *
     * <p>0.30 to 0.39 is inside 7.1's band, strictly increasing down the links, so
     * 7.0.3's actual requirement -- "settle times differ down the chain rather than
     * being shared" -- holds exactly as before; what changes is that the run is
     * spent at the bottom of the band rather than the top, for the one beat in the
     * game that is a strike rather than a settle. 7.1's own instruction is that the
     * band is "a range to spend, not a tolerance to sit inside", and 7.2 is the
     * tie-breaker where two sections pull against each other: <b>a parry that does
     * not happen is a worse failure than a wrist that arrives 90 ms early.</b>
     *
     * <p>The recovery target after the crossing keeps {@link Chain#SWORD_ARM}'s own
     * profile, so the long settle is still there where 7.1 asks for it -- after the
     * visible motion has ended.
     */
    public static final Settle STRIKE = Settle.of(0.30, 0.33, 0.36, 0.39);

    /** STYLE.md 5: a swung blade's trail "fades over ~0.4 s". */
    public static final double TRAIL_SECONDS = 0.40;

    /** How long a bloom of ink keeps spreading. STYLE.md 7.3's drop on wet paper. */
    public static final double BLOOM_SECONDS = 0.90;

    /**
     * How much of the beat's contact span the clash star is drawn over.
     *
     * <h2>This number is a measurement of a shortfall, and it is here so the shortfall
     * cannot be forgotten</h2>
     *
     * <p>It used to be {@code BLOOM_SECONDS * 0.7} = <b>0.63 s</b>, against a GUARD
     * beat whose whole contact span is <b>0.168 s</b>. The star therefore asserted
     * "these two blades are meeting" for <b>3.8x as long as the schedule itself says
     * they are together</b>, and measured headless against the rig, for <b>9.5x as
     * long as they actually were</b>. That is the pass-1 review's "a light that
     * asserts an event the picture does not contain", spread over half a second
     * instead of over two frames, and it survived two passes because the test that
     * was supposed to catch it checked one frame — see {@code RehearsalTest}.
     *
     * <p>The right value is <b>1.0</b>: STYLE.md 7.1 is explicit that "the middle span
     * <em>is</em> the contact", so the light and the contact span should be the same
     * window. Measured headless on the parry, the two blade segments are within 2% of a
     * figure height from t=1.550 to t=1.616 — <b>0.066 s of the 0.168 s contact span,
     * 39%</b> — and the meeting instant the clash ignites at is 1.568, so 0.27 is the
     * largest multiple of the contact span that keeps every drawn frame honest.
     *
     * <p><b>Three frames at 60 Hz is too short for STYLE.md §5's "soft star bloom with
     * 4-6 long soft rays", and that is the point of writing the number here rather than
     * tuning it away.</b> The light cannot be longer than the meeting, and the meeting
     * is 39% of the span the schedule declares for it. Both are one defect — the review's
     * item 4, "give the parry a span" — and this constant is how the next pass grades
     * the work: raise it toward 1.0 and {@code RehearsalTest} prints exactly how far
     * short the bind falls, on which frame, in figure heights.
     *
     * <p>A lever that lengthens the bind was found and rejected on the pixels; see
     * {@code Director.FIST_DROP}.
     */
    public static final double CLASH_SPAN = 0.27;

    // -- state -----------------------------------------------------------------

    private final Stage stage;
    private final Standing standing;
    private final List<ScheduledBeat> beats = new ArrayList<>();
    private final List<Directive> out = new ArrayList<>();

    private ScheduledBeat previous;
    private int openBeat = -1;
    private double contactNow;
    /** Bodies whose trunk is currently driving. A {@link TreeSet}, so release order is stable. */
    private final TreeSet<Integer> carriers = new TreeSet<>();
    private double lastRamp = Double.NEGATIVE_INFINITY;

    private Framing framing;
    private double framingAt;
    private int driftPhase;
    private double floor;
    private boolean wantWide;

    public Scheduler(Stage stage, Standing opening) {
        this.stage = stage;
        this.standing = opening.copy();
        this.framing = stage.planning();
        this.framingAt = 0.0;
    }

    /** The board as the scheduler currently understands it. Read-only in practice. */
    public Standing standing() {
        return standing;
    }

    /**
     * Inserts {@code seconds} of planning time before the next beat.
     *
     * <p>The turn loop is not continuous: between one command and the next the
     * player is looking at the board, and STYLE.md 9's whole camera plan -- "wide to
     * plan, push in to strike" -- depends on that pause existing. A scheduler fed
     * two resolutions back to back would butt the second phrase's push-in against
     * the first's return and compress both. Moving beats later can never break the
     * ordering theorem, only widen it, which is why this is a floor rather than a
     * general setter.
     */
    public Scheduler pause(double seconds) {
        floor = Math.max(floor, cursor() + Math.max(0.0, seconds));
        return this;
    }

    public Scheduler accept(Resolution resolution) {
        return accept(resolution.events());
    }

    public Scheduler accept(List<CombatEvent> events) {
        for (CombatEvent e : events) {
            handle(e);
        }
        return this;
    }

    public Schedule schedule() {
        if (wantWide) {
            wantWide = false;
            returnWide();
        }
        List<Directive> sorted = new ArrayList<>(out);
        // Stable by time, so the emission order breaks ties. Both halves are
        // deterministic, which is what makes two runs of one stream byte-identical.
        sorted.sort((a, b) -> Double.compare(a.at(), b.at()));
        return new Schedule(beats, sorted, stage);
    }

    // -- dispatch --------------------------------------------------------------

    private void handle(CombatEvent e) {
        if (e instanceof CombatEvent.EncounterBegan b) {
            began(b);
        } else if (e instanceof CombatEvent.BeatBegan b) {
            open(b.index(), ScheduledBeat.Bracket.STANZA, b.actor(), b.phases(), b.overlap(), b.focus());
        } else if (e instanceof CombatEvent.EnemyBeatBegan b) {
            open(b.index(), ScheduledBeat.Bracket.ENEMY, b.actor(), b.phases(), b.overlap(), b.focus());
        } else if (e instanceof CombatEvent.Swung s) {
            swung(s);
        } else if (e instanceof CombatEvent.Hit h) {
            hit(h);
        } else if (e instanceof CombatEvent.BladeMet m) {
            bladeMet(m.attacker(), m.defender(), m.meeting(), true);
        } else if (e instanceof CombatEvent.GuardHeld g) {
            bladeMet(g.attacker(), g.defender(), g.meeting(), false);
        } else if (e instanceof CombatEvent.Shoved s) {
            shoved(s);
        } else if (e instanceof CombatEvent.Swapped s) {
            swapped(s);
        } else if (e instanceof CombatEvent.Drawn d) {
            drawn(d);
        } else if (e instanceof CombatEvent.Collided c) {
            collided(c);
        } else if (e instanceof CombatEvent.VerbRefused v) {
            refused(v);
        } else if (e instanceof CombatEvent.Moved m) {
            moved(m);
        } else if (e instanceof CombatEvent.Turned t) {
            turned(t);
        } else if (e instanceof CombatEvent.Died d) {
            died(d);
        } else if (e instanceof CombatEvent.Bloomed b) {
            bloomed(b);
        } else if (e instanceof CombatEvent.Immobilised i) {
            immobilised(i);
        } else if (e instanceof CombatEvent.IntentDeclared i) {
            declared(i);
        } else if (e instanceof CombatEvent.StatusApplied s) {
            statusApplied(s);
        } else if (e instanceof CombatEvent.StatusConsumed s) {
            statusConsumed(s);
        } else if (e instanceof CombatEvent.TurnEnded || e instanceof CombatEvent.EncounterEnded) {
            releaseCarrier();
            wantWide = true;
        }
        standing.apply(e);
    }

    private void began(CombatEvent.EncounterBegan e) {
        if (e.laneLength() != stage.laneLength()) {
            throw new IllegalStateException("this stage is cut for a lane of " + stage.laneLength()
                    + " and the encounter opened on " + e.laneLength());
        }
        for (Standing.Body b : standing.bodies()) {
            // STYLE.md 4b.6's gaze channel, and system2-debt E1's loaded rest. A
            // figure that opens in a symmetric neutral has already lost the pass.
            emit(new Directive.PoseChange(b.id(), Stance.READY,
                    stage.gaze(b.id(), b.tile() + b.facing().step()), 0.0, 0.6, Timing.SETTLE_ROOT));
            plantFeet(b.id(), 0.0, 0.6);
        }
    }

    // -- beats -----------------------------------------------------------------

    private void open(int index, ScheduledBeat.Bracket bracket, int actor, Phases phases,
                      Overlap overlap, Focus focus) {
        ScheduledBeat beat = ScheduledBeat.after(previous, index, bracket, actor, phases, overlap, focus);
        if (beat.start() < floor) {
            beat = beat.shifted(floor - beat.start());
        }
        beats.add(beat);
        previous = beat;
        openBeat = beats.size() - 1;
        contactNow = beat.contactStart();
        stageBeat(beat);
    }

    /**
     * The body of the layer: one beat's worth of directives, in the order the
     * spiral of STYLE.md 7.0.1 runs.
     *
     * <p>The pelvis first, then the clavicle, then the hand -- each offset by a
     * share of {@link Timing#hipLead}. That the offsets are <em>in the schedule</em>
     * and not only in the solver's filters matters: {@code RigIk} records that "lag
     * in the filter and lead in the script are two different halves of the same
     * spiral and this rig uses both", and the script half is this one. A filter can
     * only make the hand late; only a script can make the hip early.
     */
    private void stageBeat(ScheduledBeat beat) {
        int actor = beat.actor();
        double lead = Timing.hipLead(beat.windUpSpan());
        double far = farTile(beat);

        raiseCarrier(actor, beat, far);

        emit(new Directive.PoseChange(actor, Stance.GATHERING, stage.gaze(actor, far),
                beat.start(), beat.windUpSpan(), Timing.SETTLE_ROOT));

        // The clavicle: attitude, between the trunk and the arm in time as it is in
        // space.
        emit(new Directive.IkTarget(actor, Chain.CLAVICLE, stage.guard(actor, standing),
                CLAVICLE_WEIGHT, CLAVICLE_WEIGHT,
                beat.start() + lead * Timing.CLAVICLE_LEAD_FRACTION,
                Math.max(0.05, beat.windUpSpan() - lead * Timing.CLAVICLE_LEAD_FRACTION),
                Ease.SLOW_IN_SLOWER_OUT, Chain.CLAVICLE.settle()));

        // The hand, last to leave. Gather over the wind-up...
        emit(new Directive.IkTarget(actor, Chain.SWORD_ARM, stage.gather(actor, standing),
                ARM_HOLD, ARM_PEAK, beat.start() + lead,
                Math.max(0.05, beat.windUpSpan() - lead), Ease.SLOW_IN_SLOWER_OUT,
                Chain.SWORD_ARM.settle()));
        // ...release across the contact span, which is where the gesture arrives...
        emit(new Directive.IkTarget(actor, Chain.SWORD_ARM,
                stage.reach(actor, far, heightFor(beat), standing),
                ARM_PEAK, ARM_PEAK, beat.contactStart(), beat.contactSpan(), Ease.EASE_OUT,
                Chain.SWORD_ARM.settle()));
        // ...and settle through the recovery, never to zero inside a bracket.
        emit(new Directive.IkTarget(actor, Chain.SWORD_ARM, stage.rest(actor, standing),
                ARM_PEAK, ARM_HOLD, beat.recoveryStart(), beat.recoverySpan(),
                Ease.SLOW_IN_SLOWER_OUT, Chain.SWORD_ARM.settle()));

        plantFeet(actor, beat.start(), beat.windUpSpan());
        track(beat);
    }

    /**
     * Brings the trunk up and leaves it up.
     *
     * <p>This is where "reads as one phrase rather than five events" is decided.
     * The first beat of a bracket ramps the carrier from its floor; every beat after
     * it re-aims the same chain at a held weight. So the trunk's weight is a
     * continuous positive function across the whole bracket and the five clauses
     * share one gesture, instead of each raising and dropping its own.
     */
    private void raiseCarrier(int actor, ScheduledBeat beat, double far) {
        boolean fresh = !carriers.contains(actor);
        double from = fresh ? CARRIER_HOLD : CARRIER_WEIGHT;
        emit(new Directive.IkTarget(actor, Chain.SPINE,
                stage.hipLeaning(actor, standing, far, HIP_LEAN),
                from, CARRIER_WEIGHT, beat.start(),
                fresh ? beat.windUpSpan() : Math.max(0.05, beat.contactEnd() - beat.start()),
                Ease.SLOW_IN_SLOWER_OUT, Chain.SPINE.settle()));
        carriers.add(actor);
    }

    /**
     * Lets every raised trunk down again, over the slowest thing in the chain.
     *
     * <p>Every trunk, not only the last one: a turn contains the hero's stanza and
     * then the enemy phase, so two or three bodies are carrying at once by the time
     * the turn closes and leaving any of them driving would strand a lean.
     */
    private void releaseCarrier() {
        if (previous == null) {
            return;
        }
        for (int actor : carriers) {
            emit(new Directive.IkTarget(actor, Chain.SPINE, stage.hip(actor, standing),
                    CARRIER_WEIGHT, CARRIER_HOLD, previous.end(), Timing.SETTLE_TIP,
                    Ease.SLOW_IN_SLOWER_OUT, Chain.SPINE.settle()));
            emit(new Directive.PoseChange(actor, Stance.READY,
                    stage.gaze(actor, standing.tile(actor) + standing.facing(actor).step()),
                    previous.end(), Timing.SETTLE_TIP, Timing.SETTLE_ROOT));
        }
        carriers.clear();
    }

    private void plantFeet(int body, double at, double duration) {
        // Staggered, because STYLE.md 7.0.3 applies to a pair of feet as much as to
        // a pair of joints. The near foot commits, the far one follows.
        emit(new Directive.IkTarget(body, Chain.LEG_LEAD, stage.footLead(body, standing),
                LEG_WEIGHT, LEG_WEIGHT, at, Math.max(0.05, duration), Ease.EASE_IN_OUT,
                Chain.LEG_LEAD.settle()));
        emit(new Directive.IkTarget(body, Chain.LEG_TRAIL, stage.footTrail(body, standing),
                LEG_WEIGHT, LEG_WEIGHT, at + duration * 0.12, Math.max(0.05, duration * 0.88),
                Ease.EASE_IN_OUT, Chain.LEG_TRAIL.settle()));
    }

    // -- blades ----------------------------------------------------------------

    private void swung(CombatEvent.Swung e) {
        ScheduledBeat beat = beat();
        if (beat == null) {
            return;
        }
        double t = beat.instant(e.at());
        contact(t);
        int far = e.tiles().isEmpty() ? standing.tile(e.attacker())
                : e.tiles().get(e.tiles().size() - 1);
        ContactPoint.Height h = heightForReach(Math.abs(far - standing.tile(e.attacker())));
        // A stroke that lands somewhere other than the beat's nominal contact -- a
        // Double Strike -- gets its own release, because STYLE.md 10 fails a pass on
        // sight of two things peaking together and the engine has already staggered
        // the two instants for exactly that reason.
        if (Math.abs(t - beat.contactStart()) > 1e-6) {
            emit(new Directive.IkTarget(e.attacker(), Chain.SWORD_ARM,
                    stage.reach(e.attacker(), far, h, standing), ARM_PEAK, ARM_PEAK,
                    t, beat.contactSpan(), Ease.EASE_OUT, Chain.SWORD_ARM.settle()));
        }
        double dir = Stage.alongLane(standing.tile(e.attacker()), far);
        // STYLE.md 5: the trail must exist and must curve; a straight one "reads as a
        // generic slash VFX and fails". Its origin is the guard and its direction the
        // swept path, which is what a ribbon needs to be drawn as an arc.
        emit(new Directive.Ink(e.attacker(), Directive.InkKind.TRAIL,
                stage.guard(e.attacker(), standing), dir, -0.25, 0.8,
                t - beat.contactSpan() * 0.4, TRAIL_SECONDS));
        emit(sleeve(e.attacker(), dir, Force.DRIVE, t, beat.contactSpan()));
    }

    private void hit(CombatEvent.Hit e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : contactNow;
        double span = beat == null ? Timing.BASE_BEAT : beat.recoverySpan();
        Anchor origin = e.landed() != null
                ? stage.contact(e.landed(), standing)
                : stage.hip(e.target(), standing);
        double dir = Stage.alongLane(standing.tile(e.attacker()), standing.tile(e.target()));
        if (dir == 0.0) {
            dir = -standing.facing(e.target()).step();
        }
        double force = Math.min(1.0, 0.30 + 0.18 * e.amount());

        // STYLE.md 7.3's four reactions, all four of them, and none of them a freeze.
        emit(new Directive.Ink(e.target(), Directive.InkKind.BLOOM, origin, dir, 0.0, force,
                t, BLOOM_SECONDS));
        emit(new Directive.Ink(e.target(), Directive.InkKind.FLECKS, origin, dir, 0.15, force * 0.8,
                t + 0.05, BLOOM_SECONDS * 0.7));
        heldBreath(t);
        emit(new Directive.PoseChange(e.target(), Stance.YIELDING,
                stage.gaze(e.target(), standing.tile(e.attacker())),
                t, span * Timing.YIELD_FRACTION, Timing.SETTLE_TIP));
        emit(hem(e.target(), dir, Force.DRIFT, t, span * Timing.YIELD_FRACTION));
        emit(hair(e.target(), dir, Force.DRIFT, t, span * Timing.YIELD_FRACTION));
    }

    /**
     * A parry or a plain guard: two skeletons agreeing on one point.
     *
     * <p>Both arms are aimed at their <em>own</em> named {@link ContactPoint}, which
     * is why {@link Meeting} names the crossing twice. The defender then gives ground
     * along a second target, because STYLE.md 7.2 wants "a deflection curve, not a
     * collision" and a single held point is a collision however softly it is eased
     * into.
     */
    private void bladeMet(int attacker, int defender, Meeting meeting, boolean counter) {
        ScheduledBeat beat = beat();
        if (beat == null) {
            return;
        }
        double t = beat.instant(meeting.at());
        contact(t);
        double dir = Stage.alongLane(standing.tile(attacker), standing.tile(defender));

        // One point, named twice by the engine and reconciled by Stage. Both
        // blades are aimed at it, which is the difference between "two skeletons
        // agreeing on one point in space" and two skeletons each holding its own
        // opinion 0.08 units from the other's.
        Anchor met = stage.crossing(meeting, standing);

        // <b>Both arms are led into the crossing, not released at it.</b> STYLE.md
        // 7.1's correction is explicit that the blades meet at the *start* of the
        // middle span and slide through it -- "reading the middle span as
        // travel-toward-a-hit puts the meeting at 55 and collapses the deflection
        // to a point, which is the collision this document exists to forbid". A
        // chain whose own settle is 0.48 s cannot arrive at an instant it is first
        // told about at that instant, so pass 1's attacker was still 0.8 units
        // away when its beat's contact span was over. It now leaves a full contact
        // span early, exactly as the defender already did.
        supersede(attacker, Chain.SWORD_ARM, beat.contactStart());
        double lead = Math.max(beat.contactSpan(), beat.windUpSpan() * 0.55);
        emit(new Directive.IkTarget(attacker, Chain.SWORD_ARM, met,
                ARM_PEAK, ARM_PEAK, t - lead, lead + beat.contactSpan(),
                Ease.EASE_OUT, STRIKE));
        emit(new Directive.IkTarget(defender, Chain.SWORD_ARM, met,
                ARM_HOLD, ARM_PEAK, t - lead, lead + beat.contactSpan(),
                Ease.SLOW_IN_SLOWER_OUT, STRIKE));
        // The deflection: the defender's blade slides along the lane and drops,
        // and it starts while the two are still touching, which is what makes it a
        // curve rather than a release.
        emit(new Directive.IkTarget(defender, Chain.SWORD_ARM,
                new Anchor(met.body(), met.site(), met.x() + dir * GIVE_GROUND, met.y() - 0.05),
                ARM_PEAK, ARM_HOLD, t + beat.contactSpan(),
                beat.recoverySpan() * 0.5,
                Ease.SLOW_IN_SLOWER_OUT, Chain.SWORD_ARM.settle()));
        // The defender's own source of motion. Without this the guard is an arm
        // moving on a frozen torso, which STYLE.md 7.0.1 fails before any analysis.
        emit(new Directive.IkTarget(defender, Chain.SPINE,
                stage.hipLeaning(defender, standing, standing.tile(attacker), HIP_LEAN * 0.7),
                CARRIER_HOLD, CARRIER_WEIGHT * 0.7, t - beat.contactSpan(),
                beat.contactSpan() + beat.recoverySpan() * 0.5, Ease.SLOW_IN_SLOWER_OUT,
                Chain.SPINE.settle()));
        emit(new Directive.PoseChange(defender, Stance.GUARD_RAISED,
                stage.gaze(defender, standing.tile(attacker)),
                t - beat.contactSpan(), beat.contactSpan() * 2, Timing.SETTLE_ROOT + 0.06));
        emit(new Directive.Ink(defender, Directive.InkKind.CLASH, met, dir, 0.1,
                counter ? 0.9 : 0.6, t, beat.contactSpan() * CLASH_SPAN));
        emit(sleeve(attacker, dir, Force.DRIVE, t, beat.contactSpan()));
        emit(sleeve(defender, -dir, Force.DRIFT, t, beat.contactSpan()));
    }

    // -- the two verbs ---------------------------------------------------------

    private void shoved(CombatEvent.Shoved e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : beat.instant(e.meeting().at());
        double span = beat == null ? Timing.BASE_BEAT * 0.3 : beat.contactSpan();
        if (beat != null) {
            contact(t);
        }
        double dir = Stage.alongLane(e.fromTile(), e.toTile());
        if (dir == 0.0) {
            dir = standing.facing(e.pusher()).step();
        }
        Anchor onPusher = stage.contact(e.meeting().onActor(), standing);
        // combat-design.md 2.1: "a shoulder-and-hilt check". Which of the two it is
        // decides which chain drives, and the Meeting is the only thing that knows.
        Chain driver = e.meeting().onActor().part() == ContactPoint.Part.SHOULDER
                ? Chain.SPINE : Chain.SWORD_ARM;
        emit(new Directive.IkTarget(e.pusher(), driver, onPusher,
                driver == Chain.SPINE ? CARRIER_WEIGHT : ARM_HOLD,
                driver == Chain.SPINE ? CARRIER_WEIGHT : ARM_PEAK,
                t - span, span * 2, Ease.SLOW_IN_SLOWER_OUT, driver.settle()));
        emit(new Directive.PoseChange(e.pusher(), Stance.BRACED,
                stage.gaze(e.pusher(), standing.tile(e.shoved())), t, span * 2, Timing.SETTLE_ROOT));
        emit(new Directive.PoseChange(e.shoved(), e.gaveGround() ? Stance.CARRIED : Stance.BRACED,
                stage.gaze(e.shoved(), standing.tile(e.pusher())), t, span * 2, Timing.SETTLE_TIP));
        emit(hem(e.shoved(), dir, e.gaveGround() ? Force.DRIFT : Force.DRIVE, t, span * 2));
        emit(hair(e.shoved(), dir, e.gaveGround() ? Force.DRIFT : Force.DRIVE, t, span * 2));
    }

    private void swapped(CombatEvent.Swapped e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : beat.instant(e.meeting().at());
        double span = beat == null ? Timing.BASE_BEAT * 0.3 : beat.contactSpan();
        if (beat != null) {
            contact(t);
        }
        double dir = Stage.alongLane(e.aFrom(), e.bFrom());
        // combat-design.md 2.1: "sleeves and hair crossing, each trailing into the
        // space the other has left." Force.NONE, because this is the one contact in
        // the game with nothing behind it.
        emit(new Directive.PoseChange(e.a(), Stance.PASSING, stage.gaze(e.a(), e.bFrom()),
                t - span, span * 3, Timing.SETTLE_ROOT + 0.08));
        emit(new Directive.PoseChange(e.b(), Stance.PASSING, stage.gaze(e.b(), e.aFrom()),
                t - span * 0.8, span * 3, Timing.SETTLE_ROOT + 0.12));
        emit(sleeve(e.a(), dir, Force.NONE, t, span * 2));
        emit(sleeve(e.b(), -dir, Force.NONE, t, span * 2));
        emit(hair(e.a(), dir, Force.NONE, t, span * 2));
        emit(hair(e.b(), -dir, Force.NONE, t, span * 2));
    }

    private void drawn(CombatEvent.Drawn e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : beat.instant(e.meeting().at());
        double span = beat == null ? Timing.BASE_BEAT * 0.25 : beat.contactSpan();
        if (beat != null) {
            contact(t);
        }
        // combat-design.md 2.2: "contact at distance -- a line of force between two
        // figures", and Phases.REACH gives it the widest contact span in the game
        // because the hook is held while the body comes in rather than released.
        emit(new Directive.IkTarget(e.puller(), Chain.SWORD_ARM,
                stage.contact(e.meeting().onTarget(), standing), ARM_PEAK, ARM_PEAK, t, span,
                Ease.EASE_OUT, Chain.SWORD_ARM.settle()));
        emit(new Directive.PoseChange(e.pulled(), Stance.CARRIED,
                stage.gaze(e.pulled(), standing.tile(e.puller())), t, span * 2, Timing.SETTLE_TIP));
    }

    private void collided(CombatEvent.Collided e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : contactNow;
        double span = beat == null ? Timing.BASE_BEAT * 0.3 : beat.contactSpan();
        for (int body : List.of(e.a(), e.b())) {
            emit(new Directive.PoseChange(body, Stance.BRACED,
                    stage.gaze(body, e.atTile()), t, span * 2, Timing.SETTLE_ROOT + 0.1));
        }
        emit(new Directive.Ink(e.a(), Directive.InkKind.BLOOM,
                stage.onTile(e.atTile(), ContactPoint.Height.MIDDLE), 0.0, 0.2,
                Math.min(1.0, 0.4 + 0.2 * e.damage()), t, BLOOM_SECONDS));
        heldBreath(t);
    }

    private void refused(CombatEvent.VerbRefused e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : contactNow;
        double span = beat == null ? Timing.BASE_BEAT * 0.3 : beat.contactSpan();
        double dir = Stage.alongLane(standing.tile(e.target()), standing.tile(e.actor()));
        // "A body thrown against something rooted." The recoil is on the actor, and
        // the Bulwark does not move at all -- which is the whole point of it.
        emit(new Directive.PoseChange(e.actor(), Stance.BRACED,
                stage.gaze(e.actor(), standing.tile(e.target())), t, span * 2, Timing.SETTLE_ROOT));
        emit(hem(e.actor(), dir, Force.DRIFT, t, span * 2));
        emit(hair(e.actor(), dir, Force.DRIFT, t, span * 2));
    }

    // -- travel ----------------------------------------------------------------

    /**
     * A body crossing tiles.
     *
     * <p>The carry runs from the beat's contact to the beat's end, which for a
     * one-tile {@link Phases#TRAVEL} beat is 0.784 s -- STYLE.md 7.2's "arriving
     * over ~0.8 s", reached by construction rather than by tuning. See
     * {@link Timing}.
     *
     * <p>The cloth and hair impulses point <em>along</em> the travel, not against
     * it. STYLE.md 7.2's correction is explicit that "streaming ahead" is a
     * displacement and not an arrival time: "measure the growing tip-to-root offset
     * during the strike, not who gets there first."
     */
    private void moved(CombatEvent.Moved e) {
        ScheduledBeat beat = beat();
        double at = beat == null ? cursor() : beat.contactStart();
        double span = beat == null ? Timing.KNOCKBACK_ARRIVAL
                : beat.contactSpan() + beat.recoverySpan();
        double dir = Stage.alongLane(e.fromTile(), e.toTile());

        emit(hem(e.entity(), dir, e.force(), at, span));
        emit(hair(e.entity(), dir, e.force(), at, span));
        if (e.force() == Force.DRIFT) {
            emit(new Directive.PoseChange(e.entity(), Stance.CARRIED,
                    stage.gaze(e.entity(), e.fromTile()), at, span, Timing.SETTLE_TIP));
        }

        standing.place(e.entity(), e.toTile());
        emit(new Directive.IkTarget(e.entity(), Chain.SPINE, stage.hip(e.entity(), standing),
                CARRIER_WEIGHT, CARRIER_WEIGHT, at, span, Ease.SLOW_IN_SLOWER_OUT,
                Chain.SPINE.settle()));
        // The two feet do not land together, and the near one commits first.
        emit(new Directive.IkTarget(e.entity(), Chain.LEG_LEAD, stage.footLead(e.entity(), standing),
                LEG_WEIGHT, LEG_WEIGHT, at, span * 0.82, Ease.SLOW_IN_SLOWER_OUT,
                Chain.LEG_LEAD.settle()));
        emit(new Directive.IkTarget(e.entity(), Chain.LEG_TRAIL, stage.footTrail(e.entity(), standing),
                LEG_WEIGHT, LEG_WEIGHT, at + span * 0.18, span * 0.82, Ease.SLOW_IN_SLOWER_OUT,
                Chain.LEG_TRAIL.settle()));
    }

    private void turned(CombatEvent.Turned e) {
        ScheduledBeat beat = beat();
        double at = beat == null ? cursor() : beat.contactStart();
        // combat-design.md 2.2: "the whole body winding around; cloth and hair last
        // to arrive", which Phases.WIND_AROUND expresses as 65% recovery. The facing
        // itself is done long before the garment is.
        double span = beat == null ? Timing.BASE_BEAT * 0.5
                : beat.contactSpan() + beat.recoverySpan() * 0.65;
        emit(new Directive.FacingChange(e.entity(), e.from(), e.to(), at, span,
                Ease.SLOW_IN_SLOWER_OUT));
        double dir = e.to().step();
        emit(hem(e.entity(), dir, Force.DRIVE, at, span));
        emit(hair(e.entity(), dir, Force.DRIVE, at, span * 1.2));
        standing.turn(e.entity(), e.to());
        emit(new Directive.IkTarget(e.entity(), Chain.CLAVICLE, stage.guard(e.entity(), standing),
                CLAVICLE_WEIGHT, CLAVICLE_WEIGHT, at + span * 0.25, span * 0.75,
                Ease.SLOW_IN_SLOWER_OUT, Chain.CLAVICLE.settle()));
    }

    // -- leaving ---------------------------------------------------------------

    private void died(CombatEvent.Died e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : contactNow;
        Dissolve d = e.dissolve();
        // Dissolve.spans is counted in beats, deliberately -- "not seconds; the
        // moment the engine learns about time the whole replay property becomes a
        // rendering concern". This is where a beat becomes a second.
        double beatSeconds = beat == null ? Timing.BASE_BEAT : beat.duration();
        double span = d.spans() * beatSeconds;
        double dir = d.along().step();
        double force = switch (d.force()) {
            case NONE -> 0.15;
            case DRIFT -> 0.45;
            case DRIVE -> 0.7;
            case HEADLONG -> 1.0;
        };
        emit(new Directive.Ink(e.entity(), Directive.InkKind.DISSOLVE,
                stage.hip(e.entity(), standing), dir, 0.08, force, t, span));
        emit(new Directive.PoseChange(e.entity(), Stance.SLACK,
                stage.gaze(e.entity(), e.atTile()), t, span * 0.5, Timing.SETTLE_TIP));
        emit(hem(e.entity(), dir, d.force(), t, span * 0.7));
        emit(hair(e.entity(), dir, d.force(), t, span * 0.9));
        // The body gives before the ink has finished. Both chains fade rather than
        // stopping, which is STYLE.md 7.1's "nothing arrives at rest abruptly"
        // applied to a whole figure.
        emit(new Directive.IkTarget(e.entity(), Chain.SPINE, stage.hip(e.entity(), standing),
                CARRIER_WEIGHT, 0.0, t, span * 0.6, Ease.SLOW_IN_SLOWER_OUT, Chain.SPINE.settle()));
        emit(new Directive.IkTarget(e.entity(), Chain.SWORD_ARM, stage.rest(e.entity(), standing),
                ARM_HOLD, 0.0, t, span * 0.45, Ease.SLOW_IN_SLOWER_OUT, Chain.SWORD_ARM.settle()));
        // A body that has gone is not carrying anything, and must not be handed a
        // release at the end of the turn that would put its trunk back up.
        carriers.remove(e.entity());
    }

    private void bloomed(CombatEvent.Bloomed e) {
        ScheduledBeat beat = beat();
        double t = beat == null ? cursor() : contactNow;
        double span = beat == null ? Timing.BASE_BEAT : beat.duration();
        for (int tile : e.tiles()) {
            double dir = Stage.alongLane(e.atTile(), tile);
            emit(new Directive.Ink(e.entity(), Directive.InkKind.BLOOM,
                    stage.onTile(tile, ContactPoint.Height.LOW), dir, 0.3, 1.0,
                    t + Math.abs(dir) * 0.06, span * 0.8));
        }
    }

    private void immobilised(CombatEvent.Immobilised e) {
        ScheduledBeat beat = beat();
        double at = beat == null ? cursor() : beat.start();
        double span = beat == null ? Timing.BASE_BEAT : beat.duration();
        // combat-design.md 1.4: "the figure's pigment dries; motion damping raised
        // hard." The longest settle in the band, which is what damping raised hard
        // looks like from this side of the interface.
        emit(new Directive.PoseChange(e.entity(), Stance.DRIED,
                stage.gaze(e.entity(), standing.tile(e.entity()) + standing.facing(e.entity()).step()),
                at, span, Timing.SETTLE_TIP));
    }

    // -- intent and status -----------------------------------------------------

    private void declared(CombatEvent.IntentDeclared e) {
        List<Integer> tiles = e.intent().threatened();
        int look = tiles.isEmpty() ? e.intent().destination() : tiles.get(tiles.size() - 1);
        // STYLE.md 4b.6's gaze channel carries "intent -- who this character is about
        // to act on", and a Strikethrough is precisely that, a turn early.
        emit(new Directive.PoseChange(e.entity(), Stance.READY, stage.gaze(e.entity(), look),
                cursor(), 0.45, Timing.SETTLE_ROOT + 0.05));
    }

    private void statusApplied(CombatEvent.StatusApplied e) {
        if (e.status() == Status.MARKED) {
            // combat-design.md 1.4: "a vermillion seal blooms on the body." STYLE.md
            // 2.2 keeps vermillion on a budget, so this is one small mark.
            emit(new Directive.Ink(e.entity(), Directive.InkKind.SEAL,
                    new Anchor(e.entity(), Anchor.Site.CONTACT,
                            stage.tileX(standing.tile(e.entity())), Stage.Y_MIDDLE),
                    0.0, 0.0, 0.5, cursor(), 0.7));
        } else if (e.status() == Status.GUARD) {
            emit(new Directive.PoseChange(e.entity(), Stance.GUARD_RAISED,
                    stage.gaze(e.entity(), standing.tile(e.entity()) + standing.facing(e.entity()).step()),
                    cursor(), 0.5, Timing.SETTLE_ROOT + 0.06));
        }
    }

    private void statusConsumed(CombatEvent.StatusConsumed e) {
        if (e.status() == Status.GUARD) {
            emit(new Directive.PoseChange(e.entity(), Stance.READY,
                    stage.gaze(e.entity(), standing.tile(e.entity()) + standing.facing(e.entity()).step()),
                    contactNow, 0.5, Timing.SETTLE_ROOT + 0.06));
        }
    }

    // -- camera ----------------------------------------------------------------

    /**
     * Frames one beat, going wide first if there was time to.
     *
     * <p>The return is deliberately not emitted the moment a turn ends. STYLE.md 9
     * asks for "wide to plan, push in to strike", and whether there is a plan to go
     * wide for depends on something the event stream cannot say: whether the player
     * stopped to think. So the intent is recorded when the turn closes and honoured
     * only if the next beat leaves room for a full return <em>and</em> a full
     * push-in. Otherwise the camera stays close through the exchange, which is the
     * right shot for two commands played back to back and is also the only way to
     * avoid a compressed glide -- a half-length push-in is the beginning of a cut.
     */
    private void track(ScheduledBeat beat) {
        if (beat == null) {
            return;
        }
        if (wantWide) {
            wantWide = false;
            if (beat.start() - framingAt >= stage.returnSeconds() + stage.pushInSeconds()) {
                returnWide();
            }
        }
        Framing to = stage.execution(beat.focus());
        boolean wide = framing.widthTiles() > Stage.INTIMACY_TILES * 1.4;
        glide(to, beat.start(),
                wide ? stage.pushInSeconds() : Math.min(Timing.PUSH_IN, beat.duration() * 0.6),
                wide ? Directive.CameraReason.PUSH_IN : Directive.CameraReason.TRACK);
    }

    private void returnWide() {
        Framing plan = stage.planning();
        if (same(plan, framing)) {
            return;
        }
        glide(plan, cursor(), stage.returnSeconds(), Directive.CameraReason.RETURN);
    }

    /**
     * One eased move, and the drift that fills whatever gap precedes it.
     *
     * <p>STYLE.md 9 asks for two things that are the same rule seen twice: "never
     * cut, never shake -- all camera movement is eased and continuous", and
     * "slight slow drift -- the camera is never perfectly still". So a key can only
     * start where the last one finished, and any idle stretch longer than
     * {@link Timing#CAMERA_MIN_DRIFT} is filled with a drift rather than left as a
     * hold. A gap shorter than that is absorbed into this move instead, because a
     * quarter-second nudge between two glides is shake by another name.
     */
    private void glide(Framing target, double at, double duration, Directive.CameraReason reason) {
        // A move to where the camera already is would be a hold, and STYLE.md 9 says
        // the camera is never perfectly still. Two beats over the same tiles are the
        // ordinary way that happens -- a stanza of Cuts from one place -- so the shot
        // breathes instead of freezing.
        Framing to = same(framing, target) ? nudge(target) : target;
        double start;
        if (at > framingAt + Timing.CAMERA_MIN_DRIFT) {
            drift(framingAt, at - framingAt);
            start = at;
        } else {
            start = framingAt;
        }
        // A move is delayed, never shortened. Compressing a glide because the camera
        // was still busy is how a push-in turns into a cut, and STYLE.md 9 bans the
        // cut rather than the lateness.
        double dur = Math.max(Timing.CAMERA_MIN_MOVE, Math.max(duration, at + duration - start));
        emit(new Directive.CameraKey(framing, to, start, dur, Ease.SLOW_IN_SLOWER_OUT, reason));
        framing = to;
        framingAt = start + dur;
    }

    private void drift(double at, double duration) {
        Framing to = nudge(framing);
        emit(new Directive.CameraKey(framing, to, at, Math.max(Timing.CAMERA_MIN_MOVE, duration),
                Ease.EASE_IN_OUT, Directive.CameraReason.DRIFT));
        framing = to;
        framingAt = at + Math.max(Timing.CAMERA_MIN_MOVE, duration);
    }

    /** The same shot, moved just enough that it is not the same shot. Alternates sign. */
    private Framing nudge(Framing f) {
        double sign = (driftPhase++ % 2 == 0) ? 1.0 : -1.0;
        return stage.clamp(f.drifted(sign * Timing.CAMERA_DRIFT_TILES,
                sign * Timing.CAMERA_DRIFT_WIDTH));
    }

    private static boolean same(Framing a, Framing b) {
        return Math.abs(a.centreTile() - b.centreTile()) < 1e-9
                && Math.abs(a.widthTiles() - b.widthTiles()) < 1e-9;
    }

    // -- helpers ---------------------------------------------------------------

    private ScheduledBeat beat() {
        return openBeat < 0 ? null : beats.get(openBeat);
    }

    private double cursor() {
        return previous == null ? framingAt : previous.end();
    }

    /**
     * Records a contact instant on the open beat, and remembers it for the events
     * that follow.
     *
     * <p>The open beat is always the last one placed, so {@code previous} is
     * refreshed with it -- a beat is immutable and folding in a contact makes a new
     * one, which the placement of the <em>next</em> beat must be measured against.
     */
    private void contact(double instant) {
        contactNow = instant;
        if (openBeat >= 0) {
            ScheduledBeat updated = beats.get(openBeat).withContact(instant);
            beats.set(openBeat, updated);
            previous = updated;
        }
    }

    /** STYLE.md 7.3's held breath, at most one per quarter second so it never stacks into a freeze. */
    private void heldBreath(double t) {
        if (t < lastRamp + Timing.HELD_BREATH_SECONDS) {
            return;
        }
        lastRamp = t;
        emit(new Directive.TimeRamp(Timing.HELD_BREATH_SCALE, t, Timing.HELD_BREATH_SECONDS));
    }

    private Directive.Impulse hem(int body, double dir, Force force, double at, double duration) {
        return new Directive.Impulse(body, Region.CLOTH_HEM, stage.hip(body, standing), dir, 0.0,
                Region.CLOTH_HEM.magnitude(force), at, Math.max(0.05, duration));
    }

    private Directive.Impulse sleeve(int body, double dir, Force force, double at, double duration) {
        return new Directive.Impulse(body, Region.CLOTH_SLEEVE, stage.guard(body, standing), dir, 0.0,
                Region.CLOTH_SLEEVE.magnitude(force), at, Math.max(0.05, duration));
    }

    private Directive.Impulse hair(int body, double dir, Force force, double at, double duration) {
        return new Directive.Impulse(body, Region.HAIR, stage.head(body, standing), dir, 0.06,
                Region.HAIR.magnitude(force), at, Math.max(0.05, duration));
    }

    /** The far end of a beat's focus from where its actor is standing. */
    private double farTile(ScheduledBeat beat) {
        double here = standing.tile(beat.actor());
        double from = beat.focus().fromTile();
        double to = beat.focus().toTile();
        return Math.abs(to - here) >= Math.abs(from - here) ? to : from;
    }

    private ContactPoint.Height heightFor(ScheduledBeat beat) {
        int distance = (int) Math.round(Math.abs(farTile(beat) - standing.tile(beat.actor())));
        return heightForReach(Math.max(1, distance));
    }

    /**
     * {@code ContactPoint.heightForReach}, restated because the engine's copy is
     * package-private.
     *
     * <p>Restated rather than exported: "adjacent, it comes down; from reach two it
     * comes in flat" is a rule about how a blade arrives, which is a staging
     * question, and the engine only needs it to fill in a {@link ContactPoint} of
     * its own. Two copies of a one-line rule is the smaller cost. It is pinned by a
     * test against the engine's own behaviour so the two cannot drift.
     */
    static ContactPoint.Height heightForReach(int distance) {
        return distance <= 1 ? ContactPoint.Height.HIGH : ContactPoint.Height.MIDDLE;
    }

    private void emit(Directive d) {
        out.add(d);
    }

    /**
     * Drops a chain directive a later, more specific event replaces.
     *
     * <p><b>Two directives on one chain at one instant is not a schedule, it is an
     * ambiguity</b>, and pass 1 shipped one: {@link #stageBeat} emits the actor's
     * release at {@code contactStart} from the beat's {@link Focus}, and then
     * {@link #bladeMet} emitted a second target on the same chain at the same
     * instant from the {@link Meeting}. The director resolves ties by emission
     * order, so the picture depended on which handler ran first, and the segment
     * between the two had zero duration -- so the interpolation that makes a parry
     * a curve ran from a point the hand had never been at.
     *
     * <p>The Meeting is the more specific of the two: the beat says a stroke goes
     * toward a tile, the Meeting says where it actually lands and on what. So the
     * beat's release is withdrawn rather than left to race.
     */
    private void supersede(int body, Chain chain, double at) {
        out.removeIf(d -> d instanceof Directive.IkTarget t
                && t.body() == body && t.chain() == chain && Math.abs(t.at() - at) < 1e-9);
    }
}
