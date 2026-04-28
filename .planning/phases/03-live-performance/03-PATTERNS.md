# Phase 03: Live Performance - Pattern Map

**Mapped:** 2026-04-28
**Files analyzed:** 9 (5 create, 4 modify)
**Analogs found:** 9 / 9 (100% coverage)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/com/rebeatbox/live/KeyboardMapper.java` | service/utility | transform (KeyEvent->note lookup) | `src/main/java/com/rebeatbox/visual/NoteColorMapper.java` | exact (static utility, range validation, lookup table) |
| `src/main/java/com/rebeatbox/live/DrumPadGrid.java` | container | request-response (mouse->MIDI) | `src/main/java/com/rebeatbox/ui/SidebarPanel.java` | role-match (JPanel container layout) |
| `src/main/java/com/rebeatbox/live/PadButton.java` | component | event-driven (press/release->NoteOn/Off) | `src/main/java/com/rebeatbox/ui/ControlBar.java` | role-match (JButton + MouseListener) |
| `src/main/java/com/rebeatbox/ui/KeyboardHintPanel.java` | component | event-driven (highlight->repaint) | `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` | exact (JPanel + custom paintComponent) |
| `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java` (MODIFY) | service | CRUD (MIDI send) | itself — existing `noteOn(note,velocity)` | self (add channel overload) |
| `src/main/java/com/rebeatbox/engine/NoteEventBus.java` (MODIFY) | middleware | event-driven (pub-sub) | itself — existing `subscribe`/`fire` pattern | self (add live event methods) |
| `src/main/java/com/rebeatbox/engine/NoteEventListener.java` (MODIFY) | interface | event-driven | itself — create new `LiveNoteEventListener` | self (new interface alongside existing) |
| `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` (MODIFY) | controller/window | request-response | itself — existing `wireEngine()` + BorderLayout | self (add SOUTH region + KFM dispatch) |
| `src/main/java/com/rebeatbox/ui/SidebarPanel.java` (MODIFY) | container | request-response | itself — existing `isExpanded()` getter | self (add content panel accessor) |

## Pattern Assignments

### 1. `src/main/java/com/rebeatbox/live/KeyboardMapper.java` (service, transform)

**Analog:** `src/main/java/com/rebeatbox/visual/NoteColorMapper.java` (static utility pattern)

**Class structure pattern** (NoteColorMapper.java lines 15-59):
```java
package com.rebeatbox.live;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * KeyCode-to-MIDI-note lookup table for three-row keyboard mapping.
 * Covers C3-C6 across number row, QWERTY row, and bottom row.
 * Also tracks active note state for OS auto-repeat deduplication.
 */
public final class KeyboardMapper {

    // --- Three-row key mapping (D-01) ---
    private static final Map<Integer, Integer> KEY_TO_NOTE = new HashMap<>();
    // ... populate in static block ...

    // --- Deduplication state ---
    private final boolean[] activeNotes = new boolean[128];

    private KeyboardMapper() {
        // Utility class -- no instantiation
    }

    /**
     * Looks up MIDI note for a KeyEvent keyCode.
     * Returns -1 (sentinel) if key is not mapped.
     */
    public static int keyCodeToNote(int keyCode) { ... }

    public boolean isActive(int note) { return activeNotes[note]; }
    public void setActive(int note, boolean active) { activeNotes[note] = active; }
    public void clearAll() { /* reset all to false */ }
}
```

**Key patterns to copy:**

a) **Utility class package + imports** (NoteColorMapper.java lines 1-3):
```java
package com.rebeatbox.live;

import java.awt.event.KeyEvent;
import java.util.Map;
```

b) **final class + private constructor** (NoteColorMapper.java lines 15, 33-35):
```java
public final class KeyboardMapper {
    private KeyboardMapper() {
        // Utility class -- no instantiation
    }
```

c) **Range validation / sentinel return** (NoteColorMapper.java lines 48-52):
```java
public static int keyCodeToNote(int keyCode) {
    Integer note = KEY_TO_NOTE.get(keyCode);
    return note != null ? note : -1; // sentinel for unmapped keys
}
```

d) **boolean[] activeNote tracking** (PlaybackController.java line 23):
```java
private final boolean[] activeNotes = new boolean[128];
```

e) **D-01 mapping table** from CONTEXT.md -- concrete data to populate `KEY_TO_NOTE`:
```
// Upper octave (number row): C5-C6
// VK_1->C5(72), VK_2->C#5(73), VK_3->D5(74), VK_4->D#5(75), VK_5->E5(76),
// VK_6->F5(77), VK_7->F#5(78), VK_8->G5(79), VK_9->G#5(80), VK_0->A5(81),
// VK_MINUS->A#5(82), VK_EQUALS->B5(83), VK_BACK_SPACE->C6(84)

// Middle octave (QWERTY row): C4-C5
// VK_Q->C4(60), VK_W->C#4(61), VK_E->D4(62), VK_R->D#4(63), VK_T->E4(64),
// VK_Y->F4(65), VK_U->F#4(66), VK_I->G4(67), VK_O->G#4(68), VK_P->A4(69),
// VK_OPEN_BRACKET->A#4(70), VK_CLOSE_BRACKET->B4(71), VK_BACK_SLASH->C5(72)

// Lower octave (bottom row): C3-C4
// VK_Z->C3(48), VK_X->C#3(49), VK_C->D3(50), VK_V->D#3(51), VK_B->E3(52),
// VK_N->F3(53), VK_M->F#3(54), VK_COMMA->G3(55), VK_PERIOD->G#3(56),
// VK_SLASH->A3(57), VK_SHIFT(R) -> A#3(58), VK_SHIFT(L) -> B3(59)
```

---

### 2. `src/main/java/com/rebeatbox/live/DrumPadGrid.java` (container, request-response)

**Analog:** `src/main/java/com/rebeatbox/ui/SidebarPanel.java` (JPanel container layout pattern)

**Imports pattern** (SidebarPanel.java lines 1-5):
```java
package com.rebeatbox.live;

import javax.swing.*;
import java.awt.*;
```

**JPanel constructor + layout** (SidebarPanel.java lines 14-16):
```java
public class DrumPadGrid extends JPanel {
    public DrumPadGrid() {
        setLayout(new GridLayout(4, 4, 4, 4)); // 4x4 with 4px gap
        setBackground(new Color(0x16213e));
```

**Background color pattern** (SidebarPanel.java line 16):
```java
setBackground(new Color(0x16213e));
```

**Child component creation loop** (ControlBar.java lines 26-29 for button creation, applied to 16 pads):
```java
for (int row = 0; row < 4; row++) {
    for (int col = 0; col < 4; col++) {
        int index = row * 4 + col;
        int defaultNote = DEFAULT_GM_NOTES[index];
        PadButton pad = new PadButton(defaultNote, receiver);
        pad.setPreferredSize(new Dimension(54, 54));
        add(pad);
    }
}
```

**GM drum defaults (D-05)** to embed in the class:
```
// GM Percussion defaults (MIDI Channel 10)
private static final int[] DEFAULT_GM_NOTES = {
    36, // Kick (C1)
    38, // Snare (D1)
    42, // Closed Hat (F#1)
    46, // Open Hat (A#1)
    39, // Clap (D#1)
    49, // Crash Cymbal (C#2)
    41, // Low Tom (F1)
    43, // High Tom (G1)
    45, // Low Tom (A1)
    47, // Mid Tom (B1)
    48, // High Tom (C2)
    51, // Ride Cymbal (D#2)
    56, // Cowbell (G#2)
    54, // Tambourine (F#2)
    37, // Side Stick (C#1)
    40, // Electric Snare (E1)
};
```

---

### 3. `src/main/java/com/rebeatbox/live/PadButton.java` (component, event-driven)

**Analog:** `src/main/java/com/rebeatbox/ui/ControlBar.java` (JButton creation) + `PlaybackController.java` (MIDI message sending)

**JButton creation pattern** (ControlBar.java lines 195-201):
```java
package com.rebeatbox.live;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;

public class PadButton extends JButton {
    private int midiNote;
    private final RealtimeReceiver receiver;
    private Timer holdTimer; // 200ms visual hold
    private boolean pressedVisual = false;

    public PadButton(int midiNote, RealtimeReceiver receiver) {
        this.midiNote = midiNote;
        this.receiver = receiver;
        setFocusable(false);
        // Cyberpunk square shape
        setBackground(new Color(0x1a1a2e));
        setBorder(BorderFactory.createLineBorder(new Color(0x3a3a5e), 2));
        setPreferredSize(new Dimension(54, 54));
        // ...
    }
```

**MouseListener pattern** (ControlBar.java lines 97-103, progress bar click; adapted for drum pad press/release):
```java
addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            sendNoteOn();
            setPressedVisual(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            sendNoteOff();
            holdTimer.restart(); // 200ms then restore visual
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            showAssignMenu(e.getComponent(), e.getX(), e.getY());
        }
    }
});
```

**JPopupMenu for right-click pad assignment (D-06)**:
```java
private void showAssignMenu(Component invoker, int x, int y) {
    JPopupMenu menu = new JPopupMenu("Assign Sound");
    // GM percussion instrument submenus grouped by category
    JMenu kicksMenu = new JMenu("Kicks");
    // ... add JMenuItem for each GM note, on click: this.midiNote = note;
    menu.show(invoker, x, y);
}
```

**MIDI send pattern** (RealtimeReceiver.java lines 12-19, adapted for channel 10):
```java
private void sendNoteOn() {
    receiver.noteOn(midiNote, 100, 10); // channel 10, velocity 100
}

private void sendNoteOff() {
    receiver.noteOff(midiNote, 10);
}
```

**Timer for visual hold** (ControlBar.java lines 74-76, adapted):
```java
holdTimer = new Timer(200, e -> {
    setPressedVisual(false);
    repaint();
});
holdTimer.setRepeats(false); // one-shot
```

---

### 4. `src/main/java/com/rebeatbox/ui/KeyboardHintPanel.java` (component, event-driven)

**Analog:** `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` (custom paintComponent, multi-layer drawing, coordinate mapping, neon colors) + `src/main/java/com/rebeatbox/ui/PlaceholderPanel.java` (simpler custom paint as starter)

**Imports pattern** (PianoRollPanel.java lines 1-14):
```java
package com.rebeatbox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
```

**JPanel constructor pattern** (PianoRollPanel.java lines 107-109):
```java
public class KeyboardHintPanel extends JPanel {
    // Highlight state: keyCode -> isHighlighted
    private final Map<Integer, Boolean> keyHighlights = new HashMap<>();

    public KeyboardHintPanel() {
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(800, 100)); // 3-row keyboard height
    }
```

**paintComponent override pattern** (PianoRollPanel.java lines 284-304, simplified for static keyboard):
```java
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;

    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    // Background
    g2d.setColor(new Color(0x0a0a14));
    g2d.fillRect(0, 0, w, h);

    // Draw three rows of keys
    drawKeyRow(g2d, UPPER_ROW_KEYS, 0);    // number row
    drawKeyRow(g2d, MIDDLE_ROW_KEYS, 1);   // QWERTY row
    drawKeyRow(g2d, LOWER_ROW_KEYS, 2);    // bottom row
}
```

**Key color fill pattern -- neon highlight** (PianoRollPanel.java lines 527-528 for base fill, 527 for dark key color):
```java
// Default key (dark)
g2d.setColor(new Color(0x2a2a2a));
g2d.fillRect(keyX, keyY, keyW, keyH);

// Highlighted key (neon glow, per D-08)
if (isHighlighted(keyCode)) {
    g2d.setColor(new Color(0x00d4ff)); // neon cyan
    g2d.setStroke(new BasicStroke(2.0f));
    g2d.drawRect(keyX + 1, keyY + 1, keyW - 2, keyH - 2);
    // Subtle inner glow fill
    g2d.setColor(new Color(0x00d4ff, 60));
    g2d.fillRect(keyX, keyY, keyW, keyH);
}
```

**Public highlight API** to be called from the KeyEventDispatcher:
```java
public void setKeyHighlighted(int keyCode, boolean highlighted) {
    keyHighlights.put(keyCode, highlighted);
    repaint();
}

public void clearAllHighlights() {
    keyHighlights.clear();
    repaint();
}
```

**Color palette** (from existing project conventions):
- Background: `new Color(0x0a0a14)` -- deeper dark than PianoRollPanel but same family
- White key fill: `new Color(0x2a2a2a)` (PianoRollPanel line 527)
- Black key fill: `new Color(0x1a1a1a)` (PianoRollPanel line 553)
- Neon highlight: `new Color(0x00d4ff)` -- cyan, follows PianoRollPanel's cyan trigger line color (line 666 uses `new Color(200, 255, 255, ...)`)
- Key label text: `new Color(0xe0e0e0)` (ControlBar line 39)
- Key border: `new Color(0x3a3a3a)` (PianoRollPanel line 531)

---

### 5. `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java` (MODIFY -- add channel overload)

**Analog:** itself -- existing `noteOn(int, int)` and `noteOff(int)` methods

**Existing pattern to duplicate** (RealtimeReceiver.java lines 12-27):
```java
// EXISTING -- keep unchanged (backward compat, channel 0)
public void noteOn(int noteNumber, int velocity) {
    try {
        ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, 0, noteNumber, velocity);
        receiver.send(msg, -1);
    } catch (InvalidMidiDataException e) {
        System.err.println("Invalid MIDI noteOn: note=" + noteNumber + " velocity=" + velocity);
    }
}

// NEW CHANNEL OVERLOAD -- same pattern, added channel parameter
public void noteOn(int noteNumber, int velocity, int channel) {
    try {
        ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
        receiver.send(msg, -1);
    } catch (InvalidMidiDataException e) {
        System.err.println("Invalid MIDI noteOn: note=" + noteNumber + " velocity=" + velocity + " channel=" + channel);
    }
}

// NEW CHANNEL OVERLOAD -- same pattern for noteOff
public void noteOff(int noteNumber, int channel) {
    try {
        ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, noteNumber, 0);
        receiver.send(msg, -1);
    } catch (InvalidMidiDataException e) {
        System.err.println("Invalid MIDI noteOff: note=" + noteNumber + " channel=" + channel);
    }
}
```

**Error handling pattern** (RealtimeReceiver.java lines 16-18):
```java
} catch (InvalidMidiDataException e) {
    System.err.println("Invalid MIDI noteOn: note=" + noteNumber + " velocity=" + velocity);
}
```
Print to `System.err` with key parameters -- no stack trace, no rethrow (MIDI failures are non-fatal in this app).

---

### 6. `src/main/java/com/rebeatbox/engine/NoteEventBus.java` (MODIFY -- add live events)

**Analog:** itself -- existing `subscribe`/`unsubscribe`/`fire` pattern with CopyOnWriteArrayList

**Existing pattern to duplicate** (NoteEventBus.java lines 1-24):
```java
package com.rebeatbox.engine;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class NoteEventBus {
    // EXISTING -- keep unchanged
    private final List<NoteEventListener> listeners = new CopyOnWriteArrayList<>();
    // ...

    // ===== NEW: Live note event support (Phase 3) =====

    private final List<LiveNoteEventListener> liveListeners = new CopyOnWriteArrayList<>();

    public void subscribeLive(LiveNoteEventListener listener) {
        liveListeners.add(listener);
    }

    public void unsubscribeLive(LiveNoteEventListener listener) {
        liveListeners.remove(listener);
    }

    public void fireLiveNoteOn(int note, int velocity) {
        for (LiveNoteEventListener listener : liveListeners) {
            SwingUtilities.invokeLater(() -> listener.onLiveNoteOn(note, velocity));
        }
    }

    public void fireLiveNoteOff(int note) {
        for (LiveNoteEventListener listener : liveListeners) {
            SwingUtilities.invokeLater(() -> listener.onLiveNoteOff(note));
        }
    }
}
```

**SwingUtilities.invokeLater pattern** copied from existing `fire()` method (NoteEventBus.java line 21):
```java
SwingUtilities.invokeLater(() -> listener.onActiveNotesChanged(activeNotes));
```
Ensures listener callbacks execute on EDT -- apply same pattern to live events.

---

### 7. `src/main/java/com/rebeatbox/engine/NoteEventListener.java` (MODIFY -- create new `LiveNoteEventListener`)

**Analog:** `src/main/java/com/rebeatbox/engine/NoteEventListener.java` (existing @FunctionalInterface pattern)

**Existing pattern** (NoteEventListener.java lines 1-8):
```java
package com.rebeatbox.engine;

import java.util.Set;

@FunctionalInterface
public interface NoteEventListener {
    void onActiveNotesChanged(Set<Integer> activeNoteNumbers);
}
```

**New interface** -- create as separate file `LiveNoteEventListener.java` in same package (per RESEARCH.md Pitfall 4 avoidance -- keep @FunctionalInterface on NoteEventListener):
```java
package com.rebeatbox.engine;

/**
 * Listener for real-time live performance note events (Phase 3).
 * Called on the EDT via SwingUtilities.invokeLater.
 */
public interface LiveNoteEventListener {
    /**
     * Fired when a live note-on event occurs (keyboard press or drum pad press).
     * @param note MIDI note number (0-127)
     * @param velocity MIDI velocity (0-127)
     */
    void onLiveNoteOn(int note, int velocity);

    /**
     * Fired when a live note-off event occurs (keyboard release or drum pad release).
     * @param note MIDI note number (0-127)
     */
    void onLiveNoteOff(int note);
}
```

**Key decisions:**
- NOT `@FunctionalInterface` -- has two abstract methods by design (NoteOn + NoteOff always paired)
- Keep in `engine` package alongside `NoteEventListener`
- Follow the same Javadoc conventions as existing interfaces

---

### 8. `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` (MODIFY -- SOUTH + KFM + wire live)

**Analog:** itself -- existing `wireEngine()` method, BorderLayout region management, listener registration

**Existing layout registration pattern** (ReBeatBoxWindow.java lines 33-41):
```java
setLayout(new BorderLayout());

controlBar = new ControlBar();
pianoRollPanel = new PianoRollPanel();
sidebarPanel = new SidebarPanel();

add(controlBar, BorderLayout.NORTH);
add(pianoRollPanel, BorderLayout.CENTER);
add(sidebarPanel, BorderLayout.EAST);
```

**NEW -- add SOUTH region + live component fields:**
```java
// New Phase 3 fields
private KeyboardMapper keyboardMapper;
private KeyboardHintPanel keyboardHintPanel;
private NoteEventBus eventBus;  // needing to pass eventBus -- see wireEngine signature

// In constructor, after existing add() calls:
keyboardHintPanel = new KeyboardHintPanel();
add(keyboardHintPanel, BorderLayout.SOUTH);
```

**Existing `wireEngine` field storage pattern** (ReBeatBoxWindow.java lines 55-68):
```java
public void wireEngine(PlaybackController controller, RealtimeReceiver receiver) {
    this.controller = controller;
    this.receiver = receiver;
    controlBar.wireEngine(controller);
    pianoRollPanel.setController(controller);
    // ...
```

**NEW -- extend wireEngine (needs NoteEventBus parameter, or get from controller):**
```java
public void wireEngine(PlaybackController controller, RealtimeReceiver receiver, NoteEventBus eventBus) {
    this.controller = controller;
    this.receiver = receiver;
    this.eventBus = eventBus;
    this.keyboardMapper = new KeyboardMapper();

    // Wire existing
    controlBar.wireEngine(controller);
    pianoRollPanel.setController(controller);

    // NEW: Wire drum pad grid into sidebar
    DrumPadGrid drumPadGrid = new DrumPadGrid(receiver);
    sidebarPanel.getContentPanel().add(drumPadGrid, BorderLayout.CENTER);

    // NEW: Register keyboard dispatcher
    registerKeyboardDispatcher();

    // NEW: Window focus loss handler (stuck-note prevention)
    registerFocusLossHandler();

    // File open callback (existing, unchanged)
    controlBar.setOnFileOpen(chooser -> { ... });
}
```

**registerKeyboardDispatcher pattern** (from RESEARCH.md verified pattern):
```java
private void registerKeyboardDispatcher() {
    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    kfm.addKeyEventDispatcher(e -> {
        // Step 1: Only KEY_PRESSED and KEY_RELEASED
        int id = e.getID();
        if (id != KeyEvent.KEY_PRESSED && id != KeyEvent.KEY_RELEASED) return false;

        // Step 2: Respect consumed events
        if (e.isConsumed()) return false;

        // Step 3: Text component focus guard
        Component owner = kfm.getFocusOwner();
        if (owner instanceof javax.swing.text.JTextComponent) return false;

        // Step 4: Look up MIDI note
        int note = KeyboardMapper.keyCodeToNote(e.getKeyCode());
        if (note < 0) return false;

        // Step 5: Deduplicate + send MIDI + fire events
        boolean pressed = (id == KeyEvent.KEY_PRESSED);
        if (pressed && !keyboardMapper.isActive(note)) {
            receiver.noteOn(note, 100);
            eventBus.fireLiveNoteOn(note, 100);
            keyboardMapper.setActive(note, true);
            keyboardHintPanel.setKeyHighlighted(e.getKeyCode(), true);
        } else if (!pressed && keyboardMapper.isActive(note)) {
            receiver.noteOff(note);
            eventBus.fireLiveNoteOff(note);
            keyboardMapper.setActive(note, false);
            keyboardHintPanel.setKeyHighlighted(e.getKeyCode(), false);
        }
        return false; // don't consume
    });
}
```

**registerFocusLossHandler pattern** (from RESEARCH.md verified pattern + WindowAdapter precedent at Window.java lines 47-52):
```java
private void registerFocusLossHandler() {
    addWindowFocusListener(new WindowAdapter() {
        @Override
        public void windowLostFocus(WindowEvent e) {
            for (int note = 0; note < 128; note++) {
                if (keyboardMapper.isActive(note)) {
                    receiver.noteOff(note);
                    eventBus.fireLiveNoteOff(note);
                    keyboardMapper.setActive(note, false);
                }
            }
            keyboardHintPanel.clearAllHighlights();
        }
    });
}
```

**Existing WindowAdapter pattern** (ReBeatBoxWindow.java lines 47-52):
```java
addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        if (pianoRollPanel != null) pianoRollPanel.dispose();
    }
});
```

**IMPORTANT:** `wireEngine()` signature change from `(PlaybackController, RealtimeReceiver)` to `(PlaybackController, RealtimeReceiver, NoteEventBus)` -- requires corresponding update in `App.java` line 55:
```java
// BEFORE: window.wireEngine(controller, receiver);
// AFTER:  window.wireEngine(controller, receiver, eventBus);
```

---

### 9. `src/main/java/com/rebeatbox/ui/SidebarPanel.java` (MODIFY -- expose contentPanel)

**Analog:** itself -- existing public getter pattern `isExpanded()`

**Existing getter pattern** (SidebarPanel.java lines 49-51):
```java
public boolean isExpanded() {
    return expanded;
}
```

**New getter to add:**
```java
/**
 * Returns the content panel reserved for Phase 3 drum pad grid.
 * External callers add their components to this panel.
 */
public JPanel getContentPanel() {
    return contentPanel;
}
```

**Field modification:** change `contentPanel` from `private` to `private final` (it already has final from constructor usage, but explicit final reinforces immutability of the reference).

---

## Shared Patterns

### Authentication / Guards
Not applicable. Phase 3 has no auth layer (desktop app, single-user). Keyboard dispatch uses text-component focus guard (`focusOwner instanceof JTextComponent`) as the only access control.

### Error Handling (MIDI)
**Source:** `src/main/java/com/rebeatbox/engine/RealtimeReceiver.java` lines 16-18
**Apply to:** All MIDI send call sites (KeyboardDispatcher in Window, PadButton, RealtimeReceiver overloads)
```java
try {
    ShortMessage msg = new ShortMessage(ShortMessage.NOTE_ON, channel, note, velocity);
    receiver.send(msg, -1);
} catch (InvalidMidiDataException e) {
    System.err.println("Invalid MIDI noteOn: note=" + note + " velocity=" + velocity + " channel=" + channel);
}
```
Pattern: catch `InvalidMidiDataException`, print to `System.err`, no rethrow. MIDI errors are non-fatal.

### Error Handling (UI)
**Source:** `src/main/java/com/rebeatbox/ui/ReBeatBoxWindow.java` lines 106-116
**Apply to:** File load failures, any user-facing error dialogs
```java
JOptionPane.showMessageDialog(this,
    "Descriptive error message.",
    "Error Title",
    JOptionPane.ERROR_MESSAGE);
```

### Event Bus (Publish-Subscribe)
**Source:** `src/main/java/com/rebeatbox/engine/NoteEventBus.java` lines 1-24
**Apply to:** All event bus extensions (live events)
```java
private final List<LiveNoteEventListener> liveListeners = new CopyOnWriteArrayList<>();

public void subscribeLive(LiveNoteEventListener listener) { liveListeners.add(listener); }
public void unsubscribeLive(LiveNoteEventListener listener) { liveListeners.remove(listener); }

public void fireLiveNoteOn(int note, int velocity) {
    for (LiveNoteEventListener listener : liveListeners) {
        SwingUtilities.invokeLater(() -> listener.onLiveNoteOn(note, velocity));
    }
}
```
Key rules: `CopyOnWriteArrayList` for thread safety, `SwingUtilities.invokeLater` for EDT safety.

### Custom paintComponent
**Source:** `src/main/java/com/rebeatbox/visual/PianoRollPanel.java` lines 284-304
**Apply to:** KeyboardHintPanel
```java
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    // ... custom drawing ...
}
```
Key rules: Always call `super.paintComponent(g)` first. Cast to `Graphics2D` for access to advanced rendering. Use `setRenderingHint` for antialiasing when drawing curved elements.

### Swing Timer (One-shot / Polling)
**Source:** `src/main/java/com/rebeatbox/ui/ControlBar.java` lines 74-77 (polling) + RESEARCH.md (one-shot)
**Apply to:** PadButton (one-shot 200ms visual hold), ControlBar (100ms state sync -- already exists)
```java
// Polling timer (ControlBar)
stateTimer = new Timer(100, e -> syncButtonStates());

// One-shot timer (PadButton)
holdTimer = new Timer(200, e -> { setPressedVisual(false); repaint(); });
holdTimer.setRepeats(false);
```

### Text Component Focus Guard
**Source:** RESEARCH.md Pitfall 2
**Apply to:** KeyboardFocusManager dispatcher in ReBeatBoxWindow
```java
Component focusOwner = kfm.getFocusOwner();
if (focusOwner instanceof javax.swing.text.JTextComponent) {
    return false; // Don't intercept text input
}
```

---

## No Analog Found

None. All 9 files have strong analogs in the existing codebase. This is a greenfield phase (no runtime state migration), but the architectural patterns are already well established.

| File | Role | Analog Coverage |
|------|------|-----------------|
| (none) | -- | All files have matches |

---

## Metadata

**Analog search scope:** `src/main/java/com/rebeatbox/` (all 15 source files scanned)
**Files scanned for analogs:** 15 source + 3 test files = 18 total
**Pattern extraction date:** 2026-04-28
**Test infrastructure:** JUnit Jupiter 5.10.0 via `./gradlew test`
**New package required:** `src/main/java/com/rebeatbox/live/` (create directory)
**New test files required:** `src/test/java/com/rebeatbox/live/KeyboardMapperTest.java`, `DrumPadGridTest.java`, `PadButtonTest.java`
**New test files required:** `src/test/java/com/rebeatbox/engine/NoteEventBusLiveTest.java`, `RealtimeReceiverChannelTest.java`

**App.java change needed:** `wireEngine(controller, receiver)` -> `wireEngine(controller, receiver, eventBus)` at App.java line 55.
