# Stack Research: ReBeatBox

## Executive Summary

推荐技术栈：**Java 17 + Radiance (Swing) + javax.sound.midi + Gradle**。这是唯一能在 Java 桌面端实现"赛博/Glitch 风音乐应用"且不引入过度复杂度的方案。

---

## UI 框架选择：Radiance vs Aurora

### kirill-grouchnikov 三仓库关系

```
Ephemeral (设计系统 — 抽象设计 token/色彩/皮肤/组件形态)
├── Radiance (Swing 实现) — BSD-3-Clause | 891 stars | v8.5.0
└── Aurora (Compose 实现) — Apache 2.0 | 645 stars | v2.0.0
```

Ephemeral 是共享设计语言。Radiance 和 Aurora 共用同一套：
- 色彩 token 系统（color tokens, association kinds, bundles）
- 皮肤（Nebula, Gemini, GraphiteChalk, NightShade — 均有 Light/Dark 变体）
- Painter 体系（surface, outline, highlight, decoration, overlay）
- 组件形态（Command, CommandButton, Ribbon, BreadcrumbBar, CommandPanel）

### 结论：选 Radiance

| 维度 | Radiance (Swing) | Aurora (Compose) |
|------|---------|--------|
| 语言 | **Pure Java** + 可选 Kotlin 扩展 | Kotlin DSL（Compose），Java 互操作极差 |
| 动画 | ✅ `radiance-animation` — 完整动画引擎 | ❌ 依赖 Compose 动画框架（Kotlin 限定） |
| 皮肤系统 | ✅ 内置多套 Dark skin | ✅ 同 Ephemeral 皮肤 |
| 成熟度 | 891 stars, v8.5.0, 2018 至今 | 645 stars, v2.0.0, 2020 至今 |
| SVG 图标 | ✅ `radiance-svg-transcoder` | ✅ `aurora-tools-svg-transcoder` |
| 自定义绘制 | ✅ Swing Canvas/Graphics2D 完全可控 | ⚠️ Compose Canvas（Kotlin） |
| 对项目匹配度 | **完美** — 纯 Java, 动画支持, 赛博风皮肤 | 不合适 — Kotlin-first, Compose 限制 |

### Radiance 关键模块

| 模块 | 用途 | ReBeatBox 怎么用 |
|------|------|------------------|
| `radiance-theming` | Swing 皮肤/主题系统 | NightShade Dark 皮肤作为赛博风基底 |
| `radiance-animation` | 动画引擎（补间/多动画编排） | 音符跳动、粒子效果、按钮反馈 |
| `radiance-component` | CommandButton/Ribbon/BreadcrumbBar | 控件栏、DJ pad 按钮 |
| `radiance-common` | 高 DPI 图标/字体 | 跨分辨率 UI 保持清晰 |
| `radiance-svg-transcoder` | SVG → Java2D 代码（离线转码） | 自定义赛博风图标 |

---

## MIDI / 音乐引擎

### 核心引擎：javax.sound.midi（JDK 内置）

**为什么选它而不是 ktmidi/JSyn：**

| 维度 | javax.sound.midi | ktmidi | JSyn |
|------|-----------------|--------|------|
| 依赖 | **JDK 内置，零依赖** | Kotlin Maven 依赖 | 纯 Java Maven 依赖 |
| MIDI 文件播放 | ✅ Sequencer API | ✅ MidiPlayer | ❌（音频合成，非 MIDI 播放） |
| 实时 MIDI 触发 | ✅ Receiver/Transmitter | ✅ RtMidi backend | ⚠️ MIDI→音频链路 |
| SoundFont 合成 | ✅ Gervill（内置） | ⚠️ 需要外部 | ✅ 自带合成引擎 |
| MIDI 2.0 | ❌ | ✅ | ❌ |
| 学习曲线 | 低（文档多，JDK 官方） | 中 | 高（模块化合成概念） |

**决策：javax.sound.midi 为主，JSyn 作为可选扩展**

- `javax.sound.midi` 的 Sequencer 足以做 MIDI 文件播放
- `javax.sound.midi` 的 Synthesizer + Receiver 足以做实时按键触发
- Gervill 合成器内置在 OpenJDK 中，SoundFont 音色质量过关
- JSyn 只在需要"实时音频合成效果"（滤波/混响/glitch 音效）时引入

### 乐谱 / 文件格式

| 格式 | 支持方式 | 优先级 |
|------|---------|--------|
| **MIDI (.mid)** | javax.sound.midi 原生读取 | **v1 必须** |
| **MusicXML (.xml/.mxl)** | ProxyMusic (`org.audiveris:proxymusic`) 解析 | v2 考虑 |
| **ABC Notation** | 无成熟 Java 库，自行实现或跳过 | Out of Scope |

**关于"五线谱翻译成代码"：**
- MusicXML 是五线谱的 XML 表示，MuseScore/Sibelius 等打谱软件都能导出
- ProxyMusic 可以将 MusicXML 完整反序列化为 Java 对象（325 个 schema 类）
- MusicXML → MIDI 转换是可行的，但需要开发转换层（note pitches → MIDI note numbers）
- **结论：v1 只做 MIDI 文件导入。MusicXML → MIDI 转换放 v2。**

---

## 构建工具

**Gradle** — Radiance 和 Aurora 均提供 Gradle 插件（svg-transcoder-gradle-plugin），作者生态一致。

---

## 参考项目

| 项目 | 技术栈 | 参考价值 |
|------|--------|---------|
| **Melodigram** | Java Swing + MIDI | Synthesia-like 钢琴卷帘+下落音符，**直接可参考的架构** |
| **MelodyMatrix** | JavaFX + MIDI | 多视图 MIDI 可视化 |
| **midis2jam2** | Kotlin + jMonkeyEngine | 3D MIDI 可视化（非 Java，但概念可参考） |
| **Nyquist Piano_Roll.java** | Java Swing 钢琴卷帘组件 | 参考实现（不完整但概念清晰） |

---

## Stack Summary

```
Language:    Java 17
UI:          Radiance 8.5.0 (Swing + Theming + Animation)
MIDI:        javax.sound.midi (JDK built-in)
             + JSyn (optional, for real-time audio effects)
MusicXML:    ProxyMusic (v2)
Build:       Gradle 8.x
Icons:       Radiance SVG Transcoder (offline SVG → Java2D)
Theme:       Ephemeral NightShade (dark) — 赛博风基底
```
