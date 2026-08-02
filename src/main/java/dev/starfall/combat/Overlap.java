package dev.starfall.combat;

/**
 * How far into the previous beat this one may begin.
 *
 * <p><b>This is the field that decides whether five queued tiles read as one
 * phrase or as five separate events.</b> combat-design.md 1.1a bought the
 * five-slot Ink Stanza specifically to hand the animation layer "a sentence with
 * clauses" rather than five hits, and STYLE.md 7.1 requires overlapping action
 * while STYLE.md 10 fails a pass on sight of everything peaking on the same
 * frame. Both of those are claims about the seam <em>between</em> beats, and
 * until now the stream said nothing about seams at all -- an animation layer
 * reading it had no choice but to play each beat to its end, which is the
 * definition of five separate events.
 *
 * <p><b>The measure: parts of the previous beat's recovery, never of the whole
 * beat.</b> This is what makes the hint safe. {@link Phases} guarantees a
 * strictly positive wind-up, so a beat that starts no earlier than the previous
 * beat's {@link Phases#recoveryStart()} necessarily contacts after the previous
 * beat has contacted. <b>Contacts stay strictly ordered no matter how the
 * renderer scales the beats</b>, which is STYLE.md 7.0's third positive
 * discharged in the rules. A hint measured against the whole beat could not
 * promise that, and the promise is the entire value of the hint.
 *
 * <p><b>What forbids an overlap.</b> A beat may not begin early when its own
 * geometry is the previous beat's output -- when the actor's footing or facing
 * is still being decided, or when a body it is about to read is still being
 * moved. Those are named, not merged into a boolean, because the animation layer
 * treats them differently: a beat waiting on footing can still <em>lean</em>,
 * and a beat waiting on the board cannot even aim.
 *
 * <p>The numbers are the deliberate part. Nothing is 100: even two entirely
 * unrelated bodies are staggered, because the anti-pattern table bans
 * simultaneity as such and not merely causally-linked simultaneity.
 *
 * @param intoRecovery how much of the previous beat's recovery span this beat may
 *                     consume, in parts of {@link Phases#WHOLE}. Zero means the
 *                     previous beat must finish first.
 * @param limit        what set that number
 */
public record Overlap(int intoRecovery, Overlap.Limit limit) {

    /** Two beats by the same body that depend on nothing may share this much. */
    public static final int CONTINUING = 60;

    /** Two beats by different bodies that touch nothing of each other's. */
    public static final int INDEPENDENT = 85;

    /** Why a beat may or may not begin before the last one has settled. */
    public enum Limit {
        /** Nothing precedes it. The first clause of a phrase, or of the enemy phase. */
        FIRST_BEAT,
        /**
         * The previous beat is still deciding where this body stands. A Step may be
         * blocked, may shove, may swap; until it resolves, this beat has no ground
         * to start from.
         */
        AWAITS_FOOTING,
        /**
         * The previous beat is still turning this body. combat-design.md 2.2: "the
         * whole body winding around; cloth and hair last to arrive." A stroke that
         * began before the body came round would be aimed at the old facing.
         */
        AWAITS_FACING,
        /**
         * The previous beat is still moving a body this one is about to read -- a
         * Draw hauling a target into the tile this stroke is aimed at, or a Charted
         * Shadow walking through the corridor the next one wants.
         */
        AWAITS_BOARD,
        /** The same body, carrying on. It may lean into the previous follow-through. */
        CONTINUES,
        /** A different body, touching nothing of the previous one's. */
        UNRELATED
    }

    public Overlap {
        if (intoRecovery < 0 || intoRecovery > Phases.WHOLE) {
            throw new IllegalArgumentException("overlap outside 0.." + Phases.WHOLE + ": " + intoRecovery);
        }
        if (limit == null) {
            throw new IllegalArgumentException("an overlap says what set it");
        }
    }

    public static Overlap firstBeat() {
        return new Overlap(0, Limit.FIRST_BEAT);
    }

    public static Overlap awaitingFooting() {
        return new Overlap(0, Limit.AWAITS_FOOTING);
    }

    public static Overlap awaitingFacing() {
        return new Overlap(0, Limit.AWAITS_FACING);
    }

    public static Overlap awaitingBoard() {
        return new Overlap(0, Limit.AWAITS_BOARD);
    }

    public static Overlap continues() {
        return new Overlap(CONTINUING, Limit.CONTINUES);
    }

    public static Overlap unrelated() {
        return new Overlap(INDEPENDENT, Limit.UNRELATED);
    }

    /** True when this beat must wait for the previous one to settle completely. */
    public boolean forbidden() {
        return intoRecovery == 0;
    }

    @Override
    public String toString() {
        return intoRecovery + "% " + limit;
    }
}
