package org.paperweight.ffm.net;

/** IP 地址字面量校验工具。 */
public final class AddressValidator {

    private static final java.util.regex.Pattern IPV4_PATTERN =
            java.util.regex.Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final java.util.regex.Pattern IPV6_CHARS =
            java.util.regex.Pattern.compile("^[0-9A-Fa-f:.]+$");

    private AddressValidator() {
    }

    public static boolean isIpv4Literal(String value) {
        if (value == null) {
            return false;
        }
        java.util.regex.Matcher matcher = IPV4_PATTERN.matcher(value.strip());
        if (!matcher.matches()) {
            return false;
        }
        for (int group = 1; group <= 4; group++) {
            if (Integer.parseInt(matcher.group(group)) > 255) {
                return false;
            }
        }
        return true;
    }

    public static boolean isIpv6Literal(String value) {
        if (value == null) {
            return false;
        }
        String text = value.strip();
        // 先按字符集过滤，避免 InetAddress 把主机名交给 DNS 解析
        if (!text.contains(":") || !IPV6_CHARS.matcher(text).matches()) {
            return false;
        }
        try {
            return java.net.InetAddress.getByName(text) instanceof java.net.Inet6Address;
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    public static boolean isValidLiteral(String value) {
        return isIpv4Literal(value) || isIpv6Literal(value);
    }

    /** 是否是“监听所有地址”的通配地址。 */
    public static boolean isWildcardAddress(String value) {
        if (value == null) {
            return false;
        }
        String text = value.strip();
        return text.equals("0.0.0.0") || text.equals("::") || text.equals("*") || text.equalsIgnoreCase("any");
    }

    public static boolean matchesFamily(String address, boolean ipv6) {
        return ipv6 ? isIpv6Literal(address) : isIpv4Literal(address);
    }

    /** 去掉 IPv6 的作用域后缀（如 {@code fe80::1%12} 中的 {@code %12}）。 */
    public static String stripScopeId(String address) {
        if (address == null) {
            return null;
        }
        int index = address.indexOf('%');
        return index < 0 ? address : address.substring(0, index);
    }
}
