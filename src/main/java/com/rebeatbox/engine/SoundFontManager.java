package com.rebeatbox.engine;

import com.sun.media.sound.AudioSynthesizer;

import javax.sound.midi.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SoundFontManager {
    private Synthesizer synthesizer;
    private boolean customSoundFontLoaded;

    public Synthesizer initialize() throws MidiUnavailableException {
        synthesizer = MidiSystem.getSynthesizer();

        // Reduce audio buffer for lower latency (Phase 3 live performance)
        // Default Gervill buffer ~8192 bytes ≈ 93ms — custom 2048-byte buffer drops to ~23ms
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, 2048);
                line.start();
                if (synthesizer instanceof AudioSynthesizer audioSynth) {
                    Map<String, Object> props = new HashMap<>();
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
