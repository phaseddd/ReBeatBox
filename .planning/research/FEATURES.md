# Features Research: ReBeatBox

## 领域：桌面音乐应用（Desktop Music Application）

研究范围涵盖：MIDI 播放器、数字音频工作站(DAW)简化版、鼓机/DJ 应用、音乐学习工具。

---

## Table Stakes（必须有的，用户期望）

### MIDI 文件播放
- **PLAY-01**: 加载标准 MIDI 文件（.mid）并播放
- **PLAY-02**: 播放/暂停/停止/重新开始
- **PLAY-03**: 速度控制（BPM 调节）
- **PLAY-04**: 音量控制
- **PLAY-05**: 进度条（跳转到任意位置）

### 音符可视化
- **VIS-01**: 钢琴卷帘视图（piano roll）— 横轴时间、纵轴音高
- **VIS-02**: 下落音符动画（Synthesia-like falling notes）
- **VIS-03**: 当前播放位置指示器

### 实时交互
- **LIVE-01**: 键盘映射到音符/鼓点（计算机键盘 → MIDI note）
- **LIVE-02**: 按键按下时触发声音，松开停止
- **LIVE-03**: 实时交互与背景播放共存（叠加模式）

### 视觉主题
- **UI-01**: 暗色主题（Dark mode）
- **UI-02**: 霓虹色系配色（neon color palette）
- **UI-03**: 按钮/控件有悬停和点击动画反馈

---

## Differentiators（竞争优势，做出彩的）

### 赛博/Glitch 视觉
- **GLITCH-01**: 粒子效果（音符触发时粒子爆发）
- **GLITCH-02**: Glitch 故障艺术过渡动画（窗口切换/加载时）
- **GLITCH-03**: 音符轨道使用霓虹发光线条
- **GLITCH-04**: 自定义赛博风 SVG 图标集

### 交互深度
- **DEEP-01**: Drum Pad 布局（网格按钮，每个映射不同鼓点音色）
- **DEEP-02**: 实时音效旋钮（滤波/混响/延迟，用 Radiance 组件模拟旋钮 UI）
- **DEEP-03**: Loop 模式（循环播放选中段落）
- **DEEP-04**: 多轨道视图（不同乐器分轨显示）

### 内容生态
- **CONT-01**: 内置示例曲谱（开箱即用，降低上手门槛）
- **CONT-02**: 拖拽导入 MIDI 文件
- **CONT-03**: 最近打开文件列表

---

## Anti-Features（刻意不做的）

| 功能 | 理由 |
|------|------|
| **MusicXML 导入** (v1) | ProxyMusic 可用但转换层开发量大，先聚焦 MIDI |
| **音频采样编辑 (WAV/MP3)** | 这是 DAW 领域，复杂度极高，偏离 MIDI 核心 |
| **VST 插件** | 需要原生音频管线，Java 不适合 |
| **MIDI 设备直连** | `javax.sound.midi` 理论上支持但平台兼容性差，v1 先不碰 |
| **录音** | 需要音频输入管线，v1 不做 |
| **AI 作曲/生成** | 偏离核心体验，后期可探索 |
| **乐谱渲染（五线谱绘制）** | 难度极高（排版引擎级别），MIDI→五线谱是逆向工程 |

---

## Feature Complexity Heatmap

| 功能 | 复杂度 | MIDI依赖 | UI依赖 | 风险 |
|------|--------|---------|--------|------|
| MIDI 文件加载+播放 | 🟢 低 | Sequencer | 进度条 | 低 |
| 播放控制（播放/暂停/停止）| 🟢 低 | Sequencer | 按钮 | 低 |
| BPM/音量控制 | 🟢 低 | Sequencer | 滑块 | 低 |
| 键盘映射 → MIDI note | 🟢 低 | Synthesizer | 键盘监听 | 低 |
| 钢琴卷帘视图 | 🟡 中 | Sequencer+MidiEvent | Graphics2D 自定义绘制 | 中 |
| 下落音符动画 | 🟡 中 | Sequencer + 时钟同步 | radiance-animation | 中 |
| Drum Pad 网格 | 🟢 低 | Synthesizer | radiance-component | 低 |
| 粒子效果 | 🟡 中 | 无 | Graphics2D + animation | 中 |
| Glitch 过渡动画 | 🟡 中 | 无 | radiance-animation | 中 |
| 拖拽导入 | 🟢 低 | 文件系统 | Swing DnD | 低 |
| 多轨道视图 | 🔴 高 | 多 Track 解析 | 自定义布局 | 高 |
| 实时音效旋钮 | 🔴 高 | 需要 JSyn | 自定义旋钮组件 | 高 |

---

## v1 Feature Set（建议）

基于复杂度评估和"单人项目"约束，v1 聚焦：

### Phase 1: 核心播放引擎
- MIDI 文件加载、播放、暂停、停止
- 速度/音量控制

### Phase 2: 音符可视化
- 钢琴卷帘 + 下落音符
- 播放位置指示

### Phase 3: 实时演奏
- 键盘映射 + 实时 MIDI 触发
- Drum Pad 基础版

### Phase 4: 视觉打磨
- 赛博/Glitch 主题全面应用
- 粒子效果、霓虹动画
- SVG 图标
