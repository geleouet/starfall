package com.starfall.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le pont entre la <b>fenêtre de capture</b>, écrite en Groovy, et le <b>scénario</b>, écrit en Java.
 *
 * <h2>Pourquoi ce test existe</h2>
 *
 * <p>La vitrine ne vaut que par sa <b>dernière</b> image : c'est elle qui porte la bannière de
 * défaite, la file encore garnie et le râtelier survolé sur un état terminal. Or la fenêtre qui
 * décide des images rendues — {@code --from 1 --frames 11} — est un couple de littéraux dans
 * {@code lwjgl3/build.gradle}, et la longueur du scénario est une constante Java. <b>Rien ne liait
 * les deux.</b>
 *
 * <p>Une review l'a démontré : en ajoutant un geste au scénario, la borne de {@code --from} continue
 * d'accepter la fenêtre, et l'image de la mort <em>disparaît silencieusement de la référence</em>.
 * Aucun test, aucune assertion Gradle ne le voyait. C'est le motif « le scénario s'arrête un geste
 * avant l'intéressant », déplacé dans le fichier de construction.
 *
 * <p>Lire le fichier de build depuis un test n'est pas élégant, mais c'est le seul endroit où les
 * deux nombres se rencontrent — et le projet a déjà un précédent assumé : {@code AtlasCoverageTest}
 * lit l'index généré pour confronter ce que le jeu réclame à ce que l'atlas contient.
 */
class RenderWindowTest {

    private static Path buildScript() {
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            Path script = candidate.resolve("lwjgl3/build.gradle");
            if (Files.isRegularFile(script)) {
                return script;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("lwjgl3/build.gradle introuvable depuis "
                + Path.of("").toAbsolutePath());
    }

    /** La ligne d'un scénario de rendu, telle qu'elle est déclarée dans le tableau Groovy. */
    private static String declarationOf(String scenario) throws IOException {
        String script = Files.readString(buildScript(), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile(
                "^\\s*" + Pattern.quote(scenario) + "\\s*:\\s*\\[(.*?)\\]",
                Pattern.MULTILINE | Pattern.DOTALL).matcher(script);
        assertTrue(matcher.find(), "aucun scenario de rendu nomme « " + scenario
                + " » dans lwjgl3/build.gradle");
        return matcher.group(1);
    }

    /** La valeur d'une option de ligne de commande dans cette déclaration, ou le défaut donné. */
    private static int optionOf(String declaration, String option, int fallback) {
        Matcher matcher = Pattern.compile(
                Pattern.quote("'" + option + "'") + "\\s*,\\s*'(\\d+)'").matcher(declaration);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    /**
     * La fenêtre de la vitrine s'arrête exactement sur sa dernière image.
     *
     * <p>Ni avant — on perdrait l'état terminal, qui est toute sa raison d'être — ni après, ce que
     * la borne de {@code --from} refuse déjà.
     */
    @Test
    @DisplayName("La fenêtre de capture de la vitrine couvre sa dernière image")
    void theShowcaseWindowEndsOnItsLastFrame() throws IOException {
        String declaration = declarationOf(ShowcaseScript.SCENE_NAME);
        int from = optionOf(declaration, "--from", 0);
        int frames = optionOf(declaration, "--frames", 2);

        assertEquals(ShowcaseScript.ACTIONS.size(), from + frames - 1,
                "la fenetre de capture (--from " + from + " --frames " + frames + ") s'arrete a"
                        + " l'image " + (from + frames - 1) + ", or le scenario en compte "
                        + ShowcaseScript.ACTIONS.size() + " : l'etat terminal, qui porte la"
                        + " banniere de defaite et le ratelier survole, sortirait de la reference"
                        + " sans qu'aucun test ne le dise");
        assertTrue(from >= 1,
                "l'image 0 d'un scenario d'arene est l'etat initial, donc un duplicata de celle de"
                        + " la ligne gagnante : la fenetre doit commencer plus loin");
    }

    /**
     * Et la fenêtre de la fin de partie s'arrête, elle aussi, sur la dernière image de la ligne
     * gagnante — celle qui porte la bannière de victoire.
     */
    @Test
    @DisplayName("La fenêtre de la fin de partie couvre la dernière image de la ligne gagnante")
    void theVictoryWindowEndsOnTheLastFrame() throws IOException {
        String declaration = declarationOf("arenaVictory");
        int from = optionOf(declaration, "--from", 0);
        int frames = optionOf(declaration, "--frames", 2);

        assertEquals(CaptureScript.ACTIONS.size(), from + frames - 1,
                "la fenetre de la fin de partie (--from " + from + " --frames " + frames + ")"
                        + " n'atteint pas la derniere image du scenario ("
                        + CaptureScript.ACTIONS.size() + ") : la banniere de victoire sortirait de"
                        + " la reference");
    }
}
