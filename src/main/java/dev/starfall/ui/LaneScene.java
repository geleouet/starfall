package dev.starfall.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneClock;
import dev.starfall.capture.SceneContext;
import dev.starfall.capture.SceneProbe;
import dev.starfall.direct.Director;
import dev.starfall.direct.Figure;
import dev.starfall.direct.Opaque;
import dev.starfall.render.HairRenderer;
import dev.starfall.render.InkFxRenderer;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;
import dev.starfall.stage.Framing;
import dev.starfall.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * System 5's graded scene: a fight with an interface on it.
 *
 * <p>The world half is {@code DuelScene}'s, unchanged in substance -- the same
 * dusk stage, the same ink material, the same hair pass, the same camera taken
 * from the schedule rather than authored here. Three things are new and all three
 * are the point of this system:
 *
 * <ol>
 *   <li><b>The interface is drawn before the figures.</b> {@link LaneInterface}
 *       goes down straight after the paper, so a duellist that walks into the
 *       margin passes in front of the column. That single line of ordering is the
 *       strongest available answer to "does this read as a HUD", and it is
 *       measurable rather than arguable -- see {@code docs/system5-debt.md}.</li>
 *   <li><b>The camera's vertical framing scales with its width.</b> STYLE.md 9
 *       states the shot in tiles and says nothing about height, and
 *       {@code system4-debt.md} names the consequence as unpaid: "the planning
 *       framing's composition -- two 85 px figures in the bottom-left of an
 *       85%-empty frame". At a fixed eye line the ground sits 6% up the frame at
 *       every framing, so a wide shot puts everything in the bottom fifth. Here the
 *       eye line rides from {@link #EYE_NEAR} at the intimate shot to
 *       {@link #EYE_WIDE} at the planning one, which lifts the ground to about a
 *       fifth of the frame when the camera is wide and leaves the close shot
 *       exactly where System 4 had it. It is a continuous function of the framing
 *       width, so it cannot introduce a cut into a shot the schedule guarantees is
 *       continuous.</li>
 *   <li><b>The sheet carries a bounded parallax.</b> See
 *       {@link #PARALLAX_LIMIT}.</li>
 * </ol>
 */
public class LaneScene implements Scene, SceneProbe {

    /** Seconds of cloth and hair settling run before the score starts. As {@code DuelScene}. */
    private static final float SETTLE = 0.8f;

    /** Where the camera looks at the intimate framing, as a fraction of frame height. */
    public static final float EYE_NEAR = 0.44f;

    /**
     * And at the planning framing, which lifts the ground off the bottom edge.
     *
     * <p>The ground plane lands at {@code 0.5 - eye} of the frame height, so this
     * puts it at 0.28 -- a little above the lower third -- where a fixed 0.44 puts
     * it at 0.06 and every body in the picture inside the bottom fifth.
     */
    public static final float EYE_WIDE = 0.22f;

    /**
     * The furthest the margin is allowed to drift with the camera, in frame heights.
     *
     * <p>STYLE.md 9 asks for "a subtle parallax between fog layers, figures, and
     * ground" during the move, and the margin of the sheet is simply the furthest
     * layer of all -- so it moves, and it moves least. The cap is what keeps it a
     * depth cue rather than a moving target: at 720 rows this is 14 px over a
     * push-in that carries the world across the whole frame.
     */
    public static final float PARALLAX_LIMIT = 0.020f;

    /** How much of the camera's own travel the margin takes, before the cap. */
    public static final float PARALLAX_SHARE = 0.14f;

    /**
     * How much air there is at the most intimate framing.
     *
     * <p>Not zero: STYLE.md 6's fog bands are <i>"present in every single reference
     * image"</i>, including images 3, 4 and 5, which are the intimate duel this
     * framing quotes. What the camera's distance buys is <em>more</em> of it, not
     * the difference between some and none.
     */
    public static final float FOG_FLOOR = 0.42f;

    /**
     * How much of {@link Readout#haze(double)} is still spent on the figures
     * themselves.
     *
     * <p>See the note beside its use: the bands carry the atmosphere now, and this
     * is the depth desaturation of STYLE.md 6's second bullet only.
     */
    public static final float SUBJECT_HAZE = 0.5f;

    private final Bout.Kind kind;
    private final String name;

    /**
     * The control of STYLE.md 11.2b(g): the identical scene with no interface on it.
     *
     * <p><i>"Before a number is allowed to decide anything, someone must say what it
     * would read if the thing being measured were absent -- and then produce that
     * case."</i> Every claim about what the interface puts on the sheet is a
     * difference against this, and it is a <b>separate registered scene</b> rather
     * than a flag, so the manifest a capture writes names it: a bare shot cannot be
     * mistaken for a live one, and a live one cannot be quoted as a control, because
     * {@code capture.txt} records {@code scene=} and the two differ.
     */
    private final boolean bare;

    private Bout.Staged staged;
    private Director director;
    private InkSkinnedRenderer renderer;
    private HairRenderer hair;
    private InkFxRenderer fx;
    private InkUiRenderer ui;
    private PaperBackground paper;
    private OrthographicCamera camera;
    private final Matrix4 sheet = new Matrix4();
    private int width = 960;
    private int height = 720;

    public LaneScene(Bout.Kind kind, String name) {
        this(kind, name, false);
    }

    public LaneScene(Bout.Kind kind, String name, boolean bare) {
        this.kind = kind;
        this.name = name;
        this.bare = bare;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return kind.description() + (bare ? " -- CONTROL: the same score with no interface" : "");
    }

    @Override
    public void create(SceneContext ctx) {
        this.staged = Bout.of(kind);
        this.director = new Director(staged.schedule(), figures(staged));
        this.renderer = new InkSkinnedRenderer();
        this.hair = new HairRenderer();
        this.fx = new InkFxRenderer();
        this.ui = new InkUiRenderer();
        this.paper = new PaperBackground().dusk(true);
        this.renderer.backdrop(PaperBackground.backdropStops(true));
        this.camera = new OrthographicCamera();
        resize(ctx.width, ctx.height);

        for (Figure f : director.figures()) {
            f.sim().wind(Director.BREEZE_X, Director.BREEZE_Y);
        }
        director.start();
        int steps = Math.round(SETTLE / SceneClock.SUBSTEP);
        for (int i = 0; i < steps; i++) {
            for (Figure f : director.figures()) {
                f.simulate(SceneClock.SUBSTEP, 0f);
            }
        }
    }

    /**
     * The bodies, coloured and placed.
     *
     * <p>The Night Pilgrim is the dark duellist for the reason {@code Duel} gives
     * -- it is the subject, and STYLE.md 2.2 wants the value extremes spent on what
     * matters -- and every Charted Shadow is pale. Pale first in the draw order, so
     * the dark silhouette is never interrupted.
     */
    private static List<Figure> figures(Bout.Staged staged) {
        List<Figure> out = new ArrayList<>();
        for (Bout.Staged.Body b : staged.bodies()) {
            if (!b.hero()) {
                out.add(Figure.pale(b.id()).standAt(
                        Director.stretch(b.tile() * Stage.TILE_WIDTH), b.facing().step()));
            }
        }
        for (Bout.Staged.Body b : staged.bodies()) {
            if (b.hero()) {
                out.add(Figure.dark(b.id()).standAt(
                        Director.stretch(b.tile() * Stage.TILE_WIDTH), b.facing().step()));
            }
        }
        return out;
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        sheet.setToOrtho2D(0f, 0f, width / (float) height, 1f);
        aim();
    }

    @Override
    public void update(float dt) {
        director.advance(dt);
        aim();
    }

    private void aim() {
        Framing f = director == null ? new Framing(0, Stage.INTIMACY_TILES) : director.framing();
        float w = (float) Director.stretchTiles(f.widthTiles());
        float h = w * height / (float) width;
        float eye = EYE_NEAR + (EYE_WIDE - EYE_NEAR) * (1f - intimacy());
        camera.setToOrtho(false, w, h);
        camera.position.set((float) Director.stretch(f.centreTile() * Stage.TILE_WIDTH), h * eye, 0f);
        camera.update();
    }

    private float intimacy() {
        return director == null ? 1f : (float) staged.readout().intimacy(director.time());
    }

    /** How far the margin has drifted with the camera, in frame heights. */
    private float parallax() {
        if (director == null) {
            return 0f;
        }
        Framing now = director.framing();
        Framing plan = new Stage(kind.laneLength()).planning();
        double travel = (now.centreTile() - plan.centreTile()) / Math.max(1.0, kind.laneLength());
        double p = -travel * PARALLAX_SHARE;
        return (float) Math.max(-PARALLAX_LIMIT, Math.min(PARALLAX_LIMIT, p));
    }

    @Override
    public void render() {
        float t = (float) director.time();
        Look look = staged.readout().at(t);

        paper.render(camera.combined, t);

        // STYLE.md 6's fog, as bands and on both sides of the figures. It is world
        // rather than interface, so the -bare control carries it too and every
        // live - bare number in docs/system5-debt.md still measures the interface.
        float haze = (float) staged.readout().haze(t);
        float air = FOG_FLOOR + (1f - FOG_FLOOR) * haze;
        ui.begin(camera.combined);
        Fog.bank(ui, Fog.Layer.FAR, t, camera.position.x, camera.position.y,
                camera.viewportWidth, camera.viewportHeight, air);
        ui.end();

        // The Fold of the World, in world units: it scales, translates and is
        // occluded exactly as the ground the figures stand on, because it is that
        // ground.
        if (!bare) {
            ui.begin(camera.combined);
            LaneInterface.ground(ui, look, Director.stretchTiles(Stage.TILE_WIDTH),
                    camera.viewportHeight);
            // The Charted Shadows' strokes, pinned to the bodies they count. The
            // figure's own stand position rather than the chapter's tile, so the
            // row travels with the body instead of waiting for it on the board.
            for (Look.Shadow s : look.shadows()) {
                for (Figure f : director.figures()) {
                    if (f.body() == s.body()) {
                        LaneInterface.shadow(ui, s, (float) f.standX(),
                                camera.viewportHeight, look.intimacy());
                    }
                }
            }
            ui.end();

            // The margin of the sheet, in frame heights -- and still under the figures.
            ui.begin(sheet);
            LaneInterface.sheet(ui, look, width / (float) height, parallax());
            ui.end();
        }

        // STYLE.md 9's "heavy fog, Family C mood": going wide costs the figures
        // contrast the way distance does. Driven by the framing width and nothing
        // else, so it is continuous wherever the camera is.
        //
        // <b>Halved, because the fog is now bands.</b> The pass-2 review's
        // structural finding was that atmosphere delivered as attenuation of the
        // subject "removes readable parts and adds none". The bands above and below
        // carry STYLE.md 6's requirement; this term survives only as depth
        // desaturation on a body seen through them (6's second bullet), at a weight
        // that costs the figure a fraction of what it did.
        renderer.haze(haze * SUBJECT_HAZE);
        hair.haze(haze * SUBJECT_HAZE);

        renderer.begin(camera.combined, t);
        for (Figure f : director.figures()) {
            renderer.draw(f.rig().mesh(), f.skeleton(), f.clothMaterial());
            renderer.draw(f.rig().bladeMesh(), f.skeleton(), f.bladeMaterial());
        }
        renderer.end();

        hair.begin(camera.combined, t, HairRenderer.worldPerPixel(camera.combined, width));
        for (Figure f : director.figures()) {
            hair.draw(f.sim().hair());
        }
        hair.end();

        fx.begin(camera.combined);
        director.renderInk(fx);
        fx.end();

        // And the near bank, in front of the bodies: STYLE.md 6's "occlude the
        // lower body of figures". Kept below hem height so what it veils is the
        // dissolving lower third the ink material already frays, and not the head,
        // the hands or the blade -- the parts STYLE.md 11.0 counts.
        ui.begin(camera.combined);
        Fog.bank(ui, Fog.Layer.NEAR, t, camera.position.x, camera.position.y,
                camera.viewportWidth, camera.viewportHeight, air);
        ui.end();

        paper.renderOverlay(camera.combined, t);
        Opaque.seal();
    }

    @Override
    public float duration() {
        return (float) Bout.of(kind).schedule().duration();
    }

    /**
     * {@link SceneProbe}: where the interface's own marks land, in the pixels the
     * capture writes.
     *
     * <p>STYLE.md 11.3 requires a number to be printed beside the rectangle it was
     * taken through, and every claim about the column is a claim about where a mark
     * is. So the scene reports the centre of each cartouche the frame contains,
     * named by its reading position -- {@code stanza0} is the topmost and the next
     * to resolve -- which is what lets a reading-order assertion be made against
     * delivered pixels rather than against the model that produced them.
     */
    @Override
    public Map<String, float[]> probe() {
        Map<String, float[]> out = new LinkedHashMap<>();
        Look look = staged.readout().at(director.time());
        float px = parallax();
        List<Look.Written> stanza = look.stanza();
        for (int i = 0; i < stanza.size(); i++) {
            out.put("stanza" + i, sheetPixel(LaneInterface.COLUMN_X + px,
                    LaneInterface.STANZA_BASE_Y + stanza.get(i).height() * LaneInterface.STANZA_PITCH));
        }
        float aspect = width / (float) height;
        for (int i = 0; i < look.hand().size(); i++) {
            out.put("hand" + i, sheetPixel(aspect - LaneInterface.HAND_INSET + px,
                    LaneInterface.HAND_TOP_Y - i * LaneInterface.HAND_PITCH));
        }
        out.put("health", sheetPixel(LaneInterface.HEALTH_X, LaneInterface.HEALTH_Y));
        for (int tile : look.threatened()) {
            out.put("strike" + tile, worldPixel(
                    (float) (tile * Director.stretchTiles(Stage.TILE_WIDTH)),
                    -LaneInterface.STRIKE_DROP * camera.viewportHeight));
        }
        for (Figure f : director.figures()) {
            com.badlogic.gdx.math.Vector2 v = new com.badlogic.gdx.math.Vector2();
            out.put("body" + f.body(), worldPixel(f.where("hips", v).x, f.where("hips", v).y));
        }
        for (Look.Shadow s : look.shadows()) {
            for (Figure f : director.figures()) {
                if (f.body() == s.body()) {
                    out.put("shadow" + s.body(),
                            worldPixel((float) f.standX(), LaneInterface.SHADOW_Y));
                }
            }
        }
        out.put("intimacy", new float[] {intimacy(), (float) director.framing().widthTiles()});
        return out;
    }

    /** A point in frame heights, in image pixels with the origin at the top left. */
    private float[] sheetPixel(float x, float y) {
        return new float[] {x * height, (1f - y) * height};
    }

    private float[] worldPixel(float wx, float wy) {
        Vector3 p = new Vector3(wx, wy, 0f).prj(camera.combined);
        return new float[] {(p.x * 0.5f + 0.5f) * width, (1f - (p.y * 0.5f + 0.5f)) * height};
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
        if (ui != null) {
            ui.dispose();
        }
        if (paper != null) {
            paper.dispose();
        }
    }
}
