package dev.starfall.stage;

import dev.starfall.combat.Phases;

/**
 * Every absolute number in the staging layer, and where in STYLE.md 7 it comes
 * from.
 *
 * <p><b>Why this file exists at all.</b> The combat engine is deliberately
 * ordinal -- turns, tiles, proportions, sides -- and combat-design.md 3d.5 says
 * so in as many words: "the moment the engine learns what a second is, the
 * reproducibility the whole review loop depends on becomes a rendering concern."
 * Somebody nonetheless has to name a second. This is the only file in the
 * project that does, and every constant in it is derived rather than picked.
 *
 * <h2>The derivation, in the order the constraints bind</h2>
 *
 * <p><b>1. The settle band is authored first, because STYLE.md 7.1 states it as
 * an interval and not as a point:</b> "terminal damping should let a limb settle
 * over 0.3-0.6 s after the visible motion has 'ended'." An interval with two
 * endpoints and a chain with several links is not a coincidence -- 7.0.3 wants
 * the links to arrive at <em>different</em> times, and 10's last row fails a pass
 * on sight of everything peaking together. So the band is not a tolerance to pick
 * a number out of: it is the range the chain is spread across. {@link Settle}
 * spends it from {@link #SETTLE_ROOT} at the pelvis to {@link #SETTLE_TIP} at the
 * blade tip, and nothing in the rig settles outside it.
 *
 * <p><b>2. The beat length follows from the settle, through the recovery
 * span.</b> {@link Phases}' own documentation puts the terminal damping "entirely
 * in the recovery span". A {@link Phases#STRIKE} gives recovery 45 parts of the
 * beat, and the recovery of the shortest beat in the game must be long enough to
 * contain the middle of the settle chain -- otherwise the hand is still visibly
 * arriving when the next beat's wind-up has already begun, which is legal for the
 * blade tip (that is the poetry) and wrong for the hand (that is a beat that never
 * finished). Taking the middle of the chain at about 0.50 s:
 *
 * <pre>{@code   BASE_BEAT = 0.50 / 0.45 = 1.111...  ->  1.12 s}</pre>
 *
 * <p>which puts a strike's recovery at 0.504 s -- the elbow, the wrist and most of
 * the tip's drift inside the beat that produced them, and the last of the tip
 * spilling into the next beat's wind-up, which is exactly the overlap 7.1 asks
 * for.
 *
 * <p><b>3. And the same number is confirmed independently by STYLE.md 7.2's
 * knockback.</b> "A struck figure should be carried backward like a sheet of silk
 * caught in wind, arriving over ~0.8 s." A shove resolves inside a
 * {@link Phases#TRAVEL} beat (30/20/50), the carry begins at contact and runs to
 * the end of the beat, so the carry span is 70 parts:
 *
 * <pre>{@code   0.70 * 1.12 = 0.784 s   against 7.2's ~0.8 s}</pre>
 *
 * Two unrelated numbers in STYLE.md 7, one base beat, 2% apart. That agreement is
 * the reason {@link #BASE_BEAT} is 1.12 and not a taste.
 *
 * <p><b>4. Beat length grows with the run of tiles the beat spans, sub-linearly.</b>
 * A Runner collapsing twelve tiles and a Wisp stepping one are not the same shot
 * (combat-design.md 3d.5 correction 1) and must not be the same length either. A
 * constant beat would make the charge twelve times the speed of a step, which is
 * the strobe STYLE.md 7.2 forbids; a beat linear in tiles would make it exactly as
 * fast as a step, which stops it being a charge. {@link #SPAN_EXPONENT} splits the
 * difference: total time grows, per-tile speed grows, neither runs away. At 0.35 a
 * twelve-tile collapse runs about five times the per-tile speed of a single step.
 * The stretch is measured on a beat's <em>reach</em> rather than on its raw span --
 * see {@link #beatSeconds(int)} -- so the ordinary one-tile action stays exactly
 * {@link #BASE_BEAT} and both derivations above stay exact rather than approximate.
 *
 * <p><b>5. The lags name their anchors, because STYLE.md 7.1 requires it.</b>
 * "A hem trails the hips; a sleeve trails the wrist, which is itself already far
 * behind the hips... a lag figure quoted without its anchor is unfalsifiable."
 * {@link Region} carries the anchor, and {@link Region#netLagBehindHips()} prints
 * the other reading, so both numbers exist and neither can be quoted alone.
 */
public final class Timing {

    private Timing() {
    }

    // -- the settle band (STYLE.md 7.1) ---------------------------------------

    /** The near end of STYLE.md 7.1's terminal settle band. The pelvis. */
    public static final double SETTLE_ROOT = 0.30;

    /** The far end of the same band. The blade tip, a beat behind the hand. */
    public static final double SETTLE_TIP = 0.60;

    /**
     * Where the hips arrive. The anchor every other lag in the layer is quoted
     * against, per STYLE.md 7.1's instruction to name one.
     */
    public static final double ARRIVAL_HIPS = SETTLE_ROOT;

    /** Where the head arrives: after the ribcage, before the shoulder rolls. */
    public static final double ARRIVAL_HEAD = 0.43;

    /** Where the wrist arrives. The sleeve's anchor, and 0.27 s behind the hips. */
    public static final double ARRIVAL_WRIST = 0.57;

    // -- the beat --------------------------------------------------------------

    /** One beat spanning a single tile, in seconds. See the class note, step 2. */
    public static final double BASE_BEAT = 1.12;

    /** How beat length grows with {@code Focus.span()}. See the class note, step 4. */
    public static final double SPAN_EXPONENT = 0.35;

    /** STYLE.md 7.2's knockback arrival, for the test that checks the mapping honours it. */
    public static final double KNOCKBACK_ARRIVAL = 0.80;

    /**
     * How far the hips lead the hand inside one beat, as a fraction of the beat's
     * own wind-up.
     *
     * <p>STYLE.md 7.0.1: "the hip turns before the shoulder, which turns before the
     * elbow, which turns before the tip. The arm is never the subject; it is the end
     * of a sentence the body started." {@code RigIk} already does half of this with a
     * filter -- the trunk settles at 0.24 s against the arm's 0.34 -- and records
     * that "lag in the filter and lead in the script are two different halves of the
     * same spiral and this rig uses both". This is the script half. Proportional to
     * the wind-up rather than fixed, so a long gathering leads by more than a short
     * one, floored so a fast beat still leads by something readable.
     */
    public static final double HIP_LEAD_FRACTION = 0.15;

    /** Floor on the hip's lead, about three frames at 60 Hz. */
    public static final double HIP_LEAD_MIN = 0.05;

    /** The clavicle sits between the hips and the hand, at this fraction of the lead. */
    public static final double CLAVICLE_LEAD_FRACTION = 0.35;

    // -- hit reactions (STYLE.md 7.3) ------------------------------------------

    /** "A held breath -- a brief slowing of everything (a soft time ramp, ~0.85x...)". */
    public static final double HELD_BREATH_SCALE = 0.85;

    /** "...for ~0.25 s), never a hard freeze." */
    public static final double HELD_BREATH_SECONDS = 0.25;

    /** How long a yielding takes to give and come back, as a fraction of the beat's recovery. */
    public static final double YIELD_FRACTION = 0.55;

    // -- camera (STYLE.md 9) ---------------------------------------------------

    /** "the camera glides (never cuts) toward the exchange over ~0.5 s with a soft ease". */
    public static final double PUSH_IN = 0.50;

    /** "Return is slower than the push-in (~0.8 s)." */
    public static final double RETURN = 0.80;

    /**
     * Shortest camera move the layer will emit. STYLE.md 9 bans cuts and 10 bans
     * shake, and both are the same defect measured over different durations -- a
     * move short enough is a cut, and a run of short moves is shake. A floor is the
     * only way to make "never cut, never shake" checkable rather than a matter of
     * taste.
     */
    public static final double CAMERA_MIN_MOVE = 0.25;

    /** A gap in camera work shorter than this is absorbed into the next move rather than drifted. */
    public static final double CAMERA_MIN_DRIFT = 0.30;

    /**
     * STYLE.md 9: "the camera is never perfectly still". Amplitude of the idle
     * drift, in tiles.
     */
    public static final double CAMERA_DRIFT_TILES = 0.14;

    /** And the breathing of the framing itself, in tiles of width. */
    public static final double CAMERA_DRIFT_WIDTH = 0.06;

    // -- derived ---------------------------------------------------------------

    /**
     * Seconds for a beat over a run of {@code span} tiles. See the class note,
     * step 4.
     *
     * <p>The stretch is measured on the beat's <b>reach</b> -- the tiles beyond the
     * actor's own -- and not on the raw span, and the difference is load-bearing.
     * Nearly every action in the game has a span of two: a Cut reaches one tile
     * ahead, a Step covers the tile it leaves and the tile it lands on, an enemy
     * Advance the same. That is the <em>unit</em> beat, the one both §7 numbers were
     * derived against, so it must come out at exactly {@link #BASE_BEAT}. A beat
     * that goes nowhere at all -- a Parry, a Turn, a held breath -- is not shorter
     * than the unit beat; it is the same length spent differently, which is what
     * {@link Phases} already says by giving each of them its own split of the same
     * whole. Only reaching past one tile lengthens a beat.
     */
    public static double beatSeconds(int span) {
        int reach = Math.max(1, span - 1);
        return BASE_BEAT * Math.pow(reach, SPAN_EXPONENT);
    }

    /** Seconds for {@code parts} of {@link Phases#WHOLE} of a beat {@code seconds} long. */
    public static double parts(double seconds, int parts) {
        return seconds * parts / Phases.WHOLE;
    }

    /** How far the hips lead the hand in a beat whose wind-up is {@code windUpSeconds}. */
    public static double hipLead(double windUpSeconds) {
        return Math.max(HIP_LEAD_MIN, windUpSeconds * HIP_LEAD_FRACTION);
    }
}
