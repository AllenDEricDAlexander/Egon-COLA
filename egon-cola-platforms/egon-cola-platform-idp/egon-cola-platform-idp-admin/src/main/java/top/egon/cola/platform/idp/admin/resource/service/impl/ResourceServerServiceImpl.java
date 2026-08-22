package top.egon.cola.platform.idp.admin.resource.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService.ResourceProjection;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.idp.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resource Server 管理用例实现。
 *
 * <p>Resource Server management use-case implementation.</p>
 *
 * <p>该应用服务直接编排 JPA 聚合与 Redis 投影。Core 的 Specification 风格策略负责业务
 * 事实校验；这里不增加额外 Strategy/Factory 层，因为管理动作没有可替换算法，直接编排更清晰。</p>
 *
 * <p>This application service directly orchestrates JPA aggregates and Redis projections. Core
 * Specification-style policies validate business facts. No additional Strategy or Factory layer
 * is introduced because these management actions have no interchangeable algorithm.</p>
 */
@Service
public class ResourceServerServiceImpl implements ResourceServerService {

    /** Resource Server 仓储；Resource Server repository. */
    private final IdentityResourceServerRepository resources;

    /** Client Resource Grant 仓储；Client Resource Grant repository. */
    private final IdentityClientResourceGrantRepository grants;

    /** OAuth Client 仓储；OAuth Client repository. */
    private final IdentityClientRepository clients;

    /** Redis 运行态投影服务；Redis runtime projection service. */
    private final ResourceServerProjectionService projections;

    /** 全局标识生成器；global identifier generator. */
    private final LongIdGenerator ids;

    /** JSON 编解码器；JSON codec. */
    private final ObjectMapper objectMapper;

    /** 业务时钟；business clock. */
    private final Clock clock;

    /** Resource Server 生命周期事务事件适配器；Resource Server lifecycle event adapter. */
    private final TransactionalOutboxResourceServerEventAdapter events;

    /**
     * 创建生产管理服务。
     *
     * <p>Creates the production management service.</p>
     */
    @Autowired
    public ResourceServerServiceImpl(
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            IdentityClientRepository clients,
            ResourceServerProjectionService projections,
            LongIdGenerator ids,
            ObjectMapper objectMapper,
            TransactionalOutboxResourceServerEventAdapter events
    ) {
        this(
                resources,
                grants,
                clients,
                projections,
                ids,
                objectMapper,
                Clock.systemUTC(),
                events
        );
    }

    /**
     * 创建可注入时钟的管理服务。
     *
     * <p>Creates a management service with an injectable clock.</p>
     */
    ResourceServerServiceImpl(
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            IdentityClientRepository clients,
            ResourceServerProjectionService projections,
            LongIdGenerator ids,
            ObjectMapper objectMapper,
            Clock clock,
            TransactionalOutboxResourceServerEventAdapter events
    ) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
        this.clients = Objects.requireNonNull(clients, "clients");
        this.projections = Objects.requireNonNull(
                projections,
                "projections"
        );
        this.ids = Objects.requireNonNull(ids, "ids");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.events = Objects.requireNonNull(events, "events");
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ResourceServerVO> list() {
        return resources.findAll().stream()
                .sorted(Comparator.comparing(
                        IdentityResourceServerEntity::getResourceServerId
                ))
                .map(this::view)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ResourceServerVO detail(String resourceServerId) {
        return view(resource(resourceServerId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ResourceServerVO create(CreateResourceServerDTO command) {
        Objects.requireNonNull(command, "command");
        ensureResourceIsUnique(command);
        IdentityClientEntity client = client(command.managementClientId());
        if (resources.findByManagementClientId(client.getClientId())
                .isPresent()) {
            throw new IllegalStateException(
                    "management Client is already bound to a Resource Server"
            );
        }
        Instant now = clock.instant();
        IdentityResourceServerEntity resource = resources.save(
                IdentityResourceServerEntity.create(
                        ids.nextId(),
                        command.resourceServerId(),
                        command.resourceUri(),
                        command.bizCode(),
                        command.appCode(),
                        command.environment(),
                        command.displayName(),
                        command.managementClientId(),
                        command.rbacApplicationCode(),
                        command.entryPermissionCode(),
                        IdentityResourceServerEntity.Status.DISABLED,
                        now
                )
        );
        projections.projectResource(resource, client);
        return view(resource);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ResourceServerVO enable(
            String resourceServerId,
            ResourceVersionDTO command
    ) {
        Objects.requireNonNull(command, "command");
        IdentityResourceServerEntity resource = mutableResource(
                resourceServerId
        );
        resource.enable(command.expectedVersion(), clock.instant());
        return saveAndProject(resource);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ResourceServerVO disable(
            String resourceServerId,
            ResourceVersionDTO command
    ) {
        Objects.requireNonNull(command, "command");
        IdentityResourceServerEntity resource = mutableResource(
                resourceServerId
        );
        resource.disable(command.expectedVersion(), clock.instant());
        ResourceServerVO result = saveAndProject(resource);
        events.enqueueDisabled(resource);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ClientResourceGrantVO putGrant(
            String clientId,
            String resourceServerId,
            UpsertClientResourceGrantDTO command
    ) {
        Objects.requireNonNull(command, "command");
        String exactClientId = exact(clientId, "clientId");
        if (!clients.existsById(exactClientId)) {
            throw new NoSuchElementException("OAuth Client was not found");
        }
        IdentityResourceServerEntity resource = mutableResource(
                resourceServerId
        );
        IdentityClientResourceGrantEntity grant = upsertGrant(
                exactClientId,
                resource,
                command
        );
        if (isServiceGrant(grant)) {
            projections.projectServiceGrant(grant);
        }
        return grantView(grant);
    }

    /**
     * 持久化一个已定位 Resource 上的 Client Grant。
     *
     * <p>Persists one Client grant for an already resolved Resource.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resource 目标 Resource；target Resource
     * @param command 授权命令；grant command
     * @return 已保存 Grant；saved Grant
     */
    private IdentityClientResourceGrantEntity upsertGrant(
            String clientId,
            IdentityResourceServerEntity resource,
            UpsertClientResourceGrantDTO command
    ) {
        GrantFacts facts = grantFacts(
                clientId,
                resource.getResourceServerId(),
                command.grantType(),
                command.tenantId(),
                command.allowedScopes()
        );
        resource.assertVersion(command.expectedResourceVersion());
        IdentityClientResourceGrantEntity grant = grants
                .findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                        clientId,
                        resource.getResourceServerId(),
                        facts.grantType(),
                        facts.tenantId()
                ).map(existing -> updateGrant(
                        existing,
                        facts,
                        command.expectedGrantVersion()
                )).orElseGet(() -> createGrant(
                        clientId,
                        resource.getResourceServerId(),
                        facts,
                        command.expectedGrantVersion()
                ));
        return grants.save(grant);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteGrant(
            String clientId,
            String resourceServerId,
            DeleteClientResourceGrantDTO command
    ) {
        Objects.requireNonNull(command, "command");
        IdentityResourceServerEntity resource = mutableResource(
                resourceServerId
        );
        IdentityClientResourceGrantEntity grant = removeGrant(
                exact(clientId, "clientId"),
                resource,
                command
        );
        if (isServiceGrant(grant)) {
            projections.deleteServiceGrant(grant);
        }
    }

    /**
     * 删除一个已定位 Resource 上的 Client Grant。
     *
     * <p>Deletes one Client grant for an already resolved Resource.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resource 目标 Resource；target Resource
     * @param command 删除命令；delete command
     * @return 已删除 Grant；deleted Grant
     */
    private IdentityClientResourceGrantEntity removeGrant(
            String clientId,
            IdentityResourceServerEntity resource,
            DeleteClientResourceGrantDTO command
    ) {
        resource.assertVersion(command.expectedResourceVersion());
        IdentityClientResourceGrantEntity.GrantType grantType = grantType(
                command.grantType()
        );
        String tenantId = tenant(command.grantType(), command.tenantId());
        IdentityClientResourceGrantEntity grant = grants
                .findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                        clientId,
                        resource.getResourceServerId(),
                        grantType,
                        tenantId
                ).orElseThrow(() -> new NoSuchElementException(
                        "Client Resource Grant was not found"
                ));
        grant.requireVersion(command.expectedGrantVersion());
        grants.delete(grant);
        return grant;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<ResourceServerVO> batch(
            BatchResourceServerActionDTO command
    ) {
        Objects.requireNonNull(command, "command");
        List<String> appCodes = explicitApps(
                command.appCodes(),
                command.expectedVersions()
        );
        List<IdentityResourceServerEntity> selected = selectResources(
                command.bizCode(),
                command.environment(),
                appCodes
        );
        for (IdentityResourceServerEntity resource : selected) {
            long expected = expected(
                    command.expectedVersions(),
                    resource.getAppCode()
            );
            if (command.action()
                    == BatchResourceServerActionDTO.Action.ENABLE) {
                resource.enable(expected, clock.instant());
            } else {
                resource.disable(expected, clock.instant());
            }
        }
        resources.saveAll(selected);
        projections.projectResources(selected.stream()
                .map(resource -> new ResourceProjection(
                        resource,
                        client(resource.getManagementClientId())
                ))
                .toList());
        if (command.action()
                == BatchResourceServerActionDTO.Action.DISABLE) {
            selected.forEach(events::enqueueDisabled);
        }
        return selected.stream().map(this::view).toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<ClientResourceGrantVO> batchGrants(
            String clientId,
            BatchClientResourceGrantDTO command
    ) {
        Objects.requireNonNull(command, "command");
        String exactClientId = exact(clientId, "clientId");
        if (!clients.existsById(exactClientId)) {
            throw new NoSuchElementException("OAuth Client was not found");
        }
        List<String> appCodes = explicitApps(
                command.appCodes(),
                command.expectedResourceVersions()
        );
        validateGrantVersions(
                appCodes,
                command.expectedGrantVersions(),
                command.action()
        );
        List<IdentityResourceServerEntity> selected = selectResources(
                command.bizCode(),
                command.environment(),
                appCodes
        );
        List<ClientResourceGrantVO> results = new ArrayList<>();
        List<IdentityClientResourceGrantEntity> serviceGrants =
                new ArrayList<>();
        for (IdentityResourceServerEntity resource : selected) {
            Long grantVersion = command.expectedGrantVersions().get(
                    resource.getAppCode()
            );
            if (command.action()
                    == BatchClientResourceGrantDTO.Action.UPSERT) {
                IdentityClientResourceGrantEntity grant = upsertGrant(
                        exactClientId,
                        resource,
                        new UpsertClientResourceGrantDTO(
                                command.grantType(),
                                command.tenantId(),
                                command.allowedScopes(),
                                expected(
                                        command.expectedResourceVersions(),
                                        resource.getAppCode()
                                ),
                                grantVersion
                        )
                );
                results.add(grantView(grant));
                if (isServiceGrant(grant)) {
                    serviceGrants.add(grant);
                }
            } else {
                if (grantVersion == null) {
                    throw new IllegalArgumentException(
                            "expected Grant version is required for delete"
                    );
                }
                IdentityClientResourceGrantEntity grant = removeGrant(
                        exactClientId,
                        resource,
                        new DeleteClientResourceGrantDTO(
                                command.grantType(),
                                command.tenantId(),
                                expected(
                                        command.expectedResourceVersions(),
                                        resource.getAppCode()
                                ),
                                grantVersion
                        )
                );
                if (isServiceGrant(grant)) {
                    serviceGrants.add(grant);
                }
            }
        }
        if (!serviceGrants.isEmpty()) {
            if (command.action()
                    == BatchClientResourceGrantDTO.Action.UPSERT) {
                projections.projectServiceGrants(serviceGrants);
            } else {
                projections.deleteServiceGrants(serviceGrants);
            }
        }
        return List.copyOf(results);
    }

    /**
     * 校验 Grant 版本只引用明确应用，删除动作必须覆盖全部应用。
     *
     * <p>Validates that Grant versions reference only explicit applications
     * and that delete operations cover every application.</p>
     *
     * @param appCodes 明确应用；explicit applications
     * @param versions Grant 版本映射；Grant version map
     * @param action 批量动作；batch action
     */
    private void validateGrantVersions(
            List<String> appCodes,
            Map<String, Long> versions,
            BatchClientResourceGrantDTO.Action action
    ) {
        Objects.requireNonNull(versions, "expectedGrantVersions");
        Objects.requireNonNull(action, "action");
        Set<String> exactApps = Set.copyOf(appCodes);
        if (!exactApps.containsAll(versions.keySet())
                || action == BatchClientResourceGrantDTO.Action.DELETE
                && !versions.keySet().equals(exactApps)
                || versions.values().stream().anyMatch(
                        version -> version == null || version < 0L
                )) {
            throw new IllegalArgumentException(
                    "batch Grant versions must match explicit applications"
            );
        }
    }

    /**
     * 判断 Grant 是否属于服务凭证链路。
     *
     * <p>Determines whether a Grant belongs to the service-credential flow.</p>
     *
     * @param grant Grant；Grant
     * @return 是服务 Grant 时为 true；true for a service Grant
     */
    private boolean isServiceGrant(
            IdentityClientResourceGrantEntity grant
    ) {
        return grant.getGrantType()
                == IdentityClientResourceGrantEntity.GrantType
                .CLIENT_CREDENTIALS;
    }

    /**
     * 保存单个 Resource 并同步投影。
     *
     * @param resource Resource Server；Resource Server
     * @return 管理视图；administration view
     */
    private ResourceServerVO saveAndProject(
            IdentityResourceServerEntity resource
    ) {
        resources.save(resource);
        projections.projectResource(
                resource,
                client(resource.getManagementClientId())
        );
        return view(resource);
    }

    /**
     * 校验 Resource 标识、URI、三元组和管理 Client 均未被占用。
     *
     * @param command 创建命令；create command
     */
    private void ensureResourceIsUnique(CreateResourceServerDTO command) {
        if (resources.findByResourceServerId(command.resourceServerId())
                .isPresent()
                || resources.findByResourceUri(command.resourceUri())
                .isPresent()
                || resources.findByBizCodeAndAppCodeAndEnvironment(
                        command.bizCode(),
                        command.appCode(),
                        command.environment()
                ).isPresent()) {
            throw new IllegalStateException("Resource Server already exists");
        }
    }

    /**
     * 以乐观版本更新已有 Grant。
     *
     * @param existing 已有 Grant；existing Grant
     * @param facts 规范化事实；normalized facts
     * @param expectedVersion 期望版本；expected version
     * @return 已更新 Grant；updated Grant
     */
    private IdentityClientResourceGrantEntity updateGrant(
            IdentityClientResourceGrantEntity existing,
            GrantFacts facts,
            Long expectedVersion
    ) {
        if (expectedVersion == null) {
            throw new IllegalStateException(
                    "expected Grant version is required for update"
            );
        }
        existing.update(
                facts.grantType(),
                facts.tenantId(),
                facts.allowedScopesJson(),
                expectedVersion,
                clock.instant()
        );
        return existing;
    }

    /**
     * 创建新 Grant 并拒绝错误的新建版本条件。
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource 标识；Resource identifier
     * @param facts 规范化事实；normalized facts
     * @param expectedVersion 期望已有版本；expected existing version
     * @return 新 Grant；new Grant
     */
    private IdentityClientResourceGrantEntity createGrant(
            String clientId,
            String resourceServerId,
            GrantFacts facts,
            Long expectedVersion
    ) {
        if (expectedVersion != null) {
            throw new IllegalStateException(
                    "new Grant must not provide an expected Grant version"
            );
        }
        if (facts.grantType()
                == IdentityClientResourceGrantEntity.GrantType
                .USER_DELEGATION) {
            return IdentityClientResourceGrantEntity.userDelegation(
                    ids.nextId(),
                    clientId,
                    resourceServerId,
                    clock.instant()
            );
        }
        return IdentityClientResourceGrantEntity.clientCredentials(
                ids.nextId(),
                clientId,
                resourceServerId,
                facts.tenantId(),
                facts.allowedScopesJson(),
                clock.instant()
        );
    }

    /**
     * 使用 Core 领域约束校验并规范化 Grant 输入。
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource 标识；Resource identifier
     * @param type Grant 类型；Grant type
     * @param tenantId 租户；tenant
     * @param allowedScopes 允许 Scope；allowed scopes
     * @return 规范化事实；normalized facts
     */
    private GrantFacts grantFacts(
            String clientId,
            String resourceServerId,
            ResourceGrantType type,
            String tenantId,
            Set<String> allowedScopes
    ) {
        Objects.requireNonNull(type, "grantType");
        Objects.requireNonNull(allowedScopes, "allowedScopes");
        Set<String> scopes = Collections.unmodifiableSet(
                new TreeSet<>(allowedScopes)
        );
        String normalizedTenant = tenant(type, tenantId);
        new ClientResourceGrant(
                clientId,
                resourceServerId,
                type,
                normalizedTenant,
                scopes,
                ClientResourceGrant.Status.ACTIVE,
                0L
        );
        return new GrantFacts(
                grantType(type),
                normalizedTenant,
                scopeJson(scopes)
        );
    }

    /**
     * 将 Core Grant 类型映射为持久化枚举。
     *
     * @param type Core Grant 类型；Core Grant type
     * @return 持久化 Grant 类型；persisted Grant type
     */
    private IdentityClientResourceGrantEntity.GrantType grantType(
            ResourceGrantType type
    ) {
        return IdentityClientResourceGrantEntity.GrantType.valueOf(
                Objects.requireNonNull(type, "grantType").name()
        );
    }

    /**
     * 按 Grant 类型规范化租户绑定。
     *
     * @param type Grant 类型；Grant type
     * @param tenantId 原始租户；raw tenant
     * @return 规范化租户；normalized tenant
     */
    private String tenant(ResourceGrantType type, String tenantId) {
        if (type == ResourceGrantType.USER_DELEGATION) {
            if (tenantId != null && !tenantId.isBlank()) {
                throw new IllegalArgumentException(
                        "USER_DELEGATION must not contain tenant"
                );
            }
            return null;
        }
        return exact(tenantId, "tenantId");
    }

    /**
     * 将 Scope 集合序列化为持久化 JSON。
     *
     * @param scopes Scope 集合；scope set
     * @return JSON 文本；JSON text
     */
    private String scopeJson(Set<String> scopes) {
        try {
            return objectMapper.writeValueAsString(scopes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Grant scope serialization failed",
                    exception
            );
        }
    }

    /**
     * 读取持久化 Scope JSON。
     *
     * @param json JSON 文本；JSON text
     * @return Scope 集合；scope set
     */
    private Set<String> scopes(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<LinkedHashSet<String>>() { }
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Persisted Grant scopes are invalid",
                    exception
            );
        }
    }

    /**
     * 校验批量请求仅包含明确应用且版本映射完全匹配。
     *
     * @param appCodes 应用列表；application list
     * @param expectedVersions 期望版本；expected versions
     * @return 规范化明确应用；normalized explicit applications
     */
    private List<String> explicitApps(
            List<String> appCodes,
            Map<String, Long> expectedVersions
    ) {
        Objects.requireNonNull(appCodes, "appCodes");
        Objects.requireNonNull(expectedVersions, "expectedVersions");
        List<String> exactApps = appCodes.stream()
                .map(app -> exact(app, "appCode"))
                .toList();
        if (exactApps.isEmpty()
                || new HashSet<>(exactApps).size() != exactApps.size()
                || exactApps.stream().anyMatch("*"::equals)
                || !expectedVersions.keySet().equals(Set.copyOf(exactApps))) {
            throw new IllegalArgumentException(
                    "batch appCodes and expected versions must match exactly"
            );
        }
        expectedVersions.values().forEach(version -> {
            if (version == null || version < 0L) {
                throw new IllegalArgumentException(
                        "expected Resource version is invalid"
                );
            }
        });
        return List.copyOf(exactApps);
    }

    /**
     * 以三元组范围锁定批量明确选中的 Resource。
     *
     * @param bizCode 业务域；business domain
     * @param environment 环境；environment
     * @param appCodes 明确应用；explicit applications
     * @return 完整 Resource 集合；complete Resource set
     */
    private List<IdentityResourceServerEntity> selectResources(
            String bizCode,
            String environment,
            List<String> appCodes
    ) {
        List<IdentityResourceServerEntity> selected = resources
                .findByBizCodeAndEnvironmentAndAppCodeIn(
                        exact(bizCode, "bizCode"),
                        exact(environment, "environment"),
                        appCodes
                );
        Set<String> found = selected.stream()
                .map(IdentityResourceServerEntity::getAppCode)
                .collect(java.util.stream.Collectors.toSet());
        if (!found.equals(Set.copyOf(appCodes))) {
            throw new NoSuchElementException(
                    "one or more explicitly selected applications were not found"
            );
        }
        return selected;
    }

    /**
     * 读取一个应用的必填期望版本。
     *
     * @param versions 版本映射；version map
     * @param appCode 应用编码；application code
     * @return 期望版本；expected version
     */
    private long expected(Map<String, Long> versions, String appCode) {
        Long version = versions.get(appCode);
        if (version == null || version < 0L) {
            throw new IllegalArgumentException(
                    "expected Resource version is required"
            );
        }
        return version;
    }

    /**
     * 只读加载 Resource Server。
     *
     * @param resourceServerId Resource 标识；Resource identifier
     * @return Resource Server；Resource Server
     */
    private IdentityResourceServerEntity resource(String resourceServerId) {
        return resources.findByResourceServerId(
                        exact(resourceServerId, "resourceServerId")
                ).orElseThrow(() -> new NoSuchElementException(
                        "Resource Server was not found"
                ));
    }

    /**
     * 使用写锁加载待变更 Resource Server。
     *
     * @param resourceServerId Resource 标识；Resource identifier
     * @return 可变更 Resource Server；mutable Resource Server
     */
    private IdentityResourceServerEntity mutableResource(
            String resourceServerId
    ) {
        String exactId = exact(resourceServerId, "resourceServerId");
        return resources.findByResourceServerIdForUpdate(exactId)
                .or(() -> resources.findByResourceServerId(exactId))
                .orElseThrow(() -> new NoSuchElementException(
                        "Resource Server was not found"
                ));
    }

    /**
     * 加载 OAuth Client。
     *
     * @param clientId Client 标识；Client identifier
     * @return OAuth Client；OAuth Client
     */
    private IdentityClientEntity client(String clientId) {
        return clients.findById(exact(clientId, "clientId"))
                .orElseThrow(() -> new NoSuchElementException(
                        "OAuth Client was not found"
                ));
    }

    /**
     * 构造包含已加载密钥的 Resource 管理视图。
     *
     * @param resource Resource Server；Resource Server
     * @return 管理视图；administration view
     */
    private ResourceServerVO view(IdentityResourceServerEntity resource) {
        return new ResourceServerVO(
                resource.getResourceServerId(),
                resource.getResourceUri(),
                resource.getBizCode(),
                resource.getAppCode(),
                resource.getEnvironment(),
                resource.getDisplayName(),
                resource.getManagementClientId(),
                resource.getRbacApplicationCode(),
                resource.getEntryPermissionCode(),
                resource.getStatus().name(),
                resource.getVersion(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }

    /**
     * 构造 Grant 管理视图。
     *
     * @param grant Grant 实体；Grant entity
     * @return Grant 视图；Grant view
     */
    private ClientResourceGrantVO grantView(
            IdentityClientResourceGrantEntity grant
    ) {
        return new ClientResourceGrantVO(
                grant.getClientId(),
                grant.getResourceServerId(),
                grant.getGrantType().name(),
                grant.getTenantId(),
                scopes(grant.getAllowedScopes()),
                grant.getStatus().name(),
                grant.getVersion()
        );
    }

    /**
     * 校验精确、非空且未带首尾空白的标识。
     *
     * @param value 值；value
     * @param field 字段名；field name
     * @return 原始精确值；exact original value
     */
    private String exact(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 已由 Core 策略校验并规范化的 Grant 事实。
     *
     * <p>Grant facts validated and normalized by the Core policy.</p>
     *
     * @param grantType 持久化授权类型；persisted grant type
     * @param tenantId 规范化租户；normalized tenant
     * @param allowedScopesJson 规范化 Scope JSON；normalized scope JSON
     */
    private record GrantFacts(
            IdentityClientResourceGrantEntity.GrantType grantType,
            String tenantId,
            String allowedScopesJson
    ) {
    }
}
