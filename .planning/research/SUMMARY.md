# Research Summary: ReBeatBox

## Key Findings

### Stack
**Java 17 + Radiance 8.5.0 (Swing) + javax.sound.midi + Gradle**

kirill-grouchnikov 的三个仓库构成一个完整的设计/UI 生态：Ephemeral（设计系统）→ Radiance（Swing 实现）/ Aurora（Compose 实现）。对纯 Java 项目，Radiance 是唯一正确答案——它提供了完整的动画引擎、皮肤系统（含 Dark 变体，适合赛博风）、丰富的组件库，以及 SVG 图标离线转码工具。

MIDI 方面，JDK 内置的 `javax.sound.midi` 足以覆盖 MIDI 文件播放和实时触发两大核心场景，Gervill 合成器提供 SoundFont 级别音色。不需要引入外部依赖。

### Table Stakes
1. **MIDI 文件播放** — 加载/播放/暂停/停止/BPM/音量
2. **音符可视化** — 钢琴卷帘 + 下落音符动画
3. **实时交互** — 键盘映射 → MIDI note，可叠加在自动播放之上
4. **赛博/Glitch 视觉** — 暗色基底 + 霓虹色彩 + 动画反馈

### Differentiators
- 粒子爆发效果（音符触发时）
- Glitch 故障艺术过渡动画
- Drum Pad 布局 + 多键盘映射
- 高质量 SoundFont（FluidR3_GM）
- 自定义 SVG 赛博图标集

### Watch Out For
1. **MIDI 时钟同步** — 用微秒位置而非 tick 做动画同步
2. **Graphics2D 性能** — 离屏渲染 + 粒子数量限制 + viewport culling
3. **SoundFont 质量** — 内置高质量 SF2 文件，注意许可证
4. **过度设计** — v1 只做"播放+可视化+按键演奏"，别想太多
5. **键盘冲突** — 全局键盘拦截 + 演奏/非演奏模式分离
6. **线程模型** — MIDI 回调不在 EDT，必须 invokeLater
7. **皮肤一致性** — 所有颜色通过 Radiance ColorToken 取，不硬编码

### Best Reference Project
**Melodigram**（github.com/Tbence132545/Melodigram）— Java Swing + MIDI，实现 Synthesia 式的下落音符可视化，是 ReBeatBox 最接近的参考架构。

---

## Files
- `.planning/research/STACK.md` — 完整技术栈决策
- `.planning/research/FEATURES.md` — 功能分类和复杂度评估
- `.planning/research/ARCHITECTURE.md` — 系统架构和组件设计
- `.planning/research/PITFALLS.md` — 9 个常见陷阱及预防

---
*Research completed: 2026-04-27*
