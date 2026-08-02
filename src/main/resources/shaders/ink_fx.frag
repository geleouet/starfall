// Fragment stage for System 4's ink marks.
//
// Premultiplied out, always, and the *blend equation* is what decides whether a
// mark absorbs or emits:
//
//   * absorbing marks -- the bloom, the shed flecks, the vermillion seal -- are
//     composited with (ONE, ONE_MINUS_SRC_ALPHA), which is ordinary "over". Ink
//     spreading into wet paper darkens what is under it; that is the whole of
//     STYLE.md 7.3's first reaction and it must never lighten.
//
//   * the clash is composited with (ONE, ONE_MINUS_SRC_COLOR) -- the screen
//     operator InkSkinnedRenderer already uses for the blade's arc-trail, and for
//     the reason recorded there: a pale warm wash at low alpha over warm paper
//     composites *darker* than the ground under plain alpha, so a bloom of light
//     drawn that way reads as a dirty smudge. Screen can only lighten and
//     approaches the paper's own brightness asymptotically rather than clipping,
//     which is what keeps this out of STYLE.md 10's "neon glow on everything".
//
// Both paths want premultiplied source, so there is one shader and two blend
// states rather than two shaders.

#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;

void main() {
    gl_FragColor = vec4(v_color.rgb * v_color.a, v_color.a);
}
