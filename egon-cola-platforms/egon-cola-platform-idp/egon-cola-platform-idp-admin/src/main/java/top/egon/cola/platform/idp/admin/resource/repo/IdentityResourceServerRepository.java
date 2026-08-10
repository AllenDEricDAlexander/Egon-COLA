package top.egon.cola.platform.idp.admin.resource.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Resource Server 持久化仓储。
 *
 * <p>Persistence repository for Resource Servers.</p>
 */
public interface IdentityResourceServerRepository
        extends JpaRepository<IdentityResourceServerEntity, String> {

    /**
     * 按稳定标识查询 Resource Server。
     *
     * <p>Finds a Resource Server by its stable identifier.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @return Resource Server；Resource Server
     */
    Optional<IdentityResourceServerEntity> findByResourceServerId(
            String resourceServerId
    );

    /**
     * 以数据库写锁读取待变更 Resource Server。
     *
     * <p>Loads a Resource Server for mutation under a database write lock.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @return Resource Server；Resource Server
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource from IdentityResourceServerEntity resource "
            + "where resource.resourceServerId = :resourceServerId")
    Optional<IdentityResourceServerEntity> findByResourceServerIdForUpdate(
            @Param("resourceServerId") String resourceServerId
    );

    /**
     * 按 Resource URI 查询。
     *
     * <p>Finds a Resource Server by Resource URI.</p>
     *
     * @param resourceUri Resource URI；Resource URI
     * @return Resource Server；Resource Server
     */
    Optional<IdentityResourceServerEntity> findByResourceUri(
            String resourceUri
    );

    /**
     * 按逻辑三元组查询。
     *
     * <p>Finds a Resource Server by its logical triple.</p>
     *
     * @param bizCode 业务域；business domain
     * @param appCode 应用；application
     * @param environment 环境；environment
     * @return Resource Server；Resource Server
     */
    Optional<IdentityResourceServerEntity>
            findByBizCodeAndAppCodeAndEnvironment(
            String bizCode,
            String appCode,
            String environment
    );

    /**
     * 按管理 Client 查询其唯一 Resource Server。
     *
     * <p>Finds the single Resource Server bound to a management Client.</p>
     *
     * @param managementClientId 管理 Client；management Client
     * @return Resource Server；Resource Server
     */
    Optional<IdentityResourceServerEntity> findByManagementClientId(
            String managementClientId
    );

    /**
     * 批量查询明确选中的应用。
     *
     * <p>Finds explicitly selected applications for a batch operation.</p>
     *
     * @param bizCode 业务域；business domain
     * @param environment 环境；environment
     * @param appCodes 明确应用集合；explicit application codes
     * @return Resource Server 列表；Resource Server list
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<IdentityResourceServerEntity>
            findByBizCodeAndEnvironmentAndAppCodeIn(
            String bizCode,
            String environment,
            Collection<String> appCodes
    );
}
