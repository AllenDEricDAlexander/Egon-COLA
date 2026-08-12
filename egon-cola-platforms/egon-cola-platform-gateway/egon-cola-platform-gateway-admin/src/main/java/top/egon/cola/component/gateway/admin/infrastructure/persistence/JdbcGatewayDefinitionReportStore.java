package top.egon.cola.component.gateway.admin.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionReportStore;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static top.egon.cola.component.gateway.admin.infrastructure.persistence
        .JdbcGatewayParameters.timestamp;

/**
 * 中文说明：{@code JdbcGatewayDefinitionReportStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责Jdbc网关定义报告存储相关的职责与边界。
 * English summary: {@code JdbcGatewayDefinitionReportStore} is a jdbc gateway definition report store store in the current Gateway module; it owns the jdbc gateway definition report store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Repository
public class JdbcGatewayDefinitionReportStore
        implements GatewayDefinitionReportStore {

    /**
     * 中文说明：保存 jdbc 对应的状态、依赖或配置值；字段类型为 {@code JdbcTemplate}，由 {@code JdbcGatewayDefinitionReportStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by jdbc; its type is {@code JdbcTemplate}, and {@code JdbcGatewayDefinitionReportStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcTemplate jdbc;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code JdbcGatewayDefinitionReportStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code JdbcGatewayDefinitionReportStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 idGenerator 对应的状态、依赖或配置值；字段类型为 {@code LongIdGenerator}，由 {@code JdbcGatewayDefinitionReportStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id generator; its type is {@code LongIdGenerator}, and {@code JdbcGatewayDefinitionReportStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final LongIdGenerator idGenerator;

    /**
     * 中文说明：保存 canonical映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code JdbcGatewayDefinitionReportStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonical mapper; its type is {@code ObjectMapper}, and {@code JdbcGatewayDefinitionReportStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper canonicalMapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    /**
     * 中文说明：创建 {@code JdbcGatewayDefinitionReportStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code JdbcGatewayDefinitionReportStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param jdbc 参数 jdbc；parameter jdbc。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param idGenerator 参数 idGenerator；parameter id generator。
     */
    public JdbcGatewayDefinitionReportStore(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 中文说明：执行 findBuildFingerprint 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find build fingerprint operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.findBuildFingerprint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param buildId 参数 buildId；parameter build id。
     * @return 返回 findBuildFingerprint 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<String> findBuildFingerprint(
            String applicationId,
            String buildId) {
        return jdbc.queryForList("""
                        SELECT DISTINCT fingerprint
                          FROM gateway_definition_set
                         WHERE application_id = ? AND build_id = ?
                        """,
                String.class,
                applicationId,
                buildId
        ).stream().findFirst();
    }

    /**
     * 中文说明：执行 定义SetExists 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the definition set exists operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.definitionSetExists(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @return 返回 定义SetExists 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean definitionSetExists(
            String applicationId,
            String definitionSetId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM gateway_definition_set
                 WHERE application_id = ? AND id = ?
                """, Integer.class, applicationId, definitionSetId);
        return count != null && count > 0;
    }

    /**
     * 中文说明：执行 countStarterOperations 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the count starter operations operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.countStarterOperations(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 countStarterOperations 的处理结果；returns the result of the operation.
     */
    @Override
    public int countStarterOperations(String applicationId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM gateway_operation
                 WHERE application_id = ? AND source_type = 'STARTER'
                """, Integer.class, applicationId);
        return count == null ? 0 : count;
    }

    /**
     * 中文说明：执行 ingest 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ingest operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.ingest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param report 参数 报告；parameter report。
     * @param now 参数 now；parameter now。
     * @return 返回 ingest 的处理结果；returns the result of the operation.
     */
    @Override
    public StoredReport ingest(
            String applicationId,
            GatewayInterfaceDefinitionReport report,
            Instant now) {
        int operationCount = operationCount(report);
        jdbc.update("""
                INSERT INTO gateway_definition_set(
                    id, application_id, report_id, build_id, protocol,
                    fingerprint, complete_set, status, operation_count,
                    accepted_count, conflict_count, received_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'VERIFIED', ?, ?, 0, ?, ?)
                """,
                report.definitionSetId(),
                applicationId,
                report.reportId(),
                report.build().buildId(),
                protocol(report),
                report.definitionFingerprint(),
                report.complete(),
                operationCount,
                operationCount,
                timestamp(now),
                timestamp(now)
        );
        MutableStored stored = new MutableStored();
        for (GatewayInterfaceDefinitionReport.BusinessDomain business
                : report.businessDomains()) {
            String businessId = hierarchy(
                    "gateway_business_domain",
                    "application_id",
                    applicationId,
                    business.code(),
                    business.name(),
                    business.description(),
                    now
            );
            for (GatewayInterfaceDefinitionReport.EntityDomain entity
                    : business.entityDomains()) {
                String entityId = hierarchy(
                        "gateway_entity_domain",
                        "business_domain_id",
                        businessId,
                        entity.code(),
                        entity.name(),
                        entity.description(),
                        now
                );
                for (GatewayInterfaceDefinitionReport.InterfaceGroup group
                        : entity.interfaceGroups()) {
                    String groupId = interfaceGroup(entityId, group, now);
                    for (GatewayInterfaceDefinitionReport.Operation operation
                            : group.operations()) {
                        storeOperation(
                                applicationId,
                                groupId,
                                report.definitionSetId(),
                                operation,
                                now,
                                stored
                        );
                    }
                }
            }
        }
        return stored.freeze();
    }

    /**
     * 中文说明：执行 hierarchy 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the hierarchy operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.hierarchy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param table 参数 table；parameter table。
     * @param parentColumn 参数 parentColumn；parameter parent column。
     * @param parentId 参数 parentId；parameter parent id。
     * @param code 参数 code；parameter code。
     * @param name 参数 name；parameter name。
     * @param description 参数 description；parameter description。
     * @param now 参数 now；parameter now。
     * @return 返回 hierarchy 的处理结果；returns the result of the operation.
     */
    private String hierarchy(
            String table,
            String parentColumn,
            String parentId,
            String code,
            String name,
            String description,
            Instant now) {
        List<String> existing = jdbc.queryForList(
                "SELECT id FROM "
                        + table
                        + " WHERE "
                        + parentColumn
                        + " = ? AND code = ? AND deleted = FALSE",
                String.class,
                parentId,
                code
        );
        if (!existing.isEmpty()) {
            jdbc.update(
                    "UPDATE "
                            + table
                            + " SET display_name = ?, description = ?, "
                            + "updated_at = ? WHERE id = ?",
                    name,
                    description,
                    timestamp(now),
                    existing.getFirst()
            );
            return existing.getFirst();
        }
        String id = UuidV7.simpleString();
        jdbc.update(
                "INSERT INTO "
                        + table
                        + "(id, "
                        + parentColumn
                        + ", code, display_name, description, deleted, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, FALSE, ?, ?)",
                id,
                parentId,
                code,
                name,
                description,
                timestamp(now),
                timestamp(now)
        );
        return id;
    }

    /**
     * 中文说明：执行 接口Group 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the interface group operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.interfaceGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param entityId 参数 entityId；parameter entity id。
     * @param group 参数 group；parameter group。
     * @param now 参数 now；parameter now。
     * @return 返回 接口Group 的处理结果；returns the result of the operation.
     */
    private String interfaceGroup(
            String entityId,
            GatewayInterfaceDefinitionReport.InterfaceGroup group,
            Instant now) {
        List<GroupRow> existing = jdbc.query("""
                SELECT id, source_type
                  FROM gateway_interface_group
                 WHERE entity_domain_id = ? AND code = ?
                   AND deleted = FALSE
                """, (result, row) -> new GroupRow(
                result.getString("id"),
                result.getString("source_type")
        ), entityId, group.code());
        if (!existing.isEmpty()) {
            GroupRow row = existing.getFirst();
            if (!"STARTER".equals(row.sourceType)) {
                throw new IllegalStateException(
                        "GATEWAY_ADMIN_STARTER_MANUAL_CONFLICT: "
                                + group.code()
                );
            }
            jdbc.update("""
                    UPDATE gateway_interface_group
                       SET display_name = ?, class_name = ?,
                           description = ?, updated_at = ?
                     WHERE id = ?
                    """,
                    group.name(),
                    group.className(),
                    group.description(),
                    timestamp(now),
                    row.id
            );
            return row.id;
        }
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_interface_group(
                    id, entity_domain_id, code, display_name, source_type,
                    class_name, description, deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'STARTER', ?, ?, FALSE, ?, ?)
                """,
                id,
                entityId,
                group.code(),
                group.name(),
                group.className(),
                group.description(),
                timestamp(now),
                timestamp(now)
        );
        return id;
    }

    /**
     * 中文说明：执行 存储操作 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the store operation operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.storeOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param groupId 参数 groupId；parameter group id。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @param operation 参数 操作；parameter operation。
     * @param now 参数 now；parameter now。
     * @param stored 参数 stored；parameter stored。
     */
    private void storeOperation(
            String applicationId,
            String groupId,
            String definitionSetId,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now,
            MutableStored stored) {
        List<OperationRow> existing = jdbc.query("""
                SELECT o.id, o.interface_group_id, o.source_type,
                       o.current_definition_id,
                       d.definition_sha256,
                       COALESCE(MAX(all_d.definition_version), 0) AS max_version
                  FROM gateway_operation o
                  LEFT JOIN gateway_operation_definition d
                    ON d.id = o.current_definition_id
                  LEFT JOIN gateway_operation_definition all_d
                    ON all_d.operation_id = o.id
                 WHERE o.application_id = ? AND o.operation_key = ?
                 GROUP BY o.id, o.interface_group_id, o.source_type,
                          o.current_definition_id, d.definition_sha256
                """, (result, row) -> new OperationRow(
                result.getString("id"),
                result.getString("interface_group_id"),
                result.getString("source_type"),
                result.getString("current_definition_id"),
                result.getString("definition_sha256"),
                result.getLong("max_version")
        ), applicationId, operation.operationKey());
        String definitionSha = sha256(canonical(operation));
        if (existing.isEmpty()) {
            String operationId = idGenerator.nextId();
            jdbc.update("""
                    INSERT INTO gateway_operation(
                        id, application_id, interface_group_id, operation_key,
                        protocol, method_identity, external_accessible,
                        provider_service_identity, source_type,
                        lifecycle_status, current_definition_id, revision,
                        created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'STARTER',
                              'DISCOVERED', NULL, 0, ?, ?)
                    """,
                    operationId,
                    applicationId,
                    groupId,
                    operation.operationKey(),
                    operation.protocol(),
                    operation.methodIdentity(),
                    operation.externalAccessible(),
                    json(operation.providerService()),
                    timestamp(now),
                    timestamp(now)
            );
            String definitionId = appendDefinition(
                    operationId,
                    definitionSetId,
                    1,
                    definitionSha,
                    operation,
                    now
            );
            linkDefinitionSet(
                    definitionSetId,
                    operationId,
                    definitionId,
                    operation,
                    now
            );
            pointPending(operationId, definitionId, operation, now);
            stored.created++;
            stored.refs.add(ref(operation, operationId, "CREATED"));
            return;
        }
        OperationRow row = existing.getFirst();
        if (!"STARTER".equals(row.sourceType)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_STARTER_MANUAL_CONFLICT: "
                            + operation.operationKey()
            );
        }
        if (!groupId.equals(row.interfaceGroupId)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_STARTER_GROUP_CONFLICT: "
                            + operation.operationKey()
            );
        }
        if (definitionSha.equals(row.definitionSha256)) {
            linkDefinitionSet(
                    definitionSetId,
                    row.id,
                    row.currentDefinitionId,
                    operation,
                    now
            );
            stored.refs.add(ref(operation, row.id, "UNCHANGED"));
            return;
        }
        String definitionId = findDefinition(row.id, definitionSha)
                .orElseGet(() -> appendDefinition(
                        row.id,
                        definitionSetId,
                        row.maxVersion + 1,
                        definitionSha,
                        operation,
                        now
                ));
        linkDefinitionSet(
                definitionSetId,
                row.id,
                definitionId,
                operation,
                now
        );
        pointPending(row.id, definitionId, operation, now);
        stored.updated++;
        stored.refs.add(ref(operation, row.id, "UPDATED"));
    }

    /**
     * 中文说明：执行 append定义 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append definition operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.appendDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @param version 参数 version；parameter version。
     * @param definitionSha 参数 定义Sha；parameter definition sha。
     * @param operation 参数 操作；parameter operation。
     * @param now 参数 now；parameter now。
     * @return 返回 append定义 的处理结果；returns the result of the operation.
     */
    private String appendDefinition(
            String operationId,
            String definitionSetId,
            long version,
            String definitionSha,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now) {
        String id = UuidV7.simpleString();
        jdbc.update("""
                INSERT INTO gateway_operation_definition(
                    id, operation_id, definition_set_id, definition_version,
                    definition_sha256, summary, tags, request_schema,
                    response_schema, error_schema, descriptor_snapshot,
                    attributes, external_accessible, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb,
                          ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, 'STARTER')
                """,
                id,
                operationId,
                definitionSetId,
                version,
                definitionSha,
                operation.summary(),
                json(operation.tags()),
                json(operation.requestSchema()),
                json(operation.responseSchema()),
                json(operation.errorSchema()),
                operation.descriptorSnapshot() == null
                        ? null
                        : json(operation.descriptorSnapshot()),
                json(attributes(operation)),
                operation.externalAccessible(),
                timestamp(now)
        );
        return id;
    }

    /**
     * 中文说明：执行 find定义 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find definition operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.findDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionSha 参数 定义Sha；parameter definition sha。
     * @return 返回 find定义 的处理结果；returns the result of the operation.
     */
    private Optional<String> findDefinition(
            String operationId,
            String definitionSha) {
        return jdbc.queryForList("""
                SELECT id
                  FROM gateway_operation_definition
                 WHERE operation_id = ? AND definition_sha256 = ?
                """, String.class, operationId, definitionSha)
                .stream()
                .findFirst();
    }

    /**
     * 中文说明：执行 link定义Set 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the link definition set operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.linkDefinitionSet(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionId 参数 定义Id；parameter definition id。
     * @param operation 参数 操作；parameter operation。
     * @param now 参数 now；parameter now。
     */
    private void linkDefinitionSet(
            String definitionSetId,
            String operationId,
            String definitionId,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now) {
        jdbc.update("""
                INSERT INTO gateway_definition_set_operation(
                    definition_set_id, operation_id, definition_id,
                    method_identity, provider_service_identity,
                    external_accessible, deprecated, created_at
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (definition_set_id, operation_id) DO NOTHING
                """,
                definitionSetId,
                operationId,
                definitionId,
                operation.methodIdentity(),
                json(operation.providerService()),
                operation.externalAccessible(),
                operation.deprecated(),
                timestamp(now)
        );
    }

    /**
     * 中文说明：执行 pointPending 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the point pending operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.pointPending(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definitionId 参数 定义Id；parameter definition id。
     * @param operation 参数 操作；parameter operation。
     * @param now 参数 now；parameter now。
     */
    private void pointPending(
            String operationId,
            String definitionId,
            GatewayInterfaceDefinitionReport.Operation operation,
            Instant now) {
        jdbc.update("""
                UPDATE gateway_operation
                   SET current_definition_id = ?,
                       method_identity = ?,
                       external_accessible = ?,
                       provider_service_identity = ?::jsonb,
                       lifecycle_status = 'DISCOVERED',
                       deprecated_at = NULL,
                       revision = revision + 1,
                       updated_at = ?
                 WHERE id = ?
                   AND lifecycle_status IN ('DISCOVERED', 'OFFLINE')
                """,
                definitionId,
                operation.methodIdentity(),
                operation.externalAccessible(),
                json(operation.providerService()),
                timestamp(now),
                operationId
        );
    }

    /**
     * 中文说明：执行 attributes 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attributes operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.attributes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 attributes 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> attributes(
            GatewayInterfaceDefinitionReport.Operation operation) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.putAll(operation.attributes());
        attributes.put("name", nullable(operation.name()));
        attributes.put("description", nullable(operation.description()));
        attributes.put("owner", nullable(operation.owner()));
        attributes.put("gatewaySupport", operation.gatewaySupport());
        attributes.put("deprecated", operation.deprecated());
        return attributes;
    }

    /**
     * 中文说明：执行 nullable 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the nullable operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.nullable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 nullable 的处理结果；returns the result of the operation.
     */
    private String nullable(String value) {
        return value == null ? "" : value;
    }

    /**
     * 中文说明：执行 ref 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ref operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.ref(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param changeType 参数 changeType；parameter change type。
     * @return 返回 ref 的处理结果；returns the result of the operation.
     */
    private GatewayInterfaceDefinitionReportResult.OperationRef ref(
            GatewayInterfaceDefinitionReport.Operation operation,
            String operationId,
            String changeType) {
        return new GatewayInterfaceDefinitionReportResult.OperationRef(
                operation.operationKey(),
                operationId,
                changeType
        );
    }

    /**
     * 中文说明：执行 操作Count 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation count operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.operationCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     * @return 返回 操作Count 的处理结果；returns the result of the operation.
     */
    private int operationCount(GatewayInterfaceDefinitionReport report) {
        return report.businessDomains().stream()
                .flatMap(business -> business.entityDomains().stream())
                .flatMap(entity -> entity.interfaceGroups().stream())
                .mapToInt(group -> group.operations().size())
                .sum();
    }

    /**
     * 中文说明：执行 protocol 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the protocol operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.protocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     * @return 返回 protocol 的处理结果；returns the result of the operation.
     */
    private String protocol(GatewayInterfaceDefinitionReport report) {
        List<String> protocols = report.businessDomains().stream()
                .flatMap(business -> business.entityDomains().stream())
                .flatMap(entity -> entity.interfaceGroups().stream())
                .flatMap(group -> group.operations().stream())
                .map(GatewayInterfaceDefinitionReport.Operation::protocol)
                .distinct()
                .toList();
        return protocols.size() == 1 ? protocols.getFirst() : "MIXED";
    }

    /**
     * 中文说明：执行 canonical 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the canonical operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.canonical(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 canonical 的处理结果；returns the result of the operation.
     */
    private byte[] canonical(Object value) {
        try {
            return canonicalMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway definition cannot be canonicalized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code JdbcGatewayDefinitionReportStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway definition cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：{@code GroupRow} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GroupRow相关的职责与边界。
     * English summary: {@code GroupRow} is an immutable data carrier in the current Gateway module; it owns the group row-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param sourceType 参数 sourceType；parameter source type。
     */
    private record GroupRow(
    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.GroupRow} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.GroupRow} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.GroupRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.GroupRow}; do not couple callers to its representation when the owning type exposes an API.
     */
    String id,
    /**
     * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.GroupRow} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.GroupRow} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.GroupRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.GroupRow}; do not couple callers to its representation when the owning type exposes an API.
     */
    String sourceType) {
    }

    /**
     * 中文说明：{@code OperationRow} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Row相关的职责与边界。
     * English summary: {@code OperationRow} is an immutable data carrier in the current Gateway module; it owns the operation row-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @param sourceType 参数 sourceType；parameter source type。
     * @param currentDefinitionId 参数 current定义Id；parameter current definition id。
     * @param definitionSha256 参数 定义Sha256；parameter definition sha256。
     * @param maxVersion 参数 maxVersion；parameter max version。
     */
    private record OperationRow(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.OperationRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.OperationRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.OperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.OperationRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 接口GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.OperationRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group id; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.OperationRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.OperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.OperationRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String interfaceGroupId,
            /**
             * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.OperationRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.OperationRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.OperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.OperationRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sourceType,
            /**
             * 中文说明：保存 current定义Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.OperationRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by current definition id; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.OperationRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.OperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.OperationRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String currentDefinitionId,
            /**
             * 中文说明：保存 定义Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code JdbcGatewayDefinitionReportStore.OperationRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition sha256; its type is {@code String}, and {@code JdbcGatewayDefinitionReportStore.OperationRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.OperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.OperationRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            String definitionSha256,
            /**
             * 中文说明：保存 maxVersion 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code JdbcGatewayDefinitionReportStore.OperationRow} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by max version; its type is {@code long}, and {@code JdbcGatewayDefinitionReportStore.OperationRow} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.OperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.OperationRow}; do not couple callers to its representation when the owning type exposes an API.
             */
            long maxVersion
    ) {
    }

    /**
     * 中文说明：{@code MutableStored} 是类型，位于当前 Gateway 模块的相关包中，负责MutableStored相关的职责与边界。
     * English summary: {@code MutableStored} is a type in the current Gateway module; it owns the mutable stored-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class MutableStored {

        /**
         * 中文说明：保存 created 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code JdbcGatewayDefinitionReportStore.MutableStored} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created; its type is {@code int}, and {@code JdbcGatewayDefinitionReportStore.MutableStored} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.MutableStored} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.MutableStored}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int created;

        /**
         * 中文说明：保存 updated 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code JdbcGatewayDefinitionReportStore.MutableStored} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated; its type is {@code int}, and {@code JdbcGatewayDefinitionReportStore.MutableStored} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.MutableStored} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.MutableStored}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int updated;

        /**
         * 中文说明：保存 refs 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayInterfaceDefinitionReportResult.OperationRef>}，由 {@code JdbcGatewayDefinitionReportStore.MutableStored} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by refs; its type is {@code List<GatewayInterfaceDefinitionReportResult.OperationRef>}, and {@code JdbcGatewayDefinitionReportStore.MutableStored} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code JdbcGatewayDefinitionReportStore.MutableStored} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code JdbcGatewayDefinitionReportStore.MutableStored}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final List<
                GatewayInterfaceDefinitionReportResult.OperationRef> refs =
                new ArrayList<>();

        /**
         * 中文说明：执行 freeze 操作；该方法是 {@code JdbcGatewayDefinitionReportStore.MutableStored} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the freeze operation; this method is the invocation entry point on {@code JdbcGatewayDefinitionReportStore.MutableStored} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code JdbcGatewayDefinitionReportStore.MutableStored.freeze(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 freeze 的处理结果；returns the result of the operation.
         */
        private StoredReport freeze() {
            return new StoredReport(created, updated, List.copyOf(refs));
        }
    }
}
