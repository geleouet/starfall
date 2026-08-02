package dev.starfall.direct;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/**
 * Forces the frame's alpha channel to 1 without touching its colour.
 *
 * <h2>The bug this exists for, which is in the capture harness and is not mine
 * to fix</h2>
 *
 * <p>{@code CaptureApp.flipVertically} turns the GL framebuffer's bottom-up rows
 * into the top-down rows a PNG wants, with
 *
 * <pre>{@code
 *   int top = pixmap.getPixel(x, y);
 *   int bottom = pixmap.getPixel(x, h - 1 - y);
 *   pixmap.drawPixel(x, y, bottom);
 *   pixmap.drawPixel(x, h - 1 - y, top);
 * }</pre>
 *
 * <p>and a {@code Pixmap} defaults to {@code Blending.SourceOver}, so
 * {@code drawPixel} <b>composites</b> rather than assigns. Every pixel whose
 * alpha is below 1 therefore keeps a share of the pixel it was swapped with, and
 * the written PNG carries a faint copy of the whole frame mirrored about its
 * horizontal centre line, at strength {@code 1 - alpha}.
 *
 * <p><b>Every capture this project has ever taken has this artefact.</b> It has
 * never been visible because every scene before System 4 centred a single figure
 * vertically in the frame, so the mirrored copy landed on the figure itself.
 * These scenes put two figures in the lower half of a 4:3 frame -- the framing
 * STYLE.md 9 asks for and reference images 3 to 5 show -- and the copy lands in
 * the empty upper half, where it reads as two grey ghosts hanging in the sky.
 *
 * <p>The alpha is below 1 because the passes that build the picture blend it
 * down: {@code paper.frag} writes 1.0, and every {@code SRC_ALPHA /
 * ONE_MINUS_SRC_ALPHA} pass after it leaves {@code a^2 + (1 - a)} behind, which
 * is under 1 for every {@code a} strictly between 0 and 1. The mist overlay
 * covers the whole frame, so the whole frame ends up under 1.
 *
 * <p><b>The fix belongs in {@code CaptureApp}</b> -- one call to
 * {@code pixmap.setBlending(Pixmap.Blending.None)} before the flip -- and that
 * file is not this pass's to edit. It is reported instead, and these scenes seal
 * their own alpha, which costs one clear with a colour mask and cannot change a
 * single colour value. Doing it in {@code PaperBackground} would fix it for every
 * scene at once and would also change the alpha of frames the project's bind
 * baseline is md5-guarded against, so it is deliberately not done there.
 */
final class Opaque {

    private Opaque() {
    }

    /** Call last, after everything the frame contains has been drawn. */
    static void seal() {
        Gdx.gl.glColorMask(false, false, false, true);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glColorMask(true, true, true, true);
    }
}
