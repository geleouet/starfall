package com.starfall.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.starfall.LaunchOptions;
import com.starfall.StarfallGame;

/** Desktop entry point. */
public final class Lwjgl3Launcher {

    private Lwjgl3Launcher() {
    }

    public static void main(String[] args) {
        LaunchOptions options;
        try {
            options = LaunchOptions.parse(args);
        } catch (RuntimeException e) {
            System.err.println("[Starfall] " + e.getMessage());
            System.err.println(LaunchOptions.usage());
            System.exit(2);
            return;
        }

        System.out.println("[Starfall] " + options);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Starfall");
        config.setWindowedMode(options.width, options.height);
        config.setResizable(true);
        // Never let the window shrink below one screen pixel per world pixel.
        config.setWindowSizeLimits(StarfallGame.MIN_WORLD_WIDTH, StarfallGame.MIN_WORLD_HEIGHT, -1, -1);
        // Logical coordinates == physical pixels: mandatory for pixel-perfect rendering.
        config.setHdpiMode(HdpiMode.Pixels);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);
        config.useVsync(!options.isScreenshotMode());
        config.setForegroundFPS(options.isScreenshotMode() ? 0 : 60);
        if (options.isScreenshotMode()) {
            config.setIdleFPS(0); // never throttle: the capture must not wait on the window manager
        }

        int exitCode = 0;
        try {
            new Lwjgl3Application(new StarfallGame(options), config);
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        }
        System.out.flush();
        // LWJGL leaves non-daemon threads behind; exit explicitly so the capture pipeline never hangs.
        System.exit(exitCode);
    }
}
