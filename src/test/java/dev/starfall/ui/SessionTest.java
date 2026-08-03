package dev.starfall.ui;

import dev.starfall.combat.Command;
import dev.starfall.combat.EncounterOutcome;
import dev.starfall.combat.TileType;
import dev.starfall.direct.Director;
import dev.starfall.direct.Figure;
import dev.starfall.stage.Directive;
import dev.starfall.stage.ScheduledBeat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The input loop, headless: the same {@link Session} the keyboard and the debug
 * API drive, played by the same pilots the capture scenes use.
 *
 * <p>Every guard here was observed red during authoring (MEASUREMENT.md
 * 11.2b(f)); the break that turned each one red is named in its own note.
 */
class SessionTest {

    /** A player's cadence, in seconds between glances at the board. */
    private static final double GLANCE = 0.25;

    /** Plays a session with a pilot until it ends or the clock runs out. */
    private static double play(Session session, Plays.Pilot pilot, double limit) {
        double t = 0.0;
        while (!session.outcome().over() && t < limit) {
            if (session.quietAt(t)) {
                Command cmd = pilot.decide(session.engine());
                if (cmd != null) {
                    session.command(cmd, t);
                }
            }
            t += GLANCE;
        }
        return t;
    }

    // -- the two exhibits ---------------------------------------------------------

    /**
     * A fight can be LOST through the input loop, by a player who only watches
     * -- and the defeat resolves into the staged beat BP4 asks for: the hero's
     * own dissolve, not a game-over card.
     *
     * <p>Observed red by giving the bystander the duellist's brain (outcome
     * VICTORY), and by asserting a dissolve for a body id that never dies.
     */
    @Test
    void aFightCanBeLostThroughTheInputLoop() {
        Session session = new Session(Plays.defeat());
        play(session, Plays.bystander(), 300.0);

        assertEquals(EncounterOutcome.DEFEAT, session.outcome(),
                "a player who only holds must lose this fight");
        assertFalse(Double.isNaN(session.endedAt()), "a decided fight knows when it ended");

        // The hero leaves as ink, staged like every other death.
        boolean heroDissolves = false;
        for (Directive.Ink d : session.schedule().of(Directive.Ink.class)) {
            if (d.body() == session.heroId() && d.kind() == Directive.InkKind.DISSOLVE) {
                heroDissolves = true;
            }
        }
        assertTrue(heroDissolves, "defeat is a poetic beat: the Pilgrim dissolves");

        // And the sheet knows: no strokes left at the end.
        Look last = session.readout().at(session.endedAt() + 0.5);
        assertEquals(0, last.health(), "the health row must reach zero, not stop near it");
    }

    /**
     * And WON: the duellist banks during the approach and spends the phrase on
     * contact, which is combat-design.md 1.1a's own description of play.
     *
     * <p>Observed red by swapping in the bystander (clock runs out, outcome
     * ONGOING).
     */
    @Test
    void aFightCanBeWonThroughTheInputLoop() {
        Session session = new Session(Plays.victory());
        play(session, Plays.duellist(), 300.0);

        assertEquals(EncounterOutcome.VICTORY, session.outcome(),
                "the duellist pilot must be able to win its own exhibit");

        // The Shadow's row dries with its body: dying rises through the dissolve
        // and the row is gone once the ink is.
        assertEquals(1, session.passings().size(), "one Shadow, one passing");
        var passing = session.passings().get(0);
        Look mid = session.readout().at(passing.at() + passing.span() * 0.5);
        boolean dyingSeen = false;
        for (Look.Shadow s : mid.shadows()) {
            if (s.body() == passing.body()) {
                assertTrue(s.dying() > 0.0 && s.dying() < 1.0,
                        "mid-dissolve the row must be drying, not gone: " + s.dying());
                dyingSeen = true;
            }
        }
        assertTrue(dyingSeen, "the dissolving body still carries its row mid-dissolve");
        Look after = session.readout().at(passing.at() + passing.span() + 0.1);
        for (Look.Shadow s : after.shadows()) {
            assertFalse(s.body() == passing.body(),
                    "a body whose ink has gone keeps no counter");
        }
    }

    // -- the row drops when the blade lands ----------------------------------------

    /**
     * Enemy strokes drop at the blow's own second, not at the turn's bookkeeping.
     *
     * <p>Observed red by adding +0.5 s to the wound time in {@code Readout.hpAt}'s
     * comparison, which is exactly the drift this guard exists to catch.
     */
    @Test
    void theStrokeDriesAtTheSecondTheBladeLands() {
        Session session = new Session(Plays.victory());
        play(session, Plays.duellist(), 300.0);

        int wisp = session.passings().get(0).body();
        var first = session.wounds().stream()
                .filter(w -> w.body() == wisp).findFirst().orElseThrow();
        int before = shadowHp(session.readout().at(first.at() - 0.05), wisp);
        int after = shadowHp(session.readout().at(first.at() + 0.05), wisp);
        assertTrue(before > after,
                "the row must drop across the blow's instant: " + before + " -> " + after);
        assertEquals(first.hpAfter(), after,
                "and to the value the engine stated, not a decrement someone recomputed");
    }

    private static int shadowHp(Look look, int body) {
        for (Look.Shadow s : look.shadows()) {
            if (s.body() == body) {
                return s.hp();
            }
        }
        return -1;
    }

    // -- the queue's grammar --------------------------------------------------------

    /**
     * Un-banking is LIFO and the column never moves: rubbing out the top mark
     * leaves every other mark at its height, and the next bank writes onto the
     * freed line.
     *
     * <p>Observed red by erasing from the bottom of the pending list instead of
     * the top (heights then collide), and by asserting the wrong surviving type.
     */
    @Test
    void unbankingIsLifoAndNoMarkEverMoves() {
        Session session = new Session(Plays.bout(Bout.Kind.FOLD));
        double t = 0.0;

        t = apply(session, Command.add(0), t);
        TileType first = session.readout().at(t).stanza().get(0).type();
        t = apply(session, Command.add(1), t);
        assertEquals(2, session.readout().at(t).stanza().size());

        // Remove is free and instant: the top mark -- the LAST banked -- leaves.
        assertTrue(session.command(Command.remove(0), t), "unbanking must be accepted");
        Look look = session.readout().at(t + Readout.DRY_SECONDS + 0.1);
        assertEquals(1, look.stanza().size(), "one mark left after the erase has dried");
        assertEquals(first, look.stanza().get(0).type(),
                "the surviving mark is the FIRST banked: the erase took the top, LIFO");
        assertEquals(0, look.stanza().get(0).height(), "and it never moved off its line");

        // The freed line takes the next mark.
        t = Math.max(t, session.busyUntil()) + Readout.DRY_SECONDS + 0.2;
        t = apply(session, Command.add(2), t);
        Look again = session.readout().at(t);
        assertEquals(2, again.stanza().size());
        assertEquals(1, again.stanza().get(0).height(),
                "the new mark writes onto the freed line above the survivor");
    }

    /** Applies at the first quiet instant at or after {@code t}; returns when it ended. */
    private static double apply(Session session, Command cmd, double t) {
        double now = Math.max(t, session.busyUntil());
        assertTrue(session.command(cmd, now), "command must be accepted at a quiet instant: " + cmd);
        return session.busyUntil();
    }

    /**
     * The gate: while the score is resolving, commands are refused -- the
     * all-or-nothing rule holding in the picture, not only in the rules.
     *
     * <p>Observed red by dropping the {@code quietAt} clause from
     * {@code Session.command}.
     */
    @Test
    void commandsAreRefusedWhileTheScoreResolves() {
        Session session = new Session(Plays.bout(Bout.Kind.FOLD));
        assertTrue(session.command(Command.add(0), 0.0));
        assertTrue(session.busyUntil() > 0.5,
                "a banked turn must cost visible time (the enemy phase answers it)");
        assertFalse(session.command(Command.hold(), session.busyUntil() * 0.5),
                "mid-phrase input must be refused, not queued silently");
        assertTrue(session.command(Command.hold(), session.busyUntil()),
                "and accepted again the moment the score is quiet");
    }

    /**
     * Beats are never staged in the past of a picture already drawn: every beat
     * a command places starts at or after the instant the command was given.
     *
     * <p>Observed red by removing the {@code notBefore} call from
     * {@code Session.command} -- with a slow player the scheduler's own cursor
     * then places beats seconds before the command.
     */
    @Test
    void noBeatIsStagedBeforeTheCommandThatCausedIt() {
        Session session = new Session(Plays.bout(Bout.Kind.FOLD));
        double[] commandAt = {0.0, 4.0, 11.5, 12.5, 30.0};
        Command[] cmds = {Command.add(0), Command.hold(), Command.add(1),
                Command.hold(), Command.execute()};
        int seen = 0;
        for (int i = 0; i < cmds.length; i++) {
            double now = Math.max(commandAt[i], session.busyUntil());
            assertTrue(session.command(cmds[i], now), "command " + i);
            List<ScheduledBeat> beats = session.schedule().beats();
            for (int b = seen; b < beats.size(); b++) {
                assertTrue(beats.get(b).start() >= now - 1e-6,
                        "beat " + b + " starts at " + beats.get(b).start()
                                + " before its own command at " + now);
            }
            seen = beats.size();
        }
    }

    // -- the performance survives the rescore ---------------------------------------

    /**
     * {@code Director.rescore} continues the same performance: a run that
     * rescored mid-flight lands every body exactly where the run that never
     * rescored does.
     *
     * <p>This is the teleport guard for the whole live loop -- the state the
     * schedule does not carry (clock, carries, origins, hilts) must survive the
     * handover. Observed red by dropping the pelvis-carry copy from
     * {@code rescore}: the two runs then differ by the carry snapping.
     */
    @Test
    void rescoringMidPerformanceMovesNoBody() {
        double[] plain = run(false);
        double[] rescored = run(true);
        assertEquals(plain.length, rescored.length);
        for (int i = 0; i < plain.length; i++) {
            assertEquals(plain[i], rescored[i], 1e-9,
                    "body " + i + " moved across a no-op rescore");
        }
    }

    private static double[] run(boolean rescoreMidway) {
        Session session = new Session(Plays.victory());
        List<Figure> figures = new ArrayList<>();
        for (Bout.Staged.Body b : session.bodies()) {
            if (!b.hero()) {
                figures.add(Figure.headlessPale(b.id()).standAt(
                        Director.stretch(b.tile() * dev.starfall.stage.Stage.TILE_WIDTH),
                        b.facing().step()));
            }
        }
        for (Bout.Staged.Body b : session.bodies()) {
            if (b.hero()) {
                figures.add(Figure.headlessDark(b.id()).standAt(
                        Director.stretch(b.tile() * dev.starfall.stage.Stage.TILE_WIDTH),
                        b.facing().step()));
            }
        }
        Director director = new Director(session.schedule(), figures);
        director.start();
        Plays.Pilot pilot = Plays.duellist();
        float dt = 1f / 60f;
        for (int frame = 0; frame < 600; frame++) {
            double now = director.time();
            if (!session.outcome().over() && session.quietAt(now)) {
                Command cmd = pilot.decide(session.engine());
                if (cmd != null && session.command(cmd, now)) {
                    director = director.rescore(session.schedule());
                }
            }
            if (rescoreMidway && frame == 300) {
                director = director.rescore(session.schedule());
            }
            director.advance(dt);
        }
        double[] out = new double[figures.size()];
        for (int i = 0; i < figures.size(); i++) {
            out[i] = figures.get(i).standX();
        }
        return out;
    }

    // -- reproducibility --------------------------------------------------------------

    /**
     * Two identical played fights are one fight: same commands at the same
     * seconds give the same beats at the same seconds and the same directives.
     * This is what makes a pilot's capture evidence rather than anecdote.
     */
    @Test
    void aPlayedFightIsReproducible() {
        Session a = new Session(Plays.defeat());
        Session b = new Session(Plays.defeat());
        play(a, Plays.bystander(), 300.0);
        play(b, Plays.bystander(), 300.0);
        assertEquals(a.outcome(), b.outcome());
        List<ScheduledBeat> ba = a.schedule().beats();
        List<ScheduledBeat> bb = b.schedule().beats();
        assertEquals(ba.size(), bb.size(), "same beat count");
        for (int i = 0; i < ba.size(); i++) {
            assertEquals(ba.get(i).start(), bb.get(i).start(), 1e-12, "beat " + i + " start");
            assertEquals(ba.get(i).end(), bb.get(i).end(), 1e-12, "beat " + i + " end");
        }
        assertEquals(a.wounds().size(), b.wounds().size());
        assertNotNull(a.readout().at(1.0));
    }
}
