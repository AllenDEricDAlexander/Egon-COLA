package top.egon.cola.component.common.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import top.egon.cola.component.common.exception.SystemException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 加密摘要工具，提供摘要、HMAC 和常用编码能力。
 */
public final class CryptoUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private CryptoUtils() {
    }

    public static String md5Hex(String value) {
        return DigestUtils.md5Hex(value);
    }

    public static String sha256Hex(String value) {
        return DigestUtils.sha256Hex(value);
    }

    public static String hmacSha256Hex(String value, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Hex.encodeHexString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new SystemException("CRYPTO_ERROR", "HMAC-SHA256 计算失败", e);
        }
    }

    public static String base64Encode(String value) {
        return Base64.encodeBase64String(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64DecodeToString(String value) {
        return new String(Base64.decodeBase64(value), StandardCharsets.UTF_8);
    }

    public static String hexEncode(String value) {
        return Hex.encodeHexString(value.getBytes(StandardCharsets.UTF_8));
    }
}
