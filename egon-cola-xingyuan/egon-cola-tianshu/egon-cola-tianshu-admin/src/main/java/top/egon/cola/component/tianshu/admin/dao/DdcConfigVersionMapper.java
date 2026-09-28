package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.Min;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcConfigVersionPO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcConfigVersionMapper extends EgonColaMapper<DdcConfigVersionPO> {

    @Override
    default DdcConfigVersionPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Override
    default List<DdcConfigVersionPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    @Override
    default int deleteVersionedById(DdcConfigVersionPO history) {
        throw new UnsupportedOperationException("IMMUTABLE_CONFIG_HISTORY");
    }

    @Override
    default int restoreVersionedById(DdcConfigVersionPO history) {
        throw new UnsupportedOperationException("IMMUTABLE_CONFIG_HISTORY");
    }

    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcConfigVersionPO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcConfigVersionPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcConfigVersionPO selectByConfigPublicIdAndRevision(@Param("tenantId") Long tenantId,
            @Param("configPublicId") String configPublicId, @Param("revision") Long revision);
    DdcConfigVersionPO selectByConfigItemIdAndRevision(@Param("tenantId") Long tenantId,
            @Param("configItemId") Long configItemId, @Param("revision") Long revision);
    List<DdcConfigVersionPO> selectByConfigPublicId(@Param("tenantId") Long tenantId,
            @Param("configPublicId") String configPublicId);
    List<DdcConfigVersionPO> selectByBizEnvAppResource(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("env") String env, @Param("appCode") String appCode,
            @Param("resourceName") String resourceName);
    List<DdcConfigVersionPO> selectPageByConfigPublicId(@Param("tenantId") Long tenantId,
            @Param("configPublicId") String configPublicId, @Param("offset") long offset, @Param("limit") int limit);
    long countPageByConfigPublicId(@Param("tenantId") Long tenantId, @Param("configPublicId") String configPublicId);
    List<DdcConfigVersionPO> selectPublishedRuntimePage(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("env") String env, @Param("appCode") String appCode,
            @Param("deleteType") String deleteType, @Param("offset") @Min(value = 0,
                    groups = EgonColaModelValidationGroups.Query.class) long offset,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit);
    long countPublishedRuntimePage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode, @Param("deleteType") String deleteType);
}
