# Pitfalls Research: ReBeatBox

## 桌面音乐应用常见陷阱及预防

---

## 1. MIDI 时钟同步问题

**陷阱：** Sequencer 的 tick 位置与动画帧不同步，导致音符下落与声音不同步。

**症状：**
- 听到声音了但音符还没到
- 音符已经落下去了但声音慢了半拍
- BPM 改变后可视化偏移

**预防策略：**
- 使用 `Sequencer.getMicrosecondPosition()` 而不是 `getTickPosition()` 做动画同步（微秒精度 > tick）
- 播放开始前调用 `Sequencer.setTickPosition(0)` 确保起始同步
- BPM 变化时重新计算所有音符的屏幕位置
- **Phase 2 处理**

---

## 2. Java MIDI 延迟不可控

**陷阱：** `javax.sound.midi` 的实际音频延迟取决于操作系统和音频驱动，Windows 上可能到 20-50ms。

**症状：**
- 键盘按下到听到声音有明显延迟
- 延迟在实时演奏时特别刺耳

**预防策略：**
- 在 MIDI 消息发送端做优化（使用 `Synthesizer` 直接发送而不是走 `Sequencer`）
- 延迟是可感知但可接受的——MIDI 协议本身延迟很低，瓶颈在 Java 音频层
- 如果延迟不可接受，考虑用 JSyn 替代 Gervill（JSyn 有自己的音频管线）
- **设定合理预期：本项目的实时演奏不是专业级延迟，是"能玩"级别的**
- **Phase 3 验证**

---

## 3. Graphics2D 性能陷阱

**陷阱：** 钢琴卷帘 + 下落音符 + 粒子效果在同一帧内用 Graphics2D 绘制，可能达不到 60fps。

**症状：**
- 复杂 MIDI 文件（多轨道、大量同时音符）时帧率暴跌
- 粒子数量多时卡顿
- 窗口 resize 时重绘慢

**预防策略：**
- 使用 `BufferedImage` 做离屏渲染（只重绘变化的部分）
- 粒子数量硬上限（最多 200 个同时活跃）
- 对屏幕外的音符不做渲染（viewport culling）
- 使用 `Graphics2D.setRenderingHint()` 关闭抗锯齿做性能/质量 tradeoff
- **Phase 2 和 Phase 4 持续关注**

---

## 4. SoundFont 音色质量问题

**陷阱：** Gervill 默认 SoundFont 音色一般，MIDI 文件播出来"像手机铃声"。

**症状：**
- 钢琴音色塑料感
- 鼓点不够有力
- 用户一听就觉得"廉价"

**预防策略：**
- 内置一个高质量 GM SoundFont（如 FluidR3_GM.sf2, 141MB 或 TimGM6mb.sf2, 6MB 轻量版）
- SoundFont 文件作为资源打包进 JAR
- 提供 SoundFont 切换选项（高级用户可以用自己的 SF2 文件）
- **Phase 1 就要确定默认 SoundFont**
- **注意 SoundFont 许可问题**：FluidR3 是 MIT 许可，可商用

---

## 5. 键盘映射冲突

**陷阱：** 键盘映射与操作系统快捷键/Swing 默认键盘行为冲突。

**症状：**
- 按下空格键触发按钮而不是播放音符
- Ctrl+C 被拦截无法触发 MIDI
- Tab 键在组件间切换而不是演奏

**预防策略：**
- 使用 `KeyboardFocusManager` 全局拦截键盘事件
- 演奏模式和非演奏模式切换（演奏模式下所有键盘输入 → MIDI）
- 避开系统组合键（Ctrl/Alt/Meta 开头的留给系统）
- 用功能键（F1-F12）做模式切换
- **Phase 3 实现**

---

## 6. 多轨道 MIDI 的 Track 混乱

**陷阱：** MIDI 文件可能包含多个 Track（每个 Track 不同乐器），直接按 Track 拆分显示会导致视觉混乱。

**症状：**
- 10 轨 MIDI 文件变成 10 行钢琴卷帘，屏幕塞不下
- Track 0 通常是 Tempo/Time Signature 元数据轨（没有音符）

**预防策略：**
- Track 0（元数据轨）跳过不显示
- 合并同类型乐器 Track（如所有弦乐合为一轨）
- 提供 Track 筛选/显示开关
- v1 只显示合并后的主视图，v2 再做多轨
- **Phase 2 简化处理**

---

## 7. Radiance 皮肤冲突

**陷阱：** Radiance 的皮肤系统接管了所有 Swing 组件的绘制，自定义 Canvas 绘制可能与皮肤风格冲突。

**症状：**
- 钢琴卷帘背景色与皮肤不一致
- 自定义绘制区域在皮肤切换后仍用旧配色

**预防策略：**
- 自定义 Canvas 使用 Radiance 皮肤系统的 ColorToken 统一取色
- 通过 `UIManager` 获取当前皮肤的颜色值
- 不硬编码任何颜色值——全部通过 ThemeManager 中转
- **Phase 1 就建立色彩系统**

---

## 8. MIDI 文件格式变体

**陷阱：** MIDI 文件有 Type 0（单轨）、Type 1（多轨）、Type 2（极少见），不同软件导出的 MIDI 可能有微妙差异。

**症状：**
- 某些 .mid 文件播放正常，某些完全无声
- 音符时长异常（有些音符一直不 NoteOff）
- Tempo 变化后错乱

**预防策略：**
- 测试多种来源的 MIDI 文件（DAW 导出、网上下载、MuseScore 导出）
- 对 NoteOn 没有对应 NoteOff 的情况做保护（超时自动 NoteOff）
- 使用 `Sequence.getDivisionType()` 处理不同 tick 分辨率
- **Phase 1 测试集**

---

## 9. 过度设计（本项目最大风险）

**陷阱：** 音乐软件很容易膨胀——加点效果器、加点编辑功能、加点导出格式……

**症状：**
- 三个月后还在写"音频引擎框架"
- 什么功能都有但什么都半成品
- 从未真正"玩"过自己写的软件

**预防策略：**
- **v1 只做"能打开 .mid 文件，能播放，能看到音符，能按键盘出声音"**
- 每个 Phase 结束时必须能用——不是框架，是产品
- "好玩"比"完整"重要
- `Out of Scope` 列表严格执行
- **All Phases**

---

## Phase Mapping

| Pitfall | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|---------|:-------:|:-------:|:-------:|:-------:|
| 1. MIDI 时钟同步 | — | ⚠️ 核心 | — | ⚠️ 验证 |
| 2. 延迟问题 | — | — | ⚠️ 核心 | — |
| 3. Graphics2D 性能 | — | ⚠️ 核心 | — | ⚠️ 验证 |
| 4. SoundFont 质量 | ⚠️ 核心 | — | — | — |
| 5. 键盘映射冲突 | — | — | ⚠️ 核心 | — |
| 6. 多轨道混乱 | — | ⚠️ 简化 | — | — |
| 7. 皮肤冲突 | ⚠️ 预防 | ⚠️ 预防 | ⚠️ 预防 | ⚠️ 预防 |
| 8. MIDI 格式变体 | ⚠️ 核心 | — | — | — |
| 9. 过度设计 | ⚠️ ALL | ⚠️ ALL | ⚠️ ALL | ⚠️ ALL |
