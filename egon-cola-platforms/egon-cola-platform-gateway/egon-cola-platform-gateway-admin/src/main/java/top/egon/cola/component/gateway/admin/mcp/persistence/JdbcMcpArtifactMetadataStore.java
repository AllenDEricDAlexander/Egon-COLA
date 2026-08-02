package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
public class JdbcMcpArtifactMetadataStore {

    private final JdbcTemplate jdbc;

    private final McpJdbcJson json;

    public JdbcMcpArtifactMetadataStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    public void save(ArtifactMetadata artifact) {
        Objects.requireNonNull(artifact, "artifact");
        jdbc.update("""
                INSERT INTO gateway_mcp_app_artifact(
                    id, gateway_group_id, app_code, app_version,
                    display_name, resource_uri, artifact_reference,
                    artifact_sha256, size_bytes, mime_type,
                    content_security_policy, permission_manifest,
                    allowed_origins, status, created_at, created_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?::jsonb, ?::jsonb, 'ACTIVE', ?, ?
                )
                """,
                artifact.id(),
                artifact.gatewayGroupId(),
                artifact.appCode(),
                artifact.version(),
                artifact.displayName(),
                artifact.resourceUri(),
                artifact.artifactReference(),
                artifact.sha256(),
                artifact.sizeBytes(),
                artifact.mimeType(),
                artifact.contentSecurityPolicy(),
                json.write(artifact.permissions()),
                json.write(artifact.allowedOrigins()),
                McpJdbcJson.timestamp(artifact.createdAt()),
                artifact.createdBy()
        );
    }

    public Optional<ArtifactMetadata> find(String id) {
        List<ArtifactMetadata> values = jdbc.query("""
                SELECT id, gateway_group_id, app_code, app_version,
                       display_name, resource_uri, artifact_reference,
                       artifact_sha256, size_bytes, mime_type,
                       content_security_policy,
                       permission_manifest::text AS permission_manifest,
                       allowed_origins::text AS allowed_origins,
                       created_at, created_by
                  FROM gateway_mcp_app_artifact
                 WHERE id = ? AND status = 'ACTIVE'
                """, (result, row) -> map(result), id);
        return values.stream().findFirst();
    }

    public List<ArtifactMetadata> list(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, app_code, app_version,
                       display_name, resource_uri, artifact_reference,
                       artifact_sha256, size_bytes, mime_type,
                       content_security_policy,
                       permission_manifest::text AS permission_manifest,
                       allowed_origins::text AS allowed_origins,
                       created_at, created_by
                  FROM gateway_mcp_app_artifact
                 WHERE gateway_group_id = ? AND status = 'ACTIVE'
                 ORDER BY app_code, app_version
                """, (result, row) -> map(result), gatewayGroupId);
    }

    public boolean revoke(String id) {
        return jdbc.update("""
                UPDATE gateway_mcp_app_artifact
                   SET status = 'REVOKED'
                 WHERE id = ? AND status = 'ACTIVE'
                """, id) == 1;
    }

    private ArtifactMetadata map(java.sql.ResultSet result)
            throws java.sql.SQLException {
        return new ArtifactMetadata(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getString("app_code"),
                result.getString("app_version"),
                result.getString("display_name"),
                result.getString("resource_uri"),
                result.getString("artifact_reference"),
                result.getString("artifact_sha256"),
                result.getLong("size_bytes"),
                result.getString("mime_type"),
                result.getString("content_security_policy"),
                json.stringSet(result.getString("permission_manifest")),
                json.stringSet(result.getString("allowed_origins")),
                result.getString("created_by"),
                result.getTimestamp("created_at").toInstant()
        );
    }

    public record ArtifactMetadata(
            String id,
            String gatewayGroupId,
            String appCode,
            String version,
            String displayName,
            String resourceUri,
            String artifactReference,
            String sha256,
            long sizeBytes,
            String mimeType,
            String contentSecurityPolicy,
            Set<String> permissions,
            Set<String> allowedOrigins,
            String createdBy,
            Instant createdAt
    ) {

        public ArtifactMetadata {
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            appCode = McpJdbcJson.required(appCode, "appCode");
            version = McpJdbcJson.required(version, "version");
            displayName = McpJdbcJson.required(displayName, "displayName");
            resourceUri = McpJdbcJson.required(resourceUri, "resourceUri");
            artifactReference = McpJdbcJson.required(
                    artifactReference,
                    "artifactReference"
            );
            sha256 = McpJdbcJson.required(sha256, "sha256");
            mimeType = McpJdbcJson.required(mimeType, "mimeType");
            contentSecurityPolicy = McpJdbcJson.required(
                    contentSecurityPolicy,
                    "contentSecurityPolicy"
            );
            permissions = Set.copyOf(permissions);
            allowedOrigins = Set.copyOf(allowedOrigins);
            createdBy = McpJdbcJson.required(createdBy, "createdBy");
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
            if (sha256.length() != 64) {
                throw new IllegalArgumentException(
                        "sha256 must contain 64 characters"
                );
            }
            if (sizeBytes < 0 || sizeBytes > 16L * 1024 * 1024) {
                throw new IllegalArgumentException(
                        "artifact size is outside the supported range"
                );
            }
        }
    }
}
