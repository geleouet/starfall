package dev.starfall.ui;

import dev.starfall.art.Palette;

/**
 * STYLE.md 6's fog, delivered as <b>bands</b>.
 *
 * <h2>Why this class exists, and what it replaces</h2>
 *
 * <p>STYLE.md 6's first bullet is the only line in the rubric marked
 * <i>non-negotiable</i>: <i>"horizontal drifting bands of {@code #D6D2CE} at
 * varying alpha that occlude the lower body of figures and separate depth layers.
 * This is present in every single reference image."</i> STYLE.md 9 then asks the
 * planning framing for <i>"heavy fog, Family C mood"</i>.
 *
 * <p>Pass 2 answered that with {@link Readout#haze(double)} -- an alpha attenuation
 * applied to the <em>figures</em> -- and the pass-2 review convicted it with
 * STYLE.md 11.0's matched-scale count: the graded planning frame fell from about
 * eight readable parts to about six, because <b>attenuating the subject removes
 * marks and adds none</b>. STYLE.md 11.0's corollary is that a material can only be
 * as good as the subject it paints, and thinning the subject is one worse than
 * refining the wrong thing.
 *
 * <p>So atmosphere is a <b>positive element</b> here. Every band is ink on the
 * sheet: it adds a readable part, it lies across the picture rather than inside the
 * figure, and the count goes up while the figure's contrast goes down -- which is
 * what a fog bank does to a body standing in it and what all eight reference images
 * show.
 *
 * <h2>Why it is drawn with the interface's own brush</h2>
 *
 * <p>Because STYLE.md 3b.4 says the figure, the ground and the paper are "the same
 * material event at different densities", and {@link Brush} is the one primitive in
 * this project that cannot print a boundary: a wash is a fan whose rim sits on zero
 * alpha, so a fog bank made of them has no edge anywhere, at any alpha. A band drawn
 * as a quad with a gradient would be a soft-edged rectangle, which is exactly the
 * construction {@code system5-debt.md} 1.3 records as defeating the interface's own
 * guards.
 *
 * <h2>It is world, not interface</h2>
 *
 * <p>The fog is part of the picture and therefore is drawn in the {@code -bare}
 * control as well. Every number about the interface in this project is a
 * {@code live - bare} difference at the same harness (STYLE.md 11.2b(g)), so putting
 * the fog on both sides is what keeps those differences measuring the interface. The
 * class sits beside the interface only because it shares {@link Brush.Sink}.
 */
public final class Fog {

    /** Which side of the figures a bank is drawn on. */
    public enum Layer {

        /**
         * Behind the bodies: the depth separation of STYLE.md 6's second bullet.
         * Laid high and thin, across the part of the frame the pass-2 review
         * measured as <i>"60% empty gradient"</i>.
         */
        FAR,

        /**
         * In front of the bodies, low: STYLE.md 6's <i>"occlude the lower body of
         * figures"</i>. Kept below the hem so it veils the dissolving lower third
         * the ink material already frays, rather than the head and the blade.
         */
        NEAR
    }

    /** How many bands a bank carries. Sparse: the corpus shows a handful, not a gradient. */
    private static final int BANDS = 6;

    /** How many washes are strung along one band. */
    private static final int LOBES = 11;

    /**
     * The strongest a single band is drawn, as a coverage. STYLE.md 2.1 allows Fog
     * up to 0.85; a bank of five overlapping bands at a fifth of that reads as air
     * rather than as a wall, and leaves the figures a value to be dark against.
     */
    private static final float BAND_ALPHA = 0.175f;

    /** World units a band drifts per second at its slowest. STYLE.md 6: "drifting". */
    private static final double DRIFT = 0.019;

    /** How much of the frame's width, at each side, the near bank fades out over. */
    private static final double MARGIN_FADE = 0.17;

    private Fog() {
    }

    /** 0 at the frame's edges, 1 across its middle, smooth in between. */
    private static double taper(double u) {
        double d = Math.min(u, 1.0 - u) / MARGIN_FADE;
        if (d <= 0) {
            return 0;
        }
        if (d >= 1) {
            return 1;
        }
        return d * d * (3 - 2 * d);
    }

    /**
     * One bank of bands across the visible world.
     *
     * @param t        seconds; the drift is a continuous function of it and of
     *                 nothing else, so two runs of one score draw one picture
     * @param centreX  where the camera is looking, in world units
     * @param centreY  the eye line, in world units
     * @param viewW    how much world the frame holds across
     * @param viewH    and vertically
     * @param strength 0 to 1: how much air the camera is looking through. Driven by
     *                 {@link Readout#haze(double)}, so it inherits
     *                 {@code Schedule.cameraIsContinuous} and cannot step.
     */
    public static void bank(Brush.Sink sink, Layer layer, double t, double centreX, double centreY,
                            double viewW, double viewH, double strength) {
        if (strength <= 0.0) {
            return;
        }
        double left = centreX - viewW * 0.5;
        // A band has to enter and leave the frame rather than beginning at its edge,
        // so it is drawn a lobe wider than the frame on both sides.
        double margin = viewW * 0.12;
        for (int i = 0; i < BANDS; i++) {
            float seed = (layer == Layer.FAR ? 137.7f : 211.3f) + i * 19.1f;
            // Stratified rather than drawn independently: one band per slice of the
            // range, jittered inside it. Six independent draws cluster, and a bank
            // that leaves half the frame empty is what pass 2's single horizon band
            // already was.
            double lift = (i + 0.15 + 0.70 * Brush.hash(seed, i, 3)) / BANDS;
            double y = layer == Layer.FAR
                    // The whole frame, because the depth separation of STYLE.md 6's
                    // second bullet is what the empty upper gradient and the empty
                    // near ground are both short of.
                    ? centreY + (lift - 0.30) * 0.80 * viewH
                    // Hem height and a little either side: the lower body, and
                    // nothing else. Never as high as the hands and never as low as
                    // the lane's own wash marks, because a fog that veils the
                    // interface's ground is not atmosphere, it is a curtain over the
                    // readout.
                    : 0.05 + 0.57 * lift;
            // A near band is nearer, so it is bigger. The same rule that makes the
            // reference images' foreground mist coarse and their distant mist fine.
            double thickness = viewH * (layer == Layer.FAR ? 0.018 + 0.026 * Brush.hash(seed, i, 7)
                    : 0.020 + 0.040 * Brush.hash(seed, i, 7));
            double speed = DRIFT * (0.4 + 1.6 * Brush.hash(seed, i, 11))
                    * (layer == Layer.FAR ? 0.55 : 1.0);
            double phase = t * speed;
            float alpha = (float) (BAND_ALPHA * strength
                    * (layer == Layer.FAR ? 0.62 : 1.0)
                    * (0.45 + 0.55 * Brush.hash(seed, i, 13)));
            if (alpha <= 0.004f) {
                continue;
            }
            for (int k = 0; k < LOBES; k++) {
                double u = k / (double) (LOBES - 1);
                double x = left - margin + u * (viewW + 2 * margin) + phase;
                // A band is not a rectangle of mist: it thins and gathers along its
                // own length, so the eye reads several lengths of vapour rather than
                // one stripe. Two octaves, both far above the pixel grid (3b.1).
                float body = 0.30f + 0.70f * Brush.hash(seed + 2.9f, k, 5);
                float ripple = 0.72f + 0.28f * Brush.hash(seed + 6.1f, k, 17);
                // <b>The near bank fades out at the sheet's two margins.</b> STYLE.md
                // 6's ground-plane bullet asks for wash that fades "to nothing at the
                // edges of the frame", and here it earns a second job: the interface's
                // two columns live in the outer eighth of the frame in every shot, and
                // a bank of mist drawn over them would veil a readout the countability
                // guard has to prove in delivered pixels. A band drifting through the
                // taper enters and leaves, which is what a bank of mist does anyway.
                if (layer == Layer.NEAR) {
                    double u2 = (x - phase - left) / viewW;
                    body *= (float) taper(u2);
                    if (body <= 0.01f) {
                        continue;
                    }
                }
                double rx = (viewW + 2 * margin) / (LOBES - 1) * (0.95 + 0.55 * body);
                double ry = thickness * (0.55 + 0.85 * ripple);
                Brush.wash(sink, (float) x, (float) (y + (Brush.hash(seed, k, 23) - 0.5) * thickness),
                        (float) rx, (float) ry, alpha * body, Palette.FOG,
                        seed + k * 3.7f, 0.62f);
            }
        }
    }
}
