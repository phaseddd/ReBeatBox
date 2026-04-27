package com.rebeatbox.ui;

import com.rebeatbox.engine.PlaybackController;
import com.rebeatbox.engine.RealtimeReceiver;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ReBeatBoxWindow extends JFrame {
    private ControlBar controlBar;
    private PlaceholderPanel placeholderPanel;
    private SidebarPanel sidebarPanel;
    private PlaybackController controller;
    private RealtimeReceiver receiver;

    public ReBeatBoxWindow() {
        setTitle("ReBeatBox");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        controlBar = new ControlBar();
        placeholderPanel = new PlaceholderPanel();
        sidebarPanel = new SidebarPanel();

        add(controlBar, BorderLayout.NORTH);
        add(placeholderPanel, BorderLayout.CENTER);
        add(sidebarPanel, BorderLayout.EAST);

        // Drag and drop support
        setupDragAndDrop();
    }

    public void wireEngine(PlaybackController controller, RealtimeReceiver receiver) {
        this.controller = controller;
        this.receiver = receiver;
        controlBar.wireEngine(controller);

        // File open callback
        controlBar.setOnFileOpen(chooser -> {
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                loadAndPlay(file);
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
