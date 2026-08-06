package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class JdbcMcpManagedToolOverrideStore {

    private final JdbcTemplate jdbc;

    private final McpJdbcJson json;

    public JdbcMcpManagedToolOverrideStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    public List<ManagedToolOverride> load(String gatewayGroupId) {
        return jdbc.query("""
                SELECT tool_id, gateway_group_id, operation_id, server_id,
                       additional_permissions::text AS additional_permissions,
                       minimum_risk_level, enabled, revision
                  FROM gateway_mcp_managed_tool_override
                 WHERE gateway_group_id = ?
                 ORDER BY operation_id
                """, (result, row) -> new ManagedToolOverride(
                result.getString("tool_id"),
                result.getString("gateway_group_id"),
                result.getString("operation_id"),
                result.getString("server_id"),
                json.stringSet(
                        result.getString("additional_permissions")
                ),
                result.getString("minimum_risk_level"),
                result.getObject("enabled", Boolean.class),
                result.getLong("revision")
        ), McpJdbcJson.required(gatewayGroupId, "gatewayGroupId"));
    }

    public DraftMutation save(
            ManagedToolOverride override,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(override, "override");
        validateExpectedRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_managed_tool_override
                   SET tool_id = ?,
                       server_id = ?,
                       additional_permissions = ?::jsonb,
                       minimum_risk_level = ?,
                       enabled = ?,
                       revision = revision + 1,
                       updated_at = ?,
                       updated_by = ?
                 WHERE gateway_group_id = ?
                   AND operation_id = ?
                   AND revision = ?
                """,
                override.toolId(),
                override.serverId(),
                json.write(override.additionalPermissions()),
                override.minimumRiskLevel(),
                override.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                override.gatewayGroupId(),
                override.operationId(),
                expectedRevision
        );
        if (updated == 1) {
            return new DraftMutation(override.toolId(), expectedRevision + 1);
        }
        Long current = currentRevision(
                override.gatewayGroupId(),
                override.operationId()
        );
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_managed_tool_override(
                    tool_id, gateway_group_id, operation_id, server_id,
                    additional_permissions, minimum_risk_level, enabled,
                    revision, created_at, created_by, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, 0, ?, ?, ?, ?)
                """,
                override.toolId(),
                override.gatewayGroupId(),
                override.operationId(),
                override.serverId(),
                json.write(override.additionalPermissions()),
                override.minimumRiskLevel(),
                override.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new DraftMutation(override.toolId(), 0);
    }

    public DraftMutation delete(
            String toolId,
            String gatewayGroupId,
            String operationId,
            long expectedRevision) {
        validateExpectedRevision(expectedRevision);
        int deleted = jdbc.update("""
                DELETE FROM gateway_mcp_managed_tool_override
                 WHERE gateway_group_id = ?
                   AND operation_id = ?
                   AND revision = ?
                """,
                McpJdbcJson.required(gatewayGroupId, "gatewayGroupId"),
                McpJdbcJson.required(operationId, "operationId"),
                expectedRevision
        );
        if (deleted != 1) {
            throw revisionConflict(currentRevision(
                    gatewayGroupId,
                    operationId
            ));
        }
        return new DraftMutation(toolId, expectedRevision + 1);
    }

    private Long currentRevision(
            String gatewayGroupId,
            String operationId) {
        return jdbc.query(
                "SELECT revision FROM gateway_mcp_managed_tool_override "
                        + "WHERE gateway_group_id = ? AND operation_id = ?",
                (result, row) -> result.getLong("revision"),
                gatewayGroupId,
                operationId
        ).stream().findFirst().orElse(null);
    }

    private void validateExpectedRevision(long expectedRevision) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    private GatewayAdminRevisionConflictException revisionConflict(
            Long currentRevision) {
        return new GatewayAdminRevisionConflictException(
                currentRevision == null ? -1 : currentRevision
        );
    }

    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    public record ManagedToolOverride(
            String toolId,
            String gatewayGroupId,
            String operationId,
            String serverId,
            Set<String> additionalPermissions,
            String minimumRiskLevel,
            Boolean enabled,
            long revision
    ) {

        public ManagedToolOverride {
            toolId = McpJdbcJson.required(toolId, "toolId");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            operationId = McpJdbcJson.required(operationId, "operationId");
            additionalPermissions = Set.copyOf(Objects.requireNonNull(
                    additionalPermissions,
                    "additionalPermissions"
            ));
            if (Boolean.TRUE.equals(enabled)) {
                throw new IllegalArgumentException(
                        "managed Tool override cannot enable a Tool"
                );
            }
            if (serverId == null && additionalPermissions.isEmpty()
                    && minimumRiskLevel == null && enabled == null) {
                throw new IllegalArgumentException(
                        "managed Tool override must tighten at least one field"
                );
            }
            if (revision < 0) {
                throw new IllegalArgumentException(
                        "revision must not be negative"
                );
            }
        }
    }

    public record DraftMutation(String id, long revision) {
    }
}
