package com.rebeatbox.ui;

import java.awt.Color;

/**
 * Centralized semantic color constants for the cyber/glitch aesthetic.
 *
 * <p>All UI components read colors from ThemeManager, never hardcode Color values.
 * Implements the three-tier dark background hierarchy (D-03), rainbow hue zones (D-02),
 * and keyboard hint panel state colors (D-30, D-31).</p>
 *
 * <p>Singleton pattern — all members are static. Private constructor prevents instantiation.</p>
 */
public final class ThemeManager {

    // ---- Tiered dark backgrounds (D-03) ----

    /** Deepest background — PianoRollPanel canvas, glass pane base (60% dominant) */
    public static final Color BG_ROOT = new Color(0x0D1120);

    /** Panel backgrounds — SidebarPanel, KeyboardHintPanel (30% secondary) */
    public static final Color BG_SURFACE = new Color(0x1A1E3C);

    /** Interactive element fills — buttons, sliders, pads (30% secondary) */
    public static final Color BG_ELEVATED = new Color(0x28305A);

    // ---- Semantic text colors (UI-SPEC Color section) ----

    /** Primary body text — labels, pad text */
    public static final Color TEXT_PRIMARY = new Color(0xE0E0E0);

    /** Diminished text — idle key labels, placeholder hints */
    public static final Color TEXT_SECONDARY = new Color(0x808080);

    /** Accent text — pressed state labels, active elements (#00E5FF Cyan) */
    public static final Color TEXT_ACCENT = new Color(0x00E5FF);

    // ---- Border colors (D-05) ----

    /** Default idle border for all components */
    public static final Color BORDER_IDLE = new Color(0x3A5078);

    // BORDER_HOVER and BORDER_PRESS are computed via accentForHue(regionHue) at runtime
    // because each UI region uses a different neon hue zone (D-02).

    // ---- Semantic hue zones (D-02) ----

    /** Transport controls — Cyan-Blue */
    public static final float HUE_TRANSPORT = 0.55f;

    /** Drum pads — Purple-Magenta */
    public static final float HUE_DRUM_PADS = 0.80f;

    /** Sidebar — Blue-Purple */
    public static final float HUE_SIDEBAR = 0.65f;

    /** Keyboard — Cyan */
    public static final float HUE_KEYBOARD = 0.50f;

    // ---- Destructive color (UI-SPEC) ----

    /** Destructive action foreground — PadButton context menu "Reset Pads" */
    public static final Color DESTRUCTIVE = new Color(0xFF4444);

    // ---- KeyboardHintPanel state colors (D-30, D-31) ----

    // Idle state — white keys
    public static final Color KEY_IDLE_WHITE_FILL = new Color(0x2A2A2A);
    public static final Color KEY_IDLE_WHITE_BORDER = new Color(0x3A3A3A);
    public static final Color KEY_IDLE_WHITE_LABEL = TEXT_SECONDARY; // #808080

    // Idle state — black keys
    public static final Color KEY_IDLE_BLACK_FILL = new Color(0x1A1A1A);
    public static final Color KEY_IDLE_BLACK_BORDER = new Color(0x2A2A2A);
    public static final Color KEY_IDLE_BLACK_LABEL = new Color(0x606060);

    // Pressed state — any key (instant switch, no animation per D-31)
    public static final Color KEY_PRESSED_BORDER = TEXT_ACCENT;       // #00E5FF
    public static final Color KEY_PRESSED_LABEL = TEXT_ACCENT;        // #00E5FF
    public static final Color KEY_PRESSED_WHITE_FILL = new Color(0, 229, 255, 64);  // rgba(0,229,255,64)
    public static final Color KEY_PRESSED_BLACK_FILL = new Color(0, 229, 255, 51);  // rgba(0,229,255,51)

    // ---- Utility methods ----

    /**
     * Returns a neon accent Color at the given hue with fixed saturation and brightness.
     * Uses HSB(sat=0.85, bri=0.95) — the same constants as NoteColorMapper.forPitch().
     *
     * @param hue hue value 0.0f–1.0f (use HUE_TRANSPORT, HUE_DRUM_PADS, etc.)
     * @return a saturated neon Color at the requested hue
     */
    public static Color accentForHue(float hue) {
        return Color.getHSBColor(hue, 0.85f, 0.95f);
    }

    /**
     * Maps MIDI velocity (0–127) to alpha for note bar rendering.
     * Preserves the above/below trigger line brightness distinction from Phase 2 D-06.
     *
     * @param velocity     MIDI velocity 0–127
     * @param aboveTrigger {@code true} if note is above the trigger line (future/current notes)
     * @return alpha value: 0.55–1.00 (above) or 0.25–0.65 (below)
     */
    public static float velocityToAlpha(int velocity, boolean aboveTrigger) {
        float v = velocity / 127.0f;
        if (aboveTrigger) {
            return 0.55f + v * 0.45f;  // 0.55 at vel=0, 1.00 at vel=127
        } else {
            return 0.25f + v * 0.40f;  // 0.25 at vel=0, 0.65 at vel=127
        }
    }

    /** Private constructor — singleton, no instantiation. */
    private ThemeManager() {}
}
