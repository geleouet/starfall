package dev.starfall.capture;

import dev.starfall.rig.RigBindposeScene;
import dev.starfall.rig.RigBonesScene;
import dev.starfall.rig.RigSwingScene;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Name -> scene factory. Scenes register here so both the interactive launcher
 * and the capture harness can address them by string.
 */
public final class SceneRegistry {

    private static final Map<String, Supplier<Scene>> SCENES = new LinkedHashMap<>();

    static {
        register("smoke", SmokeScene::new);
        // System 1, contract section G. rig-bindpose/rig-swing need
        // dev.starfall.render (side B); rig-bones is ShapeRenderer-only and
        // buildable standalone.
        register("rig-bindpose", RigBindposeScene::new);
        register("rig-swing", RigSwingScene::new);
        register("rig-bones", RigBonesScene::new);
    }

    private SceneRegistry() {
    }

    public static void register(String name, Supplier<Scene> factory) {
        SCENES.put(name, factory);
    }

    public static Scene create(String name) {
        Supplier<Scene> factory = SCENES.get(name);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "Unknown scene '" + name + "'. Known scenes: " + String.join(", ", SCENES.keySet()));
        }
        return factory.get();
    }

    public static String[] names() {
        return SCENES.keySet().toArray(new String[0]);
    }
}
