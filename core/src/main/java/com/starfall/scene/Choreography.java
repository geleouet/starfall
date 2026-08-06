package com.starfall.scene;

import com.starfall.game.Arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * Ce qui bouge entre deux instantanés, et où ça en est à un instant donné.
 *
 * <h2>Pourquoi cette classe existe, et pourquoi elle ne dessine rien</h2>
 *
 * <p>Le déroulé montrait les temps d'une salve l'un après l'autre : lisible, mais saccadé — une
 * figure sautait d'une case à l'autre sans qu'on voie le trajet. Rendre le mouvement fluide demande
 * de savoir, à chaque image, <em>où en est</em> chaque figure entre son ancienne case et la
 * nouvelle. C'est ce que cette classe calcule.
 *
 * <p>Elle le calcule <b>sans rien savoir du dessin</b> : ni pixel, ni sprite, ni libGDX, ni horloge.
 * Elle rend des {@link Placement} exprimés en <em>fractions</em> — « à 60 % du chemin entre la case
 * 3 et la case 4 », « soulevé de 0,8 », « opaque à 30 % » — et c'est la scène qui décide ce que ces
 * fractions valent en pixels. La raison est écrite noir sur blanc parce qu'elle commande le reste :
 * <b>les visuels de ce jeu vont changer entièrement</b>. Quand ils changeront, ce qui sera réécrit
 * est la conversion en pixels, quelques dizaines de lignes dans la scène. La <em>dérivation</em> —
 * qui bouge, d'où, vers où, à quel moment du temps — ne bougera pas, et elle emporte avec elle tous
 * ses tests, qui n'ont jamais eu besoin d'un écran pour tourner.
 *
 * <p>C'est la même séparation que le modèle et la vue, appliquée un cran plus bas : la scène ne
 * <em>déduit</em> plus ce qui s'est passé au moment de dessiner, elle ne fait plus qu'<em>exprimer</em>
 * ce qu'on lui donne.
 *
 * <h2>La fluidité ne vient pas du sous-pixel</h2>
 *
 * <p>Ce jeu dessine à l'échelle entière et au pixel près ; une figure à mi-chemin ne sera jamais
 * peinte entre deux pixels. La fluidité vient d'ailleurs : une case fait plusieurs dizaines de
 * pixels de large, donc un glissement d'une case, c'est plusieurs dizaines de positions entières
 * successives. Assez pour que l'œil lise un mouvement continu, sans jamais trahir la grille de
 * pixels. Les fractions rendues ici sont continues ; c'est la scène qui les arrondit, une fois, au
 * dernier moment.
 */
public final class Choreography {

    /**
     * De combien de cases avance celui qui frappe, au plus fort de sa fente.
     *
     * <p>Un peu moins d'une demi-case : il ne se déplace pas, il se <b>fend</b>. Aller plus loin se
     * lirait comme un déplacement, et le joueur verrait le héros occuper une case qu'il n'occupe
     * pas — un mensonge de plus dans un jeu dont toute la tension tient à ce que l'annoncé et le
     * joué coïncident.
     *
     * <p>C'est une distance <b>fixe</b>, et non une fraction du chemin jusqu'à la cible. La première
     * version faisait la seconde, et l'image l'a démentie : un estoc porte à deux cases, donc une
     * fente valant 55 % du chemin déposait le héros au-delà de la case intermédiaire — celle-là
     * même que l'estoc <em>ne frappe pas</em>. La fente dit « maintenant, par là » ; c'est le cadre
     * posé sous la case visée qui dit jusqu'où, et lui seul est en droit de le dire.
     */
    public static final float LUNGE_CELLS = 0.4f;

    /** Recul de celui qui encaisse, en fraction de case. Un tressaillement, pas un déplacement. */
    public static final float RECOIL_DEPTH = 0.22f;

    /**
     * Le moment du contact, dans le temps normalisé d'un temps de salve.
     *
     * <p>Avant lui, la fente part ; après lui, la victime recule et le mort s'efface. Les deux
     * moitiés se rejoignent sur ce nombre, et c'est ce qui fait qu'on <em>voit</em> la cause avant
     * l'effet au lieu de voir les deux ensemble.
     */
    public static final float IMPACT = 0.4f;

    private Choreography() {
    }

    /**
     * Une figure à dessiner, et où elle en est.
     *
     * <p>Tout est sans unité, à dessein. {@code slide} dit quelle fraction du chemin de
     * {@code fromCell} vers {@code toCell} est parcourue — c'est la seule chose dont la scène ait
     * besoin pour interpoler entre deux abscisses qu'elle est seule à connaître. {@code lift} est un
     * soulèvement signé, positif vers le haut, dans un intervalle de l'ordre de l'unité.
     * {@code fade} est l'opacité, de 0 à 1.
     *
     * <p>Une figure au repos est un {@code Placement} comme un autre : mêmes cases de départ et
     * d'arrivée, {@code slide} nul, pleinement opaque. C'est ce qui permet à la scène de n'avoir
     * qu'<b>une seule voie de dessin</b> pour le plateau animé et le plateau au repos — deux voies
     * auraient divergé, et ce projet a payé neuf fois pour l'apprendre.
     */
    public record Placement(Arena.Figure figure, int fromCell, int toCell,
                            float slide, float lift, float fade) {
    }

    /**
     * Le plateau immobile, exprimé dans la même langue que le plateau animé.
     *
     * <p>Elle a l'air inutile — elle ne fait que traduire — et c'est précisément son objet : sans
     * elle, la scène aurait un chemin pour « ça bouge » et un autre pour « ça ne bouge pas ».
     */
    public static List<Placement> resting(List<Arena.Figure> board) {
        List<Placement> placements = new ArrayList<>(board.size());
        for (Arena.Figure figure : board) {
            placements.add(new Placement(figure, figure.cell(), figure.cell(), 0f, 0f, 1f));
        }
        return placements;
    }

    /**
     * Ce que devient chaque figure entre deux instantanés, à l'instant {@code t}.
     *
     * <p>{@code t} va de 0 — le plateau {@code before}, intact — à 1 — le plateau {@code after},
     * établi. {@code aim} est la case que la tuile visait, ou {@code -1} si elle n'en visait
     * aucune.
     *
     * <p>L'ordre du résultat suit celui de {@code after} puis de {@code before}, c'est-à-dire
     * l'ordre des cases. Il ne dépend jamais du parcours d'une table : les numéros de figure ne
     * servent qu'à se <em>reconnaître</em>, et rien ici ne doit dépendre de leur valeur.
     */
    public static List<Placement> at(List<Arena.Figure> before, List<Arena.Figure> after,
            int aim, float t) {
        return at(before, after, aim, t, Set.of());
    }

    /**
     * La m&ecirc;me chose, en sachant <b>qui a frapp&eacute;</b>.
     *
     * <p>La fente &eacute;tait r&eacute;serv&eacute;e au h&eacute;ros, parce qu'une tuile dit sa
     * case vis&eacute;e et que la riposte ennemie n'en d&eacute;signe aucune. Cons&eacute;quence :
     * pendant toute la phase ennemie, <b>aucun assaillant ne faisait le moindre geste</b>. Un
     * sabreur au contact s'en tirait &mdash; le tressaillement du h&eacute;ros arrive juste
     * &agrave; c&ocirc;t&eacute; de lui, et l'&oelig;il fait le lien. L'archer non : il tire de
     * trois cases et reste parfaitement immobile, si bien qu'on jurerait qu'il ne tire jamais.
     * Mesure : il annonce une attaque quatre fois sur cinq.
     *
     * <p>Les num&eacute;ros donn&eacute;s ici sont ceux qui ont touch&eacute;. Ils se fendent vers
     * le h&eacute;ros, du m&ecirc;me geste que lui &mdash; c'est la m&ecirc;me phrase visuelle, dite
     * dans l'autre sens.
     */
    public static List<Placement> at(List<Arena.Figure> before, List<Arena.Figure> after,
            int aim, float t, Set<Long> strikers) {
        float time = Math.min(1f, Math.max(0f, t));
        Map<Long, Arena.Figure> was = index(before);
        Map<Long, Arena.Figure> now = index(after);

        int heroCell = heroCell(after, before);
        // Se fendre, oui ; se fendre vers la case où l'on atterrit, non. Un élan vise sa propre
        // case d'arrivée : la figure y glisse déjà, et lui ajouter une fente ferait deux fois le
        // même mouvement.
        boolean lunges = aim >= 0 && aim != heroCell;

        List<Placement> placements = new ArrayList<>(after.size() + 2);
        for (Arena.Figure figure : after) {
            Arena.Figure previous = was.get(figure.id());
            if (previous == null) {
                placements.add(new Placement(figure, figure.cell(), figure.cell(),
                        0f, 0f, easeOut(time)));
            } else if (previous.cell() != figure.cell()) {
                placements.add(new Placement(figure, previous.cell(), figure.cell(),
                        easeOut(time), hop(time), 1f));
            } else if (figure.hero() && lunges) {
                placements.add(new Placement(figure, figure.cell(), aim,
                        lunge(time, Math.abs(aim - figure.cell())), 0f, 1f));
            } else if (strikers.contains(figure.id()) && heroCell != Integer.MIN_VALUE) {
                placements.add(new Placement(figure, figure.cell(),
                        figure.cell() + Integer.signum(heroCell - figure.cell()),
                        lunge(time, 1), 0f, 1f));
            } else if (figure.health() < previous.health()) {
                // Reculer demande de savoir DE QUOI l'on recule. Une tuile le dit - sa case visee
                // est la direction du coup. La riposte ennemie ne le dit pas : plusieurs ennemis
                // frappent, des deux cotes, et choisir un sens serait inventer. Le tressaillement
                // devient alors vertical, ce qui ne ment sur aucune direction.
                placements.add(aim >= 0
                        ? new Placement(figure, figure.cell(), away(figure.cell(), heroCell),
                                recoil(time), 0f, 1f)
                        : new Placement(figure, figure.cell(), figure.cell(),
                                0f, -recoil(time), 1f));
            } else {
                placements.add(new Placement(figure, figure.cell(), figure.cell(), 0f, 0f, 1f));
            }
        }
        for (Arena.Figure figure : before) {
            if (!now.containsKey(figure.id())) {
                // Celui qui tombe reste à l'écran jusqu'au contact, puis s'efface en s'affaissant.
                // Le retirer dès le premier instant du temps ferait disparaître la cible avant le
                // coup qui la tue.
                float remaining = fall(time);
                placements.add(new Placement(figure, figure.cell(), figure.cell(),
                        0f, -(1f - remaining), remaining));
            }
        }
        return placements;
    }

    /**
     * La case du héros, prise après l'action, ou avant s'il n'y est plus.
     *
     * <p>Elle sert de repère au recul de ceux qui encaissent : on recule en s'éloignant de ce qui
     * frappe. Quand aucun héros ne figure sur le plateau — un cas que seul un test peut construire
     * — le repère est hors grille, et le recul se fait alors toujours vers la droite plutôt que de
     * faire tomber le calcul.
     */
    private static int heroCell(List<Arena.Figure> after, List<Arena.Figure> before) {
        for (Arena.Figure figure : after) {
            if (figure.hero()) {
                return figure.cell();
            }
        }
        for (Arena.Figure figure : before) {
            if (figure.hero()) {
                return figure.cell();
            }
        }
        return Integer.MIN_VALUE;
    }

    /** La case voisine du côté opposé au héros. */
    private static int away(int cell, int heroCell) {
        return cell >= heroCell ? cell + 1 : cell - 1;
    }

    private static Map<Long, Arena.Figure> index(List<Arena.Figure> board) {
        Map<Long, Arena.Figure> byId = new HashMap<>();
        for (Arena.Figure figure : board) {
            byId.put(figure.id(), figure);
        }
        return byId;
    }

    /** Départ vif, arrivée posée. Le contraire donnerait un mouvement qui accélère vers sa fin. */
    private static float easeOut(float t) {
        float remaining = 1f - t;
        return 1f - remaining * remaining * remaining;
    }

    /** Un saut : nul aux deux bouts, maximal au milieu. */
    private static float hop(float t) {
        return (float) Math.sin(Math.PI * t);
    }

    /**
     * La fente : elle part, touche, revient.
     *
     * <p>Rendue en fraction du chemin vers la cible, puisque c'est la langue des {@link Placement},
     * mais <b>calculée</b> en cases : plus la cible est loin, plus la fraction est petite, de sorte
     * que la distance parcourue reste la même. Une case au minimum au dénominateur — une fente vers
     * sa propre case n'existe pas, et on ne divisera pas par zéro pour s'en assurer.
     */
    private static float lunge(float t, int distance) {
        float u = t < IMPACT ? t / IMPACT : (1f - t) / (1f - IMPACT);
        return LUNGE_CELLS / Math.max(1, distance) * easeOut(u);
    }

    /** Le tressaillement : rien avant le contact, un sursaut au contact, puis retour. */
    private static float recoil(float t) {
        if (t < IMPACT) {
            return 0f;
        }
        return RECOIL_DEPTH * (1f - easeOut((t - IMPACT) / (1f - IMPACT)));
    }

    /** Ce qu'il reste d'un mort : entier jusqu'au contact, puis rien. */
    private static float fall(float t) {
        if (t < IMPACT) {
            return 1f;
        }
        return 1f - easeOut((t - IMPACT) / (1f - IMPACT));
    }
}
