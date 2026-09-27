package top.egon.cola.component.yuheng.mcp.engine.mcp.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.yuheng.mcp.engine.mcp.dao.McpTaskDAO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentTaskStateEnum;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpTaskRecordPO;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code McpTaskPersistenceRepository} 是数据面进程访问共享表 {@code gateway_mcp_task_instance} 的唯一受守卫
 * 边界，复用 {@link EgonColaRepository} 的租户过滤、仅活跃行读取与技术 {@code version} 乐观锁 CAS；继承的
 * {@code save/updateById/removeById/list} 保留为通用守卫路径，本类还暴露带上限的具名扫描和 transition/cancel
 * 单行 CAS；谓词对应旧手写 JDBC 语义，绝不按状态批量写。
 * English summary: {@code McpTaskPersistenceRepository} is the only guarded boundary through which the data-plane process
 * touches the shared table {@code gateway_mcp_task_instance}; it reuses the tenant filtering, active-row reads and the
 * technical {@code version} optimistic lock of {@link EgonColaRepository}. The inherited
 * {@code save/updateById/removeById/list} remain the generic guarded path; this class also exposes bounded named scans and
 * one-row transition/cancel CAS operations matching the legacy JDBC semantics. Writes keyed only by state are forbidden.
 *
 * 用法 / Usage: 业务代码只经本类访问 mapper；普通完整行更新走继承 API，必须写 NULL 的迁移/取消走本类具名 CAS，
 * 过期批次先按具名查询取 {@code id}+{@code version}，再逐行版本化逻辑删除。裸 Wrapper 与按状态批量写均禁止。
 * / Business code reaches the mapper only through this repository: ordinary full-row updates use inherited APIs, NULL-clearing
 * transitions/cancellation use the named CAS methods here, and expiry scans are followed by per-row versioned logical delete.
 * Raw wrappers and state-only bulk writes are forbidden.
 */
@Slf4j
@Repository("mcpTaskPersistenceRepository")
@RequiredArgsConstructor
public class McpTaskPersistenceRepository extends EgonColaRepository<McpTaskDAO, McpTaskRecordPO> {

    /** 中文说明：可认领状态，逐字对应旧 SQL 的 {@code state = 'WORKING'}。 English summary: the claimable state, matching the legacy {@code state = 'WORKING'} verbatim. */
    private static final McpPersistentTaskStateEnum WORKING = McpPersistentTaskStateEnum.WORKING;

    /** 中文说明：到期清理允许的终态集合，逐字对应旧 {@code state IN ('COMPLETED','FAILED','CANCELLED')}。 English summary: the terminal states expiry may prune, matching the legacy {@code state IN ('COMPLETED','FAILED','CANCELLED')} verbatim. */
    private static final List<McpPersistentTaskStateEnum> TERMINAL_STATES = List.of(
            McpPersistentTaskStateEnum.COMPLETED,
            McpPersistentTaskStateEnum.FAILED,
            McpPersistentTaskStateEnum.CANCELLED);

    /** 中文说明：{@code gateway_mcp_task_instance} 的映射器，语句集合见 {@code mybatis/mapper/mcp/McpTaskDAO.xml}。 English summary: the mapper for {@code gateway_mcp_task_instance}; its statements live in {@code mybatis/mapper/mcp/McpTaskDAO.xml}. */
    @Qualifier("mcpTaskDAO")
    private final McpTaskDAO mapper;

    /** 中文说明：组件配置，提供批量上限、分页上限与写守卫参数。 English summary: the component configuration supplying the batch ceiling, the page ceiling and the write-guard settings. */
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public McpTaskDAO getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    /**
     * 中文说明：把有 owner/payload 清空语义的状态迁移交给具名单行 CAS；通用 updateById 的非空字段策略无法表达这些 NULL。
     * English summary: Routes state transitions that must clear owner/payload columns through the named single-row CAS;
     * the generic updateById non-null field strategy cannot express those NULL values.
     * @param entity 携带观察到的身份/版本与目标业务字段；the entity carrying the observed identity/version and target fields.
     * @param expectedState 观察到的状态；the observed state.
     * @param expectedRevision 观察到的业务修订；the observed business revision.
     * @param expectedWorkerOwner 观察到的 worker owner，可为空；the observed worker owner, nullable.
     * @return CAS 是否影响且仅影响一行；whether the compare-and-set affected exactly one row.
     */
    public boolean compareAndSetTransition(
            McpTaskRecordPO entity,
            McpPersistentTaskStateEnum expectedState,
            long expectedRevision,
            String expectedWorkerOwner) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(expectedState, "expectedState");
        return mapper.transitionTask(entity, expectedState, expectedRevision,
                expectedWorkerOwner == null, expectedWorkerOwner) == 1;
    }

    /**
     * 中文说明：把取消交给具名单行 CAS；旧占有者与租约必须在同一 UPDATE 中清空。
     * English summary: Routes cancellation through the named single-row CAS so the previous owner and lease are cleared
     * by the same UPDATE.
     * @param entity 携带观察到的身份/版本与目标业务字段；the entity carrying the observed identity/version and target fields.
     * @param expectedState 观察到的状态；the observed state.
     * @param expectedRevision 观察到的业务修订；the observed business revision.
     * @return CAS 是否影响且仅影响一行；whether the compare-and-set affected exactly one row.
     */
    public boolean compareAndSetCancellation(
            McpTaskRecordPO entity,
            McpPersistentTaskStateEnum expectedState,
            long expectedRevision) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(expectedState, "expectedState");
        return mapper.cancelTask(entity, expectedState, expectedRevision) == 1;
    }

    /**
     * 中文说明：具名读取协议任务的活跃行，等价于旧 {@code SELECT ... WHERE id = ?}，迁移后协议标识落在
     * {@code task_key}；跨租户与已逻辑删除的行由守卫过滤自动排除。
     * English summary: The named active-row read for a protocol task, the equivalent of the legacy
     * {@code SELECT ... WHERE id = ?} now keyed by {@code task_key}; rows of another tenant or already logically deleted
     * are excluded by the guards themselves.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceRepository.findActiveByTaskKey(taskKey)}。
     * @param taskKey 参数 协议任务标识；parameter the protocol task identifier.
     * @return 返回 活跃行；returns the active row when present.
     */
    public Optional<McpTaskRecordPO> findActiveByTaskKey(String taskKey) {
        Objects.requireNonNull(taskKey, "taskKey");
        return scan(
                Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .eq(McpTaskRecordPO::getTaskKey, taskKey),
                1
        ).stream().findFirst();
    }

    /**
     * 中文说明：具名查询可认领行，逐字对应旧认领谓词（{@code state='WORKING'}、未过期、执行期限未到、
     * {@code attempt_count < max_attempts}、未被占有或租约已过期）与 {@code ORDER BY created_at, id}；
     * {@code attempt_count < max_attempts} 是跨列比较，经无参数谓词片段下推。
     * English summary: The named query for claimable rows, mirroring the legacy claim predicates ({@code state='WORKING'},
     * unexpired, execution deadline not reached, {@code attempt_count < max_attempts} and either unowned or an expired
     * lease) together with the {@code ORDER BY created_at, id} ordering; the cross-column
     * {@code attempt_count < max_attempts} comparison is pushed down as a parameter-free predicate fragment.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceRepository.listLeasable(now, limit)}。
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @param limit 参数 最大行数；parameter the maximum number of rows.
     * @return 返回 按认领次序排列的候选行；returns the candidate rows in claim order.
     */
    public List<McpTaskRecordPO> listLeasable(Instant now, int limit) {
        Objects.requireNonNull(now, "now");
        return scan(
                Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .eq(McpTaskRecordPO::getState, WORKING)
                        .gt(McpTaskRecordPO::getExpiresAt, now)
                        .gt(McpTaskRecordPO::getExecutionDeadline, now)
                        .apply("attempt_count < max_attempts")
                        .and(free -> free
                                .isNull(McpTaskRecordPO::getWorkerOwner)
                                .or()
                                .le(McpTaskRecordPO::getLeaseUntil, now))
                        .orderByAsc(McpTaskRecordPO::getCreateTime)
                        .orderByAsc(McpTaskRecordPO::getId),
                limit
        );
    }

    /**
     * 中文说明：具名查询应判为不可用的行，谓词等价于旧 {@code UPDATE ... WHERE state='WORKING' AND
     * (execution_deadline <= ? OR (attempt_count >= max_attempts AND (lease_until IS NULL OR lease_until <= ?)))}。
     * English summary: The named query for rows that must be failed as unavailable, equivalent to the legacy
     * {@code UPDATE ... WHERE state='WORKING' AND (execution_deadline <= ? OR (attempt_count >= max_attempts AND
     * (lease_until IS NULL OR lease_until <= ?)))}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceRepository.listUnavailable(now, limit)}。
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @param limit 参数 最大行数；parameter the maximum number of rows.
     * @return 返回 待失败行；returns the rows to fail.
     */
    public List<McpTaskRecordPO> listUnavailable(Instant now, int limit) {
        Objects.requireNonNull(now, "now");
        return scan(
                Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .eq(McpTaskRecordPO::getState, WORKING)
                        .and(due -> due
                                .le(McpTaskRecordPO::getExecutionDeadline, now)
                                .or(exhausted -> exhausted
                                        .apply("attempt_count >= max_attempts")
                                        .and(lease -> lease
                                                .isNull(McpTaskRecordPO::getLeaseUntil)
                                                .or()
                                                .le(McpTaskRecordPO::getLeaseUntil, now))))
                        .orderByAsc(McpTaskRecordPO::getCreateTime)
                        .orderByAsc(McpTaskRecordPO::getId),
                limit
        );
    }

    /**
     * 中文说明：具名查询到期终态行，谓词等价于旧 {@code DELETE FROM ... WHERE state IN (终态) AND expires_at <= ?}。
     * English summary: The named query for expired terminal rows, equivalent to the legacy
     * {@code DELETE FROM ... WHERE state IN (terminal) AND expires_at <= ?}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpTaskPersistenceRepository.listExpiredTerminal(now, limit)}。
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @param limit 参数 最大行数；parameter the maximum number of rows.
     * @return 返回 到期行；returns the expired rows.
     */
    public List<McpTaskRecordPO> listExpiredTerminal(Instant now, int limit) {
        Objects.requireNonNull(now, "now");
        return scan(
                Wrappers.<McpTaskRecordPO>lambdaQuery()
                        .in(McpTaskRecordPO::getState, TERMINAL_STATES)
                        .le(McpTaskRecordPO::getExpiresAt, now)
                        .orderByAsc(McpTaskRecordPO::getExpiresAt)
                        .orderByAsc(McpTaskRecordPO::getId),
                limit
        );
    }

    /**
     * 中文说明：把具名查询限定在 {@code limit} 行内经分页受守卫读取下发；分页上限由组件配置校验（最大 500），
     * 条件在交付边界前先成形一次，使参数在离开本类前已完全绑定。
     * English summary: Runs a named query through the guarded paged read capped at {@code limit} rows; the pagination
     * ceiling is validated by the component configuration (500 at most) and the condition is formed once before it is
     * handed to the boundary so its parameters are fully bound when they leave this class.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskPersistenceRepository.scan(predicate, limit)}。
     * @param predicate 参数 业务谓词；parameter the business predicate.
     * @param limit 参数 最大行数；parameter the maximum number of rows.
     * @return 返回 命中行；returns the matched rows.
     */
    private List<McpTaskRecordPO> scan(
            LambdaQueryWrapper<McpTaskRecordPO> predicate,
            int limit) {
        predicate.getSqlSegment();
        return list(new Page<>(1L, (long) Math.max(limit, 1)), predicate);
    }
}
