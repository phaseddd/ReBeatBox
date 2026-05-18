package com.rebeatbox.ui;

import com.rebeatbox.visual.GlitchTransition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.pushingpixels.radiance.animation.api.Timeline;
import org.pushingpixels.radiance.animation.api.Timeline.TimelineState;
import org.pushingpixels.radiance.animation.api.callback.TimelineCallback;
import org.pushingpixels.radiance.animation.api.ease.Spline;

public class SidebarPanel extends JPanel {
    private boolean expanded = true;
    private final int expandedWidth = 240;
    private final int collapsedWidth = 0;
    private final JButton toggleButton;
    private final JPanel contentPanel;

    /** Snapshot captured before collapse animation starts — reused as source for expand glitch. */
    private transient BufferedImage preCollapseSnapshot;

    /** Current glitch frame image; null when no transition active. */
    private transient BufferedImage glitchImage;

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeManager.BG_SURFACE);

        // Toggle button
        toggleButton = new JButton();
        toggleButton.setPreferredSize(new Dimension(24, 24));
        toggleButton.setToolTipText("Collapse sidebar");
        toggleButton.setFocusable(false);
        toggleButton.setBorder(BorderFactory.createLineBorder(ThemeManager.BORDER_IDLE, 1));
        toggleButton.setBackground(ThemeManager.BG_ELEVATED);
        toggleButton.getAccessibleContext().setAccessibleName("Collapse sidebar");

        // Load initial SVG icon (expanded state = "collapse-left")
        try {
            BufferedImage icon = SvgIconLoader.getIcon("collapse-left", 24);
            if (icon != null) toggleButton.setIcon(new ImageIcon(icon));
        } catch (Exception e) {
            System.err.println("Failed to load sidebar icon: " + e.getMessage());
        }

        // Wire Timeline hover/press animation (D-06: ALL interactive buttons)
        wireToggleAnimation();

        toggleButton.addActionListener(this::toggle);

        JPanel toggleBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toggleBar.setOpaque(false);
        toggleBar.add(toggleButton);

        // Content panel (reserved for Phase 3 drum pad)
        contentPanel = new JPanel();
        contentPanel.setBackground(ThemeManager.BG_SURFACE);
        contentPanel.setLayout(new BorderLayout());

        add(toggleBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        setPreferredSize(new Dimension(expandedWidth, getHeight()));
    }

    private void toggle(ActionEvent e) {
        if (!expanded) {
            // EXPANDING: use stored pre-collapse snapshot for glitch transition
            if (preCollapseSnapshot != null) {
                performGlitchExpand(preCollapseSnapshot);
            } else {
                // No snapshot available (first expand or snapshot was flushed) — instant expand
                expanded = true;
                updateToggleIcon();
                setPreferredSize(new Dimension(expandedWidth, getHeight()));
                revalidate();
                repaint();
            }
        } else {
            // COLLAPSING: capture snapshot, then run glitch collapse
            performGlitchCollapse();
        }
    }

    /**
     * Captures current sidebar content snapshot, then runs 150ms RGB glitch
     * transition before collapsing. Stores snapshot for future expand glitch.
     * Per D-16 (sidebar toggle), D-17 (RGB split), D-18 (150ms sidebar), D-19 (component-level).
     */
    private void performGlitchCollapse() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            // Fallback: instant collapse if no pixels to snapshot
            expanded = false;
            updateToggleIcon();
            setPreferredSize(new Dimension(collapsedWidth, getHeight()));
            revalidate();
            repaint();
            return;
        }

        // Capture snapshot of current sidebar content FOR BOTH collapse glitch AND expand glitch
        BufferedImage snapshot = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = snapshot.createGraphics();
        paint(g2);
        g2.dispose();

        // Store for later expand glitch (D-16: glitch on both directions)
        if (this.preCollapseSnapshot != null) {
            this.preCollapseSnapshot.flush();
        }
        this.preCollapseSnapshot = snapshot;

        // Create glitch transition with 150ms duration (D-18)
        // Max offset: 15px red left, 15px blue right (UI-SPEC)
        final int maxOffset = 15;

        Timeline glitchTimeline = Timeline.builder(this)
            .setDuration(150)
            .addCallback(new TimelineCallback() {
                @Override
                public void onTimelinePulse(float durationFraction, float timelinePosition) {
                    // Bell curve: peak at 0.5, returns to 0 at start/end
                    float bellCurve = 4.0f * timelinePosition * (1.0f - timelinePosition);
                    int offset = Math.round(maxOffset * bellCurve);

                    BufferedImage glitched = GlitchTransition.applyRgbSplit(snapshot, -offset, offset);
                    setGlitchImage(glitched);
                    repaint();
                }

                @Override
                public void onTimelineStateChanged(TimelineState oldState, TimelineState newState,
                                                   float durationFraction, float timelinePosition) {
                    if (newState == TimelineState.DONE) {
                        setGlitchImage(null);
                        expanded = false;
                        updateToggleIcon();
                        setPreferredSize(new Dimension(collapsedWidth, getHeight()));
                        revalidate();
                        repaint();
                        // NOTE: Do NOT flush preCollapseSnapshot — it's needed for expand glitch
                    }
                }
            })
            .build();
        glitchTimeline.play();
    }

    /**
     * Runs a 150ms RGB glitch transition during sidebar expand, using the
     * pre-collapse snapshot as the source texture. Per D-16 (glitch on expand too).
     */
    private void performGlitchExpand(BufferedImage sourceSnapshot) {
        // Resize to expanded width first so the component has dimensions to paint into
        expanded = true;
        updateToggleIcon();
        setPreferredSize(new Dimension(expandedWidth, getHeight()));
        revalidate();

        final int maxOffset = 15;
        final int duration = 150; // D-18: sidebar 150ms

        Timeline glitchTimeline = Timeline.builder(this)
            .setDuration(duration)
            .addCallback(new TimelineCallback() {
                @Override
                public void onTimelinePulse(float durationFraction, float timelinePosition) {
                    float bellCurve = 4.0f * timelinePosition * (1.0f - timelinePosition);
                    int offset = Math.round(maxOffset * bellCurve);

                    BufferedImage glitched = GlitchTransition.applyRgbSplit(sourceSnapshot, -offset, offset);
                    setGlitchImage(glitched);
                    repaint();
                }

                @Override
                public void onTimelineStateChanged(TimelineState oldState, TimelineState newState,
                                                   float durationFraction, float timelinePosition) {
                    if (newState == TimelineState.DONE) {
                        setGlitchImage(null);
                        // Flush the snapshot — it's regenerated on next collapse cycle
                        sourceSnapshot.flush();
                        preCollapseSnapshot = null;
                        repaint();
                    }
                }
            })
            .build();
        glitchTimeline.play();
    }

    private void setGlitchImage(BufferedImage img) {
        this.glitchImage = img;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (glitchImage != null) {
            g.drawImage(glitchImage, 0, 0, null);
        } else {
            super.paintComponent(g);
        }
    }

    private void updateToggleIcon() {
        String iconName = expanded ? "collapse-left" : "expand-right";
        String tooltip = expanded ? "Collapse sidebar" : "Expand sidebar";
        toggleButton.setToolTipText(tooltip);
        toggleButton.getAccessibleContext().setAccessibleName(tooltip);
        try {
            BufferedImage icon = SvgIconLoader.getIcon(iconName, 24);
            if (icon != null) toggleButton.setIcon(new ImageIcon(icon));
        } catch (Exception e) {
            System.err.println("Failed to load sidebar icon: " + e.getMessage());
        }
    }

    /**
     * Linearly interpolates between two Colors for smooth Timeline transitions.
     */
    private static Color interpolateColor(Color a, Color b, float t) {
        float ti = Math.max(0.0f, Math.min(1.0f, t));
        int r = (int)(a.getRed()   + ti * (b.getRed()   - a.getRed()));
        int g = (int)(a.getGreen() + ti * (b.getGreen() - a.getGreen()));
        int bl = (int)(a.getBlue() + ti * (b.getBlue() - a.getBlue()));
        return new Color(Math.min(255, Math.max(0, r)),
                         Math.min(255, Math.max(0, g)),
                         Math.min(255, Math.max(0, bl)));
    }

    /**
     * Wires Radiance Timeline hover border-color and press scale animation
     * onto the SidebarPanel toggle button. Per D-05, D-06, D-07.
     * Pattern mirrors ControlBar.wireButtonAnimation but for a single JButton.
     */
    private void wireToggleAnimation() {
        final Color accentColor = ThemeManager.accentForHue(ThemeManager.HUE_SIDEBAR);
        final Dimension origSize = new Dimension(toggleButton.getPreferredSize());

        toggleButton.addMouseListener(new MouseAdapter() {
            private Timeline hoverIn, hoverOut, pressDown, pressUp;

            @Override
            public void mouseEntered(MouseEvent e) {
                if (hoverOut != null) hoverOut.abort();
                if (hoverIn != null) hoverIn.abort();
                hoverIn = Timeline.builder(toggleButton)
                    .setDuration(200)
                    .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
                    .addCallback(new TimelineCallback() {
                        @Override public void onTimelinePulse(float df, float tp) {
                            Color c = interpolateColor(ThemeManager.BORDER_IDLE, accentColor, tp);
                            toggleButton.setBorder(BorderFactory.createLineBorder(c, 1));
                        }
                        @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {}
                    })
                    .build();
                hoverIn.play();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverIn != null) hoverIn.abort();
                if (hoverOut != null) hoverOut.abort();
                hoverOut = Timeline.builder(toggleButton)
                    .setDuration(250)
                    .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
                    .addCallback(new TimelineCallback() {
                        @Override public void onTimelinePulse(float df, float tp) {
                            Color c = interpolateColor(accentColor, ThemeManager.BORDER_IDLE, tp);
                            toggleButton.setBorder(BorderFactory.createLineBorder(c, 1));
                        }
                        @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {}
                    })
                    .build();
                hoverOut.play();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (pressUp != null) pressUp.abort();
                if (pressDown != null) pressDown.abort();
                pressDown = Timeline.builder(toggleButton)
                    .setDuration(75)
                    .addCallback(new TimelineCallback() {
                        @Override public void onTimelinePulse(float df, float tp) {
                            float scale = 1.00f + tp * (0.95f - 1.00f);
                            toggleButton.setPreferredSize(new Dimension((int)(origSize.width * scale), (int)(origSize.height * scale)));
                            toggleButton.getParent().revalidate();
                        }
                        @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {}
                    })
                    .build();
                pressDown.play();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (pressDown != null) pressDown.abort();
                if (pressUp != null) pressUp.abort();
                pressUp = Timeline.builder(toggleButton)
                    .setDuration(150)
                    .setEase(new Spline(0.4f, 0.0f, 0.2f, 1.0f))
                    .addCallback(new TimelineCallback() {
                        @Override public void onTimelinePulse(float df, float tp) {
                            float scale = 0.95f + tp * (1.00f - 0.95f);
                            toggleButton.setPreferredSize(new Dimension((int)(origSize.width * scale), (int)(origSize.height * scale)));
                            toggleButton.getParent().revalidate();
                        }
                        @Override public void onTimelineStateChanged(TimelineState o, TimelineState n, float df, float tp) {
                            if (n == TimelineState.DONE) {
                                toggleButton.setPreferredSize(origSize);
                                toggleButton.getParent().revalidate();
                            }
                        }
                    })
                    .build();
                pressUp.play();
            }
        });
    }

    public boolean isExpanded() {
        return expanded;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
