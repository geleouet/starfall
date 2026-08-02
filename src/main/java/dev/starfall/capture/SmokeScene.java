package dev.starfall.capture;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import dev.starfall.art.Palette;

/**
 * Minimal pipeline validation: the STYLE.md dusk sky gradient with a pale blade
 * sweeping across it. Exists purely to prove that context creation, offscreen
 * rendering, framebuffer readback, PNG writing and contact-sheet assembly all
 * work end to end. Not representative of the target look.
 */
public class SmokeScene implements Scene {

    private ShapeRenderer shapes;
    private int width;
    private int height;
    private float t;

    @Override
    public String name() {
        return "smoke";
    }

    @Override
    public String description() {
        return "Pipeline smoke test: dusk gradient + sweeping blade";
    }

    @Override
    public void create(SceneContext ctx) {
        this.width = ctx.width;
        this.height = ctx.height;
        this.shapes = new ShapeRenderer();
        this.shapes.setAutoShapeType(true);
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void update(float dt) {
        t += dt;
    }

    @Override
    public void render() {
        shapes.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Sky: zenith -> violet -> coral, per STYLE.md section 2.1.
        float mid = height * 0.55f;
        shapes.rect(0, mid, width, height - mid,
                Palette.SKY_MID, Palette.SKY_MID, Palette.SKY_ZENITH, Palette.SKY_ZENITH);
        shapes.rect(0, 0, width, mid,
                Palette.SKY_HORIZON, Palette.SKY_HORIZON, Palette.SKY_MID, Palette.SKY_MID);

        // Ground smear.
        shapes.rect(0, 0, width, height * 0.12f,
                Palette.INK_BLACK, Palette.INK_BLACK, Palette.INK_INDIGO, Palette.INK_INDIGO);

        // A pale luminous blade sweeping through an arc.
        float cx = width * 0.5f;
        float cy = height * 0.45f;
        float angle = MathUtils.sin(t * 1.4f) * 55f + 90f;
        float len = height * 0.34f;
        float dx = MathUtils.cosDeg(angle) * len;
        float dy = MathUtils.sinDeg(angle) * len;

        Color glow = new Color(Palette.BLADE);
        for (int i = 6; i >= 1; i--) {
            glow.a = 0.06f * i;
            shapes.setColor(glow);
            shapes.rectLine(cx, cy, cx + dx, cy + dy, 2.5f * i);
        }
        shapes.setColor(Palette.BLADE);
        shapes.rectLine(cx, cy, cx + dx, cy + dy, 2.5f);

        shapes.end();
    }

    @Override
    public float duration() {
        return 2.2f;
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
    }
}
