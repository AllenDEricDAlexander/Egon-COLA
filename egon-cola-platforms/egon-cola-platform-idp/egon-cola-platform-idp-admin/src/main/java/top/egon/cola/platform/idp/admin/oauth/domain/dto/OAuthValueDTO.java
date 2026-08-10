package top.egon.cola.platform.idp.admin.oauth.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 客户端集合字段的单值变更输入。
 *
 * <p>Single-value mutation input for OAuth client collection fields.</p>
 */
public record OAuthValueDTO(@NotBlank String value) {
}
