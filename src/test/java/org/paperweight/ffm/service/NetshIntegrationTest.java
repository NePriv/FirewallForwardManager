package org.paperweight.ffm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.ProxyType;
import org.paperweight.ffm.parse.PortProxyTableParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 在真实 Windows 上调用 netsh 的集成测试（只读命令，不需要管理员权限）。 */
@EnabledOnOs(OS.WINDOWS)
class NetshIntegrationTest {

    @Test
    void executesNetshAndParsesIpv4Table() {
        CommandResult result = new CommandRunner().run(NetshCommands.portProxyShow(ProxyType.V4_TO_V4));

        assertTrue(result.started(), () -> "netsh 未启动: " + result.failure());
        assertEquals(0, result.exitCode(), () -> "netsh 执行失败: " + result.errorText());
        assertFalse(result.stdout().isBlank(), "netsh 没有输出任何内容");
        assertFalse(result.stdout().contains("\uFFFD"), () -> "输出解码失败（出现替换字符）: " + result.stdout());

        List<ProxyEntry> entries = PortProxyTableParser.parse(result.stdout(), ProxyType.V4_TO_V4);
        System.out.println("[集成测试] netsh 原始输出:\n" + result.stdout());
        System.out.println("[集成测试] 解析结果: " + entries);
    }

    @Test
    void detectsConsoleCodePageForDecoding() {
        CommandResult result = new CommandRunner().run(NetshCommands.serviceQuery(NetshCommands.IP_HELPER_SERVICE));

        assertTrue(result.started(), () -> "sc.exe 未启动: " + result.failure());
        System.out.println("[集成测试] iphlpsvc 状态输出:\n" + result.output());
    }
}
