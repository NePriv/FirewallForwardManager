package org.paperweight.ffm.parse;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 只应提取由本工具创建的规则（FFM-IN- 前缀），系统自带规则必须被忽略。
 */
class FirewallRuleListParserTest {

    private static final String SHOW_ALL_OUTPUT = """
            ----------------------------------------------------------------------
            规则名称:                             FFM-IN-TCP-2405-192.168.3.6
            ----------------------------------------------------------------------
            已启用:                               是
            方向:                                 入
            配置文件:                             任何
            本地 IP:                              192.168.3.6
            远程 IP:                              任何
            协议:                                 TCP
            本地端口:                             2405
            远程端口:                             任何
            边缘遍历:                             否
            操作:                                 允许
            ----------------------------------------------------------------------
            规则名称:                             远程桌面 - 用户模式(TCP-In)
            ----------------------------------------------------------------------
            已启用:                               是
            方向:                                 入
            本地端口:                             3389
            ----------------------------------------------------------------------
            规则名称:                             FFM-IN-TCP-2405-v6-1f2e3d4c
            ----------------------------------------------------------------------
            已启用:                               是
            方向:                                 入
            ----------------------------------------------------------------------
            """;

    @Test
    void extractsOnlyManagedRules() {
        Set<String> names = FirewallRuleListParser.parseManagedRuleNames(SHOW_ALL_OUTPUT);

        assertEquals(Set.of("FFM-IN-TCP-2405-192.168.3.6", "FFM-IN-TCP-2405-v6-1f2e3d4c"), names);
    }

    @Test
    void returnsEmptyForNoMatch() {
        assertTrue(FirewallRuleListParser.parseManagedRuleNames("规则名称: 远程桌面\n方向: 入").isEmpty());
        assertTrue(FirewallRuleListParser.parseManagedRuleNames("").isEmpty());
        assertTrue(FirewallRuleListParser.parseManagedRuleNames(null).isEmpty());
    }

    @Test
    void doesNotTreatPrefixWithoutSuffixAsRule() {
        assertTrue(FirewallRuleListParser.parseManagedRuleNames("说明: FFM-IN-").isEmpty());
    }
}
