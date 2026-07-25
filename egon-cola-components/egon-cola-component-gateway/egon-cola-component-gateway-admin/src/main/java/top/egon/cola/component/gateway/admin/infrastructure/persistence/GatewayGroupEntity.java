package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "gateway_group")
public class GatewayGroupEntity {

    @Id
    private String id;

    @Column(name = "gateway_group_code", nullable = false)
    private String gatewayGroupCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String env;

    @Column(nullable = false)
    private String namespace;

    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    protected GatewayGroupEntity() {
    }

    public GatewayGroupEntity(
            String id,
            String gatewayGroupCode,
            String displayName,
            String env,
            String namespace,
            String description,
            String actor,
            Instant now) {
        this.id = required(id, "id");
        this.gatewayGroupCode = required(
                gatewayGroupCode,
                "gatewayGroupCode"
        );
        this.displayName = required(displayName, "displayName");
        this.env = required(env, "env");
        this.namespace = required(namespace, "namespace");
        this.description = description;
        enabled = true;
        createdAt = now;
        updatedAt = now;
        createdBy = required(actor, "actor");
        updatedBy = actor;
    }

    public void update(
            String displayName,
            String description,
            String actor,
            Instant now) {
        this.displayName = required(displayName, "displayName");
        this.description = description;
        updatedBy = required(actor, "actor");
        updatedAt = now;
    }

    public void setEnabled(boolean enabled, String actor, Instant now) {
        this.enabled = enabled;
        updatedBy = required(actor, "actor");
        updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getGatewayGroupCode() {
        return gatewayGroupCode;
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

    public boolean isEnabled() {
        return enabled;
    }

    public long getRevision() {
        return revision;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
