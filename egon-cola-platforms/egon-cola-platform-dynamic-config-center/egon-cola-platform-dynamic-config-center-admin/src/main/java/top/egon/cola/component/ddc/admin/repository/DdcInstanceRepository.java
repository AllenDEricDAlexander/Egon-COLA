package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DdcInstanceRepository extends JpaRepository<DdcInstanceEntity, String> {

    Optional<DdcInstanceEntity> findByInstanceId(String instanceId);

    List<DdcInstanceEntity> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode);

    List<DdcInstanceEntity> findByBizCodeAndEnvAndAppCodeAndStatus(
            String bizCode, String env, String appCode, String status);

    List<DdcInstanceEntity> findByStatusAndLeaseExpireAtLessThanEqual(String status, LocalDateTime leaseExpireAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcInstanceEntity instance
               set instance.status = :status,
                   instance.updatedAt = :updatedAt
             where instance.instanceId = :instanceId
               and instance.leaseId = :leaseId
            """)
    int markOfflineIfLeaseMatches(@Param("instanceId") String instanceId,
                                  @Param("leaseId") String leaseId,
                                  @Param("status") String status,
                                  @Param("updatedAt") LocalDateTime updatedAt);
}
