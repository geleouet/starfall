package dev.starfall.capture;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.util.Locale;

/**
 * Entry point for the headless timing measurement.
 *
 * <p>Runs in a hidden window for the same reason {@link CaptureLauncher} does: libGDX's truly
 * headless backend has no GL context, and a timing measurement taken without the shaders that
 * draw the figure would not be measuring the figure.
 *
 * <pre>
 *   ./gw timing -Pscene=sim-reversal -Pstart=1.0 -Pduration=0.4 -Prate=240 \
 *               -Pout=out/timing/s3-reversal.json
 * </pre>
 */
public final class TimingLauncher {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        TimingApp.TimingSpec spec = TimingApp.TimingSpec.parse(args);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("starfall-timing");
        config.setWindowedMode(Math.min(spec.width, 1280), Math.min(spec.height, 720));
        config.setInitialVisible(false);
        config.setResizable(false);
        config.useVsync(false);
        config.setForegroundFPS(0);
        config.setIdleFPS(0);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);

        System.out.println("TIMING_SCENE=" + spec.sceneName);
        System.out.printf("TIMING_WINDOW=%.4f..%.4f s%n", spec.start, spec.start + spec.duration);

        new Lwjgl3Application(new TimingApp(spec), config);
    }
}
