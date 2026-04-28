package com.rebeatbox.live;

import com.rebeatbox.engine.RealtimeReceiver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PadButton extends JButton {

    private int midiNote;
    private String padLabel;
    private final String defaultLabel;
    private final int defaultMidiNote;
    private final RealtimeReceiver receiver;
    private final Timer holdTimer;
    private boolean pressedVisual = false;

    private static final int DRUM_CHANNEL = 10;
    private static final int VELOCITY = 100;

    private static final Color DEFAULT_FILL = new Color(0x1A1A2E);
    private static final Color DEFAULT_BORDER = new Color(0x2A3A5E);
    private static final Color HOVER_FILL = new Color(0x25304A);
    private static final Color HOVER_BORDER = new Color(0xE040FB);
    private static final Color PRESSED_FILL = new Color(0x003344);
    private static final Color PRESSED_BORDER = new Color(0x00E5FF);
    private static final Color DEFAULT_TEXT = new Color(0xCCCCCC);
    private static final Color HOVER_TEXT = new Color(0xE0E0E0);
    private static final Color PRESSED_TEXT = new Color(0x00E5FF);

    public PadButton(String label, int midiNote, RealtimeReceiver receiver) {
        super(label);
        this.padLabel = label;
        this.midiNote = midiNote;
        this.defaultLabel = label;
        this.defaultMidiNote = midiNote;
        this.receiver = receiver;

        setFocusable(false);
        setPreferredSize(new Dimension(48, 48));
        setFont(new Font("SansSerif", Font.PLAIN, 10));
        setForeground(DEFAULT_TEXT);
        setBackground(DEFAULT_FILL);
        setBorder(BorderFactory.createLineBorder(DEFAULT_BORDER, 1));
        setToolTipText(label + " (Note " + midiNote + ")");

        holdTimer = new Timer(200, e -> {
            pressedVisual = false;
            repaint();
        });
        holdTimer.setRepeats(false);

        setupMouseListener();
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    sendNoteOn();
                    pressedVisual = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    sendNoteOff();
                    holdTimer.restart();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!pressedVisual) repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!pressedVisual) repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showAssignMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void sendNoteOn() {
        try {
            receiver.noteOn(midiNote, VELOCITY, DRUM_CHANNEL);
        } catch (Exception e) {
            System.err.println("MIDI send failed: note=" + midiNote + " channel=" + DRUM_CHANNEL);
        }
    }

    private void sendNoteOff() {
        try {
            receiver.noteOff(midiNote, DRUM_CHANNEL);
        } catch (Exception e) {
            System.err.println("MIDI send failed: note=" + midiNote + " channel=" + DRUM_CHANNEL);
        }
    }

    public int getMidiNote() { return midiNote; }
    public String getPadLabel() { return padLabel; }

    public void setMidiNote(int midiNote, String label) {
        if (midiNote < 0 || midiNote > 127) return;
        this.midiNote = midiNote;
        this.padLabel = label;
        setText(label);
        setToolTipText(label + " (Note " + midiNote + ")");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        ButtonModel model = getModel();
        boolean hover = model.isRollover();
        boolean pressed = pressedVisual || model.isPressed();

        Color fillColor;
        if (pressed) {
            fillColor = PRESSED_FILL;
        } else if (hover) {
            fillColor = HOVER_FILL;
        } else {
            fillColor = DEFAULT_FILL;
        }
        g2d.setColor(fillColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        Color borderColor;
        int borderWidth;
        if (pressed) {
            borderColor = PRESSED_BORDER;
            borderWidth = 2;
        } else if (hover) {
            borderColor = HOVER_BORDER;
            borderWidth = 1;
        } else {
            borderColor = DEFAULT_BORDER;
            borderWidth = 1;
        }
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRect(borderWidth / 2, borderWidth / 2,
            getWidth() - borderWidth, getHeight() - borderWidth);

        if (pressed) setForeground(PRESSED_TEXT);
        else if (hover) setForeground(HOVER_TEXT);
        else setForeground(DEFAULT_TEXT);

        super.paintComponent(g);
    }

    private void showAssignMenu(Component invoker, int x, int y) {
        JPopupMenu menu = new JPopupMenu("Assign Sound");
        JMenu assignMenu = new JMenu("Assign Sound");

        JMenu kicksMenu = new JMenu("Kicks");
        kicksMenu.add(createAssignItem("Bass Drum 1", 36));
        kicksMenu.add(createAssignItem("Bass Drum 2", 35));
        assignMenu.add(kicksMenu);

        JMenu snaresMenu = new JMenu("Snares");
        snaresMenu.add(createAssignItem("Acoustic Snare", 38));
        snaresMenu.add(createAssignItem("Electric Snare", 40));
        snaresMenu.add(createAssignItem("Side Stick", 37));
        assignMenu.add(snaresMenu);

        JMenu hatsMenu = new JMenu("Hi-Hats");
        hatsMenu.add(createAssignItem("Closed Hi-Hat", 42));
        hatsMenu.add(createAssignItem("Open Hi-Hat", 46));
        hatsMenu.add(createAssignItem("Pedal Hi-Hat", 44));
        assignMenu.add(hatsMenu);

        JMenu cymbalsMenu = new JMenu("Cymbals");
        cymbalsMenu.add(createAssignItem("Crash Cymbal 1", 49));
        cymbalsMenu.add(createAssignItem("Crash Cymbal 2", 57));
        cymbalsMenu.add(createAssignItem("Ride Cymbal 1", 51));
        cymbalsMenu.add(createAssignItem("Ride Cymbal 2", 52));
        cymbalsMenu.add(createAssignItem("Splash Cymbal", 55));
        assignMenu.add(cymbalsMenu);

        JMenu tomsMenu = new JMenu("Toms");
        tomsMenu.add(createAssignItem("Hi-Mid Tom", 48));
        tomsMenu.add(createAssignItem("Low Tom", 45));
        tomsMenu.add(createAssignItem("Low Floor Tom", 41));
        tomsMenu.add(createAssignItem("High Tom", 50));
        tomsMenu.add(createAssignItem("High Floor Tom", 43));
        assignMenu.add(tomsMenu);

        JMenu percMenu = new JMenu("Percussion");
        percMenu.add(createAssignItem("Hand Clap", 39));
        percMenu.add(createAssignItem("Cowbell", 56));
        percMenu.add(createAssignItem("Claves", 75));
        percMenu.add(createAssignItem("Maracas", 70));
        percMenu.add(createAssignItem("Cabasa", 69));
        percMenu.add(createAssignItem("Triangle", 81));
        percMenu.add(createAssignItem("Tambourine", 54));
        percMenu.add(createAssignItem("Wood Block", 76));
        percMenu.add(createAssignItem("Vibraslap", 58));
        assignMenu.add(percMenu);

        menu.add(assignMenu);
        menu.addSeparator();

        JMenuItem resetItem = new JMenuItem("Reset to \"" + defaultLabel + "\"");
        resetItem.setForeground(new Color(0xFF4444));
        resetItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Reset pad '" + padLabel + "' to default sound '" + defaultLabel + "'?",
                "Reset Pad", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                setMidiNote(defaultMidiNote, defaultLabel);
            }
        });
        menu.add(resetItem);

        menu.show(invoker, x, y);
    }

    private JMenuItem createAssignItem(String name, int note) {
        JMenuItem item = new JMenuItem(name + " (" + note + ")");
        item.addActionListener(e -> setMidiNote(note, name));
        return item;
    }
}
