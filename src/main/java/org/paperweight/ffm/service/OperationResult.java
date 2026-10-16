package org.paperweight.ffm.service;

import org.paperweight.ffm.exec.CommandResult;

import java.util.List;

/**
 * 一次业务操作的统一结果：是否成功、可读消息、以及实际执行的命令列表。
 *
 * @param <T> 携带的数据类型（不需要时用 {@code Void}）
 */
public record OperationResult<T>(
        boolean ok,
        T value,
        String message,
        List<CommandResult> commands) {

    public OperationResult {
        commands = commands == null ? List.of() : List.copyOf(commands);
    }

    public static <T> OperationResult<T> ok(T value, String message) {
        return new OperationResult<>(true, value, message, List.of());
    }

    public static <T> OperationResult<T> ok(T value, String message, List<CommandResult> commands) {
        return new OperationResult<>(true, value, message, commands);
    }

    public static <T> OperationResult<T> fail(String message) {
        return new OperationResult<>(false, null, message, List.of());
    }

    public static <T> OperationResult<T> fail(String message, List<CommandResult> commands) {
        return new OperationResult<>(false, null, message, commands);
    }

    /** 结果携带的整数计数（可能为 null）。 */
    public int countOrZero() {
        return value instanceof Integer count ? count : 0;
    }
}
