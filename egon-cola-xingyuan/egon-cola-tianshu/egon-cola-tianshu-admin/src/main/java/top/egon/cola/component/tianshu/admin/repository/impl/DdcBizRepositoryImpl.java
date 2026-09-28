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
import top.egon.cola.component.tianshu.admin.dao.DdcBizMapper;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcBizConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcBizPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcBizRepository")
@RequiredArgsConstructor
@Validated
public class DdcBizRepositoryImpl extends EgonColaRepository<DdcBizMapper, DdcBizPO>
        implements DdcBizRepository {

    @Qualifier("ddcBizMapper")
    private final DdcBizMapper mapper;

    @Qualifier("ddcBizConverterImpl")
    private final DdcBizConverter converter;

    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    @Qualifier("egonColaIdentifierGenerator")
    private final EgonColaIdentifierGenerator identifierGenerator;

    @Override
    public DdcBizMapper getBaseMapper() {
        return mapper;
    }

    @Override
    protected EgonColaMybatisPlusProperties getProperties() {
        return properties;
    }

    @Override
    public Optional<DdcBizBO> findActiveById(Long id) {
        return Optional.ofNullable(mapper.selectActiveById(EgonColaTenantIdProvider.currentTenantId(), id))
                .map(converter::toTarget);
    }

    @Override
    public Optional<DdcBizBO> findByPublicId(String publicId) {
        return Optional.ofNullable(mapper.selectActiveByPublicId(EgonColaTenantIdProvider.currentTenantId(), publicId))
                .map(converter::toTarget);
    }

    @Override
    public Optional<DdcBizBO> findByBizCode(String bizCode) {
        return Optional.ofNullable(mapper.selectActiveByBizCode(EgonColaTenantIdProvider.currentTenantId(), bizCode))
                .map(converter::toTarget);
    }

    @Override
    public boolean existsByBizCode(String bizCode) {
        return mapper.countByBizCode(EgonColaTenantIdProvider.currentTenantId(), bizCode, null) > 0;
    }

    @Override
    public boolean existsByBizCodeAndIdNot(String bizCode, Long id) {
        return mapper.countByBizCode(EgonColaTenantIdProvider.currentTenantId(), bizCode, id) > 0;
    }

    @Override
    public List<DdcBizBO> list(String keyword, Boolean enabled) {
        return converter.toTargetList(mapper.selectList(EgonColaTenantIdProvider.currentTenantId(), keyword, enabled));
    }

    @Override
    public DdcPageResult<DdcBizBO> page(String keyword, Boolean enabled, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        List<DdcBizBO> records = converter.toTargetList(mapper.selectPage(
                tenantId, keyword, enabled, page.offset(), page.pageSize()));
        long total = mapper.countList(tenantId, keyword, enabled);
        return DdcPageResult.<DdcBizBO>builder()
                .records(records)
                .total(total)
                .pageNo(page.pageNo())
                .pageSize(page.pageSize())
                .build();
    }

    @Override
    public DdcBizBO insert(DdcBizBO command) {
        DdcBizPO row = converter.toSource(command);
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        long id = identifierGenerator.nextId(row);
        row.setId(id);
        row.setTenantId(tenantId);
        row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) {
                throw new IllegalStateException("TIANSHU_BIZ_INSERT_NOT_APPLIED");
            }
        } catch (DataIntegrityViolationException failure) {
            if (constraintConflict(failure, "uk_biz_live")) {
                log.debug("Rejected duplicate live Tianshu biz key for tenantId={}", tenantId);
                throw new CommonException(DdcErrorStatus.BIZ_CODE_EXISTS, failure);
            }
            throw failure;
        }
        return converter.toTarget(row);
    }

    @Override
    public DdcBizBO update(DdcBizBO command) {
        DdcBizPO row = converter.toSource(command);
        try {
            if (!updateById(row)) {
                throw new DdcAdminException("Tianshu biz changed concurrently; reload and retry");
            }
            return converter.toTarget(row);
        } catch (DataIntegrityViolationException failure) {
            if (constraintConflict(failure, "uk_biz_live")) {
                log.debug("Rejected duplicate live Tianshu biz key for tenantId={}", row.getTenantId());
                throw new CommonException(DdcErrorStatus.BIZ_CODE_EXISTS, failure);
            }
            throw failure;
        }
    }

    @Override
    public boolean delete(DdcBizBO command) {
        boolean deleted = removeById(converter.toSource(command));
        if (!deleted) {
            throw new DdcAdminException("Tianshu biz changed concurrently; reload and retry");
        }
        return true;
    }

    private static boolean constraintConflict(Throwable failure, String constraint) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
