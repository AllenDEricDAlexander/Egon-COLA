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
import top.egon.cola.component.tianshu.admin.dao.DdcEnvMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcEnvConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcEnvPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcEnvRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcEnvRepository")
@RequiredArgsConstructor
@Validated
public class DdcEnvRepositoryImpl extends EgonColaRepository<DdcEnvMapper, DdcEnvPO> implements DdcEnvRepository {
    @Qualifier("ddcEnvMapper") private final DdcEnvMapper mapper;
    @Qualifier("ddcEnvConverterImpl") private final DdcEnvConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcEnvMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }

    @Override public Optional<DdcEnvBO> findByEnvCode(String envCode) {
        return Optional.ofNullable(mapper.selectActiveByEnvCode(EgonColaTenantIdProvider.currentTenantId(), envCode))
                .map(converter::toTarget);
    }
    @Override public boolean existsByEnvCode(String envCode) {
        return mapper.countByEnvCode(EgonColaTenantIdProvider.currentTenantId(), envCode, null) > 0;
    }
    @Override public boolean existsByEnvCodeAndIdNot(String envCode, Long id) {
        return mapper.countByEnvCode(EgonColaTenantIdProvider.currentTenantId(), envCode, id) > 0;
    }
    @Override public List<DdcEnvBO> findAllByOrderBySortOrderAsc() {
        return converter.toTargetList(mapper.selectAllActive(EgonColaTenantIdProvider.currentTenantId()));
    }
    @Override public List<DdcEnvBO> findByEnvCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String envCode, String description) {
        return converter.toTargetList(mapper.selectByText(EgonColaTenantIdProvider.currentTenantId(), envCode, description));
    }
    @Override public DdcPageResult<DdcEnvBO> search(String bizCode, String namespaceCode, String keyword,
            PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        List<DdcEnvBO> rows = converter.toTargetList(mapper.selectPage(tenantId, bizCode, namespaceCode, keyword,
                page.offset(), page.pageSize()));
        return DdcPageResult.<DdcEnvBO>builder().records(rows)
                .total(mapper.countPage(tenantId, bizCode, namespaceCode, keyword))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
    @Override public DdcEnvBO insert(DdcEnvBO command) {
        DdcEnvPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(EgonColaTenantIdProvider.currentTenantId());
        row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_ENV_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw new CommonException(DdcErrorStatus.ENV_CODE_EXISTS, failure);
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public DdcEnvBO update(DdcEnvBO command) {
        DdcEnvPO row = converter.toSource(command);
        try {
            if (!updateById(row)) throw new DdcAdminException("Tianshu env changed concurrently; reload and retry");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw new CommonException(DdcErrorStatus.ENV_CODE_EXISTS, failure);
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public boolean delete(DdcEnvBO command) {
        if (!removeById(converter.toSource(command)))
            throw new DdcAdminException("Tianshu env changed concurrently; reload and retry");
        return true;
    }
    private boolean conflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause.getMessage() != null && (cause.getMessage().contains("uk_env_live")
                    || cause.getMessage().contains("uk_env_lifecycle"))) return true;
        return false;
    }
}
