// Fragment stage for the hair ribbons of STYLE.md 4.
//
// Three jobs, in order of how much they matter:
//
//  1. Feather the ribbon across its width. STYLE.md 3 opens with "nothing in
//     this game has a hard edge except the blades", and a 4 px ribbon with two
//     hard rails is two hard edges. The falloff runs to zero at the rails, so
//     the mark has no boundary of its own anywhere.
//
//  2. Break the ink up along the strand, and break the last third of it into
//     separate flecks so the tip reads as a brush leaving the paper rather than
//     as a line being cut off.
//
//  3. Fade into the same mist everything else does.
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
// Two octaves, and their periods are declared rather than tuned by eye. At the
// capture framing the camera covers 3.0 world units over 540 px, so one world
// unit is 180 px. The coarse octave samples arc * 5.5, i.e. one noise cell per
// 0.182 units = 33 px, and value noise's characteristic wavelength is about two
// cells, so it runs at ~65 px. The fine octave is 11.0, i.e. 33 px. Nothing here
// is anywhere near the 2 px floor, and deliberately so: a hair ribbon is 2-5 px
// across, so an along-strand octave finer than about 8 px would be shimmer on an
// object with no room to hide it.
//
// The time term shifts the coarse octave by 0.09 cells per second, so a full
// pattern turnover takes about eleven seconds -- STYLE.md 3.6's "several
// seconds", pigment settling rather than a frozen texture.

#ifdef GL_ES
precision highp float;   // the fract()-based hashes below smear badly at mediump
#endif

varying vec3 v_mat;       // x seed, y arc length, z s along the strand
varying vec4 v_color;
varying float v_across;
varying float v_subpixel;
varying vec2 v_world;
varying float v_halfWidth;

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

    // Across the ribbon as well as along it, and measured in world units rather
    // than in rails. A noise argument that ignores the width is constant across
    // the whole cross-section, so the fleck threshold cuts a *band* right across
    // the ribbon: on a 2 px wisp that is invisible and on the 17 px scalp mass
    // it printed a stack of hard-edged rectangular blocks. Scaling by the real
    // half-width instead of by the rail keeps the world-space period the same on
    // every strand, which is what STYLE.md 3b.1 requires -- on the widest strand
    // the across octaves run at about 11 px and 7 px, and on a hairline wisp the
    // term is a fraction of a cell and contributes nothing at all.
    float acrossW = v_across * v_halfWidth;
    float n = 0.62 * vnoise(vec2(seed + acrossW * 18.0, arc * 5.5 + u_time * 0.09))
            + 0.38 * vnoise(vec2(seed * 1.7 + 11.3 + acrossW * 30.0, arc * 11.0));

    // Across the ribbon. Zero at both rails, so the mark never draws its own
    // boundary anywhere. A power curve rather than a smoothstep plateau: a
    // 4 px ribbon with a flat middle and a short shoulder still reads as a band
    // with two edges, and STYLE.md 3's first line is that nothing in this game
    // has a hard edge except the blades. This has no flat part at all.
    float e = clamp(1.0 - v_across * v_across, 0.0, 1.0);
    float edge = pow(e, 0.55);

    // Coverage along the strand. Ink skips everywhere -- the first version only
    // thresholded the last 45% and the first half of every strand came out as a
    // flat band, which at 7x was unmistakably a polygon rather than a brush
    // mark. So the threshold starts at 0.20 and climbs to 0.78, and by the tip
    // the strand is a run of separate flecks rather than a line being cut off.
    float tip = smoothstep(0.42, 1.0, s);
    float thr = 0.15 + 0.62 * tip;
    float cut = smoothstep(thr - 0.19, thr + 0.19, n);

    // The root does not end; it emerges. Without this the first row of the
    // ribbon is a flat cap sitting on open paper -- a straight edge, drawn
    // perpendicular to the strand, on the one object in the figure whose whole
    // job is to have no boundary.
    float root = smoothstep(0.0, 0.055, s);

    float a = v_color.a * v_subpixel * edge * cut * root * (0.74 + 0.30 * n);

    // Value varies with the same field, so a strand is not a flat fill even
    // where it is only two pixels wide -- STYLE.md 3b.5's first anti-pattern.
    vec3 ink = v_color.rgb * (0.88 + 0.20 * n);

    float fog = fogAt(v_world) * u_fogStrength;
    ink = mix(ink, u_fogColor, 0.55 * fog);
    a *= 1.0 - 0.45 * fog;

    gl_FragColor = vec4(ink, a);
}
