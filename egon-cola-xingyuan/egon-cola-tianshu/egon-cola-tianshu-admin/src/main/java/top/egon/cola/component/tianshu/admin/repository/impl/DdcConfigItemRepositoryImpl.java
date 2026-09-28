package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.dao.DdcConfigItemMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigItemConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcConfigItemPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository("ddcConfigItemRepository")
@RequiredArgsConstructor
@Validated
public class DdcConfigItemRepositoryImpl extends EgonColaRepository<DdcConfigItemMapper, DdcConfigItemPO>
        implements DdcConfigItemRepository {

    @Qualifier("ddcConfigItemMapper") private final DdcConfigItemMapper mapper;
    @Qualifier("ddcConfigItemConverterImpl") private final DdcConfigItemConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcConfigItemMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }

    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }

    @Override public Optional<DdcConfigItemBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(tenantId(), id)).map(converter::toTarget);
    }

    @Override public Optional<DdcConfigItemBO> findByPublicId(String publicId) {
        return Optional.ofNullable(mapper.selectActiveByPublicId(tenantId(), publicId)).map(converter::toTarget);
    }

    @Override public Optional<DdcConfigItemBO> findByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName) {
        return Optional.ofNullable(mapper.selectByScopeAndResource(
                tenantId(), bizCode, env, appCode, resourceName)).map(converter::toTarget);
    }

    @Override public Optional<DdcConfigItemBO> findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(
            String bizCode, String env, String appCode, String resourceName) {
        return Optional.ofNullable(mapper.selectForPublishByScopeAndResource(
                tenantId(), bizCode, env, appCode, resourceName)).map(converter::toTarget);
    }

    @Override public boolean existsByEnv(String env) {
        return mapper.countByEnv(tenantId(), env) > 0;
    }

    @Override public boolean existsByBizCodeAndAppCode(String bizCode, String appCode) {
        return mapper.countByBizCodeAndAppCode(tenantId(), bizCode, appCode) > 0;
    }

    @Override public List<DdcConfigItemBO> findByBizCodeAndEnvAndAppCode(String bizCode, String env, String appCode) {
        return converter.toTargetList(mapper.selectByScope(tenantId(), bizCode, env, appCode));
    }

    @Override public List<DdcConfigItemBO> findByBizCodeAndEnvAndAppCodeAndDeletedFalse(
            String bizCode, String env, String appCode) {
        return converter.toTargetList(mapper.selectByScopeAndDeletedFalse(tenantId(), bizCode, env, appCode));
    }

    @Override public List<DdcConfigItemBO> search(String bizCode, String namespaceCode, String env, String appCode,
            String resourceName, boolean includeDeleted) {
        return converter.toTargetList(mapper.selectSearch(tenantId(), bizCode, namespaceCode, env,
                appCode, resourceName, includeDeleted));
    }

    @Override public DdcPageResult<DdcConfigItemBO> search(String bizCode, String namespaceCode, String env,
            String appCode, String resourceName, boolean includeDeleted, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long trustedTenantId = tenantId();
        List<DdcConfigItemBO> records = converter.toTargetList(mapper.selectPage(trustedTenantId, bizCode,
                namespaceCode, env, appCode, resourceName, includeDeleted, page.offset(), page.pageSize()));
        return DdcPageResult.<DdcConfigItemBO>builder().records(records)
                .total(mapper.countPage(trustedTenantId, bizCode, namespaceCode, env,
                        appCode, resourceName, includeDeleted))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }

    @Override public DdcConfigItemBO insert(DdcConfigItemBO command) {
        DdcConfigItemPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(tenantId());
        row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_CONFIG_ITEM_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (businessKeyConflict(failure)) throw new DdcAdminException("config item already exists", failure);
            throw failure;
        }
        return converter.toTarget(row);
    }

    @Override public DdcConfigItemBO update(DdcConfigItemBO command) {
        DdcConfigItemPO row = converter.toSource(command);
        try {
            if (!updateById(row)) throw new DdcAdminException("config version changed");
        } catch (DataIntegrityViolationException failure) {
            if (businessKeyConflict(failure)) throw new DdcAdminException("config item already exists", failure);
            throw failure;
        }
        return converter.toTarget(row);
    }

    @Override
    @Transactional
    public int advancePublishedVersion(String publicId, Long expectedPublishedVersion, Long targetVersion,
            LocalDateTime updatedAt) {
        DdcConfigItemBO item = findByPublicId(publicId).orElse(null);
        if (item == null || !Objects.equals(item.getPublishedVersion(), expectedPublishedVersion)) return 0;
        item.setPublishedVersion(targetVersion).setUpdatedAt(updatedAt);
        return updateById(converter.toSource(item)) ? 1 : 0;
    }

    private boolean businessKeyConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("uk_config_item_business")) return true;
        }
        return false;
    }
}
