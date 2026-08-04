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
    NO_TARGET("aucune cible");

    private final String label;

    ActionResult(String label) {
        this.label = label;
    }

    /** Libellé affichable, en français. */
    public String label() {
        return label;
    }

    /** Vrai si l'état du jeu a changé. */
    public boolean changedState() {
        return this == MOVED || this == TURNED || this == SWAPPED;
    }
}
