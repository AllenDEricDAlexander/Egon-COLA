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

    /** Legacy implementations remain source compatible and must explicitly implement the new capability. */
    default FieldRuleReplacementResultBO replaceFieldRules(@jakarta.validation.Valid FieldRuleReplacementBO command) {
        throw new top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation("ROLE_FIELD_RULE_WRITER_UNAVAILABLE");
    }

    record FieldRuleReplacementBO(
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long tenantId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long applicationId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long roleId,
            @jakarta.validation.constraints.Min(0) long expectedRoleVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max = 256) @jakarta.validation.Valid List<FieldRuleDesiredVO> rules,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 128)
            @jakarta.validation.constraints.Pattern(regexp = "[\\x21-\\x7E]{1,128}") String actorId,
            @jakarta.validation.constraints.NotNull Instant now) {
        public FieldRuleReplacementBO {
            tenantId = positive(tenantId, "tenantId"); applicationId = positive(applicationId, "applicationId"); roleId = positive(roleId, "roleId");
            rules = List.copyOf(Objects.requireNonNull(rules, "rules")); actorId = required(actorId, "actorId"); now = Objects.requireNonNull(now, "now");
            if (rules.size() > 256 || expectedRoleVersion < 0 || !actorId.matches("[\\x21-\\x7E]{1,128}")
                    || rules.stream().map(rule -> List.of(rule.permissionId(), rule.fieldDefinitionId(), rule.validFrom())).distinct().count() != rules.size()) {
                throw new IllegalArgumentException("Field rule replacement facts are invalid");
            }
        }
    }

    record FieldRuleDesiredVO(
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long permissionId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long fieldDefinitionId,
            @jakarta.validation.constraints.NotNull top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel accessLevel,
            @jakarta.validation.constraints.NotNull Instant validFrom, Instant validTo,
            @jakarta.validation.constraints.NotNull top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.enums.FieldRuleStatusEnum status) {
        public FieldRuleDesiredVO {
            permissionId = positive(permissionId, "permissionId"); fieldDefinitionId = positive(fieldDefinitionId, "fieldDefinitionId");
            Objects.requireNonNull(accessLevel, "accessLevel"); Objects.requireNonNull(validFrom, "validFrom"); Objects.requireNonNull(status, "status");
            if (validTo != null && !validTo.isAfter(validFrom)) { throw new IllegalArgumentException("Field rule window is invalid"); }
        }
    }

    record FieldRuleReplacementResultBO(long roleVersion, int ruleCount, int addedCount, int changedCount, int disabledCount) {
        public FieldRuleReplacementResultBO {
            if (roleVersion < 0 || ruleCount < 0 || ruleCount > 256 || addedCount < 0 || changedCount < 0 || disabledCount < 0) {
                throw new IllegalArgumentException("Field rule replacement result is invalid");
            }
        }
    }

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
