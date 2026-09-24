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
 * {@code save/updateById/removeById/list} 是最终语句，本类只再暴露具名扫描查询（按协议键、可认领、不可用、到期终态），
 * 谓词逐字对应旧手写 JDBC 语句，并且一律带 {@code limit} 上限——受守卫 API 不接受只按状态的批量写。
 * English summary: {@code McpTaskPersistenceRepository} is the only guarded boundary through which the data-plane process
 * touches the shared table {@code gateway_mcp_task_instance}; it reuses the tenant filtering, active-row reads and the
 * technical {@code version} optimistic lock of {@link EgonColaRepository}. The inherited
 * {@code save/updateById/removeById/list} are the final statements and this class adds only the named scan queries (by
 * protocol key, claimable, unavailable, expired terminal) whose predicates match the legacy hand-written JDBC statements
 * verbatim and are always capped by {@code limit}, because the guarded API accepts no bulk write keyed by state alone.
 *
 * 用法 / Usage: 业务侧只经本类的具名查询取回活跃行的 {@code id}+{@code version}，再走继承的受保护单行 CAS 或
 * 版本化逻辑删除；禁止裸 Wrapper 绕过守卫，也禁止自定义按状态批量写入的语句。/ Business code takes only the
 * {@code id}+{@code version} of active rows from the named queries here and then uses the inherited protected single-row
 * compare-and-set or versioned logical delete; a raw wrapper that bypasses the guards and a custom bulk write by state are
 * both forbidden.
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
