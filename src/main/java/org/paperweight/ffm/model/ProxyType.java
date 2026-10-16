package org.paperweight.ffm.model;

/**
 * netsh interface portproxy 支持的转发方向组合。
 */
public enum ProxyType {

    /** 本机 IPv4 监听 → IPv4 目标（最常用）。 */
    V4_TO_V4("IPv4 → IPv4", "v4tov4", false, false),
    /** 本机 IPv6 监听 → IPv6 目标。 */
    V6_TO_V6("IPv6 → IPv6", "v6tov6", true, true),
    /** 本机 IPv6 监听 → IPv4 目标。 */
    V6_TO_V4("IPv6 → IPv4", "v6tov4", true, false),
    /** 本机 IPv4 监听 → IPv6 目标。 */
    V4_TO_V6("IPv4 → IPv6", "v4tov6", false, true);

    private final String displayName;
    private final String netshKeyword;
    private final boolean listenIpv6;
    private final boolean connectIpv6;

    ProxyType(String displayName, String netshKeyword, boolean listenIpv6, boolean connectIpv6) {
        this.displayName = displayName;
        this.netshKeyword = netshKeyword;
        this.listenIpv6 = listenIpv6;
        this.connectIpv6 = connectIpv6;
    }

    public String displayName() {
        return displayName;
    }

    /** netsh 子命令关键字，如 {@code v4tov4}。 */
    public String netshKeyword() {
        return netshKeyword;
    }

    /** 监听地址是否为 IPv6。 */
    public boolean listenIpv6() {
        return listenIpv6;
    }

    /** 转发目标地址是否为 IPv6。 */
    public boolean connectIpv6() {
        return connectIpv6;
    }

    /** “监听所有地址”时使用的通配地址。 */
    public String wildcardListenAddress() {
        return listenIpv6 ? "::" : "0.0.0.0";
    }

    public static ProxyType fromNetshKeyword(String keyword) {
        for (ProxyType type : values()) {
            if (type.netshKeyword.equalsIgnoreCase(keyword)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的转发类型关键字: " + keyword);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
