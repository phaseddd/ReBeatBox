---
phase: 3
slug: live-performance
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-28
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 5.10.0 |
| **Config file** | build.gradle (test block: `useJUnitPlatform()`) |
| **Quick run command** | `./gradlew test --tests "com.rebeatbox.live.*"` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "com.rebeatbox.live.*"`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-xx-01 | 01 | 1 | LIVE-01 | T-03-01 | KeyboardMapper returns -1 sentinel for unmapped keys; valid note range 0-127 guaranteed by static table | unit | `./gradlew test --tests "KeyboardMapperTest"` | ❌ W0 | ⬜ pending |
| 03-xx-02 | 01 | 1 | LIVE-02 | T-03-02 | boolean[128] dedup prevents MIDI message storm; key release always sends NoteOff | unit | `./gradlew test --tests "KeyboardMapperTest"` | ❌ W0 | ⬜ pending |
| 03-xx-03 | 02 | 2 | LIVE-03 | T-03-03 | Synthesizer guaranteed initialized at startup (System.exit(1) on failure); Receiver.send() called from EDT only | integration | Manual UAT — audio loopback required | N/A — manual | ⬜ pending |
| 03-xx-04 | 02 | 2 | LIVE-04 | T-03-04 | MIDI note range check 0-127; velocity fixed at 100; Channel 10 for percussion | unit | `./gradlew test --tests "DrumPadGridTest"` | ❌ W0 | ⬜ pending |
| 03-xx-05 | 02 | 2 | LIVE-04 | T-03-04 | PadButton sends NoteOn on press, NoteOff on release; visual hold 200ms via javax.swing.Timer | unit | `./gradlew test --tests "PadButtonTest"` | ❌ W0 | ⬜ pending |
| 03-xx-06 | 01 | 1 | D-06 | — | N/A | unit | `./gradlew test --tests "DrumPadGridTest"` | ❌ W0 | ⬜ pending |
| 03-xx-07 | 01 | 1 | D-10 | — | N/A | unit | `./gradlew test --tests "NoteEventBusLiveTest"` | ❌ W0 | ⬜ pending |
| 03-xx-08 | 01 | 1 | D-11 | — | N/A | unit | `./gradlew test --tests "PlaybackControllerTest"` | ❌ W0 | ⬜ pending |
| 03-xx-09 | 01 | 1 | — | — | RealtimeReceiver channel overload validates note 0-127, channel 0-15 | unit | `./gradlew test --tests "RealtimeReceiverChannelTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/rebeatbox/live/KeyboardMapperTest.java` — covers LIVE-01, LIVE-02 (QWERTY mapping + dedup + three octaves)
- [ ] `src/test/java/com/rebeatbox/live/DrumPadGridTest.java` — covers LIVE-04, D-06 (16 pads defaults + reassign)
- [ ] `src/test/java/com/rebeatbox/live/PadButtonTest.java` — covers LIVE-04 (NoteOn/Off on Channel 10)
- [ ] `src/test/java/com/rebeatbox/engine/NoteEventBusLiveTest.java` — covers D-10 (liveNoteOn/liveNoteOff events)
- [ ] `src/test/java/com/rebeatbox/engine/RealtimeReceiverChannelTest.java` — validates channel overload
- [ ] `src/main/java/com/rebeatbox/live/` directory — new Java package (3 new classes)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real-time overlay (live notes + Sequencer simultaneous) | LIVE-03 | Requires audio perception — no automated audio quality assertion in JUnit | Load a MIDI file, start playback, press QWERTY keys. Verify: (1) both audio sources audible simultaneously, (2) no glitches/crackling, (3) stop button cleans all notes |
| Keyboard feel latency | LIVE-02 | Perceived latency measurement needs human judgment + specialized hardware | Press keys rapidly, verify <30ms perceived delay between key press and audio onset |
| Polyphony (4+ simultaneous keys) | LIVE-02 | Audio quality under load needs human ear | Hold 4+ keys simultaneously, verify all notes sustain without dropouts |
| Drum pad visual feedback | LIVE-04 | Visual timing perception | Click pads at varying speeds, verify 200ms glow is visible even on very short clicks |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
