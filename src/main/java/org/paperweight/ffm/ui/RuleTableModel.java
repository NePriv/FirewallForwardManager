package org.paperweight.ffm.ui;

import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.model.TransportProtocol;
import org.paperweight.ffm.net.AddressValidator;
import org.paperweight.ffm.service.NetshCommands;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 转发规则表格模型。
 */
public final class RuleTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "转发类型", "协议", "监听地址", "监听端口", "转发地址", "转发端口", "防火墙入站放行"
    };

    private final List<ProxyEntry> entries = new ArrayList<>();
    private Map<String, TransportProtocol> knownProtocols = Map.of();
    private Set<String> managedFirewallRules = Set.of();

    public void setData(List<ProxyEntry> entries, Map<String, TransportProtocol> knownProtocols,
                        Set<String> managedFirewallRules) {
        this.entries.clear();
        this.entries.addAll(entries);
        this.knownProtocols = knownProtocols;
        this.managedFirewallRules = managedFirewallRules;
        fireTableDataChanged();
    }

    public List<ProxyEntry> entries() {
        return List.copyOf(entries);
    }

    public ProxyEntry entryAt(int rowIndex) {
        return entries.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProxyEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> entry.type().displayName();
            case 1 -> protocolText(entry);
            case 2 -> listenAddressText(entry);
            case 3 -> String.valueOf(entry.listenPort());
            case 4 -> entry.connectAddress();
            case 5 -> String.valueOf(entry.connectPort());
            case 6 -> firewallText(entry);
            default -> "";
        };
    }

    private String protocolText(ProxyEntry entry) {
        // netsh 的转发列表不显示协议，只有本会话内添加过的规则才可能知道协议
        TransportProtocol known = knownProtocols.get(entry.key());
        return known == null ? "TCP/UDP（未显示）" : known.displayName();
    }

    private String listenAddressText(ProxyEntry entry) {
        String address = entry.listenAddress();
        return AddressValidator.isWildcardAddress(address) ? address + "（所有地址）" : address;
    }

    private String firewallText(ProxyEntry entry) {
        boolean tcp = managedFirewallRules.contains(
                NetshCommands.firewallRuleName(TransportProtocol.TCP, entry.listenPort(), entry.listenAddress()));
        boolean udp = managedFirewallRules.contains(
                NetshCommands.firewallRuleName(TransportProtocol.UDP, entry.listenPort(), entry.listenAddress()));
        if (tcp && udp) {
            return "TCP/UDP 已放行";
        }
        if (tcp) {
            return "TCP 已放行";
        }
        if (udp) {
            return "UDP 已放行";
        }
        return "未放行";
    }
}
