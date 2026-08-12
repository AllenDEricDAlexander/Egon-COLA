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

/**
 * 中文说明：{@code JdbcMcpRemoteToolDraftStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP远程工具草稿存储相关的职责与边界。
 * English summary: {@code JdbcMcpRemoteToolDraftStore} is a jdbc mcp remote tool draft store store in the current Gateway module; it owns the jdbc mcp remote tool draft store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpRemoteToolDraftStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpRemoteToolDraftStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpRemoteToolDraftStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpRemoteToolDraftStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpRemoteToolDraftStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpRemoteToolDraftStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpRemoteToolDraftStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpRemoteToolDraftStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 softDelete 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.softDelete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 currentRevision 的处理结果；returns the result of the operation.
     */
    private Long currentRevision(String id) {
        return jdbc.query(
                "SELECT revision FROM gateway_mcp_remote_tool_draft "
                        + "WHERE id = ?",
                (result, row) -> result.getLong("revision"),
                id
        ).stream().findFirst().orElse(null);
    }

    /**
     * 中文说明：执行 validateExpectedRevision 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate expected revision operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.validateExpectedRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpRemoteToolDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpRemoteToolDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteToolDraftStore.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    /**
     * 中文说明：{@code RemoteToolDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程工具草稿相关的职责与边界。
     * English summary: {@code RemoteToolDraft} is an immutable data carrier in the current Gateway module; it owns the remote tool draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param name 参数 name；parameter name。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public record RemoteToolDraft(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteMountId,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpRemoteToolDraftStore.RemoteToolDraft} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param serverId 参数 服务器Id；parameter server id。
         * @param name 参数 name；parameter name。
         * @param remoteMountId 参数 远程MountId；parameter remote mount id。
         * @param content 参数 content；parameter content。
         * @param enabled 参数 enabled；parameter enabled。
         * @param revision 参数 revision；parameter revision。
         */
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
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteToolDraftStore.DraftMutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpRemoteToolDraftStore.DraftMutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.DraftMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.DraftMutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    String id,
    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpRemoteToolDraftStore.DraftMutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpRemoteToolDraftStore.DraftMutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteToolDraftStore.DraftMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteToolDraftStore.DraftMutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    long revision) {
    }
}
