package com.rebeatbox.engine;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * Wraps a MIDI Receiver, scaling NOTE_ON velocity by a multiplier.
 * Only NOTE_ON messages are modified; all other messages pass through unchanged.
 *
 * <p>This sits between the Sequencer and Synthesizer so the volume slider
 * controls only MIDI playback, not live keyboard/drum input.
 */
public class VolumeScaledReceiver implements Receiver {

    private final Receiver delegate;
    private volatile float scale = 1.0f; // 0.0 – 1.0

    public VolumeScaledReceiver(Receiver delegate) {
        this.delegate = delegate;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.0f, Math.min(1.0f, scale));
    }

    public float getScale() {
        return scale;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage sm && sm.getCommand() == ShortMessage.NOTE_ON) {
            int originalVelocity = sm.getData2();
            if (originalVelocity > 0) {
                int scaledVelocity = Math.round(originalVelocity * scale);
                if (scaledVelocity < 1) scaledVelocity = 1;
                try {
                    ShortMessage scaled = new ShortMessage(
                        sm.getCommand(), sm.getChannel(), sm.getData1(), scaledVelocity);
                    delegate.send(scaled, timeStamp);
                } catch (Exception e) {
                    delegate.send(message, timeStamp); // fallback
                }
                return;
            }
            // velocity == 0 is a NOTE_OFF — pass through unchanged
        }
        delegate.send(message, timeStamp);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
