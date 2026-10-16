package org.paperweight.ffm.exec;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 输出解码测试：按字节自动识别编码。 */
class CommandRunnerTest {

    private static final Charset GBK = Charset.forName("GBK");

    @Test
    void decodesNetshStyleUtf8Output() {
        // 实测 netsh 输出为 UTF-8：“侦听” = E4BEA6 E590AC
        byte[] bytes = {(byte) 0xE4, (byte) 0xBE, (byte) 0xA6, (byte) 0xE5, (byte) 0x90, (byte) 0xAC,
                0x20, 'i', 'p', 'v', '4'};

        assertEquals("侦听 ipv4", CommandRunner.decode(bytes, GBK));
    }

    @Test
    void fallsBackForConsoleCodePageOutput() {
        // 实测 whoami 输出为控制台代码页（GBK），这些字节不是合法 UTF-8
        assertEquals("组信息", CommandRunner.decode("组信息".getBytes(GBK), GBK));
    }

    @Test
    void decodesAsciiAndEmptyOutput() {
        assertEquals("SERVICE_NAME: iphlpsvc",
                CommandRunner.decode("SERVICE_NAME: iphlpsvc".getBytes(StandardCharsets.US_ASCII), GBK));
        assertEquals("", CommandRunner.decode(new byte[0], GBK));
    }
}