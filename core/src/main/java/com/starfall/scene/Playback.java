package com.starfall.scene;

import com.starfall.game.Arena;

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
    private int index;
    private float elapsed;

    /**
     * Démarre le déroulé d'une action, ou ne fait rien si elle ne compte qu'un temps.
     *
     * <p>Redémarrer alors qu'un déroulé court l'écrase : le joueur ne peut pas agir pendant, donc
     * le cas ne se produit pas en jeu — mais l'écrire ainsi évite qu'un appel de trop empile deux
     * déroulés dont l'un ne finirait jamais.
     */
    public void start(List<Arena.Beat> actionBeats) {
        if (actionBeats.size() < 2) {
            settle();
            return;
        }
        beats = List.copyOf(actionBeats);
        index = 0;
        elapsed = 0f;
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
        index = 0;
        elapsed = 0f;
    }
}
