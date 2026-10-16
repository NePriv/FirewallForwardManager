package org.paperweight.ffm.service;

import org.paperweight.ffm.model.ForwardRule;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.ProxyType;
import org.paperweight.ffm.model.TransportProtocol;
import org.paperweight.ffm.net.AddressValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** 集中构造 netsh / sc 命令行，便于单元测试、日志展示与手工复现。 */
public final class NetshCommands {

    public static final String NETSH = "netsh.exe";
    public static final String IP_HELPER_SERVICE = "iphlpsvc";
    /** 本工具创建的防火墙规则统一前缀，用于识别与清理。 */
    public static final String RULE_PREFIX = "FFM-IN-";

    private NetshCommands() {
    }

    // 直接以参数数组调用 netsh.exe，不经过 cmd.exe，因此无需处理 cmd 的引号规则
    public static List<String> portProxyShow(ProxyType type) {
        return List.of(NETSH, "interface", "portproxy", "show", type.netshKeyword());
    }

    public static List<String> portProxyAdd(ForwardRule rule) {
        return List.of(NETSH, "interface", "portproxy", "add", rule.type().netshKeyword(),
                "listenport=" + rule.listenPort(),
                "listenaddress=" + rule.listenAddress(),
                "connectport=" + rule.connectPort(),
                "connectaddress=" + rule.connectAddress(),
                "protocol=" + rule.protocol().netshValue());
    }

    /** 构造指定监听地址的删除命令。 */
    public static List<String> portProxyDelete(ProxyEntry entry, TransportProtocol protocol) {
        return portProxyDelete(entry, protocol, entry.listenAddress());
    }

    /** 返回删除同一条规则的多种等价命令形式。 */
    public static List<List<String>> portProxyDeleteVariants(ProxyEntry entry, TransportProtocol protocol) {
        List<List<String>> variants = new ArrayList<>();
        // “监听所有地址”可能存成 *、0.0.0.0 或省略 listenaddress，逐种尝试以兼容不同来源的规则
        variants.add(portProxyDelete(entry, protocol, entry.listenAddress()));
        if (AddressValidator.isWildcardAddress(entry.listenAddress())) {
            String explicitWildcard = entry.type().wildcardListenAddress();
            if (!entry.listenAddress().equals(explicitWildcard)) {
                variants.add(portProxyDelete(entry, protocol, explicitWildcard));
            }
            if (!entry.listenAddress().equals("*")) {
                variants.add(portProxyDelete(entry, protocol, "*"));
            }
            variants.add(List.of(NETSH, "interface", "portproxy", "delete", entry.type().netshKeyword(),
                    "listenport=" + entry.listenPort(),
                    "protocol=" + protocol.netshValue()));
        }
        return List.copyOf(variants);
    }

    private static List<String> portProxyDelete(ProxyEntry entry, TransportProtocol protocol, String listenAddress) {
        return List.of(NETSH, "interface", "portproxy", "delete", entry.type().netshKeyword(),
                "listenport=" + entry.listenPort(),
                "listenaddress=" + listenAddress,
                "protocol=" + protocol.netshValue());
    }

    /**
     * 构造放行入站端口的防火墙规则。
     *
     * @param localAddress  监听地址（通配地址时传 null，表示不限本地 IP）
     * @param remoteAddress 允许的远端 IP（null 表示任意来源）
     */
    public static List<String> firewallAddInbound(String ruleName, TransportProtocol protocol,
                                                  int localPort, String localAddress, String remoteAddress) {
        List<String> command = new ArrayList<>(List.of(NETSH, "advfirewall", "firewall", "add", "rule",
                "name=" + ruleName,
                "dir=in",
                "action=allow",
                "protocol=" + protocol.netshValue(),
                "localport=" + localPort));
        if (localAddress != null) {
            command.add("localip=" + localAddress);
        }
        if (remoteAddress != null) {
            command.add("remoteip=" + remoteAddress);
        }
        command.add("profile=any");
        command.add("enable=yes");
        return List.copyOf(command);
    }

    public static List<String> firewallDeleteRule(String ruleName) {
        return List.of(NETSH, "advfirewall", "firewall", "delete", "rule", "name=" + ruleName);
    }

    public static List<String> firewallShowAllRules() {
        return List.of(NETSH, "advfirewall", "firewall", "show", "rule", "name=all");
    }

    public static List<String> serviceQuery(String serviceName) {
        return List.of("sc.exe", "query", serviceName);
    }

    public static List<String> serviceStart(String serviceName) {
        return List.of("sc.exe", "start", serviceName);
    }

    /** 本工具为该转发生成的防火墙规则名，例如 {@code FFM-IN-TCP-2405-192.168.3.6}。 */
    public static String firewallRuleName(TransportProtocol protocol, int listenPort, String listenAddress) {
        return RULE_PREFIX + protocol.displayName() + '-' + listenPort + '-' + addressKey(listenAddress);
    }

    public static String firewallRuleName(ForwardRule rule) {
        return firewallRuleName(rule.protocol(), rule.listenPort(), rule.listenAddress());
    }

    /** 把监听地址压缩为规则名后缀：IPv4 用点分十进制，IPv6 用短哈希。 */
    static String addressKey(String listenAddress) {
        String text = AddressValidator.stripScopeId(listenAddress);
        if (AddressValidator.isIpv4Literal(text)) {
            return text;
        }
        return "v6-" + shortHash(text);
    }

    private static String shortHash(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
