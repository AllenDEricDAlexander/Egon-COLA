package top.egon.cola.platform.tianquan.jianshen.admin.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Validation;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.dto.ResolvedPlatformBootstrapCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.domain.dto.ResolvedPlatformBootstrapCommandDTO.PlatformCatalogBindingDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.repository.DevelopmentBootstrapPort;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.repository.jpa.JpaDevelopmentTopologyBootstrapRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.application.domain.po.ApplicationPO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.BusinessCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Controlled persistence transport; the real owner enforces the initialization rules. */
class ResolvedPlatformBootstrapContractTest {
    private static final Instant NOW = Instant.parse("2026-10-14T00:00:00Z");
    private static final String ACTOR = "tianquan-jianshen-development-bootstrap";
    private static final List<String> CURRENT = List.of("tianquan-jianshen-admin", "tianquan-shoubing-admin", "yuheng-admin", "tianshu-admin");
    private static final List<String> LEGACY = List.of("rbac3-admin", "idp-admin", "gateway-admin", "ddc-admin");
    private static final List<String> OLD_APPS = List.of("rbac3", "idp", "gateway-admin", "ddc");
    private static final List<String> BUSINESSES = List.of("permission", "identity", "platform", "platform");

    @BeforeAll
    static void initializeExistingIdEngine() {
        top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator.initialize(0L, java.time.Duration.ofMillis(5));
    }

    @Test
    void freshCurrentApplicationsUseCatalogPublicIdsAndKeepHistoricalNames() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            assertDoesNotThrow(() -> fixture.owner.bindResolvedApplications(command));
            var inserted = ArgumentCaptor.forClass(Object.class);
            verify(fixture.manager, times(4)).persist(inserted.capture());
            var applications = inserted.getAllValues().stream().map(ApplicationPO.class::cast).toList();
            assertEquals(CURRENT, applications.stream().map(ApplicationPO::getApplicationCode).toList());
            for (int i = 0; i < 4; i++) {
                assertEquals(command.getApplications().get(i).getDdcApplicationId(), applications.get(i).getDdcApplicationId());
                assertEquals(command.getApplications().get(i).getDdcBusinessId(), applications.get(i).getDdcBusinessId());
            }
            assertTrue(fixture.updates.isEmpty());
            verifyNoInteractions(fixture.state);
        }
    }

    @Test
    void canonicalBindingPreservesTheInitialApplicationIdsAndPermissionOwnership() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            fixture.seedPlaceholders();
            assertDoesNotThrow(() -> fixture.owner.bindResolvedApplications(command));
            assertEquals(4, fixture.updates.size(), "reuse the system application instead of moving its permissions");
            verify(fixture.manager, never()).persist(any());
            for (int i = 0; i < 4; i++) {
                String code = CURRENT.get(i);
                var update = fixture.updates.stream().filter(value -> code.equals(value.get("canonicalApplicationCode"))).findFirst().orElseThrow();
                assertEquals(8000000000000000001L + i, update.get("applicationId"));
                assertEquals(LEGACY.get(i), update.get("applicationCode"));
                assertEquals(command.getApplications().get(i).getDdcApplicationId(), update.get("applicationPublicId"));
            }
        }
    }

    @Test
    void anyCatalogMismatchRejectsAllWritesBeforeTheDatabaseIsTouched() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            var last = command.getApplications().getLast();
            when(fixture.catalog.findApplication(last.getDdcApplicationId())).thenReturn(Optional.of(
                    new ApplicationCatalogEntry(last.getDdcApplicationId(), "999", last.getBusinessCode(), last.getApplicationCode(), "foreign", true, true)));
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            verifyNoInteractions(fixture.manager, fixture.state);
        }
    }

    @Test
    void aDisabledBusinessCannotBeConvertedIntoAnActiveLocalApplication() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            var first = command.getApplications().getFirst();
            when(fixture.catalog.findBusiness(first.getDdcBusinessId())).thenReturn(Optional.of(
                    new BusinessCatalogEntry(first.getDdcBusinessId(), first.getBusinessCode(), "disabled", false)));
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            verifyNoInteractions(fixture.manager, fixture.state);
        }
    }

    @Test
    void aUsedSecondSeedPreventsEvenTheFirstSeedFromBeingRebound() {
        try (var fixture = new Fixture()) {
            var command = command(true);
            fixture.acceptCatalog(command);
            fixture.seedPlaceholders();
            fixture.dependentApplicationId = 8000000000000000002L;
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            assertTrue(fixture.updates.isEmpty());
            verify(fixture.manager, never()).persist(any());
            verifyNoInteractions(fixture.state);
        }
    }

    @Test
    void onlyUnchangedKnownSystemSeedsCanBeBoundAndLostCasStopsTheOperation() {
        try (var fixture = new Fixture()) {
            var command = command(true);
            fixture.acceptCatalog(command);
            fixture.seedPlaceholders();
            fixture.casResult = 0;
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            assertEquals(1, fixture.updates.size());
            assertEquals("flyway-v10", fixture.updates.getFirst().get("seedActor"));
            assertEquals(0L, fixture.updates.getFirst().get("expectedVersion"));
            verify(fixture.manager, never()).persist(any());
            verifyNoInteractions(fixture.state);
        }
    }

    @Test
    void bindingFourUnusedSeedsUsesCasAndRefreshWithoutChangingTheirCodes() {
        try (var fixture = new Fixture()) {
            var command = command(true);
            fixture.acceptCatalog(command);
            fixture.seedPlaceholders();
            fixture.owner.bindResolvedApplications(command);
            assertEquals(4, fixture.updates.size());
            verify(fixture.manager, times(4)).refresh(any(ApplicationPO.class));
            assertEquals(LEGACY.stream().sorted().toList(), fixture.applications.values().stream().map(ApplicationPO::getApplicationCode).sorted().toList());
        }
    }

    @Test
    void exactCommittedBindingsResumeButDifferentBindingsOrForeignRowsAreRejected() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            for (int i = 0; i < 4; i++) {
                var binding = command.getApplications().get(i);
                fixture.applications.put(binding.getApplicationCode(), new ApplicationPO(900L + i, 17L,
                        binding.getDdcApplicationId(), binding.getDdcBusinessId(), binding.getApplicationCode(), "platform", i, ACTOR, NOW));
            }
            fixture.owner.bindResolvedApplications(command);
            assertTrue(fixture.updates.isEmpty());
            verify(fixture.manager, never()).persist(any());
            var first = command.getApplications().getFirst();
            first.setDdcApplicationId("123456789");
            fixture.acceptCatalog(command);
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            assertTrue(fixture.updates.isEmpty());
        }
    }

    @Test
    void aChangedInitialPermissionPreventsEveryCanonicalBinding() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            fixture.seedPlaceholders();
            fixture.changedPermissions = true;
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            assertTrue(fixture.updates.isEmpty());
            verify(fixture.manager, never()).persist(any());
        }
    }

    @Test
    void resourceAndCompositeForeignKeyBusinessRowsPreventTheBinding() {
        for (String dependency : List.of("rbac3_resource", "rbac3_business_participation", "rbac3_operation_sod_rule", "rbac3_service_permission", "rbac3_service_principal")) {
            try (var fixture = new Fixture()) {
                var command = command(false);
                fixture.acceptCatalog(command);
                fixture.seedPlaceholders();
                fixture.dependentTable = dependency;
                fixture.dependentApplicationId = 8000000000000000003L;
                assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
                assertTrue(fixture.updates.isEmpty());
                verify(fixture.manager, never()).persist(any());
            }
        }
    }

    @Test
    void aDuplicateCurrentApplicationCannotTakeTheInitialPermissions() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            fixture.seedPlaceholders();
            var first = command.getApplications().getFirst();
            fixture.applications.put(first.getApplicationCode(), new ApplicationPO(900L, 17L,
                    first.getDdcApplicationId(), first.getDdcBusinessId(), first.getApplicationCode(), "duplicate", 0, ACTOR, NOW));
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            assertTrue(fixture.updates.isEmpty());
            verify(fixture.manager, never()).persist(any());
        }
    }

    @Test
    void aRenamedOrForeignInitialRowCannotBeReplacedWithANewApplication() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            fixture.applications.put("another-code", new ApplicationPO(8000000000000000001L, 17L,
                    "rbac3", "permission", "another-code", "changed initial row", 0, "flyway-v10", NOW));
            assertThrows(IllegalStateException.class, () -> fixture.owner.bindResolvedApplications(command));
            assertTrue(fixture.updates.isEmpty());
            verify(fixture.manager, never()).persist(any());
        }
    }

    @Test
    void completeBindingsOnTheOriginalIdsResumeWithoutAnyUpdate() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            fixture.acceptCatalog(command);
            for (int i = 0; i < 4; i++) {
                var binding = command.getApplications().get(i);
                var application = new ApplicationPO(8000000000000000001L + i, 17L,
                        binding.getDdcApplicationId(), binding.getDdcBusinessId(), binding.getApplicationCode(),
                        "platform", i, "flyway-v10", NOW);
                application.markUpdated(ACTOR, NOW);
                fixture.applications.put(binding.getApplicationCode(), application);
            }
            assertDoesNotThrow(() -> fixture.owner.bindResolvedApplications(command));
            assertTrue(fixture.updates.isEmpty());
            verify(fixture.manager, never()).persist(any());
            verifyNoInteractions(fixture.state);
        }
    }

    @Test
    void duplicatePlatformSelectorsAndMissingCatalogDependencyFailClosed() {
        try (var fixture = new Fixture()) {
            var command = command(false);
            command.getApplications().set(3, command.getApplications().getFirst());
            assertThrows(IllegalArgumentException.class, () -> fixture.owner.bindResolvedApplications(command));
            verifyNoInteractions(fixture.manager, fixture.catalog);
            var legacyOwner = new JpaDevelopmentTopologyBootstrapRepository(fixture.manager, Clock.systemUTC(), fixture.state);
            assertThrows(IllegalStateException.class, () -> legacyOwner.bindResolvedApplications(command(false)));
            DevelopmentBootstrapPort oldPort = (tenant, subject) -> { };
            assertThrows(UnsupportedOperationException.class, () -> oldPort.bindResolvedApplications(command(false)));
        }
    }

    private static ResolvedPlatformBootstrapCommandDTO command(boolean legacy) {
        var bindings = new ArrayList<PlatformCatalogBindingDTO>();
        for (int i = 0; i < 4; i++) {
            bindings.add(PlatformCatalogBindingDTO.builder().applicationCode((legacy ? LEGACY : CURRENT).get(i))
                    .businessCode(BUSINESSES.get(i)).ddcApplicationId("1000" + i).ddcBusinessId("2000" + (i < 2 ? i : 2)).build());
        }
        return ResolvedPlatformBootstrapCommandDTO.builder().tenantId("17").identitySub("real-owner-subject").applications(bindings).build();
    }

    private static final class Fixture implements AutoCloseable {
        private final EntityManager manager = mock(EntityManager.class);
        private final TenantAuthorizationStateRepository state = mock(TenantAuthorizationStateRepository.class);
        private final DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
        private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
        private final JpaDevelopmentTopologyBootstrapRepository owner = new JpaDevelopmentTopologyBootstrapRepository(
                manager, Clock.fixed(NOW, ZoneOffset.UTC), state, catalog, new ValidationUtils(validators.getValidator()));
        private final Map<String, ApplicationPO> applications = new HashMap<>();
        private final List<Map<String, Object>> updates = new ArrayList<>();
        private long dependentApplicationId = -1;
        private String dependentTable = "rbac3_role";
        private boolean changedPermissions;
        private int casResult = 1;

        @SuppressWarnings("unchecked")
        private Fixture() {
            when(manager.createNativeQuery(anyString())).thenAnswer(call -> {
                String sql = call.getArgument(0);
                Query query = mock(Query.class);
                Map<String, Object> parameters = new HashMap<>();
                when(query.setParameter(anyString(), any())).thenAnswer(value -> {
                    parameters.put(value.getArgument(0), value.getArgument(1)); return query;
                });
                when(query.getSingleResult()).thenAnswer(ignored -> {
                    if (sql.contains(":seedPermissionCodes")) { return changedPermissions ? 1L : 0L; }
                    return sql.contains(dependentTable) && sql.contains("count")
                            && Long.valueOf(dependentApplicationId).equals(parameters.get("applicationId")) ? 1L : 0L;
                });
                when(query.executeUpdate()).thenAnswer(ignored -> {
                    if (sql.stripLeading().toLowerCase().startsWith("update rbac3_application")) {
                        updates.add(Map.copyOf(parameters)); return casResult;
                    }
                    return 0;
                });
                return query;
            });
            when(manager.find(eq(ApplicationPO.class), any())).thenAnswer(call ->
                    applications.values().stream().filter(value -> value.getId().equals(call.getArgument(1))).findFirst().orElse(null));
            when(manager.createQuery(anyString(), eq(ApplicationPO.class))).thenAnswer(call -> {
                TypedQuery<ApplicationPO> query = mock(TypedQuery.class);
                Map<String, Object> parameters = new HashMap<>();
                when(query.setParameter(anyString(), any())).thenAnswer(value -> {
                    parameters.put(value.getArgument(0), value.getArgument(1)); return query;
                });
                when(query.getResultList()).thenAnswer(ignored -> {
                    var value = applications.get(parameters.get("applicationCode"));
                    return value == null ? List.of() : List.of(value);
                });
                return query;
            });
        }

        private void acceptCatalog(ResolvedPlatformBootstrapCommandDTO command) {
            for (var binding : command.getApplications()) {
                when(catalog.findBusiness(binding.getDdcBusinessId())).thenReturn(Optional.of(new BusinessCatalogEntry(
                        binding.getDdcBusinessId(), binding.getBusinessCode(), "platform business", true)));
                when(catalog.findApplication(binding.getDdcApplicationId())).thenReturn(Optional.of(new ApplicationCatalogEntry(
                        binding.getDdcApplicationId(), binding.getDdcBusinessId(), binding.getBusinessCode(), binding.getApplicationCode(), "platform", true, true)));
            }
        }

        private void seedPlaceholders() {
            for (int i = 0; i < 4; i++) {
                applications.put(LEGACY.get(i), new ApplicationPO(8000000000000000001L + i, 17L,
                        OLD_APPS.get(i), BUSINESSES.get(i), LEGACY.get(i), "historical platform", i, "flyway-v10", NOW));
            }
        }

        @Override public void close() { validators.close(); }
    }
}
