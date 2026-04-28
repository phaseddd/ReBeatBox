# Phase 3: Live Performance - Context

**Gathered:** 2026-04-28
**Status:** Ready for planning

<domain>
## Phase Boundary

键盘触发 MIDI 音符 + 鼓垫网格 + 实时演奏叠加在背景 MIDI 播放之上。用户既能听自动播放，也能亲手敲出节奏，全程共享同一个 Gervill Synthesizer。

**In scope:** Keyboard mapping (LIVE-01), NoteOn/NoteOff (LIVE-02), Overlay mode (LIVE-03), Drum pad grid (LIVE-04)
**Not in scope:** 粒子效果 (Phase 4), Glitch 转场 (Phase 4), 自定义 SVG 图标 (Phase 4)
</domain>

<decisions>
## Implementation Decisions

### Keyboard Mapping
- **D-01:** 三排全覆盖布局 — 数字行 (C5-C6)、QWERTY 行 (C4-C5)、下排 (C3-C4)。每排白键在下、黑键在上，覆盖 3 个完整八度。
  ```
  上八度 (数字行): 1  2  3  4  5  6  7  8  9  0  -  =
                  C5 C# D5 D# E5 F5 F# G5 G# A5 A# B5 C6
  中八度 (QWERTY): Q  W  E  R  T  Y  U  I  O  P  [  ]
                  C4 C# D4 D# E4 F4 F# G4 G# A4 A# B4 C5
  下八度 (下排):   Z  X  C  V  B  N  M  ,  .  /
                  C3 C# D3 D# E3 F3 F# G3 G# A3 A# B3
  ```
- **D-02:** 所有键盘音符固定 velocity=100，简单直接。
- **D-03:** 键盘事件通过 `KeyboardFocusManager.addKeyEventDispatcher()` 全局捕获，无论焦点在哪个组件都能收到。用 `boolean[128]` 跟踪已按下的键，防 OS 按键重复事件导致鬼音。

### Drum Pad Design
- **D-04:** 4x4 = 16 pads，经典 MPC 风格。放在 SidebarPanel 预留的 contentPanel 中。
- **D-05:** 默认加载 GM 打击乐预设 (MIDI Channel 10): 36=Kick, 38=Snare, 42=Closed Hat, 46=Open Hat, 39=Clap, 49=Crash 等。
- **D-06:** 右键点击 pad → 弹出 JPopupMenu 选择 MIDI 音符/音色。每个 pad 独立可配，配置在内存中保持（不持久化）。

### UI Layout
- **D-07:** 窗口布局调整为四区域：
  - NORTH: ControlBar (不变)
  - CENTER: PianoRollPanel (不变)
  - EAST: SidebarPanel → DrumPadGrid (4x4 pads + 右键菜单)
  - SOUTH: **新增** KeyboardHintPanel (三排虚拟键盘 + 按键高亮)
- **D-08:** KeyboardHintPanel 显示三排键位映射，被按下的键实时高亮（霓虹色边框/背景切换）。

### Live Overlay & Event Bus
- **D-09:** 实时音符和 Sequencer 播放共享同一个 `synthesizer.getReceiver()`。MIDI 协议原生支持多源叠加，无需额外混音逻辑。
- **D-10:** NoteEventBus 扩展 `liveNoteOn(int note, int velocity)` 和 `liveNoteOff(int note)` 事件，PianoRollPanel 订阅以显示实时音符闪烁。
- **D-11:** Sequencer stop 时自动发送 All Notes Off (CC 123) 清理所有声道，防止实时音符残留成"鬼音"。

### Claude's Discretion
- 三排键盘的具体键→MIDI音符映射表（从 D-01 布局推导即可）
- KeyboardHintPanel 的配色方案（跟随 Radiance NightShade 暗色主题 + 霓虹高亮）
- PadButton 的视觉风格（赛博风格，方角，边框发光）
- 右键菜单的具体 MIDI 音符列表和 UI 结构
- NoteEventBus 扩展 API 的具体方法签名
- KeyboardFocusManager 注册时机和去重策略细节
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing MIDI Engine
- `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java` — 已封装 `noteOn(note, velocity)` / `noteOff(note)` / `sendProgramChange()`，直接用于键盘和鼓垫输出
- `src/main/java/com/rebeatbox/engine/PlaybackController.java` — Sequencer 管理、volume/BPM 控制、activeNote 追踪。RealtimeReceiver 和它共享同一个 `synthesizer.getReceiver()`
- `src/main/java/com/rebeatbox/engine/NoteEventBus.java` — 当前只广播 `Set<Integer> activeNotes`，Phase 3 需扩展 `liveNoteOn`/`liveNoteOff`

### Existing UI
- `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` — 主窗口布局，已有 NORTH/CENTER/EAST。Phase 3 新增 SOUTH 和键盘事件注册
- `src/main/java/com/rebeatbox/ui/SidebarPanel.java` — 已预留 `contentPanel`（注释: "reserved for Phase 3 drum pad"），240px 宽可折叠
- `src/main/java/com/rebeatbox/App.java` — 应用入口，已创建 `RealtimeReceiver` 实例并传入 `wireEngine()`

### Project Constraints
- `.planning/PROJECT.md` — 赛博/Glitch 风格要求、Java 17 + Radiance 8.5.0、MIDI 为底层协议
- `.planning/ROADMAP.md` §Phase 3 — 完整需求和成功标准
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **RealtimeReceiver**: 已完整实现 noteOn/noteOff/programChange，零改动直接复用
- **SidebarPanel.contentPanel**: 空 JPanel 预留位置，直接添加 DrumPadGrid
- **PlaybackController.resetActiveNotes()**: 已有 All Notes Off (CC 123) 逻辑，stop 时天然清理实时音符
- **NoteEventBus + CopyOnWriteArrayList**: 线程安全的订阅模式，扩展事件类型零风险

### Established Patterns
- **合成器共享模式**: PlaybackController 和 RealtimeReceiver 都通过 `synthesizer.getReceiver()` 发送 MIDI——Gervill 的 Receiver 是线程安全的多源接收器
- **Swing Timer 轮询**: PlaybackController 用 50ms Timer 追踪 activeNotes。键盘事件是即时触发，不依赖轮询
- **Radiance 皮肤**: NightShadeSkin 在 App.java 的 main() 最开始设置，所有新组件自动继承暗色主题

### Integration Points
- **ReBeatBoxWindow.wireEngine()**: 已经接收 `RealtimeReceiver` 参数但未使用。键盘/鼓垫组件在此方法中注册到 receiver
- **App.java main()**: 已创建 `RealtimeReceiver` 实例，传给 window。无需修改启动流程
- **PianoRollPanel**: 已订阅 NoteEventBus 的 `onActiveNotesChanged`。扩展 NoteEventBus 后直接加 `onLiveNoteOn`/`onLiveNoteOff` 监听
</code_context>

<specifics>
## Specific Ideas

- 用户明确要三排键盘，覆盖 C3-C6——不是常见的单八度方案，是冲着"当 MIDI 键盘用"去的
- 鼓垫要能自定义音色（右键菜单），不是死绑定 GM 预设——虽然默认值用 GM 标准
- 键盘反馈要"高亮 + 事件广播"——为 Phase 4 粒子效果留接口但不越界做复杂动画
</specifics>

<deferred>
## Deferred Ideas

- 键盘力度可调节滑块 → Phase 4 或后续
- Pad 配置持久化（保存/加载自定义映射）→ 后续 phase
- 粒子效果 + Glitch 转场 → Phase 4
- 鼓垫键盘快捷键绑定 → 后续 phase（需求未覆盖）
</deferred>

---

*Phase: 03-live-performance*
*Context gathered: 2026-04-28*
