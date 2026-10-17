package top.egon.cola.component.tianshu.admin.repository;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.admin.dao.DdcAiUsageFeedbackMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiScoreAggregateBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiUsageFeedbackConverterImpl;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiScoreAggregatePO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcAiUsageFeedbackRepositoryImpl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiFeedbackOptimizationRepositoryContractTest {

    private static final String FEEDBACK = "top.egon.cola.component.tianshu.admin.dao.DdcAiUsageFeedbackMapper";
    private static final String POLICY = "top.egon.cola.component.tianshu.admin.dao.DdcAiOptimizationPolicyMapper";
    private static final String RUN = "top.egon.cola.component.tianshu.admin.dao.DdcAiOptimizationRunMapper";
    private static final Instant NOW = Instant.parse("2026-10-17T05:00:00Z");

    private final DdcAiUsageFeedbackMapper feedbackMapper = mock(DdcAiUsageFeedbackMapper.class);
    private final DdcAiUsageFeedbackRepositoryImpl feedbackRepository =
            new DdcAiUsageFeedbackRepositoryImpl(feedbackMapper, new DdcAiUsageFeedbackConverterImpl(), null);

    @BeforeEach
    void bindTenant() {
        MDC.put("tenantId", "42");
    }

    @AfterEach
    void clearTenant() {
        MDC.clear();
    }

    @Test
    void mapperXmlDeclaresEveryStatementOnLogicalTables() throws Exception {
        Configuration configuration = parse();

        for (String statement : List.of(FEEDBACK + ".selectActiveById", FEEDBACK + ".selectActiveByIds",
                FEEDBACK + ".selectClaimableForJudge", FEEDBACK + ".selectScoreAggregate", FEEDBACK + ".selectWorst",
                FEEDBACK + ".selectPage", FEEDBACK + ".countPage", FEEDBACK + ".selectExpiredIdsForUpdate",
                FEEDBACK + ".softDeleteByIds", FEEDBACK + ".deleteVersionedById",
                POLICY + ".selectActiveById", POLICY + ".selectActiveByIds", POLICY + ".selectActiveByResourceId",
                POLICY + ".selectActiveByResourceIdForUpdate", POLICY + ".selectEvaluableForUpdate",
                POLICY + ".selectJudgeEnabledResourceIds", POLICY + ".deleteVersionedById",
                RUN + ".selectActiveById", RUN + ".selectActiveByIds", RUN + ".selectByRequestId",
                RUN + ".selectMaxRunNo", RUN + ".selectActiveRunForResource", RUN + ".selectLatestFinished",
                RUN + ".selectPendingIds", RUN + ".selectExpiredRunning", RUN + ".selectPage", RUN + ".countPage")) {
            assertThat(configuration.hasStatement(statement)).as(statement).isTrue();
        }
    }

    @Test
    void claimAndRetentionQueriesSkipLockedTenantRows() throws Exception {
        Configuration configuration = parse();

        String claim = sql(configuration, FEEDBACK + ".selectClaimableForJudge", Map.of("tenantId", 42L,
                "resourceIds", List.of(7L, 8L), "now", NOW, "maxAttempts", 3, "limit", 50));
        String evaluable = sql(configuration, POLICY + ".selectEvaluableForUpdate", Map.of("tenantId", 42L, "limit", 50));
        String expired = sql(configuration, FEEDBACK + ".selectExpiredIdsForUpdate",
                Map.of("tenantId", 42L, "before", NOW, "limit", 1000));

        assertThat(claim).contains("from ddc_ai_usage_feedback f", "f.tenant_id = ?", "f.explicit_score is null",
                "f.judge_attempts < ?", "f.judge_state = 'judging' and f.judge_lease_until < ?",
                "for update skip locked").doesNotContain("_t0");
        assertThat(evaluable).contains("p.active_run_id is null", "nulls first", "for update skip locked");
        assertThat(expired).contains("f.occurred_at < ?", "for update skip locked");
    }

    @Test
    void pageFiltersTranslateScoreSourceAndMaxScore() throws Exception {
        Configuration configuration = parse();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", 42L);
        parameters.put("offset", 0L);
        parameters.put("limit", 20);
        parameters.put("query", DdcAiUsageFeedbackPageQuery.builder().resourceId(7L).versionNo(4L)
                .scoreSource("JUDGE").maxScore(60).pageNo(1).pageSize(20).build());

        String page = sql(configuration, FEEDBACK + ".selectPage", parameters);
        parameters.put("query", DdcAiOptimizationRunPageQuery.builder().resourceId(7L).pageNo(1).pageSize(20).build());
        String runs = sql(configuration, RUN + ".selectPage", parameters);

        assertThat(page).contains("v.version_no = ?", "f.explicit_score is null and f.judge_score is not null",
                "coalesce(f.explicit_score, f.judge_score) <= ?", "order by f.occurred_at desc, f.id desc");
        assertThat(runs).contains("r.\"trigger\"", "left join ddc_ai_version bv", "left join ddc_ai_version pv",
                "order by r.id desc").doesNotContain("r.state = ?");
    }

    @Test
    void aggregateAvoidsShardingAvgRewriteAndRoundsInJava() throws Exception {
        String aggregateSql = sql(parse(), FEEDBACK + ".selectScoreAggregate", Map.of("tenantId", 42L,
                "resourceId", 7L, "versionId", 9L, "windowStart", NOW.minusSeconds(86400), "windowEnd", NOW));
        assertThat(aggregateSql).doesNotContain("avg(", "filter (").contains("sum(coalesce(f.explicit_score, f.judge_score))");

        when(feedbackMapper.selectScoreAggregate(42L, 7L, 9L, NOW.minusSeconds(86400), NOW))
                .thenReturn(DdcAiScoreAggregatePO.builder().sampleCount(3L).scoreSum(215L).pendingJudgeCount(2L).build());
        DdcAiScoreAggregateBO aggregate = feedbackRepository.aggregate(7L, 9L, NOW.minusSeconds(86400), NOW);

        assertThat(aggregate.getSampleCount()).isEqualTo(3);
        assertThat(aggregate.getAverageScore()).isEqualByComparingTo(new BigDecimal("71.67"));
        assertThat(aggregate.getPendingJudgeCount()).isEqualTo(2);

        when(feedbackMapper.selectScoreAggregate(42L, 7L, 10L, NOW.minusSeconds(86400), NOW))
                .thenReturn(DdcAiScoreAggregatePO.builder().sampleCount(0L).build());
        assertThat(feedbackRepository.aggregate(7L, 10L, NOW.minusSeconds(86400), NOW).getAverageScore()).isNull();
    }

    @Test
    void retentionSoftDeletesOnlyLockedIds() {
        when(feedbackMapper.selectExpiredIdsForUpdate(42L, NOW, 1000)).thenReturn(List.of(1L, 2L));
        when(feedbackMapper.softDeleteByIds(eq(42L), eq(List.of(1L, 2L)), any(), anyString())).thenReturn(2);

        assertThat(feedbackRepository.softDeleteExpired(NOW, 1000)).isEqualTo(2);

        when(feedbackMapper.selectExpiredIdsForUpdate(42L, NOW, 10)).thenReturn(List.of());
        assertThat(feedbackRepository.softDeleteExpired(NOW, 10)).isZero();
        verify(feedbackMapper, never()).softDeleteByIds(eq(42L), eq(List.of()), any(), anyString());
    }

    private Configuration parse() throws Exception {
        Configuration configuration = new Configuration();
        for (String mapper : List.of("DdcAiUsageFeedbackMapper.xml", "DdcAiOptimizationPolicyMapper.xml",
                "DdcAiOptimizationRunMapper.xml")) {
            String resource = "mybatis/mapper/tianshu/" + mapper;
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(input).as(resource).isNotNull();
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        return configuration;
    }

    private static String sql(Configuration configuration, String statement, Map<String, Object> parameters) {
        return configuration.getMappedStatement(statement).getBoundSql(parameters).getSql()
                .toLowerCase().replaceAll("\\s+", " ");
    }
}
