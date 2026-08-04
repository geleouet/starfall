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

    private static final String ATLAS_DIRECTIVE = "atlas";

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
                width = parseInt(source, lineNumber, fields[1]);
                height = parseInt(source, lineNumber, fields[2]);
                continue;
            }

            if (fields.length != 5) {
                throw new ArtFormatException(source, lineNumber,
                        "une region s'ecrit « <nom> <x> <y> <largeur> <hauteur> », recu "
                                + fields.length + " champ(s)");
            }
            String name = fields[0];
            if (regions.containsKey(name)) {
                throw new ArtFormatException(source, lineNumber, "region en double : « " + name + " »");
            }
            regions.put(name, new AtlasLayout.Placement(name,
                    parseInt(source, lineNumber, fields[1]),
                    parseInt(source, lineNumber, fields[2]),
                    parseInt(source, lineNumber, fields[3]),
                    parseInt(source, lineNumber, fields[4])));
        }

        if (width < 0 || height < 0) {
            throw new ArtFormatException(source + " : ligne « atlas <largeur> <hauteur> » manquante");
        }
        if (regions.isEmpty()) {
            throw new ArtFormatException(source + " : aucune region declaree");
        }
        return new AtlasIndex(width, height, regions);
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
            throw new ArtFormatException(source, lineNumber, "entier attendu, recu « " + text + " »");
        }
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }
}
