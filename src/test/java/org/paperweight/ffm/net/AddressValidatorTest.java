package org.paperweight.ffm.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressValidatorTest {

    @Test
    void acceptsIpv4Literals() {
        assertTrue(AddressValidator.isIpv4Literal("192.168.3.6"));
        assertTrue(AddressValidator.isIpv4Literal("0.0.0.0"));
        assertTrue(AddressValidator.isIpv4Literal("255.255.255.255"));
        assertTrue(AddressValidator.isIpv4Literal(" 10.0.0.1 "));
    }

    @Test
    void rejectsInvalidIpv4Literals() {
        assertFalse(AddressValidator.isIpv4Literal("256.1.1.1"));
        assertFalse(AddressValidator.isIpv4Literal("1.2.3"));
        assertFalse(AddressValidator.isIpv4Literal("1.2.3.4.5"));
        assertFalse(AddressValidator.isIpv4Literal("example.com"));
        assertFalse(AddressValidator.isIpv4Literal(""));
        assertFalse(AddressValidator.isIpv4Literal(null));
        assertFalse(AddressValidator.isIpv4Literal("::1"));
    }

    @Test
    void acceptsIpv6Literals() {
        assertTrue(AddressValidator.isIpv6Literal("::"));
        assertTrue(AddressValidator.isIpv6Literal("::1"));
        assertTrue(AddressValidator.isIpv6Literal("2001:db8::1"));
        assertTrue(AddressValidator.isIpv6Literal("fd00:0:0:0:0:0:0:1"));
    }

    @Test
    void rejectsInvalidIpv6Literals() {
        assertFalse(AddressValidator.isIpv6Literal("192.168.3.6"));
        assertFalse(AddressValidator.isIpv6Literal("gggg::1"));
        assertFalse(AddressValidator.isIpv6Literal("example.com"));
        assertFalse(AddressValidator.isIpv6Literal("a:b"));
        assertFalse(AddressValidator.isIpv6Literal(null));
    }

    @Test
    void detectsWildcardAddresses() {
        assertTrue(AddressValidator.isWildcardAddress("0.0.0.0"));
        assertTrue(AddressValidator.isWildcardAddress("::"));
        assertTrue(AddressValidator.isWildcardAddress("*"));
        assertTrue(AddressValidator.isWildcardAddress("any"));
        assertFalse(AddressValidator.isWildcardAddress("192.168.3.6"));
        assertFalse(AddressValidator.isWildcardAddress(null));
    }

    @Test
    void doesNotTreatAsteriskAsAddressLiteral() {
        assertFalse(AddressValidator.isIpv4Literal("*"));
        assertFalse(AddressValidator.isIpv6Literal("*"));
        assertFalse(AddressValidator.matchesFamily("*", false));
    }

    @Test
    void matchesAddressFamily() {
        assertTrue(AddressValidator.matchesFamily("192.168.3.6", false));
        assertFalse(AddressValidator.matchesFamily("192.168.3.6", true));
        assertTrue(AddressValidator.matchesFamily("::", true));
        assertFalse(AddressValidator.matchesFamily("::", false));
    }

    @Test
    void stripsScopeId() {
        assertEquals("fe80::1", AddressValidator.stripScopeId("fe80::1%12"));
        assertEquals("192.168.3.6", AddressValidator.stripScopeId("192.168.3.6"));
    }
}
