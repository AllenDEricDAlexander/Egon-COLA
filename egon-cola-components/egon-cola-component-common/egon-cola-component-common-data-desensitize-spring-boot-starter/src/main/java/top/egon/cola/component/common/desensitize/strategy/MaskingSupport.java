package top.egon.cola.component.common.desensitize.strategy;

public final class MaskingSupport {

    private static final String MASK = "*";

    private MaskingSupport() {
    }

    public static String keepAround(String value, int keepStart, int keepEnd) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int[] codePoints = value.codePoints().toArray();
        if (codePoints.length <= keepStart + keepEnd) {
            return MASK.repeat(codePoints.length);
        }
        return new String(codePoints, 0, keepStart)
                + MASK.repeat(codePoints.length - keepStart - keepEnd)
                + new String(codePoints, codePoints.length - keepEnd, keepEnd);
    }

    public static String keepStart(String value, int keepStart) {
        return keepAround(value, keepStart, 0);
    }

    public static String full(String value) {
        return keepAround(value, 0, 0);
    }
}
