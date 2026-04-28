# Phase 4: Visual Polish - Context

**Gathered:** 2026-04-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Full cyber/glitch aesthetic — particles, glow, transitions, neon palette, SVG icons. This phase lives on top of Phase 2 (visualization) and Phase 3 (live performance). It does NOT add new functional capabilities — it makes everything already built look and feel cyberpunk.

**In scope:** Neon color palette (UI-02), Button animation (UI-03), Particle bursts (GLITCH-01), Glitch transitions (GLITCH-02), Neon glow amplification (GLITCH-03), SVG icon set (GLITCH-04)
**Not in scope:** New features, new modes, audio changes, content/packaging (Phase 5)
</domain>

<decisions>
## Implementation Decisions

### Neon Color Palette (UI-02)

- **D-01:** ThemeManager class — centralized semantic color constants. All UI components read colors from ThemeManager, never hardcode Color values. Replaces the scattered hardcoded colors in ControlBar (0x16213e), SidebarPanel (0x16213e), PadButton (0x1A1A2E), KeyboardHintPanel (0x0a0a14), etc.
- **D-02:** Full rainbow gradient palette — not a single accent color. Different UI regions get different hue zones. Extends NoteColorMapper's HSB approach (HUE_START=0.70, SAT=0.85, BRIGHT=0.95) to semantic UI color generation.
- **D-03:** Three-tier dark background hierarchy:
  - `BG_ROOT`: #0a0a14 (deepest — PianoRollPanel canvas, glass pane base)
  - `BG_SURFACE`: #12122a (panels — SidebarPanel, KeyboardHintPanel)
  - `BG_ELEVATED`: #1a1a3e (interactive elements — buttons, sliders, pads)
- **D-04:** Radiance NightShade retained for structural styling (corner radii, margins, font rendering). Custom neon colors override fill, border, and glow — not structural attributes.

### Button Animation (UI-03)

- **D-05:** Hover: border color transitions from dark to neon (Rainbow palette hue for that control). Press: 0.95x scale bounce + brief color flash. Release: spring-back to idle. All driven via Radiance ephemeral Timeline.
- **D-06:** Unified animation across ALL interactive buttons: ControlBar transport (5), open button, PadButton grid (16), SidebarPanel toggle. One animation logic applied uniformly.
- **D-07:** Radiance ephemeral Timeline used for animation interpolation. Fade-in/fade-out + property interpolation on border color and scale transform.

### Particle System (GLITCH-01)

- **D-08:** Cyber-block particle style — small squares/rectangles (2-8px), random rotation, glowing border. No circles or organic shapes.
- **D-09:** Particles render on a global GlassPane overlaid on the JFrame. Particles can travel anywhere in the window, crossing component boundaries.
- **D-10:** Particle color = NoteColorMapper.forPitch(noteNumber) — same pitch→hue mapping as the note bars they burst from.
- **D-11:** ALL note-on events trigger particles — both Sequencer playback notes AND live keyboard/drum-pad notes. ParticleSystem subscribes to NoteEventBus (onActiveNotesChanged) AND LiveNoteEventListener (onLiveNoteOn).
- **D-12:** Particle cap: 200 max. When full, merge the oldest 2-3 particles into a single larger particle to make room. No rejection, no sudden disappearance.
- **D-13:** Particle lifetime = f(velocity): 300ms (velocity=0) → 800ms (velocity=127). Linear mapping. Strong notes linger, soft notes fade fast.
- **D-14:** Particle size = f(velocity): 1-3px (velocity=0) → 4-8px (velocity=127). Linear mapping.
- **D-15:** Particle system runs its own javax.swing.Timer(16ms) for repaint. Independent from PianoRollPanel's 16ms animation timer. Each repaints its own region (GlassPane vs CENTER panel).

### Glitch Transitions (GLITCH-02)

- **D-16:** Two triggers: (1) SidebarPanel toggle collapse/expand, (2) new MIDI file loaded (old visualization → new).
- **D-17:** RGB channel separation effect — red channel shifted left, blue channel shifted right, brief noise overlay during transition. Classic digital glitch look.
- **D-18:** Scene-differentiated duration: sidebar toggle ~150ms (fast, keeps UI snappy), file load transition ~300ms (dramatic, masks I/O).
- **D-19:** Component-level animation via Radiance ephemeral Timeline. SidebarPanel gets its own transition timeline; PianoRollPanel gets a file-load transition timeline. NOT a monolithic screenshot-based approach.

### Neon Glow Lines (GLITCH-03)

- **D-20:** Enhance existing per-note bar GaussianBlur glow only. No new glow elements (no motion trails, no chord connectors, no octave separator glow).
- **D-21:** GaussianBlur parameters: kernel 5→7, sigma 2.0→3.5. Alpha moderately increased to ensure visibility on short/fast notes (directly resolves amplify-blur-visibility.md todo).
- **D-22:** Keep the existing separable two-pass ConvolveOp algorithm. Only constants change (BLUR_KERNEL_SIZE, BLUR_SIGMA). No architectural change to drawGlowingBar().
- **D-23:** Trigger line glow unchanged — existing pulse animation + 2px core + 8px cyan halo stays as-is.

### SVG Icons (GLITCH-04)

- **D-24:** Apache Batik library for SVG → BufferedImage rendering. Icons loaded at startup, cached as BufferedImage instances keyed by size.
- **D-25:** Complete icon set (~20 icons): Play, Pause, Stop, Restart, Open File, Sidebar Toggle (expand/collapse pair), App Icon, BPM indicator, Volume indicator, Keyboard mode, Drum mode, plus reserves for Phase 5 content panel.
- **D-26:** Replace all unicode text icons in ControlBar (▶⏸⏹⏮📂) and SidebarPanel toggle (◀▶) with SVG-rendered icons. PadButton labels stay text-based (sound names).

### Velocity → Brightness Mapping (Phase 2 deferred)

- **D-27:** Implement velocity → alpha mapping. No longer deferred.
- **D-28:** Alpha = linear interpolation: velocity 0-127 → alpha 0.3-1.0 (above trigger line), 0.15-0.4 (below trigger line). Preserves the above/below brightness distinction from Phase 2 D-06.
- **D-29:** Applied to all note bars in PianoRollPanel. Both above and below trigger line.

### KeyboardHintPanel Neon Integration

- **D-30:** KeyboardHintPanel adopts ThemeManager neon palette — idle keys use low-alpha ThemeManager colors, pressed keys use full-brightness neon (Cyan #00E5FF).
- **D-31:** Key highlight is instant color switch (no transition animation). Keyboard latency is performance-critical for live playing — animation frames would add perceived delay.

### Claude's Discretion

- ThemeManager exact API design (method signatures, color naming convention, relationship to NoteColorMapper)
- Particle motion trajectory details (initial velocity ranges, random spread angle distribution)
- Glitch transition RGB offset magnitudes, noise density, exact per-scene duration values
- SVG icon specific visual design (Path shapes, stroke weights, glow treatment)
- Button animation Timeline exact parameters (duration in ms, easing curve selection)
- Merge-old-particles algorithm details (which particles, merged properties)
- KeyboardHintPanel exact idle/pressed color values within the ThemeManager palette
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Definition
- `.planning/ROADMAP.md` § Phase 4 — Requirements (UI-02, UI-03, GLITCH-01 through GLITCH-04), success criteria
- `.planning/REQUIREMENTS.md` § UI-02, UI-03, GLITCH-01, GLITCH-02, GLITCH-03, GLITCH-04 — Full acceptance criteria

### MIDI Engine (particle system integration)
- `src/main/java/com/rebeatbox/engine/NoteEventBus.java` — fire(Set activeNotes) for sequencer notes, fireLiveNoteOn/Off for live notes. ParticleSystem subscribes to both.
- `src/main/java/com/rebeatbox/engine/NoteEventListener.java` — onActiveNotesChanged interface (sequencer notes)
- `src/main/java/com/rebeatbox/engine/LiveNoteEventListener.java` — onLiveNoteOn(note, velocity), onLiveNoteOff(note) interface

### Visualization (glow + color foundation)
- `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` — Existing 5×5 GaussianBlur per-note glow (drawGlowingBar, buildGaussianKernel, applyConvolveBlur). Target for GLITCH-03 parameter changes. 60fps Timer, three-layer compositing.
- `src/main/java/com/rebeatbox/visual/NoteColorMapper.java` — HSB pitch→neon-color mapping (HUE_START=0.70, SAT=0.85, BRIGHT=0.95). Extended by ThemeManager.

### UI Components (ThemeManager targets)
- `src/main/java/com/rebeatbox/ui/ControlBar.java` — Transport buttons, BPM/volume sliders, progress bar. Hardcoded colors (0x16213e, 0xe0e0e0). Unicode icons. Target for SVG replacement + animation.
- `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` — Main window layout (NORTH/CENTER/EAST/SOUTH). KeyboardFocusManager dispatcher. Wiring hub. Target for GlassPane overlay.
- `src/main/java/com/rebeatbox/ui/SidebarPanel.java` — Collapsible 240px sidebar, instant toggle. Target for GlitchTransition.
- `src/main/java/com/rebeatbox/ui/KeyboardHintPanel.java` — Three-row virtual keyboard, hardcoded colors, instant highlight toggle. Target for ThemeManager neon + UI-02.
- `src/main/java/com/rebeatbox/live/PadButton.java` — Custom-painted drum pad, hover/pressed color states, 8 hardcoded colors. Target for ThemeManager + animation.
- `src/main/java/com/rebeatbox/live/DrumPadGrid.java` — 4×4 grid container for PadButtons.
- `src/main/java/com/rebeatbox/App.java` — NightShade skin application, engine initialization, window creation.

### Prior Phase Context
- `.planning/phases/01-foundation/01-CONTEXT.md` — Window layout (D-01 through D-06), NoteEventBus (D-07), PlaybackController API (D-11), SoundFont (D-13), error handling. Icon placeholder decision (D-05: "temporary Radiance default icon, replaced with custom cyber SVG icons in Phase 4").
- `.planning/phases/02-visualization/02-CONTEXT.md` — PianoRollPanel rendering architecture (D-08), color scheme (D-05 through D-07 — pitch-based neon, "v1 does NOT map velocity to brightness"), glow decisions (D-07: "real GaussianBlur per-note"), deferred velocity mapping.
- `.planning/phases/03-live-performance/03-CONTEXT.md` — LiveNoteEventListener API (D-10), KeyboardMapper + KeyboardHintPanel wiring, DrumPadGrid in SidebarPanel, KeyboardFocusManager dispatcher.

### Deferred Todo (folded into scope)
- `.planning/todos/pending/amplify-blur-visibility.md` — "Amplify GaussianBlur glow visibility on fast/small notes" — resolved by D-20/D-21/D-22 (kernel 5→7, sigma 2.0→3.5)
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **NoteEventBus** (engine/): Already has dual-channel events — `onActiveNotesChanged(Set)` for sequencer AND `onLiveNoteOn/Off(int, int)` for live. ParticleSystem subscribes to both with zero engine changes.
- **PianoRollPanel 60fps timer**: Existing `javax.swing.Timer(16ms)` animation loop. Particle system creates its own independent timer (D-15) — no contention.
- **NoteColorMapper** (visual/): HSB-based neon mapping already working. ThemeManager extends or wraps this for global palette.
- **buildGaussianKernel / applyConvolveBlur** (PianoRollPanel): Existing separable ConvolveOp pipeline. Only constants change — BLUR_KERNEL_SIZE and BLUR_SIGMA.
- **Radiance NightShade skin**: Already applied in App.main() before window creation. Stays as structural skeleton.

### Established Patterns
- **Swing Timer animation**: PianoRollPanel's 16ms Timer → paintComponent pattern. Particle system follows same pattern on GlassPane.
- **Custom paintComponent**: PadButton, KeyboardHintPanel, PianoRollPanel all override paintComponent. ThemeManager colors slot directly into this pattern.
- **EDT threading**: NoteEventBus wraps callbacks in SwingUtilities.invokeLater(). All visual changes are EDT-safe by construction.
- **Direct controller access**: ControlBar and PianoRollPanel hold a direct controller reference. Pattern does NOT need extending for Phase 4.

### Integration Points
- **GlassPane**: JFrame.getGlassPane() — currently unused. ParticleSystem installs here. Must be transparent to mouse events (setVisible + custom paint only).
- **SidebarPanel.toggle()**: Currently instant setPreferredSize + revalidate. Replace with GlitchTransition Timeline animation (D-16, D-19).
- **ReBeatBoxWindow.loadAndPlay()**: File load triggers pianoRollPanel.onFileLoaded(). Add GlitchTransition trigger here (D-16).
- **ControlBar button construction**: createTransportButton() creates plain JButtons. Replace text with SVG icons + add Radiance Timeline animation listeners.
- **App.main()**: ThemeManager initialization must happen AFTER NightShade skin application but BEFORE window creation. SVG icons loaded at startup.
</code_context>

<specifics>
## Specific Ideas

- User chose "全彩虹渐变" over single-color accent — different UI areas get different hue zones for visual hierarchy, not monotonous cyan everywhere
- RGB channel separation glitch — precisely the classic cyberpunk digital-failure aesthetic, not scanline tear or block displacement
- Cyber blocks (not energy dots or organic shapes) for particles — geometric, rigid, digital — matches the note bar rectangle language already in PianoRollPanel
- KeyboardHintPanel stays instant-switch for highlights — performance-sensitive context (live playing), no animation frames that could add perceived latency
- Per-note glow approach is simple amplification (kernel 5→7, sigma 2→3.5), not a new rendering approach — respects the Phase 2 architecture while fixing UAT finding

</specifics>

<deferred>
## Deferred Ideas

- Note motion trails (horizontal glow trailing behind falling notes) — considered under GLITCH-03, determined unnecessary
- Glow connectors between chord notes — same, not needed
- Octave separator line glow — not needed
- Dual-channel velocity mapping (alpha + HSB brightness simultaneously) — chose alpha-only
- KeyboardHintPanel key scale bounce animation — keyboard latency-sensitive, animation would hurt live play feel
- Particle gravity / physics simulation — particles use simple velocity + fade, no complex physics

</deferred>

---

*Phase: 04-visual-polish-feel-it*
*Context gathered: 2026-04-28*
