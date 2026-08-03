// The blade -- STYLE.md 5. The one hard-edged, high-contrast element in the
// frame, and the point of visual focus in every duel reference.
//
// Drawn forward, straight onto the paper, and deliberately not routed through
// the merged coverage buffer: the whole point of that buffer is to destroy hard
// edges, and this is the one object that must keep one.
//
// The outer glow is *not* in this shader. The authored blade is a sliver -- six
// pixels at the guard tapering to a point -- so there is no room inside the
// polygon to put a halo in. It is drawn as a separate sheath by
// InkSkinnedRenderer, together with the swept trail.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;
varying vec2 v_worldPos;

uniform float u_time;
uniform vec3  u_base;
uniform vec3  u_fogColor;
uniform float u_haze;
uniform vec3  u_fogBands[3];

void main() {
    float across = min(v_uv.x, 1.0 - v_uv.x) * 2.0;
    float along = min(v_uv.y, 1.0 - v_uv.y) * 2.0;

    // fwidth widens the edge band exactly where the quad narrows past a pixel,
    // which is the last fifth of the blade. Without it the rasteriser drops
    // pixels intermittently there and the tip prints as a row of separate dots
    // of decreasing size -- a straight dotted trail, which STYLE.md 5 fails on
    // sight.
    float aw = fwidth(across);
    float core = smoothstep(0.0, max(0.20, aw * 1.6), across)
               * smoothstep(0.0, 0.14, along);
    float edge = smoothstep(0.0, 0.62, across);

    // Steel, blue-leaning. #EAF2F8 pushed toward a cool white rather than a warm
    // one: the blade is the only genuinely cool thing in a warm frame and it has
    // to stay that way after the fog veil below has had its go at it.
    vec3 steel = mix(u_base, vec3(0.94, 0.975, 1.0), 0.45);

    // -- the hamon (STYLE.md 3b.3, debt D2) -----------------------------------
    // The temper line between the hardened edge and the softer spine, present in
    // every grid of family E and called out by 3b.3 as the one high-frequency
    // detail that must stay faintly readable even at planning framing, because
    // the blade is the object the eye follows. It is therefore deliberately
    // exempt from the distance fade the rest of the detail budget obeys.
    //
    // u runs across the blade: 0 is the mune (the spine, which is the convex
    // side of the sori the rig authors) and 1 is the ha. The line sits about a
    // third of the width up from the edge and wanders -- a suguha base with a
    // notare undulation over it, three incommensurate terms so it never repeats.
    // The fastest of them is 61 radians over the blade's length, which at
    // capture framing is a 15 px period against 3b.1's 2 px floor.
    //
    // Below the line the steel is frostier and a shade paler; above it, softer
    // and marginally darker. The whole excursion is small on purpose: STYLE.md 5
    // wants a sliver of near-white, not a two-tone stripe.
    float hamonLine = 0.60
                    + 0.070 * sin(v_uv.y * 37.0)
                    + 0.040 * sin(v_uv.y * 61.0 + 1.9)
                    + 0.032 * sin(v_uv.y * 17.0 - 0.7);
    float hamon = smoothstep(hamonLine - 0.12, hamonLine + 0.08, v_uv.x);
    // The step across the line has to be worth about twenty luminance levels to
    // be readable at all on a seven-pixel blade. Revision 1 of this term moved
    // between two tones four levels apart, which is not a temper line, it is a
    // rounding error. The ji (the softer body above the hamon) is therefore a
    // real grey-blue steel and only the ha below the line is near-white -- which
    // is also what a photographed katana does, and keeps STYLE.md 5's
    // "near-white sliver" true of the edge, where it matters.
    vec3 hardened = mix(steel, vec3(0.985, 0.992, 1.0), 0.30);
    vec3 ji = mix(steel, vec3(0.80, 0.84, 0.90), 0.60);
    vec3 ink = mix(u_base, mix(ji, hardened, hamon), core);

    float alpha = max(core, edge * 0.30);
    // Thins toward the point instead of stopping, so the taper reads as a brush
    // lifting rather than as geometry running out. Gentler than revision 1's
    // 0.45 floor over the last 16%: with a real kissaki authored in the mesh
    // there is now geometry converging there, and dimming it as hard as well
    // dissolved the point instead of resolving it.
    alpha *= 0.66 + 0.34 * (1.0 - smoothstep(0.82, 1.0, v_uv.y));

    // Debt D2: the blade held full brightness where it crossed the fog band that
    // had erased the figure's own legs, so it was not sitting in the same
    // atmosphere as the rest of the picture -- the exact "shader effect over a
    // sprite" tell the whole style is built to avoid.
    //
    // Revision 1 was over-corrected on the right instinct. Running the cloth's
    // *tint* weight over steel does make the one cool accent in a warm frame
    // measure R-heavy, so the tint stays modest; the attenuation belongs in
    // alpha, where a warm ground shows through a dimmed blade instead of being
    // painted onto it. At the tip, which is where the mist is, that costs about
    // a third of the blade's opacity -- comparable to the 0.20 the cloth pays,
    // and enough that the kissaki now fades into the same band the legs do.
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
    ink = mix(ink, u_fogColor, fog * 0.16);
    alpha *= 1.0 - fog * 0.34;

    // The same distance haze as the cloth, at half of it: the blade is the object
    // the eye follows (STYLE.md 5) and it is the last thing a wide shot should give
    // up, but a blade that holds full luminance in a figure that is dissolving reads
    // as a bright scratch hanging in mist.
    if (u_haze > 0.0) {
        ink = mix(ink, u_fogColor, u_haze * 0.14);
        alpha *= 1.0 - u_haze * 0.12;
    }

    // STYLE.md 2.2: only the clash bloom and the blade specular may approach
    // white, and neither ever reaches it.
    gl_FragColor = vec4(clamp(ink, vec3(0.0), vec3(0.985)), clamp(alpha, 0.0, 1.0));
}
