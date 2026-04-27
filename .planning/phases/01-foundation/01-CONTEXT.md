# Phase 1: Foundation - Context

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

App opens with dark theme, loads MIDI files, plays with full transport controls. This is the foundation all subsequent phases build on — the MIDI engine and app shell must be designed with Phase 2 (visualization) and Phase 3 (live performance) as known consumers.

Requirements covered: PLAY-01, PLAY-02, PLAY-03, PLAY-04, PLAY-05, UI-01, CONT-02
</domain>

<decisions>
## Implementation Decisions

### Window Layout
- **D-01:** Control bar positioned at top of window — horizontal strip containing transport buttons (play/pause/stop/restart), BPM slider, volume slider, progress bar
- **D-02:** Main content area below control bar — Phase 1 shows a dark placeholder panel with centered "ReBeatBox" logo/title text. Phase 2 replaces this with piano roll visualization
- **D-03:** Collapsible right sidebar — empty in Phase 1, reserved for Drum Pad grid in Phase 3. Collapse/expand toggle button visible from Phase 1
- **D-04:** Window initial size 1100×700, user-resizable
- **D-05:** Window title "ReBeatBox", temporary Radiance default icon (replaced with custom cyber SVG icons in Phase 4)
- **D-06:** Radiance NightShade skin applied globally as dark theme foundation

### MIDI Engine API
- **D-07:** NoteEventBus — publisher/subscriber pattern. PlaybackController publishes active note lists each tick; UI components (PianoRollPanel, ParticleSystem in future phases) subscribe. Lightweight custom implementation — no external event library needed
- **D-08:** Separate PlaybackController (MIDI file playback via Sequencer) and RealtimeReceiver (live MIDI note trigger via Synthesizer). Both share the same Synthesizer instance for consistent audio output
- **D-09:** NoteEventBus.fire() internally wraps callbacks in SwingUtilities.invokeLater(). UI subscribers always receive events on EDT — no manual thread management required
- **D-10:** Engine auto-loads bundled SoundFont at startup via Synthesizer.open(). Uses system default MIDI output device (MidiSystem.getReceiver()). No device selection UI in v1
- **D-11:** PlaybackController exposes: play(), pause(), stop(), restart(), setBPM(int), setVolume(float), seek(long microseconds), getState() (enum: STOPPED/PLAYING/PAUSED), getPosition(), getActiveNotes()
- **D-12:** RealtimeReceiver exposes: noteOn(int noteNumber, int velocity), noteOff(int noteNumber), sendProgramChange(int channel, int program). Used by keyboard mapper and drum pad in Phase 3

### SoundFont
- **D-13:** FluidR3_GM.sf2 (141MB, MIT license) as the built-in SoundFont for Gervill synthesizer
- **D-14:** SoundFont placed in `src/main/resources/soundfonts/FluidR3_GM.sf2`, loaded via `getClass().getResourceAsStream()` at startup
- **D-15:** SoundFont loaded during application initialization. Success = proceed; failure = error dialog + exit

### Error Handling
- **D-16:** Corrupt/non-MIDI file → modal error dialog: "无法播放此文件 — 文件可能已损坏或不是标准 MIDI 格式". App remains open and functional
- **D-17:** Startup check: verify MIDI Synthesizer opens successfully AND SoundFont loads. Failure → dialog explaining the issue → app exits gracefully
- **D-18:** Orphaned NoteOn events (no corresponding NoteOff within 5 seconds) → engine auto-fires NoteOff. Console warning logged. User perceives no issue
- **D-19:** UI-level input constraints: BPM slider range 20-300, volume slider range 0-100%. Slider components physically cannot produce out-of-range values. Dragging in a new .mid file replaces the currently loaded file

### Claude's Discretion
- Exact color values and spacing of the placeholder panel
- Button icon choices for transport controls (play/pause/stop — standard Unicode or ASCII glyphs acceptable until Phase 4 SVG icons)
- Progress bar visual style (Radiance default slider look acceptable)
- Exact file dialog filter string
- Gradle project structure (single-module vs multi-module — Claude's call based on complexity assessment)
</decisions>

<specifics>
## Specific Ideas

- User wants the app to feel like a professional music tool from launch, even in Phase 1 — dark theme and clean layout are table stakes
- The right sidebar collapse animation should be smooth (foreshadowing Phase 4 glitch transitions)
- Placeholder panel should look intentional, not "broken" — centered branding, subtle dark gradient
</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Definition
- `.planning/PROJECT.md` — Project context, core value, constraints, key decisions
- `.planning/REQUIREMENTS.md` — Full v1 requirements with REQ-IDs and acceptance criteria

### Architecture & Research
- `.planning/research/STACK.md` — Radiance vs Aurora decision, javax.sound.midi rationale, Gradle recommendation
- `.planning/research/ARCHITECTURE.md` — System architecture, component boundaries, data flow, thread model
- `.planning/research/PITFALLS.md` — 9 pitfalls with prevention strategies, phase mapping (Pitfalls 4, 7, 8 apply to Phase 1)

### Roadmap
- `.planning/ROADMAP.md` § Phase 1 — Requirements, success criteria, key files list
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — greenfield project. No existing code to reuse.

### Established Patterns
- None — first phase. Establish patterns here for future phases to follow.

### Integration Points
- **Phase 2 (Visualization):** PianoRollPanel subscribes to NoteEventBus for active notes; reads PlaybackController.getPosition() for playhead sync
- **Phase 3 (Live Performance):** KeyboardMapper + DrumPadGrid call RealtimeReceiver.noteOn()/noteOff(); share the Synthesizer instance created in Phase 1
- **Phase 4 (Visual Polish):** ParticleSystem subscribes to NoteEventBus for particle triggers; GlitchTransition hooks into sidebar collapse/expand
</code_context>

<deferred>
## Deferred Ideas

- Custom title bar styling (Radiance window title pane customization) → Phase 4
- MIDI output device selection dropdown → Phase 3 (only if realtime latency is unacceptable)
- Multiple SoundFont support (user-loaded SF2 files) → Phase 5 or v2

</deferred>

---
*Phase: 01-foundation*
*Context gathered: 2026-04-27*
