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
 *       game has a hard edge except the blades". <b>That sentence was false for
 *       three marks until System 4 pass 5</b> -- the shed flecks, the embers and
 *       the clash rays -- and it is the kind of false a comment is worst at
 *       catching, because the two marks it was wrong about are the two drawn most
 *       often and nearest the eye. See {@link #FLECK_RIM} and {@link #ray}.</li>
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

    /**
     * Rim points on a single shed fleck or ember.
     *
     * <p>Seven rather than four, and the difference is the whole of System 4 pass 5's
     * first item. Through pass 4 a fleck was a rotated quad of <em>uniform</em> alpha,
     * and the pass-4 review measured what that prints: blobs at (555,343), (562,349),
     * (570,349) and (549,353) on frame 11 of {@code s4-p4-parry-contact}, 7x7 and 8x8
     * px, <b>bounding-box fill 0.78 to 1.00</b>, all within a pixel of the same size,
     * 60 px from the focal point of the frame. Filled axis-aligned quadrilaterals are
     * STYLE.md 10's "hard-edged sprites" and STYLE.md 3's first line -- "nothing in
     * this game has a hard edge except the blades" -- and the class comment below
     * claimed the opposite: <i>"every polygon in this file ends on zero alpha"</i>.
     * It was true of every mark here except the two that are drawn most often.
     */
    private static final int FLECK_RIM = 7;

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
     * speed and its own slight rotation.
     *
     * <p><b>Sizes are spread over a cube rather than a line (pass 5).</b> STYLE.md 3's
     * failure-signature list names "flecks that are all the same size", and a size
     * drawn as {@code a + b * h} for uniform {@code h} puts most of the population in
     * the middle of its range: the pass-4 review found five blobs by the clash "all
     * within a pixel of the same size". Cubing the hash and widening the range gives
     * a few large marks and many small ones, which is what a brush shedding pigment
     * actually leaves.
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
            float size = (0.0035f + 0.026f * h3 * h3 * h3) * (1f - 0.45f * u);
            float alpha = mag * (0.55f + 0.35f * h2) * (1f - u) * (1f - u);
            if (alpha <= 0.004f) {
                continue;
            }
            // Elongated along its own flight, because a fleck thrown off a wet
            // surface leaves a dab with a direction, not a dot.
            fleck(soakVerts, soakIndices, fx, fy, size, angle + h1 * 2f,
                    1.25f + 0.9f * h2, alpha, ink, seed + i * 1.7f);
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

            // <b>The hot core, and the dusk sky is what exposed the need for it.</b>
            // Screen adds {@code src * (1 - dst)}, so the level this mark reaches is
            // a function of what it is drawn over. Over Family A cream (dst 0.86) the
            // amplitude above takes the centre past luminance 240 and the pass-2 and
            // pass-3 reviews both graded the bloom through a {@code L >= 240} mask.
            // Over the Family B sky (dst 0.35 at the crossing's own height) the
            // identical mark peaks at 213: measured on the first dusk capture, the
            // warm-bright core fell from 152/276/111 px on frames 9/10/11 to 55 px on
            // frame 9 and nothing at all on the rest. The light did not get weaker --
            // the ground got darker and a mark that only lifts the ground by a
            // proportion cannot reach white on a dark one.
            //
            // STYLE.md 2.2 is explicit that this is the one mark allowed to: "only
            // the clash bloom and blade specular may approach white". So the innermost
            // third of the disc is drawn again at a saturating amplitude, which makes
            // the core's own value a property of the light rather than of the sky
            // behind it. The soft halo above is untouched, so nothing acquires an
            // edge, and on cream the extra disc is inside a region that was already
            // near the ceiling -- screen cannot clip.
            irregularDisc(lightVerts, lightIndices, x, y, (0.045f + 0.075f * mag) * (1f + 1.6f * u) * 0.34f,
                    Math.min(1f, core * 2.6f), Palette.CLASH_CORE, seed + 5.5f, 0.22f);

            // Four to six, per STYLE.md 5, and the count itself is a hash of the
            // mark rather than a constant: five rays every time is a star sprite
            // however unequal their lengths are.
            int rays = 4 + Math.round(2f * hash(seed, 0, 61));
            for (int i = 0; i < rays; i++) {
                float h = hash(seed, i, 5);
                float h2 = hash(seed, i, 43);
                // Unevenly spaced, and the two nearest the blades' shared axis are
                // the long ones.
                float angle = along + i * MathUtils.PI2 / rays + (h - 0.5f) * 0.62f;
                float axisness = Math.abs(MathUtils.cos(angle - along));
                // Lengths spread over a square, for the reason the flecks are: a
                // linear spread clusters, and rays of nearly equal length print the
                // "symmetric, uniform particle burst" STYLE.md 10 fails on sight.
                // Pass 4's five rays ran 0.55x to 1.00x of each other; these run
                // 0.375x to 1.00x, and the count varies too.
                float length = (0.14f + 0.44f * mag) * (0.42f + 0.58f * axisness)
                        * (0.45f + 0.75f * h * h) * (1f + 0.9f * u);
                float half = length * (0.075f + 0.055f * h2);
                ray(x, y, angle, length, half, core * (0.45f + 0.55f * h2), h2);
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
            float size = (0.005f + 0.030f * h3 * h3 * h3) * (1f - 0.55f * u);
            float alpha = mag * (0.5f + 0.5f * h1) * (1f - u) * (1f - u);
            if (alpha <= 0.004f) {
                continue;
            }
            scratch.set(Palette.EMBER);
            // Smeared along its own drift. An ember at rest is a dot; an ember
            // floating like paper ash (STYLE.md 5) has a long axis.
            fleck(lightVerts, lightIndices, ex, ey, size, angle,
                    1.4f + 1.1f * h2, alpha, scratch, seed + i * 2.3f);
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

    /** Stations along one ray's spine. Four gaps is enough to bend it and to fade it. */
    private static final int RAY_STATIONS = 5;

    /**
     * The peak of the spindle's own alpha profile, so replacing the triangle did not
     * dim the star.
     *
     * <p>The pass-4 triangle carried {@code 0.85 * alpha} at its single tip vertex.
     * The spindle's profile {@code min(1, t/0.15) * (1-t)^1.6} peaks at 0.771, so an
     * unscaled swap would dim the star for no reason. Dividing by the profile's own
     * peak keeps the mark's brightness a property of {@code core}, which is where the
     * amplitude reasoning in {@link #clash} lives, rather than an accident of the shape.
     *
     * <p><b>The exponent is 1.6 rather than a cube, and that is a measured correction
     * inside this pass.</b> The first probe faded the spindle as {@code (1-t)^3} and
     * shortened the length distribution at the same time; at 4x on frame 11 the star
     * had a core and <em>no visible rays at all</em>, which trades one STYLE.md 5 miss
     * for another -- 5 asks for "4-6 long soft rays". A cube puts 95% of the light
     * inside the first third of the ray. 1.6 still reaches exactly zero at the point,
     * which is the whole reason for the shape, and carries legible light to about 80%
     * of the length.
     */
    private static final float RAY_PEAK = 0.771f / 0.85f;

    /**
     * One soft ray: a tapering spindle whose whole boundary sits on zero alpha.
     *
     * <h2>What this replaces, and why it is item 1 of the pass-5 brief</h2>
     *
     * <p>Through pass 4 a ray was <em>one triangle</em>: two zero-alpha vertices at
     * the base and a single vertex at the tip carrying {@code alpha * 0.85}. Read
     * along the spine that is a soft ramp, which is what it was written for -- but
     * read <em>across</em> it, the alpha at the tip is constant to the last pixel and
     * then stops. The triangle therefore prints two straight aliased edges converging
     * on a hard terminating point, which is STYLE.md 10's "hard-edged sprites /
     * visible polygon silhouettes" drawn at the focal point of the frame.
     *
     * <p>The pass-4 review measured it against reference image 3's own clash box at
     * matched scale: single-pixel luminance steps of <b>max 134</b> and 0.021% of
     * pixels stepping over 100, where the reference has a max of 76 and <b>nothing</b>
     * over 100.
     *
     * <p>So the ray is now a strip of {@link #RAY_STATIONS} stations. Every station
     * carries a centre vertex with the light and two flank vertices at zero alpha, and
     * the last station has both zero width and zero alpha -- so there is no boundary
     * anywhere on the mark, lateral or terminal, that is not a fade to nothing. The
     * spine also bows: a straight ray is a polygon, and a hand-drawn flare is not.
     *
     * <p>The light still peaks a little way out rather than at the origin, which is
     * what the pass-4 comment got right and is kept: the core disc already holds the
     * centre, so a ray brightest at its base would only fatten it.
     */
    private void ray(float x, float y, float angle, float length, float half,
                     float alpha, float bow) {
        if (lightV + RAY_STATIONS * 3 >= MAX_VERTS || lightI + (RAY_STATIONS - 1) * 12 >= MAX_INDICES) {
            return;
        }
        float dx = MathUtils.cos(angle);
        float dy = MathUtils.sin(angle);
        // Perpendicular, and the spine's own sideways drift along it. Signed off the
        // hash so rays bow both ways within one star.
        float curve = (bow - 0.5f) * 0.55f * length;
        int base = lightV;
        for (int k = 0; k < RAY_STATIONS; k++) {
            float t = k / (float) (RAY_STATIONS - 1);
            // Width: a little narrower at the throat than at the shoulder, then to
            // nothing at the point. Not linear, so the silhouette is a spindle.
            float w = half * (0.85f + 1.05f * t * (1f - t) * 2f) * (1f - t) * (1f - t * 0.35f);
            // Light: in over the first sixth, then out as a soft power so the last
            // station is unambiguously zero rather than a small residue that would
            // print the strip's own end.
            float rise = Math.min(1f, t / 0.15f);
            float fall = (float) Math.pow(1f - t, 1.6);
            float a = Math.min(1f, alpha * rise * fall / RAY_PEAK);
            float off = curve * t * t;
            float cx = x + dx * length * t - dy * off;
            float cy = y + dy * length * t + dx * off;
            put(lightVerts, lightV++, cx - dy * w, cy + dx * w, Palette.CLASH_CORE, 0f);
            put(lightVerts, lightV++, cx, cy, Palette.CLASH_CORE, a);
            put(lightVerts, lightV++, cx + dy * w, cy - dx * w, Palette.CLASH_CORE, 0f);
        }
        for (int k = 0; k < RAY_STATIONS - 1; k++) {
            int p = base + k * 3;
            int q = p + 3;
            // Left panel, then right panel, each as two triangles.
            tri(p, p + 1, q + 1);
            tri(p, q + 1, q);
            tri(p + 1, p + 2, q + 2);
            tri(p + 1, q + 2, q + 1);
        }
    }

    private void tri(int a, int b, int c) {
        lightIndices[lightI++] = (short) a;
        lightIndices[lightI++] = (short) b;
        lightIndices[lightI++] = (short) c;
    }

    /**
     * One shed fleck or ember: a small irregular fan, elongated along its own
     * flight, whose rim sits on zero alpha.
     *
     * <h2>What this replaces</h2>
     *
     * <p>A rotated quad of uniform alpha -- four vertices all carrying the mark's
     * full value, so every one of its four edges was a step from the mark's own
     * brightness to the ground's. At the sizes these are drawn (6 to 17 px across at
     * the intimate framing) the rotation does not save it: the pass-4 review read
     * bounding-box fills of 0.78 to 1.00 and called them, correctly, "filled
     * axis-aligned quadrilaterals... the single most damaging object in the picture
     * because of where it sits".
     *
     * <p>The same mark as a fan costs three more vertices and has no boundary at all:
     * the centre carries the light and every rim point is zero. The rim radius is
     * hashed per point so no two flecks share an outline, and the ellipse is stretched
     * along the direction of travel so the mark reads as a dab with a direction rather
     * than as a disc.
     */
    private void fleck(float[] verts, short[] indices, float x, float y, float size,
                       float angle, float stretch, float alpha, Color ink, float seed) {
        boolean soak = verts == soakVerts;
        int base = soak ? soakV : lightV;
        int i = soak ? soakI : lightI;
        if (base + FLECK_RIM + 1 >= MAX_VERTS || i + FLECK_RIM * 3 >= MAX_INDICES) {
            return;
        }
        int v = base;
        float c = MathUtils.cos(angle);
        float s = MathUtils.sin(angle);
        put(verts, v++, x, y, ink, alpha);
        for (int k = 0; k < FLECK_RIM; k++) {
            float a = MathUtils.PI2 * k / FLECK_RIM;
            float wob = 0.60f + 0.80f * hash(seed, k, 29);
            float lx = MathUtils.cos(a) * size * stretch * wob;
            float ly = MathUtils.sin(a) * size * wob;
            put(verts, v++, x + c * lx - s * ly, y + s * lx + c * ly, ink, 0f);
        }
        for (int k = 0; k < FLECK_RIM; k++) {
            indices[i++] = (short) base;
            indices[i++] = (short) (base + 1 + k);
            indices[i++] = (short) (base + 1 + (k + 1) % FLECK_RIM);
        }
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
