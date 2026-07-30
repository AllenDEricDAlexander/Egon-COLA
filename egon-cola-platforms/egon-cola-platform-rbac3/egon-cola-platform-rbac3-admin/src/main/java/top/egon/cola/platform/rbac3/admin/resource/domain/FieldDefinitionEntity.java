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
@Table(name = "rbac3_field_definition")
public class FieldDefinitionEntity extends TenantScopedEntity {

    @Id
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "field_code", nullable = false, length = 128)
    private String fieldCode;

    @Column(name = "json_path", nullable = false, length = 512)
    private String jsonPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 32)
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Sensitivity sensitivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_access", nullable = false, length = 32)
    private DefaultAccess defaultAccess;

    @Column(name = "masking_strategy", length = 32)
    private String maskingStrategy;

    @Column(nullable = false)
    private boolean writable;

    @Column(nullable = false)
    private boolean exportable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "source_manifest_id")
    private Long sourceManifestId;

    protected FieldDefinitionEntity() {
    }

    public FieldDefinitionEntity(
            Long id,
            Long tenantId,
            Long applicationId,
            Long resourceId,
            String fieldCode,
            String jsonPath,
            DataType dataType,
            Sensitivity sensitivity,
            DefaultAccess defaultAccess,
            String maskingStrategy,
            boolean writable,
            boolean exportable,
            Long sourceManifestId,
            String actorId,
            Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        setTenantId(Objects.requireNonNull(tenantId, "tenantId"));
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.fieldCode = required(fieldCode, "fieldCode");
        this.jsonPath = required(jsonPath, "jsonPath");
        this.dataType = Objects.requireNonNull(dataType, "dataType");
        this.sensitivity = Objects.requireNonNull(sensitivity, "sensitivity");
        this.defaultAccess = Objects.requireNonNull(defaultAccess, "defaultAccess");
        this.maskingStrategy = maskingStrategy;
        this.writable = writable;
        this.exportable = exportable;
        this.status = Status.ACTIVE;
        this.sourceManifestId = sourceManifestId;
        markCreated(actorId, now);
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Status getStatus() {
        return status;
    }

    public enum DataType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        DATETIME,
        OBJECT,
        ARRAY
    }

    public enum Sensitivity {
        NORMAL,
        INTERNAL,
        CONFIDENTIAL,
        HIGH
    }

    public enum DefaultAccess {
        NONE,
        MASKED_READ,
        READ
    }

    public enum Status {
        ACTIVE,
        STALE,
        DISABLED,
        ARCHIVED
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
