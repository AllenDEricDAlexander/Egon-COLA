package top.egon.cola.component.common.crypto.codec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 UTF-8 codec helpers.
 */
public final class Base64s {

    private Base64s() {
    }

    public static String encodeToString(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeToString(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
