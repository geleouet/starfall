package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The staging layer: the shape of a beat, what it is about, how hard a body is
 * travelling, and how one leaves.
 *
 * <p>None of this changes a single rule. All of it changes what an animator would
 * draw, which is the test combat-design.md 0 sets for anything entering the
 * engine at all. The specification being satisfied here is STYLE.md 7 -- 7.1's
 * 40/15/45 split, 7.2's drift-not-launch, 7.3's ink dissolve, 9's glide toward
 * the exchange -- and the reason it lives in the rules rather than in the
 * renderer is that all of it is ordinal, so none of it can drift between two
 * replays of one seed.
 *
 * <p>What is deliberately <em>not</em> tested here is anything in seconds or
 * pixels, because there is nothing in seconds or pixels: every number below is a
 * proportion, a tile, or a step on a four-value scale.
 */
class StagingTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile HEAVY_CUT = Tile.of(TileType.CUT).withDamage(5);
    private static final Tile STEP = Tile.of(TileType.STEP);
    private static final Tile THRUST = Tile.of(TileType.THRUST);

    // -- phases ----------------------------------------------------------------

    @Test
    void everyBeatKindInTheGameDeclaresAWellFormedSplit() {
        // "Correctly ordered" is three separate claims and each is load-bearing:
        // the phases run wind-up then contact then recovery; they exhaust the beat
        // rather than leaving a gap the renderer has to invent; and none of them is
        // zero. That last one is the one that is easy to lose and expensive to
        // lose: a zero wind-up would break the guarantee OverlapTest rests on, and
        // a zero recovery is STYLE.md 7.1's "nothing arrives at rest abruptly"
        // violated in the data before a renderer has had a chance to violate it.
        List<Phases> catalogue = new ArrayList<>();
        for (TileType type : TileType.values()) {
            catalogue.add(Phases.of(type));
        }
        for (Intent.Kind kind : Intent.Kind.values()) {
            catalogue.add(Phases.of(kind));
        }
        catalogue.add(Phases.DEATH);
        catalogue.add(Phases.BURST);

        for (Phases p : catalogue) {
            assertTrue(p.windUp() > 0, p + " has no anticipation");
            assertTrue(p.contact() > 0, p + " never arrives");
            assertTrue(p.recovery() > 0, p + " stops dead");
            assertEquals(Phases.WHOLE, p.windUp() + p.contact() + p.recovery(), p + " does not fill its beat");
            assertTrue(p.contactStart() < p.contactEnd(), p + " contacts for no time at all");
            assertTrue(p.contactEnd() < Phases.WHOLE, p + " has no follow-through");
            assertEquals(p.contactEnd(), p.recoveryStart());
        }
    }

    @Test
    void aStrikeIsTheFortyFifteenFortyFiveOfStyleSevenPointOne() {
        // Quoted verbatim from the rubric: "a strike is roughly 40% wind-up / 15%
        // travel / 45% follow-through. Fighting-game timing (fast wind-up, hard
        // freeze on impact) is the exact opposite and is forbidden." Pinned as a
        // number because a drift toward 15/10/75 would be that exact forbidden
        // timing and no other test in the suite would notice.
        assertEquals(new Phases(40, 15, 45), Phases.STRIKE);
        for (TileType type : List.of(TileType.CUT, TileType.THRUST, TileType.SWEEP)) {
            assertEquals(Phases.STRIKE, Phases.of(type), type + " is a blade and must be shaped like one");
        }
        assertEquals(Phases.STRIKE, Phases.of(Intent.Kind.ATTACK),
                "a Charted Shadow's blade is graded by the same rubric as the hero's");
    }

    @Test
    void aTurnIsMostlyItsFollowThroughAndAParryIsMostlyItsAnticipation() {
        // combat-design.md 2.2 for the Turn -- "the whole body winding around; cloth
        // and hair last to arrive" -- and STYLE.md 7.2 for the Parry, which wants a
        // deflection curve rather than a collision. Both are claims about which
        // phase dominates, so they are testable as such.
        Phases turn = Phases.of(TileType.TURN);
        assertTrue(turn.recovery() > turn.windUp() + turn.contact(),
                "the arriving is more than half the beat: " + turn);

        Phases parry = Phases.of(TileType.PARRY);
        assertTrue(parry.windUp() > Phases.STRIKE.windUp(), "a parry gathers longer than a strike: " + parry);
        assertTrue(parry.contact() < Phases.STRIKE.contact(), "and touches for less: " + parry);
    }

    @Test
    void everyBeatEmittedInAFightCarriesTheShapeItsKindDeclares() {
        // The catalogue above is a fact about a table; this is a fact about the
        // stream. They are different failures: a beat could carry a well-formed
        // split that belongs to the wrong kind, and the animation layer would stage
        // a Turn like a Cut without anything looking wrong in isolation.
        Set<TileType> heroKinds = EnumSet.noneOf(TileType.class);
        Set<Intent.Kind> bodyKinds = EnumSet.noneOf(Intent.Kind.class);

        for (CombatEvent event : longMixedFight()) {
            if (event instanceof CombatEvent.BeatBegan b) {
                assertNotNull(b.phases(), "a beat with no shape is an instant again");
                assertEquals(Phases.of(b.tile().type()), b.phases(), b.tile() + " was staged as something else");
                assertNotNull(b.overlap());
                assertNotNull(b.focus());
                heroKinds.add(b.tile().type());
            }
            if (event instanceof CombatEvent.EnemyBeatBegan b) {
                assertNotNull(b.phases());
                assertEquals(Phases.of(b.kind()), b.phases(), b.kind() + " was staged as something else");
                assertNotNull(b.overlap());
                assertNotNull(b.focus());
                bodyKinds.add(b.kind());
            }
        }

        assertTrue(heroKinds.containsAll(List.of(TileType.CUT, TileType.STEP, TileType.TURN, TileType.DRAW)),
                "that script should have spent a blade, a step, a turn and a reach: " + heroKinds);
        assertEquals(4, heroKinds.stream().map(Phases::of).distinct().count(),
                "and those are four different shapes, not one shape wearing four names");
        assertTrue(bodyKinds.containsAll(List.of(Intent.Kind.ATTACK, Intent.Kind.ADVANCE)),
                "and should have seen bodies both walking and swinging: " + bodyKinds);
    }

    @Test
    void aDoubleStrikePutsItsTwoStrokesAtDifferentInstants() {
        // STYLE.md 10 fails a pass on sight of everything peaking on the same
        // frame, and Double Strike is the one mechanic that resolves twice inside
        // one beat. Reporting one contact instant for both would put two blows on
        // one frame in the data, before the renderer had any say.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN,
                Tile.of(TileType.CUT, Enchantment.DOUBLE_STRIKE));
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        List<CombatEvent.Swung> strokes = Encounters.only(phrase, CombatEvent.Swung.class);
        assertEquals(2, strokes.size());
        assertTrue(strokes.get(0).at() < strokes.get(1).at(),
                "two strokes in one beat, and the second must arrive after the first: "
                        + strokes.get(0).at() + " then " + strokes.get(1).at());
        assertTrue(strokes.get(1).at() < Phases.WHOLE, "and both inside the beat that holds them");
    }

    @Test
    void aSingleStrokeContactsWhereItsBeatSaysItWill() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT);
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.BeatBegan beat = Encounters.firstOf(phrase, CombatEvent.BeatBegan.class);
        CombatEvent.Swung swung = Encounters.firstOf(phrase, CombatEvent.Swung.class);
        assertEquals(beat.phases().contactStart(), swung.at(),
                "the stroke and the beat that contains it must agree on when it lands");
    }

    // -- focus -----------------------------------------------------------------

    @Test
    void aBeatNamesItsSubjectAndTheRunOfTilesTheCameraHasToHold() {
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .loadout(THRUST, Tile.of(TileType.TURN))
                .build());
        e.apply(Command.add(1)); // Turn resolves second
        e.apply(Command.add(0)); // Thrust resolves first

        List<CombatEvent.BeatBegan> beats =
                Encounters.only(Encounters.phrase(e.apply(Command.execute())), CombatEvent.BeatBegan.class);

        Focus thrust = beats.get(0).focus();
        assertEquals(e.state().hero().id(), thrust.subject(), "the subject is whose gesture it is");
        assertEquals(2, thrust.fromTile());
        assertEquals(4, thrust.toTile(), "a Thrust pierces two tiles ahead and the camera holds all three");
        assertEquals(3, thrust.span());

        Focus turn = beats.get(1).focus();
        assertEquals(1, turn.span(), "a Turn goes nowhere, so the push-in is as tight as it gets");
        assertEquals(turn.fromTile(), turn.centre());
    }

    @Test
    void theFocusSpanIsWhatMakesAChargeADifferentShotFromAStep() {
        // STYLE.md 9 wants the push-in to be a small move on a short exchange and a
        // large one on a long approach, and combat-design.md 1.6 makes the camera
        // derive its framing from the lane rather than from a constant. A Runner
        // collapsing thirteen tiles and a Wisp stepping one are the same subject
        // and completely different shots, and the span is the only thing that says
        // so before the beat resolves.
        CombatEngine runner = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.RUNNER, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        Resolution charge = runner.apply(Command.hold());
        Focus wide = Encounters.firstOf(charge.events(), CombatEvent.EnemyBeatBegan.class).focus();
        assertEquals(1, wide.fromTile());
        assertEquals(14, wide.toTile());
        assertEquals(14, wide.span(), "thirteen tiles of approach, all of it in frame");

        CombatEngine wisp = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        Resolution step = wisp.apply(Command.hold());
        Focus tight = Encounters.firstOf(step.events(), CombatEvent.EnemyBeatBegan.class).focus();
        assertEquals(2, tight.span(), "one tile of walking, and the same event kind");
    }

    // -- force -----------------------------------------------------------------

    @Test
    void aChargerAcrossTheLaneAndAWispSteppingOneAreNotTheSameForce() {
        // combat-design.md 3d.5 names this gap exactly: both emit the same move
        // event, "distinguished only by tile delta", and a tile delta is a distance
        // rather than a force.
        CombatEngine runner = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.RUNNER, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        CombatEvent.Moved charge =
                Encounters.firstOf(runner.apply(Command.hold()).events(), CombatEvent.Moved.class);
        assertEquals(CombatEvent.MoveReason.CHARGE, charge.reason());
        assertEquals(Force.HEADLONG, charge.force());
        assertEquals(-13, charge.delta());

        CombatEngine wisp = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.WISP, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        CombatEvent.Moved walk =
                Encounters.firstOf(wisp.apply(Command.hold()).events(), CombatEvent.Moved.class);
        assertEquals(Force.DRIVE, walk.force());
        assertEquals(-1, walk.delta());
    }

    @Test
    void beingShovedIsSofterThanWalkingBecauseKnockbackIsADrift() {
        // STYLE.md 7.2, and the one result here that looks wrong until it is read
        // properly: a body carried one tile registers *less* force than a body that
        // walked one tile under its own weight. That is the point. "A struck figure
        // should be carried backward like a sheet of silk caught in wind"; a launch
        // is the failure mode, and a deliberate stride has more muscle behind it
        // than being pushed does.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        List<CombatEvent.Moved> moves = Encounters.only(phrase, CombatEvent.Moved.class);
        CombatEvent.Moved shoved = moves.stream()
                .filter(m -> m.reason() == CombatEvent.MoveReason.SHOVED).findFirst().orElseThrow();
        CombatEvent.Moved walked = moves.stream()
                .filter(m -> m.reason() == CombatEvent.MoveReason.STEP).findFirst().orElseThrow();

        assertEquals(Force.DRIFT, shoved.force());
        assertEquals(Force.DRIVE, walked.force());
        assertTrue(shoved.force().ordinal() < walked.force().ordinal(),
                "carried is softer than driven, and the whole of 7.2 turns on that");
        assertEquals(1, Math.abs(shoved.delta()), "both travelled exactly one tile");
        assertEquals(1, Math.abs(walked.delta()));
    }

    @Test
    void aSwapIsTheOneContactWithNoForceAtAll() {
        // combat-design.md 2.1: "swap is an interpenetration with no impact at
        // all". If both Moveds of a swap carried the force of a step, the animation
        // layer would draw the Pilgrim's verb as a shove that happened to end in the
        // wrong tiles.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.PILGRIM)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        List<CombatEvent.Moved> moves = Encounters.only(phrase, CombatEvent.Moved.class);
        assertEquals(2, moves.size());
        for (CombatEvent.Moved m : moves) {
            assertEquals(CombatEvent.MoveReason.SWAPPED, m.reason());
            assertEquals(Force.NONE, m.force(), "nothing drives either body through the other");
        }
    }

    @Test
    void givingGroundDriftsAndWalkingInDrives() {
        // The two halves of combat-design.md 3d.1's declared footwork. A body that
        // gives ground is reluctant and a body that closes is not, and until now the
        // stream said only which direction each went.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 3, Facing.LEFT)
                .heroHp(50)
                .loadout(CUT)
                .build());
        e.apply(Command.hold()); // it strikes and stays
        CombatEvent.Moved gave =
                Encounters.firstOf(e.apply(Command.hold()).events(), CombatEvent.Moved.class);
        assertEquals(CombatEvent.MoveReason.GAVE_GROUND, gave.reason());
        assertEquals(Force.DRIFT, gave.force());

        CombatEngine aggressive = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EncounterSpec.Placement.of(EnemyArchetype.REACHER, 2, Facing.LEFT)
                        .with(Trait.AGGRESSIVE))
                .heroHp(50)
                .loadout(CUT)
                .build());
        aggressive.apply(Command.hold()); // strikes from reach two
        CombatEvent.Moved closed =
                Encounters.firstOf(aggressive.apply(Command.hold()).events(), CombatEvent.Moved.class);
        assertEquals(CombatEvent.MoveReason.CLOSED_IN, closed.reason());
        assertEquals(Force.DRIVE, closed.force());
    }

    // -- death staging ---------------------------------------------------------

    @Test
    void aBodyCutDownShedsItsInkAlongTheWayTheBladeTravelled() {
        // STYLE.md 7.3 asks for a bloom of ink rather than a fall, and 3 makes
        // dissolving into the paper the most important material rule in the game.
        // Neither is expressible against an instantaneous death, which is what
        // combat-design.md 3d.5 means by "an ink dissolve wants a duration and a
        // direction".
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 2, Facing.LEFT)
                .loadout(HEAVY_CUT)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Died died = Encounters.firstOf(phrase, CombatEvent.Died.class);
        assertNotNull(died);
        Dissolve d = died.dissolve();
        assertEquals(Facing.RIGHT, d.along(), "struck from the near end, so the ink runs away from it");
        assertEquals(Force.DRIFT, d.force(), "silk, not a launch");
        assertEquals(2, d.spans(), "and it is still settling a beat after the sentence moved on");
        assertEquals(Phases.DEATH, d.phases());
        assertTrue(d.phases().recovery() > d.phases().windUp() + d.phases().contact(),
                "a dissolve is nearly all aftermath: " + d.phases());
    }

    @Test
    void aBodyThrownApartByABloomGoesFasterAndHarderThanOneCutDown() {
        // "Choose *where* it dies, not just whether" (combat-design.md 2.4). The
        // choice is only worth making if the two deaths are drawn differently.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WARDEN, 3, Facing.LEFT)
                .enemy(EnemyArchetype.WARDEN, 4, Facing.LEFT)
                .loadout(THRUST)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent.Died> deaths =
                Encounters.only(Encounters.phrase(e.apply(Command.execute())), CombatEvent.Died.class);

        assertEquals(2, deaths.size(), "one bloom sets off the other");
        Dissolve blade = deaths.get(0).dissolve();
        Dissolve bloom = deaths.get(1).dissolve();
        assertEquals(Force.DRIFT, blade.force());
        assertEquals(Force.HEADLONG, bloom.force());
        assertEquals(Phases.BURST, bloom.phases());
        assertTrue(bloom.spans() < blade.spans(), "flung apart inside a beat rather than settling over two");
        assertTrue(bloom.phases().windUp() < blade.phases().windUp(), "and with almost no gathering first");
    }

    @Test
    void aBodyThatBleedsOutHasNoDirectionToInheritAndPoolsWhereItStands() {
        // The one death with no blow behind it. Saying so with a force of NONE and
        // the body's own facing is more useful to the animation layer than a null
        // direction, which it would have to invent a fallback for anyway.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        wisp.hp(1);
        wisp.statuses().apply(Status.SEEPING, Statuses.DURATION);

        Resolution r = e.apply(Command.hold());

        CombatEvent.Died died = Encounters.firstOf(r.events(), CombatEvent.Died.class);
        assertNotNull(died, "the tick finished it");
        assertEquals(-1, died.killer());
        assertEquals(Force.NONE, died.dissolve().force());
        assertEquals(wisp.facing(), died.dissolve().along());
        assertEquals(3, died.dissolve().spans(), "the slowest and quietest death in the game");
    }

    // -- fixtures --------------------------------------------------------------

    /** A varied fight: every command form, four archetypes, most of the hand spent. */
    private static List<CombatEvent> longMixedFight() {
        CombatEngine e = CombatEngine.create(EncounterSpec.builder(13, Hero.WARDEN)
                .heroAt(1, Facing.RIGHT)
                .heroHp(60)
                .loadout(CUT, STEP, Tile.of(TileType.SWEEP), Tile.of(TileType.TURN),
                        Tile.of(TileType.DRAW), Tile.of(TileType.PARRY))
                .enemy(EnemyArchetype.WISP, 5, Facing.LEFT)
                .enemy(EnemyArchetype.REACHER, 7, Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, 9, Facing.LEFT)
                .enemy(EnemyArchetype.RUNNER, 12, Facing.LEFT)
                .seed(20260802L)
                .build());
        List<CombatEvent> log = new ArrayList<>(e.opening());
        for (int step = 0; step < 40 && !e.state().outcome().over(); step++) {
            Command c = Command.hold();
            if (e.state().stanza().size() >= 2) {
                c = Command.execute();
            } else {
                for (int i = 0; i < e.state().loadout().size(); i++) {
                    int candidate = (i + step) % e.state().loadout().size();
                    if (e.can(Command.add(candidate))) {
                        c = Command.add(candidate);
                        break;
                    }
                }
            }
            log.addAll(e.apply(c).events());
        }
        return log;
    }
}
