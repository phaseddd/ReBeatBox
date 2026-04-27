---
phase: 02-visualization
plan: 02
type: execute
subsystem: visualization
tags: [piano-roll, note-rendering, gaussian-blur, animation, integration]
requires: [02-01]
provides: [PianoRollPanel, ReBeatBoxWindow-integration]
tech-stack:
  added: [Java2D BufferedImage, ConvolveOp, javax.swing.Timer]
  patterns: [three-layer compositing, separable GaussianBlur, binary-search viewport culling, EDT animation loop]
key-files:
  created:
    - src/main/java/com/rebeatbox/visual/PianoRollPanel.java (672 lines)
  modified:
    - src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java
decisions:
  - "48px mini keyboard width, 18% trigger line position from bottom"
  - "5x5 GaussianBlur kernel with sigma 2.0 via separable two-pass ConvolveOp"
  - "Per-note BufferedImage allocation every frame (quality over performance per D-07)"
  - "Sine-wave pulse animation for trigger line (0.80-1.00 opacity)"
  - "Beat-interval grid lines at near-invisible alpha=15 (D-07 compliant)"
  - "Animation loop via javax.swing.Timer(16ms) polling controller directly (not NoteEventBus)"
duration:
  total_seconds: 180
  tasks: 3
completed_date: 2026-04-28
---

# Phase 2 Plan 2: Piano Roll Visualization Summary

**Synthesia-style falling-notes piano roll with per-note GaussianBlur glow and adaptive mini keyboard, wired into ReBeatBoxWindow replacing PlaceholderPanel.**

## What Was Built

PianoRollPanel.java is a full three-layer composited piano roll JPanel (672 lines) rendering MIDI notes as neon glow bars falling toward a pulsing trigger line, with a mini keyboard reference on the left side. ReBeatBoxWindow.java was updated to replace the Phase 1 PlaceholderPanel with PianoRollPanel in CENTER.

### Task 1: PianoRollPanel Skeleton & Mini Keyboard

Created the panel structure, coordinate system, and static mini keyboard rendering. The panel establishes:
- Pure black canvas background (`Color.BLACK`) per D-07
- Coordinate helpers: `triggerLineY()` (18% from bottom), `pitchToX()` (adaptive pitch grid), `timeToY()` (microsecond-to-pixel mapping)
- Mini keyboard silhouette with correct piano key geometry (white/black keys, octave separators)
- Default note range A0-C8, ready for adaptive scaling in Task 3

**Commit:** `582127e`

### Task 2: Note Rendering Pipeline

Added the full three-layer compositing architecture:
- **Layer 1:** Pure black fill (from Task 1)
- **Layer 2 (Notes):** Per-note `BufferedImage` with separable 5x5 GaussianBlur via two-pass `ConvolveOp` (horizontal then vertical kernel), alpha-composited onto the main canvas. Notes below the trigger line render at 0.4 alpha (D-06). Notes spanning the trigger line are split into two segments with appropriate alpha.
- **Layer 3 (Foreground):** Neon trigger line with 2px core + 8px cyan glow halo; subtle beat-interval grid lines (alpha=15); mini keyboard (from Task 1)
- Viewport culling via `Collections.binarySearch` on startMicros-sorted `List<RenderNote>` (D-11)
- Column-per-semitone bar geometry, chords render as adjacent bars (D-04)
- `NoteColorMapper.forPitch()` integration for pitch-to-neon-color mapping (D-05)

**Commit:** `cfc1ed7`

### Task 3: Animation Loop, Pre-Scan Integration, ReBeatBoxWindow Wiring

Added the 60fps animation heartbeat and connected PianoRollPanel to real MIDI data:
- `javax.swing.Timer(16ms)` drives repaint loop, directly polling `controller.getMicrosecondPosition()` each frame (D-13)
- Trigger line pulse animation: sine-wave opacity oscillation between 0.80 and 1.00 (D-12)
- `onFileLoaded()`: triggers `MidiPreScanner.scan()` to pre-scan the sequence, computes adaptive note range with 1-semitone padding (D-02, D-10)
- `getSoundingNotes()`: determines currently-sounding notes by scanning pre-scanned RenderNote list -- zero dependency on NoteEventBus (D-15)
- `dispose()`: cleanly stops the animation timer

**ReBeatBoxWindow.java changes:**
- Field: `PlaceholderPanel placeholderPanel` replaced with `PianoRollPanel pianoRollPanel`
- Constructor: `new PianoRollPanel()`, added to `CENTER`
- `wireEngine()`: calls `pianoRollPanel.setController(controller)`
- `loadAndPlay()`: calls `pianoRollPanel.onFileLoaded()` after `controller.load()`
- Window close listener: calls `pianoRollPanel.dispose()` for resource cleanup
- Removed unused `PlaceholderPanel` import; added `PianoRollPanel` import

**Commit:** `98ba4b7`

## Verification

- All 3 tasks compile successfully with `./gradlew compileJava`
- No `TODO`, `FIXME`, or placeholder stubs in any file
- D-15 verified: PianoRollPanel does not import or reference NoteEventBus (only Javadoc mentions)
- PlaceholderPanel completely removed from ReBeatBoxWindow
- PianoRollPanel.java: 672 lines (exceeds 300+ minimum)

## Deviations from Plan

None -- plan executed exactly as written.

## Known Stubs

None -- all note rendering, animation, pre-scan, and integration code is fully wired. No placeholder data, no hardcoded empty values flowing to UI.

## Commits

| # | Hash | Message |
|---|------|---------|
| 1 | `582127e` | feat(02-visualization): add PianoRollPanel skeleton with coordinate system and mini keyboard |
| 2 | `cfc1ed7` | feat(02-visualization): add note rendering pipeline with GaussianBlur glow and trigger line |
| 3 | `98ba4b7` | feat(02-visualization): add animation loop, pre-scan integration, and wire into ReBeatBoxWindow |

## Self-Check

- [x] `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` exists (672 lines)
- [x] `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` modified (no PlaceholderPanel references)
- [x] Commit `582127e` exists (Task 1)
- [x] Commit `cfc1ed7` exists (Task 2)
- [x] Commit `98ba4b7` exists (Task 3)
- [x] All commits are sequential on `main` branch
- [x] No untracked files, no accidental deletions
- [x] D-15 compliance: No NoteEventBus import or reference in PianoRollPanel.java
