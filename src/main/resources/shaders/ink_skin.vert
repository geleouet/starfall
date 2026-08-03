// Linear blend skinning for the ink material -- contract sections A and B.
//
// No #version directive on purpose: libGDX prepends the platform-appropriate one,
// and the syntax below is the intersection of desktop GLSL 120 and GLSL ES. The
// one thing here that needs the GLES 3.0 feature set (per contract section F) is
// indexing u_bones[] with a value read from an attribute -- GLES 2.0 restricts
// uniform-array indices to constant expressions, GLES 3.0 does not.

attribute vec3 a_position;      // bind-space position, z always 0
attribute vec2 a_texCoord0;     // material-space UV: x across the strip, y along it
attribute vec4 a_color;         // NOT a colour: dissolve, wetness, stainMask, flowU
attribute vec2 a_boneWeight0;   // (boneIndex, weight)
attribute vec2 a_boneWeight1;
attribute vec2 a_boneWeight2;
attribute vec2 a_boneWeight3;

// System 3b raised this from 32: the skeleton was already at 31 bones (21 body
// + 10 cloth — the "28" in SamuraiRig's own javadoc was stale), and the three
// face bones took it to 34. 36 mat4 = 144 vec4, still comfortably inside the
// GLES 3.0 guaranteed minimum of 256 vertex uniform vectors the original cap
// was derived from; two slots remain for 3c's far sleeve / far hakama.
const int MAX_BONES = 36;

uniform mat4 u_projTrans;
uniform mat4 u_bones[MAX_BONES];

// The fragment stage does all of its sampling in material space, so the
// undeformed position travels through untouched -- that is what stops the ink
// swimming over the cloth (STYLE.md 3.5).
varying vec2 v_matPos;
varying vec2 v_uv;
varying vec4 v_ink;
varying vec2 v_flowDir;
varying vec2 v_worldPos;

void main() {
    // Weights are normalised at build time (contract section A), so the blend is
    // a plain weighted sum with no renormalisation. Unused slots carry index 0 /
    // weight 0 and contribute a zero matrix rather than a garbage transform.
    mat4 skin = u_bones[int(a_boneWeight0.x)] * a_boneWeight0.y;
    skin += u_bones[int(a_boneWeight1.x)] * a_boneWeight1.y;
    skin += u_bones[int(a_boneWeight2.x)] * a_boneWeight2.y;
    skin += u_bones[int(a_boneWeight3.x)] * a_boneWeight3.y;

    vec4 posed = skin * vec4(a_position.xy, 0.0, 1.0);

    v_matPos = a_position.xy;
    v_uv = a_texCoord0;
    v_ink = a_color;
    v_worldPos = posed.xy;

    // flowU is an angle packed into 0..1. Decoding it here and interpolating the
    // resulting vector, rather than interpolating the angle itself, means a strip
    // whose flow crosses the 1 -> 0 wrap does not sweep a full turn across one quad.
    float ang = a_color.a * 6.28318530718;
    v_flowDir = vec2(cos(ang), sin(ang));

    gl_Position = u_projTrans * posed;
}
