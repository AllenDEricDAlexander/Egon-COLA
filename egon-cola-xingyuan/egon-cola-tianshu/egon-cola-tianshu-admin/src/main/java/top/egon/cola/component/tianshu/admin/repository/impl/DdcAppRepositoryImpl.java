package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.dao.DdcAppMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAppConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcAppPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcAppRepository")
@RequiredArgsConstructor
@Validated
public class DdcAppRepositoryImpl extends EgonColaRepository<DdcAppMapper, DdcAppPO> implements DdcAppRepository {
    @Qualifier("ddcAppMapper") private final DdcAppMapper mapper;
    @Qualifier("ddcAppConverterImpl") private final DdcAppConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcAppMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }
    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }
    @Override public Optional<DdcAppBO> findByBizCodeAndAppCode(String bizCode, String appCode) {
        return Optional.ofNullable(mapper.selectActiveByBizAndAppCode(tenantId(), bizCode, appCode)).map(converter::toTarget);
    }
    @Override public Optional<DdcAppBO> findFirstByAppCodeOrderByBizCodeAsc(String appCode) {
        return Optional.ofNullable(mapper.selectFirstByAppCode(tenantId(), appCode)).map(converter::toTarget);
    }
    @Override public boolean existsByAppCode(String appCode) { return mapper.countByAppCode(tenantId(), appCode, null) > 0; }
    @Override public boolean existsByAppCodeAndIdNot(String appCode, Long id) {
        return mapper.countByAppCode(tenantId(), appCode, id) > 0;
    }
    @Override public boolean existsByBizCodeAndAppCode(String bizCode, String appCode) {
        return findByBizCodeAndAppCode(bizCode, appCode).isPresent();
    }
    @Override public boolean existsByBizCode(String bizCode) { return mapper.countByBizCode(tenantId(), bizCode) > 0; }
    @Override public List<DdcAppBO> findByBizCode(String bizCode) {
        return converter.toTargetList(mapper.selectByBizCode(tenantId(), bizCode));
    }
    @Override public List<DdcAppBO> findAllByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return converter.toTargetList(mapper.selectActiveByIds(tenantId(), ids));
    }
    @Override public List<DdcAppBO> findByAppCodeContainingIgnoreCaseOrAppNameContainingIgnoreCase(
            String appCode, String appName) {
        return converter.toTargetList(mapper.selectText(tenantId(), appCode, appName, null, null));
    }
    @Override public List<DdcAppBO> findByBizCodeAndAppCodeContainingIgnoreCaseOrBizCodeAndAppNameContainingIgnoreCase(
            String bizCode, String appCode, String ignoredBizCode, String appName) {
        return converter.toTargetList(mapper.selectText(tenantId(), appCode, appName, bizCode, null));
    }
    @Override public List<DdcAppBO> list(String bizCode, String keyword, Boolean enabled) {
        return converter.toTargetList(mapper.selectText(tenantId(), keyword, keyword, bizCode, enabled));
    }
    @Override public DdcPageResult<DdcAppBO> search(String bizCode, String namespaceCode, String env, String keyword,
            Boolean enabled, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        List<DdcAppBO> rows = converter.toTargetList(mapper.selectPage(tenantId(), bizCode, namespaceCode, env,
                keyword, enabled, page.offset(), page.pageSize()));
        return DdcPageResult.<DdcAppBO>builder().records(rows)
                .total(mapper.countPage(tenantId(), bizCode, namespaceCode, env, keyword, enabled))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
    @Override public Optional<DdcAppBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(tenantId(), id)).map(converter::toTarget);
    }
    @Override public Optional<DdcAppBO> findByPublicId(String publicId) {
        return Optional.ofNullable(mapper.selectActiveByPublicId(tenantId(), publicId)).map(converter::toTarget);
    }
    @Override public DdcAppBO insert(DdcAppBO command) {
        DdcAppPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(tenantId());
        row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_APP_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw duplicateAppCode();
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public DdcAppBO update(DdcAppBO command) {
        DdcAppPO row = converter.toSource(command);
        try {
            if (!updateById(row)) throw new DdcAdminException("Tianshu app changed concurrently; reload and retry");
        } catch (DataIntegrityViolationException failure) {
            if (conflict(failure)) throw duplicateAppCode();
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public boolean delete(DdcAppBO command) {
        if (!removeById(converter.toSource(command)))
            throw new DdcAdminException("Tianshu app changed concurrently; reload and retry");
        return true;
    }
    private boolean conflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause.getMessage() != null && (cause.getMessage().contains("uk_app_live")
                    || cause.getMessage().contains("uk_app_lifecycle"))) return true;
        return false;
    }
    private BusinessException duplicateAppCode() {
        return new BusinessException(ResultCode.CONCURRENCY_ERROR.getCode(),
                ResultCode.CONCURRENCY_ERROR.getStatus(), "app code already exists");
    }
}
