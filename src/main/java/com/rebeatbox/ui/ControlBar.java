package com.rebeatbox.ui;

import com.rebeatbox.engine.PlaybackController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.pushingpixels.radiance.animation.api.Timeline;
import org.pushingpixels.radiance.animation.api.Timeline.TimelineState;
import org.pushingpixels.radiance.animation.api.callback.TimelineCallback;
import org.pushingpixels.radiance.animation.api.ease.Spline;

public class ControlBar extends JPanel {
    private JButton restartButton, playButton, pauseButton, stopButton, openButton;
    private JSlider bpmSlider, volumeSlider;
    private JLabel bpmLabel, volumeLabel, timeLabel;
    private JProgressBar progressBar;

    private PlaybackController controller;
    private Consumer<JFileChooser> onFileOpen;
    private Timer stateTimer;

    public ControlBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));
        setBackground(ThemeManager.BG_SURFACE);

        // Transport buttons
        restartButton = createTransportButton("restart", "Restart");
        playButton     = createTransportButton("play",    "Play");
        pauseButton    = createTransportButton("pause",   "Pause");
        stopButton     = createTransportButton("stop",    "Stop");

        add(restartButton);
        add(playButton);
        add(pauseButton);
        add(stopButton);
        add(Box.createHorizontalStrut(10));

        // BPM
        bpmLabel = new JLabel("BPM: 120");
        bpmLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        bpmSlider = new JSlider(20, 300, 120);
        bpmSlider.setPreferredSize(new Dimension(140, 36));
        add(bpmLabel);
        add(bpmSlider);
        add(Box.createHorizontalStrut(10));

        // Volume
        volumeLabel = new JLabel("Vol: 75%");
        volumeLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        volumeSlider = new JSlider(0, 100, 75);
        volumeSlider.setPreferredSize(new Dimension(100, 36));
        add(volumeLabel);
        add(volumeSlider);
        add(Box.createHorizontalStrut(10));

        // Time
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setForeground(ThemeManager.TEXT_PRIMARY);
        add(timeLabel);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(240, 18));
        progressBar.setStringPainted(false);
        progressBar.setForeground(ThemeManager.accentForHue(ThemeManager.HUE_TRANSPORT));
        add(progressBar);
        add(Box.createHorizontalStrut(4));

        // File open
        openButton = new JButton();
        openButton.setPreferredSize(new Dimension(38, 38));
        openButton.setToolTipText("Open MIDI File");
        openButton.setFocusable(false);
        openButton.setBorder(BorderFactory.createLineBorder(ThemeManager.BORDER_IDLE, 1));
        openButton.setBackground(ThemeManager.BG_ELEVATED);
        openButton.getAccessibleContext().setAccessibleName("Open MIDI file");
        try {
            BufferedImage icon = SvgIconLoader.getIcon("open-file", 38);
            if (icon != null) openButton.setIcon(new ImageIcon(icon));
        } catch (Exception e) {
            System.err.println("Failed to load 'open-file' icon: " + e.getMessage());
        }
        add(openButton);

        // Wire hover/press animations per D-05, D-06 (all transport buttons + open)
        wireButtonAnimation(restartButton, ThemeManager.HUE_TRANSPORT);
        wireButtonAnimation(playButton,     ThemeManager.HUE_TRANSPORT);
        wireButtonAnimation(pauseButton,    ThemeManager.HUE_TRANSPORT);
        wireButtonAnimation(stopButton,     ThemeManager.HUE_TRANSPORT);
        wireButtonAnimation(openButton,     ThemeManager.HUE_TRANSPORT);

        // Timer for progress + state sync
        stateTimer = new Timer(100, e -> syncButtonStates());
        stateTimer.setInitialDelay(0);
        stateTimer.start();

        // Wire slider listeners
        bpmSlider.addChangeListener(e -> {
            if (controller != null) {
                int bpm = bpmSlider.getValue();
                controller.setBPM(bpm);
                bpmLabel.setText("BPM: " + bpm);
            }
        });

        volumeSlider.addChangeListener(e -> {
            if (controller != null) {
                int vol = volumeSlider.getValue();
                controller.setVolume(vol / 100.0f);
                volumeLabel.setText("Vol: " + vol + "%");
            }
        });

        // Progress bar click-to-seek
        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controller == null) return;
                int w = progressBar.getWidth();
                if (w <= 0) return;
                controller.seek((long) ((double) e.getX() / w * controller.getMicrosecondLength()));
            }
        });

        // File open
        openButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open MIDI File");
            chooser.setFileFilter(new FileNameExtensionFilter("MIDI Files (*.mid)", "mid"));
            if (onFileOpen != null) onFileOpen.accept(chooser);
        });

        syncButtonStates();
    }

    public void wireEngine(PlaybackController controller) {
        this.controller = controller;

        playButton.addActionListener(e -> controller.play());
        pauseButton.addActionListener(e -> controller.pause());
        stopButton.addActionListener(e -> controller.stop());
        restartButton.addActionListener(e -> controller.restart());

        syncButtonStates();
    }

    public void setOnFileOpen(Consumer<JFileChooser> handler) {
        this.onFileOpen = handler;
    }

    public void onFileLoaded() {
        if (controller != null) {
            int nativeBpm = controller.getNativeBPM();
            bpmSlider.setValue(nativeBpm);
            bpmLabel.setText("BPM: " + nativeBpm);
        }
        syncButtonStates();
    }

    private void syncButtonStates() {
        if (controller == null) {
            playButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            restartButton.setEnabled(false);
            progressBar.setValue(0);
            timeLabel.setText("00:00 / 00:00");
            return;
        }

        boolean hasSequence = controller.getMicrosecondLength() > 0;
        PlaybackController.State state = controller.getState();

        if (!hasSequence) {
            playButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            restartButton.setEnabled(false);
            progressBar.setValue(0);
            timeLabel.setText("00:00 / 00:00");
            return;
        }

        switch (state) {
            case PLAYING:
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
                restartButton.setEnabled(true);
                break;
            case PAUSED:
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(true);
                restartButton.setEnabled(true);
                break;
            case STOPPED:
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                restartButton.setEnabled(true);
                break;
        }

        // Progress bar
        long pos = controller.getMicrosecondPosition();
        long len = controller.getMicrosecondLength();
        if (len > 0) {
            progressBar.setValue((int) (pos * 100 / len));
            timeLabel.setText(formatTime(pos) + " / " + formatTime(len));
        }
    }

    private JButton createTransportButton(String iconName, String tooltip) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        btn.setBorder(BorderFactory.createLineBorder(ThemeManager.BORDER_IDLE, 1));
        btn.setBackground(ThemeManager.BG_ELEVATED);

        // Set SVG icon
        try {
            BufferedImage icon = SvgIconLoader.getIcon(iconName, 38);
            if (icon != null) {
                btn.setIcon(new ImageIcon(icon));
            }
        } catch (Exception e) {
            System.err.println("Failed to load SVG icon '" + iconName + "': " + e.getMessage());
        }

        // Accessible name per UI-SPEC Copywriting Contract
        btn.getAccessibleContext().setAccessibleName(tooltip);

        return btn;
    }

    /**
     * Linearly interpolates between two Colors.
     * Used by Timeline callbacks for smooth border/fill transitions.
     */
    private static Color interpolateColor(Color a, Color b, float t) {
        float ti = Math.max(0.0f, Math.min(1.0f, t));
        int r = (int)(a.getRed()   + ti * (b.getRed()   - a.getRed()));
        int g = (int)(a.getGreen() + ti * (b.getGreen() - a.getGreen()));
        int bl = (int)(a.getBlue() + ti * (b.getBlue() - a.getBlue()));
        return new Color(Math.min(255, Math.max(0, r)),
                         Math.min(255, Math.max(0, g)),
                         Math.min(255, Math.max(0, bl)));
    }

    /**
     * Wires Radiance Timeline hover border-color and press scale animations
     * onto a JButton. Per D-05, D-06, D-07.
     *
     * <p>Hover: borderColor transitions from BORDER_IDLE to accentForHue(regionHue)
     * in 200ms. Press: scale drops to 0.95x in 75ms, springs back to 1.00x in 150ms.
     */
    private void wireButtonAnimation(JButton button, float regionHue) {
        final Color accentColor = ThemeManager.accentForHue(regionHue);
        final Dimension origSize = new Dimension(button.getPreferredSize());

        button.addMouseListener(new MouseAdapter() {
            private Timeline hoverIn;
            private Timeline hoverOut;
            private Timeline pressDown;
            private Timeline pressUp;

            @Override
            public void mouseEntered(MouseEvent e) {
                if (hoverOut != null) hoverOut.abort();
                if (hoverIn != null) hoverIn.abort();
                hoverIn = Timeline.builder(button)
                    .setDuration(200)
                    .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
                    .addCallback(new TimelineCallback() {
                        @Override
                        public void onTimelinePulse(float durationFraction, float timelinePosition) {
                            Color c = interpolateColor(ThemeManager.BORDER_IDLE, accentColor, timelinePosition);
                            button.setBorder(BorderFactory.createLineBorder(c, 1));
                        }
                        @Override
                        public void onTimelineStateChanged(TimelineState old, TimelineState n, float f, float p) {}
                    })
                    .build();
                hoverIn.play();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverIn != null) hoverIn.abort();
                if (hoverOut != null) hoverOut.abort();
                hoverOut = Timeline.builder(button)
                    .setDuration(250)
                    .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
                    .addCallback(new TimelineCallback() {
                        @Override
                        public void onTimelinePulse(float durationFraction, float timelinePosition) {
                            Color c = interpolateColor(accentColor, ThemeManager.BORDER_IDLE, timelinePosition);
                            button.setBorder(BorderFactory.createLineBorder(c, 1));
                        }
                        @Override
                        public void onTimelineStateChanged(TimelineState old, TimelineState n, float f, float p) {}
                    })
                    .build();
                hoverOut.play();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (pressUp != null) pressUp.abort();
                    if (pressDown != null) pressDown.abort();
                    pressDown = Timeline.builder(button)
                        .setDuration(75)
                        .addCallback(new TimelineCallback() {
                            @Override
                            public void onTimelinePulse(float durationFraction, float timelinePosition) {
                                float scale = 1.00f + timelinePosition * (0.95f - 1.00f);
                                button.setPreferredSize(new Dimension((int)(origSize.width * scale), (int)(origSize.height * scale)));
                                button.getParent().revalidate();
                            }
                            @Override
                            public void onTimelineStateChanged(TimelineState old, TimelineState n, float f, float p) {}
                        })
                        .build();
                    pressDown.play();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (pressDown != null) pressDown.abort();
                    if (pressUp != null) pressUp.abort();
                    pressUp = Timeline.builder(button)
                        .setDuration(150)
                        .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
                        .addCallback(new TimelineCallback() {
                            @Override
                            public void onTimelinePulse(float durationFraction, float timelinePosition) {
                                float scale = 0.95f + timelinePosition * (1.00f - 0.95f);
                                button.setPreferredSize(new Dimension((int)(origSize.width * scale), (int)(origSize.height * scale)));
                                button.getParent().revalidate();
                            }
                            @Override
                            public void onTimelineStateChanged(TimelineState old, TimelineState n, float f, float p) {
                                if (n == TimelineState.DONE) {
                                    button.setPreferredSize(origSize);
                                    button.getParent().revalidate();
                                }
                            }
                        })
                        .build();
                    pressUp.play();
                }
            }
        });
    }

    private String formatTime(long micros) {
        long sec = micros / 1_000_000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
