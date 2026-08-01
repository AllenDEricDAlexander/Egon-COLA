package top.egon.cola.platform.rbac3.admin.constraint.domain;

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
@Table(name = "rbac3_role_prerequisite")
public class RolePrerequisiteEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "target_role_id", nullable = false)
    private Long targetRoleId;
    @Column(name = "group_code", nullable = false, length = 128)
    private String groupCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "match_mode", nullable = false, length = 32)
    private MatchMode matchMode;
    @Column(name = "prerequisite_role_id", nullable = false)
    private Long prerequisiteRoleId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    protected RolePrerequisiteEntity() {
    }

    public RolePrerequisiteEntity(
            Long id,
            Long tenantId,
            Long targetRoleId,
            String groupCode,
            MatchMode matchMode,
            Long prerequisiteRoleId,
            String actorId,
            Instant now) {
        if (targetRoleId.equals(prerequisiteRoleId)) {
            throw new IllegalArgumentException("prerequisite role must differ from target role");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.targetRoleId = Objects.requireNonNull(targetRoleId, "targetRoleId");
        this.groupCode = required(groupCode, "groupCode");
        this.matchMode = Objects.requireNonNull(matchMode, "matchMode");
        this.prerequisiteRoleId = Objects.requireNonNull(
                prerequisiteRoleId, "prerequisiteRoleId");
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public enum MatchMode {
        ALL_OF,
        ANY_OF
    }

    public enum Status {
        ACTIVE,
        DISABLED
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
