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
import top.egon.cola.component.tianshu.admin.dao.DdcPublishTaskMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcPublishTaskConverter;
import top.egon.cola.component.tianshu.admin.model.po.DdcPublishTaskPO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("ddcPublishTaskRepository")
@RequiredArgsConstructor
@Validated
public class DdcPublishTaskRepositoryImpl extends EgonColaRepository<DdcPublishTaskMapper, DdcPublishTaskPO>
        implements DdcPublishTaskRepository {

    @Qualifier("ddcPublishTaskMapper") private final DdcPublishTaskMapper mapper;
    @Qualifier("ddcPublishTaskConverterImpl") private final DdcPublishTaskConverter converter;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("egonColaIdentifierGenerator") private final EgonColaIdentifierGenerator identifierGenerator;

    @Override public DdcPublishTaskMapper getBaseMapper() { return mapper; }
    @Override protected EgonColaMybatisPlusProperties getProperties() { return properties; }

    private Long tenantId() { return EgonColaTenantIdProvider.currentTenantId(); }
    private String actor() {
        String actor = MDC.get("userId");
        if (actor == null || actor.isBlank()) throw new IllegalStateException("TIANSHU_AUDIT_ACTOR_REQUIRED");
        return actor;
    }

    @Override public Optional<DdcPublishTaskBO> findByChangeId(String changeId) {
        return Optional.ofNullable(mapper.selectByChangeId(tenantId(), changeId)).map(converter::toTarget);
    }
    @Override public Optional<DdcPublishTaskBO> findForUpdateByChangeId(String changeId) {
        return Optional.ofNullable(mapper.selectForUpdateByChangeId(tenantId(), changeId)).map(converter::toTarget);
    }
    @Override public List<DdcPublishTaskBO> findAll() {
        return converter.toTargetList(mapper.selectAll(tenantId()));
    }
    @Override public List<DdcPublishTaskBO> findByStatusIn(Collection<String> statuses) {
        return statuses == null || statuses.isEmpty() ? List.of()
                : converter.toTargetList(mapper.selectByStatusIn(tenantId(), statuses));
    }
    @Override public List<DdcPublishTaskBO> findByStatusInAndUpdatedAtBefore(
            Collection<String> statuses, LocalDateTime updatedAt) {
        return statuses == null || statuses.isEmpty() ? List.of()
                : converter.toTargetList(mapper.selectByStatusInAndUpdatedAtBefore(tenantId(), statuses, updatedAt));
    }
    @Override public DdcPageResult<DdcPublishTaskBO> search(String bizCode, String env, String appCode,
            String status, String changeId, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? PageQuery.defaultPage() : pageQuery;
        Long tenant = tenantId();
        List<DdcPublishTaskBO> records = converter.toTargetList(mapper.selectSearch(tenant, bizCode, env,
                appCode, status, changeId, page.offset(), page.pageSize()));
        return DdcPageResult.<DdcPublishTaskBO>builder().records(records)
                .total(mapper.countSearch(tenant, bizCode, env, appCode, status, changeId))
                .pageNo(page.pageNo()).pageSize(page.pageSize()).build();
    }
    @Override public int claimStaleForRecovery(String changeId, Collection<String> activeStatuses,
            LocalDateTime staleBefore, LocalDateTime claimedAt) {
        return activeStatuses == null || activeStatuses.isEmpty() ? 0
                : mapper.claimStaleForRecovery(tenantId(), changeId, activeStatuses, staleBefore, claimedAt, actor());
    }
    @Override public Optional<DdcPublishTaskBO> findFirstByBizCodeAndEnvAndAppCodeAndResourceNameAndStatusIn(
            String bizCode, String env, String appCode, String resourceName, Collection<String> statuses) {
        return statuses == null || statuses.isEmpty() ? Optional.empty()
                : Optional.ofNullable(mapper.selectFirstByScopeAndResourceAndStatusIn(
                        tenantId(), bizCode, env, appCode, resourceName, statuses)).map(converter::toTarget);
    }
    @Override public int transitionToPublishing(String changeId, Collection<String> dispatchableStatuses,
            String pendingStatus, String publishingStatus, LocalDateTime dispatchedAt) {
        return dispatchableStatuses == null || dispatchableStatuses.isEmpty() ? 0
                : mapper.transitionToPublishing(tenantId(), changeId, dispatchableStatuses,
                        pendingStatus, publishingStatus, dispatchedAt, actor());
    }
    @Override public int updateCounters(String changeId, int ackCount, int failedCount, int ignoredCount,
            int timeoutCount, LocalDateTime updatedAt) {
        return mapper.updateCounters(tenantId(), changeId, ackCount, failedCount, ignoredCount,
                timeoutCount, updatedAt, actor());
    }
    @Override public int transitionToTerminal(String changeId, String terminalStatus, LocalDateTime completedAt,
            String failureStage, String errorMessage, Collection<String> activeStatuses) {
        return activeStatuses == null || activeStatuses.isEmpty() ? 0
                : mapper.transitionToTerminal(tenantId(), changeId, terminalStatus, completedAt,
                        failureStage, errorMessage, activeStatuses, actor());
    }
    @Override public int resetForRetry(String changeId, String pendingStatus, String operator,
            LocalDateTime updatedAt, Collection<String> retryableStatuses) {
        return retryableStatuses == null || retryableStatuses.isEmpty() ? 0
                : mapper.resetForRetry(tenantId(), changeId, pendingStatus, operator,
                        updatedAt, retryableStatuses, actor());
    }
    @Override public int markRetryLeaseExpired(String changeId, String failedStatus, LocalDateTime completedAt,
            String failureStage, String errorMessage, Collection<String> retryableStatuses) {
        return retryableStatuses == null || retryableStatuses.isEmpty() ? 0
                : mapper.markRetryLeaseExpired(tenantId(), changeId, failedStatus, completedAt,
                        failureStage, errorMessage, retryableStatuses, actor());
    }

    @Override public DdcPublishTaskBO insert(DdcPublishTaskBO command) {
        DdcPublishTaskPO row = converter.toSource(command);
        long id = identifierGenerator.nextId(row);
        row.setId(id).setTenantId(tenantId());
        if (row.getPublicId() == null || row.getPublicId().isBlank()) row.setPublicId(Long.toString(id));
        try {
            if (!save(row)) throw new IllegalStateException("TIANSHU_PUBLISH_TASK_INSERT_NOT_APPLIED");
        } catch (DataIntegrityViolationException failure) {
            if (businessKeyConflict(failure)) throw new DdcAdminException("publish change id already exists", failure);
            throw failure;
        }
        return converter.toTarget(row);
    }

    @Override public DdcPublishTaskBO update(DdcPublishTaskBO command) {
        DdcPublishTaskPO row = converter.toSource(command);
        if (!updateById(row)) throw new DdcAdminException("publish task version changed");
        return converter.toTarget(row);
    }

    private boolean businessKeyConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("uk_publish_task_business")
                    || message.contains("uk_publish_task_public"))) return true;
        }
        return false;
    }
}
