# Phase 2: Visualization - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md — this log preserves the discussion flow.

**Date:** 2026-04-27
**Phase:** 02-visualization
**Mode:** discuss (interactive, default)
**Areas discussed:** 下落音符布局与方向, 音符色彩方案, 渲染架构与多轨策略, 播放头与同步机制

## Discussion Summary

### Area 1: 下落音符布局与方向

| Question | Options | Selected | Notes |
|----------|---------|----------|-------|
| Visual paradigm | Synthesia waterfall / Horizontal piano roll / Hybrid | Synthesia waterfall | |
| Piano keyboard reference | Left mini keyboard + adaptive range / Grid only / Full 88 keys | Left mini keyboard + adaptive range | 40-60px wide, shows octave separators |
| Trigger line position | Bottom 15-20% / Mid-screen / Top trigger | Bottom 15-20% (standard) | Notes preview ~2-3s ahead |
| Note bar shape | Fixed-width elongated / Uniform blocks + trails / Velocity-sensitive width | Fixed-width elongated | Bar length = note duration, chords = adjacent bars |

### Area 2: 音符色彩方案

| Question | Options | Selected | Notes |
|----------|---------|----------|-------|
| Color scheme | By pitch (rainbow) / Uniform neon / By instrument track | By pitch (rainbow gradient) | Low=purple/blue, high=red/orange |
| Past/future distinction | Past dimmed + skip velocity / Three-state / Velocity-mapped brightness | Past dimmed transparent (~40%), v1 ignores velocity | Velocity deferred to Phase 4 |
| Canvas background | Dark grid / Pure black + glow / Dark + gradient band | Pure black + real GaussianBlur glow | Per-note independent blur |
| Glow implementation | Multi-layer stroke / True GaussianBlur / Glow only no grid | True GaussianBlur (Java2D ConvolveOp) | User prioritized visual quality |

### Area 3: 渲染架构与多轨策略

| Question | Options | Selected | Notes |
|----------|---------|----------|-------|
| Render pipeline | Three-layer composite / Per-note blur + composite / Single-pass direct | Per-note independent blur + composite | Each note: own BufferedImage → GaussianBlur → composite |
| Multi-track strategy | Merge + skip Track 0 / Merge fully uniform / Layered toggle | Merge all tracks, skip Track 0 | v1 no per-track visual distinction |
| MIDI pre-scan | Pre-scan List<RenderNote> / Per-frame scan / Pre-scan + time buckets | Pre-scan on load, ArrayList sorted by startMicros | Binary search at render time |
| Performance guardrails | Viewport culling + 30 cap / Pure culling no cap / Aggressive 20 + degrade | Pure viewport culling, no artificial cap | Trust JVM; only cull off-screen notes |

### Area 4: 播放头与同步机制

| Question | Options | Selected | Notes |
|----------|---------|----------|-------|
| Trigger line style | Neon glow + pulse animation / Static neon line / Glow band zone | Neon glow line + pulse animation | 2px white/cyan + GaussianBlur + opacity pulse 80-100% |
| Sync mechanism | Independent 60fps Timer + direct poll / Pure NoteEventBus / Radiance Timeline | Independent javax.swing.Timer(16ms) + direct controller polling | Does NOT depend on NoteEventBus 50ms interval |
| Seek & pause behavior | Instant jump + freeze / Quick wipe animation / Pause slow pulse | Instant jump + freeze on pause | No transition animation for seek |
| Active note data source | Pre-scanned list + NoteEventBus supplement / NoteEventBus only / Pre-scan only | Pre-scanned List<RenderNote> binary search; NoteEventBus reserved for Phase 4 | Two data sources, independent. Visualization not gated on 50ms polling. |

## Claude's Discretion Items

- Exact neon color → RGB mapping table
- GaussianBlur kernel size and sigma
- Pulse animation easing curve
- RenderNote class field design
- Viewport culling window fine-tuning
- Adaptive note range padding strategy
- Mini keyboard exact drawing dimensions and colors

## Deferred Ideas

- Velocity → brightness mapping → Phase 4
- Per-track layer toggle → v2 (MTRK-01, MTRK-02)
- Time-bucket indexing optimization → profile-driven
- Multi-track color differentiation → v2
