package dev.starfall.stage;

import dev.starfall.combat.Focus;
import dev.starfall.combat.Overlap;
import dev.starfall.combat.Phases;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One beat placed in seconds: when it starts, how long it runs, and where inside
 * it each contact lands.
 *
 * <p><b>The placement law is {@link #after}, and it is the one piece of arithmetic
 * the whole layer is graded on.</b> The engine proved a theorem about its own
 * event stream -- {@link Overlap}'s note, and {@code OverlapTest}'s last case:
 *
 * <blockquote>An overlap is expressed in parts of the previous beat's
 * <em>recovery</em>, and recovery begins only after that beat's contact has
 * finished. So a beat honouring its hint starts no earlier than the previous
 * contact ended, and then waits out its own strictly positive wind-up before its
 * own contact. Two contacts therefore cannot coincide, <b>however the renderer
 * scales the beats</b>.</blockquote>
 *
 * <p>"However the renderer scales the beats" is a promise about a mapping that did
 * not exist when it was made. This is that mapping, and it is the thing that could
 * break the promise: consume more of the previous recovery than the hint allows,
 * or let a beat start before the hint says, and contacts stop being ordered. So
 * the law is one method, it is derived from the hint rather than from taste, and
 * {@code OverlapMappingTest} sweeps it over every legal combination rather than
 * over a few hand-built cases.
 *
 * @param index    the engine's own beat index within its bracket
 * @param bracket  which half of the turn this beat belongs to
 * @param actor    whose beat it is
 * @param start    absolute seconds
 * @param duration absolute seconds
 * @param contacts every instant at which something in this beat touches something,
 *                 in absolute seconds and ascending
 */
public record ScheduledBeat(int index, ScheduledBeat.Bracket bracket, int actor, Phases phases,
                            Overlap overlap, Focus focus, double start, double duration,
                            List<Double> contacts) {

    /** Which half of a turn a beat belongs to. Both are phrases -- combat-design.md 3d.5. */
    public enum Bracket {
        /** A clause of the hero's Ink Stanza. */
        STANZA,
        /** A Charted Shadow resolving its Strikethrough. */
        ENEMY
    }

    public ScheduledBeat {
        contacts = List.copyOf(contacts);
    }

    /**
     * Places a beat after {@code previous}, honouring its {@link Overlap} hint
     * exactly.
     *
     * <p>The hint says how much of the <em>previous beat's recovery span</em> this
     * beat may consume. In seconds that is
     * {@code previous.recoverySpan() * intoRecovery / 100}, and the beat starts
     * that far before the previous one ends. A hint of zero puts it exactly at the
     * previous beat's end; a hint of {@link Overlap#INDEPENDENT} leaves 15% of the
     * recovery unconsumed, which is why nothing in the game is ever simultaneous
     * even between bodies that share nothing.
     *
     * @param previous the beat before, or {@code null} for the first in the schedule
     */
    public static ScheduledBeat after(ScheduledBeat previous, int index, Bracket bracket, int actor,
                                      Phases phases, Overlap overlap, Focus focus) {
        double duration = Timing.beatSeconds(focus.span());
        double start;
        if (previous == null) {
            start = 0.0;
        } else {
            double consumed = previous.recoverySpan() * overlap.intoRecovery() / Phases.WHOLE;
            start = previous.end() - consumed;
        }
        List<Double> contacts = new ArrayList<>(1);
        contacts.add(start + Timing.parts(duration, phases.contactStart()));
        return new ScheduledBeat(index, bracket, actor, phases, overlap, focus, start, duration, contacts);
    }

    /**
     * The same beat moved later by {@code dt}, contacts and all.
     *
     * <p>Only ever used to insert planning time between two commands. Moving a beat
     * <em>later</em> can never break the ordering theorem -- it only widens the gap
     * to the beat before it -- which is why this is a shift and not a general
     * setter.
     */
    public ScheduledBeat shifted(double dt) {
        if (dt == 0.0) {
            return this;
        }
        List<Double> next = new ArrayList<>(contacts.size());
        for (double c : contacts) {
            next.add(c + dt);
        }
        return new ScheduledBeat(index, bracket, actor, phases, overlap, focus, start + dt, duration, next);
    }

    /** The same beat with another contact instant folded in, kept ascending. */
    public ScheduledBeat withContact(double instant) {
        List<Double> next = new ArrayList<>(contacts);
        int i = 0;
        while (i < next.size() && next.get(i) < instant) {
            i++;
        }
        // Both neighbours, not only the one at the insertion point. An engine event
        // that lands on the beat's own nominal contact -- which is the normal case,
        // since Swung.at is usually Phases.contactStart() -- reaches this method
        // having been recomputed through a different arithmetic path, so it can
        // differ from the seeded instant by an ulp and still be the same contact.
        if (i < next.size() && Math.abs(next.get(i) - instant) < 1e-9) {
            return this;
        }
        if (i > 0 && Math.abs(next.get(i - 1) - instant) < 1e-9) {
            return this;
        }
        next.add(i, instant);
        return new ScheduledBeat(index, bracket, actor, phases, overlap, focus, start, duration, next);
    }

    public double end() {
        return start + duration;
    }

    /** When the gathering ends and the release begins. STYLE.md 7.1's 40%, for a strike. */
    public double contactStart() {
        return start + Timing.parts(duration, phases.contactStart());
    }

    /**
     * When the release ends. The blades met at {@link #contactStart()}, slid through
     * the span, and part here -- combat-design.md 3d.5's resolution of the one
     * ambiguity in STYLE.md 7.1.
     */
    public double contactEnd() {
        return start + Timing.parts(duration, phases.contactEnd());
    }

    /** Where the beat stops committing and starts settling. Same instant as {@link #contactEnd()}. */
    public double recoveryStart() {
        return contactEnd();
    }

    /** The follow-through, in seconds. What an overlap eats into and nothing else. */
    public double recoverySpan() {
        return Timing.parts(duration, phases.recovery());
    }

    public double windUpSpan() {
        return Timing.parts(duration, phases.windUp());
    }

    public double contactSpan() {
        return Timing.parts(duration, phases.contact());
    }

    /** The absolute instant of a point given in {@link Phases#WHOLE} parts of this beat. */
    public double instant(int parts) {
        return start + Timing.parts(duration, parts);
    }

    /** How fast the subject crosses the lane, in tiles per second, over the carry span. */
    public double tilesPerSecond() {
        double carry = contactSpan() + recoverySpan();
        return carry <= 0 ? 0 : (focus.span() - 1) / carry;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "beat %s#%d actor%d %s %s %s [%.4f +%.4f]",
                bracket, index, actor, phases, overlap, focus, start, duration);
    }
}
