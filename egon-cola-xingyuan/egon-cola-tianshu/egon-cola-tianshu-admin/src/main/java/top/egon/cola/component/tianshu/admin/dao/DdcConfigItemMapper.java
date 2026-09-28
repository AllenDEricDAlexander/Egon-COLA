package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcConfigItemPO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mapper
@Validated
public interface DdcConfigItemMapper extends EgonColaMapper<DdcConfigItemPO> {

    @Override
    default DdcConfigItemPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }

    @Override
    default List<DdcConfigItemPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }

    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcConfigItemPO selectActiveById(
            @Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);

    List<DdcConfigItemPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    DdcConfigItemPO selectActiveByPublicId(@Param("tenantId") Long tenantId, @Param("publicId") String publicId);

    DdcConfigItemPO selectByScopeAndResource(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode, @Param("resourceName") String resourceName);

    DdcConfigItemPO selectForPublishByScopeAndResource(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("env") String env, @Param("appCode") String appCode,
            @Param("resourceName") String resourceName);

    List<DdcConfigItemPO> selectByScope(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("env") String env, @Param("appCode") String appCode);

    List<DdcConfigItemPO> selectByScopeAndDeletedFalse(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("env") String env, @Param("appCode") String appCode);

    long countByEnv(@Param("tenantId") Long tenantId, @Param("env") String env);

    long countByBizCodeAndAppCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("appCode") String appCode);

    List<DdcConfigItemPO> selectSearch(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("env") String env,
            @Param("appCode") String appCode, @Param("resourceName") String resourceName,
            @Param("includeDeleted") boolean includeDeleted);

    List<DdcConfigItemPO> selectPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("env") String env,
            @Param("appCode") String appCode, @Param("resourceName") String resourceName,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("offset") @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class) long offset,
            @Param("limit") @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class) int limit);

    long countPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("env") String env,
            @Param("appCode") String appCode, @Param("resourceName") String resourceName,
            @Param("includeDeleted") boolean includeDeleted);
}
