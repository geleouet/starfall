package com.starfall.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.starfall.StarfallGame;
import com.starfall.render.PixelPainter;

import java.util.List;

/**
 * Mire de contrôle des jalons M1 et M3 : rien ici ne sert au joueur, tout sert à juger d'une seule
 * capture si la grille de pixels et le pipeline d'art sont intacts.
 *
 * <p>Deux familles de motifs y cohabitent :
 * <ul>
 *   <li>les damiers d'1 et 2 pixels, les rayures, les diagonales et la règle graduée : si un
 *       pixel-monde était mis à l'échelle de travers, ils feraient immédiatement apparaître des
 *       lignes d'une autre largeur ;</li>
 *   <li>les sprites issus de l'atlas, adossés à ces motifs : un sprite flou ou décalé d'un pixel se
 *       voit tout de suite à côté d'un damier d'un pixel.</li>
 * </ul>
 */
public final class CalibrationScene implements Scene {

    private static final Color INSIDE = new Color(0x1b2743ff);
    private static final Color FRAME = new Color(0xffcc33ff);
    private static final Color CHECKER_A = new Color(0xf4f7ffff);
    private static final Color CHECKER_B = new Color(0x27324eff);
    private static final Color ACCENT = new Color(0x54d6ffff);
    private static final Color HOT = new Color(0xff5c6aff);
    private static final Color GREEN = new Color(0x7be08aff);
    private static final Color DOTS = new Color(0x3d5288ff);

    private static final int CHECKER_X = 8;
    private static final int CHECKER_Y = 62;
    private static final int CHECKER_W = 96;
    private static final int CHECKER_H = 48;
    private static final int CHECKER2_X = 232;
    private static final int CHECKER2_Y = 20;
    private static final int CHECKER2_W = 80;
    private static final int CHECKER2_H = 40;

    // Vitrine des sprites : la bande libre entre la règle graduée (jusqu'à y=17) et le damier
    // d'1 px (à partir de y=62), à gauche des diagonales (x >= 76).
    private static final int SHOWCASE_X = 8;
    private static final int SHOWCASE_GROUND_Y = 18;
    private static final int SHOWCASE_FIGURE_Y = 26;
    private static final int SHOWCASE_COLUMN = 22;

    private SceneContext context;
    private PixelPainter painter;
    private Texture checker;
    private Texture checker2;

    private boolean scrolling;
    private float cameraX = StarfallGame.MIN_WORLD_WIDTH / 2f;
    private float cameraY = StarfallGame.MIN_WORLD_HEIGHT / 2f;

        /**
     * Nom de scène qui la sélectionne, côté ligne de commande.
     *
     * <p>Il était écrit en toutes lettres à quatre endroits — ici, dans la table des scènes, dans
     * l'aiguillage du jeu, et dans l'exemption de la borne de {@code --from}. Les deux autres scènes
     * portaient déjà leur nom ; celle-ci ne l'a pas fait parce qu'elle n'a pas de scénario de
     * capture, c'est-à-dire pour une raison qui n'a rien à voir.
     */
    public static final String SCENE_NAME = "calibration";

    @Override
    public String name() {
        return SCENE_NAME;
    }

    @Override
    public void create(SceneContext context) {
        this.context = context;
        this.painter = context.painter();
        this.checker = checkerTexture(CHECKER_W, CHECKER_H, 1);
        this.checker2 = checkerTexture(CHECKER2_W, CHECKER2_H, 2);
    }

    /**
     * La mire est statique ; c'est la caméra qui bouge.
     *
     * <p>Ce défilement est le cœur de la mire : il promène la caméra sur des cibles fractionnaires,
     * et l'image doit rester nette. Il vit ici, et nulle part ailleurs — le laisser piloter la
     * caméra de l'arène y faisait sortir des cases de l'écran.
     */
    @Override
    public void act(float time, int frameIndex, boolean interactive) {
        if (interactive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                scrolling = !scrolling;
            }
        } else {
            // En capture : la première image au repos, les suivantes caméra en mouvement.
            scrolling = frameIndex > 0;
        }

        if (scrolling) {
            cameraX = StarfallGame.MIN_WORLD_WIDTH / 2f + MathUtils.sin(time * 0.6f) * 24f;
            cameraY = StarfallGame.MIN_WORLD_HEIGHT / 2f + MathUtils.cos(time * 0.4f) * 10f;
        } else {
            cameraX = StarfallGame.MIN_WORLD_WIDTH / 2f;
            cameraY = StarfallGame.MIN_WORLD_HEIGHT / 2f;
        }
    }

    @Override
    public float cameraTargetX() {
        return cameraX;
    }

    @Override
    public float cameraTargetY() {
        return cameraY;
    }

    @Override
    public void drawWorld() {
        var viewport = context.viewport();
        int left = viewport.getDrawnLeft();
        int bottom = viewport.getDrawnBottom();
        int right = left + viewport.getDrawnWorldWidth();
        int top = bottom + viewport.getDrawnWorldHeight();

        painter.fill(0, 0, StarfallGame.MIN_WORLD_WIDTH, StarfallGame.MIN_WORLD_HEIGHT, INSIDE);

        // Trame de points d'1 px sur tout le monde visible, dedans comme dehors : chaque point doit
        // être un carré de taille identique, où qu'il tombe.
        painter.color(DOTS);
        for (int x = MathUtils.floor(left / 8f) * 8; x <= right; x += 8) {
            for (int y = MathUtils.floor(bottom / 8f) * 8; y <= top; y += 8) {
                painter.fill(x, y, 1, 1, DOTS);
            }
        }

        drawCheckerboards();
        drawStripes();
        drawNestedRectangles();
        drawDiagonals();
        drawSpriteShowcase();
        drawRuler();
        drawCrosshair();

        painter.outline(0, 0, StarfallGame.MIN_WORLD_WIDTH, StarfallGame.MIN_WORLD_HEIGHT, FRAME);
        painter.color(Color.WHITE);
    }

    /**
     * Le test de référence : un damier dont les cases font exactement un pixel-monde, à côté d'un
     * damier à deux.
     */
    private void drawCheckerboards() {
        context.batch().setColor(Color.WHITE);
        context.batch().draw(checker, CHECKER_X, CHECKER_Y, CHECKER_W, CHECKER_H);
        painter.outline(CHECKER_X - 2, CHECKER_Y - 2, CHECKER_W + 4, CHECKER_H + 4, ACCENT);

        context.batch().setColor(Color.WHITE);
        context.batch().draw(checker2, CHECKER2_X, CHECKER2_Y, CHECKER2_W, CHECKER2_H);
        painter.outline(CHECKER2_X - 2, CHECKER2_Y - 2, CHECKER2_W + 4, CHECKER2_H + 4, GREEN);
    }

    /** Rayures d'1 px, verticales puis horizontales : le détecteur classique d'échelle inégale. */
    private void drawStripes() {
        int y0 = 66;
        int x0 = 108;
        for (int i = 0; i < 30; i += 2) {
            painter.fill(x0 + i, y0, 1, 40, ACCENT);
        }
        int x1 = 182;
        for (int i = 0; i < 40; i += 2) {
            painter.fill(x1, y0 + i, 30, 1, GREEN);
        }
        painter.outline(x0 - 2, y0 - 2, 34, 44, FRAME);
        painter.outline(x1 - 2, y0 - 2, 34, 44, FRAME);
    }

    private void drawNestedRectangles() {
        int cx = 270;
        int cy = 86;
        int[] sizes = {48, 38, 28, 18, 8};
        Color[] colors = {FRAME, ACCENT, HOT, GREEN, CHECKER_A};
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            painter.outline(cx - size / 2, cy - size / 2, size, size, colors[i]);
        }
    }

    private void drawDiagonals() {
        painter.line(76, 20, 112, 56, HOT);         // 45 degrés : un pixel par pas
        painter.line(118, 20, 198, 40, GREEN);      // 4:1 - longues séries régulières
        painter.line(204, 20, 220, 54, ACCENT);     // pente raide
    }

    /** Les sprites de l'atlas, adossés aux motifs de contrôle. */
    private void drawSpriteShowcase() {
        var atlas = context.atlas();
        painter.sprite(atlas.region("ground/plain"), SHOWCASE_X, SHOWCASE_GROUND_Y);
        painter.sprite(atlas.region("ground/plain"), SHOWCASE_X + SHOWCASE_COLUMN, SHOWCASE_GROUND_Y);
        painter.sprite(atlas.region("hero/idle"), SHOWCASE_X + 2, SHOWCASE_FIGURE_Y);
        painter.sprite(atlas.region("enemy/sabreur"), SHOWCASE_X + SHOWCASE_COLUMN + 2, SHOWCASE_FIGURE_Y);

        int tilesX = SHOWCASE_X + 2 * SHOWCASE_COLUMN;
        painter.sprite(atlas.region("tile/empty"), tilesX, SHOWCASE_FIGURE_Y + 16);
        painter.sprite(atlas.region("tile/slash"), tilesX, SHOWCASE_FIGURE_Y);
    }

    /** Une règle graduée d'1 px tous les 4 pixels-monde. */
    private void drawRuler() {
        int baseY = 8;
        painter.fill(8, baseY, StarfallGame.MIN_WORLD_WIDTH - 16, 1, FRAME);
        for (int x = 8; x < StarfallGame.MIN_WORLD_WIDTH - 8; x += 4) {
            int height = (x % 32 == 8) ? 9 : (x % 16 == 8 ? 6 : 3);
            painter.fill(x, baseY + 1, 1, height, x % 32 == 8 ? HOT : ACCENT);
        }
    }

    private void drawCrosshair() {
        int cx = StarfallGame.MIN_WORLD_WIDTH / 2;
        int cy = StarfallGame.MIN_WORLD_HEIGHT / 2;
        painter.fill(cx - 6, cy, 13, 1, CHECKER_A);
        painter.fill(cx, cy - 6, 1, 13, CHECKER_A);
    }

    @Override
    public int contentTopWorldY() {
        return StarfallGame.PATTERN_TOP;
    }

    @Override
    public List<String> overlayLines(int screenWidth, int screenHeight) {
        var viewport = context.viewport();
        int scale = viewport.getScale();
        return List.of(
                "STARFALL - MIRE DE CALIBRATION",
                "FENÊTRE : " + screenWidth + " x " + screenHeight + " PX ÉCRAN",
                "ÉCHELLE ENTIÈRE : x" + scale + "   (1 PX-MONDE = " + scale + "x" + scale + " PX ÉCRAN)",
                "ZONE SÛRE : " + viewport.getSafeWorldWidth() + " x " + viewport.getSafeWorldHeight()
                        + " PX-MONDE   ZONE GARANTIE : " + StarfallGame.MIN_WORLD_WIDTH + " x "
                        + StarfallGame.MIN_WORLD_HEIGHT + " (CADRE OR)",
                "ATLAS : " + context.atlas().names().size() + " SPRITES EN "
                        + context.atlas().atlasWidth() + "x" + context.atlas().atlasHeight()
                        + "   SOURCES TEXTE : ART/",
                "CAMÉRA : X=" + viewport.getSafeLeft() + " Y=" + viewport.getSafeBottom()
                        + "   DAMIERS : 1 ET 2 PX-MONDE",
                "ESPACE : DÉFILEMENT   ÉCHAP : QUITTER   F11 : PLEIN ÉCRAN");
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
        if (checker != null) {
            checker.dispose();
        }
        if (checker2 != null) {
            checker2.dispose();
        }
    }
}
