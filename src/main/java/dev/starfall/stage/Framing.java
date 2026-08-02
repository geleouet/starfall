package dev.starfall.stage;

import java.util.Locale;

/**
 * What the camera is holding: a centre, in tiles, and how many tiles wide.
 *
 * <p>Expressed in tiles rather than in pixels or world units on purpose. STYLE.md
 * 9 and combat-design.md 1.6 both state the framing law in tiles -- "the planning
 * framing must fit the lane, so a 5-tile lane is already near-intimate while a
 * 15-tile lane is genuinely wide" -- and a framing in tiles survives any choice of
 * resolution, aspect ratio or world scale the renderer later makes. The one thing
 * this layer must get right is the <em>ratio</em> between the wide shot and the
 * close one, and that ratio is a property of the lane, not of the screen.
 *
 * @param centreTile the tile the frame is centred on, fractional
 * @param widthTiles how many tiles across the frame holds
 */
public record Framing(double centreTile, double widthTiles) {

    public Framing {
        if (widthTiles <= 0) {
            throw new IllegalArgumentException("a framing holds at least some lane: " + widthTiles);
        }
    }

    public double left() {
        return centreTile - widthTiles / 2.0;
    }

    public double right() {
        return centreTile + widthTiles / 2.0;
    }

    /** The same shot nudged, for STYLE.md 9's "the camera is never perfectly still". */
    public Framing drifted(double dTile, double dWidth) {
        return new Framing(centreTile + dTile, Math.max(0.5, widthTiles + dWidth));
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "@%.3f w%.3f", centreTile, widthTiles);
    }
}
