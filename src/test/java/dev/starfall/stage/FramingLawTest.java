package dev.starfall.stage;

import dev.starfall.direct.Duel;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The planning framing, and the schedules it is not allowed to move.
 *
 * <p>{@link Stage#planning(Standing)} is the framing law three System 5 reviews
 * asked for and two declined. It changes what the camera looks at; it must not
 * change what the score <em>is</em>, because the pass-1 review's protected result
 * for this layer is three byte-identical System 4 schedule fingerprints.
 */
class FramingLawTest {

    /**
     * <b>Guard.</b> System 4's three schedules keep their durations across the
     * framing law.
     *
     * <p>The pass-2 review's brief: <i>"Guard the System 4 schedules with the three
     * duration fingerprints, which cost nothing."</i> These are the numbers pass 1
     * published and pass 2's review re-measured and reproduced -- {@code PHRASE}
     * 6.9521 s, {@code PARRY} 3.0400 s, {@code KNOCKBACK} 3.1548 s.
     *
     * <p>They are the reason the duration law still reads {@link Stage#planning()}
     * rather than the exchange's framing: {@link Stage#pushInSeconds()} scales with
     * how far the camera has to come <em>on this lane</em>, which is a fact about the
     * lane and not about where two bodies happen to be standing. Had the new framing
     * fed the duration law, all three of these would have moved.
     *
     * <p><b>Observed red</b> during authoring, in exactly that way: pointing
     * {@code pushIn()} at {@code planning(standing)} takes {@code PHRASE} to
     * 6.8410 s and {@code KNOCKBACK} to 3.0932 s, and this assertion prints both.
     */
    @Test
    void systemFoursSchedulesKeepTheirDurationsAcrossTheFramingLaw() {
        assertEquals(6.9521, Duel.of(Duel.Kind.PHRASE).schedule().duration(), 5e-5,
                "duel-phrase's duration is System 4's published fingerprint and the framing "
                        + "law is not allowed to move it");
        assertEquals(3.0400, Duel.of(Duel.Kind.PARRY).schedule().duration(), 5e-5,
                "duel-parry's duration is System 4's published fingerprint");
        assertEquals(3.1548, Duel.of(Duel.Kind.KNOCKBACK).schedule().duration(), 5e-5,
                "duel-knockback's duration is System 4's published fingerprint");
    }

    /**
     * <b>Guard.</b> The law only ever tightens the shot, and never below the shot
     * STYLE.md 9 was written about.
     *
     * <p>Both edges of STYLE.md 11.0's "band with both edges", enumerated over every
     * legal lane length and every placement of a two-body exchange on it rather than
     * checked on the one lane the graded bout uses -- which is 11.2b(f)'s
     * <i>enumerate the axis, do not index it</i> applied to a law rather than to a
     * resolution.
     */
    @Test
    void thePlanningFramingIsInsideItsOwnBandOnEveryLaneAndEveryExchange() {
        for (int lane = dev.starfall.combat.Lane.MIN_LENGTH;
                lane <= dev.starfall.combat.Lane.MAX_LENGTH; lane++) {
            Stage stage = new Stage(lane);
            double laneWide = stage.planning().widthTiles();
            for (int hero = 0; hero < lane; hero++) {
                for (int enemy = 0; enemy < lane; enemy++) {
                    if (enemy == hero) {
                        continue;
                    }
                    Standing s = Standing.opening(
                            new Standing.Body(0, hero, dev.starfall.combat.Facing.RIGHT),
                            new Standing.Body(1, enemy, dev.starfall.combat.Facing.LEFT));
                    Framing f = stage.planning(s);
                    assertTrue(f.widthTiles() <= laneWide + 1e-9, String.format(Locale.ROOT,
                            "lane %d, hero at %d, Shadow at %d: the exchange is framed at %.4f "
                                    + "tiles, wider than the lane's own %.4f. The law may only "
                                    + "tighten the shot.",
                            lane, hero, enemy, f.widthTiles(), laneWide));
                    assertTrue(f.widthTiles() >= Math.min(laneWide, Stage.SHORTEST_PLANNING) - 1e-9,
                            String.format(Locale.ROOT,
                            "lane %d, hero at %d, Shadow at %d: the exchange is framed at %.4f "
                                    + "tiles, tighter than the %.4f a knife fight plans in. "
                                    + "Below that floor the planning shot is the execution shot "
                                    + "and 'wide to plan, push in to strike' has nothing to push "
                                    + "in from.",
                            lane, hero, enemy, f.widthTiles(), Stage.SHORTEST_PLANNING));
                    assertTrue(f.left() <= Math.min(hero, enemy) + 1e-9
                                    && f.right() >= Math.max(hero, enemy) - 1e-9,
                            String.format(Locale.ROOT,
                            "lane %d: the shot %.4f..%.4f does not hold the exchange at tiles "
                                    + "%d and %d, which is the one thing it is for.",
                            lane, f.left(), f.right(), hero, enemy));
                }
            }
        }
    }

    /**
     * <b>Guard, and it is the one the composition failure is about.</b> The hero is
     * inside the corpus's own band of figure heights at the planning framing.
     *
     * <p>STYLE.md 11.0 measured on the graded bout: a hero <b>77 px</b> in 720 rows,
     * 0.107 of the frame, against a corpus whose smallest figure -- a Family C
     * background figure in reference image 7 -- is about <b>0.22</b>. A figure below
     * the corpus's own floor is the "still a diagram" verdict stated as a number.
     *
     * <p>The share is pure arithmetic on the framing: the frame holds
     * {@code widthTiles * 1.55} world units across and {@code * 0.75} of that
     * vertically at 4:3, and {@link Stage#FIGURE_HEIGHT} is 1.70 of them.
     */
    @Test
    void thePlanningFramingPutsTheFigureInsideTheCorpussOwnBand() {
        // The corpus's spread, measured on the eight reference images at 832x1088:
        // Family C background figures ~0.22 of frame height, Family B duellists 0.55
        // to 0.65. The floor is the claim; the ceiling is there so that a shot which
        // fills the frame with one body is caught as well.
        double floor = 0.20;
        double ceiling = 0.95;
        // The exchanges the floor framing can hold. Beyond this the exchange itself
        // sets the width -- a Shadow six tiles away is not in an exchange, it is an
        // approach, and combat-design.md 1.6 says a long lane has nothing but the
        // approach. The crossover is stated rather than hidden: at
        // SHORTEST_PLANNING = 6.5 tiles the shot holds a span of 4.
        int servable = (int) Math.floor(Stage.SHORTEST_PLANNING - 2 * Stage.EDGE_MARGIN
                - Stage.TILE_WIDTH);
        for (int lane = dev.starfall.combat.Lane.MIN_LENGTH;
                lane <= dev.starfall.combat.Lane.MAX_LENGTH; lane++) {
            Stage stage = new Stage(lane);
            for (int gap = 1; gap < lane && gap <= servable; gap++) {
                Standing s = Standing.opening(
                        new Standing.Body(0, 0, dev.starfall.combat.Facing.RIGHT),
                        new Standing.Body(1, gap, dev.starfall.combat.Facing.LEFT));
                Framing f = stage.planning(s);
                double share = Stage.FIGURE_HEIGHT / (f.widthTiles() * 1.55 * 0.75);
                assertTrue(share >= floor && share <= ceiling, String.format(Locale.ROOT,
                        "lane %d with the exchange %d tiles apart plans at %.4f tiles, which puts "
                                + "a standing figure at %.4f of the frame height (%.0f px of 720) "
                                + "against the corpus's own band of %.2f..%.2f. STYLE.md 11.0: a "
                                + "material can only ever be as good as the subject it is "
                                + "painting.",
                        lane, gap, f.widthTiles(), share, share * 720, floor, ceiling));
            }
        }
    }
}
