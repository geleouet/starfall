package com.starfall.game;

/**
 * Une tuile d'action : ce que le héros sait faire.
 *
 * <p>Une tuile n'est jamais jouée directement. On la <b>pose sur la file</b> — gratuitement — puis
 * on l'exécute. C'est ce décalage qui fait tout le jeu : poser trois tuiles coûte trois tours de
 * vulnérabilité, mais les exécuter ensuite enchaîne trois effets pendant que les ennemis n'ont
 * avancé que d'un cran.
 *
 * <h2>Recharge</h2>
 *
 * <p>Une tuile exécutée part en recharge et réclame {@link #rechargeCost()} points avant de pouvoir
 * être reposée. Chaque tour consommé donne un point à toutes les tuiles en recharge.
 *
 * <h2>Free-Play</h2>
 *
 * <p>Une tuile {@link #isFreePlay() Free-Play} s'exécute <b>sans consommer de tour</b> : les ennemis
 * n'avancent pas et les recharges ne progressent pas. C'est la soupape du système — sans elle, se
 * repositionner coûterait toujours un tour, et la file ne serait qu'une punition.
 *
 * <h2>Décider, puis agir</h2>
 *
 * <p>Chaque tuile est écrite en deux temps : {@link #preview(Arena)} <b>décide</b> ce qui va se
 * passer, {@link #execute} se contente de le <b>faire</b>. L'affichage lit le même préavis que
 * l'exécution, si bien que la portée montrée au joueur ne peut pas s'écarter de l'effet joué —
 * voir {@link TilePreview}.
 *
 * <p>C'est aussi ce qui a fait disparaître le doublon entre la frappe et l'estoc : elles ne
 * diffèrent plus que par un nombre.
 *
 * <h2>Portée des effets à ce jalon</h2>
 *
 * <p>Les tuiles n'infligent qu'un point de dégât. La Vagabonde ne gagne pas en frappant fort, elle
 * gagne en plaçant : une poussée contre un mur fait autant de mal qu'une frappe, et étourdit en
 * plus.
 */
public enum Tile {

    /** Frappe la case juste devant. Portée 1. */
    STRIKE("frappe", "tile/slash", 2, false,
            new int[]{1}, "frappe la case juste devant") {
        @Override
        TilePreview preview(Arena arena) {
            return TilePreview.aimed(this, arena, 1);
        }

        @Override
        ActionResult execute(Arena arena, TilePreview preview) {
            return strike(arena, preview);
        }
    },

    /**
     * Frappe la case située <b>deux</b> cases devant, en passant par-dessus la première.
     *
     * <p>Sa raison d'être : atteindre le second rang sans se déplacer. Une portée plus longue pour
     * une recharge plus longue.
     */
    THRUST("estoc", "tile/thrust", 3, false,
            new int[]{2}, "frappe la 2e case, par-dessus la 1re") {
        @Override
        TilePreview preview(Arena arena) {
            return TilePreview.aimed(this, arena, 2);
        }

        @Override
        ActionResult execute(Arena arena, TilePreview preview) {
            return strike(arena, preview);
        }
    },

    /** Repousse d'une case l'occupant juste devant. Aucun dégât : c'est du placement. */
    PUSH("poussée", "tile/push", 2, false,
            new int[]{1}, "repousse d'une case ; le mur blesse") {
        @Override
        TilePreview preview(Arena arena) {
            Direction facing = arena.hero().facing();
            return arena.planShove(this, arena.heroCell() + facing.step(), facing);
        }

        @Override
        ActionResult execute(Arena arena, TilePreview preview) {
            return arena.applyShove(preview);
        }
    },

    /** Le héros charge droit devant jusqu'à être arrêté par un bord ou un occupant. */
    DASH("élan", "tile/dash", 3, false,
            "portée variable", "court droit devant jusqu'à l'obstacle") {
        @Override
        TilePreview preview(Arena arena) {
            Direction facing = arena.hero().facing();
            int landing = arena.heroCell();
            while (arena.grid().isFree(landing + facing.step())) {
                landing += facing.step();
            }
            return landing == arena.heroCell()
                    ? TilePreview.inert(this, ActionResult.BLOCKED, facing, -1)
                    : TilePreview.move(this, ActionResult.DASHED, facing, landing);
        }

        @Override
        ActionResult execute(Arena arena, TilePreview preview) {
            return travel(arena, preview);
        }
    },

    /**
     * Recul d'une case, sans se retourner — et <b>sans consommer de tour</b>.
     *
     * <p>Reculer en gardant sa cible en vue est exactement ce qu'une tuile Free-Play doit permettre
     * : corriger un placement sans offrir un tour aux ennemis.
     */
    SIDESTEP("pas de côté", "tile/step", 3, true,
            new int[]{-1}, "recule d'une case sans se retourner") {
        @Override
        TilePreview preview(Arena arena) {
            Direction back = arena.hero().facing().opposite();
            int landing = arena.heroCell() + back.step();
            return arena.grid().isFree(landing)
                    ? TilePreview.move(this, ActionResult.MOVED, back, landing)
                    : TilePreview.inert(this, ActionResult.BLOCKED, back, landing);
        }

        @Override
        ActionResult execute(Arena arena, TilePreview preview) {
            return travel(arena, preview);
        }
    },

    /**
     * Volte-face : se retourner <b>sans consommer de tour</b>.
     *
     * <p>C'est la tuile qui donne tout son sel à la règle d'orientation du projet. Puisque se
     * retourner coûte normalement un tour, une tuile qui l'offre gratuitement vaut cher — et le
     * choix de la garder en réserve ou de la dépenser pour rattraper un placement est exactement le
     * genre de décision que la file doit produire.
     */
    PIVOT("volte-face", "tile/pivot", 2, true,
            "sur place", "se retourne sans avancer") {
        @Override
        TilePreview preview(Arena arena) {
            return new TilePreview(this, ActionResult.TURNED, TilePreview.Kind.TURN,
                    arena.hero().facing().opposite(), -1, -1);
        }

        @Override
        ActionResult execute(Arena arena, TilePreview preview) {
            arena.hero().face(preview.direction());
            return ActionResult.TURNED;
        }
    };

    /** Dégâts d'une frappe. Un point : la Vagabonde ne frappe pas fort, elle se replace. */
    public static final int DAMAGE = 1;

    private final String label;
    private final String spriteName;
    private final int rechargeCost;
    private final boolean freePlay;
    private final int[] reach;
    private final String reachLabel;
    private final String effect;

    /**
     * Tuile dont la portée est un ensemble fixe de décalages. Son libellé est <b>calculé</b> à
     * partir de ces décalages, et pas recopié à côté d'eux : c'est ce qui rend impossible qu'une
     * infobulle annonce « portée 2 » pour une tuile qui vise la case d'à côté. Le test de préavis
     * ferme la boucle en vérifiant que les décalages sont bien ceux que la tuile vise.
     */
    Tile(String label, String spriteName, int rechargeCost, boolean freePlay,
         int[] reach, String effect) {
        this(label, spriteName, rechargeCost, freePlay, reach, describe(reach), effect);
    }

    /** Tuile dont la portée ne se réduit pas à des décalages : elle dépend du terrain, ou n'existe pas. */
    Tile(String label, String spriteName, int rechargeCost, boolean freePlay,
         String reachLabel, String effect) {
        this(label, spriteName, rechargeCost, freePlay, new int[]{}, reachLabel, effect);
    }

    private Tile(String label, String spriteName, int rechargeCost, boolean freePlay,
                 int[] reach, String reachLabel, String effect) {
        this.label = label;
        this.spriteName = spriteName;
        this.rechargeCost = rechargeCost;
        this.freePlay = freePlay;
        this.reach = reach;
        this.reachLabel = reachLabel;
        this.effect = effect;
    }

    private static String describe(int[] reach) {
        int offset = reach[0];
        return offset > 0 ? "portée " + offset : "recul " + (-offset);
    }

    /**
     * Ce que la tuile ferait si elle était exécutée maintenant.
     *
     * <p>Sans effet de bord, par construction : c'est ce qui permet à l'affichage de l'appeler à
     * chaque image sans faire avancer la partie.
     */
    abstract TilePreview preview(Arena arena);

    /** Réalise un préavis déjà calculé. Ne décide de rien. */
    abstract ActionResult execute(Arena arena, TilePreview preview);

    /**
     * Applique l'effet de la tuile. Ne touche ni à la file, ni aux recharges, ni aux tours.
     *
     * <p><b>Final, et c'est le point</b> : toute tuile passe par son propre préavis. Il n'existe pas
     * de chemin d'exécution qui contourne ce que l'interface a annoncé au joueur.
     */
    final ActionResult applyTo(Arena arena) {
        return execute(arena, preview(arena));
    }

    /** Frappe partagée par la frappe et l'estoc : elles ne diffèrent que par leur portée. */
    private static ActionResult strike(Arena arena, TilePreview preview) {
        if (!preview.connects()) {
            return preview.outcome();
        }
        arena.damage(preview.aim(), DAMAGE);
        return ActionResult.STRUCK;
    }

    /** Déplacement du héros partagé par l'élan et le pas de côté. */
    private static ActionResult travel(Arena arena, TilePreview preview) {
        if (!preview.connects()) {
            return preview.outcome();
        }
        arena.grid().move(arena.heroCell(), preview.landing());
        return preview.outcome();
    }

    /** Nom affichable, en français. */
    public String label() {
        return label;
    }

    public String spriteName() {
        return spriteName;
    }

    /** Points nécessaires pour revenir de recharge après une exécution. */
    public int rechargeCost() {
        return rechargeCost;
    }

    /** Vrai si l'exécuter ne consomme pas de tour. */
    public boolean isFreePlay() {
        return freePlay;
    }

    /**
     * Cases que la tuile peut toucher, en décalages relatifs au héros, comptés <b>vers l'avant</b> :
     * {@code 1} désigne la case devant lui, {@code -1} celle derrière.
     *
     * <p>C'est une portée <em>statique</em> : elle ne sait pas qui se tient où. Elle sert au survol
     * du râtelier, et uniquement à cela — une tuile du râtelier ne s'exécute pas tout de suite, elle
     * passe d'abord par la file, donc lui prêter un résultat serait recommencer le mensonge que le
     * télégraphe a coûté deux jalons à éteindre. Voir {@link TilePreview} pour la portée
     * <em>résolue</em>, celle du sommet de la file, qui elle a le droit d'annoncer un résultat.
     *
     * <p>Vide quand la portée dépend du terrain ({@link #DASH}) ou que la tuile ne vise rien
     * ({@link #PIVOT}).
     */
    public int[] reach() {
        return reach.clone();
    }

    /** Portée en toutes lettres, pour l'infobulle. */
    public String reachLabel() {
        return reachLabel;
    }

    /** Effet en une ligne, en français, pour l'infobulle. */
    public String effect() {
        return effect;
    }
}
