package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class JdbcMcpRemoteToolDraftStore {

    private final JdbcTemplate jdbc;

    private final McpJdbcJson json;

    public JdbcMcpRemoteToolDraftStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    public List<RemoteToolDraft> load(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, server_id, tool_name,
                       remote_mount_id, content::text AS content,
                       enabled, revision
                  FROM gateway_mcp_remote_tool_draft
                 WHERE gateway_group_id = ?
                   AND deleted = FALSE
                 ORDER BY server_id, tool_name
                """, (result, row) -> new RemoteToolDraft(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getString("server_id"),
                result.getString("tool_name"),
                result.getString("remote_mount_id"),
                json.map(result.getString("content")),
                result.getBoolean("enabled"),
                result.getLong("revision")
        ), McpJdbcJson.required(gatewayGroupId, "gatewayGroupId"));
    }

    public DraftMutation save(
            RemoteToolDraft draft,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(draft, "draft");
        validateExpectedRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_remote_tool_draft
                   SET tool_name = ?,
                       remote_mount_id = ?,
                       content = ?::jsonb,
                       enabled = ?,
                       revision = revision + 1,
                       updated_at = ?,
                       updated_by = ?
                 WHERE id = ?
                   AND revision = ?
                   AND deleted = FALSE
                """,
                draft.name(),
                draft.remoteMountId(),
                json.write(draft.content()),
                draft.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                draft.id(),
                expectedRevision
        );
        if (updated == 1) {
            return new DraftMutation(draft.id(), expectedRevision + 1);
        }
        Long current = currentRevision(draft.id());
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_tool_draft(
                    id, gateway_group_id, server_id, tool_name,
                    remote_mount_id, content, enabled, revision, deleted,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?::jsonb, ?, 0, FALSE, ?, ?, ?, ?
                )
                """,
                draft.id(),
                draft.gatewayGroupId(),
                draft.serverId(),
                draft.name(),
                draft.remoteMountId(),
                json.write(draft.content()),
                draft.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new DraftMutation(draft.id(), 0);
    }

    public DraftMutation softDelete(
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateExpectedRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_remote_tool_draft
                   SET deleted = TRUE,
                       enabled = FALSE,
                       revision = revision + 1,
                       updated_at = ?,
                       updated_by = ?
                 WHERE id = ?
                   AND revision = ?
                   AND deleted = FALSE
                """,
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.required(id, "id"),
                expectedRevision
        );
        if (updated != 1) {
            throw revisionConflict(currentRevision(id));
        }
        return new DraftMutation(id, expectedRevision + 1);
    }

    private Long currentRevision(String id) {
        return jdbc.query(
                "SELECT revision FROM gateway_mcp_remote_tool_draft "
                        + "WHERE id = ?",
                (result, row) -> result.getLong("revision"),
                id
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

    public record RemoteToolDraft(
            String id,
            String gatewayGroupId,
            String serverId,
            String name,
            String remoteMountId,
            Map<String, Object> content,
            boolean enabled,
            long revision
    ) {

        public RemoteToolDraft {
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            serverId = McpJdbcJson.required(serverId, "serverId");
            name = McpJdbcJson.required(name, "name");
            remoteMountId = McpJdbcJson.required(
                    remoteMountId,
                    "remoteMountId"
            );
            content = Map.copyOf(Objects.requireNonNull(content, "content"));
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
