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
@Table(name = "rbac3_user_role_assignment")
public class UserRoleAssignmentEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 32)
    private AssignmentType assignmentType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;
    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;
    @Column(name = "source_id", nullable = false, length = 128)
    private String sourceId;
    @Column(length = 500)
    private String reason;
    @Column(name = "ticket_no", length = 128)
    private String ticketNo;

    protected UserRoleAssignmentEntity() {
    }

    public UserRoleAssignmentEntity(
            Long id,
            Long tenantId,
            Long userId,
            Long roleId,
            AssignmentType assignmentType,
            Instant validFrom,
            Instant validTo,
            String sourceType,
            String sourceId,
            String reason,
            String ticketNo,
            String actorId,
            Instant now
    ) {
        if (validTo != null && !validTo.isAfter(validFrom)
                || (assignmentType == AssignmentType.TEMPORARY
                || assignmentType == AssignmentType.EMERGENCY) && validTo == null) {
            throw new IllegalArgumentException("invalid assignment window");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.assignmentType = Objects.requireNonNull(assignmentType, "assignmentType");
        this.status = validFrom.isAfter(now) ? Status.PENDING : Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.sourceType = required(sourceType, "sourceType");
        this.sourceId = required(sourceId, "sourceId");
        this.reason = optional(reason);
        this.ticketNo = optional(ticketNo);
        markCreated(actorId, now);
    }

    public void revoke(String actorId, Instant now) {
        if (status != Status.ACTIVE && status != Status.SUSPENDED) {
            throw new IllegalStateException("assignment is not revocable");
        }
        status = Status.REVOKED;
        markUpdated(actorId, now);
    }

    public void suspend(String actorId, Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("assignment is not active");
        }
        status = Status.SUSPENDED;
        markUpdated(actorId, now);
    }

    public void resume(String actorId, Instant now) {
        if (status != Status.SUSPENDED
                || validTo != null && !validTo.isAfter(now)) {
            throw new IllegalStateException("assignment is not resumable");
        }
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    public void activate(String actorId, Instant now) {
        if (status != Status.PENDING || validFrom.isAfter(now)
                || validTo != null && !validTo.isAfter(now)) {
            throw new IllegalStateException("assignment is not activatable");
        }
        status = Status.ACTIVE;
        markUpdated(actorId, now);
    }

    public void expire(String actorId, Instant now) {
        if (status != Status.ACTIVE && status != Status.SUSPENDED
                || validTo == null || validTo.isAfter(now)) {
            throw new IllegalStateException("assignment is not expirable");
        }
        status = Status.EXPIRED;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public Status getStatus() {
        return status;
    }

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null ? null : required(value, "optional value");
    }

    public enum AssignmentType {
        AUTO,
        DIRECT,
        TEMPORARY,
        EMERGENCY
    }

    public enum Status {
        PENDING,
        ACTIVE,
        SUSPENDED,
        EXPIRED,
        REVOKED
    }
}
