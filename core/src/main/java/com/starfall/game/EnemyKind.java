package com.starfall.game;

/**
 * Les quatre archétypes d'ennemis.
 *
 * <p>Chacun se distingue par <b>la distance à laquelle il devient dangereux</b>, parce que c'est la
 * seule information qui compte sur une grille linéaire : savoir à quelle distance se tenir de qui.
 * Un archétype qui ne se distinguerait que par ses chiffres ne se lirait pas.
 *
 * <p>Le quatrième s'appelle {@code COLOSSE} et non « agressif » comme l'esquisse de cadrage le
 * suggérait : « agressif » est déjà un {@link Trait}, et deux choses différentes portant le même
 * nom finissent toujours par être confondues.
 */
public enum EnemyKind {

    /** Portée 1. Avance tant qu'il n'est pas au contact. La menace de base. */
    SABREUR("sabreur", "enemy/sabreur", 1, 1) {
        @Override
        boolean actsThisPhase(int phaseIndex) {
            return true;
        }
    },

    /**
     * Portée 3, et <b>recule</b> si on le colle.
     *
     * <p>Il inverse la question posée par le sabreur : contre lui, s'approcher est la bonne réponse,
     * et c'est ce qui rend une ligne mixte intéressante à lire.
     */
    ARCHER("archer", "enemy/archer", 3, 1) {
        @Override
        boolean actsThisPhase(int phaseIndex) {
            return true;
        }

        @Override
        boolean retreatsWhenAdjacent() {
            return true;
        }
    },

    /**
     * Prend son élan un tour, puis charge toute la ligne.
     *
     * <p>Le seul dont la menace est décalée d'un tour : il annonce, on a un tour pour s'écarter ou
     * pour le tuer.
     */
    LANCIER("lancier", "enemy/lancier", 1, 2) {
        @Override
        boolean actsThisPhase(int phaseIndex) {
            return true;
        }

        @Override
        boolean windsUp() {
            return true;
        }
    },

    /**
     * Portée 1, mais n'agit qu'une phase sur deux.
     *
     * <p>Sa lenteur est sa lisibilité : on peut le contourner, à condition de compter.
     */
    COLOSSE("colosse", "enemy/colosse", 1, 3) {
        @Override
        boolean actsThisPhase(int phaseIndex) {
            return phaseIndex % 2 == 0;
        }
    };

    private final String label;
    private final String spriteName;
    private final int range;
    private final int health;

    EnemyKind(String label, String spriteName, int range, int health) {
        this.label = label;
        this.spriteName = spriteName;
        this.range = range;
        this.health = health;
    }

    /** Libellé affichable, en français. */
    public String label() {
        return label;
    }

    public String spriteName() {
        return spriteName;
    }

    /** Distance, en cases, à laquelle l'ennemi frappe. */
    public int range() {
        return range;
    }

    /**
     * Points de vie de départ.
     *
     * <p>Ils suivent la lenteur : le colosse en a trois et n'agit qu'une phase sur deux, le sabreur
     * un seul et frappe à chaque phase. Ce qui est difficile à tuer doit être facile à éviter, sans
     * quoi il n'y a pas de décision à prendre.
     */
    public int health() {
        return health;
    }

    /** Vrai si l'archétype agit à cette phase. Le colosse n'agit qu'une fois sur deux. */
    abstract boolean actsThisPhase(int phaseIndex);

    /** Vrai si l'archétype prépare sa frappe un tour à l'avance. */
    boolean windsUp() {
        return false;
    }

    /** Vrai si l'archétype recule quand sa cible est au contact. */
    boolean retreatsWhenAdjacent() {
        return false;
    }
}
