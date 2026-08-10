package top.egon.cola.platform.idp.admin.resource.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
 * @param admissionTicketTtlSeconds 准入票据有效秒数；admission-ticket lifetime in seconds
 * @param key 初始公开 JWK；initial public JWK
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
        @NotBlank String entryPermissionCode,
        @Min(30) @Max(900) int admissionTicketTtlSeconds,
        @Valid @NotNull CreateClientJwkDTO key
) {
}
