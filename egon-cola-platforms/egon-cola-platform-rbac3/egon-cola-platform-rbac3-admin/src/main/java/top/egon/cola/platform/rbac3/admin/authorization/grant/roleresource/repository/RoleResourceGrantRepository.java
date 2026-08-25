package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository;

import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.po.RoleResourceGrantPO;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Persistence port for direct role-resource grants. */
public interface RoleResourceGrantRepository {

    GrantTreeFacts loadTree(Long tenantId, Long roleId, Instant now);

    ReplaceResult replace(ReplaceCommand command);

    Set<Long> activeDirectResourceIds(Set<Long> roleIds, Instant now);

    record GrantTreeFacts(List<RoleResourceGrantPO> directGrants) {
        public GrantTreeFacts {
            directGrants = List.copyOf(Objects.requireNonNull(
                    directGrants, "directGrants"));
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

    record ReplaceResult(Set<Long> directResourceIds, long roleVersion) {
        public ReplaceResult {
            directResourceIds = Set.copyOf(Objects.requireNonNull(
                    directResourceIds, "directResourceIds"));
            if (roleVersion < 0L) {
                throw new IllegalArgumentException("roleVersion must not be negative");
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
