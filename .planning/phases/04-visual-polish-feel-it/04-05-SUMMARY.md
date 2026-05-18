---
phase: 04-visual-polish-feel-it
plan: 05
subsystem: ui-theming
tags: [theme-manager, drum-pads, keyboard-hint, placeholder, animation, timeline, neon]
dependency_graph:
  requires: [04-01]
  provides: [PadButton-animation, ThemeManager-migration]
  affects: [04-04]
tech-stack:
  added: [Radiance Timeline animation API 8.5.0]
  patterns: [Timeline-driven paintComponent, animated property interpolation, instant key highlight]
key-files:
  created: []
  modified:
    - src/main/java/com/rebeatbox/live/PadButton.java
    - src/main/java/com/rebeatbox/live/DrumPadGrid.java
    - src/main/java/com/rebeatbox/ui/KeyboardHintPanel.java
    - src/main/java/com/rebeatbox/ui/PlaceholderPanel.java
decisions:
  - "Kept HOVER_FILL (#25304A), PRESSED_FILL (#003344), DEFAULT_TEXT (#CCCCCC) as drum-pad-specific inline colors per plan rationale — ThemeManager doesn't define these as global constants"
  - "Used Spline (not SplineEase) per Radiance 8.5.0 actual API — plan referenced non-existent class name"
  - "Used Timeline.TimelineState (inner class) and callback.TimelineCallback (sub-package) per Radiance 8.5.0 actual API"
metrics:
  duration: "~14 minutes"
  completed_date: "2026-04-28"
---

# Phase 4 Plan 5: ThemeManager Color Migration + PadButton Animation Summary

**One-liner:** Migrated all hardcoded colors in 4 live-performance UI components to ThemeManager references and added Radiance Timeline hover/press animation with scale bounce to all 16 drum pads.

## Plan Execution

**Type:** execute (autonomous, no checkpoints)
**Wave:** 2
**Tasks:** 2/2 complete

### Task 1: PadButton — ThemeManager colors + Radiance Timeline hover/press animation
**Commit:** `655796f`

Replaced all 9 hardcoded Color constants in PadButton with ThemeManager references (BG_ELEVATED, BORDER_IDLE, accentForHue(HUE_DRUM_PADS), TEXT_ACCENT, TEXT_PRIMARY, DESTRUCTIVE). Three drum-pad-specific colors retained inline (HOVER_FILL #25304A, PRESSED_FILL #003344, DEFAULT_TEXT #CCCCCC) per plan rationale.

Implemented full Radiance Timeline hover/press animation (D-05, D-06, D-07):
- `animatedBorderColor`, `animatedFillColor`, `animatedScale` instance fields driven by Timeline interpolation
- `interpolateColor()` static helper for smooth Color transitions
- `animateBorderTo(target, durationMs)` — 200ms hover-in, 250ms hover-out with Spline easing
- `animateFillTo(target, durationMs)` — 75ms press flash, 150ms spring-back
- `animatePress()` / `animateRelease()` — 0.95x scale bounce with centered AffineTransform in paintComponent
- `setupMouseListener()` triggers Timeline animations instead of direct state changes
- Reset menu item uses `ThemeManager.DESTRUCTIVE` instead of `new Color(0xFF4444)`

### Task 2: DrumPadGrid, KeyboardHintPanel, PlaceholderPanel — ThemeManager migration
**Commit:** `38a1f79`

**DrumPadGrid:** `setBackground(new Color(0x16213E))` replaced with `ThemeManager.BG_SURFACE`. Single-line change.

**KeyboardHintPanel:** All 14 color constants (BG_COLOR, WHITE/BLACK_IDLE_*, PRESSED_*, KEY_LABEL_FONT, NOTE_LABEL_FONT) deleted and replaced with `ThemeManager.KEY_*` static references in `drawSingleKey()`. Background uses `ThemeManager.BG_ROOT`. Key label font changed from `new Font("SansSerif", Font.PLAIN, 11)` to `new Font("SansSerif", Font.PLAIN, 10)` per UI-SPEC typography contract. Unused `NOTE_LABEL_FONT` constant removed. D-31 preserved: `setKeyHighlighted()` still calls `repaint()` directly with no Timeline animation.

**PlaceholderPanel:** All 4 color references migrated to ThemeManager (BG_ELEVATED for background and gradient start, BG_SURFACE for gradient end, TEXT_PRIMARY for title, TEXT_SECONDARY for subtitle). Subtitle text updated from "Drop a .mid file or click ▶ to open" to "Drag and drop a .mid file or click the Open button" per UI-SPEC empty state copywriting. Subtitle font changed from 13pt to 12pt per UI-SPEC Body token.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed Radiance 8.5.0 import errors (package/class name corrections)**
- **Found during:** Task 1 compilation
- **Issue:** Plan specified imports for `SplineEase`, `TimelineCallback` (in `animation.api` package), and `TimelineState` (as top-level class) — none of which exist in Radiance 8.5.0
- **Fix:** Corrected imports to actual Radiance 8.5.0 API:
  - `org.pushingpixels.radiance.animation.api.ease.Spline` (not SplineEase)
  - `org.pushingpixels.radiance.animation.api.callback.TimelineCallback` (sub-package)
  - `org.pushingpixels.radiance.animation.api.Timeline.TimelineState` (inner class)
- **Files modified:** `PadButton.java`, `ControlBar.java`, `ReBeatBoxWindow.java`
- **Commit:** `655796f`
- **Note:** ControlBar.java and ReBeatBoxWindow.java fixes were pre-existing issues from other incomplete plans — fixed here as blocking compilation dependencies

**2. [Rule 2 - Missing Critical] Added sendNoteOn/sendNoteOff calls in Timeline-based setupMouseListener**
- **Found during:** Task 1 implementation
- **Issue:** Plan's revised `setupMouseListener()` template omitted `sendNoteOn()` and `sendNoteOff()` calls — drum pads would visually animate but produce no MIDI output
- **Fix:** Added `sendNoteOn()` call in `mousePressed()` and `sendNoteOff()` call in `mouseReleased()` — identical to original behavior, now alongside Timeline animation triggers
- **Files modified:** `PadButton.java`
- **Commit:** `655796f`

## Threat Model Compliance

| Threat ID | Category | Status |
|-----------|----------|--------|
| T-04-11 | DoS (Timeline congestion) | Mitigated — `hoverTimeline.abort()` and `pressTimeline.abort()` called before each new animation; max 2 Timelines per pad, 32 concurrent max for all 16 pads |
| T-04-12 | DoS (repaint flooding) | Accepted — `setKeyHighlighted()` calls `repaint()` directly per D-31; max 36 key changes per frame, well within EDT capacity |

## Known Stubs

None. All components wire to ThemeManager static fields and have functional paintComponent implementations. No placeholder values, TODO markers, or unconnected data sources remain in the four modified files.

## Verification

- `./gradlew compileJava` — BUILD SUCCESSFUL (all 4 files, zero compilation errors)
- `./gradlew test` — BUILD SUCCESSFUL (all existing tests pass, no regressions)
- All 11 must_have truths verified via grep acceptance criteria
- Zero `new Color(0x...)` hardcoded literals remain in DrumPadGrid, KeyboardHintPanel, or PlaceholderPanel
- PadButton retains only 3 drum-pad-specific inline colors (HOVER_FILL, PRESSED_FILL, DEFAULT_TEXT) per plan rationale

## Self-Check: PASSED

- `src/main/java/com/rebeatbox/live/PadButton.java` — Committed (655796f)
- `src/main/java/com/rebeatbox/live/DrumPadGrid.java` — Committed (38a1f79)
- `src/main/java/com/rebeatbox/ui/KeyboardHintPanel.java` — Committed (38a1f79)
- `src/main/java/com/rebeatbox/ui/PlaceholderPanel.java` — Committed (38a1f79)
- `.planning/phases/04-visual-polish-feel-it/04-05-SUMMARY.md` — Created (pending commit)

---

*Plan 04-05 executed: 2026-04-28*
