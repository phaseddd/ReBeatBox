package com.rebeatbox.engine;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiFileLoader {

    public Sequence load(File file) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(file);
    }
}
