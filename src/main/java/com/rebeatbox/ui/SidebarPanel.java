package com.rebeatbox.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SidebarPanel extends JPanel {
    private boolean expanded = true;
    private final int expandedWidth = 240;
    private final int collapsedWidth = 0;
    private final JButton toggleButton;
    private final JPanel contentPanel;

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x16213e));

        // Toggle button
        toggleButton = new JButton("◀"); // ◀
        toggleButton.setPreferredSize(new Dimension(24, 24));
        toggleButton.setToolTipText("Collapse sidebar");
        toggleButton.setFocusable(false);
        toggleButton.addActionListener(this::toggle);

        JPanel toggleBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toggleBar.setOpaque(false);
        toggleBar.add(toggleButton);

        // Content panel (reserved for Phase 3 drum pad)
        contentPanel = new JPanel();
        contentPanel.setBackground(new Color(0x16213e));
        contentPanel.setLayout(new BorderLayout());

        add(toggleBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        setPreferredSize(new Dimension(expandedWidth, getHeight()));
    }

    private void toggle(ActionEvent e) {
        expanded = !expanded;
        toggleButton.setText(expanded ? "◀" : "▶"); // ◀ or ▶
        toggleButton.setToolTipText(expanded ? "Collapse sidebar" : "Expand sidebar");
        setPreferredSize(new Dimension(expanded ? expandedWidth : collapsedWidth, getHeight()));
        revalidate();
        repaint();
    }

    public boolean isExpanded() {
        return expanded;
    }
}
