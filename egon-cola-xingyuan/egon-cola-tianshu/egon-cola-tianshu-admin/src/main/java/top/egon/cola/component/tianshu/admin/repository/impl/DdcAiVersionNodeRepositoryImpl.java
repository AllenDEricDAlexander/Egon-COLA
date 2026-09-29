package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiVersionNodeMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionNodeConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiVersionNodePO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAiVersionNodeRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiVersionNodeRepositoryImpl extends EgonColaRepository<DdcAiVersionNodeMapper, DdcAiVersionNodePO>
        implements DdcAiVersionNodeRepository {

    @Qualifier("ddcAiVersionNodeMapper") private final DdcAiVersionNodeMapper mapper;
    @Qualifier("ddcAiVersionNodeConverterImpl") private final DdcAiVersionNodeConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiVersionNodeMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public Optional<DdcAiVersionNodeBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public List<DdcAiVersionNodeBO> findTreeByVersionId(Long versionId) {
        return converter.toTargetList(mapper.selectTree(EgonColaTenantIdProvider.currentTenantId(), versionId));
    }

    @Override
    public List<DdcAiVersionNodeBO> insertAll(List<DdcAiVersionNodeBO> nodes) {
        List<DdcAiVersionNodeBO> inserted = new ArrayList<>(nodes.size());
        for (DdcAiVersionNodeBO node : nodes) {
            DdcAiVersionNodePO persisted = converter.toSource(node);
            if (!save(persisted)) throw new IllegalStateException("AI_VERSION_NODE_INSERT_FAILED");
            inserted.add(converter.toTarget(persisted));
        }
        return inserted;
    }
}
