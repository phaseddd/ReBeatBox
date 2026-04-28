package com.rebeatbox;

import com.rebeatbox.engine.*;
import com.rebeatbox.ui.ReBeatBoxWindow;
import org.pushingpixels.radiance.theming.api.RadianceThemingCortex;
import org.pushingpixels.radiance.theming.api.skin.NightShadeSkin;

import javax.sound.midi.Synthesizer;
import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. Apply Radiance NightShade dark skin (before any window creation)
            try {
                NightShadeSkin skin = new NightShadeSkin();
                RadianceThemingCortex.GlobalScope.setSkin(skin);
            } catch (Exception e) {
                System.err.println("Warning: Failed to apply Radiance skin: " + e.getMessage());
                // Continue even without skin — use plain Swing look
            }

            // 2. Initialize MIDI engine
            Synthesizer synth = null;
            NoteEventBus eventBus = new NoteEventBus();
            PlaybackController controller = null;
            RealtimeReceiver receiver = null;

            try {
                SoundFontManager sfManager = new SoundFontManager();
                synth = sfManager.initialize();
                System.out.println("MIDI synthesizer initialized"
                    + (sfManager.isCustomSoundFontLoaded() ? " with FluidR3_GM.sf2" : " with JDK default soundbank"));
            } catch (Exception e) {
                showStartupError("Cannot Initialize Audio",
                    "Unable to open MIDI synthesizer.\nPlease check your system audio settings and restart the application.\n\n" + e.getMessage());
                e.printStackTrace();
                System.exit(1);
                return;
            }

            try {
                controller = new PlaybackController(synth, eventBus);
                receiver = new RealtimeReceiver(synth);
            } catch (Exception e) {
                showStartupError("Cannot Initialize MIDI Engine",
                    "SoundFont file may be missing or corrupted.\nPlease reinstall the application.\n\n" + e.getMessage());
                e.printStackTrace();
                if (synth != null) synth.close();
                System.exit(1);
                return;
            }

            // 3. Create UI and wire engine
            ReBeatBoxWindow window = new ReBeatBoxWindow();
            window.wireEngine(controller, receiver, eventBus);
            window.setVisible(true);
        });
    }

    private static void showStartupError(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
