package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where two bodies touch, and when.
 *
 * <p>combat-design.md 3d.5: "a shove says two bodies met, not whether at shoulder
 * or hilt; a blade meeting gives no crossing point. IK targets need both." These
 * tests are the answer to that, and they are all really one question -- can the
 * animation layer aim a chain from the stream alone, without re-deriving geometry
 * the engine already knows and would get subtly differently?
 *
 * <p>The parry is the case that matters most. STYLE.md 7.2 calls it "a deflection
 * curve, not a collision", and a deflection is two skeletons agreeing on one
 * point at one instant. Two chains each solving their own arc from their own
 * assumptions produce blades that cross on different frames, which is precisely
 * the collision reading the rubric forbids. So the crossing is named once, in
 * both bodies' vocabularies, with one shared instant.
 */
class ContactTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile PARRY = Tile.of(TileType.PARRY);
    private static final Tile STEP = Tile.of(TileType.STEP);
    private static final Tile DRAW = Tile.of(TileType.DRAW);

    // -- the parry -------------------------------------------------------------

    @Test
    void aParrysContactMomentIsOneInstantBothBodiesAreGivenTogether() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, PARRY);
        Combatant hero = e.state().hero();
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));

        Resolution r = e.apply(Command.execute());

        CombatEvent.BladeMet met = Encounters.firstOf(r.events(), CombatEvent.BladeMet.class);
        assertNotNull(met, "a parry that deflects is the signature beat and must say so");
        Meeting crossing = met.meeting();

        // One physical event, named twice.
        assertTrue(crossing.agrees(), "the two names must describe one crossing: " + crossing);
        assertEquals(bulwark.id(), crossing.onActor().body(), "the attacker's half");
        assertEquals(hero.id(), crossing.onTarget().body(), "and the defender's");
        assertEquals(ContactPoint.Part.BLADE, crossing.onActor().part());
        assertEquals(ContactPoint.Part.BLADE, crossing.onTarget().part(), "blade on blade, not blade on body");
        assertEquals(crossing.onActor().height(), crossing.onTarget().height(),
                "two blades cannot cross at two different heights");

        // And the instant both skeletons synchronise to is the contact moment of
        // the beat that contains them -- the attacker's beat, because it is the
        // attacker's gesture the defender is answering.
        CombatEvent.EnemyBeatBegan beat =
                Encounters.firstOf(r.events(), CombatEvent.EnemyBeatBegan.class);
        assertNotNull(beat);
        assertEquals(bulwark.id(), beat.actor());
        assertEquals(Intent.Kind.ATTACK, beat.kind());
        assertEquals(beat.phases().contactStart(), crossing.at(),
                "the crossing lands on the contact moment of the beat both bodies share");
        assertTrue(crossing.at() > 0 && crossing.at() < Phases.WHOLE,
                "and inside the beat, with anticipation before it and follow-through after");
    }

    @Test
    void theDefendersOwnBeatIsShapedAsAGuardAndTheAttackersAsAStrike() {
        // The other half of "synchronise two skeletons": they are running different
        // gestures. If both were staged as strikes, the defender would gather for
        // 40% of a beat that is mostly about giving ground.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, PARRY);
        e.apply(Command.add(0));
        Resolution r = e.apply(Command.execute());

        CombatEvent.BeatBegan raised =
                Encounters.firstOf(Encounters.phrase(r), CombatEvent.BeatBegan.class);
        assertEquals(TileType.PARRY, raised.tile().type());
        assertEquals(Phases.GUARD, raised.phases());

        CombatEvent.EnemyBeatBegan answered =
                Encounters.firstOf(r.events(), CombatEvent.EnemyBeatBegan.class);
        assertEquals(Phases.STRIKE, answered.phases());
        assertNotEquals(raised.phases(), answered.phases(),
                "one body is gathering and the other is arriving; they are not the same beat");
    }

    @Test
    void aGuardWithNoCounterArmedIsStillAPlaceTwoBladesTouch() {
        // GuardHeld is a negation in the rules and a contact in the drawing. It gets
        // the same crossing as a parry, because physically it is one -- what differs
        // is that nothing answers.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT);
        Combatant hero = e.state().hero();
        hero.statuses().apply(Status.GUARD, 1);

        Resolution r = e.apply(Command.hold());

        CombatEvent.GuardHeld held = Encounters.firstOf(r.events(), CombatEvent.GuardHeld.class);
        assertNotNull(held);
        assertTrue(held.meeting().agrees());
        assertEquals(hero.id(), held.meeting().onTarget().body());
        assertEquals(ContactPoint.Part.BLADE, held.meeting().onTarget().part());
    }

    @Test
    void aBladeFromReachTwoCrossesLowerThanOneFromArmsLength() {
        // The only thing on a one-dimensional lane that can genuinely vary a
        // contact's height is how far the stroke travelled, and the design spends a
        // whole archetype on that distance (combat-design.md 2.4: "Respect 2 tiles
        // of reach, so closing is not free"). A Reacher's deflection reading exactly
        // like a Wisp's would waste it.
        // The Bulwark stands at arm's length and strikes every turn; the Reacher
        // starts a tile further out, walks in on the turn the Parry is banked, and
        // strikes from two tiles on the turn it is spent.
        Meeting close = crossingOf(parried(EnemyArchetype.BULWARK, 1));
        Meeting reach = crossingOf(parried(EnemyArchetype.REACHER, 3));
        assertEquals(ContactPoint.Height.HIGH, close.onActor().height(), "adjacent, it comes down");
        assertEquals(ContactPoint.Height.MIDDLE, reach.onActor().height(), "from two tiles, it comes in flat");
    }

    // -- the shove -------------------------------------------------------------

    @Test
    void aShoveIntoABodySquaredUpAtYouIsShoulderToChest() {
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant hero = e.state().hero();
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));

        CombatEvent.Shoved shoved =
                Encounters.firstOf(Encounters.phrase(e.apply(Command.execute())), CombatEvent.Shoved.class);

        Meeting check = shoved.meeting();
        assertEquals(hero.id(), check.onActor().body());
        assertEquals(ContactPoint.Part.SHOULDER, check.onActor().part(),
                "combat-design.md 2.1's shoulder-and-hilt check, taken at the shoulder");
        assertEquals(wisp.id(), check.onTarget().body());
        assertEquals(ContactPoint.Part.TORSO, check.onTarget().part());
        assertEquals(ContactPoint.Side.LEADING, check.onTarget().side(), "it saw this coming");
        assertEquals(ContactPoint.Height.HIGH, check.onTarget().height());
        assertTrue(check.agrees());
    }

    @Test
    void aShoveIntoATurnedBackIsTheHiltInstead() {
        // The other half of the same named beat, and the branch that decides it is
        // driven by the board rather than by a constant.
        //
        // The facing is set directly here because no sequence of legal commands can
        // currently produce it: a Charted Shadow re-faces the hero for free every
        // single turn, which combat-design.md 3d.3 records as an open tuning item
        // and the one rule the engine's author would change first. The moment an
        // enemy has to spend its step to turn -- which is exactly what 3d.3
        // proposes, and what would give flanking real value -- shoving a body from
        // behind becomes ordinary play. The rule is derived from state either way,
        // so it is right now and reachable later.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        e.apply(Command.add(0));
        wisp.facing(Facing.RIGHT);

        CombatEvent.Shoved shoved =
                Encounters.firstOf(Encounters.phrase(e.apply(Command.execute())), CombatEvent.Shoved.class);

        Meeting check = shoved.meeting();
        assertEquals(ContactPoint.Part.HILT, check.onActor().part());
        assertEquals(ContactPoint.Part.BACK, check.onTarget().part());
        assertEquals(ContactPoint.Side.TRAILING, check.onTarget().side(), "it did not");
        assertEquals(ContactPoint.Height.MIDDLE, check.onTarget().height(),
                "lower and heavier than a chest-to-chest check");
    }

    @Test
    void aShoveThatMovesNobodyStillNamesWhereTheTwoBodiesMet() {
        // The best beat in the game is a shove that changes nothing in the state
        // (CombatEvent's own note), so it is the one that most needs a contact
        // point: there are no trajectories to infer anything from.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(5, Facing.RIGHT)
                .heroHp(20)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .loadout(STEP)
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Shoved shoved = Encounters.firstOf(phrase, CombatEvent.Shoved.class);
        assertNotNull(shoved);
        assertTrue(Encounters.only(phrase, CombatEvent.Moved.class).isEmpty(), "nobody gave ground");
        assertNotNull(shoved.meeting(), "and the contact is still fully described");
        assertTrue(shoved.meeting().agrees());
        assertEquals(ContactPoint.Part.SHOULDER, shoved.meeting().onActor().part());
    }

    // -- the swap and the draw -------------------------------------------------

    @Test
    void aSwapIsSleeveOnSleeveAtMidHeight() {
        // combat-design.md 2.1: "sleeves and hair crossing, each trailing into the
        // space the other has left". The one contact in the game that must be drawn
        // with no impact at all still has a place where the two figures brush.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.PILGRIM)
                .heroAt(2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 4, Facing.LEFT)
                .loadout(STEP)
                .build());
        e.apply(Command.add(0));

        CombatEvent.Swapped swapped =
                Encounters.firstOf(Encounters.phrase(e.apply(Command.execute())), CombatEvent.Swapped.class);

        assertEquals(ContactPoint.Part.ARM, swapped.meeting().onActor().part());
        assertEquals(ContactPoint.Part.ARM, swapped.meeting().onTarget().part());
        assertEquals(ContactPoint.Height.MIDDLE, swapped.meeting().onActor().height());
        assertTrue(swapped.meeting().agrees());
    }

    @Test
    void aDrawHooksABladeIntoABodyThreeTilesAway() {
        // "Contact at distance -- a line of force between two figures"
        // (combat-design.md 2.2). Both ends of that line have to be named, because
        // the near end is a blade held out and the far end is a body being hauled,
        // and an IK chain solving for one is not solving for the other.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .loadout(DRAW)
                .build());
        e.apply(Command.add(0));
        e.apply(Command.hold());

        CombatEvent.Drawn drawn = null;
        for (int turn = 0; turn < 8 && drawn == null; turn++) {
            drawn = Encounters.firstOf(Encounters.phrase(e.apply(Command.execute())), CombatEvent.Drawn.class);
            if (drawn == null) {
                while (!e.can(Command.execute())) {
                    e.apply(e.can(Command.add(0)) ? Command.add(0) : Command.hold());
                }
            }
        }
        assertNotNull(drawn, "the Wisp closed into reach eventually");
        assertEquals(ContactPoint.Part.BLADE, drawn.meeting().onActor().part(), "the near end is the blade");
        assertEquals(ContactPoint.Part.TORSO, drawn.meeting().onTarget().part(), "the far end is the body");
        assertTrue(drawn.meeting().agrees());
    }

    // -- where a blow lands ----------------------------------------------------

    @Test
    void aBlowNamesWhereOnTheStruckBodyTheInkShouldBloom() {
        // STYLE.md 7.3's first item is "a bloom of ink spreading from the contact
        // point through the garment, like a drop hitting wet paper". A bloom that
        // always starts from the same place on the body is a decal.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT);
        Combatant hero = e.state().hero();
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Hit hit = Encounters.firstOf(phrase, CombatEvent.Hit.class);
        assertNotNull(hit.landed());
        assertEquals(bulwark.id(), hit.landed().body(), "the point is on the body that took it");
        assertEquals(ContactPoint.Side.LEADING, hit.landed().side(), "it was squared up at the hero");
        assertEquals(ContactPoint.Height.HIGH, hit.landed().height(), "arm's length, so the stroke came down");
        assertNotEquals(hero.id(), hit.landed().body());
    }

    @Test
    void aBlowFromBehindLandsOnTheTrailingSide() {
        // The distinction the animation layer cannot recover from tile indices: the
        // same damage, the same two tiles, and a completely different yielding --
        // STYLE.md 7.3's "the whole figure gives on a soft IK curve, spine bending".
        // Which way the spine bends is this field.
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(4, Facing.RIGHT)
                .heroHp(40)
                .enemy(EnemyArchetype.WISP, 2, Facing.RIGHT)
                .enemy(EnemyArchetype.WISP, 6, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant hero = e.state().hero();

        CombatEvent.Hit fromBehind = null;
        CombatEvent.Hit fromInFront = null;
        for (int turn = 0; turn < 10 && (fromBehind == null || fromInFront == null); turn++) {
            for (CombatEvent.Hit h : Encounters.only(e.apply(Command.hold()).events(), CombatEvent.Hit.class)) {
                if (h.target() != hero.id() || h.landed() == null) {
                    continue;
                }
                if (h.landed().side() == ContactPoint.Side.TRAILING) {
                    fromBehind = h;
                } else {
                    fromInFront = h;
                }
            }
        }

        assertNotNull(fromInFront, "the body the hero is facing struck its leading side");
        assertNotNull(fromBehind, "and the one behind it struck the other");
        assertEquals(hero.id(), fromBehind.landed().body());
        assertEquals(hero.id(), fromInFront.landed().body());
    }

    @Test
    void inkThrownByABloomArrivesAtTheHemRatherThanTheChest() {
        // A bloom is not a blade and must not be drawn as one. STYLE.md 6 puts the
        // wash blooms in the ground plane and 3 has ink collecting at the bottom of
        // a hanging garment; ink flung across a tile reaches a body low.
        CombatEngine e = CombatEngine.create(Encounters.duel(7, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .enemy(EnemyArchetype.WARDEN, 3, Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, 4, Facing.LEFT)
                .loadout(Tile.of(TileType.THRUST))
                .build());
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.Hit bloom = Encounters.only(phrase, CombatEvent.Hit.class).stream()
                .filter(h -> h.source() == CombatEvent.HitSource.BLOOM).findFirst().orElse(null);
        assertNotNull(bloom, "the Explosive body threw ink across its neighbour");
        assertEquals(ContactPoint.Height.LOW, bloom.landed().height());
        assertEquals(ContactPoint.Part.TORSO, bloom.landed().part());
    }

    @Test
    void aSeepingTickHasNoContactPointBecauseNothingTouchedTheBody() {
        // The same distinction HitSource#isAttack() already draws for Guard and
        // Marked, seen from the animation side: a raised brushstroke stands between
        // body and blade, and a wound already bleeding is behind it. There is no
        // contact to bloom from, and inventing one would put a fresh stain on a body
        // nothing struck.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        Combatant hero = e.state().hero();
        hero.statuses().apply(Status.SEEPING, Statuses.DURATION);

        Resolution r = e.apply(Command.hold());

        CombatEvent.Hit tick = Encounters.only(r.events(), CombatEvent.Hit.class).stream()
                .filter(h -> h.source() == CombatEvent.HitSource.SEEPING).findFirst().orElse(null);
        assertNotNull(tick);
        assertNull(tick.landed(), "nothing touched it");
        assertEquals(-1, tick.attacker());
    }

    // -- fixtures --------------------------------------------------------------

    private static CombatEngine parried(EnemyArchetype archetype, int startTile) {
        CombatEngine e = CombatEngine.create(Encounters.duel(9, Hero.WARDEN)
                .heroAt(0, Facing.RIGHT)
                .heroHp(40)
                .enemy(archetype, startTile, Facing.LEFT)
                .loadout(PARRY)
                .build());
        e.apply(Command.add(0));
        return e;
    }

    private static Meeting crossingOf(CombatEngine e) {
        Resolution r = e.apply(Command.execute());
        CombatEvent.BladeMet met = Encounters.firstOf(r.events(), CombatEvent.BladeMet.class);
        assertNotNull(met, "the parry was never tested:\n" + r.events());
        return met.meeting();
    }
}
