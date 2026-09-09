package top.egon.cola.component.yuheng.contract.reporting;

import top.egon.cola.component.common.crypto.hmac.Hmacs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 定义 Gateway 报告 HMAC 请求头并提供签名能力。
 * Defines Gateway report HMAC headers and signing behavior.
 */
public final class GatewayRequestSigner {

    /** Yuheng 向 Tianshu 报告时使用的访问密钥请求头。 */
    public static final String ACCESS_KEY_HEADER = "X-TIANSHU-Access-Key";

    /** Yuheng 向 Tianshu 报告时使用的时间戳请求头。 */
    public static final String TIMESTAMP_HEADER = "X-TIANSHU-Timestamp";

    /** Yuheng 向 Tianshu 报告时使用的 nonce 请求头。 */
    public static final String NONCE_HEADER = "X-TIANSHU-Nonce";

    /** Yuheng 向 Tianshu 报告时使用的内容摘要请求头。 */
    public static final String CONTENT_SHA256_HEADER =
            "X-TIANSHU-Content-SHA256";

    /** Yuheng 向 Tianshu 报告时使用的签名请求头。 */
    public static final String SIGNATURE_HEADER = "X-TIANSHU-Signature";

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
