package dev.starfall.analysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Family B's composition, graded on delivered pixels: <b>a dark duellist against a
 * pale one.</b>
 *
 * <h2>Why this file and not {@code DirectorTest.bothFiguresAreVisuallyDistinguishable}</h2>
 *
 * <p>That test asserts on {@code InkMaterial}'s base colour. It was green through the
 * whole of the regression the pass-2 review measured, in which the two figures came
 * within <b>1.51x</b> of each other in delivered value against reference image 3's
 * <b>3.3x</b>, and the review's verdict was <i>"you cannot tell which duellist is the
 * pale one"</i> on all twelve frames of a capture. A base colour is not a picture.
 *
 * <h2>System 4 pass 4: the statistic changed, and the reason is in the pixels</h2>
 *
 * <p>Passes 2 and 3 were graded with {@link CorridorProfile#medianInkOverGround}, which
 * medians the pixels <em>darker than 0.85x</em> the row background. That is well posed
 * while both figures are darker than the ground, which they are on Family A cream. This
 * pass draws the duel against the Family B dusk sky, whose luminance at the torso rows
 * is about 89 rather than 213, and the pale duellist is now <b>brighter</b> than its own
 * ground over most of its area — so the thresholded statistic quietly changes subject
 * to "the darkest quarter of the pale figure" and reports a <em>fall</em> from 2.12x to
 * 1.20x on a frame where the separation is visibly the best this project has produced.
 *
 * <p>So the delivered gate moves to {@link CorridorProfile#medianOverGround}, which has
 * no threshold and therefore cannot change meaning when a figure crosses its ground.
 * Both statistics are asserted on the corpus first, per STYLE.md 11.0, and the
 * thresholded one is kept on the reference so the 3.27x every previous document quotes
 * stays checkable.
 */
class DuellistValueTest {

    /** STYLE.md §1's Family B, all three of it. */
    private static final File IMAGE3 = new File("inspirations/image - 2026-08-02T101033.164.png");
    private static final File IMAGE4 = new File("inspirations/image - 2026-08-02T101128.842.png");
    private static final File IMAGE5 = new File("inspirations/image - 2026-08-02T101232.595.png");

    private static final File CAPTURE =
            new File("out/captures/s4-p4-parry-contact/frame_011.png");

    private static final double FACTOR = 0.85;
    private static final int STRIP = CorridorProfile.BACKGROUND_STRIP;

    /**
     * Reference image 3's torso boxes, from the pass-2 review's own table, and the
     * numbers this reproduces are that table's.
     */
    private static final Rect REF_DARK_TORSO = Rect.ofCorners(190, 400, 300, 540);
    private static final Rect REF_PALE_TORSO = Rect.ofCorners(540, 400, 650, 540);

    /**
     * Boxes placed <em>inside</em> each duellist's torso, per image, for the
     * threshold-free statistic.
     *
     * <p>They have to be inside, and the review's wide boxes are not: through
     * {@code x300..470 y300..420} on the capture the threshold-free median reads 0.95 of
     * the ground on passes 2, 3 and 4 alike, because the box is three-quarters sky. The
     * thresholded statistic hid that by discarding every sky pixel; the threshold-free
     * one cannot, so the rectangle has to be honest. STYLE.md 11.3.
     */
    private static final Rect REF3_DARK = Rect.ofCorners(205, 415, 285, 520);
    private static final Rect REF3_PALE = Rect.ofCorners(555, 415, 635, 520);
    private static final Rect REF4_DARK = Rect.ofCorners(215, 395, 285, 505);
    private static final Rect REF4_PALE = Rect.ofCorners(535, 395, 605, 505);
    private static final Rect REF5_DARK = Rect.ofCorners(165, 395, 235, 505);
    private static final Rect REF5_PALE = Rect.ofCorners(445, 395, 515, 505);

    /** And the capture's, at the framing every {@code s4-p*-parry-contact} is shot at. */
    private static final Rect CAP_DARK_TORSO = Rect.ofCorners(385, 415, 465, 478);
    private static final Rect CAP_PALE_TORSO = Rect.ofCorners(645, 425, 720, 488);
    private static final Rect CAP_DARK_SKIRT = Rect.ofCorners(330, 500, 470, 620);
    private static final Rect CAP_PALE_SKIRT = Rect.ofCorners(620, 500, 760, 620);

    /**
     * What the capture has to reach on the threshold-free statistic.
     *
     * <p>A ratchet, set just under what pass 4 delivers (4.15x) so a regression fails
     * the build. The corpus reads 3.22x, 3.28x and 9.32x on images 4, 3 and 5, so the
     * delivered value is <em>inside</em> the corpus's own spread for the first time —
     * which is what the criterion is for and is stated here rather than celebrated,
     * because the dark duellist is still 2.5x too light against its own sky (0.314 of it
     * against the corpus's 0.115-0.134) and the ratio is carried by the pale one.
     */
    private static final double DELIVERED_FLOOR = 4.00;

    /** The corpus's own spread on the same statistic. */
    private static final double CORPUS_LOW = 3.20;

    @Test
    void everyFamilyBImageSeparatesItsTwoDuellistsByAtLeastAFactorOfThree() throws IOException {
        check("image 3", IMAGE3, REF3_DARK, REF3_PALE);
        check("image 4", IMAGE4, REF4_DARK, REF4_PALE);
        check("image 5", IMAGE5, REF5_DARK, REF5_PALE);
    }

    private static void check(String name, File file, Rect dark, Rect pale) throws IOException {
        assertTrue(file.isFile(), file + " is part of the repository and must be readable");
        Frame f = Frame.load(file);
        double d = CorridorProfile.medianOverGround(f, dark, STRIP);
        double p = CorridorProfile.medianOverGround(f, pale, STRIP);
        double ratio = p / d;
        assertTrue(ratio >= CORPUS_LOW,
                "STYLE.md 11.0: the corpus sets the criterion, so the whole corpus must pass it. "
                        + name + " reads " + describe(name, dark, d, pale, p, ratio));
        // And the dark one really is near-black against its own sky, which is the half
        // the capture misses. The corpus reads 0.115-0.142 here; note that on image 3
        // that is luminance 12 against STYLE.md 2.2's own #161A22 floor of 25.7 -- the
        // corpus breaks the floor to get it, and the debt records that.
        assertTrue(d < 0.16, name + "'s dark duellist reads " + d
                + " of the frame's own ground through " + dark.describe());
    }

    @Test
    void referenceImageThreeStillReadsThreeTwoSevenOnTheThresholdedStatistic() throws IOException {
        assertTrue(IMAGE3.isFile(), IMAGE3 + " is part of the repository");
        Frame f = Frame.load(IMAGE3);
        double dark = CorridorProfile.medianInkOverGround(f, REF_DARK_TORSO, FACTOR, STRIP);
        double pale = CorridorProfile.medianInkOverGround(f, REF_PALE_TORSO, FACTOR, STRIP);
        double ratio = pale / dark;
        // Every debt document since pass 2 quotes 3.27x through these rectangles. The
        // statistic is no longer the delivered gate, and it is still the number the
        // record is written in, so it stays checkable.
        assertTrue(ratio > 3.0 && ratio < 3.5,
                "the figure every document quotes is 3.27x; measured "
                        + describe("reference", REF_DARK_TORSO, dark, REF_PALE_TORSO, pale, ratio));
    }

    @Test
    void theTwoDuellistsAreTellableApartInDeliveredPixels() throws IOException {
        Assumptions.assumeTrue(CAPTURE.isFile(), CAPTURE + " is not on disk");
        Frame f = Frame.load(CAPTURE);
        double dark = CorridorProfile.medianOverGround(f, CAP_DARK_TORSO, STRIP);
        double pale = CorridorProfile.medianOverGround(f, CAP_PALE_TORSO, STRIP);
        double ratio = pale / dark;
        assertTrue(ratio >= DELIVERED_FLOOR,
                "Family B is a dark duellist against a pale one and this capture is a dark "
                        + "duellist against a slightly less dark one. The corpus reads "
                        + CORPUS_LOW + "x to 9.32x; this reads "
                        + describe("capture", CAP_DARK_TORSO, dark, CAP_PALE_TORSO, pale, ratio));
    }

    @Test
    void thePaleDuellistStillPoolsToTheFloorBelowTheSash() throws IOException {
        Assumptions.assumeTrue(CAPTURE.isFile(), CAPTURE + " is not on disk");
        Frame f = Frame.load(CAPTURE);
        double dark = CorridorProfile.medianInkOverGround(f, CAP_DARK_SKIRT, FACTOR, STRIP);
        double pale = CorridorProfile.medianInkOverGround(f, CAP_PALE_SKIRT, FACTOR, STRIP);
        double ratio = pale / dark;
        // The pass-2 skirt change is a protected result: reference image 3 reads 1.17x
        // below the sash -- its white-clad duellist is near-black there -- and pass 3's
        // capture read 1.63x, which the review verified. The failure mode to trap is a
        // *rising* ratio, not a falling one.
        assertTrue(ratio <= 1.75,
                "pass 2's skirt pooling is a protected result: reference image 3 reads 1.17x below "
                        + "the sash on this reader and pass 3's capture 1.63x. Measured "
                        + describe("capture skirt", CAP_DARK_SKIRT, dark, CAP_PALE_SKIRT, pale, ratio));
    }

    private static String describe(String what, Rect darkRect, double dark,
                                   Rect paleRect, double pale, double ratio) {
        return String.format(java.util.Locale.ROOT,
                "%s: dark %.3f through %s, pale %.3f through %s, ratio %.2fx",
                what, dark, darkRect.describe(), pale, paleRect.describe(), ratio);
    }
}
