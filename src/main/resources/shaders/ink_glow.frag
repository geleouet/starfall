#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;

void main() {
    // Premultiplied, because InkSkinnedRenderer composites this with
    // (ONE, ONE_MINUS_SRC_COLOR) -- the screen operator (shader-fixes-3 item 6).
    //
    // Revision 2 wrote straight alpha and blended it normally. A cool pale grey
    // at low alpha over the warm cream paper of Family A composites darker and
    // cooler than the ground it sits on, so the blade's arc-trail read as a
    // dirty smudge instead of as light. Screen can only lighten, and unlike
    // plain additive it approaches the paper's own brightness asymptotically
    // rather than clipping to white, which is what keeps this out of the
    // "neon glow on everything" failure of STYLE.md 10.
    gl_FragColor = vec4(v_color.rgb * v_color.a, v_color.a);
}
