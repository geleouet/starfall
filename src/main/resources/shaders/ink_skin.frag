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

// A constant offset added to the material-space sampling point, per figure.
//
// Everything below is sampled at v_matPos so the pattern stays nailed to the
// cloth (STYLE.md 3.5) -- which is right, and which means two figures built
// from the same rig are painted with bit-identical ink. That is invisible with
// one figure on screen and is the loudest possible tell with two: the same
// torn hem, the same dry-brush streaks, the same flecks, mirrored. Offsetting
// the sample point is the smallest change that fixes it, because a constant
// offset moves every octave together and is differentiated away by every
// fwidth() below, so the fray band, the octave fades and the strip metric are
// all bit-identical to what they were. It is emphatically not a screen-space
// term: it lives in material space with the noise it shifts, so nothing swims
// (STYLE.md 10's "screen-space noise that swims over moving surfaces").
uniform vec2 u_inkSeed;

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

// STYLE.md 3b.1's anti-shimmer guarantee, as a function rather than as a
// comment. An octave whose material-space frequency is `freq` has a period of
// mpPx/freq pixels; this fades it out over the range where that period falls
// from 6 px to 2 px and holds it at zero below. Every octave added below the
// pass-5 frequency set is gated by this, which is what makes it safe to reach
// down into the brush-hair band at all: at capture framing the 40 and 64
// octaves are 5.6 and 3.5 px and fully present, and if the camera ever pulls
// back far enough to put them on the pixel grid they switch themselves off
// rather than producing the shimmer that failed two reviews.
float octaveFade(float mpPx, float freq) {
    return smoothstep(2.0, 6.0, mpPx / freq);
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
    vec2 mp = v_matPos + u_inkSeed;

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
    // Six octaves spanning about 20:1, because a single scale of break-up reads
    // as torn wet cardboard rather than as a brush running out of ink: the big
    // lobes have to have small marks flaking off ahead of them.
    //
    // The first four frequencies are pass 3's, carried forward verbatim -- they
    // are the real fix for the periodic torso banding that failed two reviews,
    // and the architecture never was. At this camera (225 px per world unit)
    // their periods are 73, 31, 15 and 9.4 px.
    //
    // The last two are debt item D4 and are a number, not a design. STYLE.md
    // 3b.1's hard floor is 2 px and the pass-5 set stopped at 9.4, leaving an
    // octave and a half of unused headroom in exactly the band where "ink fleck"
    // lives -- which is why the review found every detached fleck to be a smooth
    // 8x6 ellipse, STYLE.md 3's "flecks that are all the same size". 40 and 64
    // land at 5.6 and 3.5 px. Both stay above the floor at this framing and both
    // are gated by octaveFade so they cannot approach it at any other.
    // Amplitudes still sum to 1.0, so the contrast expansion below is unchanged
    // and the coarse structure of the silhouette is untouched: these perturb the
    // contour by a pixel or two, they do not reshape it.
    float mpPx = 1.0 / max(length(fwidth(mp)), 1e-6);
    float f40 = octaveFade(mpPx, 40.0);
    float f64 = octaveFade(mpPx, 64.0);

    vec2 creep = vec2(u_time * 0.0035, u_time * -0.0022);
    float marks = dnoise2(mp * 3.10 + creep * 11.0, dir * 0.30) * 0.20
                + dnoise2(mp * 7.30 + 3.7 - creep * 7.0, dir * 0.30) * 0.24
                + dnoise2(mp * 15.50 - 5.1, dir * 0.30) * 0.24
                + dnoise2(mp * 24.00 + 12.9, dir * 0.30) * 0.20
                + dnoise2(mp * 40.00 - 21.3, dir * 0.30) * 0.08 * f40
                + dnoise2(mp * 64.00 + 8.4, dir * 0.30) * 0.04 * f64;
    marks += 0.5 * (0.08 * (1.0 - f40) + 0.04 * (1.0 - f64));   // keep the mean at 0.5

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

    // -- the same tooth, opening the sheet instead of lifting the value -------
    // System 3 pass 4, STYLE.md 3.3. The dry brush has two separable jobs and
    // this shader had been running both off one gate:
    //
    //   "the paper-tooth texture must show through in streaks"   -- coverage
    //   "ink is darkest where it collects"                       -- value
    //
    // `dryness` above is right for the value half and is untouched: a loaded hem
    // must not print paler than the chest, which is the ink gravity three passes
    // fought for, and its `1.0 - wetness * 2.20` switches the lift off past
    // wetness 0.45. But the haori's lower rows are authored 0.80-1.00 wet, so
    // every interior term in this file is silent there. Measured on the pass-3
    // graded window, a 25x60 px box inside the back skirt has a luminance
    // standard deviation of 3.3 on a mean of 32.6 -- a flat fill, which is the
    // first row of STYLE.md 3b.5's table, and the reason `hips` measured 100.0%
    // covered and `torso` 98.2%.
    //
    // Opening the sheet is not the same act as lifting the value and bleaches
    // nothing: the ink that remains is exactly as dark as it was, there is
    // simply paper between the marks. That is what a loaded brush does on rough
    // paper, and it is the distinction this file already draws for the cream
    // reserves -- "washed out is a *coverage* fault and cannot be fixed, or
    // caused, by value".
    //
    // The wet gate multiplies *after* the gain and its clamp, not before. That
    // ordering is the difference between "wet cloth skips less" and "wet cloth
    // skips a fifth as much": inside the clamp a large gain saturates both, and
    // measured at gain 3.5 with the gate inside, the back skirt fell from 85%
    // covered to 66% while the dry torso only reached 94% -- backwards, since
    // the references put the heaviest unbroken black on the skirt and the dry
    // brush on the shoulder.
    float openWet = 0.08 + (1.0 - 0.08)
                  * clamp(1.0 - wetness * 1.60, 0.0, 1.0);
    float openness = clamp(u_paperGrain * dryPatch * (0.35 + (1.0 - 0.35) * (1.0 - tooth)) * 3.0, 0.0, 1.0) * openWet;

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
    //
    // D4 adds a fifth and sixth, at 5.0 and 3.2 px against the pass-5 set's
    // 77/33/15/9. A boundary whose finest wobble is nine pixels can only carry
    // nine-pixel scallops, and that is the second half of why the flecks all
    // came out the same size -- they are cut out of the same contour.
    float wob = (vnoise(mp * 2.60 + 5.0) - 0.5) * 1.30
              + (vnoise(mp * 6.10 - 17.0) - 0.5) * 1.05
              + (vnoise(mp * 13.30 + 41.0) - 0.5) * 0.95
              + (vnoise(mp * 22.00 - 63.0) - 0.5) * 0.70
              + (vnoise(mp * 45.00 + 9.0) - 0.5) * 0.46 * octaveFade(mpPx, 45.0)
              + (vnoise(mp * 70.00 - 28.0) - 0.5) * 0.30 * octaveFade(mpPx, 70.0);
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
    // Pass 4 widens the dissolve half of this. The pass-3 review measured an
    // edge scanline across the back skirt going 152, 148, 150 -> 32 with *zero*
    // intermediate samples, against 145 -> 50 -> 34 in pass 2: the mesh work
    // moved the boundary and hardened it, and STYLE.md 10 fails a visible
    // polygon silhouette on sight. A wider band on the frayed rows is the direct
    // answer -- the last flecks are the most feathered of all, which is what
    // contract F1 already says and what the number was too small to deliver.
    // Zero at dissolve 0, so every solid passage in the figure is bit-identical.
    float band = 0.09 + 0.34 * dissolve;
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
    //
    // D4 again. The two fine octaves are folded into the shard field itself and
    // normalised by their own weight sum, so the threshold below keeps meaning
    // the same thing whatever the framing. A shard cut from a field that has
    // structure at 3.5, 5.6, 9.4 and 15 px comes out with structure at all four,
    // which is the size *distribution* the matched-scale reference shows and the
    // pass-5 capture does not have.
    float shardW = 0.34 + 0.36 + 0.20 * f40 + 0.10 * f64;
    float shardN = (dnoise2(mp * 15.50 - 5.1, dir * 0.30) * 0.34
                  + dnoise2(mp * 24.00 + 12.9, dir * 0.30) * 0.36
                  + dnoise2(mp * 40.00 - 21.3, dir * 0.30) * 0.20 * f40
                  + dnoise2(mp * 64.00 + 8.4, dir * 0.30) * 0.10 * f64) / max(shardW, 1e-3);
    float shardClump = smoothstep(0.40, 0.64, vnoise(mp * 3.40 + 29.0) * 0.7
                                            + vnoise(mp * 8.10 - 47.0) * 0.3);
    float shardZone = smoothstep(0.5, 3.5, edgePx)
                    * (1.0 - smoothstep(frayPx * 0.35, frayPx * 1.25 + 3.0, edgePx))
                    * smoothstep(0.04, 0.28, dissolve)
                    * shardClump;
    cov = max(cov, smoothstep(0.625, 0.715, shardN) * shardZone * 0.95);

    // -- splatter (STYLE.md 3's "flecks that are all the same size", debt D4) --
    // A second, much finer cut, kept separate from the shards on purpose. The
    // shards are torn *pieces of the edge*: they are cut from the same octaves
    // that shape the contour, so they inherit its scale. Splatter is not part of
    // the edge at all -- it is what leaves the brush and lands beyond it, and at
    // this framing the matched-scale reference puts it between 1 and 5 px.
    //
    // So this reads only the two finest octaves, at a threshold tight enough
    // that only their peaks survive (about the top 8% of the field), which turns
    // a 3.5 px noise cell into a 1-2 px speck. The zone reaches roughly twice as
    // far out as the shard zone and its own coarse drift gate is sparser, so the
    // specks arrive in spatters with clean paper between them rather than as an
    // even sprinkle -- STYLE.md 10's "symmetric, uniform particle bursts".
    //
    // Neither octave can reach the 2 px floor: octaveFade zeroes both first, and
    // when it does, speckW collapses and the term switches off cleanly instead
    // of renormalising a field that is no longer there.
    float speckW = 0.62 * f40 + 0.38 * f64;
    float speckN = (dnoise2(mp * 40.00 + 77.0, dir * 0.30) * 0.62 * f40
                  + dnoise2(mp * 64.00 - 33.0, dir * 0.30) * 0.38 * f64) / max(speckW, 1e-3);
    // Pass 4 widened this term's two gates and its reach, to answer the pass-3
    // review's ranked-1 item ("let the fleck octave reach the boundary"). Pass 5
    // pulls the reach back and feathers what is left, because the pass-4 review
    // measured what the widening actually printed:
    //
    //   "At 7x it goes paper to flat mid-grey in ONE pixel, no halo, no interior
    //    variation, narrow size distribution. That is 3.1's soft-band
    //    requirement, 3.2's halo, and two 10 fail-on-sight rows -- hard-edged
    //    sprites and cel-shaded flat fills -- in the one material this pass
    //    touched."
    //
    // Verified before changing anything, on out/captures/rev-sway-live frame 0:
    // the one genuinely detached splatter mark in the frame is a 2 px diagonal
    // streak at x446..455 y491..501 running 204 -> 78 with a single intermediate
    // sample and its surrounding paper untouched at 205-210. It is a hard-edged
    // sprite, and it sits *below the drawn figure*, which is the second half of
    // the damage: ink thrown that far past the silhouette widened the detected
    // figure box and therefore moved every figure-space region in the project.
    //
    // Three changes, and the first two are simply pass 3's numbers back:
    //
    //   * The outer reach returns to frayPx * 2.6 + 7.0. 3.1's "breaks into
    //     separate brush marks" happens just beyond the fray band, not sixty
    //     pixels beyond the hem in open paper.
    //   * The coarse drift gate returns to the top 20% of its field, so the
    //     specks arrive in occasional spatters instead of along every frayed
    //     edge.
    //   * The cut becomes a soft-shouldered PROFILE instead of a threshold, and
    //     the reason is worth writing down because it is a real constraint on
    //     this framing rather than a taste. A speck here is 1-2 px, cut from the
    //     40 and 64 octaves, whose period is 3.5 and 2.2 px -- so the field
    //     crosses any threshold in well under a pixel and NO constant band in
    //     field units can feather it. Pass 3's band was 0.075 and pass 4's 0.100
    //     and both print the same aliased chip. The only thing that gives a mark
    //     this small a soft edge is for its coverage to be a smooth function of
    //     the field: a wide smoothstep, cubed, so the mark has a low toe, a
    //     gentle shoulder and a peak that only the field's own maxima reach.
    //     That also answers "no interior variation" and "narrow size
    //     distribution" in one term, because a marginal peak now lands as a pale
    //     fleck and a strong one as a dark one, and 3's failure signature is
    //     "flecks that are all the same size" -- which for a mark two pixels
    //     across is really a statement about value.
    //
    // The peak is 0.80 rather than 0.88 for the same reason: splatter is what
    // leaves the brush and lands on damp paper, so it arrives dilute. A droplet
    // that prints at full ink density with a step edge is a decal.
    float speckDrift = smoothstep(0.50, 0.80, vnoise(mp * 2.20 - 91.0) * 0.62
                                            + vnoise(mp * 5.60 + 13.0) * 0.38);
    float speckZone = smoothstep(0.2, 1.6, edgePx)
                    * (1.0 - smoothstep(frayPx * 0.5, frayPx * 2.6 + 7.0, edgePx))
                    * smoothstep(0.03, 0.24, dissolve)
                    * speckDrift
                    * step(0.02, speckW);
    float speckCore = smoothstep(0.560, 0.960, speckN);
    cov = max(cov, speckCore * speckCore * speckCore * speckZone * 0.80);

    // The tooth is allowed to open the coverage, not just lift the value:
    // STYLE.md 3.3 asks for paper showing through in streaks, and ink that only
    // ever gets paler reads as airbrush.
    //
    // Pass 4 raises the authority, and it had to be measured to be believed:
    // with the old `skip` term run at 1.00 instead of 0.30 and its blotch gate
    // opened wide, `torso` coverage moved 98.2% -> 97.8% and `hips` did not move
    // off 100.0% at all. The cut was never the binding constraint. See the bleed
    // channel near the bottom of this file for what was.
    //
    // Still kept off the silhouette. `edgeGuard` is zero within a couple of
    // pixels of any strip boundary and one well inside, so the tooth punches the
    // sheet and can never eat the outline -- an outline eaten by an interior
    // term is revision 2's forearm-eating lobe arriving by a different route.
    float edgeGuard = smoothstep(2.0, 9.0, edgePx);
    cov = clamp(cov * (1.0 - openness * edgeGuard), 0.0, 1.0);

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
    // Debt D3's own correction, which is the part of it that is still true.
    // "Make the shoulder look like the hem" is wrong advice about the *edge* --
    // measured, the hem's edge is the harder of the two -- but it is exactly
    // right about the interior, and the interior is still flat. Measured on the
    // p6 bind capture over the shoulder mantle, luminance standard deviation is
    // 11.2 with 5.8 of that below the 9 px scale, against 33-50 and 20-29 for
    // every band from the chest down.
    //
    // The cause is in the line above. Every term that varies value is either
    // multiplied by `wetness` (poolNoise) or is a pooling term, and pooling only
    // ever darkens -- so cloth authored at the mantle's 0.03-0.20 has almost
    // nothing to pool and receives almost no variation at all. It then lands
    // just above 0.5 in the resolve's value mapping, which is the compressed
    // half of that curve: 0.1 of pool is worth 14 luminance levels below 0.5 and
    // 7 above it. Flat input into the shallow half of the ramp.
    //
    // So dry cloth gets its own zero-mean term, largest exactly where the
    // pooling terms are smallest and gone by the time the cloth is wet. It is
    // built from the two fields that already exist -- no new frequency enters
    // the image, which matters because STYLE.md 3's postscript records that the
    // last frequency artefact misdiagnosed as structural cost two passes.
    // Zero-mean is the point: the debt is explicit that the upper figure reading
    // washed out is a *coverage* fault and cannot be fixed, or caused, by value.
    // This must move the variance and leave the mean alone.
    //
    // The field has to be contrast-expanded first, and the first version of
    // this term did not do that and was worth almost nothing: it moved the
    // mantle's standard deviation from 11.2 to 11.4. This shader already knows
    // why -- "an fbm sum piles up around 0.5" -- and the raw mix is only about
    // +-0.12 wide, so a 0.40 gain buys +-0.05 of `dark`, which lands inside the
    // compressed half of the resolve's value curve and disappears. Expanded to
    // roughly the same width `field` uses, and at a gain that carries the value
    // across 0.5 into the *dilute* half where the curve is twice as steep, it
    // is worth about seventeen luminance levels.
    float dryField = clamp(0.5 + (mix(wash, marks, 0.35) - 0.5) * 2.60, 0.0, 1.0);
    float dryVar = (1.0 - smoothstep(0.10, 0.52, wetness))
                 * (dryField - 0.5) * 0.60;

    // Pass 4. `wetness * (0.85 + 0.16 * poolNoise)` plus the 0.14 base plus any
    // blot at all is >= 1.0 for every wetness above about 0.85, so the haori's
    // lower rows -- authored 0.84 to 1.00 -- were *saturated*, and poolNoise,
    // blot and hang were all being clipped away. That is the arithmetic behind
    // the flat fill measured above: standard deviation 3.3 inside the back
    // skirt.
    //
    // The fix keeps the ceiling and lowers the mean, which is STYLE.md 3.4's own
    // amendment read the other way round -- "the pooling rail sits *on* the
    // floor and everything else lifts off it". The deepest pool is still exactly
    // as deep as it was (poolNoise saturates the term above ~0.72, and a pool
    // that reaches the ceiling still reaches it), so nothing can breach the
    // value floor that was not already breaching it; what changes is that the
    // cloth *between* the pools now lifts. No new frequency enters the picture:
    // poolNoise's octaves are 141, 48 and 23 px at capture framing, all far
    // clear of STYLE.md 3b.1's floor, which matters because 3's postscript
    // records that the last frequency artefact misdiagnosed as structural cost
    // two passes.
    float dark = clamp(0.14
                     + wetness * (0.85 + 0.16 * poolNoise)
                     + blot * 0.16
                     + hang * 0.10
                     + dryVar, 0.0, 1.0);

    // -- cream reserves (STYLE.md 3b.0, debt D3) ------------------------------
    // Pooling only ever darkens, and above the waist there is almost nothing to
    // pool: the mantle is authored at wetness 0.03-0.20 and came out of pass 5
    // as a near-flat mid-indigo field with a hard polygon edge, against a hem
    // the review called "genuinely wet, dilute, cloudy, with real internal value
    // variation" and named as the target for the whole figure. The hem gets that
    // read because it is wet; the shoulder cannot get it the same way without
    // inverting the ink gravity that three passes fought for.
    //
    // Shibori supplies the other half. Where the binding kept the dye out the
    // cloth stays near the paper, so the variation runs *upward* from the base
    // tone as well as down. That is dilute and cloudy without being any lighter
    // on average in the places that matter, and it is why family E's indigo
    // reads as cloth rather than as fill.
    //
    // Deliberately sparse and strong rather than broad and weak -- a wide gentle
    // version is just a lighter garment, which is the "washed out" fault the
    // debt document explicitly warns is *not* fixed by value. Gated off as the
    // cloth gets wet, so the hem, the hakama and the grip keep their density.
    //
    // Amplitude measured, not guessed. At 0.34 the first p6 capture lifted the
    // mid-torso from 55 to 73 mean ink luminance, which is the "washed out"
    // fault the debt document is explicit cannot be fixed *or caused* by value
    // -- a reserve that shows up as a general lift is just a paler garment. At
    // 0.20 the band means are back within a few levels of pass 5's while the
    // interior still carries visible cloud structure.
    //
    // Built from the wash *and* the mark field, and thresholded over a wide
    // range rather than a narrow one. Cut tightly out of the wash alone it
    // selected that field's broad plateaus and printed as large flat pale
    // facets with straight borders -- which is not a reserve, it is debt item
    // D6's flat-facet mosaic with a lighter tone. Mixing in `marks`, which now
    // carries structure down to 3.5 px, and ramping instead of stepping gives a
    // gradient with cloud edges.
    float reserveField = clamp(0.5 + (wash - 0.5) * 1.70, 0.0, 1.0) * 0.70
                       + clamp(0.5 + (marks - 0.5) * 1.30, 0.0, 1.0) * 0.30;
    float reserve = (1.0 - smoothstep(0.16, 0.54, reserveField))
                  * (1.0 - smoothstep(0.18, 0.55, wetness));

    // 0.5 is the base tone; below it the wash is dilute, above it pigment pools.
    // Encoding it signed around the middle is what lets the dry brush lift ink
    // toward the paper rather than only punching holes in it -- and at 0.42 the
    // lift is finally large enough to *see*, which is item 9. Pass 3's torso
    // interior was a near-flat dark field with no streak structure at all.
    float pool = clamp(0.5 + 0.50 * dark - 0.72 * skip - 0.26 * reserve, 0.0, 1.0);

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
    //
    // The gate has two regimes, and the split is debt D1's. Its coarsest octave
    // is a 128 px blob, so over a 26 px object it is a coin flip: either the
    // whole thing blooms or none of it does. That is exactly right for a dye
    // bloom on a garment and useless for a *fitting* -- the leather-and-brass
    // kote, the linen obi, the lacquered saya -- which has to be reliably warm
    // because being the one non-ink value in the cluster is its entire job.
    //
    // So a stainMask authored above ~0.5 declares "this is a fitting, not a
    // bloom": the gate opens almost everywhere, keeping only a soft blotchy
    // modulation, and in exchange the *amount* is compressed hard -- to 0.36,
    // which is below the knee of the resolve's pale-ochre push, so a fitting
    // lands on a dark leather-brown instead of the bright rust of a bloom. The
    // first p6 capture ran this at 0.58 and the obi and both scabbards printed
    // as orange sticks, well past STYLE.md 2.2's accent budget. Garment stains are all
    // authored at or below 0.29 and are untouched by this.
    float fitting = smoothstep(0.42, 0.62, stainMask);
    float gate = 0.55 * blotchy + 0.45 * stainN;
    float stain = clamp(stainMask * 2.10, 0.0, 1.0)
                * (1.0 - 0.64 * fitting)
                * smoothstep(mix(0.515, 0.300, fitting), mix(0.595, 0.520, fitting), gate);

    // -- how hard this passage wicks into the paper ---------------------------
    // The resolve's halo reads this. Debt D3: the floor was 0.30, which with the
    // resolve's old halo mapping made the wet bleed worth about one luminance
    // level anywhere the garment is authored solid -- i.e. absent above the
    // waist entirely, which is STYLE.md 3.2 simply not implemented there. A hem
    // still bleeds roughly twice as hard as a shoulder crest, which is the
    // distinction this channel exists to carry; it is the *floor* that was
    // wrong, not the slope.
    //
    // Pass 4, and this is the change that makes STYLE.md 3.3 reachable at all.
    //
    // Measured on the pass-3 graded window: the brightest pixel anywhere in the
    // interior of `torso`, `hips` or the back skirt is luminance 150, against an
    // ink threshold of 0.85 x paper = 185. There is a floor under the entire
    // figure some eight levels darker than the threshold, and it is this halo --
    // deep inside a silhouette the resolve's cM and cF both saturate and
    // haloTight + haloWide lands near 0.32 alpha whatever the coverage buffer
    // says. A hole punched in `cov` is repainted by the bleed of the cloth
    // around it, so no setting of the cut above could ever show paper. That is
    // why "make the ink skip" kept failing to move the number.
    //
    // The halo has to skip with the ink. `bleedAvg` in the resolve is a ~9 px
    // coverage-weighted blur and the streak octaves here are 20 and 12 px across
    // the stroke, so a dry passage is wide enough for that blur to follow: the
    // halo thins over a streak rather than averaging it away.
    //
    // Gated on being well inside the strip, and that gate is the whole safety
    // argument. STYLE.md 3.2 exists to make the figure sit *in* the paper rather
    // than on it, and debt D3 records this floor being raised from 0.30 to 0.52
    // precisely because the halo was absent above the waist. The outer band of
    // every strip keeps its bleed untouched, so the halo that reaches out past
    // the silhouette is fed entirely by fragments this term does not reach. What
    // thins is the halo under the *middle* of the cloth, which is the only place
    // it was doing damage.
    // Driven by the coarse blotch mask and NOT by `openness`, and that is the
    // whole trick. `bleedAvg` in the resolve is mid.b / cM -- a *coverage
    // weighted* average -- so a fragment whose coverage the tooth has just taken
    // to zero contributes almost nothing to it, and thinning that fragment's
    // bleed changes the halo by almost nothing. Measured: driven by `openness`,
    // `torso` coverage moved 98.2% -> 96.6% at eight times the gain. The halo
    // has to thin over the whole dry *passage* -- 26 to 118 px across, which the
    // 9 and 20 px blurs can follow -- while the streaks inside it cut the marks.
    // That is also what a dry passage physically is: less water on the paper
    // over an area, with the tooth showing inside it.
    float bleedInside = smoothstep(7.0, 24.0, edgePx);
    float bleed = (0.52 + 0.48 * dissolve)
                * (1.0 - 1.0 * dryPatch * openWet * u_paperGrain * bleedInside);

    // Premultiplied. See the header: this is ordinary "over" compositing, which
    // averages overlapping ribbons instead of letting the topmost replace them.
    gl_FragColor = vec4(pool * cov, stain * cov, bleed * cov, cov);
}
