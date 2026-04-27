package com.rebeatbox;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Apply Radiance NightShade dark skin
                org.pushingpixels.radiance.theming.api.skin.NightShadeSkin skin =
                    new org.pushingpixels.radiance.theming.api.skin.NightShadeSkin();
                org.pushingpixels.radiance.theming.api.RadianceThemingCortex.GlobalScope.setSkin(skin);
            } catch (Exception e) {
                System.err.println("Failed to apply Radiance skin: " + e.getMessage());
            }

            JFrame frame = new JFrame("ReBeatBox");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 700);
            frame.setMinimumSize(new Dimension(800, 500));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
