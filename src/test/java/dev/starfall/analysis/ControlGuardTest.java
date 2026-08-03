package dev.starfall.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code analyse drape --control} must refuse a directory that is not a rigid control of the
 * capture it is being compared with.
 *
 * <p>STYLE.md §11.2b(e) exists because of this exact command, and names it:
 *
 * <blockquote>A discipline written into a document but not into the tool that reads it is
 * documentation, not a guard. {@code track} <i>refuses</i> to run without an anchor, and anchors
 * stopped being a problem. {@code drape} writes the control flag into the manifest and never
 * checks it — and a reviewer proved in one command that it will call a live capture a rigid
 * control.</blockquote>
 *
 * <p>So the refusals are asserted here rather than described in a comment. Each test is one of
 * the ways the pass-4 control could have been wrong without anybody noticing, and the fourth —
 * the harness check — is the one that would have caught the capture bug that corrupted every
 * frame this project took, the moment somebody compared across it.
 */
class ControlGuardTest {

    @TempDir
    File tmp;

    @Test
    void aLiveCaptureIsRefusedAsAControl() throws IOException {
        File live = capture("live", "sim-sway", "1.0", "0.0167", "none", "abc123");
        File notAControl = capture("also-live", "sim-sway", "1.0", "0.0167", "none", "abc123");
        String message = refusal(live, notAControl);
        assertTrue(message.contains("clamp=none"), message);
        assertTrue(message.contains("not a rigid control"), message);
    }

    @Test
    void aControlOfADifferentWindowIsRefused() throws IOException {
        File live = capture("live", "sim-sway", "1.0", "0.0167", "none", "abc123");
        File control = capture("control", "sim-sway", "1.66", "0.0167", "cloth", "abc123");
        String message = refusal(live, control);
        assertTrue(message.contains("different window"), message);
        assertTrue(message.contains("start"), message);
    }

    @Test
    void aControlOfADifferentSceneIsRefused() throws IOException {
        File live = capture("live", "sim-sway", "1.0", "0.0167", "none", "abc123");
        File control = capture("control", "sim-extreme", "1.0", "0.0167", "cloth", "abc123");
        assertTrue(refusal(live, control).contains("scene"), "the scene mismatch must be named");
    }

    /**
     * STYLE.md §11.2b(d): a comparison spanning two harness versions is void <em>by default</em>
     * rather than by discovery. A manifest with no {@code harness=} line is a harness version
     * too — the one from before the field existed — so it must not silently match a recorded one.
     */
    @Test
    void aControlFromAnotherHarnessIsRefusedUnlessSaidOutLoud() throws IOException {
        File live = capture("live", "sim-sway", "1.0", "0.0167", "none", "aaaaaaaaaaaa");
        File control = capture("control", "sim-sway", "1.0", "0.0167", "cloth", "bbbbbbbbbbbb");
        assertTrue(refusal(live, control).contains("different harness"), "the harness drift must be named");

        File unrecorded = capture("old", "sim-sway", "1.0", "0.0167", "cloth", null);
        assertTrue(refusal(live, unrecorded).contains("different harness"),
                "a manifest with no harness line must not match a recorded one");
    }

    @Test
    void aControlWithNoManifestIsRefused() throws IOException {
        File live = capture("live", "sim-sway", "1.0", "0.0167", "none", "abc123");
        File bare = new File(tmp, "bare");
        assertTrue(bare.mkdirs());
        writeFrames(bare);
        assertTrue(refusal(live, bare).contains("no capture.txt"), "the missing manifest must be named");
    }

    /**
     * {@code analyse corridor} must refuse without {@code --span}.
     *
     * <h2>Why this is here and not in a comment</h2>
     *
     * <p>The rule was written down. {@code docs/system4-debt.md}'s Commands section ended
     * with <i>"Give {@code --span} on every Family B capture. Without it the detected figure
     * box spans the ground smear and both frame edges, and every ratio in this document is
     * wrong by 30-50%"</i> — and the pass-4 review then found that the same document's §6.1
     * had compared a spanned reading of one {@code LANE_SPREAD} against an un-spanned reading
     * of another, and drew a structural refusal out of the difference. The gap between the
     * two readings on the shipped capture is <b>1 of 24 frames one mass against 17 of 24</b>.
     *
     * <p>§11.2b(e) is the rule this closes: a discipline written into a document but not into
     * the tool that reads it is documentation, not a guard.
     */
    @Test
    void corridorRefusesWithoutASpan() throws IOException {
        File dir = capture("corridor", "duel-parry", "1.42", "0.0167", "none", "abc123");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> corridorRun(dir, false, false));
        assertTrue(e.getMessage().contains("--span is required"), e.getMessage());
        assertTrue(e.getMessage().contains("17 of 24"), e.getMessage());
        // Both forms of the command, because both normalise by a figure height.
        assertThrows(IllegalArgumentException.class, () -> corridorRun(dir, false, true));
        // And both waivers are accepted, so the refusal is a gate and not a wall.
        assertEquals(0, corridorRun(dir, true, false), "an explicit --span must be accepted");
    }

    private int corridorRun(File dir, boolean withSpan, boolean profile) throws IOException {
        PrintStream saved = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        try {
            java.util.List<String> argv = new java.util.ArrayList<>(
                    java.util.List.of("corridor", dir.getPath()));
            if (withSpan) {
                argv.add("--span");
                argv.add("0,10,120,100");
            }
            if (profile) {
                argv.add("--profile");
            }
            return AnalysisCli.run(argv.toArray(new String[0]));
        } finally {
            System.setOut(saved);
        }
    }

    @Test
    void aMatchingRigidControlIsAccepted() throws IOException {
        File live = capture("live", "sim-sway", "1.0", "0.0167", "none", "abc123");
        File control = capture("control", "sim-sway", "1.0", "0.0167", "cloth", "abc123");
        // A non-zero exit is a gate result, not an error: these synthetic frames are static, so
        // every gate fails. What is asserted is that the run completes rather than refusing.
        assertEquals(1, run(live, control), "a matching control must be accepted");
    }

    private String refusal(File live, File control) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> run(live, control));
        return e.getMessage();
    }

    private int run(File live, File control) throws IOException {
        PrintStream saved = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        try {
            return AnalysisCli.run(new String[] {
                    "drape", live.getPath(),
                    "--region", "cloth=20,60,40,40",
                    "--region", "anchor=20,10,40,40",
                    "--anchor", "anchor",
                    "--control", control.getPath(),
                    "--axis", "x"});
        } finally {
            System.setOut(saved);
        }
    }

    private File capture(String name, String scene, String start, String step, String clamp,
                         String harness) throws IOException {
        File dir = new File(tmp, name);
        assertTrue(dir.mkdirs());
        writeFrames(dir);
        StringBuilder sb = new StringBuilder();
        sb.append("# Reproduce this capture:\n./gw capture -Pscene=").append(scene).append('\n');
        sb.append("\nscene=").append(scene);
        sb.append("\nframes=3");
        sb.append("\nstart=").append(start);
        sb.append("\nstep=").append(step);
        sb.append("\nsize=120x120");
        sb.append("\nclamp=").append(clamp);
        sb.append("\nclothRigid=").append("cloth".equals(clamp));
        sb.append("\ncommit=deadbeef");
        if (harness != null) {
            sb.append("\nharness=").append(harness);
        }
        sb.append('\n');
        Files.writeString(new File(dir, "capture.txt").toPath(), sb.toString(), StandardCharsets.UTF_8);
        return dir;
    }

    /** Three frames of paper with one dark block, enough for the tracker to have something to lock onto. */
    private void writeFrames(File dir) throws IOException {
        for (int i = 0; i < 3; i++) {
            BufferedImage img = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 120; y++) {
                for (int x = 0; x < 120; x++) {
                    boolean ink = x >= 30 + i && x < 70 + i && y >= 20 && y < 90;
                    img.setRGB(x, y, ink ? 0x1A1E26 : 0xDCDAD2);
                }
            }
            ImageIO.write(img, "png", new File(dir, String.format("frame_%03d.png", i)));
        }
    }
}
