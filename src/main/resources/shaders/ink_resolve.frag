// Pass 2 of the ink material: one full-screen resolve of the merged material
// buffer onto the paper.
//
// This is where the whole of STYLE.md 3 is applied, and it is applied *once*,
// over coverage that has already been merged across every garment ribbon. The
// three nested regions the reference paintings show at any garment edge are all
// cut from the same fields so they agree with each other:
//
//     halo   : the widest coverage blur, low alpha, no fine detail  -> soft, far
//     flecks : the material field thresholded near the silhouette   -> discrete marks
//     body   : the same field, far above the threshold              -> solid ink
//
// The thing that makes this different from revision 1, and the reason the
// refactor was worth it: "distance to the edge" is now distance to the *merged*
// silhouette, measured in every direction, instead of distance to one ribbon's
// own rail along one axis. So the torso's sides fray exactly the way the hem
// does, the head stops sprouting a fringe of radial spurs at its triangle-fan
// seams, and the bleed is free to reach far outside the authored polygon --
// which it physically could not do before, since the garments are two-vertex
// strips with no room beyond the rail.
//
// What is *not* here is the noise. It is evaluated in ink_skin.frag at bind-space
// material coordinates and arrives through u_mat already anchored to the cloth.
// Sampling it here, in screen space, would make it swim over the figure, which
// is the one failure STYLE.md 10 calls out by name and the property reviewers
// have twice confirmed this material gets right.

#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_uv;
varying vec2 v_worldPos;

uniform sampler2D u_mat;      // full res:    r field, g pool, b stain, a weight
uniform sampler2D u_covMid;   // quarter res: r coverage (sigma ~9px), g pool
uniform sampler2D u_covFar;   // quarter res: r coverage (sigma ~20px), g pool
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

float cvg(float w) {
    return smoothstep(0.05, 0.35, w);
}

// Picks whichever of two material samples carries more garment. Used to dilate
// the material field a few pixels outside the authored polygon so flecks can
// detach from the silhouette instead of stopping dead at the mesh rail.
vec4 denser(vec4 a, vec4 b) {
    return mix(a, b, step(a.a + 0.0005, b.a));
}

float hash2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

void main() {
    vec4 m = texture2D(u_mat, v_uv);

    // -- near field: coverage and dilation from one shared tap set ------------
    // Two rings, sixteen taps, rotated against each other. The averaged coverage
    // is a gaussian of about 3.2 pixels, close enough to the silhouette to place
    // the fray band; the same taps' densest sample extends the material a few
    // pixels past the polygon so the outermost marks have something to be cut
    // from.
    vec4 best = m;
    float cov = cvg(m.a) * 0.14;
    // Value and stain are averaged over the same taps. They are the two channels
    // where a polygon seam shows directly as a straight-edged step -- the merge
    // keeps the topmost strip's material rather than blending it, so a dry
    // garment laid over a wet one replaces its value outright and a stained row
    // stops dead at its neighbour's rail. Six pixels of softening turns both
    // steps into wash boundaries. The ink field itself is deliberately left
    // sharp: softening that would take the flecks with it.
    float poolSum = m.g * cvg(m.a) * 0.14;
    float stainSum = m.b * cvg(m.a) * 0.14;
    float poolDen = cvg(m.a) * 0.14;
    vec4 s;

    #define TAP(dx, dy, r, w) { s = texture2D(u_mat, v_uv + vec2(dx, dy) * (r) * u_texel); \
                                best = denser(best, s); cov += cvg(s.a) * (w); \
                                poolSum += s.g * cvg(s.a) * (w); stainSum += s.b * cvg(s.a) * (w); \
                                poolDen += cvg(s.a) * (w); }

    TAP( 1.0000,  0.0000, 3.0, 0.050)
    TAP( 0.7071,  0.7071, 3.0, 0.050)
    TAP( 0.0000,  1.0000, 3.0, 0.050)
    TAP(-0.7071,  0.7071, 3.0, 0.050)
    TAP(-1.0000,  0.0000, 3.0, 0.050)
    TAP(-0.7071, -0.7071, 3.0, 0.050)
    TAP( 0.0000, -1.0000, 3.0, 0.050)
    TAP( 0.7071, -0.7071, 3.0, 0.050)

    TAP( 0.9239,  0.3827, 7.0, 0.036)
    TAP( 0.3827,  0.9239, 7.0, 0.036)
    TAP(-0.3827,  0.9239, 7.0, 0.036)
    TAP(-0.9239,  0.3827, 7.0, 0.036)
    TAP(-0.9239, -0.3827, 7.0, 0.036)
    TAP(-0.3827, -0.9239, 7.0, 0.036)
    TAP( 0.3827, -0.9239, 7.0, 0.036)
    TAP( 0.9239, -0.3827, 7.0, 0.036)

    // The third ring is what keeps the near-field distance estimate useful out
    // to five pixels or so, which is where the quarter-resolution fields have to
    // take over. It doubles as the reach that extends the material past the
    // authored polygon, so the outermost flecks are cut from real cloth noise
    // instead of from the buffer's neutral clear value.
    TAP( 1.0000,  0.0000, 11.5, 0.021)
    TAP( 0.7071,  0.7071, 11.5, 0.021)
    TAP( 0.0000,  1.0000, 11.5, 0.021)
    TAP(-0.7071,  0.7071, 11.5, 0.021)
    TAP(-1.0000,  0.0000, 11.5, 0.021)
    TAP(-0.7071, -0.7071, 11.5, 0.021)
    TAP( 0.0000, -1.0000, 11.5, 0.021)
    TAP( 0.7071, -0.7071, 11.5, 0.021)

    vec4 mid = texture2D(u_covMid, v_uv);
    vec4 far = texture2D(u_covFar, v_uv);
    float cM = mid.r;
    float cF = far.r;

    // -- signed distance to the merged silhouette, in pixels ------------------
    // A blurred step edge is 0.5 at the boundary and moves away from it at about
    // 1/(2.5*sigma) per pixel, so each radius gives a distance that is accurate
    // near the edge and saturates beyond about 2.5 sigma. Each hand-off has to
    // complete *before* the term it is leaving saturates, or the blend stalls
    // part-way and the whole field flattens out: dN cannot exceed 4, dM cannot
    // exceed 11.5, so those are the ceilings the crossovers sit under. Positive
    // is inside; the ladder tops out around 25 px, which is deeper than any
    // fray band needs to reach.
    float dN = (cov - 0.5) * 11.0;
    float dM = (cM  - 0.5) * 23.0;
    float dF = (cF  - 0.5) * 50.0;
    float dist = dN;
    // The near term is full resolution and the other two are quarter, so hand
    // over as late as the near term's own range allows: crossing early puts a
    // bilinear-upsampled quarter-resolution field right at the silhouette, where
    // it prints as a four-pixel blocky stipple along every edge.
    dist = mix(dist, dM, smoothstep(3.0, 4.8, abs(dN)));
    dist = mix(dist, dF, smoothstep(4.5, 9.0, abs(dM)));

    // The silhouette wanders. A garment-scale average of the ink field itself --
    // blurred, coverage-normalised, and therefore still anchored to the cloth --
    // pushes the measured edge in and out in lobes tens of pixels across. This is
    // what a loaded brush does at the end of a stroke, and without it the fray
    // is a monotone ramp inward from wherever the polygon happened to end: a
    // blurred sprite rather than a wash breaking up. The finer octaves left in
    // the full-resolution field then cut that wandering boundary into islands.
    float coarse = mid.a / max(mid.r, 0.02);

    // Inside the polygon use this pixel's own material; outside, the dilated one.
    float here = cvg(m.a);
    vec4 mat = mix(best, m, here);
    float field = mat.r;
    float pool = mix(mat.g, poolSum / max(poolDen, 1e-4), 0.60 * step(0.05, poolDen));
    float stainAmt = mix(mat.b, stainSum / max(poolDen, 1e-4), 0.70 * step(0.05, poolDen));

    // The authored dissolve, taken from the coverage-normalised blur rather than
    // point-wise from this pixel. It arrives MAX-blended, so read directly it
    // steps from one region's value to another's along every polygon seam, and
    // the fray width steps with it -- a straight line across the middle of the
    // figure, which is the mesh outline STYLE.md 3 forbids arriving by the back
    // door.
    float aDis = clamp((1.0 - mid.b / max(mid.r, 0.02)) / 0.28, 0.0, 1.0);
    float haloPool = far.g / max(far.r, 0.02);

    // How far the boundary is allowed to wander is itself a function of the
    // authored dissolve: a shoulder crest keeps its shape, a hem detonates.
    // Ungated, this dissolved the whole figure into one undifferentiated cloud.
    dist += (coarse - 0.5) * mix(13.0, 44.0, aDis) * mix(0.40, 1.0, smoothstep(0.50, 0.92, cF));

    // -- the fray (STYLE.md 3.1, shader-fixes item 2) -------------------------
    // The threshold ramps across the silhouette from "nothing survives" outside
    // to "everything survives" inside, over a band whose width is set by the
    // authored dissolve: a few pixels at the shoulder crest, tens of pixels at
    // the hem. Because dist is a real 2D distance, this is the same behaviour on
    // the torso's sides as on its bottom edge.
    // The band has to be *spatially* wide, not just soft. If the threshold
    // sweeps its whole range over four or five pixels it beats the field's own
    // gradient everywhere and the result is a clean curve a few pixels inside
    // the polygon -- a hard-edged sprite with feathered antialiasing, which is
    // what the first attempt at this produced. Spread over twenty-odd pixels the
    // field's mid octaves win locally, the boundary wanders, and it breaks into
    // islands: flecks.
    // Scaled by how much cloth there is here, not just by the authored dissolve.
    // The widest blur only saturates over a genuinely large mass, so it doubles
    // as a thickness measure: a haori hem two hundred pixels across explodes,
    // while the head -- fifty pixels of the densest black in the picture -- keeps
    // its shape. A fray band sized for the hem and applied everywhere simply
    // deletes every small feature in the figure.
    float mass = smoothstep(0.50, 0.92, cF);
    float frayIn = mix(4.0, 16.0, mass) * mix(1.0, 2.1, aDis);
    float frayOut = mix(3.0, 12.0, mass) * mix(1.0, 1.8, aDis);
    float ramp = smoothstep(-frayOut, frayIn, dist);
    // The additive term is what keeps a hem breaking up even well away from its
    // own boundary -- the bottom third of the garment is meant to be ink smoke,
    // not a solid shape with a frayed outline. The floor of the ramp is above
    // zero for the same reason in miniature: it leaves the dry brush enough room
    // to skip ink out of the solid interior.
    float thr = mix(1.18, 0.20, ramp) + aDis * 0.90;

    // Contract F1: soft threshold, widening as the marks get sparser so the last
    // flecks are the most feathered of all.
    // Narrow. Contract F1 asks for a soft band so flecks are feathered rather
    // than aliased, but the feather is measured in field units against the
    // field's own slope: at these frequencies 0.09 is about two pixels of
    // feathering, and anything much wider dissolves the individual marks back
    // into the gradient they were cut from.
    float band = 0.09 + 0.11 * aDis;

    // Dense masses stay dense. The dry brush skips ink out of the field, which is
    // right at a stroke's edge and wrong in the middle of the darkest mass in the
    // picture -- the pass-2 review found the head, which should be the most solid
    // black in the frame, punched full of light speckle instead. Well inside a
    // low-dissolve region the field is floored, so the dry brush there reads as
    // value only.
    float core = smoothstep(6.0, 18.0, dist) * (1.0 - aDis);
    field = mix(field, max(field, 0.78), core * 0.80);

    float body = smoothstep(thr - band, thr + band, field);

    // -- detached flecks (STYLE.md 3.1, shader-fixes item 4) ------------------
    // Thresholding a field against a distance ramp gives a *wandering* boundary,
    // never a broken one: for the edge to break into islands the field's swing
    // would have to exceed the threshold's whole sweep, and it never does at any
    // band width narrow enough to keep the figure solid. Measured, the result is
    // a clean five-pixel step under a wide halo -- a blurred sprite.
    //
    // So the marks that separate from the edge are cut separately, by keeping
    // only the field's peaks inside a zone straddling the silhouette. Because
    // the field is four octaves, its peaks come in a wide range of sizes and sit
    // off the axis of whatever edge they came from, and the partial values of
    // the smoothstep give each one an opacity unrelated to how big it is -- the
    // three things STYLE.md 10's "symmetric, uniform particle bursts" is about.
    // Cut from the field's *high-pass* residual -- itself minus its own
    // garment-scale average -- rather than from the field directly. The field's
    // local level varies enormously from one passage of cloth to the next, so a
    // fixed threshold on it puts marks along some edges and none at all along
    // others; the residual is centred in the same place everywhere, so the marks
    // come out evenly distributed along the whole silhouette while still varying
    // in size, because the residual still carries three octaves.
    float speckZone = (1.0 - smoothstep(-2.0, frayIn * 0.80, dist))
                    * smoothstep(-frayOut - 2.0, -frayOut + 9.0, dist);
    float residual = field - coarse + 0.5;
    float specks = smoothstep(0.58, 0.84, residual) * speckZone;
    body = max(body, specks);

    // -- the wet bleed (STYLE.md 3.2, shader-fixes item 3) --------------------
    // The widest blur, at low alpha, with nothing fine in it. It reaches roughly
    // 50 pixels past the silhouette against the fray's 10-15, which is the 3-5x
    // ratio the style calls for, and it is what makes the figure sit *in* the
    // paper instead of on it.
    float haloShape = smoothstep(0.02, 0.80, cF);
    float haloA = haloShape * (0.07 + 0.21 * haloPool) * u_bleedRadius;

    float alpha = max(body, haloA);

    // -- value (contract F4) --------------------------------------------------
    // pool is signed around 0.5: below it the wash is dilute (where the brush
    // skipped), above it pigment has pooled toward the deep tone.
    vec3 dilute = mix(u_base, u_paper, 0.42);
    vec3 ink = mix(mix(dilute, u_base, clamp(pool * 2.0, 0.0, 1.0)),
                   mix(u_base, u_deep, clamp(pool * 2.0 - 1.0, 0.0, 1.0)),
                   step(0.5, pool));

    // Watercolour strands pigment at the drying front, so every fleck carries a
    // darker rim. Kept weak and wide on purpose: a tight version of this turns
    // the noise field's iso-contours into visible value terraces, which is
    // exactly the artefact the pass-2 review found on the shoulder.
    float inner = smoothstep(thr + 0.05, thr + 0.55, field);
    ink = mix(ink, u_deep, (1.0 - inner) * body * 0.22);

    // -- ochre underpainting (STYLE.md 2.1) -----------------------------------
    // The one saturated passage in the figure. Revision 1 let this arrive as a
    // faint tint over already-dark ink and it measured as mud; here the stain
    // displaces the ink rather than tinting it, and carries a pale rim where the
    // rust wash thinned out and a darker wet edge where it stopped spreading.
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
    float bodyFrac = clamp(body / max(alpha, 1e-4), 0.0, 1.0);
    ink = mix(haloInk, ink, bodyFrac);

    // -- atmosphere (STYLE.md 6) ----------------------------------------------
    // Shared with paper.frag through dev.starfall.render.Atmosphere, so the
    // figure fades into the same mist the paper shows. Low-frequency and smooth
    // by construction: this is the one field here that lives in world space, and
    // anything grainy in world space would read as swimming noise.
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
    ink = mix(ink, u_fogColor, fog * 0.30);
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
