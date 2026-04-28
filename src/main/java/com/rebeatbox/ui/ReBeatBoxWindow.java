package com.rebeatbox.ui;

import com.rebeatbox.engine.LiveNoteEventListener;
import com.rebeatbox.engine.NoteEventBus;
import com.rebeatbox.engine.PlaybackController;
import com.rebeatbox.engine.RealtimeReceiver;
import com.rebeatbox.live.DrumPadGrid;
import com.rebeatbox.live.KeyboardMapper;
import com.rebeatbox.visual.PianoRollPanel;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    }

    public void wireEngine(PlaybackController controller, RealtimeReceiver receiver, NoteEventBus eventBus) {
        this.controller = controller;
        this.receiver = receiver;
        this.eventBus = eventBus;
        this.keyboardMapper = new KeyboardMapper();

        controlBar.wireEngine(controller);
        pianoRollPanel.setController(controller);

        // Phase 3: Drum pad grid in sidebar content panel (D-04)
        drumPadGrid = new DrumPadGrid(receiver);
        sidebarPanel.getContentPanel().add(drumPadGrid, BorderLayout.CENTER);

        // Phase 3: PianoRollPanel live note flash (D-10)
        eventBus.subscribeLive(new LiveNoteEventListener() {
            @Override
            public void onLiveNoteOn(int note, int velocity) {
                pianoRollPanel.repaint();
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
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Cannot read file: " + e.getMessage(),
                "File Read Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
