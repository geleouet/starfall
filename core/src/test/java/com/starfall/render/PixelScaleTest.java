package com.starfall.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for the pixel-perfect viewport maths. No GL context required. */
class PixelScaleTest {

    private static final int MIN_W = 320;
    private static final int MIN_H = 180;

    private static PixelScale at(int windowWidth, int windowHeight) {
        return PixelScale.compute(windowWidth, windowHeight, MIN_W, MIN_H);
    }

    @Nested
    @DisplayName("Exact multiples of the minimum world size")
    class ExactMultiples {

        @ParameterizedTest(name = "{0}x{1} -> scale {2}")
        @CsvSource({
                "320,180,1",
                "640,360,2",
                "960,540,3",
                "1280,720,4",
                "1600,900,5",
                "1920,1080,6",
                "3840,2160,12",
        })
        void keepsExactlyTheMinimumAreaAndFillsTheWindow(int width, int height, int expectedScale) {
            PixelScale s = at(width, height);

            assertEquals(expectedScale, s.scale);
            assertEquals(MIN_W, s.worldWidth, "no expansion needed on an exact multiple");
            assertEquals(MIN_H, s.worldHeight, "no expansion needed on an exact multiple");
            assertEquals(width, s.screenWidth);
            assertEquals(height, s.screenHeight);
            assertEquals(0, s.screenX, "an exact fit needs no overscan");
            assertEquals(0, s.screenY, "an exact fit needs no overscan");
        }
    }

    @Nested
    @DisplayName("Windows that are not a multiple of the minimum world size")
    class NonMultiples {

        @Test
        @DisplayName("800x450 - scale 2, world expands to 400x225")
        void expandsRatherThanLetterboxes() {
            PixelScale s = at(800, 450);

            assertEquals(2, s.scale);
            assertEquals(400, s.worldWidth);
            assertEquals(225, s.worldHeight);
            assertEquals(800, s.screenWidth);
            assertEquals(450, s.screenHeight);
            assertEquals(0, s.screenX);
            assertEquals(0, s.screenY);
        }

        @Test
        @DisplayName("1000x543 - awkward size, world rounds up and overscans by <1 world pixel")
        void roundsWorldSizeUpAndOverscans() {
            PixelScale s = at(1000, 543);

            assertEquals(3, s.scale);
            assertEquals(334, s.worldWidth);   // ceil(1000 / 3)
            assertEquals(181, s.worldHeight);  // ceil(543 / 3)
            assertEquals(1002, s.screenWidth);
            assertEquals(543, s.screenHeight);
            assertEquals(-1, s.screenX, "the 2px surplus is split, bleeding off both edges");
            assertEquals(0, s.screenY);
        }

        @Test
        @DisplayName("1365x767 - odd surplus is still split with an integer offset")
        void oddSurplusStaysOnTheScreenPixelGrid() {
            PixelScale s = at(1365, 767);

            assertEquals(4, s.scale);
            assertEquals(342, s.worldWidth);   // ceil(1365 / 4)
            assertEquals(192, s.worldHeight);  // ceil(767 / 4)
            assertEquals(1368, s.screenWidth);
            assertEquals(768, s.screenHeight);
            assertEquals(-2, s.screenX);       // floor(-3 / 2)
            assertEquals(-1, s.screenY);       // floor(-1 / 2)
        }

        @Test
        @DisplayName("The scale never goes fractional: 1279x719 stays at 3, not 3.99")
        void neverUsesAFractionalScale() {
            PixelScale s = at(1279, 719);

            assertEquals(3, s.scale);
            assertTrue(s.worldWidth >= MIN_W);
            assertTrue(s.worldHeight >= MIN_H);
        }
    }

    @Nested
    @DisplayName("Very small windows")
    class SmallWindows {

        @ParameterizedTest(name = "{0}x{1}")
        @CsvSource({
                "319,179",
                "160,90",
                "100,50",
                "16,16",
                "1,1",
        })
        void scaleNeverDropsBelowOne(int width, int height) {
            PixelScale s = at(width, height);

            assertEquals(1, s.scale, "sub-minimum windows show less world rather than shrinking pixels");
            assertEquals(width, s.worldWidth);
            assertEquals(height, s.worldHeight);
            assertEquals(width, s.screenWidth);
            assertEquals(height, s.screenHeight);
            assertEquals(0, s.screenX);
            assertEquals(0, s.screenY);
        }

        @ParameterizedTest(name = "{0}x{1}")
        @CsvSource({"0,0", "-100,-100", "0,720"})
        void degenerateSizesAreClampedInsteadOfDividingByZero(int width, int height) {
            PixelScale s = at(width, height);

            assertEquals(1, s.scale);
            assertTrue(s.worldWidth >= 1);
            assertTrue(s.worldHeight >= 1);
        }

        @Test
        @DisplayName("A 1x1 minimum world is legal")
        void supportsATinyMinimumWorld() {
            PixelScale s = PixelScale.compute(100, 100, 1, 1);

            assertEquals(100, s.scale);
            assertEquals(1, s.worldWidth);
            assertEquals(1, s.worldHeight);
        }
    }

    @Nested
    @DisplayName("Extreme aspect ratios")
    class ExtremeAspectRatios {

        @Test
        @DisplayName("Ultra wide - the limiting axis is the height")
        void ultraWideExpandsHorizontally() {
            PixelScale s = at(3840, 200);

            assertEquals(1, s.scale, "height only allows a scale of 1");
            assertEquals(3840, s.worldWidth, "the extra width shows more world");
            assertEquals(200, s.worldHeight);
            assertTrue(s.worldWidth >= MIN_W);
            assertTrue(s.worldHeight >= MIN_H);
        }

        @Test
        @DisplayName("Ultra tall - the limiting axis is the width")
        void ultraTallExpandsVertically() {
            PixelScale s = at(400, 2000);

            assertEquals(1, s.scale);
            assertEquals(400, s.worldWidth);
            assertEquals(2000, s.worldHeight);
        }

        @Test
        @DisplayName("A wide window at a high scale keeps square pixels on both axes")
        void wideWindowKeepsASingleScaleForBothAxes() {
            PixelScale s = at(5120, 1440);

            assertEquals(8, s.scale, "min(5120/320, 1440/180) = min(16, 8)");
            assertEquals(640, s.worldWidth);
            assertEquals(180, s.worldHeight);
        }

        @Test
        @DisplayName("Letterbox-shaped windows never produce black bars")
        void noLetterboxing() {
            PixelScale s = at(2560, 600);

            assertTrue(s.screenWidth >= 2560, "viewport covers the whole window width");
            assertTrue(s.screenHeight >= 600, "viewport covers the whole window height");
        }
    }

    @Nested
    @DisplayName("Invariants across a wide sweep of window sizes")
    class Invariants {

        @Test
        void holdForEveryWindowSizeInRange() {
            for (int w = 1; w <= 4000; w += 7) {
                for (int h = 1; h <= 2400; h += 11) {
                    PixelScale s = at(w, h);

                    assertTrue(s.scale >= 1, () -> "scale must stay whole and positive");

                    // The world is a whole number of world pixels and the viewport an exact multiple.
                    assertEquals(s.worldWidth * s.scale, s.screenWidth);
                    assertEquals(s.worldHeight * s.scale, s.screenHeight);

                    // No letterboxing: the viewport always covers the window...
                    assertTrue(s.screenWidth >= w);
                    assertTrue(s.screenHeight >= h);
                    // ...and never wastes more than one world pixel doing so.
                    assertTrue(s.screenWidth - w < s.scale);
                    assertTrue(s.screenHeight - h < s.scale);

                    // The overscan is centred, integral, and under one world pixel per edge.
                    assertTrue(s.screenX <= 0 && s.screenY <= 0);
                    assertTrue(-s.screenX < s.scale);
                    assertTrue(-s.screenY < s.scale);

                    // The guaranteed area is honoured whenever the window is big enough for it.
                    if (w >= MIN_W && h >= MIN_H) {
                        assertTrue(s.worldWidth >= MIN_W);
                        assertTrue(s.worldHeight >= MIN_H);
                    }
                }
            }
        }

        @Test
        @DisplayName("Growing the window never shrinks the visible world")
        void visibleWorldGrowsMonotonicallyWithinAScaleStep() {
            for (int scaleStep = 1; scaleStep <= 6; scaleStep++) {
                int width = MIN_W * scaleStep;
                PixelScale exact = at(width, MIN_H * scaleStep);
                PixelScale wider = at(width + MIN_W / 2, MIN_H * scaleStep);

                assertEquals(exact.scale, wider.scale);
                assertTrue(wider.worldWidth > exact.worldWidth);
            }
        }
    }
}
