package com.starfall.art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests du format source des sprites.
 *
 * <p>Ces fichiers sont écrits et relus à la main entre deux passes de review : l'essentiel de ce
 * qui est vérifié ici, c'est qu'une faute de frappe donne un message qui pointe la bonne ligne
 * plutôt qu'un sprite silencieusement de travers.
 */
class SpriteParserTest {

    private static final Palette PALETTE = Palette.parse("palette",
            List.of("k noir 0d0b14", "w blanc f2ece0", "r rouge d4453f"));

    private static List<SpriteSource> parse(String... lines) {
        return SpriteParser.parse("test.sprite", List.of(lines), PALETTE);
    }

    @Test
    @DisplayName("Un sprite simple est lu dans le bon sens")
    void readsASpriteTopDown() {
        List<SpriteSource> sprites = parse(
                "@sprite hero/idle",
                "kw.",
                "..r");

        assertEquals(1, sprites.size());
        SpriteSource sprite = sprites.get(0);
        assertEquals("hero/idle", sprite.name());
        assertEquals(3, sprite.width());
        assertEquals(2, sprite.height());

        // y=0 est la ligne du HAUT, comme on la lit dans le fichier.
        assertEquals(0x0d0b14FF, sprite.pixel(0, 0));
        assertEquals(0xf2ece0FF, sprite.pixel(1, 0));
        assertEquals(0, sprite.pixel(2, 0), "le point doit rester transparent");
        assertEquals(0xd4453fFF, sprite.pixel(2, 1));
    }

    @Test
    @DisplayName("Plusieurs sprites peuvent cohabiter dans un fichier")
    void readsSeveralSpritesFromOneFile() {
        List<SpriteSource> sprites = parse(
                "@sprite a/one",
                "kk",
                "",
                "# une remarque",
                "@sprite a/two",
                "ww",
                "ww");

        assertEquals(List.of("a/one", "a/two"), sprites.stream().map(SpriteSource::name).toList());
        assertEquals(1, sprites.get(0).height());
        assertEquals(2, sprites.get(1).height());
    }

    @Test
    @DisplayName("Une ligne de longueur différente est refusée, avec sa vraie ligne")
    void raggedRowsAreRejectedAtTheRightLine() {
        ArtFormatException error = assertThrows(ArtFormatException.class, () -> parse(
                "@sprite a/one",
                "kkk",
                "kkk",
                "kk"));

        assertTrue(error.getMessage().startsWith("test.sprite:4:"), error.getMessage());
    }

    @Test
    @DisplayName("Un commentaire au milieu d'une grille ne décale pas le numéro de ligne")
    void interleavedCommentsDoNotShiftTheReportedLine() {
        // Le numero est porte par la ligne elle-meme, et non recalcule depuis la directive :
        // c'est precisement ce cas qui donnait un message pointant la mauvaise ligne.
        ArtFormatException error = assertThrows(ArtFormatException.class, () -> parse(
                "@sprite a/one",
                "kkk",
                "# une remarque au milieu du dessin",
                "",
                "kkk",
                "kk"));

        assertTrue(error.getMessage().startsWith("test.sprite:6:"), error.getMessage());
    }

    @Test
    @DisplayName("Un code couleur inconnu est refusé avec sa ligne et sa colonne")
    void unknownColourCodeIsRejected() {
        ArtFormatException error = assertThrows(ArtFormatException.class, () -> parse(
                "@sprite a/one",
                "kkk",
                "kzk"));

        assertTrue(error.getMessage().startsWith("test.sprite:3:"), error.getMessage());
        assertTrue(error.getMessage().contains("colonne 2"), error.getMessage());
    }

    @Test
    @DisplayName("Une grille sans directive est refusée")
    void pixelsBeforeAnyDirectiveAreRejected() {
        assertThrows(ArtFormatException.class, () -> parse("kkk"));
    }

    @Test
    @DisplayName("Un sprite déclaré mais vide est refusé")
    void anEmptySpriteIsRejected() {
        assertThrows(ArtFormatException.class, () -> parse("@sprite a/one", "@sprite a/two", "kk"));
    }

    @Test
    @DisplayName("Un fichier sans aucun sprite est refusé")
    void aFileWithoutSpritesIsRejected() {
        assertThrows(ArtFormatException.class, () -> parse("# rien", ""));
    }

    @Test
    @DisplayName("Une directive inconnue est refusée plutôt qu'ignorée")
    void unknownDirectiveIsRejected() {
        ArtFormatException error = assertThrows(ArtFormatException.class, () -> parse(
                "@sprite a/one",
                "@pivot 8 0",
                "kk"));

        assertTrue(error.getMessage().contains("@pivot"), error.getMessage());
    }

    @Test
    @DisplayName("Un nom de sprite invalide est refusé")
    void invalidNamesAreRejected() {
        for (String name : List.of("Hero", "hero idle", "hero//idle", "/hero", "hero-idle", "héros")) {
            assertThrows(ArtFormatException.class,
                    () -> parse("@sprite " + name, "kk"),
                    "aurait du refuser le nom : " + name);
        }
    }

    @Test
    @DisplayName("Les espaces de fin de ligne sont tolérés, ceux du début font partie du dessin")
    void trailingSpacesAreTrimmedButLeadingOnesAreNot() {
        // Un editeur qui coupe les espaces de fin ne doit pas casser le fichier ; en revanche un
        // decalage a gauche est un choix de dessin, et le supprimer deplacerait le sprite.
        List<SpriteSource> sprites = parse("@sprite a/one", "kk   ", "kk");
        assertEquals(2, sprites.get(0).width());

        assertThrows(ArtFormatException.class, () -> parse("@sprite a/one", "  kk", "kk"));
    }
}
