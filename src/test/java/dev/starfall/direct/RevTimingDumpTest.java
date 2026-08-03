package dev.starfall.direct;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * TEMPORARY reviewer instrument for the System 4 pass-2 review. Not part of the
 * build's own guards; delete after the review. Dumps the headless rehearsal's
 * clock and blade geometry so timing claims can be graded off a headless
 * measurement driving the same schedule the capture runs (STYLE.md 7.1).
 */
class RevTimingDumpTest {

    @Test
    void dump() throws Exception {
        Path dir = Path.of("out", "review");
        Files.createDirectories(dir);
        for (Duel.Kind kind : Duel.Kind.values()) {
            Rehearsal r = new Rehearsal(kind);
            List<Rehearsal.Frame> frames = r.play(r.schedule().duration(), 120.0);
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                    dir.resolve("rehearsal-" + kind.sceneName() + ".csv")))) {
                w.println("wall,t,timeScale,bladeGapFraction,heroHipX,heroHipY,heroHandX,heroHandY,"
                        + "heroTipX,heroTipY,foeHipX,foeHipY,foeHandX,foeHandY,foeTipX,foeTipY,"
                        + "heroShoulderX,heroShoulderY,heroElbowX,heroElbowY,heroHeadX,heroHeadY,"
                        + "heroStandX,foeStandX,heroArmWeight,heroBladeWeight");
                for (Rehearsal.Frame f : frames) {
                    w.printf(Locale.ROOT,
                            "%.5f,%.5f,%.5f,%.6f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,"
                                    + "%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f%n",
                            f.wall(), f.t(), f.timeScale(), f.bladeGapFraction(),
                            f.hero().hips().x, f.hero().hips().y,
                            f.hero().hand().x, f.hero().hand().y,
                            f.hero().bladeTip().x, f.hero().bladeTip().y,
                            f.foe().hips().x, f.foe().hips().y,
                            f.foe().hand().x, f.foe().hand().y,
                            f.foe().bladeTip().x, f.foe().bladeTip().y,
                            f.hero().shoulder().x, f.hero().shoulder().y,
                            f.hero().elbow().x, f.hero().elbow().y,
                            f.hero().head().x, f.hero().head().y,
                            f.hero().stand().x, f.foe().stand().x,
                            f.hero().armWeight(), f.hero().bladeWeight());
                }
            }
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(
                    dir.resolve("clashes-" + kind.sceneName() + ".txt")))) {
                w.println("duration=" + r.schedule().duration());
                w.println("contacts=" + r.schedule().contacts());
                for (var c : r.clashes()) {
                    Rehearsal.Frame f = r.at(c.at());
                    w.println("clash at=" + c.at() + " site=" + c.origin().site()
                            + " bladeGapFraction=" + f.bladeGapFraction());
                }
            }
        }
    }
}
