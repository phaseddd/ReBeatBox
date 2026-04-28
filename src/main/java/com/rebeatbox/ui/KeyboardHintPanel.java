package com.rebeatbox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Three-row virtual piano keyboard displaying the D-01 keyboard-to-MIDI mapping.
 * Keys highlight in neon cyan when pressed via {@link #setKeyHighlighted(int, boolean)}.
 *
 * <p>Uses custom paintComponent — no JButton subcomponents.
 */
public class KeyboardHintPanel extends JPanel {

    private static final int ROW_COUNT = 3;
    private static final int ROW_HEIGHT = 28;
    private static final int PANEL_HEIGHT = ROW_COUNT * ROW_HEIGHT;

    private static final double BLACK_KEY_WIDTH_RATIO = 0.60;
    private static final double BLACK_KEY_HEIGHT_RATIO = 0.70;

    private static final Color BG_COLOR = new Color(0x0a0a14);

    private static final Color WHITE_IDLE_FILL = new Color(0x2A2A2A);
    private static final Color WHITE_IDLE_BORDER = new Color(0x3A3A3A);
    private static final Color WHITE_IDLE_LABEL = new Color(0x808080);

    private static final Color BLACK_IDLE_FILL = new Color(0x1A1A1A);
    private static final Color BLACK_IDLE_BORDER = new Color(0x2A2A2A);
    private static final Color BLACK_IDLE_LABEL = new Color(0x606060);

    private static final Color PRESSED_BORDER = new Color(0x00E5FF);
    private static final Color PRESSED_LABEL = new Color(0x00E5FF);
    private static final Color WHITE_PRESSED_FILL = new Color(0, 229, 255, 64);
    private static final Color BLACK_PRESSED_FILL = new Color(0, 229, 255, 51);

    private static final Font KEY_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font NOTE_LABEL_FONT = new Font("Monospaced", Font.PLAIN, 9);

    private final Map<Integer, Boolean> keyHighlights = new HashMap<>();

    private static record KeyDef(int keyCode, boolean isBlack, String keyLabel, String noteName, int midiNote) {}

    private static final KeyDef[] ROW_0_KEYS = {
        new KeyDef(KeyEvent.VK_1, false, "1", "C5", 72),
        new KeyDef(KeyEvent.VK_2, true, "2", "C#5", 73),
        new KeyDef(KeyEvent.VK_3, false, "3", "D5", 74),
        new KeyDef(KeyEvent.VK_4, true, "4", "D#5", 75),
        new KeyDef(KeyEvent.VK_5, false, "5", "E5", 76),
        new KeyDef(KeyEvent.VK_6, false, "6", "F5", 77),
        new KeyDef(KeyEvent.VK_7, true, "7", "F#5", 78),
        new KeyDef(KeyEvent.VK_8, false, "8", "G5", 79),
        new KeyDef(KeyEvent.VK_9, true, "9", "G#5", 80),
        new KeyDef(KeyEvent.VK_0, false, "0", "A5", 81),
        new KeyDef(KeyEvent.VK_MINUS, true, "-", "A#5", 82),
        new KeyDef(KeyEvent.VK_EQUALS, false, "=", "B5", 83),
    };

    private static final KeyDef[] ROW_1_KEYS = {
        new KeyDef(KeyEvent.VK_Q, false, "Q", "C4", 60),
        new KeyDef(KeyEvent.VK_W, true, "W", "C#4", 61),
        new KeyDef(KeyEvent.VK_E, false, "E", "D4", 62),
        new KeyDef(KeyEvent.VK_R, true, "R", "D#4", 63),
        new KeyDef(KeyEvent.VK_T, false, "T", "E4", 64),
        new KeyDef(KeyEvent.VK_Y, false, "Y", "F4", 65),
        new KeyDef(KeyEvent.VK_U, true, "U", "F#4", 66),
        new KeyDef(KeyEvent.VK_I, false, "I", "G4", 67),
        new KeyDef(KeyEvent.VK_O, true, "O", "G#4", 68),
        new KeyDef(KeyEvent.VK_P, false, "P", "A4", 69),
        new KeyDef(KeyEvent.VK_OPEN_BRACKET, true, "[", "A#4", 70),
        new KeyDef(KeyEvent.VK_CLOSE_BRACKET, false, "]", "B4", 71),
    };

    private static final KeyDef[] ROW_2_KEYS = {
        new KeyDef(KeyEvent.VK_Z, false, "Z", "C3", 48),
        new KeyDef(KeyEvent.VK_X, true, "X", "C#3", 49),
        new KeyDef(KeyEvent.VK_C, false, "C", "D3", 50),
        new KeyDef(KeyEvent.VK_V, true, "V", "D#3", 51),
        new KeyDef(KeyEvent.VK_B, false, "B", "E3", 52),
        new KeyDef(KeyEvent.VK_N, false, "N", "F3", 53),
        new KeyDef(KeyEvent.VK_M, true, "M", "F#3", 54),
        new KeyDef(KeyEvent.VK_COMMA, false, ",", "G3", 55),
        new KeyDef(KeyEvent.VK_PERIOD, true, ".", "G#3", 56),
        new KeyDef(KeyEvent.VK_SLASH, false, "/", "A3", 57),
    };

    private static final KeyDef[][] ALL_ROWS = { ROW_0_KEYS, ROW_1_KEYS, ROW_2_KEYS };

    public KeyboardHintPanel() {
        setBackground(BG_COLOR);
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(800, PANEL_HEIGHT));
        setToolTipText("");
    }

    public void setKeyHighlighted(int keyCode, boolean highlighted) {
        boolean old = keyHighlights.getOrDefault(keyCode, false);
        if (highlighted != old) {
            keyHighlights.put(keyCode, highlighted);
            repaint();
        }
    }

    public boolean isKeyHighlighted(int keyCode) {
        return keyHighlights.getOrDefault(keyCode, false);
    }

    public void clearAllHighlights() {
        if (!keyHighlights.isEmpty()) {
            keyHighlights.clear();
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, w, h);

        for (int row = 0; row < ROW_COUNT; row++) {
            drawKeyRow(g2d, row);
        }
    }

    private void drawKeyRow(Graphics2D g2d, int rowIndex) {
        KeyDef[] keys = ALL_ROWS[rowIndex];
        if (keys.length == 0) return;

        int w = getWidth();
        int rowY = rowIndex * ROW_HEIGHT;

        int whiteKeyCount = 0;
        for (KeyDef key : keys) {
            if (!key.isBlack) whiteKeyCount++;
        }
        if (whiteKeyCount == 0) return;
        int whiteKeyWidth = w / whiteKeyCount;

        // Pass 1: white keys
        int whiteIdx = 0;
        for (KeyDef key : keys) {
            if (key.isBlack) continue;
            int keyX = whiteIdx * whiteKeyWidth;
            int keyW = whiteKeyWidth - 1;
            drawSingleKey(g2d, key, keyX, rowY, keyW, ROW_HEIGHT);
            whiteIdx++;
        }

        // Pass 2: black keys on top
        int blackKeyWidth = (int) (whiteKeyWidth * BLACK_KEY_WIDTH_RATIO);
        int blackKeyHeight = (int) (ROW_HEIGHT * BLACK_KEY_HEIGHT_RATIO);
        int blackKeyY = rowY;

        whiteIdx = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].isBlack) {
                int keyX = (whiteIdx * whiteKeyWidth) - (blackKeyWidth / 2);
                drawSingleKey(g2d, keys[i], keyX, blackKeyY, blackKeyWidth, blackKeyHeight);
            } else {
                whiteIdx++;
            }
        }
    }

    private void drawSingleKey(Graphics2D g2d, KeyDef key, int x, int y, int width, int height) {
        boolean pressed = isKeyHighlighted(key.keyCode);

        Color fillColor, borderColor, labelColor;
        int borderWidth;

        if (key.isBlack) {
            if (pressed) {
                fillColor = BLACK_PRESSED_FILL;
                borderColor = PRESSED_BORDER;
                labelColor = PRESSED_LABEL;
                borderWidth = 2;
            } else {
                fillColor = BLACK_IDLE_FILL;
                borderColor = BLACK_IDLE_BORDER;
                labelColor = BLACK_IDLE_LABEL;
                borderWidth = 1;
            }
        } else {
            if (pressed) {
                fillColor = WHITE_PRESSED_FILL;
                borderColor = PRESSED_BORDER;
                labelColor = PRESSED_LABEL;
                borderWidth = 2;
            } else {
                fillColor = WHITE_IDLE_FILL;
                borderColor = WHITE_IDLE_BORDER;
                labelColor = WHITE_IDLE_LABEL;
                borderWidth = 1;
            }
        }

        g2d.setColor(fillColor);
        g2d.fillRect(x, y, width, height);

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRect(x, y, width, height);

        if (height >= 10 && width >= 8) {
            g2d.setFont(KEY_LABEL_FONT);
            g2d.setColor(labelColor);
            FontMetrics fm = g2d.getFontMetrics();
            String label = key.keyLabel;
            int labelX = x + (width - fm.stringWidth(label)) / 2;
            int labelY = y + fm.getAscent() + 1;
            g2d.drawString(label, labelX, labelY);
        }
    }
}
