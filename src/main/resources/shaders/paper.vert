// Fullscreen ground pass for PaperBackground. The quad is built in world units
// by the Java side (it inverts the camera matrix once per frame), so the world
// position the fragment stage needs is just the vertex position -- no matrix
// inversion in GLSL, which keeps this inside the GLES 3.0 / GLSL 120 overlap.

attribute vec2 a_position;

uniform mat4 u_projTrans;

varying vec2 v_worldPos;

void main() {
    v_worldPos = a_position;
    gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
}
