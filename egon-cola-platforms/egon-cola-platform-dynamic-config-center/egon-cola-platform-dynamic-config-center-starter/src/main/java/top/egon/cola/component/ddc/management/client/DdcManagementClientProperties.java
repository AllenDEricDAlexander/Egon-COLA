package top.egon.cola.component.ddc.management.client;

import java.net.URI;
import java.time.Duration;

/**
 * DDC 管理 HTTP 客户端的连接、认证与传输安全配置。 /
 * Connection, authentication, and transport-security settings for the DDC management HTTP client.
 *
 * @param endpoint 不含上下文路径的 DDC 管理服务 HTTP(S) 基础地址 / DDC management HTTP(S) base URL without a context path
 * @param accessKey HMAC 访问密钥标识 / HMAC access-key identifier
 * @param secretKey HMAC 签名密钥 / HMAC signing secret
 * @param connectTimeout 建立连接的正超时时间 / positive connection timeout
 * @param readTimeout 读取响应的正超时时间 / positive response-read timeout
 * @param transportSecurity 明文开发模式或 mTLS 传输配置 / plaintext-development or mTLS transport configuration
 */
public record DdcManagementClientProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout,
        DdcClientTransportSecurity transportSecurity
) {

    /**
     * 使用显式开发明文模式构造客户端配置。 /
     * Constructs client settings using explicit development plaintext mode.
     *
     * @param endpoint DDC 管理服务 HTTP 基础地址 / DDC management HTTP base URL
     * @param accessKey HMAC 访问密钥标识 / HMAC access-key identifier
     * @param secretKey HMAC 签名密钥 / HMAC signing secret
     * @param connectTimeout 建立连接的正超时时间 / positive connection timeout
     * @param readTimeout 读取响应的正超时时间 / positive response-read timeout
     * @throws IllegalArgumentException 当地址、凭据或超时配置无效时 / when the endpoint, credentials, or timeouts are invalid
     */
    public DdcManagementClientProperties(
            String endpoint,
            String accessKey,
            String secretKey,
            Duration connectTimeout,
            Duration readTimeout) {
        this(
                endpoint,
                accessKey,
                secretKey,
                connectTimeout,
                readTimeout,
                DdcClientTransportSecurity.developmentPlaintextConfig()
        );
    }

    /**
     * 校验并归一化客户端配置，同时强制地址协议与传输安全模式一致。 /
     * Validates and normalizes client settings while enforcing endpoint-scheme and transport-security consistency.
     *
     * @throws IllegalArgumentException 当任一配置无效或协议与安全模式不匹配时 / when any setting is invalid or the scheme conflicts with the security mode
     */
    public DdcManagementClientProperties {
        endpoint = normalizeEndpoint(endpoint);
        accessKey = requireText(accessKey, "accessKey");
        requireText(secretKey, "secretKey");
        connectTimeout = requirePositive(connectTimeout, "connectTimeout");
        readTimeout = requirePositive(readTimeout, "readTimeout");
        if (transportSecurity == null) {
            throw new IllegalArgumentException(
                    "transportSecurity is required"
            );
        }
        if (transportSecurity.enabled()
                && !endpoint.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "DDC mTLS endpoint must use HTTPS"
            );
        }
        if (!transportSecurity.enabled()
                && !endpoint.startsWith("http://")) {
            throw new IllegalArgumentException(
                    "DDC HTTPS endpoint requires configured mTLS"
            );
        }
    }

    /**
     * 返回屏蔽签名密钥后的配置摘要。 / Returns a settings summary with the signing secret redacted.
     *
     * @return 不包含明文签名密钥的配置摘要 / settings summary without the plaintext signing secret
     */
    @Override
    public String toString() {
        return "DdcManagementClientProperties[endpoint="
                + endpoint
                + ", accessKey="
                + accessKey
                + ", secretKey=******, connectTimeout="
                + connectTimeout
                + ", readTimeout="
                + readTimeout
                + ", transportSecurity="
                + (transportSecurity.enabled() ? "MTLS" : "DEVELOPMENT")
                + "]";
    }

    /**
     * 校验 HTTP(S) 基础地址并移除结尾斜杠。 / Validates an HTTP(S) base URL and removes its trailing slash.
     *
     * @param value 待规范化地址 / endpoint to normalize
     * @return 不含结尾斜杠的规范地址 / normalized endpoint without a trailing slash
     * @throws IllegalArgumentException 当地址无效或包含用户信息、查询、片段或上下文路径时 / when the endpoint is invalid or contains user info, query, fragment, or context path
     */
    private static String normalizeEndpoint(String value) {
        String endpoint = requireText(value, "endpoint");
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("endpoint must be a valid HTTP URI", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("endpoint must use HTTP or HTTPS");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("endpoint host is required");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "endpoint must not contain user info, query, or fragment"
            );
        }
        if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
            throw new IllegalArgumentException("endpoint must not contain a context path");
        }
        return endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
    }

    /**
     * 要求持续时间为正值。 / Requires a positive duration.
     *
     * @param value 待校验持续时间 / duration to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 原始正持续时间 / original positive duration
     * @throws IllegalArgumentException 当持续时间为空、为零或为负时 / when the duration is null, zero, or negative
     */
    private static Duration requirePositive(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /**
     * 要求文本非空并去除首尾空白。 / Requires nonblank text and trims it.
     *
     * @param value 待校验文本 / text to validate
     * @param fieldName 用于错误消息的字段名 / field name used in error messages
     * @return 已去除首尾空白的文本 / trimmed text
     * @throws IllegalArgumentException 当文本为空时 / when the text is blank
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
