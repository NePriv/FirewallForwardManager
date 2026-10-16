package org.paperweight.ffm.elevate;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;
import org.paperweight.ffm.exec.ConsoleCharset;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 以管理员身份重新启动当前程序（整进程提权）。 */
public final class SelfRelauncher {

    static final String MAIN_CLASS = "org.paperweight.ffm.Main";
    static final String MARKER_PID_PROPERTY = "ffm.elevationMarkerPid";

    private static final String TEMP_PREFIX = "ffm-elevate-";
    private static final Duration MARKER_TIMEOUT = Duration.ofSeconds(12);
    private static final Duration STALE_FILE_AGE = Duration.ofHours(2);

    private SelfRelauncher() {
    }

    /** 尝试以管理员身份重启；返回是否已成功拉起提权进程。 */
    public static boolean relaunchElevated() {
        try {
            String classpath = System.getProperty("java.class.path");
            if (classpath == null || classpath.isBlank()) {
                return false;
            }
            long currentPid = ProcessHandle.current().pid();
            Path marker = markerPath(currentPid);
            Files.deleteIfExists(marker);

            Path launcher = Files.createTempFile(TEMP_PREFIX, ".cmd");
            // 用临时 .cmd 复刻当前 java 命令行，再交给 PowerShell 以 RunAs 触发 UAC
            Files.writeString(launcher, "@echo off\r\n" + buildCommandLine(javaExecutable("javaw.exe"), classpath, currentPid) + "\r\n", batchCharset());

            CommandResult started = new CommandRunner().run(
                    List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", elevatedStartCommand(launcher)),
                    Duration.ofMinutes(3));
            if (!started.ok()) {
                return false;
            }
            // 子进程写出标记文件才算提权成功，用户点“否”时不会出现该文件
            return waitForMarker(marker);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** 子进程启动时调用：写出提权标记文件通知父进程。 */
    public static void notifyElevatedStartup() {
        String pid = System.getProperty(MARKER_PID_PROPERTY);
        if (pid == null || pid.isBlank()) {
            return;
        }
        try {
            Files.writeString(markerPath(Long.parseLong(pid.strip())), "elevated");
        } catch (IOException | NumberFormatException ignored) {
            // 标记文件只是尽力而为的通知手段
        }
    }

    /** 清理历史遗留的临时启动脚本与标记文件。 */
    public static void cleanupStaleFiles() {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Instant cutoff = Instant.now().minus(STALE_FILE_AGE);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, TEMP_PREFIX + "*")) {
            for (Path file : stream) {
                try {
                    if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                    }
                } catch (IOException ignored) {
                    // 占用中的文件跳过即可
                }
            }
        } catch (IOException ignored) {
            // 临时目录不可访问时无需处理
        }
    }

    /** 组装等价于当前进程的 java 命令行。 */
    static String buildCommandLine(Path javaExecutable, String classpath, long markerPid) {
        StringBuilder builder = new StringBuilder();
        builder.append('"').append(javaExecutable).append('"');
        for (String argument : filterJvmArguments(ManagementFactory.getRuntimeMXBean().getInputArguments())) {
            builder.append(' ').append(quoteWindows(argument));
        }
        builder.append(" -D").append(MARKER_PID_PROPERTY).append('=').append(markerPid);
        builder.append(" -classpath ").append(quoteWindows(classpath));
        builder.append(' ').append(MAIN_CLASS);
        return builder.toString();
    }

    /** 过滤掉调试与 agent 相关的 JVM 参数，返回可复用的参数。 */
    static List<String> filterJvmArguments(List<String> inputArguments) {
        List<String> result = new ArrayList<>();
        for (String argument : inputArguments) {
            if (isUnsupportedArgument(argument)) {
                continue;
            }
            result.add(argument);
        }
        return result;
    }

    static Path javaExecutable(String fileName) {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path candidate = bin.resolve(fileName);
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        Path fallback = bin.resolve("java.exe");
        if (Files.isRegularFile(fallback)) {
            return fallback;
        }
        return Path.of(fileName);
    }

    static Path markerPath(long pid) {
        return Path.of(System.getProperty("java.io.tmpdir"), TEMP_PREFIX + "marker-" + pid + ".tmp");
    }

    static Charset batchCharset() {
        return ConsoleCharset.forCommands();
    }

    static String elevatedStartCommand(Path launcher) {
        return "$ErrorActionPreference='Stop';"
                + "try{Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', " + powerShellQuote(launcher.toString())
                + " -Verb RunAs;exit 0}catch{exit 2}";
    }

    static String powerShellQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    static String quoteWindows(String value) {
        if (value.isEmpty()) {
            return "\"\"";
        }
        if (value.indexOf(' ') < 0 && value.indexOf('\t') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\\\"") + '"';
    }

    private static boolean isUnsupportedArgument(String argument) {
        String lower = argument.toLowerCase(Locale.ROOT);
        return lower.startsWith("-agentlib:jdwp")
                || lower.startsWith("-agentpath:")
                || lower.startsWith("-javaagent:")
                || lower.startsWith("-xrunjdwp")
                || lower.equals("-xdebug")
                || lower.startsWith("-classpath")
                || lower.equals("-cp")
                || lower.startsWith("-dffm.")
                || lower.startsWith("-djava.class.path");
    }

    private static boolean waitForMarker(Path marker) {
        long deadline = System.nanoTime() + MARKER_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.exists(marker)) {
                try {
                    Files.deleteIfExists(marker);
                } catch (IOException ignored) {
                    // 清理失败不影响结果
                }
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
