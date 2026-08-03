package dev.starfall.capture;

import dev.starfall.direct.Duel;
import dev.starfall.direct.DuelDebugScene;
import dev.starfall.direct.DuelScene;
import dev.starfall.rig.IkDebugScene;
import dev.starfall.rig.IkScene;
import dev.starfall.rig.IkTargetScript;
import dev.starfall.rig.RigBindposeScene;
import dev.starfall.rig.RigBonesScene;
import dev.starfall.rig.RigSwingScene;
import dev.starfall.rig.SimDebugScene;
import dev.starfall.rig.SimScene;
import dev.starfall.rig.SimScript;
import dev.starfall.ui.Bout;
import dev.starfall.ui.LaneScene;

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

        // System 4. Two figures, a real CombatEngine and a real Schedule -- the
        // first scenes in the project in which any of the layers below run
        // together. Same arrangement again, and the -debug siblings ship with the
        // graded ones rather than after them: system3-debt.md records "no -debug
        // siblings for any graded window, so the reviewer shot its own in order to
        // say anything about the chain at all" as a capture-hygiene defect.
        for (Duel.Kind kind : Duel.Kind.values()) {
            String name = kind.sceneName();
            register(name, () -> new DuelScene(kind, name));
            register(name + "-debug", () -> new DuelDebugScene(kind, name + "-debug"));
            // System 3b's control: the identical score with the face system not
            // drawn (STYLE.md 11.2b(g)), same arrangement as the lanes' -bare.
            register(name + "-bareface", () -> new DuelScene(kind, name + "-bareface", true));
        }

        // System 3b pass 2: six generated faces at push-in scale in one frame,
        // so 4b.6's variety claim is a picture rather than a parameter table
        // (the pass-1 review's ranked item 8).
        register("face-gallery", () -> new dev.starfall.direct.FaceGalleryScene("face-gallery"));

        // System 5. The same world, with an interface on it: the Ink Stanza written
        // up the margin, the Fold of the World drawn as ground, and the camera of
        // STYLE.md 9 driven by the phase the engine is actually in. Three lanes,
        // because combat-design.md 1.6 makes lane length the composition dial and a
        // dial cannot be shown at one setting.
        // Each ships with its own control: -bare is the identical score with the
        // interface not drawn, which is what STYLE.md 11.2b(g) asks for -- "say what
        // a number would read if the thing being measured were absent, and then
        // produce that case". It is a separate scene rather than a flag so that
        // capture.txt names it and a control can never be confused with a live shot.
        for (Bout.Kind kind : Bout.Kind.values()) {
            String name = kind.sceneName();
            register(name, () -> new LaneScene(kind, name));
            register(name + "-bare", () -> new LaneScene(kind, name + "-bare", true));
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
