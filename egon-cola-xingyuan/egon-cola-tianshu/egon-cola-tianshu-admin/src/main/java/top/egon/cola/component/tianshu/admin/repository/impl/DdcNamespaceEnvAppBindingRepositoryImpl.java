package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.dao.DdcNamespaceEnvAppBindingMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcNamespaceEnvAppBindingConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcNamespaceEnvAppBindingPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository("ddcNamespaceEnvAppBindingRepository")
@RequiredArgsConstructor
@Validated
public class DdcNamespaceEnvAppBindingRepositoryImpl
        extends EgonColaRepository<DdcNamespaceEnvAppBindingMapper, DdcNamespaceEnvAppBindingPO>
        implements DdcNamespaceEnvAppBindingRepository {
    @Qualifier("ddcNamespaceEnvAppBindingMapper") private final DdcNamespaceEnvAppBindingMapper mapper;
    @Qualifier("ddcNamespaceEnvAppBindingConverterImpl") private final DdcNamespaceEnvAppBindingConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcNamespaceEnvAppBindingMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }
    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }
    @Override public boolean existsByNamespaceId(Long namespaceId) {
        return mapper.countByNamespaceId(tenantId(), namespaceId) > 0;
    }
    @Override public boolean existsByAppId(Long appId) { return mapper.countByAppId(tenantId(), appId) > 0; }
    @Override public boolean existsByEnvCode(String envCode) { return mapper.countByEnvCode(tenantId(), envCode) > 0; }
    @Override public boolean existsByNamespaceIdAndEnvCodeAndAppId(Long namespaceId, String envCode, Long appId) {
        return mapper.countByTuple(tenantId(), namespaceId, envCode, appId, null) > 0;
    }
    @Override public boolean existsByNamespaceIdAndEnvCodeAndAppIdAndIdNot(
            Long namespaceId, String envCode, Long appId, Long id) {
        return mapper.countByTuple(tenantId(), namespaceId, envCode, appId, id) > 0;
    }
    @Override public List<DdcNamespaceEnvAppBindingBO> findByNamespaceIdAndEnvCodeAndEnabledTrue(
            Long namespaceId, String envCode) {
        return converter.toTargetList(mapper.selectByNamespaceAndEnv(tenantId(), namespaceId, envCode, true));
    }
    @Override public List<DdcNamespaceEnvAppBindingBO> findByNamespaceIdAndEnabledTrue(Long namespaceId) {
        return converter.toTargetList(mapper.selectByNamespaceAndEnv(tenantId(), namespaceId, null, true));
    }
    @Override public List<DdcNamespaceEnvAppBindingBO> findAll(String bizCode, String namespaceCode,
            String envCode, String appCode) {
        return converter.toTargetList(mapper.selectJoinedList(tenantId(), bizCode, namespaceCode, envCode, appCode));
    }
    @Override public DdcPageResult<DdcNamespaceEnvAppBindingBO> search(String bizCode, String namespaceCode,
            String envCode, String appCode, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        List<DdcNamespaceEnvAppBindingBO> rows = converter.toTargetList(mapper.selectJoinedPage(tenantId(), bizCode,
                namespaceCode, envCode, appCode, page.offset(), page.pageSize()));
        return DdcPageResult.<DdcNamespaceEnvAppBindingBO>builder().records(rows)
                .total(mapper.countJoinedPage(tenantId(), bizCode, namespaceCode, envCode, appCode))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
    @Override public List<String> findVisibleNamespaceCodes(String bizCode, String envCode, String appCode) {
        return mapper.selectVisibleNamespaceCodes(tenantId(), bizCode, envCode, appCode);
    }
    @Override public List<Map<String, Object>> findVisiblePhysicalScopes(String bizCode, String namespaceCode) {
        return mapper.selectVisiblePhysicalScopes(tenantId(), bizCode, namespaceCode);
    }
    @Override public Optional<DdcNamespaceEnvAppBindingBO> findByPublicId(String publicId) {
        return Optional.ofNullable(mapper.selectActiveByPublicId(tenantId(), publicId)).map(converter::toTarget);
    }
    @Override public DdcNamespaceEnvAppBindingBO insert(DdcNamespaceEnvAppBindingBO command) {
        DdcNamespaceEnvAppBindingPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(tenantId());
        row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_BINDING_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS, failure);
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public DdcNamespaceEnvAppBindingBO update(DdcNamespaceEnvAppBindingBO command) {
        DdcNamespaceEnvAppBindingPO row = converter.toSource(command);
        try {
            if (!updateById(row)) throw new DdcAdminException("Tianshu binding changed concurrently; reload and retry");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw new CommonException(DdcErrorStatus.NAMESPACE_BINDING_EXISTS, failure);
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public boolean delete(DdcNamespaceEnvAppBindingBO command) {
        if (!removeById(converter.toSource(command)))
            throw new DdcAdminException("Tianshu binding changed concurrently; reload and retry");
        return true;
    }
    private boolean conflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause.getMessage() != null && (cause.getMessage().contains("uk_namespace_env_app_live")
                    || cause.getMessage().contains("uk_namespace_env_app_lifecycle"))) return true;
        return false;
    }
}
