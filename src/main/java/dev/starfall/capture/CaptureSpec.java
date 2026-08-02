package dev.starfall.capture;

import java.io.File;

/** Parsed capture-run configuration. */
public final class CaptureSpec {

    public String sceneName = "smoke";
    public File outDir = new File("out/captures/latest");
    public int frames = 12;
    public int cols = 4;
    public int width = 960;
    public int height = 540;
    public String label;

    /**
     * Seconds to skip past the scene's own warmup before the first captured frame.
     * Lets a capture zoom in on a specific moment rather than always starting at t=0.
     */
    public float start = 0f;

    /**
     * Fixed interval between captured frames, in seconds. When &gt; 0 this overrides the
     * default of spreading {@code frames} evenly across the scene's whole duration.
     *
     * <p>This exists because that default made timing ungradeable. A 12-frame capture of a
     * 3.6s scene samples every 0.327s, while STYLE.md §7.1 specifies overlapping action in
     * the 4-8 frame band at 60Hz — 0.067 to 0.133s. Every lag the motion systems are
     * required to produce is shorter than one such sample, so a review literally cannot see
     * whether the chain staggers or arrives as a unit. Passing {@code -Pstep=0.0167} with a
     * {@code -Pstart} offset captures a short window at true 60Hz, which is the only way to
     * grade §7.0.3.
     */
    public float step = 0f;

    /**
     * Simulation clamp for control captures. {@code "cloth"} welds every cloth chain to its
     * bind pose; empty runs the scene normally.
     *
     * <p>STYLE.md §7.1 requires a rigid control before any cloth lag is quoted, and §7.1's
     * drape-excursion gate 3 states one as a pass condition ({@code rigid control ≤ 0.15×}).
     * Until this flag existed the repository could not run one, which is the failure §11.3
     * exists to prevent committed by the section that warns about it. It is recorded in
     * {@code capture.txt} like every other parameter, so a control capture cannot be mistaken
     * for a live one later.
     */
    public String clamp = "";

    public static CaptureSpec parse(String[] args) {
        CaptureSpec spec = new CaptureSpec();
        for (int i = 0; i < args.length - 1; i++) {
            String key = args[i];
            String value = args[i + 1];
            switch (key) {
                case "--scene" -> { spec.sceneName = value; i++; }
                case "--out" -> { spec.outDir = new File(value); i++; }
                case "--frames" -> { spec.frames = Integer.parseInt(value); i++; }
                case "--cols" -> { spec.cols = Integer.parseInt(value); i++; }
                case "--w" -> { spec.width = Integer.parseInt(value); i++; }
                case "--h" -> { spec.height = Integer.parseInt(value); i++; }
                case "--label" -> { spec.label = value; i++; }
                case "--start" -> { spec.start = Float.parseFloat(value); i++; }
                case "--step" -> { spec.step = Float.parseFloat(value); i++; }
                case "--clamp" -> { spec.clamp = value; i++; }
                default -> { }
            }
        }
        return spec;
    }
}
