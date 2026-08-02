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

    /** Contract section B. Matches MAX_BONES in ink_skin.vert. */
    private static final int MAX_BONES = 32;

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
    private float clothBleed = 1f;

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

        clothBase.set(material.base);
        clothDeep.set(material.deep);
        clothStain.set(material.stain);
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
        mesh.mesh().render(matShader, GL20.GL_TRIANGLES);
        anyCloth = true;
    }

    public void end() {
        flushCloth();
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
        setColor(resolveShader, "u_stainPale", Palette.OCHRE_PALE);
        setColor(resolveShader, "u_paper", Palette.PAPER_WARM);
        setColor(resolveShader, "u_fogColor", Palette.FOG);
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
        final float[] rail = {-0.28f, -0.20f, -0.12f, -0.05f, 0f, 0.04f};
        final float[] railA = {0f, 0.30f, 0.62f, 0.92f, 1f, 0f};
        final float peak = 0.24f;
        int rows = 0;

        for (int i = 0; i < n - 1; i++) {
            float t0 = blade.time[i];
            float t1 = blade.time[i + 1];
            float a0 = MathUtils.atan2(blade.tipY[i] - blade.baseY[i], blade.tipX[i] - blade.baseX[i]);
            float a1 = MathUtils.atan2(blade.tipY[i + 1] - blade.baseY[i + 1], blade.tipX[i + 1] - blade.baseX[i + 1]);
            float da = MathUtils.atan2(MathUtils.sin(a1 - a0), MathUtils.cos(a1 - a0));
            float l0 = Vector2.len(blade.tipX[i] - blade.baseX[i], blade.tipY[i] - blade.baseY[i]);
            float l1 = Vector2.len(blade.tipX[i + 1] - blade.baseX[i + 1], blade.tipY[i + 1] - blade.baseY[i + 1]);

            int budget = Math.max(1, 180 / Math.max(1, n - 1));
            int steps = MathUtils.clamp(
                    Math.round(Math.abs(da) * MathUtils.radiansToDegrees / 4f), 1, Math.min(10, budget));
            // Only the last row of a segment is shared with the next segment's
            // first row, so emit [0, steps) here and the final row after the loop.
            for (int k = 0; k < steps; k++) {
                float s = k / (float) steps;
                emitTrailRow(blade, i, s, t0 + (t1 - t0) * s, a0 + da * s, l0 + (l1 - l0) * s, rail, railA, peak);
                rows++;
            }
        }
        emitTrailRow(blade, n - 2, 1f, blade.time[n - 1],
                MathUtils.atan2(blade.tipY[n - 1] - blade.baseY[n - 1], blade.tipX[n - 1] - blade.baseX[n - 1]),
                Vector2.len(blade.tipX[n - 1] - blade.baseX[n - 1], blade.tipY[n - 1] - blade.baseY[n - 1]),
                rail, railA, peak);
        rows++;

        ribbonIndices(rows, rail.length);
        renderRibbon();
    }

    private void emitTrailRow(Blade blade, int seg, float s, float t, float angle, float len,
                               float[] rail, float[] railA, float peak) {
        float bx = MathUtils.lerp(blade.baseX[seg], blade.baseX[seg + 1], s);
        float by = MathUtils.lerp(blade.baseY[seg], blade.baseY[seg + 1], s);
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
        for (int j = 0; j < rail.length; j++) {
            float d = len + rail[j];
            float px = bx + dx * d;
            float py = by + dy * d;
            // Same mist as the blade and its sheath -- a trail that stays bright
            // through a band that has swallowed the figure's legs is the tell
            // debt D2 is about, and it applies to the swept arc just as much.
            pushGlowVertex(px, py, peak * fade * railA[j] * (1f - 0.34f * Atmosphere.fogAt(px, py, time)));
        }
    }

    // -- ribbon plumbing -----------------------------------------------------

    private void beginRibbon() {
        glowVertCount = 0;
        glowIndexCount = 0;
    }

    private void pushGlowVertex(float x, float y, float alpha) {
        if (glowVertCount >= GLOW_MAX_VERTS) {
            return;
        }
        int o = glowVertCount * 6;
        glowVerts[o] = x;
        glowVerts[o + 1] = y;
        // Pale and distinctly cool, and never white. On the warm Family A paper
        // a near-white glow is invisible -- it differs from the ground by three
        // levels -- so what makes the sheath and the trail read is the warm/cool
        // opposition of STYLE.md 2.2 rather than brightness. Against the ink it
        // still lifts, which is what a luminous edge should do.
        glowVerts[o + 2] = 0.855f;
        glowVerts[o + 3] = 0.900f;
        glowVerts[o + 4] = 0.955f;
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
