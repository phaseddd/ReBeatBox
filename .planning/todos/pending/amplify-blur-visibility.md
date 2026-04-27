---
title: "Amplify GaussianBlur glow visibility on fast/small notes"
area: visualization
priority: low
resolves_phase: 4
created: 2026-04-28
source: Phase 2 UAT test 3
---

## Problem

Phase 2 的 5x5 ConvolveOp GaussianBlur 在快速掉落的小音符方块上肉眼难以分辨。代码逻辑正确，但视觉效果不够突出。

## Suggested Fix

Phase 4 (Visual Polish) 时调大 kernel size（例如 7x7 或 9x9）、提高 glow alpha、或改用 multi-pass blur 叠加。短音符（barHeight < 10px）可以适度增强发光效果。

## References

- `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` — drawNoteBar() method, ConvolveOp kernel
- `.planning/phases/02-visualization/02-UAT.md` — Test 3 (cosmetic)
