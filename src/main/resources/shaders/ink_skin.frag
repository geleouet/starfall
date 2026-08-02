// Pass 1 of the ink material: skinned garment geometry -> a merged material
// buffer. This shader writes *material data*, never final colour -- but as of
// revision 3 step 1 it also cuts the silhouette, because that is the one job
// that has to happen here.
//
// Why the cut moved back here (system1-shader-fixes-3 items 1, 3, 5, 8).
// Revision 2 of the split evaluated the fray threshold, the fleck islands and
// the edge feather in ink_resolve.frag, against a distance measured from a
// quarter-resolution blur of the coverage. Three consequences, all fatal:
//
//   * the silhouette came out shaped like a quarter-resolution gaussian --
//     smooth lobes with round holes and round detached dots, where the
//     reference paintings and pass 2 both give angular torn shards;
//   * the fray band's width was read from a ~9 px blur of the authored
//     dissolve, so the haori hem's dissolve leaked across the whole figure and
//     a band sized for the hem deleted the neck, the skull and the sword arm;
//   * a garment-scale term pushed the measured boundary by +-13..44 px, which
//     intermittently opened a pale lobe *inside* the figure and ate the
//     forearm.
//
// All three are properties of measuring the edge in screen space. Here the
// distance is a material-space quantity -- how far into its own strip this
// fragment sits, converted to pixels with fwidth -- so it is exact at full
// resolution, it is per-fragment, and the authored dissolve that scales it is
// this fragment's own rather than a blur of its neighbours'.
//
// Everything is still evaluated at v_matPos, the position the vertex was
// authored at in bind space, so the pattern stays nailed to the cloth as it
// deforms (STYLE.md 3.5).
//
// Channel layout, and the blend state it assumes (set in InkSkinnedRenderer):
//
//     r  pool  x cov   value: 0.5 is base tone, 1.0 pools deep, 0.0 lifts dilute
//     g  stain x cov   ochre underpainting strength
//     b  bleed x cov   how hard this passage wicks into the paper
//     a  cov           ink coverage after the fray -- a real alpha
//
// All four premultiplied by coverage, blended ONE / ONE_MINUS_SRC_ALPHA over a
// buffer cleared to zero. That is ordinary "over" compositing, which *averages*
// overlapping ribbons in proportion to how much ink each actually deposits.
// Revision 2 instead MAX-blended a near-constant weight, so the topmost ribbon
// replaced its neighbour's value and stain outright inside its own quad and the
// quad's rail printed as a hard step -- the three axis-aligned bars through the
// torso that failed pass 3 (item 2). Nothing here max-blends.

#ifdef GL_ES
#extension GL_OES_standard_derivatives : enable
precision highp float;   // the fract()-based hashes below smear badly at mediump
#endif

varying vec2 v_matPos;
varying vec2 v_uv;
varying vec4 v_ink;      // r dissolve, g wetness, b stainMask, a flowU (already decoded)
varying vec2 v_flowDir;
varying vec2 v_worldPos;

uniform float u_time;
uniform float u_dissolveBias;
uniform float u_paperGrain;

// -- noise ------------------------------------------------------------------
// Value noise, not gradient noise: value noise has broad flat plateaus separated
// by fast transitions, which is much closer to how a wash breaks up than
// Perlin's smooth undulation. Hashes are fract-based so there are no integer ops.

float hash2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float hash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}

// Directional smear, and the reason there is no rotated (u, v) stroke frame in
// this shader any more.
//
// Revision 1 built one -- q = (dot(mp, dir), dot(mp, perp)) -- and sampled
// stretched noise in it. That is fine while dir turns slowly along a garment
// strip and catastrophic where it turns quickly: dot(mp, dir) sweeps the whole
// length of mp as dir rotates, so around the head, whose vertices author flow
// radially outward from the scalp, the noise argument cycled dozens of times
// through one turn and printed a fringe of radial spokes. That is the black
// dandelion two reviews have now called out.
//
// Averaging isotropic noise at a pair of offsets along the flow direction gives
// the same elongated marks with none of that: the sample point moves by at most
// the offset itself, however fast the frame spins.
// The offset is 0.30 of a noise cell everywhere below, and that number is not
// free. A two-tap average at +-d has frequency response cos(2*pi*d/lambda), and
// value noise's own characteristic wavelength is about two cells -- so an offset
// of half a cell puts the two taps in antiphase with each other and *annihilates
// the octave it is smearing*. Revision 2 ran the 7.30 octave at 0.44 and the
// 15.50 octave at 0.54, which retain 19% and -12% of their amplitude
// respectively: two of the four mark octaves were effectively not in the image
// at all, and the dry brush at 0.99 was cancelled outright. That is a large part
// of why pass 3's edges came out as smooth blobs with no tooth inside them.
//
// 0.30 keeps 59% of the octave and is also exactly the 1.6:1 anisotropy the
// spec caps at: the correlation length along dir is 1 + 2d cells against 1
// across. Do not raise it to get more elongation -- past about 0.35 the octave
// starts cancelling faster than the mark stretches.
float dnoise2(vec2 p, vec2 d) {
    return 0.5 * (vnoise(p - d) + vnoise(p + d));
}

float dnoise3(vec2 p, vec2 d) {
    return 0.25 * (vnoise(p - d) + 2.0 * vnoise(p) + vnoise(p + d));
}

float vnoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash3(i);
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

void main() {
    vec2 mp = v_matPos;

    float dissolve = clamp(v_ink.x + u_dissolveBias, 0.0, 1.0);
    float wetness = v_ink.y;
    float stainMask = v_ink.z;

    // flowU says which way the brush was travelling here (contract section C).
    // Every anisotropic field below smears along it, so marks and tooth run down
    // the limb rather than across the screen.
    vec2 dir = normalize(v_flowDir + vec2(1e-5, 0.0));

    // -- how far into its own strip this fragment sits, in pixels -------------
    // Material-space UV: x runs across the strip, y along it (contract A), and
    // every garment region fills the unit square. So min(u, 1-u) and min(v, 1-v)
    // are the two distances to the strip's own boundary, and dividing each by
    // the *derivative of the interpolant itself* converts them to pixels.
    //
    // The derivative is taken of v_uv, which is linear across a triangle, and
    // never of the min() or the abs(): fwidth of a folded quantity spikes along
    // the fold, and the fold here runs down the centreline of every ribbon, so
    // that spike would print a straight line down the middle of every garment.
    float pxU = 1.0 / max(fwidth(v_uv.x), 1e-6);
    float pxV = 1.0 / max(fwidth(v_uv.y), 1e-6);
    float chebPx = min(min(v_uv.x, 1.0 - v_uv.x) * pxU,
                       min(v_uv.y, 1.0 - v_uv.y) * pxV);

    // The head and the topknot are discs, not strips: their UVs fill the disc
    // inscribed in the unit square, so the square metric above reads their rim
    // as up to 9 px *inside* the figure at the four diagonals and would leave
    // those arcs as hard polygon edges. Distance to the inscribed disc is zero
    // all the way round a disc and is far larger than the square metric
    // everywhere inside a long thin ribbon, so capping by it costs the ribbons
    // almost nothing and gives the skull a uniform rim.
    float pxG = sqrt(pxU * pxV);
    float discPx = (0.5 - length(v_uv - vec2(0.5))) * pxG;
    float edgePx = min(chebPx, max(discPx, 0.0) + 3.0);

    // How thick this passage of cloth is, in pixels, straight out of the same
    // interpolants: u spans the strip, so 0.5*pxU *is* its half-width on screen.
    //
    // This is the local-mass measure the fray band needs, and having it here
    // rather than as a blur of the coverage is the whole difference between
    // items 5 and 8 being fixed and being reintroduced. A band sized for the
    // haori -- 128 px of cloth -- applied to the 30 px neck column or the 25 px
    // hand deletes them outright, which is why the figure has been reading as a
    // headless armour stand. Sized *per strip* the haori can detonate while the
    // neck, the skull, the hand and the tsuka keep their shape.
    float halfPx = clamp(0.5 * min(pxU, pxV), 3.0, 90.0);

    // -- the wash -------------------------------------------------------------
    // Garment-scale octaves in 3D, the third dimension being time at a few
    // hundredths of a turnover per second: STYLE.md 3.6's "pigment still
    // settling", not something you can watch move. Reviewers verified this
    // evolution is slow and non-swimming; the rates here are unchanged.
    float tz = u_time * 0.055;
    float wash = vnoise3(vec3(mp * 2.30, tz)) * 0.52
               + vnoise3(vec3(mp * 5.30 + 19.0, tz * 1.6 + 5.0)) * 0.30
               + vnoise3(vec3(mp * 11.10 - 7.0, tz * 2.4 + 11.0)) * 0.18;

    // -- the mark field -------------------------------------------------------
    // Four octaves spanning about 10:1, because a single scale of break-up reads
    // as torn wet cardboard rather than as a brush running out of ink: the big
    // lobes have to have small marks flaking off ahead of them.
    //
    // The frequencies are pass 3's and are carried forward verbatim -- they are
    // the real fix for the periodic torso banding that failed two reviews, and
    // the architecture never was. At this camera (200 px per world unit) their
    // periods are 65, 27, 13 and 8.3 px, all clear of STYLE.md 3b.1's 2 px hard
    // floor and clear of the 3-5 px ripple the review measured. Anisotropy stays
    // under 1.6:1 for the same reason.
    vec2 creep = vec2(u_time * 0.0035, u_time * -0.0022);
    float marks = dnoise2(mp * 3.10 + creep * 11.0, dir * 0.30) * 0.22
                + dnoise2(mp * 7.30 + 3.7 - creep * 7.0, dir * 0.30) * 0.26
                + dnoise2(mp * 15.50 - 5.1, dir * 0.30) * 0.26
                + dnoise2(mp * 24.00 + 12.9, dir * 0.30) * 0.26;

    // Contrast expansion: an fbm sum piles up around 0.5, and a threshold that
    // only ever sees 0.35..0.65 cannot shed sparse flecks at high dissolve.
    float field = clamp(0.5 + (mix(wash, marks, 0.70) - 0.5) * 2.90, 0.0, 1.0);

    // -- dry brush (contract F3) ----------------------------------------------
    // ~18 and ~10 px across the stroke, which is brush-hair scale rather than
    // the 2.5 px grating scale that corrugated the chest. Gated by a two-octave
    // blotch mask so the tooth only shows in passages, the way it does in the
    // references, and suppressed where the cloth is authored wet -- a loaded
    // hem does not skip, and letting it skip is half of why the hem measured
    // *lighter* than the chest.
    float streak = dnoise3(mp * 11.00 + 61.0, dir * 0.30) * 0.58
                 + dnoise3(mp * 19.00 - 13.0, dir * 0.30) * 0.42;
    // Three octaves and a low threshold, both measured rather than guessed.
    // At two octaves and smoothstep(0.34, 0.80) this mask was zero over 70% of
    // the cloth and the tooth simply never appeared -- which is item 9's
    // complaint that pass 3's torso interior is a near-flat dark field. Sampled
    // over the mantle's material range the skip now has a median of 0.09 and a
    // 90th percentile of 0.74: mostly quiet, with real dry passages in it.
    float dryPatch = smoothstep(0.22, 0.60, vnoise(mp * 1.90 + 61.0) * 0.50
                                          + vnoise(mp * 4.40 - 22.0) * 0.30
                                          + vnoise(mp * 8.60 + 37.0) * 0.20);
    // Wet cloth does not skip, and this gate is sharp on purpose. Measured on
    // the p5 capture: at a floor of 0.15 and a 1.35 slope the hakama -- authored
    // at wetness 0.68 -- was still losing 0.11 of pool to the tooth, which is
    // ten luminance levels lifted off exactly the passage STYLE.md 3.4 wants to
    // be the darkest in the picture. Past wetness 0.45 the brush is loaded and
    // the tooth stops.
    float dryness = (0.05 + 0.95 * clamp(1.0 - wetness * 2.20, 0.0, 1.0))
                  * u_paperGrain * dryPatch;
    float tooth = smoothstep(0.47, 0.72, streak);   // centred on the measured mean, 0.574
    float skip = dryness * (1.0 - tooth);        // how much the brush skipped here

    // -- the fray (STYLE.md 3.1, shader-fixes-3 items 1, 3, 5) ----------------
    // The threshold ramps from "nothing survives" at the strip's own boundary to
    // "everything survives" frayPx inside it, and frayPx is the authored
    // dissolve's only job. It is read *point-wise*, never from a blur of the
    // neighbourhood: that is what lets the haori hem detonate while the skull,
    // the neck column, the hand and the tsuka keep their shape (item 5).
    //
    // Its floor is a fraction of the strip's own thickness rather than a
    // constant. A 128 px passage of haori can lose fifteen pixels to the fray
    // and still be a shape, and it has to -- a boundary that only wanders two
    // pixels is a smooth offset curve and prints the polygon's own contour. The
    // 30 px neck column gets five, the 12 px tsuka three.
    float frayPx = mix(0.22 * halfPx + 1.5, 34.0, pow(dissolve, 0.75));

    // The boundary wanders. Four octaves of the same material-space noise -- at
    // roughly 77, 33, 15 and 9 px, all clear of STYLE.md 3b.1's 2 px floor --
    // displace it in and out, which is what a loaded brush does at the end of a
    // stroke and what stops the fray being a monotone ramp inward from wherever
    // the polygon happened to end.
    //
    // The two fine octaves are load-bearing. With only the coarse pair the first
    // p5 capture measured a dead-straight 54 px vertical rail down the hakama's
    // front: a 35 px strip gets a 5 px fray band, so the threshold sweeps its
    // whole range faster than the mark field's steepest octave can climb, and
    // the boundary comes out as the polygon offset inward by five pixels --
    // straight wherever the polygon is straight.
    //
    // The amplitude is scaled by the strip's thickness and by the authored
    // dissolve, so it is a few pixels on a solid passage and tens at a hem.
    // Revision 2 did this from a quarter-resolution screen-space blur at
    // +-13..44 px gated by nothing local, which is the lobe that ate the forearm
    // (item 8). This term physically cannot: on the forearm it is worth three
    // pixels.
    float wob = (vnoise(mp * 2.60 + 5.0) - 0.5) * 1.30
              + (vnoise(mp * 6.10 - 17.0) - 0.5) * 1.05
              + (vnoise(mp * 13.30 + 41.0) - 0.5) * 0.95
              + (vnoise(mp * 22.00 - 63.0) - 0.5) * 0.70;
    float edgeW = edgePx - wob * mix(0.16 * halfPx + 2.2, 12.0, dissolve);

    float ramp = smoothstep(0.0, frayPx, edgeW);
    // The additive term keeps a hem breaking up well away from its own boundary
    // -- the bottom third of the garment is meant to be ink smoke, not a solid
    // shape with a frayed outline.
    float thr = mix(1.16, 0.16, ramp) + dissolve * 0.86;

    // Dense masses stay dense. Well inside a low-dissolve region the field is
    // floored, so the dry brush there reads as value rather than punching the
    // skull full of light speckle -- the artefact the pass-2 review found.
    float core = smoothstep(frayPx * 1.05, frayPx * 2.20 + 9.0, edgePx)
               * (1.0 - smoothstep(0.02, 0.45, dissolve));
    float cutField = mix(field, max(field, 0.82), core);

    // Contract F1: soft threshold, widening as the marks get sparser so the last
    // flecks are the most feathered of all. Measured in field units against the
    // field's own slope: 0.09 is about two pixels here.
    float band = 0.09 + 0.13 * dissolve;
    float cov = smoothstep(thr - band, thr + band, cutField);

    // -- detached flecks (STYLE.md 3.1, shader-fixes-3 item 3) ----------------
    // Marks that separate from the edge are cut *separately*, by keeping the
    // peaks of the two finest octaves inside a zone that straddles the fray
    // band. Peaks of the directional-smear noise are elongated along the stroke
    // and sit off the axis of the edge they came from, so they read as shards
    // with direction. Revision 2 cut them from a high-pass residual instead, and
    // the peaks of a residual are always round -- which is why pass 3's flecks
    // are dots.
    //
    // The zone starts a couple of pixels *inside* the rail rather than on it, so
    // the shards' outer boundary is a noise contour and never the polygon edge.
    // The gate is narrow on purpose. Value noise has broad plateaus separated by
    // fast transitions, so a *tight* threshold on it cuts polygonal shards with
    // straight-ish facets, and a wide one rounds them off into the soft dots
    // pass 3 produced. A coarse cluster gate on top keeps the marks in drifts
    // instead of spreading them evenly along every edge, which is STYLE.md 10's
    // "symmetric, uniform particle bursts" in slow motion.
    float shardN = dnoise2(mp * 15.50 - 5.1, dir * 0.30) * 0.46
                 + dnoise2(mp * 24.00 + 12.9, dir * 0.30) * 0.54;
    float shardClump = smoothstep(0.40, 0.64, vnoise(mp * 3.40 + 29.0) * 0.7
                                            + vnoise(mp * 8.10 - 47.0) * 0.3);
    float shardZone = smoothstep(0.5, 3.5, edgePx)
                    * (1.0 - smoothstep(frayPx * 0.35, frayPx * 1.25 + 3.0, edgePx))
                    * smoothstep(0.04, 0.28, dissolve)
                    * shardClump;
    cov = max(cov, smoothstep(0.625, 0.715, shardN) * shardZone * 0.95);

    // The tooth is allowed to open the coverage, not just lift the value:
    // STYLE.md 3.3 asks for paper showing through in streaks, and ink that only
    // ever gets paler reads as airbrush. Kept well under the fray so it can
    // never break the silhouette.
    cov = clamp(cov * (1.0 - skip * 0.30), 0.0, 1.0);

    // -- value (contract F4) --------------------------------------------------
    float poolNoise = vnoise3(vec3(mp * 1.60 + 41.0, tz * 0.6)) * 0.55
                    + vnoise(mp * 4.70 + 17.0) * 0.30
                    + vnoise(mp * 9.90 - 31.0) * 0.15;
    float blot = smoothstep(0.38, 0.80, clamp(0.5 + (wash - 0.5) * 2.05, 0.0, 1.0));
    float hang = smoothstep(0.15, 0.95, v_uv.y) * wetness;

    // shader-fixes-3 item 7: the authored wetness decides this, and nothing
    // else does. Revision 2 spent 1.12 of a range that clamps at 1.0 on `blot`
    // alone -- a position-noise term that saturates wherever the mesh is dense
    // -- so wetness could contribute at most half of an already-full value and
    // the chest, authored at 0.03, sat on the ink floor with the hem. The mesh
    // side is authored correctly (hem 1.0, chest 0.03); the weights below are
    // what let it print. blot and hang are now decoration on top of a value
    // that wetness has already set.
    // Measured after the first p5 capture: with blot at 0.26 the chest still
    // came out at luminance 43 against a hem at 64, because the chest was being
    // darkened by a term that has nothing to do with what the mesh authored.
    // wetness now owns two thirds of the range outright, and blot and hang are
    // decoration on top of a value it has already set. Authored 0.03 lands on
    // INK_INDIGO, the "dominant body/cloth tone" of STYLE.md 2.1; authored 1.0
    // lands on INK_BLACK.
    float dark = clamp(0.14
                     + wetness * (0.85 + 0.16 * poolNoise)
                     + blot * 0.16
                     + hang * 0.10, 0.0, 1.0);

    // 0.5 is the base tone; below it the wash is dilute, above it pigment pools.
    // Encoding it signed around the middle is what lets the dry brush lift ink
    // toward the paper rather than only punching holes in it -- and at 0.42 the
    // lift is finally large enough to *see*, which is item 9. Pass 3's torso
    // interior was a near-flat dark field with no streak structure at all.
    float pool = clamp(0.5 + 0.50 * dark - 0.72 * skip, 0.0, 1.0);

    // Watercolour strands pigment at the drying front, so every fleck carries a
    // darker rim. Partial coverage *is* the drying front here, which is a much
    // better handle on it than the iso-contours of the field -- those read as
    // topographic terraces, the artefact the pass-2 review found on the
    // shoulder.
    float rim = cov * (1.0 - smoothstep(0.30, 0.92, cov));
    pool = clamp(pool + rim * 0.22, 0.0, 1.0);

    // -- ochre underpainting (STYLE.md 2.1) -----------------------------------
    // In the references this is not a tint: it is a saturated rust bloom eating a
    // hole in the indigo, the second loudest thing in the picture after the
    // blade, and the one place the colour budget of STYLE.md 2.2 is spent.
    float stainN = vnoise(mp * 3.30 + 13.0) * 0.52
                 + vnoise(mp * 7.60 - 4.0) * 0.30
                 + vnoise(mp * 15.50 + 27.0) * 0.18;
    float blotchy = vnoise(mp * 1.75 + 71.0) * 0.60 + vnoise(mp * 3.60 - 12.0) * 0.40;
    // One tight gate on a *two-dimensional* blend of two fields, not on
    // stainMask itself: stainMask is a per-row vertex value and interpolates
    // linearly, so on its own it prints as horizontal stripes across the torso.
    // A blend rather than a product of two gates, because a product of
    // independent smoothsteps almost never reaches one and the bloom arrived as
    // a dilute tint over already-dark ink and measured as mud.
    float stain = clamp(stainMask * 2.10, 0.0, 1.0)
                * smoothstep(0.515, 0.595, 0.55 * blotchy + 0.45 * stainN);

    // -- how hard this passage wicks into the paper ---------------------------
    // The resolve's halo reads this, so a hem bleeds and a shoulder crest does
    // not. Step 1 ships the tight halo deliberately; the wide bleed is step 2.
    float bleed = 0.30 + 0.70 * dissolve;

    // Premultiplied. See the header: this is ordinary "over" compositing, which
    // averages overlapping ribbons instead of letting the topmost replace them.
    gl_FragColor = vec4(pool * cov, stain * cov, bleed * cov, cov);
}
