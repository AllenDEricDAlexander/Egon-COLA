package top.egon.cola.component.common.mask;

/**
 * Common data masking helpers.
 */
public final class Masking {

    private Masking() {
    }

    public static String mobile(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() < 7) {
            return keepAround(value, MaskRule.keepAround(1, 1));
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String email(String value) {
        if (value == null) {
            return null;
        }
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return keepAround(value, MaskRule.keepAround(1, 1));
        }
        String local = value.substring(0, atIndex);
        return keepAround(local, MaskRule.keepAround(1, 1)) + value.substring(atIndex);
    }

    public static String keepAround(String value, MaskRule rule) {
        if (value == null) {
            return null;
        }
        int keepStart = rule.getKeepStart();
        int keepEnd = rule.getKeepEnd();
        if (value.length() <= keepStart + keepEnd) {
            return value;
        }
        int maskLength = value.length() - keepStart - keepEnd;
        return value.substring(0, keepStart)
                + String.valueOf(rule.getMaskChar()).repeat(maskLength)
                + value.substring(value.length() - keepEnd);
    }
}
