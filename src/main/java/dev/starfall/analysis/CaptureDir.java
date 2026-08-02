package dev.starfall.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A capture directory: its {@code frame_NNN.png} sequence in order, and whatever metadata the
 * harness left beside them.
 *
 * <p>Frames are loaded lazily in a batch rather than one at a time because every sequence
 * measurement — registration, centroid tracking, frame diffs — needs the whole window
 * resident, and a 24-frame 960x540 window is 50 MB of doubles, which is fine.
 */
public final class CaptureDir {

    public final File dir;
    public final List<File> frameFiles;

    private CaptureDir(File dir, List<File> frameFiles) {
        this.dir = dir;
        this.frameFiles = List.copyOf(frameFiles);
    }

    public static CaptureDir of(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException("not a directory: " + dir);
        }
        File[] all = dir.listFiles((d, n) -> n.startsWith("frame_") && n.endsWith(".png"));
        if (all == null || all.length == 0) {
            throw new IOException("no frame_NNN.png files in " + dir);
        }
        List<File> files = new ArrayList<>(Arrays.asList(all));
        files.sort(Comparator.comparing(File::getName));
        return new CaptureDir(dir, files);
    }

    public int size() {
        return frameFiles.size();
    }

    public File frameFile(int i) {
        return frameFiles.get(i);
    }

    public Frame frame(int i) throws IOException {
        return Frame.load(frameFiles.get(i));
    }

    public List<Frame> loadAll() throws IOException {
        List<Frame> out = new ArrayList<>(frameFiles.size());
        for (File f : frameFiles) {
            out.add(Frame.load(f));
        }
        return out;
    }

    /** The contact sheet, if the harness wrote one. */
    public File contactSheet() {
        File f = new File(dir, "contact-sheet.png");
        return f.isFile() ? f : null;
    }

    public String name() {
        return dir.getName();
    }

    /**
     * The {@code capture.txt} the harness writes beside the frames, parsed as {@code key=value}.
     *
     * <p>Empty when the file is absent, which is itself a fact a caller may need to refuse on:
     * STYLE.md §11.2b(e) is explicit that <i>"a discipline written into a document but not into
     * the tool that reads it is documentation, not a guard"</i>, and the example it gives is
     * exactly this file — {@code drape} wrote {@code clamp=} into the manifest and never read it
     * back, so a reviewer proved in one command that it would call a live capture a rigid
     * control. Reading it is half the fix; refusing on it is the other half, and that lives in
     * {@link AnalysisCli}.
     *
     * <p>Comment lines and the reproduce-command header are skipped. Keys are whatever the
     * harness wrote: {@code scene}, {@code frames}, {@code start}, {@code step}, {@code window},
     * {@code size}, {@code substep}, {@code clamp}, {@code clothRigid}, {@code commit},
     * {@code harness}.
     */
    public Map<String, String> manifest() {
        File f = new File(dir, "capture.txt");
        Map<String, String> out = new LinkedHashMap<>();
        if (!f.isFile()) {
            return out;
        }
        try {
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#") || s.startsWith("./gw")) {
                    continue;
                }
                int eq = s.indexOf('=');
                if (eq > 0) {
                    out.put(s.substring(0, eq).trim(), s.substring(eq + 1).trim());
                }
            }
        } catch (IOException e) {
            return out;
        }
        return out;
    }

    /** True when {@code capture.txt} exists. A capture without one cannot be compared with another. */
    public boolean hasManifest() {
        return new File(dir, "capture.txt").isFile();
    }
}
