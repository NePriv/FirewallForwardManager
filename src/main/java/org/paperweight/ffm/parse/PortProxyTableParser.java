package org.paperweight.ffm.parse;

import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.ProxyType;
import org.paperweight.ffm.net.AddressValidator;
import org.paperweight.ffm.net.Ports;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 解析 {@code netsh interface portproxy show <类型>} 的输出。 */
public final class PortProxyTableParser {

    private static final Pattern ROW =
            Pattern.compile("^\\s*(\\S+)\\s+(\\d{1,5})\\s+(\\S+)\\s+(\\d{1,5})\\s*$");

    private PortProxyTableParser() {
    }

    public static List<ProxyEntry> parse(String output, ProxyType type) {
        List<ProxyEntry> entries = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return entries;
        }
        // 表头与分隔线随系统语言变化，只按数据行形态（地址 端口 地址 端口）识别
        for (String line : output.split("\\R")) {
            ProxyEntry entry = parseRow(line, type);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    static ProxyEntry parseRow(String line, ProxyType type) {
        Matcher matcher = ROW.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        String listenAddress = normalizeAddress(matcher.group(1));
        String connectAddress = normalizeAddress(matcher.group(3));
        if (listenAddress == null || connectAddress == null) {
            return null;
        }
        int listenPort = Integer.parseInt(matcher.group(2));
        int connectPort = Integer.parseInt(matcher.group(4));
        if (!Ports.isValid(listenPort) || !Ports.isValid(connectPort)) {
            return null;
        }
        return new ProxyEntry(type, listenAddress, listenPort, connectAddress, connectPort);
    }

    private static String normalizeAddress(String token) {
        String text = token.strip();
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1);
        }
        if (text.isEmpty() || "-".equals(text)) {
            return null;
        }
        // “监听所有地址”在 netsh 输出中显示为 *，直接原样保留，删除时再尝试等价形式
        return "*".equals(text) ? text : AddressValidator.stripScopeId(text);
    }
}
