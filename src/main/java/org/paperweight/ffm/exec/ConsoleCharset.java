package org.paperweight.ffm.exec;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 探测 Windows 控制台代码页，作为解码系统命令输出的回退字符集。 */
public final class ConsoleCharset {

    /** 覆盖用系统属性，例如 {@code -Dffm.charset=GBK}。 */
    public static final String OVERRIDE_PROPERTY = "ffm.charset";

    private static final Pattern DIGITS = Pattern.compile("(\\d{3,5})");
    private static volatile Charset cached;

    private ConsoleCharset() {
    }

    /** 返回回退字符集。 */
    public static Charset forCommands() {
        Charset value = cached;
        if (value != null) {
            return value;
        }
        synchronized (ConsoleCharset.class) {
            if (cached == null) {
                cached = detect();
            }
            return cached;
        }
    }

    static Charset detect() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            try {
                return Charset.forName(override.strip());
            } catch (RuntimeException ignored) {
                // 忽略非法配置，继续自动探测
            }
        }
        int codePage = queryCodePage();
        if (codePage > 0) {
            Charset byCodePage = byCodePage(codePage);
            if (byCodePage != null) {
                return byCodePage;
            }
        }
        Charset nativeCharset = nativeCharset();
        if (nativeCharset != null) {
            return nativeCharset;
        }
        return StandardCharsets.UTF_8;
    }

    /** 解析 {@code chcp} 输出中的代码页数字，例如 “活动代码页: 936”。 */
    static Integer parseCodePage(String chcpOutput) {
        if (chcpOutput == null) {
            return null;
        }
        Matcher matcher = DIGITS.matcher(chcpOutput);
        Integer found = null;
        while (matcher.find()) {
            found = Integer.valueOf(matcher.group(1));
        }
        return found;
    }

    /** 代码页到 Java 字符集的映射，未知代码页返回 null。 */
    static Charset byCodePage(int codePage) {
        String name = switch (codePage) {
            case 65001 -> "UTF-8";
            case 936 -> "GBK";
            case 950 -> "Big5";
            case 932 -> "Shift_JIS";
            case 949 -> "EUC-KR";
            case 437, 850, 852, 866 -> "IBM" + codePage;
            default -> codePage >= 1250 && codePage <= 1258 ? "windows-" + codePage : null;
        };
        if (name == null) {
            return null;
        }
        try {
            return Charset.forName(name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int queryCodePage() {
        Process process = null;
        try {
            process = new ProcessBuilder("cmd.exe", "/c", "chcp").redirectErrorStream(true).start();
            byte[] bytes;
            try (var stream = process.getInputStream()) {
                bytes = stream.readAllBytes();
            }
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1;
            }
            Integer codePage = parseCodePage(new String(bytes, StandardCharsets.ISO_8859_1));
            return codePage == null ? -1 : codePage;
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static Charset nativeCharset() {
        for (String key : new String[]{"native.encoding", "sun.jnu.encoding"}) {
            String name = System.getProperty(key);
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                return Charset.forName(name.strip());
            } catch (RuntimeException ignored) {
                // 尝试下一个属性
            }
        }
        return null;
    }
}
