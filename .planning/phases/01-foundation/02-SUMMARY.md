# Plan 02 Summary: MIDI Engine Core

**Status:** Complete
**Date:** 2026-04-27

## What Was Built

- 6 engine classes: NoteEventListener, NoteEventBus, SoundFontManager, MidiFileLoader, PlaybackController, RealtimeReceiver
- NoteEventBus — EDT-safe pub/sub with CopyOnWriteArrayList + SwingUtilities.invokeLater
- PlaybackController — full transport control (play/pause/stop/restart), BPM (20-300), volume (0-100%), seek, active note tracking via polling Timer
- RealtimeReceiver — live MIDI note trigger via Synthesizer.getReceiver(), shared with PlaybackController
- SoundFontManager — loads FluidR3_GM.sf2 from classpath if present, falls back to JDK default soundbank
- Orphaned NoteOn protection — 5-second auto-NoteOff via tick-based timeout

## Key Files Created

- `src/main/java/com/rebeatbox/engine/NoteEventListener.java`
- `src/main/java/com/rebeatbox/engine/NoteEventBus.java`
- `src/main/java/com/rebeatbox/engine/SoundFontManager.java`
- `src/main/java/com/rebeatbox/engine/MidiFileLoader.java`
- `src/main/java/com/rebeatbox/engine/PlaybackController.java`
- `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java`

## Deviations

None — all tasks executed as planned.

## Self-Check

- [x] All classes compile
- [x] NoteEventBus fires on EDT via SwingUtilities.invokeLater
- [x] PlaybackController + RealtimeReceiver share one Synthesizer instance
- [x] SoundFontManager has JDK fallback
- [x] Engine has zero Swing UI dependencies
