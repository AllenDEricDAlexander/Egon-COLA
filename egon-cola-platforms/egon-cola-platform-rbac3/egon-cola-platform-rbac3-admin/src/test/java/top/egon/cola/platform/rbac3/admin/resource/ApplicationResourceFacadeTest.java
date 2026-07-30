package top.egon.cola.platform.rbac3.admin.resource;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationResourceFacadeTest {

    @Test
    void tenantScopedQueriesAndArchiveUseTheApplicationPort() {
        RecordingStore store = new RecordingStore();
        ApplicationResourceFacade facade = new ApplicationResourceFacade(store);

        assertEquals(List.of(new ApplicationResourceFacade.ApplicationView(
                        "71001", "finance", "Finance", "ACTIVE", 2L)),
                facade.applications("10001"));
        assertEquals("81001", facade.manifest("10001", "81001").manifestId());
        assertEquals("ARCHIVED", facade.archive(
                "10001", "82001", 3L, "20001", Instant.EPOCH).status());
        assertEquals("10001", store.lastTenantId);
    }

    @Test
    void archiveRejectsNegativeExpectedVersionBeforeCallingTheStore() {
        RecordingStore store = new RecordingStore();
        ApplicationResourceFacade facade = new ApplicationResourceFacade(store);

        assertThrows(IllegalArgumentException.class,
                () -> facade.archive("10001", "82001", -1L, "20001", Instant.EPOCH));
        assertEquals(0, store.archiveCalls);
    }

    private static final class RecordingStore implements ApplicationResourceFacade.Store {

        private String lastTenantId;
        private int archiveCalls;

        @Override
        public List<ApplicationResourceFacade.ApplicationView> applications(String tenantId) {
            lastTenantId = tenantId;
            return List.of(new ApplicationResourceFacade.ApplicationView(
                    "71001", "finance", "Finance", "ACTIVE", 2L));
        }

        @Override
        public List<ApplicationResourceFacade.ResourceView> resources(
                String tenantId,
                String applicationId) {
            lastTenantId = tenantId;
            return List.of();
        }

        @Override
        public ApplicationResourceFacade.ManifestView manifest(
                String tenantId,
                String manifestId) {
            lastTenantId = tenantId;
            return new ApplicationResourceFacade.ManifestView(
                    "81001", "71001", "PENDING_VALIDATION", "sha256:test", 1L);
        }

        @Override
        public ApplicationResourceFacade.ManifestValidationView validation(
                String tenantId,
                String manifestId) {
            lastTenantId = tenantId;
            return new ApplicationResourceFacade.ManifestValidationView(
                    manifestId, true, List.of(), List.of());
        }

        @Override
        public ApplicationResourceFacade.ManifestImpactView impact(
                String tenantId,
                String manifestId) {
            lastTenantId = tenantId;
            return new ApplicationResourceFacade.ManifestImpactView(
                    manifestId, 0L, 0L, 0L, 0L, List.of());
        }

        @Override
        public ApplicationResourceFacade.ArchiveResult archive(
                String tenantId,
                String resourceId,
                long expectedVersion,
                String actorId,
                Instant now) {
            lastTenantId = tenantId;
            archiveCalls++;
            return new ApplicationResourceFacade.ArchiveResult(resourceId, "ARCHIVED", 4L);
        }
    }
}
