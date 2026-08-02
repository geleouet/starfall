package dev.starfall.stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A timed, positioned score: every beat placed in seconds, and every directive
 * that fires inside it.
 *
 * <p>Inert data. It holds no rig, no GL context and no reference to the engine, so
 * it can be built headless, compared for equality, printed, diffed between two
 * runs, and consumed several frames later by something that has never heard of a
 * {@code CombatState}. That is not an incidental property: the review loop the
 * whole project runs on depends on the same event stream producing the same
 * schedule byte for byte, and {@link #fingerprint()} is what makes that
 * checkable.
 *
 * <p>The queries are the half that gets used at runtime. {@link #weight} answers
 * "how much is this chain driving right now", which is the question a renderer
 * asks sixty times a second and the question "does this phrase read as one
 * phrase" turns out to reduce to.
 */
public record Schedule(List<ScheduledBeat> beats, List<Directive> directives, Stage stage) {

    public Schedule {
        beats = List.copyOf(beats);
        directives = List.copyOf(directives);
    }

    /** Wall time from the first beat to the last thing that is still moving. */
    public double duration() {
        double end = 0.0;
        for (ScheduledBeat b : beats) {
            end = Math.max(end, b.end());
        }
        for (Directive d : directives) {
            end = Math.max(end, d.end());
        }
        return end;
    }

    public <D extends Directive> List<D> of(Class<D> type) {
        return directives.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** Every directive on one body, in time order. */
    public List<Directive> forBody(int body) {
        return directives.stream().filter(d -> d.body() == body).toList();
    }

    /** Every IK directive on one chain of one body, in time order. */
    public List<Directive.IkTarget> chain(int body, Chain chain) {
        return of(Directive.IkTarget.class).stream()
                .filter(d -> d.body() == body && d.chain() == chain)
                .toList();
    }

    /**
     * How much {@code chain} is driving {@code body} at time {@code t}.
     *
     * <p>Segments ramp between their endpoints and <em>hold</em> afterwards, which
     * is what makes a chain's weight a continuous function of time rather than a
     * series of pulses. That distinction is the whole of "reads as one phrase":
     * a carrier chain whose weight never returns to zero between beats is one
     * gesture with clauses; one that ramps out and back in five times is five
     * events, and no amount of overlap in the beats disguises it.
     */
    public double weight(int body, Chain chain, double t) {
        double w = 0.0;
        for (Directive.IkTarget d : chain(body, chain)) {
            if (t <= d.at()) {
                break;
            }
            w = t >= d.end() ? d.weightTo() : d.ease().lerp(d.weightFrom(), d.weightTo(),
                    d.duration() <= 0 ? 1.0 : (t - d.at()) / d.duration());
        }
        return w;
    }

    /** The camera keys, in time order. */
    public List<Directive.CameraKey> camera() {
        return of(Directive.CameraKey.class);
    }

    /** Where the camera is at {@code t}. */
    public Framing framingAt(double t) {
        List<Directive.CameraKey> keys = camera();
        if (keys.isEmpty()) {
            return stage.planning();
        }
        Directive.CameraKey last = keys.get(0);
        for (Directive.CameraKey k : keys) {
            if (t < k.at()) {
                break;
            }
            last = k;
        }
        if (t < last.at()) {
            return last.from();
        }
        return t >= last.end() ? last.to() : last.framingAt(t);
    }

    /**
     * True when no camera key jumps: each one starts where the last one finished,
     * and none of them starts before the last one ended.
     *
     * <p>STYLE.md 9's "never cut" made checkable. A cut is exactly a discontinuity
     * between consecutive framings, so the ban is a statement about endpoints and
     * nothing else.
     */
    public boolean cameraIsContinuous() {
        List<Directive.CameraKey> keys = camera();
        for (int i = 1; i < keys.size(); i++) {
            Directive.CameraKey prev = keys.get(i - 1);
            Directive.CameraKey next = keys.get(i);
            if (next.at() < prev.end() - 1e-6) {
                return false;
            }
            if (Math.abs(next.from().centreTile() - prev.to().centreTile()) > 1e-6
                    || Math.abs(next.from().widthTiles() - prev.to().widthTiles()) > 1e-6) {
                return false;
            }
        }
        return true;
    }

    /** Every contact instant in the schedule, in the order the beats produced them. */
    public List<Double> contacts() {
        List<Double> out = new ArrayList<>();
        for (ScheduledBeat b : beats) {
            out.addAll(b.contacts());
        }
        return List.copyOf(out);
    }

    /** The beats of one bracket run, isolated -- the usual subject of a phrase assertion. */
    public List<ScheduledBeat> bracket(ScheduledBeat.Bracket bracket) {
        return beats.stream().filter(b -> b.bracket() == bracket).toList();
    }

    /**
     * A canonical rendering of the whole schedule.
     *
     * <p>The same role {@code CombatState.fingerprint()} plays for the board, and
     * for the same reason: comparing two schedules field by field says they differ,
     * and comparing two fingerprints says <em>where</em>. Fixed to
     * {@link Locale#ROOT} and to five decimals, because a determinism check that
     * depends on the machine's locale is not a determinism check.
     */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append("lane=").append(stage.laneLength())
                .append(" beats=").append(beats.size())
                .append(" directives=").append(directives.size())
                .append(String.format(Locale.ROOT, " duration=%.5f", duration()))
                .append('\n');
        for (ScheduledBeat b : beats) {
            sb.append("  ").append(b);
            for (double c : b.contacts()) {
                sb.append(String.format(Locale.ROOT, " c%.5f", c));
            }
            sb.append('\n');
        }
        for (Directive d : directives) {
            sb.append(String.format(Locale.ROOT, "  %.5f +%.5f %s", d.at(), d.duration(), d.describe()))
                    .append('\n');
        }
        return sb.toString();
    }
}
