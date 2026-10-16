package org.paperweight.ffm.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/**
 * 统一界面外观：FlatLaf 亮色主题 + 优先使用中文友好的字体。
 */
public final class UITheme {

    private static final List<String> PREFERRED_FONT_FAMILIES =
            List.of("Microsoft YaHei UI", "Microsoft YaHei", "Segoe UI");
    private static final List<String> PREFERRED_MONOSPACED_FAMILIES =
            List.of("NSimSun", "SimSun", "Consolas");

    private static List<String> availableFamilies;

    private UITheme() {
    }

    public static void install() {
        FlatLightLaf.setup();

        String family = pickFontFamily();
        if (family != null) {
            Font base = UIManager.getFont("Label.font");
            int size = base != null ? base.getSize() + 1 : 13;
            UIManager.put("defaultFont", new Font(family, Font.PLAIN, size));
        }

        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.alternateRowColor", true);
        UIManager.put("SplitPane.dividerSize", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.width", 14);
        UIManager.put("ToolTip.showQuickTips", true);
    }

    /** 返回等宽字体，优先选择带中文字形的字体。 */
    public static Font monospacedFont(int size) {
        List<String> available = availableFamilies();
        for (String family : PREFERRED_MONOSPACED_FAMILIES) {
            if (available.contains(family)) {
                return new Font(family, Font.PLAIN, size);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    private static String pickFontFamily() {
        List<String> available = availableFamilies();
        for (String candidate : PREFERRED_FONT_FAMILIES) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static synchronized List<String> availableFamilies() {
        if (availableFamilies == null) {
            availableFamilies = Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        }
        return availableFamilies;
    }
}
