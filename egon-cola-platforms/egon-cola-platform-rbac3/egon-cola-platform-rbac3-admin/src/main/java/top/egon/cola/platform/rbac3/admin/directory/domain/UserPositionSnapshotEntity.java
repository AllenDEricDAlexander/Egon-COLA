package top.egon.cola.platform.rbac3.admin.directory.domain;

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
@Table(name = "rbac3_user_position_snapshot")
public class UserPositionSnapshotEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "primary_flag", nullable = false)
    private boolean primary;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "external_assignment_id", length = 256)
    private String externalAssignmentId;

    protected UserPositionSnapshotEntity() {
    }

    public UserPositionSnapshotEntity(
            Long id,
            Long tenantId,
            Long snapshotId,
            Long userId,
            Long positionId,
            Long orgUnitId,
            boolean primary,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.positionId = Objects.requireNonNull(positionId, "positionId");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.primary = primary;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }
}
