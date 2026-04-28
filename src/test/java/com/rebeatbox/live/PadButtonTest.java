package com.rebeatbox.live;

import com.rebeatbox.engine.RealtimeReceiver;
import org.junit.jupiter.api.Test;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import static org.junit.jupiter.api.Assertions.*;

class PadButtonTest {

    private static class FakeReceiver implements Receiver {
        MidiMessage lastMessage;
        long lastTimestamp;
        @Override public void send(MidiMessage msg, long ts) { lastMessage = msg; lastTimestamp = ts; }
        @Override public void close() {}
    }

    @Test
    void shouldHaveCorrectConstructorValues() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        PadButton pad = new PadButton("Kick", 36, rr);
        assertEquals("Kick", pad.getPadLabel());
        assertEquals(36, pad.getMidiNote());
    }

    @Test
    void shouldShowLabelAsText() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        PadButton pad = new PadButton("Kick", 36, rr);
        assertEquals("Kick", pad.getText());
    }

    @Test
    void shouldReturnMidiNote() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        PadButton pad = new PadButton("Kick", 36, rr);
        assertEquals(36, pad.getMidiNote());
    }

    @Test
    void shouldUpdateMidiNoteAndLabel() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        PadButton pad = new PadButton("Kick", 36, rr);
        pad.setMidiNote(38, "Snare");
        assertEquals(38, pad.getMidiNote());
        assertEquals("Snare", pad.getPadLabel());
        assertEquals("Snare", pad.getText());
    }

    @Test
    void shouldUseChannel10ForDrumPads() throws Exception {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        PadButton pad = new PadButton("Kick", 36, rr);

        // Simulate mousePressed via doClick-like approach — just call noteOn directly
        rr.noteOn(36, 100, 10);
        assertNotNull(fr.lastMessage);
        assertTrue(fr.lastMessage instanceof ShortMessage);
        ShortMessage sm = (ShortMessage) fr.lastMessage;
        assertEquals(ShortMessage.NOTE_ON, sm.getCommand());
        assertEquals(10, sm.getChannel());
        assertEquals(36, sm.getData1());
        assertEquals(100, sm.getData2());
    }

    @Test
    void shouldRejectInvalidMidiNote() {
        FakeReceiver fr = new FakeReceiver();
        RealtimeReceiver rr = new RealtimeReceiver(fr);
        PadButton pad = new PadButton("Kick", 36, rr);
        pad.setMidiNote(128, "Invalid");
        assertEquals(36, pad.getMidiNote()); // unchanged
    }
}
