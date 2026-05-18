package com.rebeatbox.visual;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParticleSystem}.
 *
 * <p>Covers: particle emission, lifetime range, size range, 200-particle
 * cap with merge-on-overflow, mouse transparency via contains(), and
 * color correctness from NoteColorMapper.
 */
class ParticleSystemTest {

    // ------------------------------------------------------------------
    // Test 1: emit creates a particle with correct color
    // ------------------------------------------------------------------

    @Test
    void testEmitCreatesParticle() throws Exception {
        ParticleSystem ps = createOnEdt();

        assertEquals(0, ps.getParticleCount(),
                "No particles before emit");

        ps.emit(60, 100);
        assertTrue(ps.getParticleCount() > 0,
                "Particle should be created after emit");

        Color expected = NoteColorMapper.forPitch(60);
        Color actual = ps.getParticleColor(0);
        assertEquals(expected, actual,
                "Particle color should match NoteColorMapper.forPitch(note)");
    }

    // ------------------------------------------------------------------
    // Test 2: lifetime range (300ms vel=0 -> 800ms vel=127)
    // ------------------------------------------------------------------

    @Test
    void testLifetimeRange() throws Exception {
        ParticleSystem ps = createOnEdt();

        // Start the animation timer on EDT so dead particles are removed
        SwingUtilities.invokeAndWait(ps::start);

        // velocity=0 -> 300ms lifetime
        ps.emit(60, 0);
        assertTrue(ps.getParticleCount() > 0,
                "Particle should exist immediately after emit (300ms lifetime)");

        // Wait well past 300ms for the timer to remove dead particles
        Thread.sleep(500);
        assertEquals(0, ps.getParticleCount(),
                "300ms-lifetime particle should be dead after 500ms");

        // velocity=127 -> 800ms lifetime
        ps.emit(60, 127);
        assertTrue(ps.getParticleCount() > 0,
                "Particle should exist immediately after emit (800ms lifetime)");

        // Wait well past 800ms
        Thread.sleep(950);
        assertEquals(0, ps.getParticleCount(),
                "800ms-lifetime particle should be dead after 950ms");

        ps.stop();
    }

    // ------------------------------------------------------------------
    // Test 3: size range (1px vel=0 -> 8px vel=127)
    // ------------------------------------------------------------------

    @Test
    void testSizeRange() throws Exception {
        ParticleSystem ps = createOnEdt();

        ps.emit(60, 0);
        assertEquals(1f, ps.getParticleSize(0), 0.01f,
                "vel=0 should produce size 1px");

        ParticleSystem ps2 = createOnEdt();
        ps2.emit(60, 127);
        assertEquals(8f, ps2.getParticleSize(0), 0.01f,
                "vel=127 should produce size 8px");

        ParticleSystem ps3 = createOnEdt();
        ps3.emit(60, 64);
        float midSize = ps3.getParticleSize(0);
        assertTrue(midSize > 1f && midSize < 8f,
                "vel=64 should produce size between 1 and 8, got " + midSize);
    }

    // ------------------------------------------------------------------
    // Test 4: 200-particle cap with merge-on-overflow
    // ------------------------------------------------------------------

    @Test
    void testMaxParticlesCap() throws Exception {
        ParticleSystem ps = createOnEdt();

        // Emit MAX_PARTICLES + 10 times — cap + merge should keep count <= MAX
        int totalEmits = ParticleSystem.MAX_PARTICLES + 10;
        for (int i = 0; i < totalEmits; i++) {
            ps.emit(i % 128, 64);
        }

        assertTrue(ps.getParticleCount() <= ParticleSystem.MAX_PARTICLES,
                "Particle count should never exceed MAX_PARTICLES. "
                + "Got " + ps.getParticleCount() + " after " + totalEmits + " emits");
    }

    // ------------------------------------------------------------------
    // Test 5: contains() returns false (mouse transparency)
    // ------------------------------------------------------------------

    @Test
    void testContainsReturnsFalse() throws Exception {
        ParticleSystem ps = createOnEdt();

        // D-09: GlassPane is transparent to mouse events
        assertFalse(ps.contains(10, 10),
                "Mouse events should pass through — contains() must return false");
        assertFalse(ps.contains(100, 200),
                "Mouse events should pass through at any coordinate");
        assertFalse(ps.contains(-1, -1),
                "Mouse events should pass through at any coordinate");
    }

    // ------------------------------------------------------------------
    // Test 6: color from NoteColorMapper
    // ------------------------------------------------------------------

    @Test
    void testColorFromNoteColorMapper() throws Exception {
        ParticleSystem ps = createOnEdt();

        int[] testNotes = {0, 36, 60, 72, 100, 127};
        for (int i = 0; i < testNotes.length; i++) {
            int note = testNotes[i];
            ps.emit(note, 100);
            Color expected = NoteColorMapper.forPitch(note);
            Color actual = ps.getParticleColor(i);
            assertEquals(expected, actual,
                    "Particle color for note " + note + " should match NoteColorMapper");
        }
    }

    // ------------------------------------------------------------------
    // Helper: create ParticleSystem on EDT
    // ------------------------------------------------------------------

    /**
     * Creates a ParticleSystem on the EDT thread and returns it.
     * This ensures proper Swing component initialization.
     */
    private static ParticleSystem createOnEdt() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return new ParticleSystem();
        }
        final ParticleSystem[] holder = new ParticleSystem[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new ParticleSystem());
        return holder[0];
    }
}
