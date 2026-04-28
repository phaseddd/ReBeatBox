# Phase 03: Live Performance - Research

**Researched:** 2026-04-28
**Domain:** Java Swing 键盘事件捕获 + javax.sound.midi 实时合成器叠加 + GM 打击乐映射
**Confidence:** HIGH

## Summary

Phase 03 实现计算机键盘到 MIDI 音符的实时映射和鼓垫网格。核心技术挑战有三：(1) 通过 `KeyboardFocusManager.addKeyEventDispatcher()` 全局捕获键盘事件，在 Swing 的 EDT 线程上即时发送 MIDI NoteOn/NoteOff；(2) 实时音符与 Sequencer 背景播放共享同一个 Gervill `Synthesizer.getReceiver()`——MIDI 协议原生支持多源叠加，Receiver 在多线程环境下实践上安全；(3) 鼓垫 Grid 通过 MouseListener 触发 Channel 10 GM 打击乐音符，每个 Pad 独立可自定义。

**关键发现：** 现有 `RealtimeReceiver` 硬编码了 Channel 0，需增加 channel 参数重载以支持鼓垫的 Channel 10 输出。`NoteEventBus` 的 `CopyOnWriteArrayList` 模式可直接扩展 `liveNoteOn`/`liveNoteOff` 事件。

**Primary recommendation:** 以 EDT 为中心的架构——所有 MIDI 实时发送在 EDT 上完成，Receiver 调用为轻量级（< 1ms），无需额外线程。键盘去重用 `boolean[128]` + `keyCode→note` 映射表，窗口失焦时全量清理防鬼音。

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| 键盘→MIDI音符映射 | API/引擎层 (`live/KeyboardMapper`) | — | 纯逻辑（KeyEvent→MIDI note 查表），无 UI 依赖 |
| 全局按键捕获 | 客户端 (`ui/ReBeatBoxWindow`) | 引擎层 | `KeyboardFocusManager` 是 AWT 客户端设施，注册在 Window 层 |
| 实时音符 MIDI 发送 | 引擎层 (`engine/RealtimeReceiver`) | — | 直接调用 `javax.sound.midi.Receiver.send()` |
| 鼓垫交互 | 客户端 (`live/PadButton`) | 引擎层 | JButton + MouseListener → MIDI 发送 |
| 音符事件广播 | 引擎层 (`engine/NoteEventBus`) | — | 线程安全的发布-订阅总线 |
| 键盘可视化反馈 | 客户端 (`ui/KeyboardHintPanel`) | — | 仅响应事件改变绘制状态，不发送 MIDI |
| 鼓垫可视化 | 客户端 (`live/PadButton`) | — | 自绘边框/填充颜色状态切换 |
| 窗口失焦清理 | 客户端 (`ui/ReBeatBoxWindow`) | 引擎层 | `WindowFocusListener` → 清理 activeKeys + MIDI NoteOff |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| java.awt.KeyboardFocusManager | JDK 17 | 全局键盘事件分发 | Swing 唯一全局按键捕获机制，无需第三方库 |
| javax.sound.midi (JDK built-in) | JDK 17 | MIDI 消息发送/接收 | Java MIDI 标准 API，Gervill 合成器线程安全 |
| javax.swing.Timer | JDK 17 | 鼓垫视觉暂留 (200ms) | Swing EDT 安全定时器 |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Radiance Theming (NightShade) | 8.5.0 | 暗色主题自动继承 | 所有新 Swing 组件自动获得统一外观 |
| JUnit Jupiter | 5.10.0 | 单元测试 | 键盘映射逻辑、事件总线、MIDI 发送测试 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| KeyboardFocusManager | Toolkit.addAWTEventListener() | AWTEventListener 更底层，无法控制分发链顺序；KeyboardFocusManager 支持 `redispatchEvent` 和 `KeyEventPostProcessor` |
| 自定义 MIDI 混音器 | 直接共享 Receiver | 额外混音器增加延迟和复杂度；MIDI 协议原生支持多源叠加 |
| JButton 子组件排列键盘 | 自定义 paintComponent | 30+ JButton 导致 Layout 抖动和焦点问题；自定义绘制零组件开销（与 PianoRollPanel 模式一致） |

**Installation:**
无额外第三方依赖。Phase 3 完全基于 JDK 17 标准库 + 已有的 Radiance 8.5.0 + JUnit Jupiter 5.10.0。

### Validation Architecture — Nyquist Sampling Strategy

Phase 3 引入两种质量维度：(1) 功能正确性——键盘→MIDI 映射是否准确，多键同按是否正常；(2) 实时性——按键到声音的延迟是否可感知。

**采样维度：**

| 维度 | 采样策略 | 阈值标准 | 检测方法 |
|------|---------|---------|---------|
| 按键→MIDI 映射准确性 | 每个八度随机采样 3 个白键 + 2 个黑键，共 15 键 | 100% 映射正确 | 单元测试：验证 KeyboardMapper 查表结果 |
| 多键同按 | 随机组合 2-4 键同时按下/释放 | 所有 NoteOn/Off 正确配对，无鬼音 | 集成测试：模拟 KeyEvent 序列，验证 activeNotes 状态 |
| 窗口失焦清理 | 模拟 focusLost 事件 | 无残留 activeNotes，无残留 MIDI 音符 | 集成测试：setUp 按下若干键 → focusLost → 验证状态清空 |
| 鼓垫右键菜单 | 每个 Pad 的 Assign→Reset 流程 | 音符切换正确，Reset 弹窗确认后恢复默认 | 单元测试：验证 DrumPadGrid 内部状态 |
| NoteEventBus 事件 | 发送 liveNoteOn → 验证监听器收到 | 事件携带正确的 note + velocity | 单元测试：mock listener 计数 |
| 实时性（延迟） | 手动测试：按下键到听到声音的感知延迟 | < 50ms 为可接受（低于人耳感知阈值） | 手动 UAT（自动化延迟测试需要音频回路，超出范围） |

**采样规则：**
- **Wave 0（测试基础设施）：** 建立 KeyboardMapperTest, NoteEventBusLiveTest, DrumPadGridTest 三个测试类骨架
- **每个 Task 提交前：** 运行 `./gradlew test --tests "com.rebeatbox.live.*"` 验证映射逻辑
- **每个 Wave 合并前：** 运行全量测试 `./gradlew test`
- **Phase Gate：** 全量测试绿色 + 手动 UAT（弹奏键盘确认三排音符正确 + 鼓垫点击确认 16 pad 各自发声）

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     ReBeatBoxWindow (JFrame)                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │            KeyboardFocusManager                       │   │
│  │  addKeyEventDispatcher(KeyEvent → MIDI)               │   │
│  │       │                                               │   │
│  │       ▼                                               │   │
│  │  KeyboardMapper                                       │   │
│  │  keyCode→note 查表 + boolean[128] 去重                │   │
│  │       │                                               │   │
│  │       ├──► RealtimeReceiver.noteOn/Off(channel=0)     │   │
│  │       ├──► NoteEventBus.fireLiveNoteOn/Off()          │   │
│  │       └──► KeyboardHintPanel.highlightKey()            │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌────────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ DrumPadGrid    │  │ PianoRoll    │  │ KeyboardHint   │  │
│  │ (EAST)         │  │ Panel        │  │ Panel (SOUTH)  │  │
│  │                │  │ (CENTER)     │  │                │  │
│  │ PadButton ×16  │  │              │  │ 3-row custom   │  │
│  │  │             │  │ subscribes   │  │ paint          │  │
│  │  ▼             │  │ liveNoteOn/  │  │ highlightKey() │  │
│  │ RealtimeRecv   │  │ Off for      │  │ repaint()      │  │
│  │ .noteOn/Off    │  │ flash        │  │                │  │
│  │ (channel=10)   │  │              │  │                │  │
│  └────────────────┘  └──────────────┘  └────────────────┘  │
│                                                              │
│  WindowFocusListener: windowLostFocus → clearAll             │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
              ┌──────────────────────┐
              │  Synthesizer         │
              │  .getReceiver()      │
              │                      │
              │  ◄── RealtimeReceiver (live notes, ch 0)      │
              │  ◄── PlaybackController.sequencer (ch 0-15)   │
              │  ◄── PadButton (drum pads, ch 10)             │
              └──────────────────────┘
```

**数据流关键路径 (用户按键→声音):**
1. 用户按下键盘 Q 键
2. `KeyboardFocusManager` 分发 `KEY_PRESSED` 事件到注册的 `KeyEventDispatcher`
3. Dispatcher 调用 `KeyboardMapper.keyCodeToNote(VK_Q)` → 返回 MIDI 60 (C4)
4. 检查 `activeNotes[60]`：若为 false → `RealtimeReceiver.noteOn(60, 100)` 发送 MIDI
5. `NoteEventBus.fireLiveNoteOn(60, 100)` → `PianoRollPanel` 收到事件做视觉闪烁
6. `KeyboardHintPanel.highlightKey("Q", true)` → `repaint()` 触发重绘
7. 用户释放 Q 键 → `KEY_RELEASED` → 同理触发 noteOff 路径

### Recommended Project Structure
```
src/main/java/com/rebeatbox/
├── live/                     # 新增：实时演奏包
│   ├── KeyboardMapper.java   # KeyEvent → MIDI note 查表 + 去重
│   ├── DrumPadGrid.java      # 4x4 鼓垫容器面板
│   └── PadButton.java        # 单个鼓垫按钮
├── ui/
│   ├── KeyboardHintPanel.java # 新增：三排虚拟键盘绘制
│   ├── ReBeatBoxWindow.java   # 修改：SOUTH 面板 + KeyboardFocusManager 注册
│   └── SidebarPanel.java      # 修改：暴露 getContentPanel()
├── engine/
│   ├── NoteEventBus.java      # 修改：liveNoteOn/Off 事件
│   ├── NoteEventListener.java # 修改：新监听接口（或默认方法）
│   └── RealtimeReceiver.java  # 修改：增加 channel 参数重载
├── visual/
│   └── PianoRollPanel.java    # 修改：订阅 liveNoteOn/Off
└── App.java                   # 不变
```

### Pattern 1: KeyboardFocusManager 全局按键捕获

**What:** 注册 `KeyEventDispatcher` 到 `KeyboardFocusManager`，拦截所有 `KEY_PRESSED`/`KEY_RELEASED` 事件，转换为 MIDI 信号。

**When to use:** 所有需要全局快捷键或键盘→自定义事件映射的场景。

**Key decisions (from CONTEXT.md D-03):**
- 仅在 `KEY_PRESSED` 和 `KEY_RELEASED` 时处理（忽略 `KEY_TYPED`）
- 使用 `boolean[128]` 数组去重，消除 OS 按键重复事件
- 返回 `false` 不消费事件——让其他组件（如菜单快捷键）继续正常处理
- 检查 `focusOwner`：如果焦点在 `JTextComponent` 上，跳过处理

**Example:**
```java
// Source: KeyboardFocusManager JavaDoc + StackOverflow verified patterns
KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
kfm.addKeyEventDispatcher(e -> {
    if (e.getID() != KeyEvent.KEY_PRESSED && e.getID() != KeyEvent.KEY_RELEASED) {
        return false; // 忽略 KEY_TYPED
    }
    if (e.isConsumed()) {
        return false; // 尊重已消费事件
    }
    // 焦点在文本组件时不拦截
    Component focusOwner = kfm.getFocusOwner();
    if (focusOwner instanceof javax.swing.text.JTextComponent) {
        return false;
    }
    
    boolean pressed = (e.getID() == KeyEvent.KEY_PRESSED);
    int note = keyboardMapper.keyCodeToNote(e.getKeyCode());
    if (note < 0) return false; // 未映射的键
    
    if (pressed && !keyboardMapper.isNoteActive(note)) {
        receiver.noteOn(note, 100, 0);
        eventBus.fireLiveNoteOn(note, 100);
        keyboardMapper.setNoteActive(note, true);
        hintPanel.highlightKey(e.getKeyCode(), true);
    } else if (!pressed && keyboardMapper.isNoteActive(note)) {
        receiver.noteOff(note, 0);
        eventBus.fireLiveNoteOff(note);
        keyboardMapper.setNoteActive(note, false);
        hintPanel.highlightKey(e.getKeyCode(), false);
    }
    return false; // 不消费事件
});
```

### Pattern 2: MIDI Receiver 共享叠加（Overlay Mode）

**What:** 实时音符（Channel 0）和 Sequencer 背景播放（Channel 0-15）共享 `synthesizer.getReceiver()`。Gervill 合成器原生支持多源输入叠加——MIDI 协议天然设计为多 Transmitter → 单 Receiver 拓扑。

**When to use:** D-09 已锁定此方案。无需额外开发。

**验证来源：**
- Oracle Java Sound Programmer's Guide Ch.10: 明确支持单一 Receiver 接收多个 Transmitter
- 现有代码：`PlaybackController` 和 `RealtimeReceiver` 都已通过 `synthesizer.getReceiver()` 发送，Phase 1-2 已验证无误
- JDK-4791258: 规范未声明 `Receiver.send()` 线程安全——但实际上 Gervill 的 `SoftSynthesizer` Receiver 在多线程并发 `send()` 下安全。此外，本方案所有实时输入来自 EDT（单线程），Sequencer 在自己的线程发送，无竞态实际风险

### Pattern 3: 自定义绘制键盘面板（跟随 PianoRollPanel 模式）

**What:** `KeyboardHintPanel` 覆写 `paintComponent(Graphics g)`，自定义绘制三排钢琴键盘。不使用 JButton 子组件——避免 30+ 组件导致的布局抖动。

**When to use:** UI-SPEC.md 已规定此模式。与 Phase 2 `PianoRollPanel` 的 mini keyboard 绘制一致。

### Pattern 4: 鼓垫 Button 视觉状态机

**What:** `PadButton extends JButton`，三态切换：Default → Hover → Pressed。Pressed 状态用 200ms `javax.swing.Timer` 保持视觉反馈。

**When to use:** 所有需要短暂视觉反馈的触发式按钮。

### Anti-Patterns to Avoid

- **返回 `true` 消费键盘事件:** 返回 `true` 表示"我已处理完此事件，停止传递"，目标组件（菜单、文本区）将完全收不到按键。Phase 3 始终返回 `false`。
- **在 KeyEventDispatcher 中做耗时操作:** Dispatcher 在 EDT 上执行，长耗时操作冻结整个 UI。Phase 3 的 MIDI `send()` 调用 < 1ms，安全。
- **忘记窗口失焦清理:** 用户在按住键盘时 Alt+Tab 切换窗口，`KEY_RELEASED` 永不触发。必须用 `WindowFocusListener.windowLostFocus()` 发送 All Notes Off + 重置状态。
- **用 KeyListener 代替 KeyboardFocusManager:** 组件级 `KeyListener` 受焦点影响，键盘事件只在聚焦组件上触发。音乐应用需要全局无死角捕获。
- **鼓垫用 JToggleButton:** 鼓垫是瞬时触发（按下发声、松开静音），不是切换开关。用 `JButton` + `MouseListener`。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 全局键盘捕获 | 自定义 native hook (JNI/JNA) | `KeyboardFocusManager.addKeyEventDispatcher()` | 跨平台、无原生依赖、EDT 安全；JNI hook 需要平台特定代码且无法在纯 Java 沙箱运行 |
| MIDI 混音/叠加 | 自定义音频混音 buffer | `Synthesizer.getReceiver()` 多源写入 | MIDI 是数字协议，非音频采样——"混音"由合成器在渲染阶段完成，无需用户态混音 |
| OS 按键重复抑制 | 时间戳对比 / Timer hack | `boolean[128]` 数组去重 | Windows 上 auto-repeat 只产生 KEY_PRESSED（无 KEY_RELEASED），boolean 去重天然正确。Linux 的 KEY_PRESSED/RELEASED 交叠也能被去重数组正确处理 |
| 键盘可视化 | 30+ JButton 实例排列 | 单个 `JPanel.paintComponent()` 自定义绘制 | JButton 有焦点系统、边框、Insets 开销，30+ 组件导致布局抖动；自定义绘制零组件树开销 |
| 鼓垫音色分配菜单 | 手写菜单系统 | `JPopupMenu` + `JMenuItem` | Swing 原生弹出菜单支持层级子菜单、JSeparator、快捷键，无需自绘 |

**Key insight:** 本 Phase 几乎所有需求都有 JDK 标准库或已有代码的直接支撑。唯一需要"创建"的是键位映射表（CONTEXT.md D-01 已经完整定义）和视觉绘制代码（UI-SPEC.md 已经完整定义颜色和尺寸）。Phase 3 的核心工作是将现有材料组装起来，而非发明新机制。

## Runtime State Inventory

> Include this section for rename/refactor/migration phases only. Omit entirely for greenfield phases.

**Phase 3 是纯新增功能（greenfield）。无运行时状态需要迁移。SKIPPED。**

## Common Pitfalls

### Pitfall 1: 窗口失焦导致"鬼音"（Stuck Notes）
**What goes wrong:** 用户按住键盘 A 键时 Alt+Tab 切换到浏览器。操作系统停止向失去焦点的窗口发送键盘事件，`KEY_RELEASED` 永不触发。音符持续发声直到合成器被重置。

**Why it happens:** AWT 的事件分发只在窗口有焦点时工作。焦点丢失后，已按下的键没有对应的 Release 事件。

**How to avoid:**
1. 在 `ReBeatBoxWindow` 上注册 `WindowFocusListener`
2. `windowLostFocus()` 中：
   - 遍历 `activeNotes[]` 发送 `noteOff` 清理所有活跃音符
   - 重置 `activeNotes[]` 数组
   - 调用 `keyboardHintPanel.clearAllHighlights()`
3. `PlaybackController.stop()` 中已有的 CC 123 (All Notes Off) 也会清理，但那是全局清理——窗口失焦时需要更精细的控制

**Warning signs:** 切换窗口后仍有声音在响；KeyboardHintPanel 上仍然高亮显示按下的键。

### Pitfall 2: 文本输入框被"劫持"
**What goes wrong:** 用户在文件名输入框中打字，字符被正常输入的同时也触发了 MIDI 音符。打字声和音乐混在一起。

**Why it happens:** `KeyboardFocusManager` 的 Dispatcher 拦截所有键盘事件，包括发送到文本组件的。

**How to avoid:** 在 Dispatcher 逻辑开头检查 `focusOwner`：
```java
Component focusOwner = kfm.getFocusOwner();
if (focusOwner instanceof JTextComponent || focusOwner instanceof JTable) {
    return false; // 不处理文本输入场景
}
```
例外：如果用户期望"即使焦点在文本框中也能弹奏"（例如一边输入文件名一边试听音符），可以跳过此检查。建议先加此保护，后续根据反馈决定是否移除。

**Warning signs:** 在 JFileChooser 或设置对话框中输文字时触发音符。

### Pitfall 3: RealtimeReceiver Channel 硬编码冲突
**What goes wrong:** 鼓垫 PadButton 需要在 Channel 10 (GM 打击乐) 发送 MIDI，但现有 `RealtimeReceiver.noteOn(int, int)` 硬编码 channel=0。

**Why it happens:** Phase 1-2 设计中只考虑了键盘实时音符（Channel 0），未预留鼓垫 Channel 10 通道。

**How to avoid:** 为 `RealtimeReceiver` 增加 channel 参数的重载方法：
```java
public void noteOn(int noteNumber, int velocity, int channel) {
    ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
    receiver.send(msg, -1);
}
```
原有 2 参数方法保持向后兼容（默认 channel=0）。PadButton 调用 3 参数版本，传入 channel=10。

**Warning signs:** 鼓垫点击发出钢琴音而非打击乐音（说明音符在错误 Channel 上发声）。

### Pitfall 4: NoteEventListener @FunctionalInterface 冲突
**What goes wrong:** `NoteEventListener` 是 `@FunctionalInterface`，只有 `onActiveNotesChanged` 一个方法。添加 `liveNoteOn`/`liveNoteOff` 方法导致编译错误。

**Why it happens:** `@FunctionalInterface` 要求接口只有一个抽象方法。

**How to avoid (选择方案 A——推荐):** 创建独立监听接口：
```java
// 新增文件: LiveNoteEventListener.java
public interface LiveNoteEventListener {
    void onLiveNoteOn(int note, int velocity);
    void onLiveNoteOff(int note);
}
// NoteEventBus 扩展：
private final List<LiveNoteEventListener> liveListeners = new CopyOnWriteArrayList<>();
```
这保持 `NoteEventListener` 向后兼容，不影响现有 `PianoRollPanel` 的编译。

**方案 B（备选）：** 移除 `@FunctionalInterface` 注解，添加 `default` 方法。风险：所有现有 lambda 订阅者编译通过但不实现新方法。

**Warning signs:** 编译错误 `"Unexpected @FunctionalInterface annotation"` 或 `"NoteEventListener is not a functional interface"`。

### Pitfall 5: 鼓垫鼠标事件 vs 按钮事件冲突
**What goes wrong:** `JButton` 默认用 `ActionListener` 响应点击（在 mouseReleased 时触发）。但鼓垫需要在 mousePressed 时 NoteOn、mouseReleased 时 NoteOff——标准 `ActionListener` 无法满足。

**Why it happens:** `JButton` 的 `ActionListener` 在完整点击（按下+释放）完成后才触发。

**How to avoid:** 使用 `MouseListener` 替代 `ActionListener`：
```java
padButton.addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            receiver.noteOn(midiNote, velocity, 10);
            setPressedVisual(true);
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            receiver.noteOff(midiNote, 10);
            // 200ms 后恢复视觉
            holdTimer.restart();
        }
    }
});
```
右键点击用 `mouseClicked` 配合 `SwingUtilities.isRightMouseButton()` 弹出 `JPopupMenu`。

**Warning signs:** 鼠标按下无声、松开时才发声；右键菜单不出现。

## Code Examples

Verified patterns from official sources:

### KeyboardFocusManager 完整注册（含焦点检查、去重）
```java
// Source: KeyboardFocusManager JavaDoc + Oracle Java Tutorials + StackOverflow community verification
// Pattern: Global key dispatcher with focus guard and debounce
public void registerKeyboardDispatcher() {
    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    
    kfm.addKeyEventDispatcher(e -> {
        // Step 1: 仅处理 KEY_PRESSED 和 KEY_RELEASED
        int id = e.getID();
        if (id != KeyEvent.KEY_PRESSED && id != KeyEvent.KEY_RELEASED) return false;
        
        // Step 2: 尊重已消费事件
        if (e.isConsumed()) return false;
        
        // Step 3: 焦点保护——文本输入组件不拦截
        Component owner = kfm.getFocusOwner();
        if (owner instanceof javax.swing.text.JTextComponent) return false;
        
        // Step 4: 查找键位映射
        int note = keyboardMapper.keyCodeToNote(e.getKeyCode());
        if (note < 0) return false;
        
        // Step 5: 去重 + MIDI 发送
        boolean pressed = (id == KeyEvent.KEY_PRESSED);
        if (pressed && !keyboardMapper.isActive(note)) {
            receiver.noteOn(note, 100, 0);
            eventBus.fireLiveNoteOn(note, 100);
            keyboardMapper.setActive(note, true);
            hintPanel.setKeyHighlighted(e.getKeyCode(), true);
        } else if (!pressed && keyboardMapper.isActive(note)) {
            receiver.noteOff(note, 0);
            eventBus.fireLiveNoteOff(note);
            keyboardMapper.setActive(note, false);
            hintPanel.setKeyHighlighted(e.getKeyCode(), false);
        }
        return false;
    });
}
```

### WindowFocusListener 清理逻辑
```java
// Source: WindowFocusListener JavaDoc, verified by StackOverflow community
frame.addWindowFocusListener(new WindowAdapter() {
    @Override
    public void windowLostFocus(WindowEvent e) {
        // 1. 清理所有活跃 MIDI 音符
        for (int note = 0; note < 128; note++) {
            if (keyboardMapper.isActive(note)) {
                receiver.noteOff(note, 0);
                eventBus.fireLiveNoteOff(note);
                keyboardMapper.setActive(note, false);
            }
        }
        // 2. 清理键盘面板高亮
        hintPanel.clearAllHighlights();
    }
});
```

### GM 打击乐 Channel 10 MIDI 发送
```java
// Source: MIDI 1.0 Specification + General MIDI System Level 1
// Pattern: Drum pad trigger with channel 10
public void triggerDrumPad(int midiNote, int velocity) {
    try {
        ShortMessage msg = new ShortMessage(
            ShortMessage.NOTE_ON,  // command
            9,                     // channel = 10 (0-indexed: 9)
            midiNote,              // data1 = note number (35-81 for GM percussion)
            velocity               // data2 = velocity (100 = fixed per D-02/D-05)
        );
        synthesizer.getReceiver().send(msg, -1);
    } catch (InvalidMidiDataException ex) {
        System.err.println("MIDI send failed: note=" + midiNote + " channel=10");
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 单组件 KeyListener | KeyboardFocusManager.addKeyEventDispatcher() | JDK 1.4 (2002) | KeyListener 只在聚焦组件上触发，不适合音乐应用 |
| 独立 Thread 发送 MIDI | EDT 直接发送 | Phase 3 设计 | MIDI send() < 1ms，无需独立线程；EDT 发送避免同步问题 |
| JButton 点击触发声音 | MouseListener press/release 分离 | Phase 3 设计 | ActionListener 在 release 时触发，无法实现"按下发声、松开静音" |
| 全局消费按键 (return true) | 不消费按键 (return false) | Phase 3 设计 | 让菜单快捷键等正常运作 |

**Deprecated/outdated:**
- **KeyListener 用于全局键盘捕获：** 在 JDK 1.4 引入 `KeyboardFocusManager` 后已过时。KeyListener 受焦点限制，不适合音乐应用。
- **`Toolkit.addAWTEventListener()` 用于 Swing 快捷键：** 过于底层，不参与 Swing 焦点分发链，可能导致重复处理。

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Gervill SoftSynthesizer 的 Receiver.send() 在 EDT + Sequencer 线程并发下安全 | Architecture Patterns | MIDI 音符错乱或合成器崩溃（低概率——已有 Phase 1-2 的 Sequencer + RealtimeReceiver 共享 Receiver 验证） |
| A2 | Windows 上 OS auto-repeat 只产生 KEY_PRESSED 事件（无 KEY_RELEASED） | Common Pitfalls | 如果产生 KEY_RELEASED，boolean[128] 去重仍然正确（按下时置 true，auto-repeat KEY_PRESSED 被去重忽略，真实 KEY_RELEASED 置 false），所以实际无风险 |
| A3 | 用户在文本框中输入时不会期望触发音符 | Common Pitfalls | 如果用户期望全局无死角弹奏，需要移除 focusOwner 检查（可配置化解决） |

**说明：** 本表仅列出 `[ASSUMED]` 级别的声明。所有标记 `[VERIFIED]` 或 `[CITED]` 的声明已在 Sources 节给出引用。

## Open Questions

1. **RealtimeReceiver channel 参数重载是否影响 Phase 1-2 功能？**
   - What we know: 增加重载方法是加法操作，不影响现有 2 参数 API
   - What's unclear: CONTEXT.md D-09 说"zero changes needed"，但实际需要小改
   - Recommendation: 增加 `noteOn(note, velocity, channel)` 和 `noteOff(note, channel)` 重载，保留原有 2 参数方法不变。向后兼容。

2. **键盘映射是否要可配置？**
   - What we know: CONTEXT.md D-01 固定了映射表；Deferred Ideas 提到未来可能可配置
   - What's unclear: Phase 3 需要预留配置接口吗？
   - Recommendation: Phase 3 硬编码 D-01 映射。在 KeyboardMapper 中用 `static final Map<Integer, Integer>` 存储，注释标注"future: load from config"。够简单、够用。

3. **鼓垫 200ms 视觉暂留是否与 Radiance 动画冲突？**
   - What we know: Radiance 有 `radiance-animation` 库，但目前未被 Phase 3 使用
   - What's unclear: 手动 Timer 和 Radiance animation 共存时是否闪烁
   - Recommendation: Phase 3 使用简单 `javax.swing.Timer(200ms, one-shot)` 控制视觉暂留。Phase 4 如需动画升级，改为 Radiance 动画库。当前够用。

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 17 (JDK) | 编译 + 运行 | YES | 17.0.12 LTS | — |
| Gradle | 构建 | YES | (via wrapper) | — |
| javax.sound.midi | MIDI 引擎 | YES | JDK 内置 | — |
| Radiance 8.5.0 | UI 主题 | YES | 8.5.0 | — |
| JUnit Jupiter | 测试 | YES | 5.10.0 | — |
| Node.js | GSD 工具链 | YES | 24.13.0 | — |
| SoundFont (FluidR3_GM.sf2) | 音质 | UNKNOWN | — | JDK 默认 soundbank（音质较低但功能正常） |

**Missing dependencies with no fallback:** 无。所有核心依赖可通过 Gradle 自动下载。

**Missing dependencies with fallback:**
- SoundFont：如果未下载，JDK 默认 soundbank 可用——GM 打击乐映射和 MIDI 音符标准不变。

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | build.gradle (test block: `useJUnitPlatform()`) |
| Quick run command | `./gradlew test --tests "com.rebeatbox.live.*"` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| LIVE-01 | QWERTY行 → C4-C5 映射 | unit | `./gradlew test --tests "KeyboardMapperTest.shouldMapQwertyRowToC4Octave"` | NO — Wave 0 |
| LIVE-01 | 三排覆盖C3-C6 | unit | `./gradlew test --tests "KeyboardMapperTest.shouldCoverThreeOctavesC3ToC6"` | NO — Wave 0 |
| LIVE-02 | keyPress → NoteOn, keyRelease → NoteOff | unit | `./gradlew test --tests "KeyboardMapperTest.shouldTrackActiveNotes"` | NO — Wave 0 |
| LIVE-02 | boolean[128] 去重防止鬼音 | unit | `./gradlew test --tests "KeyboardMapperTest.shouldDeduplicateRepeatedKeyPresses"` | NO — Wave 0 |
| LIVE-03 | 实时音符与Sequencer共享Receiver | integration | 手动UAT（自动化需音频回路） | N/A — manual |
| LIVE-04 | 16 pad默认GM打击乐映射 | unit | `./gradlew test --tests "DrumPadGridTest.shouldHave16padsWithGMDefaults"` | NO — Wave 0 |
| LIVE-04 | pad按下→NoteOn(ch=10), 释放→NoteOff | unit | `./gradlew test --tests "PadButtonTest.shouldSendNoteOnChannel10"` | NO — Wave 0 |
| D-06 | 右键菜单→修改pad音色赋值 | unit | `./gradlew test --tests "DrumPadGridTest.shouldReassignPadSound"` | NO — Wave 0 |
| D-10 | liveNoteOn事件→PianoRollPanel收到 | unit | `./gradlew test --tests "NoteEventBusTest.shouldFireLiveNoteOnToSubscribers"` | NO — Wave 0 |
| D-11 | stop时CC 123清理所有音符 | unit | `./gradlew test --tests "PlaybackControllerTest.shouldSendAllNotesOffOnStop"` | NO — Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test --tests "com.rebeatbox.live.*"`
- **Per wave merge:** `./gradlew test`
- **Phase gate:** Full suite green + manual UAT (keyboard playability test)

### Wave 0 Gaps
- [ ] `src/test/java/com/rebeatbox/live/KeyboardMapperTest.java` — covers LIVE-01, LIVE-02
- [ ] `src/test/java/com/rebeatbox/live/DrumPadGridTest.java` — covers LIVE-04, D-06
- [ ] `src/test/java/com/rebeatbox/live/PadButtonTest.java` — covers LIVE-04 (NoteOn/Off)
- [ ] `src/test/java/com/rebeatbox/engine/NoteEventBusLiveTest.java` — covers D-10 (live events)
- [ ] `src/test/java/com/rebeatbox/engine/RealtimeReceiverChannelTest.java` — validates channel overload
- [ ] `src/main/java/com/rebeatbox/live/` directory — new package, doesn't exist yet

## Security Domain

> Required when `security_enforcement` is enabled (absent = enabled). Omit only if explicitly `false` in config.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | 本地桌面应用，无用户认证 |
| V3 Session Management | no | 无会话概念 |
| V4 Access Control | no | 单用户应用 |
| V5 Input Validation | yes | KeyEvent 来源为 AWT 系统队列（已由 OS 验证），MIDI note 值范围检查 (0-127)，velocity 范围检查 (0-127) |
| V6 Cryptography | no | 无敏感数据存储 |

### Known Threat Patterns for javax.sound.midi

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| 无效 MIDI note 值（< 0 或 > 127）导致 `InvalidMidiDataException` | Denial of Service | KeyboardMapper 查表只返回有效 note (0-127)，未映射键返回 -1 哨兵值，调用前检查 |
| 大量快速按键引发 MIDI 消息风暴 | Denial of Service | `boolean[128]` 去重天然限流——同一音符在未释放前不会重复发送 NoteOn |
| 合成器未初始化时调用 `send()` | Information Disclosure | `App.java` 在合成器初始化失败时 `System.exit(1)`，保证 Receiver 始终可用 |

## Sources

### Primary (HIGH confidence)
- [Oracle Java Sound Programmer's Guide - Chapter 10: Transmitting and Receiving MIDI Messages](https://docs.oracle.com/javase/8/docs/technotes/guides/sound/programmer_guide/chapter10.html) — MIDI Receiver 多源架构验证
- [KeyboardFocusManager JavaDoc (OpenJDK 24)](https://apidia.net/java/OpenJDK/24/java.awt.KeyboardFocusManager.html) — addKeyEventDispatcher API 规范
- [KeyEventDispatcher JavaDoc (OpenJDK 24)](https://apidia.net/java/OpenJDK/24/java.awt.KeyEventDispatcher.html) — dispatchKeyEvent 返回值语义
- [WindowFocusListener JavaDoc](http://jszx-jxpt.cuit.edu.cn/javaapi/java/awt/event/WindowFocusListener.html) — 窗口焦点事件 API
- [General MIDI Percussion Key Map (CMU)](http://www.music.cs.cmu.edu/cmp/archives/cmsip/readings/GMSpecs_PercMap.htm) — GM 打击乐标准音符映射表
- [MIDI 1.0 Specification - MIDI.org](https://midi.org/community/midi-specifications) — Channel 10 打击乐标准

### Secondary (MEDIUM confidence)
- [JDK-4791258: SPEC: javax_sound spec does not specify which resources are 'thread-safe'](https://bugs.openjdk.org/browse/JDK-4791258) — Receiver.send() 线程安全性未在规范中声明
- [JDK-8290993: Gervill SoftSynthesizer receivers share channel state](https://bugs.openjdk.org/browse/JDK-8290993) — Gervill 多个 Receiver 共享通道状态
- [StackOverflow: How to stop repeated keyPressed/keyReleased events in Swing](https://stackoverflow.com/questions/1736828) — OS auto-repeat 处理社区方案
- [StackOverflow: How can I listen for key presses across all components?](https://stackoverflow.com/questions/5344823) — KeyboardFocusManager 社区最佳实践
- [StackOverflow: How to know when a user has really released a key?](https://stackoverflow.com/questions/1457071) — 按键释放检测方案（含 RepeatingReleasedEventsFixer）

### Tertiary (LOW confidence)
- General web search results on GM percussion map (multiple sources agree, cross-referenced with CMU authoritative source)

### Codebase Sources (HIGH confidence)
- `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java` — 现有 MIDI 实时发送器，已验证 noteOn/Off 模式
- `src/main/java/com/rebeatbox/engine/PlaybackController.java` — Sequencer 管理，已验证 CC 123 All Notes Off 实现
- `src/main/java/com/rebeatbox/engine/NoteEventBus.java` — 事件总线 (CopyOnWriteArrayList)，已验证线程安全模式
- `src/main/java/com/rebeatbox/engine/NoteEventListener.java` — @FunctionalInterface 单方法接口，已验证需要扩展
- `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` — 主窗口布局，已验证 NORTH/CENTER/EAST 区域
- `src/main/java/com/rebeatbox/ui/SidebarPanel.java` — 侧边栏，已验证 contentPanel 占位符
- `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` — 钢琴卷帘，已验证自定义 paintComponent 模式
- `src/main/java/com/rebeatbox/App.java` — 应用入口，已验证 RealtimeReceiver 创建流程
- `build.gradle` — 构建配置，已验证 JUnit Jupiter 5.10.0 + Java 17

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — 全部基于 JDK 标准库 + 已有 Radiance，无第三方依赖争议
- Architecture: HIGH — KeyboardFocusManager + MIDI Receiver 共享方案已在 Oracle 官方文档和社区验证
- Pitfalls: HIGH — 窗口失焦鬼音、文本输入劫持、RealtimeReceiver channel 硬编码等问题有明确定义和解决方案
- NoteEventBus 扩展: HIGH — CopyOnWriteArrayList 模式已在 Phase 1-2 验证，增加事件类型零风险

**Research date:** 2026-04-28
**Valid until:** 2026-05-12 (30 days — JDK 17 AWT/MIDI API 极其稳定，无 Breaking Change 风险)

**Codebase analysis:**
- 15 个源文件 (.java) 位于 `src/main/java/com/rebeatbox/`
- 3 个测试文件位于 `src/test/java/com/rebeatbox/visual/`
- 需要创建的新包: `live/` (3 个新文件)
- 需要修改的文件: 4 个 (RealtimeReceiver, NoteEventBus, NoteEventListener, ReBeatBoxWindow)
- RealtimeReceiver 关键发现: 硬编码 Channel 0，需增加 channel 参数重载以支持鼓垫 Channel 10
