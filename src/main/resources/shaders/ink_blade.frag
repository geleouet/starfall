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
    vec3 ink = mix(u_base, steel, core);

    float alpha = max(core, edge * 0.30);
    // Thins toward the point instead of stopping, so the taper reads as a brush
    // lifting rather than as geometry running out.
    alpha *= 0.45 + 0.55 * (1.0 - smoothstep(0.84, 1.0, v_uv.y));

    // Mist barely touches steel. Revision 1 ran the figure's full fog weight over
    // it, and since the fog colour is warm the blade measured R-heavy and
    // B-light -- the one cool accent in the frame was not actually cool.
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
    ink = mix(ink, u_fogColor, fog * 0.10);
    alpha *= 1.0 - fog * 0.12;

    // STYLE.md 2.2: only the clash bloom and the blade specular may approach
    // white, and neither ever reaches it.
    gl_FragColor = vec4(clamp(ink, vec3(0.0), vec3(0.985)), clamp(alpha, 0.0, 1.0));
}
