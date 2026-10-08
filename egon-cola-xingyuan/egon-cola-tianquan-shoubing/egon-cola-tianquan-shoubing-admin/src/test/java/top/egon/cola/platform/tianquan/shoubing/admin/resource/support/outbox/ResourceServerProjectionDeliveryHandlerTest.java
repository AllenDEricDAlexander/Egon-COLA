package top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServerProjectionDeliveryHandlerTest {

    private static final TrustedRealmBO PLATFORM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(1L)
            .trustVersion(1L)
            .build();
    private static final TrustedRealmBO TENANT = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example.test/tenants/7"))
            .realmKey("tenant:7")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(7L)
            .businessTenantId(7L)
            .trustVersion(1L)
            .build();

    private final OAuthResourceRepository resources = mock(OAuthResourceRepository.class);
    private final OAuthClientRepository clients = mock(OAuthClientRepository.class);
    private final OAuthGrantRepository grants = mock(OAuthGrantRepository.class);
    private final OAuthManagementRealmResolver realms = mock(OAuthManagementRealmResolver.class);
    private final ResourceServerProjectionService projections = mock(ResourceServerProjectionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ResourceServerProjectionDeliveryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ResourceServerProjectionDeliveryHandler(resources, clients, grants, realms,
                projections, objectMapper);
        when(realms.platformRealm()).thenReturn(PLATFORM);
        when(realms.resolveTenantRealm("7")).thenReturn(TENANT);
    }

    @Test
    void reloadsLatestPlatformResourceInsteadOfProjectingAnOlderSnapshot() throws Exception {
        OAuthResourceBO current = resource().setVersion(5L);
        OAuthClientBO management = managementClient("manager");
        when(resources.findByResourceServerId(PLATFORM, "resource-a")).thenReturn(Optional.of(current));
        when(clients.findByClientId(PLATFORM, "manager")).thenReturn(Optional.of(management));

        DeliveryResult result = handler.deliver(resourceEvent(3L));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        verify(projections).projectResource(current, management);
    }

    @Test
    void resolvesTenantAndProjectsTheCurrentServiceGrant() throws Exception {
        OAuthClientBO client = serviceClient("svc", 22L);
        OAuthResourceBO resource = resource();
        OAuthGrantBO current = grant(client.getId(), 5L, ClientResourceGrant.Status.ACTIVE);
        when(clients.findByClientId(TENANT, "svc")).thenReturn(Optional.of(client));
        when(resources.findByResourceServerId(PLATFORM, "resource-a")).thenReturn(Optional.of(resource));
        when(grants.findGrant(TENANT, client.getId(), "resource-a", ResourceGrantType.CLIENT_CREDENTIALS))
                .thenReturn(Optional.of(current));

        DeliveryResult result = handler.deliver(grantEvent("tenant:7", "7", 3L));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        verify(projections).projectServiceGrant(TENANT, client, resource, current);
    }

    @Test
    void olderGrantEventReloadsAndDeletesCurrentDisabledProjection() throws Exception {
        OAuthClientBO client = serviceClient("svc", 22L);
        OAuthResourceBO resource = resource();
        OAuthGrantBO current = grant(client.getId(), 5L, ClientResourceGrant.Status.DISABLED);
        when(clients.findByClientId(TENANT, "svc")).thenReturn(Optional.of(client));
        when(resources.findByResourceServerId(PLATFORM, "resource-a")).thenReturn(Optional.of(resource));
        when(grants.findGrant(TENANT, client.getId(), "resource-a", ResourceGrantType.CLIENT_CREDENTIALS))
                .thenReturn(Optional.of(current));

        DeliveryResult result = handler.deliver(grantEvent("tenant:7", "7", 2L));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        verify(projections).deleteServiceGrant(TENANT, client, resource, current);
    }

    @Test
    void rejectsRealmTenantMismatchPermanentlyBeforeRepositoryReads() throws Exception {
        DeliveryResult result = handler.deliver(grantEvent("tenant:8", "7", 1L));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(result.code()).isEqualTo("RESOURCE_PROJECTION_EVENT_INVALID");
        verify(clients, never()).findByClientId(eq(TENANT), eq("svc"));
        verify(projections, never()).projectServiceGrant(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retriesRedisProjectionDependencyFailure() throws Exception {
        OAuthResourceBO current = resource();
        OAuthClientBO management = managementClient("manager");
        when(resources.findByResourceServerId(PLATFORM, "resource-a")).thenReturn(Optional.of(current));
        when(clients.findByClientId(PLATFORM, "manager")).thenReturn(Optional.of(management));
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(projections).projectResource(current, management);

        DeliveryResult result = handler.deliver(resourceEvent(1L));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(result.code()).isEqualTo("RESOURCE_PROJECTION_DEPENDENCY_UNAVAILABLE");
    }

    private DeliveryContext resourceEvent(long eventVersion) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceServerId", "resource-a");
        payload.put("resourceVersion", eventVersion);
        return context(TransactionalOutboxResourceServerEventAdapter.RESOURCE_PROJECTION_DESTINATION,
                "IDENTITY_RESOURCE_SERVER", "resource-a", eventVersion, payload);
    }

    private DeliveryContext grantEvent(String realmKey, String tenantId, long eventVersion) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("realmKey", realmKey);
        payload.put("tenantId", tenantId);
        payload.put("clientId", "svc");
        payload.put("resourceServerId", "resource-a");
        payload.put("grantType", "CLIENT_CREDENTIALS");
        payload.put("grantVersion", eventVersion);
        return context(TransactionalOutboxResourceServerEventAdapter.SERVICE_GRANT_PROJECTION_DESTINATION,
                "IDENTITY_SERVICE_RESOURCE_GRANT", realmKey + ":svc:resource-a:CLIENT_CREDENTIALS", eventVersion,
                payload);
    }

    private DeliveryContext context(String destination, String aggregateType, String aggregateId, long version,
            Map<String, Object> payload) throws Exception {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", "event-" + version);
        envelope.put("eventType", destination);
        envelope.put("schemaVersion", 1);
        envelope.put("aggregateType", aggregateType);
        envelope.put("aggregateId", aggregateId);
        envelope.put("aggregateVersion", version);
        envelope.put("payload", payload);
        return new DeliveryContext("event-" + version,
                TransactionalOutboxResourceServerEventAdapter.PROJECTION_CHANNEL,
                destination, objectMapper.writeValueAsString(envelope), "application/json", "1",
                Map.of(), null, 1, 10, Instant.MAX);
    }

    private static OAuthResourceBO resource() {
        return OAuthResourceBO.builder().id(11L).version(1L).resourceServerId("resource-a")
                .resourceUri(URI.create("https://resource.example.test/a")).bizCode("biz").appCode("app")
                .environment("prod").displayName("Resource A").managementClientId("manager")
                .managementClientPk(12L).rbacApplicationCode("app").entryPermissionCode("app:entry")
                .status(ResourceServerStatus.ACTIVE).admissionTicketTtl(Duration.ofMinutes(5)).build();
    }

    private static OAuthClientBO managementClient(String clientId) {
        return OAuthClientBO.builder().id(12L).clientId(clientId).clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                .secretHash("$2a$12$placeholder").credentialVersion(0L)
                .redirectUris(List.of()).resourceUris(List.of()).allowedScopes(List.of()).build();
    }

    private static OAuthClientBO serviceClient(String clientId, long id) {
        return OAuthClientBO.builder().id(id).clientId(clientId).clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                .secretHash("$2a$12$placeholder").credentialVersion(0L)
                .redirectUris(List.of()).resourceUris(List.of()).allowedScopes(List.of()).build();
    }

    private static OAuthGrantBO grant(long clientPk, long version, ClientResourceGrant.Status status) {
        return OAuthGrantBO.builder().id(31L).version(version).clientPk(clientPk)
                .resourceServerId("resource-a").grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(List.of("resource:read")).status(status).resourceVersion(1L).build();
    }
}
