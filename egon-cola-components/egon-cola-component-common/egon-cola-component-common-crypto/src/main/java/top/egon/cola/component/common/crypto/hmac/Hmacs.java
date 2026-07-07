package top.egon.cola.component.common.crypto.hmac;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * HMAC helpers.
 */
public final class Hmacs {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private Hmacs() {
    }

    public static String sha256Hex(String value, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 calculation failed", e);
        }
    }
}
