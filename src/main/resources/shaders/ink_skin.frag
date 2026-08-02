// Pass 1 of the ink material: skinned garment geometry -> a merged material
// buffer. This shader writes *material data*, never final colour.
//
// Why it is split at all (system1-shader-fixes item 1): when every ribbon ran
// its own dissolve, its own bleed and its own alpha blend and then composited
// over its neighbours, N overlapping narrow strips produced a periodic ripple
// through the chest no amount of noise tuning could remove. Here the ribbons
// only deposit material parameters; the dissolve threshold, the fray, the bleed
// and the dry-brush read all happen once, in ink_resolve.frag, over the merged
// field.
//
// Everything below is evaluated at v_matPos -- the position the vertex was
// authored at in bind space -- so the pattern stays nailed to the cloth as it
// deforms (STYLE.md 3.5). That property is the single thing this refactor was
// most at risk of losing, and it is why the noise stays on this side of the
// split rather than moving into the screen-space resolve.
//
// Channel layout, and the blend state it assumes (set in InkSkinnedRenderer):
//
//     r  field   multi-octave ink field, thresholded downstream
//     g  pool    value: 0.5 is base tone, 1.0 pools to deep, 0.0 lifts to dilute
//     b  stain   ochre underpainting strength
//     a  weight  1 - 0.28*dissolve. MAX-blended, so overlapping ribbons take the
//                densest rather than accumulating opacity.
//
// RGB blends SRC_ALPHA / ONE_MINUS_SRC_ALPHA against a buffer cleared to the
// neutral (0.5, 0.5, 0.0): a thin, mostly-dissolved strip therefore tints what
// is under it instead of punching its own fray through it.

#ifdef GL_ES
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
    // The frequencies matter more than the weights. Revision 1 ran its finest
    // octave at 44 cycles/unit, which at this camera is a 4.7 pixel period, and
    // stretched it 2.4:1 along the stroke -- a regular corrugation exactly at the
    // 3-5 pixel period the review measured through the chest. Anisotropy is kept
    // under 1.6:1 and the finest octave sits at ~6 pixels with a small weight, so
    // it flecks the fray band without corrugating the interior (where the field
    // sits far above the threshold and this term cannot open a hole).
    vec2 creep = vec2(u_time * 0.0035, u_time * -0.0022);
    float marks = dnoise2(mp * 3.10 + creep * 11.0, dir * 0.31) * 0.22
                + dnoise2(mp * 7.30 + 3.7 - creep * 7.0, dir * 0.44) * 0.26
                + dnoise2(mp * 15.50 - 5.1, dir * 0.54) * 0.26
                + dnoise2(mp * 24.00 + 12.9, dir * 0.30) * 0.26;

    // Contrast expansion: an fbm sum piles up around 0.5, and a threshold that
    // only ever sees 0.35..0.65 cannot shed sparse flecks at high dissolve.
    // Weighted toward the fine end and expanded hard, because what decides
    // whether an edge breaks into flecks or just fades is whether the field's
    // gradient beats the dissolve threshold's. The threshold sweeps its range
    // over twenty-odd pixels; a mark octave with a four-pixel half-period and
    // only a tenth of range loses that race everywhere, and the silhouette comes
    // out as a clean blurred curve.
    float field = clamp(0.5 + (mix(wash, marks, 0.70) - 0.5) * 2.90, 0.0, 1.0);

    // -- dry brush (contract F3) ----------------------------------------------
    // Amplitude is back up to clearly visible. It was cut twice during pass 1
    // because it read as venetian blinds; that was the item-1 corrugation
    // compounding it, and the frequencies here are ~9 and ~19 pixels across the
    // stroke rather than 2.5, which is brush-hair scale instead of grating scale.
    // Gated by a two-octave blotch mask so the tooth only shows in passages, the
    // way it does in the references.
    float streak = dnoise3(mp * 11.00 + 61.0, dir * 0.99) * 0.58
                 + dnoise3(mp * 19.00 - 13.0, dir * 0.95) * 0.42;
    float dryPatch = smoothstep(0.34, 0.80, vnoise(mp * 1.90 + 61.0) * 0.65
                                          + vnoise(mp * 4.40 - 22.0) * 0.35);
    // Mostly shared, only partly driven by the authored wetness. The merge keeps
    // the topmost strip's field rather than blending it, so any term that varies
    // strongly from one region to the next prints the region's outline: a limb
    // sliver drawn over the hakama is drier by its authored wetness alone, and
    // that difference alone was drawing the leg as a straight-edged pale band.
    float dryness = (0.40 + 0.60 * clamp(1.0 - wetness * 1.40, 0.0, 1.0))
                  * u_paperGrain * dryPatch;
    float tooth = smoothstep(0.26, 0.80, streak);
    float skip = dryness * (1.0 - tooth);        // how much the brush skipped here

    field = clamp(field - skip * 0.26, 0.0, 1.0);

    // -- value (contract F4) --------------------------------------------------
    // Three octaves on the pool noise rather than two. The authored wetness is a
    // per-row vertex value and interpolates linearly, which lays a Mach band
    // across the garment at every mesh row -- the horizontal terracing the
    // pass-2 review found contouring the shoulder. Breaking it up at three
    // scales is what keeps the mesh rows out of the picture.
    float poolNoise = vnoise3(vec3(mp * 1.60 + 41.0, tz * 0.6)) * 0.55
                    + vnoise(mp * 4.70 + 17.0) * 0.30
                    + vnoise(mp * 9.90 - 31.0) * 0.15;
    // Blot threshold deliberately wide. The old 0.46..0.72 window turned the
    // smooth wash into value plateaus with visible boundaries -- iso-contours of
    // a noise field read as topographic terraces, which is precisely what the
    // review saw on the shoulder.
    float blot = smoothstep(0.38, 0.80, clamp(0.5 + (wash - 0.5) * 2.05, 0.0, 1.0));
    float hang = smoothstep(0.15, 0.95, v_uv.y) * wetness;
    // The constant is not decoration. Overlapping regions no longer alpha-blend
    // -- the merge keeps the topmost strip's material -- so a dry strip laid over
    // a wet one replaces its value outright, and the difference prints as a
    // straight-edged pale band the width of the strip. Anchoring most of the
    // darkness in terms every region shares (the wash blots, which depend only on
    // material position) keeps neighbouring garments within a few values of each
    // other and the seams stop shouting.
    float dark = clamp(0.16 + wetness * (0.14 + 0.40 * poolNoise) + blot * 1.12 + hang * 0.24, 0.0, 1.0);

    // 0.5 is the base tone; below it the wash is dilute, above it pigment pools.
    // Encoding it signed around the middle is what lets the dry brush lift ink
    // toward the paper rather than only punching holes in it.
    float pool = clamp(0.5 + 0.50 * dark - 0.30 * skip, 0.0, 1.0);

    // -- ochre underpainting (STYLE.md 2.1) -----------------------------------
    // In the references this is not a tint: it is a saturated rust bloom eating a
    // hole in the indigo, the second loudest thing in the picture after the
    // blade, and the one place the colour budget of STYLE.md 2.2 is spent.
    //
    // stainMask is authored per vertex and interpolates linearly along the strip,
    // so on its own it prints as horizontal stripes across the torso (pass-2
    // review). Multiplying it by a garment-scale blotch field turns those rows
    // into blooms.
    float stainN = vnoise(mp * 3.30 + 13.0) * 0.52
                 + vnoise(mp * 7.60 - 4.0) * 0.30
                 + vnoise(mp * 15.50 + 27.0) * 0.18;
    float blotchy = vnoise(mp * 1.75 + 71.0) * 0.60 + vnoise(mp * 3.60 - 12.0) * 0.40;
    // Two independent gates rather than one blended field: their intersection is
    // a handful of isolated blooms, where a blend of the two still lets the
    // row-interpolated mask through as horizontal stripes across the torso.
    // One tight gate on a blend of the two fields, not the product of two gates:
    // a product of independent smoothsteps almost never reaches one, so the bloom
    // arrived as a dilute tint over already-dark ink and measured as mud. The
    // blend still has to be two-dimensional, though -- stainMask alone is a
    // per-row vertex value and prints as horizontal stripes across the torso.
    float stain = clamp(stainMask * 2.10, 0.0, 1.0)
                * smoothstep(0.49, 0.63, 0.55 * blotchy + 0.45 * stainN);

    // -- weight ---------------------------------------------------------------
    // MAX-blended into the alpha channel: the densest contributor at a pixel
    // wins and nothing accumulates. It doubles as the resolve's reading of the
    // authored dissolve, which is what makes hems fray wider than shoulders.
    float weight = 1.0 - 0.28 * dissolve;

    gl_FragColor = vec4(field, pool, stain, weight);
}
