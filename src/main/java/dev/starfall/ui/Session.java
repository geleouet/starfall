package dev.starfall.ui;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.Combatant;
import dev.starfall.combat.Command;
import dev.starfall.combat.EncounterOutcome;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Resolution;
import dev.starfall.combat.Tile;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.ScheduledBeat;
import dev.starfall.stage.Scheduler;
import dev.starfall.stage.Stage;
import dev.starfall.stage.Standing;

import java.util.ArrayList;
import java.util.List;

/**
 * A fight being <b>played</b>: the engine, the scheduler and the readout joined
 * to a source of commands, one continuous timeline, no GL anywhere.
 *
 * <h2>What this is, and what it deliberately is not</h2>
 *
 * <p>Seven rendering systems were built before anything let a person choose an
 * action, and the three layers this class joins all existed and were all tested:
 * {@code CombatEngine} plays the rules, {@code Scheduler} maps the ordinal stream
 * to seconds, {@code Readout} records what the sheet says. What none of them had
 * was a caller whose commands arrive on a <em>wall clock</em>. This class is that
 * caller's adapter and nothing else -- it invents no rules, no seconds and no
 * marks. Every decision stays in the layer that owns it.
 *
 * <p>The one genuinely new mapping it owns is between the player's clock and the
 * schedule's: a command given at director time {@code now} floors its beats at
 * {@code now} ({@link Scheduler#notBefore}), so nothing is ever staged in the
 * past of a picture already drawn, and the planning pauses that {@code Bout}
 * scripted are here simply the time the player actually took to think.
 *
 * <h2>The turn gate</h2>
 *
 * <p>Commands are accepted only when the score has gone quiet
 * ({@link #quietAt}). The alternative -- banking against a board whose bodies
 * are mid-phrase -- would show the player a stanza written against a state the
 * picture has not caught up with. The all-or-nothing rule of
 * combat-design.md 1.1 makes the resolution uninterruptible <em>in the
 * rules</em>; this gate makes it uninterruptible in the picture too. What that
 * costs at the game's authored tempo, and how much of it fast-forward buys
 * back, is the input pass's central open question and is reported rather than
 * hidden.
 */
public final class Session {

    /**
     * Seconds between a command and its first beat. Not zero: a beat starting on
     * the exact frame the key went down reads as the figure anticipating the
     * player. One breath of gap keeps cause visibly ahead of effect.
     */
    public static final double LEAD = 0.06;

    private final CombatEngine engine;
    private final Stage stage;
    private final Scheduler scheduler;
    private final Readout.Builder readout;
    private final List<Bout.Staged.Body> bodies = new ArrayList<>();
    private final int heroId;

    private Schedule schedule;
    private Readout current;
    private double busyUntil;
    private double endedAt = Double.NaN;

    public Session(EncounterSpec spec) {
        this.engine = CombatEngine.create(spec);
        this.stage = Stage.of(engine.state().lane());
        for (Combatant c : engine.state().all()) {
            bodies.add(new Bout.Staged.Body(c.id(), c.tile(), c.facing(), c.isHero()));
        }
        this.heroId = engine.state().hero().id();
        this.scheduler = new Scheduler(stage, opening());
        this.readout = Readout.builder(stage);
        scheduler.accept(engine.opening());
        readout.board(0.0, engine.state(), Readout.footprint(engine.previewExecution()));
        refresh();
        this.busyUntil = 0.0;
    }

    private Standing opening() {
        List<Standing.Body> out = new ArrayList<>();
        for (Bout.Staged.Body b : bodies) {
            out.add(new Standing.Body(b.id(), b.tile(), b.facing()));
        }
        return Standing.opening(out);
    }

    // -- what the scene reads ---------------------------------------------------

    /** The opening board, for building figures. Pale first is the caller's business. */
    public List<Bout.Staged.Body> bodies() {
        return List.copyOf(bodies);
    }

    public int heroId() {
        return heroId;
    }

    public Stage stage() {
        return stage;
    }

    /** The score so far. Replaced, never mutated, on every accepted command. */
    public Schedule schedule() {
        return schedule;
    }

    /** The sheet's timeline so far. Same replacement discipline. */
    public Readout readout() {
        return current;
    }

    /**
     * The engine, read-only by convention. Exposed because a player's interface
     * has to ask questions the Look does not carry -- "can this tile be banked" --
     * and because a scripted pilot <em>is</em> a player and decides from the same
     * board a human would.
     */
    public CombatEngine engine() {
        return engine;
    }

    /** When the last placed beat ends: the score is quiet after this. */
    public double busyUntil() {
        return busyUntil;
    }

    /** True when every placed beat has resolved by {@code t}. */
    public boolean quietAt(double t) {
        return t + 1e-6 >= busyUntil;
    }

    public EncounterOutcome outcome() {
        return engine.state().outcome();
    }

    /**
     * When the final beat of a decided fight resolves, or NaN while it is live.
     * The scene's epilogue -- the interface drying off the sheet -- starts here.
     */
    public double endedAt() {
        return endedAt;
    }

    // -- the input loop ----------------------------------------------------------

    /**
     * One command from the player, at director time {@code now}.
     *
     * @return true when the command was legal, the score was quiet, and the fight
     *         is still on -- i.e. when the schedule and readout were replaced and
     *         the caller must {@code rescore} its director
     */
    public boolean command(Command cmd, double now) {
        if (engine.state().outcome().over() || !quietAt(now) || !engine.can(cmd)) {
            return false;
        }
        scheduler.notBefore(now + LEAD);
        int before = scheduler.beats().size();
        Tile bankedTile = cmd instanceof Command.Add a
                ? engine.state().loadout().tile(a.loadoutIndex()) : null;

        Resolution r = engine.apply(cmd);
        scheduler.accept(r);

        List<ScheduledBeat> added =
                scheduler.beats().subList(before, scheduler.beats().size());
        double opensAt = added.isEmpty() ? now : added.get(0).start();
        double endsAt = now;
        for (ScheduledBeat b : added) {
            endsAt = Math.max(endsAt, b.end());
        }

        if (cmd instanceof Command.Add) {
            // The mark is written when the player writes it; the enemy answer is
            // the board the next chapter shows. Exactly Bout's stamping, with the
            // player's own instant in place of the scripted one.
            readout.bank(now, bankedTile);
            readout.board(opensAt, engine.state(), Readout.footprint(engine.previewExecution()));
        } else if (cmd instanceof Command.Remove) {
            readout.erase(now);
            readout.board(now, engine.state(), Readout.footprint(engine.previewExecution()));
        } else if (cmd instanceof Command.Execute) {
            List<ScheduledBeat> clauses = new ArrayList<>();
            for (ScheduledBeat b : added) {
                if (b.bracket() == ScheduledBeat.Bracket.STANZA) {
                    clauses.add(b);
                }
            }
            readout.resolve(clauses);
            double phraseEnd = clauses.isEmpty() ? opensAt
                    : clauses.get(clauses.size() - 1).end();
            readout.board(phraseEnd, engine.state(), List.of());
        } else {
            readout.board(opensAt, engine.state(), Readout.footprint(engine.previewExecution()));
        }

        refresh();
        busyUntil = Math.max(busyUntil, endsAt);
        if (engine.state().outcome().over() && Double.isNaN(endedAt)) {
            endedAt = busyUntil;
        }
        return true;
    }

    /** Rebuilds the published schedule and readout from the accumulated timeline. */
    private void refresh() {
        // scheduler.schedule() flushes the pending return-to-wide, which is
        // exactly what should happen at the moment a turn's beats are all placed:
        // the camera goes back to the plan while the player thinks.
        schedule = scheduler.schedule();
        current = readout.build(schedule, scheduler.wounds(), scheduler.passings());
    }

    /** The scheduler's second-exact record of blows. Read-only. */
    public List<Scheduler.Wound> wounds() {
        return scheduler.wounds();
    }

    /** And of deaths. Read-only. */
    public List<Scheduler.Passing> passings() {
        return scheduler.passings();
    }

    /** The first hand index that can legally be banked, or -1. A pilot's helper. */
    public int firstReady() {
        for (int i = 0; i < engine.state().loadout().size(); i++) {
            if (engine.can(Command.add(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Convenience for pilots: the nearest living Charted Shadow, or null. */
    public Combatant nearestShadow() {
        Combatant hero = engine.state().hero();
        Combatant best = null;
        int bestD = Integer.MAX_VALUE;
        for (Combatant c : engine.state().enemies()) {
            int d = Math.abs(c.tile() - hero.tile());
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /** Which way the hero would have to face to look at {@code tile}. */
    public Facing towards(int tile) {
        return tile >= engine.state().hero().tile() ? Facing.RIGHT : Facing.LEFT;
    }
}
