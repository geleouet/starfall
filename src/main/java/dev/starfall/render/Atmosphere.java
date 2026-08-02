package dev.starfall.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * The drifting fog bands of STYLE.md section 6, shared by the paper ground and
 * the ink material.
 *
 * <p>They have to be shared. The bands are drawn on the paper, and the figure
 * fades into them in its own shader (there is no foreground pass -- contract
 * section F gives PaperBackground a single render() call, which happens before
 * the figure). If the two ever computed different bands the figure would fade
 * into clear air, which is exactly the "shader effect" tell the whole style is
 * trying to avoid. Both shaders therefore read the same uniform array and run
 * the same band function.
 *
 * <p>Values are in world units, matched to the rig: the figure stands with its
 * feet near y = 0.13 and its head near y = 1.75, so band 0 buries the hem, band
 * 1 crosses the thighs and band 2 is a thin wisp at the waist.
 */
final class Atmosphere {

    /**
     * Three bands of (centre world y, half height, strength). Kept narrow on
     * purpose: a band tall enough to cover the whole lower frame stops reading as
     * a band and turns into a flat veil over the picture, which is how the first
     * pass of this lost all of its warmth.
     */
    private static final float[] BANDS = {
            0.02f, 0.34f, 0.55f,
            0.52f, 0.16f, 0.30f,
            1.02f, 0.10f, 0.16f,
    };

    private Atmosphere() {
    }

    static void setFogUniforms(ShaderProgram shader) {
        shader.setUniform3fv("u_fogBands[0]", BANDS, 0, BANDS.length);
    }
}
