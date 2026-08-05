package com.starfall;

import com.starfall.scene.CalibrationScene;
import com.starfall.scene.CaptureScenario;
import com.starfall.scene.CaptureScript;
import com.starfall.scene.ShowcaseScript;


import com.starfall.game.Arena;
import com.starfall.game.Grid;

import java.util.List;
import java.util.Locale;

/**
 * Options de ligne de commande, partagées par le lanceur et le jeu.
 *
 * <pre>
 *   --screenshot &lt;dossier&gt;   rend quelques images, écrit un PNG par image, puis quitte
 *   --size &lt;L&gt;x&lt;H&gt;           taille initiale de la fenêtre (défaut 1280x720)
 *   --frames &lt;N&gt;             nombre d'images à rendre en mode capture (défaut 2)
 *   --from &lt;N&gt;               première image du scénario à rendre (défaut 0)
 *   --scene &lt;nom&gt;            voir {@link #SCENES} — l'aide d'exécution les énumère
 *   --grid &lt;N&gt;               largeur de la grille, de 5 à 15 cases (défaut 9)
 *   --wave &lt;N&gt;               vague de départ, pour aller voir directement la fin (défaut 1)
 *   --simulate &lt;N&gt;           joue N parties par politique et imprime le bilan chiffré, sans ouvrir de fenêtre
 *   --help                   affiche l'aide et quitte
 * </pre>
 *
 * <p>L'analyse est <b>stricte</b> : toute option inconnue, mal orthographiée ou sans valeur est une
 * erreur fatale. La boucle de review du projet est automatisée, et une faute de frappe qui produit
 * silencieusement le mauvais résultat en sortie 0 est exactement le faux positif à proscrire.
 */
public final class LaunchOptions {

    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int DEFAULT_FRAMES = 2;
    public static final String DEFAULT_SCENE = CaptureScript.SCENE_NAME;
    public static final int DEFAULT_GRID_WIDTH = 9;

    /** Scènes connues. La mire de calibration reste atteignable : c'est une preuve de non-régression. */
    public static final List<String> SCENES =
            List.of(CaptureScript.SCENE_NAME, CalibrationScene.SCENE_NAME,
                    ShowcaseScript.SCENE_NAME);

    /** Dossier de sortie des captures, ou {@code null} en fonctionnement normal. */
    public final String screenshotDir;
    public final int width;
    public final int height;
    public final int frames;

    /**
     * Première image du scénario à rendre.
     *
     * <p>Elle existe pour une raison précise : le scénario de capture mène maintenant la tranche
     * jusqu'à la victoire en 87 gestes, et garder la <b>bannière de fin</b> demandait de rendre les
     * 88 images pour n'en regarder que la dernière. Une planche de référence qu'on ne peut pas
     * relire à l'œil est une planche qu'on finit par adopter sans regarder — c'est-à-dire pire que
     * pas de planche du tout.
     *
     * <p>Avec {@code --from}, huit images suffisent à garder la fin de partie.
     */
    public final int firstFrame;
    public final String scene;
    /** Largeur de la grille de combat, en cases. Bornée par {@code Grid}, pas ici. */
    public final int gridWidth;
    /**
     * Vague à laquelle la partie commence.
     *
     * <p>Elle existe pour une raison pratique : la rencontre du souverain est la quatrième et
     * dernière, donc l'atteindre demande de gagner trois vagues. Ni une capture reproductible ni un
     * relecteur ne peuvent se le permettre — et une mécanique qu'on ne peut pas montrer est une
     * mécanique qu'on ne relit pas.
     */
    public final int startWave;
    /**
     * Nombre de parties simulées par politique, ou {@code 0} pour jouer normalement.
     *
     * <p>Le jalon d'équilibrage est le seul où l'on ne peut rien prouver en lisant le code : un
     * nombre n'est ni juste ni faux, il est trop grand ou trop petit <em>pour quelqu'un qui joue</em>.
     * Cette option rend la mesure refaisable par n'importe qui, au lieu de demander qu'on me croie.
     */
    public final int simulations;
    /** Vrai si l'aide a été demandée : le lanceur l'affiche puis s'arrête sans ouvrir de fenêtre. */
    public final boolean helpRequested;

    private LaunchOptions(String screenshotDir, int width, int height, int frames, int firstFrame,
                          String scene, int gridWidth, int startWave, int simulations,
                          boolean helpRequested) {
        this.screenshotDir = screenshotDir;
        this.width = width;
        this.height = height;
        this.frames = frames;
        this.firstFrame = firstFrame;
        this.scene = scene;
        this.gridWidth = gridWidth;
        this.startWave = startWave;
        this.simulations = simulations;
        this.helpRequested = helpRequested;
    }

    public boolean isScreenshotMode() {
        return screenshotDir != null;
    }

    public static LaunchOptions parse(String[] args) {
        String screenshotDir = null;
        int width = DEFAULT_WIDTH;
        int height = DEFAULT_HEIGHT;
        int frames = DEFAULT_FRAMES;
        int firstFrame = 0;
        String scene = DEFAULT_SCENE;
        int gridWidth = DEFAULT_GRID_WIDTH;
        int startWave = 1;
        int simulations = 0;
        boolean helpRequested = false;

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--screenshot":
                        screenshotDir = requireValue(args, ++i, arg);
                        break;
                    case "--size": {
                        String value = requireValue(args, ++i, arg).toLowerCase(Locale.ROOT);
                        int x = value.indexOf('x');
                        if (x <= 0 || x == value.length() - 1) {
                            throw new IllegalArgumentException("--size attend LxH, reçu : " + value);
                        }
                        width = requirePositive(value.substring(0, x), arg);
                        height = requirePositive(value.substring(x + 1), arg);
                        break;
                    }
                    case "--frames":
                        frames = requirePositive(requireValue(args, ++i, arg), arg);
                        break;
                    case "--from":
                        // Zéro est légitime -- c'est le défaut -- donc « positif » ne convient pas.
                        firstFrame = requireNonNegative(requireValue(args, ++i, arg), arg);
                        break;
                    case "--scene": {
                        scene = requireValue(args, ++i, arg).toLowerCase(Locale.ROOT);
                        if (!SCENES.contains(scene)) {
                            throw new IllegalArgumentException("--scene attend " + String.join(" ou ", SCENES)
                                    + ", reçu : " + scene);
                        }
                        break;
                    }
                    case "--grid": {
                        // Les bornes viennent de Grid, elles ne sont pas recopiées : les redire ici
                        // les ferait diverger le jour où elles bougeraient. En revanche il faut les
                        // vérifier ICI. Ne contrôler que la forme laissait « --grid 20 » ouvrir la
                        // fenêtre puis planter avec le code 1, alors qu'une ligne de commande
                        // invalide doit sortir en 2 sans rien ouvrir.
                        gridWidth = requirePositive(requireValue(args, ++i, arg), arg);
                        if (gridWidth < Grid.MIN_WIDTH || gridWidth > Grid.MAX_WIDTH) {
                            throw new IllegalArgumentException("--grid attend de " + Grid.MIN_WIDTH
                                    + " à " + Grid.MAX_WIDTH + " cases, reçu : " + gridWidth);
                        }
                        break;
                    }
                    case "--wave": {
                        // Mêmes bornes vérifiées ICI qu'ailleurs : une ligne de commande invalide
                        // doit sortir en 2 sans ouvrir de fenêtre, jamais planter une fois ouverte.
                        startWave = requirePositive(requireValue(args, ++i, arg), arg);
                        if (startWave > Arena.WAVE_COUNT) {
                            throw new IllegalArgumentException("--wave attend de 1 à "
                                    + Arena.WAVE_COUNT + ", reçu : " + startWave);
                        }
                        break;
                    }
                    case "--simulate":
                        simulations = requirePositive(requireValue(args, ++i, arg), arg);
                        break;
                    case "--help":
                    case "-h":
                        helpRequested = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Argument inconnu : " + arg);
                }
            }
        }

        // Demander des images que le scénario n'a pas est une erreur de ligne de commande, pas un
        // détail : sans ce contrôle, « --from 200 » écrivait trois PNG rigoureusement identiques et
        // sortait en 0. Le projet a déjà eu ce défaut sous la forme d'un « --frames N » qui écrivait
        // N copies de la même image ; --from rouvrait exactement la même porte.
        // L'aide sort avant tout refus : « --help » est documente « affiche l'aide et quitte », et
        // il le faisait jusqu'a ce que le refus ci-dessous s'interpose. Mesure : « --help --scene
        // showcase » rendait le code 2 et l'usage sur la sortie d'erreur. Une option qui explique
        // le programme ne peut pas etre refusee par le programme.
        if (helpRequested) {
            return new LaunchOptions(screenshotDir, width, height, frames, firstFrame, scene,
                    gridWidth, startWave, simulations, true);
        }

        // Une simulation n'ouvre aucune fenetre et n'instancie aucune scene : le refus y est sans
        // objet, et le poser reviendrait a refuser une commande qui marche.
        if (simulations > 0) {
            return new LaunchOptions(screenshotDir, width, height, frames, firstFrame, scene,
                    gridWidth, startWave, simulations, false);
        }

        // La vitrine n'existe qu'en mode capture : son scenario n'est rejoue que la, et sans
        // « --screenshot » elle ouvrirait une partie ordinaire en pretendant montrer autre chose.
        // Le tableau de bord l'a d'ailleurs annoncee jouable pendant un commit entier, ce qui etait
        // faux -- une review l'a releve. Une option qui ment vaut mieux refusee qu'expliquee.
        if (screenshotDir == null && ShowcaseScript.SCENE_NAME.equals(scene)) {
            throw new IllegalArgumentException("--scene " + ShowcaseScript.SCENE_NAME
                    + " demande --screenshot : son scénario n'est rejoué qu'en mode capture");
        }
        if (screenshotDir != null && !CalibrationScene.SCENE_NAME.equals(scene)) {
            // La borne vient du scénario de CETTE scène : les deux n'ont pas la même longueur, et
            // une borne unique aurait laissé passer un --from hors sujet. Les longueurs ne sont pas
            // écrites ici — elles ont déjà vieilli ailleurs.
            int available = CaptureScenario.forScene(scene).size();
            int last = firstFrame + frames - 1;
            if (last > available) {
                throw new IllegalArgumentException("--from " + firstFrame + " avec --frames "
                        + frames + " demande l'image " + last + ", or le scenario « " + scene
                        + " » en compte " + (available + 1) + " (0 a " + available + ")");
            }
        }
        return new LaunchOptions(screenshotDir, width, height, frames, firstFrame, scene, gridWidth,
                startWave, simulations, helpRequested);
    }

    /**
     * Lit la valeur qui suit une option. Une valeur qui ressemble elle-même à une option est
     * refusée : {@code --screenshot --size 800x450} créerait sinon un dossier nommé « --size ».
     */
    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " attend une valeur");
        }
        String value = args[index];
        if (value.startsWith("--")) {
            throw new IllegalArgumentException(option + " attend une valeur, mais a reçu l'option " + value);
        }
        return value;
    }

    /**
     * Comme {@link #requirePositive}, mais zéro est accepté : c'est le défaut de {@code --from},
     * et refuser la valeur par défaut serait une drôle de validation.
     */
    private static int requireNonNegative(String raw, String option) {
        return requireAtLeast(raw, option, 0);
    }

    private static int requirePositive(String raw, String option) {
        return requireAtLeast(raw, option, 1);
    }

    /**
     * Un entier au moins égal à {@code minimum}.
     *
     * <p>Les deux formes ci-dessus étaient écrites en toutes lettres, à quatorze lignes près, dans
     * un projet dont la phrase récurrente est qu'une règle écrite à deux endroits finit par
     * diverger. Un paramètre suffisait.
     */
    private static int requireAtLeast(String raw, String option, int minimum) {
        String text = raw.trim();
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException(option + " attend un entier, reçu : " + text);
        }
        if (value < minimum) {
            throw new IllegalArgumentException(option + " attend un entier "
                    + (minimum == 0 ? "positif ou nul" : "au moins égal à " + minimum)
                    + ", reçu : " + value);
        }
        return value;
    }

    public static String usage() {
        return "Starfall\n"
                + "  --screenshot <dossier>   rend des images en PNG dans <dossier> puis quitte\n"
                + "  --size <L>x<H>           taille de la fenêtre (défaut " + DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT + ")\n"
                + "  --frames <N>             images à capturer en mode capture (défaut " + DEFAULT_FRAMES + ")\n"
                + "  --from <N>               première image du scénario à rendre (défaut 0)\n"
                + "  --scene <nom>            " + String.join(" ou ", SCENES) + " (défaut " + DEFAULT_SCENE + ")\n"
                + "  --simulate <N>           joue N parties par politique et imprime le bilan\n"
                + "  --wave <N>               vague de départ, 1 à " + Arena.WAVE_COUNT
                + " (défaut 1)\n"
                + "  --grid <N>               largeur de la grille, " + Grid.MIN_WIDTH + " à "
                + Grid.MAX_WIDTH + " cases (défaut " + DEFAULT_GRID_WIDTH + ")\n"
                + "  --help                   affiche cette aide\n";
    }

    @Override
    public String toString() {
        return "LaunchOptions{screenshotDir=" + screenshotDir
                + ", size=" + width + "x" + height
                + ", frames=" + frames
                + (firstFrame > 0 ? ", from=" + firstFrame : "")
                + ", scene=" + scene
                + ", grid=" + gridWidth + ", wave=" + startWave
                + (simulations > 0 ? ", simulate=" + simulations : "") + '}';
    }
}
