package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two verbs, and the thing that refuses them.
 *
 * <p>combat-design.md 2.1 calls these "the point of the design": push is a
 * collision with resistance and recoil, swap is an interpenetration with no
 * impact at all, and if both read as poetic the interaction layer is genuinely
 * general. So these tests are as much about the <em>event stream</em> as about
 * the board -- a verb that moved the right bodies but emitted no relational event
 * would have nothing for System 4 to choreograph, and would pass a
 * positions-only test.
 *
 * <p>The refusal cases matter at least as much as the successes. A shove that
 * moves nobody is invisible in the state and is one of the best beats in the
 * game.
 */
class MovementVerbTest {

    private static final Tile STEP = Tile.of(TileType.STEP);
    private static final Tile BACK_STEP = Tile.of(TileType.BACK_STEP);
    private static final Tile FEINT = Tile.of(TileType.FEINT);
    private static final Tile DRAW = Tile.of(TileType.DRAW);

    // -- push ------------------------------------------------------------------

    @Test
    void theWardenShovesTheOccupantBackAndTakesTheTile() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        assertEquals(3, wisp.tile(), "the Wisp closes to 3 while the tile is being banked");

        Resolution r = e.apply(Command.execute());
        List<CombatEvent> phrase = Encounters.phrase(r);

        CombatEvent.Shoved shoved = Encounters.firstOf(phrase, CombatEvent.Shoved.class);
        assertNotNull(shoved, "the contact must be in the stream, not just in the positions");
        assertTrue(shoved.gaveGround());
        assertEquals(3, shoved.fromTile());
        assertEquals(4, shoved.toTile());

        List<CombatEvent.Moved> moves = Encounters.only(phrase, CombatEvent.Moved.class);
        assertEquals(2, moves.size(), "two bodies move, and the animation layer needs both trajectories");
        assertEquals(CombatEvent.MoveReason.SHOVED, moves.get(0).reason());
        assertEquals(3, e.state().hero().tile());
    }

    @Test
    void aShoveWithTheLaneEndingBehindBracesAndHurtsBothBodies() {
        // The case combat-design.md names explicitly. Nobody moves; the beat is the
        // brace, and it is paid for on both sides.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(5, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        int wispHpBefore = wisp.hp();

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Collided collided = Encounters.firstOf(phrase, CombatEvent.Collided.class);
        assertNotNull(collided);
        assertEquals(CombatEvent.CollisionCause.LANE_EDGE, collided.cause());
        assertEquals(MovementVerb.COLLISION_DAMAGE, collided.damage());
        assertFalse(Encounters.firstOf(phrase, CombatEvent.Shoved.class).gaveGround());
        assertTrue(Encounters.only(phrase, CombatEvent.Moved.class).isEmpty(), "neither body gives ground");

        assertEquals(5, e.state().hero().tile());
        assertEquals(6, wisp.tile());
        assertEquals(wispHpBefore - MovementVerb.COLLISION_DAMAGE, wisp.hp());

        // The Warden pays too, which is what stops push being free tempo. Measured
        // inside the phrase rather than off the final hit points, because the Wisp
        // also gets its own turn afterwards and that would confound the reading.
        List<CombatEvent.Hit> impacts = Encounters.only(phrase, CombatEvent.Hit.class).stream()
                .filter(h -> h.source() == CombatEvent.HitSource.COLLISION).toList();
        assertEquals(2, impacts.size(), "both bodies take the impact");
        assertTrue(impacts.stream().anyMatch(h -> h.target() == e.state().hero().id()));
        assertTrue(impacts.stream().anyMatch(h -> h.target() == wisp.id()));
    }

    @Test
    void aShoveWithAnotherBodyBehindBracesTheSameWayAndDoesNotChain() {
        // combat-design.md only names the lane edge. Resolved here: an ally behind
        // is the same brace. One shove moves one body -- a column of enemies is a
        // wall, not a row of dominoes.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant front = e.state().at(3);
        Combatant back = e.state().at(4);
        e.apply(Command.add(0));
        int backHp = back.hp();

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Collided collided = Encounters.firstOf(phrase, CombatEvent.Collided.class);
        assertNotNull(collided);
        assertEquals(CombatEvent.CollisionCause.BODY_BEHIND, collided.cause());
        assertEquals(3, front.tile());
        assertEquals(4, back.tile());
        assertEquals(backHp, back.hp(), "the body behind is a wall, not a second casualty");
    }

    // -- swap ------------------------------------------------------------------

    @Test
    void thePilgrimExchangesPlacesAndEndsUpBehindTheBlade() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.PILGRIM)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        assertEquals(3, wisp.tile());

        Resolution r = e.apply(Command.execute());
        List<CombatEvent> phrase = Encounters.phrase(r);

        CombatEvent.Swapped swapped = Encounters.firstOf(phrase, CombatEvent.Swapped.class);
        assertNotNull(swapped);
        assertEquals(2, swapped.aFrom());
        assertEquals(3, swapped.bFrom());
        assertEquals(2, Encounters.only(phrase, CombatEvent.Moved.class).size());
        assertEquals(3, e.state().hero().tile());

        // And the payoff the verb is for: the Wisp's declared blade now points the
        // wrong way, so it swings at nothing. That is a whole turn bought with one
        // Step, and it is why swap and push are not the same tile with a skin.
        assertTrue(Encounters.has(r.events(), CombatEvent.IntentRetargeted.class));
        assertTrue(Encounters.has(r.events(), CombatEvent.Whiffed.class));
    }

    @Test
    void swapNeverRunsOutOfRoomEvenAtTheEndOfTheLane() {
        // Push has two failure modes and swap has one, because the destination is
        // vacated by definition. Worth pinning: it is the asymmetry that makes the
        // two heroes feel different at the edges of the board.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.PILGRIM)
                .heroAt(5, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertNotNull(Encounters.firstOf(phrase, CombatEvent.Swapped.class));
        assertTrue(Encounters.only(phrase, CombatEvent.Collided.class).isEmpty());
        assertEquals(6, e.state().hero().tile(), "the Pilgrim takes the last tile of the Fold");
        assertTrue(Encounters.only(phrase, CombatEvent.Moved.class).stream()
                        .anyMatch(m -> m.entity() == wisp.id() && m.fromTile() == 6 && m.toTile() == 5),
                "and the Wisp comes out at 5, inside the lane, with no collision anywhere");
    }

    // -- the Bulwark -----------------------------------------------------------

    @Test
    void theBulwarkRefusesThePush() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, STEP);
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));
        int hp = bulwark.hp();

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.VerbRefused refused = Encounters.firstOf(phrase, CombatEvent.VerbRefused.class);
        assertNotNull(refused, "the refusal must be a beat, not a silent no-op");
        assertEquals("push", refused.verb());
        assertEquals(CombatEvent.RefusalReason.UNYIELDING, refused.reason());
        assertEquals(0, e.state().hero().tile());
        assertEquals(1, bulwark.tile());
        assertEquals(hp, bulwark.hp(),
                "a refusal is not a collision: touching a rooted body must not punish you, "
                        + "or the answer to the Bulwark becomes 'never approach it'");
        assertTrue(Encounters.only(phrase, CombatEvent.Collided.class).isEmpty());
    }

    @Test
    void theBulwarkRefusesTheSwapToo() {
        // The Bulwark exists to deny *both* heroes their verb (combat-design.md
        // 2.4). Unyielding is therefore read as "refuses external displacement",
        // which covers a swap even though a swap never runs out of room.
        CombatEngine e = Encounters.againstBulwark(Hero.PILGRIM, STEP);
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.VerbRefused refused = Encounters.firstOf(phrase, CombatEvent.VerbRefused.class);
        assertNotNull(refused);
        assertEquals("swap", refused.verb());
        assertEquals(0, e.state().hero().tile());
        assertEquals(1, bulwark.tile());
    }

    @Test
    void theBulwarkRefusesToBeMovedButNotToBeTouched() {
        // The general rule for Unyielding, and the reason Draw is not simply dead
        // against it: the blade lands, the haul does not.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, DRAW);
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        int hp = bulwark.hp();
        e.apply(Command.add(0));

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertEquals(hp - DRAW.damage(), bulwark.hp(), "the strike must land");
        CombatEvent.VerbRefused refused = Encounters.firstOf(phrase, CombatEvent.VerbRefused.class);
        assertNotNull(refused);
        assertEquals("draw", refused.verb());
        assertEquals(1, bulwark.tile(), "and it must not have moved a tile");
    }

    // -- draw ------------------------------------------------------------------

    @Test
    void drawHaulsTheFirstBodyInRangeOneTileCloser() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(DRAW)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        assertEquals(3, wisp.tile(), "it struck and stood there, which is the whole of 3d.1");
        e.apply(Command.hold());
        assertEquals(4, wisp.tile(), "and gave ground on the turn after, so it is now two tiles out");

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Drawn drawn = Encounters.firstOf(phrase, CombatEvent.Drawn.class);
        assertNotNull(drawn);
        assertEquals(4, drawn.fromTile());
        assertEquals(3, drawn.toTile());
        assertEquals(3, wisp.tile());
    }

    @Test
    void drawOnAnAdjacentBodyHasNowhereToPutItAndSaysSo() {
        // The pull would land the target on the puller's own tile. It is refused,
        // but the Drawn beat still fires with from == to, because the line of force
        // happened even though nothing moved.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(DRAW)
                .build());
        Combatant front = e.state().at(3);
        e.apply(Command.add(0));
        assertEquals(3, front.tile(), "boxed in by its own ally, it could not give ground");

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Drawn drawn = Encounters.firstOf(phrase, CombatEvent.Drawn.class);
        assertNotNull(drawn);
        assertEquals(drawn.fromTile(), drawn.toTile());
        CombatEvent.MoveBlocked blocked = Encounters.firstOf(phrase, CombatEvent.MoveBlocked.class);
        assertNotNull(blocked);
        assertEquals(CombatEvent.BlockReason.OCCUPIED, blocked.reason());
        assertEquals(3, front.tile());
    }

    // -- motion that is deliberately not a verb --------------------------------

    @Test
    void aBackStepNeverInvokesTheVerb() {
        // Retreat is not an advance. The verb is what happens when you walk *into*
        // someone, and backing into them is refused instead -- otherwise every hero
        // would have a free reverse shove and the facing economy would evaporate.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 1, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 0, Facing.RIGHT)
                .loadout(BACK_STEP)
                .build());
        Combatant behind = e.state().at(1);
        e.apply(Command.add(0));
        assertEquals(1, behind.tile(), "boxed against its ally, it stays put");

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertTrue(Encounters.only(phrase, CombatEvent.Shoved.class).isEmpty());
        assertTrue(Encounters.only(phrase, CombatEvent.Swapped.class).isEmpty());
        assertTrue(Encounters.only(phrase, CombatEvent.VerbRefused.class).isEmpty());
        CombatEvent.MoveBlocked blocked = Encounters.firstOf(phrase, CombatEvent.MoveBlocked.class);
        assertNotNull(blocked);
        assertEquals(CombatEvent.BlockReason.OCCUPIED, blocked.reason());
        assertEquals(2, e.state().hero().tile());
    }

    @Test
    void aFeintIsMotionWithNoContactAtAll() {
        // "The negative space that makes contact read." A Feint into an occupied
        // tile is refused rather than resolved as a verb, which is what keeps it a
        // reposition and not a free shove that costs no turn.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(FEINT)
                .build());
        e.apply(Command.add(0)); // free-play: the board has not moved
        assertNotNull(e.state().at(3), "nothing has resolved yet, so the Wisp is still adjacent");

        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertTrue(Encounters.only(phrase, CombatEvent.Shoved.class).isEmpty());
        assertTrue(Encounters.only(phrase, CombatEvent.VerbRefused.class).isEmpty());
        assertEquals(CombatEvent.BlockReason.OCCUPIED,
                Encounters.firstOf(phrase, CombatEvent.MoveBlocked.class).reason());
    }

    @Test
    void aFeintIntoEmptySpaceMovesAndIsMarkedAsAFeint() {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, FEINT);
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Moved moved = Encounters.firstOf(phrase, CombatEvent.Moved.class);
        assertNotNull(moved);
        assertEquals(CombatEvent.MoveReason.FEINT, moved.reason(),
                "the reason is the animation's cue; a Feint must not read as a Step");
        assertEquals(1, e.state().hero().tile());
    }

    @Test
    void steppingOffTheEndOfTheLaneIsRefused() {
        CombatEngine e = CombatEngine.create(Encounters.duel(5, Hero.WARDEN)
                .heroAt(4, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 0, Facing.RIGHT)
                .loadout(STEP)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.MoveBlocked blocked = Encounters.firstOf(phrase, CombatEvent.MoveBlocked.class);
        assertNotNull(blocked);
        assertEquals(CombatEvent.BlockReason.LANE_EDGE, blocked.reason());
        assertEquals(4, e.state().hero().tile());
    }
}
