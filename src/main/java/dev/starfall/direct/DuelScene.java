package dev.starfall.direct;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.capture.SceneProbe;
import dev.starfall.render.HairRenderer;
import dev.starfall.render.InkFxRenderer;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;
import dev.starfall.sim.ClothSim;
import dev.starfall.sim.HairSim;
import dev.starfall.stage.Framing;
import dev.starfall.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The graded System 4 scenes: two figures, one rules engine, one schedule.
 *
 * <p>Structure follows {@code SimScene} exactly -- the same paper, the same ink
 * material, the same hair pass -- with three additions, each of which is a thing
 * that has never happened in this project before:
 *
 * <ol>
 *   <li><b>Two rigs.</b> Drawn through one {@code InkSkinnedRenderer}, in two merge
 *       groups so each resolves with its own colour and its own coverage field.
 *       See {@link Figure}.</li>
 *   <li><b>A camera that moves.</b> STYLE.md 9's "wide to plan, push in to strike",
 *       taken from the schedule rather than authored here: the framing at any
 *       instant is {@code Schedule.framingAt(t)}, which is a continuous function
 *       of eased camera keys, so the shot cannot cut even if this class wanted it
 *       to.</li>
 *   <li><b>Ink marks.</b> STYLE.md 7.3's bloom, flecks and clash, drawn over the
 *       figures and under the atmosphere.</li>
 * </ol>
 *
 * <h2>The framing, and the one choice made here rather than by the schedule</h2>
 *
 * <p>{@code Framing} is a centre and a <em>width</em> in tiles, deliberately: "the
 * one thing this layer must get right is the ratio between the wide shot and the
 * close one, and that ratio is a property of the lane, not of the screen." So the
 * width is the schedule's and the height is the capture's aspect, which makes the
 * aspect load-bearing. At 16:9 the intimate shot's 3.2 tiles give 1.8 world units
 * of height against a 1.70 figure, and a raised guard puts the kissaki above 2.2 --
 * three frames with no visible weapon is the fault {@code system1-rig-fixes-3} item
 * 6 was written about. So these scenes are captured at 4:3, where the same 3.2
 * tiles give 2.4 units and the blade stays in frame. The framing law is untouched;
 * only the shape of the sheet of paper it is printed on differs.
 */
public class DuelScene implements Scene, SceneProbe {

    /**
     * Seconds of simulation run before the schedule starts, with the clock held at
     * zero.
     *
     * <p>Not {@link Scene#warmup()}, which advances the whole scene and would eat
     * the first half-second of the phrase. Hair and cloth start laid out along
     * their rest shapes with no velocity, and a breeze takes about this long to
     * find where they hang; starting the phrase before that has the garment
     * settling into the breeze and answering the first beat at the same time.
     */
    private static final float SETTLE = 0.8f;

    /** Where the camera looks, as a fraction of the frame height above the ground. */
    private static final float EYE = 0.44f;

    private final Duel.Kind kind;
    private final String name;

    private Duel.Staged staged;
    private Director director;
    private InkSkinnedRenderer renderer;
    private HairRenderer hair;
    private InkFxRenderer fx;
    private PaperBackground paper;
    private OrthographicCamera camera;
    private int width = 960;
    private int height = 720;

    public DuelScene(Duel.Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return kind.description();
    }

    @Override
    public void create(SceneContext ctx) {
        this.staged = Duel.of(kind);
        this.director = new Director(staged.schedule(), Duel.figuresFor(staged));
        this.renderer = new InkSkinnedRenderer();
        this.hair = new HairRenderer();
        this.fx = new InkFxRenderer();
        // STYLE.md 1, Family B: "two swordsmen in profile, blades crossed, against a
        // sky grading from deep indigo at the top through violet to coral/salmon at
        // the horizon", and 1's own note that this family is "the primary template
        // for the game screen". Every graded duel before this pass was fought on
        // Family A cream paper, which inverts 2.2's warm/cool opposition across the
        // whole frame -- warm ground, warm blades, warm bloom -- and the review of
        // pass 3 measured the consequence: "until a duel is fought against a dusk
        // sky, 0 cannot be answered yes for a Family B frame however good the
        // figures get".
        //
        // The renderer is told about it too, because a halo is the ground's colour
        // wicking out of the ink and a constant one would print an aura.
        this.paper = new PaperBackground().dusk(true);
        this.renderer.backdrop(PaperBackground.backdropStops(true));
        this.camera = new OrthographicCamera();
        resize(ctx.width, ctx.height);

        for (Figure f : director.figures()) {
            f.sim().wind(Director.BREEZE_X, Director.BREEZE_Y);
        }
        director.start();
        // Let the garment find the breeze before the phrase begins. The director's
        // clock does not move, so no directive is consumed.
        int steps = Math.round(SETTLE / dev.starfall.capture.SceneClock.SUBSTEP);
        for (int i = 0; i < steps; i++) {
            for (Figure f : director.figures()) {
                f.simulate(dev.starfall.capture.SceneClock.SUBSTEP, 0f);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        aim();
    }

    @Override
    public void update(float dt) {
        director.advance(dt);
        aim();
    }

    /**
     * Points the camera at whatever the schedule says the framing is.
     *
     * <p>Every property STYLE.md 9 asks for is already in the keys -- the glide,
     * the ease, the return being slower than the push-in, the idle drift, the
     * quarter-second floor that makes "never cut, never shake" checkable -- so
     * there is nothing to do here but read them. {@code Schedule.cameraIsContinuous}
     * is the assertion that they join, and {@code DirectorTest} runs it.
     */
    private void aim() {
        Framing f = director == null ? new Framing(0, Stage.INTIMACY_TILES) : director.framing();
        float w = (float) Director.stretchTiles(f.widthTiles());
        float h = w * height / (float) width;
        camera.setToOrtho(false, w, h);
        camera.position.set((float) Director.stretch(f.centreTile() * Stage.TILE_WIDTH), h * EYE, 0f);
        camera.update();
    }

    @Override
    public void render() {
        float t = (float) director.time();
        paper.render(camera.combined, t);

        renderer.begin(camera.combined, t);
        for (Figure f : director.figures()) {
            renderer.draw(f.rig().mesh(), f.skeleton(), f.clothMaterial());
            // The blade flushes the cloth group before it draws, so each figure's
            // garment resolves with its own colour and its own halo, and steel
            // lands over the ink it belongs to rather than over both figures'.
            renderer.draw(f.rig().bladeMesh(), f.skeleton(), f.bladeMaterial());
        }
        renderer.end();

        hair.begin(camera.combined, t, HairRenderer.worldPerPixel(camera.combined, width));
        for (Figure f : director.figures()) {
            hair.draw(f.sim().hair());
        }
        hair.end();

        // Ink over the figures: STYLE.md 7.3's bloom spreads "from the contact
        // point *through the garment*", so it sits on the cloth rather than behind
        // it. Under the atmosphere, because mist is in front of everything.
        fx.begin(camera.combined);
        director.renderInk(fx);
        fx.end();

        paper.renderOverlay(camera.combined, t);
        Opaque.seal();
    }

    /** The score's own length, so a capture with no explicit step covers the whole phrase. */
    @Override
    public float duration() {
        return (float) Duel.of(kind).schedule().duration();
    }

    /**
     * {@link SceneProbe}: where the two figures' simulations think they are, in the
     * image pixels the capture writes.
     *
     * <p>Named per body, because with two figures on screen an unqualified "hips"
     * is the exact ambiguity STYLE.md 11.3 was written about. The set is the same
     * one {@code SimProbe} reports for a single figure, so a lag measured here is
     * directly comparable with the numbers {@code system3-debt.md} recorded.
     */
    @Override
    public Map<String, float[]> probe() {
        Map<String, float[]> out = new LinkedHashMap<>();
        Vector2 v = new Vector2();
        Vector3 p = new Vector3();
        for (Figure f : director.figures()) {
            String tag = f.body() == staged.hero() ? "hero" : "foe";
            out.put(tag + "Hips", project(f.where("hips", v), p));
            out.put(tag + "Head", project(f.where("head", v), p));
            out.put(tag + "Hand", project(f.where("handL", v), p));
            ClothSim cloth = f.sim().cloth();
            ClothSim.Chain back = cloth.chain(0);
            // Particle 2 rather than the tip: system3-debt.md section 3 records
            // that at 1.15 units of lever a 2.7 degree bend cancels the whole of
            // the hips' travel, so a hem that trails properly leaves its tip very
            // nearly stationary and the onset statistic on it becomes noise. Row 2
            // is the one that carries readable ink.
            out.put(tag + "Hem", pixel(back.x(2), back.y(2), p));
            ClothSim.Chain sleeve = cloth.chain(2);
            out.put(tag + "Sleeve", pixel(sleeve.x(sleeve.particleCount() - 1),
                    sleeve.y(sleeve.particleCount() - 1), p));
            HairSim hairSim = f.sim().hair();
            int longest = -1;
            float best = -1f;
            for (int i = 0; i < hairSim.strandCount(); i++) {
                HairSim.Strand s = hairSim.strand(i);
                if (s.kind == HairSim.Kind.WISP && !s.escapee && s.length() > best) {
                    best = s.length();
                    longest = i;
                }
            }
            if (longest >= 0) {
                HairSim.Strand s = hairSim.strand(longest);
                out.put(tag + "HairTip",
                        pixel(s.x(s.particleCount() - 1), s.y(s.particleCount() - 1), p));
            }
        }
        return out;
    }

    private float[] project(Vector2 world, Vector3 scratch) {
        return pixel(world.x, world.y, scratch);
    }

    private float[] pixel(float wx, float wy, Vector3 scratch) {
        scratch.set(wx, wy, 0f).prj(camera.combined);
        return new float[] {
                (scratch.x * 0.5f + 0.5f) * width,
                (1f - (scratch.y * 0.5f + 0.5f)) * height
        };
    }

    Director director() {
        return director;
    }

    OrthographicCamera camera() {
        return camera;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    @Override
    public void dispose() {
        if (director != null) {
            for (Figure f : director.figures()) {
                f.rig().mesh().dispose();
                f.rig().bladeMesh().dispose();
            }
        }
        if (renderer != null) {
            renderer.dispose();
        }
        if (hair != null) {
            hair.dispose();
        }
        if (fx != null) {
            fx.dispose();
        }
        if (paper != null) {
            paper.dispose();
        }
    }
}
