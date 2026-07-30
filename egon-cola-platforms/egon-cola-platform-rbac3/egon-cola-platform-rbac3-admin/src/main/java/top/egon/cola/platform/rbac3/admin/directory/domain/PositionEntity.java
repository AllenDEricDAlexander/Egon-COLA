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
@Table(name = "rbac3_position")
public class PositionEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(nullable = false, length = 128)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "external_id", length = 256)
    private String externalId;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    protected PositionEntity() {
    }

    public PositionEntity(
            Long id,
            Long tenantId,
            Long snapshotId,
            String code,
            String name,
            Long orgUnitId,
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
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.orgUnitId = Objects.requireNonNull(orgUnitId, "orgUnitId");
        this.status = Status.ACTIVE;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        markCreated(actorId, now);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }
}
