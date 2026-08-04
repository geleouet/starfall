package com.starfall.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Viewport adaptatif verrouillé sur les entiers, pour le pixel art.
 *
 * <p>Se comporte comme l'{@code ExtendViewport} de libGDX — une zone de monde minimale est toujours
 * visible et l'espace restant révèle davantage de monde plutôt que des bandes noires — à ceci près
 * que le zoom est restreint aux nombres entiers. Un pixel-monde couvre donc toujours exactement
 * {@link #getScale()} pixels écran : le pixel art n'affiche jamais deux pixels voisins de tailles
 * différentes et ne scintille pas au défilement.
 *
 * <p>Deux rectangles cohabitent, et la distinction est ce qui rend la garantie vraie :
 * <ul>
 *   <li>la <b>zone sûre</b> ({@link #getSafeWorldWidth()} et compagnie), entièrement visible à
 *       l'écran, toujours au moins aussi grande que le minimum demandé. C'est celle sur laquelle le
 *       jeu raisonne ;</li>
 *   <li>la <b>zone dessinée</b> ({@link #getDrawnWorldWidth()} et compagnie), qui déborde d'un
 *       pixel-monde de chaque côté quand la fenêtre ne tombe pas juste. Elle ne sert qu'à ce
 *       qu'aucune gouttière ne reste noire ; ce qui s'y trouve peut être coupé.</li>
 * </ul>
 *
 * <p>L'unité de monde est le pixel-monde : les sprites sont dessinés à cette résolution (un
 * personnage fait 32 pixels-monde de haut), donc toutes les coordonnées de jeu sont simplement des
 * coordonnées en pixels.
 *
 * <p>La caméra reste alignée sur des pixels-monde entiers : {@link #setCameraTarget} accepte une
 * position fractionnaire (physique, suivi fluide) mais les bords du frustum qu'elle produit sont
 * toujours entiers — c'est ce qui garantit réellement une image sans gigue.
 *
 * <p>Toute l'arithmétique vit dans {@link PixelScale} pour être testable sans contexte GL.
 */
public class PixelViewport extends Viewport {

    private final int minWorldWidth;
    private final int minWorldHeight;

    /** Centre de caméra souhaité, en pixels-monde ; peut être fractionnaire, il est aligné à l'usage. */
    private final Vector2 cameraTarget = new Vector2();

    private int scale = 1;
    private int safeWorldWidth;
    private int safeWorldHeight;
    private int bleedX;
    private int bleedY;

    /** Bord gauche / bas de la zone sûre, en pixels-monde entiers. */
    private int safeLeft;
    private int safeBottom;

    public PixelViewport(int minWorldWidth, int minWorldHeight) {
        this(minWorldWidth, minWorldHeight, new OrthographicCamera());
    }

    public PixelViewport(int minWorldWidth, int minWorldHeight, Camera camera) {
        if (minWorldWidth < 1 || minWorldHeight < 1) {
            throw new IllegalArgumentException("La taille de monde minimale doit valoir au moins 1x1 pixel-monde");
        }
        this.minWorldWidth = minWorldWidth;
        this.minWorldHeight = minWorldHeight;
        this.safeWorldWidth = minWorldWidth;
        this.safeWorldHeight = minWorldHeight;
        setCamera(camera);
        setWorldSize(minWorldWidth, minWorldHeight);
        cameraTarget.set(minWorldWidth / 2f, minWorldHeight / 2f);
        // Un viewport neuf doit déjà porter une matrice utilisable : sans cela, tout appelant qui
        // dessinerait avant le premier update() obtiendrait une caméra de taille nulle, en silence.
        snapCamera();
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        PixelScale layout = PixelScale.compute(screenWidth, screenHeight, minWorldWidth, minWorldHeight);
        this.scale = layout.scale;
        this.safeWorldWidth = layout.safeWorldWidth;
        this.safeWorldHeight = layout.safeWorldHeight;
        this.bleedX = layout.bleedX;
        this.bleedY = layout.bleedY;

        setWorldSize(layout.worldWidth, layout.worldHeight);
        setScreenBounds(layout.screenX, layout.screenY, layout.screenWidth, layout.screenHeight);

        if (centerCamera) {
            cameraTarget.set(minWorldWidth / 2f, minWorldHeight / 2f);
        }
        snapCamera();
        apply(false);
    }

    /**
     * Déplace la caméra. La position peut être fractionnaire : elle est alignée sur la grille des
     * pixels-monde pour que l'image ne tombe jamais entre deux pixels écran.
     */
    public void setCameraTarget(float worldX, float worldY) {
        cameraTarget.set(worldX, worldY);
        snapCamera();
    }

    /** Aligne les bords du frustum sur la grille des pixels-monde et pousse le résultat à la caméra. */
    private void snapCamera() {
        // La zone sûre est centrée sur la cible ; la zone dessinée l'entoure symétriquement, donc
        // les deux partagent le même centre et un seul arrondi suffit.
        safeLeft = Math.round(cameraTarget.x - safeWorldWidth / 2f);
        safeBottom = Math.round(cameraTarget.y - safeWorldHeight / 2f);

        int drawnWidth = getDrawnWorldWidth();
        int drawnHeight = getDrawnWorldHeight();

        Camera camera = getCamera();
        camera.viewportWidth = drawnWidth;
        camera.viewportHeight = drawnHeight;
        // Les bords du frustum tombent pile sur des entiers, y compris quand la taille est impaire.
        camera.position.set(
                getDrawnLeft() + drawnWidth / 2f,
                getDrawnBottom() + drawnHeight / 2f,
                0f);
        camera.update();
    }

    /** Pixels écran par pixel-monde. Toujours entier, jamais sous 1. */
    public int getScale() {
        return scale;
    }

    public int getMinWorldWidth() {
        return minWorldWidth;
    }

    public int getMinWorldHeight() {
        return minWorldHeight;
    }

    // ---------------------------------------------------------------- zone sûre

    /** Largeur entièrement visible, en pixels-monde. Toujours >= la largeur minimale demandée. */
    public int getSafeWorldWidth() {
        return safeWorldWidth;
    }

    /** Hauteur entièrement visible, en pixels-monde. Toujours >= la hauteur minimale demandée. */
    public int getSafeWorldHeight() {
        return safeWorldHeight;
    }

    /** Bord gauche de la zone sûre, en pixels-monde entiers. */
    public int getSafeLeft() {
        return safeLeft;
    }

    /** Bord bas de la zone sûre, en pixels-monde entiers. */
    public int getSafeBottom() {
        return safeBottom;
    }

    // ---------------------------------------------------------------- zone dessinée

    /** Largeur à couvrir par le rendu, débordement compris. */
    public int getDrawnWorldWidth() {
        return safeWorldWidth + 2 * bleedX;
    }

    /** Hauteur à couvrir par le rendu, débordement compris. */
    public int getDrawnWorldHeight() {
        return safeWorldHeight + 2 * bleedY;
    }

    /** Bord gauche de la zone dessinée, en pixels-monde entiers. Peut être partiellement coupé. */
    public int getDrawnLeft() {
        return safeLeft - bleedX;
    }

    /** Bord bas de la zone dessinée, en pixels-monde entiers. Peut être partiellement coupé. */
    public int getDrawnBottom() {
        return safeBottom - bleedY;
    }
}
