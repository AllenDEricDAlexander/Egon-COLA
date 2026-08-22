package top.egon.cola.platform.idp.admin.resource.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 从 IdP 管理库查询 Resource Server 及 Client Grant 的领域适配器。
 *
 * <p>Domain adapter that queries Resource Servers and Client Grants from the IdP administration
 * database.</p>
 */
public class JpaResourceServerStore implements ResourceServerStore {

    /** Resource Server 仓储；Resource Server repository. */
    private final IdentityResourceServerRepository resources;

    /** Client Resource Grant 仓储；Client Resource Grant repository. */
    private final IdentityClientResourceGrantRepository grants;

    /** JSON 编解码器；JSON codec. */
    private final ObjectMapper objectMapper;

    /**
     * 创建 Resource Server 查询适配器。
     *
     * <p>Creates the Resource Server lookup adapter.</p>
     *
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     * @param objectMapper JSON 编解码器；JSON codec
     */
    public JpaResourceServerStore(
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            ObjectMapper objectMapper
    ) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 按稳定标识查询 Resource Server。
     *
     * <p>Finds a Resource Server by stable identifier.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @return Resource Server；Resource Server
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findById(String resourceServerId) {
        return resources.findByResourceServerId(resourceServerId)
                .map(this::toDomain);
    }

    /**
     * 按 RFC 8707 Resource URI 查询 Resource Server。
     *
     * <p>Finds a Resource Server by RFC 8707 Resource URI.</p>
     *
     * @param resourceUri Resource URI；Resource URI
     * @return Resource Server；Resource Server
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findByUri(URI resourceUri) {
        Objects.requireNonNull(resourceUri, "resourceUri");
        return resources.findByResourceUri(resourceUri.toString())
                .map(this::toDomain);
    }

    /**
     * 按业务域、应用和环境三元组查询 Resource Server。
     *
     * <p>Finds a Resource Server by business-domain, application, and environment triple.</p>
     *
     * @param bizCode 业务域；business domain
     * @param appCode 应用；application
     * @param environment 环境；environment
     * @return Resource Server；Resource Server
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findByScope(
            String bizCode,
            String appCode,
            String environment
    ) {
        return resources.findByBizCodeAndAppCodeAndEnvironment(
                        bizCode,
                        appCode,
                        environment
                )
                .map(this::toDomain);
    }

    /**
     * 按管理 Client 查询其代表的源 Resource Server。
     *
     * <p>Finds the source Resource Server represented by a management Client.</p>
     *
     * @param clientId 管理 Client 标识；management Client identifier
     * @return Resource Server；Resource Server
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findByManagementClientId(String clientId) {
        return resources.findByManagementClientId(clientId)
                .map(this::toDomain);
    }

    /**
     * 查询精确的 Client Resource Grant。
     *
     * <p>Finds an exact Client Resource Grant.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param grantType Grant 类型；Grant type
     * @param tenantId 服务租户，USER Grant 为空；service tenant, null for USER grant
     * @return Client Resource Grant；Client Resource Grant
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ClientResourceGrant> findGrant(
            String clientId,
            String resourceServerId,
            ResourceGrantType grantType,
            String tenantId
    ) {
        return grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                        clientId,
                        resourceServerId,
                        IdentityClientResourceGrantEntity.GrantType.valueOf(
                                Objects.requireNonNull(grantType, "grantType").name()
                        ),
                        tenantId
                )
                .map(this::toDomain);
    }

    /**
     * 将 Resource Server 持久化对象映射为领域对象。
     *
     * <p>Maps a Resource Server persistence object to the domain object.</p>
     *
     * @param entity Resource Server 持久化对象；Resource Server persistence object
     * @return Resource Server 领域对象；Resource Server domain object
     */
    private ResourceServer toDomain(IdentityResourceServerEntity entity) {
        return new ResourceServer(
                entity.getResourceServerId(),
                URI.create(entity.getResourceUri()),
                entity.getBizCode(),
                entity.getAppCode(),
                entity.getEnvironment(),
                entity.getManagementClientId(),
                entity.getRbacApplicationCode(),
                entity.getEntryPermissionCode(),
                Duration.ofMinutes(5),
                ResourceServerStatus.valueOf(entity.getStatus().name()),
                entity.getVersion()
        );
    }

    /**
     * 将 Client Grant 持久化对象映射为领域对象。
     *
     * <p>Maps a Client Grant persistence object to the domain object.</p>
     *
     * @param entity Client Grant 持久化对象；Client Grant persistence object
     * @return Client Resource Grant 领域对象；Client Resource Grant domain object
     */
    private ClientResourceGrant toDomain(
            IdentityClientResourceGrantEntity entity
    ) {
        return new ClientResourceGrant(
                entity.getClientId(),
                entity.getResourceServerId(),
                ResourceGrantType.valueOf(entity.getGrantType().name()),
                entity.getTenantId(),
                scopes(entity),
                ClientResourceGrant.Status.valueOf(entity.getStatus().name()),
                entity.getVersion()
        );
    }

    /**
     * 解析 Grant 中保存的 Scope JSON；USER Grant 始终返回空集合。
     *
     * <p>Parses the scope JSON stored in a grant; USER grants always return an empty set.</p>
     *
     * @param entity Client Grant 持久化对象；Client Grant persistence object
     * @return 已解析 Scope；parsed scopes
     */
    private Set<String> scopes(IdentityClientResourceGrantEntity entity) {
        if (entity.getGrantType()
                == IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION) {
            return Set.of();
        }
        try {
            return objectMapper.readValue(
                    entity.getAllowedScopes(),
                    new TypeReference<Set<String>>() {
                    }
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Client Resource Grant contains invalid allowedScopes",
                    exception
            );
        }
    }
}
