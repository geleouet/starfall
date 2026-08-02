// Quarter-resolution gaussian, run twice with different tap spacings to give the
// resolve two coverage fields at two very different radii.
//
// Two fields, not one, because they answer two different questions. The narrower
// one resolves where the silhouette actually is, to within a few pixels, which
// is what the fray band needs. The wider one is the wet bleed itself -- a halo
// that has to reach three to five times further out than the fray, at low alpha,
// with a falloff soft enough that it reads as pigment wicking into damp paper
// rather than as a blurred copy of the figure.
//
// Both run at quarter resolution deliberately. The mobile-safe budget allows one
// extra full-resolution target and blurs at reduced resolution; a chain of
// full-resolution blurs is exactly what it forbids. Two quarter targets are
// ping-ponged, so the whole chain costs 1/8 of a full-resolution target.
//
// 5x5 separable-weight taps with the tap spacing equal to sigma: cheap, and past
// the first level the input is already smooth enough that the sparse sampling
// does not show.

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_src;
uniform vec2 u_srcTexel;   // 1 / quarter-resolution size
uniform float u_step;      // tap spacing in source texels; also the sigma

void main() {
    // Gaussian with sigma = one tap spacing, normalised.
    const float W0 = 0.38774;
    const float W1 = 0.24477;
    const float W2 = 0.06136;

    vec2 s = u_srcTexel * u_step;

    vec4 sum = vec4(0.0);
    sum += texture2D(u_src, v_uv + vec2(-2.0, -2.0) * s) * (W2 * W2);
    sum += texture2D(u_src, v_uv + vec2(-1.0, -2.0) * s) * (W1 * W2);
    sum += texture2D(u_src, v_uv + vec2( 0.0, -2.0) * s) * (W0 * W2);
    sum += texture2D(u_src, v_uv + vec2( 1.0, -2.0) * s) * (W1 * W2);
    sum += texture2D(u_src, v_uv + vec2( 2.0, -2.0) * s) * (W2 * W2);

    sum += texture2D(u_src, v_uv + vec2(-2.0, -1.0) * s) * (W2 * W1);
    sum += texture2D(u_src, v_uv + vec2(-1.0, -1.0) * s) * (W1 * W1);
    sum += texture2D(u_src, v_uv + vec2( 0.0, -1.0) * s) * (W0 * W1);
    sum += texture2D(u_src, v_uv + vec2( 1.0, -1.0) * s) * (W1 * W1);
    sum += texture2D(u_src, v_uv + vec2( 2.0, -1.0) * s) * (W2 * W1);

    sum += texture2D(u_src, v_uv + vec2(-2.0,  0.0) * s) * (W2 * W0);
    sum += texture2D(u_src, v_uv + vec2(-1.0,  0.0) * s) * (W1 * W0);
    sum += texture2D(u_src, v_uv                        ) * (W0 * W0);
    sum += texture2D(u_src, v_uv + vec2( 1.0,  0.0) * s) * (W1 * W0);
    sum += texture2D(u_src, v_uv + vec2( 2.0,  0.0) * s) * (W2 * W0);

    sum += texture2D(u_src, v_uv + vec2(-2.0,  1.0) * s) * (W2 * W1);
    sum += texture2D(u_src, v_uv + vec2(-1.0,  1.0) * s) * (W1 * W1);
    sum += texture2D(u_src, v_uv + vec2( 0.0,  1.0) * s) * (W0 * W1);
    sum += texture2D(u_src, v_uv + vec2( 1.0,  1.0) * s) * (W1 * W1);
    sum += texture2D(u_src, v_uv + vec2( 2.0,  1.0) * s) * (W2 * W1);

    sum += texture2D(u_src, v_uv + vec2(-2.0,  2.0) * s) * (W2 * W2);
    sum += texture2D(u_src, v_uv + vec2(-1.0,  2.0) * s) * (W1 * W2);
    sum += texture2D(u_src, v_uv + vec2( 0.0,  2.0) * s) * (W0 * W2);
    sum += texture2D(u_src, v_uv + vec2( 1.0,  2.0) * s) * (W1 * W2);
    sum += texture2D(u_src, v_uv + vec2( 2.0,  2.0) * s) * (W2 * W2);

    gl_FragColor = sum;
}
