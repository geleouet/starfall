package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.Combatant;
import dev.starfall.combat.Command;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Boards and readers shared by the staging tests.
 *
 * <p><b>The opening board is built here on purpose.</b> The scheduler never reads
 * {@code CombatState} -- {@link Standing}'s class note explains why -- but a test
 * may, because a test is standing outside both layers. So this is the one place in
 * the staging tests that touches the engine's state, and it touches it exactly
 * once, at turn zero, to supply the opening tiles and facings that
 * {@link CombatEvent.EncounterBegan} does not carry.
 */
final class Fixtures {

    /**
     * How long the player is assumed to sit looking at the board between two
     * commands. Long enough for STYLE.md 9's full return and full push-in to both
     * fit, which is what makes the camera assertions about a real shot rather than
     * about a compressed one.
     */
    static final double PLANNING = 1.6;

    private Fixtures() {
    }

    /** A hero at tile 0 facing right and one Charted Shadow at the far end. */
    static CombatEngine lane(int laneLength, Hero hero, EnemyArchetype archetype, int enemyTile,
                            Tile... loadout) {
        return CombatEngine.create(EncounterSpec.builder(laneLength, hero)
                .heroAt(0, Facing.RIGHT)
                .enemy(archetype, enemyTile, Facing.LEFT)
                .loadout(loadout)
                .build());
    }

    /** The opening board, in the only units the engine has: a tile and a facing each. */
    static Standing opening(CombatEngine engine) {
        List<Standing.Body> bodies = new ArrayList<>();
        for (Combatant c : engine.state().all()) {
            bodies.add(new Standing.Body(c.id(), c.tile(), c.facing()));
        }
        return Standing.opening(bodies);
    }

    /** A scheduler cut for this encounter, already fed the opening events. */
    static Scheduler scheduler(CombatEngine engine) {
        Scheduler s = new Scheduler(new Stage(engine.state().lane().length()), opening(engine));
        s.accept(engine.opening());
        return s;
    }

    /** Banks every tile in the hand, then executes, and schedules the whole thing. */
    static Schedule stanza(CombatEngine engine, int tiles) {
        Scheduler s = scheduler(engine);
        for (int i = 0; i < tiles; i++) {
            s.accept(engine.apply(Command.add(i)));
            s.pause(PLANNING);
        }
        s.accept(engine.apply(Command.execute()));
        return s.schedule();
    }

    /**
     * Plays a script, skipping anything the board has made illegal.
     *
     * <p>A long script is the point -- several of these tests need a fight busy
     * enough to reach every corner of the vocabulary -- and a long script will
     * outlive its encounter or find a tile still drying. Skipping is right rather
     * than lenient: what is being tested is the schedule built from whatever the
     * engine did emit, and the assertions say what that has to contain.
     */
    static List<CombatEvent> run(CombatEngine engine, Scheduler scheduler, Command... script) {
        List<CombatEvent> all = new ArrayList<>();
        for (Command c : script) {
            if (!engine.can(c)) {
                continue;
            }
            List<CombatEvent> produced = engine.apply(c).events();
            all.addAll(produced);
            scheduler.accept(produced);
            scheduler.pause(PLANNING);
        }
        return all;
    }

    static <E extends CombatEvent> List<E> only(List<CombatEvent> events, Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
