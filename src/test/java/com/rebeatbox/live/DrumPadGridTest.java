package com.rebeatbox.live;

import com.rebeatbox.engine.RealtimeReceiver;
import org.junit.jupiter.api.Test;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import java.awt.GridLayout;
import static org.junit.jupiter.api.Assertions.*;

class DrumPadGridTest {

    private static class FakeReceiver implements Receiver {
        MidiMessage lastMessage;
        long lastTimestamp;
        @Override public void send(MidiMessage msg, long ts) { lastMessage = msg; lastTimestamp = ts; }
        @Override public void close() {}
    }

    @Test
    void shouldCreate16Pads() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        assertEquals(16, grid.getComponentCount());
    }

    @Test
    void shouldDefaultPad0ToKick36() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        assertEquals(36, grid.getPadNote(0));
        assertEquals("Kick", grid.getPadLabel(0));
    }

    @Test
    void shouldDefaultPad1ToSnare38() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        assertEquals(38, grid.getPadNote(1));
    }

    @Test
    void shouldReassignPadSound() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        grid.setPadSound(0, 35, "Bass Drum 2");
        assertEquals(35, grid.getPadNote(0));
        assertEquals("Bass Drum 2", grid.getPadLabel(0));
    }

    @Test
    void shouldReturnCorrectDefaultNote() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        assertEquals(36, grid.getPadNote(0));
    }

    @Test
    void shouldReturnCorrectDefaultLabel() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        assertEquals("Kick", grid.getPadLabel(0));
    }

    @Test
    void shouldUseGridLayout4x4() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        assertTrue(grid.getLayout() instanceof GridLayout);
        GridLayout gl = (GridLayout) grid.getLayout();
        assertEquals(4, gl.getRows());
        assertEquals(4, gl.getColumns());
    }

    @Test
    void shouldHaveAllDefaultNotesInMidiRange() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        DrumPadGrid grid = new DrumPadGrid(rr);
        for (int i = 0; i < 16; i++) {
            int note = grid.getPadNote(i);
            assertTrue(note >= 0 && note <= 127,
                "Pad " + i + " has invalid note: " + note);
        }
    }
}
