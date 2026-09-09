package top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.dto.UpdateResourcePermissionMappingRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.vo.ResourcePermissionMappingVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.repository.ResourcePermissionMappingRepository;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.Objects;

/** Coordinates the administrator-only actual permission mapping workflow. */
@Service
public class ResourcePermissionMappingService {

    private final ResourcePermissionMappingRepository repository;

    public ResourcePermissionMappingService(ResourcePermissionMappingRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Transactional(readOnly = true)
    public ResourcePermissionMappingVO get(String resourceId, Instant now) {
        ResourcePermissionMappingRepository.MappingFacts resource =
                repository.loadForUpdate(positive(resourceId, "resourceId"));
        return view(resource, null, repository.countActiveRoleUsage(resource, now), false);
    }

    @Transactional
    public ResourcePermissionMappingVO update(
            String resourceId,
            UpdateResourcePermissionMappingRequestDTO request,
            String actorId,
            Instant now) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(now, "now");
        ResourcePermissionMappingRepository.MappingFacts resource =
                repository.loadForUpdate(positive(resourceId, "resourceId"));
        if (resource.version() != request.expectedResourceVersion()) {
            throw new Rbac3RuleViolation("RESOURCE_VERSION_CONFLICT");
        }
        ResourcePermissionMappingRepository.PermissionFacts permission =
                repository.activePermission(Long.valueOf(request.permissionId()));
        if (!resource.applicationId().equals(permission.applicationId())) {
            throw new Rbac3RuleViolation("ROLE_APPLICATION_MISMATCH");
        }
        long activeRoleCount = repository.countActiveRoleUsage(resource, now);
        if (Objects.equals(resource.requiredPermissionId(), permission.permissionId())) {
            return view(resource, permission, activeRoleCount, false);
        }
        if (activeRoleCount > 0L) {
            throw new Rbac3RuleViolation("RESOURCE_PERMISSION_MAPPING_IN_USE");
        }
        ResourcePermissionMappingRepository.MappingFacts updated =
                repository.updateActualMapping(resource, permission.permissionId(), actorId,
                        request.reason(), now);
        return view(updated, permission, activeRoleCount, true);
    }

    private static ResourcePermissionMappingVO view(
            ResourcePermissionMappingRepository.MappingFacts resource,
            ResourcePermissionMappingRepository.PermissionFacts selected,
            long activeRoleCount,
            boolean changed) {
        Long actualId = resource.requiredPermissionId();
        String actualCode = selected == null ? null : selected.permissionCode();
        String actualPermissionId = actualId == null ? null : String.valueOf(actualId);
        if (selected == null && actualId != null) {
            actualPermissionId = String.valueOf(actualId);
        }
        String mappingStatus = actualId == null ? "UNCONFIGURED" : "CONFIGURED";
        return new ResourcePermissionMappingVO(
                String.valueOf(resource.resourceId()),
                String.valueOf(resource.applicationId()),
                resource.resourceCode(), resource.resourceType(),
                resource.suggestedPermissionCode(), actualPermissionId, actualCode,
                mappingStatus, resource.version(), activeRoleCount, changed, 0L, 0L);
    }

    private static Long positive(String value, String fieldName) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(fieldName + " must be a positive decimal id");
        }
        return Long.valueOf(value);
    }
}
