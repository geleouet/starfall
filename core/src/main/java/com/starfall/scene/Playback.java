package com.starfall.scene;

import com.starfall.game.Arena;

import java.util.ArrayList;
import java.util.List;

/**
 * Le <b>déroulé</b> d'une action : ses temps montrés l'un après l'autre.
 *
 * <h2>Pourquoi ceci existe</h2>
 *
 * <p>Cinq tuiles peuvent partir dans un seul tour. Le modèle les résout d'un bloc — c'est
 * délibéré, tout ce qui en dépend voit une résolution atomique — si bien que l'écran passait de
 * l'avant à l'après sans rien montrer de l'enchaînement. Le geste central du jeu, celui pour lequel
 * la file existe, était le seul qu'on ne pouvait pas lire.
 *
 * <p>Cette classe ne dessine rien et ne connaît ni libGDX ni le temps réel : elle dit seulement
 * <em>quel temps est à l'écran maintenant</em>. C'est ce qui la rend éprouvable sans écran, dans
 * {@code gradlew test}, là où vivent les garde-fous de ce projet — plutôt que dans le seul garde-fou
 * d'images, qui demande un écran et se lance à la main.
 *
 * <h2>Ce qui ne se déroule pas</h2>
 *
 * <p>Une action d'<b>un seul temps</b> ne se déroule pas. Un pas, un échange, une salve d'une seule
 * tuile : il n'y a rien à égrener, et faire attendre le joueur devant un unique temps ajouterait de
 * la latence sans ajouter de lecture. C'est la moitié de la règle qu'un test doit garder, parce
 * qu'une implémentation qui déroulerait tout serait invisible à l'œil et ruineuse au ressenti.
 */
public final class Playback {

    /**
     * Durée d'un temps, en secondes.
     *
     * <p>Assez long pour suivre l'œil, assez court pour qu'une salve de cinq ne fasse pas attendre
     * une seconde entière. Le déroulé bloque les entrées tant qu'il court : ce que le joueur voit
     * doit correspondre à ce sur quoi il agira.
     */
    public static final float BEAT_SECONDS = 0.18f;

    private List<Arena.Beat> beats = List.of();
    private List<Arena.Figure> opening = List.of();
    /** Rang du temps de riposte, ou {@code -1} : voir {@link #showsResponse()}. */
    private int response = -1;
    private int index;
    private float elapsed;

    /**
     * Démarre le déroulé d'une action, ou ne fait rien si elle ne compte qu'un temps.
     *
     * <p>Redémarrer alors qu'un déroulé court l'écrase : le joueur ne peut pas agir pendant, donc
     * le cas ne se produit pas en jeu — mais l'écrire ainsi évite qu'un appel de trop empile deux
     * déroulés dont l'un ne finirait jamais.
     */
    public void start(List<Arena.Beat> actionBeats, List<Arena.Figure> openingBoard,
            List<Arena.Figure> settledBoard) {
        List<Arena.Beat> kept = keepWhatMoves(actionBeats, openingBoard, settledBoard);
        if (kept.isEmpty()) {
            settle();
            return;
        }
        beats = kept;
        opening = List.copyOf(openingBoard);
        response = respondsAtEnd(actionBeats, settledBoard) ? kept.size() - 1 : -1;
        index = 0;
        elapsed = 0f;
    }

    /**
     * Vrai quand le temps &agrave; l'&eacute;cran est celui de la <b>riposte</b>.
     *
     * <p>Trois natures de temps se ressemblent trop pour qu'on les devine : une tuile qui joue, le
     * geste du joueur, et la r&eacute;ponse des ennemis. Les deux derni&egrave;res ne portent
     * aucune tuile, et le panneau les nommerait pareil. Or elles disent le contraire l'une de
     * l'autre &mdash; « voil&agrave; ce que tu viens de faire » et « voil&agrave; ce qu'on te
     * fait ».
     */
    public boolean showsResponse() {
        return isRunning() && index == response;
    }

    /** Vrai si un temps de riposte a &eacute;t&eacute; ajout&eacute; ET retenu. */
    private static boolean respondsAtEnd(List<Arena.Beat> actionBeats,
            List<Arena.Figure> settledBoard) {
        return !actionBeats.isEmpty()
                && moves(actionBeats.get(actionBeats.size() - 1).board(), settledBoard);
    }

    /**
     * Les temps qui valent d'être montrés, et le temps final qui manquait.
     *
     * <h2>Le temps final</h2>
     *
     * <p>Le modèle enregistre un temps par tuile, puis la <b>phase ennemie</b> se joue — c'est là
     * que le temps passe — et enfin une vague peut apparaître. Rien de tout cela n'était enregistré,
     * si bien que le déroulé s'achevait sur le plateau d'avant la riposte et que la scène, en se
     * reposant, basculait d'un coup sur l'état vrai : ennemis déplacés, points de vie du héros en
     * moins, vague neuve. <b>L'illisibilité que ce déroulé devait supprimer avait simplement été
     * repoussée à la fin.</b> Le plateau établi forme donc un dernier temps, et tout ce qu'aucune
     * tuile n'explique s'y montre comme le reste.
     *
     * <h2>Ce qui ne se déroule pas</h2>
     *
     * <p>La règle disait « une action d'un seul temps ne se déroule pas ». Elle a été écrite quand
     * un déroulé n'était qu'une suite d'images figées : avec un seul temps, il n'y avait rien à
     * égrener. Depuis que le mouvement est continu, cet argument ne couvre plus rien — un temps
     * unique a un trajet, et c'est justement le cas du <em>pas</em>, le geste le plus fréquent du
     * jeu.
     *
     * <p>La règle devient donc : <b>un temps où rien ne bouge ne prend pas de place</b>. Bouger
     * veut dire changer de case, de santé, ou apparaître et disparaître — pas se retourner, qui est
     * instantané et qu'aucune attente n'aiderait à lire. Un demi-tour sur place ne coûte donc
     * aucune latence, et un pas en gagne une qu'il n'avait pas.
     */
    private static List<Arena.Beat> keepWhatMoves(List<Arena.Beat> actionBeats,
            List<Arena.Figure> openingBoard, List<Arena.Figure> settledBoard) {
        List<Arena.Beat> all = new ArrayList<>(actionBeats);
        if (!actionBeats.isEmpty()) {
            Arena.Beat last = actionBeats.get(actionBeats.size() - 1);
            if (moves(last.board(), settledBoard)) {
                // Le temps final ne porte aucune tuile, et la file qu'il montre est celle du
                // dernier temps : la riposte ennemie ne touche pas a la file du joueur.
                all.add(new Arena.Beat(null, null, -1, List.copyOf(settledBoard), last.queued()));
            }
        }

        List<Arena.Beat> kept = new ArrayList<>(all.size());
        List<Arena.Figure> previous = openingBoard;
        for (Arena.Beat beat : all) {
            // Une tuile qui a joue garde son temps, MEME si le plateau n'en garde aucune trace.
            // La volte-face ne deplace personne, et la premiere version l'ecartait : la tuile
            // partait, le panneau ne la nommait jamais, et le joueur voyait sa salve compter deux
            // temps pour trois tuiles. Le panneau qui nomme un coup EST l'information, autant que
            // le mouvement qui l'accompagne quand il y en a un.
            if (beat.tile() != null || moves(previous, beat.board())) {
                kept.add(beat);
                previous = beat.board();
            }
        }
        return List.copyOf(kept);
    }

    /** Vrai si quelque chose a bougé, au sens de ce qui vaut la peine d'être regardé bouger. */
    private static boolean moves(List<Arena.Figure> before, List<Arena.Figure> after) {
        if (before.size() != after.size()) {
            return true;
        }
        for (int i = 0; i < before.size(); i++) {
            Arena.Figure was = before.get(i);
            Arena.Figure now = after.get(i);
            if (was.id() != now.id() || was.cell() != now.cell()
                    || was.health() != now.health()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fait avancer le déroulé.
     *
     * <p>Il n'avance que par ce qu'on lui donne : aucune horloge, aucun appel à {@code Gdx}. C'est
     * ce qui permet de l'éprouver, et c'est aussi ce qui garantit qu'il <b>finit</b> — la contrainte
     * posée au tableau de bord, puisque le garde-fou d'images capture des états au repos.
     */
    public void advance(float seconds) {
        if (!isRunning()) {
            return;
        }
        elapsed += seconds;
        while (elapsed >= BEAT_SECONDS && isRunning()) {
            elapsed -= BEAT_SECONDS;
            index++;
        }
        if (!isRunning()) {
            settle();
        }
    }

    /** Vrai tant qu'un temps reste à montrer. */
    public boolean isRunning() {
        return index < beats.size();
    }

    /** Le temps à l'écran, ou {@code null} si rien ne se déroule. */
    public Arena.Beat current() {
        return isRunning() ? beats.get(index) : null;
    }

    /**
     * Où en est le temps à l'écran, de 0 — il commence — à 1 — il est joué.
     *
     * <p>C'est ce qui manquait pour que le mouvement soit continu : sans lui, la scène ne pouvait
     * que montrer le plateau du temps <em>n</em> puis celui du temps <em>n+1</em>, et les figures
     * sautaient d'une case à l'autre.
     */
    public float progress() {
        return isRunning() ? Math.min(1f, elapsed / BEAT_SECONDS) : 1f;
    }

    /**
     * Où en est chaque figure, maintenant.
     *
     * <p>Point de passage <b>unique</b> vers le dessin des figures : au repos comme en plein
     * déroulé, la scène lit cette liste et rien d'autre. C'est la même exigence que l'instantané
     * unique dont ce déroulé est né, poussée d'un cran — une règle écrite à deux endroits finit par
     * diverger, et « où se trouve une figure » est exactement le genre de règle qui coûte cher à
     * écrire deux fois.
     *
     * @param resting le plateau vivant, montré quand rien ne se déroule
     */
    public List<Choreography.Placement> placements(List<Arena.Figure> resting) {
        if (!isRunning()) {
            return Choreography.resting(resting);
        }
        Arena.Beat beat = beats.get(index);
        List<Arena.Figure> before = index == 0 ? opening : beats.get(index - 1).board();
        return Choreography.at(before, beat.board(), beat.cell(), progress());
    }

    /**
     * Se place sur un temps donné, à une fraction donnée de son déroulement.
     *
     * <p>Existe pour la <b>capture</b>, qui doit pouvoir montrer un mouvement à mi-course sans
     * dépendre d'une horloge. Y arriver par {@code advance} serait possible mais piégeux : avancer
     * d'exactement une durée de temps bascule sur le temps suivant, si bien qu'on ne peut pas
     * demander « la fin du temps 2 » par accumulation. Ici la position est posée, pas atteinte.
     *
     * @param beat     rang du temps, à partir de 1
     * @param fraction où en est ce temps, de 0 à 1
     */
    public void seek(int beat, float fraction) {
        if (beats.isEmpty()) {
            return;
        }
        index = Math.max(0, Math.min(beats.size() - 1, beat - 1));
        elapsed = Math.max(0f, Math.min(1f, fraction)) * BEAT_SECONDS;
    }

    /** Rang du temps à l'écran, à partir de 1, ou 0 si rien ne se déroule. */
    public int step() {
        return isRunning() ? index + 1 : 0;
    }

    /** Nombre de temps du déroulé en cours, ou 0. */
    public int total() {
        return isRunning() ? beats.size() : 0;
    }

    /** Termine le déroulé sur-le-champ. */
    public void settle() {
        beats = List.of();
        opening = List.of();
        response = -1;
        index = 0;
        elapsed = 0f;
    }
}
