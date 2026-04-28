package com.rebeatbox.live;

import com.rebeatbox.engine.RealtimeReceiver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DrumPadGrid extends JPanel {

    private static final int[] DEFAULT_GM_NOTES = {
        36, 38, 42, 46,
        39, 49, 51, 48,
        45, 41, 37, 56,
        75, 70, 69, 81,
    };

    private static final String[] DEFAULT_LABELS = {
        "Kick", "Snare", "Cl Hat", "Op Hat",
        "Clap", "Crash", "Ride", "Tom Hi",
        "Tom Mid", "Tom Lo", "Rimshot", "Cowbell",
        "Claves", "Maracas", "Shaker", "Triangle",
    };

    private final PadButton[] pads = new PadButton[16];
    private final RealtimeReceiver receiver;

    public DrumPadGrid(RealtimeReceiver receiver) {
        this.receiver = receiver;

        setLayout(new GridLayout(4, 4, 8, 8));
        setBackground(new Color(0x16213E));
        setBorder(new EmptyBorder(0, 12, 0, 12));

        for (int i = 0; i < 16; i++) {
            PadButton pad = new PadButton(DEFAULT_LABELS[i], DEFAULT_GM_NOTES[i], receiver);
            pads[i] = pad;
            add(pad);
        }
    }

    public void setPadSound(int padIndex, int midiNote, String label) {
        if (padIndex < 0 || padIndex >= 16) return;
        pads[padIndex].setMidiNote(midiNote, label);
    }

    public int getPadNote(int padIndex) {
        if (padIndex < 0 || padIndex >= 16) return -1;
        return pads[padIndex].getMidiNote();
    }

    public String getPadLabel(int padIndex) {
        if (padIndex < 0 || padIndex >= 16) return null;
        return pads[padIndex].getPadLabel();
    }

    public PadButton getPad(int padIndex) {
        if (padIndex < 0 || padIndex >= 16) return null;
        return pads[padIndex];
    }
}
