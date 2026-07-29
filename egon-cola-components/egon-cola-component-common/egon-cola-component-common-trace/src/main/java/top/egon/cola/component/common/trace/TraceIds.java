package top.egon.cola.component.common.trace;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * W3C Trace Context identifier generation and validation helpers.
 */
public final class TraceIds {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HexFormat HEX = HexFormat.of();

    private TraceIds() {
    }

    public static String newTraceId() {
        return randomHex(16);
    }

    public static String newSpanId() {
        return randomHex(8);
    }

    public static boolean isValidTraceId(String value) {
        return normalizeTraceId(value).isPresent();
    }

    public static boolean isValidSpanId(String value) {
        return normalizeSpanId(value).isPresent();
    }

    public static Optional<String> normalizeTraceId(String value) {
        return normalizeHex(value, 32, true);
    }

    public static Optional<String> normalizeSpanId(String value) {
        return normalizeHex(value, 16, true);
    }

    public static boolean isValidTraceFlags(String value) {
        return normalizeHex(value, 2, false).isPresent();
    }

    public static Optional<String> normalizeTraceFlags(String value) {
        return normalizeHex(value, 2, false);
    }

    static boolean hasLineBreak(String value) {
        return value != null
                && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0);
    }

    private static Optional<String> normalizeHex(String value,
                                                 int length,
                                                 boolean rejectAllZeros) {
        if (value == null || hasLineBreak(value)) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != length
                || (rejectAllZeros && allZeros(normalized))) {
            return Optional.empty();
        }
        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (!((character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f'))) {
                return Optional.empty();
            }
        }
        return Optional.of(normalized);
    }

    private static boolean allZeros(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        String hex;
        do {
            RANDOM.nextBytes(value);
            hex = HEX.formatHex(value);
        } while (allZeros(hex));
        return hex;
    }
}
