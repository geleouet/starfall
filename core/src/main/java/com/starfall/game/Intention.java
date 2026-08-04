package com.starfall.game;

/**
 * Ce qu'un ennemi <b>a annoncé</b> qu'il ferait à sa prochaine activation.
 *
 * <p>C'est la pièce maîtresse du jeu. Une intention est calculée à la fin d'une phase ennemie,
 * montrée au joueur pendant tout le temps où il réfléchit, puis exécutée <b>telle quelle</b> à la
 * phase suivante — même si le joueur s'est déplacé entre-temps.
 *
 * <p>Cette dernière clause n'est pas un détail d'implémentation, c'est la règle du jeu : on esquive
 * une attaque en quittant la case annoncée. Si l'ennemi recalculait sa cible au moment de frapper,
 * le télégraphe serait un mensonge et il n'y aurait plus rien à lire.
 */
public record Intention(Kind kind, int targetCell) {

    /** Nature de l'intention. */
    public enum Kind {
        /** Avancer d'une case vers la cible. */
        ADVANCE("avance"),
        /** Frapper une case précise. */
        ATTACK("frappe"),
        /** Prendre son élan : rien ce tour-ci, mais une charge au suivant. */
        WIND_UP("prend son élan"),
        /** Charger en ligne droite et frapper ce qui se trouve au bout. */
        CHARGE("charge"),
        /** Ne rien faire. */
        WAIT("attend");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        /** Libellé affichable, en français. */
        public String label() {
            return label;
        }

        /** Vrai si l'intention menace une case : c'est ce qui se marque au sol. */
        public boolean threatens() {
            return this == ATTACK || this == CHARGE;
        }
    }

    /** Intention qui ne vise aucune case. */
    public static Intention of(Kind kind) {
        return new Intention(kind, -1);
    }

    public static Intention advance(int targetCell) {
        return new Intention(Kind.ADVANCE, targetCell);
    }

    public static Intention attack(int targetCell) {
        return new Intention(Kind.ATTACK, targetCell);
    }

    public static Intention charge(int targetCell) {
        return new Intention(Kind.CHARGE, targetCell);
    }

    /** Vrai si cette intention menace la case donnée. */
    public boolean threatens(int cell) {
        return kind.threatens() && targetCell == cell;
    }

    @Override
    public String toString() {
        return kind.label() + (targetCell >= 0 ? " (case " + (targetCell + 1) + ")" : "");
    }
}
