package com.starfall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.starfall.render.PixelFont;
import com.starfall.render.PixelPainter;
import com.starfall.render.PixelViewport;
import com.starfall.render.WindowedSize;
import com.starfall.render.SpriteAtlas;
import com.starfall.scene.ArenaScene;
import com.starfall.scene.CalibrationScene;
import com.starfall.scene.CaptureScript;
import com.starfall.scene.Scene;
import com.starfall.scene.SceneContext;
import com.starfall.scene.RiposteScript;
import com.starfall.scene.SalvoScript;
import com.starfall.scene.ShowcaseScript;

import java.util.List;

/**
 * Le jeu : la fenêtre, le viewport pixel-perfect, le bandeau d'interface et la capture d'écran.
 *
 * <p>Le contenu, lui, appartient à une {@link Scene} — {@link ArenaScene} pour le jeu,
 * {@link CalibrationScene} pour la mire de contrôle des jalons précédents.
 *
 * <p>L'unité de monde est le pixel-monde, la résolution à laquelle les sprites sont dessinés.
 */
public class StarfallGame extends ApplicationAdapter {

    /** Zone garantie visible, en pixels-monde. Jamais rognée, quelle que soit la forme de la fenêtre. */
    public static final int MIN_WORLD_WIDTH = 320;
    public static final int MIN_WORLD_HEIGHT = 180;
    /** Hauteur de référence d'un sprite de personnage, en pixels-monde. */
    public static final int CHARACTER_HEIGHT = 32;
    public static final int CHARACTER_WIDTH = 16;

    /** Bord supérieur des motifs de la mire de calibration, en pixels-monde. */
    public static final int PATTERN_TOP = 112;

    /** Code de sortie signalant une capture qui n'a pas produit ce qui était demandé. */
    public static final int EXIT_CAPTURE_MISMATCH = 3;

    /** Pas de temps fixe entre deux images capturées : le mode capture doit rester reproductible. */
    private static final float SCREENSHOT_TIME_STEP = 0.37f;

    private static final Color BACKDROP = new Color(0x0b1020ff);
    private static final Color FRAME = new Color(0xffcc33ff);
    private static final Color PANEL = new Color(0x000000d0);

    private final LaunchOptions options;
    private final Scene scene;

    private SpriteBatch batch;
    private PixelPainter painter;
    private PixelFont font;
    private SpriteAtlas atlas;
    private PixelViewport viewport;
    private final Matrix4 uiProjection = new Matrix4();

    private ScreenshotRecorder recorder;
    private boolean finished;
    private int exitCode;

    private float time;

    public StarfallGame(LaunchOptions options) {
        this.options = options;
        this.windowed = new WindowedSize(options.width, options.height);
        this.scene = sceneFor(options.scene);
    }

    // Visible pour le test : SceneNamesTest recopiait ce switch a la main faute de pouvoir
    // l'appeler, si bien qu'un aiguillage rendant la mauvaise scene laissait les 491 tests verts.
    // Un pont qui recopie ce qu'il relie ne relie rien.
    static Scene sceneFor(String name) {
        // La ligne de commande a déjà refusé tout autre nom ; ce défaut n'existe que pour qu'un
        // ajout de scène oublié ici échoue bruyamment plutôt que d'afficher la mauvaise.
        return switch (name) {
            case CaptureScript.SCENE_NAME -> new ArenaScene();
            // La vitrine rejoue la MEME scene, sur un autre scenario : ce qui est garde est le
            // rendu du jeu, pas une maquette a cote.
            case ShowcaseScript.SCENE_NAME -> new ArenaScene(ShowcaseScript.SCENARIO);
            // Et la salve qui se deroule : meme scene encore, sur un scenario qui montre un TEMPS
            // par image au lieu d'un geste. C'est le seul temoin possible des deux regles de
            // l'animation, que le mode capture ne pouvait pas atteindre jusqu'ici.
            case SalvoScript.SCENE_NAME -> new ArenaScene(SalvoScript.SCENARIO);
            // Et le pas suivi de sa riposte : le geste le plus frequent du jeu, et la reponse
            // qu il paie. La ligne de la salve ne pouvait montrer ni l un ni l autre - sa
            // derniere tuile ne deplace personne, et l ennemi qui lui survit non plus.
            case RiposteScript.SCENE_NAME -> new ArenaScene(RiposteScript.SCENARIO);
            case CalibrationScene.SCENE_NAME -> new CalibrationScene();
            default -> throw new IllegalArgumentException("Scène inconnue : " + name);
        };
    }

    /** Code de sortie à propager au processus. Zéro tant que rien n'a échoué. */
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        painter = new PixelPainter(batch);
        font = new PixelFont();
        // Volontairement sans filet : si l'atlas manque ou ne correspond pas à son index, le jeu
        // s'arrête avec un message clair plutôt que d'afficher une scène amputée.
        atlas = SpriteAtlas.load();

        viewport = new PixelViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT);
        viewport.setCameraTarget(MIN_WORLD_WIDTH / 2f, MIN_WORLD_HEIGHT / 2f);
        viewport.update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);

        scene.create(new SceneContext(batch, painter, atlas, viewport, font, options));

        if (options.isScreenshotMode()) {
            recorder = new ScreenshotRecorder(options.screenshotDir, options.frames,
                    options.firstFrame, options.width, options.height);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        scene.resize(width, height);
        windowed.remember(width, height, Gdx.graphics.isFullscreen());
    }

    /**
     * Dernière taille de la fenêtre avant le plein écran.
     *
     * <p>La règle vit dans {@link WindowedSize}, hors de la couche fenêtre, parce qu'il n'y a rien
     * de graphique dans « quelle taille rendre » — et parce qu'un correctif qu'on ne peut pas
     * tester est un correctif qu'on croit sur parole.
     */
    private final WindowedSize windowed;

    @Override
    public void render() {
        int screenWidth = Math.max(1, Gdx.graphics.getBackBufferWidth());
        int screenHeight = Math.max(1, Gdx.graphics.getBackBufferHeight());

        boolean interactive = !options.isScreenshotMode();
        handleWindowInput(interactive);
        updateTime();
        // La caméra est mise à jour AVANT que la scène n'agisse : sinon le pointage de la souris
        // déprojette avec la caméra de l'image précédente.
        viewport.setCameraTarget(scene.cameraTargetX(), scene.cameraTargetY());
        scene.act(time, frameIndex(), interactive);
        viewport.setCameraTarget(scene.cameraTargetX(), scene.cameraTargetY());

        Gdx.gl.glClearColor(BACKDROP.r, BACKDROP.g, BACKDROP.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        scene.drawWorld();
        batch.end();

        // Le viewport du monde déborde de la fenêtre ; l'interface, elle, veut la fenêtre réelle.
        // Les dimensions sont déjà en pixels du tampon arrière, donc pas de conversion HDPI ici.
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        drawOverlay(screenWidth, screenHeight);
        batch.end();

        if (recorder != null && !finished) {
            if (recorder.capture()) {
                finished = true;
                if (recorder.hasSizeMismatch()) {
                    exitCode = EXIT_CAPTURE_MISMATCH;
                }
                Gdx.app.exit();
            }
        }
    }

    /**
     * Numéro de l'image du <b>scénario</b>, ou 0 hors mode capture.
     *
     * <p>Ce n'est pas le compte d'images écrites : {@code --from} décale les deux. Rendre la fin
     * d'une partie de 88 images sans écrire les 88 demande de commencer le scénario plus loin que
     * le premier fichier.
     */
    private int frameIndex() {
        return recorder == null ? 0 : options.firstFrame + recorder.framesCaptured();
    }

    /**
     * En mode capture, le temps avance d'un pas fixe par image écrite plutôt qu'au rythme réel :
     * les images restent reproductibles d'une exécution à l'autre, mais elles diffèrent entre elles.
     */
    private void updateTime() {
        if (options.isScreenshotMode()) {
            time = frameIndex() * SCREENSHOT_TIME_STEP;
        } else {
            time += Gdx.graphics.getDeltaTime();
        }
    }

    /** Entrées qui appartiennent à la fenêtre, pas à la scène. */
    private void handleWindowInput(boolean interactive) {
        if (!interactive) {
            return; // images déterministes
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(windowed.width(), windowed.height());
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }
    }

    // ------------------------------------------------------------------ interface

    private void drawOverlay(int screenWidth, int screenHeight) {
        List<String> lines = scene.overlayLines(screenWidth, screenHeight);
        if (lines.isEmpty()) {
            return;
        }

        int textScale = MathUtils.clamp(screenHeight / 300, 1, 3);
        int margin = 4 * textScale;
        int lineStep = PixelFont.LINE_HEIGHT * textScale;

        // Budget vertical : le bandeau ne doit jamais mordre sur le contenu de la scène, qui vit en
        // coordonnées monde. On convertit son bord supérieur en pixels écran et on n'affiche que le
        // nombre de lignes qui tient au-dessus.
        int contentTopScreen = viewport.getScreenY()
                + (scene.contentTopWorldY() - viewport.getDrawnBottom()) * viewport.getScale();
        int verticalBudget = screenHeight - Math.max(0, contentTopScreen);

        int lineCount = lines.size();
        while (lineCount > 0 && lineCount * lineStep + 2 * margin > verticalBudget) {
            lineCount--;
        }
        if (lineCount == 0) {
            return;
        }

        // Budget horizontal : on tronque plutôt que de laisser le texte sortir de la fenêtre.
        int maxChars = Math.max(0, (screenWidth - 2 * margin) / (PixelFont.ADVANCE * textScale));
        if (maxChars == 0) {
            return;
        }

        int widest = 0;
        for (int i = 0; i < lineCount; i++) {
            widest = Math.max(widest, font.width(truncate(lines.get(i), maxChars), textScale));
        }
        int panelWidth = Math.min(screenWidth, widest + 2 * margin);
        int panelHeight = lineCount * lineStep + 2 * margin;

        painter.fill(0, screenHeight - panelHeight, panelWidth, panelHeight, PANEL);
        painter.fill(0, screenHeight - panelHeight, panelWidth, textScale, FRAME);

        int y = screenHeight - margin;
        for (int i = 0; i < lineCount; i++) {
            batch.setColor(i == 0 ? FRAME : Color.WHITE);
            font.draw(batch, truncate(lines.get(i), maxChars), margin, y, textScale);
            y -= lineStep;
        }
        batch.setColor(Color.WHITE);
    }

    private static String truncate(String line, int maxChars) {
        return line.length() <= maxChars ? line : line.substring(0, maxChars);
    }

    @Override
    public void dispose() {
        if (scene != null) {
            scene.dispose();
        }
        if (painter != null) {
            painter.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (atlas != null) {
            atlas.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
