package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;

/**
 * 删除 Client Resource Grant 的输入。
 *
 * <p>Input for deleting a Client Resource Grant.</p>
 *
 * @param grantType 授权类型；grant type
 * @param tenantId 服务授权绑定租户；tenant bound to a service grant
 * @param expectedResourceVersion Resource Server 期望版本；expected Resource Server version
 * @param expectedGrantVersion Grant 期望版本；expected Grant version
 */
public record DeleteClientResourceGrantDTO(
        @NotNull ResourceGrantType grantType,
        String tenantId,
        @PositiveOrZero long expectedResourceVersion,
        @PositiveOrZero long expectedGrantVersion
) {
}
