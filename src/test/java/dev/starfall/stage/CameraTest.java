package dev.starfall.stage;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.Focus;
import dev.starfall.combat.Hero;
import dev.starfall.combat.Lane;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md 9, and combat-design.md 1.6's instruction that the wide shot comes from
 * the lane.
 *
 * <blockquote>The planning framing must fit the lane, so a 5-tile lane is already
 * near-intimate while a 15-tile lane is genuinely wide. The push-in is therefore a
 * <em>small</em> move on short lanes and a <em>large</em> one on long lanes --
 * which is correct, because a short lane has no approach to dramatise and a long
 * one has nothing but. The camera should derive its wide framing from lane length
 * rather than using a fixed value.</blockquote>
 */
class CameraTest {

    @Test
    void theWideShotGrowsWithTheLaneAndTheCloseShotDoesNot() {
        double previous = 0;
        for (int lane = Lane.MIN_LENGTH; lane <= Lane.MAX_LENGTH; lane++) {
            Stage stage = new Stage(lane);
            Framing plan = stage.planning();
            assertTrue(plan.widthTiles() > previous,
                    "the planning framing did not widen going from lane " + (lane - 1) + " to " + lane);
            previous = plan.widthTiles();

            assertTrue(plan.left() <= 0.0 && plan.right() >= lane - 1,
                    "lane " + lane + "'s planning framing does not hold the whole lane: " + plan);

            // The exchange is the same shot on every lane. A duel does not become
            // less intimate because the corridor behind it is longer.
            Framing close = stage.execution(new Focus(0, 1, 1));
            assertEquals(Stage.INTIMACY_TILES, close.widthTiles(), 1e-9,
                    "the execution framing drifted off images 3/4/5's intimacy on lane " + lane);
        }
    }

    @Test
    void thePushInIsSmallOnAKnifeFightAndLargeOnAnApproach() {
        double shortLane = new Stage(5).pushIn();
        double longLane = new Stage(15).pushIn();
        assertTrue(shortLane > 1.5 && shortLane < 2.5,
                "a five-tile lane is already near-intimate; its push-in measures " + shortLane + "x");
        assertTrue(longLane > 4.5,
                "a fifteen-tile lane should be genuinely wide; its push-in measures " + longLane + "x");
        assertTrue(longLane > 2.4 * shortLane,
                "the push-in barely scales with lane length: " + shortLane + "x against " + longLane + "x");
    }

    @Test
    void aBeatWiderThanTheIntimateShotOpensUpToHoldIt() {
        // combat-design.md 3d.5 correction 1: "a Runner collapsing thirteen tiles and
        // a Wisp stepping one are the same subject and completely different shots."
        // A single tile could not drive that, which is why Focus carries a span.
        Stage stage = new Stage(15);
        Framing step = stage.execution(new Focus(0, 7, 7));
        Framing charge = stage.execution(new Focus(0, 2, 13));
        assertEquals(Stage.INTIMACY_TILES, step.widthTiles(), 1e-9);
        assertTrue(charge.widthTiles() > step.widthTiles() * 3,
                "a twelve-tile collapse is framed like a step: " + charge);
        assertTrue(charge.widthTiles() <= stage.planning().widthTiles(),
                "and it must never open wider than the planning shot");
    }

    @Test
    void theCameraNeverCuts() {
        // STYLE.md 9: "Never cut. Never shake. All camera movement is eased and
        // continuous." A cut is a discontinuity between consecutive framings, and
        // that is exactly what this checks -- endpoints, not taste.
        for (int lane : new int[]{5, 9, 15}) {
            Schedule s = execution(lane);
            assertTrue(s.cameraIsContinuous(), "lane " + lane + " camera jumps:\n" + s.fingerprint());
            List<Directive.CameraKey> keys = s.camera();
            assertTrue(keys.size() >= 3, "lane " + lane + " produced only " + keys.size() + " camera keys");
            for (Directive.CameraKey k : keys) {
                assertTrue(k.duration() >= Timing.CAMERA_MIN_MOVE - 1e-9,
                        "a " + k.duration() + " s camera move is a cut by another name: " + k.describe());
                assertTrue(k.ease() != Ease.LINEAR, "every camera move is eased: " + k.describe());
            }
        }
    }

    @Test
    void theCameraIsNeverPerfectlyStill() {
        // STYLE.md 9's planning note. Every key moves the frame somewhere, including
        // the ones whose beat is over the same tiles as the last -- a stanza of Cuts
        // from one place is the ordinary way a shot would otherwise freeze.
        Schedule s = execution(11);
        for (Directive.CameraKey k : s.camera()) {
            double moved = Math.abs(k.to().centreTile() - k.from().centreTile())
                    + Math.abs(k.to().widthTiles() - k.from().widthTiles());
            assertTrue(moved > 1e-6, "a camera key holds perfectly still: " + k.describe());
        }
        // And there is no gap in the coverage, so "still" cannot hide between keys.
        List<Directive.CameraKey> keys = s.camera();
        for (int i = 1; i < keys.size(); i++) {
            assertTrue(keys.get(i).at() <= keys.get(i - 1).end() + 1e-6,
                    "the camera holds from " + keys.get(i - 1).end() + " to " + keys.get(i).at());
        }
    }

    @Test
    void theReturnIsSlowerThanThePushInOnEveryLane() {
        // STYLE.md 9: "on queue execution, the camera glides toward the exchange over
        // ~0.5 s"; "return is slower than the push-in (~0.8 s)". Both scale with how
        // far the camera has to come -- see Stage#pushInSeconds for why 9's fixed
        // number could not be taken literally on a fifteen-tile lane -- and the
        // relation between them holds on every lane, which is the part 9 states as a
        // rule rather than as a number.
        for (int lane : new int[]{5, 9, 11, 15}) {
            Stage stage = new Stage(lane);
            Schedule s = execution(lane);
            Directive.CameraKey pushIn = s.camera().stream()
                    .filter(k -> k.reason() == Directive.CameraReason.PUSH_IN)
                    .findFirst().orElseThrow(() -> new AssertionError("no push-in on lane " + lane));
            Directive.CameraKey ret = s.camera().stream()
                    .filter(k -> k.reason() == Directive.CameraReason.RETURN)
                    .findFirst().orElseThrow(() -> new AssertionError("no return on lane " + lane));

            assertEquals(stage.pushInSeconds(), pushIn.duration(), 1e-9);
            assertEquals(stage.returnSeconds(), ret.duration(), 1e-9);
            assertTrue(ret.duration() > pushIn.duration(),
                    "lane " + lane + ": the return is not slower than the push-in");
            assertTrue(pushIn.from().widthTiles() > pushIn.to().widthTiles(),
                    "the push-in does not get closer: " + pushIn.describe());
            assertTrue(ret.to().widthTiles() > ret.from().widthTiles(),
                    "the return does not get wider: " + ret.describe());
        }
        assertEquals(Timing.PUSH_IN, new Stage(5).pushInSeconds(), 1e-9,
                "the shortest lane keeps STYLE.md 9's own number exactly");
        assertEquals(Timing.RETURN, new Stage(5).returnSeconds(), 1e-9);
    }

    @Test
    void theFramingIsAContinuousFunctionOfTime() {
        // The endpoint check above proves the keys join; this proves the curve
        // between them has no step, which is what a reviewer actually sees.
        Schedule s = execution(15);
        double previousCentre = s.framingAt(0).centreTile();
        double previousWidth = s.framingAt(0).widthTiles();
        for (double t = 0; t < s.duration(); t += 1.0 / 120.0) {
            Framing f = s.framingAt(t);
            assertTrue(Math.abs(f.centreTile() - previousCentre) < 0.35,
                    "the frame jumped " + Math.abs(f.centreTile() - previousCentre)
                            + " tiles in one 120 Hz step at t=" + t);
            assertTrue(Math.abs(f.widthTiles() - previousWidth) < 0.35,
                    "the zoom jumped " + Math.abs(f.widthTiles() - previousWidth)
                            + " tiles of width in one 120 Hz step at t=" + t);
            previousCentre = f.centreTile();
            previousWidth = f.widthTiles();
        }
    }

    private static Schedule execution(int lane) {
        CombatEngine engine = Fixtures.lane(lane, Hero.WARDEN, EnemyArchetype.WISP,
                Math.min(lane - 1, 4), Tile.of(TileType.CUT), Tile.of(TileType.STEP),
                Tile.of(TileType.SWEEP));
        return Fixtures.stanza(engine, 3);
    }
}
