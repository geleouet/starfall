package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La couleur d'une plaque d'intention, que nulle planche ne peut départager.
 *
 * <h2>Pourquoi un test et pas une image</h2>
 *
 * <p>Ce projet garde d'ordinaire l'apparence par l'image : cent quatre planches comparées octet à
 * octet. Cette règle-ci leur échappe, et pas par oubli. Le cadre est <b>doré</b> quand l'ennemi
 * joue à la phase qui vient, <b>rouge</b> quand il annonce sans être imminent — or dans tous les
 * scénarios capturés, les ennemis agissent. <b>Aucune planche ne montre l'état rouge.</b>
 *
 * <p>Une version qui rendrait l'or en toute circonstance passerait donc le garde-fou d'images sans
 * qu'une seule ligne rougisse, et la distinction la plus utile de l'écran — « j'ai un tour de
 * répit » contre « ça tombe maintenant » — disparaîtrait en silence. Un choix de couleur
 * qu'aucune image ne peut départager doit être départagé ici.
 *
 * <p>C'est la même raison qui avait fait descendre le pas de temps dans le déroulé : une règle
 * vit là où elle s'éprouve, pas là où elle s'utilise.
 */
class PlaqueColorTest {

    @Test
    @DisplayName("Annoncé et imminent ne portent jamais la même couleur")
    void announcedAndImminentNeverShareAColour() {
        Color announced = HudColors.plaque(false, 0f);
        Color lit = HudColors.plaque(true, 0f);
        Color dim = HudColors.plaque(true, HudColors.BLINK_SECONDS);

        assertEquals(HudColors.THREAT, announced,
                "une menace qui ne tombe pas ce tour-ci reste rouge : c'est la couleur de"
                        + " l'annonce, et le joueur la lit comme « j'ai un tour »");
        assertNotEquals(announced, lit,
                "l'imminent porte la couleur de l'annonce : la distinction la plus utile de l'ecran"
                        + " a disparu, et aucune planche ne peut le dire puisque toutes montrent"
                        + " des ennemis qui agissent");
        assertNotEquals(announced, dim,
                "le temps creux du battement retombe sur le rouge : un joueur qui regarde au"
                        + " mauvais moment lirait « j'ai un tour » alors qu'il n'en a pas");
        assertNotEquals(lit, dim,
                "les deux temps du battement sont identiques : le cadre ne bat plus");
    }

    /**
     * Le battement reste dans l'or, des deux côtés.
     *
     * <p>Il va d'un or vif à un or sourd, jamais de l'or au rouge. Un battement qui repasserait par
     * la couleur de l'annonce rendrait la <em>catégorie</em> illisible un temps sur deux, et
     * l'urgence remplacerait l'information au lieu de s'y ajouter.
     */
    @Test
    @DisplayName("Le battement ne quitte jamais l'or, à aucun instant")
    void theBlinkNeverLeavesGold() {
        // Trois cycles complets, echantillonnes fin : une parite mal ecrite se trahit sur un bord.
        for (int step = 0; step <= 300; step++) {
            float seconds = step * HudColors.BLINK_SECONDS * 6f / 300f;
            Color colour = HudColors.plaque(true, seconds);

            assertTrue(colour == HudColors.IMMINENT || colour == HudColors.IMMINENT_DIM,
                    "a " + seconds + " s le cadre d'un ennemi imminent vaut " + colour
                            + " : le battement est sorti de l'or");
        }
    }

    @Test
    @DisplayName("Le battement bat vraiment : les deux temps paraissent tous les deux")
    void theBlinkActuallyAlternates() {
        boolean seenLit = false;
        boolean seenDim = false;
        for (int step = 0; step <= 300; step++) {
            Color colour = HudColors.plaque(true, step * HudColors.BLINK_SECONDS * 6f / 300f);
            seenLit |= colour == HudColors.IMMINENT;
            seenDim |= colour == HudColors.IMMINENT_DIM;
        }

        // La premisse du test precedent : sans elle, une couleur constante satisferait « ne quitte
        // jamais l'or » sans battre du tout, et la garde ci-dessus serait vraie pour rien.
        assertTrue(seenLit && seenDim,
                "un seul des deux temps de l'or parait sur trois cycles : le cadre ne bat pas,"
                        + " et la garde qui exige de rester dans l'or serait vraie pour rien");
    }
}
