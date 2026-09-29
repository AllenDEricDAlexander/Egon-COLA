package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiDraftNodeMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiDraftNodePO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiDraftNodeRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAiDraftNodeRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiDraftNodeRepositoryImpl extends EgonColaRepository<DdcAiDraftNodeMapper, DdcAiDraftNodePO>
        implements DdcAiDraftNodeRepository {

    @Qualifier("ddcAiDraftNodeMapper") private final DdcAiDraftNodeMapper mapper;
    @Qualifier("ddcAiDraftNodeConverterImpl") private final DdcAiDraftNodeConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiDraftNodeMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public Optional<DdcAiDraftNodeBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public List<DdcAiDraftNodeBO> findActiveByResourceId(Long resourceId) {
        return converter.toTargetList(mapper.selectTree(EgonColaTenantIdProvider.currentTenantId(), resourceId));
    }

    @Override
    public DdcAiDraftNodeBO insert(DdcAiDraftNodeBO node) {
        DdcAiDraftNodePO persisted = converter.toSource(node);
        if (!save(persisted)) throw new IllegalStateException("AI_DRAFT_NODE_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public boolean delete(DdcAiDraftNodeBO node) {
        return removeById(converter.toSource(node));
    }
}
