package com.rebeatbox.engine;

import java.util.Set;

@FunctionalInterface
public interface NoteEventListener {
    void onActiveNotesChanged(Set<Integer> activeNoteNumbers);
}
