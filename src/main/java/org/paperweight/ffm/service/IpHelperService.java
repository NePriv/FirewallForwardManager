package org.paperweight.ffm.service;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 检查并启动端口转发依赖的 IP Helper 服务（iphlpsvc）。 */
public final class IpHelperService {

    private final CommandRunner runner;

    public IpHelperService(CommandRunner runner) {
        this.runner = runner;
    }

    public OperationResult<Boolean> ensureRunning() {
        List<CommandResult> commands = new ArrayList<>();
        CommandResult query = runner.run(NetshCommands.serviceQuery(NetshCommands.IP_HELPER_SERVICE));
        commands.add(query);
        if (isRunning(query)) {
            return OperationResult.ok(true, "IP Helper 服务（" + NetshCommands.IP_HELPER_SERVICE + "）正在运行", commands);
        }

        CommandResult start = runner.run(NetshCommands.serviceStart(NetshCommands.IP_HELPER_SERVICE));
        commands.add(start);
        CommandResult queryAgain = runner.run(NetshCommands.serviceQuery(NetshCommands.IP_HELPER_SERVICE));
        commands.add(queryAgain);

        if (isRunning(queryAgain)) {
            return OperationResult.ok(true, "已启动 IP Helper 服务（" + NetshCommands.IP_HELPER_SERVICE + "）", commands);
        }
        if (alreadyRunning(start)) {
            return OperationResult.ok(true, "IP Helper 服务已在运行", commands);
        }
        return OperationResult.fail("无法启动 IP Helper 服务（端口转发依赖该服务）：" + start.errorText(), commands);
    }

    static boolean isRunning(CommandResult result) {
        return result.ok() && result.stdout().toLowerCase(Locale.ROOT).contains("running");
    }

    /** 返回启动命令是否只是提示“服务已在运行”。 */
    static boolean alreadyRunning(CommandResult startResult) {
        // 1056：服务实例已在运行
        if (startResult.exitCode() == 1056) {
            return true;
        }
        String text = startResult.output().toLowerCase(Locale.ROOT);
        return text.contains("1056")
                || text.contains("already")
                || text.contains("已经启动")
                || text.contains("已经在运行");
    }
}
