package org.paperweight.ffm.service;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;
import org.paperweight.ffm.model.ForwardRule;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.ProxyType;
import org.paperweight.ffm.model.TransportProtocol;
import org.paperweight.ffm.parse.PortProxyTableParser;

import java.util.ArrayList;
import java.util.List;

/** 封装 {@code netsh interface portproxy} 的查询与增删。 */
public final class PortProxyService {

    private final CommandRunner runner;

    public PortProxyService(CommandRunner runner) {
        this.runner = runner;
    }

    /** 查询全部转发规则。 */
    public OperationResult<List<ProxyEntry>> list() {
        List<CommandResult> commands = new ArrayList<>();
        List<ProxyEntry> entries = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        // 分开执行四条子命令：show all 会把多个方向拼在一起，表头又随语言变化
        for (ProxyType type : ProxyType.values()) {
            CommandResult result = runner.run(NetshCommands.portProxyShow(type));
            commands.add(result);
            if (!result.ok()) {
                errors.add(type.displayName() + "：" + result.errorText());
                continue;
            }
            entries.addAll(PortProxyTableParser.parse(result.stdout(), type));
        }
        if (!errors.isEmpty()) {
            return OperationResult.fail("读取端口转发列表失败：" + String.join("；", errors), commands);
        }
        return OperationResult.ok(List.copyOf(entries), "共 " + entries.size() + " 条转发规则", commands);
    }

    public OperationResult<Void> add(ForwardRule rule) {
        CommandResult result = runner.run(NetshCommands.portProxyAdd(rule));
        List<CommandResult> commands = List.of(result);
        if (!result.ok()) {
            return OperationResult.fail("添加端口转发失败：" + result.errorText(), commands);
        }
        return OperationResult.ok(null, "已添加端口转发 " + rule.describe(), commands);
    }

    /** 删除指定协议的转发规则。 */
    public OperationResult<Void> delete(ProxyEntry entry, TransportProtocol protocol) {
        List<CommandResult> commands = new ArrayList<>();
        String lastError = null;
        for (List<String> variant : NetshCommands.portProxyDeleteVariants(entry, protocol)) {
            CommandResult result = runner.run(variant);
            commands.add(result);
            if (result.ok()) {
                return OperationResult.ok(null, "已删除 " + protocol.displayName() + " 转发 " + entry.describe(), commands);
            }
            lastError = result.errorText();
        }
        return OperationResult.fail(protocol.displayName() + " 转发删除失败：" + lastError, commands);
    }
}
