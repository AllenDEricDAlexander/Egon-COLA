package top.egon.cola.platform.idp.core.oauth;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * OAuth Client 注册信息；Resource 访问关系由独立 Grant 管理。
 * OAuth Client registration; Resource access relationships are managed by explicit grants.
 *
 * @param clientId Client 稳定标识 / stable Client identifier
 * @param clientType Client 类型 / Client type
 * @param status Client 状态 / Client status
 * @param pkceRequired 是否强制 PKCE / whether PKCE is required
 * @param redirectUris 精确登记的回调地址 / exactly registered redirect URIs
 * @param accessTokenTtl Access Token 有效期 / Access Token lifetime
 * @param refreshTokenTtl Refresh Token 有效期 / Refresh Token lifetime
 */
public record OAuthClient(
        String clientId,
        ClientType clientType,
        Status status,
        boolean pkceRequired,
        List<String> redirectUris,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {

    /** 默认 Access Token 有效期。 / Default Access Token lifetime. */
    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    /** 默认 Refresh Token 有效期。 / Default Refresh Token lifetime. */
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(7);

    /**
     * 校验并规范化 Client 注册信息。
     * Validates and normalizes the Client registration.
     */
    public OAuthClient {
        clientId = required(clientId, "clientId");
        clientType = Objects.requireNonNull(clientType, "clientType");
        status = Objects.requireNonNull(status, "status");
        redirectUris = normalizedDistinct(redirectUris, "redirectUris");
        accessTokenTtl = durationInRange(
                accessTokenTtl, Duration.ofMinutes(5), Duration.ofMinutes(30),
                "accessTokenTtl");
        refreshTokenTtl = durationInRange(
                refreshTokenTtl, Duration.ofDays(1), Duration.ofDays(30),
                "refreshTokenTtl");
        if (clientType == ClientType.PUBLIC && redirectUris.isEmpty()) {
            throw new IllegalArgumentException(
                    "public Client requires at least one redirect URI"
            );
        }
        if (clientType == ClientType.PUBLIC && !pkceRequired) {
            throw new IllegalArgumentException(
                    "public Client must require PKCE"
            );
        }
        if (clientType == ClientType.CONFIDENTIAL && pkceRequired) {
            throw new IllegalArgumentException(
                    "confidential Client must not require PKCE"
            );
        }
    }

    /**
     * 使用平台默认 Token 有效期创建 Client。
     * Creates a Client with platform-default token lifetimes.
     *
     * @param clientId Client 标识 / Client identifier
     * @param clientType Client 类型 / Client type
     * @param status Client 状态 / Client status
     * @param pkceRequired 是否强制 PKCE / whether PKCE is required
     * @param redirectUris 精确回调地址 / exact redirect URIs
     */
    public OAuthClient(
            String clientId,
            ClientType clientType,
            Status status,
            boolean pkceRequired,
            List<String> redirectUris) {
        this(clientId, clientType, status, pkceRequired, redirectUris,
                DEFAULT_ACCESS_TOKEN_TTL, DEFAULT_REFRESH_TOKEN_TTL);
    }

    /**
     * 判断回调地址是否被精确登记。
     * Determines whether a redirect URI is exactly registered.
     *
     * @param redirectUri 待校验回调地址 / redirect URI to validate
     * @return 精确登记时为 {@code true} / {@code true} when exactly registered
     */
    public boolean acceptsRedirectUri(String redirectUri) {
        return redirectUris.contains(redirectUri);
    }

    /**
     * 创建状态变更后的副本。
     * Creates a copy with a changed status.
     *
     * @param value 新状态 / new status
     * @return 状态变更后的 Client / Client with the changed status
     */
    public OAuthClient withStatus(Status value) {
        return new OAuthClient(clientId, clientType, value, pkceRequired,
                redirectUris, accessTokenTtl, refreshTokenTtl);
    }

    /**
     * 校验时长范围。
     * Validates a duration range.
     *
     * @param value 待校验时长 / duration to validate
     * @param minimum 最小值 / minimum
     * @param maximum 最大值 / maximum
     * @param field 字段名 / field name
     * @return 已校验时长 / validated duration
     */
    private static Duration durationInRange(
            Duration value, Duration minimum, Duration maximum, String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is out of range");
        }
        return value;
    }

    /**
     * 规范化并拒绝重复文本集合。
     * Normalizes a text collection and rejects duplicates.
     *
     * @param values 原始值 / raw values
     * @param field 字段名 / field name
     * @return 排序后的不可变集合 / sorted immutable values
     */
    private static List<String> normalizedDistinct(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        List<String> normalized = values.stream()
                .map(value -> required(value, field)).sorted().distinct().toList();
        if (normalized.size() != values.size()) {
            throw new IllegalArgumentException(field + " contains duplicates");
        }
        return normalized;
    }

    /**
     * 校验不带首尾空白的必填文本。
     * Validates required text without surrounding whitespace.
     *
     * @param value 待校验值 / value to validate
     * @param field 字段名 / field name
     * @return 已校验文本 / validated text
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /** OAuth Client 类型。 / OAuth Client type. */
    public enum ClientType {
        /** 浏览器或本地应用公开 Client。 / Public browser or native-app Client. */
        PUBLIC,
        /** 能安全保存机器凭证的机密 Client。 / Confidential Client that protects credentials. */
        CONFIDENTIAL
    }

    /** OAuth Client 状态。 / OAuth Client status. */
    public enum Status {
        /** Client 可参与授权。 / Client may participate in authorization. */
        ACTIVE,
        /** Client 被禁用。 / Client is disabled. */
        DISABLED
    }
}
