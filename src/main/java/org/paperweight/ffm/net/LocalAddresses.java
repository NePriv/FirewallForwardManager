package org.paperweight.ffm.net;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 列出本机可用的 IP 地址，供界面下拉选择监听地址。
 */
public final class LocalAddresses {

    /**
     * 一个本机地址。
     *
     * @param text          地址文本（IPv6 已去掉作用域后缀）
     * @param ipv6          是否为 IPv6 地址
     * @param interfaceName 所属网卡名称，用于提示
     */
    public record Address(String text, boolean ipv6, String interfaceName) {
        @Override
        public String toString() {
            return text;
        }
    }

    private LocalAddresses() {
    }

    public static List<Address> list() {
        Map<String, Address> unique = new LinkedHashMap<>();
        Enumeration<NetworkInterface> interfaces = interfaces();
        if (interfaces == null) {
            return List.of();
        }
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!isUp(networkInterface)) {
                continue;
            }
            String interfaceName = displayName(networkInterface);
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress address = interfaceAddress.getAddress();
                if (address == null || address.isMulticastAddress() || address.isLinkLocalAddress()
                        || address.isLoopbackAddress() || address.isAnyLocalAddress()) {
                    continue;
                }
                String text = AddressValidator.stripScopeId(address.getHostAddress());
                unique.putIfAbsent(text, new Address(text, address instanceof Inet6Address, interfaceName));
            }
        }
        List<Address> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(Address::ipv6).thenComparing(Address::text));
        return List.copyOf(result);
    }

    private static Enumeration<NetworkInterface> interfaces() {
        try {
            return NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }
    }

    private static boolean isUp(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp();
        } catch (SocketException e) {
            return false;
        }
    }

    private static String displayName(NetworkInterface networkInterface) {
        String display = networkInterface.getDisplayName();
        return display == null || display.isBlank() ? networkInterface.getName() : display;
    }
}
