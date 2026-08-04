package com.starfall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

/**
 * Writes the rendered frame(s) to PNG so the game can be reviewed without a human at the keyboard.
 *
 * <p>Call {@link #capture()} at the very end of {@code render()}, before the buffer swap, so the
 * back buffer still holds the frame that was just drawn.
 */
public final class ScreenshotRecorder {

    private final FileHandle outputDir;
    private final int frameCount;
    private final List<String> written = new ArrayList<>();

    private int framesCaptured;

    public ScreenshotRecorder(String outputDirPath, int frameCount) {
        File dir = new File(outputDirPath).getAbsoluteFile();
        this.outputDir = Gdx.files.absolute(dir.getPath());
        this.frameCount = Math.max(1, frameCount);
        this.outputDir.mkdirs();
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Could not create screenshot directory: " + dir);
        }
        System.out.println("[Starfall] screenshot mode: " + frameCount + " frame(s) -> " + dir);
    }

    /** Captures one frame. Returns true once every requested frame has been written. */
    public boolean capture() {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();

        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, width, height);
        String name = String.format("starfall-%dx%d-frame%02d.png", width, height, framesCaptured);
        FileHandle file = outputDir.child(name);
        try {
            // flipY: the frame buffer is read bottom-up, PNG rows go top-down.
            PixmapIO.writePNG(file, pixmap, Deflater.DEFAULT_COMPRESSION, true);
        } finally {
            pixmap.dispose();
        }

        String path = file.file().getAbsolutePath();
        written.add(path);
        System.out.println("[Starfall] wrote " + path + " (" + file.length() + " bytes, " + width + "x" + height + ")");
        System.out.flush();

        framesCaptured++;
        return framesCaptured >= frameCount;
    }

    public List<String> writtenFiles() {
        return written;
    }
}
