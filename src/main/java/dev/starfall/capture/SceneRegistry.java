package dev.starfall.capture;

import dev.starfall.rig.IkDebugScene;
import dev.starfall.rig.IkScene;
import dev.starfall.rig.IkTargetScript;
import dev.starfall.rig.RigBindposeScene;
import dev.starfall.rig.RigBonesScene;
import dev.starfall.rig.RigSwingScene;
import dev.starfall.rig.SimDebugScene;
import dev.starfall.rig.SimScene;
import dev.starfall.rig.SimScript;

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

        // System 2. Each IK scene has a matching -debug variant: same driver, same
        // targets, same frames, drawn with ShapeRenderer so the target and the
        // pole are visible. The graded capture cannot show either of them, and
        // most of what goes wrong in an IK scene is a question about the target.
        for (IkTargetScript.Kind kind : IkTargetScript.Kind.values()) {
            String name = "ik-" + kind.name().toLowerCase();
            register(name, () -> new IkScene(kind, name));
            register(name + "-debug", () -> new IkDebugScene(kind, name + "-debug"));
        }

        // System 3. Same arrangement, same reason: sim-*-debug draws the
        // particles and constraints the graded capture deliberately hides. Both
        // the easy scene and the hard one get the instrumentation from the first
        // pass, which is System 2's debt item E5 paid up front.
        for (SimScript.Kind kind : SimScript.Kind.values()) {
            String name = "sim-" + kind.name().toLowerCase();
            register(name, () -> new SimScene(kind, name));
            register(name + "-debug", () -> new SimDebugScene(kind, name + "-debug"));
        }
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
