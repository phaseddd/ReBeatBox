---
phase: 03-live-performance
plan: 01
subsystem: engine
tags: [midi, event-bus, javax.sound.midi, receiver, edt]

requires: []
provides:
  - Channel-aware MIDI note sending (channel 0-15)
  - Live note event publish-subscribe bus
  - LiveNoteEventListener interface for real-time visual feedback
affects: [03-04-drum-pads, 03-05-integration]

tech-stack:
  added: []
  patterns:
    - "CopyOnWriteArrayList + SwingUtilities.invokeLater for thread-safe event dispatch (extended from existing NoteEventBus pattern)"
    - "Separate listener interface (LiveNoteEventListener) instead of modifying @FunctionalInterface (preserves backward compat)"

key-files:
  created:
    - src/main/java/com/rebeatbox/engine/LiveNoteEventListener.java
    - src/test/java/com/rebeatbox/engine/RealtimeReceiverChannelTest.java
    - src/test/java/com/rebeatbox/engine/NoteEventBusLiveTest.java
  modified:
    - src/main/java/com/rebeatbox/engine/RealtimeReceiver.java
    - src/main/java/com/rebeatbox/engine/NoteEventBus.java

key-decisions:
  - "Channel overload: added noteOn(note, velocity, channel) and noteOff(note, channel) as new methods — existing 2-param methods unchanged for backward compat"
  - "LiveNoteEventListener is NOT @FunctionalInterface — two abstract methods (onLiveNoteOn, onLiveNoteOff) always paired in live performance"
  - "Package-private constructor RealtimeReceiver(Receiver) for test injection with FakeReceiver"

patterns-established:
  - "Test FakeReceiver pattern: anonymous Receiver implementation captures last MidiMessage for assertion"
  - "EDT drain pattern: SwingUtilities.invokeAndWait(() -> {}) in test tear-down ensures async callbacks complete before assertions"

requirements-completed: [LIVE-03]

duration: 8min
completed: 2026-04-28
---

# Phase 3 Plan 01: Engine Extensions Summary

**Channel-aware MIDI sending with RealtimeReceiver overloads + LiveNoteEventListener interface + NoteEventBus live event support**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-28
- **Completed:** 2026-04-28
- **Tasks:** 2 (TDD: RED → GREEN)
- **Files modified:** 5 (2 created tests, 1 created interface, 2 modified)

## Accomplishments
- RealtimeReceiver now supports 3-parameter `noteOn(note, velocity, channel)` and `noteOff(note, channel)` — Channel 10 ready for drum pads
- Existing 2-parameter API unchanged — all Phase 1-2 callers compile without modification
- LiveNoteEventListener interface with onLiveNoteOn/onLiveNoteOff — ready for KeyboardHintPanel and PianoRollPanel subscription
- NoteEventBus extended with subscribeLive/unsubscribeLive/fireLiveNoteOn/fireLiveNoteOff — EDT-safe via CopyOnWriteArrayList + SwingUtilities.invokeLater
- 13 new tests (8 channel + 5 event bus) — 0 regressions in existing test suite

## Task Commits

1. **Task 1: Create engine test scaffold (RED)** - `d35e876` (test)
2. **Task 2: Implement channel overloads + LiveNoteEventListener + NoteEventBus live events (GREEN)** - `2dea260` (feat)

## Files Created/Modified
- `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java` — Added channel-parameter overloads + test constructor
- `src/main/java/com/rebeatbox/engine/LiveNoteEventListener.java` — New listener interface for live performance events
- `src/main/java/com/rebeatbox/engine/NoteEventBus.java` — Added live event subscribe/unsubscribe/fire methods
- `src/test/java/com/rebeatbox/engine/RealtimeReceiverChannelTest.java` — 8 tests validating channel overloads and backward compat
- `src/test/java/com/rebeatbox/engine/NoteEventBusLiveTest.java` — 5 tests validating live event bus subscribe/fire/EDT safety

## Decisions Made
- Channel overloads added as separate methods rather than modifying existing signatures — preserves Phase 1-2 binary compatibility
- LiveNoteEventListener kept as plain interface (not @FunctionalInterface) — two methods always paired, @FunctionalInterface would break with lambdas expecting single method
- Test injection via package-private constructor rather than reflection — cleaner, type-safe, follows existing codebase patterns

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- NoteEventBusLiveTest EDT timing: `SwingUtilities.invokeLater` dispatches asynchronously, test assertions fired before EDT callbacks completed. Fixed by adding `drainEdt()` helper using `SwingUtilities.invokeAndWait(() -> {})` after each fire call.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Channel-aware MIDI sending ready for drum pad Channel 10 use (Plan 03-04)
- Live event bus ready for KeyboardHintPanel/PianoRollPanel subscription (Plan 03-05)
- All engine changes are backward-compatible — no risk to Phase 1-2 functionality

---
*Phase: 03-live-performance*
*Completed: 2026-04-28*
