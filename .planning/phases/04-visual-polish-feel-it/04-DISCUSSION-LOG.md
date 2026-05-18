# Phase 4: Visual Polish - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-28
**Phase:** 04-visual-polish-feel-it
**Areas discussed:** 霓虹调色板体系, 粒子爆发效果, Glitch 转场动画, 按钮动画+SVG图标, 音符辉光线增强, Velocity亮度映射, 粒子系统性能策略, 键盘提示面板霓虹化

---

## 霓虹调色板体系

### 调色板管理方式

| Option | Description | Selected |
|--------|-------------|----------|
| ThemeManager 集中管理 | 创建 ThemeManager 类，语义化颜色常量，所有组件统一读取 | ✓ |
| 统一常量值即可 | 不改架构，统一现有硬编码值 | |
| 扩展现有 NoteColorMapper | 让 NoteColorMapper 同时管音符和 UI 颜色 | |

### 主霓虹强调色

| Option | Description | Selected |
|--------|-------------|----------|
| Cyan 主 + Magenta 辅 | 经典赛博双色 | |
| 全彩虹渐变 | 不同 UI 区域不同色相，基于 HSB 扩展 | ✓ |
| Cyan 单色系 | 只用 Cyan，不同明度区分层级 | |

### 背景色层级

| Option | Description | Selected |
|--------|-------------|----------|
| 三层暗色递进 | BG_ROOT:#0a0a14 / BG_SURFACE:#12122a / BG_ELEVATED:#1a1a3e | ✓ |
| 纯黑统一 | 全部 #000000 + 不同透明度 | |
| 只用 Radiance 默认 | NightShade 默认色，不额外覆盖 | |

### Radiance 边界

| Option | Description | Selected |
|--------|-------------|----------|
| NightShade 骨架 + 霓虹色覆盖 | Radiance 管结构，自定义管颜色 | ✓ |
| 完全自定义 | 弃用 NightShade，全 paintComponent | |
| NightShade 为主，微调 | 不改 NightShade，只在现有基础上调色 | |

---

## 粒子爆发效果

### 粒子样式

| Option | Description | Selected |
|--------|-------------|----------|
| 赛博方块 | 小正方形/矩形，随机旋转，发光边框 | ✓ |
| 能量圆点 | 圆形光点，向外扩散淡出 | |
| Glitch 碎片 | 不规则多边形碎片，模拟数字信号破碎 | |

### 渲染位置

| Option | Description | Selected |
|--------|-------------|----------|
| PianoRollPanel 内 | 在 paintComponent 里，音符层之上 | |
| 全局 GlassPane | JFrame GlassPane，粒子可飞出到任意位置 | ✓ |
| PianoRollPanel 内 + 仅向上 | 限制在 PianoRollPanel 内，固定向上爆 | |

### 粒子配色

| Option | Description | Selected |
|--------|-------------|----------|
| 跟音符 pitch 配色 | NoteColorMapper.forPitch(note) | ✓ |
| 统一 Cyan | 所有粒子 #00E5FF | |
| 随机霓虹色 | 每次 note-on 随机选色 | |

### 触发时机

| Option | Description | Selected |
|--------|-------------|----------|
| 所有 note-on 都爆 | Sequencer + 实时演奏都触发粒子 | ✓ |
| Sequencer 爆粒子，实时走高亮 | 分担粒子系统压力 | |
| 按 velocity 阈值过滤 | 只有 velocity > 阈值才爆 | |

---

## Glitch 转场动画

### 转场触发

| Option | Description | Selected |
|--------|-------------|----------|
| Sidebar toggle + 文件替换 | 当前真正存在的两个视图切换 | ✓ |
| 仅 Sidebar toggle | 最小范围 | |
| Sidebar + 文件 + 播放状态 | 最大覆盖 | |

### 转场风格

| Option | Description | Selected |
|--------|-------------|----------|
| 扫描线撕裂 | VHS/CRT 水平扫描线错位 | |
| RGB 通道分离 | 红左移、蓝右移、噪点叠加 | ✓ |
| 块位移 + 噪点 | 像素块随机切割位移 | |
| 随机混合 | 每次随机选一种效果 | |

### 转场时长

| Option | Description | Selected |
|--------|-------------|----------|
| 短促 150-250ms | 快速统一 | |
| 中等 250-500ms | 更戏剧化 | |
| 按场景区分 | Sidebar 快、文件替换慢 | ✓ |

### 实现方式

| Option | Description | Selected |
|--------|-------------|----------|
| 截图 + GlassPane | 截帧到 BufferedImage，在 GlassPane 做特效 | |
| 组件级动画 | Radiance ephemeral Timeline 驱动每个组件 | ✓ |
| PianoRollPanel 内 | 污染渲染区域 | |

---

## 按钮动画 + SVG 图标

### 动画类型

| Option | Description | Selected |
|--------|-------------|----------|
| 边框发光 + 缩放反馈 | hover 边框变霓虹 + press 0.95x 缩放 | ✓ |
| 背景色渐变脉冲 | 背景色渐变填充 | |
| 外发光 halo | 按钮周围发光光环 | |

### 动画范围

| Option | Description | Selected |
|--------|-------------|----------|
| 全部按钮统一 | ControlBar + PadButton + Sidebar toggle | ✓ |
| 核心交互按钮 | 只 ControlBar transport + PadButton | |
| 分组件差异化 | ControlBar 只换图标，PadButton 全套动画 | |

### SVG 加载

| Option | Description | Selected |
|--------|-------------|----------|
| Apache Batik | 标准 SVG→BufferedImage 渲染 | ✓ |
| 纯 Java2D 手绘 | Shape + Path2D 直接画 | |
| 预渲染 PNG | 多尺寸 PNG 加载 | |

### 图标集范围

| Option | Description | Selected |
|--------|-------------|----------|
| 最小集 7-8 个 | transport 5 + open + toggle + app icon | |
| 中等集 ~12 个 | 最小集 + BPM + Volume + Keyboard + Drum | |
| 完整集 ~20 个 | 全部预留，包括 Phase 5 | ✓ |

---

## 音符辉光线增强

### 辉光范围

| Option | Description | Selected |
|--------|-------------|----------|
| 增强现有 bar glow 即可 | 参数增强，不动架构 | ✓ |
| bar glow + 音符拖尾 | 新增水平 motion trail | |
| 全面辉光线 | bar glow + 拖尾 + 和弦连线 + 八度发光 | |

### Glow 参数

| Option | Description | Selected |
|--------|-------------|----------|
| 中等增强 7×7, sigma 3.5 | 平衡可见度和不过曝 | ✓ |
| 激进增强 9×9, sigma 5.0 | 辉光更明显但可能过曝 | |
| 自适应（按音符高度）| 短音符大 kernel，长音符小 kernel | |

### 辉光算法

| Option | Description | Selected |
|--------|-------------|----------|
| 只改参数 | kernel 和 sigma 调大，算法不变 | ✓ |
| 双层辉光叠加 | 宽光晕层 + 精确辉光层 | |
| 手动多层绘制 | 抛弃 ConvolveOp | |

### 触发线辉光

| Option | Description | Selected |
|--------|-------------|----------|
| 保持现状 | pulse + 2px core + 8px halo 不变 | ✓ |
| 扩展到全宽 | 贯穿整窗包括 mini keyboard | |
| 加扫描效果 | 周期性向上扫描光柱 | |

---

## Velocity 亮度映射

### 做不做

| Option | Description | Selected |
|--------|-------------|----------|
| 实现了 | 不再 defer，v1 就做 | ✓ |
| 继续 defer 到 v2 | Phase 2 的原决定保持 | |
| 简化版三档映射 | soft/medium/strong 三档 alpha | |

### 映射方式

| Option | Description | Selected |
|--------|-------------|----------|
| 映射到 alpha 透明度 | Velocity 0-127 → alpha 线性映射 | ✓ |
| 映射到 HSB brightness | 调整饱和度和亮度 | |
| alpha + brightness 双通道 | 两者都调 | |

### 适用范围

| Option | Description | Selected |
|--------|-------------|----------|
| 全区域 | 触发线上方和下方都映射 | ✓ |
| 仅触发线上方 | 下方保持统一 0.4 | |
| 粒子系统用 velocity | bar glow 不变，velocity 映射到粒子 | |

### 映射曲线

| Option | Description | Selected |
|--------|-------------|----------|
| 线性映射 | 简单直接 | ✓ |
| 指数曲线 | 低 velocity 压得更暗 | |
| Gamma 校正 | 视觉上更自然的亮度梯度 | |

---

## 粒子系统性能策略

### 粒子上限策略

| Option | Description | Selected |
|--------|-------------|----------|
| 拒绝新粒子 | 200 满了就不生成 | |
| 淘汰最老粒子 | FIFO 淘汰 | |
| 合并老粒子 | 最老 2-3 个合并成一个大粒子 | ✓ |

### 粒子生命周期

| Option | Description | Selected |
|--------|-------------|----------|
| 500-800ms 线性淡出 | 简单飘散 | |
| 300-500ms + 重力 | 物理式下落 | |
| Velocity → 生命周期 | 强音 800ms，弱音 300ms | ✓ |

### 帧同步

| Option | Description | Selected |
|--------|-------------|----------|
| 独立 Timer | 自己的 16ms Timer | ✓ |
| 复用 PianoRollPanel timer | 共用现有 timer | |
| Radiance Trident 驱动 | 和按钮动画统一 | |

### 粒子大小

| Option | Description | Selected |
|--------|-------------|----------|
| 固定随机 2-5px | 简单统一 | |
| Velocity → 粒子大小 | 强音 4-8px，弱音 1-3px | ✓ |
| Pitch → 粒子大小 | 按音高映射 | |

---

## 键盘提示面板霓虹化

### 霓虹化程度

| Option | Description | Selected |
|--------|-------------|----------|
| 统一霓虹化 | 走 ThemeManager，idle/pressed 都用霓虹色 | ✓ |
| 微调，保持低调 | 保持灰暗底色，只调 pressed 高亮 | |
| 按 pitch 配色高亮 | pressed 键颜色跟 NoteColorMapper 走 | |

### 按键动画

| Option | Description | Selected |
|--------|-------------|----------|
| 即时切换 | 瞬间变色，零延迟 | ✓ |
| 渐变过渡 | 100ms 渐亮 + 200ms 渐暗 | |
| 缩放弹跳 | scale 1.1x 弹回 | |

---

## Claude's Discretion

- ThemeManager 具体 API 设计
- 粒子运动轨迹细节
- Glitch 转场 RGB 偏移量、噪点密度、精确时长
- SVG 图标具体视觉设计
- 按钮 Timeline 精确参数
- 粒子合并算法细节
- KeyboardHintPanel 精确霓虹色值

## Deferred Ideas

- 音符水平拖尾 (motion trail) — GLITCH-03 讨论时排除
- 和弦辉光连线 — GLITCH-03 讨论时排除
- 八度分隔线发光 — GLITCH-03 讨论时排除
- 双通道 velocity 映射 — 只用 alpha
- 键盘按键缩放弹跳 — 延迟敏感场景排除
- 粒子重力/物理模拟 — 简单 velocity+fade 即可
