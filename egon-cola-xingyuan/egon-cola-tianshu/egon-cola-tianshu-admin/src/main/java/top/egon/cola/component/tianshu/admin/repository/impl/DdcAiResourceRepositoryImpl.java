package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.dao.DdcAiResourceMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAiResourcePO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAiResourceRepository")
@RequiredArgsConstructor
@Validated
public class DdcAiResourceRepositoryImpl extends EgonColaRepository<DdcAiResourceMapper, DdcAiResourcePO>
        implements DdcAiResourceRepository {

    @Qualifier("ddcAiResourceMapper") private final DdcAiResourceMapper mapper;
    @Qualifier("ddcAiResourceConverterImpl") private final DdcAiResourceConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Override
    public DdcAiResourceMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public Optional<DdcAiResourceBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiResourceBO> findIncludingDeletedById(Long id) {
        return Optional.ofNullable(mapper.selectIncludingDeletedById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiResourceBO> findActiveForShareById(Long id) {
        return Optional.ofNullable(mapper.selectActiveForShareById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public Optional<DdcAiResourceBO> findActiveForUpdateById(Long id) {
        return Optional.ofNullable(mapper.selectForUpdateById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public void lockScopeParentsForShare(DdcAiScopeDTO scope) {
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        mapper.selectBizIdForShare(tenantId, scope.bizCode());
        mapper.selectNamespaceIdForShare(tenantId, scope.bizCode(), scope.namespaceCode());
        mapper.selectEnvIdForShare(tenantId, scope.env());
        mapper.selectAppIdForShare(tenantId, scope.bizCode(), scope.appCode());
        mapper.selectBindingIdForShare(tenantId, scope.bizCode(), scope.namespaceCode(), scope.env(), scope.appCode());
    }

    @Override
    public Optional<DdcAiResourceBO> findByBusinessKey(String bizCode, Long namespaceId, String envCode,
            String appCode, DdcAiKindEnum kind, String resourceKey) {
        return Optional.ofNullable(mapper.selectByBusinessKey(EgonColaTenantIdProvider.currentTenantId(),
                bizCode, namespaceId, envCode, appCode, converter.toPersistenceKind(kind), resourceKey))
                .map(converter::toTarget);
    }

    @Override
    public DdcPageResult<DdcAiResourceBO> search(String bizCode, Long namespaceId, String envCode, String appCode,
            DdcAiKindEnum kind, String resourceKeyPrefix, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        var persistedKind = kind == null ? null : converter.toPersistenceKind(kind);
        String escapedResourceKeyPrefix = escapeLikePrefix(resourceKeyPrefix);
        long offset = Math.multiplyExact((long) page.pageNo() - 1, page.pageSize());
        List<DdcAiResourceBO> records = mapper.selectPage(tenantId, bizCode, namespaceId, envCode, appCode,
                        persistedKind, escapedResourceKeyPrefix, null, offset, page.pageSize()).stream()
                .map(converter::toTarget)
                .toList();
        long total = mapper.countPage(tenantId, bizCode, namespaceId, envCode, appCode, persistedKind,
                escapedResourceKeyPrefix, null);
        return DdcPageResult.<DdcAiResourceBO>builder().records(records).total(total)
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }

    private String escapeLikePrefix(String value) {
        if (value == null || value.isEmpty()) return null;
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public DdcAiResourceBO insert(DdcAiResourceBO resource) {
        DdcAiResourcePO persisted = converter.toSource(resource);
        if (!save(persisted)) throw new IllegalStateException("AI_RESOURCE_INSERT_FAILED");
        return converter.toTarget(persisted);
    }

    @Override
    public Optional<DdcAiResourceBO> update(DdcAiResourceBO resource) {
        DdcAiResourcePO persisted = converter.toSource(resource);
        if (!updateById(persisted)) return Optional.empty();
        return Optional.of(converter.toTarget(persisted));
    }

    @Override
    public boolean delete(DdcAiResourceBO resource) {
        return removeById(converter.toSource(resource));
    }
}
