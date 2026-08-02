package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lane length as a design parameter rather than a constant (combat-design.md
 * 1.6).
 *
 * <p>The document calls this the single strongest composition dial in the game,
 * and the claim it makes is falsifiable: 5-7 is a knife fight where everything is
 * already in reach, 13-15 is an approach with several turns before contact. So
 * the interesting test is not that {@code new Lane(4)} throws -- it is the
 * <em>sweep</em>: hold the encounter fixed, vary only the length, and measure how
 * many turns pass before the first blade lands. If that number did not climb with
 * length, the dial would not exist.
 */
class LaneTest {

    @Test
    void theLaneRunsFromFiveToFifteenAndRefusesAnythingElse() {
        for (int n = Lane.MIN_LENGTH; n <= Lane.MAX_LENGTH; n++) {
            assertEquals(n, new Lane(n).length());
        }
        assertThrows(IllegalArgumentException.class, () -> new Lane(4));
        assertThrows(IllegalArgumentException.class, () -> new Lane(16));
        assertThrows(IllegalArgumentException.class, () -> new Lane(0));
    }

    @Test
    void theEnemyCapScalesWithLengthAndNeverShrinks() {
        // "A 15-tile lane with three enemies is a corridor; a 5-tile lane with
        // three enemies is a crisis." Half the lane, floor, minimum two -- which
        // keeps board density roughly constant so that density stays a design
        // choice rather than an accident of length.
        int previous = 0;
        for (int n = Lane.MIN_LENGTH; n <= Lane.MAX_LENGTH; n++) {
            int cap = new Lane(n).maxSimultaneousEnemies();
            assertTrue(cap >= previous, "the cap must be monotone in length, broke at " + n);
            assertTrue(cap >= 2, "even a knife fight has room for two");
            assertTrue(cap <= n - 1, "and never so many that the hero has nowhere to stand");
            previous = cap;
        }
        assertEquals(2, new Lane(5).maxSimultaneousEnemies());
        assertEquals(7, new Lane(15).maxSimultaneousEnemies());
    }

    @Test
    void anEncounterThatOverfillsItsLaneIsRefusedAtConstruction() {
        EncounterSpec.Builder b = EncounterSpec.builder(5, Hero.WARDEN).heroAt(0, Facing.RIGHT);
        b.enemy(EnemyArchetype.WISP, 2, Facing.LEFT);
        b.enemy(EnemyArchetype.WISP, 3, Facing.LEFT);
        b.enemy(EnemyArchetype.WISP, 4, Facing.LEFT);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, b::build);
        assertTrue(thrown.getMessage().contains("at most 2"));
    }

    @Test
    void twoBodiesCannotStartOnOneTileAndNobodyCanStartOffTheLane() {
        assertThrows(IllegalArgumentException.class, () -> EncounterSpec.builder(7, Hero.WARDEN)
                .heroAt(3, Facing.RIGHT).enemy(EnemyArchetype.WISP, 3, Facing.LEFT).build());
        assertThrows(IllegalArgumentException.class, () -> EncounterSpec.builder(7, Hero.WARDEN)
                .heroAt(3, Facing.RIGHT).enemy(EnemyArchetype.WISP, 9, Facing.LEFT).build());
        assertThrows(IllegalArgumentException.class, () -> EncounterSpec.builder(7, Hero.WARDEN)
                .heroAt(-1, Facing.RIGHT).enemy(EnemyArchetype.WISP, 3, Facing.LEFT).build());
    }

    @Test
    void lengthBuysTheApproachAndTheApproachIsMeasurable() {
        // Same fixture at every legal length: hero at one end, a Wisp at the other,
        // hero holding. Count the turns before the first blade lands.
        List<Integer> turnsToContact = new ArrayList<>();
        for (int n = Lane.MIN_LENGTH; n <= Lane.MAX_LENGTH; n++) {
            CombatEngine e = CombatEngine.create(EncounterSpec.builder(n, Hero.WARDEN)
                    .heroAt(0, Facing.RIGHT)
                    .heroHp(100)
                    .loadout(Tile.of(TileType.CUT))
                    .enemy(EnemyArchetype.WISP, n - 1, Facing.LEFT)
                    .build());
            int turns = 0;
            while (turns < 40 && !Encounters.has(e.apply(Command.hold()).events(), CombatEvent.Hit.class)) {
                turns++;
            }
            turnsToContact.add(turns);
        }

        // Measured: [3, 4, 5, ... 13]. Three turns of closing on the short lane
        // against thirteen on the long one -- the "approach" is not a metaphor, it
        // is a factor of four in how long the player has before anything touches
        // them, off one integer in the encounter spec.
        int shortest = turnsToContact.get(0);
        int longest = turnsToContact.get(turnsToContact.size() - 1);
        assertTrue(shortest <= 3,
                "a five-tile lane is a knife fight: everything is already nearly in reach, got " + turnsToContact);
        assertTrue(longest >= 4 * shortest,
                "a fifteen-tile lane must buy a genuinely longer approach, got " + turnsToContact);
        for (int i = 1; i < turnsToContact.size(); i++) {
            assertTrue(turnsToContact.get(i) > turnsToContact.get(i - 1),
                    "each extra tile must buy exactly one more turn of approach: " + turnsToContact);
        }
    }

    @Test
    void aChargerFlattensThatCurveEntirelyWhichIsWhatTheTraitIsFor() {
        // combat-design.md 1.6: "on a long lane a melee-only enemy can spend several
        // turns simply walking, so waves need mixed approach speeds or the board
        // reads as empty. The Charger trait exists partly to solve this." Measured
        // against the sweep above: a Runner reaches the hero in the same two turns
        // whether the lane is five tiles or fifteen.
        List<Integer> turnsToContact = new ArrayList<>();
        for (int n : new int[]{5, 9, 15}) {
            CombatEngine e = CombatEngine.create(EncounterSpec.builder(n, Hero.WARDEN)
                    .heroAt(0, Facing.RIGHT)
                    .heroHp(100)
                    .loadout(Tile.of(TileType.CUT))
                    .enemy(EnemyArchetype.RUNNER, n - 1, Facing.LEFT)
                    .build());
            int turns = 0;
            while (turns < 40 && !Encounters.has(e.apply(Command.hold()).events(), CombatEvent.Hit.class)) {
                turns++;
            }
            turnsToContact.add(turns);
        }
        assertEquals(1, turnsToContact.stream().distinct().count(),
                "the Charger must not care how long the lane is: " + turnsToContact);
    }
}
