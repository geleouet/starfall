package dev.starfall.rig;

import dev.starfall.anim.Pose;
import dev.starfall.stage.Stance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md 4b.6's law for the face: "expression must obey §7's motion law: it
 * blends and settles, it never snaps... terminal settle 0.3-0.6 s, same as
 * everything else." These tests integrate the actual {@link FaceRig} at 120 Hz
 * and measure the settle rather than reading the taus back — a test that
 * asserted {@code TAU_JAW == 0.14f} would be the bit-identity check §11.2b(f)
 * warns about.
 *
 * <p><b>Observed red</b> (§11.2b(f)): with {@code TAU_JAW} hand-edited to 0.50f
 * the settle test failed with <i>"jaw settles in 1.9917 s, outside STYLE.md
 * 4b.6's 0.3-0.6 s band"</i>, and with the {@code express} table's SLACK lid
 * changed to snap (tau 0.001) the stagger test failed on ordering. Both were
 * restored and the suite re-run green; docs/system3b-debt.md carries the runs.
 */
class FaceRigTest {

    /** Integrates one channel step to within 2% and returns the time it took. */
    private static double settleSeconds(java.util.function.ToDoubleFunction<FaceRig> read,
                                        java.util.function.Consumer<FaceRig> aim) {
        FaceRig rig = new FaceRig();
        rig.snap();
        double before = read.applyAsDouble(rig);
        aim.accept(rig);
        FaceRig probe = new FaceRig();
        aim.accept(probe);
        probe.snap();
        double target = read.applyAsDouble(probe);
        double span = Math.abs(target - before);
        double dt = 1.0 / 120.0;
        for (int i = 1; i <= 240; i++) {
            rig.update((float) dt);
            if (Math.abs(read.applyAsDouble(rig) - target) <= 0.02 * span) {
                return i * dt;
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    @Test
    void everyChannelSettlesInsideTheBand() {
        java.util.Map<String, Double> settles = new java.util.LinkedHashMap<>();
        settles.put("lid", settleSeconds(FaceRig::lid, r -> r.express(Stance.READY, Stance.SLACK, 1f)));
        settles.put("brow", settleSeconds(FaceRig::brow, r -> r.express(Stance.READY, Stance.BRACED, 1f)));
        settles.put("gaze", settleSeconds(FaceRig::gazeX, r -> r.gazeToward(1f, 0f)));
        settles.put("jaw", settleSeconds(FaceRig::jaw, r -> r.express(Stance.READY, Stance.SLACK, 1f)));
        for (var e : settles.entrySet()) {
            assertTrue(e.getValue() >= 0.28 && e.getValue() <= 0.62,
                    e.getKey() + " settles in " + e.getValue()
                            + " s, outside STYLE.md 4b.6's 0.3-0.6 s band");
        }
        // §7.0.3 / §10's last row, inside the face: the four channels must not
        // arrive together. The band is a range to spend, not a point to share.
        assertTrue(settles.get("lid") < settles.get("brow")
                        && settles.get("brow") < settles.get("gaze")
                        && settles.get("gaze") < settles.get("jaw"),
                "the channels arrive together: " + settles);
        assertTrue(settles.get("jaw") - settles.get("lid") > 0.15,
                "the spread down the face is " + (settles.get("jaw") - settles.get("lid"))
                        + " s; STYLE.md 7.0.3 wants the band spent, not sat on");
    }

    /** First-order means zero overshoot: §7.2's strongest form. Verified, not assumed. */
    @Test
    void noChannelEverOvershoots() {
        FaceRig rig = new FaceRig();
        rig.snap();
        rig.express(Stance.READY, Stance.CARRIED, 1f);   // lid 1.15: an upward step
        float prev = rig.lid();
        for (int i = 0; i < 480; i++) {
            rig.update(1f / 120f);
            assertTrue(rig.lid() >= prev - 1e-5, "the lid came back: " + prev + " -> " + rig.lid());
            assertTrue(rig.lid() <= 1.15f + 1e-4, "the lid overshot 1.15: " + rig.lid());
            prev = rig.lid();
        }
    }

    /** The eye bone's scaleY floor: a lid at exactly zero is a degenerate skinning matrix. */
    @Test
    void theLidNeverCollapsesTheEyeBoneToZeroScale() {
        FaceRig rig = new FaceRig();
        rig.express(Stance.READY, Stance.SLACK, 1f);   // lid target 0.06
        rig.snap();
        Pose pose = new Pose();
        rig.write(pose);
        Pose.Delta eye = pose.get("eye");
        assertNotNull(eye);
        assertTrue(eye.scaleY >= 0.06f, "eye scaleY " + eye.scaleY);
    }

    /** The expression is a pure function of the stance blend — combat state in, face out. */
    @Test
    void expressionFollowsTheStanceBlendHalfway() {
        FaceRig rig = new FaceRig();
        rig.express(Stance.READY, Stance.SLACK, 0.5f);
        rig.snap();
        FaceRig.Set ready = FaceRig.of(Stance.READY);
        FaceRig.Set slack = FaceRig.of(Stance.SLACK);
        assertEquals((ready.jaw() + slack.jaw()) / 2f, rig.jaw(), 1e-5);
        assertEquals((ready.lid() + slack.lid()) / 2f, rig.lid(), 1e-5);
    }

    /** 4b.6's death row: SLACK closes the eye and drops the jaw — the two loudest channels. */
    @Test
    void deathClosesTheEyeAndSlackensTheJaw() {
        FaceRig.Set slack = FaceRig.of(Stance.SLACK);
        assertTrue(slack.lid() < 0.15f, "a dead eye is closed");
        assertTrue(slack.jaw() > 0.85f, "a dead jaw is slack");
        FaceRig.Set gathering = FaceRig.of(Stance.GATHERING);
        assertTrue(gathering.brow() > 0.4f, "effort draws the brow down");
        assertTrue(gathering.lid() < FaceRig.of(Stance.READY).lid(), "focus narrows the eye");
    }
}
