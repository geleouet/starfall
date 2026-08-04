package com.starfall.scene;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Hero;
import com.starfall.game.Tile;
import com.starfall.game.TilePreview;

import java.util.List;

/**
 * Tout ce que l'interface <b>dit</b>, en français, séparé de l'endroit où elle le dessine.
 *
 * <h2>Pourquoi c'est une classe à part</h2>
 *
 * <p>Un libellé d'interface peut échouer de deux façons qu'aucun coup d'œil ne rattrape :
 *
 * <ul>
 *   <li>il contient un caractère que la police ne sait pas dessiner — et la police <em>saute en
 *       silence</em> ce qu'elle ne connaît pas, donc « PRÊTE » s'affiche « PRTE » sans rien
 *       signaler ;</li>
 *   <li>il est trop long pour la zone garantie, donc il sort de l'écran sur certaines fenêtres et
 *       pas sur d'autres — jamais sur la machine de développement, évidemment.</li>
 * </ul>
 *
 * <p>Tant que ces phrases vivaient au milieu du code de rendu, les vérifier demandait un contexte
 * graphique, c'est-à-dire ne les vérifiait pas. Ici, elles sont pures : {@code HudTextTest} les
 * engendre toutes sur des milliers d'états de partie et mesure chacune.
 *
 * <p>Les cases sont nommées de 1 à N pour le joueur, jamais de 0 à N-1.
 */
public final class HudText {

    private HudText() {
    }

    /**
     * Bandeau d'état.
     *
     * <p>Il ne porte que ce que la scène ne montre pas déjà. Les points de vie y figurent tout de
     * même en chiffres : les pastilles au-dessus des têtes répondent à « est-ce que ça va », pas à
     * « combien exactement », et la différence décide parfois du tour.
     */
    public static String banner(Arena arena) {
        StringBuilder line = new StringBuilder();
        line.append("VAGUE ").append(arena.wave()).append('/').append(arena.waveCount())
                .append("  TOUR ").append(arena.turnsTaken())
                .append("  PV ").append(arena.hero().health()).append('/').append(Hero.MAX_HEALTH);
        if (arena.lastCombo() > 1) {
            line.append("  ENCHAÎNEMENT ").append(arena.lastCombo());
        }
        return line.toString();
    }

    /**
     * La ligne la plus lue de l'écran : ce qui partira si le joueur appuie sur espace.
     *
     * <p>Elle est composée à partir du <b>préavis</b>, c'est-à-dire de l'objet même que l'exécution
     * consommera — jamais d'une relecture parallèle des règles. Voir {@link TilePreview}.
     */
    public static String announce(Arena arena) {
        if (arena.isOver()) {
            return arena.isVictory() ? "PARTIE GAGNÉE" : "PARTIE PERDUE";
        }
        TilePreview preview = arena.previewTop();
        if (preview == null) {
            return "FILE VIDE - POSEZ UNE TUILE (1-6 OU CLIC)";
        }
        return "SOMMET : " + preview.tile().label().toUpperCase() + " - " + effectOf(arena, preview);
    }

    /**
     * Ce que le préavis annonce, en un complément.
     *
     * <p>Un <em>complément</em> et non une phrase : il se lit derrière le nom de la tuile, qui porte
     * déjà le verbe. La première version répétait celui-ci et donnait « SOMMET : FRAPPE - FRAPPE
     * CASE 5 » — une planche-contact l'a montré avant qu'un joueur ne le lise.
     */
    public static String effectOf(Arena arena, TilePreview preview) {
        return switch (preview.kind()) {
            case HIT -> "SUR LA CASE " + cell(preview.aim());
            case PUSH -> preview.outcome() == ActionResult.PUSHED
                    ? "CASE " + cell(preview.aim()) + " VERS LA CASE " + cell(preview.landing())
                    : "CASE " + cell(preview.aim()) + " CONTRE " + obstacle(arena, preview.landing())
                            + " : CHOC";
            case MOVE -> preview.outcome() == ActionResult.DASHED
                    ? "JUSQU'EN CASE " + cell(preview.landing())
                    : "VERS LA CASE " + cell(preview.landing());
            case TURN -> "REGARD À " + preview.direction().label().toUpperCase();
            case NONE -> preview.outcome() == ActionResult.NO_TARGET
                    ? "CASE " + cell(preview.aim()) + " VIDE - TUILE PERDUE"
                    : "BLOQUÉ - TUILE PERDUE";
        };
    }

    /** Retour sur l'action qui vient d'être jouée. */
    public static String lastAction(ActionResult result) {
        // Une poussée qui butte et une poussée qui aboutit déplacent toutes les deux quelqu'un
        // d'une case : sans retour écrit, le joueur ne sait pas laquelle des deux il vient de jouer.
        return "DERNIÈRE ACTION : " + result.label().toUpperCase()
                + (result == ActionResult.COLLIDED ? " - ENNEMI ÉTOURDI" : "");
    }

    /**
     * Infobulle d'une tuile du râtelier : ce qu'elle fait, ce qu'elle coûte, et si on peut la poser.
     *
     * <p>La troisième ligne est celle qui manquait le plus : le râtelier montrait bien qu'une tuile
     * était éteinte, avec des points ocre pour compter, mais rien ne disait qu'un point valait un
     * tour à attendre.
     */
    public static List<String> rackTooltip(Arena arena, Tile tile) {
        int missing = arena.rack().missingPoints(tile);
        String state;
        if (missing > 0) {
            state = "EN RECHARGE : " + missing + (missing > 1 ? " POINTS" : " POINT") + " À ATTENDRE";
        } else if (!arena.rack().holds(tile)) {
            state = "DÉJÀ POSÉE SUR LA FILE";
        } else {
            state = "PRÊTE - RECHARGE " + tile.rechargeCost() + " APRÈS USAGE";
        }
        return List.of(head(tile), tile.effect().toUpperCase(), state);
    }

    /** Infobulle d'une tuile de la file : la même, plus sa place dans l'ordre d'exécution. */
    public static List<String> queueTooltip(List<Tile> queued, int index) {
        String state = index == queued.size() - 1
                ? "SOMMET : C'EST ELLE QUI PART LA PREMIÈRE"
                : "POSITION " + (index + 1) + " SUR " + queued.size() + " - CLIC POUR LA REPRENDRE";
        return List.of(head(queued.get(index)), queued.get(index).effect().toUpperCase(), state);
    }

    /** Première ligne d'une infobulle : le nom, la portée, et la gratuité si elle s'applique. */
    public static String head(Tile tile) {
        return tile.label().toUpperCase() + "   " + tile.reachLabel().toUpperCase()
                + (tile.isFreePlay() ? "   GRATUITE" : "");
    }

    /** Bannière de fin de partie. Ne vaut la peine d'être appelée que si la partie est finie. */
    public static List<String> outcome(Arena arena) {
        if (arena.isVictory()) {
            return List.of("VICTOIRE",
                    "LES " + arena.waveCount() + " VAGUES SONT TOMBÉES - RECORD " + arena.bestCombo());
        }
        return List.of("DÉFAITE", "LA VAGABONDE EST TOMBÉE AU TOUR " + arena.turnsTaken());
    }

    /**
     * L'aide : les commandes, en français.
     *
     * <p>Elle est ouverte au départ et se referme au premier geste. Un jeu au tour par tour dont
     * les commandes ne sont écrites nulle part n'est pas jouable ; un jeu qui les affiche en
     * permanence gaspille la moitié de son écran.
     */
    public static List<String> help() {
        return List.of(
                "AIDE - STARFALL",
                "FLÈCHES OU A/D : SE TOURNER, PUIS AVANCER",
                "1 À 6 : POSER UNE TUILE SUR LA FILE (GRATUIT)",
                "ESPACE : EXÉCUTER LA TUILE DU SOMMET",
                "RETOUR ARRIÈRE : REPRENDRE LA DERNIÈRE POSÉE",
                "E : ÉCHANGER DE PLACE AVEC LA CIBLE VISÉE",
                "CLIC : TOUT CELA À LA SOURIS - SURVOL : DÉTAILS",
                "F1 FERMER   F11 PLEIN ÉCRAN   ÉCHAP QUITTER");
    }

    /**
     * Rappel de la touche d'aide, dans le coin du bandeau.
     *
     * <p>Le même texte que l'aide soit ouverte ou non, et c'est un choix de largeur autant que de
     * clarté : « F1 FERMER » est plus long de deux caractères, ce qui suffisait à faire chevaucher
     * le bandeau et le rappel dans les parties longues — le test de largeur l'a attrapé avant que
     * ça n'arrive. La touche bascule ; le libellé nomme ce qu'elle ouvre.
     */
    public static final String HELP_HINT = "F1 AIDE";

    private static String cell(int index) {
        return String.valueOf(index + 1);
    }

    private static String obstacle(Arena arena, int landing) {
        return arena.grid().contains(landing) ? "CASE " + cell(landing) : "LE MUR";
    }
}
