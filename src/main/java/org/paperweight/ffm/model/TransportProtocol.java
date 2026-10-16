package org.paperweight.ffm.model;

/** 传输层协议，对应 netsh 的 protocol 参数。 */
public enum TransportProtocol {

    TCP("TCP", "tcp"),
    UDP("UDP", "udp");

    private final String displayName;
    private final String netshValue;

    TransportProtocol(String displayName, String netshValue) {
        this.displayName = displayName;
        this.netshValue = netshValue;
    }

    public String displayName() {
        return displayName;
    }

    public String netshValue() {
        return netshValue;
    }

    public static TransportProtocol fromNetshValue(String value) {
        for (TransportProtocol protocol : values()) {
            if (protocol.netshValue.equalsIgnoreCase(value)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("未知的协议: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
