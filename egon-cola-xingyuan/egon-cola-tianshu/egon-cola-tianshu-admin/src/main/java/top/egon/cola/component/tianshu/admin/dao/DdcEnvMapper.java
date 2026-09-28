package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcEnvPO;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.Serializable;

@Mapper
@Validated
public interface DdcEnvMapper extends EgonColaMapper<DdcEnvPO> {

    @Override
    default DdcEnvPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }
    @Override
    default List<DdcEnvPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }
    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcEnvPO selectActiveById(@Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);
    List<DdcEnvPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcEnvPO selectActiveByEnvCode(@Param("tenantId") Long tenantId, @Param("envCode") String envCode);
    long countByEnvCode(@Param("tenantId") Long tenantId, @Param("envCode") String envCode,
            @Param("excludeId") Long excludeId);
    List<DdcEnvPO> selectAllActive(@Param("tenantId") Long tenantId);
    List<DdcEnvPO> selectByText(@Param("tenantId") Long tenantId, @Param("envCodeKeyword") String envCodeKeyword,
            @Param("descriptionKeyword") String descriptionKeyword);
    List<DdcEnvPO> selectPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("keyword") String keyword,
            @Param("offset") long offset, @Param("limit") int limit);
    long countPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("keyword") String keyword);
}
