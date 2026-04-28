---
phase: 04-visual-polish-feel-it
plan: 04
subsystem: ui-theming
tags: [theme-manager, svg-icons, radiance-timeline, glitch-transition, button-animation]
requires: [04-01, 04-02]
provides:
  - ControlBar with ThemeManager colors, SVG icons, Timeline hover/press animations
  - SidebarPanel with GlitchTransition bidirectional toggle, SVG icons, Timeline animations
  - PianoRollPanel BG_ROOT background
affects:
  - src/main/java/com/rebeatbox/ui/ControlBar.java
  - src/main/java/com/rebeatbox/ui/SidebarPanel.java
  - src/main/java/com/rebeatbox/visual/PianoRollPanel.java
tech-stack:
  added: []
  patterns:
    - "Radiance Timeline.builder() with Spline easing for border-color interpolation"
    - "Component snapshot + Timeline-driven GlitchTransition.applyRgbSplit for sidebar toggle"
    - "SvgIconLoader.getIcon() replacing hardcoded unicode text on all buttons"
key-files:
  created: []
  modified:
    - src/main/java/com/rebeatbox/ui/ControlBar.java
    - src/main/java/com/rebeatbox/ui/SidebarPanel.java
    - src/main/java/com/rebeatbox/visual/PianoRollPanel.java
decisions: []
metrics:
  duration: ""
  completed_date: "2026-04-28"
---

# Phase 04 Plan 04: ControlBar, SidebarPanel, and PianoRollPanel Theme/SVG/Animation Integration

**One-liner:** Integrated ThemeManager colors, SVG icons via SvgIconLoader, and Radiance Timeline hover/press animations into ControlBar and SidebarPanel, plus bidirectional GlitchTransition for sidebar toggle and BG_ROOT background for PianoRollPanel.

## Execution Summary

All three tasks executed atomically with individual commits. Build (`./gradlew compileJava`) and test suite (`./gradlew test`) both pass. No regressions.

### Task 1: ControlBar -- ThemeManager + SVG + Timeline

- Replaced 4 hardcoded `new Color(0x...)` with ThemeManager constants (BG_SURFACE, TEXT_PRIMARY for labels)
- Replaced all unicode text icons (play/pause/stop/restart) with SVG icons via `SvgIconLoader.getIcon()`
- Replaced open button unicode with `SvgIconLoader.getIcon("open-file", 38)`
- Added `createTransportButton()` accepting icon name + tooltip, setting BG_ELEVATED background, BORDER_IDLE border, SVG icon, and accessible name
- Added `wireButtonAnimation()` wiring Radiance Timeline hover border-color (200ms in / 250ms out) and press scale (0.95x over 75ms / spring-back 150ms) on all 5 transport buttons + open button
- Added `interpolateColor()` static helper for smooth border color transitions
- Set progress bar foreground to `accentForHue(HUE_TRANSPORT)`
- Added accessible names via `setAccessibleName()` per UI-SPEC Copywriting Contract

### Task 2: SidebarPanel -- GlitchTransition + SVG + Timeline

- Replaced 2 hardcoded `new Color(0x16213e)` with `ThemeManager.BG_SURFACE`
- Replaced unicode toggle icons with SVG `collapse-left` / `expand-right` via `SvgIconLoader.getIcon()`
- Replaced `toggle()` with bidirectional GlitchTransition:
  - `performGlitchCollapse()`: captures component snapshot, runs 150ms RGB split via `GlitchTransition.applyRgbSplit()` with bell-curve offset (max 15px), stores snapshot for expand glitch
  - `performGlitchExpand()`: uses stored pre-collapse snapshot as source, runs 150ms RGB split, flushes snapshot on completion
  - Fallback to instant toggle if no snapshot available (first launch expand) or zero-size component
- Added `wireToggleAnimation()` with Radiance Timeline hover border-color and press scale on toggle button (HUE_SIDEBAR)
- Added `paintComponent()` override rendering glitch image during transitions, delegating to `super.paintComponent()` otherwise
- Added `updateToggleIcon()` to refresh SVG icon and accessible name on state change
- Added `interpolateColor()` static helper

### Task 3: PianoRollPanel -- BG_ROOT Background

- Changed constructor `setBackground(Color.BLACK)` to `setBackground(ThemeManager.BG_ROOT)`
- Changed `paintComponent()` canvas fill `g2d.setColor(Color.BLACK)` to `g2d.setColor(ThemeManager.BG_ROOT)`
- Added `import com.rebeatbox.ui.ThemeManager`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Radiance 8.5.0 API class names differ from plan specification**

- **Found during:** Task 1 compilation
- **Issue:** The plan specified `TimelineCallback` in package `org.pushingpixels.radiance.animation.api`, `TimelineState` as a top-level class, and `SplineEase` as an easing class. Radiance 8.5.0 uses:
  - `TimelineCallback` in `org.pushingpixels.radiance.animation.api.callback` (subpackage)
  - `TimelineState` as inner class `Timeline.TimelineState`
  - `Spline` as the easing class (not `SplineEase`)
- **Fix:** Corrected imports and class references in both ControlBar.java and SidebarPanel.java (`SplineEase` -> `Spline`, callback import path, `Timeline.TimelineState` inner class)
- **Files modified:** `ControlBar.java`, `SidebarPanel.java`
- **Commits:** `a7d9de3` (ControlBar, incorporated in initial commit), `9440202` (SidebarPanel, correct from start)

## Commits

| Hash | Type | Message |
|------|------|---------|
| `a7d9de3` | feat | add ThemeManager colors, SVG icons, and Radiance Timeline hover/press animations to ControlBar |
| `9440202` | feat | add GlitchTransition toggle, SVG icons, and Timeline animations to SidebarPanel |
| `da30515` | feat | replace PianoRollPanel Color.BLACK background with ThemeManager.BG_ROOT |

## Verification

- `./gradlew compileJava` -- BUILD SUCCESSFUL
- `./gradlew test` -- BUILD SUCCESSFUL
- Zero `new Color(0x...)` hardcoded literals in ControlBar.java
- Zero `new Color(0x...)` hardcoded literals in SidebarPanel.java
- Zero `Color.BLACK` references in PianoRollPanel.java
- Zero unicode text icons (play/pause/stop/restart/open/toggle) in any modified file
- All SVG icon names match existing resources in `src/main/resources/icons/`
- All buttons have accessible names via `setAccessibleName()`
- ThemeManager constants referenced in all three files
- Radiance Timeline animations on 6 ControlBar buttons + 1 SidebarPanel toggle

## Known Stubs

None. All functionality is fully wired with no placeholder data.

## Threat Flags

None. All threat model mitigations (T-04-09, T-04-10, T-04-10b) are properly implemented per plan:
- Snapshot BufferedImage bounded by component dimensions, flushed on completion
- Timeline animations aborted on state transitions to prevent EDT congestion
- Max 5 concurrent Timelines for ControlBar, 3 for SidebarPanel toggle

## Self-Check

- [x] `src/main/java/com/rebeatbox/ui/ControlBar.java` -- 373 lines, ThemeManager + SVG + Timeline
- [x] `src/main/java/com/rebeatbox/ui/SidebarPanel.java` -- 348 lines, GlitchTransition + SVG + Timeline
- [x] `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` -- ThemeManager.BG_ROOT in 2 locations
- [x] All 3 commits verified in git log
- [x] `./gradlew compileJava` exits 0
- [x] `./gradlew test` exits 0

## Self-Check: PASSED
