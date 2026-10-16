package org.paperweight.ffm.exec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** 以子进程方式执行系统命令，统一处理超时、输出采集与编码解码。 */
public final class CommandRunner {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(3);

    private final Charset fallbackCharset;

    public CommandRunner() {
        this(ConsoleCharset.forCommands());
    }

    public CommandRunner(Charset fallbackCharset) {
        this.fallbackCharset = fallbackCharset;
    }

    /** 返回输出字节不是合法 UTF-8 时使用的回退字符集。 */
    public Charset charset() {
        return fallbackCharset;
    }

    public CommandResult run(List<String> command) {
        return run(command, DEFAULT_TIMEOUT);
    }

    public CommandResult run(List<String> command, Duration timeout) {
        long startedAt = System.nanoTime();
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException e) {
            return CommandResult.startFailed(command,
                    "无法启动进程：" + e.getMessage(), elapsedMillis(startedAt));
        }

        AtomicReference<byte[]> standardOutput = new AtomicReference<>(new byte[0]);
        AtomicReference<byte[]> standardError = new AtomicReference<>(new byte[0]);
        Thread outputReader = Thread.ofVirtual().start(() -> standardOutput.set(drain(process.getInputStream())));
        Thread errorReader = Thread.ofVirtual().start(() -> standardError.set(drain(process.getErrorStream())));

        boolean timedOut = false;
        int exitCode = -1;
        try {
            if (process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                exitCode = process.exitValue();
            } else {
                timedOut = true;
                process.destroyForcibly();
                process.waitFor(DRAIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            joinQuietly(outputReader);
            joinQuietly(errorReader);
            return CommandResult.startFailed(command, "命令执行被中断", elapsedMillis(startedAt));
        }

        joinQuietly(outputReader);
        joinQuietly(errorReader);

        long elapsed = elapsedMillis(startedAt);
        String stdout = decode(standardOutput.get(), fallbackCharset).stripTrailing();
        String stderr = decode(standardError.get(), fallbackCharset).stripTrailing();
        if (timedOut) {
            return new CommandResult(command, true, -1, stdout, stderr,
                    "执行超时（" + timeout.toSeconds() + " 秒）", elapsed);
        }
        return new CommandResult(command, true, exitCode, stdout, stderr, null, elapsed);
    }

    /**
     * 解码命令输出。
     *
     * @param bytes           命令输出的原始字节
     * @param fallbackCharset 字节不是合法 UTF-8 时使用的回退字符集
     * @return 解码后的文本
     */
    static String decode(byte[] bytes, Charset fallbackCharset) {
        // Windows 各工具的编码并不一致：netsh 输出 UTF-8，whoami/sc 等输出控制台代码页
        if (isValidUtf8(bytes)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return new String(bytes, fallbackCharset);
    }

    private static boolean isValidUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static byte[] drain(InputStream stream) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join(DRAIN_TIMEOUT.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
