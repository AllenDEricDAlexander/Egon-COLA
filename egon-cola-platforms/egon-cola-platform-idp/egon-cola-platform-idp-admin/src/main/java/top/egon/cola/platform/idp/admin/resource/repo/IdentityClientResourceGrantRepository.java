package top.egon.cola.platform.idp.admin.resource.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;

import java.util.List;
import java.util.Optional;

/**
 * OAuth Client Resource Grant 仓储。
 *
 * <p>Repository for OAuth Client Resource Grants.</p>
 */
public interface IdentityClientResourceGrantRepository
        extends JpaRepository<IdentityClientResourceGrantEntity, String> {

    /**
     * 查询指定 Client、授权类型和状态的授权。
     *
     * <p>Finds grants by Client, grant type, and status.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param grantType 授权类型；grant type
     * @param status 授权状态；grant status
     * @return 授权列表；grant list
     */
    List<IdentityClientResourceGrantEntity>
            findByClientIdAndGrantTypeAndStatus(
            String clientId,
            IdentityClientResourceGrantEntity.GrantType grantType,
            IdentityClientResourceGrantEntity.Status status
    );

    /**
     * 按 Client、Resource、类型和租户精确查询授权。
     *
     * <p>Finds a grant by Client, Resource, type, and tenant.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param grantType 授权类型；grant type
     * @param tenantId 租户，用户委托时为空；tenant, null for user delegation
     * @return 授权；grant
     */
    Optional<IdentityClientResourceGrantEntity>
            findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
            String clientId,
            String resourceServerId,
            IdentityClientResourceGrantEntity.GrantType grantType,
            String tenantId
    );

    /**
     * 判断应用级授权是否存在。
     *
     * <p>Checks whether an application-level grant exists.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param grantType 授权类型；grant type
     * @return 存在时为 {@code true}；{@code true} when present
     */
    boolean existsByClientIdAndResourceServerIdAndGrantType(
            String clientId,
            String resourceServerId,
            IdentityClientResourceGrantEntity.GrantType grantType
    );

    /**
     * 删除一个应用级授权。
     *
     * <p>Deletes one application-level grant.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param grantType 授权类型；grant type
     */
    void deleteByClientIdAndResourceServerIdAndGrantType(
            String clientId,
            String resourceServerId,
            IdentityClientResourceGrantEntity.GrantType grantType
    );
}
