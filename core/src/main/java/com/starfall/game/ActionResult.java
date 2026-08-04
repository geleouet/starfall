package com.starfall.game;

/**
 * Ce qui s'est réellement passé quand le joueur a agi.
 *
 * <p>Une action qui échoue doit se distinguer d'une action qui réussit : le joueur doit comprendre
 * pourquoi rien n'a bougé, et plus tard la file d'actions (M5) devra savoir si un tour a été
 * consommé. Renvoyer un booléen aurait suffi à faire marcher le jeu, pas à l'expliquer.
 */
public enum ActionResult {

    /** Le héros a avancé d'une case. */
    MOVED("déplacement"),
    /** Le héros s'est retourné sans changer de case. */
    TURNED("demi-tour"),
    /** Rien n'a bougé : un bord de grille ou un occupant bloquait. */
    BLOCKED("bloqué"),
    /** Le héros a échangé sa place avec sa cible. */
    SWAPPED("échange de place"),
    /** Capacité déclenchée dans le vide : personne devant le héros. */
    NO_TARGET("aucune cible"),

    // ------------------------------------------------------------------ tuiles

    /** Une tuile a frappé sa cible. */
    STRUCK("frappe"),
    /** Une tuile a repoussé sa cible d'une case. */
    PUSHED("poussée"),
    /** La poussée a butté : les deux encaissent, et le poussé est étourdi. */
    COLLIDED("collision"),
    /** Plusieurs ennemis sont tombés d'un seul geste. */
    COMBO("combo"),
    /** La dernière vague est tombée. */
    VICTORY("victoire"),
    /** Le héros est tombé. */
    DEFEAT("défaite"),
    /** Le héros a chargé jusqu'à être arrêté. */
    DASHED("élan"),

    // ------------------------------------------------------------------ file d'actions

    /** Une tuile a été posée sur la file. Gratuit : aucun tour consommé. */
    QUEUED("tuile posée"),
    /** Une tuile a été reprise de la file. Gratuit aussi. */
    UNQUEUED("tuile reprise"),
    /** La file porte déjà ses cinq tuiles. */
    QUEUE_FULL("file pleine"),
    /** La tuile n'a pas fini de se recharger. */
    NOT_READY("tuile en recharge"),
    /** Exécution demandée sur une file vide. */
    EMPTY_QUEUE("file vide");

    private final String label;

    ActionResult(String label) {
        this.label = label;
    }

    /** Libellé affichable, en français. */
    public String label() {
        return label;
    }
}
