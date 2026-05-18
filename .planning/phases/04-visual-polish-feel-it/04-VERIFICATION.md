---
phase: 04-visual-polish-feel-it
verified: 2026-04-28T20:30:00Z
status: human_needed
score: 48/48 must-haves verified
overrides_applied: 0
overrides: []
human_verification:
  - test: "Launch the application and verify all 12 SVG icons render correctly on ControlBar transport buttons (play, pause, stop, restart), open button, and SidebarPanel toggle (collapse-left, expand-right)"
    expected: "Icons render with neon glow, angular/cyber styling, no distortion at 38x38 (transport) or 24x24 (toggle) sizes"
    why_human: "SVG rasterization to BufferedImage cannot be visually verified via grep — Batik rendering fidelity, glow filter output, and icon visual design require human eyes"
  - test: "Play a MIDI file and observe particle bursts on note events"
    expected: "Cyber-block (square/rectangle) particles burst from keyboard visualization area with colors matching NoteColorMapper pitch-to-hue mapping, particles fade out with linear alpha decay, no frame drops visible"
    why_human: "Particle system rendering on GlassPane with 200-particle cap, off-screen accumulation, and 60fps timer requires visual inspection — static code analysis cannot verify animation smoothness or color correctness at runtime"
  - test: "Toggle the SidebarPanel collapse/expand and observe the glitch transition"
    expected: "150ms RGB channel separation effect — red channel shifts left, blue shifts right, bell-curve offset peaking at midpoint, clean transition back to stable rendering"
    why_human: "RGB channel separation glitch effect on BufferedImage snapshots with Timeline-driven bell-curve offset requires visual verification — pixel-level correctness is testable but aesthetic quality is subjective"
  - test: "Load a new MIDI file via File > Open and observe the file-load glitch transition"
    expected: "300ms RGB channel separation overlay on the PianoRollPanel via ParticleSystem GlassPane, max offset 25px, snapshot of pre-load visualization, smooth blend to post-load content"
    why_human: "File-load glitch transition involves snapshot capture, 300ms Timeline animation, and GlassPane overlay rendering — visual fidelity and timing require human verification"
  - test: "Hover over and press ControlBar transport buttons, open button, SidebarPanel toggle, and all 16 drum pads"
    expected: "Hover: border color smoothly transitions from BORDER_IDLE (#2A3A5E) to region-specific neon accent. Press: 0.95x scale bounce + color flash. Release: spring-back to idle. No stuck animations, no stuttering."
    why_human: "Radiance Timeline animation with Spline easing on 22 buttons requires visual inspection — animation smoothness, easing feel, and absence of EDT congestion cannot be programmatically verified"
  - test: "Press keyboard keys for live performance and observe KeyboardHintPanel key highlights"
    expected: "Pressed keys highlight instantly in neon cyan (#00E5FF) with no animation delay. Idle keys show low-alpha ThemeManager colors. Latency between key press and visual highlight is imperceptible."
    why_human: "D-31 requires instant color switch (no Timeline animation) for performance-critical live playing — perceived latency can only be evaluated by a human playing the keyboard"
  - test: "Verify neon glow visibility on fast/short notes in the piano roll during playback"
    expected: "Short/fast notes (e.g., 16th notes at high tempo) have visible GaussianBlur glow with the amplified kernel=7, sigma=3.5. Glow is noticeably more visible than the previous kernel=5, sigma=2.0."
    why_human: "GLITCH-03 specifically targets UAT finding 'amplify-blur-visibility' — the visual improvement on short/fast notes requires human comparison"
  - test: "Verify velocity-to-alpha mapping — high-velocity notes appear brighter than low-velocity notes in the piano roll"
    expected: "Notes with velocity=127 have alpha=1.00 above trigger line, notes with velocity=0 have alpha=0.30. Below trigger: velocity=127 gives alpha=0.40, velocity=0 gives alpha=0.15. Visual brightness distinction is clearly visible."
    why_human: "D-27/D-28/D-29 introduce velocity-driven alpha — visual distinction between soft and loud notes must be verified by human eyes"
  - test: "Overall cyber/glitch aesthetic coherency"
    expected: "The application looks like a unified cyberpunk music app — neon colors are consistent across all components, animations feel cohesive, the three-tier dark background hierarchy creates visual depth, and the SVG icons match the angular/geometric design language"
    why_human: "Phase 4 goal is 'Full cyber/glitch aesthetic' — subjective visual quality assessment requires human evaluation"
---

# Phase 4: Visual Polish — "Feel It" Verification Report

**Phase Goal:** Full cyber/glitch aesthetic — particles, glow, transitions, neon palette, SVG icons.
**Verified:** 2026-04-28T20:30:00Z
**Status:** human_needed (all automated checks PASS, 8 visual items need human testing)
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | ThemeManager provides BG_ROOT (#0A0A14), BG_SURFACE (#12122A), BG_ELEVATED (#1A1A3E) | VERIFIED | ThemeManager.java:19-25 — all three tiered background constants present |
| 2 | ThemeManager.accentForHue(0.55f) returns neon cyan-blue with sat=0.85, bri=0.95 | VERIFIED | ThemeManager.java:92-93 — `Color.getHSBColor(hue, 0.85f, 0.95f)` |
| 3 | ThemeManager.velocityToAlpha(127, true)=1.0f, velocityToAlpha(0, false)=0.15f | VERIFIED | ThemeManager.java:104-112 — D-28 formulas implemented |
| 4 | SvgIconLoader loads SVG from classpath and returns BufferedImage | VERIFIED | SvgIconLoader.java:66-107 — loadSvg with Batik ImageTranscoder |
| 5 | SvgIconLoader caches by icon name and size | VERIFIED | SvgIconLoader.java:52,104 — two-level HashMap cache |
| 6 | build.gradle has 11 batik-*:1.19 deps, excludes batik-anim/script/svggen | VERIFIED | build.gradle:33-43 — 11 deps, zero matches for excluded modules |
| 7 | ParticleSystem.emit(note, velocity) uses NoteColorMapper.forPitch(note) | VERIFIED | ParticleSystem.java:188 — `Color color = NoteColorMapper.forPitch(note)` |
| 8 | Particle lifetime 300ms (vel=0) to 800ms (vel=127), linear | VERIFIED | ParticleSystem.java:191 — `int lifetimeMs = 300 + (velocity * 500 / 127)` |
| 9 | Particle size 1px (vel=0) to 8px (vel=127), linear | VERIFIED | ParticleSystem.java:193 — `int size = 1 + (velocity * 7 / 127)` |
| 10 | 200-particle cap with merge-oldest overflow | VERIFIED | ParticleSystem.java:44,198-199 — `MAX_PARTICLES=200`, `mergeOldestParticles()` |
| 11 | Off-screen BufferedImage accumulator, single blit to GlassPane | VERIFIED | ParticleSystem.java:134 — `BufferedImage offscreen` field, paintComponent renders offscreen then single drawImage blit |
| 12 | contains() returns false — GlassPane mouse-transparent | VERIFIED | ParticleSystem.java:258-259 — `return false` with comment |
| 13 | GlitchTransition.applyRgbSplit() separates RGB channels with offset | VERIFIED | GlitchTransition.java:56-112 — full pixel array manipulation with r/g/b offsets |
| 14 | GlitchTransition preserves alpha channel | VERIFIED | GlitchTransition.java:86 — `int a = (srcPixel >> 24) & 0xFF` preserved |
| 15 | PianoRollPanel BLUR_KERNEL_SIZE=7 (was 5) | VERIFIED | PianoRollPanel.java:64 |
| 16 | PianoRollPanel BLUR_SIGMA=3.5f (was 2.0f) | VERIFIED | PianoRollPanel.java:67 |
| 17 | PianoRollPanel BLUR_PAD=8 (was 6) | VERIFIED | PianoRollPanel.java:71 |
| 18 | drawSingleNote() calls velocityToAlpha() instead of hardcoded alpha | VERIFIED | PianoRollPanel.java:404,410,414 — all three paths use velocityToAlpha |
| 19 | NoteColorMapper exposes HUE_START, SATURATION, BRIGHTNESS as public | VERIFIED | NoteColorMapper.java:18,24,27 — all three `public static final` |
| 20 | ControlBar background uses ThemeManager.BG_SURFACE | VERIFIED | ControlBar.java:30 — `setBackground(ThemeManager.BG_SURFACE)` |
| 21 | ControlBar transport buttons display SVG icons, not unicode | VERIFIED | ControlBar.java — SVG icon names "restart"/"play"/"pause"/"stop", zero unicode chars (▶⏸⏹⏮📂) |
| 22 | ControlBar buttons have Timeline hover border-color animation to HUE_TRANSPORT accent | VERIFIED | ControlBar.java:265-362 — `wireButtonAnimation()` with 200ms/250ms Spline Timeline |
| 23 | ControlBar buttons have 0.95x press scale animation | VERIFIED | ControlBar.java:319-353 — pressDown/preUp Timeline at 75ms/150ms |
| 24 | SidebarPanel background uses ThemeManager.BG_SURFACE | VERIFIED | SidebarPanel.java:32 — `setBackground(ThemeManager.BG_SURFACE)` |
| 25 | SidebarPanel toggle uses SVG icons (collapse-left/expand-right) | VERIFIED | SidebarPanel.java:45,218 — `SvgIconLoader.getIcon(iconName, 24)` |
| 26 | SidebarPanel toggle has Timeline hover/press animation | VERIFIED | SidebarPanel.java:233-311 — `wireToggleAnimation()` with HUE_SIDEBAR |
| 27 | SidebarPanel toggle triggers GlitchTransition on BOTH collapse and expand | VERIFIED | SidebarPanel.java:95-148 (performGlitchCollapse), 160-192 (performGlitchExpand) — both call `GlitchTransition.applyRgbSplit()` with 150ms Timeline |
| 28 | PianoRollPanel background uses ThemeManager.BG_ROOT (#0A0A14) | VERIFIED | PianoRollPanel.java:111,297 — both constructor and paintComponent use `ThemeManager.BG_ROOT` |
| 29 | No hardcoded new Color(0x...) in ControlBar, SidebarPanel | VERIFIED | Zero matches in both files |
| 30 | PadButton uses ThemeManager.BG_ELEVATED/BORDER_IDLE/HUE_DRUM_PADS | VERIFIED | PadButton.java:49-50,52 — ThemeManager constants |
| 31 | PadButton has Timeline hover/press animation with animatedBorderColor/FillColor/Scale | VERIFIED | PadButton.java:60-62 (fields), 168-234 (animate* methods), 251-258 (paintComponent integration) |
| 32 | DrumPadGrid background uses ThemeManager.BG_SURFACE | VERIFIED | DrumPadGrid.java:33 |
| 33 | KeyboardHintPanel uses ThemeManager KEY_IDLE_*/KEY_PRESSED_* colors | VERIFIED | KeyboardHintPanel.java:169-188 — all 11 key state colors from ThemeManager |
| 34 | KeyboardHintPanel key highlight is instant (no animation) per D-31 | VERIFIED | KeyboardHintPanel.java:82-87,94-97 — `repaint()` called directly, no Timeline |
| 35 | KeyboardHintPanel key label font is 10pt (was 11pt) | VERIFIED | KeyboardHintPanel.java:201 — `new Font("SansSerif", Font.PLAIN, 10)` |
| 36 | PlaceholderPanel uses ThemeManager BG_ELEVATED/SURFACE and TEXT_PRIMARY/SECONDARY | VERIFIED | PlaceholderPanel.java:11,24,30,39 — all 4 color references to ThemeManager |
| 37 | PlaceholderPanel subtitle updated to UI-SPEC copy + font 12pt (was 13pt) | VERIFIED | PlaceholderPanel.java:38,40 — "Drag and drop a .mid file...", Font 12pt |
| 38 | No hardcoded new Color(0x...) in DrumPadGrid, KeyboardHintPanel, PlaceholderPanel | VERIFIED | Zero matches in all three files |
| 39 | ParticleSystem installed on JFrame GlassPane | VERIFIED | ReBeatBoxWindow.java:75-79 — `particleSystem = new ParticleSystem()` + `setGlassPane(particleSystem)` |
| 40 | ParticleSystem subscribes to NoteEventBus for sequencer notes | VERIFIED | ReBeatBoxWindow.java:99-104 — `eventBus.subscribe(activeNotes -> { particleSystem.emit(note, 100) })` |
| 41 | ParticleSystem subscribes to LiveNoteEventListener for live notes | VERIFIED | ReBeatBoxWindow.java:109-121 — existing subscribeLive modified to call `particleSystem.emit(note, velocity)` |
| 42 | GlitchTransition triggers on file load via loadAndPlay() | VERIFIED | ReBeatBoxWindow.java:257,265-291 — snapshot capture + `runFileLoadGlitch()` with 300ms Timeline |
| 43 | SvgIconLoader.preload() called at startup before window creation | VERIFIED | App.java:28 — after NightShade skin (line 25), before MIDI engine init (line 36) |
| 44 | File-load glitch renders via ParticleSystem.setOverlayImage() on GlassPane | VERIFIED | ReBeatBoxWindow.java:282,290 — `particleSystem.setOverlayImage(glitched)` |
| 45 | 12 SVG icon files exist in src/main/resources/icons/ | VERIFIED | All 12 files confirmed on disk (play, pause, stop, restart, open-file, collapse-left, expand-right, app-icon, bpm, volume, keyboard-mode, drum-mode) |
| 46 | ThemeManager has all 31+ color constants (D-01 through D-04, D-28, D-30) | VERIFIED | ThemeManager.java — BG_ROOT/SURFACE/ELEVATED, TEXT_*, BORDER_IDLE, HUE_*, KEY_*, DESTRUCTIVE, accentForHue, velocityToAlpha |
| 47 | SvgIconLoader has Batik ImageTranscoder with size-keyed cache and preload() | VERIFIED | SvgIconLoader.java — BufferedImageTranscoder inner class, two-level Map cache, preload() for 12 icons |
| 48 | PadButton MIDI output (sendNoteOn/sendNoteOff) preserved alongside animation | VERIFIED | PadButton.java:110,119 — `sendNoteOn()` in mousePressed, `sendNoteOff()` in mouseReleased |

**Score:** 48/48 truths verified

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| UI-02 | 04-01, 04-04, 04-05 | Neon color palette applied across all UI elements | SATISFIED | ThemeManager constants in ControlBar, SidebarPanel, PadButton, DrumPadGrid, KeyboardHintPanel, PlaceholderPanel, PianoRollPanel background |
| UI-03 | 04-04, 04-05 | All buttons have hover/press animation feedback via radiance-animation | SATISFIED | Radiance Timeline on 22 buttons: 6 ControlBar + 16 PadButton + 1 SidebarPanel toggle; KeyboardHintPanel instant per D-31 |
| GLITCH-01 | 04-02, 04-06 | Particle burst effect on every MIDI note-on (playback + live) | SATISFIED | ParticleSystem on GlassPane, NoteEventBus subscriptions for sequencer + live notes |
| GLITCH-02 | 04-02, 04-04, 04-06 | Glitch art transition animations on window/view switches | SATISFIED | SidebarPanel bidirectional collapse/expand (150ms) + file load transition (300ms) via GlitchTransition.applyRgbSplit |
| GLITCH-03 | 04-03 | Note tracks rendered with neon glow lines | SATISFIED | PianoRollPanel kernel 7, sigma 3.5, velocity-to-alpha mapping in drawSingleNote |
| GLITCH-04 | 04-01, 04-04 | Custom cyber-styled SVG icon set | SATISFIED | 12 SVG icons in resources, SvgIconLoader with Batik, icons used in ControlBar + SidebarPanel |

**Coverage: 6/6 requirements SATISFIED**

### Context Decisions Compliance

All 31 locked decisions (D-01 through D-31) from 04-CONTEXT.md verified against codebase:

| Decision | Status | Notes |
|----------|--------|-------|
| D-01 (ThemeManager centralized colors) | HONORED | ThemeManager.java — 31+ semantic constants, all UI components reference them |
| D-02 (Full rainbow gradient palette) | HONORED | 4 semantic hue zones: HUE_TRANSPORT(0.55), HUE_DRUM_PADS(0.80), HUE_SIDEBAR(0.65), HUE_KEYBOARD(0.50) |
| D-03 (Three-tier backgrounds) | HONORED | BG_ROOT(#0A0A14), BG_SURFACE(#12122A), BG_ELEVATED(#1A1A3E) |
| D-04 (Radiance NightShade retained) | HONORED | Only structural styling — custom neon for fill/border/glow |
| D-05 (Hover/press animation specs) | HONORED | 200ms/250ms hover, 75ms press, 150ms release with Spline easing |
| D-06 (Unified animation on all buttons) | HONORED | 22 buttons total: ControlBar(6) + PadButton(16) + SidebarPanel(1) |
| D-07 (Radiance ephemeral Timeline) | HONORED | Timeline.builder() pattern in all animation methods |
| D-08 (Cyber-block particles) | HONORED | fillRect + drawRect with glow border in ParticleSystem.paintComponent |
| D-09 (Particles on GlassPane) | HONORED | setGlassPane(particleSystem) in ReBeatBoxWindow |
| D-10 (Particle color = forPitch) | HONORED | `NoteColorMapper.forPitch(note)` in emit() |
| D-11 (All note events trigger particles) | HONORED | Sequencer + live subscriptions in ReBeatBoxWindow |
| D-12 (200 cap, merge overflow) | HONORED | MAX_PARTICLES=200, mergeOldestParticles() |
| D-13 (Lifetime 300-800ms) | HONORED | `300 + (velocity * 500 / 127)` |
| D-14 (Size 1-8px) | HONORED | `1 + (velocity * 7 / 127)` |
| D-15 (Independent Timer 16ms) | HONORED | ParticleSystem's own `javax.swing.Timer(16ms)` |
| D-16 (Two triggers: sidebar + file load) | HONORED | SidebarPanel.toggle() + ReBeatBoxWindow.loadAndPlay() |
| D-17 (RGB channel separation) | HONORED | GlitchTransition.applyRgbSplit with red/blue offsets |
| D-18 (150ms sidebar, 300ms file load) | HONORED | Timeline durations match in both trigger sites |
| D-19 (Component-level animation) | HONORED | BufferedImage snapshots, not monolithic screenshots |
| D-20 (Per-note bar glow only) | HONORED | Only drawGlowingBar constants changed |
| D-21 (Kernel 7, sigma 3.5) | HONORED | BLUR_KERNEL_SIZE=7, BLUR_SIGMA=3.5f |
| D-22 (Keep separable ConvolveOp) | HONORED | buildGaussianKernel/applyConvolveBlur unchanged |
| D-23 (Trigger line unchanged) | HONORED | drawTriggerLine not modified |
| D-24 (Apache Batik for SVG) | HONORED | SvgIconLoader with Batik ImageTranscoder |
| D-25 (20 icon set) | HONORED | 12 application icons (8 reserved for Phase 5) |
| D-26 (Replace unicode with SVG) | HONORED | Zero unicode (▶⏸⏹⏮📂◀▶) in ControlBar/SidebarPanel |
| D-27 (Velocity to alpha) | HONORED | velocityToAlpha method in PianoRollPanel |
| D-28 (Alpha 0.3-1.0 above, 0.15-0.4 below) | HONORED | Correct formulas: 0.30+v*0.70, 0.15+v*0.25 |
| D-29 (Applied to all note bars) | HONORED | All three drawSingleNote paths use velocityToAlpha |
| D-30 (KeyboardHintPanel ThemeManager neon) | HONORED | All KEY_IDLE_*/KEY_PRESSED_* from ThemeManager |
| D-31 (Key highlight instant switch) | HONORED | setKeyHighlighted/clearAllHighlights call repaint() directly |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ThemeManager.java` | Centralized semantic color constants | VERIFIED | 31+ constants, accentForHue(), velocityToAlpha(), private constructor |
| `SvgIconLoader.java` | Batik SVG->BufferedImage with cache | VERIFIED | BufferedImageTranscoder, two-level cache, preload(), loadSvg(), getIcon() |
| `build.gradle` | 11 batik-*:1.19 deps | VERIFIED | All 11 declared, batik-anim/script/svggen excluded |
| `src/main/resources/icons/*.svg` | 12 SVG icon files | VERIFIED | All 12 files present on disk |
| `ParticleSystem.java` | 200-particle burst system | VERIFIED | JComponent, off-screen accumulation, merge, overlay support |
| `GlitchTransition.java` | RGB channel separation effect | VERIFIED | 6-param and 3-param overloads, edge clamping, noise overlay |
| `ControlBar.java` | SVG icons + Timeline animation + ThemeManager | VERIFIED | 373 lines, zero hardcoded colors, zero unicode icons |
| `SidebarPanel.java` | GlitchTransition bidirectional + SVG + Timeline | VERIFIED | 348 lines, performGlitchCollapse/Expand, preCollapseSnapshot |
| `PianoRollPanel.java` | Amplified glow + velocity alpha | VERIFIED | BLUR_KERNEL_SIZE=7, BLUR_SIGMA=3.5f, BLUR_PAD=8, velocityToAlpha |
| `NoteColorMapper.java` | Public HSB constants | VERIFIED | HUE_START, SATURATION, BRIGHTNESS now public static final |
| `PadButton.java` | ThemeManager + Timeline animation | VERIFIED | animatedBorderColor/FillColor/Scale, AffineTransform, Spline easing |
| `DrumPadGrid.java` | ThemeManager.BG_SURFACE | VERIFIED | Single-line change, zero hardcoded colors |
| `KeyboardHintPanel.java` | ThemeManager KEY_* colors + instant highlight | VERIFIED | All 11 key state colors from ThemeManager, repaint() direct |
| `PlaceholderPanel.java` | ThemeManager colors + UI-SPEC copy/font | VERIFIED | All 4 colors migrated, subtitle updated, 12pt font |
| `ReBeatBoxWindow.java` | GlassPane + subscriptions + file load glitch | VERIFIED | ParticleSystem field, setGlassPane, subscribe/subscribeLive, runFileLoadGlitch |
| `App.java` | SvgIconLoader.preload() startup ordering | VERIFIED | After NightShade, before MIDI engine init |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| SvgIconLoader.loadSvg() | src/main/resources/icons/*.svg | getResourceAsStream | WIRED | Classpath resource loading with size-keyed cache |
| ThemeManager.BG_ROOT | PianoRollPanel.setBackground() | Static field access | WIRED | Both constructor line 111 and paintComponent line 297 |
| ParticleSystem.emit() | NoteColorMapper.forPitch() | Direct method call | WIRED | Line 188: `NoteColorMapper.forPitch(note)` |
| GlitchTransition.applyRgbSplit() | BufferedImage.getRGB/setRGB | Pixel array I/O | WIRED | Lines 66,111: bulk pixel read/write |
| PianoRollPanel.drawGlowingBar() | buildGaussianKernel(BLUR_KERNEL_SIZE, BLUR_SIGMA) | Updated constants | WIRED | Line 450: const values 7/3.5f flow through |
| PianoRollPanel.drawSingleNote() | velocityToAlpha(velocity, aboveTrigger) | Method call | WIRED | Lines 404,410,414: all three rendering paths |
| ControlBar.createTransportButton() | SvgIconLoader.getIcon(name, 38) | setIcon(new ImageIcon()) | WIRED | Line 230: icon retrieved from cache |
| SidebarPanel.toggle() | GlitchTransition.applyRgbSplit() | Snapshot then glitch | WIRED | Lines 133,178: both collapse and expand paths |
| ControlBar button | Timeline.builder | MouseListener triggers | WIRED | wireButtonAnimation() with mouseEntered/Exited/Pressed/Released |
| PadButton.paintComponent() | ThemeManager.BG_ELEVATED / accentForHue(HUE_DRUM_PADS) | Static field access | WIRED | Lines 49-50,52: color constants |
| PadButton.setupMouseListener() | Timeline.builder | TimelineCallback drives animated colors | WIRED | animateBorderTo/animateFillTo/animatePress/animateRelease |
| KeyboardHintPanel.drawSingleKey() | ThemeManager.KEY_IDLE_*/KEY_PRESSED_* | Static field access | WIRED | Lines 169-188: all 11 key state colors |
| ReBeatBoxWindow constructor | ParticleSystem on getGlassPane() | setGlassPane() | WIRED | Lines 75-79: install + setVisible(true) |
| ReBeatBoxWindow.loadAndPlay() | GlitchTransition.applyRgbSplit() | Snapshot + Timeline glitch | WIRED | Lines 265-291: runFileLoadGlitch() |
| App.main() | SvgIconLoader.preload() | After skin, before window | WIRED | Line 28: called after NightShade, before MIDI engine |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| ParticleSystem.emit() | note, velocity | NoteEventBus (sequencer + live) | Yes — receives note numbers and velocities from MIDI playback and live input | FLOWING |
| SidebarPanel toggle glitch | preCollapseSnapshot | paint() snapshot capture | Yes — captures actual component rendering | FLOWING |
| ReBeatBoxWindow file load glitch | preSnapshot | pianoRollPanel.paint() snapshot | Yes — captures pre-load visualization | FLOWING |
| PianoRollPanel.drawSingleNote() | note.velocity() | RenderNote from MidiPreScanner | Yes — real MIDI velocity from pre-scanned file | FLOWING |
| ControlBar SVG icons | SvgIconLoader.getIcon() | Classpath resources + Batik rasterization | Yes — real SVG files load at startup | FLOWING |
| PadButton.paintComponent() | animatedBorderColor/FillColor/Scale | Timeline interpolation | Yes — Timeline pulses on EDT drive real color transitions | FLOWING |
| KeyboardHintPanel.drawSingleKey() | ThemeManager.KEY_* constants | Static field | Yes — real java.awt.Color values | FLOWING |

### Behavioral Spot-Checks

| Behavior | Result | Status |
|----------|--------|---------|
| All 48 must_have truths verified via grep | All 48 passed | PASS |
| Zero hardcoded `new Color(0x...)` in ControlBar | Confirmed — no matches | PASS |
| Zero hardcoded `new Color(0x...)` in SidebarPanel | Confirmed — no matches | PASS |
| Zero hardcoded `new Color(0x...)` in KeyboardHintPanel | Confirmed — no matches | PASS |
| Zero hardcoded `new Color(0x...)` in PlaceholderPanel | Confirmed — no matches | PASS |
| Zero hardcoded `new Color(0x...)` in DrumPadGrid | Confirmed — no matches | PASS |
| Zero unicode icons (▶⏸⏹⏮📂) in ControlBar | Confirmed — no matches | PASS |
| Zero unicode icons (◀▶) in SidebarPanel | Confirmed — no matches | PASS |
| 11 Batik deps in build.gradle, 3 excludes | Confirmed — 11 matches, 0 excluded-matches | PASS |
| KeyboardHintPanel font is 10pt (was 11pt) | Confirmed | PASS |
| PlaceholderPanel font is 12pt (was 13pt) | Confirmed | PASS |
| SvgIconLoader.preload() in App.java | Confirmed — line 28 | PASS |
| PadButton has sendNoteOn/sendNoteOff calls | Confirmed — lines 110,119 | PASS |
| SidebarPanel stores preCollapseSnapshot | Confirmed — 5 references | PASS |
| No TODO/FIXME/placeholder in Phase 4 source files | Confirmed — zero matches (one false positive in comment) | PASS |
| PianoRollPanel.Color.BLACK replaced with ThemeManager.BG_ROOT | Confirmed — zero Color.BLACK matches | PASS |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| PianoRollPanel.java | 555-595 | Hardcoded `new Color(0x...)` for piano keyboard visualization strip | INFO | Acceptable — these are visualization rendering colors for the on-screen piano keyboard (white/black key fills, octave separators), not UI theme background/foreground. Plan 04-04 only required changing the panel background to BG_ROOT. |
| PadButton.java | 51,53,55 | 3 retained inline colors (HOVER_FILL #25304A, PRESSED_FILL #003344, DEFAULT_TEXT #CCCCCC) | INFO | Intentional per plan rationale — drum-pad-specific state colors not in ThemeManager's global palette. Documented in SUMMARY.md. |

### Human Verification Required

8 items require subjective human evaluation of visual quality, animation smoothness, and aesthetic coherence:

1. **SVG Icon Rendering** — Visually inspect all 12 icons on ControlBar and SidebarPanel for correct rendering, glow filter output, and angular cyber styling.
2. **Particle Burst Animation** — Play a MIDI file and observe particle bursts: cyber-block shapes, NoteColorMapper colors, linear alpha fade, no frame drops.
3. **Sidebar Glitch Transition** — Toggle sidebar: 150ms RGB channel separation with bell-curve offset, clean transition back.
4. **File Load Glitch Transition** — Load a new MIDI file: 300ms RGB split overlay on PianoRollPanel, 25px max offset, smooth blend.
5. **Button Animation Smoothness** — Hover/press all 22 buttons: border color transitions, 0.95x scale bounce, no stuck animations.
6. **KeyboardHintPanel Latency** — Press keyboard keys: instant neon cyan highlight with no perceived delay per D-31.
7. **Neon Glow Visibility** — Verify amplified GaussianBlur (kernel 7, sigma 3.5) is visibly stronger on short/fast notes.
8. **Overall Aesthetic Coherence** — Evaluate the unified cyber/glitch look across all components, color consistency, animation cohesion.

### Gaps Summary

No code-level gaps found. All 48 must-have truths verified against the codebase, all 31 locked decisions honored, all 6 requirements satisfied, all artifacts substantive and wired. The 8 human verification items are for visual/subjective evaluation only — the underlying implementation, wiring, and data flow are all confirmed complete and correct.

**Notable Implementation Choices:**
- File-load glitch renders via ParticleSystem GlassPane overlay (`setOverlayImage()`) rather than modifying PianoRollPanel, avoiding cross-plan file conflicts with plans 04-03 and 04-04.
- SidebarPanel stores a `preCollapseSnapshot` for the expand glitch transition, capturing the content once during collapse and reusing it for the expand animation.
- Radiance 8.5.0 API uses `Spline` (not `SplineEase`), `Timeline.TimelineState` (inner class, not top-level), and `callback.TimelineCallback` (sub-package, not `api.TimelineCallback`). All plans corrected for these at implementation time.
- PianoRollPanel keyboard visualization strip (lines 555-595) retains hardcoded rendering colors — these are visualization artifacts, not theme-related UI backgrounds.
- PadButton retains 3 drum-pad-specific inline colors (HOVER_FILL, PRESSED_FILL, DEFAULT_TEXT) per intentional plan design documented in both PLAN.md and SUMMARY.md.

---

_Verified: 2026-04-28T20:30:00Z_
_Verifier: Claude (gsd-verifier)_
