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
     * Three bands of (centre world y, half height, strength).
     *
     * <p>Revision 3 (rig-fixes-3 item 5). The figure runs from feet at y=0.13 to
     * the top of the topknot near y=1.83, so its lower third ends at y=0.70. The
     * pass-2 measurement found the lowest figure ink well above the haze with
     * clean paper between the two, which is the one thing STYLE.md 6 calls
     * non-negotiable. The bands are therefore both raised and widened until they
     * overlap: band 0 buries the hem and the feet, band 1 crosses the hakama, and
     * band 2 -- the topmost -- straddles y=0.70 and so cuts across the figure's
     * lower third rather than sitting under it.
     *
     * <p>They are still bands and not a veil: the falloff reaches zero at one
     * half-height, the drift rates differ per band, and the mist pass in
     * {@link PaperBackground} breaks each one into drifting lobes on top of this.
     * Nothing here reaches the chest.
     */
    private static final float[] BANDS = {
           -0.14f, 0.56f, 0.90f,
            0.30f, 0.26f, 0.62f,
            0.72f, 0.20f, 0.30f,
    };

    private Atmosphere() {
    }

    static void setFogUniforms(ShaderProgram shader) {
        shader.setUniform3fv("u_fogBands[0]", BANDS, 0, BANDS.length);
    }
}
