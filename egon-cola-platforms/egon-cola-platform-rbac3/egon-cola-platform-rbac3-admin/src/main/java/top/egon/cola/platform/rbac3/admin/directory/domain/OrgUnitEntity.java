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
@Table(name = "rbac3_org_unit")
public class OrgUnitEntity extends TenantScopedEntity {

    private static final int MAX_DEPTH = 20;

    @Id
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 32)
    private UnitType unitType;

    @Column(nullable = false, length = 128)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, columnDefinition = "text")
    private String path;

    @Column(nullable = false)
    private int depth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "external_id", length = 256)
    private String externalId;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    protected OrgUnitEntity() {
    }

    public OrgUnitEntity(
            Long id,
            Long tenantId,
            Long snapshotId,
            UnitType unitType,
            String code,
            String name,
            Long parentId,
            String path,
            int depth,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (depth < 0 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("depth must be between 0 and 20");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
        this.unitType = Objects.requireNonNull(unitType, "unitType");
        this.code = required(code, "code");
        this.name = required(name, "name");
        this.parentId = parentId;
        this.path = required(path, "path");
        this.depth = depth;
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

    public enum UnitType {
        ORG,
        DEPT
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }
}
