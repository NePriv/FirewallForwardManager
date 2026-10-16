package org.paperweight.ffm.elevate;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfRelauncherTest {

    @Test
    void leavesTokensWithoutSpacesAlone() {
        assertEquals("-Xmx512m", SelfRelauncher.quoteWindows("-Xmx512m"));
        assertEquals("\"\"", SelfRelauncher.quoteWindows(""));
    }

    @Test
    void quotesTokensWithSpaces() {
        assertEquals("\"D:\\a b\\classes\"", SelfRelauncher.quoteWindows("D:\\a b\\classes"));
    }

    @Test
    void escapesSingleQuotesForPowerShell() {
        assertEquals("'C:\\Temp\\x.cmd'", SelfRelauncher.powerShellQuote("C:\\Temp\\x.cmd"));
        assertEquals("'a''b'", SelfRelauncher.powerShellQuote("a'b"));
    }

    @Test
    void buildsPowerShellStartProcessCommand() {
        String command = SelfRelauncher.elevatedStartCommand(Path.of("C:\\Temp\\ffm-elevate-1.cmd"));

        assertTrue(command.contains("Start-Process"));
        assertTrue(command.contains("-Verb RunAs"));
        assertTrue(command.contains("'C:\\Temp\\ffm-elevate-1.cmd'"));
        assertTrue(command.contains("exit 0"));
        assertTrue(command.contains("exit 2"));
    }

    @Test
    void filtersDebugAndAgentArguments() {
        List<String> filtered = SelfRelauncher.filterJvmArguments(List.of(
                "-Xmx512m",
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005",
                "-javaagent:C:\\tools\\agent.jar",
                "-Dffm.noElevate=true",
                "-Djava.class.path=C:\\x",
                "-Dsun.java.command=org.paperweight.ffm.Main",
                "-Dfile.encoding=UTF-8"));

        assertEquals(List.of("-Xmx512m", "-Dsun.java.command=org.paperweight.ffm.Main", "-Dfile.encoding=UTF-8"), filtered);
    }

    @Test
    void buildsEquivalentCommandLine() {
        String commandLine = SelfRelauncher.buildCommandLine(
                Path.of("C:\\Program Files\\Zulu\\zulu-25\\bin\\javaw.exe"),
                "D:\\a b\\target\\classes",
                4321L);

        assertTrue(commandLine.startsWith("\"C:\\Program Files\\Zulu\\zulu-25\\bin\\javaw.exe\""));
        assertTrue(commandLine.contains("-Dffm.elevationMarkerPid=4321"));
        assertTrue(commandLine.contains("-classpath \"D:\\a b\\target\\classes\""));
        assertTrue(commandLine.endsWith("org.paperweight.ffm.Main"));
    }

    @Test
    void markerPathIsDerivedFromPid() {
        Path marker = SelfRelauncher.markerPath(4321L);

        assertEquals("ffm-elevate-marker-4321.tmp", marker.getFileName().toString());
        assertFalse(marker.toString().contains(" "));
    }
}
