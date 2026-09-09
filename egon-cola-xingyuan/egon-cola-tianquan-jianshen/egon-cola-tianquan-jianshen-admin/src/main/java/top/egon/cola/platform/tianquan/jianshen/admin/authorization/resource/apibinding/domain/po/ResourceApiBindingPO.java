package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.domain.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.domain.enums.ResourceApiBindingStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.domain.po.GlobalAuditedPO;

import java.time.Instant;
import java.util.Objects;

/** Global mechanical relation from a ROUTE/ACTION resource to an API resource. */
@Entity(name = "ResourceApiBindingEntity")
@Table(
        name = "rbac3_resource_api_binding",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rbac3_resource_api_binding_pair",
                columnNames = {"source_resource_id", "api_resource_id"}))
public class ResourceApiBindingPO extends GlobalAuditedPO {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private Long applicationId;

    @Column(name = "source_resource_id", nullable = false, updatable = false)
    private Long sourceResourceId;

    @Column(name = "api_resource_id", nullable = false, updatable = false)
    private Long apiResourceId;

    @Column(name = "source_build_id", nullable = false, length = 256)
    private String sourceBuildId;

    @Column(name = "source_checksum", nullable = false, length = 256)
    private String sourceChecksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ResourceApiBindingStatusEnum status;

    protected ResourceApiBindingPO() {
    }

    public ResourceApiBindingPO(
            Long id,
            Long applicationId,
            Long sourceResourceId,
            Long apiResourceId,
            String sourceBuildId,
            String sourceChecksum,
            String actorId,
            Instant now) {
        this.id = positive(id, "id");
        this.applicationId = positive(applicationId, "applicationId");
        this.sourceResourceId = positive(sourceResourceId, "sourceResourceId");
        this.apiResourceId = positive(apiResourceId, "apiResourceId");
        if (sourceResourceId.equals(apiResourceId)) {
            throw new IllegalArgumentException(
                    "sourceResourceId and apiResourceId must differ");
        }
        this.sourceBuildId = required(sourceBuildId, "sourceBuildId");
        this.sourceChecksum = required(sourceChecksum, "sourceChecksum");
        this.status = ResourceApiBindingStatusEnum.ACTIVE;
        markCreated(actorId, Objects.requireNonNull(now, "now"));
    }

    public void markStale(String actorId, Instant now) {
        status = ResourceApiBindingStatusEnum.STALE;
        markUpdated(actorId, Objects.requireNonNull(now, "now"));
    }

    public void disable(String actorId, Instant now) {
        status = ResourceApiBindingStatusEnum.DISABLED;
        markUpdated(actorId, Objects.requireNonNull(now, "now"));
    }

    /** Refreshes the CI source facts for an existing binding without changing its pair. */
    public void refreshSource(String buildId, String checksum, String actorId, Instant now) {
        sourceBuildId = required(buildId, "sourceBuildId");
        sourceChecksum = required(checksum, "sourceChecksum");
        status = ResourceApiBindingStatusEnum.ACTIVE;
        markUpdated(actorId, Objects.requireNonNull(now, "now"));
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getSourceResourceId() {
        return sourceResourceId;
    }

    public Long getApiResourceId() {
        return apiResourceId;
    }

    public String getSourceBuildId() {
        return sourceBuildId;
    }

    public String getSourceChecksum() {
        return sourceChecksum;
    }

    public ResourceApiBindingStatusEnum getStatus() {
        return status;
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
