package com.starfall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.starfall.render.PixelFont;
import com.starfall.render.PixelViewport;

/**
 * Milestone M1 test scene: everything on screen is there to make it obvious, from a screenshot
 * alone, whether the pixel grid is intact.
 *
 * <p>World units are world pixels - the resolution the art is authored at. A character is
 * {@link #CHARACTER_HEIGHT} world pixels tall.
 */
public class StarfallGame extends ApplicationAdapter {

    /** Guaranteed visible area, in world pixels. Never cropped, whatever the window shape. */
    public static final int MIN_WORLD_WIDTH = 320;
    public static final int MIN_WORLD_HEIGHT = 180;
    /** Reference height of a character sprite, in world pixels. */
    public static final int CHARACTER_HEIGHT = 32;
    public static final int CHARACTER_WIDTH = 16;

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

    // Scene layout, in world pixels. Everything sits below y=112 so the screen-space overlay,
    // which is anchored to the top of the window, never hides a test pattern.
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

    private boolean scrolling;
    private float time;

    public StarfallGame(LaunchOptions options) {
        this.options = options;
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
            recorder = new ScreenshotRecorder(options.screenshotDir, options.frames);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiProjection.setToOrtho2D(0f, 0f, Math.max(1, width), Math.max(1, height));
    }

    @Override
    public void render() {
        int screenWidth = Gdx.graphics.getBackBufferWidth();
        int screenHeight = Gdx.graphics.getBackBufferHeight();

        handleInput();

        // A deliberately fractional camera target: the viewport snaps it to the world pixel grid,
        // which is what stops pixel art from shimmering while the camera moves.
        if (scrolling) {
            time += Gdx.graphics.getDeltaTime();
            viewport.setCameraTarget(
                    MIN_WORLD_WIDTH / 2f + MathUtils.sin(time * 0.6f) * 24f,
                    MIN_WORLD_HEIGHT / 2f + MathUtils.cos(time * 0.4f) * 10f);
        }

        Gdx.gl.glClearColor(OUTSIDE.r, OUTSIDE.g, OUTSIDE.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        drawWorld();
        batch.end();

        // The world viewport overscans by up to one world pixel; the overlay wants the real window.
        HdpiUtils.glViewport(0, 0, screenWidth, screenHeight);
        uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        drawOverlay(screenWidth, screenHeight);
        batch.end();

        if (recorder != null && !finished) {
            if (recorder.capture()) {
                finished = true;
                Gdx.app.exit();
            }
        }
    }

    private void handleInput() {
        if (options.isScreenshotMode()) {
            return; // deterministic frames
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

    // ------------------------------------------------------------------ world

    private void drawWorld() {
        int left = viewport.getCameraLeft();
        int bottom = viewport.getCameraBottom();
        int right = left + viewport.getVisibleWorldWidth();
        int top = bottom + viewport.getVisibleWorldHeight();

        // The guaranteed area gets its own background so the adaptive expansion is visible.
        fill(0, 0, MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, INSIDE);

        // 1px dot lattice over the whole visible world, inside and outside the guaranteed zone:
        // every dot must render as one square of identical size, wherever it lands.
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

        // 1px outline framing exactly the guaranteed minimum area.
        outline(0, 0, MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, FRAME);

        batch.setColor(Color.WHITE);
    }

    /**
     * The reference test: a checkerboard whose cells are exactly one world pixel, next to one whose
     * cells are two. If any world pixel were scaled unevenly, the 1px board would immediately show
     * rows or columns of a different width.
     */
    private void drawCheckerboards() {
        batch.setColor(Color.WHITE);
        batch.draw(checker, CHECKER_X, CHECKER_Y, CHECKER_W, CHECKER_H);
        outline(CHECKER_X - 2, CHECKER_Y - 2, CHECKER_W + 4, CHECKER_H + 4, ACCENT);

        batch.setColor(Color.WHITE);
        batch.draw(checker2, CHECKER2_X, CHECKER2_Y, CHECKER2_W, CHECKER2_H);
        outline(CHECKER2_X - 2, CHECKER2_Y - 2, CHECKER2_W + 4, CHECKER2_H + 4, GREEN);
    }

    /** 1px stripes, vertical then horizontal - the classic uneven-scaling detector. */
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
        line(76, 20, 112, 56, HOT);         // 45 degrees: one pixel per step
        line(118, 20, 198, 40, GREEN);      // 4:1 - long even runs, easy to spot a stutter
        line(204, 20, 220, 54, ACCENT);     // steep
    }

    /** Reference silhouettes at the authored character size (16x32 world pixels). */
    private void drawCharacters() {
        int y = 22;
        for (int i = 0; i < 3; i++) {
            int x = 8 + i * 22;
            fill(x, y, CHARACTER_WIDTH, CHARACTER_HEIGHT, CHECKER_B);
            outline(x, y, CHARACTER_WIDTH, CHARACTER_HEIGHT, i == 1 ? HOT : ACCENT);
            fill(x + 4, y + CHARACTER_HEIGHT - 8, 3, 3, CHECKER_A);   // eyes: 3x3 world pixels
            fill(x + 9, y + CHARACTER_HEIGHT - 8, 3, 3, CHECKER_A);
        }
    }


    /** A ruler with 1px ticks every 4 world pixels along the bottom of the guaranteed area. */
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

    // ------------------------------------------------------------------ overlay

    private void drawOverlay(int screenWidth, int screenHeight) {
        int s = viewport.getScale();
        String[] lines = {
                "STARFALL - JALON M1 - VUE PIXEL PARFAITE",
                "FENÊTRE : " + screenWidth + " x " + screenHeight + " PX ÉCRAN",
                "ÉCHELLE ENTIÈRE : x" + s + "   (1 PX-MONDE = " + s + "x" + s + " PX ÉCRAN)",
                "MONDE VISIBLE : " + viewport.getVisibleWorldWidth() + " x "
                        + viewport.getVisibleWorldHeight() + " PX-MONDE"
                        + "   ZONE GARANTIE : " + MIN_WORLD_WIDTH + " x " + MIN_WORLD_HEIGHT + " (CADRE OR)",
                "CAMÉRA : X=" + viewport.getCameraLeft() + " Y=" + viewport.getCameraBottom()
                        + "   DAMIERS : 1 ET 2 PX-MONDE   PERSONNAGE : "
                        + CHARACTER_WIDTH + "x" + CHARACTER_HEIGHT,
                "ESPACE : DÉFILEMENT   ÉCHAP : QUITTER   F11 : PLEIN ÉCRAN",
        };

        // Integer text scale: big enough to read, small enough that the panel never reaches the
        // test patterns (which all sit below y=112 in world space) and never overflows the window.
        int scale = MathUtils.clamp(screenHeight / 300, 1, 3);
        int longest = 0;
        for (String line : lines) {
            longest = Math.max(longest, line.length());
        }
        while (scale > 1 && (longest * PixelFont.ADVANCE + 8) * scale > screenWidth) {
            scale--;
        }

        int margin = 4 * scale;
        int lineStep = PixelFont.LINE_HEIGHT * scale;
        int panelHeight = lines.length * lineStep + 2 * margin;

        int widest = 0;
        for (String line : lines) {
            widest = Math.max(widest, font.width(line, scale));
        }
        int panelWidth = Math.min(screenWidth, widest + 2 * margin);

        batch.setColor(PANEL);
        batch.draw(white, 0, screenHeight - panelHeight, panelWidth, panelHeight);
        batch.setColor(FRAME);
        batch.draw(white, 0, screenHeight - panelHeight, panelWidth, scale);

        batch.setColor(Color.WHITE);
        int y = screenHeight - margin;
        for (int i = 0; i < lines.length; i++) {
            batch.setColor(i == 0 ? FRAME : Color.WHITE);
            font.draw(batch, lines[i], margin, y, scale);
            y -= lineStep;
        }
        batch.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------ primitives

    private void fill(int x, int y, int width, int height, Color color) {
        batch.setColor(color);
        batch.draw(white, x, y, width, height);
    }

    /** A 1-world-pixel-wide rectangle outline. */
    private void outline(int x, int y, int width, int height, Color color) {
        batch.setColor(color);
        batch.draw(white, x, y, width, 1);
        batch.draw(white, x, y + height - 1, width, 1);
        batch.draw(white, x, y + 1, 1, height - 2);
        batch.draw(white, x + width - 1, y + 1, 1, height - 2);
    }

    /** Bresenham line drawn one world pixel at a time - no GL line rasterisation. */
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
