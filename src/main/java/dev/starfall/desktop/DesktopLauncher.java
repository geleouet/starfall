package dev.starfall.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import dev.starfall.capture.Scene;
import dev.starfall.capture.SceneContext;
import dev.starfall.capture.SceneRegistry;

/** Interactive launcher — same scenes as the capture harness, in a real window. */
public final class DesktopLauncher {

    public static void main(String[] args) {
        String sceneName = args.length > 0 ? args[0] : "smoke";

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Starfall — " + sceneName);
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);

        new Lwjgl3Application(new SceneHost(sceneName), config);
    }

    private static final class SceneHost extends ApplicationAdapter {
        private final String sceneName;
        private Scene scene;

        SceneHost(String sceneName) {
            this.sceneName = sceneName;
        }

        @Override
        public void create() {
            scene = SceneRegistry.create(sceneName);
            scene.create(new SceneContext(
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false));
        }

        @Override
        public void resize(int width, int height) {
            scene.resize(width, height);
        }

        @Override
        public void render() {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            scene.update(Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f));
            scene.render();
        }

        @Override
        public void dispose() {
            if (scene != null) {
                scene.dispose();
            }
        }
    }
}
