package org.paperweight.ffm.service;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.exec.CommandRunner;
import org.paperweight.ffm.model.ForwardRule;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.TransportProtocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 转发与防火墙的编排层：把一条端口转发与它的防火墙放行规则当成一个整体来处理。 */
public final class ForwardManager {

    /** 一次刷新得到的完整视图。 */
    public record Snapshot(
            List<ProxyEntry> entries,
            Set<String> managedFirewallRules,
            Map<String, TransportProtocol> knownProtocols) {
    }

    private final PortProxyService portProxy;
    private final FirewallService firewall;
    /** 本次会话内添加过的规则协议，仅用于界面展示。 */
    private final Map<String, TransportProtocol> knownProtocols = new ConcurrentHashMap<>();

    public ForwardManager(CommandRunner runner) {
        this.portProxy = new PortProxyService(runner);
        this.firewall = new FirewallService(runner);
    }

    public OperationResult<Snapshot> snapshot() {
        List<CommandResult> commands = new ArrayList<>();
        OperationResult<List<ProxyEntry>> listing = portProxy.list();
        commands.addAll(listing.commands());
        if (!listing.ok()) {
            return OperationResult.fail(listing.message(), commands);
        }

        OperationResult<Set<String>> firewallRules = firewall.listManagedRuleNames();
        commands.addAll(firewallRules.commands());
        Set<String> managed = firewallRules.ok() ? firewallRules.value() : Set.of();

        String message = "共 " + listing.value().size() + " 条转发规则";
        if (firewallRules.ok()) {
            message += "，其中本工具创建的防火墙入站规则 " + managed.size() + " 条";
        } else {
            message += "；读取防火墙规则失败：" + firewallRules.message();
        }
        return OperationResult.ok(new Snapshot(List.copyOf(listing.value()), managed, Map.copyOf(knownProtocols)),
                message, commands);
    }

    public OperationResult<Void> add(ForwardRule rule, boolean addFirewallRule, boolean restrictRemoteToConnectAddress) {
        List<CommandResult> commands = new ArrayList<>();
        return addInternal(rule, addFirewallRule, restrictRemoteToConnectAddress, commands);
    }

    /** 覆盖已存在的转发：先删除原规则，再按新配置创建。 */
    public OperationResult<Void> replace(ProxyEntry existing, Set<String> managedFirewallRules, ForwardRule rule,
                                         boolean addFirewallRule, boolean restrictRemoteToConnectAddress) {
        List<CommandResult> commands = new ArrayList<>();
        OperationResult<Void> removed = deleteEntryInternal(existing, managedFirewallRules, true, commands);
        if (!removed.ok()) {
            return OperationResult.fail("覆盖失败：无法删除原有规则（" + removed.message() + "）", commands);
        }
        return addInternal(rule, addFirewallRule, restrictRemoteToConnectAddress, commands);
    }

    public OperationResult<Void> delete(ProxyEntry entry, Set<String> managedFirewallRules, boolean deleteFirewallRules) {
        List<CommandResult> commands = new ArrayList<>();
        return deleteEntryInternal(entry, managedFirewallRules, deleteFirewallRules, commands);
    }

    /** 删除全部列出的转发，并清理本工具创建的防火墙规则。 */
    public OperationResult<Void> deleteAll(Collection<ProxyEntry> entries, Set<String> managedFirewallRules) {
        List<CommandResult> commands = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (ProxyEntry entry : entries) {
            OperationResult<Void> result = deleteEntryInternal(entry, managedFirewallRules, true, commands);
            if (!result.ok()) {
                problems.add(entry.describe() + "：" + result.message());
            }
        }
        OperationResult<Integer> orphans = cleanupInternal(entries, managedFirewallRules, commands);
        int cleaned = orphans.countOrZero();
        if (!orphans.ok()) {
            problems.add(orphans.message());
        }
        String message = "已删除 " + entries.size() + " 条转发规则"
                + (cleaned > 0 ? "，并清理 " + cleaned + " 条防火墙规则" : "");
        if (problems.isEmpty()) {
            return OperationResult.ok(null, message, commands);
        }
        return OperationResult.fail(String.join("；", problems), commands);
    }

    /** 清理孤立的防火墙规则：本工具创建、但已没有对应转发规则的残留。 */
    public OperationResult<Integer> cleanupOrphanFirewallRules(Collection<ProxyEntry> entries, Set<String> managedFirewallRules) {
        List<CommandResult> commands = new ArrayList<>();
        return cleanupInternal(entries, managedFirewallRules, commands);
    }

    private OperationResult<Void> addInternal(ForwardRule rule, boolean addFirewallRule,
                                              boolean restrictRemoteToConnectAddress, List<CommandResult> commands) {
        OperationResult<Void> added = portProxy.add(rule);
        commands.addAll(added.commands());
        if (!added.ok()) {
            return OperationResult.fail(added.message(), commands);
        }

        // 防火墙规则失败则回滚转发，避免留下“规则存在但外部连不上”的半成品
        if (addFirewallRule) {
            OperationResult<Void> allowed = firewall.addInbound(rule, restrictRemoteToConnectAddress);
            commands.addAll(allowed.commands());
            if (!allowed.ok()) {
                OperationResult<Void> rollback = portProxy.delete(rule.toEntry(), rule.protocol());
                commands.addAll(rollback.commands());
                String suffix = rollback.ok()
                        ? "已回滚刚添加的转发规则。"
                        : "且回滚失败，请在列表中手工检查。";
                return OperationResult.fail(allowed.message() + "，" + suffix, commands);
            }
        }

        knownProtocols.put(rule.toEntry().key(), rule.protocol());
        String firewallNote = addFirewallRule
                ? "，并已放行防火墙入站端口 " + rule.listenPort()
                : "（未添加防火墙放行规则，外部可能无法连接）";
        return OperationResult.ok(null, "已添加转发 " + rule.describe() + firewallNote, commands);
    }

    private OperationResult<Void> deleteEntryInternal(ProxyEntry entry, Set<String> managedFirewallRules,
                                                      boolean deleteFirewallRules, List<CommandResult> commands) {
        boolean removedByPortProxy = false;
        // 转发列表不显示协议，TCP 与 UDP 各删一次，任一成功即视为删除成功
        for (TransportProtocol protocol : TransportProtocol.values()) {
            OperationResult<Void> attempt = portProxy.delete(entry, protocol);
            commands.addAll(attempt.commands());
            if (attempt.ok()) {
                removedByPortProxy = true;
            }
        }

        List<String> problems = new ArrayList<>();
        if (!removedByPortProxy) {
            problems.add("netsh 转发规则删除失败（已分别尝试 TCP 与 UDP）");
        }

        if (deleteFirewallRules) {
            for (String name : firewallRuleNames(entry)) {
                if (!managedFirewallRules.contains(name)) {
                    continue;
                }
                OperationResult<Void> removed = firewall.delete(name);
                commands.addAll(removed.commands());
                if (!removed.ok()) {
                    problems.add(removed.message());
                }
            }
        }
        knownProtocols.remove(entry.key());

        if (problems.isEmpty()) {
            return OperationResult.ok(null, "已删除转发 " + entry.describe(), commands);
        }
        String prefix = removedByPortProxy ? "转发规则已删除，但存在问题：" : "删除失败：";
        return OperationResult.fail(prefix + String.join("；", problems), commands);
    }

    private OperationResult<Integer> cleanupInternal(Collection<ProxyEntry> entries, Set<String> managedFirewallRules,
                                                     List<CommandResult> commands) {
        Set<String> expected = new HashSet<>();
        for (ProxyEntry entry : entries) {
            expected.addAll(firewallRuleNames(entry));
        }
        List<String> orphans = managedFirewallRules.stream()
                .filter(name -> !expected.contains(name))
                .sorted()
                .toList();
        if (orphans.isEmpty()) {
            return OperationResult.ok(0, "没有发现孤立的防火墙规则", commands);
        }

        List<String> problems = new ArrayList<>();
        int removedCount = 0;
        for (String name : orphans) {
            OperationResult<Void> removed = firewall.delete(name);
            commands.addAll(removed.commands());
            if (removed.ok()) {
                removedCount++;
            } else {
                problems.add(removed.message());
            }
        }
        if (problems.isEmpty()) {
            return OperationResult.ok(removedCount,
                    "已清理 " + removedCount + " 条孤立防火墙规则：" + String.join("、", orphans), commands);
        }
        return OperationResult.fail("孤立防火墙规则清理失败：" + String.join("；", problems), commands);
    }

    /** 该转发可能对应的防火墙规则名（TCP / UDP 各一个）。 */
    private static List<String> firewallRuleNames(ProxyEntry entry) {
        List<String> names = new ArrayList<>(TransportProtocol.values().length);
        for (TransportProtocol protocol : TransportProtocol.values()) {
            names.add(NetshCommands.firewallRuleName(protocol, entry.listenPort(), entry.listenAddress()));
        }
        return names;
    }
}
