package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;

import java.util.Set;

/**
 * 新建或更新一个应用级 Client Resource Grant 的输入。
 *
 * <p>Input for creating or updating one application-level Client Resource Grant.</p>
 *
 * @param grantType 授权类型；grant type
 * @param tenantId 服务授权绑定租户；tenant bound to a service grant
 * @param allowedScopes IdP 许可的服务 Scope；service scopes allowed by IdP
 * @param expectedResourceVersion Resource Server 期望版本；expected Resource Server version
 * @param expectedGrantVersion 已有 Grant 期望版本，新建时为空；expected existing Grant version, null for create
 */
public record UpsertClientResourceGrantDTO(
        @NotNull ResourceGrantType grantType,
        String tenantId,
        @NotNull Set<String> allowedScopes,
        @PositiveOrZero long expectedResourceVersion,
        @PositiveOrZero Long expectedGrantVersion
) {

    /**
     * 复制调用方集合，避免请求对象被外部修改。
     *
     * <p>Copies the caller set so the request cannot be modified externally.</p>
     */
    public UpsertClientResourceGrantDTO {
        allowedScopes = allowedScopes == null ? null : Set.copyOf(allowedScopes);
    }
}
