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
import top.egon.cola.component.tianshu.admin.dao.DdcAiVersionMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiVersionPO;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAiVersionRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiVersionRepositoryImpl extends EgonColaRepository<DdcAiVersionMapper, DdcAiVersionPO>
        implements DdcAiVersionRepository {

    @Qualifier("ddcAiVersionMapper") private final DdcAiVersionMapper mapper;
    @Qualifier("ddcAiVersionConverterImpl") private final DdcAiVersionConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiVersionMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public Optional<DdcAiVersionBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiVersionBO> findByResourceIdAndVersionNo(Long resourceId, Long versionNo) {
        return Optional.ofNullable(mapper.selectByResourceIdAndVersionNo(
                        EgonColaTenantIdProvider.currentTenantId(), resourceId, versionNo))
                .map(converter::toTarget);
    }

    @Override
    public DdcAiVersionBO insertVersion(DdcAiVersionBO version) {
        DdcAiVersionPO persisted = converter.toSource(version);
        if (!save(persisted)) throw new IllegalStateException("AI_VERSION_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public DdcPageResult<DdcAiVersionBO> pageByResourceId(Long resourceId, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        long offset = Math.multiplyExact((long) page.pageNo() - 1, page.pageSize());
        List<DdcAiVersionBO> records = converter.toTargetList(
                mapper.selectPage(tenantId, resourceId, offset, page.pageSize()));
        return DdcPageResult.<DdcAiVersionBO>builder().records(records)
                .total(mapper.countPage(tenantId, resourceId))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
}
