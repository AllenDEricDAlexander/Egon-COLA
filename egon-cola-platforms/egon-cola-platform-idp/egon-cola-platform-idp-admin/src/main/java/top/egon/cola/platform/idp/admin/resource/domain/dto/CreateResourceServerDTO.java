package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建应用环境级 Resource Server 的输入。
 *
 * <p>Input for creating an application-and-environment scoped Resource Server.</p>
 *
 * @param resourceServerId 稳定标识；stable identifier
 * @param resourceUri RFC 8707 Resource URI；RFC 8707 Resource URI
 * @param bizCode 业务域编码；business-domain code
 * @param appCode 应用编码；application code
 * @param environment 环境；environment
 * @param displayName 展示名称；display name
 * @param managementClientId 管理和机器身份 Client；management and machine-identity Client
 * @param rbacApplicationCode USER 入口权限所属应用；application owning USER entry permission
 * @param entryPermissionCode USER 入口权限；USER entry permission
 */
public record CreateResourceServerDTO(
        @NotBlank String resourceServerId,
        @NotBlank String resourceUri,
        @NotBlank String bizCode,
        @NotBlank String appCode,
        @NotBlank String environment,
        @NotBlank String displayName,
        @NotBlank String managementClientId,
        @NotBlank String rbacApplicationCode,
        @NotBlank String entryPermissionCode
) {
}
