package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class JdbcMcpCapabilityDraftStore {

    private final JdbcTemplate jdbc;

    private final McpJdbcJson json;

    public JdbcMcpCapabilityDraftStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    public McpCapabilityDraft load(String gatewayGroupId) {
        String groupId = McpJdbcJson.required(
                gatewayGroupId,
                "gatewayGroupId"
        );
        EnumMap<CapabilityKind, List<CapabilityDraft>> values =
                new EnumMap<>(CapabilityKind.class);
        for (CapabilityKind kind : CapabilityKind.values()) {
            values.put(kind, load(kind, groupId));
        }
        return new McpCapabilityDraft(groupId, values);
    }

    public DraftMutation save(
            CapabilityDraft draft,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(draft, "draft");
        validateExpectedRevision(expectedRevision);
        Binding binding = binding(draft);
        List<Object> updateValues = new ArrayList<>();
        updateValues.add(draft.name());
        updateValues.addAll(binding.values());
        updateValues.add(json.write(draft.content()));
        updateValues.add(draft.enabled());
        updateValues.add(McpJdbcJson.timestamp(now));
        updateValues.add(actorId(actor));
        updateValues.add(draft.id());
        updateValues.add(expectedRevision);
        int updated = jdbc.update(
                updateSql(draft.kind(), binding),
                updateValues.toArray()
        );
        if (updated == 1) {
            return new DraftMutation(draft.id(), expectedRevision + 1);
        }
        Long currentRevision = currentRevision(draft.kind(), draft.id());
        if (currentRevision != null || expectedRevision != 0) {
            throw revisionConflict(currentRevision);
        }

        List<Object> insertValues = new ArrayList<>();
        insertValues.add(draft.id());
        insertValues.add(draft.gatewayGroupId());
        insertValues.add(draft.serverId());
        insertValues.add(draft.name());
        insertValues.addAll(binding.values());
        insertValues.add(json.write(draft.content()));
        insertValues.add(draft.enabled());
        insertValues.add(McpJdbcJson.timestamp(now));
        insertValues.add(actorId(actor));
        insertValues.add(McpJdbcJson.timestamp(now));
        insertValues.add(actorId(actor));
        jdbc.update(
                insertSql(draft.kind(), binding),
                insertValues.toArray()
        );
        return new DraftMutation(draft.id(), 0);
    }

    public DraftMutation softDelete(
            CapabilityKind kind,
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateExpectedRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE %s
                   SET deleted = TRUE,
                       enabled = FALSE,
                       revision = revision + 1,
                       updated_at = ?,
                       updated_by = ?
                 WHERE id = ?
                   AND revision = ?
                   AND deleted = FALSE
                """.formatted(kind.table()),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.required(id, "id"),
                expectedRevision
        );
        if (updated != 1) {
            throw revisionConflict(currentRevision(kind, id));
        }
        return new DraftMutation(id, expectedRevision + 1);
    }

    private List<CapabilityDraft> load(
            CapabilityKind kind,
            String gatewayGroupId) {
        String sql = """
                SELECT id, gateway_group_id, server_id,
                       %s AS capability_name,
                       content::text AS content, enabled, revision
                  FROM %s
                 WHERE gateway_group_id = ?
                   AND deleted = FALSE
                 ORDER BY server_id, %s
                """.formatted(
                kind.nameColumn(),
                kind.table(),
                kind.nameColumn()
        );
        return jdbc.query(sql, (result, row) -> new CapabilityDraft(
                kind,
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getString("server_id"),
                result.getString("capability_name"),
                json.map(result.getString("content")),
                result.getBoolean("enabled"),
                result.getLong("revision")
        ), gatewayGroupId);
    }

    private String updateSql(CapabilityKind kind, Binding binding) {
        String extras = binding.updateAssignments().isBlank()
                ? ""
                : binding.updateAssignments() + ",";
        return """
                UPDATE %s
                   SET %s = ?,
                       %s
                       content = ?::jsonb,
                       enabled = ?,
                       revision = revision + 1,
                       updated_at = ?,
                       updated_by = ?
                 WHERE id = ?
                   AND revision = ?
                   AND deleted = FALSE
                """.formatted(kind.table(), kind.nameColumn(), extras);
    }

    private String insertSql(CapabilityKind kind, Binding binding) {
        String extraColumns = binding.columns().isBlank()
                ? ""
                : ", " + binding.columns();
        String extraParameters = binding.values().isEmpty()
                ? ""
                : ", " + "?, ".repeat(binding.values().size() - 1) + "?";
        return """
                INSERT INTO %s(
                    id, gateway_group_id, server_id, %s%s,
                    content, enabled, revision, deleted,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (
                    ?, ?, ?, ?%s, ?::jsonb, ?, 0, FALSE, ?, ?, ?, ?
                )
                """.formatted(
                kind.table(),
                kind.nameColumn(),
                extraColumns,
                extraParameters
        );
    }

    private Binding binding(CapabilityDraft draft) {
        Map<String, Object> content = draft.content();
        return switch (draft.kind()) {
            case RESOURCE -> new Binding(
                    "resource_uri, driver_type, operation_id, remote_mount_id",
                    "resource_uri = ?, driver_type = ?, operation_id = ?, "
                            + "remote_mount_id = ?",
                    nullableValues(
                            requiredContent(content, "uri"),
                            requiredContent(content, "driverType"),
                            optionalContent(content, "operationId"),
                            optionalContent(content, "remoteMountId")
                    )
            );
            case RESOURCE_TEMPLATE -> new Binding(
                    "uri_template, driver_type, operation_id, remote_mount_id",
                    "uri_template = ?, driver_type = ?, operation_id = ?, "
                            + "remote_mount_id = ?",
                    nullableValues(
                            requiredContent(content, "uriTemplate"),
                            requiredContent(content, "driverType"),
                            optionalContent(content, "operationId"),
                            optionalContent(content, "remoteMountId")
                    )
            );
            case PROMPT -> new Binding(
                    "source_type, operation_id, remote_mount_id",
                    "source_type = ?, operation_id = ?, remote_mount_id = ?",
                    nullableValues(
                            requiredContent(content, "sourceType"),
                            optionalContent(content, "operationId"),
                            optionalContent(content, "remoteMountId")
                    )
            );
            case TASK_POLICY -> Binding.none();
            case APP_BINDING -> new Binding(
                    "app_artifact_id",
                    "app_artifact_id = ?",
                    List.of(requiredContent(content, "appArtifactId"))
            );
        };
    }

    private Object requiredContent(
            Map<String, Object> content,
            String name) {
        Object value = content.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "capability content " + name + " is required"
            );
        }
        return value.toString().trim();
    }

    private Object optionalContent(
            Map<String, Object> content,
            String name) {
        Object value = content.get(name);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().trim();
    }

    private List<Object> nullableValues(Object... values) {
        return Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(values))
        );
    }

    private Long currentRevision(
            CapabilityKind kind,
            String id) {
        List<Long> values = jdbc.query(
                "SELECT revision FROM " + kind.table() + " WHERE id = ?",
                (result, row) -> result.getLong("revision"),
                id
        );
        return values.stream().findFirst().orElse(null);
    }

    private GatewayAdminRevisionConflictException revisionConflict(
            Long currentRevision) {
        return new GatewayAdminRevisionConflictException(
                currentRevision == null ? -1 : currentRevision
        );
    }

    private void validateExpectedRevision(long expectedRevision) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    public enum CapabilityKind {
        RESOURCE("gateway_mcp_resource_draft", "resource_name"),
        RESOURCE_TEMPLATE(
                "gateway_mcp_resource_template_draft",
                "template_name"
        ),
        PROMPT("gateway_mcp_prompt_draft", "prompt_name"),
        TASK_POLICY("gateway_mcp_task_policy_draft", "tool_name"),
        APP_BINDING("gateway_mcp_app_binding_draft", "tool_name");

        private final String table;

        private final String nameColumn;

        CapabilityKind(String table, String nameColumn) {
            this.table = table;
            this.nameColumn = nameColumn;
        }

        String table() {
            return table;
        }

        String nameColumn() {
            return nameColumn;
        }
    }

    public record CapabilityDraft(
            CapabilityKind kind,
            String id,
            String gatewayGroupId,
            String serverId,
            String name,
            Map<String, Object> content,
            boolean enabled,
            long revision
    ) {

        public CapabilityDraft {
            kind = Objects.requireNonNull(kind, "kind");
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            serverId = McpJdbcJson.required(serverId, "serverId");
            name = McpJdbcJson.required(name, "name");
            content = Map.copyOf(Objects.requireNonNull(content, "content"));
            if (revision < 0) {
                throw new IllegalArgumentException(
                        "revision must not be negative"
                );
            }
        }
    }

    public record McpCapabilityDraft(
            String gatewayGroupId,
            Map<CapabilityKind, List<CapabilityDraft>> capabilities
    ) {

        public McpCapabilityDraft {
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            EnumMap<CapabilityKind, List<CapabilityDraft>> copy =
                    new EnumMap<>(CapabilityKind.class);
            capabilities.forEach((kind, drafts) -> copy.put(
                    kind,
                    List.copyOf(drafts)
            ));
            capabilities = Map.copyOf(copy);
        }

        public List<CapabilityDraft> capabilities(CapabilityKind kind) {
            return capabilities.getOrDefault(kind, List.of());
        }
    }

    public record DraftMutation(String id, long revision) {
    }

    private record Binding(
            String columns,
            String updateAssignments,
            List<Object> values
    ) {

        private Binding {
            values = Collections.unmodifiableList(new ArrayList<>(values));
        }

        private static Binding none() {
            return new Binding("", "", List.of());
        }
    }
}
