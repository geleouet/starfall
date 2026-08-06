package com.starfall.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Annoncé ne veut pas dire imminent.
 *
 * <h2>Ce que ce test garde</h2>
 *
 * <p>Une intention dit ce qu'un ennemi fera <b>à sa prochaine activation</b>. Elle ne dit pas si
 * cette activation est celle qui vient. Le colosse ne joue qu'une phase sur deux, et un ennemi
 * étourdi en saute une : tous deux portent une menace qui ne tombera pas ce tour-ci.
 *
 * <p>L'écran distingue désormais les deux — cadre rouge pour « annoncé », cadre doré battant pour
 * « ça part à la fin de ce tour, quoi que tu fasses ». Toute la valeur de cette distinction tient
 * à {@link Arena#willAct(Enemy)}, et une couleur ne peut pas garder un prédicat : si celui-ci se
 * mettait à répondre vrai pour tout le monde, l'écran redeviendrait uniforme sans qu'aucune image
 * ne change de forme, seulement de teinte, et personne ne le verrait.
 *
 * <p>C'est aussi <b>la pire des deux erreurs possibles</b> qui est gardée ici. Lire « j'ai un tour
 * de répit » quand on n'en a pas coûte une partie ; lire l'inverse ne coûte qu'une prudence
 * inutile.
 */
class ImminenceTest {

    @Test
    @DisplayName("Le colosse n'est imminent qu'une phase sur deux")
    void theColossusIsImminentEveryOtherPhase() {
        Arena arena = new Arena(11, 1);
        Enemy colossus = new Enemy(EnemyKind.COLOSSE);
        Enemy sabreur = new Enemy(EnemyKind.SABREUR);
        arena.grid().place(7, colossus);
        arena.grid().place(9, sabreur);
        arena.announceIntentions();

        boolean first = arena.willAct(colossus);
        assertTrue(arena.willAct(sabreur),
                "le sabreur joue a toutes les phases : sans lui, l'alternance ci-dessous pourrait"
                        + " venir d'un compteur global et non de l'archetype");

        // Un tour passe. Le pas est le geste le moins intrusif : il ne tue personne, donc il ne
        // peut pas faire disparaitre le sujet de la mesure.
        arena.step(arena.hero().facing());

        assertFalse(first == arena.willAct(colossus),
                "le colosse est reste dans le meme etat d'une phase a l'autre : il n'agit qu'une"
                        + " fois sur deux, et c'est cette alternance que le cadre dore annonce");
        assertTrue(arena.willAct(sabreur),
                "le sabreur, lui, ne doit jamais cesser d'etre imminent");
    }

    @Test
    @DisplayName("Un ennemi étourdi annonce sans être imminent")
    void aStunnedEnemyAnnouncesWithoutBeingImminent() {
        Arena arena = new Arena(11, 1);
        Enemy sabreur = new Enemy(EnemyKind.SABREUR);
        arena.grid().place(8, sabreur);
        arena.announceIntentions();

        assertTrue(arena.willAct(sabreur), "la premisse : il etait imminent avant d'etre etourdi");

        sabreur.setStunned(true);

        assertFalse(arena.willAct(sabreur),
                "un ennemi etourdi saute son activation : son intention reste affichee, mais elle"
                        + " ne tombe pas ce tour-ci, et confondre les deux fait lire un danger"
                        + " immediat la ou il y a un tour de repit");
    }
}
