package org.paperweight.ffm.parse;

import org.junit.jupiter.api.Test;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.ProxyType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 解析器夹具测试：表头随系统语言变化，数据行格式稳定。
 */
class PortProxyTableParserTest {

    private static final String CHINESE_V4 = """
            侦听 ipv4:                 连接到 ipv4:

            地址            端口        地址            端口
            --------------- ----------  --------------- ----------
            0.0.0.0         2405       112.11.36.130   10010
            192.168.3.6     3389       192.168.3.10    3389
            """;

    private static final String ENGLISH_V4 = """
            Listen on ipv4:             Connect to ipv4:

            Address         Port        Address         Port
            --------------- ----------  --------------- ----------
            0.0.0.0         8080        10.0.0.5        80
            """;

    private static final String CHINESE_V6 = """
            侦听 ipv6:                 连接到 ipv6:

            地址            端口        地址            端口
            --------------- ----------  --------------- ----------
            ::              2405        fd00::1         10010
            2001:db8::5     443         2001:db8::9     443
            """;

    private static final String EMPTY = """
            侦听 ipv4:                 连接到 ipv4:

            地址            端口        地址            端口
            --------------- ----------  --------------- ----------
            """;

    @Test
    void parsesChineseOutput() {
        List<ProxyEntry> entries = PortProxyTableParser.parse(CHINESE_V4, ProxyType.V4_TO_V4);

        assertEquals(2, entries.size());
        assertEquals(new ProxyEntry(ProxyType.V4_TO_V4, "0.0.0.0", 2405, "112.11.36.130", 10010),
                entries.get(0));
        assertEquals(new ProxyEntry(ProxyType.V4_TO_V4, "192.168.3.6", 3389, "192.168.3.10", 3389),
                entries.get(1));
    }

    @Test
    void parsesEnglishOutput() {
        List<ProxyEntry> entries = PortProxyTableParser.parse(ENGLISH_V4, ProxyType.V4_TO_V4);

        assertEquals(1, entries.size());
        assertEquals("10.0.0.5", entries.getFirst().connectAddress());
        assertEquals(80, entries.getFirst().connectPort());
    }

    @Test
    void parsesIpv6Section() {
        List<ProxyEntry> entries = PortProxyTableParser.parse(CHINESE_V6, ProxyType.V6_TO_V6);

        assertEquals(2, entries.size());
        assertEquals("::", entries.get(0).listenAddress());
        assertEquals(10010, entries.get(0).connectPort());
        assertEquals("2001:db8::5", entries.get(1).listenAddress());
    }

    @Test
    void ignoresHeaderAndSeparatorLines() {
        assertTrue(PortProxyTableParser.parse(EMPTY, ProxyType.V4_TO_V4).isEmpty());
    }

    @Test
    void toleratesBlankAndNullOutput() {
        assertTrue(PortProxyTableParser.parse(null, ProxyType.V4_TO_V4).isEmpty());
        assertTrue(PortProxyTableParser.parse("   ", ProxyType.V4_TO_V4).isEmpty());
    }

    @Test
    void ignoresUnrelatedTextAndOutOfRangePorts() {
        String output = """
                系统找不到指定的文件。

                地址            端口        地址            端口
                --------------- ----------  --------------- ----------
                0.0.0.0         99999       112.11.36.130   10010
                """;

        assertTrue(PortProxyTableParser.parse(output, ProxyType.V4_TO_V4).isEmpty());
    }

    @Test
    void parsesWildcardListenAddressWrittenAsAsterisk() {
        // Windows 把省略了 listenaddress 的规则存成 *，netsh 也就显示为 *
        String output = """
                侦听 ipv4:                 连接到 ipv4:


                地址            端口        地址            端口
                --------------- ----------  --------------- ----------
                *               47990       192.168.88.3    47990
                """;

        List<ProxyEntry> entries = PortProxyTableParser.parse(output, ProxyType.V4_TO_V4);

        assertEquals(1, entries.size());
        assertEquals("*", entries.getFirst().listenAddress());
        assertEquals(47990, entries.getFirst().listenPort());
        assertEquals("192.168.88.3", entries.getFirst().connectAddress());
    }

    @Test
    void stripsBracketsAroundIpv6Addresses() {
        String output = """
                地址            端口        地址            端口
                --------------- ----------  --------------- ----------
                [::1]           2405        [fd00::1]       10010
                """;

        List<ProxyEntry> entries = PortProxyTableParser.parse(output, ProxyType.V6_TO_V6);

        assertEquals(1, entries.size());
        assertEquals("::1", entries.getFirst().listenAddress());
        assertEquals("fd00::1", entries.getFirst().connectAddress());
    }
}
