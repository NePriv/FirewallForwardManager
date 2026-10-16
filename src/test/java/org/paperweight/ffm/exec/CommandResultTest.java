package org.paperweight.ffm.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandResultTest {

    @Test
    void formatsCommandLineWithoutQuotesWhenNotNeeded() {
        assertEquals("netsh.exe interface portproxy show v4tov4",
                CommandResult.format(List.of("netsh.exe", "interface", "portproxy", "show", "v4tov4")));
    }

    @Test
    void quotesTokensContainingSpaces() {
        assertEquals("\"C:\\Program Files\\Zulu\\zulu-25\\bin\\javaw.exe\" -classpath \"D:\\a b\\target\\classes\"",
                CommandResult.format(List.of(
                        "C:\\Program Files\\Zulu\\zulu-25\\bin\\javaw.exe",
                        "-classpath",
                        "D:\\a b\\target\\classes")));
    }

    @Test
    void commandLineUsesStoredTokens() {
        CommandResult result = completed(0, "ok", "");
        assertEquals("netsh.exe interface portproxy show v4tov4", result.commandLine());
    }

    @Test
    void okOnlyWhenStartedAndExitCodeZero() {
        assertTrue(completed(0, "", "").ok());
        assertFalse(completed(1, "", "错误").ok());
        assertFalse(CommandResult.startFailed(List.of("netsh.exe"), "无法启动", 3).ok());
        assertFalse(new CommandResult(List.of("netsh.exe"), true, -1, "", "", "执行超时（30 秒）", 1).ok());
    }

    @Test
    void errorTextPrefersStderr() {
        assertEquals("系统找不到指定的文件。", completed(1, "一些输出", "系统找不到指定的文件。").errorText());
        assertEquals("仅在 stdout 的信息", completed(1, "仅在 stdout 的信息", "").errorText());
        assertEquals("退出码 1", completed(1, "", "").errorText());
        assertEquals("无法启动", CommandResult.startFailed(List.of("netsh.exe"), "无法启动", 3).errorText());
    }

    @Test
    void outputMergesStreams() {
        assertEquals("第一行" + System.lineSeparator() + "错误行",
                completed(1, "第一行", "错误行").output());
    }

    private static CommandResult completed(int exitCode, String stdout, String stderr) {
        return new CommandResult(List.of("netsh.exe", "interface", "portproxy", "show", "v4tov4"),
                true, exitCode, stdout, stderr, null, 12);
    }
}
