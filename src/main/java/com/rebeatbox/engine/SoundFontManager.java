package com.rebeatbox.engine;

import javax.sound.midi.*;
import java.io.InputStream;

public class SoundFontManager {
    private Synthesizer synthesizer;
    private boolean customSoundFontLoaded;

    public Synthesizer initialize() throws MidiUnavailableException {
        synthesizer = MidiSystem.getSynthesizer();

        // Reduce audio buffer for lower latency (Phase 3 live performance)
        // Default Gervill buffer adds ~100-200ms — custom line drops to ~23ms
        try {
            javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(
                44100, 16, 2, true, false);
            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
                javax.sound.sampled.SourceDataLine.class, format);
            if (javax.sound.sampled.AudioSystem.isLineSupported(info)) {
                javax.sound.sampled.SourceDataLine line =
                    (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
                // 2048 byte buffer ≈ 23ms at 44100Hz/16bit/stereo (vs default ~8192 ≈ 93ms)
                line.open(format, 2048);
                line.start();
                if (synthesizer instanceof com.sun.media.sound.AudioSynthesizer audioSynth) {
                    java.util.Map<String, Object> props = new java.util.HashMap<>();
                    props.put("latency", 50000L); // 50ms target in microseconds
                    audioSynth.open(line, props);
                    System.out.println("Synthesizer opened with low-latency audio line (buffer: 2048 bytes)");
                } else {
                    synthesizer.open();
                }
            } else {
                synthesizer.open();
            }
        } catch (Exception e) {
            System.err.println("Low-latency audio setup failed, using default: " + e.getMessage());
            synthesizer.open();
        }

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
