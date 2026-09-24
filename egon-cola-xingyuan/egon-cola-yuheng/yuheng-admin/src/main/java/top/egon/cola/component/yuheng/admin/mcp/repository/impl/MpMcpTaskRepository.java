package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpTaskPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpTaskBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpPersistentTaskStateEnum;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpTaskRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpTaskPersistenceRepository;

/**
 * 中文说明：{@code MpMcpTaskRepository} 是 MCP 异步任务实例的 MyBatis-Plus 门面存储，逐方法取代被退役的遗留持久化载体：
 * {@code gateway_mcp_task_instance} 的每次读写都走受守卫的 {@code EgonColaRepository} 边界（同租户过滤、仅活跃行
 * {@code deleted_at IS NULL}、技术 {@code version} 乐观锁），端口编号落在协议列 {@code task_key}、MCP 主体租户落在
 * {@code subject_tenant_id}；{@code claim}/{@code transition}/{@code cancel} 保留旧语句「按业务谓词读定位 + 乐观锁 CAS 写入」
 * 两步顺序与全部谓词（{@code revision} 相符、状态相符、未过期、未过执行截止、尝试次数未达上限、租约空闲或已到期），
 * 业务 {@code revision} 只在命中时自增，行缺失、谓词不匹配或影响 0 行一律如实返回 {@code false}；置空的租约与负载列
 * 经更新条件显式下推 SQL NULL；列与类型映射只经 {@code mcpTaskPersistenceConverter} 完成，装载出的每个载体仍经
 * {@code McpTaskBO.normalized(...)} 复核旧构造器不变量，公开端口不泄漏 {@code McpTaskRecordPO} 或 DAO。
 * English summary: {@code MpMcpTaskRepository} is the MyBatis-Plus facade store of MCP asynchronous task instances, replacing the
 * retired legacy persistence carrier method by method: every read and write of {@code gateway_mcp_task_instance} goes through the
 * guarded {@code EgonColaRepository} boundary (same-tenant filtering, active rows only under {@code deleted_at IS NULL}, the technical
 * {@code version} optimistic lock), the port identifier lands in the protocol column {@code task_key} and the MCP subject tenant in
 * {@code subject_tenant_id}; {@code claim}, {@code transition} and {@code cancel} keep the legacy two-step order of locating by the
 * business predicates and then writing under the optimistic lock together with every predicate (matching {@code revision}, matching
 * state, not expired, inside the execution deadline, attempts below the stored maximum, and a free or lapsed lease), the business
 * {@code revision} only increments on a hit, and a missing row, an unmatched predicate or a zero-row effect truthfully returns
 * {@code false}; cleared lease and payload columns are pushed as explicit SQL NULL through the update condition; column and type
 * mapping happens only in {@code mcpTaskPersistenceConverter}, every loaded carrier is re-checked by
 * {@code McpTaskBO.normalized(...)}, and the public port leaks neither {@code McpTaskRecordPO} nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpTaskRepository} 由 Spring 容器注入；事务边界与幂等策略与遗留实现一致地由调用方持有，
 * 本门面不声明 {@code @Transactional}，因此认领、状态推进与取消都必须在调用方的同一事务内组合。
 * Inject it through the business port {@code McpTaskRepository}; transaction boundaries and idempotency stay with the caller exactly as
 * the legacy carrier required, so this facade declares no {@code @Transactional} and claim, transition and cancel must be composed
 * inside the caller's transaction.
 */
@Slf4j
@Repository("mpMcpTaskRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpTaskRepository implements McpTaskRepository {

    /**
     * 中文说明：表示 SUPPORTED_STATES 这一固定值，逐一对应遗留实现 {@code Set.of(...)} 允许的任务状态 wire 字面量集合，
     * 由枚举的 {@code wireValue} 组成而不写裸字符串常量，供状态守护查表使用。
     * English summary: Represents the supported state set, mirroring the legacy {@code Set.of(...)} task-state wire literals, derived
     * from the enum {@code wireValue}s instead of bare string constants and used by the state guard.
     *
     * 用法 / Usage: 仅用于 {@link #state(String)} 的入参守护。/ Used only by the argument guard of {@link #state(String)}.
     */
    private static final Set<String> SUPPORTED_STATES = Arrays
            .stream(McpPersistentTaskStateEnum.values())
            .map(McpPersistentTaskStateEnum::wireValue)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * 中文说明：表示可取消状态集合，逐字对应遗留 {@code cancel} 的 {@code state IN ('WORKING', 'INPUT_REQUIRED')}。
     * English summary: Represents the cancellable state set, matching the legacy {@code state IN ('WORKING', 'INPUT_REQUIRED')} verbatim.
     *
     * 用法 / Usage: 仅用于取消动作的定位与 CAS 谓词。/ Used only by the locating and CAS predicates of cancellation.
     */
    private static final List<String> CANCELLABLE_STATES = List.of(
            McpPersistentTaskStateEnum.WORKING.wireValue(),
            McpPersistentTaskStateEnum.INPUT_REQUIRED.wireValue());

    /**
     * 中文说明：表示任务列表行数上限，等价遗留 SQL 的 {@code LIMIT 500}。
     * English summary: Represents the task-list row limit, equivalent to the legacy {@code LIMIT 500}.
     *
     * 用法 / Usage: 仅用于 {@link #list(String, String)}。/ Used only by {@link #list(String, String)}.
     */
    private static final int LIST_ROW_LIMIT = 500;

    /**
     * 中文说明：任务行的受守卫持久化仓储（租户过滤、活跃读取、乐观锁 CAS 的唯一入口）。
     * English summary: The guarded persistence store for task rows, the only entry point for tenant filtering, active reads and
     * optimistic-lock CAS.
     */
    @Qualifier("mcpTaskPersistenceRepository")
    private final McpTaskPersistenceRepository taskPersistenceRepository;

    /**
     * 中文说明：{@code McpTaskBO} 与 {@code McpTaskRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpTaskBO and McpTaskRecordPO.
     */
    @Qualifier("mcpTaskPersistenceConverter")
    private final McpTaskPersistenceConverter taskPersistenceConverter;

    /**
     * 中文说明：执行 create 操作；等价遗留 {@code INSERT INTO gateway_mcp_task_instance(...)}：入参守护沿用旧实现的
     * {@code Objects.requireNonNull(task, "task")}，并先让载体重新经 {@code McpTaskBO.normalized(...)} 复核（含旧
     * {@code state(task.getState())} 与摘要、期限、尝试次数守护），再交受守卫插入，技术主键、租户与审计列由边界补齐；
     * 影响 0 行按插入冲突如实抛出，绝不报成成功。
     * English summary: Executes the create operation; equivalent to the legacy {@code INSERT INTO gateway_mcp_task_instance(...)}: the
     * legacy {@code Objects.requireNonNull(task, "task")} guard is kept and the carrier is re-checked through
     * {@code McpTaskBO.normalized(...)} first (which carries the legacy {@code state(task.getState())} plus the digest, deadline and
     * attempt invariants), then inserted through the guarded boundary, which supplies the technical key, tenant and audit columns; a
     * zero-row effect surfaces truthfully as an insert conflict instead of a success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpTaskRepository.create(taskBO)}。
     * @param task 参数 任务；parameter task.
     */
    @Override
    public void create(McpTaskBO task) {
        Objects.requireNonNull(task, "task");
        if (!taskPersistenceRepository.save(
                taskPersistenceConverter.newRow(renormalize(task)))) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_TASK_INSERT_CONFLICT"
            );
        }
    }

    /**
     * 中文说明：执行 find 操作；等价遗留 {@code WHERE id = ?}，迁移后端口编号即协议列 {@code task_key}，
     * 必填守护沿用旧文案 {@code id is required}，命中多行时与旧 {@code stream().findFirst()} 一致取首行。
     * English summary: Executes the find operation; equivalent to the legacy {@code WHERE id = ?}, where the port identifier is the
     * migrated protocol column {@code task_key}, the required guard keeps the legacy {@code id is required} message, and multiple rows
     * resolve to the first one exactly like the legacy {@code stream().findFirst()}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpTaskRepository.find(id)}。
     * @param id 参数 id；parameter id.
     * @return 返回 find 的处理结果；returns the task carrier when present.
     */
    @Override
    public Optional<McpTaskBO> find(String id) {
        String key = required(id, "id");
        return taskPersistenceRepository.list(
                boundPredicate(Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .eq(McpTaskRecordPO::getTaskKey, key))
        ).stream().findFirst().map(this::carrier);
    }

    /**
     * 中文说明：执行 list 操作；等价遗留 {@code WHERE tenant_id = ? AND (? IS NULL OR client_id = ?) ORDER BY created_at DESC
     * LIMIT 500}：主体租户列即 {@code subject_tenant_id}，客户端为空时不加该谓词（旧 {@code ? IS NULL} 分支），
     * 迁移后 {@code created_at} 为边界的审计列 {@code create_time}，追加技术 id 降序保证同刻稳定次序，
     * 行数上限以 {@code LIMIT} 原样保留。
     * English summary: Executes the list operation; equivalent to the legacy
     * {@code WHERE tenant_id = ? AND (? IS NULL OR client_id = ?) ORDER BY created_at DESC LIMIT 500}: the subject tenant column is
     * {@code subject_tenant_id}, the client predicate is simply omitted for a null client (the legacy {@code ? IS NULL} branch),
     * {@code created_at} is the boundary's {@code create_time} audit column after migration, a descending technical-id tie-break keeps a
     * stable order among equal instants, and the row cap stays a {@code LIMIT}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpTaskRepository.list(tenantId, clientId)}。
     * @param tenantId 参数 tenantId；parameter tenant id.
     * @param clientId 参数 客户端Id；parameter client id.
     * @return 返回 list 的处理结果；returns the most recently created tasks, newest first.
     */
    @Override
    public List<McpTaskBO> list(String tenantId, String clientId) {
        return taskPersistenceRepository.list(
                boundPredicate(Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .eq(McpTaskRecordPO::getSubjectTenantId, tenantId)
                        .eq(clientId != null, McpTaskRecordPO::getClientId, clientId)
                        .orderByDesc(McpTaskRecordPO::getCreateTime)
                        .orderByDesc(McpTaskRecordPO::getId)
                        .last(true, "LIMIT " + LIST_ROW_LIMIT))
        ).stream().map(this::carrier).toList();
    }

    /**
     * 中文说明：执行 claim 操作；等价遗留 {@code UPDATE ... SET worker_owner = ?, lease_until = ?,
     * attempt_count = attempt_count + 1, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ? AND
     * state = 'WORKING' AND expires_at > ? AND execution_deadline > ? AND attempt_count < max_attempts AND
     * (worker_owner IS NULL OR lease_until <= ?)}：先按旧顺序守护（{@code leaseUntil must be after now} 先于必填换算，
     * 再 {@code workerOwner}、后 {@code id}），随后按同一组谓词读定位取 CAS 令牌，最后把租约两列、自增后的尝试计数与
     * 业务修订随实体下发，并在同一组谓词上追加技术主键；行缺失、谓词不匹配或影响 0 行都返回 {@code false}。
     * English summary: Executes the claim operation; equivalent to the legacy
     * {@code UPDATE ... SET worker_owner = ?, lease_until = ?, attempt_count = attempt_count + 1, revision = revision + 1, updated_at
     * = ? WHERE id = ? AND revision = ? AND state = 'WORKING' AND expires_at > ? AND execution_deadline > ? AND attempt_count <
     * max_attempts AND (worker_owner IS NULL OR lease_until <= ?)}: the guards run in the legacy order ({@code leaseUntil must be after
     * now} before the required normalizations, {@code workerOwner} before {@code id}), the row is then located by that very predicate set
     * to obtain the CAS token, and finally the two lease columns, the incremented attempt counter and the business revision ride the
     * entity while the technical identifier is appended to the same predicate set; a missing row, an unmatched predicate or a zero-row
     * effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpTaskRepository.claim(id, workerOwner, now, leaseUntil, expectedRevision)}。
     * @param id 参数 id；parameter id.
     * @param workerOwner 参数 工作持有者；parameter worker owner.
     * @param now 参数 now；parameter now.
     * @param leaseUntil 参数 租约Until；parameter lease until.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @return 返回 claim 的处理结果；returns {@code true} when exactly one task was claimed.
     */
    @Override
    public boolean claim(
            String id,
            String workerOwner,
            Instant now,
            Instant leaseUntil,
            long expectedRevision) {
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        String owner = required(workerOwner, "workerOwner");
        String key = required(id, "id");
        Optional<McpTaskRecordPO> current = taskPersistenceRepository.list(
                boundPredicate(claimPredicate(
                        Wrappers.<McpTaskRecordPO>lambdaQuery(),
                        key,
                        expectedRevision,
                        now))
        ).stream().findFirst();
        if (current.isEmpty()) {
            return false;
        }
        McpTaskRecordPO row = current.get();
        McpTaskRecordPO claimed = McpTaskRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .workerOwner(owner)
                .leaseUntil(leaseUntil)
                .attemptCount(nextAttempt(row.getAttemptCount()))
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return taskPersistenceRepository.update(
                claimed,
                boundPredicate(claimPredicate(
                        Wrappers.<McpTaskRecordPO>lambdaUpdate(),
                        key,
                        expectedRevision,
                        now)
                        .eq(McpTaskRecordPO::getId, row.getId()))
        );
    }

    /**
     * 中文说明：执行 transition 操作；等价遗留 {@code UPDATE ... SET state = ?, result_payload = ?::jsonb,
     * error_payload = ?::jsonb, worker_owner = NULL, lease_until = NULL, revision = revision + 1, updated_at = ?
     * WHERE id = ? AND state = ? AND revision = ?}：守护顺序与旧参数求值顺序一致（先目标状态、再负载编码、再时刻与
     * {@code id}、最后当前状态），租约两列无条件置 NULL；负载为 {@code null} 时同样显式下推 SQL NULL
     * （等价旧的 {@code ?::jsonb} 空文本绑定），非空负载随 CAS 实体走列上声明的 jsonb 处理器；
     * 行缺失、状态或修订不匹配、影响 0 行都返回 {@code false}。
     * English summary: Executes the transition operation; equivalent to the legacy
     * {@code UPDATE ... SET state = ?, result_payload = ?::jsonb, error_payload = ?::jsonb, worker_owner = NULL, lease_until = NULL,
     * revision = revision + 1, updated_at = ? WHERE id = ? AND state = ? AND revision = ?}: the guards run in the legacy argument
     * evaluation order (target state, then payload encoding, then the instant and {@code id}, finally the current state), and the two
     * lease columns are cleared unconditionally; an absent payload is pushed as an explicit SQL NULL (the equivalent of the legacy
     * {@code ?::jsonb} null-text binding) while a present payload rides the CAS entity through the jsonb type handler declared on the
     * column; a missing row, an unmatched state or revision, or a zero-row effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpTaskRepository.transition(id, currentState, targetState, resultPayload,
     * errorPayload, expectedRevision, now)}。
     * @param id 参数 id；parameter id.
     * @param currentState 参数 currentState；parameter current state.
     * @param targetState 参数 targetState；parameter target state.
     * @param resultPayload 参数 resultPayload；parameter result payload.
     * @param errorPayload 参数 errorPayload；parameter error payload.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param now 参数 now；parameter now.
     * @return 返回 transition 的处理结果；returns {@code true} when exactly one task moved.
     */
    @Override
    public boolean transition(
            String id,
            String currentState,
            String targetState,
            Map<String, Object> resultPayload,
            Map<String, Object> errorPayload,
            long expectedRevision,
            Instant now) {
        String target = state(targetState);
        JsonNode resultNode = taskPersistenceConverter.payloadNode(resultPayload);
        JsonNode errorNode = taskPersistenceConverter.payloadNode(errorPayload);
        Objects.requireNonNull(now, "now");
        String key = required(id, "id");
        String current = state(currentState);
        Optional<McpTaskRecordPO> located = taskPersistenceRepository.list(
                boundPredicate(stateRevisionPredicate(
                        Wrappers.<McpTaskRecordPO>lambdaQuery(),
                        key,
                        current,
                        expectedRevision))
        ).stream().findFirst();
        if (located.isEmpty()) {
            return false;
        }
        McpTaskRecordPO row = located.get();
        McpTaskRecordPO moved = McpTaskRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .state(target)
                .resultPayload(resultNode)
                .errorPayload(errorNode)
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return taskPersistenceRepository.update(
                moved,
                boundPredicate(stateRevisionPredicate(
                        Wrappers.<McpTaskRecordPO>lambdaUpdate(),
                        key,
                        current,
                        expectedRevision)
                        .eq(McpTaskRecordPO::getId, row.getId())
                        .set(McpTaskRecordPO::getWorkerOwner, null)
                        .set(McpTaskRecordPO::getLeaseUntil, null)
                        .set(resultNode == null, McpTaskRecordPO::getResultPayload, null)
                        .set(errorNode == null, McpTaskRecordPO::getErrorPayload, null))
        );
    }

    /**
     * 中文说明：执行 cancel 操作；等价遗留 {@code UPDATE ... SET state = 'CANCELLED', worker_owner = NULL,
     * lease_until = NULL, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ? AND
     * state IN ('WORKING', 'INPUT_REQUIRED')}：旧语句对 {@code id} 无必填守护，故原样下推（空白或非法文本只会读不到行），
     * 租约两列无条件置 NULL，行缺失、状态不在可取消集合、修订不匹配或影响 0 行都返回 {@code false}。
     * English summary: Executes the cancel operation; equivalent to the legacy
     * {@code UPDATE ... SET state = 'CANCELLED', worker_owner = NULL, lease_until = NULL, revision = revision + 1, updated_at = ? WHERE
     * id = ? AND revision = ? AND state IN ('WORKING', 'INPUT_REQUIRED')}: the legacy statement guarded {@code id} not at all, so the
     * value is pushed as it arrives (blank or malformed text simply locates no row), the two lease columns are cleared unconditionally,
     * and a missing row, a state outside the cancellable set, a revision mismatch or a zero-row effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpTaskRepository.cancel(id, expectedRevision, now)}。
     * @param id 参数 id；parameter id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param now 参数 now；parameter now.
     * @return 返回 cancel 的处理结果；returns {@code true} when exactly one task was cancelled.
     */
    @Override
    public boolean cancel(
            String id,
            long expectedRevision,
            Instant now) {
        Objects.requireNonNull(now, "now");
        Optional<McpTaskRecordPO> located = taskPersistenceRepository.list(
                boundPredicate(Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .eq(McpTaskRecordPO::getTaskKey, id)
                        .eq(McpTaskRecordPO::getRevision, expectedRevision)
                        .in(McpTaskRecordPO::getState, CANCELLABLE_STATES))
        ).stream().findFirst();
        if (located.isEmpty()) {
            return false;
        }
        McpTaskRecordPO row = located.get();
        McpTaskRecordPO cancelled = McpTaskRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .state(McpPersistentTaskStateEnum.CANCELLED.wireValue())
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return taskPersistenceRepository.update(
                cancelled,
                boundPredicate(Wrappers.<McpTaskRecordPO>lambdaUpdate()
                        .eq(McpTaskRecordPO::getId, row.getId())
                        .eq(McpTaskRecordPO::getTaskKey, id)
                        .eq(McpTaskRecordPO::getRevision, expectedRevision)
                        .in(McpTaskRecordPO::getState, CANCELLABLE_STATES)
                        .set(McpTaskRecordPO::getWorkerOwner, null)
                        .set(McpTaskRecordPO::getLeaseUntil, null))
        );
    }

    /**
     * 中文说明：把旧 {@code claim} 的 WHERE 谓词组挂到任意 lambda 条件上：同一协议编号、同一业务修订、状态为
     * {@code WORKING}、未过期、未过执行截止、尝试计数未达列存上限（列与列比较，故以原生片段表达）且租约空闲或已到期；
     * 定位读取与 CAS 写入共用同一份谓词，避免两条路径漂移。
     * English summary: Attaches the WHERE predicate set of the legacy {@code claim} to any lambda condition: the same protocol
     * identifier, the same business revision, the {@code WORKING} state, not expired, inside the execution deadline, the attempt counter
     * below the column-stored maximum (a column-to-column comparison, hence a native fragment) and a free or lapsed lease; the locating
     * read and the compare-and-set write share one predicate definition so the two paths cannot drift apart.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param key 参数 协议编号；parameter the protocol task identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param now 参数 认领时刻；parameter the claim instant.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends AbstractLambdaWrapper<McpTaskRecordPO, W>> W claimPredicate(
            W predicate,
            String key,
            long expectedRevision,
            Instant now) {
        return predicate
                .eq(McpTaskRecordPO::getTaskKey, key)
                .eq(McpTaskRecordPO::getRevision, expectedRevision)
                .eq(
                        McpTaskRecordPO::getState,
                        McpPersistentTaskStateEnum.WORKING.wireValue()
                )
                .gt(McpTaskRecordPO::getExpiresAt, now)
                .gt(McpTaskRecordPO::getExecutionDeadline, now)
                .apply(true, "attempt_count < max_attempts")
                .and(free -> free
                        .isNull(McpTaskRecordPO::getWorkerOwner)
                        .or()
                        .le(McpTaskRecordPO::getLeaseUntil, now));
    }

    /**
     * 中文说明：把旧 {@code transition} 的 {@code WHERE id = ? AND state = ? AND revision = ?} 挂到任意 lambda 条件上，
     * 定位读取与 CAS 写入共用同一份谓词。
     * English summary: Attaches the legacy {@code WHERE id = ? AND state = ? AND revision = ?} of {@code transition} to any lambda
     * condition, so the locating read and the compare-and-set write share one predicate definition.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param key 参数 协议编号；parameter the protocol task identifier.
     * @param current 参数 调用方期望的当前状态；parameter the current state the caller expects.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends AbstractLambdaWrapper<McpTaskRecordPO, W>> W stateRevisionPredicate(
            W predicate,
            String key,
            String current,
            long expectedRevision) {
        return predicate
                .eq(McpTaskRecordPO::getTaskKey, key)
                .eq(McpTaskRecordPO::getState, current)
                .eq(McpTaskRecordPO::getRevision, expectedRevision);
    }

    /**
     * 中文说明：把行模型经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a row onto the business carrier through the converter and immediately re-checks the legacy
     * constructor invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpTaskBO carrier(McpTaskRecordPO row) {
        return renormalize(taskPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的载体逐字段送回 {@code McpTaskBO.normalized(...)}，
     * 因此无论读写都不存在「未经校验构造 {@code McpTaskBO}」的路径，必填、摘要、状态与期限守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding an already column-mapped carrier field by field back into
     * {@code McpTaskBO.normalized(...)}, so neither reads nor writes can hold an unvalidated {@code McpTaskBO} and the required,
     * digest, state and deadline guards all apply with the legacy messages.
     *
     * 用法 / Usage: 由 {@link #create(McpTaskBO)} 与 {@link #carrier(McpTaskRecordPO)} 调用。
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpTaskBO renormalize(McpTaskBO carrier) {
        return McpTaskBO.normalized(
                carrier.getId(),
                carrier.getPrincipalFingerprint(),
                carrier.getSubjectId(),
                carrier.getTenantId(),
                carrier.getClientId(),
                carrier.getServerCode(),
                carrier.getToolName(),
                carrier.getRequestDigest(),
                carrier.getState(),
                carrier.getInputPayload(),
                carrier.getResultPayload(),
                carrier.getErrorPayload(),
                carrier.getWorkerOwner(),
                carrier.getLeaseUntil(),
                carrier.getExecutionDeadline(),
                carrier.getExpiresAt(),
                carrier.getAttemptCount(),
                carrier.getMaxAttempts(),
                carrier.getRevision(),
                carrier.getCreatedAt(),
                carrier.getUpdatedAt()
        );
    }

    /**
     * 中文说明：执行 state 操作，等价旧实现的入参守护：先按 {@code state is required} 必填规范化，
     * 再按旧文案 {@code unsupported task state} 拒绝集合外的状态。
     * English summary: Executes the state operation, equivalent to the legacy argument guard: first the required normalization with
     * {@code state is required}, then rejection of anything outside the set with the legacy {@code unsupported task state} message.
     *
     * 用法 / Usage: 仅由 {@link #transition(String, String, String, Map, Map, long, Instant)} 调用。
     * @param value 参数 状态；parameter state.
     * @return 返回 规范化后的状态；returns the normalized state.
     */
    private static String state(String value) {
        String candidate = required(value, "state");
        if (!SUPPORTED_STATES.contains(candidate)) {
            throw new IllegalArgumentException("unsupported task state");
        }
        return candidate;
    }

    /**
     * 中文说明：按旧 SQL 的 {@code attempt_count = attempt_count + 1} 递增读到的尝试计数，{@code null} 视为 0。
     * English summary: Increments the attempt counter that was read, matching the legacy
     * {@code attempt_count = attempt_count + 1} and treating {@code null} as zero.
     * @param attemptCount 参数 读到的尝试计数；parameter the stored attempt counter.
     * @return 返回 自增后的尝试计数；returns the incremented attempt counter.
     */
    private static Integer nextAttempt(Integer attemptCount) {
        return (attemptCount == null ? 0 : attemptCount) + 1;
    }

    /**
     * 中文说明：沿用遗留边界的必填规范化：{@code null} 抛 {@code NullPointerException}，空白抛
     * {@code IllegalArgumentException(field + " is required")}，否则去除首尾空白。
     * English summary: Keeps the legacy required normalization: {@code null} raises a {@code NullPointerException} and a blank value
     * raises {@code IllegalArgumentException(field + " is required")}, otherwise the value is trimmed.
     * @param value 参数 值；parameter value.
     * @param field 参数 字段名；parameter field.
     * @return 返回 规范化后的值；returns the trimmed value.
     */
    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/in/gt/le/apply} 只在 SQL 真正成形时
     * 才把取值写进 {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the values of
     * {@code eq/in/gt/le/apply} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first means the
     * parameters are fully bound when the predicate leaves the facade; later renders (including the one MyBatis performs) hit the same
     * segment cache and change neither the parameters nor the SQL, while a column-resolution failure (a missing lambda cache) surfaces
     * truthfully at the facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
