package com.starfall.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le retour du plein écran, enfin testé.
 *
 * <p>Le correctif existait depuis un cycle, et le commit qui le livrait s'intitulait « couvert par
 * ce qui peut l'être » — alors que le seul bloc de tests ajouté à côté ne contenait que des
 * identités arithmétiques incapables d'échouer, et que le correctif lui-même n'était couvert par
 * rien. La review l'a relevé sans détour. Il n'y avait pourtant rien de graphique dans « quelle
 * taille rendre » : trois entiers et une règle.
 */
class WindowedSizeTest {

    @Test
    @DisplayName("Sans redimensionnement, on revient à la taille de lancement")
    void withoutAnyResizeWeReturnToTheLaunchSize() {
        WindowedSize size = new WindowedSize(1280, 720);

        assertEquals(1280, size.width());
        assertEquals(720, size.height());
    }

    /** Le défaut d'origine : le redimensionnement du joueur était jeté au retour. */
    @Test
    @DisplayName("On revient à la dernière taille choisie par le joueur, pas à celle du lancement")
    void wereturnToThePlayersOwnSize() {
        WindowedSize size = new WindowedSize(1280, 720);

        size.remember(1600, 900, false);

        assertEquals(1600, size.width());
        assertEquals(900, size.height());
    }

    /**
     * Le piège du correctif, et la raison d'être de cette classe : le rappel de redimensionnement
     * se déclenche <b>aussi</b> quand on entre en plein écran. Retenir cette taille-là reviendrait à
     * ne jamais pouvoir revenir — on « restaurerait » la taille du moniteur.
     */
    @Test
    @DisplayName("Une taille vue en plein écran n'est jamais retenue")
    void afullscreenSizeIsNeverRemembered() {
        WindowedSize size = new WindowedSize(1280, 720);
        size.remember(1600, 900, false);

        size.remember(3840, 2160, true);

        assertEquals(1600, size.width(), "la taille du moniteur a ete prise pour celle de la fenetre");
        assertEquals(900, size.height());
    }

    @Test
    @DisplayName("Un aller-retour complet rend la fenêtre telle qu'on l'avait laissée")
    void afullRoundTripGivesTheWindowBackUnchanged() {
        WindowedSize size = new WindowedSize(1280, 720);

        size.remember(1024, 640, false);   // le joueur redimensionne
        size.remember(2560, 1440, true);   // il passe en plein ecran
        size.remember(2560, 1440, true);   // et le gestionnaire de fenetres insiste

        assertEquals(1024, size.width());
        assertEquals(640, size.height());
    }

    /**
     * Une taille nulle ou négative n'est pas une taille. Certains gestionnaires de fenêtres en
     * envoient une quand la fenêtre est réduite dans la barre des tâches — la retenir ferait
     * rouvrir le jeu dans une fenêtre de zéro pixel.
     */
    @Test
    @DisplayName("Une taille dégénérée est ignorée")
    void adegenerateSizeIsIgnored() {
        WindowedSize size = new WindowedSize(1280, 720);
        size.remember(1600, 900, false);

        size.remember(0, 0, false);
        size.remember(-1, 400, false);

        assertEquals(1600, size.width());
        assertEquals(900, size.height());
    }
}
