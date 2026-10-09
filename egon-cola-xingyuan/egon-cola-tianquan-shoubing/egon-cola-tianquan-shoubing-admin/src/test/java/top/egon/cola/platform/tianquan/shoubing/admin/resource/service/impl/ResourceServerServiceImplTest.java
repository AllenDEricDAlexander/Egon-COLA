package top.egon.cola.platform.tianquan.shoubing.admin.resource.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.OptimisticLockingFailureException;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ClientResourceGrantViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ResourceServerViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServerServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
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
    private final OAuthGrantRepository grants = mock(OAuthGrantRepository.class);
    private final OAuthClientRepository clients = mock(OAuthClientRepository.class);
    private final ResourceServerViewConverter resourceViews = mock(ResourceServerViewConverter.class);
    private final ClientResourceGrantViewConverter grantViews = mock(ClientResourceGrantViewConverter.class);
    private final TransactionalOutboxResourceServerEventAdapter events =
            mock(TransactionalOutboxResourceServerEventAdapter.class);
    private final jakarta.validation.ValidatorFactory validatorFactory =
            jakarta.validation.Validation.buildDefaultValidatorFactory();
    private ResourceServerServiceImpl service;

    @org.junit.jupiter.api.AfterEach
    void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        service = new ResourceServerServiceImpl(resources, grants, clients,
                resourceViews, grantViews, Clock.fixed(NOW, ZoneOffset.UTC), events, validatorFactory.getValidator());
        when(resourceViews.toTarget(any(OAuthResourceBO.class))).thenAnswer(invocation -> {
            OAuthResourceBO value = invocation.getArgument(0);
            return resourceView(value.getResourceServerId(), value.getAppCode(), value.getStatus(), value.getVersion());
        });
        when(grantViews.toTarget(any(OAuthGrantBO.class), any(TrustedRealmBO.class), any(OAuthClientBO.class)))
                .thenAnswer(invocation -> grantView(invocation.getArgument(0), invocation.getArgument(1)));
    }

    @Test
    void createsDisabledResourceInThePlatformPartition() {
        when(clients.findByClientId(PLATFORM, "tianquan-shoubing-service"))
                .thenReturn(Optional.of(managementClient("tianquan-shoubing-service", 11L)));
        when(resources.save(eq(PLATFORM), any(OAuthResourceBO.class), anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ResourceServerVO created = service.create(PLATFORM, createCommand());

        assertThat(created.getResourceServerId()).isEqualTo("permission-tianquan-shoubing-prod");
        assertThat(created.getStatus()).isEqualTo("DISABLED");
        verify(resources).save(eq(PLATFORM), any(OAuthResourceBO.class), eq(0L));
        verify(events).enqueueResourceProjection(any(OAuthResourceBO.class));
    }

    @Test
    void rejectsNonPlatformDirectoryAccessAndDuplicateResourceUri() {
        assertThatThrownBy(() -> service.list(TENANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OAUTH_PLATFORM_REALM_REQUIRED");
        when(resources.findByResourceUri(PLATFORM, URI.create(
                "https://api.egon.internal/prod/permission/tianquan-shoubing")))
                .thenReturn(Optional.of(resource("existing", "tianquan-shoubing", ResourceServerStatus.DISABLED)));

        assertThatThrownBy(() -> service.create(PLATFORM, createCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAUTH_RESOURCE_ALREADY_EXISTS");
        verify(resources, never()).save(any(), any(), anyLong());
    }

    @Test
    void listsStableResourceDirectoryAndFailsClosedOnOverflow() {
        OAuthResourceBO later = resource("z-resource", "zeta", ResourceServerStatus.ACTIVE);
        OAuthResourceBO earlier = resource("a-resource", "alpha", ResourceServerStatus.DISABLED);
        when(resources.listDirectory(PLATFORM, 10001)).thenReturn(List.of(later, earlier));

        assertThat(service.list(PLATFORM)).extracting(ResourceServerVO::getResourceServerId)
                .containsExactly("a-resource", "z-resource");

        List<OAuthResourceBO> overflow = java.util.stream.IntStream.range(0, 10001)
                .mapToObj(index -> resource("resource-" + index, "app-" + index, ResourceServerStatus.ACTIVE))
                .toList();
        when(resources.listDirectory(PLATFORM, 10001)).thenReturn(overflow);
        assertThatThrownBy(() -> service.list(PLATFORM))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OAUTH_RESOURCE_DIRECTORY_LIMIT_EXCEEDED");
    }

    @Test
    void statusMutationsUseExpectedVersionAndEnqueueDisableEvent() {
        OAuthResourceBO disabled = resource("permission-tianquan-shoubing-prod", "tianquan-shoubing",
                ResourceServerStatus.DISABLED).setVersion(0L);
        AtomicReference<OAuthResourceBO> current = new AtomicReference<>(disabled);
        when(resources.findByResourceServerIdForUpdate(PLATFORM, disabled.getResourceServerId()))
                .thenAnswer(invocation -> Optional.of(current.get()));
        when(clients.findByClientId(PLATFORM, disabled.getManagementClientId()))
                .thenReturn(Optional.of(managementClient(disabled.getManagementClientId(), 11L)));
        when(resources.save(eq(PLATFORM), any(OAuthResourceBO.class), anyLong()))
                .thenAnswer(invocation -> {
                    current.set(invocation.getArgument(1));
                    return current.get();
                });

        ResourceServerVO enabled = service.enable(PLATFORM, disabled.getResourceServerId(), new ResourceVersionDTO(0L));
        assertThat(enabled.getStatus()).isEqualTo("ACTIVE");
        assertThat(enabled.getVersion()).isEqualTo(1L);
        assertThatThrownBy(() -> service.disable(PLATFORM, disabled.getResourceServerId(), new ResourceVersionDTO(0L)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        service.disable(PLATFORM, disabled.getResourceServerId(), new ResourceVersionDTO(1L));
        verify(events).enqueueDisabled(any(OAuthResourceBO.class));
    }

    @Test
    void serviceGrantRequiresTenantContextAndProjectsOnlyServiceClientCredentials() {
        OAuthResourceBO resource = resource("permission-tianquan-jianshen-prod", "tianquan-jianshen",
                ResourceServerStatus.ACTIVE);
        OAuthClientBO client = serviceClient("tianquan-shoubing-service", 21L);
        when(clients.findByClientIdForUpdate(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(resources.findByResourceServerIdForUpdate(PLATFORM, resource.getResourceServerId()))
                .thenReturn(Optional.of(resource));
        when(grants.findGrantForUpdate(TENANT, client.getId(), resource.getResourceServerId(),
                ResourceGrantType.CLIENT_CREDENTIALS)).thenReturn(Optional.empty());
        when(grants.save(eq(TENANT), any(OAuthGrantBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ClientResourceGrantVO created = service.putGrant(TENANT, PLATFORM, client.getClientId(),
                resource.getResourceServerId(), UpsertClientResourceGrantDTO.builder()
                        .grantType(ResourceGrantType.CLIENT_CREDENTIALS).tenantId("7")
                        .allowedScopes(Set.of("orders:read")).expectedResourceVersion(0L).build());

        assertThat(created.getTenantId()).isEqualTo("7");
        verify(events).enqueueServiceGrantProjection(eq(TENANT), eq(client), any(OAuthGrantBO.class));
        assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(),
                resource.getResourceServerId(), UpsertClientResourceGrantDTO.builder()
                        .grantType(ResourceGrantType.CLIENT_CREDENTIALS).allowedScopes(Set.of())
                        .expectedResourceVersion(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OAUTH_SERVICE_GRANT_INVALID");
    }

    @Test
    void userDelegationGrantHasNoTenantOrServiceProjection() {
        OAuthResourceBO resource = resource("permission-tianquan-shoubing-prod", "tianquan-shoubing",
                ResourceServerStatus.ACTIVE);
        OAuthClientBO client = userClient("tianquan-shoubing-admin-web", 22L)
                .setAllowedScopes(List.of("openid", "orders:read", "orders:write"));
        when(clients.findByClientIdForUpdate(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(resources.findByResourceServerIdForUpdate(PLATFORM, resource.getResourceServerId()))
                .thenReturn(Optional.of(resource));
        when(grants.findGrantForUpdate(TENANT, client.getId(), resource.getResourceServerId(),
                ResourceGrantType.USER_DELEGATION)).thenReturn(Optional.empty());
        when(grants.save(eq(TENANT), any(OAuthGrantBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ClientResourceGrantVO result = service.putGrant(TENANT, PLATFORM, client.getClientId(),
                resource.getResourceServerId(), UpsertClientResourceGrantDTO.builder()
                        .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of("orders:write", "orders:read"))
                        .expectedResourceVersion(0L).build());

        assertThat(result.getTenantId()).isNull();
        assertThat(result.getAllowedScopes()).containsExactlyInAnyOrder("orders:read", "orders:write");
        verify(grants).save(eq(TENANT), org.mockito.ArgumentMatchers.argThat(value ->
                value.getAllowedScopes().equals(List.of("orders:read", "orders:write"))), eq(0L));
        verify(events, never()).enqueueServiceGrantProjection(any(), any(), any());
    }

    @Test
    void userGrantUpdateListBatchAndClearKeepExactScopesAndVersions() {
        OAuthResourceBO resource = resource("resource-user", "orders", ResourceServerStatus.ACTIVE);
        OAuthClientBO client = userClient("user-client", 22L)
                .setAllowedScopes(List.of("openid", "orders:read", "orders:write"));
        AtomicReference<OAuthGrantBO> current = new AtomicReference<>(OAuthGrantBO.builder()
                .id(77L).version(2L).clientPk(client.getId()).resourceServerId(resource.getResourceServerId())
                .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(List.of("orders:read", "orders:write"))
                .status(ClientResourceGrant.Status.ACTIVE).resourceVersion(0L).build());
        when(clients.findByClientIdForUpdate(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(clients.findByClientId(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(resources.findByResourceServerIdForUpdate(PLATFORM, resource.getResourceServerId()))
                .thenReturn(Optional.of(resource));
        when(resources.findByBizCodeAndEnvironmentAndAppCodeIn(PLATFORM, "permission", "prod", List.of("orders")))
                .thenReturn(List.of(resource));
        when(grants.findGrantForUpdate(TENANT, client.getId(), resource.getResourceServerId(),
                ResourceGrantType.USER_DELEGATION)).thenAnswer(call -> Optional.of(current.get()));
        when(grants.listByClient(TENANT, client.getId(), 65)).thenAnswer(call -> List.of(current.get()));
        when(grants.save(eq(TENANT), any(OAuthGrantBO.class), anyLong())).thenAnswer(call -> {
            current.set(call.getArgument(1));
            return current.get();
        });
        service.putGrant(TENANT, PLATFORM, client.getClientId(), resource.getResourceServerId(),
                UpsertClientResourceGrantDTO.builder().grantType(ResourceGrantType.USER_DELEGATION)
                        .allowedScopes(Set.of("orders:read")).expectedResourceVersion(0L)
                        .expectedGrantVersion(2L).build());
        assertThat(service.listGrants(TENANT, PLATFORM, client.getClientId()).get(0).getAllowedScopes())
                .containsExactly("orders:read");
        assertThat(current.get().getVersion()).isEqualTo(3L);
        assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(), resource.getResourceServerId(),
                UpsertClientResourceGrantDTO.builder().grantType(ResourceGrantType.USER_DELEGATION)
                        .allowedScopes(Set.of("orders:write")).expectedResourceVersion(0L)
                        .expectedGrantVersion(2L).build())).isInstanceOf(OptimisticLockingFailureException.class);
        service.batchGrants(TENANT, PLATFORM, client.getClientId(), BatchClientResourceGrantDTO.builder()
                .bizCode("permission").environment("prod").appCodes(List.of("orders"))
                .action(BatchClientResourceGrantDTO.Action.UPSERT).grantType(ResourceGrantType.USER_DELEGATION)
                .allowedScopes(Set.of("orders:write"))
                .expectedResourceVersions(Map.of("orders", 0L)).expectedGrantVersions(Map.of("orders", 3L)).build());
        assertThat(current.get().getAllowedScopes()).containsExactly("orders:write");
        service.putGrant(TENANT, PLATFORM, client.getClientId(), resource.getResourceServerId(),
                UpsertClientResourceGrantDTO.builder().grantType(ResourceGrantType.USER_DELEGATION)
                        .allowedScopes(Set.of()).expectedResourceVersion(0L).expectedGrantVersion(4L).build());
        assertThat(current.get().getAllowedScopes()).isEmpty();
        verify(events, never()).enqueueServiceGrantProjection(any(), any(), any());
    }

    @Test
    void directUserGrantSchemaValidationRejectsOversizeMalformedNullAndNegativeInputs() {
        OAuthResourceBO resource = resource("resource-user", "orders", ResourceServerStatus.ACTIVE);
        OAuthClientBO client = userClient("user-client", 22L).setAllowedScopes(List.of("orders:read"));
        when(clients.findByClientIdForUpdate(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(resources.findByResourceServerIdForUpdate(PLATFORM, resource.getResourceServerId()))
                .thenReturn(Optional.of(resource));
        Set<String> oversized = java.util.stream.IntStream.range(0, 65)
                .mapToObj(index -> "orders:" + index).collect(java.util.stream.Collectors.toSet());
        for (Set<String> scopes : List.of(Set.of("*"), Set.of("orders/read"), Set.of("x".repeat(129)), oversized)) {
            assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(),
                    resource.getResourceServerId(), UpsertClientResourceGrantDTO.builder()
                            .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(scopes)
                            .expectedResourceVersion(0L).build()))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        }
        assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(),
                resource.getResourceServerId(), UpsertClientResourceGrantDTO.builder()
                        .grantType(ResourceGrantType.USER_DELEGATION).expectedResourceVersion(0L).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(),
                resource.getResourceServerId(), UpsertClientResourceGrantDTO.builder()
                        .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of("orders:read"))
                        .expectedResourceVersion(-1L).build()))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verify(grants, never()).save(any(), any(), anyLong());
    }

    @Test
    void userGrantCannotExceedClientOrContainIdentityScopesOrStaleResourceVersion() {
        OAuthResourceBO resource = resource("resource-user", "orders", ResourceServerStatus.ACTIVE);
        OAuthClientBO client = userClient("user-client", 22L).setAllowedScopes(List.of("openid", "orders:read"));
        when(clients.findByClientIdForUpdate(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(resources.findByResourceServerIdForUpdate(PLATFORM, resource.getResourceServerId()))
                .thenReturn(Optional.of(resource));
        for (String scope : List.of("orders:write", "openid", "*", "s".repeat(129))) {
            assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(), resource.getResourceServerId(),
                    UpsertClientResourceGrantDTO.builder().grantType(ResourceGrantType.USER_DELEGATION)
                            .allowedScopes(Set.of(scope)).expectedResourceVersion(0L).build()))
                    .isInstanceOf(RuntimeException.class);
        }
        assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(), resource.getResourceServerId(),
                UpsertClientResourceGrantDTO.builder().grantType(ResourceGrantType.USER_DELEGATION)
                        .tenantId("7").allowedScopes(Set.of("orders:read")).expectedResourceVersion(0L).build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.putGrant(TENANT, PLATFORM, client.getClientId(), resource.getResourceServerId(),
                UpsertClientResourceGrantDTO.builder().grantType(ResourceGrantType.USER_DELEGATION)
                        .allowedScopes(Set.of("orders:read")).expectedResourceVersion(1L).build()))
                .isInstanceOf(OptimisticLockingFailureException.class);
        verify(grants, never()).save(any(), any(), anyLong());
    }

    @Test
    void userDelegationViewDoesNotInventAServiceGrantTenantId() {
        OAuthGrantBO grant = OAuthGrantBO.builder().id(1L).version(0L).clientPk(22L)
                .resourceServerId("resource-a").grantType(ResourceGrantType.USER_DELEGATION)
                .allowedScopes(List.of()).status(ClientResourceGrant.Status.ACTIVE).resourceVersion(0L).build();
        OAuthClientBO client = userClient("web-client", 22L);

        ClientResourceGrantVO view = Mappers.getMapper(ClientResourceGrantViewConverter.class)
                .toTarget(grant, TENANT, client);

        assertThat(view.getTenantId()).isNull();
        assertThat(view.getAllowedScopes()).isEmpty();
    }

    @Test
    void batchStatusExpandsOnlyExplicitApplicationCodes() {
        OAuthResourceBO first = resource("resource-idp", "tianquan-shoubing", ResourceServerStatus.DISABLED);
        OAuthResourceBO second = resource("resource-jianshen", "tianquan-jianshen", ResourceServerStatus.DISABLED);
        when(resources.findByBizCodeAndEnvironmentAndAppCodeIn(PLATFORM, "permission", "prod",
                List.of("tianquan-shoubing", "tianquan-jianshen"))).thenReturn(List.of(first, second));
        when(clients.findByClientId(PLATFORM, "manager-tianquan-shoubing"))
                .thenReturn(Optional.of(managementClient("manager-tianquan-shoubing", 11L)));
        when(clients.findByClientId(PLATFORM, "manager-tianquan-jianshen"))
                .thenReturn(Optional.of(managementClient("manager-tianquan-jianshen", 12L)));
        when(resources.save(eq(PLATFORM), any(OAuthResourceBO.class), any(Long.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<ResourceServerVO> results = service.batch(PLATFORM, new BatchResourceServerActionDTO(
                "permission", "prod", List.of("tianquan-shoubing", "tianquan-jianshen"),
                BatchResourceServerActionDTO.Action.ENABLE,
                Map.of("tianquan-shoubing", 0L, "tianquan-jianshen", 0L)));

        assertThat(results).extracting(ResourceServerVO::getAppCode)
                .containsExactlyInAnyOrder("tianquan-shoubing", "tianquan-jianshen");
        assertThat(results).allMatch(value -> value.getVersion() == 1L);
        verify(events, times(2)).enqueueResourceProjection(any(OAuthResourceBO.class));
        assertThatThrownBy(() -> service.batch(PLATFORM, new BatchResourceServerActionDTO(
                "permission", "prod", List.of("*"), BatchResourceServerActionDTO.Action.ENABLE, Map.of("*", 0L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAUTH_RESOURCE_BATCH_SELECTION_INVALID");
    }

    @Test
    void batchServiceGrantWritesEnqueueOneProjectionEventPerGrant() {
        OAuthResourceBO first = resource("resource-idp", "tianquan-shoubing", ResourceServerStatus.ACTIVE);
        OAuthResourceBO second = resource("resource-jianshen", "tianquan-jianshen", ResourceServerStatus.ACTIVE);
        OAuthClientBO client = serviceClient("caller-service", 61L);
        when(clients.findByClientIdForUpdate(TENANT, client.getClientId())).thenReturn(Optional.of(client));
        when(resources.findByBizCodeAndEnvironmentAndAppCodeIn(PLATFORM, "permission", "prod",
                List.of("tianquan-shoubing", "tianquan-jianshen"))).thenReturn(List.of(first, second));
        when(grants.findGrantForUpdate(eq(TENANT), eq(client.getId()), any(String.class),
                eq(ResourceGrantType.CLIENT_CREDENTIALS))).thenReturn(Optional.empty());
        when(grants.save(eq(TENANT), any(OAuthGrantBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<ClientResourceGrantVO> results = service.batchGrants(TENANT, PLATFORM, client.getClientId(),
                BatchClientResourceGrantDTO.builder()
                        .bizCode("permission")
                        .environment("prod")
                        .appCodes(List.of("tianquan-shoubing", "tianquan-jianshen"))
                        .action(BatchClientResourceGrantDTO.Action.UPSERT)
                        .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                        .tenantId("7")
                        .allowedScopes(Set.of("service:authorization:decide"))
                        .expectedResourceVersions(Map.of("tianquan-shoubing", 0L, "tianquan-jianshen", 0L))
                        .expectedGrantVersions(Map.of())
                        .build());

        assertThat(results).hasSize(2);
        verify(events, times(2)).enqueueServiceGrantProjection(eq(TENANT), eq(client), any(OAuthGrantBO.class));
    }

    @Test
    void grantListUsesBoundedQuery() {
        OAuthClientBO client = serviceClient("client", 31L);
        when(clients.findByClientId(TENANT, "client")).thenReturn(Optional.of(client));
        when(grants.listByClient(TENANT, 31L, 65)).thenReturn(List.of(grant(31L, "resource-z")));

        assertThat(service.listGrants(TENANT, PLATFORM, "client")).hasSize(1);
        verify(grants).listByClient(TENANT, 31L, 65);
    }

    @Test
    void grantCommandsDefensivelyCopyCollectionInputs() {
        Set<String> scopes = new java.util.HashSet<>(Set.of("orders:read"));
        UpsertClientResourceGrantDTO upsert = UpsertClientResourceGrantDTO.builder()
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .tenantId("7")
                .allowedScopes(scopes)
                .expectedResourceVersion(0L)
                .build();
        scopes.clear();
        assertThat(upsert.getAllowedScopes()).containsExactly("orders:read");
        assertThatThrownBy(() -> upsert.getAllowedScopes().add("orders:write"))
                .isInstanceOf(UnsupportedOperationException.class);

        List<String> appCodes = new ArrayList<>(List.of("orders"));
        Set<String> batchScopes = new java.util.HashSet<>(Set.of("orders:read"));
        Map<String, Long> versions = new HashMap<>(Map.of("orders", 0L));
        BatchClientResourceGrantDTO batch = BatchClientResourceGrantDTO.builder()
                .bizCode("orders")
                .environment("prod")
                .appCodes(appCodes)
                .action(BatchClientResourceGrantDTO.Action.UPSERT)
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .tenantId("7")
                .allowedScopes(batchScopes)
                .expectedResourceVersions(versions)
                .expectedGrantVersions(Map.of())
                .build();
        appCodes.clear();
        batchScopes.clear();
        versions.clear();
        assertThat(batch.getAppCodes()).containsExactly("orders");
        assertThat(batch.getAllowedScopes()).containsExactly("orders:read");
        assertThat(batch.getExpectedResourceVersions()).containsEntry("orders", 0L);
        assertThatThrownBy(() -> batch.getExpectedResourceVersions().put("other", 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static CreateResourceServerDTO createCommand() {
        return new CreateResourceServerDTO("permission-tianquan-shoubing-prod",
                "https://api.egon.internal/prod/permission/tianquan-shoubing", "permission", "tianquan-shoubing",
                "prod", "Tianquan-Shoubing Production", "tianquan-shoubing-service", "tianquan-shoubing",
                "tianquan-shoubing:access");
    }

    private static OAuthResourceBO resource(String id, String appCode, ResourceServerStatus status) {
        return OAuthResourceBO.builder().id(10L).version(0L).resourceServerId(id)
                .resourceUri(URI.create("https://api.egon.internal/prod/permission/" + appCode))
                .bizCode("permission").appCode(appCode).environment("prod").displayName(appCode)
                .managementClientId("manager-" + appCode).managementClientPk(11L)
                .rbacApplicationCode(appCode).entryPermissionCode(appCode + ":access")
                .status(status).admissionTicketTtl(Duration.ofMinutes(5)).createdAt(NOW).updatedAt(NOW).build();
    }

    private static OAuthClientBO managementClient(String clientId, long id) {
        return OAuthClientBO.builder().id(id).version(0L).clientId(clientId).clientName(clientId)
                .clientType(OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.SERVICE)
                .status(OAuthClient.Status.ACTIVE).secretHash("$2a$12$hash").secretHint("hash")
                .credentialVersion(0L).redirectUris(List.of()).resourceUris(List.of()).allowedScopes(List.of("scope:read"))
                .accessTtlSeconds(600).refreshTtlSeconds(0).build();
    }

    private static OAuthClientBO serviceClient(String clientId, long id) {
        return managementClient(clientId, id);
    }

    private static OAuthClientBO userClient(String clientId, long id) {
        return OAuthClientBO.builder().id(id).version(0L).clientId(clientId).clientName(clientId)
                .clientType(OAuthClient.ClientType.PUBLIC).purpose(OAuthClientPurposeEnum.USER)
                .status(OAuthClient.Status.ACTIVE).credentialVersion(0L).redirectUris(List.of("https://web.example/cb"))
                .resourceUris(List.of()).allowedScopes(List.of("openid")).accessTtlSeconds(600)
                .refreshTtlSeconds(0).build();
    }

    private static OAuthGrantBO grant(long clientPk, String resourceId) {
        return OAuthGrantBO.builder().id(1L).version(0L).clientPk(clientPk).resourceServerId(resourceId)
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS).allowedScopes(List.of("scope:read"))
                .status(ClientResourceGrant.Status.ACTIVE).resourceVersion(0L).build();
    }

    private static ResourceServerVO resourceView(String id, String appCode, ResourceServerStatus status, long version) {
        return ResourceServerVO.builder().resourceServerId(id).resourceUri("https://resource.example/" + appCode)
                .bizCode("permission").appCode(appCode).environment("prod").displayName(appCode)
                .managementClientId("manager-" + appCode).rbacApplicationCode(appCode)
                .entryPermissionCode(appCode + ":access").status(status.name()).version(version)
                .createdAt(NOW).updatedAt(NOW).build();
    }

    private static ClientResourceGrantVO grantView(OAuthGrantBO grant, TrustedRealmBO realm) {
        return ClientResourceGrantVO.builder().clientId("client").resourceServerId(grant.getResourceServerId())
                .grantType(grant.getGrantType().name())
                .tenantId(grant.getGrantType() == ResourceGrantType.USER_DELEGATION || realm.getBusinessTenantId() == null
                        ? null : realm.getBusinessTenantId().toString())
                .allowedScopes(Set.copyOf(grant.getAllowedScopes()))
                .status(grant.getStatus().name()).version(grant.getVersion()).build();
    }
}
