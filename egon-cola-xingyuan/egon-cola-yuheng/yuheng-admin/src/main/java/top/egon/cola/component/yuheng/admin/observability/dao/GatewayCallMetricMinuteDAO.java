package top.egon.cola.component.yuheng.admin.observability.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayProtocolCallDTO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallMetricMinutePO;

import java.time.Instant;
import java.util.List;

/**
 * gateway_call_metric_minute 的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for gateway_call_metric_minute; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用；累加上插的实体参数
 * 必须命名为 {@code et}，否则 {@code EgonColaMetaObjectHandler} 与主键生成器不会盖章技术列。
 */
public interface GatewayCallMetricMinuteDAO extends EgonColaMapper<GatewayCallMetricMinutePO> {

    /**
     * 中文说明：把一次调用累加进所属分钟桶，替代旧 {@code INSERT INTO gateway_call_metric_minute (...) VALUES (?, ?, ?,
     * ?, ?, 1, ?, ?, ?) ON CONFLICT (bucket_at, env, namespace, protocol, gateway_group_id) DO UPDATE SET ...}，
     * 保持 {@code request_count} 加一、{@code error_count} 与 {@code duration_total_ms} 累加、{@code duration_max_ms}
     * 取 {@code GREATEST} 的语义。
     * English summary: Accumulates one call into its minute bucket, replacing the legacy
     * {@code INSERT INTO gateway_call_metric_minute (...) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?) ON CONFLICT (bucket_at,
     * env, namespace, protocol, gateway_group_id) DO UPDATE SET ...} and keeping {@code request_count} incremented by
     * one, {@code error_count} and {@code duration_total_ms} summed from {@code EXCLUDED} and {@code duration_max_ms}
     * resolved by {@code GREATEST}.
     *
     * 用法 / Usage: 仅在事件投影确实新写入后调用；行内的 {@code requestCount} 不参与 SQL，因为冲突分支自行加一。
     * Call only after the projection row was really inserted; the row's {@code requestCount} stays unbound because the
     * conflict branch increments by itself.
     * @param metric 聚合增量与业务列已备齐的分钟桶行；the minute bucket row carrying the increments and business columns.
     * @return 受影响行数；the affected row count.
     */
    int accumulate(@Param("et") GatewayCallMetricMinutePO metric);

    /**
     * 中文说明：按协议汇总分钟请求量，替代旧仪表盘的 {@code SELECT protocol, sum(request_count) AS value ...
     * GROUP BY protocol ORDER BY protocol}。
     * English summary: Sums the per-minute request counts by protocol, replacing the legacy dashboard statement
     * {@code SELECT protocol, sum(request_count) AS value ... GROUP BY protocol ORDER BY protocol}.
     *
     * 用法 / Usage: 供仪表盘协议分布使用，{@code since} 是闭区间下界。
     * Feeds the dashboard protocol split and {@code since} stays an inclusive lower bound.
     * @param env 运行环境；the environment.
     * @param namespace 命名空间；the namespace.
     * @param since 统计起始时刻；the inclusive start instant.
     * @return 按协议名升序排列的调用量；the protocol call totals in ascending protocol order.
     */
    List<GatewayProtocolCallDTO> selectProtocolCalls(@Param("env") String env,
                                                     @Param("namespace") String namespace,
                                                     @Param("since") Instant since);
}
