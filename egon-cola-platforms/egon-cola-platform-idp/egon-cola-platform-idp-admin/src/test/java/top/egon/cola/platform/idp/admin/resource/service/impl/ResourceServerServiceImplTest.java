package top.egon.cola.platform.idp.admin.resource.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.oauth.repo.IdentityClientRepository;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityClientResourceGrantRepository;
import top.egon.cola.platform.idp.admin.resource.repo.IdentityResourceServerRepository;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.idp.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServerServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    private final IdentityResourceServerRepository resources =
            mock(IdentityResourceServerRepository.class);
    private final IdentityClientResourceGrantRepository grants =
            mock(IdentityClientResourceGrantRepository.class);
    private final IdentityClientRepository clients =
            mock(IdentityClientRepository.class);
    private final ResourceServerProjectionService projections =
            mock(ResourceServerProjectionService.class);
    private final TransactionalOutboxResourceServerEventAdapter events =
            mock(TransactionalOutboxResourceServerEventAdapter.class);
    private final AtomicLong ids = new AtomicLong(1000L);

    private ResourceServerServiceImpl service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        service = new ResourceServerServiceImpl(
                resources,
                grants,
                clients,
                projections,
                ids::incrementAndGet,
                objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC),
                events
        );
    }

    @Test
    void createsDisabledResourceAndProjectsIt() {
        IdentityClientEntity client = client("idp-service");
        when(clients.findById("idp-service")).thenReturn(Optional.of(client));
        when(resources.save(any())).thenAnswer(call -> call.getArgument(0));

        var created = service.create(createCommand());

        assertThat(created.resourceServerId()).isEqualTo("permission-idp-prod");
        assertThat(created.status()).isEqualTo("DISABLED");
        assertThat(created.status()).isEqualTo("DISABLED");
        verify(projections).projectResource(any(), any());
    }

    @Test
    void rejectsDuplicateUriOrApplicationTripleBeforeWriting() {
        when(resources.findByResourceUri(
                "https://api.egon.internal/prod/permission/idp"
        )).thenReturn(Optional.of(resource(
                "permission-existing-prod",
                "existing",
                "idp-service"
        )));

        assertThatThrownBy(() -> service.create(createCommand()))
                .isInstanceOf(IllegalStateException.class);
        verify(resources, never()).save(any());
    }

    @Test
    void rejectsManagementClientAlreadyBoundToAnotherResource() {
        IdentityClientEntity client = client("idp-service");
        when(clients.findById("idp-service")).thenReturn(Optional.of(client));
        when(resources.findByManagementClientId("idp-service"))
                .thenReturn(Optional.of(resource(
                        "permission-existing-prod",
                        "existing",
                        "idp-service"
                )));

        assertThatThrownBy(() -> service.create(createCommand()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already bound");
        verify(resources, never()).save(any());
    }

    @Test
    void listsAndLoadsResourceAdministrationViews() {
        IdentityResourceServerEntity resource = resource(
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        when(resources.findAll()).thenReturn(List.of(resource));
        when(resources.findByResourceServerId("permission-idp-prod"))
                .thenReturn(Optional.of(resource));
        assertThat(service.list()).extracting(ResourceServerVO::appCode)
                .containsExactly("idp");
        assertThat(service.detail("permission-idp-prod").resourceServerId())
                .isEqualTo("permission-idp-prod");
    }

    @Test
    void statusMutationsRequireCurrentVersions() {
        IdentityResourceServerEntity resource = resource(
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        IdentityClientEntity client = client("idp-service");
        when(resources.findByResourceServerId("permission-idp-prod"))
                .thenReturn(Optional.of(resource));
        when(clients.findById("idp-service")).thenReturn(Optional.of(client));
        var enabled = service.enable(
                "permission-idp-prod",
                new ResourceVersionDTO(0L)
        );

        assertThat(enabled.status()).isEqualTo("ACTIVE");
        assertThat(enabled.version()).isEqualTo(1L);
        assertThatThrownBy(() -> service.disable(
                "permission-idp-prod",
                new ResourceVersionDTO(0L)
        )).isInstanceOf(IllegalStateException.class);

    }

    @Test
    void disableEnqueuesExactResourceLifecycleEvent() {
        IdentityResourceServerEntity resource = resource(
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        when(resources.findByResourceServerId("permission-idp-prod"))
                .thenReturn(Optional.of(resource));
        when(clients.findById("idp-service"))
                .thenReturn(Optional.of(client("idp-service")));

        service.disable(
                "permission-idp-prod",
                new ResourceVersionDTO(0L)
        );

        verify(events).enqueueDisabled(resource);
        assertThat(resource.getVersion()).isEqualTo(1L);
    }

    @Test
    void serviceGrantRequiresTenantScopesAndProjectsWithoutRbac3() {
        IdentityResourceServerEntity resource = resource(
                "permission-rbac3-prod",
                "rbac3",
                "rbac3-service"
        );
        when(resources.findByResourceServerId("permission-rbac3-prod"))
                .thenReturn(Optional.of(resource));
        when(clients.existsById("idp-service")).thenReturn(true);
        when(grants.save(any())).thenAnswer(call -> call.getArgument(0));

        var grant = service.putGrant(
                "idp-service",
                "permission-rbac3-prod",
                new UpsertClientResourceGrantDTO(
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        "tenant-1",
                        Set.of("rbac3:policy:read"),
                        0L,
                        null
                )
        );

        assertThat(grant.tenantId()).isEqualTo("tenant-1");
        assertThat(grant.allowedScopes())
                .containsExactly("rbac3:policy:read");
        verify(projections).projectServiceGrant(any());

        assertThatThrownBy(() -> service.putGrant(
                "idp-service",
                "permission-rbac3-prod",
                new UpsertClientResourceGrantDTO(
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        null,
                        Set.of(),
                        1L,
                        null
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userDelegationGrantHasNoTenantOrServiceProjection() {
        IdentityResourceServerEntity resource = resource(
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        when(resources.findByResourceServerId("permission-idp-prod"))
                .thenReturn(Optional.of(resource));
        when(clients.existsById("idp-admin-web")).thenReturn(true);
        when(grants.save(any())).thenAnswer(call -> call.getArgument(0));

        var grant = service.putGrant(
                "idp-admin-web",
                "permission-idp-prod",
                new UpsertClientResourceGrantDTO(
                        ResourceGrantType.USER_DELEGATION,
                        null,
                        Set.of(),
                        0L,
                        null
                )
        );

        assertThat(grant.grantType()).isEqualTo("USER_DELEGATION");
        assertThat(grant.tenantId()).isNull();
        assertThat(grant.allowedScopes()).isEmpty();
        verify(projections, never()).projectServiceGrant(any());
    }

    @Test
    void batchStatusExpandsOnlyExplicitApplicationCodes() {
        IdentityResourceServerEntity idp = resource(
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        IdentityResourceServerEntity rbac3 = resource(
                "permission-rbac3-prod",
                "rbac3",
                "rbac3-service"
        );
        when(resources.findByBizCodeAndEnvironmentAndAppCodeIn(
                "permission",
                "prod",
                List.of("idp", "rbac3")
        )).thenReturn(List.of(idp, rbac3));
        when(clients.findById("idp-service"))
                .thenReturn(Optional.of(client("idp-service")));
        when(clients.findById("rbac3-service"))
                .thenReturn(Optional.of(client("rbac3-service")));
        var results = service.batch(new BatchResourceServerActionDTO(
                "permission",
                "prod",
                List.of("idp", "rbac3"),
                BatchResourceServerActionDTO.Action.ENABLE,
                Map.of("idp", 0L, "rbac3", 0L)
        ));

        assertThat(results).extracting(result -> result.appCode())
                .containsExactlyInAnyOrder("idp", "rbac3");
        assertThat(results).allMatch(result -> result.version() == 1L);
        verify(projections).projectResources(any());
        verify(projections, never()).projectResource(any(), any());
    }

    @Test
    void batchServiceGrantsUseOneAtomicProjectionCall() {
        IdentityResourceServerEntity idp = resource(
                "permission-idp-prod",
                "idp",
                "idp-service"
        );
        IdentityResourceServerEntity rbac3 = resource(
                "permission-rbac3-prod",
                "rbac3",
                "rbac3-service"
        );
        when(resources.findByBizCodeAndEnvironmentAndAppCodeIn(
                "permission",
                "prod",
                List.of("idp", "rbac3")
        )).thenReturn(List.of(idp, rbac3));
        when(clients.existsById("caller-service")).thenReturn(true);
        when(grants.save(any())).thenAnswer(call -> call.getArgument(0));

        var results = service.batchGrants(
                "caller-service",
                new BatchClientResourceGrantDTO(
                        "permission",
                        "prod",
                        List.of("idp", "rbac3"),
                        BatchClientResourceGrantDTO.Action.UPSERT,
                        ResourceGrantType.CLIENT_CREDENTIALS,
                        "tenant-1",
                        Set.of("resource:read"),
                        Map.of("idp", 0L, "rbac3", 0L),
                        Map.of()
                )
        );

        assertThat(results).hasSize(2);
        verify(projections).projectServiceGrants(any());
        verify(projections, never()).projectServiceGrant(any());
    }

    @Test
    void batchRejectsWildcardApplicationSelection() {
        assertThatThrownBy(() -> service.batch(
                new BatchResourceServerActionDTO(
                        "permission",
                        "prod",
                        List.of("*"),
                        BatchResourceServerActionDTO.Action.ENABLE,
                        Map.of("*", 0L)
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match exactly");

        verify(resources, never())
                .findByBizCodeAndEnvironmentAndAppCodeIn(any(), any(), any());
    }

    private static CreateResourceServerDTO createCommand() {
        return new CreateResourceServerDTO(
                "permission-idp-prod",
                "https://api.egon.internal/prod/permission/idp",
                "permission",
                "idp",
                "prod",
                "IdP Production",
                "idp-service",
                "idp",
                "idp:access"
        );
    }

    private static IdentityClientEntity client(String clientId) {
        return IdentityClientEntity.createPublic(
                clientId,
                clientId,
                900,
                604_800,
                NOW
        );
    }

    private static IdentityResourceServerEntity resource(
            String resourceServerId,
            String appCode,
            String managementClientId
    ) {
        return IdentityResourceServerEntity.create(
                "row-" + resourceServerId,
                resourceServerId,
                "https://api.egon.internal/prod/permission/" + appCode,
                "permission",
                appCode,
                "prod",
                appCode,
                managementClientId,
                appCode,
                appCode + ":access",
                300,
                IdentityResourceServerEntity.Status.DISABLED,
                NOW
        );
    }
}
