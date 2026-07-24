package top.egon.cola.component.ddc.security;

import top.egon.cola.component.common.crypto.hmac.Hmacs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class DdcRequestSigner {

    public static final String ACCESS_KEY_HEADER = "X-DDC-Access-Key";

    public static final String TIMESTAMP_HEADER = "X-DDC-Timestamp";

    public static final String NONCE_HEADER = "X-DDC-Nonce";

    public static final String CONTENT_SHA256_HEADER = "X-DDC-Content-SHA256";

    public static final String SIGNATURE_HEADER = "X-DDC-Signature";

    public String sign(DdcCanonicalRequest request, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey is required");
        }
        return Hmacs.sha256Hex(request.canonicalValue(), secretKey);
    }

    public boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
