package com.starfall.game;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Le râtelier : les tuiles que possède le héros, et l'état de recharge de chacune.
 *
 * <p>Une tuile posée sur la file quitte le râtelier ; reprise, elle y revient telle quelle. Ce n'est
 * qu'à l'<b>exécution</b> qu'elle part en recharge — poser puis reprendre ne coûte donc rien du
 * tout, ce qui est indispensable : la file se prépare, et se préparer ne doit jamais punir.
 */
public final class TileRack {

    /** Points encore nécessaires avant que la tuile ne redevienne posable. Zéro = prête. */
    private final Map<Tile, Integer> missingPoints = new EnumMap<>(Tile.class);
    /** Tuiles actuellement dans le râtelier, c'est-à-dire ni posées ni exécutées. */
    private final List<Tile> available = new ArrayList<>();

    public TileRack(Tile... tiles) {
        for (Tile tile : tiles) {
            if (available.contains(tile)) {
                throw new IllegalArgumentException("Tuile en double dans le râtelier : " + tile.label());
            }
            available.add(tile);
            missingPoints.put(tile, 0);
        }
    }

    /**
     * Toutes les tuiles du héros, posées ou non, dans un ordre stable.
     *
     * <p>L'ordre vient de l'{@link EnumMap}, donc de la déclaration de {@link Tile} : il ne dépend
     * ni de l'ordre de construction, ni de ce qui a été joué. C'est indispensable — les raccourcis
     * clavier « 1 à 6 » suivent cet ordre, et une tuile qui changerait de place au fil de la partie
     * rendrait le clavier inutilisable.
     */
    public List<Tile> tiles() {
        return List.copyOf(missingPoints.keySet());
    }

    /** Vrai si la tuile est dans le râtelier et rechargée : elle peut être posée sur la file. */
    public boolean isReady(Tile tile) {
        return available.contains(tile) && missingPoints.getOrDefault(tile, 1) == 0;
    }

    /** Vrai si la tuile est en ce moment dans le râtelier (ni sur la file, ni en cours d'exécution). */
    public boolean holds(Tile tile) {
        return available.contains(tile);
    }

    /** Points restant à accumuler avant que la tuile ne soit rechargée. Zéro si elle est prête. */
    public int missingPoints(Tile tile) {
        return missingPoints.getOrDefault(tile, 0);
    }

    /** Retire une tuile du râtelier pour la poser sur la file. */
    void take(Tile tile) {
        if (!isReady(tile)) {
            throw new IllegalStateException("La tuile " + tile.label() + " n'est pas disponible");
        }
        available.remove(tile);
    }

    /** Remet une tuile reprise de la file, sans la mettre en recharge : elle n'a pas servi. */
    void giveBack(Tile tile) {
        if (available.contains(tile)) {
            throw new IllegalStateException("La tuile " + tile.label() + " est déjà au râtelier");
        }
        available.add(tile);
    }

    /** Remet une tuile exécutée, en recharge cette fois. */
    void giveBackSpent(Tile tile) {
        giveBack(tile);
        missingPoints.put(tile, tile.rechargeCost());
    }

    /**
     * Accorde un point de recharge à toutes les tuiles qui en attendent.
     *
     * <p>Appelé une fois par tour consommé — donc jamais par une tuile Free-Play, ni par une action
     * bloquée. C'est ce qui donne son prix au temps.
     */
    void gainRechargePoint() {
        for (Map.Entry<Tile, Integer> entry : missingPoints.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("TileRack{");
        for (Tile tile : tiles()) {
            builder.append(tile.label())
                    .append(holds(tile) ? "" : "(posée)")
                    .append(missingPoints(tile) > 0 ? "-" + missingPoints(tile) : "")
                    .append(' ');
        }
        return builder.append('}').toString();
    }
}
