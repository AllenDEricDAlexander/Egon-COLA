package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository;

import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Persistence port for direct role-resource grants. */
public interface RoleResourceGrantRepository {

    GrantTreeFacts loadTree(Long tenantId, Long roleId, Instant now);

    ReplaceResult replace(ReplaceCommand command);

    Set<Long> activeDirectResourceIds(Set<Long> roleIds, Instant now);

    record GrantTreeFacts(
            Long applicationId,
            long roleVersion,
            List<RoleResourceGrantPO> directGrants,
            List<ResourceFact> resources) {
        public GrantTreeFacts {
            applicationId = positive(applicationId, "applicationId");
            if (roleVersion < 0L) {
                throw new IllegalArgumentException("roleVersion must not be negative");
            }
            directGrants = List.copyOf(Objects.requireNonNull(
                    directGrants, "directGrants"));
            resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        }
    }

    record ResourceFact(
            Long resourceId,
            String resourceCode,
            String resourceName,
            String category,
            String technicalType,
            String parentCode,
            String status,
            String mappingStatus,
            List<LinkedApi> linkedApis) {
        public ResourceFact {
            resourceId = positive(resourceId, "resourceId");
            resourceCode = required(resourceCode, "resourceCode");
            resourceName = required(resourceName, "resourceName");
            category = required(category, "category");
            technicalType = required(technicalType, "technicalType");
            status = required(status, "status");
            mappingStatus = required(mappingStatus, "mappingStatus");
            linkedApis = List.copyOf(Objects.requireNonNull(linkedApis, "linkedApis"));
        }
    }

    record LinkedApi(
            Long resourceId,
            String resourceCode,
            String resourceName,
            String method,
            String path) {
        public LinkedApi {
            resourceId = positive(resourceId, "resourceId");
            resourceCode = required(resourceCode, "resourceCode");
            resourceName = required(resourceName, "resourceName");
        }
    }

    record ReplaceCommand(
            Long tenantId,
            Long applicationId,
            Long roleId,
            Set<Long> resourceIds,
            Instant validFrom,
            Instant validTo,
            long expectedRoleVersion,
            String actorId) {
        public ReplaceCommand {
            tenantId = positive(tenantId, "tenantId");
            applicationId = positive(applicationId, "applicationId");
            roleId = positive(roleId, "roleId");
            resourceIds = Set.copyOf(Objects.requireNonNull(
                    resourceIds, "resourceIds"));
            validFrom = Objects.requireNonNull(validFrom, "validFrom");
            if (validTo != null && !validTo.isAfter(validFrom)) {
                throw new IllegalArgumentException("validTo must be after validFrom");
            }
            if (expectedRoleVersion < 0L) {
                throw new IllegalArgumentException(
                        "expectedRoleVersion must not be negative");
            }
            actorId = required(actorId, "actorId");
        }
    }

    record ReplaceResult(
            Set<Long> directResourceIds,
            long roleVersion,
            long addedCount,
            long removedCount) {
        public ReplaceResult {
            directResourceIds = Set.copyOf(Objects.requireNonNull(
                    directResourceIds, "directResourceIds"));
            if (roleVersion < 0L) {
                throw new IllegalArgumentException("roleVersion must not be negative");
            }
            if (addedCount < 0L || removedCount < 0L) {
                throw new IllegalArgumentException("mutation counts must not be negative");
            }
        }
    }

    private static Long positive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
