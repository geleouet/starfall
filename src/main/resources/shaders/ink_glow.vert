// Vertex stage for the blade's outer glow sheath and its swept trail
// (STYLE.md 5). Both are CPU-built world-space ribbons whose entire shape --
// the cross-ribbon falloff, the brightening toward the tip, the fade with age --
// is carried in per-vertex alpha, so the fragment stage has nothing to do but
// interpolate. That is deliberate: a soft luminous arc wants a smooth gradient,
// and a gradient authored at the vertices is exactly smooth.

attribute vec2 a_position;
attribute vec4 a_color;

uniform mat4 u_projTrans;

varying vec4 v_color;

void main() {
    v_color = a_color;
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}
