package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
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

    /**
     * Les planches que le tableau de bord donne pour actuelles le sont vraiment.
     *
     * <h2>Le défaut que ce test ferme</h2>
     *
     * <p>Les planches-contact de ce projet sont <b>datées</b> : chacune documente son jalon tel
     * qu'il a été livré, et les rafraîchir reviendrait à réécrire l'histoire. La règle est bonne,
     * mais elle laisse le lecteur sans repère — aucune de ces images ne montre le jeu d'aujourd'hui,
     * et il n'a aucun moyen de savoir de combien chacune a vieilli. Le tableau de bord répondait
     * donc en désignant une planche : « la seule qui montre le jeu tel qu'il est aujourd'hui ».
     *
     * <p>Cette phrase est devenue fausse <b>en silence</b>, deux fois de suite. Elle désignait la
     * planche de M10 ; depuis, les tuiles ont reçu le chiffre de leurs dégâts, et la planche ne
     * montrait plus le jeu d'aujourd'hui sans que rien ne le signale. Une désignation qui n'est
     * reliée à rien ne survit pas au travail qu'elle est censée suivre.
     *
     * <h2>Comment il s'y prend</h2>
     *
     * <p>{@code captures/actuel} cesse d'être une convention de rangement pour devenir une
     * promesse : ses images doivent être identiques, <b>octet pour octet</b>, à des planches de
     * référence — c'est-à-dire à ce que le jeu dessine maintenant, puisque {@code verifyRender} le
     * garantit de son côté. Le jour où les visuels changeront, elles cesseront de correspondre et
     * ce test rougira : il faudra les reprendre, ou renoncer à la promesse. C'est le seul des
     * chiffres et des affirmations du tableau de bord qui, avec le compte de planches, soit
     * <em>exactement</em> vérifiable.
     */
    @Test
    @DisplayName("Les planches données pour actuelles le sont, à l'octet près")
    void thePlatesPresentedAsCurrentReallyAre() throws IOException {
        Path root = repositoryRoot();
        Path current = root.resolve("captures/actuel");
        assertTrue(Files.isDirectory(current),
                "captures/actuel a disparu, et avec lui les seules planches du tableau de bord qui"
                        + " pretendent montrer le jeu d'aujourd'hui");

        Set<String> reference = new HashSet<>();
        try (Stream<Path> screens = Files.list(root.resolve("captures/reference"))) {
            for (Path screen : screens.filter(Files::isDirectory).toList()) {
                try (Stream<Path> plates = Files.list(screen)) {
                    for (Path plate : plates.filter(f -> f.toString().endsWith(".png")).toList()) {
                        reference.add(digest(plate));
                    }
                }
            }
        }

        List<Path> claimed;
        try (Stream<Path> files = Files.list(current)) {
            claimed = files.filter(f -> f.toString().endsWith(".png")).sorted().toList();
        }

        // La premisse, assertee et non supposee : un dossier vide rendrait la boucle ci-dessous
        // vraie sans rien avoir regarde, et le tableau de bord continuerait d'affirmer que trois
        // planches montrent le jeu d'aujourd'hui.
        assertFalse(claimed.isEmpty(),
                "aucune planche dans captures/actuel : le tableau de bord promet qu'il en contient,"
                        + " et ce test ne comparerait plus rien");

        List<String> stale = new ArrayList<>();
        for (Path plate : claimed) {
            if (!reference.contains(digest(plate))) {
                stale.add(plate.getFileName().toString());
            }
        }
        assertTrue(stale.isEmpty(),
                "ces planches sont donnees pour actuelles et ne correspondent a aucune planche de"
                        + " reference : le jeu ne les dessine plus ainsi, donc le tableau de bord"
                        + " montre un jeu qui n'existe plus. " + stale);
    }

    /** L'empreinte d'un fichier. Deux planches identiques a l'octet ont la meme. */
    private static String digest(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
