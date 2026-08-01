package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "gateway_application")
public class GatewayApplicationEntity {

    @Id
    private String id;

    @Column(name = "application_code", nullable = false, updatable = false)
    private String applicationCode;

    @Column(name = "biz_code", nullable = false, updatable = false)
    private String bizCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, updatable = false)
    private String env;

    @Column(nullable = false, updatable = false)
    private String namespace;

    private String description;

    @Version
    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    protected GatewayApplicationEntity() {
    }

    public GatewayApplicationEntity(
            String id,
            String bizCode,
            String applicationCode,
            String displayName,
            String env,
            String namespace,
            String description,
            String actorId,
            Instant now) {
        this.id = id;
        this.bizCode = bizCode;
        this.applicationCode = applicationCode;
        this.displayName = displayName;
        this.env = env;
        this.namespace = namespace;
        this.description = description;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            String displayName,
            String description,
            String actorId,
            Instant now) {
        this.displayName = displayName;
        this.description = description;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public String getId() {
        return id;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getBizCode() {
        return bizCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEnv() {
        return env;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getDescription() {
        return description;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
