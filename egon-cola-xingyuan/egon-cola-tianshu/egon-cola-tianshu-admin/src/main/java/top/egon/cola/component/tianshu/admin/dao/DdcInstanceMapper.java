package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcInstancePO;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcInstanceMapper extends EgonColaMapper<DdcInstancePO> {

    @Override
    default DdcInstancePO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), internalId(id));
    }

    @Override
    default List<DdcInstancePO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), ids.stream().map(DdcInstanceMapper::internalId).toList());
    }

    private static Long internalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcInstancePO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcInstancePO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcInstancePO selectByInstanceId(@Param("tenantId") Long tenantId, @Param("instanceId") String instanceId);
    List<DdcInstancePO> selectByScope(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode);
    List<DdcInstancePO> selectByScopeAndStatus(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode, @Param("status") String status);
    List<DdcInstancePO> selectPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode, @Param("offset") long offset,
            @Param("limit") int limit);
    long countPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode);
    List<DdcInstancePO> selectByStatusAndLeaseExpireAtLessThanEqual(@Param("tenantId") Long tenantId,
            @Param("status") String status, @Param("leaseExpireAt") LocalDateTime leaseExpireAt);
    int markOfflineIfLeaseMatches(@Param("tenantId") Long tenantId, @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion, @Param("instanceId") String instanceId,
            @Param("leaseId") String leaseId, @Param("status") String status,
            @Param("updatedAt") LocalDateTime updatedAt, @Param("updateTime") Instant updateTime,
            @Param("actor") String actor);
    int markResourceAdmissionOffline(@Param("tenantId") Long tenantId, @Param("id") Long id,
            @Param("expectedVersion") Long expectedVersion,
            @Param("resourceServerId") String resourceServerId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode,
            @Param("resourceVersion") long resourceVersion, @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updateTime") Instant updateTime,
            @Param("actor") String actor);
}
