package dev.starfall.ui;

import dev.starfall.combat.CombatEngine;
import dev.starfall.combat.CombatEvent;
import dev.starfall.combat.Combatant;
import dev.starfall.combat.Command;
import dev.starfall.combat.EnemyArchetype;
import dev.starfall.combat.EncounterSpec;
import dev.starfall.combat.Facing;
import dev.starfall.combat.Hero;
import dev.starfall.combat.InkStanza;
import dev.starfall.combat.Resolution;
import dev.starfall.combat.Tile;
import dev.starfall.combat.TileType;
import dev.starfall.stage.Schedule;
import dev.starfall.stage.ScheduledBeat;
import dev.starfall.stage.Scheduler;
import dev.starfall.stage.Stage;
import dev.starfall.stage.Standing;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Ink Stanza as the player reads it: order, wetness, and the one line that has
 * nothing on it.
 *
 * <p>Every assertion here is against the {@link Look} the renderer is handed or the
 * geometry it emits, never against the arithmetic that produced either.
 */
class StanzaColumnTest {

    /** A board with five tiles banked, its schedule, and the readout of both. */
    private record Bench(CombatEngine engine, Schedule schedule, Readout readout,
                         List<ScheduledBeat> clauses, Resolution phrase) {
    }

    private static Bench bench() {
        CombatEngine engine = CombatEngine.create(EncounterSpec.builder(11, Hero.WARDEN)
                .heroAt(2, Facing.RIGHT)
                .heroHp(20)
                .enemy(EnemyArchetype.REACHER, 6, Facing.LEFT)
                .enemy(EnemyArchetype.BULWARK, 10, Facing.LEFT)
                .loadout(Tile.of(TileType.CUT), Tile.of(TileType.STEP), Tile.of(TileType.SWEEP),
                        Tile.of(TileType.THRUST), Tile.of(TileType.PARRY))
                .build());
        Stage stage = Stage.of(engine.state().lane());
        Readout.Builder builder = Readout.builder(stage);
        for (int i = 0; i < InkStanza.CAPACITY; i++) {
            Tile tile = engine.state().loadout().tile(i);
            assertTrue(engine.can(Command.add(i)), "tile " + i + " must be bankable");
            engine.apply(Command.add(i));
            builder.bank(0.0, tile);
        }
        List<Standing.Body> bodies = new ArrayList<>();
        for (Combatant c : engine.state().all()) {
            bodies.add(new Standing.Body(c.id(), c.tile(), c.facing()));
        }
        Scheduler scheduler = new Scheduler(stage, Standing.opening(bodies));
        scheduler.accept(engine.opening());
        builder.board(0.0, engine.state(), Readout.footprint(engine.previewExecution()));
        scheduler.pause(2.0);
        int before = scheduler.beats().size();
        Resolution phrase = engine.apply(Command.execute());
        scheduler.accept(phrase);
        List<ScheduledBeat> clauses = new ArrayList<>();
        for (ScheduledBeat b : scheduler.beats().subList(before, scheduler.beats().size())) {
            if (b.bracket() == ScheduledBeat.Bracket.STANZA) {
                clauses.add(b);
            }
        }
        builder.resolve(clauses);
        builder.board(clauses.get(clauses.size() - 1).end(), engine.state(), List.of());
        Schedule schedule = scheduler.schedule();
        return new Bench(engine, schedule, builder.build(schedule), clauses, phrase);
    }

    private static List<TileType> types(List<Look.Written> stanza) {
        List<TileType> out = new ArrayList<>();
        for (Look.Written w : stanza) {
            out.add(w.type());
        }
        return out;
    }

    /**
     * <b>Guard.</b> The column the player reads downward is the order the engine
     * resolves in.
     *
     * <p>The two lists come from different places and that is the point: the left
     * one is the sequence of {@link CombatEvent.BeatBegan} the engine emitted while
     * resolving the phrase, and the right one is what the interface put on the sheet
     * from the top down. combat-design.md 3's whole argument for the vertical column
     * is that those two are the same list and that nothing anywhere reverses one, so
     * a divergence between them is the rule becoming something that has to be
     * explained.
     */
    @Test
    void theColumnReadsDownwardInTheOrderTheEngineResolves() {
        Bench b = bench();
        List<TileType> resolved = new ArrayList<>();
        for (CombatEvent.BeatBegan beat : b.phrase().of(CombatEvent.BeatBegan.class)) {
            resolved.add(beat.tile().type());
        }
        List<TileType> read = types(b.readout().at(1.0).stanza());
        assertEquals(resolved, read,
                "combat-design.md 3: the column is read top-down as what happens next. "
                        + "The engine resolves " + resolved + " and the sheet is written " + read
                        + " -- the player would have to be taught the rule.");
    }

    /**
     * <b>Guard.</b> The mark the engine resolves first is drawn highest.
     *
     * <p>The reading order above is a property of a list; this is a property of the
     * picture. Each cartouche is drawn on its own through {@link Recorder}, and the
     * assertion is on the alpha-weighted centre of the ink that came out, so it
     * fails if the column's pitch changes sign, if the base-anchored heights of
     * {@link Look.Written} are inverted, or if the marks are laid out in any order
     * other than the one they resolve in.
     */
    @Test
    void theMarkThatResolvesFirstIsDrawnHighestOnTheSheet() {
        Bench b = bench();
        List<Look.Written> stanza = b.readout().at(1.0).stanza();
        double previous = Double.MAX_VALUE;
        for (int i = 0; i < stanza.size(); i++) {
            Recorder rec = new Recorder();
            LaneInterface.column(rec, one(stanza.get(i)), 0f);
            double y = rec.meanY();
            assertTrue(y < previous,
                    "reading position " + i + " (" + stanza.get(i).type() + ") is drawn at y=" + y
                            + " and the mark above it at y=" + previous
                            + "; the column must descend as it is read, or the top of the sheet "
                            + "is not what happens next.");
            previous = y;
        }
        assertEquals(InkStanza.CAPACITY, stanza.size());
    }

    /**
     * <b>Guard.</b> The clause now resolving is the strongest mark in the column.
     *
     * <p>Measured as ink -- the sum of the alpha the renderer was handed -- over
     * each cartouche drawn alone, at the middle of every one of the five beats, and
     * <b>normalised by that same glyph's own ink at full wetness</b>. The
     * normalisation is not a softening: the glyphs are different gestures and a
     * Parry is two strokes where a Cut is one, so a raw comparison between two
     * cartouches would be measuring the vocabulary. It was caught by breaking the
     * <em>other</em> guard in this class -- with the column's drying gradient set to
     * zero, this test went red on a Parry losing to a Sweep purely on stroke count.
     *
     * <p>The first version of {@code cartouche} faded a mark from the instant its
     * beat opened, which made the resolving clause the <em>weakest</em> thing on the
     * sheet: distinguishable, and distinguishable in a way that reads as cancelled
     * rather than as happening. The absolute form of the claim is measured on the
     * delivered pixels instead and recorded in {@code docs/system5-debt.md}.
     */
    @Test
    void theResolvingClauseIsTheStrongestMarkInTheColumn() {
        Bench b = bench();
        for (ScheduledBeat clause : b.clauses()) {
            double t = clause.start() + (clause.contactEnd() - clause.start()) * 0.35;
            Look look = b.readout().at(t);
            Look.Written resolving = look.resolving();
            assertNotNull(resolving, "no clause is resolving at t=" + t
                    + ", inside beat " + clause);
            double best = share(look, resolving);
            for (Look.Written w : look.stanza()) {
                if (w.height() == resolving.height()) {
                    continue;
                }
                double other = share(look, w);
                assertTrue(best > other,
                        "at t=" + t + " the clause being resolved (" + resolving.type()
                                + ", height " + resolving.height() + ") carries " + best
                                + " of ink and the queued " + w.type() + " at height " + w.height()
                                + " carries " + other
                                + "; a beat drinking a mark must make it the strongest thing in "
                                + "the column, not the faintest.");
            }
        }
    }

    /**
     * <b>Guard.</b> The column dries downward: the mark that resolves next carries
     * the most ink and every line below it carries less.
     *
     * <p>This is combat-design.md 3's whole argument for the vertical column made
     * true of the picture rather than only of the ordering -- <i>"what you wrote
     * last is at the top and goes first, so the player never learns LIFO, they just
     * read downward"</i>. A column of five equally weighted marks says what the
     * order is; it does not say <em>why</em> that is the order, and a player who has
     * not been told still has to be told.
     *
     * <p>Measured on the ink the renderer was handed, per cartouche drawn alone,
     * and <b>normalised by the same glyph's own ink at full wetness</b> -- because
     * the marks are different gestures and a Parry is two strokes where a Cut is
     * one, so a raw comparison between two cartouches measures the vocabulary
     * rather than the wetness.
     */
    @Test
    void theColumnDriesDownwardFromTheMarkThatResolvesNext() {
        Bench b = bench();
        List<Look.Written> stanza = b.readout().at(1.5).stanza();
        assertEquals(InkStanza.CAPACITY, stanza.size());
        double previous = Double.MAX_VALUE;
        Look look = b.readout().at(1.5);
        for (int i = 0; i < stanza.size(); i++) {
            Look.Written w = stanza.get(i);
            double share = share(look, w);
            assertTrue(share < previous, String.format(
                    "reading position %d (%s) carries %.4f of its own full-wetness ink and the "
                            + "line above it carries %.4f. The column has to dry downward, or the "
                            + "player is told the order without being told why it is the order.",
                    i, w.type(), share, previous));
            previous = share;
        }
    }

    /**
     * <b>Guard.</b> An empty stanza still prints its column.
     *
     * <p>A sheet with nothing written on it has to say that there is a stanza, how
     * many lines it holds, and which line the next mark lands on. Otherwise "the
     * queue is empty" and "the interface is not drawn" are the same picture.
     */
    @Test
    void anEmptyStanzaStillPrintsEveryLineOfItsColumn() {
        Bench b = bench();
        double after = b.clauses().get(b.clauses().size() - 1).end() + Readout.DRY_SECONDS + 0.2;
        Look look = b.readout().at(after);
        assertTrue(look.stanzaIsEmpty(), "the phrase is over, so the stanza is empty at t=" + after);

        Recorder rec = new Recorder();
        LaneInterface.sheet(rec, look, 4f / 3f, 0f);
        for (int h = 0; h < InkStanza.CAPACITY; h++) {
            float y = LaneInterface.STANZA_BASE_Y + h * LaneInterface.STANZA_PITCH;
            double ink = 0;
            for (Recorder.Vertex v : rec.vertices) {
                if (Math.abs(v.y() - y) < LaneInterface.STANZA_PITCH * 0.35f
                        && Math.abs(v.x() - LaneInterface.COLUMN_X) < LaneInterface.STANZA_GLYPH) {
                    ink += v.alpha();
                }
            }
            assertTrue(ink > 0.01,
                    "line " + h + " of an empty stanza, at y=" + y + ", carries " + ink
                            + " of ink; an empty queue and an absent interface must not be the "
                            + "same picture.");
        }
    }

    /** Known answer, not observed red: every tile the engine can bank has a mark authored. */
    @Test
    void everyTileTypeHasAMark() {
        assertTrue(Glyph.isComplete(), "a tile with no glyph throws at draw time");
        for (TileType type : TileType.values()) {
            assertTrue(Glyph.of(type).size() >= 1, type + " has no strokes");
            assertEquals(Glyph.of(type).size(), Glyph.of(type, -1).size(),
                    type + " loses a stroke when the hero is turned");
        }
    }

    /**
     * <b>Guard.</b> No two things the player can be looking at are the same picture.
     *
     * <p>This is the one property a no-text alphabet cannot do without, and until now
     * nothing watched it. {@code everyTileTypeHasAMark} asserts that a mark exists and
     * that mirroring preserves the <em>stroke count</em>; neither says two tiles are
     * different, and for {@code STEP} and {@code BACK_STEP} they provably were not.
     * The pass-1 review enumerated it: {@code Glyph.of(STEP, -1)} was
     * <b>vertex-identical</b> to {@code Glyph.of(BACK_STEP, +1)}, so the alphabet had
     * 16 distinct shapes for 18 {@code (tile, facing)} pairs, and the two it merged
     * were forward and back on a lane -- the sharpest choice a player makes, told
     * apart only by first reading an 86 px figure's facing.
     *
     * <h2>Distance, and why it is not a vertex comparison</h2>
     *
     * <p>Vertex identity is what the review found and it is the weakest possible form
     * of the property: two glyphs one control point apart would pass it and be the
     * same picture. So the comparison is between the marks as <b>pictures</b> -- each
     * rasterised alone at the shipped cartouche side of 71 px, differenced, and
     * normalised by the ink there is rather than by the box, because a mean over the
     * whole cartouche makes every sparse mark resemble every other one.
     *
     * <p>0 means identical and 1 means sharing no paper at all. The floor is
     * {@value #GLYPH_FLOOR}: two marks that share more than four fifths of their ink
     * are one mark with a wobble.
     *
     * <h2>Where this is narrower than the review asked, and why</h2>
     *
     * <p>The review asked for distinctness across all 18 {@code (tile, facing)} pairs.
     * That is one step too strong and the measurement says so: a <b>Sweep</b> is "one
     * continuous arc through two bodies", it hits the tile in front <em>and</em> the
     * tile behind, and its mark is a near-symmetric 230 degree arc -- so its own
     * mirror measures <b>0.154</b> away from it, and that is the mark correctly
     * saying that this tile does not care which way the hero is turned. Requiring the
     * two facings of one tile to differ would be requiring the alphabet to lie about
     * a symmetric gesture.
     *
     * <p>So what is asserted is the property the collision actually broke: <b>two
     * different tiles never draw one picture, at any pair of facings.</b> That is the
     * 144 cross-tile comparisons, and the closest of them is recorded in
     * {@code docs/system5-debt.md} beside the same-tile mirror distances.
     */
    @Test
    void noTwoTilesAreTheSamePicture() {
        TileType[] types = TileType.values();
        int pairs = types.length * 2;
        double closest = Double.MAX_VALUE;
        String where = "";
        double closestMirror = Double.MAX_VALUE;
        String mirror = "";
        for (int i = 0; i < pairs; i++) {
            for (int j = i + 1; j < pairs; j++) {
                Raster a = Guards.glyphField(types[i / 2], i % 2 == 0 ? 1 : -1, CARTOUCHE_PX);
                Raster b = Guards.glyphField(types[j / 2], j % 2 == 0 ? 1 : -1, CARTOUCHE_PX);
                double d = Raster.distance(a, b);
                if (types[i / 2] == types[j / 2]) {
                    if (d < closestMirror) {
                        closestMirror = d;
                        mirror = types[i / 2].toString();
                    }
                    continue;
                }
                if (d < closest) {
                    closest = d;
                    where = name(types[i / 2], i % 2 == 0) + " and " + name(types[j / 2], j % 2 == 0);
                }
            }
        }
        System.out.printf(java.util.Locale.ROOT,
                "CONTROL closest cross-tile pair of %d at the shipped %d px cartouche: %s = "
                        + "%.4f, against a floor of %.2f%n",
                (pairs * (pairs - 1)) / 2 - types.length, CARTOUCHE_PX, where, closest, GLYPH_FLOOR);
        assertTrue(closest > GLYPH_FLOOR, String.format(
                "%s are the same picture: they differ by %.4f of their own ink at the shipped "
                        + "%d px cartouche, against a floor of %.2f. Two tiles that draw one mark "
                        + "can only be told apart by something outside the mark, and at the "
                        + "planning framing there is nothing outside the mark to read.",
                where, closest, CARTOUCHE_PX, GLYPH_FLOOR));
        assertTrue(closestMirror > 0.0,
                mirror + " is bit-identical to its own mirror, which cannot happen and means "
                        + "the measurement is not reading the glyphs it thinks it is");
        assertEquals(TileType.SWEEP.toString(), mirror,
                "the closest same-tile mirror pair is " + mirror + " at " + closestMirror
                        + "; the Sweep is supposed to be the symmetric one");
    }

    /**
     * <b>Guard.</b> The Sweep is the <em>only</em> tile allowed below the distinctness
     * floor under mirroring, and every other tile clears it.
     *
     * <p>{@link #noTwoTilesAreTheSamePicture()} narrows its assertion to the 144
     * cross-tile pairs and {@code continue}s past every same-tile one, and the pass-2
     * review upheld that narrowing as better reasoning than the instruction it
     * replaced -- <i>"requiring 0.20 there would require the alphabet to lie about a
     * symmetric gesture"</i> -- while naming what was still missing: <b>the exception
     * lives in prose, and the guard is silent on the axis it excludes.</b> A future
     * tile authored near-symmetric by accident would land in the same hole without a
     * sound, which is STYLE.md 11.2b(e) exactly: <i>"a discipline written into a
     * document but not into the tool that reads it is documentation, not a guard."</i>
     * {@code TURN} is one keystroke away -- a spiral closed into a ring is symmetric
     * -- and sits at 0.79 today.
     *
     * <p>So the exception is a claim the suite defends: Sweep is below the floor,
     * everything else is above it, and both halves fail if the alphabet moves.
     */
    @Test
    void sweepIsTheOnlyTilePermittedToBeItsOwnMirror() {
        StringBuilder all = new StringBuilder();
        for (TileType t : TileType.values()) {
            double d = Raster.distance(Guards.glyphField(t, 1, CARTOUCHE_PX),
                    Guards.glyphField(t, -1, CARTOUCHE_PX));
            all.append(String.format(java.util.Locale.ROOT, "%s %.4f  ", t, d));
            if (t == TileType.SWEEP) {
                assertTrue(d < GLYPH_FLOOR, String.format(
                        "the Sweep measures %.4f from its own mirror, which is above the %.2f "
                                + "floor -- so it is no longer the symmetric gesture "
                                + "combat-design.md 2.2 describes (\"one continuous arc through "
                                + "two bodies\", front and behind), and the exception the "
                                + "distinctness guard makes for it is no longer earned. All "
                                + "mirrors: %s", d, GLYPH_FLOOR, all));
            } else {
                assertTrue(d > GLYPH_FLOOR, String.format(
                        "%s measures %.4f from its own mirror at the shipped %d px cartouche, "
                                + "below the %.2f floor. Only the Sweep is permitted there, "
                                + "because only the Sweep hits the tile in front and the tile "
                                + "behind; every other tile points, and a tile that points must "
                                + "look different pointing the other way. All mirrors: %s",
                        t, d, CARTOUCHE_PX, GLYPH_FLOOR, all));
            }
        }
        System.out.println("CONTROL same-tile mirror distances at " + CARTOUCHE_PX + " px: " + all);
    }

    /** The side of a delivered cartouche in pixels: {@code STANZA_GLYPH} of 720 rows. */
    private static final int CARTOUCHE_PX =
            Math.round(LaneInterface.STANZA_GLYPH * 720f);

    private static final double GLYPH_FLOOR = 0.20;

    private static String name(TileType t, boolean right) {
        return t + (right ? " facing right" : " facing left");
    }

    private static Look one(Look.Written w) {
        return new Look(0.0, List.of(w), List.of(), 8, 8, 11, 2, 1, List.of(), List.of(), 1.0, 0.0);
    }

    /**
     * How much of its own full-wetness ink a cartouche is carrying: the quantity
     * that is about pigment rather than about which gesture the tile happens to be.
     */
    private static double share(Look look, Look.Written w) {
        Look.Written full = new Look.Written(w.type(), w.enchantment(), w.height(),
                1.0, 0.0, 0.0, w.seed());
        return ink(look, w) / ink(look, full);
    }

    private static double ink(Look look, Look.Written w) {
        Recorder rec = new Recorder();
        LaneInterface.column(rec, new Look(look.time(), List.of(w), look.hand(), look.health(),
                look.maxHealth(), look.laneLength(), look.heroTile(), look.heroStep(),
                look.reached(), look.threatened(), look.bleed(), look.intimacy()), 0f);
        return rec.ink();
    }
}
