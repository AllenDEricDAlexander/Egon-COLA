package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    List<DdcPublishTaskEntity> findByStatusIn(Collection<String> statuses);

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
}
