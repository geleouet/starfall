package com.starfall.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests du viewport lui-même, et pas seulement de son arithmétique.
 *
 * <p>C'est {@link PixelViewport} qui porte l'affirmation « caméra alignée sur la grille, image sans
 * gigue », et elle n'était couverte par aucun test : le découpage « maths pures dans
 * {@link PixelScale} » s'arrêtait juste avant la partie qu'il fallait isoler.
 *
 * <p>Deux dépendances natives sont contournées, sans rien changer au code de production :
 * une {@link Camera} bouchon évite {@code Matrix4.prj} (méthode native), et {@code Gdx.gl} est un
 * proxy sans effet pour l'appel {@code glViewport} que fait {@code Viewport.apply}.
 */
class PixelViewportTest {

    private static final int MIN_W = 320;
    private static final int MIN_H = 180;

    /** Caméra qui enregistre ce qu'on lui donne sans jamais toucher aux natifs gdx. */
    private static final class StubCamera extends Camera {
        @Override
        public void update() {
            // Volontairement vide : update() recalculerait les matrices via du code natif.
        }

        @Override
        public void update(boolean updateFrustum) {
            update();
        }

        float left() {
            return position.x - viewportWidth / 2f;
        }

        float bottom() {
            return position.y - viewportHeight / 2f;
        }
    }

    @BeforeAll
    static void installHeadlessGl() {
        // En mode Pixels, HdpiUtils.glViewport délègue directement à Gdx.gl sans passer par
        // Gdx.graphics : un seul bouchon suffit.
        HdpiUtils.setMode(HdpiMode.Pixels);
        Gdx.gl = (GL20) Proxy.newProxyInstance(
                PixelViewportTest.class.getClassLoader(),
                new Class<?>[]{GL20.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        return 0;
    }

    private static PixelViewport viewportAt(int windowWidth, int windowHeight, StubCamera camera) {
        PixelViewport viewport = new PixelViewport(MIN_W, MIN_H, camera);
        viewport.update(windowWidth, windowHeight, false);
        return viewport;
    }

    @Nested
    @DisplayName("État initial")
    class Construction {

        @Test
        @DisplayName("Un viewport neuf porte déjà une caméra utilisable")
        void aFreshViewportHasAUsableCamera() {
            StubCamera camera = new StubCamera();
            PixelViewport viewport = new PixelViewport(MIN_W, MIN_H, camera);

            // Avant correction, le constructeur n'appelait jamais snapCamera() : un appelant qui
            // dessinait avant le premier update() héritait d'une caméra de taille nulle, en silence.
            assertEquals(MIN_W, camera.viewportWidth);
            assertEquals(MIN_H, camera.viewportHeight);
            assertEquals(0, viewport.getSafeLeft());
            assertEquals(0, viewport.getSafeBottom());
            assertEquals(MIN_W / 2f, camera.position.x);
            assertEquals(MIN_H / 2f, camera.position.y);
        }

        @Test
        @DisplayName("Une taille minimale nulle ou négative est refusée")
        void rejectsADegenerateMinimumWorld() {
            assertThrows(IllegalArgumentException.class, () -> new PixelViewport(0, MIN_H));
            assertThrows(IllegalArgumentException.class, () -> new PixelViewport(MIN_W, -1));
        }
    }

    @Nested
    @DisplayName("Alignement de la caméra sur la grille")
    class CameraSnapping {

        /**
         * L'affirmation anti-scintillement, prise au mot : quelle que soit la cible fractionnaire,
         * les bords du frustum doivent tomber sur des entiers. Un bord fractionnaire ferait tomber
         * l'image entre deux pixels écran et la ferait vibrer au défilement.
         */
        @ParameterizedTest(name = "cible ({0}, {1}) en {2}x{3}")
        @CsvSource({
                "160.5,90.5,1280,720",
                "160.49999,90.5,1281,721",
                "0.5,0.5,1000,543",
                "-0.5,-0.5,1000,543",
                "-7.5,12345.678,801,451",
                "12345.678,-7.5,1279,719",
                "160.0,90.0,320,180",
        })
        void frustumEdgesAlwaysLandOnWholeWorldPixels(float targetX, float targetY, int width, int height) {
            StubCamera camera = new StubCamera();
            PixelViewport viewport = viewportAt(width, height, camera);
            viewport.setCameraTarget(targetX, targetY);

            assertEquals(Math.rint(camera.left()), camera.left(), "bord gauche fractionnaire");
            assertEquals(Math.rint(camera.bottom()), camera.bottom(), "bord bas fractionnaire");
            assertEquals(viewport.getDrawnLeft(), camera.left(), "le bord annoncé et le bord réel divergent");
            assertEquals(viewport.getDrawnBottom(), camera.bottom(), "le bord annoncé et le bord réel divergent");
        }

        @Test
        @DisplayName("La zone sûre reste centrée sur la cible, débordement compris")
        void theSafeAreaStaysCentredOnTheTarget() {
            StubCamera camera = new StubCamera();
            PixelViewport viewport = viewportAt(1281, 721, camera);
            viewport.setCameraTarget(160f, 90f);

            // Le débordement est symétrique : la zone sûre est donc centrée là où la zone dessinée
            // l'est, et le jeu peut raisonner uniquement sur la première.
            assertEquals(viewport.getDrawnLeft() + 1, viewport.getSafeLeft());
            assertEquals(viewport.getDrawnBottom() + 1, viewport.getSafeBottom());
            assertEquals(160f, viewport.getSafeLeft() + viewport.getSafeWorldWidth() / 2f);
            assertEquals(90f, viewport.getSafeBottom() + viewport.getSafeWorldHeight() / 2f);
        }

        @Test
        @DisplayName("Un déplacement sous-pixel ne bouge la caméra que par crans entiers")
        void subPixelMovementSnapsInWholeSteps() {
            StubCamera camera = new StubCamera();
            PixelViewport viewport = viewportAt(1280, 720, camera);

            int distinctPositions = 0;
            int previous = Integer.MIN_VALUE;
            for (int step = 0; step <= 40; step++) {
                viewport.setCameraTarget(160f + step * 0.1f, 90f);
                int left = viewport.getSafeLeft();
                assertEquals(Math.rint(camera.left()), camera.left());
                if (left != previous) {
                    distinctPositions++;
                    previous = left;
                }
            }
            // 4 pixels-monde parcourus en 41 pas : la caméra ne prend que 5 positions distinctes.
            assertEquals(5, distinctPositions);
        }
    }

    @Nested
    @DisplayName("Cohérence avec le calcul d'échelle")
    class LayoutAgreement {

        @Test
        @DisplayName("Le viewport expose exactement ce que PixelScale a calculé")
        void exposesTheComputedLayout() {
            for (int w = 320; w <= 2000; w += 13) {
                for (int h = 180; h <= 1200; h += 7) {
                    PixelScale expected = PixelScale.compute(w, h, MIN_W, MIN_H);
                    PixelViewport viewport = viewportAt(w, h, new StubCamera());

                    assertEquals(expected.scale, viewport.getScale());
                    assertEquals(expected.safeWorldWidth, viewport.getSafeWorldWidth());
                    assertEquals(expected.safeWorldHeight, viewport.getSafeWorldHeight());
                    assertEquals(expected.worldWidth, viewport.getDrawnWorldWidth());
                    assertEquals(expected.worldHeight, viewport.getDrawnWorldHeight());
                    assertEquals(expected.screenX, viewport.getScreenX());
                    assertEquals(expected.screenY, viewport.getScreenY());
                    assertEquals(expected.screenWidth, viewport.getScreenWidth());
                    assertEquals(expected.screenHeight, viewport.getScreenHeight());
                    assertTrue(viewport.getSafeWorldWidth() >= MIN_W);
                    assertTrue(viewport.getSafeWorldHeight() >= MIN_H);
                }
            }
        }

        @Test
        @DisplayName("Redimensionner ne fait pas dériver la caméra")
        void resizingKeepsTheCameraTarget() {
            StubCamera camera = new StubCamera();
            PixelViewport viewport = new PixelViewport(MIN_W, MIN_H, camera);
            viewport.setCameraTarget(200f, 120f);

            for (int[] size : new int[][]{{1280, 720}, {1281, 721}, {640, 360}, {1920, 1080}}) {
                viewport.update(size[0], size[1], false);
                float centreX = viewport.getSafeLeft() + viewport.getSafeWorldWidth() / 2f;
                float centreY = viewport.getSafeBottom() + viewport.getSafeWorldHeight() / 2f;

                // La cible reste au centre à un demi pixel-monde près - l'arrondi sur la grille.
                assertTrue(Math.abs(centreX - 200f) <= 0.5f, "dérive horizontale en " + size[0] + "x" + size[1]);
                assertTrue(Math.abs(centreY - 120f) <= 0.5f, "dérive verticale en " + size[0] + "x" + size[1]);
            }
        }
    }
}
