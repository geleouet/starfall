package dev.starfall.analysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The measurements this tool reproduces from the debt documents, locked as regressions.
 *
 * <p>These are not tests of the corpus; they are tests of the tool, taken against numbers that
 * were arrived at independently and written down before it existed. If a change to a primitive
 * silently moves one of them, that is exactly the class of error that would otherwise surface
 * as a wrong verdict in a review.
 *
 * <p>Each case names the document and the sentence it checks. Everything here is skipped, not
 * failed, when the capture is not on disk — the corpus is build output and someone cloning the
 * repo will not have it.
 */
class CorpusTest {

    private static final File CAPTURES = new File("out/captures");

    private static CaptureDir capture(String name) throws IOException {
        File dir = new File(CAPTURES, name);
        Assumptions.assumeTrue(dir.isDirectory(), "capture " + name + " is not on disk");
        return CaptureDir.of(dir);
    }

    private static File frame(String capture, String file) {
        File f = new File(new File(CAPTURES, capture), file);
        Assumptions.assumeTrue(f.isFile(), f + " is not on disk");
        return f;
    }

    /**
     * {@code docs/system3-debt.md} heads its comparison table "matched scale, 357 px figure".
     * Reproduced exactly by the largest-connected-component rule at 0.85 x paper.
     */
    @Test
    void figureHeightMatchesTheRecordedMatchedScale() throws IOException {
        Frame f = capture("s3-p1-reversal").frame(0);
        Paper paper = Paper.estimate(f);
        Figure figure = Figure.detect(f, paper, 0.85);
        assertEquals(357, figure.bounds.h, 2, "recorded matched scale is 357 px");
    }

    /**
     * {@code docs/system3-debt.md}: "The bind pose is bit-identical to s1-p7-bind, md5
     * ce533e77b1a1addefb4eda803bd11d5c on both."
     */
    @Test
    void theBindPoseIsStillBitIdentical() throws IOException {
        File a = frame("s3-p1-bind-regress", "frame_000.png");
        File b = frame("s1-p7-bind", "frame_000.png");
        FrameDiff d = FrameDiff.of(a, b, 0);
        assertEquals("ce533e77b1a1addefb4eda803bd11d5c", d.md5a);
        assertTrue(d.identicalBytes(), "the bind pose must stay bit-identical");
    }

    /**
     * {@code docs/system3-debt.md}: "the diff against s2-p3-gesture is 76,287 pixels".
     * Reproduced exactly, and the document is right that it is frame 0 only — the later frames
     * diverge by 289k, 353k and 387k, so the figure was never evidence of a small change.
     */
    @Test
    void theIkRegressionDiffIsTheRecordedCount() throws IOException {
        FrameDiff d = FrameDiff.of(frame("s3-p1-ik-regress", "frame_000.png"),
                frame("s2-p3-gesture", "frame_000.png"), 0);
        assertEquals(76287, d.changedPixels);
    }

    /**
     * {@code docs/system3-debt.md}: "s3-p1-hair is billed 'across a reversal' and contains zero
     * velocity sign changes in any region across all 24 frames."
     */
    @Test
    void theHairCaptureContainsNoReversalAtAll() throws IOException {
        CaptureDir dir = capture("s3-p1-hair");
        List<Frame> frames = dir.loadAll();
        Paper paper = Paper.estimate(frames.get(0));
        Figure figure = Figure.detect(frames.get(0), paper, 0.85);
        Map<String, Rect> regions = RegionSet.samurai().resolve(frames.get(0), figure);
        for (Map.Entry<String, Rect> e : regions.entrySet()) {
            Track t = Track.of(e.getKey(), e.getValue(), frames, paper, 0.85,
                    Track.Method.REGISTER, Track.Axis.PRINCIPAL, Track.DEFAULT_GATE, Registration.RADIUS);
            assertTrue(t.crossings().isEmpty(),
                    "region " + e.getKey() + " should not reverse in s3-p1-hair, found " + t.crossings());
        }
    }

    /**
     * {@code docs/system3-debt.md}: "velocity zero-crossings land at hips 11.5 -> head 13.5 ->
     * hair root 18.5 / hair mid 20.5 -> sleeve+blade 22.5. An 11-frame spread down the chain."
     *
     * <p>The ordering and the spread reproduce. The individual frames land about one frame
     * earlier throughout, which is within the region boxes the document does not record, so
     * this test locks the ordering and the spread rather than the absolute frames.
     */
    @Test
    void theArrivalChainReproduces() throws IOException {
        CaptureDir dir = capture("s3-p1-reversal");
        List<Frame> frames = dir.loadAll();
        Paper paper = Paper.estimate(frames.get(0));
        Figure figure = Figure.detect(frames.get(0), paper, 0.85);
        RegionSet set = RegionSet.samurai();
        Map<String, Track> tracks = new LinkedHashMap<>();
        for (String name : List.of("hips", "head", "hair-root", "hair-mid", "sleeve")) {
            Rect r = set.def(name).resolve(frames.get(0), figure);
            tracks.put(name, Track.of(name, r, frames, paper, 0.85,
                    Track.Method.REGISTER, Track.Axis.X, Track.DEFAULT_GATE, Registration.RADIUS));
        }
        Arrivals arrivals = new Arrivals("hips", tracks, 60);
        assertEquals(List.of("hips", "head", "hair-root", "hair-mid", "sleeve"), arrivals.chain(),
                "the recorded order is hips -> head -> hair root -> hair mid -> sleeve");
        assertEquals(11.0, arrivals.spreadFrames(), 1.5, "recorded as an 11-frame spread");
        assertTrue(arrivals.selected("hair-root").frame() > arrivals.selected("head").frame() + 3,
                "hair must lag the head by several frames, not arrive with it");
    }

    /**
     * {@code docs/system3-debt.md}: "Measured edge profile across a strand: paper to core in one
     * pixel, core to paper in one pixel, shoulders 1-3 px, no wet-bleed halo at all."
     */
    @Test
    void theHairEdgeIsStillOnePixelWithNoHalo() throws IOException {
        Frame f = capture("s3-p1-reversal").frame(0);
        for (int x : new int[] {430, 440, 450, 460}) {
            EdgeProfile e = EdgeProfile.measure(f, x, 128, EdgeProfile.Direction.DOWN, 26, 8);
            assertEquals(1, e.transitionWidth(), "strand at x=" + x + " should transition in one pixel");
            assertTrue(e.haloWidth() <= 2, "strand at x=" + x + " halo was " + e.haloWidth());
            assertTrue(e.hardEdge(), "strand at x=" + x + " should read as a hard edge");
        }
    }

    /**
     * The same primitive on the garment must <i>not</i> report a hard edge —
     * {@code docs/system1-debt.md}'s pass-7 correction records 1-76 px transitions behind
     * 10-131 px halos. Without this half, a broken measurement that always said "hard edge"
     * would pass the test above.
     */
    @Test
    void theGarmentEdgeIsSoft() throws IOException {
        Frame f = capture("s3-p1-reversal").frame(0);
        for (int y : new int[] {330, 360, 400}) {
            EdgeProfile e = EdgeProfile.measure(f, 400, y, EdgeProfile.Direction.RIGHT, 140, 8);
            assertTrue(e.haloWidth() >= 5,
                    "garment at y=" + y + " should sit behind a halo, got " + e.haloWidth());
            assertTrue(!e.hardEdge(), "garment at y=" + y + " must not read as a hard edge");
        }
    }

    /**
     * {@code docs/system1-debt.md}: "Brightest non-emissive (246, 241, 208). No pure black or
     * pure white in any frame", and "Darkest 0.5% of pixels = (28, 34, 44), right on #161A22
     * and never below."
     */
    @Test
    void theValueRangeHoldsAtBothEnds() throws IOException {
        Frame f = Frame.load(frame("s1-p5-swing", "frame_011.png"));
        ValueRange v = ValueRange.measure(f, Paper.estimate(f));
        assertEquals(0xF6F1D0, v.maxRgb, "recorded brightest non-emissive is (246, 241, 208)");
        assertEquals(0x161A22, v.minRgb, "the floor is #161A22 and the renderer sits exactly on it");
        assertTrue(v.floorRespected());
        assertTrue(!v.pureBlack && !v.pureWhite);
    }

    /**
     * {@code docs/system1-debt.md}: "Autocorrelation of the ground band shows no peak above 0.25
     * at any lag from 4 to 200 px."
     */
    @Test
    void theGroundBandHasNoPeriodicArtefact() throws IOException {
        Frame f = Frame.load(frame("s1-p7-swing", "frame_000.png"));
        Rect ground = f.bounds().fraction(0, 0.82, 1.0, 0.14);
        Autocorrelation ac = Autocorrelation.measure(f, ground, Autocorrelation.Axis.X, 4, 200);
        assertTrue(ac.clean(), "ground band prominence was " + ac.peak().prominence()
                + " at lag " + ac.peak().lag());
    }

    /**
     * {@code docs/system1-debt.md} D5: mean ink luminance rising towards the hem — the figure
     * ending in erasure rather than in ink smoke. The absolute band values in the document were
     * taken through an unrecorded column, so this locks the direction and the size of the jump,
     * which are what the finding rests on.
     */
    @Test
    void inkGravityIsStillInvertedInPass5() throws IOException {
        Frame f = Frame.load(frame("s1-p5-swing", "frame_011.png"));
        Paper paper = Paper.estimate(f);
        Figure figure = Figure.detect(f, paper, 0.85);
        BandProfile bp = BandProfile.measure(f, paper, figure.bounds, 5, 0.85);
        assertTrue(bp.gravity() < -30,
                "D5 records the hem as far paler than the chest; gravity was " + bp.gravity());
        assertTrue(bp.bands.get(4).meanInkLuminance() > 110,
                "the bottom band was recorded at 146; measured "
                        + bp.bands.get(4).meanInkLuminance());
    }
}
