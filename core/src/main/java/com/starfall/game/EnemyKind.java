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
    SABREUR("sabreur", "enemy/sabreur", 1, 1, 2) {
        },

    /**
     * Portée 3, et <b>recule</b> si on le colle.
     *
     * <p>Il inverse la question posée par le sabreur : contre lui, s'approcher est la bonne réponse,
     * et c'est ce qui rend une ligne mixte intéressante à lire.
     */
    ARCHER("archer", "enemy/archer", 3, 1, 1) {
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
    LANCIER("lancier", "enemy/lancier", 1, 2, 3) {
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
    COLOSSE("colosse", "enemy/colosse", 1, 1, 3) {
        @Override
        int planSize() {
            return 2;
        }
    },

    /**
     * Le souverain : la rencontre finale de la tranche.
     *
     * <p>Il ne se distingue pas des autres par un chiffre plus gros mais par <b>la façon dont la
     * distance décide de ce qu'il fait</b> — c'est la même grammaire que les quatre archétypes, en
     * trois réponses au lieu d'une :
     *
     * <ul>
     *   <li><b>au contact</b>, il frappe, une fois, pour un point. C'est sa forme la moins
     *       dangereuse ;</li>
     *   <li><b>à deux cases</b>, il fait sa {@link Intention.Kind#RUSH ruée} : il vient au contact,
     *       frappe, et <em>repousse</em>. Le coup vaut un point comme les autres ; ce qui coûte,
     *       c'est de se retrouver déplacé sans l'avoir choisi ;</li>
     *   <li><b>au loin</b>, il {@link Intention.Kind#SUMMON invoque} — et il invoque
     *       <em>derrière</em> le héros.</li>
     * </ul>
     *
     * <p>Le renversement est là : contre les quatre autres, la bonne réponse est presque toujours
     * de garder ses distances. Contre lui, s'éloigner est ce qui remplit le plateau. Et rester
     * collé, c'est accepter d'être pris en tenaille par ce qu'on a laissé apparaître.
     */
    SOUVERAIN("souverain", "enemy/souverain", 1, 2, 5) {
        @Override
        int summons() {
            return 1;
        }
    };

    private final String label;
    private final String spriteName;
    private final int range;
    private final int damage;
    private final int health;

    EnemyKind(String label, String spriteName, int range, int damage, int health) {
        this.label = label;
        this.spriteName = spriteName;
        this.range = range;
        this.damage = damage;
        this.health = health;
    }

    /**
     * Points de vie qu'un de ses coups retire.
     *
     * <p>L'axe existait c&ocirc;t&eacute; joueur &mdash; frappe &agrave; 1, estoc &agrave; 3 &mdash;
     * et pas c&ocirc;t&eacute; adversaire : tout coup ennemi co&ucirc;tait exactement un point, si
     * bien que la question qui se pose devant une ligne mixte, <b>lequel fait le plus mal</b>,
     * n'avait pas de r&eacute;ponse.
     *
     * <p>La force suit la <b>lenteur</b>, comme les points de vie. Le sabreur et l'archer frappent
     * &agrave; chaque phase et retirent un point ; le lancier annonce sa charge un tour &agrave;
     * l'avance, le colosse ne joue qu'une phase sur deux, le souverain se paie de son invocation
     * &mdash; les trois en retirent deux. Ce qui frappe fort laisse le temps de s'&eacute;carter,
     * et c'est la m&ecirc;me &eacute;conomie que la file du joueur, o&ugrave; l'estoc co&ucirc;te
     * plus de recharge que la frappe.
     *
     * <p>Le trait <b>rapide</b> multiplie les coups et non leur force : un rapide porte deux fois
     * la m&ecirc;me frappe. Les deux axes se composent sans se confondre.
     */
    public int damage() {
        return damage;
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

    

    /**
     * Combien d'actions cet archétype accumule avant de les lâcher <b>toutes ensemble</b>.
     *
     * <h2>Ce que la file change</h2>
     *
     * <p>Un ennemi à file de un annonce puis frappe, tour après tour : c'est tout le monde sauf le
     * colosse, et c'était tout le monde sans exception jusqu'ici. Le colosse en tient <b>deux</b>.
     * Il ne frappe donc qu'un tour sur deux — exactement la cadence qu'il avait — mais les deux
     * tours se lisent au lieu d'un seul : on le voit <em>remplir</em>, puis <em>lâcher</em>, là où
     * il alternait entre une menace et un tour de silence que rien n'expliquait.
     *
     * <p>Sa force par coup passe de deux à un : deux coups d'un point valent les deux points qu'il
     * retirait d'un seul, et la cadence est inchangée. <b>Le total ne bouge pas ; la lecture,
     * si.</b> Et une nuance de jeu apparaît sans qu'on l'ait cherchée : ses deux coups visent les
     * cases annoncées <em>au moment où il les a mises en file</em>, donc le plus ancien porte
     * souvent à côté. S'écarter tôt en esquive un ; s'écarter tard les esquive tous les deux.
     */
    int planSize() {
        return 1;
    }

    /** Vrai si l'archétype prépare sa frappe un tour à l'avance. */
    boolean windsUp() {
        return false;
    }

    /** Vrai si l'archétype recule quand sa cible est au contact. */
    boolean retreatsWhenAdjacent() {
        return false;
    }

    /**
     * Nombre d'ennemis que l'archétype peut faire apparaître au cours de sa vie.
     *
     * <p>Fini, et volontairement petit. Une invocation sans plafond ne pose aucune question au
     * joueur : elle transforme une rencontre en course contre la montre, où la seule réponse est de
     * tuer vite — c'est-à-dire exactement le contraire d'un jeu de placement. Chaque invocation
     * qu'on refuse est une invocation qui ne reviendra pas.
     *
     * <p>Le nombre est passé de deux à un, et ce n'est pas un ajustement d'humeur : la correction
     * du placement des invocations a <b>changé ce que ce nombre voulait dire</b>. Tant qu'une
     * invocation sur deux tombait derrière le souverain, où le sbire naissait coincé et inerte, en
     * accorder deux revenait à en accorder une. Maintenant que chacune se lève au contact du héros,
     * deux, c'est deux. Un paramètre dont la signification bouge doit être redérivé, pas conservé
     * par habitude — le réaccordage complet reste l'affaire du jalon d'équilibrage.
     */
    int summons() {
        return 0;
    }
}
