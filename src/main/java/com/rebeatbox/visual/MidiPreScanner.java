package com.rebeatbox.visual;

import javax.sound.midi.*;
import java.util.*;

/**
 * Pre-scans a MIDI Sequence to produce a sorted list of RenderNote objects.
 *
 * <p>Single static entry point: {@link #scan(Sequence)}.
 * Pairs NoteOn/NoteOff events, handles overlapping notes, orphan detection,
 * Type 0/Type 1 MIDI files, and tempo map extraction for accurate tick-to-microsecond
 * conversion.
 *
 * <p>Per D-09: Track 0 is skipped for Type 1 files (conductor metadata).
 * Type 0 files (single track) process the only track.
 */
public final class MidiPreScanner {

    /** Default tempo in microseconds per beat (120 BPM). */
    private static final int DEFAULT_TEMPO_MPQ = 500_000;

    /** Key separator for pitch+channel composite keys. */
    private static final String KEY_SEPARATOR = ":";

    private MidiPreScanner() {
        // Utility class — no instantiation
    }

    /**
     * Pre-scans the given MIDI Sequence and returns a list of RenderNote objects
     * sorted by startMicros ascending.
     *
     * @param sequence the MIDI sequence to scan; may be null
     * @return immutable sorted list of RenderNote (empty if no notes found)
     */
    public static List<RenderNote> scan(Sequence sequence) {
        if (sequence == null) {
            return Collections.emptyList();
        }

        Track[] tracks = sequence.getTracks();
        if (tracks == null || tracks.length == 0) {
            return Collections.emptyList();
        }

        // Build tempo map from all tracks (including track 0 for Type 1)
        List<TempoEvent> tempoMap = buildTempoMap(sequence);

        List<RenderNote> result = new ArrayList<>();
        Map<String, PendingNote> pending = new HashMap<>();

        int startTrack = determineStartTrack(tracks);

        for (int t = startTrack; t < tracks.length; t++) {
            Track track = tracks[t];
            if (track == null) continue;

            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                if (!(message instanceof ShortMessage)) {
                    continue;
                }

                ShortMessage sm = (ShortMessage) message;
                int command = sm.getCommand();

                if (command == ShortMessage.NOTE_ON) {
                    int pitch = sm.getData1();
                    int velocity = sm.getData2();
                    int channel = sm.getChannel();
                    long tick = event.getTick();

                    String key = keyFor(pitch, channel);

                    if (velocity > 0) {
                        // Real NoteOn
                        // If there's already a pending note on this key,
                        // treat this new NoteOn as an implicit NoteOff for the pending one
                        PendingNote existing = pending.remove(key);
                        if (existing != null) {
                            result.add(buildNote(existing, tick, sequence, tempoMap));
                        }
                        // Store the new pending note
                        pending.put(key, new PendingNote(pitch, tick, velocity, channel));
                    } else {
                        // NOTE_ON with velocity 0 = NoteOff
                        PendingNote pn = pending.remove(key);
                        if (pn != null) {
                            result.add(buildNote(pn, tick, sequence, tempoMap));
                        }
                    }
                } else if (command == ShortMessage.NOTE_OFF) {
                    int pitch = sm.getData1();
                    int channel = sm.getChannel();
                    long tick = event.getTick();

                    String key = keyFor(pitch, channel);
                    PendingNote pn = pending.remove(key);
                    if (pn != null) {
                        result.add(buildNote(pn, tick, sequence, tempoMap));
                    }
                }
            }

            // Handle orphaned notes at end of this track
            long endTickForOrphans = sequence.getTickLength();
            long sequenceLengthMicros = sequence.getMicrosecondLength();

            var iter = pending.values().iterator();
            while (iter.hasNext()) {
                PendingNote pn = iter.next();
                long endTick = endTickForOrphans;
                long endMicros = tickToMicrosecond(endTick, sequence, tempoMap);

                // Prefer sequence.getMicrosecondLength() for end time
                // if it's more accurate for the actual duration
                if (sequenceLengthMicros > 0 && sequenceLengthMicros > endMicros) {
                    endMicros = sequenceLengthMicros;
                }

                result.add(new RenderNote(pn.pitch,
                    tickToMicrosecond(pn.startTick, sequence, tempoMap),
                    endMicros, pn.velocity, pn.channel));
                iter.remove();
            }
            pending.clear(); // Clear remaining pendings (track-ended contexts)
        }

        // Sort by startMicros ascending (required for binary search in Plan 02-02)
        result.sort(Comparator.comparingLong(RenderNote::startMicros));

        return Collections.unmodifiableList(result);
    }

    // --- Private helpers ---

    /**
     * Determines the first track index to process.
     * Type 0 (single track): process track 0 (it's the only one).
     * Type 1 (multi-track): skip track 0 (conductor metadata).
     */
    private static int determineStartTrack(Track[] tracks) {
        if (tracks.length == 1) {
            // Type 0 MIDI: single track contains everything
            return 0;
        }
        // Type 1 MIDI: skip track 0 (conductor/metadata)
        return 1;
    }

    /**
     * Builds a sorted tempo map from MetaMessage tempo events (type 0x51)
     * across all tracks in the sequence.
     * Returns a list of TempoEvent sorted by tick, with a default
     * (tick=0, mpq=500000) entry if none exists at tick 0.
     */
    static List<TempoEvent> buildTempoMap(Sequence sequence) {
        List<TempoEvent> events = new ArrayList<>();
        // Always include a default at tick 0
        events.add(new TempoEvent(0, DEFAULT_TEMPO_MPQ));

        Track[] tracks = sequence.getTracks();
        for (Track track : tracks) {
            if (track == null) continue;
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage) {
                    MetaMessage mm = (MetaMessage) message;
                    if (mm.getType() == 0x51) {
                        // Tempo event: 3 bytes — microseconds per quarter note
                        byte[] data = mm.getData();
                        int mpq = ((data[0] & 0xFF) << 16)
                                | ((data[1] & 0xFF) << 8)
                                | (data[2] & 0xFF);
                        events.add(new TempoEvent(event.getTick(), mpq));
                    }
                }
            }
        }

        // Sort by tick
        events.sort(Comparator.comparingLong(e -> e.tick));

        // Remove the default tick-0 entry if we found a real tempo at tick 0
        if (events.size() > 1 && events.get(1).tick == 0) {
            events.remove(0); // Remove our synthetic default
        }

        return events;
    }

    /**
     * Converts a MIDI tick position to microseconds using the tempo map.
     */
    static long tickToMicrosecond(long tick, Sequence sequence, List<TempoEvent> tempoMap) {
        if (tick <= 0) {
            return 0;
        }

        int resolution = sequence.getResolution();
        if (resolution <= 0) {
            resolution = 480; // Fallback
        }

        long micros = 0;
        long lastTick = 0;
        int lastMpq = DEFAULT_TEMPO_MPQ;

        for (TempoEvent te : tempoMap) {
            if (te.tick >= tick) {
                // Current tick falls between lastTick and this tempo event
                long ticksInSegment = tick - lastTick;
                micros += ticksInSegment * lastMpq / resolution;
                return micros;
            }
            // Accumulate the full segment
            long ticksInSegment = te.tick - lastTick;
            micros += ticksInSegment * lastMpq / resolution;
            lastTick = te.tick;
            lastMpq = te.mpq;
        }

        // Handle ticks beyond the last tempo event
        long remainingTicks = tick - lastTick;
        micros += remainingTicks * lastMpq / resolution;

        return micros;
    }

    /**
     * Builds a RenderNote from a pending entry and an end tick.
     */
    private static RenderNote buildNote(PendingNote pn, long endTick,
                                        Sequence sequence, List<TempoEvent> tempoMap) {
        long startMicros = tickToMicrosecond(pn.startTick, sequence, tempoMap);
        long endMicros = tickToMicrosecond(endTick, sequence, tempoMap);
        return new RenderNote(pn.pitch, startMicros, endMicros, pn.velocity, pn.channel);
    }

    /**
     * Creates a composite key for pitch + channel pair.
     */
    private static String keyFor(int pitch, int channel) {
        return pitch + KEY_SEPARATOR + channel;
    }

    // --- Inner types ---

    /** A pending (unclosed) NoteOn waiting for its NoteOff. */
    private static class PendingNote {
        final int pitch;
        final long startTick;
        final int velocity;
        final int channel;

        PendingNote(int pitch, long startTick, int velocity, int channel) {
            this.pitch = pitch;
            this.startTick = startTick;
            this.velocity = velocity;
            this.channel = channel;
        }
    }

    /** A tempo change event at a specific tick. */
    static class TempoEvent {
        final long tick;
        final int mpq; // microseconds per quarter note

        TempoEvent(long tick, int mpq) {
            this.tick = tick;
            this.mpq = mpq;
        }
    }
}
