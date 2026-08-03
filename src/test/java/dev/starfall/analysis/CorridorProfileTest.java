package dev.starfall.analysis;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md §11.0's rule, as a test file: <b>run the criterion on the reference.</b>
 *
 * <blockquote>A threshold justified by the corpus must be <em>shown to pass on the
 * corpus</em>, in the same command, before it is allowed to fail anything… let the
 * assertion that the reference passes live in the test suite beside the one that the
 * capture must.</blockquote>
 *
 * <p>That paragraph was written because System 4 set "the corridor between two bodies
 * is never below 0.06 of a figure height" from an eyeballed reading of reference image
 * 3, and two passes then chased it — while the image itself scores 0.015, and 0.000
 * over its full figure height. Pass 2 moved {@code LANE_SPREAD} 1.35 → 1.55 toward a
 * target its own ground truth misses by 4×, and broke the parry finding out that 1.70
 * was worse still.
 *
 * <p>So there are three kinds of assertion here and all three are load-bearing:
 *
 * <ol>
 *   <li>the instrument returns the analytically known answer on a synthetic frame,
 *       and returns the <em>right kind</em> of answer when the thing it measures is
 *       absent (§11.2b's generalised control);</li>
 *   <li><b>reference image 3 passes every floor {@link CorridorProfile} declares</b>,
 *       band by band — the assertion whose absence cost two passes;</li>
 *   <li>reference image 3 <b>fails</b> the whole-column criterion it was supposed to
 *       have set. Without this one, a broken reader that returned a large number
 *       everywhere would satisfy (2).</li>
 * </ol>
 */
class CorridorProfileTest {

    /** STYLE.md §1's Family B, image 3: the primary template for the game screen. */
    private static final File REFERENCE =
            new File("inspirations/image - 2026-08-02T101033.164.png");

    /** Its figure box. See {@code AnalysisCli.CORPUS_DUEL_SPAN} for why it is given rather than detected. */
    private static final Rect SPAN = Rect.ofCorners(0, 283, 831, 955);

    private static final double FACTOR = 0.85;
    private static final int STRIP = CorridorProfile.BACKGROUND_STRIP;

    // -- the instrument, against a known answer --------------------------------

    private static final int W = 400;
    private static final int H = 300;

    /**
     * Two bodies whose gap is a known width, and which is <em>narrower over the
     * middle band</em> — the shape reference image 3 has and the capture does not.
     */
    private static Frame twoBodies(int wideGap, int narrowGap) {
        int wl = 200 - wideGap / 2, wr = 200 + wideGap / 2;
        int nl = 200 - narrowGap / 2, nr = 200 + narrowGap / 2;
        return Synth.frame(W, H, (x, y) -> {
            if (y < 60 || y > 259) {
                return Synth.grey(Synth.PAPER);
            }
            // The pinch sits in the torso band: rows 60..259 is the figure, so the
            // torso is 0.18..0.41 of it, rows 96..141.
            boolean pinch = y >= 96 && y <= 141;
            int l = pinch ? nl : wl;
            int r = pinch ? nr : wr;
            boolean body = (x >= 40 && x <= l) || (x >= r && x <= 360);
            return body ? Synth.grey(30) : Synth.grey(Synth.PAPER);
        });
    }

    @Test
    void theProfileReturnsTheGapItIsToldToMeasure() {
        Frame f = twoBodies(120, 20);
        CorridorProfile.Profile p = CorridorProfile.measure(f, FACTOR, STRIP,
                Rect.ofCorners(0, 60, W - 1, 259));
        assertFalse(p.merged(), p.describe());
        // Figure height 200 px. The wide gap leaves columns 141..259 clear = 119.
        assertEquals(119, p.band("sash").columns(), p.describe());
        assertEquals(119 / 200.0, p.band("sash").fraction(), 1e-9, p.describe());
        // The torso band is pinched to 20 px of gap: columns 191..209 = 19 clear.
        assertEquals(19, p.band("torso").columns(), p.describe());
        assertEquals(19 / 200.0, p.band("torso").fraction(), 1e-9, p.describe());
    }

    @Test
    void theProfileSaysOneMassRatherThanZeroWhenTheBodiesTouch() {
        // §11.2b's control, and the same distinction DuellistsTest draws for the
        // scalar: "no corridor exists" and "a corridor of zero columns" are different
        // claims about the picture and only one of them is what a merge looks like.
        Frame f = Synth.frame(W, H, (x, y) ->
                y >= 60 && y <= 259 && x >= 40 && x <= 360 ? Synth.grey(30) : Synth.grey(Synth.PAPER));
        CorridorProfile.Profile p = CorridorProfile.measure(f, FACTOR, STRIP, null);
        assertTrue(p.merged(), p.describe());
        assertFalse(p.pass(), "one mass cannot pass a corridor criterion");
    }

    @Test
    void theRowBackgroundFollowsAGradedSkyWhereAPaperLevelCannot() {
        // The one change that lets the corpus and a capture be measured by the same
        // command. A background running 120 at the top to 40 at the bottom, with one
        // mark near the *bright* end at 0.8 of its own row. The mark is luminance 93;
        // open background at the dark end is 44. No single global threshold can call
        // the first ink and the second not, whatever level it picks — which is the
        // whole argument for a row-local background, stated as an inequality rather
        // than as a preference.
        Frame f = Synth.frame(200, 200, (x, y) -> {
            int sky = 120 - (y * 80) / 199;
            boolean mark = x >= 90 && x <= 110 && y >= 20 && y <= 40;
            return Synth.grey(mark ? (int) Math.round(0.8 * sky) : sky);
        });
        boolean[] ink = CorridorProfile.inkMask(f, FACTOR, 40);
        assertTrue(ink[30 * 200 + 100], "the mark at y=30 is 0.8 of its own row's sky and must be ink");
        assertFalse(ink[150 * 200 + 100], "open background at y=150 must not be ink");
        double mark = f.lum(100, 30);
        double farSky = f.lum(100, 190);
        assertTrue(mark > farSky,
                "the fixture is only interesting if the mark (" + mark + ") is brighter than "
                        + "empty background at the far end (" + farSky + ")");
        Paper flat = Paper.estimate(f);
        assertFalse(f.lum(100, 30) < flat.threshold(FACTOR) && f.lum(100, 190) >= flat.threshold(FACTOR),
                "a single global level " + flat.level + " cannot separate a mark at " + mark
                        + " from empty background at " + farSky + "; that is why the row background exists");
    }

    // -- the criterion, on the corpus ------------------------------------------

    @Test
    void referenceImageThreePassesEveryFloorTheProjectFailsCapturesOn() throws IOException {
        assertTrue(REFERENCE.isFile(), REFERENCE + " is part of the repository and must be readable");
        CorridorProfile.Profile p = CorridorProfile.measure(Frame.load(REFERENCE), FACTOR, STRIP, SPAN);
        assertFalse(p.merged(), p.describe());
        for (CorridorProfile.Reading r : p.readings()) {
            assertTrue(r.pass(), "STYLE.md 11.0: the corpus must pass the criterion the corpus set. "
                    + "Reference image 3's " + r.band() + " band reads " + r.fraction()
                    + " against a floor of " + r.floor() + ".\n" + p.describe());
        }
    }

    @Test
    void referenceImageThreeFailsTheWholeColumnCriterionTwoPassesChased() throws IOException {
        assertTrue(REFERENCE.isFile(), REFERENCE + " must be readable");
        Frame f = Frame.load(REFERENCE);
        double whole = CorridorProfile.wholeColumn(f, FACTOR, STRIP, SPAN);
        // docs/system4-debt.md's acceptance, quoted from pass 1: "the corridor must
        // never be zero and must not fall below 6% of figure height." Measured on the
        // image the number was read off, it is about a quarter of that.
        assertTrue(whole < 0.06,
                "the whole-column corridor on reference image 3 is " + whole
                        + ", which should be well under the 0.06 two passes were failed against; "
                        + "if this now passes, the reader has changed and the floors above are stale");
        assertEquals(0.015, whole, 0.004,
                "STYLE.md 11.0 records this image's clear whole-column run as 0.015 of a figure "
                        + "height; measured " + whole);
    }

    @Test
    void theReferenceProfileIsStableUnderItsOwnNuisanceParameters() throws IOException {
        assertTrue(REFERENCE.isFile(), REFERENCE + " must be readable");
        Frame f = Frame.load(REFERENCE);
        // The three bands the acceptance actually rests on, swept over ink factor and
        // over the figure span. A threshold taken from a number that moves under its
        // own nuisance parameters is the failure this whole file exists to stop, so
        // the stability is asserted rather than asserted-in-a-comment.
        for (double factor : new double[] {0.75, 0.85, 0.90}) {
            for (Rect span : new Rect[] {SPAN, Rect.ofCorners(0, 275, 831, 945),
                    Rect.ofCorners(0, 290, 831, 975)}) {
                CorridorProfile.Profile p = CorridorProfile.measure(f, factor, STRIP, span);
                for (String band : new String[] {"torso", "sash", "skirt"}) {
                    CorridorProfile.Reading r = p.band(band);
                    assertNotNull(r);
                    assertTrue(r.pass(), "reference band " + band + " at factor " + factor
                            + " span " + span.describe() + " reads " + r.fraction()
                            + " against floor " + r.floor());
                }
            }
        }
    }
}
