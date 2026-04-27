package com.rebeatbox.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class NoteColorMapperTest {

    @Test
    void shouldReturnNonNullColorForValidPitch() {
        Color color = NoteColorMapper.forPitch(60);
        assertNotNull(color);
    }

    @Test
    void shouldReturnPurpleishColorForLowPitch() {
        // Pitch 0 (C-1) — lowest note, should be purple (hue ~0.70)
        Color color = NoteColorMapper.forPitch(0);
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        // Hue should be in the purple range: 0.65-0.85 (purple/violet/blue)
        assertTrue(hsb[0] >= 0.60f && hsb[0] <= 0.85f,
            "Pitch 0 should have purple-ish hue, got " + hsb[0]);
    }

    @Test
    void shouldReturnRedishColorForHighPitch() {
        // Pitch 127 (G9) — highest note, should be red (hue ~0.0)
        Color color = NoteColorMapper.forPitch(127);
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        // Hue should be in the red range: 0.0-0.05 or 0.95-1.0
        boolean isRed = (hsb[0] >= 0.0f && hsb[0] <= 0.05f)
                     || (hsb[0] >= 0.95f && hsb[0] <= 1.0f);
        assertTrue(isRed, "Pitch 127 should have red-ish hue, got " + hsb[0]);
    }

    @Test
    void shouldReturnVisiblyDifferentColorsForPianoLowAndHigh() {
        // A0 = 21 (low piano), C8 = 108 (high piano)
        Color low = NoteColorMapper.forPitch(21);
        Color high = NoteColorMapper.forPitch(108);

        float[] lowHsb = Color.RGBtoHSB(low.getRed(), low.getGreen(), low.getBlue(), null);
        float[] highHsb = Color.RGBtoHSB(high.getRed(), high.getGreen(), high.getBlue(), null);

        // Hues should differ significantly (at least 0.15 apart on 0-1 scale)
        float hueDiff = Math.abs(lowHsb[0] - highHsb[0]);
        assertTrue(hueDiff >= 0.15f,
            "A0 and C8 should have visibly different hues, diff=" + hueDiff
            + " (low=" + lowHsb[0] + " high=" + highHsb[0] + ")");
    }

    @Test
    void shouldHaveMonotonicallyDecreasingHueForAscendingPitches() {
        // As pitch increases, hue should decrease (rainbow: purple→blue→green→yellow→orange→red)
        // Pitch 0 = hue ~0.70, Pitch 127 = hue ~0.0
        Color low = NoteColorMapper.forPitch(0);
        Color mid = NoteColorMapper.forPitch(64);
        Color high = NoteColorMapper.forPitch(127);

        float lowHue = Color.RGBtoHSB(low.getRed(), low.getGreen(), low.getBlue(), null)[0];
        float midHue = Color.RGBtoHSB(mid.getRed(), mid.getGreen(), mid.getBlue(), null)[0];
        float highHue = Color.RGBtoHSB(high.getRed(), high.getGreen(), high.getBlue(), null)[0];

        assertTrue(lowHue >= midHue,
            "Low pitch hue (" + lowHue + ") should be >= mid pitch hue (" + midHue + ")");
        assertTrue(midHue >= highHue,
            "Mid pitch hue (" + midHue + ") should be >= high pitch hue (" + highHue + ")");
    }

    @Test
    void shouldRejectNegativePitch() {
        assertThrows(IllegalArgumentException.class, () ->
            NoteColorMapper.forPitch(-1));
    }

    @Test
    void shouldRejectPitchAbove127() {
        assertThrows(IllegalArgumentException.class, () ->
            NoteColorMapper.forPitch(128));
    }

    @Test
    void shouldHaveHighSaturationAndBrightnessForNeonEffect() {
        // Test a range of pitches to ensure neon aesthetic (saturation >= 0.7, brightness >= 0.8)
        for (int pitch : new int[]{0, 21, 48, 60, 72, 96, 108, 127}) {
            Color color = NoteColorMapper.forPitch(pitch);
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            assertTrue(hsb[1] >= 0.7f,
                "Pitch " + pitch + " saturation " + hsb[1] + " should be >= 0.7");
            assertTrue(hsb[2] >= 0.8f,
                "Pitch " + pitch + " brightness " + hsb[2] + " should be >= 0.8");
        }
    }

    @Test
    void shouldDistinguishSameNoteInDifferentOctaves() {
        // C4 (60) and C5 (72) — same note class, different octave
        Color c4 = NoteColorMapper.forPitch(60);
        Color c5 = NoteColorMapper.forPitch(72);

        assertNotEquals(c4, c5, "C4 and C5 should have different colors");
    }

    @Test
    void shouldReturnSameColorForSamePitch() {
        Color a = NoteColorMapper.forPitch(60);
        Color b = NoteColorMapper.forPitch(60);
        assertEquals(a, b, "Same pitch should return same color");
    }

    @Test
    void shouldDistinguishAdjacentSemitones() {
        // Adjacent semitones should have perceptibly different hues
        Color c60 = NoteColorMapper.forPitch(60);
        Color c61 = NoteColorMapper.forPitch(61);

        float h60 = Color.RGBtoHSB(c60.getRed(), c60.getGreen(), c60.getBlue(), null)[0];
        float h61 = Color.RGBtoHSB(c61.getRed(), c61.getGreen(), c61.getBlue(), null)[0];

        // At least ~3 degrees of hue difference (3/360 ≈ 0.008)
        float hueDiff = Math.abs(h60 - h61);
        assertTrue(hueDiff >= 0.005f,
            "Adjacent semitones should differ in hue, diff=" + hueDiff
            + " (C4=" + h60 + ", C#4=" + h61 + ")");
    }
}
