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
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.dao.DdcConfigVersionMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigVersionConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcConfigVersionPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcConfigVersionRepository")
@RequiredArgsConstructor
@Validated
public class DdcConfigVersionRepositoryImpl extends EgonColaRepository<DdcConfigVersionMapper, DdcConfigVersionPO>
        implements DdcConfigVersionRepository {

    @Qualifier("ddcConfigVersionMapper") private final DdcConfigVersionMapper mapper;
    @Qualifier("ddcConfigVersionConverterImpl") private final DdcConfigVersionConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcConfigVersionMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }
    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }

    @Override public List<DdcConfigVersionBO> findByConfigIdOrderByVersionDesc(String configPublicId) {
        return converter.toTargetList(mapper.selectByConfigPublicId(tenantId(), configPublicId));
    }

    @Override public DdcPageResult<DdcConfigVersionBO> findByConfigIdOrderByVersionDescIdDesc(
            String configPublicId, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long trustedTenantId = tenantId();
        List<DdcConfigVersionBO> records = converter.toTargetList(mapper.selectPageByConfigPublicId(
                trustedTenantId, configPublicId, page.offset(), page.pageSize()));
        return DdcPageResult.<DdcConfigVersionBO>builder().records(records)
                .total(mapper.countPageByConfigPublicId(trustedTenantId, configPublicId))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }

    @Override public Optional<DdcConfigVersionBO> findByConfigIdAndVersion(String configPublicId, Long configRevision) {
        return Optional.ofNullable(mapper.selectByConfigPublicIdAndRevision(
                tenantId(), configPublicId, configRevision)).map(converter::toTarget);
    }

    @Override public Optional<DdcConfigVersionBO> findByConfigItemIdAndRevision(Long configItemId, Long configRevision) {
        return Optional.ofNullable(mapper.selectByConfigItemIdAndRevision(
                tenantId(), configItemId, configRevision)).map(converter::toTarget);
    }

    @Override public List<DdcConfigVersionBO> findByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName) {
        return converter.toTargetList(mapper.selectByBizEnvAppResource(
                tenantId(), bizCode, env, appCode, resourceName));
    }

    @Override public DdcPageResult<DdcConfigVersionBO> findPublishedRuntimeVersions(String bizCode, String env,
            String appCode, String deleteType, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long trustedTenantId = tenantId();
        List<DdcConfigVersionBO> records = converter.toTargetList(mapper.selectPublishedRuntimePage(
                trustedTenantId, bizCode, env, appCode, deleteType, page.offset(), page.pageSize()));
        return DdcPageResult.<DdcConfigVersionBO>builder().records(records)
                .total(mapper.countPublishedRuntimePage(trustedTenantId, bizCode, env, appCode, deleteType))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }

    @Override public DdcConfigVersionBO insert(DdcConfigVersionBO command) {
        DdcConfigVersionPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(tenantId());
        row.setPublicId(Long.toString(id));
        if (!save(row)) throw new IllegalStateException("TIANSHU_CONFIG_VERSION_INSERT_NOT_APPLIED");
        DdcConfigVersionBO saved = converter.toTarget(row);
        saved.setConfigId(command.getConfigId());
        return saved;
    }
}
