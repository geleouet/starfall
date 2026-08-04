package com.starfall.scene;

import java.util.List;

/**
 * Un contenu de jeu affichable, indépendant de la plomberie qui l'entoure.
 *
 * <p>Le jeu porte deux scènes, et les deux ont leur raison d'être :
 * <ul>
 *   <li>{@link ArenaScene} — le jeu lui-même ;</li>
 *   <li>{@link CalibrationScene} — la mire de contrôle des jalons M1 et M3. Elle ne sert à rien au
 *       joueur, mais c'est la preuve visuelle que la grille de pixels et le pipeline d'art sont
 *       intacts. La garder atteignable en une option, plutôt que la remplacer par le jeu, évite de
 *       perdre une preuve de non-régression à chaque jalon.</li>
 * </ul>
 *
 * <p>La fenêtre, le viewport, le bandeau d'interface et la capture d'écran restent l'affaire de
 * {@code StarfallGame} : une scène ne s'occupe que de son propre contenu.
 */
public interface Scene {

    /** Nom de la scène tel qu'il s'écrit sur la ligne de commande. */
    String name();

    /** Appelé une fois, quand le contexte graphique est prêt. */
    void create(SceneContext context);

    /**
     * Fait avancer la scène.
     *
     * @param time        temps écoulé, en secondes ; déterministe en mode capture
     * @param frameIndex  numéro de l'image capturée, ou 0 hors mode capture ; permet à une scène de
     *                    faire varier ce qu'elle montre sans cesser d'être reproductible
     * @param interactive faux en mode capture : la scène ne doit alors lire aucune entrée, sinon
     *                    les images cesseraient d'être reproductibles
     */
    void act(float time, int frameIndex, boolean interactive);

    /**
     * Point que la caméra doit viser, en pixels-monde.
     *
     * <p>C'est la scène qui décide, et non le jeu : le défilement de démonstration hérité de M1 est
     * une mire de non-régression, et le laisser piloter la caméra de l'arène y faisait sortir des
     * cases de l'écran — une grille de 15 n'a que 10 px-monde de marge de chaque côté, contre une
     * amplitude de défilement de 24.
     */
    float cameraTargetX();

    float cameraTargetY();

    /** Dessine le contenu, en coordonnées monde, entre un {@code begin()} et un {@code end()}. */
    void drawWorld();

    /**
     * Bord supérieur du contenu, en pixels-monde. Le bandeau d'interface est borné pour ne jamais
     * descendre en dessous.
     */
    int contentTopWorldY();

    /** Lignes du bandeau d'interface, de la plus importante à la moins importante. */
    List<String> overlayLines(int screenWidth, int screenHeight);

    default void resize(int width, int height) {
    }

    default void dispose() {
    }
}
