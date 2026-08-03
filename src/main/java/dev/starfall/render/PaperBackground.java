package dev.starfall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.starfall.art.Palette;

/**
 * The warm paper ground of STYLE.md section 2, with the drifting fog bands of
 * section 6 -- contract section F.
 *
 * <p>Everything is procedural; the work here is only to hand the shaders a quad
 * that exactly covers the visible world rect, plus the rect itself. The rect
 * comes from inverting the camera matrix rather than from the caller passing
 * viewport numbers, because the contract fixes render() to take nothing but the
 * matrix and the clock -- and because it means the paper grain stays locked to
 * world space when the camera pushes in (STYLE.md 9), so the sheet does not
 * slide under the figure during a camera move.
 *
 * <h2>Revision 3 -- the mist (rig-fixes-3 item 5)</h2>
 *
 * Measured down x=250 in pass 2, the lower frame lost 55 luminance units and 60%
 * of its warmth as it descended: a neutral-grey subtractive gradient, which is
 * the definition of a muddy fog bank. In references 6, 7, 8 and the lower third
 * of 1 and 2 the mist is the <em>lightest and warmest</em> region in the frame.
 * It is what the figures dissolve into -- an emitter, not a haze.
 *
 * <p>So the ground pass now runs with its own fog bands switched off, and a
 * second additive-ish pass paints them back as warm luminous bands, broken into
 * drifting lobes so that two to four separate depth layers read rather than one
 * monotonic ramp. The band <em>geometry</em> still comes from
 * {@link Atmosphere}, unchanged and shared, so the mist the figure fades into
 * inside {@code ink_resolve.frag} is the same mist the paper shows -- that
 * sharing is what contract section F requires, and nothing here is a second
 * authoring of the fog.
 *
 * <p>The same pass also carries the jewel motes at their palette chroma (item 8:
 * frame-wide peak saturation measured 0.23 against a 0.59 spec) and, in its
 * overlay mode, grass strokes drawn <em>over</em> the figure's hem -- reference 2
 * draws grass across the figure's legs and that single trick does more for
 * grounding than any amount of haze. The overlay carries no fog of its own; fog
 * occlusion of the figure stays where the contract puts it, inside the ink
 * shader's evaluation of {@code u_fogBands}.
 *
 * <p>The mist GLSL is inlined here rather than added to
 * {@code src/main/resources/shaders/}: that directory is being revised
 * concurrently and this pass must not add a file to it.
 */
public final class PaperBackground {

    /**
     * Warm luminous mist -- deliberately a little lighter and distinctly warmer
     * than {@code Palette.PAPER_WARM} (237,228,211; R-B = 26). This is the one
     * place STYLE.md 2.2's "brightest non-emissive is the paper ground" is
     * departed from, on rig-fixes-3 item 5's explicit instruction and on
     * STYLE.md's own rule that the reference images win. It stays far short of
     * white.
     */
    private static final float MIST_R = 250f / 255f;
    private static final float MIST_G = 241f / 255f;
    private static final float MIST_B = 208f / 255f;

    /**
     * The same mist at dusk. STYLE.md 6 makes the fog bands non-negotiable and they
     * are still the lightest thing on the sheet, but a cream mist (250,241,208) laid
     * over a sky whose own luminance is 95-100 is a white-out rather than a haze.
     * Read off reference image 3's pale smoke around the duellists' legs, which sits
     * about 40% above its own local sky rather than 150% above it.
     */
    private static final float DUSK_MIST_R = 146f / 255f;
    private static final float DUSK_MIST_G = 135f / 255f;
    private static final float DUSK_MIST_B = 153f / 255f;

    private static final float[] NO_BANDS = new float[9];

    /**
     * Family B's stage, off by default.
     *
     * <h2>Why this is not "cosmetic", which two passes recorded it as</h2>
     *
     * <p>{@code Palette.SKY_ZENITH / SKY_MID / SKY_HORIZON / SKY_HORIZON_HOT} have
     * existed and been calibrated from the corpus since before pass 1, and until this
     * pass the only scene that referenced any of them was {@code SmokeScene}. Every
     * graded duel this project has shot was fought on Family A cream paper, which
     * inverts STYLE.md 2.2's warm/cool opposition across the whole frame -- warm
     * ground, warm blades, warm bloom -- while STYLE.md 1 makes Family B "the primary
     * template for the game screen". It is a background rather than a rig, and it is
     * the largest change in what the frame looks like per unit of work available.
     *
     * @see #dusk(boolean)
     */
    private boolean dusk;

    private final ShaderProgram shader;
    private final ShaderProgram mistShader;
    private final Mesh quad;
    private final float[] vertices = new float[8];
    private final Matrix4 inverse = new Matrix4();
    private final Vector3 corner = new Vector3();

    private float minX, minY, maxX, maxY;

    public PaperBackground() {
        this.shader = InkSkinnedRenderer.Shaders.load("paper");
        this.mistShader = new ShaderProgram(MIST_VERT, MIST_FRAG);
        if (!mistShader.isCompiled()) {
            throw new IllegalStateException("mist shader failed to compile:\n" + mistShader.getLog());
        }
        this.quad = new Mesh(false, 4, 6,
                new VertexAttributes(new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")));
        this.quad.setIndices(new short[] {0, 1, 2, 2, 3, 0});
    }

    /**
     * Draws the Family B dusk stage instead of the Family A sheet.
     *
     * <p>Everything else -- the ground smear, the contact pool, the grass, the fog
     * bands, the motes, the vignette and the paper tooth -- stays exactly where it is
     * and is only re-aimed at the tone that plays its role against a sky. The band
     * geometry in {@link Atmosphere} is untouched, so the mist the figure fades into
     * inside {@code ink_resolve.frag} is still the mist the ground shows.
     */
    public PaperBackground dusk(boolean on) {
        this.dusk = on;
        return this;
    }

    public boolean isDusk() {
        return dusk;
    }

    /**
     * The three stops {@code ink_resolve.frag} reads for the tone a wet bleed wicks
     * into, low to high in world y.
     *
     * <p>The halo of STYLE.md 3.2 is pigment leaving ink and entering the ground, so
     * its colour is the ground's. Against cream that is one constant and always has
     * been; against a graded sky a constant is a visible aura -- a cream halo on an
     * indigo zenith, or an indigo halo on a coral horizon. These are the same three
     * anchors {@code duskSky} is built from, sampled at world y 0.30, 0.95 and 2.30.
     */
    public static Color[] backdropStops(boolean dusk) {
        if (!dusk) {
            return new Color[] {Palette.PAPER_WARM, Palette.PAPER_WARM, Palette.PAPER_WARM};
        }
        return new Color[] {
                new Color(Palette.SKY_ZENITH).lerp(Palette.INK_BLACK, 0.42f),
                new Color(Palette.SKY_HORIZON).lerp(Palette.SKY_ZENITH, 0.45f),
                new Color(Palette.SKY_ZENITH),
        };
    }

    /** World y of {@link #backdropStops}. */
    public static final float[] BACKDROP_STOP_Y = {0.30f, 0.95f, 2.30f};

    public void render(Matrix4 projTrans, float timeSeconds) {
        buildQuad(projTrans);

        shader.bind();
        shader.setUniformMatrix("u_projTrans", projTrans);
        shader.setUniformf("u_time", timeSeconds);
        shader.setUniformf("u_frameMin", minX, minY);
        shader.setUniformf("u_frameSize", maxX - minX, maxY - minY);
        shader.setUniformf("u_paperWarm", Palette.PAPER_WARM.r, Palette.PAPER_WARM.g, Palette.PAPER_WARM.b);
        shader.setUniformf("u_paperCool", Palette.PAPER_COOL.r, Palette.PAPER_COOL.g, Palette.PAPER_COOL.b);
        shader.setUniformf("u_ochre", Palette.OCHRE.r, Palette.OCHRE.g, Palette.OCHRE.b);
        shader.setUniformf("u_inkIndigo", Palette.INK_INDIGO.r, Palette.INK_INDIGO.g, Palette.INK_INDIGO.b);
        shader.setUniformf("u_fogColor", MIST_R, MIST_G, MIST_B);
        shader.setUniformf("u_moteCyan", Palette.MOTE_CYAN.r, Palette.MOTE_CYAN.g, Palette.MOTE_CYAN.b);
        shader.setUniformf("u_moteMagenta", Palette.MOTE_MAGENTA.r, Palette.MOTE_MAGENTA.g, Palette.MOTE_MAGENTA.b);
        shader.setUniformf("u_ember", Palette.EMBER.r, Palette.EMBER.g, Palette.EMBER.b);
        shader.setUniformf("u_dusk", dusk ? 1f : 0f);
        shader.setUniformf("u_skyZenith", Palette.SKY_ZENITH.r, Palette.SKY_ZENITH.g, Palette.SKY_ZENITH.b);
        shader.setUniformf("u_skyMid", Palette.SKY_MID.r, Palette.SKY_MID.g, Palette.SKY_MID.b);
        shader.setUniformf("u_skyHorizon", Palette.SKY_HORIZON.r, Palette.SKY_HORIZON.g, Palette.SKY_HORIZON.b);
        shader.setUniformf("u_skyHot", Palette.SKY_HORIZON_HOT.r, Palette.SKY_HORIZON_HOT.g,
                Palette.SKY_HORIZON_HOT.b);
        shader.setUniformf("u_inkBlack", Palette.INK_BLACK.r, Palette.INK_BLACK.g, Palette.INK_BLACK.b);
        // The ground pass draws no mist of its own. Its band term is a
        // subtractive grey mix, and this revision needs the mist emissive; the
        // shared band geometry is still what the pass below uses, and still what
        // ink_resolve.frag reads for the figure's own fade, so the two agree.
        shader.setUniform3fv("u_fogBands[0]", NO_BANDS, 0, NO_BANDS.length);

        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        quad.render(shader, GL20.GL_TRIANGLES);

        drawMist(projTrans, timeSeconds, 0f);
    }

    /**
     * Grass strokes over whatever has already been drawn. Called by a scene
     * after the figure so the strokes cross the hem, per rig-fixes-3 item 5.
     *
     * <p>This is <em>not</em> the foreground fog pass contract section F rules
     * out: it paints no haze, and the figure's own dissolve into the mist stays
     * inside the ink shader where the contract puts it.
     */
    public void renderOverlay(Matrix4 projTrans, float timeSeconds) {
        buildQuad(projTrans);
        drawMist(projTrans, timeSeconds, 1f);
    }

    public void dispose() {
        shader.dispose();
        mistShader.dispose();
        quad.dispose();
    }

    private void drawMist(Matrix4 projTrans, float timeSeconds, float mode) {
        mistShader.bind();
        mistShader.setUniformMatrix("u_projTrans", projTrans);
        mistShader.setUniformf("u_time", timeSeconds);
        mistShader.setUniformf("u_frameMin", minX, minY);
        mistShader.setUniformf("u_frameSize", maxX - minX, maxY - minY);
        boolean d = dusk;
        mistShader.setUniformf("u_mist", d ? DUSK_MIST_R : MIST_R, d ? DUSK_MIST_G : MIST_G,
                d ? DUSK_MIST_B : MIST_B);
        mistShader.setUniformf("u_dusk", d ? 1f : 0f);
        mistShader.setUniformf("u_inkBlack", Palette.INK_BLACK.r, Palette.INK_BLACK.g,
                Palette.INK_BLACK.b);
        mistShader.setUniformf("u_inkIndigo", Palette.INK_INDIGO.r, Palette.INK_INDIGO.g, Palette.INK_INDIGO.b);
        mistShader.setUniformf("u_moteCyan", Palette.MOTE_CYAN.r, Palette.MOTE_CYAN.g, Palette.MOTE_CYAN.b);
        mistShader.setUniformf("u_moteMagenta", Palette.MOTE_MAGENTA.r, Palette.MOTE_MAGENTA.g, Palette.MOTE_MAGENTA.b);
        mistShader.setUniformf("u_ember", Palette.EMBER.r, Palette.EMBER.g, Palette.EMBER.b);
        mistShader.setUniformf("u_mode", mode);
        Atmosphere.setFogUniforms(mistShader);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        quad.render(mistShader, GL20.GL_TRIANGLES);
    }

    private void buildQuad(Matrix4 projTrans) {
        inverse.set(projTrans).inv();
        minX = unprojectX(-1f, -1f);
        minY = unprojectY(-1f, -1f);
        maxX = unprojectX(1f, 1f);
        maxY = unprojectY(1f, 1f);

        vertices[0] = minX; vertices[1] = minY;
        vertices[2] = maxX; vertices[3] = minY;
        vertices[4] = maxX; vertices[5] = maxY;
        vertices[6] = minX; vertices[7] = maxY;
        quad.setVertices(vertices);
    }

    private float unprojectX(float clipX, float clipY) {
        corner.set(clipX, clipY, 0f).prj(inverse);
        return corner.x;
    }

    private float unprojectY(float clipX, float clipY) {
        corner.set(clipX, clipY, 0f).prj(inverse);
        return corner.y;
    }

    // -- the mist pass -------------------------------------------------------

    private static final String MIST_VERT =
            "attribute vec2 a_position;\n"
            + "uniform mat4 u_projTrans;\n"
            + "varying vec2 v_worldPos;\n"
            + "void main() {\n"
            + "    v_worldPos = a_position;\n"
            + "    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);\n"
            + "}\n";

    private static final String MIST_FRAG =
            "#ifdef GL_ES\n"
            + "precision highp float;\n"
            + "#endif\n"
            + "varying vec2 v_worldPos;\n"
            + "uniform float u_time;\n"
            + "uniform vec2  u_frameMin;\n"
            + "uniform vec2  u_frameSize;\n"
            + "uniform vec3  u_mist;\n"
            + "uniform vec3  u_inkIndigo;\n"
            + "uniform vec3  u_moteCyan;\n"
            + "uniform vec3  u_moteMagenta;\n"
            + "uniform vec3  u_ember;\n"
            + "uniform vec3  u_fogBands[3];\n"
            + "uniform float u_mode;\n"
            + "uniform float u_dusk;\n"
            + "uniform vec3  u_inkBlack;\n"
            + "\n"
            + "float hash2(vec2 p) {\n"
            + "    vec3 q = fract(vec3(p.xyx) * 0.1031);\n"
            + "    q += dot(q, q.yzx + 33.33);\n"
            + "    return fract((q.x + q.y) * q.z);\n"
            + "}\n"
            + "float vnoise(vec2 p) {\n"
            + "    vec2 i = floor(p);\n"
            + "    vec2 f = fract(p);\n"
            + "    f = f * f * (3.0 - 2.0 * f);\n"
            + "    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x),\n"
            + "               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);\n"
            + "}\n"
            + "float fbm(vec2 p) {\n"
            + "    return vnoise(p) * 0.58 + vnoise(p * 2.3 + 7.3) * 0.28 + vnoise(p * 4.7 - 3.1) * 0.14;\n"
            + "}\n"
            + "\n"
            + "void main() {\n"
            + "    vec2 w = v_worldPos;\n"
            + "    vec2 uv = (w - u_frameMin) / u_frameSize;\n"
            + "    float aspect = u_frameSize.x / u_frameSize.y;\n"
            + "    vec3 col = u_mist;\n"
            + "    float a = 0.0;\n"
            + "\n"
            + "    if (u_mode < 0.5) {\n"
            // Bands, not a ramp. Same geometry and drift as ink_resolve.frag so
            // the figure fades into exactly this, then each band is multiplied
            // by its own wide low-frequency lobe field so the three separate as
            // depth layers instead of summing into one veil (STYLE.md 6).
            + "        for (int i = 0; i < 3; i++) {\n"
            + "            vec3 b = u_fogBands[i];\n"
            + "            float fi = float(i);\n"
            + "            float d = u_time * (0.055 + 0.031 * fi);\n"
            + "            float wob = b.y * (0.30 * sin(w.x * (0.85 + 0.45 * fi) + d + fi * 2.1)\n"
            + "                             + 0.19 * sin(w.x * 2.43 - d * 0.7 + fi)\n"
            + "                             + 0.11 * sin(w.x * 5.11 + d * 1.6 - fi * 1.7));\n"
            + "            float dist = abs(w.y - (b.x + wob)) / b.y;\n"
            + "            float f = b.z * (1.0 - smoothstep(0.0, 1.0, dist));\n"
            + "            float lb = fbm(vec2(w.x * 0.50 + fi * 17.0 - d * 0.9,\n"
            + "                                w.y * 1.35 + fi * 5.0));\n"
            // On cream the lobe never falls below 0.52, so the three bands are a
            // continuous veil with structure in it. At dusk that veil is what
            // stands between the ground and the corpus: measured, it lifts the
            // row background below world y 0.3 from 30 to 60 where reference
            // image 3 reads 27-39. In image 3 the mist low down is *wisps* with
            // dark sky between them, not a bank -- so the lobe is allowed to
            // reach zero and the same fbm decides where.
            + "            float lobe = mix(0.52 + 0.48 * lb, smoothstep(0.34, 0.76, lb), u_dusk);\n"
            + "            a += f * lobe;\n"
            + "        }\n"
            + "        a = clamp(a, 0.0, 1.0);\n"
            // At dusk the bands sit on a ground that is a *dark ink smear*
            // (STYLE.md 1, Family B) rather than on cream, and a mist authored to
            // be the brightest thing on a cream sheet turns that smear into snow.
            // Reference image 3 keeps its lower third at luminance 29-55 with pale
            // smoke *wisps* through it, not a bank; this is the wisp strength.
            + "        a *= mix(1.0, 0.42, u_dusk);\n"
            // A faint high, wide veil across the top so the sky end of the sheet
            // is not perfectly clean either -- references 6-8 have light
            // everywhere, just less of it up high.
            + "        a = max(a, smoothstep(0.30, 0.86, uv.y) * 0.10\n"
            + "                   * (0.5 + 0.5 * fbm(vec2(w.x * 0.4 - u_time * 0.02, w.y * 0.9))));\n"
            + "\n"
            // Jewel motes at palette chroma (item 8). A wide, edgeless bokeh
            // halo carries the shape; a small near-opaque core inside it is what
            // actually puts a saturated pixel on the sheet, because a 0.22 tint
            // of a 0.59-saturation colour over cream measures 0.1 and reads as
            // nothing. Two dozen would be a particle system; this is twelve.
            + "        for (int i = 0; i < 14; i++) {\n"
            + "            float fi = float(i);\n"
            + "            vec2 seed = vec2(fi * 7.13 + 1.0, fi * 3.71 + 2.0);\n"
            + "            vec2 c = vec2(hash2(seed), hash2(seed + 11.0));\n"
            + "            c.x += u_time * (0.003 + 0.005 * hash2(seed + 3.0));\n"
            + "            c.y += u_time * 0.0024;\n"
            + "            c = fract(c);\n"
            + "            float r = 0.016 + 0.026 * hash2(seed + 5.0);\n"
            + "            float dm = length((uv - c) * vec2(aspect, 1.0));\n"
            + "            float halo = pow(1.0 - smoothstep(0.0, r, dm), 2.0) * 0.12;\n"
            + "            float core = pow(1.0 - smoothstep(0.0, r * 0.20, dm), 1.6) * 0.72;\n"
            + "            float ma = clamp(halo + core, 0.0, 1.0);\n"
            + "            float k = hash2(seed + 17.0);\n"
            + "            vec3 mc = k < 0.40 ? u_moteCyan : (k < 0.76 ? u_moteMagenta : u_ember);\n"
            + "            float outA = ma + a * (1.0 - ma);\n"
            + "            col = (mc * ma + col * a * (1.0 - ma)) / max(outA, 1e-4);\n"
            + "            a = outA;\n"
            + "        }\n"
            + "    } else {\n"
            // Grass strokes, drawn over the figure. Each stroke is a function of
            // a sheared x, so it is a single continuous curved blade rather than
            // a stack of dashes, and they arrive in clumps biased toward the
            // figure's own footing -- an even comb across the frame reads as a
            // ruler, not as a field. Value sits between the mist and the ink so
            // the same strokes read against the pale haze and against the dark
            // hem they cross.
            + "        float base = 0.02 + 0.035 * vnoise(vec2(w.x * 2.6, 1.0));\n"
            // Debt D5. These reached to world y = 0.48, which is above the near
            // knee (0.53 is the joint, the shin runs 0.53 down to 0.13), so the
            // strokes were crossing the entire lower leg rather than the hem.
            // Shortened to reach y = 0.31, which crosses the ankle and the foot
            // and leaves the shin alone.
            + "        float len = 0.11 + 0.20 * vnoise(vec2(w.x * 0.75, 7.0));\n"
            + "        float lean = (vnoise(vec2(w.x * 5.5, 3.0)) - 0.5) * 0.55;\n"
            + "        float xs = w.x + (w.y - base) * lean;\n"
            + "        float blade = smoothstep(0.72, 0.94, vnoise(vec2(xs * 52.0, 2.0)))\n"
            + "                    + smoothstep(0.80, 0.97, vnoise(vec2(xs * 23.0, 9.0)));\n"
            + "        float up = smoothstep(base + len, base + len * 0.18, w.y);\n"
            + "        float down = smoothstep(base - 0.09, base + 0.01, w.y);\n"
            + "        float clump = (0.28 + 0.72 * smoothstep(0.20, 0.60, vnoise(vec2(w.x * 1.15, 11.0))))\n"
            + "                    * (0.60 + 0.40 * smoothstep(2.4, 0.0, abs(w.x)));\n"
            + "        float taper = mix(1.0, 0.35, smoothstep(base + len * 0.2, base + len, w.y));\n"
            + "        a = clamp(blade, 0.0, 1.0) * up * down * clump * taper * 0.62;\n"
            // Debt D5, and this was the single largest bleaching term in the
            // lower third -- larger than the fog and larger than the mesh. At
            // mix 0.42 these strokes are luminance 163 and they were composited
            // at up to 0.88 alpha *over the figure*, so every stroke that
            // crossed the leg lifted near-black ink (26) to about 150. The
            // measured "146 descending to the hem" of the debt record is
            // substantially this, painted on top of a figure that was already
            // dark underneath.
            //
            // Reference image 2 does draw grass across the figure's legs, which
            // is where this came from and it is worth keeping -- but there the
            // strokes are *darker* than the pale ground and lighter than the
            // figure, so they read against both without erasing either. At 0.70
            // they are luminance 112: still clearly visible on the mist, no
            // longer able to bleach a leg.
            + "        col = mix(mix(u_mist, u_inkIndigo, 0.70),\n"
            + "                  mix(u_mist, u_inkBlack, 0.78), u_dusk);\n"
            + "    }\n"
            + "\n"
            + "    gl_FragColor = vec4(clamp(col, vec3(0.06), vec3(0.965)), clamp(a, 0.0, 1.0));\n"
            + "}\n";
}
