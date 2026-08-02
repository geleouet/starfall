// Vertex stage for the hair ribbons of STYLE.md 4.
//
// The CPU builds a smooth spine (Catmull-Rom through the Verlet particles) and
// hands this shader, per spine sample, the point, its normal, its half-width and
// which rail this vertex is. The widening happens here rather than on the CPU
// for one reason, and it is the reason the tips read as brush lift-off at all:
//
//   STYLE.md 4 asks the taper to reach *sub-pixel*. A polygon narrower than a
//   pixel is not narrow, it is absent -- the rasteriser drops it, which is
//   exactly what debt D2 recorded about the blade's kissaki ("the last fifth was
//   two quads: the rasteriser dropped it and the blade ended by fading out at a
//   constant two pixels rather than converging"). So the geometry stops
//   narrowing at just over half a pixel and the *remaining* narrowing is carried
//   in alpha. A ribbon whose true half-width is a fifth of a pixel is drawn at
//   0.55 px and a fifth as opaque, which is what a fifth of a pixel of ink
//   actually looks like.
//
// u_pxWorld is world units per pixel, i.e. the same pixel-footprint quantity
// STYLE.md 3b.1 asks every detail octave to declare itself against. It is *not*
// a screen-space measurement of a silhouette -- that is the pass-3 regression
// the debt document warns about, and it is a different thing: nothing here reads
// back a rendered edge, this is a constant handed down from the camera.

attribute vec2 a_position;      // spine point, world space
attribute vec3 a_texCoord0;     // x strand seed, y arc length along the strand (world units), z s in 0..1
attribute vec4 a_color;         // rgb ink value, a authored opacity
attribute vec4 a_generic;       // xy unit normal, z half-width (world), w rail (-1 or +1)

uniform mat4 u_projTrans;
uniform float u_pxWorld;

varying vec3 v_mat;
varying vec4 v_color;
varying float v_across;
varying float v_subpixel;
varying vec2 v_world;
/** World half-width of this ribbon here, so the fragment stage can measure across it in metres rather than in rails. */
varying float v_halfWidth;

void main() {
    float wMin = 0.55 * u_pxWorld;
    float halfW = max(a_generic.z, wMin);
    v_subpixel = min(1.0, a_generic.z / max(wMin, 1e-7));

    v_mat = a_texCoord0;
    v_color = a_color;
    v_across = a_generic.w;
    v_halfWidth = halfW;

    vec2 p = a_position + a_generic.xy * (halfW * a_generic.w);
    v_world = p;
    gl_Position = u_projTrans * vec4(p, 0.0, 1.0);
}
