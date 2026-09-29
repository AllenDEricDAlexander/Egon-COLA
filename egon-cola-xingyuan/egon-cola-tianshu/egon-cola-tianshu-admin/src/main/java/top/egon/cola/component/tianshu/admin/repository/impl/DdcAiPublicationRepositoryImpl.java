package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.tianshu.admin.dao.DdcAiPublicationMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiPublicationPO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiPublicationRepository;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
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

    @Override
    public Optional<DdcAiPublicationBO> findByTenantRequestId(String requestId) {
        return Optional.ofNullable(mapper.selectByTenantRequestId(
                        EgonColaTenantIdProvider.currentTenantId(), requestId))
                .map(converter::toTarget);
    }

    @Override
    public DdcAiPublicationBO insertPublication(DdcAiPublicationBO publication) {
        DdcAiPublicationPO persisted = converter.toSource(publication);
        if (!save(persisted)) throw new IllegalStateException("AI_PUBLICATION_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public DdcPageResult<DdcAiPublicationBO> pageByResourceId(Long resourceId, String requestId,
            PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        long offset = Math.multiplyExact((long) page.pageNo() - 1, page.pageSize());
        List<DdcAiPublicationBO> records = converter.toTargetList(
                mapper.selectPage(tenantId, resourceId, requestId, offset, page.pageSize()));
        return DdcPageResult.<DdcAiPublicationBO>builder().records(records)
                .total(mapper.countPage(tenantId, resourceId, requestId))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
}
