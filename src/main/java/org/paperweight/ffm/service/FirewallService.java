package org.paperweight.ffm.service;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;
import org.paperweight.ffm.model.ForwardRule;
import org.paperweight.ffm.net.AddressValidator;
import org.paperweight.ffm.parse.FirewallRuleListParser;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** 封装 {@code netsh advfirewall firewall} 的入站规则增删与查询。 */
public final class FirewallService {

    private static final Duration SHOW_ALL_TIMEOUT = Duration.ofSeconds(90);

    private final CommandRunner runner;

    public FirewallService(CommandRunner runner) {
        this.runner = runner;
    }

    /**
     * 为转发规则添加入站放行规则。
     *
     * @param restrictRemoteToConnectAddress 是否把来源限制为转发目标地址
     */
    public OperationResult<Void> addInbound(ForwardRule rule, boolean restrictRemoteToConnectAddress) {
        String ruleName = NetshCommands.firewallRuleName(rule);
        // 通配监听地址不指定 localip；remoteip 仅在用户要求限制来源时给出
        String localAddress = rule.listenOnWildcardAddress() ? null : rule.listenAddress();
        String remoteAddress = restrictRemoteToConnectAddress
                && !AddressValidator.isWildcardAddress(rule.connectAddress())
                ? rule.connectAddress()
                : null;

        CommandResult result = runner.run(NetshCommands.firewallAddInbound(
                ruleName, rule.protocol(), rule.listenPort(), localAddress, remoteAddress));
        List<CommandResult> commands = List.of(result);
        if (!result.ok()) {
            return OperationResult.fail("添加防火墙入站规则失败：" + result.errorText(), commands);
        }
        return OperationResult.ok(null, "已添加防火墙入站规则 " + ruleName
                + "（" + rule.protocol().displayName() + " " + rule.listenPort() + "）", commands);
    }

    public OperationResult<Void> delete(String ruleName) {
        CommandResult result = runner.run(NetshCommands.firewallDeleteRule(ruleName));
        List<CommandResult> commands = List.of(result);
        if (!result.ok()) {
            return OperationResult.fail("删除防火墙规则 " + ruleName + " 失败：" + result.errorText(), commands);
        }
        return OperationResult.ok(null, "已删除防火墙规则 " + ruleName, commands);
    }

    /** 列出所有由本工具创建的防火墙规则名（前缀 FFM-IN-）。 */
    public OperationResult<Set<String>> listManagedRuleNames() {
        CommandResult result = runner.run(NetshCommands.firewallShowAllRules(), SHOW_ALL_TIMEOUT);
        List<CommandResult> commands = List.of(result);
        if (!result.ok()) {
            return OperationResult.fail("读取防火墙规则失败：" + result.errorText(), commands);
        }
        Set<String> names = FirewallRuleListParser.parseManagedRuleNames(result.stdout());
        return OperationResult.ok(Set.copyOf(names), "本工具创建的防火墙入站规则 " + names.size() + " 条", commands);
    }
}
