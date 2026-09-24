package top.egon.cola.component.yuheng.admin.observability.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayConsumeFailureRecordPO;

/**
 * gateway_call_event_consume_failure 的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for gateway_call_event_consume_failure; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用；毒记录登记的实体参数
 * 必须命名为 {@code et}，否则 {@code EgonColaMetaObjectHandler} 不会盖章技术列。
 */
public interface GatewayConsumeFailureDAO extends EgonColaMapper<GatewayConsumeFailureRecordPO> {

    /**
     * 中文说明：幂等登记一条消费失败记录，替代旧 {@code INSERT INTO gateway_call_event_consume_failure (...) VALUES (...)
     * ON CONFLICT (topic, partition_no, offset_no) DO NOTHING}。
     * English summary: Records one consume failure idempotently, replacing the legacy
     * {@code INSERT INTO gateway_call_event_consume_failure (...) VALUES (...) ON CONFLICT (topic, partition_no,
     * offset_no) DO NOTHING}.
     *
     * 用法 / Usage: Kafka 重放会命中同一分区位点，返回 0 即该位点已登记过；调用方按旧口径丢弃返回值。
     * A Kafka replay hits the same partition offset, so 0 means the failure was already recorded and the caller
     * discards the value exactly as before.
     * @param failure 业务列已备齐的失败行；the failure row with its business columns filled.
     * @return 受影响行数，位点冲突时为 0；the affected row count, 0 when the position conflicts.
     */
    int insertIfAbsent(@Param("et") GatewayConsumeFailureRecordPO failure);
}
