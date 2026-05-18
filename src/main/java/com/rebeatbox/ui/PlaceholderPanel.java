package com.rebeatbox.ui;

import com.rebeatbox.ui.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel() {
        setBackground(ThemeManager.BG_ELEVATED);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Dark gradient background
        GradientPaint gradient = new GradientPaint(0, 0, ThemeManager.BG_ELEVATED, 0, h, ThemeManager.BG_SURFACE);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, w, h);

        // Title
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.setColor(ThemeManager.TEXT_PRIMARY);
        String title = "ReBeatBox";
        FontMetrics fmTitle = g2d.getFontMetrics();
        int titleX = (w - fmTitle.stringWidth(title)) / 2;
        int titleY = h / 2 - 8;
        g2d.drawString(title, titleX, titleY);

        // Subtitle
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(ThemeManager.TEXT_SECONDARY);
        String subtitle = "Drag and drop a .mid file or click the Open button";
        FontMetrics fmSub = g2d.getFontMetrics();
        int subX = (w - fmSub.stringWidth(subtitle)) / 2;
        int subY = titleY + 28;
        g2d.drawString(subtitle, subX, subY);
    }
}
