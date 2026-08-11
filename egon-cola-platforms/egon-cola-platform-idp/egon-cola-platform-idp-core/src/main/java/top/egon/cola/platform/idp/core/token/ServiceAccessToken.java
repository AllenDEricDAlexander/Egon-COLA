package top.egon.cola.platform.idp.core.token;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 不含 Refresh Token 的 OAuth Client Credentials 签发结果。
 *
 * <p>OAuth Client Credentials issuance result without a refresh token.</p>
 *
 * @param accessToken 已签名 SERVICE Access Token；signed SERVICE access token
 * @param tokenType OAuth Token 类型；OAuth token type
 * @param expiresAt Access Token 过期时间；access-token expiration instant
 * @param scopes 实际签发的 Scope；actually issued scopes
 */
public record ServiceAccessToken(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        Set<String> scopes
) {

    /**
     * 校验服务 Token 签发结果。
     *
     * <p>Validates the service-token issuance result.</p>
     */
    public ServiceAccessToken {
        accessToken = required(accessToken, "accessToken");
        tokenType = required(tokenType, "tokenType");
        if (!"Bearer".equals(tokenType)) {
            throw new IllegalArgumentException("tokenType must be Bearer");
        }
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(scopes, "scopes");
        TreeSet<String> normalized = new TreeSet<>();
        scopes.forEach(scope -> normalized.add(required(scope, "scope")));
        if (normalized.isEmpty() || normalized.size() != scopes.size()) {
            throw new IllegalArgumentException(
                    "scopes must be non-empty and distinct"
            );
        }
        scopes = Collections.unmodifiableSet(normalized);
    }

    /**
     * 生成 HTTP Authorization 请求头值。
     *
     * <p>Builds the HTTP Authorization header value.</p>
     *
     * @return Bearer 请求头；Bearer header value
     */
    public String authorizationHeader() {
        return tokenType + " " + accessToken;
    }

    /**
     * 校验必填文本。
     *
     * <p>Validates required text.</p>
     *
     * @param value 待校验值；value to validate
     * @param field 字段名；field name
     * @return 已校验值；validated value
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
