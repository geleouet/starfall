package com.starfall.sim;

import com.starfall.game.ActionResult;
import com.starfall.game.Arena;
import com.starfall.game.Direction;
import com.starfall.game.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Un geste possible du joueur, réifié pour qu'une machine puisse en essayer plusieurs.
 *
 * <p>Le jeu a toujours su ce qu'un geste <em>fait</em> ; il ne savait pas <em>les énumérer</em>.
 * Cette classe le permet, et c'est ce qui rend l'équilibrage mesurable au lieu d'être une affaire
 * d'impression : sans quelqu'un qui joue, on ne peut pas dire si un nombre est trop grand.
 *
 * <p>Un geste porte son propre libellé français, pour qu'une partie simulée puisse être relue.
 */
public record Move(String label, boolean free, Function<Arena, ActionResult> action) {

    /** Applique le geste et rend son résultat. */
    public ActionResult applyTo(Arena arena) {
        return action.apply(arena);
    }

    /**
     * Tous les gestes légaux dans l'état courant.
     *
     * <p>« Légal » au sens de l'interface, pas du modèle : on n'énumère pas les gestes que le jeu
     * refuserait de toute façon — poser une tuile en recharge, dépiler une file vide. Une politique
     * qui les essaierait passerait son temps à se faire dire non, et les mesures compteraient des
     * tours qui n'existent pas.
     */
    public static List<Move> legal(Arena arena) {
        List<Move> moves = new ArrayList<>();
        if (arena.isOver()) {
            return moves;
        }

        for (Direction direction : Direction.values()) {
            // Un pas contre un mur rend BLOCKED et ne consomme aucun tour : l'enumerer donnait aux
            // politiques un geste qui ne fait rien, dans l'instrument meme qui sert a mesurer
            // combien de tours coute une ligne de jeu.
            if (arena.canStep(direction)) {
                moves.add(new Move("pas " + direction.label(), false, a -> a.step(direction)));
            }
        }
        if (arena.swapTarget() >= 0) {
            moves.add(new Move("échange", false, Arena::swapWithTarget));
        }
        if (!arena.queue().isEmpty()) {
            moves.add(new Move("salve", false, Arena::unleash));
        }
        // La question « une touche peut-elle poser cette tuile » se pose a l'arene, elle ne se
        // recopie pas. Elle l'etait ici : fin de partie, file pleine, recharge - les trois memes
        // conditions que refusalToQueue, dupliquees. C'etait le QUATRIEME endroit, et le seul qui
        // restait apres la couleur de portee, le halo de survol et l'infobulle de ratelier ; il
        // gouverne l'instrument de mesure sur lequel repose tout M10.
        for (Tile tile : arena.rack().tiles()) {
            if (arena.canQueue(tile)) {
                // Poser coûte un tour, sauf pour une tuile Free-Play : c'est là que le temps
                // passe désormais, et la distinction compte pour la politique autant que pour
                // le compte des tours.
                moves.add(new Move("poser " + tile.label(), tile.isFreePlay(),
                        a -> a.queueTile(tile)));
            }
        }
        for (int index = 0; index < arena.queue().size(); index++) {
            int slot = index;
            // Comme le pas et la pose : on demande a l'arene. Mesure faite depuis, ce filtre ne
            // filtre RIEN ici - la boucle est bornee par la taille de la file et legal() sort deja
            // par anticipation sur isOver() -, et le dire vaut mieux que de le laisser croire. Il
            // reste parce qu'un contexte qui garantit aujourd'hui peut cesser de garantir demain,
            // pas parce qu'il rattrape quoi que ce soit.
            if (arena.canUnqueue(slot)) {
                moves.add(new Move("reprendre " + (slot + 1), true, a -> a.unqueueAt(slot)));
            }
        }
        return moves;
    }
}
