package top.egon.cola.component.common.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具门面，优先复用 Apache Commons Lang。
 */
public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String value) {
        return StringUtils.isBlank(value);
    }

    public static boolean isNotBlank(String value) {
        return StringUtils.isNotBlank(value);
    }

    public static String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0) {
            return value;
        }
        return StringUtils.truncate(value, maxLength);
    }

    public static boolean equals(String left, String right) {
        return StringUtils.equals(left, right);
    }

    public static boolean equalsIgnoreCase(String left, String right) {
        return StringUtils.equalsIgnoreCase(left, right);
    }
}
