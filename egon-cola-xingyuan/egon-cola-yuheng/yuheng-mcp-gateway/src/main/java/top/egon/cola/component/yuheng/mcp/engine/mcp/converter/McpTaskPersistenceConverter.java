package top.egon.cola.component.yuheng.mcp.engine.mcp.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentTaskStateEnum;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpTaskRecordPO;
import top.egon.cola.component.yuheng.mcp.task.domain.McpTask;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpTaskPersistenceConverter} 是数据面 {@code McpTask} 领域记录与 {@code gateway_mcp_task_instance}
 * 行模型之间唯一的列映射入口：协议任务标识写入 {@code task_key}，owner 的字符串租户写入
 * {@code subject_tenant_id}，数值 {@code tenant_id}、审计与 {@code version} 只由受守卫边界补齐；JSON 载荷列在
 * {@code Map<String, Object>} 与 {@code JsonNode} 之间经 Spring 托管的 Jackson 映射器往返，空载荷保持为
 * {@code null} 而不写成 {@code null} 字面量。
 * English summary: {@code McpTaskPersistenceConverter} is the only column-mapping entry point between the data-plane
 * {@code McpTask} domain record and the {@code gateway_mcp_task_instance} row model: the protocol task identifier goes
 * to {@code task_key}, the owner's string tenant to {@code subject_tenant_id}, while the numeric {@code tenant_id},
 * audit and {@code version} columns are supplied exclusively by the guarded boundary; the JSON payload columns travel
 * between {@code Map<String, Object>} and {@code JsonNode} through the Spring-managed Jackson mapper and an absent
 * payload stays {@code null} instead of a written {@code null} literal.
 *
 * 用法 / Usage: 由 {@code MpMcpRuntimeTaskStore} 注入使用，业务侧不得在其他位置手写列映射；控制面未引入 MapStruct
 * 依赖（本模块 pom 不含 {@code org.mapstruct}，新增依赖需单独批准），故本转换器为显式手写实现，映射语义与
 * MapStruct 生成的同名转换器一致。/ Inject it into {@code MpMcpRuntimeTaskStore} only, so no other place hand-writes
 * column mapping; MapStruct is not on this module's classpath (its pom has no {@code org.mapstruct} entry and adding a
 * dependency needs separate approval), which is why this converter is written explicitly while keeping the semantics a
 * generated converter of the same name would have.
 */
@Slf4j
@Component("mcpTaskPersistenceConverter")
@RequiredArgsConstructor
@Validated
public class McpTaskPersistenceConverter {

    /** 中文说明：Spring 托管的 Jackson 映射器，用于 JSON 载荷列的无损往返。 English summary: the Spring-managed Jackson mapper used for the lossless roundtrip of the JSON payload columns. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：把领域任务投影为待插入的行模型；只填业务列，技术列（{@code id}/{@code tenant_id}/审计/
     * {@code version}）留给受守卫边界补齐，{@code createTime}/{@code updateTime} 沿用领域时间以复刻旧
     * {@code created_at}/{@code updated_at} 的落库语义。
     * English summary: Projects a domain task onto the row model to insert, filling business columns only so the technical
     * columns ({@code id}, {@code tenant_id}, audit and {@code version}) stay for the guarded boundary to supply;
     * {@code createTime}/{@code updateTime} carry the domain instants, reproducing what the legacy
     * {@code created_at}/{@code updated_at} persisted.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceConverter.newRow(task)}。
     * @param task 参数 领域任务；parameter the domain task.
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpTaskRecordPO newRow(McpTask task) {
        Objects.requireNonNull(task, "task");
        return McpTaskRecordPO.builder()
                .taskKey(task.id())
                .subjectTenantId(task.tenantId())
                .principalFingerprint(task.principalFingerprint())
                .subjectId(task.subjectId())
                .clientId(task.clientId())
                .serverCode(task.serverCode())
                .toolName(task.toolName())
                .requestDigest(task.requestDigest())
                .state(stateColumnOf(task.state()))
                .inputPayload(payloadNode(task.inputPayload()))
                .resultPayload(payloadNode(task.resultPayload()))
                .errorPayload(payloadNode(task.errorPayload()))
                .workerOwner(task.workerOwner())
                .leaseUntil(task.leaseUntil())
                .executionDeadline(task.executionDeadline())
                .expiresAt(task.expiresAt())
                .attemptCount(task.attemptCount())
                .maxAttempts(task.maxAttempts())
                .revision(task.revision())
                .createTime(task.createdAt())
                .updateTime(task.updatedAt())
                .build();
    }

    /**
     * 中文说明：把行模型投影回领域记录，逐字复刻旧 JDBC 行映射器的取值与异常语义：JSON 载荷不可解析时抛
     * {@code IllegalStateException("MCP task payload is invalid")}，数值列按 {@code int}/{@code long} 读取。
     * English summary: Projects a row model back onto the domain record, reproducing the legacy JDBC row mapper's value
     * reads and failure semantics verbatim: an unparsable JSON payload raises
     * {@code IllegalStateException("MCP task payload is invalid")} and the numeric columns are read as
     * {@code int}/{@code long}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceConverter.toBusiness(row)}。
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 领域任务；returns the domain task.
     */
    public McpTask toBusiness(McpTaskRecordPO row) {
        Objects.requireNonNull(row, "row");
        return new McpTask(
                row.getTaskKey(),
                row.getPrincipalFingerprint(),
                row.getSubjectId(),
                row.getSubjectTenantId(),
                row.getClientId(),
                row.getServerCode(),
                row.getToolName(),
                row.getRequestDigest(),
                stateDomain(row.getState()),
                payload(row.getInputPayload()),
                payload(row.getResultPayload()),
                payload(row.getErrorPayload()),
                row.getWorkerOwner(),
                row.getLeaseUntil(),
                row.getExecutionDeadline(),
                row.getExpiresAt(),
                row.getAttemptCount() == null ? 0 : row.getAttemptCount(),
                row.getMaxAttempts() == null ? 0 : row.getMaxAttempts(),
                row.getRevision() == null ? 0L : row.getRevision(),
                row.getCreateTime(),
                row.getUpdateTime()
        );
    }

    /**
     * 中文说明：执行 stateColumn 操作；把领域状态映射为持久化 wire 枚举，映射表与列上的 CHECK 约束一致。
     * English summary: Executes the stateColumn operation; maps a domain state onto the persisted wire enum with a table
     * that matches the CHECK constraint on the column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskPersistenceConverter.stateColumnOf(state)}。
     * @param state 参数 领域状态；parameter the domain state.
     * @return 返回 持久化状态枚举；returns the persistence state enum.
     */
    public static McpPersistentTaskStateEnum stateColumnOf(McpTask.State state) {
        return switch (Objects.requireNonNull(state, "state")) {
            case WORKING -> McpPersistentTaskStateEnum.WORKING;
            case INPUT_REQUIRED -> McpPersistentTaskStateEnum.INPUT_REQUIRED;
            case COMPLETED -> McpPersistentTaskStateEnum.COMPLETED;
            case FAILED -> McpPersistentTaskStateEnum.FAILED;
            case CANCELLED -> McpPersistentTaskStateEnum.CANCELLED;
        };
    }

    /**
     * 中文说明：执行 stateDomain 操作；把持久化 wire 枚举还原为领域状态，未知值按数据损坏抛出。
     * English summary: Executes the stateDomain operation; restores the domain state from the persisted wire enum and raises
     * on an unknown value as corrupt data.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskPersistenceConverter.stateDomain(state)}。
     * @param state 参数 持久化状态枚举；parameter the persistence state enum.
     * @return 返回 领域状态；returns the domain state.
     */
    private static McpTask.State stateDomain(McpPersistentTaskStateEnum state) {
        return switch (Objects.requireNonNull(state, "state")) {
            case WORKING -> McpTask.State.WORKING;
            case INPUT_REQUIRED -> McpTask.State.INPUT_REQUIRED;
            case COMPLETED -> McpTask.State.COMPLETED;
            case FAILED -> McpTask.State.FAILED;
            case CANCELLED -> McpTask.State.CANCELLED;
        };
    }

    /**
     * 中文说明：把结构化载荷编码为 JSON 列值；{@code null} 原样返回 {@code null}，与旧实现的
     * {@code setString(null)} 一致。
     * English summary: Encodes a structured payload into the JSON column value; {@code null} returns {@code null} as the
     * legacy {@code setString(null)} did.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceConverter.payloadNode(payload)}。
     * @param payload 参数 结构化载荷；parameter the structured payload.
     * @return 返回 JSON 节点或 null；returns the JSON node, or null.
     */
    public JsonNode payloadNode(Map<String, Object> payload) {
        return payload == null ? null : objectMapper.valueToTree(payload);
    }

    /**
     * 中文说明：把 JSON 列值解码为结构化载荷；空列与 SQL NULL 都返回 {@code null}，解析失败按旧实现抛
     * {@code IllegalStateException("MCP task payload is invalid")}。
     * English summary: Decodes a JSON column value into the structured payload; an absent node and SQL NULL both yield
     * {@code null}, and a failure raises the legacy
     * {@code IllegalStateException("MCP task payload is invalid")}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceConverter.payload(node)}。
     * @param node 参数 JSON 列值；parameter the JSON column value.
     * @return 返回 结构化载荷或 null；returns the structured payload, or null.
     */
    private Map<String, Object> payload(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("MCP task payload is invalid", failure);
        }
    }
}
