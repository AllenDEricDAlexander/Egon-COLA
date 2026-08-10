package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 标准 OAuth 错误响应。
 *
 * <p>Standard OAuth error response.</p>
 */
public record OAuthErrorVO(
        String error,
        @JsonProperty("error_description") String errorDescription
) {
}
