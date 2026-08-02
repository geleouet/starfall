package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md 7.0.3, and the debt it left open.
 *
 * <blockquote>Nothing may arrive at the same time. 10's last row bans everything
 * peaking on the same frame, and it applies <em>within</em> a chain, not only
 * between body and cloth. The wrist should arrive after the elbow; the blade tip
 * should drift a beat after the hand has stopped. This is the cheapest poetry
 * available and it is almost free -- it requires only that settle times differ
 * down the chain rather than being shared.</blockquote>
 *
 * <p>{@code system2-debt.md} E3 measured the failure and named the fix: "pelvis
 * shortest settle, then spine, shoulder, elbow, wrist, tip, spread across
 * 0.3-0.6 s". Nothing owned those numbers -- {@code IkChain} provides the knobs
 * and declines to choose values, and {@code RigIk} chose a set for one gesture.
 * The staging layer owns them, so this is where they are pinned.
 */
class ChainSettleTest {

    @Test
    void everyChainArrivesLinkByLinkAndNeverAsAUnit() {
        for (Chain chain : Chain.values()) {
            Settle s = chain.settle();
            assertEquals(chain.bones().size(), s.links(),
                    chain + " has " + chain.bones().size() + " links and " + s.links() + " arrivals");
            for (int i = 1; i < s.links(); i++) {
                assertTrue(s.arrival(i) > s.arrival(i - 1),
                        chain + " link " + i + " arrives with link " + (i - 1));
            }
        }
    }

    @Test
    void everyArrivalSitsInsideStyleSevenPointOnesBand() {
        // "Terminal damping should let a limb settle over 0.3-0.6 s after the
        // visible motion has 'ended'." An arrival outside that is not a stagger, it
        // is a different rule being broken.
        for (Chain chain : Chain.values()) {
            for (int i = 0; i < chain.settle().links(); i++) {
                double a = chain.settle().arrival(i);
                assertTrue(a >= Timing.SETTLE_ROOT - 1e-9 && a <= Timing.SETTLE_TIP + 1e-9,
                        chain + " link " + i + " settles at " + a);
            }
        }
    }

    @Test
    void theSwordSideRunIsStrictlyIncreasingFromThePelvisToTheBladeTip() {
        // The run STYLE.md 7.0.3 is actually about crosses three chains, so checking
        // each chain in isolation would pass a rig whose clavicle arrived before its
        // chest. Laid end to end there must be no repeat and no reversal.
        List<Double> run = new ArrayList<>();
        for (Chain chain : Chain.SWORD_SIDE) {
            for (int i = 0; i < chain.settle().links(); i++) {
                run.add(chain.settle().arrival(i));
            }
        }
        assertEquals(8, run.size(), "the pelvis-to-tip run should be eight links");
        for (int i = 1; i < run.size(); i++) {
            assertTrue(run.get(i) > run.get(i - 1),
                    "link " + i + " of the sword-side run arrives at " + run.get(i)
                            + ", not after " + run.get(i - 1));
        }
        assertEquals(Timing.SETTLE_ROOT, run.get(0), 1e-9, "the run should start on the band's floor");
        assertEquals(Timing.SETTLE_TIP, run.get(run.size() - 1), 1e-9,
                "and end on its ceiling, so the band is spent rather than sampled");
    }

    @Test
    void noTwoLinksAnywhereInTheRigShareAnArrival() {
        // The blunt form of 10's last row: count distinct arrival times. Every link
        // in the whole rig, legs included.
        TreeSet<Double> distinct = new TreeSet<>();
        int links = 0;
        for (Chain chain : Chain.values()) {
            for (int i = 0; i < chain.settle().links(); i++) {
                distinct.add(chain.settle().arrival(i));
                links++;
            }
        }
        assertEquals(links, distinct.size(),
                "two links share an arrival time: " + distinct);
    }

    @Test
    void aProfileThatArrivesAsAUnitIsRefusedAtConstruction() {
        // The guard has to be in the type, not in a review. A single shared settle
        // down a chain is exactly what pass 3 was failed for, and it is the easiest
        // thing in the world to reintroduce by writing one number.
        assertThrows(IllegalArgumentException.class, () -> Settle.of(0.4, 0.4, 0.5));
        assertThrows(IllegalArgumentException.class, () -> Settle.of(0.5, 0.4));
        assertThrows(IllegalArgumentException.class, () -> Settle.of(0.2, 0.4));
        assertThrows(IllegalArgumentException.class, () -> Settle.of(0.4, 0.7));
    }

    @Test
    void everyIkDirectiveInARealScheduleCarriesAStaggeredProfile() {
        CombatEngine engine = Fixtures.lane(11, Hero.WARDEN, EnemyArchetype.REACHER, 3,
                Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.PARRY),
                Tile.of(TileType.SWEEP), Tile.of(TileType.TURN));
        Schedule s = Fixtures.stanza(engine, 5);
        List<Directive.IkTarget> ik = s.of(Directive.IkTarget.class);
        assertTrue(ik.size() > 20, "a five-beat turn produced only " + ik.size() + " IK directives");
        for (Directive.IkTarget d : ik) {
            assertEquals(d.chain().settle(), d.settle(),
                    "an IK directive carried a profile that is not its chain's");
            Settle p = d.settle();
            for (int i = 1; i < p.links(); i++) {
                assertTrue(p.lag(i) > p.lag(i - 1), d.describe() + " arrives as a unit");
            }
        }
    }

    @Test
    void theTwoKnobsIkChainExposesAreBothDerivable() {
        // IkChain has settleSeconds (the whole chain) and boneLagSeconds (one bone
        // behind its parent), and the difference between them is the difference
        // between the thing 7.0.3 bans and the thing it asks for. A profile has to
        // give both without the renderer doing arithmetic.
        Settle arm = Chain.SWORD_ARM.settle();
        assertEquals(0.48, arm.base(), 1e-9);
        assertEquals(0.0, arm.lag(0), 1e-9, "the root link carries no extra lag by definition");
        assertEquals(0.05, arm.lag(1), 1e-9);
        assertEquals(0.12, arm.tip() - arm.base(), 1e-9,
                "the tip should trail the shoulder by about seven frames at 60 Hz");
    }

    @Test
    void theClothAndHairLagsBothNameTheirAnchorAndBothReadingsAreAvailable() {
        // STYLE.md 7.1: "a lag figure quoted without its anchor is unfalsifiable.
        // State it every time." And: "both readings are defensible and they differ
        // by a factor of three", which is the number checked here.
        assertEquals(LagAnchor.HIPS, Region.CLOTH_HEM.anchor());
        assertEquals(LagAnchor.WRIST, Region.CLOTH_SLEEVE.anchor());
        assertEquals(LagAnchor.HEAD, Region.HAIR.anchor());

        // Against its own anchor, every lag sits in 7.1's stated bands: cloth 4-8
        // frames, hair 8-14.
        assertBetween(Region.CLOTH_HEM.lag(), 4 / 60.0, 8 / 60.0, "hem behind hips");
        assertBetween(Region.CLOTH_SLEEVE.lag(), 4 / 60.0, 8 / 60.0, "sleeve behind wrist");
        assertBetween(Region.HAIR.lag(), 8 / 60.0, 14 / 60.0, "hair behind head");

        // Against the hips, the sleeve reads three and a half times larger, which is
        // exactly the ambiguity 7.1 warns a review about.
        double ratio = Region.CLOTH_SLEEVE.netLagBehindHips() / Region.CLOTH_SLEEVE.lag();
        assertTrue(ratio > 3.0 && ratio < 4.5,
                "the two readings of the sleeve's lag differ by " + ratio + "x");
        assertEquals(Region.CLOTH_HEM.lag(), Region.CLOTH_HEM.netLagBehindHips(), 1e-9,
                "the hem's anchor is the hips, so its two readings are the same number");
    }

    private static void assertBetween(double v, double lo, double hi, String what) {
        assertTrue(v >= lo - 1e-9 && v <= hi + 1e-9, what + " is " + v + ", outside " + lo + ".." + hi);
    }
}
