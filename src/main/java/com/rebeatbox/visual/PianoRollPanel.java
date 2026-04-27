package com.rebeatbox.visual;

import com.rebeatbox.engine.PlaybackController;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.util.Collections;
import java.util.Comparator;
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

    /** GaussianBlur kernel size (odd number). */
    private static final int BLUR_KERNEL_SIZE = 5;

    /** GaussianBlur sigma value. */
    private static final float BLUR_SIGMA = 2.0f;

    /** Padding around each note bar BufferedImage to accommodate glow spread. */
    private static final int BLUR_PAD = 6;

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

    /** Phase accumulator for trigger line pulse animation (D-12). */
    float pulsePhase = 0.0f;

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

        // Layer 2: Note bars with GaussianBlur glow (D-07, D-08)
        drawNoteBars(g2d);

        // Layer 3: Foreground — mini keyboard, grid lines, trigger line
        drawMiniKeyboard(g2d);
        drawGridLines(g2d);
        drawTriggerLine(g2d);

        // Note: Animation loop added in Task 3
    }

    // --- Layer 2: Note bars ---

    /**
     * Renders all visible notes with per-note GaussianBlur glow (D-07, D-08).
     *
     * <p>Viewport culling (D-11): only notes within {@code [currentPos - PAST_MICROS,
     * currentPos + PREVIEW_MICROS]} are rendered. Uses binary search to find the
     * first visible note index.
     *
     * <p>Handles empty renderNotes gracefully (renders nothing).
     */
    private void drawNoteBars(Graphics2D g2d) {
        if (renderNotes.isEmpty()) return;

        long pos = currentPositionMicros;
        long windowStart = pos - PAST_MICROS;
        long windowEnd = pos + PREVIEW_MICROS;

        int tly = triggerLineY();

        // Binary search for first visible note (D-10, D-11)
        int firstIdx = binarySearchFirstVisible(windowStart);
        if (firstIdx < 0) return;

        for (int i = firstIdx; i < renderNotes.size(); i++) {
            RenderNote note = renderNotes.get(i);
            if (note.startMicros() > windowEnd) break;
            // Skip notes whose endMicros is entirely before the window
            if (note.endMicros() < windowStart) continue;

            drawSingleNote(g2d, note, pos, tly);
        }
    }

    /**
     * Finds the index of the first RenderNote with {@code startMicros >= windowStart}
     * using {@link Collections#binarySearch} with a sentinel note.
     *
     * @param windowStart lower bound of the visible time window
     * @return index of the first visible note, or -1 if no notes are in range
     */
    private int binarySearchFirstVisible(long windowStart) {
        // Create a sentinel RenderNote at windowStart for binary search
        RenderNote sentinel = new RenderNote(60, windowStart, windowStart + 1, 64, 0);
        int idx = Collections.binarySearch(renderNotes, sentinel,
            Comparator.comparingLong(RenderNote::startMicros));

        if (idx < 0) {
            idx = -(idx + 1); // Convert to insertion point
        } else {
            // Exact match found — walk backward to the first occurrence
            while (idx > 0 && renderNotes.get(idx - 1).startMicros() == windowStart) {
                idx--;
            }
        }

        if (idx >= renderNotes.size()) return -1;
        return idx;
    }

    /**
     * Renders a single note bar with GaussianBlur glow onto the canvas.
     *
     * <p>Per D-07 and D-08: each note gets its own BufferedImage, ConvolveOp GaussianBlur,
     * then alpha-composited onto the main Graphics2D. Notes below the trigger line
     * render at 40% opacity (D-06).
     *
     * <p>Per D-04: bar width is one column per semitone, chords naturally render as
     * adjacent bars at the same Y position.
     */
    private void drawSingleNote(Graphics2D g2d, RenderNote note, long currentPos, int tly) {
        // Compute bar geometry (D-04)
        double barX = pitchToX(note.pitch());
        double nextPitchX = pitchToX(note.pitch() + 1);
        int barWidth = (int) (nextPitchX - barX) - COLUMN_GAP;
        if (barWidth <= 0) return;

        int barY = timeToY(note.startMicros(), currentPos);
        int barBottomY = timeToY(note.endMicros(), currentPos);
        int barHeight = Math.max(barBottomY - barY, MIN_BAR_HEIGHT);

        // Clamp to panel bounds
        if (barY > getHeight() || barBottomY < 0) return;

        // Determine if note crosses the trigger line (D-06)
        Color baseColor = NoteColorMapper.forPitch(note.pitch());
        boolean spansTrigger = barY < tly && barBottomY > tly;

        if (spansTrigger) {
            // Split into two segments: above and below trigger line
            int aboveHeight = tly - barY;
            if (aboveHeight >= MIN_BAR_HEIGHT) {
                drawGlowingBar(g2d, (int) barX, barY, barWidth, aboveHeight, baseColor, 1.0f);
            }
            int belowY = tly;
            int belowHeight = barBottomY - tly;
            if (belowHeight >= MIN_BAR_HEIGHT) {
                drawGlowingBar(g2d, (int) barX, belowY, barWidth, belowHeight, baseColor, 0.4f);
            }
        } else {
            float alpha = (barY >= tly) ? 0.4f : 1.0f; // below trigger = dimmed (D-06)
            drawGlowingBar(g2d, (int) barX, barY, barWidth, barHeight, baseColor, alpha);
        }
    }

    /**
     * Creates a BufferedImage for a single note bar, applies separable GaussianBlur
     * via two-pass ConvolveOp, then alpha-composites the result onto the target Graphics2D.
     *
     * <p>The per-note BufferedImage is padded by {@link #BLUR_PAD} pixels on each side
     * to accommodate glow spread from the convolution kernel.
     *
     * @param g2d    target graphics context
     * @param x      bar left edge
     * @param y      bar top edge
     * @param width  bar width in pixels
     * @param height bar height in pixels
     * @param color  base fill color for the note
     * @param alpha  compositing alpha (1.0 = full brightness, 0.4 = dimmed per D-06)
     */
    private void drawGlowingBar(Graphics2D g2d, int x, int y, int width, int height,
                                Color color, float alpha) {
        width = Math.max(1, width);
        height = Math.max(1, height);

        int imgW = width + BLUR_PAD * 2;
        int imgH = height + BLUR_PAD * 2;

        BufferedImage barImg = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D barG = barImg.createGraphics();

        barG.setColor(color);
        barG.fillRect(BLUR_PAD, BLUR_PAD, width, height);
        barG.dispose();

        // Apply separable GaussianBlur (horizontal + vertical pass)
        float[] kernel = buildGaussianKernel(BLUR_KERNEL_SIZE, BLUR_SIGMA);
        BufferedImage blurred = applyConvolveBlur(barImg, kernel);

        // Alpha-composite the blurred note onto the main canvas
        Composite origComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(blurred, x - BLUR_PAD, y - BLUR_PAD, null);
        g2d.setComposite(origComposite);
    }

    /**
     * Builds a normalized 1D Gaussian kernel.
     *
     * <p>Formula: G(x) = (1 / (sigma * sqrt(2*pi))) * e^(-x^2 / (2*sigma^2)).
     *
     * @param size  kernel size (odd number)
     * @param sigma standard deviation
     * @return normalized 1D kernel array
     */
    private static float[] buildGaussianKernel(int size, float sigma) {
        float[] kernel = new float[size];
        int half = size / 2;
        float sum = 0f;
        for (int i = 0; i < size; i++) {
            int x = i - half;
            kernel[i] = (float) (Math.exp(-(x * x) / (2.0 * sigma * sigma))
                    / (sigma * Math.sqrt(2.0 * Math.PI)));
            sum += kernel[i];
        }
        // Normalize
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    /**
     * Applies a Gaussian blur via two-pass separable ConvolveOp
     * (horizontal kernel then vertical kernel).
     *
     * @param source source image
     * @param kernel 1D normalized Gaussian kernel
     * @return blurred image
     */
    private static BufferedImage applyConvolveBlur(BufferedImage source, float[] kernel) {
        // Horizontal pass
        float[] hData = kernel.clone();
        java.awt.image.Kernel hk = new java.awt.image.Kernel(kernel.length, 1, hData);
        ConvolveOp hOp = new ConvolveOp(hk, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage hBlurred = hOp.filter(source, null);

        // Vertical pass
        float[] vData = new float[kernel.length];
        System.arraycopy(kernel, 0, vData, 0, kernel.length);
        java.awt.image.Kernel vk = new java.awt.image.Kernel(1, kernel.length, vData);
        ConvolveOp vOp = new ConvolveOp(vk, ConvolveOp.EDGE_NO_OP, null);
        return vOp.filter(hBlurred, null);
    }

    // --- Layer 3: Mini keyboard ---

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

    // --- Layer 3: Grid lines ---

    /**
     * Draws subtle horizontal grid lines at one-beat intervals for orientation.
     * Near-invisible white lines (alpha=15) that do not violate D-07's pure black
     * aesthetic.
     */
    private void drawGridLines(Graphics2D g2d) {
        if (controller == null) return;

        // Get BPM for beat interval calculation
        int bpm = 120; // default
        try {
            bpm = (int) Math.round(controller.getSequencer().getTempoInBPM());
        } catch (Exception ignored) {
            // Fall back to default BPM
        }
        if (bpm <= 0) bpm = 120;

        long beatIntervalMicros = 60_000_000L / bpm; // microseconds per beat
        if (beatIntervalMicros <= 0) return;

        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2d.setColor(new Color(255, 255, 255, 15)); // near-invisible white

        long pos = currentPositionMicros;
        long windowStart = pos - PAST_MICROS;
        long windowEnd = pos + PREVIEW_MICROS;

        // Align to beat boundaries
        long firstBeat = ((windowStart / beatIntervalMicros) - 1) * beatIntervalMicros;
        if (firstBeat < 0) firstBeat = 0;

        int waterfallLeft = KEYBOARD_WIDTH;
        int waterfallRight = getWidth();

        for (long beatTime = firstBeat; beatTime <= windowEnd; beatTime += beatIntervalMicros) {
            int y = timeToY(beatTime, pos);
            if (y >= 0 && y < getHeight()) {
                g2d.drawLine(waterfallLeft, y, waterfallRight, y);
            }
        }
    }

    // --- Layer 3: Trigger line ---

    /**
     * Draws the pulsing neon trigger line (D-12).
     *
     * <p>Two-pass rendering: a wide translucent cyan halo (8px stroke), then a
     * 2px bright cyan/white core line. The core line opacity pulses via a sine-wave
     * between 0.80 and 1.00 for a heartbeat feel.
     *
     * <p>For Task 2, uses a constant pulseAlpha of 1.0f. Pulse animation is wired
     * in Task 3 when the Timer updates pulsePhase.
     */
    private void drawTriggerLine(Graphics2D g2d) {
        int ty = triggerLineY();
        int waterfallLeft = KEYBOARD_WIDTH;
        int waterfallRight = getWidth();

        // Compute pulse alpha (constant for now; animated in Task 3)
        float pulseAlpha = 0.80f + 0.20f * (float) ((Math.sin(pulsePhase) + 1.0) / 2.0);

        // Enable antialiasing for smooth line rendering
        Object origAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Outer glow halo
        g2d.setStroke(new BasicStroke(8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(0, 255, 255, (int) (60 * pulseAlpha))); // cyan, low alpha
        g2d.drawLine(waterfallLeft, ty, waterfallRight, ty);

        // Core line
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int coreAlpha = (int) (255 * pulseAlpha);
        g2d.setColor(new Color(200, 255, 255, coreAlpha));
        g2d.drawLine(waterfallLeft, ty, waterfallRight, ty);

        // Restore original rendering hint
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, origAntialias);
    }
}
