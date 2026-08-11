package top.egon.cola.component.ddc.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public interface DdcInstanceRepository extends JpaRepository<DdcInstanceEntity, String> {

    Optional<DdcInstanceEntity> findByInstanceId(String instanceId);

    List<DdcInstanceEntity> findByBizCodeAndEnvAndAppCode(
            String bizCode, String env, String appCode);

    Page<DdcInstanceEntity> findByBizCodeAndEnvAndAppCode(
            String bizCode,
            String env,
            String appCode,
            Pageable pageable);

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

    /**
     * 将精确 Resource Server 三元组且版本不晚于停用事件的在线实例标记离线。
     * / Marks online instances offline for the exact Resource Server triple when their version is
     * not newer than the disable event.
     *
     * @param resourceServerId Resource Server 标识 / Resource Server identifier
     * @param bizCode 业务域编码 / business-domain code
     * @param env 环境 / environment
     * @param appCode 应用编码 / application code
     * @param resourceVersion 停用事件版本 / disable-event version
     * @param updatedAt 更新时间 / update time
     * @return 更新行数 / updated row count
     */
    default int markResourceAdmissionOffline(
            String resourceServerId,
            String bizCode,
            String env,
            String appCode,
            long resourceVersion,
            Instant updatedAt) {
        return markResourceAdmissionOfflineAt(
                resourceServerId,
                bizCode,
                env,
                appCode,
                resourceVersion,
                LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC)
        );
    }

    /**
     * 执行精确三元组的批量离线更新。
     * / Executes the exact-triple bulk offline update.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DdcInstanceEntity instance
               set instance.status = 'OFFLINE',
                   instance.updatedAt = :updatedAt
             where instance.resourceServerId = :resourceServerId
               and instance.bizCode = :bizCode
               and instance.env = :env
               and instance.appCode = :appCode
               and instance.resourceVersion <= :resourceVersion
               and instance.status = 'ONLINE'
            """)
    int markResourceAdmissionOfflineAt(
            @Param("resourceServerId") String resourceServerId,
            @Param("bizCode") String bizCode,
            @Param("env") String env,
            @Param("appCode") String appCode,
            @Param("resourceVersion") long resourceVersion,
            @Param("updatedAt") LocalDateTime updatedAt);
}
