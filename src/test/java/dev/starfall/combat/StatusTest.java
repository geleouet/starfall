package dev.starfall.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four statuses of combat-design.md 1.4: two durations and two flags.
 *
 * <p>Durations are measured rather than asserted at a single point -- a status
 * that lasts two turns or four turns passes a "does it apply" test perfectly
 * well, and off-by-one is the entire failure mode here. So Seeping is counted in
 * ticks and total damage, and Stillness is counted in enemy phases actually
 * missed.
 *
 * <p>The interactions are where the real decisions live, and two of them are
 * choices this engine made rather than rules it inherited: Guard resolves before
 * Marked, and neither of them applies to a Seeping tick.
 */
class StatusTest {

    private static final Tile CUT = Tile.of(TileType.CUT);
    private static final Tile PARRY = Tile.of(TileType.PARRY);
    private static final Tile SEEPING_CUT = Tile.of(TileType.CUT, Enchantment.SEEPING);
    private static final Tile STILLNESS_CUT = Tile.of(TileType.CUT, Enchantment.STILLNESS);
    private static final Tile MARKING_CUT = Tile.of(TileType.CUT, Enchantment.MARKING);

    // -- durations -------------------------------------------------------------

    @Test
    void seepingDealsExactlyOneDamageForExactlyThreeTurns() {
        // A Bulwark, thirteen tiles away: five hit points survive three of bleeding,
        // so the count is not cut short by a death, and it is much too far off to
        // interfere with the measurement.
        CombatEngine e = CombatEngine.create(Encounters.duel(15, Hero.WARDEN)
                .enemy(EnemyArchetype.BULWARK, 14, Facing.LEFT)
                .loadout(CUT)
                .build());
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.BULWARK);
        wisp.statuses().apply(Status.SEEPING, Statuses.DURATION);
        int hp = wisp.hp();

        int ticks = 0;
        int expiries = 0;
        for (int turn = 0; turn < 6; turn++) {
            Resolution r = e.apply(Command.hold());
            ticks += Encounters.only(r.events(), CombatEvent.StatusTicked.class).stream()
                    .filter(t -> t.status() == Status.SEEPING).toList().size();
            expiries += Encounters.only(r.events(), CombatEvent.StatusExpired.class).stream()
                    .filter(t -> t.status() == Status.SEEPING).toList().size();
        }

        assertEquals(3, ticks, "three ticks, not two and not four");
        assertEquals(1, expiries);
        assertEquals(hp - 3, wisp.hp(), "one damage per tick, and then it stops bleeding");
        assertEquals(0, wisp.statuses().seeping());
    }

    @Test
    void stillnessCostsExactlyThreeEnemyPhases() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, STILLNESS_CUT);
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));

        // Turn 2 strikes and freezes it; the enemy phase of that same turn is
        // already missed, which is the correct reading of "immobilised 3 turns"
        // when the status lands during the player's half of the turn.
        Resolution frozen = e.apply(Command.execute());
        CombatEvent.StatusApplied applied =
                Encounters.firstOf(frozen.events(), CombatEvent.StatusApplied.class);
        assertNotNull(applied);
        assertEquals(Statuses.DURATION, applied.turns(), "three turns are written on the body");
        assertEquals(Statuses.DURATION - 1, bulwark.statuses().stillness(),
                "and one of them has already been spent by the end of the turn it landed on");

        int missed = Encounters.has(frozen.events(), CombatEvent.Immobilised.class) ? 1 : 0;
        boolean actedWhileFrozen = false;
        for (int i = 0; i < 3; i++) {
            Resolution r = e.apply(Command.hold());
            if (Encounters.has(r.events(), CombatEvent.Immobilised.class)) {
                missed++;
                if (Encounters.has(r.events(), CombatEvent.Swung.class)) {
                    actedWhileFrozen = true;
                }
            }
        }
        assertEquals(3, missed, "three enemy phases lost");
        assertFalse(actedWhileFrozen, "a body with dried pigment must not also swing");
        assertEquals(0, bulwark.statuses().stillness());
    }

    @Test
    void aDurationRefreshesRatherThanStacking() {
        // Otherwise a status-enchanted tile scales with queue length, which is
        // arithmetic rather than choreography. See Statuses' class note.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        Combatant wisp = Encounters.enemy(e, EnemyArchetype.WISP);
        wisp.statuses().apply(Status.SEEPING, Statuses.DURATION);
        e.apply(Command.hold());
        assertEquals(2, wisp.statuses().seeping());

        wisp.statuses().apply(Status.SEEPING, Statuses.DURATION);
        assertEquals(3, wisp.statuses().seeping(), "refreshed to three, not extended to five");
    }

    // -- flags -----------------------------------------------------------------

    @Test
    void markedDoublesTheNextHitAndIsThenSpent() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT, MARKING_CUT);
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        int hp = bulwark.hp();

        e.apply(Command.add(0)); // plain Cut, will resolve second
        e.apply(Command.add(1)); // marking Cut, sits on top and resolves first
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        List<CombatEvent.Hit> hits = Encounters.only(phrase, CombatEvent.Hit.class);
        assertEquals(2, hits.size());
        assertFalse(hits.get(0).doubled());
        assertEquals(1, hits.get(0).amount());
        assertTrue(hits.get(1).doubled(), "the second stroke lands on the seal");
        assertEquals(2, hits.get(1).amount());
        assertEquals(hp - 3, bulwark.hp());
        assertFalse(bulwark.statuses().marked(), "and the seal is spent, not permanent");
    }

    @Test
    void guardNegatesTheNextAttackEntirely() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT);
        Combatant hero = e.state().hero();
        hero.statuses().apply(Status.GUARD, 1);
        int hp = hero.hp();

        Resolution r = e.apply(Command.hold());

        assertTrue(Encounters.has(r.events(), CombatEvent.GuardHeld.class));
        assertEquals(hp, hero.hp(), "a plain guard costs the attacker the whole blow");
        assertFalse(hero.statuses().guard(), "and is spent doing it");
    }

    @Test
    void guardResolvesBeforeMarkedSoANegatedBlowDoesNotEatTheSeal() {
        // The decision documented on CombatEngine#damage. Marked doubles the next
        // hit *taken*, and an attack a guard stood in front of was not taken. The
        // other order would quietly cost the player a seal for free, and would read
        // as a bug at the table.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, CUT);
        Combatant hero = e.state().hero();
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        hero.statuses().apply(Status.GUARD, 1);
        hero.statuses().apply(Status.MARKED, 1);
        int hp = hero.hp();

        Resolution first = e.apply(Command.hold());
        assertTrue(Encounters.has(first.events(), CombatEvent.GuardHeld.class));
        assertEquals(hp, hero.hp());
        assertTrue(hero.statuses().marked(), "the seal survives a blow that never landed");

        Resolution second = e.apply(Command.hold());
        CombatEvent.Hit hit = Encounters.firstOf(second.events(), CombatEvent.Hit.class);
        assertNotNull(hit);
        assertTrue(hit.doubled());
        assertEquals(bulwark.damage() * 2, hit.amount());
        assertEquals(hp - bulwark.damage() * 2, hero.hp());
    }

    @Test
    void neitherGuardNorMarkedTouchesASeepingTick() {
        // A raised brushstroke stands between body and blade; a wound that is
        // already bleeding is behind it. Same reasoning for the seal: a tick is
        // damage, not a hit taken.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, CUT);
        Combatant hero = e.state().hero();
        hero.statuses().apply(Status.SEEPING, Statuses.DURATION);
        hero.statuses().apply(Status.GUARD, 1);
        hero.statuses().apply(Status.MARKED, 1);
        int hp = hero.hp();

        e.apply(Command.hold());

        assertEquals(hp - 1, hero.hp(), "one damage, not zero and not two");
        assertTrue(hero.statuses().guard(), "the guard is still standing");
        assertTrue(hero.statuses().marked(), "and the seal is still unbroken");
    }

    // -- parry -----------------------------------------------------------------

    @Test
    void aParryDeflectsAndAnswers() {
        // The signature beat of STYLE.md 7.2. It is a Guard with a counter armed,
        // so the negation and the answer are one status rather than two systems,
        // and the stream distinguishes them: BladeMet rather than GuardHeld.
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, PARRY);
        Combatant hero = e.state().hero();
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));
        int heroHp = hero.hp();
        int bulwarkHp = bulwark.hp();

        Resolution r = e.apply(Command.execute());

        assertTrue(Encounters.has(r.events(), CombatEvent.BladeMet.class),
                "blade on blade, not a shield absorbing a blow");
        assertFalse(Encounters.has(r.events(), CombatEvent.GuardHeld.class));
        CombatEvent.Countered countered = Encounters.firstOf(r.events(), CombatEvent.Countered.class);
        assertNotNull(countered);
        assertEquals(CombatEngine.COUNTER_DAMAGE, countered.damage());
        assertEquals(heroHp, hero.hp(), "the deflection costs nothing");
        assertEquals(bulwarkHp - CombatEngine.COUNTER_DAMAGE, bulwark.hp(), "and answers");
    }

    @Test
    void aParryThatIsNeverTestedSimplyKeepsStanding() {
        // Parry is reactive, so a parry into an empty turn is not wasted -- the
        // guard waits. This is what makes banking one at the top of a stanza a real
        // decision rather than a read on this exact turn.
        CombatEngine e = Encounters.quietLane(Hero.WARDEN, PARRY);
        e.apply(Command.add(0));
        e.apply(Command.execute());
        assertTrue(e.state().hero().statuses().guard());
        assertTrue(e.state().hero().statuses().counterArmed());
        e.apply(Command.hold());
        assertTrue(e.state().hero().statuses().guard(), "nothing tested it, so nothing spent it");
    }

    // -- enchantments applying statuses ----------------------------------------

    @Test
    void anEnchantmentAppliesItsStatusOnlyWhenTheStrikeLands() {
        CombatEngine e = Encounters.againstBulwark(Hero.WARDEN, SEEPING_CUT);
        Combatant bulwark = Encounters.enemy(e, EnemyArchetype.BULWARK);
        e.apply(Command.add(0));
        List<CombatEvent> phrase = Encounters.phrase(e.apply(Command.execute()));

        CombatEvent.StatusApplied applied = Encounters.firstOf(phrase, CombatEvent.StatusApplied.class);
        assertNotNull(applied);
        assertEquals(Status.SEEPING, applied.status());
        assertEquals(bulwark.id(), applied.entity());

        // Now the same tile into empty air: charges are still spent, but nothing is
        // seeping, because there was nothing to bleed.
        CombatEngine miss = Encounters.quietLane(Hero.WARDEN, SEEPING_CUT);
        miss.apply(Command.add(0));
        List<CombatEvent> missed = Encounters.phrase(miss.apply(Command.execute()));
        assertTrue(Encounters.has(missed, CombatEvent.Whiffed.class));
        assertTrue(Encounters.only(missed, CombatEvent.StatusApplied.class).isEmpty());
        assertTrue(Encounters.has(missed, CombatEvent.TileSpent.class));
    }

    @Test
    void enchantmentsCostCooldownInProportionToWhatTheyBuy() {
        assertEquals(TileType.CUT.baseCooldown() + Enchantment.SEEPING.cooldownPenalty(), SEEPING_CUT.cooldown());
        assertTrue(STILLNESS_CUT.cooldown() > SEEPING_CUT.cooldown(),
                "three turns of immobility is worth more than three turns of bleeding");
        assertEquals(1, Tile.of(TileType.CUT, Enchantment.PERFECT_STRIKE).cooldown() - CUT.cooldown());
    }

    @Test
    void aTileCarriesAtMostOneEnchantment() {
        Tile once = Tile.of(TileType.CUT, Enchantment.SEEPING);
        Tile again = Tile.of(TileType.CUT, Enchantment.MARKING);
        assertEquals(Enchantment.MARKING, again.enchantment());
        assertEquals(Enchantment.SEEPING, once.enchantment());
    }
}
