package top.egon.cola.platform.idp.admin.support.bootstrap;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdpDevelopmentClientBootstrapTest {

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
        when(clientEntities.findById(any())).thenReturn(Optional.of(
                machineClient("management-client")
        ));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(
                        clients,
                        resources,
                        grants,
                        clientEntities,
                        projections
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
                        && resource.getAppCode()
                        .equals("gateway-engine-default")));
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
                grant.getClientId().equals("gateway-engine-service")
                        && grant.getResourceServerId().equals(
                        "identity-gateway-test-mcp-provider-local")
                        && grant.getAllowedScopes().contains(
                        "mcp:operation:invoke")
                        && grant.getGrantType()
                        == IdentityClientResourceGrantEntity.GrantType
                        .CLIENT_CREDENTIALS));
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
                        "tenant-42,tenant-84"
                );

        bootstrap.afterSingletonsInstantiated();

        verify(grants).save(argThat(grant -> grant == existing
                && grant.getVersion() == 1L
                && grant.getTenantId().equals("tenant-42")
                && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(grants).save(argThat(grant -> grant != existing
                && grant.getClientId().equals("idp-service")
                && grant.getResourceServerId().equals(
                        "permission-rbac3-local")
                && grant.getTenantId().equals("tenant-84")
                && grant.getAllowedScopes().contains(
                        "service:authorization:decide")));
        verify(projections).projectServiceGrant(existing);
        verify(projections).projectServiceGrant(argThat(grant ->
                grant != existing
                        && grant.getClientId().equals("idp-service")
                        && grant.getTenantId().equals("tenant-84")));
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
        when(clientEntities.findById(any())).thenReturn(Optional.of(
                machineClient("management-client")
        ));
        IdpDevelopmentClientBootstrap bootstrap =
                new IdpDevelopmentClientBootstrap(
                        clients,
                        resources,
                        grants,
                        clientEntities,
                        projections
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

    private static OAuthClientVO client(String clientId) {
        return new OAuthClientVO(
                clientId, clientId, "PUBLIC", "ACTIVE", true,
                900, 604800, List.of(), List.of(), 0,
                java.time.Instant.EPOCH, java.time.Instant.EPOCH);
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
