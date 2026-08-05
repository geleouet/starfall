package com.starfall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/**
 * Écrit l'image rendue en PNG pour que le jeu puisse être relu sans personne au clavier.
 *
 * <p>Appeler {@link #capture()} tout à la fin de {@code render()}, avant l'échange de tampons, tant
 * que le tampon arrière contient encore l'image qui vient d'être dessinée.
 *
 * <p>Toute la boucle de review du projet repose sur ce harnais. Il est donc volontairement bavard :
 * il signale les fichiers qu'il écrase, les fichiers périmés qu'il laisse derrière lui, et refuse de
 * faire croire au succès quand la fenêtre obtenue n'est pas celle qui était demandée.
 */
public final class ScreenshotRecorder {

    private final FileHandle outputDir;
    private final int frameCount;
    private final int firstFrame;
    private final int requestedWidth;
    private final int requestedHeight;

    private int framesCaptured;
    private boolean sizeMismatch;

    /**
     * @param outputDirPath   dossier de sortie, créé s'il n'existe pas
     * @param frameCount      nombre d'images à écrire
     * @param requestedWidth  largeur de fenêtre demandée sur la ligne de commande
     * @param requestedHeight hauteur de fenêtre demandée sur la ligne de commande
     */
    public ScreenshotRecorder(String outputDirPath, int frameCount, int firstFrame,
            int requestedWidth, int requestedHeight) {
        File dir = new File(outputDirPath).getAbsoluteFile();
        this.outputDir = Gdx.files.absolute(dir.getPath());
        this.frameCount = Math.max(1, frameCount);
        this.firstFrame = Math.max(0, firstFrame);
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
        this.outputDir.mkdirs();
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Impossible de créer le dossier de capture : " + dir);
        }
        System.out.println("[Starfall] mode capture : " + this.frameCount + " image(s) -> " + dir);
        warnAboutStaleFiles();
    }

    /**
     * Les noms de fichiers ne dépendent que de la taille et de l'index. Une capture à une autre
     * taille laisse donc en place les PNG de la précédente, et rien ne distingue à l'œil une image
     * fraîche d'une image périmée. On énumère donc ce qui était déjà là : le relecteur sait
     * exactement quelles images cette exécution n'a pas produites, sans comparer des horodatages.
     */
    private void warnAboutStaleFiles() {
        FileHandle[] existing = outputDir.list(".png");
        if (existing.length == 0) {
            return;
        }
        StringBuilder names = new StringBuilder();
        for (FileHandle file : existing) {
            names.append(names.isEmpty() ? "" : ", ").append(file.name());
        }
        System.out.println("[Starfall] déjà présents avant cette capture (non produits par cette exécution) : "
                + names);
    }

    /** Capture une image. Renvoie vrai une fois que toutes les images demandées ont été écrites. */
    public boolean capture() {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();

        if (framesCaptured == 0) {
            checkRequestedSize(width, height);
        }

        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
        // Le nom porte l'image du SCÉNARIO et non le compteur d'écriture : avec « --from 80 », le
        // premier fichier s'appelle frame80 et pas frame00.
        //
        // La justification écrite ici disait « sans quoi deux planches de deux écrans différents
        // porteraient le même nom en montrant des instants sans rapport ». C'était faux, et une
        // review l'a relevé : c'est déjà le cas partout, arena/frame00 et wave3/frame00 coexistent
        // sans se gêner puisqu'ils vivent dans des dossiers distincts. La vraie raison est plus
        // simple : celui qui relit une planche doit pouvoir la situer dans le scénario sans
        // calculer un décalage de tête.
        String name = String.format("starfall-%dx%d-frame%02d.png", width, height,
                firstFrame + framesCaptured);
        FileHandle file = outputDir.child(name);
        if (file.exists()) {
            System.out.println("[Starfall] écrasement de " + name);
        }
        try {
            forceOpaque(pixmap);
            // flipY : le tampon d'image se lit de bas en haut, les lignes d'un PNG vont de haut en bas.
            PixmapIO.writePNG(file, pixmap, Deflater.DEFAULT_COMPRESSION, true);
        } finally {
            pixmap.dispose();
        }

        System.out.println("[Starfall] écrit " + file.file().getAbsolutePath()
                + " (" + file.length() + " octets, " + width + "x" + height + ")");
        System.out.flush();

        framesCaptured++;
        return framesCaptured >= frameCount;
    }

    /**
     * Le gestionnaire de fenêtres borne la taille demandée à la zone de travail du bureau, et le
     * lanceur impose lui-même une taille minimale. Obtenir 2884x1770 après avoir demandé 3000x2000,
     * sans un mot et en sortie 0, rendrait toute la boucle de review mensongère.
     */
    private void checkRequestedSize(int actualWidth, int actualHeight) {
        if (actualWidth == requestedWidth && actualHeight == requestedHeight) {
            return;
        }
        sizeMismatch = true;
        System.err.println("[Starfall] ERREUR : taille demandée " + requestedWidth + "x" + requestedHeight
                + ", taille obtenue " + actualWidth + "x" + actualHeight
                + ". L'écran ou la taille minimale de fenêtre a borné la demande ;"
                + " les PNG produits ne montrent pas ce qui a été demandé.");
        System.err.flush();
    }

    /**
     * Le tampon arrière conserve son canal alpha, et le panneau d'interface est composé avec un
     * alpha de 0xd0 : les PNG sortaient donc semi-transparents, avec des RGB déjà composés sur du
     * noir. Rendus sur un fond clair par un navigateur ou un visualiseur, ils n'avaient plus
     * l'apparence qu'ils avaient à l'écran. Une capture destinée à être relue doit être opaque.
     */
    private static void forceOpaque(Pixmap pixmap) {
        ByteBuffer pixels = pixmap.getPixels();
        int capacity = pixels.capacity();
        for (int i = 3; i < capacity; i += 4) {
            pixels.put(i, (byte) 0xFF);
        }
    }

    /** Nombre d'images déjà écrites. Sert au jeu à faire varier la scène de façon déterministe. */
    public int framesCaptured() {
        return framesCaptured;
    }

    /** Vrai si la fenêtre obtenue ne correspondait pas à la taille demandée. */
    public boolean hasSizeMismatch() {
        return sizeMismatch;
    }
}
