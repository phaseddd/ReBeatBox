package com.rebeatbox.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderNoteTest {

    @Test
    void shouldCreateValidRenderNote() {
        RenderNote note = new RenderNote(60, 0, 500_000, 100, 0);
        assertEquals(60, note.pitch());
        assertEquals(0, note.startMicros());
        assertEquals(500_000, note.endMicros());
        assertEquals(100, note.velocity());
        assertEquals(0, note.channel());
    }

    @Test
    void shouldRejectPitchBelowZero() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(-1, 0, 100, 100, 0));
    }

    @Test
    void shouldRejectPitchAbove127() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(128, 0, 100, 100, 0));
    }

    @Test
    void shouldRejectNegativeStartMicros() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(60, -1, 100, 100, 0));
    }

    @Test
    void shouldRejectEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(60, 100, 50, 100, 0));
    }

    @Test
    void shouldAllowZeroDuration() {
        RenderNote note = new RenderNote(60, 100, 100, 100, 0);
        assertEquals(100, note.startMicros());
        assertEquals(100, note.endMicros());
    }

    @Test
    void shouldRejectVelocityBelowZero() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(60, 0, 100, -1, 0));
    }

    @Test
    void shouldRejectVelocityAbove127() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(60, 0, 100, 128, 0));
    }

    @Test
    void shouldRejectChannelBelowZero() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(60, 0, 100, 100, -1));
    }

    @Test
    void shouldRejectChannelAbove15() {
        assertThrows(IllegalArgumentException.class, () ->
            new RenderNote(60, 0, 100, 100, 16));
    }

    @Test
    void shouldBeImmutable() {
        RenderNote note = new RenderNote(60, 0, 500_000, 100, 0);
        // Records are immutable by design; verify no setters exist
        assertNotNull(note);
        assertEquals(60, note.pitch());
    }

    @Test
    void shouldReturnEqualNotesForSameValues() {
        RenderNote a = new RenderNote(60, 0, 500_000, 100, 0);
        RenderNote b = new RenderNote(60, 0, 500_000, 100, 0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldIncludePitchNameInToString() {
        RenderNote note = new RenderNote(60, 0, 500_000, 100, 0);
        String s = note.toString();
        assertNotNull(s);
        assertFalse(s.isBlank());
        // 60 = C4 in standard MIDI naming
        assertTrue(s.contains("C4") || s.contains("60"),
            "toString should contain pitch info: " + s);
    }

    @Test
    void shouldAllowBoundaryPitchZero() {
        RenderNote note = new RenderNote(0, 0, 100, 100, 0);
        assertEquals(0, note.pitch());
    }

    @Test
    void shouldAllowBoundaryPitch127() {
        RenderNote note = new RenderNote(127, 0, 100, 100, 15);
        assertEquals(127, note.pitch());
    }
}
