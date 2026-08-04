package com.starfall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.starfall.render.PixelFont;
import com.starfall.render.PixelViewport;

/**
 * Scène de test du jalon M1 : tout ce qui est à l'écran est là pour qu'une simple capture suffise à
 * juger si la grille de pixels est intacte.
 *
 * <p>L'unité de monde est le pixel-monde — la résolution à laquelle les sprites sont dessinés. Un
 * personnage fait {@link #CHARACTER_HEIGHT} pixels-monde de haut.
 */
public class StarfallGame extends ApplicationAdapter {

    /** Zone garantie visible, en pixels-monde. Jamais rognée, quelle que soit la forme de la fenêtre. */
    public static final int MIN_WORLD_WIDTH = 320;
    public static final int MIN_WORLD_HEIGHT = 180;
    /** Hauteur de référence d'un sprite de personnage, en pixels-monde. */
    public static final int CHARACTER_HEIGHT = 32;
    public static final int CHARACTER_WIDTH = 16;

    /** Code de sortie signalant une capture qui n'a pas produit ce qui était demandé. */
    public static final int EXIT_CAPTURE_MISMATCH = 3;

    /** Pas de temps fixe entre deux images capturées : le mode capture doit rester reproductible. */
    private static final float SCREENSHOT_TIME_STEP = 0.37f;

    private static final Color OUTSIDE = new Color(0x0b1020ff);
    private static final Color INSIDE = new Color(0x1b2743ff);
    private static final Color FRAME = new Color(0xffcc33ff);
    private static final Color CHECKER_A = new Color(0xf4f7ffff);
    private static final Color CHECKER_B = new Color(0x27324eff);
    private static final Color ACCENT = new Color(0x54d6ffff);
    private static final Color HOT = new Color(0xff5c6aff);
    private static final Color GREEN = new Color(0x7be08aff);
    private static final Color DOTS = new Color(0x3d5288ff);
    private static final Color PANEL = new Color(0x000000d0);

    /**
     * Bord supérieur des motifs de test, en pixels-monde. Le panneau d'interface est ancré en haut
     * de la fenêtre et vit en coordonnées écran : il est explicitement borné pour ne jamais
     * descendre sous cette ligne, quitte à afficher moins de lignes de texte.
     */
    private static final int PATTERN_TOP = 112;

    // Disposition de la scène, en pixels-monde.
    private static final int CHECKER_X = 8;
    private static final int CHECKER_Y = 62;
    private static final int CHECKER_W = 96;
    private static final int CHECKER_H = 48;
    private static final int CHECKER2_X = 232;
    private static final int CHECKER2_Y = 20;
    private static final int CHECKER2_W = 80;
    private static final int CHECKER2_H = 40;

    private final LaunchOptions options;

    private SpriteBatch batch;
    private Texture white;
    private Texture checker;
    private Texture checker2;
    private PixelFont font;
    private PixelViewport viewport;
    private final Matrix4 uiProjection = new Matrix4();

    private ScreenshotRecorder recorder;
    private boolean finished;
    private int exitCode;

    private boolean scrolling;
    private float time;

    public StarfallGame(LaunchOptions options) {
        this.options = options;
    }

    /** Code de sortie à propager au processus. Zéro tant que rien n'a échoué. */
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        white = solidTexture();
        checker = checkerTexture(CHECKER_W, CHECKER_H, 1);
        checker2 = checkerTexture(CHECKER2_W, CHECKER2_H, 2);
        font = new PixelFont();

        viewport = new PixelViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT);
        viewport.setCameraTarget(MIN_WORLD_WIDTH / 2f, MIN_WORLD_HEIGHT / 2f);
        viewport.update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);

        if (options.isScreenshotMode()) {
            recorder = new ScreenshotRecorder(options.screenshotDir, options.frames, options.width, options.height);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void render() {
        int screenWidth = Math.max(1, Gdx.graphics.getBackBufferWidth());
        int screenHeight = Math.max(1, Gdx.graphics.getBackBufferHeight());

        handleInput();
        updateCamera();

        Gdx.gl.glClearColor(OUTSIDE.r, OUTSIDE.g, OUTSIDE.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        drawWorld();
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
     * En mode capture, le temps avance d'un pas fixe par image écrite plutôt qu'au rythme réel :
     * les images restent reproductibles d'une exécution à l'autre, mais elles diffèrent entre elles.
     * La première est cadrée au repos — c'est la planche de référence — et les suivantes montrent la
     * caméra en mouvement sur des cibles fractionnaires, ce qui est justement ce que l'alignement sur
     * la grille doit absorber.
     */
    private void updateCamera() {
        if (options.isScreenshotMode()) {
            int frame = recorder == null ? 0 : recorder.framesCaptured();
            scrolling = frame > 0;
            time = frame * SCREENSHOT_TIME_STEP;
        } else if (scrolling) {
            time += Gdx.graphics.getDeltaTime();
        }

        if (scrolling) {
            viewport.setCameraTarget(
                    MIN_WORLD_WIDTH / 2f + MathUtils.sin(time * 0.6f) * 24f,
                    MIN_WORLD_HEIGHT / 2f + MathUtils.cos(time * 0.4f) * 10f);
        } else {
            viewport.setCameraTarget(MIN_WORLD_WIDTH / 2f, MIN_WORLD_HEIGHT / 2f);
        }
    }

    private void handleInput() {
        if (options.isScreenshotMode()) {
            return; // images déterministes
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            scrolling = !scrolling;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(options.width, options.height);
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        }
    }

    // ------------------------------------------------------------------ monde

    private void drawWorld() {
        // On couvre la zone dessinée, débordement compris, pour qu'aucune gouttière ne reste noire.
        int left = viewport.getDrawnLeft();
        int bottom = viewport.getDrawnBottom();
        int right = left + viewport.getDrawnWorldWidth();
        int top = bottom + viewport.getDrawnWorldHeight();

        // La zone garantie a son propre fond, pour que l'élargissement adaptatif se voie.
        fill(0, 0, MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, INSIDE);

        // Trame de points d'1 px sur tout le monde visible, dedans comme dehors : chaque point doit
        // être un carré de taille identique, où qu'il tombe.
        batch.setColor(DOTS);
        for (int x = MathUtils.floor(left / 8f) * 8; x <= right; x += 8) {
            for (int y = MathUtils.floor(bottom / 8f) * 8; y <= top; y += 8) {
                batch.draw(white, x, y, 1, 1);
            }
        }

        drawCheckerboards();
        drawStripes();
        drawNestedRectangles();
        drawDiagonals();
        drawCharacters();
        drawRuler();
        drawCrosshair();

        // Contour d'1 px cadrant exactement la zone garantie minimale.
        outline(0, 0, MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, FRAME);

        batch.setColor(Color.WHITE);
    }

    /**
     * Le test de référence : un damier dont les cases font exactement un pixel-monde, à côté d'un
     * damier à deux. Si un pixel-monde était mis à l'échelle de travers, le damier d'1 px ferait
     * immédiatement apparaître des lignes ou des colonnes d'une autre largeur.
     */
    private void drawCheckerboards() {
        batch.setColor(Color.WHITE);
        batch.draw(checker, CHECKER_X, CHECKER_Y, CHECKER_W, CHECKER_H);
        outline(CHECKER_X - 2, CHECKER_Y - 2, CHECKER_W + 4, CHECKER_H + 4, ACCENT);

        batch.setColor(Color.WHITE);
        batch.draw(checker2, CHECKER2_X, CHECKER2_Y, CHECKER2_W, CHECKER2_H);
        outline(CHECKER2_X - 2, CHECKER2_Y - 2, CHECKER2_W + 4, CHECKER2_H + 4, GREEN);
    }

    /** Rayures d'1 px, verticales puis horizontales : le détecteur classique d'échelle inégale. */
    private void drawStripes() {
        int y0 = 66;
        int x0 = 108;
        for (int i = 0; i < 30; i += 2) {
            fill(x0 + i, y0, 1, 40, ACCENT);
        }
        int x1 = 182;
        for (int i = 0; i < 40; i += 2) {
            fill(x1, y0 + i, 30, 1, GREEN);
        }
        outline(x0 - 2, y0 - 2, 34, 44, FRAME);
        outline(x1 - 2, y0 - 2, 34, 44, FRAME);
    }

    private void drawNestedRectangles() {
        int cx = 270;
        int cy = 86;
        int[] sizes = {48, 38, 28, 18, 8};
        Color[] colors = {FRAME, ACCENT, HOT, GREEN, CHECKER_A};
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            outline(cx - size / 2, cy - size / 2, size, size, colors[i]);
        }
    }

    private void drawDiagonals() {
        line(76, 20, 112, 56, HOT);         // 45 degrés : un pixel par pas
        line(118, 20, 198, 40, GREEN);      // 4:1 - longues séries régulières, un raté se voit
        line(204, 20, 220, 54, ACCENT);     // pente raide
    }

    /** Silhouettes de référence à la taille de personnage retenue (16x32 pixels-monde). */
    private void drawCharacters() {
        int y = 22;
        for (int i = 0; i < 3; i++) {
            int x = 8 + i * 22;
            fill(x, y, CHARACTER_WIDTH, CHARACTER_HEIGHT, CHECKER_B);
            outline(x, y, CHARACTER_WIDTH, CHARACTER_HEIGHT, i == 1 ? HOT : ACCENT);
            fill(x + 4, y + CHARACTER_HEIGHT - 8, 3, 3, CHECKER_A);   // yeux : 3x3 pixels-monde
            fill(x + 9, y + CHARACTER_HEIGHT - 8, 3, 3, CHECKER_A);
        }
    }

    /** Une règle graduée d'1 px tous les 4 pixels-monde, en bas de la zone garantie. */
    private void drawRuler() {
        int baseY = 8;
        fill(8, baseY, MIN_WORLD_WIDTH - 16, 1, FRAME);
        for (int x = 8; x < MIN_WORLD_WIDTH - 8; x += 4) {
            int height = (x % 32 == 8) ? 9 : (x % 16 == 8 ? 6 : 3);
            fill(x, baseY + 1, 1, height, x % 32 == 8 ? HOT : ACCENT);
        }
    }

    private void drawCrosshair() {
        int cx = MIN_WORLD_WIDTH / 2;
        int cy = MIN_WORLD_HEIGHT / 2;
        fill(cx - 6, cy, 13, 1, CHECKER_A);
        fill(cx, cy - 6, 1, 13, CHECKER_A);
    }

    // ------------------------------------------------------------------ interface

    private void drawOverlay(int screenWidth, int screenHeight) {
        int s = viewport.getScale();
        String[] lines = {
                "STARFALL - JALON M1 - VUE PIXEL PARFAITE",
                "FENÊTRE : " + screenWidth + " x " + screenHeight + " PX ÉCRAN",
                "ÉCHELLE ENTIÈRE : x" + s + "   (1 PX-MONDE = " + s + "x" + s + " PX ÉCRAN)",
                "ZONE SÛRE : " + viewport.getSafeWorldWidth() + " x " + viewport.getSafeWorldHeight()
                        + " PX-MONDE   ZONE GARANTIE : " + MIN_WORLD_WIDTH + " x " + MIN_WORLD_HEIGHT + " (CADRE OR)",
                "CAMÉRA : X=" + viewport.getSafeLeft() + " Y=" + viewport.getSafeBottom()
                        + "   DAMIERS : 1 ET 2 PX-MONDE   PERSONNAGE : "
                        + CHARACTER_WIDTH + "x" + CHARACTER_HEIGHT,
                "ESPACE : DÉFILEMENT   ÉCHAP : QUITTER   F11 : PLEIN ÉCRAN",
        };

        int textScale = MathUtils.clamp(screenHeight / 300, 1, 3);
        int margin = 4 * textScale;
        int lineStep = PixelFont.LINE_HEIGHT * textScale;

        // Budget vertical : le panneau ne doit jamais mordre sur les motifs de test, qui vivent en
        // coordonnées monde. On convertit leur bord supérieur en pixels écran et on n'affiche que le
        // nombre de lignes qui tient au-dessus. Sans cela, à 320x180 - la taille minimale que le
        // lanceur impose lui-même - le panneau recouvrait près de la moitié de la zone garantie.
        int patternTopScreen = viewport.getScreenY()
                + (PATTERN_TOP - viewport.getDrawnBottom()) * viewport.getScale();
        int verticalBudget = screenHeight - Math.max(0, patternTopScreen);

        int lineCount = lines.length;
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
            widest = Math.max(widest, font.width(truncate(lines[i], maxChars), textScale));
        }
        int panelWidth = Math.min(screenWidth, widest + 2 * margin);
        int panelHeight = lineCount * lineStep + 2 * margin;

        batch.setColor(PANEL);
        batch.draw(white, 0, screenHeight - panelHeight, panelWidth, panelHeight);
        batch.setColor(FRAME);
        batch.draw(white, 0, screenHeight - panelHeight, panelWidth, textScale);

        int y = screenHeight - margin;
        for (int i = 0; i < lineCount; i++) {
            batch.setColor(i == 0 ? FRAME : Color.WHITE);
            font.draw(batch, truncate(lines[i], maxChars), margin, y, textScale);
            y -= lineStep;
        }
        batch.setColor(Color.WHITE);
    }

    private static String truncate(String line, int maxChars) {
        return line.length() <= maxChars ? line : line.substring(0, maxChars);
    }

    // ------------------------------------------------------------------ primitives

    private void fill(int x, int y, int width, int height, Color color) {
        batch.setColor(color);
        batch.draw(white, x, y, width, height);
    }

    /** Contour de rectangle large d'un pixel-monde. */
    private void outline(int x, int y, int width, int height, Color color) {
        if (width < 1 || height < 1) {
            return;
        }
        batch.setColor(color);
        batch.draw(white, x, y, width, 1);
        if (height < 2) {
            return; // un rectangle d'une seule ligne est déjà entièrement dessiné
        }
        batch.draw(white, x, y + height - 1, width, 1);
        if (height > 2) {
            batch.draw(white, x, y + 1, 1, height - 2);
            batch.draw(white, x + width - 1, y + 1, 1, height - 2);
        }
    }

    /** Ligne de Bresenham tracée un pixel-monde à la fois - pas de rastérisation GL. */
    private void line(int x0, int y0, int x1, int y1, Color color) {
        batch.setColor(color);
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0;
        int y = y0;
        while (true) {
            batch.draw(white, x, y, 1, 1);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static Texture solidTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private static Texture checkerTexture(int width, int height, int cell) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean even = ((x / cell + y / cell) & 1) == 0;
                pixmap.setColor(even ? CHECKER_A : CHECKER_B);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (white != null) {
            white.dispose();
        }
        if (checker != null) {
            checker.dispose();
        }
        if (checker2 != null) {
            checker2.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
