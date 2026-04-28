package com.rebeatbox.engine;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class NoteEventBus {
    private final List<NoteEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(NoteEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(NoteEventListener listener) {
        listeners.remove(listener);
    }

    public void fire(Set<Integer> activeNotes) {
        for (NoteEventListener listener : listeners) {
            SwingUtilities.invokeLater(() -> listener.onActiveNotesChanged(activeNotes));
        }
    }

    private final List<LiveNoteEventListener> liveListeners = new CopyOnWriteArrayList<>();

    public void subscribeLive(LiveNoteEventListener listener) {
        liveListeners.add(listener);
    }

    public void unsubscribeLive(LiveNoteEventListener listener) {
        liveListeners.remove(listener);
    }

    public void fireLiveNoteOn(int note, int velocity) {
        for (LiveNoteEventListener listener : liveListeners) {
            SwingUtilities.invokeLater(() -> listener.onLiveNoteOn(note, velocity));
        }
    }

    public void fireLiveNoteOff(int note) {
        for (LiveNoteEventListener listener : liveListeners) {
            SwingUtilities.invokeLater(() -> listener.onLiveNoteOff(note));
        }
    }
}
