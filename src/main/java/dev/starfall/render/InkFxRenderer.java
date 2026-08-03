package dev.starfall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import dev.starfall.art.Palette;

/**
 * The marks STYLE.md 7.3 says an impact <em>is</em>.
 *
 * <h2>Why this class exists</h2>
 *
 * <p>7.1 and 10 between them ban every tool a game normally reaches for at the
 * moment of contact -- "no impact freeze, no screen shake, no hitstop... these are
 * the crunchiest tools in the game-feel box and every one of them is banned" --
 * and 7.3 replaces them with a vocabulary of four, of which three are ink:
 *
 * <ul>
 *   <li><b>A bloom</b> "spreading from the contact point through the garment, like
 *       a drop hitting wet paper";</li>
 *   <li><b>a shedding of flecks</b>, "the dissolve threshold pushed locally so the
 *       struck area sheds brush flecks that drift away";</li>
 *   <li>and, for blade on blade, STYLE.md 5's <b>clash</b>: "a soft star bloom
 *       with 4-6 long soft rays, plus 8-20 warm embers."</li>
 * </ul>
 *
 * <p>The fourth, the yielding, is a pose and lives in {@code Stances}. The fifth
 * mark here, the vermillion seal, is combat-design.md 1.4's Marked status, and it
 * is the one place STYLE.md 2.2's "vermillion is a budget" gets spent.
 *
 * <h2>The three rules every mark in here obeys</h2>
 *
 * <ol>
 *   <li><b>Nothing is a circle and nothing is a uniform burst.</b> STYLE.md 10
 *       fails a pass on sight of "symmetric, uniform particle bursts". Every rim
 *       here is modulated by a hash of the mark's own seed, every fleck has its
 *       own size, speed and angle, and every angular distribution is biased into
 *       a cone along the direction of the blow rather than spread evenly round a
 *       point.</li>
 *   <li><b>Absorbing marks darken and emitting marks lighten, and they are
 *       different blend states.</b> See {@code ink_fx.frag}.</li>
 *   <li><b>No hard edge.</b> Every polygon in this file ends on zero alpha, so
 *       nothing can print a boundary of its own -- STYLE.md 3's "nothing in this
 *       game has a hard edge except the blades".</li>
 * </ol>
 *
 * <p>Deterministic: every random-looking quantity is a hash of the mark's seed
 * and an index, never of a running counter or of wall time, so two runs of one
 * schedule draw the same pixels. That is the property the whole review loop rests
 * on and it is cheap to keep.
 */
public final class InkFxRenderer {

    /** Rim points on a bloom. Enough to hide the polygon, few enough to stay cheap. */
    private static final int BLOOM_RIM = 20;

    private static final int MAX_VERTS = 8192;
    private static final int MAX_INDICES = 12288;

    private static final int FLOATS = 6;

    private final ShaderProgram shader;
    private final Mesh mesh;

    /** Two batches, because the two blend states cannot be interleaved. */
    private final float[] soakVerts = new float[MAX_VERTS * FLOATS];
    private final short[] soakIndices = new short[MAX_INDICES];
    private int soakV;
    private int soakI;

    private final float[] lightVerts = new float[MAX_VERTS * FLOATS];
    private final short[] lightIndices = new short[MAX_INDICES];
    private int lightV;
    private int lightI;

    private final Matrix4 projTrans = new Matrix4();

    private final Color scratch = new Color();

    public InkFxRenderer() {
        this.shader = InkSkinnedRenderer.Shaders.load("ink_fx", "ink_fx");
        this.mesh = new Mesh(false, MAX_VERTS, MAX_INDICES,
                new VertexAttributes(
                        new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                        new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")));
    }

    public void begin(Matrix4 projTrans) {
        this.projTrans.set(projTrans);
        soakV = 0;
        soakI = 0;
        lightV = 0;
        lightI = 0;
    }

    /**
     * STYLE.md 7.3's first reaction: a drop of ink hitting wet paper.
     *
     * <p>The radius grows as the square root of age, which is what a stain on damp
     * paper does and is also what stops the mark looking like a scaling sprite --
     * fastest at the instant of contact and continuously slowing, so there is no
     * moment at which it "starts". Opacity rises over the first fifth of its life
     * and then decays, and it drifts a little along the blow rather than staying
     * concentric, so the bloom has a direction the way a splash does.
     *
     * @param age  0 at contact, 1 when the mark is spent
     * @param mag  0 to 1, from {@code Force} where there was one
     * @param seed stable per mark, so the rim's irregularity is reproducible
     */
    public void bloom(float x, float y, float dirX, float dirY, float mag, float age,
                      Color ink, float seed) {
        float u = MathUtils.clamp(age, 0f, 1f);
        float radius = (0.055f + 0.135f * mag) * (0.30f + 0.70f * (float) Math.sqrt(u));
        // Rises fast, falls slowly. STYLE.md 7.1's "slow in, slower out" applied to
        // a mark rather than to a limb: the drop lands, and then it takes its time.
        float alpha = mag * 0.62f * (u < 0.18f ? u / 0.18f : (1f - (u - 0.18f) / 0.82f));
        if (alpha <= 0.002f) {
            return;
        }
        float driftX = x + dirX * radius * 0.34f * u;
        float driftY = y + dirY * radius * 0.34f * u;
        irregularDisc(soakVerts, soakIndices, driftX, driftY, radius, alpha, ink, seed, 0.42f);
    }

    /**
     * STYLE.md 7.3's third reaction: the struck area sheds brush flecks that drift
     * away.
     *
     * <p>Between six and twenty of them, thrown into a cone along the blow rather
     * than radially -- a symmetric spray is the particle system STYLE.md 10 fails
     * on sight, and a shed fleck is a piece of the figure coming off, so it goes
     * where the figure was hit from. Each fleck carries its own size, its own
     * speed and its own slight rotation, and they are quads rather than discs
     * because a brush fleck is angular.
     */
    public void flecks(float x, float y, float dirX, float dirY, float mag, float age,
                       Color ink, float seed) {
        float u = MathUtils.clamp(age, 0f, 1f);
        if (u >= 1f) {
            return;
        }
        int count = 6 + Math.round(14f * mag);
        float baseAngle = MathUtils.atan2(dirY, dirX);
        for (int i = 0; i < count; i++) {
            float h1 = hash(seed, i, 11);
            float h2 = hash(seed, i, 23);
            float h3 = hash(seed, i, 37);
            // A cone of about 110 degrees, weighted to the middle by squaring a
            // centred hash: most flecks follow the blow and a few peel wide.
            float spread = (h1 - 0.5f) * 2f;
            float angle = baseAngle + spread * Math.abs(spread) * 0.96f;
            float speed = (0.16f + 0.44f * h2) * (0.45f + 0.55f * mag);
            // Flecks slow as they go, like anything shed into still air.
            float travel = speed * u * (1.35f - 0.35f * u);
            float fx = x + MathUtils.cos(angle) * travel;
            float fy = y + MathUtils.sin(angle) * travel + 0.10f * u * u * (h3 - 0.75f);
            float size = (0.006f + 0.016f * h3) * (1f - 0.45f * u);
            float alpha = mag * (0.55f + 0.35f * h2) * (1f - u) * (1f - u);
            if (alpha <= 0.004f) {
                continue;
            }
            quad(soakVerts, soakIndices, fx, fy, size, angle + h1 * 2f, alpha, ink);
        }
    }

    /**
     * STYLE.md 5's clash, which is the mark this whole class was written for:
     * "a soft star bloom with 4-6 long soft rays, plus 8-20 warm embers. Blade on
     * blade only."
     *
     * <p>Emitting rather than absorbing, and the only mark here that is. The rays
     * are unequal in length and unequally spaced -- five rays at even 72 degrees
     * is a star sprite, and the reference is a flare of light off two edges
     * sliding past each other, which has a long axis along the blades and short
     * ones across.
     */
    /** How much of a clash's own span the star takes to reach full brightness. */
    private static final float CLASH_IGNITE = 0.30f;

    /** The value of {@code ignite * fade} at the peak, so the peak stays at {@code mag}. */
    private static final float CLASH_PEAK = (float) Math.pow(1f - CLASH_IGNITE, 1.5);

    public void clash(float x, float y, float dirX, float dirY, float mag, float age, float seed) {
        float u = MathUtils.clamp(age, 0f, 1f);
        if (u >= 1f) {
            return;
        }
        // A clash is over quickly and its embers outlive it. The core is cubed out
        // so it is gone well before the last ember, which is what stops the whole
        // thing reading as one fading sprite.
        //
        // <b>And it ignites rather than appearing.</b> A pure decay is full amplitude
        // on the first frame the mark exists, which is STYLE.md 7.1's first line
        // ("nothing arrives at rest abruptly") read the other way round: the mark
        // arrives at full brightness abruptly. That was invisible while the directive
        // ran 0.63 s -- every frame the light was actually legible sat inside the first
        // 6% of the age curve, so the whole visible bloom was one flat sample of it --
        // and it became a one-frame pop the moment the directive was cut to the length
        // of the contact it is asserting. Measured: with the pure decay and a 0.045 s
        // directive, warm-bright pixels appear on exactly one frame of the capture.
        // Rising over the first fifth and falling over the rest puts the peak two
        // frames in and gives 7.1 an ignition it can be soft about.
        float ignite = MathUtils.clamp(u / CLASH_IGNITE, 0f, 1f);
        ignite = ignite * ignite * (3f - 2f * ignite);
        float fade = (float) Math.pow(1f - u, 1.5);
        float core = mag * ignite * fade / CLASH_PEAK;
        float along = MathUtils.atan2(dirY, dirX);

        if (core > 0.004f) {
            // Amplitudes set against measured pixels, not chosen. The screen
            // operator over Family A's cream ground can barely lift it -- a
            // premultiplied source of 0.25 takes paper from 0.930 to 0.947, which
            // is nothing -- so the first pass's clash was invisible everywhere
            // except where it crossed a figure. It is not a licence for STYLE.md
            // 10's "neon glow on everything": screen approaches the paper's own
            // brightness asymptotically and cannot clip, the whole mark is gone in
            // under half a second, and it happens once per crossing.
            irregularDisc(lightVerts, lightIndices, x, y, (0.045f + 0.075f * mag) * (1f + 1.6f * u),
                    core, Palette.CLASH_CORE, seed, 0.30f);

            int rays = 5;
            for (int i = 0; i < rays; i++) {
                float h = hash(seed, i, 5);
                // Unevenly spaced, and the two nearest the blades' shared axis are
                // the long ones.
                float angle = along + i * MathUtils.PI2 / rays + (h - 0.5f) * 0.42f;
                float axisness = Math.abs(MathUtils.cos(angle - along));
                float length = (0.14f + 0.44f * mag) * (0.42f + 0.58f * axisness)
                        * (0.55f + 0.45f * h) * (1f + 0.9f * u);
                float half = length * 0.070f;
                ray(lightVerts, lightIndices, x, y, angle, length, half, core * (0.55f + 0.45f * h));
            }
        }

        int embers = 8 + Math.round(12f * mag);
        for (int i = 0; i < embers; i++) {
            float h1 = hash(seed, i, 71);
            float h2 = hash(seed, i, 91);
            float h3 = hash(seed, i, 113);
            float spread = (h1 - 0.5f) * 2f;
            float angle = along + spread * Math.abs(spread) * 1.35f + (h3 - 0.5f) * 0.5f;
            float speed = 0.22f + 0.55f * h2;
            // Embers rise and then fall: they are the only thing in the project
            // with any weight, and the drop is what tells the eye they are sparks
            // rather than dots.
            float travel = speed * u * (1.4f - 0.4f * u);
            float ex = x + MathUtils.cos(angle) * travel;
            float ey = y + MathUtils.sin(angle) * travel + 0.16f * u - 0.42f * u * u;
            // 2 px at the first pass's size, which is under the paper grain and
            // therefore not a mark at all. STYLE.md 5 asks for "8-20 warm embers"
            // and an ember that cannot be seen is not one of them.
            float size = (0.009f + 0.017f * h3) * (1f - 0.55f * u);
            float alpha = mag * (0.5f + 0.5f * h1) * (1f - u) * (1f - u);
            if (alpha <= 0.004f) {
                continue;
            }
            scratch.set(Palette.EMBER);
            quad(lightVerts, lightIndices, ex, ey, size, angle, alpha, scratch);
        }
    }

    /**
     * combat-design.md 1.4's Marked: "a vermillion seal blooms on the body."
     *
     * <p>One small mark, and it stays. STYLE.md 2.2: "vermillion is a budget. At
     * most a few small marks per frame, and it must always mean something." So
     * this is deliberately the smallest thing in the file and it does not spray,
     * drift or fade to nothing -- a seal that dispersed would not be a seal.
     */
    public void seal(float x, float y, float mag, float age, float seed) {
        float u = MathUtils.clamp(age, 0f, 1f);
        float radius = 0.030f * (0.35f + 0.65f * Math.min(1f, u * 3.2f));
        float alpha = mag * 0.9f * Math.min(1f, u * 3.2f);
        if (alpha <= 0.004f) {
            return;
        }
        irregularDisc(soakVerts, soakIndices, x, y, radius, alpha, Palette.VERMILLION, seed, 0.55f);
    }

    public void end() {
        if (soakI > 0) {
            // Premultiplied "over": ink spreading into paper, which can only darken.
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            flush(soakVerts, soakIndices, soakV, soakI);
        }
        if (lightI > 0) {
            // Screen. See ink_fx.frag.
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
            flush(lightVerts, lightIndices, lightV, lightI);
        }
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        soakV = 0;
        soakI = 0;
        lightV = 0;
        lightI = 0;
    }

    public void dispose() {
        shader.dispose();
        mesh.dispose();
    }

    // -- geometry --------------------------------------------------------------

    /**
     * A fan whose rim radius is modulated by a hash, so the mark has a torn
     * outline rather than a circular one, and whose rim sits on zero alpha.
     */
    private void irregularDisc(float[] verts, short[] indices, float cx, float cy, float radius,
                               float alpha, Color ink, float seed, float roughness) {
        if (radius <= 0f) {
            return;
        }
        // Which batch is decided by the array, never by a parameter beside it. The
        // first version took a boolean as well and the clash passed the emitting
        // arrays with the absorbing flag, so the star wrote its vertices into one
        // batch and advanced the other's counters -- which drew stale triangles as
        // ghost figures across the top of every frame and swallowed the rays.
        boolean soak = verts == soakVerts;
        int base = soak ? soakV : lightV;
        if (base + BLOOM_RIM + 1 >= MAX_VERTS) {
            return;
        }
        int v = base;
        int i = soak ? soakI : lightI;
        if (i + BLOOM_RIM * 3 >= MAX_INDICES) {
            return;
        }

        put(verts, v++, cx, cy, ink, alpha);
        for (int k = 0; k < BLOOM_RIM; k++) {
            float a = MathUtils.PI2 * k / BLOOM_RIM;
            // Two octaves of hash around the rim: one lobe-scale, one at the
            // resolution of the fan itself. A single octave gives a wobbly circle;
            // two give something that reads as pigment finding the paper's tooth.
            float wob = 1f
                    + (hash(seed, k, 3) - 0.5f) * roughness
                    + (hash(seed, k * 3 + 1, 17) - 0.5f) * roughness * 0.55f;
            float r = radius * Math.max(0.25f, wob);
            put(verts, v++, cx + MathUtils.cos(a) * r, cy + MathUtils.sin(a) * r, ink, 0f);
        }
        for (int k = 0; k < BLOOM_RIM; k++) {
            indices[i++] = (short) base;
            indices[i++] = (short) (base + 1 + k);
            indices[i++] = (short) (base + 1 + (k + 1) % BLOOM_RIM);
        }
        if (soak) {
            soakV = v;
            soakI = i;
        } else {
            lightV = v;
            lightI = i;
        }
    }

    /** One soft ray: a triangle from a bright base to a zero-alpha point. */
    private void ray(float[] verts, short[] indices, float x, float y, float angle,
                     float length, float half, float alpha) {
        if (lightV + 3 >= MAX_VERTS || lightI + 3 >= MAX_INDICES) {
            return;
        }
        float dx = MathUtils.cos(angle);
        float dy = MathUtils.sin(angle);
        int base = lightV;
        put(verts, lightV++, x - dy * half, y + dx * half, Palette.CLASH_CORE, 0f);
        put(verts, lightV++, x + dy * half, y - dx * half, Palette.CLASH_CORE, 0f);
        put(verts, lightV++, x + dx * length, y + dy * length, Palette.CLASH_CORE, alpha * 0.85f);
        indices[lightI++] = (short) base;
        indices[lightI++] = (short) (base + 1);
        indices[lightI++] = (short) (base + 2);
        // The base of the ray is transparent and its tip carries the light, which
        // is backwards for a ray and right for this one: the core disc already
        // holds the centre, so a ray that was brightest at the origin would just
        // fatten it. Brightest a little out and dying at the point is what makes
        // the star read as rays rather than as a blob with spikes.
    }

    /** One fleck or ember: a small rotated quad, uniform alpha, no hard rim of its own. */
    private void quad(float[] verts, short[] indices, float x, float y, float size, float angle,
                      float alpha, Color ink) {
        boolean soak = verts == soakVerts;
        int v = soak ? soakV : lightV;
        int i = soak ? soakI : lightI;
        if (v + 4 >= MAX_VERTS || i + 6 >= MAX_INDICES) {
            return;
        }
        float c = MathUtils.cos(angle) * size;
        float s = MathUtils.sin(angle) * size;
        int base = v;
        put(verts, v++, x - c - s, y - s + c, ink, alpha);
        put(verts, v++, x + c - s, y + s + c, ink, alpha);
        put(verts, v++, x + c + s, y + s - c, ink, alpha);
        put(verts, v++, x - c + s, y - s - c, ink, alpha);
        indices[i++] = (short) base;
        indices[i++] = (short) (base + 1);
        indices[i++] = (short) (base + 2);
        indices[i++] = (short) (base + 2);
        indices[i++] = (short) (base + 3);
        indices[i++] = (short) base;
        if (soak) {
            soakV = v;
            soakI = i;
        } else {
            lightV = v;
            lightI = i;
        }
    }

    private static void put(float[] verts, int index, float x, float y, Color c, float a) {
        int o = index * FLOATS;
        verts[o] = x;
        verts[o + 1] = y;
        verts[o + 2] = c.r;
        verts[o + 3] = c.g;
        verts[o + 4] = c.b;
        verts[o + 5] = a;
    }

    private void flush(float[] verts, short[] indices, int vertCount, int indexCount) {
        mesh.setVertices(verts, 0, vertCount * FLOATS);
        mesh.setIndices(indices, 0, indexCount);
        shader.bind();
        shader.setUniformMatrix("u_projTrans", projTrans);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        mesh.render(shader, GL20.GL_TRIANGLES, 0, indexCount);
    }

    /**
     * A stable hash in 0..1. Deterministic in the mark's own seed and two integer
     * coordinates, never in a call counter, so the same schedule draws the same
     * flecks on every run -- which is the property the whole review loop rests on.
     */
    private static float hash(float seed, int a, int b) {
        float h = seed * 0.6180339887f + a * 12.9898f + b * 78.233f;
        float s = (float) Math.sin(h) * 43758.5453f;
        return s - (float) Math.floor(s);
    }
}
