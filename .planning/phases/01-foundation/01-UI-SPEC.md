---
phase: 1
slug: foundation
status: approved
framework: radiance-swing
created: 2026-04-27
---

# Phase 1 — UI Design Contract

> Visual and interaction contract for ReBeatBox Phase 1: Foundation.
> Java Swing + Radiance 8.5.0 — desktop application, not web.

---

## Design System

| Property | Value |
|----------|-------|
| Toolkit | Java Swing + Radiance 8.5.0 |
| Skin | Radiance NightShade (dark) |
| Icon library | Unicode/ASCII glyphs (Phase 1 temporary) → custom SVG via radiance-svg-transcoder (Phase 4) |
| Font | SansSerif system default — Phase 1 |

---

## Spacing Scale

Radiance `LayoutManager` tokens. All values in pixels, multiples of 4.

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | Transport button gap, label-to-slider gap |
| sm | 8px | Control bar padding, button internal padding |
| md | 16px | Control bar section gaps, sidebar width padding |
| lg | 24px | Window edge margins, placeholder panel padding |
| xl | 32px | Major section separation |
| 2xl | 48px | Placeholder title vertical offset |
| 3xl | 64px | — (reserved) |

Exceptions: none

---

## Typography

Java `Font` declarations. SansSerif family, consistent with Radiance default.

| Role | Size | Style | Usage |
|------|------|-------|-------|
| Body | 13px | PLAIN | Control labels, file path text |
| Label | 12px | PLAIN | BPM/volume slider labels, sidebar header |
| Heading | 18px | BOLD | Placeholder panel title "ReBeatBox" |
| Display | 28px | BOLD | — (reserved for Phase 4 splash) |

Line height: default Java component text rendering (no CSS line-height — Swing handles this via component sizing).

---

## Color

Mapped to Radiance NightShade skin color tokens. Values are approximate hex equivalents — actual rendering uses Radiance `ColorToken` system.

| Role | Hex (approx) | Radiance Token Source | Usage |
|------|-------------|----------------------|-------|
| Dominant (60%) | #1a1a2e | NightShade background | Main window background, placeholder panel |
| Secondary (30%) | #16213e | NightShade container | Control bar background, sidebar panel, buttons |
| Accent (10%) | #0f3460 | NightShade accent | Transport button active state, progress bar fill |
| Destructive | #e94560 | Custom override | Stop button, error dialog highlight |
| Text Primary | #e0e0e0 | NightShade foreground | All body text |
| Text Muted | #888888 | NightShade disabled | Slider labels, inactive button text |
| Neon Cyan | #00f5ff | Custom (Phase 4 full application) | — reserved for Phase 4 accent replacement |
| Neon Magenta | #ff00ff | Custom (Phase 4 full application) | — reserved for Phase 4 particle/note mapping |

Accent reserved for: transport button active state, progress bar filled track, sidebar toggle button hover. NOT for: general backgrounds, regular button borders.

**Phase 1 note:** Full cyber neon palette (#00f5ff, #ff00ff) is reserved for Phase 4. Phase 1 uses Radiance NightShade defaults — clean, professional dark theme without neon effects.

---

## Component Inventory

### Control Bar (Top)
| Component | Type | Dimensions | Behavior |
|-----------|------|-----------|----------|
| Transport buttons | `JButton` with Unicode glyphs | 40×40px each | Click → action. Hover → background highlight. Unicode: ▶ (play), ⏸ (pause), ⏹ (stop), ⏮ (restart) |
| BPM slider | `JSlider` + `JLabel` | 200px wide + label | Range 20-300, default 120. Label reads "BPM: 120" |
| Volume slider | `JSlider` + `JLabel` | 150px wide + label | Range 0-100, default 75. Label reads "Vol: 75%" |
| Progress bar | `JProgressBar` | fills remaining width | Click to seek. Shows mm:ss / mm:ss |
| File open button | `JButton` | 36×36px | Unicode: 📂. Opens JFileChooser with filter "MIDI Files (*.mid)" |

Control bar layout: `[TransportButtons] [gap] [BPM] [gap] [Volume] [gap] [Progress===] [gap] [Open]`

### Main Content Area (Center)
| Component | Type | Dimensions | Behavior |
|-----------|------|-----------|----------|
| Placeholder panel | `JPanel` with custom painting | fills available space | Dark gradient background (NightShade deep). Centered "ReBeatBox" title in BOLD 18px, subtitle "Drop a .mid file or click ▶ to open" in 13px below |

### Sidebar (Right, Collapsible)
| Component | Type | Dimensions | Behavior |
|-----------|------|-----------|----------|
| Sidebar panel | `JPanel` | 240px wide, collapsible | Toggle button in top-right corner. Phase 1: empty dark panel. Collapse animation: smooth width transition |
| Toggle button | `JButton` | 24×24px | Unicode: ◀ (expanded) / ▶ (collapsed). Located at sidebar top |

### Window Chrome
| Property | Value |
|----------|-------|
| Title | "ReBeatBox" |
| Icon | Radiance default (temporary) |
| Initial size | 1100 × 700 px |
| Minimum size | 800 × 500 px |
| Resizable | Yes |

---

## Copywriting Contract

| Element | Copy |
|---------|------|
| Window title | "ReBeatBox" |
| Placeholder heading | "ReBeatBox" |
| Placeholder subtitle | "Drop a .mid file or click ▶ to open" |
| File dialog title | "Open MIDI File" |
| File dialog filter | "MIDI Files (*.mid)" |
| BPM label | "BPM: {value}" |
| Volume label | "Vol: {value}%" |
| Error — corrupt file | "无法播放此文件" |
| Error — corrupt file detail | "文件可能已损坏或不是标准 MIDI 格式" |
| Error — no audio | "无法初始化音频设备" |
| Error — no audio detail | "请检查系统音频设置后重新启动应用" |
| Error — no SoundFont | "无法加载音色库" |
| Error — no SoundFont detail | "SoundFont 文件缺失或损坏，请重新安装应用" |
| Transport tooltip — play | "播放" |
| Transport tooltip — pause | "暂停" |
| Transport tooltip — stop | "停止" |
| Transport tooltip — restart | "重新开始" |

---

## Interaction States

| Component | Default | Hover | Active/Pressed | Disabled |
|-----------|---------|-------|----------------|----------|
| Transport button | NightShade button bg | Brighter bg (hover highlight) | Accent blue bg | Muted text |
| Slider thumb | NightShade slider thumb | Brighter | Accent blue | Muted |
| Progress bar track | Dark bg | — | Accent blue filled portion | — |
| Sidebar toggle | NightShade button | Brighter bg | Accent blue bg | — |

---

## Drag and Drop

- Accept: `.mid` files only
- Drag-over visual: window border glow (subtle NightShade accent outline around content area)
- Drop: load file → auto-play. If file invalid → error dialog
- Drop zone: entire window (not just placeholder panel)

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS — All UI text, tooltips, error messages, labels defined
- [x] Dimension 2 Visuals: PASS — Component inventory, dimensions, interaction states defined
- [x] Dimension 3 Color: PASS — 60/30/10 split, accent reserved list, token sources mapped
- [x] Dimension 4 Typography: PASS — 4 roles declared, sizes + styles specified
- [x] Dimension 5 Spacing: PASS — 4px-multiple scale, token→usage mapping
- [x] Dimension 6 Registry Safety: N/A — Java desktop app, no third-party UI registries

**Approval:** approved 2026-04-27
