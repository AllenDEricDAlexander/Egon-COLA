package top.egon.cola.component.rpc.ddc.security;

/**
 * DDC RPC 客户端签名凭据。
 * / Signing credential for a DDC RPC client.
 *
 * @param accessKey 公开访问键 / public access key
 * @param secretKey HMAC 密钥 / HMAC secret key
 */
public record DdcRpcCredential(String accessKey, String secretKey) {

    public DdcRpcCredential {
        accessKey = require(accessKey, "accessKey");
        secretKey = require(secretKey, "secretKey");
    }

    /**
     * 返回不包含密钥内容的诊断文本。
     * / Returns diagnostic text that never exposes the secret.
     */
    @Override
    public String toString() {
        return "DdcRpcCredential[accessKey=" + accessKey
                + ", secretKey=<redacted>]";
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
