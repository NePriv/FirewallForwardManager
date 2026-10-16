package org.paperweight.ffm.exec;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 代码页解析与映射测试。 */
class ConsoleCharsetTest {

    @Test
    void parsesChineseChcpOutput() {
        assertEquals(936, ConsoleCharset.parseCodePage("活动代码页: 936"));
        assertEquals(65001, ConsoleCharset.parseCodePage("活动代码页: 65001"));
    }

    @Test
    void parsesEnglishChcpOutput() {
        assertEquals(437, ConsoleCharset.parseCodePage("Active code page: 437"));
    }

    @Test
    void parsesLastNumberFromOutput() {
        assertEquals(936, ConsoleCharset.parseCodePage("cmd.exe 版本 10.0.22631\n活动代码页: 936"));
    }

    @Test
    void returnsNullWhenNoNumberFound() {
        assertNull(ConsoleCharset.parseCodePage(null));
        assertNull(ConsoleCharset.parseCodePage(""));
        assertNull(ConsoleCharset.parseCodePage("no numbers here"));
    }

    @Test
    void mapsKnownCodePages() {
        assertEquals(Charset.forName("GBK").name(), ConsoleCharset.byCodePage(936).name());
        assertEquals(StandardCharsets.UTF_8, ConsoleCharset.byCodePage(65001));
        assertEquals(Charset.forName("Big5").name(), ConsoleCharset.byCodePage(950).name());
        assertEquals(Charset.forName("IBM437").name(), ConsoleCharset.byCodePage(437).name());
        assertEquals(Charset.forName("windows-1252").name(), ConsoleCharset.byCodePage(1252).name());
    }

    @Test
    void returnsNullForUnknownCodePage() {
        assertNull(ConsoleCharset.byCodePage(99999));
        assertNull(ConsoleCharset.byCodePage(1));
    }

    @Test
    void detectsACharsetAtRuntime() {
        assertNotNull(ConsoleCharset.forCommands());
    }
}
