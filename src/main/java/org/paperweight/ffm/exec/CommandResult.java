package org.paperweight.ffm.exec;

import java.util.List;

/**
 * 一次命令执行的完整结果。
 *
 * @param command        实际执行的命令行（参数数组形式）
 * @param started        进程是否成功启动
 * @param exitCode       退出码，进程未启动或超时时为 -1
 * @param stdout         标准输出（已按控制台编码解码）
 * @param stderr         标准错误（已按控制台编码解码）
 * @param failure        启动失败/超时的原因，正常结束为 null
 * @param durationMillis 执行耗时（毫秒）
 */
public record CommandResult(
        List<String> command,
        boolean started,
        int exitCode,
        String stdout,
        String stderr,
        String failure,
        long durationMillis) {

    public CommandResult {
        command = command == null ? List.of() : List.copyOf(command);
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }

    public static CommandResult startFailed(List<String> command, String reason, long durationMillis) {
        return new CommandResult(command, false, -1, "", "", reason, durationMillis);
    }

    public boolean ok() {
        return started && failure == null && exitCode == 0;
    }

    /** 命令的完整文本形式，便于复制到命令行手工执行。 */
    public String commandLine() {
        return format(command);
    }

    /** stdout 与 stderr 的合并文本。 */
    public String output() {
        StringBuilder builder = new StringBuilder();
        if (!stdout.isBlank()) {
            builder.append(stdout.stripTrailing());
        }
        if (!stderr.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(System.lineSeparator());
            }
            builder.append(stderr.stripTrailing());
        }
        return builder.toString();
    }

    /** 返回可展示给用户的错误摘要。 */
    public String errorText() {
        String fromError = firstNonBlank(stderr);
        if (fromError != null) {
            return fromError;
        }
        String fromOutput = firstNonBlank(stdout);
        if (fromOutput != null) {
            return fromOutput;
        }
        if (!started) {
            return failure == null ? "进程未启动" : failure;
        }
        if (failure != null) {
            return failure;
        }
        return "退出码 " + exitCode;
    }

    /** 把参数数组还原为可读的命令行文本。 */
    public static String format(List<String> command) {
        StringBuilder builder = new StringBuilder();
        for (String token : command) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            if (token.contains(" ") || token.contains("\t")) {
                builder.append('"').append(token).append('"');
            } else {
                builder.append(token);
            }
        }
        return builder.toString();
    }

    private static String firstNonBlank(String text) {
        if (text == null) {
            return null;
        }
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                return line.strip();
            }
        }
        return null;
    }
}
