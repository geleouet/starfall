package dev.starfall.ui;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Brush.Sink} that keeps the geometry instead of drawing it.
 *
 * <p>STYLE.md 11.2b(c): <i>"a cross-check must cross an implementation boundary,
 * not a call site."</i> This is the other implementation of the sink the renderer
 * writes to, so an assertion about the delivered marks reads the vertices the
 * renderer would have been handed rather than recomputing them from the numbers
 * that produced them.
 */
final class Recorder implements Brush.Sink {

    record Vertex(float x, float y, float r, float g, float b, float alpha) {
    }

    record Triangle(int a, int b, int c) {
    }

    final List<Vertex> vertices = new ArrayList<>();
    final List<Triangle> triangles = new ArrayList<>();

    @Override
    public int vertex(float x, float y, Color ink, float alpha) {
        vertices.add(new Vertex(x, y, ink.r, ink.g, ink.b, alpha));
        return vertices.size() - 1;
    }

    @Override
    public void triangle(int a, int b, int c) {
        triangles.add(new Triangle(a, b, c));
    }

    /** Sum of vertex alpha: how much ink this recording put on the paper. */
    double ink() {
        double sum = 0;
        for (Vertex v : vertices) {
            sum += v.alpha();
        }
        return sum;
    }

    /** Alpha-weighted mean height of everything recorded. */
    double meanY() {
        double sum = 0;
        double weight = 0;
        for (Vertex v : vertices) {
            sum += v.y() * v.alpha();
            weight += v.alpha();
        }
        return weight <= 0 ? Double.NaN : sum / weight;
    }

    /** Every vertex whose colour is the vermillion of STYLE.md 2.1, and which carries ink. */
    List<Vertex> vermillion() {
        List<Vertex> out = new ArrayList<>();
        for (Vertex v : vertices) {
            if (v.alpha() > 0.001f
                    && Math.abs(v.r() - dev.starfall.art.Palette.VERMILLION.r) < 1e-4
                    && Math.abs(v.g() - dev.starfall.art.Palette.VERMILLION.g) < 1e-4
                    && Math.abs(v.b() - dev.starfall.art.Palette.VERMILLION.b) < 1e-4) {
                out.add(v);
            }
        }
        return out;
    }

    void clear() {
        vertices.clear();
        triangles.clear();
    }
}
