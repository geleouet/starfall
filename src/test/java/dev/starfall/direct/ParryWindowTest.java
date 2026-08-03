package dev.starfall.direct;

import dev.starfall.stage.Framing;
import dev.starfall.stage.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The figure height of the graded parry window, in delivered rows, computed from the
 * scene rather than detected in it.
 *
 * <h2>Why a span has to be given, and why it has to be checked in</h2>
 *
 * <p>STYLE.md §11.3 makes every pixel number on a moving-camera scene "a ratio to the
 * figure's own span", which makes the span a load-bearing half of every measurement in
 * this project. The pass-3 review found seven pixels of auto-detected span moving a band
 * reading by <b>67% of itself</b> between two readers, and asked for the capture to be
 * given an explicit one as the corpus already is.
 *
 * <p>From System 4 pass 4 the duel is fought against the Family B dusk sky, and
 * detecting it is no longer merely unstable — it is wrong. The ground is now itself a
 * dark ink smear (STYLE.md §1: "Ground is a dark ink smear with grass strokes"), so the
 * largest ink component runs from the head down into it and out to both frame edges:
 * measured on {@code s4-p4-parry-contact} frame 11 it spans {@code x17..699 y320..719},
 * 683 px wide. Detected figure heights over the window's 24 frames range 219..226 px
 * against a true 329.
 *
 * <p>So the span quoted in {@code docs/system4-debt.md} and passed to
 * {@code analyse ... --span 0,299,960,378} is derived here, from
 * {@code Schedule.framingAt} and {@code Stage.FIGURE_HEIGHT} through the same camera
 * arithmetic {@code DuelScene.aim} runs, and asserted so it cannot rot silently when a
 * framing constant moves.
 */
class ParryWindowTest {

    /** The graded window: {@code -Pstart=1.42 -Pstep=0.0167 -Pframes=24 -Pw=960 -Ph=720}. */
    private static final double START = 1.42;
    private static final double STEP = 0.0167;
    private static final int FRAMES = 24;
    private static final int W = 960;
    private static final int H = 720;

    /** {@code EYE} in {@code DuelScene}: where the camera looks, as a fraction of frame height. */
    private static final double EYE = 0.44;

    /**
     * The span every {@code s4-p5-*} parry measurement is normalised by.
     *
     * <p><b>It moved in pass 5 and the move is the point of the assertion.</b>
     * {@code Director.LANE_SPREAD} went 1.55 to 1.35, and the camera framing is
     * stretched by the same factor ({@code Director.stretchTiles}), so the intimate
     * shot is 13% narrower and the same 1.70-unit figure lands on <b>378</b> rows
     * rather than 329. Every ratio in {@code docs/system4-debt.md} is taken through
     * whichever of the two the capture was shot at, and each is labelled with it.
     *
     * <p>The feet row does not move: {@code rowOfGround} works out to
     * {@code (0.5 + EYE) * H} whatever the zoom, so world y = 0 sits at row 677 at
     * both spreads. Only the crown rises, 348 to 299.
     */
    static final int SPAN_TOP = 299;
    static final int SPAN_HEIGHT = 378;

    @Test
    void theGradedParryWindowPutsAFigureHeightAt378Rows() {
        Rehearsal r = new Rehearsal(Duel.Kind.PARRY);
        r.play();
        double minH = Double.MAX_VALUE;
        double maxH = 0;
        double minTop = Double.MAX_VALUE;
        double maxTop = 0;
        for (int i = 0; i < FRAMES; i++) {
            double t = START + i * STEP;
            Framing f = r.schedule().framingAt(t);
            double worldW = Director.stretchTiles(f.widthTiles());
            double worldH = worldW * H / (double) W;
            double centreY = worldH * EYE;
            double yMin = centreY - worldH / 2;
            double rowOfGround = (1 - (0.0 - yMin) / worldH) * H;
            double rowOfCrown = (1 - (Stage.FIGURE_HEIGHT - yMin) / worldH) * H;
            minH = Math.min(minH, rowOfGround - rowOfCrown);
            maxH = Math.max(maxH, rowOfGround - rowOfCrown);
            minTop = Math.min(minTop, rowOfCrown);
            maxTop = Math.max(maxTop, rowOfCrown);
        }
        // The camera drifts through the window -- STYLE.md §9 requires it never to be
        // still -- so the span is the window's own mean and the drift is asserted to be
        // small enough that one span serves all 24 frames.
        assertTrue(maxH - minH < 4, "the framing moves " + (maxH - minH)
                + " rows of figure height across the graded window; one span no longer serves it");
        assertEquals(SPAN_HEIGHT, Math.round((minH + maxH) / 2), 2,
                "docs/system4-debt.md normalises every s4-p5 parry number by " + SPAN_HEIGHT
                        + " rows; the scene now says " + (minH + maxH) / 2);
        assertEquals(SPAN_TOP, Math.round((minTop + maxTop) / 2), 2,
                "the crown row quoted with that span is " + SPAN_TOP + "; the scene now says "
                        + (minTop + maxTop) / 2);
    }
}
