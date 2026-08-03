package dev.starfall.ui;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.Combatant;
import dev.starfall.combat.Command;
import dev.starfall.combat.Enchantment;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Resolution;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.ScheduledBeat;
import dev.starfall.stage.Scheduler;
import dev.starfall.stage.Stage;
import dev.starfall.stage.Standing;

import java.util.ArrayList;
import java.util.List;

/**
 * The encounters the interface is shot against: a real fight, played in three
 * acts, so that every state STYLE.md 9 and combat-design.md 3 describe happens
 * inside one continuous run.
 *
 * <h2>Why one scene and three acts rather than three scenes</h2>
 *
 * <p>The brief for this layer asks for the planning state, the execution state, a
 * full stanza, a nearly-empty one, and <b>the transition between planning and
 * striking</b>. The last of those cannot be shot as a separate scene without
 * begging the question: a capture of a push-in that starts at its own t = 0 has
 * assumed the thing it is supposed to show. So there is one score --
 *
 * <ol>
 *   <li><b>Act I, the plan.</b> A full five-tile Ink Stanza, the camera at the
 *       planning framing derived from lane length, Strikethroughs standing over
 *       the tiles they threaten, and {@link Scheduler#pause} holding it there long
 *       enough that the shot is a held wide and not a pause between two glides.</li>
 *   <li><b>Act II, the strike.</b> The execution: the push-in, five clauses
 *       resolving without interruption, the column draining from the top, and the
 *       enemy phase answering it.</li>
 *   <li><b>Act III, the next plan.</b> The return to the wide framing, and one
 *       tile banked -- a nearly-empty stanza in the planning framing, which is the
 *       state the fight spends most of its time in and the one an interface is
 *       easiest to get wrong.</li>
 * </ol>
 *
 * <p>The banking turns of act I are played but not staged, exactly as
 * {@code Duel} does and for the reason recorded there: filling five slots costs
 * five turns and a capture of them is mostly a figure standing still.
 *
 * <h2>Three lanes, because lane length is the composition dial</h2>
 *
 * <p>combat-design.md 1.6 makes lane length "the single strongest composition dial
 * in the game" and STYLE.md 9 makes the push-in scale with it. Neither claim is
 * checkable from one lane, so the same three acts are cut for 5, 11 and 15 tiles
 * and the camera arithmetic is left to derive the rest: {@code Stage.pushIn()}
 * gives 2.0x, 3.6x and 5.2x and {@code Stage.pushInSeconds()} 0.50 s, 0.68 s and
 * 0.80 s without any of them being authored here.
 */
public final class Bout {

    /** How long the plan is held before the phrase, in seconds. */
    public static final double PLANNING_SECONDS = 3.0;

    /** And after it, before the next tile is banked. */
    public static final double REPLAN_SECONDS = 2.6;

    /** Which fight. */
    public enum Kind {

        /**
         * combat-design.md 1.6's knife fight: "everything is already in reach, so
         * tactics are about ordering". The whole lane is barely wider than the
         * intimate shot, so the push-in is the smallest the game ever makes.
         */
        KNIFE("lane-knife", 5, "a five-tile knife fight: the smallest push-in the lane law allows"),

        /**
         * The middle game, and the scene the interface is graded on. Long enough
         * that the planning framing is genuinely wide and the column has a sheet to
         * be written on.
         */
        FOLD("lane-fold", 11, "eleven tiles: plan, strike, and plan again -- the interface in one score"),

        /**
         * combat-design.md 1.6's approach: "several turns of closing before
         * contact... a long lane has nothing but the approach". The largest
         * push-in, and the test of whether a column survives a 5.2x zoom.
         */
        APPROACH("lane-approach", 15, "fifteen tiles: the approach, and the largest push-in in the game");

        private final String scene;
        private final int laneLength;
        private final String description;

        Kind(String scene, int laneLength, String description) {
            this.scene = scene;
            this.laneLength = laneLength;
            this.description = description;
        }

        public String sceneName() {
            return scene;
        }

        public int laneLength() {
            return laneLength;
        }

        public String description() {
            return description;
        }
    }

    /** A staged bout: the score, what the sheet says, and who opens where. */
    public record Staged(Schedule schedule, Readout readout, int hero, int heroTile, Facing heroFacing,
                         List<Body> bodies) {

        /** One body placed for the renderer, in the order it should be drawn. */
        public record Body(int id, int tile, Facing facing, boolean hero) {
        }
    }

    private Bout() {
    }

    /**
     * The hand each bout is cut for, and why it is seven tiles rather than five.
     *
     * <h2>A full stanza and a hand that still has something in it</h2>
     *
     * <p>Pass 1 gave every bout exactly {@link dev.starfall.combat.InkStanza#CAPACITY}
     * tiles, so filling the stanza emptied the hand, and the review measured the
     * consequence on the one shot the interface is graded on: the hand column read at
     * <b>1.016x its own bare control</b> through {@code x790..919 y30..359} -- the
     * right third of the graded frame doing nothing at all. That is correct behaviour
     * and a bad picture, and the fix is a hand rather than a layout: two tiles beyond
     * the stanza's capacity stay on the sheet with their charges on them, so the
     * planning shot shows a full commitment <em>and</em> what is still possible,
     * which is the state a player is actually reading.
     *
     * <p>The first five are the same five in the same order on every lane, because
     * they are the written stanza and the pass-1 review read them cold off the
     * delivered frames; nothing here is allowed to disturb that. The two extras
     * differ by lane, and between the three bouts they cover <b>Back-step, Feint,
     * Draw and Turn</b> -- the four letters of the alphabet the review found had
     * never been through the renderer -- plus one enchanted tile, so the enchantment
     * mark is drawn somewhere for the first time as well.
     */
    private static List<Tile> hand(Kind kind) {
        List<Tile> out = new ArrayList<>(List.of(
                Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.SWEEP),
                Tile.of(TileType.THRUST), Tile.of(TileType.PARRY)));
        switch (kind) {
            case FOLD -> {
                out.add(Tile.of(TileType.BACK_STEP));
                out.add(Tile.of(TileType.FEINT, Enchantment.MARKING));
            }
            case KNIFE -> {
                out.add(Tile.of(TileType.DRAW));
                out.add(Tile.of(TileType.TURN));
            }
            case APPROACH -> {
                out.add(Tile.of(TileType.TURN));
                out.add(Tile.of(TileType.DRAW, Enchantment.SEEPING));
            }
            default -> throw new IllegalStateException(kind.toString());
        }
        return List.copyOf(out);
    }

    public static Staged of(Kind kind) {
        int n = kind.laneLength();
        // Placed so the fight converges near the middle of the Fold rather than at
        // one end of it. Five banking turns and up to four holds are played before
        // the score opens, and a Charted Shadow closes about a tile a turn, so a
        // wave that starts against the far edge has walked most of the way in by the
        // time the planning shot is taken -- which puts every body in the first
        // third of a frame that holds the whole lane.
        int heroTile = Math.max(1, n / 4);
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(n, Hero.WARDEN)
                .heroAt(heroTile, Facing.RIGHT)
                // Enough to outlast the five banking turns, which are played and not
                // shown. Ten rather than the default eight so the health marks have
                // both live strokes and dry ghosts to say something with.
                .heroHp(10)
                .enemy(EnemyArchetype.REACHER, Math.min(n - 2, n / 2 + 1), Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, n - 1, Facing.LEFT)
                .loadout(hand(kind))
                .build());

        Stage stage = Stage.of(engine.state().lane());
        Readout.Builder readout = Readout.builder(stage);

        // Act I: write the stanza. Played, not staged.
        for (int i = 0; i < 5; i++) {
            Command add = Command.add(i);
            if (engine.can(add)) {
                Tile tile = engine.state().loadout().tile(i);
                engine.apply(add);
                // Before the score opens, so the opening stanza is settled ink
                // rather than five marks all arriving on frame zero. The banking
                // turns are played and not staged -- see the class note.
                readout.bank(-Readout.WET_SECONDS, tile);
            }
        }

        // Wait for a Strikethrough before committing the phrase.
        //
        // Not a contrivance for the capture: it is the play combat-design.md 3d.1
        // argues for -- "one hold re-phases and the same cadence connects", and
        // inspiration.md names tactical patience as a dominant strategy. It is also
        // the only way the planning shot is guaranteed to contain the thing the
        // planning shot exists to show. Bounded, because the engine's own measured
        // guarantee is first contact within distance + 4 turns.
        for (int i = 0; i < 4 && engine.state().threatenedTiles().isEmpty(); i++) {
            engine.apply(Command.hold());
        }

        // The board the score is cut against, read before any of it is staged.
        List<Staged.Body> bodies = new ArrayList<>();
        for (Combatant c : engine.state().all()) {
            bodies.add(new Staged.Body(c.id(), c.tile(), c.facing(), c.isHero()));
        }
        int heroId = engine.state().hero().id();
        int heroStands = engine.state().hero().tile();
        Facing heroFacing = engine.state().hero().facing();

        Scheduler scheduler = new Scheduler(stage, opening(bodies));
        scheduler.accept(engine.opening());
        readout.board(0.0, engine.state(), Readout.footprint(engine.previewExecution()));

        // Act II: the strike.
        scheduler.pause(PLANNING_SECONDS);
        int before = scheduler.beats().size();
        Resolution phrase = engine.apply(Command.execute());
        scheduler.accept(phrase);
        List<ScheduledBeat> clauses = new ArrayList<>();
        for (ScheduledBeat b : scheduler.beats().subList(before, scheduler.beats().size())) {
            if (b.bracket() == ScheduledBeat.Bracket.STANZA) {
                clauses.add(b);
            }
        }
        readout.resolve(clauses);
        // The board updates when the phrase is over, not when the turn is: the
        // footprint of a phrase that is happening is still worth showing, and the
        // enemy phase that answers it belongs to the next board.
        double phraseEnd = clauses.isEmpty() ? endOf(scheduler, before)
                : clauses.get(clauses.size() - 1).end();
        readout.board(phraseEnd, engine.state(), List.of());

        // Act III: plan again, with one tile written.
        scheduler.pause(REPLAN_SECONDS);
        int mark = scheduler.beats().size();
        int next = firstReady(engine);
        if (next >= 0) {
            Tile tile = engine.state().loadout().tile(next);
            Resolution written = engine.apply(Command.add(next));
            scheduler.accept(written);
            readout.bank(startOf(scheduler, mark), tile);
            readout.board(startOf(scheduler, mark), engine.state(),
                    Readout.footprint(engine.previewExecution()));
        }
        // And then the player thinks. Without this the score ends on the return
        // itself and the nearly-empty planning shot exists for one instant; with it
        // the wide framing is held and drifting with one mark on the sheet, which is
        // the state a fight spends most of its time in.
        scheduler.pause(REPLAN_SECONDS);
        int last = scheduler.beats().size();
        scheduler.accept(engine.apply(Command.hold()));
        readout.board(startOf(scheduler, last), engine.state(),
                Readout.footprint(engine.previewExecution()));

        Schedule schedule = scheduler.schedule();
        return new Staged(schedule, readout.build(schedule), heroId, heroStands, heroFacing,
                List.copyOf(bodies));
    }

    /**
     * The board as it stands when the score opens.
     *
     * <p>Read off the engine rather than off {@code EncounterBegan}, because the
     * stream does not carry an opening position -- {@code Standing}'s own note
     * calls that a genuine gap and this is the fifth place in the project to work
     * around it.
     */
    private static Standing opening(List<Staged.Body> bodies) {
        List<Standing.Body> out = new ArrayList<>();
        for (Staged.Body b : bodies) {
            out.add(new Standing.Body(b.id(), b.tile(), b.facing()));
        }
        return Standing.opening(out);
    }

    private static int firstReady(CombatEngine engine) {
        for (int i = 0; i < engine.state().loadout().size(); i++) {
            if (engine.can(Command.add(i))) {
                return i;
            }
        }
        return -1;
    }

    private static double startOf(Scheduler scheduler, int from) {
        List<ScheduledBeat> beats = scheduler.beats();
        return from < beats.size() ? beats.get(from).start() : endOf(scheduler, from);
    }

    private static double endOf(Scheduler scheduler, int from) {
        double end = 0.0;
        List<ScheduledBeat> beats = scheduler.beats();
        for (int i = from; i < beats.size(); i++) {
            end = Math.max(end, beats.get(i).end());
        }
        return end;
    }
}
