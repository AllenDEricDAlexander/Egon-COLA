package top.egon.cola.component.common.mask;

/**
 * Rule describing how many characters to keep around masked content.
 */
public class MaskRule {

    private final int keepStart;

    private final int keepEnd;

    private final char maskChar;

    private MaskRule(int keepStart, int keepEnd, char maskChar) {
        this.keepStart = Math.max(keepStart, 0);
        this.keepEnd = Math.max(keepEnd, 0);
        this.maskChar = maskChar;
    }

    public static MaskRule keepAround(int keepStart, int keepEnd) {
        return new MaskRule(keepStart, keepEnd, '*');
    }

    public int getKeepStart() {
        return keepStart;
    }

    public int getKeepEnd() {
        return keepEnd;
    }

    public char getMaskChar() {
        return maskChar;
    }
}
