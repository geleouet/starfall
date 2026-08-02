package dev.starfall.analysis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ground-truth tests for the sequence primitives — registration, tracking and the arrival
 * chain.
 *
 * <p>The synthetic sequences here have their reversal frames written into their construction,
 * so a claim like "hips reverse at 11.5" can be checked against a case where 11.5 is true by
 * definition. This is the part of the tool most likely to be used to pass or fail a system, so
 * it is the part that most needs to be right.
 */
class MotionTest {

    /** A soft dark disc on paper, centred wherever asked, so a track has something to follow. */
    private static Frame disc(double cx, double cy) {
        return Synth.frame(200, 120, (x, y) -> {
            double d = Math.hypot(x - cx, y - cy);
            double a = Math.exp(-(d * d) / (2 * 8.0 * 8.0));
            return Synth.grey((int) Math.round(Synth.PAPER - 170 * a));
        });
    }

    private static List<Frame> sequence(java.util.function.IntToDoubleFunction xs,
                                        java.util.function.IntToDoubleFunction ys, int n) {
        List<Frame> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(disc(xs.applyAsDouble(i), ys.applyAsDouble(i)));
        }
        return out;
    }

    @Test
    void registrationRecoversAKnownIntegerShift() {
        Frame a = disc(100, 60);
        Frame b = disc(107, 57);
        Registration.Shift s = Registration.between(a, b, new Rect(80, 40, 40, 40));
        assertEquals(7, s.dx(), 0.15);
        assertEquals(-3, s.dy(), 0.15);
        assertFalse(s.clipped());
    }

    @Test
    void registrationRecoversASubPixelShift() {
        Frame a = disc(100.0, 60.0);
        Frame b = disc(102.5, 60.0);
        Registration.Shift s = Registration.between(a, b, new Rect(80, 40, 40, 40));
        assertEquals(2.5, s.dx(), 0.35);
        assertEquals(0.0, s.dy(), 0.35);
    }

    @Test
    void registrationReportsWhenItHitsTheSearchWindow() {
        Frame a = disc(60, 60);
        Frame b = disc(120, 60);
        Registration.Shift s = Registration.between(a, b, new Rect(45, 45, 30, 30), 6);
        assertTrue(s.clipped(), "a 60 px move inside a 6 px window must be reported as clipped");
    }

    @Test
    void centroidTrackFindsTheReversalOfAKnownParabola() {
        // x(i) = 100 + 0.08 (i - 11.5)^2 : velocity is exactly zero at i = 11.5.
        List<Frame> frames = sequence(i -> 100 + 0.08 * Math.pow(i - 11.5, 2), i -> 60.0, 24);
        Track t = Track.of("probe", new Rect(0, 20, 200, 80), frames, Paper.estimate(frames.get(0)),
                0.85, Track.Method.CENTROID, Track.Axis.X, Track.DEFAULT_GATE, Registration.RADIUS);
        List<Track.Crossing> cs = t.crossings();
        assertEquals(1, cs.size(), "one reversal expected, got " + cs);
        assertEquals(11.5, cs.get(0).frame(), 0.35);
    }

    @Test
    void registrationTrackFindsTheSameReversal() {
        List<Frame> frames = sequence(i -> 100 + 0.4 * Math.pow(i - 11.5, 2), i -> 60.0, 24);
        Track t = Track.of("probe", new Rect(80, 40, 40, 40), frames, Paper.estimate(frames.get(0)),
                0.85, Track.Method.REGISTER, Track.Axis.X, Track.DEFAULT_GATE, Registration.RADIUS);
        assertEquals(11.5, t.dominantCrossing().frame(), 0.6);
    }

    @Test
    void theDominantReversalIsNotNecessarilyTheFirst() {
        // Six frames of half-pixel jitter — a strand in an ambient breeze — then one real
        // excursion bottoming out at frame 15. The first crossing is jitter; the dominant
        // one is the event.
        int n = 25;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i < 6 ? 100 + 0.5 * (i % 2) : 100 - 20 * Math.sin(Math.PI * (i - 6) / 18.0);
            y[i] = 60;
        }
        Track t = Track.fromPositions("probe", new Rect(0, 20, 200, 80), x, y,
                Track.Axis.X, Track.DEFAULT_GATE);
        assertTrue(t.crossings().size() > 1, "the jitter should register as extra crossings, got "
                + t.crossings());
        assertEquals(0.5, t.crossings().get(0).frame(), 0.6);
        assertEquals(15.0, t.dominantCrossing().frame(), 1.0);
    }

    @Test
    void principalAxisFollowsDiagonalMotion() {
        double[] px = {0, 1, 2, 3, 4};
        double[] py = {0, 1, 2, 3, 4};
        double[] axis = Track.principalAxis(px, py);
        assertEquals(Math.sqrt(0.5), axis[0], 1e-6);
        assertEquals(Math.sqrt(0.5), axis[1], 1e-6);
    }

    @Test
    void anArrivalChainCannotBeBuiltWithoutAnAnchor() {
        Map<String, Track> tracks = staggeredChain();
        assertThrows(IllegalArgumentException.class, () -> new Arrivals(null, tracks, 60));
        assertThrows(IllegalArgumentException.class, () -> new Arrivals("  ", tracks, 60));
        assertThrows(IllegalArgumentException.class, () -> new Arrivals("elbow", tracks, 60));
    }

    @Test
    void lagIsReportedAgainstTheNamedAnchorAndChangesWithIt() {
        Map<String, Track> tracks = staggeredChain();

        Arrivals againstHips = new Arrivals("hips", tracks, 60);
        double hemVsHips = lagOf(againstHips, "hem");

        Arrivals againstWrist = new Arrivals("wrist", tracks, 60);
        double hemVsWrist = lagOf(againstWrist, "hem");

        // Same hem, same capture, two defensible anchors, two different lags. This is the
        // whole reason STYLE.md 7.1 requires the anchor to be stated.
        assertEquals(3.0, hemVsHips, 1.0);
        assertEquals(-8.0, hemVsWrist, 1.0);
        assertTrue(Math.abs(hemVsHips - hemVsWrist) > 3.0,
                "expected the anchor to change the lag materially: " + hemVsHips + " vs " + hemVsWrist);
    }

    @Test
    void theChainReportsItsSpreadAndFlagsSimultaneousArrival() {
        Arrivals staggered = new Arrivals("hips", staggeredChain(), 60);
        assertTrue(staggered.spreadFrames() > 5, "spread was " + staggered.spreadFrames());
        assertFalse(staggered.everythingArrivesTogether(1.0));

        Map<String, Track> together = new LinkedHashMap<>();
        together.put("hips", parabolaTrack("hips", 11.5));
        together.put("hem", parabolaTrack("hem", 11.7));
        together.put("wrist", parabolaTrack("wrist", 11.6));
        Arrivals lockstep = new Arrivals("hips", together, 60);
        assertTrue(lockstep.everythingArrivesTogether(1.0),
                "STYLE.md 10's last row must trip when everything peaks together; spread was "
                        + lockstep.spreadFrames());
    }

    @Test
    void directionMatchingKeepsTheChainOnOneEvent() {
        // The sleeve turns the other way early, then arrives late. Matching the anchor's
        // direction must pick the late arrival, not the early opposite turn.
        Map<String, Track> tracks = new LinkedHashMap<>();
        tracks.put("hips", parabolaTrack("hips", 10.0));
        // Peaks at frame 4 (a + to - turn) and troughs at frame 18 (a - to + turn, the arrival).
        tracks.put("sleeve", syntheticTrack("sleeve",
                i -> 100 + 12 * Math.cos(2 * Math.PI * (i - 4) / 28), 24));
        Arrivals matched = new Arrivals("hips", tracks, 60);
        assertTrue(matched.selected("sleeve").frame() > 14,
                "expected the late same-direction arrival, got " + matched.selected("sleeve").frame());

        Arrivals unmatched = new Arrivals("hips", tracks, 60, Arrivals.Selector.FIRST, 0, false);
        assertTrue(unmatched.selected("sleeve").frame() < 10,
                "without direction matching the first crossing is the opposite turn");
    }

    @Test
    void settleFindsTheLastMovingStep() {
        Track t = syntheticTrack("tip", i -> 100 + (i < 10 ? i * 3.0 : 30.0), 20);
        assertEquals(9.5, t.settle(0.5), 0.01);
    }

    // ------------------------------------------------------------------ helpers

    private static double lagOf(Arrivals a, String region) {
        return a.lags.stream().filter(l -> l.region().equals(region))
                .mapToDouble(Arrivals.Lag::lagFrames).findFirst().orElseThrow();
    }

    /** hips reverse at 10, hem at 13, wrist at 21 — a chain with a known stagger. */
    private static Map<String, Track> staggeredChain() {
        Map<String, Track> tracks = new LinkedHashMap<>();
        tracks.put("hips", parabolaTrack("hips", 10.0));
        tracks.put("hem", parabolaTrack("hem", 13.0));
        tracks.put("wrist", parabolaTrack("wrist", 21.0));
        return tracks;
    }

    private static Track parabolaTrack(String name, double vertex) {
        return syntheticTrack(name, i -> 100 + 0.08 * Math.pow(i - vertex, 2), 30);
    }

    /** Builds a track from a position function, through real frames and real measurement. */
    private static Track syntheticTrack(String name, java.util.function.DoubleUnaryOperator posX, int n) {
        List<Frame> frames = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            frames.add(disc(posX.applyAsDouble(i), 60));
        }
        return Track.of(name, new Rect(0, 20, 200, 80), frames, Paper.estimate(frames.get(0)),
                0.85, Track.Method.CENTROID, Track.Axis.X, Track.DEFAULT_GATE, Registration.RADIUS);
    }
}
