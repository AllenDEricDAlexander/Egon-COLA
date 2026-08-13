package top.egon.cola.component.gateway.admin.mcp.repository.jdbc;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteProviderDraftPO;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteProviderDraftMutationDTO;
/**
 * 中文说明：{@code JdbcMcpRemoteProviderRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCP远程提供方存储相关的职责与边界。
 * English summary: {@code JdbcMcpRemoteProviderRepository} is a jdbc mcp remote provider store store in the current Gateway module; it owns the jdbc mcp remote provider store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpRemoteProviderRepository {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpRemoteProviderRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpRemoteProviderRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpRemoteProviderRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpRemoteProviderRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpRemoteProviderRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpRemoteProviderRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpRemoteProviderRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpRemoteProviderRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpRemoteProviderRepository(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 providers 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the providers operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.providers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 providers 的处理结果；returns the result of the operation.
     */
    public List<McpRemoteProviderDraftPO> providers(String gatewayGroupId) {
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
            return new McpRemoteProviderDraftPO(
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
     * 中文说明：执行 save提供方 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save provider operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.saveProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save提供方 的处理结果；returns the result of the operation.
     */
    public McpRemoteProviderDraftMutationDTO saveProvider(
            McpRemoteProviderDraftPO provider,
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
            return new McpRemoteProviderDraftMutationDTO(provider.id(), expectedRevision + 1);
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
        return new McpRemoteProviderDraftMutationDTO(provider.id(), 0);
    }

    /**
     * 中文说明：执行 capabilities 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the capabilities operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.capabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerId 参数 提供方Id；parameter provider id。
     * @return 返回 capabilities 的处理结果；returns the result of the operation.
     */
    public List<McpRemoteCapabilityPO> capabilities(String providerId) {
        return jdbc.query("""
                SELECT id, provider_id, primitive_type, remote_name,
                       descriptor::text AS descriptor,
                       capability_fingerprint, synced_at
                  FROM gateway_mcp_remote_capability
                 WHERE provider_id = ?
                 ORDER BY primitive_type, remote_name
                """, (result, row) -> new McpRemoteCapabilityPO(
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
     * 中文说明：执行 replaceCapabilities 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the replace capabilities operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.replaceCapabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param fingerprint 参数 fingerprint；parameter fingerprint。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param syncedAt 参数 syncedAt；parameter synced at。
     */
    @Transactional
    public void replaceCapabilities(
            String providerId,
            String fingerprint,
            List<McpRemoteCapabilityPO> capabilities,
            Instant syncedAt) {
        jdbc.update(
                "DELETE FROM gateway_mcp_remote_capability "
                        + "WHERE provider_id = ?",
                providerId
        );
        for (McpRemoteCapabilityPO capability : capabilities) {
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
     * 中文说明：执行 mounts 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mounts operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.mounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 mounts 的处理结果；returns the result of the operation.
     */
    public List<McpRemoteMountDraftPO> mounts(String gatewayGroupId) {
        return jdbc.query("""
                SELECT id, gateway_group_id, server_id, provider_id,
                       namespace, capability_fingerprint,
                       content::text AS content, enabled, revision
                  FROM gateway_mcp_remote_mount_draft
                 WHERE gateway_group_id = ? AND deleted = FALSE
                 ORDER BY server_id, namespace
                """, (result, row) -> new McpRemoteMountDraftPO(
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
     * 中文说明：执行 saveMount 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save mount operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.saveMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mount 参数 mount；parameter mount。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 saveMount 的处理结果；returns the result of the operation.
     */
    public McpRemoteProviderDraftMutationDTO saveMount(
            McpRemoteMountDraftPO mount,
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
            return new McpRemoteProviderDraftMutationDTO(mount.id(), expectedRevision + 1);
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
        return new McpRemoteProviderDraftMutationDTO(mount.id(), 0);
    }

    /**
     * 中文说明：执行 softDelete提供方 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete provider operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.softDeleteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete提供方 的处理结果；returns the result of the operation.
     */
    public McpRemoteProviderDraftMutationDTO softDeleteProvider(
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
     * 中文说明：执行 softDeleteMount 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete mount operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.softDeleteMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDeleteMount 的处理结果；returns the result of the operation.
     */
    public McpRemoteProviderDraftMutationDTO softDeleteMount(
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
     * 中文说明：执行 softDelete 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.softDelete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param table 参数 table；parameter table。
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete 的处理结果；returns the result of the operation.
     */
    private McpRemoteProviderDraftMutationDTO softDelete(
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
        return new McpRemoteProviderDraftMutationDTO(id, expectedRevision + 1);
    }

    /**
     * 中文说明：执行 提供方Values 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider values operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.providerValues(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param content 参数 content；parameter content。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 提供方Values 的处理结果；returns the result of the operation.
     */
    private Object[] providerValues(
            McpRemoteProviderDraftPO provider,
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
     * 中文说明：执行 append 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 required 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 optional 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 put 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.put(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 validateRevision 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate revision operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.validateRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpRemoteProviderRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpRemoteProviderRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpRemoteProviderRepository.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }








}
