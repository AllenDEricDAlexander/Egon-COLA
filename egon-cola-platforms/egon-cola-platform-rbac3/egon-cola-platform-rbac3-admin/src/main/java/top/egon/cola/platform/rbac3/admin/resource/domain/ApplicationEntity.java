package top.egon.cola.platform.rbac3.admin.resource.domain;

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
@Table(name = "rbac3_application")
public class ApplicationEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_code", nullable = false, length = 128)
    private String applicationCode;

    @Column(name = "application_name", nullable = false, length = 200)
    private String applicationName;

    @Column(name = "display_priority", nullable = false)
    private int displayPriority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "current_manifest_id")
    private Long currentManifestId;

    @Column(name = "current_manifest_version")
    private Long currentManifestVersion;

    protected ApplicationEntity() {
    }

    public ApplicationEntity(
            Long id,
            Long tenantId,
            String applicationCode,
            String applicationName,
            int displayPriority,
            String actorId,
            Instant now) {
        if (displayPriority < 0) {
            throw new IllegalArgumentException("displayPriority must not be negative");
        }
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationCode = required(applicationCode, "applicationCode");
        this.applicationName = required(applicationName, "applicationName");
        this.displayPriority = displayPriority;
        this.status = Status.ACTIVE;
        markCreated(actorId, now);
    }

    public void activateManifest(
            Long manifestId,
            long manifestVersion,
            String actorId,
            Instant now) {
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("application is not active");
        }
        currentManifestId = Objects.requireNonNull(manifestId, "manifestId");
        currentManifestVersion = manifestVersion;
        markUpdated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public Status getStatus() {
        return status;
    }

    public Long getCurrentManifestVersion() {
        return currentManifestVersion;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        DISABLED,
        ARCHIVED
    }
}
