package top.egon.cola.component.yuheng.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.yuheng.admin.observability.converter.GatewayConsumeFailurePersistenceConverter;
import top.egon.cola.component.yuheng.admin.observability.dao.GatewayAuditLogDAO;
import top.egon.cola.component.yuheng.admin.observability.dao.GatewayCallEventSummaryDAO;
import top.egon.cola.component.yuheng.admin.observability.dao.GatewayCallMetricMinuteDAO;
import top.egon.cola.component.yuheng.admin.observability.dao.GatewayConsumeFailureDAO;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayConsumeFailureBO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallEventSummaryPO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallMetricMinutePO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayConsumeFailureRecordPO;
import top.egon.cola.component.yuheng.admin.observability.repository.impl.MpGatewayObservabilityRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayAuditLogPersistenceRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayCallEventSummaryPersistenceRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayCallMetricMinutePersistenceRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayConsumeFailurePersistenceRepository;
import top.egon.cola.component.yuheng.contract.observability.GatewayCallEventV1;

/**
 * 中文说明：{@code GatewayKafkaMpProjectionTest} 固定 Step 8 的 Kafka 投影合同：一条调用事件必须先落摘要、
 * 且<b>只有</b>摘要真正新增（{@code insertIfAbsent} 影响 1 行）时才累积分钟指标，因此同一 {@code event_id} 重投
 * 既不能重复计数、也不能触碰指标表，<b>更不能抛异常</b>——PostgreSQL 任一语句报错都会把当前事务打成
 * {@code current transaction is aborted}，靠 {@code catch DuplicateKeyException} 做幂等会连带废掉同事务内的分钟累积，
 * 这正是旧 {@code ON CONFLICT DO NOTHING} 必须原样保留的原因。摘要列沿用旧 JDBC 的空白归一
 * （{@code gateway_group_id} 等落 NULL），指标列必须保留旧语句的<b>原值</b>口径，分桶时刻仍按 Java 侧
 * {@code ofEpochMilli().truncatedTo(MINUTES)} 计算；到期回收只能隐藏行（受守卫软删）；迁移后的可变 SQL 必须归
 * mapper XML 所有（{@code ON CONFLICT}、{@code GREATEST}/{@code EXCLUDED} 累积、{@code percentile_cont} 分位、
 * 审计跨表 {@code EXISTS}、每个 select 带 {@code deleted_at IS NULL}、每个写语句带 {@code tenant_id}）。
 * English summary: {@code GatewayKafkaMpProjectionTest} pins the Step 8 Kafka projection contract: an event writes the summary first and
 * accumulates the per-minute metric <b>only</b> when {@code insertIfAbsent} really affected one row, so redelivering the same
 * {@code event_id} neither double counts nor touches the metric table and, above all, <b>never raises</b> — in PostgreSQL any statement
 * error turns the transaction into {@code current transaction is aborted}, so idempotency built on catching
 * {@code DuplicateKeyException} would void the metric accumulation sharing that transaction, which is exactly why the legacy
 * {@code ON CONFLICT DO NOTHING} has to survive. The summary columns keep the legacy blank-to-null normalisation while the metric column
 * must keep the legacy <b>raw</b> value, the bucket stays derived Java-side from {@code ofEpochMilli().truncatedTo(MINUTES)}, the
 * retention sweep may only hide rows (guarded soft delete), and the migrated mutable SQL must be owned by the mapper XML
 * ({@code ON CONFLICT}, the {@code GREATEST}/{@code EXCLUDED} accumulation, {@code percentile_cont} percentiles, the audit cross-table
 * {@code EXISTS}, {@code deleted_at IS NULL} in every select and {@code tenant_id} in every write).
 *
 * 用法 / Usage: 与 {@code GatewayCatalogMpTest} 同构的非数据库合同测试——mock 受守卫仓储与其具名 DAO、装配真实
 * MapStruct 转换器、用 MDC 提供受信租户 9001；不启动 Docker、数据库或 Spring 上下文（真实
 * MyBatis→guard→ShardingSphere→PG 执行属 Step 9 的授权空库验收）。/ Runs like {@code GatewayCatalogMpTest} without a database: the
 * guarded stores and their named DAOs are mocked, the real MapStruct converter is assembled, and MDC supplies tenant 9001; no Docker,
 * database or Spring context starts (real MyBatis→guard→ShardingSphere→PG execution belongs to Step 9's authorized empty-database
 * acceptance).
 */
class GatewayKafkaMpProjectionTest {

    /** 中文说明：计划的受信租户。 English summary: the plan's trusted tenant. */
    private static final String TRUSTED_TENANT = "9001";

    /** 中文说明：保留期回收软删时盖写的操作者，取自边界的 MDC 用户提供者。 */
    private static final String TRUSTED_OPERATOR = "operator-1";

    private GatewayCallEventSummaryDAO summaryDAO;
    private GatewayCallMetricMinuteDAO metricDAO;
    private GatewayAuditLogDAO auditDAO;
    private GatewayConsumeFailureDAO consumeFailureDAO;
    private MpGatewayObservabilityRepository projection;

    @BeforeEach
    void assembleFacade() {
        summaryDAO = mock(GatewayCallEventSummaryDAO.class);
        metricDAO = mock(GatewayCallMetricMinuteDAO.class);
        auditDAO = mock(GatewayAuditLogDAO.class);
        consumeFailureDAO = mock(GatewayConsumeFailureDAO.class);
        EgonColaUserIdProvider userIdProvider = mock(EgonColaUserIdProvider.class);
        when(userIdProvider.currentUserId()).thenReturn(TRUSTED_OPERATOR);
        GatewayCallEventSummaryPersistenceRepository summaryRepository =
                mock(GatewayCallEventSummaryPersistenceRepository.class);
        GatewayCallMetricMinutePersistenceRepository metricRepository =
                mock(GatewayCallMetricMinutePersistenceRepository.class);
        GatewayAuditLogPersistenceRepository auditRepository =
                mock(GatewayAuditLogPersistenceRepository.class);
        GatewayConsumeFailurePersistenceRepository consumeFailureRepository =
                mock(GatewayConsumeFailurePersistenceRepository.class);
        // 受守卫仓储的具名 DAO 是其唯一写读入口：门面一律经 getBaseMapper() 落到映射文件里的托管语句。
        when(summaryRepository.getBaseMapper()).thenReturn(summaryDAO);
        when(metricRepository.getBaseMapper()).thenReturn(metricDAO);
        when(auditRepository.getBaseMapper()).thenReturn(auditDAO);
        when(consumeFailureRepository.getBaseMapper()).thenReturn(consumeFailureDAO);
        MDC.put("tenantId", TRUSTED_TENANT);
        projection = new MpGatewayObservabilityRepository(
                summaryRepository,
                metricRepository,
                auditRepository,
                consumeFailureRepository,
                new GatewayConsumeFailurePersistenceConverter(),
                userIdProvider
        );
    }

    @AfterEach
    void clearTenantContext() {
        MDC.remove("tenantId");
    }

    @Test
    @DisplayName("首投只累积一次指标，指标写入绑定真实插入的摘要行")
    void projectsMetricOnlyWhenSummaryIsNew() {
        when(summaryDAO.insertIfAbsent(any())).thenReturn(1);

        assertThat(projection.project(event("group-1"), Instant.parse("2026-09-22T17:00:00Z"))).isTrue();

        ArgumentCaptor<GatewayCallMetricMinutePO> metric =
                ArgumentCaptor.forClass(GatewayCallMetricMinutePO.class);
        verify(metricDAO).accumulate(metric.capture());
        GatewayCallMetricMinutePO row = metric.getValue();
        assertThat(row.getBucketAt())
                .isEqualTo(Instant.ofEpochMilli(100).truncatedTo(ChronoUnit.MINUTES));
        assertThat(row.getErrorCount()).isZero();
        assertThat(row.getDurationTotalMs()).isEqualTo(1L);
        assertThat(row.getDurationMaxMs()).isEqualTo(1L);
        assertThat(row.getEnv()).isEqualTo("test");
        assertThat(row.getNamespace()).isEqualTo("default");
        assertThat(row.getProtocol()).isEqualTo("HTTP");
    }

    @Test
    @DisplayName("重投既不计两次数也不触碰指标表，且全程不抛异常")
    void duplicateDeliveryNeverDoubleCountsOrRaises() {
        when(summaryDAO.insertIfAbsent(any())).thenReturn(0);

        // 冲突必须由 ON CONFLICT DO NOTHING 折成 0 行而非异常：任何异常都会废掉同事务里的分钟累积。
        assertThatCode(() -> projection.project(event("group-1"), Instant.parse("2026-09-22T17:00:00Z")))
                .doesNotThrowAnyException();
        assertThat(projection.project(event("group-1"), Instant.parse("2026-09-22T17:00:00Z"))).isFalse();

        verify(metricDAO, never()).accumulate(any());
    }

    @Test
    @DisplayName("摘要列空白归一，指标列保留旧原值口径")
    void keepsLegacyBlankAndRawColumnConventions() {
        when(summaryDAO.insertIfAbsent(any())).thenReturn(1);

        projection.project(event("   "), Instant.parse("2026-09-22T17:00:00Z"));

        ArgumentCaptor<GatewayCallEventSummaryPO> summary =
                ArgumentCaptor.forClass(GatewayCallEventSummaryPO.class);
        verify(summaryDAO).insertIfAbsent(summary.capture());
        assertThat(summary.getValue().getGatewayGroupId()).isNull();
        assertThat(summary.getValue().getGatewayErrorCode()).isNull();
        assertThat(summary.getValue().getGrpcStatus()).isNull();
        assertThat(summary.getValue().getProviderService()).isEqualTo("order-service");
        assertThat(summary.getValue().getAttemptCount()).isZero();

        ArgumentCaptor<GatewayCallMetricMinutePO> metric =
                ArgumentCaptor.forClass(GatewayCallMetricMinutePO.class);
        verify(metricDAO).accumulate(metric.capture());
        assertThat(metric.getValue().getGatewayGroupId()).isEqualTo("   ");
    }

    @Test
    @DisplayName("到期回收改走受守卫软删并携带可信租户与操作者")
    void retentionSweepBecomesGuardedSoftDelete() {
        Instant now = Instant.parse("2026-09-22T16:30:00Z");
        when(summaryDAO.softDeleteExpired(now, 9001L, TRUSTED_OPERATOR)).thenReturn(7);

        assertThat(projection.deleteExpired(now)).isEqualTo(7);

        verify(summaryDAO).softDeleteExpired(now, 9001L, TRUSTED_OPERATOR);
    }

    @Test
    @DisplayName("毒记录重复落账保持静默幂等")
    void poisonReplayStaysIdempotent() {
        when(consumeFailureDAO.insertIfAbsent(any())).thenReturn(0);

        // 旧 JDBC 用 ON CONFLICT DO NOTHING 静默幂等：同一偏移重放不得抛错，否则 Kafka 会无限重投。
        assertThatCode(() -> projection.recordFailure(failure())).doesNotThrowAnyException();

        ArgumentCaptor<GatewayConsumeFailureRecordPO> row =
                ArgumentCaptor.forClass(GatewayConsumeFailureRecordPO.class);
        verify(consumeFailureDAO).insertIfAbsent(row.capture());
        assertThat(row.getValue().getTopic()).isEqualTo("yuheng.call-event");
        assertThat(row.getValue().getPartitionNo()).isZero();
        assertThat(row.getValue().getOffsetNo()).isEqualTo(10L);
        assertThat(row.getValue().getFailureCode()).isEqualTo("PAYLOAD_REJECTED");
    }

    @Test
    @DisplayName("可变 SQL 归 mapper XML 所有且满足架构守卫的硬性谓词")
    void migratedSqlIsOwnedByMapperXml() {
        String metric = mapperXml("mybatis/mapper/observability/GatewayCallMetricMinuteDAO.xml");
        assertThat(metric).contains("ON CONFLICT (bucket_at, env, namespace, protocol, gateway_group_id)");
        assertThat(metric).contains("GREATEST");
        assertThat(metric).contains("EXCLUDED.duration_total_ms");
        assertThat(metric).contains("request_count = gateway_call_metric_minute.request_count + 1");
        assertThat(metric).contains("tenant_id");

        String summary = mapperXml("mybatis/mapper/observability/GatewayCallEventSummaryDAO.xml");
        assertThat(summary).contains("ON CONFLICT (event_id) DO NOTHING");
        assertThat(summary).contains("percentile_cont(0.50)");
        assertThat(summary).contains("percentile_cont(0.95)");
        assertThat(summary).contains("percentile_cont(0.99)");
        assertThat(summary).contains("date_trunc('minute', occurred_at)");
        assertThat(summary).contains("deleted_at IS NULL");
        assertThat(summary).contains("ORDER BY occurred_at DESC");
        assertThat(summary).doesNotContain("${");

        String audit = mapperXml("mybatis/mapper/observability/GatewayAuditLogDAO.xml");
        assertThat(audit).contains("EXISTS (SELECT 1 FROM gateway_group gg");
        assertThat(audit).contains("gateway_release r");
        assertThat(audit).contains("a.deleted_at IS NULL");

        assertThat(mapperXml("mybatis/mapper/observability/GatewayConsumeFailureDAO.xml"))
                .contains("ON CONFLICT (topic, partition_no, offset_no) DO NOTHING");
    }

    @Test
    @DisplayName("迁移后不得残留硬删语句")
    void noHardDeleteSurvivesInTheMigratedXml() {
        List<String> migrated = List.of(
                "mybatis/mapper/observability/GatewayCallEventSummaryDAO.xml",
                "mybatis/mapper/observability/GatewayCallMetricMinuteDAO.xml",
                "mybatis/mapper/observability/GatewayConsumeFailureDAO.xml",
                "mybatis/mapper/observability/GatewayAuditLogDAO.xml",
                "mybatis/mapper/shared/IdempotencyDAO.xml"
        );
        assertThat(migrated).allSatisfy(resource ->
                assertThat(mapperXml(resource)).doesNotContain("<delete"));
    }

    private String mapperXml(String resource) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as("mapper resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private GatewayConsumeFailureBO failure() {
        return GatewayConsumeFailureBO.builder()
                .id("9000000000000001")
                .topic("yuheng.call-event")
                .partition(0)
                .offset(10L)
                .failureCode("PAYLOAD_REJECTED")
                .failureMessage("schema version mismatch")
                .payloadSha256("0".repeat(64))
                .payloadSize(12)
                .occurredAt(Instant.parse("2026-09-22T16:30:00Z"))
                .build();
    }

    private GatewayCallEventV1 event(String gatewayGroupId) {
        return new GatewayCallEventV1(
                "v1",
                "event-1",
                100,
                101,
                new GatewayCallEventV1.Trace(
                        "0123456789abcdef0123456789abcdef",
                        "0123456789abcdef",
                        true
                ),
                new GatewayCallEventV1.Request(
                        "request-1",
                        "HTTP",
                        "PUBLIC",
                        "GET",
                        "/orders/{id}",
                        0,
                        "UNSPECIFIED"
                ),
                new GatewayCallEventV1.Routing(
                        "test",
                        "default",
                        gatewayGroupId,
                        "engine-1",
                        "release-1",
                        "operation-1",
                        "route-1",
                        Map.of("serviceKey", "order-service")
                ),
                new GatewayCallEventV1.Governance(
                        "COMPLETE",
                        "ALLOW",
                        "CLOSED",
                        "ALLOW",
                        0
                ),
                new GatewayCallEventV1.Result(
                        "SUCCESS",
                        "",
                        200,
                        "",
                        0,
                        1
                ),
                List.of()
        );
    }
}
