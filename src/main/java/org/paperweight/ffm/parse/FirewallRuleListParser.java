package org.paperweight.ffm.parse;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从 {@code netsh advfirewall firewall show rule name=all} 的输出中提取由本工具创建的规则名。 */
public final class FirewallRuleListParser {

    public static final String RULE_PREFIX = "FFM-IN-";

    private static final Pattern MANAGED_NAME =
            Pattern.compile(Pattern.quote(RULE_PREFIX) + "[A-Za-z0-9._-]+");

    private FirewallRuleListParser() {
    }

    public static Set<String> parseManagedRuleNames(String output) {
        Set<String> names = new TreeSet<>();
        if (output == null || output.isBlank()) {
            return names;
        }
        Matcher matcher = MANAGED_NAME.matcher(output);
        while (matcher.find()) {
            names.add(matcher.group());
        }
        return names;
    }
}
