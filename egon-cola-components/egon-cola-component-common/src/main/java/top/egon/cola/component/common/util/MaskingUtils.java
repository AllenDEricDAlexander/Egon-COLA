package top.egon.cola.component.common.util;

/**
 * 敏感信息脱敏工具，提供企业高频字段的默认脱敏规则。
 */
public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static String mobile(String value) {
        return range(value, 3, 7);
    }

    public static String email(String value) {
        if (StringUtils.isBlank(value) || !value.contains("@")) {
            return value;
        }
        int atIndex = value.indexOf('@');
        String prefix = value.substring(0, atIndex);
        String suffix = value.substring(atIndex);
        if (prefix.length() <= 1) {
            return "*" + suffix;
        }
        return prefix.charAt(0) + "***" + suffix;
    }

    public static String idCard(String value) {
        return range(value, 6, Math.max(6, value == null ? 0 : value.length() - 4));
    }

    public static String bankCard(String value) {
        return range(value, 4, Math.max(4, value == null ? 0 : value.length() - 4));
    }

    public static String name(String value) {
        if (StringUtils.isBlank(value) || value.length() == 1) {
            return value;
        }
        return value.charAt(0) + "*".repeat(value.length() - 1);
    }

    public static String range(String value, int startInclusive, int endExclusive) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        int start = Math.max(0, startInclusive);
        int end = Math.min(value.length(), Math.max(start, endExclusive));
        if (start >= end) {
            return value;
        }
        return value.substring(0, start) + "*".repeat(end - start) + value.substring(end);
    }
}
