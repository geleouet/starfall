package com.starfall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.starfall.art.ArtFormatException;
import com.starfall.art.AtlasBuilder;
import com.starfall.art.AtlasIndex;
import com.starfall.art.AtlasLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Côté jeu du pipeline pixel art : charge l'atlas produit au build et donne accès aux sprites
 * par leur nom.
 *
 * <p>Le filtrage est en « plus proche voisin » et le mipmapping désactivé : à échelle entière,
 * c'est ce qui garantit qu'un pixel du sprite reste un carré net à l'écran. Tout autre filtre
 * ruinerait ce que {@link PixelViewport} s'efforce d'obtenir.
 */
public final class SpriteAtlas implements Disposable {

    /** Chemin de l'atlas dans les ressources embarquées. */
    public static final String DEFAULT_PATH = "generated/";

    private final Texture texture;
    private final AtlasIndex index;
    private final Map<String, TextureRegion> regions = new LinkedHashMap<>();

    private SpriteAtlas(Texture texture, AtlasIndex index) {
        this.texture = texture;
        this.index = index;
    }

    /** Charge l'atlas depuis les ressources embarquées. */
    public static SpriteAtlas load() {
        return load(DEFAULT_PATH);
    }

    public static SpriteAtlas load(String directory) {
        FileHandle imageFile = Gdx.files.internal(directory + AtlasBuilder.ATLAS_IMAGE);
        FileHandle indexFile = Gdx.files.internal(directory + AtlasBuilder.ATLAS_INDEX);

        // L'atlas est un produit de build : s'il manque, c'est l'etape de build qui n'a pas tourne,
        // et le dire franchement vaut mieux qu'un ecran vide a deboguer.
        if (!imageFile.exists() || !indexFile.exists()) {
            throw new ArtFormatException("atlas introuvable sous « " + directory
                    + " » : lancer « gradlew :core:generateAtlas » (ou simplement « gradlew build »)");
        }

        AtlasIndex index = AtlasIndex.parse(indexFile.path(),
                Arrays.asList(indexFile.readString("UTF-8").split("\\R")));

        Texture texture = new Texture(imageFile);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        if (texture.getWidth() != index.width() || texture.getHeight() != index.height()) {
            texture.dispose();
            throw new ArtFormatException("l'atlas fait " + texture.getWidth() + "x" + texture.getHeight()
                    + " mais son index annonce " + index.width() + "x" + index.height()
                    + " : image et index sont desynchronises");
        }

        SpriteAtlas atlas = new SpriteAtlas(texture, index);
        for (String name : index.names()) {
            AtlasLayout.Placement placement = index.region(name);
            atlas.regions.put(name, new TextureRegion(texture,
                    placement.x(), placement.y(), placement.width(), placement.height()));
        }
        return atlas;
    }

    /**
     * Région d'un sprite.
     *
     * @throws ArtFormatException si le nom est inconnu — un sprite manquant doit se voir tout de
     *                            suite, pas se traduire par un rectangle invisible
     */
    public TextureRegion region(String name) {
        TextureRegion region = regions.get(name);
        if (region == null) {
            throw new ArtFormatException("sprite inconnu : « " + name + " » (connus : "
                    + String.join(", ", regions.keySet()) + ")");
        }
        return region;
    }

    /** Noms des sprites, dans l'ordre de l'index. */
    public List<String> names() {
        return new ArrayList<>(regions.keySet());
    }

    public int atlasWidth() {
        return index.width();
    }

    public int atlasHeight() {
        return index.height();
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
