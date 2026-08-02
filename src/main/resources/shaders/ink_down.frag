// Full-resolution material buffer -> quarter resolution, the first link in the
// chain that gives the resolve its halo.
//
// Four bilinear taps at the centres of the four 2x2 sub-blocks are an exact 4x4
// box filter over the quarter-texel's footprint, which is why there are only
// four of them.
//
// Revision 3 step 1: a plain average, with no binarisation and no reweighting.
// The source buffer is already premultiplied by coverage -- ink_skin.frag writes
// (pool, stain, bleed) * cov and cov -- so averaging it is *already* a
// coverage-weighted average, and the blur that follows preserves that. The
// previous version had to binarise its own coverage because the alpha channel
// held a near-constant weight (1 - 0.28*dissolve) rather than a coverage mask;
// that is no longer true and reconstructing coverage from it would now throw
// away the fray this chain is meant to blur.
//
//     r  pool  x coverage
//     g  stain x coverage
//     b  bleed x coverage
//     a  coverage
//
// The chain no longer feeds the silhouette in any way (shader-fixes-3 item 1).
// Its one remaining consumer is the halo.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_src;
uniform vec2 u_srcTexel;   // 1 / full-resolution size

void main() {
    vec2 o = u_srcTexel;
    vec4 a = texture2D(u_src, v_uv + vec2(-o.x, -o.y));
    vec4 b = texture2D(u_src, v_uv + vec2( o.x, -o.y));
    vec4 c = texture2D(u_src, v_uv + vec2(-o.x,  o.y));
    vec4 d = texture2D(u_src, v_uv + vec2( o.x,  o.y));

    gl_FragColor = (a + b + c + d) * 0.25;
}
