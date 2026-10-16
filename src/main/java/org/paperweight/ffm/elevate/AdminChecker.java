package org.paperweight.ffm.elevate;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;

import java.util.List;

/** 判断当前进程是否以管理员（提升后）权限运行。 */
public final class AdminChecker {

    /** “高”强制完整性级别，只有提升后的管理员令牌才包含。 */
    static final String HIGH_MANDATORY_LEVEL = "S-1-16-12288";
    /** “中”强制完整性级别，普通用户令牌包含。 */
    static final String MEDIUM_MANDATORY_LEVEL = "S-1-16-8192";

    private static volatile Boolean elevated;

    private AdminChecker() {
    }

    public static boolean isElevated() {
        Boolean value = elevated;
        if (value != null) {
            return value;
        }
        synchronized (AdminChecker.class) {
            if (elevated == null) {
                elevated = detect();
            }
            return elevated;
        }
    }

    /** 测试用：重置缓存。 */
    static void resetCache() {
        elevated = null;
    }

    static boolean detect() {
        CommandRunner runner = new CommandRunner();
        // 强制完整性级别 SID 与系统显示语言无关，比解析文本更可靠
        CommandResult whoami = runner.run(List.of("whoami.exe", "/groups"));
        if (whoami.started()) {
            String output = whoami.output();
            if (output.contains(HIGH_MANDATORY_LEVEL)) {
                return true;
            }
            if (output.contains(MEDIUM_MANDATORY_LEVEL)) {
                return false;
            }
        }
        // whoami 不可用时退回 net session 的退出码
        CommandResult netSession = runner.run(List.of("net.exe", "session"));
        return netSession.ok();
    }

    /** 判断 whoami /groups 输出是否表明进程已提权。 */
    static boolean containsHighIntegrity(String whoamiGroupsOutput) {
        return whoamiGroupsOutput != null && whoamiGroupsOutput.contains(HIGH_MANDATORY_LEVEL);
    }
}
