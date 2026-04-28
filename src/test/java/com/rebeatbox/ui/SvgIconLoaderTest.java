package com.rebeatbox.ui;

import java.awt.image.BufferedImage;

import org.apache.batik.transcoder.TranscoderException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SvgIconLoader — verifies SVG loading, caching, error handling,
 * and the convenience getIcon() method against the D-24 contract.
 */
class SvgIconLoaderTest {

    @Test
    void testLoadTestIcon() throws TranscoderException {
        BufferedImage img = SvgIconLoader.loadSvg("test-icon", 24, 24);
        assertNotNull(img, "loadSvg('test-icon', 24, 24) must return a non-null image");
        assertEquals(24, img.getWidth(), "Rendered width must be 24");
        assertEquals(24, img.getHeight(), "Rendered height must be 24");
    }

    @Test
    void testCacheReturnsSameInstance() throws TranscoderException {
        BufferedImage img1 = SvgIconLoader.loadSvg("test-icon", 24, 24);
        BufferedImage img2 = SvgIconLoader.loadSvg("test-icon", 24, 24);
        assertNotNull(img1);
        assertNotNull(img2);
        assertSame(img1, img2,
                "Same name+size must return the exact same BufferedImage instance");
    }

    @Test
    void testDifferentSizesDifferentInstances() throws TranscoderException {
        BufferedImage img24 = SvgIconLoader.loadSvg("test-icon", 24, 24);
        BufferedImage img48 = SvgIconLoader.loadSvg("test-icon", 48, 48);
        assertNotNull(img24);
        assertNotNull(img48);
        assertNotSame(img24, img48,
                "Different sizes must return different BufferedImage instances");
    }

    @Test
    void testMissingIconThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                SvgIconLoader.loadSvg("nonexistent", 24, 24);
            } catch (TranscoderException e) {
                fail("Should not get TranscoderException for missing icon; "
                        + "IllegalArgumentException expected first");
            }
        });
    }

    @Test
    void testGetIconSquare() throws TranscoderException {
        BufferedImage img = SvgIconLoader.getIcon("test-icon", 24);
        assertNotNull(img, "getIcon('test-icon', 24) must return a non-null image");
        assertEquals(24, img.getWidth(), "Width must be 24");
        assertEquals(24, img.getHeight(), "Height must be 24");
    }
}
