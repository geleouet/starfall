// Vertex stage for System 4's ink marks: the bloom, the shed flecks, the clash
// and the vermillion seal of STYLE.md 7.3 and 5.
//
// Same shape as ink_glow: a CPU-built world-space mesh whose entire falloff is
// carried in per-vertex alpha. That is deliberate rather than lazy -- a drop of
// ink hitting wet paper has a soft, irregular boundary, and an irregular
// boundary authored at the vertices is exactly as smooth as the interpolator,
// with none of the ringing a radial function in the fragment stage would give at
// the rim. It also means the marks cost nothing per pixel, which matters because
// a five-beat phrase can have a dozen of them alive at once.

attribute vec2 a_position;
attribute vec4 a_color;

uniform mat4 u_projTrans;

varying vec4 v_color;

void main() {
    v_color = a_color;
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}
