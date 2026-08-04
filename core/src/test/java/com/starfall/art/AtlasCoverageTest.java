package com.starfall.art;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starfall.game.EnemyKind;
import com.starfall.game.Tile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le pont entre ce que le jeu <b>demande</b> à l'atlas et ce que l'atlas <b>contient</b>.
 *
 * <h2>Pourquoi ce test existe</h2>
 *
 * <p>C'est le seul endroit du projet où deux mondes se rencontrent sans se parler : les noms de
 * sprites vivent en dur dans des chaînes de caractères — {@code "enemy/souverain"}, {@code
 * "tile/slash"} — et l'atlas est produit à part, depuis des fichiers texte, par une étape de build.
 * Rien ne garantissait que les deux listes coïncident.
 *
 * <p>Ça s'est déjà produit. Au jalon du pipeline d'art, la mire de calibration référençait encore
 * {@code enemy/melee} après un renommage : la figure disparaissait purement et simplement. Le défaut
 * n'a été attrapé que parce que la recherche d'une région manquante avait été durcie pour lever une
 * erreur au lieu de rendre un rectangle vide — c'est-à-dire par chance, et à l'exécution.
 *
 * <p>Ce test le dit au build, sans contexte graphique : il lit l'index généré, demande à chaque
 * source de noms du jeu ce qu'elle réclame, et compare. C'est le « test doré » qui manquait entre
 * {@code art/} et le jeu.
 */
class AtlasCoverageTest {

    private static Path locateGeneratedIndex() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            Path index = candidate.resolve("assets/generated/starfall-atlas.txt");
            if (Files.isRegularFile(index)) {
                return index;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("index d'atlas introuvable depuis " + Path.of("").toAbsolutePath());
    }

    private static AtlasIndex index() throws IOException {
        Path path = locateGeneratedIndex();
        return AtlasIndex.parse(path.toString(),
                Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    /**
     * Tout ce que le jeu sait nommer.
     *
     * <p>La liste est <b>dérivée</b> des énumérations plutôt que recopiée : une sixième tuile ou un
     * sixième archétype ajouté sans son sprite fait échouer ce test tout seul, ce qui est
     * exactement le service qu'on lui demande.
     */
    private static List<String> everySpriteTheGameAsksFor() {
        List<String> names = new ArrayList<>();
        for (Tile tile : Tile.values()) {
            names.add(tile.spriteName());
        }
        for (EnemyKind kind : EnemyKind.values()) {
            names.add(kind.spriteName());
        }
        // Les trois noms que le jeu écrit en dur dans la scène, faute d'énumération pour les porter.
        names.add("hero/idle");
        names.add("ground/plain");
        names.add("tile/empty");
        return names;
    }

    @Test
    @DisplayName("L'atlas contient tous les sprites que le jeu réclame")
    void theAtlasHoldsEverySpriteTheGameAsksFor() throws IOException {
        AtlasIndex atlas = index();

        List<String> missing = new ArrayList<>();
        for (String name : everySpriteTheGameAsksFor()) {
            if (!atlas.contains(name)) {
                missing.add(name);
            }
        }

        assertTrue(missing.isEmpty(), "sprites reclames par le jeu et absents de l'atlas : "
                + missing + "\n  l'atlas declare : " + atlas.names());
    }

    /**
     * L'inverse : un sprite dessiné que plus personne ne réclame.
     *
     * <p>Moins grave — un sprite orphelin ne fait rien disparaître — mais il coûte de la place dans
     * un atlas dont la taille est bornée, et il signale presque toujours un renommage à moitié fait.
     * Le test ne l'interdit pas, il le <b>nomme</b> : un art de démonstration a le droit d'exister,
     * mais on doit savoir qu'il est là.
     */
    @Test
    @DisplayName("Aucun sprite orphelin ne dort dans l'atlas sans qu'on le sache")
    void noOrphanSpriteSitsUnnoticed() throws IOException {
        AtlasIndex atlas = index();
        List<String> asked = everySpriteTheGameAsksFor();

        List<String> orphans = new ArrayList<>();
        for (String name : atlas.names()) {
            if (!asked.contains(name)) {
                orphans.add(name);
            }
        }

        // La mire de calibration en utilise quelques-uns qui ne passent pas par une énumération.
        // Au-delà d'une poignée, c'est qu'un renommage n'a été fait qu'à moitié.
        assertTrue(orphans.size() <= 2,
                "trop de sprites orphelins dans l'atlas : " + orphans
                        + "\n  soit le jeu a cesse de les demander, soit un renommage est incomplet");
    }

    /**
     * Les figures partagent une taille, et ce n'est pas cosmétique : la géométrie de la scène est
     * calculée dessus. Un sprite d'ennemi plus haut de deux pixels dépasserait dans la bande des
     * points de vie sans qu'aucun test de mise en page ne bronche, puisque celle-ci raisonne sur
     * {@code ArenaLayout.FIGURE_HEIGHT} et non sur le contenu réel de l'atlas.
     */
    @Test
    @DisplayName("Toutes les figures ont la taille que la mise en page suppose")
    void everyFigureHasTheSizeTheLayoutAssumes() throws IOException {
        AtlasIndex atlas = index();

        List<String> wrong = new ArrayList<>();
        List<String> figures = new ArrayList<>();
        figures.add("hero/idle");
        for (EnemyKind kind : EnemyKind.values()) {
            figures.add(kind.spriteName());
        }

        for (String name : figures) {
            if (!atlas.contains(name)) {
                continue; // l'autre test s'en charge, et son message est plus clair
            }
            AtlasLayout.Placement placement = atlas.region(name);
            if (placement.width() != com.starfall.game.ArenaLayout.FIGURE_WIDTH
                    || placement.height() != com.starfall.game.ArenaLayout.FIGURE_HEIGHT) {
                wrong.add(name + " fait " + placement.width() + "x" + placement.height());
            }
        }

        assertTrue(wrong.isEmpty(), "figures dont la taille contredit la mise en page ("
                + com.starfall.game.ArenaLayout.FIGURE_WIDTH + "x"
                + com.starfall.game.ArenaLayout.FIGURE_HEIGHT + ") : " + wrong);
    }
}
