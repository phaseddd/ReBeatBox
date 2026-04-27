package com.rebeatbox.ui;

import com.rebeatbox.engine.PlaybackController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class ControlBar extends JPanel {
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton restartButton;
    private JSlider bpmSlider;
    private JLabel bpmLabel;
    private JSlider volumeSlider;
    private JLabel volumeLabel;
    private JProgressBar progressBar;
    private JLabel timeLabel;
    private JButton openButton;

    private PlaybackController controller;
    private Consumer<JFileChooser> onFileOpen;

    public ControlBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setBackground(new Color(0x16213e));

        // Transport buttons
        restartButton = createTransportButton("⏮", "Restart"); // ⏮
        playButton = createTransportButton("▶", "Play"); // ▶
        pauseButton = createTransportButton("⏸", "Pause"); // ⏸
        stopButton = createTransportButton("⏹", "Stop"); // ⏹

        add(restartButton);
        add(playButton);
        add(pauseButton);
        add(stopButton);

        add(Box.createHorizontalStrut(16));

        // BPM slider
        bpmLabel = new JLabel("BPM: 120");
        bpmLabel.setForeground(new Color(0xe0e0e0));
        bpmSlider = new JSlider(20, 300, 120);
        bpmSlider.setPreferredSize(new Dimension(180, 40));
        bpmSlider.addChangeListener(e -> {
            int bpm = bpmSlider.getValue();
            bpmLabel.setText("BPM: " + bpm);
            if (controller != null) controller.setBPM(bpm);
        });

        add(bpmLabel);
        add(bpmSlider);

        add(Box.createHorizontalStrut(16));

        // Volume slider
        volumeLabel = new JLabel("Vol: 75%");
        volumeLabel.setForeground(new Color(0xe0e0e0));
        volumeSlider = new JSlider(0, 100, 75);
        volumeSlider.setPreferredSize(new Dimension(120, 40));
        volumeSlider.addChangeListener(e -> {
            int vol = volumeSlider.getValue();
            volumeLabel.setText("Vol: " + vol + "%");
            if (controller != null) controller.setVolume(vol / 100.0f);
        });

        add(volumeLabel);
        add(volumeSlider);

        add(Box.createHorizontalStrut(16));

        // Time label
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setForeground(new Color(0xe0e0e0));
        add(timeLabel);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressBar.setStringPainted(false);
        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controller != null) {
                    int width = progressBar.getWidth();
                    double fraction = (double) e.getX() / width;
                    long seekPos = (long) (fraction * controller.getMicrosecondLength());
                    controller.seek(seekPos);
                }
            }
        });
        add(progressBar);

        add(Box.createHorizontalStrut(8));

        // File open button
        openButton = new JButton("📂"); // 📂
        openButton.setPreferredSize(new Dimension(36, 36));
        openButton.setToolTipText("Open MIDI file");
        openButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open MIDI File");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MIDI Files (*.mid)", "mid"));
            if (onFileOpen != null) onFileOpen.accept(chooser);
        });
        add(openButton);

        // Initial button states
        setTransportEnabled(false);
    }

    private JButton createTransportButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(40, 40));
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        return btn;
    }

    public void wireEngine(PlaybackController controller) {
        this.controller = controller;

        playButton.addActionListener(e -> controller.play());
        pauseButton.addActionListener(e -> controller.pause());
        stopButton.addActionListener(e -> controller.stop());
        restartButton.addActionListener(e -> controller.restart());

        // Progress update timer
        new Timer(100, e -> {
            if (controller.getState() == PlaybackController.State.PLAYING) {
                long pos = controller.getMicrosecondPosition();
                long len = controller.getMicrosecondLength();
                if (len > 0) {
                    int pct = (int) ((pos * 100) / len);
                    progressBar.setValue(pct);
                    timeLabel.setText(formatTime(pos) + " / " + formatTime(len));
                }
                // Update button states
                setTransportEnabled(true);
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            } else if (controller.getState() == PlaybackController.State.PAUSED) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(true);
            } else {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
            }
        }).start();
    }

    public void setOnFileOpen(Consumer<JFileChooser> handler) {
        this.onFileOpen = handler;
    }

    public void onFileLoaded() {
        setTransportEnabled(true);
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
    }

    private void setTransportEnabled(boolean enabled) {
        playButton.setEnabled(enabled);
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        restartButton.setEnabled(enabled);
    }

    private String formatTime(long micros) {
        long totalSec = micros / 1_000_000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
