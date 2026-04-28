package com.rebeatbox.engine;

import javax.sound.midi.*;

public class RealtimeReceiver {
    private final Receiver receiver;

    public RealtimeReceiver(Synthesizer synthesizer) throws MidiUnavailableException {
        this.receiver = synthesizer.getReceiver();
    }

    RealtimeReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public void noteOn(int noteNumber, int velocity) {
        try {
            ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, noteNumber, velocity);
            receiver.send(msg, -1);
        } catch (InvalidMidiDataException e) {
            System.err.println("Invalid MIDI noteOn: note=" + noteNumber + " velocity=" + velocity);
        }
    }

    public void noteOff(int noteNumber) {
        try {
            ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, noteNumber, 0);
            receiver.send(msg, -1);
        } catch (InvalidMidiDataException e) {
            System.err.println("Invalid MIDI noteOff: note=" + noteNumber);
        }
    }

    public void noteOn(int noteNumber, int velocity, int channel) {
        try {
            ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
            receiver.send(msg, -1);
        } catch (InvalidMidiDataException e) {
            System.err.println("Invalid MIDI noteOn: note=" + noteNumber
                + " velocity=" + velocity + " channel=" + channel);
        }
    }

    public void noteOff(int noteNumber, int channel) {
        try {
            ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, noteNumber, 0);
            receiver.send(msg, -1);
        } catch (InvalidMidiDataException e) {
            System.err.println("Invalid MIDI noteOff: note=" + noteNumber
                + " channel=" + channel);
        }
    }

    public void sendProgramChange(int channel, int program) {
        try {
            ShortMessage msg = new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0);
            receiver.send(msg, -1);
        } catch (InvalidMidiDataException e) {
            System.err.println("Invalid MIDI program change: channel=" + channel + " program=" + program);
        }
    }

    public void close() {
        if (receiver != null) {
            receiver.close();
        }
    }
}
