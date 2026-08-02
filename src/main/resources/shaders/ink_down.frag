// Full-resolution material buffer -> quarter-resolution coverage, the first link
// in the chain that gives the resolve a distance-to-silhouette.
//
// Four bilinear taps at the centres of the four 2x2 sub-blocks are an exact 4x4
// box filter over the quarter-texel's footprint, which is why there are only
// four of them.
//
// Coverage is binarised *before* the average. The material buffer's alpha is a
// weight (1 - 0.28*dissolve), not a coverage mask, so blurring it directly would
// read a solid hem as if it were half outside the figure and the whole fray
// would collapse onto the hems. What the resolve needs from this chain is purely
// "is there garment here", and how far away the nearest edge of it is.
//
//     r  coverage
//     g  pool   x coverage -- carries the wet-bleed halo's value
//     b  weight x coverage -- the authored dissolve, for the fray band's width
//     a  field  x coverage -- a garment-scale version of the ink field, which
//                             the resolve uses to make the silhouette wander
//
// The last two are premultiplied by coverage so the resolve can divide by r and
// get a genuine coverage-weighted average instead of a value that fades out
// toward the silhouette for the wrong reason.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_src;
uniform vec2 u_srcTexel;   // 1 / full-resolution size

float cvg(float w) {
    return smoothstep(0.05, 0.35, w);
}

void main() {
    vec2 o = u_srcTexel;
    vec4 a = texture2D(u_src, v_uv + vec2(-o.x, -o.y));
    vec4 b = texture2D(u_src, v_uv + vec2( o.x, -o.y));
    vec4 c = texture2D(u_src, v_uv + vec2(-o.x,  o.y));
    vec4 d = texture2D(u_src, v_uv + vec2( o.x,  o.y));

    float ca = cvg(a.a);
    float cb = cvg(b.a);
    float cc = cvg(c.a);
    float cd = cvg(d.a);

    float cov = (ca + cb + cc + cd) * 0.25;
    float pool = (a.g * ca + b.g * cb + c.g * cc + d.g * cd) * 0.25;
    float weight = (a.a * ca + b.a * cb + c.a * cc + d.a * cd) * 0.25;
    float fieldC = (a.r * ca + b.r * cb + c.r * cc + d.r * cd) * 0.25;

    gl_FragColor = vec4(cov, pool, weight, fieldC);
}
