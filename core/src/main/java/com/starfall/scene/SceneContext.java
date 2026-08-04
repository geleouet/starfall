package com.starfall.scene;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.starfall.LaunchOptions;
import com.starfall.render.PixelFont;
import com.starfall.render.PixelPainter;
import com.starfall.render.PixelViewport;
import com.starfall.render.SpriteAtlas;

/**
 * Ce qu'une scène reçoit du jeu : de quoi dessiner, et rien de plus.
 *
 * <p>Les ressources appartiennent au jeu, pas à la scène : changer de scène ne recharge donc pas
 * l'atlas ni la police.
 *
 * <p>La police est la <b>même</b> que celle du bandeau de diagnostic, et volontairement : une
 * seconde police, plus petite, aurait voulu dire une seconde table de glyphes à tenir à jour en
 * français accentué, et donc un jour un « É » manquant d'un seul côté. Sa cellule fait 11 px-monde,
 * ce qui, à l'échelle du plateau, donne un texte de la taille d'une étiquette — assez pour être lu,
 * assez peu pour laisser voir la scène.
 */
public record SceneContext(SpriteBatch batch,
                           PixelPainter painter,
                           SpriteAtlas atlas,
                           PixelViewport viewport,
                           PixelFont font,
                           LaunchOptions options) {
}
