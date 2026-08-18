package top.egon.cola.platform.rbac3.admin.iam.application.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.enums.TenantApplicationStatusEnum;
import top.egon.cola.platform.rbac3.admin.shared.domain.po.TenantScopedPO;

import java.time.Instant;
import java.util.Objects;

/** Tenant entitlement for one global RBAC application catalog row. */
@Entity(name = "TenantApplicationEntity")
@Table(name = "rbac3_tenant_application")
public class TenantApplicationPO extends TenantScopedPO {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantApplicationStatusEnum status;

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

    protected TenantApplicationPO() {
    }

    public TenantApplicationPO(
            Long id,
            Long tenantId,
            Long applicationId,
            TenantApplicationStatusEnum status,
            Instant validFrom,
            Instant validTo,
            String sourceType,
            String sourceId,
            String reason,
            String ticketNo,
            String actorId,
            Instant now) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.status = Objects.requireNonNull(status, "status");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        this.sourceType = required(sourceType, "sourceType");
        this.sourceId = required(sourceId, "sourceId");
        this.reason = reason;
        this.ticketNo = ticketNo;
        markCreated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public TenantApplicationStatusEnum getStatus() {
        return status;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getReason() {
        return reason;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public boolean isEffectiveAt(Instant at) {
        Objects.requireNonNull(at, "at");
        return status == TenantApplicationStatusEnum.ACTIVE
                && !validFrom.isAfter(at)
                && (validTo == null || validTo.isAfter(at));
    }

    public boolean changeStatus(
            TenantApplicationStatusEnum nextStatus,
            long expectedVersion,
            String actorId,
            Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus");
        if (expectedVersion < 0L || getVersion() != expectedVersion) {
            throw new IllegalStateException("tenant application version conflict");
        }
        if (status == nextStatus) {
            return false;
        }
        status = nextStatus;
        markUpdated(actorId, now);
        return true;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
