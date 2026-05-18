# ReBeatBox

## What This Is

ReBeatBox 是一个赛博/Glitch 风格的 Java 桌面音乐应用。用户可以导入 MIDI 文件进行自动演奏、用键盘实时触发音效和节奏（DJ/鼓机模式），并在屏幕上看到音符跳动的可视化效果。名字致敬 Head First Java 最后一章的 BeatBox 项目。

## Core Value

打开应用，音乐就在指尖——既能听着音符自动流淌，也能亲手敲出节奏，全程有赛博朋克式的视觉反馈。

## Requirements

### Validated

- [x] 用户能导入 MIDI 文件并自动播放 — Phase 1 (PLAY-01~05, CONT-02)
- [x] 用户能用键盘/鼠标实时触发节奏和音效 — Phase 3 (LIVE-01~04)
- [x] 自动演奏时有音符跳动可视化（钢琴卷帘/瀑布流）— Phase 2 (VIS-01~03)
- [x] 实时演奏时有点击反馈动画 — Phase 3 live highlight + Phase 4 particles
- [x] 赛博/Glitch 风格 UI 主题（暗色 + 霓虹 + 粒子效果）— Phase 1 dark theme + Phase 4 full polish

### Active

- [ ] 应用内置示例曲谱/节奏 pattern，开箱即玩 — Phase 5 (CONT-01, CONT-03)

### Out of Scope

- 移动端 / Web 端 — Java 桌面端 only
- 多人协作 / 在线功能 — 纯本地应用
- 真实音频采样编辑（WAV/MP3 波形编辑）— MIDI 为主，不碰 DAW 领域
- 多语言国际化 — 个人项目
- VST 插件支持 — 复杂度太高

## Context

- **灵感来源**: Head First Java 最后一章的 BeatBox 项目（MIDI 合成器 + 鼓机 pattern）
- **UI 框架**: kirill-grouchnikov 的 aurora（UI 组件框架）、radiance（组件库集合）、ephemeral（动画/过渡库）——这三个项目证明了 Java 桌面端可以做出极高质量的 UI
- **技术背景**: Java 在桌面端已经边缘化，但 `javax.sound.midi` API 在 MIDI 处理方面仍然成熟可靠。本项目不回避这个现实——用 Java 是情怀选择，不是技术最优解
- **用户**: 开发者自己，个人玩具项目，不需要考虑多用户、权限、国际化
- **音乐领域**: 用户喜欢听音乐但对音乐编程（MIDI、乐谱、音频处理）无深入了解——技术方案需要调研后推荐

## Constraints

- **语言**: Java（必须，情怀原因）
- **UI 框架**: kirill-grouchnikov 系列（aurora + radiance + ephemeral），不引入 WebView/Electron 方案
- **平台**: Windows 为主（开发环境），跨平台是 Java 的自然红利，不做额外承诺
- **音乐格式**: 以 MIDI 为底层协议，支持导入标准 MIDI 文件（.mid），具体乐谱解析方案待调研
- **复杂度**: 一人项目，控制 scope，不搞微服务、数据库、网络通信
- **视觉优先级**: UI 美观度是核心指标，赛博/Glitch 风格必须统一贯穿

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 路线 C（混合模式）| 自动演奏保证"好看"，实时交互保证"好玩"，共享 MIDI 引擎不增加架构复杂度 | ✅ Phase 1-3 实现：Sequencer + RealtimeReceiver 共享 Synthesizer |
| 赛博/Glitch 视觉风格 | 用户明确偏好，aurora 框架风格匹配度高 | ✅ Phase 1 NightShade + Phase 4 ThemeManager/ParticleSystem/GlitchTransition |
| MIDI 作为音乐数据协议 | Java 原生支持，标准化，文件小，适合实时交互 | ✅ javax.sound.midi + Gervill + FluidR3_GM.sf2 |
| 不参照原书 BeatBox 代码 | 原书是教学级代码，架构和 UI 都过时了 | ✅ 全量重写，Radiance 组件库 + 自定义 paint |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-18 — Phase 1-4 validated, Key Decisions documented*
