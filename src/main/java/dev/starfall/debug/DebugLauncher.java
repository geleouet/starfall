package dev.starfall.debug;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.util.Locale;

/**
 * Starts the game with the debug API attached.
 *
 * <p><b>The API is off unless {@code --debug-server} is passed.</b> Running this class without
 * it exits with an explanation rather than silently opening a socket — a debug server that can
 * start by accident is a debug server that will eventually ship.
 *
 * <pre>
 *   ./gw debugServer -Pport=7671 -Pscene=sim-extreme
 *   ./gw debugServer -Pport=7671 -Pscene=sim-extreme -Pvisible   # watch it work
 * </pre>
 *
 * <p>Bound to loopback. Simulated time never advances on its own, so leaving the server
 * running costs nothing and the state a command sees is the state the previous command left.
 */
public final class DebugLauncher {

    public static final int DEFAULT_PORT = 7671;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        boolean enabled = false;
        int port = DEFAULT_PORT;
        String scene = "smoke";
        int width = 960;
        int height = 540;
        boolean visible = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--debug-server" -> enabled = true;
                case "--visible" -> visible = true;
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--scene" -> scene = args[++i];
                case "--w" -> width = Integer.parseInt(args[++i]);
                case "--h" -> height = Integer.parseInt(args[++i]);
                default -> { }
            }
        }

        if (!enabled) {
            System.err.println("""
                    The debug API did not start: --debug-server was not passed.

                    This is intentional. The API is a development-only control surface and must
                    never be reachable in a normal run, so it is opt-in at the process level
                    rather than behind a config file someone can forget to change.

                      ./gw debugServer -Pport=7671 -Pscene=sim-extreme
                    """);
            System.exit(2);
            return;
        }

        DebugApp app = new DebugApp(scene, width, height);
        DebugServer server = new DebugServer(app, port);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("starfall — debug API :" + port);
        config.setWindowedMode(Math.min(width, 1280), Math.min(height, 720));
        config.setInitialVisible(visible);
        config.setResizable(false);
        config.useVsync(false);
        // The render loop only drains the command queue and redraws the current state; it never
        // advances simulated time. 60 Hz is plenty and keeps an idle server off the CPU.
        config.setForegroundFPS(60);
        config.setIdleFPS(60);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);

        // The server can accept before the GL context exists; commands queue until it does.
        server.start();
        try {
            new Lwjgl3Application(app, config);
        } finally {
            server.stop();
        }
    }
}
