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

    // -- the halo (STYLE.md 3.2) ----------------------------------------------
    // Step 1 ships the *tight* halo on purpose. The wide wet bleed is worth
    // keeping and is step 2 of this revision; reintroducing it here would once
    // again make the change unattributable. This reaches roughly 15 px past the
    // coverage, is strongest where the garment is authored to fray, and carries
    // nothing fine.
    float haloShape = smoothstep(0.02, 0.44, cM);
    float haloA = haloShape * (0.05 + 0.21 * bleedAvg) * u_bleedRadius;

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
    float nearness = smoothstep(0.02, 0.55, cM);
    vec3 haloNear = mix(u_base, u_paper, 0.24);
    vec3 haloFar = mix(mix(u_base, u_paper, 0.74), u_stain, 0.16);
    vec3 haloInk = mix(haloFar, haloNear, nearness);
    float bodyFrac = clamp(cov / max(alpha, 1e-4), 0.0, 1.0);
    ink = mix(haloInk, ink, bodyFrac);

    // -- atmosphere (STYLE.md 6) ----------------------------------------------
    // Shared with paper.frag through dev.starfall.render.Atmosphere, so the
    // figure fades into the same mist the paper shows.
    //
    // The tint is 0.18 rather than 0.30 (item 7). All three bands sit on the
    // figure's lower third, which is exactly where STYLE.md 3.4 wants the
    // darkest ink in the picture, and a 30% lift toward #D6D2CE there was
    // fighting the ink gravity this revision is trying to restore. Occlusion
    // still reads: the paper behind is fogged by the same bands, and alpha
    // still drops.
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
    ink = mix(ink, u_fogColor, fog * 0.06);
    alpha *= 1.0 - fog * 0.20;

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
