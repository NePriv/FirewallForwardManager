package org.paperweight.ffm.ui;

import org.paperweight.ffm.exec.CommandRunner;
import org.paperweight.ffm.model.ProxyEntry;
import org.paperweight.ffm.service.OperationResult;

import java.util.List;

/** 界面冒烟测试：验证主窗口能完整创建（无图形环境时跳过）。 */
class MainFrameSmokeTest {

    @org.junit.jupiter.api.Test
    void buildsMainWindowInReadOnlyMode() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(), "无图形环境，跳过界面冒烟测试");

        UITheme.install();
        final javax.swing.JFrame[] holder = new javax.swing.JFrame[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame(true);
            frame.setVisible(false);
            holder[0] = frame;
        });

        javax.swing.JFrame frame = holder[0];
        try {
            org.junit.jupiter.api.Assertions.assertNotNull(frame);
            org.junit.jupiter.api.Assertions.assertTrue(frame.getTitle().contains("只读模式"),
                    () -> "只读模式标题不正确: " + frame.getTitle());
            org.junit.jupiter.api.Assertions.assertTrue(frame.getContentPane().getComponentCount() > 0,
                    "主窗口内容为空");
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @org.junit.jupiter.api.Test
    void refreshingTwiceKeepsManagerUsable() {
        // 管理器本身不依赖界面，这里只验证多次「读取快照」不会抛异常（不需要管理员权限）
        CommandRunner runner = new CommandRunner();
        var manager = new org.paperweight.ffm.service.ForwardManager(runner);

        OperationResult<org.paperweight.ffm.service.ForwardManager.Snapshot> first = manager.snapshot();
        OperationResult<org.paperweight.ffm.service.ForwardManager.Snapshot> second = manager.snapshot();

        org.junit.jupiter.api.Assertions.assertTrue(first.commands().size() >= 1, "应至少执行一条 netsh 命令");
        org.junit.jupiter.api.Assertions.assertTrue(second.commands().size() >= 1, "应至少执行一条 netsh 命令");
        List<ProxyEntry> entries = second.ok() ? second.value().entries() : List.of();
        System.out.println("[界面冒烟] 第二次刷新得到 " + entries.size() + " 条转发规则");
    }
}
