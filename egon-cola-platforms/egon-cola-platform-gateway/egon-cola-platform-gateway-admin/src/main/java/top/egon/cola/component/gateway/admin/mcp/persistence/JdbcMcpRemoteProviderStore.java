package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code JdbcMcpRemoteProviderStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP远程提供方存储相关的职责与边界。
 * English summary: {@code JdbcMcpRemoteProviderStore} is a jdbc mcp remote provider store store in the current Gateway module; it owns the jdbc mcp remote provider store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpRemoteProviderStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpRemoteProviderStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpRemoteProviderStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpRemoteProviderStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpRemoteProviderStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpRemoteProviderStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpRemoteProviderStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpRemoteProviderStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 providers 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the providers operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.providers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 providers 的处理结果；returns the result of the operation.
     */
    public List<RemoteProviderDraft> providers(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, provider_code, display_name,
                       dialect, transport_type, endpoint_reference,
                       auth_profile_reference, tls_profile_reference,
                       capability_fingerprint, status, enabled, revision
                  FROM gateway_mcp_remote_provider
                 WHERE gateway_group_id = ?
                   AND deleted = FALSE
                 ORDER BY provider_code
                """, (result, row) -> {
            Map<String, Object> content = new java.util.LinkedHashMap<>();
            content.put("displayName", result.getString("display_name"));
            content.put("dialect", result.getString("dialect"));
            content.put("transportType", result.getString("transport_type"));
            content.put(
                    "endpointReference",
                    result.getString("endpoint_reference")
            );
            put(content, "authProfileReference", result.getString(
                    "auth_profile_reference"
            ));
            put(content, "tlsProfileReference", result.getString(
                    "tls_profile_reference"
            ));
            put(content, "capabilityFingerprint", result.getString(
                    "capability_fingerprint"
            ));
            content.put("status", result.getString("status"));
            return new RemoteProviderDraft(
                    result.getString("id"),
                    result.getString("gateway_group_id"),
                    result.getString("provider_code"),
                    content,
                    result.getBoolean("enabled"),
                    result.getLong("revision")
            );
        }, gatewayGroupId);
    }

    /**
     * 中文说明：执行 save提供方 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save provider operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.saveProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save提供方 的处理结果；returns the result of the operation.
     */
    public Mutation saveProvider(
            RemoteProviderDraft provider,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateRevision(expectedRevision);
        Map<String, Object> content = provider.content();
        Object[] mutable = providerValues(provider, content, actor, now);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_remote_provider
                   SET provider_code = ?, display_name = ?, dialect = ?,
                       transport_type = ?, endpoint_reference = ?,
                       auth_profile_reference = ?, tls_profile_reference = ?,
                       capability_fingerprint = ?, status = ?, enabled = ?,
                       revision = revision + 1,
                       updated_at = ?, updated_by = ?
                 WHERE id = ? AND revision = ? AND deleted = FALSE
                """, append(mutable, provider.id(), expectedRevision));
        if (updated == 1) {
            return new Mutation(provider.id(), expectedRevision + 1);
        }
        Long currentRevision = currentRevision(
                "gateway_mcp_remote_provider",
                provider.id()
        );
        if (currentRevision != null || expectedRevision != 0) {
            throw revisionConflict(currentRevision);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_provider(
                    id, gateway_group_id, provider_code, display_name,
                    dialect, transport_type, endpoint_reference,
                    auth_profile_reference, tls_profile_reference,
                    capability_fingerprint, status, enabled,
                    revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    0, FALSE, ?, ?, ?, ?
                )
                """,
                provider.id(),
                provider.gatewayGroupId(),
                provider.providerCode(),
                required(content, "displayName"),
                required(content, "dialect"),
                required(content, "transportType"),
                required(content, "endpointReference"),
                optional(content, "authProfileReference"),
                optional(content, "tlsProfileReference"),
                optional(content, "capabilityFingerprint"),
                content.getOrDefault("status", "CONFIGURED").toString(),
                provider.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new Mutation(provider.id(), 0);
    }

    /**
     * 中文说明：执行 capabilities 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the capabilities operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.capabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerId 参数 提供方Id；parameter provider id。
     * @return 返回 capabilities 的处理结果；returns the result of the operation.
     */
    public List<RemoteCapability> capabilities(String providerId) {
        return jdbc.query("""
                SELECT id, provider_id, primitive_type, remote_name,
                       descriptor::text AS descriptor,
                       capability_fingerprint, synced_at
                  FROM gateway_mcp_remote_capability
                 WHERE provider_id = ?
                 ORDER BY primitive_type, remote_name
                """, (result, row) -> new RemoteCapability(
                result.getString("id"),
                result.getString("provider_id"),
                result.getString("primitive_type"),
                result.getString("remote_name"),
                json.map(result.getString("descriptor")),
                result.getString("capability_fingerprint"),
                result.getTimestamp("synced_at").toInstant()
        ), providerId);
    }

    /**
     * 中文说明：执行 replaceCapabilities 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the replace capabilities operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.replaceCapabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param fingerprint 参数 fingerprint；parameter fingerprint。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param syncedAt 参数 syncedAt；parameter synced at。
     */
    @Transactional
    public void replaceCapabilities(
            String providerId,
            String fingerprint,
            List<RemoteCapability> capabilities,
            Instant syncedAt) {
        jdbc.update(
                "DELETE FROM gateway_mcp_remote_capability "
                        + "WHERE provider_id = ?",
                providerId
        );
        for (RemoteCapability capability : capabilities) {
            jdbc.update("""
                    INSERT INTO gateway_mcp_remote_capability(
                        id, provider_id, primitive_type, remote_name,
                        descriptor, capability_fingerprint, synced_at
                    ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?)
                    """,
                    capability.id(),
                    providerId,
                    capability.primitiveType(),
                    capability.remoteName(),
                    json.write(capability.descriptor()),
                    fingerprint,
                    McpJdbcJson.timestamp(syncedAt)
            );
        }
        jdbc.update("""
                UPDATE gateway_mcp_remote_provider
                   SET capability_fingerprint = ?,
                       status = 'SYNCED',
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ? AND deleted = FALSE
                """, fingerprint, McpJdbcJson.timestamp(syncedAt), providerId);
    }

    /**
     * 中文说明：执行 mounts 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mounts operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.mounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 mounts 的处理结果；returns the result of the operation.
     */
    public List<RemoteMountDraft> mounts(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, server_id, provider_id,
                       namespace, capability_fingerprint,
                       content::text AS content, enabled, revision
                  FROM gateway_mcp_remote_mount_draft
                 WHERE gateway_group_id = ? AND deleted = FALSE
                 ORDER BY server_id, namespace
                """, (result, row) -> new RemoteMountDraft(
                result.getString("id"),
                result.getString("gateway_group_id"),
                result.getString("server_id"),
                result.getString("provider_id"),
                result.getString("namespace"),
                result.getString("capability_fingerprint"),
                json.map(result.getString("content")),
                result.getBoolean("enabled"),
                result.getLong("revision")
        ), gatewayGroupId);
    }

    /**
     * 中文说明：执行 saveMount 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save mount operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.saveMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mount 参数 mount；parameter mount。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 saveMount 的处理结果；returns the result of the operation.
     */
    public Mutation saveMount(
            RemoteMountDraft mount,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE gateway_mcp_remote_mount_draft
                   SET server_id = ?, provider_id = ?, namespace = ?,
                       capability_fingerprint = ?, content = ?::jsonb,
                       enabled = ?, revision = revision + 1,
                       updated_at = ?, updated_by = ?
                 WHERE id = ? AND revision = ? AND deleted = FALSE
                """,
                mount.serverId(),
                mount.providerId(),
                mount.namespace(),
                mount.capabilityFingerprint(),
                json.write(mount.content()),
                mount.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                mount.id(),
                expectedRevision
        );
        if (updated == 1) {
            return new Mutation(mount.id(), expectedRevision + 1);
        }
        Long currentRevision = currentRevision(
                "gateway_mcp_remote_mount_draft",
                mount.id()
        );
        if (currentRevision != null || expectedRevision != 0) {
            throw revisionConflict(currentRevision);
        }
        jdbc.update("""
                INSERT INTO gateway_mcp_remote_mount_draft(
                    id, gateway_group_id, server_id, provider_id, namespace,
                    capability_fingerprint, content, enabled,
                    revision, deleted, created_at, created_by,
                    updated_at, updated_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?::jsonb, ?,
                    0, FALSE, ?, ?, ?, ?
                )
                """,
                mount.id(),
                mount.gatewayGroupId(),
                mount.serverId(),
                mount.providerId(),
                mount.namespace(),
                mount.capabilityFingerprint(),
                json.write(mount.content()),
                mount.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        );
        return new Mutation(mount.id(), 0);
    }

    /**
     * 中文说明：执行 softDelete提供方 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete provider operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.softDeleteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete提供方 的处理结果；returns the result of the operation.
     */
    public Mutation softDeleteProvider(
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        return softDelete(
                "gateway_mcp_remote_provider",
                id,
                expectedRevision,
                actor,
                now
        );
    }

    /**
     * 中文说明：执行 softDeleteMount 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete mount operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.softDeleteMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDeleteMount 的处理结果；returns the result of the operation.
     */
    public Mutation softDeleteMount(
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        return softDelete(
                "gateway_mcp_remote_mount_draft",
                id,
                expectedRevision,
                actor,
                now
        );
    }

    /**
     * 中文说明：执行 softDelete 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.softDelete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param table 参数 table；parameter table。
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete 的处理结果；returns the result of the operation.
     */
    private Mutation softDelete(
            String table,
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateRevision(expectedRevision);
        int updated = jdbc.update("""
                UPDATE %s
                   SET deleted = TRUE, enabled = FALSE,
                       revision = revision + 1,
                       updated_at = ?, updated_by = ?
                 WHERE id = ? AND revision = ? AND deleted = FALSE
                """.formatted(table),
                McpJdbcJson.timestamp(now),
                actorId(actor),
                id,
                expectedRevision
        );
        if (updated != 1) {
            throw revisionConflict(currentRevision(table, id));
        }
        return new Mutation(id, expectedRevision + 1);
    }

    /**
     * 中文说明：执行 提供方Values 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider values operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.providerValues(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param content 参数 content；parameter content。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 提供方Values 的处理结果；returns the result of the operation.
     */
    private Object[] providerValues(
            RemoteProviderDraft provider,
            Map<String, Object> content,
            AdminActor actor,
            Instant now) {
        return new Object[]{
                provider.providerCode(),
                required(content, "displayName"),
                required(content, "dialect"),
                required(content, "transportType"),
                required(content, "endpointReference"),
                optional(content, "authProfileReference"),
                optional(content, "tlsProfileReference"),
                optional(content, "capabilityFingerprint"),
                content.getOrDefault("status", "CONFIGURED").toString(),
                provider.enabled(),
                McpJdbcJson.timestamp(now),
                actorId(actor)
        };
    }

    /**
     * 中文说明：执行 append 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param tail 参数 tail；parameter tail。
     * @return 返回 append 的处理结果；returns the result of the operation.
     */
    private Object[] append(Object[] values, Object... tail) {
        Object[] result = java.util.Arrays.copyOf(
                values,
                values.length + tail.length
        );
        System.arraycopy(tail, 0, result, values.length, tail.length);
        return result;
    }

    /**
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param table 参数 table；parameter table。
     * @param id 参数 id；parameter id。
     * @return 返回 currentRevision 的处理结果；returns the result of the operation.
     */
    private Long currentRevision(
            String table,
            String id) {
        List<Long> values = jdbc.query(
                "SELECT revision FROM " + table + " WHERE id = ?",
                (result, row) -> result.getLong("revision"),
                id
        );
        return values.stream().findFirst().orElse(null);
    }

    /**
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param current 参数 current；parameter current。
     * @return 返回 revisionConflict 的处理结果；returns the result of the operation.
     */
    private GatewayAdminRevisionConflictException revisionConflict(
            Long current) {
        return new GatewayAdminRevisionConflictException(
                current == null ? -1 : current
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param name 参数 name；parameter name。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(Map<String, Object> content, String name) {
        Object value = content.get(name);
        return McpJdbcJson.required(
                value == null ? null : value.toString(),
                name
        );
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param name 参数 name；parameter name。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private String optional(Map<String, Object> content, String name) {
        Object value = content.get(name);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().trim();
    }

    /**
     * 中文说明：执行 put 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.put(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     */
    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 中文说明：执行 validateRevision 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate revision operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.validateRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param revision 参数 revision；parameter revision。
     */
    private void validateRevision(long revision) {
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    /**
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpRemoteProviderStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderStore.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }

    /**
     * 中文说明：{@code RemoteProviderDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程提供方草稿相关的职责与边界。
     * English summary: {@code RemoteProviderDraft} is an immutable data carrier in the current Gateway module; it owns the remote provider draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param providerCode 参数 提供方Code；parameter provider code。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public record RemoteProviderDraft(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 提供方Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider code; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerCode,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpRemoteProviderStore.RemoteProviderDraft} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param providerCode 参数 提供方Code；parameter provider code。
         * @param content 参数 content；parameter content。
         * @param enabled 参数 enabled；parameter enabled。
         * @param revision 参数 revision；parameter revision。
         */
        public RemoteProviderDraft {
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            providerCode = McpJdbcJson.required(
                    providerCode,
                    "providerCode"
            );
            content = Map.copyOf(Objects.requireNonNull(content, "content"));
        }
    }

    /**
     * 中文说明：{@code RemoteCapability} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程Capability相关的职责与边界。
     * English summary: {@code RemoteCapability} is an immutable data carrier in the current Gateway module; it owns the remote capability-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @param remoteName 参数 远程Name；parameter remote name。
     * @param descriptor 参数 descriptor；parameter descriptor。
     * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
     * @param syncedAt 参数 syncedAt；parameter synced at。
     */
    public record RemoteCapability(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerId,
            /**
             * 中文说明：保存 primitiveType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by primitive type; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String primitiveType,
            /**
             * 中文说明：保存 远程Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote name; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteName,
            /**
             * 中文说明：保存 descriptor 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by descriptor; its type is {@code Map<String, Object>}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> descriptor,
            /**
             * 中文说明：保存 capabilityFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capability fingerprint; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            String capabilityFingerprint,
            /**
             * 中文说明：保存 syncedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by synced at; its type is {@code Instant}, and {@code JdbcMcpRemoteProviderStore.RemoteCapability} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteCapability} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteCapability}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant syncedAt
    ) {
    }

    /**
     * 中文说明：{@code RemoteMountDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程Mount草稿相关的职责与边界。
     * English summary: {@code RemoteMountDraft} is an immutable data carrier in the current Gateway module; it owns the remote mount draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public record RemoteMountDraft(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerId,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 capabilityFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capability fingerprint; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String capabilityFingerprint,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {

        /**
         * 中文说明：创建 {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcMcpRemoteProviderStore.RemoteMountDraft} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param serverId 参数 服务器Id；parameter server id。
         * @param providerId 参数 提供方Id；parameter provider id。
         * @param namespace 参数 命名空间；parameter namespace。
         * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
         * @param content 参数 content；parameter content。
         * @param enabled 参数 enabled；parameter enabled。
         * @param revision 参数 revision；parameter revision。
         */
        public RemoteMountDraft {
            id = McpJdbcJson.required(id, "id");
            gatewayGroupId = McpJdbcJson.required(
                    gatewayGroupId,
                    "gatewayGroupId"
            );
            serverId = McpJdbcJson.required(serverId, "serverId");
            providerId = McpJdbcJson.required(providerId, "providerId");
            namespace = McpJdbcJson.required(namespace, "namespace");
            capabilityFingerprint = McpJdbcJson.required(
                    capabilityFingerprint,
                    "capabilityFingerprint"
            );
            content = Map.copyOf(Objects.requireNonNull(content, "content"));
        }
    }

    /**
     * 中文说明：{@code Mutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Mutation相关的职责与边界。
     * English summary: {@code Mutation} is an immutable data carrier in the current Gateway module; it owns the mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param revision 参数 revision；parameter revision。
     */
    public record Mutation(
    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcMcpRemoteProviderStore.Mutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcMcpRemoteProviderStore.Mutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.Mutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.Mutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    String id,
    /**
     * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcMcpRemoteProviderStore.Mutation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code JdbcMcpRemoteProviderStore.Mutation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderStore.Mutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderStore.Mutation}; do not couple callers to its representation when the owning type exposes an API.
     */
    long revision) {
    }
}
