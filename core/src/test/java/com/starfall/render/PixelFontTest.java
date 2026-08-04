package com.starfall.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests de la police intégrée qui ne demandent aucun contexte GL : mesure de texte et complétude de
 * la table de glyphes. Le rendu lui-même reste jugé sur les captures.
 */
class PixelFontTest {

    @Test
    @DisplayName("Une ligne vide n'occupe aucune largeur")
    void emptyTextHasNoWidth() {
        assertEquals(0, PixelFont.widthOf("", 1));
        assertEquals(0, PixelFont.widthOf("", 3));
    }

    @Test
    @DisplayName("La largeur compte les avances moins l'espacement final")
    void widthCountsAdvancesMinusTheTrailingGap() {
        assertEquals(PixelFont.ADVANCE - 1, PixelFont.widthOf("A", 1));
        assertEquals(4 * PixelFont.ADVANCE - 1, PixelFont.widthOf("TEST", 1));
        assertEquals((4 * PixelFont.ADVANCE - 1) * 3, PixelFont.widthOf("TEST", 3));
    }

    @Test
    @DisplayName("La mesure porte sur le texte réellement dessiné, donc en majuscules")
    void widthIsMeasuredOnTheNormalisedText() {
        // draw() passe le texte en majuscules ; mesurer la casse d'origine décalerait la mise en page.
        assertEquals(PixelFont.widthOf("FENÊTRE", 2), PixelFont.widthOf("fenêtre", 2));
        assertEquals(PixelFont.widthOf("ÉCHELLE", 1), PixelFont.widthOf("échelle", 1));
    }

    @Test
    @DisplayName("La largeur croît strictement avec l'échelle")
    void widthScalesLinearly() {
        int atOne = PixelFont.widthOf("STARFALL", 1);
        assertEquals(atOne * 2, PixelFont.widthOf("STARFALL", 2));
        assertEquals(atOne * 3, PixelFont.widthOf("STARFALL", 3));
    }

    /**
     * L'interface du jeu est en français : un accent manquant dans la table se traduirait par un
     * trou silencieux dans le texte, puisque {@code draw} saute simplement les caractères inconnus.
     */
    @ParameterizedTest(name = "glyphe {0}")
    @ValueSource(chars = {'É', 'È', 'Ê', 'Ë', 'À', 'Â', 'Î', 'Ï', 'Ô', 'Ù', 'Û', 'Ü', 'Ç'})
    void frenchAccentedCapitalsAreAllPresent(char accented) {
        assertTrue(PixelFont.hasGlyph(accented), "glyphe manquant pour " + accented);
    }

    @Test
    @DisplayName("Toutes les lignes réellement affichées par le jalon M1 sont dessinables")
    void everyCharacterUsedByTheOverlayHasAGlyph() {
        String overlay = "STARFALL - JALON M1 - VUE PIXEL PARFAITE"
                + "FENÊTRE : 1280 x 720 PX ÉCRAN"
                + "ÉCHELLE ENTIÈRE : x4 (1 PX-MONDE = 4x4 PX ÉCRAN)"
                + "ZONE SÛRE : 320 x 180 PX-MONDE ZONE GARANTIE : 320 x 180 (CADRE OR)"
                + "CAMÉRA : X=0 Y=0 DAMIERS : 1 ET 2 PX-MONDE PERSONNAGE : 16x32"
                + "ESPACE : DÉFILEMENT ÉCHAP : QUITTER F11 : PLEIN ÉCRAN";

        int missing = PixelFont.firstUndrawableCharacter(overlay);
        assertEquals(-1, missing,
                () -> "l'interface utilise un caractère sans glyphe : '" + (char) missing
                        + "' (U+" + Integer.toHexString(missing).toUpperCase() + ")");
    }

    @Test
    @DisplayName("Un caractère hors table est bien détecté comme non dessinable")
    void anUnknownCharacterIsReported() {
        // Sans quoi le test précédent pourrait passer en ne vérifiant rien du tout.
        assertEquals('€', PixelFontTest.firstMissing("PRIX : 12 €"));
        assertEquals(-1, PixelFont.firstUndrawableCharacter("prix : 12 euros"));
    }

    private static int firstMissing(String text) {
        return PixelFont.firstUndrawableCharacter(text);
    }

    @Test
    @DisplayName("Les lettres, chiffres et ponctuations de base sont couverts")
    void baseAlphabetIsComplete() {
        for (char c = 'A'; c <= 'Z'; c++) {
            assertTrue(PixelFont.hasGlyph(c), "lettre manquante : " + c);
        }
        for (char c = '0'; c <= '9'; c++) {
            assertTrue(PixelFont.hasGlyph(c), "chiffre manquant : " + c);
        }
        for (char c : " :.,;-_+=/\\()[]!?'\"%*<>#".toCharArray()) {
            assertTrue(PixelFont.hasGlyph(c), "ponctuation manquante : " + c);
        }
    }
}
