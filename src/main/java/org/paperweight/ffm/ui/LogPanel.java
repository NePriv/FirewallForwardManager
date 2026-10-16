package org.paperweight.ffm.ui;

import org.paperweight.ffm.exec.CommandResult;
import org.paperweight.ffm.service.OperationResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志面板：展示实际执行的 netsh 命令与原始输出，方便排障与复制。
 */
public final class LogPanel extends JPanel {

    private static final int MAX_LINES = 4000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea area = new JTextArea();

    public LogPanel() {
        super(new BorderLayout());
        area.setEditable(false);
        area.setLineWrap(false);
        // 固定行数让日志区在 BorderLayout 中拥有合理高度
        area.setRows(12);
        area.setMargin(new Insets(6, 8, 6, 8));
        area.setBorder(BorderFactory.createTitledBorder("执行日志（可复制命令后手工执行）"));
        area.setFont(UITheme.monospacedFont(12));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JButton copyButton = new JButton("复制日志");
        copyButton.addActionListener(e -> copyToClipboard());
        JButton clearButton = new JButton("清空日志");
        clearButton.addActionListener(e -> area.setText(""));
        toolbar.add(copyButton);
        toolbar.add(clearButton);
        toolbar.add(Box.createHorizontalGlue());

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void info(String message) {
        append("· " + stamp() + " " + message);
    }

    public void success(String message) {
        append("✔ " + stamp() + " " + message);
    }

    public void failure(String message) {
        append("✖ " + stamp() + " " + message);
    }

    /** 记录一次操作及其执行的命令。 */
    public void operation(String title, OperationResult<?> result) {
        // 先写结论，再逐条附上命令与输出
        if (result.ok()) {
            success(title + "：" + result.message());
        } else {
            failure(title + "：" + result.message());
        }
        commands(result.commands());
    }

    public void commands(List<CommandResult> results) {
        for (CommandResult result : results) {
            command(result);
        }
    }

    public void command(CommandResult result) {
        StringBuilder header = new StringBuilder("> ").append(result.commandLine());
        if (!result.ok()) {
            header.append("   [")
                    .append(result.started() ? "退出码 " + result.exitCode() : "未启动")
                    .append(']');
        }
        append(header.toString());
        String output = result.output();
        if (output.isBlank()) {
            if (!result.ok()) {
                append("    （无输出）");
            }
            return;
        }
        for (String line : output.split("\\R")) {
            append("    " + line);
        }
    }

    private String stamp() {
        return LocalTime.now().format(TIME_FORMAT);
    }

    private void append(String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> append(line));
            return;
        }
        area.append(line + System.lineSeparator());
        int excess = area.getLineCount() - MAX_LINES;
        if (excess > 0) {
            try {
                area.replaceRange("", 0, area.getLineEndOffset(excess - 1));
            } catch (BadLocationException ignored) {
                // 行号越界时忽略，最多保留略多的行数
            }
        }
        area.setCaretPosition(area.getDocument().getLength());
    }

    private void copyToClipboard() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(area.getText()), null);
    }
}
