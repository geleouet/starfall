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
 * <h2>Portée des effets à ce jalon</h2>
 *
 * <p>Les tuiles n'infligent qu'un point de dégât. La Vagabonde ne gagne pas en frappant fort, elle
 * gagne en plaçant : une poussée contre un mur fait autant de mal qu'une frappe, et étourdit en
 * plus.
 */
public enum Tile {

    /** Frappe la case juste devant. Portée 1. */
    STRIKE("frappe", "tile/slash", 2, false) {
        @Override
        ActionResult applyTo(Arena arena) {
            int target = arena.heroCell() + arena.hero().facing().step();
            if (arena.grid().occupantAt(target) == null) {
                return ActionResult.NO_TARGET;
            }
            arena.damage(target, DAMAGE);
            return ActionResult.STRUCK;
        }
    },

    /**
     * Frappe la case située <b>deux</b> cases devant, en passant par-dessus la première.
     *
     * <p>Sa raison d'être : atteindre le second rang sans se déplacer. Une portée plus longue pour
     * une recharge plus longue.
     */
    THRUST("estoc", "tile/thrust", 3, false) {
        @Override
        ActionResult applyTo(Arena arena) {
            int target = arena.heroCell() + 2 * arena.hero().facing().step();
            if (arena.grid().occupantAt(target) == null) {
                return ActionResult.NO_TARGET;
            }
            arena.damage(target, DAMAGE);
            return ActionResult.STRUCK;
        }
    },

    /** Repousse d'une case l'occupant juste devant. Aucun dégât : c'est du placement. */
    PUSH("poussée", "tile/push", 2, false) {
        @Override
        ActionResult applyTo(Arena arena) {
            return arena.shove(arena.heroCell() + arena.hero().facing().step(),
                    arena.hero().facing());
        }
    },

    /** Le héros charge droit devant jusqu'à être arrêté par un bord ou un occupant. */
    DASH("élan", "tile/dash", 3, false) {
        @Override
        ActionResult applyTo(Arena arena) {
            int step = arena.hero().facing().step();
            int moved = 0;
            while (arena.grid().isFree(arena.heroCell() + step)) {
                arena.grid().move(arena.heroCell(), arena.heroCell() + step);
                moved++;
            }
            return moved > 0 ? ActionResult.DASHED : ActionResult.BLOCKED;
        }
    },

    /**
     * Recul d'une case, sans se retourner — et <b>sans consommer de tour</b>.
     *
     * <p>Reculer en gardant sa cible en vue est exactement ce qu'une tuile Free-Play doit permettre
     * : corriger un placement sans offrir un tour aux ennemis.
     */
    SIDESTEP("pas de côté", "tile/step", 3, true) {
        @Override
        ActionResult applyTo(Arena arena) {
            int back = arena.heroCell() + arena.hero().facing().opposite().step();
            if (!arena.grid().isFree(back)) {
                return ActionResult.BLOCKED;
            }
            arena.grid().move(arena.heroCell(), back);
            return ActionResult.MOVED;
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
    PIVOT("volte-face", "tile/pivot", 2, true) {
        @Override
        ActionResult applyTo(Arena arena) {
            arena.hero().face(arena.hero().facing().opposite());
            return ActionResult.TURNED;
        }
    };

    /** Dégâts d'une frappe. Un point : la Vagabonde ne frappe pas fort, elle se replace. */
    public static final int DAMAGE = 1;

    private final String label;
    private final String spriteName;
    private final int rechargeCost;
    private final boolean freePlay;

    Tile(String label, String spriteName, int rechargeCost, boolean freePlay) {
        this.label = label;
        this.spriteName = spriteName;
        this.rechargeCost = rechargeCost;
        this.freePlay = freePlay;
    }

    /** Applique l'effet de la tuile. Ne touche ni à la file, ni aux recharges, ni aux tours. */
    abstract ActionResult applyTo(Arena arena);

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
}
