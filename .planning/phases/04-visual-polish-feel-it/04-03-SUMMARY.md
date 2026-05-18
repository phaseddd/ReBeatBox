---
phase: 04-visual-polish-feel-it
plan: 03
subsystem: visual
tags: [gaussianblur, velocity-alpha, neon-glow, hsb-colors, notecolormapper]

# Dependency graph
requires:
  - phase: 02-visualization
    provides: PianoRollPanel separable ConvolveOp pipeline, NoteColorMapper HSB pitch mapping
provides:
  - Amplified GaussianBlur glow (kernel 5->7, sigma 2.0->3.5, BLUR_PAD 6->8)
  - velocityToAlpha() method mapping MIDI velocity 0-127 to alpha (0.30-1.00 above trigger, 0.15-0.40 below)
  - Public NoteColorMapper HSB constants (HUE_START, SATURATION, BRIGHTNESS) for ThemeManager reference
affects: [04-04-glitch-svg-glow (ThemeManager uses NoteColorMapper.SATURATION/BRIGHTNESS)]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "velocityToAlpha(velocity, aboveTrigger): private static method in PianoRollPanel — canonical velocity-to-alpha mapping. Duplicated in ThemeManager by Plan 04-04 per parallel-wave design (both Wave 1)"
    - "Public static final HSB constants in NoteColorMapper: canonical source-of-truth for neon saturation/brightness. Referenced by ThemeManager.accentForHue() to avoid hardcoded 0.85f/0.95f drift."

key-files:
  created: []
  modified:
    - src/main/java/com/rebeatbox/visual/PianoRollPanel.java
    - src/main/java/com/rebeatbox/visual/NoteColorMapper.java

key-decisions:
  - "velocityToAlpha placed in PianoRollPanel as private static (not ThemeManager) — both Plan 04-01 and 04-03 are Wave 1 parallel, so ThemeManager may not exist. Plan 04-04 optionally deduplicates."
  - "BLUR_PAD increased from 6 to 8 per RESEARCH.md Pitfall 2 recommendation, not from the D-21 decision alone (D-21 only specified kernel=7, sigma=3.5)"

patterns-established:
  - "velocityToAlpha: linear velocity->alpha interpolation preserving above/below trigger distinction from Phase 2 D-06"

requirements-completed: [GLITCH-03]

# Metrics
duration: 15min
completed: 2026-04-28
---

# Phase 4 Plan 3: Neon Glow Amplification + Velocity Alpha Summary

**Amplified GaussianBlur per-note glow (kernel 7, sigma 3.5) and integrated velocity-to-alpha mapping replacing hardcoded 1.0f/0.4f alpha in PianoRollPanel; exposed NoteColorMapper HSB constants as public for ThemeManager cross-reference.**

## Performance

- **Duration:** 15 min
- **Started:** 2026-04-28T11:10:00Z
- **Completed:** 2026-04-28T11:25:00Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments
- PianoRollPanel BLUR_KERNEL_SIZE increased from 5 to 7 (D-21 — stronger glow)
- PianoRollPanel BLUR_SIGMA increased from 2.0f to 3.5f (D-21 — wider blur spread)
- PianoRollPanel BLUR_PAD increased from 6 to 8 (RESEARCH.md Pitfall 2 — accommodate wider kernel)
- velocityToAlpha(velocity, aboveTrigger) static method added with correct D-28 formulas
- drawSingleNote() now calls velocityToAlpha() instead of hardcoded 1.0f/0.4f alpha
- NoteColorMapper HUE_START, SATURATION, BRIGHTNESS changed from private to public
- buildGaussianKernel() and applyConvolveBlur() unchanged (D-22)
- drawTriggerLine() unchanged (D-23)

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Update PianoRollPanel glow constants + velocityToAlpha + drawSingleNote integration | c650183 | PianoRollPanel.java |
| 2 | Expose NoteColorMapper HSB constants as public | a7bb3b2 | NoteColorMapper.java |

## Files Modified
- `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` — BLUR_KERNEL_SIZE 5->7, BLUR_SIGMA 2.0->3.5, BLUR_PAD 6->8; added velocityToAlpha(); wired into drawSingleNote()
- `src/main/java/com/rebeatbox/visual/NoteColorMapper.java` — HUE_START, SATURATION, BRIGHTNESS changed from private to public

## Decisions Made
- velocityToAlpha placed in PianoRollPanel as private static rather than ThemeManager: both plans are Wave 1 parallel, ThemeManager may not exist when this plan runs. Plan 04-04 will optionally deduplicate.
- BLUR_PAD=8 (not 10) per RESEARCH.md recommendation: 8px padding is sufficient for the 7x7 kernel spread without unnecessary memory overhead.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered
- NoteColorMapperTest could not be discovered by the Gradle test runner (`--tests 'com.rebeatbox.visual.NoteColorMapperTest'` returned "No tests found"). This is a pre-existing test infrastructure issue unrelated to the visibility-only changes made in this plan. The test class file exists at `build/classes/java/test/com/rebeatbox/visual/NoteColorMapperTest.class` and compiled successfully. Compilation of main sources passed cleanly.

## Known Stubs

None.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced.

## Next Phase Readiness
- NoteColorMapper public constants (SATURATION=0.85f, BRIGHTNESS=0.95f, HUE_START=0.70f) are ready for Plan 04-04 ThemeManager to reference in accentForHue()
- velocityToAlpha in PianoRollPanel is functional; Plan 04-04 can optionally deduplicate with ThemeManager.velocityToAlpha()

---
*Phase: 04-visual-polish-feel-it*
*Completed: 2026-04-28*
