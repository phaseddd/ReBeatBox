---
phase: 04-visual-polish-feel-it
plan: 01
subsystem: ui
tags: [java, swing, batik, svg, theming, neon, cyberpunk]

# Dependency graph
requires: []
provides:
  - ThemeManager singleton with 31+ semantic neon color constants and 2 utility methods (accentForHue, velocityToAlpha)
  - SvgIconLoader singleton with Batik ImageTranscoder, size-keyed BufferedImage cache, and preload() for all 12 icons
  - 12 cyber-styled SVG icon files in src/main/resources/icons/
  - build.gradle updated with 11 Batik 1.19 dependency declarations
affects: [04-02, 04-03, 04-04, 04-05, 04-06]

# Tech tracking
tech-stack:
  added:
    - |-
      Apache Batik 1.19 (batik-transcoder, batik-awt-util, batik-bridge,
      batik-dom, batik-ext, batik-gvt, batik-svg-dom, batik-util, batik-xml,
      batik-parser, batik-css)
  patterns:
    - "ThemeManager: final class with private constructor, all-static Color constants and utility methods — no instantiation"
    - "SvgIconLoader: two-level cache Map<String, Map<Integer, BufferedImage>> keyed by icon name and render width for identity-equality reuse"
    - "SVG design: stroke=currentColor (not hardcoded hex), glow via feGaussianBlur filter, miter joins, square caps"

key-files:
  created:
    - src/main/java/com/rebeatbox/ui/ThemeManager.java
    - src/test/java/com/rebeatbox/ui/ThemeManagerTest.java
    - src/main/java/com/rebeatbox/ui/SvgIconLoader.java
    - src/test/java/com/rebeatbox/ui/SvgIconLoaderTest.java
    - src/test/resources/icons/test-icon.svg
    - src/main/resources/icons/play.svg
    - src/main/resources/icons/pause.svg
    - src/main/resources/icons/stop.svg
    - src/main/resources/icons/restart.svg
    - src/main/resources/icons/open-file.svg
    - src/main/resources/icons/collapse-left.svg
    - src/main/resources/icons/expand-right.svg
    - src/main/resources/icons/app-icon.svg
    - src/main/resources/icons/bpm.svg
    - src/main/resources/icons/volume.svg
    - src/main/resources/icons/keyboard-mode.svg
    - src/main/resources/icons/drum-mode.svg
  modified:
    - build.gradle

key-decisions:
  - "Batik 1.19 individual module declarations (11 artifacts) instead of batik-all uber-jar — excludes batik-anim, batik-script, batik-svggen for read-only icon rendering"
  - "BORDER_HOVER and BORDER_PRESS computed via accentForHue(regionHue) at runtime rather than static constants — each UI region uses its own neon hue zone"
  - "preload() errors logged via System.err not thrown — icons load best-effort at startup"
  - "SVG icons use currentColor + glow filter; ThemeManager tints via JComponent foreground — no hardcoded colors in SVG paths"

patterns-established:
  - "ThemeManager accentForHue(float) wraps Color.getHSBColor(hue, 0.85f, 0.95f) — same constants as NoteColorMapper.forPitch()"
  - "velocityToAlpha(int, boolean) maps MIDI velocity 0-127 to alpha: 0.30-1.00 above trigger, 0.15-0.40 below trigger"
  - "SvgIconLoader.loadSvg(name, w, h) with cache-first strategy, classpath resource loading, and IllegalArgumentException for missing resources"

requirements-completed: [UI-02, GLITCH-04]

# Metrics
duration: 17min
completed: 2026-04-28
---

# Phase 4 Plan 1: ThemeManager + SvgIconLoader + SVG Icons Summary

**Centralized neon color theming and Batik SVG icon loading infrastructure, delivering 31+ semantic color constants, 2 utility methods, and 12 cyber-styled icon assets for Phase 4 downstream plans.**

## Performance

- **Duration:** 17 min
- **Started:** 2026-04-28T11:02:45Z
- **Completed:** 2026-04-28T11:19:16Z
- **Tasks:** 4
- **Files created/modified:** 17

## Accomplishments

- ThemeManager with three-tier dark backgrounds (BG_ROOT/BG_SURFACE/BG_ELEVATED), 4 semantic hue zones, 14 keyboard state colors, text/border/destructive colors
- SvgIconLoader with Batik BufferedImageTranscoder inner class, two-level size-keyed cache, and preload() for all 12 application icons
- 11 Batik 1.19 dependency declarations added to build.gradle (batik-anim, batik-script, batik-svggen excluded)
- 12 cyber-styled SVG icons: transport controls (5), sidebar toggle chevrons (2), mode indicators (2), label adornments (2), app icon (1)
- 12 unit tests passing: 7 ThemeManager (colors, accentForHue, velocityToAlpha, keys, borders) + 5 SvgIconLoader (load, cache identity, different sizes, missing throws, getIcon)

## Task Commits

1. **Task 1: Add Batik dependencies to build.gradle** - `ba353cb` (chore)
2. **Task 2: Create ThemeManager.java + ThemeManagerTest.java** - `6c357ae` (feat)
3. **Task 3: Create SvgIconLoader.java + SvgIconLoaderTest.java + test fixture** - `3d975e0` (feat)
4. **Task 4: Create 12 application SVG icon resource files** - `f204f26` (feat)

## Files Created/Modified

**Created:**
- `src/main/java/com/rebeatbox/ui/ThemeManager.java` — 31+ static Color constants + accentForHue()/velocityToAlpha() utility methods
- `src/test/java/com/rebeatbox/ui/ThemeManagerTest.java` — 7 unit tests verifying all color hexes, HSB saturation, monotonic velocity mapping
- `src/main/java/com/rebeatbox/ui/SvgIconLoader.java` — Batik SVG rasterization with BufferedImageTranscoder inner class and two-level cache
- `src/test/java/com/rebeatbox/ui/SvgIconLoaderTest.java` — 5 unit tests for loading, caching, error handling, and getIcon()
- `src/test/resources/icons/test-icon.svg` — Minimal SVG fixture for unit tests
- `src/main/resources/icons/*.svg` — 12 cyber-styled application icons (play, pause, stop, restart, open-file, collapse-left, expand-right, app-icon, bpm, volume, keyboard-mode, drum-mode)

**Modified:**
- `build.gradle` — Added 11 Batik 1.19 dependency declarations after radiance-component line

## Decisions Made

- BORDER_HOVER and BORDER_PRESS are computed at runtime via `accentForHue(regionHue)` rather than as static Color constants — each UI region gets its own neon hue zone per D-02
- Batik modules declared individually (11 artifacts) rather than using batik-all uber-jar — excludes batik-anim, batik-script, batik-svggen per RESEARCH.md Pitfall 4
- SvgIconLoader.preload() catches and logs errors to System.err — icons load best-effort at startup without blocking application launch
- SVG icons use `stroke="currentColor"` so ThemeManager can tint them via JComponent foreground properties — no hardcoded colors in SVG paths

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed missing throws TranscoderException in test methods**
- **Found during:** Task 3 (SvgIconLoaderTest compilation)
- **Issue:** loadSvg() declares checked TranscoderException but test methods did not include throws clause, causing 6 compilation errors
- **Fix:** Added `throws TranscoderException` to testLoadTestIcon, testCacheReturnsSameInstance, testDifferentSizesDifferentInstances, testGetIconSquare. Wrapped loadSvg() in try/catch for testMissingIconThrows.
- **Files modified:** src/test/java/com/rebeatbox/ui/SvgIconLoaderTest.java
- **Committed in:** 3d975e0 (Task 3 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug)
**Impact on plan:** Trivial compilation fix. No scope creep. No architectural change.

## Issues Encountered

- SoundFont download timed out during gradient builds (network restricted environment) — non-blocking, app uses JDK default soundbank
- Gradle wrapper shell script (.gradlew) not present — used `java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain` directly
- PowerShell pwsh command failed in bash shell context — used Java-based Gradle wrapper invocation instead

## Self-Check: PASSED

- All 17 created files verified on disk
- All 4 task commits verified in git log
- Combined test suite: BUILD SUCCESSFUL (12 tests passing)

## Next Phase Readiness

- ThemeManager and SvgIconLoader are ready as foundational singletons for all remaining Phase 4 plans
- All 12 SVG icons are loadable via SvgIconLoader.preload() — downstream plans (04-02 through 04-06) can use getIcon() immediately
- No stubs or placeholders — all color constants, utility methods, and icon files are production-ready

---
*Phase: 04-visual-polish-feel-it*
*Completed: 2026-04-28*
