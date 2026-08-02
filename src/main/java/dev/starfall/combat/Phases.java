package dev.starfall.combat;

/**
 * The inside of a beat: wind-up, contact, recovery, as <b>proportions</b>.
 *
 * <p><b>Why the engine says this at all.</b> Until now a beat was an instant --
 * {@code Swung}, then {@code Hit}, and nothing about the shape of the gesture
 * that produced them. STYLE.md 7.1 is explicit that the shape is the aesthetic:
 * "a strike is roughly 40% wind-up / 15% travel / 45% follow-through. Fighting-game
 * timing (fast wind-up, hard freeze on impact) is the exact opposite and is
 * forbidden." A stream that only says <em>what</em> happened lets the animation
 * layer invent that split, and the first thing an animator reaches for when
 * nobody has specified it is the forbidden one.
 *
 * <p><b>Why it is unit-free.</b> The three numbers are parts of {@link #WHOLE},
 * not seconds. The engine never learns how long a beat is; the renderer chooses
 * that, and may stretch or compress it freely, because everything here survives
 * scaling. That keeps determinism a property of the rules rather than of a clock
 * -- see {@code DeterminismTest}.
 *
 * <p><b>The two instants that matter.</b> {@link #contactStart()} is the moment
 * the gesture arrives -- the frame two skeletons must agree on to cross blades --
 * and {@link #recoveryStart()} is where the beat stops committing and starts
 * settling. STYLE.md 7.1's terminal damping lives entirely in the recovery span,
 * which is why every shape here gives recovery the largest share except the
 * ones that are all anticipation.
 *
 * <p><b>Two invariants, and both are load-bearing.</b> Every phase is strictly
 * positive, and the three sum to {@link #WHOLE}. Positive wind-up is what makes
 * {@link Overlap}'s guarantee true: a beat that may only eat into the previous
 * beat's <em>recovery</em> starts after the previous contact has finished, and
 * then waits out its own wind-up before its own contact, so two contacts can
 * never coincide however the renderer scales them. That is STYLE.md 7.0's third
 * positive -- "nothing may arrive at the same time" -- discharged in the rules
 * rather than left to the renderer's good taste.
 *
 * @param windUp   anticipation: the body gathering. STYLE.md 7.1 wants this long.
 * @param contact  the release, and the only span in which two bodies touch.
 * @param recovery follow-through and settle. Never zero: nothing arrives at rest
 *                 abruptly.
 */
public record Phases(int windUp, int contact, int recovery) {

    /** The whole of one beat. Parts, not seconds -- the engine owns no clock. */
    public static final int WHOLE = 100;

    /**
     * STYLE.md 7.1, verbatim: 40% wind-up, 15% travel, 45% follow-through. Every
     * blade in the game -- the hero's Cut, Thrust and Sweep, and every Charted
     * Shadow's declared attack -- is shaped this way.
     */
    public static final Phases STRIKE = new Phases(40, 15, 45);

    /**
     * The Draw. combat-design.md 2.2 calls it "contact at distance -- a line of
     * force between two figures", so the contact span is the widest of any strike:
     * the hook is <em>held</em> while the body comes in, rather than released.
     */
    public static final Phases REACH = new Phases(35, 25, 40);

    /**
     * The Parry. The longest anticipation and the shortest contact in the set,
     * because STYLE.md 7.2 wants "a deflection curve, not a collision" -- the
     * gesture is almost entirely the raising of the guard and the giving of
     * ground, and the blades touch for a sliver.
     */
    public static final Phases GUARD = new Phases(45, 10, 45);

    /**
     * Weight transfer: Step, Back-step, Feint, and every Charted Shadow's walk.
     * Contact here is the foot landing, and the long recovery is STYLE.md 7.1's
     * slow-in-slower-out -- a body that has stopped travelling is still settling.
     */
    public static final Phases TRAVEL = new Phases(30, 20, 50);

    /**
     * The Turn. combat-design.md 2.2: "the whole body winding around; cloth and
     * hair last to arrive." Two thirds of this beat is the arriving.
     */
    public static final Phases WIND_AROUND = new Phases(25, 10, 65);

    /**
     * A body that does nothing and still occupies a beat: a held intent, or a
     * figure whose pigment has dried under {@link Status#STILLNESS}. It gathers,
     * holds, and releases nothing.
     */
    public static final Phases BREATH = new Phases(40, 10, 50);

    /**
     * A death by blade, by collision or by a wound. Almost all aftermath: the
     * body gives early and the ink keeps spreading, which is STYLE.md 7.3's
     * "bloom of ink ... like a drop hitting wet paper" rather than a fall.
     */
    public static final Phases DEATH = new Phases(15, 10, 75);

    /**
     * A death thrown apart by an Explosive bloom. Almost no anticipation and a
     * wide contact -- the ink is flung rather than allowed to settle.
     */
    public static final Phases BURST = new Phases(10, 20, 70);

    public Phases {
        if (windUp <= 0 || contact <= 0 || recovery <= 0) {
            throw new IllegalArgumentException(
                    "every phase must be positive, or contacts stop being strictly ordered: "
                            + windUp + "/" + contact + "/" + recovery);
        }
        if (windUp + contact + recovery != WHOLE) {
            throw new IllegalArgumentException("phases must sum to " + WHOLE + ": "
                    + windUp + "/" + contact + "/" + recovery);
        }
    }

    /** The shape of a beat spent on one tile of the Ink Stanza. */
    public static Phases of(TileType type) {
        return switch (type) {
            case CUT, THRUST, SWEEP -> STRIKE;
            case DRAW -> REACH;
            case PARRY -> GUARD;
            case STEP, BACK_STEP, FEINT -> TRAVEL;
            case TURN -> WIND_AROUND;
        };
    }

    /** The shape of a beat spent by a Charted Shadow resolving its Strikethrough. */
    public static Phases of(Intent.Kind kind) {
        return switch (kind) {
            case ATTACK -> STRIKE;
            case ADVANCE, WITHDRAW, CLOSE_IN -> TRAVEL;
            case HOLD -> BREATH;
        };
    }

    /** The instant the gesture arrives. Two bodies in contact synchronise here. */
    public int contactStart() {
        return windUp;
    }

    /** The instant the gesture stops committing. */
    public int contactEnd() {
        return windUp + contact;
    }

    /** Same instant as {@link #contactEnd()}, named for the phase that begins there. */
    public int recoveryStart() {
        return contactEnd();
    }

    /** The contact instant of a beat resolved as a single stroke. */
    public int contactAt() {
        return contactAt(0, 1);
    }

    /**
     * The contact instant of stroke {@code stroke} of {@code strokes}, in the
     * whole beat's scale.
     *
     * <p>This exists for one mechanic and it is the right shape for it: the
     * Double Strike enchantment resolves the same beat twice
     * ({@code PhraseTest.doubleStrikeResolvesTheSameBeatTwice}), and if both
     * strokes reported the same contact instant the animation layer would land
     * two blows on one frame -- STYLE.md 10's fail-on-sight row. The beat is
     * subdivided instead, so a doubled Cut contacts at 20 and again at 70 rather
     * than twice at 40.
     */
    public int contactAt(int stroke, int strokes) {
        if (strokes < 1 || stroke < 0 || stroke >= strokes) {
            throw new IllegalArgumentException("stroke " + stroke + " of " + strokes);
        }
        int span = WHOLE / strokes;
        return stroke * span + windUp * span / WHOLE;
    }

    @Override
    public String toString() {
        return windUp + "/" + contact + "/" + recovery;
    }
}
