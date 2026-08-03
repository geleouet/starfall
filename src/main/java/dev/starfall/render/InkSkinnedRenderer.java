package dev.starfall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import dev.starfall.anim.Skeleton;
import dev.starfall.anim.SkinnedMesh;
import dev.starfall.art.Palette;

import java.nio.IntBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws skinned meshes with the ink material -- contract section F.
 *
 * <p>The cloth path is in two passes, but revision 3 step 1
 * (docs/system1-shader-fixes-3.md) moves the line between them:
 *
 * <ol>
 *   <li>all cloth geometry renders into one offscreen buffer. ink_skin.frag
 *       cuts the fray per-fragment at full resolution in material space and
 *       writes <em>coverage</em> plus (value, stain, bleed) premultiplied by it.
 *       Contributions composite with ordinary premultiplied "over", so
 *       overlapping ribbons average rather than the topmost replacing its
 *       neighbours -- the rule that printed three axis-aligned bars through
 *       pass 3's torso;</li>
 *   <li>two quarter-resolution blurs of that coverage, used <em>only</em> for
 *       the halo. Revision 2 built a distance-to-silhouette out of them and
 *       thresholded the fray against it, which is why the outline came out
 *       shaped like a quarter-resolution gaussian and why a band sized for the
 *       haori hem deleted the neck, the skull and the sword arm;</li>
 *   <li>one full-screen resolve turns the merged material into colour and
 *       composites onto the paper.</li>
 * </ol>
 *
 * <p>Everything that follows from that is in the shaders. What is here is the
 * plumbing, plus the blade, which is drawn forward and is the one object in the
 * frame that must keep a hard edge.
 *
 * <p>Budget: one extra target at capture resolution and two at a quarter of it,
 * which is 1.125 full-resolution targets in total -- inside the mobile-safe
 * decision, which allows one extra full-resolution target and requires any blur
 * to run at reduced resolution.
 */
public final class InkSkinnedRenderer {

    /**
     * Contract section B, amended by System 3b. Matches MAX_BONES in
     * ink_skin.vert: the skeleton was already at 31 bones when 3b arrived (the
     * contract-era count of 28 was stale), the three face bones took it to 34,
     * and 36 mat4 is 144 vec4 against the GLES 3.0 guaranteed 256 the original
     * 32 was derived from. Two slots remain for 3c.
     */
    private static final int MAX_BONES = 36;

    /** GLES 3.0 blend equations. Not in libGDX's GL20 interface, which predates them. */
    private static final int GL_FUNC_ADD = 0x8006;

    /** Tap spacing (and sigma) of the two coverage blurs, in quarter-resolution texels. */
    private static final float BLUR_MID = 2.0f;
    private static final float BLUR_FAR = 4.5f;

    /**
     * STYLE.md 5: a swung blade's arc-trail fades over roughly this long.
     *
     * <p>0.48 rather than 0.40 for one reason: the capture step is 0.20 s, so a
     * 0.40 s life stores two poses and the trail is a single interpolated
     * segment. Three poses give two segments and the arc the interpolation
     * already computes actually becomes visible in a still frame
     * (shader-fixes-3 item 6). The fade is squared, so the extra 0.08 s
     * contributes almost no opacity.
     */
    private static final float TRAIL_SECONDS = 0.48f;
    private static final int TRAIL_SAMPLES = 24;

    /**
     * The other end the trail is trimmed from: how much <em>turn</em> it may hold,
     * in radians, however little time that took.
     *
     * <h2>Why time alone is the wrong trim, measured</h2>
     *
     * <p>{@link #TRAIL_SECONDS} bounds the trail's age. At a tip speed around 18
     * units/s that is an enormous arc, and the pass-4 review measured what it prints
     * on the phrase capture: a single connected ridge in the sky window
     * {@code x200..800 y130..320} of frame 6, box {@code x228..591 y143..319} --
     * <b>364 px wide = 1.11 figure heights</b>, best-fit circle of radius 162 px =
     * 0.49 figure heights, and an <b>angular extent of 177 degrees</b>. A half-circle
     * of pale light standing in empty sky is a moon, and the review said so twice:
     * once in its measurements and once, before it measured, by describing it as one.
     *
     * <p>STYLE.md 5 asks for "a smooth ribbon following the blade's <em>swept path</em>,
     * brightest at the leading edge". A wake is a fragment of the path, not the whole
     * of it. So the history is trimmed on <b>accumulated turn</b> as well as on age:
     * the stored run may hold {@code TRAIL_SWEEP} radians of angular path, summed over
     * its own steps so a back-and-forth spends the budget rather than cancelling it.
     *
     * <p><b>0.60 rad = 34 degrees, and the pass-5 brief asked for 60 to 80. The reason
     * for the difference is granularity, and it is worth recording because it is a
     * property of the mechanism rather than a taste.</b> On a fast phrase beat the
     * stored poses are already <em>0.5 to 0.7 rad apart</em> -- one sample of history is
     * a third of the budget the brief names -- so this constant does not choose an angle,
     * it chooses a <em>number of stored poses</em>. Measured on {@code duel-phrase} frame
     * 108, largest local-background sky ridge with the blade's own cool-bright mask
     * excluded, window {@code x100..860 y60..330}, width as a fraction of the figure
     * span:
     *
     * <ul>
     *   <li>pass 4 (no cap): <b>432 px = 1.31 figure heights</b>, and the ridge is the
     *       arc;</li>
     *   <li>1.22 rad, which keeps three poses: 423 px = <b>1.12</b>, still an arc, still
     *       reads as a crescent at 4x;</li>
     *   <li>0.60 rad, which keeps two: 221 px = <b>0.58</b>, and the largest ridge in
     *       that window is no longer the trail at all -- it is the hero's own ink at
     *       {@code x208..428 y239..329}. Every value from 0.60 down to 0.05 draws the
     *       identical frame, because two poses is the floor this method will trim to.</li>
     * </ul>
     *
     * <p>So 60-80 degrees is not available: the choice is 34 or about 80, and at 80 the
     * mark still reads as a moon. Taking the lower one is the one that answers the
     * defect. A slow blade is unaffected either way -- 34 degrees of turn takes longer
     * than 0.48 s to accumulate at a walking pace, so {@code TRAIL_SECONDS} still binds
     * and the ribbon behaves exactly as it did.
     */
    private static final float TRAIL_SWEEP = 0.60f;

    private static final int FLOATS = SkinnedMesh.FLOATS_PER_VERTEX;

    static {
        // Drivers are free to eliminate a uniform whose value cannot reach the
        // output, and libGDX's pedantic mode turns that into an exception at
        // setUniform time. Tuning a shader means temporarily commenting terms
        // out; crashing the capture run when that happens would make iteration
        // considerably worse.
        ShaderProgram.pedantic = false;
    }

    private final ShaderProgram matShader;
    private final ShaderProgram downShader;
    private final ShaderProgram blurShader;
    private final ShaderProgram resolveShader;
    private final ShaderProgram bladeShader;
    private final ShaderProgram glowShader;

    private final float[] bones = new float[MAX_BONES * 16];
    private final Mesh fullscreenQuad;
    private final Mesh glowMesh;
    private static final int GLOW_MAX_VERTS = 1024;
    private final float[] glowVerts = new float[GLOW_MAX_VERTS * 6];
    private final short[] glowIndices = new short[4096];
    private int glowVertCount;
    private int glowIndexCount;

    private FrameBuffer matTarget;
    private FrameBuffer blurA;
    private FrameBuffer blurB;
    private int targetWidth;
    private int targetHeight;

    private final IntBuffer query = BufferUtils.newIntBuffer(16);
    private final int[] savedViewport = new int[4];
    private int savedFramebuffer;
    private boolean materialBound;
    private boolean anyCloth;

    private final Matrix4 projTrans = new Matrix4();
    private final Matrix4 inverse = new Matrix4();
    private final Vector3 corner = new Vector3();
    private final Vector2 frameMin = new Vector2();
    private final Vector2 frameSize = new Vector2();
    private float time;

    private final Color clothBase = new Color(Palette.INK_INDIGO);
    private final Color clothDeep = new Color(Palette.INK_BLACK);
    private final Color clothStain = new Color(Palette.OCHRE);
    private final Color clothStainPale = new Color(Palette.OCHRE_PALE);
    private float clothBleed = 1f;

    /**
     * What the wet bleed of STYLE.md 3.2 wicks into, low to high in world y.
     *
     * <p>Defaults to three copies of the Family A paper level, which is what every
     * scene before System 4 pass 4 drew and is bit-identical to the single constant it
     * replaces -- {@code mix(c, c, t)} is exactly {@code c}. A Family B scene hands it
     * {@link PaperBackground#backdropStops} so a figure's halo is the colour of the sky
     * it is standing against rather than a cream aura on an indigo zenith.
     */
    private Color[] backdrop = PaperBackground.backdropStops(false);

    /** @see #backdrop */
    public void backdrop(Color[] stops) {
        this.backdrop = stops;
    }

    /** The arc-trail's peak alpha on cream, which is what it was tuned at. */
    private static final float TRAIL_PEAK = 0.24f;

    /**
     * The same trail, graded against the ground it is screened onto rather than
     * against a constant.
     *
     * <p>The ribbon is composited with {@code ONE, ONE_MINUS_SRC_COLOR} -- screen --
     * which adds {@code src * (1 - dst)}. On Family A cream ({@code dst} luminance
     * 0.86) that is 0.14 of the source and three reviews in a row recorded the trail
     * as invisible, 2.7% above paper, as a fault. On the Family B dusk sky
     * ({@code dst} 0.30-0.40 through the whole upper frame) the identical ribbon is
     * five times further above its ground, and the first dusk capture printed it as a
     * near-closed pale crescent -- a moon, not a smear.
     *
     * <p>So the peak is scaled to hold a fixed multiple of the contrast it had on
     * cream: <b>1.6x</b>, chosen so the trail is finally legible where the debt says
     * it never was without becoming the brightest object in the frame. Cream itself
     * clamps at 1.0, so every Family A capture is bit-identical.
     */
    private float trailPeak() {
        Color mid = backdrop[1];
        float lum = 0.2126f * mid.r + 0.7152f * mid.g + 0.0722f * mid.b;
        float onCream = 1f - 0.86f;
        float here = Math.max(1e-3f, 1f - lum);
        return TRAIL_PEAK * Math.min(1f, Math.max(0.25f, onCream / here * 1.6f));
    }

    private final Map<SkinnedMesh, Blade> blades = new IdentityHashMap<>();
    private final Vector2 tmpA = new Vector2();
    private final Vector2 tmpB = new Vector2();

    public InkSkinnedRenderer() {
        this.matShader = Shaders.load("ink_skin", "ink_skin");
        this.downShader = Shaders.load("ink_fullscreen", "ink_down");
        this.blurShader = Shaders.load("ink_fullscreen", "ink_blur");
        this.resolveShader = Shaders.load("ink_fullscreen", "ink_resolve");
        this.bladeShader = Shaders.load("ink_skin", "ink_blade");
        this.glowShader = Shaders.load("ink_glow", "ink_glow");

        this.fullscreenQuad = new Mesh(true, 4, 6,
                new VertexAttributes(new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")));
        fullscreenQuad.setVertices(new float[] {-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f});
        fullscreenQuad.setIndices(new short[] {0, 1, 2, 2, 3, 0});

        this.glowMesh = new Mesh(false, GLOW_MAX_VERTS, 4096,
                new VertexAttributes(
                        new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                        new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")));
    }

    public void begin(Matrix4 projTrans, float timeSeconds) {
        this.projTrans.set(projTrans);
        this.time = timeSeconds;

        saveTarget();
        ensureTargets(savedViewport[2], savedViewport[3]);
        computeFrameRect();

        bindMaterialTarget(true);
        anyCloth = false;
    }

    public void draw(SkinnedMesh mesh, Skeleton skeleton, InkMaterial material) {
        if (skeleton.boneCount() > MAX_BONES) {
            throw new IllegalArgumentException(
                    "Skeleton has " + skeleton.boneCount() + " bones, the shader caps at " + MAX_BONES);
        }
        skeleton.fillSkinningMatrices(bones);

        if (material.emissive) {
            // The blade is not cloth and must not be merged into the coverage
            // buffer -- that buffer exists to destroy hard edges. Flushing here
            // rather than at end() keeps the painter's order the scene asked
            // for: cloth resolves onto the paper, then steel over it.
            flushCloth();
            drawBlade(mesh, material);
            return;
        }

        // The resolve is per *group*, not per draw: u_base, u_deep, u_stain and
        // u_bleedRadius are read once in flushCloth() and painted over everything
        // that has accumulated in the coverage buffer. With one figure that
        // distinction never surfaced. With two it is a silent wrong-colour bug --
        // a pale duellist drawn before a dark one comes out dark -- so a material
        // whose resolve-stage parameters differ from the group's ends the group.
        //
        // It also gives each figure its own coverage field, which matters for more
        // than colour: the wet bleed of STYLE.md 3.2 reaches tens of pixels past a
        // silhouette, and two figures merged into one premultiplied buffer average
        // their ink where they overlap instead of the near one sitting over the
        // far one. Two figures passing through each other is the Pilgrim's whole
        // movement verb, so that is not a corner case.
        if (materialBound && anyCloth && !resolvesTheSame(material)) {
            flushCloth();
        }

        clothBase.set(material.base);
        clothDeep.set(material.deep);
        clothStain.set(material.stain);
        clothStainPale.set(material.stainPale);
        clothBleed = material.bleedRadius;

        if (!materialBound) {
            // Re-entering after a flush starts a fresh merge group, otherwise
            // the already-resolved coverage would be composited a second time.
            bindMaterialTarget(true);
        }
        matShader.bind();
        matShader.setUniformMatrix("u_projTrans", projTrans);
        matShader.setUniformf("u_time", time);
        matShader.setUniformMatrix4fv("u_bones", bones, 0, MAX_BONES * 16);
        matShader.setUniformf("u_dissolveBias", material.dissolveBias);
        matShader.setUniformf("u_paperGrain", material.paperGrain);
        matShader.setUniformf("u_inkSeed", material.seedX, material.seedY);
        matShader.setUniformf("u_sash", material.sashHeight, material.sashLift);
        matShader.setUniformf("u_sashTop", material.sashTop);
        matShader.setUniformf("u_covScale", material.covScale);
        matShader.setUniformf("u_feather", material.feather);
        mesh.mesh().render(matShader, GL20.GL_TRIANGLES);
        anyCloth = true;
    }

    /**
     * Resolves everything drawn so far and starts a new merge group.
     *
     * <p>{@link #draw} calls this for you when a material's resolve-stage
     * parameters change, which covers the ordinary two-figure case. It is public
     * for the case that is not ordinary: two figures the <em>same</em> colour,
     * which must still not share a coverage field if either is meant to occlude
     * the other.
     */
    public void flush() {
        flushCloth();
    }

    public void end() {
        flushCloth();
    }

    /**
     * True when {@code material} would resolve identically to the group currently
     * open, i.e. when merging it in costs nothing. Only the four parameters
     * {@link #flushCloth} actually reads are compared -- {@code dissolveBias},
     * {@code paperGrain} and {@code seedX/Y} are per-draw uniforms on the
     * material pass and never a reason to split a group.
     */
    private boolean resolvesTheSame(InkMaterial material) {
        return clothBase.equals(material.base)
                && clothDeep.equals(material.deep)
                && clothStain.equals(material.stain)
                && clothStainPale.equals(material.stainPale)
                && clothBleed == material.bleedRadius;
    }

    /**
     * How far the figures have receded into the air, 0 at the intimate framing and
     * 1 at the widest one.
     *
     * <p>STYLE.md 9's planning framing asks for <i>"the full lane readable, figures
     * small, heavy fog, Family C mood"</i>, and the pass-1 review found the last two
     * absent: <b>the 86 px hero held 85% of the contrast the 280 px hero held</b>, so
     * going wide cost size and cost nothing else. This is the one dial that makes
     * distance cost value, and it is a continuous function of the framing width, so
     * it inherits {@code Schedule.cameraIsContinuous} and cannot introduce a step
     * into a shot the schedule guarantees is a glide.
     *
     * <p>Zero by default, which is every scene shot before System 5.
     */
    private float haze = 0f;

    /** @see #haze */
    public InkSkinnedRenderer haze(float haze) {
        this.haze = Math.max(0f, Math.min(1f, haze));
        return this;
    }

    public void dispose() {
        matShader.dispose();
        downShader.dispose();
        blurShader.dispose();
        resolveShader.dispose();
        bladeShader.dispose();
        glowShader.dispose();
        fullscreenQuad.dispose();
        glowMesh.dispose();
        disposeTargets();
    }

    // -- offscreen targets ---------------------------------------------------

    private void ensureTargets(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (matTarget != null && targetWidth == w && targetHeight == h) {
            return;
        }
        disposeTargets();
        targetWidth = w;
        targetHeight = h;
        int qw = Math.max(1, w / 4);
        int qh = Math.max(1, h / 4);
        matTarget = newTarget(w, h);
        blurA = newTarget(qw, qh);
        blurB = newTarget(qw, qh);
    }

    private static FrameBuffer newTarget(int w, int h) {
        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        Texture tex = fbo.getColorBufferTexture();
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        // Clamp, never wrap: the wet bleed reaches tens of pixels past the
        // figure, and a wrapped tap would print the far side of the frame into
        // the halo.
        tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return fbo;
    }

    private void disposeTargets() {
        if (matTarget != null) {
            matTarget.dispose();
            blurA.dispose();
            blurB.dispose();
            matTarget = null;
            blurA = null;
            blurB = null;
        }
    }

    /**
     * libGDX's {@code FrameBuffer#end()} rebinds the <em>default</em> buffer,
     * not whatever was bound before, so nesting a target inside the capture
     * harness's own target through that API would silently redirect the rest of
     * the frame to the screen. Asking GL what is currently bound is the only
     * thing that survives being called from inside someone else's pass.
     */
    private void saveTarget() {
        query.clear();
        Gdx.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, query);
        savedFramebuffer = query.get(0);
        query.clear();
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, query);
        for (int i = 0; i < 4; i++) {
            savedViewport[i] = query.get(i);
        }
    }

    private void bindTarget(FrameBuffer fbo) {
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fbo.getFramebufferHandle());
        Gdx.gl.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
    }

    private void restoreTarget() {
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, savedFramebuffer);
        Gdx.gl.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }

    private void bindMaterialTarget(boolean clear) {
        bindTarget(matTarget);
        if (clear) {
            // Zero, because the buffer is premultiplied: "no ink here" is
            // literally no contribution, and every material channel is
            // reconstructed downstream by dividing by the coverage that carried
            // it. Revision 2 cleared to a neutral (0.5, 0.5, 0) because its RGB
            // was *not* premultiplied and a thin strip had to land on something.
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        // Premultiplied "over", on every channel including alpha --
        // docs/system1-shader-fixes-3.md item 2.
        //
        // Revision 2 MAX-blended the alpha channel, which carried a
        // near-constant weight (1 - 0.28*dissolve, so 0.72..1.0 everywhere), and
        // alpha-blended RGB against it. With a source alpha that high, "blend"
        // means "replace": the topmost ribbon overwrote its neighbour's value
        // and stain inside its own quad, and the quad's rail printed as a hard
        // axis-aligned step. Those are the three bars through pass 3's torso,
        // and the review's judgement is that they are decisively worse than the
        // periodic banding they replaced.
        //
        // Now ink_skin.frag writes a real coverage alpha -- the fray is already
        // cut -- and premultiplies the material channels by it, so this is
        // ordinary alpha compositing: overlapping ribbons *average* in
        // proportion to how much ink each deposits, exactly as pass 2's
        // per-ribbon blending did. Nothing max-blends anywhere any more.
        Gdx.gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE); // the rig authors both windings
        materialBound = true;
    }

    // -- the resolve ---------------------------------------------------------

    private void flushCloth() {
        if (!materialBound) {
            return;
        }
        materialBound = false;
        boolean hadCloth = anyCloth;
        anyCloth = false;
        Gdx.gl.glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (!hadCloth) {
            restoreTarget();
            restoreBlend();
            return;
        }

        // Coverage ladder: quarter-res downsample, then two gaussians. blurB ends
        // up holding the narrow field (about 9 px) and blurA the wide one (about
        // 20 px). Since revision 3 step 1 neither shapes the silhouette -- the
        // narrow one carries the halo, the wide one is read only for a fallback
        // value outside the coverage. The wide bleed that would use blurA
        // properly is step 2.
        bindTarget(blurA);
        downShader.bind();
        bindTexture(matTarget, 0);
        downShader.setUniformi("u_src", 0);
        downShader.setUniformf("u_srcTexel", 1f / targetWidth, 1f / targetHeight);
        fullscreenQuad.render(downShader, GL20.GL_TRIANGLES);

        float qw = 1f / blurA.getWidth();
        float qh = 1f / blurA.getHeight();

        bindTarget(blurB);
        blurShader.bind();
        bindTexture(blurA, 0);
        blurShader.setUniformi("u_src", 0);
        blurShader.setUniformf("u_srcTexel", qw, qh);
        blurShader.setUniformf("u_step", BLUR_MID);
        fullscreenQuad.render(blurShader, GL20.GL_TRIANGLES);

        bindTarget(blurA);
        blurShader.bind();
        bindTexture(blurB, 0);
        blurShader.setUniformi("u_src", 0);
        blurShader.setUniformf("u_srcTexel", qw, qh);
        blurShader.setUniformf("u_step", BLUR_FAR);
        fullscreenQuad.render(blurShader, GL20.GL_TRIANGLES);

        restoreTarget();
        restoreBlend();

        resolveShader.bind();
        bindTexture(matTarget, 0);
        bindTexture(blurB, 1);
        bindTexture(blurA, 2);
        resolveShader.setUniformi("u_mat", 0);
        resolveShader.setUniformi("u_covMid", 1);
        resolveShader.setUniformi("u_covFar", 2);
        resolveShader.setUniformf("u_texel", 1f / targetWidth, 1f / targetHeight);
        resolveShader.setUniformf("u_frameMin", frameMin.x, frameMin.y);
        resolveShader.setUniformf("u_frameSize", frameSize.x, frameSize.y);
        resolveShader.setUniformf("u_time", time);
        setColor(resolveShader, "u_base", clothBase);
        setColor(resolveShader, "u_deep", clothDeep);
        setColor(resolveShader, "u_stain", clothStain);
        setColor(resolveShader, "u_stainPale", clothStainPale);
        setColor(resolveShader, "u_paperLo", backdrop[0]);
        setColor(resolveShader, "u_paperMid", backdrop[1]);
        setColor(resolveShader, "u_paperHi", backdrop[2]);
        resolveShader.setUniformf("u_paperStops",
                PaperBackground.BACKDROP_STOP_Y[0],
                PaperBackground.BACKDROP_STOP_Y[1],
                PaperBackground.BACKDROP_STOP_Y[2]);
        setColor(resolveShader, "u_fogColor", Palette.FOG);
        resolveShader.setUniformf("u_haze", haze);
        resolveShader.setUniformf("u_bleedRadius", clothBleed);
        Atmosphere.setFogUniforms(resolveShader);
        fullscreenQuad.render(resolveShader, GL20.GL_TRIANGLES);

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }

    private void restoreBlend() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
    }

    private static void bindTexture(FrameBuffer fbo, int unit) {
        fbo.getColorBufferTexture().bind(unit);
    }

    // -- the blade -----------------------------------------------------------

    private void drawBlade(SkinnedMesh mesh, InkMaterial material) {
        Blade blade = blades.get(mesh);
        if (blade == null) {
            blade = Blade.of(mesh);
            blades.put(mesh, blade);
        }

        if (blade.valid()) {
            blade.sample(bones, time, tmpA, tmpB);
            drawTrail(blade);
            drawSheath(blade);
        }

        bladeShader.bind();
        bladeShader.setUniformMatrix("u_projTrans", projTrans);
        bladeShader.setUniformf("u_time", time);
        bladeShader.setUniformMatrix4fv("u_bones", bones, 0, MAX_BONES * 16);
        setColor(bladeShader, "u_base", material.base);
        setColor(bladeShader, "u_fogColor", Palette.FOG);
        bladeShader.setUniformf("u_haze", haze);
        Atmosphere.setFogUniforms(bladeShader);
        mesh.mesh().render(bladeShader, GL20.GL_TRIANGLES);
    }

    /**
     * STYLE.md 5's outer glow. The authored blade is a sliver -- six pixels at
     * the guard converging to a true point -- so there is physically no room
     * inside the polygon for a halo, and revision 1 accordingly had none: a
     * measured cross-section was five flat pixels with a hard step on one side.
     * The sheath is a separate ribbon laid along the blade's own axis, widest at
     * the guard, brightest toward the tip, and running a little past the point
     * so the tip glows rather than stopping.
     */
    private void drawSheath(Blade blade) {
        float bx = blade.bx;
        float by = blade.by;
        float dx = blade.tx - bx;
        float dy = blade.ty - by;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) {
            return;
        }
        dx /= len;
        dy /= len;

        final float[] along = {0f, 0.30f, 0.60f, 0.84f, 1.0f, 1.10f};
        // STYLE.md 5: the glow is stronger toward the tip, not uniform along the
        // blade. Debt D2 recorded it as simply absent, which was half the
        // authored amplitude being too low and half the profile being symmetric
        // -- a symmetric halo around a two-pixel sliver reads as a slightly
        // fatter sliver, not as light.
        final float[] bright = {0.42f, 0.62f, 0.82f, 1.0f, 1.02f, 0.26f};
        // ...and "stronger along the edge", which is the other half of 5. The
        // rig authors +perp as the mune (spine) side, so the edge is negative:
        // the profile below reaches 1.35 half-widths past the ha and only 0.85
        // past the mune. Both outermost rails sit on zero alpha, so nothing here
        // can print a straight boundary of its own.
        final float[] rail = {-1.35f, -0.55f, 0f, 0.40f, 0.85f};
        final float[] railA = {0f, 0.88f, 1f, 0.72f, 0f};
        final float halfWidth = 0.052f;
        final float peak = 0.40f;

        beginRibbon();
        for (int i = 0; i < along.length; i++) {
            float s = along[i];
            float w = halfWidth * (1f - 0.42f * MathUtils.clamp(s, 0f, 1.15f));
            float px = bx + dx * len * s;
            float py = by + dy * len * s;
            // Debt D2: the same mist the blade itself now fades into. Evaluated
            // per row rather than once, because at the framing of the bind pose
            // the guard is above the fog and the kissaki is inside it.
            float clear = 1f - 0.34f * Atmosphere.fogAt(px, py, time);
            for (int j = 0; j < rail.length; j++) {
                float off = rail[j] * w;
                pushGlowVertex(px - dy * off, py + dx * off, peak * bright[i] * railA[j] * clear);
            }
        }
        ribbonIndices(along.length, rail.length);
        renderRibbon();
    }

    /**
     * The swept arc-trail of STYLE.md 5 and 7.2. Between two rendered frames the
     * blade can cross most of a quadrant -- the review measured 85 degrees in one
     * 0.218 s capture step -- and revision 1 left nothing between them, so the
     * cut strobed as a set of discrete poses instead of smearing.
     *
     * <p>Sub-steps interpolate the blade's <em>angle</em> and length about its
     * own base rather than lerping the tip, so the swept edge is a genuine arc.
     * STYLE.md 5 is explicit that a straight trail reads as generic slash VFX and
     * fails; a chord between two poses is exactly that straight trail.
     */
    private void drawTrail(Blade blade) {
        int n = blade.count;
        if (n < 2) {
            return;
        }
        blade.unwrapAngles();
        beginRibbon();

        // A ribbon following the tip, not the whole swept sector. STYLE.md 5
        // asks for "a smooth ribbon following the blade's swept path"; the full
        // triangle from the hilt outward is an enormous straight-edged wedge laid
        // over the figure, and reads as a translucent polygon rather than light.
        // Offsets are absolute world units back from the tip, so the ribbon keeps
        // its width instead of scaling with the blade.
        //
        // shader-fixes-3 item 6: about twice as wide as revision 2's 0.15 units
        // (30 px), which was simply too thin to expose the arc the angle
        // interpolation already computes. Both rails end on zero alpha, so no
        // straight edge survives -- the inner rail feathers away toward the hilt
        // over 0.16 units and the outer one dies 0.04 past the tip. It is not
        // wider than this because the ribbon is screened onto warm paper: a cool
        // pale wash reads as light over ink but as a grey veil over open ground,
        // so its area has to stay near the blade's path.
        //
        // <b>System 4 pass 4: the taper, and why the dusk sky forced it.</b> Both
        // numbers above were tuned against Family A cream, and the tuning is
        // recorded in the paragraph above in exactly those terms -- "screened onto
        // warm paper". Screen adds {@code src * (1 - dst)}, so the same ribbon that
        // sat 2.7% above a 0.86 paper (which three reviews recorded as *invisible*,
        // as a fault) sits five times further above a 0.30 dusk sky. The first
        // Family B capture shot in this project printed it as a near-closed pale
        // dome a figure height across, and it read as a moon rather than as a smear.
        //
        // The fix is the taper the debt has recorded as missing for three passes,
        // not a dimmer: {@code rail} is scaled by the row's own age so the ribbon
        // narrows to a point behind the blade, which is what a brush lifting off
        // the paper does and what stops a swept arc closing into a shape. STYLE.md
        // 5's "brightest at the leading edge" is then carried by the width as well
        // as by the alpha.
        final float[] rail = {-0.28f, -0.20f, -0.12f, -0.05f, 0f, 0.04f};
        final float[] railA = {0f, 0.30f, 0.62f, 0.92f, 1f, 0f};
        final float peak = trailPeak();
        int rows = 0;

        // Catmull-Rom through the stored poses rather than a chord per segment.
        //
        // Revision 2 interpolated angle and base linearly *within* each segment,
        // which is C0: the spine's tangent changed direction at every stored pose,
        // and where the blade turns hard between two frames that prints as a
        // chevron kink -- visible at frame 05 of s2-check-extreme24, with frame 06
        // clean because its samples happened to be collinear. STYLE.md 5 is
        // explicit that the trail "must curve", so C0 is not enough: the ribbon's
        // own spine has to be C1, which is what a centripetal Catmull-Rom through
        // (base, angle, length) gives. Angles are unwrapped first so a pose
        // sequence crossing +-180 does not spline the long way round.
        //
        // Substepping is then driven by the *curvature* the spline actually has
        // rather than by the chord's turn, which is the review's "insert
        // intermediate samples when angular rate is high and swept path is short":
        // a hard turn covering little ground is exactly the case a fixed step
        // count under-samples, and it is the case that kinks.
        for (int i = 0; i < n - 1; i++) {
            float turnDeg = Math.abs(blade.angle[i + 1] - blade.angle[i]) * MathUtils.radiansToDegrees;
            float sweptPx = Vector2.len(blade.tipX[i + 1] - blade.tipX[i], blade.tipY[i + 1] - blade.tipY[i]);
            // Two independent demands, whichever is larger. The angular term is
            // the old one; the second raises the sample count when the tip barely
            // moved, because a short arc with a big turn has the tightest radius
            // in the ribbon and the least geometry to hide a facet in.
            int byAngle = Math.round(turnDeg / 3f);
            int byTightness = sweptPx > 1e-4f ? Math.round(turnDeg / (60f * (sweptPx + 0.02f))) : 0;
            int budget = Math.max(2, 200 / Math.max(1, n - 1));
            int steps = MathUtils.clamp(Math.max(byAngle, byTightness), 2, Math.min(14, budget));
            // Only the last row of a segment is shared with the next segment's
            // first row, so emit [0, steps) here and the final row after the loop.
            for (int k = 0; k < steps; k++) {
                float s = k / (float) steps;
                emitSplineRow(blade, i, s, rail, railA, peak);
                rows++;
            }
        }
        emitSplineRow(blade, n - 2, 1f, rail, railA, peak);
        rows++;

        ribbonIndices(rows, rail.length);
        renderRibbon();
    }

    /**
     * One ribbon row at parameter {@code s} inside stored segment {@code seg},
     * with every channel taken off the same Catmull-Rom so the spine, the width
     * and the fade all stay C1 together.
     */
    private void emitSplineRow(Blade blade, int seg, float s, float[] rail, float[] railA, float peak) {
        int n = blade.count;
        int i0 = Math.max(0, seg - 1);
        int i1 = seg;
        int i2 = Math.min(n - 1, seg + 1);
        int i3 = Math.min(n - 1, seg + 2);
        float bx = catmullRom(blade.baseX[i0], blade.baseX[i1], blade.baseX[i2], blade.baseX[i3], s);
        float by = catmullRom(blade.baseY[i0], blade.baseY[i1], blade.baseY[i2], blade.baseY[i3], s);
        float angle = catmullRom(blade.angle[i0], blade.angle[i1], blade.angle[i2], blade.angle[i3], s);
        float len = catmullRom(blade.len[i0], blade.len[i1], blade.len[i2], blade.len[i3], s);
        float t = MathUtils.lerp(blade.time[i1], blade.time[i2], s);
        emitTrailRow(bx, by, t, angle, len, rail, railA, peak);
    }

    /** Uniform Catmull-Rom, clamped at the ends by duplicating the endpoint control values. */
    private static float catmullRom(float p0, float p1, float p2, float p3, float s) {
        float s2 = s * s;
        float s3 = s2 * s;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * s
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * s2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * s3);
    }

    private void emitTrailRow(float bx, float by, float t, float angle, float len,
                               float[] rail, float[] railA, float peak) {
        float dx = MathUtils.cos(angle);
        float dy = MathUtils.sin(angle);
        // Age fade. Squared-ish so the freshest sliver of the arc carries most of
        // the light and the tail drops away rather than ending on a visible edge.
        float age = MathUtils.clamp((time - t) / TRAIL_SECONDS, 0f, 1f);
        // Cubed rather than squared. The oldest stored pose sits at about 83% of
        // the trail's life, and at a squared fade it still carried 2% alpha --
        // enough, screened over smooth paper, to print the ribbon's own end rail
        // as a faint straight line well clear of the figure. Cubed it is 0.5%.
        float f = 1f - age;
        float fade = f * f * f;
        // The taper. Nonlinear, and fast in the last third, for the same reason
        // STYLE.md 4 gives for a hair strand: "so tips look like a brush lifting
        // off the paper". At the freshest row the ribbon keeps its full width; by
        // the oldest stored pose it is a fifth of it, so the tail converges instead
        // of ending on a rail of constant width.
        float width = 0.18f + 0.82f * f * f;
        for (int j = 0; j < rail.length; j++) {
            float d = len + rail[j] * width;
            float px = bx + dx * d;
            float py = by + dy * d;
            // Same mist as the blade and its sheath -- a trail that stays bright
            // through a band that has swallowed the figure's legs is the tell
            // debt D2 is about, and it applies to the swept arc just as much.
            pushGlowVertex(px, py, peak * fade * railA[j] * (1f - 0.34f * Atmosphere.fogAt(px, py, time)));
        }
    }

    /**
     * STYLE.md 4b.4's specular: "exactly one specular dot, and it must not be
     * centred". A dot this small — a couple of pixels — cannot carry a soft edge
     * as geometry-with-alpha through the cloth buffer, and an emissive quad would
     * be a hard-edged sprite, §10's first fail-on-sight row. So it goes through
     * the same screen-blended glow mesh the blade's light uses: an eight-point
     * fan whose rim sits on zero alpha, which is the one drawing operator in this
     * renderer that can only ever read as light.
     *
     * <p>Call between merge groups (after the face's ink has resolved), with
     * {@code alpha} already scaled by the scene's push-in fade so the dot cannot
     * shimmer at the planning framing.
     */
    public void lightSpeck(float x, float y, float worldRadius, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        flushCloth();
        beginRibbon();
        // 4b.2's specular #FFF6E2: the one warm-white note on a face.
        glowR = 1.0f;
        glowG = 0.965f;
        glowB = 0.886f;
        pushGlowVertex(x, y, alpha);
        int points = 8;
        for (int i = 0; i <= points; i++) {
            float a = (float) (i * Math.PI * 2.0 / points);
            pushGlowVertex(x + worldRadius * MathUtils.cos(a), y + worldRadius * MathUtils.sin(a), 0f);
        }
        glowIndexCount = 0;
        for (int i = 1; i <= points; i++) {
            glowIndices[glowIndexCount++] = 0;
            glowIndices[glowIndexCount++] = (short) i;
            glowIndices[glowIndexCount++] = (short) (i + 1);
        }
        renderRibbon();
        glowR = 0.855f;
        glowG = 0.900f;
        glowB = 0.955f;
    }

    // -- ribbon plumbing -----------------------------------------------------

    private void beginRibbon() {
        glowVertCount = 0;
        glowIndexCount = 0;
    }

    // Pale and distinctly cool, and never white. On the warm Family A paper
    // a near-white glow is invisible -- it differs from the ground by three
    // levels -- so what makes the sheath and the trail read is the warm/cool
    // opposition of STYLE.md 2.2 rather than brightness. Against the ink it
    // still lifts, which is what a luminous edge should do.
    // The eye's speck overrides these with 4b.2's warm specular for one fan
    // and puts them back.
    private float glowR = 0.855f;
    private float glowG = 0.900f;
    private float glowB = 0.955f;

    private void pushGlowVertex(float x, float y, float alpha) {
        if (glowVertCount >= GLOW_MAX_VERTS) {
            return;
        }
        int o = glowVertCount * 6;
        glowVerts[o] = x;
        glowVerts[o + 1] = y;
        glowVerts[o + 2] = glowR;
        glowVerts[o + 3] = glowG;
        glowVerts[o + 4] = glowB;
        glowVerts[o + 5] = alpha;
        glowVertCount++;
    }

    private void ribbonIndices(int rows, int rails) {
        glowIndexCount = 0;
        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < rails - 1; j++) {
                int a = i * rails + j;
                int b = a + 1;
                int c = (i + 1) * rails + j;
                int d = c + 1;
                if (d >= glowVertCount || glowIndexCount + 6 > glowIndices.length) {
                    return;
                }
                glowIndices[glowIndexCount++] = (short) a;
                glowIndices[glowIndexCount++] = (short) c;
                glowIndices[glowIndexCount++] = (short) d;
                glowIndices[glowIndexCount++] = (short) a;
                glowIndices[glowIndexCount++] = (short) d;
                glowIndices[glowIndexCount++] = (short) b;
            }
        }
    }

    private void renderRibbon() {
        if (glowIndexCount == 0) {
            return;
        }
        glowMesh.setVertices(glowVerts, 0, glowVertCount * 6);
        glowMesh.setIndices(glowIndices, 0, glowIndexCount);
        glowShader.bind();
        glowShader.setUniformMatrix("u_projTrans", projTrans);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        // Screen, not straight alpha -- shader-fixes-3 item 6.
        //
        // dst*(1 - src) + src is exactly the screen operator, and (ONE,
        // ONE_MINUS_SRC_COLOR) against a premultiplied source is exactly that.
        // It matters because the trail was being alpha-blended as a cool pale
        // grey over warm cream paper, and a cool grey at 22% alpha over #EDE4D3
        // composites *darker and cooler* than the ground: the trail measured as
        // a dirty smudge rather than as light, which is the opposite of what
        // STYLE.md 5 asks a luminous arc to do. Screen can only lighten, so the
        // ribbon reads as light over both the paper and the ink, and it still
        // cannot reach white the way plain additive would -- the objection
        // STYLE.md 10 raises against glow-on-everything.
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
        glowMesh.render(glowShader, GL20.GL_TRIANGLES, 0, glowIndexCount);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // -- misc ----------------------------------------------------------------

    private void computeFrameRect() {
        inverse.set(projTrans).inv();
        corner.set(-1f, -1f, 0f).prj(inverse);
        float minX = corner.x;
        float minY = corner.y;
        corner.set(1f, 1f, 0f).prj(inverse);
        frameMin.set(minX, minY);
        frameSize.set(corner.x - minX, corner.y - minY);
    }

    private static void setColor(ShaderProgram shader, String name, Color c) {
        shader.setUniformf(name, c.r, c.g, c.b);
    }

    /**
     * Per-emissive-mesh state: which vertices are the guard end and which the
     * point, plus a short history of where those two ended up, which is what the
     * arc-trail is built from. Keyed on mesh identity so a scene with more than
     * one blade keeps them apart.
     */
    private static final class Blade {

        private final float[] verts;
        private final int[] baseVerts;
        private final int[] tipVerts;

        private final float[] time = new float[TRAIL_SAMPLES];
        private final float[] baseX = new float[TRAIL_SAMPLES];
        private final float[] baseY = new float[TRAIL_SAMPLES];
        private final float[] tipX = new float[TRAIL_SAMPLES];
        private final float[] tipY = new float[TRAIL_SAMPLES];
        /** Base-to-tip direction and length per stored pose, rebuilt by {@link #unwrapAngles}. */
        private final float[] angle = new float[TRAIL_SAMPLES];
        private final float[] len = new float[TRAIL_SAMPLES];
        private int count;

        private float bx, by, tx, ty;

        private Blade(float[] verts, int[] baseVerts, int[] tipVerts) {
            this.verts = verts;
            this.baseVerts = baseVerts;
            this.tipVerts = tipVerts;
        }

        /**
         * Ribbon meshes are authored with v running 0 at the attached end to 1 at
         * the free end (contract section A, material-space UV), so the two ends
         * of any bladelike strip fall out of the UVs without this having to know
         * anything about the rig.
         */
        static Blade of(SkinnedMesh mesh) {
            float[] data = new float[mesh.vertexCount() * FLOATS];
            mesh.mesh().getVertices(data);
            int nBase = 0;
            int nTip = 0;
            for (int v = 0; v < mesh.vertexCount(); v++) {
                float uy = data[v * FLOATS + 4];
                if (uy < 0.05f) {
                    nBase++;
                } else if (uy > 0.95f) {
                    nTip++;
                }
            }
            int[] base = new int[nBase];
            int[] tip = new int[nTip];
            int bi = 0;
            int ti = 0;
            for (int v = 0; v < mesh.vertexCount(); v++) {
                float uy = data[v * FLOATS + 4];
                if (uy < 0.05f) {
                    base[bi++] = v;
                } else if (uy > 0.95f) {
                    tip[ti++] = v;
                }
            }
            return new Blade(data, base, tip);
        }

        boolean valid() {
            return baseVerts.length > 0 && tipVerts.length > 0;
        }

        /**
         * Rebuilds the per-pose angle and length channels the trail spline
         * interpolates, with the angles unwrapped into a continuous run.
         *
         * <p>Unwrapping is not cosmetic. A blade sweeping through the -180/+180
         * seam gives {@code atan2} a sign change, and a spline fitted through
         * {@code (+179, -179)} takes the long way round -- 358 degrees of ribbon
         * laid across the frame in one segment. Accumulating the shortest arc
         * instead keeps the stored run monotone through the seam, and because
         * only differences are ever used downstream the absolute value drifting
         * past a turn costs nothing.
         */
        void unwrapAngles() {
            float prev = 0f;
            for (int i = 0; i < count; i++) {
                float dx = tipX[i] - baseX[i];
                float dy = tipY[i] - baseY[i];
                len[i] = Vector2.len(dx, dy);
                float a = MathUtils.atan2(dy, dx);
                if (i == 0) {
                    angle[i] = a;
                } else {
                    float d = a - prev;
                    angle[i] = angle[i - 1] + MathUtils.atan2(MathUtils.sin(d), MathUtils.cos(d));
                }
                prev = a;
            }
        }

        void sample(float[] mats, float now, Vector2 a, Vector2 b) {
            skin(baseVerts, mats, a);
            skin(tipVerts, mats, b);
            bx = a.x;
            by = a.y;
            tx = b.x;
            ty = b.y;

            // Drop anything older than the trail's own life, then append. Doing
            // it here rather than on a clock means a paused scene keeps its
            // trail and a stepped capture keeps exactly the arc it swept.
            int first = 0;
            while (first < count && now - time[first] > TRAIL_SECONDS) {
                first++;
            }
            if (first > 0) {
                for (int i = first; i < count; i++) {
                    time[i - first] = time[i];
                    baseX[i - first] = baseX[i];
                    baseY[i - first] = baseY[i];
                    tipX[i - first] = tipX[i];
                    tipY[i - first] = tipY[i];
                }
                count -= first;
            }
            if (count == TRAIL_SAMPLES) {
                for (int i = 1; i < count; i++) {
                    time[i - 1] = time[i];
                    baseX[i - 1] = baseX[i];
                    baseY[i - 1] = baseY[i];
                    tipX[i - 1] = tipX[i];
                    tipY[i - 1] = tipY[i];
                }
                count--;
            }
            time[count] = now;
            baseX[count] = a.x;
            baseY[count] = a.y;
            tipX[count] = b.x;
            tipY[count] = b.y;
            count++;

            trimToSweep();
        }

        /**
         * The second trim, on accumulated turn rather than on age.
         *
         * <p>Walks back from the newest pose summing the absolute shortest-arc turn
         * between consecutive stored poses, and drops everything older than the point
         * where the sum passes {@link #TRAIL_SWEEP}. Summed absolute rather than
         * end-to-end so a reversal inside the window spends budget instead of
         * cancelling it: the ring the pass-4 review measured is drawn by a sweep, but
         * a sweep-and-return would print the same area with an end-to-end difference
         * of nothing.
         *
         * <p>Two poses are always kept; a trail of one row is not a ribbon, and the
         * cap must never be able to delete the smear STYLE.md 7.2 requires.
         */
        private void trimToSweep() {
            if (count < 3) {
                return;
            }
            float sum = 0f;
            int keepFrom = 0;
            for (int i = count - 1; i > 0; i--) {
                float a1 = MathUtils.atan2(tipY[i] - baseY[i], tipX[i] - baseX[i]);
                float a0 = MathUtils.atan2(tipY[i - 1] - baseY[i - 1], tipX[i - 1] - baseX[i - 1]);
                float d = a1 - a0;
                sum += Math.abs(MathUtils.atan2(MathUtils.sin(d), MathUtils.cos(d)));
                if (sum > TRAIL_SWEEP) {
                    keepFrom = i - 1;
                    break;
                }
            }
            // Keep the pose that took the sum over the cap, so the ribbon still
            // reaches the whole of the permitted turn rather than stopping short of it.
            if (keepFrom <= 0) {
                return;
            }
            keepFrom = Math.min(keepFrom, count - 2);
            for (int i = keepFrom; i < count; i++) {
                time[i - keepFrom] = time[i];
                baseX[i - keepFrom] = baseX[i];
                baseY[i - keepFrom] = baseY[i];
                tipX[i - keepFrom] = tipX[i];
                tipY[i - keepFrom] = tipY[i];
            }
            count -= keepFrom;
        }

        /** Linear blend skinning of a group of vertices, matching ink_skin.vert exactly. */
        private void skin(int[] group, float[] mats, Vector2 out) {
            float sx = 0f;
            float sy = 0f;
            for (int g = 0; g < group.length; g++) {
                int o = group[g] * FLOATS;
                float px = verts[o];
                float py = verts[o + 1];
                for (int slot = 0; slot < 4; slot++) {
                    int wo = o + 9 + slot * 2;
                    int bone = (int) verts[wo];
                    float w = verts[wo + 1];
                    if (w <= 0f) {
                        continue;
                    }
                    int m = bone * 16;
                    sx += w * (mats[m] * px + mats[m + 4] * py + mats[m + 12]);
                    sy += w * (mats[m + 1] * px + mats[m + 5] * py + mats[m + 13]);
                }
            }
            out.set(sx / group.length, sy / group.length);
        }
    }

    /** Shared classpath shader loading, with the compile log surfaced on failure. */
    static final class Shaders {

        private Shaders() {
        }

        static ShaderProgram load(String name) {
            return load(name, name);
        }

        static ShaderProgram load(String vertName, String fragName) {
            FileHandle vert = Gdx.files.classpath("shaders/" + vertName + ".vert");
            FileHandle frag = Gdx.files.classpath("shaders/" + fragName + ".frag");
            ShaderProgram program = new ShaderProgram(vert.readString(), frag.readString());
            if (!program.isCompiled()) {
                throw new IllegalStateException(
                        "Shader '" + vertName + "/" + fragName + "' failed to compile:\n" + program.getLog());
            }
            return program;
        }
    }
}
