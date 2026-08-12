package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

/**
 * 中文说明：{@code JdbcGatewayCatalogStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关目录存储相关的职责与边界。
 * English summary: {@code JdbcGatewayCatalogStore} is a jdbc gateway catalog store store in the current Gateway module; it owns the jdbc gateway catalog store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayCatalogStore implements GatewayCatalogStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayCatalogStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayCatalogStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code JdbcGatewayCatalogStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code JdbcGatewayCatalogStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code JdbcGatewayCatalogStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayCatalogStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public JdbcGatewayCatalogStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * 中文说明：执行 load目录 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load catalog operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.loadCatalog(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 load目录 的处理结果；returns the result of the operation.
     */
    @Override
    public CatalogTree loadCatalog(String applicationId) {
        Map<String, MutableBusiness> businesses = new LinkedHashMap<>();
        jdbc.query("""
                SELECT b.id AS business_id,
                       b.code AS business_code,
                       b.display_name AS business_name,
                       e.id AS entity_id,
                       e.code AS entity_code,
                       e.display_name AS entity_name,
                       g.id AS group_id,
                       g.code AS group_code,
                       g.display_name AS group_name,
                       g.source_type,
                       g.class_name,
                       o.id AS operation_id,
                       o.operation_key,
                       o.protocol,
                       o.method_identity,
                       o.external_accessible,
                       o.lifecycle_status,
                       o.source_type AS operation_source,
                       o.revision
                  FROM gateway_business_domain b
                  LEFT JOIN gateway_entity_domain e
                    ON e.business_domain_id = b.id AND e.deleted = FALSE
                  LEFT JOIN gateway_interface_group g
                    ON g.entity_domain_id = e.id AND g.deleted = FALSE
                  LEFT JOIN gateway_operation o
                    ON o.interface_group_id = g.id
                 WHERE b.application_id = ? AND b.deleted = FALSE
                 ORDER BY b.code, e.code, g.code, o.operation_key
                """, result -> {
            collect(result, businesses);
            return null;
        }, applicationId);
        return new CatalogTree(
                applicationId,
                businesses.values().stream()
                        .map(MutableBusiness::freeze)
                        .toList()
        );
    }

    /**
     * 中文说明：执行 createManualHierarchy 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create manual hierarchy operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.createManualHierarchy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @param now 参数 now；parameter now。
     * @return 返回 createManualHierarchy 的处理结果；returns the result of the operation.
     */
    @Override
    public String createManualHierarchy(
            String applicationId,
            ManualHierarchy hierarchy,
            Instant now) {
        requireApplication(applicationId);
        String businessId = findOrCreateBusiness(
                applicationId,
                hierarchy,
                now
        );
        String entityId = findOrCreateEntity(
                businessId,
                hierarchy,
                now
        );
        String interfaceGroupId = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_interface_group(
                    id, entity_domain_id, code, display_name, source_type,
                    class_name, description, deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'MANUAL', ?, ?, FALSE, ?, ?)
                """,
                interfaceGroupId,
                entityId,
                hierarchy.interfaceGroupCode(),
                hierarchy.interfaceGroupName(),
                hierarchy.className(),
                hierarchy.description(),
                timestamp(now),
                timestamp(now)
        );
        return interfaceGroupId;
    }

    /**
     * 中文说明：执行 find接口Group 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find interface group operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.findInterfaceGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 find接口Group 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<InterfaceGroupScope> findInterfaceGroup(String id) {
        return jdbc.query("""
                SELECT g.id, a.id AS application_id, a.biz_code,
                       a.application_code,
                       a.env, a.namespace
                  FROM gateway_interface_group g
                  JOIN gateway_entity_domain e ON e.id = g.entity_domain_id
                  JOIN gateway_business_domain b
                    ON b.id = e.business_domain_id
                  JOIN gateway_application a ON a.id = b.application_id
                 WHERE g.id = ? AND g.deleted = FALSE
                   AND e.deleted = FALSE AND b.deleted = FALSE
                   AND a.deleted = FALSE
                """, (result, row) -> new InterfaceGroupScope(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("biz_code"),
                result.getString("application_code"),
                result.getString("env"),
                result.getString("namespace")
        ), id).stream().findFirst();
    }

    /**
     * 中文说明：执行 find操作 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.findOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 find操作 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<OperationRecord> findOperation(String operationId) {
        return queryOperation("WHERE o.id = ?", operationId);
    }

    /**
     * 中文说明：执行 find操作 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.findOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @return 返回 find操作 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<OperationRecord> findOperation(
            String applicationId,
            String operationKey) {
        return queryOperation(
                "WHERE o.application_id = ? AND o.operation_key = ?",
                applicationId,
                operationKey
        );
    }

    /**
     * 中文说明：执行 loadDefinitions 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load definitions operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.loadDefinitions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 loadDefinitions 的处理结果；returns the result of the operation.
     */
    @Override
    public List<OperationDefinition> loadDefinitions(String operationId) {
        return jdbc.query("""
                SELECT id, operation_id, definition_version,
                       definition_sha256, summary, tags::text AS tags,
                       request_schema::text AS request_schema,
                       response_schema::text AS response_schema,
                       error_schema::text AS error_schema,
                       descriptor_snapshot::text AS descriptor_snapshot,
                       attributes::text AS attributes,
                       external_accessible, created_at, created_by
                  FROM gateway_operation_definition
                 WHERE operation_id = ?
                 ORDER BY definition_version DESC
                """, (result, row) -> new OperationDefinition(
                result.getString("id"),
                result.getString("operation_id"),
                result.getLong("definition_version"),
                result.getString("definition_sha256"),
                result.getString("summary"),
                list(result.getString("tags")),
                map(result.getString("request_schema")),
                map(result.getString("response_schema")),
                mapList(result.getString("error_schema")),
                result.getString("descriptor_snapshot") == null
                        ? null
                        : map(result.getString("descriptor_snapshot")),
                map(result.getString("attributes")),
                result.getBoolean("external_accessible"),
                result.getTimestamp("created_at").toInstant(),
                result.getString("created_by")
        ), operationId);
    }

    /**
     * 中文说明：执行 loadCurrent操作Definitions 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the load current operation definitions operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.loadCurrentOperationDefinitions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 loadCurrent操作Definitions 的处理结果；returns the result of the operation.
     */
    @Override
    public List<CurrentOperationDefinition> loadCurrentOperationDefinitions(
            String gatewayGroupId) {
        return jdbc.query("""
                SELECT o.id, o.application_id, o.interface_group_id,
                       o.operation_key, o.protocol, o.method_identity,
                       o.external_accessible,
                       o.provider_service_identity::text AS provider_identity,
                       o.source_type, o.lifecycle_status,
                       o.current_definition_id, o.revision,
                       o.created_at, o.updated_at,
                       d.id AS definition_id,
                       d.definition_version, d.definition_sha256,
                       d.summary, d.tags::text AS definition_tags,
                       d.request_schema::text AS definition_request_schema,
                       d.response_schema::text AS definition_response_schema,
                       d.error_schema::text AS definition_error_schema,
                       d.descriptor_snapshot::text
                           AS definition_descriptor_snapshot,
                       d.attributes::text AS definition_attributes,
                       d.external_accessible AS definition_external_accessible,
                       d.created_at AS definition_created_at,
                       d.created_by AS definition_created_by
                  FROM gateway_group release_group
                  JOIN gateway_application a
                    ON a.env = release_group.env
                   AND a.namespace = release_group.namespace
                   AND a.deleted = FALSE
                  JOIN gateway_operation o ON o.application_id = a.id
                  JOIN gateway_operation_definition d
                    ON d.id = o.current_definition_id
                 WHERE release_group.id = ?
                   AND release_group.deleted = FALSE
                   AND o.lifecycle_status <> 'OFFLINE'
                 ORDER BY a.application_code, o.operation_key
                """, (result, row) -> new CurrentOperationDefinition(
                operation(result),
                new OperationDefinition(
                        result.getString("definition_id"),
                        result.getString("id"),
                        result.getLong("definition_version"),
                        result.getString("definition_sha256"),
                        result.getString("summary"),
                        list(result.getString("definition_tags")),
                        map(result.getString("definition_request_schema")),
                        map(result.getString("definition_response_schema")),
                        mapList(result.getString("definition_error_schema")),
                        result.getString(
                                "definition_descriptor_snapshot"
                        ) == null
                                ? null
                                : map(result.getString(
                                "definition_descriptor_snapshot"
                        )),
                        map(result.getString("definition_attributes")),
                        result.getBoolean(
                                "definition_external_accessible"
                        ),
                        result.getTimestamp("definition_created_at")
                                .toInstant(),
                        result.getString("definition_created_by")
                )
        ), gatewayGroupId);
    }

    /**
     * 中文说明：执行 insert操作 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the insert operation operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.insertOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     */
    @Override
    public void insertOperation(OperationRecord operation) {
        jdbc.update("""
                INSERT INTO gateway_operation(
                    id, application_id, interface_group_id, operation_key,
                    protocol, method_identity, external_accessible,
                    provider_service_identity, source_type, lifecycle_status,
                    current_definition_id, revision, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, NULL, 0, ?, ?)
                """,
                operation.id(),
                operation.applicationId(),
                operation.interfaceGroupId(),
                operation.operationKey(),
                operation.protocol(),
                operation.methodIdentity(),
                operation.externalAccessible(),
                json(operation.providerServiceIdentity()),
                operation.sourceType(),
                operation.lifecycleStatus(),
                timestamp(operation.createdAt()),
                timestamp(operation.updatedAt())
        );
    }

    /**
     * 中文说明：执行 append定义 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append definition operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.appendDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param definition 参数 定义；parameter definition。
     */
    @Override
    public void appendDefinition(OperationDefinition definition) {
        jdbc.update("""
                INSERT INTO gateway_operation_definition(
                    id, operation_id, definition_set_id, definition_version,
                    definition_sha256, summary, tags, request_schema,
                    response_schema, error_schema, descriptor_snapshot,
                    attributes, external_accessible, created_at, created_by
                ) VALUES (?, ?, NULL, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb,
                          ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?)
                """,
                definition.id(),
                definition.operationId(),
                definition.definitionVersion(),
                definition.definitionSha256(),
                definition.summary(),
                json(definition.tags()),
                json(definition.requestSchema()),
                json(definition.responseSchema()),
                json(definition.errorSchema()),
                definition.descriptorSnapshot() == null
                        ? null
                        : json(definition.descriptorSnapshot()),
                json(definition.attributes()),
                definition.externalAccessible(),
                timestamp(definition.createdAt()),
                definition.createdBy()
        );
    }

    /**
     * 中文说明：执行 pointTo定义 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the point to definition operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.pointToDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionId 参数 定义Id；parameter definition id。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void pointToDefinition(
            String operationId,
            String definitionId,
            boolean externalAccessible,
            Instant now) {
        int updated = jdbc.update("""
                UPDATE gateway_operation
                   SET current_definition_id = ?,
                       external_accessible = ?,
                       revision = revision + 1,
                       lifecycle_status = 'ACTIVE',
                       updated_at = ?
                 WHERE id = ?
                """, definitionId, externalAccessible, timestamp(now),
                operationId);
        if (updated == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway operation " + operationId + " was not found"
            );
        }
    }

    /**
     * 中文说明：执行 deprecate 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deprecate operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.deprecate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void deprecate(String operationId, Instant now) {
        int updated = jdbc.update("""
                UPDATE gateway_operation
                   SET lifecycle_status = 'DEPRECATED',
                       deprecated_at = ?,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ?
                """, timestamp(now), timestamp(now), operationId);
        if (updated == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway operation " + operationId + " was not found"
            );
        }
    }

    /**
     * 中文说明：执行 query操作 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.queryOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param where 参数 where；parameter where。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 query操作 的处理结果；returns the result of the operation.
     */
    private Optional<OperationRecord> queryOperation(
            String where,
            Object... arguments) {
        return jdbc.query("""
                SELECT o.id, o.application_id, o.interface_group_id,
                       o.operation_key, o.protocol, o.method_identity,
                       o.external_accessible,
                       o.provider_service_identity::text AS provider_identity,
                       o.source_type, o.lifecycle_status,
                       o.current_definition_id, o.revision,
                       o.created_at, o.updated_at
                  FROM gateway_operation o
                """ + where, (result, row) -> operation(result), arguments)
                .stream()
                .findFirst();
    }

    /**
     * 中文说明：执行 操作 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.operation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 操作 的处理结果；returns the result of the operation.
     */
    private OperationRecord operation(ResultSet result) throws SQLException {
        return new OperationRecord(
                result.getString("id"),
                result.getString("application_id"),
                result.getString("interface_group_id"),
                result.getString("operation_key"),
                result.getString("protocol"),
                result.getString("method_identity"),
                result.getBoolean("external_accessible"),
                map(result.getString("provider_identity")),
                result.getString("source_type"),
                result.getString("lifecycle_status"),
                result.getString("current_definition_id"),
                result.getLong("revision"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()
        );
    }

    /**
     * 中文说明：执行 collect 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the collect operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.collect(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @param businesses 参数 businesses；parameter businesses。
     */
    private void collect(
            ResultSet result,
            Map<String, MutableBusiness> businesses) throws SQLException {
        while (result.next()) {
            String businessId = result.getString("business_id");
            if (businessId == null) {
                continue;
            }
            MutableBusiness business = businesses.computeIfAbsent(
                    businessId,
                    ignored -> new MutableBusiness(
                            businessId,
                            get(result, "business_code"),
                            get(result, "business_name")
                    )
            );
            String entityId = result.getString("entity_id");
            if (entityId == null) {
                continue;
            }
            MutableEntity entity = business.entities.computeIfAbsent(
                    entityId,
                    ignored -> new MutableEntity(
                            entityId,
                            get(result, "entity_code"),
                            get(result, "entity_name")
                    )
            );
            String groupId = result.getString("group_id");
            if (groupId == null) {
                continue;
            }
            MutableGroup group = entity.groups.computeIfAbsent(
                    groupId,
                    ignored -> new MutableGroup(
                            groupId,
                            get(result, "group_code"),
                            get(result, "group_name"),
                            get(result, "source_type"),
                            get(result, "class_name")
                    )
            );
            String operationId = result.getString("operation_id");
            if (operationId != null) {
                group.operations.add(new OperationNode(
                        operationId,
                        get(result, "operation_key"),
                        get(result, "protocol"),
                        get(result, "method_identity"),
                        result.getBoolean("external_accessible"),
                        get(result, "lifecycle_status"),
                        get(result, "operation_source"),
                        result.getLong("revision")
                ));
            }
        }
    }

    /**
     * 中文说明：执行 findOrCreateBusiness 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find or create business operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.findOrCreateBusiness(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @param now 参数 now；parameter now。
     * @return 返回 findOrCreateBusiness 的处理结果；returns the result of the operation.
     */
    private String findOrCreateBusiness(
            String applicationId,
            ManualHierarchy hierarchy,
            Instant now) {
        List<String> existing = jdbc.queryForList("""
                SELECT id FROM gateway_business_domain
                 WHERE application_id = ? AND code = ? AND deleted = FALSE
                """, String.class, applicationId, hierarchy.businessCode());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_business_domain(
                    id, application_id, code, display_name, description,
                    deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, NULL, FALSE, ?, ?)
                """, id, applicationId, hierarchy.businessCode(),
                hierarchy.businessName(), timestamp(now), timestamp(now));
        return id;
    }

    /**
     * 中文说明：执行 findOrCreateEntity 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find or create entity operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.findOrCreateEntity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param businessId 参数 businessId；parameter business id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @param now 参数 now；parameter now。
     * @return 返回 findOrCreateEntity 的处理结果；returns the result of the operation.
     */
    private String findOrCreateEntity(
            String businessId,
            ManualHierarchy hierarchy,
            Instant now) {
        List<String> existing = jdbc.queryForList("""
                SELECT id FROM gateway_entity_domain
                 WHERE business_domain_id = ? AND code = ?
                   AND deleted = FALSE
                """, String.class, businessId, hierarchy.entityCode());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_entity_domain(
                    id, business_domain_id, code, display_name, description,
                    deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, NULL, FALSE, ?, ?)
                """, id, businessId, hierarchy.entityCode(),
                hierarchy.entityName(), timestamp(now), timestamp(now));
        return id;
    }

    /**
     * 中文说明：执行 requireApplication 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require application operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.requireApplication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     */
    private void requireApplication(String applicationId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM gateway_application
                 WHERE id = ? AND deleted = FALSE
                """, Integer.class, applicationId);
        if (count == null || count == 0) {
            throw new GatewayAdminNotFoundException(
                    "gateway application " + applicationId + " was not found"
            );
        }
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "catalog value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> map(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored provider identity is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    private List<String> list(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<List<String>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("stored tags are invalid", failure);
        }
    }

    /**
     * 中文说明：执行 mapList 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map list operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.mapList(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 mapList 的处理结果；returns the result of the operation.
     */
    private List<Map<String, Object>> mapList(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored error schema is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code JdbcGatewayCatalogStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @param column 参数 column；parameter column。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    private static String get(ResultSet result, String column) {
        try {
            return result.getString(column);
        } catch (SQLException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /**
     * 中文说明：{@code MutableBusiness} 是类型，位于当前 Gateway 模块的相关包中，负责MutableBusiness相关的职责与边界。
     * English summary: {@code MutableBusiness} is a type in the current Gateway module; it owns the mutable business-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class MutableBusiness {

        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableBusiness} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableBusiness} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String id;

        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableBusiness} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableBusiness} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String code;

        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableBusiness} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableBusiness} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String displayName;

        /**
         * 中文说明：保存 entities 对应的状态、依赖或配置值；字段类型为 {@code Map<String, MutableEntity>}，由 {@code JdbcGatewayCatalogStore.MutableBusiness} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by entities; its type is {@code Map<String, MutableEntity>}, and {@code JdbcGatewayCatalogStore.MutableBusiness} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableBusiness} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableBusiness}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<String, MutableEntity> entities =
                new LinkedHashMap<>();

        /**
         * 中文说明：创建 {@code JdbcGatewayCatalogStore.MutableBusiness} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcGatewayCatalogStore.MutableBusiness} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param code 参数 code；parameter code。
         * @param displayName 参数 displayName；parameter display name。
         */
        private MutableBusiness(
                String id,
                String code,
                String displayName) {
            this.id = id;
            this.code = code;
            this.displayName = displayName;
        }

        /**
         * 中文说明：执行 freeze 操作；该方法是 {@code JdbcGatewayCatalogStore.MutableBusiness} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the freeze operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore.MutableBusiness} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.MutableBusiness.freeze(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 freeze 的处理结果；returns the result of the operation.
         */
        private BusinessNode freeze() {
            return new BusinessNode(
                    id,
                    code,
                    displayName,
                    entities.values().stream()
                            .map(MutableEntity::freeze)
                            .toList()
            );
        }
    }

    /**
     * 中文说明：{@code MutableEntity} 是类型，位于当前 Gateway 模块的相关包中，负责MutableEntity相关的职责与边界。
     * English summary: {@code MutableEntity} is a type in the current Gateway module; it owns the mutable entity-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class MutableEntity {

        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableEntity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableEntity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableEntity}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String id;

        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableEntity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableEntity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableEntity}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String code;

        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableEntity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableEntity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableEntity}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String displayName;

        /**
         * 中文说明：保存 groups 对应的状态、依赖或配置值；字段类型为 {@code Map<String, MutableGroup>}，由 {@code JdbcGatewayCatalogStore.MutableEntity} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by groups; its type is {@code Map<String, MutableGroup>}, and {@code JdbcGatewayCatalogStore.MutableEntity} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableEntity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableEntity}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<String, MutableGroup> groups =
                new LinkedHashMap<>();

        /**
         * 中文说明：创建 {@code JdbcGatewayCatalogStore.MutableEntity} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcGatewayCatalogStore.MutableEntity} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param code 参数 code；parameter code。
         * @param displayName 参数 displayName；parameter display name。
         */
        private MutableEntity(
                String id,
                String code,
                String displayName) {
            this.id = id;
            this.code = code;
            this.displayName = displayName;
        }

        /**
         * 中文说明：执行 freeze 操作；该方法是 {@code JdbcGatewayCatalogStore.MutableEntity} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the freeze operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore.MutableEntity} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.MutableEntity.freeze(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 freeze 的处理结果；returns the result of the operation.
         */
        private EntityNode freeze() {
            return new EntityNode(
                    id,
                    code,
                    displayName,
                    groups.values().stream()
                            .map(MutableGroup::freeze)
                            .toList()
            );
        }
    }

    /**
     * 中文说明：{@code MutableGroup} 是类型，位于当前 Gateway 模块的相关包中，负责MutableGroup相关的职责与边界。
     * English summary: {@code MutableGroup} is a type in the current Gateway module; it owns the mutable group-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class MutableGroup {

        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableGroup} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableGroup} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableGroup} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableGroup}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String id;

        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableGroup} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableGroup} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableGroup} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableGroup}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String code;

        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableGroup} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableGroup} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableGroup} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableGroup}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String displayName;

        /**
         * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableGroup} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableGroup} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableGroup} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableGroup}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String sourceType;

        /**
         * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayCatalogStore.MutableGroup} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code JdbcGatewayCatalogStore.MutableGroup} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableGroup} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableGroup}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String className;

        /**
         * 中文说明：保存 operations 对应的状态、依赖或配置值；字段类型为 {@code List<OperationNode>}，由 {@code JdbcGatewayCatalogStore.MutableGroup} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operations; its type is {@code List<OperationNode>}, and {@code JdbcGatewayCatalogStore.MutableGroup} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayCatalogStore.MutableGroup} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayCatalogStore.MutableGroup}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final List<OperationNode> operations = new ArrayList<>();

        /**
         * 中文说明：创建 {@code JdbcGatewayCatalogStore.MutableGroup} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code JdbcGatewayCatalogStore.MutableGroup} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param id 参数 id；parameter id。
         * @param code 参数 code；parameter code。
         * @param displayName 参数 displayName；parameter display name。
         * @param sourceType 参数 sourceType；parameter source type。
         * @param className 参数 className；parameter class name。
         */
        private MutableGroup(
                String id,
                String code,
                String displayName,
                String sourceType,
                String className) {
            this.id = id;
            this.code = code;
            this.displayName = displayName;
            this.sourceType = sourceType;
            this.className = className;
        }

        /**
         * 中文说明：执行 freeze 操作；该方法是 {@code JdbcGatewayCatalogStore.MutableGroup} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the freeze operation; this method is the invocation entry point on {@code JdbcGatewayCatalogStore.MutableGroup} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayCatalogStore.MutableGroup.freeze(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 freeze 的处理结果；returns the result of the operation.
         */
        private InterfaceGroupNode freeze() {
            return new InterfaceGroupNode(
                    id,
                    code,
                    displayName,
                    sourceType,
                    className,
                    List.copyOf(operations)
            );
        }
    }
}
