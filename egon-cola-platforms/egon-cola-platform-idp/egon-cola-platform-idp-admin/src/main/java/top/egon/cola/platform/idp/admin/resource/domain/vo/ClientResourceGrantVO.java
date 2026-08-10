package top.egon.cola.platform.idp.admin.resource.domain.vo;

import java.util.Set;

/**
 * Client Resource Grant 管理视图。
 *
 * <p>Client Resource Grant administration view.</p>
 *
 * @param clientId Client 标识；Client identifier
 * @param resourceServerId Resource Server 标识；Resource Server identifier
 * @param grantType 授权类型；grant type
 * @param tenantId 服务授权租户；service-grant tenant
 * @param allowedScopes IdP 许可 Scope；scopes allowed by IdP
 * @param status 授权状态；grant status
 * @param version 乐观锁版本；optimistic-lock version
 */
public record ClientResourceGrantVO(
        String clientId,
        String resourceServerId,
        String grantType,
        String tenantId,
        Set<String> allowedScopes,
        String status,
        long version
) {

    /**
     * 复制 Scope 集合。
     *
     * <p>Copies the scope set.</p>
     */
    public ClientResourceGrantVO {
        allowedScopes = Set.copyOf(allowedScopes);
    }
}
