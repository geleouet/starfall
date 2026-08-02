package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seam between beats -- the field that decides whether five queued tiles read
 * as one phrase or as five separate events.
 *
 * <p>combat-design.md 1.1a spent the whole argument for a five-slot Ink Stanza on
 * this: five linked beats "each one flowing out of the last" is qualitatively
 * different material from three, and the flowing-out is the part the stream never
 * said anything about. STYLE.md 7.1 requires overlapping action and STYLE.md 10
 * fails a pass on sight of everything peaking on the same frame, and both of
 * those are claims about seams.
 *
 * <p>The tests split three ways, and the middle one is the substance:
 * <ol>
 *   <li>beats that may lean into the previous follow-through, and by how much;</li>
 *   <li>beats that may not, because their own geometry is the previous beat's
 *       output -- and the three different reasons that happens, which the
 *       animation layer treats differently;</li>
 *   <li>the guarantee that makes the hint safe to honour at all.</li>
 * </ol>
 */
class OverlapTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile KILLING_CUT = Tile.of(TileType.CUT).withDamage(9);
    private static final Tile STEP = Tile.of(TileType.STEP);
    private static final Tile TURN = Tile.of(TileType.TURN);
    private static final Tile DRAW = Tile.of(TileType.DRAW);
    private static final Tile PARRY = Tile.of(TileType.PARRY);
    private static final Tile SWEEP = Tile.of(TileType.SWEEP);

    // -- what may overlap ------------------------------------------------------

    @Test
    void theFirstBeatOfAPhraseHasNothingToLeanInto() {
        List<CombatEvent.BeatBegan> beats = phraseOf(CUT, Tile.of(TileType.CUT));
        assertEquals(Overlap.Limit.FIRST_BEAT, beats.get(0).overlap().limit());
        assertEquals(0, beats.get(0).overlap().intoRecovery());
        assertTrue(beats.get(0).overlap().forbidden());
    }

    @Test
    void oneBladeAfterAnotherLeansIntoTheFollowThrough() {
        // The ordinary case, and the whole point of the field: two strokes by the
        // same body that depend on nothing may share most of a beat, so a stanza of
        // blades reads as one continuous sentence rather than as a metronome.
        List<CombatEvent.BeatBegan> beats = phraseOf(CUT, Tile.of(TileType.CUT));
        Overlap seam = beats.get(1).overlap();
        assertEquals(Overlap.Limit.CONTINUES, seam.limit());
        assertEquals(Overlap.CONTINUING, seam.intoRecovery());
        assertFalse(seam.forbidden());
    }

    @Test
    void aBeatThatKillsDoesNotForbidTheNextOneFromStartingEarly() {
        // The deliberate non-dependency, and the decision that keeps the field
        // meaningful. Beat two genuinely resolves differently when beat one cleared
        // its target -- that is the whole of PhraseTest -- but the *gesture* does
        // not: the body winds up and cuts from the same place into the same tiles
        // whether or not anything is standing there, which is exactly why
        // combat-design.md makes a phrase keep resolving through an empty board.
        //
        // Treating a kill as a dependency would forbid nearly every overlap in the
        // game, and a hint that is almost always zero would have been worth
        // nothing.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .loadout(CUT, KILLING_CUT)
                .build());
        e.apply(Command.add(0)); // plain Cut, resolves second
        e.apply(Command.add(1)); // killing Cut, resolves first
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        assertTrue(Encounters.has(phrase, CombatEvent.Died.class), "the first beat did clear the board");
        assertTrue(Encounters.has(phrase, CombatEvent.Whiffed.class), "and the second went through empty air");
        List<CombatEvent.BeatBegan> beats = Encounters.only(phrase, CombatEvent.BeatBegan.class);
        assertEquals(Overlap.Limit.CONTINUES, beats.get(1).overlap().limit(),
                "the stroke is the same stroke whether or not it finds anything");
    }

    @Test
    void aGuardMayRiseWhileTheStrokeBeforeItIsStillTrailing() {
        // A Parry reads nothing and moves nobody, so nothing about it waits.
        List<CombatEvent.BeatBegan> beats = phraseOf(CUT, PARRY);
        assertEquals(TileType.PARRY, beats.get(1).tile().type());
        assertFalse(beats.get(1).overlap().forbidden());
    }

    // -- what forbids an overlap -----------------------------------------------

    @Test
    void aBladeAfterAStepWaitsBecauseItDoesNotYetKnowWhereItIsStandingFrom() {
        // The headline case combat-design.md 3d.5 asks for. A Step may be blocked
        // by the lane edge, may shove, may swap, may be refused by an Unyielding
        // body -- four different outcomes and four different tiles to strike from.
        // A stroke that began winding up before the footwork resolved would be
        // aimed from a tile the body never reached.
        List<CombatEvent.BeatBegan> beats = phraseOf(STEP, CUT);
        Overlap seam = beats.get(1).overlap();
        assertEquals(Overlap.Limit.AWAITS_FOOTING, seam.limit());
        assertEquals(0, seam.intoRecovery());
        assertTrue(seam.forbidden(), "this is the refusal the whole field exists to be able to express");
    }

    @Test
    void everythingAfterAStepWaitsAndNotJustBlades() {
        // The dependency is on the actor's footing, not on what the next beat does
        // with it, so it holds even for beats that touch nothing.
        for (Tile after : List.of(CUT, PARRY, TURN, SWEEP)) {
            List<CombatEvent.BeatBegan> beats = phraseOf(STEP, after);
            assertEquals(Overlap.Limit.AWAITS_FOOTING, beats.get(1).overlap().limit(),
                    after + " after a Step must wait for the ground under it");
        }
    }

    @Test
    void aBladeAfterATurnWaitsForTheBodyToComeRound() {
        // A different refusal from the one above, and named differently on purpose:
        // the body knows where it is standing, it just is not pointing there yet.
        // combat-design.md 2.2 makes the Turn "the whole body winding around; cloth
        // and hair last to arrive", so a stroke launched into the wind-around would
        // be aimed at the old facing.
        List<CombatEvent.BeatBegan> beats = phraseOf(TURN, CUT);
        assertEquals(Overlap.Limit.AWAITS_FACING, beats.get(1).overlap().limit());
        assertEquals(0, beats.get(1).overlap().intoRecovery());
    }

    @Test
    void aBladeAfterADrawWaitsForTheBodyItIsAboutToStrikeToArrive() {
        // The third refusal: nothing about the hero has changed, but the tile the
        // stroke is written for is still being filled. The Draw may haul the target
        // in, may be refused by an Unyielding body, may find the tile behind it
        // occupied -- and each of those puts a different body in front of the blade.
        List<CombatEvent.BeatBegan> beats = phraseOf(DRAW, CUT);
        assertEquals(Overlap.Limit.AWAITS_BOARD, beats.get(1).overlap().limit());
        assertEquals(0, beats.get(1).overlap().intoRecovery());
    }

    @Test
    void aTurnAfterADrawDoesNotWaitBecauseItReadsNothing() {
        // The control that stops AWAITS_BOARD from being a blanket rule. The
        // dependency is between a beat that moves a body and a beat that reads one;
        // a Turn does neither, so the hero may start winding around while the haul
        // is still coming in.
        List<CombatEvent.BeatBegan> beats = phraseOf(DRAW, TURN);
        assertEquals(Overlap.Limit.CONTINUES, beats.get(1).overlap().limit());
        assertFalse(beats.get(1).overlap().forbidden());
    }

    // -- the enemy phase is a phrase too ---------------------------------------

    @Test
    void twoBodiesAtOppositeEndsOfTheLaneAreNotWaitingOnEachOther() {
        // STYLE.md 7 does not grade the enemy phase by a gentler standard, and a
        // lane with several bodies acting in board order is as much a phrase as a
        // stanza is. Two of them fifteen tiles apart share nothing but the frame.
        CombatEngine e = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 5, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        List<CombatEvent.EnemyBeatBegan> beats =
                Encounters.only(e.apply(Command.hold()).events(), CombatEvent.EnemyBeatBegan.class);

        assertEquals(2, beats.size());
        assertEquals(Overlap.Limit.FIRST_BEAT, beats.get(0).overlap().limit());
        assertEquals(Overlap.Limit.UNRELATED, beats.get(1).overlap().limit());
        assertEquals(Overlap.INDEPENDENT, beats.get(1).overlap().intoRecovery());
        assertTrue(beats.get(1).overlap().intoRecovery() < Phases.WHOLE,
                "and still staggered, because STYLE.md 10 bans simultaneity as such "
                        + "and not merely simultaneity that something caused");
    }

    @Test
    void aBodyWalkingIntoTheTileTheBodyAheadIsLeavingHasToWaitForIt() {
        // The enemy-phase form of the same dependency. Charted Shadows resolve in
        // board order and the second one's destination is the first one's origin,
        // so its whole walk is conditional on a step that has not happened yet.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(CUT)
                .build());
        List<CombatEvent.EnemyBeatBegan> beats =
                Encounters.only(e.apply(Command.hold()).events(), CombatEvent.EnemyBeatBegan.class);

        assertEquals(2, beats.size());
        assertEquals(Overlap.Limit.AWAITS_BOARD, beats.get(1).overlap().limit());
        assertTrue(beats.get(1).overlap().forbidden());
    }

    @Test
    void aBodyWhosePigmentHasDriedStillTakesABeatOfItsOwn() {
        // combat-design.md 1.4 makes Stillness "the figure's pigment dries; motion
        // damping raised hard", which is a thing to draw rather than an absence of
        // one. It gets the shape of a held breath and a focus on itself, so the
        // animation layer has somewhere to hang the damping.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        wisp.statuses().apply(Status.STILLNESS, Statuses.DURATION);

        Resolution r = e.apply(Command.hold());

        CombatEvent.EnemyBeatBegan beat =
                Encounters.firstOf(r.events(), CombatEvent.EnemyBeatBegan.class);
        assertNotNull(beat, "a body that does nothing still occupies a beat");
        assertEquals(Intent.Kind.HOLD, beat.kind());
        assertEquals(Phases.BREATH, beat.phases());
        assertEquals(1, beat.focus().span(), "and the subject is itself, going nowhere");
        assertTrue(Encounters.has(r.events(), CombatEvent.Immobilised.class));
    }

    // -- the guarantee ---------------------------------------------------------

    @Test
    void anOverlapIsAlwaysMeasuredAgainstRecoveryOnlySoContactsStayStrictlyOrdered() {
        // This is the theorem the whole hint rests on, and it is worth stating as a
        // test rather than as a comment.
        //
        //   An overlap is expressed in parts of the *previous beat's recovery*, and
        //   recovery begins only after that beat's contact has finished. So a beat
        //   honouring its hint starts no earlier than the previous contact ended,
        //   and then waits out its own strictly positive wind-up before its own
        //   contact. Two contacts therefore cannot coincide, however the renderer
        //   scales the beats -- which is STYLE.md 7.0's third positive ("nothing may
        //   arrive at the same time") discharged in the rules rather than left to a
        //   renderer's good taste.
        //
        // Both premises are checked here across a real five-beat phrase.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT, STEP, SWEEP, TURN, Tile.of(TileType.BACK_STEP));
        for (int i = 0; i < 5; i++) {
            e.apply(Command.add(i));
        }
        List<CombatEvent.BeatBegan> beats =
                Encounters.only(Encounters.phrase(e.apply(Command.execute())), CombatEvent.BeatBegan.class);

        assertEquals(5, beats.size());
        for (CombatEvent.BeatBegan b : beats) {
            assertTrue(b.phases().windUp() > 0,
                    b.tile() + " has no wind-up, so its contact could coincide with the last one");
            assertTrue(b.overlap().intoRecovery() <= Phases.WHOLE,
                    b.tile() + " would eat past the previous beat's recovery and into its contact");
            assertTrue(b.phases().recoveryStart() > b.phases().contactStart(),
                    b.tile() + " has no recovery to lean into");
        }
    }

    @Test
    void aPhraseOfFiveNamesEverySeamAndNoneOfThemTwice() {
        // The stanza is five tiles because combat-design.md 1.1a wanted a sentence
        // with clauses. Five clauses means four seams plus an opening, and every one
        // of them has to carry a decision -- a phrase with an unstated seam is a
        // phrase the animation layer has to guess at, and it will guess "play to the
        // end", which is five separate events.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT, STEP, SWEEP, TURN, Tile.of(TileType.BACK_STEP));
        for (int i = 0; i < 5; i++) {
            e.apply(Command.add(i));
        }
        List<CombatEvent.BeatBegan> beats =
                Encounters.only(Encounters.phrase(e.apply(Command.execute())), CombatEvent.BeatBegan.class);

        assertEquals(5, beats.size());
        assertEquals(Overlap.Limit.FIRST_BEAT, beats.get(0).overlap().limit());
        for (int i = 1; i < beats.size(); i++) {
            Overlap seam = beats.get(i).overlap();
            assertNotNull(seam, "beat " + i + " left its seam unstated");
            assertTrue(seam.limit() != Overlap.Limit.FIRST_BEAT,
                    beats.get(i).tile() + " is not the first beat and must not claim to be");
        }
    }

    // -- fixtures --------------------------------------------------------------

    /**
     * A two-beat phrase resolving {@code first} then {@code second}, on a lane
     * quiet enough that nothing interferes. The stanza is LIFO, so the tiles go in
     * backwards.
     */
    private static List<CombatEvent.BeatBegan> phraseOf(Tile first, Tile second) {
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, first, second);
        e.apply(Command.add(1));
        e.apply(Command.add(0));
        List<CombatEvent.BeatBegan> beats =
                Encounters.only(Encounters.phrase(e.apply(Command.execute())), CombatEvent.BeatBegan.class);
        assertEquals(2, beats.size());
        assertEquals(first.type(), beats.get(0).tile().type());
        assertEquals(second.type(), beats.get(1).tile().type());
        return beats;
    }
}
