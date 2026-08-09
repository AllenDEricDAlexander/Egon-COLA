package top.egon.cola.component.ddc.admin.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DdcPublishTaskRepository extends JpaRepository<DdcPublishTaskEntity, String> {

    Optional<DdcPublishTaskEntity> findByChangeId(String changeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DdcPublishTaskEntity> findForUpdateByChangeId(String changeId);

    List<DdcPublishTaskEntity> findByStatusIn(Collection<String> statuses);

    List<DdcPublishTaskEntity> findByStatusInAndUpdatedAtBefore(
            Collection<String> statuses,
            LocalDateTime updatedAt);

    Optional<DdcPublishTaskEntity>
    findFirstByBizCodeAndEnvAndAppCodeAndResourceNameAndStatusIn(
            String bizCode,
            String env,
            String appCode,
            String resourceName,
            Collection<String> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishTaskEntity task
               set task.status = :publishingStatus,
                   task.attemptCount = case
                       when task.status = :pendingStatus
                       then task.attemptCount
                       else coalesce(task.attemptCount, 0) + 1
                   end,
                   task.dispatchedAt = :dispatchedAt,
                   task.completedAt = null,
                   task.failureStage = null,
                   task.errorMessage = null,
                   task.updatedAt = :dispatchedAt
             where task.changeId = :changeId
               and task.status in :dispatchableStatuses
            """)
    int transitionToPublishing(@Param("changeId") String changeId,
                               @Param("dispatchableStatuses") Collection<String> dispatchableStatuses,
                               @Param("pendingStatus") String pendingStatus,
                               @Param("publishingStatus") String publishingStatus,
                               @Param("dispatchedAt") LocalDateTime dispatchedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishTaskEntity task
               set task.ackCount = :ackCount,
                   task.failedCount = :failedCount,
                   task.ignoredCount = :ignoredCount,
                   task.timeoutCount = :timeoutCount,
                   task.updatedAt = :updatedAt
             where task.changeId = :changeId
            """)
    int updateCounters(@Param("changeId") String changeId,
                       @Param("ackCount") int ackCount,
                       @Param("failedCount") int failedCount,
                       @Param("ignoredCount") int ignoredCount,
                       @Param("timeoutCount") int timeoutCount,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishTaskEntity task
               set task.status = :terminalStatus,
                   task.completedAt = :completedAt,
                   task.failureStage = :failureStage,
                   task.errorMessage = :errorMessage,
                   task.updatedAt = :completedAt
             where task.changeId = :changeId
               and task.status in :activeStatuses
            """)
    int transitionToTerminal(@Param("changeId") String changeId,
                             @Param("terminalStatus") String terminalStatus,
                             @Param("completedAt") LocalDateTime completedAt,
                             @Param("failureStage") String failureStage,
                             @Param("errorMessage") String errorMessage,
                             @Param("activeStatuses") Collection<String> activeStatuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishTaskEntity task
               set task.status = :pendingStatus,
                   task.attemptCount = coalesce(task.attemptCount, 0) + 1,
                   task.dispatchedAt = null,
                   task.completedAt = null,
                   task.failureStage = null,
                   task.errorMessage = null,
                   task.ackCount = 0,
                   task.failedCount = 0,
                   task.ignoredCount = 0,
                   task.timeoutCount = 0,
                   task.operator = coalesce(:operator, task.operator),
                   task.updatedAt = :updatedAt
             where task.changeId = :changeId
               and task.status in :retryableStatuses
            """)
    int resetForRetry(@Param("changeId") String changeId,
                      @Param("pendingStatus") String pendingStatus,
                      @Param("operator") String operator,
                      @Param("updatedAt") LocalDateTime updatedAt,
                      @Param("retryableStatuses") Collection<String> retryableStatuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishTaskEntity task
               set task.status = :failedStatus,
                   task.completedAt = :completedAt,
                   task.failureStage = :failureStage,
                   task.errorMessage = :errorMessage,
                   task.updatedAt = :completedAt
             where task.changeId = :changeId
               and task.status in :retryableStatuses
            """)
    int markRetryLeaseExpired(@Param("changeId") String changeId,
                              @Param("failedStatus") String failedStatus,
                              @Param("completedAt") LocalDateTime completedAt,
                              @Param("failureStage") String failureStage,
                              @Param("errorMessage") String errorMessage,
                              @Param("retryableStatuses") Collection<String> retryableStatuses);
}
