package top.egon.cola.component.tianshu.admin.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.dao.DdcInstanceMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcInstanceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcInstanceConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcInstancePO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcInstanceRepository")
@RequiredArgsConstructor
@Validated
public class DdcInstanceRepositoryImpl extends EgonColaRepository<DdcInstanceMapper, DdcInstancePO>
        implements DdcInstanceRepository {

    @Qualifier("ddcInstanceMapper") private final DdcInstanceMapper mapper;
    @Qualifier("ddcInstanceConverterImpl") private final DdcInstanceConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcInstanceMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }
    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }
    private String actor() {
        String actor = MDC.get("userId");
        if (actor == null || actor.isBlank()) throw new IllegalStateException("TIANSHU_AUDIT_ACTOR_REQUIRED");
        return actor;
    }

    @Override public Optional<DdcInstanceBO> findByInstanceId(String instanceId) {
        return Optional.ofNullable(mapper.selectByInstanceId(tenantId(), instanceId)).map(converter::toTarget);
    }
    @Override public List<DdcInstanceBO> findByBizCodeAndEnvAndAppCode(String bizCode, String env, String appCode) {
        return converter.toTargetList(mapper.selectByScope(tenantId(), bizCode, env, appCode));
    }
    @Override public DdcPageResult<DdcInstanceBO> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenant = tenantId();
        List<DdcInstanceBO> records = converter.toTargetList(mapper.selectPage(tenant, bizCode, env, appCode,
                page.offset(), page.pageSize()));
        return DdcPageResult.<DdcInstanceBO>builder().records(records)
                .total(mapper.countPage(tenant, bizCode, env, appCode))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
    @Override public List<DdcInstanceBO> findByBizCodeAndEnvAndAppCodeAndStatus(
            String bizCode, String env, String appCode, String status) {
        return converter.toTargetList(mapper.selectByScopeAndStatus(tenantId(), bizCode, env, appCode, status));
    }
    @Override public List<DdcInstanceBO> findByStatusAndLeaseExpireAtLessThanEqual(
            String status, LocalDateTime leaseExpireAt) {
        return converter.toTargetList(mapper.selectByStatusAndLeaseExpireAtLessThanEqual(tenantId(), status, leaseExpireAt));
    }
    @Override public int markOfflineIfLeaseMatches(String instanceId, String leaseId, String status,
            LocalDateTime updatedAt) {
        Long tenant = tenantId();
        DdcInstancePO row = mapper.selectByInstanceId(tenant, instanceId);
        if (row == null) return 0;
        return mapper.markOfflineIfLeaseMatches(tenant, row.getId(), row.getVersion(),
                instanceId, leaseId, status, updatedAt, updatedAt.toInstant(ZoneOffset.UTC), actor());
    }
    @Override public int markResourceAdmissionOfflineAt(String resourceServerId, String bizCode,
            String env, String appCode, long resourceVersion, LocalDateTime updatedAt) {
        Long tenant = tenantId();
        String actor = actor();
        int changed = 0;
        for (DdcInstancePO row : mapper.selectByScopeAndStatus(tenant, bizCode, env, appCode, "ONLINE")) {
            if (java.util.Objects.equals(resourceServerId, row.getResourceServerId())
                    && row.getResourceVersion() != null && row.getResourceVersion() <= resourceVersion) {
                changed += mapper.markResourceAdmissionOffline(tenant, row.getId(), row.getVersion(),
                        resourceServerId, bizCode, env, appCode, resourceVersion, updatedAt,
                        updatedAt.toInstant(ZoneOffset.UTC), actor);
            }
        }
        return changed;
    }
    @Override public DdcInstanceBO insert(DdcInstanceBO command) {
        DdcInstancePO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id).setTenantId(tenantId());
        if (row.getPublicId() == null || row.getPublicId().isBlank()) row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_INSTANCE_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (businessKeyConflict(failure)) {
                throw new DdcAdminException("instance id already exists for this tenant", failure);
            }
            throw failure;
        }
        return converter.toTarget(row);
    }
    @Override public DdcInstanceBO update(DdcInstanceBO command) {
        DdcInstancePO row = converter.toSource(command);
        if (!updateById(row)) throw new DdcAdminException("instance version changed");
        return converter.toTarget(row);
    }

    private boolean businessKeyConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("uk_instance_business")) return true;
        }
        return false;
    }
}
