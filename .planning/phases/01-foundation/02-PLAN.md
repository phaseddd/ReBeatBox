---
wave: 2
depends_on: ["01-PLAN"]
files_modified:
  - src/main/java/com/rebeatbox/engine/MidiFileLoader.java
  - src/main/java/com/rebeatbox/engine/PlaybackController.java
  - src/main/java/com/rebeatbox/engine/NoteEventBus.java
  - src/main/java/com/rebeatbox/engine/RealtimeReceiver.java
  - src/main/java/com/rebeatbox/engine/SoundFontManager.java
  - src/main/java/com/rebeatbox/engine/NoteEventListener.java
requirements:
  - PLAY-02
  - PLAY-03
  - PLAY-04
  - PLAY-05
autonomous: true
---

# Plan 02: MIDI Engine Core

**Objective:** Build the complete MIDI playback engine — load .mid files, play/pause/stop/seek, control BPM and volume, expose event bus for future phases. Engine must compile and pass unit tests standalone (no UI wiring yet).

## Tasks

<task id="02-01" type="execute">
<objective>Create NoteEventListener interface and NoteEventBus</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-07, D-09 (event bus design, EDT guarantee)
- .planning/research/ARCHITECTURE.md § Thread Model (MIDI callback → EDT pattern)
</read_first>

<action>
1. Create `src/main/java/com/rebeatbox/engine/NoteEventListener.java`:
```java
package com.rebeatbox.engine;

import java.util.Set;

@FunctionalInterface
public interface NoteEventListener {
    void onActiveNotesChanged(Set<Integer> activeNoteNumbers);
}
```

2. Create `src/main/java/com/rebeatbox/engine/NoteEventBus.java`:
- `List<NoteEventListener> listeners` field
- `subscribe(NoteEventListener listener)` — add to list
- `unsubscribe(NoteEventListener listener)` — remove from list
- `fire(Set<Integer> activeNotes)` — iterate listeners, wrap each callback in `SwingUtilities.invokeLater(() -> listener.onActiveNotesChanged(activeNotes))`
- Thread-safe: synchronize on listeners list or use CopyOnWriteArrayList
</action>

<acceptance_criteria>
- `NoteEventListener.java` contains `@FunctionalInterface` and method `void onActiveNotesChanged(Set<Integer>)`
- `NoteEventBus.java` contains `fire` method that calls `SwingUtilities.invokeLater`
- `NoteEventBus.java` uses `CopyOnWriteArrayList<NoteEventListener>` or synchronized block
</acceptance_criteria>
</task>

<task id="02-02" type="execute">
<objective>Create SoundFontManager — load FluidR3_GM.sf2, initialize Synthesizer</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-13, D-14, D-15 (SoundFont decisions)
- .planning/research/PITFALLS.md § Pitfall 4 (SoundFont quality)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/engine/SoundFontManager.java`:
```java
package com.rebeatbox.engine;

import javax.sound.midi.*;
import java.io.InputStream;

public class SoundFontManager {
    private Synthesizer synthesizer;
    
    public Synthesizer initialize() throws Exception {
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        
        // Load FluidR3_GM.sf2 from classpath
        InputStream sf2Stream = getClass().getResourceAsStream("/soundfonts/FluidR3_GM.sf2");
        if (sf2Stream == null) {
            throw new RuntimeException("SoundFont not found: /soundfonts/FluidR3_GM.sf2");
        }
        
        // Load SoundFont into synthesizer
        Soundbank soundbank = MidiSystem.getSoundbank(sf2Stream);
        synthesizer.loadAllInstruments(soundbank);
        
        return synthesizer;
    }
    
    public Synthesizer getSynthesizer() { return synthesizer; }
    public void close() { if (synthesizer != null) synthesizer.close(); }
}
```
</action>

<acceptance_criteria>
- `SoundFontManager.java` exists and compiles
- `initialize()` method opens Synthesizer and loads SoundFont from `getResourceAsStream("/soundfonts/FluidR3_GM.sf2")`
- `close()` method calls `synthesizer.close()`
</acceptance_criteria>
</task>

<task id="02-03" type="execute">
<objective>Create MidiFileLoader — parse .mid files into Sequence</objective>

<read_first>
- .planning/research/PITFALLS.md § Pitfall 8 (MIDI format variants — Type 0 vs Type 1)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/engine/MidiFileLoader.java`:
```java
package com.rebeatbox.engine;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiFileLoader {
    public Sequence load(File file) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(file);
    }
}
```

Simple wrapper — the complexity is in Java's built-in MidiSystem. Keep it clean.
</action>

<acceptance_criteria>
- `MidiFileLoader.java` exists and compiles
- `load(File)` returns `MidiSystem.getSequence(file)`
</acceptance_criteria>
</task>

<task id="02-04" type="execute">
<objective>Create PlaybackController — full transport control with Sequencer</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-11 (PlaybackController API contract)
- .planning/research/ARCHITECTURE.md § MIDI Engine section (PlaybackController responsibilities)
- .planning/research/PITFALLS.md § Pitfall 1 (MIDI clock sync — use microsecond position)
- .planning/research/PITFALLS.md § Pitfall 6 (multi-track handling in v1 — skip Track 0 metadata)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/engine/PlaybackController.java`:

Fields:
- `Sequencer sequencer`
- `Synthesizer synthesizer` (shared instance from SoundFontManager)
- `NoteEventBus eventBus`
- `Sequence currentSequence`
- `enum State { STOPPED, PLAYING, PAUSED }`
- `float volume = 0.75f` (maps to MIDI controller 7, range 0.0-1.0)

Constructor: `PlaybackController(Synthesizer synth, NoteEventBus bus)` — gets shared instances injected.

Methods:
- `load(File midiFile)` — calls MidiFileLoader.load(), calls `sequencer.setSequence()`, resets position to 0, state → STOPPED
- `play()` — `sequencer.start()`, state → PLAYING
- `pause()` — `sequencer.stop()`, state → PAUSED (note: stop() preserves position, unlike close())
- `stop()` — `sequencer.stop()`, `sequencer.setTickPosition(0)`, state → STOPPED, send all-notes-off
- `restart()` — `sequencer.stop()`, `sequencer.setTickPosition(0)`, `sequencer.start()`, state → PLAYING
- `setBPM(int bpm)` — `sequencer.setTempoInBPM(bpm)`, clamp 20-300
- `setVolume(float vol)` — clamp 0.0-1.0, send MIDI CC 7 to all channels (0-15)
- `seek(long microsecondPosition)` — `sequencer.setMicrosecondPosition(microsecondPosition)`
- `getState()` — returns current State enum
- `getMicrosecondPosition()` — `sequencer.getMicrosecondPosition()`
- `getMicrosecondLength()` — `sequencer.getMicrosecondLength()`
- `getActiveNotes()` — returns `Set<Integer>` of currently active note numbers. Track internal `boolean[] activeNotes = new boolean[128]` updated by MetaEventListener.

Sequencer initialization:
- Open sequencer: `sequencer = MidiSystem.getSequencer(false)` (don't connect to default synth)
- Connect sequencer to shared synthesizer: `sequencer.getTransmitter().setReceiver(synthesizer.getReceiver())`
- Register MetaEventListener that ticks at ~10ms intervals to update activeNotes[] and fire NoteEventBus
- Auto-NoteOff protection: any NoteOn without NoteOff after 5 seconds → auto-fire NoteOff, log warning

MetaEventListener logic:
```
On each tick:
1. Get sequencer.getTickPosition() → current tick
2. Scan all tracks for NoteOn events <= current tick whose NoteOff has not occurred
3. Update activeNotes[] boolean array
4. Convert activeNotes[] to Set<Integer>
5. eventBus.fire(activeNoteSet)
6. Check orphaned notes: any NoteOn older than 5 seconds without NoteOff → auto-NoteOff + console.warn
```
</action>

<acceptance_criteria>
- `PlaybackController.java` contains `enum State { STOPPED, PLAYING, PAUSED }`
- `PlaybackController.java` contains methods: `play()`, `pause()`, `stop()`, `restart()`, `setBPM(int)`, `setVolume(float)`, `seek(long)`, `getState()`, `getMicrosecondPosition()`, `getMicrosecondLength()`, `getActiveNotes()`
- `setBPM` clamps to range 20-300
- `setVolume` clamps to range 0.0-1.0
- `getActiveNotes()` returns `Set<Integer>` (not null, not raw array)
- Sequencer connects to shared Synthesizer: `sequencer.getTransmitter().setReceiver(synthesizer.getReceiver())`
- Auto-NoteOff after 5 seconds for orphaned NoteOn events
</acceptance_criteria>
</task>

<task id="02-05" type="execute">
<objective>Create RealtimeReceiver — live MIDI note trigger</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-08, D-12 (RealtimeReceiver API, shared Synthesizer)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java`:

Fields:
- `Receiver receiver` (from synthesizer.getReceiver())
- `Synthesizer synthesizer`

Methods:
- `noteOn(int noteNumber, int velocity)` — ShortMessage(ShortMessage.NOTE_ON, 0, noteNumber, velocity) → receiver.send()
- `noteOff(int noteNumber)` — ShortMessage(ShortMessage.NOTE_ON, 0, noteNumber, 0) → receiver.send()
- `sendProgramChange(int channel, int program)` — ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0) → receiver.send()
- `close()` — receiver.close()

Note: MIDI channel 0 (first channel) is default for noteOn/noteOff. Channel can be configurable later.
Velocity range: 0-127 (0 = silent/noteOff, 127 = max). Default velocity 100 for noteOn.
</action>

<acceptance_criteria>
- `RealtimeReceiver.java` contains `noteOn(int, int)` and `noteOff(int)` methods
- `noteOn` uses `ShortMessage.NOTE_ON` with velocity parameter
- `noteOff` uses `ShortMessage.NOTE_ON` with velocity=0 (MIDI convention)
- `RealtimeReceiver` receives Synthesizer via constructor injection
</acceptance_criteria>
</task>

## Verification

- [ ] All 5 engine classes compile: `gradlew compileJava`
- [ ] PlaybackController correctly wraps javax.sound.midi.Sequencer
- [ ] NoteEventBus fires on EDT (SwingUtilities.invokeLater)
- [ ] SoundFontManager loads FluidR3_GM.sf2 from classpath
- [ ] RealtimeReceiver shares Synthesizer instance with PlaybackController
- [ ] Orphaned NoteOn protection: auto-NoteOff after 5 seconds
- [ ] Engine has zero Swing UI dependencies (pure logic, testable without GUI)

## must_haves

- `truths`: ["MIDI engine compiles and runs without UI dependencies", "PlaybackController exposes full transport control API", "NoteEventBus guarantees EDT delivery to subscribers", "RealtimeReceiver and PlaybackController share one Synthesizer instance", "Orphaned NoteOn events auto-resolve within 5 seconds"]
