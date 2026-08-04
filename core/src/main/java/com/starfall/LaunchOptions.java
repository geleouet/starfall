package com.starfall;

import java.util.Locale;

/**
 * Options de ligne de commande, partagées par le lanceur et le jeu.
 *
 * <pre>
 *   --screenshot &lt;dossier&gt;   rend quelques images, écrit un PNG par image, puis quitte
 *   --size &lt;L&gt;x&lt;H&gt;           taille initiale de la fenêtre (défaut 1280x720)
 *   --frames &lt;N&gt;             nombre d'images à rendre en mode capture (défaut 2)
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

    /** Dossier de sortie des captures, ou {@code null} en fonctionnement normal. */
    public final String screenshotDir;
    public final int width;
    public final int height;
    public final int frames;
    /** Vrai si l'aide a été demandée : le lanceur l'affiche puis s'arrête sans ouvrir de fenêtre. */
    public final boolean helpRequested;

    private LaunchOptions(String screenshotDir, int width, int height, int frames, boolean helpRequested) {
        this.screenshotDir = screenshotDir;
        this.width = width;
        this.height = height;
        this.frames = frames;
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
                    case "--help":
                    case "-h":
                        helpRequested = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Argument inconnu : " + arg);
                }
            }
        }

        return new LaunchOptions(screenshotDir, width, height, frames, helpRequested);
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
                + "  --help                   affiche cette aide\n";
    }

    @Override
    public String toString() {
        return "LaunchOptions{screenshotDir=" + screenshotDir
                + ", size=" + width + "x" + height
                + ", frames=" + frames + '}';
    }
}
