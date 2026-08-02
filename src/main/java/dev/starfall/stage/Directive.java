package dev.starfall.stage;

import dev.starfall.combat.Facing;

import java.util.Locale;

/**
 * One thing the staging layer tells the renderer to do, and when.
 *
 * <p><b>This is the interface the whole layer exists to produce.</b> The combat
 * engine emits an ordinal event stream; the renderer draws pixels; between them
 * something has to say "put this chain's effector here, ramp its weight in over
 * this interval, and give the hem a shove along the lane a tenth of a second after
 * the hips arrive". Nothing did. combat-design.md 3d.5 states the gap in one line:
 * "No space and no time. Someone must map turn/tile to seconds/units."
 *
 * <h2>Why these seven and not others</h2>
 *
 * <p>Each entry answers a requirement in STYLE.md 7 or 9 that no other entry can.
 *
 * <ul>
 *   <li>{@link IkTarget} -- 7.0.1's source of motion, 7.0.2's visible effort and
 *       7.2's "the defender's arm gives ground on an IK curve rather than stopping
 *       dead". {@code IkChain}'s own documentation names the shape it wants: "a
 *       parry that fades IK in over the wind-up and out over the recovery is a
 *       weight ramp." So a target without a ramp and an interval would be
 *       unusable, and one without a {@link Settle} would resurrect 7.0.3.</li>
 *   <li>{@link Impulse} -- 7.1's overlapping action and 7.3's "a change in how
 *       cloth trails", which is one of only four things 7.3 allows an impact to be
 *       expressed as. {@code system3-debt.md} records that System 4 "must express
 *       impact through how cloth trails... and it has nothing to express it
 *       with"; this is the something.</li>
 *   <li>{@link CameraKey} -- all of 9. Framing, glide, return, and the ban on cuts
 *       and shake, which is checkable only because a key carries a duration and
 *       both endpoints.</li>
 *   <li>{@link FacingChange} -- combat-design.md 2.2's Turn, "the whole body
 *       winding around; cloth and hair last to arrive". A facing flip is the one
 *       change that would read as a snap if it were a boolean, and 7.2's first
 *       line is "no snapping".</li>
 *   <li>{@link PoseChange} -- {@code system2-debt.md} E1's finding that the poetry
 *       must be a <em>condition</em> and not an event, plus 7.3's yielding and
 *       4b.6's gaze channel.</li>
 *   <li>{@link TimeRamp} -- 7.3's held breath, "a soft time ramp, ~0.85x for
 *       ~0.25 s, never a hard freeze". It is the only global directive and it
 *       exists so the renderer never has to invent hitstop, which 7.1 and 10 both
 *       ban outright.</li>
 *   <li>{@link Ink} -- 7.3's other three reactions (bloom, flecks, yielding's
 *       visual half) and 3's dissolve, which is how a body leaves. Impact in this
 *       game "is expressed by ink", and something has to say where and how much.</li>
 * </ul>
 *
 * <p>Everything here is a record and every field is a number, an enum or an
 * {@link Anchor} that is itself a record, so two schedules built from one event
 * stream are {@code equals}. That is the determinism contract, inherited
 * deliberately from {@link dev.starfall.combat.CombatEvent}'s.
 */
public sealed interface Directive
        permits Directive.IkTarget, Directive.Impulse, Directive.CameraKey,
        Directive.FacingChange, Directive.PoseChange, Directive.TimeRamp, Directive.Ink {

    /** For directives that are about the frame rather than about a body. */
    int NO_BODY = -1;

    /** When it starts, in seconds from the start of the schedule. */
    double at();

    /** How long it takes. Never negative; zero only for an instant with no ramp. */
    double duration();

    /** Whose it is, or {@link #NO_BODY}. */
    int body();

    default double end() {
        return at() + duration();
    }

    /** A stable one-line rendering, for {@link Schedule#fingerprint()}. */
    String describe();

    // -- the seven -------------------------------------------------------------

    /**
     * Put a chain's end effector at a point, fading the chain in or out over an
     * interval, with these arrival times down its links.
     *
     * <p>Maps one-to-one onto {@code IkChain}: {@code target(x, y)},
     * {@code weight(w)} ramped across {@code [at, end]},
     * {@code settleSeconds(settle.base())} and
     * {@code boneLagSeconds(i, settle.lag(i))}.
     *
     * @param weightFrom the chain's blend weight at {@link #at()}
     * @param weightTo   its weight at {@link #end()}, held afterwards until the
     *                   next directive on the same chain
     */
    record IkTarget(int body, Chain chain, Anchor target, double weightFrom, double weightTo,
                    double at, double duration, Ease ease, Settle settle) implements Directive {

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "ik %d %s %s w%.3f->%.3f %s settle%s",
                    body, chain, target, weightFrom, weightTo, ease, settle);
        }
    }

    /**
     * Shove a simulated surface, along a direction, at a strength.
     *
     * <p>{@link #at()} is when the <em>driver</em> acts; the surface answers
     * {@code region.lag()} later, behind {@code region.anchor()}. Both halves are
     * carried because STYLE.md 7.1 refuses a lag quoted without its anchor, and
     * {@link #arrives()} prints the answer.
     */
    record Impulse(int body, Region region, Anchor origin, double dirX, double dirY,
                   double magnitude, double at, double duration) implements Directive {

        /** When the surface actually answers: the driver's instant plus the region's lag. */
        public double arrives() {
            return at + region.lag();
        }

        /** The same instant read against the pelvis instead. See {@link Region#netLagBehindHips()}. */
        public double arrivesBehindHips() {
            return at + region.netLagBehindHips();
        }

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "imp %d %s %s dir(%.3f,%.3f) m%.3f lag%.4f/%s",
                    body, region, origin, dirX, dirY, magnitude, region.lag(), region.anchor());
        }
    }

    /** Why the camera is moving. STYLE.md 9 names three of these and forbids a fourth. */
    enum CameraReason {
        /** "Slight slow drift -- the camera is never perfectly still." */
        DRIFT,
        /** "On queue execution, the camera glides toward the exchange over ~0.5 s." */
        PUSH_IN,
        /** Following the exchange from beat to beat, still inside the close framing. */
        TRACK,
        /** "Return is slower than the push-in (~0.8 s)." */
        RETURN
    }

    /**
     * One eased, continuous camera move. There is no cut in this vocabulary,
     * which is how STYLE.md 9's "never cut" is enforced rather than remembered:
     * a key carries both endpoints and a duration, and
     * {@link Schedule#cameraIsContinuous()} checks that consecutive keys join.
     */
    record CameraKey(Framing from, Framing to, double at, double duration, Ease ease,
                     CameraReason reason) implements Directive {

        @Override
        public int body() {
            return NO_BODY;
        }

        /** The framing at raw time {@code t}, eased. */
        public Framing framingAt(double t) {
            double u = duration <= 0 ? 1.0 : (t - at) / duration;
            return new Framing(ease.lerp(from.centreTile(), to.centreTile(), u),
                    ease.lerp(from.widthTiles(), to.widthTiles(), u));
        }

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "cam %s %s->%s %s", reason, from, to, ease);
        }
    }

    /**
     * The body coming round. combat-design.md 2.2 makes a Turn "the whole body
     * winding around", and {@link dev.starfall.combat.Phases#WIND_AROUND} gives it
     * 65% recovery so cloth and hair arrive last -- which is a statement about how
     * long this takes, not about whether it happens.
     */
    record FacingChange(int body, Facing from, Facing to, double at, double duration,
                        Ease ease) implements Directive {

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "face %d %s->%s %s", body, from, to, ease);
        }
    }

    /**
     * The whole body's condition, and where it is looking.
     *
     * @param settle how long the change takes to come to rest, inside STYLE.md
     *               7.1's band -- 4b.6 requires expression to settle "same as
     *               everything else" and explicitly bans popping between states
     */
    record PoseChange(int body, Stance stance, Anchor gaze, double at, double duration,
                      double settle) implements Directive {

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "pose %d %s gaze%s settle%.3f", body, stance, gaze, settle);
        }
    }

    /**
     * STYLE.md 7.3's held breath. Global, and the only directive with no body.
     *
     * <p>It is a directive rather than a renderer convention because the alternative
     * is that a renderer decides for itself what an impact feels like, and every
     * default in the game-feel box -- hitstop, freeze, shake -- is banned by name in
     * 7.1 and again in 10.
     */
    record TimeRamp(double scale, double at, double duration) implements Directive {

        @Override
        public int body() {
            return NO_BODY;
        }

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "time x%.3f", scale);
        }
    }

    /** What kind of mark. Each one is named in STYLE.md 3, 5 or 7.3. */
    enum InkKind {
        /** 7.3: "a bloom of ink spreading from the contact point... like a drop hitting wet paper". */
        BLOOM,
        /** 7.3: "the dissolve threshold pushed locally so the struck area sheds brush flecks". */
        FLECKS,
        /** 5: the swung blade's "soft luminous arc-trail that fades over ~0.4 s", and it must curve. */
        TRAIL,
        /** 5: "a soft star bloom with 4-6 long soft rays, plus 8-20 warm embers." Blade on blade only. */
        CLASH,
        /** 1.4's Marked: "a vermillion seal blooms on the body." The one loud colour, on a budget. */
        SEAL,
        /** 3 and 7.3: the body going, over the beats {@code Dissolve} counted. */
        DISSOLVE
    }

    /**
     * A mark of ink, somewhere, travelling somewhere, for a while.
     *
     * @param magnitude 0 to 1, from {@link dev.starfall.combat.Force} where there is one
     */
    record Ink(int body, InkKind kind, Anchor origin, double dirX, double dirY,
               double magnitude, double at, double duration) implements Directive {

        @Override
        public String describe() {
            return String.format(Locale.ROOT, "ink %d %s %s dir(%.3f,%.3f) m%.3f",
                    body, kind, origin, dirX, dirY, magnitude);
        }
    }
}
