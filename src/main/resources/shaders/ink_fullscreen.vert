// Shared vertex stage for every screen-space pass of the ink material: the two
// coverage blurs and the resolve. The quad is already in clip space, so there is
// no matrix here at all.
//
// u_frameMin / u_frameSize are the world-space rect the frame covers, handed in
// by the Java side (which inverts the camera once per frame, exactly as
// PaperBackground does). Only the resolve reads v_worldPos -- it needs world
// coordinates for the fog bands of STYLE.md 6, which are the one part of the
// material that lives in world space rather than material space. The blur passes
// leave the uniforms unset, which is harmless: they never sample the varying.

attribute vec2 a_position;

uniform vec2 u_frameMin;
uniform vec2 u_frameSize;

varying vec2 v_uv;
varying vec2 v_worldPos;

void main() {
    v_uv = a_position * 0.5 + 0.5;
    v_worldPos = u_frameMin + v_uv * u_frameSize;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
