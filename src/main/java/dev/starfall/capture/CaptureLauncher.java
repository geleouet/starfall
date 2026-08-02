package dev.starfall.capture;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Entry point for the automated visual feedback loop.
 *
 * <p>Runs the requested scene in a hidden window so it never steals focus, but
 * still on a real GL context — libGDX's truly headless backend has no GL at all,
 * which would defeat the purpose of capturing shader output.
 */
public final class CaptureLauncher {

    public static void main(String[] args) {
        CaptureSpec spec = CaptureSpec.parse(args);
        spec.outDir.mkdirs();
        // Thrown before the scene is built, because a control has to be a control from the
        // warmup onward: a garment that simulated for its warmup and then froze would carry
        // whatever drape the breeze had already put into it.
        dev.starfall.sim.ClothSim.clampRigid("cloth".equals(spec.clamp));

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("starfall-capture");
        config.setWindowedMode(Math.min(spec.width, 1280), Math.min(spec.height, 720));
        config.setInitialVisible(false);
        config.setResizable(false);
        config.useVsync(false);
        config.setForegroundFPS(0);
        config.setIdleFPS(0);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);

        System.out.println("CAPTURE_SCENE=" + spec.sceneName);
        System.out.println("CAPTURE_OUT=" + spec.outDir.getAbsolutePath());

        new Lwjgl3Application(new CaptureApp(spec), config);
    }
}
