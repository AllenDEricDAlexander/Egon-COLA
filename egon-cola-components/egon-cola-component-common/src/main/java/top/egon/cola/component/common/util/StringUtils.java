package top.egon.cola.component.common.util;

/**
 * 字符串工具门面，优先复用 Apache Commons Lang。
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String value) {
        return org.apache.commons.lang3.StringUtils.isBlank(value);
    }

    public static boolean isNotBlank(String value) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(value);
    }

    public static String defaultIfBlank(String value, String defaultValue) {
        return org.apache.commons.lang3.StringUtils.defaultIfBlank(value, defaultValue);
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0) {
            return value;
        }
        return org.apache.commons.lang3.StringUtils.truncate(value, maxLength);
    }

    public static boolean equals(String left, String right) {
        return org.apache.commons.lang3.StringUtils.equals(left, right);
    }

    public static boolean equalsIgnoreCase(String left, String right) {
        return org.apache.commons.lang3.StringUtils.equalsIgnoreCase(left, right);
    }
}
