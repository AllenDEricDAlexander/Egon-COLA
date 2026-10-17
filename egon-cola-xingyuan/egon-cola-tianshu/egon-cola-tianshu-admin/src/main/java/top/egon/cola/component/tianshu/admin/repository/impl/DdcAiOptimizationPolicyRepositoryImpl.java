package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiOptimizationPolicyMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationPolicyConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiOptimizationPolicyPO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository("ddcAiOptimizationPolicyRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiOptimizationPolicyRepositoryImpl
        extends EgonColaRepository<DdcAiOptimizationPolicyMapper, DdcAiOptimizationPolicyPO>
        implements DdcAiOptimizationPolicyRepository {

    @Qualifier("ddcAiOptimizationPolicyMapper") private final DdcAiOptimizationPolicyMapper mapper;
    @Qualifier("ddcAiOptimizationPolicyConverterImpl") private final DdcAiOptimizationPolicyConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiOptimizationPolicyMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public DdcAiOptimizationPolicyBO insert(DdcAiOptimizationPolicyBO policy) {
        DdcAiOptimizationPolicyPO persisted = converter.toSource(policy);
        if (!save(persisted)) throw new IllegalStateException("AI_OPTIMIZATION_POLICY_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public Optional<DdcAiOptimizationPolicyBO> findActiveByResourceId(Long resourceId) {
        return Optional.ofNullable(mapper.selectActiveByResourceId(tenantId(), resourceId)).map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiOptimizationPolicyBO> findActiveByResourceIdForUpdate(Long resourceId) {
        return Optional.ofNullable(mapper.selectActiveByResourceIdForUpdate(tenantId(), resourceId))
                .map(converter::toTarget);
    }

    @Override
    public List<DdcAiOptimizationPolicyBO> findEvaluableForUpdate(int limit) {
        return converter.toTargetList(mapper.selectEvaluableForUpdate(tenantId(), limit));
    }

    @Override
    public Set<Long> findJudgeEnabledResourceIds() {
        return new LinkedHashSet<>(mapper.selectJudgeEnabledResourceIds(tenantId()));
    }

    @Override
    public Optional<DdcAiOptimizationPolicyBO> update(DdcAiOptimizationPolicyBO policy) {
        DdcAiOptimizationPolicyPO persisted = converter.toSource(policy);
        if (!updateById(persisted)) return Optional.empty();
        return Optional.of(converter.toTarget(persisted));
    }

    @Override
    public boolean delete(DdcAiOptimizationPolicyBO policy) {
        return removeById(converter.toSource(policy));
    }

    private static Long tenantId() {
        return EgonColaTenantIdProvider.currentTenantId();
    }
}
