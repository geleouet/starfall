package dev.starfall.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ground-truth tests for the still-frame primitives.
 *
 * <p>These matter more than most tests in the project, because their outputs are used to
 * decide whether a pass passes. Every case here is a synthetic frame whose answer is known by
 * construction, so a wrong measurement fails here rather than in a review.
 */
class MeasurementTest {

    @Test
    void paperIsTheBorderMode() {
        // A dark figure in the middle must not pull the estimate: the border is all paper.
        Frame f = Synth.paperWithBlock(200, 120, new Rect(60, 30, 60, 50), 40);
        Paper p = Paper.estimate(f);
        assertEquals(Synth.PAPER, p.level, 0.51);
    }

    @Test
    void figureIsTheLargestConnectedComponent() {
        // One 40x60 block plus a smaller detached one: the figure is the larger.
        Frame f = Synth.frame(200, 120, (x, y) -> {
            if (x >= 60 && x < 100 && y >= 30 && y < 90) {
                return Synth.grey(40);
            }
            if (x >= 150 && x < 160 && y >= 20 && y < 30) {
                return Synth.grey(40);
            }
            return Synth.grey(Synth.PAPER);
        });
        Figure fig = Figure.detect(f, Paper.estimate(f), 0.85);
        assertEquals(new Rect(60, 30, 40, 60), fig.bounds);
        assertEquals(40 * 60, fig.componentInk);
        assertEquals(60, fig.height());
    }

    @Test
    void coverageCountsExactlyTheInkPixels() {
        Frame f = Synth.paperWithBlock(200, 120, new Rect(60, 30, 40, 60), 40);
        Paper p = Paper.estimate(f);
        Figure fig = Figure.detect(f, p, 0.85);
        // A window half on the block: 20 of 40 columns are ink.
        Rect probe = new Rect(80, 30, 40, 60);
        Coverage c = Coverage.measure(f, p, "probe", probe, 0.85, fig);
        assertEquals(20 * 60, c.inkPixels());
        assertEquals(0.5, c.fraction(), 1e-9);
        assertEquals(0.5, c.shareByCount(), 1e-9);
    }

    @Test
    void coverageThresholdIsRelativeToPaper() {
        // Ink at 0.7 x paper is inside a 0.85 threshold and outside a 0.6 one.
        int level = (int) Math.round(0.7 * Synth.PAPER);
        Frame f = Synth.paperWithBlock(200, 120, new Rect(60, 30, 40, 60), level);
        Paper p = Paper.estimate(f);
        assertEquals(40 * 60, Coverage.measure(f, p, new Rect(60, 30, 40, 60), 0.85, null).inkPixels());
        assertEquals(0, Coverage.measure(f, p, new Rect(60, 30, 40, 60), 0.60, null).inkPixels());
    }

    @Test
    void bandProfileFindsInvertedInkGravity() {
        // Dark at the top, pale at the bottom — System 1's D5 failure, by construction.
        Rect body = new Rect(50, 20, 40, 80);
        Frame f = Synth.frame(200, 140, (x, y) -> {
            if (!body.contains(x, y)) {
                return Synth.grey(Synth.PAPER);
            }
            int t = (y - body.y) * 100 / body.h;
            return Synth.grey(40 + t);
        });
        BandProfile bp = BandProfile.measure(f, Paper.estimate(f), body, 4, 0.85);
        assertEquals(4, bp.bands.size());
        double top = bp.bands.get(0).meanInkLuminance();
        double bottom = bp.bands.get(3).meanInkLuminance();
        assertTrue(bottom > top, "bottom band should be paler in this construction");
        assertTrue(bp.gravity() < 0, "negative gravity means the figure bleaches downward");
        // Bands are 20 rows each; the mean of a linear ramp is its midpoint.
        assertEquals(40 + (0 + 19) / 2.0 * 100 / 80.0, top, 0.6);
    }

    @Test
    void bandProfileIgnoresPaperInsideTheBand() {
        // Half the band is paper. The mean must be the ink level, not the average of both.
        Rect body = new Rect(50, 20, 40, 40);
        Frame f = Synth.frame(200, 140, (x, y) ->
                body.contains(x, y) && x < body.x + 20 ? Synth.grey(60) : Synth.grey(Synth.PAPER));
        BandProfile bp = BandProfile.measure(f, Paper.estimate(f), body, 2, 0.85);
        assertEquals(60, bp.bands.get(0).meanInkLuminance(), 0.01);
        assertEquals(0.5, bp.bands.get(0).coverage(), 1e-9);
    }

    @Test
    void centroidIsInkWeighted() {
        // Two equal-size marks, one twice as far below paper: the centroid sits nearer it.
        Frame f = Synth.frame(100, 40, (x, y) -> {
            if (y >= 10 && y < 20 && x >= 10 && x < 20) {
                return Synth.grey(Synth.PAPER - 40);
            }
            if (y >= 10 && y < 20 && x >= 70 && x < 80) {
                return Synth.grey(Synth.PAPER - 80);
            }
            return Synth.grey(Synth.PAPER);
        });
        Centroid c = Centroid.measure(f, Paper.estimate(f), f.bounds(), 0.99);
        // Marks centred at x=14.5 and x=74.5 with weights 1 and 2.
        assertEquals((14.5 * 1 + 74.5 * 2) / 3.0, c.x(), 0.05);
    }

    @Test
    void valueFloorIsCheckedAgainstTheStyleColour() {
        Frame ok = Synth.frame(40, 40, (x, y) -> x == 0 && y == 0 ? ValueRange.FLOOR_RGB : Synth.grey(Synth.PAPER));
        assertTrue(ValueRange.measure(ok, Paper.estimate(ok)).floorRespected());

        Frame tooDark = Synth.frame(40, 40, (x, y) -> x == 0 && y == 0 ? 0x000000 : Synth.grey(Synth.PAPER));
        ValueRange v = ValueRange.measure(tooDark, Paper.estimate(tooDark));
        assertFalse(v.floorRespected());
        assertTrue(v.pureBlack);
    }

    @Test
    void edgeProfileMeasuresAHardEdgeAsOnePixel() {
        // Paper then an instant step to ink: transition 1 px, no halo.
        Frame f = Synth.frame(80, 10, (x, y) -> x < 40 ? Synth.grey(Synth.PAPER) : Synth.grey(40));
        EdgeProfile e = EdgeProfile.measure(f, 20, 5, EdgeProfile.Direction.RIGHT, 50, 5);
        assertEquals(1, e.transitionWidth());
        assertEquals(0, e.haloWidth());
        assertTrue(e.hardEdge());
    }

    @Test
    void edgeProfileSeparatesTheHaloFromTheTransition() {
        // 20 px of paper, then a 10 px shallow halo, then a 10 px ramp into the core.
        Frame f = Synth.frame(120, 10, (x, y) -> {
            if (x < 20) {
                return Synth.grey(200);
            }
            if (x < 30) {
                return Synth.grey(198);          // halo: a slight dip
            }
            if (x < 40) {
                return Synth.grey(198 - (x - 30) * 16); // ramp down to 38
            }
            return Synth.grey(38);
        });
        EdgeProfile e = EdgeProfile.measure(f, 0, 5, EdgeProfile.Direction.RIGHT, 60, 5);
        assertEquals(200, e.paperLevel, 0.01);
        assertEquals(38, e.coreLevel, 0.01);
        assertFalse(e.hardEdge());
        assertTrue(e.haloWidth() >= 9 && e.haloWidth() <= 11, "halo was " + e.haloWidth());
        assertTrue(e.transitionWidth() >= 8 && e.transitionWidth() <= 12, "transition was " + e.transitionWidth());
    }

    @Test
    void markWidthsRecoverExactRunLengths() {
        // A column with ink runs of 3, 7 and 20 px separated by paper.
        Frame f = Synth.frame(120, 120, (x, y) -> {
            boolean inColumn = x >= 40 && x < 80;
            boolean ink = inColumn
                    && ((y >= 12 && y < 15) || (y >= 20 && y < 27) || (y >= 40 && y < 60));
            return Synth.grey(ink ? 40 : Synth.PAPER);
        });
        MarkWidths m = MarkWidths.measure(f, Paper.estimate(f), new Rect(40, 10, 40, 100), true, 1, 0.85);
        assertEquals(java.util.List.of(3, 7, 20), m.cuts.get(0).runs());
    }

    @Test
    void markWidthsSeparateBimodalFromUnimodal() {
        // Bimodal: 1 px hairlines beside a 40 px mass — the reference hair's signature.
        Frame bimodal = Synth.frame(40, 120, (x, y) -> {
            boolean ink = (y >= 2 && y < 3) || (y >= 6 && y < 7) || (y >= 10 && y < 11)
                    || (y >= 14 && y < 15) || (y >= 40 && y < 80);
            return Synth.grey(ink ? 40 : Synth.PAPER);
        });
        MarkWidths bm = MarkWidths.measure(bimodal, Paper.estimate(bimodal), bimodal.bounds(), true, 1, 0.85);
        assertTrue(bm.bimodal(), "expected bimodal, coefficient was " + bm.bimodality());

        // Unimodal: every mark the same width — System 3's failure, by construction.
        Frame unimodal = Synth.frame(40, 120, (x, y) -> {
            boolean ink = (y % 16) < 8 && y >= 2;
            return Synth.grey(ink ? 40 : Synth.PAPER);
        });
        MarkWidths um = MarkWidths.measure(unimodal, Paper.estimate(unimodal), unimodal.bounds(), true, 1, 0.85);
        assertFalse(um.bimodal(), "expected unimodal, coefficient was " + um.bimodality());
    }

    @Test
    void autocorrelationFindsAKnownPeriodAndClearsASmoothWash() {
        int period = 12;
        Frame banded = Synth.frame(240, 40, (x, y) ->
                Synth.grey((int) Math.round(150 + 20 * Math.sin(2 * Math.PI * x / period))));
        Autocorrelation ac = Autocorrelation.measure(banded, banded.bounds(), Autocorrelation.Axis.X, 4, 100);
        assertEquals(period, ac.peak().lag());
        assertFalse(ac.clean(), "a sine at period 12 is a periodic artefact");

        // A smooth ramp correlates near 1.0 at short lags but has no period: it must pass.
        Frame wash = Synth.frame(240, 40, (x, y) -> Synth.grey(120 + x / 4));
        Autocorrelation clean = Autocorrelation.measure(wash, wash.bounds(), Autocorrelation.Axis.X, 4, 100);
        assertTrue(clean.clean(), "a smooth gradient must not be called banding; prominence was "
                + clean.peak().prominence());
    }

    @Test
    void highPassRemovesTheLocalMean() {
        double[] ramp = new double[64];
        for (int i = 0; i < ramp.length; i++) {
            ramp[i] = 100 + 2.0 * i;
        }
        double[] hp = Autocorrelation.highPass(ramp, 9);
        // Away from the borders a linear ramp is exactly its own local mean.
        for (int i = 8; i < 56; i++) {
            assertEquals(0.0, hp[i], 1e-9);
        }
    }

    @Test
    void frameDiffReportsIdenticalAndChangedPixels() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("starfall-diff").toFile();
        java.io.File a = new java.io.File(dir, "a.png");
        java.io.File b = new java.io.File(dir, "b.png");
        java.io.File c = new java.io.File(dir, "c.png");
        write(Synth.paperWithBlock(40, 40, new Rect(5, 5, 10, 10), 40), a);
        write(Synth.paperWithBlock(40, 40, new Rect(5, 5, 10, 10), 40), b);
        write(Synth.paperWithBlock(40, 40, new Rect(6, 5, 10, 10), 40), c);

        FrameDiff same = FrameDiff.of(a, b, 0);
        assertTrue(same.identicalPixels());
        assertEquals(same.md5a, same.md5b);

        FrameDiff moved = FrameDiff.of(a, c, 0);
        assertFalse(moved.identicalBytes());
        assertEquals(2 * 10, moved.changedPixels, "one column gained, one lost");
        assertNotNull(moved.changedBounds);
    }

    private static void write(Frame f, java.io.File file) throws Exception {
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(f.width, f.height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, f.width, f.height, f.rgb, 0, f.width);
        javax.imageio.ImageIO.write(img, "png", file);
    }

    @Test
    void regionSpecsParseInEverySpace() {
        RegionSet.Def pixel = RegionSet.parseSpec("hips=475,266,90,35");
        assertEquals(RegionSet.Space.PIXEL, pixel.space());
        Frame f = Synth.paperWithBlock(400, 400, new Rect(100, 50, 200, 300), 40);
        Figure fig = Figure.detect(f, Paper.estimate(f), 0.85);
        assertEquals(new Rect(475, 266, 90, 35), pixel.resolve(f, fig));

        RegionSet.Def figure = RegionSet.parseSpec("hem=fig:0.0,0.5,1.0,0.5");
        assertEquals(new Rect(100, 200, 200, 150), figure.resolve(f, fig));

        RegionSet.Def image = RegionSet.parseSpec("band=img:0.0,0.5,1.0,0.25");
        assertEquals(new Rect(0, 200, 400, 100), image.resolve(f, fig));
    }

    @Test
    void regionSetSurvivesAJsonRoundTrip() throws Exception {
        RegionSet original = RegionSet.samurai();
        java.io.File file = java.nio.file.Files.createTempFile("regions", ".json").toFile();
        java.nio.file.Files.writeString(file.toPath(), original.toJson());
        RegionSet loaded = RegionSet.load(file);
        assertEquals(original.names(), loaded.names());
        for (String n : original.names()) {
            assertEquals(original.def(n), loaded.def(n));
        }
    }

    @Test
    void jsonRoundTripsNestedValues() {
        String text = new Json.Writer().beginObject()
                .prop("name", "hair \"mid\"")
                .prop("coverage", 0.483)
                .prop("clean", true)
                .prop("rect", new Rect(1, 2, 3, 4))
                .endObject().toString();
        java.util.Map<String, Object> parsed = Json.parseObject(text);
        assertEquals("hair \"mid\"", parsed.get("name"));
        assertEquals(0.483, (Double) parsed.get("coverage"), 1e-6);
        assertEquals(Boolean.TRUE, parsed.get("clean"));
        assertEquals(java.util.List.of(1.0, 2.0, 3.0, 4.0), parsed.get("rect"));
    }

    @Test
    void figureRelativeRegionsWithoutAFigureFailLoudly() {
        Frame blank = Synth.frame(40, 40, (x, y) -> Synth.grey(Synth.PAPER));
        Figure none = Figure.detect(blank, Paper.estimate(blank), 0.85);
        RegionSet.Def d = RegionSet.parseSpec("hem=fig:0,0.5,1,0.5");
        assertThrows(IllegalStateException.class, () -> d.resolve(blank, none));
    }
}
