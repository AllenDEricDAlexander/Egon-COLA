package top.egon.cola.platform.rbac3.admin.resource;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.resource.service.ApplicationResourceFacade;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import top.egon.cola.platform.rbac3.admin.resource.repository.ApplicationResourceRepository;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ApplicationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ResourceVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestValidationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ManifestImpactVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ArchiveResultVO;

class ApplicationResourceFacadeTest {

    @Test
    void tenantScopedQueriesAndArchiveUseTheApplicationPort() {
        RecordingStore store = new RecordingStore();
        ApplicationResourceFacade facade = new ApplicationResourceFacade(store);

        assertEquals(List.of(new ApplicationVO(
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

    private static final class RecordingStore implements ApplicationResourceRepository {

        private String lastTenantId;
        private int archiveCalls;

        @Override
        public List<ApplicationVO> applications(String tenantId) {
            lastTenantId = tenantId;
            return List.of(new ApplicationVO(
                    "71001", "finance", "Finance", "ACTIVE", 2L));
        }

        @Override
        public List<ResourceVO> resources(
                String tenantId,
                String applicationId) {
            lastTenantId = tenantId;
            return List.of();
        }

        @Override
        public ManifestVO manifest(
                String tenantId,
                String manifestId) {
            lastTenantId = tenantId;
            return new ManifestVO(
                    "81001", "71001", "PENDING_VALIDATION", "sha256:test", 1L);
        }

        @Override
        public ManifestValidationVO validation(
                String tenantId,
                String manifestId) {
            lastTenantId = tenantId;
            return new ManifestValidationVO(
                    manifestId, true, List.of(), List.of());
        }

        @Override
        public ManifestImpactVO impact(
                String tenantId,
                String manifestId) {
            lastTenantId = tenantId;
            return new ManifestImpactVO(
                    manifestId, 0L, 0L, 0L, 0L, List.of());
        }

        @Override
        public ArchiveResultVO archive(
                String tenantId,
                String resourceId,
                long expectedVersion,
                String actorId,
                Instant now) {
            lastTenantId = tenantId;
            archiveCalls++;
            return new ArchiveResultVO(resourceId, "ARCHIVED", 4L);
        }
    }
}
