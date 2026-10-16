package org.paperweight.ffm.model;

import org.paperweight.ffm.net.AddressValidator;

import java.util.Objects;

/** 待创建的转发规则：本机监听 → 远端目标，并携带协议。 */
public record ForwardRule(
        ProxyType type,
        TransportProtocol protocol,
        String listenAddress,
        int listenPort,
        String connectAddress,
        int connectPort) {

    public ForwardRule {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(listenAddress, "listenAddress");
        Objects.requireNonNull(connectAddress, "connectAddress");
    }

    public ProxyEntry toEntry() {
        return new ProxyEntry(type, listenAddress, listenPort, connectAddress, connectPort);
    }

    /** 监听地址是否为通配地址。 */
    public boolean listenOnWildcardAddress() {
        return AddressValidator.isWildcardAddress(listenAddress);
    }

    public String describe() {
        return toEntry().describe() + "（" + protocol.displayName() + "）";
    }
}
