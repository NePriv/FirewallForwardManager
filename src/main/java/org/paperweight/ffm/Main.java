package org.paperweight.ffm;

import org.paperweight.ffm.elevate.AdminChecker;
import org.paperweight.ffm.elevate.SelfRelauncher;
import org.paperweight.ffm.ui.MainFrame;
import org.paperweight.ffm.ui.UITheme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 程序入口：初始化外观、确认管理员权限（必要时自提权），然后打开主窗口。 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SelfRelauncher.notifyElevatedStartup();
        SelfRelauncher.cleanupStaleFiles();
        UITheme.install();

        boolean elevated = AdminChecker.isElevated();
        boolean readOnly = !elevated;

        // -Dffm.noElevate=true 可跳过提权弹窗，便于在管理员的终端或 IDE 里调试
        if (!elevated && !Boolean.getBoolean("ffm.noElevate")) {
            int choice = askForElevation();
            if (choice == 0) {
                if (SelfRelauncher.relaunchElevated()) {
                    System.exit(0);
                }
                JOptionPane.showMessageDialog(null,
                        "自动提权失败（可能取消了 UAC 提示），将以只读模式启动。\n"
                                + "请右键以“管理员身份运行”，或从管理员终端启动本程序。",
                        "提权失败", JOptionPane.ERROR_MESSAGE);
            } else if (choice != 1) {
                System.exit(0);
            }
        }

        boolean readOnlyMode = readOnly;
        SwingUtilities.invokeLater(() -> new MainFrame(readOnlyMode).setVisible(true));
    }

    private static int askForElevation() {
        Object[] options = {"以管理员身份重启（推荐）", "只读模式查看", "退出"};
        return JOptionPane.showOptionDialog(null,
                """
                        本工具通过 netsh 修改 Windows 端口转发与防火墙规则，
                        这些操作要求进程具备管理员权限，而当前进程不是管理员。

                        · 以管理员身份重启：弹出 UAC 提示后以管理员身份重新打开本工具
                        · 只读模式查看：可以刷新、查看规则和复制命令，但不能增删
                        """,
                "需要管理员权限",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
    }
}
