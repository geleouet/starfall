package com.starfall.scene;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.starfall.LaunchOptions;
import com.starfall.render.PixelPainter;
import com.starfall.render.PixelViewport;
import com.starfall.render.SpriteAtlas;

/**
 * Ce qu'une scène reçoit du jeu : de quoi dessiner, et rien de plus.
 *
 * <p>Les ressources appartiennent au jeu, pas à la scène : changer de scène ne recharge donc pas
 * l'atlas ni la police.
 */
public record SceneContext(SpriteBatch batch,
                           PixelPainter painter,
                           SpriteAtlas atlas,
                           PixelViewport viewport,
                           LaunchOptions options) {
}
