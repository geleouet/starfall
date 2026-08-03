package dev.starfall.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two-figure instruments, asserted against frames whose right answer is known
 * analytically — STYLE.md §11.2b(a)'s calibration card, applied to the two
 * measurements System 4 pass 2 is graded on.
 *
 * <p>Both of these decided the pass-1 review and both were one-off scripts. A
 * measurement with no null case is provisional (§11.2b, generalised from §7.1's
 * cloth control), so each one here is run against a frame where the thing being
 * measured is <em>absent</em> as well as one where it is present.
 */
class DuellistsTest {

    private static final int W = 400;
    private static final int H = 300;

    /** Paper, two dark bodies, and a gap of known width between them. */
    private static Frame twoBodies(int leftRight, int rightLeft) {
        return Synth.frame(W, H, (x, y) -> {
            boolean body = y >= 60 && y <= 260
                    && ((x >= 40 && x <= leftRight) || (x >= rightLeft && x <= 360));
            return body ? Synth.grey(30) : Synth.grey(Synth.PAPER);
        });
    }

    // -- the corridor ----------------------------------------------------------

    @Test
    void theCorridorIsTheGapItIsToldToMeasure() {
        Frame f = twoBodies(150, 250);
        Paper p = Paper.estimate(f);
        Duellists.Corridor c = Duellists.corridor(f, p, 0.85);
        assertEquals(2, c.bodies(), "two blocks of ink are two bodies");
        // Columns 151..249 inclusive carry no ink: 99 of them.
        assertEquals(99, c.columns(), c.describe());
        // The figure box is 201 px tall, so the corridor is 99/201 of it.
        assertEquals(99 / 201.0, c.fraction(), 1e-9);
    }

    @Test
    void theCorridorRefusesToInventAGapWhenTheBodiesAreOneMass() {
        // The null case, and the one that matters: pass 1's parry spent six frames
        // as a single connected component holding 78% of the frame's ink. A
        // corridor measurement that returned "0 px" there would be indistinguishable
        // from two bodies touching at one column, which is a different picture.
        Frame f = twoBodies(200, 201);
        Duellists.Corridor c = Duellists.corridor(f, Paper.estimate(f), 0.85);
        assertTrue(c.merged(), "two blocks that meet are one mass and must say so");
        assertEquals(0, c.columns());
        assertTrue(c.describe().contains("one connected ink component"), c.describe());
    }

    @Test
    void theCorridorNarrowsMonotonicallyAsTheBodiesClose() {
        double previous = Double.MAX_VALUE;
        for (int gap : new int[] {160, 120, 80, 40, 10}) {
            int leftRight = 200 - gap / 2;
            int rightLeft = 200 + gap / 2;
            Duellists.Corridor c = Duellists.corridor(twoBodies(leftRight, rightLeft),
                    Paper.estimate(twoBodies(leftRight, rightLeft)), 0.85);
            assertTrue(c.fraction() < previous, "corridor did not narrow at gap " + gap);
            previous = c.fraction();
        }
    }

    // -- the blades ------------------------------------------------------------

    /** Two cool-bright slivers with a known separation, over warm paper. */
    private static Frame twoBlades(int gap) {
        int left = 190 - gap / 2;
        int right = 190 + gap / 2;
        return Synth.frame(W, H, (x, y) -> {
            boolean blade = y >= 100 && y <= 180
                    && (Math.abs(x - left) <= 1 || Math.abs(x - right) <= 1);
            // Near-white and faintly cool, per STYLE.md 2.1's BLADE_LUMINANCE.
            return blade ? Synth.rgb(234, 242, 248) : Synth.rgb(237, 228, 211);
        });
    }

    @Test
    void bladeSeparationIsThePointToPointMinimum() {
        Duellists.Blades b = Duellists.blades(twoBlades(60), Paper.estimate(twoBlades(60)), 0.85);
        assertEquals(2, b.clouds(), b.describe());
        // Each sliver is 3 px wide and their centres are 60 apart, so the facing
        // edges sit at x=161 and x=219: 58 px between the nearest lit pixels.
        assertEquals(58.0, b.separation(), 1e-9, b.describe());
    }

    @Test
    void warmPaperIsNeverMistakenForABlade() {
        // The null case for the colour test. Paper is warm at r-b = +24 and this
        // frame is nothing but paper; a segmenter keyed on luminance alone would
        // return the whole frame, because paper at 218 is brighter than the 212
        // threshold.
        Frame f = Synth.frame(W, H, (x, y) -> Synth.rgb(237, 228, 211));
        assertTrue(Duellists.coolBrightClouds(f).isEmpty(),
                "warm paper segmented as steel: the colour half of the test is not working");
    }

    @Test
    void aWarmClashCoreDoesNotBridgeTwoBlades() {
        // The reason the review's definition is cool-bright rather than bright: a
        // clash bloom is FFF6E2, warm and brighter than the blade. If it counted,
        // two blades either side of a bloom would measure as met -- and the whole
        // acceptance criterion would be satisfied by drawing a bigger bloom.
        Frame f = Synth.frame(W, H, (x, y) -> {
            boolean blade = y >= 100 && y <= 180 && (Math.abs(x - 160) <= 1 || Math.abs(x - 220) <= 1);
            boolean bloom = Math.abs(x - 190) <= 25 && Math.abs(y - 140) <= 25;
            if (blade) {
                return Synth.rgb(234, 242, 248);
            }
            return bloom ? Synth.rgb(255, 246, 226) : Synth.rgb(237, 228, 211);
        });
        Duellists.Blades b = Duellists.blades(f, Paper.estimate(f), 0.85);
        assertEquals(2, b.clouds(), "the bloom joined the two blades into one cloud: " + b.describe());
        assertEquals(58.0, b.separation(), 1e-9, b.describe());
    }

    // -- the refusal -----------------------------------------------------------

    @Test
    void aFigureRelativeRegionRefusesATwoFigureFrame() {
        // STYLE.md §11.3: "a silent wrong answer is worse than a refusal." Pass 1's
        // tooling resolved `head` onto the wrong figure's hair and reported a hem
        // lag inside §7.1's own band from it.
        Frame f = twoBodies(150, 250);
        Paper p = Paper.estimate(f);
        RegionSet set = RegionSet.samurai();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> set.resolve(f, p, 0.85, RegionSet.Which.SOLE));
        assertTrue(e.getMessage().contains("no figure has been named"), e.getMessage());
    }

    @Test
    void namingAFigureResolvesAgainstThatBodyAlone() {
        Frame f = twoBodies(150, 250);
        Paper p = Paper.estimate(f);
        RegionSet set = RegionSet.samurai();
        Rect left = set.resolve(f, p, 0.85, RegionSet.Which.LEFT).get("figure");
        Rect right = set.resolve(f, p, 0.85, RegionSet.Which.RIGHT).get("figure");
        assertEquals(40, left.x);
        assertEquals(111, left.w, "the left figure box spans 40..150");
        assertEquals(250, right.x);
        assertEquals(111, right.w, "the right figure box spans 250..360");
        // And the un-named box, which is what pass 1 measured through, spans both
        // the moment the two bodies touch -- which they did for sixteen consecutive
        // frames of the parry. That is the defect, stated as a rectangle.
        Frame touching = twoBodies(200, 201);
        Figure both = Figure.detect(touching, Paper.estimate(touching), 0.85);
        assertTrue(both.bounds.w > left.w * 2,
                "the detected box on a merged frame is " + both.bounds.describe()
                        + ", which should span both bodies");
    }

    @Test
    void namingAFigureOnAOneFigureFrameAlsoRefuses() {
        // The other direction of the same mismatch: a caller that thinks it is
        // measuring the right-hand duellist on a frame that has only one body is
        // as wrong as the reverse, and equally silent if allowed through.
        Frame f = Synth.paperWithBlock(W, H, new Rect(150, 60, 100, 200), 30);
        Paper p = Paper.estimate(f);
        assertThrows(IllegalStateException.class,
                () -> RegionSet.samurai().resolve(f, p, 0.85, RegionSet.Which.RIGHT));
    }

    @Test
    void oneFigureFramesStillResolveExactlyAsTheyDid() {
        // The whole corpus is single-figure captures and none of their numbers may
        // move: the refusal is additive, per §11.2b's rule that a guard must not be
        // a re-measurement.
        Frame f = Synth.paperWithBlock(W, H, new Rect(150, 60, 100, 200), 30);
        Paper p = Paper.estimate(f);
        assertEquals(RegionSet.samurai().resolve(f, Figure.detect(f, p, 0.85)),
                RegionSet.samurai().resolve(f, p, 0.85, RegionSet.Which.SOLE));
    }
}
