// Pass 2 of the ink material: one full-screen resolve of the merged material
// buffer onto the paper.
//
// Revision 3 step 1 cuts this shader back hard, along the line the pass-3 review
// drew (docs/system1-shader-fixes-3.md, "The cut line"): almost everything pass
// 3 improved lives in ink_skin.frag, the material-space side, and almost
// everything it broke lives in the merge rule and here, the screen-space side.
//
// So the silhouette is no longer decided here at all. ink_skin.frag delivers a
// real coverage alpha, cut per-fragment at full resolution against a
// material-space distance, and this shader's whole job is:
//
//   * un-premultiply the material channels and soften them across polygon seams;
//   * lay a tight halo outside the coverage, from the quarter-resolution blur;
//   * turn value and stain into colour;
//   * fade the whole thing into the shared fog bands.
//
// What is gone, and must stay gone:
//
//   * the distance ladder (dN/dM/dF) and the fray threshold built on it. The
//     outline is a quarter-resolution gaussian's outline if it is measured from
//     one, which is why pass 3's edges were smooth lobes with round holes;
//   * `dist += (coarse - 0.5) * mix(13, 44, aDis)`. That is the term that
//     intermittently opened a pale lobe *inside* the figure and ate the forearm
//     and hand (item 8). It is not attenuated, it is deleted;
//   * `denser()`, which max-blended whole material samples together and is the
//     screen-space half of the rule item 2 is about.
//
// The noise is still not here and never will be. It is evaluated in
// ink_skin.frag at bind-space material coordinates and arrives already anchored
// to the cloth; sampling it in screen space would make it swim over the figure,
// which is the one failure STYLE.md 10 calls out by name.

#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_uv;
varying vec2 v_worldPos;

// Both premultiplied by coverage. r pool, g stain, b bleed, a coverage.
uniform sampler2D u_mat;      // full res
uniform sampler2D u_covMid;   // quarter res, gaussian sigma ~9 px
uniform sampler2D u_covFar;   // quarter res, gaussian sigma ~20 px
uniform vec2  u_texel;        // 1 / full resolution

uniform float u_time;
uniform vec3  u_base;
uniform vec3  u_deep;
uniform vec3  u_stain;
uniform vec3  u_stainPale;
uniform vec3  u_paper;
uniform vec3  u_fogColor;
uniform float u_bleedRadius;
uniform vec3  u_fogBands[3];

// #161A22. STYLE.md 2.2: ink is blue-black, never #000000.
const vec3 INK_FLOOR = vec3(0.0863, 0.1020, 0.1333);

float hash2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

void main() {
    vec4 m = texture2D(u_mat, v_uv);
    float cov = m.a;

    // -- seam softening -------------------------------------------------------
    // Value and stain are the two channels where a polygon seam can still show
    // as a step, because two adjoining garments may be authored at different
    // wetness and the upper one is genuinely opaque. Averaging over a couple of
    // rings -- coverage-weighted, since the buffer is premultiplied, so a tap
    // that is half paper counts half -- turns any residual step into a wash
    // boundary a few pixels wide. Coverage itself is left strictly alone: it is
    // the silhouette, and softening it here would put the outline back in screen
    // space.
    // Deliberately small. Six pixels of softening was blurring away the
    // dry-brush tooth ink_skin.frag had just put in -- the streaks are 10 to 18
    // px across, which a 7.5 px ring eats -- and item 9 wants that tooth
    // visible. Two and a half pixels is enough to turn a residual seam into a
    // wash boundary without touching the streak scale.
    vec4 sum = m * 0.28;
    vec4 s;

    #define TAP(dx, dy, r, w) { s = texture2D(u_mat, v_uv + vec2(dx, dy) * (r) * u_texel); \
                                sum += s * (w); }

    TAP( 1.0000,  0.0000, 2.5, 0.060)
    TAP( 0.7071,  0.7071, 2.5, 0.060)
    TAP( 0.0000,  1.0000, 2.5, 0.060)
    TAP(-0.7071,  0.7071, 2.5, 0.060)
    TAP(-1.0000,  0.0000, 2.5, 0.060)
    TAP(-0.7071, -0.7071, 2.5, 0.060)
    TAP( 0.0000, -1.0000, 2.5, 0.060)
    TAP( 0.7071, -0.7071, 2.5, 0.060)

    TAP( 0.9239,  0.3827, 5.5, 0.030)
    TAP( 0.3827,  0.9239, 5.5, 0.030)
    TAP(-0.3827,  0.9239, 5.5, 0.030)
    TAP(-0.9239,  0.3827, 5.5, 0.030)
    TAP(-0.9239, -0.3827, 5.5, 0.030)
    TAP(-0.3827, -0.9239, 5.5, 0.030)
    TAP( 0.3827, -0.9239, 5.5, 0.030)
    TAP( 0.9239, -0.3827, 5.5, 0.030)

    vec4 mid = texture2D(u_covMid, v_uv);
    vec4 far = texture2D(u_covFar, v_uv);
    float cM = mid.a;
    float cF = far.a;

    // Un-premultiply. Every one of these divisions is a genuine
    // coverage-weighted average, which is what item 2 asks for in place of the
    // topmost-wins merge.
    float poorlyCovered = step(cov, 0.02);
    float poolPt = m.r / max(cov, 1e-3);
    float stainPt = m.g / max(cov, 1e-3);
    float poolAvg = sum.r / max(sum.a, 1e-3);
    float stainAvg = sum.g / max(sum.a, 1e-3);
    float bleedAvg = mid.b / max(cM, 0.02);
    float poolFar = far.r / max(cF, 0.02);

    // Stain leans harder on the neighbourhood than value does: the authored
    // stainMask is a per-vertex value with deliberate blotchy variation, so it
    // is the channel most able to step across a rail.
    float pool = mix(mix(poolPt, poolAvg, 0.40), poolFar, poorlyCovered);
    float stainAmt = mix(mix(stainPt, stainAvg, 0.80), 0.0, poorlyCovered);

    // -- the wet bleed (STYLE.md 3.2, debt D3) --------------------------------
    // Step 1 shipped only the tight halo and deferred the wide one. Measured on
    // the pass-5 capture, what actually shipped was neither: scanning the left
    // shoulder at y=200 and y=230 the paper holds 218-222 right up to a
    // three-pixel step down to ink 124, with stair-stepping visible at 7x. The
    // review's verdict was that 3.2 is absent above the waist entirely, and it
    // was right. Two independent reasons, both fixed:
    //
    //   * the shape term was smoothstep(0.02, 0.44, cM) on a sigma-8 blur, and
    //     that collapses to zero about four pixels outside the boundary -- the
    //     halo died exactly where it was supposed to start;
    //   * `bleed` was floored at 0.30 where the garment is authored solid, so
    //     the whole term was worth about 11% alpha at its strongest and one
    //     luminance level at the shoulder.
    //
    // Now there are genuinely two halos. The tight one carries the density, out
    // to roughly 12 px. The wide one is read from the sigma-20 field with a much
    // lower knee, so it is still worth ~12% alpha twenty pixels out and decays
    // to nothing by forty -- the "softer and larger than the dissolve band" that
    // 3.2 asks for, and what makes the figure sit *in* the paper. They compose
    // as over rather than adding, so the pair can never exceed the tight one's
    // own ceiling and no edge acquires a hard second ring.
    //
    // This is still not a screen-space measurement of the edge. The silhouette
    // is cut in ink_skin.frag at full resolution in material space and arrives
    // here already decided; these blurs are read strictly *outside* it, where
    // coverage is zero, and can no more move the outline than a shadow can move
    // the object.
    float bleedFar = far.b / max(cF, 0.02);

    // System 3 pass 4. `wick` is the one change, and it enforces the invariant the
    // paragraph above already claims: "these blurs are read strictly *outside* it,
    // where coverage is zero". They were not. `alpha = max(cov, haloA)` applies
    // haloA everywhere, including under solid cloth, and the two additive
    // constants -- 0.09 and 0.05 -- do not answer the material's bleed channel at
    // all. So every fragment inside any silhouette carried a floor of
    //
    //     (0.09 + 0.05 * 0.91) * u_bleedRadius = 0.136 alpha
    //
    // whatever ink_skin.frag wrote. That is invisible while coverage is 1 (the
    // max() takes cov) and decisive the moment the material shader tries to open
    // the sheet: measured on the pass-3 graded window, the brightest pixel
    // anywhere inside `torso`, `hips` or the back skirt is luminance 150 against
    // an ink threshold of 0.85 x paper = 185, and with the material's bleed
    // channel driven to zero over every dry passage `torso` coverage still only
    // reached 94.7% and `hips` did not move off 100.0% at all. STYLE.md 3.3's
    // "coverage is not uniform -- ink skips" was unreachable from the material
    // shader by construction, and three passes of trying to make the ink skip
    // failed against this line rather than against anything in the ink.
    //
    // A halo is pigment wicking *from* ink *into* paper, so where the material
    // reports that this passage barely wicks, the halo must go with it -- floors
    // included. Sized against the 0.52 floor `bleed` carries for ordinary solid
    // cloth, so anything wicking normally is bit-identical and only a passage the
    // material has explicitly opened thins. The exterior halo of STYLE.md 3.2 is
    // untouched for the same reason: it is fed by the coverage-weighted average
    // of the *edge* fragments, and ink_skin.frag leaves those at full bleed.
    float wick = smoothstep(0.02, 0.34, max(bleedAvg, bleedFar));
    float haloTight = smoothstep(0.015, 0.30, cM) * (0.09 + 0.26 * bleedAvg) * wick;
    float haloWide = smoothstep(0.006, 0.20, cF) * (0.05 + 0.14 * bleedFar) * wick;
    float haloA = (haloTight + haloWide * (1.0 - haloTight)) * u_bleedRadius;

    float alpha = max(cov, haloA);

    // -- value (contract F4) --------------------------------------------------
    // pool is signed around 0.5: below it the wash is dilute (where the brush
    // skipped), above it pigment has pooled toward the deep tone.
    vec3 dilute = mix(u_base, u_paper, 0.42);
    vec3 ink = mix(mix(dilute, u_base, clamp(pool * 2.0, 0.0, 1.0)),
                   mix(u_base, u_deep, clamp(pool * 2.0 - 1.0, 0.0, 1.0)),
                   step(0.5, pool));

    // -- ochre underpainting (STYLE.md 2.1) -----------------------------------
    // The stain displaces the ink rather than tinting it -- revision 1 let it
    // arrive as a faint tint over already-dark ink and it measured as mud -- and
    // carries a pale rim where the rust wash thinned out and a darker wet edge
    // where it stopped spreading.
    float wetEdge = smoothstep(0.05, 0.20, stainAmt) * (1.0 - smoothstep(0.20, 0.46, stainAmt));
    ink = mix(ink, mix(u_stain, u_deep, 0.45), wetEdge * 0.55);
    ink = mix(ink, u_stain, stainAmt * 0.97);
    ink = mix(ink, u_stainPale, smoothstep(0.45, 0.90, stainAmt) * 0.70);

    // -- the halo's own colour ------------------------------------------------
    // Dilute pigment is not the core tone at lower opacity. As a wash wicks out
    // through damp paper the pigment separates: it loses the indigo first and
    // ends up greyer and warmer than where it came from. Reading the same tint
    // all the way out is what makes a bleed look like a gaussian blur.
    float nearness = smoothstep(0.010, 0.45, cM);
    vec3 haloNear = mix(u_base, u_paper, 0.24);
    vec3 haloFar = mix(mix(u_base, u_paper, 0.74), u_stain, 0.16);
    vec3 haloInk = mix(haloFar, haloNear, nearness);
    float bodyFrac = clamp(cov / max(alpha, 1e-4), 0.0, 1.0);
    ink = mix(haloInk, ink, bodyFrac);

    // -- atmosphere (STYLE.md 6) ----------------------------------------------
    // Shared with paper.frag through dev.starfall.render.Atmosphere, so the
    // figure fades into the same mist the paper shows.
    //
    // Debt D5, and this is the largest single term in it. Measured on the p6
    // bind capture, mean ink luminance ran 65 at the chest and 125-159 from the
    // knee down, where references 1 and 2 put their single darkest passage
    // exactly at the knee. Two of the three fog bands overlap across the whole
    // lower leg, so `fog` saturates there, and the figure was paying for it
    // twice: a 20% alpha cut let a mist that is *brighter than the paper*
    // (250,241,208) show straight through the hem, and a 6% tint lifted what
    // survived.
    //
    // The mistake is a depth mistake, not a strength one. STYLE.md 6 puts fog
    // *between* depth layers and reserves "lose saturation and contrast and
    // gain fog until they are barely more than a value shape" for **background**
    // figures; in image 8 the foreground woman keeps a dark garment to the
    // bottom of frame while the receding figures dissolve. The hero stands in
    // front of these bands. So the bands keep their full strength on the paper
    // -- the ground pass is untouched, the band geometry in
    // dev.starfall.render.Atmosphere is untouched, and depth still reads,
    // because a dark figure against a band of luminous mist is *more*
    // separated from the ground than one against bare paper, not less.
    //
    // What is left on the figure is a residual: enough that it is demonstrably
    // in the same air as everything else, far too little to erase it. When
    // System 3 gives the lane real depth layers, the scale factor here is the
    // knob a background figure turns back up.
    float fog = 0.0;
    for (int i = 0; i < 3; i++) {
        vec3 b = u_fogBands[i];
        float fi = float(i);
        float drift = u_time * (0.055 + 0.031 * fi);
        float wobble = b.y * (0.30 * sin(v_worldPos.x * (0.85 + 0.45 * fi) + drift + fi * 2.1)
                            + 0.19 * sin(v_worldPos.x * 2.43 - drift * 0.7 + fi)
                            + 0.11 * sin(v_worldPos.x * 5.11 + drift * 1.6 - fi * 1.7));
        float d = abs(v_worldPos.y - (b.x + wobble)) / b.y;
        fog += b.z * (1.0 - smoothstep(0.0, 1.0, d));
    }
    fog = clamp(fog, 0.0, 1.0);
    // The tint is weighted by how dilute the passage is. A loaded black
    // passage physically holds its value through thin mist and a dry frayed
    // one does not, and it is the frayed hem that is *supposed* to dissolve --
    // so the ink cloud at the knee keeps its density while the smoke below it
    // still goes into the air. pool is signed around 0.5; 0.62 upward is the
    // pooled half of the ramp.
    float dense = smoothstep(0.52, 0.86, pool);
    ink = mix(ink, u_fogColor, fog * 0.05 * (1.0 - 0.75 * dense));
    alpha *= 1.0 - fog * 0.06 * (1.0 - 0.6 * dense);

    // Sub-LSB dither. The material buffer is 8-bit and several of the fields
    // above are smooth ramps across large areas; without this they quantise into
    // faint terraces, which is the same visual signature as the artefact this
    // revision is removing and would be a poor way to reintroduce it.
    float d8 = hash2(floor(v_uv / u_texel)) - 0.5;
    ink += d8 * (1.6 / 255.0);
    alpha += d8 * (1.0 / 255.0);

    // STYLE.md 2.2 / anti-pattern table: nothing reaches either end of the range.
    ink = clamp(ink, INK_FLOOR, vec3(0.96));

    gl_FragColor = vec4(ink, clamp(alpha, 0.0, 1.0));
}
