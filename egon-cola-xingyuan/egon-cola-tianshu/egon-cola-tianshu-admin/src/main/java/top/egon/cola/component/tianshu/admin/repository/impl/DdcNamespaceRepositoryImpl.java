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
import top.egon.cola.component.tianshu.admin.dao.DdcNamespaceMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcNamespaceConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcNamespacePO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcNamespaceRepository")
@RequiredArgsConstructor
@Validated
public class DdcNamespaceRepositoryImpl extends EgonColaRepository<DdcNamespaceMapper, DdcNamespacePO>
        implements DdcNamespaceRepository {
    @Qualifier("ddcNamespaceMapper") private final DdcNamespaceMapper mapper;
    @Qualifier("ddcNamespaceConverterImpl") private final DdcNamespaceConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcNamespaceMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }
    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }
    @Override public boolean existsByBizCode(String bizCode) { return mapper.countByBizCode(tenantId(), bizCode) > 0; }
    @Override public boolean existsByBizCodeAndNamespace(String bizCode, String namespace) {
        return mapper.countByNamespace(tenantId(), bizCode, namespace, null) > 0;
    }
    @Override public boolean existsByBizCodeAndNamespaceAndIdNot(String bizCode, String namespace, Long id) {
        return mapper.countByNamespace(tenantId(), bizCode, namespace, id) > 0;
    }
    @Override public boolean existsByBizCodeAndNamespaceCode(String bizCode, String namespaceCode) {
        return mapper.countByNamespaceCode(tenantId(), bizCode, namespaceCode, null) > 0;
    }
    @Override public boolean existsByBizCodeAndNamespaceCodeAndIdNot(String bizCode, String namespaceCode, Long id) {
        return mapper.countByNamespaceCode(tenantId(), bizCode, namespaceCode, id) > 0;
    }
    @Override public Optional<DdcNamespaceBO> findByBizCodeAndNamespaceCode(String bizCode, String namespaceCode) {
        return Optional.ofNullable(mapper.selectActiveByBizAndCode(tenantId(), bizCode, namespaceCode)).map(converter::toTarget);
    }
    @Override public Optional<DdcNamespaceBO> findByPublicId(String publicId) {
        return Optional.ofNullable(mapper.selectActiveByPublicId(tenantId(), publicId)).map(converter::toTarget);
    }
    @Override public List<DdcNamespaceBO> findByBizCode(String bizCode) {
        return converter.toTargetList(mapper.selectByBizCode(tenantId(), bizCode));
    }
    @Override public List<DdcNamespaceBO> findByBizCodeAndNamespaceContainingIgnoreCaseOrBizCodeAndNamespaceCodeContainingIgnoreCase(
            String bizCode, String namespace, String ignoredBizCode, String namespaceCode) {
        return converter.toTargetList(mapper.selectList(tenantId(), bizCode, namespace, namespaceCode));
    }
    @Override public DdcPageResult<DdcNamespaceBO> search(String bizCode, String keyword, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        List<DdcNamespaceBO> rows = converter.toTargetList(mapper.selectPage(tenantId(), bizCode, keyword,
                page.offset(), page.pageSize()));
        return DdcPageResult.<DdcNamespaceBO>builder().records(rows)
                .total(mapper.countPage(tenantId(), bizCode, keyword)).pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
    @Override public Optional<DdcNamespaceBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(tenantId(), id)).map(converter::toTarget);
    }
    @Override public DdcNamespaceBO insert(DdcNamespaceBO command) {
        DdcNamespacePO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(tenantId());
        row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_NAMESPACE_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS, failure);
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public DdcNamespaceBO update(DdcNamespaceBO command) {
        DdcNamespacePO row = converter.toSource(command);
        try {
            if (!updateById(row)) throw new DdcAdminException("Tianshu namespace changed concurrently; reload and retry");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw new CommonException(DdcErrorStatus.NAMESPACE_CODE_EXISTS, failure);
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public boolean delete(DdcNamespaceBO command) {
        if (!removeById(converter.toSource(command)))
            throw new DdcAdminException("Tianshu namespace changed concurrently; reload and retry");
        return true;
    }
    private boolean conflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause.getMessage() != null && (cause.getMessage().contains("uk_namespace_live")
                    || cause.getMessage().contains("uk_namespace_lifecycle"))) return true;
        return false;
    }
}
