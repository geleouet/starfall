package dev.starfall.rig;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import dev.starfall.anim.Skeleton;
import dev.starfall.sim.ClothSim;
import dev.starfall.sim.HairSim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link dev.starfall.capture.SceneProbe}'s payload for the System 3 scenes: where the
 * simulation thinks its particles are, in the image pixels the capture writes.
 *
 * <h2>Why this exists, stated plainly</h2>
 *
 * <p>Two review passes in a row were lost to one confusion. Pass 1's finding was
 * <i>"whatever the particle does, the picture does not show it"</i>. Pass 2 answered it
 * with {@code SimTimingTest}, which measures
 * {@code back.x(back.particleCount()-1)} -- <b>the particle, not the picture</b> -- so
 * every cloth lag figure pass 2 stated was a reading of the simulation's own state and
 * the pass-1 finding went unaddressed while appearing to be closed.
 *
 * <p>{@code docs/feedback-loop.md} anticipated exactly this and named the cure:
 *
 * <blockquote>A scene may also implement {@code dev.starfall.capture.SceneProbe} to
 * report its own simulation state; when it does, {@code timing} records that alongside
 * the pixel measurement. That is how you tell "the particle moved but the picture did
 * not" from "nothing moved" -- a distinction {@code system3-debt.md} turns on. Nothing
 * implements it yet.</blockquote>
 *
 * <p>This is that implementation. It reports the particle; {@link dev.starfall.capture.TimingApp}
 * reports the pixels; {@code analyse timing} prints both in one arrival chain, tagged
 * {@code sim:} so nobody can quote one while believing it is the other. Only the pixel
 * rows are the picture, and the pixel rows are what STYLE.md grades.
 *
 * <h2>Coordinates</h2>
 *
 * <p>Image pixels, origin top left, +y down -- the same convention as the written PNGs
 * and as every rectangle in {@code docs/regions.json}. The projection is the scene's own
 * camera, so a probe point and the ink drawn from it land on the same pixel by
 * construction rather than by agreement.
 */
public final class SimProbe {

    private SimProbe() {
    }

    /**
     * Named simulation points for a {@link SimSceneDriver}, projected through {@code camera}.
     *
     * <p>The set is deliberately small and deliberately paired with the region set: every
     * probe here has a rectangle in {@code docs/regions.json} it can be read against.
     * {@code backTip} is the particle {@code SimTimingTest} reads, so the one number that
     * has been quoted for two passes is directly comparable with the pixels beside it.
     */
    public static Map<String, float[]> probe(SimSceneDriver driver, OrthographicCamera camera,
                                             int width, int height) {
        Map<String, float[]> out = new LinkedHashMap<>();
        Skeleton skeleton = driver.skeleton();
        Vector2 v = new Vector2();
        Vector3 t = new Vector3();

        skeleton.worldPosition(skeleton.bone("hips").index, v);
        out.put("hips", project(camera, width, height, v.x, v.y, t));
        skeleton.worldPosition(skeleton.bone("head").index, v);
        out.put("head", project(camera, width, height, v.x, v.y, t));

        ClothSim cloth = driver.sim().cloth();
        ClothSim.Chain back = cloth.chain(0);
        // Every particle of the back rail, because the whole question is *where down the
        // panel* the motion starts: particle 0 is pinned to the hips and can only ever
        // arrive with them, and a lag quoted off the tip says nothing about the rows in
        // between, which are the ones that carry the readable mass.
        for (int i = 0; i < back.particleCount(); i++) {
            out.put("back" + i, project(camera, width, height, back.x(i), back.y(i), t));
        }
        ClothSim.Chain front = cloth.chain(1);
        out.put("frontTip", project(camera, width, height,
                front.x(front.particleCount() - 1), front.y(front.particleCount() - 1), t));
        ClothSim.Chain sleeve = cloth.chain(2);
        out.put("sleeveTip", project(camera, width, height,
                sleeve.x(sleeve.particleCount() - 1), sleeve.y(sleeve.particleCount() - 1), t));

        HairSim hair = driver.sim().hair();
        int longest = -1;
        float best = -1f;
        for (int i = 0; i < hair.strandCount(); i++) {
            HairSim.Strand s = hair.strand(i);
            if (s.kind == HairSim.Kind.WISP && !s.escapee && s.length() > best) {
                best = s.length();
                longest = i;
            }
        }
        if (longest >= 0) {
            HairSim.Strand s = hair.strand(longest);
            out.put("hairTip", project(camera, width, height,
                    s.x(s.particleCount() - 1), s.y(s.particleCount() - 1), t));
        }
        return out;
    }

    private static float[] project(OrthographicCamera camera, int width, int height,
                                   float worldX, float worldY, Vector3 scratch) {
        scratch.set(worldX, worldY, 0f).prj(camera.combined);
        return new float[] {
                (scratch.x * 0.5f + 0.5f) * width,
                (1f - (scratch.y * 0.5f + 0.5f)) * height
        };
    }
}
