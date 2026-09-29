package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiPublicationMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiPublicationPO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiPublicationRepository;

import java.util.Optional;

@Slf4j
@Repository("ddcAiPublicationRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiPublicationRepositoryImpl extends EgonColaRepository<DdcAiPublicationMapper, DdcAiPublicationPO>
        implements DdcAiPublicationRepository {

    @Qualifier("ddcAiPublicationMapper") private final DdcAiPublicationMapper mapper;
    @Qualifier("ddcAiPublicationConverterImpl") private final DdcAiPublicationConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiPublicationMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public Optional<DdcAiPublicationBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }
}
