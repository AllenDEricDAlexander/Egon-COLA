package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 访问令牌响应，刷新令牌仍仅通过 HttpOnly Cookie 传输。
 *
 * <p>OAuth access-token response; the refresh token remains confined to an HttpOnly cookie.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OAuthTokenVO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}
