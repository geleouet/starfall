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
                default -> { }
            }
        }
        return spec;
    }
}
