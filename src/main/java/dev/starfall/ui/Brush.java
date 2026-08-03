package dev.starfall.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * The only mark-making primitive the interface has: a loaded brush drawn along a
 * path, and a pool of wash.
 *
 * <h2>Why the interface has a brush rather than a widget library</h2>
 *
 * <p>STYLE.md 8 forbids every shape a game UI is normally assembled out of --
 * boxes, bevels, bars with borders, drop shadows -- and STYLE.md 3 forbids the
 * thing they all have in common: <i>"nothing in this game has a hard edge except
 * the blades."</i> A rounded rectangle with a soft shadow is still a rectangle;
 * softening a widget does not make it a mark. So there is no primitive here that
 * <em>can</em> print a boundary. Every triangle this class emits carries at least
 * one vertex at exactly zero alpha, which is the property
 * {@code InterfaceInkTest.nothingInTheInterfaceHasAHardEdge} asserts over the
 * whole delivered frame, and it is the reason the assertion is cheap enough to
 * run over every mark rather than over a sample.
 *
 * <h2>Why the geometry is separated from the renderer</h2>
 *
 * <p>STYLE.md 11.2b(c): <i>"a cross-check must cross an implementation boundary,
 * not a call site."</i> The marks are built against {@link Sink}, of which
 * {@link InkUiRenderer} is one implementation and a recording sink in the test
 * suite is another, so a test can measure the delivered geometry without a GL
 * context and without re-deriving it from the same code that drew it.
 *
 * <p>Everything is deterministic in a per-mark seed and never in a call counter,
 * so two runs of one schedule draw the same strokes -- the property the whole
 * review loop rests on.
 */
public final class Brush {

    private Brush() {
    }

    /** Somewhere for a mark's triangles to go. */
    public interface Sink {

        /** @return the index of the vertex just written */
        int vertex(float x, float y, Color ink, float alpha);

        void triangle(int a, int b, int c);
    }

    /**
     * One loaded stroke along {@code (xs, ys)}.
     *
     * <p>Three rows of vertices -- one rim, the spine, the other rim -- and the two
     * rims sit on zero alpha, so the stroke has no flank. The spine's own alpha is
     * the <em>pressure</em>, which is zero at both ends: a brush lands and lifts,
     * it does not start and stop, so a stroke cannot print a cap either.
     *
     * <p>Pressure is heavy just after the landing and thins along the travel,
     * which is what makes a stroke read as having a direction -- and the direction
     * is what carries most of the glyph vocabulary in {@link Glyph}. On top of it
     * {@code dryness} takes the brush off the paper in places (STYLE.md 3.3, "ink
     * skips"), and at high values it is the whole of the Feint's mark: a gesture
     * that makes no contact is ink that never quite lands.
     *
     * @param width   full width of the stroke at full pressure, in the sink's units
     * @param alpha   peak ink density
     * @param dryness 0 for a wet stroke, up to about 0.8 for one that barely lands
     */
    public static void stroke(Sink sink, float[] xs, float[] ys, float width, float alpha,
                              Color ink, float seed, float dryness) {
        int n = xs.length;
        if (n < 2 || alpha <= 0f || width <= 0f) {
            return;
        }
        int[] left = new int[n];
        int[] spine = new int[n];
        int[] right = new int[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            float p = pressure(u) * skip(u, seed, dryness);
            float half = width * 0.5f * Math.max(0.18f, p);
            float nx = 0f;
            float ny = 0f;
            int a = Math.max(0, i - 1);
            int b = Math.min(n - 1, i + 1);
            float dx = xs[b] - xs[a];
            float dy = ys[b] - ys[a];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 1e-6f) {
                nx = -dy / len;
                ny = dx / len;
            }
            // STYLE.md 3.4: ink is darkest where it collects, and it collects at the
            // trailing end of a wash. The multiplier on the ink value is never below
            // 1.0 -- the pooling rail sits on the floor and everything else lifts off
            // it, which is the rule 3.4 states for a material already at the value
            // floor.
            Color tone = ink;
            left[i] = sink.vertex(xs[i] + nx * half, ys[i] + ny * half, tone, 0f);
            spine[i] = sink.vertex(xs[i], ys[i], tone, alpha * p);
            right[i] = sink.vertex(xs[i] - nx * half, ys[i] - ny * half, tone, 0f);
        }
        for (int i = 0; i + 1 < n; i++) {
            sink.triangle(left[i], spine[i], spine[i + 1]);
            sink.triangle(left[i], spine[i + 1], left[i + 1]);
            sink.triangle(spine[i], right[i], right[i + 1]);
            sink.triangle(spine[i], right[i + 1], spine[i + 1]);
        }
    }

    /**
     * A pool of wash: an ellipse with a torn rim, used for the ground of the Fold
     * of the World and for the impression a slot leaves on an empty sheet.
     *
     * <p>Same contract as a stroke -- the rim is on zero alpha and the mark is
     * therefore edgeless -- and the rim radius is modulated by two octaves of a
     * hash of the mark's own seed, because a wobbly circle is still a circle and
     * STYLE.md 10 fails a pass on sight of anything that reads as a shape
     * primitive.
     */
    public static void wash(Sink sink, float cx, float cy, float rx, float ry, float alpha,
                            Color ink, float seed, float roughness) {
        if (alpha <= 0f || rx <= 0f || ry <= 0f) {
            return;
        }
        // Five lobes of smooth noise around the rim, wrapped, plus a fifth of that
        // again at twice the rate.
        //
        // <b>The frequency is the whole of it, and the first version had it at the
        // resolution of the fan.</b> An independent draw per rim vertex makes a
        // star, not a pool -- delivered, the lane printed as a row of small
        // starbursts, which is STYLE.md 10's "symmetric, uniform particle bursts"
        // arriving from the opposite direction. A wash has a torn outline at the
        // scale of the wash, not at the scale of its triangles.
        int rim = 28;
        int lobes = 5;
        int centre = sink.vertex(cx, cy, ink, alpha);
        int[] ring = new int[rim];
        for (int k = 0; k < rim; k++) {
            double a = Math.PI * 2 * k / rim;
            float wob = 1f
                    + (ringNoise(seed, k, rim, lobes) - 0.5f) * roughness
                    + (ringNoise(seed + 4.3f, k, rim, lobes * 2) - 0.5f) * roughness * 0.35f;
            float r = Math.max(0.30f, wob);
            ring[k] = sink.vertex(cx + (float) Math.cos(a) * rx * r,
                    cy + (float) Math.sin(a) * ry * r, ink, 0f);
        }
        for (int k = 0; k < rim; k++) {
            sink.triangle(centre, ring[k], ring[(k + 1) % rim]);
        }
    }

    /**
     * The pressure a brush puts on the paper along a stroke.
     *
     * <p>Zero at both ends, so nothing can print a cap; peaked early, so the mark
     * has a head and a tail and the eye can tell which way it was drawn. The
     * exponent is under one, which keeps the body of the stroke broad and confines
     * the taper to the ends -- a sine to the first power gives a lens, which is a
     * shape, not a stroke.
     *
     * <p><b>0.55 rather than pass 1's 0.30, and the difference is a landing.</b> At
     * 0.30 the curve reaches 0.76 of full pressure by the first sample of a
     * nine-point stroke, so on a mark that is only fifteen or twenty pixels long the
     * brush arrives inside a single pixel: measured through the interface's own
     * raster, the landing of a short stroke was the steepest thing several marks
     * printed, at up to 0.42 of the frame's amplitude. The docstring above already
     * claimed a stroke "cannot print a cap"; at 0.30 it could, whenever the stroke
     * was short. The body is still broad -- at the quarter point the two exponents
     * differ by 9% -- and what changes is only how the brush touches down.
     */
    static float pressure(float u) {
        float s = (float) Math.sin(Math.PI * Math.max(0f, Math.min(1f, u)));
        return (float) Math.pow(s, 0.55) * (1f - 0.30f * u);
    }

    /**
     * STYLE.md 3.3's dry-brush breakup: coverage is not uniform, ink skips.
     *
     * <p><b>The frequency matters more than the amplitude, and the first version had
     * it wrong.</b> Sampling the hash at the resolution of the polyline made every
     * vertex an independent draw, so a nine-point stroke came out as a row of beads
     * -- which is a texture, not a skip, and it is the same class of defect
     * STYLE.md 3's own note traces to "an octave whose period lands near the pixel
     * grid". The breakup is now a smooth value noise over six spans of the stroke
     * however many points it has, so a skip is a length of paper the brush missed
     * and reads as one.
     */
    static float skip(float u, float seed, float dryness) {
        float f = Math.max(0f, Math.min(1f, u)) * 6f;
        int k = (int) f;
        float frac = f - k;
        frac = frac * frac * (3f - 2f * frac);
        float g = hash(seed, k, 5) * (1f - frac) + hash(seed, k + 1, 5) * frac;
        float wet = 0.84f + 0.16f * g;
        if (dryness <= 0.02f) {
            return wet;
        }
        float t = (g - dryness * 0.95f) / 0.22f;
        return wet * Math.max(0f, Math.min(1f, t));
    }

    /** Smooth noise around a closed ring, so a rim's outline joins where it started. */
    private static float ringNoise(float seed, int k, int rim, int lobes) {
        float f = k / (float) rim * lobes;
        int i = (int) f;
        float frac = f - i;
        frac = frac * frac * (3f - 2f * frac);
        float a = hash(seed, i % lobes, 3);
        float b = hash(seed, (i + 1) % lobes, 3);
        return a * (1f - frac) + b * frac;
    }

    /**
     * A stable hash in 0..1, in the mark's own seed and two integers -- never in a
     * call counter or in wall time, so the same interface state draws the same
     * pixels on every run.
     */
    static float hash(float seed, int a, int b) {
        float h = seed * 0.6180339887f + a * 12.9898f + b * 78.233f;
        float s = (float) Math.sin(h) * 43758.5453f;
        return s - (float) Math.floor(s);
    }
}
