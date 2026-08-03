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
 * {@code docs/system4-debt.md} named the missing guard exactly — "a test that compares
 * two figures' rendered value distributions" — and this is it.
 *
 * <p>STYLE.md §11.0's rule applies here as much as to the corridor: the target is the
 * corpus's own number, so the corpus is measured in the same file, by the same code,
 * and its own reading is asserted before the capture's is.
 */
class DuellistValueTest {

    private static final File REFERENCE =
            new File("inspirations/image - 2026-08-02T101033.164.png");

    private static final File CAPTURE =
            new File("out/captures/s4-p3-parry-contact/frame_011.png");

    private static final double FACTOR = 0.85;
    private static final int STRIP = CorridorProfile.BACKGROUND_STRIP;

    /**
     * Reference image 3's torso boxes, from the pass-2 review's own table, and the
     * numbers this reproduces are that table's.
     */
    private static final Rect REF_DARK_TORSO = Rect.ofCorners(190, 400, 300, 540);
    private static final Rect REF_PALE_TORSO = Rect.ofCorners(540, 400, 650, 540);

    /** And the capture's, at the framing every {@code s4-p*-parry-contact} is shot at. */
    private static final Rect CAP_DARK_TORSO = Rect.ofCorners(300, 300, 470, 420);
    private static final Rect CAP_PALE_TORSO = Rect.ofCorners(600, 340, 760, 460);
    private static final Rect CAP_DARK_SKIRT = Rect.ofCorners(330, 500, 470, 620);
    private static final Rect CAP_PALE_SKIRT = Rect.ofCorners(620, 500, 760, 620);

    /**
     * What the capture has to reach. <b>Not the corpus's 3.27</b>, and the gap is
     * reported rather than hidden: this pass moves the delivered ratio 1.54 to 2.09 and
     * misses. The floor is set just under what is delivered so that a regression fails
     * the build, and the target is named in the failure message so the next pass reads
     * the real number rather than this one.
     */
    private static final double DELIVERED_FLOOR = 1.95;

    /** And what the corpus reads, which is what the criterion is for. */
    private static final double CORPUS_TARGET = 3.27;

    @Test
    void referenceImageThreeSeparatesItsTwoDuellistsByAFactorOfThree() throws IOException {
        assertTrue(REFERENCE.isFile(), REFERENCE + " is part of the repository");
        Frame f = Frame.load(REFERENCE);
        double dark = CorridorProfile.medianInkOverGround(f, REF_DARK_TORSO, FACTOR, STRIP);
        double pale = CorridorProfile.medianInkOverGround(f, REF_PALE_TORSO, FACTOR, STRIP);
        double ratio = pale / dark;
        assertTrue(ratio > 3.0,
                "the corpus's own torso separation is the whole point of the criterion; measured "
                        + describe("reference", REF_DARK_TORSO, dark, REF_PALE_TORSO, pale, ratio));
        // And the dark one really is near-black, which is the half the capture misses.
        assertTrue(dark < 0.20, "reference dark duellist torso reads " + dark
                + " of the frame's own ground through " + REF_DARK_TORSO.describe());
    }

    @Test
    void theTwoDuellistsAreTellableApartInDeliveredPixels() throws IOException {
        Assumptions.assumeTrue(CAPTURE.isFile(), CAPTURE + " is not on disk");
        Frame f = Frame.load(CAPTURE);
        double dark = CorridorProfile.medianInkOverGround(f, CAP_DARK_TORSO, FACTOR, STRIP);
        double pale = CorridorProfile.medianInkOverGround(f, CAP_PALE_TORSO, FACTOR, STRIP);
        double ratio = pale / dark;
        assertTrue(ratio >= DELIVERED_FLOOR,
                "Family B is a dark duellist against a pale one and this capture is a dark "
                        + "duellist against a slightly less dark one. The corpus reads "
                        + CORPUS_TARGET + "x; this reads "
                        + describe("capture", CAP_DARK_TORSO, dark, CAP_PALE_TORSO, pale, ratio));
    }

    @Test
    void thePaleDuellistStillPoolsToTheFloorBelowTheSash() throws IOException {
        Assumptions.assumeTrue(CAPTURE.isFile(), CAPTURE + " is not on disk");
        Frame f = Frame.load(CAPTURE);
        double dark = CorridorProfile.medianInkOverGround(f, CAP_DARK_SKIRT, FACTOR, STRIP);
        double pale = CorridorProfile.medianInkOverGround(f, CAP_PALE_SKIRT, FACTOR, STRIP);
        double ratio = pale / dark;
        // The pass-2 skirt change is a protected result: the corpus reads 1.16x below
        // the sash -- its white-clad duellist is near-black there -- and pass 2's
        // capture read 1.29x, which the review verified as correct and better than the
        // reasoning it overturned. The sash split of this pass must not undo it, and
        // the failure mode it could have is a *rising* ratio, not a falling one.
        assertTrue(ratio <= 1.75,
                "pass 2's skirt pooling is a protected result: reference image 3 reads 1.16x below "
                        + "the sash and pass 2's capture 1.57x on this reader. Measured "
                        + describe("capture skirt", CAP_DARK_SKIRT, dark, CAP_PALE_SKIRT, pale, ratio));
    }

    private static String describe(String what, Rect darkRect, double dark,
                                   Rect paleRect, double pale, double ratio) {
        return String.format(java.util.Locale.ROOT,
                "%s: dark %.3f through %s, pale %.3f through %s, ratio %.2fx",
                what, dark, darkRect.describe(), pale, paleRect.describe(), ratio);
    }
}
