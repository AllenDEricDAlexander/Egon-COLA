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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityRecordPO;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityDraftMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.McpCapabilityBinding;
/**
 * 中文说明：{@code JdbcMcpCapabilityDraftRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责JdbcMCPCapability草稿存储相关的职责与边界。
 * English summary: {@code JdbcMcpCapabilityDraftRepository} is a jdbc mcp capability draft store store in the current Gateway module; it owns the jdbc mcp capability draft store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcMcpCapabilityDraftRepository {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcMcpCapabilityDraftRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcMcpCapabilityDraftRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 json 对应的状态、依赖或配置值；字段类型为 {@code McpJdbcJson}，由 {@code JdbcMcpCapabilityDraftRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by json; its type is {@code McpJdbcJson}, and {@code JdbcMcpCapabilityDraftRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcMcpCapabilityDraftRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcMcpCapabilityDraftRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpJdbcJson json;

    /**
     * 中文说明：创建 {@code JdbcMcpCapabilityDraftRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcMcpCapabilityDraftRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcMcpCapabilityDraftRepository(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        json = new McpJdbcJson(objectMapper);
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
    public McpCapabilityDraftPO load(String gatewayGroupId) {
        String groupId = McpJdbcJson.required(
                gatewayGroupId,
                "gatewayGroupId"
        );
        EnumMap<McpCapabilityKindEnum, List<McpCapabilityRecordPO>> values =
                new EnumMap<>(McpCapabilityKindEnum.class);
        for (McpCapabilityKindEnum kind : McpCapabilityKindEnum.values()) {
            values.put(kind, load(kind, groupId));
        }
        return new McpCapabilityDraftPO(groupId, values);
    }

    /**
     * 中文说明：执行 save 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the save operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.save(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 save 的处理结果；returns the result of the operation.
     */
    public McpCapabilityDraftMutationDTO save(
            McpCapabilityRecordPO draft,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(draft, "draft");
        validateExpectedRevision(expectedRevision);
        McpCapabilityBinding binding = binding(draft);
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
            return new McpCapabilityDraftMutationDTO(draft.id(), expectedRevision + 1);
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
        return new McpCapabilityDraftMutationDTO(draft.id(), 0);
    }

    /**
     * 中文说明：执行 softDelete 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the soft delete operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.softDelete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param actor 参数 actor；parameter actor。
     * @param now 参数 now；parameter now。
     * @return 返回 softDelete 的处理结果；returns the result of the operation.
     */
    public McpCapabilityDraftMutationDTO softDelete(
            McpCapabilityKindEnum kind,
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
        return new McpCapabilityDraftMutationDTO(id, expectedRevision + 1);
    }

    /**
     * 中文说明：执行 load 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.load(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 load 的处理结果；returns the result of the operation.
     */
    private List<McpCapabilityRecordPO> load(
            McpCapabilityKindEnum kind,
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
        return jdbc.query(sql, (result, row) -> new McpCapabilityRecordPO(
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
     * 中文说明：执行 updateSql 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update sql operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.updateSql(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param binding 参数 binding；parameter binding。
     * @return 返回 updateSql 的处理结果；returns the result of the operation.
     */
    private String updateSql(McpCapabilityKindEnum kind, McpCapabilityBinding binding) {
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
     * 中文说明：执行 insertSql 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert sql operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.insertSql(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param binding 参数 binding；parameter binding。
     * @return 返回 insertSql 的处理结果；returns the result of the operation.
     */
    private String insertSql(McpCapabilityKindEnum kind, McpCapabilityBinding binding) {
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
     * 中文说明：执行 binding 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the binding operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.binding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @return 返回 binding 的处理结果；returns the result of the operation.
     */
    private McpCapabilityBinding binding(McpCapabilityRecordPO draft) {
        Map<String, Object> content = draft.content();
        return switch (draft.kind()) {
            case RESOURCE -> new McpCapabilityBinding(
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
            case RESOURCE_TEMPLATE -> new McpCapabilityBinding(
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
            case PROMPT -> new McpCapabilityBinding(
                    "source_type, operation_id, remote_mount_id",
                    "source_type = ?, operation_id = ?, remote_mount_id = ?",
                    nullableValues(
                            requiredContent(content, "sourceType"),
                            optionalContent(content, "operationId"),
                            optionalContent(content, "remoteMountId")
                    )
            );
            case TASK_POLICY -> McpCapabilityBinding.none();
            case APP_BINDING -> new McpCapabilityBinding(
                    "app_artifact_id",
                    "app_artifact_id = ?",
                    List.of(requiredContent(content, "appArtifactId"))
            );
        };
    }

    /**
     * 中文说明：执行 requiredContent 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required content operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.requiredContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 optionalContent 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional content operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.optionalContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 nullableValues 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the nullable values operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.nullableValues(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @return 返回 nullableValues 的处理结果；returns the result of the operation.
     */
    private List<Object> nullableValues(Object... values) {
        return Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(values))
        );
    }

    /**
     * 中文说明：执行 currentRevision 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the current revision operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.currentRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @param id 参数 id；parameter id。
     * @return 返回 currentRevision 的处理结果；returns the result of the operation.
     */
    private Long currentRevision(
            McpCapabilityKindEnum kind,
            String id) {
        List<Long> values = jdbc.query(
                "SELECT revision FROM " + kind.table() + " WHERE id = ?",
                (result, row) -> result.getLong("revision"),
                id
        );
        return values.stream().findFirst().orElse(null);
    }

    /**
     * 中文说明：执行 revisionConflict 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revision conflict operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.revisionConflict(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 validateExpectedRevision 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate expected revision operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.validateExpectedRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 actorId 操作；该方法是 {@code JdbcMcpCapabilityDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the actor id operation; this method is the invocation entry point on {@code JdbcMcpCapabilityDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcMcpCapabilityDraftRepository.actorId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @return 返回 actorId 的处理结果；returns the result of the operation.
     */
    private String actorId(AdminActor actor) {
        return Objects.requireNonNull(actor, "actor").actorId();
    }










}
