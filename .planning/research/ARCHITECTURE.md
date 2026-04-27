# Architecture Research: ReBeatBox

## 桌面音乐应用的典型架构

### 标准分层

```
┌─────────────────────────────────────────┐
│           UI Layer (View)               │
│  Radiance Components + Custom Canvas    │
├─────────────────────────────────────────┤
│       Animation / Visual Layer          │
│  radiance-animation + Graphics2D        │
├─────────────────────────────────────────┤
│         Music Engine (Domain)           │
│    MIDI Player / Sequencer / Receiver   │
├─────────────────────────────────────────┤
│       MIDI Backend (Infrastructure)     │
│     javax.sound.midi + Gervill Synth    │
└─────────────────────────────────────────┘
```

### 组件边界和数据流

```
┌──────────┐    ┌──────────────┐    ┌──────────────┐
│ File I/O │───▶│ MIDI Parser  │───▶│  Sequencer   │
│ (.mid)   │    │ (Sequence)   │    │  (播放引擎)   │
└──────────┘    └──────────────┘    └──────┬───────┘
                                           │
                    ┌──────────────────────┤
                    │                      │
                    ▼                      ▼
            ┌──────────────┐    ┌──────────────────┐
            │ Note Tracker  │    │  Synthesizer      │
            │ (时间线状态)   │    │  (实时声音合成)    │
            └──────┬───────┘    └────────┬─────────┘
                   │                     │
                   ▼                     ▼
            ┌──────────────┐    ┌──────────────────┐
            │ Piano Roll   │    │  Audio Output     │
            │ Renderer     │    │  (Gervill → 声卡) │
            └──────────────┘    └──────────────────┘
                   │
                   ▼
            ┌──────────────┐
            │ Particle FX  │
            │ Renderer     │
            └──────────────┘
```

---

## ReBeatBox 建议架构

### 核心领域模块

#### 1. MIDI Engine (`engine` package)

```
engine/
├── MidiFileLoader.java       // MIDI 文件读取 → Sequence
├── PlaybackController.java   // 播放/暂停/停止/BPM
├── NoteEventBus.java         // 音符事件分发（Observer pattern）
├── RealtimeReceiver.java     // 实时 MIDI 消息接收和发送
└── SoundFontManager.java     // SoundFont 加载管理
```

**关键设计决策：**
- `javax.sound.midi.Sequencer` 负责 MIDI 文件播放
- `javax.sound.midi.Synthesizer` 负责实时声音合成
- 两个可以共用同一个 Synthesizer 实例（Sequencer 输出连接到 Synth）
- NoteEventBus 作为 Observer：Sequencer 每 tick 检查当前音符状态 → 通知 UI 更新

#### 2. Visualization (`visual` package)

```
visual/
├── PianoRollPanel.java       // 钢琴卷帘主视图（JPanel 子类）
├── FallingNoteRenderer.java  // 下落音符渲染
├── ParticleSystem.java       // 粒子效果引擎
├── GlitchTransition.java     // Glitch 切换动画
├── NoteColorMapper.java      // 音符→霓虹颜色映射
└── TrackHighlighter.java     // 当前播放位置高亮
```

**关键设计决策：**
- 钢琴卷帘和下落音符使用 `Graphics2D` 自定义绘制（不是 Swing 组件组合）
- `radiance-animation` 的 `Timeline` API 驱动动画帧
- 粒子系统自己实现（轻量级），60fps 目标
- NoteColorMapper：不同音高映射不同霓虹色（C=红, D=橙, E=黄, F=绿, G=青, A=蓝, B=紫）

#### 3. Live Performance (`live` package)

```
live/
├── KeyboardMapper.java       // 计算机键盘 → MIDI note 映射
├── DrumPadGrid.java          // Drum Pad 网格组件
├── PadButton.java            // 单个 Pad 按钮（Radiance CommandButton 子类）
└── KeyFeedbackAnimator.java  // 按键反馈动画
```

**关键设计决策：**
- 键盘映射：QWERTY 两排键 → 一个八度 + 鼓点
- DrumPadGrid 使用 Radiance `CommandButton` + GridLayout
- 同时按多个键 = 同时触发多个 MIDI note（Synthesizer 支持复音）

#### 4. UI Shell (`ui` package)

```
ui/
├── ReBeatBoxWindow.java      // 主窗口（JFrame + Radiance 皮肤）
├── ControlBar.java           // 顶部控制栏（播放/暂停/BPM/音量）
├── SidePanel.java            // 侧边栏（Drum Pad / 曲目列表切换）
├── ThemeManager.java         // 赛博主题管理
└── SvgIconLoader.java        // SVG 图标加载
```

**关键设计决策：**
- 主窗口使用 Radiance 的皮肤系统（NightShade dark）
- 布局：控制栏（顶）+ 钢琴卷帘（中）+ Drum Pad（侧/底）
- SVG 图标通过 radiance-svg-transcoder 预编译为 Java2D 类

---

## 数据流详解

### 播放 MIDI 文件流程

```
1. 用户拖拽/打开 .mid 文件
2. MidiFileLoader → MidiSystem.getSequence() → Sequence 对象
3. Sequence 拆分为 Track[] 
4. Sequencer.setSequence(sequence) → sequencer.start()
5. 播放循环（每 ~10ms）：
   a. Sequencer.getTickPosition() → 当前 tick
   b. 遍历所有 Track，找出当前 tick 活跃的 NoteOn（未 NoteOff 的）
   c. NoteEventBus.fire(activeNotes) → PianoRollPanel + ParticleSystem
6. PianoRollPanel 更新下落音符位置
7. ParticleSystem 在 NoteOn 事件时生成粒子爆发
```

### 实时演奏流程

```
1. 用户按下键盘 'A' 键
2. KeyboardMapper.map(KeyEvent) → MIDI note number (e.g., 60 = C4)
3. RealtimeReceiver.send(NoteOn, note=60, velocity=100)
4. Synthesizer 合成声音 → Gervill → 声卡输出（延迟 <10ms）
5. KeyFeedbackAnimator 触发按钮发光动画 + 粒子爆发
6. 用户松开按键 → NoteOff → 声音停止
```

---

## 线程模型

```
┌────────────────┐
│  EDT (UI线程)   │ ← 所有 Swing 组件操作必须在此线程
│  60fps 渲染     │
└───────┬────────┘
        │
┌───────┴────────┐
│  MIDI 线程      │ ← Sequencer 内部线程，发送 MetaEventListener 回调
│  (JDK内部)      │    回调中使用 SwingUtilities.invokeLater() 回 EDT
└───────┬────────┘
        │
┌───────┴────────┐
│  动画 Timer     │ ← radiance-animation Timeline，60fps
│  (EDT 友好)     │    已经在 EDT 上执行
└────────────────┘
```

**线程安全关键点：**
- MIDI 回调（`MetaEventListener.meta()`）不在 EDT 上，必须 `SwingUtilities.invokeLater()`
- 粒子系统状态更新在动画回调中（EDT），渲染在 paintComponent 中（EDT）
- 无需额外的同步锁（单 EDT 模型）

---

## 建议 Build Order（依赖关系）

```
Phase 1: MIDI Engine（无 UI 依赖）
    ↓
Phase 2: Piano Roll Visualization（依赖 Phase 1 的 NoteEventBus）
    ↓
Phase 3: Live Performance（依赖 Phase 1 的 RealtimeReceiver）
    ↓
Phase 4: Visual Polish（依赖 Phase 2+3 的所有 UI 组件）
```

Phase 2 和 Phase 3 互不依赖，可以并行开发。
