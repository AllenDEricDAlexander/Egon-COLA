package top.egon.cola.component.yuheng.mcp.engine.mcp.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentTaskStateEnum;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpTaskRecordPO;

/**
 * 中文说明：{@code McpTaskDAO} 是数据面进程自有的 {@code gateway_mcp_task_instance} 映射器，继承
 * {@link EgonColaMapper} 的受守卫读取与逻辑删除，仅增加 transition/cancel 两条逐行 CAS；批量过期仍先具名查询活跃行
 * 的 {@code id}/{@code version}，再逐行走受保护 CAS 或逻辑删除，不会按状态批量写入。
 * English summary: {@code McpTaskDAO} is the data-plane process' own mapper for {@code gateway_mcp_task_instance}. It
 * only inherits the guarded statements of {@link EgonColaMapper} ({@code selectActiveById}, {@code selectActiveByIds},
 * {@code deleteVersionedById}). Its only custom writes are the named per-row {@code transitionTask}/{@code cancelTask}
 * compare-and-set statements; they bind id, tenant, active state, technical version, business state/revision and the
 * expected owner where applicable, and explicitly clear owner/lease columns.
 *
 * 用法 / Usage: 由 {@code @MapperScan} 注册，仅经 {@code McpTaskPersistenceRepository} 的受守卫边界访问，
 * 业务代码不得直接注入本接口。/ Registered through {@code @MapperScan} and reached only through the guarded
 * {@code McpTaskPersistenceRepository}; business code must not inject this interface directly.
 */
public interface McpTaskDAO extends EgonColaMapper<McpTaskRecordPO> {

    /**
     * 中文说明：在单条 UPDATE 中复核观察到的行身份、技术版本、业务状态/修订与期望 owner，写出完整 payload 和目标状态，
     * 并无条件清空 worker owner/lease；0 行是并发冲突或所有权变化，不是成功。
     * English summary: Re-checks the observed row identity, technical version, business state/revision and expected owner
     * in one UPDATE, writes the complete payload and target state, and always clears the worker owner/lease; zero rows
     * means a concurrent conflict or ownership change, never success.
     * @param entity 带观察到的 id/tenant/version 与目标值的行；the row carrying observed id/tenant/version and target values.
     * @param expectedState 迁移前状态；the state before transition.
     * @param expectedRevision 迁移前业务修订；the business revision before transition.
     * @param expectedWorkerOwnerNull 是否要求 owner 为空；whether the expected owner must be null.
     * @param expectedWorkerOwner 迁移前 owner；the owner before transition.
     * @return 1 表示 CAS 命中，0 表示未命中；one when the compare-and-set hit, zero otherwise.
     */
    int transitionTask(@Param("et") McpTaskRecordPO entity,
                       @Param("expectedState") McpPersistentTaskStateEnum expectedState,
                       @Param("expectedRevision") long expectedRevision,
                       @Param("expectedWorkerOwnerNull") boolean expectedWorkerOwnerNull,
                       @Param("expectedWorkerOwner") String expectedWorkerOwner);

    /**
     * 中文说明：在单条 UPDATE 中复核行身份、技术版本、业务状态与修订，将状态改为 CANCELLED 并清空 owner/lease。
     * English summary: Re-checks row identity, technical version, business state and revision in one UPDATE, changes the
     * state to CANCELLED and clears the owner/lease.
     * @param entity 带观察到的 id/tenant/version 与取消后值的行；the row carrying observed id/tenant/version and cancel values.
     * @param expectedState 取消前状态；the state before cancellation.
     * @param expectedRevision 取消前业务修订；the business revision before cancellation.
     * @return 1 表示 CAS 命中，0 表示未命中；one when the compare-and-set hit, zero otherwise.
     */
    int cancelTask(@Param("et") McpTaskRecordPO entity,
                   @Param("expectedState") McpPersistentTaskStateEnum expectedState,
                   @Param("expectedRevision") long expectedRevision);
}
