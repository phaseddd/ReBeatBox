package com.rebeatbox.engine;

import javax.sound.midi.*;
import java.io.InputStream;

public class SoundFontManager {
    private Synthesizer synthesizer;
    private boolean customSoundFontLoaded;

    public Synthesizer initialize() throws MidiUnavailableException {
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();

        // Try to load FluidR3_GM.sf2 from classpath
        try (InputStream sf2Stream = getClass().getResourceAsStream("/soundfonts/FluidR3_GM.sf2")) {
            if (sf2Stream != null) {
                Soundbank soundbank = MidiSystem.getSoundbank(sf2Stream);
                synthesizer.loadAllInstruments(soundbank);
                customSoundFontLoaded = true;
                System.out.println("Loaded custom SoundFont: FluidR3_GM.sf2");
            } else {
                System.out.println("FluidR3_GM.sf2 not found -- using JDK default soundbank");
                customSoundFontLoaded = false;
            }
        } catch (Exception e) {
            System.err.println("Failed to load FluidR3_GM.sf2: " + e.getMessage());
            System.err.println("Falling back to JDK default soundbank");
            customSoundFontLoaded = false;
        }

        return synthesizer;
    }

    public Synthesizer getSynthesizer() {
        return synthesizer;
    }

    public boolean isCustomSoundFontLoaded() {
        return customSoundFontLoaded;
    }

    public void close() {
        if (synthesizer != null && synthesizer.isOpen()) {
            synthesizer.close();
        }
    }
}
