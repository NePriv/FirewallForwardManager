package org.paperweight.ffm.model;

import java.util.Objects;

/** 一条已存在的端口转发记录（不含协议，netsh 的转发列表不显示协议）。 */
public record ProxyEntry(
        ProxyType type,
        String listenAddress,
        int listenPort,
        String connectAddress,
        int connectPort) {

    public ProxyEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listenAddress, "listenAddress");
        Objects.requireNonNull(connectAddress, "connectAddress");
    }

    /** 会话内识别同一转发的键。 */
    public String key() {
        return type.name() + '|' + listenAddress + '|' + listenPort;
    }

    public String listenEndpoint() {
        return formatEndpoint(listenAddress, listenPort);
    }

    public String connectEndpoint() {
        return formatEndpoint(connectAddress, connectPort);
    }

    public String describe() {
        return type.displayName() + " " + listenEndpoint() + " → " + connectEndpoint();
    }

    /** 补上协议后得到可用于增删操作的规则。 */
    public ForwardRule withProtocol(TransportProtocol protocol) {
        return new ForwardRule(type, protocol, listenAddress, listenPort, connectAddress, connectPort);
    }

    /** 拼接可读的“地址:端口”，IPv6 地址用方括号包裹。 */
    public static String formatEndpoint(String address, int port) {
        return address.contains(":") ? "[" + address + "]:" + port : address + ":" + port;
    }
}
