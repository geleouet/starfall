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
            moves.add(new Move("pas " + direction.label(), false, a -> a.step(direction)));
        }
        if (arena.swapTarget() >= 0) {
            moves.add(new Move("échange", false, Arena::swapWithTarget));
        }
        Tile top = arena.queue().top();
        if (top != null) {
            // Exécuter une tuile Free-Play ne consomme pas de tour : c'est un geste gratuit, et la
            // distinction compte pour la politique comme pour le compte des tours.
            moves.add(new Move("exécuter " + top.label(), top.isFreePlay(), Arena::executeTop));
        }
        if (!arena.queue().isFull()) {
            for (Tile tile : arena.rack().tiles()) {
                if (arena.rack().isReady(tile)) {
                    moves.add(new Move("poser " + tile.label(), true, a -> a.queueTile(tile)));
                }
            }
        }
        for (int index = 0; index < arena.queue().size(); index++) {
            int slot = index;
            moves.add(new Move("reprendre " + (slot + 1), true, a -> a.unqueueAt(slot)));
        }
        return moves;
    }
}
