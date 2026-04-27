package com.rebeatbox.visual;

import org.junit.jupiter.api.Test;

import javax.sound.midi.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MidiPreScannerTest {

    @Test
    void shouldReturnEmptyListForNullSequence() {
        List<RenderNote> notes = MidiPreScanner.scan(null);
        assertNotNull(notes);
        assertTrue(notes.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForSequenceWithNoTracks() throws Exception {
        Sequence seq = createSequenceWithResolution();
        // No tracks added — empty
        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertNotNull(notes);
        assertTrue(notes.isEmpty());
    }

    @Test
    void shouldPairSingleNoteOnAndNoteOff() throws Exception {
        Sequence seq = createSequenceWithResolution();
        Track track = seq.createTrack();

        // NOTE_ON: pitch=60, velocity=100, channel=0, tick=0
        track.add(createNoteOnEvent(60, 100, 0, 0));
        // NOTE_OFF: pitch=60, channel=0, tick=100
        track.add(createNoteOffEvent(60, 0, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(1, notes.size());
        RenderNote note = notes.get(0);
        assertEquals(60, note.pitch());
        assertEquals(100, note.velocity());
        assertEquals(0, note.channel());
        assertTrue(note.startMicros() >= 0);
        assertTrue(note.endMicros() > note.startMicros());
    }

    @Test
    void shouldSkipTrackZeroEvents() throws Exception {
        // Type 1 MIDI: track 0 = conductor, track 1+ = instrument data
        Sequence seq = createSequenceWithResolution();
        Track track0 = seq.createTrack(); // conductor track (index 0, always skipped)
        Track track1 = seq.createTrack(); // instrument track (index 1, processed)

        // Note on track 0 (should be skipped)
        track0.add(createNoteOnEvent(60, 100, 0, 0));
        track0.add(createNoteOffEvent(60, 0, 100));
        // Note on track 1 (should be processed)
        track1.add(createNoteOnEvent(72, 80, 0, 0));
        track1.add(createNoteOffEvent(72, 0, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        // Only track 1's note should appear; track 0 notes excluded
        assertEquals(1, notes.size(), "Track 0 notes should be excluded, only track 1 note present");
        assertEquals(72, notes.get(0).pitch(), "Only the track 1 note (pitch 72) should appear");
    }

    @Test
    void shouldProcessNonConductorTracks() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — conductor (empty)
        Track track1 = seq.createTrack(); // track 1 — note data

        track1.add(createNoteOnEvent(60, 100, 0, 0));
        track1.add(createNoteOffEvent(60, 0, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(1, notes.size());
        RenderNote note = notes.get(0);
        assertEquals(60, note.pitch());
    }

    @Test
    void shouldHandleNoteOnVelocityZeroAsNoteOff() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — skipped
        Track track1 = seq.createTrack();

        track1.add(createNoteOnEvent(60, 100, 0, 0)); // NoteOn
        track1.add(createNoteOnEvent(60, 0, 0, 100));  // NoteOn vel=0 = NoteOff

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(1, notes.size());
        assertEquals(60, notes.get(0).pitch());
        assertEquals(100, notes.get(0).velocity());
    }

    @Test
    void shouldHandleOrphanedNoteOnByUsingSequenceLength() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — skipped
        Track track1 = seq.createTrack();

        // NoteOn with no matching NoteOff (orphaned)
        track1.add(createNoteOnEvent(60, 100, 0, 0));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(1, notes.size());
        RenderNote note = notes.get(0);
        assertEquals(60, note.pitch());
        assertTrue(note.endMicros() >= note.startMicros(),
            "Orphaned note should have endMicros set to sequence length");
    }

    @Test
    void shouldSortByStartMicrosAscending() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — skipped
        Track track1 = seq.createTrack();

        // Note starting at tick 200 (should appear second in sorted list)
        track1.add(createNoteOnEvent(72, 100, 0, 200));
        track1.add(createNoteOffEvent(72, 0, 300));
        // Note starting at tick 0 (should appear first)
        track1.add(createNoteOnEvent(60, 100, 0, 0));
        track1.add(createNoteOffEvent(60, 0, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(2, notes.size());
        assertTrue(notes.get(0).startMicros() <= notes.get(1).startMicros(),
            "First note should start before or at same time as second: "
                + notes.get(0).startMicros() + " vs " + notes.get(1).startMicros());
    }

    @Test
    void shouldHandleDuplicateNoteOnAsImplicitNoteOff() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — skipped
        Track track1 = seq.createTrack();

        // Two NoteOn for same pitch+channel without NoteOff in between
        track1.add(createNoteOnEvent(60, 100, 0, 0));
        track1.add(createNoteOnEvent(60, 80, 0, 50));
        track1.add(createNoteOffEvent(60, 0, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(2, notes.size(),
            "Should create two notes: first (vel=100, tick=0-50), second (vel=80, tick=50-100)");
        // First note: pitch 60, velocity 100, from tick 0 to 50
        RenderNote n1 = notes.get(0);
        assertEquals(60, n1.pitch());
        assertEquals(100, n1.velocity());
        // Second note: pitch 60, velocity 80, from tick 50 to 100
        RenderNote n2 = notes.get(1);
        assertEquals(60, n2.pitch());
        assertEquals(80, n2.velocity());
    }

    @Test
    void shouldHandleOverlappingNotesOnDifferentPitches() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — skipped
        Track track1 = seq.createTrack();

        // Pitch 60 plays from tick 0 to 200
        track1.add(createNoteOnEvent(60, 100, 0, 0));
        // Pitch 64 starts at tick 100, overlaps with pitch 60
        track1.add(createNoteOnEvent(64, 100, 0, 100));
        // Pitch 60 ends at tick 200
        track1.add(createNoteOffEvent(60, 0, 200));
        // Pitch 64 ends at tick 300
        track1.add(createNoteOffEvent(64, 0, 300));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(2, notes.size());

        // Verify both notes exist
        boolean found60 = notes.stream().anyMatch(n -> n.pitch() == 60);
        boolean found64 = notes.stream().anyMatch(n -> n.pitch() == 64);
        assertTrue(found60);
        assertTrue(found64);
    }

    @Test
    void shouldHandleMultipleTracks() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — conductor, skipped
        Track track1 = seq.createTrack(); // melody
        Track track2 = seq.createTrack(); // bass

        track1.add(createNoteOnEvent(72, 100, 0, 0));
        track1.add(createNoteOffEvent(72, 0, 100));
        track2.add(createNoteOnEvent(48, 80, 1, 0));
        track2.add(createNoteOffEvent(48, 1, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(2, notes.size());
    }

    @Test
    void shouldDisambiguateSamePitchOnDifferentChannels() throws Exception {
        Sequence seq = createSequenceWithResolution();
        seq.createTrack(); // track 0 — skipped
        Track track = seq.createTrack();

        // Pitch 60 on channel 0
        track.add(createNoteOnEvent(60, 100, 0, 0));
        track.add(createNoteOffEvent(60, 0, 50));
        // Pitch 60 on channel 1 (same pitch, different channel)
        track.add(createNoteOnEvent(60, 80, 1, 0));
        track.add(createNoteOffEvent(60, 1, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        assertEquals(2, notes.size());
    }

    @Test
    void shouldHandleTypeZeroMidiFile() throws Exception {
        // Type 0: single track contains ALL events (notes + conductor)
        Sequence seq = new Sequence(Sequence.PPQ, 480);

        Track singleTrack = seq.createTrack();
        // Add a tempo meta message first (standard in Type 0)
        singleTrack.add(createNoteOnEvent(60, 100, 0, 0));
        singleTrack.add(createNoteOffEvent(60, 0, 100));

        List<RenderNote> notes = MidiPreScanner.scan(seq);
        // Track 0 is the ONLY track — should we skip it?
        // Per D-09: "Track 0 is skipped entirely"
        // But for Type 0 files, the entire music is in track 0.
        // The plan says to handle Type 0 files — so we need a special case:
        // if only 1 track exists, process it rather than returning empty.
        assertFalse(notes.isEmpty(),
            "Type 0 MIDI files (single track) should still yield notes");
    }

    // --- Helpers ---

    private Sequence createSequenceWithResolution() throws Exception {
        // PPQ (pulses per quarter note), 480 ticks per quarter note
        return new Sequence(Sequence.PPQ, 480);
    }

    private MidiEvent createNoteOnEvent(int pitch, int velocity, int channel, long tick) throws Exception {
        ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
        return new MidiEvent(msg, tick);
    }

    private MidiEvent createNoteOffEvent(int pitch, int channel, long tick) throws Exception {
        ShortMessage msg = new ShortMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
        return new MidiEvent(msg, tick);
    }
}
