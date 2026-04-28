package com.rebeatbox.ui;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThemeManager — verifies all color constants and utility methods
 * against the D-01 through D-04, D-28, D-30, D-31 contracts.
 */
class ThemeManagerTest {

    @Test
    void testBackgroundColors() {
        assertEquals(new Color(0x0A0A14), ThemeManager.BG_ROOT,
                "BG_ROOT must be #0A0A14");
        assertEquals(new Color(0x12122A), ThemeManager.BG_SURFACE,
                "BG_SURFACE must be #12122A");
        assertEquals(new Color(0x1A1A3E), ThemeManager.BG_ELEVATED,
                "BG_ELEVATED must be #1A1A3E");
    }

    @Test
    void testTextColors() {
        assertEquals(new Color(0xE0E0E0), ThemeManager.TEXT_PRIMARY,
                "TEXT_PRIMARY must be #E0E0E0");
        assertEquals(new Color(0x808080), ThemeManager.TEXT_SECONDARY,
                "TEXT_SECONDARY must be #808080");
        assertEquals(new Color(0x00E5FF), ThemeManager.TEXT_ACCENT,
                "TEXT_ACCENT must be #00E5FF");
    }

    @Test
    void testAccentForHue() {
        Color cyan = ThemeManager.accentForHue(0.55f);
        assertNotNull(cyan, "accentForHue(0.55f) must return non-null");

        Color purple = ThemeManager.accentForHue(0.80f);
        assertNotNull(purple, "accentForHue(0.80f) must return non-null");

        // Different hues must produce different colors
        assertNotEquals(cyan.getRGB(), purple.getRGB(),
                "accentForHue(0.55f) and accentForHue(0.80f) must produce different colors");

        // Verify saturation >= 0.80f via HSB decomposition
        float[] hsb = Color.RGBtoHSB(
                cyan.getRed(), cyan.getGreen(), cyan.getBlue(), null);
        assertTrue(hsb[1] >= 0.80f,
                "Saturation must be >= 0.80, got: " + hsb[1]);
    }

    @Test
    void testVelocityToAlphaAbove() {
        assertEquals(0.30f, ThemeManager.velocityToAlpha(0, true), 0.001,
                "velocityToAlpha(0, true) must be 0.30");
        assertEquals(1.00f, ThemeManager.velocityToAlpha(127, true), 0.001,
                "velocityToAlpha(127, true) must be 1.00");

        // Monotonic: higher velocity = higher alpha
        assertTrue(
                ThemeManager.velocityToAlpha(64, true) > ThemeManager.velocityToAlpha(32, true),
                "velocityToAlpha must be monotonic (64 > 32)");
    }

    @Test
    void testVelocityToAlphaBelow() {
        assertEquals(0.15f, ThemeManager.velocityToAlpha(0, false), 0.001,
                "velocityToAlpha(0, false) must be 0.15");
        assertEquals(0.40f, ThemeManager.velocityToAlpha(127, false), 0.001,
                "velocityToAlpha(127, false) must be 0.40");
    }

    @Test
    void testKeyColors() {
        assertEquals(new Color(0x00E5FF), ThemeManager.KEY_PRESSED_BORDER,
                "KEY_PRESSED_BORDER must be #00E5FF");
        assertEquals(new Color(0x2A2A2A), ThemeManager.KEY_IDLE_WHITE_FILL,
                "KEY_IDLE_WHITE_FILL must be #2A2A2A");
        assertEquals(new Color(0x1A1A1A), ThemeManager.KEY_IDLE_BLACK_FILL,
                "KEY_IDLE_BLACK_FILL must be #1A1A1A");
    }

    @Test
    void testBorderColors() {
        assertEquals(new Color(0x2A3A5E), ThemeManager.BORDER_IDLE,
                "BORDER_IDLE must be #2A3A5E");
        assertEquals(new Color(0xFF4444), ThemeManager.DESTRUCTIVE,
                "DESTRUCTIVE must be #FF4444");
    }
}
