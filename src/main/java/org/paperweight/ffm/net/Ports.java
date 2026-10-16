package org.paperweight.ffm.net;

/**
 * 端口号校验。
 */
public final class Ports {

    public static final int MIN = 1;
    public static final int MAX = 65535;

    private Ports() {
    }

    public static boolean isValid(int port) {
        return port >= MIN && port <= MAX;
    }

    /** 解析端口文本，非法时返回 null。 */
    public static Integer parse(String text) {
        if (text == null) {
            return null;
        }
        String value = text.strip();
        if (value.isEmpty() || !value.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String describeRange() {
        return MIN + " - " + MAX;
    }
}
