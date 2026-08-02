package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.Command;
import dev.starfall.combat.ContactPoint;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Phases;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That each entry in the directive vocabulary earns its place, by being produced
 * for the beat it was designed for and by carrying enough to act on.
 *
 * <p>A vocabulary is easy to over-design. The test that keeps it honest is whether
 * the engine's own events actually drive every entry: a directive kind that no
 * legal sequence of commands can produce is a speculation, and combat-design.md 0
 * filters the whole project on exactly that question.
 */
class DirectiveVocabularyTest {

    @Test
    void everyDirectiveKindIsProducedByAnOrdinaryFight() {
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .heroHp(60)
                .enemy(EnemyArchetype.WARDEN, 1, Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, 3, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT).withDamage(9), Tile.of(TileType.STEP),
                        Tile.of(TileType.TURN), Tile.of(TileType.PARRY), Tile.of(TileType.SWEEP))
                .build());
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.add(0), Command.add(1), Command.execute(),
                Command.add(2), Command.add(3), Command.execute(),
                Command.add(4), Command.execute(), Command.hold());
        Schedule score = s.schedule();

        assertTrue(!score.of(Directive.IkTarget.class).isEmpty(), "no IK targets");
        assertTrue(!score.of(Directive.Impulse.class).isEmpty(), "no cloth or hair impulses");
        assertTrue(!score.of(Directive.CameraKey.class).isEmpty(), "no camera keys");
        assertTrue(!score.of(Directive.FacingChange.class).isEmpty(), "no facing changes");
        assertTrue(!score.of(Directive.PoseChange.class).isEmpty(), "no pose changes");
        assertTrue(!score.of(Directive.TimeRamp.class).isEmpty(), "no held breath");
        assertTrue(!score.of(Directive.Ink.class).isEmpty(), "no ink");
    }

    @Test
    void everyChainIsAddressedAndEveryRegionIsShoved() {
        Schedule score = busyFight();
        Set<Chain> chains = EnumSet.noneOf(Chain.class);
        for (Directive.IkTarget d : score.of(Directive.IkTarget.class)) {
            chains.add(d.chain());
        }
        assertEquals(EnumSet.allOf(Chain.class), chains,
                "a chain in the vocabulary is never driven: " + chains);

        Set<Region> regions = EnumSet.noneOf(Region.class);
        for (Directive.Impulse d : score.of(Directive.Impulse.class)) {
            regions.add(d.region());
        }
        assertEquals(EnumSet.allOf(Region.class), regions,
                "a simulated region in the vocabulary is never shoved: " + regions);
    }

    @Test
    void everyStanceIsReachedFromLegalPlay() {
        // A stance nothing can produce is a speculation, and combat-design.md 0 filters
        // the whole project on that. Three boards, because no single one can contain
        // both movement verbs -- push and swap belong to different heroes.
        Set<Stance> seen = EnumSet.noneOf(Stance.class);
        for (Schedule score : List.of(busyFight(), knockback(), swap(), dried())) {
            for (Directive.PoseChange d : score.of(Directive.PoseChange.class)) {
                seen.add(d.stance());
            }
        }
        assertEquals(EnumSet.allOf(Stance.class), seen,
                "these stances are unreachable: "
                        + EnumSet.complementOf(EnumSet.copyOf(seen)));
    }

    /** Hero at 0, one Wisp at 1, room behind it: a Step becomes the Warden's shove. */
    private static Schedule knockback() {
        CombatEngine engine = Fixtures.lane(11, Hero.WARDEN, EnemyArchetype.WISP, 1,
                Tile.of(TileType.STEP), Tile.of(TileType.DRAW));
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.add(0), Command.execute(),
                Command.add(1), Command.execute());
        return s.schedule();
    }

    /** The same board under the other hero, where a Step is an interpenetration. */
    private static Schedule swap() {
        CombatEngine engine = Fixtures.lane(11, Hero.PILGRIM, EnemyArchetype.WISP, 1,
                Tile.of(TileType.STEP));
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.add(0), Command.execute());
        return s.schedule();
    }

    /** combat-design.md 1.4's Stillness: "the figure's pigment dries", which is a thing to draw. */
    private static Schedule dried() {
        CombatEngine engine = Fixtures.lane(9, Hero.WARDEN, EnemyArchetype.WISP, 1,
                Tile.of(TileType.CUT, dev.starfall.combat.Enchantment.STILLNESS));
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.add(0), Command.execute(), Command.hold(), Command.hold());
        return s.schedule();
    }

    @Test
    void theSwapProducesTheOneContactWithNothingBehindIt() {
        // combat-design.md 2.1 calls the Pilgrim's verb "an interpenetration with no
        // impact at all" and "the harder animation problem of the two". It has to be
        // stageable and it has to be staged differently from a shove.
        CombatEngine engine = Fixtures.lane(7, Hero.PILGRIM, EnemyArchetype.WISP, 1,
                Tile.of(TileType.STEP));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s, Command.add(0), Command.execute());
        Schedule score = s.schedule();

        CombatEvent.Swapped swap = Fixtures.only(events, CombatEvent.Swapped.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("the Pilgrim did not swap"));
        List<Directive.PoseChange> passing = score.of(Directive.PoseChange.class).stream()
                .filter(d -> d.stance() == Stance.PASSING)
                .toList();
        assertEquals(2, passing.size(), "a swap is two figures passing, not one");
        assertTrue(passing.get(0).at() != passing.get(1).at(),
                "both halves of the swap begin on the same frame, which STYLE.md 10 fails on sight");
        assertEquals(ContactPoint.Part.ARM, swap.meeting().onActor().part(),
                "the fixture stopped being sleeve on sleeve");

        // And the sleeves cross: the two impulses point opposite ways.
        List<Directive.Impulse> sleeves = score.of(Directive.Impulse.class).stream()
                .filter(d -> d.region() == Region.CLOTH_SLEEVE)
                .filter(d -> d.body() == swap.a() || d.body() == swap.b())
                .toList();
        assertTrue(sleeves.size() >= 2, "the swap moved fewer than two sleeves");
        assertTrue(sleeves.get(0).dirX() * sleeves.get(1).dirX() < 0,
                "the two sleeves are thrown the same way, so nothing crosses");
    }

    @Test
    void aParryAimsBothArmsAtTheirOwnNamedContactPoint() {
        // Meeting's whole reason for naming the crossing twice: "a parry is two
        // skeletons that have to agree on one point in space, and each of them solves
        // for it in its own frame."
        CombatEngine engine = Fixtures.lane(7, Hero.WARDEN, EnemyArchetype.BULWARK, 1,
                Tile.of(TileType.PARRY));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s,
                Command.add(0), Command.execute(), Command.hold());
        Schedule score = s.schedule();

        CombatEvent.BladeMet met = Fixtures.only(events, CombatEvent.BladeMet.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no blade ever met a blade"));
        assertTrue(met.meeting().agrees(), "the engine's two names do not describe one crossing");

        List<Directive.IkTarget> attacker = score.chain(met.attacker(), Chain.SWORD_ARM);
        List<Directive.IkTarget> defender = score.chain(met.defender(), Chain.SWORD_ARM);
        assertTrue(attacker.stream().anyMatch(d -> d.target().body() == met.attacker()
                        && d.target().site() == Anchor.Site.CONTACT),
                "the attacker's arm is not aimed at its own half of the meeting");
        assertTrue(defender.stream().anyMatch(d -> d.target().body() == met.defender()
                        && d.target().site() == Anchor.Site.CONTACT),
                "the defender's arm is not aimed at its own half of the meeting");

        // STYLE.md 7.2: "the defender's arm gives ground on an IK curve rather than
        // stopping dead", so there are two defender targets and the second is further
        // back along the blow.
        assertTrue(defender.size() >= 2, "the defender's arm stops dead on one target");
        assertTrue(!score.of(Directive.Ink.class).stream()
                        .filter(d -> d.kind() == Directive.InkKind.CLASH).toList().isEmpty(),
                "no clash bloom at the crossing");
    }

    @Test
    void aContactPointBecomesAPointInSpaceThatDependsOnTheTouchedBodysOwnFacing() {
        // ContactPoint: "why the side is relative and not left/right. Two bodies
        // squared up on a lane face opposite ways, so a single world-space side would
        // be the leading side of one and the trailing side of the other."
        Stage stage = new Stage(9);
        Standing right = Standing.opening(new Standing.Body(7, 4, Facing.RIGHT));
        Standing left = Standing.opening(new Standing.Body(7, 4, Facing.LEFT));
        ContactPoint leading = new ContactPoint(7, ContactPoint.Part.TORSO,
                ContactPoint.Side.LEADING, ContactPoint.Height.MIDDLE);

        Anchor facingRight = stage.contact(leading, right);
        Anchor facingLeft = stage.contact(leading, left);
        assertTrue(facingRight.x() > stage.tileX(4), "a leading contact should sit ahead of the body");
        assertTrue(facingLeft.x() < stage.tileX(4), "and ahead means the other way when it is turned");
        assertEquals(facingRight.y(), facingLeft.y(), 1e-9, "height does not depend on facing");
        assertEquals(Stage.Y_MIDDLE, facingRight.y(), 1e-9);
    }

    @Test
    void theHeightRuleMatchesTheEnginesOwn() {
        // Scheduler restates ContactPoint.heightForReach because the engine's copy is
        // package-private. Two copies of a rule drift; this is the pin that stops it.
        CombatEngine engine = Fixtures.lane(9, Hero.WARDEN, EnemyArchetype.REACHER, 3,
                Tile.of(TileType.CUT), Tile.of(TileType.THRUST));
        Scheduler s = Fixtures.scheduler(engine);
        List<CombatEvent> events = Fixtures.run(engine, s,
                Command.hold(), Command.hold(), Command.hold(), Command.hold());
        List<CombatEvent.Hit> hits = Fixtures.only(events, CombatEvent.Hit.class);
        assertTrue(!hits.isEmpty(), "the Reacher never landed a blow");
        for (CombatEvent.Hit h : hits) {
            if (h.landed() == null) {
                continue;
            }
            int distance = Math.abs(s.standing().tile(h.attacker()) - s.standing().tile(h.target()));
            if (distance >= 1) {
                assertEquals(Scheduler.heightForReach(distance), h.landed().height(),
                        "the staging layer's height rule has drifted from the engine's at reach "
                                + distance);
            }
        }
    }

    @Test
    void anImpulseAlwaysCarriesTheAnchorItsLagIsMeasuredAgainst() {
        Schedule score = busyFight();
        for (Directive.Impulse d : score.of(Directive.Impulse.class)) {
            assertNotNull(d.region().anchor(), "an impulse without an anchor is unfalsifiable");
            assertTrue(d.arrives() > d.at(), "an impulse that answers instantly has no lag");
            assertTrue(d.arrivesBehindHips() >= d.arrives() - 1e-9,
                    "the two readings of the same lag are inconsistent");
            assertTrue(d.magnitude() >= 0.0 && d.magnitude() <= 1.0,
                    "impulse magnitude " + d.magnitude() + " is outside 0..1");
        }
    }

    @Test
    void aBeatsPhasesBecomeAbsoluteInstantsThatStillSumToTheWholeBeat() {
        // The unit conversion itself: proportions in, seconds out, nothing lost.
        for (int span = 1; span <= 15; span++) {
            for (Phases p : List.of(Phases.STRIKE, Phases.REACH, Phases.GUARD, Phases.TRAVEL,
                    Phases.WIND_AROUND, Phases.BREATH, Phases.DEATH, Phases.BURST)) {
                ScheduledBeat b = ScheduledBeat.after(null, 0, ScheduledBeat.Bracket.STANZA, 0, p,
                        dev.starfall.combat.Overlap.firstBeat(),
                        new dev.starfall.combat.Focus(0, 0, span - 1));
                assertEquals(b.duration(), b.windUpSpan() + b.contactSpan() + b.recoverySpan(), 1e-9,
                        p + " over " + span + " tiles loses time in the conversion");
                assertTrue(b.contactStart() < b.contactEnd(), p + " contacts for no time");
                assertTrue(b.recoveryStart() < b.end(), p + " has no follow-through");
                assertEquals(b.contactEnd(), b.recoveryStart(), 1e-9);
            }
        }
    }

    @Test
    void theUnitBeatIsTheOneBothStyleNumbersWereDerivedAgainst() {
        // The two independent checks of BASE_BEAT, stated as a test so a future
        // retune has to answer both rather than one.
        double unit = Timing.beatSeconds(2);
        assertEquals(Timing.BASE_BEAT, unit, 1e-9, "the ordinary one-tile action is the unit beat");

        double strikeRecovery = Timing.parts(unit, Phases.STRIKE.recovery());
        assertTrue(strikeRecovery >= Timing.SETTLE_ROOT && strikeRecovery <= Timing.SETTLE_TIP,
                "a strike's recovery is " + strikeRecovery + " s, outside STYLE.md 7.1's settle band");

        double carry = Timing.parts(unit, Phases.TRAVEL.contact() + Phases.TRAVEL.recovery());
        assertEquals(Timing.KNOCKBACK_ARRIVAL, carry, 0.05,
                "the knockback carry is " + carry + " s against STYLE.md 7.2's ~0.8 s");
    }

    private static Schedule busyFight() {
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(11, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .heroHp(60)
                .enemy(EnemyArchetype.WARDEN, 1, Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, 3, Facing.LEFT)
                .enemy(EnemyArchetype.RUNNER, 8, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT).withDamage(9), Tile.of(TileType.STEP),
                        Tile.of(TileType.PARRY), Tile.of(TileType.TURN), Tile.of(TileType.DRAW))
                .build());
        Scheduler s = Fixtures.scheduler(engine);
        Fixtures.run(engine, s, Command.add(0), Command.add(1), Command.execute(),
                Command.hold(), Command.add(2), Command.execute(),
                Command.add(3), Command.add(4), Command.execute(), Command.hold());
        return s.schedule();
    }
}
