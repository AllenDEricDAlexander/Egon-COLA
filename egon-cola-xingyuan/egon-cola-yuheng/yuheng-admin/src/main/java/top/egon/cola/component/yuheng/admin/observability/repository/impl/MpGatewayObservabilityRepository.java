package top.egon.cola.component.yuheng.admin.observability.repository.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.yuheng.admin.observability.converter.GatewayConsumeFailurePersistenceConverter;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayConsumeFailureBO;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayAuditQueryDTO;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayProtocolCallDTO;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayRequestPointDTO;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.GatewayTraceQueryDTO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallEventSummaryPO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallMetricMinutePO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayConsumeFailureRecordPO;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.GatewayAuditVO;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.GatewayDashboardVO;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.GatewayPageVO;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.GatewayTraceVO;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayObservabilityRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayAuditLogPersistenceRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayCallEventSummaryPersistenceRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayCallMetricMinutePersistenceRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayConsumeFailurePersistenceRepository;
import top.egon.cola.component.yuheng.contract.observability.GatewayCallEventV1;

/**
 * 中文说明：{@code MpGatewayObservabilityRepository} 是网关可观测性投影与查询的 MyBatis-Plus 门面，逐方法取代退役的手写 JDBC
 * {@code JdbcGatewayObservabilityRepository}：{@code project} 保留「摘要先落、只有一行真正插入才累积分钟指标」的去重次序，
 * 重复投递由 {@code event_id} 唯一键的 {@code ON CONFLICT DO NOTHING} 如实折成 0 行并返回 false，全程不抛异常，
 * 因此不会触发 PostgreSQL「current transaction is aborted」而连带废掉同一事务内的分钟累积；
 * {@code recordFailure} 保留毒记录按 {@code (topic, partition_no, offset_no)} 位点的静默幂等；
 * {@code traces}/{@code audits} 保留「计数与分页共用同一段谓词、固定按 {@code occurred_at DESC}」的旧不变式，
 * 视图仍由映射文件按 record 构造器位置装配，门面不再手写行映射；{@code dashboard} 保留 5 个占位零值与
 * 「无数据即 NO_DATA」的口径；{@code deleteExpired} 由旧的物理 {@code DELETE} 改为受守卫软删（架构守护禁止硬删），
 * 所以到期行是被隐藏而非回收，这是本 Step 显式记录的行为差异。
 * English summary: {@code MpGatewayObservabilityRepository} is the MyBatis-Plus facade that replaces the retired hand-written
 * {@code JdbcGatewayObservabilityRepository} method by method. {@code project} keeps the legacy ordering where the minute metric is
 * accumulated only when the summary row was really inserted, with a redelivery folded to zero affected rows by the
 * {@code event_id} {@code ON CONFLICT DO NOTHING} clause: no exception is raised, so the PostgreSQL aborted-transaction state that a
 * caught {@code DuplicateKeyException} would have caused can never void the metric update sharing the same transaction.
 * {@code recordFailure} keeps the silent per-offset idempotency of poison records, {@code traces} and {@code audits} keep the invariant
 * that count and page share one predicate with a fixed {@code occurred_at DESC} order while the record views stay assembled by the
 * mapper's constructor result maps, {@code dashboard} keeps the five placeholder zeros and the {@code NO_DATA} reading, and
 * {@code deleteExpired} becomes a guarded soft delete because the architecture gate forbids hard deletes, so expired rows are hidden
 * rather than reclaimed — a behaviour difference this Step records explicitly.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayObservabilityRepository} 注入；与遗留实现一致，本门面不声明 {@code @Transactional}，
 * 「投影摘要 + 累积指标」必须留在调用方（Kafka 入库用例）的同一事务内，租户与操作者上下文由调用方经 MDC 携带，
 * 自定义写入语句的实体参数一律命名 {@code et}，由 {@code EgonColaMetaObjectHandler} 与主键生成器盖章技术列。
 * Inject it through the {@code GatewayObservabilityRepository} port; as in the legacy store this facade declares no
 * {@code @Transactional}, so the summary-and-metric pair stays inside the caller's transaction, the tenant and operator contexts are
 * carried by the caller through MDC, and every custom write statement binds its entity as {@code et} so the meta-object handler and key
 * generator stamp the technical columns.
 */
@Slf4j
@Validated
@Repository("gatewayObservabilityRepository")
@RequiredArgsConstructor
public class MpGatewayObservabilityRepository implements GatewayObservabilityRepository {

    /**
     * 中文说明：摘要行的受守卫持久化仓储，也是 {@code insertIfAbsent}、链路分页与看板聚合语句的唯一入口。
     * English summary: The guarded store for summary rows and the single entry point to the insert-if-absent, trace page and dashboard
     * aggregate statements.
     */
    @Qualifier("gatewayCallEventSummaryPersistenceRepository")
    private final GatewayCallEventSummaryPersistenceRepository summaryRepository;

    /**
     * 中文说明：分钟指标桶的受守卫持久化仓储，提供累加上插与协议分布语句。
     * English summary: The guarded store for minute buckets, providing the accumulate upsert and the protocol split.
     */
    @Qualifier("gatewayCallMetricMinutePersistenceRepository")
    private final GatewayCallMetricMinutePersistenceRepository metricRepository;

    /**
     * 中文说明：审计行的受守卫持久化仓储，提供审计计数与分页语句。
     * English summary: The guarded store for audit rows, providing the audit count and page statements.
     */
    @Qualifier("gatewayAuditLogPersistenceRepository")
    private final GatewayAuditLogPersistenceRepository auditRepository;

    /**
     * 中文说明：毒记录行的受守卫持久化仓储，提供按位点的幂等写入。
     * English summary: The guarded store for poison records, providing the idempotent per-offset write.
     */
    @Qualifier("gatewayConsumeFailurePersistenceRepository")
    private final GatewayConsumeFailurePersistenceRepository consumeFailureRepository;

    /**
     * 中文说明：{@code GatewayConsumeFailureBO} 与行模型的双向转换器。
     * English summary: The bidirectional converter between GatewayConsumeFailureBO and its row model.
     */
    @Qualifier("gatewayConsumeFailurePersistenceConverter")
    private final GatewayConsumeFailurePersistenceConverter consumeFailureConverter;

    /**
     * 中文说明：软删回收需要写审计列，操作者取自边界同一个 MDC 提供者，而非业务入参。
     * English summary: The soft-delete sweep stamps the audit column, so the operator comes from the boundary's own MDC provider rather
     * than a business argument.
     */
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;

    /**
     * 中文说明：执行 project 操作；先落摘要投影，只有 {@code event_id} 首次插入成功才累积分钟指标，重复投递返回 false。
     * English summary: Executes the project operation; the projection lands first and the minute metric is accumulated only when the
     * {@code event_id} was inserted for the first time, with a redelivery returning false.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code observability.project(event, expiresAt)}。必须在调用方事务内使用并携带可信租户上下文。
     * @param event 参数 调用事件契约；parameter call event contract。
     * @param expiresAt 参数 保留期到期时刻；parameter expiry instant。
     * @return 返回 是否首次投影；returns whether this event was projected for the first time.
     */
    @Override
    public boolean project(GatewayCallEventV1 event, Instant expiresAt) {
        if (summaryRepository.getBaseMapper()
                .insertIfAbsent(summaryRow(event, expiresAt)) == 0) {
            return false;
        }
        metricRepository.getBaseMapper().accumulate(metricRow(event));
        return true;
    }

    /**
     * 中文说明：执行 recordFailure 操作；同一 topic/partition/offset 的重复落账按旧口径静默幂等，不影响位点提交。
     * English summary: Executes the recordFailure operation; a repeated topic/partition/offset stays silently idempotent as before and never
     * disturbs offset committing.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code observability.recordFailure(failure)}。
     * @param failure 参数 消费失败载体；parameter consume failure carrier。
     */
    @Override
    public void recordFailure(GatewayConsumeFailureBO failure) {
        GatewayConsumeFailureRecordPO row = consumeFailureConverter.newRow(failure);
        if (consumeFailureRepository.getBaseMapper().insertIfAbsent(row) == 0) {
            log.debug("consume failure already recorded for topic {} partition {} offset {}",
                    row.getTopic(), row.getPartitionNo(), row.getOffsetNo());
        }
    }

    /**
     * 中文说明：执行 traces 操作；计数与分页各传同一组扁平取值，共用映射文件中同一段 {@code traceFilter}。
     * English summary: Executes the traces operation; count and page receive the same flat values and share one {@code traceFilter} in the
     * mapper.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code observability.traces(query)}。
     * @param query 参数 链路查询条件；parameter trace criteria。
     * @return 返回 链路分页；returns the trace page.
     */
    @Override
    public GatewayPageVO<GatewayTraceVO> traces(GatewayTraceQueryDTO query) {
        long total = summaryRepository.getBaseMapper().countTraces(
                query.env(), query.namespace(), query.traceId(),
                query.protocol(), query.statusCategory());
        List<GatewayTraceVO> items = summaryRepository.getBaseMapper().selectTracePage(
                query.env(), query.namespace(), query.traceId(), query.protocol(),
                query.statusCategory(), query.size(), (query.page() - 1) * query.size());
        return new GatewayPageVO<>(items, query.page(), query.size(), total);
    }

    /**
     * 中文说明：执行 dashboard 操作；组数、分钟序列、协议分布与发布成功率分别走各自的托管语句。
     * English summary: Executes the dashboard operation; the group counter, the per-minute series, the protocol split and the release
     * success rate each take their own hosted statement.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code observability.dashboard(env, namespace, since)}。
     * @param env 参数 环境；parameter environment。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param since 参数 起始时刻；parameter lower bound.
     * @return 返回 看板视图；returns the dashboard view.
     */
    @Override
    public GatewayDashboardVO dashboard(String env, String namespace, Instant since) {
        long groups = summaryRepository.getBaseMapper().countActiveGroups(env, namespace);
        List<GatewayRequestPointDTO> series =
                summaryRepository.getBaseMapper().selectRequestSeries(env, namespace, since);
        List<GatewayProtocolCallDTO> protocols =
                metricRepository.getBaseMapper().selectProtocolCalls(env, namespace, since);
        return new GatewayDashboardVO(
                groups, 0, 0, 0, 0, 0, releaseSuccessRate(env, namespace), series, protocols,
                series.isEmpty() ? "NO_DATA" : "AVAILABLE");
    }

    /**
     * 中文说明：执行 audits 操作；保留跨 {@code gateway_group}/{@code gateway_release} 的相关 EXISTS 可见性谓词。
     * English summary: Executes the audits operation, keeping the correlated {@code gateway_group}/{@code gateway_release} EXISTS visibility
     * predicate.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code observability.audits(query)}。
     * @param query 参数 审计查询条件；parameter audit criteria。
     * @return 返回 审计分页；returns the audit page.
     */
    @Override
    public GatewayPageVO<GatewayAuditVO> audits(GatewayAuditQueryDTO query) {
        long total = auditRepository.getBaseMapper().countAudits(
                query.env(), query.namespace(), query.actorId(), query.resourceId(),
                query.traceId(), query.successful());
        List<GatewayAuditVO> items = auditRepository.getBaseMapper().selectAuditPage(
                query.env(), query.namespace(), query.actorId(), query.resourceId(),
                query.traceId(), query.successful(), query.size(), (query.page() - 1) * query.size());
        return new GatewayPageVO<>(items, query.page(), query.size(), total);
    }

    /**
     * 中文说明：执行 deleteExpired 操作；旧的物理 {@code DELETE} 在此是受守卫软删，故返回的是被隐藏的行数，
     * 且只命中仍活跃的行，可重复执行。
     * English summary: Executes the deleteExpired operation; the legacy physical {@code DELETE} is a guarded soft delete here, so the count
     * is the number of hidden rows and only active rows match, which keeps the sweep repeatable.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code observability.deleteExpired(now)}。需在持有租户与操作者 MDC 的保留期任务中调用。
     * @param now 参数 当前时刻；parameter current instant。
     * @return 返回 受影响行数；returns the affected row count.
     */
    @Override
    public int deleteExpired(Instant now) {
        return summaryRepository.getBaseMapper().softDeleteExpired(
                now, EgonColaTenantIdProvider.currentTenantId(), userIdProvider.currentUserId());
    }

    /**
     * 中文说明：按旧 JDBC 的 20 个位置绑定构造摘要行，空白列一律落 NULL，{@code provider_service} 取身份映射的
     * {@code serviceKey}；技术列留空由 {@code et} 命名参数的边界盖章。
     * English summary: Builds the summary row from the twenty positional bindings the legacy JDBC used, blank-normalising the optional
     * columns and reading {@code provider_service} from the identity map's {@code serviceKey}; the technical columns stay empty because the
     * boundary stamps the parameter named {@code et}.
     *
     * 用法 / Usage: 仅由 {@link #project(GatewayCallEventV1, Instant)} 调用。
     * @param event 参数 调用事件契约；parameter call event contract。
     * @param expiresAt 参数 到期时刻；parameter expiry instant。
     * @return 返回 摘要行模型；returns the summary row.
     */
    private GatewayCallEventSummaryPO summaryRow(GatewayCallEventV1 event, Instant expiresAt) {
        GatewayCallEventV1.Routing routing = event.routing();
        GatewayCallEventV1.Request request = event.request();
        GatewayCallEventV1.Result result = event.result();
        return new GatewayCallEventSummaryPO()
                .setEventId(event.eventId())
                .setTraceId(event.trace().traceId())
                .setOccurredAt(Instant.ofEpochMilli(event.occurredAt()))
                .setCompletedAt(Instant.ofEpochMilli(event.completedAt()))
                .setDurationMs(result.durationMs())
                .setProtocol(request.protocol())
                .setAccessZone(request.accessZone())
                .setEnv(routing.env())
                .setNamespace(routing.namespace())
                .setGatewayGroupId(blankToNull(routing.gatewayGroupId()))
                .setOperationId(blankToNull(routing.operationId()))
                .setRouteId(blankToNull(routing.routeId()))
                .setResultCategory(result.category())
                .setGatewayErrorCode(blankToNull(result.gatewayErrorCode()))
                .setHttpStatus(result.httpStatus())
                .setGrpcStatus(blankToNull(result.grpcStatus()))
                .setEngineNodeId(routing.engineNodeId())
                .setProviderService(providerService(routing))
                .setAttemptCount(event.attempts().size())
                .setExpiresAt(expiresAt);
    }

    /**
     * 中文说明：按旧 JDBC 口径构造分钟指标行；{@code gateway_group_id} 保持原值不做空白归一（旧语句即如此，
     * 且冲突键的比较沿用同一取值），分桶时刻沿用 Java 侧 {@code ofEpochMilli().truncatedTo(MINUTES)}。
     * English summary: Builds the minute metric row as the legacy JDBC did, keeping the raw {@code gateway_group_id} (the retired
     * statement never blank-normalised it and the conflict key compares that same value) and the Java-side minute bucket.
     *
     * 用法 / Usage: 仅由 {@link #project(GatewayCallEventV1, Instant)} 在摘要确实新插入后调用。
     * @param event 参数 调用事件契约；parameter call event contract。
     * @return 返回 指标行模型；returns the metric row.
     */
    private GatewayCallMetricMinutePO metricRow(GatewayCallEventV1 event) {
        return new GatewayCallMetricMinutePO()
                .setBucketAt(Instant.ofEpochMilli(event.occurredAt())
                        .truncatedTo(ChronoUnit.MINUTES))
                .setEnv(event.routing().env())
                .setNamespace(event.routing().namespace())
                .setProtocol(event.request().protocol())
                .setGatewayGroupId(event.routing().gatewayGroupId())
                .setErrorCount("SUCCESS".equals(event.result().category()) ? 0L : 1L)
                .setDurationTotalMs(event.result().durationMs())
                .setDurationMaxMs(event.result().durationMs());
    }

    /**
     * 中文说明：按旧 SQL 换算发布成功率，聚合恒有一行，总数为 0 时返回 0D 而非 NaN。
     * English summary: Computes the release success rate with the legacy SQL; the ungrouped aggregate always yields one row and a zero
     * total returns 0D instead of NaN.
     *
     * 用法 / Usage: 仅由 {@link #dashboard(String, String, Instant)} 调用。
     * @param env 参数 环境；parameter environment。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 成功率；returns the success rate.
     */
    private double releaseSuccessRate(String env, String namespace) {
        List<Map<String, Object>> rows =
                summaryRepository.getBaseMapper().selectReleaseTotals(env, namespace);
        Map<String, Object> counts = rows.isEmpty() ? Map.of() : rows.get(0);
        long total = counts.get("total") == null ? 0L : ((Number) counts.get("total")).longValue();
        long succeeded = counts.get("succeeded") == null
                ? 0L : ((Number) counts.get("succeeded")).longValue();
        return total == 0 ? 0D : (double) succeeded / (double) total;
    }

    /**
     * 中文说明：空白即视为未提供，与旧 {@code blankToNull} 一致。
     * English summary: A blank counts as absent, exactly as the legacy {@code blankToNull} did.
     */
    private String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    /**
     * 中文说明：{@code provider_service} 取自路由身份映射的 {@code serviceKey}。
     * English summary: {@code provider_service} is read from the {@code serviceKey} of the routing identity map.
     */
    private String providerService(GatewayCallEventV1.Routing routing) {
        Object value = routing.providerServiceIdentity().get("serviceKey");
        return value == null ? null : String.valueOf(value);
    }
}
