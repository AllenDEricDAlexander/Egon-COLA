package top.egon.cola.platform.rbac3.admin.iam.position.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.enums.UserDirectoryAssignmentStatusEnum;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;

/** A tenant-scoped user-to-organization-to-position membership. */
@Entity(name = "UserPositionAssignmentEntity")
@Table(name = "rbac3_user_position_assignment")
public class UserPositionAssignmentPO extends TenantScopedPO {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "primary_assignment", nullable = false)
    private boolean primaryAssignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserDirectoryAssignmentStatusEnum status;

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

    protected UserPositionAssignmentPO() {
    }

    public UserPositionAssignmentPO(
            Long id,
            Long tenantId,
            Long userId,
            Long orgUnitId,
            Long positionId,
            boolean primaryAssignment,
            Instant validFrom,
            Instant validTo,
            String sourceType,
            String sourceId,
            String reason,
            String ticketNo,
            String actorId,
            Instant now) {
        validateWindow(validFrom, validTo);
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.userId = Objects.requireNonNull(userId, "userId");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.positionId = Objects.requireNonNull(positionId, "positionId");
        this.primaryAssignment = primaryAssignment;
        this.status = UserDirectoryAssignmentStatusEnum.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.sourceType = required(sourceType, "sourceType");
        this.sourceId = required(sourceId, "sourceId");
        this.reason = reason;
        this.ticketNo = ticketNo;
        markCreated(actorId, now);
    }

    public void suspend(String actorId, Instant now) {
        status = UserDirectoryAssignmentStatusEnum.SUSPENDED;
        markUpdated(actorId, now);
    }

    public void revoke(String actorId, Instant now) {
        status = UserDirectoryAssignmentStatusEnum.REVOKED;
        markUpdated(actorId, now);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getPositionId() { return positionId; }
    public boolean isPrimaryAssignment() { return primaryAssignment; }
    public UserDirectoryAssignmentStatusEnum getStatus() { return status; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo() { return validTo; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public String getReason() { return reason; }
    public String getTicketNo() { return ticketNo; }

    private static void validateWindow(Instant validFrom, Instant validTo) {
        Objects.requireNonNull(validFrom, "validFrom");
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
