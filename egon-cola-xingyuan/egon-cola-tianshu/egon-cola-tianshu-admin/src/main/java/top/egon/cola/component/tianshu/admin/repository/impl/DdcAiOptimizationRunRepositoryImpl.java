package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiOptimizationRunMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationRunConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationRunPO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationRunRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAiOptimizationRunRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiOptimizationRunRepositoryImpl
        extends EgonColaRepository<DdcAiOptimizationRunMapper, DdcAiOptimizationRunPO>
        implements DdcAiOptimizationRunRepository {

    @Qualifier("ddcAiOptimizationRunMapper") private final DdcAiOptimizationRunMapper mapper;
    @Qualifier("ddcAiOptimizationRunConverterImpl") private final DdcAiOptimizationRunConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiOptimizationRunMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public DdcAiOptimizationRunBO insert(DdcAiOptimizationRunBO run) {
        DdcAiOptimizationRunPO persisted = converter.toSource(run);
        if (!save(persisted)) throw new IllegalStateException("AI_OPTIMIZATION_RUN_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public Optional<DdcAiOptimizationRunBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(tenantId(), id)).map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiOptimizationRunBO> findByRequestId(String requestId) {
        return Optional.ofNullable(mapper.selectByRequestId(tenantId(), requestId)).map(converter::toTarget);
    }

    @Override
    public long nextRunNo(Long resourceId) {
        return Math.addExact(mapper.selectMaxRunNo(tenantId(), resourceId), 1L);
    }

    @Override
    public Optional<DdcAiOptimizationRunBO> findActiveRunForResource(Long resourceId) {
        return Optional.ofNullable(mapper.selectActiveRunForResource(tenantId(), resourceId)).map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiOptimizationRunBO> findLatestFinished(Long resourceId) {
        return Optional.ofNullable(mapper.selectLatestFinished(tenantId(), resourceId)).map(converter::toTarget);
    }

    @Override
    public List<Long> findPendingIds(int limit) {
        return mapper.selectPendingIds(tenantId(), limit);
    }

    @Override
    public List<DdcAiOptimizationRunBO> findExpiredRunning(Instant now, int limit) {
        return converter.toTargetList(mapper.selectExpiredRunning(tenantId(), now, limit));
    }

    @Override
    public Optional<DdcAiOptimizationRunBO> update(DdcAiOptimizationRunBO run) {
        DdcAiOptimizationRunPO persisted = converter.toSource(run);
        if (!updateById(persisted)) return Optional.empty();
        return Optional.of(converter.toTarget(persisted));
    }

    @Override
    public DdcPageResult<DdcAiOptimizationRunBO> page(DdcAiOptimizationRunPageQuery query) {
        Long tenantId = tenantId();
        long offset = Math.multiplyExact((long) query.getPageNo() - 1, query.getPageSize());
        return DdcPageResult.<DdcAiOptimizationRunBO>builder()
                .records(converter.toTargetList(mapper.selectPage(tenantId, query, offset, query.getPageSize())))
                .total(mapper.countPage(tenantId, query))
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize())
                .build();
    }

    private static Long tenantId() {
        return EgonColaTenantIdProvider.currentTenantId();
    }
}
