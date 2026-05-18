---
phase: 04-visual-polish-feel-it
plan: 02
subsystem: visual
tags: [particle-system, glitch-effect, java2d, bufferedimage, rgb-separation, swing-glasspane]

# Dependency graph
requires:
  - phase: 04-visual-polish-feel-it
    provides: "NoteColorMapper.forPitch() for particle color mapping"
provides:
  - "ParticleSystem: 200-particle burst with off-screen accumulation and GlassPane rendering"
  - "GlitchTransition: RGB channel separation effect on BufferedImage snapshots"
affects: [04-04, 04-06]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Off-screen BufferedImage accumulation with AlphaComposite.Src clear + SrcOver draw"
    - "Swing Timer(16ms) animation loop with EDT-safe repaint"
    - "BufferedImage.getRGB/setRGB bulk pixel array manipulation"
    - "Package-private accessors for unit testing internal state (getParticleCount, getParticleSize)"

key-files:
  created:
    - src/main/java/com/rebeatbox/visual/ParticleSystem.java
    - src/test/java/com/rebeatbox/visual/ParticleSystemTest.java
    - src/main/java/com/rebeatbox/visual/GlitchTransition.java
    - src/test/java/com/rebeatbox/visual/GlitchTransitionTest.java
  modified: []

key-decisions:
  - "Particle merge algorithm selects 2 oldest by remaining lifetime, averages position/velocity/lifetime, sums size (capped 12px), keeps dominant color"
  - "Particle emission uses uniform random angle 0-360 and speed 80-200 px/s with +/-10px origin spread"
  - "GlitchTransition provides both 6-parameter full overload and 3-parameter convenience overload for common use"
  - "Test image uses horizontal (red) + vertical (green) gradient with constant blue for channel-shift verification"

patterns-established:
  - "Pattern 1: Off-screen accumulation — clear with AlphaComposite.Src, draw all particles with SrcOver, single drawImage blit. 1 AlphaComposite call instead of per-particle."
  - "Pattern 2: Pixel array bulk I/O — getRGB/setRGB for full-image channel manipulation, single allocation per call."
  - "Pattern 3: Cyber-block rendering — fillRect for body, drawRect with brighter color for glow border per D-08."

requirements-completed: [GLITCH-01, GLITCH-02]

# Metrics
duration: 10min
completed: 2026-04-28
---

# Phase 4 Plan 2: ParticleSystem + GlitchTransition Summary

**200-particle cyber-block burst system with off-screen GlassPane rendering and RGB channel separation glitch effect for component snapshots**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-28T11:05:45Z
- **Completed:** 2026-04-28T11:15:17Z
- **Tasks:** 2
- **Files created:** 4

## Accomplishments

- ParticleSystem.java (322 lines) — JComponent with off-screen BufferedImage accumulation, 200-particle cap, merge-oldest overflow, cyber-block rendering with glow borders, mouse-transparent contains(), lifetime/size proportional to MIDI velocity
- GlitchTransition.java (147 lines) — Static utility with 6-parameter and 3-parameter applyRgbSplit() overloads, red/blue channel shift with edge clamping, green alpha multiplier, configurable noise overlay
- 12 unit tests (6 + 6) covering emit lifecycle, lifetime range, size range, cap enforcement, mouse transparency, color correctness, channel shift verification, alpha preservation, dimension stability, edge clamping, green alpha reduction, and noise density
- Full test suite passes: `./gradlew test` exits 0

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ParticleSystem.java + ParticleSystemTest.java** - `08adfa8` (feat)
2. **Task 2: Create GlitchTransition.java + GlitchTransitionTest.java** - `73a974d` (feat)

## Files Created

- `src/main/java/com/rebeatbox/visual/ParticleSystem.java` — 200-particle burst system: inner Particle class, emit(note, velocity), mergeOldestParticles(), off-screen accumulation, cyber-block rendering, javax.swing.Timer(16ms)
- `src/test/java/com/rebeatbox/visual/ParticleSystemTest.java` — 6 tests: emit, lifetime range (Thread.sleep), size range, max cap, contains=false, color from NoteColorMapper
- `src/main/java/com/rebeatbox/visual/GlitchTransition.java` — RGB channel separation: 6-param applyRgbSplit (red/green/blue offsets, greenAlpha, noiseDensity), 3-param convenience overload, clamp helper
- `src/test/java/com/rebeatbox/visual/GlitchTransitionTest.java` — 6 tests: channel shift correctness, alpha preservation, dimensions, edge clamping, green alpha reduction, noise density (100% vs 0%)

## Decisions Made

- **Particle merge algorithm:** Select 2 oldest by remaining lifetime (ascending). Merged position is arithmetic mean. Merged size is sum capped at 12px. Merged color from particle with largest remaining lifetime. Merged velocity is vector average with preserved magnitude.
- **Particle emission trajectory:** Uniform random angle (0-360 deg), random speed (80-200 px/s), constant velocity (no gravity/acceleration). Random rotation speed (90-360 deg/s).
- **GlitchTransition overloads:** Full 6-parameter method for advanced use (green alpha dip, noise). 3-parameter convenience method for common case (just redOffsetX, blueOffsetX; green=1.0, noise=0.0).
- **Test image design:** Horizontal gradient in red channel (x*16), vertical gradient in green (y*16), constant blue=50. This allows precise verification of per-channel offset sampling.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Both classes compiled and all 12 tests passed on first attempt.

## Threat Flags

None. All security surface covered by plan's threat model (T-04-04 particle cap, T-04-05 edge clamping, T-04-06 offscreen reuse).

## Known Stubs

None. Both classes are fully functional — no placeholder values, no TODO/FIXME, no mock data paths.

## User Setup Required

None - no external service configuration required. Both classes use JDK built-in APIs (javax.swing, java.awt.image) with no additional dependencies.

## Next Phase Readiness

- ParticleSystem is ready for wiring in Plan 04-06 (ReBeatBoxWindow GlassPane integration) — public emit(note, velocity) and setEmitOrigin(x, y) are the only integration points needed
- GlitchTransition is ready for Timeline-driven use in Plans 04-04 and 04-06 — both overloads accept BufferedImage snapshots and return new images, suitable for Radiance Timeline property interpolation
- NoteColorMapper.forPitch() already provides particle colors (D-10 satisfied)
- Both classes are self-contained in com.rebeatbox.visual package with zero dependencies on other Phase 4 artifacts

## Self-Check: PASSED

- [x] `src/main/java/com/rebeatbox/visual/ParticleSystem.java` — exists
- [x] `src/test/java/com/rebeatbox/visual/ParticleSystemTest.java` — exists
- [x] `src/main/java/com/rebeatbox/visual/GlitchTransition.java` — exists
- [x] `src/test/java/com/rebeatbox/visual/GlitchTransitionTest.java` — exists
- [x] Commit `08adfa8` — Task 1 (ParticleSystem) confirmed in git log
- [x] Commit `73a974d` — Task 2 (GlitchTransition) confirmed in git log
- [x] `./gradlew test` — BUILD SUCCESSFUL, all 12 tests pass

---
*Phase: 04-visual-polish-feel-it*
*Completed: 2026-04-28*
