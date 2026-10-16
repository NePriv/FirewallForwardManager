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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * netsh 命令行等价的文本测试，确保生成的命令与手工执行的一致。
 */
class NetshCommandsTest {

    private static final ForwardRule RULE = new ForwardRule(
            ProxyType.V4_TO_V4, TransportProtocol.TCP, "192.168.3.6", 2405, "112.11.36.130", 10010);

    @Test
    void buildsPortProxyAddCommand() {
        assertEquals("netsh.exe interface portproxy add v4tov4 listenport=2405 listenaddress=192.168.3.6"
                        + " connectport=10010 connectaddress=112.11.36.130 protocol=tcp",
                CommandResult.format(NetshCommands.portProxyAdd(RULE)));
    }

    @Test
    void buildsPortProxyDeleteCommand() {
        ProxyEntry entry = RULE.toEntry();

        assertEquals("netsh.exe interface portproxy delete v4tov4 listenport=2405 listenaddress=192.168.3.6"
                        + " protocol=tcp",
                CommandResult.format(NetshCommands.portProxyDelete(entry, TransportProtocol.TCP)));
        assertEquals("netsh.exe interface portproxy delete v4tov4 listenport=2405 listenaddress=192.168.3.6"
                        + " protocol=udp",
                CommandResult.format(NetshCommands.portProxyDelete(entry, TransportProtocol.UDP)));
    }

    @Test
    void buildsFirewallRuleWithLocalAndRemoteAddress() {
        List<String> command = NetshCommands.firewallAddInbound(
                "FFM-IN-TCP-2405-192.168.3.6", TransportProtocol.TCP, 2405, "192.168.3.6", "112.11.36.130");

        assertEquals("netsh.exe advfirewall firewall add rule name=FFM-IN-TCP-2405-192.168.3.6 dir=in"
                        + " action=allow protocol=tcp localport=2405 localip=192.168.3.6"
                        + " remoteip=112.11.36.130 profile=any enable=yes",
                CommandResult.format(command));
    }

    @Test
    void omitsWildcardLocalAddressFromFirewallRule() {
        List<String> command = NetshCommands.firewallAddInbound(
                "FFM-IN-TCP-2405-0.0.0.0", TransportProtocol.TCP, 2405, null, null);

        assertFalse(command.stream().anyMatch(token -> token.startsWith("localip=")));
        assertFalse(command.stream().anyMatch(token -> token.startsWith("remoteip=")));
    }

    @Test
    void generatesReadableIpv4RuleName() {
        assertEquals("FFM-IN-TCP-2405-192.168.3.6",
                NetshCommands.firewallRuleName(TransportProtocol.TCP, 2405, "192.168.3.6"));
        assertEquals("FFM-IN-UDP-53-0.0.0.0",
                NetshCommands.firewallRuleName(TransportProtocol.UDP, 53, "0.0.0.0"));
    }

    @Test
    void generatesSafeDeterministicIpv6RuleName() {
        String first = NetshCommands.firewallRuleName(TransportProtocol.TCP, 2405, "::1");
        String second = NetshCommands.firewallRuleName(TransportProtocol.TCP, 2405, "::1");
        String other = NetshCommands.firewallRuleName(TransportProtocol.TCP, 2405, "fd00::1");

        assertEquals(first, second);
        assertNotEquals(first, other);
        assertTrue(first.startsWith("FFM-IN-TCP-2405-v6-"));
        assertFalse(first.contains(":"));
        assertFalse(first.contains(" "));
    }

    @Test
    void buildsEquivalentDeleteVariantsForExplicitAddress() {
        List<List<String>> variants = NetshCommands.portProxyDeleteVariants(RULE.toEntry(), TransportProtocol.TCP);

        assertEquals(1, variants.size());
        assertEquals("netsh.exe interface portproxy delete v4tov4 listenport=2405 listenaddress=192.168.3.6 protocol=tcp",
                CommandResult.format(variants.getFirst()));
    }

    @Test
    void buildsEquivalentDeleteVariantsForWildcardRules() {
        List<String> rendered = NetshCommands
                .portProxyDeleteVariants(new ProxyEntry(ProxyType.V4_TO_V4, "*", 47990, "192.168.88.3", 47990),
                        TransportProtocol.UDP)
                .stream()
                .map(CommandResult::format)
                .toList();

        assertEquals(List.of(
                "netsh.exe interface portproxy delete v4tov4 listenport=47990 listenaddress=* protocol=udp",
                "netsh.exe interface portproxy delete v4tov4 listenport=47990 listenaddress=0.0.0.0 protocol=udp",
                "netsh.exe interface portproxy delete v4tov4 listenport=47990 protocol=udp"), rendered);
    }

    @Test
    void wildcardDeleteVariantsForIpv6UseDoubleColon() {
        List<String> rendered = NetshCommands
                .portProxyDeleteVariants(new ProxyEntry(ProxyType.V6_TO_V6, "::", 2405, "fd00::1", 10010),
                        TransportProtocol.TCP)
                .stream()
                .map(CommandResult::format)
                .toList();

        assertEquals(List.of(
                "netsh.exe interface portproxy delete v6tov6 listenport=2405 listenaddress=:: protocol=tcp",
                "netsh.exe interface portproxy delete v6tov6 listenport=2405 listenaddress=* protocol=tcp",
                "netsh.exe interface portproxy delete v6tov6 listenport=2405 protocol=tcp"), rendered);
    }

    @Test
    void showAndServiceCommandsAreStable() {
        assertEquals("netsh.exe interface portproxy show v4tov4",
                CommandResult.format(NetshCommands.portProxyShow(ProxyType.V4_TO_V4)));
        assertEquals("netsh.exe interface portproxy show v6tov6",
                CommandResult.format(NetshCommands.portProxyShow(ProxyType.V6_TO_V6)));
        assertEquals("netsh.exe advfirewall firewall show rule name=all",
                CommandResult.format(NetshCommands.firewallShowAllRules()));
        assertEquals("netsh.exe advfirewall firewall delete rule name=FFM-IN-TCP-2405-192.168.3.6",
                CommandResult.format(NetshCommands.firewallDeleteRule("FFM-IN-TCP-2405-192.168.3.6")));
        assertEquals("sc.exe query iphlpsvc", CommandResult.format(NetshCommands.serviceQuery("iphlpsvc")));
        assertEquals("sc.exe start iphlpsvc", CommandResult.format(NetshCommands.serviceStart("iphlpsvc")));
    }
}
