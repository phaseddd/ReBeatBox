# Phase 2: Visualization - Context

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Piano roll view + falling notes animation, perfectly synced to MIDI playback. Replace Phase 1's PlaceholderPanel with a Synthesia-style waterfall visualization. PianoRollPanel subscribes to NoteEventBus and reads PlaybackController position directly for 60fps rendering.

Requirements covered: VIS-01, VIS-02, VIS-03
</domain>

<decisions>
## Implementation Decisions

### Layout — Synthesia Waterfall
- **D-01:** Synthesia-style waterfall paradigm — notes fall from top of screen toward bottom. Time = vertical axis (notes move downward), pitch = horizontal axis. This is the "game-like" experience referenced in the project vision.
- **D-02:** Fixed mini piano keyboard on the left side (40-60px wide, white/black key silhouettes only). Pitch grid auto-scales to the actual note range used in the current MIDI file — no wasted space for unused octaves.
- **D-03:** Trigger line fixed at ~15-20% from bottom of screen. Notes cross this line → triggered. Notes that have passed continue falling off-screen with fade-out.
- **D-04:** Fixed-width note bars (one column per semitone in the active range). Bar length = note duration (short notes = small blocks, long notes = elongated rectangles). Chords = multiple adjacent bars at the same vertical position.

### Color Scheme — Pitch-Based Neon
- **D-05:** Color-by-pitch mapping — different pitches map to different neon colors, forming a rainbow gradient across the keyboard. Low notes lean purple/blue, high notes lean red/orange.
- **D-06:** Past notes (below trigger line) rendered at reduced brightness and ~40% opacity, same hue. Upcoming notes (above trigger line) rendered at full brightness. v1 does NOT map velocity to brightness — all notes use uniform brightness regardless of velocity.
- **D-07:** Pure black canvas background. Notes rendered with real Java2D GaussianBlur glow — each note gets its own BufferedImage, blurred independently, then composited onto the main canvas.

### Rendering Architecture
- **D-08:** Three-layer compositing pipeline:
  - Layer 1 (Background): Pure black, static, only repainted on window resize
  - Layer 2 (Notes): Each visible note → BufferedImage → GaussianBlur → composite to main canvas. Rebuilt every frame.
  - Layer 3 (Foreground): Trigger line, keyboard, grid lines drawn directly on main canvas. Rebuilt every frame.
- **D-09:** Multi-track MIDI files merged into single waterfall view. Track 0 (tempo/conductor metadata track) skipped entirely. Different instrument tracks are NOT visually distinguished in v1.
- **D-10:** Pre-scan on file load: iterate all Tracks, pair NoteOn/NoteOff into `RenderNote` objects (pitch, startMicros, endMicros, velocity, channel), store in ArrayList sorted by startMicros. Binary search at render time for visible notes.
- **D-11:** Pure viewport culling — only render notes within the visible time window (current position ± ~2 seconds). No artificial note count cap. Trust the render pipeline and culling to maintain 60fps.

### Playhead & Sync
- **D-12:** Trigger line rendered as a 2px horizontal neon line (bright white/cyan) with GaussianBlur glow. Continuous pulse animation during playback — opacity oscillates 80% ↔ 100% for a "heartbeat" feel.
- **D-13:** PianoRollPanel uses its own `javax.swing.Timer(16ms)` for independent 60fps repaint loop. Each frame: directly calls `controller.getMicrosecondPosition()` for time, binary-searches pre-scanned `List<RenderNote>` for visible notes. Does NOT depend on NoteEventBus timing for smoothness.
- **D-14:** Seek → instant visual jump to new position (no transition animation). Pause → animation timer continues but position freezes (controller.getMicrosecondPosition() returns static value when paused).
- **D-15:** PianoRollPanel determines "currently sounding" notes by binary-searching its pre-scanned RenderNote list (notes where currentPosition ∈ [startTime, endTime)). NoteEventBus is reserved for Phase 4 ParticleSystem — PianoRollPanel does not depend on it for note state.

### Claude's Discretion
- Exact neon color → RGB mapping table (which MIDI note number → which color)
- GaussianBlur kernel size and sigma values
- Pulse animation easing curve (linear vs sine)
- RenderNote class field design
- Viewport culling window size fine-tuning (1.5s vs 2s vs 3s)
- Adaptive note range calculation (padding strategy for edge octaves)
- Mini keyboard drawing details (key width, exact color scheme)
</decisions>

<specifics>
## Specific Ideas

- User explicitly chose "real GaussianBlur per-note" over cheaper glow approximations — visual quality is the priority, performance is secondary
- Pure black background with no grid lines — the glow of the notes themselves defines the visual space (Matrix-like cyber aesthetic)
- The mini keyboard on the left should feel like a permanent reference, not a distracting element
</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase Definition
- `.planning/ROADMAP.md` § Phase 2 — Requirements (VIS-01, VIS-02, VIS-03), success criteria, key files list
- `.planning/REQUIREMENTS.md` § VIS-01, VIS-02, VIS-03 — Full acceptance criteria

### Architecture & Research
- `.planning/research/ARCHITECTURE.md` — System architecture, component boundaries, data flow, thread model
- `.planning/research/PITFALLS.md` — Pitfalls 1 (MIDI clock sync), 3 (Graphics2D performance), 6 (multi-track confusion), 7 (skin conflicts), 9 (over-engineering)

### Phase 1 Context (foundation this builds on)
- `.planning/phases/01-foundation/01-CONTEXT.md` — NoteEventBus API, PlaybackController API, window layout decisions, Thread model (EDT wrapping)
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **NoteEventBus** (`engine/NoteEventBus.java`): Existing pub/sub, EDT-wrapped callbacks. Reserved for Phase 4 ParticleSystem — Phase 2 does NOT use it for frame timing.
- **PlaybackController** (`engine/PlaybackController.java`): Exposes getMicrosecondPosition(), getMicrosecondLength(), getActiveNotes(), getState(), getSequencer(). PianoRollPanel directly polls these.
- **PlaceholderPanel** (`ui/PlaceholderPanel.java`): Replaced by PianoRollPanel in ReBeatBoxWindow.CENTER. Reference for size/position.

### Established Patterns
- **EDT threading**: All Swing operations on EDT. javax.swing.Timer callbacks are on EDT. No manual thread management needed.
- **Direct Controller access**: ControlBar wires to controller directly — PianoRollPanel follows same pattern.
- **Dark theme**: Radiance NightShade applied globally. Pure black canvas is consistent.

### Integration Points
- **ReBeatBoxWindow.CENTER**: Replace `placeholderPanel` with `pianoRollPanel`. Window wiring in `wireEngine()` passes controller reference.
- **Phase 3 (Live Performance)**: RealtimeReceiver shares the Synthesizer. Live notes appear in same waterfall as playback notes.
- **Phase 4 (Visual Polish)**: ParticleSystem subscribes to NoteEventBus for particle bursts. GlitchTransition hooks into seek/pause state changes.
</code_context>

<deferred>
## Deferred Ideas

- Velocity → brightness mapping in note rendering → Phase 4
- Per-track layer toggle (show/hide individual instrument tracks) → v2 (MTRK-01, MTRK-02)
- Time-bucket indexing for RenderNote (optimization for very long MIDI files) → add only if profiling shows need
- Multi-track color differentiation in waterfall → v2
- GaussianBlur glow 在小/快音符上肉眼不可见（5x5 kernel 偏保守）→ Phase 4 调大 kernel 或增强 glow alpha (see: UAT test 3, todo amplify-blur-visibility.md)
</deferred>

---
*Phase: 02-visualization*
*Context gathered: 2026-04-27*
