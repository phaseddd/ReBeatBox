# Phase 1: Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md — this log preserves the discussion.

**Date:** 2026-04-27
**Phase:** 01-foundation
**Mode:** discuss (default interactive)
**Areas discussed:** Window Layout, MIDI Engine API, SoundFont Selection, Error Handling

## Window Layout

| Question | Options | Selected |
|----------|---------|----------|
| 控制栏位置 | 置顶 / 置底 / 浮动 | **置顶** — 顶部横条，下方大面积留给钢琴卷帘 |
| 主内容区 Phase 1 占位 | 暗色面板+logo / 波形占位图 / 全黑最大化 | **暗色面板+logo** — 1100x700，可缩放 |
| Drum Pad 侧边栏预留 | 可折叠右侧栏 / 不留 / 放底部 | **可折叠右侧栏** — Phase 1 就做折叠按钮 |
| 窗口标题与图标 | ReBeatBox+临时图标+NightShade / 现在就设计 / 系统原生 | **ReBeatBox+临时图标+NightShade** |

## MIDI Engine API

| Question | Options | Selected |
|----------|---------|----------|
| 音符事件分发机制 | 事件总线 / 轮询 / 直接回调 | **事件总线** — NoteEventBus，publisher/subscriber 解耦 |
| 播放与实时演奏分离 | RealtimeReceiver / 大一统 / 先只做播放 | **独立 RealtimeReceiver** — PlaybackController + RealtimeReceiver 共享 Synthesizer |
| 线程模型 | 事件总线保证EDT / 消费者自理 / 单线程 | **事件总线保证EDT** — fire() 内部 invokeLater |
| SoundFont/设备加载策略 | 自动加载+默认设备 / 提供设备选择 / Lazy加载 | **自动加载+默认设备** — 启动时加载，失败弹框 |

## SoundFont Selection

| Question | Options | Selected |
|----------|---------|----------|
| SoundFont 文件 | FluidR3_GM (141MB, MIT) / TimGM6mb (6MB, GPL) / JDK内置 | **FluidR3_GM.sf2** — 141MB, MIT, 音色饱满 |
| 打包方式 | Gradle 资源目录 / JAR外部 / 首次下载 | **src/main/resources/soundfonts/** — getResourceAsStream 加载 |

## Error Handling

| Question | Options | Selected |
|----------|---------|----------|
| 损坏的 .mid 文件 | 错误对话框 / 静默+日志 / Toast | **错误对话框** — 不崩溃，可继续使用 |
| 启动时音频不可用 | 检查+提示退出 / 降级运行 / 不检查 | **启动检查+提示退出** |
| 异常 MIDI 数据 | 容错+自动保护 / 严格模式 / 不做保护 | **容错+自动NoteOff** — 5秒超时保护 |
| 边界值防护 | 输入约束+防呆 / 后端验证 / 不管 | **输入约束+防呆** — 滑块范围限制，拖新文件替换旧文件 |

## Deferred Ideas
- 自定义标题栏样式 → Phase 4
- MIDI 输出设备选择 → Phase 3（仅实时延迟不可接受时）
- 多 SoundFont 支持（用户自加载 SF2）→ Phase 5 或 v2

---
*Discussion logged: 2026-04-27*
