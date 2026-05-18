---
status: complete
phase: 03-live-performance
source: 03-01-SUMMARY.md, 03-02-SUMMARY.md, 03-03-SUMMARY.md, 03-04-SUMMARY.md, 03-05-SUMMARY.md
started: 2026-05-18T00:00:00Z
updated: 2026-05-18T00:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Cold Start Smoke Test
expected: 杀掉所有正在运行的 Java 进程。从零启动应用（gradlew run）。应用应无报错启动，主窗口显示，Gervill 合成器初始化成功，所有四个区域（ControlBar / PianoRollPanel / DrumPadGrid / KeyboardHintPanel）正常渲染。
result: pass

### 2. 四区域布局 + KeyboardHintPanel 显示
expected: 窗口分为四个区域 — NORTH: ControlBar（不变），CENTER: PianoRollPanel（不变），EAST: SidebarPanel 内含 4x4 鼓垫网格，SOUTH: KeyboardHintPanel 显示三排虚拟键盘（数字行/QWERTY行/下排行），每排白键在下、黑键在上。
result: pass

### 3. 键盘 MIDI 触发
expected: 在应用窗口获得焦点时，按下键盘映射键（如 QWERTY 行的 Q→C4, W→C#4, E→D4...），应听到对应 MIDI 音符通过扬声器/耳机播放。松开按键音符停止。所有音符 velocity=100，Channel 0。
result: pass

### 4. KeyboardHintPanel 按键高亮
expected: 按下映射键时，虚拟键盘上对应的键位瞬间切换为霓虹青色（Cyan 0x00E5FF）高亮——白键填充变亮+边框 2px 霓虹、黑键同样高亮变色。松开后立即恢复暗色空闲态。多键同时按下时各自独立高亮。
result: pass

### 5. DrumPadGrid 鼓垫网格显示
expected: EAST 侧边栏 contentPanel 显示 4x4=16 个鼓垫，带标签：Kick, Snare, Cl Hat, Op Hat / Clap, Crash, Ride, Tom Hi / Tom Mid, Tom Lo, Rimshot, Cowbell / Claves, Maracas, Shaker, Triangle。每个 pad 48x48px，暗色填充+暗色边框。
result: pass

### 6. 鼓垫 MIDI 触发
expected: 鼠标点击鼓垫（如 Kick）→ pad 边框变为霓虹青 2px 发光、填充变暗青色 → 听到对应 GM 打击乐声音（Channel 10）→ 松开后 pad 恢复默认外观。200ms 视觉保持使得极短点击也能看到发光反馈。
result: pass
reported: "听到的是钢琴声，每个鼓垫都是钢琴声"
severity: minor
fix: "PadButton.java:24 DRUM_CHANNEL 10→9（ShortMessage 0-based: Channel 9=GM Percussion, Channel 10=旋律默认钢琴）"

### 7. 鼓垫右键音色重新分配
expected: 右键点击某个鼓垫 → 弹出 JPopupMenu，含分类子菜单（Kicks/Snares/Hi-Hats/Cymbals/Toms/Percussion/Effects）→ 选择一个新音色（如 "Cowbell (56)"）→ pad 标签更新为新音色名，后续点击播放新音色。
result: pass

### 8. 鼓垫 Reset to Default
expected: 右键已修改的鼓垫 → 菜单底部 "Reset to {default_label}"（由 JSeparator 分隔）→ 弹出确认对话框 "Reset pad '{current_label}' to default sound '{default_label}'?" → 点 Yes → pad 恢复默认标签和 MIDI 音符。
result: pass

### 9. 实时演奏叠加 MIDI 播放
expected: 先加载并播放一个 MIDI 文件（背景有音符在滚动），同时按下键盘键或点击鼓垫 → 实时音符和背景 MIDI 同时发声，互不干扰，无卡顿无爆音。停止播放时 All Notes Off (CC 123) 清理所有声道，实时音符也被终止。
result: pass
reported: "效果可以。但大音量 MIDI 文件播放时键盘/鼓垫几乎听不到——setVolume() 的 CC 7 会波及 live 通道，且被 MIDI 文件自身 CC 7 覆盖。"

### 10. 窗口失焦时 Stuck Note 清理
expected: 按住一个键盘映射键不放 → 按键高亮显示 → Alt+Tab 切到其他窗口（应用失去焦点）→ 切回应用 → 所有按键高亮已清除，没有卡住的音符持续响。KeyboardHintPanel 回到全部空闲态。
result: pass

### 11. 多键同时按下
expected: 同时按住 2-3 个不同的映射键（如 Q + W + E），应听到多个音符和弦同时发声，每个键各自独立高亮。逐一松开时对应音符逐个停止。OS 按键重复（auto-repeat）不产生鬼音——只有首次 KEY_PRESSED 触发音符。
result: pass

## Summary

total: 11
passed: 11
issues: 2
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "点击鼓垫听到对应 GM 打击乐声音"
  status: resolved
  reason: "每个鼓垫都是钢琴声"
  severity: minor
  test: 6
  root_cause: "PadButton.DRUM_CHANNEL = 10，但 ShortMessage 是 0-based：索引 10 = MIDI Channel 11（旋律默认钢琴），GM 打击乐在索引 9 = MIDI Channel 10"
  fix: "DRUM_CHANNEL 10 → 9"
  artifacts:
    - path: "src/main/java/com/rebeatbox/live/PadButton.java"
      issue: "行 24 DRUM_CHANNEL = 10 应为 9"
  missing: []

- truth: "实时演奏与 MIDI 播放音量平衡"
  status: open
  reason: "大音量 MIDI 文件播放时键盘/鼓垫几乎听不到——setVolume() 通过 CC 7 波及 live 通道（0, 9），且被 MIDI 文件自身 CC 7 事件覆盖"
  severity: major
  test: 9
  root_cause: "PlaybackController.setVolume() 用 CC 7 控制全部 16 通道，但 Sequencer 播放时 MIDI 文件内嵌的 CC 7 会覆盖设定值。Live 通道（0 键盘, 9 鼓）被波及后无恢复机制"
  artifacts:
    - path: "src/main/java/com/rebeatbox/engine/PlaybackController.java"
      issue: "setVolume() 无差别向 16 通道发送 CC 7，无 live 通道保护"
  missing:
    - "方案：Sequencer 输出经 VelocityScaledReceiver 缩放，live 路径直连 Synthesizer"
    - "或：setVolume 跳过 Channel 0 和 Channel 9"
