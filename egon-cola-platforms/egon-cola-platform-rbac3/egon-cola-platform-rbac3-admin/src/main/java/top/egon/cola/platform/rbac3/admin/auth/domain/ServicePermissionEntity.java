package top.egon.cola.platform.rbac3.admin.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "rbac3_service_permission")
public class ServicePermissionEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "principal_id", nullable = false)
    private Long principalId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    protected ServicePermissionEntity() {
    }

    public ServicePermissionEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long principalId,
            Long permissionId,
            String applicationCode,
            Instant validFrom,
            Instant validTo,
            String actorId,
            Instant now) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.principalId = Objects.requireNonNull(principalId, "principalId");
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.applicationCode = required(applicationCode, "applicationCode");
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
}
