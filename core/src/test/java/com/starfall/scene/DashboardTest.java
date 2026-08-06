package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le tableau de bord ne doit pas mentir sur ce qu'il compte.
 *
 * <h2>Pourquoi ce test existe</h2>
 *
 * <p>{@code progress.html} est la mémoire du projet, et ses chiffres ont dérivé plusieurs fois :
 * un compte de planches resté en arrière d'un écran, un ratio faux d'un facteur douze, une phrase
 * affirmant une intro qui n'avait pas été écrite. Chaque fois, une <b>review</b> l'a relevé — et la
 * règle que ce projet en a tirée est qu'un chiffre invérifiable n'a pas sa place dans sa mémoire.
 *
 * <p>Le nombre de planches et d'écrans est le seul de ces chiffres qui soit <em>exactement</em>
 * vérifiable : il est sur le disque. Ce test le vérifie, et ferme ainsi la seule porte par laquelle
 * l'affirmation la plus répétée du tableau de bord pouvait vieillir en silence.
 *
 * <h2>Ce qu'il ne vérifie pas, et pourquoi</h2>
 *
 * <p>Les entrées du <b>journal</b> portent les comptes de leur époque — 71 planches sur 11, 84 sur
 * 12 — et c'est voulu : ce sont des relevés datés, comme les planches-contact. Les rafraîchir
 * reviendrait à réécrire l'histoire. Seule la ligne d'<b>en-tête</b> parle du présent, et c'est
 * donc la seule que ce test lit.
 */
class DashboardTest {

    /** Le chiffre du présent, en tête de page : « N planches sur M écrans ». */
    private static final Pattern HEAD_FIGURE =
            Pattern.compile("(\\d+) planches sur (\\d+) &eacute;crans");

    private static Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            if (Files.isRegularFile(candidate.resolve("progress.html"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("progress.html introuvable depuis " + Path.of("").toAbsolutePath());
    }

    @Test
    @DisplayName("L'en-tête du tableau de bord compte les planches qui existent")
    void theDashboardHeadCountsThePlatesThatExist() throws IOException {
        Path root = repositoryRoot();
        String page = Files.readString(root.resolve("progress.html"), StandardCharsets.UTF_8);
        String head = page.substring(0, page.indexOf("<h2>"));

        Matcher figure = HEAD_FIGURE.matcher(head);
        assertTrue(figure.find(),
                "l'en-tete ne dit plus combien de planches le projet garde : c'est l'affirmation"
                        + " la plus repetee du tableau de bord, et la seule qui soit exactement"
                        + " verifiable");

        Path reference = root.resolve("captures/reference");
        List<Path> screens;
        try (Stream<Path> entries = Files.list(reference)) {
            screens = entries.filter(Files::isDirectory).toList();
        }
        int plates = 0;
        for (Path screen : screens) {
            try (Stream<Path> files = Files.list(screen)) {
                plates += (int) files.filter(f -> f.toString().endsWith(".png")).count();
            }
        }

        assertEquals(plates, Integer.parseInt(figure.group(1)),
                "l'en-tete annonce " + figure.group(1) + " planches, il y en a " + plates
                        + " dans captures/reference");
        assertEquals(screens.size(), Integer.parseInt(figure.group(2)),
                "l'en-tete annonce " + figure.group(2) + " ecrans, il y en a " + screens.size());

        // Et la premisse : sans planches, les deux egalites ci-dessus seraient vraies pour rien.
        assertTrue(plates > 0 && !screens.isEmpty(),
                "aucune planche de reference : ce test ne compare plus rien");
    }
}
