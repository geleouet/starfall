// The ground the figure is painted on: Family A warm paper, per STYLE.md 2 and 6.
//
// This is not a backdrop, it is half the picture. In the reference paintings the
// paper is doing as much work as the figure -- cool wash clouds, ochre bleeds,
// mist bands lying across the lower body, a ground that is a smear rather than a
// floor. A flat cream fill would leave the samurai floating in a void and would
// make every judgement about the ink material meaningless.

#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_worldPos;

uniform float u_time;
uniform vec2  u_frameMin;    // world-space rect of the visible frame
uniform vec2  u_frameSize;
uniform vec3  u_paperWarm;
uniform vec3  u_paperCool;
uniform vec3  u_ochre;
uniform vec3  u_inkIndigo;
uniform vec3  u_fogColor;
uniform vec3  u_moteCyan;
uniform vec3  u_moteMagenta;
uniform vec3  u_ember;
uniform vec3  u_fogBands[3];

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

float fbm(vec2 p) {
    float v = vnoise(p) * 0.55;
    v += vnoise(p * 2.1 + 7.3) * 0.27;
    v += vnoise(p * 4.3 - 3.1) * 0.12;
    v += vnoise(p * 9.7 + 11.0) * 0.06;
    return v;
}

void main() {
    vec2 w = v_worldPos;
    vec2 uv = (w - u_frameMin) / u_frameSize;
    float aspect = u_frameSize.x / u_frameSize.y;

    // Base sheet. Deliberately kept bright and warm nearly everywhere: in the
    // reference paintings most of the sheet is clean cream and the grey only
    // appears as distinct shapes. Grading the whole ground toward the cool tone
    // is what turned the first pass of this into a flat pewter fog.
    vec3 col = mix(u_paperWarm * 0.955, u_paperWarm, smoothstep(-0.10, 0.90, uv.y));

    // Cool wash clouds: read as separate shapes with clean paper between them,
    // so only the top of the noise range gets any pigment at all. Sampled
    // much wider than tall so the shapes drift out as horizontal streaks
    // rather than round blobs -- revision 2, this is what the fog bands
    // below are meant to look like and the two need to agree.
    vec2 drift = vec2(u_time * 0.012, u_time * 0.005);
    float cloud = fbm(vec2(w.x * 0.28, w.y * 0.95) + drift);
    col = mix(col, u_paperCool, smoothstep(0.56, 0.88, cloud) * 0.60);
    col = mix(col, mix(u_paperCool, u_inkIndigo, 0.45), smoothstep(0.70, 0.97, cloud) * 0.45);

    // Ochre bleeds low in the frame, off to the sides. Sparse and unsaturated
    // enough to stay inside the accent budget (STYLE.md 2.2).
    float ochreN = fbm(w * 1.15 + vec2(31.0, 7.0) - drift * 0.6);
    float ochreMask = smoothstep(0.62, 0.94, ochreN)
                    * smoothstep(0.75, 0.20, uv.y)
                    * smoothstep(0.14, 0.36, abs(uv.x - 0.5));
    col = mix(col, u_ochre, ochreMask * 0.42);

    // Ground: an ink smear with a ragged top edge and wet blooms, fading out
    // before it reaches either side of the frame. Not a floor (STYLE.md 6).
    float groundY = 0.075 + 0.045 * fbm(vec2(w.x * 1.6, 5.0));
    float ground = smoothstep(groundY + 0.11, groundY - 0.07, uv.y);
    ground *= smoothstep(0.01, 0.26, uv.x) * smoothstep(0.01, 0.26, 1.0 - uv.x);
    float bloom = smoothstep(0.42, 0.86, fbm(w * 2.6 + 19.0));
    col = mix(col, mix(u_inkIndigo, u_paperCool, 0.30), ground * (0.42 + 0.45 * bloom));

    // Contact pool: a rounder, darker wash directly under the figure's feet,
    // distinct from the wide side-to-side ground smear above -- the rig
    // stands at world x=0, so this is centred there rather than on frame
    // centre, and it stays correct if the camera ever re-centres. Revision 2
    // grounding fix: without this the figure floats on blank paper with no
    // contact shadow at all (STYLE.md 6).
    vec2 poolCenter = vec2(0.0, 0.10);
    vec2 poolDelta = w - poolCenter;
    float poolNoise = fbm(w * 2.2 + 41.0);
    float poolDist = length(poolDelta * vec2(0.95, 1.9)) / (0.62 + 0.10 * poolNoise);
    float pool = smoothstep(1.0, 0.0, poolDist);
    col = mix(col, mix(u_inkIndigo, u_paperCool, 0.22), pool * 0.50);
    // A second, smaller and wetter bloom slightly forward of the pool centre,
    // like pigment that kept spreading after the wash was laid down.
    float bloom2 = fbm(w * 3.4 + vec2(7.0, -11.0));
    float pool2Dist = length((w - vec2(0.16, 0.06)) * vec2(1.3, 2.3)) / 0.32;
    col = mix(col, u_inkIndigo, smoothstep(1.0, 0.0, pool2Dist) * smoothstep(0.5, 0.85, bloom2) * 0.35);

    // Grass strokes: thin, sparse, only just above the smear. Gated by a second
    // low-frequency mask so they come in clumps -- an even comb of strokes right
    // across the frame reads as a ruler, not as a field. Clumps are biased to
    // cluster near the contact pool so the grounding reads as a place, not a
    // strip of lawn running the width of the frame.
    float bladeMask = smoothstep(groundY + 0.13, groundY + 0.02, uv.y)
                    * smoothstep(groundY - 0.04, groundY + 0.02, uv.y);
    float clump = smoothstep(0.40, 0.78, vnoise(vec2(w.x * 1.3, 11.0)))
                * (0.55 + 0.45 * smoothstep(1.0, 0.0, abs(w.x) / 1.1));
    float grass = smoothstep(0.72, 0.95, vnoise(vec2(w.x * 38.0, w.y * 2.0)));
    col = mix(col, mix(u_inkIndigo, u_paperCool, 0.30), bladeMask * grass * clump * 0.34
                    * smoothstep(0.05, 0.28, uv.x) * smoothstep(0.05, 0.28, 1.0 - uv.x));

    // Fog bands. Same construction as ink_skin.frag so the mist the figure fades
    // into is the same mist that is drawn on the paper -- if these two drifted
    // apart the figure would fade into nothing at all.
    float fog = 0.0;
    for (int i = 0; i < 3; i++) {
        vec3 b = u_fogBands[i];
        float fi = float(i);
        float d = u_time * (0.055 + 0.031 * fi);
        float wobble = b.y * (0.30 * sin(w.x * (0.85 + 0.45 * fi) + d + fi * 2.1)
                            + 0.19 * sin(w.x * 2.43 - d * 0.7 + fi)
                            + 0.11 * sin(w.x * 5.11 + d * 1.6 - fi * 1.7));
        float dist = abs(w.y - (b.x + wobble)) / b.y;
        fog += b.z * (1.0 - smoothstep(0.0, 1.0, dist));
    }
    fog = clamp(fog, 0.0, 1.0);
    col = mix(col, mix(u_fogColor, u_paperWarm, 0.35), fog * 0.62);

    // Jewel motes: out-of-focus, slow, and few (STYLE.md 6). Additive but tiny --
    // the moment these read as a particle system they have failed.
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        vec2 seed = vec2(fi * 7.13 + 1.0, fi * 3.71 + 2.0);
        vec2 c = vec2(hash2(seed), hash2(seed + 11.0));
        c.x += u_time * (0.003 + 0.004 * hash2(seed + 3.0));
        c.y += u_time * 0.0022;
        c = fract(c);
        float r = 0.022 + 0.030 * hash2(seed + 5.0);
        // Wide and very soft: a bokeh mote has no edge at all. Anything with a
        // disc boundary reads as dust on the lens rather than as a light.
        float g = pow(1.0 - smoothstep(0.0, r, length((uv - c) * vec2(aspect, 1.0))), 1.8);
        float k = hash2(seed + 17.0);
        vec3 mc = k < 0.34 ? u_moteCyan : (k < 0.67 ? u_moteMagenta : u_ember);
        // Tinted rather than brightened: additive motes on a cream ground wash out
        // to white.
        col = mix(col, mix(col, mc, 0.22), g);
    }

    // Vignette by wash, never by black (STYLE.md 6).
    float vig = smoothstep(0.55, 1.15, length((uv - 0.5) * vec2(1.05, 1.25)) * 1.6);
    col = mix(col, u_paperCool * 0.94, vig * 0.26);

    // Paper tooth. Barely there, but it is the difference between a sheet of
    // paper and a gradient.
    float tooth = vnoise(w * 240.0) * 0.6 + vnoise(w * 90.0 + 5.0) * 0.4;
    col *= 0.975 + 0.05 * tooth;

    gl_FragColor = vec4(clamp(col, vec3(0.06), vec3(0.97)), 1.0);
}
