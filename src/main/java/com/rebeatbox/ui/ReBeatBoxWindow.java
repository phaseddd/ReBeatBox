package com.rebeatbox.ui;

import com.rebeatbox.engine.LiveNoteEventListener;
import com.rebeatbox.engine.NoteEventBus;
import com.rebeatbox.engine.NoteEventListener;
import com.rebeatbox.engine.PlaybackController;
import com.rebeatbox.engine.RealtimeReceiver;
import com.rebeatbox.live.DrumPadGrid;
import com.rebeatbox.live.KeyboardMapper;
import com.rebeatbox.visual.GlitchTransition;
import com.rebeatbox.visual.ParticleSystem;
import com.rebeatbox.visual.PianoRollPanel;
import org.pushingpixels.radiance.animation.api.Timeline;
import org.pushingpixels.radiance.animation.api.Timeline.TimelineState;
import org.pushingpixels.radiance.animation.api.callback.TimelineCallback;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ReBeatBoxWindow extends JFrame {
    private ControlBar controlBar;
    private PianoRollPanel pianoRollPanel;
    private SidebarPanel sidebarPanel;
    private PlaybackController controller;
    private RealtimeReceiver receiver;
    private NoteEventBus eventBus;
    private KeyboardMapper keyboardMapper;
    private KeyboardHintPanel keyboardHintPanel;
    private DrumPadGrid drumPadGrid;
    private ParticleSystem particleSystem;

    public ReBeatBoxWindow() {
        setTitle("ReBeatBox");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        controlBar = new ControlBar();
        pianoRollPanel = new PianoRollPanel();
        sidebarPanel = new SidebarPanel();

        add(controlBar, BorderLayout.NORTH);
        add(pianoRollPanel, BorderLayout.CENTER);
        add(sidebarPanel, BorderLayout.EAST);

        // Phase 3: Keyboard hint panel in SOUTH region (D-07)
        keyboardHintPanel = new KeyboardHintPanel();
        add(keyboardHintPanel, BorderLayout.SOUTH);

        setupDragAndDrop();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (pianoRollPanel != null) pianoRollPanel.dispose();
            }
        });

        // Phase 4: Install particle system on GlassPane (D-09, D-15)
        particleSystem = new ParticleSystem();
        particleSystem.setOpaque(false);
        // Mouse events pass through GlassPane to underlying components (D-09)
        setGlassPane(particleSystem);
        getGlassPane().setVisible(true);
    }

    public void wireEngine(PlaybackController controller, RealtimeReceiver receiver, NoteEventBus eventBus) {
        this.controller = controller;
        this.receiver = receiver;
        this.eventBus = eventBus;
        this.keyboardMapper = new KeyboardMapper();

        controlBar.wireEngine(controller);
        pianoRollPanel.setController(controller);

        // Phase 4: ParticleSystem subscribes to all note-on events (D-11)
        // Ensure ParticleSystem timer is running
        particleSystem.start();

        // Subscribe to sequencer notes (via NoteEventBus)
        eventBus.subscribe(activeNotes -> {
            // Fire a particle at center of PianoRollPanel for each active note
            for (int note : activeNotes) {
                // Default velocity=100 for sequencer notes — KNOWN LIMITATION: NoteEventBus.activeNotes
                // does not carry per-note velocity. Sequencer notes always emit with velocity=100.
                particleSystem.emit(note, 100);
            }
        });

        // Phase 3: Drum pad grid in sidebar content panel (D-04)
        drumPadGrid = new DrumPadGrid(receiver);
        sidebarPanel.getContentPanel().add(drumPadGrid, BorderLayout.CENTER);

        // Phase 3+4: PianoRollPanel live note flash + ParticleSystem emission (D-10, D-11)
        eventBus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) {
                pianoRollPanel.repaint();
                particleSystem.emit(note, velocity);
            }
            @Override
            public void onLiveNoteOff(int note) {
                pianoRollPanel.repaint();
            }
        });

        // Phase 3: Global keyboard dispatcher (D-03)
        registerKeyboardDispatcher();

        // Phase 3: Window focus loss handler (stuck-note prevention)
        registerFocusLossHandler();

        controlBar.setOnFileOpen(chooser -> {
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                loadAndPlay(file);
            }
        });

        // Phase 4: Set particle emit origin to center of PianoRollPanel area.
        // Update on resize so bursts always originate from the piano roll region.
        pianoRollPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateParticleEmitOrigin();
            }
        });
        SwingUtilities.invokeLater(this::updateParticleEmitOrigin);
    }

    private void updateParticleEmitOrigin() {
        if (particleSystem == null || pianoRollPanel == null) return;
        Point pt = SwingUtilities.convertPoint(
            pianoRollPanel,
            pianoRollPanel.getWidth() / 2,
            pianoRollPanel.getHeight() / 2,
            particleSystem);
        particleSystem.setEmitOrigin(pt.x, pt.y);
    }

    private void registerKeyboardDispatcher() {
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(e -> {
            int id = e.getID();
            if (id != KeyEvent.KEY_PRESSED && id != KeyEvent.KEY_RELEASED) {
                return false;
            }
            if (e.isConsumed()) {
                return false;
            }

            Component focusOwner = kfm.getFocusOwner();
            if (focusOwner instanceof JTextComponent) {
                return false;
            }

            int note = KeyboardMapper.keyCodeToNote(e.getKeyCode());
            if (note < 0) {
                return false;
            }

            boolean pressed = (id == KeyEvent.KEY_PRESSED);
            if (pressed && !keyboardMapper.isActive(note)) {
                receiver.noteOn(note, 100);
                eventBus.fireLiveNoteOn(note, 100);
                keyboardMapper.setActive(note, true);
                keyboardHintPanel.setKeyHighlighted(e.getKeyCode(), true);
            } else if (!pressed && keyboardMapper.isActive(note)) {
                receiver.noteOff(note);
                eventBus.fireLiveNoteOff(note);
                keyboardMapper.setActive(note, false);
                keyboardHintPanel.setKeyHighlighted(e.getKeyCode(), false);
            }

            return false;
        });
    }

    private void registerFocusLossHandler() {
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (keyboardMapper == null || receiver == null) return;

                for (int note = 0; note < 128; note++) {
                    if (keyboardMapper.isActive(note)) {
                        receiver.noteOff(note);
                        eventBus.fireLiveNoteOff(note);
                        keyboardMapper.setActive(note, false);
                    }
                }

                if (keyboardHintPanel != null) {
                    keyboardHintPanel.clearAllHighlights();
                }
            }
        });
    }

    private void setupDragAndDrop() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        if (file.getName().toLowerCase().endsWith(".mid")) {
                            loadAndPlay(file);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Drag-and-drop failed: " + e.getMessage());
                }
                return false;
            }
        });
    }

    private void loadAndPlay(File file) {
        // Phase 4: Capture snapshot for glitch transition (D-16 trigger: new MIDI file loaded)
        BufferedImage preSnapshot = null;
        if (pianoRollPanel.getWidth() > 0 && pianoRollPanel.getHeight() > 0) {
            preSnapshot = new BufferedImage(pianoRollPanel.getWidth(), pianoRollPanel.getHeight(),
                                            BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = preSnapshot.createGraphics();
            pianoRollPanel.paint(g2);
            g2.dispose();
        }

        try {
            controller.load(file);
            pianoRollPanel.onFileLoaded();
            controller.play();
            controlBar.onFileLoaded();
            setTitle("ReBeatBox - " + file.getName());
        } catch (InvalidMidiDataException e) {
            JOptionPane.showMessageDialog(this,
                "The file may be corrupted or is not a standard MIDI file.",
                "Cannot Play File",
                JOptionPane.ERROR_MESSAGE);
            return; // don't run glitch if file failed
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Cannot read file: " + e.getMessage(),
                "File Read Error",
                JOptionPane.ERROR_MESSAGE);
            return; // don't run glitch if file failed
        }

        // Phase 4: Run glitch transition on the captured pre-load snapshot (D-16, D-18: 300ms)
        if (preSnapshot != null) {
            runFileLoadGlitch(preSnapshot);
        }
    }

    /**
     * Runs an RGB channel-split glitch transition on the pre-load snapshot
     * of the PianoRollPanel. Duration: 300ms per D-18. Max offset: 25px per UI-SPEC.
     */
    private void runFileLoadGlitch(BufferedImage snapshot) {
        final int maxOffset = 25; // UI-SPEC: file load max offset 25px
        final int duration = 300; // D-18: file load transition 300ms

        // Calculate PianoRollPanel position in GlassPane coordinate space
        // so the glitch overlay renders directly over the piano roll, not at (0,0).
        Point panelOrigin = SwingUtilities.convertPoint(pianoRollPanel, 0, 0, particleSystem);
        final int overlayX = panelOrigin.x;
        final int overlayY = panelOrigin.y;

        Timeline glitchTimeline = Timeline.builder(this)
            .setDuration(duration)
            .addCallback(new TimelineCallback() {
                @Override
                public void onTimelinePulse(float durationFraction, float timelinePosition) {
                    // Bell curve: peak at 0.5, returns to 0 at start/end
                    float bellCurve = 4.0f * timelinePosition * (1.0f - timelinePosition);
                    int offset = Math.round(maxOffset * bellCurve);

                    // Apply glitch to snapshot (not to live PianoRollPanel — per D-19/Pitfall 5)
                    BufferedImage glitched = GlitchTransition.applyRgbSplit(snapshot, -offset, offset);

                    // Render glitch overlay via ParticleSystem GlassPane at the correct position
                    particleSystem.setOverlayImage(glitched, overlayX, overlayY);
                    particleSystem.repaint();
                }

                @Override
                public void onTimelineStateChanged(TimelineState oldState, TimelineState newState,
                                                   float durationFraction, float timelinePosition) {
                    if (newState == TimelineState.DONE) {
                        particleSystem.setOverlayImage(null, 0, 0);
                        particleSystem.repaint();
                        snapshot.flush();
                    }
                }
            })
            .build();
        glitchTimeline.play();
    }
}
