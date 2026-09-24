package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.support.McpGatewayPersistenceContext;
import top.egon.cola.component.yuheng.mcp.engine.mcp.converter.McpTaskPersistenceConverter;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentTaskStateEnum;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpTaskRecordPO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.repository.McpTaskPersistenceRepository;
import top.egon.cola.component.yuheng.mcp.task.domain.McpTask;
import top.egon.cola.component.yuheng.mcp.task.service.McpTaskStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code MpMcpRuntimeTaskStore} 以 MyBatis-Plus 实现 {@link McpTaskStore}，逐方法取代旧手写 JDBC 适配器：
 * 每次读写都先进入受信任身份上下文（{@link McpGatewayPersistenceContext}）再走该表的受守卫边界，因此同租户过滤、
 * 仅活跃行（{@code deleted_at IS NULL}）与技术 {@code version} 乐观锁是结构性的。旧语句的
 * {@code state}/{@code revision}/{@code worker_owner}/租约与尝试计数谓词先经本表具名查询下推并在新分配的行上
 * 逐字复核，随后只走继承的 {@code save/updateById/removeById}：{@code updateById} 以 {@code id}+读取到的
 * {@code version} 做单行 CAS，两个并发工作者读到同一行时只有一个能命中，认领互斥与“影响 0 行即失败”的语义不变，
 * 业务 {@code revision} 只在命中时自增。协议任务标识只经 {@code task_key} 查询，绝不做 {@code parseUUID} 或
 * base64 转 {@code Long}；到期清理由旧的“一条按状态批量 {@code DELETE}/{@code UPDATE}”改为“具名查询至多
 * {@code batchLimit} 行的 {@code id}+{@code version}，再逐行受保护 CAS 或版本化逻辑删除”，逻辑删除时间戳由组件的
 * {@code CURRENT_TIMESTAMP AT TIME ZONE 'UTC'} 生成。
 * English summary: {@code MpMcpRuntimeTaskStore} implements {@link McpTaskStore} on MyBatis-Plus, replacing the
 * hand-written JDBC adapter method by method: every read and write first enters the trusted identity context
 * ({@link McpGatewayPersistenceContext}) and then the table's guarded boundary, so same-tenant filtering, active rows
 * only ({@code deleted_at IS NULL}) and the technical {@code version} optimistic lock are structural. The legacy
 * {@code state}/{@code revision}/{@code worker_owner}, lease and attempt-counting predicates are pushed down through this
 * table's named queries and then re-checked literally on the freshly loaded row, after which only the inherited
 * {@code save/updateById/removeById} are used: {@code updateById} performs the single-row compare-and-set on {@code id}
 * plus the {@code version} that was read, so two workers that loaded the same row cannot both win, which keeps claim
 * mutual exclusion and the "zero rows is never success" contract intact while the business {@code revision} increments
 * only on a hit. The protocol task identifier is queried through {@code task_key} alone, with no {@code parseUUID} or
 * base64-to-{@code Long}. Expiry sweeps become "read at most {@code batchLimit} rows' {@code id}+{@code version} by a
 * named query, then apply a protected CAS or versioned logical delete per row" instead of the legacy bulk statement, and
 * the logical-delete timestamp is the component's own {@code CURRENT_TIMESTAMP AT TIME ZONE 'UTC'}.
 *
 * 用法 / Usage: 通过 {@link McpTaskStore} 端口由 Spring 注入，返回 {@code Mono} 且阻塞工作固定在 boundedElastic 上；
 * 列与类型映射只由 {@link McpTaskPersistenceConverter} 完成，端口不泄漏 {@code McpTaskRecordPO}，本类也不出现任何
 * 手写 {@code UPDATE}/{@code DELETE} SQL。/ Inject it through the {@link McpTaskStore} port; it returns {@code Mono}s
 * whose blocking work stays on boundedElastic, column and type mapping belongs to {@link McpTaskPersistenceConverter}
 * alone, the port never leaks a {@code McpTaskRecordPO} and this class contains no hand-written {@code UPDATE} or
 * {@code DELETE} SQL.
 */
@Slf4j
@Component("mcpRuntimeTaskStore")
@RequiredArgsConstructor
@Validated
public class MpMcpRuntimeTaskStore implements McpTaskStore {

    /** 中文说明：可被工作者认领的状态，逐字对应旧 SQL 的 {@code state = 'WORKING'}。 English summary: the state a worker may claim, matching the legacy {@code state = 'WORKING'} verbatim. */
    private static final McpPersistentTaskStateEnum WORKING = McpPersistentTaskStateEnum.WORKING;

    /** 中文说明：{@code failUnavailable} 写入的错误载荷，等价于旧字面量 {@code '{"code":"MCP_TASK_UNAVAILABLE"}'::jsonb}。 English summary: the error payload {@code failUnavailable} writes, equivalent to the legacy literal {@code '{"code":"MCP_TASK_UNAVAILABLE"}'::jsonb}. */
    private static final Map<String, Object> UNAVAILABLE_ERROR = Map.of("code", "MCP_TASK_UNAVAILABLE");

    /** 中文说明：{@code gateway_mcp_task_instance} 的受守卫持久化边界。 English summary: the guarded persistence boundary for {@code gateway_mcp_task_instance}. */
    @Qualifier("mcpTaskPersistenceRepository")
    private final McpTaskPersistenceRepository taskPersistenceRepository;

    /** 中文说明：{@code McpTask} 与行模型之间的唯一列映射器。 English summary: the only column mapper between {@code McpTask} and the row model. */
    @Qualifier("mcpTaskPersistenceConverter")
    private final McpTaskPersistenceConverter taskPersistenceConverter;

    /** 中文说明：受守卫读写所需的部署身份上下文包装器。 English summary: the identity-context wrapper the guarded reads and writes need. */
    @Qualifier("mcpGatewayPersistenceContext")
    private final McpGatewayPersistenceContext persistenceContext;

    /** 中文说明：单次清理扫描的最大行数，即规范修订 §11.2.35 的 {@code batch-limit}（默认 64），由 bootstrap 配置注入。 English summary: the maximum rows one sweep touches, the {@code batch-limit} of amendment section 11.2.35 (64 by default), supplied by the bootstrap configuration. */
    private final int batchLimit;

    /**
     * 中文说明：执行 create 操作；受守卫插入完整业务行（租户、审计与技术 {@code version} 由边界补齐），
     * 影响 0 行按 {@code IllegalStateException} 抛出，绝不当作成功。
     * English summary: Executes the create operation; the guarded insert writes the full business row (the boundary supplies
     * tenant, audit and the technical {@code version}) and a zero-row effect is raised as an
     * {@code IllegalStateException} instead of being reported as success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.create(task)}。
     * @param task 参数 领域任务；parameter the domain task.
     * @return 返回 create 的处理结果；returns the completion signal of the insert.
     */
    @Override
    public Mono<Void> create(McpTask task) {
        return blocking(() -> {
            Objects.requireNonNull(task, "task");
            if (!taskPersistenceRepository.save(taskPersistenceConverter.newRow(task))) {
                throw new IllegalStateException("MCP task create affected no row");
            }
            return Boolean.TRUE;
        }).then();
    }

    /**
     * 中文说明：执行 find 操作；按 {@code task_key} 走受守卫活跃读取，行缺失按旧实现返回空结果。
     * English summary: Executes the find operation; the guarded active read runs on {@code task_key} and an absent row stays
     * empty like the legacy behaviour.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.find(taskId)}。
     * @param taskId 参数 协议任务标识；parameter the protocol task identifier.
     * @return 返回 find 的处理结果；returns the stored task when present.
     */
    @Override
    public Mono<McpTask> find(String taskId) {
        String key = required(taskId, "taskId");
        return blocking(() -> taskPersistenceRepository.findActiveByTaskKey(key)
                .map(taskPersistenceConverter::toBusiness)
                .orElse(null));
    }

    /**
     * 中文说明：执行 租约Next 操作；先按旧认领谓词具名查询候选行，再对每一行做 {@code id}+{@code version}
     * 单行 CAS（写入占有者、租约、{@code attempt_count + 1}、业务 {@code revision + 1} 与时间），命中后按旧实现
     * 重读权威行返回；CAS 落败就顺延下一个候选，全部落败返回空。
     * English summary: Executes the lease-next operation; the candidates come from the named query carrying the legacy
     * claim predicates and each then goes through the single-row {@code id}+{@code version} compare-and-set that writes the
     * owner, the lease, {@code attempt_count + 1}, the business {@code revision + 1} and the timestamp; a winner re-reads
     * the authoritative row like the legacy implementation, a lost compare-and-set falls through to the next candidate and
     * an all-lost sweep stays empty.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.leaseNext(workerOwner, now, leaseUntil)}。
     * @param workerOwner 参数 工作者占有者；parameter the worker owner.
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @param leaseUntil 参数 新租约截止时刻；parameter the new lease deadline.
     * @return 返回 租约Next 的处理结果；returns the claimed task when one was won.
     */
    @Override
    public Mono<McpTask> leaseNext(
            String workerOwner,
            Instant now,
            Instant leaseUntil) {
        String owner = required(workerOwner, "workerOwner");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(leaseUntil, "leaseUntil");
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        return blocking(() -> {
            for (McpTaskRecordPO candidate
                    : taskPersistenceRepository.listLeasable(now, batchLimit)) {
                McpTaskRecordPO claimed = McpTaskRecordPO.builder()
                        .id(candidate.getId())
                        .version(candidate.getVersion())
                        .workerOwner(owner)
                        .leaseUntil(leaseUntil)
                        .attemptCount(nextAttempt(candidate.getAttemptCount()))
                        .revision(nextRevision(candidate.getRevision()))
                        .updateTime(now)
                        .build();
                if (taskPersistenceRepository.updateById(claimed)) {
                    return authoritativeTask(candidate.getId());
                }
            }
            return null;
        });
    }

    /**
     * 中文说明：执行 renew租约 操作；等价于旧 {@code UPDATE ... SET lease_until = ?, updated_at = ? WHERE id = ?
     * AND state = 'WORKING' AND worker_owner = ? AND lease_until > ?}：三条件在新分配行上逐字复核后只延长租约，
     * 不触碰业务 {@code revision}；行缺失、谓词不匹配或影响 0 行都返回 {@code false}。
     * English summary: Executes the renew-lease operation; the equivalent of the legacy
     * {@code UPDATE ... SET lease_until = ?, updated_at = ? WHERE id = ? AND state = 'WORKING' AND worker_owner = ? AND
     * lease_until > ?}: the three conditions are re-checked literally on the freshly loaded row and only the lease moves,
     * leaving the business {@code revision} untouched, so a missing row, an unmatched predicate or a zero-row effect
     * returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.renewLease(taskId, workerOwner, now, leaseUntil)}。
     * @param taskId 参数 协议任务标识；parameter the protocol task identifier.
     * @param workerOwner 参数 工作者占有者；parameter the worker owner.
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @param leaseUntil 参数 新租约截止时刻；parameter the new lease deadline.
     * @return 返回 renew租约 的处理结果；returns whether the lease was extended.
     */
    @Override
    public Mono<Boolean> renewLease(
            String taskId,
            String workerOwner,
            Instant now,
            Instant leaseUntil) {
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        String key = required(taskId, "taskId");
        String owner = required(workerOwner, "workerOwner");
        return blocking(() -> {
            Optional<McpTaskRecordPO> current =
                    taskPersistenceRepository.findActiveByTaskKey(key);
            if (current.isEmpty() || !heldBy(current.get(), owner, now)) {
                return false;
            }
            McpTaskRecordPO row = current.get();
            return taskPersistenceRepository.updateById(McpTaskRecordPO.builder()
                    .id(row.getId())
                    .version(row.getVersion())
                    .leaseUntil(leaseUntil)
                    .updateTime(now)
                    .build());
        });
    }

    /**
     * 中文说明：执行 transition 操作；旧语句的 {@code WHERE id = ? AND state = ? AND revision = ?
     * AND (worker_owner IS NULL | worker_owner = ?)} 全部在新分配行上逐字复核，随后写入目标状态、三个载荷、
     * 业务修订自增与时间；行缺失、谓词不匹配或影响 0 行都返回 {@code false}。
     * 注意：{@code updateById} 只下发非空字段，故旧语句无条件写 {@code NULL} 的
     * {@code worker_owner}/{@code lease_until} 与被清空载荷无法经该 API 表达，已按阻塞项上报。
     * English summary: Executes the transition operation; the legacy
     * {@code WHERE id = ? AND state = ? AND revision = ? AND (worker_owner IS NULL | worker_owner = ?)} is re-checked
     * literally on the freshly loaded row, after which the target state, the three payloads, the incremented business
     * revision and the timestamp are written; a missing row, an unmatched predicate or a zero-row effect returns
     * {@code false}. Note that {@code updateById} only pushes non-null fields, so the columns the legacy statement wrote
     * as {@code NULL} unconditionally - {@code worker_owner}/{@code lease_until} and a payload cleared to
     * {@code null} - are not expressible through that API and were reported as a blocker.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.transition(transition)}。
     * @param transition 参数 迁移意图；parameter the transition intent.
     * @return 返回 transition 的处理结果；returns whether one row moved.
     */
    @Override
    public Mono<Boolean> transition(Transition transition) {
        Objects.requireNonNull(transition, "transition");
        String key = required(transition.taskId(), "taskId");
        return blocking(() -> {
            Optional<McpTaskRecordPO> current =
                    taskPersistenceRepository.findActiveByTaskKey(key);
            if (current.isEmpty()
                    || !matchesState(current.get(), transition.expectedState())
                    || !Objects.equals(current.get().getRevision(), transition.expectedRevision())
                    || !matchesExpectedOwner(current.get(), transition.expectedWorkerOwner())) {
                return false;
            }
            McpTaskRecordPO row = current.get();
            return taskPersistenceRepository.updateById(McpTaskRecordPO.builder()
                    .id(row.getId())
                    .version(row.getVersion())
                    .state(McpTaskPersistenceConverter.stateColumnOf(transition.targetState()))
                    .inputPayload(taskPersistenceConverter.payloadNode(transition.inputPayload()))
                    .resultPayload(taskPersistenceConverter.payloadNode(transition.resultPayload()))
                    .errorPayload(taskPersistenceConverter.payloadNode(transition.errorPayload()))
                    .revision(nextRevision(transition.expectedRevision()))
                    .updateTime(transition.now())
                    .build());
        });
    }

    /**
     * 中文说明：执行 cancel 操作；等价于旧 {@code UPDATE ... SET state = 'CANCELLED', revision = revision + 1,
     * updated_at = ? WHERE id = ? AND state = ? AND revision = ?}：状态与修订在新分配行上复核后单行 CAS，
     * 行缺失、不匹配或影响 0 行都返回 {@code false}。旧语句同时清空的
     * {@code worker_owner}/{@code lease_until} 受 {@code updateById} 的非空下发限制，已按阻塞项上报。
     * English summary: Executes the cancel operation; the equivalent of the legacy
     * {@code UPDATE ... SET state = 'CANCELLED', revision = revision + 1, updated_at = ? WHERE id = ? AND state = ? AND
     * revision = ?}: state and revision are re-checked on the freshly loaded row and then a single-row compare-and-set
     * moves it, so a missing row, a mismatch or a zero-row effect returns {@code false}. The
     * {@code worker_owner}/{@code lease_until} the legacy statement also cleared are limited by the non-null push-down of
     * {@code updateById} and were reported as a blocker.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.cancel(taskId, expectedState, expectedRevision, now)}。
     * @param taskId 参数 协议任务标识；parameter the protocol task identifier.
     * @param expectedState 参数 期望当前状态；parameter the expected current state.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param now 参数 取消时刻；parameter the cancellation instant.
     * @return 返回 cancel 的处理结果；returns whether one row was cancelled.
     */
    @Override
    public Mono<Boolean> cancel(
            String taskId,
            McpTask.State expectedState,
            long expectedRevision,
            Instant now) {
        String key = required(taskId, "taskId");
        Objects.requireNonNull(expectedState, "expectedState");
        Objects.requireNonNull(now, "now");
        return blocking(() -> {
            Optional<McpTaskRecordPO> current =
                    taskPersistenceRepository.findActiveByTaskKey(key);
            if (current.isEmpty()
                    || !matchesState(current.get(), expectedState)
                    || !Objects.equals(current.get().getRevision(), expectedRevision)) {
                return false;
            }
            McpTaskRecordPO row = current.get();
            return taskPersistenceRepository.updateById(McpTaskRecordPO.builder()
                    .id(row.getId())
                    .version(row.getVersion())
                    .state(McpPersistentTaskStateEnum.CANCELLED)
                    .revision(nextRevision(expectedRevision))
                    .updateTime(now)
                    .build());
        });
    }

    /**
     * 中文说明：执行 failUnavailable 操作；旧语句是一次 {@code UPDATE ... WHERE state='WORKING' AND
     * (execution_deadline <= ? OR (attempt_count >= max_attempts AND (lease_until IS NULL OR lease_until <= ?)))}，
     * 迁移后先按同一谓词具名查询至多 {@code batchLimit} 行，再逐行以 {@code id}+{@code version} 受保护 CAS
     * 写入 {@code FAILED} 与 {@code {"code":"MCP_TASK_UNAVAILABLE"}} 并自增业务修订，返回真正命中的行数。
     * English summary: Executes the fail-unavailable operation; the legacy statement was one
     * {@code UPDATE ... WHERE state='WORKING' AND (execution_deadline <= ? OR (attempt_count >= max_attempts AND
     * (lease_until IS NULL OR lease_until <= ?)))}. After migration at most {@code batchLimit} rows come from a named
     * query with those predicates and each then goes through the protected {@code id}+{@code version}
     * compare-and-set that writes {@code FAILED} plus {@code {"code":"MCP_TASK_UNAVAILABLE"}} and increments the business
     * revision, returning the number of rows actually hit.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.failUnavailable(now)}。
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @return 返回 failUnavailable 的处理结果；returns the number of tasks failed.
     */
    @Override
    public Mono<Integer> failUnavailable(Instant now) {
        Objects.requireNonNull(now, "now");
        return blocking(() -> {
            int failed = 0;
            for (McpTaskRecordPO row
                    : taskPersistenceRepository.listUnavailable(now, batchLimit)) {
                if (taskPersistenceRepository.updateById(McpTaskRecordPO.builder()
                        .id(row.getId())
                        .version(row.getVersion())
                        .state(McpPersistentTaskStateEnum.FAILED)
                        .errorPayload(taskPersistenceConverter.payloadNode(UNAVAILABLE_ERROR))
                        .revision(nextRevision(row.getRevision()))
                        .updateTime(now)
                        .build())) {
                    failed++;
                }
            }
            return failed;
        });
    }

    /**
     * 中文说明：执行 deleteExpired 操作；旧语句是一次 {@code DELETE FROM ... WHERE state IN (终态) AND
     * expires_at <= ?}，迁移后按规范修订 §11.2.35 改为具名查询至多 {@code batchLimit} 行的
     * {@code id}+{@code version}，再逐行走受守卫版本化逻辑删除；逻辑删除时间戳由组件写为
     * {@code (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')}，同刻冲突按规范以一次重读后的新事务重试，命中数如实计数。
     * English summary: Executes the delete-expired operation; the legacy statement was one
     * {@code DELETE FROM ... WHERE state IN (terminal) AND expires_at <= ?}. Per amendment section 11.2.35 this became a
     * named query for at most {@code batchLimit} rows' {@code id}+{@code version} followed by the guarded versioned
     * logical delete per row, whose timestamp the component writes as
     * {@code (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')}; a same-instant conflict is retried once against a freshly loaded
     * row as the standard requires, and the returned count reflects the rows actually deleted.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.deleteExpired(now)}。
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @return 返回 deleteExpired 的处理结果；returns the number of tasks removed.
     */
    @Override
    public Mono<Integer> deleteExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return blocking(() -> {
            int removed = 0;
            for (McpTaskRecordPO row
                    : taskPersistenceRepository.listExpiredTerminal(now, batchLimit)) {
                if (taskPersistenceRepository.removeById(row)) {
                    removed++;
                } else if (taskPersistenceRepository.getOptById(row.getId())
                        .filter(fresh -> taskPersistenceRepository.removeById(fresh))
                        .isPresent()) {
                    removed++;
                }
            }
            return removed;
        });
    }

    /**
     * 中文说明：认领成功后按技术主键重读权威行并投影为领域记录，复刻旧实现在提交后重新 {@code find} 的行为；
     * 重读不到即返回 {@code null}，绝不返回入参冒充落库结果。
     * English summary: Re-reads the authoritative row by technical primary key after a successful claim and projects it
     * onto the domain record, reproducing the legacy re-{@code find} after commit; a missing reload yields
     * {@code null} instead of echoing the argument back as a fake success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.authoritativeTask(id)}。
     * @param id 参数 技术主键；parameter the technical primary key.
     * @return 返回 权威领域记录；returns the authoritative domain record.
     */
    private McpTask authoritativeTask(Long id) {
        return taskPersistenceRepository.getOptById(id)
                .map(taskPersistenceConverter::toBusiness)
                .orElse(null);
    }

    /**
     * 中文说明：复核旧续租谓词 {@code state='WORKING' AND worker_owner = ? AND lease_until > ?}。
     * English summary: Re-checks the legacy renew predicate
     * {@code state='WORKING' AND worker_owner = ? AND lease_until > ?}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.heldBy(row, workerOwner, now)}。
     * @param row 参数 新分配行；parameter the freshly loaded row.
     * @param workerOwner 参数 工作者占有者；parameter the worker owner.
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @return 返回 是否由该工作者持有；returns whether that worker holds the lease.
     */
    private static boolean heldBy(McpTaskRecordPO row, String workerOwner, Instant now) {
        return row.getState() == WORKING
                && Objects.equals(row.getWorkerOwner(), workerOwner)
                && row.getLeaseUntil() != null
                && row.getLeaseUntil().isAfter(now);
    }

    /**
     * 中文说明：复核旧 {@code state = ?} 谓词；{@code McpTask.State} 与持久化 wire 枚举按同一 wire 字符串比较。
     * English summary: Re-checks the legacy {@code state = ?} predicate; {@code McpTask.State} and the persistence wire
     * enum are compared through the same wire string.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.matchesState(row, expectedState)}。
     * @param row 参数 新分配行；parameter the freshly loaded row.
     * @param expectedState 参数 期望状态；parameter the expected state.
     * @return 返回 是否匹配；returns whether the state matches.
     */
    private static boolean matchesState(McpTaskRecordPO row, McpTask.State expectedState) {
        return row.getState() == McpTaskPersistenceConverter.stateColumnOf(expectedState);
    }

    /**
     * 中文说明：复核旧迁移的占有者谓词：期望值为空时要求 {@code worker_owner IS NULL}，否则要求精确相等。
     * English summary: Re-checks the legacy owner predicate of a transition: {@code worker_owner IS NULL} when none is
     * expected, otherwise an exact match.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.matchesExpectedOwner(row, expectedWorkerOwner)}。
     * @param row 参数 新分配行；parameter the freshly loaded row.
     * @param expectedWorkerOwner 参数 期望占有者，可为空；parameter the expected owner, nullable.
     * @return 返回 是否匹配；returns whether the owner matches.
     */
    private static boolean matchesExpectedOwner(McpTaskRecordPO row, String expectedWorkerOwner) {
        return expectedWorkerOwner == null
                ? row.getWorkerOwner() == null
                : Objects.equals(row.getWorkerOwner(), expectedWorkerOwner);
    }

    /**
     * 中文说明：按旧 SQL 的 {@code attempt_count = attempt_count + 1} 递增读到的尝试计数，null 视为 0。
     * English summary: Increments the attempt counter that was read, matching the legacy
     * {@code attempt_count = attempt_count + 1} and treating null as zero.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpMcpRuntimeTaskStore.nextAttempt(attemptCount)}。
     * @param attemptCount 参数 读到的尝试计数；parameter the stored attempt counter.
     * @return 返回 自增后的尝试计数；returns the incremented attempt counter.
     */
    private static Integer nextAttempt(Integer attemptCount) {
        return (attemptCount == null ? 0 : attemptCount) + 1;
    }

    /**
     * 中文说明：按旧 SQL 的 {@code revision = revision + 1} 递增业务修订，null 视为 0。
     * English summary: Increments the business revision, matching the legacy {@code revision = revision + 1} and treating
     * null as zero.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpMcpRuntimeTaskStore.nextRevision(revision)}。
     * @param revision 参数 读到的修订；parameter the stored revision.
     * @return 返回 自增后的修订；returns the incremented revision.
     */
    private static Long nextRevision(Long revision) {
        return (revision == null ? 0L : revision) + 1L;
    }

    /**
     * 中文说明：执行 nextRevision 操作；以调用方期望的修订为基自增，仅在 CAS 命中时落库。
     * English summary: Executes the nextRevision operation; increments from the revision the caller observed and lands only
     * when the compare-and-set hits.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpMcpRuntimeTaskStore.nextRevision(revision)}。
     * @param revision 参数 期望修订；parameter the expected revision.
     * @return 返回 自增后的修订；returns the incremented revision.
     */
    private static Long nextRevision(long revision) {
        return revision + 1L;
    }

    /**
     * 中文说明：沿用旧实现的必填规范化：{@code null} 或空白抛
     * {@code IllegalArgumentException(field + " is required")}。
     * English summary: Keeps the legacy required normalization: {@code null} or blank raises
     * {@code IllegalArgumentException(field + " is required")}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpMcpRuntimeTaskStore.required(value, field)}。
     * @param value 参数 值；parameter value.
     * @param field 参数 字段名；parameter field.
     * @return 返回 规范化后的值；returns the trimmed value.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：把阻塞的受守卫工作放到 boundedElastic 上执行，并在该线程内进出身份上下文，
     * 使租户与审计身份只在语句期间存在；工作抛出的异常如实向下游传播。
     * English summary: Runs the blocking guarded work on boundedElastic, entering and leaving the identity context inside
     * that thread so the tenant and audit identities exist only while the statement runs; exceptions propagate unchanged.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpRuntimeTaskStore.blocking(work)}。
     * @param work 参数 受守卫工作；parameter the guarded work.
     * @param <T> 返回类型 / the return type.
     * @return 返回 异步结果；returns the asynchronous result.
     */
    private <T> Mono<T> blocking(Callable<T> work) {
        return Mono.fromCallable(() -> persistenceContext.call(work))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
