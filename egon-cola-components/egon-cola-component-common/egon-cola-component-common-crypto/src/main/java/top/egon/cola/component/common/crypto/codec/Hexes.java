package top.egon.cola.component.common.crypto.codec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Hex UTF-8 codec helpers.
 */
public final class Hexes {

    private Hexes() {
    }

    public static String encodeToString(String value) {
        return HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeToString(String value) {
        return new String(HexFormat.of().parseHex(value), StandardCharsets.UTF_8);
    }
}
