# Requirements: ReBeatBox

**Defined:** 2026-04-27
**Core Value:** 打开应用，音乐就在指尖——既能听着音符自动流淌，也能亲手敲出节奏，全程有赛博朋克式的视觉反馈。

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Playback (PLAY)

- [ ] **PLAY-01**: User can load standard MIDI files (.mid) via file dialog or drag-and-drop
- [ ] **PLAY-02**: User can play, pause, stop, and restart playback
- [ ] **PLAY-03**: User can adjust playback tempo (BPM) via slider
- [ ] **PLAY-04**: User can adjust master volume via slider
- [ ] **PLAY-05**: User can seek to any position via progress bar during playback

### Visualization (VIS)

- [ ] **VIS-01**: User sees a piano roll view — horizontal timeline, vertical pitch grid
- [ ] **VIS-02**: User sees falling notes animation (Synthesia-style) synced to playback
- [ ] **VIS-03**: User sees a clear playback position indicator (playhead / highlight)

### Live Performance (LIVE)

- [ ] **LIVE-01**: User can map computer keyboard keys to MIDI notes (default: QWERTY row = one octave)
- [ ] **LIVE-02**: Pressing a mapped key triggers a MIDI note; releasing stops it
- [ ] **LIVE-03**: Live key presses play over background MIDI file playback (overlay mode)
- [ ] **LIVE-04**: User sees and can click a Drum Pad grid — each pad triggers a different percussion sound

### Visual Theme (UI)

- [ ] **UI-01**: Application uses a dark theme (Radiance NightShade skin) as visual foundation
- [ ] **UI-02**: Neon color palette applied across all UI elements (note tracks, buttons, indicators)
- [ ] **UI-03**: All buttons and interactive controls have hover/press animation feedback via radiance-animation

### Cyber/Glitch Effects (GLITCH)

- [ ] **GLITCH-01**: Particle burst effect triggers on every MIDI note-on event (in both playback and live modes)
- [ ] **GLITCH-02**: Glitch art transition animations on window/view switches
- [ ] **GLITCH-03**: Note tracks rendered with neon glow lines (Graphics2D glow effect)
- [ ] **GLITCH-04**: Custom cyber-styled SVG icon set (play, pause, stop, drum, keyboard, etc.)

### Content (CONT)

- [ ] **CONT-01**: Application ships with 3-5 built-in demo MIDI songs (public domain / CC-licensed)
- [ ] **CONT-02**: User can drag and drop .mid files from file explorer into the application window
- [ ] **CONT-03**: Application maintains a recent files list (last 10 opened files)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

- **MTRK-01**: Multi-track view — separate piano roll per MIDI track with instrument labels
- **MTRK-02**: Track mute/solo buttons per instrument track
- **MXL-01**: MusicXML (.xml/.mxl) file import via ProxyMusic library
- **MXL-02**: MusicXML → MIDI conversion pipeline
- **FX-01**: Real-time audio effects knob (filter/reverb/delay) using JSyn
- **FX-02**: Effect parameter automation alongside MIDI playback
- **LOOP-01**: Loop mode — select and repeat a section of the timeline
- **MIDI-01**: External MIDI device input support via ktmidi

## Out of Scope

| Feature | Reason |
|---------|--------|
| Audio sample editing (WAV/MP3) | DAW territory — extreme complexity, not MIDI-focused |
| Sheet music rendering (staff notation) | Typesetting-engine complexity; MusicXML → notation is a separate research domain |
| VST plugin support | Requires native audio pipeline; Java fundamentally unsuited |
| Recording / audio export | Audio input pipeline not in scope; MIDI export can be v2 |
| Multi-language i18n | Personal project |
| Mobile / Web version | Java desktop only |
| AI music generation | Core value is playback + performance, not generation |
| External MIDI device input (v1) | Platform compatibility risk; defer to v2 |
| Real-time audio effects (v1) | Requires JSyn integration; defer to v2 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PLAY-01 | Phase 1 | Pending |
| PLAY-02 | Phase 1 | Pending |
| PLAY-03 | Phase 1 | Pending |
| PLAY-04 | Phase 1 | Pending |
| PLAY-05 | Phase 1 | Pending |
| CONT-02 | Phase 1 | Pending |
| UI-01 | Phase 1 | Pending |
| VIS-01 | Phase 2 | Pending |
| VIS-02 | Phase 2 | Pending |
| VIS-03 | Phase 2 | Pending |
| LIVE-01 | Phase 3 | Pending |
| LIVE-02 | Phase 3 | Pending |
| LIVE-03 | Phase 3 | Pending |
| LIVE-04 | Phase 3 | Pending |
| UI-02 | Phase 4 | Pending |
| UI-03 | Phase 4 | Pending |
| GLITCH-01 | Phase 4 | Pending |
| GLITCH-02 | Phase 4 | Pending |
| GLITCH-03 | Phase 4 | Pending |
| GLITCH-04 | Phase 4 | Pending |
| CONT-01 | Phase 5 | Pending |
| CONT-03 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 22 total
- Mapped to phases: 22
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-27*
*Last updated: 2026-04-27 after initial definition*
