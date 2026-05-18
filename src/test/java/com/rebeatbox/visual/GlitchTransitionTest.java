package com.rebeatbox.visual;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlitchTransition}.
 *
 * <p>Covers: RGB channel separation correctness, alpha preservation,
 * dimension stability, edge clamping, green alpha reduction, and
 * noise density behavior.
 */
class GlitchTransitionTest {

    private static final int IMG_SIZE = 16;

    // ------------------------------------------------------------------
    // Test 1: RGB split correctly shifts channels
    // ------------------------------------------------------------------

    @Test
    void testRgbSplitShiftsChannels() {
        BufferedImage source = createTestImage();

        // Shift red left by 2, blue right by 2
        BufferedImage result = GlitchTransition.applyRgbSplit(source, -2, 2);

        // Read center pixel (8, 8) — expected:
        // red from x=6, green from x=8, blue from x=10

        int centerX = 8;
        int centerY = 8;

        int resultPixel = result.getRGB(centerX, centerY);
        int a = (resultPixel >> 24) & 0xFF;
        int r = (resultPixel >> 16) & 0xFF;
        int g = (resultPixel >> 8) & 0xFF;
        int b = resultPixel & 0xFF;

        // Alpha should be preserved
        assertEquals(255, a, "Alpha should be preserved at 255");

        // Red should come from x=6, not x=8
        // Source at x=6: red = (6*16) = 96
        // Source at x=8: red = (8*16) = 128
        assertEquals(96, r, "Red channel should be sampled from x + redOffsetX (x=6)");

        // Green should come from x=8 (unchanged)
        // Source at x=8: green = (8*16) = 128
        assertEquals(128, g, "Green channel should be sampled from original x=8");

        // Blue should come from x=10, not x=8
        // Blue is constant 50 in source
        assertEquals(50, b, "Blue channel should be sampled from x + blueOffsetX (x=10)");
    }

    // ------------------------------------------------------------------
    // Test 2: alpha channel is preserved
    // ------------------------------------------------------------------

    @Test
    void testAlphaPreserved() {
        int testSize = 8;
        BufferedImage source = new BufferedImage(testSize, testSize, BufferedImage.TYPE_INT_ARGB);

        // Set center pixel with half alpha
        int centerX = 4;
        int centerY = 4;
        int halfAlphaPixel = (128 << 24) | (200 << 16) | (100 << 8) | 50;
        source.setRGB(centerX, centerY, halfAlphaPixel);

        BufferedImage result = GlitchTransition.applyRgbSplit(source, -2, 2);

        int resultPixel = result.getRGB(centerX, centerY);
        int alpha = (resultPixel >> 24) & 0xFF;

        assertEquals(128, alpha,
                "Alpha channel should be preserved after RGB split");
    }

    // ------------------------------------------------------------------
    // Test 3: output dimensions match source
    // ------------------------------------------------------------------

    @Test
    void testDimensionsUnchanged() {
        BufferedImage source = createTestImage();
        BufferedImage result = GlitchTransition.applyRgbSplit(source, -3, 3);

        assertEquals(source.getWidth(), result.getWidth(),
                "Output width should match source width");
        assertEquals(source.getHeight(), result.getHeight(),
                "Output height should match source height");
    }

    // ------------------------------------------------------------------
    // Test 4: extreme offsets are clamped at edges
    // ------------------------------------------------------------------

    @Test
    void testClampAtEdges() {
        BufferedImage source = createTestImage();

        // Extreme left shift beyond image boundary
        BufferedImage result = null;
        try {
            result = GlitchTransition.applyRgbSplit(source, -100, 0);
        } catch (Exception e) {
            fail("applyRgbSplit should not throw with extreme offset: " + e.getMessage());
        }

        assertNotNull(result, "Result should not be null with extreme offset");

        // Pixel at x=0 should use red from x=0 (clamped, not x=-100)
        int leftPixel = result.getRGB(0, 0);
        int r = (leftPixel >> 16) & 0xFF;
        // Source at x=0: red = 0. Clamp means red from x=0 (the clamped value)
        assertEquals(0, r, "Edge pixel should use clamped offset (x=0) not x=-100");

        // Pixel at x=15 (last column) with extreme right shift
        BufferedImage result2 = GlitchTransition.applyRgbSplit(source, 100, 0);
        int rightPixel = result2.getRGB(IMG_SIZE - 1, 0);
        int b2 = rightPixel & 0xFF;
        // Blue at x=15 in source is 50 (constant). Offset 100 clamps to x=15.
        assertEquals(50, b2, "Right edge pixel should use clamped offset");
    }

    // ------------------------------------------------------------------
    // Test 5: green alpha reduction
    // ------------------------------------------------------------------

    @Test
    void testGreenAlphaReduction() {
        BufferedImage source = createTestImage();

        // Apply with 50% green alpha
        BufferedImage result = GlitchTransition.applyRgbSplit(
                source, 0, 0, 0, 0.5f, 0.0f);

        // Center pixel: source green at (8,8) = 8 * 16 = 128
        // With 50% alpha: 128 * 0.5 = 64
        int centerX = 8;
        int centerY = 8;
        int resultPixel = result.getRGB(centerX, centerY);
        int g = (resultPixel >> 8) & 0xFF;

        // Allow tolerance of 5 due to rounding
        assertTrue(g >= 59 && g <= 69,
                "Green channel with 0.5 alpha should be ~64, got " + g);
    }

    // ------------------------------------------------------------------
    // Test 6: noise density behavior
    // ------------------------------------------------------------------

    @Test
    void testNoiseApplied() {
        BufferedImage source = createTestImage();

        // 100% noise — virtually all pixels should differ
        BufferedImage resultFullNoise = GlitchTransition.applyRgbSplit(
                source, 0, 0, 0, 1.0f, 1.0f);
        int matchingFull = countMatchingPixels(source, resultFullNoise);
        int total = IMG_SIZE * IMG_SIZE;

        // With 100% noise, very few (<10%) should match
        double matchRatioFull = (double) matchingFull / total;
        assertTrue(matchRatioFull < 0.10,
                "With 100% noise, less than 10% of pixels should match. "
                + "Matched: " + matchingFull + "/" + total
                + " (" + String.format("%.1f", matchRatioFull * 100) + "%)");

        // 0% noise + zero offsets — all pixels should match exactly
        BufferedImage resultNoNoise = GlitchTransition.applyRgbSplit(
                source, 0, 0, 0, 1.0f, 0.0f);
        int matchingNone = countMatchingPixels(source, resultNoNoise);

        assertEquals(total, matchingNone,
                "With 0% noise and 0 offsets, all pixels should match");
    }

    // ------------------------------------------------------------------
    // Test image factory: horizontal + vertical gradient
    // ------------------------------------------------------------------

    /**
     * Creates a 16x16 test image with:
     * - Red channel: horizontal gradient (0 to 240, increasing with x)
     * - Green channel: vertical gradient (0 to 240, increasing with y)
     * - Blue channel: constant 50
     * - Alpha: constant 255 (fully opaque)
     */
    private static BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < IMG_SIZE; y++) {
            for (int x = 0; x < IMG_SIZE; x++) {
                int r = (x * 16) & 0xFF; // 0-240, horizontal
                int g = (y * 16) & 0xFF; // 0-240, vertical
                int b = 50;
                img.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------
    // Helper: count identical pixels between two images
    // ------------------------------------------------------------------

    private static int countMatchingPixels(BufferedImage a, BufferedImage b) {
        int w = a.getWidth();
        int h = a.getHeight();
        if (w != b.getWidth() || h != b.getHeight()) return 0;

        int count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (a.getRGB(x, y) == b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }
}
