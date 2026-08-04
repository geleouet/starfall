package com.starfall;

/**
 * Command line options, shared by the launcher and the game.
 *
 * <pre>
 *   --screenshot &lt;outputDir&gt;   render a few frames, write a PNG each, then exit(0)
 *   --size &lt;W&gt;x&lt;H&gt;             initial window size (default 1280x720)
 *   --frames &lt;N&gt;               number of frames to render in screenshot mode (default 2)
 * </pre>
 */
public final class LaunchOptions {

    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int DEFAULT_FRAMES = 2;

    /** Output directory for screenshots, or {@code null} when running normally. */
    public final String screenshotDir;
    public final int width;
    public final int height;
    public final int frames;

    private LaunchOptions(String screenshotDir, int width, int height, int frames) {
        this.screenshotDir = screenshotDir;
        this.width = width;
        this.height = height;
        this.frames = frames;
    }

    public boolean isScreenshotMode() {
        return screenshotDir != null;
    }

    public static LaunchOptions parse(String[] args) {
        String screenshotDir = null;
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        int frames = DEFAULT_FRAMES;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--screenshot":
                        screenshotDir = requireValue(args, ++i, arg);
                        break;
                    case "--size": {
                        String value = requireValue(args, ++i, arg).toLowerCase(java.util.Locale.ROOT);
                        int x = value.indexOf('x');
                        if (x <= 0 || x == value.length() - 1) {
                            throw new IllegalArgumentException("--size expects WxH, got: " + value);
                        }
                        width = Math.max(1, Integer.parseInt(value.substring(0, x).trim()));
                        height = Math.max(1, Integer.parseInt(value.substring(x + 1).trim()));
                        break;
                    }
                    case "--frames":
                        frames = Math.max(1, Integer.parseInt(requireValue(args, ++i, arg)));
                        break;
                    case "--help":
                    case "-h":
                        System.out.println(usage());
                        break;
                    default:
                        System.err.println("[Starfall] Unknown argument ignored: " + arg);
                        break;
                }
            }
        }

        return new LaunchOptions(screenshotDir, width, height, frames);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " requires a value");
        }
        return args[index];
    }

    public static String usage() {
        return "Starfall\n"
                + "  --screenshot <dir>   render frames to PNG in <dir> then exit\n"
                + "  --size <W>x<H>       window size (default " + DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT + ")\n"
                + "  --frames <N>         frames to capture in screenshot mode (default " + DEFAULT_FRAMES + ")\n";
    }

    @Override
    public String toString() {
        return "LaunchOptions{screenshotDir=" + screenshotDir
                + ", size=" + width + "x" + height
                + ", frames=" + frames + '}';
    }
}
