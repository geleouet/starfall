package com.starfall;

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
 *   --scene &lt;nom&gt;            arena (défaut) ou calibration
 *   --grid &lt;N&gt;               largeur de la grille, de 5 à 15 cases (défaut 9)
 *   --wave &lt;N&gt;               vague de départ, pour aller voir directement la fin (défaut 1)
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
    public static final String DEFAULT_SCENE = "arena";
    public static final int DEFAULT_GRID_WIDTH = 9;

    /** Scènes connues. La mire de calibration reste atteignable : c'est une preuve de non-régression. */
    public static final List<String> SCENES = List.of("arena", "calibration");

    /** Dossier de sortie des captures, ou {@code null} en fonctionnement normal. */
    public final String screenshotDir;
    public final int width;
    public final int height;
    public final int frames;
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
    /** Vrai si l'aide a été demandée : le lanceur l'affiche puis s'arrête sans ouvrir de fenêtre. */
    public final boolean helpRequested;

    private LaunchOptions(String screenshotDir, int width, int height, int frames,
                          String scene, int gridWidth, int startWave, boolean helpRequested) {
        this.screenshotDir = screenshotDir;
        this.width = width;
        this.height = height;
        this.frames = frames;
        this.scene = scene;
        this.gridWidth = gridWidth;
        this.startWave = startWave;
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
        String scene = DEFAULT_SCENE;
        int gridWidth = DEFAULT_GRID_WIDTH;
        int startWave = 1;
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
                    case "--help":
                    case "-h":
                        helpRequested = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Argument inconnu : " + arg);
                }
            }
        }

        return new LaunchOptions(screenshotDir, width, height, frames, scene, gridWidth,
                startWave, helpRequested);
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

    private static int requirePositive(String raw, String option) {
        String text = raw.trim();
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " attend un entier, reçu : " + text);
        }
        if (value < 1) {
            throw new IllegalArgumentException(option + " attend un entier strictement positif, reçu : " + value);
        }
        return value;
    }

    public static String usage() {
        return "Starfall\n"
                + "  --screenshot <dossier>   rend des images en PNG dans <dossier> puis quitte\n"
                + "  --size <L>x<H>           taille de la fenêtre (défaut " + DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT + ")\n"
                + "  --frames <N>             images à capturer en mode capture (défaut " + DEFAULT_FRAMES + ")\n"
                + "  --scene <nom>            " + String.join(" ou ", SCENES) + " (défaut " + DEFAULT_SCENE + ")\n"
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
                + ", scene=" + scene
                + ", grid=" + gridWidth + ", wave=" + startWave + '}';
    }
}
