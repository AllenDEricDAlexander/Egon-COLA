package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcPublishAckPO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcPublishAckMapper extends EgonColaMapper<DdcPublishAckPO> {

    @Override
    default DdcPublishAckPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), internalId(id));
    }

    @Override
    default List<DdcPublishAckPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), ids.stream().map(DdcPublishAckMapper::internalId).toList());
    }

    private static Long internalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcPublishAckPO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcPublishAckPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcPublishAckPO selectByChangeIdAndInstanceIdAndLeaseId(@Param("tenantId") Long tenantId,
            @Param("changeId") String changeId, @Param("instanceId") String instanceId, @Param("leaseId") String leaseId);
    DdcPublishAckPO selectForUpdateByChangeIdAndInstanceIdAndLeaseId(@Param("tenantId") Long tenantId,
            @Param("changeId") String changeId, @Param("instanceId") String instanceId, @Param("leaseId") String leaseId);
    List<DdcPublishAckPO> selectByChangeId(@Param("tenantId") Long tenantId, @Param("changeId") String changeId);
    int markIncompleteTimeout(@Param("tenantId") Long tenantId, @Param("changeId") String changeId,
            @Param("timeoutStatus") String timeoutStatus, @Param("ackAt") LocalDateTime ackAt, @Param("actor") String actor);
    int resetTargets(@Param("tenantId") Long tenantId, @Param("changeId") String changeId, @Param("actor") String actor);
}
