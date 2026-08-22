package top.egon.cola.platform.idp.admin.support.bootstrap;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 幂等注册本地拓扑使用的 OAuth Client、Resource Server 和应用级授权。
 *
 * <p>Idempotently registers OAuth Clients, Resource Servers, and application-level grants used
 * by the explicit local topology.</p>
 */
@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.idp.development-bootstrap",
        name = "enabled",
        havingValue = "true")
public class IdpDevelopmentClientBootstrap
        implements SmartInitializingSingleton {

    /** 开发 Client 的 Access Token 有效秒数；development access-token lifetime in seconds. */
    private static final int ACCESS_TOKEN_TTL_SECONDS = 900;

    /** 开发 Client 的 Refresh Token 有效秒数；development refresh-token lifetime in seconds. */
    private static final int REFRESH_TOKEN_TTL_SECONDS = 604_800;

    /** 开发环境中需要幂等创建的 Public Client；public Clients created idempotently for local use. */
    private static final List<ClientSpec> CLIENTS = List.of(
            new ClientSpec("idp-admin-web", "IdP Admin Web", 18121),
            new ClientSpec("rbac3-admin-web", "RBAC3 Admin Web", 18131),
            new ClientSpec("gateway-admin-web", "Gateway Admin Web", 18141),
            new ClientSpec(
                    "ddc-admin-web",
                    "DDC Admin Web",
                    18152,
                    List.of("http://127.0.0.1:18151/oauth/callback")
            ),
            new ClientSpec("mock-backend", "Unified Identity Mock Backend", 18161));

    /** 开发环境中需要幂等创建的机器 Client；machine Clients created idempotently for local use. */
    private static final List<MachineClientSpec> MACHINE_CLIENTS = List.of(
            new MachineClientSpec("idp-service", "IdP Local Service"),
            new MachineClientSpec("rbac3-service", "RBAC3 Local Service"),
            new MachineClientSpec("ddc-service", "DDC Local Service"),
            new MachineClientSpec(
                    "gateway-admin-service",
                    "Gateway Admin Local Service"
            ),
            new MachineClientSpec(
                    "gateway-engine-service",
                    "Gateway Engine Local Service"
            ),
            new MachineClientSpec(
                    "mock-backend-service",
                    "Mock Backend Local Service"
            ),
            new MachineClientSpec(
                    "mcp-provider-service",
                    "MCP Provider Local Service"
            )
    );

    /** 开发环境明确审批的应用级 Resource Server；explicitly approved local Resource Servers. */
    private static final List<ResourceSpec> RESOURCES = List.of(
            new ResourceSpec(
                    "permission-idp-local",
                    "https://api.egon.internal/local/permission/idp",
                    "permission",
                    "idp",
                    "IdP Local",
                    "idp-service",
                    "idp-admin",
                    "idp:identity:self:read",
                    "idp-admin-web"
            ),
            new ResourceSpec(
                    "permission-rbac3-local",
                    "https://api.egon.internal/local/permission/rbac3",
                    "permission",
                    "rbac3",
                    "RBAC3 Local",
                    "rbac3-service",
                    "rbac3-admin",
                    "system:tenant:read",
                    "rbac3-admin-web"
            ),
            new ResourceSpec(
                    "platform-ddc-local",
                    "https://api.egon.internal/local/platform/ddc",
                    "platform",
                    "ddc",
                    "DDC Local",
                    "ddc-service",
                    "ddc-admin",
                    "DDC_READ",
                    "ddc-admin-web"
            ),
            new ResourceSpec(
                    "platform-gateway-admin-local",
                    "https://api.egon.internal/local/platform/gateway-admin",
                    "platform",
                    "gateway-admin",
                    "Gateway Admin Local",
                    "gateway-admin-service",
                    "gateway-admin",
                    "gateway:read",
                    "gateway-admin-web"
            ),
            new ResourceSpec(
                    "identity-gateway-engine-default-local",
                    "https://api.egon.internal/local/identity/gateway-engine-default",
                    "identity",
                    "gateway-engine-default",
                    "Gateway Engine Local",
                    "gateway-engine-service",
                    "mock-backend",
                    "mock:read",
                    null
            ),
            new ResourceSpec(
                    "identity-mock-backend-local",
                    "https://api.egon.internal/local/identity/mock-backend",
                    "identity",
                    "mock-backend",
                    "Mock Backend Local",
                    "mock-backend-service",
                    "mock-backend",
                    "mock:read",
                    "mock-backend"
            ),
            new ResourceSpec(
                    "identity-gateway-test-mcp-provider-local",
                    "https://api.egon.internal/local/identity/gateway-test-mcp-provider",
                    "identity",
                    "gateway-test-mcp-provider",
                    "Gateway MCP Provider Local",
                    "mcp-provider-service",
                    "mock-backend",
                    "mock:read",
                    null
            )
    );

    /** 需要访问 RBAC3 USER 决策接口的本地服务 Client；local service Clients calling RBAC3 USER decisions. */
    private static final List<String> RBAC3_SERVICE_CLIENTS = List.of(
            "idp-service",
            "rbac3-service",
            "ddc-service",
            "gateway-admin-service",
            "gateway-engine-service",
            "mock-backend-service",
            "mcp-provider-service"
    );

    /** RBAC3 内部 USER 决策接口所需 Scope；scopes required by RBAC3 internal USER-decision APIs. */
    private static final Set<String> RBAC3_SERVICE_SCOPES = Set.of(
            "service:authorization:decide",
            "service:authorization:snapshot",
            "service:identity:resolve"
    );

    /**
     * Gateway Admin 控制面 Service Token 所需 Scope；scopes required by the Gateway Admin
     * control-plane Service Token.
     */
    private static final Set<String> GATEWAY_ADMIN_SERVICE_SCOPES = Set.of(
            "gateway:read",
            "gateway:applications:write",
            "gateway:catalog:write",
            "gateway:credentials:write",
            "gateway:drafts:write",
            "gateway:groups:write",
            "gateway:mcp:approve",
            "gateway:mcp:read",
            "gateway:mcp:runtime:read",
            "gateway:mcp:test",
            "gateway:mcp:write",
            "gateway:releases:write"
    );

    /** MCP Task Worker 的 Source Client；source Client used by the MCP task worker. */
    private static final String MCP_TASK_SERVICE_CLIENT =
            "gateway-engine-service";

    /** MCP Provider 的目标 Resource；target Resource exposed by the MCP Provider. */
    private static final String MCP_TASK_RESOURCE_SERVER =
            "identity-gateway-test-mcp-provider-local";

    /** MCP Task Worker 调用 Provider 所需 Scope；scope required to invoke the MCP Provider. */
    private static final Set<String> MCP_TASK_SERVICE_SCOPES =
            Set.of("mcp:operation:invoke");

    /** OAuth Client 管理服务；OAuth Client management service. */
    private final OAuthClientService clients;

    /** Resource Server 仓储；Resource Server repository. */
    private final IdentityResourceServerRepository resources;

    /** Client Resource Grant 仓储；Client Resource Grant repository. */
    private final IdentityClientResourceGrantRepository grants;

    /** OAuth Client 主记录仓储；OAuth Client master-record repository. */
    private final IdentityClientRepository clientEntities;

    /** Resource 运行态投影服务；Resource runtime projection service. */
    private final ResourceServerProjectionService projections;

    /** RBAC3 本地服务授权绑定的精确租户集合；exact tenants bound to local RBAC3 service grants. */
    private final Set<String> rbac3ServiceTenantIds;

    /**
     * 创建开发拓扑初始化器。
     *
     * <p>Creates the development-topology bootstrap.</p>
     *
     * @param clients OAuth Client 管理服务；OAuth Client management service
     * @param resources Resource Server 仓储；Resource Server repository
     * @param grants Client Resource Grant 仓储；Client Resource Grant repository
     */
    @Autowired
    public IdpDevelopmentClientBootstrap(
            OAuthClientService clients,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            IdentityClientRepository clientEntities,
            ResourceServerProjectionService projections,
            @Value("${egon.idp.development-bootstrap.rbac3-service-tenant-ids:default}")
            String rbac3ServiceTenantIds
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.resources = Objects.requireNonNull(resources, "resources");
        this.grants = Objects.requireNonNull(grants, "grants");
        this.clientEntities = Objects.requireNonNull(
                clientEntities,
                "clientEntities"
        );
        this.projections = Objects.requireNonNull(
                projections,
                "projections"
        );
        this.rbac3ServiceTenantIds = tenantIds(rbac3ServiceTenantIds);
    }

    IdpDevelopmentClientBootstrap(
            OAuthClientService clients,
            IdentityResourceServerRepository resources,
            IdentityClientResourceGrantRepository grants,
            IdentityClientRepository clientEntities,
            ResourceServerProjectionService projections
    ) {
        this(
                clients,
                resources,
                grants,
                clientEntities,
                projections,
                "default"
        );
    }

    /**
     * 在 DDC 生命周期启动前，幂等对齐本地 Client、Resource 与显式 Grant。
     *
     * <p>Idempotently reconciles local Clients, Resources, and explicit grants before the
     * DDC lifecycle starts.</p>
     */
    @Override
    public void afterSingletonsInstantiated() {
        Map<String, OAuthClientVO> existing =
                clients.list().stream().collect(Collectors.toUnmodifiableMap(
                        OAuthClientVO::clientId,
                        client -> client
                ));
        CLIENTS.forEach(client -> reconcile(client, existing.get(
                client.clientId()
        )));
        MACHINE_CLIENTS.forEach(client -> reconcile(
                client,
                existing.get(client.clientId())
        ));
        RESOURCES.forEach(this::reconcileResourceAndGrant);
        reconcileRbac3ServiceGrants();
        reconcileGatewayAdminServiceGrants();
        reconcileMcpTaskServiceGrants();
    }

    /**
     * 对齐一个开发 Client 的创建和回调地址。
     *
     * <p>Reconciles creation and redirect URIs for one development Client.</p>
     *
     * @param client 期望 Client；desired Client
     * @param existing 已有 Client 或空；existing Client or {@code null}
     */
    private void reconcile(
            ClientSpec client,
            OAuthClientVO existing) {
        if (existing == null) {
            create(client);
            return;
        }
        String redirectUri = redirectUri(client);
        if (!existing.redirectUris().contains(redirectUri)) {
            clients.putRedirectUri(client.clientId(), redirectUri);
        }
        client.obsoleteRedirectUris().stream()
                .filter(existing.redirectUris()::contains)
                .forEach(uri -> clients.deleteRedirectUri(
                        client.clientId(),
                        uri
                ));
    }

    /**
     * 创建一个尚不存在的开发 Client。
     *
     * <p>Creates one missing development Client.</p>
     *
     * @param client Client 规格；Client specification
     */
    private void create(ClientSpec client) {
        clients.create(new CreateOAuthClientDTO(
                client.clientId(),
                client.clientName(),
                ACCESS_TOKEN_TTL_SECONDS,
                REFRESH_TOKEN_TTL_SECONDS,
                List.of(redirectUri(client)),
                List.of()));
    }

    /**
     * 对齐一个机器 Confidential Client。
     *
     * <p>Reconciles one machine Confidential Client.</p>
     *
     * @param client 期望机器 Client；desired machine Client
     * @param existing 已有 Client 或空；existing Client or {@code null}
     */
    private void reconcile(
            MachineClientSpec client,
            OAuthClientVO existing
    ) {
        if (existing == null) {
            clients.create(new CreateOAuthClientDTO(
                    client.clientId(),
                    client.clientName(),
                    IdentityClientEntity.ClientType.CONFIDENTIAL,
                    ACCESS_TOKEN_TTL_SECONDS,
                    REFRESH_TOKEN_TTL_SECONDS,
                    List.of(),
                    List.of()
            ));
            return;
        }
        if (!IdentityClientEntity.ClientType.CONFIDENTIAL.name()
                .equals(existing.clientType())) {
            throw new IllegalStateException(
                    "local machine Client type does not match: "
                            + client.clientId()
            );
        }
    }

    /**
     * 生成开发 Client 回调地址。
     *
     * <p>Builds a development Client redirect URI.</p>
     *
     * @param client Client 规格；Client specification
     * @return 回调地址；redirect URI
     */
    private String redirectUri(ClientSpec client) {
        return "http://127.0.0.1:" + client.port() + "/oauth/callback";
    }

    /**
     * 幂等创建一个 Resource Server 和对应 Public Client 授权。
     *
     * <p>Idempotently creates one Resource Server and its Public Client grant.</p>
     *
     * @param spec Resource 规格；Resource specification
     */
    private void reconcileResourceAndGrant(ResourceSpec spec) {
        IdentityResourceServerEntity resource = resources
                .findByResourceServerId(spec.resourceServerId())
                .orElseGet(() -> createResource(spec));
        requireMatchingResource(spec, resource);
        if (spec.userClientId() != null
                && !grants.existsByClientIdAndResourceServerIdAndGrantType(
                spec.userClientId(),
                resource.getResourceServerId(),
                IdentityClientResourceGrantEntity.GrantType.USER_DELEGATION
        )) {
            grants.save(IdentityClientResourceGrantEntity.userDelegation(
                    "dev-user-grant-" + spec.appCode(),
                    spec.userClientId(),
                    resource.getResourceServerId(),
                    Instant.now()
            ));
        }
        projections.projectResource(
                resource,
                clientEntities.findById(spec.managementClientId())
                        .orElseThrow(() -> new IllegalStateException(
                                "local management Client is missing: "
                                        + spec.managementClientId()
                        ))
        );
    }

    /**
     * 创建 ACTIVE 开发 Resource Server。
     *
     * <p>Creates an ACTIVE development Resource Server.</p>
     *
     * @param spec Resource 规格；Resource specification
     * @return 新 Resource Server；new Resource Server
     */
    private IdentityResourceServerEntity createResource(ResourceSpec spec) {
        IdentityResourceServerEntity resource =
                IdentityResourceServerEntity.create(
                        "dev-resource-" + spec.appCode(),
                        spec.resourceServerId(),
                        spec.resourceUri(),
                        spec.bizCode(),
                        spec.appCode(),
                        "local",
                        spec.displayName(),
                        spec.managementClientId(),
                        spec.rbacApplicationCode(),
                        spec.entryPermissionCode(),
                        300,
                        IdentityResourceServerEntity.Status.ACTIVE,
                        Instant.now()
                );
        resources.save(resource);
        return resource;
    }

    /**
     * 校验已有本地 Resource 没有漂移到另一业务三元组或管理 Client。
     *
     * <p>Checks that an existing local Resource has not drifted to another business triple or
     * management Client.</p>
     */
    private void requireMatchingResource(
            ResourceSpec spec,
            IdentityResourceServerEntity resource
    ) {
        if (!spec.resourceUri().equals(resource.getResourceUri())
                || !spec.bizCode().equals(resource.getBizCode())
                || !spec.appCode().equals(resource.getAppCode())
                || !"local".equals(resource.getEnvironment())
                || !spec.managementClientId().equals(
                resource.getManagementClientId()
        )) {
            throw new IllegalStateException(
                    "local Resource Server definition does not match: "
                            + spec.resourceServerId()
            );
        }
    }

    /**
     * 给需要查询 USER 权限的服务显式登记到 RBAC3 的 Service Grant。
     *
     * <p>Explicitly grants services that query USER permissions access to the RBAC3 Resource.</p>
     */
    private void reconcileRbac3ServiceGrants() {
        String target = "permission-rbac3-local";
        String allowedScopes = RBAC3_SERVICE_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        RBAC3_SERVICE_CLIENTS.forEach(clientId -> {
            List<IdentityClientResourceGrantEntity> reusable =
                    new ArrayList<>(grants
                            .findByClientIdAndGrantTypeAndStatus(
                                    clientId,
                                    IdentityClientResourceGrantEntity.GrantType
                                            .CLIENT_CREDENTIALS,
                                    IdentityClientResourceGrantEntity.Status.ACTIVE
                            ).stream()
                            .filter(grant -> target.equals(
                                    grant.getResourceServerId()))
                            .filter(grant -> grant.getId().startsWith(
                                    "dev-rbac3-grant-"))
                            .filter(grant -> !rbac3ServiceTenantIds.contains(
                                    grant.getTenantId()))
                            .toList());
            rbac3ServiceTenantIds.forEach(tenantId -> {
                Optional<IdentityClientResourceGrantEntity> exact =
                        grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                                clientId,
                                target,
                                IdentityClientResourceGrantEntity.GrantType
                                        .CLIENT_CREDENTIALS,
                                tenantId
                        );
                IdentityClientResourceGrantEntity grant;
                boolean existing;
                if (exact.isPresent()) {
                    grant = exact.orElseThrow();
                    reusable.remove(grant);
                    existing = true;
                } else if (!reusable.isEmpty()) {
                    grant = reusable.removeFirst();
                    existing = true;
                } else {
                    grant = IdentityClientResourceGrantEntity.clientCredentials(
                            serviceGrantId(clientId, tenantId),
                            clientId,
                            target,
                            tenantId,
                            allowedScopes,
                            Instant.now()
                    );
                    existing = false;
                }
                if (existing
                        && tenantId.equals(grant.getTenantId())
                        && allowedScopes.equals(grant.getAllowedScopes())
                        && grant.getStatus()
                        == IdentityClientResourceGrantEntity.Status.ACTIVE) {
                    return;
                }
                if (existing) {
                    grant.update(
                            IdentityClientResourceGrantEntity.GrantType
                                    .CLIENT_CREDENTIALS,
                            tenantId,
                            allowedScopes,
                            grant.getVersion(),
                            Instant.now()
                    );
                }
                grants.save(grant);
                projections.projectServiceGrant(grant);
            });
        });
    }

    /**
     * 给 Gateway Admin 控制面 Client 显式登记 IdP 签名的管理 Scope。
     *
     * <p>Explicitly grants the Gateway Admin control-plane Client the IdP-signed management
     * scopes used by the local catalog and route publisher.</p>
     */
    private void reconcileGatewayAdminServiceGrants() {
        String target = "platform-gateway-admin-local";
        String allowedScopes = GATEWAY_ADMIN_SERVICE_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        rbac3ServiceTenantIds.forEach(tenantId -> {
            Optional<IdentityClientResourceGrantEntity> exact =
                    grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                            "gateway-admin-service",
                            target,
                            IdentityClientResourceGrantEntity.GrantType
                                    .CLIENT_CREDENTIALS,
                            tenantId
                    );
            IdentityClientResourceGrantEntity grant = exact.orElseGet(() ->
                    IdentityClientResourceGrantEntity.clientCredentials(
                            gatewayAdminServiceGrantId(tenantId),
                            "gateway-admin-service",
                            target,
                            tenantId,
                            allowedScopes,
                            Instant.now()
                    ));
            if (exact.isPresent()
                    && allowedScopes.equals(grant.getAllowedScopes())
                    && grant.getStatus()
                    == IdentityClientResourceGrantEntity.Status.ACTIVE) {
                return;
            }
            if (exact.isPresent()) {
                grant.update(
                        IdentityClientResourceGrantEntity.GrantType
                                .CLIENT_CREDENTIALS,
                        tenantId,
                        allowedScopes,
                        grant.getVersion(),
                        Instant.now()
                );
            }
            grants.save(grant);
            projections.projectServiceGrant(grant);
        });
    }

    /**
     * 给 Gateway Engine 的异步 MCP Worker 显式登记目标 Provider 的 Service Grant。
     * Explicitly grants the Gateway Engine asynchronous MCP worker access to the target
     * Provider Resource.
     */
    private void reconcileMcpTaskServiceGrants() {
        String allowedScopes = MCP_TASK_SERVICE_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        rbac3ServiceTenantIds.forEach(tenantId -> {
            Optional<IdentityClientResourceGrantEntity> exact =
                    grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                            MCP_TASK_SERVICE_CLIENT,
                            MCP_TASK_RESOURCE_SERVER,
                            IdentityClientResourceGrantEntity.GrantType
                                    .CLIENT_CREDENTIALS,
                            tenantId
                    );
            IdentityClientResourceGrantEntity grant = exact.orElseGet(() ->
                    IdentityClientResourceGrantEntity.clientCredentials(
                            mcpTaskServiceGrantId(tenantId),
                            MCP_TASK_SERVICE_CLIENT,
                            MCP_TASK_RESOURCE_SERVER,
                            tenantId,
                            allowedScopes,
                            Instant.now()
                    ));
            if (exact.isPresent()
                    && allowedScopes.equals(grant.getAllowedScopes())
                    && grant.getStatus()
                    == IdentityClientResourceGrantEntity.Status.ACTIVE) {
                return;
            }
            if (exact.isPresent()) {
                grant.update(
                        IdentityClientResourceGrantEntity.GrantType
                                .CLIENT_CREDENTIALS,
                        tenantId,
                        allowedScopes,
                        grant.getVersion(),
                        Instant.now()
                );
            }
            grants.save(grant);
            projections.projectServiceGrant(grant);
        });
    }

    /**
     * 解析逗号分隔且保持声明顺序的精确租户集合。
     *
     * <p>Parses a comma-delimited exact-tenant set while preserving declaration order.</p>
     *
     * @param value 租户配置；tenant configuration
     * @return 非空且去重的租户集合；non-empty deduplicated tenant set
     */
    private static Set<String> tenantIds(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(
                    "rbac3ServiceTenantIds is required"
            );
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String tenantId : value.split(",", -1)) {
            if (tenantId.isBlank() || !tenantId.equals(tenantId.trim())) {
                throw new IllegalArgumentException(
                        "rbac3ServiceTenantId is required"
                );
            }
            values.add(tenantId);
        }
        return Collections.unmodifiableSet(values);
    }

    /**
     * 为一个 Client 与精确租户生成稳定的开发 Grant 标识。
     *
     * <p>Generates a stable development Grant identifier for one Client and exact tenant.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param tenantId 精确租户；exact tenant
     * @return 长度受控的稳定 Grant 标识；bounded stable Grant identifier
     */
    private static String serviceGrantId(String clientId, String tenantId) {
        String suffix = UUID.nameUUIDFromBytes(
                tenantId.getBytes(StandardCharsets.UTF_8)
        ).toString().substring(0, 8);
        return "dev-rbac3-grant-" + clientId + "-" + suffix;
    }

    /**
     * 为精确租户生成稳定的 MCP Task Service Grant 标识。
     * Generates a stable MCP task Service Grant identifier for an exact tenant.
     */
    private static String mcpTaskServiceGrantId(String tenantId) {
        String suffix = UUID.nameUUIDFromBytes(
                tenantId.getBytes(StandardCharsets.UTF_8)
        ).toString().substring(0, 8);
        return "dev-mcp-task-grant-" + suffix;
    }

    /**
     * 为精确租户生成稳定的 Gateway Admin Service Grant 标识。
     */
    private static String gatewayAdminServiceGrantId(String tenantId) {
        String suffix = UUID.nameUUIDFromBytes(
                tenantId.getBytes(StandardCharsets.UTF_8)
        ).toString().substring(0, 8);
        return "dev-gateway-admin-grant-" + suffix;
    }

    /**
     * 开发 Public Client 规格。
     *
     * <p>Development Public Client specification.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param clientName 展示名称；display name
     * @param port 本地回调端口；local redirect port
     * @param obsoleteRedirectUris 需要删除的旧回调；obsolete redirects to remove
     */
    private record ClientSpec(
            String clientId,
            String clientName,
            int port,
            List<String> obsoleteRedirectUris
    ) {
        /**
         * 创建没有旧回调地址的 Client 规格。
         *
         * <p>Creates a Client specification without obsolete redirects.</p>
         *
         * @param clientId Client 标识；Client identifier
         * @param clientName 展示名称；display name
         * @param port 本地回调端口；local redirect port
         */
        private ClientSpec(String clientId, String clientName, int port) {
            this(clientId, clientName, port, List.of());
        }
    }

    /**
     * 开发机器 Client 规格。
     *
     * <p>Development machine Client specification.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param clientName 展示名称；display name
     */
    private record MachineClientSpec(
            String clientId,
            String clientName
    ) {
    }

    /**
     * 开发 Resource Server 规格。
     *
     * <p>Development Resource Server specification.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param resourceUri Resource URI；Resource URI
     * @param bizCode 业务域编码；business-domain code
     * @param appCode 应用编码；application code
     * @param displayName 展示名称；display name
     * @param managementClientId 管理 Client；management Client
     * @param rbacApplicationCode RBAC3 应用；RBAC3 application
     * @param entryPermissionCode 入口权限；entry permission
     * @param userClientId 获准请求 USER Token 的 Public Client；Public Client allowed to request
     * USER tokens
     */
    private record ResourceSpec(
            String resourceServerId,
            String resourceUri,
            String bizCode,
            String appCode,
            String displayName,
            String managementClientId,
            String rbacApplicationCode,
            String entryPermissionCode,
            String userClientId
    ) {
    }
}
