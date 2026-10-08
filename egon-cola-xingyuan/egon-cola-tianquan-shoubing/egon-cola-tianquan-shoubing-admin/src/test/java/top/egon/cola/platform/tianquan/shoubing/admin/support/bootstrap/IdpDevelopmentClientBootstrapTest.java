package top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
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

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdpDevelopmentClientBootstrapTest {

    private static final TrustedRealmBO PLATFORM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(1L)
            .trustVersion(1L)
            .build();
    private final OAuthClientService clients = mock(OAuthClientService.class);
    private final ResourceServerService resources = mock(ResourceServerService.class);
    private final OAuthManagementRealmResolver realms = mock(OAuthManagementRealmResolver.class);
    private final IdpDevelopmentTenantBootstrap tenants = mock(IdpDevelopmentTenantBootstrap.class);
    private final Map<String, ResourceServerVO> resourceStore = new ConcurrentHashMap<>();

    @TempDir
    Path secretDirectory;

    @BeforeEach
    void setUp() {
        when(realms.platformRealm()).thenReturn(PLATFORM);
        when(realms.resolveTenantRealm("7")).thenReturn(tenant(7L));
        when(tenants.resolveTenantIds(any())).thenReturn(Set.of("7"));
        when(clients.list(any(TrustedRealmBO.class))).thenReturn(List.of());
        when(clients.create(any(), any(), any(), any())).thenAnswer(invocation -> {
            TrustedRealmBO realm = invocation.getArgument(0);
            var command = invocation.<top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto
                    .CreateOAuthClientDTO>getArgument(2);
            String secret = command.getClientType() == OAuthClient.ClientType.CONFIDENTIAL
                    ? "local-secret-" + realm.getRealmKey() + "-" + command.getClientId()
                    : null;
            return CreatedOAuthClientVO.builder()
                    .clientId(command.getClientId())
                    .appId(command.getAppId())
                    .clientName(command.getClientName())
                    .clientType(command.getClientType().name())
                    .purpose(command.getPurpose().name())
                    .status("ACTIVE")
                    .clientSecret(secret)
                    .secretHint(secret == null ? null : "cret")
                    .version(0L)
                    .createdAt(Instant.EPOCH)
                    .issuer(realm.getIssuer().toASCIIString())
                    .build();
        });
        when(resources.list(PLATFORM)).thenReturn(List.of());
        when(resources.create(eq(PLATFORM), any(CreateResourceServerDTO.class))).thenAnswer(invocation -> {
            CreateResourceServerDTO command = invocation.getArgument(1);
            ResourceServerVO created = resource(command, "DISABLED", 0L);
            resourceStore.put(command.resourceServerId(), created);
            return created;
        });
        when(resources.enable(eq(PLATFORM), any(String.class), any(ResourceVersionDTO.class)))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(1);
                    ResourceServerVO active = withState(resourceStore.get(id), "ACTIVE", 1L);
                    resourceStore.put(id, active);
                    return active;
                });
        when(resources.detail(eq(PLATFORM), any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(resourceStore.get(invocation.getArgument(1)))
                        .orElseThrow(() -> new IllegalStateException("test resource missing")));
        when(resources.listGrants(any(TrustedRealmBO.class), eq(PLATFORM), any(String.class)))
                .thenReturn(List.of());
        when(resources.putGrant(any(), eq(PLATFORM), any(String.class), any(String.class),
                any(UpsertClientResourceGrantDTO.class))).thenAnswer(invocation -> null);
    }

    @Test
    void createsRealmScopedClientsResourcesGrantsAndRestrictedSecretFiles() throws Exception {
        fixture("7").afterSingletonsInstantiated();

        verify(clients, atLeastOnce()).create(
                eq(tenant(7L)), eq(PLATFORM), any(), eq("local-development-bootstrap"));
        verify(resources, atLeastOnce()).create(eq(PLATFORM), any(CreateResourceServerDTO.class));
        verify(resources).putGrant(eq(tenant(7L)), eq(PLATFORM), eq("tianquan-shoubing-service"), any(), any());

        Path platformSecret = secretDirectory.resolve("tianquan-shoubing-service.secret");
        Path tenantSecret = secretDirectory.resolve("tianquan-shoubing-service.7.secret");
        assertThat(Files.readString(platformSecret)).contains("platform");
        assertThat(Files.readString(tenantSecret)).contains("tenant:7");
        assertThat(Files.getPosixFilePermissions(secretDirectory))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE);
        assertThat(Files.getPosixFilePermissions(tenantSecret))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(Files.readString(platformSecret)).isNotEqualTo(Files.readString(tenantSecret));
    }

    @Test
    void reconcilesOneExactRbac3ServiceGrantForEachConfiguredTenant() {
        when(tenants.resolveTenantIds(any())).thenReturn(Set.of("7", "8"));
        when(realms.resolveTenantRealm("8")).thenReturn(tenant(8L));

        fixture("7,8").afterSingletonsInstantiated();

        Set<String> serviceClients = Set.of(
                "tianquan-shoubing-service", "tianquan-jianshen-service", "tianshu-service",
                "yuheng-admin-service", "yuheng-biz-gateway-service", "yuheng-mcp-gateway-service",
                "mock-backend-service", "mcp-provider-service");
        for (TrustedRealmBO realm : List.of(tenant(7L), tenant(8L))) {
            for (String clientId : serviceClients) {
                var grant = org.mockito.ArgumentCaptor.forClass(UpsertClientResourceGrantDTO.class);
                verify(resources).putGrant(eq(realm), eq(PLATFORM), eq(clientId),
                        eq("permission-tianquan-jianshen-local"), grant.capture());
                assertThat(grant.getValue().getGrantType()).isEqualTo(ResourceGrantType.CLIENT_CREDENTIALS);
                assertThat(grant.getValue().getTenantId()).isEqualTo(realm.getBusinessTenantId().toString());
                assertThat(grant.getValue().getAllowedScopes()).containsExactlyInAnyOrder(
                        "service:authorization:decide", "service:authorization:snapshot", "service:identity:resolve");
            }
        }
    }

    @Test
    void rejectsMismatchedMcpResourceInsteadOfOverwritingIt() {
        ResourceServerVO mismatch = ResourceServerVO.builder()
                .resourceServerId("identity-yuheng-test-mcp-provider-local")
                .resourceUri("https://wrong.example/resource")
                .bizCode("identity")
                .appCode("yuheng-test-mcp-provider")
                .environment("local")
                .displayName("incorrect")
                .managementClientId("mcp-provider-service")
                .rbacApplicationCode("mock-backend")
                .entryPermissionCode("mock:read")
                .status("ACTIVE")
                .version(4L)
                .build();
        when(resources.list(PLATFORM)).thenReturn(List.of(mismatch));

        assertThatThrownBy(() -> fixture("7").afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("definition does not match");
        verify(resources, never()).enable(eq(PLATFORM), eq(mismatch.getResourceServerId()), any());
    }

    @Test
    void preservesExistingGatewayIdentitiesAndSecrets() throws Exception {
        List<String> clientIds = List.of("yuheng-admin-service", "yuheng-biz-gateway-service",
                "yuheng-mcp-gateway-service");
        when(clients.list(PLATFORM)).thenReturn(clientIds.stream().map(this::serviceClientView).toList());
        Files.createDirectories(secretDirectory);
        for (String clientId : clientIds) {
            Files.writeString(secretDirectory.resolve(clientId + ".secret"), "existing-secret-" + clientId);
        }

        fixture("7").afterSingletonsInstantiated();

        for (String clientId : clientIds) {
            verify(clients, never()).rotateSecret(eq(PLATFORM), eq(clientId), any(), any());
            assertThat(Files.readString(secretDirectory.resolve(clientId + ".secret")))
                    .isEqualTo("existing-secret-" + clientId);
        }
    }

    @Test
    void replacesTheObsoleteTianshuAdminRedirectWithTheCurrentCallback() {
        OAuthClientVO existing = OAuthClientVO.builder()
                .clientId("tianshu-admin-web")
                .clientName("Tianshu Admin Web")
                .clientType("PUBLIC")
                .purpose("USER")
                .status("ACTIVE")
                .version(0L)
                .redirectUris(List.of("http://127.0.0.1:18151/oauth/callback"))
                .resourceUris(List.of())
                .allowedScopes(List.of("openid", "profile"))
                .build();
        when(clients.list(tenant(7L))).thenReturn(List.of(existing));

        fixture("7").afterSingletonsInstantiated();

        verify(clients).putRedirectUri(tenant(7L), "tianshu-admin-web",
                "http://127.0.0.1:18152/oauth/callback");
        verify(clients).deleteRedirectUri(tenant(7L), "tianshu-admin-web",
                "http://127.0.0.1:18151/oauth/callback");
        var created = org.mockito.ArgumentCaptor.forClass(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO.class);
        verify(clients, times(12)).create(eq(tenant(7L)), eq(PLATFORM), created.capture(),
                eq("local-development-bootstrap"));
        assertThat(created.getAllValues()).extracting(
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO::getClientId)
                .doesNotContain("tianshu-admin-web");
    }

    @Test
    void doesNotReuseAnOtherClientLegacyMcpTaskGrant() {
        ClientResourceGrantVO legacy = grant("yuheng-biz-gateway-service",
                "identity-yuheng-test-mcp-provider-local", "7", Set.of("mcp:operation:invoke"), 0L);
        when(resources.listGrants(tenant(7L), PLATFORM, "yuheng-biz-gateway-service"))
                .thenReturn(List.of(legacy));

        fixture("7").afterSingletonsInstantiated();

        var grant = org.mockito.ArgumentCaptor.forClass(UpsertClientResourceGrantDTO.class);
        verify(resources).putGrant(eq(tenant(7L)), eq(PLATFORM), eq("yuheng-mcp-gateway-service"),
                eq("identity-yuheng-test-mcp-provider-local"), grant.capture());
        assertThat(grant.getValue().getExpectedGrantVersion()).isNull();
        verify(resources, never()).putGrant(eq(tenant(7L)), eq(PLATFORM),
                eq("yuheng-biz-gateway-service"), eq("identity-yuheng-test-mcp-provider-local"), any());
    }

    @Test
    void reusesExistingMcpGrantsWithoutRewritingTheirVersions() {
        List<ClientResourceGrantVO> existing = List.of(
                grant("yuheng-mcp-gateway-service", "permission-tianquan-jianshen-local", "7",
                        Set.of("service:authorization:decide", "service:authorization:snapshot",
                                "service:identity:resolve"), 0L),
                grant("yuheng-mcp-gateway-service", "identity-yuheng-test-mcp-provider-local", "7",
                        Set.of("mcp:operation:invoke"), 0L));
        when(resources.listGrants(tenant(7L), PLATFORM, "yuheng-mcp-gateway-service")).thenReturn(existing);

        fixture("7").afterSingletonsInstantiated();

        verify(resources, never()).putGrant(eq(tenant(7L)), eq(PLATFORM), eq("yuheng-mcp-gateway-service"),
                eq("permission-tianquan-jianshen-local"), any());
        verify(resources, never()).putGrant(eq(tenant(7L)), eq(PLATFORM), eq("yuheng-mcp-gateway-service"),
                eq("identity-yuheng-test-mcp-provider-local"), any());
    }

    @Test
    void addsDdcRegistrationScopeToTheExistingPlatformOpenApiGrant() {
        ClientResourceGrantVO existing = grant("yuheng-admin-service", "platform-tianshu-local", null,
                Set.of("tianshu:registration:write"), 3L);
        when(resources.listGrants(PLATFORM, PLATFORM, "yuheng-admin-service")).thenReturn(List.of(existing));

        fixture("7").afterSingletonsInstantiated();

        var grant = org.mockito.ArgumentCaptor.forClass(UpsertClientResourceGrantDTO.class);
        verify(resources).putGrant(eq(PLATFORM), eq(PLATFORM), eq("yuheng-admin-service"),
                eq("platform-tianshu-local"), grant.capture());
        assertThat(grant.getValue().getTenantId()).isNull();
        assertThat(grant.getValue().getAllowedScopes()).containsExactlyInAnyOrder(
                "tianshu:registration:write", "yuheng.openapi.read");
        assertThat(grant.getValue().getExpectedGrantVersion()).isEqualTo(3L);
    }

    @Test
    void rejectsInvalidTenantConfigurationBeforeServiceSideEffects() {
        for (String configured : List.of("", " 7", "7,", "7, 8")) {
            assertThatThrownBy(() -> fixture(configured).afterSingletonsInstantiated())
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(clients, resources);
    }

    @Test
    void constructorRetainsQualifiedDependenciesAndConfiguredTenantProperty() throws Exception {
        Class<?> type = IdpDevelopmentClientBootstrap.class;
        assertThat(type.getAnnotation(Component.class).value()).isEqualTo("idpDevelopmentClientBootstrap");
        assertThat(type.getAnnotation(Profile.class).value()).contains("local");
        assertThat(type.getAnnotation(ConditionalOnProperty.class).havingValue()).isEqualTo("true");
        assertThat(field("clients").getAnnotation(Qualifier.class).value()).isEqualTo("OAuthClientServiceImpl");
        assertThat(field("resources").getAnnotation(Qualifier.class).value()).isEqualTo("resourceServerServiceImpl");
        assertThat(field("realms").getAnnotation(Qualifier.class).value()).isEqualTo("oauthManagementRealmResolver");
        assertThat(field("secretDirectory").getAnnotation(Value.class).value())
                .isEqualTo("${egon.tianquan-shoubing.development-bootstrap.key-directory:target/local-unified-xingyuan/secrets}");
        assertThat(field("rbac3ServiceTenantIds").getAnnotation(Value.class).value())
                .isEqualTo("${egon.tianquan-shoubing.development-bootstrap.tianquan-jianshen-service-tenant-ids:default}");
    }

    private IdpDevelopmentClientBootstrap fixture(String configuredTenants) {
        return new IdpDevelopmentClientBootstrap(clients, resources, realms, tenants,
                secretDirectory.toString(), configuredTenants);
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = IdpDevelopmentClientBootstrap.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static TrustedRealmBO tenant(long tenantId) {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example.test/tenants/" + tenantId))
                .realmKey("tenant:" + tenantId)
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(tenantId)
                .businessTenantId(tenantId)
                .trustVersion(1L)
                .build();
    }

    private static ResourceServerVO resource(CreateResourceServerDTO command, String status, long version) {
        return ResourceServerVO.builder()
                .resourceServerId(command.resourceServerId())
                .resourceUri(command.resourceUri())
                .bizCode(command.bizCode())
                .appCode(command.appCode())
                .environment(command.environment())
                .displayName(command.displayName())
                .managementClientId(command.managementClientId())
                .rbacApplicationCode(command.rbacApplicationCode())
                .entryPermissionCode(command.entryPermissionCode())
                .status(status)
                .version(version)
                .createdAt(Instant.EPOCH)
                .updatedAt(Instant.EPOCH)
                .build();
    }

    private static ResourceServerVO withState(ResourceServerVO resource, String status, long version) {
        return ResourceServerVO.builder()
                .resourceServerId(resource.getResourceServerId())
                .resourceUri(resource.getResourceUri())
                .bizCode(resource.getBizCode())
                .appCode(resource.getAppCode())
                .environment(resource.getEnvironment())
                .displayName(resource.getDisplayName())
                .managementClientId(resource.getManagementClientId())
                .rbacApplicationCode(resource.getRbacApplicationCode())
                .entryPermissionCode(resource.getEntryPermissionCode())
                .status(status)
                .version(version)
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }

    private static ClientResourceGrantVO grant(String clientId, String resourceId, String tenantId,
            Set<String> scopes, long version) {
        return ClientResourceGrantVO.builder()
                .clientId(clientId)
                .resourceServerId(resourceId)
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS.name())
                .tenantId(tenantId)
                .allowedScopes(scopes)
                .status("ACTIVE")
                .version(version)
                .build();
    }

    private OAuthClientVO serviceClientView(String clientId) {
        return OAuthClientVO.builder()
                .clientId(clientId)
                .appId(clientId)
                .clientName(clientId)
                .clientType("CONFIDENTIAL")
                .purpose("SERVICE")
                .status("ACTIVE")
                .secretStatus("ACTIVE")
                .version(5L)
                .redirectUris(List.of())
                .resourceUris(List.of())
                .allowedScopes(List.of())
                .build();
    }
}
