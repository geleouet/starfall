package dev.starfall.combat;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fixtures and readers shared by the combat tests.
 *
 * <p>Every fixture here is written out longhand rather than generated, because a
 * rules test whose board you cannot read off the first three lines is a test that
 * will be deleted the first time it fails. The one generated fixture,
 * {@link EncounterSpec#wave}, is exercised only where its determinism is the
 * subject.
 */
final class Encounters {

    private Encounters() {
    }

    /** A hero at tile 0 facing right, and whatever else the caller adds. */
    static EncounterSpec.Builder duel(int laneLength, Hero hero) {
        return EncounterSpec.builder(laneLength, hero).heroAt(0, Facing.RIGHT);
    }

    /**
     * Hero at 0, one Bulwark at 1. The most useful fixture in the file: the
     * Bulwark is Aggressive and stands at reach 1, so the tile it would close into
     * is the one the hero occupies and it therefore owes no step after striking.
     * It attacks every single turn, where everything else runs the three-beat
     * advance / strike / withdraw cycle of combat-design.md 3d.1 -- which makes it
     * the simplest enemy to write a multi-turn assertion against. It is no longer
     * the <em>only</em> one: the other cycle is three beats rather than two, so it
     * no longer phase-locks with the hero's.
     */
    static CombatEngine againstBulwark(Hero hero, Tile... loadout) {
        return CombatEngine.create(duel(5, hero)
                .enemy(EnemyArchetype.BULWARK, 1, Facing.LEFT)
                .loadout(loadout)
                .build());
    }

    /** A long lane with one distant Wisp: quiet enough to measure the turn economy against. */
    static CombatEngine quietLane(Hero hero, Tile... loadout) {
        return CombatEngine.create(duel(15, hero)
                .enemy(EnemyArchetype.WISP, 14, Facing.LEFT)
                .loadout(loadout)
                .build());
    }

    // -- reading the stream ----------------------------------------------------

    /**
     * The events between {@code PhraseBegan} and {@code PhraseEnded}, inclusive --
     * one uninterrupted execution, isolated from the enemy phase that follows it.
     * Most assertions about tiles want this rather than the whole turn.
     */
    static List<CombatEvent> phrase(Resolution r) {
        int from = -1;
        int to = -1;
        for (int i = 0; i < r.events().size(); i++) {
            CombatEvent e = r.events().get(i);
            if (e instanceof CombatEvent.PhraseBegan) {
                from = i;
            }
            if (e instanceof CombatEvent.PhraseEnded) {
                to = i;
            }
        }
        if (from < 0 || to < 0) {
            fail("no phrase in this resolution: " + r.events());
        }
        return r.events().subList(from, to + 1);
    }

    static <E extends CombatEvent> List<E> only(List<CombatEvent> events, Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    static <E extends CombatEvent> E firstOf(List<CombatEvent> events, Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    static boolean has(List<CombatEvent> events, Class<? extends CombatEvent> type) {
        return events.stream().anyMatch(type::isInstance);
    }

    /** Index of the first event of a type, or -1. Used to assert ordering within a turn. */
    static int indexOf(List<CombatEvent> events, Class<? extends CombatEvent> type) {
        for (int i = 0; i < events.size(); i++) {
            if (type.isInstance(events.get(i))) {
                return i;
            }
        }
        return -1;
    }

    static Combatant enemy(CombatEngine engine, EnemyArchetype archetype) {
        for (Combatant c : engine.state().all()) {
            if (c.archetype() == archetype) {
                return c;
            }
        }
        return null;
    }

    static List<CombatEvent> run(CombatEngine engine, Command... script) {
        List<CombatEvent> out = new ArrayList<>();
        for (Command c : script) {
            out.addAll(engine.apply(c).events());
        }
        return out;
    }

    // -- invariants ------------------------------------------------------------

    /**
     * The one rule the whole board rests on: a tile holds at most one body. Enemy
     * ordering, pushes and swaps are all only correct if this survives them, so it
     * is checked after every step of the sweep tests rather than at the end.
     */
    static void assertNoTileShared(CombatState state) {
        for (int t = 0; t < state.lane().length(); t++) {
            int here = 0;
            for (Combatant c : state.all()) {
                if (c.alive() && c.tile() == t) {
                    here++;
                }
            }
            assertTrue(here <= 1, "tile " + t + " holds " + here + " bodies:\n" + state.fingerprint());
        }
        for (Combatant c : state.all()) {
            assertTrue(!c.alive() || state.lane().contains(c.tile()),
                    c + " is standing off the Fold of the World");
        }
    }
}
