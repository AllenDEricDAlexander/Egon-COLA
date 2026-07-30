package top.egon.cola.platform.rbac3.admin.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.platform.rbac3.admin.infrastructure.persistence.TenantScopedEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "rbac3_resource")
public class ResourceEntity extends TenantScopedEntity {

    @Id
    private Long id;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;
    @Column(name = "resource_code", nullable = false, length = 128)
    private String resourceCode;
    @Column(name = "resource_name", nullable = false, length = 200)
    private String resourceName;
    @Column(name = "parent_resource_id")
    private Long parentResourceId;
    @Column(name = "required_permission_id")
    private Long requiredPermissionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "source_manifest_id")
    private Long sourceManifestId;
    @Column(name = "source_build_id", length = 256)
    private String sourceBuildId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mechanical_facts", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> mechanicalFacts;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> displayMetadata;
    @Column(name = "stale_since")
    private Instant staleSince;

    protected ResourceEntity() {
    }

    public ResourceEntity(
            Long id, Long tenantId, Long applicationId, ResourceType resourceType,
            String resourceCode, String resourceName, Long parentResourceId,
            Long requiredPermissionId, Long sourceManifestId, String sourceBuildId,
            Map<String, Object> mechanicalFacts, Map<String, Object> displayMetadata,
            String actorId, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.resourceCode = required(resourceCode, "resourceCode");
        this.resourceName = required(resourceName, "resourceName");
        this.parentResourceId = parentResourceId;
        this.requiredPermissionId = requiredPermissionId;
        this.status = Status.PENDING_VALIDATION;
        this.sourceManifestId = sourceManifestId;
        this.sourceBuildId = sourceBuildId;
        this.mechanicalFacts = Map.copyOf(mechanicalFacts);
        this.displayMetadata = Map.copyOf(displayMetadata);
        markCreated(actorId, now);
    }

    public void activate(String actorId, Instant now) {
        status = Status.ACTIVE;
        staleSince = null;
        markUpdated(actorId, now);
    }

    public void markStale(String actorId, Instant now) {
        if (status == Status.ACTIVE) {
            status = Status.STALE;
            staleSince = now;
            markUpdated(actorId, now);
        }
    }

    public void archive(String actorId, Instant now) {
        if (status != Status.STALE) {
            throw new IllegalStateException("only stale resource can be archived");
        }
        status = Status.ARCHIVED;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Long getParentResourceId() {
        return parentResourceId;
    }

    public Long getRequiredPermissionId() {
        return requiredPermissionId;
    }

    public Status getStatus() {
        return status;
    }

    public Long getSourceManifestId() {
        return sourceManifestId;
    }

    public enum ResourceType {
        APP,
        MENU,
        ROUTE,
        ACTION,
        API
    }

    public enum Status {
        PENDING_VALIDATION,
        ACTIVE,
        STALE,
        ARCHIVED
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return value.trim();
    }
}
