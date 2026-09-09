package top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthClientServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService;

import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class IdpDevelopmentClientBootstrapTest {

    @TempDir
    Path secretDirectory;

    private final OAuthClientService bootstrapClients = mock(OAuthClientService.class);
    private final IdentityResourceServerRepository bootstrapResources =
            mock(IdentityResourceServerRepository.class);
    private final IdentityClientResourceGrantRepository bootstrapGrants =
            mock(IdentityClientResourceGrantRepository.class);
    private final IdentityClientRepository bootstrapClientEntities =
            mock(IdentityClientRepository.class);
    private final ResourceServerProjectionService bootstrapProjections =
            mock(ResourceServerProjectionService.class);

    @Test
    void createsOnlyMissingPublicClients() throws Exception {
        OAuthClientService clients = mock(OAuthClientService.class);
        IdentityResourceServerRepository resources =
                mock(IdentityResourceServerRepository.class);
        IdentityClientResourceGrantRepository grants =
                mock(IdentityClientResourceGrantRepository.class);
        IdentityClientRepository clientEntities =
                mock(IdentityClientRepository.class);
        ResourceServerProjectionService projections =
                mock(ResourceServerProjectionService.class);
        when(clients.list()).thenReturn(List.of(client("tianquan-shoubing-admin-web")));
        stubClientCreation(clients);
        when(clientEntities.findById(any())).thenReturn(Optional.of(
                machineClient("management-client")
        ));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(
                        clients,
                        resources,
                        grants,
                        clientEntities,
                        projections,
                        tenantBootstrap(),
                        secretDirectory.toString(),
                        "default"
                );

        bootstrap.afterSingletonsInstantiated();

        verify(clients, never()).create(argThat(command ->
                command.clientId().equals("tianquan-shoubing-admin-web")));
        verify(clients).create(argThat(command ->
                command.clientId().equals("mock-backend")
                        && command.redirectUris().equals(List.of(
                        "http://127.0.0.1:18161/oauth/callback"))
                        && command.resourceUris().isEmpty()));
        verify(clients).create(argThat(command ->
                command.clientId().equals("tianquan-shoubing-service")
                        && command.clientType()
                        == IdentityClientEntity.ClientType.CONFIDENTIAL
                        && command.redirectUris().isEmpty()
                        && command.resourceUris().isEmpty()));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("permission-tianquan-shoubing-local")
                        && resource.getAppCode().equals("tianquan-shoubing")
                        && resource.getManagementClientId()
                        .equals("tianquan-shoubing-service")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("permission-tianquan-jianshen-local")
                        && resource.getAppCode().equals("tianquan-jianshen")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("platform-tianshu-local")
                        && resource.getBizCode().equals("xingyuan")
                        && resource.getAppCode().equals("tianshu")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId()
                        .equals("identity-yuheng-biz-gateway-default-local")
                        && resource.getResourceUri().equals(
                        "https://api.egon.internal/local/identity/yuheng-biz-gateway-default")
                        && resource.getBizCode().equals("identity")
                        && resource.getEnvironment().equals("local")
                        && resource.getManagementClientId().equals("yuheng-biz-gateway-service")
                        && resource.getAppCode()
                        .equals("yuheng-biz-gateway-default")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("platform-yuheng-admin-local")
                        && resource.getResourceUri().equals(
                        "https://api.egon.internal/local/platform/yuheng-admin")
                        && resource.getBizCode().equals("xingyuan")
                        && resource.getAppCode().equals("yuheng-admin")
                        && resource.getEnvironment().equals("local")
                        && resource.getManagementClientId().equals("yuheng-admin-service")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("identity-yuheng-mcp-gateway-default-local")
                        && resource.getResourceUri().equals(
                        "https://api.egon.internal/local/identity/yuheng-mcp-gateway-default")
                        && resource.getBizCode().equals("identity")
                        && resource.getAppCode().equals("yuheng-mcp-gateway-default")
                        && resource.getEnvironment().equals("local")
                        && resource.getManagementClientId().equals("yuheng-mcp-gateway-service")
                        && resource.getRbacApplicationCode().equals("mock-backend")
                        && resource.getEntryPermissionCode().equals("mock:read")));
        verify(clients).create(argThat(command ->
                command.clientId().equals("yuheng-mcp-gateway-service")
                        && command.clientType() == IdentityClientEntity.ClientType.CONFIDENTIAL
                        && command.redirectUris().isEmpty()
                        && command.resourceUris().isEmpty()));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId()
                        .equals("identity-mock-backend-local")
                        && resource.getAppCode().equals("mock-backend")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("tianquan-shoubing-service")
                        && grant.getResourceServerId()
                        .equals("permission-tianquan-jianshen-local")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS
                        && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("tianquan-jianshen-service")
                        && grant.getResourceServerId()
                        .equals("permission-tianquan-jianshen-local")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("yuheng-mcp-gateway-service")
                        && grant.getResourceServerId().equals(
                        "identity-yuheng-test-mcp-provider-local")
                        && grant.getAllowedScopes().contains(
                        "mcp:operation:invoke")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("yuheng-mcp-gateway-service")
                        && grant.getResourceServerId().equals("platform-tianshu-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().equals("[\"tianshu:registration:write\"]")));
        verify(grants, never()).save(argThat(grant ->
                grant.getClientId().equals("yuheng-mcp-gateway-service")
                        && grant.getResourceServerId().equals("platform-yuheng-admin-local")));
        verify(grants, never()).save(argThat(grant ->
                grant.getClientId().equals("yuheng-biz-gateway-service")
                        && grant.getResourceServerId().equals(
                        "identity-yuheng-test-mcp-provider-local")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("tianquan-shoubing-service")
                        && grant.getResourceServerId().equals(
                        "platform-tianshu-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.tianquan.shoubing.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "tianshu:registration:write")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("tianshu-service")
                        && grant.getResourceServerId().equals(
                        "platform-tianshu-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.tianquan.shoubing.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "tianshu:registration:write")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("yuheng-biz-gateway-service")
                        && grant.getResourceServerId().equals(
                        "permission-tianquan-shoubing-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.tianquan.shoubing.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "tianquan-shoubing:refresh-token:validate")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("yuheng-admin-service")
                        && grant.getResourceServerId().equals(
                        "permission-tianquan-jianshen-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.tianquan.shoubing.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "yuheng.openapi.read")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("yuheng-admin-service")
                        && grant.getResourceServerId().equals("platform-yuheng-admin-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().equals("[\"yuheng.openapi.read\"]")));
        for (String provider : java.util.List.of("permission-tianquan-shoubing-local", "platform-tianshu-local")) {
            verify(grants).save(argThat(grant ->
                    grant.getClientId().equals("yuheng-admin-service")
                            && grant.getResourceServerId().equals(provider)
                            && grant.getGrantContext()
                            == top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.PLATFORM
                            && grant.getTenantId() == null
                            && grant.getAllowedScopes().contains("yuheng.openapi.read")));
        }
        verify(grants, atLeastOnce()).save(any(
                IdentityClientResourceGrantEntity.class
        ));
        verify(projections, atLeastOnce()).projectResource(any(), any());
        verify(projections, atLeastOnce()).projectServiceGrant(any());
    }

    @Test
    void reconcilesOneExactRbac3ServiceGrantForEachConfiguredTenant()
            throws Exception {
        OAuthClientService clients = mock(OAuthClientService.class);
        IdentityResourceServerRepository resources =
                mock(IdentityResourceServerRepository.class);
        IdentityClientResourceGrantRepository grants =
                mock(IdentityClientResourceGrantRepository.class);
        IdentityClientRepository clientEntities =
                mock(IdentityClientRepository.class);
        ResourceServerProjectionService projections =
                mock(ResourceServerProjectionService.class);
        IdentityClientResourceGrantEntity existing =
                IdentityClientResourceGrantEntity.clientCredentials(
                        "dev-tianquan-jianshen-grant-tianquan-shoubing-service",
                        "tianquan-shoubing-service",
                        "permission-tianquan-jianshen-local",
                        "default",
                        "[\"service:identity:resolve\"]",
                        Instant.EPOCH
                );
        when(clients.list()).thenReturn(List.of());
        stubClientCreation(clients);
        when(clientEntities.findById(any())).thenReturn(Optional.of(
                machineClient("management-client")
        ));
        when(grants.findByClientIdAndGrantTypeAndStatus(
                "tianquan-shoubing-service",
                IdentityClientResourceGrantEntity.GrantType.CLIENT_CREDENTIALS,
                IdentityClientResourceGrantEntity.Status.ACTIVE
        )).thenReturn(List.of(existing));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(
                        clients,
                        resources,
                        grants,
                        clientEntities,
                        projections,
                        tenantBootstrap(),
                        secretDirectory.toString(),
                        "tenant-42,tenant-84"
                );

        bootstrap.afterSingletonsInstantiated();

        verify(grants).save(argThat(grant -> grant == existing
                && grant.getVersion() == 1L
                && "tenant-42".equals(grant.getTenantId())
                && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(grants).save(argThat(grant -> grant != existing
                && grant.getClientId().equals("tianquan-shoubing-service")
                && grant.getResourceServerId().equals(
                        "permission-tianquan-jianshen-local")
                && "tenant-84".equals(grant.getTenantId())
                && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(projections).projectServiceGrant(existing);
        verify(projections).projectServiceGrant(argThat(grant ->
                grant != existing
                        && grant.getClientId().equals("tianquan-shoubing-service")
                        && "tenant-84".equals(grant.getTenantId())));
        for (String tenantId : List.of("tenant-42", "tenant-84")) {
            verify(grants).save(argThat(grant ->
                    grant.getClientId().equals("yuheng-mcp-gateway-service")
                            && tenantId.equals(grant.getTenantId())
                            && grant.getResourceServerId().equals("permission-tianquan-jianshen-local")
                            && grant.getAllowedScopes().equals(
                            "[\"service:authorization:decide\",\"service:authorization:snapshot\",\"service:identity:resolve\"]")));
            verify(grants).save(argThat(grant ->
                    grant.getClientId().equals("yuheng-mcp-gateway-service")
                            && tenantId.equals(grant.getTenantId())
                            && grant.getResourceServerId().equals(
                            "identity-yuheng-test-mcp-provider-local")
                            && grant.getAllowedScopes().equals("[\"mcp:operation:invoke\"]")));
        }
    }

    @Test
    void replacesAnObsoleteRedirectUriOnAnExistingDevelopmentClient()
            throws Exception {
        OAuthClientService clients = mock(OAuthClientService.class);
        IdentityResourceServerRepository resources =
                mock(IdentityResourceServerRepository.class);
        IdentityClientResourceGrantRepository grants =
                mock(IdentityClientResourceGrantRepository.class);
        IdentityClientRepository clientEntities =
                mock(IdentityClientRepository.class);
        ResourceServerProjectionService projections =
                mock(ResourceServerProjectionService.class);
        when(clients.list()).thenReturn(List.of(new OAuthClientVO(
                "tianshu-admin-web", "Tianshu Admin Web", "PUBLIC", "ACTIVE", true,
                900, 604800,
                List.of("http://127.0.0.1:18151/oauth/callback"),
                List.of("tianshu-admin-web"), 0,
                java.time.Instant.EPOCH, java.time.Instant.EPOCH
        )));
        stubClientCreation(clients);
        when(clientEntities.findById(any())).thenReturn(Optional.of(
                machineClient("management-client")
        ));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(
                        clients,
                        resources,
                        grants,
                        clientEntities,
                        projections,
                        tenantBootstrap(),
                        secretDirectory.toString(),
                        "default"
                );

        bootstrap.afterSingletonsInstantiated();

        verify(clients).putRedirectUri(
                "tianshu-admin-web",
                "http://127.0.0.1:18152/oauth/callback"
        );
        verify(clients).deleteRedirectUri(
                "tianshu-admin-web",
                "http://127.0.0.1:18151/oauth/callback"
        );
    }

    @Test
    void preservesExistingGatewayIdentitiesAndSecrets() throws Exception {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        List<String> clientIds = List.of(
                "yuheng-admin-service", "yuheng-biz-gateway-service", "yuheng-mcp-gateway-service");
        when(bootstrapClients.list()).thenReturn(clientIds.stream()
                .map(clientId -> new OAuthClientVO(
                        clientId, clientId, "CONFIDENTIAL", "ACTIVE", true,
                        900, 604800, List.of(), List.of(), 0,
                        Instant.EPOCH, Instant.EPOCH, clientId, "test", "ACTIVE"))
                .toList());
        List<IdentityResourceServerEntity> existing = List.of(
                gatewayResource("platform-yuheng-admin-local", "xingyuan", "yuheng-admin",
                        "yuheng-admin-service"),
                gatewayResource("identity-yuheng-biz-gateway-default-local", "identity",
                        "yuheng-biz-gateway-default", "yuheng-biz-gateway-service"),
                gatewayResource("identity-yuheng-mcp-gateway-default-local", "identity",
                        "yuheng-mcp-gateway-default", "yuheng-mcp-gateway-service"));
        for (IdentityResourceServerEntity resource : existing) {
            when(bootstrapResources.findByResourceServerId(resource.getResourceServerId()))
                    .thenReturn(Optional.of(resource));
        }
        for (String clientId : clientIds) {
            Files.writeString(secretDirectory.resolve(clientId + ".secret"), "preserved-test-secret");
        }

        bootstrap.afterSingletonsInstantiated();
        bootstrap.afterSingletonsInstantiated();

        for (String clientId : clientIds) {
            verify(bootstrapClients, never()).create(argThat(command ->
                    command.clientId().equals(clientId)));
            assertThat(Files.readString(secretDirectory.resolve(clientId + ".secret")))
                    .isEqualTo("preserved-test-secret");
        }
        verify(bootstrapClients, never()).rotateSecret(any(), any());
        for (IdentityResourceServerEntity resource : existing) {
            verify(bootstrapResources, never()).save(resource);
            verify(bootstrapProjections, org.mockito.Mockito.times(2))
                    .projectResource(org.mockito.ArgumentMatchers.same(resource), any());
        }
    }

    @Test
    void rejectsMismatchedMcpResource() {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        for (IdentityResourceServerEntity mismatched : List.of(
                gatewayResource("identity-yuheng-mcp-gateway-default-local", "identity",
                        "yuheng-biz-gateway-default", "yuheng-mcp-gateway-service"),
                gatewayResource("identity-yuheng-mcp-gateway-default-local", "identity",
                        "yuheng-mcp-gateway-default", "yuheng-biz-gateway-service"))) {
            when(bootstrapResources.findByResourceServerId("identity-yuheng-mcp-gateway-default-local"))
                    .thenReturn(Optional.of(mismatched));

            assertThatThrownBy(bootstrap::afterSingletonsInstantiated)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("local Resource Server definition does not match");
            verify(bootstrapResources, never()).save(argThat(resource ->
                    resource.getResourceServerId().equals("identity-yuheng-mcp-gateway-default-local")));
        }
    }

    @Test
    void doesNotReuseLegacyMcpTaskGrantId() {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        String suffix = UUID.nameUUIDFromBytes("default".getBytes(
                java.nio.charset.StandardCharsets.UTF_8)).toString().substring(0, 8);
        IdentityClientResourceGrantEntity legacy =
                IdentityClientResourceGrantEntity.clientCredentials(
                        "dev-mcp-task-grant-" + suffix, "yuheng-biz-gateway-service",
                        "identity-yuheng-test-mcp-provider-local", "default",
                        "[\"mcp:operation:invoke\"]", Instant.EPOCH);
        when(bootstrapGrants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                "yuheng-biz-gateway-service", "identity-yuheng-test-mcp-provider-local",
                IdentityClientResourceGrantEntity.GrantType.CLIENT_CREDENTIALS, "default"))
                .thenReturn(Optional.of(legacy));

        bootstrap.afterSingletonsInstantiated();

        verify(bootstrapGrants).save(argThat(grant ->
                grant.getClientId().equals("yuheng-mcp-gateway-service")
                        && grant.getResourceServerId().equals(
                        "identity-yuheng-test-mcp-provider-local")
                        && grant.getId().equals("dev-mcp-engine-task-grant-" + suffix)
                        && !grant.getId().equals(legacy.getId())));
        verify(bootstrapGrants, never()).save(legacy);
        assertThat(legacy.getClientId()).isEqualTo("yuheng-biz-gateway-service");
        assertThat(legacy.getVersion()).isZero();
    }

    @Test
    void reusesExistingMcpGrants() {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        List<IdentityClientResourceGrantEntity> existing = List.of(
                IdentityClientResourceGrantEntity.clientCredentials(
                        "existing-mcp-rbac", "yuheng-mcp-gateway-service",
                        "permission-tianquan-jianshen-local", "default",
                        "[\"service:authorization:decide\",\"service:authorization:snapshot\",\"service:identity:resolve\"]",
                        Instant.EPOCH),
                IdentityClientResourceGrantEntity.clientCredentials(
                        "existing-mcp-task", "yuheng-mcp-gateway-service",
                        "identity-yuheng-test-mcp-provider-local", "default",
                        "[\"mcp:operation:invoke\"]", Instant.EPOCH),
                IdentityClientResourceGrantEntity.platformClientCredentials(
                        "existing-mcp-ddc", "yuheng-mcp-gateway-service",
                        "platform-tianshu-local", "[\"tianshu:registration:write\"]", Instant.EPOCH));
        for (IdentityClientResourceGrantEntity grant : existing) {
            when(bootstrapGrants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                    grant.getClientId(), grant.getResourceServerId(),
                    grant.getGrantType(), grant.getTenantId())).thenReturn(Optional.of(grant));
        }

        bootstrap.afterSingletonsInstantiated();
        bootstrap.afterSingletonsInstantiated();

        for (IdentityClientResourceGrantEntity grant : existing) {
            verify(bootstrapGrants, never()).save(grant);
            verify(bootstrapProjections, never()).projectServiceGrant(grant);
            assertThat(grant.getVersion()).isZero();
        }
    }

    @Test
    void constructorRetainsQualifiedDependenciesAndPropertyDefaults() throws Exception {
        var constructor = IdpDevelopmentClientBootstrap.class.getConstructor(
                OAuthClientService.class, IdentityResourceServerRepository.class,
                IdentityClientResourceGrantRepository.class, IdentityClientRepository.class,
                ResourceServerProjectionService.class, IdpDevelopmentTenantBootstrap.class,
                String.class, String.class);
        var parameters = constructor.getParameters();
        List<String> beanNames = List.of(
                new AnnotationBeanNameGenerator().generateBeanName(
                        new AnnotatedGenericBeanDefinition(OAuthClientServiceImpl.class),
                        new DefaultListableBeanFactory()),
                "identityResourceServerRepository",
                "identityClientResourceGrantRepository", "identityClientRepository",
                "resourceServerProjectionService");
        for (int index = 0; index < beanNames.size(); index++) {
            Qualifier qualifier = parameters[index].getAnnotation(Qualifier.class);
            assertThat(qualifier).isNotNull();
            assertThat(qualifier.value()).isEqualTo(beanNames.get(index));
        }
        assertThat(parameters[6].getAnnotation(Value.class).value()).isEqualTo(
                "${egon.tianquan-shoubing.development-bootstrap.key-directory:target/local-unified-xingyuan/secrets}");
        assertThat(parameters[7].getAnnotation(Value.class).value()).isEqualTo(
                "${egon.tianquan-shoubing.development-bootstrap.tianquan-jianshen-service-tenant-ids:default}");
        assertThat(IdpDevelopmentClientBootstrap.class.getAnnotation(Component.class).value())
                .isEqualTo("idpDevelopmentClientBootstrap");
    }

    @Test
    void rejectsInvalidTenantBeforeSideEffects() {
        for (String tenants : List.of("", " default", "default,", "default, tenant")) {
            assertThatThrownBy(() -> new IdpDevelopmentClientBootstrap(
                    bootstrapClients, bootstrapResources, bootstrapGrants,
                    bootstrapClientEntities, bootstrapProjections,
                    tenantBootstrap(),
                    secretDirectory.toString(), tenants).afterSingletonsInstantiated())
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(bootstrapClients, bootstrapResources, bootstrapGrants,
                bootstrapClientEntities, bootstrapProjections);
    }

    @Test
    void retainsDdcRegistrationAndOpenApiScopesOnTheSamePlatformGrant() {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        IdentityClientResourceGrantEntity existing = IdentityClientResourceGrantEntity.platformClientCredentials(
                "existing-tianshu-grant", "yuheng-admin-service", "platform-tianshu-local",
                "[\"tianshu:registration:write\"]", java.time.Instant.EPOCH);
        when(bootstrapGrants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                "yuheng-admin-service", "platform-tianshu-local",
                IdentityClientResourceGrantEntity.GrantType.CLIENT_CREDENTIALS, null))
                .thenReturn(Optional.of(existing));

        bootstrap.afterSingletonsInstantiated();

        assertThat(existing.getAllowedScopes())
                .isEqualTo("[\"tianshu:registration:write\",\"yuheng.openapi.read\"]");
        assertThat(existing.getVersion()).isEqualTo(1);
        verify(bootstrapGrants).save(existing);
    }

    private IdpDevelopmentClientBootstrap bootstrapFixture() {
        when(bootstrapClients.list()).thenReturn(List.of());
        stubClientCreation(bootstrapClients);
        when(bootstrapClientEntities.findById(any())).thenAnswer(invocation ->
                Optional.of(machineClient(invocation.getArgument(0))));
        return new IdpDevelopmentClientBootstrap(
                bootstrapClients, bootstrapResources, bootstrapGrants,
                bootstrapClientEntities, bootstrapProjections, tenantBootstrap(), secretDirectory.toString(), "default");
    }

    private IdpDevelopmentTenantBootstrap tenantBootstrap() {
        IdpDevelopmentTenantBootstrap bootstrap = mock(IdpDevelopmentTenantBootstrap.class);
        when(bootstrap.resolveTenantIds(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return bootstrap;
    }

    private static IdentityResourceServerEntity gatewayResource(
            String resourceServerId, String bizCode, String appCode, String clientId) {
        return IdentityResourceServerEntity.create(
                "existing-" + appCode, resourceServerId,
                "https://api.egon.internal/local/"
                        + ("xingyuan".equals(bizCode) ? "platform" : bizCode)
                        + "/" + appCode,
                bizCode, appCode, "local", appCode, clientId,
                "mock-backend", "mock:read", 300,
                IdentityResourceServerEntity.Status.ACTIVE, Instant.EPOCH);
    }

    private static OAuthClientVO client(String clientId) {
        return new OAuthClientVO(
                clientId, clientId, "PUBLIC", "ACTIVE", true,
                900, 604800, List.of(), List.of(), 0,
                java.time.Instant.EPOCH, java.time.Instant.EPOCH);
    }

    private static void stubClientCreation(OAuthClientService clients) {
        when(clients.create(any())).thenAnswer(invocation -> {
            var command = invocation.<top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO>
                    getArgument(0);
            return new CreatedOAuthClientVO(
                    command.clientId(),
                    command.appId(),
                    command.clientName(),
                    command.clientType().name(),
                    "ACTIVE",
                    command.clientType()
                            == IdentityClientEntity.ClientType.CONFIDENTIAL
                            ? "local-test-secret"
                            : null,
                    command.clientType()
                            == IdentityClientEntity.ClientType.CONFIDENTIAL
                            ? "cret"
                            : null,
                    0L,
                    Instant.EPOCH
            );
        });
    }

    private static IdentityClientEntity machineClient(String clientId) {
        return IdentityClientEntity.createConfidential(
                clientId,
                clientId,
                900,
                604_800,
                Instant.EPOCH
        );
    }
}
