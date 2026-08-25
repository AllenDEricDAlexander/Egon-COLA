package top.egon.cola.platform.rbac3.admin.authorization.permission;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.dto.UpdateResourcePermissionMappingRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.domain.vo.ResourcePermissionMappingVO;
import top.egon.cola.platform.rbac3.admin.authorization.permission.repository.ResourcePermissionMappingRepository;
import top.egon.cola.platform.rbac3.admin.authorization.permission.service.ResourcePermissionMappingService;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourcePermissionMappingServiceTest {

    @Test
    void requestNormalizesReasonAndRejectsInvalidPermissionId() {
        UpdateResourcePermissionMappingRequestDTO request =
                new UpdateResourcePermissionMappingRequestDTO(" 701 ", 3L,
                        "  reviewed by admin  ");
        assertEquals("701", request.permissionId());
        assertEquals("reviewed by admin", request.reason());
        assertThrows(IllegalArgumentException.class,
                () -> new UpdateResourcePermissionMappingRequestDTO("permission.read", 0L, null));
    }

    @Test
    void mappingViewKeepsActualPermissionVisibleOnlyInAdminView() {
        ResourcePermissionMappingVO mapping = new ResourcePermissionMappingVO(
                "501", "71", "users.list", "API", "system:user:read",
                "701", "system:user:read", "CONFIGURED", 4L, 0L, false,
                2L, 5L);
        assertEquals("system:user:read", mapping.actualPermissionCode());
        assertEquals("CONFIGURED", mapping.mappingStatus());
    }

    @Test
    void sameMappingIsIdempotentAndInUseChangeIsRejected() {
        FakeRepository repository = new FakeRepository();
        ResourcePermissionMappingService service = new ResourcePermissionMappingService(repository);
        Instant now = Instant.parse("2026-08-25T00:00:00Z");

        ResourcePermissionMappingVO same = service.update("501",
                new UpdateResourcePermissionMappingRequestDTO("701", 4L, null),
                "admin", now);
        assertEquals(false, same.changed());

        repository.activeRoleCount = 1L;
        assertThrows(Rbac3RuleViolation.class, () -> service.update("501",
                new UpdateResourcePermissionMappingRequestDTO("702", 4L, null),
                "admin", now));
    }

    private static final class FakeRepository implements ResourcePermissionMappingRepository {
        private MappingFacts resource = new MappingFacts(501L, 71L, "users.list", "API",
                "ACTIVE", 701L, "system:user:read", 4L);
        private long activeRoleCount;

        @Override
        public MappingFacts loadForUpdate(Long resourceId) {
            return resource;
        }

        @Override
        public PermissionFacts activePermission(Long permissionId) {
            return new PermissionFacts(permissionId, 71L,
                    permissionId == 701L ? "system:user:read" : "system:user:write", "ACTIVE");
        }

        @Override
        public long countActiveRoleUsage(MappingFacts resource, Instant now) {
            return activeRoleCount;
        }

        @Override
        public MappingFacts updateActualMapping(MappingFacts resource, Long permissionId,
                                                 String actorId, String reason, Instant now) {
            this.resource = new MappingFacts(resource.resourceId(), resource.applicationId(),
                    resource.resourceCode(), resource.resourceType(), resource.status(),
                    permissionId, resource.suggestedPermissionCode(), resource.version() + 1L);
            return this.resource;
        }
    }
}
