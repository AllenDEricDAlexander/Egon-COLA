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

/**
 * 中文说明：{@code JdbcMcpCapabilityDraftStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCPCapability草稿存储相关的职责与边界。
 * English summary: {@code JdbcMcpCapabilityDraftStore} is a jdbc mcp capability draft store store in the current Gateway module; it owns the jdbc mcp capability draft store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpCapabilityDraftStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpCapabilityDraftStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpCapabilityDraftStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpCapabilityDraftStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpCapabilityDraftStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpCapabilityDraftStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpCapabilityDraftStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpCapabilityDraftStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 softDelete 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.softDelete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 updateSql 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update sql operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.updateSql(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param binding 参数 binding；parameter binding。
     * @return 返回 updateSql 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 insertSql 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert sql operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.insertSql(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param binding 参数 binding；parameter binding。
     * @return 返回 insertSql 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 binding 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the binding operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.binding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @return 返回 binding 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 requiredContent 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required content operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.requiredContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param name 参数 name；parameter name。
     * @return 返回 requiredContent 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 optionalContent 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional content operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.optionalContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param name 参数 name；parameter name。
     * @return 返回 optionalContent 的处理结果；returns the result of the operation.
     */
    private Object optionalContent(
            Map<String, Object> content,
            String name) {
        Object value = content.get(name);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().trim();
    }

    /**
     * 中文说明：执行 nullableValues 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the nullable values operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.nullableValues(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 nullableValues 的处理结果；returns the result of the operation.
     */
    private List<Object> nullableValues(Object... values) {
        return Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(values))
        );
    }

    /**
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param id 参数 id；parameter id。
     * @return 返回 currentRevision 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param currentRevision 参数 currentRevision；parameter current revision。
     * @return 返回 revisionConflict 的处理结果；returns the result of the operation.
     */
    private GatewayAdminRevisionConflictException revisionConflict(
            Long currentRevision) {
        return new GatewayAdminRevisionConflictException(
                currentRevision == null ? -1 : currentRevision
        );
    }

    /**
     * 中文说明：执行 validateExpectedRevision 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate expected revision operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.validateExpectedRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    private void validateExpectedRevision(long expectedRevision) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    /**
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpCapabilityDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    /**
     * 中文说明：{@code CapabilityKind} 是枚举类型，位于当前 Gateway 模块的相关包中，负责CapabilityKind相关的职责与边界。
     * English summary: {@code CapabilityKind} is an enumeration in the current Gateway module; it owns the capability kind-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum CapabilityKind {
        /**
         * 中文说明：表示 资源 这一固定值；它属于 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value resource; it is a state, type, or protocol value of {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        RESOURCE("gateway_mcp_resource_draft", "resource_name"),
        /**
         * 中文说明：表示 资源模板 这一固定值；它属于 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value resource template; it is a state, type, or protocol value of {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        RESOURCE_TEMPLATE(
                "gateway_mcp_resource_template_draft",
                "template_name"
        ),
        /**
         * 中文说明：表示 提示词 这一固定值；它属于 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value prompt; it is a state, type, or protocol value of {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        PROMPT("gateway_mcp_prompt_draft", "prompt_name"),
        /**
         * 中文说明：表示 任务策略 这一固定值；它属于 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value task policy; it is a state, type, or protocol value of {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        TASK_POLICY("gateway_mcp_task_policy_draft", "tool_name"),
        /**
         * 中文说明：表示 APPBINDING 这一固定值；它属于 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value app binding; it is a state, type, or protocol value of {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        APP_BINDING("gateway_mcp_app_binding_draft", "tool_name");

        /**
         * 中文说明：保存 table 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by table; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.CapabilityKind} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String table;

        /**
         * 中文说明：保存 nameColumn 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by name column; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.CapabilityKind} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityKind}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String nameColumn;

        /**
         * 中文说明：创建 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpCapabilityDraftStore.CapabilityKind} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param table 参数 table；parameter table。
         * @param nameColumn 参数 nameColumn；parameter name column。
         */
        CapabilityKind(String table, String nameColumn) {
            this.table = table;
            this.nameColumn = nameColumn;
        }

        /**
         * 中文说明：执行 table 操作；该方法是 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the table operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.CapabilityKind.table(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 table 的处理结果；returns the result of the operation.
         */
        String table() {
            return table;
        }

        /**
         * 中文说明：执行 nameColumn 操作；该方法是 {@code JdbcMcpCapabilityDraftStore.CapabilityKind} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the name column operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore.CapabilityKind} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.CapabilityKind.nameColumn(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 nameColumn 的处理结果；returns the result of the operation.
         */
        String nameColumn() {
            return nameColumn;
        }
    }

    /**
     * 中文说明：{@code CapabilityDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Capability草稿相关的职责与边界。
     * English summary: {@code CapabilityDraft} is an immutable data carrier in the current Gateway module; it owns the capability draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param kind 参数 kind；parameter kind。
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param name 参数 name；parameter name。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public record CapabilityDraft(
            /**
             * 中文说明：保存 kind 对应的状态、依赖或配置值；字段类型为 {@code CapabilityKind}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by kind; its type is {@code CapabilityKind}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            CapabilityKind kind,
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpCapabilityDraftStore.CapabilityDraft} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param kind 参数 kind；parameter kind。
         * @param id 参数 id；parameter id。
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param serverId 参数 服务器Id；parameter server id。
         * @param name 参数 name；parameter name。
         * @param content 参数 content；parameter content。
         * @param enabled 参数 enabled；parameter enabled。
         * @param revision 参数 revision；parameter revision。
         */
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

    /**
     * 中文说明：{@code McpCapabilityDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCPCapability草稿相关的职责与边界。
     * English summary: {@code McpCapabilityDraft} is an immutable data carrier in the current Gateway module; it owns the mcp capability draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param capabilities 参数 capabilities；parameter capabilities。
     */
    public record McpCapabilityDraft(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code Map<CapabilityKind, List<CapabilityDraft>>}，由 {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code Map<CapabilityKind, List<CapabilityDraft>>}, and {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<CapabilityKind, List<CapabilityDraft>> capabilities
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param capabilities 参数 capabilities；parameter capabilities。
         */
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

        /**
         * 中文说明：执行 capabilities 操作；该方法是 {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the capabilities operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.McpCapabilityDraft.capabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param kind 参数 kind；parameter kind。
         * @return 返回 capabilities 的处理结果；returns the result of the operation.
         */
        public List<CapabilityDraft> capabilities(CapabilityKind kind) {
            return capabilities.getOrDefault(kind, List.of());
        }
    }

    /**
     * 中文说明：{@code DraftMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿Mutation相关的职责与边界。
     * English summary: {@code DraftMutation} is an immutable data carrier in the current Gateway module; it owns the draft mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param revision 参数 revision；parameter revision。
     */
    public record DraftMutation(
    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.DraftMutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.DraftMutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.DraftMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.DraftMutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    String id,
    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpCapabilityDraftStore.DraftMutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpCapabilityDraftStore.DraftMutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.DraftMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.DraftMutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    long revision) {
    }

    /**
     * 中文说明：{@code Binding} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Binding相关的职责与边界。
     * English summary: {@code Binding} is an immutable data carrier in the current Gateway module; it owns the binding-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param columns 参数 columns；parameter columns。
     * @param updateAssignments 参数 updateAssignments；parameter update assignments。
     * @param values 参数 values；parameter values。
     */
    private record Binding(
            /**
             * 中文说明：保存 columns 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.Binding} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by columns; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.Binding} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.Binding} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.Binding}; do not couple callers to its representation when the owning type exposes an API.
             */
            String columns,
            /**
             * 中文说明：保存 updateAssignments 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpCapabilityDraftStore.Binding} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by update assignments; its type is {@code String}, and {@code JdbcMcpCapabilityDraftStore.Binding} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.Binding} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.Binding}; do not couple callers to its representation when the owning type exposes an API.
             */
            String updateAssignments,
            /**
             * 中文说明：保存 values 对应的状态、依赖或配置值；字段类型为 {@code List<Object>}，由 {@code JdbcMcpCapabilityDraftStore.Binding} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by values; its type is {@code List<Object>}, and {@code JdbcMcpCapabilityDraftStore.Binding} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftStore.Binding} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftStore.Binding}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<Object> values
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpCapabilityDraftStore.Binding} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpCapabilityDraftStore.Binding} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param columns 参数 columns；parameter columns。
         * @param updateAssignments 参数 updateAssignments；parameter update assignments。
         * @param values 参数 values；parameter values。
         */
        private Binding {
            values = Collections.unmodifiableList(new ArrayList<>(values));
        }

        /**
         * 中文说明：执行 none 操作；该方法是 {@code JdbcMcpCapabilityDraftStore.Binding} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the none operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftStore.Binding} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftStore.Binding.none(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 none 的处理结果；returns the result of the operation.
         */
        private static Binding none() {
            return new Binding("", "", List.of());
        }
    }
}
