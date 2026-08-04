package com.starfall.art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests de la palette maître : c'est elle qui donne leur sens aux caractères des grilles. */
class PaletteTest {

    private static Palette parse(String... lines) {
        return Palette.parse("palette-de-test", List.of(lines));
    }

    @Test
    @DisplayName("Une couleur sur 6 chiffres est opaque")
    void sixDigitColoursAreOpaque() {
        Palette palette = parse("k noir 0d0b14");

        // Sans complement d'alpha, un « 0d0b14 » donnerait un sprite parfaitement invisible.
        assertEquals(0x0d0b14FF, palette.color('k', "test", 1));
    }

    @Test
    @DisplayName("Une couleur sur 8 chiffres garde son alpha")
    void eightDigitColoursKeepTheirAlpha() {
        Palette palette = parse("h voile 0d0b1480");

        assertEquals(0x0d0b1480, palette.color('h', "test", 1));
    }

    @Test
    @DisplayName("Le point est transparent sans être déclaré")
    void theDotIsAlwaysTransparent() {
        Palette palette = parse("k noir 000000");

        assertEquals(0, palette.color(Palette.TRANSPARENT, "test", 1));
        assertTrue(palette.hasCode(Palette.TRANSPARENT));
        assertEquals("transparent", palette.name(Palette.TRANSPARENT));
    }

    @Test
    @DisplayName("Commentaires et lignes vides sont ignorés")
    void commentsAndBlankLinesAreIgnored() {
        Palette palette = parse(
                "# un commentaire",
                "",
                "   ",
                "k noir 000000   # commentaire en fin de ligne",
                "w blanc ffffff");

        assertEquals(2, palette.size());
        assertEquals(List.of('k', 'w'), palette.codes());
    }

    @Test
    @DisplayName("Un code en double est refusé")
    void duplicateCodeIsRejected() {
        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> parse("k noir 000000", "k gris 888888"));

        assertTrue(error.getMessage().contains("palette-de-test:2"), error.getMessage());
        assertTrue(error.getMessage().contains("déjà défini"), error.getMessage());
    }

    @Test
    @DisplayName("Les caractères réservés ne peuvent pas être des codes couleur")
    void reservedCharactersCannotBeColourCodes() {
        // « . » est le transparent. « @ » ouvre une directive : une ligne de pixels commencant par
        // lui serait lue comme telle. « # » ouvre un commentaire : la declaration serait avalee en
        // silence, on croirait avoir defini la couleur sans l'avoir fait.
        assertThrows(ArtFormatException.class, () -> parse(". opaque ffffff"));
        assertThrows(ArtFormatException.class, () -> parse("@ arobase ffffff"));

        // Pour « # », la ligne est un commentaire avant meme d'etre analysee : ce qui doit etre
        // verifie, c'est qu'elle ne definit rien - et donc que la palette reste vide, ce qui est
        // refuse a son tour.
        assertThrows(ArtFormatException.class, () -> parse("# diese ffffff"));
    }

    @Test
    @DisplayName("Un hexadécimal signé est refusé")
    void signedHexadecimalIsRejected() {
        // « +ff000 » et « -f0000 » font bien six caracteres : le controle de longueur les laissait
        // passer, et Long.parseLong acceptait le signe. La couleur obtenue etait silencieusement
        // fausse dans un format annonce strict.
        for (String bad : List.of("+ff000", "-f0000", "ff 000", "0x0000")) {
            assertThrows(ArtFormatException.class, () -> parse("k noir " + bad),
                    "aurait du refuser : " + bad);
        }
    }

    @Test
    @DisplayName("Un code de plusieurs caractères est refusé")
    void multiCharacterCodeIsRejected() {
        assertThrows(ArtFormatException.class, () -> parse("kk noir 000000"));
    }

    @Test
    @DisplayName("Une couleur mal formée est refusée avec sa ligne")
    void malformedColourIsRejected() {
        for (String bad : List.of("k noir zzzzzz", "k noir 00000", "k noir 0000000", "k noir")) {
            ArtFormatException error = assertThrows(ArtFormatException.class, () -> parse(bad),
                    "aurait du refuser : " + bad);
            assertTrue(error.getMessage().startsWith("palette-de-test:1:"), error.getMessage());
        }
    }

    @Test
    @DisplayName("Une palette vide est refusée")
    void emptyPaletteIsRejected() {
        assertThrows(ArtFormatException.class, () -> parse("# rien que des commentaires"));
    }

    @Test
    @DisplayName("Un code inconnu est signalé, pas remplacé par du transparent")
    void unknownCodeIsReported() {
        Palette palette = parse("k noir 000000");

        assertFalse(palette.hasCode('z'));
        ArtFormatException error = assertThrows(ArtFormatException.class,
                () -> palette.color('z', "hero.sprite", 12));
        assertTrue(error.getMessage().startsWith("hero.sprite:12:"), error.getMessage());
    }
}
