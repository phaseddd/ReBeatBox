package com.rebeatbox.live;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Three-row computer keyboard to MIDI note mapping (D-01).
 * Covers C3-C6 across number row, QWERTY row, and bottom row.
 * Tracks active note state for OS auto-repeat deduplication (D-03).
 */
public final class KeyboardMapper {

    /** KeyCode (VK_*) to MIDI note number (0-127) lookup. */
    private static final Map<Integer, Integer> KEY_TO_NOTE = new HashMap<>();

    static {
        // Upper octave — number row: C5-C6
        KEY_TO_NOTE.put(KeyEvent.VK_1, 72);
        KEY_TO_NOTE.put(KeyEvent.VK_2, 73);
        KEY_TO_NOTE.put(KeyEvent.VK_3, 74);
        KEY_TO_NOTE.put(KeyEvent.VK_4, 75);
        KEY_TO_NOTE.put(KeyEvent.VK_5, 76);
        KEY_TO_NOTE.put(KeyEvent.VK_6, 77);
        KEY_TO_NOTE.put(KeyEvent.VK_7, 78);
        KEY_TO_NOTE.put(KeyEvent.VK_8, 79);
        KEY_TO_NOTE.put(KeyEvent.VK_9, 80);
        KEY_TO_NOTE.put(KeyEvent.VK_0, 81);
        KEY_TO_NOTE.put(KeyEvent.VK_MINUS, 82);
        KEY_TO_NOTE.put(KeyEvent.VK_EQUALS, 83);

        // Middle octave — QWERTY row: C4-C5
        KEY_TO_NOTE.put(KeyEvent.VK_Q, 60);
        KEY_TO_NOTE.put(KeyEvent.VK_W, 61);
        KEY_TO_NOTE.put(KeyEvent.VK_E, 62);
        KEY_TO_NOTE.put(KeyEvent.VK_R, 63);
        KEY_TO_NOTE.put(KeyEvent.VK_T, 64);
        KEY_TO_NOTE.put(KeyEvent.VK_Y, 65);
        KEY_TO_NOTE.put(KeyEvent.VK_U, 66);
        KEY_TO_NOTE.put(KeyEvent.VK_I, 67);
        KEY_TO_NOTE.put(KeyEvent.VK_O, 68);
        KEY_TO_NOTE.put(KeyEvent.VK_P, 69);
        KEY_TO_NOTE.put(KeyEvent.VK_OPEN_BRACKET, 70);
        KEY_TO_NOTE.put(KeyEvent.VK_CLOSE_BRACKET, 71);

        // Lower octave — bottom row: C3-C4
        KEY_TO_NOTE.put(KeyEvent.VK_Z, 48);
        KEY_TO_NOTE.put(KeyEvent.VK_X, 49);
        KEY_TO_NOTE.put(KeyEvent.VK_C, 50);
        KEY_TO_NOTE.put(KeyEvent.VK_V, 51);
        KEY_TO_NOTE.put(KeyEvent.VK_B, 52);
        KEY_TO_NOTE.put(KeyEvent.VK_N, 53);
        KEY_TO_NOTE.put(KeyEvent.VK_M, 54);
        KEY_TO_NOTE.put(KeyEvent.VK_COMMA, 55);
        KEY_TO_NOTE.put(KeyEvent.VK_PERIOD, 56);
        KEY_TO_NOTE.put(KeyEvent.VK_SLASH, 57);
    }

    /** Active note state for OS auto-repeat deduplication (D-03). */
    private final boolean[] activeNotes = new boolean[128];

    public KeyboardMapper() {
        // activeNotes initialized to false by default
    }

    /**
     * Looks up the MIDI note for a KeyEvent keyCode.
     * @param keyCode the AWT key code (e.g., KeyEvent.VK_Q)
     * @return MIDI note number (0-127), or -1 if the key is not mapped
     */
    public static int keyCodeToNote(int keyCode) {
        Integer note = KEY_TO_NOTE.get(keyCode);
        return note != null ? note : -1;
    }

    /**
     * Checks whether a MIDI note is currently active (key is held down).
     */
    public boolean isActive(int note) {
        if (note < 0 || note >= 128) return false;
        return activeNotes[note];
    }

    /**
     * Sets the active state of a MIDI note.
     */
    public void setActive(int note, boolean active) {
        if (note < 0 || note >= 128) return;
        activeNotes[note] = active;
    }

    /**
     * Clears all active note states. Called on window focus loss.
     */
    public void clearAll() {
        for (int i = 0; i < 128; i++) {
            activeNotes[i] = false;
        }
    }
}
