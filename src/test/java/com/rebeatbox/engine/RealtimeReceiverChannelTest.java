package com.rebeatbox.engine;

import javax.sound.midi.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RealtimeReceiverChannelTest {

    private static class FakeReceiver implements Receiver {
        MidiMessage lastMessage;
        long lastTimestamp;

        @Override
        public void send(MidiMessage msg, long ts) {
            lastMessage = msg;
            lastTimestamp = ts;
        }

        @Override
        public void close() {}
    }

    private FakeReceiver fake;
    private RealtimeReceiver receiver;

    private void setUpWithFake() {
        fake = new FakeReceiver();
        receiver = new RealtimeReceiver(fake);
    }

    @Test
    void shouldSendNoteOnChannel0() throws Exception {
        setUpWithFake();
        receiver.noteOn(60, 100, 0);
        assertNotNull(fake.lastMessage);
        assertTrue(fake.lastMessage instanceof ShortMessage);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(ShortMessage.NOTE_ON, sm.getCommand());
        assertEquals(0, sm.getChannel());
        assertEquals(60, sm.getData1());
        assertEquals(100, sm.getData2());
    }

    @Test
    void shouldSendNoteOnChannel10() throws Exception {
        setUpWithFake();
        receiver.noteOn(36, 100, 10);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(ShortMessage.NOTE_ON, sm.getCommand());
        assertEquals(10, sm.getChannel());
        assertEquals(36, sm.getData1());
        assertEquals(100, sm.getData2());
    }

    @Test
    void shouldSendNoteOffChannel0() throws Exception {
        setUpWithFake();
        receiver.noteOff(60, 0);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(ShortMessage.NOTE_ON, sm.getCommand());
        assertEquals(0, sm.getChannel());
        assertEquals(0, sm.getData2());
    }

    @Test
    void shouldSendNoteOffChannel10() throws Exception {
        setUpWithFake();
        receiver.noteOff(36, 10);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(ShortMessage.NOTE_ON, sm.getCommand());
        assertEquals(10, sm.getChannel());
        assertEquals(0, sm.getData2());
    }

    @Test
    void shouldKeepBackwardCompatNoteOn() throws Exception {
        setUpWithFake();
        receiver.noteOn(60, 100);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(0, sm.getChannel());
        assertEquals(60, sm.getData1());
        assertEquals(100, sm.getData2());
    }

    @Test
    void shouldKeepBackwardCompatNoteOff() throws Exception {
        setUpWithFake();
        receiver.noteOff(60);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(0, sm.getChannel());
        assertEquals(0, sm.getData2());
    }

    @Test
    void shouldNotCrashOnInvalidNoteForNewNoteOn() {
        setUpWithFake();
        assertDoesNotThrow(() -> receiver.noteOn(128, 100, 0));
    }

    @Test
    void shouldPreserveProgramChange() throws Exception {
        setUpWithFake();
        receiver.sendProgramChange(0, 42);
        ShortMessage sm = (ShortMessage) fake.lastMessage;
        assertEquals(ShortMessage.PROGRAM_CHANGE, sm.getCommand());
        assertEquals(0, sm.getChannel());
        assertEquals(42, sm.getData1());
    }
}
