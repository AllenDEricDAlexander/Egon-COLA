package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对明确应用集合增删 Client Resource Grant 的输入。
 *
 * <p>Input for adding or deleting Client Resource Grants for explicit applications.</p>
 *
 * @param bizCode 业务域；business domain
 * @param environment 环境；environment
 * @param appCodes 明确应用集合；explicit application codes
 * @param action Grant 动作；Grant action
 * @param grantType 授权类型；grant type
 * @param tenantId 服务授权租户；service-grant tenant
 * @param allowedScopes 服务 Scope；service scopes
 * @param expectedResourceVersions 各 Resource 期望版本；expected Resource versions
 * @param expectedGrantVersions 各已有 Grant 期望版本；expected existing Grant versions
 */
public record BatchClientResourceGrantDTO(
        @NotBlank String bizCode,
        @NotBlank String environment,
        @NotEmpty List<@NotBlank String> appCodes,
        @NotNull Action action,
        @NotNull ResourceGrantType grantType,
        String tenantId,
        @NotNull Set<String> allowedScopes,
        @NotNull Map<String, Long> expectedResourceVersions,
        @NotNull Map<String, Long> expectedGrantVersions
) {

    /**
     * 复制所有集合，保证批量请求不可变。
     *
     * <p>Copies all collections to keep the batch request immutable.</p>
     */
    public BatchClientResourceGrantDTO {
        appCodes = appCodes == null ? null : List.copyOf(appCodes);
        allowedScopes = allowedScopes == null ? null : Set.copyOf(allowedScopes);
        expectedResourceVersions = expectedResourceVersions == null
                ? null : Map.copyOf(expectedResourceVersions);
        expectedGrantVersions = expectedGrantVersions == null
                ? null : Map.copyOf(expectedGrantVersions);
    }

    /**
     * Grant 批量动作。
     *
     * <p>Grant batch action.</p>
     */
    public enum Action {

        /** 新建或更新逐应用 Grant；create or update per-application Grants. */
        UPSERT,

        /** 删除逐应用 Grant；delete per-application Grants. */
        DELETE
    }
}
