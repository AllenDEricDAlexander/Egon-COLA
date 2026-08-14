package top.egon.cola.component.gateway.admin.mcp.repository.jdbc;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
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
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;


import top.egon.cola.component.gateway.admin.mcp.domain.po.McpManagedToolOverridePO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolDraftMutationDTO;
/**
 * 中文说明：{@code JdbcMcpManagedToolOverrideRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCPManaged工具Override存储相关的职责与边界。
 * English summary: {@code JdbcMcpManagedToolOverrideRepository} is a jdbc mcp managed tool override store store in the current Gateway module; it owns the jdbc mcp managed tool override store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpManagedToolOverrideRepository {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpManagedToolOverrideRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpManagedToolOverrideRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpManagedToolOverrideRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpManagedToolOverrideRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpManagedToolOverrideRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpManagedToolOverrideRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpManagedToolOverrideRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpManagedToolOverrideRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpManagedToolOverrideRepository(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
    public List<McpManagedToolOverridePO> load(String gatewayGroupId) {
        return jdbc.query("""
                SELECT tool_id, gateway_group_id, operation_id, server_id,
                       additional_permissions::text AS additional_permissions,
                       minimum_risk_level, enabled, revision
                  FROM gateway_mcp_managed_tool_override
                 WHERE gateway_group_id = ?
                 ORDER BY operation_id
                """, (result, row) -> new McpManagedToolOverridePO(
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

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param override 参数 override；parameter override。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save 的处理结果；returns the result of the operation.
     */
    public McpManagedToolDraftMutationDTO save(
            McpManagedToolOverridePO override,
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
            return new McpManagedToolDraftMutationDTO(override.toolId(), expectedRevision + 1);
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
        return new McpManagedToolDraftMutationDTO(override.toolId(), 0);
    }

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 delete 的处理结果；returns the result of the operation.
     */
    public McpManagedToolDraftMutationDTO delete(
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
        return new McpManagedToolDraftMutationDTO(toolId, expectedRevision + 1);
    }

    /**
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 currentRevision 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 validateExpectedRevision 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate expected revision operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.validateExpectedRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpManagedToolOverrideRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpManagedToolOverrideRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpManagedToolOverrideRepository.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }




}
