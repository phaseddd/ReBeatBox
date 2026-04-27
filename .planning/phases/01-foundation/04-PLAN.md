---
wave: 3
depends_on: ["02-PLAN", "03-PLAN"]
files_modified:
  - src/main/java/com/rebeatbox/App.java
  - src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java
  - src/main/java/com/rebeatbox/ui/ControlBar.java
requirements:
  - PLAY-01
  - PLAY-02
  - PLAY-03
  - PLAY-04
  - PLAY-05
  - UI-01
  - CONT-02
autonomous: true
---

# Plan 04: Integration & Startup Flow

**Objective:** Wire the MIDI engine to the UI shell. Connect transport buttons to PlaybackController, sliders to BPM/volume, progress bar to sequencer position. Implement startup sequence with error handling, file dialog integration, and drag-and-drop complete flow. After this plan, the application is fully functional per all Phase 1 success criteria.

## Tasks

<task id="04-01" type="execute">
<objective>Wire MIDI engine to ControlBar — transport, BPM, volume, progress</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-11 (PlaybackController full API)
- .planning/phases/01-foundation/02-PLAN.md (PlaybackController method signatures)
- src/main/java/com/rebeatbox/ui/ControlBar.java (slider/button field names)
</read_first>

<action>
Update `ControlBar.java` — add a `wireEngine(PlaybackController controller)` method:

Wire transport buttons:
- play button → `controller.play()`
- pause button → `controller.pause()`
- stop button → `controller.stop()`
- restart button → `controller.restart()`

Wire sliders:
- BPM slider change listener → `controller.setBPM(bpmSlider.getValue())`
- BPM label update: `"BPM: " + bpmSlider.getValue()`
- Volume slider change listener → `controller.setVolume(volSlider.getValue() / 100.0f)`
- Volume label update: `"Vol: " + volSlider.getValue() + "%"`

Wire progress bar:
- Create `javax.swing.Timer(100, e -> updateProgress(controller))` — polls every 100ms
- Timer action: get `controller.getMicrosecondPosition()` and `controller.getMicrosecondLength()`
- Convert to mm:ss format: `String.format("%02d:%02d / %02d:%02d", posMin, posSec, lenMin, lenSec)`
- Update progress bar value: `(int)((posMicros * 100) / lenMicros)` → percentage 0-100
- Update time label with the formatted string

Progress bar click-to-seek:
- Add MouseListener to progress bar
- On click: compute `seekMicros = (clickX / barWidth) * controller.getMicrosecondLength()`
- Call `controller.seek(seekMicros)`

Button enable states:
- Stop and restart buttons disabled when state == STOPPED and no file loaded
- Play button disabled when state == PLAYING
- Pause button disabled when state != PLAYING
- On file load: enable all transport buttons
</action>

<acceptance_criteria>
- `ControlBar.java` contains `wireEngine(PlaybackController)` method
- Play button click calls `controller.play()`
- Pause button click calls `controller.pause()`
- Stop button click calls `controller.stop()`
- Restart button click calls `controller.restart()`
- BPM slider change calls `controller.setBPM(value)` with range 20-300
- Volume slider change calls `controller.setVolume(value/100.0f)` with range 0.0-1.0
- `javax.swing.Timer` at 100ms interval updates progress bar and time label
- Time label shows "mm:ss / mm:ss" format
- Progress bar click computes seek position and calls `controller.seek()`
</acceptance_criteria>
</task>

<task id="04-02" type="execute">
<objective>Implement application startup sequence with error handling</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-15, D-17 (startup SoundFont check, exit on failure)
- .planning/phases/01-foundation/01-UI-SPEC.md § Copywriting (error messages)
</read_first>

<action>
Update `App.java` — implement startup sequence:

```java
SwingUtilities.invokeLater(() -> {
    // 1. Apply skin first (before any window creation)
    RadianceThemingCortex.GlobalScope.setSkin(new NightShadeSkin());
    
    // 2. Initialize audio engine
    Synthesizer synth = null;
    NoteEventBus eventBus = new NoteEventBus();
    PlaybackController controller = null;
    RealtimeReceiver receiver = null;
    
    try {
        SoundFontManager sfManager = new SoundFontManager();
        synth = sfManager.initialize();
    } catch (Exception e) {
        showStartupError("无法初始化音频设备",
            "请检查系统音频设置后重新启动应用\n\n" + e.getMessage());
        e.printStackTrace();
        System.exit(1);
        return;
    }
    
    try {
        controller = new PlaybackController(synth, eventBus);
        receiver = new RealtimeReceiver(synth);
    } catch (Exception e) {
        showStartupError("无法加载音色库",
            "SoundFont 文件缺失或损坏，请重新安装应用\n\n" + e.getMessage());
        e.printStackTrace();
        if (synth != null) synth.close();
        System.exit(1);
        return;
    }
    
    // 3. Create UI
    ReBeatBoxWindow window = new ReBeatBoxWindow();
    window.wireEngine(controller, receiver);
    window.setVisible(true);
});

private static void showStartupError(String title, String message) {
    // Must run on EDT since called from invokeLater
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
}
```

The `showStartupError` method shows the error dialog BEFORE System.exit(). Use JOptionPane.ERROR_MESSAGE.

The `ReBeatBoxWindow.wireEngine(PlaybackController, RealtimeReceiver)` method:
- Calls `controlBar.wireEngine(controller)`
- Stores references for drag-and-drop/file-open handlers (Task 04-03)
</action>

<acceptance_criteria>
- `App.java` initializes SoundFontManager, PlaybackController, RealtimeReceiver in order
- Startup failure shows `JOptionPane.ERROR_MESSAGE` dialog then calls `System.exit(1)`
- Error dialog for audio init failure contains text "无法初始化音频设备"
- Error dialog for SoundFont failure contains text "无法加载音色库"
- `ReBeatBoxWindow.wireEngine(PlaybackController, RealtimeReceiver)` method exists
</acceptance_criteria>
</task>

<task id="04-03" type="execute">
<objective>Implement file open dialog and drag-and-drop complete flow</objective>

<read_first>
- .planning/phases/01-foundation/01-CONTEXT.md § D-16 (corrupt file → error dialog)
- .planning/phases/01-foundation/01-UI-SPEC.md § Copywriting (error messages), Drag and Drop section
- src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java (callback stubs from Plan 03)
</read_first>

<action>
Implement the file loading callback in `ReBeatBoxWindow.java`:

```java
public void wireEngine(PlaybackController controller, RealtimeReceiver receiver) {
    this.controller = controller;
    this.receiver = receiver;
    controlBar.wireEngine(controller);
    
    // File selection callback
    onFileSelected = (file) -> loadAndPlay(file);
    onFileDropped = (file) -> loadAndPlay(file);
}

private void loadAndPlay(File file) {
    try {
        controller.load(file);
        controller.play();
    } catch (InvalidMidiDataException e) {
        JOptionPane.showMessageDialog(this,
            "文件可能已损坏或不是标准 MIDI 格式",
            "无法播放此文件",
            JOptionPane.ERROR_MESSAGE);
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this,
            "无法读取文件: " + e.getMessage(),
            "文件读取错误",
            JOptionPane.ERROR_MESSAGE);
    }
}
```

Drag-and-drop (already stubbed in Plan 03, complete the handler):
- TransferHandler.importData: extract File from Transferable, check .mid extension
- Call `onFileDropped.accept(file)`
- Drag-over: highlight window content pane border with accent color (#0f3460, 2px line border)

File open button → JFileChooser:
- Set current directory to user.home
- Filter: `new FileNameExtensionFilter("MIDI Files (*.mid)", "mid")`
- On approve: `onFileSelected.accept(selectedFile)`

After successful load:
- Window title updates to "ReBeatBox — {filename}"
</action>

<acceptance_criteria>
- Drag-and-drop of .mid file calls `controller.load(file)` and `controller.play()`
- File open dialog filters for .mid files
- Corrupt .mid file shows `JOptionPane.ERROR_MESSAGE` with text "无法播放此文件"
- Content pane has accent border highlight on drag-over
- Window title updates to include filename after successful load
- IOException shows error dialog with file-specific message
</acceptance_criteria>
</task>

<task id="04-04" type="execute">
<objective>Final integration — end-to-end test pass for all 8 success criteria</objective>

<read_first>
- .planning/ROADMAP.md § Phase 1 Success Criteria (8 items)
- .planning/phases/01-foundation/01-CONTEXT.md (all D-* decisions for verification)
</read_first>

<action>
Manual verification checklist. Launch the application and verify:

1. App launches with NightShade dark theme window ✓
2. Open a .mid file via File dialog → audio plays with correct sounds ✓
3. Pause pauses, resume resumes, stop resets to beginning ✓
4. BPM slider changes speed in real-time ✓
5. Volume slider adjusts output smoothly ✓
6. Progress bar tracks playback, dragging seeks ✓
7. Drag .mid onto window loads and plays it ✓
8. App plays 5 different .mid files without issues ✓

Fix any issues found. Pay special attention to:
- MIDI clock sync (progress bar vs audio timing)
- BPM change glitchiness (should be smooth)
- Drag-and-drop on Windows (TransferHandler quirks)
- Error dialog wording (must match UI-SPEC copywriting)

Also verify edge cases:
- Drag non-.mid file → ignored (no error)
- Open non-.mid file via dialog → error dialog
- Close window while playing → synth closes cleanly
- Minimize/restore window → playback continues
</action>

<acceptance_criteria>
- All 8 success criteria from ROADMAP.md Phase 1 are verified working
- Drag non-.mid file does not trigger error dialog (silently ignored)
- Non-.mid file via open dialog shows error dialog
- Window close during playback does not throw exceptions
- `gradlew run` launches app and plays MIDI
</acceptance_criteria>
</task>

## Verification

- [ ] App compiles: `gradlew compileJava`
- [ ] App runs: `gradlew run`
- [ ] All 8 Phase 1 success criteria verified
- [ ] No EDT thread violations (check console for Swing threading warnings)
- [ ] Error dialogs match UI-SPEC copywriting exactly
- [ ] Window title shows "ReBeatBox — {filename}" when file loaded
- [ ] SoundFont loads from classpath (test with JAR: `gradlew jar && java -jar build/libs/ReBeatBox.jar`)

## must_haves

- `truths`: ["Application launches and plays MIDI files end-to-end", "All transport controls function correctly", "BPM and volume changes apply in real-time", "Progress bar syncs with audio to within 100ms", "Drag-and-drop and file dialog both work for .mid files", "Error dialogs show correct Chinese copy from UI-SPEC", "Application recovers from invalid files without crashing", "Startup failure exits cleanly with descriptive error dialog"]
