package dev.starfall.ui;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.Combatant;
import dev.starfall.combat.Command;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;

import java.util.List;

/**
 * The played encounters and the two scripted players.
 *
 * <h2>Why scripted players exist beside the keyboard</h2>
 *
 * <p>The capture harness cannot press a key, and MEASUREMENT.md 11.2 requires the
 * evidence for any claim to come off frames a command line can reproduce. So the
 * two claims this pass must prove on pixels -- <b>a fight can be won</b> and
 * <b>a fight can be lost</b> -- are played by {@link Pilot}s: deterministic
 * players that issue {@link Command}s through the same {@link Session} the
 * keyboard does, deciding from the same board a human would. They are players,
 * not scripts of instants: nothing here knows a second, only the board.
 */
public final class Plays {

    /** A player: reads the board, returns a command, or null to wait. */
    public interface Pilot {
        Command decide(CombatEngine engine);
    }

    private Plays() {
    }

    // -- encounters --------------------------------------------------------------

    /**
     * The interactive encounter for a lane: <b>identical to the staged bout's</b>,
     * so the fight a person plays is the fight the interface was graded on.
     */
    public static EncounterSpec bout(Bout.Kind kind) {
        int n = kind.laneLength();
        return EncounterSpec.builder(n, Hero.WARDEN)
                .heroAt(Math.max(1, n / 4), Facing.RIGHT)
                .heroHp(10)
                .enemy(EnemyArchetype.REACHER, Math.min(n - 2, n / 2 + 1), Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, n - 1, Facing.LEFT)
                .loadout(Bout.hand(kind))
                .build();
    }

    /**
     * The victory exhibit: one Wisp on a nine-tile Fold, and a hand that can
     * kill it. Small on purpose -- the claim is that victory is <em>reachable
     * through the input loop</em>, not that the tuning is right.
     */
    public static EncounterSpec victory() {
        return EncounterSpec.builder(9, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .heroHp(6)
                .enemy(EnemyArchetype.WISP, 7, Facing.LEFT)
                .loadout(List.of(
                        Tile.of(TileType.CUT), Tile.of(TileType.CUT), Tile.of(TileType.CUT),
                        Tile.of(TileType.PARRY), Tile.of(TileType.STEP)))
                .build();
    }

    /**
     * The defeat exhibit: a Bulwark and a Pilgrim who never writes a mark.
     * Three strokes of ink, and the Bulwark strikes every turn once it is in
     * reach -- so a player who only holds <b>loses</b>, on telegraphed blows
     * they watched arrive. That reachability is the whole point: a game nobody
     * can lose is not a game, and no scene in the project could lose one until
     * this.
     */
    public static EncounterSpec defeat() {
        return EncounterSpec.builder(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .heroHp(3)
                .enemy(EnemyArchetype.BULWARK, 5, Facing.LEFT)
                .loadout(List.of(
                        Tile.of(TileType.CUT), Tile.of(TileType.PARRY), Tile.of(TileType.STEP)))
                .build();
    }

    // -- pilots -------------------------------------------------------------------

    /**
     * The duellist: banks Cuts during the approach -- combat-design.md 1.1a's own
     * play, "bank the whole phrase during the approach, then spend it in one
     * exchange" -- and executes when the nearest Shadow is in reach of the
     * written phrase.
     */
    public static Pilot duellist() {
        return engine -> {
            var s = engine.state();
            Combatant hero = s.hero();
            Combatant near = nearest(s.enemies(), hero.tile());
            if (near == null) {
                return null;
            }
            int d = Math.abs(near.tile() - hero.tile());
            int written = s.stanza().size();
            if (written > 0 && d == 1 && engine.can(Command.execute())
                    && (written >= near.hp() || s.stanza().isFull())) {
                return Command.execute();
            }
            if (d > 1 && written < 3) {
                for (int i = 0; i < s.loadout().size(); i++) {
                    if (s.loadout().tile(i).type() == TileType.CUT && engine.can(Command.add(i))) {
                        return Command.add(i);
                    }
                }
            }
            return Command.hold();
        };
    }

    /**
     * The player who only watches. Every blow that kills them was telegraphed a
     * whole turn ahead; holding through all of them is a legal way to play and
     * the honest way to reach the defeat the design has never staged.
     */
    public static Pilot bystander() {
        return engine -> Command.hold();
    }

    private static Combatant nearest(List<Combatant> enemies, int heroTile) {
        Combatant best = null;
        int bestD = Integer.MAX_VALUE;
        for (Combatant c : enemies) {
            int d = Math.abs(c.tile() - heroTile);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }
}
