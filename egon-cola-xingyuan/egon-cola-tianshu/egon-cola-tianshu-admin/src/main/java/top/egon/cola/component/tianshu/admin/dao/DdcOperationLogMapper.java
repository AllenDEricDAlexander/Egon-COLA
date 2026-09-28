package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcOperationLogPO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcOperationLogMapper extends EgonColaMapper<DdcOperationLogPO> {

    @Override
    default DdcOperationLogPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), internalId(id));
    }

    @Override
    default List<DdcOperationLogPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), ids.stream().map(DdcOperationLogMapper::internalId).toList());
    }

    private static Long internalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcOperationLogPO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcOperationLogPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    List<DdcOperationLogPO> selectByScope(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode);
}
