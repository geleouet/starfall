package dev.starfall.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneClock;
import dev.starfall.capture.SceneContext;
import dev.starfall.capture.SceneEvents;
import dev.starfall.capture.SceneProbe;
import dev.starfall.combat.Command;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.direct.Director;
import dev.starfall.direct.Figure;
import dev.starfall.direct.Opaque;
import dev.starfall.render.HairRenderer;
import dev.starfall.render.InkFxRenderer;
import dev.starfall.render.InkSkinnedRenderer;
import dev.starfall.render.PaperBackground;
import dev.starfall.stage.Framing;
import dev.starfall.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The first scene in this project a person can play and lose.
 *
 * <h2>What it is</h2>
 *
 * <p>{@code LaneScene}'s world -- same stage, same material, same fog, same
 * camera law -- driven by a {@link Session} instead of a pre-staged score. The
 * duplication of the render body is the same accepted debt {@code LaneScene}
 * already carries against {@code DuelScene}, one scene further along.
 *
 * <h2>The three ways it is driven, and why they are one code path</h2>
 *
 * <ol>
 *   <li><b>Keyboard</b>, in the desktop launcher: digits bank, backspace
 *       un-banks (LIFO -- only the top can be taken back, which is the queue's
 *       whole grammar), enter commits, T turns, H holds, space held is
 *       fast-forward.</li>
 *   <li><b>A {@link Plays.Pilot}</b>, for the capture harness: a deterministic
 *       player, so the victory and defeat exhibits are reproducible frames.</li>
 *   <li><b>The debug API</b>, via {@link SceneEvents}: {@code add}, {@code remove},
 *       {@code execute}, {@code hold}, {@code turn} -- so a fight can be played
 *       turn by turn over HTTP and every state examined. This is how the pass's
 *       own play-throughs were driven.</li>
 * </ol>
 *
 * <p>All three call {@link #command(Command)}; there is no privileged path and
 * nothing a pilot can do that a key cannot.
 *
 * <h2>Tempo</h2>
 *
 * <p>The authored tempo is the schedule's own -- the same seconds every graded
 * capture runs at. Fast-forward is a multiplier on the director's clock
 * ({@link #FAST_FORWARD}), shipped from day one because a five-clause stanza
 * plus an enemy phrase is a long uninterruptible span every single turn and
 * combat-design.md 3d.3's open items all need a playable tempo to be judged at.
 * It scales <em>time</em>, not the schedule: every lag, settle and overlap keeps
 * its shape, exactly as a film keeps its cutting when played fast.
 */
public final class PlayScene implements Scene, SceneProbe, SceneEvents {

    /** Seconds of cloth and hair settling run before the fight opens. */
    private static final float SETTLE = 0.8f;

    /** Held-space clock multiple. Chosen by playing; see the pass report. */
    public static final float FAST_FORWARD = 3.0f;

    /** The persistent brisk toggle (F): quicker without abandoning the poem. */
    public static final float BRISK = 1.6f;

    /** How long the sheet takes to dry off once the fight is decided, in seconds. */
    public static final double EPILOGUE_DRY = 2.8;

    /** And how long after the last beat the drying waits. A held breath, not a cut. */
    public static final double EPILOGUE_HOLD = 0.9;

    private final String name;
    private final EncounterSpec spec;
    private final Plays.Pilot pilot;
    private final boolean bare;

    private Session session;
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
    private boolean brisk;

    public PlayScene(String name, EncounterSpec spec, Plays.Pilot pilot, boolean bare) {
        this.name = name;
        this.spec = spec;
        this.pilot = pilot;
        this.bare = bare;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return (pilot == null
                ? "a playable fight: digits bank, backspace un-banks, enter commits, "
                        + "T turns, H holds, space fast-forwards"
                : "a fight played by a deterministic pilot through the input loop")
                + (bare ? " -- CONTROL: no interface" : "");
    }

    @Override
    public void create(SceneContext ctx) {
        this.session = new Session(spec);
        this.director = new Director(session.schedule(), figures());
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

    /** Pale first, hero last, as {@code LaneScene}: the dark silhouette is never interrupted. */
    private List<Figure> figures() {
        List<Figure> out = new java.util.ArrayList<>();
        for (Bout.Staged.Body b : session.bodies()) {
            if (!b.hero()) {
                out.add(Figure.pale(b.id()).standAt(
                        Director.stretch(b.tile() * Stage.TILE_WIDTH), b.facing().step()));
            }
        }
        for (Bout.Staged.Body b : session.bodies()) {
            if (b.hero()) {
                out.add(Figure.dark(b.id()).standAt(
                        Director.stretch(b.tile() * Stage.TILE_WIDTH), b.facing().step()));
            }
        }
        return out;
    }

    // -- input -------------------------------------------------------------------

    /**
     * One command through the one gate. Returns whether it was accepted; on
     * acceptance the director continues the same performance under the extended
     * score.
     */
    public boolean command(Command cmd) {
        boolean applied = session.command(cmd, director.time());
        if (applied) {
            director = director.rescore(session.schedule());
        }
        return applied;
    }

    private void keyboard() {
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1 + i)) {
                command(Command.add(i));
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            command(Command.remove(0));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            command(Command.execute());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            command(Command.turnAround());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)
                || Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
            command(Command.hold());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            brisk = !brisk;
        }
    }

    private float tempo() {
        if (pilot != null) {
            // Scripted runs stay at the authored tempo so their captures are the
            // schedule's own seconds.
            return 1f;
        }
        if (Gdx.input != null && Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            return FAST_FORWARD;
        }
        return brisk ? BRISK : 1f;
    }

    // -- SceneEvents: the debug API plays through the same gate -------------------

    @Override
    public List<String> events() {
        return List.of("add", "remove", "execute", "hold", "turn");
    }

    @Override
    public boolean fire(String event, Map<String, String> args) {
        return switch (event) {
            case "add" -> command(Command.add(Integer.parseInt(args.getOrDefault("index", "0"))));
            case "remove" -> command(Command.remove(0));
            case "execute" -> command(Command.execute());
            case "hold" -> command(Command.hold());
            case "turn" -> command(Command.turnAround());
            default -> false;
        };
    }

    // -- the loop ------------------------------------------------------------------

    @Override
    public void update(float dt) {
        if (pilot != null && !session.outcome().over()
                && session.quietAt(director.time())) {
            Command cmd = pilot.decide(session.engine());
            if (cmd != null) {
                command(cmd);
            }
        } else if (pilot == null && Gdx.input != null) {
            keyboard();
        }
        director.advance(dt * tempo());
        aim();
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        sheet.setToOrtho2D(0f, 0f, width / (float) height, 1f);
        aim();
    }

    private void aim() {
        Framing f = director == null ? new Framing(0, Stage.INTIMACY_TILES) : director.framing();
        float w = (float) Director.stretchTiles(f.widthTiles());
        float h = w * height / (float) width;
        float eye = LaneScene.EYE_NEAR
                + (LaneScene.EYE_WIDE - LaneScene.EYE_NEAR) * (1f - intimacy());
        camera.setToOrtho(false, w, h);
        camera.position.set((float) Director.stretch(f.centreTile() * Stage.TILE_WIDTH),
                h * eye, 0f);
        camera.update();
    }

    private float intimacy() {
        return director == null ? 1f
                : (float) session.readout().intimacy(director.time());
    }

    private float parallax() {
        if (director == null) {
            return 0f;
        }
        Framing now = director.framing();
        Framing plan = session.stage().planning();
        double travel = (now.centreTile() - plan.centreTile())
                / Math.max(1.0, session.stage().laneLength());
        double p = -travel * LaneScene.PARALLAX_SHARE;
        return (float) Math.max(-LaneScene.PARALLAX_LIMIT,
                Math.min(LaneScene.PARALLAX_LIMIT, p));
    }

    /**
     * How much of the interface is still on the sheet once the fight is decided:
     * 1 while it is live, drying to 0 over {@link #EPILOGUE_DRY}.
     *
     * <p>This is the resolution the brief asks defeat and victory to have. No
     * card, no text: the fight ends, the last beat settles, and the marks that
     * carried the fight dry off the sheet the way every mark leaves (STYLE.md 8),
     * leaving the world -- on defeat, a world with no figure in it. A held breath
     * and a page, which is what "the Pilgrim's ink ran out" should look like.
     */
    private float presence(double t) {
        if (!session.outcome().over() || Double.isNaN(session.endedAt())) {
            return 1f;
        }
        double u = (t - session.endedAt() - EPILOGUE_HOLD) / EPILOGUE_DRY;
        return 1f - (float) Math.max(0.0, Math.min(1.0, u));
    }

    @Override
    public void render() {
        float t = (float) director.time();
        Look look = session.readout().at(t);
        float presence = presence(t);

        paper.render(camera.combined, t);

        float haze = (float) session.readout().haze(t);
        float air = LaneScene.FOG_FLOOR + (1f - LaneScene.FOG_FLOOR) * haze;
        ui.begin(camera.combined);
        Fog.bank(ui, Fog.Layer.FAR, t, camera.position.x, camera.position.y,
                camera.viewportWidth, camera.viewportHeight, air);
        ui.end();

        if (!bare && presence > 0.005f) {
            Brush.Sink sink = presence >= 0.999f ? ui : new Dried(ui, presence);
            ui.begin(camera.combined);
            LaneInterface.ground(sink, look, Director.stretchTiles(Stage.TILE_WIDTH),
                    camera.viewportHeight);
            for (Look.Shadow s : look.shadows()) {
                for (Figure f : director.figures()) {
                    if (f.body() == s.body()) {
                        LaneInterface.shadow(sink, s, (float) f.standX(),
                                camera.viewportHeight, look.intimacy());
                    }
                }
            }
            ui.end();

            ui.begin(sheet);
            LaneInterface.sheet(sink, look, width / (float) height, parallax());
            ui.end();
        }

        renderer.haze(haze * LaneScene.SUBJECT_HAZE);
        hair.haze(haze * LaneScene.SUBJECT_HAZE);

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

        ui.begin(camera.combined);
        Fog.bank(ui, Fog.Layer.NEAR, t, camera.position.x, camera.position.y,
                camera.viewportWidth, camera.viewportHeight, air);
        ui.end();

        paper.renderOverlay(camera.combined, t);
        Opaque.seal();
    }

    /**
     * A sink whose ink is drying out: every mark, at a fraction of its alpha.
     * The epilogue's whole mechanism -- the geometry is untouched, so nothing
     * moves while it goes; it only fades, which is how STYLE.md 8 says marks
     * leave.
     */
    private record Dried(Brush.Sink under, float k) implements Brush.Sink {
        @Override
        public int vertex(float x, float y, Color ink, float alpha) {
            return under.vertex(x, y, ink, alpha * k);
        }

        @Override
        public void triangle(int a, int b, int c) {
            under.triangle(a, b, c);
        }
    }

    @Override
    public float duration() {
        return 60f;
    }

    @Override
    public Map<String, float[]> probe() {
        Map<String, float[]> out = new LinkedHashMap<>();
        double t = director.time();
        Look look = session.readout().at(t);
        out.put("time", new float[] {(float) t, (float) session.busyUntil()});
        out.put("health", new float[] {look.health(), look.maxHealth()});
        out.put("stanza", new float[] {look.stanza().size()});
        out.put("outcome", new float[] {session.outcome().ordinal()});
        for (Look.Shadow s : look.shadows()) {
            out.put("shadow" + s.body(),
                    new float[] {s.hp(), s.maxHp(), (float) s.dying()});
        }
        for (Figure f : director.figures()) {
            out.put("body" + f.body(), new float[] {(float) f.standX()});
        }
        out.put("intimacy", new float[] {intimacy(), (float) director.framing().widthTiles()});
        return out;
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
