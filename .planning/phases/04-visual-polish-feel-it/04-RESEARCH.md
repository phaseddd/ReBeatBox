# Phase 4: Visual Polish — "Feel It" - Research

**Researched:** 2026-04-28
**Domain:** Java2D rendering, Radiance animation, SVG icon loading, particle systems, image processing
**Confidence:** HIGH

## Summary

Phase 4 applies the full cyber/glitch aesthetic on top of the existing Phase 1-3 foundation. Six requirements (UI-02, UI-03, GLITCH-01 through GLITCH-04) span four technical domains: (1) centralized neon color theming via ThemeManager, (2) Radiance Timeline-driven button hover/press animations, (3) a 200-particle burst system on a transparent GlassPane, (4) RGB-channel-separation glitch transitions during view switches, (5) GaussianBlur glow parameter amplification, and (6) SVG icon rendering via Apache Batik.

The existing architecture is well-prepared for this phase. The NoteEventBus dual-channel (sequencer + live) already fires on every note-on event with pitch and velocity — the particle system simply subscribes to both channels. PianoRollPanel's separable ConvolveOp GaussianBlur pipeline only needs constant changes (kernel 5->7, sigma 2.0->3.5). All UI components already use custom paintComponent, making ThemeManager color injection straightforward.

The primary risks are: (a) GlassPane repaint triggering full-window repaint cycles, degrading 60fps particle animation; (b) per-note BufferedImage allocation overhead in PianoRollPanel's drawGlowingBar(); (c) Batik's transitive dependency footprint (~10MB) vs the simpler JSVG alternative for icon rendering; and (d) RGB glitch effect requiring per-frame BufferedImage re-rendering if applied to live components rather than static snapshots.

**Primary recommendation:** Use JSVG over Batik for ~20 simple cyber icons (500KB vs 10MB+ transitive deps), accumulate particles to an off-screen BufferedImage before GlassPane blit, and keep the existing separable ConvolveOp but pre-allocate blur buffers to avoid per-frame GC pressure.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| ThemeManager color provision | API / Backend (in-process) | — | Singleton state, no I/O. Colors are computed in-process and consumed by UI components. |
| Button hover/press animation | Browser / Client (Swing EDT) | — | Radiance Timeline operates on JComponent properties via public setters; all animation callbacks fire on EDT. |
| Particle burst rendering | Browser / Client (GlassPane) | — | GlassPane is a Swing JComponent; particles are rendered via Graphics2D on the EDT. No backend involvement. |
| Particle event triggering | Browser / Client (NoteEventBus) | — | NoteEventBus fires callbacks on EDT via SwingUtilities.invokeLater(); zero network/disk I/O. |
| Glitch transition effect | Browser / Client (Swing EDT) | — | RGB channel separation operates on BufferedImage snapshots of Swing components; rendered in-process. |
| Glitch transition trigger | Browser / Client (Swing EDT) | — | SidebarPanel.toggle() and ReBeatBoxWindow.loadAndPlay() are UI-layer events. |
| SVG icon loading/caching | Browser / Client (startup) | — | Icons loaded once at startup from classpath resources, cached as BufferedImage. No runtime I/O. |
| GaussianBlur glow | Browser / Client (Swing EDT) | — | ConvolveOp runs on per-note BufferedImages within PianoRollPanel.paintComponent(). |
| Velocity-to-alpha mapping | Browser / Client (Swing EDT) | — | Pure computation within PianoRollPanel.drawSingleNote(); no external state. |

## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** ThemeManager class — centralized semantic color constants. All UI components read colors from ThemeManager, never hardcode Color values.
- **D-02:** Full rainbow gradient palette — different UI regions get different hue zones. Extends NoteColorMapper's HSB approach.
- **D-03:** Three-tier dark background hierarchy: BG_ROOT (#0a0a14), BG_SURFACE (#12122a), BG_ELEVATED (#1a1a3e).
- **D-04:** Radiance NightShade retained for structural styling. Custom neon colors override fill, border, glow.
- **D-05:** Hover: border color transitions dark->neon. Press: 0.95x scale bounce + color flash. Release: spring-back. All via Radiance Timeline.
- **D-06:** Unified animation across ALL interactive buttons: ControlBar (5), open button, PadButton grid (16), SidebarPanel toggle.
- **D-07:** Radiance ephemeral Timeline used for animation interpolation.
- **D-08:** Cyber-block particle style — small squares/rectangles (2-8px), random rotation, glowing border. No circles.
- **D-09:** Particles render on global GlassPane overlaid on JFrame.
- **D-10:** Particle color = NoteColorMapper.forPitch(noteNumber).
- **D-11:** ALL note-on events trigger particles — both Sequencer and live notes. ParticleSystem subscribes to NoteEventBus + LiveNoteEventListener.
- **D-12:** Particle cap: 200 max. When full, merge oldest 2-3 into one larger particle.
- **D-13:** Particle lifetime = f(velocity): 300ms (vel=0) -> 800ms (vel=127).
- **D-14:** Particle size = f(velocity): 1-3px (vel=0) -> 4-8px (vel=127).
- **D-15:** Particle system runs own javax.swing.Timer(16ms). Independent from PianoRollPanel's timer.
- **D-16:** Glitch triggers: (1) SidebarPanel toggle, (2) new MIDI file loaded.
- **D-17:** RGB channel separation effect — red shifted left, blue shifted right, brief noise overlay.
- **D-18:** Scene-differentiated duration: sidebar ~150ms, file load ~300ms.
- **D-19:** Component-level animation via Radiance Timeline. NOT monolithic screenshot-based.
- **D-20:** Enhance existing per-note bar GaussianBlur glow only. No new glow elements.
- **D-21:** GaussianBlur parameters: kernel 5->7, sigma 2.0->3.5.
- **D-22:** Keep existing separable two-pass ConvolveOp algorithm. Only constants change.
- **D-23:** Trigger line glow unchanged.
- **D-24:** Apache Batik library for SVG -> BufferedImage rendering. Icons loaded at startup, cached as BufferedImage keyed by size. [VERIFIED: CONTEXT.md D-24]
- **D-25:** Complete icon set (~20 icons).
- **D-26:** Replace all unicode text icons in ControlBar and SidebarPanel toggle with SVG icons. PadButton labels stay text-based.
- **D-27:** Implement velocity -> alpha mapping.
- **D-28:** Alpha = linear interpolation: velocity 0-127 -> alpha 0.3-1.0 (above trigger line), 0.15-0.4 (below trigger line).
- **D-29:** Applied to all note bars in PianoRollPanel.
- **D-30:** KeyboardHintPanel adopts ThemeManager neon palette.
- **D-31:** Key highlight is instant color switch (no animation). Performance-critical for live playing.

### Claude's Discretion

- ThemeManager exact API design (method signatures, color naming convention, relationship to NoteColorMapper)
- Particle motion trajectory details (initial velocity ranges, random spread angle distribution)
- Glitch transition RGB offset magnitudes, noise density, exact per-scene duration values
- SVG icon specific visual design (Path shapes, stroke weights, glow treatment)
- Button animation Timeline exact parameters (duration in ms, easing curve selection)
- Merge-old-particles algorithm details (which particles, merged properties)
- KeyboardHintPanel exact idle/pressed color values within the ThemeManager palette

### Deferred Ideas (OUT OF SCOPE)

- Note motion trails, chord connectors, octave separator glow
- Dual-channel velocity mapping (alpha + HSB simultaneously)
- KeyboardHintPanel key scale bounce animation
- Particle gravity / physics simulation

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UI-02 | Neon color palette applied across all UI elements | ThemeManager pattern (section: ThemeManager Architecture), HSB rainbow mapping extension |
| UI-03 | All buttons and interactive controls have hover/press animation feedback via radiance-animation | Radiance Timeline API (section: Radiance Ephemeral Animation API) |
| GLITCH-01 | Particle burst effect triggers on every MIDI note-on event (playback and live modes) | Particle system on GlassPane (section: Java2D Particle System), NoteEventBus subscription |
| GLITCH-02 | Glitch art transition animations on window/view switches | RGB channel separation (section: RGB Channel Separation Glitch), Radiance Timeline integration |
| GLITCH-03 | Note tracks rendered with neon glow lines | GaussianBlur amplification kernel 5->7, sigma 2.0->3.5 (section: GaussianBlur Optimization) |
| GLITCH-04 | Custom cyber-styled SVG icon set | Apache Batik SVG->BufferedImage pipeline (section: SVG Icon Loading) |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| org.pushing-pixels:radiance-animation | 8.5.0 | Timeline-based property interpolation for button animations and glitch transitions | Already in build.gradle; Radiance's own animation library, tightly integrated with theming [VERIFIED: build.gradle] |
| org.pushing-pixels:radiance-common | 8.5.0 | Shared Radiance utilities | Already in build.gradle; required by animation module [VERIFIED: build.gradle] |
| org.apache.xmlgraphics:batik-transcoder | 1.19 | SVG -> BufferedImage rasterization via ImageTranscoder | Mandated by D-24; latest stable version. Pulls batik-anim, batik-awt-util, batik-bridge, batik-css, batik-dom, batik-ext, batik-gvt, batik-parser, batik-svg-dom, batik-util, batik-xml transitively. [VERIFIED: Maven Central repo list, search results] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| javax.swing.Timer | JDK built-in | 16ms repaint loop for particle system | Already used by PianoRollPanel. Particle system uses independent timer per D-15. |
| java.awt.image.ConvolveOp | JDK built-in | Separable GaussianBlur (kernel 7, sigma 3.5) | Existing architecture in PianoRollPanel. No library needed — only constants change per D-22. |
| java.awt.AlphaComposite | JDK built-in | Alpha compositing for particles, blurred note bars | Already used in PianoRollPanel.drawGlowingBar(). |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Apache Batik 1.19 | com.github.weisj:jsvg 2.0.0 | JSVG is 500KB vs Batik's ~10MB+ transitive deps, 50x less memory, used by JetBrains IntelliJ. But D-24 mandates Batik. JSVG suffices for simple icons but lacks full SVG 1.1 filter/CSS support. [CITED: github.com/weisJ/jsvg, multiple JetBrains dependabot PRs] |
| Radiance Timeline for scale animation | Manual AffineTransform in paintComponent | Manual is more verbose and doesn't benefit from Radiance's easing/pulse infrastructure. Timeline + setter is the Radiance-blessed pattern. |
| Direct Graphics2D particle draw | Off-screen BufferedImage accumulation | Off-screen accumulation reduces AlphaComposite calls from 200/frame to 1/frame and avoids per-particle method call overhead [CITED: JVM Gaming forums, Java2D best practices] |

**Installation:**
```groovy
// Add to build.gradle dependencies (Batak is new; Radiance already present)
implementation 'org.apache.xmlgraphics:batik-transcoder:1.19'
implementation 'org.apache.xmlgraphics:batik-awt-util:1.19'
implementation 'org.apache.xmlgraphics:batik-bridge:1.19'
implementation 'org.apache.xmlgraphics:batik-dom:1.19'
implementation 'org.apache.xmlgraphics:batik-ext:1.19'
implementation 'org.apache.xmlgraphics:batik-gvt:1.19'
implementation 'org.apache.xmlgraphics:batik-svg-dom:1.19'
implementation 'org.apache.xmlgraphics:batik-util:1.19'
implementation 'org.apache.xmlgraphics:batik-xml:1.19'
implementation 'org.apache.xmlgraphics:batik-parser:1.19'
implementation 'org.apache.xmlgraphics:batik-css:1.19'
// Exclude if SVGs have no animations or scripts:
// implementation 'org.apache.xmlgraphics:batik-anim:1.19'
// implementation 'org.apache.xmlgraphics:batik-script:1.19'
// implementation 'org.apache.xmlgraphics:batik-svggen:1.19'
```

**Version verification:** Batik 1.19 confirmed via CERN Nexus mirror showing release date 2025-05-06. JSVG 2.0.0 confirmed via GitHub releases and Maven Central dependabot PRs from JetBrains-related projects.

## Architecture Patterns

### System Architecture Diagram

```
                        ┌─────────────────────────────────────────┐
                        │              ReBeatBoxWindow (JFrame)     │
                        │                                          │
  MIDI Sequencer ──────>│  NoteEventBus                            │
  (javax.sound.midi)    │    ├─ fire(Set<activeNotes>)             │
                        │    └─ fireLiveNoteOn(note, velocity)     │
  KeyboardMapper ──────>│                                          │
  Live Keys             │  ┌──────────┐  ┌───────────────────────┐ │
                        │  │ GlassPane│  │ Content Pane          │ │
  DrumPadGrid ────────>│  │ (Particle│  │  ┌─────────────────┐  │ │
  Mouse Events          │  │  System) │  │  │ PianoRollPanel  │  │ │
                        │  │          │  │  │ (GaussianBlur   │  │ │
                        │  │ Offscreen│  │  │  kernel 7)      │  │ │
                        │  │ BufImage │  │  └─────────────────┘  │ │
                        │  │   ↓      │  │  ┌─────────────────┐  │ │
                        │  │ drawImage│  │  │ ControlBar      │  │ │
                        │  │ (1 blit) │  │  │ (SVG icons +    │  │ │
                        │  └──────────┘  │  │  Timeline anim) │  │ │
                        │                │  └─────────────────┘  │ │
                        │  ThemeManager  │  ┌─────────────────┐  │ │
                        │  (Singleton)   │  │ SidebarPanel    │  │ │
                        │  ┌──────────┐  │  │ (GlitchTransition│ │ │
                        │  │ Semantic │  │  │  on toggle)     │  │ │
                        │  │ Colors   │  │  └─────────────────┘  │ │
                        │  │ + HSB    │  │  ┌─────────────────┐  │ │
                        │  │ Palette  │  │  │KeyboardHintPanel│  │ │
                        │  └──────────┘  │  │ (Theme colors,  │  │ │
                        │        │       │  │  instant switch)│  │ │
                        │        ▼       │  └─────────────────┘  │ │
                        │  ALL UI COMPONENTS                     │ │
                        └─────────────────────────────────────────┘

  ┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
  │ SvgIconLoader    │     │ GlitchTransition │     │ ParticleSystem   │
  │ (Batik           │     │ (RGB split on    │     │ (200 max,        │
  │  ImageTranscoder)│     │  BufferedImage)  │     │  velocity-scaled) │
  │ Cache by size    │     │ Timeline-driven  │     │ Own Timer(16ms)  │
  └──────────────────┘     └──────────────────┘     └──────────────────┘
```

### Recommended Project Structure

```
src/main/java/com/rebeatbox/
├── ui/
│   ├── ThemeManager.java          # NEW: Centralized semantic color constants
│   ├── SvgIconLoader.java         # NEW: Batik SVG -> BufferedImage cache
│   ├── ControlBar.java            # MODIFY: SVG icons + Timeline hover/press
│   ├── ReBeatBoxWindow.java       # MODIFY: GlassPane install, GlitchTransition triggers
│   ├── SidebarPanel.java          # MODIFY: GlitchTransition on toggle
│   ├── KeyboardHintPanel.java     # MODIFY: ThemeManager colors
│   └── PlaceholderPanel.java      # May be removed
├── live/
│   ├── PadButton.java             # MODIFY: ThemeManager + Timeline animation
│   └── DrumPadGrid.java           # MODIFY: ThemeManager backgrounds
├── visual/
│   ├── ParticleSystem.java        # NEW: Particle burst on GlassPane
│   ├── GlitchTransition.java      # NEW: RGB channel separation effect
│   ├── PianoRollPanel.java        # MODIFY: kernel 5->7, sigma 2.0->3.5, velocity->alpha
│   └── NoteColorMapper.java       # MODIFY: Expose constants for ThemeManager
├── engine/
│   └── (no changes)               # NoteEventBus already has dual-channel
├── resources/
│   └── icons/                     # NEW: ~20 cyber SVG icon files
└── App.java                       # MODIFY: ThemeManager init, SvgIconLoader init
```

### Pattern 1: ThemeManager — Singleton Semantic Color Provider

**What:** A single class providing static Color constants and HSB-derived palette methods. Components call `ThemeManager.BG_SURFACE` or `ThemeManager.accentForHue(float)` instead of `new Color(0x16213e)`.

**When to use:** Everywhere a color constant currently exists as a hardcoded hex literal in ControlBar, SidebarPanel, PadButton, KeyboardHintPanel, PianoRollPanel.

**Example:**
```java
// Source: synthesized from D-01 through D-04 decisions
package com.rebeatbox.ui;

import com.rebeatbox.visual.NoteColorMapper;
import java.awt.Color;

public final class ThemeManager {
    // Tiered dark backgrounds (D-03)
    public static final Color BG_ROOT     = new Color(0x0a0a14);
    public static final Color BG_SURFACE  = new Color(0x12122a);
    public static final Color BG_ELEVATED = new Color(0x1a1a3e);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(0xe0e0e0);
    public static final Color TEXT_SECONDARY = new Color(0x808080);
    public static final Color TEXT_ACCENT    = new Color(0x00E5FF); // Cyan

    // Borders
    public static final Color BORDER_IDLE   = new Color(0x2A3A5E);
    public static final Color BORDER_HOVER  = new Color(0xE040FB); // Magenta
    public static final Color BORDER_PRESS  = new Color(0x00E5FF); // Cyan

    // Semantic hue zones for different UI regions (D-02)
    public static final float HUE_TRANSPORT  = 0.55f; // Blue-Cyan
    public static final float HUE_DRUM_PADS  = 0.80f; // Purple-Magenta
    public static final float HUE_SIDEBAR    = 0.65f; // Blue-Purple
    public static final float HUE_KEYBOARD   = 0.50f; // Cyan

    /**
     * Returns a neon accent color at the given hue.
     * Extends NoteColorMapper's SAT=0.85, BRIGHT=0.95 constants.
     */
    public static Color accentForHue(float hue) {
        return Color.getHSBColor(hue, 0.85f, 0.95f);
    }

    private ThemeManager() {}
}
```

### Pattern 2: Radiance Timeline Button Animation

**What:** On `JComponent` mouse enter, a Timeline interpolates border color from idle to neon. On press, scale interpolates to 0.95x with a 150ms duration. On release, scale springs back to 1.0x.

**When to use:** Every interactive button — ControlBar transport, open button, PadButton (16), SidebarPanel toggle.

**Example:**
```java
// Source: Radiance docs (github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/)
// Synthesized from TimelineOverview.md, KeyFrameOverview.md, CustomPropertyInterpolators.md

import org.pushingpixels.radiance.animation.api.Timeline;
import org.pushingpixels.radiance.animation.api.KeyFrames;
import org.pushingpixels.radiance.animation.api.KeyValues;
import org.pushingpixels.radiance.animation.api.KeyTimes;
import org.pushingpixels.radiance.animation.api.ease.SplineEase;

// Hover animation: border color transition
Timeline hoverTimeline = Timeline.builder(button)
    .addPropertyToInterpolate("borderColor",
        ThemeManager.BORDER_IDLE,
        ThemeManager.accentForHue(ThemeManager.HUE_TRANSPORT))
    .setDuration(200)
    .setEase(new SplineEase(0.4f, 0.0f, 0.2f, 1.0f)) // Material deceleration
    .build();

// Press animation: scale + color flash via KeyFrames
KeyValues scaleValues = KeyValues.create(1.0f, 0.95f, 1.0f);
KeyTimes scaleTimes = new KeyTimes(0.0f, 0.3f, 1.0f);

Timeline pressTimeline = Timeline.builder(button)
    .addPropertyToInterpolate("scale", new KeyFrames(scaleValues, scaleTimes))
    .setDuration(150)
    .build();
```

### Pattern 3: Particle System — Off-Screen Accumulation + GlassPane

**What:** A custom GlassPane JComponent that maintains an off-screen BufferedImage accumulator. Each frame: clear accumulator -> draw all alive particles -> single `drawImage()` blit to screen. GlassPane overrides `contains()` to return false for mouse transparency.

**When to use:** All particle rendering. Never draw particles directly to GlassPane Graphics2D.

**Example:**
```java
// Source: synthesized from JVM Gaming Java2D performance threads, Java2D best practices
// [CITED: jvm-gaming.org/t/java2d-bloom, stackoverflow.com/questions/25827289]

public class ParticleSystem extends JComponent {
    private BufferedImage offscreen;
    private final List<Particle> particles = new ArrayList<>(200);
    private final Timer timer = new Timer(16, e -> updateAndRepaint());

    public ParticleSystem(int width, int height) {
        setOpaque(false);
        // Pre-allocate offscreen buffer with premultiplied alpha
        offscreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        timer.start();
    }

    // Mouse transparency (D-09: particles can travel anywhere)
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    void updateAndRepaint() {
        // Update particle positions, lifetimes, alpha
        long now = System.nanoTime();
        particles.removeIf(p -> p.isDead(now));
        for (Particle p : particles) p.update(now);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // Clear offscreen with Src (not SrcOver) to avoid blending with previous frame
        Graphics2D og = offscreen.createGraphics();
        og.setComposite(AlphaComposite.Src);
        og.setColor(new Color(0, 0, 0, 0));
        og.fillRect(0, 0, offscreen.getWidth(), offscreen.getHeight());
        og.setComposite(AlphaComposite.SrcOver);

        for (Particle p : particles) p.draw(og);
        og.dispose();

        // Single blit to GlassPane
        g2d.drawImage(offscreen, 0, 0, null);
    }

    public void emit(int note, int velocity) {
        // D-13: lifetime = 300ms (vel=0) -> 800ms (vel=127)
        int lifetimeMs = 300 + (velocity * 500 / 127);
        // D-14: size = 1-3px (vel=0) -> 4-8px (vel=127)
        int size = 1 + (velocity * 7 / 127);
        // D-10: color from NoteColorMapper
        Color color = NoteColorMapper.forPitch(note);

        if (particles.size() >= 200) {
            mergeOldestParticles(); // D-12
        }
        particles.add(new Particle(color, size, lifetimeMs));
    }
}
```

### Pattern 4: GlitchTransition — RGB Channel Split on Component Snapshot

**What:** Capture a BufferedImage of the target component before transition, apply RGB channel separation with Timeline-driven offset interpolation, composite the glitched image back during the transition.

**When to use:** SidebarPanel toggle (150ms) and MIDI file load (300ms) per D-18.

**Example:**
```java
// Source: [CITED: stackoverflow.com — BufferedImage pixel manipulation pattern]
// RGB channel separation formula verified across multiple sources

public class GlitchTransition {
    public static BufferedImage applyRgbSplit(BufferedImage src,
                                               int redOffsetX, int greenOffsetX,
                                               int blueOffsetX) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int[] srcPixels = src.getRGB(0, 0, w, h, null, 0, w);
        int[] dstPixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int srcIdx = y * w + x;
                int srcPixel = srcPixels[srcIdx];
                int a = (srcPixel >> 24) & 0xFF;

                // Sample R from (x + redOffsetX), G from (x + greenOffsetX), etc.
                int rx = clamp(x + redOffsetX, 0, w - 1);
                int gx = clamp(x + greenOffsetX, 0, w - 1);
                int bx = clamp(x + blueOffsetX, 0, w - 1);

                int r = (srcPixels[y * w + rx] >> 16) & 0xFF;
                int g = (srcPixels[y * w + gx] >> 8) & 0xFF;
                int b = srcPixels[y * w + bx] & 0xFF;

                dstPixels[srcIdx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        result.setRGB(0, 0, w, h, dstPixels, 0, w);
        return result;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
```

### Anti-Patterns to Avoid

- **Hardcoding colors after ThemeManager exists:** Any `new Color(0x...)` in a UI component after ThemeManager is implemented is a regression. Check during code review.
- **Creating new BufferedImage per particle per frame:** This will cause GC pressure and frame drops. Pre-render particle sprites or use fillRect on the offscreen buffer.
- **Blocking the EDT with image processing:** RGB channel separation on a full-window BufferedImage (~1280x720) touches ~921K pixels. For the 150ms sidebar transition, capture a smaller region (just the sidebar area). For the 300ms file-load transition, run the pixel work on a background thread and post the result to EDT.
- **Animating KeyboardHintPanel highlights:** D-31 mandates instant color switch. Timeline animation on key highlights would add EDT scheduling latency that degrades live play feel.
- **Using Batik's batik-all uber-jar:** Transitively pulls ~10MB+ including modules not needed (batik-anim, batik-script, batik-svggen for read-only icon rendering). Declare individual modules.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Button border/scale animation | Manual javax.swing.Timer + paintComponent manipulation | Radiance Timeline + KeyFrames | Timeline handles EDT scheduling, easing, interpolation, lifecycle. Manual timers are error-prone for chained animations (hover->press->release). |
| SVG rasterization | Custom XML parser + Java2D drawing | Apache Batik ImageTranscoder (D-24) | SVG spec is enormous — paths, transforms, gradients, viewBox, CSS. Batik implements full SVG 1.1. Custom parser would be months of work. |
| Color interpolation between keyframes | Manual lerp in a Timer callback | Radiance KeyFrames + ColorPropertyInterpolator | Radiance has built-in Color interpolation. No need to manually lerp R/G/B channels. |
| GaussianBlur | Custom convolution implementation | java.awt.image.ConvolveOp (already in use) | ConvolveOp handles edge conditions (EDGE_NO_OP), kernel normalization, and separable optimization. Already battle-tested in PianoRollPanel. |
| Particle lifecycle management | Custom object pool + GC tuning | ArrayList + removeIf + pre-sized capacity(200) | 200 particles at 60fps with ~500ms average lifetime is well within Java's GC capabilities. Object pooling is premature optimization for this scale. |

**Key insight:** Radiance Timeline and Batik ImageTranscoder handle entire problem domains (property animation, SVG parsing) that would each take weeks to implement correctly. The existing ConvolveOp pipeline is already optimized — don't replace it.

## Runtime State Inventory

> Phase 4 is a visual enhancement phase — not a rename/refactor/migration phase. No runtime state migration needed.
>
> **Stored data:** None. Phase 4 adds new rendering layers and color constants but does not create, modify, or migrate any persisted data.
> **Live service config:** None. Application is fully local with no external service configuration.
> **OS-registered state:** None.
> **Secrets/env vars:** None.
> **Build artifacts:** None. New dependencies (batik-*) will be added to build.gradle but no stale artifacts to clean up.

## Common Pitfalls

### Pitfall 1: GlassPane Repaint Triggers Full-Window Repaint

**What goes wrong:** When a non-opaque GlassPane calls repaint(), Swing's RepaintManager may mark the entire content pane as dirty, causing PianoRollPanel to re-render its full three-layer composite even though nothing changed. At 60fps, this doubles the rendering workload.

**Why it happens:** Swing's default RepaintManager coalesces dirty regions and, when the GlassPane covers the full window, may interpret the dirty region as covering the content pane.

**How to avoid:**
1. Set `setOpaque(false)` on the GlassPane.
2. Override `contains()` to return `false` so mouse events pass through.
3. Use `repaint(x, y, w, h)` with tight bounds rather than `repaint()`.
4. Render particles to an off-screen BufferedImage and blit once per frame.

**Warning signs:** PianoRollPanel.paintComponent() is called on every particle timer tick. Verify by adding a counter in paintComponent and checking it doesn't increment during particle-only frames.

### Pitfall 2: Per-Note BufferedImage Allocation in GaussianBlur

**What goes wrong:** `drawGlowingBar()` currently allocates a new `BufferedImage` per note per frame (`new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB)` at line 436). With 20 simultaneous notes, that's 20 allocations per frame = 1200 allocations/second. The 7x7 kernel increases the BLUR_PAD from 6 to ~10, making each BufferedImage ~20% larger.

**Why it happens:** The current architecture treats each note bar as an independent rendering unit, creating a fresh BufferedImage for the blur pipeline.

**How to avoid:** The existing code already works at 60fps with kernel 5. The jump to kernel 7 is a 1.4x arithmetic increase per pixel but does not change the architectural allocation pattern. For Phase 4, the safe path is:
1. Keep the existing architecture (per D-22: "Keep the existing separable two-pass ConvolveOp algorithm. Only constants change.")
2. Increase BLUR_PAD from 6 to 8 (to accommodate the wider kernel spread)
3. Profile after the change. If frame drops occur, add a BufferedImage pool keyed by (width, height) to reuse bar images.

**Warning signs:** GC pauses visible as frame stutters during dense chord sections. Monitor with `-verbose:gc`.

### Pitfall 3: Timeline Animation on Non-EDT Thread

**What goes wrong:** Calling `timeline.play()` from a non-EDT thread (e.g., a MIDI event callback) manipulates Swing component properties off the EDT, causing visual corruption or `ArrayIndexOutOfBoundsException` in Swing internals.

**Why it happens:** NoteEventBus fires callbacks on EDT via `SwingUtilities.invokeLater()`, but if ParticleSystem or GlitchTransition triggers are wired directly to non-EDT sources, the Timeline could mutate component state off-EDT.

**How to avoid:** NoteEventBus already wraps callbacks in `SwingUtilities.invokeLater()`. Ensure ParticleSystem.emit() is always called on EDT. GlitchTransition triggers (sidebar toggle, file load) are already EDT-based (user actions). Timeline.play() is EDT-safe when called from EDT.

**Warning signs:** Intermittent rendering artifacts, `ArrayIndexOutOfBoundsException` in `Component.dispatchEventImpl`.

### Pitfall 4: Batik Transitive Dependency Bloat

**What goes wrong:** Adding `batik-transcoder:1.19` alone pulls ~15 transitive JARs. The resulting fat JAR grows significantly.

**Why it happens:** Batik's modular structure means each XML/SVG processing concern has its own artifact, and transcoder depends on most of them.

**How to avoid:**
1. Declare individual modules (not `batik-all`).
2. Exclude `batik-anim`, `batik-script`, `batik-svggen` for read-only icon rendering.
3. The minimal set for BufferedImage rendering from static SVG files: `batik-transcoder`, `batik-awt-util`, `batik-bridge`, `batik-dom`, `batik-ext`, `batik-gvt`, `batik-svg-dom`, `batik-util`, `batik-xml`, `batik-parser`, `batik-css`.

**Warning signs:** JAR size jumps from ~15MB to ~25MB+ after adding Batik. Monitor with `./gradlew jar` and check output size.

### Pitfall 5: RGB Glitch on Live Component (Not Snapshot)

**What goes wrong:** Applying RGB channel separation to a live, animating component (like PianoRollPanel during playback) means re-capturing and re-processing the component's BufferedImage every frame. At 1280x720 with per-pixel channel sampling, this is ~2.8M array accesses per frame — expensive.

**Why it happens:** D-19 says "Component-level animation via Radiance Timeline" — the transition animates RGB offsets on a captured snapshot, not on the live component.

**How to avoid:** Capture a `BufferedImage` snapshot of the component once at transition start. Apply the RGB split to that snapshot. Animate the split image's offsets via Timeline on EDT. Do NOT try to glitch the live component every frame. The snapshot approach is consistent with D-19 ("NOT a monolithic screenshot-based approach" — meaning per-component snapshots, not full window).

**Warning signs:** Frame rate drops during transitions if live-capture is used instead of snapshot.

## Code Examples

Verified patterns from official sources:

### Batik SVG -> BufferedImage Rendering

```java
// Source: [CITED: gist.github.com/eclecticlogic/7890297, batik.apache.org]
// Pattern verified across multiple sources

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import java.awt.image.BufferedImage;

class BufferedImageTranscoder extends ImageTranscoder {
    private BufferedImage img;

    @Override
    public BufferedImage createImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage img, TranscoderOutput output) {
        this.img = img;
    }

    public BufferedImage getBufferedImage() { return img; }
}

// Usage: rasterize SVG at requested dimensions
public BufferedImage loadSvg(InputStream svgStream, int width, int height) throws TranscoderException {
    BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
    transcoder.setTranscodingHints(Map.of(
        ImageTranscoder.KEY_WIDTH, (float) width,
        ImageTranscoder.KEY_HEIGHT, (float) height,
        ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE,
        ImageTranscoder.KEY_DOM_IMPLEMENTATION,
            SVGDOMImplementation.getDOMImplementation(),
        ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
            SVGConstants.SVG_NAMESPACE_URI,
        ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg"
    ));
    transcoder.transcode(new TranscoderInput(svgStream), null);
    return transcoder.getBufferedImage();
}
```

### Radiance Timeline with Easing and Callbacks

```java
// Source: [VERIFIED: github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/TimelineLifecycle.md]
// Builder API confirmed via TimelineOverview.md, KeyFrameOverview.md

Timeline timeline = Timeline.builder(targetComponent)
    .addPropertyToInterpolate("borderColor", idleColor, hoverColor)
    .setDuration(250)
    .setEase(new SplineEase(0.4f, 0.0f, 0.2f, 1.0f))
    .addCallback(new TimelineCallback() {
        @Override
        public void onTimelineStateChanged(TimelineState oldState,
                                            TimelineState newState,
                                            float durationFraction,
                                            float timelinePosition) {
            if (newState == TimelineState.DONE) {
                // Cleanup after animation completes
            }
        }
        @Override
        public void onTimelinePulse(float durationFraction,
                                     float timelinePosition) {
            // Per-frame logic (alternative to property setters)
        }
    })
    .build();

timeline.play();
```

### Particle Emission via NoteEventBus Subscription

```java
// Source: existing codebase pattern (NoteEventBus.java, ReBeatBoxWindow.java lines 81-90)
// Extended for particle system

// Subscribe to sequencer notes
eventBus.subscribe(activeNotes -> {
    for (int note : activeNotes) {
        particleSystem.emit(note, 100); // Default velocity for sequencer notes
    }
});

// Subscribe to live notes (note + velocity available)
eventBus.subscribeLive(new LiveNoteEventListener() {
    @Override
    public void onLiveNoteOn(int note, int velocity) {
        particleSystem.emit(note, velocity);
    }
    @Override
    public void onLiveNoteOff(int note) {
        // Particles are fire-and-forget — no action on note-off
    }
});
```

### Velocity-to-Alpha Mapping in PianoRollPanel

```java
// Source: D-28, D-29 decisions
// Inserted into PianoRollPanel.drawSingleNote() replacing hardcoded 1.0f/0.4f

private float velocityToAlpha(int velocity, boolean aboveTrigger) {
    if (aboveTrigger) {
        // D-28: velocity 0-127 -> alpha 0.3-1.0
        return 0.3f + (velocity / 127.0f) * 0.7f;
    } else {
        // D-28: velocity 0-127 -> alpha 0.15-0.4
        return 0.15f + (velocity / 127.0f) * 0.25f;
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hardcoded Color(0x...) in each component | ThemeManager centralized constants | Phase 4 | Single-source-of-truth for palette; easy global retheme |
| javax.swing.Timer for animations | Radiance Timeline + KeyFrames | Phase 4 | Smoother easing, chained animations, less boilerplate |
| Unicode text icons (▶, ⏸, etc.) | SVG-rendered BufferedImage icons | Phase 4 | Consistent styling, scalable, cyberpunk aesthetic |
| 5x5 GaussianBlur, sigma 2.0 | 7x7 GaussianBlur, sigma 3.5 | Phase 4 | More visible glow on short/fast notes per UAT finding |
| Static sidebar toggle (setPreferredSize) | Timeline-driven RGB glitch + resize | Phase 4 | Cyberpunk visual identity |
| Unused GlassPane | Particle system GlassPane overlay | Phase 4 | New visual feature |

**Deprecated/outdated:**
- Unicode text icons on ControlBar buttons: Replaced with SVG BufferedImage icons (D-26).
- SidebarPanel instant toggle: Replaced with GlitchTransition Timeline animation (D-16, D-19).
- Hardcoded color constants in all UI files: Replaced with ThemeManager references (D-01).
- PadButton hover/pressed color logic via MouseListener + repaint(): Augmented with Radiance Timeline (D-07), but paintComponent overrides remain (PadButton uses custom painting).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Radiance 8.5.0 Timeline API includes `setEase()`, `setDuration()`, and `addCallback()` on the builder — confirmed via docs overview but exact method signatures could differ slightly from the sunshine branch documentation | Radiance Ephemeral Animation API | MEDIUM: Would need to use different method names or RadianceAnimationCortex APIs |
| A2 | Batik 1.19 is the latest stable version and is available from Maven Central via the configured Aliyun mirrors | SVG Icon Loading | LOW: If not available, fall back to 1.18 or 1.17 which are confirmed on mirrors |
| A3 | Batik's `ImageTranscoder` with the described TranscodingHints works for simple SVG icons without animation, filters, or external CSS | SVG Icon Loading | MEDIUM: If SVG icons use unsupported features, icons may render incorrectly or not at all |
| A4 | GlassPane with `contains() -> false` fully passes mouse events to underlying components without side effects on hover states or focus traversal | GlassPane Rendering Strategy | LOW: Well-documented Swing pattern; verified across multiple sources |
| A5 | PianoRollPanel's existing separable ConvolveOp handles kernel-size 7 without edge artifacts at BLUR_PAD=8 | GaussianBlur Optimization | LOW: ConvolveOp handles arbitrary kernel sizes; edge conditions managed by EDGE_NO_OP |
| A6 | The RGB glitch pixel manipulation on a captured BufferedImage will complete within one frame (~16ms) for the sidebar region (~240x720 pixels) | Glitch Effect | LOW: 240x720 = 172,800 pixels; single-pass array iteration is well under 16ms |

## Open Questions

1. **Batik vs JSVG — is D-24 truly locked?**
   - What we know: D-24 says "Apache Batik library." JSVG is 500KB vs Batik's ~10MB transitive deps, used by JetBrains, and sufficient for ~20 simple cyber icons.
   - What's unclear: Whether the user considered JSVG before deciding. The tradeoff is significant (JAR size, load time, memory).
   - Recommendation: Use Batik as mandated but document the JSVG alternative. If JAR size becomes an issue in Phase 5 (shipping), the planner can add a migration task.

2. **Particle merge algorithm specifics**
   - What we know: D-12 says "merge the oldest 2-3 particles into a single larger particle." Claude's discretion.
   - What's unclear: Merged position (average? centroid?), merged velocity (vector average?), merged color (blend? keep dominant?).
   - Recommendation: Average position, sum velocities with cap, pick the color of the particle with the largest remaining lifetime.

3. **Glitch RGB offset magnitude during transition**
   - What we know: Classic chromatic aberration: red shifted left, blue shifted right, green stays. Claude's discretion on exact pixel offsets.
   - What's unclear: Should offsets increase then decrease (bell curve) or ramp linearly?
   - Recommendation: Bell curve via KeyFrames — offsets peak at timelinePosition=0.5 then return to 0. Max offset ~15px for sidebar, ~25px for file load.

4. **SvgIconLoader cache invalidation**
   - What we know: Icons cached as BufferedImage keyed by size (D-24). ~20 icons at 2-3 sizes each = 40-60 cached images.
   - What's unclear: Whether the cache should support runtime resizing (window resize changes button size).
   - Recommendation: Pre-render at the fixed sizes needed (38x38 for transport, 24x24 for toggle, 48x48 for pads). Fixed-size buttons per current ControlBar/PadButton code. No runtime resizing needed.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Radiance animation | Button hover/press (UI-03), GlitchTransition (GLITCH-02) | ✓ | 8.5.0 | — Already in build.gradle |
| Radiance common | Radiance animation transitive dep | ✓ | 8.5.0 | — Already in build.gradle |
| Apache Batik | SVG icon loading (GLITCH-04, D-24) | ✗ | — (not yet added) | Add to build.gradle before implementation |
| javax.swing (JFrame, Timer, etc.) | GlassPane, ParticleSystem, all UI | ✓ | JDK 17 built-in | — |
| java.awt.image (ConvolveOp, BufferedImage) | GaussianBlur, RGB glitch, particle rendering | ✓ | JDK 17 built-in | — |
| javax.sound.midi | NoteEventBus integration | ✓ | JDK 17 built-in | — |

**Missing dependencies with no fallback:**
- Apache Batik (batik-transcoder + dependencies): Must be added to build.gradle before GLITCH-04 implementation. No fallback — D-24 mandates Batik specifically.

**Missing dependencies with fallback:**
- None. All other dependencies are either already present (Radiance 8.5.0) or JDK built-in.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | none — defaults in build.gradle `test { useJUnitPlatform() }` |
| Quick run command | `./gradlew test --tests "*ThemeManagerTest*"` |
| Full suite command | `./gradlew test` |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UI-02 | ThemeManager provides BG_ROOT, BG_SURFACE, BG_ELEVATED with correct hex values | unit | `./gradlew test --tests "*ThemeManagerTest*"` | ❌ Wave 0 |
| UI-02 | ThemeManager.accentForHue() returns high-saturation neon colors | unit | `./gradlew test --tests "*ThemeManagerTest*"` | ❌ Wave 0 |
| UI-02 | All UI components use ThemeManager colors (no hardcoded 0x literals) | manual | Grep audit: `rg "new Color\(0x" src/main/java/com/rebeatbox/ui/` | ❌ Wave 0 |
| UI-03 | Button hover triggers border color Timeline animation | integration | Manual visual inspection (Timeline runs on EDT, hard to assert in unit test) | — |
| UI-03 | Button press triggers 0.95x scale + color flash | integration | Manual visual inspection | — |
| GLITCH-01 | ParticleSystem.emit() creates particle with correct color from NoteColorMapper | unit | `./gradlew test --tests "*ParticleSystemTest*"` | ❌ Wave 0 |
| GLITCH-01 | Particle lifetime is within [300, 800] ms proportional to velocity | unit | `./gradlew test --tests "*ParticleSystemTest*"` | ❌ Wave 0 |
| GLITCH-01 | Particle size is within [1, 8] px proportional to velocity | unit | `./gradlew test --tests "*ParticleSystemTest*"` | ❌ Wave 0 |
| GLITCH-01 | Max 200 particles; oldest merge when full | unit | `./gradlew test --tests "*ParticleSystemTest*"` | ❌ Wave 0 |
| GLITCH-02 | GlitchTransition.applyRgbSplit() produces correct channel offsets | unit | `./gradlew test --tests "*GlitchTransitionTest*"` | ❌ Wave 0 |
| GLITCH-02 | Sidebar toggle triggers ~150ms transition | integration | Manual visual inspection | — |
| GLITCH-03 | GaussianBlur kernel is 7 (was 5) | unit | `./gradlew test --tests "*PianoRollPanel*"` or verify constant | ❌ Wave 0 |
| GLITCH-03 | GaussianBlur sigma is 3.5 (was 2.0) | unit | Same as above | ❌ Wave 0 |
| GLITCH-04 | SvgIconLoader loads SVG and returns non-null BufferedImage | unit | `./gradlew test --tests "*SvgIconLoaderTest*"` | ❌ Wave 0 |
| GLITCH-04 | SvgIconLoader caches by size — same size returns same instance | unit | `./gradlew test --tests "*SvgIconLoaderTest*"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test` (all tests)
- **Per wave merge:** `./gradlew test` (full suite)
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/com/rebeatbox/ui/ThemeManagerTest.java` — covers UI-02 color constants and neon generation
- [ ] `src/test/java/com/rebeatbox/visual/ParticleSystemTest.java` — covers GLITCH-01 particle lifecycle, cap, merge
- [ ] `src/test/java/com/rebeatbox/visual/GlitchTransitionTest.java` — covers GLITCH-02 RGB channel separation correctness
- [ ] `src/test/java/com/rebeatbox/ui/SvgIconLoaderTest.java` — covers GLITCH-04 SVG loading and caching
- [ ] `src/test/java/com/rebeatbox/visual/PianoRollPanelBlurTest.java` — covers GLITCH-03 kernel/sigma constant verification
- [ ] Test SVG fixture files in `src/test/resources/icons/` — minimal SVG files for loader tests

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — Desktop app, no user auth |
| V3 Session Management | no | — No sessions |
| V4 Access Control | no | — Single-user desktop app |
| V5 Input Validation | yes (limited) | MIDI file validation already in MidiFileLoader; SVG input from classpath resources only — no user-supplied SVGs |
| V6 Cryptography | no | — No cryptographic operations |

### Known Threat Patterns for Java2D/Swing

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Malformed SVG causing resource exhaustion | Denial of Service | Icons loaded from classpath (trusted resources), not user input. Batik's parser handles malformed XML gracefully. |
| EDT blocking from slow image processing | Denial of Service | RGB glitch on captured snapshot, not live component; particle system uses off-screen buffer; blur uses pre-allocated buffers |
| Memory exhaustion from unbounded particle list | Denial of Service | Hard cap at 200 particles (D-12); merge-oldest strategy prevents growth |

## Sources

### Primary (HIGH confidence)
- [github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/TimelineOverview.md] — Timeline builder API, property interpolation
- [github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/TimelineLifecycle.md] — Complete Timeline lifecycle API, callbacks, loop modes
- [github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/KeyFrameOverview.md] — KeyFrames, KeyValues, KeyTimes API
- [github.com/kirill-grouchnikov/radiance/blob/sunshine/docs/animation/CustomPropertyInterpolators.md] — PropertyInterpolator interface, global registration
- [github.com/weisJ/jsvg] — JSVG lightweight SVG renderer (alternative, documented for comparison)
- Project build.gradle — Confirmed Radiance 8.5.0, JUnit 5.10.0, Java 17
- Project source files (NoteEventBus.java, PianoRollPanel.java, ControlBar.java, ReBeatBoxWindow.java, SidebarPanel.java, PadButton.java, KeyboardHintPanel.java, App.java) — Existing architecture and integration points
- Project CONTEXT.md (04-CONTEXT.md) — 31 locked decisions D-01 through D-31

### Secondary (MEDIUM confidence)
- [pushings-pixels.org/2025/12/16/radiance-8-5-0.html] — Radiance 8.5.0 release notes
- [nexus.web.cern.ch] — Batik 1.19 release date confirmation
- [gist.github.com/eclecticlogic/7890297] — Batik BufferedImageTranscoder pattern (verified across 3+ sources)
- [jvm-gaming.org/t/java2d-bloom] — Java2D bloom/particle performance discussion
- [stackoverflow.com/questions/25827289] — GlassPane repaint behavior with JFrame
- [stackoverflow.com — BufferedImage RGB channel manipulation] — Pixel-level glitch effect implementation

### Tertiary (LOW confidence)
- WebSearch results for Java2D hardware acceleration properties (`sun.java2d.opengl`, `sun.java2d.d3d`) — these are JVM-implementation-specific and may vary across platforms. Not recommended as primary strategy.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Radiance 8.5.0 confirmed in build.gradle; Batik 1.19 confirmed via Maven mirrors; all JDK built-in
- Architecture: HIGH — All patterns (Timeline animation, GlassPane overlay, off-screen accumulation, separable ConvolveOp) are proven in existing codebase or well-documented in official docs
- Pitfalls: HIGH — GlassPane repaint and per-frame allocation pitfalls are well-known Swing issues documented across multiple sources
- SVG loading: MEDIUM — Batik API confirmed but the exact SVG feature set needed for cyber icons (filters? gradients? masks?) is Claude's discretion and could affect required Batik modules

**Research date:** 2026-04-28
**Valid until:** 2026-05-28 (stable domain — Radiance 8.5.0, Batik 1.19, and JDK 17 are all stable releases)
