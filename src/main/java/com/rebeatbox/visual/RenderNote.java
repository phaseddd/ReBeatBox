package com.rebeatbox.visual;

/**
 * Immutable representation of a single note for the render pipeline.
 *
 * <p>Pairs a MIDI NoteOn/NoteOff into start/end times (in microseconds) with pitch,
 * velocity, and channel information. Validation in the compact constructor catches
 * data corruption early.
 */
public record RenderNote(int pitch, long startMicros, long endMicros, int velocity, int channel) {

    private static final String[] NOTE_NAMES = {
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static final int MIN_PITCH = 0;
    private static final int MAX_PITCH = 127;
    private static final int MIN_VELOCITY = 0;
    private static final int MAX_VELOCITY = 127;
    private static final int MIN_CHANNEL = 0;
    private static final int MAX_CHANNEL = 15;

    /**
     * Compact constructor with validation.
     * Throws IllegalArgumentException for out-of-range values.
     */
    public RenderNote {
        if (pitch < MIN_PITCH || pitch > MAX_PITCH) {
            throw new IllegalArgumentException(
                "pitch must be 0-127, got " + pitch);
        }
        if (velocity < MIN_VELOCITY || velocity > MAX_VELOCITY) {
            throw new IllegalArgumentException(
                "velocity must be 0-127, got " + velocity);
        }
        if (channel < MIN_CHANNEL || channel > MAX_CHANNEL) {
            throw new IllegalArgumentException(
                "channel must be 0-15, got " + channel);
        }
        if (startMicros < 0) {
            throw new IllegalArgumentException(
                "startMicros must be >= 0, got " + startMicros);
        }
        if (endMicros < startMicros) {
            throw new IllegalArgumentException(
                "endMicros (" + endMicros + ") must be >= startMicros (" + startMicros + ")");
        }
    }

    /**
     * Returns a compact debugging representation including the pitch name and time range.
     * Example: "RenderNote[pitch=C4(60), 0.00s-0.50s, vel=100, ch=0]"
     */
    @Override
    public String toString() {
        String name = pitchName(pitch);
        double startSec = startMicros / 1_000_000.0;
        double endSec = endMicros / 1_000_000.0;
        return String.format("RenderNote[pitch=%s(%d), %.2fs-%.2fs, vel=%d, ch=%d]",
            name, pitch, startSec, endSec, velocity, channel);
    }

    /**
     * Converts a MIDI note number to a human-readable pitch name.
     * Examples: 60 -> "C4", 61 -> "C#4", 0 -> "C-1", 127 -> "G9"
     */
    public static String pitchName(int midiNote) {
        if (midiNote < MIN_PITCH || midiNote > MAX_PITCH) {
            throw new IllegalArgumentException("midiNote must be 0-127, got " + midiNote);
        }
        int noteIndex = midiNote % 12;
        int octave = (midiNote / 12) - 1;
        return NOTE_NAMES[noteIndex] + octave;
    }
}
