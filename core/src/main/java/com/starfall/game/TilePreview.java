package com.starfall.game;

/**
 * Ce qu'une tuile ferait si on l'exécutait <b>maintenant</b>.
 *
 * <h2>Pourquoi cet objet existe</h2>
 *
 * <p>Le joueur pose des tuiles avant de les jouer. Entre le moment où il pose et le moment où il
 * exécute, il doit pouvoir répondre à une question simple : <i>qu'est-ce que ça va faire ?</i>
 * Jusqu'ici le jeu ne répondait pas — l'estoc portait à deux cases, la frappe à une, et rien à
 * l'écran ne le disait.
 *
 * <p>La tentation était de dessiner la portée dans la scène, à côté du code qui l'applique. C'est
 * exactement le piège qui a coûté deux jalons : <b>une règle écrite à deux endroits finit par
 * diverger</b>, et un affichage de portée qui ment est pire que pas d'affichage du tout — il fait
 * prendre des décisions fausses avec confiance.
 *
 * <p>Alors le préavis n'est pas une lecture du code d'exécution : il <b>est</b> le code
 * d'exécution. {@link Tile#applyTo} calcule son préavis puis l'exécute, et l'affichage lit le même
 * préavis. Il n'y a pas deux versions de la règle à tenir d'accord ; il n'y en a qu'une.
 *
 * <h2>Ce que le préavis promet, et ce qu'il ne promet pas</h2>
 *
 * <p>Il promet <b>l'effet direct</b> : quelle case est visée, où quelqu'un atterrit, et quel
 * résultat sera annoncé. Il ne promet pas les <em>conséquences</em> — un colosse explosif qui meurt
 * blesse ses voisins, et cette chaîne appartient à la mort de l'ennemi, pas à la tuile. Le résultat
 * final peut donc être <i>promu</i> en {@link ActionResult#COMBO}, {@link ActionResult#VICTORY} ou
 * {@link ActionResult#DEFEAT} : un enchaînement reste un enchaînement de frappes, une frappe
 * annoncée qui tue n'est pas une frappe annoncée qui rate.
 *
 * @param tile      la tuile concernée
 * @param outcome   le résultat qu'elle produirait, avant promotion éventuelle
 * @param kind      la forme de l'effet, pour le dessiner
 * @param direction sens de l'effet ; jamais {@code null}, même pour un effet immobile
 * @param aim       case visée, ou {@code -1} si la tuile ne vise personne
 * @param landing   case d'arrivée — du poussé, ou du héros — ou {@code -1}. Peut être
 *                  <b>hors grille</b> pour une poussée contre un bord : c'est précisément ce qui
 *                  fait du mur une arme, et l'affichage doit pouvoir le montrer.
 */
public record TilePreview(Tile tile,
                          ActionResult outcome,
                          Kind kind,
                          Direction direction,
                          int aim,
                          int landing) {

    /** Forme de l'effet, du point de vue de ce qu'il faut dessiner. */
    public enum Kind {
        /** Un coup part sur {@link #aim}. */
        HIT,
        /** L'occupant de {@link #aim} part vers {@link #landing}. */
        PUSH,
        /** Le héros se rend en {@link #landing}. */
        MOVE,
        /** Le héros se retourne sur place. */
        TURN,
        /** Rien ne se passera : la tuile sera dépensée dans le vide. */
        NONE,
    }

    /** Vrai si la tuile portera. Une tuile qui ne porte pas est tout de même dépensée. */
    public boolean connects() {
        return kind != Kind.NONE;
    }

    /**
     * Résultats qui ne sont pas des échecs de la tuile mais des <em>conséquences</em> de son succès.
     *
     * <p>Ce sont les seules promotions que {@code settle} a le droit d'appliquer par-dessus le
     * résultat annoncé. La liste vit ici plutôt que dans le test qui la vérifie : sinon le test se
     * contenterait de recopier ce que le code fait, au lieu de contrôler ce qu'il a le droit de
     * faire.
     */
    public static boolean isConsequence(ActionResult result) {
        return result == ActionResult.COMBO
                || result == ActionResult.VICTORY
                || result == ActionResult.DEFEAT;
    }

    // ---------------------------------------------------------------- fabriques

    /**
     * Tuile qui frappe une case à {@code range} devant le héros.
     *
     * <p>La frappe et l'estoc ne diffèrent que par ce nombre. Les écrire deux fois aurait laissé
     * exactement la place qu'il faut pour qu'une seule des deux soit corrigée un jour.
     */
    static TilePreview aimed(Tile tile, Arena arena, int range) {
        Direction facing = arena.hero().facing();
        int aim = arena.heroCell() + range * facing.step();
        if (arena.grid().occupantAt(aim) == null) {
            return new TilePreview(tile, ActionResult.NO_TARGET, Kind.NONE, facing, aim, -1);
        }
        return new TilePreview(tile, ActionResult.STRUCK, Kind.HIT, facing, aim, -1);
    }

    /** Tuile qui ne fera rien : la case visée est vide, ou le pas est bloqué. */
    static TilePreview inert(Tile tile, ActionResult outcome, Direction direction, int aim) {
        return new TilePreview(tile, outcome, Kind.NONE, direction, aim, -1);
    }

    /** Déplacement du héros vers {@code landing}. */
    static TilePreview move(Tile tile, ActionResult outcome, Direction direction, int landing) {
        return new TilePreview(tile, outcome, Kind.MOVE, direction, -1, landing);
    }
}
