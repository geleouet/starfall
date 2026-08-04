package com.starfall.art;

/**
 * Un sprite tel qu'il a été écrit à la main : un nom, une taille, et une couleur RGBA par pixel.
 *
 * <p>Les lignes sont rangées <b>de haut en bas</b>, comme on les lit dans le fichier source et comme
 * elles seront écrites dans le PNG. La conversion vers le repère du monde, où Y monte, est l'affaire
 * du rendu, pas du format.
 */
public final class SpriteSource {

    private final String name;
    private final int width;
    private final int height;
    /** Couleurs RGBA empaquetées {@code 0xRRGGBBAA}, ligne par ligne depuis le haut. */
    private final int[] pixels;

    SpriteSource(String name, int width, int height, int[] pixels) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * Couleur d'un pixel, en {@code 0xRRGGBBAA}.
     *
     * @param x colonne, 0 à gauche
     * @param y ligne, 0 <b>en haut</b>
     */
    public int pixel(int x, int y) {
        return pixels[y * width + x];
    }

    @Override
    public String toString() {
        return "SpriteSource{" + name + " " + width + "x" + height + "}";
    }
}
