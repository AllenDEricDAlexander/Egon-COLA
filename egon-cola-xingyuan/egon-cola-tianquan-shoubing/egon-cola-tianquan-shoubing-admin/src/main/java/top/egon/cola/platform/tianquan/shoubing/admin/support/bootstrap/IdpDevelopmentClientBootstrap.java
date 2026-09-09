package top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap;

import org.springframework.beans.factory.SmartInitializingSingleton;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
@Slf4j
@RequiredArgsConstructor
@Component("idpDevelopmentClientBootstrap")
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.tianquan-shoubing.development-bootstrap",
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
            new ClientSpec("tianquan-shoubing-admin-web", "Tianquan-Shoubing Admin Web", 18121),
            new ClientSpec("tianquan-jianshen-admin-web", "Tianquan-Jianshen Admin Web", 18131),
            new ClientSpec("yuheng-admin-web", "Yuheng Admin Web", 18141),
            new ClientSpec(
                    "tianshu-admin-web",
                    "Tianshu Admin Web",
                    18152,
                    List.of("http://127.0.0.1:18151/oauth/callback")
            ),
            new ClientSpec("mock-backend", "Unified Identity Mock Backend", 18161));

    /** 开发环境中需要幂等创建的机器 Client；machine Clients created idempotently for local use. */
    private static final List<MachineClientSpec> MACHINE_CLIENTS = List.of(
            new MachineClientSpec("tianquan-shoubing-service", "Tianquan-Shoubing Local Service"),
            new MachineClientSpec("tianquan-jianshen-service", "Tianquan-Jianshen Local Service"),
            new MachineClientSpec("tianshu-service", "Tianshu Local Service"),
            new MachineClientSpec(
                    "yuheng-admin-service",
                    "Yuheng Admin Local Service"
            ),
            new MachineClientSpec(
                    "yuheng-biz-gateway-service",
                    "Yuheng Engine Local Service"
            ),
            new MachineClientSpec(
                    "yuheng-mcp-gateway-service",
                    "Yuheng MCP Engine Local Service"
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
                    "permission-tianquan-shoubing-local",
                    "https://api.egon.internal/local/permission/tianquan-shoubing",
                    "permission",
                    "tianquan-shoubing",
                    "Tianquan-Shoubing Local",
                    "tianquan-shoubing-service",
                    "tianquan-shoubing-admin",
                    "tianquan-shoubing:identity:self:read",
                    "tianquan-shoubing-admin-web"
            ),
            new ResourceSpec(
                    "permission-tianquan-jianshen-local",
                    "https://api.egon.internal/local/permission/tianquan-jianshen",
                    "permission",
                    "tianquan-jianshen",
                    "Tianquan-Jianshen Local",
                    "tianquan-jianshen-service",
                    "tianquan-jianshen-admin",
                    "system:tenant:read",
                    "tianquan-jianshen-admin-web"
            ),
            new ResourceSpec(
                    "platform-tianshu-local",
                    "https://api.egon.internal/local/platform/tianshu",
                    "xingyuan",
                    "tianshu",
                    "Tianshu Local",
                    "tianshu-service",
                    "tianshu-admin",
                    "TIANSHU_READ",
                    "tianshu-admin-web"
            ),
            new ResourceSpec(
                    "platform-yuheng-admin-local",
                    "https://api.egon.internal/local/platform/yuheng-admin",
                    "xingyuan",
                    "yuheng-admin",
                    "Yuheng Admin Local",
                    "yuheng-admin-service",
                    "yuheng-admin",
                    "yuheng:read",
                    "yuheng-admin-web"
            ),
            new ResourceSpec(
                    "identity-yuheng-biz-gateway-default-local",
                    "https://api.egon.internal/local/identity/yuheng-biz-gateway-default",
                    "identity",
                    "yuheng-biz-gateway-default",
                    "Yuheng Engine Local",
                    "yuheng-biz-gateway-service",
                    "mock-backend",
                    "mock:read",
                    null
            ),
            new ResourceSpec(
                    "identity-yuheng-mcp-gateway-default-local",
                    "https://api.egon.internal/local/identity/yuheng-mcp-gateway-default",
                    "identity",
                    "yuheng-mcp-gateway-default",
                    "Yuheng MCP Engine Local",
                    "yuheng-mcp-gateway-service",
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
                    "identity-yuheng-test-mcp-provider-local",
                    "https://api.egon.internal/local/identity/yuheng-test-mcp-provider",
                    "identity",
                    "yuheng-test-mcp-provider",
                    "Yuheng MCP Provider Local",
                    "mcp-provider-service",
                    "mock-backend",
                    "mock:read",
                    null
            )
    );

    /** 需要访问 Tianquan-Jianshen USER 决策接口的本地服务 Client；local service Clients calling Tianquan-Jianshen USER decisions. */
    private static final List<String> TIANQUAN_JIANSHEN_SERVICE_CLIENTS = List.of(
            "tianquan-shoubing-service",
            "tianquan-jianshen-service",
            "tianshu-service",
            "yuheng-admin-service",
            "yuheng-biz-gateway-service",
            "yuheng-mcp-gateway-service",
            "mock-backend-service",
            "mcp-provider-service"
    );

    /** Tianquan-Jianshen 内部 USER 决策接口所需 Scope；scopes required by Tianquan-Jianshen internal USER-decision APIs. */
    private static final Set<String> TIANQUAN_JIANSHEN_SERVICE_SCOPES = Set.of(
            "service:authorization:decide",
            "service:authorization:snapshot",
            "service:identity:resolve"
    );

    /** Gateway USER 在线状态检查所需的 Tianquan-Shoubing 内部 Scope。 */
    private static final Set<String> YUHENG_REFRESH_STATUS_SCOPES = Set.of(
            "tianquan-shoubing:refresh-token:validate"
    );

    /**
     * Gateway Admin 控制面 Service Token 所需 Scope；scopes required by the Gateway Admin
     * control-plane Service Token.
     */
    private static final Set<String> YUHENG_ADMIN_SERVICE_SCOPES = Set.of(
            "yuheng:read",
            "yuheng:applications:write",
            "yuheng:catalog:write",
            "yuheng:credentials:write",
            "yuheng:drafts:write",
            "yuheng:groups:write",
            "yuheng:mcp:approve",
            "yuheng:mcp:read",
            "yuheng:mcp:runtime:read",
            "yuheng:mcp:test",
            "yuheng:mcp:write",
            "yuheng:releases:write"
    );

    /** Gateway Admin 读取 Provider OpenAPI 文档所需的 PLATFORM Scope。 */
    private static final Set<String> YUHENG_OPENAPI_SERVICE_SCOPES = Set.of(
            "yuheng.openapi.read"
    );

    /** MCP Task Worker 的 Source Client；source Client used by the MCP task worker. */
    private static final String MCP_TASK_SERVICE_CLIENT =
            "yuheng-mcp-gateway-service";

    /** MCP Provider 的目标 Resource；target Resource exposed by the MCP Provider. */
    private static final String MCP_TASK_RESOURCE_SERVER =
            "identity-yuheng-test-mcp-provider-local";

    /** MCP Task Worker 调用 Provider 所需 Scope；scope required to invoke the MCP Provider. */
    private static final Set<String> MCP_TASK_SERVICE_SCOPES =
            Set.of("mcp:operation:invoke");

    /** 需要向 Tianshu 注册的本地服务 Client；local service Clients registering with Tianshu. */
    private static final List<String> TIANSHU_REGISTRATION_CLIENTS = List.of(
            "tianshu-service",
            "tianquan-shoubing-service",
            "tianquan-jianshen-service",
            "yuheng-admin-service",
            "yuheng-biz-gateway-service",
            "yuheng-mcp-gateway-service",
            "mock-backend-service",
            "mcp-provider-service"
    );

    /** OAuth Client 管理服务；OAuth Client management service. */
    @NonNull
    @Qualifier("OAuthClientServiceImpl")
    private final OAuthClientService clients;

    /** Resource Server 仓储；Resource Server repository. */
    @NonNull
    @Qualifier("identityResourceServerRepository")
    private final IdentityResourceServerRepository resources;

    /** Client Resource Grant 仓储；Client Resource Grant repository. */
    @NonNull
    @Qualifier("identityClientResourceGrantRepository")
    private final IdentityClientResourceGrantRepository grants;

    /** OAuth Client 主记录仓储；OAuth Client master-record repository. */
    @NonNull
    @Qualifier("identityClientRepository")
    private final IdentityClientRepository clientEntities;

    /** Resource 运行态投影服务；Resource runtime projection service. */
    @NonNull
    @Qualifier("resourceServerProjectionService")
    private final ResourceServerProjectionService projections;

    /** Tianquan-Shoubing 本地租户初始化；Tianquan-Shoubing-owned local tenant initialization. */
    @NonNull
    private final IdpDevelopmentTenantBootstrap developmentTenants;

    /** 本地机器 Client Secret 目录；local machine-Client Secret directory. */
    @NonNull
    @Value("${egon.tianquan-shoubing.development-bootstrap.key-directory:target/local-unified-xingyuan/secrets}")
    private final String secretDirectory;

    /** Tianquan-Jianshen 服务授权租户集合配置；configured tenants for Tianquan-Jianshen service grants. */
    @Value("${egon.tianquan-shoubing.development-bootstrap.tianquan-jianshen-service-tenant-ids:default}")
    private final String rbac3ServiceTenantIds;

    /**
     * 在 Tianshu 生命周期启动前，幂等对齐本地 Client、Resource 与显式 Grant。
     *
     * <p>Idempotently reconciles local Clients, Resources, and explicit grants before the
     * Tianshu lifecycle starts.</p>
     */
    @Override
    public void afterSingletonsInstantiated() {
        // 在任何身份写入前校验配置；validate configuration before identity side effects.
        Path.of(secretDirectory);
        Set<String> serviceTenantIds = developmentTenants.resolveTenantIds(
                tenantIds(rbac3ServiceTenantIds));
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
        reconcileRbac3ServiceGrants(serviceTenantIds);
        reconcileGatewayOpenApiServiceGrant();
        reconcileDdcPlatformServiceGrants();
        reconcileGatewayRefreshStatusGrant();
        reconcileGatewayAdminServiceGrants(serviceTenantIds);
        reconcileMcpTaskServiceGrants(serviceTenantIds);
        log.info("Reconciled local Tianquan-Shoubing development Client and Resource Server definitions");
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
            CreatedOAuthClientVO created = clients.create(new CreateOAuthClientDTO(
                    client.clientId(),
                    client.clientName(),
                    IdentityClientEntity.ClientType.CONFIDENTIAL,
                    ACCESS_TOKEN_TTL_SECONDS,
                    REFRESH_TOKEN_TTL_SECONDS,
                    List.of(),
                    List.of()
            ));
            writeSecret(client.clientId(), created.clientSecret());
            return;
        }
        if (!IdentityClientEntity.ClientType.CONFIDENTIAL.name()
                .equals(existing.clientType())) {
            throw new IllegalStateException(
                    "local machine Client type does not match: "
                            + client.clientId()
            );
        }
        Path secretFile = secretFile(client.clientId());
        if (!"ACTIVE".equals(existing.secretStatus())
                || !Files.isRegularFile(secretFile)) {
            RotatedClientSecretVO rotated = clients.rotateSecret(
                    client.clientId(),
                    new RotateClientSecretDTO(existing.version())
            );
            writeSecret(client.clientId(), rotated.clientSecret());
        }
    }

    /** 将一次性 Secret 原子写入 owner-only 本地文件。 */
    private void writeSecret(String clientId, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "local machine Client secret was not returned: " + clientId
            );
        }
        Path secretDirectory = Path.of(this.secretDirectory).toAbsolutePath().normalize();
        Path target = secretFile(clientId);
        Path temporary = null;
        try {
            Files.createDirectories(secretDirectory);
            Files.setPosixFilePermissions(
                    secretDirectory,
                    PosixFilePermissions.fromString("rwx------")
            );
            temporary = Files.createTempFile(
                    secretDirectory,
                    "." + clientId + ".",
                    ".secret"
            );
            Files.writeString(temporary, secret, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(
                    temporary,
                    PosixFilePermissions.fromString("rw-------")
            );
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "local machine Client secret could not be stored: "
                            + clientId,
                    exception
            );
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (java.io.IOException ignored) {
                    // The primary failure already preserves the safe error boundary.
                }
            }
        }
    }

    /** 返回一个受限目录内的稳定 Secret 文件。 */
    private Path secretFile(String clientId) {
        return Path.of(secretDirectory).toAbsolutePath().normalize()
                .resolve(clientId + ".secret").normalize();
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
     * 给需要查询 USER 权限的服务显式登记到 Tianquan-Jianshen 的 Service Grant。
     *
     * <p>Explicitly grants services that query USER permissions access to the Tianquan-Jianshen Resource.</p>
     */
    private void reconcileRbac3ServiceGrants(Set<String> rbac3ServiceTenantIds) {
        String target = "permission-tianquan-jianshen-local";
        String allowedScopes = TIANQUAN_JIANSHEN_SERVICE_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        TIANQUAN_JIANSHEN_SERVICE_CLIENTS.forEach(clientId -> {
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
                                    "dev-tianquan-jianshen-grant-"))
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
     * 给 Gateway Admin 控制面 Client 显式登记 Tianquan-Shoubing 签名的管理 Scope。
     *
     * <p>Explicitly grants the Gateway Admin control-plane Client the Tianquan-Shoubing-signed management
     * scopes used by the local catalog and route publisher.</p>
     */
    private void reconcileGatewayAdminServiceGrants(Set<String> rbac3ServiceTenantIds) {
        String target = "platform-yuheng-admin-local";
        String allowedScopes = YUHENG_ADMIN_SERVICE_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        rbac3ServiceTenantIds.forEach(tenantId -> {
            Optional<IdentityClientResourceGrantEntity> exact =
                    grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                            "yuheng-admin-service",
                            target,
                            IdentityClientResourceGrantEntity.GrantType
                                    .CLIENT_CREDENTIALS,
                            tenantId
                    );
            IdentityClientResourceGrantEntity grant = exact.orElseGet(() ->
                    IdentityClientResourceGrantEntity.clientCredentials(
                            gatewayAdminServiceGrantId(tenantId),
                            "yuheng-admin-service",
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
     * 给 Gateway Admin 注册读取本地 Provider OpenAPI 文档的 PLATFORM 授权。
     *
     * <p>OpenAPI documents are fetched with a PLATFORM Service Token because
     * the provider document is application metadata, not a tenant operation.</p>
     */
    private void reconcileGatewayOpenApiServiceGrant() {
        String allowedScopes = YUHENG_OPENAPI_SERVICE_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        for (String target : List.of("permission-tianquan-shoubing-local", "permission-tianquan-jianshen-local",
                "platform-yuheng-admin-local")) {
            Optional<IdentityClientResourceGrantEntity> existing =
                    grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                            "yuheng-admin-service",
                            target,
                            IdentityClientResourceGrantEntity.GrantType.CLIENT_CREDENTIALS,
                            null
                    );
            IdentityClientResourceGrantEntity grant = existing.orElseGet(() ->
                    IdentityClientResourceGrantEntity.platformClientCredentials(
                            "dev-yuheng-openapi-" + target,
                            "yuheng-admin-service",
                            target,
                            allowedScopes,
                            Instant.now()
                    ));
            if (existing.isPresent()
                    && allowedScopes.equals(grant.getAllowedScopes())
                    && grant.getStatus() == IdentityClientResourceGrantEntity.Status.ACTIVE) {
                continue;
            }
            if (existing.isPresent()) {
                grant.update(
                        IdentityClientResourceGrantEntity.GrantType.CLIENT_CREDENTIALS,
                        null,
                        allowedScopes,
                        grant.getVersion(),
                        Instant.now()
                );
            }
            grants.save(grant);
            projections.projectServiceGrant(grant);
        }
    }

    /** 给本地服务登记 Tianshu PLATFORM 注册授权。 */
    private void reconcileDdcPlatformServiceGrants() {
        String target = "platform-tianshu-local";
        TIANSHU_REGISTRATION_CLIENTS.forEach(clientId -> {
            String allowedScopes = "yuheng-admin-service".equals(clientId)
                    ? "[\"tianshu:registration:write\",\"yuheng.openapi.read\"]"
                    : "[\"tianshu:registration:write\"]";
            Optional<IdentityClientResourceGrantEntity> existing =
                    grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                            clientId,
                            target,
                            IdentityClientResourceGrantEntity.GrantType
                                    .CLIENT_CREDENTIALS,
                            null
                    );
            IdentityClientResourceGrantEntity grant = existing.orElseGet(() ->
                    IdentityClientResourceGrantEntity
                            .platformClientCredentials(
                                    "dev-tianshu-platform-grant-" + clientId,
                                    clientId,
                                    target,
                                    allowedScopes,
                                    Instant.now()
                            ));
            if (existing.isPresent()
                    && allowedScopes.equals(grant.getAllowedScopes())
                    && grant.getStatus()
                    == IdentityClientResourceGrantEntity.Status.ACTIVE) {
                return;
            }
            if (existing.isPresent()) {
                grant.update(
                        IdentityClientResourceGrantEntity.GrantType
                                .CLIENT_CREDENTIALS,
                        null,
                        allowedScopes,
                        grant.getVersion(),
                        Instant.now()
                );
            }
            grants.save(grant);
            projections.projectServiceGrant(grant);
        });
    }

    /** 给 Gateway Engine 登记 Tianquan-Shoubing Refresh Token 状态检查的 PLATFORM 授权。 */
    private void reconcileGatewayRefreshStatusGrant() {
        String allowedScopes = YUHENG_REFRESH_STATUS_SCOPES.stream()
                .sorted()
                .map(scope -> "\"" + scope + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        Optional<IdentityClientResourceGrantEntity> existing =
                grants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                        "yuheng-biz-gateway-service",
                        "permission-tianquan-shoubing-local",
                        IdentityClientResourceGrantEntity.GrantType
                                .CLIENT_CREDENTIALS,
                        null
                );
        IdentityClientResourceGrantEntity grant = existing.orElseGet(() ->
                IdentityClientResourceGrantEntity.platformClientCredentials(
                        "dev-tianquan-shoubing-refresh-status-platform-grant-yuheng-biz-gateway",
                        "yuheng-biz-gateway-service",
                        "permission-tianquan-shoubing-local",
                        allowedScopes,
                        Instant.now()
                ));
        if (existing.isPresent()
                && allowedScopes.equals(grant.getAllowedScopes())
                && grant.getStatus()
                == IdentityClientResourceGrantEntity.Status.ACTIVE) {
            return;
        }
        if (existing.isPresent()) {
            grant.update(
                    IdentityClientResourceGrantEntity.GrantType
                            .CLIENT_CREDENTIALS,
                    null,
                    allowedScopes,
                    grant.getVersion(),
                    Instant.now()
            );
        }
        grants.save(grant);
        projections.projectServiceGrant(grant);
    }

    /**
     * 给 Gateway Engine 的异步 MCP Worker 显式登记目标 Provider 的 Service Grant。
     * Explicitly grants the Gateway Engine asynchronous MCP worker access to the target
     * Provider Resource.
     */
    private void reconcileMcpTaskServiceGrants(Set<String> rbac3ServiceTenantIds) {
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
        return "dev-tianquan-jianshen-grant-" + clientId + "-" + suffix;
    }

    /**
     * 为精确租户生成稳定的 MCP Task Service Grant 标识。
     * Generates a stable MCP task Service Grant identifier for an exact tenant.
     */
    private static String mcpTaskServiceGrantId(String tenantId) {
        String suffix = UUID.nameUUIDFromBytes(
                tenantId.getBytes(StandardCharsets.UTF_8)
        ).toString().substring(0, 8);
        return "dev-mcp-engine-task-grant-" + suffix;
    }

    /**
     * 为精确租户生成稳定的 Gateway Admin Service Grant 标识。
     */
    private static String gatewayAdminServiceGrantId(String tenantId) {
        String suffix = UUID.nameUUIDFromBytes(
                tenantId.getBytes(StandardCharsets.UTF_8)
        ).toString().substring(0, 8);
        return "dev-yuheng-admin-grant-" + suffix;
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
     * @param rbacApplicationCode Tianquan-Jianshen 应用；Tianquan-Jianshen application
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
