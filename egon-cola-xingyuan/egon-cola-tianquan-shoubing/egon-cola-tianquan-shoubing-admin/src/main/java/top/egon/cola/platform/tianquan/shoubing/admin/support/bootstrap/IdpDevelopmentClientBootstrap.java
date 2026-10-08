package top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

@Slf4j
@RequiredArgsConstructor
@Component("idpDevelopmentClientBootstrap")
@Profile("local")
@ConditionalOnProperty(prefix = "egon.tianquan-shoubing.development-bootstrap",
        name = "enabled", havingValue = "true")
public class IdpDevelopmentClientBootstrap implements SmartInitializingSingleton {

    private static final String ACTOR = "local-development-bootstrap";

    /** 开发 Client 的 Access Token 有效秒数；development access-token lifetime in seconds. */
    private static final int ACCESS_TOKEN_TTL_SECONDS = 600;

    /** 开发 Client 的 Refresh Token 有效秒数；development refresh-token lifetime in seconds. */
    private static final int REFRESH_TOKEN_TTL_SECONDS = 0;

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

    @NonNull
    @Qualifier("OAuthClientServiceImpl")
    private final OAuthClientService clients;
    @NonNull
    @Qualifier("resourceServerServiceImpl")
    private final ResourceServerService resources;
    @NonNull
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realms;
    @NonNull
    private final IdpDevelopmentTenantBootstrap developmentTenants;
    @Value("${egon.tianquan-shoubing.development-bootstrap.key-directory:target/local-unified-xingyuan/secrets}")
    private final String secretDirectory;
    @Value("${egon.tianquan-shoubing.development-bootstrap.tianquan-jianshen-service-tenant-ids:default}")
    private final String rbac3ServiceTenantIds;

    @Override
    public void afterSingletonsInstantiated() {
        Path.of(secretDirectory);
        Set<String> tenantIds = developmentTenants.resolveTenantIds(tenantIds(rbac3ServiceTenantIds));
        TrustedRealmBO platform = realms.platformRealm();
        List<TrustedRealmBO> tenantRealms = tenantIds.stream().map(realms::resolveTenantRealm).toList();

        List<OAuthClientVO> platformClients = clients.list(platform);
        MACHINE_CLIENTS.forEach(spec -> reconcileMachineClient(platform, spec, platformClients));

        Map<String, ResourceServerVO> platformResources = resources.list(platform).stream()
                .collect(Collectors.toUnmodifiableMap(ResourceServerVO::getResourceServerId, value -> value));
        for (ResourceSpec spec : RESOURCES) {
            ResourceServerVO resource = reconcileResource(platform, spec, platformResources.get(spec.resourceServerId()));
            platformResources = replace(platformResources, spec.resourceServerId(), resource);
        }

        for (TrustedRealmBO tenantRealm : tenantRealms) {
            List<OAuthClientVO> existing = clients.list(tenantRealm);
            for (ClientSpec spec : CLIENTS) {
                reconcilePublicClient(tenantRealm, platform, spec, existing);
            }
            for (String clientId : TIANQUAN_JIANSHEN_SERVICE_CLIENTS) {
                reconcileMachineClient(tenantRealm, machineClient(clientId), clients.list(tenantRealm));
            }
            reconcileUserGrants(tenantRealm, platform);
            reconcileRbac3ServiceGrants(tenantRealm, platform);
            reconcileGatewayAdminServiceGrants(tenantRealm, platform);
            reconcileMcpTaskServiceGrants(tenantRealm, platform);
        }

        reconcileGatewayOpenApiServiceGrant(platform);
        reconcileDdcPlatformServiceGrants(platform);
        reconcileGatewayRefreshStatusGrant(platform);
        log.info("Reconciled local development OAuth clients and resources for {} tenant realms", tenantRealms.size());
    }

    private void reconcilePublicClient(
            TrustedRealmBO realm,
            TrustedRealmBO platform,
            ClientSpec spec,
            List<OAuthClientVO> existingClients) {
        List<String> resourceUris = RESOURCES.stream()
                .filter(resource -> spec.clientId().equals(resource.userClientId()))
                .map(ResourceSpec::resourceUri)
                .sorted()
                .toList();
        if (resourceUris.isEmpty()) {
            throw new IllegalStateException("local public client has no registered Resource URI: " + spec.clientId());
        }
        String redirect = redirectUri(spec);
        OAuthClientVO current = existingClients.stream()
                .filter(client -> spec.clientId().equals(client.getClientId()))
                .findFirst().orElse(null);
        if (current == null) {
            clients.create(realm, platform, CreateOAuthClientDTO.builder()
                    .clientId(spec.clientId())
                    .clientName(spec.clientName())
                    .clientType(OAuthClient.ClientType.PUBLIC)
                    .purpose(OAuthClientPurposeEnum.USER)
                    .accessTokenTtlSeconds(ACCESS_TOKEN_TTL_SECONDS)
                    .refreshTokenTtlSeconds(REFRESH_TOKEN_TTL_SECONDS)
                    .redirectUris(List.of(redirect))
                    .resourceUris(resourceUris)
                    .allowedScopes(List.of("openid", "profile"))
                    .build(), ACTOR);
            return;
        }
        if (!"PUBLIC".equals(current.getClientType())
                || !"USER".equals(current.getPurpose())
                || !"ACTIVE".equals(current.getStatus())) {
            throw new IllegalStateException("local public Client definition does not match: " + spec.clientId());
        }
        if (!current.getRedirectUris().contains(redirect)) {
            clients.putRedirectUri(realm, spec.clientId(), redirect);
        }
        for (String obsolete : spec.obsoleteRedirectUris()) {
            if (current.getRedirectUris().contains(obsolete)) {
                clients.deleteRedirectUri(realm, spec.clientId(), obsolete);
            }
        }
        for (String resourceUri : resourceUris) {
            if (!current.getResourceUris().contains(resourceUri)) {
                clients.putResourceUri(realm, platform, spec.clientId(), resourceUri);
            }
        }
        for (String obsolete : current.getResourceUris()) {
            if (!resourceUris.contains(obsolete)) {
                clients.deleteResourceUri(realm, platform, spec.clientId(), obsolete);
            }
        }
    }

    private void reconcileMachineClient(TrustedRealmBO realm, MachineClientSpec spec, List<OAuthClientVO> existingClients) {
        OAuthClientVO current = existingClients.stream()
                .filter(client -> spec.clientId().equals(client.getClientId()))
                .findFirst().orElse(null);
        if (current == null) {
            CreatedOAuthClientVO created = clients.create(realm, realms.platformRealm(), machineClientCommand(spec), ACTOR);
            writeSecret(spec.clientId(), realm, created.getClientSecret());
            return;
        }
        if (!"CONFIDENTIAL".equals(current.getClientType()) || !"SERVICE".equals(current.getPurpose())) {
            throw new IllegalStateException("local machine Client type or purpose does not match: " + spec.clientId());
        }
        if (!"ACTIVE".equals(current.getSecretStatus()) || !Files.isRegularFile(secretFile(spec.clientId(), realm))) {
            RotatedClientSecretVO rotated = clients.rotateSecret(
                    realm, spec.clientId(), new RotateClientSecretDTO(current.getVersion()), ACTOR);
            writeSecret(spec.clientId(), realm, rotated.getClientSecret());
        }
    }

    private static CreateOAuthClientDTO machineClientCommand(MachineClientSpec spec) {
        return CreateOAuthClientDTO.builder()
                .appId(spec.clientId())
                .clientId(spec.clientId())
                .clientName(spec.clientName())
                .clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE)
                .accessTokenTtlSeconds(ACCESS_TOKEN_TTL_SECONDS)
                .refreshTokenTtlSeconds(REFRESH_TOKEN_TTL_SECONDS)
                .redirectUris(List.of())
                .resourceUris(List.of())
                .allowedScopes(List.of())
                .build();
    }

    private MachineClientSpec machineClient(String clientId) {
        return MACHINE_CLIENTS.stream().filter(spec -> spec.clientId().equals(clientId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown development service Client: " + clientId));
    }

    private ResourceServerVO reconcileResource(TrustedRealmBO platform, ResourceSpec spec, ResourceServerVO existing) {
        ResourceServerVO resource = existing;
        if (resource == null) {
            resource = resources.create(platform, new CreateResourceServerDTO(
                    spec.resourceServerId(), spec.resourceUri(), spec.bizCode(), spec.appCode(), "local",
                    spec.displayName(), spec.managementClientId(), spec.rbacApplicationCode(), spec.entryPermissionCode()));
        } else {
            requireMatchingResource(spec, resource);
        }
        if (!"ACTIVE".equals(resource.getStatus())) {
            resource = resources.enable(platform, resource.getResourceServerId(), new ResourceVersionDTO(resource.getVersion()));
        }
        return resource;
    }

    private void reconcileUserGrants(TrustedRealmBO realm, TrustedRealmBO platform) {
        for (ResourceSpec spec : RESOURCES) {
            if (spec.userClientId() != null) {
                ResourceServerVO resource = resources.detail(platform, spec.resourceServerId());
                ensureGrant(realm, platform, spec.userClientId(), spec.resourceServerId(),
                        ResourceGrantType.USER_DELEGATION, null, Set.of(), resource);
            }
        }
    }

    private void reconcileRbac3ServiceGrants(TrustedRealmBO realm, TrustedRealmBO platform) {
        for (String clientId : TIANQUAN_JIANSHEN_SERVICE_CLIENTS) {
            String target = "permission-tianquan-jianshen-local";
            ensureGrant(realm, platform, clientId, target, ResourceGrantType.CLIENT_CREDENTIALS,
                    realm.getBusinessTenantId().toString(), TIANQUAN_JIANSHEN_SERVICE_SCOPES,
                    resources.detail(platform, target));
        }
    }

    private void reconcileGatewayAdminServiceGrants(TrustedRealmBO realm, TrustedRealmBO platform) {
        String target = "platform-yuheng-admin-local";
        ensureGrant(realm, platform, "yuheng-admin-service", target, ResourceGrantType.CLIENT_CREDENTIALS,
                realm.getBusinessTenantId().toString(), YUHENG_ADMIN_SERVICE_SCOPES, resources.detail(platform, target));
    }

    private void reconcileMcpTaskServiceGrants(TrustedRealmBO realm, TrustedRealmBO platform) {
        ensureGrant(realm, platform, MCP_TASK_SERVICE_CLIENT, MCP_TASK_RESOURCE_SERVER,
                ResourceGrantType.CLIENT_CREDENTIALS, realm.getBusinessTenantId().toString(),
                MCP_TASK_SERVICE_SCOPES, resources.detail(platform, MCP_TASK_RESOURCE_SERVER));
    }

    private void reconcileGatewayOpenApiServiceGrant(TrustedRealmBO platform) {
        for (String target : List.of("permission-tianquan-shoubing-local", "permission-tianquan-jianshen-local",
                "platform-yuheng-admin-local")) {
            ensureGrant(platform, platform, "yuheng-admin-service", target, ResourceGrantType.CLIENT_CREDENTIALS,
                    null, YUHENG_OPENAPI_SERVICE_SCOPES, resources.detail(platform, target));
        }
    }

    private void reconcileDdcPlatformServiceGrants(TrustedRealmBO platform) {
        for (String clientId : TIANSHU_REGISTRATION_CLIENTS) {
            Set<String> scopes = "yuheng-admin-service".equals(clientId)
                    ? Set.of("tianshu:registration:write", "yuheng.openapi.read")
                    : Set.of("tianshu:registration:write");
            String target = "platform-tianshu-local";
            ensureGrant(platform, platform, clientId, target, ResourceGrantType.CLIENT_CREDENTIALS,
                    null, scopes, resources.detail(platform, target));
        }
    }

    private void reconcileGatewayRefreshStatusGrant(TrustedRealmBO platform) {
        String target = "permission-tianquan-shoubing-local";
        ensureGrant(platform, platform, "yuheng-biz-gateway-service", target,
                ResourceGrantType.CLIENT_CREDENTIALS, null, YUHENG_REFRESH_STATUS_SCOPES,
                resources.detail(platform, target));
    }

    private void ensureGrant(
            TrustedRealmBO clientRealm, TrustedRealmBO platform, String clientId, String resourceId,
            ResourceGrantType type, String tenantId, Set<String> scopes, ResourceServerVO resource) {
        List<ClientResourceGrantVO> existing = resources.listGrants(clientRealm, platform, clientId);
        if (existing.size() > 64) {
            throw new IllegalStateException("local Client Grant list exceeds the management limit: " + clientId);
        }
        ClientResourceGrantVO current = existing.stream()
                .filter(grant -> resourceId.equals(grant.getResourceServerId())
                        && type.name().equals(grant.getGrantType())
                        && Objects.equals(tenantId, grant.getTenantId()))
                .findFirst().orElse(null);
        Set<String> desiredScopes = type == ResourceGrantType.USER_DELEGATION ? Set.of() : Set.copyOf(scopes);
        if (current != null && "ACTIVE".equals(current.getStatus())
                && desiredScopes.equals(current.getAllowedScopes())) {
            return;
        }
        resources.putGrant(clientRealm, platform, clientId, resourceId,
                UpsertClientResourceGrantDTO.builder()
                        .grantType(type)
                        .tenantId(tenantId)
                        .allowedScopes(desiredScopes)
                        .expectedResourceVersion(resource.getVersion())
                        .expectedGrantVersion(current == null ? null : current.getVersion())
                        .build());
    }

    private void writeSecret(String clientId, TrustedRealmBO realm, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("local machine Client secret was not returned: " + clientId);
        }
        Path directory = Path.of(secretDirectory).toAbsolutePath().normalize();
        Path target = secretFile(clientId, realm);
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
            temporary = Files.createTempFile(directory, "." + clientId + ".", ".secret");
            Files.writeString(temporary, secret, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"));
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("local machine Client secret could not be stored: " + clientId, failure);
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

    private Path secretFile(String clientId, TrustedRealmBO realm) {
        String suffix = realm.getBusinessTenantId() == null ? "" : "." + realm.getBusinessTenantId();
        return Path.of(secretDirectory).toAbsolutePath().normalize().resolve(clientId + suffix + ".secret");
    }

    private String redirectUri(ClientSpec client) {
        return "http://127.0.0.1:" + client.port() + "/oauth/callback";
    }

    private static Map<String, ResourceServerVO> replace(Map<String, ResourceServerVO> current, String id,
            ResourceServerVO resource) {
        java.util.Map<String, ResourceServerVO> updated = new java.util.LinkedHashMap<>(current);
        updated.put(id, resource);
        return java.util.Collections.unmodifiableMap(updated);
    }

    private static void requireMatchingResource(ResourceSpec spec, ResourceServerVO actual) {
        if (!spec.resourceUri().equals(actual.getResourceUri())
                || !spec.bizCode().equals(actual.getBizCode())
                || !spec.appCode().equals(actual.getAppCode())
                || !"local".equals(actual.getEnvironment())
                || !spec.managementClientId().equals(actual.getManagementClientId())) {
            throw new IllegalStateException("local Resource Server definition does not match: " + spec.resourceServerId());
        }
    }

    private static Set<String> tenantIds(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("rbac3ServiceTenantIds is required");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tenantId : value.split(",", -1)) {
            if (tenantId.isBlank() || !tenantId.equals(tenantId.trim())) {
                throw new IllegalArgumentException("rbac3ServiceTenantId is required");
            }
            result.add(tenantId);
        }
        return Collections.unmodifiableSet(result);
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
