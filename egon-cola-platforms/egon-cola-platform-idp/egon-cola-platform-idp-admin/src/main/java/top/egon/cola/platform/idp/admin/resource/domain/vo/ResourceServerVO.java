package top.egon.cola.platform.idp.admin.resource.domain.vo;

import java.time.Instant;

/**
 * Resource Server 管理视图。
 *
 * <p>Resource Server administration view.</p>
 *
 * @param resourceServerId 稳定标识；stable identifier
 * @param resourceUri Resource URI；Resource URI
 * @param bizCode 业务域；business domain
 * @param appCode 应用；application
 * @param environment 环境；environment
 * @param displayName 展示名称；display name
 * @param managementClientId 管理 Client；management Client
 * @param rbacApplicationCode RBAC3 应用；RBAC3 application
 * @param entryPermissionCode USER 入口权限；USER entry permission
 * @param status Resource Server 状态；Resource Server status
 * @param version 乐观锁和投影版本；optimistic-lock and projection version
 * @param createdAt 创建时间；creation instant
 * @param updatedAt 最后更新时间；last update instant
 */
public record ResourceServerVO(
        String resourceServerId,
        String resourceUri,
        String bizCode,
        String appCode,
        String environment,
        String displayName,
        String managementClientId,
        String rbacApplicationCode,
        String entryPermissionCode,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
