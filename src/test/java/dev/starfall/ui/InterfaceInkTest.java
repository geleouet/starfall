package dev.starfall.ui;

import dev.starfall.combat.TileType;
import dev.starfall.stage.Stage;
import dev.starfall.stage.Timing;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interface as a material: no hard edges, no glow, and vermillion kept to its
 * budget -- measured over every frame of every bout rather than on a chosen one.
 */
class InterfaceInkTest {

    /** One sample every 1/60 s, which is the rate STYLE.md 11.2 grades timing at. */
    private static final double STEP = 1.0 / 60.0;

    /**
     * And the rate the rasterised guard sweeps at, which is coarser and says so.
     *
     * <p>Rasterising the whole interface at 960x720 is three orders of magnitude
     * dearer than reading its vertices, so this one samples at 20 Hz rather than 60.
     * That is honest about what it covers: every distinct <em>state</em> of the
     * interface lasts far longer than 50 ms -- the shortest thing in it is a
     * Strikethrough's 0.60 s wick -- but a defect confined to a single frame could
     * hide here where the two vertex guards, which do run at 60 Hz, could not.
     */
    private static final double RASTER_STEP = 0.05;

    /** Pixels of frame border the raster guard ignores. */
    private static final int BORDER_MARGIN = 8;

    /**
     * The share of its own amplitude the interface may deliver in one pixel.
     *
     * <p>Derived, not chosen: the bordered panel of {@code Guards.borderedPanel}
     * reads 1.00 through the same raster, so this is <b>a third of what a hard edge
     * prints</b> -- a mark that takes at least three pixels to arrive. Delivered, the
     * worst over all three bouts is 0.299.
     */
    private static final float EDGE_CEILING = 0.34f;

    private static Recorder draw(Bout.Staged staged, double t) {
        Recorder rec = new Recorder();
        Look look = staged.readout().at(t);
        double spread = 1.55 * Stage.TILE_WIDTH;
        double frameHeight = staged.schedule().framingAt(t).widthTiles() * 1.55 * 0.75;
        LaneInterface.ground(rec, look, spread, frameHeight);
        LaneInterface.sheet(rec, look, 4f / 3f, 0f);
        return rec;
    }

    /**
     * <b>Guard, and its scope is now stated in what it tests.</b> Every triangle the
     * interface emits has at least one vertex at exactly zero alpha.
     *
     * <p><b>What this is not.</b> Pass 1's version of this docstring claimed that
     * <i>"a panel, a bar, a border, a rounded rectangle or an icon in a frame all
     * fail it on the first triangle, whatever they are shaded with"</i>, and the
     * pass-1 review destroyed that sentence: a triangle of the form {@code (0, a, a)}
     * satisfies this assertion and prints a hard edge along its {@code a}-{@code a}
     * side, and a filled bordered panel cut as {@code (TL,TR,BR)} + {@code (TL,BR,BL)}
     * with one corner at zero passed this test and the whole suite. The claim was
     * about every way of printing an edge; the assertion covered one.
     *
     * <p>So the sentence is gone and the claim is narrowed to exactly what is
     * checked: <b>no triangle here is a flat fill</b>. That is a true and useful
     * invariant of {@link Brush} -- it is what makes every mark have a soft side --
     * and it is not a hard-edge certificate. The hard-edge certificate is
     * {@link #noMarkPutsInkOnASilhouetteEdge()} and
     * {@link #noMarkPrintsAnEdgeInTheRasterItWouldDraw()}, and both are pointed at
     * the reviewer's own panel in {@link #theForbiddenThingIsCaughtByTheGuardsThatForbidIt()}.
     *
     * <p>Swept over all three bouts, so it covers every state the interface has
     * rather than a frame someone chose.
     */
    @Test
    void noTriangleInTheInterfaceIsAFlatFill() {
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            double end = staged.schedule().duration();
            for (double t = 0; t <= end; t += STEP * 6) {
                Recorder rec = draw(staged, t);
                for (Recorder.Triangle tri : rec.triangles) {
                    float a = rec.vertices.get(tri.a()).alpha();
                    float b = rec.vertices.get(tri.b()).alpha();
                    float c = rec.vertices.get(tri.c()).alpha();
                    assertTrue(a == 0f || b == 0f || c == 0f, String.format(Locale.ROOT,
                            "%s at t=%.3f emits a triangle whose three corners carry ink "
                                    + "(%.3f, %.3f, %.3f) at (%.4f,%.4f)-(%.4f,%.4f)-(%.4f,%.4f). "
                                    + "STYLE.md 3: nothing has a hard edge except the blades, and "
                                    + "a filled triangle prints one.",
                            kind, t, a, b, c,
                            rec.vertices.get(tri.a()).x(), rec.vertices.get(tri.a()).y(),
                            rec.vertices.get(tri.b()).x(), rec.vertices.get(tri.b()).y(),
                            rec.vertices.get(tri.c()).x(), rec.vertices.get(tri.c()).y()));
                }
            }
        }
    }

    /**
     * <b>Guard.</b> STYLE.md 3, in the form the rule is actually written in:
     * <i>nothing in this game has a hard edge except the blades.</i>
     *
     * <p>An edge is a boundary of the drawn region that carries ink on it. So the
     * property is: <b>no edge of the emitted mesh that belongs to a single triangle
     * has ink at both of its ends.</b> Interior edges may carry all the ink they
     * like -- they are inside a mark, and a mark is allowed to be dark in the middle.
     * It is the outline that must be on paper.
     *
     * <p>This is what makes {@link Brush} edgeless and pass 1's assertion never said
     * it: a stroke's two rims sit on zero alpha either side of the spine, so the only
     * inked edge it emits is the spine chain, and every spine edge is shared by the
     * triangle on its left and the one on its right. A wash is a fan whose rim is on
     * zero. Neither can present an inked outline; a panel, a bar, a border or an icon
     * in a frame present nothing else.
     *
     * <p>Swept over every state of every bout, and pointed at the forbidden thing in
     * {@link #theForbiddenThingIsCaughtByTheGuardsThatForbidIt()}.
     */
    @Test
    void noMarkPutsInkOnASilhouetteEdge() {
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            double end = staged.schedule().duration();
            for (double t = 0; t <= end; t += STEP * 6) {
                List<String> bad = Guards.inkedSilhouetteEdges(draw(staged, t));
                assertTrue(bad.isEmpty(), String.format(Locale.ROOT,
                        "%s at t=%.3f emits %d edge(s) on the outline of a mark with ink at "
                                + "both ends, the first being %s. STYLE.md 3: nothing has a hard "
                                + "edge except the blades, and an inked silhouette is one.",
                        kind, t, bad.size(), bad.isEmpty() ? "-" : bad.get(0)));
            }
        }
    }

    /**
     * <b>Guard.</b> The same rule, measured where an edge exists: on the raster.
     *
     * <p>The topology guard above is a statement about a mesh, and a mesh can be
     * cheated -- draw every triangle twice and every boundary edge becomes interior.
     * This one cannot be cheated by construction, because it measures the picture:
     * {@link Raster} composites the emitted triangles at the shipped resolution with
     * the renderer's own premultiplied over, and the statistic is the pass-1 review's
     * own -- <b>the steepest single-pixel step, as a fraction of the field's own
     * peak.</b>
     *
     * <h2>Where the ceiling comes from</h2>
     *
     * <h2>The denominator is the interface at its strongest, not at that instant</h2>
     *
     * <p>The interface recedes with the push-in by design, so at the intimate framing
     * the whole field is at a third of the amplitude it carries wide. Dividing each
     * frame's step by <em>that frame's own</em> peak asks a receding mark to get
     * softer as it gets fainter, which is not what STYLE.md 3 says and not what a
     * viewer sees: a step of 0.09 coverage over a dusk sky is a small change in the
     * delivered picture whether or not the rest of the interface has faded with it.
     * So every step in a bout is measured against the largest amplitude that bout's
     * interface ever reaches, which is its planning framing.
     *
     * <p>Not chosen. {@value #EDGE_CEILING} of the amplitude is a fraction of what the
     * <em>forbidden thing</em> reads through this same instrument: the reviewer's
     * bordered panel delivers <b>1.00</b> -- its entire amplitude in one pixel -- and
     * {@link #theForbiddenThingIsCaughtByTheGuardsThatForbidIt()} measures that rather
     * than assuming it. STYLE.md 11.2b(g): <i>"before a number is allowed to decide
     * anything, someone must say what it would read if the thing being measured were
     * absent -- and then produce that case."</i> The case is produced. Delivered, the
     * interface's worst step over all three bouts is <b>0.299</b>, so the mark that
     * arrives most abruptly still takes more than three pixels to arrive where a hard
     * edge takes one.
     *
     * <p>The margin at the frame border is excluded, for the reason the pass-1 review
     * gave when it found the same thing in delivered pixels: a lane pool reaching the
     * viewport is <i>"a mark cut by the viewport, which the ground and sky also are;
     * it is not a printed boundary"</i>.
     *
     * <p>This measures <b>coverage</b>, not luminance, and is therefore stricter than
     * the delivered measurement in {@code docs/system5-debt.md}: a dark pigment cannot
     * hide a step behind a small luminance difference.
     */
    @Test
    void noMarkPrintsAnEdgeInTheRasterItWouldDraw() {
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            double end = staged.schedule().duration();
            List<double[]> samples = new ArrayList<>();
            float amplitude = 0f;
            for (double t = 0; t <= end; t += RASTER_STEP) {
                Raster field = Guards.interfaceField(staged, t,
                        Guards.SHIPPED_WIDTH, Guards.SHIPPED_HEIGHTS[0]);
                amplitude = Math.max(amplitude, field.peak());
                Raster.Step worst = field.steepestStep(BORDER_MARGIN);
                samples.add(new double[] {t, worst.size(), worst.x(), worst.y()});
            }
            assertTrue(amplitude > 0.1f, kind + " draws no interface at all");
            for (double[] s : samples) {
                double share = s[1] / amplitude;
                assertTrue(share <= EDGE_CEILING, String.format(Locale.ROOT,
                        "%s at t=%.3f: the interface's steepest one-pixel step is %.4f, which is "
                                + "%.4f of the %.4f amplitude the interface reaches at its "
                                + "strongest, at (%d,%d) of %dx%d, against a ceiling of %.2f -- "
                                + "and a filled panel reads 1.00 through this same raster. "
                                + "STYLE.md 3: nothing has a hard edge except the blades.",
                        kind, s[0], s[1], share, amplitude, (int) s[2], (int) s[3],
                        Guards.SHIPPED_WIDTH, Guards.SHIPPED_HEIGHTS[0], EDGE_CEILING));
            }
        }
    }

    /**
     * <b>The second exhibit STYLE.md 11.2b(f) asks for, checked in.</b>
     *
     * <p><i>"Try to build the thing the guard forbids while satisfying it. If you
     * succeed, the guard's scope is the finding and the name is a lie."</i> The thing
     * is {@link Guards#borderedPanel}, which is the pass-1 reviewer's construction
     * verbatim. This test asserts three things about it, in the order they matter:
     *
     * <ol>
     *   <li>it <b>passes</b> the flat-fill invariant -- every triangle has a zero
     *       corner -- which is the whole finding of the pass-1 review, preserved so
     *       that nobody re-derives it;</li>
     *   <li>it is <b>caught</b> by the silhouette guard, with the offending edges
     *       named;</li>
     *   <li>it is <b>caught</b> by the raster guard, and the number it reads there is
     *       the control the raster guard's ceiling is a fraction of.</li>
     * </ol>
     *
     * <p>A guard that stops catching this stops compiling a green suite.
     */
    @Test
    void theForbiddenThingIsCaughtByTheGuardsThatForbidIt() {
        Recorder rec = new Recorder();
        Guards.borderedPanel(rec);

        for (Recorder.Triangle tri : rec.triangles) {
            float a = rec.vertices.get(tri.a()).alpha();
            float b = rec.vertices.get(tri.b()).alpha();
            float c = rec.vertices.get(tri.c()).alpha();
            assertTrue(a == 0f || b == 0f || c == 0f,
                    "the exhibit must satisfy the flat-fill invariant, or it is not the "
                            + "counter-example the pass-1 review found");
        }

        List<String> bad = Guards.inkedSilhouetteEdges(rec);
        assertEquals(2, bad.size(),
                "a filled rectangle with one alpha-zero corner has exactly two inked "
                        + "silhouette edges; the silhouette guard reported " + bad);

        Raster panel = new Raster(Guards.SHIPPED_WIDTH, Guards.SHIPPED_HEIGHTS[0]);
        panel.paint(rec, Guards.SHIPPED_HEIGHTS[0], 0f, 0f);
        float share = panel.steepestStep(BORDER_MARGIN).size() / panel.peak();
        assertTrue(share > EDGE_CEILING * 2.0f, String.format(Locale.ROOT,
                "the panel reads %.4f of its amplitude in one pixel through the raster the "
                        + "guard uses, which must be far above the guard's ceiling of %.2f or "
                        + "the ceiling is not derived from anything",
                share, EDGE_CEILING));
        System.out.printf(Locale.ROOT,
                "CONTROL bordered panel: steepest 1-px step = %.4f of amplitude "
                        + "(%dx%d, box x0.60..1.00 y0.60..0.90 frame heights); "
                        + "interface ceiling %.2f%n",
                share, Guards.SHIPPED_WIDTH, Guards.SHIPPED_HEIGHTS[0], EDGE_CEILING);

        // And the attack that beats the silhouette guard on its own, kept here so that
        // deleting the raster guard cannot look safe. Drawing every triangle twice
        // makes every boundary edge appear an even number of times, so the topology
        // sees a closed mesh with no outline at all -- and the picture is unchanged.
        Recorder twice = new Recorder();
        Guards.borderedPanel(twice);
        int n = twice.triangles.size();
        for (int i = 0; i < n; i++) {
            twice.triangles.add(twice.triangles.get(i));
        }
        assertTrue(Guards.inkedSilhouetteEdges(twice).isEmpty(),
                "the duplication attack is supposed to defeat the silhouette guard; if it no "
                        + "longer does, this note and the debt entry beside it are stale");
        Raster doubled = new Raster(Guards.SHIPPED_WIDTH, Guards.SHIPPED_HEIGHTS[0]);
        doubled.paint(twice, Guards.SHIPPED_HEIGHTS[0], 0f, 0f);
        assertTrue(doubled.steepestStep(BORDER_MARGIN).size() / doubled.peak() > EDGE_CEILING,
                "a panel drawn twice defeats the topology guard, so the raster guard is the "
                        + "only thing catching it and it must");
    }

    /**
     * <b>Guard.</b> A mark that is counted is countable at every resolution the game
     * ships.
     *
     * <p>The pass-1 review refused the debt's prescription -- compressing the cooldown
     * scale is a mechanic, and <i>"a rendering constraint at one resolution is not a
     * reason to change a mechanic"</i> -- and left this obligation in its place:
     * <b>"a mark that is counted must be countable at every resolution the game ships,
     * or it must stop being a count."</b> Two runs of marks are counts: the charge
     * ticks beside a held tile, and the health strokes at the head of the stanza.
     *
     * <p>Read the way a player reads: the runs of ink across the densest row of the
     * band, at a floor a tenth of the run's own peak, against the control of the same
     * sheet with no charges and no health on it. Every cooldown the engine can carry
     * (0 to 8) at every charge count inside it, and every tally from empty to full at
     * three different maxima, at every entry of {@code Guards.SHIPPED_HEIGHTS}.
     *
     * <p>Observed red on the pass-1 geometry, which delivered <b>2 runs for a cooldown
     * of 3 at one charge, and 3 for a cooldown of 4 at two charges</b> -- the misread
     * the review names as worse than an omission, because a player counts a different
     * number rather than noticing a missing one.
     */
    @Test
    void everyCountedMarkIsCountableAtEveryShippedResolution() {
        for (int h : Guards.SHIPPED_HEIGHTS) {
            int w = Math.round(h * 4f / 3f);
            for (int cooldown = 0; cooldown <= 8; cooldown++) {
                for (int charges = 0; charges <= cooldown; charges++) {
                    int runs = Guards.tickRuns(TileType.CUT, charges, cooldown, w, h).size();
                    assertEquals(cooldown, runs, String.format(Locale.ROOT,
                            "at %dx%d a tile at %d charges of %d draws %d separable marks. "
                                    + "A player counts what is separable, so this is not a "
                                    + "missing tick, it is a wrong number -- on the resource "
                                    + "that decides whether a tile can be banked.",
                            w, h, charges, cooldown, runs));
                }
            }
            for (int max : new int[] {8, 10, 12}) {
                for (int hp = 0; hp <= max; hp++) {
                    int runs = Guards.healthRuns(hp, max, w, h).size();
                    assertEquals(max, runs, String.format(Locale.ROOT,
                            "at %dx%d a Night Pilgrim at %d of %d draws %d separable strokes. "
                                    + "The lost ones stay as ghosts so the row says how much "
                                    + "there ever was, and a ghost the reader cannot separate "
                                    + "says a smaller number instead.",
                            w, h, hp, max, runs));
                }
            }
        }
    }

    /**
     * <b>Guard.</b> The thing the player composes with out-reads the thing they check.
     *
     * <p>The pass-1 review measured the health row at a peak lift of <b>+99.6</b> over
     * its own ground and the freshest mark in the stanza at <b>+94.8</b>, and named
     * the fault: <i>"the loudest thing in the margin is the one element the player is
     * not composing. Health is a state you check; the stanza is the thing you are
     * building, and it should out-read it."</i>
     *
     * <p>Measured on the sheet the renderer would draw, through the two boxes named
     * in {@code docs/system5-debt.md} 0, with a full stanza and full health so that
     * neither is being flattered.
     */
    @Test
    void theStanzaOutReadsTheHealthRow() {
        Bout.Staged staged = Bout.of(Bout.Kind.FOLD);
        int h = Guards.SHIPPED_HEIGHTS[0];
        int w = Guards.SHIPPED_WIDTH;
        Look look = staged.readout().at(1.0);
        assertTrue(look.stanza().size() >= 1, "the planning shot must have a written stanza");
        Raster field = Guards.sheetField(look, w, h);
        float stanza = box(field, LaneInterface.COLUMN_X, LaneInterface.STANZA_GLYPH * 0.6f,
                LaneInterface.STANZA_BASE_Y + look.stanza().get(0).height() * LaneInterface.STANZA_PITCH,
                LaneInterface.STANZA_GLYPH * 0.6f, h);
        float health = box(field, LaneInterface.HEALTH_X
                        + LaneInterface.HEALTH_PITCH * (look.maxHealth() - 1) * 0.5f,
                LaneInterface.HEALTH_PITCH * look.maxHealth() * 0.6f,
                LaneInterface.HEALTH_Y, 0.03f, h);
        assertTrue(stanza > health, String.format(Locale.ROOT,
                "the freshest mark in the stanza peaks at %.4f and the health row at %.4f "
                        + "(sheet %dx%d, stanza box centred on x=%.3f y=%.3f, health row "
                        + "x=%.3f y=%.3f, frame heights). The margin's loudest mark must be "
                        + "the one the player is composing with.",
                stanza, health, w, h, LaneInterface.COLUMN_X,
                LaneInterface.STANZA_BASE_Y + look.stanza().get(0).height() * LaneInterface.STANZA_PITCH,
                LaneInterface.HEALTH_X, LaneInterface.HEALTH_Y));
    }

    /** Peak coverage inside a box given in frame heights. */
    private static float box(Raster r, float cx, float halfW, float cy, float halfH, int h) {
        int x0 = Math.max(0, Math.round((cx - halfW) * h));
        int x1 = Math.min(r.width - 1, Math.round((cx + halfW) * h));
        int y0 = Math.max(0, Math.round((1f - cy - halfH) * h));
        int y1 = Math.min(r.height - 1, Math.round((1f - cy + halfH) * h));
        float peak = 0f;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                peak = Math.max(peak, r.alpha[y * r.width + x]);
            }
        }
        return peak;
    }

    /**
     * <b>Guard.</b> STYLE.md 8: vermillion is spent on the Strikethrough and on
     * nothing else, and STYLE.md 2.2 requires it to "always mean something".
     *
     * <p>Both halves are statements about <em>meaning</em>, so both are checked
     * against the engine's own {@code threatenedTiles()} rather than against the
     * numbers the interface used to draw them. A frame with nothing threatened must
     * carry no vermillion at all, and every vermillion vertex in a frame that does
     * must lie over a tile the engine says is threatened -- within half a tile of its
     * centre, since a Strikethrough is a stroke through one tile and not a wash over
     * a region.
     *
     * <p>An earlier version of this test bounded vermillion as a <em>share of the
     * interface's own ink</em> and it was the wrong quantity: on a five-tile lane
     * with two tiles threatened it read 17.7%, which is not a budget violation but
     * an accurate report that two fifths of a knife fight is under a blade. The
     * budget is a count of marks and where they are, not a proportion of a total
     * that changes with lane length. The delivered pixel share is measured on the
     * captures instead and recorded in {@code docs/system5-debt.md}.
     */
    @Test
    void vermillionIsSpentOnlyOnTheTilesTheEngineSaysAreThreatened() {
        double spread = 1.55 * Stage.TILE_WIDTH;
        int frames = 0;
        int withThreat = 0;
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            double end = staged.schedule().duration();
            for (double t = 0; t <= end; t += STEP * 6) {
                Look look = staged.readout().at(t);
                Recorder rec = draw(staged, t);
                List<Recorder.Vertex> red = rec.vermillion();
                frames++;
                if (look.threatened().isEmpty()) {
                    assertTrue(red.isEmpty(), String.format(Locale.ROOT,
                            "%s at t=%.3f draws %d vermillion vertices with no Strikethrough "
                                    + "standing. STYLE.md 2.2: vermillion must always mean "
                                    + "something, and here it means nothing.",
                            kind, t, red.size()));
                    continue;
                }
                withThreat++;
                for (Recorder.Vertex v : red) {
                    double nearest = Double.MAX_VALUE;
                    int at = -1;
                    for (int tile : look.threatened()) {
                        double d = Math.abs(v.x() - tile * spread);
                        if (d < nearest) {
                            nearest = d;
                            at = tile;
                        }
                    }
                    assertTrue(nearest <= spread * 0.55, String.format(Locale.ROOT,
                            "%s at t=%.3f puts vermillion at x=%.3f, %.3f tiles from the nearest "
                                    + "threatened tile (%d) of %s. A Strikethrough is a stroke "
                                    + "through the tile it threatens; anywhere else the colour is "
                                    + "decoration.",
                            kind, t, v.x(), nearest / spread, at, look.threatened()));
                }
            }
        }
        // Swept over the three lanes together rather than each on its own: on a
        // five-tile lane every body is always in reach, so combat-design.md 1.6's
        // knife fight legitimately never has a frame without a Strikethrough, and
        // requiring one per lane would be failing the design rather than the code.
        assertTrue(withThreat > 0 && withThreat < frames,
                withThreat + " of " + frames + " sampled frames carry a Strikethrough; a sweep "
                        + "that is all one way proves only one half of this test");
    }

    /**
     * <b>Guard.</b> The interface's recession is continuous, because it is driven by
     * the camera and the camera cannot cut.
     *
     * <p>{@code Schedule.cameraIsContinuous} makes the framing a continuous function
     * of time, and {@link Readout#intimacy(double)} is a function of the framing
     * width alone, so the interface inherits the property. The obvious alternative --
     * recede while a beat is running -- is a boolean, and a boolean steps by 1.0
     * inside a single frame, which is a pop composited over a glide.
     *
     * <p>The ceiling is derived rather than chosen: no camera move is shorter than
     * {@link Timing#CAMERA_MIN_MOVE}, and the steepest point of
     * {@code Ease.SLOW_IN_SLOWER_OUT} is measured below, so the largest legal step at
     * 60 Hz is that slope divided by 0.25 s of frames.
     */
    @Test
    void theInterfaceRecedesContinuouslyBecauseTheCameraDoes() {
        double slope = steepestEase();
        double ceiling = slope / (Timing.CAMERA_MIN_MOVE * 60.0);
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            assertTrue(staged.schedule().cameraIsContinuous(),
                    kind + ": the camera keys do not join, so nothing derived from them can be "
                            + "continuous either");
            double end = staged.schedule().duration();
            double previous = staged.readout().intimacy(0);
            double previousHaze = staged.readout().haze(0);
            double worst = 0;
            double worstAt = 0;
            for (double t = STEP; t <= end; t += STEP) {
                double now = staged.readout().intimacy(t);
                double d = Math.abs(now - previous);
                if (d > worst) {
                    worst = d;
                    worstAt = t;
                }
                previous = now;
                // The distance haze of STYLE.md 9 rides the same camera, so it is held
                // to the same ceiling: a fog that steps is a pop drawn over a glide
                // exactly as a recession that steps is.
                double haze = staged.readout().haze(t);
                double dh = Math.abs(haze - previousHaze);
                if (dh > worst) {
                    worst = dh;
                    worstAt = t;
                }
                previousHaze = haze;
            }
            assertTrue(worst <= ceiling, String.format(Locale.ROOT,
                    "%s: the interface's recession steps by %.4f in one 1/60 s frame at t=%.3f, "
                            + "against a ceiling of %.4f derived from a %.2f s minimum camera "
                            + "move. A recession that steps is a pop drawn over a glide.",
                    kind, worst, worstAt, ceiling, Timing.CAMERA_MIN_MOVE));
        }
    }

    /** The largest one-step change {@code Ease.SLOW_IN_SLOWER_OUT} makes over a unit move. */
    private static double steepestEase() {
        double worst = 0;
        for (int i = 1; i <= 1000; i++) {
            double a = dev.starfall.stage.Ease.SLOW_IN_SLOWER_OUT.lerp(0, 1, (i - 1) / 1000.0);
            double b = dev.starfall.stage.Ease.SLOW_IN_SLOWER_OUT.lerp(0, 1, i / 1000.0);
            worst = Math.max(worst, Math.abs(b - a) * 1000.0);
        }
        return worst;
    }

    /**
     * <b>Guard.</b> STYLE.md 9's "wide to plan" happens <em>before</em> the next
     * command, not after it.
     *
     * <p>This is the defect System 5 found in the staging layer. {@code Scheduler}
     * emitted the return from {@code cursor()}, which by the time
     * {@code track} runs is the end of the beat that triggered the return -- so a
     * score with two commands in it went wide in the wrong gap and the planning
     * framing was never held. It could not appear in System 4 because each of those
     * scenes contains exactly one command.
     *
     * <p>The assertion is the one that fails on the old code: every return to the
     * planning framing must have <b>finished</b> before the beat that follows it
     * begins, so there is a wide shot to plan in.
     */
    @Test
    void theCameraReachesTheWideFramingInsideThePlanningGap() {
        int gaps = 0;
        for (Bout.Kind kind : Bout.Kind.values()) {
            Bout.Staged staged = Bout.of(kind);
            Stage stage = new Stage(kind.laneLength());
            double plan = stage.planning().widthTiles();
            double room = stage.returnSeconds() + stage.pushInSeconds();
            var beats = staged.schedule().beats();
            for (int i = 1; i < beats.size(); i++) {
                double from = beats.get(i - 1).end();
                double to = beats.get(i).start();
                if (to - from < room) {
                    continue;
                }
                gaps++;
                double widest = 0;
                double at = from;
                for (double t = from; t <= to; t += STEP) {
                    double w = staged.schedule().framingAt(t).widthTiles();
                    if (w > widest) {
                        widest = w;
                        at = t;
                    }
                }
                assertTrue(widest >= plan * 0.95, String.format(Locale.ROOT,
                        "%s: between the beat ending at %.3f and the one opening at %.3f there "
                                + "are %.3f s of planning time -- more than the %.3f s a return "
                                + "and a push-in need -- and the camera never opens wider than "
                                + "%.2f tiles (at t=%.3f) against a planning framing of %.2f. "
                                + "STYLE.md 9 is 'wide to plan, push in to strike'; this is a "
                                + "plan made through the strike's own lens.",
                        kind, from, to, to - from, room, widest, at, plan));
            }
        }
        assertTrue(gaps >= 3, gaps + " planning gaps long enough to return in; a sweep with no "
                + "gap in it proves nothing about going wide");
    }
}
