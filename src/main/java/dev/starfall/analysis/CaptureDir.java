package dev.starfall.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
}
