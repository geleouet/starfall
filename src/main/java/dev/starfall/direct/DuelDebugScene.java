package dev.starfall.direct;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import dev.starfall.anim.SkinnedMesh;
import dev.starfall.art.Palette;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneClock;
import dev.starfall.capture.SceneContext;
import dev.starfall.ik.IkChain;
import dev.starfall.sim.ClothSim;
import dev.starfall.sim.HairSim;
import dev.starfall.stage.Chain;
import dev.starfall.stage.Directive;
import dev.starfall.stage.Framing;
import dev.starfall.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug sibling of {@link DuelScene}: the same schedule, the same clock, the same
 * framing, drawn as a diagram.
 *
 * <p>It exists for the reason System 2's debt (E5) records and System 3's pass-2
 * review repeated as a capture-hygiene defect -- "no {@code -debug} siblings for
 * any graded window, so the reviewer shot its own in order to say anything about
 * the chain at all". The graded capture cannot show an IK target, a contact
 * instant or a weight, and with two figures on screen the questions that matter
 * are all about those:
 *
 * <ul>
 *   <li><b>Do the two skeletons agree on the contact point?</b> Both bodies' sword
 *       targets are drawn, and the contact instants are marked on a strip along the
 *       bottom, so a frame can be read against {@code Meeting} directly.</li>
 *   <li><b>Is the trunk actually driving?</b> Each chain's weight is drawn as a bar,
 *       so "the carrier never lets go" -- the property that makes a phrase a phrase
 *       -- is visible per frame rather than only assertable in a test.</li>
 *   <li><b>Where is the pelvis, versus where the schedule asked for it?</b> Both are
 *       drawn, and the gap between them is the lean.</li>
 * </ul>
 *
 * <p>{@link ShapeRenderer} only, matching {@code SimDebugScene} and
 * {@code IkDebugScene}: no shader, so a debug capture can never be blamed on the
 * material.
 */
public class DuelDebugScene implements Scene {

    private static final int FLOATS = SkinnedMesh.FLOATS_PER_VERTEX;

    private static final float SETTLE = 0.8f;

    private final Duel.Kind kind;
    private final String name;

    private ShapeRenderer shapes;
    private Duel.Staged staged;
    private Director director;
    private int width = 960;
    private int height = 720;

    private final List<Ghost> ghosts = new ArrayList<>();
    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();
    private final com.badlogic.gdx.math.Matrix4 proj = new com.badlogic.gdx.math.Matrix4();

    /** A CPU-skinned copy of one figure's mesh, for the ghosted fill. */
    private static final class Ghost {
        final Figure figure;
        final float[] verts;
        final short[] indices;
        final float[] mats;
        final float[] x;
        final float[] y;

        Ghost(Figure figure) {
            this.figure = figure;
            Mesh mesh = figure.rig().mesh().mesh();
            this.verts = new float[figure.rig().mesh().vertexCount() * FLOATS];
            mesh.getVertices(verts);
            this.indices = new short[mesh.getNumIndices()];
            mesh.getIndices(indices);
            this.mats = new float[figure.skeleton().boneCount() * 16];
            this.x = new float[figure.rig().mesh().vertexCount()];
            this.y = new float[figure.rig().mesh().vertexCount()];
        }

        void skin() {
            figure.skeleton().fillSkinningMatrices(mats);
            for (int v = 0; v < x.length; v++) {
                int o = v * FLOATS;
                float px = verts[o];
                float py = verts[o + 1];
                float sx = 0f;
                float sy = 0f;
                for (int slot = 0; slot < 4; slot++) {
                    int wo = o + 9 + slot * 2;
                    int bone = (int) verts[wo];
                    float w = verts[wo + 1];
                    if (w <= 0f) {
                        continue;
                    }
                    int m = bone * 16;
                    sx += w * (mats[m] * px + mats[m + 4] * py + mats[m + 12]);
                    sy += w * (mats[m + 1] * px + mats[m + 5] * py + mats[m + 13]);
                }
                x[v] = sx;
                y[v] = sy;
            }
        }
    }

    public DuelDebugScene(Duel.Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return "Debug: " + kind.description();
    }

    @Override
    public void create(SceneContext ctx) {
        this.width = ctx.width;
        this.height = ctx.height;
        this.shapes = new ShapeRenderer();
        this.shapes.setAutoShapeType(true);
        this.staged = Duel.of(kind);
        this.director = new Director(staged.schedule(), Duel.figuresFor(staged));
        for (Figure f : director.figures()) {
            f.sim().wind(Director.BREEZE_X, Director.BREEZE_Y);
            ghosts.add(new Ghost(f));
        }
        director.start();
        int steps = Math.round(SETTLE / SceneClock.SUBSTEP);
        for (int i = 0; i < steps; i++) {
            for (Figure f : director.figures()) {
                f.simulate(SceneClock.SUBSTEP, 0f);
            }
        }
        for (Ghost g : ghosts) {
            g.skin();
        }
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void update(float dt) {
        director.advance(dt);
        for (Ghost g : ghosts) {
            g.skin();
        }
    }

    @Override
    public void render() {
        Framing f = director.framing();
        float halfW = (float) Director.stretchTiles(f.widthTiles()) * 0.5f;
        float halfH = halfW * height / (float) width;
        float cx = (float) Director.stretch(f.centreTile() * Stage.TILE_WIDTH);
        float cy = halfH * 0.88f;
        // setProjectionMatrix rather than mutating getProjectionMatrix() in place.
        // ShapeRenderer only recomputes its combined matrix when a setter marks it
        // dirty, so mutating the matrix it hands out leaves every frame after the
        // first drawn through the first frame's camera. The System 2 and System 3
        // debug scenes do mutate it and are correct anyway, because their cameras
        // never move; this is the first scene in the project whose does.
        proj.setToOrtho(cx - halfW, cx + halfW, cy - halfH, cy + halfH, -1f, 1f);
        shapes.setProjectionMatrix(proj);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Palette.PAPER_WARM);
        shapes.rect(cx - halfW, cy - halfH, halfW * 2f, halfH * 2f);

        // The ground line, so a foot that leaves it is visible.
        shapes.setColor(Palette.alpha(Palette.INK_SLATE, 0.35f));
        shapes.rectLine(cx - halfW, 0f, cx + halfW, 0f, halfH * 0.004f);
        // Tile boundaries: the whole engine is ordinal in these, so a body that
        // ends up between two of them is a staging bug rather than a rig one.
        for (int tile = 0; tile <= staged.schedule().stage().laneLength(); tile++) {
            float tx = (float) (Director.stretch(tile * Stage.TILE_WIDTH)
                    - 0.5 * Stage.TILE_WIDTH * Director.LANE_SPREAD);
            shapes.rectLine(tx, 0f, tx, halfH * 0.10f, halfH * 0.003f);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Ghost g : ghosts) {
            boolean hero = g.figure.body() == staged.hero();
            shapes.setColor(Palette.alpha(hero ? Palette.INK_INDIGO : Palette.INK_SLATE, 0.13f));
            for (int i = 0; i + 2 < g.indices.length; i += 3) {
                int p = g.indices[i];
                int q = g.indices[i + 1];
                int r = g.indices[i + 2];
                shapes.triangle(g.x[p], g.y[p], g.x[q], g.y[q], g.x[r], g.y[r]);
            }
        }

        for (Figure fig : director.figures()) {
            drawFigure(fig, halfH);
        }
        drawScore(cx, halfW, halfH, cy);
        shapes.end();
        Opaque.seal();
    }

    private void drawFigure(Figure fig, float halfH) {
        float dot = halfH * 0.012f;
        ClothSim cloth = fig.sim().cloth();
        for (int i = 0; i < cloth.chainCount(); i++) {
            ClothSim.Chain c = cloth.chain(i);
            shapes.setColor(Palette.OCHRE);
            for (int p = 0; p + 1 < c.particleCount(); p++) {
                shapes.rectLine(c.x(p), c.y(p), c.x(p + 1), c.y(p + 1), dot * 0.7f);
            }
            shapes.setColor(Palette.VERMILLION);
            for (int p = 0; p < c.particleCount(); p++) {
                shapes.circle(c.x(p), c.y(p), dot, 12);
            }
        }

        HairSim hair = fig.sim().hair();
        for (int i = 0; i < hair.strandCount(); i++) {
            HairSim.Strand st = hair.strand(i);
            shapes.setColor(st.escapee ? Palette.MOTE_MAGENTA : Palette.INK_SLATE);
            for (int p = 0; p + 1 < st.particleCount(); p++) {
                shapes.rectLine(st.x(p), st.y(p), st.x(p + 1), st.y(p + 1), dot * 0.28f);
            }
        }
        shapes.setColor(Palette.alpha(Palette.INK_SLATE, 0.4f));
        for (int i = 0; i < hair.colliderCount(); i++) {
            ring(hair.colliderX(i), hair.colliderY(i), hair.colliderRadius(i));
        }

        // Every IK target, with the chain's weight as the marker's size: a target
        // a chain is not driving toward is the commonest way an IK scene lies.
        target(fig, Chain.SWORD_ARM, fig.ik().swordArm(), Palette.MOTE_CYAN, halfH);
        target(fig, Chain.SPINE, fig.ik().spine(), Palette.EMBER, halfH);
        target(fig, Chain.LEG_LEAD, fig.ik().legL(), Palette.OCHRE_PALE, halfH);
        target(fig, Chain.LEG_TRAIL, fig.ik().legR(), Palette.OCHRE_PALE, halfH);

        // Where the pelvis actually is. The gap between this and the trunk target
        // above it is the lean, which is the whole of STYLE.md 7.0.1 on this rig.
        fig.where("hips", a);
        shapes.setColor(Palette.VERMILLION);
        shapes.circle(a.x, a.y, dot * 1.7f, 14);

        // Wind on each region, drawn from the head: the only input to the sim that
        // is not visible in the geometry, and the one STYLE.md 7.3 says impact is
        // expressed through.
        fig.where("head", b);
        shapes.setColor(Palette.EMBER);
        shapes.rectLine(b.x, b.y + 0.34f, b.x + fig.sim().windX() * 0.06f,
                b.y + 0.34f + fig.sim().windY() * 0.06f, dot);
    }

    private void target(Figure fig, Chain chain, IkChain ik, com.badlogic.gdx.graphics.Color colour,
                        float halfH) {
        float w = ik.weight();
        shapes.setColor(Palette.alpha(colour, 0.35f + 0.65f * w));
        float r = halfH * (0.006f + 0.022f * w);
        shapes.circle(ik.targetX(), ik.targetY(), r, 14);
        // A line from the effector to the target: its length is the residual, and a
        // residual that never closes is a chain reaching for something it cannot
        // have -- which is exactly the failure both seams in Director are about.
        ik.endEffector(a);
        shapes.rectLine(a.x, a.y, ik.targetX(), ik.targetY(), halfH * 0.0025f);
    }

    /**
     * The score along the bottom of the frame: every contact instant, and the
     * playhead.
     *
     * <p>The contact-ordering guarantee is the one property three layers have now
     * each promised to keep, and this is the only place it is <em>visible</em>: two
     * marks that ever coincided would be one mark.
     */
    private void drawScore(float cx, float halfW, float halfH, float cy) {
        double duration = Math.max(1e-6, staged.schedule().duration());
        float y = cy - halfH * 0.94f;
        float left = cx - halfW * 0.92f;
        float span = halfW * 1.84f;
        shapes.setColor(Palette.alpha(Palette.INK_SLATE, 0.30f));
        shapes.rectLine(left, y, left + span, y, halfH * 0.004f);

        shapes.setColor(Palette.VERMILLION);
        for (double c : staged.schedule().contacts()) {
            float x = left + span * (float) (c / duration);
            shapes.rectLine(x, y - halfH * 0.022f, x, y + halfH * 0.022f, halfH * 0.005f);
        }
        // The held breaths, which are the only thing in the game that changes the
        // rate of this axis.
        shapes.setColor(Palette.alpha(Palette.MOTE_CYAN, 0.55f));
        for (Directive.TimeRamp r : staged.schedule().of(Directive.TimeRamp.class)) {
            float x0 = left + span * (float) (r.at() / duration);
            float x1 = left + span * (float) (r.end() / duration);
            shapes.rectLine(x0, y - halfH * 0.010f, x1, y - halfH * 0.010f, halfH * 0.010f);
        }
        shapes.setColor(Palette.INK_BLACK);
        float p = left + span * (float) (director.time() / duration);
        shapes.rectLine(p, y - halfH * 0.034f, p, y + halfH * 0.034f, halfH * 0.006f);
    }

    private void ring(float x, float y, float r) {
        int n = 24;
        float inner = r * 0.94f;
        for (int i = 0; i < n; i++) {
            double t0 = 2 * Math.PI * i / n;
            double t1 = 2 * Math.PI * (i + 1) / n;
            float c0 = (float) Math.cos(t0);
            float s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1);
            float s1 = (float) Math.sin(t1);
            shapes.triangle(x + inner * c0, y + inner * s0, x + r * c0, y + r * s0, x + r * c1, y + r * s1);
            shapes.triangle(x + inner * c0, y + inner * s0, x + r * c1, y + r * s1,
                    x + inner * c1, y + inner * s1);
        }
    }

    @Override
    public float duration() {
        return (float) Duel.of(kind).schedule().duration();
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
        if (director != null) {
            for (Figure f : director.figures()) {
                f.rig().mesh().dispose();
                f.rig().bladeMesh().dispose();
            }
        }
    }
}
