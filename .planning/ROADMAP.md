# Roadmap: ReBeatBox

**Created:** 2026-04-27
**Granularity:** Standard (5 phases)
**Total v1 Requirements:** 22

## Phase Overview

| # | Phase | Goal | Reqs | Depends On | UI |
|---|-------|------|------|------------|:--:|
| 1 | Foundation | App shell + MIDI playback working end-to-end | 7 | — | yes |
| 2 | Visualization | Piano roll + falling notes sync to playback | 3 | Phase 1 | yes |
| 3 | Live Performance | Keyboard + drum pad, overlay on playback | 4 | Phase 1 | yes |
| 4 | Visual Polish | Full cyber/glitch aesthetic | 6 | Phase 2, 3 | yes |
| 5 | Content & Ship | Demo songs, polish, ship-ready | 2 | Phase 1 | no |

**Execution Strategy:** Phase 2 and Phase 3 are independent — both depend only on Phase 1. They can run in parallel.

```
Phase 1 ──┬── Phase 2 ──┬── Phase 4
          │              │
          └── Phase 3 ──┘
          
Phase 5 (parallel with 2-4, gates on 4 for ship)
```

---

## Phase 1: Foundation — "Hear It"

**Goal:** App opens with dark theme, loads MIDI files, plays with full transport controls.

**Requirements:**
- PLAY-01: Load MIDI files (.mid) via file dialog or drag-and-drop
- PLAY-02: Play, pause, stop, restart playback
- PLAY-03: BPM tempo slider
- PLAY-04: Master volume slider
- PLAY-05: Progress bar with seek
- UI-01: Dark theme (Radiance NightShade skin)
- CONT-02: Drag-and-drop MIDI file import

**Success Criteria:**
1. App launches with dark theme window, no errors
2. Open a .mid file → audio plays through Gervill synthesizer
3. Pause pauses, resume resumes, stop stops and resets to beginning
4. BPM slider changes playback speed in real-time without audible glitches
5. Volume slider changes output volume smoothly
6. Progress bar reflects current position; dragging jumps to that position
7. Dragging a .mid file onto the window loads and plays it
8. App plays correctly for at least 5 different .mid files of varying complexity

**UI hint:** yes
**Key files:** `engine/MidiFileLoader.java`, `engine/PlaybackController.java`, `ui/ReBeatBoxWindow.java`, `ui/ControlBar.java`

---

## Phase 2: Visualization — "See It"

**Goal:** Piano roll view + falling notes animation, perfectly synced to MIDI playback.

**Requirements:**
- VIS-01: Piano roll view (horizontal timeline, vertical pitch grid)
- VIS-02: Falling notes animation (Synthesia-style)
- VIS-03: Playback position indicator (playhead/highlight)

**Plans:** 2 plans in 2 waves

Plans:
- [ ] 02-01-PLAN.md — RenderNote data model, MIDI pre-scan, pitch-to-neon-color mapping
- [ ] 02-02-PLAN.md — PianoRollPanel: three-layer compositing, GaussianBlur glow, 60fps animation, ReBeatBoxWindow integration

**Wave Structure:**

| Wave | Plans | Description |
|------|-------|-------------|
| 1 | 02-01 | Data layer (no UI dependencies) |
| 2 | 02-02 | Rendering + integration (depends on 02-01) |

**Success Criteria:**
1. Piano roll displays with correct note-to-pitch grid mapping
2. During playback, note bars scroll/fall in sync with audio (no visible lag)
3. Playhead moves smoothly across timeline, matching audio position to within 50ms
4. Notes that have passed the playhead are visually distinct from upcoming notes
5. Visualization works at 60fps for MIDI files with up to 20 simultaneous notes

**UI hint:** yes
**Key files:** `visual/PianoRollPanel.java`, `visual/RenderNote.java`, `visual/MidiPreScanner.java`, `visual/NoteColorMapper.java`, `ui/ReBeatBoxWindow.java`

---

## Phase 3: Live Performance — "Play It"

**Goal:** Keyboard triggers MIDI notes, drum pad grid works, overlay on playback.

**Requirements:**
- LIVE-01: Computer keyboard → MIDI note mapping (QWERTY row = one octave)
- LIVE-02: Key press triggers note, release stops note
- LIVE-03: Live notes play over background MIDI playback (overlay mode)
- LIVE-04: Drum pad grid — each pad triggers a different percussion sound

**Success Criteria:**
1. Pressing QWERTY keys triggers audible MIDI notes with <30ms perceived delay
2. Releasing keys correctly sends NoteOff; no stuck notes
3. At least 4 keys can be held simultaneously (polyphony)
4. Drum pad grid displays correctly; clicking a pad triggers its assigned percussion sound
5. Live notes play simultaneously with background MIDI playback without audio glitches
6. Both keyboard and drum pad send notes to the same Synthesizer as the Sequencer

**UI hint:** yes
**Key files:** `live/KeyboardMapper.java`, `live/DrumPadGrid.java`, `live/PadButton.java`, `engine/RealtimeReceiver.java`

---

## Phase 4: Visual Polish — "Feel It"

**Goal:** Full cyber/glitch aesthetic — particles, glow, transitions, neon palette, SVG icons.

**Requirements:**
- UI-02: Neon color palette applied across all UI elements
- UI-03: Button hover/press animation feedback via radiance-animation
- GLITCH-01: Particle burst effect on every MIDI note-on event
- GLITCH-02: Glitch art transition animations on view switches
- GLITCH-03: Note tracks rendered with neon glow lines
- GLITCH-04: Custom cyber-styled SVG icon set

**Success Criteria:**
1. Particle burst appears on every note trigger (playback and live), max 200 particles with consistent 60fps
2. Glitch transition plays when switching between views/modes
3. Piano roll note tracks have visible neon glow effect
4. All toolbar and control icons use custom cyber SVG set (not default Radiance icons)
5. Neon color palette is consistently applied — no hardcoded non-theme colors remain
6. All buttons/controls have smooth hover and press animation feedback
7. Visual effects do not cause perceptible audio latency or frame drops

**UI hint:** yes
**Key files:** `visual/ParticleSystem.java`, `visual/GlitchTransition.java`, `ui/ThemeManager.java`, `ui/SvgIconLoader.java`

---

## Phase 5: Content & Ship — "Share It"

**Goal:** Built-in demos, recent files list, ship-ready packaging.

**Requirements:**
- CONT-01: 3-5 built-in demo MIDI songs (public domain / CC)
- CONT-03: Recent files list (last 10 opened)

**Success Criteria:**
1. Demo songs menu lists 3-5 tracks with names
2. Selecting a demo song loads and plays it correctly
3. Recent files list shows last 10 opened files, persisted across app restarts
4. Selecting a recent file loads and plays it
5. Application can be built into a single runnable JAR with all dependencies

**UI hint:** no
**Key files:** `ui/SidePanel.java` (demo list), `ui/RecentFilesManager.java`

---

## Requirement Coverage

| Requirement | Phase | Status |
|-------------|-------|--------|
| PLAY-01 | Phase 1 | Pending |
| PLAY-02 | Phase 1 | Pending |
| PLAY-03 | Phase 1 | Pending |
| PLAY-04 | Phase 1 | Pending |
| PLAY-05 | Phase 1 | Pending |
| UI-01 | Phase 1 | Pending |
| CONT-02 | Phase 1 | Pending |
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

**Coverage:** 22/22 v1 requirements mapped ✓

---
*Roadmap created: 2026-04-27*
*Last updated: 2026-04-28 — Phase 2 planned*
