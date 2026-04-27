---
wave: 2
depends_on: ["01-PLAN"]
files_modified:
  - src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java
  - src/main/java/com/rebeatbox/ui/ControlBar.java
  - src/main/java/com/rebeatbox/ui/PlaceholderPanel.java
  - src/main/java/com/rebeatbox/ui/SidebarPanel.java
  - src/main/java/com/rebeatbox/App.java
requirements:
  - CONT-02
autonomous: true
---

# Plan 03: App Window & UI Shell

**Objective:** Build the complete application window with NightShade dark theme — control bar with transport buttons / BPM slider / volume slider / progress bar, placeholder center panel, collapsible right sidebar. Window is ready for Phase 2 piano roll and Phase 3 drum pad. Drag-and-drop support for .mid files.

## Tasks

<task id="03-01" type="execute">
<objective>Create PlaceholderPanel — dark branded center panel</objective>

<read_first>
- .planning/phases/01-foundation/01-UI-SPEC.md § Main Content Area, Copywriting, Typography
- .planning/phases/01-foundation/01-CONTEXT.md § D-02 (dark placeholder with centered logo)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/ui/PlaceholderPanel.java`:

Extends `JPanel`. Override `paintComponent(Graphics g)`:
- Cast to Graphics2D
- Fill background with a subtle dark gradient (top: NightShade background darker shade, bottom: slightly lighter). Use `GradientPaint`.
- Draw centered title "ReBeatBox" — Font("SansSerif", BOLD, 18), color from Radiance color token (foreground)
- Draw subtitle below title — Font("SansSerif", PLAIN, 13), color muted: "Drop a .mid file or click ▶ to open"
- Use FontMetrics to center both text strings horizontally and vertically

Constructor: no args needed. Set `setBackground` from Radiance color token.
Set preferred size: null (fills available space in BorderLayout.CENTER).
</action>

<acceptance_criteria>
- `PlaceholderPanel.java` overrides `paintComponent(Graphics)`
- Title text "ReBeatBox" rendered with Font("SansSerif", BOLD, 18)
- Subtitle "Drop a .mid file or click ▶ to open" rendered with Font("SansSerif", PLAIN, 13)
- Uses `GradientPaint` for background
</acceptance_criteria>
</task>

<task id="03-02" type="execute">
<objective>Create ControlBar — transport buttons, sliders, progress bar</objective>

<read_first>
- .planning/phases/01-foundation/01-UI-SPEC.md § Control Bar (Top) for component inventory, dimensions, layout
- .planning/phases/01-foundation/01-UI-SPEC.md § Copywriting for tooltips and labels
- .planning/phases/01-foundation/01-CONTEXT.md § D-01 (top-positioned control bar), D-19 (BPM 20-300, Vol 0-100)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/ui/ControlBar.java`:

Extends `JPanel` with `FlowLayout(FlowLayout.LEFT)`. Apply Radiance component styling.

Components (left to right):
1. Transport buttons panel (JPanel, FlowLayout, gap xs=4):
   - ⏮ restart (rewind) — JButton, 40x40, Unicode '⏮'
   - ▶ play — JButton, 40x40, Unicode '▶'
   - ⏸ pause — JButton, 40x40, Unicode '⏸'
   - ⏹ stop — JButton, 40x40, Unicode '⏹'
   Tooltips: "重新开始" / "播放" / "暂停" / "停止"

2. Separator gap (rigid area 16px)

3. BPM panel (JPanel, LEFT):
   - JLabel("BPM: 120")
   - JSlider(20, 300, 120) — 200px width
   Tooltip: none (label is self-explanatory)

4. Separator gap (16px)

5. Volume panel (JPanel, LEFT):
   - JLabel("Vol: 75%")
   - JSlider(0, 100, 75) — 150px width

6. Glue/horizontal strut to push progress bar to right side

7. Progress bar panel (RIGHT side of control bar):
   - JProgressBar(0, 100) — displays current playback position as percentage
   - Time label above/beside: "00:00 / 00:00" — shows current time / total time
   - Click on progress bar → compute seek position from click x-coordinate

8. File open button (RIGHTMOST):
   - 📂 JButton, 36x36, Unicode '📂'

Store sliders and progress bar as fields for external access (PlaybackController wiring in Plan 04).
</action>

<acceptance_criteria>
- `ControlBar.java` contains 4 transport JButtons: play(▶), pause(⏸), stop(⏹), restart(⏮)
- `ControlBar.java` contains BPM JSlider with range 20-300, default 120
- `ControlBar.java` contains Volume JSlider with range 0-100, default 75
- `ControlBar.java` contains JProgressBar
- `ControlBar.java` contains JLabel for time display showing "00:00 / 00:00" format
- `ControlBar.java` contains file open JButton
- All tooltips use Chinese copy from UI-SPEC
- Control bar has FlowLayout(LEFT) with proper separator gaps
</acceptance_criteria>
</task>

<task id="03-03" type="execute">
<objective>Create SidebarPanel — collapsible right panel reserved for Drum Pad</objective>

<read_first>
- .planning/phases/01-foundation/01-UI-SPEC.md § Sidebar (Right, Collapsible)
- .planning/phases/01-foundation/01-CONTEXT.md § D-03 (collapsible right sidebar, Phase 3 drum pad)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/ui/SidebarPanel.java`:

Extends `JPanel`. Layout: BorderLayout.

Components:
- Toggle button (NORTH, right-aligned): ◀ (expanded, '◀') / ▶ (collapsed, '▶'). 24x24px.
  Tooltip: "收起侧边栏" / "展开侧边栏"
- Content panel (CENTER): empty in Phase 1. Dark background matching NightShade secondary color.

Collapse behavior:
- Default: expanded, width = 240px
- On toggle click: animate width from 240 → 0 (or 0 → 240) using a Swing Timer for smooth transition
- When collapsed: sidebar invisible except the toggle button (which moves to edge of window)
- Timer: 16ms interval (~60fps), 15 steps → 240ms animation. Decrease/increase width by 16px per step.

Width field: `int panelWidth = 240` (expanded) / `int collapsedWidth = 0`.
Set preferred size via `setPreferredSize(new Dimension(panelWidth, getHeight()))` and `revalidate()` parent.

For Phase 1 simplicity: just toggle between full/zero width instantly. Smooth animation can be refined in Phase 4.
</action>

<acceptance_criteria>
- `SidebarPanel.java` contains toggle JButton with ◀/▶ Unicode characters
- Default width is 240px
- Toggle button click collapses/expands sidebar
- Sidebar uses NightShade secondary background color
- Content panel exists and is empty (ready for Phase 3 Drum Pad grid)
</acceptance_criteria>
</task>

<task id="03-04" type="execute">
<objective>Create ReBeatBoxWindow — main application frame with layout assembly</objective>

<read_first>
- .planning/phases/01-foundation/01-UI-SPEC.md § Window Chrome (title, size, min size)
- .planning/phases/01-foundation/01-CONTEXT.md § D-04, D-05, D-06 (window config)
</read_first>

<action>
Create `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java`:

Extends `JFrame`.

Constructor:
```java
setTitle("ReBeatBox");
setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
setSize(1100, 700);
setMinimumSize(new Dimension(800, 500));
setLocationRelativeTo(null); // center on screen

// Apply Radiance NightShade skin — already done globally in App.java

setLayout(new BorderLayout());

ControlBar controlBar = new ControlBar();
PlaceholderPanel placeholder = new PlaceholderPanel();
SidebarPanel sidebar = new SidebarPanel();

add(controlBar, BorderLayout.NORTH);
add(placeholder, BorderLayout.CENTER);
add(sidebar, BorderLayout.EAST);
```

Public getters for controlBar and placeholder (needed by Plan 04 for wiring).

Drag-and-drop support:
- Set `setTransferHandler(new TransferHandler() { ... })` on the content pane
- Accept: `DataFlavor.javaFileListFlavor`
- Filter: only .mid files (check extension)
- On drop: call a callback/listener `onMidiFileDropped(File file)`
- On drag-over: highlight border (change content pane border to accent color line)
- The actual load+play logic is wired in Plan 04; just provide the callback hook

File open action:
- ControlBar's open button → `JFileChooser` with filter `new FileNameExtensionFilter("MIDI Files (*.mid)", "mid")`
- On select: call callback `onMidiFileSelected(File file)`
- Callbacks stored as `Consumer<File>` fields: `onFileSelected` and `onFileDropped`
</action>

<acceptance_criteria>
- `ReBeatBoxWindow.java` sets title "ReBeatBox"
- `ReBeatBoxWindow.java` sets size to 1100x700, min size 800x500
- `ReBeatBoxWindow.java` uses BorderLayout: NORTH=ControlBar, CENTER=PlaceholderPanel, EAST=SidebarPanel
- `ReBeatBoxWindow.java` registers TransferHandler for drag-and-drop accepting .mid files
- `ReBeatBoxWindow.java` has `Consumer<File>` fields for file selection and drop callbacks
- `JFileChooser` filter includes "MIDI Files (*.mid)"
</acceptance_criteria>
</task>

<task id="03-05" type="execute">
<objective>Update App.java — full application bootstrap with NightShade theme</objective>

<read_first>
- .planning/phases/01-foundation/01-UI-SPEC.md (full UI contract)
- .planning/research/STACK.md (Radiance API for global skin application)
</read_first>

<action>
Update `src/main/java/com/rebeatbox/App.java`:

```java
package com.rebeatbox;

import javax.swing.*;
import org.pushingpixels.radiance.theming.api.skin.NightShadeSkin;
import org.pushingpixels.radiance.theming.api.RadianceThemingCortex;
import com.rebeatbox.ui.ReBeatBoxWindow;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Apply Radiance NightShade skin globally
            RadianceThemingCortex.GlobalScope.setSkin(new NightShadeSkin());
            
            ReBeatBoxWindow window = new ReBeatBoxWindow();
            window.setVisible(true);
        });
    }
}
```

Note: Radiance API class names may vary slightly. Check actual Radiance 8.5.0 Javadoc:
- `RadianceThemingCortex.GlobalScope.setSkin(Skin)` — correct for 8.x
- `NightShadeSkin` — built-in dark skin

If exact class names differ, use Radiance demo code or Javadoc to find correct names.
</action>

<acceptance_criteria>
- `App.java` calls `RadianceThemingCortex.GlobalScope.setSkin(new NightShadeSkin())` (or equivalent)
- `App.java` creates `ReBeatBoxWindow` and calls `setVisible(true)`
- Application compiles: `gradlew compileJava`
</acceptance_criteria>
</task>

## Verification

- [ ] App launches with NightShade dark theme
- [ ] Control bar displays all transport buttons, sliders, progress bar
- [ ] Placeholder panel shows centered "ReBeatBox" title and subtitle
- [ ] Sidebar toggles collapse/expand with button click
- [ ] Window size 1100x700, centered on screen
- [ ] Drag-and-drop handler registered (accepts .mid, ignores other file types)
- [ ] File chooser opens with .mid filter on open button click

## must_haves

- `truths`: ["NightShade dark theme applied globally before window creation", "Control bar contains all transport controls as specified in UI-SPEC", "Placeholder panel renders centered branding text", "Sidebar toggles between 240px and collapsed", "Drag-and-drop accepts .mid files via TransferHandler", "Window layout reserves BorderLayout.CENTER for Phase 2 piano roll"]
