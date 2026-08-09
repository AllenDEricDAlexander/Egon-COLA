package top.egon.cola.component.rpc.ddc.security;

import top.egon.cola.component.common.crypto.hmac.Hmacs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * DDC RPC 规范请求的 HMAC-SHA256 签名器。
 * / HMAC-SHA256 signer for canonical DDC RPC requests.
 */
public final class DdcRpcRequestSigner {

    /** 生成小写十六进制签名。 / Generates a lowercase hexadecimal signature. */
    public String sign(DdcRpcCanonicalRequest request, String secretKey) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey is required");
        }
        return Hmacs.sha256Hex(request.canonicalValue(), secretKey);
    }

    /** 常量时间比较两个 ASCII 签名。 / Compares two ASCII signatures in constant time. */
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
