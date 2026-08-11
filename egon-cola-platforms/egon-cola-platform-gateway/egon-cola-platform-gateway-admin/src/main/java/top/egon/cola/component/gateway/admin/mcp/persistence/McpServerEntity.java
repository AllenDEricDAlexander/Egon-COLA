package top.egon.cola.component.gateway.admin.mcp.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "gateway_mcp_server")
public class McpServerEntity {

    @Id
    private String id;

    @Column(name = "gateway_group_id", nullable = false)
    private String gatewayGroupId;

    @Column(name = "server_code", nullable = false)
    private String serverCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String description;

    private String instructions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Set<String> dialects;

    @Column(name = "resource_uri", nullable = false)
    private String resourceUri;

    @Column(name = "list_cache_ttl_seconds", nullable = false)
    private long listCacheTtlSeconds;

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

    protected McpServerEntity() {
    }

    public McpServerEntity(
            String id,
            String gatewayGroupId,
            String serverCode,
            String displayName,
            String description,
            String instructions,
            Set<String> dialects,
            String resourceUri,
            long listCacheTtlSeconds,
            AdminActor actor,
            Instant now) {
        this.id = required(id, "id");
        this.gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        this.serverCode = required(serverCode, "serverCode");
        this.displayName = required(displayName, "displayName");
        this.description = optional(description);
        this.instructions = optional(instructions);
        this.dialects = nonEmpty(dialects, "dialects");
        this.resourceUri = resourceUri(resourceUri);
        this.listCacheTtlSeconds = nonNegative(
                listCacheTtlSeconds,
                "listCacheTtlSeconds"
        );
        enabled = true;
        createdAt = now;
        updatedAt = now;
        createdBy = actor(actor);
        updatedBy = createdBy;
    }

    public void update(
            String displayName,
            String description,
            String instructions,
            Set<String> dialects,
            String resourceUri,
            long listCacheTtlSeconds,
            boolean enabled,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        assertRevision(expectedRevision);
        this.displayName = required(displayName, "displayName");
        this.description = optional(description);
        this.instructions = optional(instructions);
        this.dialects = nonEmpty(dialects, "dialects");
        this.resourceUri = resourceUri(resourceUri);
        this.listCacheTtlSeconds = nonNegative(
                listCacheTtlSeconds,
                "listCacheTtlSeconds"
        );
        this.enabled = enabled;
        updatedAt = now;
        updatedBy = actor(actor);
    }

    public void softDelete(
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        assertRevision(expectedRevision);
        deleted = true;
        enabled = false;
        updatedAt = now;
        updatedBy = actor(actor);
    }

    public void assertRevision(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new GatewayAdminRevisionConflictException(revision);
        }
    }

    public String getId() {
        return id;
    }

    public String getGatewayGroupId() {
        return gatewayGroupId;
    }

    public String getServerCode() {
        return serverCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public Set<String> getDialects() {
        return Set.copyOf(dialects);
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public long getListCacheTtlSeconds() {
        return listCacheTtlSeconds;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    private static String actor(AdminActor actor) {
        return java.util.Objects.requireNonNull(actor, "actor").actorId();
    }

    private static Set<String> nonEmpty(Set<String> values, String field) {
        Set<String> copy = Set.copyOf(values == null ? Set.of() : values);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return copy;
    }

    private static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String resourceUri(String value) {
        URI uri;
        try {
            uri = URI.create(required(value, "resourceUri")).normalize();
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "resourceUri must be a valid URI", invalid);
        }
        if (!uri.isAbsolute() || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "resourceUri must be absolute and must not contain a fragment"
            );
        }
        return uri.toString();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
