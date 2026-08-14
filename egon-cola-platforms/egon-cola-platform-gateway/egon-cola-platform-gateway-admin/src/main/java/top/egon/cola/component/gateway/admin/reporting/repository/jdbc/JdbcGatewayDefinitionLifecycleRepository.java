package top.egon.cola.component.gateway.admin.reporting.repository.jdbc;


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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.gateway.admin.reporting.repository.GatewayDefinitionLifecycleRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code JdbcGatewayDefinitionLifecycleRepository} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关定义生命周期存储相关的职责与边界。
 * English summary: {@code JdbcGatewayDefinitionLifecycleRepository} is a jdbc gateway definition lifecycle store store in the current Gateway module; it owns the jdbc gateway definition lifecycle store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayDefinitionLifecycleRepository
        implements GatewayDefinitionLifecycleRepository {

    /**
     * 中文说明：表示 NOACTIVE定义SET 这一固定值；它属于 {@code JdbcGatewayDefinitionLifecycleRepository} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value no active definition set; it is a state, type, or protocol value of {@code JdbcGatewayDefinitionLifecycleRepository} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionLifecycleRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionLifecycleRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String NO_ACTIVE_DEFINITION_SET =
            "__gateway_no_active_definition_set__";

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code NamedParameterJdbcTemplate}，由 {@code JdbcGatewayDefinitionLifecycleRepository} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code NamedParameterJdbcTemplate}, and {@code JdbcGatewayDefinitionLifecycleRepository} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionLifecycleRepository} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionLifecycleRepository}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * 中文说明：创建 {@code JdbcGatewayDefinitionLifecycleRepository} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayDefinitionLifecycleRepository} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     */
    public JdbcGatewayDefinitionLifecycleRepository(
            NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 中文说明：执行 reconcile 操作；该方法是 {@code JdbcGatewayDefinitionLifecycleRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reconcile operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionLifecycleRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionLifecycleRepository.reconcile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activeDefinitionSetIds 参数 active定义SetIds；parameter active definition set ids。
     * @param now 参数 now；parameter now。
     * @return 返回 reconcile 的处理结果；returns the result of the operation.
     */
    @Override
    public GatewayReconcileResultVO reconcile(
            Set<String> activeDefinitionSetIds,
            Instant now) {
        Set<String> activeIds = activeDefinitionSetIds == null
                ? Set.of()
                : Set.copyOf(activeDefinitionSetIds);
        Map<String, Set<String>> applications = new LinkedHashMap<>();
        MapSqlParameterSource scanParameters = new MapSqlParameterSource();
        String scanSql;
        if (activeIds.isEmpty()) {
            scanSql = """
                    SELECT id, application_id
                      FROM gateway_definition_set
                     WHERE status = 'ACTIVE'
                    """;
        } else {
            scanSql = """
                    SELECT id, application_id
                      FROM gateway_definition_set
                     WHERE status = 'ACTIVE'
                        OR id IN (:definitionSetIds)
                    """;
            scanParameters.addValue("definitionSetIds", activeIds);
        }
        jdbc.query(
                scanSql,
                scanParameters,
                (org.springframework.jdbc.core.RowCallbackHandler) result ->
                        collectApplication(
                                applications,
                                activeIds,
                                result.getString("application_id"),
                                result.getString("id")
                        )
        );
        int activatedSets = 0;
        int retiredSets = 0;
        int activatedOperations = 0;
        int offlinedOperations = 0;
        for (Map.Entry<String, Set<String>> application
                : applications.entrySet()) {
            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("applicationId", application.getKey())
                    .addValue(
                            "definitionSetIds",
                            application.getValue().isEmpty()
                                    ? Set.of(NO_ACTIVE_DEFINITION_SET)
                                    : application.getValue()
                    )
                    .addValue("now", Timestamp.from(now));
            activatedSets += jdbc.update("""
                    UPDATE gateway_definition_set
                       SET status = 'ACTIVE',
                           activated_at = COALESCE(activated_at, :now),
                           retired_at = NULL
                     WHERE application_id = :applicationId
                       AND id IN (:definitionSetIds)
                       AND status <> 'ACTIVE'
                    """, parameters);
            retiredSets += jdbc.update("""
                    UPDATE gateway_definition_set
                       SET status = 'RETIRED', retired_at = :now
                     WHERE application_id = :applicationId
                       AND status = 'ACTIVE'
                       AND id NOT IN (:definitionSetIds)
                    """, parameters);
            activatedOperations += jdbc.update("""
                    WITH selected AS (
                        SELECT DISTINCT ON (membership.operation_id)
                               membership.operation_id,
                               membership.definition_id,
                               membership.method_identity,
                               membership.provider_service_identity,
                               membership.external_accessible,
                               membership.deprecated
                          FROM gateway_definition_set_operation membership
                          JOIN gateway_definition_set definition_set
                            ON definition_set.id =
                               membership.definition_set_id
                         WHERE definition_set.application_id =
                               :applicationId
                           AND definition_set.id IN (:definitionSetIds)
                         ORDER BY membership.operation_id,
                                  definition_set.received_at DESC
                    )
                    UPDATE gateway_operation operation
                       SET current_definition_id = selected.definition_id,
                           method_identity = selected.method_identity,
                           provider_service_identity =
                               selected.provider_service_identity,
                           external_accessible =
                               selected.external_accessible,
                           lifecycle_status = CASE
                               WHEN selected.deprecated THEN 'DEPRECATED'
                               ELSE 'ACTIVE'
                           END,
                           deprecated_at = CASE
                               WHEN selected.deprecated
                                   THEN CAST(:now AS TIMESTAMP WITH TIME ZONE)
                               ELSE NULL
                           END,
                           revision = operation.revision + 1,
                           updated_at = :now
                      FROM selected
                     WHERE operation.id = selected.operation_id
                       AND (
                           operation.current_definition_id IS DISTINCT FROM
                               selected.definition_id
                           OR operation.method_identity IS DISTINCT FROM
                               selected.method_identity
                           OR operation.provider_service_identity
                               IS DISTINCT FROM
                               selected.provider_service_identity
                           OR operation.external_accessible IS DISTINCT FROM
                               selected.external_accessible
                           OR operation.lifecycle_status IS DISTINCT FROM
                               CASE WHEN selected.deprecated
                                    THEN 'DEPRECATED' ELSE 'ACTIVE' END
                       )
                    """, parameters);
            offlinedOperations += jdbc.update("""
                    UPDATE gateway_operation operation
                       SET lifecycle_status = 'OFFLINE',
                           revision = operation.revision + 1,
                           updated_at = :now
                     WHERE operation.application_id = :applicationId
                       AND operation.source_type = 'STARTER'
                       AND operation.lifecycle_status <> 'OFFLINE'
                       AND NOT EXISTS (
                           SELECT 1
                             FROM gateway_definition_set_operation membership
                            WHERE membership.operation_id = operation.id
                              AND membership.definition_set_id
                                  IN (:definitionSetIds)
                       )
                    """, parameters);
        }
        return new GatewayReconcileResultVO(
                activatedSets,
                retiredSets,
                activatedOperations,
                offlinedOperations
        );
    }

    /**
     * 中文说明：执行 collectApplication 操作；该方法是 {@code JdbcGatewayDefinitionLifecycleRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the collect application operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionLifecycleRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionLifecycleRepository.collectApplication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applications 参数 applications；parameter applications。
     * @param activeDefinitionSetIds 参数 active定义SetIds；parameter active definition set ids。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     */
    private void collectApplication(
            Map<String, Set<String>> applications,
            Set<String> activeDefinitionSetIds,
            String applicationId,
            String definitionSetId) {
        Set<String> applicationActiveSets = applications.computeIfAbsent(
                applicationId,
                ignored -> new LinkedHashSet<>()
        );
        if (activeDefinitionSetIds.contains(definitionSetId)) {
            applicationActiveSets.add(definitionSetId);
        }
    }
}
