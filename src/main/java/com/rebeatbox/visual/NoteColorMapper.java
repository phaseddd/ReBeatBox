package com.rebeatbox.visual;

import java.awt.Color;

/**
 * Static pitch-to-neon-color mapping utility.
 *
 * <p>Maps MIDI note numbers (0-127) to a rainbow spectrum of vivid, neon colors
 * suitable for rendering on a dark background. Low pitches map to purple/blue,
 * mid to green/cyan, high to orange/red.
 *
 * <p>Uses HSB color space with high saturation and brightness for the neon
 * cyberpunk aesthetic specified in D-05.
 */
public final class NoteColorMapper {

    /** Start of the hue range — purple at pitch 0. */
    private static final float HUE_START = 0.70f;

    /** Total hue span from lowest to highest pitch. */
    private static final float HUE_SPAN = 0.70f;

    /** Saturation for the neon look. */
    private static final float SATURATION = 0.85f;

    /** Brightness for luminous appearance on black background. */
    private static final float BRIGHTNESS = 0.95f;

    /** MIDI note range constants. */
    private static final int MIN_PITCH = 0;
    private static final int MAX_PITCH = 127;

    private NoteColorMapper() {
        // Utility class — no instantiation
    }

    /**
     * Returns a neon color for the given MIDI pitch.
     *
     * <p>The mapping produces a full rainbow gradient:
     * purples at the low end (pitch 0), through blues, cyans, greens,
     * yellows, and oranges, to reds at the high end (pitch 127).
     *
     * @param midiNote MIDI note number, 0-127
     * @return an AWT Color with neon aesthetics
     * @throws IllegalArgumentException if midiNote is outside 0-127
     */
    public static Color forPitch(int midiNote) {
        if (midiNote < MIN_PITCH || midiNote > MAX_PITCH) {
            throw new IllegalArgumentException(
                "midiNote must be 0-127, got " + midiNote);
        }

        // Linear mapping: pitch 0 → hue 0.70 (purple), pitch 127 → hue 0.0 (red)
        float hue = HUE_START - (midiNote / (float) MAX_PITCH) * HUE_SPAN;

        return Color.getHSBColor(hue, SATURATION, BRIGHTNESS);
    }
}
