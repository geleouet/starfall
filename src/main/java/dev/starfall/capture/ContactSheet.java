package dev.starfall.capture;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Assembles captured frames into a single labelled grid image.
 *
 * <p>The point of a contact sheet is to judge a whole trajectory at a glance —
 * per STYLE.md the failure modes we care about (snapping, mechanical overshoot,
 * everything peaking on the same frame) are only visible across consecutive
 * frames, not in any single one. Frames are therefore numbered and timestamped
 * so a critique can name exactly where the motion goes wrong.
 *
 * <p>Uses only {@code java.desktop}, so the feedback loop has no Python or
 * ImageMagick dependency.
 */
public final class ContactSheet {

    private static final int PAD = 10;
    private static final int HEADER = 46;
    private static final int CAPTION = 18;
    private static final Color SHEET_BG = new Color(0x1A1E26);
    private static final Color TEXT = new Color(0xC8C2B8);
    private static final Color TEXT_DIM = new Color(0x6E7684);
    private static final Color CELL_BG = new Color(0x11141A);

    private ContactSheet() {
    }

    public static void build(List<File> frames, File out, int cols, String title,
                             float duration, int frameCount) throws IOException {
        if (frames.isEmpty()) {
            throw new IOException("no frames to assemble");
        }

        BufferedImage first = ImageIO.read(frames.get(0));
        int cellW = first.getWidth();
        int cellH = first.getHeight();

        // Keep sheets a sane size regardless of capture resolution.
        double scale = Math.min(1.0, 460.0 / cellW);
        int tw = (int) Math.round(cellW * scale);
        int th = (int) Math.round(cellH * scale);

        int rows = (int) Math.ceil(frames.size() / (double) cols);
        int sheetW = PAD + cols * (tw + PAD);
        int sheetH = HEADER + rows * (th + CAPTION + PAD) + PAD;

        BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(SHEET_BG);
        g.fillRect(0, 0, sheetW, sheetH);

        g.setColor(TEXT);
        g.setFont(new Font("Georgia", Font.PLAIN, 17));
        g.drawString(title, PAD, 24);

        g.setColor(TEXT_DIM);
        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        g.drawString(String.format("%d frames over %.2fs  |  %dx%d  |  %s",
                frameCount, duration, cellW, cellH, out.getParentFile().getName()), PAD, 40);

        float dt = frameCount <= 1 ? 0f : duration / (frameCount - 1);

        for (int i = 0; i < frames.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = PAD + col * (tw + PAD);
            int y = HEADER + row * (th + CAPTION + PAD);

            g.setColor(CELL_BG);
            g.fillRect(x, y, tw, th);

            BufferedImage img = ImageIO.read(frames.get(i));
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(img, x, y, tw, th, null);

            g.setColor(TEXT_DIM);
            g.setFont(new Font("Consolas", Font.PLAIN, 10));
            g.drawString(String.format("%02d   t=%.3fs", i, i * dt), x + 1, y + th + 12);
        }

        g.dispose();
        File parent = out.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(sheet, "png", out);
    }
}
