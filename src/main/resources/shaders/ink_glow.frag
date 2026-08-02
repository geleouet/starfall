#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;

void main() {
    // Straight alpha over the paper rather than additive. STYLE.md 10 lists
    // "neon glow / bloom on everything" as a fail, and additive blending over a
    // ground this bright clips to white almost immediately; a pale cool wash at
    // low alpha reads as luminous against warm paper without ever getting there.
    gl_FragColor = v_color;
}
