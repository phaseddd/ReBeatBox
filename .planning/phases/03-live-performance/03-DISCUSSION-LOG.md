# Phase 3: Live Performance - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md — this log preserves the discussion.

**Date:** 2026-04-28
**Phase:** 03-live-performance
**Mode:** discuss (default)
**Areas discussed:** Keyboard Mapping, Drum Pad Design, UI Layout Integration, Keyboard Event Capture

## Area 1: Keyboard Mapping

| Question | Options Presented | User Selection |
|----------|------------------|----------------|
| 键盘覆盖的八度范围 | 两排一音阶 / 两排双音阶 / 三排全覆盖 | 三排全覆盖 |
| 音符力度 | 固定100 / 固定但可配置 / 随机力度 | 固定力度 100 |

**Outcome:**
- 数字行+QWERTY行+下排 = C3 到 C6 三完整八度
- 每排白键在下、黑键在上
- 所有键盘音符 velocity=100

## Area 2: Drum Pad Design

| Question | Options Presented | User Selection |
|----------|------------------|----------------|
| 鼓垫布局 | 4x4 / 3x4 / 2x8 | 4x4 = 16 pads |
| 音色映射 | GM 预设 / 可自定义 | 可自定义 |

**Outcome:**
- 4x4 MPC 风格网格，放在 SidebarPanel contentPanel
- 默认 GM 打击乐 Channel 10 预设
- 右键菜单切换音色，内存级配置

## Area 3: UI Layout Integration

| Question | Options Presented | User Selection |
|----------|------------------|----------------|
| 键盘提示位置 | SOUTH 底部 / PianoRoll 叠加 / 可折叠侧面板 | SOUTH 底部面板 |
| 鼓垫自定义 UI | 右键菜单 / 双击弹窗 / 独立设置面板 | Pad 右键菜单 |

**Outcome:**
- 新增 SOUTH KeyboardHintPanel（三排虚拟键盘 + 按键高亮）
- DrumPadGrid 右键 JPopupMenu 切换音色
- 布局: NORTH=ControlBar | CENTER=PianoRoll | EAST=DrumPadGrid | SOUTH=KeyboardHintPanel

## Area 4: Keyboard Event Capture

| Question | Options Presented | User Selection |
|----------|------------------|----------------|
| 事件捕获方式 | KeyboardFocusManager / JFrame setFocusable(false) / InputMap | KeyboardFocusManager 全局捕获 |
| 视觉反馈 | 虚拟键盘高亮 / 高亮+NoteEventBus / 暂不做 | 虚拟键盘 + NoteEventBus 广播 |

**Outcome:**
- KeyboardFocusManager.addKeyEventDispatcher() 全局捕获
- boolean[128] 防 OS 按键重复
- NoteEventBus 扩展 liveNoteOn/liveNoteOff
- KeyboardHintPanel 实时高亮 + PianoRollPanel 订阅事件

## Auto-Resolved

*(none — all decisions made interactively)*

## External Research

*(none — codebase analysis sufficient for all decisions)*
