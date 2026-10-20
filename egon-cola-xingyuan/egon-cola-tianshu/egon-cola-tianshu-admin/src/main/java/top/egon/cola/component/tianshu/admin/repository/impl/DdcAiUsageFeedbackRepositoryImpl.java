package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiUsageFeedbackMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiScoreAggregateBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiUsageFeedbackConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiScoreAggregatePO;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiUsageFeedbackPO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiUsageFeedbackRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAiUsageFeedbackRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiUsageFeedbackRepositoryImpl extends EgonColaRepository<DdcAiUsageFeedbackMapper, DdcAiUsageFeedbackPO>
        implements DdcAiUsageFeedbackRepository {

    private static final String SYSTEM_ACTOR = "system:ai-retention";

    @Qualifier("ddcAiUsageFeedbackMapper") private final DdcAiUsageFeedbackMapper mapper;
    @Qualifier("ddcAiUsageFeedbackConverterImpl") private final DdcAiUsageFeedbackConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiUsageFeedbackMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public DdcAiUsageFeedbackBO insert(DdcAiUsageFeedbackBO feedback) {
        DdcAiUsageFeedbackPO persisted = converter.toSource(feedback);
        if (!save(persisted)) throw new IllegalStateException("AI_USAGE_FEEDBACK_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public Optional<DdcAiUsageFeedbackBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(tenantId(), id)).map(converter::toTarget);
    }

    @Override
    public List<DdcAiUsageFeedbackBO> findClaimableForJudge(Collection<Long> resourceIds, Instant now,
            int maxAttempts, int limit) {
        return converter.toTargetList(mapper.selectClaimableForJudge(tenantId(), resourceIds, now, maxAttempts, limit));
    }

    @Override
    public Optional<DdcAiUsageFeedbackBO> update(DdcAiUsageFeedbackBO feedback) {
        DdcAiUsageFeedbackPO persisted = converter.toSource(feedback);
        if (!updateById(persisted)) return Optional.empty();
        return Optional.of(converter.toTarget(persisted));
    }

    @Override
    public DdcAiScoreAggregateBO aggregate(Long resourceId, Long versionId, Instant windowStart, Instant windowEnd) {
        DdcAiScoreAggregatePO row = mapper.selectScoreAggregate(tenantId(), resourceId, versionId, windowStart, windowEnd);
        long samples = row == null || row.getSampleCount() == null ? 0L : row.getSampleCount();
        long sum = row == null || row.getScoreSum() == null ? 0L : row.getScoreSum();
        long pending = row == null || row.getPendingJudgeCount() == null ? 0L : row.getPendingJudgeCount();
        BigDecimal average = samples == 0L ? null
                : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(samples), 2, RoundingMode.HALF_UP);
        return DdcAiScoreAggregateBO.builder()
                .sampleCount(Math.toIntExact(samples))
                .averageScore(average)
                .pendingJudgeCount(Math.toIntExact(pending))
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .build();
    }

    @Override
    public List<DdcAiUsageFeedbackBO> findWorst(Long resourceId, Long versionId, Instant windowStart,
            Instant windowEnd, int limit) {
        return converter.toTargetList(mapper.selectWorst(tenantId(), resourceId, versionId, windowStart, windowEnd, limit));
    }

    @Override
    public DdcPageResult<DdcAiUsageFeedbackBO> page(DdcAiUsageFeedbackPageQuery query) {
        Long tenantId = tenantId();
        long offset = Math.multiplyExact((long) query.getPageNo() - 1, query.getPageSize());
        return DdcPageResult.<DdcAiUsageFeedbackBO>builder()
                .records(converter.toTargetList(mapper.selectPage(tenantId, query, offset, query.getPageSize())))
                .total(mapper.countPage(tenantId, query))
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize())
                .build();
    }

    @Override
    public int softDeleteExpired(Instant before, int limit) {
        Long tenantId = tenantId();
        List<Long> ids = mapper.selectExpiredIdsForUpdate(tenantId, before, limit);
        if (ids.isEmpty()) return 0;
        String actor = Optional.ofNullable(MDC.get("userId")).filter(value -> !value.isBlank()).orElse(SYSTEM_ACTOR);
        Instant updateTime = Instant.now();
        int changed = 0;
        for (Long id : ids) {
            DdcAiUsageFeedbackPO row = mapper.selectActiveById(tenantId, id);
            if (row != null) {
                row.setUpdateUserId(actor).setUpdateTime(updateTime);
                if (removeById(row)) changed++;
            }
        }
        return changed;
    }

    private static Long tenantId() {
        return EgonColaTenantIdProvider.currentTenantId();
    }
}
