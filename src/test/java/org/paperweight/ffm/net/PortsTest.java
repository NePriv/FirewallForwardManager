package org.paperweight.ffm.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortsTest {

    @Test
    void parsesValidNumbers() {
        assertEquals(2405, Ports.parse("2405"));
        assertEquals(80, Ports.parse(" 80 "));
        assertEquals(0, Ports.parse("0"));
    }

    @Test
    void rejectsNonNumericInput() {
        assertNull(Ports.parse(""));
        assertNull(Ports.parse("abc"));
        assertNull(Ports.parse("-1"));
        assertNull(Ports.parse("80.5"));
        assertNull(Ports.parse(null));
        assertNull(Ports.parse("999999999999"));
    }

    @Test
    void validatesRange() {
        assertTrue(Ports.isValid(1));
        assertTrue(Ports.isValid(65535));
        assertFalse(Ports.isValid(0));
        assertFalse(Ports.isValid(65536));
    }
}
