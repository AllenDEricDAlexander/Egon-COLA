package top.egon.cola.component.gateway.contract.reporting;

import top.egon.cola.component.common.crypto.hmac.Hmacs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 定义 Gateway 报告 HMAC 请求头并提供签名能力。
 * Defines Gateway report HMAC headers and signing behavior.
 */
public final class GatewayRequestSigner {

    /** 为保持现有报告协议兼容而保留的访问密钥请求头。 */
    public static final String ACCESS_KEY_HEADER = "X-DDC-Access-Key";

    /** 为保持现有报告协议兼容而保留的时间戳请求头。 */
    public static final String TIMESTAMP_HEADER = "X-DDC-Timestamp";

    /** 为保持现有报告协议兼容而保留的 nonce 请求头。 */
    public static final String NONCE_HEADER = "X-DDC-Nonce";

    /** 为保持现有报告协议兼容而保留的内容摘要请求头。 */
    public static final String CONTENT_SHA256_HEADER =
            "X-DDC-Content-SHA256";

    /** 为保持现有报告协议兼容而保留的签名请求头。 */
    public static final String SIGNATURE_HEADER = "X-DDC-Signature";

    public String sign(
            GatewayCanonicalRequest request,
            String secretKey) {
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
