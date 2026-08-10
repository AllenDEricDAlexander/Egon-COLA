package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;
import top.egon.cola.platform.idp.core.resource.ResourceServer;

import java.net.URI;
import java.util.Optional;

/**
 * Resource Server 与 Client Grant 的领域查询端口。
 *
 * <p>Domain lookup port for Resource Servers and Client Grants.</p>
 */
public interface ResourceServerStore {

    /**
     * 按内部标识查询 Resource。
     *
     * <p>Finds a Resource by internal identifier.</p>
     *
     * @param resourceServerId Resource 标识；Resource identifier
     * @return Resource；Resource
     */
    Optional<ResourceServer> findById(String resourceServerId);

    /**
     * 按 Resource URI 查询。
     *
     * <p>Finds a Resource by Resource URI.</p>
     *
     * @param resourceUri Resource URI；Resource URI
     * @return Resource；Resource
     */
    Optional<ResourceServer> findByUri(URI resourceUri);

    /**
     * 按精确业务域、应用和环境查询。
     *
     * <p>Finds a Resource by exact business domain, application, and environment.</p>
     *
     * @param bizCode     业务域；business domain
     * @param appCode     应用；application
     * @param environment 环境；environment
     * @return Resource；Resource
     */
    Optional<ResourceServer> findByScope(
            String bizCode,
            String appCode,
            String environment);

    /**
     * 查询 Client 代表的源 Resource。
     *
     * <p>Finds the source Resource represented by a Client.</p>
     *
     * @param clientId Management Client 标识；Management Client identifier
     * @return Resource；Resource
     */
    Optional<ResourceServer> findByManagementClientId(String clientId);

    /**
     * 查询一个精确 Client Resource Grant。
     *
     * <p>Finds one exact Client Resource Grant.</p>
     *
     * @param clientId         Client 标识；Client identifier
     * @param resourceServerId Resource 标识；Resource identifier
     * @param grantType        Grant 类型；Grant type
     * @param tenantId         服务租户，USER Grant 为空；service tenant, null for USER grants
     * @return Client Resource Grant；Client Resource Grant
     */
    Optional<ClientResourceGrant> findGrant(
            String clientId,
            String resourceServerId,
            ResourceGrantType grantType,
            String tenantId);
}
