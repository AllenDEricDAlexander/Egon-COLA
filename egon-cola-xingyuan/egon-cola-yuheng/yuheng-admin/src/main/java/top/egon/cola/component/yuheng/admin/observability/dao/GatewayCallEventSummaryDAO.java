package top.egon.cola.component.yuheng.admin.observability.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayRequestPointDTO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallEventSummaryPO;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.GatewayTraceVO;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * gateway_call_event_summary 的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for gateway_call_event_summary; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用；写入语句的实体参数
 * 必须命名为 {@code et}，否则 {@code EgonColaMetaObjectHandler} 与主键生成器不会盖章技术列。
 */
public interface GatewayCallEventSummaryDAO extends EgonColaMapper<GatewayCallEventSummaryPO> {

    /**
     * 中文说明：幂等写入一行调用事件投影，替代旧 {@code INSERT INTO gateway_call_event_summary (...) VALUES (...)
     * ON CONFLICT (event_id) DO NOTHING}。
     * English summary: Inserts one call-event projection idempotently, replacing the legacy
     * {@code INSERT INTO gateway_call_event_summary (...) VALUES (...) ON CONFLICT (event_id) DO NOTHING}.
     *
     * 用法 / Usage: 返回 0 表示该 {@code event_id} 已被投影，调用方据此跳过分钟聚合累加；租户、雪花主键、审计与版本列
     * 由受守卫边界盖章，调用方只填业务列。/ A zero result means the event was already projected, so the caller skips the
     * minute accumulation; the guarded boundary stamps the technical columns and callers fill business columns only.
     * @param summary 业务列已备齐的投影行；the projection row with its business columns filled.
     * @return 受影响行数，冲突时为 0；the affected row count, 0 when the event identifier conflicts.
     */
    int insertIfAbsent(@Param("et") GatewayCallEventSummaryPO summary);

    /**
     * 中文说明：链路分页总数，与 {@link #selectTracePage} 共用同一段活跃谓词，{@code env}/{@code namespace} 无条件参与过滤。
     * English summary: Counts the trace page total, sharing one active predicate with {@link #selectTracePage} where
     * {@code env} and {@code namespace} always apply.
     *
     * 用法 / Usage: 可选过滤项传 {@code null} 即不参与过滤，空白取值由门面先归一为 {@code null}。
     * Pass {@code null} to drop an optional filter; the facade normalises blanks to {@code null}.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @param traceId 链路标识，可空；the optional trace identifier.
     * @param protocol 协议，可空；the optional protocol.
     * @param statusCategory 结果类别，落 {@code result_category} 列，可空；the optional result category.
     * @return 活跃且租户可见的链路总数；the number of active, tenant-visible traces.
     */
    long countTraces(@Param("env") String env,
                     @Param("namespace") String namespace,
                     @Param("traceId") String traceId,
                     @Param("protocol") String protocol,
                     @Param("statusCategory") String statusCategory);

    /**
     * 中文说明：按发生时间倒序读取一页链路投影行，替代旧 {@code ORDER BY occurred_at DESC LIMIT :limit OFFSET :offset}，
     * 视图由门面按 {@link GatewayCallEventSummaryPO} 组装。
     * English summary: Reads one page of projected rows ordered by descending occurrence time, replacing the legacy
     * {@code ORDER BY occurred_at DESC LIMIT :limit OFFSET :offset}, while the facade assembles the views.
     *
     * 用法 / Usage: 与 {@link #countTraces} 传同一组取值，窗口按 {@code (page - 1) * size} 计算。
     * Pass the very same values as {@link #countTraces} with the window derived as {@code (page - 1) * size}.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @param traceId 链路标识，可空；the optional trace identifier.
     * @param protocol 协议，可空；the optional protocol.
     * @param statusCategory 结果类别，可空；the optional result category.
     * @param limit 页大小；the page size.
     * @param offset 行偏移；the row offset.
     * @return 当前页的投影行；the projected rows of the requested page.
     */
    List<GatewayTraceVO> selectTracePage(@Param("env") String env,
                                                    @Param("namespace") String namespace,
                                                    @Param("traceId") String traceId,
                                                    @Param("protocol") String protocol,
                                                    @Param("statusCategory") String statusCategory,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);

    /**
     * 中文说明：按分钟桶聚合调用量、错误量与分位耗时，旧 Java 侧的 {@code Math.round} 已下推为 SQL 的
     * {@code FLOOR(x + 0.5)} 取整。
     * English summary: Aggregates per-minute requests, errors and latency percentiles, with the legacy Java-side
     * {@code Math.round} pushed into SQL as {@code FLOOR(x + 0.5)}.
     *
     * 用法 / Usage: 供仪表盘时间序列使用，{@code since} 是闭区间下界。
     * Feeds the dashboard time series and {@code since} stays an inclusive lower bound.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @param since 统计起始时刻；the inclusive start instant.
     * @return 按分钟升序排列的调用序列；the per-minute series in ascending bucket order.
     */
    List<GatewayRequestPointDTO> selectRequestSeries(@Param("env") String env,
                                                     @Param("namespace") String namespace,
                                                     @Param("since") Instant since);

    /**
     * 中文说明：统计一个环境命名空间下的活跃网关组数量，替代旧 {@code deleted = FALSE} 的组计数。
     * English summary: Counts the active gateway groups of one environment and namespace, replacing the legacy
     * {@code deleted = FALSE} group counter.
     *
     * 用法 / Usage: 跨表只读语句托管在可观测性映射文件内，以免在分组模块重复声明一份 SQL。
     * A cross-table read hosted in the observability mapper so the group module declares no second statement.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @return 活跃网关组数量；the number of active gateway groups.
     */
    long countActiveGroups(@Param("env") String env, @Param("namespace") String namespace);

    /**
     * 中文说明：按旧 {@code gateway_release} 联 {@code gateway_group} 的语句取发布总数与成功数，成功率由门面换算。
     * English summary: Reads the release total and succeeded counts with the legacy join of {@code gateway_release}
     * onto {@code gateway_group}, leaving the rate to the facade.
     *
     * 用法 / Usage: 无分组的聚合恒返回一行，键为 {@code total} 与 {@code succeeded}。
     * The ungrouped aggregate always yields one row keyed by {@code total} and {@code succeeded}.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @return 含 total 与 succeeded 两键的单行结果；the single row holding both counters.
     */
    List<Map<String, Object>> selectReleaseTotals(@Param("env") String env, @Param("namespace") String namespace);

    /**
     * 中文说明：软删除已过期的事件投影，替代旧物理 {@code DELETE FROM gateway_call_event_summary WHERE expires_at < ?}；
     * 守护边界禁止硬删，故改为盖写 {@code deleted_at}、{@code update_user_id}、{@code update_time} 并递增 {@code version}。
     * English summary: Soft-deletes expired projections, replacing the legacy physical
     * {@code DELETE FROM gateway_call_event_summary WHERE expires_at < ?}; the guarded boundary forbids hard deletes, so
     * {@code deleted_at}, {@code update_user_id} and {@code update_time} are stamped and {@code version} is bumped.
     *
     * 用法 / Usage: 由保留期回收任务调用，租户与操作者取自 MDC 守卫上下文而非业务入参；重复执行只命中尚未软删的行。
     * Invoked by the retention reaper with tenant and operator read from the MDC guard context rather than business
     * arguments; re-running only matches rows that are still active.
     * @param now 判定过期的时间基准，同时写入 {@code update_time}；the expiry instant, also written as {@code update_time}.
     * @param tenantId 当前租户标识；the current tenant identifier.
     * @param operatorId 当前操作者标识；the current operator identifier.
     * @return 本次被软删除的行数；the number of rows soft-deleted.
     */
    int softDeleteExpired(@Param("now") Instant now,
                          @Param("tenantId") Long tenantId,
                          @Param("operatorId") String operatorId);
}
