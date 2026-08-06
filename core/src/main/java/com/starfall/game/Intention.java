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
        /**
         * Charger, frapper, <b>et repousser</b> : la combinaison du souverain.
         *
         * <p>Elle vaut d'être une intention à part et non un trait posé sur la charge : ce qu'elle
         * ajoute n'est pas de la puissance mais un <em>déplacement subi</em>, et dans un jeu de
         * placement c'est une catégorie de menace différente. Le joueur doit pouvoir la lire comme
         * telle, donc elle a son propre glyphe.
         *
         * <p>La poussée ne blesse pas, même contre un mur. C'est délibéré : le compte des coups
         * annoncés doit rester exactement le compte des coups reçus, et un dégât conditionnel à la
         * géométrie ne peut pas être annoncé un tour à l'avance sans risquer de sur-promettre.
         */
        RUSH("ruée"),
        /**
         * Faire apparaître un ennemi sur une case annoncée.
         *
         * <p>Elle ne <b>menace</b> pas au sens du compteur de coups : rien ne tombe sur cette case,
         * et la mêler aux frappes fausserait le nombre que le bandeau affiche. Elle se marque donc
         * au sol d'une forme à elle.
         */
        SUMMON("invoque"),
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

        /**
         * Vrai si l'intention fait tomber un coup sur une case.
         *
         * <p>C'est ce qui alimente le compteur de coups du bandeau, donc la liste doit contenir
         * <b>exactement</b> ce qui blesse : une invocation se marque au sol elle aussi, mais ne
         * frappe personne, et l'y ajouter ferait mentir un nombre que le joueur lit pour décider
         * s'il reste.
         */
        public boolean threatens() {
            return this == ATTACK || this == CHARGE || this == RUSH;
        }
    }

    /**
     * Vrai si cette nature d'intention doit &ecirc;tre encadr&eacute;e d'une <b>plaque</b>.
     *
     * <h2>Pourquoi la r&egrave;gle vit ici</h2>
     *
     * <p>Elle vivait dans la sc&egrave;ne, sous la forme de deux bool&eacute;ens lus l'un
     * apr&egrave;s l'autre, et elle y &eacute;tait <b>incompl&egrave;te</b> : la plaque n'&eacute;tait
     * dessin&eacute;e que pour ce qui frappe ou ce qui se charge, tandis que la <em>pile</em>
     * &mdash; la carte de ce qui suit &mdash; l'&eacute;tait d&egrave;s qu'un ennemi tenait plus
     * d'une action. Un colosse qui met &laquo; avancer, puis frapper &raquo; n'avait donc aucune
     * plaque de t&ecirc;te et une carte flottant seule au-dessus de sa t&ecirc;te, rattach&eacute;e
     * &agrave; rien.
     *
     * <p>Ce cas n'existait pas la veille : c'est la projection de la position, pos&eacute;e au
     * cycle pr&eacute;c&eacute;dent, qui l'a rendu atteignable &mdash; mesur&eacute; &agrave;
     * quatre-vingt-quatorze occurrences sur onze cents l&acirc;chers. Une correction qui ouvre un
     * cas de dessin que le dessin ne pr&eacute;voyait pas.
     *
     * <p>La r&egrave;gle est donc devenue une seule question, pos&eacute;e ici o&ugrave; elle
     * s'&eacute;prouve sans &eacute;cran : <b>faut-il une plaque ?</b> Oui si un coup part, oui si
     * quelque chose se pr&eacute;pare, et oui si une file en tient plus d'une &mdash; parce que la
     * plaque est ce qui dit &laquo; ceci est une file &raquo;, et qu'une file dont la t&ecirc;te
     * manque ne se lit pas.
     */
    public static boolean needsPlaque(Kind head, int planSize) {
        return head.threatens() || head == Kind.WIND_UP || head == Kind.SUMMON || planSize > 1;
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

    public static Intention rush(int targetCell) {
        return new Intention(Kind.RUSH, targetCell);
    }

    /** @param cell case où l'ennemi invoqué apparaîtra */
    public static Intention summon(int cell) {
        return new Intention(Kind.SUMMON, cell);
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
