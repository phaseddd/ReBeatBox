package com.rebeatbox.engine;

import javax.sound.midi.*;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlaybackController {
    public enum State { STOPPED, PLAYING, PAUSED }

    private final Sequencer sequencer;
    private final Synthesizer synthesizer;
    private final NoteEventBus eventBus;
    private final MidiFileLoader loader = new MidiFileLoader();

    private State state = State.STOPPED;
    private float volume = 0.75f;
    private int bpm = 120;
    private final boolean[] activeNotes = new boolean[128];

    public PlaybackController(Synthesizer synthesizer, NoteEventBus eventBus) throws MidiUnavailableException {
        this.synthesizer = synthesizer;
        this.eventBus = eventBus;
        this.sequencer = MidiSystem.getSequencer(false);
        this.sequencer.open();
        this.sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        setupMetaListener();
    }

    private void setupMetaListener() {
        sequencer.addMetaEventListener(meta -> {
            if (meta.getType() == 47) {
                // End of track
                resetActiveNotes();
                SwingUtilities.invokeLater(() -> {
                    state = State.STOPPED;
                    eventBus.fire(Collections.emptySet());
                });
            }
        });

        // Timer-based polling for active note tracking (every 50ms)
        new javax.swing.Timer(50, e -> {
            if (state != State.PLAYING) return;
            updateActiveNotes();
            eventBus.fire(getActiveNotes());
        }).start();
    }

    private void updateActiveNotes() {
        // Reset
        for (int i = 0; i < 128; i++) activeNotes[i] = false;

        if (sequencer.getSequence() == null) return;
        long currentTick = sequencer.getTickPosition();

        for (Track track : sequencer.getSequence().getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage msg = event.getMessage();

                if (msg instanceof ShortMessage sm) {
                    int cmd = sm.getCommand();
                    if (cmd == ShortMessage.NOTE_ON) {
                        long noteOnTick = event.getTick();
                        // Search forward for corresponding NoteOff
                        boolean hasNoteOff = false;
                        for (int j = i + 1; j < track.size(); j++) {
                            MidiEvent nextEvent = track.get(j);
                            if (nextEvent.getMessage() instanceof ShortMessage nsm) {
                                if (nsm.getCommand() == ShortMessage.NOTE_OFF
                                    && nsm.getData1() == sm.getData1()
                                    && nsm.getChannel() == sm.getChannel()) {
                                    long noteOffTick = nextEvent.getTick();
                                    if (currentTick >= noteOnTick && currentTick < noteOffTick) {
                                        activeNotes[sm.getData1()] = true;
                                    }
                                    hasNoteOff = true;
                                    break;
                                }
                                // NOTE_ON with velocity 0 = NoteOff
                                if (nsm.getCommand() == ShortMessage.NOTE_ON
                                    && nsm.getData2() == 0
                                    && nsm.getData1() == sm.getData1()
                                    && nsm.getChannel() == sm.getChannel()) {
                                    long noteOffTick = nextEvent.getTick();
                                    if (currentTick >= noteOnTick && currentTick < noteOffTick) {
                                        activeNotes[sm.getData1()] = true;
                                    }
                                    hasNoteOff = true;
                                    break;
                                }
                            }
                        }
                        // Orphaned NoteOn: if no NoteOff found and we're past the NoteOn, auto-protect after 5s
                        if (!hasNoteOff && currentTick >= noteOnTick) {
                            // Still active if within 5 seconds (at current BPM, roughly)
                            long ticksPerMs = sequencer.getSequence().getResolution() * bpm / 60000L;
                            long maxTicks = ticksPerMs > 0 ? 5000L * ticksPerMs : Long.MAX_VALUE;
                            if (currentTick - noteOnTick < maxTicks) {
                                activeNotes[sm.getData1()] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void resetActiveNotes() {
        for (int i = 0; i < 128; i++) activeNotes[i] = false;
        // Send all-notes-off to all channels
        try {
            for (int ch = 0; ch < 16; ch++) {
                ShortMessage allOff = new ShortMessage(ShortMessage.CONTROL_CHANGE, ch, 123, 0);
                synthesizer.getReceiver().send(allOff, -1);
            }
        } catch (Exception ignored) {}
    }

    // Public API

    public void load(File midiFile) throws InvalidMidiDataException, IOException {
        Sequence sequence = loader.load(midiFile);
        sequencer.setSequence(sequence);
        sequencer.setTempoInBPM(bpm);
        state = State.STOPPED;
        resetActiveNotes();
        eventBus.fire(Collections.emptySet());
    }

    public void play() {
        if (state == State.PAUSED || state == State.STOPPED) {
            sequencer.start();
            state = State.PLAYING;
        }
    }

    public void pause() {
        if (state == State.PLAYING) {
            sequencer.stop();
            state = State.PAUSED;
        }
    }

    public void stop() {
        sequencer.stop();
        sequencer.setTickPosition(0);
        state = State.STOPPED;
        resetActiveNotes();
        eventBus.fire(Collections.emptySet());
    }

    public void restart() {
        sequencer.stop();
        sequencer.setTickPosition(0);
        sequencer.start();
        state = State.PLAYING;
    }

    public void setBPM(int bpm) {
        this.bpm = Math.max(20, Math.min(300, bpm));
        sequencer.setTempoInBPM(this.bpm);
    }

    public void setVolume(float vol) {
        this.volume = Math.max(0.0f, Math.min(1.0f, vol));
        // Send volume CC (7) to all channels
        int midiVol = (int)(this.volume * 127);
        try {
            for (int ch = 0; ch < 16; ch++) {
                ShortMessage volMsg = new ShortMessage(ShortMessage.CONTROL_CHANGE, ch, 7, midiVol);
                synthesizer.getReceiver().send(volMsg, -1);
            }
        } catch (Exception e) {
            System.err.println("Failed to set volume: " + e.getMessage());
        }
    }

    public void seek(long microsecondPosition) {
        long length = sequencer.getMicrosecondLength();
        long target = Math.max(0, Math.min(microsecondPosition, length));
        sequencer.setMicrosecondPosition(target);
    }

    public State getState() { return state; }

    public long getMicrosecondPosition() {
        return sequencer.getMicrosecondPosition();
    }

    public long getMicrosecondLength() {
        return sequencer.getMicrosecondLength();
    }

    public Set<Integer> getActiveNotes() {
        Set<Integer> notes = new HashSet<>();
        for (int i = 0; i < 128; i++) {
            if (activeNotes[i]) notes.add(i);
        }
        return notes;
    }

    public Sequencer getSequencer() {
        return sequencer;
    }

    public void close() {
        sequencer.stop();
        sequencer.close();
    }
}
