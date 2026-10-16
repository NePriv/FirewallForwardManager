package org.paperweight.ffm.service;

import org.junit.jupiter.api.Test;
import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.model.ForwardRule;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.ProxyType;
import org.paperweight.ffm.model.TransportProtocol;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyEntryTest {

    private static final ProxyEntry IPV4_ENTRY =
            new ProxyEntry(ProxyType.V4_TO_V4, "192.168.3.6", 2405, "112.11.36.130", 10010);

    @Test
    void formatsEndpointsForIpv4() {
        assertEquals("192.168.3.6:2405", IPV4_ENTRY.listenEndpoint());
        assertEquals("112.11.36.130:10010", IPV4_ENTRY.connectEndpoint());
    }

    @Test
    void wrapsIpv6EndpointsInBrackets() {
        ProxyEntry entry = new ProxyEntry(ProxyType.V6_TO_V6, "::1", 2405, "fd00::1", 10010);

        assertEquals("[::1]:2405", entry.listenEndpoint());
        assertEquals("[fd00::1]:10010", entry.connectEndpoint());
        assertTrue(entry.describe().contains("IPv6 → IPv6"));
    }

    @Test
    void keyIgnoresConnectSideAndProtocol() {
        ForwardRule rule = IPV4_ENTRY.withProtocol(TransportProtocol.UDP);

        assertEquals(IPV4_ENTRY.key(), rule.toEntry().key());
        assertEquals(TransportProtocol.UDP, rule.protocol());
    }

    @Test
    void detectsWildcardListenAddress() {
        assertTrue(new ForwardRule(ProxyType.V4_TO_V4, TransportProtocol.TCP, "0.0.0.0", 80, "10.0.0.1", 80)
                .listenOnWildcardAddress());
        assertTrue(new ForwardRule(ProxyType.V6_TO_V6, TransportProtocol.TCP, "::", 80, "fd00::1", 80)
                .listenOnWildcardAddress());
        assertFalse(new ForwardRule(ProxyType.V4_TO_V4, TransportProtocol.TCP, "192.168.3.6", 80, "10.0.0.1", 80)
                .listenOnWildcardAddress());
    }
}
