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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "rbac3_resource_manifest")
public class ResourceManifestEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "artifact_version", nullable = false, length = 128)
    private String artifactVersion;

    @Column(name = "build_id", nullable = false, length = 256)
    private String buildId;

    @Column(name = "manifest_version", nullable = false)
    private long manifestVersion;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "definition_set_id", length = 64)
    private String definitionSetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> validationResult = new LinkedHashMap<>();

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    protected ResourceManifestEntity() {
    }

    public ResourceManifestEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            int schemaVersion,
            String artifactVersion,
            String buildId,
            long manifestVersion,
            String checksum,
            String definitionSetId,
            Map<String, Object> payload,
            String actorId,
            Instant now) {
        if (schemaVersion < 1 || manifestVersion < 0) {
            throw new IllegalArgumentException("manifest versions are invalid");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.schemaVersion = schemaVersion;
        this.artifactVersion = required(artifactVersion, "artifactVersion");
        this.buildId = required(buildId, "buildId");
        this.manifestVersion = manifestVersion;
        this.checksum = required(checksum, "checksum");
        this.status = Status.PENDING_VALIDATION;
        this.definitionSetId = required(definitionSetId, "definitionSetId");
        this.payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
        this.receivedAt = Objects.requireNonNull(now, "now");
        markCreated(actorId, now);
    }

    public void activate(String actorId, Instant now) {
        if (status != Status.PENDING_VALIDATION) {
            throw new IllegalStateException("only pending manifest can be activated");
        }
        status = Status.ACTIVE;
        activatedAt = now;
        markUpdated(actorId, now);
    }

    public void supersede(String actorId, Instant now) {
        if (status == Status.ACTIVE) {
            status = Status.SUPERSEDED;
            markUpdated(actorId, now);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public String getBuildId() {
        return buildId;
    }

    public long getManifestVersion() {
        return manifestVersion;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getDefinitionSetId() {
        return definitionSetId;
    }

    public Map<String, Object> getPayload() {
        return Map.copyOf(payload);
    }

    public Map<String, Object> getValidationResult() {
        return Map.copyOf(validationResult);
    }

    public Status getStatus() {
        return status;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        PENDING_VALIDATION,
        ACTIVE,
        SUPERSEDED
    }
}
