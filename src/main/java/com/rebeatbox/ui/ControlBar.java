package com.rebeatbox.ui;

import com.rebeatbox.engine.PlaybackController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

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
        setBackground(new Color(0x16213e));

        // Transport buttons
        restartButton = createTransportButton("⏮", "Restart");
        playButton = createTransportButton("▶", "Play");
        pauseButton = createTransportButton("⏸", "Pause");
        stopButton = createTransportButton("⏹", "Stop");

        add(restartButton);
        add(playButton);
        add(pauseButton);
        add(stopButton);
        add(Box.createHorizontalStrut(10));

        // BPM
        bpmLabel = new JLabel("BPM: 120");
        bpmLabel.setForeground(new Color(0xe0e0e0));
        bpmSlider = new JSlider(20, 300, 120);
        bpmSlider.setPreferredSize(new Dimension(140, 36));
        add(bpmLabel);
        add(bpmSlider);
        add(Box.createHorizontalStrut(10));

        // Volume
        volumeLabel = new JLabel("Vol: 75%");
        volumeLabel.setForeground(new Color(0xe0e0e0));
        volumeSlider = new JSlider(0, 100, 75);
        volumeSlider.setPreferredSize(new Dimension(100, 36));
        add(volumeLabel);
        add(volumeSlider);
        add(Box.createHorizontalStrut(10));

        // Time
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setForeground(new Color(0xe0e0e0));
        add(timeLabel);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(240, 18));
        progressBar.setStringPainted(false);
        add(progressBar);
        add(Box.createHorizontalStrut(4));

        // File open
        openButton = new JButton("📂");
        openButton.setPreferredSize(new Dimension(36, 36));
        openButton.setToolTipText("Open MIDI file");
        add(openButton);

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

    private JButton createTransportButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        return btn;
    }

    private String formatTime(long micros) {
        long sec = micros / 1_000_000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
