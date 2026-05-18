package com.rebeatbox.live;

import com.rebeatbox.engine.RealtimeReceiver;
import com.rebeatbox.ui.ThemeManager;
import org.pushingpixels.radiance.animation.api.Timeline;
import org.pushingpixels.radiance.animation.api.Timeline.TimelineState;
import org.pushingpixels.radiance.animation.api.callback.TimelineCallback;
import org.pushingpixels.radiance.animation.api.ease.Spline;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class PadButton extends JButton {

    private int midiNote;
    private String padLabel;
    private final String defaultLabel;
    private final int defaultMidiNote;
    private final RealtimeReceiver receiver;

    private static final int DRUM_CHANNEL = 10;
    private static final int VELOCITY = 100;

    // State tracking for animation transitions
    private enum State { IDLE, HOVER, PRESSED }
    private State state = State.IDLE;

    /**
     * Linearly interpolates between two Colors for smooth Timeline transitions.
     * Clamps t to [0,1]. Used by all animation callbacks.
     */
    private static Color interpolateColor(Color a, Color b, float t) {
        float ti = Math.max(0.0f, Math.min(1.0f, t));
        int r = (int)(a.getRed()   + ti * (b.getRed()   - a.getRed()));
        int g = (int)(a.getGreen() + ti * (b.getGreen() - a.getGreen()));
        int bl = (int)(a.getBlue() + ti * (b.getBlue() - a.getBlue()));
        int alpha = (int)(a.getAlpha() + ti * (b.getAlpha() - a.getAlpha()));
        return new Color(Math.min(255, Math.max(0, r)),
                         Math.min(255, Math.max(0, g)),
                         Math.min(255, Math.max(0, bl)),
                         Math.min(255, Math.max(0, alpha)));
    }

    // Color state derived from ThemeManager (D-01, D-02) — these are the TARGET colors
    // that Timeline animations interpolate toward
    private static final Color DEFAULT_FILL   = ThemeManager.BG_ELEVATED;
    private static final Color DEFAULT_BORDER = ThemeManager.BORDER_IDLE;
    private static final Color HOVER_FILL     = new Color(0x25304A); // slightly lighter elevated — drum-pad-specific
    private static final Color HOVER_BORDER   = ThemeManager.accentForHue(ThemeManager.HUE_DRUM_PADS);
    private static final Color PRESSED_FILL   = new Color(0x003344); // dark cyan for contrast — drum-pad-specific per UI-SPEC
    private static final Color PRESSED_BORDER = ThemeManager.TEXT_ACCENT;
    private static final Color DEFAULT_TEXT   = new Color(0xCCCCCC); // dimmer than PRIMARY for idle — drum-pad-specific
    private static final Color HOVER_TEXT     = ThemeManager.TEXT_PRIMARY;
    private static final Color PRESSED_TEXT   = ThemeManager.TEXT_ACCENT;

    // Timeline-driven animated properties — rendered by paintComponent for smooth transitions
    private Color animatedBorderColor;  // current border color (Timeline interpolated toward target)
    private Color animatedFillColor;    // current fill color (Timeline interpolated toward target)
    private float animatedScale = 1.0f; // press scale bounce (1.00 normal, 0.95 pressed)

    // Active Timeline instances — aborted on new transitions to prevent EDT congestion
    private Timeline hoverTimeline;
    private Timeline pressTimeline;

    public PadButton(String label, int midiNote, RealtimeReceiver receiver) {
        super(label);
        this.padLabel = label;
        this.midiNote = midiNote;
        this.defaultLabel = label;
        this.defaultMidiNote = midiNote;
        this.receiver = receiver;

        this.animatedBorderColor = DEFAULT_BORDER;
        this.animatedFillColor = DEFAULT_FILL;

        setFocusable(false);
        setPreferredSize(new Dimension(48, 48));
        setFont(new Font("SansSerif", Font.PLAIN, 10));
        setForeground(DEFAULT_TEXT);
        setBackground(DEFAULT_FILL);
        setBorder(BorderFactory.createLineBorder(DEFAULT_BORDER, 1));
        setToolTipText(label + " (Note " + midiNote + ")");

        setupMouseListener();
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                state = State.HOVER;
                animateBorderTo(HOVER_BORDER, 200);  // D-05: 200ms hover border in
                animateFillTo(HOVER_FILL, 200);      // Fill transitions too for cohesion
            }

            @Override
            public void mouseExited(MouseEvent e) {
                state = State.IDLE;
                animateBorderTo(DEFAULT_BORDER, 250);  // D-05: 250ms release
                animateFillTo(DEFAULT_FILL, 250);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                state = State.PRESSED;
                sendNoteOn();
                animateFillTo(PRESSED_FILL, 75);      // D-05: 75ms press flash
                animateBorderTo(PRESSED_BORDER, 75);
                animatePress();                        // D-05: 0.95x scale bounce down
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                sendNoteOff();
                // Determine target state: HOVER if mouse still over button, else IDLE
                state = contains(e.getPoint()) ? State.HOVER : State.IDLE;
                Color targetBorder = (state == State.HOVER) ? HOVER_BORDER : DEFAULT_BORDER;
                Color targetFill   = (state == State.HOVER) ? HOVER_FILL   : DEFAULT_FILL;
                animateFillTo(targetFill, 150);        // D-05: 150ms spring-back
                animateBorderTo(targetBorder, 150);
                animateRelease();                      // D-05: scale springs back to 1.00x
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

    /** Animates border color toward target. 200ms hover-in, 250ms hover-out. */
    private void animateBorderTo(Color target, int durationMs) {
        if (hoverTimeline != null) hoverTimeline.abort();
        final Color from = this.animatedBorderColor;
        hoverTimeline = Timeline.builder(this)
            .setDuration(durationMs)
            .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
            .addCallback(new TimelineCallback() {
                @Override public void onTimelinePulse(float df, float tp) {
                    animatedBorderColor = interpolateColor(from, target, tp);
                    repaint();
                }
                @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {
                    if (n == TimelineState.DONE) { animatedBorderColor = target; repaint(); }
                }
            })
            .build();
        hoverTimeline.play();
    }

    /** Animates fill color toward target. 75ms press-down, 150ms release-up. */
    private void animateFillTo(Color target, int durationMs) {
        final Color from = this.animatedFillColor;
        Timeline fillTimeline = Timeline.builder(this)
            .setDuration(durationMs)
            .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
            .addCallback(new TimelineCallback() {
                @Override public void onTimelinePulse(float df, float tp) {
                    animatedFillColor = interpolateColor(from, target, tp);
                    repaint();
                }
                @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {
                    if (n == TimelineState.DONE) { animatedFillColor = target; repaint(); }
                }
            })
            .build();
        fillTimeline.play();
    }

    /** Animates scale down to 0.95x over 75ms (press). */
    private void animatePress() {
        if (pressTimeline != null) pressTimeline.abort();
        pressTimeline = Timeline.builder(this)
            .setDuration(75)
            .addCallback(new TimelineCallback() {
                @Override public void onTimelinePulse(float df, float tp) {
                    animatedScale = 1.00f + tp * (0.95f - 1.00f);
                    repaint();
                }
                @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {
                    if (n == TimelineState.DONE) { animatedScale = 0.95f; repaint(); }
                }
            })
            .build();
        pressTimeline.play();
    }

    /** Animates scale back to 1.00x over 150ms (release spring-back). */
    private void animateRelease() {
        if (pressTimeline != null) pressTimeline.abort();
        pressTimeline = Timeline.builder(this)
            .setDuration(150)
            .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
            .addCallback(new TimelineCallback() {
                @Override public void onTimelinePulse(float df, float tp) {
                    animatedScale = 0.95f + tp * (1.00f - 0.95f);
                    repaint();
                }
                @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {
                    if (n == TimelineState.DONE) { animatedScale = 1.00f; repaint(); }
                }
            })
            .build();
        pressTimeline.play();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply press scale transform centered on the pad
        AffineTransform originalTransform = g2d.getTransform();
        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        g2d.translate(cx, cy);
        g2d.scale(animatedScale, animatedScale);
        g2d.translate(-cx, -cy);

        // Draw pad body with animated colors
        g2d.setColor(animatedFillColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(animatedBorderColor);
        int borderWidth = (state == State.PRESSED) ? 2 : 1;
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRect(borderWidth / 2, borderWidth / 2,
            getWidth() - borderWidth, getHeight() - borderWidth);

        // Set text color based on state
        if (state == State.PRESSED) setForeground(PRESSED_TEXT);
        else if (state == State.HOVER) setForeground(HOVER_TEXT);
        else setForeground(DEFAULT_TEXT);

        g2d.setTransform(originalTransform);
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
        resetItem.setForeground(ThemeManager.DESTRUCTIVE);
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
