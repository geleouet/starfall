// Fragment stage for the hair of STYLE.md 4.
//
// Three marks, in one shader, selected by v_mode. They are three marks rather
// than one parameterised mark because the pass-1 review's whole finding was that
// hair is *bimodal* -- "a 30-55 px near-opaque mass, plus 1-2 px hairlines
// peeling off it, with nothing in between" -- and a single path with a width
// uniform is exactly how a pass ends up authoring the register in between, which
// reads as cord.
//
//   MODE_BLEED (2)    STYLE.md 3.2's wet bleed. Wide, very soft, no threshold of
//                     any kind. Drawn under the mass so the mass sits *in* the
//                     paper rather than on it.
//   MODE_MASS (0)     The plume. A real plateau across its middle at INK_BLACK,
//                     a shoulder, and a rail eroded by the material noise so the
//                     silhouette breaks into flecks before it vanishes
//                     (STYLE.md 3.1). Dry-brush streaks run *along* the stroke
//                     (3.3) and the trailing rail pools darker (3.4).
//   MODE_HAIRLINE (1) One hair. At roughly a pixel across there is no room for
//                     an interior, so all it has is a feathered cross-section, a
//                     fleck threshold along its length, and a root that emerges
//                     rather than starts.
//
// -- the value floor, which is an invariant and not a preference ------------
//
// STYLE.md 2.2: "No pure black, no pure white, ever. Darkest is #161A22."
// Hair is authored *at* that floor -- the mass holds INK_BLACK across its whole
// first half so the topknot and the plume read as one object at one value -- so
// every value term in this shader can only ever lighten. There is no headroom
// underneath. A term written as `0.86 + 0.14 * x`, centred below 1.0, is not a
// contrast control here; it is a request for a colour the palette does not
// contain, and it printed 20.9 against a floor of 25.73 for a whole pass.
//
// The rule, then: **every multiplier applied to `ink` in this file has a
// minimum of exactly 1.0.** Pooling, dry brush and grain are all expressed as
// how far a pixel lifts *off* the floor, never as how far it sinks below it.
// Coverage is a separate question and `a` is free to do as it likes.
//
// -- material-space anchoring (STYLE.md 3.5) --------------------------------
//
// The noise argument is (strand seed, arc length along that strand). Both are
// intrinsic to the strand: the seed is a constant and the arc length is measured
// along the spine, so the pattern is nailed to the hair by construction and
// cannot swim however the strand moves. There is no world position and no screen
// position anywhere in the noise path. This is the strongest form of the rule --
// the material coordinate is not merely stable under deformation, it is not a
// spatial coordinate at all.
//
// -- the frequency budget (STYLE.md 3b.1) -----------------------------------
//
// At capture framing the camera covers 4.91 world units over 960 px, so one
// world unit is 196 px, and value noise's characteristic wavelength is about two
// cells. Every octave below, in world-space period and then in pixels:
//
//   along, coarse    arc * 5.5    0.182 u   36 px cell    71 px wavelength
//   along, fine      arc * 11.0   0.091 u   18 px cell    36 px wavelength
//   along, dissolve  arc * 22.0   0.045 u    9 px cell    18 px wavelength
//   along, streak    arc * 2.2    0.455 u   89 px cell   178 px wavelength
//   across, mass     w   * 33.0   0.030 u    6 px cell    12 px wavelength
//   across, streak   w   * 60.0   0.017 u    3 px cell     7 px wavelength
//
// The finest is 7 px, well clear of 3b.1's 2 px floor, and deliberately so: a
// mass is 30-50 px across and a hairline is one, so an across-octave finer than
// this would be shimmer on an object with no room to hide it. The across terms
// are scaled by the *world* half-width rather than by the rail, so their period
// is the same on every strand -- scaling by the rail made the fleck threshold cut
// a band right across the ribbon, invisible on a wisp and a stack of hard-edged
// rectangular blocks on the widest mass.
//
// The time term shifts the coarse octave by 0.09 cells per second, so a full
// pattern turnover takes about eleven seconds -- STYLE.md 3.6's "several
// seconds", pigment settling rather than a frozen texture.

#ifdef GL_ES
precision highp float;   // the fract()-based hashes below smear badly at mediump
#endif

varying vec3 v_mat;       // x seed, y arc length, z s along the strand
varying float v_mode;
varying vec4 v_color;
varying float v_across;
varying float v_subpixel;
varying vec2 v_world;
varying float v_halfWidth;
varying float v_halfPx;

uniform float u_time;
uniform vec3 u_fogColor;
uniform vec3 u_fogBands[3];
uniform float u_fogStrength;

// Same value-noise vocabulary as ink_skin.frag, deliberately: hair and cloth are
// the same pigment on the same paper and a second noise basis would print as a
// second material.
float hash2(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x),
               mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}

// Kept identical to Atmosphere#fogAt and to the loop in ink_skin.frag: a
// divergence here does not fail loudly, it just puts the hair in a different
// atmosphere from the head it grows out of.
float fogAt(vec2 world) {
    float fog = 0.0;
    for (int i = 0; i < 3; i++) {
        float centre = u_fogBands[i].x;
        float halfH = u_fogBands[i].y;
        float strength = u_fogBands[i].z;
        float fi = float(i);
        float drift = u_time * (0.055 + 0.031 * fi);
        float wobble = halfH * (0.30 * sin(world.x * (0.85 + 0.45 * fi) + drift + fi * 2.1)
                             + 0.19 * sin(world.x * 2.43 - drift * 0.7 + fi)
                             + 0.11 * sin(world.x * 5.11 + drift * 1.6 - fi * 1.7));
        float d = clamp(abs(world.y - (centre + wobble)) / halfH, 0.0, 1.0);
        fog += strength * (1.0 - d * d * (3.0 - 2.0 * d));
    }
    return clamp(fog, 0.0, 1.0);
}

void main() {
    float seed = v_mat.x;
    float arc = v_mat.y;
    float s = v_mat.z;
    float acrossW = v_across * v_halfWidth;
    float rail = 1.0 - abs(v_across);   // 1 at the spine, 0 at the rail

    vec3 ink = v_color.rgb;
    float a;

    if (v_mode > 1.5) {
        // -- STYLE.md 3.2, the wet bleed -----------------------------------
        //
        // "Outside the dissolve band, a low-alpha halo of the garment colour
        // extends several pixels further, blurred, like pigment wicking into
        // damp paper. This halo must be *softer and larger* than the dissolve
        // band, so the figure reads as sitting *in* the paper rather than on
        // it." No threshold, no fleck, no rail: a power falloff and a slow
        // bloom, which is the only shape a wicking front has.
        //
        // The review measured pass 1's hair going paper-to-core in one pixel
        // with no halo at all, beside a garment transitioning over 1-76 px
        // behind a 10-131 px halo, and called the hair "the only thing on the
        // figure with a hard edge". At 2.6x the mass's half-width this halo runs
        // 25-30 px past the plume's own shoulder.
        float bloom = 0.55 + 0.90 * vnoise(vec2(seed * 1.9 + 4.1, arc * 3.1 + u_time * 0.05));
        a = v_color.a * pow(rail, 1.9) * bloom;
    } else if (v_mode > 0.5) {
        // -- one hair -------------------------------------------------------
        float n = 0.62 * vnoise(vec2(seed + acrossW * 18.0, arc * 5.5 + u_time * 0.09))
                + 0.38 * vnoise(vec2(seed * 1.7 + 11.3, arc * 11.0));

        // Across the hair. Zero at both rails, so the mark never draws its own
        // boundary anywhere. A power curve rather than a smoothstep plateau: at
        // roughly a pixel across, a flat middle with a short shoulder still
        // reads as a band with two edges.
        float e = clamp(1.0 - v_across * v_across, 0.0, 1.0);
        float edge = pow(e, 0.55);

        // Coverage along the hair. Ink skips everywhere, and by the tip the hair
        // is a run of separate flecks rather than a line being cut off.
        float tip = smoothstep(0.45, 1.0, s);
        float thr = 0.14 + 0.60 * tip;
        float cut = smoothstep(thr - 0.20, thr + 0.20, n);

        // The root does not end; it emerges. Without this the first row of the
        // ribbon is a flat cap drawn perpendicular to the hair, on the one
        // object in the figure whose whole job is to have no boundary.
        float root = smoothstep(0.0, 0.05, s);

        a = v_color.a * v_subpixel * edge * cut * root * (0.78 + 0.28 * n);
        // Lightening only, for the reason spelled out in the mass branch below.
        // A hairline leaves its lock at INK_BLACK as well, so the old 0.88x was
        // the same floor violation, waiting on a hairline opaque enough at its
        // root to print it. Contrast preserved: 1:1.227 was 0.88:1.08.
        ink *= 1.0 + 0.227 * n;
    } else {
        // -- the mass -------------------------------------------------------
        float n = 0.62 * vnoise(vec2(seed + acrossW * 33.0, arc * 5.5 + u_time * 0.09))
                + 0.38 * vnoise(vec2(seed * 1.7 + 11.3 + acrossW * 33.0, arc * 11.0));

        // STYLE.md 3.1: the silhouette breaks into discrete brush flecks before
        // it vanishes, over a soft band. The rail is *eroded* by the noise
        // rather than thresholded at it, so the boundary wanders in and out by
        // several pixels and takes the shoulder with it; the erosion grows
        // toward the tip, so a plume ends by coming apart rather than by
        // stopping.
        float dis = vnoise(vec2(seed * 2.3 + 7.7 + acrossW * 33.0, arc * 22.0 + u_time * 0.06));
        float erode = (0.10 + 0.40 * s) * dis * dis;

        // A plateau, which is what "near-opaque mass" means and what pass 1 had
        // nowhere: its widest ribbon was a dome, so even 17 px of authored width
        // was never solid anywhere but on its centre line. The shoulder is
        // stated in pixels and converted, so it is 3 px on the mass and cannot
        // swallow a narrower mark whole.
        float sh = clamp(3.0 / max(v_halfPx, 0.5), 0.12, 0.80);
        float body = smoothstep(0.0, sh, rail - erode);

        // Coverage along the plume: solid through the first half, breaking up
        // over the last third. STYLE.md 3.1's smoothstep band is ~0.12 wide, so
        // flecks have feathered edges rather than aliased ones.
        float tip = smoothstep(0.52, 1.0, s);
        float thr = 0.08 + 0.66 * tip;
        float cut = smoothstep(thr - 0.13, thr + 0.13, n);

        // STYLE.md 3.3: dry-brush breakup *aligned with the stroke*. The along
        // term is very coarse and the across term is fine, so the field is a set
        // of streaks running down the plume rather than a blotch field. Kept
        // shallow on purpose -- it must read as ink skipping, not as holes.
        float streak = vnoise(vec2(arc * 2.2 + seed, acrossW * 60.0));

        // STYLE.md 3.4: value pooling. Ink collects at the trailing edge of a
        // wash, so the trailing rail is the darkest part of the plume.
        //
        // The previous form of this line got that wrong twice, and both errors
        // were the same error. It read
        //
        //     pool = 0.86 + 0.14 * smoothstep(-0.2, -1.0, v_across);
        //     ink *= pool * (0.92 + 0.14 * streak) * (0.94 + 0.12 * n);
        //
        // which puts pool at 1.0 on the v_across = -1 rail and 0.86 on the
        // spine and the whole far half of the ribbon. So:
        //
        //   1. Most of the plume printed at 0.86x INK_BLACK or below -- the
        //      three factors bottom out at 0.744x together. INK_BLACK *is* the
        //      floor: STYLE.md 2.2, "no pure black, no pure white, ever.
        //      Darkest is #161A22". Multiplying it by anything under 1.0 does
        //      not mean "more ink", because there is no more ink; it means a
        //      colour the palette does not contain. Measured on s3-p2-reversal,
        //      19590 pixels over 24 frames below the floor, darkest #12151C at
        //      luminance 20.87 against the floor's 25.73.
        //   2. The rail that 3.4 says pools *darkest* was the one part of the
        //      plume that was not darkened, so the pooling ran backwards: the
        //      trailing rail was the lightest thing in the mass.
        //
        // Both are fixed by the same move. The pooling rail sits *on*
        // INK_BLACK and everything else lifts off it, which is the only way to
        // express "ink collects here" when the base value is already the
        // darkest value there is. Rail-to-spine contrast is 1:1.163, which is
        // exactly what 0.86:1.00 carried.
        float pool = 1.0 + 0.163 * smoothstep(-1.0, -0.2, v_across);

        // 3.3's dry brush and the material grain are rebased the same way and
        // for the same reason: ink skipping and paper tooth both leave *less*
        // pigment than a full lay-down, so they can only lighten. Contrast is
        // preserved term for term -- 1:1.152 was 0.92:1.06, 1:1.128 was
        // 0.94:1.06.
        float dry = 1.0 + 0.152 * streak;
        float grain = 1.0 + 0.128 * n;

        a = v_color.a * v_subpixel * body * cut * (0.86 + 0.18 * streak);
        ink *= pool * dry * grain;
    }

    float fog = fogAt(v_world) * u_fogStrength;
    ink = mix(ink, u_fogColor, 0.55 * fog);
    a *= 1.0 - 0.45 * fog;

    gl_FragColor = vec4(ink, clamp(a, 0.0, 1.0));
}
