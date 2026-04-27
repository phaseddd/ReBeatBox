---
phase: 02-visualization
plan: 01
subsystem: visualization-data-layer
tags: [midi, rendering, color-mapping, data-layer]
dependency_graph:
  provides:
    - RenderNote (immutable note record)
    - MidiPreScanner (MIDI Sequence → pre-sorted RenderNote list)
    - NoteColorMapper (pitch → neon Color)
  affects:
    - 02-02-piano-roll (consumes RenderNote list and NoteColorMapper)
tech_stack:
  added:
    - java.awt.Color (HSB color space)
    - javax.sound.midi (Sequence, Track, MidiEvent, ShortMessage, MetaMessage)
  patterns:
    - Java 17 record for immutable data
    - Static utility class with single entry point
    - TDD (RED/GREEN) on all three classes
key_files:
  created:
    - src/main/java/com/rebeatbox/visual/RenderNote.java
    - src/main/java/com/rebeatbox/visual/MidiPreScanner.java
    - src/main/java/com/rebeatbox/visual/NoteColorMapper.java
    - src/test/java/com/rebeatbox/visual/RenderNoteTest.java
    - src/test/java/com/rebeatbox/visual/MidiPreScannerTest.java
    - src/test/java/com/rebeatbox/visual/NoteColorMapperTest.java
decisions:
  - Java 17 record chosen for RenderNote (immutable by design, auto-generated equals/hashCode/toString)
  - Type 0 vs Type 1 detection heuristic: single track → process it; multi-track → skip track 0
  - Tempo map built from MetaMessage type 0x51 across ALL tracks including conductor track
  - NoteColorMapper uses linear hue mapping (0.70→0.0) with fixed saturation 0.85 and brightness 0.95
metrics:
  duration: "2m"
  completed_date: "2026-04-28"
---

# Phase 2 Plan 1: MIDI Visualization Data Foundation

One-liner: Pre-scan MIDI sequences into immutable, sorted note records with neon rainbow color mapping — the pure data layer feeding the piano roll renderer.

## Tasks Completed

| # | Type | Name | Commit | Status |
|---|------|------|--------|--------|
| 1 | auto (tdd) | RenderNote record + MidiPreScanner | `70f2ba3` (RED), `977ec80` (GREEN) | Done |
| 2 | auto (tdd) | NoteColorMapper pitch-to-color mapping | `67840c8` (RED), `2730c75` (GREEN) | Done |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed `shouldSkipTrackZeroEvents` test to create proper Type 1 scenario**
- **Found during:** Task 1 GREEN phase
- **Issue:** The original test created a single-track Sequence but expected track 0 notes to be skipped. With only 1 track, the code correctly treated it as Type 0 (processing the only track). The test needed a second track to exercise the Type 1 "skip track 0" behavior.
- **Fix:** Added a second track (track 1) with a distinct note; assertions now verify only track 1's note appears and track 0's notes are excluded.
- **Files modified:** `src/test/java/com/rebeatbox/visual/MidiPreScannerTest.java`
- **Commit:** `977ec80` (included in GREEN commit)

## Verification Results

| Criterion | Result |
|-----------|--------|
| `./gradlew compileJava` exits 0 | PASS |
| All unit tests pass (39 tests) | PASS |
| RenderNote is immutable (Java record) | PASS |
| MidiPreScanner returns empty list for null Sequence | PASS |
| NoteColorMapper distinguishes C4 (60) vs C5 (72) | PASS |
| Pre-scanned list sorted by startMicros | PASS |
| Track 0 events excluded (Type 1) | PASS |
| No Swing/UI imports in visual package | PASS |

## Decisions Made

- **Hue range for color mapper:** Chose 0.70→0.0 (purple through rainbow to red). This is Claude's discretion per D-05, matching the user's stated preference for "low=purple/blue, high=red/orange."
- **Saturation/Brightness constants:** 0.85 saturation, 0.95 brightness — vivid neon on black background without being harsh.
- **Type 0 detection heuristic:** `tracks.length == 1` treats the sole track as music data (not conductor). Type 0 MIDI files have exactly 1 track containing all events including conductor metadata mixed with notes.

## Known Stubs

None — all three classes are fully functional with no hardcoded placeholders, TODOs, or mock data paths.

## Threat Flags

None — all threat mitigations from the plan's threat model are implemented: RenderNote compact constructor validation (T-02-02), NoteColorMapper input validation (T-02-03). T-02-01 (DoS from malicious MIDI) is accepted risk per the threat model.

## Self-Check: PASSED

- `src/main/java/com/rebeatbox/visual/RenderNote.java` — FOUND
- `src/main/java/com/rebeatbox/visual/MidiPreScanner.java` — FOUND
- `src/main/java/com/rebeatbox/visual/NoteColorMapper.java` — FOUND
- `src/test/java/com/rebeatbox/visual/RenderNoteTest.java` — FOUND
- `src/test/java/com/rebeatbox/visual/MidiPreScannerTest.java` — FOUND
- `src/test/java/com/rebeatbox/visual/NoteColorMapperTest.java` — FOUND
- Commit `70f2ba3` (RED RenderNote) — FOUND
- Commit `977ec80` (GREEN RenderNote) — FOUND
- Commit `67840c8` (RED NoteColorMapper) — FOUND
- Commit `2730c75` (GREEN NoteColorMapper) — FOUND
