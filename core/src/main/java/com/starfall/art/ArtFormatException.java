package com.starfall.art;

/**
 * Erreur de format dans une source d'art, signalée avec l'emplacement exact.
 *
 * <p>Les sources sont du texte écrit à la main : une faute de frappe doit dire <em>où</em> elle se
 * trouve, pas produire un sprite silencieusement de travers. Le message est délibérément formaté
 * {@code fichier:ligne: message} pour être cliquable dans un terminal ou un IDE.
 */
public class ArtFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ArtFormatException(String source, int lineNumber, String message) {
        super(source + ":" + lineNumber + ": " + message);
    }

    public ArtFormatException(String message) {
        super(message);
    }
}
