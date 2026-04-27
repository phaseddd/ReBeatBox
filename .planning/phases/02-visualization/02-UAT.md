---
status: complete
phase: 02-visualization
source: 02-01-SUMMARY.md, 02-02-SUMMARY.md
started: 2026-04-28T03:45:00Z
updated: 2026-04-28T04:15:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Piano Roll 面板替换了旧占位黑屏
expected: 启动应用后，窗口正中间显示纯黑画布 + 左侧钢琴键盘轮廓 + 中间青色触发线
result: pass

### 2. 加载 MIDI 文件后掉落音符动画
expected: 拖入或打开 .mid 文件后，音符从画布上方掉落，不同音高显示不同霓虹颜色（低音紫/蓝 → 高音橙/红）
result: pass

### 3. 音符带高斯模糊发光
expected: 每个音符方块周围有可见的霓虹光晕（模糊发光），不是生硬的纯色方块
result: issue
reported: "方块太小太快了，看不清有没有霓虹光晕"
severity: cosmetic

### 4. 触发线以下音符变暗
expected: 穿过触发线之后的音符亮度降低、透明度变淡（约 40%），上方的音符保持全亮
result: pass

### 5. 触发线脉冲动画
expected: 播放时触发线（青色横线）有呼吸式的明暗脉冲动画，不是一条死线
result: pass

### 6. Mini 键盘自动适配音域
expected: 加载不同音域的 MIDI 文件后，左侧钢琴键盘显示的琴键范围会跟着变化（只显示文件实际用到的音域）
result: pass

### 7. 点击进度条 → 音符立刻跳转
expected: 播放中点击进度条不同位置，琴键画面上音符立刻跳到对应位置（非动画过渡），音频也跳到对应位置
result: pass

### 8. 暂停 → 音符冻结
expected: 点击暂停后音符停止移动、固定在当前位置不动，恢复播放后继续从该位置掉落
result: pass

### 9. 60fps 流畅无卡顿
expected: 动画流畅无肉眼可见卡顿，在含 20 个同时音符的 MIDI 下不掉帧
result: pass

### 10. 多轨 MIDI 合并显示
expected: 加载包含多个乐器轨道的 MIDI 文件，所有轨道音符合并显示在同一个瀑布视图中，不出现重复行
result: pass

## Summary

total: 10
passed: 9
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "拖入或打开 .mid 文件后，音符从画布上方掉落，不同音高显示不同霓虹颜色"
  status: resolved
  reason: "binarySearchFirstVisible 用负数 startMicros 创建 RenderNote 哨兵，构造函数校验抛出 IllegalArgumentException"
  severity: blocker
  test: 2
  root_cause: "Collections.binarySearch 需要哨兵对象，但 RenderNote 构造函数不允许负数 startMicros。当 currentPosition=0 时 windowStart = 0 - PAST_MICROS = -2000000 触发异常。"
  artifacts:
    - path: "src/main/java/com/rebeatbox/visual/PianoRollPanel.java"
      issue: "binarySearchFirstVisible 方法用 new RenderNote(60, windowStart, ...) 创建哨兵"
  missing:
    - "改用手动二分查找，直接比较 startMicros 长整数值，不创建 RenderNote 对象"
  debug_session: ""

- truth: "每个音符方块周围有可见的霓虹光晕（模糊发光），不是生硬的纯色方块"
  status: failed
  reason: "方块太小太快了，看不清有没有霓虹光晕"
  severity: cosmetic
  test: 3
  root_cause: "5x5 ConvolveOp 高斯模糊在快速掉落的小方块上肉眼难以分辨。代码正确但视觉效果不够突出。"
  artifacts:
    - path: "src/main/java/com/rebeatbox/visual/PianoRollPanel.java"
      issue: "高斯模糊参数可能偏保守，短音符不够明显"
  missing:
    - "可考虑增大 kernel 或提高 glow 亮度，但属视觉微调，非功能性缺陷"
