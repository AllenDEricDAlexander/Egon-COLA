package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcPublishTaskPO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcPublishTaskMapper extends EgonColaMapper<DdcPublishTaskPO> {

    @Override
    default DdcPublishTaskPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), internalId(id));
    }

    @Override
    default List<DdcPublishTaskPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), ids.stream().map(DdcPublishTaskMapper::internalId).toList());
    }

    private static Long internalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcPublishTaskPO selectActiveById(@Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    List<DdcPublishTaskPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcPublishTaskPO selectByChangeId(@Param("tenantId") Long tenantId, @Param("changeId") String changeId);
    DdcPublishTaskPO selectForUpdateByChangeId(@Param("tenantId") Long tenantId, @Param("changeId") String changeId);
    List<DdcPublishTaskPO> selectAll(@Param("tenantId") Long tenantId);
    List<DdcPublishTaskPO> selectByStatusIn(@Param("tenantId") Long tenantId, @Param("statuses") Collection<String> statuses);
    List<DdcPublishTaskPO> selectByStatusInAndUpdatedAtBefore(@Param("tenantId") Long tenantId,
            @Param("statuses") Collection<String> statuses, @Param("updatedAt") LocalDateTime updatedAt);
    List<DdcPublishTaskPO> selectSearch(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode, @Param("status") String status,
            @Param("changeId") String changeId, @Param("offset") long offset, @Param("limit") int limit);
    long countSearch(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode, @Param("status") String status,
            @Param("changeId") String changeId);
    DdcPublishTaskPO selectFirstByScopeAndResourceAndStatusIn(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("env") String env, @Param("appCode") String appCode,
            @Param("resourceName") String resourceName, @Param("statuses") Collection<String> statuses);

    int claimStaleForRecovery(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("activeStatuses") Collection<String> activeStatuses, @Param("staleBefore") LocalDateTime staleBefore,
            @Param("claimedAt") LocalDateTime claimedAt, @Param("actor") String actor);
    int transitionToPublishing(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("dispatchableStatuses") Collection<String> dispatchableStatuses,
            @Param("pendingStatus") String pendingStatus, @Param("publishingStatus") String publishingStatus,
            @Param("dispatchedAt") LocalDateTime dispatchedAt, @Param("actor") String actor);
    int updateCounters(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("ackCount") int ackCount, @Param("failedCount") int failedCount,
            @Param("ignoredCount") int ignoredCount, @Param("timeoutCount") int timeoutCount,
            @Param("updatedAt") LocalDateTime updatedAt, @Param("actor") String actor);
    int transitionToTerminal(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("terminalStatus") String terminalStatus, @Param("completedAt") LocalDateTime completedAt,
            @Param("failureStage") String failureStage, @Param("errorMessage") String errorMessage,
            @Param("activeStatuses") Collection<String> activeStatuses, @Param("actor") String actor);
    int resetForRetry(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("pendingStatus") String pendingStatus, @Param("operator") String operator,
            @Param("updatedAt") LocalDateTime updatedAt, @Param("retryableStatuses") Collection<String> retryableStatuses,
            @Param("actor") String actor);
    int markRetryLeaseExpired(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("failedStatus") String failedStatus, @Param("completedAt") LocalDateTime completedAt,
            @Param("failureStage") String failureStage, @Param("errorMessage") String errorMessage,
            @Param("retryableStatuses") Collection<String> retryableStatuses, @Param("actor") String actor);
}
