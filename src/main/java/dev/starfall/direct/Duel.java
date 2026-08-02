package dev.starfall.direct;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.Combatant;
import dev.starfall.combat.Command;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.Scheduler;
import dev.starfall.stage.Stage;
import dev.starfall.stage.Standing;

import java.util.ArrayList;
import java.util.List;

/**
 * The three duels, built by a real {@link CombatEngine} and staged by a real
 * {@link Scheduler}.
 *
 * <p><b>Nothing here authors a beat.</b> Every schedule below is the event stream
 * of an encounter the rules engine actually resolved, mapped to seconds by the
 * staging layer. That is the point of the exercise: the systems underneath have
 * been built and tested and none of them has ever run with any of the others, so a
 * scene that hand-wrote its own directives would prove nothing at all about
 * whether they join up.
 *
 * <h2>Why the banking turns are played but not staged</h2>
 *
 * <p>Filling a five-slot Ink Stanza costs five turns, and each of them lets a
 * Charted Shadow act. A schedule containing all of that is a correct schedule and
 * about twenty seconds long, of which the phrase under test is the last three --
 * so the capture would be mostly a figure standing still, and STYLE.md 11.2 is
 * explicit that a capture has to be aimed at "the beat that matters".
 *
 * <p>So the engine plays the whole encounter and the scheduler is fed the opening
 * events and the execution, with the board as it stands at the moment of
 * execution. Both halves are real; only the waiting is left out.
 */
public final class Duel {

    /** Which of the three. */
    public enum Kind {

        /**
         * A five-tile stanza against a body at arm's length: the multi-tile phrase
         * the Ink Stanza was designed to produce.
         *
         * <p>combat-design.md 1.1a bought the five slots with an argument about
         * exactly this shot -- "five linked beats resolving without interruption,
         * each one flowing out of the last, is a qualitatively different thing to
         * animate than three" -- and {@code Overlap} calls itself "the field that
         * decides whether five queued tiles read as one phrase or as five separate
         * events". This is where that is answered in pixels rather than in a
         * schedule.
         *
         * <p>The Bulwark is the opponent because it is Unyielding and Aggressive:
         * it stands its ground for the whole phrase instead of being knocked out of
         * frame, and the third clause -- a Step into the tile it occupies -- becomes
         * a shove it refuses, which is combat-design.md's "a body thrown against
         * something rooted" and gives the phrase a beat that is not another cut.
         * Four damage against five hit points, so nothing dies mid-phrase and the
         * capture is about the phrase rather than about a dissolve.
         */
        PHRASE("duel-phrase", "five-tile stanza against a rooted body: one phrase, five clauses"),

        /**
         * The signature beat. Two skeletons agreeing on one point in space.
         *
         * <p>The hero banks a Parry and the Bulwark strikes into it, which the
         * engine resolves as a {@code BladeMet} carrying a {@code Meeting} that
         * names the crossing twice -- once in each body's own vocabulary -- and
         * carries the instant both bodies synchronise to. STYLE.md 7.2: "a
         * deflection curve, not a collision. The blades should slide and redirect
         * along a smooth arc; the defender's arm gives ground on an IK curve rather
         * than stopping dead."
         */
        PARRY("duel-parry", "blade meets blade: one contact instant, two skeletons, a deflection"),

        /**
         * A shove, and the body carried backward by it.
         *
         * <p>STYLE.md 7.2: "knockback is a drift, not a launch. A struck figure
         * should be carried backward like a sheet of silk caught in wind, arriving
         * over ~0.8 s, cloth and hair streaming ahead of the body's arrival" -- with
         * the correction that "streaming ahead" is a displacement and not an arrival
         * time. The Wisp is the opponent because it is the one archetype light
         * enough to give ground rather than root.
         */
        KNOCKBACK("duel-knockback", "a shove, and a body carried: cloth and hair thrown along the travel");

        private final String scene;
        private final String description;

        Kind(String scene, String description) {
            this.scene = scene;
            this.description = description;
        }

        public String sceneName() {
            return scene;
        }

        public String description() {
            return description;
        }
    }

    /** A staged duel: the score, and who is standing where when it starts. */
    public record Staged(Schedule schedule, int hero, int enemy, int heroTile, int enemyTile,
                         Facing heroFacing, Facing enemyFacing) {
    }

    private Duel() {
    }

    public static Staged of(Kind kind) {
        return switch (kind) {
            case PHRASE -> phrase();
            case PARRY -> parry();
            case KNOCKBACK -> knockback();
        };
    }

    private static Staged phrase() {
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                // Filling five slots costs five turns and the Bulwark is Aggressive,
                // so it strikes on every one of them: at the default eight hit
                // points the hero is dead before the stanza it queued can be played.
                // The wait is not part of the shot -- see the class note on why the
                // banking turns are played and not staged -- so the hit points only
                // have to outlast it.
                .heroHp(40)
                .enemy(EnemyArchetype.BULWARK, 1, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.THRUST), Tile.of(TileType.STEP),
                        Tile.of(TileType.SWEEP), Tile.of(TileType.CUT).withCooldown(0))
                .build());
        for (int i = 0; i < 5; i++) {
            Command add = Command.add(i);
            if (engine.can(add)) {
                engine.apply(add);
            }
        }
        return stage(engine, Command.execute());
    }

    private static Staged parry() {
        // ContactTest's own fixture, deliberately: the parry is proved to come out
        // of this board, so the scene cannot be showing a beat the rules do not
        // actually produce.
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(5, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.BULWARK, 1, Facing.LEFT)
                .loadout(Tile.of(TileType.PARRY))
                .build());
        engine.apply(Command.add(0));
        return stage(engine, Command.execute());
    }

    private static Staged knockback() {
        // Also ContactTest's, for the same reason: hero at 2, Wisp at 4, one Step.
        // The step closes to arm's length and the shove carries the Wisp on to 5.
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(Tile.of(TileType.STEP))
                .build());
        engine.apply(Command.add(0));
        return stage(engine, Command.execute());
    }

    /**
     * Reads the board as it stands, then schedules the opening and one command.
     *
     * <p>The opening board has to be handed in because the event stream does not
     * carry it -- {@code Standing}'s own note reports that as a genuine gap:
     * "{@code EncounterBegan} names the lane length, the hero and the ids of the
     * Charted Shadows, and does not say where any of them is standing or which way
     * it faces... a {@code List<Standing.Body>} on {@code EncounterBegan} would
     * close it and would keep the stream entirely ordinal." This method is the
     * fourth place in the project to work around it.
     */
    private static Staged stage(CombatEngine engine, Command command) {
        List<Standing.Body> bodies = new ArrayList<>();
        Combatant hero = engine.state().hero();
        Combatant enemy = null;
        for (Combatant c : engine.state().all()) {
            bodies.add(new Standing.Body(c.id(), c.tile(), c.facing()));
            if (c.id() != hero.id() && enemy == null) {
                enemy = c;
            }
        }
        Standing opening = Standing.opening(bodies);
        Scheduler scheduler = new Scheduler(new Stage(engine.state().lane().length()), opening);
        scheduler.accept(engine.opening());
        scheduler.accept(engine.apply(command));
        Combatant e = enemy == null ? hero : enemy;
        return new Staged(scheduler.schedule(), hero.id(), e.id(), hero.tile(), e.tile(),
                hero.facing(), e.facing());
    }

    /**
     * The two figures, coloured and placed.
     *
     * <p>Reference images 3, 4 and 5 are a dark duellist against a pale one, and
     * STYLE.md 2.1 carries {@code CLOTH_PALE} for exactly that -- "the white-kimono
     * duellist; cool grey, not white". The hero is the dark one because it is the
     * subject and because STYLE.md 2.2 wants the value extremes spent on what
     * matters.
     *
     * <p>Order is the draw order: the pale figure first, so the dark one sits over
     * it where they cross. That is not arbitrary either -- the dark figure carries
     * the frame's darkest values and its silhouette is the strongest mark in the
     * picture, so it is the one that must not be interrupted.
     */
    public static List<Figure> figuresFor(Staged staged) {
        Figure enemy = Figure.pale(staged.enemy())
                .standAt(Director.stretch(staged.enemyTile() * Stage.TILE_WIDTH),
                        staged.enemyFacing().step());
        Figure hero = Figure.dark(staged.hero())
                .standAt(Director.stretch(staged.heroTile() * Stage.TILE_WIDTH),
                        staged.heroFacing().step());
        return List.of(enemy, hero);
    }
}
