package com.rebeatbox.engine;

public interface LiveNoteEventListener {
    void onLiveNoteOn(int note, int velocity);
    void onLiveNoteOff(int note);
}
