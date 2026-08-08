package top.egon.cola.component.ddc.model.security;

import top.egon.cola.component.common.crypto.hmac.Hmacs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 定义 DDC HMAC 请求头，并负责签名生成和常量时间比较。 Defines DDC HMAC headers and provides signature generation and constant-time comparison.
 */
public final class DdcRequestSigner {

    /**
     * 访问密钥请求头名称。 Access-key request header name.
     */
    public static final String ACCESS_KEY_HEADER = "X-DDC-Access-Key";

    /**
     * 请求时间戳请求头名称。 Request-timestamp header name.
     */
    public static final String TIMESTAMP_HEADER = "X-DDC-Timestamp";

    /**
     * 防重放 nonce 请求头名称。 Anti-replay nonce header name.
     */
    public static final String NONCE_HEADER = "X-DDC-Nonce";

    /**
     * 请求体 SHA-256 摘要头名称。 Request-body SHA-256 header name.
     */
    public static final String CONTENT_SHA256_HEADER = "X-DDC-Content-SHA256";

    /**
     * HMAC-SHA256 签名头名称。 HMAC-SHA256 signature header name.
     */
    public static final String SIGNATURE_HEADER = "X-DDC-Signature";

    /**
     * 使用密钥对规范请求文本计算 HMAC-SHA256 十六进制签名。 Computes a hexadecimal HMAC-SHA256 signature for canonical request text using the secret key.
     *
     * @param request   规范请求。 canonical request
     * @param secretKey HMAC 密钥。 HMAC secret key
     * @return 十六进制签名。 hexadecimal signature
     * @throws IllegalArgumentException 密钥为空或空白时抛出。 thrown when the secret key is null or blank
     */
    public String sign(DdcCanonicalRequest request, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey is required");
        }
        return Hmacs.sha256Hex(request.canonicalValue(), secretKey);
    }

    /**
     * 以 ASCII 字节执行常量时间签名比较。 Compares signatures in constant time over their ASCII bytes.
     *
     * @param expected 期望签名。 expected signature
     * @param actual   实际签名。 actual signature
     * @return 两个非空签名完全一致时为 {@code true}。 {@code true} when both non-null signatures match exactly
     */
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
