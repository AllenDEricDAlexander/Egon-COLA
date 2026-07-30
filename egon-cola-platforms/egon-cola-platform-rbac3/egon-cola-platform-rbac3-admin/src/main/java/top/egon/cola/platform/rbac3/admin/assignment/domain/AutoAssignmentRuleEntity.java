package top.egon.cola.platform.rbac3.admin.assignment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rbac3_auto_assignment_rule")
public class AutoAssignmentRuleEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "rule_code", nullable = false, length = 128)
    private String ruleCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 32)
    private MatchType matchType;
    @Column(name = "match_ref_id")
    private Long matchReferenceId;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;

    protected AutoAssignmentRuleEntity() {
    }

    public AutoAssignmentRuleEntity(
            Long id,
            Long tenantId,
            String ruleCode,
            MatchType matchType,
            Long matchReferenceId,
            Long roleId,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now
    ) {
        if (matchType == MatchType.ALL_ACTIVE_USERS && matchReferenceId != null
                || matchType == MatchType.POSITION && matchReferenceId == null
                || validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("invalid auto-assignment rule");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.ruleCode = required(ruleCode);
        this.matchType = Objects.requireNonNull(matchType, "matchType");
        this.matchReferenceId = matchReferenceId;
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ruleCode is required");
        }
        return value.trim();
    }

    public enum MatchType {
        ALL_ACTIVE_USERS,
        POSITION
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        EXPIRED
    }
}
