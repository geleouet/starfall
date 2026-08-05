package com.starfall.render;

/**
 * La taille à retrouver en sortant du plein écran.
 *
 * <h2>Pourquoi une classe pour trois entiers</h2>
 *
 * <p>Le retour du plein écran repassait par la taille de <em>lancement</em>, ce qui jetait
 * silencieusement le redimensionnement fait avant d'appuyer sur la touche : on agrandit sa fenêtre,
 * on bascule, on revient — et on se retrouve à la taille du double-clic. Le correctif tenait en deux
 * champs posés dans la classe du jeu, donc derrière un contexte graphique, donc invérifiable.
 *
 * <p>La review l'a relevé sans détour : le commit s'intitulait « couvert par ce qui peut l'être »,
 * et ce qui pouvait l'être ne l'avait pas été — pendant que le bloc de tests ajouté à côté ne
 * contenait que des identités arithmétiques incapables d'échouer. C'est le contraire qu'il fallait
 * faire : sortir la décision de la couche fenêtre, et la tester.
 *
 * <p>Il n'y a rien de graphique dans « quelle taille rendre » : trois entiers et une règle.
 */
public final class WindowedSize {

    private final int fallbackWidth;
    private final int fallbackHeight;

    private int width;
    private int height;

    /**
     * @param fallbackWidth  taille de lancement, servie tant qu'aucune taille en fenêtre n'a été vue
     * @param fallbackHeight idem
     */
    public WindowedSize(int fallbackWidth, int fallbackHeight) {
        this.fallbackWidth = fallbackWidth;
        this.fallbackHeight = fallbackHeight;
    }

    /**
     * Enregistre une taille observée.
     *
     * <p>Les tailles vues <b>en plein écran</b> sont ignorées : ce sont celles du moniteur, et les
     * retenir reviendrait à ne jamais pouvoir revenir. C'est tout le piège de ce correctif — le
     * rappel de redimensionnement se déclenche aussi quand on <em>entre</em> en plein écran.
     */
    public void remember(int observedWidth, int observedHeight, boolean fullscreen) {
        if (fullscreen || observedWidth <= 0 || observedHeight <= 0) {
            return;
        }
        this.width = observedWidth;
        this.height = observedHeight;
    }

    /** Largeur à restaurer : la dernière vue en fenêtre, ou celle du lancement à défaut. */
    public int width() {
        return width > 0 ? width : fallbackWidth;
    }

    /** Hauteur à restaurer. */
    public int height() {
        return height > 0 ? height : fallbackHeight;
    }
}
