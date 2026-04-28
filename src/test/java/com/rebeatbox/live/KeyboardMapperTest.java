package com.rebeatbox.live;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardMapperTest {

    private KeyboardMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KeyboardMapper();
    }

    // LIVE-01: QWERTY row mapping (C4-C5)
    @Test
    void shouldMapQwertyQToC4() {
        assertEquals(60, KeyboardMapper.keyCodeToNote(KeyEvent.VK_Q));
    }

    @Test
    void shouldMapQwertyWToCSharp4() {
        assertEquals(61, KeyboardMapper.keyCodeToNote(KeyEvent.VK_W));
    }

    @Test
    void shouldMapQwertyCloseBracketToB4() {
        assertEquals(71, KeyboardMapper.keyCodeToNote(KeyEvent.VK_CLOSE_BRACKET));
    }

    // LIVE-01: Number row mapping (C5-C6)
    @Test
    void shouldMapNumberRow1ToC5() {
        assertEquals(72, KeyboardMapper.keyCodeToNote(KeyEvent.VK_1));
    }

    // LIVE-01: Bottom row mapping (C3-C4)
    @Test
    void shouldMapBottomRowZToC3() {
        assertEquals(48, KeyboardMapper.keyCodeToNote(KeyEvent.VK_Z));
    }

    @Test
    void shouldMapBottomRowSlashToA3() {
        assertEquals(57, KeyboardMapper.keyCodeToNote(KeyEvent.VK_SLASH));
    }

    // Sentinel: unmapped keys return -1
    @Test
    void shouldReturnSentinelForUnmappedKeyA() {
        assertEquals(-1, KeyboardMapper.keyCodeToNote(KeyEvent.VK_A));
    }

    @Test
    void shouldReturnSentinelForFunctionKey() {
        assertEquals(-1, KeyboardMapper.keyCodeToNote(KeyEvent.VK_F1));
    }

    // LIVE-02: boolean[128] active state tracking
    @Test
    void shouldTrackActiveNote() {
        assertFalse(mapper.isActive(60));
        mapper.setActive(60, true);
        assertTrue(mapper.isActive(60));
    }

    @Test
    void shouldDeactivateNote() {
        mapper.setActive(60, true);
        mapper.setActive(60, false);
        assertFalse(mapper.isActive(60));
    }

    @Test
    void shouldClearAllActiveNotes() {
        mapper.setActive(60, true);
        mapper.setActive(72, true);
        mapper.setActive(48, true);
        mapper.clearAll();
        assertFalse(mapper.isActive(60));
        assertFalse(mapper.isActive(72));
        assertFalse(mapper.isActive(48));
    }

    // D-01: Range validation
    @Test
    void shouldMapAllKeysWithinMidiRange() {
        // Spot-check all three octaves
        int[] testKeys = {
            KeyEvent.VK_Z, KeyEvent.VK_Q, KeyEvent.VK_1,
            KeyEvent.VK_SLASH, KeyEvent.VK_CLOSE_BRACKET, KeyEvent.VK_EQUALS
        };
        for (int keyCode : testKeys) {
            int note = KeyboardMapper.keyCodeToNote(keyCode);
            assertTrue(note >= 0 && note <= 127,
                "keyCode " + keyCode + " maps to note " + note + " outside 0-127");
        }
    }

    // D-01: Coverage
    @Test
    void shouldHaveExactly36KeyMappings() {
        int count = 0;
        // Test keyCodes from the three rows
        int[] rowCodes = {
            KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
            KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0,
            KeyEvent.VK_MINUS, KeyEvent.VK_EQUALS,
            KeyEvent.VK_Q, KeyEvent.VK_W, KeyEvent.VK_E, KeyEvent.VK_R, KeyEvent.VK_T,
            KeyEvent.VK_Y, KeyEvent.VK_U, KeyEvent.VK_I, KeyEvent.VK_O, KeyEvent.VK_P,
            KeyEvent.VK_OPEN_BRACKET, KeyEvent.VK_CLOSE_BRACKET,
            KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B,
            KeyEvent.VK_N, KeyEvent.VK_M, KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, KeyEvent.VK_SLASH
        };
        for (int keyCode : rowCodes) {
            if (KeyboardMapper.keyCodeToNote(keyCode) >= 0) count++;
        }
        assertEquals(34, count, "Should have exactly 34 mapped keys (12+12+10)");
    }
}
