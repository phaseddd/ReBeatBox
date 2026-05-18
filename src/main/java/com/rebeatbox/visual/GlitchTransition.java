package com.rebeatbox.visual;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * RGB channel separation glitch effect on {@link BufferedImage} snapshots.
 *
 * <p>Implements the classic digital glitch look by shifting the red channel
 * left and blue channel right while optionally fading the green channel and
 * applying random noise.
 *
 * <p>Edge pixels are clamped to prevent out-of-bounds reads.
 *
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li>D-17: RGB channel separation — red shifted left, blue shifted right</li>
 *   <li>D-17: Configurable noise overlay for digital glitch aesthetic</li>
 *   <li>Edge clamping prevents ArrayIndexOutOfBoundsException</li>
 *   <li>Alpha channel is preserved from source image</li>
 * </ul>
 *
 * <p>This class is used by the transition animation system (Plans 04-04 and
 * 04-06) which apply Timeline-interpolated offset values to snapshots of
 * components during view transitions (sidebar toggle, file load).
 */
public final class GlitchTransition {

    private GlitchTransition() {
        // Utility class — no instantiation
    }

    // ------------------------------------------------------------------
    // Full overload: all parameters configurable
    // ------------------------------------------------------------------

    /**
     * Applies RGB channel separation and optional noise to a source image.
     *
     * <p>For each pixel, the red channel is sampled from {@code x + redOffsetX},
     * green from {@code x + greenOffsetX}, and blue from {@code x + blueOffsetX}.
     * Offsets are clamped to image boundaries.
     *
     * <p>After channel separation, random noise is applied: for each pixel,
     * if a random float is below {@code noiseDensity}, the RGB channels are
     * replaced with a random grayscale value while preserving alpha.
     *
     * @param source       source image (not modified)
     * @param redOffsetX   pixel offset for red channel (negative = left)
     * @param greenOffsetX pixel offset for green channel
     * @param blueOffsetX  pixel offset for blue channel (positive = right)
     * @param greenAlpha   multiplier for green channel brightness (0.0-1.0)
     * @param noiseDensity fraction of pixels to randomize (0.0-1.0)
     * @return new BufferedImage with RGB split applied
     */
    public static BufferedImage applyRgbSplit(BufferedImage source,
                                               int redOffsetX,
                                               int greenOffsetX,
                                               int blueOffsetX,
                                               float greenAlpha,
                                               float noiseDensity) {
        int w = source.getWidth();
        int h = source.getHeight();

        // Extract source pixels in one bulk read
        int[] srcPixels = source.getRGB(0, 0, w, h, null, 0, w);
        int[] dstPixels = new int[w * h];

        // Pre-compute green alpha as a multiplier (0-1 range scaled to 0-255)
        float gAlpha = Math.max(0f, Math.min(1f, greenAlpha));

        for (int y = 0; y < h; y++) {
            int rowOffset = y * w;
            for (int x = 0; x < w; x++) {
                int idx = rowOffset + x;

                // Preserve source alpha
                int a = (srcPixels[idx] >> 24) & 0xFF;

                // Sample R, G, B from offset positions (clamped to image bounds)
                int rx = clamp(x + redOffsetX, 0, w - 1);
                int gx = clamp(x + greenOffsetX, 0, w - 1);
                int bx = clamp(x + blueOffsetX, 0, w - 1);

                int r = (srcPixels[rowOffset + rx] >> 16) & 0xFF;
                int gOrig = (srcPixels[rowOffset + gx] >> 8) & 0xFF;
                int b = srcPixels[rowOffset + bx] & 0xFF;

                // Apply green alpha reduction
                int g = (int) (gOrig * gAlpha);
                g = Math.max(0, Math.min(255, g));

                dstPixels[idx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // Apply random noise overlay
        if (noiseDensity > 0f) {
            Random random = new Random();
            for (int i = 0; i < dstPixels.length; i++) {
                if (random.nextFloat() < noiseDensity) {
                    int randGray = random.nextInt(256);
                    int a = (dstPixels[i] >> 24) & 0xFF;
                    dstPixels[i] = (a << 24) | (randGray << 16) | (randGray << 8) | randGray;
                }
            }
        }

        // Write result pixels in one bulk write
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, dstPixels, 0, w);
        return result;
    }

    // ------------------------------------------------------------------
    // Convenience overload: common case (no green alpha change, no noise)
    // ------------------------------------------------------------------

    /**
     * Convenience overload for the common case — RGB channel separation
     * without green channel adjustment or noise.
     *
     * <p>Equivalent to:
     * {@code applyRgbSplit(source, redOffsetX, 0, blueOffsetX, 1.0f, 0.0f)}.
     *
     * @param source      source image (not modified)
     * @param redOffsetX  pixel offset for red channel
     * @param blueOffsetX pixel offset for blue channel
     * @return new BufferedImage with RGB split applied
     */
    public static BufferedImage applyRgbSplit(BufferedImage source,
                                               int redOffsetX,
                                               int blueOffsetX) {
        return applyRgbSplit(source, redOffsetX, 0, blueOffsetX, 1.0f, 0.0f);
    }

    // ------------------------------------------------------------------
    // Private helper
    // ------------------------------------------------------------------

    /**
     * Clamps a value between min and max inclusive.
     */
    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
