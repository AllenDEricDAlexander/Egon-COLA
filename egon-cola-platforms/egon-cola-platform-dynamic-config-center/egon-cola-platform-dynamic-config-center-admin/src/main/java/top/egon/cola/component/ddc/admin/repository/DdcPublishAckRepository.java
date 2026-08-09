package top.egon.cola.component.ddc.admin.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DdcPublishAckRepository extends JpaRepository<DdcPublishAckEntity, String> {

    Optional<DdcPublishAckEntity> findByChangeIdAndInstanceIdAndLeaseId(
            String changeId,
            String instanceId,
            String leaseId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DdcPublishAckEntity> findForUpdateByChangeIdAndInstanceIdAndLeaseId(
            String changeId,
            String instanceId,
            String leaseId
    );

    List<DdcPublishAckEntity> findByChangeId(String changeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishAckEntity target
               set target.ackStatus = :timeoutStatus,
                   target.ackAt = :ackAt
             where target.changeId = :changeId
               and target.ackStatus is null
            """)
    int markIncompleteTimeout(@Param("changeId") String changeId,
                              @Param("timeoutStatus") String timeoutStatus,
                              @Param("ackAt") LocalDateTime ackAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcPublishAckEntity target
               set target.currentVersion = null,
                   target.ackStatus = null,
                   target.errorMessage = null,
                   target.ackAt = null
             where target.changeId = :changeId
            """)
    int resetTargets(@Param("changeId") String changeId);
}
