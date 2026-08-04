package com.starfall.art;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Index de l'atlas : quel sprite se trouve où dans le PNG.
 *
 * <p>Écrit par l'étape de build, relu par le jeu. C'est du texte, donc un diff montre qu'un sprite a
 * changé de taille ou de nom — contrairement au PNG, dont le diff ne dit rien.
 *
 * <pre>
 *   # commentaire
 *   atlas 128 64
 *   hero/idle 1 1 16 32
 * </pre>
 *
 * <p>Le repère est celui de l'image : origine en haut à gauche, Y vers le bas.
 */
public final class AtlasIndex {

    /**
     * Nom de l'en-tête, et donc nom interdit pour un sprite : un sprite ainsi nommé rendrait
     * l'index illisible. {@link SpriteParser} le refuse au build pour cette raison.
     */
    public static final String RESERVED_NAME = "atlas";

    private static final String ATLAS_DIRECTIVE = RESERVED_NAME;

    private final int width;
    private final int height;
    private final Map<String, AtlasLayout.Placement> regions;

    private AtlasIndex(int width, int height, Map<String, AtlasLayout.Placement> regions) {
        this.width = width;
        this.height = height;
        this.regions = regions;
    }

    public static AtlasIndex of(AtlasLayout layout) {
        Map<String, AtlasLayout.Placement> regions = new LinkedHashMap<>();
        for (AtlasLayout.Placement placement : layout.placements()) {
            regions.put(placement.name(), placement);
        }
        return new AtlasIndex(layout.width(), layout.height(), regions);
    }

    /** Rend le fichier d'index, prêt à être écrit. */
    public String render() {
        StringBuilder builder = new StringBuilder();
        // Les en-têtes restent sans accent : ce fichier est écrit puis relu par la machine, et
        // rester en ASCII pur évite tout aléa d'encodage sur le seul artefact du pipeline qui
        // traverse un cycle écriture/relecture.
        builder.append("# Index d'atlas Starfall - genere par AtlasBuilder, ne pas editer a la main.\n");
        builder.append("# Repere de l'image : origine en haut a gauche, Y vers le bas.\n");
        builder.append("# <nom> <x> <y> <largeur> <hauteur>\n");
        builder.append(ATLAS_DIRECTIVE).append(' ').append(width).append(' ').append(height).append('\n');
        for (AtlasLayout.Placement placement : regions.values()) {
            builder.append(placement.name())
                    .append(' ').append(placement.x())
                    .append(' ').append(placement.y())
                    .append(' ').append(placement.width())
                    .append(' ').append(placement.height())
                    .append('\n');
        }
        return builder.toString();
    }

    /**
     * Relit un index.
     *
     * @param source nom affiché dans les messages d'erreur
     * @param lines  contenu, une entrée par ligne
     */
    public static AtlasIndex parse(String source, List<String> lines) {
        int width = -1;
        int height = -1;
        Map<String, AtlasLayout.Placement> regions = new LinkedHashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = stripComment(lines.get(i)).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\\s+");

            if (ATLAS_DIRECTIVE.equals(fields[0])) {
                if (fields.length != 3) {
                    throw new ArtFormatException(source, lineNumber,
                            "« atlas » attend une largeur et une hauteur");
                }
                if (width >= 0) {
                    // Sans cela, la dernière ligne « atlas » gagnait en silence, et toutes les
                    // régions se retrouvaient validées contre une taille qui n'était pas la bonne.
                    throw new ArtFormatException(source, lineNumber,
                            "l'index déclare deux fois la taille de l'atlas");
                }
                width = requirePositive(source, lineNumber, fields[1], "largeur d'atlas");
                height = requirePositive(source, lineNumber, fields[2], "hauteur d'atlas");
                continue;
            }
            if (width < 0) {
                throw new ArtFormatException(source, lineNumber,
                        "région déclarée avant la ligne « atlas <largeur> <hauteur> »");
            }

            if (fields.length != 5) {
                throw new ArtFormatException(source, lineNumber,
                        "une région s'écrit « <nom> <x> <y> <largeur> <hauteur> », reçu "
                                + fields.length + " champ(s)");
            }
            String name = fields[0];
            if (regions.containsKey(name)) {
                throw new ArtFormatException(source, lineNumber, "région en double : « " + name + " »");
            }
            AtlasLayout.Placement placement = new AtlasLayout.Placement(name,
                    parseInt(source, lineNumber, fields[1]),
                    parseInt(source, lineNumber, fields[2]),
                    requirePositive(source, lineNumber, fields[3], "largeur de région"),
                    requirePositive(source, lineNumber, fields[4], "hauteur de région"));
            checkInsideAtlas(source, lineNumber, placement, width, height);
            checkNoOverlap(source, lineNumber, placement, regions.values());
            regions.put(name, placement);
        }

        if (width < 0 || height < 0) {
            throw new ArtFormatException(source + " : ligne « atlas <largeur> <hauteur> » manquante");
        }
        if (regions.isEmpty()) {
            throw new ArtFormatException(source + " : aucune région déclarée");
        }
        return new AtlasIndex(width, height, regions);
    }

    /**
     * Une région qui sort de l'image ne provoque aucune erreur au rendu : elle donne un sprite vide
     * ou étiré, en silence. C'est le mode d'échec le plus dangereux du pipeline, puisque la boucle
     * de review du projet ne juge que sur des captures.
     */
    private static void checkInsideAtlas(String source, int lineNumber,
                                         AtlasLayout.Placement placement, int width, int height) {
        if (placement.x() < 0 || placement.y() < 0
                || placement.right() > width || placement.bottom() > height) {
            throw new ArtFormatException(source, lineNumber,
                    "la région « " + placement.name() + " » occupe "
                            + placement.x() + "," + placement.y() + " à "
                            + placement.right() + "," + placement.bottom()
                            + ", en dehors de l'atlas " + width + "x" + height);
        }
    }

    /**
     * Deux régions qui se recouvrent afficheraient chacune un bout de l'autre. L'index étant
     * produit par la machine, un chevauchement ne peut venir que d'une corruption ou d'une édition
     * à la main — dans les deux cas mieux vaut s'arrêter que dessiner n'importe quoi.
     */
    private static void checkNoOverlap(String source, int lineNumber, AtlasLayout.Placement placement,
                                       Iterable<AtlasLayout.Placement> existing) {
        for (AtlasLayout.Placement other : existing) {
            if (placement.overlaps(other)) {
                throw new ArtFormatException(source, lineNumber,
                        "la région « " + placement.name() + " » recouvre « " + other.name() + " »");
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(String name) {
        return regions.containsKey(name);
    }

    /**
     * Emplacement d'un sprite.
     *
     * @throws ArtFormatException si le nom est inconnu - un sprite absent doit se voir tout de
     *                            suite, pas se traduire par un carré invisible à l'écran
     */
    public AtlasLayout.Placement region(String name) {
        AtlasLayout.Placement placement = regions.get(name);
        if (placement == null) {
            throw new ArtFormatException("sprite inconnu dans l'atlas : « " + name + " » (connus : "
                    + String.join(", ", regions.keySet()) + ")");
        }
        return placement;
    }

    /** Noms de tous les sprites, dans l'ordre du fichier. */
    public List<String> names() {
        return new ArrayList<>(regions.keySet());
    }

    private static int parseInt(String source, int lineNumber, String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ArtFormatException(source, lineNumber, "entier attendu, reçu « " + text + " »");
        }
    }

    private static int requirePositive(String source, int lineNumber, String text, String what) {
        int value = parseInt(source, lineNumber, text);
        if (value < 1) {
            throw new ArtFormatException(source, lineNumber,
                    what + " : entier strictement positif attendu, reçu " + value);
        }
        return value;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }
}
