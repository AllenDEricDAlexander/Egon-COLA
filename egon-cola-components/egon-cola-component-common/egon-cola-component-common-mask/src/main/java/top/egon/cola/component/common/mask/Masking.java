package top.egon.cola.component.common.mask;

/**
 * Common data masking helpers.
 */
public final class Masking {

    private static final int MOBILE_KEEP_START = 3;

    private static final int MOBILE_KEEP_END = 4;

    /**
     * Minimum number of characters the mobile window must hide before it may be applied. Below this
     * the conservative {@code keepAround(1, 1)} window is used, which always hides more.
     */
    private static final int MOBILE_MIN_MASKED = 4;

    private Masking() {
    }

    public static String mobile(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= MOBILE_KEEP_START + MOBILE_KEEP_END + MOBILE_MIN_MASKED) {
            return keepAround(value, MaskRule.keepAround(MOBILE_KEEP_START, MOBILE_KEEP_END));
        }
        return keepAround(value, MaskRule.keepAround(1, 1));
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

    /**
     * Masks {@code value}, keeping the leading and trailing characters described by {@code rule}.
     *
     * <p>Fails closed: when the kept window would span the whole value, every character is masked
     * rather than returning the input unmasked.
     */
    public static String keepAround(String value, MaskRule rule) {
        if (value == null) {
            return null;
        }
        int keepStart = rule.getKeepStart();
        int keepEnd = rule.getKeepEnd();
        if (value.length() <= keepStart + keepEnd) {
            return String.valueOf(rule.getMaskChar()).repeat(value.length());
        }
        int maskLength = value.length() - keepStart - keepEnd;
        return value.substring(0, keepStart)
                + String.valueOf(rule.getMaskChar()).repeat(maskLength)
                + value.substring(value.length() - keepEnd);
    }
}
