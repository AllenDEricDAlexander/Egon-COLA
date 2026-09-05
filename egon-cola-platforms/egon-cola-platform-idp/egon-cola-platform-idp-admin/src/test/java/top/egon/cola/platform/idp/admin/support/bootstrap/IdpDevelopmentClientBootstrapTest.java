package top.egon.cola.platform.idp.admin.support.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.oauth.service.impl.OAuthClientServiceImpl;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;

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
        when(clients.list()).thenReturn(List.of(client("idp-admin-web")));
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
                        secretDirectory.toString(),
                        "default"
                );

        bootstrap.afterSingletonsInstantiated();

        verify(clients, never()).create(argThat(command ->
                command.clientId().equals("idp-admin-web")));
        verify(clients).create(argThat(command ->
                command.clientId().equals("mock-backend")
                        && command.redirectUris().equals(List.of(
                        "http://127.0.0.1:18161/oauth/callback"))
                        && command.resourceUris().isEmpty()));
        verify(clients).create(argThat(command ->
                command.clientId().equals("idp-service")
                        && command.clientType()
                        == IdentityClientEntity.ClientType.CONFIDENTIAL
                        && command.redirectUris().isEmpty()
                        && command.resourceUris().isEmpty()));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("permission-idp-local")
                        && resource.getAppCode().equals("idp")
                        && resource.getManagementClientId()
                        .equals("idp-service")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("permission-rbac3-local")
                        && resource.getAppCode().equals("rbac3")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("platform-ddc-local")
                        && resource.getBizCode().equals("platform")
                        && resource.getAppCode().equals("ddc")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId()
                        .equals("identity-gateway-engine-default-local")
                        && resource.getResourceUri().equals(
                        "https://api.egon.internal/local/identity/gateway-engine-default")
                        && resource.getBizCode().equals("identity")
                        && resource.getEnvironment().equals("local")
                        && resource.getManagementClientId().equals("gateway-engine-service")
                        && resource.getAppCode()
                        .equals("gateway-engine-default")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("platform-gateway-admin-local")
                        && resource.getResourceUri().equals(
                        "https://api.egon.internal/local/platform/gateway-admin")
                        && resource.getBizCode().equals("platform")
                        && resource.getAppCode().equals("gateway-admin")
                        && resource.getEnvironment().equals("local")
                        && resource.getManagementClientId().equals("gateway-admin-service")));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId().equals("identity-gateway-mcp-engine-default-local")
                        && resource.getResourceUri().equals(
                        "https://api.egon.internal/local/identity/gateway-mcp-engine-default")
                        && resource.getBizCode().equals("identity")
                        && resource.getAppCode().equals("gateway-mcp-engine-default")
                        && resource.getEnvironment().equals("local")
                        && resource.getManagementClientId().equals("gateway-mcp-engine-service")
                        && resource.getRbacApplicationCode().equals("mock-backend")
                        && resource.getEntryPermissionCode().equals("mock:read")));
        verify(clients).create(argThat(command ->
                command.clientId().equals("gateway-mcp-engine-service")
                        && command.clientType() == IdentityClientEntity.ClientType.CONFIDENTIAL
                        && command.redirectUris().isEmpty()
                        && command.resourceUris().isEmpty()));
        verify(resources).save(argThat(resource ->
                resource.getResourceServerId()
                        .equals("identity-mock-backend-local")
                        && resource.getAppCode().equals("mock-backend")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("idp-service")
                        && grant.getResourceServerId()
                        .equals("permission-rbac3-local")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS
                        && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("rbac3-service")
                        && grant.getResourceServerId()
                        .equals("permission-rbac3-local")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("gateway-mcp-engine-service")
                        && grant.getResourceServerId().equals(
                        "identity-gateway-test-mcp-provider-local")
                        && grant.getAllowedScopes().contains(
                        "mcp:operation:invoke")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("gateway-mcp-engine-service")
                        && grant.getResourceServerId().equals("platform-ddc-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.idp.contract.ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().equals("[\"ddc:registration:write\"]")));
        verify(grants, never()).save(argThat(grant ->
                grant.getClientId().equals("gateway-mcp-engine-service")
                        && grant.getResourceServerId().equals("platform-gateway-admin-local")));
        verify(grants, never()).save(argThat(grant ->
                grant.getClientId().equals("gateway-engine-service")
                        && grant.getResourceServerId().equals(
                        "identity-gateway-test-mcp-provider-local")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("idp-service")
                        && grant.getResourceServerId().equals(
                        "platform-ddc-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.idp.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "ddc:registration:write")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("ddc-service")
                        && grant.getResourceServerId().equals(
                        "platform-ddc-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.idp.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "ddc:registration:write")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("gateway-engine-service")
                        && grant.getResourceServerId().equals(
                        "permission-idp-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.idp.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "idp:refresh-token:validate")));
        verify(grants).save(argThat(grant ->
                grant.getClientId().equals("gateway-admin-service")
                        && grant.getResourceServerId().equals(
                        "permission-rbac3-local")
                        && grant.getGrantContext()
                        == top.egon.cola.platform.idp.contract
                        .ServiceTokenContext.PLATFORM
                        && grant.getTenantId() == null
                        && grant.getAllowedScopes().contains(
                        "gateway.openapi.read")));
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
                        "dev-rbac3-grant-idp-service",
                        "idp-service",
                        "permission-rbac3-local",
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
                "idp-service",
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
                && grant.getClientId().equals("idp-service")
                && grant.getResourceServerId().equals(
                        "permission-rbac3-local")
                && "tenant-84".equals(grant.getTenantId())
                && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(projections).projectServiceGrant(existing);
        verify(projections).projectServiceGrant(argThat(grant ->
                grant != existing
                        && grant.getClientId().equals("idp-service")
                        && "tenant-84".equals(grant.getTenantId())));
        for (String tenantId : List.of("tenant-42", "tenant-84")) {
            verify(grants).save(argThat(grant ->
                    grant.getClientId().equals("gateway-mcp-engine-service")
                            && tenantId.equals(grant.getTenantId())
                            && grant.getResourceServerId().equals("permission-rbac3-local")
                            && grant.getAllowedScopes().equals(
                            "[\"service:authorization:decide\",\"service:authorization:snapshot\",\"service:identity:resolve\"]")));
            verify(grants).save(argThat(grant ->
                    grant.getClientId().equals("gateway-mcp-engine-service")
                            && tenantId.equals(grant.getTenantId())
                            && grant.getResourceServerId().equals(
                            "identity-gateway-test-mcp-provider-local")
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
                "ddc-admin-web", "DDC Admin Web", "PUBLIC", "ACTIVE", true,
                900, 604800,
                List.of("http://127.0.0.1:18151/oauth/callback"),
                List.of("ddc-admin-web"), 0,
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
                        secretDirectory.toString(),
                        "default"
                );

        bootstrap.afterSingletonsInstantiated();

        verify(clients).putRedirectUri(
                "ddc-admin-web",
                "http://127.0.0.1:18152/oauth/callback"
        );
        verify(clients).deleteRedirectUri(
                "ddc-admin-web",
                "http://127.0.0.1:18151/oauth/callback"
        );
    }

    @Test
    void preservesExistingGatewayIdentitiesAndSecrets() throws Exception {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        List<String> clientIds = List.of(
                "gateway-admin-service", "gateway-engine-service", "gateway-mcp-engine-service");
        when(bootstrapClients.list()).thenReturn(clientIds.stream()
                .map(clientId -> new OAuthClientVO(
                        clientId, clientId, "CONFIDENTIAL", "ACTIVE", true,
                        900, 604800, List.of(), List.of(), 0,
                        Instant.EPOCH, Instant.EPOCH, clientId, "test", "ACTIVE"))
                .toList());
        List<IdentityResourceServerEntity> existing = List.of(
                gatewayResource("platform-gateway-admin-local", "platform", "gateway-admin",
                        "gateway-admin-service"),
                gatewayResource("identity-gateway-engine-default-local", "identity",
                        "gateway-engine-default", "gateway-engine-service"),
                gatewayResource("identity-gateway-mcp-engine-default-local", "identity",
                        "gateway-mcp-engine-default", "gateway-mcp-engine-service"));
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
                gatewayResource("identity-gateway-mcp-engine-default-local", "identity",
                        "gateway-engine-default", "gateway-mcp-engine-service"),
                gatewayResource("identity-gateway-mcp-engine-default-local", "identity",
                        "gateway-mcp-engine-default", "gateway-engine-service"))) {
            when(bootstrapResources.findByResourceServerId("identity-gateway-mcp-engine-default-local"))
                    .thenReturn(Optional.of(mismatched));

            assertThatThrownBy(bootstrap::afterSingletonsInstantiated)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("local Resource Server definition does not match");
            verify(bootstrapResources, never()).save(argThat(resource ->
                    resource.getResourceServerId().equals("identity-gateway-mcp-engine-default-local")));
        }
    }

    @Test
    void doesNotReuseLegacyMcpTaskGrantId() {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        String suffix = UUID.nameUUIDFromBytes("default".getBytes(
                java.nio.charset.StandardCharsets.UTF_8)).toString().substring(0, 8);
        IdentityClientResourceGrantEntity legacy =
                IdentityClientResourceGrantEntity.clientCredentials(
                        "dev-mcp-task-grant-" + suffix, "gateway-engine-service",
                        "identity-gateway-test-mcp-provider-local", "default",
                        "[\"mcp:operation:invoke\"]", Instant.EPOCH);
        when(bootstrapGrants.findByClientIdAndResourceServerIdAndGrantTypeAndTenantId(
                "gateway-engine-service", "identity-gateway-test-mcp-provider-local",
                IdentityClientResourceGrantEntity.GrantType.CLIENT_CREDENTIALS, "default"))
                .thenReturn(Optional.of(legacy));

        bootstrap.afterSingletonsInstantiated();

        verify(bootstrapGrants).save(argThat(grant ->
                grant.getClientId().equals("gateway-mcp-engine-service")
                        && grant.getResourceServerId().equals(
                        "identity-gateway-test-mcp-provider-local")
                        && grant.getId().equals("dev-mcp-engine-task-grant-" + suffix)
                        && !grant.getId().equals(legacy.getId())));
        verify(bootstrapGrants, never()).save(legacy);
        assertThat(legacy.getClientId()).isEqualTo("gateway-engine-service");
        assertThat(legacy.getVersion()).isZero();
    }

    @Test
    void reusesExistingMcpGrants() {
        IdpDevelopmentClientBootstrap bootstrap = bootstrapFixture();
        List<IdentityClientResourceGrantEntity> existing = List.of(
                IdentityClientResourceGrantEntity.clientCredentials(
                        "existing-mcp-rbac", "gateway-mcp-engine-service",
                        "permission-rbac3-local", "default",
                        "[\"service:authorization:decide\",\"service:authorization:snapshot\",\"service:identity:resolve\"]",
                        Instant.EPOCH),
                IdentityClientResourceGrantEntity.clientCredentials(
                        "existing-mcp-task", "gateway-mcp-engine-service",
                        "identity-gateway-test-mcp-provider-local", "default",
                        "[\"mcp:operation:invoke\"]", Instant.EPOCH),
                IdentityClientResourceGrantEntity.platformClientCredentials(
                        "existing-mcp-ddc", "gateway-mcp-engine-service",
                        "platform-ddc-local", "[\"ddc:registration:write\"]", Instant.EPOCH));
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
                ResourceServerProjectionService.class, String.class, String.class);
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
        assertThat(parameters[5].getAnnotation(Value.class).value()).isEqualTo(
                "${egon.idp.development-bootstrap.key-directory:target/local-unified-platform/secrets}");
        assertThat(parameters[6].getAnnotation(Value.class).value()).isEqualTo(
                "${egon.idp.development-bootstrap.rbac3-service-tenant-ids:default}");
        assertThat(IdpDevelopmentClientBootstrap.class.getAnnotation(Component.class).value())
                .isEqualTo("idpDevelopmentClientBootstrap");
    }

    @Test
    void rejectsInvalidTenantBeforeSideEffects() {
        for (String tenants : List.of("", " default", "default,", "default, tenant")) {
            assertThatThrownBy(() -> new IdpDevelopmentClientBootstrap(
                    bootstrapClients, bootstrapResources, bootstrapGrants,
                    bootstrapClientEntities, bootstrapProjections,
                    secretDirectory.toString(), tenants).afterSingletonsInstantiated())
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(bootstrapClients, bootstrapResources, bootstrapGrants,
                bootstrapClientEntities, bootstrapProjections);
    }

    private IdpDevelopmentClientBootstrap bootstrapFixture() {
        when(bootstrapClients.list()).thenReturn(List.of());
        stubClientCreation(bootstrapClients);
        when(bootstrapClientEntities.findById(any())).thenAnswer(invocation ->
                Optional.of(machineClient(invocation.getArgument(0))));
        return new IdpDevelopmentClientBootstrap(
                bootstrapClients, bootstrapResources, bootstrapGrants,
                bootstrapClientEntities, bootstrapProjections, secretDirectory.toString(), "default");
    }

    private static IdentityResourceServerEntity gatewayResource(
            String resourceServerId, String bizCode, String appCode, String clientId) {
        return IdentityResourceServerEntity.create(
                "existing-" + appCode, resourceServerId,
                "https://api.egon.internal/local/" + bizCode + "/" + appCode,
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
            var command = invocation.<top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO>
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
