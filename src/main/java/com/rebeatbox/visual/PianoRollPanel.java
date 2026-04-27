package com.rebeatbox.visual;

import com.rebeatbox.engine.PlaybackController;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Synthesia-style piano roll visualization panel.
 *
 * <p>Three-layer compositing architecture (D-08):
 * <ol>
 *   <li>Pure black background canvas</li>
 *   <li>Per-note BufferedImage with GaussianBlur glow, alpha-composited</li>
 *   <li>Foreground: trigger line, mini keyboard, subtle grid lines</li>
 * </ol>
 *
 * <p>Coordinate system:
 * <ul>
 *   <li>Left zone [0, KEYBOARD_WIDTH): mini keyboard silhouette</li>
 *   <li>Right zone [KEYBOARD_WIDTH, width): note waterfall</li>
 *   <li>Vertical: time-to-Y mapping, trigger line at ~18% from bottom (D-03)</li>
 *   <li>Horizontal: pitch-to-X mapping, one column per semitone (D-04)</li>
 * </ul>
 */
public class PianoRollPanel extends JPanel {

    // --- Configuration constants ---

    /** Width of the mini keyboard on the left side (D-02: 40-60px). */
    static final int KEYBOARD_WIDTH = 48;

    /** Trigger line position as fraction from bottom (D-03: 15-20%). */
    private static final double TRIGGER_LINE_RATIO = 0.18;

    /** Visible time window ahead of current position, in microseconds (D-11). */
    static final long PREVIEW_MICROS = 2_000_000L;

    /** Visible time window behind current position, in microseconds (D-11). */
    static final long PAST_MICROS = 2_000_000L;

    /** Minimum bar height in pixels so very short notes remain visible. */
    static final int MIN_BAR_HEIGHT = 2;

    /** Gap between adjacent note columns in pixels. */
    static final int COLUMN_GAP = 1;

    // White key MIDI note offsets within an octave
    private static final int[] WHITE_KEY_OFFSETS = {0, 2, 4, 5, 7, 9, 11};
    // Black key MIDI note offsets within an octave
    private static final int[] BLACK_KEY_OFFSETS = {1, 3, 6, 8, 10};

    // --- Fields ---

    /** Reference to the playback controller; set via {@link #setController(PlaybackController)}. */
    PlaybackController controller;

    /** Pre-scanned note list, sorted by startMicros ascending; populated by {@link #onFileLoaded()}. */
    List<RenderNote> renderNotes = Collections.emptyList();

    /** Low end of the adaptive note range (inclusive). Default: A0 (21). */
    int noteRangeMin = 21;

    /** High end of the adaptive note range (inclusive). Default: C8 (108). */
    int noteRangeMax = 108;

    /** Current playback position in microseconds, updated each timer tick (Task 3). */
    long currentPositionMicros = 0;

    // --- Constructor ---

    /**
     * Creates the panel with a pure black background and double-buffering enabled.
     * The controller is wired later via {@link #setController(PlaybackController)}.
     */
    public PianoRollPanel() {
        setBackground(Color.BLACK);  // D-07
        setDoubleBuffered(true);
    }

    // --- Public API ---

    /**
     * Stores the playback controller reference.
     * The animation loop is started in Task 3.
     *
     * @param controller the playback controller; must not be null
     */
    public void setController(PlaybackController controller) {
        this.controller = controller;
    }

    // --- Coordinate helpers ---

    /**
     * Returns the Y coordinate of the trigger line (18% from bottom per D-03).
     */
    int triggerLineY() {
        return (int) (getHeight() * (1.0 - TRIGGER_LINE_RATIO));
    }

    /**
     * Maps a MIDI pitch to its X coordinate within the waterfall zone.
     * The pitch grid auto-scales to the adaptive note range with 1-semitone padding.
     *
     * @param pitch MIDI note number (0-127)
     * @return X coordinate of the left edge of this pitch's column
     */
    double pitchToX(int pitch) {
        int effectiveMin = Math.max(0, noteRangeMin);
        int effectiveMax = Math.min(127, noteRangeMax);
        int numColumns = effectiveMax - effectiveMin + 1;
        if (numColumns <= 0) return KEYBOARD_WIDTH;

        double colWidth = (double) (getWidth() - KEYBOARD_WIDTH) / numColumns;
        return KEYBOARD_WIDTH + (pitch - effectiveMin) * colWidth;
    }

    /**
     * Maps a note time to a Y coordinate relative to the current playback position.
     *
     * @param noteTimeMicros   the note's absolute time in microseconds
     * @param currentPosMicros the current playback position in microseconds
     * @return Y coordinate on screen
     */
    int timeToY(long noteTimeMicros, long currentPosMicros) {
        long delta = noteTimeMicros - currentPosMicros; // positive = future, negative = past
        int tly = triggerLineY();

        if (delta >= 0) {
            // Future note: map to area above trigger line
            if (PREVIEW_MICROS <= 0) return tly;
            double pixelsPerMicroAbove = (double) tly / PREVIEW_MICROS;
            return tly - (int) (delta * pixelsPerMicroAbove);
        } else {
            // Past note: map to area below trigger line
            if (PAST_MICROS <= 0) return tly;
            double pixelsPerMicroBelow = (double) (getHeight() - tly) / PAST_MICROS;
            return tly + (int) (-delta * pixelsPerMicroBelow);
        }
    }

    // --- Painting ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Layer 1: Pure black background (D-07, D-08)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, w, h);

        // Layer 3: Mini keyboard (left side reference)
        drawMiniKeyboard(g2d);

        // Note: Layer 2 (note bars) and trigger line added in Task 2
        // Note: Animation loop added in Task 3
    }

    // --- Mini keyboard ---

    /**
     * Draws the mini keyboard silhouette on the left side of the panel (D-02).
     *
     * <p>Only renders keys within the adaptive note range. White keys are drawn first,
     * then black keys on top. All keys span the full height of the panel.
     */
    private void drawMiniKeyboard(Graphics2D g2d) {
        int startPitch = Math.max(0, noteRangeMin);
        int endPitch = Math.min(127, noteRangeMax);
        int numKeys = endPitch - startPitch + 1;
        if (numKeys <= 0) return;

        double keyHeight = getHeight() / (double) numKeys;

        // Draw white keys first
        for (int pitch = startPitch; pitch <= endPitch; pitch++) {
            if (!isWhiteKey(pitch)) continue;

            int keyIndex = pitch - startPitch;
            int keyY = (int) (keyIndex * keyHeight);
            int keyH = (int) ((keyIndex + 1) * keyHeight) - keyY;
            if (keyH <= 0) continue;

            g2d.setColor(new Color(0x2a2a2a));
            g2d.fillRect(0, keyY, KEYBOARD_WIDTH - 1, keyH);

            // Subtle border on white keys
            g2d.setColor(new Color(0x3a3a3a));
            g2d.drawRect(0, keyY, KEYBOARD_WIDTH - 1, keyH);
        }

        // Draw black keys on top (narrower silhouette)
        int blackKeyWidth = (int) (KEYBOARD_WIDTH * 0.6);
        int blackKeyX = (KEYBOARD_WIDTH - blackKeyWidth) / 2;
        int blackKeyHeight = (int) (keyHeight * 0.6);

        for (int pitch = startPitch; pitch <= endPitch; pitch++) {
            if (!isBlackKey(pitch)) continue;

            int keyIndex = pitch - startPitch;
            // Black key centered on the pitch boundary
            int keyCenterY = (int) ((keyIndex + 0.5) * keyHeight);
            int keyY = keyCenterY - blackKeyHeight / 2;

            if (keyY < 0) keyY = 0;
            if (keyY + blackKeyHeight > getHeight()) {
                blackKeyHeight = getHeight() - keyY;
            }
            if (blackKeyHeight <= 0) continue;

            g2d.setColor(new Color(0x1a1a1a));
            g2d.fillRect(blackKeyX, keyY, blackKeyWidth, blackKeyHeight);
        }

        // Draw octave separator lines at C notes (pitch % 12 == 0)
        drawOctaveSeparators(g2d, startPitch, endPitch, keyHeight);
    }

    /**
     * Draws subtle horizontal separator lines at octave boundaries (C notes)
     * that visually tie the keyboard to the waterfall view.
     */
    private void drawOctaveSeparators(Graphics2D g2d, int startPitch, int endPitch, double keyHeight) {
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(0x50, 0x50, 0x50, 120));
        for (int pitch = startPitch; pitch <= endPitch; pitch++) {
            if (pitch % 12 != 0) continue; // C note
            int keyIndex = pitch - startPitch;
            int y = (int) (keyIndex * keyHeight);
            if (y > 0 && y < getHeight()) {
                g2d.drawLine(0, y, KEYBOARD_WIDTH, y);
            }
        }
    }

    private static boolean isWhiteKey(int pitch) {
        int offset = pitch % 12;
        for (int wk : WHITE_KEY_OFFSETS) {
            if (wk == offset) return true;
        }
        return false;
    }

    private static boolean isBlackKey(int pitch) {
        int offset = pitch % 12;
        for (int bk : BLACK_KEY_OFFSETS) {
            if (bk == offset) return true;
        }
        return false;
    }
}
