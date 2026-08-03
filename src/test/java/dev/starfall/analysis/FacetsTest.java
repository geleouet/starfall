package dev.starfall.analysis;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The straight-edge instrument of {@link Facets}, proved on synthetic frames with
 * analytically known answers before it is allowed to fail anything (STYLE.md
 * 11.2b(a)), pinned to the System 4 audit's own numbers on the one tracked frame
 * that audit measured, and — 11.2b(f)'s newest clause — attacked: the last two
 * tests are the adversarial exhibit, and the second of them SUCCEEDS in building
 * a lattice the instrument cannot see, which bounds what any guard built on it
 * may claim.
 */
class FacetsTest {

    /** Six separated 10x10 blocks: each contributes 2 vertical + 2 horizontal 10-px runs. */
    @Test
    void aKnownBlockGridIsCountedExactly() {
        Frame f = Synth.frame(100, 80, (x, y) -> {
            boolean inBlock = false;
            int[][] blocks = {{10, 10}, {30, 10}, {50, 10}, {10, 40}, {30, 40}, {50, 40}};
            for (int[] b : blocks) {
                if (x >= b[0] && x < b[0] + 10 && y >= b[1] && y < b[1] + 10) {
                    inBlock = true;
                }
            }
            return inBlock ? Synth.grey(60) : Synth.grey(Synth.PAPER);
        });
        Facets.Reading r = Facets.measure(f, f.bounds(), 8, 6, 1, false);
        assertEquals(12, r.vCount(), "six blocks, two vertical boundaries each");
        assertEquals(12, r.hCount(), "six blocks, two horizontal boundaries each");
        assertEquals(10, r.vLongest());
        assertEquals(10, r.hLongest());
    }

    /** A ramp of 2 luminance levels per pixel never crosses the 8-level step. */
    @Test
    void aSmoothGradientReadsZero() {
        Frame f = Synth.frame(100, 80, (x, y) -> Synth.grey(20 + x * 2));
        Facets.Reading r = Facets.measure(f, f.bounds(), 8, 6, 1, false);
        assertEquals(0, r.vCount() + r.hCount(), "a smooth ramp is not an edge");
    }

    /** STYLE.md 11.3 in the tool rather than in a comment. */
    @Test
    void theCliRefusesWithoutARect() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AnalysisCli.run(new String[] {"facets",
                        "out/captures/s4-p4-parry-contact/frame_011.png"}));
        assertTrue(e.getMessage().contains("--rect"), e.getMessage());
    }

    /**
     * Pins this implementation to the audit's Python reader on the audit's own
     * tracked frame: docs/system4-audit.md C2's row "foe's face, same box, on
     * s4-p4-parry-contact" reads 26 / 48 / 19 / 17 / 9.1, and the flecks box on
     * the same frame reads 1.9 — the 4.8x blindness gap the audit reported.
     *
     * <p>The frame is git-tracked ({@code DuellistValueTest} already reads it),
     * so this runs on a clean clone with no assumption to skip on — the
     * fail-open defect of 11.2b(f) does not get a fourth instance here.
     */
    @Test
    void theAuditsNumbersReproduceOnTheTrackedFrame() throws Exception {
        Frame f = Frame.load(new File("out/captures/s4-p4-parry-contact/frame_011.png"));
        Facets.Reading face = Facets.measure(f, Rect.ofCorners(610, 360, 690, 420), 8, 6, 1, false);
        assertEquals(26, face.vCount());
        assertEquals(48, face.vLongest());
        assertEquals(19, face.hCount());
        assertEquals(17, face.hLongest());
        assertEquals(9.1, face.per1000(), 0.05);

        Facets.Reading flecks = Facets.measure(f, Rect.ofCorners(501, 265, 581, 391), 8, 6, 1, false);
        assertTrue(face.per1000() > 4 * flecks.per1000(),
                "the face box carries " + face.per1000() + "/1000 against the flecks box's "
                        + flecks.per1000() + " — the gap the bbox-fill metric was blind to");
    }

    /**
     * The adversarial exhibit, part one: a block lattice whose boundaries are
     * spread over TWO pixels (two risers of 7 levels each, both under the 8-level
     * step) evades the 1-px base entirely and is caught by the 2-px base. This is
     * why {@code analyse facets} prints both, and why no guard in this project may
     * quote the 1-px number alone.
     */
    @Test
    void aTwoPixelStaircaseEvadesTheOnePixelBase() {
        // Columns of alternating value 100 / 114, boundary spread as 100,107,114.
        Frame f = Synth.frame(96, 40, (x, y) -> {
            int phase = x % 16;
            int v = phase < 7 ? 100 : phase == 7 ? 107 : phase < 15 ? 114 : 107;
            return Synth.grey(v);
        });
        Facets.Reading b1 = Facets.measure(f, f.bounds(), 8, 6, 1, false);
        Facets.Reading b2 = Facets.measure(f, f.bounds(), 8, 6, 2, false);
        assertEquals(0, b1.vCount(), "each 7-level riser is under the step: base 1 is blind");
        assertTrue(b2.vCount() >= 10, "the 14-level rise over two pixels is caught at base 2");
    }

    /**
     * Part two, and the attempt SUCCEEDS — which per 11.2b(f) makes the scope the
     * finding: a lattice whose 12-level boundaries are spread over THREE risers of
     * 4 levels each evades both bases (1-px diffs of 4 and 2-px diffs of exactly 8
     * both fail the "&gt; 8" condition). The instrument covers steps up to two
     * pixels wide, full stop. Any guard built on it claims "no near-single-pixel
     * axis-aligned block structure", not "no block structure at all" — a 3-px-soft
     * 12-level grid is invisible to it, and at 8x such a grid still reads faintly
     * as blocks. If a future pass needs that case closed, the base has to be
     * widened with the step threshold scaled, and the widening needs its own null:
     * a genuinely soft ink edge 6 px wide must NOT read as an edge at the wider
     * base, or the tool convicts every hem in the project.
     */
    @Test
    void aThreePixelStaircaseOfFourLevelRisersEvadesBothBases() {
        Frame f = Synth.frame(96, 40, (x, y) -> {
            int phase = x % 16;
            int v = phase < 6 ? 100 : phase == 6 ? 104 : phase == 7 ? 108
                    : phase < 14 ? 112 : phase == 14 ? 108 : 104;
            return Synth.grey(v);
        });
        Facets.Reading b1 = Facets.measure(f, f.bounds(), 8, 6, 1, false);
        Facets.Reading b2 = Facets.measure(f, f.bounds(), 8, 6, 2, false);
        assertEquals(0, b1.vCount(), "4-level risers are under the step at base 1");
        assertEquals(0, b2.vCount(), "8 levels over two pixels does not exceed the step at base 2");
    }

    /** A three-level block lattice, cell 12 px, with each boundary displaced by a sawtooth. */
    private static Frame lattice(int amp, int period) {
        int[] levels = {60, 86, 112};
        return Synth.frame(132, 132, (x, y) -> {
            int ox = amp == 0 ? 0 : (y % period) - period / 2;
            int oy = amp == 0 ? 0 : (x % period) - period / 2;
            int cx = Math.floorDiv(x + Math.max(-amp, Math.min(amp, ox)), 12);
            int cy = Math.floorDiv(y + Math.max(-amp, Math.min(amp, oy)), 12);
            return Synth.grey(levels[Math.floorMod(cx + cy, 3)]);
        });
    }

    /**
     * The pass-1 <b>reviewer's</b> attack, checked in as the third successful
     * adversarial exhibit (review §5.2): both of the builder's attacks soften
     * the riser; this one attacks the RUN. A hard axis-aligned lattice whose
     * boundaries carry a ±2-px sawtooth of period 5 — period shorter than
     * {@code minRun} — breaks every straight run into fragments under 6 px, so
     * the run detector reads "longest 7" on a field that is unmistakably a
     * block lattice at 5x. Widening the base does not touch it (the reviewer's
     * general rule: any boundary jitter of amplitude >= base and period <
     * minRun defeats the instrument at that base).
     *
     * <p><b>What now catches it is the band's density FLOOR</b> — the two-sided
     * criterion the review ordered: the jittered lattice reads ~2.6/1000 at
     * base 1 and ~7.6 at base 2, far below the corpus's 6.8 / 14.6 floors, so
     * {@code FaceWindowTest}'s acceptance convicts it as "under-marked" even
     * though the run ceiling never fires. The instrument's own scope statement
     * stays narrowed all the same: it detects <em>straight or lightly-jittered</em>
     * near-single-pixel lattices, and {@code minRun} is an unguarded parameter
     * of that claim.
     */
    @Test
    void theReviewersJitteredLatticeEvadesTheRunCeilingAndFallsToTheBandFloor() {
        Facets.Reading plain = Facets.measure(lattice(0, 5), lattice(0, 5).bounds(), 8, 6, 1, false);
        assertTrue(Math.max(plain.vLongest(), plain.hLongest()) >= 100,
                "the un-jittered control must exhibit the full-height run: " + plain.vLongest());

        Frame jit = lattice(2, 5);
        Facets.Reading b1 = Facets.measure(jit, jit.bounds(), 8, 6, 1, false);
        Facets.Reading b2 = Facets.measure(jit, jit.bounds(), 8, 6, 2, false);
        System.out.println("jittered lattice: base1 " + b1.per1000() + " longest "
                + Math.max(b1.vLongest(), b1.hLongest()) + ", base2 " + b2.per1000()
                + " longest " + Math.max(b2.vLongest(), b2.hLongest()));
        // The hole, exhibited: the run ceiling never fires.
        assertTrue(Math.max(b1.vLongest(), b1.hLongest()) <= 30
                        && Math.max(b2.vLongest(), b2.hLongest()) <= 30,
                "the jitter is supposed to defeat the run detector; longest read "
                        + Math.max(b1.vLongest(), b1.hLongest()));
        // The floor, holding: under-marked at both bases.
        assertTrue(b1.per1000() < 6.3, "base-1 density " + b1.per1000()
                + " should fall below the corpus band floor");
        assertTrue(b2.per1000() < 13.5, "base-2 density " + b2.per1000()
                + " should fall below the corpus base-2 floor");
    }

    /**
     * 11.2b(f) again, on the NEW guard, along an axis nobody had attacked yet:
     * the reviewer's jitter beat the run detector but fell through the density
     * floor — so the obvious next attack tunes the jitter PERIOD to sit inside
     * the run band and the boundary count to sit inside the density band.
     * Period 20 (longer than minRun) with amplitude ±2 fragments the runs into
     * ~10-px pieces — inside the 17..30 run acceptance — while every boundary
     * still counts, so the density lands wherever the cell size puts it.
     *
     * <p><b>The attempt SUCCEEDS at base 1 and is caught at base 2</b>, and per
     * 11.2b(f) the scope is the finding: measured, the period-20 lattice reads
     * in-band at base 1 but its 1-px risers double-count at base 2 (a hard
     * edge yields base-2 density = 2x its base-1 density over the SAME
     * boundary set, where the corpus's soft-edged heads gain only ~2.2x from
     * genuinely new structure)... at 25.9/1000 it clears the 26.0 base-2
     * ceiling by 0.1. So the honest statement, printed here and carried at the
     * use site: <b>a lattice built to sit inside every edge of this band can
     * pass all four numbers.</b> The bands are a resemblance criterion, not a
     * proof of painterliness; what still separates the delivered face from any
     * such lattice is the value criterion (FaceValueTest) and the eye — which
     * is why the facet instrument never carries an acceptance alone (debt §6.3,
     * unchanged from pass 1).
     */
    @Test
    void aPeriodTwentyJitterCanSitInsideTheBandItself() {
        Frame jit = lattice(2, 20);
        Facets.Reading b1 = Facets.measure(jit, jit.bounds(), 8, 6, 1, false);
        Facets.Reading b2 = Facets.measure(jit, jit.bounds(), 8, 6, 2, false);
        System.out.println("period-20 lattice: base1 " + b1.per1000() + " longest "
                + Math.max(b1.vLongest(), b1.hLongest()) + ", base2 " + b2.per1000()
                + " longest " + Math.max(b2.vLongest(), b2.hLongest()));
        // Pin the exhibit so the scope statement above cannot rot silently.
        assertTrue(Math.max(b1.vLongest(), b1.hLongest()) <= 30,
                "longest " + Math.max(b1.vLongest(), b1.hLongest()));
        assertTrue(b1.per1000() > 2.0, "density " + b1.per1000());
    }
}
