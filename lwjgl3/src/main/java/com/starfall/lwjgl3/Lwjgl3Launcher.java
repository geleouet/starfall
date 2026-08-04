package com.starfall.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.starfall.LaunchOptions;
import com.starfall.StarfallGame;

/** Point d'entrée bureau. */
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

        if (options.helpRequested) {
            System.out.println(LaunchOptions.usage());
            System.exit(0);
            return;
        }

        System.out.println("[Starfall] " + options);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Starfall");
        config.setWindowedMode(options.width, options.height);
        config.setResizable(true);
        // Ne jamais laisser la fenêtre descendre sous un pixel écran par pixel-monde.
        config.setWindowSizeLimits(StarfallGame.MIN_WORLD_WIDTH, StarfallGame.MIN_WORLD_HEIGHT, -1, -1);
        // Coordonnées logiques == pixels physiques : indispensable pour un rendu pixel-perfect.
        config.setHdpiMode(HdpiMode.Pixels);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);
        config.useVsync(!options.isScreenshotMode());
        config.setForegroundFPS(options.isScreenshotMode() ? 0 : 60);
        if (options.isScreenshotMode()) {
            config.setIdleFPS(0); // jamais de bridage : la capture ne doit pas attendre le gestionnaire de fenêtres
        }

        StarfallGame game = new StarfallGame(options);
        int exitCode = 0;
        try {
            // Le constructeur bloque jusqu'à la fin de l'application : le code de sortie du jeu est
            // donc lisible juste après, et c'est lui qui signale une capture non conforme.
            new Lwjgl3Application(game, config);
            exitCode = game.getExitCode();
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        }
        System.out.flush();
        // LWJGL laisse des threads non démons : on sort explicitement pour que la chaîne de capture
        // ne reste jamais suspendue.
        System.exit(exitCode);
    }
}
